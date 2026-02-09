(ns org.soulspace.arrayfire.integration.vision
  "High-level Clojure wrapper for ArrayFire computer vision functions.
   
   Computer vision functions provide feature detection, descriptor extraction,
   feature matching, and geometric transformations. These are essential for
   image analysis, object recognition, tracking, and 3D reconstruction.
   
   ## Quick Start
   
   ```clojure
   (require '[org.soulspace.arrayfire.integration.vision :as vision])
   (require '[org.soulspace.arrayfire.integration.array :as array])
   
   ;; Detect FAST corners
   (let [img (array/load-image \"photo.jpg\")
         features (vision/fast img 20.0)]
     features)
   
   ;; Extract ORB features and descriptors
   (let [img (array/load-image \"photo.jpg\")
         [features descriptors] (vision/orb img)]
     {:features features :descriptors descriptors})
   
   ;; Match features using Hamming distance
   (let [[f1 d1] (vision/orb img1)
         [f2 d2] (vision/orb img2)
         [idx dist] (vision/hamming-matcher d1 d2)]
     {:indices idx :distances dist})
   
   ;; Template matching
   (let [search (array/load-image \"scene.jpg\")
         template (array/load-image \"object.jpg\")
         result (vision/match-template search template :sad)]
     result)
   ```
   
   ## Feature Detection
   
   ### Corner Detection
   
   Corner detectors identify points where image intensity changes significantly
   in multiple directions. These are stable, repeatable features useful for
   tracking and matching.
   
   **FAST (Features from Accelerated Segment Test)**:
   - Very fast corner detection (real-time capable)
   - Compares pixel intensities on circular arc around candidate
   - No orientation or scale invariance
   - Good for video tracking and SLAM
   
   **Harris Corner Detector**:
   - Classic corner detector based on eigenvalues of structure tensor
   - Rotation invariant, not scale invariant
   - Responds to corners, not edges
   - Good corner localization
   
   **SUSAN (Smallest Univalue Segment Assimilating Nucleus)**:
   - Compares pixel values in circular region
   - Robust to noise
   - Detects corners and edges
   - Good for low-level feature detection
   
   ### Feature Descriptors
   
   Feature descriptors encode the local appearance around detected features,
   enabling matching across images despite changes in viewpoint, lighting,
   or scale.
   
   **ORB (Oriented FAST and Rotated BRIEF)**:
   - Binary descriptor (256 bits)
   - Rotation invariant
   - Very fast computation and matching
   - Good balance of speed and quality
   - Used in: Real-time SLAM, mobile applications
   
   **SIFT (Scale-Invariant Feature Transform)**:
   - Floating-point descriptor (128-D)
   - Scale and rotation invariant
   - Highly distinctive
   - Robust to illumination changes
   - Used in: Object recognition, image stitching
   
   **GLOH (Gradient Location and Orientation Histogram)**:
   - Extension of SIFT (272-D)
   - Log-polar spatial bins
   - More distinctive than SIFT
   - Better for complex scenes
   
   ## Feature Matching
   
   ### Nearest Neighbor Search
   
   Find closest matching features between two sets of descriptors:
   
   **Hamming Matcher**:
   - For binary descriptors (ORB, BRIEF, BRISK)
   - Hamming distance: count of differing bits
   - Extremely fast (XOR + popcount)
   - Use with: ORB, binary descriptors
   
   **Nearest Neighbour**:
   - For floating-point descriptors (SIFT, SURF)
   - Distance metrics: SSD (sum of squared differences), SAD (sum of absolute differences)
   - Use with: SIFT, GLOH, floating-point descriptors
   
   ### Matching Strategies
   
   **1-to-1 Matching**:
   ```clojure
   (let [[idx dist] (hamming-matcher query train 0 1)]
     ;; Each query has 1 nearest neighbor
     )
   ```
   
   **k-NN Matching**:
   ```clojure
   (let [[idx dist] (hamming-matcher query train 0 2)]
     ;; Each query has 2 nearest neighbors
     ;; Use for ratio test (Lowe's ratio test)
     )
   ```
   
   **Ratio Test** (Lowe's criterion):
   Filter ambiguous matches by comparing distances to 1st and 2nd nearest
   neighbors. Keep matches where d1/d2 < threshold (typically 0.7-0.8).
   
   ## Template Matching
   
   Find occurrences of a template image within a larger search image:
   
   **Match Types**:
   - **SAD** (Sum of Absolute Differences): Fast, simple
   - **ZSAD** (Zero-mean SAD): Robust to brightness changes
   - **LSAD** (Locally scaled SAD): Robust to contrast changes
   - **SSD** (Sum of Squared Differences): Emphasizes large errors
   - **ZSSD** (Zero-mean SSD): Robust to brightness changes
   - **LSSD** (Locally scaled SSD): Robust to contrast changes
   - **NCC** (Normalized Cross-Correlation): Robust to brightness/contrast
   - **ZNCC** (Zero-mean NCC): Most robust, slower
   
   Returns correlation/distance map; find minimum (for SAD/SSD) or maximum
   (for NCC) to locate best match.
   
   ## Image Processing
   
   **DOG (Difference of Gaussians)**:
   - Approximates Laplacian of Gaussian
   - Detects blobs at specific scales
   - Used in SIFT scale-space construction
   - Enhances edges and ridges
   
   ## Geometric Transformations
   
   **Homography Estimation**:
   - Estimates planar transformation between two views
   - Maps points from one image plane to another
   - Uses RANSAC for robust estimation with outliers
   - Applications: Image stitching, augmented reality, calibration
   
   **Homography Types**:
   - **RANSAC**: Random sample consensus, robust to outliers
   - **LMedS**: Least median of squares, good for <50% outliers
   
   A homography H is a 3×3 matrix relating corresponding points:
   ```
   [x']   [h11 h12 h13]   [x]
   [y'] = [h21 h22 h23] * [y]
   [1 ]   [h31 h32 h33]   [1]
   ```
   
   In normalized coordinates: x' = H * x
   
   ## Common Workflows
   
   ### Object Detection and Tracking
   
   ```clojure
   ;; 1. Detect features in reference image
   (let [[ref-feat ref-desc] (orb reference-img)]
     
     ;; 2. Detect features in current frame
     (let [[cur-feat cur-desc] (orb current-frame)]
       
       ;; 3. Match features
       (let [[idx dist] (hamming-matcher ref-desc cur-desc 0 2)]
         
         ;; 4. Filter matches (ratio test)
         ;; 5. Estimate homography
         ;; 6. Draw bounding box
         )))
   ```
   
   ### Image Stitching (Panorama)
   
   ```clojure
   ;; For each pair of overlapping images:
   ;; 1. Detect SIFT features
   (let [[f1 d1] (sift img1)
         [f2 d2] (sift img2)]
     
     ;; 2. Match features
     (let [[idx dist] (nearest-neighbour d1 d2 0 2 :ssd)]
       
       ;; 3. Filter matches
       ;; 4. Estimate homography
       (let [[H inliers] (homography x1 y1 x2 y2 :ransac 3.0 1000)]
         
         ;; 5. Warp and blend images
         )))
   ```
   
   ### Feature Tracking (Video)
   
   ```clojure
   ;; Fast corner tracking for video
   (let [corners (fast frame 20.0 9 true 0.05 3)]
     ;; Track corners across frames
     ;; Use optical flow or feature matching
     ;; Update positions
     )
   ```
   
   ## Performance Tips
   
   1. **Choose appropriate detector**:
      - Real-time: FAST (corners only)
      - Speed with descriptors: ORB
      - Quality: SIFT, GLOH
   
   2. **Reduce feature count**:
      - Limit max features in detector
      - Use higher thresholds
      - Non-maximum suppression
   
   3. **Matching optimization**:
      - Use binary descriptors (ORB) for speed
      - Limit search to local regions
      - Use spatial constraints
   
   4. **Template matching**:
      - Downscale images if possible
      - Use SAD/SSD for speed, NCC for robustness
      - Search in smaller regions of interest
   
   5. **Homography estimation**:
      - Limit RANSAC iterations for speed
      - Use good initial estimates
      - Filter matches before RANSAC
   
   ## Best Practices
   
   1. **Feature Detection**:
      - Convert to grayscale first
      - Normalize lighting
      - Apply bilateral filter to reduce noise
      - Use appropriate threshold/parameters
   
   2. **Feature Matching**:
      - Use ratio test (Lowe's criterion)
      - Apply geometric constraints (epipolar, homography)
      - Verify matches with cross-check
      - Filter by distance threshold
   
   3. **Homography**:
      - Need at least 4 point pairs
      - More points = better estimate
      - Filter outliers with RANSAC
      - Check condition number
   
   ## Applications
   
   - **Object recognition**: SIFT/ORB features + matching
   - **Image stitching**: SIFT features + homography
   - **SLAM**: FAST corners + tracking
   - **Augmented reality**: Homography estimation
   - **Motion tracking**: Corner detection + optical flow
   - **Face detection**: Cascade + feature matching
   - **3D reconstruction**: Feature matching + triangulation
   - **Image registration**: Feature-based alignment
   
   See also:
   - ArrayFire vision documentation: https://arrayfire.org/docs/group__vision__func__fast.htm
   - Image processing functions for preprocessing
   - BLAS operations for geometric transformations"
  (:require [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.c-api.fast :as fast]
            [org.soulspace.arrayfire.ffi.c-api.harris :as harris]
            [org.soulspace.arrayfire.ffi.c-api.orb :as orb]
            [org.soulspace.arrayfire.ffi.c-api.sift :as sift]
            [org.soulspace.arrayfire.ffi.c-api.hamming :as hamming]
            [org.soulspace.arrayfire.ffi.c-api.nearest-neighbour :as nn]
            [org.soulspace.arrayfire.ffi.c-api.match-template :as match-template]
            [org.soulspace.arrayfire.ffi.c-api.dog :as dog]
            [org.soulspace.arrayfire.ffi.c-api.homography :as homography-ffi]
            [org.soulspace.arrayfire.integration.jvm-integration :as jvm])
  (:import (org.soulspace.arrayfire.integration.jvm_integration AFArray)))

;;;
;;; Match Type Constants
;;;

(def MATCH_SAD
  "Sum of Absolute Differences (SAD) - fast, simple matching.
   
   d(a,b) = Σ|aᵢ - bᵢ|
   
   Lower values = better match."
  0)

(def MATCH_ZSAD
  "Zero-mean SAD - robust to brightness changes.
   
   Subtracts mean from patches before computing SAD.
   Lower values = better match."
  1)

(def MATCH_LSAD
  "Locally scaled SAD - robust to contrast changes.
   
   Normalizes patches by local standard deviation.
   Lower values = better match."
  2)

(def MATCH_SSD
  "Sum of Squared Differences (SSD) - emphasizes large errors.
   
   d(a,b) = Σ(aᵢ - bᵢ)²
   
   Lower values = better match."
  3)

(def MATCH_ZSSD
  "Zero-mean SSD - robust to brightness changes.
   
   Subtracts mean from patches before computing SSD.
   Lower values = better match."
  4)

(def MATCH_LSSD
  "Locally scaled SSD - robust to contrast changes.
   
   Normalizes patches by local standard deviation.
   Lower values = better match."
  5)

(def MATCH_NCC
  "Normalized Cross-Correlation (NCC) - robust to brightness and contrast.
   
   r(a,b) = Σ(aᵢ × bᵢ) / √(Σaᵢ² × Σbᵢ²)
   
   Higher values = better match. Range: [-1, 1]."
  6)

(def MATCH_ZNCC
  "Zero-mean NCC - most robust, compensates for brightness and contrast.
   
   Uses normalized cross-correlation with mean-subtracted patches.
   Higher values = better match. Range: [-1, 1]."
  7)

;;;
;;; Homography Type Constants
;;;

(def HOMOGRAPHY_RANSAC
  "RANSAC (Random Sample Consensus) homography estimation.
   
   Robust to outliers up to ~80% outlier ratio.
   Iteratively fits model to random subsets.
   Standard choice for most applications."
  0)

(def HOMOGRAPHY_LMEDS
  "LMedS (Least Median of Squares) homography estimation.
   
   Robust to <50% outliers.
   Minimizes median of squared residuals.
   More deterministic than RANSAC."
  1)

;;;
;;; Feature Detection (Corners)
;;;

(defn fast
  "Detect FAST (Features from Accelerated Segment Test) corners.
   
   FAST is a high-speed corner detection algorithm that identifies pixels
   where intensity changes significantly on a circular arc. It's one of the
   fastest corner detectors available.
   
   Algorithm:
   1. Consider 16-pixel circle (Bresenham circle of radius 3) around candidate
   2. Pixel p is a corner if n contiguous pixels are all brighter or darker
      than p by threshold thr
   3. Default n = 9 (FAST-9), can use 12 (FAST-12) for fewer features
   
   Parameters:
   - in: Input grayscale image (AFArray), typically uint8
   - thr: Intensity threshold for corner detection (default 20.0)
          Higher = fewer, stronger corners
   - arc-length: Number of contiguous pixels required (default 9)
                 Valid: 9, 11, 12, 16
   - non-max: Apply non-maximum suppression (default true)
              Removes adjacent corner responses
   - feature-ratio: Maximum fraction of pixels to return as features (default 0.05 = 5%)
   - edge: Border width to exclude from detection (default 3)
   
   Returns:
   af_features struct handle (opaque pointer)
   Use with af-get-features-* functions to extract data
   
   Features structure contains:
   - n: Number of features detected
   - x: X coordinates (float array)
   - y: Y coordinates (float array)
   - score: Corner response scores (float array)
   - orientation: Feature orientations (float array, 0.0 for FAST)
   - size: Feature sizes (float array, 1.0 for FAST)
   
   Example:
   ```clojure
   ;; Basic corner detection
   (let [img (array/load-image \"scene.jpg\")
         gray (colorspace/rgb-to-gray img)
         features (fast gray 20.0 9 true 0.05 3)]
     features)
   
   ;; More selective (fewer corners)
   (let [features (fast gray 30.0 12 true 0.02 3)]
     features)
   
   ;; Fast tracking (more corners, no suppression)
   (let [features (fast gray 15.0 9 false 0.1 3)]
     features)
   ```
   
   Performance:
   - Very fast: ~1ms for 640×480 on GPU
   - Real-time capable even on mobile devices
   - Linear complexity in image size
   
   Use cases:
   - Video tracking and SLAM
   - Real-time applications
   - Initial feature detection before descriptor extraction
   - Motion detection
   
   Limitations:
   - No rotation invariance
   - No scale invariance
   - No descriptors (corners only)
   - Can respond to edges with non-max disabled
   
   See also:
   - harris: More selective corner detector
   - orb: FAST corners with ORB descriptors
   - susan: Alternative corner detector"
  ([^AFArray in]
   (fast in 20.0 9 true 0.05 3))
  ([^AFArray in thr]
   (fast in thr 9 true 0.05 3))
  ([^AFArray in thr arc-length]
   (fast in thr arc-length true 0.05 3))
  ([^AFArray in thr arc-length non-max]
   (fast in thr arc-length non-max 0.05 3))
  ([^AFArray in thr arc-length non-max feature-ratio]
   (fast in thr arc-length non-max feature-ratio 3))
  ([^AFArray in thr arc-length non-max feature-ratio edge]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (fast/af-fast out (jvm/af-handle in) (float thr) (int arc-length)
                               (if non-max 1 0) (float feature-ratio) (int edge))
                 "af-fast")
     (jvm/deref-af-array out))))

(defn harris
  "Detect Harris corners.
   
   Harris corner detector uses eigenvalues of the structure tensor (second
   moment matrix) to identify corners. It's more selective than FAST and
   provides better corner localization.
   
   Algorithm:
   1. Compute image gradients (Ix, Iy)
   2. Build structure tensor M = Gaussian * [[Ix², IxIy], [IxIy, Iy²]]
   3. Compute corner response: R = det(M) - k×trace(M)²
   4. Threshold R and apply non-maximum suppression
   
   Corner response R:
   - R > 0: corner
   - R < 0: edge
   - R ≈ 0: flat region
   
   Parameters:
   - in: Input grayscale image (AFArray)
   - max-corners: Maximum number of corners to detect (default 500)
   - min-response: Minimum corner response threshold (default 1e6)
                   Higher = fewer, stronger corners
   - sigma: Gaussian smoothing parameter (default 1.0)
            Larger = detects corners at larger scales
   - block-size: Neighborhood size for gradient computation (default 3)
                 Typical: 3, 5, 7
   - k-thr: Harris detector sensitivity parameter k (default 0.04)
            Range: [0.04, 0.06]
            Lower = more sensitive to corners
   
   Returns:
   af_features struct handle containing:
   - n: Number of corners
   - x, y: Corner coordinates
   - score: Corner response values
   - orientation: 0.0 (Harris has no orientation)
   - size: Feature scale based on sigma
   
   Example:
   ```clojure
   ;; Basic Harris corners
   (let [img (array/load-image \"scene.jpg\")
         gray (colorspace/rgb-to-gray img)
         corners (harris gray 500 1e6 1.0 3 0.04)]
     corners)
   
   ;; Multi-scale corners (larger sigma)
   (let [corners (harris gray 300 1e6 2.0 5 0.04)]
     corners)
   
   ;; High-quality corners (higher threshold)
   (let [corners (harris gray 100 1e7 1.0 3 0.04)]
     corners)
   ```
   
   Performance:
   - Slower than FAST (gradient computation + Gaussian)
   - Still real-time capable on GPU (~5-10ms for 640×480)
   
   Use cases:
   - Accurate corner localization
   - Structure from motion
   - Image registration
   - When FAST produces too many false positives
   
   See also:
   - fast: Faster corner detection
   - orb: Features with descriptors"
  ([^AFArray in]
   (harris in 500 1e6 1.0 3 0.04))
  ([^AFArray in max-corners]
   (harris in max-corners 1e6 1.0 3 0.04))
  ([^AFArray in max-corners min-response]
   (harris in max-corners min-response 1.0 3 0.04))
  ([^AFArray in max-corners min-response sigma]
   (harris in max-corners min-response sigma 3 0.04))
  ([^AFArray in max-corners min-response sigma block-size]
   (harris in max-corners min-response sigma block-size 0.04))
  ([^AFArray in max-corners min-response sigma block-size k-thr]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (harris/af-harris out (jvm/af-handle in) (int max-corners)
                                   (float min-response) (float sigma)
                                   (int block-size) (float k-thr))
                 "af-harris")
     (jvm/deref-af-array out))))

;;;
;;; Feature Descriptors
;;;

(defn orb
  "Extract ORB (Oriented FAST and Rotated BRIEF) features and descriptors.
   
   ORB combines FAST keypoint detection with BRIEF descriptors, adding
   rotation invariance and improving performance. It's one of the fastest
   feature extraction algorithms with descriptors.
   
   Algorithm:
   1. Detect FAST corners at multiple scales (image pyramid)
   2. Compute orientation using intensity centroid
   3. Extract rotated BRIEF descriptors (256 bits)
   4. Apply orientation normalization
   
   Binary Descriptor:
   - 256 bits (32 bytes)
   - Each bit: result of intensity comparison
   - Rotation invariant (steered by keypoint orientation)
   - Very fast to compute and match (Hamming distance)
   
   Parameters:
   - in: Input grayscale image (AFArray)
   - fast-thr: FAST corner threshold (default 20.0)
               Lower = more features
   - max-feat: Maximum features to extract (default 400)
   - scl-fctr: Pyramid scale factor (default 1.5)
               Larger = fewer pyramid levels
   - levels: Number of pyramid levels (default 4)
             More = better scale invariance, slower
   - blur-img: Apply Gaussian blur before detection (default true)
               Reduces noise
   
   Returns:
   Vector of [features descriptors]:
   - features: af_features handle with keypoint locations, orientations, scales
   - descriptors: AFArray of binary descriptors (Nx32 uint8 array)
                  N = number of features, 32 bytes = 256 bits per feature
   
   Example:
   ```clojure
   ;; Basic ORB extraction
   (let [img (array/load-image \"scene.jpg\")
         gray (colorspace/rgb-to-gray img)
         [features desc] (orb gray)]
     {:features features :descriptors desc})
   
   ;; More features with finer scale
   (let [[feat desc] (orb gray 15.0 1000 1.2 6 true)]
     [feat desc])
   
   ;; Fast extraction for tracking
   (let [[feat desc] (orb gray 20.0 200 2.0 3 false)]
     [feat desc])
   
   ;; Matching between images
   (let [[f1 d1] (orb img1)
         [f2 d2] (orb img2)
         [idx dist] (hamming-matcher d1 d2 0 2)]
     {:indices idx :distances dist})
   ```
   
   Performance:
   - Very fast: ~10-20ms for 640×480 on GPU
   - Faster than SIFT/SURF by 1-2 orders of magnitude
   - Real-time capable
   
   Descriptor matching:
   - Use hamming-matcher for binary descriptors
   - Hamming distance = count of differing bits
   - XOR + popcount = extremely fast
   
   Use cases:
   - Real-time object recognition
   - Visual SLAM
   - Image stitching
   - Mobile applications (low memory, fast)
   - Video stabilization
   
   Advantages:
   - Free and open (vs. SIFT/SURF patents)
   - Rotation invariant
   - Some scale invariance (pyramid)
   - Very fast computation and matching
   - Low memory footprint
   
   Limitations:
   - Less distinctive than SIFT
   - Less robust to large scale changes
   - Not affine invariant
   
   See also:
   - sift: More distinctive, slower
   - gloh: Even more distinctive
   - hamming-matcher: Match ORB descriptors
   - fast: Corner detection only"
  ([^AFArray in]
   (orb in 20.0 400 1.5 4 true))
  ([^AFArray in fast-thr]
   (orb in fast-thr 400 1.5 4 true))
  ([^AFArray in fast-thr max-feat]
   (orb in fast-thr max-feat 1.5 4 true))
  ([^AFArray in fast-thr max-feat scl-fctr]
   (orb in fast-thr max-feat scl-fctr 4 true))
  ([^AFArray in fast-thr max-feat scl-fctr levels]
   (orb in fast-thr max-feat scl-fctr levels true))
  ([^AFArray in fast-thr max-feat scl-fctr levels blur-img]
   (let [feat-ptr (jvm/native-af-array-pointer)
         desc-ptr (jvm/native-af-array-pointer)]
     (jvm/check! (orb/af-orb feat-ptr desc-ptr (jvm/af-handle in)
                             (float fast-thr) (int max-feat) (float scl-fctr)
                             (int levels) (if blur-img 1 0))
                 "af-orb")
     [(jvm/deref-af-array feat-ptr)
      (jvm/af-array-new (jvm/deref-af-array desc-ptr))])))

(defn sift
  "Extract SIFT (Scale-Invariant Feature Transform) features and descriptors.
   
   SIFT is the gold standard for feature extraction, providing highly
   distinctive descriptors that are invariant to scale, rotation, and
   partially invariant to illumination and viewpoint changes.
   
   Algorithm:
   1. Build scale-space pyramid (Gaussian pyramid + DOG)
   2. Detect extrema in scale-space
   3. Refine keypoint localization (sub-pixel, sub-scale)
   4. Assign dominant orientation(s) to each keypoint
   5. Extract 128-D gradient histogram descriptor
   
   Descriptor:
   - 128-dimensional float vector
   - 4×4 spatial bins × 8 orientation bins
   - Normalized for illumination invariance
   - Highly distinctive
   
   Parameters:
   - in: Input grayscale image (AFArray)
   - n-layers: Number of layers per octave (default 3)
               More = better scale sampling
   - contrast-thr: Contrast threshold for keypoint detection (default 0.04)
                   Higher = fewer, stronger features
   - edge-thr: Edge response threshold (default 10.0)
               Higher = fewer edge responses, more corners
   - init-sigma: Initial Gaussian smoothing (default 1.6)
   - double-input: Upsample image 2× before processing (default false)
                   Detects features at finer scales, slower
   - intensity-scale: Descriptor normalization factor (default 0.00390625 = 1/256)
   - feature-ratio: Max fraction of pixels as features (default 0.05)
   
   Returns:
   Vector of [features descriptors]:
   - features: af_features handle with locations, orientations, scales
   - descriptors: AFArray of SIFT descriptors (Nx128 float array)
                  N = number of features
   
   Example:
   ```clojure
   ;; Basic SIFT extraction
   (let [img (array/load-image \"scene.jpg\")
         gray (colorspace/rgb-to-gray img)
         [features desc] (sift gray)]
     {:features features :descriptors desc})
   
   ;; High-quality features (fewer, stronger)
   (let [[feat desc] (sift gray 3 0.06 10.0 1.6 false)]
     [feat desc])
   
   ;; Fine-scale features (upsampled image)
   (let [[feat desc] (sift gray 3 0.04 10.0 1.6 true)]
     [feat desc])
   
   ;; Matching with nearest neighbor search
   (let [[f1 d1] (sift img1)
         [f2 d2] (sift img2)
         [idx dist] (nearest-neighbour d1 d2 0 2 :ssd)]
     {:indices idx :distances dist})
   ```
   
   Performance:
   - Slower than ORB (~100-200ms for 640×480)
   - High computational cost (scale-space + descriptor)
   - Not real-time on CPU, feasible on GPU
   
   Descriptor matching:
   - Use nearest-neighbour with SSD distance
   - Apply ratio test: d1/d2 < 0.8 (Lowe's criterion)
   - Euclidean distance in 128-D space
   
   Use cases:
   - Object recognition
   - Image stitching and panoramas
   - 3D reconstruction
   - Image retrieval
   - When quality > speed
   
   Advantages:
   - Highly distinctive (low false positive rate)
   - Scale and rotation invariant
   - Robust to illumination changes
   - Good for wide-baseline matching
   
   Limitations:
   - Computationally expensive
   - Not real-time on CPU
   - Larger descriptor (128 float vs 32 bytes for ORB)
   - Patent issues (expired 2020)
   
   See also:
   - gloh: More distinctive variant
   - orb: Faster binary descriptor
   - nearest-neighbour: Match SIFT descriptors"
  ([^AFArray in]
   (sift in 3 0.04 10.0 1.6 false 0.00390625 0.05))
  ([^AFArray in n-layers]
   (sift in n-layers 0.04 10.0 1.6 false 0.00390625 0.05))
  ([^AFArray in n-layers contrast-thr]
   (sift in n-layers contrast-thr 10.0 1.6 false 0.00390625 0.05))
  ([^AFArray in n-layers contrast-thr edge-thr]
   (sift in n-layers contrast-thr edge-thr 1.6 false 0.00390625 0.05))
  ([^AFArray in n-layers contrast-thr edge-thr init-sigma]
   (sift in n-layers contrast-thr edge-thr init-sigma false 0.00390625 0.05))
  ([^AFArray in n-layers contrast-thr edge-thr init-sigma double-input]
   (sift in n-layers contrast-thr edge-thr init-sigma double-input 0.00390625 0.05))
  ([^AFArray in n-layers contrast-thr edge-thr init-sigma double-input intensity-scale]
   (sift in n-layers contrast-thr edge-thr init-sigma double-input intensity-scale 0.05))
  ([^AFArray in n-layers contrast-thr edge-thr init-sigma double-input intensity-scale feature-ratio]
   (let [feat-ptr (jvm/native-af-array-pointer)
         desc-ptr (jvm/native-af-array-pointer)]
     (jvm/check! (sift/af-sift feat-ptr desc-ptr (jvm/af-handle in)
                               (int n-layers) (float contrast-thr) (float edge-thr)
                               (float init-sigma) (if double-input 1 0)
                               (float intensity-scale) (float feature-ratio))
                 "af-sift")
     [(jvm/deref-af-array feat-ptr)
      (jvm/af-array-new (jvm/deref-af-array desc-ptr))])))

(defn gloh
  "Extract GLOH (Gradient Location and Orientation Histogram) descriptors.
   
   GLOH is an extension of SIFT using a log-polar spatial grid instead of
   the regular Cartesian grid. This makes descriptors more distinctive,
   particularly for complex scenes.
   
   Differences from SIFT:
   - Log-polar spatial bins (3 radial × 8 angular = 17 regions)
   - 272-dimensional descriptor (17 regions × 16 orientation bins)
   - More distinctive than SIFT
   - Better for scenes with radial structure
   
   Parameters: Same as SIFT
   - in: Input grayscale image (AFArray)
   - n-layers: Layers per octave (default 3)
   - contrast-thr: Contrast threshold (default 0.04)
   - edge-thr: Edge threshold (default 10.0)
   - init-sigma: Initial smoothing (default 1.6)
   - double-input: Upsample 2× (default false)
   - intensity-scale: Normalization (default 0.00390625)
   - feature-ratio: Max feature fraction (default 0.05)
   
   Returns:
   Vector of [features descriptors]:
   - features: af_features handle
   - descriptors: AFArray (Nx272 float array)
   
   Example:
   ```clojure
   ;; GLOH extraction
   (let [img (array/load-image \"complex-scene.jpg\")
         gray (colorspace/rgb-to-gray img)
         [features desc] (gloh gray)]
     {:features features :descriptors desc})
   
   ;; Matching
   (let [[f1 d1] (gloh img1)
         [f2 d2] (gloh img2)
         [idx dist] (nearest-neighbour d1 d2 0 2 :ssd)]
     [idx dist])
   ```
   
   Performance:
   - Similar to SIFT (scale-space construction)
   - Slightly slower descriptor computation (more bins)
   - Larger descriptor size (272 vs 128)
   
   Use cases:
   - Complex scenes requiring high distinctiveness
   - When SIFT produces ambiguous matches
   - Object recognition in cluttered environments
   - Fine-grained recognition tasks
   
   See also:
   - sift: Standard SIFT (128-D, faster)
   - nearest-neighbour: Match GLOH descriptors"
  ([^AFArray in]
   (gloh in 3 0.04 10.0 1.6 false 0.00390625 0.05))
  ([^AFArray in n-layers]
   (gloh in n-layers 0.04 10.0 1.6 false 0.00390625 0.05))
  ([^AFArray in n-layers contrast-thr]
   (gloh in n-layers contrast-thr 10.0 1.6 false 0.00390625 0.05))
  ([^AFArray in n-layers contrast-thr edge-thr]
   (gloh in n-layers contrast-thr edge-thr 1.6 false 0.00390625 0.05))
  ([^AFArray in n-layers contrast-thr edge-thr init-sigma]
   (gloh in n-layers contrast-thr edge-thr init-sigma false 0.00390625 0.05))
  ([^AFArray in n-layers contrast-thr edge-thr init-sigma double-input]
   (gloh in n-layers contrast-thr edge-thr init-sigma double-input 0.00390625 0.05))
  ([^AFArray in n-layers contrast-thr edge-thr init-sigma double-input intensity-scale]
   (gloh in n-layers contrast-thr edge-thr init-sigma double-input intensity-scale 0.05))
  ([^AFArray in n-layers contrast-thr edge-thr init-sigma double-input intensity-scale feature-ratio]
   (let [feat-ptr (jvm/native-af-array-pointer)
         desc-ptr (jvm/native-af-array-pointer)]
     (jvm/check! (sift/af-gloh feat-ptr desc-ptr (jvm/af-handle in)
                               (int n-layers) (float contrast-thr) (float edge-thr)
                               (float init-sigma) (if double-input 1 0)
                               (float intensity-scale) (float feature-ratio))
                 "af-gloh")
     [(jvm/deref-af-array feat-ptr)
      (jvm/af-array-new (jvm/deref-af-array desc-ptr))])))

;;;
;;; Feature Matching
;;;

(defn hamming-matcher
  "Match binary descriptors using Hamming distance.
   
   Hamming distance counts the number of differing bits between two binary
   strings. It's the optimal distance metric for binary descriptors like
   ORB, BRIEF, BRISK, and FREAK.
   
   Hamming distance: d_H(a,b) = popcount(a XOR b)
   
   For each query descriptor, finds the n_dist nearest neighbors in the
   training set based on Hamming distance.
   
   Parameters:
   - query: Query descriptors (AFArray, Nq × D uint8)
           Each row is one descriptor
   - train: Training descriptors (AFArray, Nt × D uint8)
           Database to search
   - dist-dim: Dimension along which to compute distance (default 0)
               0 = match across rows
   - n-dist: Number of nearest neighbors to return (default 1)
             1 = best match, 2 = for ratio test, etc.
   
   Returns:
   Vector of [indices distances]:
   - indices: AFArray of neighbor indices (Nq × n_dist int)
             indices[i,j] = index in train of j-th nearest to query[i]
   - distances: AFArray of Hamming distances (Nq × n_dist int)
               distances[i,j] = Hamming distance to j-th nearest
   
   Example:
   ```clojure
   ;; 1-to-1 matching
   (let [[f1 d1] (orb img1)
         [f2 d2] (orb img2)
         [idx dist] (hamming-matcher d1 d2 0 1)]
     ;; idx[i] = index of best match in d2 for d1[i]
     ;; dist[i] = Hamming distance
     )
   
   ;; k-NN matching (for ratio test)
   (let [[idx dist] (hamming-matcher d1 d2 0 2)]
     ;; idx[i,0] = best match, idx[i,1] = second best
     ;; Apply Lowe's ratio test: dist[i,0] / dist[i,1] < 0.8
     )
   
   ;; Cross-check (mutual nearest neighbors)
   (let [[idx12 dist12] (hamming-matcher d1 d2 0 1)
         [idx21 dist21] (hamming-matcher d2 d1 0 1)]
     ;; Keep matches where idx12[i] = j AND idx21[j] = i
     )
   ```
   
   Performance:
   - Extremely fast: ~1-5ms for 1000 features
   - Hamming distance = XOR + popcount (hardware accelerated)
   - Much faster than Euclidean distance for binary descriptors
   
   Matching strategies:
   
   **1-to-1 (n-dist=1)**:
   - Simplest matching
   - May include ambiguous matches
   - Filter by distance threshold
   
   **Ratio Test (n-dist=2)**:
   - Lowe's ratio test: d1/d2 < threshold (typically 0.7-0.8)
   - Rejects ambiguous matches
   - Reduces false positives significantly
   
   **Cross-Check**:
   - Match query→train and train→query
   - Keep only mutual nearest neighbors
   - Very conservative, fewer matches
   
   Typical Hamming distances (ORB, 256 bits):
   - Good match: 0-30
   - Acceptable: 30-60
   - Poor: 60+
   - Random: ~128 (half bits different)
   
   Use cases:
   - ORB feature matching
   - Real-time object recognition
   - SLAM feature tracking
   - Any binary descriptor matching
   
   See also:
   - orb: Extract binary descriptors
   - nearest-neighbour: For floating-point descriptors"
  ([^AFArray query ^AFArray train]
   (hamming-matcher query train 0 1))
  ([^AFArray query ^AFArray train dist-dim]
   (hamming-matcher query train dist-dim 1))
  ([^AFArray query ^AFArray train dist-dim n-dist]
   (let [idx-ptr (jvm/native-af-array-pointer)
         dist-ptr (jvm/native-af-array-pointer)]
     (jvm/check! (hamming/af-hamming-matcher idx-ptr dist-ptr
                                             (jvm/af-handle query) (jvm/af-handle train)
                                             (long dist-dim) (int n-dist))
                 "af-hamming-matcher")
     [(jvm/af-array-new (jvm/deref-af-array idx-ptr))
      (jvm/af-array-new (jvm/deref-af-array dist-ptr))])))

(defn nearest-neighbour
  "Match floating-point descriptors using nearest neighbor search.
   
   Finds nearest neighbors for each query descriptor in the training set
   using SSD (sum of squared differences) or SAD (sum of absolute differences).
   
   Distance metrics:
   - SSD: d(a,b) = Σ(aᵢ - bᵢ)² (emphasizes large errors)
   - SAD: d(a,b) = Σ|aᵢ - bᵢ| (more robust to outliers)
   
   Parameters:
   - query: Query descriptors (AFArray, Nq × D float)
   - train: Training descriptors (AFArray, Nt × D float)
   - dist-dim: Dimension along which to compute distance (default 0)
   - n-dist: Number of nearest neighbors (default 1)
   - dist-type: Distance metric (default :ssd)
                :ssd - Sum of squared differences
                :sad - Sum of absolute differences
   
   Returns:
   Vector of [indices distances]:
   - indices: AFArray of neighbor indices (Nq × n_dist int)
   - distances: AFArray of distances (Nq × n_dist float)
   
   Example:
   ```clojure
   ;; SIFT matching with ratio test
   (let [[f1 d1] (sift img1)
         [f2 d2] (sift img2)
         [idx dist] (nearest-neighbour d1 d2 0 2 :ssd)]
     ;; Apply Lowe's ratio test
     ;; Keep matches where dist[i,0]/dist[i,1] < 0.8
     )
   
   ;; GLOH matching with SAD
   (let [[f1 d1] (gloh img1)
         [f2 d2] (gloh img2)
         [idx dist] (nearest-neighbour d1 d2 0 1 :sad)]
     [idx dist])
   ```
   
   Performance:
   - Slower than Hamming matcher (floating-point ops)
   - ~10-50ms for 1000 features depending on descriptor size
   - SSD slightly faster than SAD (better GPU optimization)
   
   Ratio test (Lowe's criterion):
   ```clojure
   (let [[idx dist] (nearest-neighbour query train 0 2 :ssd)
         ratio-threshold 0.8]
     ;; Filter matches
     (filter (fn [i]
               (< (/ (aget dist i 0) (aget dist i 1))
                  ratio-threshold))
             (range (count query))))
   ```
   
   Use cases:
   - SIFT descriptor matching
   - GLOH descriptor matching
   - Any floating-point descriptor
   - When descriptors are not binary
   
   See also:
   - sift: Extract SIFT descriptors
   - gloh: Extract GLOH descriptors
   - hamming-matcher: For binary descriptors"
  ([^AFArray query ^AFArray train]
   (nearest-neighbour query train 0 1 :ssd))
  ([^AFArray query ^AFArray train dist-dim]
   (nearest-neighbour query train dist-dim 1 :ssd))
  ([^AFArray query ^AFArray train dist-dim n-dist]
   (nearest-neighbour query train dist-dim n-dist :ssd))
  ([^AFArray query ^AFArray train dist-dim n-dist dist-type]
   (let [idx-ptr (jvm/native-af-array-pointer)
         dist-ptr (jvm/native-af-array-pointer)
         type-val (case dist-type
                    :ssd 0    ; AF_SSD
                    :sad 1    ; AF_SAD
                    0)]
     (jvm/check! (nn/af-nearest-neighbour idx-ptr dist-ptr
                                          (jvm/af-handle query) (jvm/af-handle train)
                                          (long dist-dim) (int n-dist) (int type-val))
                 "af-nearest-neighbour")
     [(jvm/af-array-new (jvm/deref-af-array idx-ptr))
      (jvm/af-array-new (jvm/deref-af-array dist-ptr))])))

;;;
;;; Template Matching
;;;

(defn match-template
  "Find occurrences of a template image within a search image.
   
   Slides the template over the search image and computes a similarity/
   distance measure at each location. Returns a correlation map where
   peaks indicate potential matches.
   
   Match types:
   - SAD/SSD: Difference-based (lower = better match)
   - NCC: Correlation-based (higher = better match)
   - Z-prefix: Zero-mean (robust to brightness)
   - L-prefix: Locally scaled (robust to contrast)
   
   Parameters:
   - search-img: Large image to search (AFArray)
   - template-img: Small template to find (AFArray)
   - match-type: Matching metric (keyword or integer)
                 :sad, :zsad, :lsad, :ssd, :zssd, :lssd, :ncc, :zncc
                 Or use MATCH_* constants
   
   Returns:
   AFArray correlation map of size:
   (search_h - template_h + 1) × (search_w - template_w + 1)
   
   Finding best match:
   - SAD/SSD: argmin (minimum value)
   - NCC/ZNCC: argmax (maximum value)
   
   Example:
   ```clojure
   ;; Find template in scene
   (let [scene (array/load-image \"scene.jpg\")
         template (array/load-image \"object.jpg\")
         result (match-template scene template :zncc)]
     ;; Find location of maximum
     (let [max-loc (array/imax result)]
       max-loc))
   
   ;; Multiple matches (find all above threshold)
   (let [result (match-template scene template :ncc)
         threshold 0.8
         matches (array/where (array/gt result threshold))]
     matches)
   
   ;; Fast matching with SAD
   (let [result (match-template scene template :sad)]
     (array/imin result))  ; Find minimum
   ```
   
   Performance:
   - Fast on GPU (FFT-based for large templates)
   - Spatial domain for small templates
   - ZNCC slowest but most robust
   - SAD/SSD fastest
   
   Tips:
   1. Convert to grayscale first (faster)
   2. Downscale for coarse search, refine at full resolution
   3. Use NCC/ZNCC for lighting variations
   4. Use SAD/SSD for speed
   5. Apply non-maximum suppression for multiple matches
   
   Use cases:
   - Object detection (known appearance)
   - Logo/symbol detection
   - Quality inspection
   - GUI automation (finding buttons)
   - Medical image registration
   
   Limitations:
   - No scale invariance
   - No rotation invariance
   - Assumes similar appearance
   - Slow for large templates (use features instead)
   
   See also:
   - orb/sift: For scale/rotation invariance
   - Features + matching: More robust to transformations"
  ([^AFArray search-img ^AFArray template-img]
   (match-template search-img template-img :sad))
  ([^AFArray search-img ^AFArray template-img match-type]
   (let [out (jvm/native-af-array-pointer)
         type-val (if (keyword? match-type)
                    (case match-type
                      :sad MATCH_SAD
                      :zsad MATCH_ZSAD
                      :lsad MATCH_LSAD
                      :ssd MATCH_SSD
                      :zssd MATCH_ZSSD
                      :lssd MATCH_LSSD
                      :ncc MATCH_NCC
                      :zncc MATCH_ZNCC
                      MATCH_SAD)
                    match-type)]
     (jvm/check! (match-template/af-match-template out
                                                   (jvm/af-handle search-img)
                                                   (jvm/af-handle template-img)
                                                   (int type-val))
                 "af-match-template")
     (jvm/af-array-new (jvm/deref-af-array out)))))

;;;
;;; Image Processing for Vision
;;;

(defn dog
  "Compute Difference of Gaussians (DOG).
   
   DOG approximates the Laplacian of Gaussian (LoG) operator, which is
   used for blob detection and edge enhancement. It's computed by subtracting
   two Gaussian-blurred versions of an image at different scales.
   
   DOG(x,y) = G(x,y,σ₁) - G(x,y,σ₂)
   
   Where G is Gaussian convolution and σ₁ < σ₂.
   
   Used in SIFT algorithm for scale-space extrema detection.
   
   Parameters:
   - in: Input image (AFArray)
   - radius1: Radius for first Gaussian (smaller, default 3)
   - radius2: Radius for second Gaussian (larger, default 6)
   
   Returns:
   AFArray with difference of Gaussians
   
   Example:
   ```clojure
   ;; Basic DOG
   (let [img (array/load-image \"scene.jpg\")
         gray (colorspace/rgb-to-gray img)
         dog-img (dog gray 3 6)]
     dog-img)
   
   ;; Fine-scale blob detection
   (let [dog-img (dog gray 2 4)]
     dog-img)
   
   ;; Coarse-scale blob detection
   (let [dog-img (dog gray 5 10)]
     dog-img)
   ```
   
   Properties:
   - Positive response: dark blob on light background
   - Negative response: light blob on dark background
   - Zero: flat region
   
   Use cases:
   - Blob detection
   - Scale-space construction (SIFT)
   - Edge enhancement
   - Feature saliency
   
   See also:
   - sift: Uses DOG internally
   - Gaussian filter for smoothing"
  ([^AFArray in]
   (dog in 3 6))
  ([^AFArray in radius1]
   (dog in radius1 6))
  ([^AFArray in radius1 radius2]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (dog/af-dog out (jvm/af-handle in) (int radius1) (int radius2))
                 "af-dog")
     (jvm/af-array-new (jvm/deref-af-array out)))))

;;;
;;; Geometric Transformations
;;;

(defn homography
  "Estimate homography transformation between corresponding points.
   
   A homography H is a 3×3 matrix that maps points from one plane to another.
   It's used for image stitching, augmented reality, and geometric correction.
   
   Homography equation (in homogeneous coordinates):
   [x']       [x]
   [y'] = H * [y]
   [1 ]       [1]
   
   Or in Cartesian: x' = (h₁₁x + h₁₂y + h₁₃) / (h₃₁x + h₃₂y + h₃₃)
                    y' = (h₂₁x + h₂₂y + h₂₃) / (h₃₁x + h₃₂y + h₃₃)
   
   Parameters:
   - x-src: Source X coordinates (AFArray, N floats)
   - y-src: Source Y coordinates (AFArray, N floats)
   - x-dst: Destination X coordinates (AFArray, N floats)
   - y-dst: Destination Y coordinates (AFArray, N floats)
   - htype: Estimation method (default :ransac)
            :ransac - Robust to outliers, standard choice
            :lmeds - Least median of squares, <50% outliers
            Or use HOMOGRAPHY_* constants
   - inlier-thr: RANSAC inlier threshold in pixels (default 3.0)
                 Larger = more inliers, less accurate
   - iterations: RANSAC iterations (default 1000)
                 More = better with more outliers
   - dtype: Output data type (default f32)
   
   Returns:
   Vector of [H inliers]:
   - H: Homography matrix (AFArray, 3×3)
   - inliers: Number of inlier points (int)
   
   Minimum points:
   - Need at least 4 point correspondences
   - More points = better, more robust estimate
   - RANSAC handles outliers automatically
   
   Example:
   ```clojure
   ;; From matched features
   (let [[f1 d1] (sift img1)
         [f2 d2] (sift img2)
         [idx dist] (nearest-neighbour d1 d2 0 2 :ssd)
         ;; Extract coordinates from features
         x1 (feature-x-coords f1)
         y1 (feature-y-coords f1)
         x2 (feature-x-coords f2 idx)
         y2 (feature-y-coords f2 idx)
         [H inliers] (homography x1 y1 x2 y2 :ransac 3.0 1000)]
     {:homography H :inliers inliers})
   
   ;; Strict threshold (more accurate, fewer inliers)
   (let [[H inliers] (homography x1 y1 x2 y2 :ransac 1.0 2000)]
     [H inliers])
   
   ;; LMedS for moderate outliers
   (let [[H inliers] (homography x1 y1 x2 y2 :lmeds 3.0 1000)]
     [H inliers])
   ```
   
   RANSAC algorithm:
   1. Randomly sample 4 point pairs
   2. Compute homography from sample
   3. Count inliers (distance < threshold)
   4. Repeat for specified iterations
   5. Return homography with most inliers
   
   Using the homography:
   ```clojure
   ;; Warp image using homography
   (let [H (homography ...)
         warped (transform img H)]
     warped)
   
   ;; Transform points
   (let [[x' y'] (apply-homography H x y)]
     [x' y'])
   ```
   
   Applications:
   - **Image stitching**: Align overlapping images
   - **Augmented reality**: Project graphics onto planar surfaces
   - **Document rectification**: Correct perspective distortion
   - **Calibration**: Camera calibration from planar patterns
   - **Mosaicking**: Create panoramas
   
   Tips:
   1. Filter feature matches before RANSAC (ratio test)
   2. Use more iterations with more outliers
   3. Adjust threshold based on image resolution
   4. Check inlier count (low = bad estimate)
   5. Verify homography is well-conditioned
   
   See also:
   - Feature matching: Generate point correspondences
   - Transform functions: Apply homography to images"
  ([x-src y-src x-dst y-dst]
   (homography x-src y-src x-dst y-dst :ransac 3.0 1000 jvm/AF_DTYPE_F32))
  ([x-src y-src x-dst y-dst htype]
   (homography x-src y-src x-dst y-dst htype 3.0 1000 jvm/AF_DTYPE_F32))
  ([x-src y-src x-dst y-dst htype inlier-thr]
   (homography x-src y-src x-dst y-dst htype inlier-thr 1000 jvm/AF_DTYPE_F32))
  ([x-src y-src x-dst y-dst htype inlier-thr iterations]
   (homography x-src y-src x-dst y-dst htype inlier-thr iterations jvm/AF_DTYPE_F32))
  ([x-src y-src x-dst y-dst htype inlier-thr iterations dtype]
   (let [H-ptr (jvm/native-af-array-pointer)
         inliers-buf (mem/alloc-instance ::mem/int)
         type-val (if (keyword? htype)
                    (case htype
                      :ransac HOMOGRAPHY_RANSAC
                      :lmeds HOMOGRAPHY_LMEDS
                      HOMOGRAPHY_RANSAC)
                    htype)]
     (jvm/check! (homography-ffi/af-homography H-ptr inliers-buf
                                               (jvm/af-handle x-src) (jvm/af-handle y-src)
                                               (jvm/af-handle x-dst) (jvm/af-handle y-dst)
                                               (int type-val) (float inlier-thr)
                                               (int iterations) (int dtype))
                 "af-homography")
     [(jvm/af-array-new (jvm/deref-af-array H-ptr))
      (mem/read-int inliers-buf 0)])))

