(ns org.soulspace.arrayfire.ffi.c-api.corrcoef
  "Bindings for the ArrayFire correlation coefficient function.
   
   Correlation coefficient measures the linear relationship between two arrays.
   This implementation computes the Pearson product-moment correlation coefficient,
   which is one of the most common measures of correlation.
   
   ## Pearson Correlation Coefficient
   
   The Pearson correlation coefficient r is defined as:
   
   r(X, Y) = (n×Σ(xy) - Σx×Σy) / (√(n×Σ(x²) - (Σx)²) × √(n×Σ(y²) - (Σy)²))
   
   Where:
   - n = number of elements
   - Σ(xy) = sum of element-wise products
   - Σx, Σy = sums of elements
   - Σ(x²), Σ(y²) = sums of squared elements
   
   ## Interpretation
   
   The correlation coefficient r ranges from -1 to +1:
   - **r = +1**: Perfect positive linear correlation
   - **r = 0**: No linear correlation
   - **r = -1**: Perfect negative linear correlation
   - **|r| > 0.8**: Strong correlation
   - **0.5 < |r| < 0.8**: Moderate correlation
   - **0.3 < |r| < 0.5**: Weak correlation
   - **|r| < 0.3**: Very weak or no correlation
   
   ## Properties
   
   - **Symmetric**: r(X, Y) = r(Y, X)
   - **Dimensionless**: No units, always between -1 and +1
   - **Scale invariant**: r(aX + b, cY + d) = r(X, Y) for a,c > 0
   - **Measures linear relationship only**: May miss non-linear patterns
   
   ## Statistical Significance
   
   For sample size n, the t-statistic for testing H₀: ρ = 0 is:
   t = r × √(n-2) / √(1-r²)
   
   which follows a t-distribution with n-2 degrees of freedom.
   
   ## Use Cases
   
   - **Signal processing**: Measure similarity between signals
   - **Finance**: Analyze asset return correlations
   - **Statistics**: Test for linear relationships
   - **Quality control**: Validate measurement consistency
   - **Image processing**: Compare image patches
   - **Machine learning**: Feature selection, detect multicollinearity
   
   ## Limitations
   
   - Only measures linear relationships (Spearman/Kendall for non-linear)
   - Sensitive to outliers
   - Zero correlation does not imply independence
   - Assumes continuous variables
   
   ## Related Functions
   
   - **Covariance**: af_cov (unnormalized correlation)
   - **Mean**: af_mean (used internally)
   - **Standard deviation**: af_stdev (used internally)"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; af_err af_corrcoef(double *realVal, double *imagVal, const af_array X, const af_array Y)
(defcfn af-corrcoef
  "Compute Pearson product-moment correlation coefficient.
   
   Calculates the correlation coefficient between two arrays of the same shape.
   The result is a single scalar value representing the strength and direction
   of the linear relationship between X and Y.
   
   Parameters:
   - realVal: out pointer to double for the real part of correlation coefficient
   - imagVal: out pointer to double for the imaginary part (currently unused for real inputs)
   - X: first input array
   - Y: second input array
   
   Formula:
   r = (n×Σ(xy) - Σx×Σy) / (√(n×Σ(x²) - (Σx)²) × √(n×Σ(y²) - (Σy)²))
   
   Algorithm:
   1. Compute element-wise products: xy = X × Y
   2. Compute squares: x² = X × X, y² = Y × Y
   3. Sum all arrays: Σx, Σy, Σ(xy), Σ(x²), Σ(y²)
   4. Apply Pearson formula
   5. Return scalar correlation coefficient
   
   Constraints:
   - X and Y must have identical dimensions
   - X and Y must have the same data type
   - Arrays can be any dimensionality (1D, 2D, 3D, etc.)
   - Minimum 2 elements required for meaningful correlation
   
   Type handling:
   - Input types: All ArrayFire types (f32, f64, s32, u32, etc.)
   - Integer types automatically converted to float/double for computation
   - f32/s32/u32/s16/u16/s8/u8/b8 → float computation
   - f64/s64/u64 → double computation
   - Output: Always double precision
   
   Special cases:
   - Constant arrays: r undefined (division by zero in denominator)
   - Identical arrays: r = 1.0
   - Opposite arrays: r = -1.0
   - Complex inputs: Currently only real part is computed
   
   Interpretation examples:
   
   **Perfect positive correlation (r = 1.0)**:
   X = [1, 2, 3, 4, 5]
   Y = [2, 4, 6, 8, 10]  (Y = 2×X)
   
   **Perfect negative correlation (r = -1.0)**:
   X = [1, 2, 3, 4, 5]
   Y = [10, 8, 6, 4, 2]  (Y decreases as X increases)
   
   **No correlation (r ≈ 0)**:
   X = [1, 2, 3, 4, 5]
   Y = [3, 1, 4, 1, 5]  (random pattern)
   
   **Strong positive correlation (r ≈ 0.9)**:
   X = [1, 2, 3, 4, 5]
   Y = [2.1, 3.9, 6.2, 7.8, 10.1]  (nearly linear with noise)
   
   Use cases:
   
   **Financial analysis**:
   - Portfolio diversification: Check asset correlations
   - Risk management: Identify correlated risks
   - Pairs trading: Find cointegrated securities
   
   **Signal processing**:
   - Signal similarity: Compare time series
   - Template matching: Correlate signal with template
   - Synchronization: Detect time-aligned signals
   
   **Machine learning**:
   - Feature selection: Remove highly correlated features
   - Multicollinearity detection: Identify redundant predictors
   - Data validation: Verify consistency across measurements
   
   **Image processing**:
   - Patch comparison: Measure similarity between image regions
   - Registration: Align images based on correlation
   - Quality assessment: Compare original vs processed images
   
   **Quality control**:
   - Measurement validation: Compare different instruments
   - Process monitoring: Track variable relationships
   - Calibration: Verify sensor accuracy
   
   **Scientific research**:
   - Variable relationships: Test hypotheses about associations
   - Reproducibility: Compare experimental replicates
   - Model validation: Check predicted vs observed values
   
   Statistical testing:
   To test if correlation is significantly different from zero:
   - Compute t-statistic: t = r × √(n-2) / √(1-r²)
   - Compare with t-distribution (n-2 degrees of freedom)
   - Critical values at α=0.05: roughly ±2 for n>30
   
   Example workflow:
   1. Load two datasets of equal size
   2. Optionally normalize or standardize data
   3. Compute correlation with af_corrcoef
   4. Interpret r value (strength and direction)
   5. Calculate statistical significance if needed
   6. Make decisions based on correlation strength
   
   Performance notes:
   - Single pass through data (efficient O(n) algorithm)
   - All operations vectorized on GPU
   - Reduction operations optimized per backend
   - Small overhead for type conversion if needed
   
   Common pitfalls:
   - **Outliers**: Single extreme values can dominate correlation
   - **Non-linear relationships**: Pearson only detects linear patterns
   - **Sample size**: Small samples may show spurious correlation
   - **Causation**: Correlation ≠ causation, always consider context
   - **Range restriction**: Limited data range reduces correlation
   
   Alternatives for specific cases:
   - Non-linear relationships: Use Spearman or Kendall correlation
   - Outlier-prone data: Use robust correlation measures
   - Categorical data: Use chi-square or Cramér's V
   - Time series: Consider autocorrelation or cross-correlation
   
   See also:
   - af_cov: Covariance (unnormalized correlation)
   - af_mean: Mean calculation
   - af_stdev: Standard deviation
   - af_dot: Dot product (related to correlation numerator)
   
   Returns:
   ArrayFire error code
   
   Note: Currently only real correlation is computed. Complex correlation
   (imagVal) is not yet implemented and will be zero."
  "af_corrcoef" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)
