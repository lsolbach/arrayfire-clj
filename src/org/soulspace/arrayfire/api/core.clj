(ns org.soulspace.arrayfire.api.core
  "Core API namespace for arrayfire-clj.
  
   This namespace is intended to be the main public API for users of arrayfire-clj.
   It abstracts away low-level details and provides a clean, idiomatic Clojure
   interface to ArrayFire functionality.
   
   The namespace provides:
   - Backend/device management
   - Array creation and host conversion utilities
   - Basic array operations
     - indexing, reduction, reshaping, etc.
   - Arithmetic and algorithmic operations
   - with-arrayfire execution region macro
   - Region predicate and guard (`within-arrayfire?`, `assert-within-arrayfire!`)

   The functions in this namespace use Clojure conventions with regards to
   naming, keywords, argument order and return values.

   The `with-arrayfire` macro establishes a deterministic execution region for GPU
   compute, handling initialization, resource management, and optional
   backend/device switching. All API functions are designed to be used within a
   `with-arrayfire` region. AFArray values returned from API functions are
   automatically converted to host data (e.g. dtype-next native buffers or
   Clojure data structures) before they are returned from the region.
   This ensures that AFArray instances do not escape the resource management scope,
   preventing memory leaks and ensuring safe interoperability with Clojure code."
  (:require [coffi.mem :as mem]
            [tech.v3.resource :refer [stack-resource-context]]
            [org.soulspace.arrayfire.integration.base.definitions :as defs]
            [org.soulspace.arrayfire.integration.base.memory :as bmem]
            [org.soulspace.arrayfire.integration.dtype-next.dtype-next :as dtype-next]
            [org.soulspace.arrayfire.integration.unified-api.array :as array]
            [org.soulspace.arrayfire.integration.unified-api.arith :as arith]
            [org.soulspace.arrayfire.integration.unified-api.algorithm :as algo]
            [org.soulspace.arrayfire.integration.unified-api.memory :as uamem]
            [org.soulspace.arrayfire.integration.unified-api.device :as device]
            [org.soulspace.arrayfire.integration.unified-api.data :as data]
            [org.soulspace.arrayfire.integration.unified-api.random :as random])
  (:import (org.soulspace.arrayfire.integration.base.resource AFArray)))

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
        _   (array/get-data-ptr arr buf)
        out (double-array n)]
    (doseq [i (range n)]
      (aset-double out i (mem/read-double buf (* i 8))))
    out))


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
    (device/init!)))

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

(def ^:dynamic *within-arrayfire?*
  "True when the current thread is executing inside a `with-arrayfire` region.
   Bound to `true` by the `with-arrayfire` macro; `false` at the root binding.

   Note: Clojure's `binding` conveys dynamic vars to child threads created via
   `future`, `pmap`, and `agent/send-off`. A `future` spawned inside a
   `with-arrayfire` body therefore also sees `within-arrayfire?` as `true`.
   This is correct for `:arena-type :shared` multi-threaded bodies, but be
   aware of the behaviour when detaching work to unrelated threads.

   Use `within-arrayfire?` (the predicate) to query this value from user or
   library code — do not read this var directly."
  false)

(defn within-arrayfire?
  "Return `true` when called from code executing inside a `with-arrayfire`
   region; `false` otherwise.

   Reads the thread-local dynamic var `*within-arrayfire?*` that
   `with-arrayfire` binds to `true` for the duration of the body.

   Intended uses:
   - Guard API functions to fail fast when called outside a region.
   - Conditional logic that behaves differently inside vs outside a region.
   - Instrumentation and debugging.

   Example:
     (within-arrayfire?)      ;; => false  (no region active)

     (with-arrayfire
       (within-arrayfire?))   ;; => true"
  []
  *within-arrayfire?*)

(defn assert-within-arrayfire!
  "Throw an `IllegalStateException` when not inside a `with-arrayfire` region.
   Call this at the top of API functions that require an active region.

   Parameters:
   - fname: string — the calling function name, used in the error message.

   Example:
     (defn create-array [values dims]
       (assert-within-arrayfire! \"create-array\")
       ...)

   Throws:
   `java.lang.IllegalStateException` with an informative message."
  [fname]
  (when-not (within-arrayfire?)
    (throw (IllegalStateException.
            (str fname " must be called within a `with-arrayfire` region.")))))

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

(defn ->native-buffer
  "Default converter for AFArray → host data.
   Converts an AFArray to a dtype-next native buffer, preserving dtype.
   Uses the integration layer to query array metadata from ArrayFire.

   Parameters:
   - arr: AFArray instance

   Returns:
   dtype-next native buffer with the array data."
  [^AFArray arr]
  (let [n        (array/get-elements arr)
        af-type  (array/get-type arr)
        dtype-kw (get dtype-next/af-dtype->dtype-next-kw af-type :float64)]
    (dtype-next/to-native-buffer arr dtype-kw n)))

(defn ->value
  "Converter that converts an AFArray to a Clojure scalar or vector based value.
   It uses the effective dimensions of the array (trailing size-1 dims are stripped)
   to reshape the flat column-major data into nested vectors.
   Uses the to-host function, which always returns double data.

   Parameters:
   - arr: AFArray instance

   Returns:
   Clojure scalar or vector based value with the array data.
   1D arrays return a flat vector, 2D a vector of column vectors, etc."
  [^AFArray arr]
  (let [n    (array/get-elements arr)
        ;; ArrayFire always returns 4 dims, padding unused dims with 1.
        ;; Strip trailing 1s to get the effective logical shape.
        all-dims       (array/get-dims arr)
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
                  Defaults to `->native-buffer` (→ dtype-next native buffer).
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
        converter    (or (:converter-fn opts) `->native-buffer)
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
                  `(device/set-backend! (defs/resolve-backend ~(:backend opts))))
               ~(when has-device?
                  `(device/set-device! ~(:device opts)))
               ;; Push an introspection frame onto the per-thread stack.
               ;; `binding` unwinds automatically — no explicit pop needed.
               (binding [*backend-device-stack*
                         (conj *backend-device-stack*
                               {:backend (device/get-active-backend)
                                :device  (device/get-device)})
                         *within-arrayfire?* true]
                 (with-open [~arena-sym (bmem/open-arena ~arena-type)]
                   (binding [bmem/*af-arena* ~arena-sym]
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
         (binding [*within-arrayfire?* true]
           (with-open [~arena-sym (bmem/open-arena ~arena-type)]
             (binding [bmem/*af-arena* ~arena-sym]
               (stack-resource-context
                (let [~result-sym (do ~@body)]
                  (device/sync!)
                  (result-convert ~converter ~result-sym))))))))))

;;;
;;; Helper functions
;;;
(defn normalize-dims
  [dims]
  (cond
    (nil? dims) []                       ;; scalar
    (number? dims) [dims]                ;; 1D
    (sequential? dims) (vec dims)
    :else
    (throw (ex-info "Invalid dims" {:dims dims}))))

(defn infer-shape
  "Infer the shape of a nested Clojure data structure (vector of vectors, etc.).
   Used for inferring array dimensions when creating an AFArray from Clojure data.

   Parameters:
   - data: nested Clojure data structure (e.g. vector of vectors)

   Returns:
   Vector of dimensions inferred from the structure.

   Example:
   (infer-shape [[1 2 3] [4 5 6]]) ; => [2 3]"
  [data]
  (cond
    (number? data) []
    (sequential? data)
    (let [len (count data)]
      (if (sequential? (first data))
        (into [len] (infer-shape (first data)))
        [len]))
    :else
    (throw (ex-info "Cannot infer shape" {:data data}))))

;;;
;;; Array creation
;;;
(defn create-array
  "Create an ArrayFire array from a Clojure vector of values.
   Values are copied to a double-array and the array is created via the integration layer.

   Parameters:
   - values: vector of numeric values (doubles)
   - dims: vector specifying the dimensions of the array (e.g. [] for scalar, [2 3] for a 2x3 array)
   - dtype: (optional) keyword specifying the ArrayFire data type
              (e.g. :f32, :f64, :s32, etc.). Defaults to :f64 (double).

   Returns:
   AFArray instance.

   Example:
   (create-array [1.0 2.0 3.0 4.0] [2 2]) ; creates a 2x2 array"
  ([values dims]
   (create-array values dims :f64))
  ([values dims dtype]
   (assert-within-arrayfire! "create-array")
   (array/create-array (double-array (flatten values)) dims (defs/dtype-kw->const dtype))))

(defn create-constant
  "Create an ArrayFire array filled with a constant value.

   Parameters:
   - value: numeric value to fill the array with
   - dims: vector specifying the dimensions of the array (e.g. [] for scalar, [2 3] for a 2x3 array)
   - dtype: (optional) keyword specifying the ArrayFire data type
              (e.g. :f32, :f64, :s32, etc.). Defaults to :f64 (double).

   Returns:
   AFArray instance.

   Example:
   (constant 42.0 [2 3]) ; creates a 2x3 array filled with 42.0"
  ^AFArray
  ([value]
   (create-constant value nil :f64))
  ([value dims]
   (create-constant value dims :f64))
  ([value dims dtype]
   (assert-within-arrayfire! "constant")
   (data/constant value (normalize-dims dims) (defs/resolve-dtype dtype))))

(defn create-zeros
  "Create an ArrayFire array filled with zeros.

   Parameters:
   - dims: vector specifying the dimensions of the array (e.g. [] for scalar, [2 3] for a 2x3 array)
   - dtype: (optional) keyword specifying the ArrayFire data type
              (e.g. :f32, :f64, :s32, etc.). Defaults to :f64 (double).

   Returns:
   AFArray instance.

   Example:
   (zeros [2 3]) ; creates a 2x3 array filled with zeros"
  ^AFArray
  ([]
   (create-zeros nil :f64))
  ([dims]
   (create-zeros dims :f64))
  ([dims dtype]
   (create-constant 0 dims dtype)))

(defn create-ones
  "Create an ArrayFire array filled with ones.

   Parameters:
   - dims: vector specifying the dimensions of the array (e.g. [] for scalar, [2 3] for a 2x3 array)
   - dtype: (optional) keyword specifying the ArrayFire data type
              (e.g. :f32, :f64, :s32, etc.). Defaults to :f64 (double).

   Returns:
   AFArray instance.

   Example:
   (ones [2 3]) ; creates a 2x3 array filled with ones"
  ^AFArray
  ([]
   (create-ones nil :f64))
  ([dims]
   (create-ones dims :f64))
  ([dims dtype]
   (create-constant 1 dims dtype)))

(defn create-range
  "Create an ArrayFire array filled with a range of values from 0 to n-1.

   Parameters:
   - n: number of elements in the range (if a number) or vector of dimensions (if sequential)
   - dtype: (optional) keyword specifying the ArrayFire data type
              (e.g. :f32, :f64, :s32, etc.). Defaults to :f64 (double).

   Returns:
   AFArray instance.

   Example:
   (create-range 5) ; creates a 1D array with values [0.0, 1.0, 2.0, 3.0, 4.0]"
  ^AFArray
  ([n]
   (create-range n :f64))
  ([n dtype]
   (assert-within-arrayfire! "create-range")
   (let [dtype-const (defs/resolve-dtype dtype)]
     (cond
       (number? n)
       (data/range n dtype-const)

       (sequential? n)
       (data/range (normalize-dims n) dtype-const)

       :else
       (throw (ex-info "Invalid range argument" {:n n}))))))

(defn create-uniform-random
  "Create an ArrayFire array filled with uniformly distributed random values.

   Parameters:
   - dims: vector specifying the dimensions of the array (e.g. [] for scalar, [2 3] for a 2x3 array)
   - dtype: (optional) keyword specifying the ArrayFire data type
              (e.g. :f32, :f64, :s32, etc.). Defaults to :f64 (double).

   Returns:
   AFArray instance.

   Example:
   (create-uniform-random [2 3]) ; creates a 2x3 array filled with uniform random values"
  ^AFArray
  ([]
   (create-uniform-random nil :f64))
  ([dims]
   (create-uniform-random dims :f64))
  ([dims dtype]
   (assert-within-arrayfire! "create-uniform-random")
   (random/randu (normalize-dims dims)
                 (defs/resolve-dtype dtype))))

(defn create-normal-random
  "Create an ArrayFire array filled with normally distributed random values.

   Parameters:
   - dims: vector specifying the dimensions of the array (e.g. [] for scalar, [2 3] for a 2x3 array)
   - dtype: (optional) keyword specifying the ArrayFire data type
              (e.g. :f32, :f64, :s32, etc.). Defaults to :f64 (double).

   Returns:
   AFArray instance.

   Example:
   (create-normal-random [2 3]) ; creates a 2x3 array filled with normal random values"
  ^AFArray
  ([]
   (create-normal-random nil :f64))
  ([dims]
   (create-normal-random dims :f64))
  ([dims dtype]
   (assert-within-arrayfire! "create-normal-random")
   (random/randn (normalize-dims dims)
                 (defs/resolve-dtype dtype))))


(defn create-zeros-like
  "Create an ArrayFire array filled with zeros, with the same shape and dtype as the given array.

   Parameters:
   - arr: AFArray instance to infer shape and dtype from
   - dtype: (optional) keyword specifying the ArrayFire data type
              (e.g. :f32, :f64, :s32, etc.). If not provided, uses the dtype of `arr`.

   Returns:
   AFArray instance with the same shape and dtype as `arr`, filled with zeros.

   Example:
   (create-zeros-like my-array) ; creates a new array with the same shape and dtype as my-array"
  ([arr]
   (create-zeros-like arr nil))
  ([arr dtype]
   (assert-within-arrayfire! "create-zeros-like")
   (let [shape (array/get-dims arr)
         dtype (or dtype (array/get-type arr))]
     (create-zeros shape dtype))))

(defn create-normal-random-like
  "Create an ArrayFire array filled with normally distributed random values, with the same shape and dtype as the given array.

   Parameters:
   - arr: AFArray instance to infer shape and dtype from
   - dtype: (optional) keyword specifying the ArrayFire data type
              (e.g. :f32, :f64, :s32, etc.). If not provided, uses the dtype of `arr`.

   Returns:
   AFArray instance with the same shape and dtype as `arr`, filled with normal random values.

   Example:
   (create-normal-random-like my-array) ; creates a new array with the same shape and dtype as my-array"
  ([arr]
   (create-normal-random-like arr nil))
  ([arr dtype]
   (assert-within-arrayfire! "create-normal-random-like")
   (let [shape (array/get-dims arr)
         dtype (or dtype (array/get-type arr))]
     (create-normal-random shape dtype))))


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
  ;; array/create-array returns an AFArray, which result-convert picks up.
  (with-arrayfire {:backend    :cpu
                   :converter-fn vec-converter}
    (let [data (double-array [1.0 2.0 3.0 4.0 5.0 6.0])]
      (create-array data [2 3] :f64)))

  ;
  )

