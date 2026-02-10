(ns org.soulspace.arrayfire.ffi.c-api.median
  "Bindings for the ArrayFire median functions (src/api/c/median.cpp).
   
   Median is a robust statistical measure that represents the middle value
   in a sorted dataset. Unlike mean, median is resistant to outliers, making
   it valuable for data with extreme values or non-normal distributions.
   
   # What is Median?
   
   The median is the value separating the higher half from the lower half
   of a data sample:
   - For odd N: middle element after sorting
   - For even N: average of two middle elements after sorting
   
   Mathematically:
   - Odd N: median = x[(N+1)/2]
   - Even N: median = (x[N/2] + x[N/2+1]) / 2
   
   where x is sorted in ascending order.
   
   # Visual Example (1D)
   
   ```
   Odd count (N=7):
   Data: [12, 3, 7, 19, 2, 15, 8]
   Sorted: [2, 3, 7, 8, 12, 15, 19]
                      ^-- median = 8 (middle element)
   
   Even count (N=6):
   Data: [5, 2, 8, 1, 9, 3]
   Sorted: [1, 2, 3, 5, 8, 9]
                  ^ ^-- median = (3 + 5) / 2 = 4
   ```
   
   # 2D Example (Column-wise median)
   
   ```
   Matrix (3x4):           Median along dim=0 (columns):
   [5  2  8  1]            [(4), (3), (6), (2)]
   [4  7  6  2]             ^    ^    ^    ^
   [3  3  4  5]             |    |    |    |
                            |    |    |    middle values
   
   Column 0: [5,4,3] → sorted [3,4,5] → median = 4
   Column 1: [2,7,3] → sorted [2,3,7] → median = 3
   Column 2: [8,6,4] → sorted [4,6,8] → median = 6
   Column 3: [1,2,5] → sorted [1,2,5] → median = 2
   ```
   
   # Algorithm
   
   ArrayFire computes median using the following approach:
   
   1. **Sort the data**: Uses efficient GPU sorting along specified dimension
   2. **Find middle position(s)**:
      - Odd length: mid = (N+1)/2
      - Even length: mid = N/2 and mid+1
   3. **Extract median value(s)**:
      - Odd: Select element at mid position
      - Even: Average of two middle elements
   
   Implementation details:
   - Shortcut for N=1: Returns the single element
   - Shortcut for N=2: Returns average of both elements
   - Uses af_index to extract middle element(s) from sorted array
   - Automatically casts integer results to float for consistency
   
   # Performance Characteristics
   
   **Complexity**:
   - Time: O(N log N) per dimension due to sorting
     * Sorting: O(N log N)
     * Indexing: O(1)
     * Arithmetic: O(1)
   - Space: O(N) for sorted copy
   
   **Comparison with Mean**:
   - Mean: O(N) - single pass summation
   - Median: O(N log N) - requires sorting
   - Median is ~10-100× slower than mean
   - Use mean when outliers are not a concern
   - Use median for robust statistics
   
   **GPU Acceleration**:
   - Sorting is highly parallel on GPU
   - Speedup: 10-100× vs CPU for large arrays
   - Most efficient for large datasets (N > 10,000)
   
   # Type Handling
   
   **Input Types Supported**:
   All numeric types: f32, f64, s32, u32, s16, u16, s8, u8, s64, u64
   
   **Output Type Rules**:
   - Dimensional reduction (af-median):
     * Odd length: Returns input type (or f32 for integers)
     * Even length: Returns f32 or f64 (average requires floating-point)
     * Integer inputs → f32 output (to represent average accurately)
     * Float inputs → preserved (f32 → f32, f64 → f64)
   - Global reduction (af-median-all):
     * Always returns double (for precision)
   
   **Complex Numbers**:
   - Not directly supported (no natural ordering)
   - Workarounds:
     * Compute median of magnitudes: median(abs(z))
     * Separate real/imaginary: median(real(z)) + i*median(imag(z))
     * Compute median of angles: median(arg(z))
   
   # When to Use Median vs Mean
   
   **Use Median When**:
   - Data contains outliers or extreme values
   - Distribution is skewed (not normal)
   - Robust estimate needed (resistant to contamination)
   - Typical applications:
     * Income/wealth statistics (highly skewed)
     * Sensor data with occasional spikes
     * Medical measurements with artifacts
     * Real estate prices (outliers common)
     * Image processing (impulse noise)
   
   **Use Mean When**:
   - Data is normally distributed
   - Outliers are rare or non-existent
   - Need optimal statistical efficiency
   - Speed is critical (mean is much faster)
   - Typical applications:
     * Scientific measurements (controlled conditions)
     * Signal averaging (random noise)
     * Batch normalization (ML/DL)
     * Financial averages (no extreme outliers)
   
   **Comparison Table**:
   ```
   Property         | Mean           | Median
   -----------------|----------------|------------------
   Outlier resistance| Low           | High (robust)
   Computation      | O(N)           | O(N log N)
   Statistical efficiency| Optimal   | ~64% (normal dist)
   Interpretation   | Average        | Middle value
   Stability        | Varies greatly | Stable
   ```
   
   # Examples in Different Domains
   
   ## Statistics
   ```clojure
   ;; Income distribution (highly skewed)
   (let [incomes [30k 32k 35k 28k 40k 500k] ; one outlier
         mean-income (mean incomes)    ; ~$111k (misleading!)
         median-income (median incomes)] ; ~$33k (representative)
     ;; Median better represents \"typical\" income
   )
   ```
   
   ## Signal Processing
   ```clojure
   ;; Sensor data with occasional spikes
   (let [sensor-data [2.1 2.0 2.2 50.0 2.1 2.0] ; spike at index 3
         mean-val (mean sensor-data)      ; ~10.1 (contaminated)
         median-val (median sensor-data)] ; 2.1 (robust)
     ;; Median filters out the spike
   )
   ```
   
   ## Image Processing
   ```clojure
   ;; Remove salt-and-pepper noise (use medfilt for this)
   ;; But median of pixel intensities can find \"middle\" brightness
   (let [image-patch [0 0 128 255 255] ; mixed dark/bright/noise
         mean-bright (mean image-patch)     ; 128 (average)
         median-bright (median image-patch)] ; 128 (middle value)
     ;; In this case both are same, but with more outliers median is better
   )
   ```
   
   ## Machine Learning
   ```clojure
   ;; Robust scaling/normalization
   (let [features [array of measurements]
         median-val (median features 0)    ; per-feature median
         mad (median (abs (sub features median-val)))] ; median absolute deviation
     ;; More robust than mean/std for outlier-contaminated data
     (div (sub features median-val) mad)) ; robust normalization
   ```
   
   # Median vs Other Robust Statistics
   
   **Median Absolute Deviation (MAD)**:
   ```
   MAD = median(|x - median(x)|)
   ```
   - Robust alternative to standard deviation
   - Measures spread around median
   - Use median + MAD for robust statistics
   
   **Trimmed Mean**:
   - Mean after removing extreme values (e.g., top/bottom 10%)
   - Compromise between mean and median
   - Not directly available, must implement
   
   **Quantiles/Percentiles**:
   - Median is the 50th percentile
   - af_median computes exactly this
   - Use for quartiles, deciles, etc.
   
   # Common Patterns
   
   ## Outlier Detection
   ```clojure
   (let [data (create-array values dims)
         med (median data)
         mad (median (abs (sub data med)))
         threshold (* 3 mad)  ; 3-MAD rule
         outliers (gt (abs (sub data med)) threshold)]
     ;; outliers is boolean mask of outlier positions
   )
   ```
   
   ## Robust Baseline Estimation
   ```clojure
   (let [time-series (create-array signal [N])
         baseline (median time-series)] ; robust center
     ;; Remove baseline
     (sub time-series baseline))
   ```
   
   ## Quantile Estimation
   ```clojure
   ;; Median is 50th percentile (Q2)
   ;; For other quantiles, need to implement or use specialized functions
   (let [data (sort data)  ; ascending order
         q1-idx (/ (count data) 4)   ; 25th percentile position
         q2-idx (/ (count data) 2)   ; 50th percentile (median)
         q3-idx (* 3 (/ (count data) 4))] ; 75th percentile position
     ;; Use af-index to extract specific quantiles
   )
   ```
   
   # Best Practices
   
   1. **Choose dimension carefully**:
      - dim=-1: First non-singleton dimension (often columns)
      - dim=0: Reduce along rows (column-wise median)
      - dim=1: Reduce along columns (row-wise median)
   
   2. **Consider computational cost**:
      - Median is O(N log N) vs mean's O(N)
      - For large arrays, consider sampling if approximate median sufficient
      - Use mean when outliers are not expected
   
   3. **Handle even vs odd counts**:
      - Median automatically handles both cases
      - Even-count median is average of two middle values
      - Result may not be an actual data value
   
   4. **Robustness trade-offs**:
      - Median ignores ~50% of data distribution
      - Statistical efficiency is only ~64% of mean (for normal distributions)
      - Use only when robustness outweighs efficiency loss
   
   5. **Combine with other statistics**:
      - Report both mean and median to show skewness
      - Use MAD with median for robust spread
      - Compare mean-median difference to detect outliers
   
   6. **Type awareness**:
      - Integer inputs converted to float for even-count median
      - Use f32/f64 inputs if maintaining type is important
      - af-median-all always returns double
   
   # Limitations
   
   - **No complex number support**: Natural ordering doesn't exist
   - **No weighted median**: Unlike af_mean_weighted, no weight support
   - **Single dimension**: Can't compute multi-dimensional median directly
   - **Slower than mean**: O(N log N) vs O(N) complexity
   - **No incremental update**: Must recompute for new data
   
   # Applications by Domain
   
   ## Medical Imaging
   - Baseline intensity estimation (robust to artifacts)
   - Tissue characterization (resist instrument noise)
   - Preprocessing for segmentation
   
   ## Finance
   - Median income/wealth (highly skewed distributions)
   - Robust portfolio metrics
   - Outlier-resistant risk measures
   
   ## Computer Vision
   - Robust background estimation
   - Feature descriptor statistics
   - Illumination normalization
   
   ## Sensor Networks
   - Consensus among sensors (Byzantine fault tolerance)
   - Robust aggregation (resist faulty sensors)
   - Baseline drift correction
   
   ## Scientific Computing
   - Robust parameter estimation
   - Outlier-resistant averages
   - Quantile regression
   
   See also:
   - af-mean: Arithmetic mean (faster but not robust)
   - af-medfilt, af-medfilt1, af-medfilt2: Median filtering (spatial filters)
   - Quantile functions (for other percentiles)"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.loader]))

;; Median statistics functions

;; af_err af_median(af_array *out, const af_array in, const dim_t dim)
(defcfn af-median
  "Compute median along a specified dimension.
   
   The median is the middle value in a sorted dataset, providing a robust
   measure of central tendency that is resistant to outliers.
   
   Parameters:
   - out: out pointer for result array
   - in: input array to compute median over
   - dim: dimension along which to compute median (0-3, or -1 for first non-singleton)
   
   Algorithm:
   For each position along the specified dimension:
   1. Sort values along that dimension
   2. If odd count: Select middle element
   3. If even count: Average the two middle elements
   
   Dimensional Behavior:
   - Reduces the specified dimension to size 1
   - Other dimensions remain unchanged
   - Example: [100, 200, 50] along dim=1 → [100, 1, 50]
   
   Type Conversion:
   - Integer inputs with odd count → may stay integer or convert to f32
   - Integer inputs with even count → f32 (average of two integers)
   - f32 inputs → f32 output
   - f64 inputs → f64 output
   - For consistency, integer types typically convert to float
   
   Performance:
   - Complexity: O(N log N) per independent reduction (due to sorting)
   - GPU parallel: Sorts are performed in parallel across other dimensions
   - Much slower than af-mean (which is O(N))
   - Example: For [1000, 1000] array, median along dim=0:
     * Performs 1000 parallel sorts of length 1000
     * GPU speedup: 10-100× vs CPU
   
   Example 1: Row-wise median (reduce columns)
   ```clojure
   ;; Compute median of each row
   (let [data (create-array [[1.0 5.0 3.0]
                             [2.0 8.0 4.0]
                             [9.0 1.0 7.0]] [3 3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-median out-ptr data 1)] ; dim=1 reduces columns
     ;; Result shape: [3, 1]
     ;; Result: [3.0, 4.0, 7.0] (median of each row)
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example 2: Column-wise median (reduce rows)
   ```clojure
   ;; Compute median of each column
   (let [data (create-array [[1.0 5.0 3.0]
                             [2.0 8.0 4.0]
                             [9.0 1.0 7.0]] [3 3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-median out-ptr data 0)] ; dim=0 reduces rows
     ;; Result shape: [1, 3]
     ;; Result: [2.0, 5.0, 4.0] (median of each column)
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example 3: Robust batch statistics
   ```clojure
   ;; Dataset with outliers in some batches
   (let [measurements (create-array data [1000 100]) ; 100 batches of 1000 measurements
         ;; Some measurements have outliers/artifacts
         median-per-batch (let [out-ptr (mem/alloc-pointer ::mem/pointer)
                                _ (af-median out-ptr measurements 0)]
                            (mem/read-pointer out-ptr ::mem/pointer))
         ;; Shape: [1, 100] - one median per batch
         mean-per-batch (let [out-ptr (mem/alloc-pointer ::mem/pointer)
                              _ (af-mean out-ptr measurements 0)]
                          (mem/read-pointer out-ptr ::mem/pointer))]
     ;; Compare mean vs median to detect outlier-contaminated batches
     ;; Large mean-median difference indicates skewness/outliers
     (abs (sub mean-per-batch median-per-batch)))
   ```
   
   Example 4: Temporal median (time series)
   ```clojure
   ;; Sensor data over time [channels, time-points]
   (let [sensor-data (create-array readings [10 10000]) ; 10 sensors, 10k time points
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-median out-ptr sensor-data 1)] ; median over time for each sensor
     ;; Result shape: [10, 1]
     ;; Result: Typical (median) value for each sensor
     ;; Robust to occasional spikes/artifacts
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example 5: Image brightness normalization
   ```clojure
   ;; Batch of images [width, height, channels, batch-size]
   (let [images (create-array image-data [256 256 3 50]) ; 50 RGB images
         out-ptr (mem/alloc-pointer ::mem/pointer)
         ;; Compute median brightness per image
         _ (af-median out-ptr 
                      (af-median out-ptr
                                 (af-median out-ptr images 0) ; reduce width
                                 0) ; reduce height
                      0)] ; reduce channels
     ;; Result shape: [1, 1, 1, 50]
     ;; Result: Median brightness per image (robust to bright/dark spots)
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example 6: Detect skewed distributions
   ```clojure
   ;; Compare mean and median to detect skewness
   (let [data (create-array values [1000 100])
         mean-ptr (mem/alloc-pointer ::mem/pointer)
         median-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-mean mean-ptr data 0)
         _ (af-median median-ptr data 0)
         mean-arr (mem/read-pointer mean-ptr ::mem/pointer)
         median-arr (mem/read-pointer median-ptr ::mem/pointer)
         skewness-indicator (sub mean-arr median-arr)]
     ;; If mean >> median: Right-skewed (positive outliers)
     ;; If mean << median: Left-skewed (negative outliers)
     ;; If mean ≈ median: Symmetric distribution
     skewness-indicator)
   ```
   
   Example 7: Robust feature scaling (ML preprocessing)
   ```clojure
   ;; Scale features using median and MAD instead of mean and std
   (let [features (create-array feature-data [10000 50]) ; 10k samples, 50 features
         median-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-median median-ptr features 0) ; per-feature median
         median-arr (mem/read-pointer median-ptr ::mem/pointer)
         ;; Compute MAD (Median Absolute Deviation)
         deviations (abs (sub features median-arr))
         mad-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-median mad-ptr deviations 0)
         mad-arr (mem/read-pointer mad-ptr ::mem/pointer)
         ;; Robust scaling
         scaled (div (sub features median-arr)
                    (add mad-arr 1e-8))] ; small constant for numerical stability
     ;; More robust than (x - mean) / std when outliers present
     scaled)
   ```
   
   When to Use:
   - Data contains outliers or extreme values
   - Need robust estimate resistant to contamination
   - Distribution is skewed (not symmetric/normal)
   - \"Typical\" value more important than true average
   
   When NOT to Use:
   - Data is normally distributed without outliers → use af-mean (much faster)
   - Need optimal statistical efficiency → median is only ~64% efficient vs mean
   - Real-time performance critical → O(N log N) vs O(N) for mean
   - Working with complex numbers → no natural ordering
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-median-all: Global median (returns scalar)
   - af-mean: Arithmetic mean (faster, not robust)
   - af-medfilt, af-medfilt1, af-medfilt2: Spatial median filtering"
  "af_median" [::mem/pointer ::mem/pointer ::mem/long] ::mem/int)

;; af_err af_median_all(double *realVal, double *imagVal, const af_array in)
(defcfn af-median-all
  "Compute global median of entire array.
   
   Returns the median of all elements in the array as a scalar value,
   regardless of array dimensions.
   
   Parameters:
   - real-val: out pointer for real part of median (always used)
   - imag-val: out pointer for imaginary part (currently unused, set to NULL)
   - in: input array
   
   Algorithm:
   1. Flatten array to 1D (logically, not physically)
   2. Sort all elements
   3. If odd count: Select middle element
   4. If even count: Average the two middle elements
   5. Return as double precision scalar
   
   Behavior:
   - Treats the array as a single long vector
   - All dimensions are reduced to a scalar
   - Example: [100, 50, 20] → scalar median value
   
   Type Conversion:
   - All input types → double (for precision and consistency)
   - Integer inputs: Converted to double
   - Float inputs: Converted to double
   - Complex inputs: Not supported (imagVal currently unused)
   
   Performance:
   - Complexity: O(N log N) where N is total number of elements
   - GPU acceleration: Significant speedup for large arrays
   - Shortcut optimizations:
     * N=1: Returns the single element
     * N=2: Returns average of both elements
     * General case: Full sort and index
   
   Example 1: Overall median of matrix
   ```clojure
   ;; Find median value across entire dataset
   (let [data (create-array [[1.0 5.0 9.0]
                             [2.0 6.0 8.0]
                             [3.0 4.0 7.0]] [3 3])
         real-ptr (mem/alloc-pointer ::mem/double)
         imag-ptr (mem/nullptr)
         _ (af-median-all real-ptr imag-ptr data)
         median (mem/read-pointer real-ptr ::mem/double)]
     ;; Sorted: [1,2,3,4,5,6,7,8,9]
     ;; Median: 5.0 (middle element)
     median) ; => 5.0
   ```
   
   Example 2: Robust center of distribution
   ```clojure
   ;; Dataset with outliers
   (let [values (create-array [1.0 2.0 3.0 4.0 5.0 100.0] [6]) ; one outlier
         ;; Compute both mean and median
         mean-real (mem/alloc-pointer ::mem/double)
         mean-imag (mem/nullptr)
         _ (af-mean-all mean-real mean-imag values)
         mean-val (mem/read-pointer mean-real ::mem/double)
         
         median-real (mem/alloc-pointer ::mem/double)
         median-imag (mem/nullptr)
         _ (af-median-all median-real median-imag values)
         median-val (mem/read-pointer median-real ::mem/double)]
     ;; mean-val ≈ 19.17 (pulled up by outlier)
     ;; median-val = 3.5 (robust to outlier)
     ;; Median represents \"typical\" value better
     {:mean mean-val :median median-val})
   ```
   
   Example 3: Income statistics
   ```clojure
   ;; Income distribution (typically right-skewed)
   (let [incomes (create-array [30000 32000 35000 28000 40000 500000] [6])
         real-ptr (mem/alloc-pointer ::mem/double)
         _ (af-median-all real-ptr (mem/nullptr) incomes)
         median-income (mem/read-pointer real-ptr ::mem/double)]
     ;; Sorted: [28k, 30k, 32k, 35k, 40k, 500k]
     ;; Median: (32k + 35k) / 2 = 33,500
     ;; Mean: ~111k (misleading due to outlier)
     ;; Median better represents \"typical\" income
     median-income) ; => 33500.0
   ```
   
   Example 4: Sensor baseline
   ```clojure
   ;; Establish robust baseline for sensor readings
   (let [sensor-readings (create-array readings [10000]) ; 10k readings over time
         real-ptr (mem/alloc-pointer ::mem/double)
         _ (af-median-all real-ptr (mem/nullptr) sensor-readings)
         baseline (mem/read-pointer real-ptr ::mem/double)
         ;; Remove baseline
         centered (sub sensor-readings baseline)]
     ;; Baseline is robust to occasional spikes/artifacts
     {:baseline baseline :centered-data centered})
   ```
   
   Example 5: Image brightness
   ```clojure
   ;; Find median brightness of image
   (let [image (create-array pixels [512 512]) ; grayscale image
         real-ptr (mem/alloc-pointer ::mem/double)
         _ (af-median-all real-ptr (mem/nullptr) image)
         median-brightness (mem/read-pointer real-ptr ::mem/double)]
     ;; Median brightness is robust to very dark/bright regions
     ;; Useful for normalization or histogram equalization
     median-brightness)
   ```
   
   Example 6: Outlier detection threshold
   ```clojure
   ;; Use median + MAD for robust outlier detection
   (let [data (create-array values [10000])
         ;; Step 1: Compute median
         median-real (mem/alloc-pointer ::mem/double)
         _ (af-median-all median-real (mem/nullptr) data)
         median-val (mem/read-pointer median-real ::mem/double)
         
         ;; Step 2: Compute absolute deviations
         abs-dev (abs (sub data median-val))
         
         ;; Step 3: Compute MAD (Median Absolute Deviation)
         mad-real (mem/alloc-pointer ::mem/double)
         _ (af-median-all mad-real (mem/nullptr) abs-dev)
         mad-val (mem/read-pointer mad-real ::mem/double)
         
         ;; Step 4: Outlier threshold (3-MAD rule)
         threshold (* 3 mad-val)
         outliers (gt (abs (sub data median-val)) threshold)]
     ;; More robust than mean ± 3*std for non-normal distributions
     {:median median-val
      :mad mad-val
      :threshold threshold
      :outlier-mask outliers})
   ```
   
   Example 7: Quality control
   ```clojure
   ;; Manufacturing: Check if batch median is within specification
   (let [measurements (create-array batch-data [500]) ; 500 parts measured
         spec-lower 99.0
         spec-upper 101.0
         real-ptr (mem/alloc-pointer ::mem/double)
         _ (af-median-all real-ptr (mem/nullptr) measurements)
         batch-median (mem/read-pointer real-ptr ::mem/double)
         within-spec? (and (>= batch-median spec-lower)
                          (<= batch-median spec-upper))]
     ;; Median less sensitive to occasional measurement errors
     {:batch-median batch-median
      :passes-spec? within-spec?})
   ```
   
   When to Use:
   - Need single robust center value for entire dataset
   - Data may contain outliers or is skewed
   - \"Typical\" value more important than exact average
   - Comparing datasets for central tendency
   
   When NOT to Use:
   - Need dimension-specific medians → use af-median instead
   - Data is normally distributed → use af-mean-all (much faster)
   - Working with very large arrays → consider sampling for approximate median
   - Need exact middle of sorted data in original type → use custom sort+index
   
   Returns:
   ArrayFire error code (af_err enum)
   
   Note:
   - imagVal parameter exists for API consistency but is currently unused
   - Always pass NULL/nullptr for imagVal
   - Complex numbers are not supported for median
   
   See also:
   - af-median: Dimensional median reduction
   - af-mean-all: Global arithmetic mean (faster, not robust)
   - af-mean: Dimensional mean reduction"
  "af_median_all" [::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)
