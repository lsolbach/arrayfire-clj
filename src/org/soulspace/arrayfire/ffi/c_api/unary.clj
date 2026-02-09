(ns org.soulspace.arrayfire.ffi.c-api.unary
  "Bindings for the ArrayFire unary mathematical functions.
   
   Unary operations transform arrays element-wise, applying mathematical
   functions to each element independently. ArrayFire provides comprehensive
   coverage of standard mathematical functions including:
   
   - **Rounding Functions**: trunc, round, floor, ceil, sign
   - **Exponential & Logarithmic**: exp, log, log10, log2, log1p, expm1
   - **Power Functions**: sqrt, cbrt, rsqrt, pow2
   - **Trigonometric**: sin, cos, tan (and hyperbolic sinh, cosh, tanh)
   - **Inverse Trigonometric**: asin, acos, atan (and hyperbolic variants)
   - **Special Functions**: sigmoid, erf, erfc, tgamma, lgamma, factorial
   - **Logical Checks**: isinf, isnan, iszero
   - **Bitwise**: bitnot
   - **Complex**: arg (phase angle)
   
   All functions operate element-wise and preserve array dimensions.
   
   Type Support:
   
   Most unary functions work with:
   - Floating-point types: f32, f64, f16
   - Complex types (where applicable): c32, c64
   - Some functions support integer types
   
   Complex Number Support:
   
   Functions marked as UNARY_COMPLEX support complex inputs and outputs:
   - Trigonometric and hyperbolic functions extend naturally to complex plane
   - Implementation uses standard complex analysis formulas
   - Example: sin(a + ib) = sin(a)cosh(b) + i*cos(a)sinh(b)
   
   GPU Acceleration:
   
   All unary operations are highly parallelized on GPU:
   - Each element processed independently
   - Memory-bandwidth limited (not compute-limited)
   - Typical speedup: 10-100× vs CPU
   - Performance: ~1-10 GB/s depending on GPU
   
   Performance Characteristics:
   
   - Time Complexity: O(N) where N = number of elements
   - Space Complexity: O(N) for output array
   - Memory Bandwidth: Dominant factor (read input + write output)
   - Compute: Minimal (single operation per element)
   
   For an array of size N:
   - Memory Transfer: 2N × sizeof(type) (read + write)
   - Compute: N operations
   - Bandwidth Limited: Performance depends on memory speed
   
   Mathematical Precision:
   
   **f32 (single precision)**:
   - Relative error: ~10⁻⁷ (7 decimal digits)
   - Fast hardware implementation
   - Sufficient for most applications
   
   **f64 (double precision)**:
   - Relative error: ~10⁻¹⁶ (16 decimal digits)
   - Essential for numerical stability in some algorithms
   - 2-32× slower than f32 on GPUs
   
   **f16 (half precision)**:
   - Relative error: ~10⁻³ (3-4 decimal digits)
   - 2-8× faster than f32 on modern GPUs
   - Suitable for deep learning inference
   
   Common Use Cases:
   
   **Machine Learning**:
   - sigmoid: Activation function for neural networks
   - exp, log: Softmax, cross-entropy loss
   - sqrt, rsqrt: Normalization layers
   - tanh: Alternative activation function
   
   **Signal Processing**:
   - sin, cos: Wave generation, modulation
   - sqrt: RMS calculations, magnitude
   - log: Decibel conversions, spectral analysis
   - Complex functions: Fourier analysis
   
   **Scientific Computing**:
   - exp, log: Differential equations, growth models
   - tgamma, lgamma: Statistics, probability distributions
   - erf, erfc: Normal distribution, error analysis
   - Trigonometric: Geometry, oscillations
   
   **Image Processing**:
   - sqrt: Gamma correction
   - log: Dynamic range compression
   - exp: Contrast enhancement
   - floor, ceil, round: Quantization
   
   **Financial Mathematics**:
   - exp, log: Compound interest, returns
   - erf: Black-Scholes option pricing
   - log1p, expm1: Stable small value calculations
   
   Numerical Stability Considerations:
   
   Some functions have numerically stable variants:
   
   **log1p(x) vs log(1+x)**:
   - log1p(x) = log(1+x) but more accurate for small |x|
   - Use when x is close to zero
   - Avoids catastrophic cancellation
   
   **expm1(x) vs exp(x)-1**:
   - expm1(x) = exp(x) - 1 but more accurate for small |x|
   - Use when result is close to zero
   - Critical for numerical stability
   
   **rsqrt(x) vs 1/sqrt(x)**:
   - rsqrt(x) = 1/sqrt(x) but potentially faster
   - Hardware accelerated on some GPUs
   - Use in normalization operations
   
   Special Function Properties:
   
   **Gamma Functions**:
   - tgamma(n) = (n-1)! for positive integers
   - lgamma(x) = log(|tgamma(x)|) more stable
   - Used in statistics and probability
   
   **Error Functions**:
   - erf(x): Error function, cumulative normal distribution
   - erfc(x) = 1 - erf(x) but more accurate for large x
   - Range: erf ∈ [-1, 1], erfc ∈ [0, 2]
   
   **Sigmoid Function**:
   - sigmoid(x) = 1 / (1 + exp(-x))
   - Maps (-∞, ∞) to (0, 1)
   - Smooth, differentiable activation function
   - Gradient: sigmoid(x) × (1 - sigmoid(x))
   
   Complex Number Formulas:
   
   For z = a + ib:
   
   **Exponential**:
   exp(z) = exp(a) × (cos(b) + i×sin(b))
   
   **Logarithm**:
   log(z) = log(|z|) + i×arg(z)
   
   **Sine**:
   sin(z) = sin(a)cosh(b) + i×cos(a)sinh(b)
   
   **Cosine**:
   cos(z) = cos(a)cosh(b) - i×sin(a)sinh(b)
   
   **Square Root**:
   sqrt(z) = sqrt(|z|) × exp(i×arg(z)/2)
   
   **Inverse Functions**:
   - Use complex logarithm and square root
   - May have branch cuts (discontinuities)
   - Principal branch returned
   
   Branch Cuts:
   
   Complex functions have branch cuts where discontinuous:
   - log(z): Negative real axis
   - sqrt(z): Negative real axis
   - asin(z), acos(z): Real axis outside [-1, 1]
   - atanh(z): Real axis outside (-1, 1)
   
   Be aware of branch cuts when working with complex arrays.
   
   Pattern Examples:
   
   **Pattern 1: Softmax Activation**
   ```clojure
   (defn softmax [x]
     (let [exp-x-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-exp exp-x-ptr x)
           exp-x (mem/read-pointer exp-x-ptr ::mem/pointer)
           sum-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-sum sum-ptr exp-x 0)  ; Sum along dim 0
           sum (mem/read-pointer sum-ptr ::mem/pointer)
           result-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-div result-ptr exp-x sum false)]
       (mem/read-pointer result-ptr ::mem/pointer)))
   ```
   
   **Pattern 2: Numerically Stable Log-Sum-Exp**
   ```clojure
   (defn log-sum-exp [x]
     (let [max-x-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-max max-x-ptr x 0)
           max-x (mem/read-pointer max-x-ptr ::mem/pointer)
           ;; x - max(x)
           diff-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-sub diff-ptr x max-x false)
           diff (mem/read-pointer diff-ptr ::mem/pointer)
           ;; exp(x - max(x))
           exp-diff-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-exp exp-diff-ptr diff)
           exp-diff (mem/read-pointer exp-diff-ptr ::mem/pointer)
           ;; sum(exp(x - max(x)))
           sum-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-sum sum-ptr exp-diff 0)
           sum (mem/read-pointer sum-ptr ::mem/pointer)
           ;; log(sum(exp(x - max(x)))) + max(x)
           log-sum-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-log log-sum-ptr sum)
           log-sum (mem/read-pointer log-sum-ptr ::mem/pointer)
           result-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-add result-ptr log-sum max-x false)]
       (mem/read-pointer result-ptr ::mem/pointer)))
   ```
   
   **Pattern 3: RMS (Root Mean Square)**
   ```clojure
   (defn rms [x]
     (let [;; x^2
           sq-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-pow sq-ptr x 2.0 false)
           sq (mem/read-pointer sq-ptr ::mem/pointer)
           ;; mean(x^2)
           mean-sq-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-mean mean-sq-ptr sq 0)
           mean-sq (mem/read-pointer mean-sq-ptr ::mem/pointer)
           ;; sqrt(mean(x^2))
           rms-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-sqrt rms-ptr mean-sq)]
       (mem/read-pointer rms-ptr ::mem/pointer)))
   ```
   
   **Pattern 4: Complex Magnitude and Phase**
   ```clojure
   (defn magnitude-phase [z]
     (let [;; |z|
           mag-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-abs mag-ptr z)
           mag (mem/read-pointer mag-ptr ::mem/pointer)
           ;; arg(z)
           phase-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-arg phase-ptr z)
           phase (mem/read-pointer phase-ptr ::mem/pointer)]
       {:magnitude mag
        :phase phase}))
   ```
   
   Best Practices:
   
   1. **Choose appropriate precision**:
      - f32 for general computing
      - f64 for numerical algorithms requiring stability
      - f16 for memory-bound operations (deep learning)
   
   2. **Use stable variants**:
      - log1p for log(1+x) when x is small
      - expm1 for exp(x)-1 when x is small
      - erfc for 1-erf(x) when x is large
   
   3. **Batch operations**:
      - Apply to entire arrays at once
      - Leverage GPU parallelism
      - Avoid element-by-element loops
   
   4. **Memory management**:
      - Release intermediate arrays
      - Reuse buffers when possible
      - Monitor GPU memory usage
   
   5. **Numerical awareness**:
      - Check for overflow/underflow
      - Use isinf, isnan, iszero for validation
      - Consider dynamic range of data
   
   6. **Complex numbers**:
      - Be aware of branch cuts
      - Use arg for phase calculations
      - Consider principal value conventions
   
   Limitations:
   
   - Some functions require floating-point types
   - Integer inputs auto-promoted to float
   - Complex functions only support c32/c64
   - Branch cuts in complex domain
   - Hardware precision limits (f16 < f32 < f64)
   
   See also:
   - Binary arithmetic operations (arith.clj)
   - Reduction operations (reduce.clj)
   - Complex number operations (complex.clj)
   - Statistical functions (statistics.clj)"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; Rounding functions

(defcfn af-trunc
  "Truncate array elements towards zero.
   
   Removes fractional part, rounding towards zero. For positive numbers,
   equivalent to floor. For negative numbers, equivalent to ceil.
   
   Parameters:
   - out: Output pointer for truncated array
   - in: Input array
   
   Operation:
   - trunc(3.7) = 3.0
   - trunc(-3.7) = -3.0
   - trunc(5.0) = 5.0
   
   Type Support: f32, f64, f16
   
   Example:
   ```clojure
   (let [x (create-array [3.7 -2.3 5.0] [3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-trunc out-ptr x)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)  ; [3.0 -2.0 5.0]
   ```
   
   Use Cases:
   - Integer conversion (removing decimals)
   - Fixed-point arithmetic
   - Quantization towards zero
   
   Returns: AF_SUCCESS or error code"
  "af_trunc" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-sign
  "Compute sign of array elements.
   
   Returns -1 for negative, 0 for zero, +1 for positive values.
   
   Parameters:
   - out: Output pointer for sign array
   - in: Input array
   
   Operation:
   - sign(x) = -1 if x < 0
   - sign(x) = 0 if x = 0
   - sign(x) = +1 if x > 0
   
   Type Support: f32, f64, f16
   
   Example:
   ```clojure
   (let [x (create-array [3.7 -2.3 0.0 -0.0] [4])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-sign out-ptr x)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)  ; [1.0 -1.0 0.0 0.0]
   ```
   
   Use Cases:
   - Direction indicators
   - Activation functions (sign function)
   - Comparison results
   
   Returns: AF_SUCCESS or error code"
  "af_sign" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-round
  "Round array elements to nearest integer.
   
   Rounds to nearest integer. Halfway cases round away from zero.
   
   Parameters:
   - out: Output pointer for rounded array
   - in: Input array
   
   Operation:
   - round(3.4) = 3.0
   - round(3.5) = 4.0
   - round(3.6) = 4.0
   - round(-3.5) = -4.0
   
   Type Support: f32, f64, f16
   
   Example:
   ```clojure
   (let [x (create-array [3.4 3.5 3.6 -3.5] [4])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-round out-ptr x)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)  ; [3.0 4.0 4.0 -4.0]
   ```
   
   Use Cases:
   - Nearest integer conversion
   - Quantization
   - Rounding for display
   
   Returns: AF_SUCCESS or error code"
  "af_round" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-floor
  "Round array elements down to nearest integer.
   
   Always rounds towards negative infinity.
   
   Parameters:
   - out: Output pointer for floored array
   - in: Input array
   
   Operation:
   - floor(3.7) = 3.0
   - floor(3.0) = 3.0
   - floor(-3.2) = -4.0
   
   Type Support: f32, f64, f16
   
   Example:
   ```clojure
   (let [x (create-array [3.7 3.0 -3.2] [3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-floor out-ptr x)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)  ; [3.0 3.0 -4.0]
   ```
   
   Use Cases:
   - Integer conversion (always down)
   - Binning, histogram indices
   - Tile calculations
   
   Returns: AF_SUCCESS or error code"
  "af_floor" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-ceil
  "Round array elements up to nearest integer.
   
   Always rounds towards positive infinity.
   
   Parameters:
   - out: Output pointer for ceiled array
   - in: Input array
   
   Operation:
   - ceil(3.2) = 4.0
   - ceil(3.0) = 3.0
   - ceil(-3.7) = -3.0
   
   Type Support: f32, f64, f16
   
   Example:
   ```clojure
   (let [x (create-array [3.2 3.0 -3.7] [3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-ceil out-ptr x)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)  ; [4.0 3.0 -3.0]
   ```
   
   Use Cases:
   - Integer conversion (always up)
   - Buffer size calculations
   - Ceiling division
   
   Returns: AF_SUCCESS or error code"
  "af_ceil" [::mem/pointer ::mem/pointer] ::mem/int)

;; Activation and special functions

(defcfn af-sigmoid
  "Compute sigmoid (logistic) function element-wise.
   
   Sigmoid function: σ(x) = 1 / (1 + exp(-x))
   Maps (-∞, ∞) to (0, 1).
   
   Parameters:
   - out: Output pointer for sigmoid array
   - in: Input array
   
   Operation:
   - sigmoid(x) = 1 / (1 + e^(-x))
   - sigmoid(0) = 0.5
   - sigmoid(∞) → 1
   - sigmoid(-∞) → 0
   
   Properties:
   - Range: (0, 1)
   - Symmetric: σ(-x) = 1 - σ(x)
   - Derivative: σ'(x) = σ(x)(1 - σ(x))
   
   Type Support: f32, f64, f16
   
   Example:
   ```clojure
   (let [x (create-array [-2.0 -1.0 0.0 1.0 2.0] [5])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-sigmoid out-ptr x)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)  ; [0.119 0.269 0.5 0.731 0.881]
   ```
   
   Use Cases:
   - Neural network activation
   - Binary classification output
   - Probability estimates
   - Smooth thresholding
   
   Numerical Notes:
   - Numerically stable implementation
   - Avoids overflow for large |x|
   
   Returns: AF_SUCCESS or error code"
  "af_sigmoid" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-expm1
  "Compute exp(x) - 1 with numerical stability for small x.
   
   More accurate than exp(x) - 1 when x is close to zero.
   
   Parameters:
   - out: Output pointer for result array
   - in: Input array
   
   Operation:
   - expm1(x) = exp(x) - 1
   - Accurate for |x| << 1
   
   Type Support: f32, f64, f16
   
   Example:
   ```clojure
   (let [x (create-array [1e-10 0.1 1.0] [3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-expm1 out-ptr x)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)  ; [1e-10 0.105 1.718]
   ```
   
   Use Cases:
   - Small exponential growth
   - Financial calculations (interest)
   - Numerical stability in algorithms
   
   Why Use This:
   - exp(x) - 1 loses precision for small x
   - expm1 maintains full precision
   - Critical in numerical analysis
   
   Returns: AF_SUCCESS or error code"
  "af_expm1" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-erf
  "Compute error function element-wise.
   
   Error function: erf(x) = (2/√π) ∫₀ˣ e^(-t²) dt
   Related to cumulative normal distribution.
   
   Parameters:
   - out: Output pointer for error function values
   - in: Input array
   
   Operation:
   - erf(0) = 0
   - erf(∞) = 1
   - erf(-x) = -erf(x)
   
   Properties:
   - Range: (-1, 1)
   - Odd function: erf(-x) = -erf(x)
   - Derivative: erf'(x) = (2/√π)e^(-x²)
   
   Type Support: f32, f64, f16
   
   Example:
   ```clojure
   (let [x (create-array [-2.0 -1.0 0.0 1.0 2.0] [5])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-erf out-ptr x)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)  ; [-0.995 -0.843 0.0 0.843 0.995]
   ```
   
   Use Cases:
   - Normal distribution CDF
   - Error analysis in statistics
   - Option pricing (Black-Scholes)
   - Signal processing
   
   Returns: AF_SUCCESS or error code"
  "af_erf" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-erfc
  "Compute complementary error function: erfc(x) = 1 - erf(x).
   
   More accurate than 1 - erf(x) for large x.
   
   Parameters:
   - out: Output pointer for complementary error function
   - in: Input array
   
   Operation:
   - erfc(x) = 1 - erf(x)
   - erfc(0) = 1
   - erfc(∞) = 0
   
   Properties:
   - Range: (0, 2)
   - More accurate for large x
   
   Type Support: f32, f64, f16
   
   Example:
   ```clojure
   (let [x (create-array [0.0 1.0 2.0 3.0] [4])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-erfc out-ptr x)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)  ; [1.0 0.157 0.0047 0.00002]
   ```
   
   Use Cases:
   - Tail probabilities
   - Q-function in communications
   - Large argument error analysis
   
   Returns: AF_SUCCESS or error code"
  "af_erfc" [::mem/pointer ::mem/pointer] ::mem/int)

;; Logarithmic functions

(defcfn af-log10
  "Compute base-10 logarithm element-wise.
   
   log₁₀(x) = log(x) / log(10)
   
   Parameters:
   - out: Output pointer for log10 values
   - in: Input array (must be positive)
   
   Operation:
   - log10(10) = 1
   - log10(100) = 2
   - log10(0.1) = -1
   
   Type Support: f32, f64, f16, c32, c64
   
   Example:
   ```clojure
   (let [x (create-array [0.1 1.0 10.0 100.0] [4])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-log10 out-ptr x)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)  ; [-1.0 0.0 1.0 2.0]
   ```
   
   Use Cases:
   - Decibel calculations
   - pH scale
   - Order of magnitude
   
   Returns: AF_SUCCESS or error code"
  "af_log10" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-log1p
  "Compute log(1 + x) with numerical stability for small x.
   
   More accurate than log(1 + x) when x is close to zero.
   
   Parameters:
   - out: Output pointer for result
   - in: Input array
   
   Operation:
   - log1p(x) = log(1 + x)
   - Accurate for |x| << 1
   
   Type Support: f32, f64, f16
   
   Example:
   ```clojure
   (let [x (create-array [1e-10 0.1 1.0] [3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-log1p out-ptr x)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)  ; [1e-10 0.0953 0.693]
   ```
   
   Use Cases:
   - Small relative changes
   - Financial returns
   - Numerical stability
   
   Why Use This:
   - log(1 + x) loses precision for small x
   - log1p maintains accuracy
   - Essential for numerical algorithms
   
   Returns: AF_SUCCESS or error code"
  "af_log1p" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-log2
  "Compute base-2 logarithm element-wise.
   
   log₂(x) = log(x) / log(2)
   
   Parameters:
   - out: Output pointer for log2 values
   - in: Input array (must be positive)
   
   Operation:
   - log2(2) = 1
   - log2(8) = 3
   - log2(0.5) = -1
   
   Type Support: f32, f64, f16
   
   Example:
   ```clojure
   (let [x (create-array [0.5 1.0 2.0 8.0] [4])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-log2 out-ptr x)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)  ; [-1.0 0.0 1.0 3.0]
   ```
   
   Use Cases:
   - Information theory (bits)
   - Binary tree depth
   - Power-of-two calculations
   
   Returns: AF_SUCCESS or error code"
  "af_log2" [::mem/pointer ::mem/pointer] ::mem/int)

;; Root functions

(defcfn af-cbrt
  "Compute cube root element-wise.
   
   cbrt(x) = x^(1/3)
   
   Parameters:
   - out: Output pointer for cube root
   - in: Input array
   
   Operation:
   - cbrt(8) = 2
   - cbrt(27) = 3
   - cbrt(-8) = -2
   
   Type Support: f32, f64, f16
   
   Note: Works with negative values (returns negative cube root).
   
   Example:
   ```clojure
   (let [x (create-array [-8.0 1.0 8.0 27.0] [4])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-cbrt out-ptr x)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)  ; [-2.0 1.0 2.0 3.0]
   ```
   
   Use Cases:
   - Volume to length conversions
   - Physics equations
   - 3D scaling
   
   Returns: AF_SUCCESS or error code"
  "af_cbrt" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-rsqrt
  "Compute reciprocal square root: 1/sqrt(x).
   
   May be faster than computing 1/sqrt(x) separately.
   
   Parameters:
   - out: Output pointer for reciprocal square root
   - in: Input array (must be positive)
   
   Operation:
   - rsqrt(x) = 1 / sqrt(x)
   - rsqrt(4) = 0.5
   - rsqrt(1) = 1.0
   
   Type Support: f32, f64, f16
   
   Example:
   ```clojure
   (let [x (create-array [1.0 4.0 9.0 16.0] [4])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-rsqrt out-ptr x)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)  ; [1.0 0.5 0.333 0.25]
   ```
   
   Use Cases:
   - Vector normalization
   - Fast inverse square root
   - Physics simulations
   
   Performance:
   - May use hardware instruction
   - Faster than 1/sqrt(x) on some GPUs
   
   Returns: AF_SUCCESS or error code"
  "af_rsqrt" [::mem/pointer ::mem/pointer] ::mem/int)

;; Gamma functions

(defcfn af-tgamma
  "Compute gamma function element-wise.
   
   Γ(x) = ∫₀^∞ t^(x-1) e^(-t) dt
   For positive integers: Γ(n) = (n-1)!
   
   Parameters:
   - out: Output pointer for gamma values
   - in: Input array
   
   Operation:
   - tgamma(1) = 1
   - tgamma(2) = 1
   - tgamma(3) = 2
   - tgamma(4) = 6
   - tgamma(n) = (n-1)!
   
   Type Support: f32, f64, f16
   
   Example:
   ```clojure
   (let [x (create-array [1.0 2.0 3.0 4.0 5.0] [5])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-tgamma out-ptr x)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)  ; [1.0 1.0 2.0 6.0 24.0]
   ```
   
   Use Cases:
   - Factorial for non-integers
   - Beta distribution
   - Statistical distributions
   - Combinatorics
   
   Note: Can overflow for large values.
   
   Returns: AF_SUCCESS or error code"
  "af_tgamma" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-lgamma
  "Compute log of absolute value of gamma function.
   
   lgamma(x) = log(|Γ(x)|)
   More numerically stable than log(tgamma(x)).
   
   Parameters:
   - out: Output pointer for log-gamma values
   - in: Input array
   
   Operation:
   - lgamma(x) = log(|tgamma(x)|)
   - Avoids overflow
   
   Type Support: f32, f64, f16
   
   Example:
   ```clojure
   (let [x (create-array [1.0 2.0 3.0 10.0 100.0] [5])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-lgamma out-ptr x)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)  ; [0.0 0.0 0.693 12.8 359.1]
   ```
   
   Use Cases:
   - Statistics (avoid factorial overflow)
   - Log-likelihood calculations
   - Large combinatorial values
   
   Returns: AF_SUCCESS or error code"
  "af_lgamma" [::mem/pointer ::mem/pointer] ::mem/int)

;; Trigonometric functions (complex-aware)

(defcfn af-sin
  "Compute sine element-wise.
   
   For real: sin(x)
   For complex: sin(a + ib) = sin(a)cosh(b) + i×cos(a)sinh(b)
   
   Parameters:
   - out: Output pointer for sine values
   - in: Input array
   
   Type Support: f32, f64, f16, c32, c64
   
   Example:
   ```clojure
   (let [x (create-array [0.0 (/ Math/PI 2) Math/PI] [3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-sin out-ptr x)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)  ; [0.0 1.0 0.0]
   ```
   
   Returns: AF_SUCCESS or error code"
  "af_sin" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-cos
  "Compute cosine element-wise.
   
   For real: cos(x)
   For complex: cos(a + ib) = cos(a)cosh(b) - i×sin(a)sinh(b)
   
   Parameters:
   - out: Output pointer for cosine values
   - in: Input array
   
   Type Support: f32, f64, f16, c32, c64
   
   Example:
   ```clojure
   (let [x (create-array [0.0 (/ Math/PI 2) Math/PI] [3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-cos out-ptr x)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)  ; [1.0 0.0 -1.0]
   ```
   
   Returns: AF_SUCCESS or error code"
  "af_cos" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-tan
  "Compute tangent element-wise.
   
   tan(x) = sin(x) / cos(x)
   
   Parameters:
   - out: Output pointer for tangent values
   - in: Input array
   
   Type Support: f32, f64, f16, c32, c64
   
   Returns: AF_SUCCESS or error code"
  "af_tan" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-asin
  "Compute arcsine element-wise.
   
   asin: [-1, 1] → [-π/2, π/2]
   
   Parameters:
   - out: Output pointer for arcsine values
   - in: Input array
   
   Type Support: f32, f64, f16, c32, c64
   
   Note: For real inputs outside [-1, 1], result is NaN.
   Complex extension allows any input.
   
   Returns: AF_SUCCESS or error code"
  "af_asin" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-acos
  "Compute arccosine element-wise.
   
   acos: [-1, 1] → [0, π]
   
   Parameters:
   - out: Output pointer for arccosine values
   - in: Input array
   
   Type Support: f32, f64, f16, c32, c64
   
   Returns: AF_SUCCESS or error code"
  "af_acos" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-atan
  "Compute arctangent element-wise.
   
   atan: ℝ → (-π/2, π/2)
   
   Parameters:
   - out: Output pointer for arctangent values
   - in: Input array
   
   Type Support: f32, f64, f16, c32, c64
   
   Returns: AF_SUCCESS or error code"
  "af_atan" [::mem/pointer ::mem/pointer] ::mem/int)

;; Hyperbolic functions

(defcfn af-sinh
  "Compute hyperbolic sine element-wise.
   
   sinh(x) = (e^x - e^(-x)) / 2
   
   Parameters:
   - out: Output pointer for sinh values
   - in: Input array
   
   Type Support: f32, f64, f16, c32, c64
   
   Returns: AF_SUCCESS or error code"
  "af_sinh" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-cosh
  "Compute hyperbolic cosine element-wise.
   
   cosh(x) = (e^x + e^(-x)) / 2
   
   Parameters:
   - out: Output pointer for cosh values
   - in: Input array
   
   Type Support: f32, f64, f16, c32, c64
   
   Returns: AF_SUCCESS or error code"
  "af_cosh" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-tanh
  "Compute hyperbolic tangent element-wise.
   
   tanh(x) = sinh(x) / cosh(x) = (e^x - e^(-x)) / (e^x + e^(-x))
   Maps (-∞, ∞) to (-1, 1).
   
   Parameters:
   - out: Output pointer for tanh values
   - in: Input array
   
   Type Support: f32, f64, f16, c32, c64
   
   Use Cases:
   - Neural network activation
   - Alternative to sigmoid
   
   Returns: AF_SUCCESS or error code"
  "af_tanh" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-asinh
  "Compute inverse hyperbolic sine element-wise.
   
   asinh(x) = log(x + sqrt(x² + 1))
   
   Parameters:
   - out: Output pointer for asinh values
   - in: Input array
   
   Type Support: f32, f64, f16, c32, c64
   
   Returns: AF_SUCCESS or error code"
  "af_asinh" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-acosh
  "Compute inverse hyperbolic cosine element-wise.
   
   acosh(x) = log(x + sqrt(x² - 1))
   Domain: x ≥ 1
   
   Parameters:
   - out: Output pointer for acosh values
   - in: Input array
   
   Type Support: f32, f64, f16, c32, c64
   
   Returns: AF_SUCCESS or error code"
  "af_acosh" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-atanh
  "Compute inverse hyperbolic tangent element-wise.
   
   atanh(x) = 0.5 × log((1+x)/(1-x))
   Domain: -1 < x < 1
   
   Parameters:
   - out: Output pointer for atanh values
   - in: Input array
   
   Type Support: f32, f64, f16, c32, c64
   
   Returns: AF_SUCCESS or error code"
  "af_atanh" [::mem/pointer ::mem/pointer] ::mem/int)

;; Exponential and logarithm (complex-aware)

(defcfn af-exp
  "Compute exponential element-wise.
   
   For real: exp(x) = e^x
   For complex: exp(a + ib) = e^a × (cos(b) + i×sin(b))
   
   Parameters:
   - out: Output pointer for exponential values
   - in: Input array
   
   Type Support: f32, f64, f16, c32, c64
   
   Example:
   ```clojure
   (let [x (create-array [0.0 1.0 2.0] [3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-exp out-ptr x)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)  ; [1.0 2.718 7.389]
   ```
   
   Returns: AF_SUCCESS or error code"
  "af_exp" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-log
  "Compute natural logarithm element-wise.
   
   For real: log(x) = ln(x)
   For complex: log(z) = log(|z|) + i×arg(z)
   
   Parameters:
   - out: Output pointer for logarithm values
   - in: Input array (must be positive for real)
   
   Type Support: f32, f64, f16, c32, c64
   
   Example:
   ```clojure
   (let [x (create-array [1.0 Math/E 10.0] [3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-log out-ptr x)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)  ; [0.0 1.0 2.303]
   ```
   
   Returns: AF_SUCCESS or error code"
  "af_log" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-sqrt
  "Compute square root element-wise.
   
   For real: sqrt(x) = x^(1/2)
   For complex: sqrt(z) uses principal branch
   
   Parameters:
   - out: Output pointer for square root values
   - in: Input array (must be non-negative for real)
   
   Type Support: f32, f64, f16, c32, c64
   
   Example:
   ```clojure
   (let [x (create-array [1.0 4.0 9.0 16.0] [4])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-sqrt out-ptr x)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)  ; [1.0 2.0 3.0 4.0]
   ```
   
   Returns: AF_SUCCESS or error code"
  "af_sqrt" [::mem/pointer ::mem/pointer] ::mem/int)

;; Logical check functions

(defcfn af-isinf
  "Check for infinite values element-wise.
   
   Returns 1 (true) for ±∞, 0 (false) otherwise.
   
   Parameters:
   - out: Output pointer for boolean array
   - in: Input array
   
   Type Support: f32, f64, f16, c32, c64
   
   Example:
   ```clojure
   (let [x (create-array [1.0 ##Inf ##-Inf] [3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-isinf out-ptr x)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)  ; [0 1 1]
   ```
   
   Use Cases:
   - Overflow detection
   - Data validation
   - Numerical debugging
   
   Returns: AF_SUCCESS or error code"
  "af_isinf" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-isnan
  "Check for NaN (Not-a-Number) values element-wise.
   
   Returns 1 (true) for NaN, 0 (false) otherwise.
   
   Parameters:
   - out: Output pointer for boolean array
   - in: Input array
   
   Type Support: f32, f64, f16, c32, c64
   
   Example:
   ```clojure
   (let [x (create-array [1.0 ##NaN 3.0] [3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-isnan out-ptr x)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)  ; [0 1 0]
   ```
   
   Use Cases:
   - Invalid computation detection
   - Data cleaning
   - Numerical error checking
   
   Returns: AF_SUCCESS or error code"
  "af_isnan" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-iszero
  "Check for zero values element-wise.
   
   Returns 1 (true) for exact zero, 0 (false) otherwise.
   
   Parameters:
   - out: Output pointer for boolean array
   - in: Input array
   
   Type Support: f32, f64, f16, c32, c64
   
   Example:
   ```clojure
   (let [x (create-array [0.0 1.0 -0.0] [3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-iszero out-ptr x)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)  ; [1 0 1]
   ```
   
   Note: Tests for exact zero, not near-zero.
   
   Returns: AF_SUCCESS or error code"
  "af_iszero" [::mem/pointer ::mem/pointer] ::mem/int)

;; Logical and bitwise

(defcfn af-not
  "Compute logical NOT element-wise.
   
   Returns 1 where input is 0, and 0 where input is non-zero.
   
   Parameters:
   - out: Output pointer for logical NOT array
   - in: Input array
   
   Operation:
   - not(0) = 1
   - not(x) = 0 for x ≠ 0
   
   Type Support: All types
   
   Example:
   ```clojure
   (let [x (create-array [0 1 5 0] [4])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-not out-ptr x)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)  ; [1 0 0 1]
   ```
   
   Use Cases:
   - Boolean logic
   - Mask inversion
   - Conditional operations
   
   Returns: AF_SUCCESS or error code"
  "af_not" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-bitnot
  "Compute bitwise NOT element-wise.
   
   Flips all bits: ~x
   
   Parameters:
   - out: Output pointer for bitwise NOT array
   - in: Input array
   
   Operation:
   - bitnot(x) = ~x (all bits flipped)
   
   Type Support: Integer types (s8, u8, s16, u16, s32, u32, s64, u64, b8)
   
   Example:
   ```clojure
   (let [x (create-array [0 1 255] [3])  ; u8 array
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-bitnot out-ptr x)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)  ; [255 254 0]
   ```
   
   Use Cases:
   - Bit manipulation
   - Mask operations
   - Low-level optimizations
   
   Returns: AF_SUCCESS or error code"
  "af_bitnot" [::mem/pointer ::mem/pointer] ::mem/int)

;; Complex and power functions

(defcfn af-arg
  "Compute argument (phase angle) of complex numbers.
   
   Returns angle in radians: arg(a + ib) = atan2(b, a)
   Range: [-π, π]
   
   Parameters:
   - out: Output pointer for phase angles
   - in: Input array (complex or real)
   
   Operation:
   - For real: returns 0
   - For complex: arg(z) = atan2(imag(z), real(z))
   
   Type Support: All types (returns 0 for real)
   
   Example:
   ```clojure
   (let [z (create-complex-array [[1 1] [1 0] [0 1]] [3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-arg out-ptr z)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)  ; [π/4 0 π/2]
   ```
   
   Use Cases:
   - FFT phase analysis
   - Complex number polar form
   - Signal phase extraction
   
   Returns: AF_SUCCESS or error code"
  "af_arg" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-pow2
  "Compute 2^x element-wise.
   
   pow2(x) = 2^x
   
   Parameters:
   - out: Output pointer for power-of-2 values
   - in: Input array (exponent)
   
   Operation:
   - pow2(0) = 1
   - pow2(1) = 2
   - pow2(3) = 8
   - pow2(-1) = 0.5
   
   Type Support: f32, f64, f16
   
   Example:
   ```clojure
   (let [x (create-array [-1.0 0.0 1.0 3.0] [4])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-pow2 out-ptr x)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)  ; [0.5 1.0 2.0 8.0]
   ```
   
   Use Cases:
   - Binary scaling
   - Frequency domain operations
   - Power-of-2 calculations
   
   Returns: AF_SUCCESS or error code"
  "af_pow2" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-factorial
  "Compute factorial element-wise.
   
   factorial(n) = n! = Γ(n+1)
   
   Parameters:
   - out: Output pointer for factorial values
   - in: Input array (non-negative integers)
   
   Operation:
   - factorial(0) = 1
   - factorial(1) = 1
   - factorial(5) = 120
   - factorial(n) = n × (n-1) × ... × 1
   
   Implementation: Uses tgamma(n+1)
   
   Type Support: f32, f64, f16
   
   Example:
   ```clojure
   (let [x (create-array [0.0 1.0 2.0 5.0] [4])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-factorial out-ptr x)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)  ; [1.0 1.0 2.0 120.0]
   ```
   
   Use Cases:
   - Combinatorics
   - Probability calculations
   - Series expansions
   
   Note: Can overflow for large n (use lgamma for large values).
   
   Returns: AF_SUCCESS or error code"
  "af_factorial" [::mem/pointer ::mem/pointer] ::mem/int)
