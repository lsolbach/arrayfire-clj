(ns org.soulspace.arrayfire.ffi.c-api.susan
  "Bindings for ArrayFire SUSAN corner detector.
   
   SUSAN (Smallest Univalue Segment Assimilating Nucleus) is a corner and
   edge detection algorithm that operates on the principle of local intensity
   similarity. Unlike Harris, which uses derivatives, SUSAN uses a circular
   mask to compare pixel intensities, making it more robust to noise.
   
   Mathematical Foundation:
   
   SUSAN algorithm principle:
   
   For each pixel p with intensity I(p), define a circular region (USAN - 
   Univalue Segment Assimilating Nucleus) of radius r around it.
   
   For each pixel q in the circular region:
   - Compare I(q) with I(p)
   - If |I(q) - I(p)| < threshold, q belongs to USAN
   
   USAN area c(p):
   c(p) = Σ exp(-((I(q) - I(p))/t)^6)
   
   Where:
   - t is the intensity difference threshold (diff_thr)
   - The sum is over all pixels q in the circular mask
   - Exponential weighting provides smooth transitions
   
   Corner Response:
   
   R(p) = { g - c(p)  if c(p) < g
          { 0         otherwise
   
   Where:
   - g is the geometric threshold (typically 0.5 * circle_area)
   - R(p) is maximum when c(p) is minimum (corner)
   - R(p) = 0 when c(p) ≥ g (flat or edge)
   
   Interpretation:
   
   - **Small USAN** (c(p) << g): Corner detected
     Pixel is surrounded by many different intensities
     High response value
   
   - **Medium USAN** (c(p) ≈ g): Edge detected
     About half the mask has similar intensity
     Zero or low response
   
   - **Large USAN** (c(p) ≈ circle_area): Flat region
     Most pixels have similar intensity
     Zero response
   
   Algorithm Steps:
   
   1. **Define Circular Mask**:
      - Radius r specified by radius parameter
      - Typical values: 3-7 pixels
      - Larger radius → more smoothing, fewer features
   
   2. **For Each Pixel**:
      - Place circular mask centered at pixel
      - Compare intensity with nucleus (center pixel)
      - Weight by exponential of intensity difference
   
   3. **Calculate USAN Area**:
      - Sum weighted similarities
      - c(p) = number of similar pixels
      - Normalized by mask area
   
   4. **Compute Response**:
      - R(p) = max(0, g - c(p))
      - High response → corner
      - Low response → edge or flat
   
   5. **Non-Maximal Suppression**:
      - Keep only local maxima
      - Suppress weaker responses in neighborhood
   
   6. **Apply Thresholds**:
      - Filter by feature_ratio (keep top N%)
      - Or use geometric threshold
   
   Parameters Explained:
   
   **radius**:
   - Radius of circular mask in pixels
   - Range: 1 to 9 (must be < 10)
   - Must be ≤ edge parameter
   - Larger radius:
     * More smoothing
     * Fewer corners detected
     * More robust to noise
     * Slower computation
   - Typical values: 3-5
   
   **diff_thr**:
   - Intensity difference threshold
   - Range: > 0.0 (typically 10-50)
   - Controls sensitivity to intensity variations
   - Lower threshold:
     * More selective (stricter similarity)
     * Detects sharper corners
     * More sensitive to noise
   - Higher threshold:
     * Less selective (looser similarity)
     * Detects more corners
     * More robust to noise
   - Typical values:
     * 8-bit images: 15-30
     * Float images [0,1]: 0.05-0.15
   
   **geom_thr**:
   - Geometric threshold for corner response
   - Range: > 0.0 (typically 10-50)
   - Usually set to fraction of mask area:
     geom_thr = k * π * radius²
     where k ≈ 0.5
   - Lower threshold:
     * More corners detected
     * May include weaker corners
   - Higher threshold:
     * Fewer corners detected
     * Only strong corners
   - Typical values:
     * radius=3: geom_thr ≈ 14 (0.5 * π * 9)
     * radius=5: geom_thr ≈ 39 (0.5 * π * 25)
   
   **feature_ratio**:
   - Fraction of corners to retain
   - Range: (0.0, 1.0]
   - After computing all responses, keep top N%:
     * 1.0 = keep all corners above geom_thr
     * 0.1 = keep top 10% of corners
     * 0.01 = keep top 1% of corners
   - Use to limit number of features
   - Typical values: 0.05-0.20 (5-20%)
   
   **edge**:
   - Border size to exclude from detection
   - Range: ≥ radius
   - Pixels within edge pixels of image border ignored
   - Prevents mask from extending outside image
   - Must satisfy: edge ≥ radius
   - Typical value: edge = radius (minimum required)
   - Larger edge → smaller effective detection area
   
   Output Features:
   
   The function returns a features structure containing:
   - **n**: number of corners detected
   - **x**: x-coordinates of corners (float array)
   - **y**: y-coordinates of corners (float array)
   - **score**: SUSAN response values (float array)
   - **orientation**: always 0 (SUSAN doesn't compute orientation)
   - **size**: always 1 (SUSAN doesn't compute scale)
   
   Use Cases:
   
   1. **Noise-Robust Corner Detection**:
      - SUSAN excels in noisy images
      - No derivative computation (noise-sensitive)
      - Direct intensity comparison
      - Applications: medical imaging, low-light photography
   
   2. **Real-Time Feature Tracking**:
      - Fast computation (no convolution)
      - Simple circular mask
      - Efficient GPU implementation
      - Applications: video tracking, AR/VR
   
   3. **Edge Detection**:
      - SUSAN can also detect edges
      - Different threshold settings
      - Unified corner/edge framework
      - Applications: image segmentation
   
   4. **Structure Analysis**:
      - Analyze local image structure
      - Texture characterization
      - Pattern recognition
      - Applications: quality inspection, material science
   
   5. **Camera Calibration**:
      - Detect calibration pattern corners
      - Robust to lighting variations
      - Sub-pixel accuracy possible
      - Applications: computer vision setup
   
   Comparison with Other Detectors:
   
   | Feature        | SUSAN | Harris | FAST  | ORB   |
   |----------------|-------|--------|-------|-------|
   | Speed          | Fast  | Medium | Fast  | Fast  |
   | Noise Robust   | High  | Medium | Low   | Medium|
   | Derivatives    | No    | Yes    | No    | No    |
   | Sub-pixel      | Yes   | Yes    | No    | No    |
   | Scale Inv.     | No    | No     | No    | Yes   |
   | Rotation Inv.  | Yes   | Partial| No    | Yes   |
   
   Advantages over Harris:
   - More robust to noise (no derivatives)
   - Better localization accuracy
   - Unified corner/edge detection
   - Simpler computation (no eigenvalues)
   - Better performance on textured images
   
   Limitations:
   - Not scale invariant
   - No orientation computation
   - Parameters need tuning per image type
   - Fixed circular mask (not adaptive)
   
   Performance Considerations:
   
   **Computational Complexity**:
   - Per pixel: O(π * radius²) comparisons
   - Total: O(W × H × radius²)
   - No convolution required
   - GPU parallelizes well
   - Typical: 5-30ms for 640×480 on GPU
   
   **Memory Usage**:
   - Response array: 1 × image size
   - No derivative arrays needed
   - Circular mask: pre-computed (small)
   - Output features: N corners × 5 values
   
   **Optimization Tips**:
   1. Use smallest radius that works
   2. Adjust feature_ratio to limit output
   3. GPU provides 20-50× speedup
   4. Pre-filter very noisy images
   5. Set edge = radius (minimum required)
   
   Example Workflow:
   
   ```clojure
   (require '[org.soulspace.arrayfire.ffi.susan :as susan])
   (require '[coffi.mem :as mem])
   
   ;; Load grayscale image
   (def img (load-image \"noisy-image.jpg\" :grayscale true))
   
   ;; Detect corners with SUSAN
   (let [radius 3
         diff-thr 20.0
         geom-thr 14.0  ; ≈ 0.5 * π * radius²
         feature-ratio 0.1  ; keep top 10%
         edge 3  ; must be ≥ radius
         features-ptr (mem/alloc-instance ::mem/pointer)]
     
     ;; Call SUSAN detector
     (susan/af-susan features-ptr img radius diff-thr geom-thr
                     feature-ratio edge)
     
     ;; Extract and use features
     (let [features (mem/read-ptr features-ptr)
           n (get-feature-count features)
           x (get-feature-x features)
           y (get-feature-y features)
           scores (get-feature-scores features)]
       
       (println \"Detected\" n \"corners\")
       (process-corners x y scores)))
   ```
   
   Common Parameter Combinations:
   
   1. **Standard Detection**:
      - radius: 3
      - diff_thr: 20.0
      - geom_thr: 14.0
      - feature_ratio: 0.15
      - edge: 3
   
   2. **Noise-Robust**:
      - radius: 5
      - diff_thr: 30.0
      - geom_thr: 39.0
      - feature_ratio: 0.1
      - edge: 5
   
   3. **High Precision**:
      - radius: 3
      - diff_thr: 10.0
      - geom_thr: 10.0
      - feature_ratio: 0.05
      - edge: 3
   
   4. **Fast/Coarse**:
      - radius: 2
      - diff_thr: 25.0
      - geom_thr: 6.0
      - feature_ratio: 0.2
      - edge: 2
   
   Troubleshooting:
   
   **Too Few Corners**:
   - Increase diff_thr (less selective)
   - Decrease geom_thr (lower response threshold)
   - Increase feature_ratio (keep more features)
   - Decrease radius (finer detail)
   
   **Too Many Corners**:
   - Decrease diff_thr (more selective)
   - Increase geom_thr (higher response threshold)
   - Decrease feature_ratio (keep fewer features)
   - Increase radius (coarser features)
   
   **False Positives in Noisy Images**:
   - Despite noise robustness, very noisy images may need:
   - Increase radius (more smoothing)
   - Increase diff_thr (less sensitive)
   - Pre-filter with median or bilateral filter
   
   **Corners on Edges**:
   - Increase geom_thr
   - Decrease feature_ratio
   - SUSAN is inherently good at separating corners from edges
   
   **Missing Corners**:
   - Check if within edge boundary
   - Decrease geom_thr
   - Increase diff_thr
   - Image may have low contrast
   
   Mathematical Notes:
   
   The exponential weighting function:
   w(I(q), I(p)) = exp(-((I(q) - I(p))/t)^6)
   
   Properties:
   - Smooth falloff (6th power for sharp transitions)
   - w ≈ 1 when |I(q) - I(p)| << t (similar)
   - w ≈ 0 when |I(q) - I(p)| >> t (different)
   - t = diff_thr controls transition steepness
   
   USAN area interpretation:
   - Maximum c(p) ≈ π * radius² (all similar)
   - Minimum c(p) ≈ 0 (all different)
   - Corner: c(p) small (varies in all directions)
   - Edge: c(p) ≈ 0.5 * π * radius² (varies in one direction)
   - Flat: c(p) ≈ π * radius² (uniform intensity)
   
   See Also:
   - af_harris: Harris corner detector (derivative-based)
   - af_fast: FAST corner detector (very fast, less robust)
   - af_orb: ORB features (includes scale and rotation)
   - af_sift: SIFT features (scale-invariant)
   
   References:
   - Smith, S.M. and Brady, J.M. (1997)
     'SUSAN - A New Approach to Low Level Image Processing'
     International Journal of Computer Vision, 23(1), 45-78"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; af_err af_susan(af_features *out, const af_array in, const unsigned radius,
;;                 const float diff_thr, const float geom_thr,
;;                 const float feature_ratio, const unsigned edge)
(defcfn af-susan
  "SUSAN corner detector.
   
   Detects corners (and edges) in grayscale images using the SUSAN
   (Smallest Univalue Segment Assimilating Nucleus) algorithm. SUSAN
   is particularly robust to noise as it uses direct intensity comparison
   within a circular mask rather than derivative computation.
   
   Parameters:
   
   - out: af_features* output features structure containing:
     * n: number of corners detected
     * x: array of x-coordinates (float)
     * y: array of y-coordinates (float)
     * score: array of SUSAN response values (float)
     * orientation: array of zeros (SUSAN doesn't compute orientation)
     * size: array of ones (SUSAN doesn't compute scale)
   
   - in: af_array input grayscale image
     * Must be 2D (height × width)
     * Recommended: grayscale (color images should be converted)
     * Type: f32, f64, or integer types
     * Works with various data types (see susan.cpp)
   
   - radius: unsigned circular mask radius in pixels
     * Range: 1 to 9 (must be < 10)
     * Must satisfy: radius ≤ edge
     * Larger radius → more smoothing, fewer corners
     * Typical values: 3-5
     * Constraint: radius < 10 (enforced by ArrayFire)
   
   - diff_thr: float intensity difference threshold
     * Range: > 0.0
     * Controls sensitivity to intensity variations
     * Lower → more selective (sharper corners only)
     * Higher → less selective (more corners, noise robust)
     * Typical values:
       - 8-bit images: 15-30
       - Float [0,1]: 0.05-0.15
     * Must be positive (diff_thr > 0.0)
   
   - geom_thr: float geometric threshold for corner response
     * Range: > 0.0
     * Typical: ≈ 0.5 * π * radius²
     * Controls minimum corner strength
     * Lower → more corners (weaker corners accepted)
     * Higher → fewer corners (only strong corners)
     * Must be positive (geom_thr > 0.0)
   
   - feature_ratio: float fraction of corners to retain
     * Range: (0.0, 1.0]
     * Keeps top (feature_ratio × 100)% of detected corners
     * 1.0 = keep all corners above geom_thr
     * 0.1 = keep top 10% of corners
     * Typical values: 0.05-0.20
     * Must satisfy: 0.0 < feature_ratio ≤ 1.0
   
   - edge: unsigned border exclusion size in pixels
     * Range: ≥ radius
     * Pixels within edge pixels of border are ignored
     * Prevents circular mask from exceeding image bounds
     * Must satisfy: edge ≥ radius
     * Typical: edge = radius (minimum required)
     * Also requires: image dimensions ≥ (2*edge + 1)
   
   Returns:
   AF_SUCCESS (0) on success, error code otherwise:
   - AF_ERR_ARG: Invalid arguments (null pointers, invalid dimensions)
   - AF_ERR_SIZE: Invalid radius, edge, or image too small
   - AF_ERR_TYPE: Unsupported array type
   - AF_ERR_NO_MEM: Out of memory
   
   Validation (from susan.cpp):
   - dims.ndims() == 2 (must be 2D image)
   - radius < 10
   - radius ≤ edge
   - diff_thr > 0.0
   - geom_thr > 0.0
   - 0.0 < feature_ratio ≤ 1.0
   - dims[0] ≥ (2*edge + 1) AND dims[1] ≥ (2*edge + 1)
   
   Algorithm Overview:
   
   1. For each pixel (x, y):
      a. Place circular mask of radius r centered at (x, y)
      b. Compare intensity I(x,y) with all pixels in mask
      c. Weight by exp(-((I_mask - I_center)/diff_thr)^6)
      d. Sum weights to get USAN area c
   
   2. Compute corner response:
      R = max(0, geom_thr - c)
      High response indicates corner
   
   3. Non-maximal suppression:
      Keep only local maxima
   
   4. Apply feature_ratio threshold:
      Keep top (feature_ratio × 100)% of corners
   
   Parameter Selection Guide:
   
   **Standard Detection**:
   - radius = 3
   - diff_thr = 20.0
   - geom_thr = 14.0  ; ≈ 0.5 * π * 3²
   - feature_ratio = 0.15
   - edge = 3
   
   **Noise-Robust**:
   - radius = 5
   - diff_thr = 30.0
   - geom_thr = 39.0  ; ≈ 0.5 * π * 5²
   - feature_ratio = 0.1
   - edge = 5
   
   **High Precision**:
   - radius = 3
   - diff_thr = 10.0
   - geom_thr = 10.0
   - feature_ratio = 0.05
   - edge = 3
   
   **Fast Detection**:
   - radius = 2
   - diff_thr = 25.0
   - geom_thr = 6.0  ; ≈ 0.5 * π * 2²
   - feature_ratio = 0.2
   - edge = 2
   
   Use Cases:
   
   1. **Noisy Image Processing**:
      SUSAN excels where Harris struggles with noise
      No derivative computation → noise immunity
   
   2. **Medical Imaging**:
      Robust feature detection in low SNR images
      Reliable corner localization
   
   3. **Real-Time Tracking**:
      Fast GPU implementation
      Good repeatability across frames
   
   4. **Calibration Patterns**:
      Accurate corner detection in checkerboards
      Sub-pixel precision achievable
   
   5. **Texture Analysis**:
      Characterize local structure
      Edge and corner detection unified
   
   Example:
   ```clojure
   ;; Detect corners in noisy image
   (let [img (load-grayscale-image \"noisy.jpg\")
         features-ptr (mem/alloc-instance ::mem/pointer)]
     
     (af-susan features-ptr img
               3      ; radius
               20.0   ; diff_thr
               14.0   ; geom_thr (≈ 0.5 * π * radius²)
               0.15   ; feature_ratio (keep top 15%)
               3)     ; edge (= radius)
     
     (let [features (mem/read-ptr features-ptr)]
       (println \"Detected\" (get-feature-count features) \"corners\")
       (process-features features)))
   ```
   
   Performance:
   - Typical: 5-30ms for 640×480 on GPU
   - Scales with O(W × H × radius²)
   - No convolution → faster than Harris for small radius
   - GPU provides 20-50× speedup
   
   Advantages:
   - Excellent noise robustness
   - No derivative computation
   - Simple, interpretable algorithm
   - Fast for small radius values
   - Good localization accuracy
   - Unified corner/edge detection
   
   Limitations:
   - Not scale invariant (single-scale detection)
   - No orientation computation
   - Fixed circular mask (not adaptive)
   - Requires parameter tuning per image type
   - Performance degrades for large radius
   
   Troubleshooting:
   
   **No corners detected**:
   - Check edge parameter (edge ≥ radius)
   - Decrease geom_thr
   - Increase diff_thr (less selective)
   - Increase feature_ratio
   - Verify image is grayscale
   
   **Too many corners**:
   - Decrease diff_thr (more selective)
   - Increase geom_thr
   - Decrease feature_ratio
   - Increase radius (smoother)
   
   **Corners on edges not true corners**:
   - Increase geom_thr
   - Decrease feature_ratio
   - SUSAN naturally distinguishes corners from edges
   
   **Missing corners in noisy image**:
   - Increase radius (more smoothing)
   - Increase diff_thr (more tolerance)
   - Consider pre-filtering (median, bilateral)
   
   **Error: radius < 10 assertion**:
   - ArrayFire limits radius to < 10
   - Use radius ≤ 9
   - For larger smoothing, pre-filter image
   
   **Error: radius ≤ edge assertion**:
   - Increase edge to be ≥ radius
   - Or decrease radius
   - Minimum: edge = radius
   
   **Error: image too small**:
   - Image must be at least (2*edge+1) × (2*edge+1)
   - Reduce edge parameter
   - Or use larger image
   
   Comparison:
   
   **SUSAN vs Harris**:
   - SUSAN: Better noise robustness, no derivatives
   - Harris: Better mathematical foundation, rotation invariant
   - Choose SUSAN for noisy images
   - Choose Harris for clean images with strong features
   
   **SUSAN vs FAST**:
   - SUSAN: More robust, better localization
   - FAST: Much faster, less accurate
   - Choose SUSAN for quality
   - Choose FAST for real-time on CPU
   
   See Also:
   - af_harris: Harris corner detector
   - af_fast: FAST corner detector  
   - af_orb: ORB features with scale/rotation
   - af_sift: SIFT scale-invariant features
   
   References:
   Smith, S.M. and Brady, J.M. (1997)
   'SUSAN - A New Approach to Low Level Image Processing'
   International Journal of Computer Vision, 23(1), 45-78"
  "af_susan" [::mem/pointer ::mem/pointer ::mem/int ::mem/double ::mem/double ::mem/double ::mem/int] ::mem/int)