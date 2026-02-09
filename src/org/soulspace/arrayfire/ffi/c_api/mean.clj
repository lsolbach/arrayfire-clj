(ns org.soulspace.arrayfire.ffi.c-api.mean
  "Bindings for the ArrayFire mean (arithmetic average) functions.
   
   Mean (arithmetic average) is a fundamental statistical measure that computes
   the central tendency of data. ArrayFire provides efficient GPU-accelerated
   mean computation along dimensions or across entire arrays, with optional
   weighted averaging.
   
   Categories of Mean Functions:
   
   1. **Dimensional Mean** (af-mean):
      - Compute mean along a specific dimension
      - Reduces that dimension to size 1
      - Preserves other dimensions
      - Used for batch statistics, time series analysis
   
   2. **Weighted Dimensional Mean** (af-mean-weighted):
      - Compute weighted arithmetic mean along dimension
      - Each value scaled by corresponding weight before averaging
      - Weights array must match or be broadcastable to input dimensions
      - Applications: weighted averaging, importance sampling
   
   3. **Global Mean** (af-mean-all):
      - Compute mean of all elements in array
      - Returns single scalar value (real and imaginary parts)
      - Reduces entire array to one number
      - Most common statistical operation
   
   4. **Global Weighted Mean** (af-mean-all-weighted):
      - Compute weighted mean of all elements
      - Returns scalar with real and imaginary parts
      - Weights must match input array dimensions
      - Used when elements have different importance
   
   Mathematical Foundation:
   
   **Arithmetic Mean**:
   For a dataset X = {x₁, x₂, ..., xₙ}:
   
   μ = (1/n) * Σ xᵢ
   
   where:
   - μ (mu) = arithmetic mean
   - n = number of elements
   - Σ = summation over all elements
   
   **Weighted Mean**:
   For dataset X with weights W = {w₁, w₂, ..., wₙ}:
   
   μ_w = (Σ wᵢ·xᵢ) / (Σ wᵢ)
   
   where:
   - μ_w = weighted mean
   - wᵢ = weight for element i
   - Denominator normalizes by sum of weights
   
   **Dimensional Mean**:
   For 2D array A[i,j], mean along dimension 0 (rows):
   
   μ[j] = (1/m) * Σᵢ A[i,j]
   
   Result is 1D array with one mean per column.
   
   **Complex Mean**:
   For complex numbers z = a + bi:
   
   μ = (1/n) * Σ (aᵢ + bᵢi) = (Σ aᵢ)/n + i(Σ bᵢ)/n
   
   Real and imaginary parts averaged separately.
   
   Properties of Arithmetic Mean:
   
   1. **Linearity**:
      - mean(a·X + b) = a·mean(X) + b
      - Scaling and shifting commute with mean
   
   2. **Sum Relationship**:
      - sum(X) = n * mean(X)
      - Mean is normalized sum
   
   3. **Minimizes Squared Error**:
      - μ minimizes Σ (xᵢ - c)²
      - Mean is least-squares optimal center
   
   4. **Sensitive to Outliers**:
      - Single extreme value affects mean significantly
      - Consider median or trimmed mean for robustness
   
   5. **Center of Mass**:
      - Weighted mean is center of mass
      - Balance point of distribution
   
   Type Conversions:
   
   Input Type → Output Type (unweighted):
   - f64, s64, u64 → f64 (double precision)
   - f32, s32, u32, s16, u16, s8, u8, b8, f16 → f32 (single precision)
   - c32 → c32 (complex single precision)
   - c64 → c64 (complex double precision)
   
   Output is floating point even for integer input to preserve fractional results.
   
   Dimensional Behavior:
   
   For 4D array with dims [W, H, D, B]:
   - dim=0: Mean along width → output dims [1, H, D, B]
   - dim=1: Mean along height → output dims [W, 1, D, B]
   - dim=2: Mean along depth → output dims [W, H, 1, B]
   - dim=3: Mean along batch → output dims [W, H, D, 1]
   
   The specified dimension collapses to size 1.
   
   Performance Considerations:
   
   **GPU Acceleration**:
   - Parallel reduction algorithms
   - 10-100× faster than CPU for large arrays
   - Optimized for different data types
   - Batch operations highly efficient
   
   **Algorithmic Complexity**:
   - Time: O(N) where N = total elements
   - Space: O(output_size) for dimensional mean
   - Single pass through data
   - Numerically stable summation
   
   **Memory Access**:
   - Coalesced reads on GPU
   - Efficient reduction trees
   - Minimizes global memory traffic
   
   **Numerical Stability**:
   - Uses compensated summation algorithms
   - Reduces floating-point error accumulation
   - Important for large datasets
   
   Applications:
   
   1. **Statistical Analysis**:
      - Central tendency measurement
      - Data normalization (subtract mean)
      - Feature scaling
      - Baseline estimation
   
   2. **Signal Processing**:
      - DC offset removal (subtract mean)
      - Signal averaging for noise reduction
      - Temporal mean for video stabilization
   
   3. **Image Processing**:
      - Mean pixel intensity
      - Channel-wise means for color normalization
      - Background estimation (temporal mean)
      - Mean filtering (convolution with uniform kernel)
   
   4. **Machine Learning**:
      - Feature standardization (zero mean)
      - Batch normalization (mean per batch)
      - Loss function computation
      - Model evaluation metrics
   
   5. **Financial Analysis**:
      - Average returns
      - Moving averages
      - Price indices
      - Risk metrics
   
   6. **Time Series**:
      - Trend estimation
      - Seasonal adjustment
      - Smoothing
      - Forecasting baselines
   
   7. **Physics Simulations**:
      - Ensemble averages
      - Center of mass calculations
      - Field averages
      - Statistical mechanics
   
   Common Patterns:
   
   **Zero-Mean Normalization**:
   ```clojure
   ;; Subtract mean to center data at zero
   (let [data (create-array values [1000 100])
         mean-vals (af-mean data 0)  ; Mean per feature
         centered (af-sub data mean-vals)]  ; Broadcast subtraction
     centered)
   ```
   
   **Batch Statistics**:
   ```clojure
   ;; Compute mean per sample in batch
   (let [batch (create-array data [28 28 1 64])  ; 64 images
         sample-means (af-mean batch 3)]  ; Mean along batch dimension
     sample-means)
   ```
   
   **Weighted Average**:
   ```clojure
   ;; Compute importance-weighted mean
   (let [values (create-array data [1000])
         weights (create-array importance [1000])
         weighted-mean (af-mean-all-weighted values weights)]
     weighted-mean)
   ```
   
   **Temporal Mean** (Video/Signals):
   ```clojure
   ;; Average over time dimension
   (let [video (create-array frames [640 480 3 300])  ; 300 frames
         temporal-mean (af-mean video 3)]  ; Background estimate
     temporal-mean)
   ```
   
   Comparison with Other Statistics:
   
   **Mean vs Median**:
   - Mean: Optimal for symmetric distributions, least-squares
   - Median: Robust to outliers, better for skewed data
   - Mean faster to compute
   - Median requires sorting
   
   **Mean vs Mode**:
   - Mean: Continuous measure, uses all data
   - Mode: Categorical/discrete, most frequent value
   - Mean more informative for numeric data
   
   **Unweighted vs Weighted Mean**:
   - Unweighted: All samples equal importance
   - Weighted: Variable importance per sample
   - Weighted mean for non-uniform sampling, reliability weighting
   
   Best Practices:
   
   1. **Check for Outliers**: Mean sensitive to extreme values
   2. **Consider Distribution**: Mean best for symmetric distributions
   3. **Numerical Precision**: Use f64 for high precision requirements
   4. **Memory Efficiency**: Dimensional mean keeps memory manageable
   5. **Normalization**: Subtract mean before computing variance/covariance
   
   Limitations:
   
   - **Outlier Sensitivity**: Single extreme value can skew mean
   - **Not Scale-Invariant**: Scaling data scales mean proportionally
   - **Assumes Linearity**: Inappropriate for multiplicative processes
   - **Loss of Information**: Single number can't capture distribution shape
   
   Error Handling:
   
   Common errors:
   - Invalid dimension (must be 0-3)
   - Weight dimensions don't match input (must be same or broadcastable)
   - Empty arrays (must have at least one element)
   - Memory allocation failures
   
   See also:
   - af-var: Variance (spread around mean)
   - af-stdev: Standard deviation (square root of variance)
   - af-median: Median (robust central tendency)
   - af-sum: Summation (mean = sum / count)
   - af-meanvar: Compute mean and variance simultaneously (more efficient)"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; Mean functions

;; af_err af_mean(af_array *out, const af_array in, const dim_t dim)
(defcfn af-mean
  "Compute mean (arithmetic average) along a dimension.
   
   Computes the arithmetic mean of array elements along the specified
   dimension. The output array has the same number of dimensions as the
   input, but the specified dimension is reduced to size 1.
   
   Parameters:
   - out: out pointer for result array
   - in: input array
   - dim: dimension along which to compute mean (0-3)
     * 0: Mean along first dimension (columns for 2D)
     * 1: Mean along second dimension (rows for 2D)
     * 2: Mean along third dimension
     * 3: Mean along fourth dimension
   
   Mathematical Operation:
   For input array A with n elements along dimension d:
   
   out[...] = (1/n) * Σᵢ A[..., i, ...]
   
   where the sum is over index i in dimension d.
   
   Type Conversions:
   Input → Output type:
   - f64, s64, u64 → f64 (double precision floating point)
   - f32, s32, u32, s16, u16, s8, u8, b8, f16 → f32 (single precision)
   - c32 → c32 (complex single precision)
   - c64 → c64 (complex double precision)
   
   Integer inputs converted to floating point to preserve fractional results.
   
   Dimension Behavior:
   For 3D array with dimensions [10, 20, 30]:
   - dim=0: Output dims [1, 20, 30] (10 values averaged per position)
   - dim=1: Output dims [10, 1, 30] (20 values averaged per position)
   - dim=2: Output dims [10, 20, 1] (30 values averaged per position)
   
   Performance:
   - Complexity: O(N) where N = total elements
   - GPU parallelizes across all output positions
   - Memory: Single pass through data
   - Efficient reduction trees on GPU
   
   Example (Column Means):
   ```clojure
   ;; Compute mean of each column in matrix
   (let [matrix (create-array data [100 50])  ; 100 rows, 50 columns
         out-ptr (mem/alloc-pointer ::mem/pointer)
         status (af-mean out-ptr matrix 0)]  ; Mean along dim 0 (rows)
     ;; Result: [1 50] array with mean of each column
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (Row Means):
   ```clojure
   ;; Compute mean of each row in matrix
   (let [matrix (create-array data [100 50])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         status (af-mean out-ptr matrix 1)]  ; Mean along dim 1 (columns)
     ;; Result: [100 1] array with mean of each row
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (Batch Mean Images):
   ```clojure
   ;; Compute mean image across batch
   (let [batch (create-array images [256 256 3 32])  ; 32 RGB images
         out-ptr (mem/alloc-pointer ::mem/pointer)
         status (af-mean out-ptr batch 3)]  ; Mean along batch dimension
     ;; Result: [256 256 3 1] - average image
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (Feature Normalization):
   ```clojure
   ;; Zero-center features by subtracting column means
   (let [features (create-array data [1000 50])  ; 1000 samples, 50 features
         mean-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-mean mean-ptr features 0)  ; Mean per feature
         mean-arr (mem/read-pointer mean-ptr ::mem/pointer)
         centered-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-sub centered-ptr features mean-arr)]  ; Subtract (broadcasts)
     (mem/read-pointer centered-ptr ::mem/pointer))
   ```
   
   Example (Temporal Averaging):
   ```clojure
   ;; Average signal over time windows
   (let [signal (create-array data [10000 100])  ; 100 channels, 10k time points
         out-ptr (mem/alloc-pointer ::mem/pointer)
         status (af-mean out-ptr signal 0)]  ; Mean across time
     ;; Result: [1 100] - average value per channel
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (3D Volume Mean):
   ```clojure
   ;; Average 3D volume slices
   (let [volume (create-array data [128 128 64])  ; 64 slices
         out-ptr (mem/alloc-pointer ::mem/pointer)
         status (af-mean out-ptr volume 2)]  ; Mean along depth
     ;; Result: [128 128 1] - average 2D slice
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (Complex Number Mean):
   ```clojure
   ;; Average complex FFT coefficients
   (let [fft-result (create-array complex-data [512 512] :c32)
         out-ptr (mem/alloc-pointer ::mem/pointer)
         status (af-mean out-ptr fft-result 0)]
     ;; Result: [1 512] complex array with mean per frequency bin
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Applications:
   - Statistical analysis: Central tendency per feature/variable
   - Batch processing: Average over samples
   - Signal processing: DC offset estimation, averaging for SNR
   - Image processing: Mean pixel values, channel statistics
   - Machine learning: Feature normalization, batch normalization
   
   Notes:
   - Dimension parameter must be in range [0, 3]
   - Output retains all dimensions; specified dim becomes size 1
   - For mean of entire array, use af-mean-all instead
   - Mean can be broadcast back for operations like centering
   - GPU implementation uses parallel reduction algorithms
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-mean-all: Mean of all elements (returns scalar)
   - af-mean-weighted: Weighted mean along dimension
   - af-sum: Sum along dimension (mean = sum / count)
   - af-var: Variance along dimension"
  "af_mean" [::mem/pointer ::mem/pointer ::mem/long] ::mem/int)

;; af_err af_mean_weighted(af_array *out, const af_array in, const af_array weights, const dim_t dim)
(defcfn af-mean-weighted
  "Compute weighted mean along a dimension.
   
   Computes the weighted arithmetic mean where each element is multiplied
   by its corresponding weight before averaging. The weights array must
   match the input dimensions or be broadcastable to the input shape.
   
   Parameters:
   - out: out pointer for result array
   - in: input array (values to average)
   - weights: weight array (must be real f32 or f64)
   - dim: dimension along which to compute weighted mean (0-3)
   
   Mathematical Operation:
   For input array X with weights W along dimension d:
   
   out[...] = (Σᵢ wᵢ·xᵢ) / (Σᵢ wᵢ)
   
   where:
   - xᵢ = elements along dimension d
   - wᵢ = corresponding weights
   - Denominator normalizes by sum of weights
   
   Weight Requirements:
   - Weights must be real floating point (f32 or f64)
   - Weights array dimensions must match input OR be broadcastable
   - Broadcasting rules: dimension can be 1 if input dimension > 1
   - Negative weights are allowed (though unusual for means)
   
   Type Conversions:
   Input → Output:
   - f64, s64, u64 → f64
   - f32, s32, u32, s16, u16, s8, u8, b8, f16 → f32
   - c32 → c32 (weights applied to both real and imaginary parts)
   - c64 → c64
   
   Weights are always real; complex inputs weighted uniformly.
   
   Broadcasting Examples:
   
   1. **Same Shape** (elementwise weighting):
      Input: [100, 50], Weights: [100, 50]
      Each element has unique weight
   
   2. **Column Weights** (weight per column):
      Input: [100, 50], Weights: [1, 50]
      All rows in a column share same weight
   
   3. **Row Weights** (weight per row):
      Input: [100, 50], Weights: [100, 1]
      All columns in a row share same weight
   
   4. **Global Weights** (applied everywhere):
      Input: [100, 50], Weights: [1, 1]
      Uniform weighting (equivalent to unweighted mean)
   
   Performance:
   - Complexity: O(N) where N = total elements
   - Requires two passes: weight sum, then weighted sum
   - GPU parallelizes both passes efficiently
   - Memory: broadcasts weights as needed
   
   Example (Importance Weighting):
   ```clojure
   ;; Weight samples by reliability/confidence
   (let [measurements (create-array data [1000 10])  ; 1000 samples, 10 sensors
         confidence (create-array weights [1000 1])  ; Confidence per sample
         out-ptr (mem/alloc-pointer ::mem/pointer)
         status (af-mean-weighted out-ptr measurements confidence 0)]
     ;; Result: [1 10] weighted mean per sensor
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (Time-Weighted Average):
   ```clojure
   ;; Weight recent observations more heavily
   (let [time-series (create-array data [1000 50])  ; 1000 time points
         decay-weights (create-array (exponential-decay) [1000 1])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         status (af-mean-weighted out-ptr time-series decay-weights 0)]
     ;; Result: [1 50] exponentially weighted mean
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (Frequency Weighting):
   ```clojure
   ;; Weight values by their frequency of occurrence
   (let [values (create-array data [100 200])
         frequencies (create-array counts [100 1])  ; Frequency per row
         out-ptr (mem/alloc-pointer ::mem/pointer)
         status (af-mean-weighted out-ptr values frequencies 0)]
     ;; Result: [1 200] frequency-weighted mean
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (Stratified Sampling):
   ```clojure
   ;; Combine strata with population-proportional weights
   (let [strata-samples (create-array data [500 100])  ; 500 samples per stratum
         strata-weights (create-array proportions [500 1])  ; Population proportion
         out-ptr (mem/alloc-pointer ::mem/pointer)
         status (af-mean-weighted out-ptr strata-samples strata-weights 0)]
     ;; Result: [1 100] population-weighted estimate
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (Quality-Weighted Image Averaging):
   ```clojure
   ;; Average images with quality weights
   (let [images (create-array data [512 512 3 20])  ; 20 images
         quality (create-array scores [1 1 1 20])  ; Quality per image
         out-ptr (mem/alloc-pointer ::mem/pointer)
         status (af-mean-weighted out-ptr images quality 3)]
     ;; Result: [512 512 3 1] quality-weighted average image
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (Inverse Variance Weighting):
   ```clojure
   ;; Optimal weights proportional to inverse variance
   (let [measurements (create-array data [100 10])
         variances (create-array vars [100 1])
         inv-var-weights (af-div (constant 1.0) variances)
         out-ptr (mem/alloc-pointer ::mem/pointer)
         status (af-mean-weighted out-ptr measurements inv-var-weights 0)]
     ;; Result: [1 10] minimum variance estimate
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (Kernel Density Estimation):
   ```clojure
   ;; Compute weighted average with kernel weights
   (let [samples (create-array data [1000 2])  ; 2D points
         kernel-weights (create-array kernels [1000 1])  ; Distance-based
         out-ptr (mem/alloc-pointer ::mem/pointer)
         status (af-mean-weighted out-ptr samples kernel-weights 0)]
     ;; Result: [1 2] kernel-weighted center
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Applications:
   - Survey data: Population-weighted estimates
   - Time series: Exponential smoothing, recency weighting
   - Statistics: Inverse variance weighting for efficiency
   - Machine learning: Sample weighting for class imbalance
   - Finance: Value-weighted returns, portfolio averaging
   - Signal processing: Weighted averaging for noise reduction
   - Sensor fusion: Confidence-weighted multi-sensor integration
   
   Common Weight Patterns:
   - **Uniform**: All weights equal 1 (equivalent to unweighted mean)
   - **Frequency**: Weight by count/frequency
   - **Reliability**: Weight by measurement quality/confidence
   - **Exponential Decay**: exp(-λt) for time series
   - **Inverse Variance**: 1/σ² for optimal statistical efficiency
   - **Distance**: Kernel-based weights for local averaging
   
   Notes:
   - Weights must be real floating point (f32 or f64)
   - Input can be any numeric type including complex
   - Zero weights effectively exclude those elements
   - Sum of weights must be non-zero (divide by zero otherwise)
   - If weights sum to 1, output is direct weighted average
   - Broadcasting allows efficient memory use
   - GPU automatically tiles weights as needed
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-mean: Unweighted mean along dimension
   - af-mean-all-weighted: Weighted mean of all elements
   - af-tile: Manually tile weights to match input shape"
  "af_mean_weighted" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/long] ::mem/int)

;; af_err af_mean_all(double *real, double *imag, const af_array in)
(defcfn af-mean-all
  "Compute mean of all elements in an array (global mean).
   
   Computes the arithmetic mean of all elements in the array, reducing
   the entire array to a single scalar value. For complex arrays, returns
   both real and imaginary parts of the mean.
   
   Parameters:
   - real: out pointer to double for real part (or entire value for real arrays)
   - imag: out pointer to double for imaginary part (0 for real arrays)
   - in: input array
   
   Mathematical Operation:
   For array A with N total elements:
   
   mean = (1/N) * Σ A[i]
   
   where the sum is over all N elements.
   
   For complex arrays:
   mean = (Σ real(A[i]))/N + i*(Σ imag(A[i]))/N
   
   Type Handling:
   - All input types converted to double precision for output
   - Real arrays: real parameter receives result, imag receives 0
   - Complex arrays: both real and imag receive their respective means
   - Integer types promoted to double to preserve precision
   
   Output Precision:
   - Always double precision (f64) regardless of input type
   - Ensures maximum precision for final result
   - Important for large arrays or small values
   
   Performance:
   - Complexity: O(N) where N = total elements
   - Single global reduction
   - Parallel tree reduction on GPU
   - Result transferred to host memory
   - Very fast even for large arrays (millions of elements)
   
   Example (Simple Mean):
   ```clojure
   ;; Compute mean of entire array
   (let [data (create-array values [1000 1000])  ; 1M elements
         real-ptr (mem/alloc-pointer ::mem/double)
         imag-ptr (mem/alloc-pointer ::mem/double)
         status (af-mean-all real-ptr imag-ptr data)
         mean-value (mem/read-pointer real-ptr ::mem/double)]
     mean-value)
   ```
   
   Example (Image Mean Intensity):
   ```clojure
   ;; Average pixel value across entire image
   (let [image (create-array pixels [1920 1080 3])  ; RGB image
         real-ptr (mem/alloc-pointer ::mem/double)
         imag-ptr (mem/alloc-pointer ::mem/double)
         _ (af-mean-all real-ptr imag-ptr image)
         mean-intensity (mem/read-pointer real-ptr ::mem/double)]
     ;; Result: average intensity across all pixels and channels
     mean-intensity)
   ```
   
   Example (Complex FFT Mean):
   ```clojure
   ;; Mean of complex FFT coefficients
   (let [fft-data (create-array complex-vals [512 512] :c32)
         real-ptr (mem/alloc-pointer ::mem/double)
         imag-ptr (mem/alloc-pointer ::mem/double)
         _ (af-mean-all real-ptr imag-ptr fft-data)
         mean-real (mem/read-pointer real-ptr ::mem/double)
         mean-imag (mem/read-pointer imag-ptr ::mem/double)]
     {:real mean-real :imag mean-imag})
   ```
   
   Example (Data Quality Check):
   ```clojure
   ;; Check if data is reasonable by examining mean
   (let [measurements (create-array data [10000])
         real-ptr (mem/alloc-pointer ::mem/double)
         imag-ptr (mem/alloc-pointer ::mem/double)
         _ (af-mean-all real-ptr imag-ptr measurements)
         mean-val (mem/read-pointer real-ptr ::mem/double)]
     (if (< (abs mean-val) 1e-10)
       :data-looks-zero-centered
       :data-has-dc-offset))
   ```
   
   Example (Comparing Datasets):
   ```clojure
   ;; Compare means of two datasets
   (let [data1 (create-array values1 [5000])
         data2 (create-array values2 [5000])
         real1 (mem/alloc-pointer ::mem/double)
         imag1 (mem/alloc-pointer ::mem/double)
         real2 (mem/alloc-pointer ::mem/double)
         imag2 (mem/alloc-pointer ::mem/double)
         _ (af-mean-all real1 imag1 data1)
         _ (af-mean-all real2 imag2 data2)
         mean1 (mem/read-pointer real1 ::mem/double)
         mean2 (mem/read-pointer real2 ::mem/double)]
     (- mean1 mean2))  ; Difference in means
   ```
   
   Example (Normalization Factor):
   ```clojure
   ;; Compute normalization factor for zero-centering
   (let [features (create-array data [1000 100])
         real-ptr (mem/alloc-pointer ::mem/double)
         imag-ptr (mem/alloc-pointer ::mem/double)
         _ (af-mean-all real-ptr imag-ptr features)
         global-mean (mem/read-pointer real-ptr ::mem/double)
         normalized (af-sub features (constant global-mean (dims features)))]
     normalized)
   ```
   
   Example (Sanity Check):
   ```clojure
   ;; Verify mean is in expected range
   (let [probabilities (create-array probs [1000])  ; Should be in [0, 1]
         real-ptr (mem/alloc-pointer ::mem/double)
         imag-ptr (mem/alloc-pointer ::mem/double)
         _ (af-mean-all real-ptr imag-ptr probabilities)
         mean-prob (mem/read-pointer real-ptr ::mem/double)]
     (assert (and (>= mean-prob 0.0) (<= mean-prob 1.0))
             \"Mean probability out of valid range!\"))
   ```
   
   Applications:
   - Statistical summary: Single number describing central tendency
   - Data validation: Check if values are in expected range
   - Normalization: Global offset for zero-centering
   - Quality metrics: Average quality/error across all samples
   - Signal processing: DC component estimation
   - Image processing: Average brightness/intensity
   - Sanity checks: Verify data loaded correctly
   
   Notes:
   - Most common statistical operation
   - For real arrays, imag parameter receives 0.0
   - Result always double precision for maximum accuracy
   - Very efficient: single parallel reduction
   - For dimensional mean, use af-mean instead
   - Mean can be broadcast to array size for centering operations
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-mean: Mean along specific dimension
   - af-mean-all-weighted: Weighted mean of all elements
   - af-sum: Sum of all elements (mean = sum / count)
   - af-median-all: Median of all elements (robust alternative)"
  "af_mean_all" [::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_mean_all_weighted(double *real, double *imag, const af_array in, const af_array weights)
(defcfn af-mean-all-weighted
  "Compute weighted mean of all elements in an array.
   
   Computes the weighted arithmetic mean across all elements, where each
   element is multiplied by its corresponding weight. Returns a single
   scalar value (with real and imaginary parts for complex arrays).
   
   Parameters:
   - real: out pointer to double for real part of weighted mean
   - imag: out pointer to double for imaginary part (0 for real arrays)
   - in: input array (values to average)
   - weights: weight array (must be real f32 or f64, same dimensions as in)
   
   Mathematical Operation:
   For array X with weights W, both having N elements:
   
   weighted_mean = (Σ wᵢ·xᵢ) / (Σ wᵢ)
   
   where:
   - xᵢ = element i of input array
   - wᵢ = corresponding weight
   - Numerator: sum of weighted values
   - Denominator: sum of weights (normalization)
   
   For complex arrays:
   weighted_mean = (Σ wᵢ·real(xᵢ))/Σwᵢ + i*(Σ wᵢ·imag(xᵢ))/Σwᵢ
   
   Weight Requirements:
   - Weights must be real floating point (f32 or f64)
   - Weights array must match input array dimensions exactly
   - No broadcasting allowed (unlike af-mean-weighted)
   - Negative weights allowed but unusual
   - Zero weights exclude those elements
   - Sum of weights must be non-zero
   
   Type Handling:
   - Input: Any numeric type (including complex)
   - Weights: Must be f32 or f64 (real only)
   - Output: Always double precision (f64)
   - Complex inputs: weights apply uniformly to real and imaginary parts
   
   Performance:
   - Complexity: O(N) where N = total elements
   - Two parallel reductions: weighted sum, weight sum
   - GPU parallelizes both passes
   - Result transferred to host
   - Efficient for arrays of any size
   
   Example (Global Weighted Average):
   ```clojure
   ;; Compute overall weighted mean
   (let [values (create-array data [1000])
         weights (create-array importance [1000])
         real-ptr (mem/alloc-pointer ::mem/double)
         imag-ptr (mem/alloc-pointer ::mem/double)
         status (af-mean-all-weighted real-ptr imag-ptr values weights)
         weighted-mean (mem/read-pointer real-ptr ::mem/double)]
     weighted-mean)
   ```
   
   Example (Confidence-Weighted Estimate):
   ```clojure
   ;; Weight measurements by confidence
   (let [measurements (create-array data [500 10])  ; 5000 total measurements
         confidence (create-array conf [500 10])  ; Confidence per measurement
         real-ptr (mem/alloc-pointer ::mem/double)
         imag-ptr (mem/alloc-pointer ::mem/double)
         _ (af-mean-all-weighted real-ptr imag-ptr measurements confidence)
         best-estimate (mem/read-pointer real-ptr ::mem/double)]
     best-estimate)
   ```
   
   Example (Frequency-Weighted Mean):
   ```clojure
   ;; Compute mean considering frequency of each value
   (let [unique-values (create-array vals [100])
         frequencies (create-array counts [100])
         real-ptr (mem/alloc-pointer ::mem/double)
         imag-ptr (mem/alloc-pointer ::mem/double)
         _ (af-mean-all-weighted real-ptr imag-ptr unique-values frequencies)
         overall-mean (mem/read-pointer real-ptr ::mem/double)]
     ;; Equivalent to mean of full dataset with repetitions
     overall-mean)
   ```
   
   Example (Portfolio Return):
   ```clojure
   ;; Compute value-weighted portfolio return
   (let [returns (create-array asset-returns [100])  ; Return per asset
         values (create-array asset-values [100])  ; Value per asset
         real-ptr (mem/alloc-pointer ::mem/double)
         imag-ptr (mem/alloc-pointer ::mem/double)
         _ (af-mean-all-weighted real-ptr imag-ptr returns values)
         portfolio-return (mem/read-pointer real-ptr ::mem/double)]
     portfolio-return)
   ```
   
   Example (Spatial Weighted Mean):
   ```clojure
   ;; Compute center of mass (weighted mean of positions)
   (let [mass-distribution (create-array masses [100 100])  ; 2D mass distribution
         x-coords (create-array x-positions [100 100])
         real-ptr (mem/alloc-pointer ::mem/double)
         imag-ptr (mem/alloc-pointer ::mem/double)
         _ (af-mean-all-weighted real-ptr imag-ptr x-coords mass-distribution)
         center-x (mem/read-pointer real-ptr ::mem/double)]
     ;; Similar for y-coordinate to get full center of mass
     center-x)
   ```
   
   Example (Time-Decayed Mean):
   ```clojure
   ;; Exponentially weighted mean emphasizing recent values
   (let [time-series (create-array data [1000])
         decay-weights (create-array (map #(exp (* -0.01 %)) (range 1000)) [1000])
         real-ptr (mem/alloc-pointer ::mem/double)
         imag-ptr (mem/alloc-pointer ::mem/double)
         _ (af-mean-all-weighted real-ptr imag-ptr time-series decay-weights)
         ewma (mem/read-pointer real-ptr ::mem/double)]
     ewma)
   ```
   
   Example (Quality-Weighted Image Metric):
   ```clojure
   ;; Compute quality-weighted average pixel value
   (let [image-pixels (create-array pixels [1920 1080 3])
         pixel-quality (create-array quality [1920 1080 3])  ; Quality per pixel
         real-ptr (mem/alloc-pointer ::mem/double)
         imag-ptr (mem/alloc-pointer ::mem/double)
         _ (af-mean-all-weighted real-ptr imag-ptr image-pixels pixel-quality)
         quality-weighted-intensity (mem/read-pointer real-ptr ::mem/double)]
     quality-weighted-intensity)
   ```
   
   Applications:
   - Statistics: Inverse variance weighting for optimal estimates
   - Finance: Value-weighted returns, portfolio metrics
   - Survey data: Population-weighted or sampling-weighted estimates
   - Physics: Center of mass, weighted averages of measurements
   - Machine learning: Sample-weighted loss, class imbalance correction
   - Signal processing: Confidence-weighted sensor fusion
   - Quality control: Reliability-weighted product metrics
   
   Common Weighting Schemes:
   - **Frequency Weights**: Count/frequency of each unique value
   - **Reliability Weights**: Measurement precision or confidence
   - **Value Weights**: Size/importance (e.g., market cap in finance)
   - **Inverse Variance**: 1/σ² for statistically optimal weighting
   - **Exponential Decay**: exp(-λt) for time series
   - **Distance Weights**: Kernel-based spatial weighting
   - **Quality Weights**: Quality score per measurement
   
   Notes:
   - Weights must exactly match input dimensions (no broadcasting)
   - Weights must be real floating point (f32 or f64)
   - For dimensional weighted mean, use af-mean-weighted
   - Sum of weights must be non-zero to avoid division by zero
   - Result always double precision regardless of input types
   - Zero weights effectively exclude corresponding elements
   - For uniform weights, equivalent to af-mean-all
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-mean-all: Unweighted mean of all elements
   - af-mean-weighted: Weighted mean along dimension
   - af-sum: Sum of all elements"
  "af_mean_all_weighted" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)
