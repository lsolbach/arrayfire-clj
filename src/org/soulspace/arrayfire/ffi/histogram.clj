(ns org.soulspace.arrayfire.ffi.histogram
  "Bindings for ArrayFire histogram computation.
   
   Histograms are fundamental tools in image processing and data analysis that
   summarize the distribution of values in an array by counting occurrences
   within discrete bins. They provide statistical insights and enable various
   processing techniques.
   
   Mathematical Foundation:
   
   A histogram H with n bins partitions the value range [min, max] into n
   equal-width intervals:
   
   Bin width: w = (max - min) / n
   Bin i contains: [min + i·w, min + (i+1)·w)
   
   For each value v in the input:
   - Compute bin index: i = floor((v - min) / w)
   - Increment: H[i] = H[i] + 1
   
   The histogram satisfies:
   Σ H[i] = total number of elements (for i = 0 to n-1)
   
   Use Cases:
   
   1. **Image Analysis**:
      - Brightness distribution analysis
      - Contrast assessment
      - Exposure evaluation
      - Color distribution (per channel)
   
   2. **Histogram Equalization**:
      - Enhance image contrast
      - Flatten histogram distribution
      - Improve visual appearance
      - Applications: medical imaging, low-light enhancement
   
   3. **Thresholding**:
      - Otsu's method for automatic threshold selection
      - Bimodal histogram analysis
      - Foreground/background separation
      - Applications: document scanning, object segmentation
   
   4. **Feature Extraction**:
      - Histogram of Oriented Gradients (HOG)
      - Local Binary Patterns (LBP)
      - Color histograms for object recognition
      - Applications: pedestrian detection, texture analysis
   
   5. **Quality Control**:
      - Detect outliers via histogram tails
      - Monitor data distribution shifts
      - Validate sensor readings
      - Applications: manufacturing, signal processing
   
   6. **Image Matching**:
      - Compare histograms for similarity
      - Color-based image retrieval
      - Histogram intersection/correlation
      - Applications: content-based image search
   
   7. **Data Visualization**:
      - Understand data distribution
      - Identify clusters or gaps
      - Spot anomalies
      - Applications: exploratory data analysis
   
   Parameter Selection:
   
   **Number of Bins (nbins)**:
   - Too few bins: Loss of detail, oversimplified distribution
   - Too many bins: Noisy histogram, sparse bins
   
   Guidelines:
   - 8-bit images: 256 bins (one per intensity level)
   - 16-bit images: 256-1024 bins (sample the range)
   - Float data: 32-256 bins (depends on range)
   - General data: Sturges' rule: nbins = 1 + log₂(N)
   - General data: Rice rule: nbins = 2·∛N
   
   **Range (minval, maxval)**:
   - Must span all data values of interest
   - Values < minval accumulate in bin 0
   - Values ≥ maxval accumulate in bin (nbins-1)
   - Can use to clip outliers
   
   Auto-range (when omitted):
   - minval = minimum value in array
   - maxval = maximum value in array
   - Ensures all data is captured
   - May be slow due to min/max computation
   
   Histogram Metrics:
   
   1. **Mean**: Σ(bin_center[i] × H[i]) / Σ H[i]
      - Average value in the distribution
   
   2. **Variance**: Σ((bin_center[i] - mean)² × H[i]) / Σ H[i]
      - Spread of the distribution
   
   3. **Entropy**: -Σ(p[i] × log₂(p[i])) where p[i] = H[i] / Σ H[i]
      - Information content, randomness measure
   
   4. **Mode**: Bin with maximum count
      - Most common value range
   
   5. **Quantiles**: Cumulative histogram analysis
      - Median: value at 50% cumulative count
      - Percentiles: values at specific cumulative counts
   
   Histogram Operations:
   
   1. **Histogram Matching**:
      Transform image A to match histogram of image B
      - Compute CDFs of both histograms
      - Create mapping function
      - Apply to transform image A
   
   2. **Histogram Equalization**:
      Flatten histogram for uniform distribution
      - Compute CDF: CDF[i] = Σ H[j] for j ≤ i
      - Normalize: CDF_norm = (CDF - CDF_min) / (N - CDF_min)
      - Map: new_value = CDF_norm × (max_level - 1)
   
   3. **Adaptive Histogram Equalization (AHE)**:
      Apply equalization locally in tiles
      - Divide image into non-overlapping tiles
      - Compute histogram per tile
      - Equalize each tile independently
      - Interpolate at tile boundaries
   
   4. **Contrast Limited AHE (CLAHE)**:
      AHE with clipping to limit amplification
      - Clip histogram bins at threshold
      - Redistribute excess to other bins
      - Apply AHE with clipped histograms
   
   Multi-Dimensional Histograms:
   
   ArrayFire's histogram operates on each 2D plane independently:
   - Input: [width, height, channels, batches]
   - Output: [nbins, 1, channels, batches]
   
   For 3D joint histograms (e.g., RGB color):
   - Flatten dimensions: [width×height, channels]
   - Compute 1D histograms per channel
   - Or use external multi-dimensional binning
   
   Performance Considerations:
   
   **Computational Complexity**:
   - Serial: O(N) where N = number of elements
   - Parallel: O(N/P + log(P)) with P processors
   - GPU: ~1-5ms for 1M elements with 256 bins
   
   **Memory Usage**:
   - Output: nbins × sizeof(uint32) per 2D plane
   - 256 bins: 1KB per plane
   - Minimal memory footprint
   
   **Optimization**:
   - Use linear memory layout (isLinear=true) when possible
   - Power-of-2 bins slightly faster (32, 64, 128, 256)
   - Large nbins (>4000) may be slower due to memory access
   
   **Atomic Operations**:
   - GPU histogram uses atomic increments
   - Contention on popular bins can slow computation
   - Uniform distributions faster than peaked distributions
   
   Common Patterns:
   
   1. **Image Histogram**:
   ```clojure
   ;; 8-bit grayscale image histogram
   (let [img (load-image \"image.jpg\" :grayscale true)
         hist-ptr (mem/alloc-instance ::mem/pointer)]
     (af-histogram hist-ptr img 256 0.0 255.0)
     (let [hist (mem/read-ptr hist-ptr)]
       (visualize-histogram hist)))
   ```
   
   2. **Auto-Range Histogram**:
   ```clojure
   ;; Histogram with automatic range detection
   (let [data (random-array [1000 1000])
         min-val (array-min data)
         max-val (array-max data)
         hist-ptr (mem/alloc-instance ::mem/pointer)]
     (af-histogram hist-ptr data 100 min-val max-val)
     (process-histogram (mem/read-ptr hist-ptr)))
   ```
   
   3. **Batch Histograms**:
   ```clojure
   ;; Compute histograms for multiple images
   (let [images (load-image-batch [\"img1.jpg\" \"img2.jpg\" \"img3.jpg\"])
         ;; images: [width height 1 3]
         hist-ptr (mem/alloc-instance ::mem/pointer)]
     (af-histogram hist-ptr images 256 0.0 255.0)
     ;; hist: [256 1 1 3] - one histogram per image
     (analyze-batch-histograms (mem/read-ptr hist-ptr)))
   ```
   
   4. **Color Histogram**:
   ```clojure
   ;; Separate histograms for R, G, B channels
   (let [rgb-img (load-image \"color.jpg\")
         ;; rgb-img: [width height 3 1]
         hist-ptr (mem/alloc-instance ::mem/pointer)]
     (af-histogram hist-ptr rgb-img 256 0.0 255.0)
     ;; hist: [256 1 3 1] - one histogram per color channel
     (let [hist (mem/read-ptr hist-ptr)
           r-hist (get-channel hist 0)
           g-hist (get-channel hist 1)
           b-hist (get-channel hist 2)]
       (compare-color-distributions r-hist g-hist b-hist)))
   ```
   
   5. **Outlier Clipping**:
   ```clojure
   ;; Clip outliers by restricting range
   (let [noisy-data (sensor-readings)
         ;; Ignore values < 0.1 and > 0.9 (1st and 99th percentiles)
         hist-ptr (mem/alloc-instance ::mem/pointer)]
     (af-histogram hist-ptr noisy-data 80 0.1 0.9)
     ;; Values outside [0.1, 0.9] are clipped
     (analyze-clean-histogram (mem/read-ptr hist-ptr)))
   ```
   
   Histogram Comparison Metrics:
   
   1. **Chi-Square Distance**:
      χ² = Σ((H1[i] - H2[i])² / (H1[i] + H2[i]))
      - Symmetric, always non-negative
      - 0 indicates identical histograms
   
   2. **Intersection**:
      I = Σ min(H1[i], H2[i])
      - Measures overlap
      - Higher values indicate similarity
   
   3. **Bhattacharyya Distance**:
      D = -ln(Σ √(H1[i] × H2[i]))
      - Requires normalized histograms
      - 0 indicates identical, ∞ indicates no overlap
   
   4. **Earth Mover's Distance (EMD)**:
      - Minimum cost to transform H1 into H2
      - Considers bin positions (structure-aware)
      - More computationally expensive
   
   Applications by Domain:
   
   **Medical Imaging**:
   - Intensity distribution analysis
   - Contrast enhancement for diagnosis
   - Bone density measurement
   - Tumor detection via histogram analysis
   
   **Photography**:
   - Exposure assessment
   - Dynamic range evaluation
   - Histogram-based auto-exposure
   - Tone curve adjustment
   
   **Machine Learning**:
   - Feature extraction (HOG, color histograms)
   - Data preprocessing and normalization
   - Outlier detection
   - Distribution matching for domain adaptation
   
   **Quality Control**:
   - Defect detection via histogram anomalies
   - Product classification by feature distribution
   - Process monitoring (temperature, pressure distributions)
   
   **Astronomy**:
   - Star brightness distribution
   - Galaxy classification
   - Background subtraction
   - Noise characterization
   
   Troubleshooting:
   
   **Empty bins in sparse data**:
   - Reduce number of bins
   - Check if data range matches [minval, maxval]
   - Consider logarithmic binning for power-law distributions
   
   **All data in one bin**:
   - Check minval and maxval span the data range
   - Verify data has variation (not all same value)
   - Ensure correct data type (integer vs float)
   
   **Unexpected distribution shape**:
   - Verify minval < maxval
   - Check for outliers dominating bins
   - Ensure correct number of bins
   - Validate input data preprocessing
   
   **Poor performance**:
   - Reduce number of bins if > 1000
   - Use power-of-2 bin counts
   - Ensure linear memory layout when possible
   - Batch multiple histogram computations
   
   See Also:
   - af_hist_equal: Histogram equalization
   - af_min: Compute minimum for range
   - af_max: Compute maximum for range
   - af_sum: Verify histogram sum equals element count"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; af_err af_histogram(af_array *out, const af_array in,
;;                     const unsigned nbins, const double minval, const double maxval)
(defcfn af-histogram
  "Compute histogram of array data.
   
   Bins the values in the input array into discrete intervals (bins) and
   counts the number of occurrences in each bin. Returns an array of unsigned
   integers representing the histogram.
   
   Parameters:
   
   - out: af_array* output histogram array
     * Dimensions: [nbins, 1, in.dims[2], in.dims[3]]
     * Type: u32 (unsigned 32-bit integer)
     * Contains counts for each bin
     * For 2D input [W, H]: output is [nbins, 1, 1, 1]
     * For 3D input [W, H, C]: output is [nbins, 1, C, 1] (one histogram per channel)
     * For 4D input [W, H, C, N]: output is [nbins, 1, C, N] (one histogram per batch)
   
   - in: af_array input data array
     * Can be any numeric type (f32, f64, s32, u32, s16, u16, s8, u8, etc.)
     * 1D, 2D, 3D, or 4D arrays supported
     * Values outside [minval, maxval) are clamped to edge bins
   
   - nbins: unsigned number of bins
     * Must be > 0
     * Typical values: 32, 64, 128, 256
     * For 8-bit images: 256 bins (one per intensity level)
     * For 16-bit images: 256-1024 bins
     * For float data: 32-256 bins
     * Maximum: ~4000 bins (implementation-dependent)
   
   - minval: double minimum value for binning
     * Lower bound of histogram range
     * Values < minval are placed in bin 0
     * Must be < maxval
   
   - maxval: double maximum value for binning
     * Upper bound of histogram range
     * Values ≥ maxval are placed in bin (nbins-1)
     * Must be > minval
   
   Returns:
   AF_SUCCESS (0) on success, error code otherwise:
   - AF_ERR_ARG: Invalid arguments (null pointers, nbins = 0, minval ≥ maxval)
   - AF_ERR_SIZE: Array dimensions invalid
   - AF_ERR_TYPE: Unsupported array type
   - AF_ERR_NO_MEM: Out of memory
   
   Binning Formula:
   
   For each value v in the input:
   1. Compute normalized position: t = (v - minval) / (maxval - minval)
   2. Compute bin index: i = floor(t × nbins)
   3. Clamp to valid range: i = clamp(i, 0, nbins-1)
   4. Increment bin count: histogram[i] = histogram[i] + 1
   
   Edge Cases:
   - Values exactly equal to minval → bin 0
   - Values exactly equal to maxval → bin (nbins-1)
   - Values < minval → bin 0 (accumulated)
   - Values > maxval → bin (nbins-1) (accumulated)
   
   Multi-dimensional Processing:
   
   For 2D arrays [width, height]:
   - Single histogram computed over all elements
   - Output: [nbins, 1, 1, 1]
   
   For 3D arrays [width, height, channels]:
   - One histogram per channel (independent)
   - Output: [nbins, 1, channels, 1]
   - Useful for RGB images (3 histograms)
   
   For 4D arrays [width, height, channels, batches]:
   - One histogram per (channel, batch) combination
   - Output: [nbins, 1, channels, batches]
   - Useful for batch processing
   
   Common Use Cases:
   
   1. **Image Histogram** (8-bit grayscale):
   ```clojure
   (af-histogram hist-out grayscale-img 256 0.0 255.0)
   ;; Output: 256 bins covering intensity range [0, 255]
   ```
   
   2. **Color Image Histogram** (RGB channels):
   ```clojure
   ;; rgb-img dimensions: [width, height, 3, 1]
   (af-histogram hist-out rgb-img 256 0.0 255.0)
   ;; Output: [256, 1, 3, 1] - separate histograms for R, G, B
   ```
   
   3. **Data Distribution Analysis**:
   ```clojure
   (af-histogram hist-out float-data 100 -5.0 5.0)
   ;; Output: 100 bins covering range [-5.0, 5.0)
   ```
   
   4. **Automatic Range** (requires computing min/max first):
   ```clojure
   (let [min-val (af-min-all data)
         max-val (af-max-all data)]
     (af-histogram hist-out data 128 min-val max-val))
   ```
   
   5. **Outlier Clipping**:
   ```clojure
   ;; Clip to 1st-99th percentile range
   (af-histogram hist-out data 100 0.01 0.99)
   ;; Values outside [0.01, 0.99] are accumulated in edge bins
   ```
   
   Performance:
   - Typical: 1-5ms for 1M elements, 256 bins on GPU
   - Scales linearly with number of elements
   - GPU acceleration provides 10-100× speedup
   - Larger bin counts slightly slower (more memory access)
   - Power-of-2 bin counts may be slightly faster
   
   Bin Selection Guidelines:
   
   **Sturges' Rule** (general data):
   nbins = 1 + ⌈log₂(N)⌉ where N = number of elements
   - 1,000 elements → 11 bins
   - 10,000 elements → 15 bins
   - 1,000,000 elements → 21 bins
   
   **Rice Rule** (alternative):
   nbins = ⌈2 × ∛N⌉
   - 1,000 elements → 20 bins
   - 10,000 elements → 43 bins
   
   **Scott's Rule** (for normally distributed data):
   bin_width = 3.49 × σ × N^(-1/3)
   nbins = (max - min) / bin_width
   
   **Freedman-Diaconis Rule** (robust to outliers):
   bin_width = 2 × IQR × N^(-1/3)
   nbins = (max - min) / bin_width
   
   **Image Processing**:
   - 8-bit: 256 bins (one per level)
   - 16-bit: 256-1024 bins (sample the range)
   - HDR: 128-512 bins
   
   Histogram Normalization:
   
   To convert counts to probability distribution:
   ```clojure
   ;; P(x) = count(x) / total_count
   (let [hist (af-histogram ...)]
     (af-div hist hist (af-sum-all hist)))
   ;; Result: normalized histogram where Σ P(x) = 1.0
   ```
   
   Applications:
   
   1. **Histogram Equalization**:
      - Enhance image contrast
      - Flatten intensity distribution
      - See af_hist_equal function
   
   2. **Thresholding**:
      - Otsu's method for automatic threshold
      - Analyze bimodal distributions
      - Segment foreground/background
   
   3. **Image Comparison**:
      - Compute histograms of two images
      - Compare using chi-square, intersection, or correlation
      - Content-based image retrieval
   
   4. **Quality Control**:
      - Monitor data distribution
      - Detect anomalies or shifts
      - Statistical process control
   
   5. **Feature Extraction**:
      - Color histograms for object recognition
      - Histogram of Oriented Gradients (HOG)
      - Texture analysis
   
   Troubleshooting:
   
   **Empty histogram (all zeros)**:
   - Check if minval and maxval span the data range
   - Verify input array is not empty
   - Ensure data type is supported
   
   **All counts in one bin**:
   - Data may have no variation (constant value)
   - Range [minval, maxval) may be too wide
   - Check for correct min/max values
   
   **Unexpected distribution**:
   - Verify minval < maxval
   - Check for outliers affecting the range
   - Ensure correct bin count
   - Validate data preprocessing
   
   **Performance issues**:
   - Reduce number of bins if > 1000
   - Use linear memory layout
   - Batch multiple histograms
   - Profile to identify bottlenecks
   
   See Also:
   - af_hist_equal: Histogram equalization for contrast enhancement
   - af_min/af_max: Compute range for automatic binning
   - af_sum: Verify total count equals element count
   - af_draw_hist: Visualize histogram (graphics)"
  "af_histogram" [::mem/pointer ::mem/pointer ::mem/int ::mem/double ::mem/double] ::mem/int)
