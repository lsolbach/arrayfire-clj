(ns org.soulspace.arrayfire.ffi.dog
  "Bindings for the ArrayFire Difference of Gaussians (DOG) function.
   
   The Difference of Gaussians (DOG) is an image processing technique that
   approximates the Laplacian of Gaussian (LoG) by computing the difference
   between two Gaussian-blurred versions of the same image with different
   standard deviations (or radii).
   
   ## Mathematical Foundation
   
   **Gaussian Blur**:
   For radius r, creates a Gaussian kernel of size (2r + 1) × (2r + 1):
   ```
   G(x, y, σ) = (1 / (2πσ²)) × exp(-(x² + y²) / (2σ²))
   ```
   
   **Difference of Gaussians**:
   ```
   DOG(x, y) = G(x, y, σ₁) * I(x, y) - G(x, y, σ₂) * I(x, y)
             = (G₁ - G₂) * I
   ```
   Where * denotes convolution, I is input image, and σ₁ < σ₂.
   
   **Relationship to Laplacian of Gaussian**:
   DOG approximates the LoG (∇²G):
   ```
   DOG(x, y) ≈ (σ₂² - σ₁²) × ∇²G(x, y, σ) × I(x, y)
   ```
   Where σ is between σ₁ and σ₂.
   
   ## Algorithm
   
   1. Generate first Gaussian kernel: K₁ with radius r₁
      - Kernel size: (2×r₁ + 1) × (2×r₁ + 1)
      - Standard deviation: σ₁ (auto-calculated from radius)
   
   2. Generate second Gaussian kernel: K₂ with radius r₂
      - Kernel size: (2×r₂ + 1) × (2×r₂ + 1)
      - Standard deviation: σ₂ (auto-calculated from radius)
   
   3. Convolve input with both kernels:
      - S₁ = I ⊗ K₁ (fine scale blur)
      - S₂ = I ⊗ K₂ (coarse scale blur)
   
   4. Compute difference: DOG = S₁ - S₂
   
   ## Scale Space and σ Calculation
   
   ArrayFire auto-calculates σ from radius using:
   ```
   σ = 0.25 × size + 0.75
   ```
   Where size = 2 × radius + 1
   
   For radius r:
   ```
   σ = 0.25 × (2r + 1) + 0.75
     = 0.5r + 1.0
   ```
   
   Typical values:
   - radius = 1: σ ≈ 1.5
   - radius = 2: σ ≈ 2.0
   - radius = 3: σ ≈ 2.5
   - radius = 4: σ ≈ 3.0
   
   ## Properties and Characteristics
   
   **Scale Sensitivity**:
   - Small σ difference: Detects fine edges and details
   - Large σ difference: Detects coarse structures
   - Optimal ratio: σ₂/σ₁ ≈ 1.6 (approximates scale-space extrema)
   
   **Response**:
   - Positive values: Light regions surrounded by dark (bright blobs)
   - Negative values: Dark regions surrounded by light (dark blobs)
   - Zero crossings: Edge locations
   
   **Band-pass filter**:
   DOG acts as a band-pass filter:
   - Removes very low frequencies (DC component)
   - Removes very high frequencies (fine noise)
   - Preserves mid-range frequencies (edges, textures)
   
   ## Applications
   
   **Computer Vision**:
   - Blob detection (finds circular/elliptical structures)
   - Interest point detection (SIFT algorithm uses DOG)
   - Scale-invariant feature detection
   - Object recognition preprocessing
   
   **Edge Detection**:
   - Alternative to Laplacian operator
   - Less sensitive to noise than raw Laplacian
   - Multi-scale edge detection
   - Zero-crossing based edge extraction
   
   **Medical Imaging**:
   - Vessel detection in angiography
   - Tumor boundary detection
   - Cell detection in microscopy
   - Enhancement of anatomical structures
   
   **Astronomy**:
   - Star/galaxy detection
   - Background subtraction
   - Feature enhancement in nebulae
   - Artifact removal
   
   **Microscopy**:
   - Cell segmentation
   - Particle detection
   - Fluorescence microscopy enhancement
   - Spot detection in molecular imaging
   
   **Quality Control**:
   - Defect detection in manufacturing
   - Texture analysis
   - Surface inspection
   - Print quality assessment
   
   ## Advantages
   
   **Computational Efficiency**:
   - Faster than computing LoG directly
   - Separable Gaussian filters (O(n) per dimension)
   - GPU acceleration available
   - Reuses Gaussian blur results
   
   **Noise Robustness**:
   - Gaussian blur reduces high-frequency noise
   - More stable than raw second derivatives
   - Controlled smoothing at multiple scales
   
   **Scale Invariance**:
   - Detects features at different scales
   - Foundation of SIFT descriptor
   - Useful for objects of varying sizes
   
   **Biological Plausibility**:
   - Models receptive fields in human visual cortex
   - Center-surround antagonism
   - Basis for early vision models
   
   ## Use Cases and Patterns
   
   **Blob Detection**:
   ```clojure
   ;; Detect circular features (cells, stars, etc.)
   (let [dog-result (mem/alloc-instance ::mem/pointer)]
     (af-dog dog-result image-arr 2 4)
     ;; Threshold to find blobs
     ;; Positive peaks = bright blobs
     ;; Negative peaks = dark blobs
     @dog-result)
   ```
   
   **Multi-scale Feature Detection**:
   ```clojure
   ;; Detect features at multiple scales
   (defn multi-scale-dog [image scales]
     (for [[r1 r2] scales]
       (let [dog (mem/alloc-instance ::mem/pointer)]
         (af-dog dog image r1 r2)
         @dog)))
   
   ;; Example: detect features from fine to coarse
   (def scale-pairs [[1 2] [2 4] [4 8] [8 16]])
   (def dog-pyramid (multi-scale-dog image scale-pairs))
   ```
   
   **SIFT-style Scale Space**:
   ```clojure
   ;; Ratio of 1.6 between scales (SIFT uses sqrt(2) ≈ 1.414)
   ;; With our σ = 0.5r + 1.0 formula:
   ;; r1=2 (σ≈2.0), r2=3 (σ≈2.5) gives ratio 1.25
   ;; r1=3 (σ≈2.5), r2=5 (σ≈3.5) gives ratio 1.4
   (af-dog dog-arr image 3 5)  ; Approximate SIFT scale ratio
   ```
   
   **Edge Detection**:
   ```clojure
   ;; Find zero crossings for edge detection
   (let [dog (mem/alloc-instance ::mem/pointer)]
     (af-dog dog image 1 2)
     ;; Zero crossings indicate edges
     ;; Can use sign changes or threshold near zero
     @dog)
   ```
   
   ## Parameter Selection Guidelines
   
   **Radius Selection**:
   
   **For Blob Detection**:
   - r2/r1 ratio ≈ 1.6-2.0 for optimal scale detection
   - r1: size of smallest features to detect
   - r2: size of largest features in this scale
   - Example: cells of 4-8 pixels → r1=2, r2=4
   
   **For Edge Detection**:
   - Small difference for fine edges: r1=1, r2=2
   - Larger difference for prominent edges: r1=2, r2=5
   - Very different radii emphasize strong edges only
   
   **For Noise Reduction**:
   - Larger r1 provides more smoothing
   - r1=2 or 3 for noisy images
   - r1=1 for clean images
   
   **Batch Processing**:
   - ArrayFire automatically handles batch dimension
   - 3D input: processes each channel/frame independently
   - Efficient for video or multi-channel images
   
   ## Comparison with Alternatives
   
   **vs. Laplacian of Gaussian (LoG)**:
   - DOG: Faster (two Gaussians vs. one LoG)
   - LoG: More theoretically precise
   - DOG: Good approximation in practice
   - Use DOG for real-time, LoG for accuracy
   
   **vs. Mexican Hat Wavelet**:
   - Mexican hat = LoG in wavelet form
   - DOG: Easier to implement with standard filters
   - Mexican hat: Better theoretical properties
   - Similar practical results
   
   **vs. Raw Laplacian**:
   - Laplacian: Very sensitive to noise
   - DOG: Much more robust to noise
   - DOG: Controlled scale selection
   - Always prefer DOG over raw Laplacian for images
   
   **vs. Sobel/Prewitt**:
   - Sobel: First derivative (gradient)
   - DOG: Second derivative (curvature)
   - Sobel: Directional edge detection
   - DOG: Isotropic blob/edge detection
   - Use Sobel for edge orientation, DOG for blob detection
   
   ## Technical Considerations
   
   **Type Handling**:
   - Integer inputs converted to float for accurate computation
   - Floating-point inputs processed directly
   - Output preserves input precision (float→float, double→double)
   - Integer types use float accumulation internally
   
   **Border Handling**:
   - Gaussian convolution uses appropriate padding
   - Output size matches input size
   - No size reduction (unlike some implementations)
   - Border artifacts minimal due to Gaussian smooth decay
   
   **Batch Mode**:
   - 3D input (W×H×N): Processes N images in batch
   - Each channel/image processed independently
   - Efficient GPU parallelization
   - Same parameters applied to all in batch
   
   **Memory**:
   - Allocates two intermediate arrays (smoothed images)
   - Released automatically after subtraction
   - Final output size = input size
   - Consider memory for large images/batches
   
   ## Performance
   
   **Complexity**:
   - O(n × k₁) + O(n × k₂) where n = pixels, k = kernel size
   - For separable Gaussian: O(n × (r₁ + r₂))
   - GPU parallelized across all pixels
   - Scales well with image size and batch count
   
   **Optimization Tips**:
   - Use power-of-2 image dimensions when possible
   - Batch multiple images for efficiency
   - Consider downsampling for very large images
   - Reuse parameters across similar images
   
   ## Common Pitfalls
   
   **Radius Order**:
   Ensure r1 < r2 for meaningful results. If r1 > r2, result is negated:
   - DOG(r1=3, r2=1) = -(DOG(r1=1, r2=3))
   - Convention: r1 = fine scale, r2 = coarse scale
   
   **Inappropriate Scale Ratios**:
   - r2/r1 too small (e.g., 1.1): Minimal response, noise amplification
   - r2/r1 too large (e.g., 10): Misses intermediate scales
   - Optimal: r2/r1 ≈ 1.6-2.0
   
   **Dimension Requirements**:
   - Input must be 2D (single image) or 3D (batch/multi-channel)
   - 1D or 4D inputs not supported
   - Use assertions to check dimensions
   
   **Interpretation of Sign**:
   - Remember: bright blobs give positive response
   - Dark blobs give negative response
   - Use abs() if only magnitude matters
   - Consider sign when thresholding
   
   **Noise Sensitivity**:
   - Even with smoothing, noise can affect results
   - Pre-smooth very noisy images before DOG
   - Or use larger r1 for more initial smoothing
   
   ## Mathematical Properties
   
   **Linearity**:
   ```
   DOG(a×I₁ + b×I₂) = a×DOG(I₁) + b×DOG(I₂)
   ```
   
   **Scale Selection**:
   At the optimal scale for a blob of size s:
   ```
   σ_optimal ≈ s / √2
   ```
   
   **Normalization**:
   For scale-invariant detection, normalize by σ²:
   ```
   DOG_normalized = σ² × DOG(σ)
   ```
   
   **Zero Crossings**:
   Points where DOG changes sign correspond to edges.
   Marr-Hildreth edge detector uses this property.
   
   ## Examples
   
   **Basic Usage**:
   ```clojure
   ;; Simple DOG application
   (let [dog-out (mem/alloc-instance ::mem/pointer)]
     (af-dog dog-out image-array 2 4)
     @dog-out)
   ```
   
   **Blob Detection**:
   ```clojure
   ;; Detect bright blobs (e.g., cells, stars)
   (let [dog (mem/alloc-instance ::mem/pointer)
         blobs (mem/alloc-instance ::mem/pointer)]
     (af-dog dog image 3 5)
     ;; Threshold positive responses
     (af-gt blobs @dog (create-const-array threshold) 0)
     ;; Find connected components or local maxima
     @blobs)
   ```
   
   **Multi-scale Analysis**:
   ```clojure
   ;; Build DOG pyramid for scale-space analysis
   (defn dog-pyramid [image octaves]
     (let [scales (map #(let [s (Math/pow 2 %)]
                          [(int s) (int (* 1.6 s))])
                       (range octaves))]
       (mapv (fn [[r1 r2]]
               (let [dog (mem/alloc-instance ::mem/pointer)]
                 (af-dog dog image r1 r2)
                 @dog))
             scales)))
   
   ;; Process image at multiple scales
   (def pyramid (dog-pyramid image 4))
   ;; pyramid contains DOG responses at 4 scales
   ```
   
   **Edge Enhancement**:
   ```clojure
   ;; Use DOG for edge enhancement
   (let [dog (mem/alloc-instance ::mem/pointer)
         enhanced (mem/alloc-instance ::mem/pointer)]
     (af-dog dog image 1 2)
     ;; Add DOG to original for sharpening
     (af-add enhanced image @dog 0)
     @enhanced)
   ```
   
   ## Integration with Other Operations
   
   **With Thresholding**:
   ```clojure
   ;; Detect significant features
   (let [dog (mem/alloc-instance ::mem/pointer)]
     (af-dog dog image 2 4)
     ;; Threshold by magnitude
     (let [mag (abs-array @dog)
           mask (threshold mag cutoff)]
       mask))
   ```
   
   **With Non-maximum Suppression**:
   ```clojure
   ;; Find local maxima in DOG response
   ;; (pseudo-code, actual implementation needs dilate/compare)
   (defn find-peaks [dog-array window-size]
     ;; Dilate to find local maxima
     ;; Compare with original
     ;; Keep only local peaks
     )
   ```
   
   **In SIFT Pipeline**:
   ```clojure
   ;; DOG is first step in SIFT feature detection
   (defn sift-dog-stage [image]
     ;; Build DOG scale space
     (let [octaves 4
           scales 3]
       (for [o (range octaves)
             s (range scales)]
         (let [sigma-base (* 1.6 (Math/pow 2 o))
               r1 (sigma-to-radius (* sigma-base (Math/pow 1.6 s)))
               r2 (sigma-to-radius (* sigma-base (Math/pow 1.6 (inc s))))]
           (dog image r1 r2)))))
   ```
   
   See also:
   - af_gaussian_kernel: Generate Gaussian kernels (used internally)
   - af_convolve2: 2D convolution (underlying operation)
   - af_sobel_operator: Alternative edge detection (first derivative)
   - af_bilateral: Edge-preserving smoothing
   - af_canny: Complete edge detection algorithm
   
   References:
   - Marr, D., & Hildreth, E. (1980). Theory of edge detection.
   - Lowe, D. G. (2004). Distinctive image features from scale-invariant keypoints (SIFT).
   - Lindeberg, T. (1998). Feature detection with automatic scale selection."
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; af_err af_dog(af_array *out, const af_array in, const int radius1, const int radius2)
(defcfn af-dog
  "Compute Difference of Gaussians (DOG) for an image.
   
   Calculates the difference between two Gaussian-blurred versions of the input
   image with different radii. This operation approximates the Laplacian of
   Gaussian (LoG) and is widely used for blob detection, edge detection, and
   feature extraction.
   
   The operation performs:
   1. Blur image with Gaussian kernel of radius r1: I₁ = G(r1) ⊗ input
   2. Blur image with Gaussian kernel of radius r2: I₂ = G(r2) ⊗ input  
   3. Compute difference: output = I₁ - I₂
   
   Where G(r) is a Gaussian kernel of size (2r+1)×(2r+1) with auto-calculated σ.
   
   Parameters:
   - out: out pointer to array handle (output DOG image)
   - in: input image array handle (2D or 3D)
   - radius1: radius of first Gaussian kernel (fine scale)
   - radius2: radius of second Gaussian kernel (coarse scale)
   
   Gaussian kernel generation:
   - Kernel size = (2 × radius + 1) × (2 × radius + 1)
   - Standard deviation: σ = 0.25 × size + 0.75 = 0.5 × radius + 1.0
   
   Examples:
   - radius1=1 → 3×3 kernel, σ≈1.5
   - radius1=2 → 5×5 kernel, σ≈2.0
   - radius1=3 → 7×7 kernel, σ≈2.5
   - radius1=4 → 9×9 kernel, σ≈3.0
   
   Mathematical interpretation:
   ```
   DOG(x,y) = [G(σ₁) - G(σ₂)] * I(x,y)
            ≈ (σ₂² - σ₁²) × ∇²G × I
   ```
   
   Where ∇²G is the Laplacian of Gaussian (second derivative).
   
   Constraints:
   - Input must be 2D (single image) or 3D (batch/channels)
   - For 3D: shape [width, height, n] processes n images independently
   - Radius values must be positive integers
   - Typically radius1 < radius2 (fine scale - coarse scale)
   - If radius1 > radius2, result is negated
   
   Type handling:
   - Input types: All numeric types supported
   - Float types (f32, f64): Direct processing, output same type
   - Integer types: Converted to float for processing, output as float
   - Complex types: Not applicable for image processing
   - Accumulation type:
     * f32 input → f32 accumulation
     * f64 input → f64 accumulation  
     * Integer input → f32 accumulation
   
   Batch processing:
   - 3D input [W×H×N]: Processes N images in batch
   - Same DOG parameters applied to all images
   - Efficient GPU parallelization
   - Output shape matches input shape
   
   Response characteristics:
   
   **Sign interpretation**:
   - Positive values: Bright regions surrounded by darker areas (bright blobs)
   - Negative values: Dark regions surrounded by brighter areas (dark blobs)
   - Near-zero: Uniform regions or edges
   - Magnitude: Strength of blob/edge response
   
   **Scale sensitivity**:
   - Small radius difference: Responds to fine details and edges
   - Large radius difference: Responds to coarse structures
   - Optimal blob detection: radius2/radius1 ≈ 1.6-2.0
   
   Applications:
   
   **Blob Detection**:
   Used to find circular or elliptical structures:
   ```clojure
   ;; Detect bright circular features (cells, stars, etc.)
   (af-dog out image 2 4)
   ;; Peak positive values = bright blob centers
   ;; Peak negative values = dark blob centers
   ```
   
   **Interest Point Detection**:
   Foundation of SIFT (Scale-Invariant Feature Transform):
   ```clojure
   ;; Multi-scale DOG for SIFT
   (doseq [[r1 r2] [[2 3] [3 5] [5 8]]]
     (af-dog dog-out image r1 r2)
     ;; Find local extrema across scales
     ;; These become SIFT keypoints
     )
   ```
   
   **Edge Detection**:
   Zero-crossings in DOG indicate edge locations:
   ```clojure
   ;; Marr-Hildreth edge detector
   (af-dog dog image 1 2)
   ;; Find zero crossings (sign changes)
   ;; These correspond to edges
   ```
   
   **Medical Imaging**:
   - Vessel detection in angiography
   - Cell counting in microscopy
   - Tumor boundary detection
   - Spot detection in molecular imaging
   
   **Astronomy**:
   - Star/galaxy detection
   - Cosmic ray removal
   - Background subtraction
   
   Parameter selection guidelines:
   
   **For Small Feature Detection** (cells, stars):
   - radius1 = 2-3 (fine details)
   - radius2 = 4-6 (background)
   - Ratio ≈ 1.6-2.0 for optimal scale
   
   **For Edge Detection**:
   - radius1 = 1 (minimal smoothing)
   - radius2 = 2-3 (slight smoothing)
   - Small difference for fine edges
   
   **For Large Structure Detection**:
   - radius1 = 4-5
   - radius2 = 8-10
   - Larger scales for bigger features
   
   **Noisy Images**:
   - Use larger radius1 (3-4) for more initial smoothing
   - Maintain ratio around 1.6-2.0
   - May need pre-filtering for very noisy data
   
   Algorithm complexity:
   - Time: O(n × (k₁ + k₂)) where n = pixels, k = kernel size
   - For separable Gaussian: O(n × (r₁ + r₂)) per dimension
   - Space: O(n) for output + 2 temporary arrays
   - GPU parallelized for all pixels simultaneously
   
   Advantages over alternatives:
   
   **vs. Laplacian of Gaussian (LoG)**:
   - Faster: Two Gaussians cheaper than computing LoG directly
   - Good approximation: DOG ≈ (σ₂² - σ₁²) × LoG
   - More intuitive: Based on familiar Gaussian blur
   
   **vs. Raw Laplacian**:
   - Much more robust to noise (Gaussian smoothing)
   - Scale selection via radius parameters
   - Stable numerical properties
   
   **vs. Second derivative filters**:
   - Isotropic (rotation invariant)
   - Smooth response (no high-frequency artifacts)
   - Biological plausibility (models retinal cells)
   
   Common patterns and techniques:
   
   **Scale-space pyramid**:
   ```clojure
   ;; Build multi-scale representation
   (defn build-dog-pyramid [image n-scales]
     (for [i (range n-scales)]
       (let [s (Math/pow 1.6 i)
             r1 (int (* 2 s))
             r2 (int (* 3.2 s))]
         (dog image r1 r2))))
   ```
   
   **Feature detection**:
   ```clojure
   ;; Find significant blobs
   (let [dog-result (dog image 3 5)
         abs-response (abs dog-result)
         threshold 10.0]
     ;; Peak values above threshold = features
     (find-peaks abs-response threshold))
   ```
   
   **Edge enhancement**:
   ```clojure
   ;; Unsharp masking with DOG
   (let [dog-edge (dog image 1 2)
         alpha 0.5]
     ;; Enhanced = original + α × DOG
     (add image (mul dog-edge alpha)))
   ```
   
   Numerical considerations:
   
   **Output range**:
   - Unbounded (can be positive or negative)
   - Magnitude depends on input image and radii
   - Typical range: [-255, 255] for 8-bit images
   - May need normalization for visualization
   
   **Border effects**:
   - Gaussian convolution uses padding
   - Borders may show artifacts
   - Usually minimal due to Gaussian smooth decay
   - Crop borders if critical
   
   **Precision**:
   - Float computation recommended for accuracy
   - Integer inputs auto-converted to float
   - Loss of precision possible with integer output
   
   Performance tips:
   - Use power-of-2 dimensions when possible
   - Batch multiple images for GPU efficiency
   - Consider downsampling very large images first
   - Reuse DOG results across pipeline stages
   
   Common pitfalls:
   
   **Inverted results**:
   If radius1 > radius2, result is negated:
   ```clojure
   (dog img 4 2) = -(dog img 2 4)
   ```
   Always use radius1 < radius2 for standard interpretation.
   
   **Scale mismatch**:
   Feature size should match scale:
   - Too small radii: Miss large features, amplify noise
   - Too large radii: Miss small features, over-smooth
   - Solution: Use multiple scales (pyramid)
   
   **Dimension errors**:
   ```clojure
   ;; ERROR: 1D input not supported
   (dog vector-1d 2 4)  ; Fails!
   
   ;; OK: 2D image
   (dog matrix-2d 2 4)  ; Works
   
   ;; OK: Batch of images
   (dog tensor-3d 2 4)  ; Works (processes each independently)
   ```
   
   **Interpretation confusion**:
   - Positive ≠ Always edges
   - Sign matters for blob type
   - Use abs() only if blob polarity doesn't matter
   
   Example calculations:
   
   **Simple image**:
   ```
   Input (5×5):
   [0  0  0  0  0]
   [0  0  0  0  0]
   [0  0 255 0  0]  (bright spot)
   [0  0  0  0  0]
   [0  0  0  0  0]
   
   DOG(radius1=1, radius2=2):
   Center region will be positive (bright blob detected)
   Magnitude indicates blob strength
   ```
   
   **Edge image**:
   ```
   Input (5×5):
   [0  0  0  0  0]
   [0  0  0  0  0]
   [255 255 255 255 255]  (horizontal edge)
   [255 255 255 255 255]
   [255 255 255 255 255]
   
   DOG(radius1=1, radius2=2):
   Zero-crossing at edge location
   Positive on one side, negative on other
   ```
   
   Relationship to other operations:
   
   **Gaussian Pyramid**:
   DOG is difference between adjacent levels of Gaussian pyramid.
   Essential for multi-scale analysis.
   
   **SIFT Features**:
   SIFT detects extrema in DOG scale-space as keypoints.
   DOG approximates scale-normalized LoG.
   
   **Marr-Hildreth Edges**:
   Zero-crossings in DOG locate edges.
   More robust than first-derivative methods.
   
   **Biological Vision**:
   Models center-surround receptive fields in retina.
   ON-center cells: positive DOG
   OFF-center cells: negative DOG
   
   See also:
   - af_gaussian_kernel: Generate Gaussian kernels explicitly
   - af_convolve2: General 2D convolution (underlying operation)
   - af_sobel_operator: First-derivative edge detection
   - af_bilateral: Edge-preserving smoothing (alternative approach)
   - af_canny: Complete edge detection with non-maximum suppression
   - af_sift: Full SIFT feature detector (uses DOG internally)
   
   Returns:
   ArrayFire error code
   
   Example:
   ```clojure
   ;; Basic DOG computation
   (let [dog-out (mem/alloc-instance ::mem/pointer)]
     (af-dog dog-out image-array 2 4)
     @dog-out)
   
   ;; Multi-scale blob detection
   (let [dog1 (mem/alloc-instance ::mem/pointer)
         dog2 (mem/alloc-instance ::mem/pointer)
         dog3 (mem/alloc-instance ::mem/pointer)]
     (af-dog dog1 image 2 3)   ; Fine scale
     (af-dog dog2 image 3 5)   ; Medium scale
     (af-dog dog3 image 5 8)   ; Coarse scale
     ;; Find extrema across scales
     [@dog1 @dog2 @dog3])
   
   ;; Batch processing
   (let [batch-dog (mem/alloc-instance ::mem/pointer)]
     (af-dog batch-dog image-batch 2 4)
     ;; Processes all images in batch
     @batch-dog)
   ```"
  "af_dog" [::mem/pointer ::mem/pointer ::mem/int ::mem/int] ::mem/int)
