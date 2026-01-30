(ns org.soulspace.arrayfire.integration.lapack
  "Integration of the ArrayFire LAPACK related FFI bindings with the error
   handling and resource management on the JVM."
  (:require [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.cholesky :as cholesky]
            [org.soulspace.arrayfire.ffi.det :as det]
            [org.soulspace.arrayfire.ffi.inverse :as inverse]
            [org.soulspace.arrayfire.ffi.pinverse :as pinverse]
            [org.soulspace.arrayfire.ffi.solve :as solve]
            [org.soulspace.arrayfire.ffi.norm :as norm]
            [org.soulspace.arrayfire.ffi.rank :as rank]
            [org.soulspace.arrayfire.ffi.lu :as lu]
            [org.soulspace.arrayfire.integration.jvm-integration :as jvm])
  (:import (org.soulspace.arrayfire.integration.jvm_integration AFArray)))

;;;
;;; Matrix Decompositions
;;;

(defn cholesky
  "Perform Cholesky decomposition of a positive definite matrix.
   
   The Cholesky decomposition factors a positive definite matrix A into:
   - A = L * L^H (lower triangular, when is-upper = false)
   - A = U^H * U (upper triangular, when is-upper = true)
   
   The input matrix must be square, positive definite, and of floating point
   or complex type (f32, f64, c32, c64).
   
   Parameters:
   - in: Input positive definite matrix (AFArray)
   - is-upper: Boolean, true for upper triangular U, false for lower triangular L (default false)
   
   Returns:
   A map containing:
   - :result - AFArray with the triangular matrix
   - :info - Integer status (0 if successful, otherwise rank at which decomposition fails)
   
   Example:
   (let [{:keys [result info]} (cholesky positive-def-matrix)]
     (when (zero? info)
       result))"
  ([^AFArray in]
   (cholesky in false))
  ([^AFArray in is-upper]
   (let [out (jvm/native-af-array-pointer)
         info-buf (mem/alloc 4)]
     (jvm/check! (cholesky/af-cholesky out info-buf (jvm/af-handle in) (if is-upper 1 0))
                 "af-cholesky")
     {:result (jvm/af-array-new (jvm/deref-af-array out))
      :info (mem/read-int info-buf 0)})))

(defn cholesky!
  "Perform in-place Cholesky decomposition of a positive definite matrix.
   
   This function modifies the input array directly. The output overwrites
   the input with the triangular matrix (upper or lower portion).
   
   Parameters:
   - in: Input/output positive definite matrix (AFArray), modified in place
   - is-upper: Boolean, true for upper triangular U, false for lower triangular L (default false)
   
   Returns:
   A map containing:
   - :result - The modified input AFArray
   - :info - Integer status (0 if successful, otherwise rank at which decomposition fails)"
  ([^AFArray in]
   (cholesky! in false))
  ([^AFArray in is-upper]
   (let [info-buf (mem/alloc 4)]
     (jvm/check! (cholesky/af-cholesky-inplace info-buf (jvm/af-handle in) (if is-upper 1 0))
                 "af-cholesky-inplace")
     {:result in
      :info (mem/read-int info-buf 0)})))

;;;
;;; Matrix Properties
;;;

(defn det
  "Compute the determinant of a square matrix.
   
   The determinant is computed via LU decomposition.
   
   Parameters:
   - in: Input square matrix (AFArray)
   
   Returns:
   For real arrays: the determinant as a double
   For complex arrays: [real imag] vector
   
   Example:
   (let [a (af/array [[1.0 2.0] [3.0 4.0]])
         d (det a)]
     d) ; => -2.0"
  [^AFArray in]
  (let [real-buf (mem/alloc 8)
        imag-buf (mem/alloc 8)]
    (jvm/check! (det/af-det real-buf imag-buf (jvm/af-handle in))
                "af-det")
    (let [real (mem/read-double real-buf 0)
          imag (mem/read-double imag-buf 0)]
      (if (zero? imag)
        real
        [real imag]))))

(defn rank
  "Compute the rank of a matrix.
   
   The rank is the number of linearly independent rows or columns,
   computed via QR decomposition with a numerical tolerance.
   
   Parameters:
   - in: Input matrix (AFArray)
   - tol: Tolerance for numerical rank (default 1e-5)
   
   Returns:
   Integer rank of the matrix
   
   Example:
   (let [a (af/array [[1.0 2.0] [2.0 4.0]]) ; rank-deficient
         r (rank a)]
     r) ; => 1"
  ([^AFArray in]
   (rank in 1e-5))
  ([^AFArray in tol]
   (let [rank-buf (mem/alloc 4)]
     (jvm/check! (rank/af-rank rank-buf (jvm/af-handle in) (double tol))
                 "af-rank")
     (mem/read-int rank-buf 0))))

;;;
;;; Norms
;;;

(defn norm
  "Compute the norm of a matrix or vector.
   
   Supports various norm types via the type parameter:
   - 0: AF_NORM_VECTOR_1 (L1 norm, sum of absolute values)
   - 1: AF_NORM_VECTOR_INF (L∞ norm, maximum absolute value)
   - 2: AF_NORM_VECTOR_2 (L2 norm, Euclidean norm)
   - 3: AF_NORM_VECTOR_P (Lp norm)
   - 4: AF_NORM_MATRIX_1 (Matrix L1 norm)
   - 5: AF_NORM_MATRIX_INF (Matrix L∞ norm)
   - 6: AF_NORM_MATRIX_2 (Matrix L2 norm, largest singular value)
   - 7: AF_NORM_MATRIX_L_PQ (Matrix Lp,q norm)
   - 8: AF_NORM_EUCLID (same as L2)
   
   Parameters:
   - in: Input array (AFArray)
   - norm-type: Type of norm to compute (integer constant, default 2 for L2)
   - p: Parameter for Lp norms (default 1.0)
   - q: Second parameter for Lp,q matrix norms (default 1.0)
   
   Returns:
   The computed norm as a double
   
   Example:
   (let [a (af/array [3.0 4.0])
         l2 (norm a 2)]
     l2) ; => 5.0"
  ([^AFArray in]
   (norm in 2 1.0 1.0))
  ([^AFArray in norm-type]
   (norm in norm-type 1.0 1.0))
  ([^AFArray in norm-type p]
   (norm in norm-type p 1.0))
  ([^AFArray in norm-type p q]
   (let [out-buf (mem/alloc 8)]
     (jvm/check! (norm/af-norm out-buf (jvm/af-handle in) (int norm-type) (double p) (double q))
                 "af-norm")
     (mem/read-double out-buf 0))))

;;;
;;; Matrix Inversion
;;;

(defn inverse
  "Compute the inverse of a square matrix.
   
   The matrix must be square and non-singular (det ≠ 0).
   Computed using LU decomposition with partial pivoting.
   
   For rectangular or singular matrices, use pinverse instead.
   
   Parameters:
   - in: Input square matrix (AFArray)
   - options: Optional map with:
     - :method - Algorithm selection (default 0 for auto)
   
   Returns:
   AFArray containing the inverse matrix
   
   Example:
   (let [a (af/array [[1.0 2.0] [3.0 4.0]])
         inv-a (inverse a)]
     inv-a)"
  ([^AFArray in]
   (inverse in {}))
  ([^AFArray in options]
   (let [out (jvm/native-af-array-pointer)
         method (get options :method 0)]
     (jvm/check! (inverse/af-inverse out (jvm/af-handle in) (int method))
                 "af-inverse")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn pinverse
  "Compute the pseudo-inverse (Moore-Penrose inverse) of a matrix.
   
   Works for rectangular and singular matrices. Provides best-fit
   solution to linear systems that may be overdetermined, underdetermined,
   or inconsistent.
   
   Computed via SVD (Singular Value Decomposition).
   
   Parameters:
   - in: Input matrix (AFArray) - can be rectangular
   - tol: Tolerance for singular values (default 1e-6)
   - options: Optional map with:
     - :method - Algorithm selection (default 0 for auto)
   
   Returns:
   AFArray containing the pseudo-inverse
   
   Example:
   (let [a (af/array [[1.0 2.0] [2.0 4.0]]) ; singular
         pinv-a (pinverse a)]
     pinv-a)"
  ([^AFArray in]
   (pinverse in 1e-6 {}))
  ([^AFArray in tol]
   (pinverse in tol {}))
  ([^AFArray in tol options]
   (let [out (jvm/native-af-array-pointer)
         method (get options :method 0)]
     (jvm/check! (pinverse/af-pinverse out (jvm/af-handle in) (double tol) (int method))
                 "af-pinverse")
     (jvm/af-array-new (jvm/deref-af-array out)))))

;;;
;;; Linear System Solving
;;;

(defn solve
  "Solve a system of linear equations: A·x = b
   
   Supports square, overdetermined, and underdetermined systems.
   Uses appropriate decomposition based on matrix properties:
   - Square: LU decomposition
   - Overdetermined (m > n): QR decomposition (least squares)
   - Underdetermined (m < n): LQ decomposition
   - Triangular: Direct triangular solve
   
   Parameters:
   - a: Coefficient matrix (AFArray)
   - b: Right-hand side vector/matrix (AFArray)
   - options: Optional map with:
     - :method - Matrix property hint (0=auto, 1=lower triangular, 2=upper triangular)
   
   Returns:
   AFArray containing the solution x
   
   Example:
   (let [a (af/array [[1.0 2.0] [3.0 4.0]])
         b (af/array [5.0 11.0])
         x (solve a b)]
     x) ; => [1.0 2.0]"
  ([^AFArray a ^AFArray b]
   (solve a b {}))
  ([^AFArray a ^AFArray b options]
   (let [out (jvm/native-af-array-pointer)
         method (get options :method 0)]
     (jvm/check! (solve/af-solve out (jvm/af-handle a) (jvm/af-handle b) (int method))
                 "af-solve")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn solve-lu
  "Solve a system using pre-computed LU decomposition.
   
   Efficient when solving multiple systems with the same coefficient matrix.
   Use with lu decomposition from integration.algorithm namespace.
   
   Parameters:
   - a: LU factorized coefficient matrix (AFArray from af-lu)
   - pivot: Pivot array (AFArray from af-lu)
   - b: Right-hand side vector/matrix (AFArray)
   - options: Optional map with:
     - :method - Matrix property hint (default 0)
   
   Returns:
   AFArray containing the solution x
   
   Example:
   (let [[l u p] (algorithm/lu a)
         x (solve-lu u p b)]
     x)"
  ([^AFArray a ^AFArray pivot ^AFArray b]
   (solve-lu a pivot b {}))
  ([^AFArray a ^AFArray pivot ^AFArray b options]
   (let [out (jvm/native-af-array-pointer)
         method (get options :method 0)]
     (jvm/check! (solve/af-solve-lu out (jvm/af-handle a) (jvm/af-handle pivot) (jvm/af-handle b) (int method))
                 "af-solve-lu")
     (jvm/af-array-new (jvm/deref-af-array out)))))

;;;
;;; Utility Functions
;;;

(defn lapack-available?
  "Check if LAPACK support is available in the current ArrayFire build.
   
   LAPACK functions require ArrayFire to be built with LAPACK support.
   This function checks whether the underlying backend has LAPACK available.
   
   Returns:
   Boolean - true if LAPACK is available, false otherwise
   
   Example:
   (when (lapack-available?)
     (println \"LAPACK operations are supported\"))"
  []
  (let [available-buf (mem/alloc 1)]
    (jvm/check! (lu/af-is-lapack-available available-buf)
                "af-is-lapack-available")
    (not (zero? (mem/read-byte available-buf 0)))))

