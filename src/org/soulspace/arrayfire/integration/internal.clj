(ns org.soulspace.arrayfire.integration.internal
  "Integration of the ArrayFire internal/advanced FFI bindings with error
   handling and resource management on the JVM.
   
   This namespace provides low-level functions for:
   
   - Memory layout control: create-strided-array
   - Layout inspection: get-strides, get-offset, is-linear?
   - Memory access: get-raw-ptr, get-allocated-bytes
   - Ownership: is-owner?
   
   These functions enable:
   - Zero-copy interoperability with external libraries
   - Custom memory layouts and strides
   - Performance optimization through layout inspection
   - Direct GPU memory access
   - Advanced memory management
   
   Most users don't need these functions - they're for advanced scenarios
   like CUDA kernel integration, external library interop, and performance
   optimization."
  (:require [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.internal :as internal-ffi]
            [org.soulspace.arrayfire.integration.jvm-integration :as jvm])
  (:import (org.soulspace.arrayfire.integration.jvm_integration AFArray)
           (java.lang.foreign ValueLayout)))

;;;
;;; Strided Array Creation
;;;

(defn create-strided-array
  "Create an ArrayFire array from data with custom strides.
   
   Enables zero-copy wrapping of external data with custom memory layouts.
   
   Parameters:
   - data: MemorySegment containing the data
   - offset: Offset from data pointer in number of elements (long), default 0
   - dims: Vector of dimensions [d0 d1 d2 d3]
   - strides: Vector of strides [s0 s1 s2 s3]
   - dtype: ArrayFire dtype constant (from jvm-integration), default AF_DTYPE_F32
   - location: Data source (0=host, 1=device), default 0
   
   Strides define memory layout:
   - Standard row-major: [1, d0, d0*d1, d0*d1*d2]
   - Element at [i,j,k,l] is at: offset + i*s0 + j*s1 + k*s2 + l*s3
   
   Returns:
   AFArray wrapping the strided data
   
   Examples:
   ```clojure
   ;; Standard contiguous array
   (create-strided-array data-seg 0 [100 200] [1 100] jvm/AF_DTYPE_F64 0)
   
   ;; Transposed view (swap strides)
   (create-strided-array data-seg 0 [200 100] [100 1] jvm/AF_DTYPE_F64 0)
   
   ;; Array starting at offset 100
   (create-strided-array data-seg 100 [50 50] [1 50] jvm/AF_DTYPE_F32 0)
   ```"
  ([data dims strides]
   (create-strided-array data 0 dims strides jvm/AF_DTYPE_F32 0))
  ([data offset dims strides dtype location]
   (let [out (jvm/native-af-array-pointer)
         ndims (count dims)
         dims-seg (jvm/dims->segment dims)
         strides-seg (jvm/dims->segment strides)]
     (jvm/check! (internal-ffi/af-create-strided-array 
                  out data (long offset) (int ndims) 
                  dims-seg strides-seg (int dtype) (int location))
                 "af-create-strided-array")
     (jvm/af-array-new (jvm/deref-af-array out)))))

;;;
;;; Memory Layout Inspection
;;;

(defn get-strides
  "Get the strides of an array.
   
   Strides define memory layout: element [i,j,k,l] is at linear index
   offset + i*s0 + j*s1 + k*s2 + l*s3
   
   Standard row-major strides for dims [d0,d1,d2,d3]:
   - s0 = 1 (contiguous in first dimension)
   - s1 = d0
   - s2 = d0 * d1
   - s3 = d0 * d1 * d2
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Vector of strides [s0 s1 s2 s3]
   
   Examples:
   ```clojure
   ;; Check if array is contiguous
   (let [[s0 s1 s2 s3] (get-strides arr)]
     (when (= s0 1)
       (println \"Contiguous in first dimension\")))
   
   ;; Detect transposed layout
   (let [[s0 s1] (take 2 (get-strides arr))
         [d0 d1] (take 2 (get-dims arr))]
     (when (and (= s0 d1) (= s1 1))
       (println \"Transposed layout detected\")))
   ```"
  [^AFArray arr]
  (let [s0-buf (mem/alloc 8)
        s1-buf (mem/alloc 8)
        s2-buf (mem/alloc 8)
        s3-buf (mem/alloc 8)]
    (jvm/check! (internal-ffi/af-get-strides s0-buf s1-buf s2-buf s3-buf (jvm/af-handle arr))
                "af-get-strides")
    [(mem/read-long s0-buf 0)
     (mem/read-long s1-buf 0)
     (mem/read-long s2-buf 0)
     (mem/read-long s3-buf 0)]))

(defn get-offset
  "Get the offset of an array from its base pointer.
   
   Returns the number of elements between the base allocation pointer
   and the start of this array's data.
   
   - offset = 0: Array starts at beginning of allocation
   - offset > 0: Array is a view/slice starting at element N
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Offset as long (element count, not bytes)
   
   Examples:
   ```clojure
   ;; Check if array is a view
   (let [offset (get-offset arr)]
     (if (zero? offset)
       (println \"Full array\")
       (println (str \"View with offset \" offset))))
   ```"
  [^AFArray arr]
  (let [offset-buf (mem/alloc 8)]
    (jvm/check! (internal-ffi/af-get-offset offset-buf (jvm/af-handle arr))
                "af-get-offset")
    (mem/read-long offset-buf 0)))

(defn is-linear?
  "Check if an array has contiguous memory layout.
   
   Returns true if array elements are stored contiguously without gaps,
   with standard row-major stride pattern. Linear arrays have optimal
   performance.
   
   Linear criteria:
   - offset = 0
   - Standard strides: [1, d0, d0*d1, d0*d1*d2]
   - No gaps between elements
   
   Non-linear arrays result from slicing, transposition, or reordering.
   Consider copying non-linear arrays for better performance.
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean - true if contiguous, false otherwise
   
   Examples:
   ```clojure
   ;; Optimize for performance
   (let [arr (some-operation)]
     (if-not (is-linear? arr)
       (copy-array arr)  ; Create contiguous copy
       arr))            ; Use original
   
   ;; Performance analysis
   (when-not (is-linear? arr)
     (println \"Non-contiguous array - performance may be reduced\"))
   ```"
  [^AFArray arr]
  (let [result-buf (mem/alloc 4)]
    (jvm/check! (internal-ffi/af-is-linear result-buf (jvm/af-handle arr))
                "af-is-linear")
    (not (zero? (mem/read-int result-buf 0)))))

(defn is-owner?
  "Check if an array owns its underlying data.
   
   Returns true if the array allocated its own memory and is responsible
   for deallocation. False indicates the array is a view referencing
   another array's data.
   
   Owner arrays:
   - Allocated their own memory
   - Independent lifetime
   - Created via create-array, randu, computations
   
   View arrays (non-owner):
   - Reference parent array's memory
   - Dependent on parent lifetime
   - Created via indexing, slicing operations
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean - true if owner, false if view
   
   Examples:
   ```clojure
   ;; Check ownership
   (let [parent (create-array data [1000])
         view (index parent [100 200])]
     (println (str \"Parent is owner: \" (is-owner? parent)))  ; true
     (println (str \"View is owner: \" (is-owner? view))))      ; false
   
   ;; Safe cleanup considering ownership
   (when (is-owner? arr)
     (println \"Will deallocate memory\"))
   ```"
  [^AFArray arr]
  (let [result-buf (mem/alloc 4)]
    (jvm/check! (internal-ffi/af-is-owner result-buf (jvm/af-handle arr))
                "af-is-owner")
    (not (zero? (mem/read-int result-buf 0)))))

;;;
;;; Memory Access
;;;

(defn get-raw-ptr
  "Get the raw device pointer to an array's data.
   
   Returns a pointer to the array's device memory for zero-copy
   interoperability with custom kernels and external libraries.
   
   **CRITICAL WARNINGS**:
   - DO NOT FREE manually - ArrayFire manages memory
   - Pointer is device memory (GPU), not host accessible
   - Pointer invalidated when array is released
   - Multiple arrays may share same pointer (views)
   
   Use cases:
   - Custom CUDA/OpenCL kernels
   - External library interop (cuBLAS, cuDNN)
   - Zero-copy data exchange
   
   For view arrays, use get-offset to find actual data start:
   actual_data = raw_ptr + (offset * element_size)
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   MemorySegment containing device pointer
   
   Examples:
   ```clojure
   ;; Get device pointer for custom kernel
   (let [ptr (get-raw-ptr arr)
         offset (get-offset arr)]
     ;; Pass to custom CUDA kernel
     (launch-kernel ptr offset ...))
   
   ;; Check if two arrays share memory
   (let [ptr1 (get-raw-ptr arr1)
         ptr2 (get-raw-ptr arr2)]
     (when (= (.address ptr1) (.address ptr2))
       (println \"Arrays share memory\")))
   ```"
  [^AFArray arr]
  (let [ptr-buf (mem/alloc 8)]
    (jvm/check! (internal-ffi/af-get-raw-ptr ptr-buf (jvm/af-handle arr))
                "af-get-raw-ptr")
    (.get ptr-buf ValueLayout/ADDRESS 0)))

(defn get-allocated-bytes
  "Get the physical memory size allocated for an array.
   
   Returns actual GPU/device memory size in bytes. For view arrays,
   returns the size of the parent allocation (full memory block).
   
   - Owner arrays: Size of this array's allocation
   - View arrays: Size of parent's full allocation
   - Includes: Padding, alignment, metadata
   - Excludes: Host memory copies
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Size in bytes (long)
   
   Examples:
   ```clojure
   ;; Memory profiling
   (let [bytes (get-allocated-bytes arr)
         mb (/ bytes 1024.0 1024.0)]
     (println (str \"Array uses \" mb \" MB of GPU memory\")))
   
   ;; Track memory usage
   (defn profile-memory [label arr]
     (let [bytes (get-allocated-bytes arr)
           mb (/ bytes 1024 1024)]
       (println (str label \": \" mb \" MB\"))))
   
   (profile-memory \"Input\" input-arr)
   (profile-memory \"After transform\" result-arr)
   ```"
  [^AFArray arr]
  (let [bytes-buf (mem/alloc 8)]
    (jvm/check! (internal-ffi/af-get-allocated-bytes bytes-buf (jvm/af-handle arr))
                "af-get-allocated-bytes")
    (mem/read-long bytes-buf 0)))

;;;
;;; Convenience Functions
;;;

(defn array-info
  "Get comprehensive information about an array's memory layout.
   
   Returns a map with all internal array properties for debugging
   and optimization.
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Map containing:
   - :strides - Vector of strides [s0 s1 s2 s3]
   - :offset - Offset from base pointer (elements)
   - :is-linear - Boolean, true if contiguous
   - :is-owner - Boolean, true if owns memory
   - :allocated-bytes - Physical memory size (bytes)
   - :raw-ptr - Device pointer (MemorySegment)
   
   Examples:
   ```clojure
   ;; Comprehensive array inspection
   (let [info (array-info arr)]
     (println \"Array Information:\")
     (println (str \"  Strides: \" (:strides info)))
     (println (str \"  Offset: \" (:offset info)))
     (println (str \"  Linear: \" (:is-linear info)))
     (println (str \"  Owner: \" (:is-owner info)))
     (println (str \"  Memory: \" (/ (:allocated-bytes info) 1024 1024) \" MB\")))
   
   ;; Performance diagnosis
   (defn diagnose-performance [arr]
     (let [info (array-info arr)]
       (cond
         (not (:is-linear info))
         \"Non-contiguous layout - consider copying\"
         
         (> (:allocated-bytes info) (* 1024 1024 1024))
         \"Large array (>1GB) - watch memory usage\"
         
         :else
         \"Optimal layout\")))
   ```"
  [^AFArray arr]
  {:strides (get-strides arr)
   :offset (get-offset arr)
   :is-linear (is-linear? arr)
   :is-owner (is-owner? arr)
   :allocated-bytes (get-allocated-bytes arr)
   :raw-ptr (get-raw-ptr arr)})
