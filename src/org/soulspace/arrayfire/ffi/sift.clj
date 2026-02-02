(ns org.soulspace.arrayfire.ffi.sift
  "Bindings for the ArrayFire SIFT and GLOH feature detection and description functions.
   
   SIFT (Scale-Invariant Feature Transform) is a classic computer vision algorithm
   for detecting and describing local features in images. These features are
   invariant to image scaling, rotation, and partially invariant to illumination
   changes and affine transformations.
   
   **SIFT Overview**:
   
   SIFT detects keypoints in an image at multiple scales and orientations, then
   computes descriptors that characterize the local appearance around each keypoint.
   
   **Algorithm Steps**:
   
   1. **Scale-Space Extrema Detection**:
      - Build Gaussian pyramid with multiple octaves and layers
      - Compute Difference of Gaussians (DoG) at each scale
      - Find local extrema in DoG space (keypoint candidates)
   
   2. **Keypoint Localization**:
      - Refine keypoint positions using quadratic interpolation
      - Filter low-contrast keypoints (contrast_thr)
      - Filter edge-like keypoints (edge_thr)
   
   3. **Orientation Assignment**:
      - Compute gradient magnitude and orientation around each keypoint
      - Assign dominant orientation(s) to achieve rotation invariance
   
   4. **Descriptor Computation**:
      - SIFT: 128-dimensional descriptor (4×4 grid of 8-bin histograms)
      - GLOH: 272-dimensional descriptor (log-polar grid, more distinctive)
   
   **SIFT Descriptor**:
   - 128 dimensions (4×4 × 8 orientations)
   - Gradient histograms in 16×16 pixel region around keypoint
   - Normalized to handle illumination changes
   - Typical matching: Euclidean distance with ratio test
   
   **GLOH Descriptor** (Gradient Location-Orientation Histogram):
   - 272 dimensions (more discriminative than SIFT)
   - Log-polar location grid (3 radial + 8 angular bins)
   - Better performance on difficult matching tasks
   - Higher computational cost than SIFT
   
   **Parameters**:
   
   - **n_layers**: Layers per octave (typically 3)
     * More layers = finer scale sampling
     * Original paper suggests 3
   
   - **contrast_thr**: Low-contrast filter (typically 0.04)
     * Higher = fewer but stronger features
     * Filters out noise-like features
   
   - **edge_thr**: Edge response threshold (typically 10.0)
     * Higher = allows more edge-like features
     * Filters features on edges (less stable)
   
   - **init_sigma**: Initial Gaussian blur (typically 1.6)
     * Smoothing before pyramid construction
     * Original paper suggests 1.6
   
   - **double_input**: Double image size (typically false)
     * If true, upscale input 2× for first octave
     * More features at finer scales, but slower
   
   - **intensity_scale**: Inverse of intensity range
     * 1/256 for [0,255] images
     * 1.0 for [0,1] normalized images
   
   - **feature_ratio**: Max features as ratio of pixels
     * Limits total features detected
     * E.g., 0.05 = max 5% of pixels can be features
   
   **Output Structure** (af_features):
   - x: X-coordinates of keypoints
   - y: Y-coordinates of keypoints
   - score: DoG response (keypoint strength)
   - orientation: Dominant orientation (radians)
   - size: Keypoint scale (sigma value)
   - n: Total number of features detected
   
   **Applications**:
   - Image matching and registration
   - Object recognition
   - 3D reconstruction (structure from motion)
   - Panorama stitching
   - Visual odometry
   - Image retrieval
   
   **Performance**:
   - GPU-accelerated (10-50× faster than CPU)
   - Detection: O(n × octaves × layers)
   - Typical: 500-2000 features per 640×480 image
   - GLOH slower than SIFT due to larger descriptors
   
   **Type Support**:
   - f32, f64 (grayscale images only)
   - Input must be 2D
   - Minimum size: 15×15 pixels
   
   **Usage Pattern**:
   
   ```clojure
   ;; Detect and describe SIFT features
   (let [img (load-grayscale-image \"scene.jpg\")
         feat-ptr (mem/alloc-pointer ::mem/pointer)
         desc-ptr (mem/alloc-pointer ::mem/pointer)]
     
     ;; Extract SIFT features with standard parameters
     (af-sift feat-ptr desc-ptr img
              3    ; n_layers
              0.04 ; contrast_thr
              10.0 ; edge_thr
              1.6  ; init_sigma
              false ; double_input
              (/ 1.0 256.0) ; intensity_scale
              0.05) ; feature_ratio (max 5% of pixels)
     
     (let [features (mem/read-pointer feat-ptr ::mem/pointer)
           descriptors (mem/read-pointer desc-ptr ::mem/pointer)]
       ;; features is af_features struct with x, y, score, orientation, size
       ;; descriptors is [N × 128] array for SIFT
       {:features features
        :descriptors descriptors}))
   ```
   
   See also:
   - af-fast: Fast corner detector (faster, but no descriptors)
   - af-orb: ORB features (binary descriptors, very fast)
   - af-hamming-matcher: Match binary descriptors
   - af-nearest-neighbour: Match SIFT descriptors"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; af_err af_sift(af_features *feat, af_array *desc, const af_array in, const unsigned n_layers, const float contrast_thr, const float edge_thr, const float init_sigma, const bool double_input, const float intensity_scale, const float feature_ratio)
(defcfn af-sift
  "Detect and describe SIFT features in a grayscale image.
   
   SIFT (Scale-Invariant Feature Transform) detects keypoints at multiple
   scales and computes 128-dimensional descriptors that are invariant to
   scale, rotation, and partially to illumination and viewpoint changes.
   
   Parameters:
   - feat: Output pointer for af_features structure containing:
     * x: X-coordinates of detected keypoints
     * y: Y-coordinates of detected keypoints
     * score: DoG response values (feature strength)
     * orientation: Dominant orientations in radians
     * size: Keypoint scales (sigma values)
     * n: Total number of features detected
   - desc: Output pointer for descriptor array [N × 128], where N is number
     of features. Each row is a 128-dimensional SIFT descriptor
   - in: Input grayscale image (2D array, f32 or f64)
     * Minimum size: 15×15 pixels
     * Color images not supported (convert to grayscale first)
   - n-layers: Number of layers per octave (typically 3)
     * More layers = finer scale sampling
     * Original SIFT paper suggests 3
     * Range: 1-10 practical
   - contrast-thr: Contrast threshold to filter weak features (typically 0.04)
     * Filters keypoints with low DoG response
     * Higher values = fewer, stronger features
     * Original SIFT paper suggests 0.04
     * Range: 0.01-0.1 practical
   - edge-thr: Edge response threshold (typically 10.0)
     * Filters features on edges (less stable for matching)
     * Higher values = allow more edge-like features
     * Original SIFT paper suggests 10.0
     * Range: 5.0-20.0 practical
   - init-sigma: Initial Gaussian sigma for first octave (typically 1.6)
     * Smoothing before building scale pyramid
     * Original SIFT paper suggests 1.6
     * Range: 0.5-2.0 practical
   - double-input: Whether to double input image size (typically false)
     * If true, upscale input 2× for first octave
     * Detects more features at finer scales
     * Significantly slower (4× pixels to process)
   - intensity-scale: Inverse of intensity range
     * For [0, 255] images: 1/256 = 0.00390625
     * For [0, 1] images: 1.0
     * For [0, 65535] images: 1/65536
   - feature-ratio: Maximum feature ratio (typically 0.05)
     * Max features = feature_ratio × total_pixels
     * Limits memory and computation
     * Range: 0.01-0.1 practical
   
   Algorithm Overview:
   1. Build scale-space pyramid (Gaussian + DoG)
   2. Find local extrema in DoG space
   3. Refine keypoint locations (sub-pixel accuracy)
   4. Filter low-contrast and edge-like keypoints
   5. Assign dominant orientation(s) to each keypoint
   6. Compute 128-D descriptor for each keypoint
   
   Descriptor Format:
   - 128 dimensions per feature
   - 4×4 spatial grid around keypoint
   - 8-bin orientation histogram per grid cell
   - Normalized for illumination invariance
   
   Performance:
   - GPU-accelerated (10-50× faster than CPU)
   - Typical: 500-2000 features for 640×480 image
   - Descriptor extraction ~70% of total time
   
   Example:
   ```clojure
   ;; Standard SIFT with typical parameters
   (let [img (load-grayscale-image \"scene.jpg\")
         feat-ptr (mem/alloc-pointer ::mem/pointer)
         desc-ptr (mem/alloc-pointer ::mem/pointer)]
     
     (af-sift feat-ptr desc-ptr img
              3     ; layers per octave
              0.04  ; contrast threshold
              10.0  ; edge threshold
              1.6   ; initial sigma
              false ; don't double input
              (/ 1.0 256.0) ; for [0,255] images
              0.05) ; max 5% features
     
     (let [features (mem/read-pointer feat-ptr ::mem/pointer)
           descriptors (mem/read-pointer desc-ptr ::mem/pointer)]
       {:n-features (get-feature-count features)
        :descriptors descriptors}))
   
   ;; High-quality mode: more layers, double input
   (af-sift feat-ptr desc-ptr img
            5 0.03 10.0 1.6 true (/ 1.0 256.0) 0.1)
   
   ;; Fast mode: fewer features
   (af-sift feat-ptr desc-ptr img
            3 0.06 10.0 1.6 false (/ 1.0 256.0) 0.02)
   ```
   
   Type Support:
   - f32, f64 (grayscale images only)
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-gloh: GLOH descriptor (272-D, more distinctive)
   - af-orb: Faster binary features
   - af-fast: Corner detection only (no descriptors)"
  "af_sift" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/float ::mem/float ::mem/float ::mem/int ::mem/float ::mem/float] ::mem/int)

;; af_err af_gloh(af_features *feat, af_array *desc, const af_array in, const unsigned n_layers, const float contrast_thr, const float edge_thr, const float init_sigma, const bool double_input, const float intensity_scale, const float feature_ratio)
(defcfn af-gloh
  "Detect SIFT features and compute GLOH descriptors in a grayscale image.
   
   GLOH (Gradient Location-Orientation Histogram) uses SIFT keypoint detection
   but computes 272-dimensional descriptors with a log-polar spatial grid,
   providing better discriminative power than standard SIFT at the cost of
   higher computation and memory usage.
   
   Parameters:
   - feat: Output pointer for af_features structure (same as SIFT)
   - desc: Output pointer for descriptor array [N × 272], where N is number
     of features. Each row is a 272-dimensional GLOH descriptor
   - in: Input grayscale image (2D array, f32 or f64)
     * Minimum size: 15×15 pixels
     * Must be strictly 2D (not 3D like SIFT allows)
   - n-layers: Number of layers per octave (typically 3)
   - contrast-thr: Contrast threshold (typically 0.04)
   - edge-thr: Edge response threshold (typically 10.0)
   - init-sigma: Initial Gaussian sigma (typically 1.6)
   - double-input: Whether to double input size (typically false)
   - intensity-scale: Inverse of intensity range (e.g., 1/256)
   - feature-ratio: Maximum feature ratio (typically 0.05)
   
   GLOH Descriptor:
   - 272 dimensions (vs 128 for SIFT)
   - Log-polar spatial grid (3 radial bins × 8 angular bins)
   - 16-bin orientation histogram per spatial bin
   - More distinctive than SIFT (better matching performance)
   - Computation ~2× slower than SIFT
   - Memory usage ~2× higher than SIFT
   
   Advantages over SIFT:
   - Better discriminative power (higher matching accuracy)
   - More robust to local deformations
   - Better performance in challenging scenarios
   
   Disadvantages:
   - Slower computation (larger descriptors)
   - Higher memory usage
   - Longer matching time (more dimensions to compare)
   
   When to Use GLOH:
   - High-accuracy matching required
   - Sufficient computational resources available
   - Difficult matching scenarios (viewpoint, occlusion)
   - Feature quality more important than speed
   
   When to Use SIFT Instead:
   - Real-time requirements
   - Limited memory/computation
   - Standard matching scenarios sufficient
   
   Performance:
   - Detection: Same as SIFT (uses SIFT detector)
   - Descriptor: ~2× slower than SIFT
   - Matching: ~2× slower than SIFT (more dimensions)
   
   Example:
   ```clojure
   ;; Extract GLOH features for high-accuracy matching
   (let [img (load-grayscale-image \"object.jpg\")
         feat-ptr (mem/alloc-pointer ::mem/pointer)
         desc-ptr (mem/alloc-pointer ::mem/pointer)]
     
     (af-gloh feat-ptr desc-ptr img
              3 0.04 10.0 1.6 false (/ 1.0 256.0) 0.05)
     
     (let [features (mem/read-pointer feat-ptr ::mem/pointer)
           descriptors (mem/read-pointer desc-ptr ::mem/pointer)]
       ;; descriptors is [N × 272] array
       {:n-features (get-feature-count features)
        :descriptor-dims 272
        :descriptors descriptors}))
   ```
   
   Type Support:
   - f32, f64 (grayscale images only)
   - Input must be strictly 2D
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-sift: Standard SIFT (128-D, faster)
   - af-nearest-neighbour: Match GLOH descriptors"
  "af_gloh" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/float ::mem/float ::mem/float ::mem/int ::mem/float ::mem/float] ::mem/int)
