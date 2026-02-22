(ns org.soulspace.arrayfire.integration.unified-api.statistics
  "High-level Clojure wrapper for ArrayFire statistical functions.

  Statistical operations compute measures of central tendency, dispersion,
  correlation, and data characteristics. These are fundamental for data
  analysis, machine learning, scientific computing, and signal processing.

  ## Quick Start

  ```clojure
  (require '[org.soulspace.arrayfire.integration.statistics :as stats])
  (require '[org.soulspace.arrayfire.integration.array :as array])

  ;; Basic statistics
  (let [data (array/random [1000 100])]
    (println \"Mean:\" (stats/mean data))
    (println \"Stdev:\" (stats/stdev data))
    (println \"Median:\" (stats/median data)))

  ;; Weighted statistics
  (let [values (array/from-vec [1.0 2.0 3.0 4.0] [4])
        weights (array/from-vec [0.1 0.2 0.3 0.4] [4])
        wmean (stats/mean-weighted values weights)]
    wmean)

  ;; Correlation and covariance
  (let [x (array/random [100])
        y (array/random [100])
        corr (stats/corrcoef x y)
        cov (stats/cov x y)]
    {:correlation corr :covariance cov})

  ;; Top-k selection
  (let [data (array/random [1000])
        [top-vals top-idx] (stats/topk data 10)]
    (println \"Top 10 values:\" top-vals))
  ```

  ## Statistical Measures

  ### Central Tendency
  Measures of the \"center\" of data distribution:

  **Mean (Average)**:
  μ = (1/N) × Σ xᵢ

  - Most common measure of central tendency
  - Sensitive to outliers
  - Best for normally distributed data
  - Functions: `mean`, `mean-weighted`, `mean-all`

  **Median**:
  Middle value when data is sorted

  - Robust to outliers
  - Better for skewed distributions
  - 50th percentile of data
  - Functions: `median`, `median-all`

  ### Dispersion (Spread)
  Measures of how spread out the data is:

  **Variance**:
  - Population: σ² = (1/N) × Σ(xᵢ - μ)²
  - Sample: s² = 1/(N-1) × Σ(xᵢ - μ)² (Bessel's correction)

  - Measures average squared deviation from mean
  - Units are squared (e.g., meters²)
  - Functions: `var`, `var-weighted`, `var-all`

  **Standard Deviation**:
  σ = √variance

  - Square root of variance
  - Same units as original data
  - More interpretable than variance
  - Functions: `stdev`, `stdev-all`

  ### Correlation and Covariance
  Measures of relationships between variables:

  **Covariance**:
  Cov(X,Y) = (1/N) × Σ(xᵢ - μₓ)(yᵢ - μᵧ)

  - Measures how two variables vary together
  - Positive: variables increase together
  - Negative: one increases as other decreases
  - Zero: no linear relationship
  - Magnitude depends on data scale
  - Functions: `cov`, `meanvar`

  **Correlation Coefficient**:
  ρ(X,Y) = Cov(X,Y) / (σₓ × σᵧ)

  - Normalized covariance [-1, 1]
  - +1: Perfect positive correlation
  - -1: Perfect negative correlation
  - 0: No linear correlation
  - Scale-independent
  - Function: `corrcoef`

  ## Bias Types (Variance/Stdev)

  ArrayFire uses the `af_var_bias` enum to control variance calculation:

  ### AF_VARIANCE_DEFAULT (0) - Sample Bias
  Uses Bessel's correction (N-1 denominator):
  ```
  s² = 1/(N-1) × Σ(xᵢ - μ)²
  ```
  - **Recommended for samples from larger population**
  - Provides unbiased estimator
  - Default in most statistical software

  ### AF_VARIANCE_SAMPLE (1) - Sample Bias
  Same as AF_VARIANCE_DEFAULT:
  ```
  s² = 1/(N-1) × Σ(xᵢ - μ)²
  ```
  - Explicit name for clarity
  - Use when data is a sample

  ### AF_VARIANCE_POPULATION (2) - Population Bias
  Uses full population (N denominator):
  ```
  σ² = (1/N) × Σ(xᵢ - μ)²
  ```
  - Use when data is complete population
  - Slightly smaller value than sample variance
  - Biased estimator when used on samples

  ## Top-K Selection

  Find the k largest or smallest values efficiently without full sorting:

  **Orders**:
  - AF_TOPK_MIN (1): k smallest values (ascending)
  - AF_TOPK_MAX (2): k largest values (descending)
  - AF_TOPK_STABLE (4): Preserve order of equal values (bitwise flag)
  - AF_TOPK_DEFAULT (0): Defaults to MAX

  **Complexity**:
  - O(n log k) vs O(n log n) for full sorting
  - Very efficient for k ≪ n
  - GPU accelerated for large arrays

  ## Dimension Reduction

  Most functions support dimension-wise computation:
  - dim=-1: First non-singleton dimension (default)
  - dim=0: Along first dimension (rows)
  - dim=1: Along second dimension (columns)
  - dim=2, 3: Higher dimensions

  Example:
  ```clojure
  ;; Column means of 100×10 matrix
  (let [data (array/random [100 10])
        col-means (mean data 0)]  ;; Shape: [1 10]
    col-means)
  ```

  ## Weighted Statistics

  Weight each data point by importance:
  ```clojure
  ;; More recent data has higher weight
  (let [data (array/from-vec [1.0 2.0 3.0 4.0] [4])
        weights (array/from-vec [0.1 0.2 0.3 0.4] [4])]
    (mean-weighted data weights))  ;; Recent values weighted more
  ```

  Applications:
  - Time series with recency bias
  - Quality-weighted measurements
  - Confidence-weighted aggregation

  ## Performance Tips

  1. **Use dimension-wise operations**: Faster than iterating
  2. **Batch computations**: Process multiple arrays together
  3. **Choose right measure**: Median is slower than mean
  4. **GPU acceleration**: 10-100× speedup for large arrays
  5. **Top-k vs sorting**: Use topk for small k values
  6. **All-reduce functions**: Use `-all` variants when reducing entire array

  ## Common Use Cases

  ### Data Analysis
  - Descriptive statistics for exploratory analysis
  - Outlier detection via standard deviations
  - Data normalization (z-scores)

  ### Machine Learning
  - Feature normalization/standardization
  - Top-k predictions in classification
  - Correlation-based feature selection
  - Weighted loss functions

  ### Signal Processing
  - Signal-to-noise ratio calculation
  - Statistical process control
  - Anomaly detection

  ### Scientific Computing
  - Experimental data analysis
  - Uncertainty quantification
  - Monte Carlo simulations

  See also:
  - ArrayFire statistics documentation: https://arrayfire.org/docs/group__stat__func__mean.htm
  - Reduction operations in integration.algorithm
  - BLAS operations for linear algebra"
  (:refer-clojure :exclude [var])
  (:require [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.definitions :as defs]
            [org.soulspace.arrayfire.ffi.c-api.mean :as mean]
            [org.soulspace.arrayfire.ffi.c-api.var :as variance]
            [org.soulspace.arrayfire.ffi.c-api.stdev :as stdev]
            [org.soulspace.arrayfire.ffi.c-api.median :as median]
            [org.soulspace.arrayfire.ffi.c-api.covariance :as covariance]
            [org.soulspace.arrayfire.ffi.c-api.corrcoef :as corrcoef]
            [org.soulspace.arrayfire.ffi.c-api.topk :as topk]
            [org.soulspace.arrayfire.integration.base.error :refer [check!]]
            [org.soulspace.arrayfire.integration.base.resource :as res])
  (:import (org.soulspace.arrayfire.integration.base.resource AFArray)))

;;;
;;; Mean (Central Tendency)
;;;

(defn mean
  "Compute the arithmetic mean along a dimension.
   
   The mean is the sum of values divided by the count:
   μ = (1/N) × Σ xᵢ
   
   This is the most common measure of central tendency, representing
   the \"center\" of the data distribution.
   
   Parameters:
   - in: Input array (AFArray)
   - dim: Dimension along which to compute mean (default -1 for first non-singleton)
   
   Returns:
   AFArray with mean values (dimension reduced to size 1)
   
   Example:
   ```clojure
   ;; Overall mean
   (let [data (array/random [100 10])
         m (mean data)]  ;; Shape: [1 10]
     m)
   
   ;; Column means
   (let [data (array/random [100 10])
         col-means (mean data 0)]  ;; Shape: [1 10]
     col-means)
   
   ;; Row means
   (let [data (array/random [100 10])
         row-means (mean data 1)]  ;; Shape: [100 1]
     row-means)
   ```
   
   See also:
   - mean-weighted: Weighted mean
   - mean-all: Reduce to scalar
   - median: Robust alternative"
  ([^AFArray in]
   (mean in -1))
  ([^AFArray in dim]
   (let [out (res/native-af-array-pointer)]
     (check! (mean/af-mean out (res/af-handle in) (int dim))
                 "af-mean")
     (res/af-array-new (res/deref-af-array out)))))

(defn mean-weighted
  "Compute weighted arithmetic mean along a dimension.
   
   Each value is weighted by its importance:
   μw = Σ(wᵢ × xᵢ) / Σ wᵢ
   
   Useful when different data points have different reliability,
   importance, or confidence levels.
   
   Parameters:
   - in: Input array (AFArray)
   - weights: Weight for each element (AFArray, same shape as in)
   - dim: Dimension along which to compute mean (default -1)
   
   Returns:
   AFArray with weighted mean values
   
   Example:
   ```clojure
   ;; Recent data weighted more heavily
   (let [data (array/from-vec [1.0 2.0 3.0 4.0] [4])
         weights (array/from-vec [0.1 0.2 0.3 0.4] [4])
         wmean (mean-weighted data weights)]
     wmean)  ;; Emphasizes recent values
   
   ;; Quality-weighted measurements
   (let [measurements (array/from-vec [10.1 9.9 10.0] [3])
         quality (array/from-vec [0.8 0.9 1.0] [3])]  ;; confidence
     (mean-weighted measurements quality))
   ```
   
   See also:
   - mean: Unweighted mean
   - mean-all-weighted: Reduce to scalar
   - var-weighted: Weighted variance"
  ([^AFArray in ^AFArray weights]
   (mean-weighted in weights -1))
  ([^AFArray in ^AFArray weights dim]
   (let [out (res/native-af-array-pointer)]
     (check! (mean/af-mean-weighted out (res/af-handle in) (res/af-handle weights) (int dim))
                 "af-mean-weighted")
     (res/af-array-new (res/deref-af-array out)))))

(defn mean-all
  "Compute mean of all elements in the array.
   
   Reduces entire array to a single scalar value.
   
   Parameters:
   - in: Input array (AFArray)
   
   Returns:
   - For real arrays: mean as double
   - For complex arrays: [real imag] vector
   
   Example:
   ```clojure
   (let [data (array/from-vec [1.0 2.0 3.0 4.0 5.0] [5])
         m (mean-all data)]
     m)  ;; => 3.0
   
   ;; Complex array
   (let [complex-data (array/complex [1.0 2.0] [3.0 4.0])
         m (mean-all complex-data)]
     m)  ;; => [1.5 3.5]
   ```
   
   See also:
   - mean: Dimension-wise mean
   - mean-all-weighted: Weighted scalar mean"
  [^AFArray in]
  (let [real-buf (mem/alloc-instance ::mem/double)
        imag-buf (mem/alloc-instance ::mem/double)]
    (check! (mean/af-mean-all real-buf imag-buf (res/af-handle in))
                "af-mean-all")
    (let [real (mem/read-double real-buf)
          imag (mem/read-double imag-buf)]
      (if (zero? imag)
        real
        [real imag]))))

(defn mean-all-weighted
  "Compute weighted mean of all elements in the array.
   
   Reduces entire array to scalar with weights.
   
   Parameters:
   - in: Input array (AFArray)
   - weights: Weight array (AFArray, same shape as in)
   
   Returns:
   - For real arrays: weighted mean as double
   - For complex arrays: [real imag] vector
   
   Example:
   ```clojure
   (let [data (array/from-vec [1.0 2.0 3.0] [3])
         weights (array/from-vec [0.2 0.3 0.5] [3])
         wm (mean-all-weighted data weights)]
     wm)  ;; Emphasizes last value
   ```
   
   See also:
   - mean-all: Unweighted scalar mean
   - mean-weighted: Dimension-wise weighted mean"
  [^AFArray in ^AFArray weights]
  (let [real-buf (mem/alloc-instance ::mem/double)
        imag-buf (mem/alloc-instance ::mem/double)]
    (check! (mean/af-mean-all-weighted real-buf imag-buf (res/af-handle in) (res/af-handle weights))
                "af-mean-all-weighted")
    (let [real (mem/read-double real-buf)
          imag (mem/read-double imag-buf)]
      (if (zero? imag)
        real
        [real imag]))))

;;;
;;; Variance (Dispersion)
;;;

(defn var
  "Compute variance along a dimension.
   
   Variance measures spread of data around the mean:
   - Sample: s² = 1/(N-1) × Σ(xᵢ - μ)²  (Bessel's correction)
   - Population: σ² = (1/N) × Σ(xᵢ - μ)²
   
   Parameters:
   - in: Input array (AFArray)
   - bias: Variance bias type (VARIANCE_SAMPLE, VARIANCE_POPULATION, default VARIANCE_SAMPLE)
   - dim: Dimension along which to compute variance (default -1)
   
   Returns:
   AFArray with variance values
   
   Example:
   ```clojure
   ;; Sample variance (default, unbiased estimator)
   (let [data (array/random [1000])
         v (var data)]
     v)
   
   ;; Population variance
   (let [data (array/random [1000])
         v (var data VARIANCE_POPULATION)]
     v)
   
   ;; Column variances
   (let [data (array/random [100 10])
         col-vars (var data VARIANCE_SAMPLE 0)]
     col-vars)
   ```
   
   See also:
   - stdev: Standard deviation (sqrt of variance)
   - var-weighted: Weighted variance
   - var-all: Reduce to scalar"
  ([^AFArray in]
   (let [out (res/native-af-array-pointer)]
     (check! (variance/af-var-v2 out (res/af-handle in) (int defs/AF_VARIANCE_SAMPLE) (int -1))
                 "af-var-v2")
     (res/af-array-new (res/deref-af-array out))))
  ([^AFArray in bias]
   (let [out (res/native-af-array-pointer)]
     (check! (variance/af-var-v2 out (res/af-handle in) (int bias) (int -1))
                 "af-var-v2")
     (res/af-array-new (res/deref-af-array out))))
  ([^AFArray in bias dim]
   (let [out (res/native-af-array-pointer)]
     (check! (variance/af-var-v2 out (res/af-handle in) (int bias) (int dim))
                 "af-var-v2")
     (res/af-array-new (res/deref-af-array out)))))

(defn var-weighted
  "Compute weighted variance along a dimension.
   
   Weighted variance accounts for different importance of values:
   σ²w = Σ wᵢ(xᵢ - μw)² / Σ wᵢ
   
   Parameters:
   - in: Input array (AFArray)
   - weights: Weight array (AFArray, same shape as in)
   - dim: Dimension along which to compute variance (default -1)
   
   Returns:
   AFArray with weighted variance values
   
   Example:
   ```clojure
   (let [data (array/from-vec [1.0 2.0 3.0 4.0] [4])
         weights (array/from-vec [0.1 0.2 0.3 0.4] [4])
         wv (var-weighted data weights)]
     wv)
   ```
   
   See also:
   - var: Unweighted variance
   - mean-weighted: Weighted mean
   - var-all-weighted: Reduce to scalar"
  ([^AFArray in ^AFArray weights]
   (var-weighted in weights -1))
  ([^AFArray in ^AFArray weights dim]
   (let [out (res/native-af-array-pointer)]
     (check! (variance/af-var-weighted out (res/af-handle in) (res/af-handle weights) (int dim))
                 "af-var-weighted")
     (res/af-array-new (res/deref-af-array out)))))

(defn var-all
  "Compute variance of all elements in the array.
   
   Reduces entire array to scalar variance.
   
   Parameters:
   - in: Input array (AFArray)
   - bias: Variance bias type (default VARIANCE_SAMPLE)
   
   Returns:
   - For real arrays: variance as double
   - For complex arrays: [real imag] vector
   
   Example:
   ```clojure
   (let [data (array/from-vec [1.0 2.0 3.0 4.0 5.0] [5])
         v (var-all data)]
     v)  ;; Sample variance
   ```
   
   See also:
   - var: Dimension-wise variance
   - stdev-all: Scalar standard deviation"
  ([^AFArray in]
   (var-all in defs/AF_VARIANCE_SAMPLE))
  ([^AFArray in bias]
   (let [real-buf (mem/alloc-instance ::mem/double)
         imag-buf (mem/alloc-instance ::mem/double)]
     (check! (variance/af-var-all-v2 real-buf imag-buf (res/af-handle in) (int bias))
                 "af-var-all-v2")
     (let [real (mem/read-double real-buf)
           imag (mem/read-double imag-buf)]
       (if (zero? imag)
         real
         [real imag])))))

(defn var-all-weighted
  "Compute weighted variance of all elements.
   
   Parameters:
   - in: Input array (AFArray)
   - weights: Weight array (AFArray)
   
   Returns:
   - For real arrays: weighted variance as double
   - For complex arrays: [real imag] vector
   
   Example:
   ```clojure
   (let [data (array/from-vec [1.0 2.0 3.0] [3])
         weights (array/from-vec [0.2 0.3 0.5] [3])
         wv (var-all-weighted data weights)]
     wv)
   ```
   
   See also:
   - var-all: Unweighted scalar variance
   - var-weighted: Dimension-wise weighted variance"
  [^AFArray in ^AFArray weights]
  (let [real-buf (mem/alloc-instance ::mem/double)
        imag-buf (mem/alloc-instance ::mem/double)]
    (check! (variance/af-var-all-weighted real-buf imag-buf (res/af-handle in) (res/af-handle weights))
                "af-var-all-weighted")
    (let [real (mem/read-double real-buf)
          imag (mem/read-double imag-buf)]
      (if (zero? imag)
        real
        [real imag]))))

;;;
;;; Standard Deviation (Dispersion)
;;;

(defn stdev
  "Compute standard deviation along a dimension.
   
   Standard deviation is the square root of variance:
   σ = √variance
   
   More interpretable than variance as it's in the same units as the data.
   
   Parameters:
   - in: Input array (AFArray)
   - bias: Variance bias type (VARIANCE_SAMPLE, VARIANCE_POPULATION, default VARIANCE_SAMPLE)
   - dim: Dimension along which to compute stdev (default -1)
   
   Returns:
   AFArray with standard deviation values
   
   Example:
   ```clojure
   ;; Sample standard deviation
   (let [data (array/random [1000])
         s (stdev data)]
     s)
   
   ;; For z-score normalization
   (let [data (array/random [100 10])
         m (mean data 0)
         s (stdev data VARIANCE_SAMPLE 0)
         z-scores (div (sub data m) s)]
     z-scores)
   ```
   
   See also:
   - var: Variance (stdev squared)
   - stdev-all: Reduce to scalar
   - mean: For normalization"
  ([^AFArray in]
   (stdev in defs/AF_VARIANCE_SAMPLE -1))
  ([^AFArray in bias]
   (stdev in bias -1))
  ([^AFArray in bias dim]
   (let [out (res/native-af-array-pointer)]
     (check! (stdev/af-stdev-v2 out (res/af-handle in) (int bias) (int dim))
                 "af-stdev-v2")
     (res/af-array-new (res/deref-af-array out)))))

(defn stdev-all
  "Compute standard deviation of all elements.
   
   Reduces entire array to scalar standard deviation.
   
   Parameters:
   - in: Input array (AFArray)
   - bias: Variance bias type (default VARIANCE_SAMPLE)
   
   Returns:
   - For real arrays: stdev as double
   - For complex arrays: [real imag] vector
   
   Example:
   ```clojure
   (let [data (array/from-vec [1.0 2.0 3.0 4.0 5.0] [5])
         s (stdev-all data)]
     s)
   ```
   
   See also:
   - stdev: Dimension-wise stdev
   - var-all: Scalar variance"
  ([^AFArray in]
   (stdev-all in defs/AF_VARIANCE_SAMPLE))
  ([^AFArray in bias]
   (let [real-buf (mem/alloc-instance ::mem/double)
         imag-buf (mem/alloc-instance ::mem/double)]
     (check! (stdev/af-stdev-all-v2 real-buf imag-buf (res/af-handle in) (int bias))
                 "af-stdev-all-v2")
     (let [real (mem/read-double real-buf)
           imag (mem/read-double imag-buf)]
       (if (zero? imag)
         real
         [real imag])))))

;;;
;;; Median (Robust Central Tendency)
;;;

(defn median
  "Compute median along a dimension.
   
   The median is the middle value when data is sorted. It's robust
   to outliers unlike the mean, making it better for skewed distributions.
   
   For odd N: median = sorted[N/2]
   For even N: median = (sorted[N/2-1] + sorted[N/2]) / 2
   
   Parameters:
   - in: Input array (AFArray)
   - dim: Dimension along which to compute median (default -1)
   
   Returns:
   AFArray with median values
   
   Example:
   ```clojure
   ;; Median is robust to outliers
   (let [data (array/from-vec [1.0 2.0 3.0 4.0 1000.0] [5])
         med (median data)    ;; => 3.0 (robust)
         avg (mean data)]     ;; => 202.0 (skewed by outlier)
     {:median med :mean avg})
   
   ;; Column medians
   (let [data (array/random [100 10])
         col-medians (median data 0)]
     col-medians)
   ```
   
   Note: Median is O(n log n) due to sorting, slower than mean O(n).
   
   See also:
   - mean: Faster but sensitive to outliers
   - median-all: Reduce to scalar"
  ([^AFArray in]
   (median in -1))
  ([^AFArray in dim]
   (let [out (res/native-af-array-pointer)]
     (check! (median/af-median out (res/af-handle in) (int dim))
                 "af-median")
     (res/af-array-new (res/deref-af-array out)))))

(defn median-all
  "Compute median of all elements.
   
   Reduces entire array to scalar median value.
   
   Parameters:
   - in: Input array (AFArray)
   
   Returns:
   - For real arrays: median as double
   - For complex arrays: [real imag] vector
   
   Example:
   ```clojure
   (let [data (array/from-vec [5.0 1.0 3.0 2.0 4.0] [5])
         m (median-all data)]
     m)  ;; => 3.0
   ```
   
   See also:
   - median: Dimension-wise median
   - mean-all: Scalar mean"
  [^AFArray in]
  (let [real-buf (mem/alloc-instance ::mem/double)
        imag-buf (mem/alloc-instance ::mem/double)]
    (check! (median/af-median-all real-buf imag-buf (res/af-handle in))
                "af-median-all")
    (let [real (mem/read-double real-buf)
          imag (mem/read-double imag-buf)]
      (if (zero? imag)
        real
        [real imag]))))

;;;
;;; Combined Mean and Variance
;;;

(defn meanvar
  "Compute both mean and variance in a single operation.
   
   More efficient than calling mean and var separately as it
   computes both in one pass over the data.
   
   Parameters:
   - in: Input array (AFArray)
   - weights: Weight array (AFArray, same shape as in)
   - bias: Variance bias type (default VARIANCE_SAMPLE)
   - dim: Dimension along which to compute (default -1)
   
   Returns:
   A map containing:
   - :mean - AFArray with mean values
   - :var - AFArray with variance values
   
   Example:
   ```clojure
   (let [data (array/random [100 10])
         weights (array/constant 1.0 [100 10])
         {:keys [mean var]} (meanvar data weights)]
     {:mean mean :variance var :stdev (sqrt var)})
   
   ;; For normalization
   (let [data (array/random [1000])
         weights (array/constant 1.0 [1000])
         {:keys [mean var]} (meanvar data weights VARIANCE_SAMPLE)
         normalized (div (sub data mean) (sqrt var))]
     normalized)
   ```
   
   See also:
   - mean: Compute mean only
   - var: Compute variance only
   - mean-weighted: Weighted mean
   - var-weighted: Weighted variance"
  ([^AFArray in ^AFArray weights]
   (meanvar in weights defs/AF_VARIANCE_SAMPLE 0))
  ([^AFArray in ^AFArray weights bias]
   (meanvar in weights bias 0))
  ([^AFArray in ^AFArray weights bias dim]
   (let [mean-ptr (res/native-af-array-pointer)
         var-ptr (res/native-af-array-pointer)]
     (check! (variance/af-meanvar mean-ptr var-ptr (res/af-handle in) (res/af-handle weights) (int bias) (int dim))
                 "af-meanvar")
     {:mean (res/af-array-new (res/deref-af-array mean-ptr))
      :var (res/af-array-new (res/deref-af-array var-ptr))})))

;;;
;;; Covariance and Correlation
;;;

(defn cov
  "Compute covariance between two variables.
   
   Covariance measures how two variables vary together:
   Cov(X,Y) = (1/N) × Σ(xᵢ - μₓ)(yᵢ - μᵧ)
   
   - Positive: variables increase together
   - Negative: one increases as other decreases
   - Zero: no linear relationship
   
   Note: Magnitude depends on data scale. Use corrcoef for normalized measure.
   
   Parameters:
   - x: First variable (AFArray)
   - y: Second variable (AFArray, same shape as x)
   - bias: Variance bias type (default VARIANCE_SAMPLE)
   
   Returns:
   AFArray with covariance value
   
   Example:
   ```clojure
   ;; Positive covariance
   (let [x (array/from-vec [1.0 2.0 3.0 4.0] [4])
         y (array/from-vec [2.0 4.0 6.0 8.0] [4])  ;; y = 2x
         c (cov x y)]
     c)  ;; Positive, variables increase together
   
   ;; Negative covariance
   (let [x (array/from-vec [1.0 2.0 3.0 4.0] [4])
         y (array/from-vec [8.0 6.0 4.0 2.0] [4])  ;; inverse
         c (cov x y)]
     c)  ;; Negative
   ```
   
   See also:
   - corrcoef: Normalized correlation [-1, 1]
   - meanvar: Combined mean and variance"
  ([^AFArray x ^AFArray y]
   (cov x y defs/AF_VARIANCE_SAMPLE))
  ([^AFArray x ^AFArray y bias]
   (let [out (res/native-af-array-pointer)]
     (check! (covariance/af-cov-v2 out (res/af-handle x) (res/af-handle y) (int bias))
                 "af-cov-v2")
     (res/af-array-new (res/deref-af-array out)))))

(defn corrcoef
  "Compute correlation coefficient between two variables.
   
   Correlation is normalized covariance:
   ρ(X,Y) = Cov(X,Y) / (σₓ × σᵧ)
   
   Range: [-1, 1]
   - +1: Perfect positive correlation
   - -1: Perfect negative correlation
   - 0: No linear correlation
   
   Scale-independent, easier to interpret than covariance.
   
   Parameters:
   - x: First variable (AFArray)
   - y: Second variable (AFArray, same shape as x)
   
   Returns:
   - For real arrays: correlation as double
   - For complex arrays: [real imag] vector
   
   Example:
   ```clojure
   ;; Perfect positive correlation
   (let [x (array/from-vec [1.0 2.0 3.0 4.0] [4])
         y (array/from-vec [2.0 4.0 6.0 8.0] [4])  ;; y = 2x
         r (corrcoef x y)]
     r)  ;; => 1.0
   
   ;; Perfect negative correlation
   (let [x (array/from-vec [1.0 2.0 3.0 4.0] [4])
         y (array/from-vec [4.0 3.0 2.0 1.0] [4])
         r (corrcoef x y)]
     r)  ;; => -1.0
   
   ;; No correlation
   (let [x (array/random [100])
         y (array/random [100])  ;; independent
         r (corrcoef x y)]
     r)  ;; ≈ 0.0
   ```
   
   See also:
   - cov: Unnormalized covariance"
  [^AFArray x ^AFArray y]
  (let [real-buf (mem/alloc-instance ::mem/double)
        imag-buf (mem/alloc-instance ::mem/double)]
    (check! (corrcoef/af-corrcoef real-buf imag-buf (res/af-handle x) (res/af-handle y))
                "af-corrcoef")
    (let [real (mem/read-double real-buf)
          imag (mem/read-double imag-buf)]
      (if (zero? imag)
        real
        [real imag]))))

;;;
;;; Top-K Selection
;;;

(defn topk
  "Find the k largest or smallest values along a dimension.
   
   More efficient than full sorting (O(n log k) vs O(n log n)) when k ≪ n.
   Returns both values and their original indices.
   
   Parameters:
   - in: Input array (AFArray)
   - k: Number of elements to select (integer, must be ≤ array size)
   - dim: Dimension along which to select (default -1)
   - order: Selection order (TOPK_MAX, TOPK_MIN, TOPK_STABLE, default TOPK_MAX)
   
   Returns:
   Vector of [values indices]:
   - values: AFArray with k selected values (sorted)
   - indices: AFArray with original positions (u32)
   
   Example:
   ```clojure
   ;; Top 5 largest values
   (let [data (array/random [1000])
         [top-vals top-idx] (topk data 5 -1 TOPK_MAX)]
     {:values top-vals :indices top-idx})
   
   ;; Top-5 smallest
   (let [data (array/random [1000])
         [min-vals min-idx] (topk data 5 -1 TOPK_MIN)]
     min-vals)
   
   ;; Top-k predictions in ML classification
   (let [probs (softmax logits)
         [top5-probs top5-classes] (topk probs 5 0 TOPK_MAX)]
     {:probabilities top5-probs :classes top5-classes})
   
   ;; Per-row top-k
   (let [scores (array/random [100 10])  ;; 100 samples, 10 features
         [top3 idx3] (topk scores 3 1 TOPK_MAX)]  ;; Top 3 per row
     top3)  ;; Shape: [100 3]
   ```
   
   Performance:
   - O(n log k) complexity
   - Very efficient for k ≪ n
   - GPU accelerated
   - Best for k ≤ 256
   
   See also:
   - sort: Full sorting (slower for small k)
   - max/min: Single extreme value"
  ([^AFArray in k]
   (topk in k -1 defs/AF_TOPK_MAX))
  ([^AFArray in k dim]
   (topk in k dim defs/AF_TOPK_MAX))
  ([^AFArray in k dim order]
   (let [values-ptr (res/native-af-array-pointer)
         indices-ptr (res/native-af-array-pointer)]
     (check! (topk/af-topk values-ptr indices-ptr (res/af-handle in) (int k) (int dim) (int order))
                 "af-topk")
     [(res/af-array-new (res/deref-af-array values-ptr))
      (res/af-array-new (res/deref-af-array indices-ptr))])))

