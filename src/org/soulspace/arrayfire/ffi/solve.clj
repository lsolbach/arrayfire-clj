(ns org.soulspace.arrayfire.ffi.solve
  "ArrayFire FFI bindings for solving systems of linear equations.

  Linear systems of equations are fundamental in scientific computing, appearing
  in numerical methods, optimization, physics simulations, and machine learning.
  ArrayFire provides GPU-accelerated solvers for various matrix types.

  ## Mathematical Foundation

  A linear system of equations has the form:
  ```
  A·x = b
  ```
  where:
  - A is an m×n coefficient matrix
  - x is an n×1 vector of unknowns
  - b is an m×1 vector of measured values

  ### Solution Methods

  1. **General Solver (af_solve)**:
     - Square systems (m = n): Uses LU decomposition with partial pivoting
     - Overdetermined systems (m > n): Uses QR decomposition (least squares)
     - Underdetermined systems (m < n): Uses LQ decomposition
     - Triangular systems: Direct triangular solve (fast)

  2. **LU-based Solver (af_solve_lu)**:
     - Uses pre-computed LU decomposition with pivot array
     - Efficient when solving multiple systems with same coefficient matrix
     - Two-step process: (1) af_lu to get LU + pivots, (2) af_solve_lu

  ### Triangular Systems

  For triangular matrices, specify matrix properties for optimized solving:
  - AF_MAT_LOWER: Lower triangular (forward substitution)
  - AF_MAT_UPPER: Upper triangular (backward substitution)

  Forward substitution (lower triangular):
  ```
  L·x = b  where L[i,j] = 0 for j > i
  x[0] = b[0] / L[0,0]
  x[i] = (b[i] - Σ(L[i,j] * x[j])) / L[i,i]  for j < i
  ```

  Backward substitution (upper triangular):
  ```
  U·x = b  where U[i,j] = 0 for j < i
  x[n-1] = b[n-1] / U[n-1,n-1]
  x[i] = (b[i] - Σ(U[i,j] * x[j])) / U[i,i]  for j > i
  ```

  ## Computational Complexity

  - General solve: O(n³) for first system, O(n²) for additional systems
  - Triangular solve: O(n²)
  - LU decomposition (one-time): O(n³)
  - Solve with pre-computed LU: O(n²)

  ## Numerical Stability

  - Uses partial pivoting for numerical stability
  - Condition number of A affects solution accuracy
  - Ill-conditioned matrices (high condition number) may yield inaccurate results
  - For overdetermined systems, provides least-squares solution

  ## Supported Types

  - Single precision: f32, c32 (complex float)
  - Double precision: f64, c64 (complex double)

  ## Performance Notes

  - GPU acceleration provides 10-100× speedup for large matrices
  - Batch operations solve multiple systems in parallel
  - Pre-compute LU decomposition when solving repeatedly with same A
  - Triangular solvers are significantly faster than general solvers

  ## Applications

  - Numerical methods (finite elements, finite differences)
  - Physics simulations (circuit analysis, heat transfer)
  - Machine learning (linear regression, neural networks)
  - Computer graphics (mesh deformation, animation)
  - Signal processing (filter design, system identification)
  - Optimization (quadratic programming, trust regions)

  See also:
  - LU decomposition (af_lu) for factorization
  - Inverse matrix (af_inverse) for matrix inversion
  - Least squares (af_lstsq) for overdetermined systems"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem :refer [deref-ptr]])
  (:refer-clojure :exclude [deref]))

;;
;; Linear System Solvers
;;

(defcfn af-solve
  "Solve a system of linear equations A·x = b.

  Solves linear systems using the appropriate method based on matrix properties:
  - Square matrices: LU decomposition with partial pivoting
  - Overdetermined: QR decomposition (least squares)
  - Underdetermined: LQ decomposition
  - Triangular: Direct triangular solve (if options specify)

  Parameters:
  - x: Output array for solution vector/matrix
  - a: Coefficient matrix A (m×n)
  - b: Right-hand side vector/matrix b (m×k)
  - options: Matrix properties (af_mat_prop enum)
    * AF_MAT_NONE (0): General matrix (default)
    * AF_MAT_LOWER (2): Lower triangular matrix
    * AF_MAT_UPPER (4): Upper triangular matrix

  Returns:
  Error code indicating success or failure.

  Matrix Dimensions:
  - a: [m, n] - coefficient matrix
  - b: [m, k] - right-hand side (k systems solved in parallel)
  - x: [n, k] - solution matrix

  Example (single system):
  ```clojure
  ;; Solve A·x = b for x
  (let [A (af-constant [3 3] [[2 1 1] [4 -6 0] [-2 7 2]] :f64)
        b (af-constant [3 1] [5 -2 9] :f64)
        x (mem/alloc-instance ::mem/pointer)]
    (af-solve x A b 0) ;; AF_MAT_NONE
    ;; x now contains the solution
    )
  ```

  Example (multiple systems):
  ```clojure
  ;; Solve A·x1 = b1, A·x2 = b2, A·x3 = b3 in parallel
  (let [A (af-constant [100 100] matrix-data :f64)
        B (af-constant [100 3] rhs-data :f64)  ;; 3 systems
        X (mem/alloc-instance ::mem/pointer)]
    (af-solve X A B 0)
    ;; X contains [x1, x2, x3] as columns
    )
  ```

  Example (triangular system):
  ```clojure
  ;; Fast solve for upper triangular matrix
  (let [U (af-constant [50 50] upper-tri-data :f32)
        b (af-constant [50 1] rhs-data :f32)
        x (mem/alloc-instance ::mem/pointer)]
    (af-solve x U b 4) ;; AF_MAT_UPPER for O(n²) solve
    )
  ```

  Batch Operations:
  For batch solving, dimensions 2 and 3 of a and b must match:
  - a: [m, n, p, q] - p×q batches of m×n matrices
  - b: [m, k, p, q] - p×q batches of m×k vectors
  - x: [n, k, p, q] - p×q batches of solutions

  Type Support:
  - f32, f64: Real single/double precision
  - c32, c64: Complex single/double precision

  Performance Tips:
  - Use AF_MAT_LOWER or AF_MAT_UPPER for triangular matrices
  - Pre-compute LU decomposition for repeated solves (see af-solve-lu)
  - Batch multiple systems for better GPU utilization

  Limitations:
  - Not supported in GFOR (gfor loops)
  - AF_MAT_TRANS and AF_MAT_CTRANS not yet supported for triangular solves

  Notes:
  - Solution accuracy depends on matrix condition number
  - Ill-conditioned matrices may produce inaccurate results
  - For overdetermined systems, returns least-squares solution
  - Empty arrays (ndims=0) return empty output

  See also:
  - af_solve (ArrayFire C API)
  - af-solve-lu: Solve using pre-computed LU decomposition
  - af-lu: Compute LU decomposition
  - af-inverse: Compute matrix inverse"
  "af_solve" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

(defcfn af-solve-lu
  "Solve a system of linear equations using pre-computed LU decomposition.

  Solves A·x = b where A has been decomposed as P·L·U (LU with pivoting).
  This is more efficient than af-solve when solving multiple systems with
  the same coefficient matrix.

  Parameters:
  - x: Output array for solution vector/matrix
  - a: Packed LU decomposition from af_lu (m×m matrix)
  - piv: Pivot array from af_lu (m-element vector)
  - b: Right-hand side vector/matrix (m×k)
  - options: Matrix properties (currently must be AF_MAT_NONE)

  Returns:
  Error code indicating success or failure.

  Two-Step Process:
  1. Compute LU decomposition once: af_lu(lower, upper, pivot, input)
  2. Solve repeatedly: af_solve_lu(x, lower, pivot, b, AF_MAT_NONE)

  Matrix Dimensions:
  - a: [m, m] - packed LU decomposition (square matrix required)
  - piv: [m] - pivot indices from LU decomposition
  - b: [m, k] - right-hand side (k systems in parallel)
  - x: [m, k] - solution matrix

  Example (single solve with LU):
  ```clojure
  ;; Decompose once, solve once
  (let [A (af-constant [100 100] matrix-data :f64)
        b (af-constant [100 1] rhs-data :f64)
        lower (mem/alloc-instance ::mem/pointer)
        upper (mem/alloc-instance ::mem/pointer)
        pivot (mem/alloc-instance ::mem/pointer)
        x (mem/alloc-instance ::mem/pointer)]
    ;; Step 1: Compute LU decomposition
    (af-lu lower upper pivot A)
    ;; Step 2: Solve using LU
    (af-solve-lu x (mem/read-pointer lower ::mem/pointer) 
                   (mem/read-pointer pivot ::mem/pointer)
                   b 0) ;; AF_MAT_NONE
    )
  ```

  Example (multiple solves with same A):
  ```clojure
  ;; Decompose once, solve many times (efficient!)
  (let [A (af-constant [200 200] matrix-data :f64)
        lower (mem/alloc-instance ::mem/pointer)
        upper (mem/alloc-instance ::mem/pointer)
        pivot (mem/alloc-instance ::mem/pointer)
        x (mem/alloc-instance ::mem/pointer)]
    ;; One-time LU decomposition
    (af-lu lower upper pivot A)
    (let [lu-mat (mem/read-pointer lower ::mem/pointer)
          piv-arr (mem/read-pointer pivot ::mem/pointer)]
      ;; Solve for multiple right-hand sides
      (doseq [b-data rhs-datasets]
        (let [b (af-constant [200 1] b-data :f64)]
          (af-solve-lu x lu-mat piv-arr b 0)
          ;; Process solution x
          ))))
  ```

  Performance Comparison:
  - First solve (with LU): O(n³) + O(n²) = O(n³)
  - Subsequent solves: O(n²) only (no decomposition)
  - Direct af-solve k times: k × O(n³)

  For k > 1 solves with same A:
  - LU approach: O(n³) + k×O(n²) ≈ O(n³) for large n
  - Direct approach: k×O(n³)
  - Speedup factor: approximately k (for large n)

  Type Support:
  - a and b: f32, f64, c32, c64 (floating point types)
  - piv: s32 (32-bit signed integer)

  Constraints:
  - Matrix a must be square (m×m)
  - Batch mode not supported (ndims must be ≤ 2)
  - options must currently be AF_MAT_NONE (0)
  - Pivot array must be 32-bit integers

  Use Cases:
  - Iterative algorithms (multiple solves per iteration)
  - Time-varying right-hand side with fixed coefficient matrix
  - Inverse matrix computation (solve with identity matrix)
  - Determinant calculation (from LU decomposition)

  Notes:
  - More efficient than af-solve for repeated systems
  - Requires pre-computation of LU decomposition via af_lu
  - Pivot array essential for numerical stability
  - Empty arrays (ndims=0) return empty output

  Limitations:
  - No batch operations (use af-solve for batch)
  - options parameter not yet fully utilized (must be AF_MAT_NONE)

  See also:
  - af_solve_lu (ArrayFire C API)
  - af-solve: General linear system solver
  - af-lu: Compute LU decomposition
  - af-inverse: Compute matrix inverse"
  "af_solve_lu" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)
