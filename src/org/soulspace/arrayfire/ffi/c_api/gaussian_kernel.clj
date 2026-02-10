(ns org.soulspace.arrayfire.ffi.c-api.gaussian-kernel
  "Bindings for the ArrayFire gaussian kernel generation function."
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.loader]))

;; af_err af_gaussian_kernel(af_array *out, const int rows, const int cols, const double sigma_r, const double sigma_c)
(defcfn af-gaussian-kernel
  "Generate a Gaussian kernel array.
   
   This function generates a 2D Gaussian kernel (filter) that can be used for
   Gaussian blurring and smoothing operations. The kernel values are computed
   using the Gaussian (normal) distribution function.
   
   Mathematical Formula:
   The 2D Gaussian function is:
     G(x,y) = (1/(2πσ_xσ_y)) * exp(-(x²/(2σ_x²) + y²/(2σ_y²)))
   
   Where:
   - x, y are distances from the center
   - σ_x (sigma_r) is standard deviation in x direction (rows)
   - σ_y (sigma_c) is standard deviation in y direction (cols)
   
   The kernel is normalized so that all values sum to 1, ensuring that
   the overall image brightness is preserved during convolution.
   
   Default Sigma Values:
   If sigma_r = 0 or sigma_c = 0, they are calculated as:
     sigma = 0.25 * dimension + 0.75
   
   This default follows the rule that the kernel should capture approximately
   ±3σ of the distribution, which contains ~99.7% of the Gaussian mass.
   
   Kernel Properties:
   1. **Separability**: 2D Gaussian = product of two 1D Gaussians
      G(x,y) = G(x) * G(y)
      This enables efficient separable convolution: O(N*k) vs O(N*k²)
   
   2. **Symmetry**: Radially symmetric (if σ_x = σ_y)
      G(x,y) = G(-x,-y) = G(x,-y) = G(-x,y)
   
   3. **Normalization**: Σ G(x,y) = 1
      Preserves brightness during convolution
   
   4. **Low-pass filter**: Attenuates high frequencies
      Acts as smoothing/anti-aliasing filter
   
   Sigma Selection Guidelines:
   
   1. **Small sigma (< 1.0)**:
      - Minimal smoothing
      - Preserves fine details
      - Small kernel size (3×3 or 5×5)
      - Fast computation
      - Use for: slight noise reduction, anti-aliasing
   
   2. **Medium sigma (1.0 - 3.0)**:
      - Moderate smoothing
      - Balances detail vs noise reduction
      - Standard kernel sizes (5×5 to 11×11)
      - Use for: general purpose blurring, preprocessing
   
   3. **Large sigma (> 3.0)**:
      - Heavy smoothing
      - Removes fine details
      - Large kernel sizes (13×13 or larger)
      - Slower computation
      - Use for: strong denoising, background removal
   
   Kernel Size vs Sigma Relationship:
   Common practice: kernel_size ≈ 6*sigma + 1 (capture ±3σ)
   - sigma=1.0 → 7×7 kernel
   - sigma=2.0 → 13×13 kernel
   - sigma=3.0 → 19×19 kernel
   
   For efficient computation, use odd kernel sizes to have a clear center pixel.
   
   Applications:
   
   1. **Image Blurring**:
      ```clojure
      ;; Create 5×5 Gaussian kernel with sigma=1.0
      (let [kernel (af-gaussian-kernel 5 5 1.0 1.0)
            blurred (af-convolve2 image kernel)]
        ;; Image smoothed with Gaussian blur
        )
      ```
   
   2. **Noise Reduction**:
      - Gaussian blur reduces high-frequency noise
      - Preserves edges better than box filter
      - Optimal for Gaussian-distributed noise
   
   3. **Edge Detection Preprocessing**:
      - Smooth image before edge detection
      - Reduces false edges from noise
      - Standard practice: Gaussian blur → Sobel/Canny
   
   4. **Scale-Space Analysis**:
      - Generate image pyramid with different blur levels
      - Each level represents different scale
      - Foundation of SIFT, SURF features
   
   5. **Difference of Gaussians (DoG)**:
      - Approximates Laplacian of Gaussian
      - Used for blob detection
      - Edge enhancement and sharpening
   
   6. **Bilateral Filter Component**:
      - Spatial Gaussian kernel
      - Combined with range kernel
      - Edge-preserving smoothing
   
   7. **Multi-scale Image Processing**:
      - Create Gaussian pyramids
      - Octave generation in SIFT
      - Image blending and compositing
   
   Performance Considerations:
   
   1. **Separable Convolution**:
      2D Gaussian can be decomposed into two 1D convolutions:
      - Convolve rows with 1D horizontal kernel: O(N*rows)
      - Convolve columns with 1D vertical kernel: O(N*cols)
      - Total: O(N*(rows+cols)) vs O(N*rows*cols) for direct 2D
   
   2. **Kernel Size Impact**:
      - Larger kernels = more computation
      - Use smallest kernel that captures desired sigma
      - Clip kernel at ±3σ (99.7% of distribution)
   
   3. **GPU Optimization**:
      - Kernel generation is fast (CPU-based)
      - Store and reuse kernels
      - Actual convolution is GPU-accelerated
   
   Implementation Notes:
   - Always returns float (f32) array
   - Kernel normalized to sum to 1.0
   - Even if rows/cols differ, maintains Gaussian properties
   - Can create non-square kernels (anisotropic blur)
   
   Examples:
   
   1. Basic Gaussian kernel (isotropic):
      ```clojure
      ;; Create 7×7 kernel, auto-calculated sigma
      (let [kernel (af-gaussian-kernel 7 7 0.0 0.0)]
        ;; sigma_r = 0.25*7 + 0.75 = 2.5
        ;; sigma_c = 0.25*7 + 0.75 = 2.5
        )
      ```
   
   2. Custom sigma values:
      ```clojure
      ;; Create 9×9 kernel with sigma=2.0
      (let [kernel (af-gaussian-kernel 9 9 2.0 2.0)
            blurred (af-convolve2 image kernel)]
        ;; Apply Gaussian blur
        )
      ```
   
   3. Anisotropic Gaussian (different sigmas):
      ```clojure
      ;; Blur more in horizontal direction
      (let [kernel (af-gaussian-kernel 11 11 1.0 3.0)]
        ;; sigma_r=1.0 (less vertical blur)
        ;; sigma_c=3.0 (more horizontal blur)
        )
      ```
   
   4. Separable convolution for efficiency:
      ```clojure
      ;; Create 1D kernels for separable convolution
      (defn separable-gaussian-blur [image sigma]
        (let [size (int (+ (* 6 sigma) 1))
              kernel-h (af-gaussian-kernel 1 size 0.0 sigma)
              kernel-v (af-gaussian-kernel size 1 sigma 0.0)
              temp (af-convolve2 image kernel-h)
              result (af-convolve2 temp kernel-v)]
          result))
      ```
   
   5. Multi-scale Gaussian pyramid:
      ```clojure
      ;; Generate pyramid with increasing blur
      (defn gaussian-pyramid [image levels]
        (loop [pyr [image]
               lvl 0]
          (if (>= lvl levels)
            pyr
            (let [sigma (* (Math/pow 2 lvl) 1.6)
                  size (int (+ (* 6 sigma) 1))
                  kernel (af-gaussian-kernel size size sigma sigma)
                  blurred (af-convolve2 (last pyr) kernel)
                  downsampled (af-resize blurred 
                                        (/ (af-get-dims blurred 0) 2)
                                        (/ (af-get-dims blurred 1) 2))]
              (recur (conj pyr downsampled) (inc lvl))))))
      ```
   
   6. Difference of Gaussians (DoG):
      ```clojure
      ;; DoG for edge detection/blob detection
      (defn difference-of-gaussians [image sigma1 sigma2]
        (let [size1 (int (+ (* 6 sigma1) 1))
              size2 (int (+ (* 6 sigma2) 1))
              kernel1 (af-gaussian-kernel size1 size1 sigma1 sigma1)
              kernel2 (af-gaussian-kernel size2 size2 sigma2 sigma2)
              blur1 (af-convolve2 image kernel1)
              blur2 (af-convolve2 image kernel2)]
          (af-sub blur1 blur2)))
      ```
   
   7. Unsharp masking (sharpening):
      ```clojure
      ;; Sharpen by subtracting blurred version
      (defn unsharp-mask [image sigma amount]
        (let [size (int (+ (* 6 sigma) 1))
              kernel (af-gaussian-kernel size size sigma sigma)
              blurred (af-convolve2 image kernel)
              diff (af-sub image blurred)
              sharpened (af-add image (af-mul diff amount))]
          sharpened))
      ```
   
   8. Adaptive noise reduction:
      ```clojure
      ;; Vary blur based on local image characteristics
      (defn adaptive-gaussian-blur [image noise-level]
        (let [sigma (if (< noise-level 0.1)
                      1.0  ; Low noise: minimal blur
                      (if (< noise-level 0.3)
                        2.0  ; Medium noise: moderate blur
                        3.0))  ; High noise: strong blur
              size (int (+ (* 6 sigma) 1))
              kernel (af-gaussian-kernel size size sigma sigma)]
          (af-convolve2 image kernel)))
      ```
   
   Related Functions:
   - af-convolve2: Apply kernel to image (2D convolution)
   - af-bilateral: Edge-preserving blur (combines Gaussian with range kernel)
   - af-mean-shift: Non-linear smoothing
   - af-medfilt: Non-linear median filter
   
   Common Pitfalls:
   1. **Too small kernel for large sigma**: Truncates Gaussian tail
      - Rule: kernel_size >= 6*sigma + 1
   2. **Using even kernel sizes**: No clear center pixel
      - Always use odd sizes (3, 5, 7, 9, 11, ...)
   3. **Not reusing kernels**: Regenerating same kernel repeatedly
      - Cache and reuse identical kernels
   4. **Ignoring separability**: Using direct 2D when 1D×1D faster
      - For large kernels, separable convolution is much faster
   
   Type Support:
   - Output is always float (f32) precision
   - Input parameters are integers (rows, cols) and doubles (sigmas)
   - Suitable for all subsequent convolution operations
   
   Parameters:
   - out: out pointer for generated kernel array handle
   - rows: number of rows in the kernel (height)
   - cols: number of columns in the kernel (width)
   - sigma_r: standard deviation for rows (0 = auto-calculate)
   - sigma_c: standard deviation for columns (0 = auto-calculate)
   
   Returns:
   ArrayFire error code
   
   Notes:
   - Default sigmas: sigma = 0.25 * dimension + 0.75
   - Output kernel normalized to sum to 1.0
   - Kernel centered at (rows/2, cols/2)
   - Use odd kernel sizes for symmetry
   - Larger sigma = more blur, requires larger kernel
   - Isotropic blur: set sigma_r = sigma_c
   - Anisotropic blur: use different sigma_r and sigma_c"
  "af_gaussian_kernel" [::mem/pointer ::mem/int ::mem/int ::mem/double ::mem/double] ::mem/int)
