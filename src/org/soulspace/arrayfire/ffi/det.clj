(ns org.soulspace.arrayfire.ffi.det
  "Bindings for the ArrayFire determinant function.
   
   The determinant is a scalar value that can be computed from a square matrix.
   It provides important information about the matrix and the linear transformation
   it represents.
   
   ## Determinant Properties
   
   - **Non-singularity**: det(A) ≠ 0 ⟺ A is invertible
   - **Orientation**: sign indicates orientation of transformation
   - **Volume scaling**: |det(A)| = volume scaling factor
   - **Product rule**: det(AB) = det(A) × det(B)
   - **Transpose**: det(A^T) = det(A)
   - **Inverse**: det(A^-1) = 1/det(A)
   - **Scalar multiple**: det(cA) = c^n × det(A) for n×n matrix
   
   ## Geometric Interpretation
   
   For a matrix representing a linear transformation:
   - |det| = how much the transformation scales volumes
   - det > 0: preserves orientation
   - det < 0: reverses orientation
   - det = 0: collapses space (loses dimension)
   
   ## Applications
   
   - **Matrix invertibility**: Check if matrix is singular
   - **System solvability**: Determine if linear system has unique solution
   - **Volume calculations**: Compute parallelepiped volumes
   - **Characteristic polynomial**: Find eigenvalues
   - **Jacobian determinant**: Measure local volume change in transformations
   - **Computational geometry**: Area/volume computations
   
   ## Algorithm
   
   ArrayFire computes determinant via LU decomposition:
   1. Perform LU factorization: PA = LU
   2. det(A) = det(P) × det(L) × det(U)
   3. det(L) = 1 (unit diagonal)
   4. det(U) = product of diagonal elements
   5. det(P) = (-1)^(number of row swaps)
   
   Complexity: O(n³) for n×n matrix"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; af_err af_det(double *det_real, double *det_imag, const af_array in)
(defcfn af-det
  "Compute the determinant of a square matrix.
   
   Calculates the determinant using LU decomposition. The determinant is a scalar
   value that provides important properties about the matrix, including whether
   it is invertible and how it scales volumes under linear transformation.
   
   Parameters:
   - det_real: out pointer to double for real part of determinant
   - det_imag: out pointer to double for imaginary part of determinant
   - in: input square matrix (af_array)
   
   Formula:
   For n×n matrix A:
   det(A) = sum over all permutations σ of {1,...,n} of:
            sgn(σ) × ∏(i=1 to n) a[i,σ(i)]
   
   Where sgn(σ) = ±1 depending on permutation parity
   
   Implementation via LU decomposition:
   1. Factor A = PLU where P is permutation, L lower triangular, U upper triangular
   2. det(A) = det(P) × det(L) × det(U)
   3. det(L) = 1 (unit diagonal)
   4. det(U) = u[0,0] × u[1,1] × ... × u[n-1,n-1]
   5. det(P) = (-1)^k where k = number of row swaps
   
   Constraints:
   - Input must be a square matrix (m×m)
   - Input must be 2D (not batched)
   - Input must be floating-point or complex type
   - Matrix dimensions must be equal: dims[0] == dims[1]
   
   Type handling:
   - Input types: f32, f64, c32, c64
   - Real matrices (f32, f64): det_imag = 0
   - Complex matrices (c32, c64): Both det_real and det_imag set
   - Output always in double precision
   - Integer types not supported (must cast to float first)
   
   Special cases:
   - **Empty matrix (0×0)**: Returns 1.0 (mathematical convention)
   - **1×1 matrix**: Returns the single element
   - **2×2 matrix**: ad - bc
   - **Triangular matrix**: Product of diagonal elements
   - **Diagonal matrix**: Product of diagonal elements
   - **Identity matrix**: Returns 1.0
   - **Zero matrix**: Returns 0.0
   - **Singular matrix**: Returns 0.0 (or very close to 0 with rounding errors)
   
   Interpretation:
   
   **Magnitude |det(A)|**:
   - |det| = 0: Matrix is singular (not invertible)
     * System Ax = b has no unique solution
     * Columns/rows are linearly dependent
     * Transformation collapses space
   - |det| < 1: Transformation compresses volumes
   - |det| = 1: Transformation preserves volumes (orthogonal/unitary)
   - |det| > 1: Transformation expands volumes
   
   **Sign of det(A)** (real matrices):
   - det > 0: Transformation preserves orientation (proper rotation/scaling)
   - det < 0: Transformation reverses orientation (reflection involved)
   
   **Complex determinant**:
   - Phase angle: Rotation component of transformation
   - Magnitude: Scaling component
   
   Geometric interpretation:
   
   **2D (2×2 matrix)**:
   det(A) = signed area of parallelogram formed by column vectors
   - Positive: Counterclockwise orientation
   - Negative: Clockwise orientation
   - Zero: Vectors are collinear (degenerate parallelogram)
   
   **3D (3×3 matrix)**:
   det(A) = signed volume of parallelepiped formed by column vectors
   - Positive: Right-hand rule orientation
   - Negative: Left-hand rule orientation
   - Zero: Vectors are coplanar (degenerate parallelepiped)
   
   **nD (n×n matrix)**:
   |det(A)| = n-dimensional hypervolume scaling factor
   
   Use cases:
   
   **Matrix invertibility test**:
   ```clojure
   (let [det-val (compute-det matrix)]
     (if (< (abs det-val) epsilon)
       :singular  ; Not invertible
       :invertible))
   ```
   
   **System solvability (Ax = b)**:
   - det(A) ≠ 0: Unique solution exists
   - det(A) = 0: Either no solution or infinite solutions
   
   **Volume calculations**:
   ```clojure
   ;; Volume of parallelepiped with edges a, b, c
   (abs (det [a b c]))  ; 3D volume
   ```
   
   **Jacobian determinant**:
   Used in change of variables for integrals:
   ∫∫ f(x,y) dxdy = ∫∫ f(u,v) |det(J)| dudv
   Where J is the Jacobian matrix ∂(x,y)/∂(u,v)
   
   **Characteristic polynomial**:
   det(A - λI) = 0 yields eigenvalues λ
   
   **Cramer's rule**:
   For Ax = b, x[i] = det(A_i) / det(A)
   Where A_i is A with column i replaced by b
   
   **Computational geometry**:
   - Triangle area: ½|det([x1 y1 1; x2 y2 1; x3 y3 1])|
   - Point in polygon tests
   - Convex hull algorithms
   
   Performance considerations:
   - Complexity: O(n³) via LU decomposition
   - More efficient than naive expansion for n > 3
   - GPU accelerated for large matrices
   - For many determinants: Consider batching at higher level
   - For small matrices (2×2, 3×3): Direct formulas may be faster
   
   Numerical stability:
   - LU decomposition with partial pivoting is stable
   - For ill-conditioned matrices, result may be inaccurate
   - Very large/small determinants may overflow/underflow
   - Consider using log(|det|) for extreme values
   - Check condition number first if accuracy is critical
   
   Common issues and solutions:
   
   **Near-zero determinant**:
   - Cause: Matrix is nearly singular
   - Solution: Use threshold (e.g., |det| < 1e-10)
   - Alternative: Compute condition number or rank
   
   **Floating-point overflow**:
   - Cause: Very large matrix entries or dimension
   - Solution: Scale matrix first, adjust result
   - Alternative: Use log-determinant if only magnitude matters
   
   **Incorrect singularity detection**:
   - Cause: Rounding errors give det ≈ 1e-15 instead of 0
   - Solution: Use threshold appropriate for problem scale
   - Rule of thumb: threshold ≈ n × machine_epsilon × ||A||
   
   **Batch mode error**:
   - Cause: Trying to compute determinants of 3D+ arrays
   - Solution: Extract 2D slices and compute separately
   
   Efficiency tips:
   - **Already have LU decomposition**: Compute det directly from U diagonal
   - **Only need sign**: Use rank or pivoting information
   - **Only need magnitude**: May use log-determinant to avoid overflow
   - **Multiple related matrices**: Factor once, adjust result
   
   Mathematical properties and identities:
   
   **Basic properties**:
   - det(I) = 1 (identity matrix)
   - det(A^T) = det(A) (transpose)
   - det(A^*) = det(A)* (conjugate transpose gives conjugate determinant)
   - det(A^-1) = 1/det(A) (inverse)
   - det(AB) = det(A) × det(B) (product)
   - det(cA) = c^n × det(A) for n×n matrix and scalar c
   
   **Row/column operations**:
   - Swapping two rows/columns: det changes sign
   - Multiplying row/column by c: det multiplied by c
   - Adding multiple of one row/column to another: det unchanged
   
   **Special matrices**:
   - Diagonal: det = product of diagonal elements
   - Triangular: det = product of diagonal elements
   - Orthogonal (A^T A = I): |det| = 1
   - Unitary (A^* A = I): |det| = 1
   - Hermitian: det is real
   - Skew-symmetric (odd dimension): det = 0
   
   **Matrix transformations**:
   - Similar matrices: det(P^-1 AP) = det(A)
   - Hadamard product: det(A ∘ B) generally ≠ det(A) × det(B)
   - Kronecker product: det(A ⊗ B) = det(A)^m × det(B)^n for A:n×n, B:m×m
   
   Related computations:
   - **Rank**: Use af_rank for numerical rank determination
   - **Condition number**: ||A|| × ||A^-1||, measure of invertibility quality
   - **Eigenvalues**: Product of eigenvalues equals determinant
   - **Trace**: Sum of eigenvalues (not directly related to det)
   
   Example calculations:
   
   **2×2 determinant**:
   ```
   A = [a b]    det(A) = ad - bc
       [c d]
   ```
   
   **3×3 determinant (rule of Sarrus)**:
   ```
   A = [a b c]    det(A) = aei + bfg + cdh - ceg - bdi - afh
       [d e f]
       [g h i]
   ```
   
   **Rotation matrix (2D)**:
   ```
   R = [cos θ  -sin θ]    det(R) = 1
       [sin θ   cos θ]
   ```
   
   **Scaling matrix**:
   ```
   S = [s₁  0  ...  0]    det(S) = s₁ × s₂ × ... × sₙ
       [0  s₂  ...  0]
       [⋮   ⋮   ⋱   ⋮]
       [0   0  ... sₙ]
   ```
   
   See also:
   - af_rank: Determine matrix rank (numerical rank for near-singular matrices)
   - af_inverse: Compute matrix inverse (requires det ≠ 0)
   - af_lu: LU factorization (underlying computation for det)
   - af_solve: Solve linear systems (alternative when only solution needed)
   - af_norm: Matrix norms (condition number estimation)
   
   Returns:
   ArrayFire error code
   
   Example:
   ```clojure
   ;; Compute determinant of real matrix
   (let [det-real (mem/alloc-instance ::mem/double)
         det-imag (mem/alloc-instance ::mem/double)]
     (af-det det-real det-imag matrix-array)
     (let [det-value (mem/deref det-real)]
       (if (< (abs det-value) 1e-10)
         (println \"Matrix is singular\")
         (println \"Matrix is invertible, det =\" det-value))))
   
   ;; Compute determinant of complex matrix
   (let [det-real (mem/alloc-instance ::mem/double)
         det-imag (mem/alloc-instance ::mem/double)]
     (af-det det-real det-imag complex-matrix)
     (let [real-part (mem/deref det-real)
           imag-part (mem/deref det-imag)]
       (println \"det =\" real-part \"+\" imag-part \"i\")))
   ```"
  "af_det" [::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)
