(ns org.soulspace.arrayfire.ffi.stdev
  "ArrayFire FFI bindings for standard deviation computation.

  Standard deviation is a fundamental statistical measure quantifying the
  dispersion or spread of data values around the mean. It indicates how much
  individual data points deviate from the average.

  ## Mathematical Foundation

  For a dataset x = {x₁, x₂, ..., xₙ}:

  **Population Standard Deviation** (unbiased, N denominator):
  ```
  σ = √(Σ(xᵢ - μ)² / N)
  ```

  **Sample Standard Deviation** (biased, N-1 denominator):
  ```
  s = √(Σ(xᵢ - x̄)² / (N-1))
  ```

  where:
  - σ (sigma) = population standard deviation
  - s = sample standard deviation
  - μ (mu) = population mean
  - x̄ = sample mean
  - N = number of elements
  - Σ = sum over all elements

  ## Bias Correction

  The choice between N and N-1 in the denominator depends on whether computing
  the population or sample standard deviation:

  ### Population Standard Deviation (AF_VARIANCE_POPULATION)
  - Uses denominator N
  - Assumes data represents entire population
  - Provides actual standard deviation of the given data
  - Biased estimator when used on samples

  ### Sample Standard Deviation (AF_VARIANCE_SAMPLE)
  - Uses denominator N-1 (Bessel's correction)
  - Corrects bias when estimating population σ from sample
  - Unbiased estimator of population standard deviation
  - Recommended for statistical inference from samples

  **When to use which:**
  - Use POPULATION when analyzing complete dataset (entire population)
  - Use SAMPLE when dataset is a sample from larger population (statistical inference)

  ## Relationship to Variance

  Standard deviation is the square root of variance:
  ```
  σ = √(σ²)
  ```

  This makes standard deviation:
  - In same units as original data (unlike variance)
  - More interpretable than variance
  - Commonly used in practical applications

  ## Computational Complexity

  - Per-element computation: O(N)
  - Requires two passes: one for mean, one for deviation
  - Memory: O(N) temporary storage
  - Along dimension: O(N×M) where M is dimension size
  - GPU parallelization provides significant speedup

  ## Numerical Stability

  ArrayFire uses the two-pass algorithm:
  1. Compute mean: μ = Σxᵢ / N
  2. Compute variance: σ² = Σ(xᵢ - μ)² / N
  3. Take square root: σ = √(σ²)

  This is numerically stable and avoids catastrophic cancellation that can
  occur in single-pass algorithms.

  ## Type Promotion

  Integer types are promoted to floating-point for standard deviation:
  - s8, u8, s16, u16, s32, u32, b8 → f32
  - s64, u64 → f64
  - f32 → f32
  - f64 → f64
  - c32, c64 → Currently not supported (sqrt of complex not implemented)

  ## Dimension-wise Operations

  Standard deviation can be computed:
  - Along specific dimension (column-wise, row-wise, etc.)
  - Across entire array (all elements)

  Example for 2D array [3×4]:
  - dim=0: Standard deviation of each column (3 values → vector of 4)
  - dim=1: Standard deviation of each row (4 values → vector of 3)
  - all: Standard deviation of all 12 elements (→ scalar)

  ## Applications

  - Statistical analysis: Measuring data spread and dispersion
  - Quality control: Process variation monitoring
  - Finance: Risk measurement (volatility)
  - Machine learning: Feature normalization, anomaly detection
  - Signal processing: Noise level estimation
  - Image processing: Contrast and texture analysis
  - Scientific data: Measurement uncertainty
  - A/B testing: Statistical significance

  ## Common Interpretations

  **68-95-99.7 Rule** (for normal distributions):
  - 68% of data within ±1σ of mean
  - 95% of data within ±2σ of mean
  - 99.7% of data within ±3σ of mean

  **Coefficient of Variation**: CV = σ/μ (relative standard deviation)

  See also:
  - Variance functions (af_var) for squared deviation
  - Mean functions (af_mean) for average values
  - Median functions (af_median) for robust central tendency"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem :refer [deref-ptr]])
  (:refer-clojure :exclude [deref]))

;;
;; Standard Deviation Functions
;;

(defcfn af-stdev
  "Compute standard deviation along a dimension (deprecated).

  Computes the standard deviation of array elements along the specified
  dimension using population variance (N denominator).

  Parameters:
  - out: Output array containing standard deviations
  - in: Input array
  - dim: Dimension along which to compute (0-3)

  Returns:
  Error code indicating success or failure.

  Deprecation Notice:
  This function is deprecated. Use af-stdev-v2 instead, which allows
  specifying the bias type (population vs sample).

  Example:
  ```clojure
  ;; Deprecated - use af-stdev-v2 instead
  (let [data (af-randn [100 10] :f32)
        stdev-out (mem/alloc-instance ::mem/pointer)]
    (af-stdev stdev-out data 0) ;; standard deviation of each column
    )
  ```

  See also:
  - af_stdev (ArrayFire C API, deprecated)
  - af-stdev-v2: Preferred version with bias control"
  "af_stdev" [::mem/pointer ::mem/pointer ::mem/long] ::mem/int)

(defcfn af-stdev-v2
  "Compute standard deviation along a dimension with bias control.

  Computes the standard deviation of array elements along the specified
  dimension, with control over the bias correction (population vs sample).

  Parameters:
  - out: Output array containing standard deviations
  - in: Input array
  - bias: Variance bias type (af_var_bias enum)
    * AF_VARIANCE_POPULATION (0): Use N denominator (population)
    * AF_VARIANCE_SAMPLE (1): Use N-1 denominator (sample, Bessel's correction)
  - dim: Dimension along which to compute (0-3)

  Returns:
  Error code indicating success or failure.

  Example (column-wise standard deviation):
  ```clojure
  ;; Compute standard deviation of each column
  (let [data (af-randn [100 10] :f32)  ;; 100 rows, 10 columns
        stdev-cols (mem/alloc-instance ::mem/pointer)]
    (af-stdev-v2 stdev-cols data 0 0) ;; dim=0, population bias
    ;; Result: vector of 10 values (one per column)
    )
  ```

  Example (row-wise standard deviation):
  ```clojure
  ;; Compute standard deviation of each row
  (let [data (af-constant [5 20] matrix-data :f64)
        stdev-rows (mem/alloc-instance ::mem/pointer)]
    (af-stdev-v2 stdev-rows data 1 1) ;; dim=1, sample bias
    ;; Result: vector of 5 values (one per row)
    )
  ```

  Example (3D array):
  ```clojure
  ;; Standard deviation along different dimensions
  (let [volume (af-randn [64 64 32] :f32)
        stdev-z (mem/alloc-instance ::mem/pointer)]
    (af-stdev-v2 stdev-z volume 0 2) ;; along z-dimension
    ;; Result: [64, 64, 1] array
    )
  ```

  Example (population vs sample):
  ```clojure
  ;; Compare population and sample standard deviation
  (let [sample-data (af-constant [30] data :f32)
        pop-std (mem/alloc-instance ::mem/pointer)
        smp-std (mem/alloc-instance ::mem/pointer)]
    (af-stdev-v2 pop-std sample-data 0 0) ;; population (N)
    (af-stdev-v2 smp-std sample-data 1 0) ;; sample (N-1)
    ;; Sample std will be slightly larger (corrected for bias)
    )
  ```

  Dimension Behavior:
  For array of shape [m, n, p, q]:
  - dim=0: Compute along rows → output shape [1, n, p, q]
  - dim=1: Compute along columns → output shape [m, 1, p, q]
  - dim=2: Compute along 3rd dimension → output shape [m, n, 1, q]
  - dim=3: Compute along 4th dimension → output shape [m, n, p, 1]

  Bias Selection Guide:
  - **Population (AF_VARIANCE_POPULATION = 0)**:
    * Use when analyzing complete dataset
    * Descriptive statistics of given data
    * No inference to larger population needed
    * Example: Standard deviation of all exam scores in a class

  - **Sample (AF_VARIANCE_SAMPLE = 1)**:
    * Use for statistical inference
    * Estimating population σ from sample
    * Unbiased estimator (Bessel's correction)
    * Example: Standard deviation of sample to estimate population

  Type Support:
  - Integer types → promoted to f32 or f64
  - f32 → f32
  - f64 → f64
  - Complex types (c32, c64) not currently supported

  Performance:
  - Two-pass algorithm: mean computation + deviation computation
  - GPU parallelization across dimensions
  - Efficient memory access patterns
  - O(N) complexity per dimension

  Notes:
  - Output has one fewer element along specified dimension
  - Empty arrays return empty output
  - NaN values propagate through computation
  - For single element along dimension: result is 0

  See also:
  - af_stdev_v2 (ArrayFire C API)
  - af-stdev-all-v2: Standard deviation of all elements
  - af-var-v2: Variance computation (σ²)
  - af-mean: Mean computation"
  "af_stdev_v2" [::mem/pointer ::mem/pointer ::mem/int ::mem/long] ::mem/int)

(defcfn af-stdev-all
  "Compute standard deviation of all elements (deprecated).

  Computes the standard deviation of all elements in the array, returning
  a scalar value. Uses population variance (N denominator).

  Parameters:
  - real: Output pointer for real part of standard deviation
  - imag: Output pointer for imaginary part (currently unused)
  - in: Input array

  Returns:
  Error code indicating success or failure.

  Deprecation Notice:
  This function is deprecated. Use af-stdev-all-v2 instead, which allows
  specifying the bias type.

  Example:
  ```clojure
  ;; Deprecated - use af-stdev-all-v2
  (let [data (af-randn [1000] :f32)
        stdev-val (mem/alloc-instance ::mem/double)]
    (af-stdev-all stdev-val nil data)
    (println \"Standard deviation:\" (mem/read-double stdev-val)))
  ```

  See also:
  - af_stdev_all (ArrayFire C API, deprecated)
  - af-stdev-all-v2: Preferred version with bias control"
  "af_stdev_all" [::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-stdev-all-v2
  "Compute standard deviation of all elements with bias control.

  Computes the standard deviation across all elements in the array, reducing
  it to a single scalar value. Provides control over bias correction.

  Parameters:
  - real: Output pointer for real part of standard deviation
  - imag: Output pointer for imaginary part (currently unused, pass NULL/nil)
  - in: Input array
  - bias: Variance bias type (af_var_bias enum)
    * AF_VARIANCE_POPULATION (0): Use N denominator
    * AF_VARIANCE_SAMPLE (1): Use N-1 denominator (Bessel's correction)

  Returns:
  Error code indicating success or failure.

  Example (basic usage):
  ```clojure
  ;; Compute standard deviation of entire array
  (let [data (af-randn [100 100] :f32)
        stdev (mem/alloc-instance ::mem/double)]
    (af-stdev-all-v2 stdev nil data 0) ;; population
    (println \"σ =\" (mem/read-double stdev)))
  ```

  Example (population vs sample):
  ```clojure
  ;; Compare population and sample standard deviation
  (let [dataset (af-constant [1000] values :f64)
        pop-std (mem/alloc-instance ::mem/double)
        smp-std (mem/alloc-instance ::mem/double)]
    (af-stdev-all-v2 pop-std nil dataset 0) ;; population
    (af-stdev-all-v2 smp-std nil dataset 1) ;; sample
    (let [pop (mem/read-double pop-std)
          smp (mem/read-double smp-std)]
      (println \"Population σ:\" pop)
      (println \"Sample s:\" smp)
      (println \"Ratio:\" (/ smp pop)))) ;; ≈ √(N/(N-1))
  ```

  Example (statistical analysis):
  ```clojure
  ;; Compute mean and standard deviation for normalization
  (defn normalize-data [data]
    (let [mean-val (mem/alloc-instance ::mem/double)
          stdev-val (mem/alloc-instance ::mem/double)]
      (af-mean-all mean-val nil data)
      (af-stdev-all-v2 stdev-val nil data 1) ;; sample std
      (let [μ (mem/read-double mean-val)
            σ (mem/read-double stdev-val)
            normalized (mem/alloc-instance ::mem/pointer)]
        ;; z-score: (x - μ) / σ
        (af-sub normalized data (af-constant (af-get-dims data) μ))
        (af-div normalized normalized (af-constant (af-get-dims data) σ))
        normalized)))
  ```

  Example (coefficient of variation):
  ```clojure
  ;; Compute coefficient of variation (CV = σ/μ)
  (defn coefficient-of-variation [data]
    (let [mean-val (mem/alloc-instance ::mem/double)
          stdev-val (mem/alloc-instance ::mem/double)]
      (af-mean-all mean-val nil data)
      (af-stdev-all-v2 stdev-val nil data 1)
      (/ (mem/read-double stdev-val)
         (mem/read-double mean-val))))
  ```

  Example (outlier detection):
  ```clojure
  ;; Flag values beyond 3 standard deviations (3σ rule)
  (defn find-outliers [data]
    (let [mean-val (mem/alloc-instance ::mem/double)
          stdev-val (mem/alloc-instance ::mem/double)]
      (af-mean-all mean-val nil data)
      (af-stdev-all-v2 stdev-val nil data 0)
      (let [μ (mem/read-double mean-val)
            σ (mem/read-double stdev-val)
            threshold (* 3 σ)
            deviation (af-abs (af-sub data (af-constant dims μ)))
            outliers (af-gt deviation (af-constant dims threshold))]
        outliers)))
  ```

  Output Format:
  - real: Contains the standard deviation value (double precision)
  - imag: Currently unused (complex not supported), pass NULL/nil

  Type Conversion:
  - All integer types compute as f32 or f64
  - f32 input → f32 computation → double output
  - f64 input → f64 computation → double output
  - Result always returned as double regardless of input type

  Bias Selection:
  Choose based on context:
  - **Population (0)**: Analyzing complete dataset
  - **Sample (1)**: Inferring population from sample (statistics)

  Mathematical Example:
  Data: [2, 4, 4, 4, 5, 5, 7, 9]
  Mean: μ = 5
  Deviations: [-3, -1, -1, -1, 0, 0, 2, 4]
  Squared: [9, 1, 1, 1, 0, 0, 4, 16] → Sum = 32

  Population: σ = √(32/8) = √4 = 2.0
  Sample: s = √(32/7) = √4.571 = 2.138

  Use Cases:
  - Data normalization (z-score standardization)
  - Outlier detection (n-sigma rules)
  - Quality control (process capability)
  - Risk measurement (volatility)
  - Coefficient of variation
  - Statistical inference

  Performance:
  - Single reduction over all elements
  - Two-pass algorithm for numerical stability
  - GPU acceleration for large arrays
  - O(N) complexity

  Notes:
  - Complex types (c32, c64) currently not supported
  - imag parameter should be NULL/nil
  - Empty arrays behavior implementation-dependent
  - NaN handling: NaN propagates to result

  See also:
  - af_stdev_all_v2 (ArrayFire C API)
  - af-stdev-v2: Dimension-wise standard deviation
  - af-var-all-v2: Variance of all elements
  - af-mean-all: Mean of all elements"
  "af_stdev_all_v2" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)
