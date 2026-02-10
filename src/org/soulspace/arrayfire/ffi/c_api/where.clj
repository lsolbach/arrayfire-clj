(ns org.soulspace.arrayfire.ffi.c-api.where
  "Bindings for the ArrayFire where function.
   
   The where function locates indices of non-zero elements in an array,
   providing a fundamental operation for conditional indexing, filtering,
   and masked operations in array programming.
   
   Mathematical Foundation:
   
   Given an array A and a condition, where returns the linear indices
   of all elements satisfying the condition (non-zero values).
   
   **Linear Indexing**:
   For a multi-dimensional array with dimensions [d₀, d₁, d₂, d₃],
   the linear index of element at position (i, j, k, l) is:
   
   linear_idx = i + j*d₀ + k*d₀*d₁ + l*d₀*d₁*d₂
   
   **Result Array**:
   - Output is a 1D array of type u32 (unsigned 32-bit integers)
   - Length equals the number of non-zero elements
   - Indices are in row-major (C-style) order
   - Empty array if no non-zero elements found
   
   Key Concepts:
   
   1. **Non-Zero Definition**:
      - Numeric types: Any value != 0
      - Boolean (b8): true (non-zero) values
      - Complex (c32, c64): |z| != 0 (magnitude non-zero)
      - Floating point: Careful with == 0 due to precision
   
   2. **Conditional Indexing**:
      ```
      A = [1, 0, 3, 0, 5]
      where(A) = [0, 2, 4]  // Indices of 1, 3, 5
      ```
   
   3. **Boolean Masks**:
      ```
      A = [1, 2, 3, 4, 5]
      mask = A > 2        // [false, false, true, true, true]
      indices = where(mask)  // [2, 3, 4]
      selected = A[indices]  // [3, 4, 5]
      ```
   
   4. **Sparse Representation**:
      - where provides coordinate list (COO) format
      - Efficient for sparse arrays (mostly zeros)
      - Basis for sparse matrix operations
   
   Common Patterns:
   
   **Pattern 1: Filter by Condition**
   ```clojure
   ;; Select elements > threshold
   (let [data (create-array [1 5 2 8 3 9 4])
         threshold 4
         mask (af-gt data threshold)  ; [false true false true false true false]
         indices-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-where indices-ptr mask)
         indices (mem/read-pointer indices-ptr ::mem/pointer)]
     ;; indices = [1, 3, 5] (positions of 5, 8, 9)
     (af-index data indices))  ; Returns [5, 8, 9]
   ```
   
   **Pattern 2: Find Specific Values**
   ```clojure
   ;; Find all zeros in array
   (let [data (create-array [1 0 2 0 3 0 4])
         is-zero (af-eq data 0)
         indices-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-where indices-ptr is-zero)
         zero-indices (mem/read-pointer indices-ptr ::mem/pointer)]
     ;; zero-indices = [1, 3, 5]
     zero-indices)
   ```
   
   **Pattern 3: 2D Coordinate Finding**
   ```clojure
   ;; Find (row, col) of elements > threshold
   (let [matrix (create-array data [10 10])
         mask (af-gt matrix threshold)
         linear-idx-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-where linear-idx-ptr mask)
         linear-indices (mem/read-pointer linear-idx-ptr ::mem/pointer)
         ;; Convert linear to 2D coordinates
         [h w] [10 10]
         rows (af-mod linear-indices h)
         cols (af-div linear-indices h)]
     {:rows rows :cols cols :linear-indices linear-indices})
   ```
   
   **Pattern 4: Sparse Matrix Construction**
   ```clojure
   ;; Extract non-zero elements and their positions
   (let [sparse-data (create-array data [1000 1000])  ; Mostly zeros
         nz-idx-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-where nz-idx-ptr sparse-data)
         nz-indices (mem/read-pointer nz-idx-ptr ::mem/pointer)
         nz-values (af-index sparse-data nz-indices)
         n-nonzero (af-get-elements nz-indices)]
     {:nnz n-nonzero
      :indices nz-indices
      :values nz-values})
   ```
   
   **Pattern 5: Multi-Condition Filtering**
   ```clojure
   ;; Find elements in range [lower, upper]
   (let [data (create-array [1 2 3 4 5 6 7 8 9])
         lower 3
         upper 7
         mask-lower (af-ge data lower)  ; data >= lower
         mask-upper (af-le data upper)  ; data <= upper
         combined-mask (af-and mask-lower mask-upper)
         indices-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-where indices-ptr combined-mask)
         indices (mem/read-pointer indices-ptr ::mem/pointer)]
     (af-index data indices))  ; Returns [3, 4, 5, 6, 7]
   ```
   
   **Pattern 6: NaN/Inf Detection**
   ```clojure
   ;; Find NaN values in floating-point array
   (let [data (create-array [1.0 NaN 2.0 Inf 3.0 NaN])
         is-nan (af-isnan data)
         nan-idx-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-where nan-idx-ptr is-nan)
         nan-indices (mem/read-pointer nan-idx-ptr ::mem/pointer)]
     ;; Replace NaNs with zeros
     (af-replace data nan-indices 0.0))
   ```
   
   Performance Considerations:
   
   - **Algorithm**: Parallel prefix sum (scan) to count/locate non-zeros
   - **Complexity**: O(N) where N = total array elements
   - **GPU Acceleration**: Highly parallel, 10-100× faster than CPU
   - **Memory**: Output size proportional to number of non-zeros
   - **Sparse Data**: Very efficient (output << input size)
   - **Dense Data**: Less efficient (output ≈ input size)
   
   Optimization Tips:
   
   1. **Reuse Masks**: Compute mask once, use multiple times
   2. **Combine Conditions**: Use logical ops before where
   3. **Check Count First**: Use af-count to estimate output size
   4. **Stream Operations**: Chain where with other operations
   
   Applications:
   
   1. **Data Filtering**:
      - Select elements meeting criteria
      - Remove outliers
      - Extract regions of interest
   
   2. **Sparse Matrix Operations**:
      - COO (Coordinate) format conversion
      - Sparse-dense operations
      - Graph algorithms
   
   3. **Image Processing**:
      - Segmentation (find pixels in range)
      - Object detection (locate features)
      - Binary morphology operations
   
   4. **Signal Processing**:
      - Peak detection (find local maxima)
      - Threshold-based filtering
      - Event detection
   
   5. **Scientific Computing**:
      - Conditional statistics
      - Masked operations
      - Data validation
   
   6. **Machine Learning**:
      - Sample selection
      - Feature filtering
      - Outlier removal
   
   Use Cases with Examples:
   
   **1. Outlier Detection**:
   ```clojure
   (defn detect-outliers [data n-std-dev]
     (let [mean (af-mean-all data)
           std (af-stdev-all data)
           lower-bound (- mean (* n-std-dev std))
           upper-bound (+ mean (* n-std-dev std))
           too-low (af-lt data lower-bound)
           too-high (af-gt data upper-bound)
           is-outlier (af-or too-low too-high)
           outlier-idx-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-where outlier-idx-ptr is-outlier)]
       (mem/read-pointer outlier-idx-ptr ::mem/pointer)))
   ```
   
   **2. Image Segmentation**:
   ```clojure
   (defn segment-by-intensity [image lower upper]
     (let [in-range-lower (af-ge image lower)
           in-range-upper (af-le image upper)
           in-range (af-and in-range-lower in-range-upper)
           pixel-idx-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-where pixel-idx-ptr in-range)
           pixel-indices (mem/read-pointer pixel-idx-ptr ::mem/pointer)
           [h w] (af-get-dims image)
           y-coords (af-mod pixel-indices w)
           x-coords (af-div pixel-indices w)]
       {:indices pixel-indices
        :coordinates {:x x-coords :y y-coords}
        :count (af-get-elements pixel-indices)}))
   ```
   
   **3. Peak Finding**:
   ```clojure
   (defn find-local-maxima [signal]
     (let [n (af-get-elements signal)
           left-higher (af-gt (af-index signal (range 1 n))
                             (af-index signal (range 0 (dec n))))
           right-higher (af-gt (af-index signal (range 0 (dec n)))
                              (af-index signal (range 1 n)))
           is-peak (af-and left-higher right-higher)
           peak-idx-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-where peak-idx-ptr is-peak)]
       (af-add (mem/read-pointer peak-idx-ptr ::mem/pointer) 1)))  ; +1 offset
   ```
   
   **4. Sparse Matrix From Dense**:
   ```clojure
   (defn dense-to-sparse [matrix tolerance]
     \"Convert dense matrix to sparse representation.\"
     (let [abs-vals (af-abs matrix)
           significant (af-gt abs-vals tolerance)
           idx-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-where idx-ptr significant)
           linear-indices (mem/read-pointer idx-ptr ::mem/pointer)
           values (af-index matrix linear-indices)
           [rows cols] (af-get-dims matrix)
           row-indices (af-mod linear-indices rows)
           col-indices (af-div linear-indices rows)]
       {:format :coo
        :shape [rows cols]
        :nnz (af-get-elements linear-indices)
        :row-indices row-indices
        :col-indices col-indices
        :values values}))
   ```
   
   **5. Conditional Replacement**:
   ```clojure
   (defn replace-conditional [array condition replacement-value]
     \"Replace elements where condition is true.\"
     (let [mask (condition array)  ; e.g., #(af-lt % 0) for negatives
           idx-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-where idx-ptr mask)
           indices (mem/read-pointer idx-ptr ::mem/pointer)
           result (af-copy array)]
       (af-set-elements result indices replacement-value)
       result))
   
   ;; Usage: Replace negative values with zero
   (replace-conditional data #(af-lt % 0) 0.0)
   ```
   
   Comparison with Other Languages:
   
   **NumPy**:
   ```python
   # NumPy
   indices = np.where(array > threshold)[0]
   
   # ArrayFire
   (af-where indices-ptr (af-gt array threshold))
   ```
   
   **MATLAB**:
   ```matlab
   % MATLAB
   indices = find(array > threshold);
   
   % ArrayFire
   (af-where indices-ptr (af-gt array threshold))
   ```
   
   **R**:
   ```r
   # R
   indices <- which(array > threshold)
   
   # ArrayFire
   (af-where indices-ptr (af-gt array threshold))
   ```
   
   Type Support:
   
   - **Input**: All types (f32, f64, c32, c64, s32, u32, s64, u64, s16, u16, s8, u8, b8)
   - **Output**: Always u32 (unsigned 32-bit integers)
   - **Complex**: Non-zero if |real| > 0 or |imag| > 0
   - **Boolean**: Non-zero if true
   
   Edge Cases:
   
   1. **All Zeros**: Returns empty array (size 0)
   2. **No Zeros**: Returns all indices [0, 1, 2, ..., N-1]
   3. **Empty Input**: Returns empty array
   4. **Large Output**: May exceed memory if too many non-zeros
   
   Limitations:
   
   - Maximum array size: 2³² elements (u32 index limit)
   - For larger arrays, requires 64-bit indexing (not standard in ArrayFire)
   - Output is 1D (linear indices only)
   - Multi-dimensional coordinates require conversion
   
   Related Functions:
   
   - af-count: Count non-zero elements (faster if only count needed)
   - af-any: Check if any element is non-zero
   - af-all: Check if all elements are non-zero
   - af-sum: Sum of elements (for counting with numeric interpretation)
   - Comparison ops: af-lt, af-gt, af-eq, af-le, af-ge, af-ne
   - Logical ops: af-and, af-or, af-not
   
   See also:
   - af-index: Index array with indices
   - af-lookup: Lookup values by indices
   - af-replace: Replace elements at indices"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.loader]))

;; Where function (non-zero element location)

;; af_err af_where(af_array *idx, const af_array in)
(defcfn af-where
  "Locate the indices of non-zero elements in an array.
   
   Returns the linear indices of all non-zero elements in the input array.
   This is a fundamental operation for conditional indexing, filtering, and
   sparse array operations.
   
   Parameters:
   - idx: Output pointer for indices array (1D array of type u32)
   - in: Input array (any type, any dimensions)
   
   Behavior:
   - Scans input array for non-zero values
   - Returns linear indices where input is non-zero
   - Output is 1D array of unsigned 32-bit integers (u32)
   - Indices are in row-major (C-style) order
   - Empty array returned if no non-zero elements
   
   Non-Zero Definition:
   - **Numeric types**: value != 0
   - **Boolean (b8)**: true values
   - **Complex (c32, c64)**: |z| != 0 (real² + imag² > 0)
   - **Floating point**: exact comparison with 0.0
   
   Linear Indexing:
   For array with dimensions [d₀, d₁, d₂, d₃],
   linear index of element at (i, j, k, l) is:
   
   idx = i + j*d₀ + k*d₀*d₁ + l*d₀*d₁*d₂
   
   Output Properties:
   - Type: u32 (always)
   - Dimensions: [N] where N = count of non-zeros
   - Sorted: Yes (ascending order)
   - Range: [0, total_elements - 1]
   
   Performance:
   - Algorithm: Parallel prefix sum + compaction
   - Complexity: O(N) where N = array size
   - GPU parallel: Very efficient
   - Memory: Output size = number of non-zeros
   
   Example (Basic usage):
   ```clojure
   ;; Find non-zero elements
   (let [data (create-array [1 0 3 0 5 0 7])
         idx-ptr (mem/alloc-pointer ::mem/pointer)
         err (af-where idx-ptr data)]
     (when (= err AF_SUCCESS)
       (let [indices (mem/read-pointer idx-ptr ::mem/pointer)]
         ;; indices = [0, 2, 4, 6] (positions of 1, 3, 5, 7)
         (println \"Non-zero at indices:\" indices)
         indices)))
   ```
   
   Example (Conditional filtering):
   ```clojure
   ;; Select elements greater than threshold
   (let [data (create-array [1 5 2 8 3 9 4])
         threshold 4
         mask (af-gt data threshold)  ; Boolean mask
         idx-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-where idx-ptr mask)
         indices (mem/read-pointer idx-ptr ::mem/pointer)]
     ;; indices = [1, 3, 5] (positions where data > 4)
     ;; Extract filtered values
     (af-index data indices))  ; Returns [5, 8, 9]
   ```
   
   Example (2D image segmentation):
   ```clojure
   ;; Find pixels in intensity range
   (let [image (load-image \"photo.jpg\")  ; [height, width]
         lower 100
         upper 200
         in-range (af-and (af-ge image lower) (af-le image upper))
         pixel-idx-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-where pixel-idx-ptr in-range)
         linear-indices (mem/read-pointer pixel-idx-ptr ::mem/pointer)
         ;; Convert to 2D coordinates
         [h w] (af-get-dims image)
         rows (af-mod linear-indices w)
         cols (af-div linear-indices w)]
     {:pixel-count (af-get-elements linear-indices)
      :row-coords rows
      :col-coords cols})
   ```
   
   Example (Outlier detection):
   ```clojure
   ;; Find values beyond 3 standard deviations
   (let [data (create-array measurements)
         mean-ptr (mem/alloc ::mem/double)
         std-ptr (mem/alloc ::mem/double)
         _ (af-mean-all mean-ptr data)
         _ (af-stdev-all std-ptr data)
         mean-val (mem/deref mean-ptr ::mem/double)
         std-val (mem/deref std-ptr ::mem/double)
         lower-bound (- mean-val (* 3 std-val))
         upper-bound (+ mean-val (* 3 std-val))
         too-low (af-lt data lower-bound)
         too-high (af-gt data upper-bound)
         is-outlier (af-or too-low too-high)
         outlier-idx-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-where outlier-idx-ptr is-outlier)
         outlier-indices (mem/read-pointer outlier-idx-ptr ::mem/pointer)]
     {:outlier-indices outlier-indices
      :outlier-values (af-index data outlier-indices)
      :count (af-get-elements outlier-indices)})
   ```
   
   Example (Sparse matrix extraction):
   ```clojure
   ;; Extract non-zero elements for sparse storage
   (let [matrix (create-array data [1000 1000])
         nz-idx-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-where nz-idx-ptr matrix)
         linear-indices (mem/read-pointer nz-idx-ptr ::mem/pointer)
         values (af-index matrix linear-indices)
         [rows cols] [1000 1000]
         row-idx (af-mod linear-indices rows)
         col-idx (af-div linear-indices rows)
         nnz (af-get-elements linear-indices)]
     (println (format \"Sparse matrix: %d non-zeros out of %d (%.2f%% sparse)\"
                     nnz (* rows cols) (* 100.0 (- 1.0 (/ nnz (* rows cols))))))
     {:format :coo
      :shape [rows cols]
      :nnz nnz
      :row-indices row-idx
      :col-indices col-idx
      :values values})
   ```
   
   Example (NaN removal):
   ```clojure
   ;; Find and remove NaN values
   (let [data (create-array [1.0 ##NaN 2.0 3.0 ##NaN 4.0])
         is-finite (af-not (af-isnan data))
         valid-idx-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-where valid-idx-ptr is-finite)
         valid-indices (mem/read-pointer valid-idx-ptr ::mem/pointer)
         cleaned-data (af-index data valid-indices)]
     ;; cleaned-data = [1.0, 2.0, 3.0, 4.0]
     cleaned-data)
   ```
   
   Example (Multi-condition selection):
   ```clojure
   ;; Find elements in multiple ranges
   (let [data (create-array [1 2 3 4 5 6 7 8 9 10])
         ;; Select values in [2,4] or [7,9]
         range1 (af-and (af-ge data 2) (af-le data 4))
         range2 (af-and (af-ge data 7) (af-le data 9))
         combined (af-or range1 range2)
         idx-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-where idx-ptr combined)
         indices (mem/read-pointer idx-ptr ::mem/pointer)]
     (af-index data indices))  ; Returns [2, 3, 4, 7, 8, 9]
   ```
   
   Example (Count check before extraction):
   ```clojure
   ;; Efficient check: count before extracting indices
   (let [data (create-array large-array)
         mask (af-gt data threshold)
         count-ptr (mem/alloc-pointer ::mem/long)
         _ (af-count-all count-ptr mask)
         n-matches (mem/deref count-ptr ::mem/long)]
     (if (> n-matches 1000000)
       (println \"Warning: Large result set, consider more restrictive filter\")
       (let [idx-ptr (mem/alloc-pointer ::mem/pointer)
             _ (af-where idx-ptr mask)]
         (mem/read-pointer idx-ptr ::mem/pointer))))
   ```
   
   Use Cases:
   1. **Conditional Indexing**: Select elements meeting criteria
   2. **Sparse Operations**: Extract non-zero coordinates/values
   3. **Data Cleaning**: Locate and handle invalid values
   4. **Segmentation**: Find pixels/elements in target range
   5. **Feature Extraction**: Locate peaks, edges, patterns
   6. **Graph Algorithms**: Find connected components
   
   Common Patterns:
   - Combine with comparison ops: af-gt, af-lt, af-eq
   - Chain with logical ops: af-and, af-or, af-not
   - Use with af-index to extract filtered values
   - Convert linear to multi-D: row = idx % width, col = idx / width
   
   Optimization Tips:
   - Use af-count-all first to check output size
   - Combine conditions before calling where
   - Reuse masks when possible
   - Consider af-any/af-all for simple checks
   
   Type Support:
   - Input: All types supported
   - Output: Always u32 (32-bit unsigned)
   - Maximum index: 2³² - 1 (4,294,967,295)
   
   Edge Cases:
   - Empty input: Returns empty array (size 0)
   - All zeros: Returns empty array
   - All non-zero: Returns [0, 1, 2, ..., N-1]
   - Single element: Returns [0] if non-zero, [] if zero
   
   Returns:
   AF_SUCCESS (0) on success, error code otherwise
   
   See also:
   - af-count: Count non-zero elements
   - af-any: Check if any element is non-zero
   - af-all: Check if all elements are non-zero
   - af-index: Index array using indices
   - af-lookup: Lookup values at indices
   - af-replace: Replace elements at indices"
  "af_where" [::mem/pointer ::mem/pointer] ::mem/int)
