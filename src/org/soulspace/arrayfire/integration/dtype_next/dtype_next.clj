(ns org.soulspace.arrayfire.integration.dtype-next.dtype-next
  "Integration utilities for zero-copy interoperability between ArrayFire and dtype-next."
  (:require [coffi.memory :as mem]
            [tech.v3.datatype :as dtype]
            [tech.v3.datatype.native-buffer :as native-buf]
            [tech.v3.datatype.protocols :as dtype-proto]
            [org.soulspace.arrayfire.ffi.base.definitions :as defs]
            [org.soulspace.arrayfire.integration.unified-api.array :as array])
  (:import (org.soulspace.arrayfire.integration.base.resource AFArray)))

;;
;; Zero-copy integration with dtype-next
;;
(def af-dtype->dtype-next-kw
  "Mapping of ArrayFire dtype constants to dtype-next datatype keywords."
  {defs/AF_DTYPE_F32 :float32
   defs/AF_DTYPE_F64 :float64
   defs/AF_DTYPE_S32 :int32
   defs/AF_DTYPE_U32 :uint32
   defs/AF_DTYPE_S64 :int64
   defs/AF_DTYPE_U64 :uint64
   defs/AF_DTYPE_S16 :int16
   defs/AF_DTYPE_U16 :uint16
   defs/AF_DTYPE_U8  :uint8
   defs/AF_DTYPE_B8  :uint8})

(defn dtype-next-kw->af-dtype
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

   The native buffer's memory address is passed directly to ArrayFire without
   intermediate copies. Note: ArrayFire will still copy the data from host to GPU
   (unavoidable hardware operation).

   Parameters:
   - native-buffer: dtype-next native buffer or tensor (must be :native-heap backed)
   - dims: Clojure vector specifying the dimensions of the array

   Returns:
   AFArray instance.

   Example:
   (let [tensor (dtype/make-container :native-heap :float64 [100])]
     (create-array-from-native tensor [100]))"
  [native-buffer dims]
  (let [dtype-kw (dtype/elemwise-datatype native-buffer)
        af-dtype (dtype-next-kw->af-dtype dtype-kw)
        nbuf     (dtype/as-native-buffer native-buffer)
        _        (when-not nbuf
                   (throw (ex-info "Buffer must be native-backed for zero-copy operation"
                                  {:dtype dtype-kw})))
        address  (.address nbuf)
        n-bytes  (* (dtype/ecount native-buffer) (get defs/dtype->size af-dtype))
        ;; Reinterpret the native address as a MemorySegment (zero-copy)
        host-seg (mem/reinterpret (java.lang.foreign.MemorySegment/ofAddress address) n-bytes)]
    (array/create-array host-seg dims af-dtype)))

(defn to-native-buffer
  "Copy ArrayFire array data to a dtype-next native buffer (minimal copies).

   Data flow: GPU → coffi native memory → wrapped in dtype-next native buffer (zero-copy wrap).
   Note: The GPU→host copy is unavoidable (hardware limitation).

   Parameters:
   - arr: AFArray instance
   - dtype-kw: dtype-next datatype keyword (e.g., :float64, :int32)
   - n: number of elements to copy

   Returns:
   dtype-next native buffer containing the data.

   Example:
   (to-native-buffer my-array :float64 100)"
  [^AFArray arr dtype-kw n]
  (let [type-size (get defs/dtype->size (dtype-next-kw->af-dtype dtype-kw))
        n-bytes   (* n type-size)
        buf       (mem/alloc n-bytes)
        _         (array/get-data-ptr arr buf)
        address   (mem/address-of buf)
        nbuf      (native-buf/wrap-address
                    address
                    n-bytes
                    dtype-kw
                    (dtype-proto/platform-endianness)
                    buf)]
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

