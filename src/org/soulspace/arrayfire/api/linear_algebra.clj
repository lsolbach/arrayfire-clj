(ns org.soulspace.arrayfire.api.linear-algebra
  "Idiomatic Clojure linear algebra API for ArrayFire arrays.

   Provides GPU-accelerated linear algebra operations including:

   BLAS operations:
   - General matrix multiply with scalars: `gemm`
   - Matrix multiply variants: `matmul-nt`, `matmul-tn`, `matmul-tt`, `matmul3`, `matmul4`
   - Dot product with immediate result: `dot-all`
   - Transpose: `transpose`, `transpose!`, `transpose-conjugate`, `transpose-conjugate!`

   Matrix decompositions (LAPACK):
   - LU factorization:    `lu`, `lu!`
   - QR factorization:    `qr`, `qr!`
   - SVD:                 `svd`, `svd!`
   - Cholesky:            `cholesky`, `cholesky!`

   Linear system solvers:
   - `solve`     — Ax = b (direct)
   - `solve-lu`  — Ax = b using pre-factored LU

   Matrix properties:
   - `inverse`, `pinverse`, `det`, `matrix-rank`, `norm`

   Utilities:
   - `lapack-available?`

   All functions must be called within a `with-arrayfire` region from
   `org.soulspace.arrayfire.api.core`."
  (:require [org.soulspace.arrayfire.api.core :as core :refer [assert-within-arrayfire!]]
            [org.soulspace.arrayfire.integration.base.definitions :as defs]
            [org.soulspace.arrayfire.integration.unified-api.blas :as blas]
            [org.soulspace.arrayfire.integration.unified-api.lapack :as lapack])
  (:import (org.soulspace.arrayfire.integration.base.resource AFArray)))

;;;
;;; Private keyword→integer constant maps
;;;

(def ^:private solve-options-kw->method
  "Map from solve option keywords to the :method integer used by the integration layer.
   The integration layer for solve/inverse takes a map {:method int}."
  {:none  0
   :lower 1
   :upper 2})

(defn- solve-options-map
  "Build the {:method int} options map expected by the integration solve/inverse functions."
  [options-kw]
  {:method (get solve-options-kw->method options-kw 0)})

;;;
;;; Utilities
;;;

(defn lapack-available?
  "Return true if LAPACK is available on the current ArrayFire backend.

   LAPACK is required by: `lu`, `qr`, `svd`, `cholesky`, `solve`, `inverse`,
   `pinverse`, `det`, `matrix-rank`, `norm`.

   Returns:
     `true` if LAPACK is available, `false` otherwise."
  []
  (lapack/lapack-available?))

;;;
;;; BLAS operations
;;;

(defn transpose
  "Transpose a matrix, optionally computing the conjugate (Hermitian) transpose.

   Delegates to `org.soulspace.arrayfire.api.core/transpose`.

   Parameters:
     arr        — AFArray to transpose
     conjugate? — (optional) if true, also conjugate elements; default false

   Returns:
     New transposed AFArray."
  (^AFArray [^AFArray arr]
   (core/transpose arr))
  (^AFArray [^AFArray arr conjugate?]
   (core/transpose arr conjugate?)))

(defn transpose!
  "Transpose a matrix in-place, optionally computing the conjugate (Hermitian) transpose.

   Parameters:
     arr        — AFArray to transpose in-place (mutated)
     conjugate? — (optional) if true, also conjugate elements; default false

   Returns:
     The (mutated) `arr`."
  (^AFArray [^AFArray arr]
   (transpose! arr false))
  (^AFArray [^AFArray arr conjugate?]
   (assert-within-arrayfire! "transpose!")
   (blas/transpose! arr conjugate?)))

(defn transpose-conjugate
  "Compute the conjugate (Hermitian) transpose Aᴴ of a matrix.

   For complex matrices this transposes and conjugates each element (Aᴴ).
   For real matrices this is equivalent to a plain `transpose`.

   Parameters:
     arr — AFArray to conjugate-transpose

   Returns:
     New conjugate-transposed AFArray."
  ^AFArray [^AFArray arr]
  (core/transpose arr true))

(defn transpose-conjugate!
  "Compute the conjugate (Hermitian) transpose Aᴴ of a matrix in-place.

   For complex matrices transposes and conjugates each element in-place.
   For real matrices this is equivalent to `transpose!`.
   Requires a square matrix (rows = cols).

   Parameters:
     arr — square AFArray to conjugate-transpose in-place (mutated)

   Returns:
     The (mutated) `arr`."
  ^AFArray [^AFArray arr]
  (transpose! arr true))

(defn gemm
  "General matrix multiply: result = alpha * op(A) * op(B) + beta * result.

   op is controlled by the `op-a` and `op-b` keyword options:
     :none   — no transposition (default)
     :trans  — transpose
     :ctrans — conjugate transpose

   Parameters:
     op-a  — matrix property keyword for A (:none, :trans, :ctrans)
     op-b  — matrix property keyword for B (:none, :trans, :ctrans)
     alpha — scalar multiplier for A*B (double)
     a     — left AFArray matrix
     b     — right AFArray matrix
     beta  — scalar multiplier for accumulation (0.0 for no accumulation)

   Returns:
     New AFArray result."
  ^AFArray [op-a op-b alpha ^AFArray a ^AFArray b beta]
  (assert-within-arrayfire! "gemm")
  (blas/gemm (defs/resolve-mat-prop op-a)
             (defs/resolve-mat-prop op-b)
             (double alpha)
             a
             b
             (double beta)))

(defn matmul-nt
  "Matrix multiplication A × Bᵀ (B transposed).

   Convenience wrapper for matmul with the second matrix transposed.

   Parameters:
     lhs — left-hand AFArray matrix A [m×k]
     rhs — right-hand AFArray matrix B [n×k], will be transposed to [k×n]

   Returns:
     New AFArray result [m×n]."
  ^AFArray [^AFArray lhs ^AFArray rhs]
  (assert-within-arrayfire! "matmul-nt")
  (blas/matmul-nt lhs rhs))

(defn matmul-tn
  "Matrix multiplication Aᵀ × B (A transposed).

   Convenience wrapper for matmul with the first matrix transposed.

   Parameters:
     lhs — left-hand AFArray matrix A [k×m], will be transposed to [m×k]
     rhs — right-hand AFArray matrix B [k×n]

   Returns:
     New AFArray result [m×n]."
  ^AFArray [^AFArray lhs ^AFArray rhs]
  (assert-within-arrayfire! "matmul-tn")
  (blas/matmul-tn lhs rhs))

(defn matmul-tt
  "Matrix multiplication Aᵀ × Bᵀ (both transposed).

   Convenience wrapper for matmul with both matrices transposed.

   Parameters:
     lhs — left-hand AFArray matrix A [k×m], will be transposed
     rhs — right-hand AFArray matrix B [n×k], will be transposed

   Returns:
     New AFArray result [m×n]."
  ^AFArray [^AFArray lhs ^AFArray rhs]
  (assert-within-arrayfire! "matmul-tt")
  (blas/matmul-tt lhs rhs))

(defn matmul3
  "Chain matrix multiplication of three matrices: (A × B) × C.

   Parameters:
     a — first AFArray matrix [m×k]
     b — second AFArray matrix [k×n]
     c — third AFArray matrix [n×p]

   Returns:
     New AFArray result [m×p]."
  ^AFArray [^AFArray a ^AFArray b ^AFArray c]
  (assert-within-arrayfire! "matmul3")
  (blas/matmul3 a b c))

(defn matmul4
  "Chain matrix multiplication of four matrices: ((A × B) × C) × D.

   Parameters:
     a — first AFArray matrix [m×k]
     b — second AFArray matrix [k×n]
     c — third AFArray matrix [n×p]
     d — fourth AFArray matrix [p×q]

   Returns:
     New AFArray result [m×q]."
  ^AFArray [^AFArray a ^AFArray b ^AFArray c ^AFArray d]
  (assert-within-arrayfire! "matmul4")
  (blas/matmul4 a b c d))

(defn dot-all
  "Dot (inner) product of two vectors, returning the scalar result directly.

   For real arrays returns a `double`. For complex arrays returns `[real imag]`.
   Unlike `dot` in `core`, this extracts the scalar immediately without
   creating a single-element AFArray.

   Parameters:
     lhs      — left-hand AFArray vector
     rhs      — right-hand AFArray vector
     opt-lhs  — (optional) matrix property keyword for lhs (:none or :conj); default :none
     opt-rhs  — (optional) matrix property keyword for rhs (:none or :conj); default :none

   Returns:
     For real inputs: a `double`.
     For complex inputs: `[real imag]` vector."
  ([^AFArray lhs ^AFArray rhs]
   (dot-all lhs rhs :none :none))
  ([^AFArray lhs ^AFArray rhs opt-lhs opt-rhs]
   (assert-within-arrayfire! "dot-all")
   (blas/dot-all lhs rhs (defs/resolve-mat-prop opt-lhs) (defs/resolve-mat-prop opt-rhs))))

;;;
;;; LAPACK matrix decompositions
;;;

(defn lu
  "LU decomposition of matrix `a` with partial pivoting.

   Decomposes `a` into matrices such that P * A = L * U, where:
   - L — lower triangular matrix with unit diagonal
   - U — upper triangular matrix
   - pivot — permutation pivot vector

   Parameters:
     a — AFArray matrix to factorize (should be square)

   Returns:
     Map {:lower L :upper U :pivot pivot}
     where L, U, and pivot are AFArrays."
  [^AFArray a]
  (assert-within-arrayfire! "lu")
  (let [[lower upper pivot] (lapack/lu a)]
    {:lower lower :upper upper :pivot pivot}))

(defn lu!
  "LU decomposition of `in` in-place; overwrites `in` with combined L+U data.

   More memory-efficient than `lu` for large matrices, at the cost of
   destroying the original matrix contents.

   Parameters:
     in            — AFArray to factorize in-place (mutated)
     lapack-pivot? — (optional) use LAPACK-style pivot indexing; default false

   Returns:
     Pivot AFArray."
  (^AFArray [^AFArray in]
   (lu! in false))
  (^AFArray [^AFArray in lapack-pivot?]
   (assert-within-arrayfire! "lu!")
   (lapack/lu! in lapack-pivot?)))

(defn qr
  "QR decomposition of matrix `a`.

   Decomposes `a` into Q * R where:
   - Q — orthogonal matrix
   - R — upper triangular matrix
   - tau — Householder reflector scalars (needed for qr!)

   Parameters:
     a — AFArray matrix to factorize (may be rectangular)

   Returns:
     Map {:q Q :r R :tau tau} where Q, R, and tau are AFArrays."
  [^AFArray a]
  (assert-within-arrayfire! "qr")
  (let [[q r tau] (lapack/qr a)]
    {:q q :r r :tau tau}))

(defn qr!
  "QR decomposition of `in` in-place (overwrites `in` with packed QR form).

   Parameters:
     in — AFArray matrix to factorize in-place (mutated)

   Returns:
     tau AFArray (Householder reflector scalars)."
  ^AFArray [^AFArray in]
  (assert-within-arrayfire! "qr!")
  (lapack/qr! in))

(defn svd
  "Singular Value Decomposition of matrix `a`.

   Decomposes `a` into U × diag(S) × VT where:
   - U  — left singular vectors (orthogonal/unitary)
   - S  — singular values in descending order (diagonal of Σ)
   - VT — right singular vectors, already transposed (orthogonal/unitary)

   Parameters:
     a — AFArray matrix to decompose (may be rectangular)

   Returns:
     Map {:u U :s S :vt VT} where U, S, and VT are AFArrays."
  [^AFArray a]
  (assert-within-arrayfire! "svd")
  (let [[u s vt] (lapack/svd a)]
    {:u u :s s :vt vt}))

(defn svd!
  "SVD of `in` in-place; destroys `in` during computation (more memory-efficient).

   Parameters:
     in — AFArray matrix (will be overwritten/destroyed)

   Returns:
     Map {:u U :s S :vt VT} where U, S, and VT are AFArrays."
  [^AFArray in]
  (assert-within-arrayfire! "svd!")
  (let [[u s vt] (lapack/svd! in)]
    {:u u :s s :vt vt}))

(defn cholesky
  "Cholesky decomposition of symmetric positive-definite matrix `in`.

   Factors A into:
   - L × Lᴴ  when `upper?` is false (default) — lower triangular L
   - Uᴴ × U  when `upper?` is true — upper triangular U

   Parameters:
     in     — symmetric positive-definite AFArray matrix
     upper? — (optional) compute upper triangular factor; default false (lower)

   Returns:
     Map {:result factor :info info :upper? up?}
     where `factor` is the triangular AFArray and `info` is 0 on success.
     A positive `info` indicates the matrix is not positive-definite at that
     leading minor."
  ([^AFArray in]
   (cholesky in false))
  ([^AFArray in upper?]
   (assert-within-arrayfire! "cholesky")
   (let [{:keys [result info]} (lapack/cholesky in (boolean upper?))]
     {:result result :info info :upper? (boolean upper?)})))

(defn cholesky!
  "Cholesky decomposition of `in` in-place (overwrites `in` with triangular factor).

   Parameters:
     in     — symmetric positive-definite AFArray matrix (mutated)
     upper? — (optional) compute upper triangular factor; default false (lower)

   Returns:
     Map {:result in :info info :upper? up?} where `result` is the mutated `in`."
  ([^AFArray in]
   (cholesky! in false))
  ([^AFArray in upper?]
   (assert-within-arrayfire! "cholesky!")
   (let [{:keys [result info]} (lapack/cholesky! in (boolean upper?))]
     {:result result :info info :upper? (boolean upper?)})))

;;;
;;; Linear system solvers
;;;

(defn solve
  "Solve the linear system Ax = b for x.

   Automatically selects the decomposition based on matrix shape:
   - Square matrix: LU decomposition
   - Overdetermined (rows > cols): QR decomposition (least-squares solution)
   - Underdetermined (rows < cols): LQ decomposition

   Pass `:lower` or `:upper` as `options` to hint that `a` is already triangular,
   enabling a faster direct triangular solve.

   Parameters:
     a       — coefficient AFArray matrix
     b       — right-hand-side AFArray (vector or matrix of multiple RHS columns)
     options — (optional) keyword hint: :none (default), :lower, :upper

   Returns:
     Solution AFArray x satisfying Ax ≈ b."
  (^AFArray [^AFArray a ^AFArray b]
   (solve a b :none))
  (^AFArray [^AFArray a ^AFArray b options]
   (assert-within-arrayfire! "solve")
   (lapack/solve a b (solve-options-map options))))

(defn solve-lu
  "Solve the linear system Ax = b reusing a pre-computed LU factorization.

   Use after `lu` or `lu!` to efficiently solve multiple systems Ax = b₁, Ax = b₂…
   with the same coefficient matrix without re-factorizing.

   Parameters:
     a       — LU-factored AFArray (from `lu!` in-place result, or L from `lu`)
     pivot   — pivot AFArray (from `lu` {:pivot …} or `lu!` return value)
     b       — right-hand-side AFArray
     options — (optional) keyword hint: :none (default)

   Returns:
     Solution AFArray x."
  (^AFArray [^AFArray a ^AFArray pivot ^AFArray b]
   (solve-lu a pivot b :none))
  (^AFArray [^AFArray a ^AFArray pivot ^AFArray b options]
   (assert-within-arrayfire! "solve-lu")
   (lapack/solve-lu a pivot b (solve-options-map options))))

;;;
;;; Matrix properties
;;;

(defn inverse
  "Compute the inverse of a square non-singular matrix.

   Computed using LU decomposition with partial pivoting.
   For rectangular or singular matrices, use `pinverse`.

   Parameters:
     in — square non-singular AFArray matrix

   Returns:
     Inverse AFArray A⁻¹ such that A × A⁻¹ ≈ I."
  ^AFArray [^AFArray in]
  (assert-within-arrayfire! "inverse")
  (lapack/inverse in {}))

(defn pinverse
  "Compute the Moore-Penrose pseudo-inverse of a matrix.

   Works for rectangular or singular matrices. Uses SVD internally.
   Singular values below `tol` (relative to the largest singular value)
   are treated as zero, stabilising the inversion.

   Parameters:
     in  — AFArray matrix (any shape, real or complex)
     tol — (optional) singular-value truncation tolerance; default 1e-6

   Returns:
     Pseudo-inverse AFArray."
  (^AFArray [^AFArray in]
   (pinverse in 1e-6))
  (^AFArray [^AFArray in tol]
   (assert-within-arrayfire! "pinverse")
   (lapack/pinverse in (double tol) {})))

(defn det
  "Compute the determinant of a square matrix.

   Computed via LU decomposition.

   Parameters:
     in — square AFArray matrix

   Returns:
     For real matrices: a `double`.
     For complex matrices: a vector `[real imag]` of doubles."
  [^AFArray in]
  (assert-within-arrayfire! "det")
  (lapack/det in))

(defn matrix-rank
  "Compute the numerical rank of a matrix.

   Uses QR decomposition with column pivoting. Diagonal elements of R whose
   absolute value falls below `tol` are treated as linearly dependent.

   Parameters:
     in  — AFArray matrix
     tol — (optional) tolerance for zero detection; default 1e-5 (ArrayFire default)

   Returns:
     Rank as a JVM `int`."
  ([^AFArray in]
   (assert-within-arrayfire! "matrix-rank")
   (lapack/rank in))
  ([^AFArray in tol]
   (assert-within-arrayfire! "matrix-rank")
   (lapack/rank in (double tol))))

(defn norm
  "Compute the norm of a vector or matrix.

   `norm-type` keyword selects the norm:

   Vector norms (for 1-D arrays):
     :vector-1   — L¹ norm: Σ |xᵢ|
     :vector-2   — L² (Euclidean) norm: √(Σ xᵢ²)  (default)
     :vector-inf — L∞ norm: max |xᵢ|
     :vector-p   — Lᵖ norm: (Σ |xᵢ|^p)^(1/p); requires `p`
     :euclid     — same as :vector-2

   Matrix norms (for 2-D arrays):
     :matrix-1   — maximum absolute column sum
     :matrix-inf — maximum absolute row sum
     :matrix-2   — spectral norm (largest singular value)
     :matrix-lpq — L_{p,q} norm; requires both `p` and `q`

   Parameters:
     in        — AFArray (vector or matrix)
     norm-type — (optional) keyword; default :vector-2
     p         — (optional) p parameter for :vector-p or :matrix-lpq; default 1.0
     q         — (optional) q parameter for :matrix-lpq; default 1.0

   Returns:
     Norm as a `double`."
  ([^AFArray in]
   (norm in :vector-2))
  ([^AFArray in norm-type]
   (assert-within-arrayfire! "norm")
   (lapack/norm in (defs/resolve-norm-type norm-type)))
  ([^AFArray in norm-type p]
   (assert-within-arrayfire! "norm")
   (lapack/norm in (defs/resolve-norm-type norm-type) (double p)))
  ([^AFArray in norm-type p q]
   (assert-within-arrayfire! "norm")
   (lapack/norm in (defs/resolve-norm-type norm-type) (double p) (double q))))


(comment
  ;; Linear algebra REPL experiments
  ;; All examples must be called inside (with-arrayfire ...).
  ;; Load and require:
  ;;   (require '[org.soulspace.arrayfire.api.core :as af] :reload)
  ;;
  ;; Note: af/array takes [flat-values dims dtype].
  ;; ArrayFire uses COLUMN-MAJOR order:
  ;;   [[2 1] [1 3]] (row-major) → stored as [2 1 1 3] col-major

  ;; LAPACK availability
  (af/with-arrayfire
    (lapack-available?))

  ;; --- transpose ---
  ;; A = [[1 2 3]   (2×3, col-major: [1 4 2 5 3 6])
  ;;      [4 5 6]]
  (af/with-arrayfire
    (let [a (af/array [1.0 4.0 2.0 5.0 3.0 6.0] [2 3] :f32)]
      (af/->value (transpose a))))
  ;; => shape [3 2]: [[1.0 4.0] [2.0 5.0] [3.0 6.0]]

  ;; --- gemm: C = 2 * A * I ---
  (af/with-arrayfire
    (let [a (af/array [1.0 3.0 2.0 4.0] [2 2] :f32)  ; [[1 2][3 4]] col-major
          b (af/array [1.0 0.0 0.0 1.0] [2 2] :f32)]  ; identity
      (af/->value (gemm :none :none 2.0 a b 0.0))))
  ;; => [[2.0 4.0] [6.0 8.0]]

  ;; --- dot-all ---
  (af/with-arrayfire
    (let [u (af/array [1.0 2.0 3.0] [3] :f64)
          v (af/array [4.0 5.0 6.0] [3] :f64)]
      (dot-all u v)))
  ;; => 32.0

  ;; --- LU decomposition ---
  ;; A = [[4 3][6 3]] col-major: [4 6 3 3]
  (af/with-arrayfire
    (let [a (af/array [4.0 6.0 3.0 3.0] [2 2] :f64)
          {:keys [lower upper pivot]} (lu a)]
      {:L     (af/->value lower)
       :U     (af/->value upper)
       :pivot (af/->value pivot)}))

  ;; --- QR decomposition ---
  ;; A = [[1 2][3 4][5 6]] (3×2) col-major: [1 3 5 2 4 6]
  (af/with-arrayfire
    (let [a (af/array [1.0 3.0 5.0 2.0 4.0 6.0] [3 2] :f64)
          {:keys [q r tau]} (qr a)]
      {:Q (af/->value q) :R (af/->value r)}))

  ;; --- SVD: singular values of a 2x3 matrix ---
  ;; A = [[1 2 3][4 5 6]] (2×3) col-major: [1 4 2 5 3 6]
  (af/with-arrayfire
    (let [a (af/array [1.0 4.0 2.0 5.0 3.0 6.0] [2 3] :f64)
          {:keys [u s vt]} (svd a)]
      {:singular-values (af/->value s)}))

  ;; --- Cholesky decomposition ---
  ;; A = [[4 2][2 3]] (symmetric, pos-def) col-major: [4 2 2 3]
  (af/with-arrayfire
    (let [a (af/array [4.0 2.0 2.0 3.0] [2 2] :f64)
          {:keys [result info]} (cholesky a)]
      {:info info :L (af/->value result)}))
  ;; => {:info 0, :L [[2.0 0.0][1.0 1.414...]]}

  ;; --- Solve Ax = b ---
  ;; A = [[2 1][1 3]] col-major: [2 1 1 3], b = [5 10]
  (af/with-arrayfire
    (let [a (af/array [2.0 1.0 1.0 3.0] [2 2] :f64)
          b (af/array [5.0 10.0] [2] :f64)]
      (af/->value (solve a b))))
  ;; => [1.0 3.0]  (since 2*1 + 1*3 = 5, 1*1 + 3*3 = 10)

  ;; --- Solve with pre-factored LU ---
  (af/with-arrayfire
    (let [a     (af/array [2.0 1.0 1.0 3.0] [2 2] :f64)
          {:keys [lower upper pivot]} (lu a)
          b     (af/array [5.0 10.0] [2] :f64)]
      (af/->value (solve-lu lower pivot b))))
  ;; => [1.0 3.0]

  ;; --- Inverse ---
  ;; A = [[1 2][3 4]] col-major: [1 3 2 4]
  (af/with-arrayfire
    (let [a (af/array [1.0 3.0 2.0 4.0] [2 2] :f64)]
      (af/->value (inverse a))))
  ;; => [[-2.0 1.5] [1.0 -0.5]]

  ;; --- Pseudo-inverse of a rectangular matrix ---
  ;; A = [[1 2 3][4 5 6]] (2×3) col-major: [1 4 2 5 3 6]
  (af/with-arrayfire
    (let [a (af/array [1.0 4.0 2.0 5.0 3.0 6.0] [2 3] :f64)]
      (af/shape (pinverse a))))
  ;; => [3 2 1 1] — pseudo-inverse of a 2×3 is 3×2

  ;; --- Determinant ---
  ;; A = [[1 2][3 4]] col-major: [1 3 2 4]
  (af/with-arrayfire
    (let [a (af/array [1.0 3.0 2.0 4.0] [2 2] :f64)]
      (det a)))
  ;; => -2.0

  ;; --- Matrix rank ---
  ;; A = [[1 2 3][2 4 6][1 1 1]] rank 2 (row 2 = 2 * row 1)
  ;; col-major: [1 2 1  2 4 1  3 6 1]
  (af/with-arrayfire
    (let [a (af/array [1.0 2.0 1.0 2.0 4.0 1.0 3.0 6.0 1.0] [3 3] :f64)]
      (matrix-rank a)))
  ;; => 2

  ;; --- Norms: l1, l2, l-inf of vector [3 4] ---
  (af/with-arrayfire
    (let [v (af/array [3.0 4.0] [2] :f64)]
      {:l1   (norm v :vector-1)
       :l2   (norm v :vector-2)
       :linf (norm v :vector-inf)}))
  ;; => {:l1 7.0, :l2 5.0, :linf 4.0}

  ;; --- Matrix 1-norm: max col-sum of [[1 2][3 4]] = max(1+3, 2+4) = 6.0 ---
  ;; col-major: [1 3 2 4]
  ;; Note: :matrix-2 (spectral norm) is not supported on all backends.
  (af/with-arrayfire
    (let [m (af/array [1.0 3.0 2.0 4.0] [2 2] :f64)]
      (norm m :matrix-1)))

  ;
  )
