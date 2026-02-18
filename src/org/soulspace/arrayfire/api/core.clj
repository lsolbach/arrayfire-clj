(ns org.soulspace.arrayfire.api.core
  (:require [coffi.mem :as mem]
            [tech.v3.resource :refer [stack-resource-context]]
            [tech.v3.datatype :as dtype]
            [tech.v3.datatype.native-buffer :as native-buf]
            [tech.v3.datatype.protocols :as dtype-proto]
            [org.soulspace.arrayfire.ffi.base.definitions :as defs]
            [org.soulspace.arrayfire.integration.unified-api.array :as ua-array]
            [org.soulspace.arrayfire.integration.unified-api.device :as device])
  (:import (org.soulspace.arrayfire.integration.base.resource AFArray)))

;;;
;;; Definitions
;;;
(def dtype-kw->dtype-constant
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

(def dtype-constant->dtype-kw
  "Mapping of ArrayFire dtype constants to Clojure keywords."
    (into {}
        (map (fn [[k v]] [v k]) dtype-kw->dtype-constant)))

(def dtype-kw->size
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

(def return-kw->return-constant
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

(def return-constant->return-kw
  "Mapping of ArrayFire return codes to error keywords."
  (into {}
        (map (fn [[k v]] [v k]) return-kw->return-constant)))

(def backend-kw->backend-constant
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
    (or (get backend-kw->backend-constant backend)
        (throw (ex-info (str "Unknown backend: " backend)
                        {:backend backend
                         :valid-backends (keys backend-kw->backend-constant)})))
    (int backend)))

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
   Values are copied to a double-array and the array is created via the integration layer.

   Parameters:
   - values: Clojure vector of numeric values (doubles)
   - dims: Clojure vector specifying the dimensions of the array

   Returns:
   AFArray instance.

   Example:
   (create-array [1.0 2.0 3.0 4.0] [2 2]) ; creates a 2x2 array"
  [values dims]
  (ua-array/create-array (double-array values) dims defs/AF_DTYPE_F64))

(defn to-host
  "Copy ArrayFire array data to host memory, returning a double array.
   Note: n (number of elements) must be provided.

   Parameters:
   - arr: AFArray instance
   - n: number of elements to copy

   Returns:
   Java double array containing the data.

   Example:
   (to-host my-array 100) ; copies 100 elements from the array"
  [^AFArray arr n]
  (let [buf (mem/alloc (* n 8)) ; 8 bytes per double
        _   (ua-array/get-data-ptr arr buf)
        out (double-array n)]
    (doseq [i (range n)]
      (aset-double out i (mem/read-double buf (* i 8))))
    out))

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
        af-dtype (dtype->af-dtype dtype-kw)
        nbuf     (dtype/as-native-buffer native-buffer)
        _        (when-not nbuf
                   (throw (ex-info "Buffer must be native-backed for zero-copy operation"
                                  {:dtype dtype-kw})))
        address  (.address nbuf)
        n-bytes  (* (dtype/ecount native-buffer) (get defs/dtype->size af-dtype))
        ;; Reinterpret the native address as a MemorySegment (zero-copy)
        host-seg (mem/reinterpret (java.lang.foreign.MemorySegment/ofAddress address) n-bytes)]
    (ua-array/create-array host-seg dims af-dtype)))


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
  (let [type-size (get defs/dtype->size (dtype->af-dtype dtype-kw))
        n-bytes   (* n type-size)
        buf       (mem/alloc n-bytes)
        _         (ua-array/get-data-ptr arr buf)
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

(def ^:private ^:dynamic *af-arena*
  "Dynamic var holding the current coffi Arena inside a `with-arrayfire` region.
   Thread-confined by default (`:arena-type :confined`). Do NOT access from other
   threads (e.g. `future`, `core.async go` blocks) — doing so will throw
   `WrongThreadException`. Use `:arena-type :shared` in `with-arrayfire` for
   multi-threaded use cases."
  nil)

(def backend-lock
  "Lock object for serializing backend/device switching.
   Public because it is referenced by the `with-arrayfire` macro expansion
   from other namespaces."
  (Object.))

(def ^:dynamic *backend-device-stack*
  "Thread-local stack of backend/device frames pushed by nested `with-arrayfire`
   regions that switch the backend or device.

   Each frame is a map with keys:
   - `:backend`  — the ArrayFire backend constant (integer) active in this region
   - `:device`   — the device index (integer) active in this region

   The top-most (innermost) frame is accessible via `(peek *backend-device-stack*)`.
   An empty vector means no switching region is currently active.

   This var is public to allow introspection of the current backend/device context
   from helpers called inside a `with-arrayfire` body.

   Example:
     (with-arrayfire {:backend :cpu :device 0}
       (peek *backend-device-stack*))
     ;; => {:backend 2, :device 0}  (AF_BACKEND_CPU = 2)"
  [])

(defn open-arena
  "Open an FFM Arena of the requested type.
  Public because it is referenced by the `with-arrayfire` macro expansion
  from other namespaces.

  Parameters:
  - arena-type: `:confined` (default, thread-local, cheap allocation) or
                `:shared`   (cross-thread safe, higher overhead)

  Returns:
  An open `java.lang.foreign.Arena` instance (use in `with-open`)."
  [arena-type]
  (case arena-type
    :confined (mem/confined-arena)
    :shared   (mem/shared-arena)
    (throw (ex-info (str "Unknown :arena-type " arena-type
                         ". Valid values: :confined, :shared")
                    {:arena-type arena-type}))))

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
        dtype-kw (get af-dtype->dtype-keyword af-type :float64)]
    (to-native-buffer arr dtype-kw n)))

(defn vec-converter
  "Converter that converts an AFArray to a Clojure vector based structure.
   It uses the effective dimensions of the array (trailing size-1 dims are stripped)
   to reshape the flat column-major data into nested vectors.
   Uses the to-host function, which always returns double data.

   Parameters:
   - arr: AFArray instance

   Returns:
   Clojure vector based structure with the array data.
   1D arrays return a flat vector, 2D a vector of column vectors, etc."
  [^AFArray arr]
  (let [n    (ua-array/get-elements arr)
        ;; ArrayFire always returns 4 dims, padding unused dims with 1.
        ;; Strip trailing 1s to get the effective logical shape.
        all-dims       (ua-array/get-dims arr)
        effective-dims (vec (reverse (drop-while #(= 1 %) (reverse all-dims))))
        data           (to-host arr n)]
    (loop [d  effective-dims
           ds data]
      (if (empty? d)
        (first ds)
        (let [size      (first d)
              rest-dims (rest d)]
          (recur rest-dims
                 (mapv #(vec (take size %))
                       (partition-all size ds))))))))

;;
;; with-arrayfire execution region
;;
(defmacro with-arrayfire
  "Execute body within a deterministic GPU compute region.

  Establishes:
  - ArrayFire initialization (once)
  - Optional backend/device switching (serialized via lock)
  - FFM Arena scope (deterministic cleanup)
  - tech.resource scope (AFArray lifecycle management)
  - Result conversion (AFArray → host data via deep walk)

  AFArray values MUST NOT escape this region. Any AFArray in the return
  value is automatically converted to host data via the converter function.

  Parameters:
  - opts  (optional map, first argument) — see Options below
  - body  — forms to execute within the ArrayFire region

  Options map (optional first argument):
  - :backend      keyword (:cpu, :cuda, :opencl, :oneapi, :default) or int constant.
                  Switches the global ArrayFire backend for the duration of the body
                  and restores it afterwards (serialized via a lock).
  - :device       integer device index. Switches the active device and restores it.
  - :converter-fn function to convert a single AFArray to host data.
                  Defaults to `default-af-converter` (→ dtype-next native buffer).
                  Pass `identity` to skip auto-conversion (only safe when body
                  does not return any AFArray values).
  - :arena-type   `:confined` (default) or `:shared`.
                  `:confined`  — thread-local arena, cheap allocation.  Do NOT
                                 use from other threads (future, go blocks) —
                                 `WrongThreadException` will be thrown.
                  `:shared`    — cross-thread safe arena, higher allocation
                                 overhead.  Use when the body dispatches work
                                 to other threads and those threads allocate
                                 native memory through the arena.

  Returns:
  The result of evaluating body, with all AFArray instances converted to host data.

  Examples:
    ;; Basic usage — results auto-converted
    (with-arrayfire
      (let [a (create-array [1.0 2.0 3.0 4.0] [2 2])]
        (to-host a 4)))

    ;; With explicit backend and device selection
    (with-arrayfire {:backend :cuda :device 0}
      ...)

    ;; Multi-threaded body — use shared arena
    (with-arrayfire {:arena-type :shared}
      (let [f (future (create-array [1.0 2.0] [2]))]
        (vec (to-host @f 2))))

    ;; Skip auto-conversion when result is already host data
    (with-arrayfire {:converter-fn identity}
      ...)"
  [& args]
  (let [known-opts   #{:backend :device :converter-fn :arena-type}
        opts-map?    (and (map? (first args))
                          (or (empty? (first args))
                              (some known-opts (keys (first args)))))
        [opts body]  (if opts-map?
                       [(first args) (rest args)]
                       [{} args])
        converter    (or (:converter-fn opts) `default-af-converter)
        arena-type   (get opts :arena-type :confined)
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
               ;; Push an introspection frame onto the per-thread stack.
               ;; `binding` unwinds automatically — no explicit pop needed.
               (binding [*backend-device-stack*
                         (conj *backend-device-stack*
                               {:backend (device/get-active-backend)
                                :device  (device/get-device)})]
                 (with-open [~arena-sym (open-arena ~arena-type)]
                   (binding [*af-arena* ~arena-sym]
                     (stack-resource-context
                      (let [~result-sym (do ~@body)]
                        (device/sync!)
                        (result-convert ~converter ~result-sym))))))
               (finally
                 ~(when has-device?
                    `(device/set-device! ~prev-device))
                 ~(when has-backend?
                    `(device/set-backend! ~prev-backend)))))))
      ;; No backend/device switching — no lock needed
      `(do
         (ensure-af-init!)
         (with-open [~arena-sym (open-arena ~arena-type)]
           (binding [*af-arena* ~arena-sym]
             (stack-resource-context
              (let [~result-sym (do ~@body)]
                (device/sync!)
                (result-convert ~converter ~result-sym)))))))))

(comment
  ;; with-arrayfire REPL experiments

  ;; Basic usage — explicit host conversion (create-array returns AFArray)
  (with-arrayfire
    (let [a (create-array [1.0 2.0 3.0 4.0] [2 2])]
      (vec (to-host a 4))))

  ;; Empty opts map (valid — treated as no options)
  (with-arrayfire {}
    (vec (to-host (create-array [1.0 2.0] [2]) 2)))

  ;; With backend selection
  (with-arrayfire {:backend :cpu}
    (let [a (create-array [1.0 2.0 3.0] [3])]
      (vec (to-host a 3))))

  ;; With shared arena for multi-threaded body
  (with-arrayfire {:arena-type :shared}
    (let [f (future (create-array [1.0 2.0] [2]))]
      (vec (to-host @f 2))))

  ;; Introspect the current backend/device frame from inside a switching region
  (with-arrayfire {:backend :cpu :device 0}
    (println "current frame:" (peek *backend-device-stack*))
    (with-arrayfire {:backend :cpu :device 0}
      (println "nested frame:" (peek *backend-device-stack*))
      (println "full stack:" *backend-device-stack*)))

  ;; Nested regions (no backend switch — no frame pushed)
  (with-arrayfire
    (with-arrayfire
      (vec (to-host (create-array [42.0] [1]) 1))))
  
  ;; Vector Converter (Clojure vector output instead of dtype-next native buffer)
  ;; ua-array/create-array returns an AFArray, which result-convert picks up.
  (with-arrayfire {:backend    :cpu
                   :converter-fn vec-converter}
    (let [data (double-array [1.0 2.0 3.0 4.0 5.0 6.0])]
      (ua-array/create-array data [2 3] defs/AF_DTYPE_F64)))
  
  ;
  )

