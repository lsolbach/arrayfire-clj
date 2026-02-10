(ns org.soulspace.arrayfire.ffi.c-api.lu
  "Bindings for the ArrayFire LU decomposition functions.
   
   LU decomposition (also called LU factorization) is a fundamental matrix
   factorization algorithm in linear algebra that decomposes a matrix into
   the product of a lower triangular matrix (L), an upper triangular matrix
   (U), and a permutation matrix (P).
   
   Mathematical Foundation:
   
   The LU decomposition computes:
     PA = LU
   
   Where:
   - P is a permutation matrix (encoded as a pivot vector)
   - L is a lower triangular matrix with ones on the diagonal
   - U is an upper triangular matrix
   - A is the input matrix
   
   More formally:
   - L[i,j] = 0 for i < j  (all entries above diagonal are zero)
   - L[i,i] = 1 for all i  (diagonal entries are one)
   - U[i,j] = 0 for i > j  (all entries below diagonal are zero)
   
   Visual Example (3×3 matrix):
   
   ```
   Original matrix A:
   ┌           ┐
   │ 4  3  -1  │
   │ 1  5   2  │
   │ 3  2   4  │
   └           ┘
   
   After LU decomposition: PA = LU
   
   Permutation (pivot): [0, 1, 2] (no swapping)
   
   Lower matrix L:          Upper matrix U:
   ┌           ┐            ┌           ┐
   │ 1  0  0   │            │ 4  3  -1  │
   │ 0.25 1  0 │            │ 0  4.25 2.25 │
   │ 0.75 0.12 1│           │ 0  0   3.47 │
   └           ┘            └           ┘
   
   Verification: PA = LU (multiply P×A, then L×U to verify equality)
   ```
   
   The Pivot Vector:
   
   The pivot vector encodes row permutations performed during decomposition:
   - ArrayFire pivot format: pivot[i] indicates which row to swap with row i
   - LAPACK pivot format: 1-based indices, slightly different encoding
   - The is_lapack_piv parameter controls which format is used
   
   Example pivot interpretation:
   ```
   pivot = [2, 1, 0] means:
   - Row 0 was swapped with row 2
   - Row 1 stayed in place
   - Row 2 was swapped with row 0
   ```
   
   Two Decomposition Modes:
   
   1. **Separate Output (af_lu)**:
      - Returns three separate arrays: L, U, and pivot
      - L and U are separate matrices
      - Requires more memory but easier to use
      - Use when you need explicit L and U matrices
   
   2. **In-Place (af_lu_inplace)**:
      - Overwrites input array with packed LU decomposition
      - L and U are stored in a single matrix
      - Memory efficient (no additional allocation for L and U)
      - Use for large matrices or when memory is constrained
   
   Packed LU Format:
   
   In the in-place version, L and U are stored together:
   ```
   Packed LU matrix:
   ┌           ┐
   │ u₁₁ u₁₂ u₁₃│  ← Upper triangle (including diagonal)
   │ l₂₁ u₂₂ u₂₃│  ← Lower triangle (below diagonal) + Upper
   │ l₃₁ l₃₂ u₃₃│  ← Lower + Upper
   └           ┘
   
   To extract:
   - L: Take lower triangle, set diagonal to 1
   - U: Take upper triangle (including diagonal)
   ```
   
   Algorithm:
   
   LU decomposition uses Gaussian elimination with partial pivoting:
   
   1. **Partial Pivoting**: At each step k, find the row with largest
      absolute value in column k (from row k onwards)
   2. **Row Swap**: Swap current row k with the pivot row (record in pivot)
   3. **Elimination**: Compute multipliers l[i,k] = a[i,k] / a[k,k]
   4. **Update**: Update remaining submatrix: a[i,j] -= l[i,k] * a[k,j]
   5. **Repeat**: Continue for all columns
   
   The partial pivoting ensures numerical stability by avoiding division
   by very small numbers.
   
   Applications:
   
   LU decomposition is used as a building block for many algorithms:
   
   1. **Solving Linear Systems**: Ax = b
      - After PA = LU, solve: Ly = Pb, then Ux = y
      - Forward substitution (L) + back substitution (U)
      - Efficient when solving multiple systems with same A
   
   2. **Matrix Inversion**: A⁻¹
      - Solve AX = I using LU decomposition
      - Each column of X is a solution to Ax = eᵢ
   
   3. **Determinant Calculation**: det(A)
      - det(A) = (-1)ⁿ × det(U)  (where n = number of row swaps)
      - det(U) = product of diagonal elements (triangular matrix)
   
   4. **Matrix Rank**: rank(A)
      - Count non-zero diagonal elements in U
      - Consider numerical tolerance for near-zero values
   
   5. **Condition Number**: cond(A)
      - Estimate using triangular solves with L and U
   
   Performance Characteristics:
   
   **Computational Complexity**:
   - O(n³/3) for n×n matrix (⅔n³ operations for LU, ⅓n³ for solve)
   - More precisely: (2/3)n³ - (1/2)n² + (5/6)n FLOPs
   
   **Memory Usage**:
   - af_lu: O(n²) additional space (separate L and U)
   - af_lu_inplace: O(n) additional space (only pivot vector)
   
   **GPU Acceleration**:
   - Highly parallelizable: 10-100× speedup over CPU
   - Optimal for matrices larger than 128×128
   - Backend-specific optimizations:
     * CUDA: cuSOLVER (cusolverDngetrf)
     * OpenCL: clBLAS/clMAGMA with CPU fallback
     * CPU: LAPACK (sgetrf/dgetrf/cgetrf/zgetrf)
     * OneAPI: oneMKL (oneapi::mkl::lapack::getrf)
   
   **Size Considerations**:
   - Small (< 32×32): CPU may be faster due to overhead
   - Medium (32-256): GPU begins to show advantage
   - Large (> 256): GPU strongly preferred
   - Very large (> 4096): Consider blocked algorithms
   
   **Numerical Stability**:
   - Partial pivoting ensures reasonable stability
   - For ill-conditioned matrices, consider:
     * Complete pivoting (not available in ArrayFire)
     * SVD for more stability (slower but more robust)
     * Iterative refinement for better accuracy
   
   Matrix Requirements:
   
   **Dimensions**:
   - Input can be rectangular (m × n)
   - L will be m × min(m,n) - lower trapezoidal
   - U will be min(m,n) × n - upper trapezoidal
   - For square matrices: L and U are both n × n
   
   **Type Support**:
   - Float32 (f32): Single precision, faster, less accurate
   - Float64 (f64): Double precision, slower, more accurate
   - Complex32 (c32): Single precision complex numbers
   - Complex64 (c64): Double precision complex numbers
   
   **Batch Mode**:
   - NOT supported: LU cannot process batches (ndims > 2)
   - Use af_lu multiple times for batch processing
   - Future versions may support batched operations
   
   **Singularity**:
   - LU may fail for singular matrices (det = 0)
   - Check return status or examine U diagonal
   - Near-singular matrices may have large errors
   
   Comparison with Other Factorizations:
   
   | Factorization | Use Case                  | Complexity | Stability |
   |---------------|---------------------------|------------|-----------|
   | LU            | General linear systems    | O(n³/3)    | Good      |
   | Cholesky      | Symmetric positive def.   | O(n³/6)    | Excellent |
   | QR            | Least squares, orthogonal | O(2n³/3)   | Excellent |
   | SVD           | Rank, pseudoinverse       | O(4n³)     | Excellent |
   
   Choose LU when:
   - Matrix is general (not symmetric or positive definite)
   - Speed is important and stability is acceptable
   - Solving multiple systems with same matrix
   - Need determinant or matrix inversion
   
   Error Handling:
   
   Common error codes:
   - AF_ERR_ARG: Invalid arguments (null pointers)
   - AF_ERR_TYPE: Unsupported data type (not floating point)
   - AF_ERR_BATCH: Attempted batch mode (ndims > 2)
   - AF_ERR_NOT_CONFIGURED: LAPACK not available
   - AF_ERR_MEM: Insufficient device memory
   - AF_ERR_SIZE: Invalid matrix dimensions
   
   Best Practices:
   
   1. **Choose the Right Mode**:
      - Use af_lu for clarity and ease of use
      - Use af_lu_inplace when memory is constrained
   
   2. **Reuse Decompositions**:
      - If solving Ax = b multiple times with same A
      - Compute LU once, solve many times with different b
      - Massive speedup: O(n³) once + O(n²) per solve
   
   3. **Check Availability**:
      - Call af_is_lapack_available before LU operations
      - Handle AF_ERR_NOT_CONFIGURED gracefully
   
   4. **Numerical Considerations**:
      - For ill-conditioned matrices, consider scaling
      - Check condition number before decomposition
      - Consider iterative refinement for high accuracy
   
   5. **Memory Management**:
      - Release L, U, pivot arrays when done
      - For large matrices, use in-place version
      - Consider sparse formats for sparse matrices
   
   Example Workflow:
   
   ```clojure
   ;; Example: Solve Ax = b using LU decomposition
   (let [;; Create coefficient matrix A
         A (create-array (flatten [[4 3 -1]
                                   [1 5  2]
                                   [3 2  4]])
                        [3 3])
         
         ;; Create right-hand side b
         b (create-array [10.0 12.0 14.0] [3 1])
         
         ;; Perform LU decomposition
         lower-ptr (mem/alloc-pointer ::mem/pointer)
         upper-ptr (mem/alloc-pointer ::mem/pointer)
         pivot-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-lu lower-ptr upper-ptr pivot-ptr A)
         
         ;; Get decomposition results
         L (mem/read-pointer lower-ptr ::mem/pointer)
         U (mem/read-pointer upper-ptr ::mem/pointer)
         pivot (mem/read-pointer pivot-ptr ::mem/pointer)
         
         ;; Solve the system using LU
         x-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-solve-lu x-ptr A pivot b AF_MAT_NONE)
         x (mem/read-pointer x-ptr ::mem/pointer)]
     
     ;; x now contains the solution to Ax = b
     ;; Verify: A × x ≈ b
     
     ;; Clean up
     (af-release-array L)
     (af-release-array U)
     (af-release-array pivot)
     (af-release-array x))
   ```
   
   See also:
   - af_solve_lu: Solve linear system using precomputed LU
   - af_inverse: Matrix inversion (uses LU internally)
   - af_det: Determinant (uses LU internally)
   - af_cholesky: For symmetric positive definite matrices
   - af_qr: For least squares and orthogonalization
   - af_svd: For rank, condition number, pseudoinverse"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.loader]))

;; LU decomposition functions

;; af_err af_lu(af_array *lower, af_array *upper, af_array *pivot, const af_array in)
(defcfn af-lu
  "Perform LU decomposition with separate output matrices.
   
   Decomposes input matrix A into lower triangular (L), upper triangular (U),
   and permutation (pivot) such that PA = LU, where P is the permutation
   matrix encoded by the pivot vector.
   
   Parameters:
   - lower: out pointer for lower triangular matrix L
   - upper: out pointer for upper triangular matrix U
   - pivot: out pointer for pivot vector (permutation indices)
   - in: input matrix array (2D only, no batch mode)
   
   Output Dimensions:
   - For m×n input:
     * L: m × min(m,n) - lower trapezoidal with 1's on diagonal
     * U: min(m,n) × n - upper trapezoidal
     * pivot: min(m,n) × 1 - permutation vector
   - For square n×n input:
     * L: n × n - lower triangular
     * U: n × n - upper triangular
     * pivot: n × 1 - permutation vector
   
   Pivot Vector Format:
   - ArrayFire format (default): pivot[i] is the row index that was
     swapped with row i during elimination
   - Values are 0-based indices
   - No swap if pivot[i] == i
   
   Type Support:
   - f32: Single precision (faster, ~7 decimal digits accuracy)
   - f64: Double precision (slower, ~16 decimal digits accuracy)
   - c32: Single precision complex
   - c64: Double precision complex
   
   Constraints:
   - Input must be 2D (ndims ≤ 2)
   - Batch mode NOT supported
   - Must be floating point type
   - Requires LAPACK support
   
   Algorithm:
   Uses Gaussian elimination with partial pivoting:
   1. Find largest element in current column (pivoting)
   2. Swap rows to bring pivot to diagonal
   3. Eliminate elements below pivot
   4. Record multipliers in L, results in U
   
   Performance:
   - Complexity: O(⅔n³) for n×n matrix
   - GPU accelerated via cuSOLVER/clBLAS/LAPACK
   - Memory: Creates new arrays for L, U, pivot
   - Typical timing:
     * 128×128: ~0.5ms (GPU), ~5ms (CPU)
     * 512×512: ~3ms (GPU), ~100ms (CPU)
     * 2048×2048: ~50ms (GPU), ~2000ms (CPU)
   
   Example 1 (Basic square matrix):
   ```clojure
   (let [;; Create 3×3 matrix
         A (create-array [4.0 1.0 3.0
                         3.0 5.0 2.0
                         -1.0 2.0 4.0] [3 3])
         lower-ptr (mem/alloc-pointer ::mem/pointer)
         upper-ptr (mem/alloc-pointer ::mem/pointer)
         pivot-ptr (mem/alloc-pointer ::mem/pointer)
         err (af-lu lower-ptr upper-ptr pivot-ptr A)]
     (when (zero? err)
       (let [L (mem/read-pointer lower-ptr ::mem/pointer)
             U (mem/read-pointer upper-ptr ::mem/pointer)
             pivot (mem/read-pointer pivot-ptr ::mem/pointer)]
         ;; L is 3×3 lower triangular with 1's on diagonal
         ;; U is 3×3 upper triangular
         ;; pivot contains permutation indices
         ;; Verify: P×A = L×U
         [L U pivot])))
   ```
   
   Example 2 (With error checking):
   ```clojure
   (let [A (create-array data [100 100])
         lower-ptr (mem/alloc-pointer ::mem/pointer)
         upper-ptr (mem/alloc-pointer ::mem/pointer)
         pivot-ptr (mem/alloc-pointer ::mem/pointer)
         err (af-lu lower-ptr upper-ptr pivot-ptr A)]
     (if (zero? err)
       (let [L (mem/read-pointer lower-ptr ::mem/pointer)
             U (mem/read-pointer upper-ptr ::mem/pointer)
             pivot (mem/read-pointer pivot-ptr ::mem/pointer)]
         ;; Success: decomposition computed
         {:lower L :upper U :pivot pivot})
       ;; Error: handle failure
       (throw (ex-info \"LU decomposition failed\"
                       {:error-code err}))))
   ```
   
   Example 3 (Rectangular matrix):
   ```clojure
   (let [;; Tall matrix 5×3
         A (create-array (range 15.0) [5 3])
         lower-ptr (mem/alloc-pointer ::mem/pointer)
         upper-ptr (mem/alloc-pointer ::mem/pointer)
         pivot-ptr (mem/alloc-pointer ::mem/pointer)
         err (af-lu lower-ptr upper-ptr pivot-ptr A)]
     (when (zero? err)
       (let [L (mem/read-pointer lower-ptr ::mem/pointer)
             U (mem/read-pointer upper-ptr ::mem/pointer)
             pivot (mem/read-pointer pivot-ptr ::mem/pointer)]
         ;; L is 5×3 (lower trapezoidal)
         ;; U is 3×3 (upper triangular)
         ;; pivot is 3×1
         [L U pivot])))
   ```
   
   Example 4 (Solving linear system):
   ```clojure
   ;; Solve Ax = b using LU decomposition
   (let [A (create-array [[2 1] [1 2]] [2 2])
         b (create-array [3.0 3.0] [2 1])
         
         ;; Decompose A
         lower-ptr (mem/alloc-pointer ::mem/pointer)
         upper-ptr (mem/alloc-pointer ::mem/pointer)
         pivot-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-lu lower-ptr upper-ptr pivot-ptr A)
         
         L (mem/read-pointer lower-ptr ::mem/pointer)
         U (mem/read-pointer upper-ptr ::mem/pointer)
         pivot (mem/read-pointer pivot-ptr ::mem/pointer)
         
         ;; Solve using LU (requires af-solve-lu)
         x-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-solve-lu x-ptr A pivot b AF_MAT_NONE)
         x (mem/read-pointer x-ptr ::mem/pointer)]
     ;; x contains solution to Ax = b
     x)
   ```
   
   Example 5 (Determinant via LU):
   ```clojure
   ;; Compute determinant using det(A) = det(P) × det(L) × det(U)
   (let [A (create-array matrix-data [n n])
         lower-ptr (mem/alloc-pointer ::mem/pointer)
         upper-ptr (mem/alloc-pointer ::mem/pointer)
         pivot-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-lu lower-ptr upper-ptr pivot-ptr A)
         
         U (mem/read-pointer upper-ptr ::mem/pointer)
         pivot (mem/read-pointer pivot-ptr ::mem/pointer)]
     
     ;; det(L) = 1 (unit diagonal)
     ;; det(U) = product of diagonal elements
     ;; det(P) = (-1)^(number of swaps)
     ;; [compute determinant from U diagonal and pivot swaps]
     ))
   ```
   
   Example 6 (Complex matrix):
   ```clojure
   (let [;; Complex matrix (c64)
         real-part (create-array [1.0 2.0 3.0 4.0] [2 2])
         imag-part (create-array [0.5 1.0 1.5 2.0] [2 2])
         A (af-cplx real-part imag-part)
         
         lower-ptr (mem/alloc-pointer ::mem/pointer)
         upper-ptr (mem/alloc-pointer ::mem/pointer)
         pivot-ptr (mem/alloc-pointer ::mem/pointer)
         err (af-lu lower-ptr upper-ptr pivot-ptr A)]
     (when (zero? err)
       (let [L (mem/read-pointer lower-ptr ::mem/pointer)
             U (mem/read-pointer upper-ptr ::mem/pointer)
             pivot (mem/read-pointer pivot-ptr ::mem/pointer)]
         ;; Complex LU decomposition
         [L U pivot])))
   ```
   
   Example 7 (Multiple systems with same matrix):
   ```clojure
   ;; Efficient: decompose once, solve many times
   (let [A (create-array matrix-data [n n])
         
         ;; Decompose once
         lower-ptr (mem/alloc-pointer ::mem/pointer)
         upper-ptr (mem/alloc-pointer ::mem/pointer)
         pivot-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-lu lower-ptr upper-ptr pivot-ptr A)
         
         L (mem/read-pointer lower-ptr ::mem/pointer)
         U (mem/read-pointer upper-ptr ::mem/pointer)
         pivot (mem/read-pointer pivot-ptr ::mem/pointer)]
     
     ;; Solve multiple systems Ax = b₁, Ax = b₂, ...
     (for [b [b1 b2 b3 b4 b5]]
       (let [x-ptr (mem/alloc-pointer ::mem/pointer)
             _ (af-solve-lu x-ptr A pivot b AF_MAT_NONE)]
         (mem/read-pointer x-ptr ::mem/pointer)))
     
     ;; Much faster than solving each system from scratch
     ;; O(n³) decomposition + 5×O(n²) solves vs 5×O(n³) full solves
     ))
   ```
   
   Common Errors:
   - AF_ERR_ARG: Null pointer for output parameters
   - AF_ERR_TYPE: Input not floating point type
   - AF_ERR_BATCH: Input has ndims > 2
   - AF_ERR_NOT_CONFIGURED: LAPACK not available
   
   Notes:
   - Allocates new memory for L, U, and pivot
   - For memory efficiency, use af-lu-inplace instead
   - Partial pivoting ensures numerical stability
   - For ill-conditioned matrices, check condition number first
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-lu-inplace: In-place LU decomposition (memory efficient)
   - af-solve-lu: Solve linear system using precomputed LU
   - af-is-lapack-available: Check LAPACK availability"
  "af_lu" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_lu_inplace(af_array *pivot, af_array in, const bool is_lapack_piv)
(defcfn af-lu-inplace
  "Perform in-place LU decomposition with packed storage.
   
   Computes LU decomposition and stores the result in the input array
   (overwrites input). L and U are stored in a packed format within a
   single matrix, saving memory compared to af-lu.
   
   Parameters:
   - pivot: out pointer for pivot vector (permutation indices)
   - in: input/output array (2D only)
        * On entry: matrix to decompose
        * On exit: packed LU decomposition
   - is-lapack-piv: boolean (int) controlling pivot format
        * true (1): LAPACK format (1-based, different encoding)
        * false (0): ArrayFire format (0-based, standard encoding)
   
   Packed LU Format:
   The input array is overwritten with a packed representation:
   ```
   Original matrix A:     Packed LU result:
   ┌         ┐            ┌              ┐
   │ a₁₁ a₁₂ │            │ u₁₁ u₁₂ u₁₃  │ ← Upper triangle
   │ a₂₁ a₂₂ │    →       │ l₂₁ u₂₂ u₂₃  │ ← Lower + Upper
   │ a₃₁ a₃₂ │            │ l₃₁ l₃₂ u₃₃  │ ← Lower + Upper
   └         ┘            └              ┘
   
   To extract:
   - Upper (U): Take upper triangle (including diagonal)
   - Lower (L): Take lower triangle (excluding diagonal), set diagonal = 1
   ```
   
   Pivot Format Comparison:
   
   **ArrayFire format** (is_lapack_piv = false):
   - 0-based indices
   - pivot[i] is the row index swapped with row i
   - Direct interpretation: pivot[i] = j means \"swap row i with row j\"
   - Example: [2, 1, 0] = row 0↔2, row 1 stays, row 2↔0
   
   **LAPACK format** (is_lapack_piv = true):
   - 1-based indices (historical LAPACK convention)
   - pivot[i] is the row that was swapped at step i
   - Encoded differently, requires conversion for direct use
   - Example: [3, 2, 1] (1-based) equivalent to [2, 1, 0] (0-based)
   
   Recommendation: Use ArrayFire format (is_lapack_piv = false) unless
   interfacing with external LAPACK code.
   
   Memory Efficiency:
   - No additional memory for L and U (stored in input array)
   - Only allocates pivot vector: O(n) space vs O(n²) for separate L, U
   - Ideal for large matrices where memory is constrained
   - Destructive operation: input array is overwritten
   
   Performance:
   - Identical computational complexity to af-lu: O(⅔n³)
   - Slightly faster due to reduced memory allocation
   - Same GPU acceleration benefits
   - Typical speedup: 5-10% faster than af-lu
   
   Type Support:
   - f32: Single precision
   - f64: Double precision
   - c32: Single precision complex
   - c64: Double precision complex
   
   Constraints:
   - Input must be 2D (ndims ≤ 2)
   - Batch mode NOT supported
   - Must be floating point type
   - Requires LAPACK support
   - Input array is destroyed (overwritten)
   
   Example 1 (Basic usage with ArrayFire pivots):
   ```clojure
   (let [;; Create matrix (will be overwritten!)
         A (create-array [2.0 1.0 3.0
                         1.0 3.0 2.0
                         3.0 2.0 1.0] [3 3])
         
         ;; Perform in-place LU
         pivot-ptr (mem/alloc-pointer ::mem/pointer)
         is-lapack-piv 0  ; ArrayFire format (0-based)
         err (af-lu-inplace pivot-ptr A is-lapack-piv)]
     
     (when (zero? err)
       (let [pivot (mem/read-pointer pivot-ptr ::mem/pointer)]
         ;; A now contains packed LU
         ;; Upper triangle = U
         ;; Lower triangle (with diagonal=1) = L
         ;; pivot contains 0-based permutation
         {:packed-lu A :pivot pivot})))
   ```
   
   Example 2 (Extract L and U from packed format):
   ```clojure
   (let [A (create-array matrix-data [n n])
         pivot-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-lu-inplace pivot-ptr A 0)
         pivot (mem/read-pointer pivot-ptr ::mem/pointer)
         
         ;; Extract upper triangle (including diagonal)
         U-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-upper U-ptr A false)  ; false = keep diagonal
         U (mem/read-pointer U-ptr ::mem/pointer)
         
         ;; Extract lower triangle (excluding diagonal)
         L-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-lower L-ptr A true)   ; true = set diagonal to 1
         L (mem/read-pointer L-ptr ::mem/pointer)]
     
     ;; Now have separate L and U matrices
     {:lower L :upper U :pivot pivot})
   ```
   
   Example 3 (LAPACK pivot format):
   ```clojure
   ;; Use LAPACK format (for interfacing with LAPACK code)
   (let [A (create-array matrix-data [n n])
         pivot-ptr (mem/alloc-pointer ::mem/pointer)
         is-lapack-piv 1  ; LAPACK format (1-based)
         err (af-lu-inplace pivot-ptr A is-lapack-piv)]
     (when (zero? err)
       (let [pivot (mem/read-pointer pivot-ptr ::mem/pointer)]
         ;; pivot now in LAPACK format (1-based indices)
         ;; Convert to 0-based if needed for ArrayFire operations
         {:packed-lu A :lapack-pivot pivot})))
   ```
   
   Example 4 (Solving with in-place LU):
   ```clojure
   ;; Solve Ax = b using in-place LU (memory efficient)
   (let [A (create-array matrix-data [n n])
         b (create-array rhs-data [n 1])
         
         ;; Compute in-place LU
         pivot-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-lu-inplace pivot-ptr A 0)
         pivot (mem/read-pointer pivot-ptr ::mem/pointer)
         
         ;; Solve using packed LU
         ;; Note: A is now the packed LU, not original matrix
         x-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-solve-lu x-ptr A pivot b AF_MAT_NONE)
         x (mem/read-pointer x-ptr ::mem/pointer)]
     
     ;; x contains solution
     ;; Original A is destroyed, but memory usage minimized
     x)
   ```
   
   Example 5 (Large matrix memory optimization):
   ```clojure
   ;; For very large matrices, in-place is crucial
   (let [;; Large 4096×4096 matrix
         A (create-array large-data [4096 4096])
         ;; Using af-lu would require ~400MB additional memory
         ;; Using af-lu-inplace requires only ~32KB for pivot
         
         pivot-ptr (mem/alloc-pointer ::mem/pointer)
         err (af-lu-inplace pivot-ptr A 0)]
     
     (when (zero? err)
       (let [pivot (mem/read-pointer pivot-ptr ::mem/pointer)]
         ;; Packed LU ready for solving multiple systems
         ;; with minimal memory footprint
         {:lu A :pivot pivot})))
   ```
   
   Example 6 (Copy input if needed later):
   ```clojure
   ;; If you need to preserve the original matrix
   (let [A-original (create-array matrix-data [n n])
         
         ;; Make a copy for in-place decomposition
         A-copy-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-copy-array A-copy-ptr A-original)
         A-copy (mem/read-pointer A-copy-ptr ::mem/pointer)
         
         ;; Decompose the copy
         pivot-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-lu-inplace pivot-ptr A-copy 0)
         pivot (mem/read-pointer pivot-ptr ::mem/pointer)]
     
     ;; A-original preserved, A-copy contains LU
     {:original A-original
      :lu A-copy
      :pivot pivot})
   ```
   
   Example 7 (Error handling):
   ```clojure
   (let [A (create-array data [n n])
         pivot-ptr (mem/alloc-pointer ::mem/pointer)
         err (af-lu-inplace pivot-ptr A 0)]
     
     (case err
       0 (let [pivot (mem/read-pointer pivot-ptr ::mem/pointer)]
           {:success true :lu A :pivot pivot})
       
       ;; AF_ERR_NOT_CONFIGURED
       2 {:error \"LAPACK not available\"}
       
       ;; AF_ERR_TYPE
       4 {:error \"Invalid type (must be floating point)\"}
       
       ;; AF_ERR_BATCH
       7 {:error \"Batch mode not supported (matrix must be 2D)\"}
       
       ;; Other errors
       {:error \"LU decomposition failed\" :code err}))
   ```
   
   Performance Comparison:
   ```clojure
   ;; Memory usage for 2048×2048 f64 matrix:
   
   ;; af-lu (separate L, U):
   ;; - Input: 32MB
   ;; - L: 32MB
   ;; - U: 32MB
   ;; - Pivot: 16KB
   ;; - Total: ~96MB
   
   ;; af-lu-inplace:
   ;; - Input/LU: 32MB (reused)
   ;; - Pivot: 16KB
   ;; - Total: ~32MB
   
   ;; Memory savings: ~64MB (67% reduction)
   ```
   
   Common Errors:
   - AF_ERR_ARG: Null pivot pointer
   - AF_ERR_TYPE: Input not floating point type
   - AF_ERR_BATCH: Input has ndims > 2
   - AF_ERR_NOT_CONFIGURED: LAPACK not available
   
   Notes:
   - Input array is destroyed (overwritten with packed LU)
   - Make a copy if original matrix is needed later
   - ArrayFire pivot format (is_lapack_piv=false) recommended
   - Use af-upper and af-lower to extract L and U if needed
   - More memory efficient than af-lu (no separate L, U allocation)
   - Ideal for large matrices or memory-constrained systems
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-lu: LU decomposition with separate L and U output
   - af-upper: Extract upper triangle from packed LU
   - af-lower: Extract lower triangle from packed LU
   - af-solve-lu: Solve linear system using LU decomposition"
  "af_lu_inplace" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_is_lapack_available(bool *out)
(defcfn af-is-lapack-available
  "Check if LAPACK support is available.
   
   Determines whether the current ArrayFire backend has been compiled
   with LAPACK support, which is required for LU decomposition and
   other linear algebra operations.
   
   Parameters:
   - out: out pointer for boolean result (int: 0=false, non-zero=true)
   
   LAPACK Availability by Backend:
   
   **CUDA Backend**:
   - Uses cuSOLVER (NVIDIA's CUDA LAPACK implementation)
   - Always available on CUDA-capable GPUs
   - High performance, optimized for NVIDIA GPUs
   
   **OpenCL Backend**:
   - Uses clBLAS/clMAGMA if available
   - Falls back to CPU LAPACK if GPU not available
   - Availability depends on build configuration
   - May be disabled if libraries not found at compile time
   
   **CPU Backend**:
   - Uses standard LAPACK (from MKL, OpenBLAS, or system LAPACK)
   - Availability depends on build configuration
   - Check this before attempting LU on CPU backend
   
   **OneAPI Backend**:
   - Uses oneMKL (Intel's Math Kernel Library for oneAPI)
   - Generally available on Intel GPUs
   - Check availability before operations
   
   Usage Pattern:
   Always check LAPACK availability before attempting LU decomposition
   to provide meaningful error messages to users.
   
   Example 1 (Basic check):
   ```clojure
   (let [available-ptr (mem/alloc-pointer ::mem/int)
         err (af-is-lapack-available available-ptr)]
     (when (zero? err)
       (let [available (mem/read-int available-ptr)]
         (if (pos? available)
           (println \"LAPACK available: can use LU, QR, SVD, etc.\")
           (println \"LAPACK not available: linear algebra disabled\")))))
   ```
   
   Example 2 (Safe LU wrapper):
   ```clojure
   (defn safe-lu [matrix]
     (let [available-ptr (mem/alloc-pointer ::mem/int)
           _ (af-is-lapack-available available-ptr)
           available (mem/read-int available-ptr)]
       
       (if (pos? available)
         ;; LAPACK available: proceed with LU
         (let [lower-ptr (mem/alloc-pointer ::mem/pointer)
               upper-ptr (mem/alloc-pointer ::mem/pointer)
               pivot-ptr (mem/alloc-pointer ::mem/pointer)
               err (af-lu lower-ptr upper-ptr pivot-ptr matrix)]
           (if (zero? err)
             {:lower (mem/read-pointer lower-ptr ::mem/pointer)
              :upper (mem/read-pointer upper-ptr ::mem/pointer)
              :pivot (mem/read-pointer pivot-ptr ::mem/pointer)}
             (throw (ex-info \"LU decomposition failed\"
                            {:error-code err}))))
         
         ;; LAPACK not available: return error
         (throw (ex-info \"LAPACK support not available\"
                        {:backend (get-active-backend)
                         :message \"Recompile ArrayFire with LAPACK\"})))))
   ```
   
   Example 3 (Startup check):
   ```clojure
   ;; Check capabilities at application startup
   (defn check-arrayfire-capabilities []
     (let [backend (get-active-backend)
           lapack-ptr (mem/alloc-pointer ::mem/int)
           _ (af-is-lapack-available lapack-ptr)
           has-lapack (pos? (mem/read-int lapack-ptr))]
       
       {:backend backend
        :lapack has-lapack
        :lu-available has-lapack
        :qr-available has-lapack
        :svd-available has-lapack
        :cholesky-available has-lapack}))
   
   ;; At startup:
   (def capabilities (check-arrayfire-capabilities))
   
   (when-not (:lapack capabilities)
     (println \"WARNING: Linear algebra operations unavailable\")
     (println \"Backend:\" (:backend capabilities)))
   ```
   
   Example 4 (Conditional algorithm selection):
   ```clojure
   (defn solve-linear-system [A b]
     (let [available-ptr (mem/alloc-pointer ::mem/int)
           _ (af-is-lapack-available available-ptr)
           has-lapack (pos? (mem/read-int available-ptr))]
       
       (if has-lapack
         ;; Use fast LU-based solver
         (let [pivot-ptr (mem/alloc-pointer ::mem/pointer)
               _ (af-lu-inplace pivot-ptr A 0)
               pivot (mem/read-pointer pivot-ptr ::mem/pointer)
               x-ptr (mem/alloc-pointer ::mem/pointer)
               _ (af-solve-lu x-ptr A pivot b AF_MAT_NONE)]
           (mem/read-pointer x-ptr ::mem/pointer))
         
         ;; Fallback to iterative solver (slower but no LAPACK needed)
         (iterative-solve A b))))
   ```
   
   Example 5 (Build verification):
   ```clojure
   ;; Verify build configuration
   (defn verify-build []
     (let [available-ptr (mem/alloc-pointer ::mem/int)
           err (af-is-lapack-available available-ptr)]
       
       (if (zero? err)
         (let [available (mem/read-int available-ptr)]
           (println \"LAPACK check successful:\")
           (println \"  Status:\" (if (pos? available) \"Available\" \"Not available\"))
           (println \"  Backend:\" (get-active-backend-name))
           (println \"  Device:\" (get-device-info))
           
           (when-not (pos? available)
             (println \"\\nTo enable LAPACK support:\")
             (println \"  1. Install LAPACK library (MKL, OpenBLAS, etc.)\")
             (println \"  2. Recompile ArrayFire with -DUSE_LINEAR_ALGEBRA=ON\")
             (println \"  3. Ensure linker can find LAPACK libraries\")))
         
         (println \"Error checking LAPACK availability:\" err))))
   ```
   
   Common Scenarios:
   
   **LAPACK Available**:
   - Can use: af-lu, af-lu-inplace, af-qr, af-svd, af-cholesky
   - Can use: af-solve, af-solve-lu, af-inverse
   - Can use: af-det, af-rank, af-norm (some modes)
   - Full linear algebra functionality
   
   **LAPACK Not Available**:
   - Cannot use factorization-based operations
   - Limited to basic matrix operations
   - Consider using iterative methods
   - Or recompile ArrayFire with LAPACK support
   
   Build-Time Considerations:
   - CMake option: -DUSE_LINEAR_ALGEBRA=ON
   - Requires LAPACK library during compilation
   - Common libraries: MKL, OpenBLAS, ATLAS, LAPACK
   - Check with: ldd libaf.so | grep lapack (Linux)
   
   Performance Impact:
   - No performance overhead (simple flag check)
   - O(1) operation
   - Can be called frequently without concern
   
   Error Codes:
   - Should always return AF_SUCCESS (0)
   - If error occurs, indicates internal issue
   
   Notes:
   - Result is determined at compile time, not runtime
   - Cannot be changed without recompiling ArrayFire
   - Check once at startup and cache result if desired
   - Backend-specific: may differ between CUDA, OpenCL, CPU
   
   Returns:
   ArrayFire error code (af_err enum), typically AF_SUCCESS
   
   See also:
   - af-lu: Requires LAPACK support
   - af-lu-inplace: Requires LAPACK support
   - af-get-active-backend: Check which backend is active
   - af-get-device-info: Get device capabilities"
  "af_is_lapack_available" [::mem/pointer] ::mem/int)
