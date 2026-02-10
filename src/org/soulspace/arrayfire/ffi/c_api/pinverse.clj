(ns org.soulspace.arrayfire.ffi.c-api.pinverse
  "Bindings for the ArrayFire pseudo-inverse (Moore-Penrose inverse) function.
   
   The pseudo-inverse (also called generalized inverse or Moore-Penrose inverse)
   extends the concept of matrix inversion to rectangular and singular matrices.
   It provides a best-fit solution to linear systems that may be overdetermined,
   underdetermined, or inconsistent.
   
   ## What is the Pseudo-Inverse?
   
   The pseudo-inverse A⁺ of a matrix A is a unique matrix that satisfies the
   four Moore-Penrose conditions:
   
   1. **AA⁺A = A** (reconstruction)
   2. **A⁺AA⁺ = A⁺** (weak inverse)
   3. **(AA⁺)ᴴ = AA⁺** (symmetric/Hermitian)
   4. **(A⁺A)ᴴ = A⁺A** (symmetric/Hermitian)
   
   where ᴴ denotes conjugate transpose (transpose for real matrices).
   
   ### Mathematical Properties
   
   **For square invertible matrices:**
   ```
   A⁺ = A⁻¹  (equals the regular inverse)
   ```
   
   **For rectangular matrices:**
   - Left pseudo-inverse (m > n, overdetermined):
     ```
     A⁺ = (AᴴA)⁻¹Aᴴ
     ```
     Minimizes ||Ax - b||² (least squares solution)
   
   - Right pseudo-inverse (m < n, underdetermined):
     ```
     A⁺ = Aᴴ(AAᴴ)⁻¹
     ```
     Finds minimum norm solution ||x||²
   
   **For singular matrices:**
   - Computed via SVD (Singular Value Decomposition)
   - Inverts only non-zero singular values
   
   ### Geometric Interpretation
   
   The pseudo-inverse provides:
   - **Best-fit solution**: Minimizes residual ||Ax - b||
   - **Minimum norm**: Among all best-fit solutions, finds the one with smallest ||x||
   - **Orthogonal projection**: Projects onto the range of A
   
   ## SVD-Based Computation
   
   ArrayFire computes the pseudo-inverse using SVD decomposition:
   
   **Step 1: SVD Decomposition**
   ```
   A = UΣVᴴ
   ```
   where:
   - U: m×m unitary matrix (left singular vectors)
   - Σ: m×n diagonal matrix (singular values σᵢ ≥ 0)
   - Vᴴ: n×n unitary matrix (right singular vectors, conjugate transposed)
   
   **Step 2: Invert Singular Values**
   ```
   Σ⁺[i,i] = { 1/σᵢ  if σᵢ > threshold
             { 0     otherwise
   ```
   
   **Step 3: Reconstruct Pseudo-Inverse**
   ```
   A⁺ = VΣ⁺Uᴴ
   ```
   
   The threshold determines which singular values are considered zero:
   ```
   threshold = tol × max(m, n) × max(σ)
   ```
   where tol is the tolerance parameter.
   
   ### Why SVD?
   
   SVD-based pseudo-inverse is:
   - **Numerically stable**: Works even for ill-conditioned matrices
   - **Handles rank deficiency**: Automatically identifies the effective rank
   - **Robust**: Tolerates small singular values without catastrophic cancellation
   - **General**: Works for any matrix (square, rectangular, singular)
   
   ## Tolerance Parameter
   
   The tolerance (tol) controls which singular values are considered zero:
   
   **Relative Threshold Calculation:**
   ```
   actual_threshold = tol × max(rows, cols) × max(singular_values)
   ```
   
   **Small tolerance (1e-12):**
   - Keeps more singular values
   - Higher effective rank
   - More sensitive to noise
   - Can be numerically unstable
   
   **Medium tolerance (1e-6, default):**
   - Balanced approach
   - Good for most applications
   - Recommended for single precision
   
   **Large tolerance (1e-3):**
   - Discards more small singular values
   - Lower effective rank
   - More robust to noise
   - May lose information
   
   **Selection Guidelines:**
   
   | Data Type | Recommended tol | When to Adjust |
   |-----------|----------------|----------------|
   | float (f32) | 1e-6 | 1e-4 to 1e-8 |
   | double (f64) | 1e-12 | 1e-10 to 1e-14 |
   | Noisy data | Increase | 10× higher |
   | Clean data | Decrease | 10× lower |
   
   ## Performance Characteristics
   
   ### Time Complexity
   
   For matrix size m×n (m ≥ n without loss of generality):
   
   **SVD Computation:**
   - O(mn²) operations
   - Dominates the computation
   
   **Threshold and Invert:**
   - O(min(m,n)) operations
   - Negligible compared to SVD
   
   **Reconstruct:**
   - O(mn × min(m,n)) matrix multiplications
   - Significant but less than SVD
   
   **Total:** O(mn²) for m ≥ n, O(m²n) for m < n
   
   ### GPU Acceleration
   
   ArrayFire's GPU implementation provides significant speedup:
   - **SVD**: 5-20× faster (depends on backend)
   - **Matrix operations**: 10-100× faster
   - **Overall**: 5-15× speedup typical
   
   Batch processing (3D/4D arrays) benefits even more:
   - Multiple independent SVDs parallelized
   - Can achieve 20-50× speedup
   
   ### Typical Timings (GPU)
   
   | Matrix Size | CPU Time | GPU Time | Speedup |
   |-------------|----------|----------|---------|
   | 100×80 | 5 ms | 0.8 ms | 6× |
   | 500×400 | 200 ms | 15 ms | 13× |
   | 1000×800 | 1.5 s | 120 ms | 12× |
   | 2000×1600 | 12 s | 1.2 s | 10× |
   
   Note: Timings for double precision, vary by GPU
   
   ### Memory Usage
   
   For matrix A with dimensions m×n:
   - **Input A**: m×n elements
   - **U matrix**: m×m elements (temporary)
   - **Σ vector**: min(m,n) elements (temporary)
   - **V matrix**: n×n elements (temporary)
   - **Output A⁺**: n×m elements
   - **Intermediate**: Various n×m arrays
   - **Total peak**: ≈ (m² + n² + 5mn) elements
   
   For double precision (8 bytes/element), 1000×800:
   - Peak: ≈ 88 MB
   
   Batch processing multiplies by number of matrices
   
   ## Applications
   
   ### 1. Least Squares Regression
   
   Solve overdetermined system Ax ≈ b:
   ```
   x = A⁺b
   ```
   Minimizes ||Ax - b||² (sum of squared errors)
   
   Applications:
   - Linear regression
   - Curve fitting
   - Data smoothing
   - Parameter estimation
   
   ### 2. Underdetermined Systems
   
   For systems with infinite solutions (Ax = b, m < n):
   ```
   x = A⁺b
   ```
   Finds minimum-norm solution (smallest ||x||)
   
   Applications:
   - Compressed sensing
   - Regularization
   - Optimization with constraints
   
   ### 3. Optimal Control
   
   Compute optimal control input:
   ```
   u = A⁺(x_desired - Bx_current)
   ```
   
   Applications:
   - Robotics path planning
   - Aerospace control
   - Process control
   
   ### 4. Machine Learning
   
   **Ridge Regression:**
   ```
   w = (XᴴX + λI)⁻¹Xᴴy
     ≈ (X + λI/Xᴴ)⁺y  (approximate for regularization)
   ```
   
   **Principal Component Regression:**
   - Use pseudo-inverse after dimensionality reduction
   - Handles multicollinearity
   
   Applications:
   - Feature selection
   - Regularized regression
   - Collaborative filtering
   
   ### 5. Image Processing
   
   **Image Reconstruction:**
   ```
   I = A⁺b
   ```
   where A represents blur/degradation
   
   Applications:
   - Deblurring
   - Super-resolution
   - Computed tomography (CT)
   - MRI reconstruction
   
   ### 6. Signal Processing
   
   **Adaptive Filtering:**
   ```
   h = R⁺p
   ```
   where R is correlation matrix, p is cross-correlation
   
   Applications:
   - Echo cancellation
   - Noise reduction
   - Channel equalization
   
   ### 7. Computer Graphics
   
   **Shape Morphing:**
   ```
   T = target_points × control_points⁺
   ```
   
   Applications:
   - Animation interpolation
   - Mesh deformation
   - Inverse kinematics
   
   ## Type Support
   
   Input matrix types:
   - **f32** (float): Single precision (recommended tol: 1e-6)
   - **f64** (double): Double precision (recommended tol: 1e-12)
   - **c32** (complex float): Single precision complex
   - **c64** (complex double): Double precision complex
   
   Output:
   - Same type as input
   - Dimensions swapped: [m,n] → [n,m]
   
   Constraints:
   - Must be floating-point (f32, f64, c32, c64)
   - Can be rectangular (any m, n)
   - Can be singular (rank deficient)
   - Supports batching (3D, 4D arrays)
   
   ## Batching Support
   
   The pseudo-inverse supports batch processing:
   
   **3D Input [m, n, p]:**
   - Computes p independent pseudo-inverses
   - Output: [n, m, p]
   - Each [:,:,i] slice processed independently
   
   **4D Input [m, n, p, q]:**
   - Computes p×q independent pseudo-inverses
   - Output: [n, m, p, q]
   - Each [:,:,i,j] slice processed independently
   
   Batch processing is highly efficient on GPU as operations are parallelized.
   
   ## Comparison with Regular Inverse
   
   | Feature | Inverse (A⁻¹) | Pseudo-Inverse (A⁺) |
   |---------|---------------|---------------------|
   | Requires | Square, non-singular | Any shape, any rank |
   | Method | LU/Cholesky | SVD |
   | Complexity | O(n³) | O(mn²) |
   | Stability | Can be unstable | Very stable |
   | Singular matrices | Fails | Works |
   | Rectangular | No | Yes |
   | Result | Exact inverse | Best approximation |
   | Use case | Well-conditioned systems | Least squares, ill-conditioned |
   
   ## Numerical Considerations
   
   ### Conditioning
   
   Condition number: κ(A) = σ_max / σ_min
   
   **Well-conditioned (κ < 100):**
   - Both inverse and pseudo-inverse accurate
   - Small tolerance safe (1e-12 for f64)
   
   **Ill-conditioned (κ > 10⁶):**
   - Regular inverse numerically unstable
   - Pseudo-inverse more robust
   - Increase tolerance to 1e-6 or higher
   
   **Near-singular (κ > 10¹⁵):**
   - Effectively rank deficient
   - Pseudo-inverse essential
   - Tolerance critical: balance accuracy vs stability
   
   ### Accuracy
   
   Relative error in pseudo-inverse:
   ```
   ||A⁺ - A⁺_computed|| / ||A⁺|| ≈ κ(A) × ε_machine
   ```
   
   For f32: ε ≈ 10⁻⁷, safe up to κ ≈ 10⁵
   For f64: ε ≈ 10⁻¹⁶, safe up to κ ≈ 10¹⁴
   
   ### Rank Determination
   
   Effective rank depends on tolerance:
   ```
   rank = count(σᵢ > threshold)
   ```
   
   **Full rank:**
   - rank = min(m, n)
   - All singular values significant
   - Similar to regular inverse (for square)
   
   **Rank deficient:**
   - rank < min(m, n)
   - Some singular values below threshold
   - Information lost or redundant
   
   Use af-rank to explicitly compute rank with given tolerance.
   
   ## Design Patterns
   
   ### Pattern 1: Robust Linear Regression
   
   ```clojure
   (defn linear-regression
     \"Fit linear model y = Xw using pseudo-inverse (least squares).\"
     [X y]
     (let [;; Compute pseudo-inverse of design matrix
           X-pinv-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-pinverse X-pinv-ptr X 1e-6 0) ; AF_MAT_NONE = 0
           X-pinv (mem/read-pointer X-pinv-ptr ::mem/pointer)
           
           ;; Compute weights: w = X⁺y
           w-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-matmul w-ptr X-pinv y 0 0) ; No transpose
           w (mem/read-pointer w-ptr ::mem/pointer)
           
           ;; Compute predictions: y_pred = Xw
           y-pred-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-matmul y-pred-ptr X w 0 0)
           y-pred (mem/read-pointer y-pred-ptr ::mem/pointer)
           
           ;; Compute residuals and R²
           residuals (af-sub y y-pred)
           ss-res (compute-sum-squares residuals)
           ss-tot (compute-sum-squares (af-sub y (af-mean y)))
           r-squared (- 1.0 (/ ss-res ss-tot))]
       
       {:weights w
        :predictions y-pred
        :r-squared r-squared
        :residuals residuals}))
   ```
   
   ### Pattern 2: Regularized Regression (Ridge-like)
   
   ```clojure
   (defn ridge-regression-approx
     \"Approximate ridge regression using pseudo-inverse with adjusted tolerance.
      Not exact ridge, but similar effect via tolerance.\"
     [X y lambda]
     (let [;; Higher tolerance acts like regularization
           ;; (discards small singular values)
           tol (max 1e-6 (* lambda 1e-4))
           
           X-pinv-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-pinverse X-pinv-ptr X tol 0)
           X-pinv (mem/read-pointer X-pinv-ptr ::mem/pointer)
           
           w-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-matmul w-ptr X-pinv y 0 0)
           w (mem/read-pointer w-ptr ::mem/pointer)]
       
       {:weights w
        :lambda lambda
        :effective-tol tol}))
   ```
   
   ### Pattern 3: Adaptive Tolerance Selection
   
   ```clojure
   (defn adaptive-pinverse
     \"Compute pseudo-inverse with automatically adjusted tolerance.\"
     [A]
     (let [;; Get matrix dimensions
           dims (get-array-dims A)
           m (first dims)
           n (second dims)
           
           ;; Estimate condition number via SVD
           cond-est (estimate-condition-number A)
           
           ;; Adapt tolerance based on condition number
           tol (cond
                 (< cond-est 100) 1e-12     ; Well-conditioned
                 (< cond-est 1e6) 1e-8      ; Moderate
                 (< cond-est 1e12) 1e-6     ; Ill-conditioned
                 :else 1e-4)                ; Near-singular
           
           A-pinv-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-pinverse A-pinv-ptr A tol 0)
           A-pinv (mem/read-pointer A-pinv-ptr ::mem/pointer)]
       
       {:pinverse A-pinv
        :tolerance tol
        :condition-estimate cond-est}))
   ```
   
   ### Pattern 4: Minimum Norm Solution
   
   ```clojure
   (defn min-norm-solution
     \"Find minimum norm solution to underdetermined system Ax = b.\"
     [A b]
     ;; For underdetermined (rows < cols), A⁺b gives minimum ||x||
     (let [dims (get-array-dims A)
           m (first dims)
           n (second dims)]
       
       (when (>= m n)
         (throw (ex-info \"System is not underdetermined\" {:m m :n n})))
       
       (let [A-pinv-ptr (mem/alloc-pointer ::mem/pointer)
             _ (af-pinverse A-pinv-ptr A 1e-12 0)
             A-pinv (mem/read-pointer A-pinv-ptr ::mem/pointer)
             
             x-ptr (mem/alloc-pointer ::mem/pointer)
             _ (af-matmul x-ptr A-pinv b 0 0)
             x (mem/read-pointer x-ptr ::mem/pointer)
             
             ;; Verify: Ax should equal b
             Ax-ptr (mem/alloc-pointer ::mem/pointer)
             _ (af-matmul Ax-ptr A x 0 0)
             Ax (mem/read-pointer Ax-ptr ::mem/pointer)
             
             error (af-norm (af-sub b Ax) 2 1.0 1.0)]
         
         {:solution x
          :norm (af-norm x 2 1.0 1.0)
          :residual error
          :is-minimum-norm true})))
   ```
   
   ### Pattern 5: Batch Pseudo-Inverse
   
   ```clojure
   (defn batch-pinverse
     \"Compute pseudo-inverse for batch of matrices.\"
     [matrices-3d tol]
     ;; Input: [m, n, batch_size]
     ;; Output: [n, m, batch_size]
     (let [dims (get-array-dims matrices-3d)
           m (first dims)
           n (second dims)
           batch-size (nth dims 2)
           
           ;; Single call handles all matrices
           pinv-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-pinverse pinv-ptr matrices-3d tol 0)
           pinv (mem/read-pointer pinv-ptr ::mem/pointer)]
       
       (println \"Computed\" batch-size \"pseudo-inverses in batch\")
       pinv))
   ```
   
   ### Pattern 6: Iterative Refinement
   
   ```clojure
   (defn refined-pinverse
     \"Compute pseudo-inverse with iterative refinement for accuracy.\"
     [A max-iterations]
     (let [A-pinv-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-pinverse A-pinv-ptr A 1e-6 0)
           initial-pinv (mem/read-pointer A-pinv-ptr ::mem/pointer)]
       
       (loop [pinv initial-pinv
              iter 0]
         (if (>= iter max-iterations)
           pinv
           (let [;; Compute residual: I - A⁺A
                 product (af-matmul pinv A 0 0)
                 identity (af-identity (get-array-dims product))
                 residual (af-sub identity product)
                 
                 ;; Check convergence
                 res-norm (af-norm residual 2 1.0 1.0)]
             
             (if (< res-norm 1e-10)
               (do
                 (println \"Converged at iteration\" iter)
                 pinv)
               ;; Refine: A⁺ = A⁺ + (I - A⁺A)A⁺
               (let [correction (af-matmul residual pinv 0 0)
                     refined (af-add pinv correction)]
                 (recur refined (inc iter)))))))))
   ```
   
   ## When to Use Pseudo-Inverse
   
   **Good fit:**
   - Overdetermined systems (least squares)
   - Underdetermined systems (minimum norm)
   - Singular or near-singular matrices
   - Rectangular matrices
   - Ill-conditioned problems
   - When robustness needed
   - Batch processing
   
   **Not ideal:**
   - Well-conditioned square matrices (use af-inverse, faster)
   - Real-time applications requiring speed (if well-conditioned)
   - Very large matrices (consider iterative methods)
   - Need exact inverse (not just best approximation)
   
   ## Common Issues and Solutions
   
   ### Issue 1: Numerical Instability
   
   **Symptoms:**
   - Large errors in A⁺AA⁺ = A⁺
   - NaN or Inf values
   - Unexpected results
   
   **Causes:**
   - Matrix very ill-conditioned
   - Tolerance too small
   - Accumulation of rounding errors
   
   **Solutions:**
   - Increase tolerance (try 1e-4)
   - Use double precision (f64 instead of f32)
   - Check condition number first
   - Preconditioning (normalize columns)
   
   ### Issue 2: Wrong Tolerance
   
   **Symptoms:**
   - Pseudo-inverse seems wrong
   - Solution not minimizing residual
   - Rank lower than expected
   
   **Causes:**
   - Default tolerance inappropriate
   - Data scale not considered
   - Mixing data types
   
   **Solutions:**
   - Adjust based on precision (1e-6 for f32, 1e-12 for f64)
   - Scale data to similar magnitudes
   - Compute condition number to guide choice
   - Try multiple tolerances and compare
   
   ### Issue 3: Slow Performance
   
   **Symptoms:**
   - Takes longer than expected
   - GPU not faster than CPU
   
   **Causes:**
   - Small matrices (overhead dominates)
   - Memory transfer bottleneck
   - Not batching when possible
   
   **Solutions:**
   - For small matrices (< 100×100), CPU may be faster
   - Batch multiple pseudo-inverses (use 3D/4D arrays)
   - Keep data on GPU between operations
   - Use single precision if acceptable (2× faster)
   
   ### Issue 4: Memory Exhaustion
   
   **Symptoms:**
   - Out of memory errors
   - System slowdown
   
   **Causes:**
   - Very large matrices
   - Large batch size
   - Temporary arrays not released
   
   **Solutions:**
   - Process in smaller batches
   - Use single precision (halves memory)
   - Release intermediate arrays explicitly
   - Consider iterative solvers for huge matrices
   
   ### Issue 5: Loss of Rank Information
   
   **Symptoms:**
   - Pseudo-inverse seems to discard information
   - Rank lower than matrix dimensions
   
   **Causes:**
   - Tolerance too high
   - Matrix truly rank deficient
   - Numerical near-singularity
   
   **Solutions:**
   - Decrease tolerance carefully
   - Use af-rank to check effective rank
   - Check singular values explicitly (SVD)
   - May be inherent in data (not an error)
   
   ## Best Practices
   
   1. **Choose appropriate tolerance:**
      - f32: 1e-6 (default)
      - f64: 1e-12
      - Adjust based on condition number
   
   2. **Check condition number:**
      - Estimate κ(A) before computing pseudo-inverse
      - Warns of potential numerical issues
      - Guides tolerance selection
   
   3. **Verify Moore-Penrose conditions:**
      - Test AA⁺A = A for validation
      - Especially for critical applications
      - Can detect numerical issues
   
   4. **Use batching for multiple matrices:**
      - 3D/4D arrays much faster than loops
      - Better GPU utilization
      - Reduced overhead
   
   5. **Scale data appropriately:**
      - Columns with similar magnitudes
      - Improves conditioning
      - More stable numerics
   
   6. **Prefer double precision for ill-conditioned:**
      - f64 when κ(A) > 10⁶
      - Worth the 2× memory and ~1.5× time cost
      - Much more accurate
   
   7. **Consider alternatives for speed:**
      - QR decomposition for least squares
      - Cholesky for well-conditioned normal equations
      - Iterative methods for huge matrices
   
   8. **Handle edge cases:**
      - Zero matrices: pseudo-inverse is zero
      - Identity matrix: pseudo-inverse is identity
      - Single value: reciprocal (if non-zero)
   
   9. **Memory management:**
      - Release temporary arrays
      - Reuse arrays when possible
      - Monitor memory usage in batch mode
   
   10. **Validate results:**
       - Check residual ||Ax - b||
       - Verify solution makes sense
       - Test on known problems first
   
   ## Mathematical Background
   
   ### Moore-Penrose Conditions (Detailed)
   
   For matrix A and its pseudo-inverse A⁺, these always hold:
   
   **Condition 1: AA⁺A = A**
   - Any vector in range(A) is preserved
   - Reconstruction property
   - Verified: ||AA⁺A - A|| / ||A|| should be small
   
   **Condition 2: A⁺AA⁺ = A⁺**
   - Pseudo-inverse is self-consistent
   - Weak inverse property
   - Verified: ||A⁺AA⁺ - A⁺|| / ||A⁺|| should be small
   
   **Condition 3: (AA⁺)ᴴ = AA⁺**
   - AA⁺ is Hermitian (or symmetric for real)
   - Orthogonal projection onto range(A)
   - Verified: ||AA⁺ - (AA⁺)ᴴ|| should be small
   
   **Condition 4: (A⁺A)ᴴ = A⁺A**
   - A⁺A is Hermitian (or symmetric for real)
   - Orthogonal projection onto range(Aᴴ)
   - Verified: ||A⁺A - (A⁺A)ᴴ|| should be small
   
   ### Singular Value Decomposition
   
   **Full SVD:**
   ```
   A = UΣVᴴ
   
   U: m×m unitary (UᴴU = I)
   Σ: m×n diagonal (σ₁ ≥ σ₂ ≥ ... ≥ σᵣ > 0)
   V: n×n unitary (VᴴV = I)
   ```
   
   **Reduced SVD (rank r):**
   ```
   A = U_r Σ_r V_r^ᴴ
   
   U_r: m×r
   Σ_r: r×r diagonal
   V_r: n×r
   ```
   
   **Pseudo-Inverse Construction:**
   ```
   Σ⁺[i,i] = 1/σᵢ  for i ≤ r
   A⁺ = V Σ⁺ Uᴴ
      = Σᵢ (vᵢ/σᵢ) uᵢᴴ  (outer product sum)
   ```
   
   ### Relationship to Other Inverses
   
   **Regular Inverse (A⁻¹):**
   - Exists only for square, non-singular
   - A⁺ = A⁻¹ when both exist
   - Faster to compute via LU or Cholesky
   
   **Left Inverse (A_L⁻¹):**
   - For m > n (tall matrix)
   - A_L⁻¹A = I_n
   - A⁺ = A_L⁻¹ = (AᴴA)⁻¹Aᴴ when full column rank
   
   **Right Inverse (A_R⁻¹):**
   - For m < n (wide matrix)
   - AA_R⁻¹ = I_m
   - A⁺ = A_R⁻¹ = Aᴴ(AAᴴ)⁻¹ when full row rank
   
   **Generalized Inverse:**
   - Moore-Penrose is one type of generalized inverse
   - Unique one satisfying all four conditions
   - Others exist but less commonly used
   
   ## References
   
   **Original Papers:**
   - Moore, E. H. (1920). \"On the reciprocal of the general algebraic matrix.\"
   - Penrose, R. (1955). \"A generalized inverse for matrices.\"
   
   **Algorithms:**
   - Golub, G. H., & Van Loan, C. F. (2013). \"Matrix Computations\" (4th ed.).
     Chapter on SVD and pseudo-inverse.
   
   **Applications:**
   - Lawson, C. L., & Hanson, R. J. (1995). \"Solving Least Squares Problems.\"
   
   **Numerical Analysis:**
   - Higham, N. J. (2002). \"Accuracy and Stability of Numerical Algorithms.\"
   
   See also:
   - af-inverse: Regular matrix inverse (square, non-singular only)
   - af-solve: Solve linear systems (more efficient when applicable)
   - af-svd: Singular Value Decomposition (underlying algorithm)
   - af-rank: Compute matrix rank with tolerance
   - af-norm: Compute matrix norms for error checking"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.loader]))

;; af_err af_pinverse(af_array *out, const af_array in, const double tol, const af_mat_prop options)
(defcfn af-pinverse
  "Compute the Moore-Penrose pseudo-inverse of a matrix.
   
   The pseudo-inverse (also called generalized inverse) extends matrix inversion
   to rectangular and singular matrices using Singular Value Decomposition (SVD).
   It provides the best least-squares solution for overdetermined systems and the
   minimum-norm solution for underdetermined systems.
   
   Parameters:
   - out: out pointer for pseudo-inverse matrix
     * Dimensions: [n, m] for input [m, n]
     * Type: Same as input
   - in: input matrix
     * Dimensions: [m, n, p, q] (supports batching)
     * Type: f32, f64, c32, c64 (floating-point only)
     * Can be rectangular (any m, n)
     * Can be singular (rank deficient)
   - tol: tolerance for singular value threshold
     * Range: tol ≥ 0
     * Recommended: 1e-6 for f32, 1e-12 for f64
     * Actual threshold: tol × max(m,n) × max(singular_values)
     * Lower = keep more singular values (less stable)
     * Higher = discard more small values (more robust)
   - options: matrix property flags
     * Must be AF_MAT_NONE (0) currently
     * Other options not yet supported
   
   Algorithm (SVD-based):
   1. Compute SVD: A = UΣVᴴ
   2. Invert singular values: σᵢ⁺ = 1/σᵢ if σᵢ > threshold, else 0
   3. Reconstruct: A⁺ = VΣ⁺Uᴴ
   
   Threshold:
   ```
   threshold = tol × max(m, n) × max(σ₁, σ₂, ..., σᵣ)
   ```
   
   Output Properties (Moore-Penrose conditions):
   - AA⁺A = A (reconstruction)
   - A⁺AA⁺ = A⁺ (weak inverse)
   - (AA⁺)ᴴ = AA⁺ (Hermitian projection)
   - (A⁺A)ᴴ = A⁺A (Hermitian projection)
   
   Performance:
   - Time: O(mn × min(m,n)) dominated by SVD
   - GPU speedup: 5-15× typical
   - Batch processing: Highly parallelized, 20-50× possible
   
   Example 1: Basic pseudo-inverse (overdetermined least squares)
   ```clojure
   ;; Solve Ax ≈ b in least squares sense
   (let [A (create-array [[1.0 2.0]
                          [3.0 4.0]
                          [5.0 6.0]] [3 2]) ; 3×2, overdetermined
         b (create-array [7.0 8.0 9.0] [3 1])
         
         ;; Compute pseudo-inverse
         A-pinv-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-pinverse A-pinv-ptr A 1e-6 0)
         A-pinv (mem/read-pointer A-pinv-ptr ::mem/pointer)
         
         ;; Solve: x = A⁺b (minimizes ||Ax - b||²)
         x-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-matmul x-ptr A-pinv b 0 0)
         x (mem/read-pointer x-ptr ::mem/pointer)
         
         ;; Check residual
         Ax-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-matmul Ax-ptr A x 0 0)
         Ax (mem/read-pointer Ax-ptr ::mem/pointer)
         residual (af-sub b Ax)
         error (af-norm residual 2 1.0 1.0)]
     
     (println \"Solution x:\" x)
     (println \"Residual error:\" error)
     {:solution x :error error}))
   ```
   
   Example 2: Underdetermined system (minimum norm solution)
   ```clojure
   ;; System has infinite solutions, find smallest ||x||
   (let [A (create-array [[1.0 2.0 3.0]
                          [4.0 5.0 6.0]] [2 3]) ; 2×3, underdetermined
         b (create-array [7.0 8.0] [2 1])
         
         A-pinv-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-pinverse A-pinv-ptr A 1e-12 0)
         A-pinv (mem/read-pointer A-pinv-ptr ::mem/pointer)
         
         ;; x = A⁺b has minimum ||x|| among all solutions
         x-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-matmul x-ptr A-pinv b 0 0)
         x (mem/read-pointer x-ptr ::mem/pointer)
         
         ;; Verify solution
         Ax-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-matmul Ax-ptr A x 0 0)
         Ax (mem/read-pointer Ax-ptr ::mem/pointer)
         
         solution-norm (af-norm x 2 1.0 1.0)
         verification-error (af-norm (af-sub Ax b) 2 1.0 1.0)]
     
     (println \"Minimum norm solution, ||x|| =\" solution-norm)
     (println \"Verification error:\" verification-error)
     {:solution x :norm solution-norm}))
   ```
   
   Example 3: Linear regression (least squares fit)
   ```clojure
   ;; Fit y = w0 + w1*x1 + w2*x2 to data
   (let [;; Design matrix with bias column
         X (create-array [[1.0 2.0 3.0]  ; bias
                          [1.5 2.5 3.5]  ; feature 1
                          [2.0 3.0 4.0]] ; feature 2
                        [3 100]) ; 100 samples
         y (create-array [...] [100 1])  ; target values
         
         ;; Compute pseudo-inverse
         X-pinv-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-pinverse X-pinv-ptr (af-transpose X false) ; Need [100,3]
                       1e-6 0)
         X-pinv (mem/read-pointer X-pinv-ptr ::mem/pointer)
         
         ;; Fit: w = X⁺y
         w-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-matmul w-ptr X-pinv y 0 0)
         w (mem/read-pointer w-ptr ::mem/pointer)
         
         ;; Predictions and R²
         y-pred (af-matmul (af-transpose X false) w 0 0)
         residuals (af-sub y y-pred)
         ss-res (compute-sum-squares residuals)
         ss-tot (compute-sum-squares (af-sub y (af-mean y)))
         r-squared (- 1.0 (/ ss-res ss-tot))]
     
     (println \"Weights:\" w)
     (println \"R²:\" r-squared)
     {:weights w :r-squared r-squared}))
   ```
   
   Example 4: Singular matrix (rank deficient)
   ```clojure
   ;; Matrix with dependent columns
   (let [A (create-array [[1.0 2.0 3.0]
                          [2.0 4.0 6.0]  ; 2× first row
                          [3.0 6.0 9.0]] ; 3× first row
                        [3 3])           ; Rank = 1
         
         ;; Regular inverse would fail, pseudo-inverse works
         A-pinv-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-pinverse A-pinv-ptr A 1e-6 0)
         A-pinv (mem/read-pointer A-pinv-ptr ::mem/pointer)
         
         ;; Check Moore-Penrose condition: AA⁺A = A
         temp1 (af-matmul A A-pinv 0 0)
         result (af-matmul temp1 A 0 0)
         error (af-norm (af-sub A result) 2 1.0 1.0)]
     
     (println \"Moore-Penrose error:\" error)
     (when (< error 1e-5)
       (println \"✓ Pseudo-inverse satisfies AA⁺A = A\"))
     {:pinverse A-pinv :mp-error error}))
   ```
   
   Example 5: Batch processing (multiple matrices)
   ```clojure
   ;; Process 10 matrices at once (much faster than loop)
   (let [batch-size 10
         matrices (create-array [...] [50 40 batch-size]) ; 10 × [50,40]
         
         ;; Single call processes all
         pinv-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-pinverse pinv-ptr matrices 1e-6 0)
         pinv-batch (mem/read-pointer pinv-ptr ::mem/pointer)
         ;; Result: [40, 50, 10]
         
         ;; Extract individual pseudo-inverses
         pinv-list (for [i (range batch-size)]
                     (af-index pinv-batch [:all :all i]))]
     
     (println \"Processed\" batch-size \"pseudo-inverses in batch\")
     pinv-batch))
   ```
   
   Example 6: Adaptive tolerance based on condition number
   ```clojure
   (defn smart-pinverse
     \"Compute pseudo-inverse with automatically chosen tolerance.\"
     [A]
     (let [;; Estimate condition number
           singular-values (compute-singular-values A)
           max-sv (af-max singular-values)
           min-sv (af-min singular-values)
           cond-est (/ max-sv min-sv)
           
           ;; Choose tolerance based on conditioning
           tol (cond
                 (< cond-est 100) 1e-12      ; Well-conditioned
                 (< cond-est 1e6) 1e-8       ; Moderate
                 (< cond-est 1e12) 1e-6      ; Ill-conditioned
                 :else 1e-4)                 ; Very ill-conditioned
           
           A-pinv-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-pinverse A-pinv-ptr A tol 0)
           A-pinv (mem/read-pointer A-pinv-ptr ::mem/pointer)]
       
       (println \"Condition estimate:\" cond-est \", using tol =\" tol)
       {:pinverse A-pinv :tolerance tol :condition cond-est}))
   ```
   
   Example 7: Image deblurring (inverse problem)
   ```clojure
   ;; Solve for sharp image from blurred observation
   (let [blurred-img (load-image \"blurred.png\")
         blur-kernel (create-gaussian-kernel 5 2.0)
         
         ;; Create blur matrix (convolution as matrix multiply)
         H (create-blur-matrix blur-kernel [640 480])
         
         ;; Pseudo-inverse deblurring: sharp ≈ H⁺ * blurred
         H-pinv-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-pinverse H-pinv-ptr H 1e-4 0) ; Higher tol for stability
         H-pinv (mem/read-pointer H-pinv-ptr ::mem/pointer)
         
         blurred-vec (reshape-to-vector blurred-img)
         sharp-vec-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-matmul sharp-vec-ptr H-pinv blurred-vec 0 0)
         sharp-vec (mem/read-pointer sharp-vec-ptr ::mem/pointer)
         
         sharp-img (reshape-from-vector sharp-vec [640 480])]
     
     (save-image \"deblurred.png\" sharp-img)
     {:deblurred sharp-img}))
   ```
   
   Common Patterns:
   
   1. **Verification:**
      ```clojure
      ;; Check Moore-Penrose: AA⁺A = A
      (defn verify-pinverse [A A-pinv]
        (let [product1 (af-matmul A A-pinv 0 0)
              product2 (af-matmul product1 A 0 0)
              diff (af-sub A product2)
              error (af-norm diff 2 1.0 1.0)
              relative-error (/ error (af-norm A 2 1.0 1.0))]
          (< relative-error 1e-6)))
      ```
   
   2. **Condition-aware computation:**
      ```clojure
      (defn safe-pinverse [A]
        (let [cond (estimate-condition-number A)]
          (when (> cond 1e12)
            (println \"Warning: Matrix is nearly singular, κ =\" cond))
          (let [tol (max 1e-12 (* (/ 1.0 cond) 1e-6))]
            (af-pinverse-ptr A tol 0))))
      ```
   
   3. **Regularized solution:**
      ```clojure
      ;; Approximate Tikhonov regularization via tolerance
      (defn regularized-pinverse [A lambda]
        (let [tol (sqrt lambda)] ; Rough approximation
          (af-pinverse-ptr A tol 0)))
      ```
   
   When to adjust tolerance:
   
   - **Decrease (more singular values kept):**
     * Clean, high-quality data
     * Need maximum accuracy
     * Well-conditioned matrices
   
   - **Increase (more singular values dropped):**
     * Noisy data
     * Ill-conditioned matrices
     * Prefer stability over accuracy
     * Getting numerical instabilities
   
   Type Support:
   - Input: f32, f64, c32, c64 (floating-point only)
   - Output: Same type as input
   - Dimensions: Any m×n (including rectangular)
   - Batching: 3D [m,n,p] and 4D [m,n,p,q] supported
   
   Gotchas:
   - Tolerance is relative, not absolute threshold
   - options must be AF_MAT_NONE (0), others unsupported
   - For square non-singular, af-inverse is faster
   - Large tolerance can reduce effective rank significantly
   - Negative tolerance causes error
   - Integer types not supported (convert to float first)
   - Very large matrices may exhaust memory (SVD uses O(mn²))
   - Result dimensions swapped: [m,n] → [n,m]
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-inverse: Regular matrix inverse (faster for well-conditioned square)
   - af-solve: Solve linear systems (more efficient when applicable)
   - af-svd: Singular Value Decomposition (underlying algorithm)
   - af-rank: Compute matrix rank with tolerance
   - af-norm: Compute norms for error checking
   - af-matmul: Matrix multiplication for verification"
  "af_pinverse" [::mem/pointer ::mem/pointer ::mem/double ::mem/int] ::mem/int)
