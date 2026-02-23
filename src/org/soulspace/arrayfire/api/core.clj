(ns org.soulspace.arrayfire.api.core
  "Idiomatic Clojure core API for arrayfire-clj.
  
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
  (:refer-clojure :exclude [+ - * / abs mod rem range min max sort flatten not cast])
  (:require [clojure.math]
            [tech.v3.resource :refer [stack-resource-context]]
            [org.soulspace.arrayfire.integration.base.definitions :as defs]
            [org.soulspace.arrayfire.integration.base.memory :as bmem]
            [org.soulspace.arrayfire.integration.dtype-next.dtype-next :as dtype-next]
            [org.soulspace.arrayfire.integration.unified-api.array :as array]
            [org.soulspace.arrayfire.integration.unified-api.arith :as arith]
            [org.soulspace.arrayfire.integration.unified-api.algorithm :as algo]
            [org.soulspace.arrayfire.integration.unified-api.blas :as blas]
            [org.soulspace.arrayfire.integration.unified-api.complex :as complex]
            [org.soulspace.arrayfire.integration.unified-api.memory :as uamem]
            [org.soulspace.arrayfire.integration.unified-api.device :as device]
            [org.soulspace.arrayfire.integration.unified-api.data :as data]
            [org.soulspace.arrayfire.integration.unified-api.index :as index]
            [org.soulspace.arrayfire.integration.unified-api.random :as random]
            [org.soulspace.arrayfire.integration.unified-api.util :as util])
  (:import (org.soulspace.arrayfire.integration.base.resource AFArray)))

;;;
;;; AFArray conversion
;;; 
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
  "Default converter for AFArray → dtype-next native buffer, preserving dtype.
   Uses the integration layer to query array metadata from ArrayFire.

   Not supported for complex types (C32, C64): dtype-next has no complex
   dtype. Use `->value` or `to-host` instead for complex arrays.

   Parameters:
   - arr: AFArray instance

   Returns:
   dtype-next native buffer with the array data.

   Throws:
   ExceptionInfo when arr has a complex element type (C32 or C64)."
  [^AFArray arr]
  (let [n       (array/get-elements arr)
        af-type (array/get-type arr)
        complex-types #{(defs/resolve-dtype :c32) (defs/resolve-dtype :c64)}]
    (when (complex-types af-type)
      (throw (ex-info (str "->native-buffer does not support complex arrays (C32/C64). "
                           "Use ->value or to-host instead.")
                      {:af-type af-type})))
    (let [dtype-kw (get dtype-next/af-dtype->dtype-next-kw af-type :float64)]
      (dtype-next/to-native-buffer arr dtype-kw n))))

(defn ->value
  "Converter that converts an AFArray to a Clojure scalar or vector based value.
   It uses the effective dimensions of the array (trailing size-1 dims are stripped)
   to reshape the element sequence into nested vectors.

   For real/integer/boolean dtypes, elements are JVM primitives (boxed to numbers).
   For complex dtypes (C32/C64), each element is a [real imag] vector.

   Parameters:
   - arr: AFArray instance

   Returns:
   Clojure scalar or vector based value with the array data.
   1D arrays return a flat vector, 2D a vector of column vectors, etc."
  [^AFArray arr]
  (let [;; ArrayFire always returns 4 dims, padding unused dims with 1.
        ;; Strip trailing 1s to get the effective logical shape.
        all-dims       (array/get-dims arr)
        effective-dims (vec (reverse (drop-while #(= 1 %) (reverse all-dims))))
        data           (array/array->host arr)]
    (loop [d  effective-dims
           ds data]
      (if (empty? d)
        (first ds)
        (let [size      (first d)
              rest-dims (rest d)]
          (recur rest-dims
                 (mapv #(vec (take size %))
                       (partition-all size ds))))))))

;;;
;;; Resource management and initialization
;;;
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

(def ^:dynamic *random-engine*
  "The random engine handle bound by `with-random-engine`, or `nil` when no
   custom engine is active (ArrayFire's default engine is used in that case).

   `random-uniform` and `random-normal` inspect this var at call-time: when
   non-nil they delegate to the engine-qualified integration functions; when nil
   they fall back to `randu`/`randn` (default engine).

   Do not set this var directly — use the `with-random-engine` macro."
  nil)

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

;;
;; with-arrayfire execution region
;;
(defmacro with-arrayfire
  "Execute body within a deterministic GPU compute region.

  Establishes:
  - ArrayFire initialization (once)
  - Optional backend/device switching (serialized via lock)
  - Optional manual JIT evaluation mode
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
  - :manual-eval  boolean. When `true`, disables ArrayFire's automatic JIT
                  kernel evaluation for the duration of the body — all deferred
                  computations must be triggered explicitly via `eval!` or
                  `eval-multiple!`. The previous flag value is restored in a
                  `finally` block so the caller's evaluation mode is always
                  preserved even if an exception is thrown.

  Returns:
  The result of evaluating body, with all AFArray instances converted to host data.

  Examples:
    ;; Basic usage — results auto-converted
    (with-arrayfire
      (let [a (array [1.0 2.0 3.0 4.0] [2 2])]
        (to-host a 4)))

    ;; With explicit backend and device selection
    (with-arrayfire {:backend :cuda :device 0}
      ...)

    ;; Multi-threaded body — use shared arena
    (with-arrayfire {:arena-type :shared}
      (let [f (future (array [1.0 2.0] [2]))]
        (vec (to-host @f 2))))

    ;; Skip auto-conversion when result is already host data
    (with-arrayfire {:converter-fn identity}
      ...)

    ;; Defer all JIT evaluation until explicit eval! calls
    (with-arrayfire {:manual-eval true}
      (let [a (+ x y)
            b (* x y)]
        (eval-multiple! [a b])
        ...))"
  [& args]
  (let [known-opts       #{:backend :device :converter-fn :arena-type :manual-eval}
        opts-map?        (and (map? (first args))
                              (or (empty? (first args))
                                  (some known-opts (keys (first args)))))
        [opts body]      (if opts-map?
                           [(first args) (rest args)]
                           [{} args])
        converter        (or (:converter-fn opts) `->native-buffer)
        arena-type       (get opts :arena-type :confined)
        has-backend?     (contains? opts :backend)
        has-device?      (contains? opts :device)
        has-manual-eval? (contains? opts :manual-eval)
        prev-backend     (gensym "prev-backend")
        prev-device      (gensym "prev-device")
        prev-manual-eval (gensym "prev-manual-eval")
        arena-sym        (gensym "arena")
        result-sym       (gensym "result")]
    (if (or has-backend? has-device?)
      ;; With backend/device switching — needs lock
      `(do
         (device/ensure-af-init!)
         (locking device/backend-lock
           (let [~prev-backend (device/get-active-backend)
                 ~prev-device  (device/get-device)
                 ~@(when has-manual-eval?
                     [prev-manual-eval `(device/get-manual-eval-flag)])]
             (try
               ~(when has-backend?
                  `(device/set-backend! (defs/resolve-backend ~(:backend opts))))
               ~(when has-device?
                  `(device/set-device! ~(:device opts)))
               ~(when has-manual-eval?
                  `(device/set-manual-eval-flag! ~(:manual-eval opts)))
               ;; Push an introspection frame onto the per-thread stack.
               ;; `binding` unwinds automatically — no explicit pop needed.
               (binding [device/*backend-device-stack*
                         (conj device/*backend-device-stack*
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
                 ~(when has-manual-eval?
                    `(device/set-manual-eval-flag! ~prev-manual-eval))
                 ~(when has-device?
                    `(device/set-device! ~prev-device))
                 ~(when has-backend?
                    `(device/set-backend! ~prev-backend)))))))
      ;; No backend/device switching — no lock needed
      (if has-manual-eval?
        ;; manual-eval requires save/restore via try/finally
        `(do
           (device/ensure-af-init!)
           (let [~prev-manual-eval (device/get-manual-eval-flag)]
             (try
               (device/set-manual-eval-flag! ~(:manual-eval opts))
               (binding [*within-arrayfire?* true]
                 (with-open [~arena-sym (bmem/open-arena ~arena-type)]
                   (binding [bmem/*af-arena* ~arena-sym]
                     (stack-resource-context
                      (let [~result-sym (do ~@body)]
                        (device/sync!)
                        (result-convert ~converter ~result-sym))))))
               (finally
                 (device/set-manual-eval-flag! ~prev-manual-eval)))))
        ;; No backend/device/manual-eval switching — no lock or try/finally needed
        `(do
           (device/ensure-af-init!)
           (binding [*within-arrayfire?* true]
             (with-open [~arena-sym (bmem/open-arena ~arena-type)]
               (binding [bmem/*af-arena* ~arena-sym]
                 (stack-resource-context
                  (let [~result-sym (do ~@body)]
                    (device/sync!)
                    (result-convert ~converter ~result-sym)))))))))))


;;
;; with-random-engine scoped random engine region
;;
(defmacro with-random-engine
  "Execute body within a scoped random engine region.

  Creates a custom ArrayFire random engine for the duration of the body,
  binds it to `*random-engine*`, and releases it in a `finally` block.
  `random-uniform` and `random-normal` called inside the body will
  automatically use this engine instead of ArrayFire's default engine.

  Must be called within an active `with-arrayfire` region.

  Parameters:
  - opts  — map of options (required):
      - :type   keyword: :philox (default), :threefry, or :mersenne
      - :seed   long integer seed value (default 0)
  - body  — forms to execute within the engine scope

  Returns:
  The result of evaluating body (no automatic conversion — this is an inner
  utility scope; rely on the enclosing `with-arrayfire` for conversion).

  Examples:
    ;; Reproducible uniform random array with Philox engine
    (with-arrayfire {:backend :cpu}
      (with-random-engine {:type :philox :seed 42}
        (->value (random-uniform [4] :f32))))

    ;; Two runs with the same seed produce identical results
    (with-arrayfire {:backend :cpu}
      (let [run1 (with-random-engine {:seed 7}
                   (->value (random-uniform [3] :f32)))
            run2 (with-random-engine {:seed 7}
                   (->value (random-uniform [3] :f32)))]
        (= run1 run2)))  ;; => true"
  [opts & body]
  (let [engine-type (get opts :type :philox)
        seed        (get opts :seed 0)
        engine-sym  (gensym "engine")]
    `(do
       (assert-within-arrayfire! "with-random-engine")
       (let [~engine-sym (random/create-engine ~engine-type ~seed)]
         (try
           (binding [*random-engine* ~engine-sym]
             ~@body)
           (finally
             (random/release-engine! ~engine-sym)))))))

;;;
;;; Helper functions
;;;

; TODO improve datatype handling
(defn scalar->array
  "Lift a JVM Number to a 1-element AFArray of the given ArrayFire dtype constant.
   Used together with batch=true to broadcast a scalar against an array without
   allocating a full-sized constant array — ArrayFire handles the expansion natively.
   
   Parameters:
   - x: JVM Number to lift
   - dtype: ArrayFire dtype constant (integer)
   
   Returns:
   AFArray instance containing the scalar value, with the specified dtype.
   
   Example:
   (scalar->array 3.14 (defs/resolve-dtype :f32)) ; creates a 1-element array with value 3.14 as float32"
  ^AFArray
  [x dtype]
  (data/constant (double x) [1] dtype))

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

(defn normalize-dims
  "Normalize dimensions input to a vector of integers. Accepts nil (scalar), number (1D), or sequential (vector of dims).

   Parameters:
   - dims: nil, number, or sequential specifying dimensions

   Returns:
   Vector of dimensions. Examples:
   - nil → []
   - 5 → [5]
   - [2 3] → [2 3]

   Throws:
   ExceptionInfo if dims is not nil, a number, or sequential."
  [dims]
  (cond
    (nil? dims) []                       ;; scalar
    (number? dims) [dims]                ;; 1D
    (sequential? dims) (vec dims)
    :else
    (throw (ex-info "Invalid dims" {:dims dims}))))

(defn- normalize-range-spec
  "Coerce a range spec to [begin end step] as doubles.

   Accepted forms:
   - nil              → [0.0 -1.0 1.0]  (select all, -1 = ArrayFire 'last' sentinel)
   - n (Number)       → [n n 1.0]       (single element at index n)
   - [start end]      → [start end 1.0] (inclusive range, step 1)
   - [start end step] → as given"
  [spec]
  (cond
    (nil? spec)
    [0.0 -1.0 1.0]

    (number? spec)
    [(double spec) (double spec) 1.0]

    (and (sequential? spec) (= 2 (count spec)))
    [(double (first spec)) (double (second spec)) 1.0]

    (and (sequential? spec) (= 3 (count spec)))
    [(double (first spec)) (double (second spec)) (double (nth spec 2))]

    :else
    (throw (ex-info "Invalid range spec — expected nil, number, [start end] or [start end step]"
                    {:spec spec}))))

(defn- configure-indexers!
  "Set seq-param indexers for all 4 dimensions.
   Range specs beyond what is given default to nil (= 'all elements').
   Always returns 4 (ArrayFire requires all dims initialised when using index-gen)."
  [indexers range-specs]
  (dotimes [dim 4]
    (let [spec             (nth range-specs dim nil)
          [begin end step] (normalize-range-spec spec)]
      (index/set-seq-param-indexer! indexers begin end step dim false)))
  4)

(defn- range-map->range-specs
  "Expand a map {:rows … :cols … :depth … :batch …} to an ordered vector of
   specs for dims 0–3. Dimensions between the first and last explicitly set
   that are missing from the map default to nil (= 'all elements')."
  [range-map]
  (let [dim-keys [:rows :cols :depth :batch]
        max-dim  (reduce (fn [acc [i k]]
                           (if (contains? range-map k) i acc))
                         -1
                         (map-indexed vector dim-keys))]
    (if (neg? max-dim)
      []
      (mapv #(get range-map %) (take (inc max-dim) dim-keys)))))

;;;
;;; Array creation
;;;
(defn array
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
   (array [1.0 2.0 3.0 4.0] [2 2]) ; creates a 2x2 array"
  ^AFArray
  ([values dims]
   (array values dims :f64))
  ([values dims dtype]
   (assert-within-arrayfire! "array")
   ;; `flatten` only works on sequential? Clojure collections; Java arrays (e.g. double[])
   ;; are seqable but NOT sequential?, so `flatten` silently returns [].
   ;; Use `seq` instead for arrays, `flatten` only for nested Clojure sequences.
   (let [double-arr (cond
                      (sequential? values) (double-array (clojure.core/flatten values))
                      (seqable? values)    (double-array (seq values))
                      :else                values)]
     (array/create-array double-arr dims (defs/dtype-kw->const dtype)))))

(defn constant
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
   (constant value nil :f64))
  ([value dims]
   (constant value dims :f64))
  ([value dims dtype]
   (assert-within-arrayfire! "constant")
   (data/constant value (normalize-dims dims) (defs/resolve-dtype dtype))))

(defn constant-complex
  "Create an array filled with a complex constant value.

   Parameters:
   - real:  Real part of the constant (double)
   - imag:  Imaginary part of the constant (double)
   - dims:  Vector of dimensions, e.g. [4 4]
   - dtype: Complex element type keyword :c32 or :c64 (default :c32)

   Returns:
   AFArray filled with the complex constant `real + imag*i`.
   Requires an active `with-arrayfire` region.

   Example:
   (constant-complex 0.0 1.0 [4])          ; [0+1i 0+1i 0+1i 0+1i] as c32
   (constant-complex 1.0 0.0 [2 2] :c64)   ; 2×2 real-valued complex array"
  (^AFArray [real imag dims]
   (constant-complex real imag dims :c32))
  (^AFArray [real imag dims dtype]
   (assert-within-arrayfire! "constant-complex")
   (data/constant-complex real imag (normalize-dims dims) (defs/resolve-dtype dtype))))

(defn zeros
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
   (zeros nil :f64))
  ([dims]
   (zeros dims :f64))
  ([dims dtype]
   (constant 0 dims dtype)))

(defn ones
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
   (ones nil :f64))
  ([dims]
   (ones dims :f64))
  ([dims dtype]
   (constant 1 dims dtype)))

(defn range
  "Create an ArrayFire array filled with a range of values from 0 to n-1.

   Parameters:
   - n: number of elements in the range (if a number) or vector of dimensions (if sequential)
   - dtype: (optional) keyword specifying the ArrayFire data type
              (e.g. :f32, :f64, :s32, etc.). Defaults to :f64 (double).

   Returns:
   AFArray instance.

   Example:
   (range 5) ; creates a 1D array with values [0.0, 1.0, 2.0, 3.0, 4.0]"
  ^AFArray
  ([n]
   (range n :f64))
  ([n dtype]
   (assert-within-arrayfire! "range")
   (let [dtype-const (defs/resolve-dtype dtype)]
     (cond
       (number? n)
       (data/range n dtype-const)

       (sequential? n)
       (data/range (normalize-dims n) dtype-const)

       :else
       (throw (ex-info "Invalid range argument" {:n n}))))))

(defn iota
  "Create an array with sequential values tiled across multiple dimensions.

   Similar to `range` but supports independent per-dimension tiling via `tdims`.
   Generates a flattened sequence [0, n-1] reshaped according to `dims` and
   tiled `tdims` times along each dimension.

   Parameters:
   - dims:  Vector of output dimensions, e.g. [4 4]
   - tdims: Vector of tiling counts per dimension, e.g. [1 2]
   - dtype: Element type keyword (default :f32)

   Returns:
   AFArray with sequential values tiled as specified.
   Requires an active `with-arrayfire` region.

   Example:
   (iota [3] [2])      ; [0 1 2 0 1 2] — tiled twice along dim 0
   (iota [2 2] [1 2])  ; 2×4 tiled matrix"
  (^AFArray [dims tdims]
   (iota dims tdims :f32))
  (^AFArray [dims tdims dtype]
   (assert-within-arrayfire! "iota")
   (data/iota (normalize-dims dims) (normalize-dims tdims) (defs/resolve-dtype dtype))))

(defn random-uniform
  "Create an ArrayFire array filled with uniformly distributed random values.

   When called inside a `with-random-engine` scope, uses the bound custom
   engine (for reproducible independent streams). Outside any such scope,
   ArrayFire's default engine is used.

   Parameters:
   - dims: vector specifying the dimensions of the array (e.g. [] for scalar, [2 3] for a 2x3 array)
   - dtype: (optional) keyword specifying the ArrayFire data type
              (e.g. :f32, :f64, :s32, etc.). Defaults to :f64 (double).

   Returns:
   AFArray instance.

   Example:
   (random-uniform [2 3]) ; creates a 2x3 array filled with uniform random values"
  ^AFArray
  ([]
   (random-uniform nil :f64))
  ([dims]
   (random-uniform dims :f64))
  ([dims dtype]
   (assert-within-arrayfire! "random-uniform")
   (let [ndims (normalize-dims dims)
         dtype-const (defs/resolve-dtype dtype)]
     (if *random-engine*
       (random/random-uniform ndims dtype-const *random-engine*)
       (random/randu ndims dtype-const)))))

(defn random-normal
  "Create an ArrayFire array filled with normally distributed random values.

   When called inside a `with-random-engine` scope, uses the bound custom
   engine (for reproducible independent streams). Outside any such scope,
   ArrayFire's default engine is used.

   Parameters:
   - dims: vector specifying the dimensions of the array (e.g. [] for scalar, [2 3] for a 2x3 array)
   - dtype: (optional) keyword specifying the ArrayFire data type
              (e.g. :f32, :f64, :s32, etc.). Defaults to :f64 (double).

   Returns:
   AFArray instance.

   Example:
   (random-normal [2 3]) ; creates a 2x3 array filled with normal random values"
  ^AFArray
  ([]
   (random-normal nil :f64))
  ([dims]
   (random-normal dims :f64))
  ([dims dtype]
   (assert-within-arrayfire! "random-normal")
   (let [ndims (normalize-dims dims)
         dtype-const (defs/resolve-dtype dtype)]
     (if *random-engine*
       (random/random-normal ndims dtype-const *random-engine*)
       (random/randn ndims dtype-const)))))


(defn zeros-like
  "Create an ArrayFire array filled with zeros, with the same shape and dtype as the given array.

   Parameters:
   - arr: AFArray instance to infer shape and dtype from
   - dtype: (optional) keyword specifying the ArrayFire data type
              (e.g. :f32, :f64, :s32, etc.). If not provided, uses the dtype of `arr`.

   Returns:
   AFArray instance with the same shape and dtype as `arr`, filled with zeros.

   Example:
   (zeros-like my-array) ; creates a new array with the same shape and dtype as my-array"
  ^AFArray
  ([arr]
   (zeros-like arr nil))
  ([arr dtype]
   (assert-within-arrayfire! "zeros-like")
   (let [shape (array/get-dims arr)
         dtype (or dtype (array/get-type arr))]
     (zeros shape dtype))))

(defn random-uniform-like
  "Create an ArrayFire array filled with uniformly distributed random values, with the same shape and dtype as the given array.

   Parameters:
   - arr: AFArray instance to infer shape and dtype from
   - dtype: (optional) keyword specifying the ArrayFire data type
              (e.g. :f32, :f64, :s32, etc.). If not provided, uses the dtype of `arr`.

   Returns:
   AFArray instance with the same shape and dtype as `arr`, filled with uniform random values.

   Example:
   (random-uniform-like my-array) ; creates a new array with the same shape and dtype as my-array"
  ^AFArray
  ([arr]
   (random-uniform-like arr nil))
  ([arr dtype]
   (assert-within-arrayfire! "random-uniform-like")
   (let [shape (array/get-dims arr)
         dtype (or dtype (array/get-type arr))]
     (random-uniform shape dtype))))

(defn random-normal-like
  "Create an ArrayFire array filled with normally distributed random values, with the same shape and dtype as the given array.

   Parameters:
   - arr: AFArray instance to infer shape and dtype from
   - dtype: (optional) keyword specifying the ArrayFire data type
              (e.g. :f32, :f64, :s32, etc.). If not provided, uses the dtype of `arr`.

   Returns:
   AFArray instance with the same shape and dtype as `arr`, filled with normal random values.

   Example:
   (random-normal-like my-array) ; creates a new array with the same shape and dtype as my-array"
  ^AFArray
  ([arr]
   (random-normal-like arr nil))
  ([arr dtype]
   (assert-within-arrayfire! "random-normal-like")
   (let [shape (array/get-dims arr)
         dtype (or dtype (array/get-type arr))]
     (random-normal shape dtype))))

(defn identity-matrix
  "Create an identity matrix of the given size and dtype.

   Parameters:
   - n: Number of rows and columns (square matrix size)
   - dtype: dtype keyword (default :f64)

   Returns:
   n×n identity AFArray.
   Requires an active `with-arrayfire` region.

   Example:
   (identity-matrix 3)       ; 3×3 float64 identity matrix
   (identity-matrix 4 :f32)  ; 4×4 float32 identity matrix"
  (^AFArray [n]
   (identity-matrix n :f64))
  (^AFArray [n dtype]
   (assert-within-arrayfire! "identity-matrix")
   (data/identity [n n] (defs/resolve-dtype dtype))))

(defn diagonal
  "Create a diagonal matrix from a 1-D array.

   Parameters:
   - arr: 1-D input AFArray of values to place on the diagonal
   - num: Diagonal offset (default 0 = main diagonal; positive = above, negative = below)

   Returns:
   Square AFArray diagonal matrix.
   Requires an active `with-arrayfire` region.

   Example:
   (diagonal (array [1.0 2.0 3.0] [3]))       ; 3×3 diagonal matrix
   (diagonal (array [1.0 2.0 3.0] [3]) 1)     ; 3+1×3+1 matrix, offset +1 diagonal"
  (^AFArray [^AFArray arr]
   (assert-within-arrayfire! "diagonal")
   (data/diag-create arr 0))
  (^AFArray [^AFArray arr num]
   (assert-within-arrayfire! "diagonal")
   (data/diag-create arr num)))

(defn ones-like
  "Array of ones with same shape and dtype as `arr`.

   Parameters:
   - arr: Reference AFArray
   - dtype: Optional override dtype keyword

   Returns:
   AFArray filled with ones.
   Requires an active `with-arrayfire` region.

   Example:
   (ones-like arr)        ; ones matching arr
   (ones-like arr :f32)   ; ones in f32"
  (^AFArray [^AFArray arr]
   (ones-like arr nil))
  (^AFArray [^AFArray arr dtype]
   (assert-within-arrayfire! "ones-like")
   (let [dims (array/get-dims arr)
         dtype-const (if dtype (defs/resolve-dtype dtype) (array/get-type arr))]
     (data/constant 1.0 (vec dims) dtype-const))))

(defn constant-like
  "Array filled with `value` having the same shape and dtype as `arr`.

   Parameters:
   - arr: Reference AFArray
   - value: Fill value (Number)
   - dtype: Optional override dtype keyword

   Returns:
   AFArray filled with `value`.
   Requires an active `with-arrayfire` region.

   Example:
   (constant-like arr 3.14)       ; same shape, same dtype, all 3.14
   (constant-like arr 0.0 :f32)   ; same shape, f32 dtype, all 0.0"
  (^AFArray [^AFArray arr value]
   (constant-like arr value nil))
  (^AFArray [^AFArray arr value dtype]
   (assert-within-arrayfire! "constant-like")
   (let [dims (array/get-dims arr)
         dtype-const (if dtype (defs/resolve-dtype dtype) (array/get-type arr))]
     (data/constant (double value) (vec dims) dtype-const))))

;;;
;;; Array information and metadata
;;;
(defn datatype
  "Get the dtype of an AFArray as a keyword.

   Parameters:
   - arr: AFArray instance

   Returns:
   Keyword representing the dtype (e.g. :f32, :f64, :s32, etc.).

   Example:
   (datatype my-array) ; => :f64"
  [^AFArray arr]
  (assert-within-arrayfire! "datatype")
  (get defs/dtype-const->kw (array/get-type arr) :unknown))

(defn shape
  "Get the effective shape (dimensions) of an AFArray, stripping trailing size-1 dimensions.

   ArrayFire internally always stores 4 dimensions, padding unused dimensions with 1.
   This function strips those trailing ones, returning the logical shape.
   Use `raw-shape` to get the padded 4-element dimension vector.

   Parameters:
   - arr: AFArray instance

   Returns:
   Vector of effective dimensions. A 2×3 array returns [2 3].
   A 1-D array of length 5 returns [5]. A scalar returns [].

   Example:
   (shape my-array) ; => [2 3] for a 2×3 matrix"
  [^AFArray arr]
  (assert-within-arrayfire! "shape")
  (let [all-dims (array/get-dims arr)]
    (vec (reverse (drop-while #(= 1 %) (reverse all-dims))))))

(defn raw-shape
  "Get the raw ArrayFire dimension vector of an AFArray (always 4 elements, padded with 1s).

   ArrayFire internally uses 4-dimensional indexing. Dimensions beyond the logical
   rank of the array are set to 1. Use `shape` for the logical (stripped) shape.

   Parameters:
   - arr: AFArray instance

   Returns:
   Vector of 4 integers, e.g. [2 3 1 1] for a 2×3 matrix.

   Example:
   (raw-shape my-array) ; => [2 3 1 1] for a 2×3 matrix"
  [^AFArray arr]
  (assert-within-arrayfire! "raw-shape")
  (vec (array/get-dims arr)))

(defn size
  "Get the size of an AFArray along a specific dimension.

   Parameters:
   - arr: AFArray instance
   - dim: integer dimension index (0-based)

   Returns:
   Integer size along the specified dimension. For example, for a 2x3 array, (size arr 0) returns 2 and (size arr 1) returns 3.

   Example:
   (size my-array 0) ; => size along the first dimension"
  [^AFArray arr dim]
  (assert-within-arrayfire! "size")
  (nth (array/get-dims arr) dim ))

(defn ndim
  "Get the number of dimensions of an AFArray.

   Parameters:
   - arr: AFArray instance

   Returns:
   Integer representing the number of dimensions.
   For example, a 2-D array returns 2, a scalar returns 0.

   Example:
   (ndim my-array) ; => 2 for a 2-D array"
  [^AFArray arr]
  (assert-within-arrayfire! "ndim")
  (array/get-numdims arr))

(defn element-count
  "Get the total number of elements in an AFArray.

   Parameters:
   - arr: AFArray instance

   Returns:
   Integer count of total elements in the array.

   Example:
   (element-count my-array) ; => 6 for a 2x3 array"
  [^AFArray arr]
  (assert-within-arrayfire! "element-count")
  (array/get-elements arr))

;;;
;;; Array indexing and manipulation
;;;

(defn slice
  "Extract a subarray using range-based (sequence) indexing.

   Supports two calling forms:

   **Vector form** — positional range per dimension:
     (slice arr range-specs)

   **Map form** — named ranges for named dimensions:
     (slice arr {:rows … :cols … :depth … :batch …})

   Each range spec can be:
   - `nil`              — all elements along that dimension
   - `n` (integer)      — single element at index n
   - `[start end]`      — indices start..end inclusive, step 1
   - `[start end step]` — with given step (-1 as end → last element)

   Parameters:
   - arr:         AFArray to index
   - range-specs: vector of range specs, one per dimension; OR a map with
                  keys :rows (dim 0), :cols (dim 1), :depth (dim 2), :batch (dim 3)

   Returns:
   Subarray AFArray.

   Examples:
   ;; Select all columns of row 0
   (slice arr [0 nil])

   ;; Rows 2–5, columns 1–3
   (slice arr [[2 5] [1 3]])

   ;; Every other row, all columns
   (slice arr [[0 -1 2] nil])

   ;; Map form — rows 0–4, all columns
   (slice arr {:rows [0 4]})"
  ^AFArray
  [^AFArray arr range-specs]
  (assert-within-arrayfire! "slice")
  (let [specs    (if (map? range-specs)
                   (range-map->range-specs range-specs)
                   (vec range-specs))
        indexers (index/create-indexers)]
    (try
      (let [ndims (configure-indexers! indexers specs)]
        (index/index-gen arr indexers ndims))
      (finally
        (index/release-indexers! indexers)))))

(defn at
  "Select a single element by exact indices per dimension.

   Parameters:
   - arr:     AFArray to index
   - indices: vector of integer indices, one per dimension

   Returns:
   A 1-element AFArray at the specified position.

   Example:
   (at arr [1 2])  ; element at row 1, column 2"
  ^AFArray
  [^AFArray arr indices]
  (assert-within-arrayfire! "at")
  (let [specs    (mapv (fn [i] [(double i) (double i) 1.0]) indices)
        indexers (index/create-indexers)]
    (try
      (dotimes [dim 4]
        (let [[begin end step] (if (< dim (count specs))
                                 (nth specs dim)
                                 [0.0 -1.0 1.0])]
          (index/set-seq-param-indexer! indexers begin end step dim false)))
      (index/index-gen arr indexers 4)
      (finally
        (index/release-indexers! indexers)))))

(defn row
  "Select a single row (dimension 0) of a 2-D (or higher) array.

   Parameters:
   - arr: AFArray
   - i:   row index (integer)

   Returns:
   AFArray with row i selected (size 1 along dim 0).

   Example:
   (row arr 0)  ; first row"
  ^AFArray
  [^AFArray arr i]
  (assert-within-arrayfire! "row")
  (slice arr [(double i) nil]))

(defn col
  "Select a single column (dimension 1) of a 2-D (or higher) array.

   Parameters:
   - arr: AFArray
   - j:   column index (integer)

   Returns:
   AFArray with column j selected (size 1 along dim 1).

   Example:
   (col arr 2)  ; third column"
  ^AFArray
  [^AFArray arr j]
  (assert-within-arrayfire! "col")
  (slice arr [nil (double j)]))

(defn rows
  "Select a contiguous range of rows (dimension 0).

   Parameters:
   - arr:   AFArray
   - start: first row index (integer, inclusive)
   - end:   last row index (integer, inclusive)

   Returns:
   AFArray with rows start..end.

   Example:
   (rows arr 2 5)  ; rows 2, 3, 4, 5"
  ^AFArray
  [^AFArray arr start end]
  (assert-within-arrayfire! "rows")
  (slice arr [[start end] nil]))

(defn cols
  "Select a contiguous range of columns (dimension 1).

   Parameters:
   - arr:   AFArray
   - start: first column index (integer, inclusive)
   - end:   last column index (integer, inclusive)

   Returns:
   AFArray with columns start..end.

   Example:
   (cols arr 1 3)  ; columns 1, 2, 3"
  ^AFArray
  [^AFArray arr start end]
  (assert-within-arrayfire! "cols")
  (slice arr [nil [start end]]))

(defn select
  "Fancy indexing: select elements by an integer index array along one dimension.

   Parameters:
   - arr:     AFArray to index
   - indices: Clojure vector of integer indices, or an AFArray of integer dtype
   - dim:     (optional) dimension along which to index, default 0

   Returns:
   AFArray with the selected elements.

   Examples:
   ;; Select elements at positions 0, 2, 4 along dim 0
   (select arr [0 2 4])

   ;; Select columns 1 and 3
   (select arr [1 3] 1)"
  (^AFArray [^AFArray arr indices]
   (select arr indices 0))
  (^AFArray [^AFArray arr indices dim]
   (assert-within-arrayfire! "select")
   (let [idx-arr (if (instance? AFArray indices)
                   indices
                   (array/create-array (int-array indices)
                                       [(count indices)]
                                       (defs/dtype-kw->const :s32)))]
     (index/lookup arr idx-arr (int dim)))))

(defn assoc-slice
  "Functional (copy-on-write) assignment: return a new array with values from
   `new-values` written into the region specified by `range-specs`.

   The original array is not modified — a new array is returned.
   Analogous to `clojure.core/assoc` for maps, but addressing array slices.

   Parameters:
   - arr:         AFArray (destination, not mutated)
   - range-specs: same format as `slice` (vector of range specs or map)
   - new-values:  AFArray of values to write into the selected region

   Returns:
   New AFArray with the slice replaced.

   Examples:
   ;; Zero out row 0 of a 4×3 array
   (assoc-slice arr [0 nil] (zeros [1 3]))

   ;; Replace column 2 with a constant
   (assoc-slice arr [nil 2] (constant 99.0 [4 1]))

   ;; Map form
   (assoc-slice arr {:rows [0 4]} new-block)"
  ^AFArray
  [^AFArray arr range-specs ^AFArray new-values]
  (assert-within-arrayfire! "assoc-slice")
  (let [specs        (if (map? range-specs)
                       (range-map->range-specs range-specs)
                       (vec range-specs))
        ;; af_assign_gen modifies lhs in-place; copy first to preserve functional semantics.
        arr-copy     (array/copy-array arr)
        ;; assign-gen requires ndims ≥ array ndim so all addressed dimensions
        ;; have properly initialised indexers.
        arr-ndim     (array/get-numdims arr)
        assign-ndims (clojure.core/max (count specs) arr-ndim)
        indexers     (index/create-indexers)]
    (try
      ;; Initialise all 4 indexers (avoid uninitialised memory).
      (configure-indexers! indexers specs)
      (index/assign-gen arr-copy indexers assign-ndims new-values)
      (finally
        (index/release-indexers! indexers)))))

;;;
;;; Array shape manipulation
;;;

(defn reshape
  "Reshape an array to different dimensions without changing its data.

   The total number of elements must remain the same.

   Parameters:
   - arr: Input AFArray
   - dims: New dimensions (vector, number, or nil for scalar)

   Returns:
   AFArray with new shape but same data.
   Requires an active `with-arrayfire` region.

   Example:
   (reshape (range 6) [2 3])  ; 1-D [0..5] → 2×3 matrix"
  ^AFArray
  [^AFArray arr dims]
  (assert-within-arrayfire! "reshape")
  (data/moddims arr (normalize-dims dims)))

(defn flatten
  "Flatten an array to a 1-D vector (all elements in column-major order).

   Parameters:
   - arr: Input AFArray

   Returns:
   1-D AFArray with all elements.
   Requires an active `with-arrayfire` region.

   Example:
   (flatten (array [[1 2] [3 4]] [2 2]))  ; => [1.0 3.0 2.0 4.0]  (column-major)"
  ^AFArray
  [^AFArray arr]
  (assert-within-arrayfire! "flatten")
  (data/flat arr))

(defn transpose
  "Transpose a 2-D (or higher) array., optionally computing the conjugate (Hermitian) transpose.

   Parameters:
   - arr: Input AFArray
   - conjugate?: If true, applies complex conjugation (Hermitian transpose). Default false.

   Returns:
   Transposed AFArray.
   Requires an active `with-arrayfire` region.

   Example:
   (transpose m)        ; regular transpose
   (transpose m true)   ; conjugate (Hermitian) transpose for complex arrays"
  (^AFArray [^AFArray arr]
   (transpose arr false))
  (^AFArray [^AFArray arr conjugate?]
   (assert-within-arrayfire! "transpose")
   (blas/transpose arr conjugate?)))

(defn join
  "Concatenate arrays along a dimension.

   Parameters:
   - dim: Dimension along which to concatenate (0 = rows, 1 = cols, etc.)
   - arrays: Two or more AFArrays to concatenate

   Returns:
   Concatenated AFArray.
   Requires an active `with-arrayfire` region.

   Example:
   (join 0 a b)        ; stack rows (vertical cat)
   (join 1 a b c)      ; stack cols  (horizontal cat)"
  ^AFArray
  [dim & arrays]
  (assert-within-arrayfire! "join")
  (case (clojure.core/count arrays)
    1 (first arrays)
    2 (data/join dim (first arrays) (second arrays))
    (data/join-many dim (vec arrays))))

(defn flip
  "Reverse elements along a dimension.

   Parameters:
   - arr: Input AFArray
   - dim: Dimension along which to flip (default 0)

   Returns:
   AFArray with elements reversed along the specified dimension.
   Requires an active `with-arrayfire` region.

   Example:
   (flip arr 0)  ; flip rows (vertical flip)
   (flip arr 1)  ; flip cols (horizontal flip)"
  (^AFArray [^AFArray arr]
   (flip arr 0))
  (^AFArray [^AFArray arr dim]
   (assert-within-arrayfire! "flip")
   (data/flip arr dim)))

(defn shift
  "Cyclically shift elements along dimensions.

   Parameters:
   - arr: Input AFArray
   - shifts: Vector of shift amounts per dimension (up to 4 dimensions)

   Returns:
   AFArray with elements cyclically shifted.
   Requires an active `with-arrayfire` region.

   Example:
   (shift arr [1 0])  ; shift rows by 1"
  ^AFArray
  [^AFArray arr shifts]
  (assert-within-arrayfire! "shift")
  (let [[s0 s1 s2 s3] (map int (concat shifts (repeat 0)))]
    (data/shift arr s0 s1 s2 s3)))

(defn tile
  "Tile (repeat) an array along each dimension.

   Parameters:
   - arr: Input AFArray
   - repeats: Vector of repetition counts per dimension

   Returns:
   Tiled AFArray.
   Requires an active `with-arrayfire` region.

   Example:
   (tile arr [2 3])  ; tile 2× vertically and 3× horizontally"
  ^AFArray
  [^AFArray arr repeats]
  (assert-within-arrayfire! "tile")
  (let [[r0 r1 r2 r3] (map int (concat repeats (repeat 1)))]
    (data/tile arr r0 r1 r2 r3)))

(defn reorder
  "Permute the dimensions of an array (generalised transpose).

   Parameters:
   - arr: Input AFArray
   - order: Vector of 4 dimension indices specifying the new order

   Returns:
   AFArray with permuted dimensions.
   Requires an active `with-arrayfire` region.

   Example:
   (reorder arr [1 0 2 3])  ; swap rows and cols (same as transpose for 2-D)"
  ^AFArray
  [^AFArray arr order]
  (assert-within-arrayfire! "reorder")
  (let [[d0 d1 d2 d3] (map int (concat order (clojure.core/range 4)))]
    (data/reorder arr d0 d1 d2 d3)))

; TODO rename to diagonal-of?
(defn get-diagonal
  "Extract the diagonal of a 2-D array as a 1-D vector.

   Parameters:
   - arr: 2-D input AFArray
   - num: Diagonal offset (default 0 = main diagonal; positive = above, negative = below)

   Returns:
   1-D AFArray of diagonal elements.
   Requires an active `with-arrayfire` region.

   Example:
   (get-diagonal my-matrix)       ; extract main diagonal
   (get-diagonal my-matrix 1)    ; extract superdiagonal"
  (^AFArray [^AFArray arr]
   (assert-within-arrayfire! "get-diagonal")
   (data/diag-extract arr 0))
  (^AFArray [^AFArray arr num]
   (assert-within-arrayfire! "get-diagonal")
   (data/diag-extract arr num)))

(defn lower-tri
  "Return the lower-triangular part of a matrix.

   Parameters:
   - arr: Input 2-D AFArray
   - is-unit-diag?: If true, the diagonal is set to 1; if false, original values are kept (default false)

   Returns:
   Lower-triangular AFArray.
   Requires an active `with-arrayfire` region.

   Example:
   (lower-tri m)       ; lower triangle with original diagonal
   (lower-tri m true)  ; lower triangle with unit diagonal"
  (^AFArray [^AFArray arr]
   (lower-tri arr false))
  (^AFArray [^AFArray arr is-unit-diag?]
   (assert-within-arrayfire! "lower-tri")
   (data/lower arr is-unit-diag?)))

(defn upper-tri
  "Return the upper-triangular part of a matrix.

   Parameters:
   - arr: Input 2-D AFArray
   - is-unit-diag?: If true, the diagonal is set to 1; if false, original values are kept (default false)

   Returns:
   Upper-triangular AFArray.
   Requires an active `with-arrayfire` region.

   Example:
   (upper-tri m)       ; upper triangle with original diagonal
   (upper-tri m true)  ; upper triangle with unit diagonal"
  (^AFArray [^AFArray arr]
   (upper-tri arr false))
  (^AFArray [^AFArray arr is-unit-diag?]
   (assert-within-arrayfire! "upper-tri")
   (data/upper arr is-unit-diag?)))

(defn pad
  "Pad an array with a border.

   Parameters:
   - arr: Input AFArray
   - begin-dims: Vector of beginning pad sizes per dimension
   - end-dims: Vector of ending pad sizes per dimension
   - pad-type: Padding type keyword (:zero, :symmetric, :periodic, :clamp-to-edge) — default :zero

   Returns:
   Padded AFArray.
   Requires an active `with-arrayfire` region.

   Example:
   (pad arr [1 1 0 0] [1 1 0 0] :zero)  ; pad 1 on each side of first two dims"
  (^AFArray [^AFArray arr begin-dims end-dims]
   (pad arr begin-dims end-dims :zero))
  (^AFArray [^AFArray arr begin-dims end-dims pad-type]
   (assert-within-arrayfire! "pad")
   (let [pad-kw->const {:zero 0 :symmetric 1 :periodic 2 :clamp-to-edge 3}
         pad-const (get pad-kw->const pad-type 0)
         [b0 b1 b2 b3] (map int (concat begin-dims (repeat 0)))
         [e0 e1 e2 e3] (map int (concat end-dims (repeat 0)))]
     (data/pad arr [b0 b1 b2 b3] [e0 e1 e2 e3] pad-const))))

;;;
;;; Reductions
;;;

(defn sum
  "Sum elements along a dimension (default: all dimensions → scalar).

   Parameters:
   - arr: Input AFArray
   - dim: Dimension to reduce along (omit to reduce all elements to a scalar)

   Returns:
   AFArray with summed values.
   Requires an active `with-arrayfire` region.

   Example:
   (sum arr)     ; total sum → scalar array
   (sum arr 0)   ; column-wise sums of a 2-D array"
  (^AFArray [^AFArray arr]
   (assert-within-arrayfire! "sum")
   (algo/sum (data/flat arr) 0))
  (^AFArray [^AFArray arr dim]
   (assert-within-arrayfire! "sum")
   (algo/sum arr dim)))

(defn product
  "Product of elements along a dimension (default: all dimensions → scalar).

   Parameters:
   - arr: Input AFArray
   - dim: Dimension to reduce along (omit to reduce all elements to a scalar)

   Returns:
   AFArray with product values.
   Requires an active `with-arrayfire` region.

   Example:
   (product arr)     ; total product → scalar array
   (product arr 0)   ; column-wise product of a 2-D array"
  (^AFArray [^AFArray arr]
   (assert-within-arrayfire! "product")
   (algo/product (data/flat arr) 0))
  (^AFArray [^AFArray arr dim]
   (assert-within-arrayfire! "product")
   (algo/product arr dim)))

(defn sum-nan
  "Sum elements along a dimension, replacing NaN with a specified value.

   Parameters:
   - arr:     Input AFArray
   - dim:     Dimension to reduce along
   - nan-val: Value to substitute for NaN before summing (default 0.0)

   Returns:
   AFArray with summed values.
   Requires an active `with-arrayfire` region.

   Example:
   (sum-nan arr 0)        ; sum along dim 0, NaN treated as 0.0
   (sum-nan arr 0 -1.0)   ; sum along dim 0, NaN → -1.0"
  (^AFArray [^AFArray arr dim]
   (sum-nan arr dim 0.0))
  (^AFArray [^AFArray arr dim nan-val]
   (assert-within-arrayfire! "sum-nan")
   (algo/sum-nan arr dim nan-val)))

(defn product-nan
  "Multiply elements along a dimension, replacing NaN with a specified value.

   Parameters:
   - arr:     Input AFArray
   - dim:     Dimension to reduce along
   - nan-val: Value to substitute for NaN before multiplying (default 1.0)

   Returns:
   AFArray with product values.
   Requires an active `with-arrayfire` region.

   Example:
   (product-nan arr 0)        ; product along dim 0, NaN treated as 1.0
   (product-nan arr 0 0.0)    ; product along dim 0, NaN → 0.0"
  (^AFArray [^AFArray arr dim]
   (product-nan arr dim 1.0))
  (^AFArray [^AFArray arr dim nan-val]
   (assert-within-arrayfire! "product-nan")
   (algo/product-nan arr dim nan-val)))

(defn min
  "Minimum of arrays along a dimension, element-wise, or scalar minimum.

   Dispatch rules for 2-arity:
   - (min arr dim)    — AFArray reduction along integer dim; dim=-1 reduces all
   - (min arr1 arr2)  — element-wise minimum of two AFArrays
   - (min n1  n2)     — falls through to `clojure.core/min`

   Parameters (AFArray arity):
   - a: Input AFArray
   - b: Integer dimension OR second AFArray

   Returns:
   AFArray with minimum values, or Number.
   Requires an active `with-arrayfire` region for AFArray inputs.

   Example:
   (min arr)        ; global minimum → scalar array
   (min arr 0)      ; column-wise minimum of a 2-D array
   (min a b)        ; element-wise minimum of two arrays
   (min 3 5)        ; => 3  (Clojure numbers)"
  ([a]
   (if (instance? AFArray a)
     (do (assert-within-arrayfire! "min")
         (algo/min (data/flat a) 0))
     (clojure.core/min a)))
  ([a b]
   (cond
     (and (instance? AFArray a) (instance? AFArray b))
     (do (assert-within-arrayfire! "min")
         (arith/minof a b))
     (and (instance? AFArray a) (integer? b))
     (do (assert-within-arrayfire! "min")
         (algo/min a b))
     (instance? AFArray a)
     (do (assert-within-arrayfire! "min")
         (algo/min a (int b)))
     :else
     (clojure.core/min a b)))
  ([a b & more]
   (reduce min (min a b) more)))

(defn max
  "Maximum of arrays along a dimension, element-wise, or scalar maximum.

   Dispatch rules for 2-arity:
   - (max arr dim)    — AFArray reduction along integer dim; dim=-1 reduces all
   - (max arr1 arr2)  — element-wise maximum of two AFArrays
   - (max n1  n2)     — falls through to `clojure.core/max`

   Parameters (AFArray arity):
   - a: Input AFArray
   - b: Integer dimension OR second AFArray

   Returns:
   AFArray with maximum values, or Number.
   Requires an active `with-arrayfire` region for AFArray inputs.

   Example:
   (max arr)        ; global maximum → scalar array
   (max arr 0)      ; column-wise maximum of a 2-D array
   (max a b)        ; element-wise maximum of two arrays
   (max 3 5)        ; => 5  (Clojure numbers)"
  ([a]
   (if (instance? AFArray a)
     (do (assert-within-arrayfire! "max")
         (algo/max (data/flat a) 0))
     (clojure.core/max a)))
  ([a b]
   (cond
     (and (instance? AFArray a) (instance? AFArray b))
     (do (assert-within-arrayfire! "max")
         (arith/maxof a b))
     (and (instance? AFArray a) (integer? b))
     (do (assert-within-arrayfire! "max")
         (algo/max a b))
     (instance? AFArray a)
     (do (assert-within-arrayfire! "max")
         (algo/max a (int b)))
     :else
     (clojure.core/max a b)))
  ([a b & more]
   (reduce max (max a b) more)))

(defn argmin
  "Find minimum values and their indices along a dimension.

   Parameters:
   - arr: Input AFArray
   - dim: Dimension to reduce along

   Returns:
   Vector of [values indices] where:
   - values:  AFArray with minimum values along dim
   - indices: AFArray of u32 indices where minima occur
   Requires an active `with-arrayfire` region.

   Example:
   (let [[mn-vals mn-idx] (argmin arr 0)]
     ...)"
  [^AFArray arr dim]
  (assert-within-arrayfire! "argmin")
  (algo/argmin arr dim))

(defn argmax
  "Find maximum values and their indices along a dimension.

   Parameters:
   - arr: Input AFArray
   - dim: Dimension to reduce along

   Returns:
   Vector of [values indices] where:
   - values:  AFArray with maximum values along dim
   - indices: AFArray of u32 indices where maxima occur
   Requires an active `with-arrayfire` region.

   Example:
   (let [[mx-vals mx-idx] (argmax arr 0)]
     ...)"
  [^AFArray arr dim]
  (assert-within-arrayfire! "argmax")
  (algo/argmax arr dim))

(defn all
  "Test whether all elements are truthy (non-zero) along a dimension.

   Parameters:
   - arr: Input AFArray (typically b8 boolean array)
   - dim: Dimension to reduce along (-1 for all, default -1)

   Returns:
   AFArray (b8) — true if all elements are non-zero.
   Requires an active `with-arrayfire` region.

   Example:
   (all (ge arr 0.0))   ; true if all elements are non-negative"
  (^AFArray [^AFArray arr]
   (assert-within-arrayfire! "all")
   (algo/all-true (data/flat arr) 0))
  (^AFArray [^AFArray arr dim]
   (assert-within-arrayfire! "all")
   (algo/all-true arr dim)))

(defn any
  "Test whether any element is truthy (non-zero) along a dimension.

   Parameters:
   - arr: Input AFArray (typically b8 boolean array)
   - dim: Dimension to reduce along (-1 for all, default -1)

   Returns:
   AFArray (b8) — true if any element is non-zero.
   Requires an active `with-arrayfire` region.

   Example:
   (any (nan-mask arr))   ; true if any NaN is present"
  (^AFArray [^AFArray arr]
   (assert-within-arrayfire! "any")
   (algo/any-true (data/flat arr) 0))
  (^AFArray [^AFArray arr dim]
   (assert-within-arrayfire! "any")
   (algo/any-true arr dim)))

(defn count-nonzero
  "Count non-zero elements along a dimension.

   Parameters:
   - arr: Input AFArray
   - dim: Dimension to reduce along (-1 for all, default -1)

   Returns:
   AFArray with count of non-zero elements.
   Requires an active `with-arrayfire` region.

   Example:
   (count-nonzero (ne arr 0.0))   ; number of non-zero elements"
  (^AFArray [^AFArray arr]
   (assert-within-arrayfire! "count-nonzero")
   (algo/count (data/flat arr) 0))
  (^AFArray [^AFArray arr dim]
   (assert-within-arrayfire! "count-nonzero")
   (algo/count arr dim)))

(defn where
  "Return the linear indices of non-zero (true) elements.

   Parameters:
   - arr: Input AFArray (typically b8 boolean array)

   Returns:
   AFArray of linear indices where arr is non-zero.
   Requires an active `with-arrayfire` region.

   Example:
   (where (gt arr 0.0))   ; indices of positive elements"
  ^AFArray
  [^AFArray arr]
  (assert-within-arrayfire! "where")
  (algo/where arr))

(defn cumsum
  "Compute inclusive prefix sum (running total) along a dimension.

   Parameters:
   - arr: Input AFArray
   - dim: Dimension along which to accumulate (default 0)

   Returns:
   AFArray with inclusive prefix sums.
   Requires an active `with-arrayfire` region.

   Example:
   (cumsum (array [1.0 2.0 3.0 4.0] [4]))  ; => [1.0 3.0 6.0 10.0]"
  (^AFArray [^AFArray arr]
   (cumsum arr 0))
  (^AFArray [^AFArray arr dim]
   (assert-within-arrayfire! "cumsum")
   (algo/scan arr dim (get defs/binary-op-kw->const :add) true)))

(defn scan
  "General prefix scan along a dimension.

   Parameters:
   - arr: Input AFArray
   - dim: Dimension along which to scan (default 0)
   - op: Binary operation keyword (:add, :mul, :min, :max)
   - inclusive?: True for inclusive scan, false for exclusive (default true)

   Returns:
   AFArray with scanned values.
   Requires an active `with-arrayfire` region.

   Example:
   (scan arr 0 :add)      ; inclusive prefix sum along dim 0
   (scan arr 0 :mul true) ; inclusive prefix product"
  (^AFArray [^AFArray arr dim op-kw]
   (scan arr dim op-kw true))
  (^AFArray [^AFArray arr dim op-kw inclusive?]
   (assert-within-arrayfire! "scan")
   (algo/scan arr dim (get defs/binary-op-kw->const op-kw 0) inclusive?)))

;;;
;;; Differences (Finite Differences)
;;;

(defn diff1
  "Compute first-order differences along a dimension.

   Computes successive differences: out[i] = in[i+1] - in[i].
   Output size along the dimension is reduced by 1.

   Parameters:
   - arr: Input AFArray
   - dim: Dimension along which to compute differences (default 0)

   Returns:
   AFArray with first-order differences.
   Requires an active `with-arrayfire` region.

   Example:
   (diff1 (array [0.0 1.0 4.0 9.0 16.0]))  ; => [1 3 5 7]  (velocity from position)"
  (^AFArray [^AFArray arr]
   (diff1 arr 0))
  (^AFArray [^AFArray arr dim]
   (assert-within-arrayfire! "diff1")
   (algo/diff1 arr dim)))

(defn diff2
  "Compute second-order differences along a dimension.

   Computes: out[i] = in[i+2] - 2*in[i+1] + in[i].
   Output size along the dimension is reduced by 2.

   Parameters:
   - arr: Input AFArray
   - dim: Dimension along which to compute differences (default 0)

   Returns:
   AFArray with second-order differences.
   Requires an active `with-arrayfire` region.

   Example:
   (diff2 (array [0.0 1.0 4.0 9.0 16.0]))  ; => [2 2 2]  (constant acceleration)"
  (^AFArray [^AFArray arr]
   (diff2 arr 0))
  (^AFArray [^AFArray arr dim]
   (assert-within-arrayfire! "diff2")
   (algo/diff2 arr dim)))

;;;
;;; Group-by Reductions
;;;

(defn sum-by-key
  "Sum values grouped by key along a dimension.

   Parameters:
   - keys: Key array (AFArray) — determines grouping
   - vals: Values array (AFArray) — to be summed
   - dim:  Dimension along which to reduce (default 0)

   Returns:
   Vector of [keys-out vals-out] as AFArrays.
   Requires an active `with-arrayfire` region.

   Example:
   (let [[k v] (sum-by-key (array [1 1 1 2 2 3] :s32) (array [10.0 20.0 30.0 40.0 50.0 60.0]))]
     ...)"
  ([^AFArray keys ^AFArray vals]
   (sum-by-key keys vals 0))
  ([^AFArray keys ^AFArray vals dim]
   (assert-within-arrayfire! "sum-by-key")
   (algo/sum-by-key keys vals dim)))

(defn product-by-key
  "Multiply values grouped by key along a dimension.

   Parameters:
   - keys: Key array (AFArray) — determines grouping
   - vals: Values array (AFArray) — to be multiplied
   - dim:  Dimension along which to reduce (default 0)

   Returns:
   Vector of [keys-out vals-out] as AFArrays.
   Requires an active `with-arrayfire` region."
  ([^AFArray keys ^AFArray vals]
   (product-by-key keys vals 0))
  ([^AFArray keys ^AFArray vals dim]
   (assert-within-arrayfire! "product-by-key")
   (algo/product-by-key keys vals dim)))

(defn min-by-key
  "Find minimum value per key group.

   Parameters:
   - keys: Key array (AFArray) — determines grouping
   - vals: Values array (AFArray)
   - dim:  Dimension along which to reduce (default 0)

   Returns:
   Vector of [keys-out vals-out] as AFArrays.
   Requires an active `with-arrayfire` region."
  ([^AFArray keys ^AFArray vals]
   (min-by-key keys vals 0))
  ([^AFArray keys ^AFArray vals dim]
   (assert-within-arrayfire! "min-by-key")
   (algo/min-by-key keys vals dim)))

(defn max-by-key
  "Find maximum value per key group.

   Parameters:
   - keys: Key array (AFArray) — determines grouping
   - vals: Values array (AFArray)
   - dim:  Dimension along which to reduce (default 0)

   Returns:
   Vector of [keys-out vals-out] as AFArrays.
   Requires an active `with-arrayfire` region."
  ([^AFArray keys ^AFArray vals]
   (max-by-key keys vals 0))
  ([^AFArray keys ^AFArray vals dim]
   (assert-within-arrayfire! "max-by-key")
   (algo/max-by-key keys vals dim)))

(defn all-true-by-key
  "Test whether all values are non-zero per key group (logical AND by group).

   Parameters:
   - keys: Key array (AFArray) — determines grouping
   - vals: Values array (AFArray) — tested for non-zero
   - dim:  Dimension along which to reduce (default 0)

   Returns:
   Vector of [keys-out vals-out] as AFArrays.
   Requires an active `with-arrayfire` region."
  ([^AFArray keys ^AFArray vals]
   (all-true-by-key keys vals 0))
  ([^AFArray keys ^AFArray vals dim]
   (assert-within-arrayfire! "all-true-by-key")
   (algo/all-true-by-key keys vals dim)))

(defn any-true-by-key
  "Test whether any value is non-zero per key group (logical OR by group).

   Parameters:
   - keys: Key array (AFArray) — determines grouping
   - vals: Values array (AFArray) — tested for non-zero
   - dim:  Dimension along which to reduce (default 0)

   Returns:
   Vector of [keys-out vals-out] as AFArrays.
   Requires an active `with-arrayfire` region."
  ([^AFArray keys ^AFArray vals]
   (any-true-by-key keys vals 0))
  ([^AFArray keys ^AFArray vals dim]
   (assert-within-arrayfire! "any-true-by-key")
   (algo/any-true-by-key keys vals dim)))

(defn count-by-key
  "Count non-zero values per key group.

   Parameters:
   - keys: Key array (AFArray) — determines grouping
   - vals: Values array (AFArray) — counted if non-zero
   - dim:  Dimension along which to reduce (default 0)

   Returns:
   Vector of [keys-out vals-out] as AFArrays.
   Requires an active `with-arrayfire` region."
  ([^AFArray keys ^AFArray vals]
   (count-by-key keys vals 0))
  ([^AFArray keys ^AFArray vals dim]
   (assert-within-arrayfire! "count-by-key")
   (algo/count-by-key keys vals dim)))

(defn scan-by-key
  "Prefix scan within groups defined by a key array.

   Parameters:
   - keys:       Key array (AFArray) — determines grouping
   - arr:        Input values array (AFArray)
   - dim:        Dimension along which to scan
   - op-kw:      Binary operation keyword (:add, :mul, :min, :max)
   - inclusive?: True for inclusive scan, false for exclusive (default true)

   Returns:
   AFArray with scanned values within each key group.
   Requires an active `with-arrayfire` region.

   Example:
   (scan-by-key keys arr 0 :add)  ; cumulative sum per group"
  (^AFArray [^AFArray keys ^AFArray arr dim op-kw]
   (scan-by-key keys arr dim op-kw true))
  (^AFArray [^AFArray keys ^AFArray arr dim op-kw inclusive?]
   (assert-within-arrayfire! "scan-by-key")
   (algo/scan-by-key keys arr dim (get defs/binary-op-kw->const op-kw 0) inclusive?)))

;;;
;;; Sorting
;;;

(defn sort
  "Sort elements along a dimension.

   Parameters:
   - arr: Input AFArray
   - dim: Dimension to sort along (default 0)
   - ascending?: True for ascending, false for descending (default true)

   Returns:
   Sorted AFArray.
   Requires an active `with-arrayfire` region.

   Example:
   (sort arr)              ; ascending sort along dim 0
   (sort arr 0 false)      ; descending"
  (^AFArray [^AFArray arr]
   (sort arr 0 true))
  (^AFArray [^AFArray arr dim]
   (sort arr dim true))
  (^AFArray [^AFArray arr dim ascending?]
   (assert-within-arrayfire! "sort")
   (algo/sort arr dim ascending?)))

(defn argsort
  "Sort and return both sorted values and sort-order indices.

   Parameters:
   - arr: Input AFArray
   - dim: Dimension to sort along (default 0)
   - ascending?: True for ascending, false for descending (default true)

   Returns:
   Vector of [sorted-values indices] as AFArrays.
   Requires an active `with-arrayfire` region.

   Example:
   (let [[vals idxs] (argsort arr)]
     ...)"
  ([^AFArray arr]
   (argsort arr 0 true))
  ([^AFArray arr dim]
   (argsort arr dim true))
  ([^AFArray arr dim ascending?]
   (assert-within-arrayfire! "argsort")
   (algo/sort-index arr dim ascending?)))

;;;
;;; Set operations
;;;

(defn unique
  "Return the unique elements of an array (sorted).

   Parameters:
   - arr: Input AFArray
   - is-sorted?: Hint that input is already sorted, enabling a faster path (default false)

   Returns:
   AFArray of unique elements in sorted order.
   Requires an active `with-arrayfire` region.

   Example:
   (unique (array [3.0 1.0 2.0 1.0 3.0] [5]))  ; => [1.0 2.0 3.0]"
  (^AFArray [^AFArray arr]
   (unique arr false))
  (^AFArray [^AFArray arr is-sorted?]
   (assert-within-arrayfire! "unique")
   (algo/set-unique arr is-sorted?)))

(defn array-union
  "Set union of two arrays (all unique elements from either).

   Parameters:
   - a: First input AFArray
   - b: Second input AFArray
   - is-unique?: Hint that inputs already contain only unique elements (default false)

   Returns:
   AFArray of sorted union elements.
   Requires an active `with-arrayfire` region.

   Example:
   (array-union a b)"
  (^AFArray [^AFArray a ^AFArray b]
   (array-union a b false))
  (^AFArray [^AFArray a ^AFArray b is-unique?]
   (assert-within-arrayfire! "array-union")
   (algo/set-union a b is-unique?)))

(defn array-intersect
  "Set intersection of two arrays (elements common to both).

   Parameters:
   - a: First input AFArray
   - b: Second input AFArray
   - is-unique?: Hint that inputs already contain only unique elements (default false)

   Returns:
   AFArray of sorted intersection elements.
   Requires an active `with-arrayfire` region.

   Example:
   (array-intersect a b)"
  (^AFArray [^AFArray a ^AFArray b]
   (array-intersect a b false))
  (^AFArray [^AFArray a ^AFArray b is-unique?]
   (assert-within-arrayfire! "array-intersect")
   (algo/set-intersect a b is-unique?)))

;;;
;;; Type conversion
;;;

(defn cast
  "Cast an array from one dtype to another.

   Parameters:
   - arr: Input AFArray
   - dtype: Target dtype keyword (e.g. :f32, :f64, :s32)

   Returns:
   New AFArray with elements cast to target dtype.
   Requires an active `with-arrayfire` region.

   Example:
   (cast arr :f32)   ; downcast f64 → f32"
  ^AFArray
  [^AFArray arr dtype]
  (assert-within-arrayfire! "cast")
  (arith/cast arr (defs/resolve-dtype dtype)))

(defn clamp
  "Clamp each element of `arr` to the range [lo, hi].

   Supports scalar or array bounds. Scalar bounds are broadcast automatically.

   Parameters:
   - arr: Input AFArray
   - lo: Lower bound (Number or AFArray)
   - hi: Upper bound (Number or AFArray)

   Returns:
   AFArray with values clamped to [lo, hi].
   Requires an active `with-arrayfire` region.

   Example:
   (clamp arr 0.0 1.0)   ; clip to unit interval"
  ^AFArray
  [^AFArray arr lo hi]
  (assert-within-arrayfire! "clamp")
  (let [arr-type (array/get-type arr)
        lo-arr (if (instance? AFArray lo) lo (scalar->array lo arr-type))
        hi-arr (if (instance? AFArray hi) hi (scalar->array hi arr-type))
        ;; Use batch=true to broadcast scalar bounds [1 1 1 1] across any arr shape
        batch? (or (clojure.core/not (instance? AFArray lo))
                   (clojure.core/not (instance? AFArray hi))
                   (clojure.core/not= (array/get-dims arr) (array/get-dims lo-arr)))]
    (arith/clamp arr lo-arr hi-arr batch?)))

;;;
;;; Complex numbers
;;;

; TODO add handling of numbers, too (e.g. (complex 1.0 2.0) → c64 scalar array)
(defn complex
  "Create a complex array from real and (optionally) imaginary arrays.

   Parameters:
   - real: Real-part AFArray
   - imag: (optional) Imaginary-part AFArray; defaults to zeros if not provided

   Returns:
   Complex AFArray (c32 or c64 depending on input dtype).
   Requires an active `with-arrayfire` region.

   Example:
   (complex re-arr im-arr)  ; [re+im*i ...]"
  (^AFArray [^AFArray real]
   (assert-within-arrayfire! "complex")
   (complex/cplx2 real (zeros-like real)))
  (^AFArray [^AFArray real ^AFArray imag]
   (assert-within-arrayfire! "complex")
   (complex/cplx2 real imag)))

(defn real-part
  "Extract the real part of a complex array.

   Parameters:
   - arr: Complex AFArray (c32 or c64)

   Returns:
   Real-part AFArray (f32 or f64).
   Requires an active `with-arrayfire` region.

   Example:
   (real-part complex-arr)  ; => f64 array of real parts"
  ^AFArray
  [^AFArray arr]
  (assert-within-arrayfire! "real-part")
  (complex/real arr))

(defn imag-part
  "Extract the imaginary part of a complex array.

   Parameters:
   - arr: Complex AFArray (c32 or c64)

   Returns:
   Imaginary-part AFArray (f32 or f64).
   Requires an active `with-arrayfire` region.

   Example:
   (imag-part complex-arr)  ; => f64 array of imaginary parts"
  ^AFArray
  [^AFArray arr]
  (assert-within-arrayfire! "imag-part")
  (complex/imag arr))

(defn conjg
  "Complex conjugate of each element (a + bi → a - bi).

   Parameters:
   - arr: Complex AFArray (c32 or c64)

   Returns:
   Complex AFArray with negated imaginary parts.
   Requires an active `with-arrayfire` region.

   Example:
   (conjg complex-arr)"
  ^AFArray
  [^AFArray arr]
  (assert-within-arrayfire! "conjg")
  (complex/conjg arr))

;;;
;;; Linear algebra essentials
;;;

; TODO rename to matrix-multiply
(defn matmul
  "Matrix multiplication (dense or sparse-dense), accepting 2 or more matrices.

   With 2 arguments computes lhs × rhs.
   With 3 or more arguments chains left-associatively: (A × B) × C × …

   All arrays must be 2-D matrices with compatible shapes.

   Parameters:
   - lhs:  Left-hand side AFArray (m × k)
   - rhs:  Right-hand side AFArray (k × n)
   - more: Additional AFArrays to chain multiply (optional)

   Returns:
   Result AFArray.
   Requires an active `with-arrayfire` region.

   Examples:
   (matmul A B)         ; A × B
   (matmul A B C)       ; (A × B) × C
   (reduce matmul [A B C D])  ; same"
  (^AFArray [^AFArray lhs ^AFArray rhs]
   (assert-within-arrayfire! "matmul")
   (blas/matmul lhs rhs))
  (^AFArray [^AFArray lhs ^AFArray rhs & more]
   (reduce matmul (matmul lhs rhs) more)))

(defn dot
  "Dot (inner) product of two vectors, returning a scalar array.

   Parameters:
   - lhs: First vector AFArray
   - rhs: Second vector AFArray (same length as lhs)

   Returns:
   Scalar AFArray with the dot product.
   Requires an active `with-arrayfire` region.

   Example:
   (dot u v)   ; u · v"
  ^AFArray
  [^AFArray lhs ^AFArray rhs]
  (assert-within-arrayfire! "dot")
  (blas/dot lhs rhs))

;;;
;;; Basic arithmetic functions
;;;

;;
;; Binary operators — shadow clojure.core/+ - * / abs
;;

(defn +
  "Element-wise addition of arrays or numbers.

   Array operands require an active `with-arrayfire` region.
   
   Parameters:
   - lhs: left-hand side operand (Number or AFArray)
   - rhs: right-hand side operand (Number or AFArray)
   - more: additional operands for variadic addition (optional)
 
   Arities:
   - ()        → 0 (additive identity)
   - (x)       → x
   - (lhs rhs) → cond:
       both Number    → clojure.core/+
       both AFArray   → element-wise GPU add (arith/add)
       AFArray+Number → scalar broadcast via data/constant [1] + batch=true
       Number+AFArray → scalar broadcast (commutative)
   - (lhs rhs & more) → left-associative fold

   Returns:
   The sum of the operands, with AFArray results converted to host data.
   
   Example:
     (+ 1 2) ; => 3
     (+ arr1 arr2) ; element-wise GPU add of two AFArrays
     (+ arr1 5) ; adds scalar 5 to each element of arr1 via GPU broadcast"
  ([] 0)
  ([x] x)
  ([lhs rhs]
   (cond
     (and (number? lhs) (number? rhs))
     (clojure.core/+ lhs rhs)

     (and (instance? AFArray lhs) (instance? AFArray rhs))
     (do (assert-within-arrayfire! "+")
         (arith/add lhs rhs))

     (instance? AFArray lhs)
     (do (assert-within-arrayfire! "+")
         (arith/add lhs (scalar->array rhs (array/get-type lhs)) true))

     :else ; rhs is AFArray
     (do (assert-within-arrayfire! "+")
         (arith/add (scalar->array lhs (array/get-type rhs)) rhs true))))
  ([lhs rhs & more]
   (reduce + (+ lhs rhs) more)))

(defn -
  "Element-wise subtraction of arrays or numbers.

   Array operands require an active `with-arrayfire` region.

   Parameters:
   - lhs: left-hand side operand (Number or AFArray)
   - rhs: right-hand side operand (Number or AFArray)
   - more: additional operands for variadic subtraction (optional)

   Arities:
   - (x)       → negate: Number → clojure.core/-, AFArray → element-wise negation
   - (lhs rhs) → cond:
       both Number    → clojure.core/-
       both AFArray   → element-wise GPU sub
       AFArray-Number → scalar broadcast via 1-element constant + batch=true
       Number-AFArray → scalar broadcast
   - (lhs rhs & more) → left-associative fold

   Returns:
   The difference of the operands, as a Number or AFArray.

   Example:
   (- 5 3)      ; => 2
   (- arr 1.0)  ; subtracts 1.0 from each element of arr via GPU broadcast
   (- arr)      ; negates every element of arr"
  ([x]
   (if (instance? AFArray x)
     (do (assert-within-arrayfire! "-")
         (arith/mul x (scalar->array -1.0 (array/get-type x)) true))
     (clojure.core/- x)))
  ([lhs rhs]
   (cond
     (and (number? lhs) (number? rhs))
     (clojure.core/- lhs rhs)

     (and (instance? AFArray lhs) (instance? AFArray rhs))
     (do (assert-within-arrayfire! "-")
         (arith/sub lhs rhs))

     (instance? AFArray lhs)
     (do (assert-within-arrayfire! "-")
         (arith/sub lhs (scalar->array rhs (array/get-type lhs)) true))

     :else ; rhs is AFArray
     (do (assert-within-arrayfire! "-")
         (arith/sub (scalar->array lhs (array/get-type rhs)) rhs true))))
  ([lhs rhs & more]
   (reduce - (- lhs rhs) more)))

(defn *
  "Element-wise multiplication of arrays or numbers.

   Array operands require an active `with-arrayfire` region.

   Parameters:
   - lhs: left-hand side operand (Number or AFArray)
   - rhs: right-hand side operand (Number or AFArray)
   - more: additional operands for variadic multiplication (optional)

   Arities:
   - ()        → 1 (multiplicative identity)
   - (x)       → x
   - (lhs rhs) → cond:
       both Number    → clojure.core/*
       both AFArray   → element-wise GPU mul
       AFArray*Number → scalar broadcast via 1-element constant + batch=true
       Number*AFArray → scalar broadcast (commutative)
   - (lhs rhs & more) → left-associative fold

   Returns:
   The product of the operands, as a Number or AFArray.

   Example:
   (* 3 4)      ; => 12
   (* arr 2.0)  ; multiplies each element of arr by 2.0 via GPU broadcast
   (* arr1 arr2) ; element-wise GPU multiplication of two AFArrays"
  ([] 1)
  ([x] x)
  ([lhs rhs]
   (cond
     (and (number? lhs) (number? rhs))
     (clojure.core/* lhs rhs)

     (and (instance? AFArray lhs) (instance? AFArray rhs))
     (do (assert-within-arrayfire! "*")
         (arith/mul lhs rhs))

     (instance? AFArray lhs)
     (do (assert-within-arrayfire! "*")
         (arith/mul lhs (scalar->array rhs (array/get-type lhs)) true))

     :else ; rhs is AFArray
     (do (assert-within-arrayfire! "*")
         (arith/mul (scalar->array lhs (array/get-type rhs)) rhs true))))
  ([lhs rhs & more]
   (reduce * (* lhs rhs) more)))

(defn /
  "Element-wise division of arrays or numbers.

   Array operands require an active `with-arrayfire` region.

   Parameters:
   - lhs: left-hand side operand / numerator (Number or AFArray)
   - rhs: right-hand side operand / denominator (Number or AFArray)
   - more: additional operands for variadic division (optional)

   Arities:
   - (x)       → reciprocal: Number → (clojure.core// 1 x),
                              AFArray → 1.0 divided element-wise by x via broadcast
   - (lhs rhs) → cond:
       both Number    → clojure.core//
       both AFArray   → element-wise GPU div
       AFArray/Number → scalar broadcast via 1-element constant + batch=true
       Number/AFArray → scalar broadcast
   - (lhs rhs & more) → left-associative fold

   Returns:
   The quotient of the operands, as a Number or AFArray.

   Example:
   (/ 10 2)     ; => 5
   (/ arr 2.0)  ; divides each element of arr by 2.0 via GPU broadcast
   (/ arr)      ; element-wise reciprocal 1/x for each element"
  ([x]
   (if (instance? AFArray x)
     (do (assert-within-arrayfire! "/")
         (arith/div (scalar->array 1.0 (array/get-type x)) x true))
     (clojure.core// 1 x)))
  ([lhs rhs]
   (cond
     (and (number? lhs) (number? rhs))
     (clojure.core// lhs rhs)

     (and (instance? AFArray lhs) (instance? AFArray rhs))
     (do (assert-within-arrayfire! "/")
         (arith/div lhs rhs))

     (instance? AFArray lhs)
     (do (assert-within-arrayfire! "/")
         (arith/div lhs (scalar->array rhs (array/get-type lhs)) true))

     :else ; rhs is AFArray
     (do (assert-within-arrayfire! "/")
         (arith/div (scalar->array lhs (array/get-type rhs)) rhs true))))
  ([lhs rhs & more]
   (reduce / (/ lhs rhs) more)))

(defn abs
  "Absolute value of each element.
   For complex arrays returns the element-wise magnitude sqrt(re² + im²).
   Falls through to clojure.core/abs for plain numbers.

   Supported types: f32, f64, c32, c64, s32, s64, u32, u64, s16, u16, u8, b8

   Parameters:
   - x: input value (Number or AFArray)

   Returns:
   For AFArray: AFArray with the element-wise absolute value. Requires an active `with-arrayfire` region.
   For Number: the absolute value as a Number.

   Example:
   (abs -5)      ; => 5
   (abs arr)     ; element-wise |x| across all array elements"
  [x]
  (if (instance? AFArray x)
    (do (assert-within-arrayfire! "abs")
        (arith/abs x))
    (clojure.core/abs x)))

(defn neg
  "Negate every element of an AFArray (equivalent to `(- arr)`).

   Supported types: f32, f64, c32, c64, s32, s64, u32, u64, s16, u16, u8, b8

   Parameters:
   - a: input array (AFArray)

   Returns:
   AFArray with every element negated. Requires an active `with-arrayfire` region.

   Example:
   (neg arr)  ; negates every element: [1.0 -2.0 3.0] → [-1.0 2.0 -3.0]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "neg")
  (arith/mul a (scalar->array -1.0 (array/get-type a)) true))

;;
;; Modulo and remainder — fall through to clojure.core for plain numbers
;;

(defn mod
  "Element-wise modulo (floored division remainder).
   Falls through to clojure.core/mod for plain numbers.

   Supported types: f32, f64, s32, u32, u8, s64, u64, s16, u16

   Parameters:
   - lhs: dividend (Number or AFArray)
   - rhs: divisor  (Number or AFArray)

   Returns:
   Element-wise lhs mod rhs, as a Number or AFArray.
   Supports scalar broadcasting when one operand is a Number.
   Requires an active `with-arrayfire` region when any operand is an AFArray.

   Example:
   (mod 10 3)    ; => 1
   (mod arr 3.0) ; element-wise modulo by 3.0 via GPU broadcast"
  ^AFArray
  [lhs rhs]
  (cond
    (and (number? lhs) (number? rhs))
    (clojure.core/mod lhs rhs)

    (and (instance? AFArray lhs) (instance? AFArray rhs))
    (do (assert-within-arrayfire! "mod")
        (arith/mod lhs rhs))

    (instance? AFArray lhs)
    (do (assert-within-arrayfire! "mod")
        (arith/mod lhs (scalar->array rhs (array/get-type lhs)) true))

    :else ; rhs is AFArray
    (do (assert-within-arrayfire! "mod")
        (arith/mod (scalar->array lhs (array/get-type rhs)) rhs true))))

(defn rem
  "Element-wise remainder (truncated division).
   Falls through to clojure.core/rem for plain numbers.

   Unlike `mod` (which uses floored division), `rem` uses truncated division —
   the result has the same sign as the dividend.

   Supported types: f32, f64, s32, u32, u8, s64, u64, s16, u16

   Parameters:
   - lhs: dividend (Number or AFArray)
   - rhs: divisor  (Number or AFArray)

   Returns:
   Element-wise remainder of lhs divided by rhs, as a Number or AFArray.
   Supports scalar broadcasting when one operand is a Number.
   Requires an active `with-arrayfire` region when any operand is an AFArray.

   Example:
   (rem 10 3)    ; => 1
   (rem arr 3.0) ; element-wise remainder by 3.0 via GPU broadcast"
  ^AFArray
  [lhs rhs]
  (cond
    (and (number? lhs) (number? rhs))
    (clojure.core/rem lhs rhs)

    (and (instance? AFArray lhs) (instance? AFArray rhs))
    (do (assert-within-arrayfire! "rem")
        (arith/rem lhs rhs))

    (instance? AFArray lhs)
    (do (assert-within-arrayfire! "rem")
        (arith/rem lhs (scalar->array rhs (array/get-type lhs)) true))

    :else ; rhs is AFArray
    (do (assert-within-arrayfire! "rem")
        (arith/rem (scalar->array lhs (array/get-type rhs)) rhs true))))

;;
;; Power and roots — named as in clojure.math
;;
(defn pow
  "Raise each element of lhs to the power rhs (element-wise).
   Falls through to clojure.math/pow for plain numbers.

   Supported types: f32, f64, c32, c64

   Parameters:
   - lhs: base (Number or AFArray)
   - rhs: exponent (Number or AFArray)

   Returns:
   Element-wise lhs^rhs, as a Number or AFArray.
   Supports scalar broadcasting when one operand is a Number.
   Requires an active `with-arrayfire` region when any operand is an AFArray.

   Example:
   (pow 2.0 3.0) ; => 8.0
   (pow arr 2.0) ; squares each element of arr via GPU broadcast"
  ^AFArray
  [lhs rhs]
  (assert-within-arrayfire! "pow")
  (cond
    (and (number? lhs) (number? rhs))
    (clojure.math/pow lhs rhs)

    (and (instance? AFArray lhs) (instance? AFArray rhs))
    (arith/pow lhs rhs)

    (instance? AFArray lhs)
    (arith/pow lhs (scalar->array rhs (array/get-type lhs)) true)

    :else ; rhs is AFArray
    (arith/pow (scalar->array lhs (array/get-type rhs)) rhs true)))

(defn sqrt
  "Element-wise square root of each array element.

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: input array (AFArray)

   Returns:
   AFArray with element-wise sqrt(x). Requires an active `with-arrayfire` region.

   Example:
   (sqrt (array [4.0 9.0 16.0] [3])) ; => [2.0 3.0 4.0]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "sqrt")
  (arith/sqrt a))

(defn cbrt
  "Element-wise cube root of each array element.

   Supported types: f32, f64

   Parameters:
   - a: input array (AFArray)

   Returns:
   AFArray with element-wise x^(1/3). Requires an active `with-arrayfire` region.

   Example:
   (cbrt (array [8.0 27.0] [2])) ; => [2.0 3.0]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "cbrt")
  (arith/cbrt a))

(defn rsqrt
  "Element-wise reciprocal square root: 1 / sqrt(x).

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: input array (AFArray)

   Returns:
   AFArray with element-wise 1/sqrt(x). Requires an active `with-arrayfire` region.

   Example:
   (rsqrt (array [4.0 16.0] [2])) ; => [0.5 0.25]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "rsqrt")
  (arith/rsqrt a))

(defn pow2
  "Element-wise 2^x for each array element.

   Supported types: f32, f64

   Parameters:
   - a: input array (AFArray)

   Returns:
   AFArray with element-wise 2^x. Requires an active `with-arrayfire` region.

   Example:
   (pow2 (array [0.0 1.0 2.0 3.0] [4])) ; => [1.0 2.0 4.0 8.0]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "pow2")
  (arith/pow2 a))

;;
;; Exponential and logarithm — named as in clojure.math
;;

(defn exp
  "Element-wise natural exponential e^x.

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: input array (AFArray)

   Returns:
   AFArray with element-wise e^x. Requires an active `with-arrayfire` region.

   Example:
   (exp (array [0.0 1.0 2.0] [3])) ; => [1.0 e e²]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "exp")
  (arith/exp a))

(defn expm1
  "Element-wise exp(x) - 1, numerically stable for small x.

   Prefer `expm1` over `(- (exp x) 1)` when x is close to zero to avoid
   catastrophic cancellation.

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: input array (AFArray)

   Returns:
   AFArray with element-wise exp(x) - 1. Requires an active `with-arrayfire` region.

   Example:
   (expm1 (array [0.0 1.0] [2])) ; => [0.0 (e - 1)]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "expm1")
  (arith/expm1 a))

(defn log
  "Element-wise natural logarithm ln(x).

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: input array (AFArray)

   Returns:
   AFArray with element-wise ln(x). Requires an active `with-arrayfire` region.

   Example:
   (log (array [1.0 Math/E] [2])) ; => [0.0 1.0]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "log")
  (arith/log a))

(defn log2
  "Element-wise base-2 logarithm log₂(x).

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: input array (AFArray)

   Returns:
   AFArray with element-wise log₂(x). Requires an active `with-arrayfire` region.

   Example:
   (log2 (array [1.0 2.0 4.0 8.0] [4])) ; => [0.0 1.0 2.0 3.0]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "log2")
  (arith/log2 a))

(defn log10
  "Element-wise base-10 logarithm log₁₀(x).

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: input array (AFArray)

   Returns:
   AFArray with element-wise log₁₀(x). Requires an active `with-arrayfire` region.

   Example:
   (log10 (array [1.0 10.0 100.0] [3])) ; => [0.0 1.0 2.0]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "log10")
  (arith/log10 a))

(defn log1p
  "Element-wise log(1 + x), numerically stable for small x.

   Prefer `log1p` over `(log (+ 1 x))` when x is close to zero to avoid
   catastrophic cancellation.

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: input array (AFArray)

   Returns:
   AFArray with element-wise log(1 + x). Requires an active `with-arrayfire` region.

   Example:
   (log1p (array [0.0 1.0] [2])) ; => [0.0 ln(2)]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "log1p")
  (arith/log1p a))

;;
;; Rounding — named as in clojure.core
;;

(defn floor
  "Element-wise floor: largest integer ≤ x.

   Supported types: f32, f64

   Parameters:
   - a: input array (AFArray)

   Returns:
   AFArray with element-wise floor(x). Requires an active `with-arrayfire` region.

   Example:
   (floor (array [1.7 -1.7 2.0] [3])) ; => [1.0 -2.0 2.0]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "floor")
  (arith/floor a))

(defn ceil
  "Element-wise ceiling: smallest integer ≥ x.

   Supported types: f32, f64

   Parameters:
   - a: input array (AFArray)

   Returns:
   AFArray with element-wise ceil(x). Requires an active `with-arrayfire` region.

   Example:
   (ceil (array [1.2 -1.2 2.0] [3])) ; => [2.0 -1.0 2.0]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "ceil")
  (arith/ceil a))

(defn round
  "Element-wise rounding to the nearest integer (round half away from zero).

   Supported types: f32, f64

   Parameters:
   - a: input array (AFArray)

   Returns:
   AFArray with element-wise round(x). Requires an active `with-arrayfire` region.

   Example:
   (round (array [1.4 1.5 -1.5] [3])) ; => [1.0 2.0 -2.0]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "round")
  (arith/round a))

(defn trunc
  "Element-wise truncation toward zero (drop the fractional part).

   Supported types: f32, f64

   Parameters:
   - a: input array (AFArray)

   Returns:
   AFArray with element-wise trunc(x). Requires an active `with-arrayfire` region.

   Example:
   (trunc (array [1.7 -1.7] [2])) ; => [1.0 -1.0]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "trunc")
  (arith/trunc a))

(defn sign-bit
  "Element-wise sign function: returns 1 for negatives, 0 otherwise.

   Note: ArrayFire's af_sign returns 1 for negative values and 0 for non-negative values
   (i.e. it is the sign bit, not the mathematical signum).
   Use `signum` for the mathematical -1/0/+1 result.

   Supported types: f32, f64, s32, s64

   Parameters:
   - a: input array (AFArray)

   Returns:
   AFArray with element-wise sign values. Requires an active `with-arrayfire` region.

   Example:
   (sign-bit (array [-3.0 0.0 5.0] [3])) ; => [1 0 0] (sign bit convention)"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "sign-bit")
  (arith/sign a))

; TODO should support more types (e.g. integers)
(defn signum
  "Element-wise mathematical signum: -1.0 for negatives, 0.0 for zero, +1.0 for positives.

   Differs from `sign-bit`, which returns 1 for negative and 0 otherwise (IEEE sign bit).
   Computed as: 1*(x > 0) - 1*(x < 0)

   Supported types: f32, f64

   Parameters:
   - a: input array (AFArray)

   Returns:
   AFArray with element-wise signum values. Requires an active `with-arrayfire` region.

   Example:
   (signum (array [-3.0 0.0 5.0] [3])) ; => [-1.0 0.0 1.0]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "signum")
  (let [pos (arith/cast (arith/gt a (scalar->array 0.0 (array/get-type a))) (array/get-type a))
        neg (arith/cast (arith/lt a (scalar->array 0.0 (array/get-type a))) (array/get-type a))]
    (arith/sub pos neg)))

;;
;; Trigonometry — named as in clojure.math
;;

(defn sin
  "Element-wise sine of each element (input in radians).

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: input array (AFArray), values interpreted as radians

   Returns:
   AFArray with element-wise sin(x). Requires an active `with-arrayfire` region.

   Example:
   (sin (array [0.0 (/ Math/PI 2)] [2])) ; => [0.0 1.0]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "sin")
  (arith/sin a))

(defn cos
  "Element-wise cosine of each element (input in radians).

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: input array (AFArray), values interpreted as radians

   Returns:
   AFArray with element-wise cos(x). Requires an active `with-arrayfire` region.

   Example:
   (cos (array [0.0 Math/PI] [2])) ; => [1.0 -1.0]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "cos")
  (arith/cos a))

(defn tan
  "Element-wise tangent of each element (input in radians).

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: input array (AFArray), values interpreted as radians

   Returns:
   AFArray with element-wise tan(x). Requires an active `with-arrayfire` region.

   Example:
   (tan (array [0.0 (/ Math/PI 4)] [2])) ; => [0.0 1.0]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "tan")
  (arith/tan a))

(defn asin
  "Element-wise arcsine; result in [-π/2, π/2] (radians).

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: input array (AFArray), values in [-1, 1]

   Returns:
   AFArray with element-wise arcsin(x) in radians. Requires an active `with-arrayfire` region.

   Example:
   (asin (array [0.0 1.0] [2])) ; => [0.0 π/2]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "asin")
  (arith/asin a))

(defn acos
  "Element-wise arccosine; result in [0, π] (radians).

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: input array (AFArray), values in [-1, 1]

   Returns:
   AFArray with element-wise arccos(x) in radians. Requires an active `with-arrayfire` region.

   Example:
   (acos (array [1.0 0.0 -1.0] [3])) ; => [0.0 π/2 π]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "acos")
  (arith/acos a))

(defn atan
  "Element-wise arctangent; result in (-π/2, π/2) (radians).

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: input array (AFArray)

   Returns:
   AFArray with element-wise arctan(x) in radians. Requires an active `with-arrayfire` region.

   Example:
   (atan (array [0.0 1.0] [2])) ; => [0.0 π/4]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "atan")
  (arith/atan a))

(defn atan2
  "Element-wise two-argument arctangent atan(y/x).
   Uses the signs of both arguments to determine the correct quadrant.
   Result is in (-π, π] (radians).

   Supports scalar broadcasting: one operand may be a Number.

   Supported types: f32, f64

   Parameters:
   - y: y-coordinate (Number or AFArray)
   - x: x-coordinate (Number or AFArray)

   Returns:
   AFArray with element-wise atan2(y, x) in radians.
   Requires an active `with-arrayfire` region.

   Example:
   (atan2 (array [1.0 1.0 -1.0] [3])
          (array [1.0 -1.0 1.0] [3])) ; => [π/4 3π/4 -π/4]"
  ^AFArray
  [y x]
  (assert-within-arrayfire! "atan2")
  (cond
    (and (instance? AFArray y) (instance? AFArray x))
    (arith/atan2 y x)

    (instance? AFArray y)
    (arith/atan2 y (scalar->array x (array/get-type y)) true)

    :else ; x is AFArray
    (arith/atan2 (scalar->array y (array/get-type x)) x true)))

;;
;; Hyperbolic functions — named as in clojure.math
;;

(defn sinh
  "Element-wise hyperbolic sine: (e^x - e^-x) / 2.

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: input array (AFArray)

   Returns:
   AFArray with element-wise sinh(x). Requires an active `with-arrayfire` region.

   Example:
   (sinh (array [0.0 1.0] [2])) ; => [0.0 ~1.1752]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "sinh")
  (arith/sinh a))

(defn cosh
  "Element-wise hyperbolic cosine: (e^x + e^-x) / 2.

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: input array (AFArray)

   Returns:
   AFArray with element-wise cosh(x). Requires an active `with-arrayfire` region.

   Example:
   (cosh (array [0.0 1.0] [2])) ; => [1.0 ~1.5431]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "cosh")
  (arith/cosh a))

(defn tanh
  "Element-wise hyperbolic tangent: sinh(x) / cosh(x).

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: input array (AFArray)

   Returns:
   AFArray with element-wise tanh(x) in (-1, 1). Requires an active `with-arrayfire` region.

   Example:
   (tanh (array [0.0 1.0] [2])) ; => [0.0 ~0.7616]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "tanh")
  (arith/tanh a))

(defn asinh
  "Element-wise inverse hyperbolic sine: log(x + sqrt(x² + 1)).

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: input array (AFArray)

   Returns:
   AFArray with element-wise asinh(x). Requires an active `with-arrayfire` region.

   Example:
   (asinh (array [0.0 1.0] [2])) ; => [0.0 ~0.8814]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "asinh")
  (arith/asinh a))

(defn acosh
  "Element-wise inverse hyperbolic cosine: log(x + sqrt(x² - 1)); x ≥ 1.

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: input array (AFArray), values must be ≥ 1

   Returns:
   AFArray with element-wise acosh(x). Requires an active `with-arrayfire` region.

   Example:
   (acosh (array [1.0 2.0] [2])) ; => [0.0 ~1.3170]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "acosh")
  (arith/acosh a))

(defn atanh
  "Element-wise inverse hyperbolic tangent: 0.5 * log((1 + x) / (1 - x)); |x| < 1.

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: input array (AFArray), values must be in (-1, 1)

   Returns:
   AFArray with element-wise atanh(x). Requires an active `with-arrayfire` region.

   Example:
   (atanh (array [0.0 0.5] [2])) ; => [0.0 ~0.5493]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "atanh")
  (arith/atanh a))

;;
;; Special / activation functions
;;

(defn sigmoid
  "Element-wise sigmoid activation function: 1 / (1 + exp(-x)).

   Supported types: f32, f64

   Parameters:
   - a: input array (AFArray)

   Returns:
   AFArray with element-wise sigmoid(x) in (0, 1). Requires an active `with-arrayfire` region.

   Example:
   (sigmoid (array [0.0 1.0 -1.0] [3])) ; => [0.5 ~0.731 ~0.269]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "sigmoid")
  (arith/sigmoid a))

(defn erf
  "Element-wise Gauss error function: (2/√π) ∫₀ˣ exp(-t²) dt.

   Supported types: f32, f64

   Parameters:
   - a: input array (AFArray)

   Returns:
   AFArray with element-wise erf(x) in (-1, 1). Requires an active `with-arrayfire` region.

   Example:
   (erf (array [0.0 1.0] [2])) ; => [0.0 ~0.8427]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "erf")
  (arith/erf a))

(defn erfc
  "Element-wise complementary error function: 1 - erf(x).

   Prefer `erfc` over `(- 1 (erf x))` for large x to avoid catastrophic cancellation.

   Supported types: f32, f64

   Parameters:
   - a: input array (AFArray)

   Returns:
   AFArray with element-wise erfc(x) in (0, 2). Requires an active `with-arrayfire` region.

   Example:
   (erfc (array [0.0 1.0] [2])) ; => [1.0 ~0.1573]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "erfc")
  (arith/erfc a))

(defn tgamma
  "Element-wise gamma function Γ(x).

   Supported types: f32, f64

   Parameters:
   - a: input array (AFArray)

   Returns:
   AFArray with element-wise Γ(x). Requires an active `with-arrayfire` region.

   Example:
   (tgamma (array [1.0 2.0 3.0 4.0] [4])) ; => [1.0 1.0 2.0 6.0]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "tgamma")
  (arith/tgamma a))

(defn lgamma
  "Element-wise log-gamma function: log|Γ(x)|.

   Supported types: f32, f64

   Parameters:
   - a: input array (AFArray)

   Returns:
   AFArray with element-wise log|Γ(x)|. Requires an active `with-arrayfire` region.

   Example:
   (lgamma (array [1.0 2.0 3.0] [3])) ; => [0.0 0.0 ~0.693]"
  [^AFArray a]
  (assert-within-arrayfire! "lgamma")
  (arith/lgamma a))

(defn factorial
  "Element-wise factorial: n! = Γ(n + 1).

   Supported types: f32, f64

   Parameters:
   - a: input array (AFArray), values should be non-negative integers

   Returns:
   AFArray with element-wise n!. Requires an active `with-arrayfire` region.

   Example:
   (factorial (array [0.0 1.0 2.0 3.0 4.0] [5])) ; => [1.0 1.0 2.0 6.0 24.0]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "factorial")
  (arith/factorial a))

(defn arg
  "Element-wise complex argument (phase angle) of a complex array.
   For a complex value z = re + im·i, returns atan2(im, re).

   Supported types: c32, c64
   Output types: f32 (from c32), f64 (from c64)

   Parameters:
   - a: input complex array (AFArray)

   Returns:
   AFArray with element-wise phase angle in (-π, π] (radians).
   Requires an active `with-arrayfire` region.

   Example:
   (arg complex-arr) ; returns the phase angle of each complex element"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "arg")
  (arith/arg a))

;;
;; Element-wise masks (return boolean arrays, not scalar predicates)
;;
(defn nan-mask
  "Element-wise NaN check. Returns a b8 boolean array — not a scalar boolean.
   Use `(any (nan-mask arr))` to test whether any element is NaN.

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: input array (AFArray)

   Returns:
   AFArray of b8 where 1 marks NaN positions, 0 otherwise.
   Requires an active `with-arrayfire` region.

   Example:
   (nan-mask (array [1.0 Double/NaN 3.0] [3])) ; => [0 1 0]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "nan-mask")
  (arith/nan? a))

(defn inf-mask
  "Element-wise infinity check. Returns a b8 boolean array — not a scalar boolean.
   Use `(any (inf-mask arr))` to test whether any element is infinite.

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: input array (AFArray)

   Returns:
   AFArray of b8 where 1 marks infinite positions, 0 otherwise.
   Requires an active `with-arrayfire` region.

   Example:
   (inf-mask (array [1.0 Double/POSITIVE_INFINITY 3.0] [3])) ; => [0 1 0]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "inf-mask")
  (arith/inf? a))

;;
;; Element-wise comparisons — distinct names to avoid shadowing =, <, > etc.
;;

(defn eq
  "Element-wise equality test. Returns a boolean array (b8).
   Supports scalar broadcasting when one operand is a Number.

   Supported types: f32, f64, c32, c64, s32, u32, u8, s64, u64, s16, u16, b8

   Parameters:
   - lhs: left-hand side (Number or AFArray)
   - rhs: right-hand side (Number or AFArray)

   Returns:
   AFArray of b8 where true (1) marks positions where lhs = rhs.
   Requires an active `with-arrayfire` region.

   Example:
   (eq arr 2.0) ; marks positions where each element equals 2.0"
  ^AFArray
  [lhs rhs]
  (assert-within-arrayfire! "eq")
  (cond
    (and (instance? AFArray lhs) (instance? AFArray rhs))
    (arith/eq lhs rhs)
    (instance? AFArray lhs)
    (arith/eq lhs (scalar->array rhs (array/get-type lhs)) true)
    :else
    (arith/eq (scalar->array lhs (array/get-type rhs)) rhs true)))

(defn ne
  "Element-wise inequality test. Returns a boolean array (b8).
   Supports scalar broadcasting when one operand is a Number.

   Supported types: f32, f64, c32, c64, s32, u32, u8, s64, u64, s16, u16, b8

   Parameters:
   - lhs: left-hand side (Number or AFArray)
   - rhs: right-hand side (Number or AFArray)

   Returns:
   AFArray of b8 where true (1) marks positions where lhs ≠ rhs.
   Requires an active `with-arrayfire` region.

   Example:
   (ne arr 0.0) ; marks positions where each element is not zero"
  ^AFArray
  [lhs rhs]
  (assert-within-arrayfire! "ne")
  (cond
    (and (instance? AFArray lhs) (instance? AFArray rhs))
    (arith/neq lhs rhs)
    (instance? AFArray lhs)
    (arith/neq lhs (scalar->array rhs (array/get-type lhs)) true)
    :else
    (arith/neq (scalar->array lhs (array/get-type rhs)) rhs true)))

(defn lt
  "Element-wise less-than test. Returns a boolean array (b8).
   Supports scalar broadcasting when one operand is a Number.

   Supported types: f32, f64, s32, u32, u8, s64, u64, s16, u16

   Parameters:
   - lhs: left-hand side (Number or AFArray)
   - rhs: right-hand side (Number or AFArray)

   Returns:
   AFArray of b8 where true (1) marks positions where lhs < rhs.
   Requires an active `with-arrayfire` region.

   Example:
   (lt arr 0.0) ; marks negative elements"
  ^AFArray
  [lhs rhs]
  (assert-within-arrayfire! "lt")
  (cond
    (and (instance? AFArray lhs) (instance? AFArray rhs))
    (arith/lt lhs rhs)
    (instance? AFArray lhs)
    (arith/lt lhs (scalar->array rhs (array/get-type lhs)) true)
    :else
    (arith/lt (scalar->array lhs (array/get-type rhs)) rhs true)))

(defn le
  "Element-wise less-than-or-equal test. Returns a boolean array (b8).
   Supports scalar broadcasting when one operand is a Number.

   Supported types: f32, f64, s32, u32, u8, s64, u64, s16, u16

   Parameters:
   - lhs: left-hand side (Number or AFArray)
   - rhs: right-hand side (Number or AFArray)

   Returns:
   AFArray of b8 where true (1) marks positions where lhs ≤ rhs.
   Requires an active `with-arrayfire` region.

   Example:
   (le arr 1.0) ; marks elements that are at most 1.0"
  ^AFArray
  [lhs rhs]
  (assert-within-arrayfire! "le")
  (cond
    (and (instance? AFArray lhs) (instance? AFArray rhs))
    (arith/le lhs rhs)
    (instance? AFArray lhs)
    (arith/le lhs (scalar->array rhs (array/get-type lhs)) true)
    :else
    (arith/le (scalar->array lhs (array/get-type rhs)) rhs true)))

(defn gt
  "Element-wise greater-than test. Returns a boolean array (b8).
   Supports scalar broadcasting when one operand is a Number.

   Supported types: f32, f64, s32, u32, u8, s64, u64, s16, u16

   Parameters:
   - lhs: left-hand side (Number or AFArray)
   - rhs: right-hand side (Number or AFArray)

   Returns:
   AFArray of b8 where true (1) marks positions where lhs > rhs.
   Requires an active `with-arrayfire` region.

   Example:
   (gt arr 0.0) ; marks positive elements"
  ^AFArray
  [lhs rhs]
  (assert-within-arrayfire! "gt")
  (cond
    (and (instance? AFArray lhs) (instance? AFArray rhs))
    (arith/gt lhs rhs)
    (instance? AFArray lhs)
    (arith/gt lhs (scalar->array rhs (array/get-type lhs)) true)
    :else
    (arith/gt (scalar->array lhs (array/get-type rhs)) rhs true)))

(defn ge
  "Element-wise greater-than-or-equal test. Returns a boolean array (b8).
   Supports scalar broadcasting when one operand is a Number.

   Supported types: f32, f64, s32, u32, u8, s64, u64, s16, u16

   Parameters:
   - lhs: left-hand side (Number or AFArray)
   - rhs: right-hand side (Number or AFArray)

   Returns:
   AFArray of b8 where true (1) marks positions where lhs ≥ rhs.
   Requires an active `with-arrayfire` region.

   Example:
   (ge arr 0.0) ; marks non-negative elements"
  ^AFArray
  [lhs rhs]
  (assert-within-arrayfire! "ge")
  (cond
    (and (instance? AFArray lhs) (instance? AFArray rhs))
    (arith/ge lhs rhs)
    (instance? AFArray lhs)
    (arith/ge lhs (scalar->array rhs (array/get-type lhs)) true)
    :else
    (arith/ge (scalar->array lhs (array/get-type rhs)) rhs true)))

;;;
;;; Logical and bitwise operations
;;;

(defn not
  "Element-wise logical NOT of a boolean array (b8 → b8).

   Parameters:
   - arr: Input AFArray (b8 boolean array)

   Returns:
   AFArray (b8) with each element negated.
   Requires an active `with-arrayfire` region.

   Example:
   (not (nan-mask arr))   ; marks non-NaN elements"
  ^AFArray
  [^AFArray arr]
  (assert-within-arrayfire! "not")
  (arith/not arr))

(defn logical-and
  "Element-wise logical AND of two boolean arrays (b8).

   Parameters:
   - lhs: Left-hand AFArray (b8)
   - rhs: Right-hand AFArray (b8)

   Returns:
   AFArray (b8) — true where both inputs are non-zero.
   Requires an active `with-arrayfire` region.

   Example:
   (logical-and (ge arr 0.0) (le arr 1.0))  ; elements in [0, 1]"
  ^AFArray
  [^AFArray lhs ^AFArray rhs]
  (assert-within-arrayfire! "logical-and")
  (arith/and lhs rhs))

(defn logical-or
  "Element-wise logical OR of two boolean arrays (b8).

   Parameters:
   - lhs: Left-hand AFArray (b8)
   - rhs: Right-hand AFArray (b8)

   Returns:
   AFArray (b8) — true where either input is non-zero.
   Requires an active `with-arrayfire` region.

   Example:
   (logical-or (nan-mask arr) (inf-mask arr))  ; marks NaN or infinite elements"
  ^AFArray
  [^AFArray lhs ^AFArray rhs]
  (assert-within-arrayfire! "logical-or")
  (arith/or lhs rhs))

(defn zero-mask
  "Element-wise zero check. Returns a b8 boolean array — not a scalar boolean.
   Use `(any (zero-mask arr))` to test whether any element is zero.

   Parameters:
   - arr: Input AFArray

   Returns:
   AFArray (b8) — 1 for zero elements, 0 otherwise.
   Requires an active `with-arrayfire` region.

   Example:
   (zero-mask arr)  ; b8 array marking zero elements"
  ^AFArray
  [^AFArray arr]
  (assert-within-arrayfire! "zero-mask")
  (arith/zero? arr))

(defn bitnot
  "Bitwise NOT of each element.

   Supported integer types: b8 s32 u32 u8 s64 u64 s16 u16.

   Parameters:
   - a: Input AFArray (integer dtype)

   Returns:
   AFArray with bitwise NOT applied.
   Requires an active `with-arrayfire` region."
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "bitnot")
  (arith/bitnot a))

(defn bitand
  "Element-wise bitwise AND.

   Supported integer types: b8 s32 u32 u8 s64 u64 s16 u16.

   Parameters:
   - lhs: Left-hand AFArray (integer dtype)
   - rhs: Right-hand AFArray (integer dtype)

   Returns:
   AFArray with bitwise AND applied.
   Requires an active `with-arrayfire` region."
  ^AFArray
  [^AFArray lhs ^AFArray rhs]
  (assert-within-arrayfire! "bitand")
  (arith/bitand lhs rhs))

(defn bitor
  "Element-wise bitwise OR.

   Supported integer types: b8 s32 u32 u8 s64 u64 s16 u16.

   Parameters:
   - lhs: Left-hand AFArray (integer dtype)
   - rhs: Right-hand AFArray (integer dtype)

   Returns:
   AFArray with bitwise OR applied.
   Requires an active `with-arrayfire` region."
  ^AFArray
  [^AFArray lhs ^AFArray rhs]
  (assert-within-arrayfire! "bitor")
  (arith/bitor lhs rhs))

(defn bitxor
  "Element-wise bitwise XOR.

   Supported integer types: b8 s32 u32 u8 s64 u64 s16 u16.

   Parameters:
   - lhs: Left-hand AFArray (integer dtype)
   - rhs: Right-hand AFArray (integer dtype)

   Returns:
   AFArray with bitwise XOR applied.
   Requires an active `with-arrayfire` region."
  ^AFArray
  [^AFArray lhs ^AFArray rhs]
  (assert-within-arrayfire! "bitxor")
  (arith/bitxor lhs rhs))

(defn bitshiftl
  "Element-wise bitwise left shift.

   Each element in `lhs` is shifted left by the corresponding element in `rhs`.
   Equivalent to `lhs[i] << rhs[i]`.

   Supported integer types: s32 u32 u8 s64 u64 s16 u16.

   Parameters:
   - lhs: Input AFArray (integer dtype) — values to shift
   - rhs: Input AFArray (integer dtype) — shift amounts

   Returns:
   AFArray with left-shifted values.
   Requires an active `with-arrayfire` region."
  ^AFArray
  [^AFArray lhs ^AFArray rhs]
  (assert-within-arrayfire! "bitshiftl")
  (arith/bitshiftl lhs rhs))

(defn bitshiftr
  "Element-wise bitwise right shift.

   Each element in `lhs` is shifted right by the corresponding element in `rhs`.
   Equivalent to `lhs[i] >> rhs[i]`.

   Supported integer types: s32 u32 u8 s64 u64 s16 u16.

   Parameters:
   - lhs: Input AFArray (integer dtype) — values to shift
   - rhs: Input AFArray (integer dtype) — shift amounts

   Returns:
   AFArray with right-shifted values.
   Requires an active `with-arrayfire` region."
  ^AFArray
  [^AFArray lhs ^AFArray rhs]
  (assert-within-arrayfire! "bitshiftr")
  (arith/bitshiftr lhs rhs))

;;;
;;; Extended math
;;;

(defn minof
  "Element-wise minimum of two arrays.

   Parameters:
   - lhs: Left-hand AFArray
   - rhs: Right-hand AFArray

   Returns:
   AFArray with element-wise minimum values.
   Requires an active `with-arrayfire` region.

   Example:
   (minof arr1 arr2)"
  ^AFArray
  [^AFArray lhs ^AFArray rhs]
  (assert-within-arrayfire! "minof")
  (arith/minof lhs rhs))

(defn maxof
  "Element-wise maximum of two arrays.

   Parameters:
   - lhs: Left-hand AFArray
   - rhs: Right-hand AFArray

   Returns:
   AFArray with element-wise maximum values.
   Requires an active `with-arrayfire` region.

   Example:
   (maxof arr1 arr2)"
  ^AFArray
  [^AFArray lhs ^AFArray rhs]
  (assert-within-arrayfire! "maxof")
  (arith/maxof lhs rhs))

(defn hypot
  "Element-wise sqrt(lhs² + rhs²), numerically safe against overflow.

   Parameters:
   - lhs: Left-hand AFArray
   - rhs: Right-hand AFArray

   Returns:
   AFArray with hypotenuse values.
   Requires an active `with-arrayfire` region.

   Example:
   (hypot (array [3.0] [1]) (array [4.0] [1]))  ; => [5.0]"
  ^AFArray
  [^AFArray lhs ^AFArray rhs]
  (assert-within-arrayfire! "hypot")
  (arith/hypot lhs rhs))

(defn root
  "Element-wise nth root: nth-root(x) = x^(1/n).

   Parameters:
   - arr: Input AFArray
   - n: Root degree (Number or AFArray)

   Returns:
   AFArray with nth roots.
   Requires an active `with-arrayfire` region.

   Example:
   (root arr 3.0)  ; cube root"
  ^AFArray
  [^AFArray arr n]
  (assert-within-arrayfire! "root")
  (let [scalar-n? (clojure.core/not (instance? AFArray n))
        n-arr (if (instance? AFArray n) n (scalar->array n (array/get-type arr)))]
    ;; arith/root: lhs = root order, rhs = values; use batch when n is a scalar
    (arith/root n-arr arr scalar-n?)))

;;;
;;; Conditional selection
;;;

(defn select-where
  "Select elements from `a` or `b` based on a boolean condition array.

   Returns `a[i]` where `cond[i]` is non-zero, else `b[i]`.

   Parameters:
   - cond: Boolean AFArray (b8) used as selector
   - a: AFArray for true positions (or scalar Number)
   - b: AFArray for false positions (or scalar Number)

   Returns:
   AFArray with selected elements.
   Requires an active `with-arrayfire` region.

   Example:
   (select-where (ge arr 0.0) arr (neg arr))  ; abs value"
  ^AFArray
  [^AFArray condition a b]
  (assert-within-arrayfire! "select-where")
  (cond
    (and (instance? AFArray a) (instance? AFArray b))
    (data/select condition a b)
    (instance? AFArray a)
    (data/select-scalar-r condition a (double b))
    (instance? AFArray b)
    (data/select-scalar-l condition (double a) b)
    :else
    (data/select condition
                 (constant (double a) (shape condition) (datatype condition))
                 (constant (double b) (shape condition) (datatype condition)))))

(defn replace-where!
  "Replace elements in `arr` with values from `b` wherever `cond` is false (zero).

   Modifies `arr` in-place. Use for mask-based value replacement.

   Parameters:
   - arr: Target AFArray (modified in-place)
   - cond: Boolean AFArray (b8) — elements where cond=0 are replaced
   - b: Replacement AFArray (or scalar Number)

   Returns:
   `arr` (modified in-place).
   Requires an active `with-arrayfire` region.

   Example:
   (replace-where! arr (ge arr 0.0) 0.0)  ; set negatives to zero"
  ^AFArray
  [^AFArray arr ^AFArray condition b]
  (assert-within-arrayfire! "replace-where!")
  (if (instance? AFArray b)
    (data/replace! arr condition b)
    (data/replace-scalar! arr condition (double b)))
  arr)

;;;
;;; Array type predicates
;;;

(defn empty-array?
  "Return true if `arr` has zero elements.
   Requires an active `with-arrayfire` region."
  [^AFArray arr]
  (assert-within-arrayfire! "empty-array?")
  (array/empty? arr))

(defn scalar-array?
  "Return true if `arr` is a scalar (single-element) array.
   Requires an active `with-arrayfire` region."
  [^AFArray arr]
  (assert-within-arrayfire! "scalar-array?")
  (array/scalar? arr))

(defn row-array?
  "Return true if `arr` is a row vector (1×n).
   Requires an active `with-arrayfire` region."
  [^AFArray arr]
  (assert-within-arrayfire! "row-array?")
  (array/row? arr))

(defn column-array?
  "Return true if `arr` is a column vector (n×1).
   Requires an active `with-arrayfire` region."
  [^AFArray arr]
  (assert-within-arrayfire! "column-array?")
  (array/column? arr))

(defn vector-array?
  "Return true if `arr` is a 1-D vector (row or column).
   Requires an active `with-arrayfire` region."
  [^AFArray arr]
  (assert-within-arrayfire! "vector-array?")
  (array/vector? arr))

(defn complex-array?
  "Return true if `arr` has a complex element type (c32 or c64).
   Requires an active `with-arrayfire` region."
  [^AFArray arr]
  (assert-within-arrayfire! "complex-array?")
  (array/complex? arr))

(defn real-array?
  "Return true if `arr` has a real (non-complex) element type.
   Requires an active `with-arrayfire` region."
  [^AFArray arr]
  (assert-within-arrayfire! "real-array?")
  (array/real? arr))

(defn double-array?
  "Return true if `arr` has f64 (double-precision) element type.
   Requires an active `with-arrayfire` region."
  [^AFArray arr]
  (assert-within-arrayfire! "double-array?")
  (array/double? arr))

(defn single-array?
  "Return true if `arr` has f32 (single-precision) element type.
   Requires an active `with-arrayfire` region."
  [^AFArray arr]
  (assert-within-arrayfire! "single-array?")
  (array/single? arr))

(defn half-array?
  "Return true if `arr` has f16 (half-precision) element type.
   Requires an active `with-arrayfire` region."
  [^AFArray arr]
  (assert-within-arrayfire! "half-array?")
  (array/half? arr))

(defn integer-array?
  "Return true if `arr` has an integer element type.
   Requires an active `with-arrayfire` region."
  [^AFArray arr]
  (assert-within-arrayfire! "integer-array?")
  (array/integer? arr))

(defn bool-array?
  "Return true if `arr` has a b8 (boolean) element type.
   Requires an active `with-arrayfire` region."
  [^AFArray arr]
  (assert-within-arrayfire! "bool-array?")
  (array/bool? arr))

(defn sparse-array?
  "Return true if `arr` is a sparse array.
   Requires an active `with-arrayfire` region."
  [^AFArray arr]
  (assert-within-arrayfire! "sparse-array?")
  (array/sparse? arr))

;;;
;;; Device introspection and evaluation
;;;

(defn device-count
  "Number of available compute devices on the active backend.

   Returns:
   Integer number of devices.
   Requires an active `with-arrayfire` region."
  []
  (assert-within-arrayfire! "device-count")
  (device/get-device-count))

(defn available-backends
  "Set of available backend keywords (:cpu, :cuda, :opencl, :oneapi).

   Returns:
   Set of backend keywords for all available backends.
   Requires an active `with-arrayfire` region."
  []
  (assert-within-arrayfire! "available-backends")
  (let [bitmask (device/get-available-backends)]
    (set (keep (fn [[k v]] (when (and (pos? k) (pos? (bit-and bitmask k))) v))
               defs/backend-const->kw))))

(defn backend-device-info
  "Map of information about the currently active device.

   Returns:
   Map with device information.
   Requires an active `with-arrayfire` region."
  []
  (assert-within-arrayfire! "backend-device-info")
  (device/device-info))

(defn eval!
  "Force immediate GPU evaluation of a lazy array (JIT).

   ArrayFire uses lazy evaluation by default; `eval!` forces
   materialization of the computation graph for `arr`.

   Parameters:
   - arr: AFArray to evaluate

   Returns:
   `arr` (same object, now evaluated).
   Requires an active `with-arrayfire` region.

   Example:
   (eval! result-arr)  ; force GPU computation now"
  ^AFArray
  [^AFArray arr]
  (assert-within-arrayfire! "eval!")
  (device/eval-array! arr)
  arr)

(defn eval-multiple!
  "Evaluate multiple arrays simultaneously for GPU efficiency.

   More efficient than calling `eval!` on each array sequentially.
   Allows the JIT compiler to optimise across all arrays in one pass.

   Parameters:
   - arrays: Collection of AFArray instances to evaluate

   Returns:
   nil (side-effecting).
   Requires an active `with-arrayfire` region.

   Example:
   (let [a (+ x y)
         b (* x y)]
     (eval-multiple! [a b]))"
  [arrays]
  (assert-within-arrayfire! "eval-multiple!")
  (device/eval-multiple! arrays))

;;;
;;; Random seed management
;;;

(defn set-random-seed!
  "Set the global random seed for reproducible results.

   Parameters:
   - seed: Long integer seed value

   Returns:
   nil.
   Requires an active `with-arrayfire` region.

   Example:
   (set-random-seed! 42)"
  [seed]
  (assert-within-arrayfire! "set-random-seed!")
  (random/set-seed! seed))

(defn get-random-seed
  "Return the current global random seed.

   Returns:
   Long integer seed value.
   Requires an active `with-arrayfire` region.

   Example:
   (get-random-seed)"
  []
  (assert-within-arrayfire! "get-random-seed")
  (random/get-seed))

;;;
;;; Debugging utilities
;;;

(defn print-array
  "Print array contents to stdout. Useful for REPL debugging.

   Parameters:
   - arr: AFArray to print
   - exp: (optional) Label string to display before the array

   Returns:
   nil.
   Requires an active `with-arrayfire` region.

   Example:
   (print-array arr \"my array\")"
  ([^AFArray arr]
   (assert-within-arrayfire! "print-array")
   (device/info-string)
   (println (->value arr)))
  ([^AFArray arr exp]
   (assert-within-arrayfire! "print-array")
   (println (str exp ":"))
   (println (->value arr))))

(defn array->string
  "Return a formatted string representation of an array.

   Parameters:
   - arr: AFArray to represent

   Returns:
   String representation of the array values.
   Requires an active `with-arrayfire` region.

   Example:
   (array->string arr)"
  [^AFArray arr]
  (assert-within-arrayfire! "array->string")
  (str (->value arr)))

(defn print-array-gen
  "Print an array with a custom label and decimal precision.

   More flexible than `print-array` — allows a descriptive name and
   configurable number of decimal places.

   Parameters:
   - label:     String label displayed above the array
   - arr:       AFArray to print
   - precision: Number of decimal places, 0–16 (default 4)

   Returns:
   nil (prints to stdout).
   Requires an active `with-arrayfire` region.

   Example:
   (print-array-gen \"weights\" w 6)"
  ([label ^AFArray arr]
   (print-array-gen label arr 4))
  ([label ^AFArray arr precision]
   (assert-within-arrayfire! "print-array-gen")
   (util/print-array-gen label arr precision)))

(comment
  ;; with-arrayfire REPL experiments

  ;; Basic usage — explicit host conversion (array returns AFArray)
  (with-arrayfire
    (let [a (array [1.0 2.0 3.0 4.0] [2 2])]
      (vec (array/array->host a 4))))

  ;; Empty opts map (valid — treated as no options)
  (with-arrayfire {}
    (vec (array/array->host (array [1.0 2.0] [2]) 2)))

  ;; With backend selection
  (with-arrayfire {:backend :cpu}
    (let [a (array [1.0 2.0 3.0] [3])]
      (vec (array/array->host a 3))))

  ;; With shared arena for multi-threaded body
  (with-arrayfire {:arena-type :shared}
    (let [f (future (array [1.0 2.0] [2]))]
      (vec (array/array->host @f 2))))

  ;; Introspect the current backend/device frame from inside a switching region
  (with-arrayfire {:backend :cpu :device 0}
    (println "current frame:" (peek *backend-device-stack*))
    (with-arrayfire {:backend :cpu :device 0}
      (println "nested frame:" (peek *backend-device-stack*))
      (println "full stack:" *backend-device-stack*)))

  ;; Nested regions (no backend switch — no frame pushed)
  (with-arrayfire
    (with-arrayfire
      (vec (to-host (array [42.0] [1]) 1))))

  ;; Clojure vector output instead of dtype-next native buffer.
  ;; ArrayFire is column-major, so a [2 3] array returns 3 column vectors of length 2.
  ;; => [[1.0 2.0] [3.0 4.0] [5.0 6.0]]
  (with-arrayfire {:backend      :cpu
                   :converter-fn ->value}
    (let [data (double-array [1.0 2.0 3.0 4.0 5.0 6.0])]
      (array data [2 3] :f64)))

  ;; Indexing and manipulation
  ;; col-major 2×3:  col0=[1,2]  col1=[3,4]  col2=[5,6]
  ;; row0=[1,3,5]    row1=[2,4,6]     element[row,col] at [1,2] = 6.0
  (with-arrayfire {:backend :cpu :converter-fn ->value}
    (let [m (array [1.0 2.0 3.0 4.0 5.0 6.0] [2 3])]

      ;; slice — select row 0 (vector form)
      (->value (slice m [0 nil]))               ; => [[1.0][3.0][5.0]]

      ;; slice — select col 1 (vector form)
      (->value (slice m [nil 1]))               ; => [3.0 4.0]

      ;; slice — rows 0-0, cols 1-2 (map form)
      (->value (slice m {:rows 0 :cols [1 2]})) ; => [[3.0][5.0]]

      ;; at — single element
      (->value (at m [1 2]))                    ; => 6.0

      ;; row / col shortcuts
      (->value (row m 0))                       ; => [[1.0][3.0][5.0]]
      (->value (col m 2))                       ; => [5.0 6.0]

      ;; rows / cols range shortcuts
      (->value (rows m 0 1))                    ; => all rows
      (->value (cols m 0 1))                    ; => [[1.0 2.0][3.0 4.0]]

      ;; select — fancy indexing by integer index array
      (->value (select m [0 2] 1))              ; cols 0 and 2: [[1.0 2.0][5.0 6.0]]

      ;; assoc-slice — functional (original unchanged)
      (let [res (assoc-slice m [0 nil] (array [99.0 98.0 97.0] [1 3]))]
        [(->value res)                          ; row 0 replaced
         (->value (row m 0))])))               ; original row 0 unchanged

  ;
  )

