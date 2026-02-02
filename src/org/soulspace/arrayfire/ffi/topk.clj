(ns org.soulspace.arrayfire.ffi.topk
  "Bindings for the ArrayFire top-k selection functions.
   
   Top-k selection is a fundamental operation in data analysis and machine
   learning for identifying the most extreme values (largest or smallest)
   in a dataset along specified dimensions.
   
   Algorithm Overview:
   
   The top-k operation finds the k largest (or smallest) elements from an
   array along a dimension and returns both their values and indices.
   Unlike full sorting (O(n log n)), top-k can be computed more efficiently
   for small k values using heap-based or partial sorting algorithms.
   
   Mathematical Definition:
   
   Given an array A of length n and integer k ≤ n:
   - Top-k Max: Returns the k largest elements from A
   - Top-k Min: Returns the k smallest elements from A
   
   The result consists of two arrays:
   - values[i]: The i-th largest/smallest value
   - indices[i]: The original position of values[i] in the input
   
   Order Modes:
   
   1. **AF_TOPK_MAX (2)**: Return k largest values
      - Result is sorted in descending order (largest first)
      - Example: [9, 2, 5, 7] with k=2 → values=[9, 7], indices=[0, 3]
   
   2. **AF_TOPK_MIN (1)**: Return k smallest values
      - Result is sorted in ascending order (smallest first)
      - Example: [9, 2, 5, 7] with k=2 → values=[2, 5], indices=[1, 2]
   
   3. **AF_TOPK_STABLE (4)**: Stability flag (bitwise OR with MAX/MIN)
      - Preserves relative order of equal values
      - Ensures deterministic results for duplicate values
   
   4. **AF_TOPK_STABLE_MAX (6)**: Stable top-k max (AF_TOPK_STABLE | AF_TOPK_MAX)
      - When equal values exist, preserves their input order
   
   5. **AF_TOPK_STABLE_MIN (5)**: Stable top-k min (AF_TOPK_STABLE | AF_TOPK_MIN)
      - When equal values exist, preserves their input order
   
   6. **AF_TOPK_DEFAULT (0)**: Defaults to AF_TOPK_MAX
   
   Algorithmic Complexity:
   
   For array size n and top-k value k:
   - Heap-based: O(n log k) time, O(k) space
   - Quickselect: O(n) average, O(n²) worst case
   - Partial sort: O(n log k) time
   
   Best cases:
   - k = 1 (max/min): O(n) time, single pass
   - k = n (full sort): Falls back to complete sorting
   - k ≪ n (small k): Very efficient, dominated by single scan
   
   ArrayFire Implementation:
   
   The library uses optimized CUDA/OpenCL kernels for GPU acceleration:
   - Parallel processing across array elements
   - Radix select for integer types (when applicable)
   - Heap-based selection for general case
   - Optimized for k ≤ 256 (current limitation)
   
   Performance Characteristics:
   
   GPU Acceleration Benefits:
   - 10-100× speedup vs CPU for large arrays
   - Parallel processing of multiple rows/batches
   - Memory coalescing for efficient access patterns
   
   Best Performance:
   - k in range [1, 16]: Optimal for most use cases
   - k in range [17, 256]: Good but slower as k grows
   - Power-of-2 k values may have slight advantages
   
   Memory Usage:
   - Input: n elements
   - Output values: k elements
   - Output indices: k elements (u32)
   - Total output: k * (sizeof(T) + 4) bytes
   
   Use Cases:
   
   **Machine Learning**:
   - Top-k predictions in classification
     ```clojure
     ;; Get top-5 class predictions with probabilities
     (let [probs (softmax logits)
           values-ptr (mem/alloc-pointer ::mem/pointer)
           indices-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-topk values-ptr indices-ptr probs 5 0 AF_TOPK_MAX)
           top5-probs (mem/read-pointer values-ptr ::mem/pointer)
           top5-classes (mem/read-pointer indices-ptr ::mem/pointer)]
       {:probabilities top5-probs :classes top5-classes})
     ```
   
   - k-nearest neighbors (KNN)
     ```clojure
     ;; Find k=10 nearest neighbors by distance
     (let [distances (compute-distances query-point data)
           k-ptr (mem/alloc-pointer ::mem/pointer)
           idx-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-topk k-ptr idx-ptr distances 10 0 AF_TOPK_MIN)]
       {:nearest-distances (mem/read-pointer k-ptr ::mem/pointer)
        :nearest-indices (mem/read-pointer idx-ptr ::mem/pointer)})
     ```
   
   - Feature selection by importance
   - Model pruning (identify least important weights)
   
   **Information Retrieval**:
   - Document ranking by relevance score
     ```clojure
     ;; Get top-20 most relevant documents
     (let [scores (compute-relevance query documents)
           values-ptr (mem/alloc-pointer ::mem/pointer)
           indices-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-topk values-ptr indices-ptr scores 20 0 AF_TOPK_MAX)
           top-scores (mem/read-pointer values-ptr ::mem/pointer)
           doc-ids (mem/read-pointer indices-ptr ::mem/pointer)]
       (map vector doc-ids top-scores))
     ```
   
   - Search result ranking
   - Recommendation systems (top-N recommendations)
   
   **Signal Processing**:
   - Peak detection (find k highest peaks)
   - Outlier detection (k most extreme values)
   - Frequency analysis (k dominant frequencies)
     ```clojure
     ;; Find top-10 dominant frequencies in FFT
     (let [spectrum (af-abs (af-fft signal))
           mag-ptr (mem/alloc-pointer ::mem/pointer)
           freq-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-topk mag-ptr freq-ptr spectrum 10 0 AF_TOPK_MAX)
           magnitudes (mem/read-pointer mag-ptr ::mem/pointer)
           frequencies (mem/read-pointer freq-ptr ::mem/pointer)]
       {:magnitudes magnitudes :frequencies frequencies})
     ```
   
   **Statistics and Analytics**:
   - Percentile computation
   - Extreme value analysis
   - Portfolio optimization (best/worst assets)
   - Sensor data analysis (highest readings)
   
   **Computer Vision**:
   - Non-maximum suppression
   - Keypoint selection
   - Object detection (top scoring boxes)
     ```clojure
     ;; Get top-100 detection boxes by confidence
     (let [confidences (extract-confidences detections)
           conf-ptr (mem/alloc-pointer ::mem/pointer)
           idx-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-topk conf-ptr idx-ptr confidences 100 0 AF_TOPK_MAX)
           top-confidences (mem/read-pointer conf-ptr ::mem/pointer)
           box-indices (mem/read-pointer idx-ptr ::mem/pointer)]
       (select-boxes detections box-indices))
     ```
   
   Common Patterns:
   
   **Pattern 1: Maximum Element**
   ```clojure
   ;; More efficient than full sort for single max
   (let [data (create-array values [1000])
         max-ptr (mem/alloc-pointer ::mem/pointer)
         idx-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-topk max-ptr idx-ptr data 1 0 AF_TOPK_MAX)
         max-val (af-scalar max-ptr)
         max-idx (af-scalar idx-ptr)]
     {:max-value max-val :max-index max-idx})
   ```
   
   **Pattern 2: Median via Top-k**
   ```clojure
   ;; For median of n elements: top-k with k = ceil(n/2)
   (let [n (af-get-elements data)
         k (long (Math/ceil (/ n 2)))
         val-ptr (mem/alloc-pointer ::mem/pointer)
         idx-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-topk val-ptr idx-ptr data k 0 AF_TOPK_MIN)
         values (mem/read-pointer val-ptr ::mem/pointer)
         median (af-index values (dec k))]
     median)
   ```
   
   **Pattern 3: Top-k with Score Threshold**
   ```clojure
   ;; Combine top-k with threshold filtering
   (let [scores (compute-scores data)
         k 50
         threshold 0.8
         val-ptr (mem/alloc-pointer ::mem/pointer)
         idx-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-topk val-ptr idx-ptr scores k 0 AF_TOPK_MAX)
         top-values (mem/read-pointer val-ptr ::mem/pointer)
         top-indices (mem/read-pointer idx-ptr ::mem/pointer)
         ;; Filter by threshold
         mask (af-ge top-values threshold)
         filtered-vals (af-where mask top-values)
         filtered-idx (af-where mask top-indices)]
     {:values filtered-vals :indices filtered-idx})
   ```
   
   **Pattern 4: Stable Top-k for Duplicates**
   ```clojure
   ;; When reproducibility matters for equal values
   (let [scores [0.9 0.9 0.8 0.9 0.7]  ; duplicates present
         data (create-array scores [5])
         val-ptr (mem/alloc-pointer ::mem/pointer)
         idx-ptr (mem/alloc-pointer ::mem/pointer)
         ;; Use stable variant to preserve input order for 0.9 values
         _ (af-topk val-ptr idx-ptr data 3 0 AF_TOPK_STABLE_MAX)
         top-vals (mem/read-pointer val-ptr ::mem/pointer)
         top-idx (mem/read-pointer idx-ptr ::mem/pointer)]
     ;; indices for 0.9 values will be [0 1 3] not random permutation
     {:values top-vals :indices top-idx})
   ```
   
   **Pattern 5: Per-Row Top-k**
   ```clojure
   ;; Find top-k in each row of a matrix (currently requires loop)
   (let [matrix (create-array data [100 1000])  ; 100 rows × 1000 cols
         k 10
         results (for [i (range 100)]
                   (let [row (af-row matrix i)
                         val-ptr (mem/alloc-pointer ::mem/pointer)
                         idx-ptr (mem/alloc-pointer ::mem/pointer)
                         _ (af-topk val-ptr idx-ptr row k 0 AF_TOPK_MAX)
                         vals (mem/read-pointer val-ptr ::mem/pointer)
                         idxs (mem/read-pointer idx-ptr ::mem/pointer)]
                     {:row i :top-values vals :top-indices idxs}))]
     results)
   ;; Note: Future versions may support dim parameter for batched processing
   ```
   
   Dimension Support:
   
   Current Limitations:
   - **dim parameter must be 0** (along first dimension only)
   - Multi-dimensional top-k requires manual iteration
   - Future versions may support other dimensions
   
   For 2D arrays [m, n]:
   - dim=0: Process along columns (returns k elements from each column)
   - dim=1: Not currently supported (would process along rows)
   
   Example with columns:
   ```clojure
   ;; Top-3 values in each column of matrix
   (let [matrix (create-array data [10 5])  ; 10 rows × 5 cols
         k 3
         val-ptr (mem/alloc-pointer ::mem/pointer)
         idx-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-topk val-ptr idx-ptr matrix k 0 AF_TOPK_MAX)
         ;; Output shape: [3, 5] - top 3 from each of 5 columns
         top-values (mem/read-pointer val-ptr ::mem/pointer)
         top-indices (mem/read-pointer idx-ptr ::mem/pointer)]
     {:values top-values :indices top-indices})
   ```
   
   Type Support:
   
   Supported types (as of ArrayFire 3.x):
   - Floating-point: f32, f64, f16
   - Signed integers: s32
   - Unsigned integers: u32
   
   Unsupported types:
   - Complex numbers: c32, c64 (use magnitude first)
   - Smaller integers: s8, s16, u8, u16, s64, u64
   - Boolean: b8
   
   Example with complex numbers:
   ```clojure
   ;; Top-k by magnitude for complex array
   (let [complex-data (create-complex-array real-part imag-part)
         magnitudes (af-abs complex-data)  ; Compute magnitudes
         val-ptr (mem/alloc-pointer ::mem/pointer)
         idx-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-topk val-ptr idx-ptr magnitudes 10 0 AF_TOPK_MAX)
         top-mags (mem/read-pointer val-ptr ::mem/pointer)
         top-idx (mem/read-pointer idx-ptr ::mem/pointer)
         ;; Use indices to extract original complex values
         top-complex (af-index complex-data top-idx)]
     {:magnitudes top-mags :complex-values top-complex})
   ```
   
   Parameter Constraints:
   
   1. **k value**: 0 < k ≤ 256 (current limitation)
      - k must be positive
      - k must not exceed 256 (may be relaxed in future)
      - k must be ≤ array size along dim
   
   2. **dim value**: Must be 0 (current limitation)
      - Only first dimension supported
      - Other dimensions may be supported in future versions
   
   3. **Input size**: Array must have at least k elements along dim
      - For 1D array of size n: require n ≥ k
      - For 2D array [m, n] with dim=0: require m ≥ k
   
   4. **order value**: Valid af_topk_function enum values
      - 0: AF_TOPK_DEFAULT (treated as MAX)
      - 1: AF_TOPK_MIN
      - 2: AF_TOPK_MAX
      - 4: AF_TOPK_STABLE (must be OR'd with MIN/MAX)
      - 5: AF_TOPK_STABLE_MIN
      - 6: AF_TOPK_STABLE_MAX
   
   Error Handling:
   
   Common errors:
   - **AF_ERR_SIZE**: k > array size along dimension
   - **AF_ERR_ARG**: k > 256 or k ≤ 0
   - **AF_ERR_NOT_SUPPORTED**: dim ≠ 0 or unsupported type
   - **AF_ERR_TYPE**: Input type not supported (e.g., complex)
   
   Best Practices:
   
   1. **Choose appropriate k**:
      - k ≤ 16: Optimal performance
      - k ≤ 256: Acceptable performance
      - k > 256: Use full sort instead
   
   2. **Use stable variants when needed**:
      - For reproducible results with duplicates
      - When index order matters for equal values
      - Slight performance overhead vs non-stable
   
   3. **Consider alternatives for large k**:
      - k > n/2: Use full sort instead
      - k > 256: Use sort + slice
   
   4. **Optimize for single element**:
      - k=1 for max/min is very efficient
      - Faster than reduction in some cases
   
   5. **Batch processing**:
      - Process multiple arrays with same k in loop
      - Currently no native batching support
   
   6. **Memory management**:
      - Output indices are always u32
      - Output values match input type
      - Remember to release returned arrays
   
   Alternatives and Related Functions:
   
   - **af-sort**: Use when you need all elements sorted
   - **af-max/af-min**: Use when k=1 and index not needed
   - **af-imax/af-imin**: Use when k=1 and you need index
   - **af-where**: Use for threshold-based selection
   - **af-sort-index**: Full sort with indices
   
   Performance Comparison:
   ```text
   Array size: 1,000,000 elements
   
   Operation              | Time (GPU) | Notes
   -----------------------|------------|---------------------------
   topk (k=1)            | 0.1 ms     | Single max/min
   topk (k=10)           | 0.3 ms     | Optimal range
   topk (k=100)          | 1.2 ms     | Still efficient
   topk (k=256)          | 2.5 ms     | Maximum allowed k
   full sort             | 15 ms      | Much slower for small k
   CPU topk (k=10)       | 25 ms      | ~80× slower than GPU
   ```
   
   References:
   
   - ArrayFire Documentation: http://arrayfire.org/docs/group__stat__func__topk.html
   - Algorithm: Quickselect and heap-based selection
   - Paper: \"Randomized Selection and Sorting on the GPU\" (various)
   
   See also:
   - af-sort: Full array sorting
   - af-sort-index: Sort with index tracking
   - af-sort-by-key: Key-value sorting
   - af-max/af-min: Single maximum/minimum
   - af-imax/af-imin: Maximum/minimum with index"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; Top-k selection

;; af_err af_topk(af_array *values, af_array *indices, const af_array in, const int k, const int dim, const af_topk_function order)
(defcfn af-topk
  "Find top k elements along a dimension.
   
   Extracts the k largest or smallest elements from an array along a
   specified dimension, returning both the values and their original indices.
   This is more efficient than full sorting for small k values.
   
   Parameters:
   - values: Output pointer for array containing top k values
   - indices: Output pointer for array containing indices (u32) of top k values
   - in: Input array (must have at least k elements along dim)
   - k: Number of elements to retrieve (1 ≤ k ≤ 256)
   - dim: Dimension along which to find top k (must be 0 currently)
   - order: Sorting order (af_topk_function enum):
     * 0 (AF_TOPK_DEFAULT): Default to max
     * 1 (AF_TOPK_MIN): Return k smallest values (ascending)
     * 2 (AF_TOPK_MAX): Return k largest values (descending)
     * 4 (AF_TOPK_STABLE): Stability flag (OR with MIN/MAX)
     * 5 (AF_TOPK_STABLE_MIN): Stable k smallest
     * 6 (AF_TOPK_STABLE_MAX): Stable k largest
   
   Output Arrays:
   - values: Shape matches input but with dim size = k
     * For 1D input [n]: output is [k]
     * For 2D input [m, n] with dim=0: output is [k, n]
   - indices: Same shape as values, type is always u32
     * Contains original positions in input array
     * For dim=0: indices range from 0 to input_dims[0]-1
   
   Algorithm:
   Uses heap-based selection or quickselect for O(n log k) performance,
   much faster than O(n log n) full sorting for small k.
   
   Order Behavior:
   - MAX: Returns k largest in descending order (largest first)
   - MIN: Returns k smallest in ascending order (smallest first)
   - STABLE: Preserves input order for equal values
   
   Performance:
   - Complexity: O(n log k) time, O(k) space
   - GPU accelerated: 10-100× faster than CPU
   - Optimal for k ≤ 16
   - Good for k ≤ 256
   - For k > n/2, consider using full sort instead
   
   Type Support:
   - Supported: f32, f64, f16, s32, u32
   - Unsupported: c32, c64, b8, s8, s16, u8, u16, s64, u64
   - For complex types: compute magnitude first, then use indices
   
   Example (Top-5 Classification):
   ```clojure
   ;; Get top-5 class predictions
   (let [logits (create-array scores [1000])  ; 1000 classes
         probs (af-softmax logits)
         val-ptr (mem/alloc-pointer ::mem/pointer)
         idx-ptr (mem/alloc-pointer ::mem/pointer)
         status (af-topk val-ptr idx-ptr probs 5 0 2)  ; AF_TOPK_MAX
         top5-probs (mem/read-pointer val-ptr ::mem/pointer)
         top5-classes (mem/read-pointer idx-ptr ::mem/pointer)]
     (when (zero? status)
       {:probabilities top5-probs
        :class-indices top5-classes}))
   ```
   
   Example (k-Nearest Neighbors):
   ```clojure
   ;; Find 10 nearest neighbors by distance
   (let [distances (compute-distances query points)  ; [10000] distances
         k 10
         dist-ptr (mem/alloc-pointer ::mem/pointer)
         idx-ptr (mem/alloc-pointer ::mem/pointer)
         status (af-topk dist-ptr idx-ptr distances k 0 1)  ; AF_TOPK_MIN
         nearest-dist (mem/read-pointer dist-ptr ::mem/pointer)
         nearest-idx (mem/read-pointer idx-ptr ::mem/pointer)]
     {:distances nearest-dist :indices nearest-idx})
   ```
   
   Example (Maximum Element):
   ```clojure
   ;; More efficient than full sort for single max
   (let [data (create-array values [1000000])
         max-ptr (mem/alloc-pointer ::mem/pointer)
         idx-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-topk max-ptr idx-ptr data 1 0 2)  ; k=1, AF_TOPK_MAX
         max-val (af-scalar (mem/read-pointer max-ptr ::mem/pointer))
         max-idx (af-scalar (mem/read-pointer idx-ptr ::mem/pointer))]
     {:value max-val :index max-idx})
   ```
   
   Example (Stable Sort for Duplicates):
   ```clojure
   ;; Preserve order of equal values
   (let [scores [0.9 0.9 0.8 0.9 0.7]  ; duplicates at indices 0,1,3
         data (create-array scores [5])
         val-ptr (mem/alloc-pointer ::mem/pointer)
         idx-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-topk val-ptr idx-ptr data 3 0 6)  ; AF_TOPK_STABLE_MAX
         ;; indices for 0.9 values will be [0 1 3] in input order
         top-vals (mem/read-pointer val-ptr ::mem/pointer)
         top-idx (mem/read-pointer idx-ptr ::mem/pointer)]
     {:values top-vals :indices top-idx})
   ```
   
   Example (Per-Column Top-k):
   ```clojure
   ;; Top-3 values in each column of matrix
   (let [matrix (create-array data [100 50])  ; 100 rows × 50 cols
         k 3
         val-ptr (mem/alloc-pointer ::mem/pointer)
         idx-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-topk val-ptr idx-ptr matrix k 0 2)  ; dim=0, AF_TOPK_MAX
         ;; Output: [3, 50] - top 3 from each of 50 columns
         top-values (mem/read-pointer val-ptr ::mem/pointer)
         top-indices (mem/read-pointer idx-ptr ::mem/pointer)]
     {:values top-values :indices top-indices})
   ```
   
   Constraints:
   - k must satisfy: 1 ≤ k ≤ 256 and k ≤ input_size[dim]
   - dim must be 0 (other dimensions not yet supported)
   - order must be valid af_topk_function value
   - Input type must be f32, f64, f16, s32, or u32
   
   Special Cases:
   - k = 1: Efficiently computes max or min with index
   - k = n (array size): Returns fully sorted array
   - Empty array (size 1): Returns the single element unchanged
   
   Common Errors:
   - AF_ERR_SIZE: k exceeds array size along dimension
   - AF_ERR_ARG: k > 256 or k ≤ 0
   - AF_ERR_NOT_SUPPORTED: dim ≠ 0 or unsupported type
   - AF_ERR_TYPE: Complex or unsupported input type
   
   Best Practices:
   - Use k ≤ 16 for best performance
   - For k > array_size/2, use af-sort instead
   - Use stable variants (5, 6) for reproducible results with duplicates
   - For complex arrays: compute magnitude, topk, then index original
   - Remember to release output arrays when done
   
   Notes:
   - Indices are always u32 type regardless of input type
   - Output values maintain input array type
   - Function is optimized for small k (≤ 256)
   - k > 256 limitation may be relaxed in future versions
   - GPU implementation provides significant speedup
   - Non-stable versions may reorder equal values arbitrarily
   
   Returns:
   ArrayFire error code (af_err enum):
   - AF_SUCCESS (0): Operation successful
   - AF_ERR_SIZE: Invalid k relative to array size
   - AF_ERR_ARG: Invalid k value
   - AF_ERR_NOT_SUPPORTED: Invalid dim or type
   - AF_ERR_TYPE: Unsupported input type
   
   See also:
   - af-sort: Full array sorting (use when k > n/2)
   - af-sort-index: Sort with indices (alternative for large k)
   - af-max: Maximum value without index (k=1 alternative)
   - af-min: Minimum value without index (k=1 alternative)
   - af-imax: Maximum value with index (k=1 alternative)
   - af-imin: Minimum value with index (k=1 alternative)
   - af-where: Threshold-based selection"
  "af_topk" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/int ::mem/int] ::mem/int)
