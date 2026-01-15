(ns org.soulspace.arrayfire.core
  (:require [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi :as ffi]
            [org.soulspace.arrayfire.ffi.device :as af-device]
            [org.soulspace.arrayfire.ffi.array :as af-array]
            [org.soulspace.arrayfire.ffi.binary :as af-binary]
            [tech.v3.datatype :as dtype]
            [tech.v3.datatype.native-buffer :as native-buf]
            [tech.v3.datatype.protocols :as dtype-proto]))

(defn init!
  "Initialize ArrayFire runtime.
   Must be called before any other ArrayFire functions.
   
   Returns:
   true on success."
  []
  (ffi/check! (af-device/af-init) "af_init")
  true)


(defn info
  "Print ArrayFire device information.
   
   Returns:
   :ok on success."
  []
  (ffi/check! (af-device/af-info) "af_info")
  :ok)


(defn create-array
  "Create an ArrayFire array from a Clojure vector of values.
   Values are copied to native memory and dims specifies the array dimensions.
   
   DEPRECATED: Use type-specific functions like create-array-f64, create-array-c64, etc.
   
   Parameters:
   - values: Clojure vector of numeric values (doubles)
   - dims: Clojure vector specifying the dimensions of the array
   
   Returns:
   ArrayFire array handle.
   
   Example:
   (create-array [1.0 2.0 3.0 4.0] [2 2]) ; creates a 2x2 array"
  [values dims]
  (let [n (count values)
        ;; Allocate host buffer for input data
        host (mem/alloc (* n 8)) ; 8 bytes per double
        _ (doseq [i (range n)]
            (mem/write-double host (* i 8) (double (nth values i))))
        dimsbuf (ffi/dims->native dims)
        outptr (mem/alloc mem/pointer-size)]
    (ffi/check!
     (af-array/af-create-array outptr host (int (count dims)) dimsbuf ffi/AF_DTYPE_F64)
     "af_create_array")
    ;; Return the array handle
    (mem/read-address outptr)))


(defn create-array-f32
  "Create an ArrayFire array of 32-bit floats.
   
   Parameters:
   - values: Clojure vector of numeric values
   - dims: Clojure vector specifying the dimensions of the array
   
   Returns:
   ArrayFire array handle.
   
   Example:
   (create-array-f32 [1.0 2.0 3.0 4.0] [2 2])"
  [values dims]
  (let [n (count values)
        host (mem/alloc (* n 4))
        _ (doseq [i (range n)]
            (ffi/write-float! host (* i 4) (nth values i)))
        dimsbuf (ffi/dims->native dims)
        outptr (mem/alloc mem/pointer-size)]
    (ffi/check!
     (af-array/af-create-array outptr host (int (count dims)) dimsbuf ffi/AF_DTYPE_F32)
     "af_create_array")
    (mem/read-address outptr)))


(defn create-array-f64
  "Create an ArrayFire array of 64-bit doubles.
   
   Parameters:
   - values: Clojure vector of numeric values
   - dims: Clojure vector specifying the dimensions of the array
   
   Returns:
   ArrayFire array handle.
   
   Example:
   (create-array-f64 [1.0 2.0 3.0 4.0] [2 2])"
  [values dims]
  (let [n (count values)
        host (mem/alloc (* n 8))
        _ (doseq [i (range n)]
            (ffi/write-double! host (* i 8) (nth values i)))
        dimsbuf (ffi/dims->native dims)
        outptr (mem/alloc mem/pointer-size)]
    (ffi/check!
     (af-array/af-create-array outptr host (int (count dims)) dimsbuf ffi/AF_DTYPE_F64)
     "af_create_array")
    (mem/read-address outptr)))


(defn create-array-c32
  "Create an ArrayFire array of 32-bit complex floats.
   
   Parameters:
   - values: Clojure vector of [real imag] complex number vectors
   - dims: Clojure vector specifying the dimensions of the array
   
   Returns:
   ArrayFire array handle.
   
   Example:
   (create-array-c32 [[1.0 0.5] [2.0 1.0] [3.0 1.5]] [3])"
  [values dims]
  (let [n (count values)
        host (mem/alloc (* n 8)) ; 8 bytes per complex float (2 floats)
        _ (doseq [i (range n)]
            (ffi/write-complex-float! host (* i 8) (nth values i)))
        dimsbuf (ffi/dims->native dims)
        outptr (mem/alloc mem/pointer-size)]
    (ffi/check!
     (af-array/af-create-array outptr host (int (count dims)) dimsbuf ffi/AF_DTYPE_C32)
     "af_create_array")
    (mem/read-address outptr)))


(defn create-array-c64
  "Create an ArrayFire array of 64-bit complex doubles.
   
   Parameters:
   - values: Clojure vector of [real imag] complex number vectors
   - dims: Clojure vector specifying the dimensions of the array
   
   Returns:
   ArrayFire array handle.
   
   Example:
   (create-array-c64 [[1.0 0.5] [2.0 1.0] [3.0 1.5]] [3])"
  [values dims]
  (let [n (count values)
        host (mem/alloc (* n 16)) ; 16 bytes per complex double (2 doubles)
        _ (doseq [i (range n)]
            (ffi/write-complex-double! host (* i 16) (nth values i)))
        dimsbuf (ffi/dims->native dims)
        outptr (mem/alloc mem/pointer-size)]
    (ffi/check!
     (af-array/af-create-array outptr host (int (count dims)) dimsbuf ffi/AF_DTYPE_C64)
     "af_create_array")
    (mem/read-address outptr)))


(defn create-array-s32
  "Create an ArrayFire array of 32-bit signed integers.
   
   Parameters:
   - values: Clojure vector of integer values
   - dims: Clojure vector specifying the dimensions of the array
   
   Returns:
   ArrayFire array handle.
   
   Example:
   (create-array-s32 [1 2 3 4] [2 2])"
  [values dims]
  (let [n (count values)
        host (mem/alloc (* n 4))
        _ (doseq [i (range n)]
            (ffi/write-int! host (* i 4) (nth values i)))
        dimsbuf (ffi/dims->native dims)
        outptr (mem/alloc mem/pointer-size)]
    (ffi/check!
     (af-array/af-create-array outptr host (int (count dims)) dimsbuf ffi/AF_DTYPE_S32)
     "af_create_array")
    (mem/read-address outptr)))


(defn create-array-u32
  "Create an ArrayFire array of 32-bit unsigned integers.
   
   Parameters:
   - values: Clojure vector of integer values
   - dims: Clojure vector specifying the dimensions of the array
   
   Returns:
   ArrayFire array handle.
   
   Example:
   (create-array-u32 [1 2 3 4] [2 2])"
  [values dims]
  (let [n (count values)
        host (mem/alloc (* n 4))
        _ (doseq [i (range n)]
            (ffi/write-int! host (* i 4) (nth values i)))
        dimsbuf (ffi/dims->native dims)
        outptr (mem/alloc mem/pointer-size)]
    (ffi/check!
     (af-array/af-create-array outptr host (int (count dims)) dimsbuf ffi/AF_DTYPE_U32)
     "af_create_array")
    (mem/read-address outptr)))


(defn create-array-s64
  "Create an ArrayFire array of 64-bit signed integers (long long).
   
   Parameters:
   - values: Clojure vector of integer values
   - dims: Clojure vector specifying the dimensions of the array
   
   Returns:
   ArrayFire array handle.
   
   Example:
   (create-array-s64 [1 2 3 4] [2 2])"
  [values dims]
  (let [n (count values)
        host (mem/alloc (* n 8))
        _ (doseq [i (range n)]
            (ffi/write-long! host (* i 8) (nth values i)))
        dimsbuf (ffi/dims->native dims)
        outptr (mem/alloc mem/pointer-size)]
    (ffi/check!
     (af-array/af-create-array outptr host (int (count dims)) dimsbuf ffi/AF_DTYPE_S64)
     "af_create_array")
    (mem/read-address outptr)))


(defn create-array-u64
  "Create an ArrayFire array of 64-bit unsigned integers (unsigned long long).
   
   Parameters:
   - values: Clojure vector of integer values
   - dims: Clojure vector specifying the dimensions of the array
   
   Returns:
   ArrayFire array handle.
   
   Example:
   (create-array-u64 [1 2 3 4] [2 2])"
  [values dims]
  (let [n (count values)
        host (mem/alloc (* n 8))
        _ (doseq [i (range n)]
            (ffi/write-long! host (* i 8) (nth values i)))
        dimsbuf (ffi/dims->native dims)
        outptr (mem/alloc mem/pointer-size)]
    (ffi/check!
     (af-array/af-create-array outptr host (int (count dims)) dimsbuf ffi/AF_DTYPE_U64)
     "af_create_array")
    (mem/read-address outptr)))


(defn release
  "Release an ArrayFire array handle, freeing GPU memory.
   
   Parameters:
   - handle: ArrayFire array handle to release
   
   Returns:
   true on success."
  [handle]
  (ffi/check! (af-array/af-release-array handle) "af_release_array")
  true)


(defn add
  "Add two ArrayFire arrays element-wise.
   
   Parameters:
   - a: first ArrayFire array handle
   - b: second ArrayFire array handle
   
   Returns:
   New ArrayFire array handle containing the result.
   
   Example:
   (add array1 array2)"
  [a b]
  (let [outptr (mem/alloc mem/pointer-size)]
    (ffi/check! (af-binary/af-add outptr a b 0) "af_add") ; 0 = false for batch parameter
    (mem/read-address outptr)))


(defn to-host
  "Copy ArrayFire array data to host memory, returning a double array.
   Note: n (number of elements) must be provided.
   
   DEPRECATED: Use type-specific functions like to-host-f64, to-host-c64, etc.
   
   Parameters:
   - handle: ArrayFire array handle
   - n: number of elements to copy
   
   Returns:
   Clojure double array containing the data.
   
   Example:
   (to-host array 100) ; copies 100 elements from the array"
  [handle n]
  (let [buf (mem/alloc (* n 8))] ; 8 bytes per double
    (ffi/check! (af-array/af-get-data-ptr buf handle) "af_get_data_ptr")
    (let [arr (double-array n)]
      (doseq [i (range n)]
        (aset-double arr i (mem/read-double buf (* i 8))))
      arr)))


(defn to-host-f32
  "Copy ArrayFire array of floats to host memory, returning a Clojure vector.
   
   Parameters:
   - handle: ArrayFire array handle
   - n: number of elements to copy
   
   Returns:
   Clojure vector of floats.
   
   Example:
   (to-host-f32 array 100)"
  [handle n]
  (let [buf (mem/alloc (* n 4))]
    (ffi/check! (af-array/af-get-data-ptr buf handle) "af_get_data_ptr")
    (mapv #(ffi/read-float buf (* % 4)) (range n))))


(defn to-host-f64
  "Copy ArrayFire array of doubles to host memory, returning a Clojure vector.
   
   Parameters:
   - handle: ArrayFire array handle
   - n: number of elements to copy
   
   Returns:
   Clojure vector of doubles.
   
   Example:
   (to-host-f64 array 100)"
  [handle n]
  (let [buf (mem/alloc (* n 8))]
    (ffi/check! (af-array/af-get-data-ptr buf handle) "af_get_data_ptr")
    (mapv #(ffi/read-double buf (* % 8)) (range n))))


(defn to-host-c32
  "Copy ArrayFire array of complex floats to host memory, returning a Clojure vector of [real imag] vectors.
   
   Parameters:
   - handle: ArrayFire array handle
   - n: number of complex elements to copy
   
   Returns:
   Clojure vector of [real imag] complex number vectors.
   
   Example:
   (to-host-c32 array 100)"
  [handle n]
  (let [buf (mem/alloc (* n 8))] ; 8 bytes per complex float
    (ffi/check! (af-array/af-get-data-ptr buf handle) "af_get_data_ptr")
    (mapv #(ffi/read-complex-float buf (* % 8)) (range n))))


(defn to-host-c64
  "Copy ArrayFire array of complex doubles to host memory, returning a Clojure vector of [real imag] vectors.
   
   Parameters:
   - handle: ArrayFire array handle
   - n: number of complex elements to copy
   
   Returns:
   Clojure vector of [real imag] complex number vectors.
   
   Example:
   (to-host-c64 array 100)"
  [handle n]
  (let [buf (mem/alloc (* n 16))] ; 16 bytes per complex double
    (ffi/check! (af-array/af-get-data-ptr buf handle) "af_get_data_ptr")
    (mapv #(ffi/read-complex-double buf (* % 16)) (range n))))


(defn to-host-s32
  "Copy ArrayFire array of 32-bit signed integers to host memory, returning a Clojure vector.
   
   Parameters:
   - handle: ArrayFire array handle
   - n: number of elements to copy
   
   Returns:
   Clojure vector of integers.
   
   Example:
   (to-host-s32 array 100)"
  [handle n]
  (let [buf (mem/alloc (* n 4))]
    (ffi/check! (af-array/af-get-data-ptr buf handle) "af_get_data_ptr")
    (mapv #(ffi/read-int buf (* % 4)) (range n))))


(defn to-host-u32
  "Copy ArrayFire array of 32-bit unsigned integers to host memory, returning a Clojure vector.
   
   Parameters:
   - handle: ArrayFire array handle
   - n: number of elements to copy
   
   Returns:
   Clojure vector of integers.
   
   Example:
   (to-host-u32 array 100)"
  [handle n]
  (let [buf (mem/alloc (* n 4))]
    (ffi/check! (af-array/af-get-data-ptr buf handle) "af_get_data_ptr")
    (mapv #(ffi/read-int buf (* % 4)) (range n))))


(defn to-host-s64
  "Copy ArrayFire array of 64-bit signed integers to host memory, returning a Clojure vector.
   
   Parameters:
   - handle: ArrayFire array handle
   - n: number of elements to copy
   
   Returns:
   Clojure vector of longs.
   
   Example:
   (to-host-s64 array 100)"
  [handle n]
  (let [buf (mem/alloc (* n 8))]
    (ffi/check! (af-array/af-get-data-ptr buf handle) "af_get_data_ptr")
    (mapv #(ffi/read-long buf (* % 8)) (range n))))


(defn to-host-u64
  "Copy ArrayFire array of 64-bit unsigned integers to host memory, returning a Clojure vector.
   
   Parameters:
   - handle: ArrayFire array handle
   - n: number of elements to copy
   
   Returns:
   Clojure vector of longs.
   
   Example:
   (to-host-u64 array 100)"
  [handle n]
  (let [buf (mem/alloc (* n 8))]
    (ffi/check! (af-array/af-get-data-ptr buf handle) "af_get_data_ptr")
    (mapv #(ffi/read-long buf (* % 8)) (range n))))

;;
;; Zero-copy integration with dtype-next
;;

(defn dtype->af-dtype
  "Convert dtype-next datatype to ArrayFire dtype constant.
   
   Parameters:
   - dtype: dtype-next datatype keyword
   
   Returns:
   ArrayFire dtype constant."
  [dtype]
  (case dtype
    :float32 ffi/AF_DTYPE_F32
    :float64 ffi/AF_DTYPE_F64
    :int32   ffi/AF_DTYPE_S32
    :uint32  ffi/AF_DTYPE_U32
    :int64   ffi/AF_DTYPE_S64
    :uint64  ffi/AF_DTYPE_U64
    :int16   ffi/AF_DTYPE_S16
    :uint16  ffi/AF_DTYPE_U16
    :int8    ffi/AF_DTYPE_S32  ; dtype-next int8 maps to s32
    :uint8   ffi/AF_DTYPE_U8
    (throw (ex-info (str "Unsupported dtype: " dtype) {:dtype dtype}))))


(defn create-array-from-native
  "Create an ArrayFire array from a dtype-next native buffer (zero-copy on host side).
   
   The native buffer's memory is passed directly to ArrayFire without intermediate copies.
   Note: ArrayFire will still copy the data from host to GPU (unavoidable hardware operation).
   
   Parameters:
   - native-buffer: dtype-next native buffer or tensor (must be :native-heap backed)
   - dims: Clojure vector specifying the dimensions of the array
   
   Returns:
   ArrayFire array handle.
   
   Example:
   (let [tensor (dtype/make-container :native-heap :float64 [100])]
     (create-array-from-native tensor [100]))"
  [native-buffer dims]
  (let [dtype-kw (dtype/elemwise-datatype native-buffer)
        af-dtype (dtype->af-dtype dtype-kw)
        ;; Get the native buffer and its address
        nbuf (dtype/as-native-buffer native-buffer)
        _ (when-not nbuf
            (throw (ex-info "Buffer must be native-backed for zero-copy operation" 
                           {:dtype dtype-kw})))
        address (.address nbuf)
        n-bytes (* (dtype/ecount native-buffer) (get ffi/type-sizes af-dtype))
        ;; Wrap the address in a coffi MemorySegment (zero-copy)
        host (mem/reinterpret (java.lang.foreign.MemorySegment/ofAddress address) n-bytes)
        dimsbuf (ffi/dims->native dims)
        outptr (mem/alloc mem/pointer-size)]
    (ffi/check!
     (af-array/af-create-array outptr host (int (count dims)) dimsbuf af-dtype)
     "af_create_array")
    (mem/read-address outptr)))


(defn to-native-buffer
  "Copy ArrayFire array data to a dtype-next native buffer (minimal copies).
   
   Data flow: GPU → coffi native memory → wrapped in dtype-next native buffer (zero-copy wrap).
   Note: The GPU→host copy is unavoidable (hardware limitation).
   
   Parameters:
   - handle: ArrayFire array handle
   - dtype: dtype-next datatype keyword (e.g., :float64, :int32)
   - n: number of elements to copy
   
   Returns:
   dtype-next native buffer containing the data.
   
   Example:
   (to-native-buffer array :float64 100)"
  [handle dtype-kw n]
  (let [type-size (get ffi/type-sizes (dtype->af-dtype dtype-kw))
        n-bytes (* n type-size)
        ;; Allocate coffi memory
        buf (mem/alloc n-bytes)
        _ (ffi/check! (af-array/af-get-data-ptr buf handle) "af_get_data_ptr")
        ;; Get the address and wrap it in a dtype-next native buffer (zero-copy)
        address (mem/address-of buf)
        nbuf (native-buf/wrap-address 
               address 
               n-bytes 
               dtype-kw 
               (dtype-proto/platform-endianness) 
               buf)]
    ;; Return as a dtype-next tensor/container
    (dtype-proto/->buffer nbuf)))


(defn create-array-from-tensor
  "Create an ArrayFire array from a dtype-next tensor.
   
   If the tensor is native-backed, uses zero-copy on host side.
   If heap-backed, copies to native memory first, then uses zero-copy to ArrayFire.
   
   Parameters:
   - tensor: dtype-next tensor (can be native-backed for zero-copy, or heap-backed)
   - dims: Clojure vector specifying the dimensions of the array (optional, inferred from tensor shape if not provided)
   
   Returns:
   ArrayFire array handle.
   
   Example:
   (let [tensor (dtype/make-container :native-heap :float64 [100])]
     (create-array-from-tensor tensor))"
  ([tensor]
   (create-array-from-tensor tensor (vec (dtype/shape tensor))))
  ([tensor dims]
   (if (dtype/as-native-buffer tensor)
     ;; Zero-copy path: tensor is already native
     (create-array-from-native tensor dims)
     ;; Need to copy to native memory first
     (let [native-tensor (dtype/make-container :native-heap 
                                               (dtype/elemwise-datatype tensor) 
                                               (dtype/shape tensor))]
       (dtype/copy! tensor native-tensor)
       (create-array-from-native native-tensor dims)))))