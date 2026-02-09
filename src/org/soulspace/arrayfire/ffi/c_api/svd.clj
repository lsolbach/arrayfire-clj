(ns org.soulspace.arrayfire.ffi.c-api.svd
  "ArrayFire FFI bindings for Singular Value Decomposition (SVD).

  Singular Value Decomposition is a fundamental matrix factorization technique
  in linear algebra that decomposes a matrix into three matrices, revealing
  its underlying structure and properties.

  ## Mathematical Foundation

  For any m×n matrix A, the SVD decomposes it as:
  ```
  A = U × Σ × V^H
  ```

  Where:
  - **U**: m×m unitary matrix (left singular vectors)
    * Columns are orthonormal eigenvectors of A×A^H
    * Span the column space of A
  - **Σ** (sigma): m×n diagonal matrix (singular values)
    * Diagonal entries σ_i ≥ 0 in descending order
    * σ_1 ≥ σ_2 ≥ ... ≥ σ_min(m,n) ≥ 0
  - **V^H**: n×n unitary matrix (right singular vectors, conjugate transpose)
    * Rows are orthonormal eigenvectors of A^H×A
    * Span the row space of A

  Note: ArrayFire returns `s` as a vector of diagonal values, not the full Σ matrix.

  ## Properties

  ### Unitary Matrices
  U and V are unitary (orthogonal for real matrices):
  ```
  U × U^H = U^H × U = I_m (identity)
  V × V^H = V^H × V = I_n (identity)
  ```

  ### Singular Values
  - Always real and non-negative (even for complex matrices)
  - Ordered: σ_1 ≥ σ_2 ≥ ... ≥ 0
  - Related to eigenvalues: σ_i = √λ_i where λ_i are eigenvalues of A^H×A
  - Invariant under unitary transformations

  ### Matrix Properties from SVD
  - **Rank**: Number of non-zero singular values
  - **Condition Number**: κ(A) = σ_max / σ_min
  - **2-Norm**: ||A||_2 = σ_max (largest singular value)
  - **Frobenius Norm**: ||A||_F = √(Σσ_i²)
  - **Nuclear Norm**: ||A||_* = Σσ_i (sum of singular values)

  ## Use Cases

  ### Linear Algebra & Numerical Analysis
  - **Matrix approximation**: Low-rank approximation (truncated SVD)
  - **Pseudoinverse**: A^+ = V × Σ^+ × U^H (Moore-Penrose inverse)
  - **Condition number**: Assess numerical stability
  - **Null space**: Find null space and range of a matrix
  - **Least squares**: Solve overdetermined systems

  ### Signal Processing
  - **Noise reduction**: Filter small singular values (denoising)
  - **Compression**: Represent signals with fewer components
  - **Principal Component Analysis (PCA)**: Find principal directions
  - **Blind source separation**: Independent component analysis (ICA)

  ### Data Science & Machine Learning
  - **Dimensionality reduction**: Project high-dimensional data to lower dimensions
  - **Recommender systems**: Collaborative filtering (Netflix Prize)
  - **Latent semantic analysis**: Text mining, document similarity
  - **Image compression**: JPEG-like compression
  - **Feature extraction**: Discover latent features

  ### Computer Vision
  - **Image denoising**: Remove noise while preserving structure
  - **Face recognition**: Eigenfaces (faces as linear combinations)
  - **3D reconstruction**: Structure from motion
  - **Image watermarking**: Embed information in singular values

  ### Scientific Computing
  - **Quantum mechanics**: Density matrix decomposition
  - **Control theory**: System identification, model reduction
  - **Structural analysis**: Modal analysis, vibration modes
  - **Fluid dynamics**: Proper orthogonal decomposition (POD)

  ## Computational Complexity

  ### Standard SVD
  - **Time**: O(min(m²n, mn²)) for m×n matrix
    * For square matrix: O(n³)
  - **Space**: O(mn + m² + n²) for storing A, U, V
  - **Algorithm**: Typically uses divide-and-conquer or QR iteration

  ### In-Place SVD
  - **Time**: Same as standard SVD (O(min(m²n, mn²)))
  - **Space**: O(m² + n²) - saves input array memory
  - **Constraint**: Requires m ≥ n (rows ≥ columns)

  ## Numerical Considerations

  ### Stability
  - SVD is numerically stable (backward stable algorithm)
  - Small perturbations in A → small perturbations in U, Σ, V
  - More stable than computing eigenvalues directly

  ### Accuracy
  - Singular values accurate to machine precision
  - Small singular values may be affected by roundoff
  - Use double precision (f64) for higher accuracy

  ### Conditioning
  - Well-conditioned: All singular values similar magnitude
  - Ill-conditioned: Large ratio σ_max / σ_min
  - Rank-deficient: Some singular values ≈ 0

  ## Performance Optimization

  ### When to Use In-Place SVD
  Use `af-svd-inplace` when:
  - Input matrix is no longer needed
  - Memory is constrained
  - Matrix has m ≥ n (tall or square)

  Use standard `af-svd` when:
  - Need to preserve input matrix
  - Matrix has m < n (wide)
  - Performing multiple decompositions

  ### Type Selection
  - **f32**: Faster, ~7 significant digits, suitable for most applications
  - **f64**: Slower, ~16 significant digits, use for high-precision needs
  - **c32/c64**: Complex matrices, same performance as f32/f64

  ### Size Guidelines
  - Small (< 100×100): Very fast (<1ms)
  - Medium (100×1000): Fast (10-100ms)
  - Large (1000×1000): Moderate (100ms-1s)
  - Huge (5000×5000): Slow (several seconds)

  ## Truncated SVD (Low-Rank Approximation)

  Keep only top k singular values for approximation:
  ```clojure
  ;; Approximate A with rank-k matrix
  (let [[u s vt] (svd A)
        k 10  ;; keep top 10 components
        u-k (select u [all (range k)])
        s-k (select s (range k))
        vt-k (select vt [(range k) all])
        A-approx (matmul u-k (matmul (diag s-k) vt-k))]
    A-approx)
  ```

  Applications:
  - **Compression**: Store A-approx instead of A (smaller)
  - **Denoising**: Remove noise in small singular values
  - **Speed**: Faster computations with low-rank matrices

  ## SVD vs Eigendecomposition

  | Property | SVD | Eigendecomposition |
  |----------|-----|-------------------|
  | Applicability | Any matrix | Only square matrices |
  | Stability | More stable | Can be unstable |
  | Values | Always real ≥ 0 | Can be complex |
  | Vectors | Always orthonormal | May not be orthogonal |
  | Geometric | Stretch + rotate | Stretch along eigenvectors |

  ## Common Patterns

  ### Pattern 1: Pseudoinverse
  ```clojure
  ;; Compute Moore-Penrose pseudoinverse: A^+ = V × Σ^+ × U^H
  (defn pseudoinverse [A tol]
    (let [[u s vt] (svd A)
          ;; Invert singular values above threshold
          s-inv (select-modify s #(if (> % tol) (/ 1.0 %) 0.0))
          ;; A^+ = V × Σ^+ × U^H
          result (matmul (transpose vt)
                        (matmul (diag s-inv) (conjg (transpose u))))]
      result))
  ```

  ### Pattern 2: Rank Determination
  ```clojure
  ;; Count non-zero singular values
  (defn matrix-rank [A tol]
    (let [[_ s _] (svd A)]
      (count (filter #(> % tol) s))))
  ```

  ### Pattern 3: Condition Number
  ```clojure
  ;; Measure numerical stability
  (defn condition-number [A]
    (let [[_ s _] (svd A)
          sigma-max (first s)        ;; largest singular value
          sigma-min (last (filter pos? s))]  ;; smallest non-zero
      (if sigma-min
        (/ sigma-max sigma-min)
        Double/POSITIVE_INFINITY)))  ;; rank-deficient
  ```

  ### Pattern 4: Principal Component Analysis (PCA)
  ```clojure
  ;; Find principal components of data matrix
  (defn pca [X n-components]
    ;; X is data matrix: rows = samples, cols = features
    (let [;; Center the data
          X-mean (mean X 0)
          X-centered (sub X X-mean)
          ;; SVD of centered data
          [u s vt] (svd X-centered)
          ;; Principal components = right singular vectors
          components (select vt [(range n-components) all])
          ;; Explained variance = squared singular values
          explained-var (pow s 2)
          ;; Project data onto principal components
          X-transformed (matmul X-centered (transpose components))]
      {:components components
       :explained-variance explained-var
       :transformed X-transformed}))
  ```

  ### Pattern 5: Image Compression
  ```clojure
  ;; Compress grayscale image using SVD
  (defn compress-image [img k]
    ;; img is m×n grayscale image, k = number of components
    (let [[u s vt] (svd img)
          ;; Keep top k components
          u-k (select u [all (range k)])
          s-k (select s (range k))
          vt-k (select vt [(range k) all])
          ;; Reconstruct
          compressed (matmul u-k (matmul (diag s-k) vt-k))
          ;; Compression ratio
          ratio (/ (* k (+ (rows img) (cols img) 1))
                   (* (rows img) (cols img)))]
      {:image compressed
       :compression-ratio ratio
       :quality (/ (sum s-k) (sum s))}))
  ```

  ## Type Support

  **Supported Types**:
  - **f32** (float): Single precision (recommended for most applications)
  - **f64** (double): Double precision (high accuracy needs)
  - **c32** (cfloat): Complex single precision
  - **c64** (cdouble): Complex double precision

  **Output Types**:
  - Singular values (s): Always real (base type of input)
    * f32/c32 → f32 singular values
    * f64/c64 → f64 singular values
  - U and V matrices: Same type as input (complex if input is complex)

  ## Error Handling

  Common issues and solutions:
  - **Empty matrix**: Returns empty arrays for 0-dimensional input
  - **m < n for in-place**: Use standard SVD or transpose first
  - **Non-convergence**: Very rare, try different precision
  - **Memory**: Use in-place variant for large matrices

  ## Reconstruction Formula

  To verify SVD correctness:
  ```clojure
  ;; Reconstruct original matrix from SVD components
  (defn reconstruct-svd [u s vt]
    ;; Create diagonal matrix from singular values
    (let [m (rows u)
          n (cols vt)
          sigma (zeros m n)]
      ;; Fill diagonal with singular values
      (doseq [i (range (count s))]
        (set sigma i i (nth s i)))
      ;; A = U × Σ × V^H
      (matmul u (matmul sigma vt))))

  ;; Check reconstruction error
  (let [[u s vt] (svd A)
        A-reconstructed (reconstruct-svd u s vt)
        error (norm (sub A A-reconstructed))]
    (println \"Reconstruction error:\" error))
  ```

  ## Notes on Unitary Matrices

  For real matrices, U and V are orthogonal:
  - Columns have unit length: ||u_i|| = 1
  - Columns are mutually perpendicular: u_i · u_j = 0 (i ≠ j)
  - U^T × U = I and V^T × V = I

  For complex matrices, U and V are unitary:
  - Columns have unit length in complex inner product
  - U^H × U = I and V^H × V = I (where ^H is conjugate transpose)

  ## LAPACK Backend

  ArrayFire SVD uses highly optimized LAPACK routines:
  - **DGESVD**: Double precision real
  - **SGESVD**: Single precision real
  - **ZGESVD**: Double precision complex
  - **CGESVD**: Single precision complex

  These are industry-standard, battle-tested algorithms used in:
  - MATLAB, NumPy/SciPy, R
  - Scientific computing worldwide
  - Decades of optimization and validation

  ## Resources

  - Golub & Van Loan: \"Matrix Computations\" (canonical reference)
  - Trefethen & Bau: \"Numerical Linear Algebra\"
  - ArrayFire LAPACK docs: https://arrayfire.org/docs/group__lapack__factor__func__svd.htm
  - LAPACK documentation: http://www.netlib.org/lapack/

  See also:
  - Eigenvalue decomposition: af_svd can be used to compute eigenvalues
  - QR decomposition: af_qr for orthogonal factorization
  - LU decomposition: af_lu for solving linear systems
  - Cholesky decomposition: af_cholesky for positive definite matrices
  - Matrix inversion: af_inverse uses SVD internally
  - Pseudoinverse: af_pinverse (uses SVD)
  - Matrix rank: af_rank (uses SVD)"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;;
;; Singular Value Decomposition Functions
;;

(defcfn af-svd
  "Perform Singular Value Decomposition of a matrix.

  Decomposes input matrix A into three matrices: A = U × Σ × V^H

  Parameters:
  - u: Output pointer for U matrix (m×m unitary matrix, left singular vectors)
  - s: Output pointer for singular values (min(m,n) vector, diagonal of Σ)
  - vt: Output pointer for V^H matrix (n×n conjugate transpose of right singular vectors)
  - in: Input matrix (m×n)

  Returns:
  Error code indicating success or failure.

  ## Mathematical Details

  For input matrix A (m×n), the decomposition produces:
  ```
  A = U × Σ × V^H
  ```

  Where:
  - **U** (m×m): Orthonormal matrix of left singular vectors
    * Columns are eigenvectors of A×A^H
  - **Σ** (m×n): Diagonal matrix of singular values (returned as vector s)
    * s[i] = σ_i where σ_1 ≥ σ_2 ≥ ... ≥ 0
  - **V^H** (n×n): Conjugate transpose of right singular vectors
    * Rows are eigenvectors of A^H×A

  ## Output Dimensions

  - **u**: Always m×m (full unitary matrix)
  - **s**: min(m,n) vector (diagonal singular values only)
  - **vt**: Always n×n (full unitary matrix)

  Example for 5×3 matrix:
  ```
  A: 5×3  →  U: 5×5,  s: [3],  V^H: 3×3
  ```

  Example for 3×5 matrix:
  ```
  A: 3×5  →  U: 3×3,  s: [3],  V^H: 5×5
  ```

  ## Basic Example

  ```clojure
  (require '[org.soulspace.arrayfire.ffi.core :as af])
  (require '[org.soulspace.arrayfire.ffi.svd :as svd])
  (require '[coffi.mem :as mem])

  ;; Create 4×3 matrix
  (let [data [1.0 2.0 3.0
              4.0 5.0 6.0
              7.0 8.0 9.0
              10.0 11.0 12.0]
        A (af/af-create-array data [4 3] :f32)
        
        ;; Allocate output pointers
        u-ptr (mem/alloc-instance ::mem/pointer)
        s-ptr (mem/alloc-instance ::mem/pointer)
        vt-ptr (mem/alloc-instance ::mem/pointer)]
    
    ;; Perform SVD
    (svd/af-svd u-ptr s-ptr vt-ptr A)
    
    ;; Extract results
    (let [u (mem/read-pointer u-ptr ::mem/pointer)   ;; 4×4 matrix
          s (mem/read-pointer s-ptr ::mem/pointer)   ;; 3 singular values
          vt (mem/read-pointer vt-ptr ::mem/pointer)] ;; 3×3 matrix
      
      ;; Print results
      (println \"U (left singular vectors):\")
      (af/af-print-array u)
      
      (println \"\\nSingular values:\")
      (af/af-print-array s)
      
      (println \"\\nV^H (right singular vectors, conjugate transpose):\")
      (af/af-print-array vt)
      
      ;; Verify reconstruction: A ≈ U × diag(s) × V^H
      (let [sigma (af/af-diag s 0 false)  ;; Create m×n diagonal matrix
            reconstructed (af/af-matmul u (af/af-matmul sigma vt))
            error (af/af-norm (af/af-sub A reconstructed) :vector-2)]
        (println \"\\nReconstruction error:\" error))
      
      ;; Release arrays
      (af/af-release-array A)
      (af/af-release-array u)
      (af/af-release-array s)
      (af/af-release-array vt)))
  ```

  ## Example: Low-Rank Approximation

  ```clojure
  ;; Approximate matrix with rank-k approximation
  (defn low-rank-approx [A k]
    (let [u-ptr (mem/alloc-instance ::mem/pointer)
          s-ptr (mem/alloc-instance ::mem/pointer)
          vt-ptr (mem/alloc-instance ::mem/pointer)]
      
      (svd/af-svd u-ptr s-ptr vt-ptr A)
      
      (let [u (mem/read-pointer u-ptr ::mem/pointer)
            s (mem/read-pointer s-ptr ::mem/pointer)
            vt (mem/read-pointer vt-ptr ::mem/pointer)
            
            ;; Keep only top k components
            u-k (af/af-col u (range k))
            s-k (af/af-row s (range k))
            vt-k (af/af-row vt (range k))
            
            ;; Reconstruct: A_k = U_k × Σ_k × V_k^H
            sigma-k (af/af-diag s-k 0 false)
            A-approx (af/af-matmul u-k (af/af-matmul sigma-k vt-k))
            
            ;; Compute approximation error
            diff (af/af-sub A A-approx)
            error (af/af-norm diff :frobenius)]
        
        (println \"Kept\" k \"singular values\")
        (println \"Approximation error:\" error)
        
        ;; Clean up
        (af/af-release-array u)
        (af/af-release-array s)
        (af/af-release-array vt)
        
        A-approx)))

  ;; Use for image compression
  (let [img (load-image \"photo.jpg\")  ;; 512×512 image
        compressed (low-rank-approx img 50)]  ;; Keep 50 components
    (save-image compressed \"compressed.jpg\"))
  ```

  ## Example: Condition Number

  ```clojure
  ;; Compute condition number (ratio of largest to smallest singular value)
  (defn condition-number [A]
    (let [u-ptr (mem/alloc-instance ::mem/pointer)
          s-ptr (mem/alloc-instance ::mem/pointer)
          vt-ptr (mem/alloc-instance ::mem/pointer)]
      
      (svd/af-svd u-ptr s-ptr vt-ptr A)
      
      (let [s (mem/read-pointer s-ptr ::mem/pointer)
            ;; Get singular values as host vector
            s-host (af/af-copy-to-host s)
            sigma-max (first s-host)
            sigma-min (last (filter pos? s-host))
            cond (if sigma-min (/ sigma-max sigma-min) ##Inf)]
        
        ;; Release arrays
        (af/af-release-array (mem/read-pointer u-ptr ::mem/pointer))
        (af/af-release-array s)
        (af/af-release-array (mem/read-pointer vt-ptr ::mem/pointer))
        
        {:condition-number cond
         :largest-singular-value sigma-max
         :smallest-singular-value sigma-min
         :status (cond
                   (< cond 10) :well-conditioned
                   (< cond 1000) :moderately-conditioned
                   :else :ill-conditioned)})))

  (let [result (condition-number my-matrix)]
    (println \"Condition number:\" (:condition-number result))
    (println \"Status:\" (:status result)))
  ```

  ## Example: Matrix Rank

  ```clojure
  ;; Determine numerical rank using singular values
  (defn matrix-rank [A tolerance]
    (let [s-ptr (mem/alloc-instance ::mem/pointer)
          u-ptr (mem/alloc-instance ::mem/pointer)
          vt-ptr (mem/alloc-instance ::mem/pointer)]
      
      (svd/af-svd u-ptr s-ptr vt-ptr A)
      
      (let [s (mem/read-pointer s-ptr ::mem/pointer)
            s-host (af/af-copy-to-host s)
            ;; Count singular values above threshold
            rank (count (filter #(> % tolerance) s-host))]
        
        ;; Clean up
        (af/af-release-array (mem/read-pointer u-ptr ::mem/pointer))
        (af/af-release-array s)
        (af/af-release-array (mem/read-pointer vt-ptr ::mem/pointer))
        
        rank)))

  ;; Check if matrix is full rank
  (let [A (af/af-randn [100 50] :f64)
        rank (matrix-rank A 1e-10)]
    (println \"Matrix rank:\" rank)
    (println \"Full rank?\" (= rank 50)))
  ```

  ## Example: Pseudoinverse (Moore-Penrose)

  ```clojure
  ;; Compute pseudoinverse using SVD: A^+ = V × Σ^+ × U^H
  (defn pseudoinverse [A tolerance]
    (let [u-ptr (mem/alloc-instance ::mem/pointer)
          s-ptr (mem/alloc-instance ::mem/pointer)
          vt-ptr (mem/alloc-instance ::mem/pointer)]
      
      (svd/af-svd u-ptr s-ptr vt-ptr A)
      
      (let [u (mem/read-pointer u-ptr ::mem/pointer)
            s (mem/read-pointer s-ptr ::mem/pointer)
            vt (mem/read-pointer vt-ptr ::mem/pointer)
            
            ;; Invert singular values (reciprocal), zero out small values
            s-inv (af/af-select s
                                #(if (> % tolerance) (/ 1.0 %) 0.0))
            
            ;; A^+ = V × Σ^+ × U^H
            ;; V = (V^H)^H, U^H = conj(transpose(U))
            V (af/af-conjg (af/af-transpose vt false))
            U-H (af/af-conjg (af/af-transpose u false))
            
            sigma-inv (af/af-diag s-inv 0 false)
            A-pinv (af/af-matmul V (af/af-matmul sigma-inv U-H))]
        
        ;; Clean up
        (af/af-release-array u)
        (af/af-release-array s)
        (af/af-release-array vt)
        (af/af-release-array s-inv)
        (af/af-release-array V)
        (af/af-release-array U-H)
        (af/af-release-array sigma-inv)
        
        A-pinv)))

  ;; Solve least-squares problem: minimize ||Ax - b||
  (let [A (af/af-randn [100 50] :f64)  ;; Overdetermined system
        b (af/af-randn [100 1] :f64)
        A-pinv (pseudoinverse A 1e-10)
        x (af/af-matmul A-pinv b)]
    (println \"Solution found via pseudoinverse\")
    (af/af-print-array x))
  ```

  ## Type Requirements

  **Input**:
  - f32, f64: Real matrices
  - c32, c64: Complex matrices
  - Must be 2D (or 1D treated as column vector)

  **Output**:
  - **u**: Same type as input (m×m)
  - **s**: Base type (real) - f32 for f32/c32, f64 for f64/c64
  - **vt**: Same type as input (n×n)

  ## Dimension Handling

  - **0D input**: Returns empty arrays for u, s, vt
  - **1D input**: Treated as column vector (n×1)
  - **2D input**: Standard matrix decomposition
  - **Higher dimensions**: Not supported (assertion error)

  ## Properties of Output

  ### Orthonormality
  For real matrices:
  ```
  U^T × U = I_m (m×m identity)
  V^T × V = I_n (n×n identity)
  ```

  For complex matrices:
  ```
  U^H × U = I_m (conjugate transpose)
  V^H × V = I_n
  ```

  ### Singular Values
  - Always non-negative: s[i] ≥ 0
  - Sorted descending: s[0] ≥ s[1] ≥ ... ≥ s[k-1]
  - Where k = min(m, n)

  ## Performance

  **Computational Cost**:
  - Time: O(min(m²n, mn²))
  - For square n×n: O(n³)
  - Space: O(m² + n² + mn)

  **Typical Timings** (f32 on modern GPU):
  - 100×100: ~1 ms
  - 500×500: ~10 ms
  - 1000×1000: ~50 ms
  - 2000×2000: ~300 ms

  ## Memory Usage

  **Allocations**:
  - U: m×m elements
  - s: min(m,n) elements
  - vt: n×n elements
  - Input preserved (not modified)

  **Example**: 1000×500 f32 matrix
  - U: 1000×1000 = 4 MB
  - s: 500 = 2 KB
  - vt: 500×500 = 1 MB
  - Total: ~5 MB output

  ## Numerical Stability

  - Backward stable algorithm
  - Relative error in singular values: O(ε) where ε = machine epsilon
  - For f32: ~10^-7 relative error
  - For f64: ~10^-16 relative error

  ## Error Conditions

  - **AF_ERR_ARG**: Null pointers or invalid array
  - **AF_ERR_SIZE**: Input dimensions > 2D
  - **AF_ERR_TYPE**: Unsupported type (not f32/f64/c32/c64)
  - **AF_ERR_NOT_SUPPORTED**: LAPACK not available

  ## Implementation Notes

  - Uses LAPACK's divide-and-conquer algorithm (DGESDD family)
  - Highly optimized, industry-standard implementation
  - Thread-safe on GPU (multiple simultaneous calls OK)
  - Not supported in GFOR loops

  ## Comparison: Standard vs In-Place

  | Feature | af-svd | af-svd-inplace |
  |---------|--------|----------------|
  | Input modified? | No | Yes (destroyed) |
  | Memory | Higher | Lower |
  | Constraint | Any m,n | Requires m ≥ n |
  | Use when | Need input later | Input disposable |

  See also:
  - af_svd (ArrayFire C API)
  - af-svd-inplace: Memory-efficient in-place variant
  - af-qr: QR decomposition (orthogonal factorization)
  - af-lu: LU decomposition (Gaussian elimination)
  - af-cholesky: Cholesky decomposition (positive definite)
  - af-inverse: Matrix inversion (uses SVD internally)
  - af-pinverse: Pseudoinverse (explicitly uses SVD)
  - af-rank: Matrix rank (uses SVD)
  - af-norm: Matrix norms (can use singular values)"
  "af_svd" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-svd-inplace
  "Perform in-place Singular Value Decomposition of a matrix.

  Memory-efficient variant of SVD that destroys the input matrix to save memory.
  Input matrix is limited to tall or square matrices (m ≥ n).

  Parameters:
  - u: Output pointer for U matrix (m×m unitary matrix, left singular vectors)
  - s: Output pointer for singular values (n vector, diagonal of Σ)
  - vt: Output pointer for V^H matrix (n×n conjugate transpose of right singular vectors)
  - in: Input/output matrix (m×n) - **DESTROYED** by this operation

  Returns:
  Error code indicating success or failure.

  ## Key Difference from af-svd

  **af-svd**:
  - Input matrix preserved (read-only)
  - Works for any m×n matrix
  - Higher memory usage

  **af-svd-inplace**:
  - Input matrix **destroyed** (overwritten with workspace data)
  - Requires m ≥ n (tall or square matrices only)
  - Lower memory usage (saves m×n array)

  ## When to Use In-Place

  Use `af-svd-inplace` when:
  1. Input matrix no longer needed after SVD
  2. Memory is limited or constrained
  3. Matrix is tall or square (m ≥ n)
  4. Processing many large matrices sequentially

  Use `af-svd` when:
  1. Need to preserve input matrix
  2. Matrix is wide (m < n)
  3. Memory is not a concern
  4. Input used in multiple operations

  ## Memory Savings

  For m×n matrix (m ≥ n):
  - **Saved**: m×n × sizeof(element) bytes
  - Example: 2000×1000 f32 → saves 8 MB

  Space comparison:
  ```
  af-svd:         Input(m×n) + U(m×m) + s(n) + V(n×n) + temp(varies)
  af-svd-inplace: U(m×m) + s(n) + V(n×n) + temp(varies)
  ```

  ## Basic Example

  ```clojure
  (require '[org.soulspace.arrayfire.ffi.core :as af])
  (require '[org.soulspace.arrayfire.ffi.svd :as svd])
  (require '[coffi.mem :as mem])

  ;; Create tall matrix (5×3): rows ≥ cols required
  (let [data [1.0 2.0 3.0
              4.0 5.0 6.0
              7.0 8.0 9.0
              10.0 11.0 12.0
              13.0 14.0 15.0]
        A (af/af-create-array data [5 3] :f32)  ;; 5 rows, 3 cols
        
        ;; IMPORTANT: A will be destroyed!
        ;; Make a copy if you need original later
        A-copy (af/af-copy-array A)
        
        ;; Allocate output pointers
        u-ptr (mem/alloc-instance ::mem/pointer)
        s-ptr (mem/alloc-instance ::mem/pointer)
        vt-ptr (mem/alloc-instance ::mem/pointer)]
    
    ;; Perform in-place SVD (A is destroyed!)
    (svd/af-svd-inplace u-ptr s-ptr vt-ptr A)
    
    ;; Extract results
    (let [u (mem/read-pointer u-ptr ::mem/pointer)   ;; 5×5
          s (mem/read-pointer s-ptr ::mem/pointer)   ;; 3 values
          vt (mem/read-pointer vt-ptr ::mem/pointer)] ;; 3×3
      
      (println \"Singular values:\")
      (af/af-print-array s)
      
      ;; A is now garbage - don't use it!
      ;; Use A-copy if you need original
      
      ;; Clean up
      (af/af-release-array u)
      (af/af-release-array s)
      (af/af-release-array vt)
      (af/af-release-array A-copy)))
  ```

  ## Example: Batch Processing

  ```clojure
  ;; Process multiple matrices efficiently
  (defn batch-svd-inplace [matrices]
    (for [matrix matrices]
      (let [u-ptr (mem/alloc-instance ::mem/pointer)
            s-ptr (mem/alloc-instance ::mem/pointer)
            vt-ptr (mem/alloc-instance ::mem/pointer)]
        
        ;; Matrix will be destroyed - that's OK, we don't need it
        (svd/af-svd-inplace u-ptr s-ptr vt-ptr matrix)
        
        ;; Extract results
        (let [u (mem/read-pointer u-ptr ::mem/pointer)
              s (mem/read-pointer s-ptr ::mem/pointer)
              vt (mem/read-pointer vt-ptr ::mem/pointer)]
          
          ;; Return singular values only (most common use case)
          ;; Release u and vt if not needed
          (af/af-release-array u)
          (af/af-release-array vt)
          
          s))))  ;; Return singular values

  ;; Process 100 large matrices
  (let [matrices (repeatedly 100 #(af/af-randn [2000 1000] :f32))
        singular-values (batch-svd-inplace matrices)]
    (println \"Processed\" (count singular-values) \"matrices\")
    ;; Analyze singular values...
    (doseq [s singular-values]
      (af/af-release-array s)))
  ```

  ## Example: Memory-Constrained Environment

  ```clojure
  ;; Compute condition number with minimal memory
  (defn condition-number-inplace [A]
    ;; IMPORTANT: A will be destroyed!
    (let [u-ptr (mem/alloc-instance ::mem/pointer)
          s-ptr (mem/alloc-instance ::mem/pointer)
          vt-ptr (mem/alloc-instance ::mem/pointer)]
      
      (svd/af-svd-inplace u-ptr s-ptr vt-ptr A)
      
      (let [s (mem/read-pointer s-ptr ::mem/pointer)
            s-host (af/af-copy-to-host s)
            cond (/ (first s-host) (last (filter pos? s-host)))]
        
        ;; Release everything
        (af/af-release-array (mem/read-pointer u-ptr ::mem/pointer))
        (af/af-release-array s)
        (af/af-release-array (mem/read-pointer vt-ptr ::mem/pointer))
        
        cond)))

  ;; Process large matrix with limited memory
  (let [large-matrix (af/af-randn [5000 3000] :f32)  ;; 60 MB
        cond (condition-number-inplace large-matrix)]
    ;; large-matrix is now garbage
    (println \"Condition number:\" cond))
  ```

  ## Example: Pipeline with Disposable Intermediates

  ```clojure
  ;; Data processing pipeline where intermediates can be destroyed
  (defn process-data-pipeline [input-data]
    ;; Step 1: Preprocess (creates matrix)
    (let [preprocessed (preprocess input-data)
          
          ;; Step 2: Extract features with SVD (destroys preprocessed)
          u-ptr (mem/alloc-instance ::mem/pointer)
          s-ptr (mem/alloc-instance ::mem/pointer)
          vt-ptr (mem/alloc-instance ::mem/pointer)]
      
      (svd/af-svd-inplace u-ptr s-ptr vt-ptr preprocessed)
      ;; preprocessed is now garbage
      
      (let [u (mem/read-pointer u-ptr ::mem/pointer)
            s (mem/read-pointer s-ptr ::mem/pointer)
            vt (mem/read-pointer vt-ptr ::mem/pointer)
            
            ;; Step 3: Keep top 50 components
            features (af/af-cols u (range 50))]
        
        ;; Clean up unneeded arrays
        (af/af-release-array u)
        (af/af-release-array s)
        (af/af-release-array vt)
        
        features)))
  ```

  ## Constraint: m ≥ n Required

  ```clojure
  ;; CORRECT: Tall matrix (rows ≥ cols)
  (let [A (af/af-randn [1000 500] :f32)]  ;; 1000 rows, 500 cols - OK!
    (svd/af-svd-inplace u s vt A))

  ;; CORRECT: Square matrix
  (let [A (af/af-randn [500 500] :f32)]   ;; 500 rows, 500 cols - OK!
    (svd/af-svd-inplace u s vt A))

  ;; ERROR: Wide matrix (rows < cols)
  (let [A (af/af-randn [500 1000] :f32)]  ;; 500 rows, 1000 cols - FAIL!
    (svd/af-svd-inplace u s vt A))        ;; Assertion error!

  ;; SOLUTION: Transpose, or use af-svd instead
  (let [A (af/af-randn [500 1000] :f32)
        A-T (af/af-transpose A false)]    ;; Now 1000×500
    (svd/af-svd-inplace u s vt A-T))      ;; OK!
  ```

  ## Input Destruction Behavior

  After calling `af-svd-inplace`:
  ```clojure
  (let [A (af/af-randn [100 50] :f32)]
    (println \"Before SVD:\")
    (af/af-print-array A)  ;; Valid data
    
    (svd/af-svd-inplace u-ptr s-ptr vt-ptr A)
    
    (println \"After SVD:\")
    (af/af-print-array A)  ;; GARBAGE! Random workspace data
    ;; DO NOT USE A ANYMORE!
    
    (af/af-release-array A))  ;; Free the garbage
  ```

  ## Type Support

  Same as `af-svd`:
  - f32, f64: Real matrices
  - c32, c64: Complex matrices

  Output types same as input type rules.

  ## Performance

  **Time Complexity**: Same as `af-svd` - O(min(m²n, mn²))

  **Space Complexity**: Lower than `af-svd`
  - Saves: m×n × sizeof(element) bytes
  - Faster memory allocation (one fewer large array)

  **Real-World Impact**:
  - For small matrices (<100×100): Negligible difference
  - For medium matrices (100-1000): 10-20% memory savings
  - For large matrices (>1000): Significant memory savings, may avoid OOM

  ## Algorithm Details

  Uses LAPACK's divide-and-conquer algorithm, but with input matrix used as workspace:
  - Input overwritten with intermediate computations
  - Reduces peak memory usage during decomposition
  - Same numerical stability as `af-svd`

  ## Error Conditions

  All errors from `af-svd`, plus:
  - **AF_ERR_SIZE**: If m < n (rows < cols)
    * Error message: Dimension assertion failed
    * Solution: Use `af-svd` or transpose input

  ## Best Practices

  1. **Always comment when using in-place**:
     ```clojure
     ;; DESTROYS input matrix!
     (svd/af-svd-inplace u s vt A)
     ```

  2. **Copy if needed later**:
     ```clojure
     (let [A-copy (af/af-copy-array A)]
       (svd/af-svd-inplace u s vt A)
       ;; Use A-copy if you need original
       )
     ```

  3. **Check dimensions first**:
     ```clojure
     (let [[m n] (af/af-get-dims A)]
       (if (>= m n)
         (svd/af-svd-inplace u s vt A)
         (svd/af-svd u s vt A)))  ;; Fall back to standard
     ```

  4. **Use in pipelines**:
     - Where intermediate results are temporary
     - Sequential processing of many matrices
     - Streaming data workflows

  ## When NOT to Use In-Place

  Don't use in-place if:
  - Input matrix needed later
  - Matrix is wide (m < n)
  - Code clarity more important than memory
  - Debugging (harder to inspect inputs after)

  ## Comparison Table

  | Scenario | Use af-svd | Use af-svd-inplace |
  |----------|------------|-------------------|
  | Need input later | ✓ | ✗ |
  | Wide matrix (m < n) | ✓ | ✗ |
  | Memory constrained | ✗ | ✓ |
  | Batch processing | ✗ | ✓ |
  | Disposable input | Either | ✓ (preferred) |
  | Square/tall matrix | Either | ✓ (if input disposable) |

  See also:
  - af_svd_inplace (ArrayFire C API)
  - af-svd: Standard SVD (preserves input)
  - af-lu-inplace: In-place LU decomposition
  - af-qr-inplace: In-place QR decomposition
  - af-cholesky-inplace: In-place Cholesky decomposition"
  "af_svd_inplace" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)
