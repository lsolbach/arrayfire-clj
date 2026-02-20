(ns org.soulspace.arrayfire.integration.unified-api.array
  "Integration of the ArrayFire array related FFI bindings with the error
   handling and resource management on the JVM."
  (:refer-clojure :exclude [empty? vector? double? integer? bytes?])
  (:require [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.definitions :as ffi-defs]
            [org.soulspace.arrayfire.ffi.c-api.array :as array-ffi]
            [org.soulspace.arrayfire.ffi.c-api.internal :as internal]
            [org.soulspace.arrayfire.integration.base.error :refer [check!]]
            [org.soulspace.arrayfire.integration.base.memory :as bmem]
            [org.soulspace.arrayfire.integration.base.resource :as res])
  (:import (org.soulspace.arrayfire.integration.base.resource AFArray)))

;;;
;;; Type Helper Functions
;;;
(defn floats?
  "Check if data is a float array or a collection of floats."
  [data]
  (or (instance? (Class/forName "[F") data)
      (and (coll? data)
           (every? float? data))))

(defn ints?
  "Check if data is an int array or a collection of ints."
  [data]
  (or (instance? (Class/forName "[I") data)
      (and (coll? data)
           (every? int? data))))

(defn short?
  "Check if x is a short."
  [x]
  (and (clojure.core/integer? x)
       (<= Short/MIN_VALUE x Short/MAX_VALUE)))

(defn shorts?
  "Check if data is a short array or a collection of shorts."
  [data]
  (or (instance? (Class/forName "[S") data)
      (and (coll? data)
           (every? short? data))))

(defn doubles?
  "Check if data is a double array or a collection of doubles."
  [data]
  (or (instance? (Class/forName "[D") data)
      (and (coll? data)
           (every? #(instance? Double %) data))))

(defn longs?
  "Check if data is a long array or a collection of longs."
  [data]
  (or (instance? (Class/forName "[J") data)
      (and (coll? data)
           (every? #(instance? Long %) data))))

(defn bytes?
  "Check if data is a byte array or a collection of bytes."
  [data]
  (or (instance? (Class/forName "[B") data)
      (and (coll? data)
           (every? #(instance? Byte %) data))))

(defn complex-pair?
  "Check if x is a valid complex number pair [real imag]."
  [x]
  (and (clojure.core/vector? x)
       (= 2 (clojure.core/count x))
       (number? (first x))
       (number? (second x))))

(defn complex-floats?
  "Check if data is a collection of complex float pairs."
  [data]
  (and (coll? data)
       (every? complex-pair? data)
       (every? #(every? float? %) data)))

(defn complex-doubles?
  "Check if data is a collection of complex double pairs."
  [data]
  (and (coll? data)
       (every? complex-pair? data)
       (every? #(every? clojure.core/double? %) data)))

;;;
;;; Array Creation
;;;

(defn create-array
  "Create an ArrayFire array from host data.
   
   Parameters:
   - data: Native memory segment or primitive array containing the data
   - dims: Vector of dimensions [d0 d1 d2 d3]
   - dtype: ArrayFire data type constant (e.g., AF_DTYPE_F32)
   
   Returns:
   AFArray instance
   
   Example:
   (let [data (float-array [1.0 2.0 3.0 4.0])
         arr (create-array data [2 2] jvm/AF_DTYPE_F32)]
     arr)"
  ^AFArray
  [data dims dtype]
  (let [out (res/native-af-array-pointer)
        data-seg (cond
                   (floats? data) (bmem/float-array->segment data)
                   (doubles? data) (bmem/double-array->segment data)
                   (ints? data) (bmem/int-array->segment data)
                   (longs? data) (bmem/long-array->segment data)
                   (shorts? data) (bmem/short-array->segment data)
                   (bytes? data) (bmem/byte-array->segment data)
                   ;; For complex pair collections, use dtype to select the correct
                   ;; encoding. C64 requires 8-byte doubles; all others use 4-byte floats.
                   ;; This is necessary because Clojure 1.12's float? returns true for
                   ;; both Float and Double instances, making the predicates ambiguous.
                   (and (coll? data) (every? complex-pair? data))
                   (if (= (int dtype) ffi-defs/AF_DTYPE_C64)
                     (bmem/complex-double-array->segment data)
                     (bmem/complex-float-array->segment data))
                   :else data)
        dims-seg (bmem/dims->segment dims)]
    (check! (array-ffi/af-create-array out data-seg (clojure.core/count dims) dims-seg (int dtype))
                "af-create-array")
    (res/af-array-new (res/deref-af-array out))))

(defn create-handle
  "Create an empty array with specified dimensions and type.
   
   This allocates memory but does not initialize it. Use write-array
   to populate the data.
   
   Parameters:
   - dims: Vector of dimensions [d0 d1 d2 d3]
   - dtype: ArrayFire data type constant
   
   Returns:
   AFArray instance
   
   Example:
   (let [arr (create-handle [10 10] jvm/AF_DTYPE_F64)]
     arr)"
  ^AFArray
  [dims dtype]
  (let [out (res/native-af-array-pointer)
        ndims (clojure.core/count dims)
        dims-seg (bmem/dims->segment dims)]
    (check! (array-ffi/af-create-handle out (int ndims) dims-seg (int dtype))
                "af-create-handle")
    (res/af-array-new (res/deref-af-array out))))

(defn- handle->segment
  "Convert an AFArray handle to a MemorySegment for FFI calls."
  ^java.lang.foreign.MemorySegment
  [^AFArray arr]
  (res/af-handle arr))

(defn copy-array
  "Create a deep copy of an array.
   
   Parameters:
   - in: Input array (AFArray)
   
   Returns:
   New AFArray instance with copied data
   
   Example:
   (let [original (af/array [[1.0 2.0] [3.0 4.0]])
         copy (copy-array original)]
     copy)"
  ^AFArray
  [^AFArray in]
  (let [out (res/native-af-array-pointer)]
    (check! (array-ffi/af-copy-array out (handle->segment in))
                "af-copy-array")
    (res/af-array-new (res/deref-af-array out))))

;;;
;;; Array Data Transfer
;;;
; TODO use constants for src types, check for ArrayFire defined values or define our own
(defn write-array!
  "Write data from host/device memory to an existing array.
   
   This modifies the array in-place.
   
   Parameters:
   - arr: Target array (AFArray)
   - data: Native memory segment containing the data
   - bytes: Number of bytes to write
  - src: Source type (1 for host, 0 for device, default 1)
   
   Returns:
   The modified array
   
   Example:
   (let [arr (create-handle [4] jvm/AF_DTYPE_F32)
         data (float-array [1.0 2.0 3.0 4.0])]
     (write-array! arr data (* 4 4) 1))"
  ([^AFArray arr data bytes]
   (write-array! arr data bytes 1))
  ([^AFArray arr data bytes src]
  (check! (array-ffi/af-write-array (handle->segment arr) data (long bytes) (int src))
               "af-write-array")
   arr))

(defn get-data-ptr
  "Copy array data from device to host memory.
   
   Parameters:
   - arr: Array to read from (AFArray)
   - data: Native memory segment to write data into
   
   Returns:
   nil (data is written to the provided buffer)
   
   Example:
   (let [arr (af/array [1.0 2.0 3.0])
         buf (mem/alloc (* 3 4))]
     (get-data-ptr arr buf)
     ;; buf now contains the array data
     )"
  [^AFArray arr data]
  (check! (array-ffi/af-get-data-ptr data (handle->segment arr))
              "af-get-data-ptr")
  nil)

;;;
;;; Array Information
;;;

(defn get-elements
  "Get the total number of elements in an array.
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Long integer representing total elements
   
   Example:
   (let [arr (af/array [[1 2 3] [4 5 6]])
         n (get-elements arr)]
     n) ; => 6"
  [^AFArray arr]
  (let [elems-buf (mem/alloc 8)]
    (check! (array-ffi/af-get-elements elems-buf (handle->segment arr))
                "af-get-elements")
    (mem/read-long elems-buf 0)))

(defn get-type
  "Get the data type of an array.
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Integer representing the ArrayFire dtype constant
   
   Example:
   (let [arr (af/array [1.0 2.0])
         dtype (get-type arr)]
     dtype) ; => AF_DTYPE_F32 or AF_DTYPE_F64"
  [^AFArray arr]
  (let [type-buf (mem/alloc 4)]
    (check! (array-ffi/af-get-type type-buf (handle->segment arr))
                "af-get-type")
    (mem/read-int type-buf 0)))

(defn get-dims
  "Get the dimensions of an array.
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Vector of four dimension values [d0 d1 d2 d3]
   
   Example:
   (let [arr (af/array [[1 2 3] [4 5 6]])
         dims (get-dims arr)]
     dims) ; => [3 2 1 1] or similar"
  [^AFArray arr]
  (let [d0 (mem/alloc 8)
        d1 (mem/alloc 8)
        d2 (mem/alloc 8)
        d3 (mem/alloc 8)]
      (check! (array-ffi/af-get-dims d0 d1 d2 d3 (handle->segment arr))
                "af-get-dims")
    [(mem/read-long d0 0)
     (mem/read-long d1 0)
     (mem/read-long d2 0)
     (mem/read-long d3 0)]))

(defn get-numdims
  "Get the number of dimensions of an array.
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Integer representing number of dimensions (1-4)
   
   Example:
   (let [arr (af/array [[1 2] [3 4]])
         ndims (get-numdims arr)]
     ndims) ; => 2"
  [^AFArray arr]
  (let [result-buf (mem/alloc 4)]
    (check! (array-ffi/af-get-numdims result-buf (handle->segment arr))
                "af-get-numdims")
    (mem/read-int result-buf 0)))

(defn get-data-ref-count
  "Get the reference count of the array's underlying data.
   
   This indicates how many arrays share the same data buffer.
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Integer reference count
   
   Example:
   (let [arr (af/array [1 2 3])
         count (get-data-ref-count arr)]
     count)"
  [^AFArray arr]
  (let [count-buf (mem/alloc 4)]
    (check! (array-ffi/af-get-data-ref-count count-buf (handle->segment arr))
                "af-get-data-ref-count")
    (mem/read-int count-buf 0)))

;;;
;;; Array Type Predicates
;;;

(defn empty?
  "Check if an array is empty (has zero elements).
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean"
  [^AFArray arr]
  (let [result-buf (mem/alloc 1)]
    (check! (array-ffi/af-is-empty result-buf (handle->segment arr))
                "af-is-empty")
    (not (zero? (mem/read-byte result-buf 0)))))

(defn scalar?
  "Check if an array is a scalar (single element).
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean"
  [^AFArray arr]
  (let [result-buf (mem/alloc 1)]
    (check! (array-ffi/af-is-scalar result-buf (handle->segment arr))
                "af-is-scalar")
    (not (zero? (mem/read-byte result-buf 0)))))

(defn row?
  "Check if an array is a row vector (1 x n).
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean"
  [^AFArray arr]
  (let [result-buf (mem/alloc 1)]
    (check! (array-ffi/af-is-row result-buf (handle->segment arr))
                "af-is-row")
    (not (zero? (mem/read-byte result-buf 0)))))

(defn column?
  "Check if an array is a column vector (n x 1).
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean"
  [^AFArray arr]
  (let [result-buf (mem/alloc 1)]
    (check! (array-ffi/af-is-column result-buf (handle->segment arr))
                "af-is-column")
    (not (zero? (mem/read-byte result-buf 0)))))

(defn vector?
  "Check if an array is a vector (either row or column).
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean"
  [^AFArray arr]
  (let [result-buf (mem/alloc 1)]
    (check! (array-ffi/af-is-vector result-buf (handle->segment arr))
                "af-is-vector")
    (not (zero? (mem/read-byte result-buf 0)))))

(defn complex?
  "Check if an array has complex data type.
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean"
  [^AFArray arr]
  (let [result-buf (mem/alloc 1)]
    (check! (array-ffi/af-is-complex result-buf (handle->segment arr))
                "af-is-complex")
    (not (zero? (mem/read-byte result-buf 0)))))

(defn real?
  "Check if an array has real (non-complex) data type.
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean"
  [^AFArray arr]
  (let [result-buf (mem/alloc 1)]
    (check! (array-ffi/af-is-real result-buf (handle->segment arr))
                "af-is-real")
    (not (zero? (mem/read-byte result-buf 0)))))

(defn double?
  "Check if an array has double precision floating point type.
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean"
  [^AFArray arr]
  (let [result-buf (mem/alloc 1)]
    (check! (array-ffi/af-is-double result-buf (handle->segment arr))
                "af-is-double")
    (not (zero? (mem/read-byte result-buf 0)))))

(defn single?
  "Check if an array has single precision floating point type.
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean"
  [^AFArray arr]
  (let [result-buf (mem/alloc 1)]
    (check! (array-ffi/af-is-single result-buf (handle->segment arr))
                "af-is-single")
    (not (zero? (mem/read-byte result-buf 0)))))

(defn half?
  "Check if an array has half precision floating point type.
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean"
  [^AFArray arr]
  (let [result-buf (mem/alloc 1)]
    (check! (array-ffi/af-is-half result-buf (handle->segment arr))
                "af-is-half")
    (not (zero? (mem/read-byte result-buf 0)))))

(defn realfloating?
  "Check if an array has real floating point type (float, double, half).
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean"
  [^AFArray arr]
  (let [result-buf (mem/alloc 1)]
    (check! (array-ffi/af-is-realfloating result-buf (handle->segment arr))
                "af-is-realfloating")
    (not (zero? (mem/read-byte result-buf 0)))))

(defn floating?
  "Check if an array has floating point type (real or complex).
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean"
  [^AFArray arr]
  (let [result-buf (mem/alloc 1)]
    (check! (array-ffi/af-is-floating result-buf (handle->segment arr))
                "af-is-floating")
    (not (zero? (mem/read-byte result-buf 0)))))

(defn integer?
  "Check if an array has integer data type.
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean"
  [^AFArray arr]
  (let [result-buf (mem/alloc 1)]
    (check! (array-ffi/af-is-integer result-buf (handle->segment arr))
                "af-is-integer")
    (not (zero? (mem/read-byte result-buf 0)))))

(defn bool?
  "Check if an array has boolean data type.
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean"
  [^AFArray arr]
  (let [result-buf (mem/alloc 1)]
    (check! (array-ffi/af-is-bool result-buf (handle->segment arr))
                "af-is-bool")
    (not (zero? (mem/read-byte result-buf 0)))))

(defn sparse?
  "Check if an array is stored in sparse format.
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean"
  [^AFArray arr]
  (let [result-buf (mem/alloc 1)]
    (check! (array-ffi/af-is-sparse result-buf (handle->segment arr))
                "af-is-sparse")
    (not (zero? (mem/read-byte result-buf 0)))))

;;;
;;; Scalar Access
;;;

(defn get-scalar
  "Get the scalar value from a single-element array.
   
   The array must contain exactly one element.
   
   Parameters:
   - arr: Input array (AFArray) with one element
   - output-buffer: Native memory segment to write the value into
   
   Returns:
   nil (value is written to the buffer)
   
   Example:
   (let [arr (af/array [42.0])
         buf (mem/alloc 8)]
     (get-scalar arr buf)
     (mem/read-double buf 0)) ; => 42.0"
  [^AFArray arr output-buffer]
  (check! (array-ffi/af-get-scalar output-buffer (handle->segment arr))
              "af-get-scalar")
  nil)




;;;
;;; Memory Information
;;;

(defn get-allocated-bytes
  "Get the physical memory size allocated for an array.
   
   Returns the actual GPU/device memory size in bytes.
   For views/subsets, returns the size of the parent allocation.
   
   Parameters:
   - arr: Array (AFArray) to query
   
   Returns:
   Size in bytes as a long integer
   
   Example:
   ```clojure
   ;; Check memory usage of an array
   (let [arr (array [[1 2 3] [4 5 6]])
         bytes (get-allocated-bytes arr)
         mb (/ bytes 1024.0 1024.0)]
     (println (str \"Array uses \" mb \" MB\")))
   
   ;; Compare different array sizes
   (let [small (array [1 2 3])
         large (array (repeat 1000000 1.0))]
     {:small (get-allocated-bytes small)
      :large (get-allocated-bytes large)})
   ```
   
   Use cases:
   - Memory profiling: Track GPU memory usage
   - Optimization: Identify large allocations
   - Resource planning: Estimate memory requirements
   - Debugging: Verify expected sizes
   - Monitoring: Detect memory leaks
   
   Notes:
   - Size includes full parent allocation for views
   - Actual memory may be slightly larger (alignment)
   - Does not include temporary allocations
   - Multiple views share single allocation"
  [^AFArray arr]
  (let [bytes-buf (mem/alloc 8)]
    (check! (internal/af-get-allocated-bytes bytes-buf (handle->segment arr))
                "af-get-allocated-bytes")
    (mem/read-long bytes-buf 0)))

(defn allocated-bytes
  "Alias for get-allocated-bytes. 
   
   Get the physical memory size allocated for an array in bytes.
   
   Parameters:
   - arr: Array (AFArray) to query
   
   Returns:
   Size in bytes as a long integer
   
   See: get-allocated-bytes"
  [^AFArray arr]
  (get-allocated-bytes arr))

;;;
;;; Type-aware host copy (array->host)
;;;

;;
;; Per-type reader helpers — private
;;

(defn- read-float-array
  "Read n float elements from a native buffer into a Java float[]."
  [buf n]
  (let [arr (float-array n)]
    (dotimes [i n]
      (aset arr i (mem/read-float buf (* i 4))))
    arr))

(defn- read-double-array
  "Read n double elements from a native buffer into a Java double[]."
  [buf n]
  (let [arr (double-array n)]
    (dotimes [i n]
      (aset arr i (mem/read-double buf (* i 8))))
    arr))

(defn- read-byte-array
  "Read n byte (b8) elements from a native buffer into a Java byte[].
   0 = false, 1 = true."
  [buf n]
  (let [arr (byte-array n)]
    (dotimes [i n]
      (aset arr i (mem/read-byte buf i)))
    arr))

(defn- read-int-array
  "Read n s32 elements from a native buffer into a Java int[]."
  [buf n]
  (let [arr (int-array n)]
    (dotimes [i n]
      (aset arr i (mem/read-int buf (* i 4))))
    arr))

(defn- read-uint32-array
  "Read n u32 elements from a native buffer into a Java long[].
   Widened to long to preserve the full 0–4294967295 unsigned range."
  [buf n]
  (let [arr (long-array n)]
    (dotimes [i n]
      (aset arr i (Integer/toUnsignedLong (mem/read-int buf (* i 4)))))
    arr))

(defn- read-short-array
  "Read n s16 elements from a native buffer into a Java short[]."
  [buf n]
  (let [arr (short-array n)]
    (dotimes [i n]
      (aset arr i (mem/read-short buf (* i 2))))
    arr))

(defn- read-uint16-array
  "Read n u16 elements from a native buffer into a Java int[].
   Widened to int to preserve the full 0–65535 unsigned range."
  [buf n]
  (let [arr (int-array n)]
    (dotimes [i n]
      (aset arr i (Short/toUnsignedInt (mem/read-short buf (* i 2)))))
    arr))

(defn- read-uint8-array
  "Read n u8 elements from a native buffer into a Java short[].
   Widened to short to preserve the full 0–255 unsigned range."
  [buf n]
  (let [arr (short-array n)]
    (dotimes [i n]
      (aset arr i (short (Byte/toUnsignedInt (mem/read-byte buf i)))))
    arr))

(defn- read-long-array
  "Read n s64 elements from a native buffer into a Java long[]."
  [buf n]
  (let [arr (long-array n)]
    (dotimes [i n]
      (aset arr i (mem/read-long buf (* i 8))))
    arr))

(defn- read-ulong-array
  "Read n u64 elements from a native buffer into a Java long[].
   Bits are preserved; callers needing unsigned semantics should use
   Long/toUnsignedString."
  [buf n]
  (read-long-array buf n))

(defn- read-complex-float-array
  "Read n c32 elements (interleaved re/im float pairs) from a native buffer.
   Returns a persistent Clojure vector of [real imag] float pairs.
   Memory layout: [re₀ im₀ re₁ im₁ …], 8 bytes per element."
  [buf n]
  (mapv (fn [i]
          [(mem/read-float buf (* i 8))
           (mem/read-float buf (+ (* i 8) 4))])
        (range n)))

(defn- read-complex-double-array
  "Read n c64 elements (interleaved re/im double pairs) from a native buffer.
   Returns a persistent Clojure vector of [real imag] double pairs.
   Memory layout: [re₀ im₀ re₁ im₁ …], 16 bytes per element."
  [buf n]
  (mapv (fn [i]
          [(mem/read-double buf (* i 16))
           (mem/read-double buf (+ (* i 16) 8))])
        (range n)))

;;
;; Internal buffer allocation helper
;;

(defn- alloc-host-buf
  "Allocate a host MemorySegment of the correct byte size for arr and copy
   the GPU data into it via get-data-ptr.

   Returns a map:
   - :buf     — the populated MemorySegment
   - :n       — element count (long)
   - :af-type — AF dtype integer constant"
  [^AFArray arr]
  (let [n       (get-elements arr)
        af-type (get-type arr)
        n-bytes (* n (get ffi-defs/dtype->size af-type 0))]
    (when (zero? n-bytes)
      (throw (ex-info "Array has zero bytes; cannot copy to host"
                      {:n n :af-type af-type})))
    (let [buf (mem/alloc n-bytes)]
      (get-data-ptr arr buf)
      {:buf buf :n n :af-type af-type})))

;;
;; Public API
;;

(defn array->host
  "Copy AFArray data to an appropriate JVM host object.

   The return type depends on the ArrayFire element dtype:
   - F32  → float[]
   - F64  → double[]
   - B8   → byte[]  (0 = false, 1 = true)
   - S32  → int[]
   - U32  → long[]  (widened to preserve unsigned 0–4294967295 range)
   - U8   → short[] (widened to preserve unsigned 0–255 range)
   - S64  → long[]
   - U64  → long[]  (bits preserved; use Long/toUnsignedString for display)
   - S16  → short[]
   - U16  → int[]   (widened to preserve unsigned 0–65535 range)
   - C32  → [[re₀ im₀] [re₁ im₁] …]  persistent vector of float pairs
   - C64  → [[re₀ im₀] [re₁ im₁] …]  persistent vector of double pairs

   Parameters:
   - arr: AFArray instance

   Returns:
   JVM host representation appropriate for the array's element type.

   Example:
   (let [f32-arr (create-array (float-array [1.0 2.0 3.0]) [3] AF_DTYPE_F32)]
     (array->host f32-arr)) ; => float[] {1.0, 2.0, 3.0}"
  [^AFArray arr]
  (let [{:keys [buf n af-type]} (alloc-host-buf arr)]
    (cond
      (= af-type ffi-defs/AF_DTYPE_F32) (read-float-array buf n)
      (= af-type ffi-defs/AF_DTYPE_C32) (read-complex-float-array buf n)
      (= af-type ffi-defs/AF_DTYPE_F64) (read-double-array buf n)
      (= af-type ffi-defs/AF_DTYPE_C64) (read-complex-double-array buf n)
      (= af-type ffi-defs/AF_DTYPE_B8)  (read-byte-array buf n)
      (= af-type ffi-defs/AF_DTYPE_S32) (read-int-array buf n)
      (= af-type ffi-defs/AF_DTYPE_U32) (read-uint32-array buf n)
      (= af-type ffi-defs/AF_DTYPE_U8)  (read-uint8-array buf n)
      (= af-type ffi-defs/AF_DTYPE_S64) (read-long-array buf n)
      (= af-type ffi-defs/AF_DTYPE_U64) (read-ulong-array buf n)
      (= af-type ffi-defs/AF_DTYPE_S16) (read-short-array buf n)
      (= af-type ffi-defs/AF_DTYPE_U16) (read-uint16-array buf n)
      :else
      (throw (ex-info "Unsupported AF dtype for array->host"
                      {:af-type af-type})))))
