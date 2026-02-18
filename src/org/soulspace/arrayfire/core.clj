(ns org.soulspace.arrayfire.core
  (:require [coffi.mem :as mem]
            [tech.v3.resource :refer [stack-resource-context]]
            [tech.v3.datatype :as dtype]
            [tech.v3.datatype.native-buffer :as native-buf]
            [tech.v3.datatype.protocols :as dtype-proto]
            [org.soulspace.arrayfire.ffi.base.definitions :as defs]
            [org.soulspace.arrayfire.ffi.c-api.array :as af-array]
            [org.soulspace.arrayfire.integration.base.error :refer [check!]]
            [org.soulspace.arrayfire.integration.base.memory :as bmem]
            [org.soulspace.arrayfire.integration.base.resource :as res]
            [org.soulspace.arrayfire.integration.unified-api.array :as ua-array]
            [org.soulspace.arrayfire.integration.unified-api.device :as device])
  (:import (org.soulspace.arrayfire.integration.base.resource AFArray)))

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

(def backend-keyword->constant
  "Mapping of backend keywords to ArrayFire backend constants."
  {:default defs/AF_BACKEND_DEFAULT
   :cpu     defs/AF_BACKEND_CPU
   :cuda    defs/AF_BACKEND_CUDA
   :opencl  defs/AF_BACKEND_OPENCL
   :oneapi  defs/AF_BACKEND_ONEAPI})

(def af-dtype->dtype-keyword
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

(defn resolve-backend
  "Resolve a backend keyword or integer to an ArrayFire backend constant.
   
   Parameters:
   - backend: keyword (:cpu, :cuda, :opencl, :oneapi, :default) or integer constant
   
   Returns:
   ArrayFire backend constant (integer)."
  [backend]
  (if (keyword? backend)
    (or (get backend-keyword->constant backend)
        (throw (ex-info (str "Unknown backend: " backend)
                        {:backend backend
                         :valid-backends (keys backend-keyword->constant)})))
    (int backend)))

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


;;;
;;; Resource management and initialization
;;;

; Atom to track whether ArrayFire has been initialized.
; Ensures init! is called only once.
(defonce af-initialized? (atom false))

(defn ensure-af-init!
  "Ensure that ArrayFire is initialized. Calls init! only on the first invocation.
   Subsequent calls will be no-op, ensuring efficient initialization."
  []
  (when (compare-and-set! af-initialized? false true)
    (init!)))

(def ^:dynamic *af-arena*
  "Dynamic var holding the current coffi Arena inside a `with-arrayfire` region.
   Thread-confined by default — do not access from other threads."
  nil)

(def backend-lock
  "Lock object for serializing backend/device switching."
  (Object.))

(defn result-convert
  "Convert AFArray values in the result before they escape the resource context.
   Walks the result structure to find and convert any AFArray instances.
   
   Parameters:
   - converter: function to convert a single AFArray to host data
   - result: the value returned from the with-arrayfire body
   
   Returns:
   The result with all AFArray instances converted to host data."
  [converter result]
  (cond
    (instance? AFArray result)
    (converter result)

    (map? result)
    (persistent!
     (reduce-kv (fn [m k v]
                  (assoc! m k (result-convert converter v)))
                (transient {}) result))

    (vector? result)
    (mapv #(result-convert converter %) result)

    (sequential? result)
    (map #(result-convert converter %) result)

    (set? result)
    (into #{} (map #(result-convert converter %)) result)

    :else
    result))

(defn default-af-converter
  "Default converter for AFArray → host data.
   Converts an AFArray to a dtype-next native buffer, preserving dtype.
   Uses the integration layer to query array metadata from ArrayFire.
   
   Parameters:
   - arr: AFArray instance
   
   Returns:
   dtype-next native buffer with the array data."
  [^AFArray arr]
  (let [n        (ua-array/get-elements arr)
        af-type  (ua-array/get-type arr)
        dtype-kw (get af-dtype->dtype-keyword af-type :float64)
        handle   (res/af-handle arr)]
    (to-native-buffer handle dtype-kw n)))

;;
;; with-arrayfire execution region
;;
(defmacro with-arrayfire
  "Execute body within a deterministic GPU compute region.
   
   Establishes:
   - ArrayFire initialization (once)
   - Optional backend/device switching (serialized via lock)
   - FFM Arena scope (confined, deterministic cleanup)
   - tech.resource scope (AFArray lifecycle management)
   - Result conversion (AFArray → host data)
   
   AFArray values MUST NOT escape this region. Any AFArray in the return
   value is automatically converted to host data via the converter function.
   
   Parameters:
   - body: code to execute within the ArrayFire region (can return AFArray values)
   - opts (optional): map of options for backend/device selection and result conversion

   Options map (optional first argument):
   - :backend      - keyword (:cpu, :cuda, :opencl, :oneapi) or int constant
   - :device       - integer device index
   - :converter-fn - function to convert AFArray to host data
                     (default: default-af-converter)
   
   Returns:
   The result of evaluating body, with all AFArray instances converted to host data.

   Examples:
     ;; Basic usage — results auto-converted
     (with-arrayfire
       (let [a (create-array [1.0 2.0 3.0 4.0] [2 2])]
         (to-host a 4)))
   
     ;; With backend selection
     (with-arrayfire {:backend :cuda :device 0}
       ...)
   
     ;; Skip auto-conversion when result is already host data
     (with-arrayfire {:converter-fn identity}
       ...)"
  [& args]
  (let [known-opts   #{:backend :device :converter-fn}
        opts-map?    (and (map? (first args))
                          (some known-opts (keys (first args))))
        [opts body]  (if opts-map?
                       [(first args) (rest args)]
                       [{} args])
        converter    (or (:converter-fn opts) `default-af-converter)
        has-backend? (contains? opts :backend)
        has-device?  (contains? opts :device)
        prev-backend (gensym "prev-backend")
        prev-device  (gensym "prev-device")
        arena-sym    (gensym "arena")
        result-sym   (gensym "result")]
    (if (or has-backend? has-device?)
      ;; With backend/device switching — needs lock
      `(do
         (ensure-af-init!)
         (locking backend-lock
           (let [~prev-backend (device/get-active-backend)
                 ~prev-device  (device/get-device)]
             (try
               ~(when has-backend?
                  `(device/set-backend! (resolve-backend ~(:backend opts))))
               ~(when has-device?
                  `(device/set-device! ~(:device opts)))
               (with-open [~arena-sym (mem/confined-arena)]
                 (binding [*af-arena* ~arena-sym]
                   (stack-resource-context
                    (let [~result-sym (do ~@body)]
                      (device/sync!)
                      (result-convert ~converter ~result-sym)))))
               (finally
                 ~(when has-device?
                    `(device/set-device! ~prev-device))
                 ~(when has-backend?
                    `(device/set-backend! ~prev-backend)))))))
      ;; No backend/device switching — no lock needed
      `(do
         (ensure-af-init!)
         (with-open [~arena-sym (mem/confined-arena)]
           (binding [*af-arena* ~arena-sym]
             (stack-resource-context
              (let [~result-sym (do ~@body)]
                (device/sync!)
                (result-convert ~converter ~result-sym)))))))))

(comment
  ;; with-arrayfire REPL experiments

  ;; Basic usage — explicit host conversion
  (with-arrayfire
    (let [a (create-array [1.0 2.0 3.0 4.0] [2 2])]
      (vec (to-host a 4))))

  ;; With backend selection
  (with-arrayfire {:backend :cpu}
    (let [a (create-array [1.0 2.0 3.0] [3])]
      (vec (to-host a 3))))

  ;; Nested regions
  (with-arrayfire
    (with-arrayfire
      (vec (to-host (create-array [42.0] [1]) 1))))
  )

