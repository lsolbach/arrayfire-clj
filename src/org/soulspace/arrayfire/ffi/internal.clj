(ns org.soulspace.arrayfire.ffi.internal
  "Bindings for the ArrayFire internal array inspection functions.
   
   This namespace provides low-level introspection capabilities for ArrayFire
   arrays, exposing internal memory layout, ownership, and optimization details.
   These functions are primarily useful for:
   
   1. **Performance optimization**: Understanding memory layout
   2. **Interoperability**: Working with raw device pointers
   3. **Debugging**: Inspecting array structure and ownership
   4. **Advanced memory management**: Understanding allocation patterns
   
   **WARNING**: These are advanced, low-level functions. Incorrect usage can
   lead to memory corruption, crashes, or undefined behavior. Use only when
   necessary for performance optimization or interoperability.
   
   Function Categories:
   
   1. **Strided Array Creation** (af-create-strided-array):
      - Create arrays with custom memory layouts
      - Specify exact strides and offsets
      - Zero-copy wrapping of external data
   
   2. **Memory Layout Inspection**:
      - af-get-strides: Dimensional stride information
      - af-get-offset: Base pointer offset
      - af-is-linear: Contiguity check
   
   3. **Pointer Access** (af-get-raw-ptr):
      - Direct device memory pointer
      - Bypass ArrayFire abstractions
      - Required for external library integration
   
   4. **Ownership and Allocation**:
      - af-is-owner: Ownership status (owner vs view)
      - af-get-allocated-bytes: Physical memory usage
   
   Memory Layout Fundamentals:
   
   **Strides**: Distance between consecutive elements in each dimension.
   For a 3D array (rows × cols × depth):
   - stride[0]: Always 1 (contiguous rows)
   - stride[1]: rows (column stride)
   - stride[2]: rows × cols (depth stride)
   
   Example 4×3×2 array:
   ```
   dims = [4, 3, 2]
   strides = [1, 4, 12]
   ```
   
   Element [i,j,k] is at: offset + i*1 + j*4 + k*12
   
   **Linear (Contiguous) Arrays**:
   Array is linear when elements are stored without gaps:
   - All strides follow standard pattern
   - No offset from base pointer
   - Optimal for performance (cache-friendly)
   
   **Non-linear Arrays** (Views/Subsets):
   - Result from slicing operations: arr[1:5, 2:8]
   - Share data with parent array
   - May have custom strides or offset
   - Not owners (parent owns data)
   
   **Array Ownership**:
   - Owner: Responsible for memory deallocation
   - Non-owner (View): References subset of another array
   - Multiple views can reference same data
   - Parent must outlive all views
   
   Performance Considerations:
   
   **Linear vs Non-linear**:
   - Linear arrays: 2-5× faster for many operations
   - GPU memory access patterns favor contiguous data
   - Consider copying non-linear arrays for repeated use
   
   **Strided Arrays**:
   - Avoid unnecessary copies with custom strides
   - Useful for interoperability with other libraries
   - Can represent transposed data without copying
   
   **Raw Pointers**:
   - Zero-copy interop with CUDA/OpenCL/custom kernels
   - Pointer may be shared (check ownership)
   - Must not free pointer manually
   - Pointer invalidated when array is released
   
   Common Use Cases:
   
   1. **Zero-copy Interop**:
      ```clojure
      ;; Wrap external GPU buffer without copying
      (let [external-ptr (cuda-malloc size)
            arr-ptr (mem/alloc-pointer ::mem/pointer)
            _ (af-create-strided-array
                arr-ptr external-ptr 0 2
                (dims-array [rows cols])
                (strides-array [1 rows])
                af-dtype-f32
                af-source-device)
            arr (mem/read-pointer arr-ptr ::mem/pointer)]
        ;; Use arr...
        )
      ```
   
   2. **Performance Analysis**:
      ```clojure
      ;; Check if operation created copy
      (let [original (create-array data [1000 1000])
            subset (index-array original ...)
            is-owner-ptr (mem/alloc-pointer ::mem/int)
            _ (af-is-owner is-owner-ptr subset)]
        (if (zero? (mem/read-int is-owner-ptr))
          (println \"No copy - efficient view\")
          (println \"Copy created - consider optimization\")))
      ```
   
   3. **Memory Usage Tracking**:
      ```clojure
      ;; Monitor actual GPU memory usage
      (let [arr (create-array data [10000 10000])
            bytes-ptr (mem/alloc-pointer ::mem/long)
            _ (af-get-allocated-bytes bytes-ptr arr)
            bytes (mem/read-long bytes-ptr)]
        (println (str \"GPU memory: \" (/ bytes 1024 1024) \" MB\")))
      ```
   
   4. **Custom Kernel Integration**:
      ```clojure
      ;; Pass ArrayFire array to custom CUDA kernel
      (let [arr (create-array data [1000 1000])
            ptr-ptr (mem/alloc-pointer ::mem/pointer)
            _ (af-get-raw-ptr ptr-ptr arr)
            device-ptr (mem/read-pointer ptr-ptr ::mem/pointer)]
        (cuda-launch-custom-kernel device-ptr ...))
      ```
   
   Type Support:
   All ArrayFire types supported: f32, f64, c32, c64, s32, u32, s64, u64,
   s16, u16, s8, u8, b8, f16
   
   Safety Notes:
   
   1. **Raw Pointers**:
      - Never free manually (ArrayFire manages memory)
      - Pointer may be shared between arrays
      - Invalidated when array released
      - Device-specific (not host accessible)
   
   2. **Strided Arrays**:
      - Validate strides match actual memory layout
      - Incorrect strides cause memory corruption
      - Source type (host/device) must match pointer
   
   3. **Ownership**:
      - Non-owners depend on parent lifetime
      - Don't release parent while views exist
      - Views don't prevent parent deallocation
   
   4. **Thread Safety**:
      - ArrayFire is not thread-safe by default
      - Synchronize access from multiple threads
      - Consider af-lock-array for manual control
   
   API Version Requirements:
   - af_create_strided_array: v3.1+
   - af_get_strides: v3.1+
   - af_get_offset: v3.3+
   - af_get_raw_ptr: v3.3+
   - af_is_linear: v3.3+
   - af_is_owner: v3.3+
   - af_get_allocated_bytes: v3.5+
   
   See also:
   - array.clj: Basic array creation and management
   - memory.clj: Memory allocation and device management
   - device.clj: Device pointer operations"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; Strided array creation

;; af_err af_create_strided_array(af_array *arr, const void *data, const dim_t offset,
;;                                const unsigned ndims, const dim_t *const dims,
;;                                const dim_t *const strides, const af_dtype ty,
;;                                const af_source location)
(defcfn af-create-strided-array
  "Create an ArrayFire array from data with custom strides.
   
   Creates an array with explicit control over memory layout, allowing
   zero-copy wrapping of external data with custom stride patterns.
   
   Parameters:
   - arr: out pointer for array handle
   - data: pointer to data (host or device, based on location)
   - offset: offset from data pointer in number of elements
   - ndims: number of dimensions (1-4)
   - dims: pointer to dimensions array [dim0, dim1, dim2, dim3]
   - strides: pointer to strides array [stride0, stride1, stride2, stride3]
   - ty: data type (af_dtype enum: 0=f32, 1=c32, 2=f64, 3=c64, etc.)
   - location: data source (af_source enum: 0=host, 1=device)
   
   Strides Definition:
   stride[i] = number of elements to skip to reach next element in dimension i
   
   Standard (row-major) strides for dims [d0, d1, d2, d3]:
   - stride[0] = 1  (contiguous in first dimension)
   - stride[1] = d0
   - stride[2] = d0 * d1
   - stride[3] = d0 * d1 * d2
   
   Custom Strides Use Cases:
   1. **Zero-copy transpose**: Swap stride[0] and stride[1]
   2. **External library interop**: Match numpy/cuBLAS/etc layouts
   3. **Shared memory views**: Multiple arrays reference same data
   4. **Padding**: Represent aligned data (e.g., stride larger than dimension)
   
   Offset Use Cases:
   - Start array at non-zero position in buffer
   - Implement array views without data copy
   - Represent sub-regions of larger allocations
   
   Location Parameter:
   - af_source_host (0): data is in CPU memory
     * ArrayFire will copy to device as needed
     * Useful for wrapping host buffers
   - af_source_device (1): data is in GPU memory
     * Zero-copy if already on correct device
     * Pointer must be valid device pointer
   
   Memory Management:
   - ArrayFire TAKES OWNERSHIP of device pointers (location=device)
   - ArrayFire COPIES data from host pointers (location=host)
   - For device pointers: Do NOT free manually after this call
   - Data must remain valid for array lifetime
   
   Validation:
   - ArrayFire validates strides are consistent
   - Strides must allow accessing all dims elements
   - offset + max_index must fit in allocation
   - Invalid parameters return AF_ERR_ARG
   
   Performance:
   - Zero-copy for device pointers with valid strides
   - Host data always copied to device
   - Non-standard strides may reduce performance
   - Linear strides optimize cache access
   
   Example (Standard layout):
   ```clojure
   ;; Create 3×4 array with standard strides
   (let [data (float-array [1 2 3 4 5 6 7 8 9 10 11 12])
         data-ptr (mem/alloc-pointer ::mem/pointer)
         _ (mem/write-pointer data-ptr (mem/address-of data))
         dims (long-array [3 4])  ; 3 rows, 4 cols
         dims-ptr (mem/address-of dims)
         strides (long-array [1 3])  ; standard strides
         strides-ptr (mem/address-of strides)
         arr-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-create-strided-array
             arr-ptr data-ptr 0 2 dims-ptr strides-ptr
             0  ; af_dtype_f32
             0) ; af_source_host
         arr (mem/read-pointer arr-ptr ::mem/pointer)]
     arr)
   ```
   
   Example (Transposed view):
   ```clojure
   ;; Zero-copy transpose via stride swap
   (let [data (float-array (range 12))
         data-ptr (mem/alloc-pointer ::mem/pointer)
         _ (mem/write-pointer data-ptr (mem/address-of data))
         dims (long-array [4 3])  ; transposed dimensions
         dims-ptr (mem/address-of dims)
         strides (long-array [3 1])  ; swapped strides
         strides-ptr (mem/address-of strides)
         arr-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-create-strided-array
             arr-ptr data-ptr 0 2 dims-ptr strides-ptr
             0 0)  ; f32, host
         arr (mem/read-pointer arr-ptr ::mem/pointer)]
     arr)
   ```
   
   Example (Sub-array with offset):
   ```clojure
   ;; Create view of buffer starting at element 10
   (let [data (float-array (range 100))
         data-ptr (mem/address-of data)
         dims (long-array [5 5])  ; 5×5 subarray
         dims-ptr (mem/address-of dims)
         strides (long-array [1 5])
         strides-ptr (mem/address-of strides)
         arr-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-create-strided-array
             arr-ptr data-ptr
             10  ; start at 10th element
             2 dims-ptr strides-ptr 0 0)
         arr (mem/read-pointer arr-ptr ::mem/pointer)]
     arr)
   ```
   
   Common Pitfalls:
   1. Incorrect strides causing out-of-bounds access
   2. Freeing device pointer after transfer of ownership
   3. Using invalid device pointer (from different context)
   4. Offset + size exceeding buffer bounds
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-create-array: Standard array creation
   - af-device-array: Create from device pointer (simpler)
   - af-get-strides: Query array strides"
  "af_create_strided_array" [::mem/pointer ::mem/pointer ::mem/long ::mem/int
                             ::mem/pointer ::mem/pointer ::mem/int ::mem/int] ::mem/int)

;; Memory layout inspection

;; af_err af_get_strides(dim_t *s0, dim_t *s1, dim_t *s2, dim_t *s3, const af_array in)
(defcfn af-get-strides
  "Get the strides of an array.
   
   Returns the stride (element skip distance) for each dimension. Strides
   define the memory layout and determine how multi-dimensional indexing
   maps to linear memory addresses.
   
   Parameters:
   - s0: out pointer for first dimension stride (dim_t/long)
   - s1: out pointer for second dimension stride (dim_t/long)
   - s2: out pointer for third dimension stride (dim_t/long)
   - s3: out pointer for fourth dimension stride (dim_t/long)
   - in: array handle
   
   Stride Definition:
   For element [i, j, k, l], linear index = offset + i*s0 + j*s1 + k*s2 + l*s3
   
   Standard Strides (row-major, contiguous):
   For array with dims [d0, d1, d2, d3]:
   - s0 = 1 (elements adjacent in first dimension)
   - s1 = d0
   - s2 = d0 * d1
   - s3 = d0 * d1 * d2
   
   Non-standard Strides Indicate:
   1. **Array views/slices**: Non-contiguous subset of parent
   2. **Transposed data**: Dimensions reordered
   3. **Padded data**: Alignment requirements (stride > dimension)
   4. **Interleaved data**: Custom memory layout
   
   Interpreting Strides:
   - s0 = 1: Contiguous in first dimension (most common)
   - s0 > 1: Gaps between consecutive elements in first dimension
   - Large strides: Non-local memory access (cache inefficient)
   
   Use Cases:
   1. **Performance analysis**: Identify non-contiguous arrays
   2. **Memory layout validation**: Verify expected structure
   3. **Interoperability**: Match external library requirements
   4. **Debug**: Understand view relationships
   
   Performance Impact:
   - Standard strides: Optimal cache utilization
   - Non-standard strides: Potential 2-10× slowdown
   - GPU coalescing requires contiguous memory access
   - Consider af-copy-array to create contiguous copy
   
   Example (Inspect strides):
   ```clojure
   ;; Check array memory layout
   (let [arr (create-array data [10 20 30])
         s0-ptr (mem/alloc-pointer ::mem/long)
         s1-ptr (mem/alloc-pointer ::mem/long)
         s2-ptr (mem/alloc-pointer ::mem/long)
         s3-ptr (mem/alloc-pointer ::mem/long)
         _ (af-get-strides s0-ptr s1-ptr s2-ptr s3-ptr arr)
         s0 (mem/read-long s0-ptr)
         s1 (mem/read-long s1-ptr)
         s2 (mem/read-long s2-ptr)]
     (println (str \"Strides: [\" s0 \", \" s1 \", \" s2 \"]\"))
     ;; Expected: [1, 10, 200] for standard layout
     )
   ```
   
   Example (Detect non-contiguous):
   ```clojure
   ;; Check if array is a view
   (let [original (create-array data [100 100])
         subset (index-array original ...) ; some slicing operation
         s0-ptr (mem/alloc-pointer ::mem/long)
         s1-ptr (mem/alloc-pointer ::mem/long)
         s2-ptr (mem/alloc-pointer ::mem/long)
         s3-ptr (mem/alloc-pointer ::mem/long)
         _ (af-get-strides s0-ptr s1-ptr s2-ptr s3-ptr subset)
         s0 (mem/read-long s0-ptr)
         s1 (mem/read-long s1-ptr)]
     (if (or (not= s0 1) (not= s1 100))
       (println \"Non-standard strides - consider copying\")
       (println \"Standard strides - optimal\")))
   ```
   
   Example (Calculate element address):
   ```clojure
   ;; Compute linear index for element [i j k]
   (defn linear-index [i j k strides offset]
     (let [[s0 s1 s2 _] strides]
       (+ offset (* i s0) (* j s1) (* k s2))))
   ```
   
   Type Support:
   All array types supported.
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-is-linear: Check if strides are standard
   - af-get-offset: Get base pointer offset
   - af-create-strided-array: Create array with custom strides"
  "af_get_strides" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_get_offset(dim_t *offset, const af_array arr)
(defcfn af-get-offset
  "Get the offset of an array from its base pointer.
   
   Returns the number of elements between the base allocation pointer
   and the start of this array's data. Non-zero offsets indicate the
   array is a view/slice of a larger allocation.
   
   Parameters:
   - offset: out pointer for offset value (dim_t/long, element count)
   - arr: array handle
   
   Offset Interpretation:
   - offset = 0: Array starts at beginning of allocation
   - offset > 0: Array is a view/slice starting at element N
   
   Use Cases:
   1. **View detection**: Non-zero offset indicates slice/subset
   2. **Memory sharing**: Multiple arrays can share base allocation
   3. **Pointer arithmetic**: Calculate actual data start
   4. **Debugging**: Understand array relationships
   
   Combined with Raw Pointer:
   actual_data_start = raw_ptr + (offset * element_size)
   
   Example (Check offset):
   ```clojure
   ;; Determine if array is a view
   (let [original (create-array data [1000])
         subset (index-array original 100 200) ; elements 100-199
         offset-ptr (mem/alloc-pointer ::mem/long)
         _ (af-get-offset offset-ptr subset)
         offset (mem/read-long offset-ptr)]
     (if (zero? offset)
       (println \"Full array (offset=0)\")
       (println (str \"View with offset=\" offset))))
   ```
   
   Example (Calculate data address):
   ```clojure
   ;; Get actual memory address of array data
   (let [arr (some-array)
         offset-ptr (mem/alloc-pointer ::mem/long)
         raw-ptr-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-get-offset offset-ptr arr)
         _ (af-get-raw-ptr raw-ptr-ptr arr)
         offset (mem/read-long offset-ptr)
         base-ptr (mem/read-pointer raw-ptr-ptr ::mem/pointer)
         element-size 4  ; bytes (e.g., f32)
         data-start (+ base-ptr (* offset element-size))]
     (println (str \"Data starts at: \" data-start)))
   ```
   
   Performance Notes:
   - Offset alone doesn't impact performance
   - Non-zero offset typically accompanies non-standard strides
   - Combined effect (offset + strides) determines access patterns
   
   Type Support:
   All array types supported.
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-get-raw-ptr: Get base allocation pointer
   - af-get-strides: Get stride information
   - af-is-owner: Check array ownership"
  "af_get_offset" [::mem/pointer ::mem/pointer] ::mem/int)

;; Pointer access

;; af_err af_get_raw_ptr(void **ptr, const af_array arr)
(defcfn af-get-raw-ptr
  "Get the raw device pointer to an array's data.
   
   Returns a pointer to the array's device memory. This enables zero-copy
   interoperability with custom kernels, external libraries, and direct
   GPU programming.
   
   Parameters:
   - ptr: out pointer for device pointer (void**)
   - arr: array handle
   
   **CRITICAL SAFETY WARNINGS**:
   1. **DO NOT FREE**: ArrayFire manages memory, manual free causes crash
   2. **SHARED POINTER**: Multiple arrays may reference same memory
   3. **DEVICE ONLY**: Pointer is device memory (not host accessible)
   4. **LIFETIME**: Pointer invalidated when array released
   5. **SYNCHRONIZATION**: May need device sync before use
   
   Pointer Characteristics:
   - Points to BASE allocation (use af-get-offset for views)
   - Device-specific (CUDA, OpenCL, CPU backend)
   - Read-only OR read-write depending on usage
   - Shared between parent and view arrays
   
   Use Cases:
   1. **Custom CUDA/OpenCL kernels**: Direct GPU memory access
   2. **External library interop**: cuBLAS, cuDNN, thrust, etc.
   3. **Zero-copy data exchange**: Avoid host-device transfers
   4. **Performance optimization**: Bypass ArrayFire abstractions
   
   Workflow Pattern:
   ```
   1. Get raw pointer via af-get-raw-ptr
   2. Optionally get offset via af-get-offset (for views)
   3. Use pointer in custom kernel/library
   4. Synchronize device if needed
   5. Continue using ArrayFire array (no manual free)
   ```
   
   Backend-specific Types:
   - **CUDA backend**: Pointer is `CUdeviceptr` or `void*` (device memory)
   - **OpenCL backend**: Pointer is `cl_mem` object
   - **CPU backend**: Pointer is regular memory address
   
   Example (CUDA kernel integration):
   ```clojure
   ;; Pass ArrayFire array to custom CUDA kernel
   (let [arr (create-array data [1024 1024])
         ptr-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-get-raw-ptr ptr-ptr arr)
         device-ptr (mem/read-pointer ptr-ptr ::mem/pointer)
         
         ;; Use device-ptr in custom CUDA kernel
         _ (cuda-launch-kernel device-ptr ...)]
     
     ;; Continue using arr in ArrayFire
     (af-add-array result arr other-arr))
   ```
   
   Example (cuBLAS interop):
   ```clojure
   ;; Use ArrayFire array in cuBLAS operation
   (let [arr-a (create-array data-a [1000 1000])
         arr-b (create-array data-b [1000 1000])
         ptr-a-ptr (mem/alloc-pointer ::mem/pointer)
         ptr-b-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-get-raw-ptr ptr-a-ptr arr-a)
         _ (af-get-raw-ptr ptr-b-ptr arr-b)
         dev-ptr-a (mem/read-pointer ptr-a-ptr ::mem/pointer)
         dev-ptr-b (mem/read-pointer ptr-b-ptr ::mem/pointer)
         
         ;; Call cuBLAS with these pointers
         _ (cublas-gemm handle dev-ptr-a dev-ptr-b ...)]
     
     ;; Results visible in ArrayFire arrays
     (af-print-array arr-a))
   ```
   
   Example (Verify pointer sharing):
   ```clojure
   ;; Check if two arrays share memory
   (let [original (create-array data [1000])
         view (index-array original 100 200)
         ptr1-ptr (mem/alloc-pointer ::mem/pointer)
         ptr2-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-get-raw-ptr ptr1-ptr original)
         _ (af-get-raw-ptr ptr2-ptr view)
         ptr1 (mem/read-pointer ptr1-ptr ::mem/pointer)
         ptr2 (mem/read-pointer ptr2-ptr ::mem/pointer)]
     (if (= ptr1 ptr2)
       (println \"Shared memory - zero-copy view\")
       (println \"Separate allocations\")))
   ```
   
   Common Mistakes:
   1. Calling free() on returned pointer
   2. Assuming pointer is host-accessible
   3. Using pointer after array released
   4. Forgetting device synchronization
   5. Modifying shared pointer without coordination
   
   Performance:
   - No overhead (just returns pointer)
   - Zero-copy: No data movement
   - Ideal for tight integration with GPU libraries
   
   Type Support:
   All array types supported.
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-get-offset: Get offset for view arrays
   - af-get-device-ptr: Alternative name (memory.clj)
   - af-lock-array: Prevent ArrayFire from moving data"
  "af_get_raw_ptr" [::mem/pointer ::mem/pointer] ::mem/int)

;; Contiguity check

;; af_err af_is_linear(bool *result, const af_array arr)
(defcfn af-is-linear
  "Check if an array has contiguous memory layout.
   
   Returns true if array elements are stored contiguously in memory
   without gaps, with standard row-major stride pattern. Linear arrays
   have optimal performance for most operations.
   
   Parameters:
   - result: out pointer for boolean result (int: 0=false, 1=true)
   - arr: array handle
   
   Linear Array Criteria:
   1. offset = 0 (starts at base allocation)
   2. Standard strides: [1, d0, d0*d1, d0*d1*d2]
   3. No gaps between elements
   4. Contiguous memory block
   
   Non-linear Arrays Result From:
   - Slicing: arr[10:20, 5:15]
   - Transposition: af-transpose
   - Reordering: af-reorder
   - Views: af-flat, indexing operations
   - Stride manipulation: af-create-strided-array
   
   Performance Impact:
   
   **Linear Arrays** (result = true):
   - Optimal cache utilization
   - GPU memory coalescing
   - SIMD-friendly access patterns
   - 2-5× faster for many operations
   
   **Non-linear Arrays** (result = false):
   - Scattered memory access
   - Poor cache locality
   - No GPU coalescing
   - Consider copying for repeated use
   
   Optimization Strategy:
   ```clojure
   (if (not (is-linear? arr))
     (copy-array arr)  ; Create contiguous copy
     arr)              ; Use original
   ```
   
   Use Cases:
   1. **Performance optimization**: Identify copy candidates
   2. **Pre-condition checking**: Verify algorithm assumptions
   3. **Benchmarking**: Compare linear vs non-linear performance
   4. **Memory layout validation**: Ensure contiguity
   
   Example (Check contiguity):
   ```clojure
   ;; Determine if array is contiguous
   (let [arr (create-array data [1000 1000])
         result-ptr (mem/alloc-pointer ::mem/int)
         _ (af-is-linear result-ptr arr)
         is-linear (not (zero? (mem/read-int result-ptr)))]
     (if is-linear
       (println \"Contiguous - optimal performance\")
       (println \"Non-contiguous - consider copying\")))
   ```
   
   Example (Optimize for performance):
   ```clojure
   ;; Ensure contiguous array for tight loop
   (defn ensure-linear [arr]
     (let [result-ptr (mem/alloc-pointer ::mem/int)
           _ (af-is-linear result-ptr arr)
           is-linear (not (zero? (mem/read-int result-ptr)))]
       (if is-linear
         arr
         (let [copy-ptr (mem/alloc-pointer ::mem/pointer)
               _ (af-copy-array copy-ptr arr)]
           (mem/read-pointer copy-ptr ::mem/pointer)))))
   
   ;; Use in hot path
   (let [arr (some-operation)
         optimized (ensure-linear arr)]
     (dotimes [_ 1000]
       (process optimized)))
   ```
   
   Example (Measure impact):
   ```clojure
   ;; Compare performance: linear vs non-linear
   (let [original (create-array data [1000 1000])
         transposed (af-transpose original)
         
         ;; Check linearity
         lin-ptr (mem/alloc-pointer ::mem/int)
         _ (af-is-linear lin-ptr transposed)
         is-lin (not (zero? (mem/read-int lin-ptr)))
         
         ;; Benchmark
         t1 (time (dotimes [_ 100] (process transposed)))
         
         ;; Copy to contiguous
         contig-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-copy-array contig-ptr transposed)
         contig (mem/read-pointer contig-ptr ::mem/pointer)
         
         t2 (time (dotimes [_ 100] (process contig)))]
     
     (println (str \"Non-linear: \" t1 \"ms\"))
     (println (str \"Linear: \" t2 \"ms\"))
     (println (str \"Speedup: \" (/ t1 t2) \"×\")))
   ```
   
   Related Concepts:
   - **Contiguous**: All elements adjacent (linear = true)
   - **Strided**: Gaps between elements (may be linear = false)
   - **View**: References parent data (often linear = false)
   - **Owner**: Independent allocation (usually linear = true)
   
   Type Support:
   All array types supported.
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-get-strides: Get exact stride pattern
   - af-copy-array: Create contiguous copy
   - af-is-owner: Check ownership status"
  "af_is_linear" [::mem/pointer ::mem/pointer] ::mem/int)

;; Ownership check

;; af_err af_is_owner(bool *result, const af_array arr)
(defcfn af-is-owner
  "Check if an array owns its underlying data.
   
   Returns true if the array is responsible for deallocating its memory.
   False indicates the array is a view/subset referencing another array's data.
   
   Parameters:
   - result: out pointer for boolean result (int: 0=false, 1=true)
   - arr: array handle
   
   Ownership States:
   
   **Owner (result = true)**:
   - Allocated its own memory
   - Responsible for deallocation
   - Independent lifetime
   - Created via: af-create-array, af-randu, computations
   
   **Non-owner/View (result = false)**:
   - References parent array's memory
   - Parent owns the memory
   - Dependent lifetime (parent must outlive view)
   - Created via: indexing, af-flat, slicing operations
   
   Ownership Implications:
   
   1. **Memory Management**:
      - Owner: af-release-array frees memory
      - View: af-release-array only releases handle
      - Parent deallocation invalidates all views
   
   2. **Lifetime Dependencies**:
      - Views depend on parent staying alive
      - Releasing parent too early causes undefined behavior
      - Multiple views can reference same parent
   
   3. **Memory Sharing**:
      - Views enable zero-copy subsets
      - Modifications to view affect parent (if mutable)
      - Efficient for large arrays
   
   Use Cases:
   1. **Memory leak prevention**: Track ownership chains
   2. **Lifetime management**: Ensure parent outlives views
   3. **Performance analysis**: Identify zero-copy views
   4. **Debugging**: Understand array relationships
   
   Common Patterns:
   
   **Safe View Usage**:
   ```clojure
   (let [parent (create-array data [1000 1000])]
     (let [view (index-array parent 100 200)]
       ;; Use view while parent is alive
       (process view))
     ;; View out of scope, safe to release parent
     (af-release-array parent))
   ```
   
   **Unsafe View Usage**:
   ```clojure
   (let [view (let [parent (create-array data [1000 1000])]
                (index-array parent 100 200))]
     ;; DANGER: parent released, view now invalid!
     (process view)) ; Undefined behavior
   ```
   
   Example (Check ownership):
   ```clojure
   ;; Determine if array is owner or view
   (let [original (create-array data [1000 1000])
         subset (index-array original 100 200)
         
         owner-ptr (mem/alloc-pointer ::mem/int)
         _ (af-is-owner owner-ptr subset)
         is-owner (not (zero? (mem/read-int owner-ptr)))]
     
     (if is-owner
       (println \"Owner - independent memory\")
       (println \"View - depends on parent\")))
   ```
   
   Example (Track ownership for cleanup):
   ```clojure
   ;; Resource management with ownership tracking
   (defn safe-cleanup [arrays]
     (doseq [arr arrays]
       (let [owner-ptr (mem/alloc-pointer ::mem/int)
             _ (af-is-owner owner-ptr arr)
             is-owner (not (zero? (mem/read-int owner-ptr)))]
         (when is-owner
           (println \"Releasing owned array\")
           (af-release-array arr))
         (when-not is-owner
           (println \"Skipping view (parent will release)\")))))
   ```
   
   Example (Verify zero-copy):
   ```clojure
   ;; Confirm operation didn't copy
   (let [original (create-array data [10000 10000])
         ;; Some indexing operation
         result (index-array original ...)
         
         owner-ptr (mem/alloc-pointer ::mem/int)
         _ (af-is-owner owner-ptr result)
         is-owner (not (zero? (mem/read-int owner-ptr)))
         
         ;; Also check same raw pointer
         ptr1-ptr (mem/alloc-pointer ::mem/pointer)
         ptr2-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-get-raw-ptr ptr1-ptr original)
         _ (af-get-raw-ptr ptr2-ptr result)
         same-ptr (= (mem/read-pointer ptr1-ptr ::mem/pointer)
                     (mem/read-pointer ptr2-ptr ::mem/pointer))]
     
     (if (and (not is-owner) same-ptr)
       (println \"Zero-copy view confirmed\")
       (println \"Copy created\")))
   ```
   
   Ownership Transition:
   - af-copy-array: Creates new owner from view
   - af-retain-array: Increments reference, doesn't change ownership
   - Most operations: Create new owners
   - Indexing/slicing: Create views (non-owners)
   
   Memory Efficiency:
   - Views: Zero memory overhead (shared allocation)
   - Owners: Full allocation size
   - Trade-off: Lifetime complexity vs memory savings
   
   Type Support:
   All array types supported.
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-get-raw-ptr: Check pointer sharing
   - af-copy-array: Convert view to owner
   - af-retain-array: Increment reference count"
  "af_is_owner" [::mem/pointer ::mem/pointer] ::mem/int)

;; Memory allocation query

;; af_err af_get_allocated_bytes(size_t *bytes, const af_array arr)
(defcfn af-get-allocated-bytes
  "Get the physical memory size allocated for an array.
   
   Returns the actual GPU/device memory size in bytes. For views/subsets,
   returns the size of the parent allocation (full memory block size).
   
   Parameters:
   - bytes: out pointer for size in bytes (size_t/long)
   - arr: array handle
   
   Behavior:
   - **Owner arrays**: Returns size of this array's allocation
   - **View arrays**: Returns size of parent's allocation (full block)
   - **Includes**: All padding, alignment, internal metadata
   - **Excludes**: Host memory copies (if any)
   
   Size Calculation Factors:
   1. Element count: Total elements in allocation
   2. Element size: Bytes per element (f32=4, f64=8, c64=16, etc.)
   3. Padding: Alignment requirements
   4. Overhead: ArrayFire metadata (minimal)
   
   Expected Sizes (no padding):
   - f32 array [1000]: 4,000 bytes (1000 * 4)
   - f64 array [100, 100]: 80,000 bytes (10000 * 8)
   - c64 array [256, 256]: 1,048,576 bytes (65536 * 16)
   
   Use Cases:
   1. **Memory profiling**: Track GPU memory usage
   2. **Optimization**: Identify large allocations
   3. **Resource planning**: Estimate memory requirements
   4. **Debugging**: Verify expected sizes
   5. **Monitoring**: Detect memory leaks
   
   Example (Check memory usage):
   ```clojure
   ;; Get array memory footprint
   (let [arr (create-array data [10000 10000])
         bytes-ptr (mem/alloc-pointer ::mem/long)
         _ (af-get-allocated-bytes bytes-ptr arr)
         bytes (mem/read-long bytes-ptr)
         mb (/ bytes 1024.0 1024.0)]
     (println (str \"Array uses \" mb \" MB of GPU memory\")))
   ```
   
   Example (Memory profiling):
   ```clojure
   ;; Track memory usage during computation
   (defn profile-memory [label arr]
     (let [bytes-ptr (mem/alloc-pointer ::mem/long)
           _ (af-get-allocated-bytes bytes-ptr arr)
           bytes (mem/read-long bytes-ptr)]
       (println (str label \": \" (/ bytes 1024 1024) \" MB\"))))
   
   (let [a (create-array data-a [1000 1000])]
     (profile-memory \"Input A\" a)
     
     (let [b (create-array data-b [1000 1000])]
       (profile-memory \"Input B\" b)
       
       (let [result (af-matmul a b)]
         (profile-memory \"Result\" result)
         
         ;; Output:
         ;; Input A: 7.62939453125 MB
         ;; Input B: 7.62939453125 MB
         ;; Result: 7.62939453125 MB
         )))
   ```
   
   Example (View vs owner size):
   ```clojure
   ;; Compare view and parent sizes
   (let [parent (create-array data [10000 10000])
         view (index-array parent 100 200) ; small subset
         
         parent-bytes-ptr (mem/alloc-pointer ::mem/long)
         view-bytes-ptr (mem/alloc-pointer ::mem/long)
         _ (af-get-allocated-bytes parent-bytes-ptr parent)
         _ (af-get-allocated-bytes view-bytes-ptr view)
         
         parent-mb (/ (mem/read-long parent-bytes-ptr) 1024.0 1024.0)
         view-mb (/ (mem/read-long view-bytes-ptr) 1024.0 1024.0)]
     
     (println (str \"Parent: \" parent-mb \" MB\"))
     (println (str \"View: \" view-mb \" MB (same allocation)\"))
     ;; Both show same size (full allocation)
     )
   ```
   
   Example (Memory leak detection):
   ```clojure
   ;; Monitor memory growth
   (defn total-gpu-memory []
     (let [info (af-device-mem-info)
           ;; Sum all tracked allocations
           ]))
   
   (let [before (total-gpu-memory)]
     ;; Perform operations
     (dotimes [i 100]
       (let [arr (create-array (random-data) [1000 1000])]
         (process arr)
         ;; Should release here!
         ))
     
     (let [after (total-gpu-memory)
           leaked (- after before)]
       (when (pos? leaked)
         (println (str \"WARNING: \" leaked \" bytes not released\")))))
   ```
   
   Example (Estimate memory requirements):
   ```clojure
   ;; Predict memory needs before allocation
   (defn estimate-array-bytes [dims dtype]
     (let [element-size (case dtype
                          :f32 4, :f64 8, :c32 8, :c64 16
                          :s32 4, :u32 4, :s64 8, :u64 8
                          :s16 2, :u16 2, :s8 1, :u8 1
                          :b8 1, :f16 2)
           element-count (reduce * dims)]
       (* element-count element-size)))
   
   (let [planned-dims [10000 10000]
         estimated (estimate-array-bytes planned-dims :f64)
         mb (/ estimated 1024.0 1024.0)]
     (println (str \"Will allocate ~\" mb \" MB\"))
     ;; Proceed if enough GPU memory available
     )
   ```
   
   Notes:
   - Size includes full parent allocation for views
   - Actual memory may be slightly larger (alignment)
   - Does not include temporary allocations
   - Multiple views share single allocation
   
   Performance:
   - Very fast (just returns cached value)
   - No device synchronization required
   
   Type Support:
   All array types supported.
   
   API Version:
   Requires ArrayFire v3.5+
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-device-mem-info: Global device memory stats (device.clj)
   - af-get-elements: Get element count
   - af-is-owner: Check ownership status"
  "af_get_allocated_bytes" [::mem/pointer ::mem/pointer] ::mem/int)
