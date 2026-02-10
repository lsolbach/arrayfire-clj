(ns org.soulspace.arrayfire.integration.unified-api.jit-test-api
  "Integration of the ArrayFire JIT test API related FFI bindings with error
   handling and resource management on the JVM.
   
   Just-In-Time (JIT) compilation is ArrayFire's core optimization that delays
   operation evaluation to enable kernel fusion - combining multiple operations
   into a single optimized kernel for significant performance gains.
   
   This namespace provides control over JIT behavior:
   
   - get-max-jit-len: Query current JIT tree depth limit
   - set-max-jit-len!: Control when automatic evaluation occurs
   
   JIT Tree Concept:
   
   ArrayFire builds an expression tree of unevaluated operations:
   ```
   a = constant(1, 100)
   b = constant(2, 100)
   c = a + b              // Not evaluated yet
   d = c * 2              // Tree grows
   e = sin(d)             // Tree depth = 3
   ```
   
   When tree depth exceeds max-jit-len, ArrayFire compiles and executes
   the accumulated operations automatically.
   
   Trade-offs:
   
   Small max-jit-len (1-5):
   - Frequent evaluation, more kernel launches (slower)
   - Lower memory usage
   - Good for debugging and memory-constrained systems
   
   Large max-jit-len (20-100):
   - Deferred evaluation, fewer kernels (faster)
   - Higher memory usage
   - Better for compute-bound workloads
   - Risk of compilation limits
   
   Default: 20 (balanced for most workloads)
   
   These functions are primarily for testing, benchmarking, and performance
   tuning. Most applications should use the default settings."
  (:require [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.c-api.jit-test-api :as jit-ffi]
            [org.soulspace.arrayfire.integration.base.jvm-integration :as jvm]))

;;;
;;; JIT Control Functions
;;;

(defn get-max-jit-len
  "Get the maximum JIT tree depth for the active backend.
   
   Returns the current maximum depth of the JIT expression tree - the longest
   path from root (final operation) to any leaf (input array).
   
   The depth controls when ArrayFire automatically evaluates expression trees.
   When tree depth reaches this limit, evaluation is triggered.
   
   Returns:
   Integer representing maximum JIT tree depth
   
   Backend defaults (if never modified):
   - CUDA: 20
   - OpenCL: 20
   - CPU: 20
   
   Examples:
   ```clojure
   ;; Query current setting
   (let [depth (get-max-jit-len)]
     (println (str \"Max JIT depth: \" depth)))
   
   ;; Save for later restore
   (let [saved-len (get-max-jit-len)]
     (set-max-jit-len! 5)
     ;; ... do work with modified setting ...
     (set-max-jit-len! saved-len))  ; restore
   
   ;; Check if using default
   (when (= 20 (get-max-jit-len))
     (println \"Using default JIT settings\"))
   ```"
  []
  (let [len-buf (mem/alloc 4)]
    (jvm/check! (jit-ffi/af-get-max-jit-len len-buf)
                "af-get-max-jit-len")
    (mem/read-int len-buf 0)))

(defn set-max-jit-len!
  "Set the maximum JIT tree depth for the active backend.
   
   Controls when ArrayFire automatically evaluates JIT expression trees.
   When tree depth (longest path from root to leaf) reaches this limit,
   evaluation is triggered before adding more operations.
   
   Parameters:
   - jit-len: Maximum tree depth (integer, must be > 0)
   
   Effect:
   - Takes effect immediately
   - Applies to subsequent operations only
   - Per-backend setting (doesn't affect other backends)
   - Thread-local setting
   
   Recommended values:
   
   - 1: Immediate evaluation (no fusion)
     * Use for: debugging, deterministic testing
     * Performance: Slowest (many kernel launches)
   
   - 5-10: Conservative fusion
     * Use for: development, initial optimization
     * Performance: 2-5× speedup typical
   
   - 20: Default (recommended)
     * Use for: most production workloads
     * Performance: 5-10× speedup typical
   
   - 50-100: Aggressive fusion
     * Use for: simple operations, benchmarking
     * Performance: 10-15× speedup possible
     * Risk: May hit compilation limits on complex expressions
   
   Throws:
   Exception if jit-len ≤ 0
   
   Examples:
   ```clojure
   ;; Disable fusion for testing
   (defn test-with-immediate-eval [test-fn]
     (let [saved (get-max-jit-len)]
       (try
         (set-max-jit-len! 1)  ; Force immediate evaluation
         (test-fn)
         (finally
           (set-max-jit-len! saved)))))
   
   ;; Benchmark different JIT lengths
   (defn benchmark-jit-settings [workload-fn]
     (let [saved (get-max-jit-len)
           results (atom {})]
       (doseq [depth [1 5 10 20 50 100]]
         (set-max-jit-len! depth)
         (let [start (System/nanoTime)
               _ (workload-fn)
               elapsed (- (System/nanoTime) start)]
           (swap! results assoc depth elapsed)))
       (set-max-jit-len! saved)
       @results))
   
   ;; Performance tuning for specific workload
   (let [saved (get-max-jit-len)]
     (try
       (set-max-jit-len! 50)  ; Aggressive fusion
       ;; ... compute-intensive operations ...
       (finally
         (set-max-jit-len! saved))))
   ```"
  [jit-len]
  (when (<= jit-len 0)
    (throw (ex-info "JIT length must be positive" {:jit-len jit-len})))
  (jvm/check! (jit-ffi/af-set-max-jit-len (int jit-len))
              "af-set-max-jit-len")
  nil)

;;;
;;; Convenience Functions
;;;

(defn with-jit-len
  "Execute a function with a specific JIT length, restoring the original.
   
   A convenience macro-like function that temporarily changes the JIT length,
   executes the provided function, and restores the original setting.
   
   Parameters:
   - jit-len: Temporary JIT length to use
   - f: Function to execute
   
   Returns:
   Result of executing f
   
   Examples:
   ```clojure
   ;; Run tests with immediate evaluation
   (with-jit-len 1
     (fn []
       (let [a (constant 1.0 [100])
             b (constant 2.0 [100])
             c (add a b)]
         (test-result c))))
   
   ;; Benchmark with aggressive fusion
   (with-jit-len 100
     (fn []
       (run-intensive-computation)))
   ```"
  [jit-len f]
  (let [saved (get-max-jit-len)]
    (try
      (set-max-jit-len! jit-len)
      (f)
      (finally
        (set-max-jit-len! saved)))))

(defmacro with-jit-depth
  "Macro to execute body with a specific JIT depth, restoring original.
   
   Parameters:
   - jit-len: Temporary JIT length to use
   - body: Code to execute
   
   Returns:
   Result of executing body
   
   Examples:
   ```clojure
   ;; Disable fusion for debugging
   (with-jit-depth 1
     (let [a (create-array [1 2 3])
           b (create-array [4 5 6])
           c (add a b)]
       (println \"Result:\" c)))
   
   ;; Performance testing with different depths
   (doseq [depth [1 10 20 50]]
     (println (str \"Testing with depth \" depth))
     (with-jit-depth depth
       (run-workload)))
   ```"
  [jit-len & body]
  `(let [saved# (get-max-jit-len)]
     (try
       (set-max-jit-len! ~jit-len)
       ~@body
       (finally
         (set-max-jit-len! saved#)))))

(defn jit-info
  "Get comprehensive JIT configuration information.
   
   Returns a map with JIT settings and interpretation.
   
   Returns:
   Map containing:
   - :max-jit-len - Current maximum tree depth
   - :fusion-level - Interpretation (:none, :conservative, :default, :aggressive)
   - :recommendation - Usage recommendation
   
   Examples:
   ```clojure
   ;; Display current JIT config
   (let [info (jit-info)]
     (println \"JIT Configuration:\")
     (println (str \"  Max depth: \" (:max-jit-len info)))
     (println (str \"  Fusion level: \" (:fusion-level info)))
     (println (str \"  Recommendation: \" (:recommendation info))))
   
   ;; Check if fusion is disabled
   (when (= :none (:fusion-level (jit-info)))
     (println \"Warning: JIT fusion is disabled\"))
   ```"
  []
  (let [len (get-max-jit-len)
        level (cond
                (<= len 1) :none
                (<= len 10) :conservative
                (<= len 30) :default
                :else :aggressive)
        rec (cond
              (<= len 1) "Fusion disabled - good for debugging, poor performance"
              (<= len 10) "Conservative fusion - safe for development"
              (<= len 30) "Balanced fusion - recommended for production"
              :else "Aggressive fusion - may hit compilation limits")]
    {:max-jit-len len
     :fusion-level level
     :recommendation rec}))
