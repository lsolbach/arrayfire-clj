(ns org.soulspace.arrayfire.ffi.c-api.fast
  "Bindings for the ArrayFire FAST corner detection function.
   
   FAST (Features from Accelerated Segment Test) is a high-speed corner
   detection algorithm designed for real-time computer vision applications.
   Developed by Rosten and Drummond (2006), FAST identifies corners by
   examining pixels on a circular pattern around each candidate point.
   
   Algorithm Overview:
   
   For each pixel p in the image with intensity I(p):
   1. Consider a circle of 16 pixels around p (Bresenham circle of radius 3)
   2. A pixel on the circle is brighter if I(x) > I(p) + threshold
   3. A pixel is darker if I(x) < I(p) - threshold  
   4. If N contiguous pixels (arc) are all brighter OR all darker, p is a corner
   5. Typical arc_length values: 9 (more features), 12 (balanced), 16 (fewer, stronger)
   
   Circle Pixel Pattern (16 positions):
   ```
          15 0  1
       14        2
     13            3
     12      p     4
     11            5
       10        6
          9  8  7
   ```
   
   Mathematical Formulation:
   
   Corner Score S(p) = max(bright_score, dark_score) where:
   - bright_score = Σ |I(x_i) - I(p)| for all brighter pixels in arc
   - dark_score = Σ |I(x_i) - I(p)| for all darker pixels in arc
   
   High score indicates strong corner (high contrast with surroundings).
   
   Key Features:
   
   1. Speed:
      - Extremely fast (designed for real-time, ~1000 FPS possible)
      - First tests 4 cardinal pixels (positions 0, 4, 8, 12)
      - Early rejection if < 3 of these 4 meet threshold
      - Full circle test only for candidates
      - Optimized with machine learning decision trees
   
   2. Repeatability:
      - Detects same corners under rotation (with proper arc_length)
      - Consistent under illumination changes (threshold-based)
      - Scale-variant (use pyramid for scale invariance)
   
   3. Non-maximal Suppression:
      - Optional post-processing step
      - Removes weak corners near stronger ones
      - Keeps only local maxima in corner score
      - Essential for preventing clustered detections
   
   4. Multi-scale Detection:
      - Single-scale by default
      - Combine with image pyramids for scale invariance
      - Used in ORB (FAST + pyramids + orientation)
   
   Parameters Explained:
   
   1. threshold (thr):
      - Intensity difference required for pixel classification
      - Range: Typically 10-30 for 8-bit images (0-255 range)
      - Lower values: More corners detected (more noise)
      - Higher values: Fewer, stronger corners (may miss features)
      - Rule of thumb: 20.0 for general purpose
      - Adapt based on image contrast and application
   
   2. arc_length:
      - Number of contiguous pixels required
      - Valid range: 9-16 (ArrayFire constraint)
      - 9: FAST-9 (most permissive, more features, some false positives)
      - 12: FAST-12 (good balance, commonly used)
      - 16: FAST-16 (most restrictive, strong corners only)
      - Original paper: 12 recommended
      - ORB algorithm: Uses 9 for maximum features
   
   3. non_max:
      - Enables non-maximal suppression
      - true: Apply suppression (recommended for most uses)
      - false: Keep all detected corners (may have clusters)
      - Suppression window: 3×3 neighborhood
      - Significantly reduces feature count (50-80% typical)
   
   4. feature_ratio:
      - Maximum features as fraction of image pixels
      - Range: (0.0, 1.0]
      - 0.05: Up to 5% of pixels can be corners (default)
      - 0.01: 1% limit (for efficiency)
      - 0.10: 10% limit (for dense matching)
      - max_features = feature_ratio × width × height
      - Features beyond limit are discarded (not scored)
   
   5. edge:
      - Border width to ignore (pixels from image edges)
      - Minimum: 3 (circle radius)
      - Typical: 3-5 pixels
      - Reason: Circle extends 3 pixels in each direction
      - Prevents out-of-bounds access
      - Larger values: More conservative, fewer edge features
   
   Return Value (af_features):
   
   Structure containing arrays for detected features:
   - x: X coordinates of corners (float array)
   - y: Y coordinates of corners (float array)
   - score: Corner response strength (float array)
   - orientation: Always 0 (FAST doesn't compute orientation)
   - size: Always 1 (FAST doesn't compute scale)
   - n: Number of features detected
   
   Performance Characteristics:
   
   1. Computational Complexity:
      - O(N) where N = image pixels
      - Per-pixel: O(1) with early rejection
      - GPU acceleration: Massive parallelism
      - Typical: 1-5ms for 640×480 on modern GPU
   
   2. Memory Usage:
      - Input: Image array
      - Output: 3×n floats (x, y, score) plus 2×n constant arrays
      - Temporary: Score matrix (W×H floats) if non_max=true
      - Efficient memory layout for GPU
   
   3. Scaling:
      - Linear with image size
      - Sublinear with feature count (due to ratio limit)
      - Excellent GPU utilization (embarrassingly parallel)
   
   Comparison with Other Detectors:
   
   FAST vs. Harris:
   - FAST: 100× faster, less rotation invariant
   - Harris: Slower, more rotation invariant, better localization
   - Use FAST for: Real-time, tracking
   - Use Harris for: Accuracy-critical applications
   
   FAST vs. SIFT:
   - FAST: 1000× faster, no scale/rotation invariance
   - SIFT: Scale+rotation invariant, slower, patented (expired 2020)
   - Use FAST for: Speed-critical applications
   - Use SIFT for: Difficult matching scenarios
   
   FAST vs. ORB:
   - ORB = FAST + orientation + pyramids + rBRIEF descriptor
   - FAST: Detection only
   - ORB: Detection + description, suitable for matching
   
   Common Applications:
   
   1. Visual Odometry:
      - Track features frame-to-frame
      - Estimate camera motion
      - SLAM (Simultaneous Localization and Mapping)
   
   2. Object Tracking:
      - Detect features in first frame
      - Track in subsequent frames (optical flow)
      - Handle occlusion and motion blur
   
   3. Image Stitching:
      - Detect features in multiple images
      - Match features (requires descriptors)
      - Compute homography/warp
   
   4. Augmented Reality:
      - Real-time feature tracking
      - Pose estimation
      - Marker detection
   
   5. Motion Detection:
      - Compare feature sets between frames
      - Identify moving objects
      - Background subtraction enhancement
   
   6. 3D Reconstruction:
      - Multi-view stereo
      - Structure from Motion (SfM)
      - Dense matching preparation
   
   Best Practices:
   
   1. Threshold Selection:
      - Start with default (20.0)
      - Decrease if too few features detected
      - Increase if too many false positives
      - Adapt to image contrast range
   
   2. Arc Length Selection:
      - 9: Maximum features, use with ORB
      - 12: General purpose, good balance
      - 16: High-quality corners only
   
   3. Non-maximal Suppression:
      - Enable for most applications
      - Disable for:
        * Dense optical flow
        * When feature count is critical
        * Custom suppression logic needed
   
   4. Feature Ratio:
      - 0.05: Good default
      - Lower (0.01): For efficiency in large images
      - Higher (0.10): For matching in texture-poor scenes
   
   5. Edge Handling:
      - Keep default (3) unless specific need
      - Increase (5-10) for images with border artifacts
      - Consider: Features near edges may be unreliable
   
   6. Multi-scale Detection:
      - Build image pyramid manually
      - Detect at each scale
      - Merge results with scale information
      - Or use ORB which handles this automatically
   
   Limitations and Considerations:
   
   1. Not Rotation Invariant:
      - Arc pattern changes under rotation
      - Partial invariance (±30° typical)
      - Solution: Use ORB or compute orientation separately
   
   2. Not Scale Invariant:
      - Features at one scale only
      - Solution: Multi-scale detection (pyramid)
   
   3. Clustered Detection:
      - May detect multiple features near true corner
      - Solution: Always enable non-maximal suppression
   
   4. Sensitive to Noise:
      - High-frequency noise can create false corners
      - Solution: Pre-filter image (Gaussian blur)
   
   5. Texture Dependence:
      - Poor performance on smooth surfaces
      - Excellent on textured scenes
      - Solution: Combine with other features
   
   6. Edge Effects:
      - Features near edges may be less stable
      - Solution: Increase edge parameter
   
   Implementation Details (ArrayFire):
   
   1. GPU Kernels:
      - CUDA: Optimized with texture memory
      - OpenCL: Efficient work-group patterns
      - CPU: SIMD vectorization where possible
   
   2. Circle Pattern:
      - Precomputed lookup table for circle pixels
      - Shared memory for fast access
      - Coalesced memory access patterns
   
   3. Early Rejection:
      - Test cardinal directions first
      - Reduces computation by ~50-70%
   
   4. Non-maximal Suppression:
      - Two-pass algorithm:
        1. Compute scores for all candidates
        2. Suppress non-maxima in neighborhood
      - Efficient atomic operations on GPU
   
   5. Feature Extraction:
      - Compact storage (SoA layout)
      - Direct GPU→CPU transfer for small counts
      - Efficient handling of feature_ratio limit
   
   Integration with ArrayFire Ecosystem:
   
   1. Combine with ORB:
      ```clojure
      (let [fast-features (af-fast img 20.0 9 true 0.05 3)
            [orb-features descriptors] (af-orb img 20.0 400 1.5 4 false)]
        ;; Use ORB for full pipeline (detection + description)
        )
      ```
   
   2. Multi-scale Pipeline:
      ```clojure
      (loop [scale 1.0
             pyramid []
             features []]
        (if (< scale 0.5)
          features
          (let [scaled (resize img (* width scale) (* height scale))
                feats (af-fast scaled 20.0 12 true 0.05 3)]
            (recur (* scale 0.8)
                   (conj pyramid scaled)
                   (conj features feats)))))
      ```
   
   3. Tracking Pipeline:
      ```clojure
      (defn track-features [img1 img2]
        (let [features1 (af-fast img1 20.0 12 true 0.05 3)
              ;; Use optical flow to track features
              tracked (lucas-kanade img1 img2 features1)]
          tracked))
      ```
   
   Tuning for Specific Scenarios:
   
   1. High-Resolution Images (>2MP):
      - Lower feature_ratio (0.01-0.02)
      - Higher threshold (25-30)
      - Consider downsampling first
   
   2. Low-Light Images:
      - Lower threshold (15-20)
      - Pre-process with histogram equalization
      - Consider adaptive thresholding
   
   3. Fast-Moving Objects:
      - Increase feature count (feature_ratio=0.10)
      - Lower arc_length (9) for more features
      - Enable motion blur handling
   
   4. Static Scene (Structure from Motion):
      - Higher threshold (25-30) for quality
      - arc_length=12 or 16
      - Enable non-maximal suppression
   
   5. Real-Time Tracking:
      - Optimize for speed
      - arc_length=9 (fastest)
      - Feature_ratio=0.02 (fewer features)
      - Profile GPU utilization
   
   Related Functions:
   - af_harris: Alternative corner detector (more accurate, slower)
   - af_orb: FAST + orientation + descriptors for matching
   - af_sift: Scale-invariant features (slower, more robust)
   - af_susan: Edge-preserving corner detector
   - af_features: Structure for managing detected features"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.loader]))

;; FAST corner detection function

;; af_err af_fast(af_features *out, const af_array in, const float thr,
;;                const unsigned arc_length, const bool non_max,
;;                const float feature_ratio, const unsigned edge)
(defcfn af-fast
  "FAST (Features from Accelerated Segment Test) corner detection.
   
   Detects corner features in grayscale images using the FAST algorithm,
   one of the fastest corner detection methods available. FAST identifies
   corners by examining intensity patterns on a circular ring of pixels
   around each candidate point.
   
   The algorithm tests whether a sufficient arc (contiguous sequence) of
   pixels on the circle are all significantly brighter or darker than the
   central pixel. This creates a high-speed test for corner-like structures.
   
   Parameters:
   
   - out: af_features* output structure containing:
     * x: X coordinates of detected corners (float array, length n)
     * y: Y coordinates of detected corners (float array, length n)
     * score: Corner response strength (float array, length n)
     * orientation: Set to 0 for all features (FAST doesn't compute orientation)
     * size: Set to 1 for all features (FAST is single-scale)
     * n: Number of features detected
   
   - in: af_array handle to grayscale input image
     * Must be 2D array (single channel)
     * All numeric types supported (f32, f64, u8, s8, u16, s16, u32, s32)
     * Color images: Convert to grayscale first (af_colorSpace)
     * Dimensions: At least (2*edge+1) × (2*edge+1)
   
   - thr: float threshold for pixel classification
     * Intensity difference required to classify pixel as brighter/darker
     * Typical range: 10.0-30.0 for 8-bit images
     * Default: 20.0 (good general-purpose value)
     * Lower: More features detected (may include noise)
     * Higher: Fewer, stronger corners only
     * Scale: Should be proportional to image dynamic range
   
   - arc_length: unsigned, contiguous pixels required for corner
     * Valid range: 9-16 (enforced by ArrayFire)
     * Common values:
       - 9: FAST-9, most permissive (used in ORB)
       - 12: FAST-12, balanced (original paper recommendation)
       - 16: FAST-16, most restrictive (strongest corners only)
     * Lower: More features, faster computation
     * Higher: Fewer features, stronger corners, slower
   
   - non_max: bool (as int), enable non-maximal suppression
     * 1 (true): Apply suppression (recommended)
       - Removes weaker corners in 3×3 neighborhood
       - Keeps only local maxima in corner score
       - Significantly reduces feature count (50-80%)
       - Prevents clustered detections
     * 0 (false): Keep all detected corners
       - May have multiple detections near true corner
       - Use when feature density is critical
       - Or when applying custom suppression
   
   - feature_ratio: float, maximum features as fraction of pixels
     * Range: (0.0, 1.0]
     * max_features = feature_ratio × image_width × image_height
     * Default: 0.05 (up to 5% of pixels)
     * Common values:
       - 0.01: Conservative (large images, efficiency)
       - 0.05: Standard (good for most applications)
       - 0.10: Dense (texture-poor scenes, matching)
     * Features beyond limit are discarded
     * Not score-based: First N features kept, rest dropped
   
   - edge: unsigned, border width to ignore (pixels)
     * Minimum: 3 (required for circle radius)
     * Typical: 3-5 pixels
     * Reason: Circle pattern extends 3 pixels from center
     * Larger values: More conservative, ignore near-edge features
     * Features within edge pixels of border are not tested
   
   Returns:
   AF_SUCCESS (0) on success, error code otherwise:
   - AF_ERR_ARG: Invalid threshold (thr <= 0)
   - AF_ERR_ARG: Invalid arc_length (< 9 or > 16)
   - AF_ERR_ARG: Invalid feature_ratio (<= 0 or > 1.0)
   - AF_ERR_SIZE: Image too small (< 2*edge+1 in any dimension)
   - AF_ERR_TYPE: Unsupported input type
   - AF_ERR_NO_MEM: Insufficient GPU memory
   
   Output Structure:
   
   The af_features structure contains:
   ```c
   typedef struct {
       dim_t n;              // Number of features
       af_array x;           // X coordinates (length n)
       af_array y;           // Y coordinates (length n)
       af_array score;       // Corner scores (length n)
       af_array orientation; // Always 0 (FAST has no orientation)
       af_array size;        // Always 1 (FAST is single-scale)
   } af_features_t;
   ```
   
   Usage Examples:
   
   1. Basic Detection:
   ```clojure
   (let [features-ptr (mem/alloc-instance ::mem/pointer)
         err (af-fast features-ptr img-array 20.0 12 1 0.05 3)]
     (when (zero? err)
       (let [features (mem/read-ptr features-ptr)]
         ;; Use features for tracking, matching, etc.
         (af-release-features features))))
   ```
   
   2. High-Quality Corners:
   ```clojure
   ;; Stronger threshold, longer arc, suppression enabled
   (af-fast out img 30.0 16 1 0.02 5)
   ```
   
   3. Dense Feature Detection:
   ```clojure
   ;; More permissive for optical flow
   (af-fast out img 15.0 9 0 0.10 3)
   ```
   
   Algorithm Details:
   
   1. For each pixel p at (x, y) in valid region:
      a. Test 4 cardinal pixels (0, 4, 8, 12) first
      b. If < 3 meet threshold, skip pixel (early rejection)
      c. Test remaining 12 pixels on circle
      d. If arc_length contiguous pixels are brighter OR darker, p is corner
      e. Compute corner score from intensity differences
   
   2. If non_max enabled:
      a. Build score matrix for all detected corners
      b. For each corner, check 3×3 neighborhood
      c. Keep only if score is local maximum
   
   3. Sort/limit features:
      a. Keep up to max_features = feature_ratio × pixels
      b. Features beyond limit discarded (order-dependent)
   
   Performance:
   - Extremely fast: O(N) where N = image pixels
   - GPU parallelism: Each pixel processed independently
   - Early rejection: ~50-70% of pixels skipped quickly
   - Typical: 1-5ms for 640×480 on modern GPU
   
   Best Practices:
   1. Always enable non-maximal suppression (non_max=1)
   2. Start with defaults: thr=20.0, arc=12, ratio=0.05
   3. Tune threshold based on image contrast
   4. Use arc_length=9 for maximum features (ORB, tracking)
   5. Use arc_length=12-16 for quality (matching, SfM)
   6. Pre-filter noisy images with Gaussian blur
   7. Convert color images to grayscale first
   8. Consider multi-scale detection for scale invariance
   
   Limitations:
   - Not rotation invariant (arc pattern rotates)
   - Not scale invariant (single-scale detection)
   - Sensitive to high-frequency noise
   - May cluster features without non-maximal suppression
   - Poor on smooth, texture-free surfaces
   
   See Also:
   - af_harris: More accurate corner detection (slower)
   - af_orb: FAST + orientation + binary descriptors
   - af_sift: Scale and rotation invariant features
   - af_susan: Edge-preserving corner detector"
  "af_fast" [::mem/pointer ::mem/pointer ::mem/float ::mem/int ::mem/int ::mem/float ::mem/int] ::mem/int)
