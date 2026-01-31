(ns org.soulspace.arrayfire.ffi.set
  "Bindings for the ArrayFire set operation functions.
   
   Set operations treat arrays as collections of unique values and perform
   standard mathematical set operations: finding unique elements, union,
   and intersection.
   
   **Set Operations**:
   
   1. **Unique** - Extract unique values from an array
      - Removes duplicate values
      - Returns sorted array of unique elements
      - Input can be pre-sorted for optimization
   
   2. **Union** - Combine two sets (A ∪ B)
      - All elements from both arrays
      - No duplicates in result
      - Result is sorted in increasing order
   
   3. **Intersection** - Common elements (A ∩ B)
      - Elements present in both arrays
      - No duplicates in result
      - Result is sorted in increasing order
   
   **Mathematical Definitions**:
   
   - **Unique**: unique(A) = {x | x ∈ A}
     Returns each element of A exactly once
   
   - **Union**: A ∪ B = {x | x ∈ A or x ∈ B}
     Example: {1,2,3} ∪ {2,3,4} = {1,2,3,4}
   
   - **Intersection**: A ∩ B = {x | x ∈ A and x ∈ B}
     Example: {1,2,3} ∩ {2,3,4} = {2,3}
   
   **Input Requirements**:
   - Input arrays must be vectors (1D) or scalars
   - For union/intersection: both arrays must have same type
   - Empty arrays are handled: union returns other array, intersection returns empty
   
   **Performance Optimization**:
   
   The `is_sorted` and `is_unique` flags optimize performance:
   
   - **is_sorted=true** (for unique): Skip internal sorting if input already sorted
   - **is_unique=true** (for union/intersection): Skip unique() call if inputs
     already contain only unique values
   
   Without optimization:
   1. Sort input arrays
   2. Remove duplicates
   3. Perform operation
   
   With optimization:
   1. Directly perform operation (assumes inputs valid)
   
   **Usage Patterns**:
   
   ```clojure
   ;; Find unique values in unsorted data
   (af-set-unique out-ptr arr false)
   
   ;; Union of two arrays (not pre-processed)
   (af-set-union out-ptr arr1 arr2 false)
   
   ;; Intersection when inputs already unique (optimized)
   (af-set-intersect out-ptr unique-arr1 unique-arr2 true)
   ```
   
   **Type Support**:
   - All numeric types: f32, f64, s8, u8, s16, u16, s32, u32, s64, u64
   - Boolean: b8
   - Complex types: Not supported
   
   **Algorithm Complexity**:
   - Unique: O(n log n) for sorting + O(n) for deduplication
   - Union: O(n log n + m log m) worst case
   - Intersection: O(n + m) with sorted inputs
   where n, m are input array sizes
   
   **GPU Acceleration**:
   - Parallel sorting algorithms
   - Efficient merge operations
   - Particularly fast for large arrays (10K+ elements)
   
   **Applications**:
   - Data deduplication
   - Set-based filtering
   - Finding common elements between datasets
   - Statistical analysis (unique value counts)
   - Graph algorithms (vertex/edge set operations)
   
   See also:
   - af-sort: For sorting without deduplication
   - af-sort-index: Get indices of sorted unique values"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; af_err af_set_unique(af_array *out, const af_array in, const bool is_sorted)
(defcfn af-set-unique
  "Find unique values in an array.
   
   Returns a sorted array containing each unique value from the input
   array exactly once, with duplicates removed.
   
   Parameters:
   - out: Output pointer for array of unique values (sorted)
   - in: Input array (must be vector or scalar)
   - is-sorted: Boolean flag indicating if input is already sorted
     * true: Skip internal sorting (optimization)
     * false: Sort internally before finding uniques
   
   Behavior:
   - Scalar input: Returns the scalar unchanged
   - Empty input: Returns empty array
   - Vector input: Returns sorted unique values
   
   Algorithm:
   1. If not sorted: Sort input array
   2. Scan through sorted array
   3. Keep only first occurrence of each value
   4. Return compacted result
   
   Performance:
   - O(n log n) if is_sorted=false (sorting dominates)
   - O(n) if is_sorted=true (single pass)
   - GPU parallelizes sorting and compaction
   
   Example:
   ```clojure
   ;; Find unique values in unsorted data
   (let [data (create-array [3 1 4 1 5 9 2 6 5] [9])
         out-ptr (mem/alloc-pointer ::mem/pointer)]
     (af-set-unique out-ptr data false)
     ;; Result: [1 2 3 4 5 6 9]
     (mem/read-pointer out-ptr ::mem/pointer))
   
   ;; Optimized version with pre-sorted input
   (let [sorted-data (create-array [1 1 2 3 3 4] [6])
         out-ptr (mem/alloc-pointer ::mem/pointer)]
     (af-set-unique out-ptr sorted-data true)
     ;; Result: [1 2 3 4]
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Type Support:
   - All numeric types: f32, f64, s8-s64, u8-u64
   - Boolean: b8
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-set-union: Unique values from two arrays combined
   - af-sort: Sort without removing duplicates"
  "af_set_unique" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_set_union(af_array *out, const af_array first, const af_array second, const bool is_unique)
(defcfn af-set-union
  "Compute the union of two arrays.
   
   Returns a sorted array containing all unique values present in either
   the first or second array (or both). Mathematical set union: A ∪ B.
   
   Parameters:
   - out: Output pointer for union array (sorted, no duplicates)
   - first: First input array (vector or scalar)
   - second: Second input array (vector or scalar, same type as first)
   - is-unique: Boolean flag indicating if inputs already contain only
     unique values
     * true: Skip internal unique() calls (optimization)
     * false: Call unique() on inputs first
   
   Behavior:
   - Empty first: Returns second array
   - Empty second: Returns first array
   - Both types must match
   
   Algorithm (if is_unique=false):
   1. Find unique values in first array
   2. Find unique values in second array
   3. Merge the two unique sets
   4. Sort and deduplicate the result
   
   Algorithm (if is_unique=true):
   1. Merge inputs directly (assumes already unique)
   2. Sort and deduplicate
   
   Performance:
   - O(n log n + m log m) worst case
   - Faster with is_unique=true if inputs pre-processed
   - GPU parallelizes merge and sort operations
   
   Example:
   ```clojure
   ;; Basic union
   (let [arr1 (create-array [1 2 3 2 1] [5])
         arr2 (create-array [3 4 5 4] [4])
         out-ptr (mem/alloc-pointer ::mem/pointer)]
     (af-set-union out-ptr arr1 arr2 false)
     ;; Result: [1 2 3 4 5]
     (mem/read-pointer out-ptr ::mem/pointer))
   
   ;; Optimized with pre-unique inputs
   (let [unique1 (create-array [1 2 3] [3])
         unique2 (create-array [3 4 5] [3])
         out-ptr (mem/alloc-pointer ::mem/pointer)]
     (af-set-union out-ptr unique1 unique2 true)
     ;; Result: [1 2 3 4 5]
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Type Support:
   - Both arrays must be same type
   - All numeric types: f32, f64, s8-s64, u8-u64
   - Boolean: b8
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-set-intersect: Common elements (A ∩ B)
   - af-set-unique: Unique values in single array"
  "af_set_union" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_set_intersect(af_array *out, const af_array first, const af_array second, const bool is_unique)
(defcfn af-set-intersect
  "Compute the intersection of two arrays.
   
   Returns a sorted array containing only values present in both the
   first and second arrays. Mathematical set intersection: A ∩ B.
   
   Parameters:
   - out: Output pointer for intersection array (sorted, no duplicates)
   - first: First input array (vector or scalar)
   - second: Second input array (vector or scalar, same type as first)
   - is-unique: Boolean flag indicating if inputs already contain only
     unique values
     * true: Skip internal unique() calls (optimization)
     * false: Call unique() on inputs first
   
   Behavior:
   - Empty first: Returns empty array
   - Empty second: Returns empty array
   - No common elements: Returns empty array
   - Both types must match
   
   Algorithm (if is_unique=false):
   1. Find unique values in first array
   2. Find unique values in second array
   3. Find common values between unique sets
   4. Return sorted result
   
   Algorithm (if is_unique=true):
   1. Directly find common values (assumes already unique)
   2. Return sorted result
   
   Performance:
   - O(n + m) with sorted inputs (linear merge)
   - O(n log n + m log m) if sorting needed
   - GPU parallelizes comparison operations
   - Result size ≤ min(n, m)
   
   Example:
   ```clojure
   ;; Basic intersection
   (let [arr1 (create-array [1 2 3 2 1] [5])
         arr2 (create-array [2 3 4 3] [4])
         out-ptr (mem/alloc-pointer ::mem/pointer)]
     (af-set-intersect out-ptr arr1 arr2 false)
     ;; Result: [2 3]
     (mem/read-pointer out-ptr ::mem/pointer))
   
   ;; Optimized with pre-unique inputs
   (let [unique1 (create-array [1 2 3 4] [4])
         unique2 (create-array [3 4 5 6] [4])
         out-ptr (mem/alloc-pointer ::mem/pointer)]
     (af-set-intersect out-ptr unique1 unique2 true)
     ;; Result: [3 4]
     (mem/read-pointer out-ptr ::mem/pointer))
   
   ;; No common elements
   (let [arr1 (create-array [1 2 3] [3])
         arr2 (create-array [4 5 6] [3])
         out-ptr (mem/alloc-pointer ::mem/pointer)]
     (af-set-intersect out-ptr arr1 arr2 false)
     ;; Result: [] (empty)
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Type Support:
   - Both arrays must be same type
   - All numeric types: f32, f64, s8-s64, u8-u64
   - Boolean: b8
   
   Applications:
   - Finding common IDs between datasets
   - Set-based filtering
   - Graph algorithms (common neighbors)
   - Database-style inner join on values
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-set-union: All elements from both arrays (A ∪ B)
   - af-set-unique: Unique values in single array"
  "af_set_intersect" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)
