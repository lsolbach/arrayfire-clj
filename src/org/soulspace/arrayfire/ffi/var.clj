(ns org.soulspace.arrayfire.ffi.var
  "Bindings for the ArrayFire variance and related statistical functions.
   
   Variance measures the spread or dispersion of data around its mean value.
   It is fundamental to statistics, data analysis, and machine learning for
   understanding data distribution and uncertainty.
   
   Mathematical Foundation:
   
   **Population Variance**:
   σ² = (1/N) * Σ(xᵢ - μ)²
   
   where:
   - N = total number of elements
   - xᵢ = individual values
   - μ = mean of all values
   
   **Sample Variance** (Bessel's correction):
   s² = 1/(N-1) * Σ(xᵢ - x̄)²
   
   Uses N-1 (degrees of freedom correction) to provide an unbiased
   estimator when working with samples from a larger population.
   
   **Weighted Variance**:
   σ²_w = (Σ wᵢ(xᵢ - μ_w)²) / (Σ wᵢ)
   
   where:
   - wᵢ = weight for element i
   - μ_w = weighted mean
   
   **Standard Deviation**:
   σ = √(variance)
   
   More interpretable than variance as it's in the same units as the data.
   
   Key Concepts:
   
   1. **Bias Types** (af_var_bias enum):
      - AF_VARIANCE_DEFAULT (0): Uses sample variance (N-1)
      - AF_VARIANCE_SAMPLE (1): Sample variance with Bessel's correction
      - AF_VARIANCE_POPULATION (2): Population variance (N)
   
   2. **Dimension Reduction**:
      - Variance can be computed along any dimension
      - dim=-1 uses first non-singleton dimension
      - Output reduces specified dimension to size 1
   
   3. **Complex Numbers**:
      - Variance of complex array: Var(|z|) where |z| = √(real² + imag²)
      - Returns variance of magnitudes
      - For var_all functions: Returns complex variance (real and imag parts)
   
   4. **Weighted Variance**:
      - Useful for frequency-weighted data
      - Importance-weighted observations
      - Reliability-weighted measurements
   
   Performance Characteristics:
   
   - Algorithm: Two-pass (mean then variance) for numerical stability
   - Complexity: O(N) where N = number of elements
   - GPU acceleration: 10-100× faster than CPU for large arrays
   - Memory: Requires temporary storage for mean computation
   
   Numerical Stability:
   
   ArrayFire uses the two-pass algorithm:
   1. First pass: Compute mean
   2. Second pass: Compute squared deviations from mean
   
   This avoids catastrophic cancellation in the naive algorithm:
   Var(X) = E[X²] - (E[X])² ← numerically unstable!
   
   Applications:
   
   1. **Statistical Analysis**:
      - Measure data spread and uncertainty
      - Quality control and process monitoring
      - Hypothesis testing
   
   2. **Machine Learning**:
      - Feature normalization (zero mean, unit variance)
      - Batch normalization in neural networks
      - Principal Component Analysis (PCA)
      - Anomaly detection
   
   3. **Signal Processing**:
      - Power spectrum estimation
      - Noise level estimation
      - Signal-to-noise ratio (SNR) calculation
   
   4. **Finance**:
      - Risk measurement (volatility)
      - Portfolio optimization
      - Value at Risk (VaR) calculations
   
   5. **Image Processing**:
      - Texture analysis
      - Contrast measurement
      - Adaptive thresholding
   
   6. **Quality Control**:
      - Process capability indices
      - Control charts
      - Six Sigma analysis
   
   Common Patterns:
   
   ```clojure
   ;; 1. Compute sample variance along dimension
   (let [data (create-array [100 200])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-var-v2 out-ptr data AF_VARIANCE_SAMPLE -1)]
     (mem/read-pointer out-ptr ::mem/pointer))
   
   ;; 2. Feature normalization (standardization)
   (let [features (create-array data [1000 50])  ; 1000 samples, 50 features
         mean-ptr (mem/alloc-pointer ::mem/pointer)
         var-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-meanvar mean-ptr var-ptr features empty-array 
                       AF_VARIANCE_POPULATION 0)
         mean (mem/read-pointer mean-ptr ::mem/pointer)
         var (mem/read-pointer var-ptr ::mem/pointer)
         std (af-sqrt var)
         normalized (af-div (af-sub features mean) std)]
     normalized)
   
   ;; 3. Weighted variance for reliability weighting
   (let [measurements (create-array [1000])
         reliability-weights (create-array [1000])  ; Higher = more reliable
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-var-weighted out-ptr measurements reliability-weights -1)]
     (mem/read-pointer out-ptr ::mem/pointer))
   
   ;; 4. Variance of entire array (scalar result)
   (let [data (create-array [100 100])
         real-ptr (mem/alloc ::mem/double)
         imag-ptr (mem/alloc ::mem/double)
         _ (af-var-all-v2 real-ptr imag-ptr data AF_VARIANCE_SAMPLE)
         variance (mem/deref real-ptr ::mem/double)]
     variance)
   
   ;; 5. Batch variance computation
   (let [batch-data (create-array [64 32 128])  ; 128 batches, 64×32 each
         out-ptr (mem/alloc-pointer ::mem/pointer)
         ;; Compute variance along first two dims, keep batch dim
         _ (af-var-v2 out-ptr batch-data AF_VARIANCE_SAMPLE 0)
         var-intermediate (mem/read-pointer out-ptr ::mem/pointer)
         _ (af-var-v2 out-ptr var-intermediate AF_VARIANCE_SAMPLE 0)]
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Choosing Bias Type:
   
   - **Population variance** (AF_VARIANCE_POPULATION):
     * Use when you have the entire population
     * Variance of a fixed dataset
     * Normalization of known distributions
   
   - **Sample variance** (AF_VARIANCE_SAMPLE):
     * Use when data is a sample from larger population
     * Statistical inference and estimation
     * Most scientific applications
     * Default choice when unsure
   
   Type Support:
   - Input: f32, f64, c32, c64, s32, u32, s16, u16, s8, u8, b8
   - Output: f32 for f32/integers/b8, f64 for f64, c32 for c32, c64 for c64
   - Weighted functions: Weights should match or be promotable to output type
   
   Relationship to Standard Deviation:
   - Standard deviation = √(variance)
   - Variance in squared units, std dev in original units
   - Std dev more intuitive for interpretation
   - Variance better for mathematical operations (linear combinations)
   
   See also:
   - af-stdev, af-stdev-v2: Standard deviation functions
   - af-mean, af-mean-weighted: Mean computation
   - af-cov, af-cov-v2: Covariance between arrays
   - af-corrcoef: Correlation coefficient"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; Variance computation

;; af_err af_var(af_array *out, const af_array in, const bool isbiased, const dim_t dim)
(defcfn af-var
  "Compute variance along specified dimension (deprecated version).
   
   **DEPRECATED**: Use af-var-v2 instead for more explicit bias control.
   
   Computes variance along the specified dimension using boolean bias flag.
   
   Parameters:
   - out: Output pointer for variance array
   - in: Input array
   - isbiased: Boolean flag
     * false (0): Sample variance (N-1 denominator)
     * true (1): Population variance (N denominator)
   - dim: Dimension along which to compute variance
     * -1: First non-singleton dimension (default behavior)
     * 0, 1, 2, 3: Specific dimension
   
   Output dimensions:
   - Same as input except specified dimension becomes size 1
   - Example: [100, 200, 50] with dim=1 → [100, 1, 50]
   
   Algorithm:
   1. Compute mean along dimension
   2. Compute (x - mean)² for all elements
   3. Sum squared deviations along dimension
   4. Divide by N (population) or N-1 (sample)
   
   Type behavior:
   - f32 input → f32 output
   - f64 input → f64 output
   - Integer/bool → f32 output
   - Complex → complex output (variance of magnitudes)
   
   Example:
   ```clojure
   ;; Sample variance of each column
   (let [data (create-array [[1 2 3]
                             [4 5 6]
                             [7 8 9]])  ; 3×3 array
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-var out-ptr data false 0)  ; Along rows (columns variance)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)  ; Shape: [1, 3] - variance for each column
   ```
   
   Migration to af-var-v2:
   - isbiased=false → AF_VARIANCE_SAMPLE (1)
   - isbiased=true → AF_VARIANCE_POPULATION (2)
   
   Returns:
   ArrayFire error code (AF_SUCCESS on success)
   
   See also:
   - af-var-v2: Recommended version with explicit bias enum"
  "af_var" [::mem/pointer ::mem/pointer ::mem/int ::mem/long] ::mem/int)

;; af_err af_var_v2(af_array *out, const af_array in, const af_var_bias bias, const dim_t dim)
(defcfn af-var-v2
  "Compute variance along specified dimension with explicit bias control.
   
   Recommended variance function with explicit bias type specification using
   the af_var_bias enum for clarity.
   
   Parameters:
   - out: Output pointer for variance array
   - in: Input array
   - bias: Bias type (af_var_bias enum)
     * AF_VARIANCE_DEFAULT (0): Same as AF_VARIANCE_SAMPLE
     * AF_VARIANCE_SAMPLE (1): Bessel-corrected (N-1)
     * AF_VARIANCE_POPULATION (2): Population variance (N)
   - dim: Dimension along which to compute variance (-1 for first non-singleton)
   
   Mathematical formulas:
   
   **Sample variance** (bias = AF_VARIANCE_SAMPLE):
   Var = 1/(N-1) * Σ(xᵢ - mean)²
   
   **Population variance** (bias = AF_VARIANCE_POPULATION):
   Var = 1/N * Σ(xᵢ - mean)²
   
   Dimension behavior:
   - dim = -1: First non-singleton dimension
   - dim = 0: Variance along first dimension (column-wise for 2D)
   - dim = 1: Variance along second dimension (row-wise for 2D)
   - dim = 2, 3: Higher dimensions
   
   Output shape:
   - Input: [d₀, d₁, d₂, d₃]
   - Output with dim=1: [d₀, 1, d₂, d₃]
   
   Performance:
   - O(N) complexity where N = array elements
   - Two-pass algorithm for numerical stability
   - GPU parallel across all output elements
   - Efficient for large arrays
   
   Type promotion:
   - f32 → f32
   - f64 → f64
   - c32 → c32 (variance of magnitudes)
   - c64 → c64 (variance of magnitudes)
   - Integer types → f32
   - b8 → f32
   
   Example (Feature variance):
   ```clojure
   ;; Compute variance of each feature across samples
   (let [features (create-array data [10000 256])  ; 10000 samples, 256 features
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-var-v2 out-ptr features AF_VARIANCE_SAMPLE 0)
         variances (mem/read-pointer out-ptr ::mem/pointer)]
     ;; variances shape: [1, 256] - variance per feature
     variances)
   ```
   
   Example (Batch statistics):
   ```clojure
   ;; Variance for each batch independently
   (let [batches (create-array [64 64 32])  ; 32 batches of 64×64
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-var-v2 out-ptr batches AF_VARIANCE_POPULATION 0)
         var1 (mem/read-pointer out-ptr ::mem/pointer)
         _ (af-var-v2 out-ptr var1 AF_VARIANCE_POPULATION 0)]
     (mem/read-pointer out-ptr ::mem/pointer))  ; Shape: [1, 1, 32]
   ```
   
   When to use each bias:
   - **SAMPLE**: Estimating population variance from sample (most common)
   - **POPULATION**: Known complete dataset, normalization, theoretical work
   - **DEFAULT**: Same as SAMPLE, use for compatibility
   
   Returns:
   ArrayFire error code (AF_SUCCESS on success)
   
   See also:
   - af-var: Deprecated boolean version
   - af-stdev-v2: Standard deviation (√variance)
   - af-var-weighted: Variance with weights"
  "af_var_v2" [::mem/pointer ::mem/pointer ::mem/int ::mem/long] ::mem/int)

;; af_err af_var_weighted(af_array *out, const af_array in, const af_array weights, const dim_t dim)
(defcfn af-var-weighted
  "Compute weighted variance along specified dimension.
   
   Calculates variance with each element weighted by corresponding weight value.
   Useful for reliability-weighted measurements, frequency-weighted data, or
   importance sampling.
   
   Parameters:
   - out: Output pointer for weighted variance array
   - in: Input array
   - weights: Weight array (must be broadcastable to input shape)
   - dim: Dimension along which to compute variance (-1 for first non-singleton)
   
   Mathematical formula:
   
   Weighted variance:
   σ²_w = (Σ wᵢ(xᵢ - μ_w)²) / (Σ wᵢ)
   
   where weighted mean:
   μ_w = (Σ wᵢxᵢ) / (Σ wᵢ)
   
   Weight requirements:
   - Non-negative values (negative weights may produce unexpected results)
   - Same dimensionality as input or broadcastable
   - Zero weights: Exclude corresponding elements
   - Normalized or unnormalized (automatically normalized)
   
   Dimension behavior:
   - Weights must have same size along dimension being reduced
   - Can be 1D array broadcast across other dimensions
   - Weight sum computed along same dimension as variance
   
   Type compatibility:
   - Input and weights should have compatible types
   - Output type matches input precision
   - Weights promoted to match output type if needed
   
   Performance:
   - O(N) complexity
   - Three passes: weight sum, weighted mean, weighted variance
   - GPU accelerated
   
   Example (Reliability weighting):
   ```clojure
   ;; Measurements with reliability scores
   (let [measurements (create-array [1000 10])  ; 1000 observations, 10 sensors
         reliability (create-array [10])         ; Reliability per sensor
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-var-weighted out-ptr measurements reliability 0)]
     (mem/read-pointer out-ptr ::mem/pointer))  ; Shape: [1, 10]
   ```
   
   Example (Frequency-weighted histogram variance):
   ```clojure
   ;; Histogram bins with frequencies
   (let [bin-centers (create-array [0.5 1.5 2.5 3.5 4.5])
         frequencies (create-array [10 25 30 20 15])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-var-weighted out-ptr bin-centers frequencies -1)]
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (Time series with confidence weights):
   ```clojure
   ;; Economic data with confidence indicators
   (let [time-series (create-array [365])       ; Daily values
         confidence (create-array [365])         ; Confidence scores 0-1
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-var-weighted out-ptr time-series confidence -1)]
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Use cases:
   - **Sensor fusion**: Weight by sensor accuracy/reliability
   - **Survey data**: Weight by sample representativeness
   - **Financial data**: Weight by trading volume or liquidity
   - **Image processing**: Adaptive filters with spatially-varying weights
   - **Machine learning**: Importance-weighted samples
   
   Special cases:
   - All weights equal: Same as unweighted variance
   - All weights zero: Undefined (check before use)
   - Single non-zero weight: Zero variance
   
   Bias:
   - Uses population-style computation (denominator = Σwᵢ)
   - For sample-like behavior with weights, use: Var * (Σwᵢ)/(Σwᵢ - 1)
   
   Returns:
   ArrayFire error code (AF_SUCCESS on success)
   
   See also:
   - af-mean-weighted: Weighted mean computation
   - af-var-v2: Unweighted variance with bias control
   - af-var-all-weighted: Weighted variance of entire array"
  "af_var_weighted" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/long] ::mem/int)

;; Variance of all elements (reduction to scalar)

;; af_err af_var_all(double *realVal, double *imagVal, const af_array in, const bool isbiased)
(defcfn af-var-all
  "Compute variance of all elements in array (deprecated version).
   
   **DEPRECATED**: Use af-var-all-v2 for explicit bias control.
   
   Reduces entire array to single variance value (scalar). For complex arrays,
   returns complex variance with real and imaginary parts.
   
   Parameters:
   - realVal: Pointer to double for real part of variance
   - imagVal: Pointer to double for imaginary part of variance
   - in: Input array (any dimensions)
   - isbiased: Boolean flag
     * false (0): Sample variance (N-1)
     * true (1): Population variance (N)
   
   Output:
   - For real arrays: realVal = variance, imagVal = 0
   - For complex arrays: Both realVal and imagVal contain variance components
   
   Algorithm:
   1. Flatten array to 1D conceptually
   2. Compute mean of all elements
   3. Compute Σ(xᵢ - mean)² / divisor
   4. divisor = N (population) or N-1 (sample)
   
   Complex number handling:
   For complex array z = a + bi:
   - Computes variance of complex values
   - Real part: Variance including real and imaginary contributions
   - Imaginary part: Cross-variance terms
   
   Example:
   ```clojure
   ;; Variance of entire matrix
   (let [data (create-array (range 10000) [100 100])
         real-ptr (mem/alloc ::mem/double)
         imag-ptr (mem/alloc ::mem/double)
         _ (af-var-all real-ptr imag-ptr data false)
         variance (mem/deref real-ptr ::mem/double)]
     variance)
   ```
   
   Migration to af-var-all-v2:
   - isbiased=false → AF_VARIANCE_SAMPLE
   - isbiased=true → AF_VARIANCE_POPULATION
   
   Returns:
   ArrayFire error code (AF_SUCCESS on success)
   
   See also:
   - af-var-all-v2: Recommended version with enum bias"
  "af_var_all" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_var_all_v2(double *realVal, double *imagVal, const af_array in, const af_var_bias bias)
(defcfn af-var-all-v2
  "Compute variance of all elements in array with explicit bias control.
   
   Reduces entire array to single scalar variance value. Recommended version
   with explicit bias type specification.
   
   Parameters:
   - realVal: Pointer to double for real part of variance
   - imagVal: Pointer to double for imaginary part of variance (0 for real arrays)
   - in: Input array (any shape)
   - bias: Bias type (af_var_bias enum)
     * AF_VARIANCE_DEFAULT (0): Sample variance
     * AF_VARIANCE_SAMPLE (1): Bessel-corrected (N-1)
     * AF_VARIANCE_POPULATION (2): Population variance (N)
   
   Behavior:
   - Treats array as single flat vector
   - Computes global variance across all elements
   - Ignores array shape (all dimensions reduced)
   
   Mathematical formulas:
   
   **Sample variance**:
   Var = 1/(N-1) * Σᵢ₌₁ᴺ(xᵢ - x̄)²
   
   **Population variance**:
   Var = 1/N * Σᵢ₌₁ᴺ(xᵢ - μ)²
   
   where N = total number of elements
   
   Complex arrays:
   - For z = a + bi, computes complex-valued variance
   - realVal: Real component of variance
   - imagVal: Imaginary component of variance
   - For magnitude variance, use: af-abs first, then af-var-all-v2
   
   Type behavior:
   - All types → double precision output
   - Ensures sufficient precision for accumulation
   - No overflow risk for reasonable array sizes
   
   Performance:
   - O(N) complexity where N = total elements
   - Two-pass algorithm
   - GPU reduction highly parallel
   - Efficient even for very large arrays (billions of elements)
   
   Example (Global statistics):
   ```clojure
   ;; Overall variance of large dataset
   (let [data (create-array data [1000 1000 10])  ; 10M elements
         real-ptr (mem/alloc ::mem/double)
         imag-ptr (mem/alloc ::mem/double)
         _ (af-var-all-v2 real-ptr imag-ptr data AF_VARIANCE_SAMPLE)
         variance (mem/deref real-ptr ::mem/double)]
     (println \"Global variance:\" variance)
     variance)
   ```
   
   Example (Coefficient of variation):
   ```clojure
   ;; Relative variability measure
   (let [measurements (create-array data [10000])
         mean-real (mem/alloc ::mem/double)
         mean-imag (mem/alloc ::mem/double)
         var-real (mem/alloc ::mem/double)
         var-imag (mem/alloc ::mem/double)
         _ (af-mean-all mean-real mean-imag measurements)
         _ (af-var-all-v2 var-real var-imag measurements AF_VARIANCE_SAMPLE)
         mean-val (mem/deref mean-real ::mem/double)
         var-val (mem/deref var-real ::mem/double)
         std-dev (Math/sqrt var-val)
         cv (/ std-dev mean-val)]  ; Coefficient of variation
     cv)
   ```
   
   Example (Signal-to-noise ratio):
   ```clojure
   ;; SNR estimation
   (let [signal (create-array signal-data [1000000])
         mean-ptr (mem/alloc ::mem/double)
         imag-ptr (mem/alloc ::mem/double)
         _ (af-mean-all mean-ptr imag-ptr signal)
         var-ptr (mem/alloc ::mem/double)
         _ (af-var-all-v2 var-ptr imag-ptr signal AF_VARIANCE_POPULATION)
         mean-val (mem/deref mean-ptr ::mem/double)
         var-val (mem/deref var-ptr ::mem/double)
         snr-db (* 10 (Math/log10 (/ (* mean-val mean-val) var-val)))]
     snr-db)
   ```
   
   Applications:
   - **Quality metrics**: Overall data quality assessment
   - **Process control**: Global process variability
   - **Normalization**: Compute statistics for standardization
   - **Model evaluation**: Prediction error variance
   - **Feature engineering**: Variance-based feature selection
   
   Returns:
   ArrayFire error code (AF_SUCCESS on success)
   
   See also:
   - af-var-v2: Variance along dimension
   - af-stdev-all-v2: Standard deviation of all elements
   - af-mean-all: Mean of all elements"
  "af_var_all_v2" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_var_all_weighted(double *realVal, double *imagVal, const af_array in, const af_array weights)
(defcfn af-var-all-weighted
  "Compute weighted variance of all elements in array.
   
   Reduces entire array to single weighted variance value (scalar). Useful for
   computing global statistics with importance weights, reliability scores, or
   frequency data.
   
   Parameters:
   - realVal: Pointer to double for real part of weighted variance
   - imagVal: Pointer to double for imaginary part of weighted variance
   - in: Input array (any shape)
   - weights: Weight array (must match input dimensions)
   
   Mathematical formula:
   
   Weighted variance:
   σ²_w = (Σᵢ₌₁ᴺ wᵢ(xᵢ - μ_w)²) / (Σᵢ₌₁ᴺ wᵢ)
   
   where weighted mean:
   μ_w = (Σᵢ₌₁ᴺ wᵢxᵢ) / (Σᵢ₌₁ᴺ wᵢ)
   
   Weight behavior:
   - Flattens both input and weights
   - Element-wise correspondence between data and weights
   - Automatic normalization (weight sum in denominator)
   - Zero weights exclude corresponding elements
   
   Complex arrays:
   - Computes complex-valued weighted variance
   - realVal and imagVal both contain variance information
   
   Output precision:
   - Always returns double precision
   - Suitable for accumulating large numbers of elements
   
   Performance:
   - O(N) complexity
   - Three-pass: weight sum, weighted mean, weighted variance
   - GPU parallel reduction
   
   Example (Reliability-weighted measurement):
   ```clojure
   ;; Global variance of sensor network readings
   (let [all-readings (create-array readings [100 50])  ; 100 time points, 50 sensors
         reliability-map (create-array reliability [100 50])  ; Per-reading reliability
         real-ptr (mem/alloc ::mem/double)
         imag-ptr (mem/alloc ::mem/double)
         _ (af-var-all-weighted real-ptr imag-ptr all-readings reliability-map)
         weighted-var (mem/deref real-ptr ::mem/double)]
     weighted-var)
   ```
   
   Example (Frequency-weighted distribution):
   ```clojure
   ;; Variance from histogram
   (let [bin-centers (create-array [0.5 1.5 2.5 3.5 4.5 5.5])
         frequencies (create-array [15 45 80 60 30 10])  ; Count in each bin
         real-ptr (mem/alloc ::mem/double)
         imag-ptr (mem/alloc ::mem/double)
         _ (af-var-all-weighted real-ptr imag-ptr bin-centers frequencies)
         variance (mem/deref real-ptr ::mem/double)
         std-dev (Math/sqrt variance)]
     {:variance variance :std-dev std-dev})
   ```
   
   Example (Portfolio variance):
   ```clojure
   ;; Risk of weighted portfolio
   (let [asset-returns (create-array returns [252 10])  ; Daily returns, 10 assets
         portfolio-weights (create-array weights [10])   ; Allocation weights
         ;; Broadcast weights to match returns shape
         weight-matrix (af-tile portfolio-weights [252 1])
         real-ptr (mem/alloc ::mem/double)
         imag-ptr (mem/alloc ::mem/double)
         _ (af-var-all-weighted real-ptr imag-ptr asset-returns weight-matrix)
         portfolio-var (mem/deref real-ptr ::mem/double)]
     portfolio-var)
   ```
   
   Use cases:
   - **Meta-analysis**: Combining studies with different sample sizes
   - **Survey statistics**: Accounting for sampling weights
   - **Financial risk**: Portfolio variance with position weights
   - **Quality control**: Variance with batch-size weights
   - **Ensemble methods**: Weighted prediction variance
   
   Weight interpretation:
   - Relative importance: Larger weights = more influence
   - Frequency/count: Weight = number of occurrences
   - Reliability: Weight = confidence/precision
   - Inverse variance: Weight = 1/variance (precision weighting)
   
   Special cases:
   - Uniform weights: Equivalent to af-var-all-v2
   - Binary weights: Variance of subset (nonzero weights)
   - Normalized weights: Same result as unnormalized
   
   Returns:
   ArrayFire error code (AF_SUCCESS on success)
   
   See also:
   - af-var-weighted: Weighted variance along dimension
   - af-var-all-v2: Unweighted variance of all elements
   - af-mean-all-weighted: Weighted mean of all elements"
  "af_var_all_weighted" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; Mean and variance computation (combined for efficiency)

;; af_err af_meanvar(af_array *mean, af_array *var, const af_array in, const af_array weights, const af_var_bias bias, const dim_t dim)
(defcfn af-meanvar
  "Compute both mean and variance along dimension in single operation.
   
   Efficient combined computation of mean and variance. More efficient than
   calling af-mean and af-var separately due to shared computation and single
   array traversal.
   
   Parameters:
   - mean: Output pointer for mean array
   - var: Output pointer for variance array
   - in: Input array
   - weights: Weight array (can be empty/null for unweighted)
   - bias: Bias type (af_var_bias enum)
     * AF_VARIANCE_DEFAULT (0): Sample variance
     * AF_VARIANCE_SAMPLE (1): Bessel-corrected (N-1)
     * AF_VARIANCE_POPULATION (2): Population variance (N)
   - dim: Dimension along which to compute (-1 for first non-singleton)
   
   Behavior:
   - Computes both statistics in single efficient pass
   - If weights is empty: Unweighted mean and variance
   - If weights provided: Weighted mean and variance
   - Both outputs have same shape (reduced along dim)
   
   Empty weights:
   - Pass null/empty af_array for unweighted computation
   - More efficient than af-mean + af-var separately
   
   Weighted computation:
   - Weights must be broadcastable to input shape
   - Same weight array used for both mean and variance
   - More efficient than af-mean-weighted + af-var-weighted
   
   Performance advantage:
   - Single pass through data
   - Shared mean computation (variance needs mean)
   - Reduced memory traffic
   - ~40% faster than separate calls
   
   Output shapes:
   - Both mean and var have same shape
   - Input [d₀, d₁, d₂], dim=1 → Output [d₀, 1, d₂]
   
   Example (Batch normalization prep):
   ```clojure
   ;; Compute statistics for batch norm layer
   (let [activations (create-array data [64 128 32])  ; 32 batches, 64×128 each
         mean-ptr (mem/alloc-pointer ::mem/pointer)
         var-ptr (mem/alloc-pointer ::mem/pointer)
         empty-weights (af-create-handle 0 [0])
         _ (af-meanvar mean-ptr var-ptr activations empty-weights
                       AF_VARIANCE_POPULATION 0)
         mean (mem/read-pointer mean-ptr ::mem/pointer)
         var (mem/read-pointer var-ptr ::mem/pointer)
         ;; Normalize: (x - mean) / sqrt(var + epsilon)
         epsilon 1e-5
         normalized (af-div (af-sub activations mean)
                           (af-sqrt (af-add var epsilon)))]
     normalized)
   ```
   
   Example (Feature standardization):
   ```clojure
   ;; Standardize features to zero mean, unit variance
   (let [features (create-array data [10000 100])  ; 10000 samples, 100 features
         mean-ptr (mem/alloc-pointer ::mem/pointer)
         var-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-meanvar mean-ptr var-ptr features (af-create-handle 0 [0])
                       AF_VARIANCE_SAMPLE 0)
         mean (mem/read-pointer mean-ptr ::mem/pointer)
         var (mem/read-pointer var-ptr ::mem/pointer)
         std (af-sqrt var)
         standardized (af-div (af-sub features mean) std)]
     standardized)
   ```
   
   Example (Weighted statistics):
   ```clojure
   ;; Time series with confidence weights
   (let [time-series (create-array data [365 50])  ; 365 days, 50 variables
         confidence (create-array weights [365 50])  ; Per-observation confidence
         mean-ptr (mem/alloc-pointer ::mem/pointer)
         var-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-meanvar mean-ptr var-ptr time-series confidence
                       AF_VARIANCE_POPULATION 0)
         wmean (mem/read-pointer mean-ptr ::mem/pointer)
         wvar (mem/read-pointer var-ptr ::mem/pointer)]
     {:weighted-mean wmean :weighted-variance wvar})
   ```
   
   Example (Multi-dimensional reduction):
   ```clojure
   ;; Compute statistics along multiple dimensions sequentially
   (let [data-3d (create-array data [100 100 100])
         ;; First dimension
         mean1-ptr (mem/alloc-pointer ::mem/pointer)
         var1-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-meanvar mean1-ptr var1-ptr data-3d (af-create-handle 0 [0])
                       AF_VARIANCE_SAMPLE 0)
         mean1 (mem/read-pointer mean1-ptr ::mem/pointer)
         var1 (mem/read-pointer var1-ptr ::mem/pointer)
         ;; Second dimension
         mean2-ptr (mem/alloc-pointer ::mem/pointer)
         var2-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-meanvar mean2-ptr var2-ptr mean1 (af-create-handle 0 [0])
                       AF_VARIANCE_SAMPLE 0)]
     {:mean (mem/read-pointer mean2-ptr ::mem/pointer)
      :var (mem/read-pointer var2-ptr ::mem/pointer)})
   ```
   
   Applications:
   - **Batch normalization**: Neural network layer normalization
   - **Feature scaling**: Standardization/Z-score normalization
   - **Anomaly detection**: Compute baseline statistics
   - **Quality control**: Process capability analysis
   - **A/B testing**: Group statistics comparison
   
   Efficiency comparison:
   ```clojure
   ;; Less efficient (two passes):
   (let [mean (af-mean data dim)
         var (af-var-v2 data bias dim)]
     [mean var])
   
   ;; More efficient (optimized single pass):
   (let [mean-ptr (mem/alloc-pointer ::mem/pointer)
         var-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-meanvar mean-ptr var-ptr data empty-weights bias dim)]
     [(mem/read-pointer mean-ptr ::mem/pointer)
      (mem/read-pointer var-ptr ::mem/pointer)])
   ```
   
   When to use:
   - Always prefer af-meanvar when both statistics needed
   - Use separate functions only if just one statistic required
   - Especially beneficial for large arrays
   - Critical for performance in tight loops
   
   Returns:
   ArrayFire error code (AF_SUCCESS on success)
   
   See also:
   - af-mean, af-mean-weighted: Compute only mean
   - af-var-v2, af-var-weighted: Compute only variance
   - af-stdev-v2: Standard deviation (√variance)"
  "af_meanvar" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/long] ::mem/int)
