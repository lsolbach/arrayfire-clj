(ns org.soulspace.arrayfire.ffi.c-api.deconvolution
  "Bindings for the ArrayFire deconvolution functions.
   
   Deconvolution is the inverse operation of convolution, used to restore
   blurred images when the blur kernel (point spread function) is known.
   
   ## Deconvolution Problem
   
   Given:
   - Blurred image: g = f ⊗ h + n
   - Known blur kernel (PSF): h
   - Noise: n
   
   Goal: Recover sharp image f from blurred observation g
   
   ## Approaches
   
   **Iterative Methods** (af_iterative_deconv):
   - Richardson-Lucy: Maximum likelihood for Poisson noise
   - Landweber: Gradient descent with relaxation
   
   **Inverse Methods** (af_inverse_deconv):
   - Tikhonov: Regularized least squares
   
   ## Applications
   
   - **Astronomy**: Telescope image enhancement
   - **Microscopy**: Biological imaging deblurring
   - **Medical imaging**: MRI/CT sharpening
   - **Photography**: Motion blur removal
   - **Surveillance**: License plate recovery
   - **Machine learning**: Image preprocessing
   
   ## Key Concepts
   
   - **Point Spread Function (PSF)**: Blur kernel modeling optical system
   - **Regularization**: Prevent noise amplification
   - **Iterations**: Trade-off between sharpness and noise
   - **Ill-posed problem**: Small errors in data cause large errors in solution"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; af_err af_iterative_deconv(af_array *out, const af_array in, const af_array ker,
;;                            const unsigned iterations, const float relax_factor,
;;                            const af_iterative_deconv_algo algo)
(defcfn af-iterative-deconv
  "Perform iterative image deconvolution.
   
   Recovers a sharp image estimate from a blurred input when the blur kernel
   (point spread function) is known. Uses iterative algorithms that refine
   the estimate through multiple passes.
   
   Parameters:
   - out: out pointer to array for deconvolved (sharp) image
   - in: blurred input image (2D array)
   - ker: blur kernel/point spread function (PSF) (2D array)
   - iterations: number of algorithm iterations (unsigned int)
   - relax_factor: relaxation parameter (float, ignored for Richardson-Lucy)
   - algo: algorithm type (af_iterative_deconv_algo enum)
   
   Algorithms (af_iterative_deconv_algo enum):
   - **AF_ITERATIVE_DECONV_DEFAULT** (0): Default algorithm (Landweber)
   - **AF_ITERATIVE_DECONV_LANDWEBER** (1): Landweber iteration
     * Gradient descent with relaxation factor
     * Formula: f^(k+1) = f^k + α(g - h⊗f^k)⊗h*
     * relax_factor (α) controls step size (0 < α < 2)
     * Stable, converges slowly
     * Good for general deblurring
   - **AF_ITERATIVE_DECONV_RICHARDSONLUCY** (2): Richardson-Lucy iteration
     * Maximum likelihood for Poisson noise
     * Formula: f^(k+1) = f^k × [(g / (h⊗f^k)) ⊗ h*]
     * relax_factor parameter ignored (not used)
     * Non-negative estimates (preserves physical meaning)
     * Ideal for low-light imaging (photon counting)
   
   Mathematical formulation:
   
   **Convolution model**:
   g = h ⊗ f + n
   
   Where:
   - g = observed blurred image
   - h = point spread function (PSF) / blur kernel
   - f = original sharp image (to recover)
   - n = additive noise
   - ⊗ = convolution operator
   
   **Landweber iteration**:
   f^(k+1) = f^k + α × h* ⊗ (g - h⊗f^k)
   
   Where:
   - f^k = estimate at iteration k
   - α = relaxation factor (step size)
   - h* = conjugate/flipped kernel
   - (g - h⊗f^k) = residual error
   
   **Richardson-Lucy iteration**:
   f^(k+1) = f^k × [h* ⊗ (g / (h⊗f^k))]
   
   Where:
   - Division and multiplication are element-wise
   - Ratio (g / (h⊗f^k)) measures observation vs prediction
   - Multiplication preserves positivity
   
   Constraints:
   - Input image must be 2D (single grayscale or one color channel)
   - Kernel must be 2D
   - iterations > 0
   - relax_factor > 0 and finite (for Landweber)
   - algo must be valid enum value
   
   Type handling:
   - Input types: f32, s16, u16, s8, u8
   - Computation always in float or double precision
   - Output type matches computation precision
   - Integer inputs converted to float
   
   Parameter selection:
   
   **iterations**:
   - More iterations: Sharper but more noise
   - Fewer iterations: Smoother but less sharp
   - Typical: 10-100 iterations
   - Monitor: Stop when residual stops decreasing
   - Landweber: 50-200 iterations typical
   - Richardson-Lucy: 10-50 iterations typical
   
   **relax_factor (Landweber only)**:
   - Range: 0 < α < 2 (theory), practical: 0.01-0.5
   - Larger α: Faster convergence, risk of instability
   - Smaller α: Slower but more stable
   - Typical: 0.05 (default in examples)
   - Too large: Oscillations, divergence
   - Too small: Very slow convergence
   
   **Algorithm choice**:
   - **Landweber**: General purpose, stable, predictable
     * Use for: Gaussian blur, motion blur, general deblurring
     * Pros: Stable, easy parameter tuning
     * Cons: Slower convergence, can produce negative values
   - **Richardson-Lucy**: Optimal for Poisson noise
     * Use for: Astronomy, microscopy, low-light imaging
     * Pros: Physical validity (non-negative), faster convergence
     * Cons: Can amplify high-frequency noise
   
   Typical workflows:
   
   **Photography motion blur removal**:
   1. Estimate motion PSF (direction and extent)
   2. Create Gaussian or motion blur kernel
   3. Apply Landweber: 50 iterations, relax_factor=0.05
   4. Evaluate: Check edges for artifacts
   5. Adjust: Increase iterations for sharper, decrease for smoother
   
   **Astronomy telescope deblurring**:
   1. Measure telescope PSF (calibration star)
   2. Apply Richardson-Lucy: 20 iterations
   3. Monitor: Check flux conservation (sum of pixels)
   4. Stop early if noise starts dominating
   5. Optional: Apply Wiener filter post-processing
   
   **Microscopy fluorescence imaging**:
   1. Acquire PSF from calibration bead
   2. Apply Richardson-Lucy: 10-30 iterations
   3. Preserve: Non-negativity crucial for intensity
   4. Validate: Check biological structure makes sense
   5. Compare: Original vs deconvolved for improvements
   
   **General deblurring pipeline**:
   1. Estimate or measure PSF
   2. Normalize PSF (sum to 1)
   3. Choose algorithm based on noise type
   4. Start with conservative parameters
   5. Increase iterations until quality plateaus
   6. Watch for noise amplification
   7. Consider: Regularization if noise is severe
   
   Performance considerations:
   - Each iteration requires 2 FFTs (forward and inverse)
   - Complexity: O(iterations × N log N) for N-pixel image
   - Memory: Multiple image-sized arrays
   - GPU acceleration: Significant speedup
   - Batch processing: Can process multiple images
   
   Common issues and solutions:
   
   **Ringing artifacts (Gibbs phenomenon)**:
   - Cause: Sharp edges in PSF, too many iterations
   - Solution: Apply windowing to PSF, reduce iterations
   - Alternative: Use regularization
   
   **Noise amplification**:
   - Cause: Deconvolution amplifies high frequencies
   - Solution: Stop iterations earlier, denoise first
   - Alternative: Add Tikhonov regularization
   
   **Slow convergence (Landweber)**:
   - Cause: Relax factor too small
   - Solution: Increase relax_factor (e.g., 0.05 → 0.2)
   - Check: Ensure PSF is normalized
   
   **Negative values (Landweber)**:
   - Cause: Algorithm doesn't enforce positivity
   - Solution: Use Richardson-Lucy instead
   - Workaround: Clamp negative values to zero
   
   **Over-sharpening**:
   - Cause: Too many iterations
   - Solution: Reduce iterations
   - Monitor: Visual quality vs iteration number
   
   Edge effects:
   - FFT assumes periodic boundaries
   - Can cause artifacts at image edges
   - Mitigation: Pad image with mirrored boundaries
   
   Validation techniques:
   - Compare original PSF with (deconvolved ⊗ PSF)
   - Check residual: Should approach noise level
   - Visual inspection: Edges should be sharp not artifacted
   - Quantitative: Peak signal-to-noise ratio (PSNR)
   
   Related preprocessing:
   - Denoise input before deconvolution
   - Normalize image intensities
   - Estimate noise variance
   - Apply flat-field correction (microscopy)
   
   See also:
   - af_inverse_deconv: Inverse/regularized deconvolution (Tikhonov)
   - af_fft_convolve2: FFT-based convolution (validation)
   - af_convolve2: Spatial convolution (small kernels)
   - gaussian_kernel: Generate Gaussian PSF
   
   Returns:
   ArrayFire error code
   
   Example:
   ```clojure
   ;; Motion blur removal (Landweber)
   (af-iterative-deconv out-ptr blurred-image motion-kernel 
                        50    ; 50 iterations
                        0.05  ; relaxation factor
                        1)    ; AF_ITERATIVE_DECONV_LANDWEBER
   
   ;; Astronomy deblurring (Richardson-Lucy)
   (af-iterative-deconv out-ptr telescope-image psf
                        20     ; 20 iterations
                        0.0    ; ignored for Richardson-Lucy
                        2)     ; AF_ITERATIVE_DECONV_RICHARDSONLUCY
   ```"
  "af_iterative_deconv" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/float ::mem/int] ::mem/int)

;; af_err af_inverse_deconv(af_array *out, const af_array in, const af_array psf,
;;                          const float gamma, const af_inverse_deconv_algo algo)
(defcfn af-inverse-deconv
  "Perform inverse/regularized image deconvolution.
   
   Recovers a sharp image estimate from a blurred input using direct inverse
   methods with regularization to handle noise and ill-conditioning. Single-pass
   algorithms that are faster than iterative methods but may be less flexible.
   
   Parameters:
   - out: out pointer to array for deconvolved (sharp) image
   - in: blurred input image (2D array)
   - psf: point spread function (blur kernel) (2D array)
   - gamma: regularization parameter (float > 0)
   - algo: algorithm type (af_inverse_deconv_algo enum)
   
   Algorithms (af_inverse_deconv_algo enum):
   - **AF_INVERSE_DECONV_DEFAULT** (0): Default algorithm (Tikhonov)
   - **AF_INVERSE_DECONV_TIKHONOV** (1): Tikhonov regularization
     * Regularized least squares
     * Formula: F = (H* × G) / (|H|² + γ)
     * gamma controls noise suppression
     * Closed-form solution (one pass)
     * Optimal for Gaussian noise
   
   Mathematical formulation:
   
   **Frequency domain representation**:
   G(ω) = H(ω) × F(ω) + N(ω)
   
   Where (capital = Fourier transform):
   - G = observed blurred image spectrum
   - H = point spread function spectrum
   - F = original sharp image spectrum (to recover)
   - N = noise spectrum
   - ω = spatial frequency
   
   **Naive inverse (unstable)**:
   F̂(ω) = G(ω) / H(ω)
   Problem: Division by small H(ω) amplifies noise catastrophically
   
   **Tikhonov regularization (stable)**:
   F̂(ω) = [H*(ω) × G(ω)] / [|H(ω)|² + γ]
   
   Where:
   - H* = complex conjugate of H
   - |H|² = H* × H (power spectrum)
   - γ = regularization parameter (prevents division by zero)
   - Larger γ: More smoothing, less noise amplification
   - Smaller γ: Closer to true inverse, more noise
   
   Regularization interpretation:
   - γ acts as a frequency-dependent filter
   - Low frequencies (|H|² >> γ): Nearly perfect recovery
   - High frequencies (|H|² ≈ γ): Suppressed (noise reduction)
   - Very high frequencies (|H|² << γ): Heavily attenuated
   
   Constraints:
   - Input image must be 2D
   - PSF must be 2D
   - gamma > 0 and finite
   - algo must be valid enum value
   
   Type handling:
   - Input types: f32, s16, u16, s8, u8
   - Computation in float or double precision
   - Complex FFT used internally
   - Output type matches computation precision
   
   Parameter selection:
   
   **gamma (regularization parameter)**:
   - Critical parameter controlling quality
   - Range: Typically 0.001 to 1.0
   - Optimal value depends on SNR (signal-to-noise ratio)
   
   **Heuristics for gamma**:
   - High SNR (clean image): γ = 0.001 - 0.01 (minimal regularization)
   - Medium SNR (some noise): γ = 0.01 - 0.1 (balanced)
   - Low SNR (noisy image): γ = 0.1 - 1.0 (heavy smoothing)
   - Very noisy: γ > 1.0 (primarily smoothing)
   
   **Automatic gamma estimation**:
   1. Estimate noise variance σ²_n from image
   2. Estimate signal variance σ²_s from image
   3. γ ≈ σ²_n / σ²_s (Wiener filter principle)
   4. Adjust empirically based on visual quality
   
   **Algorithm choice**:
   - **Tikhonov (only option currently)**:
     * Use for: Any deblurring with known PSF
     * Pros: Fast (single pass), stable, predictable
     * Cons: Uniform regularization (doesn't preserve edges well)
   
   Comparison with iterative methods:
   
   **Inverse (Tikhonov) advantages**:
   - Much faster: O(N log N) vs O(iterations × N log N)
   - Single parameter (gamma) vs (iterations, relax_factor)
   - Deterministic: Same gamma always gives same result
   - Stable: No convergence issues
   
   **Inverse (Tikhonov) disadvantages**:
   - Less flexible: Can't incorporate constraints (non-negativity)
   - Uniform regularization: Treats all regions equally
   - Edge preservation: May over-smooth edges
   - Limited noise models: Assumes Gaussian noise
   
   **Iterative (Richardson-Lucy, Landweber) advantages**:
   - Better edge preservation
   - Can enforce positivity (Richardson-Lucy)
   - Adaptive: Different regions converge at different rates
   - Better for Poisson noise (Richardson-Lucy)
   
   **When to use Tikhonov**:
   - Speed is critical
   - Noise is approximately Gaussian
   - Uniform blur across image
   - Quick preview/initial estimate
   
   **When to use iterative**:
   - Quality is critical
   - Poisson/photon noise (microscopy, astronomy)
   - Non-uniform blur
   - Need physical constraints (non-negativity)
   
   Typical workflows:
   
   **Quick deblurring (Tikhonov)**:
   1. Estimate or measure PSF
   2. Normalize PSF (sum to 1)
   3. Start with γ = 0.05
   4. Apply af_inverse_deconv
   5. Adjust γ based on result:
      - Too noisy: Increase γ
      - Too smooth: Decrease γ
   6. 2-3 tries typically sufficient
   
   **Combining methods (hybrid approach)**:
   1. Quick Tikhonov pass: γ = 0.1 (conservative)
   2. Use as initialization for Richardson-Lucy
   3. Run 5-10 Richardson-Lucy iterations
   4. Benefits: Speed + quality
   
   **Batch processing**:
   1. Process sample image, find optimal γ
   2. Apply same γ to entire batch
   3. Assumes consistent noise/blur characteristics
   
   **Adaptive parameter selection**:
   1. Estimate noise level in each image
   2. Adjust γ proportionally
   3. Higher noise → larger γ
   
   Performance considerations:
   - Single FFT forward + inverse pass
   - Complexity: O(N log N) for N-pixel image
   - Much faster than iterative methods
   - Memory: Several image-sized arrays (FFT, PSF)
   - GPU: Highly efficient for large images
   
   Implementation details (algorithm steps):
   1. Pad image and PSF to avoid circular convolution artifacts
   2. Shift PSF to center frequency domain
   3. FFT of padded image: G = FFT(padded_in)
   4. FFT of shifted PSF: H = FFT(shifted_psf)
   5. Complex conjugate: H* = conj(H)
   6. Numerator: num = H* × G
   7. Denominator: denom = |H|² + γ
   8. Division: F = num / denom (element-wise)
   9. Threshold: if |denom| < γ, set F = 0 (avoid division issues)
   10. Inverse FFT: result = IFFT(F)
   11. Extract: Remove padding, return central region
   
   Common issues and solutions:
   
   **Too much noise in result**:
   - Cause: γ too small
   - Solution: Increase γ by factor of 2-10
   - Check: Noise should be visibly reduced
   
   **Over-smoothed, loss of detail**:
   - Cause: γ too large
   - Solution: Decrease γ by factor of 2-10
   - Check: Edges should be sharper
   
   **Ringing artifacts**:
   - Cause: PSF has sharp cutoff
   - Solution: Smooth/window PSF edges
   - Alternative: Use iterative method
   
   **Inconsistent results across images**:
   - Cause: Different noise levels
   - Solution: Normalize images first
   - Alternative: Estimate γ per image
   
   Edge effects:
   - Padding strategy critical
   - CLAMP_TO_EDGE used internally (replicates border)
   - Alternative: Symmetric padding (mirror)
   - Trade-off: Computation time vs artifact reduction
   
   Wiener filter connection:
   Tikhonov regularization is equivalent to Wiener filter when:
   γ = (noise power spectrum) / (signal power spectrum)
   
   This makes Tikhonov optimal under:
   - Gaussian noise
   - Known signal/noise power spectra
   - Minimum mean square error criterion
   
   Validation:
   - Visual inspection: Sharp edges, minimal noise
   - Quantitative: PSNR, SSIM compared to ground truth
   - Residual: (result ⊗ PSF) should match blurred input
   - Frequency: Check spectrum for noise amplification
   
   Related techniques:
   - Blind deconvolution: PSF unknown (not supported)
   - Lucy-Richardson: Iterative alternative
   - Wiener filter: Similar to Tikhonov with optimal γ
   - Total variation: Edge-preserving regularization
   
   See also:
   - af_iterative_deconv: Iterative deconvolution (Richardson-Lucy, Landweber)
   - af_fft_convolve2: FFT convolution (for validation)
   - af_convolve2: Spatial convolution
   
   Returns:
   ArrayFire error code
   
   Example:
   ```clojure
   ;; Fast deblurring with Tikhonov regularization
   (af-inverse-deconv out-ptr blurred-image psf-kernel
                      0.05  ; gamma = 0.05 (moderate regularization)
                      1)    ; AF_INVERSE_DECONV_TIKHONOV
   
   ;; Heavy regularization for noisy image
   (af-inverse-deconv out-ptr noisy-blurred psf-kernel
                      0.5   ; gamma = 0.5 (heavy smoothing)
                      1)    ; AF_INVERSE_DECONV_TIKHONOV
   ```"
  "af_inverse_deconv" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/float ::mem/int] ::mem/int)
