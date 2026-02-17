(ns org.soulspace.arrayfire.core
  (:require [coffi.mem :as mem]
            [tech.v3.datatype :as dtype]
            [tech.v3.datatype.native-buffer :as native-buf]
            [tech.v3.datatype.protocols :as dtype-proto]
            [org.soulspace.arrayfire.ffi.base.definitions :as defs]
            [org.soulspace.arrayfire.ffi.c-api.array :as af-array]
            [org.soulspace.arrayfire.integration.base.error :refer [check!]]
            [org.soulspace.arrayfire.integration.base.memory :as bmem]
            [org.soulspace.arrayfire.integration.unified-api.device :as device]))

;;;
;;; Definitions
;;;
(def type->constant
  "Mapping of Clojure keywords to ArrayFire dtype constants."
  {::f32 defs/AF_DTYPE_F32 ; float
   ::c32 defs/AF_DTYPE_C32 ; complex float
   ::f64 defs/AF_DTYPE_F64 ; double
   ::c64 defs/AF_DTYPE_C64 ; complex double
   ::b8  defs/AF_DTYPE_B8  ; bool
   ::s32 defs/AF_DTYPE_S32 ; int
   ::u32 defs/AF_DTYPE_U32 ; unsigned int
   ::u8  defs/AF_DTYPE_U8  ; unsigned char
   ::s64 defs/AF_DTYPE_S64 ; long
   ::u64 defs/AF_DTYPE_U64 ; unsigned long
   ::s16 defs/AF_DTYPE_S16 ; short
   ::u16 defs/AF_DTYPE_U16 ; unsigned short
   })

(def constant->type
  "Mapping of ArrayFire dtype constants to Clojure keywords."
    (into {}
        (map (fn [[k v]] [v k]) type->constant)))

(def type->size
  "Mapping of Clojure keywords to sizes in bytes for each ArrayFire dtype."
  {::f32 4  ; float
   ::c32 8  ; complex float (2 floats)
   ::f64 8  ; double
   ::c64 16 ; complex double (2 doubles)
   ::b8  1  ; bool
   ::s32 4  ; int
   ::u32 4  ; unsigned int
   ::u8  1  ; unsigned char
   ::s64 8  ; long
   ::u64 8  ; unsigned long
   ::s16 2  ; short
   ::u16 2  ; unsigned short
   })

(def return->constant
  "Mapping of error keywords to ArrayFire error codes."
  {::success                    defs/AF_SUCCESS
   ::err-no-mem                 defs/AF_ERR_NO_MEM
   ::err-driver                 defs/AF_ERR_DRIVER
   ::err-runtime                defs/AF_ERR_RUNTIME
   ::err-invalid-array          defs/AF_ERR_INVALID_ARRAY
   ::err-arg                    defs/AF_ERR_ARG
   ::err-size                   defs/AF_ERR_SIZE
   ::err-type                   defs/AF_ERR_TYPE
   ::err-diff-type              defs/AF_ERR_DIFF_TYPE
   ::err-batch                  defs/AF_ERR_BATCH
   ::err-device                 defs/AF_ERR_DEVICE
   ::err-not-supported          defs/AF_ERR_NOT_SUPPORTED
   ::err-not-configured         defs/AF_ERR_NOT_CONFIGURED
   ::err-non-free               defs/AF_ERR_NONFREE
   ::err-no-double              defs/AF_ERR_NO_DBL
   ::err-no-gfx                 defs/AF_ERR_NO_GFX
   ::err-no-half                defs/AF_ERR_NO_HALF
   ::err-load-lib               defs/AF_ERR_LOAD_LIB
   ::err-load-sym               defs/AF_ERR_LOAD_SYM
   ::err-array-backend-mismatch defs/AF_ERR_ARR_BKND_MISMATCH
   ::err-internal               defs/AF_ERR_INTERNAL
   ::err-unknown                defs/AF_ERR_UNKNOWN
   ;
   })

(def constant->return
  "Mapping of ArrayFire return codes to error keywords."
  (into {}
        (map (fn [[k v]] [v k]) return->constant)))

;(def messages
;  "Mapping of ArrayFire return codes to messages."
;  {})

(defn init!
  "Initialize ArrayFire runtime.
   Must be called before any other ArrayFire functions.
   
   Returns:
   true on success."
  []
  (device/init!))


(defn info
  "Print ArrayFire device information.
   
   Returns:
   :ok on success."
  []
  (device/info))

(comment
  (init!)
  (info)
  )


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
        dimsbuf (bmem/dims->native dims)
        outptr (mem/alloc mem/pointer-size)]
    (check!
     (af-array/af-create-array outptr host (int (count dims)) dimsbuf defs/AF_DTYPE_F64)
     "af_create_array")
    ;; Return the array handle
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
    (check! (af-array/af-get-data-ptr buf handle) "af_get_data_ptr")
    (let [arr (double-array n)]
      (doseq [i (range n)]
        (aset-double arr i (mem/read-double buf (* i 8))))
      arr)))

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
    :float32 defs/AF_DTYPE_F32
    :float64 defs/AF_DTYPE_F64
    :int32   defs/AF_DTYPE_S32
    :uint32  defs/AF_DTYPE_U32
    :int64   defs/AF_DTYPE_S64
    :uint64  defs/AF_DTYPE_U64
    :int16   defs/AF_DTYPE_S16
    :uint16  defs/AF_DTYPE_U16
    :int8    defs/AF_DTYPE_S32  ; dtype-next int8 maps to s32
    :uint8   defs/AF_DTYPE_U8
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
        n-bytes (* (dtype/ecount native-buffer) (get defs/dtype->size af-dtype))
        ;; Wrap the address in a coffi MemorySegment (zero-copy)
        host (mem/reinterpret (java.lang.foreign.MemorySegment/ofAddress address) n-bytes)
        dimsbuf (bmem/dims->native dims)
        outptr (mem/alloc mem/pointer-size)]
    (check!
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
  (let [type-size (get defs/dtype->size (dtype->af-dtype dtype-kw))
        n-bytes (* n type-size)
        ;; Allocate coffi memory
        buf (mem/alloc n-bytes)
        _ (check! (af-array/af-get-data-ptr buf handle) "af_get_data_ptr")
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