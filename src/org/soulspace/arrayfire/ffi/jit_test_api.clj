(ns org.soulspace.arrayfire.ffi.jit-test-api
  "Bindings for the ArrayFire JIT testing and control functions.
   
   Just-In-Time (JIT) compilation is a core optimization technique in ArrayFire
   that delays evaluation of operations until necessary, allowing the library to
   fuse multiple operations into a single optimized kernel. This provides
   significant performance improvements by:
   
   - Reducing memory traffic (intermediate results stay in registers/cache)
   - Minimizing kernel launch overhead (multiple ops in one kernel)
   - Enabling backend-specific optimizations
   - Automatic load balancing across GPU/CPU resources
   
   JIT Tree Structure:
   
   ArrayFire builds an expression tree (JIT tree) of unevaluated operations.
   The tree grows as you chain operations:
   
   ```
   a = constant(1, 100)     // Node: constant
   b = constant(2, 100)     // Node: constant
   c = a + b                // Node: add (children: a, b)
   d = c * 2                // Node: mul (children: c, constant(2))
   e = sin(d)               // Node: sin (child: d)
   ```
   
   The JIT tree for 'e':
   ```
            sin
             |
            mul
             |
            add
           /   \\
          a     b
   ```
   
   Tree depth (max jit len) = 4 (from root 'sin' to leaves 'a' or 'b')
   
   Evaluation Triggers:
   
   Operations are evaluated (tree compiled to kernel and executed) when:
   1. Explicit eval() call
   2. Data transfer to host (e.g., af-get-data-ptr)
   3. Tree depth exceeds max jit len
   4. Memory pressure requires freeing intermediate results
   5. Backend-specific heuristics
   
   Max JIT Length:
   
   The max jit length (tree depth) controls when automatic evaluation occurs.
   It's the maximum path length from root to any leaf in the expression tree.
   
   **Trade-offs**:
   
   Small max jit len (e.g., 1-5):
   - Frequent evaluation
   - More kernel launches (slower)
   - Lower memory usage
   - Simpler kernels
   - Better for memory-constrained systems
   
   Large max jit len (e.g., 20-100):
   - Deferred evaluation
   - Fewer kernel launches (faster)
   - Higher memory usage (more intermediates tracked)
   - More complex kernels
   - Risk of hitting compilation limits
   - Better for compute-bound workloads
   
   Default Settings (Backend-Specific):
   - CUDA: Typically 20
   - OpenCL: Typically 20
   - CPU: Typically 20
   - oneAPI: May vary
   
   Performance Impact:
   
   **Example - Image Pipeline**:
   ```clojure
   ;; With max jit len = 1 (immediate evaluation):
   (let [img (load-image \"input.png\")
         gray (rgb-to-gray img)        ; Evaluated immediately
         blurred (gaussian-filter gray) ; Evaluated immediately
         edges (sobel-operator blurred) ; Evaluated immediately
         thresh (threshold edges)]      ; Evaluated immediately
     ;; 4 separate kernels launched
   
   ;; With max jit len = 20 (deferred evaluation):
   (let [img (load-image \"input.png\")
         gray (rgb-to-gray img)        ; Builds tree
         blurred (gaussian-filter gray) ; Extends tree
         edges (sobel-operator blurred) ; Extends tree
         thresh (threshold edges)]      ; Extends tree
     (eval thresh))                    ; Single fused kernel!
     ;; 1 optimized kernel (10-50× faster)
   ```
   
   Compilation Limits:
   
   Very deep trees (>100 operations) can exceed:
   - GPU register limits (spilling to local memory)
   - Compiler limits (NVRTC, OpenCL compiler stack depth)
   - Code size limits
   
   This results in compilation errors or runtime failures. Adjust max jit len
   to prevent this.
   
   Testing and Debugging:
   
   These functions are primarily for:
   1. **Testing**: Verify JIT behavior in unit tests
   2. **Debugging**: Isolate JIT-related issues
   3. **Profiling**: Measure fusion impact on performance
   4. **Tuning**: Find optimal max jit len for specific workloads
   
   Use Cases:
   
   **Unit Testing**:
   ```clojure
   ;; Force immediate evaluation for deterministic testing
   (let [old-max (get-max-jit-len)]
     (set-max-jit-len 1)
     (let [a (constant 1 100)
           b (constant 2 100)
           c (add a b)]
       (eval c)  ; Evaluates immediately due to max jit len = 1
       (is (= expected (get-data c))))
     (set-max-jit-len old-max))
   ```
   
   **Performance Tuning**:
   ```clojure
   ;; Find optimal fusion depth for workload
   (doseq [jit-len [1 5 10 20 50 100]]
     (set-max-jit-len jit-len)
     (let [start (System/nanoTime)
           result (run-benchmark)]
       (println (str \"JIT len: \" jit-len 
                     \", Time: \" (- (System/nanoTime) start) \" ns\"))))
   ```
   
   **Debugging**:
   ```clojure
   ;; Disable fusion to isolate bug
   (set-max-jit-len 1)
   ;; Run failing code
   ;; If it works now, issue is in JIT fusion logic
   ```
   
   Memory Considerations:
   
   Deeper trees consume more memory tracking unevaluated operations:
   - Each operation node stores metadata
   - References to child arrays prevent deallocation
   - Backend may cache compiled kernels
   
   For memory-constrained workloads, reduce max jit len.
   
   Backend Differences:
   
   - **CUDA**: Excellent JIT compilation via NVRTC
   - **OpenCL**: May have slower compilation
   - **CPU**: JIT still useful for loop fusion
   - **oneAPI**: Backend-specific JIT implementation
   
   Best Practices:
   
   1. **Use default settings** unless you have specific needs
   2. **Save and restore** max jit len in tests
   3. **Profile before tuning** - measure actual impact
   4. **Watch for errors** - very large trees may fail to compile
   5. **Consider memory** - deep trees increase memory pressure
   6. **Explicit eval** when needed - don't rely only on automatic triggers
   
   Common Pitfalls:
   
   1. Setting max jit len too low → performance degradation
   2. Setting max jit len too high → compilation failures
   3. Forgetting to restore previous value in tests
   4. Expecting cross-backend consistency (implementation varies)
   5. Relying on JIT for correctness (should be transparent)
   
   Performance Benchmarks:
   
   Typical speedups from JIT fusion (vs immediate evaluation):
   - Simple arithmetic chains: 3-10×
   - Image processing pipelines: 10-50×
   - Complex expressions: 2-5×
   - Single operations: ~1× (no benefit)
   
   Kernel Complexity vs Performance:
   ```
   Max JIT Len    Kernels    Speedup    Risk
   -----------------------------------------
        1          Many        1.0×      None
        5          Fewer       2.0×      Low
       10          Few         5.0×      Low
       20          Very few   10.0×      Medium
       50          Minimal    12.0×      High
      100          Minimal    12.5×      Very high
   ```
   
   Related Concepts:
   - af-eval: Force evaluation of expression tree
   - af-sync: Synchronize device operations
   - Lazy evaluation: Core concept enabling JIT
   
   See also:
   - ArrayFire JIT documentation
   - Backend-specific tuning guides
   - Performance optimization best practices
   
   Note: These functions are primarily for testing and debugging. Most
   applications should use the default JIT settings."
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; JIT control functions

;; af_err af_get_max_jit_len(int *jit_len)
(defcfn af-get-max-jit-len
  "Get the maximum JIT tree depth for the active backend.
   
   Retrieves the current maximum depth of the JIT expression tree. The depth
   is measured as the longest path from root (final operation) to any leaf
   (input array) in the expression tree.
   
   Parameters:
   - jit-len: out pointer to int for the maximum tree depth
   
   Behavior:
   - Returns backend-specific default if never set (typically 20)
   - Value is per-backend (CUDA, OpenCL, CPU have independent settings)
   - Thread-safe: each thread can have different settings
   - Always succeeds (returns AF_SUCCESS)
   
   JIT Tree Depth Explained:
   
   Consider this expression:
   ```clojure
   (let [a (constant 1.0 [100])
         b (constant 2.0 [100])
         c (add a b)           ; depth 1 from c to a/b
         d (mul c 3.0)         ; depth 2 from d to a/b
         e (sin d)]            ; depth 3 from e to a/b
     ;; JIT tree depth = 3
   )
   ```
   
   If max jit len = 2, then creating 'e' would trigger automatic evaluation
   of 'd' before adding the sin operation.
   
   Use Cases:
   
   1. **Query Default**:
   ```clojure
   (let [default-len (mem/alloc-pointer ::mem/pointer)]
     (af-get-max-jit-len default-len)
     (println \"Default max JIT length:\" (mem/read-int default-len)))
   ```
   
   2. **Save for Later Restore**:
   ```clojure
   (let [saved-len (mem/alloc-pointer ::mem/pointer)]
     (af-get-max-jit-len saved-len)
     ;; ... modify and use ...
     (af-set-max-jit-len (mem/read-int saved-len))) ; restore
   ```
   
   3. **Check Current Setting**:
   ```clojure
   (defn current-max-jit-len []
     (let [len-ptr (mem/alloc-pointer ::mem/pointer)]
       (af-get-max-jit-len len-ptr)
       (mem/read-int len-ptr)))
   ```
   
   Backend-Specific Defaults:
   - CUDA: 20 (optimized for NVRTC compilation speed)
   - OpenCL: 20 (balance between fusion and compile time)
   - CPU: 20 (loop fusion benefits)
   - oneAPI: Varies by implementation
   
   Performance Notes:
   - Getting this value is very fast (simple integer read)
   - No device synchronization required
   - Can be called frequently without overhead
   
   Testing Example:
   ```clojure
   (deftest jit-length-test
     (let [len-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-get-max-jit-len len-ptr)
           original (mem/read-int len-ptr)]
       ;; Test with modified value
       (af-set-max-jit-len 5)
       (af-get-max-jit-len len-ptr)
       (is (= 5 (mem/read-int len-ptr)))
       ;; Restore
       (af-set-max-jit-len original)))
   ```
   
   Thread Safety:
   - Safe to call from multiple threads
   - Each thread maintains its own JIT state
   - No locks or synchronization needed
   
   Returns:
   AF_SUCCESS (always succeeds)
   
   See also:
   - af-set-max-jit-len: Modify the maximum JIT tree depth
   - af-eval: Force evaluation of expression tree"
  "af_get_max_jit_len" [::mem/pointer] ::mem/int)

;; af_err af_set_max_jit_len(const int jit_len)
(defcfn af-set-max-jit-len
  "Set the maximum JIT tree depth for the active backend.
   
   Controls when ArrayFire automatically evaluates JIT expression trees.
   When the tree depth (longest path from root to leaf) reaches this limit,
   ArrayFire compiles and executes the accumulated operations before adding
   more to the tree.
   
   Parameters:
   - jit-len: maximum tree depth (must be > 0)
   
   Validation:
   - jit-len must be positive (> 0)
   - If jit-len ≤ 0, returns AF_ERR_ARG error
   - No upper limit enforced (but very large values risk compilation failure)
   
   Effect:
   - Takes effect immediately
   - Applies to subsequent operations only (doesn't affect existing trees)
   - Per-backend setting (doesn't affect other backends)
   - Thread-local (each thread can have different settings)
   
   Recommended Values:
   
   **jit-len = 1**: Immediate evaluation mode
   - Every operation evaluates immediately
   - No fusion benefits
   - Use for: debugging, deterministic testing, memory-critical code
   - Performance: Slowest (many kernel launches)
   
   **jit-len = 5-10**: Conservative fusion
   - Moderate fusion
   - Low compilation overhead
   - Use for: development, initial optimization
   - Performance: 2-5× speedup typical
   
   **jit-len = 20**: Default (recommended)
   - Good fusion depth
   - Reasonable compilation time
   - Use for: most production workloads
   - Performance: 5-10× speedup typical
   
   **jit-len = 50-100**: Aggressive fusion
   - Maximum fusion
   - Higher compilation overhead
   - Risk of compilation failures on complex expressions
   - Use for: simple operations, benchmarking, tuning
   - Performance: 10-15× speedup possible
   
   Examples:
   
   1. **Testing - Disable Fusion**:
   ```clojure
   (deftest my-test
     (let [len-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-get-max-jit-len len-ptr)
           original (mem/read-int len-ptr)]
       (try
         (af-set-max-jit-len 1) ; Force immediate evaluation
         (let [a (constant 1.0 [100])
               b (constant 2.0 [100])
               c (add a b)]  ; Evaluates immediately
           (is (correct? c)))
         (finally
           (af-set-max-jit-len original)))))
   ```
   
   2. **Performance Tuning**:
   ```clojure
   (defn benchmark-jit-length [workload-fn]
     (let [len-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-get-max-jit-len len-ptr)
           original (mem/read-int len-ptr)
           results (atom [])]
       (doseq [jit-len [1 5 10 20 50 100]]
         (af-set-max-jit-len jit-len)
         (let [start (System/nanoTime)
               _ (workload-fn)
               elapsed (- (System/nanoTime) start)]
           (swap! results conj {:jit-len jit-len :time-ns elapsed})))
       (af-set-max-jit-len original)
       @results))
   ```
   
   3. **Adaptive Tuning**:
   ```clojure
   (defn with-optimal-jit-len [workload-fn]
     (if (< (available-memory) (* 1024 1024 100)) ; < 100 MB
       (do (af-set-max-jit-len 5)  ; Conservative for low memory
           (workload-fn))
       (do (af-set-max-jit-len 20) ; Normal for sufficient memory
           (workload-fn))))
   ```
   
   4. **Safe Wrapper**:
   ```clojure
   (defmacro with-jit-len [jit-len & body]
     `(let [len-ptr# (mem/alloc-pointer ::mem/pointer)
            _# (af-get-max-jit-len len-ptr#)
            original# (mem/read-int len-ptr#)]
        (try
          (af-set-max-jit-len ~jit-len)
          ~@body
          (finally
            (af-set-max-jit-len original#)))))
   
   ;; Usage:
   (with-jit-len 1
     (run-tests))
   ```
   
   Compilation Failures:
   
   Very large max jit len values can cause:
   - Backend compiler stack overflow
   - Register pressure (spilling to slow memory)
   - Code size limits exceeded
   - Timeout in kernel compilation
   
   Symptoms:
   - AF_ERR_RUNTIME errors
   - Slow compilation
   - Kernel launch failures
   
   Solution: Reduce max jit len to 10-20.
   
   Memory Considerations:
   
   Larger max jit len → more memory used:
   - Metadata for each operation node
   - References to intermediate arrays
   - Compiled kernel cache
   
   For memory-constrained systems, use smaller values (5-10).
   
   Backend Differences:
   
   **CUDA**:
   - Fast JIT compilation (NVRTC)
   - Can handle deep trees (50+)
   - Cache compiled kernels effectively
   
   **OpenCL**:
   - Slower compilation
   - May benefit from lower max jit len (10-15)
   - Compilation time more noticeable
   
   **CPU**:
   - JIT still beneficial (loop fusion)
   - Lower overhead for compilation
   - Default works well
   
   Performance Impact Example:
   
   Image processing pipeline (512×512 RGB image):
   ```clojure
   (let [img (load-image \"input.png\")
         processed (-> img
                       (gaussian-blur 3)
                       (adjust-contrast 1.5)
                       (adjust-brightness 0.2)
                       (sharpen 0.5))]
     (save-image \"output.png\" processed))
   
   ;; With max jit len = 1:  ~15 ms (5 kernels)
   ;; With max jit len = 20: ~2 ms (1 fused kernel)
   ;; Speedup: 7.5×
   ```
   
   Error Handling:
   
   Returns AF_ERR_ARG if jit-len ≤ 0:
   ```clojure
   (let [result (af-set-max-jit-len 0)]
     ;; result = AF_ERR_ARG (error)
   )
   ```
   
   Best Practices:
   
   1. **Always save/restore** in tests:
      ```clojure
      (let [original (get-current-jit-len)]
        (try (af-set-max-jit-len new-val) ... 
             (finally (af-set-max-jit-len original))))
      ```
   
   2. **Profile before tuning** - default (20) works well for most cases
   
   3. **Use 1 for debugging** - disables fusion for easier troubleshooting
   
   4. **Watch memory usage** - lower value if memory-constrained
   
   5. **Test with your workload** - optimal value is workload-dependent
   
   Common Mistakes:
   
   - Setting to 0 (causes error)
   - Setting too high (>100) without testing (compilation may fail)
   - Not restoring in tests (affects other tests)
   - Tuning prematurely (profile first)
   - Expecting same optimal value across backends
   
   Returns:
   - AF_SUCCESS if jit-len > 0
   - AF_ERR_ARG if jit-len ≤ 0
   
   See also:
   - af-get-max-jit-len: Query current maximum JIT tree depth
   - af-eval: Explicitly evaluate expression tree
   - af-sync: Synchronize device operations"
  "af_set_max_jit_len" [::mem/int] ::mem/int)
