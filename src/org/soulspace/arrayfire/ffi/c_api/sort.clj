(ns org.soulspace.arrayfire.ffi.c-api.sort
  "ArrayFire FFI bindings for sorting operations.

  Sorting is a fundamental operation in data processing and analysis. ArrayFire
  provides GPU-accelerated sorting algorithms that can be applied along any
  dimension of multi-dimensional arrays.

  ## Mathematical Foundation

  Sorting arranges elements in a specified order (ascending or descending)
  according to a comparison function. For array element a[i] and a[j]:
  - Ascending: a[i] ≤ a[i+1] for all valid i
  - Descending: a[i] ≥ a[i+1] for all valid i

  ## Sorting Variants

  1. **Basic Sort (af_sort)**:
     - Sorts array elements along a specified dimension
     - Returns sorted values only
     - Most efficient when only sorted values are needed

  2. **Sort with Indices (af_sort_index)**:
     - Returns both sorted values and original indices
     - Indices track where each sorted element came from
     - Useful for reordering related data or analyzing rank

  3. **Sort by Key (af_sort_by_key)**:
     - Sorts values array according to keys array
     - Both keys and values are reordered together
     - Essential for key-value pair sorting and database operations

  ## Dimension-wise Sorting

  Sorting can be performed along any dimension:
  - dim=0: Sort each column independently (rows within columns)
  - dim=1: Sort each row independently (columns within rows)
  - dim=2: Sort along third dimension (for 3D+ arrays)
  - dim=3: Sort along fourth dimension (for 4D arrays)

  Example for 2D array [3×4]:
  ```
  Original:        Sort dim=0:      Sort dim=1:
  [5 2 8 1]       [1 2 3 1]       [1 2 5 8]
  [1 7 3 9]  -->  [3 4 5 6]  or  [1 3 7 9]
  [3 4 5 6]       [5 7 8 9]       [3 4 5 6]
  ```

  ## Computational Complexity

  - Average case: O(n log n) per dimension
  - Best case: O(n) for already sorted data (some implementations)
  - Worst case: O(n log n) guaranteed
  - Space complexity: O(n) for temporary storage
  - GPU parallelization: Sorts multiple slices simultaneously

  ## Sorting Algorithms

  ArrayFire uses optimized GPU sorting algorithms:
  - Radix sort: For integer types (linear complexity for fixed-width integers)
  - Merge sort: For floating-point types (guaranteed O(n log n))
  - Bitonic sort: For power-of-2 sizes on GPU (highly parallel)

  The specific algorithm is selected automatically based on data type and size.

  ## Stability

  Sorting stability refers to preserving relative order of equal elements:
  - ArrayFire sorting is generally stable for most types
  - Stable sorting maintains original order of equal keys
  - Important for multi-level sorting (sort by secondary key first, then primary)

  ## Supported Types

  All types are supported for sorting:
  - Floating point: f32, f64
  - Complex: c32, c64 (all types for values in sort_by_key)
  - Signed integers: s8, s16, s32, s64
  - Unsigned integers: u8, u16, u32, u64
  - Boolean: b8

  Note: For sort_by_key, keys must be real-valued (no complex keys).

  ## Performance Notes

  - GPU acceleration provides 5-50× speedup over CPU for large arrays
  - Sorting along first dimension (dim=0) is often fastest
  - Batch sorting (higher dimensions) processes slices in parallel
  - Pre-sorted or nearly-sorted data may benefit from specialized algorithms
  - Memory-bound operation: Performance depends on data transfer bandwidth

  ## Applications

  - Data analysis: Ranking, percentiles, order statistics
  - Statistics: Median, quartiles, outlier detection
  - Signal processing: Rank filters, order-statistic filters
  - Database operations: Key-value sorting, indexing
  - Graphics: Z-sorting for rendering
  - Machine learning: k-nearest neighbors, decision trees
  - Scientific computing: Particle sorting, adaptive mesh refinement

  See also:
  - Set operations (af_set_unique, af_set_union, af_set_intersect)
  - Reorder functions for custom permutations"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.loader]))

;;
;; Sorting Functions
;;

(defcfn af-sort
  "Sort an array along a specified dimension.

  Sorts array elements in ascending or descending order along the specified
  dimension. This is the most basic and efficient sorting operation when only
  the sorted values are needed (indices not required).

  Parameters:
  - out: Output array containing sorted values
  - in: Input array to sort
  - dim: Dimension along which to sort (0-3)
  - is-ascending: Sorting order (true=ascending, false=descending)

  Returns:
  Error code indicating success or failure.

  Dimension Behavior:
  - dim=0: Sort within each column (along rows)
  - dim=1: Sort within each row (along columns)
  - dim=2: Sort along 3rd dimension
  - dim=3: Sort along 4th dimension

  Example (1D array):
  ```clojure
  ;; Sort a vector in ascending order
  (let [data (af-constant [10] [9 2 5 1 8 3 7 4 6 0] :s32)
        out (mem/alloc-instance ::mem/pointer)]
    (af-sort out data 0 true) ;; ascending
    ;; Result: [0 1 2 3 4 5 6 7 8 9]
    )
  ```

  Example (2D array, sort columns):
  ```clojure
  ;; Sort each column independently
  (let [data (af-constant [3 4] [[5 2 8 1]
                                  [1 7 3 9]
                                  [3 4 5 6]] :f32)
        out (mem/alloc-instance ::mem/pointer)]
    (af-sort out data 0 true) ;; sort along dim 0 (within columns)
    ;; Result: [[1 2 3 1]
    ;;          [3 4 5 6]
    ;;          [5 7 8 9]]
    )
  ```

  Example (2D array, sort rows):
  ```clojure
  ;; Sort each row independently
  (let [data (af-constant [3 4] [[5 2 8 1]
                                  [1 7 3 9]
                                  [3 4 5 6]] :f32)
        out (mem/alloc-instance ::mem/pointer)]
    (af-sort out data 1 true) ;; sort along dim 1 (within rows)
    ;; Result: [[1 2 5 8]
    ;;          [1 3 7 9]
    ;;          [3 4 5 6]]
    )
  ```

  Example (descending order):
  ```clojure
  ;; Sort in descending order
  (let [data (af-constant [5] [3 1 4 1 5] :f64)
        out (mem/alloc-instance ::mem/pointer)]
    (af-sort out data 0 false) ;; descending
    ;; Result: [5 4 3 1 1]
    )
  ```

  Batch Processing:
  For multi-dimensional arrays, sorting is performed on all slices
  perpendicular to the sort dimension in parallel:
  - Input: [m, n, p, q] sorted along dim=0
  - Performs: n × p × q independent sorts of length m
  - All sorts execute in parallel on GPU

  Type Support:
  All numeric and boolean types:
  - f32, f64: Floating-point
  - c32, c64: Complex (sorted by magnitude, then phase)
  - s8, s16, s32, s64: Signed integers
  - u8, u16, u32, u64: Unsigned integers
  - b8: Boolean (false < true)

  Performance Tips:
  - Sorting along dim=0 is often fastest (memory layout)
  - Use af-sort when indices are not needed (more efficient)
  - GPU provides best speedup for large arrays (>1000 elements)
  - Multiple small sorts benefit from batch parallelism

  Edge Cases:
  - Empty arrays (elements=0): Returns copy of input
  - Single element: Returns input unchanged
  - Already sorted: Still performs full sort (no early exit)

  See also:
  - af_sort (ArrayFire C API)
  - af-sort-index: Sort and return original indices
  - af-sort-by-key: Sort values according to keys"
  "af_sort" [::mem/pointer ::mem/pointer ::mem/int ::mem/int] ::mem/int)

(defcfn af-sort-index
  "Sort an array and return both sorted values and original indices.

  Sorts the input array and simultaneously generates an index array that tracks
  where each sorted element originated. This is essential when you need to apply
  the same reordering to related data or analyze element ranks.

  Parameters:
  - out: Output array containing sorted values
  - indices: Output array containing original indices of sorted elements
  - in: Input array to sort
  - dim: Dimension along which to sort (0-3)
  - is-ascending: Sorting order (true=ascending, false=descending)

  Returns:
  Error code indicating success or failure.

  Index Array:
  The indices array contains the original positions of elements:
  - indices[i] = original position of sorted element i
  - Indices are 32-bit unsigned integers (u32)
  - Can be used with af_lookup or af_index to reorder other arrays

  Example (basic usage):
  ```clojure
  ;; Sort and track original positions
  (let [data (af-constant [5] [30 10 50 20 40] :f32)
        values (mem/alloc-instance ::mem/pointer)
        indices (mem/alloc-instance ::mem/pointer)]
    (af-sort-index values indices data 0 true)
    ;; values: [10 20 30 40 50]
    ;; indices: [1 3 0 4 2]  (0-based positions in original array)
    )
  ```

  Example (reordering related data):
  ```clojure
  ;; Sort scores and reorder student names accordingly
  (let [scores (af-constant [4] [85 92 78 95] :f32)
        names (af-constant [4] [0 1 2 3] :u32) ;; student IDs
        sorted-scores (mem/alloc-instance ::mem/pointer)
        indices (mem/alloc-instance ::mem/pointer)]
    (af-sort-index sorted-scores indices scores 0 false) ;; descending
    ;; sorted-scores: [95 92 85 78]
    ;; indices: [3 1 0 2]
    ;; Use indices to reorder names array with af-lookup
    )
  ```

  Example (finding rank):
  ```clojure
  ;; Compute rank of each element (1=smallest)
  (let [data (af-constant [6] [30 10 50 20 40 10] :s32)
        values (mem/alloc-instance ::mem/pointer)
        indices (mem/alloc-instance ::mem/pointer)]
    (af-sort-index values indices data 0 true)
    ;; indices: [1 5 3 0 4 2]
    ;; To get ranks: Create inverse mapping where rank[indices[i]] = i
    )
  ```

  Example (2D sorting with indices):
  ```clojure
  ;; Sort columns and track original row positions
  (let [matrix (af-constant [4 3] [[5 8 2]
                                    [1 6 9]
                                    [7 3 4]
                                    [2 5 1]] :f64)
        sorted (mem/alloc-instance ::mem/pointer)
        indices (mem/alloc-instance ::mem/pointer)]
    (af-sort-index sorted indices matrix 0 true) ;; sort each column
    ;; Each column sorted independently with its own index array
    )
  ```

  Batch Processing:
  For multi-dimensional arrays, indices are generated for each slice:
  - Input: [m, n, p] sorted along dim=0
  - Output values: [m, n, p] sorted values
  - Output indices: [m, n, p] indices in range [0, m-1] for each column

  Index Range:
  - Indices are always in range [0, N-1] where N is size along sort dimension
  - Each slice has independent indices (indices reset for each slice)
  - Type is always u32 regardless of input type

  Type Support:
  Input (values): All numeric and boolean types
  Output (indices): Always u32

  Performance:
  - Slightly slower than af-sort (due to index tracking)
  - Use af-sort when indices not needed
  - Index generation has minimal overhead (~5-10%)

  Applications:
  - Ranking and percentile calculations
  - Reordering parallel arrays (names with scores, etc.)
  - Finding k-th smallest/largest elements
  - Argsort operations in machine learning
  - Creating lookup tables and permutations

  Edge Cases:
  - Empty arrays (elements=0): Returns empty arrays for both outputs
  - Duplicate values: Stable sort preserves original order (indices reflect this)

  See also:
  - af_sort_index (ArrayFire C API)
  - af-sort: Basic sort without indices
  - af-sort-by-key: Sort by key array
  - af-lookup: Use indices to reorder arrays"
  "af_sort_index" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/int] ::mem/int)

(defcfn af-sort-by-key
  "Sort values array according to a keys array.

  Performs a key-value sort where the keys array determines the sorting order,
  and both keys and values are reordered together. This is fundamental for
  database-style operations and maintaining relationships between arrays.

  Parameters:
  - out-keys: Output array containing sorted keys
  - out-values: Output array containing values reordered by keys
  - keys: Input array of keys (determines sort order)
  - values: Input array of values (reordered according to keys)
  - dim: Dimension along which to sort (0-3)
  - is-ascending: Sorting order (true=ascending, false=descending)

  Returns:
  Error code indicating success or failure.

  Operation:
  1. Sort keys array to determine ordering
  2. Apply same permutation to values array
  3. Return both sorted keys and reordered values

  Example (basic key-value sort):
  ```clojure
  ;; Sort students by exam score
  (let [scores (af-constant [4] [85 92 78 95] :f32)      ;; keys
        ids (af-constant [4] [101 102 103 104] :u32)     ;; values
        sorted-scores (mem/alloc-instance ::mem/pointer)
        sorted-ids (mem/alloc-instance ::mem/pointer)]
    (af-sort-by-key sorted-scores sorted-ids scores ids 0 false) ;; descending
    ;; sorted-scores: [95 92 85 78]
    ;; sorted-ids: [104 102 101 103]
    )
  ```

  Example (multi-column sorting):
  ```clojure
  ;; Sort 2D data by key column
  (let [keys (af-constant [5] [3.5 1.2 4.8 2.1 3.5] :f64)
        values (af-constant [5 3] [[10 20 30]     ;; multiple value columns
                                    [40 50 60]
                                    [70 80 90]
                                    [11 22 33]
                                    [44 55 66]] :s32)
        out-keys (mem/alloc-instance ::mem/pointer)
        out-vals (mem/alloc-instance ::mem/pointer)]
    (af-sort-by-key out-keys out-vals keys values 0 true)
    ;; Rows reordered based on key values
    )
  ```

  Example (timestamp sorting):
  ```clojure
  ;; Sort sensor readings by timestamp
  (let [timestamps (af-constant [100] time-data :f64)  ;; Unix timestamps
        readings (af-constant [100] sensor-data :f32)   ;; sensor values
        sorted-times (mem/alloc-instance ::mem/pointer)
        sorted-readings (mem/alloc-instance ::mem/pointer)]
    (af-sort-by-key sorted-times sorted-readings timestamps readings 0 true)
    ;; Time series now in chronological order
    )
  ```

  Example (multi-dimensional batch):
  ```clojure
  ;; Sort multiple time series by their keys
  (let [keys (af-constant [100 10] key-data :f32)     ;; 10 time series
        values (af-constant [100 10] value-data :f32)
        out-k (mem/alloc-instance ::mem/pointer)
        out-v (mem/alloc-instance ::mem/pointer)]
    (af-sort-by-key out-k out-v keys values 0 true)
    ;; Each of 10 time series sorted independently
    )
  ```

  Dimension Requirements:
  - Keys and values must have identical dimensions
  - Both are sorted along the same dimension
  - All other dimensions must match exactly

  Type Support:
  - Keys: All real-valued types (no complex keys)
    * f32, f64, s8, s16, s32, s64, u8, u16, u32, u64, b8
  - Values: All types including complex
    * f32, f64, c32, c64, s8, s16, s32, s64, u8, u16, u32, u64, b8

  Batch Processing:
  When sorting multi-dimensional arrays:
  - Each slice perpendicular to sort dimension is sorted independently
  - Keys and values for each slice are kept together
  - All slices processed in parallel on GPU

  Stability:
  - Sort is stable: Equal keys preserve original relative order
  - Important for multi-level sorting (sort by secondary key first, then primary)

  Performance:
  - Similar performance to af-sort (key-value tracking has minimal overhead)
  - GPU parallelization provides significant speedup for large datasets
  - Memory usage: Requires space for both input and output arrays

  Applications:
  - Database operations: Sort records by key field
  - Time series: Reorder by timestamp
  - Scientific data: Sort measurements by parameter value
  - Graphics: Z-sorting (sort by depth)
  - Event processing: Sort events by time or priority
  - Histograms: Sort bins by count

  Edge Cases:
  - Empty arrays (elements=0): Returns empty arrays for both outputs
  - Duplicate keys: Stable sort maintains original order for equal keys
  - Dimension mismatch: Returns error

  Limitations:
  - Keys must be real-valued (complex keys not supported)
  - Dimensions of keys and values must match exactly

  See also:
  - af_sort_by_key (ArrayFire C API)
  - af-sort: Basic sorting
  - af-sort-index: Sort with index tracking"
  "af_sort_by_key" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/int] ::mem/int)
