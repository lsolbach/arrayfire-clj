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
  (:refer-clojure :exclude [+ - * / abs mod rem range])
  (:require [clojure.math]
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
         (device/ensure-af-init!)
         (locking device/backend-lock
           (let [~prev-backend (device/get-active-backend)
                 ~prev-device  (device/get-device)]
             (try
               ~(when has-backend?
                  `(device/set-backend! (defs/resolve-backend ~(:backend opts))))
               ~(when has-device?
                  `(device/set-device! ~(:device opts)))
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
                 ~(when has-device?
                    `(device/set-device! ~prev-device))
                 ~(when has-backend?
                    `(device/set-backend! ~prev-backend)))))))
      ;; No backend/device switching — no lock needed
      `(do
         (device/ensure-af-init!)
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
                      (sequential? values) (double-array (flatten values))
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

(defn random-uniform
  "Create an ArrayFire array filled with uniformly distributed random values.

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
   (random/randu (normalize-dims dims)
                 (defs/resolve-dtype dtype))))

(defn random-normal
  "Create an ArrayFire array filled with normally distributed random values.

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
   (random/randn (normalize-dims dims)
                 (defs/resolve-dtype dtype))))


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

;;;
;;; Basic arithmetic functions
;;;

;;
;; Helper
;;

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
    (arith/pow (scalar->array lhs (array/get-type rhs)) rhs true))))

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
;; Predicates
;;
(defn nan?
  "Element-wise NaN check. Returns a boolean array (b8) indicating NaN elements.

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: input array (AFArray)

   Returns:
   AFArray of b8 (boolean) where true (1) marks NaN positions.
   Requires an active `with-arrayfire` region.

   Example:
   (nan? (array [1.0 Double/NaN 3.0] [3])) ; => [0 1 0]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "nan?")
  (arith/nan? a))

(defn inf?
  "Element-wise infinity check. Returns a boolean array (b8) indicating infinite elements.

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: input array (AFArray)

   Returns:
   AFArray of b8 (boolean) where true (1) marks infinite positions.
   Requires an active `with-arrayfire` region.

   Example:
   (inf? (array [1.0 Double/POSITIVE_INFINITY 3.0] [3])) ; => [0 1 0]"
  ^AFArray
  [^AFArray a]
  (assert-within-arrayfire! "inf?")
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

(comment
  ;; with-arrayfire REPL experiments

  ;; Basic usage — explicit host conversion (array returns AFArray)
  (with-arrayfire
    (let [a (array [1.0 2.0 3.0 4.0] [2 2])]
      (vec (to-host a 4))))

  ;; Empty opts map (valid — treated as no options)
  (with-arrayfire {}
    (vec (to-host (array [1.0 2.0] [2]) 2)))

  ;; With backend selection
  (with-arrayfire {:backend :cpu}
    (let [a (array [1.0 2.0 3.0] [3])]
      (vec (to-host a 3))))

  ;; With shared arena for multi-threaded body
  (with-arrayfire {:arena-type :shared}
    (let [f (future (array [1.0 2.0] [2]))]
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
      (vec (to-host (array [42.0] [1]) 1))))

  ;; Clojure vector output instead of dtype-next native buffer.
  ;; ArrayFire is column-major, so a [2 3] array returns 3 column vectors of length 2.
  ;; => [[1.0 2.0] [3.0 4.0] [5.0 6.0]]
  (with-arrayfire {:backend      :cpu
                   :converter-fn ->value}
    (let [data (double-array [1.0 2.0 3.0 4.0 5.0 6.0])]
      (array data [2 3] :f64)))

  ;
  )

