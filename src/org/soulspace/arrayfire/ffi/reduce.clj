(ns org.soulspace.arrayfire.ffi.reduce
  "Bindings for the ArrayFire reduction operations.
   
   Reduction operations aggregate values across dimensions of an array,
   producing output arrays with reduced dimensionality. These are fundamental
   operations in data analysis, statistics, and linear algebra.
   
   Categories of Reductions:
   
   1. **Arithmetic Reductions**:
      - Sum: Σ elements
      - Product: Π elements
      - Operations support NaN handling
   
   2. **Comparison Reductions**:
      - Min: minimum value
      - Max: maximum value  
      - Find both values and indices
   
   3. **Logical Reductions**:
      - All True: AND reduction (all non-zero?)
      - Any True: OR reduction (any non-zero?)
      - Count: count non-zero elements
   
   4. **By-Key Reductions**:
      - Group reduction by key arrays
      - Useful for categorical data
      - All operations available in by-key versions
   
   Mathematical Foundations:
   
   **General Reduction**:
   For array A with dimensions [d₀, d₁, d₂, d₃], reducing along dimension k:
   
   Output[i, :, j, l] = reduce_op{A[i, 0:d_k, j, l]}
   
   Output shape: removes dimension k (or sets it to 1)
   
   **Sum Reduction**:
   sum(A, dim) = Σ A[..., i, ...] for i in dimension dim
   
   Properties:
   - Identity: 0
   - Associative: (a + b) + c = a + (b + c)
   - Commutative: a + b = b + a
   - Complexity: O(N) where N = elements in dimension
   
   **Product Reduction**:
   product(A, dim) = Π A[..., i, ...] for i in dimension dim
   
   Properties:
   - Identity: 1
   - Associative: (a × b) × c = a × (b × c)
   - Commutative: a × b = b × a
   - Complexity: O(N)
   
   **Min/Max Reduction**:
   min(A, dim) = min{A[..., i, ...]} for i in dimension dim
   max(A, dim) = max{A[..., i, ...]} for i in dimension dim
   
   Properties:
   - Idempotent: min(a, a) = a
   - Associative: min(min(a, b), c) = min(a, min(b, c))
   - Commutative: min(a, b) = min(b, a)
   - NaN handling: NaN values ignored by default
   
   **Logical Reductions**:
   all_true(A, dim) = AND{A[..., i, ...] ≠ 0} for i in dim
   any_true(A, dim) = OR{A[..., i, ...] ≠ 0} for i in dim
   count(A, dim) = Σ (A[..., i, ...] ≠ 0 ? 1 : 0) for i in dim
   
   Properties:
   - All true: Returns 1 if all elements non-zero, else 0
   - Any true: Returns 1 if any element non-zero, else 0
   - Count: Returns count of non-zero elements (NaN is non-zero)
   
   **By-Key Reductions**:
   For keys K and values V:
   1. Group consecutive elements with same key
   2. Apply reduction within each group
   3. Output unique keys and reduced values
   
   Example:
   Keys:   [1, 1, 1, 2, 2, 3, 3, 3, 3]
   Values: [a, b, c, d, e, f, g, h, i]
   sum_by_key →
   Keys_out:  [1, 2, 3]
   Values_out: [a+b+c, d+e, f+g+h+i]
   
   Dimension Handling:
   
   **First Non-Singleton Dimension (FNSD)**:
   - Dimension -1: Automatically selects first dimension with size > 1
   - Example: Shape [1, 100, 1, 50] → FNSD = 1 (second dimension)
   
   **Explicit Dimension**:
   - Dimension 0-3: Reduces along specified dimension
   - Output shape: specified dimension becomes 1
   - Example: sum(A[10, 20, 30], dim=1) → Output[10, 1, 30]
   
   **All Reduction**:
   - Reduces entire array to single scalar or single-element array
   - No dimension parameter
   - Returns double/complex values or arrays
   
   NaN Handling:
   
   ArrayFire provides special versions of sum and product that handle NaN:
   - **af-sum-nan**: Replace NaN with specified value before summing
   - **af-product-nan**: Replace NaN with specified value before multiplying
   
   Default behavior:
   - Sum/Product: NaN propagates (result is NaN if any input is NaN)
   - Min/Max: NaN values are ignored
   - All/Any/Count: NaN is treated as non-zero (true)
   
   Use Cases:
   
   1. **Statistics**:
      - Mean: sum(A, dim) / elements(A, dim)
      - Variance: sum((A - mean)², dim) / n
      - Standard deviation: sqrt(variance)
   
   2. **Data Analysis**:
      - Aggregate sales by category (by-key)
      - Find maximum sensor reading
      - Count valid measurements
   
   3. **Linear Algebra**:
      - Matrix norms: sqrt(sum(A²))
      - Trace: sum(diag(A))
      - Row/column statistics
   
   4. **Machine Learning**:
      - Loss functions (sum of errors)
      - Gradient accumulation
      - Batch statistics
      - Softmax normalization
   
   5. **Image Processing**:
      - Histogram calculation (count by bins)
      - Image statistics (mean, variance)
      - Integral images (cumulative sum)
   
   6. **Signal Processing**:
      - Energy: sum(signal²)
      - DC component: sum(signal) / length
      - Peak detection: max(signal)
   
   Performance Considerations:
   
   **GPU Acceleration**:
   - Parallel reduction algorithms (tree reduction)
   - Typical speedup: 10-100× for large arrays
   - Optimized for memory bandwidth
   
   **Complexity**:
   - Time: O(N) where N = number of elements in reduced dimension
   - Space: O(output_size) for result
   - By-key: O(N log N) for sorting keys if not pre-sorted
   
   **Memory Layout**:
   - Column-major order (Fortran-style)
   - Dimension 0 reduction most efficient (contiguous)
   - Dimension 3 reduction less efficient (strided)
   
   **Optimization Tips**:
   - Batch reductions when possible
   - Reduce along innermost dimensions first
   - Use appropriate data types (avoid unnecessary precision)
   - Consider fused operations (reduce + map)
   
   Type Promotion:
   
   Some operations promote types to prevent overflow:
   - **Sum/Product**: Promote integers to wider types
     * u8, s8 → int (s32)
     * u16, s16 → int (s32)
     * u32, s32 → int (s32)
     * u64, s64 → long (s64)
     * f32 → f32
     * f64 → f64
   
   - **Min/Max/Count**: Keep input type
   - **All/Any**: Output boolean (char type)
   
   Indexed Reductions (imin/imax):
   
   Return both the reduced value AND the index where it occurs:
   - af-imin: minimum value and its index
   - af-imax: maximum value and its index
   - Useful for finding peak locations, outlier detection
   
   Example Applications:
   
   **Column-wise statistics** (reduce dim 0):
   ```clojure
   ;; Compute mean of each column
   (let [data (create-array values [1000 50])  ; 1000 rows, 50 features
         col-sums (af-sum data 0)              ; Sum each column
         col-means (div col-sums 1000)]        ; Divide by row count
     col-means)  ; Shape [1 50]
   ```
   
   **Row-wise statistics** (reduce dim 1):
   ```clojure
   ;; Compute sum of each row
   (let [data (create-array values [100 200])  ; 100 samples, 200 features
         row-sums (af-sum data 1)]             ; Sum each row
     row-sums)  ; Shape [100 1]
   ```
   
   **Global statistics** (reduce all):
   ```clojure
   ;; Compute total sum
   (let [data (create-array values [100 200 50])
         total-ptr (mem/alloc-double)
         _ (af-sum-all total-ptr nil data)]
     (mem/read-double total-ptr))  ; Single scalar value
   ```
   
   **Find peaks**:
   ```clojure
   ;; Find maximum and its location
   (let [signal (create-array data [10000])
         max-val-ptr (mem/alloc-pointer ::mem/pointer)
         max-idx-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-imax max-val-ptr max-idx-ptr signal 0)
         max-val (mem/read-pointer max-val-ptr ::mem/pointer)
         max-idx (mem/read-pointer max-idx-ptr ::mem/pointer)]
     {:value max-val :index max-idx})
   ```
   
   **Categorical aggregation**:
   ```clojure
   ;; Sum sales by category
   (let [categories (create-array [1 1 2 2 2 3] [6])  ; Category IDs
         sales (create-array [10 20 30 40 50 60] [6]) ; Sales values
         keys-out (mem/alloc-pointer ::mem/pointer)
         vals-out (mem/alloc-pointer ::mem/pointer)
         _ (af-sum-by-key keys-out vals-out categories sales 0)]
     {:categories (mem/read-pointer keys-out ::mem/pointer)
      :totals (mem/read-pointer vals-out ::mem/pointer)})
   ; => {:categories [1 2 3], :totals [30 120 60]}
   ```
   
   **NaN handling**:
   ```clojure
   ;; Sum with NaN replacement
   (let [data-with-nans (create-array [1.0 2.0 NaN 4.0 NaN] [5])
         sum-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-sum-nan sum-ptr data-with-nans 0 0.0)]  ; Replace NaN with 0
     (mem/read-pointer sum-ptr ::mem/pointer))  ; Result: 7.0
   ```
   
   **Validation checks**:
   ```clojure
   ;; Check if all values are positive
   (let [data (create-array values [100 200])
         positive-check (gt data 0)              ; Boolean array
         all-positive-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-all-true all-positive-ptr positive-check 0)]
     (if (= 1 (array-to-scalar all-positive-ptr))
       :all-positive
       :some-negative))
   ```
   
   Best Practices:
   
   1. **Choose appropriate dimensions**:
      - Understand data layout (row-major vs column-major)
      - Reduce along dimensions that make semantic sense
      - Consider computational efficiency
   
   2. **Handle NaN values**:
      - Use -nan versions when data may contain NaN
      - Choose appropriate replacement values
      - Document NaN handling strategy
   
   3. **Type awareness**:
      - Be aware of type promotion for sum/product
      - Use appropriate precision for accumulation
      - Avoid unnecessary type conversions
   
   4. **Memory efficiency**:
      - Reuse output arrays when possible
      - Consider in-place operations for large data
      - Free arrays promptly after use
   
   5. **Numerical stability**:
      - For large sums, consider Kahan summation
      - For products, consider log-space computation
      - Be aware of overflow/underflow risks
   
   6. **Validation**:
      - Check dimensions before reduction
      - Validate key arrays for by-key operations
      - Handle edge cases (empty arrays, all-NaN)
   
   Common Pitfalls:
   
   - **Wrong dimension**: Reducing along wrong axis
   - **NaN propagation**: Forgetting NaN handling
   - **Type overflow**: Integer overflow in sum/product
   - **Key mismatch**: Keys and values different sizes (by-key)
   - **Memory leaks**: Not releasing result arrays
   - **Dimension confusion**: -1 vs explicit dimension
   
   See Also:
   - Scan operations (af-accum): Cumulative versions
   - ireduce operations: Indexed reductions (return indices)
   - Statistical functions: mean, var, stdev (build on reductions)
   - Matrix operations: matmul, dot (specialized reductions)"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; Basic reduction operations

;; af_err af_sum(af_array *out, const af_array in, const int dim)
(defcfn af-sum
  "Sum array elements along a dimension.
   
   Computes the sum of elements along the specified dimension, reducing
   that dimension to size 1 while preserving other dimensions.
   
   Parameters:
   - out: out pointer for result array
   - in: input array
   - dim: dimension to reduce along (0-3, or -1 for first non-singleton)
     * -1: First non-singleton dimension (automatic)
     * 0: Sum along first dimension (column-wise for matrices)
     * 1: Sum along second dimension (row-wise for matrices)
     * 2: Sum along third dimension
     * 3: Sum along fourth dimension
   
   Behavior:
   - For each position in output, sums all values along specified dimension
   - Output shape: input shape with dimension 'dim' set to 1
   - Empty dimension: Returns input unchanged
   - NaN: Propagates (result is NaN if any element is NaN)
   
   Type Promotion:
   - Integer types promoted to avoid overflow:
     * u8, s8, u16, s16, u32, s32 → s32
     * u64, s64 → s64
   - Floating types unchanged: f32 → f32, f64 → f64
   - Complex types unchanged: c32 → c32, c64 → c64
   
   Example (Column sum):
   ```clojure
   ;; Sum columns (reduce dimension 0)
   (let [A (create-array [1 2 3
                          4 5 6] [2 3])  ; 2×3 matrix
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-sum out-ptr A 0)]
     (mem/read-pointer out-ptr ::mem/pointer))
   ; Result: [5 7 9] (shape [1 3])
   ```
   
   Example (Row sum):
   ```clojure
   ;; Sum rows (reduce dimension 1)
   (let [A (create-array [1 2 3
                          4 5 6] [2 3])  ; 2×3 matrix
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-sum out-ptr A 1)]
     (mem/read-pointer out-ptr ::mem/pointer))
   ; Result: [6 15] (shape [2 1])
   ```
   
   Applications:
   - Statistics: Computing totals, means
   - Linear algebra: Vector/matrix norms
   - Data analysis: Aggregating measurements
   - Machine learning: Loss function accumulation
   
   Returns:
   ArrayFire error code (af_err)
   
   See also:
   - af-sum-nan: Sum with NaN replacement
   - af-sum-all: Sum all elements to scalar
   - af-product: Multiply elements along dimension"
  "af_sum" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_sum_nan(af_array *out, const af_array in, const int dim, const double nanval)
(defcfn af-sum-nan
  "Sum array elements with NaN replacement.
   
   Like af-sum but replaces NaN values with specified value before summing.
   Useful for datasets with missing or invalid values represented as NaN.
   
   Parameters:
   - out: out pointer for result array
   - in: input array
   - dim: dimension to reduce (0-3 or -1)
   - nanval: value to replace NaN with (typically 0.0)
   
   Behavior:
   1. Replace all NaN values with nanval
   2. Perform sum reduction along dimension
   3. Return result
   
   Common nanval choices:
   - 0.0: Ignore NaN (treat as zero)
   - Mean/median: Imputation strategy
   - Large negative: Mark as invalid but include
   
   Example:
   ```clojure
   ;; Sum with NaN treated as zero
   (let [data (create-array [1.0 NaN 3.0 NaN 5.0] [5])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-sum-nan out-ptr data 0 0.0)]
     (mem/read-pointer out-ptr ::mem/pointer))
   ; Result: 9.0 (1+0+3+0+5)
   ```
   
   Returns:
   ArrayFire error code (af_err)"
  "af_sum_nan" [::mem/pointer ::mem/pointer ::mem/int ::mem/double] ::mem/int)

;; af_err af_product(af_array *out, const af_array in, const int dim)
(defcfn af-product
  "Multiply array elements along a dimension.
   
   Computes the product of elements along the specified dimension.
   
   Parameters:
   - out: out pointer for result array
   - in: input array
   - dim: dimension to reduce (0-3 or -1)
   
   Behavior:
   - Multiplies all elements along dimension
   - NaN: Propagates (result is NaN if any element is NaN)
   - Zero: Result is zero if any element is zero
   
   Type Promotion: Same as af-sum
   
   Example:
   ```clojure
   ;; Product of each column
   (let [A (create-array [1 2 3
                          4 5 6] [2 3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-product out-ptr A 0)]
     (mem/read-pointer out-ptr ::mem/pointer))
   ; Result: [4 10 18] (1*4, 2*5, 3*6)
   ```
   
   Applications:
   - Probability: Joint probabilities
   - Geometry: Volume calculations
   - Statistics: Geometric mean (via log)
   
   Returns:
   ArrayFire error code (af_err)"
  "af_product" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_product_nan(af_array *out, const af_array in, const int dim, const double nanval)
(defcfn af-product-nan
  "Multiply elements with NaN replacement.
   
   Like af-product but replaces NaN values before multiplying.
   
   Parameters:
   - out: out pointer for result
   - in: input array
   - dim: dimension to reduce
   - nanval: replacement for NaN (typically 1.0 for product)
   
   Returns:
   ArrayFire error code (af_err)"
  "af_product_nan" [::mem/pointer ::mem/pointer ::mem/int ::mem/double] ::mem/int)

;; af_err af_min(af_array *out, const af_array in, const int dim)
(defcfn af-min
  "Find minimum value along a dimension.
   
   Returns the minimum value along the specified dimension.
   NaN values are ignored.
   
   Parameters:
   - out: out pointer for minimum values
   - in: input array
   - dim: dimension to reduce (0-3 or -1)
   
   Type Support:
   - All numeric types: f32, f64, c32, c64, integers
   - Complex: Comparison by magnitude
   - Boolean: 0 (false) < 1 (true)
   
   NaN Handling:
   - NaN values are ignored (not considered for minimum)
   - If all values are NaN, result is NaN
   
   Example:
   ```clojure
   ;; Find minimum in each column
   (let [A (create-array [3 1 4
                          1 5 9] [2 3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-min out-ptr A 0)]
     (mem/read-pointer out-ptr ::mem/pointer))
   ; Result: [1 1 4]
   ```
   
   Applications:
   - Data analysis: Finding minimum values
   - Optimization: Tracking best values
   - Image processing: Min pooling
   - Signal processing: Envelope detection
   
   Returns:
   ArrayFire error code (af_err)
   
   See also:
   - af-imin: Returns minimum AND index
   - af-max: Maximum value"
  "af_min" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_max(af_array *out, const af_array in, const int dim)
(defcfn af-max
  "Find maximum value along a dimension.
   
   Returns the maximum value along the specified dimension.
   NaN values are ignored.
   
   Parameters:
   - out: out pointer for maximum values
   - in: input array
   - dim: dimension to reduce (0-3 or -1)
   
   Type Support: Same as af-min
   NaN Handling: Same as af-min (ignored)
   
   Example:
   ```clojure
   ;; Find maximum in each row
   (let [A (create-array [3 1 4
                          1 5 9] [2 3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-max out-ptr A 1)]
     (mem/read-pointer out-ptr ::mem/pointer))
   ; Result: [4 9]
   ```
   
   Applications:
   - Peak detection
   - Max pooling (CNNs)
   - Range calculation
   - Outlier detection
   
   Returns:
   ArrayFire error code (af_err)
   
   See also:
   - af-imax: Returns maximum AND index"
  "af_max" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_all_true(af_array *out, const af_array in, const int dim)
(defcfn af-all-true
  "Check if all elements are true (non-zero) along dimension.
   
   Performs logical AND reduction. Returns 1 (true) if all elements
   in the dimension are non-zero, 0 (false) otherwise.
   
   Parameters:
   - out: out pointer for boolean result array
   - in: input array (any numeric type)
   - dim: dimension to reduce (0-3 or -1)
   
   Behavior:
   - Non-zero values treated as true
   - Zero values treated as false
   - NaN is non-zero (treated as true)
   - Result type: char (boolean)
   
   Example:
   ```clojure
   ;; Check if all columns have only positive values
   (let [A (create-array [1 2 3
                          4 5 6] [2 3])
         positive (gt A 0)               ; Boolean array
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-all-true out-ptr positive 0)]
     (mem/read-pointer out-ptr ::mem/pointer))
   ; Result: [1 1 1] (all true for each column)
   ```
   
   Applications:
   - Validation: Check constraints
   - Data quality: Verify no missing values
   - Mask operations: Combined conditions
   - Testing: Assert all values meet criteria
   
   Returns:
   ArrayFire error code (af_err)
   
   See also:
   - af-any-true: OR reduction
   - af-count: Count true values"
  "af_all_true" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_any_true(af_array *out, const af_array in, const int dim)
(defcfn af-any-true
  "Check if any element is true (non-zero) along dimension.
   
   Performs logical OR reduction. Returns 1 (true) if at least one
   element in the dimension is non-zero, 0 (false) otherwise.
   
   Parameters:
   - out: out pointer for boolean result
   - in: input array
   - dim: dimension to reduce (0-3 or -1)
   
   Behavior: Same as af-all-true but with OR logic
   
   Example:
   ```clojure
   ;; Check if any negative values in each row
   (let [A (create-array [1 -2 3
                          4 5 6] [2 3])
         negative (lt A 0)
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-any-true out-ptr negative 1)]
     (mem/read-pointer out-ptr ::mem/pointer))
   ; Result: [1 0] (first row has negative, second doesn't)
   ```
   
   Applications:
   - Outlier detection: Any values outside range?
   - Missing data: Any NaN values?
   - Condition checking: Any violations?
   
   Returns:
   ArrayFire error code (af_err)"
  "af_any_true" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_count(af_array *out, const af_array in, const int dim)
(defcfn af-count
  "Count non-zero elements along dimension.
   
   Counts the number of non-zero (true) elements along the dimension.
   NaN is considered non-zero.
   
   Parameters:
   - out: out pointer for count array (unsigned int type)
   - in: input array
   - dim: dimension to reduce (0-3 or -1)
   
   Output type: unsigned int (u32)
   
   Example:
   ```clojure
   ;; Count non-zero values in each column
   (let [A (create-array [0 1 2
                          3 0 4] [2 3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-count out-ptr A 0)]
     (mem/read-pointer out-ptr ::mem/pointer))
   ; Result: [1 1 2] (counts per column)
   ```
   
   Applications:
   - Sparsity analysis: Count non-zero elements
   - Data quality: Count valid measurements
   - Histograms: Count occurrences (with masks)
   - Statistics: Sample sizes per group
   
   Returns:
   ArrayFire error code (af_err)"
  "af_count" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; All-reduction functions (reduce entire array to scalar)

;; af_err af_sum_all(double *real, double *imag, const af_array in)
(defcfn af-sum-all
  "Sum all elements in array to scalar.
   
   Reduces entire array to a single sum value, returned as double precision.
   
   Parameters:
   - real: out pointer to double (real part or full value)
   - imag: out pointer to double (imaginary part, can be NULL)
   - in: input array
   
   Return Values:
   - For real types: *real = sum, *imag = 0 (if provided)
   - For complex types: *real = real part sum, *imag = imag part sum
   
   Type Handling:
   - All types converted to double for result
   - Maintains precision for large sums
   
   Example:
   ```clojure
   ;; Sum entire array
   (let [A (create-array [1 2 3 4 5] [5])
         real-ptr (mem/alloc-double)
         _ (af-sum-all real-ptr nil A)]
     (mem/read-double real-ptr))
   ; Result: 15.0
   ```
   
   Applications:
   - Global statistics: Total sum
   - Loss functions: Total error
   - Norms: L1 norm = sum(abs(x))
   
   Returns:
   ArrayFire error code (af_err)"
  "af_sum_all" [::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_sum_nan_all(double *real, double *imag, const af_array in, const double nanval)
(defcfn af-sum-nan-all
  "Sum all elements with NaN replacement.
   
   Parameters:
   - real: out double for sum
   - imag: out double for imag part (can be NULL)
   - in: input array
   - nanval: replacement value for NaN
   
   Returns:
   ArrayFire error code (af_err)"
  "af_sum_nan_all" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/double] ::mem/int)

;; af_err af_product_all(double *real, double *imag, const af_array in)
(defcfn af-product-all
  "Multiply all elements in array to scalar.
   
   Parameters:
   - real: out double for product
   - imag: out double for imag part (can be NULL)
   - in: input array
   
   Returns:
   ArrayFire error code (af_err)"
  "af_product_all" [::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_product_nan_all(double *real, double *imag, const af_array in, const double nanval)
(defcfn af-product-nan-all
  "Multiply all elements with NaN replacement.
   
   Parameters:
   - real: out double for product
   - imag: out double for imag part (can be NULL)
   - in: input array
   - nanval: replacement value for NaN (typically 1.0)
   
   Returns:
   ArrayFire error code (af_err)"
  "af_product_nan_all" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/double] ::mem/int)

;; af_err af_min_all(double *real, double *imag, const af_array in)
(defcfn af-min-all
  "Find minimum value in entire array.
   
   Parameters:
   - real: out double for minimum value
   - imag: out double for imag part (can be NULL)
   - in: input array
   
   Returns:
   ArrayFire error code (af_err)"
  "af_min_all" [::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_max_all(double *real, double *imag, const af_array in)
(defcfn af-max-all
  "Find maximum value in entire array.
   
   Parameters:
   - real: out double for maximum value
   - imag: out double for imag part (can be NULL)
   - in: input array
   
   Returns:
   ArrayFire error code (af_err)"
  "af_max_all" [::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_all_true_all(double *real, double *imag, const af_array in)
(defcfn af-all-true-all
  "Check if all elements in array are true.
   
   Parameters:
   - real: out double (1.0 if all true, 0.0 otherwise)
   - imag: out double (unused, can be NULL)
   - in: input array
   
   Returns:
   ArrayFire error code (af_err)"
  "af_all_true_all" [::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_any_true_all(double *real, double *imag, const af_array in)
(defcfn af-any-true-all
  "Check if any element in array is true.
   
   Parameters:
   - real: out double (1.0 if any true, 0.0 otherwise)
   - imag: out double (unused, can be NULL)
   - in: input array
   
   Returns:
   ArrayFire error code (af_err)"
  "af_any_true_all" [::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_count_all(double *real, double *imag, const af_array in)
(defcfn af-count-all
  "Count all non-zero elements in array.
   
   Parameters:
   - real: out double for count
   - imag: out double (unused, can be NULL)
   - in: input array
   
   Returns:
   ArrayFire error code (af_err)"
  "af_count_all" [::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; By-key reduction functions

;; af_err af_sum_by_key(af_array *keys_out, af_array *vals_out, const af_array keys, const af_array vals, const int dim)
(defcfn af-sum-by-key
  "Sum values grouped by keys.
   
   Groups consecutive elements with the same key and sums values within
   each group. Useful for categorical data aggregation.
   
   Parameters:
   - keys-out: out pointer for unique keys array
   - vals-out: out pointer for summed values array
   - keys: input keys array (determines grouping)
   - vals: input values array (to be summed)
   - dim: dimension along which to reduce (usually 0)
   
   Behavior:
   1. Identify consecutive elements with same key
   2. Sum values within each group
   3. Output unique keys and corresponding sums
   
   Requirements:
   - Keys and vals must have same shape
   - Keys should be sorted for meaningful grouping
   - Integer keys recommended (int or uint)
   
   Example:
   ```clojure
   ;; Sum sales by product category
   (let [categories (create-array [1 1 1 2 2 3] [6])  ; Product categories
         sales (create-array [10 20 30 40 50 60] [6]) ; Sales amounts
         keys-out (mem/alloc-pointer ::mem/pointer)
         vals-out (mem/alloc-pointer ::mem/pointer)
         _ (af-sum-by-key keys-out vals-out categories sales 0)]
     {:categories (mem/read-pointer keys-out ::mem/pointer)  ; [1 2 3]
      :totals (mem/read-pointer vals-out ::mem/pointer)})    ; [60 90 60]
   ```
   
   Applications:
   - Data analysis: Group-by aggregations
   - Statistics: Conditional statistics
   - Databases: GROUP BY operations
   - Time series: Aggregate by time bins
   
   Returns:
   ArrayFire error code (af_err)
   
   See also:
   - af-sum-by-key-nan: With NaN handling
   - af-product-by-key: Product aggregation
   - af-min-by-key: Minimum per group
   - af-max-by-key: Maximum per group"
  "af_sum_by_key" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_sum_by_key_nan(af_array *keys_out, af_array *vals_out, const af_array keys, const af_array vals, const int dim, const double nanval)
(defcfn af-sum-by-key-nan
  "Sum values by key with NaN replacement.
   
   Parameters:
   - keys-out: out pointer for unique keys
   - vals-out: out pointer for summed values
   - keys: input keys array
   - vals: input values array
   - dim: dimension to reduce
   - nanval: replacement for NaN values
   
   Returns:
   ArrayFire error code (af_err)"
  "af_sum_by_key_nan" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/double] ::mem/int)

;; af_err af_product_by_key(af_array *keys_out, af_array *vals_out, const af_array keys, const af_array vals, const int dim)
(defcfn af-product-by-key
  "Multiply values grouped by keys.
   
   Parameters:
   - keys-out: out pointer for unique keys
   - vals-out: out pointer for product values
   - keys: input keys array
   - vals: input values array
   - dim: dimension to reduce
   
   Returns:
   ArrayFire error code (af_err)"
  "af_product_by_key" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_product_by_key_nan(af_array *keys_out, af_array *vals_out, const af_array keys, const af_array vals, const int dim, const double nanval)
(defcfn af-product-by-key-nan
  "Multiply values by key with NaN replacement.
   
   Parameters:
   - keys-out: out pointer for unique keys
   - vals-out: out pointer for product values
   - keys: input keys array
   - vals: input values array
   - dim: dimension to reduce
   - nanval: replacement for NaN (typically 1.0)
   
   Returns:
   ArrayFire error code (af_err)"
  "af_product_by_key_nan" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/double] ::mem/int)

;; af_err af_min_by_key(af_array *keys_out, af_array *vals_out, const af_array keys, const af_array vals, const int dim)
(defcfn af-min-by-key
  "Find minimum value per key group.
   
   Parameters:
   - keys-out: out pointer for unique keys
   - vals-out: out pointer for minimum values per group
   - keys: input keys array
   - vals: input values array
   - dim: dimension to reduce
   
   Returns:
   ArrayFire error code (af_err)"
  "af_min_by_key" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_max_by_key(af_array *keys_out, af_array *vals_out, const af_array keys, const af_array vals, const int dim)
(defcfn af-max-by-key
  "Find maximum value per key group.
   
   Parameters:
   - keys-out: out pointer for unique keys
   - vals-out: out pointer for maximum values per group
   - keys: input keys array
   - vals: input values array
   - dim: dimension to reduce
   
   Returns:
   ArrayFire error code (af_err)"
  "af_max_by_key" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_all_true_by_key(af_array *keys_out, af_array *vals_out, const af_array keys, const af_array vals, const int dim)
(defcfn af-all-true-by-key
  "Check if all values are true per key group.
   
   Parameters:
   - keys-out: out pointer for unique keys
   - vals-out: out pointer for boolean results per group
   - keys: input keys array
   - vals: input values array (checked for non-zero)
   - dim: dimension to reduce
   
   Returns:
   ArrayFire error code (af_err)"
  "af_all_true_by_key" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_any_true_by_key(af_array *keys_out, af_array *vals_out, const af_array keys, const af_array vals, const int dim)
(defcfn af-any-true-by-key
  "Check if any value is true per key group.
   
   Parameters:
   - keys-out: out pointer for unique keys
   - vals-out: out pointer for boolean results per group
   - keys: input keys array
   - vals: input values array (checked for non-zero)
   - dim: dimension to reduce
   
   Returns:
   ArrayFire error code (af_err)"
  "af_any_true_by_key" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_count_by_key(af_array *keys_out, af_array *vals_out, const af_array keys, const af_array vals, const int dim)
(defcfn af-count-by-key
  "Count non-zero values per key group.
   
   Parameters:
   - keys-out: out pointer for unique keys
   - vals-out: out pointer for counts per group
   - keys: input keys array
   - vals: input values array (non-zero counted)
   - dim: dimension to reduce
   
   Returns:
   ArrayFire error code (af_err)"
  "af_count_by_key" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)
