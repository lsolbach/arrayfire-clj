(ns org.soulspace.arrayfire.ffi.c-api.qr
  "Bindings for the ArrayFire QR decomposition functions.
   
   QR decomposition (also called QR factorization) is a fundamental matrix
   decomposition that factors a matrix A into the product of an orthogonal
   matrix Q and an upper triangular matrix R:
   
   A = Q × R
   
   where:
   - Q is an orthogonal matrix (Q^T × Q = I, columns are orthonormal)
   - R is an upper triangular matrix (elements below diagonal are zero)
   - For complex matrices, Q is unitary (Q^H × Q = I, where ^H is conjugate transpose)
   
   Mathematical Foundations:
   
   **Orthogonality**:
   The columns of Q form an orthonormal basis:
   - q_i^T × q_j = δ_ij (Kronecker delta)
   - ||q_i|| = 1 for all columns
   - Preserves vector norms: ||Q×x|| = ||x||
   - Numerically stable for solving linear systems
   
   **Upper Triangular Form**:
   R has the structure:
   [r11 r12 r13 ... r1n]
   [ 0  r22 r23 ... r2n]
   [ 0   0  r33 ... r3n]
   [ 0   0   0  ... rmn]
   
   For m×n matrix A:
   - If m ≥ n: R is n×n (full column rank case)
   - If m < n: R is m×n (underdetermined case)
   - Diagonal elements r_ii indicate column independence
   
   **Gram-Schmidt Process** (conceptual algorithm):
   Classical QR is equivalent to Gram-Schmidt orthogonalization:
   1. For each column a_i of A:
      - Project onto existing orthonormal basis: v_i = a_i - Σ(q_j^T × a_i) × q_j
      - Normalize: q_i = v_i / ||v_i||
      - Store normalization factor: r_ii = ||v_i||
   2. Projections become off-diagonal R elements
   
   **Actual Implementation** (Householder reflections):
   ArrayFire uses LAPACK's GEQRF (GEneral QR Factorization):
   1. Construct Householder matrix H_i = I - τ_i × v_i × v_i^T
   2. Each H_i zeros column elements below diagonal
   3. Accumulated product: Q = H_1 × H_2 × ... × H_k
   4. Packed storage: R stored in upper triangle, v_i in lower triangle
   5. τ (tau) coefficients needed to reconstruct Q
   
   **Tau Parameter**:
   The tau array contains scaling factors for Householder reflections:
   - Length: min(m, n) elements
   - Type: Same as input matrix (f32, f64, c32, c64)
   - Purpose: Reconstructing Q from packed representation
   - Used by subsequent LAPACK routines (e.g., least squares solvers)
   
   Numerical Properties:
   
   **Stability**:
   - Backward stable: (Q̂ × R̂) = A + E, where ||E|| ≈ ε × ||A||
   - ε = machine epsilon (≈10^-7 for f32, ≈10^-16 for f64)
   - More stable than Gram-Schmidt for ill-conditioned matrices
   - Householder method preferred over Gram-Schmidt
   
   **Condition Number**:
   - κ(Q) = 1 (orthogonal matrices are perfectly conditioned)
   - κ(R) = κ(A) (R inherits condition number of A)
   - QR is stable even for κ(A) ≈ 1/ε
   
   **Rank Detection**:
   Diagonal elements of R reveal matrix rank:
   - Large |r_ii|: Column i is independent
   - Small |r_ii| ≈ ε×||A||: Column i nearly dependent
   - Zero r_ii: Rank deficiency (numerically rank-deficient if < ε×||A||)
   
   Performance Characteristics:
   
   **Complexity**:
   For m×n matrix (m ≥ n):
   - Computation: O(2mn² - (2/3)n³) flops
   - For square matrix (m=n): O((4/3)n³) flops
   - Compare: LU is O((2/3)n³), but less stable
   - Memory: O(mn) for storage
   
   **GPU Acceleration**:
   ArrayFire leverages GPU-optimized LAPACK implementations:
   - CUDA backend: cuSolverDN geqrf
   - OpenCL backend: MagmaQR (if available), fallback to clBLAS
   - oneAPI backend: oneMKL geqrf
   - CPU backend: Standard LAPACK (OpenBLAS, MKL, ATLAS)
   
   Typical GPU speedup over CPU:
   - Small matrices (< 256×256): 2-5× (overhead dominant)
   - Medium (1024×1024): 10-30×
   - Large (4096×4096): 30-100× (memory bandwidth exploited)
   
   **Two Variants**:
   
   1. **Full QR** (af-qr):
      - Returns separate Q, R, tau matrices
      - Q is m×m orthogonal (full rank)
      - R is m×n upper triangular
      - More memory, full reconstruction
      - Use when Q is needed explicitly
   
   2. **In-place QR** (af-qr-inplace):
      - Overwrites input with packed QR
      - Returns only tau coefficients
      - Saves memory (no Q allocation)
      - Faster for large matrices
      - Use for solving systems without explicit Q
   
   Applications:
   
   **1. Solving Linear Systems**:
   For Ax = b where A is m×n (m ≥ n):
   - Compute QR: A = Q×R
   - Transform: Q^T×b = R×x (orthogonal transformation)
   - Solve triangular: R×x = Q^T×b (back substitution)
   - Advantages: More stable than LU for ill-conditioned A
   
   **2. Least Squares**:
   Minimize ||A×x - b||² for overdetermined systems (m > n):
   - QR gives optimal solution: x = R^(-1) × Q^T × b
   - Equivalent to normal equations but numerically superior
   - Used in regression, curve fitting, data smoothing
   
   **3. Eigenvalue Algorithms**:
   - QR iteration: A_k+1 = R_k × Q_k
   - Converges to Schur form (eigenvalues on diagonal)
   - Foundation for QR algorithm (not exposed directly)
   
   **4. Orthogonalization**:
   - Extract orthonormal basis from column space of A
   - Gram-Schmidt alternative with better stability
   - Used in signal processing, statistics
   
   **5. Rank Determination**:
   - Examine diagonal of R for small elements
   - Threshold: |r_ii| < ε × ||A|| indicates rank deficiency
   - More reliable than determinant for numerical rank
   
   **6. Matrix Conditioning**:
   - Estimate condition number: κ(A) ≈ |r_11| / |r_nn|
   - Identify ill-conditioning before solving
   - Preconditioning strategy
   
   **7. Computer Vision**:
   - Camera calibration (perspective-n-point)
   - Structure from motion
   - Bundle adjustment (Gauss-Newton with QR)
   
   **8. Machine Learning**:
   - Ridge regression
   - Regularized least squares
   - Feature extraction (PCA alternative)
   
   Type Support:
   - f32 (float): Single precision real
   - f64 (double): Double precision real
   - c32 (complex float): Single precision complex
   - c64 (complex double): Double precision complex
   - Integer types: Not supported (convert to floating point)
   
   Constraints and Limitations:
   
   **Batch Processing**:
   - NOT supported: Cannot process multiple matrices at once
   - Error: AF_ERR_BATCH if input has more than 2 dimensions
   - Workaround: Loop over batch dimension in user code
   
   **Matrix Dimensions**:
   - Input: m×n matrix (any m, n > 0)
   - m ≥ n recommended (overdetermined/square)
   - m < n: Underdetermined, R has extra columns, still works
   - Empty matrices (0 elements): Returns empty Q, R, tau
   
   **Memory Requirements**:
   - Full QR: ~3×m×n elements (Q: m×m, R: m×n, tau: min(m,n))
   - In-place: ~m×n + min(m,n) (input + tau)
   - Large matrices may exceed GPU memory
   
   **Numerical Limitations**:
   - Rank-deficient matrices: QR succeeds but R has small/zero diagonal
   - Severe ill-conditioning: Results may have large relative error
   - Underflow/overflow: Very large/small matrix norms may cause issues
   
   LAPACK Backend:
   
   ArrayFire requires LAPACK for QR decomposition:
   - Check availability: Use af-is-lapack-available
   - If unavailable: Operations fail with AF_ERR_NOT_CONFIGURED
   - CPU: Requires LAPACK library (OpenBLAS, Intel MKL, ATLAS, etc.)
   - GPU: Built-in solver libraries (cuSolver, MagmaQR, oneMKL)
   
   See also:
   - LU decomposition (af-lu): Faster but less stable
   - Cholesky decomposition (af-cholesky): For positive-definite matrices
   - SVD (af-svd): Most general, handles rank deficiency better
   - Solve functions: Use decompositions to solve linear systems"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; QR decomposition functions

;; af_err af_qr(af_array *q, af_array *r, af_array *tau, const af_array in)
(defcfn af-qr
  "Perform QR decomposition, returning separate Q, R, and tau arrays.
   
   Decomposes matrix A into A = Q × R where Q is orthogonal (unitary for
   complex) and R is upper triangular. Returns full matrices suitable for
   explicit reconstruction and analysis.
   
   Parameters:
   - q: out pointer for orthogonal/unitary matrix Q (m×m)
   - r: out pointer for upper triangular matrix R (m×n)
   - tau: out pointer for Householder reflection coefficients (min(m,n))
   - in: input matrix A to decompose (m×n, floating/complex type)
   
   Matrix Dimensions:
   For input A with dimensions m×n:
   - Q: m×m (full orthogonal matrix, square)
   - R: m×n (upper triangular, same shape as A)
   - tau: min(m,n)×1 (one coefficient per Householder reflection)
   
   Output Properties:
   
   **Q (Orthogonal/Unitary Matrix)**:
   - For real types (f32, f64): Q^T × Q = I (orthogonal)
   - For complex (c32, c64): Q^H × Q = I (unitary, ^H = conjugate transpose)
   - Dimensions: m×m (square)
   - All columns are orthonormal: q_i^T × q_j = δ_ij
   - Preserves vector norms: ||Q×x|| = ||x||
   - Determinant: |det(Q)| = 1
   
   **R (Upper Triangular Matrix)**:
   - Structure: All elements below diagonal are zero
   - Dimensions: m×n (same as input A)
   - Diagonal elements r_ii indicate column independence:
     * Large |r_ii|: Column i is independent
     * Small |r_ii| ≈ ε×||A||: Nearly rank-deficient
     * Zero r_ii: Exact rank deficiency (rare in floating point)
   - For m > n: Bottom (m-n) rows are zero
   - For m = n: Square upper triangular
   - For m < n: Trapezoidal (upper triangular with extra columns)
   
   **tau (Householder Coefficients)**:
   - Type: Same as input (f32, f64, c32, c64)
   - Length: min(m, n) elements
   - Purpose: Reconstructing Q from packed representation
   - Needed for LAPACK routines (ormqr/unmqr, solving least squares)
   - Values: τ_i ∈ [0, 2] for real, [0, 2] for complex norm
   
   Verification:
   You can verify correctness:
   ```clojure
   ;; Check: Q×R ≈ A (within numerical precision)
   (let [QR (matrix-multiply Q R)
         diff (subtract QR A)
         error (norm diff)]
     (< error (* 1e-6 (norm A)))) ;; f32 precision
   
   ;; Check: Q^T×Q ≈ I (orthogonality)
   (let [QT (transpose Q)
         QtQ (matrix-multiply QT Q)
         I (identity-matrix m)
         diff (subtract QtQ I)
         error (norm diff)]
     (< error 1e-6)) ;; Should be nearly zero
   ```
   
   Algorithm (Householder QR via LAPACK GEQRF):
   
   1. **Householder Reflection**:
      For each column k = 1 to min(m,n):
      - Construct vector v_k from column k elements below diagonal
      - Compute Householder matrix: H_k = I - τ_k × v_k × v_k^T
      - Apply: A := H_k × A (zeros elements below diagonal in column k)
   
   2. **Packed Storage**:
      After k iterations:
      - Upper triangle and diagonal: R matrix elements
      - Lower triangle: Householder vectors v_k (unit diagonal assumed)
      - Tau array: Scaling factors τ_k
   
   3. **Q Reconstruction**:
      - Q = H_1 × H_2 × ... × H_min(m,n)
      - Start with identity matrix
      - Apply Householder reflections in sequence
      - Uses LAPACK routines (orgqr for real, ungqr for complex)
   
   4. **Separation**:
      - Extract R from upper triangle
      - Reconstruct full Q matrix
      - Return separate arrays
   
   Performance:
   - Complexity: O(2mn² - (2/3)n³) flops for m×n matrix
   - For square (m=n): O((4/3)n³) flops
   - Memory: O(m² + mn) for Q and R storage
   - GPU speedup: 10-100× over CPU for large matrices
   
   Compared to af-qr-inplace:
   - More memory: Allocates full Q (m×m)
   - Slower: Additional reconstruction step for Q
   - More convenient: Separate Q and R ready to use
   - Use when: You need explicit Q matrix
   
   Type Support:
   - f32: Single precision real (6-7 significant digits)
   - f64: Double precision real (15-16 significant digits)
   - c32: Complex single precision
   - c64: Complex double precision
   - Integers: NOT supported (convert to floating point first)
   
   Example 1 - Basic QR decomposition:
   ```clojure
   (require '[coffi.mem :as mem])
   (require '[org.soulspace.arrayfire.ffi.array :as af-array])
   (require '[org.soulspace.arrayfire.ffi.qr :as af-qr])
   
   ;; Create 4×3 matrix
   (let [data [1.0 2.0 3.0
               4.0 5.0 6.0
               7.0 8.0 9.0
               10.0 11.0 12.0]
         dims (mem/alloc-long [4 3])
         in-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-array/af-create-array in-ptr
                                     (mem/serialize data ::mem/float)
                                     4 dims 0) ;; 0 = f32
         in (mem/read-pointer in-ptr ::mem/pointer)
         
         ;; Perform QR
         q-ptr (mem/alloc-pointer ::mem/pointer)
         r-ptr (mem/alloc-pointer ::mem/pointer)
         tau-ptr (mem/alloc-pointer ::mem/pointer)
         err (af-qr/af-qr q-ptr r-ptr tau-ptr in)
         
         Q (mem/read-pointer q-ptr ::mem/pointer)
         R (mem/read-pointer r-ptr ::mem/pointer)
         tau (mem/read-pointer tau-ptr ::mem/pointer)]
     
     ;; Q is 4×4 orthogonal
     ;; R is 4×3 upper triangular
     ;; tau is length 3
     
     ;; Clean up
     (af-array/af-release-array in)
     (af-array/af-release-array Q)
     (af-array/af-release-array R)
     (af-array/af-release-array tau))
   ```
   
   Example 2 - Solving least squares (overdetermined system):
   ```clojure
   ;; Minimize ||A×x - b||² for m×n system (m > n)
   ;; Solution: x = R^(-1) × Q^T × b
   
   (let [;; A is m×n, b is m×1
         m 100, n 50
         A (create-random-matrix m n)
         b (create-random-vector m)
         
         ;; QR decomposition
         q-ptr (mem/alloc-pointer ::mem/pointer)
         r-ptr (mem/alloc-pointer ::mem/pointer)
         tau-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-qr/af-qr q-ptr r-ptr tau-ptr A)
         Q (mem/read-pointer q-ptr ::mem/pointer)
         R (mem/read-pointer r-ptr ::mem/pointer)
         
         ;; Compute Q^T × b
         QT (transpose Q)
         QTb (matrix-multiply QT b)
         
         ;; Extract top n×n block of R (ignore zero rows)
         R-square (get-submatrix R 0 0 n n)
         QTb-top (get-submatrix QTb 0 0 n 1)
         
         ;; Solve upper triangular: R×x = Q^T×b
         x (solve-upper-triangular R-square QTb-top)]
     
     ;; x is the least-squares solution
     ;; Verify: residual = A×x - b should be minimized
     (let [Ax (matrix-multiply A x)
           residual (subtract Ax b)
           error-norm (norm residual)]
       (println \"Least squares residual:\" error-norm))
     
     ;; Clean up...
     )
   ```
   
   Example 3 - Checking matrix rank:
   ```clojure
   ;; Use diagonal elements of R to determine numerical rank
   
   (let [;; Potentially rank-deficient matrix
         A (create-nearly-singular-matrix 5 5)
         
         ;; Perform QR
         q-ptr (mem/alloc-pointer ::mem/pointer)
         r-ptr (mem/alloc-pointer ::mem/pointer)
         tau-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-qr/af-qr q-ptr r-ptr tau-ptr A)
         R (mem/read-pointer r-ptr ::mem/pointer)
         
         ;; Extract diagonal elements
         diag-R (get-diagonal R)
         
         ;; Compute threshold: ε × ||A||
         A-norm (norm A)
         epsilon 1e-6 ;; Machine precision dependent
         threshold (* epsilon A-norm)
         
         ;; Count significant diagonal elements
         rank (count (filter #(> (abs %) threshold) diag-R))]
     
     (println \"Matrix dimensions:\" 5 \"×\" 5)
     (println \"Numerical rank:\" rank)
     (println \"Diagonal of R:\" diag-R)
     
     ;; If rank < 5, matrix is rank-deficient
     (when (< rank 5)
       (println \"WARNING: Matrix is rank-deficient!\")
       (println \"Condition number estimate:\" 
                (/ (first diag-R) (last diag-R))))
     
     ;; Clean up...
     )
   ```
   
   Example 4 - Orthogonalization (Gram-Schmidt alternative):
   ```clojure
   ;; Extract orthonormal basis from column space of A
   
   (let [;; Matrix with potentially non-orthogonal columns
         A (create-random-matrix 100 20)
         
         ;; QR decomposition
         q-ptr (mem/alloc-pointer ::mem/pointer)
         r-ptr (mem/alloc-pointer ::mem/pointer)
         tau-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-qr/af-qr q-ptr r-ptr tau-ptr A)
         Q (mem/read-pointer q-ptr ::mem/pointer)
         
         ;; Q contains orthonormal basis
         ;; Extract first n columns (span same space as A columns)
         n 20
         basis (get-submatrix Q 0 0 100 n)]
     
     ;; Verify orthonormality
     (let [BT (transpose basis)
           BTB (matrix-multiply BT basis)
           I (identity-matrix n)
           diff (subtract BTB I)
           error (norm diff)]
       (println \"Orthonormality error:\" error)
       ;; Should be < 1e-6 for f32, < 1e-14 for f64
       )
     
     ;; Basis columns are orthonormal and span column space of A
     ;; More numerically stable than classical Gram-Schmidt
     
     ;; Clean up...
     )
   ```
   
   Example 5 - Complex matrices:
   ```clojure
   ;; QR decomposition for complex-valued matrices
   
   (let [;; Complex matrix (e.g., signal processing, quantum mechanics)
         real-part (create-random-matrix 50 30)
         imag-part (create-random-matrix 50 30)
         A-complex (create-complex-matrix real-part imag-part) ;; c32 or c64
         
         ;; QR decomposition
         q-ptr (mem/alloc-pointer ::mem/pointer)
         r-ptr (mem/alloc-pointer ::mem/pointer)
         tau-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-qr/af-qr q-ptr r-ptr tau-ptr A-complex)
         Q (mem/read-pointer q-ptr ::mem/pointer) ;; Unitary matrix
         R (mem/read-pointer r-ptr ::mem/pointer) ;; Upper triangular
         
         ;; For complex: Q is unitary (Q^H × Q = I)
         ;; ^H is conjugate transpose
         QH (conjugate-transpose Q)
         QHQ (matrix-multiply QH Q)
         I (identity-matrix 50)
         diff (subtract QHQ I)
         error (norm diff)]
     
     (println \"Unitary property error:\" error)
     ;; Q preserves complex inner products
     
     ;; Verify: Q×R ≈ A
     (let [QR (matrix-multiply Q R)
           diff (subtract QR A-complex)
           reconstruction-error (norm diff)]
       (println \"Reconstruction error:\" reconstruction-error))
     
     ;; Clean up...
     )
   ```
   
   Example 6 - Condition number estimation:
   ```clojure
   ;; Estimate matrix condition number from QR diagonal
   
   (let [A (create-random-matrix 100 100)
         
         ;; QR decomposition
         q-ptr (mem/alloc-pointer ::mem/pointer)
         r-ptr (mem/alloc-pointer ::mem/pointer)
         tau-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-qr/af-qr q-ptr r-ptr tau-ptr A)
         R (mem/read-pointer r-ptr ::mem/pointer)
         
         ;; Extract diagonal
         diag-R (get-diagonal R)
         abs-diag (map abs diag-R)
         
         ;; Condition number estimate: max/min of |diagonal elements|
         r-max (apply max abs-diag)
         r-min (apply min abs-diag)
         kappa-estimate (/ r-max r-min)]
     
     (println \"Condition number estimate:\" kappa-estimate)
     
     (cond
       (< kappa-estimate 100)
       (println \"Matrix is well-conditioned\")
       
       (< kappa-estimate 1e6)
       (println \"Matrix is moderately conditioned\")
       
       :else
       (println \"WARNING: Matrix is ill-conditioned!\")
       (println \"Numerical issues likely in solving linear systems\"))
     
     ;; Note: This is only an estimate
     ;; For accurate condition number, use SVD
     
     ;; Clean up...
     )
   ```
   
   Example 7 - Empty matrix handling:
   ```clojure
   ;; QR of empty matrix returns empty arrays
   
   (let [dims (mem/alloc-long [0 0]) ;; 0×0 matrix
         in-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-array/af-create-array in-ptr
                                     (mem/alloc-float 0)
                                     0 dims 0) ;; Empty array
         in (mem/read-pointer in-ptr ::mem/pointer)
         
         q-ptr (mem/alloc-pointer ::mem/pointer)
         r-ptr (mem/alloc-pointer ::mem/pointer)
         tau-ptr (mem/alloc-pointer ::mem/pointer)
         err (af-qr/af-qr q-ptr r-ptr tau-ptr in)]
     
     ;; Returns success with empty arrays
     ;; Q, R, tau all have 0 elements
     (println \"Error code:\" err) ;; AF_SUCCESS
     
     ;; Clean up...
     )
   ```
   
   Common Use Cases:
   
   1. **Solving Linear Systems**:
      - More stable than LU for ill-conditioned matrices
      - Solve Ax = b via Q^T×b then back substitution
   
   2. **Least Squares Regression**:
      - Overdetermined systems (more equations than unknowns)
      - Optimal solution: x = R^(-1) × Q^T × b
   
   3. **Orthogonalization**:
      - Convert arbitrary basis to orthonormal basis
      - More stable than Gram-Schmidt process
   
   4. **Rank Determination**:
      - Check diagonal of R for small elements
      - Indicates linear dependence in columns
   
   5. **Eigenvalue Algorithms**:
      - QR iteration for eigenvalue computation
      - Foundation for advanced decompositions
   
   6. **Numerical Analysis**:
      - Condition number estimation
      - Preconditioning for iterative methods
   
   7. **Signal Processing**:
      - Subspace methods (MUSIC, ESPRIT)
      - Adaptive filtering
   
   Error Handling:
   - AF_SUCCESS (0): Decomposition successful
   - AF_ERR_ARG: Invalid arguments (null pointers, wrong types)
   - AF_ERR_SIZE: Invalid dimensions
   - AF_ERR_TYPE: Non-floating type (integers not supported)
   - AF_ERR_BATCH: Input has > 2 dimensions (batch not supported)
   - AF_ERR_NOT_CONFIGURED: LAPACK not available
   - AF_ERR_NO_MEM: Insufficient memory for decomposition
   
   Debugging Tips:
   
   1. **Check dimensions**: Ensure m, n > 0 and input is 2D
   2. **Verify reconstruction**: Compute Q×R and compare to A
   3. **Test orthogonality**: Compute Q^T×Q, should be identity
   4. **Examine R diagonal**: Small values indicate ill-conditioning
   5. **Watch for NaN/Inf**: Indicates numerical overflow/underflow
   6. **Use appropriate precision**: f64 for ill-conditioned problems
   
   Best Practices:
   
   1. **Precision Selection**:
      - Use f64 for ill-conditioned matrices (κ > 10^6)
      - f32 sufficient for well-conditioned matrices
      - Complex types when needed (preserve phase information)
   
   2. **Memory Management**:
      - Always release returned arrays (Q, R, tau)
      - Consider in-place variant for large matrices
      - Check available memory before large decompositions
   
   3. **Numerical Stability**:
      - Check condition number before solving systems
      - Use column pivoting for rank-deficient matrices
      - Consider SVD for severely ill-conditioned cases
   
   4. **Performance Optimization**:
      - Batch processing: Loop in user code (not supported natively)
      - Reuse tau for multiple solves with same A
      - Use in-place variant when Q not needed explicitly
   
   5. **Verification**:
      - Check ||Q×R - A|| / ||A|| < ε (reconstruction error)
      - Check ||Q^T×Q - I|| < ε (orthogonality)
      - Compare results with known solutions for testing
   
   6. **Error Checking**:
      - Always check return code
      - Validate input dimensions before call
      - Handle rank-deficient cases appropriately
   
   7. **Type Consistency**:
      - Match precision to problem requirements
      - Don't mix real and complex unnecessarily
      - Convert integers to float/double first
   
   8. **Documentation**:
      - Document matrix dimensions and expected ranks
      - Note condition number when known
      - Specify precision requirements
   
   9. **Testing**:
      - Test with identity matrix (Q=I, R=A)
      - Test with orthogonal input (R should have ±1 diagonal)
      - Test with singular matrices (R has zero diagonal elements)
   
   10. **Integration**:
       - Combine with solve routines for linear systems
       - Use with matrix multiply for verification
       - Integrate with norm functions for error checking
   
   Limitations:
   
   1. **No Batch Processing**:
      - Cannot decompose multiple matrices at once
      - Error if input has > 2 dimensions
      - Must loop manually for batch operations
   
   2. **Memory Intensive**:
      - Allocates full Q matrix (m×m)
      - Large matrices may exceed GPU memory
      - Consider in-place variant for memory savings
   
   3. **No Column Pivoting**:
      - Cannot handle rank-deficient matrices optimally
      - Use SVD for better rank-deficient handling
      - No automatic column reordering
   
   4. **Type Restrictions**:
      - Only floating and complex types
      - Integers must be converted first
      - No mixed-precision operations
   
   5. **2D Only**:
      - Cannot handle higher-dimensional tensors
      - No tensor decomposition support
      - Reshape to 2D if needed
   
   6. **Requires LAPACK**:
      - Must have LAPACK backend available
      - Check with af-is-lapack-available
      - May not work on all platforms
   
   7. **Numerical Precision**:
      - Limited by floating-point precision
      - f32: ~7 digits, f64: ~16 digits
      - Ill-conditioned matrices lose accuracy
   
   Returns:
   ArrayFire error code (af_err enum):
   - 0 (AF_SUCCESS): Decomposition completed successfully
   - Non-zero: Error occurred (see error codes above)
   
   See also:
   - af-qr-inplace: In-place QR (memory-efficient, overwrites input)
   - af-lu: LU decomposition (faster, less stable)
   - af-cholesky: Cholesky decomposition (positive-definite only)
   - af-svd: Singular value decomposition (most general, handles rank deficiency)
   - af-solve: Solve linear systems using decompositions"
  "af_qr" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_qr_inplace(af_array *tau, af_array in)
(defcfn af-qr-inplace
  "Perform in-place QR decomposition, overwriting input with packed QR.
   
   Computes QR decomposition A = Q × R, storing the result in packed format
   within the input array. More memory-efficient than af-qr, as it does not
   allocate a separate Q matrix. Returns only tau coefficients needed to
   reconstruct Q from the packed representation.
   
   Parameters:
   - tau: out pointer for Householder reflection coefficients (min(m,n))
   - in: in/out array; input matrix A on entry, packed QR on exit (m×n)
   
   In-place Operation:
   **Critical**: The input array is MODIFIED:
   - On entry: Contains matrix A to decompose
   - On exit: Contains packed QR representation:
     * Upper triangle (including diagonal): R matrix
     * Lower triangle (below diagonal): Householder vectors for Q
   - Original A is DESTROYED
   - Make a copy first if you need to preserve A
   
   Packed Storage Format:
   After decomposition, the input array contains:
   ```
   [r11 r12 r13 ... r1n]  ← R matrix (upper triangle)
   [v21 r22 r23 ... r2n]  ← R matrix (upper), v vectors (lower)
   [v31 v32 r33 ... r3n]
   [v41 v42 v43 ... r4n]
   ```
   where:
   - r_ij: Elements of upper triangular matrix R
   - v_ij: Householder reflection vectors (implied unit diagonal)
   - tau: Separate array with scaling factors τ_i
   
   Reconstructing Q:
   To reconstruct Q from packed format + tau:
   - Use LAPACK routine orgqr (real) or ungqr (complex)
   - Q = H_1 × H_2 × ... × H_k, where k = min(m,n)
   - Each H_i = I - τ_i × v_i × v_i^T
   - This is the same internal format used by LAPACK
   
   Extracting R:
   To extract R matrix from packed result:
   ```clojure
   ;; R is the upper triangle (including diagonal) of the packed array
   (let [R (upper-triangle packed-qr)]
     ;; R is now m×n upper triangular matrix
     )
   ```
   
   Output Properties:
   
   **tau (Householder Coefficients)**:
   - Type: Same as input (f32, f64, c32, c64)
   - Length: min(m, n) elements
   - Values: τ_i for each Householder reflection H_i
   - Used to reconstruct Q: H_i = I - τ_i × v_i × v_i^T
   - Needed for LAPACK solve routines (ormqr/unmqr)
   
   **Packed QR Array** (modified input):
   - Upper triangle: R matrix elements
   - Lower triangle: Householder vectors v_i (unit diagonal implicit)
   - Dimensions: m×n (unchanged from input)
   - Type: Same as input (f32, f64, c32, c64)
   
   Algorithm (LAPACK GEQRF):
   
   1. **Householder Factorization**:
      For k = 1 to min(m,n):
      - Select column k, elements below diagonal: x
      - Compute Householder vector: v = x + sign(x_1)×||x||×e_1
      - Compute scaling: τ = 2 / (v^T × v)
      - Apply reflection: A := (I - τ×v×v^T) × A
      - Zero out elements below diagonal in column k
   
   2. **In-place Storage**:
      - Overwrite upper triangle with R elements
      - Store v vectors in lower triangle (v_i in column i)
      - Save τ values in separate array
      - No extra m×m allocation needed
   
   3. **Result**:
      - Modified input: Packed QR format
      - tau array: Householder coefficients
      - Original matrix A is lost
   
   Performance:
   - Complexity: O(2mn² - (2/3)n³) flops (same as full QR)
   - Memory: O(m×n + min(m,n)) vs O(m² + m×n) for full QR
   - Speed: Faster than af-qr (no Q reconstruction)
   - Memory savings: Significant for large m (no m×m Q matrix)
   
   Compared to af-qr:
   - Less memory: No separate Q matrix (saves m×m elements)
   - Faster: Skips Q reconstruction step
   - Less convenient: Q not immediately available
   - Destructive: Overwrites input array
   - Use when: Memory limited or Q not needed explicitly
   
   Type Support:
   - f32: Single precision real
   - f64: Double precision real
   - c32: Complex single precision
   - c64: Complex double precision
   - Integers: NOT supported (convert first)
   
   Example 1 - Basic in-place QR:
   ```clojure
   (require '[coffi.mem :as mem])
   (require '[org.soulspace.arrayfire.ffi.array :as af-array])
   (require '[org.soulspace.arrayfire.ffi.qr :as af-qr])
   
   ;; Create 5×4 matrix
   (let [data (range 1.0 21.0) ;; 20 elements
         dims (mem/alloc-long [5 4])
         in-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-array/af-create-array in-ptr
                                     (mem/serialize data ::mem/float)
                                     5 dims 0) ;; 0 = f32
         A (mem/read-pointer in-ptr ::mem/pointer)
         
         ;; IMPORTANT: Make a copy if you need to preserve A
         A-copy (copy-array A) ;; If needed later
         
         ;; Perform in-place QR (A is modified!)
         tau-ptr (mem/alloc-pointer ::mem/pointer)
         err (af-qr/af-qr-inplace tau-ptr A)
         tau (mem/read-pointer tau-ptr ::mem/pointer)]
     
     ;; A now contains packed QR format
     ;; - Upper triangle: R matrix
     ;; - Lower triangle: Householder vectors
     ;; tau contains 4 coefficients (min(5,4) = 4)
     
     ;; Extract R from upper triangle
     (let [R (upper-triangle A)] ;; 5×4 upper triangular
       ;; R is the same R as from af-qr
       ;; Use R for solving systems, etc.
       )
     
     ;; Clean up
     (af-array/af-release-array A) ;; Contains packed QR
     (af-array/af-release-array tau))
   ```
   
   Example 2 - Solving linear system (memory-efficient):
   ```clojure
   ;; Solve Ax = b using in-place QR (saves memory)
   ;; For m×n system with m ≥ n
   
   (let [m 200, n 100
         A (create-random-matrix m n)
         b (create-random-vector m)
         
         ;; Make copy of A (will be destroyed)
         A-for-qr (copy-array A)
         
         ;; In-place QR
         tau-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-qr/af-qr-inplace tau-ptr A-for-qr)
         tau (mem/read-pointer tau-ptr ::mem/pointer)
         
         ;; Extract R from upper triangle
         R (upper-triangle A-for-qr) ;; Now have R
         R-square (get-submatrix R 0 0 n n) ;; Top n×n block
         
         ;; Apply Q^T to b using tau and packed representation
         ;; This requires LAPACK ormqr/unmqr routine
         ;; (Not directly exposed in ArrayFire C API)
         ;; Alternative: Use af_solve with the factorization
         
         ;; For simple case, extract R and use triangular solve
         ;; (This is less efficient than using Q directly)
         ]
     
     ;; Note: Full solution requires LAPACK ormqr
     ;; ArrayFire's solve functions can use the factorization
     
     ;; Clean up...
     )
   ```
   
   Example 3 - Memory comparison:
   ```clojure
   ;; Compare memory usage: in-place vs full QR
   
   (let [m 4000, n 2000 ;; Large matrix
         element-size 4 ;; bytes per f32
         
         ;; Full QR memory:
         ;; Q: m×m = 4000×4000 = 16M elements
         ;; R: m×n = 4000×2000 = 8M elements
         ;; tau: min(m,n) = 2000 elements
         ;; Total: 24M elements ≈ 96 MB
         full-qr-memory (* (+ (* m m) (* m n) (min m n)) element-size)
         
         ;; In-place QR memory:
         ;; Packed: m×n = 4000×2000 = 8M elements
         ;; tau: 2000 elements
         ;; Total: 8M elements ≈ 32 MB
         inplace-qr-memory (* (+ (* m n) (min m n)) element-size)
         
         savings (- full-qr-memory inplace-qr-memory)]
     
     (println \"Full QR memory:\" (format \"%.1f MB\" (/ full-qr-memory 1e6)))
     (println \"In-place QR memory:\" (format \"%.1f MB\" (/ inplace-qr-memory 1e6)))
     (println \"Memory savings:\" (format \"%.1f MB\" (/ savings 1e6)))
     (println \"Reduction:\" (format \"%.1f%%\" 
                                    (* 100.0 (/ savings full-qr-memory))))
     
     ;; For large matrices, in-place saves significant memory
     ;; Especially when m >> n (many more rows than columns)
     )
   ```
   
   Example 4 - Preserving original matrix:
   ```clojure
   ;; Pattern for in-place QR when you need to keep original A
   
   (let [A (create-important-matrix 100 80)
         
         ;; Make a copy for QR (A will remain unchanged)
         A-copy (copy-array A)
         
         ;; In-place QR on the copy
         tau-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-qr/af-qr-inplace tau-ptr A-copy)
         tau (mem/read-pointer tau-ptr ::mem/pointer)
         
         ;; Now:
         ;; - A still contains original matrix
         ;; - A-copy contains packed QR
         ;; - tau contains Householder coefficients
         
         ;; Use A for other computations
         ;; Use A-copy (packed QR) + tau for solving systems
         ]
     
     ;; This is still more memory-efficient than full QR
     ;; if you need both A and the factorization
     
     ;; Clean up...
     )
   ```
   
   Example 5 - Extracting R matrix:
   ```clojure
   ;; Extract upper triangular R from packed QR result
   
   (let [A (create-random-matrix 6 4)
         
         ;; In-place QR
         tau-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-qr/af-qr-inplace tau-ptr A)
         tau (mem/read-pointer tau-ptr ::mem/pointer)
         
         ;; A now contains:
         ;; [r11 r12 r13 r14]
         ;; [v21 r22 r23 r24]
         ;; [v31 v32 r33 r34]
         ;; [v41 v42 v43 r44]
         ;; [v51 v52 v53 v54] ← these are all zero (since 6 > 4)
         ;; [v61 v62 v63 v64] ← these are all zero
         
         ;; Extract R by taking upper triangle
         R (upper-triangle A)]
     
     ;; R is 6×4 upper triangular:
     ;; - Top 4×4: Non-zero upper triangle
     ;; - Bottom 2×4: All zeros (since m > n)
     
     ;; For square matrices (m=n), R is n×n upper triangular
     ;; For wide matrices (m<n), R is m×n trapezoidal
     
     ;; Use R for back substitution, condition number, etc.
     
     ;; Clean up...
     )
   ```
   
   Example 6 - When NOT to use in-place:
   ```clojure
   ;; Cases where full QR (af-qr) is better
   
   ;; Case 1: Need explicit Q matrix
   ;; (e.g., orthogonalization, eigenvalue algorithms)
   (let [A (create-matrix 50 50)]
     ;; Use af-qr to get separate Q and R
     ;; In-place would require manual Q reconstruction
     )
   
   ;; Case 2: Need to preserve original A AND use Q
   (let [A (important-matrix)]
     ;; Using in-place: Must copy A + reconstruct Q
     ;; Using full QR: Direct Q and R, can still use A
     ;; Memory savings from in-place negated by Q reconstruction
     )
   
   ;; Case 3: Small matrices
   ;; (memory not a concern, convenience more important)
   (let [A (create-matrix 10 10)]
     ;; Use af-qr for simpler code
     ;; Memory difference negligible
     )
   
   ;; Use in-place QR when:
   ;; - Large matrices (memory constrained)
   ;; - Q not needed explicitly (only R)
   ;; - Solving systems (use with LAPACK routines)
   ;; - Original A not needed after factorization
   ```
   
   Example 7 - Complex matrices:
   ```clojure
   ;; In-place QR for complex matrices
   
   (let [;; Complex matrix (e.g., quantum mechanics, signal processing)
         real (create-random-matrix 60 40)
         imag (create-random-matrix 60 40)
         A-complex (create-complex-matrix real imag) ;; c32 or c64
         
         ;; In-place QR (A-complex will be overwritten)
         tau-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-qr/af-qr-inplace tau-ptr A-complex)
         tau (mem/read-pointer tau-ptr ::mem/pointer)
         
         ;; A-complex now contains packed QR:
         ;; - Upper triangle: Complex R matrix
         ;; - Lower triangle: Complex Householder vectors
         ;; tau contains complex scaling factors
         
         ;; Extract R
         R-complex (upper-triangle A-complex)]
     
     ;; R-complex is upper triangular complex matrix
     ;; Use for solving complex linear systems
     ;; Diagonal |r_ii| indicates column independence
     
     ;; Clean up...
     )
   ```
   
   Common Use Cases:
   
   1. **Memory-Constrained Environments**:
      - Large matrices on limited GPU memory
      - Embedded systems with memory restrictions
      - Batch processing multiple matrices
   
   2. **Solving Linear Systems**:
      - Combine with LAPACK ormqr for Q^T×b
      - Use R for back substitution
      - More efficient than explicit Q
   
   3. **Least Squares (Q not needed)**:
      - Compute R and tau
      - Use LAPACK least squares solver
      - Avoids explicit Q construction
   
   4. **Iterative Algorithms**:
      - QR iteration for eigenvalues
      - Update factorization in-place
      - Minimize memory allocations
   
   5. **Preprocessing Pipelines**:
      - Factor once, solve multiple times
      - Keep packed format and tau
      - Reuse for multiple right-hand sides
   
   Error Handling:
   - AF_SUCCESS (0): Decomposition successful
   - AF_ERR_ARG: Invalid arguments (null tau pointer, invalid array)
   - AF_ERR_SIZE: Invalid dimensions
   - AF_ERR_TYPE: Non-floating type
   - AF_ERR_BATCH: Input has > 2 dimensions
   - AF_ERR_NOT_CONFIGURED: LAPACK not available
   - AF_ERR_NO_MEM: Insufficient memory
   
   Debugging Tips:
   
   1. **Check input preservation**: Copy A before in-place operation
   2. **Verify R extraction**: Upper triangle should be valid
   3. **Inspect tau values**: Should be in reasonable range [0,2]
   4. **Compare with full QR**: Extract R, should match af-qr result
   5. **Test with identity**: In=I should give Out=I (R=I, trivial Q)
   
   Best Practices:
   
   1. **Memory Management**:
      - Always copy input if original needed
      - Release tau array after use
      - Consider memory vs convenience tradeoff
   
   2. **When to Use In-place**:
      - Large matrices (> 1000×1000)
      - Memory limited environments
      - Q not needed explicitly
      - Multiple solves with same A
   
   3. **When to Use Full QR**:
      - Small matrices (convenience)
      - Need explicit Q matrix
      - Orthogonalization tasks
      - Educational/debugging code
   
   4. **Precision**:
      - Use f64 for ill-conditioned matrices
      - f32 sufficient for well-conditioned
      - Match precision to problem requirements
   
   5. **Testing**:
      - Compare R with full QR result
      - Check R diagonal for rank
      - Verify dimensions after operation
   
   6. **Documentation**:
      - Note that input is destroyed
      - Document packed format usage
      - Specify when copy is needed
   
   7. **Integration**:
      - Use with LAPACK solve routines
      - Combine with matrix multiply for verification
      - Keep tau for multiple solves
   
   8. **Error Checking**:
      - Validate return code
      - Check dimensions before call
      - Handle memory allocation failures
   
   Limitations:
   
   1. **Destructive Operation**:
      - Input matrix A is DESTROYED
      - Must copy if original needed
      - No way to recover A after operation
   
   2. **Q Not Immediately Available**:
      - Q is in packed format
      - Requires LAPACK reconstruction (orgqr/ungqr)
      - Not directly usable like af-qr
   
   3. **No Batch Processing**:
      - Cannot process multiple matrices at once
      - Must loop manually
      - Each call overwrites one matrix
   
   4. **Type Restrictions**:
      - Only floating and complex types
      - Must convert integers first
      - No mixed-precision
   
   5. **LAPACK Dependency**:
      - Requires LAPACK backend
      - May not be available on all systems
      - Check with af-is-lapack-available
   
   6. **Packed Format Complexity**:
      - Extracting Q requires expertise
      - Not intuitive to work with
      - Prefer full QR for simplicity
   
   Returns:
   ArrayFire error code (af_err enum):
   - 0 (AF_SUCCESS): Decomposition completed, input modified
   - Non-zero: Error occurred, input may be corrupted
   
   See also:
   - af-qr: Full QR decomposition (separate Q, R, tau arrays)
   - af-lu-inplace: In-place LU decomposition
   - af-cholesky-inplace: In-place Cholesky decomposition
   - af-solve: Solve linear systems using decompositions"
  "af_qr_inplace" [::mem/pointer ::mem/pointer] ::mem/int)
