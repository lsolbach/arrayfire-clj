(ns org.soulspace.arrayfire.ffi.c-api.norm
  "Bindings for the ArrayFire matrix and vector norm functions.
   
   Matrix and vector norms are fundamental measures in linear algebra that
   quantify the \"size\" or \"magnitude\" of matrices and vectors. They are
   essential for numerical analysis, optimization, machine learning, and
   scientific computing.
   
   ## What are Norms?
   
   A norm is a function that assigns a non-negative length or size to vectors
   or matrices. Norms satisfy specific mathematical properties:
   
   1. **Non-negativity**: ‖x‖ ≥ 0, and ‖x‖ = 0 if and only if x = 0
   2. **Scalar multiplication**: ‖αx‖ = |α| ‖x‖
   3. **Triangle inequality**: ‖x + y‖ ≤ ‖x‖ + ‖y‖
   
   ## Vector Norms
   
   Vector norms measure the \"length\" or \"magnitude\" of vectors.
   
   ### 1. L1 Norm (Manhattan / Taxicab Norm)
   
   **Formula**:
   ‖x‖₁ = Σᵢ |xᵢ|
   
   **Properties**:
   - Sum of absolute values of components
   - Also called Manhattan distance or taxicab norm
   - Robust to outliers (less sensitive than L2)
   - Promotes sparsity in optimization
   
   **Geometric interpretation**:
   - Distance traveled along coordinate axes
   - Like walking city blocks in Manhattan
   
   **Example**:
   ```
   x = [3, -4, 2]
   ‖x‖₁ = |3| + |-4| + |2| = 3 + 4 + 2 = 9
   ```
   
   **Applications**:
   - LASSO regression (L1 regularization)
   - Sparse signal recovery
   - Feature selection
   - Robust statistics
   
   ### 2. L2 Norm (Euclidean Norm)
   
   **Formula**:
   ‖x‖₂ = √(Σᵢ xᵢ²) = √(x₁² + x₂² + ... + xₙ²)
   
   **Properties**:
   - Standard Euclidean distance
   - Most commonly used norm
   - Smooth and differentiable everywhere except at origin
   - Corresponds to physical distance in space
   
   **Geometric interpretation**:
   - Straight-line distance (\"as the crow flies\")
   - Length of vector in Euclidean space
   
   **Example**:
   ```
   x = [3, -4, 0]
   ‖x‖₂ = √(3² + (-4)² + 0²) = √(9 + 16 + 0) = √25 = 5
   ```
   
   **Applications**:
   - Ridge regression (L2 regularization)
   - Principal Component Analysis (PCA)
   - Least squares optimization
   - Distance metrics
   - Default norm for most applications
   
   ### 3. L∞ Norm (Maximum / Infinity / Chebyshev Norm)
   
   **Formula**:
   ‖x‖∞ = maxᵢ |xᵢ|
   
   **Properties**:
   - Maximum absolute component
   - Also called max norm or Chebyshev norm
   - Corresponds to L∞ space limit
   - Computationally very efficient
   
   **Geometric interpretation**:
   - Largest coordinate value
   - Distance in a grid where you can move diagonally
   
   **Example**:
   ```
   x = [3, -7, 2]
   ‖x‖∞ = max(|3|, |-7|, |2|) = max(3, 7, 2) = 7
   ```
   
   **Applications**:
   - Error analysis (worst-case error)
   - Game theory
   - Control systems
   - Robustness analysis
   
   ### 4. Lp Norm (General p-Norm)
   
   **Formula**:
   ‖x‖ₚ = (Σᵢ |xᵢ|ᵖ)^(1/p)
   
   **Properties**:
   - Generalizes L1, L2, and L∞ norms
   - p = 1: L1 norm
   - p = 2: L2 norm
   - p → ∞: L∞ norm
   - 0 < p < 1: Not a true norm (violates triangle inequality)
   - p ≥ 1: Valid norm
   
   **Example** (p = 3):
   ```
   x = [2, -3, 1]
   ‖x‖₃ = (|2|³ + |-3|³ + |1|³)^(1/3)
       = (8 + 27 + 1)^(1/3)
       = 36^(1/3) ≈ 3.30
   ```
   
   **Applications**:
   - p = 0.5: Sparse optimization (not true norm)
   - p = 1.5: Compromise between L1 and L2
   - p = 3: Robust regression
   - Flexible regularization in ML
   
   ## Matrix Norms
   
   Matrix norms measure the \"size\" of matrices and are induced by vector norms.
   
   ### 1. Matrix 1-Norm (Column Sum Norm)
   
   **Formula**:
   ‖A‖₁ = maxⱼ Σᵢ |aᵢⱼ|
   
   **Properties**:
   - Maximum absolute column sum
   - Induced by vector L1 norm
   - ‖A‖₁ = max over all columns of (sum of absolute values in column)
   
   **Algorithm**:
   1. For each column j: compute cⱼ = Σᵢ |aᵢⱼ|
   2. Return max(c₁, c₂, ..., cₙ)
   
   **Example**:
   ```
   A = [[1, -2,  3]
        [4,  5, -6]
        [7, -8,  9]]
   
   Column sums:
   - Column 0: |1| + |4| + |7| = 1 + 4 + 7 = 12
   - Column 1: |-2| + |5| + |-8| = 2 + 5 + 8 = 15
   - Column 2: |3| + |-6| + |9| = 3 + 6 + 9 = 18
   
   ‖A‖₁ = max(12, 15, 18) = 18
   ```
   
   **Applications**:
   - Condition number estimation
   - Error propagation analysis
   - Numerical stability
   
   ### 2. Matrix ∞-Norm (Row Sum Norm)
   
   **Formula**:
   ‖A‖∞ = maxᵢ Σⱼ |aᵢⱼ|
   
   **Properties**:
   - Maximum absolute row sum
   - Induced by vector L∞ norm
   - ‖A‖∞ = max over all rows of (sum of absolute values in row)
   
   **Algorithm**:
   1. For each row i: compute rᵢ = Σⱼ |aᵢⱼ|
   2. Return max(r₁, r₂, ..., rₘ)
   
   **Example**:
   ```
   A = [[1, -2,  3]
        [4,  5, -6]
        [7, -8,  9]]
   
   Row sums:
   - Row 0: |1| + |-2| + |3| = 1 + 2 + 3 = 6
   - Row 1: |4| + |5| + |-6| = 4 + 5 + 6 = 15
   - Row 2: |7| + |-8| + |9| = 7 + 8 + 9 = 24
   
   ‖A‖∞ = max(6, 15, 24) = 24
   ```
   
   **Applications**:
   - Iterative solver convergence
   - Numerical stability analysis
   - Error bounds
   
   ### 3. Matrix 2-Norm (Spectral Norm)
   
   **Formula**:
   ‖A‖₂ = σₘₐₓ(A) = √(λₘₐₓ(AᵀA))
   
   Where:
   - σₘₐₓ(A) is the largest singular value of A
   - λₘₐₓ(AᵀA) is the largest eigenvalue of AᵀA
   
   **Properties**:
   - Maximum singular value of matrix
   - Induced by vector L2 norm
   - Most expensive to compute (requires SVD)
   - Submultiplicative: ‖AB‖₂ ≤ ‖A‖₂‖B‖₂
   
   **Geometric interpretation**:
   - Maximum stretching factor of matrix
   - ‖A‖₂ = max{‖Ax‖₂ : ‖x‖₂ = 1}
   
   **Note**: Currently NOT SUPPORTED in ArrayFire
   - Requires singular value decomposition (SVD)
   - Use af_svd separately if needed
   
   **Applications**:
   - Condition number calculation
   - Optimal error bounds
   - Control theory
   - Principal angles between subspaces
   
   ### 4. Matrix Lpq-Norm
   
   **Formula**:
   ‖A‖ₚ,q = (Σⱼ (Σᵢ |aᵢⱼ|ᵖ)^(q/p))^(1/q)
   
   **Algorithm**:
   1. For each column j:
      - If p = 1: compute cⱼ = Σᵢ |aᵢⱼ|
      - If p ≠ 1: compute cⱼ = (Σᵢ |aᵢⱼ|ᵖ)^(1/p)
   2. If q = 1: return Σⱼ cⱼ
   3. If q ≠ 1: return (Σⱼ cⱼᵍ)^(1/q)
   
   **Properties**:
   - Generalization of matrix norms
   - Two-parameter family of norms
   - p controls within-column aggregation
   - q controls between-column aggregation
   
   **Special cases**:
   - (p=1, q=1): Sum of all absolute values
   - (p=2, q=2): Frobenius norm (‖A‖F = √(Σᵢⱼ |aᵢⱼ|²))
   - (p=∞, q=1): Matrix 1-norm
   - (p=1, q=∞): Matrix ∞-norm
   
   **Example** (p=2, q=2 - Frobenius norm):
   ```
   A = [[1, 2]
        [3, 4]]
   
   ‖A‖₂,₂ = √(1² + 2² + 3² + 4²)
         = √(1 + 4 + 9 + 16)
         = √30 ≈ 5.48
   ```
   
   **Applications**:
   - Frobenius norm (most common): matrix approximation, SVD truncation
   - Mixed-norm regularization
   - Image processing
   - Signal processing
   
   ## Frobenius Norm (Important Special Case)
   
   **Formula**:
   ‖A‖F = √(Σᵢⱼ |aᵢⱼ|²) = √(trace(AᵀA))
   
   **Properties**:
   - Lpq-norm with p=2, q=2
   - Equivalent to L2 norm of vectorized matrix
   - Computationally efficient (no eigenvalues needed)
   - Submultiplicative: ‖AB‖F ≤ ‖A‖F‖B‖F
   - Invariant under orthogonal transformations
   
   **Relation to singular values**:
   ‖A‖F = √(σ₁² + σ₂² + ... + σᵣ²)
   
   Where σᵢ are singular values of A.
   
   **Applications**:
   - Most common matrix norm in practice
   - Low-rank matrix approximation
   - Matrix completion
   - Machine learning regularization
   - Image reconstruction
   
   ## Norm Types (af_norm_type Enum)
   
   ArrayFire provides the following norm types:
   
   | Constant             | Value | Description                  | Formula                      |
   |----------------------|-------|------------------------------|------------------------------|
   | AF_NORM_VECTOR_1     | 0     | Vector L1 norm               | Σ\\|xᵢ\\|                      |
   | AF_NORM_VECTOR_INF   | 1     | Vector L∞ norm               | max\\|xᵢ\\|                    |
   | AF_NORM_VECTOR_2     | 2     | Vector L2 norm (Euclidean)   | √(Σxᵢ²)                      |
   | AF_NORM_VECTOR_P     | 3     | Vector Lp norm               | (Σ\\|xᵢ\\|ᵖ)^(1/p)              |
   | AF_NORM_MATRIX_1     | 4     | Matrix 1-norm (column sum)   | maxⱼ Σᵢ\\|aᵢⱼ\\|                |
   | AF_NORM_MATRIX_INF   | 5     | Matrix ∞-norm (row sum)      | maxᵢ Σⱼ\\|aᵢⱼ\\|                |
   | AF_NORM_MATRIX_2     | 6     | Matrix 2-norm (spectral)     | σₘₐₓ(A) - NOT SUPPORTED      |
   | AF_NORM_MATRIX_L_PQ  | 7     | Matrix Lpq-norm              | (Σⱼ(Σᵢ\\|aᵢⱼ\\|ᵖ)^(q/p))^(1/q)  |
   | AF_NORM_EUCLID       | 2     | Alias for AF_NORM_VECTOR_2   | Default norm                 |
   
   ## Performance Characteristics
   
   **Computational Complexity**:
   
   For n-element vector:
   - L1 norm: O(n) - sum absolute values
   - L2 norm: O(n) - sum squares + sqrt
   - L∞ norm: O(n) - find maximum
   - Lp norm: O(n) - power operations
   
   For m×n matrix:
   - Matrix 1-norm: O(mn) - column sums
   - Matrix ∞-norm: O(mn) - row sums
   - Matrix 2-norm: O(min(m,n)³) - SVD (NOT SUPPORTED)
   - Frobenius norm: O(mn) - sum all squares
   - Matrix Lpq-norm: O(mn) - with power operations
   
   **GPU Acceleration**:
   - All norms highly parallelizable
   - 10-100× speedup on GPU vs CPU
   - Reduction operations optimized
   - Memory bandwidth often bottleneck
   
   **Typical Timings** (NVIDIA GPU, f32):
   - Vector (1M elements): ~0.1ms for any norm
   - Matrix 512×512: ~0.1ms for 1-norm or ∞-norm
   - Matrix 1024×1024: ~0.3ms for Frobenius norm
   - Large matrices scale linearly with size
   
   ## Applications
   
   ### 1. Regularization in Machine Learning
   ```clojure
   ;; L1 regularization (LASSO) - promotes sparsity
   (defn lasso-loss [X y w lambda]
     (let [residual (- y (matmul X w))
           mse (/ (sum (pow residual 2)) (count y))
           l1-penalty (* lambda (af-norm w AF_NORM_VECTOR_1))]
       (+ mse l1-penalty)))
   
   ;; L2 regularization (Ridge) - prevents overfitting
   (defn ridge-loss [X y w lambda]
     (let [residual (- y (matmul X w))
           mse (/ (sum (pow residual 2)) (count y))
           l2-penalty (* lambda (pow (af-norm w AF_NORM_VECTOR_2) 2))]
       (+ mse l2-penalty)))
   ```
   
   ### 2. Convergence Criteria
   ```clojure
   ;; Check if optimization converged
   (defn converged? [x-new x-old tolerance]
     (let [diff (- x-new x-old)
           change (af-norm diff AF_NORM_VECTOR_2)]
       (< change tolerance)))
   ```
   
   ### 3. Condition Number Estimation
   ```clojure
   ;; Estimate condition number using 1-norm
   (defn condition-number-1 [A]
     (let [A-inv (inverse A)
           norm-A (af-norm A AF_NORM_MATRIX_1)
           norm-A-inv (af-norm A-inv AF_NORM_MATRIX_1)]
       (* norm-A norm-A-inv)))
   ```
   
   ### 4. Vector Normalization
   ```clojure
   ;; Normalize vector to unit length
   (defn normalize [x norm-type]
     (let [norm-value (af-norm x norm-type)]
       (if (> norm-value 0)
         (/ x norm-value)
         x)))
   ```
   
   ### 5. Distance Metrics
   ```clojure
   ;; Compute distance between two vectors
   (defn distance [x y norm-type]
     (let [diff (- x y)]
       (af-norm diff norm-type)))
   ```
   
   ### 6. Matrix Low-Rank Approximation Quality
   ```clojure
   ;; Measure approximation error with Frobenius norm
   (defn approximation-error [A A-approx]
     (let [error-matrix (- A A-approx)]
       (af-norm error-matrix AF_NORM_MATRIX_L_PQ 2.0 2.0)))
   ```
   
   ### 7. Gradient Clipping (Neural Networks)
   ```clojure
   ;; Clip gradients by norm to prevent exploding gradients
   (defn clip-gradients [gradients max-norm]
     (let [grad-norm (af-norm gradients AF_NORM_VECTOR_2)]
       (if (> grad-norm max-norm)
         (* gradients (/ max-norm grad-norm))
         gradients)))
   ```
   
   ## Design Patterns
   
   ### Pattern 1: Robust Error Measurement
   ```clojure
   ;; Use different norms for different error sensitivities
   (defn measure-error [true-values predicted-values error-type]
     (let [error (- true-values predicted-values)]
       (case error-type
         :mean-absolute-error  ; L1 - robust to outliers
         (af-norm error AF_NORM_VECTOR_1)
         
         :root-mean-square-error  ; L2 - penalizes large errors
         (af-norm error AF_NORM_VECTOR_2)
         
         :max-absolute-error  ; L∞ - worst-case error
         (af-norm error AF_NORM_VECTOR_INF))))
   ```
   
   ### Pattern 2: Multi-Norm Regularization
   ```clojure
   ;; Elastic net: combines L1 and L2 regularization
   (defn elastic-net-penalty [w alpha l1-ratio]
     (let [l1-penalty (* alpha l1-ratio (af-norm w AF_NORM_VECTOR_1))
           l2-penalty (* alpha (- 1.0 l1-ratio) (pow (af-norm w AF_NORM_VECTOR_2) 2))]
       (+ l1-penalty l2-penalty)))
   ```
   
   ### Pattern 3: Adaptive Learning Rate
   ```clojure
   ;; Scale learning rate by gradient norm
   (defn adaptive-step [gradient base-lr]
     (let [grad-norm (af-norm gradient AF_NORM_VECTOR_2)
           scaled-lr (/ base-lr (+ 1.0 grad-norm))]
       (* gradient scaled-lr)))
   ```
   
   ### Pattern 4: Matrix Comparison
   ```clojure
   ;; Compare matrices using multiple norms
   (defn matrix-difference [A B]
     {:l1-norm (af-norm (- A B) AF_NORM_MATRIX_1)
      :linf-norm (af-norm (- A B) AF_NORM_MATRIX_INF)
      :frobenius-norm (af-norm (- A B) AF_NORM_MATRIX_L_PQ 2.0 2.0)})
   ```
   
   ## Type Support and Constraints
   
   **Supported Types**:
   - f32 (float): Single precision
   - f64 (double): Double precision
   - c32 (cfloat): Complex single precision
   - c64 (cdouble): Complex double precision
   - f16 (half): Half precision floating point
   
   **Complex Number Handling**:
   - Norms are computed on absolute values
   - For complex z = a + bi: |z| = √(a² + b²)
   - Result is always real (double)
   
   **Constraints**:
   - Input must be 1D or 2D (ndims ≤ 2)
   - For batch operations, use multiple norm calls
   - Empty arrays return 0
   - Only floating-point and complex types supported
   
   **Output**:
   - Always returns double (f64)
   - Single scalar value
   - Never returns complex
   
   ## Numerical Considerations
   
   ### Overflow and Underflow
   
   **Problem**: Computing √(x₁² + x₂² + ... + xₙ²) can overflow even if result fits
   ```
   x = [1e200, 1e200]
   x₁² = 1e400  → overflow!
   But ‖x‖₂ = √2 × 1e200 is representable
   ```
   
   **ArrayFire handles this internally** using robust algorithms
   
   ### Ill-Conditioning
   
   For nearly singular matrices:
   - Small changes in input → large changes in norm
   - Use double precision when possible
   - Matrix 2-norm most stable (when available)
   
   ### Sparse vs Dense
   
   - L1 norm: Same cost for sparse and dense
   - L2 norm: Benefits from sparsity (fewer squares)
   - L∞ norm: Benefits from sparsity (fewer comparisons)
   - For very sparse matrices, consider specialized libraries
   
   ## Comparison with Alternatives
   
   ### vs Manual Computation:
   ```clojure
   ;; Manual L2 norm (less efficient)
   (defn manual-l2-norm [x]
     (sqrt (sum (pow x 2))))
   
   ;; ArrayFire norm (optimized)
   (defn optimized-l2-norm [x]
     (af-norm x AF_NORM_VECTOR_2))
   ```
   **Advantage ArrayFire**: Optimized reductions, overflow handling, GPU parallelization
   
   ### vs Component-wise Operations:
   - Manual: Multiple passes over data
   - ArrayFire: Single fused kernel
   - 2-5× speedup from fusion
   
   ### vs External BLAS (dnrm2, dasum):
   - Similar performance on CPU
   - ArrayFire advantage: GPU acceleration, unified API
   
   ## Common Issues and Solutions
   
   ### Issue 1: Matrix 2-Norm Not Supported
   **Symptom**: Error when using AF_NORM_MATRIX_2
   **Solution**: Use SVD separately
   ```clojure
   ;; Get spectral norm via SVD
   (defn spectral-norm [A]
     (let [[U S V] (svd A)]
       (max S)))  ; Largest singular value
   ```
   
   ### Issue 2: NaN or Inf Results
   **Symptom**: norm returns NaN or Infinity
   **Solutions**:
   - Check input for NaN or Inf values
   - Use double precision for better range
   - Scale input if values are extreme
   ```clojure
   ;; Safe norm with checking
   (defn safe-norm [x norm-type]
     (if (any (isnan x))
       Double/NaN
       (af-norm x norm-type)))
   ```
   
   ### Issue 3: Zero Norm for Normalization
   **Symptom**: Division by zero when normalizing
   **Solution**: Add epsilon or check before dividing
   ```clojure
   ;; Safe normalization
   (defn safe-normalize [x norm-type epsilon]
     (let [n (af-norm x norm-type)]
       (if (> n epsilon)
         (/ x n)
         x)))  ; Return original if nearly zero
   ```
   
   ### Issue 4: Slow Performance
   **Symptom**: Norm computation slower than expected
   **Solutions**:
   - Ensure GPU backend is active
   - Check for implicit type conversions
   - Use f32 instead of f64 if precision allows
   - Batch multiple norm computations
   
   ## Mathematical Properties and Relations
   
   ### Norm Equivalence
   
   All norms on finite-dimensional spaces are equivalent:
   
   For any two norms ‖·‖ₐ and ‖·‖ᵦ, there exist constants c₁, c₂ > 0:
   c₁‖x‖ᵦ ≤ ‖x‖ₐ ≤ c₂‖x‖ᵦ
   
   **Specific relationships** (for n-dimensional vectors):
   - ‖x‖∞ ≤ ‖x‖₂ ≤ √n ‖x‖∞
   - ‖x‖₂ ≤ ‖x‖₁ ≤ √n ‖x‖₂
   - ‖x‖∞ ≤ ‖x‖₁ ≤ n ‖x‖∞
   
   ### Dual Norms
   
   The dual norm ‖·‖* is defined by:
   ‖y‖* = max{⟨x,y⟩ : ‖x‖ ≤ 1}
   
   **Duality relationships**:
   - (Lp)* = Lq where 1/p + 1/q = 1
   - (L1)* = L∞
   - (L2)* = L2 (self-dual)
   - (L∞)* = L1
   
   ### Submultiplicativity
   
   For matrix norms:
   ‖AB‖ ≤ ‖A‖ · ‖B‖
   
   Holds for:
   - All induced norms (1-norm, ∞-norm, 2-norm)
   - Frobenius norm
   - Most Lpq-norms
   
   ## Error Handling
   
   The function returns an error code (af_err). Common errors:
   
   - **AF_ERR_TYPE**: Input not floating-point or complex
     * Only f32, f64, c32, c64, f16 supported
   
   - **AF_ERR_SIZE**: Input has more than 2 dimensions
     * Use ndims ≤ 2 for norms
   
   - **AF_ERR_ARG**: Invalid norm type
     * Check norm type is valid enum value
   
   - **AF_ERR_NOT_SUPPORTED**: Matrix 2-norm requested
     * Use SVD separately for spectral norm
   
   - **AF_ERR_BATCH**: Input is batched (ndims > 2)
     * Reshape or slice to 2D
   
   ## Best Practices
   
   1. **Choose appropriate norm**:
      - Default: L2 (Euclidean) for most applications
      - Robustness: L1 for outlier-resistant metrics
      - Worst-case: L∞ for error bounds
      - Sparsity: L1 for regularization
   
   2. **Numerical stability**:
      - Use double precision for ill-conditioned problems
      - ArrayFire handles overflow automatically
      - Check for NaN/Inf in results
   
   3. **Performance**:
      - Batch operations when possible
      - Use f32 unless f64 required
      - Frobenius norm faster than spectral norm
   
   4. **Regularization**:
      - L1: Feature selection, sparse solutions
      - L2: Smooth solutions, prevents overfitting
      - Elastic net: Compromise between L1 and L2
   
   5. **Convergence checking**:
      - Relative change: ‖xₙ₊₁ - xₙ‖ / ‖xₙ‖
      - Absolute change: ‖xₙ₊₁ - xₙ‖
      - Use same norm throughout optimization
   
   ## See Also
   
   - af-dot: Dot product (related to L2 norm)
   - af-abs: Element-wise absolute value
   - af-sum: Reduction operation (used in L1 norm)
   - af-max: Maximum element (used in L∞ norm)
   - af-svd: Singular value decomposition (for spectral norm)
   - af-det: Determinant
   - af-rank: Matrix rank
   
   ## References
   
   - Golub & Van Loan: \"Matrix Computations\" (4th ed.)
   - Horn & Johnson: \"Matrix Analysis\" (2nd ed.)
   - Boyd & Vandenberghe: \"Convex Optimization\"
   - Trefethen & Bau: \"Numerical Linear Algebra\""
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.loader]))

;; af_norm_type enum constants
;; Defines different types of norms for vectors and matrices
(def AF_NORM_VECTOR_1 0)      ; Vector L1 norm (sum of absolute values)
(def AF_NORM_VECTOR_INF 1)    ; Vector L∞ norm (maximum absolute value)
(def AF_NORM_VECTOR_2 2)      ; Vector L2 norm (Euclidean norm)
(def AF_NORM_VECTOR_P 3)      ; Vector Lp norm (general p-norm)
(def AF_NORM_MATRIX_1 4)      ; Matrix 1-norm (maximum absolute column sum)
(def AF_NORM_MATRIX_INF 5)    ; Matrix ∞-norm (maximum absolute row sum)
(def AF_NORM_MATRIX_2 6)      ; Matrix 2-norm (spectral norm) - NOT SUPPORTED
(def AF_NORM_MATRIX_L_PQ 7)   ; Matrix Lpq-norm (generalized matrix norm)
(def AF_NORM_EUCLID 2)        ; Alias for AF_NORM_VECTOR_2 (default)

;; af_err af_norm(double *out, const af_array in, const af_norm_type type, const double p, const double q)
(defcfn af-norm
  "Compute the norm of a vector or matrix.
   
   Calculates various vector and matrix norms that measure the \"size\" or
   \"magnitude\" of arrays. Essential for numerical analysis, optimization,
   machine learning, and scientific computing.
   
   Parameters:
   - out: out pointer for norm value (double)
     * Always double precision regardless of input type
     * Single scalar value
     * Non-negative (norm properties)
     * 0 if and only if input is zero array
   
   - in: input array (vector or matrix)
     * Must be 1D (vector) or 2D (matrix)
     * Types: f32, f64, c32, c64, f16 (floating-point or complex)
     * For complex: norm computed on absolute values
     * Empty arrays return 0
     * Shape constraints:
       - Vector: [n] or [n, 1] or [1, n]
       - Matrix: [m, n]
       - Batch mode NOT supported (ndims must be ≤ 2)
   
   - type: norm type (af_norm_type enum)
     * AF_NORM_VECTOR_1 (0): L1 norm (sum of absolute values)
       - Formula: ‖x‖₁ = Σᵢ |xᵢ|
       - Complexity: O(n)
       - Use for: Robust statistics, LASSO regularization
     
     * AF_NORM_VECTOR_INF (1): L∞ norm (maximum absolute value)
       - Formula: ‖x‖∞ = maxᵢ |xᵢ|
       - Complexity: O(n)
       - Use for: Worst-case error, error bounds
     
     * AF_NORM_VECTOR_2 (2): L2 norm (Euclidean norm)
       - Formula: ‖x‖₂ = √(Σᵢ xᵢ²)
       - Complexity: O(n)
       - Use for: Standard distance, Ridge regularization
       - Default norm (AF_NORM_EUCLID)
     
     * AF_NORM_VECTOR_P (3): Lp norm (requires parameter p)
       - Formula: ‖x‖ₚ = (Σᵢ |xᵢ|ᵖ)^(1/p)
       - Complexity: O(n)
       - Use for: Flexible regularization, custom metrics
     
     * AF_NORM_MATRIX_1 (4): Matrix 1-norm (max column sum)
       - Formula: ‖A‖₁ = maxⱼ Σᵢ |aᵢⱼ|
       - Complexity: O(mn)
       - Use for: Condition number estimation
     
     * AF_NORM_MATRIX_INF (5): Matrix ∞-norm (max row sum)
       - Formula: ‖A‖∞ = maxᵢ Σⱼ |aᵢⱼ|
       - Complexity: O(mn)
       - Use for: Numerical stability analysis
     
     * AF_NORM_MATRIX_2 (6): Matrix 2-norm (spectral norm)
       - Formula: ‖A‖₂ = σₘₐₓ(A) (largest singular value)
       - NOT SUPPORTED - requires SVD
       - Use af_svd separately if needed
     
     * AF_NORM_MATRIX_L_PQ (7): Matrix Lpq-norm (requires p and q)
       - Formula: ‖A‖ₚ,q = (Σⱼ (Σᵢ |aᵢⱼ|ᵖ)^(q/p))^(1/q)
       - Complexity: O(mn) with power operations
       - Special case (p=2, q=2): Frobenius norm
       - Use for: Matrix approximation, regularization
   
   - p: parameter for Lp or Lpq norms
     * Used when type = AF_NORM_VECTOR_P or AF_NORM_MATRIX_L_PQ
     * Must be > 0 (typically p ≥ 1 for true norm)
     * Common values:
       - p = 1: L1 norm
       - p = 2: L2 norm (Euclidean)
       - p = ∞: Use AF_NORM_VECTOR_INF instead
     * Ignored for other norm types (can pass 1.0)
   
   - q: parameter for Lpq matrix norms
     * Used only when type = AF_NORM_MATRIX_L_PQ
     * Must be > 0
     * Controls aggregation across columns
     * Common values:
       - q = 1: Sum of column norms
       - q = 2: Frobenius norm (with p=2)
       - q = ∞: Maximum column norm
     * Ignored for vector norms (can pass 1.0)
   
   Algorithm (varies by norm type):
   
   Vector L1:
   1. Compute absolute values: |x₁|, |x₂|, ..., |xₙ|
   2. Sum: Σᵢ |xᵢ|
   
   Vector L2:
   1. Square each element: x₁², x₂², ..., xₙ²
   2. Sum: Σᵢ xᵢ²
   3. Square root: √(Σᵢ xᵢ²)
   
   Vector L∞:
   1. Compute absolute values: |x₁|, |x₂|, ..., |xₙ|
   2. Find maximum: maxᵢ |xᵢ|
   
   Matrix 1-norm:
   1. For each column j: sum absolute values cⱼ = Σᵢ |aᵢⱼ|
   2. Return maximum: maxⱼ cⱼ
   
   Matrix Frobenius (p=2, q=2):
   1. Square all elements: aᵢⱼ²
   2. Sum all: Σᵢⱼ aᵢⱼ²
   3. Square root: √(Σᵢⱼ aᵢⱼ²)
   
   Performance:
   - Complexity: O(n) for vectors, O(mn) for m×n matrices
   - GPU acceleration: 10-100× speedup
   - Memory: Single pass through data
   - Typical timings (NVIDIA GPU, f32):
     * Vector 1M elements: ~0.1ms
     * Matrix 1024×1024: ~0.3ms for Frobenius
     * Scales linearly with array size
   
   Type Support:
   - Input: f32, f64, c32, c64, f16
   - Complex: Norm computed on |z| = √(real² + imag²)
   - Output: Always double (f64)
   - Result is always real and non-negative
   
   Example 1: Vector L2 Norm (Euclidean Length)
   ```clojure
   ;; Compute length of vector
   (let [v (create-array [3.0 4.0] [2])  ; 2D vector
         norm-ptr (mem/alloc-pointer ::mem/double)
         
         ;; Compute L2 norm
         _ (af-norm norm-ptr v AF_NORM_VECTOR_2 1.0 1.0)
         
         length (mem/read-pointer norm-ptr ::mem/double)]
     
     length)  ; => 5.0 (√(3² + 4²) = √25 = 5)
   ```
   
   Example 2: Vector Normalization
   ```clojure
   ;; Normalize vector to unit length
   (let [v (create-array [1.0 2.0 3.0] [3])
         norm-ptr (mem/alloc-pointer ::mem/double)
         
         ;; Get L2 norm
         _ (af-norm norm-ptr v AF_NORM_VECTOR_2 1.0 1.0)
         norm-value (mem/read-pointer norm-ptr ::mem/double)
         
         ;; Divide by norm
         v-normalized (/ v norm-value)]
     
     v-normalized)  ; Unit vector in same direction
   ```
   
   Example 3: L1 Regularization (LASSO)
   ```clojure
   ;; Compute LASSO loss with L1 penalty
   (defn lasso-loss [X y weights lambda]
     (let [predictions (matmul X weights)
           residuals (- y predictions)
           
           ;; Mean squared error
           mse-ptr (mem/alloc-pointer ::mem/double)
           _ (af-norm mse-ptr residuals AF_NORM_VECTOR_2 1.0 1.0)
           mse-value (mem/read-pointer mse-ptr ::mem/double)
           mse (/ (* mse-value mse-value) (count y))
           
           ;; L1 penalty
           l1-ptr (mem/alloc-pointer ::mem/double)
           _ (af-norm l1-ptr weights AF_NORM_VECTOR_1 1.0 1.0)
           l1-penalty (* lambda (mem/read-pointer l1-ptr ::mem/double))]
       
       (+ mse l1-penalty)))
   ```
   
   Example 4: Matrix 1-Norm (Column Sum)
   ```clojure
   ;; Compute matrix 1-norm for condition number estimation
   (let [A (create-array [[1.0 -2.0 3.0]
                          [4.0  5.0 -6.0]
                          [7.0 -8.0 9.0]] [3 3])
         
         norm-ptr (mem/alloc-pointer ::mem/double)
         
         ;; Matrix 1-norm: max column sum
         _ (af-norm norm-ptr A AF_NORM_MATRIX_1 1.0 1.0)
         
         norm-1 (mem/read-pointer norm-ptr ::mem/double)]
     
     norm-1)  ; => 18.0 (max of columns: 12, 15, 18)
   ```
   
   Example 5: Frobenius Norm (Matrix Magnitude)
   ```clojure
   ;; Compute Frobenius norm for matrix approximation error
   (defn frobenius-error [A A-approx]
     (let [error (- A A-approx)
           norm-ptr (mem/alloc-pointer ::mem/double)
           
           ;; Frobenius norm: p=2, q=2
           _ (af-norm norm-ptr error AF_NORM_MATRIX_L_PQ 2.0 2.0)
           
           error-norm (mem/read-pointer norm-ptr ::mem/double)]
       
       error-norm))
   ```
   
   Example 6: Convergence Checking
   ```clojure
   ;; Check if optimization converged
   (defn converged? [x-new x-old tolerance]
     (let [diff (- x-new x-old)
           norm-ptr (mem/alloc-pointer ::mem/double)
           
           ;; Compute L2 norm of difference
           _ (af-norm norm-ptr diff AF_NORM_VECTOR_2 1.0 1.0)
           
           change (mem/read-pointer norm-ptr ::mem/double)]
       
       (< change tolerance)))
   ```
   
   Example 7: Gradient Clipping (Neural Networks)
   ```clojure
   ;; Clip gradients by global norm to prevent exploding gradients
   (defn clip-gradients [gradients max-norm]
     (let [norm-ptr (mem/alloc-pointer ::mem/double)
           
           ;; Compute gradient norm
           _ (af-norm norm-ptr gradients AF_NORM_VECTOR_2 1.0 1.0)
           
           grad-norm (mem/read-pointer norm-ptr ::mem/double)]
       
       (if (> grad-norm max-norm)
         ;; Scale down to max-norm
         (* gradients (/ max-norm grad-norm))
         ;; Keep unchanged
         gradients)))
   ```
   
   Common Use Cases:
   - **Regularization**: L1 (sparsity), L2 (smoothness), elastic net
   - **Convergence criteria**: Optimization stopping conditions
   - **Error measurement**: Residual norms, approximation errors
   - **Normalization**: Unit vectors, standardization
   - **Condition numbers**: Matrix well-conditioning
   - **Distance metrics**: Similarity measures, clustering
   - **Gradient clipping**: Neural network training stability
   
   Common Patterns:
   
   Pattern 1: Safe normalization (avoid division by zero)
   ```clojure
   (defn safe-normalize [v norm-type epsilon]
     (let [norm-ptr (mem/alloc-pointer ::mem/double)
           _ (af-norm norm-ptr v norm-type 1.0 1.0)
           n (mem/read-pointer norm-ptr ::mem/double)]
       (if (> n epsilon)
         (/ v n)
         v)))  ; Return original if nearly zero
   ```
   
   Pattern 2: Elastic net regularization
   ```clojure
   (defn elastic-net-penalty [weights alpha l1-ratio]
     (let [l1-ptr (mem/alloc-pointer ::mem/double)
           l2-ptr (mem/alloc-pointer ::mem/double)
           _ (af-norm l1-ptr weights AF_NORM_VECTOR_1 1.0 1.0)
           _ (af-norm l2-ptr weights AF_NORM_VECTOR_2 1.0 1.0)
           l1 (mem/read-pointer l1-ptr ::mem/double)
           l2 (mem/read-pointer l2-ptr ::mem/double)]
       (+ (* alpha l1-ratio l1)
          (* alpha (- 1.0 l1-ratio) l2 l2))))
   ```
   
   Pattern 3: Relative change metric
   ```clojure
   (defn relative-change [x-new x-old]
     (let [diff-ptr (mem/alloc-pointer ::mem/double)
           old-ptr (mem/alloc-pointer ::mem/double)
           diff (- x-new x-old)
           _ (af-norm diff-ptr diff AF_NORM_VECTOR_2 1.0 1.0)
           _ (af-norm old-ptr x-old AF_NORM_VECTOR_2 1.0 1.0)
           diff-norm (mem/read-pointer diff-ptr ::mem/double)
           old-norm (mem/read-pointer old-ptr ::mem/double)]
       (if (> old-norm 0)
         (/ diff-norm old-norm)
         diff-norm)))
   ```
   
   When to use which norm:
   - **L1**: Outlier robustness, sparse solutions
   - **L2**: Standard metric, smooth solutions
   - **L∞**: Worst-case bounds, uniform error
   - **Lp (0<p<1)**: Very sparse solutions (not true norm)
   - **Frobenius**: Matrix approximation, efficient computation
   
   When NOT to use:
   - Matrix 2-norm: Not supported (use SVD separately)
   - 3D+ arrays: Batch mode not supported (slice or reshape to 2D)
   - Integer arrays: Only floating-point supported
   - When specific norm structure not needed: Use specialized functions
   
   Gotchas:
   - **Matrix 2-norm fails**: Use af_svd for spectral norm
   - **NaN propagation**: NaN in input → NaN output
   - **Overflow**: Very large values may overflow (use f64)
   - **Zero norm**: Check before dividing for normalization
   - **p/q parameters**: Ignored unless using AF_NORM_VECTOR_P or AF_NORM_MATRIX_L_PQ
   - **Complex input**: Norm always real (absolute values used)
   - **Empty arrays**: Return 0, not error
   - **Batch error**: ndims > 2 not supported
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-dot: Inner product (related to L2 norm squared)
   - af-sum: Sum reduction (used in L1 norm)
   - af-max: Maximum (used in L∞ norm)
   - af-abs: Element-wise absolute value
   - af-svd: Singular value decomposition (for spectral norm)
   - af-det: Determinant
   - af-rank: Matrix rank"
  "af_norm" [::mem/pointer ::mem/pointer ::mem/int ::mem/double ::mem/double] ::mem/int)
