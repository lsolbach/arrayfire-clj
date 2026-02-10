(ns org.soulspace.arrayfire.ffi.c-api.rank
  "Bindings for the ArrayFire matrix rank function.
   
   Matrix rank is a fundamental concept in linear algebra that measures
   the dimension of the vector space spanned by the rows or columns of
   a matrix. It represents the maximum number of linearly independent
   rows or columns in the matrix.
   
   Mathematical Definition:
   
   The **rank** of an m×n matrix A is:
   - The dimension of the column space (image) of A
   - The dimension of the row space of A
   - The number of linearly independent columns
   - The number of linearly independent rows
   - The number of non-zero singular values in the SVD
   
   For an m×n matrix A:
   - rank(A) ≤ min(m, n)  (maximum possible rank)
   - If rank(A) = min(m, n), the matrix has full rank
   - If rank(A) < min(m, n), the matrix is rank-deficient
   
   Properties:
   1. **Rank-Nullity Theorem**: For A ∈ ℝ^(m×n):
      rank(A) + nullity(A) = n
      where nullity is the dimension of the null space
   
   2. **Transpose**: rank(A) = rank(A^T)
   
   3. **Product**: rank(AB) ≤ min(rank(A), rank(B))
   
   4. **Sum**: rank(A + B) ≤ rank(A) + rank(B)
   
   5. **Invertibility**: Square matrix A is invertible ⟺ rank(A) = n
   
   Computational Method:
   
   ArrayFire computes rank using the QR decomposition:
   
   1. **QR Factorization**: A = QR
      - Q is orthogonal (m×m)
      - R is upper triangular (m×n)
   
   2. **Diagonal Inspection**: Examine diagonal elements R[i,i]
      - If |R[i,i]| > tol: linearly independent
      - If |R[i,i]| ≤ tol: linearly dependent
   
   3. **Count**: rank = number of diagonal elements > tol
   
   The QR method is numerically stable and efficient, with complexity
   O(mn²) for an m×n matrix.
   
   Tolerance Parameter:
   
   The tolerance `tol` determines when a diagonal element is considered
   effectively zero:
   - **Purpose**: Handle numerical errors in floating-point arithmetic
   - **Effect**: |R[i,i]| ≤ tol ⇒ treat as zero (linearly dependent)
   - **Default**: 1e-5 (suitable for many applications)
   - **Typical values**:
     * High precision: 1e-12 to 1e-10 (double precision)
     * Standard: 1e-6 to 1e-5 (general use)
     * Robust: 1e-4 to 1e-3 (noisy data)
   
   Choosing tolerance:
   - **Too small**: May count noise as rank, overestimate
   - **Too large**: May miss weak dependencies, underestimate
   - **Rule of thumb**: tol ≈ max(m,n) * eps * ||A||
     where eps is machine epsilon
   
   Matrix Classification by Rank:
   
   For m×n matrix A:
   - **Full rank**: rank(A) = min(m, n)
     * All rows/columns linearly independent
     * Maximum possible rank achieved
   
   - **Rank-deficient**: rank(A) < min(m, n)
     * Some rows/columns linearly dependent
     * Redundant information present
   
   - **Zero rank**: rank(A) = 0
     * All elements effectively zero
     * Only possible for zero matrix
   
   Special Cases:
   - **Square matrix (n×n)**:
     * Full rank (rank = n): Invertible, det ≠ 0
     * Rank-deficient (rank < n): Singular, det = 0
   
   - **Tall matrix (m > n)**:
     * Full rank: rank = n (all columns independent)
     * Can have unique least-squares solutions
   
   - **Wide matrix (m < n)**:
     * Full rank: rank = m (all rows independent)
     * Underdetermined systems, multiple solutions
   
   Applications:
   
   1. **System Solvability**:
      - For Ax = b to have a unique solution:
        * A must be square (n×n)
        * rank(A) = n (full rank, invertible)
      - If rank(A) < n: infinite solutions or no solution
   
   2. **Data Analysis**:
      - Determine effective dimensionality of data
      - Identify redundant features/measurements
      - Data compression possibilities
   
   3. **Model Complexity**:
      - Degrees of freedom in statistical models
      - Number of independent parameters
      - Model identifiability
   
   4. **Image Processing**:
      - Rank of image matrix indicates complexity
      - Low-rank approximation for compression
      - Denoising via low-rank models
   
   5. **Control Theory**:
      - Controllability and observability
      - System rank determines reachable states
      - Stability analysis
   
   6. **Machine Learning**:
      - Feature selection (remove dependent features)
      - Principal Component Analysis (PCA)
      - Matrix completion problems
      - Collaborative filtering
   
   7. **Computer Vision**:
      - Structure from motion (fundamental matrix rank 2)
      - Homography estimation
      - Multi-view geometry
   
   8. **Signal Processing**:
      - Subspace methods for signal estimation
      - Array processing and beamforming
      - Source localization
   
   Performance Considerations:
   
   - **Complexity**: O(mn²) for m×n matrix via QR
   - **Memory**: O(mn) for matrix storage, O(min(m,n)²) for Q
   - **GPU Acceleration**: Significant speedup for large matrices
     * Typical: 10-50× faster than CPU for m,n > 1000
     * Parallel QR decomposition
   - **Batch Processing**: Can process multiple matrices
   - **Precision**: Double precision recommended for accuracy
   
   Numerical Stability:
   
   The QR-based method is numerically stable because:
   1. Orthogonal transformations preserve numerical properties
   2. No squaring of condition number (unlike normal equations)
   3. Directly exposes linear dependencies via R diagonal
   
   Alternative Methods:
   - **SVD**: More accurate but slower (O(mn²) to O(mn min(m,n)))
     * rank = number of σ_i > tol * σ_max
     * Most reliable for ill-conditioned matrices
   - **LU**: Similar cost but less stable
   - **Gaussian elimination**: Direct but less stable
   
   Examples and Use Cases:
   
   Basic rank computation:
   ```clojure
   (let [A (create-array [1 2 3
                          4 5 6
                          7 8 9] [3 3])
         rank-ptr (mem/alloc-int)
         _ (af-rank rank-ptr A 1e-5)]
     (mem/read-int rank-ptr))
   ; => 2 (third row is 3*first + 0*second)
   ```
   
   Check invertibility:
   ```clojure
   (defn invertible? [matrix tol]
     (let [dims (get-dims matrix)
           n (first dims)
           rank-ptr (mem/alloc-int)
           _ (af-rank rank-ptr matrix tol)
           r (mem/read-int rank-ptr)]
       (= r n)))
   
   (invertible? identity-matrix 1e-5)
   ; => true
   ```
   
   Detect rank deficiency:
   ```clojure
   (defn rank-deficiency [matrix tol]
     (let [dims (get-dims matrix)
           full-rank (apply min dims)
           rank-ptr (mem/alloc-int)
           _ (af-rank rank-ptr matrix tol)
           actual-rank (mem/read-int rank-ptr)]
       (- full-rank actual-rank)))
   
   (rank-deficiency singular-matrix 1e-5)
   ; => 1 (one dimension of dependency)
   ```
   
   Data dimensionality:
   ```clojure
   ;; Each column is a data sample, each row is a feature
   (let [data (create-array samples [n-features n-samples])
         rank-ptr (mem/alloc-int)
         _ (af-rank rank-ptr data 1e-5)
         effective-dims (mem/read-int rank-ptr)]
     (println \"Effective feature dimensions:\" effective-dims)
     (when (< effective-dims n-features)
       (println \"Redundant features detected!\")))
   ```
   
   Tolerance sensitivity:
   ```clojure
   (defn rank-vs-tolerance [matrix tolerances]
     (for [tol tolerances]
       (let [rank-ptr (mem/alloc-int)
             _ (af-rank rank-ptr matrix tol)]
         {:tolerance tol
          :rank (mem/read-int rank-ptr)})))
   
   (rank-vs-tolerance noisy-matrix [1e-8 1e-6 1e-4 1e-2])
   ; => ({:tolerance 1e-8, :rank 100}
   ;     {:tolerance 1e-6, :rank 95}
   ;     {:tolerance 1e-4, :rank 90}
   ;     {:tolerance 1e-2, :rank 50})
   ```
   
   System analysis:
   ```clojure
   (defn analyze-system [A b tol]
     (let [m (first (get-dims A))
           n (second (get-dims A))
           rank-A (mem/alloc-int)
           _ (af-rank rank-A A tol)
           rA (mem/read-int rank-A)
           
           ;; Augmented matrix [A|b]
           Ab (join 1 A b)
           rank-Ab (mem/alloc-int)
           _ (af-rank rank-Ab Ab tol)
           rAb (mem/read-int rank-Ab)]
       
       (cond
         (not= rA rAb)
         {:solvable false
          :reason \"Inconsistent system (rank(A) ≠ rank([A|b]))\"}
         
         (and (= rA rAb) (= rA n))
         {:solvable true
          :solution-type :unique
          :rank rA}
         
         (and (= rA rAb) (< rA n))
         {:solvable true
          :solution-type :infinite
          :rank rA
          :free-variables (- n rA)})))
   ```
   
   Low-rank approximation check:
   ```clojure
   (defn recommend-compression [matrix target-rank tol]
     (let [rank-ptr (mem/alloc-int)
           _ (af-rank rank-ptr matrix tol)
           actual-rank (mem/read-int rank-ptr)
           dims (get-dims matrix)
           m (first dims)
           n (second dims)
           original-size (* m n)
           compressed-size (* (+ m n) target-rank)]
       
       (when (<= target-rank actual-rank)
         {:actual-rank actual-rank
          :target-rank target-rank
          :original-size original-size
          :compressed-size compressed-size
          :compression-ratio (/ compressed-size original-size)
          :recommended? (< compressed-size (* 0.5 original-size))})))
   ```
   
   Feature selection:
   ```clojure
   (defn redundant-features? [feature-matrix tol]
     \"Check if feature matrix has redundant features\"
     (let [dims (get-dims feature-matrix)
           n-features (first dims)
           rank-ptr (mem/alloc-int)
           _ (af-rank rank-ptr feature-matrix tol)
           effective-rank (mem/read-int rank-ptr)]
       
       {:redundant? (< effective-rank n-features)
        :n-features n-features
        :effective-rank effective-rank
        :redundant-count (- n-features effective-rank)}))
   ```
   
   Relationship to Other Operations:
   
   **Rank and SVD**:
   ```clojure
   ;; Rank via SVD (alternative, more accurate)
   (defn rank-from-svd [matrix tol]
     (let [[U S Vt] (svd matrix)
           sigma-max (max-all S)
           threshold (* tol sigma-max)]
       (count-nonzero (> S threshold))))
   ```
   
   **Rank and Determinant**:
   ```clojure
   ;; For square matrices
   (defn full-rank-square? [matrix tol]
     (let [rank-ptr (mem/alloc-int)
           _ (af-rank rank-ptr matrix tol)
           r (mem/read-int rank-ptr)
           n (first (get-dims matrix))]
       (and (= r n)
            (not= 0.0 (det matrix)))))
   ```
   
   **Rank and Condition Number**:
   ```clojure
   ;; High condition number may affect rank computation
   (defn stable-rank-estimation [matrix]
     (let [condition (norm matrix :inf)
           suggested-tol (* condition 1e-15)
           rank-ptr (mem/alloc-int)
           _ (af-rank rank-ptr matrix suggested-tol)]
       {:rank (mem/read-int rank-ptr)
        :condition-number condition
        :tolerance-used suggested-tol}))
   ```
   
   Type Support:
   - **Supported**: f32 (float), f64 (double), c32 (complex float), c64 (complex double)
   - **Not supported**: Integer types, boolean, half precision
   - **Requirement**: Floating-point or complex types only
   
   Error Handling:
   - **AF_ERR_BATCH**: Matrix must be 2D (no batch mode)
   - **AF_ERR_ARG**: Invalid arguments (null pointer, wrong type)
   - **AF_ERR_TYPE**: Non-floating type provided
   - **Empty matrix**: Returns rank 0
   
   Best Practices:
   
   1. **Tolerance Selection**:
      - Use relative tolerance: tol ≈ eps * max(m,n) * ||A||
      - For double precision: start with 1e-12 to 1e-10
      - For single precision: start with 1e-6 to 1e-5
      - Increase for noisy data
   
   2. **Numerical Conditioning**:
      - Check condition number before rank computation
      - High condition number → use tighter tolerance
      - Consider scaling/normalization for ill-conditioned matrices
   
   3. **Validation**:
      - Verify rank makes sense for problem context
      - Cross-check with determinant for square matrices
      - Use SVD for critical applications requiring highest accuracy
   
   4. **Performance**:
      - Batch rank computation when possible
      - Use GPU for large matrices (m,n > 1000)
      - Consider caching results for repeated queries
   
   5. **Interpretation**:
      - Rank < min(m,n) indicates dependency
      - For data analysis, rank indicates effective dimensionality
      - For systems, rank determines solvability
   
   Common Pitfalls:
   - **Fixed tolerance**: Use relative tolerance, not absolute
   - **Ignoring conditioning**: Ill-conditioned matrices need careful handling
   - **Type mismatch**: Must use floating-point types
   - **Over-interpretation**: Rank is sensitive to tolerance choice
   
   See Also:
   - QR decomposition (af-qr): Underlying algorithm for rank
   - SVD (af-svd): Alternative, more accurate rank computation
   - Determinant (af-det): Related to rank for square matrices
   - Solve (af-solve): Requires full rank for unique solution
   - Norm (af-norm): Used in relative tolerance computation"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.loader]))

;; af_err af_rank(unsigned *rank, const af_array in, const double tol)
(defcfn af-rank
  "Compute the rank of a matrix.
   
   Calculates the matrix rank using QR decomposition. The rank is the
   number of linearly independent rows or columns, equivalently the
   number of non-zero diagonal elements in R (from QR = A) above the
   specified tolerance.
   
   Algorithm:
   1. Perform QR decomposition: A = QR
   2. Examine diagonal of upper triangular matrix R
   3. Count diagonal elements where |R[i,i]| > tol
   4. Return count as rank
   
   Parameters:
   - rank: out pointer to unsigned int (stores computed rank)
   - in: input matrix (must be 2D, floating-point type)
   - tol: tolerance threshold for considering elements non-zero
     * Typical values: 1e-5 (default), 1e-12 (high precision)
     * Elements with |R[i,i]| ≤ tol are treated as zero
     * Affects rank determination for nearly-singular matrices
   
   Matrix Requirements:
   - Dimensions: 2D only (no batch mode support)
   - Type: f32, f64, c32, or c64 (floating-point or complex)
   - Shape: Any m×n matrix
   - Empty matrix: Returns rank 0
   
   Rank Interpretation:
   - rank(A) = min(m, n): Full rank matrix
   - rank(A) < min(m, n): Rank-deficient matrix
   - rank(A) = 0: Zero matrix (all elements ≈ 0)
   
   For square matrices (n×n):
   - rank(A) = n: Matrix is invertible (det ≠ 0)
   - rank(A) < n: Matrix is singular (det = 0, not invertible)
   
   Tolerance Effects:
   - Smaller tol: More strict, may count noise as rank
   - Larger tol: More lenient, may miss weak dependencies
   - Rule of thumb: tol ≈ max(m,n) * eps * ||A||_F
     where eps is machine epsilon (2.22e-16 for double)
   
   Complexity:
   - Time: O(mn²) for m×n matrix (via QR decomposition)
   - Space: O(mn) for storage, additional O(min(m,n)²) for Q
   - GPU: Significant speedup for large matrices (10-50×)
   
   Numerical Stability:
   - QR method is numerically stable
   - Orthogonal transformations preserve conditioning
   - More stable than methods using A^T*A (normal equations)
   
   Type Support:
   - f32: Single precision float
   - f64: Double precision float (recommended for accuracy)
   - c32: Complex float (cfloat)
   - c64: Complex double (cdouble)
   
   Error Conditions:
   - AF_ERR_ARG: rank pointer is null
   - AF_ERR_TYPE: Input is not floating-point type
   - AF_ERR_BATCH: Input has more than 2 dimensions
   - AF_SUCCESS: Operation completed successfully
   
   Example (Full Rank):
   ```clojure
   ;; Identity matrix (full rank)
   (let [I (identity-matrix 4)
         rank-ptr (mem/alloc-int)
         err (af-rank rank-ptr I 1e-5)
         r (mem/read-int rank-ptr)]
     (println \"Identity rank:\" r))
   ; => Identity rank: 4
   ```
   
   Example (Rank Deficient):
   ```clojure
   ;; Matrix with linearly dependent rows
   ;; Row 3 = 2*Row1 - Row2
   (let [A (create-array [1.0  2.0  3.0
                          4.0  5.0  6.0
                          -2.0 -1.0  0.0] [3 3])
         rank-ptr (mem/alloc-int)
         err (af-rank rank-ptr A 1e-5)
         r (mem/read-int rank-ptr)]
     (println \"Matrix rank:\" r))
   ; => Matrix rank: 2
   ```
   
   Example (Tolerance Effect):
   ```clojure
   ;; Nearly singular matrix
   (let [A (create-array [1.0    0.0
                          0.0    1e-7] [2 2])
         rank-strict (mem/alloc-int)
         rank-lenient (mem/alloc-int)
         _ (af-rank rank-strict A 1e-8)
         _ (af-rank rank-lenient A 1e-6)]
     (println \"Strict tolerance rank:\" (mem/read-int rank-strict))
     (println \"Lenient tolerance rank:\" (mem/read-int rank-lenient)))
   ; => Strict tolerance rank: 2
   ; => Lenient tolerance rank: 1
   ```
   
   Example (System Solvability):
   ```clojure
   ;; Check if Ax=b has unique solution
   (defn has-unique-solution? [A tol]
     (let [dims (get-dims A)
           m (first dims)
           n (second dims)
           rank-ptr (mem/alloc-int)
           _ (af-rank rank-ptr A tol)
           r (mem/read-int rank-ptr)]
       (and (= m n)      ; Square matrix
            (= r n))))   ; Full rank
   
   (has-unique-solution? coefficient-matrix 1e-5)
   ```
   
   Example (Data Analysis):
   ```clojure
   ;; Determine effective dimensionality of data
   ;; Columns are samples, rows are features
   (let [data (create-array samples [100 1000])  ; 100 features
         rank-ptr (mem/alloc-int)
         _ (af-rank rank-ptr data 1e-5)
         effective-dim (mem/read-int rank-ptr)]
     (println \"Effective dimensions:\" effective-dim)
     (when (< effective-dim 100)
       (println \"Redundancy detected: could reduce to\"
                effective-dim \"features\")))
   ```
   
   Example (Conditioning Check):
   ```clojure
   ;; Adjust tolerance based on condition number
   (defn compute-rank-with-conditioning [matrix]
     (let [cond-num (/ (norm matrix :inf)
                       (norm (inverse matrix) :inf))
           adaptive-tol (* cond-num 1e-15)
           rank-ptr (mem/alloc-int)
           _ (af-rank rank-ptr matrix adaptive-tol)]
       {:rank (mem/read-int rank-ptr)
        :condition-number cond-num
        :tolerance adaptive-tol}))
   ```
   
   Example (Batch Checking):
   ```clojure
   ;; Check ranks of multiple matrices
   (defn rank-statistics [matrices tol]
     (let [ranks (map (fn [m]
                        (let [rp (mem/alloc-int)
                              _ (af-rank rp m tol)]
                          (mem/read-int rp)))
                      matrices)]
       {:ranks ranks
        :mean (/ (reduce + ranks) (count ranks))
        :max (apply max ranks)
        :min (apply min ranks)}))
   ```
   
   Applications:
   - Linear algebra: Check invertibility, solve systems
   - Statistics: Determine model degrees of freedom
   - Data science: Feature selection, dimensionality reduction
   - Image processing: Image rank for compression analysis
   - Control theory: Controllability and observability
   - Computer vision: Fundamental matrix estimation (rank 2)
   - Machine learning: Feature redundancy detection
   
   Related Functions:
   - af-qr: QR decomposition (underlying algorithm)
   - af-svd: SVD (alternative method, more accurate but slower)
   - af-det: Determinant (zero if rank-deficient for square matrices)
   - af-solve: Linear system solver (requires full rank)
   - af-inverse: Matrix inversion (requires full rank)
   
   Returns:
   ArrayFire error code (af_err):
   - AF_SUCCESS (0): Rank computed successfully
   - AF_ERR_ARG: Invalid argument (null pointer)
   - AF_ERR_TYPE: Invalid type (not floating-point)
   - AF_ERR_BATCH: Invalid dimensions (not 2D)
   
   See also:
   - QR decomposition for the underlying algorithm
   - SVD for an alternative, more accurate method
   - Condition number for numerical stability assessment"
  "af_rank" [::mem/pointer ::mem/pointer ::mem/double] ::mem/int)
