(ns org.soulspace.arrayfire.ffi.c-api.covariance
  "Bindings for the ArrayFire covariance functions.
   
   Covariance measures the joint variability of two random variables.
   It indicates the direction of the linear relationship between variables.
   
   ## Covariance
   
   The covariance between two arrays X and Y is defined as:
   
   cov(X, Y) = Σ((X - μ_X) × (Y - μ_Y)) / N
   
   Where:
   - μ_X = mean of X
   - μ_Y = mean of Y
   - N = number of elements (or N-1 for sample covariance)
   
   ## Bias Types
   
   - **AF_VARIANCE_POPULATION** (N): Population covariance (biased)
   - **AF_VARIANCE_SAMPLE** (N-1): Sample covariance (unbiased, Bessel's correction)
   
   ## Interpretation
   
   - **cov > 0**: Positive relationship (both tend to increase together)
   - **cov = 0**: No linear relationship
   - **cov < 0**: Negative relationship (one increases as other decreases)
   - **Magnitude**: Not normalized, depends on variable scales
   
   ## Relationship to Correlation
   
   Covariance is the unnormalized version of correlation coefficient:
   
   corr(X, Y) = cov(X, Y) / (σ_X × σ_Y)
   
   Where σ_X and σ_Y are the standard deviations of X and Y.
   
   ## Properties
   
   - **Symmetric**: cov(X, Y) = cov(Y, X)
   - **Scale dependent**: cov(aX, bY) = ab × cov(X, Y)
   - **Units**: Product of X and Y units
   - **Range**: (-∞, +∞) unbounded
   
   ## Use Cases
   
   - **Portfolio theory**: Measure asset return relationships
   - **Signal processing**: Analyze signal dependencies
   - **Statistics**: Variance-covariance matrices
   - **Machine learning**: Feature covariance analysis
   - **Quality control**: Process variable relationships
   - **Physics**: Uncertainty analysis
   
   ## Limitations
   
   - Only measures linear relationships
   - Scale dependent (not normalized)
   - Sensitive to outliers
   - Requires continuous variables
   
   ## Related Functions
   
   - **Correlation**: af_corrcoef (normalized covariance)
   - **Mean**: af_mean (used internally)
   - **Variance**: af_var (covariance with itself)
   - **Standard deviation**: af_stdev (square root of variance)"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; af_err af_cov(af_array *out, const af_array X, const af_array Y, const bool isbiased)
(defcfn af-cov
  "Compute covariance between two arrays (deprecated, use af-cov-v2).
   
   Calculates the covariance between two arrays of the same shape along the first dimension.
   The result is an array of covariances for each column.
   
   Parameters:
   - out: out pointer to array for covariance results
   - X: first input array
   - Y: second input array
   - isbiased: boolean (0=sample/unbiased with N-1, 1=population/biased with N)
   
   Formula:
   cov(X, Y) = Σ((X - μ_X) × (Y - μ_Y)) / N
   
   Where:
   - μ_X = mean(X), μ_Y = mean(Y)
   - N = number of elements (first dimension size)
   - If isbiased=false: N = N-1 (Bessel's correction for sample covariance)
   
   Algorithm:
   1. Compute means: μ_X = mean(X, dim=0), μ_Y = mean(Y, dim=0)
   2. Compute deviations: diffX = X - μ_X, diffY = Y - μ_Y
   3. Compute products: mulXY = diffX × diffY
   4. Sum products: Σ(mulXY) along first dimension
   5. Normalize: result = Σ(mulXY) / N
   
   Constraints:
   - X and Y must have the same dimensions
   - X and Y must have the same data type
   - Arrays must be 1D or 2D (vectors or matrices)
   - Minimum 2 elements in first dimension for meaningful covariance
   
   Dimensions:
   - Vector input (N×1): Returns scalar covariance
   - Matrix input (N×M): Returns 1×M array of covariances (one per column)
   
   Type handling:
   - Input types: All ArrayFire types except complex
   - Integer types automatically converted to float/double
   - f32/s32/u32/s16/u16/s8/u8/b8 → float computation
   - f64/s64/u64 → double computation
   - Output type matches computation type (float or double)
   - Complex types: Not supported (will throw error)
   
   Bias selection:
   - **isbiased=false (recommended)**: Sample covariance with N-1 (unbiased estimator)
     * Use when X and Y are samples from larger populations
     * Corrects for degrees of freedom (Bessel's correction)
     * More conservative estimate
   - **isbiased=true**: Population covariance with N (biased estimator)
     * Use when X and Y represent entire populations
     * Maximum likelihood estimator
     * Slightly underestimates true population covariance
   
   Special cases:
   - Constant arrays: cov = 0 (no variability)
   - Identical arrays: cov = variance (covariance with itself)
   - Perfectly opposite: cov = -variance
   
   Interpretation examples:
   
   **Positive covariance (cov > 0)**:
   X = [1, 2, 3, 4, 5]
   Y = [2, 4, 5, 8, 10]  (increases with X)
   Result: cov > 0 indicating positive relationship
   
   **Negative covariance (cov < 0)**:
   X = [1, 2, 3, 4, 5]
   Y = [10, 8, 6, 4, 2]  (decreases with X)
   Result: cov < 0 indicating negative relationship
   
   **Zero covariance (cov ≈ 0)**:
   X = [1, 2, 3, 4, 5]
   Y = [3, 1, 4, 1, 5]  (no linear pattern)
   Result: cov ≈ 0 indicating no linear relationship
   
   **Large magnitude covariance**:
   X = [100, 200, 300, 400, 500]
   Y = [1000, 2000, 3000, 4000, 5000]
   Result: Large cov due to scale (normalize with correlation for scale-free measure)
   
   Use cases:
   
   **Portfolio analysis**:
   - Asset return covariance: Measure diversification potential
   - Risk assessment: Identify correlated vs independent assets
   - Variance-covariance matrices: Portfolio optimization
   
   **Signal processing**:
   - Cross-covariance: Measure signal dependencies
   - Time series analysis: Lagged relationships
   - Noise analysis: Signal/noise covariance structure
   
   **Statistics**:
   - Multivariate analysis: Covariance matrices for PCA, LDA
   - Regression: Predictor covariance for multicollinearity
   - ANOVA: Within/between group covariance
   
   **Machine learning**:
   - Feature analysis: Identify redundant features
   - Gaussian processes: Kernel covariance functions
   - Bayesian inference: Prior/posterior covariances
   
   **Quality control**:
   - Process monitoring: Variable relationship stability
   - Measurement systems: Instrument correlation
   - Six Sigma: Variation source identification
   
   **Physics/Engineering**:
   - Uncertainty analysis: Error propagation
   - System identification: Input/output covariance
   - Kalman filtering: State covariance updates
   
   Sample vs population covariance:
   When analyzing data, choose bias based on context:
   - **Sample (isbiased=false)**: Data represents subset of larger population
     Example: Monthly returns from 5 years to estimate future covariance
   - **Population (isbiased=true)**: Data is complete population
     Example: Covariance of all trades within a specific day (the day is the population)
   
   Example workflow:
   1. Collect paired observations in arrays X and Y
   2. Ensure X and Y have same dimensions and types
   3. Choose bias type based on sample vs population
   4. Compute covariance with af_cov
   5. Interpret sign (direction) and magnitude (strength)
   6. For scale-free measure, compute correlation instead
   
   Performance notes:
   - Single pass through data (efficient O(n) algorithm)
   - All operations vectorized on GPU
   - Mean calculation optimized per backend
   - Reduction operations use optimized kernels
   
   Common pitfalls:
   - **Scale confusion**: Large covariance doesn't necessarily mean strong relationship
     * Solution: Use correlation coefficient for normalized measure
   - **Outliers**: Single extreme values can dominate covariance
     * Solution: Robust alternatives or outlier removal
   - **Sample size**: Small samples yield unreliable covariance estimates
     * Solution: Use sample covariance (isbiased=false) for better properties
   - **Non-linear relationships**: Covariance only measures linear association
     * Solution: Consider non-linear correlation measures or scatter plots
   - **Complex numbers**: Not supported, will throw error
     * Solution: Compute covariance of real and imaginary parts separately
   
   Variance-covariance matrix:
   For multiple variables, compute full covariance matrix:
   - Diagonal: Variances var(X_i) = cov(X_i, X_i)
   - Off-diagonal: Covariances cov(X_i, X_j)
   - Symmetric: cov(X_i, X_j) = cov(X_j, X_i)
   - Positive semi-definite: All eigenvalues ≥ 0
   
   See also:
   - af_corrcoef: Correlation coefficient (normalized covariance)
   - af_var: Variance (covariance with itself)
   - af_stdev: Standard deviation (for normalization)
   - af_mean: Mean (used internally)
   - af_cov_v2: Version with explicit bias enum (recommended)
   
   Returns:
   ArrayFire error code
   
   Note: This function is deprecated. Use af_cov_v2 which takes an explicit
   af_var_bias enum parameter instead of a boolean for better clarity."
  "af_cov" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_cov_v2(af_array *out, const af_array X, const af_array Y, const af_var_bias bias)
(defcfn af-cov-v2
  "Compute covariance between two arrays with explicit bias parameter.
   
   Calculates the covariance between two arrays of the same shape along the first dimension.
   The result is an array of covariances for each column. This is the recommended version
   that uses an explicit bias enum for clarity.
   
   Parameters:
   - out: out pointer to array for covariance results
   - X: first input array
   - Y: second input array
   - bias: af_var_bias enum (AF_VARIANCE_SAMPLE=0 or AF_VARIANCE_POPULATION=1)
   
   Formula:
   cov(X, Y) = Σ((X - μ_X) × (Y - μ_Y)) / N
   
   Where:
   - μ_X = mean(X, dim=0), μ_Y = mean(Y, dim=0)
   - N = number of elements (first dimension size)
   - If bias=AF_VARIANCE_SAMPLE: N = N-1 (Bessel's correction)
   
   Algorithm:
   1. Compute means: μ_X = mean(X, dim=0), μ_Y = mean(Y, dim=0)
   2. Broadcast means to input dimensions
   3. Compute deviations: diffX = X - μ_X, diffY = Y - μ_Y
   4. Compute element-wise products: mulXY = diffX × diffY
   5. Reduce along first dimension: sum(mulXY, dim=0)
   6. Normalize by N (or N-1): result / N
   
   Constraints:
   - X and Y must have the same dimensions
   - X and Y must have the same data type
   - Arrays must be 1D or 2D (vectors or matrices)
   - Minimum 2 elements in first dimension required
   
   Dimensions:
   - Vector input (N×1): Returns scalar covariance
   - Matrix input (N×M): Returns 1×M array of covariances (one per column)
   - First dimension: Observations (reduced)
   - Second dimension: Variables (preserved)
   
   Type handling:
   - Input types: All ArrayFire types except complex
   - Integer types automatically converted to float/double
   - f32/s32/u32/s16/u16/s8/u8 → float computation → float output
   - f64/s64/u64 → double computation → double output
   - Complex types: Not supported, will throw TYPE_ERROR
   
   Bias parameter (af_var_bias enum):
   - **AF_VARIANCE_SAMPLE = 0** (recommended for samples):
     * Uses N-1 in denominator (Bessel's correction)
     * Unbiased estimator of population covariance
     * Better for inferential statistics
     * Standard choice in most statistical software
   - **AF_VARIANCE_POPULATION = 1** (for complete populations):
     * Uses N in denominator
     * Biased but consistent estimator
     * Maximum likelihood estimator
     * Use when data represents entire population
   
   Mathematical properties:
   - **Symmetry**: cov(X, Y) = cov(Y, X)
   - **Linearity**: cov(aX, bY) = ab × cov(X, Y)
   - **Bilinearity**: cov(X₁+X₂, Y) = cov(X₁, Y) + cov(X₂, Y)
   - **Variance relation**: var(X) = cov(X, X)
   - **Cauchy-Schwarz**: cov(X,Y)² ≤ var(X) × var(Y)
   
   Special cases:
   - **X = Y**: Returns variance var(X)
   - **Constant X or Y**: Returns 0 (no variability)
   - **Perfect linear relation**: |cov| = σ_X × σ_Y
   - **Independent variables**: E[cov] = 0 (but sample cov may be non-zero)
   
   Interpretation guidelines:
   
   **Sign interpretation**:
   - Positive: Variables tend to increase/decrease together
   - Zero: No linear relationship (may still have non-linear)
   - Negative: Variables move in opposite directions
   
   **Magnitude interpretation**:
   - Scale dependent: Cannot compare across different variable scales
   - Units: Product of X and Y units
   - Range: Unbounded (-∞, +∞)
   - Normalize with correlation for scale-free interpretation
   
   Use cases:
   
   **Financial analysis**:
   - Portfolio variance: Combine asset covariances
   - Risk models: Multi-factor covariance matrices
   - Hedging: Identify offsetting positions
   - Diversification: Low covariance = better diversification
   
   **Signal processing**:
   - Cross-covariance functions: Signal relationships at different lags
   - Adaptive filtering: Wiener filter covariance matrices
   - Source separation: Independent component analysis
   - Noise characterization: Signal/noise covariance
   
   **Multivariate statistics**:
   - Principal component analysis (PCA): Eigendecomposition of covariance matrix
   - Linear discriminant analysis (LDA): Within/between class covariances
   - Factor analysis: Shared covariance structure
   - Canonical correlation: Maximum covariance projections
   
   **Machine learning**:
   - Feature selection: Remove high-covariance features
   - Gaussian processes: Covariance kernel functions
   - Bayesian methods: Prior/posterior covariances
   - Ensemble methods: Predictor diversity via low covariance
   
   **Quality control**:
   - Multivariate control charts: Covariance matrix monitoring
   - Measurement system analysis: Gage covariance
   - Process capability: Multivariate process variation
   - Design of experiments: Response covariance analysis
   
   **Physics and engineering**:
   - Uncertainty propagation: Error covariance matrices
   - Kalman filtering: State estimation covariance updates
   - System identification: Input-output covariance
   - Structural analysis: Load covariance in reliability
   
   Example workflow:
   1. Prepare data: Ensure X and Y have same shape, type
   2. Center if needed: Or rely on internal mean computation
   3. Choose bias:
      - AF_VARIANCE_SAMPLE for generalizing to population
      - AF_VARIANCE_POPULATION for descriptive statistics
   4. Call af_cov_v2(out, X, Y, bias)
   5. Interpret results:
      - Sign indicates direction of relationship
      - Compare with variances: cov / (σ_X × σ_Y) gives correlation
   6. Build covariance matrix if multiple variables
   
   Performance considerations:
   - O(N×M) complexity for N×M matrices
   - Single pass through data (efficient)
   - GPU-optimized reductions
   - Memory: Intermediate arrays for deviations
   - Batch processing: Process multiple column pairs in parallel
   
   Common pitfalls and solutions:
   
   **Scale dependency**:
   - Problem: Covariance magnitude depends on variable scales
   - Solution: Use correlation coefficient (af_corrcoef) for normalized measure
   - Example: Stock prices ($) vs returns (%) have incomparable covariances
   
   **Outlier sensitivity**:
   - Problem: Extreme values can dominate covariance
   - Solution: Robust covariance estimators or outlier removal
   - Example: Single abnormal data point can flip covariance sign
   
   **Small sample size**:
   - Problem: High variance in covariance estimate
   - Solution: Use AF_VARIANCE_SAMPLE, collect more data, or shrinkage estimators
   - Example: N=3 observations yield unreliable covariance
   
   **Non-linear relationships**:
   - Problem: Covariance = 0 doesn't imply independence
   - Solution: Use scatter plots, non-linear correlation measures
   - Example: Quadratic relationship can have zero covariance
   
   **Interpretation confusion**:
   - Problem: Covariance magnitude hard to interpret
   - Solution: Normalize to [-1, +1] scale via correlation
   - Example: cov=1000 might be weak (large variances) or strong (small variances)
   
   Statistical testing:
   To test if covariance is significantly different from zero:
   - Use t-test based on correlation coefficient
   - Transform: t = r × √(n-2) / √(1-r²) where r = cov/(σ_X × σ_Y)
   - Compare with t-distribution (n-2 degrees of freedom)
   
   Relationship to other measures:
   - **Correlation**: ρ(X,Y) = cov(X,Y) / (σ_X × σ_Y) ∈ [-1, +1]
   - **Variance**: var(X) = cov(X, X)
   - **Standard deviation**: σ_X = √var(X) = √cov(X, X)
   - **Coefficient of variation**: CV = σ / μ (normalized variance)
   
   Covariance matrix properties:
   When building full covariance matrix Σ:
   - Symmetric: Σᵢⱼ = Σⱼᵢ
   - Positive semi-definite: xᵀΣx ≥ 0 for all x
   - Diagonal: Variances var(Xᵢ)
   - Off-diagonal: Covariances cov(Xᵢ, Xⱼ)
   - Determinant: Generalized variance
   - Eigenvalues: All non-negative
   
   See also:
   - af_corrcoef: Pearson correlation coefficient (normalized covariance)
   - af_var: Variance (covariance with itself)
   - af_var_v2: Variance with explicit bias parameter
   - af_stdev: Standard deviation
   - af_stdev_v2: Standard deviation with explicit bias parameter
   - af_mean: Mean (used internally)
   - af_cov: Deprecated version with boolean bias parameter
   
   Returns:
   ArrayFire error code
   
   Note: This is the recommended version over af_cov. The explicit af_var_bias enum
   parameter makes the bias choice clearer than a boolean flag."
  "af_cov_v2" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)
