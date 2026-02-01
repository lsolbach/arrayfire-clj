(ns org.soulspace.arrayfire.integration.algorithm
  "Integration of the ArrayFire algorithm related FFI bindings with the error
   handling and resource management on the JVM."
  (:refer-clojure :exclude [min max count sort])
  (:require [org.soulspace.arrayfire.ffi.lu :as lu]
            [org.soulspace.arrayfire.ffi.qr :as qr]
            [org.soulspace.arrayfire.ffi.svd :as svd]
            [org.soulspace.arrayfire.ffi.reduce :as reduce]
            [org.soulspace.arrayfire.ffi.scan :as scan]
            [org.soulspace.arrayfire.ffi.sort :as sort]
            [org.soulspace.arrayfire.ffi.set :as set-ops]
            [org.soulspace.arrayfire.ffi.where :as where]
            [org.soulspace.arrayfire.integration.jvm-integration :as jvm])
  (:import (org.soulspace.arrayfire.integration.jvm_integration AFArray)))

;;;
;;; Matrix Decompositions
;;;

(defn lu
  "Compute the LU decomposition of a matrix.
   
   Parameters:
   - a: Input matrix (AFArray)
   
   Returns:
   A vector containing three AFArray instances:
   - L: Lower triangular matrix
   - U: Upper triangular matrix
   - P: Pivot indices as a permutation matrix"
  [^AFArray a]
  (let [l (jvm/native-af-array-pointer)
        u (jvm/native-af-array-pointer)
        p (jvm/native-af-array-pointer)]
    (jvm/check! (lu/af-lu l u p (jvm/af-handle a)) "af-lu")
    [(jvm/af-array-new (jvm/deref-af-array l))
     (jvm/af-array-new (jvm/deref-af-array u))
     (jvm/af-array-new (jvm/deref-af-array p))]))

(defn qr
  "Compute the QR decomposition of a matrix.
   
   Parameters:
   - a: Input matrix (AFArray)
   
   Returns:
   A vector containing two AFArray instances:
   - Q: Orthogonal matrix
   - R: Upper triangular matrix"
  [^AFArray a]
  (let [q (jvm/native-af-array-pointer)
        r (jvm/native-af-array-pointer)
        tau (jvm/native-af-array-pointer)]
    (jvm/check! (qr/af-qr q r tau (jvm/af-handle a)) "af-qr")
    [(jvm/af-array-new (jvm/deref-af-array q))
     (jvm/af-array-new (jvm/deref-af-array r))
     (jvm/af-array-new (jvm/deref-af-array tau))]))

(defn svd
  "Compute the Singular Value Decomposition (SVD) of a matrix.
   
   Parameters:
   - a: Input matrix (AFArray)
   
   Returns:
   A vector containing three AFArray instances:
   - U: Left singular vectors
   - S: Singular values (as a diagonal matrix)
   - VT: Right singular vectors (transposed)"
  [^AFArray a]
  (let [u  (jvm/native-af-array-pointer)
        s  (jvm/native-af-array-pointer)
        vt (jvm/native-af-array-pointer)]
    (jvm/check! (svd/af-svd u s vt (jvm/af-handle a)) "af-svd")
    [(jvm/af-array-new (jvm/deref-af-array u))
     (jvm/af-array-new (jvm/deref-af-array s))
     (jvm/af-array-new (jvm/deref-af-array vt))]))

(defn lu!
  "Perform in-place LU decomposition of a matrix.
   
   This function modifies the input array directly, making it memory-efficient
   for large matrices. The input is overwritten with the combined L and U matrices.
   
   Parameters:
   - in: Input/output matrix (AFArray), modified in place
   - is-lapack-piv: Boolean, use LAPACK pivot format (default false for ArrayFire format)
   
   Returns:
   AFArray containing pivot indices"
  ([^AFArray in]
   (lu! in false))
  ([^AFArray in is-lapack-piv]
   (let [pivot (jvm/native-af-array-pointer)]
     (jvm/check! (lu/af-lu-inplace pivot (jvm/af-handle in) (if is-lapack-piv 1 0))
                 "af-lu-inplace")
     (jvm/af-array-new (jvm/deref-af-array pivot)))))

(defn qr!
  "Perform in-place QR decomposition of a matrix.
   
   This function modifies the input array directly. The input is overwritten
   with the Q and R matrices combined in a compact format.
   
   Parameters:
   - in: Input/output matrix (AFArray), modified in place
   
   Returns:
   AFArray containing tau values (reflector scalars)"
  [^AFArray in]
  (let [tau (jvm/native-af-array-pointer)]
    (jvm/check! (qr/af-qr-inplace tau (jvm/af-handle in))
                "af-qr-inplace")
    (jvm/af-array-new (jvm/deref-af-array tau))))

(defn svd!
  "Perform in-place Singular Value Decomposition (SVD) of a matrix.
   
   This function modifies the input array directly, making it memory-efficient.
   The input matrix is destroyed during the computation.
   
   Parameters:
   - in: Input/output matrix (AFArray), destroyed during computation
   
   Returns:
   A vector containing three AFArray instances:
   - U: Left singular vectors
   - S: Singular values (as a diagonal matrix)
   - VT: Right singular vectors (transposed)"
  [^AFArray in]
  (let [u  (jvm/native-af-array-pointer)
        s  (jvm/native-af-array-pointer)
        vt (jvm/native-af-array-pointer)]
    (jvm/check! (svd/af-svd-inplace u s vt (jvm/af-handle in)) "af-svd-inplace")
    [(jvm/af-array-new (jvm/deref-af-array u))
     (jvm/af-array-new (jvm/deref-af-array s))
     (jvm/af-array-new (jvm/deref-af-array vt))]))

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
