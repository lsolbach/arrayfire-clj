(ns org.soulspace.arrayfire.integration.algorithm
  "Integration of the ArrayFire algorithm related FFI bindings with the error
   handling and resource management on the JVM.
   
   Note: Matrix decomposition functions (LU, QR, SVD) have been moved to
   org.soulspace.arrayfire.integration.lapack to align with the ArrayFire
   Unified API structure."
  (:refer-clojure :exclude [min max count sort])
  (:require [org.soulspace.arrayfire.ffi.c-api.reduce :as reduce]
            [org.soulspace.arrayfire.ffi.c-api.scan :as scan]
            [org.soulspace.arrayfire.ffi.c-api.sort :as sort]
            [org.soulspace.arrayfire.ffi.c-api.set :as set-ops]
            [org.soulspace.arrayfire.ffi.c-api.where :as where]
            [org.soulspace.arrayfire.ffi.c-api.diff :as diff]
            [org.soulspace.arrayfire.integration.jvm-integration :as jvm])
  (:import (org.soulspace.arrayfire.integration.jvm_integration AFArray)))

;;;
;;; Reduction Operations
;;;

(defn sum
  "Sum elements along a dimension.
   
   Parameters:
   - in: Input array (AFArray)
   - dim: Dimension along which to sum (default -1 for all dimensions)
   
   Returns:
   AFArray with summed values"
  ([^AFArray in]
   (sum in -1))
  ([^AFArray in dim]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (reduce/af-sum out (jvm/af-handle in) (int dim))
                 "af-sum")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn sum-nan
  "Sum elements along a dimension, treating NaN as a specified value.
   
   Parameters:
   - in: Input array (AFArray)
   - dim: Dimension along which to sum
   - nan-val: Value to treat NaN as (default 0.0)
   
   Returns:
   AFArray with summed values"
  ([^AFArray in dim]
   (sum-nan in dim 0.0))
  ([^AFArray in dim nan-val]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (reduce/af-sum-nan out (jvm/af-handle in) (int dim) (double nan-val))
                 "af-sum-nan")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn product
  "Multiply elements along a dimension.
   
   Parameters:
   - in: Input array (AFArray)
   - dim: Dimension along which to multiply (default -1 for all dimensions)
   
   Returns:
   AFArray with product values"
  ([^AFArray in]
   (product in -1))
  ([^AFArray in dim]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (reduce/af-product out (jvm/af-handle in) (int dim))
                 "af-product")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn min
  "Find minimum values along a dimension.
   
   Parameters:
   - in: Input array (AFArray)
   - dim: Dimension along which to find minimum (default -1)
   
   Returns:
   AFArray with minimum values"
  ([^AFArray in]
   (min in -1))
  ([^AFArray in dim]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (reduce/af-min out (jvm/af-handle in) (int dim))
                 "af-min")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn max
  "Find maximum values along a dimension.
   
   Parameters:
   - in: Input array (AFArray)
   - dim: Dimension along which to find maximum (default -1)
   
   Returns:
   AFArray with maximum values"
  ([^AFArray in]
   (max in -1))
  ([^AFArray in dim]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (reduce/af-max out (jvm/af-handle in) (int dim))
                 "af-max")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn all-true
  "Check if all elements are true (non-zero) along a dimension.
   
   Parameters:
   - in: Input array (AFArray)
   - dim: Dimension along which to check (default -1)
   
   Returns:
   AFArray with boolean results"
  ([^AFArray in]
   (all-true in -1))
  ([^AFArray in dim]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (reduce/af-all-true out (jvm/af-handle in) (int dim))
                 "af-all-true")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn any-true
  "Check if any element is true (non-zero) along a dimension.
   
   Parameters:
   - in: Input array (AFArray)
   - dim: Dimension along which to check (default -1)
   
   Returns:
   AFArray with boolean results"
  ([^AFArray in]
   (any-true in -1))
  ([^AFArray in dim]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (reduce/af-any-true out (jvm/af-handle in) (int dim))
                 "af-any-true")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn count
  "Count non-zero elements along a dimension.
   
   Parameters:
   - in: Input array (AFArray)
   - dim: Dimension along which to count (default -1)
   
   Returns:
   AFArray with counts"
  ([^AFArray in]
   (count in -1))
  ([^AFArray in dim]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (reduce/af-count out (jvm/af-handle in) (int dim))
                 "af-count")
     (jvm/af-array-new (jvm/deref-af-array out)))))

;;;
;;; Scan Operations
;;;

(defn scan
  "Perform inclusive or exclusive scan (prefix sum/product/etc) along a dimension.
   
   Parameters:
   - in: Input array (AFArray)
   - dim: Dimension along which to scan
   - op: Binary operation (AF_BINARY_ADD, AF_BINARY_MUL, AF_BINARY_MIN, AF_BINARY_MAX)
   - inclusive: Boolean, true for inclusive scan, false for exclusive (default true)
   
   Returns:
   AFArray with scanned values"
  ([^AFArray in dim op]
   (scan in dim op true))
  ([^AFArray in dim op inclusive]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (scan/af-scan out (jvm/af-handle in) (int dim) (int op) (if inclusive 1 0))
                 "af-scan")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn scan-by-key
  "Perform scan operation grouped by keys.
   
   Parameters:
   - key: Key array (AFArray)
   - in: Input array (AFArray)
   - dim: Dimension along which to scan
   - op: Binary operation
   - inclusive: Boolean, true for inclusive scan (default true)
   
   Returns:
   AFArray with scanned values"
  ([^AFArray key ^AFArray in dim op]
   (scan-by-key key in dim op true))
  ([^AFArray key ^AFArray in dim op inclusive]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (scan/af-scan-by-key out (jvm/af-handle key) (jvm/af-handle in) (int dim) (int op) (if inclusive 1 0))
                 "af-scan-by-key")
     (jvm/af-array-new (jvm/deref-af-array out)))))

;;;
;;; Sorting Operations
;;;

(defn sort
  "Sort an array along a dimension.
   
   Parameters:
   - in: Input array (AFArray)
   - dim: Dimension along which to sort (default 0)
   - is-ascending: Boolean, true for ascending order (default true)
   
   Returns:
   Sorted AFArray"
  ([^AFArray in]
   (sort in 0 true))
  ([^AFArray in dim]
   (sort in dim true))
  ([^AFArray in dim is-ascending]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (sort/af-sort out (jvm/af-handle in) (int dim) (if is-ascending 1 0))
                 "af-sort")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn sort-index
  "Sort an array and return both sorted values and indices.
   
   Parameters:
   - in: Input array (AFArray)
   - dim: Dimension along which to sort (default 0)
   - is-ascending: Boolean, true for ascending order (default true)
   
   Returns:
   Vector of [sorted-values indices] as AFArrays"
  ([^AFArray in]
   (sort-index in 0 true))
  ([^AFArray in dim]
   (sort-index in dim true))
  ([^AFArray in dim is-ascending]
   (let [out (jvm/native-af-array-pointer)
         indices (jvm/native-af-array-pointer)]
     (jvm/check! (sort/af-sort-index out indices (jvm/af-handle in) (int dim) (if is-ascending 1 0))
                 "af-sort-index")
     [(jvm/af-array-new (jvm/deref-af-array out))
      (jvm/af-array-new (jvm/deref-af-array indices))])))

(defn sort-by-key
  "Sort values array based on keys array.
   
   Parameters:
   - keys: Keys array (AFArray)
   - values: Values array (AFArray)
   - dim: Dimension along which to sort (default 0)
   - is-ascending: Boolean, true for ascending order (default true)
   
   Returns:
   Vector of [sorted-keys sorted-values] as AFArrays"
  ([^AFArray keys ^AFArray values]
   (sort-by-key keys values 0 true))
  ([^AFArray keys ^AFArray values dim]
   (sort-by-key keys values dim true))
  ([^AFArray keys ^AFArray values dim is-ascending]
   (let [out-keys (jvm/native-af-array-pointer)
         out-values (jvm/native-af-array-pointer)]
     (jvm/check! (sort/af-sort-by-key out-keys out-values (jvm/af-handle keys) (jvm/af-handle values) (int dim) (if is-ascending 1 0))
                 "af-sort-by-key")
     [(jvm/af-array-new (jvm/deref-af-array out-keys))
      (jvm/af-array-new (jvm/deref-af-array out-values))])))

;;;
;;; Set Operations
;;;

(defn set-unique
  "Find unique elements in an array.
   
   Parameters:
   - in: Input array (AFArray)
   - is-sorted: Boolean, true if input is already sorted (default false)
   
   Returns:
   AFArray with unique elements"
  ([^AFArray in]
   (set-unique in false))
  ([^AFArray in is-sorted]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (set-ops/af-set-unique out (jvm/af-handle in) (if is-sorted 1 0))
                 "af-set-unique")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn set-union
  "Compute the union of two arrays.
   
   Parameters:
   - first: First array (AFArray)
   - second: Second array (AFArray)
   - is-unique: Boolean, true if inputs contain only unique values (default false)
   
   Returns:
   AFArray with union of elements"
  ([^AFArray first ^AFArray second]
   (set-union first second false))
  ([^AFArray first ^AFArray second is-unique]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (set-ops/af-set-union out (jvm/af-handle first) (jvm/af-handle second) (if is-unique 1 0))
                 "af-set-union")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn set-intersect
  "Compute the intersection of two arrays.
   
   Parameters:
   - first: First array (AFArray)
   - second: Second array (AFArray)
   - is-unique: Boolean, true if inputs contain only unique values (default false)
   
   Returns:
   AFArray with intersection of elements"
  ([^AFArray first ^AFArray second]
   (set-intersect first second false))
  ([^AFArray first ^AFArray second is-unique]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (set-ops/af-set-intersect out (jvm/af-handle first) (jvm/af-handle second) (if is-unique 1 0))
                 "af-set-intersect")
     (jvm/af-array-new (jvm/deref-af-array out)))))

;;;
;;; Other Algorithm Operations
;;;

(defn where
  "Find indices of non-zero elements.
   
   Parameters:
   - in: Input array (AFArray)
   
   Returns:
   AFArray with linear indices of non-zero elements"
  [^AFArray in]
  (let [idx (jvm/native-af-array-pointer)]
    (jvm/check! (where/af-where idx (jvm/af-handle in))
                "af-where")
    (jvm/af-array-new (jvm/deref-af-array idx))))

;;;  
;;; By-Key Reduction Operations
;;;

(defn sum-by-key
  "Sum values grouped by keys.
   
   Groups consecutive elements with the same key and sums values within
   each group. Useful for categorical data aggregation.
   
   Parameters:
   - keys: Input keys array (AFArray) - determines grouping
   - vals: Input values array (AFArray) - to be summed
   - dim: Dimension along which to reduce (default 0)
   
   Returns:
   Vector of [keys-out vals-out] where:
   - keys-out: AFArray with unique keys
   - vals-out: AFArray with summed values per key
   
   Notes:
   - Keys and vals must have same shape
   - Keys should be sorted for meaningful grouping
   - Integer keys recommended (int or uint)
   
   Example:
   ```clojure
   (let [[categories totals] (sum-by-key 
                               (array [1 1 1 2 2 3])
                               (array [10 20 30 40 50 60]))
     {:categories categories  ; [1 2 3]
      :totals totals})        ; [60 90 60]
   ```"
  ([^AFArray keys ^AFArray vals]
   (sum-by-key keys vals 0))
  ([^AFArray keys ^AFArray vals dim]
   (let [keys-out (jvm/native-af-array-pointer)
         vals-out (jvm/native-af-array-pointer)]
     (jvm/check! (reduce/af-sum-by-key keys-out vals-out 
                                       (jvm/af-handle keys) (jvm/af-handle vals) (int dim))
                 "af-sum-by-key")
     [(jvm/af-array-new (jvm/deref-af-array keys-out))
      (jvm/af-array-new (jvm/deref-af-array vals-out))])))

(defn product-by-key
  "Multiply values grouped by keys.
   
   Groups consecutive elements with the same key and multiplies values
   within each group.
   
   Parameters:
   - keys: Input keys array (AFArray) - determines grouping
   - vals: Input values array (AFArray) - to be multiplied
   - dim: Dimension along which to reduce (default 0)
   
   Returns:
   Vector of [keys-out vals-out] where:
   - keys-out: AFArray with unique keys
   - vals-out: AFArray with product of values per key
   
   Example:
   ```clojure
   (let [[keys products] (product-by-key
                           (array [1 1 2 2 3])
                           (array [2 3 4 5 6]))]
     {:keys keys         ; [1 2 3]
      :products products}) ; [6 20 6]
   ```"
  ([^AFArray keys ^AFArray vals]
   (product-by-key keys vals 0))
  ([^AFArray keys ^AFArray vals dim]
   (let [keys-out (jvm/native-af-array-pointer)
         vals-out (jvm/native-af-array-pointer)]
     (jvm/check! (reduce/af-product-by-key keys-out vals-out
                                           (jvm/af-handle keys) (jvm/af-handle vals) (int dim))
                 "af-product-by-key")
     [(jvm/af-array-new (jvm/deref-af-array keys-out))
      (jvm/af-array-new (jvm/deref-af-array vals-out))])))

(defn min-by-key
  "Find minimum value per key group.
   
   Parameters:
   - keys: Input keys array (AFArray) - determines grouping
   - vals: Input values array (AFArray)
   - dim: Dimension along which to reduce (default 0)
   
   Returns:
   Vector of [keys-out vals-out] where:
   - keys-out: AFArray with unique keys
   - vals-out: AFArray with minimum value per key
   
   Example:
   ```clojure
   (let [[keys mins] (min-by-key
                       (array [1 1 1 2 2])
                       (array [5 2 8 3 7]))]
     {:keys keys  ; [1 2]
      :mins mins}) ; [2 3]
   ```"
  ([^AFArray keys ^AFArray vals]
   (min-by-key keys vals 0))
  ([^AFArray keys ^AFArray vals dim]
   (let [keys-out (jvm/native-af-array-pointer)
         vals-out (jvm/native-af-array-pointer)]
     (jvm/check! (reduce/af-min-by-key keys-out vals-out
                                       (jvm/af-handle keys) (jvm/af-handle vals) (int dim))
                 "af-min-by-key")
     [(jvm/af-array-new (jvm/deref-af-array keys-out))
      (jvm/af-array-new (jvm/deref-af-array vals-out))])))

(defn max-by-key
  "Find maximum value per key group.
   
   Parameters:
   - keys: Input keys array (AFArray) - determines grouping
   - vals: Input values array (AFArray)
   - dim: Dimension along which to reduce (default 0)
   
   Returns:
   Vector of [keys-out vals-out] where:
   - keys-out: AFArray with unique keys
   - vals-out: AFArray with maximum value per key
   
   Example:
   ```clojure
   (let [[keys maxs] (max-by-key
                       (array [1 1 1 2 2])
                       (array [5 2 8 3 7]))]
     {:keys keys  ; [1 2]
      :maxs maxs}) ; [8 7]
   ```"
  ([^AFArray keys ^AFArray vals]
   (max-by-key keys vals 0))
  ([^AFArray keys ^AFArray vals dim]
   (let [keys-out (jvm/native-af-array-pointer)
         vals-out (jvm/native-af-array-pointer)]
     (jvm/check! (reduce/af-max-by-key keys-out vals-out
                                       (jvm/af-handle keys) (jvm/af-handle vals) (int dim))
                 "af-max-by-key")
     [(jvm/af-array-new (jvm/deref-af-array keys-out))
      (jvm/af-array-new (jvm/deref-af-array vals-out))])))

(defn all-true-by-key
  "Check if all values are true (non-zero) per key group.
   
   Tests whether all elements in each key group are non-zero.
   Returns boolean array with one value per unique key.
   
   Parameters:
   - keys: Input keys array (AFArray) - determines grouping
   - vals: Input values array (AFArray) - tested for non-zero
   - dim: Dimension along which to reduce (default 0)
   
   Returns:
   Vector of [keys-out vals-out] where:
   - keys-out: AFArray with unique keys
   - vals-out: AFArray with 1 if all values true, 0 otherwise
   
   Example:
   ```clojure
   (let [[keys results] (all-true-by-key
                          (array [1 1 1 2 2 2])
                          (array [1 1 1 0 1 1]))]
     {:keys keys      ; [1 2]
      :all-true results}) ; [1 0] - group 1 all true, group 2 has a zero
   ```
   
   Use cases:
   - Data validation: Check all records in group meet criteria
   - Logic: AND operation over grouped data
   - Quality control: All measurements in batch pass threshold"
  ([^AFArray keys ^AFArray vals]
   (all-true-by-key keys vals 0))
  ([^AFArray keys ^AFArray vals dim]
   (let [keys-out (jvm/native-af-array-pointer)
         vals-out (jvm/native-af-array-pointer)]
     (jvm/check! (reduce/af-all-true-by-key keys-out vals-out
                                            (jvm/af-handle keys) (jvm/af-handle vals) (int dim))
                 "af-all-true-by-key")
     [(jvm/af-array-new (jvm/deref-af-array keys-out))
      (jvm/af-array-new (jvm/deref-af-array vals-out))])))

(defn any-true-by-key
  "Check if any value is true (non-zero) per key group.
   
   Tests whether at least one element in each key group is non-zero.
   Returns boolean array with one value per unique key.
   
   Parameters:
   - keys: Input keys array (AFArray) - determines grouping
   - vals: Input values array (AFArray) - tested for non-zero
   - dim: Dimension along which to reduce (default 0)
   
   Returns:
   Vector of [keys-out vals-out] where:
   - keys-out: AFArray with unique keys
   - vals-out: AFArray with 1 if any value true, 0 if all zero
   
   Example:
   ```clojure
   (let [[keys results] (any-true-by-key
                          (array [1 1 1 2 2 2])
                          (array [0 0 1 0 0 0]))]
     {:keys keys       ; [1 2]
      :any-true results}) ; [1 0] - group 1 has a true, group 2 all false
   ```
   
   Use cases:
   - Data filtering: Any record in group meets criteria
   - Logic: OR operation over grouped data
   - Alert systems: Any sensor in zone triggered
   - Error detection: Any test in suite failed"
  ([^AFArray keys ^AFArray vals]
   (any-true-by-key keys vals 0))
  ([^AFArray keys ^AFArray vals dim]
   (let [keys-out (jvm/native-af-array-pointer)
         vals-out (jvm/native-af-array-pointer)]
     (jvm/check! (reduce/af-any-true-by-key keys-out vals-out
                                            (jvm/af-handle keys) (jvm/af-handle vals) (int dim))
                 "af-any-true-by-key")
     [(jvm/af-array-new (jvm/deref-af-array keys-out))
      (jvm/af-array-new (jvm/deref-af-array vals-out))])))

(defn count-by-key
  "Count non-zero values per key group.
   
   Counts the number of non-zero elements in each key group.
   
   Parameters:
   - keys: Input keys array (AFArray) - determines grouping
   - vals: Input values array (AFArray) - counted if non-zero
   - dim: Dimension along which to reduce (default 0)
   
   Returns:
   Vector of [keys-out vals-out] where:
   - keys-out: AFArray with unique keys
   - vals-out: AFArray with count of non-zero values per key
   
   Example:
   ```clojure
   (let [[keys counts] (count-by-key
                         (array [1 1 1 2 2 2])
                         (array [1 0 1 1 1 0]))]
     {:keys keys    ; [1 2]
      :counts counts}) ; [2 2] - 2 non-zero in each group
   ```
   
   Use cases:
   - Statistics: Count valid observations per category
   - Data quality: Count non-missing values per group
   - Event counting: Occurrences per category
   - Sparse data: Count non-zero entries per key"
  ([^AFArray keys ^AFArray vals]
   (count-by-key keys vals 0))
  ([^AFArray keys ^AFArray vals dim]
   (let [keys-out (jvm/native-af-array-pointer)
         vals-out (jvm/native-af-array-pointer)]
     (jvm/check! (reduce/af-count-by-key keys-out vals-out
                                         (jvm/af-handle keys) (jvm/af-handle vals) (int dim))
                 "af-count-by-key")
     [(jvm/af-array-new (jvm/deref-af-array keys-out))
      (jvm/af-array-new (jvm/deref-af-array vals-out))])))

;;;
;;; Cumulative Operations (Scans)
;;;

(defn accum
  "Cumulative sum (inclusive prefix sum) along a dimension.
   
   Computes running sum: out[i] = in[0] + in[1] + ... + in[i]
   Convenience wrapper for scan with addition operation.
   
   Parameters:
   - in: Input array (AFArray)
   - dim: Dimension along which to accumulate (default 0)
   
   Returns:
   AFArray with cumulative sums
   
   Example:
   ```clojure
   ;; Running total
   (let [values (array [1 2 3 4 5])
         cumsum (accum values)]
     cumsum)  ; [1 3 6 10 15]
   
   ;; Financial: cumulative returns
   (let [returns (array [0.01 -0.02 0.03 0.01 -0.01])
         cum-return (accum returns)]
     cum-return)  ; [0.01 -0.01 0.02 0.03 0.02]
   
   ;; 2D: accumulate down columns
   (let [data (array [[1 2] [3 4] [5 6]])
         cumsum (accum data 0)]
     cumsum)  ; [[1 2] [4 6] [9 12]]
   ```
   
   Use cases:
   - Time series: Running totals, cumulative statistics
   - Finance: Cumulative returns, portfolio value over time
   - Physics: Position from velocity (integration)
   - Signal processing: Cumulative energy
   - Graphics: Opacity accumulation, path integration
   
   Notes:
   - This is the inverse operation of diff1
   - Also known as: cumsum, prefix sum, inclusive scan
   - For exclusive scan, use scan with inclusive=false"
  ([^AFArray in]
   (accum in 0))
  ([^AFArray in dim]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (scan/af-accum out (jvm/af-handle in) (int dim))
                 "af-accum")
     (jvm/af-array-new (jvm/deref-af-array out)))))

;;;  
;;; Difference Operations
;;;

(defn diff1
  "Compute first-order difference along a dimension.
   
   Computes: out[i] = in[i+1] - in[i]
   Output size along dimension is reduced by 1.
   
   Parameters:
   - in: Input array (AFArray)
   - dim: Dimension along which to compute differences (default 0)
   
   Returns:
   AFArray with first-order differences
   
   Example:
   ```clojure
   ;; Compute velocity from position
   (let [position (array [0 1 4 9 16])  ; positions at t=0,1,2,3,4
         velocity (diff1 position)]      ; [1 3 5 7] - velocities
     velocity)
   
   ;; Image gradients
   (let [img (load-image \"photo.jpg\")
         dx (diff1 img 1)  ; horizontal gradient
         dy (diff1 img 0)] ; vertical gradient
     [dx dy])
   ```
   
   Applications:
   - Time series: Compute rates of change
   - Signal processing: Detect discontinuities
   - Image processing: Edge detection (gradients)
   - Physics: Velocity from position, acceleration from velocity"
  ([^AFArray in]
   (diff1 in 0))
  ([^AFArray in dim]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (diff/af-diff1 out (jvm/af-handle in) (int dim))
                 "af-diff1")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn diff2
  "Compute second-order difference along a dimension.
   
   Computes: out[i] = in[i+2] - 2*in[i+1] + in[i]
   Output size along dimension is reduced by 2.
   Equivalent to diff1(diff1(in)) but more numerically stable.
   
   Parameters:
   - in: Input array (AFArray)
   - dim: Dimension along which to compute differences (default 0)
   
   Returns:
   AFArray with second-order differences
   
   Example:
   ```clojure
   ;; Compute acceleration from position
   (let [position (array [0 1 4 9 16])  ; positions
         accel (diff2 position)]         ; [2 2 2] - constant acceleration
     accel)
   
   ;; Laplacian approximation
   (let [img (load-image \"photo.jpg\")
         laplacian-x (diff2 img 1)  ; second derivative horizontally
         laplacian-y (diff2 img 0)] ; second derivative vertically
     [laplacian-x laplacian-y])
   ```
   
   Applications:
   - Physics: Acceleration from position
   - Signal processing: Curvature detection
   - Image processing: Laplacian operator for edge detection
   - Numerical analysis: Approximate second derivatives"
  ([^AFArray in]
   (diff2 in 0))
  ([^AFArray in dim]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (diff/af-diff2 out (jvm/af-handle in) (int dim))
                 "af-diff2")
     (jvm/af-array-new (jvm/deref-af-array out)))))
