(ns org.soulspace.arrayfire.ffi.orb
  "Bindings for the ArrayFire ORB (Oriented FAST and Rotated BRIEF) feature detector and descriptor.
   
   ORB (Oriented FAST and Rotated BRIEF) is an efficient alternative to SIFT and SURF
   for feature detection and description in computer vision applications. It combines
   the FAST keypoint detector with the BRIEF descriptor, adding orientation computation
   for rotation invariance.
   
   ## What is ORB?
   
   ORB was introduced by Rublee et al. in 2011 as a fast, robust alternative to
   patented methods like SIFT and SURF. It provides similar performance while being
   significantly faster and completely free to use.
   
   ### Key Components
   
   **1. FAST Keypoint Detection**
   
   ORB uses FAST (Features from Accelerated Segment Test) to detect keypoints:
   - Tests a circle of 16 pixels around a candidate point
   - A point is a corner if N contiguous pixels are all brighter or darker than the center
   - Highly efficient: O(1) per pixel with machine code optimization
   - Non-maximal suppression removes nearby duplicates
   
   Circle pattern (Bresenham circle of radius 3):
   ```
         15  0  1
       14        2
     13            3
     12      p     4
     11            5
       10        6
         9   8  7
   ```
   
   **2. Scale Space Pyramid**
   
   To achieve scale invariance, ORB constructs an image pyramid:
   - Multiple levels with decreasing resolution
   - Each level scaled by scl_fctr (typically 1.2-1.5)
   - Features detected independently at each scale
   - Continues until image too small for descriptor computation
   
   Example pyramid with scl_fctr=1.2:
   ```
   Level 0: 640×480 (original)
   Level 1: 533×400 (640/1.2)
   Level 2: 444×333 (533/1.2)
   Level 3: 370×277 (444/1.2)
   ...
   ```
   
   **3. Harris Corner Measure**
   
   After FAST detection, features are scored using Harris corner response:
   ```
   R = det(M) - k × trace(M)²
   
   where M is the structure tensor:
   M = Σ w(x,y) [Ix²   IxIy]
                [IxIy  Iy² ]
   ```
   
   Properties:
   - det(M) = λ₁λ₂ (product of eigenvalues)
   - trace(M) = λ₁ + λ₂ (sum of eigenvalues)
   - k typically 0.04-0.06
   - High R indicates a corner (large λ₁ and λ₂)
   - Only top max_feat features kept
   
   **4. Orientation Computation**
   
   ORB computes orientation using intensity centroid:
   ```
   m_pq = Σ x^p × y^q × I(x,y)  for (x,y) in patch
   
   Centroid: C = (m_10/m_00, m_01/m_00)
   
   Orientation: θ = atan2(m_01, m_10)
   ```
   
   This provides rotation invariance:
   - Vector from center to centroid gives orientation
   - Range: -π to π
   - Used to rotate descriptor sampling pattern
   
   **5. rBRIEF Descriptor**
   
   Rotated BRIEF (Binary Robust Independent Elementary Features):
   - 256-bit binary string (32 bytes, stored as 8 × 32-bit unsigned ints)
   - Constructed from 256 binary tests
   - Each test compares two pixel intensities
   - Pre-learned pattern rotated by feature orientation
   
   Binary test:
   ```
   τ(p1, p2) = 1 if I(p1) < I(p2)
               0 otherwise
   ```
   
   Full descriptor:
   ```
   d = Σ 2^i × τ(pi1, pi2)  for i = 0 to 255
   ```
   
   Properties:
   - Extremely fast to compute (just pixel comparisons)
   - Efficient to match (Hamming distance via XOR + popcount)
   - Rotation invariant via pattern rotation
   - Not scale invariant within a level (handled by pyramid)
   
   ## ORB Algorithm Overview
   
   For each pyramid level:
   
   1. **Detect FAST corners**
      - Apply FAST threshold test to all pixels
      - Use non-maximal suppression
      - Complexity: O(W×H) where W×H is level size
   
   2. **Compute Harris response**
      - Calculate structure tensor for each corner
      - Compute corner strength R
      - Keep top N features by R value
      - Complexity: O(N × patch_size²)
   
   3. **Compute orientation**
      - Calculate intensity centroid for each feature
      - Compute angle from center to centroid
      - Complexity: O(N × patch_size²)
   
   4. **Optional Gaussian blur**
      - If blur_img=true, apply Gaussian σ=2
      - Increases robustness to noise
      - Slightly slower but more stable
      - Complexity: O(W×H × filter_size)
   
   5. **Extract rBRIEF descriptor**
      - Rotate pre-learned pattern by orientation
      - Sample 512 points (256 pairs)
      - Compare intensities to create 256-bit descriptor
      - Complexity: O(N × 256)
   
   ## Parameter Selection
   
   ### fast_thr (FAST Threshold)
   
   Controls sensitivity of corner detection:
   - **Low (5-10)**: More features, more noise, less repeatable
   - **Medium (15-25)**: Balanced, good for general use
   - **High (30-50)**: Fewer features, higher quality, more stable
   
   Default: 20.0
   
   Selection guide:
   - High-contrast scenes (outdoors): 20-30
   - Low-contrast scenes (indoors): 10-20
   - Textured scenes: 25-35
   - Smooth scenes: 10-20
   
   ### max_feat (Maximum Features)
   
   Upper limit on features to retain:
   - **Low (100-200)**: Fast matching, might miss some regions
   - **Medium (400-800)**: Balanced coverage and speed
   - **High (1000+)**: Dense coverage, slower matching
   
   Default: 400
   
   Considerations:
   - More features → better coverage but slower matching
   - Features distributed across pyramid levels
   - Each level gets roughly proportional share
   - Features sorted by Harris response
   
   ### scl_fctr (Scale Factor)
   
   Downsampling factor between pyramid levels:
   - **Small (1.1-1.2)**: More levels, finer scale coverage, slower
   - **Medium (1.3-1.5)**: Good balance (recommended)
   - **Large (1.8-2.0)**: Fewer levels, faster, less scale coverage
   
   Default: 1.5
   
   Trade-offs:
   - Smaller factor → more overlap between scales → more robust
   - Larger factor → fewer levels → faster computation
   - Too small: redundant features, high computation
   - Too large: scale gaps, missed features
   
   Original ORB paper suggests: 1.2
   
   ### levels (Pyramid Levels)
   
   Number of pyramid levels to compute:
   - **Few (3-4)**: Limited scale range, faster
   - **Medium (6-8)**: Good scale coverage (recommended)
   - **Many (10-12)**: Wide scale range, slower
   
   Default: 4
   
   Actual levels computed: min(levels, max_computable)
   where max_computable depends on image size and patch_size (31)
   
   Scale range covered: scl_fctr^levels
   - levels=4, scl_fctr=1.5: 1× to 5×
   - levels=8, scl_fctr=1.2: 1× to 4.3×
   - levels=8, scl_fctr=1.5: 1× to 25.6×
   
   ### blur_img (Pre-blur Image)
   
   Whether to blur image before descriptor extraction:
   - **false**: Faster, less robust to noise (default)
   - **true**: Slower, more robust, better repeatability
   
   When to use:
   - true: Noisy images, better repeatability needed
   - false: Clean images, speed critical
   
   Gaussian blur applied: σ=2, kernel size typically 7×7
   
   ## Performance Characteristics
   
   ### Time Complexity
   
   For image size W×H, N features, L levels:
   - FAST detection: O(L × W×H)
   - Harris response: O(N × 49) for 7×7 window
   - Orientation: O(N × 961) for 31×31 patch
   - Descriptor: O(N × 256)
   - Total: O(L × W×H + N × 1266)
   
   Typical values:
   - Dominated by FAST detection for large images
   - Descriptor extraction significant for many features
   
   ### GPU Acceleration
   
   ORB benefits greatly from GPU parallelization:
   - FAST: Parallelized per pixel (10-50× speedup)
   - Harris: Parallelized per feature (20-100× speedup)
   - Descriptor: Parallelized per feature (50-200× speedup)
   
   Overall GPU speedup: 15-50× depending on parameters
   
   ### Typical Timings (GPU)
   
   Image 640×480, max_feat=400, levels=4, scl_fctr=1.5:
   - Detection: 2-5 ms
   - Harris scoring: 0.5-1 ms
   - Orientation: 0.2-0.5 ms
   - Descriptors: 0.3-0.8 ms
   - **Total: 3-7 ms**
   
   Image 1920×1080 (Full HD):
   - Total: 8-15 ms
   
   Image 3840×2160 (4K):
   - Total: 25-40 ms
   
   ### Memory Usage
   
   Per feature:
   - Position (x, y): 2 × 4 bytes = 8 bytes
   - Score: 4 bytes
   - Orientation: 4 bytes
   - Size: 4 bytes
   - Descriptor: 8 × 4 bytes = 32 bytes
   - **Total: 52 bytes per feature**
   
   For 400 features: ~21 KB
   For 1000 features: ~51 KB
   
   Pyramid memory (temporary):
   - Level 0: W×H × sizeof(T)
   - Level 1: (W/s)×(H/s) × sizeof(T)
   - ...
   - Total: ≈ (1 + 1/s² + 1/s⁴ + ...) × W×H
   - For s=1.5: ≈ 1.8 × W×H × sizeof(T)
   
   ## Output Format
   
   ### Features (af_features)
   
   Features returned as struct with arrays:
   - **n**: Number of features (scalar)
   - **x**: X coordinates [n] (float)
   - **y**: Y coordinates [n] (float)
   - **score**: Harris response [n] (float)
   - **orientation**: Angle in radians [n] (float, range: -π to π)
   - **size**: Feature size in pixels [n] (float, always patch_size × scale)
   
   Note: Coordinates scaled back to original image space
   
   ### Descriptors (af_array)
   
   Binary descriptors returned as 2D array:
   - Dimensions: [8, n] where n is number of features
   - Type: unsigned 32-bit integers (u32)
   - Each feature: 8 × 32 bits = 256 bits total
   - Layout: Column-major (feature i in column i)
   
   Bit layout per feature:
   ```
   Element 0: bits 0-31
   Element 1: bits 32-63
   Element 2: bits 64-95
   Element 3: bits 96-127
   Element 4: bits 128-159
   Element 5: bits 160-191
   Element 6: bits 192-223
   Element 7: bits 224-255
   ```
   
   ## Matching Descriptors
   
   ORB descriptors are binary, matched using Hamming distance:
   ```
   distance(d1, d2) = popcount(d1 XOR d2)
   ```
   
   Properties:
   - Range: 0 (identical) to 256 (completely different)
   - Fast computation: XOR + population count
   - Hardware-accelerated on modern CPUs
   - GPU can compute thousands of distances in parallel
   
   Matching strategies:
   
   **1. Nearest Neighbor**
   ```
   For each descriptor d1 in image1:
     Find d2 in image2 with minimum Hamming distance
     If distance < threshold, accept match
   ```
   
   **2. Ratio Test (Lowe's criterion)**
   ```
   For each descriptor d1:
     Find two nearest neighbors d2_1, d2_2
     ratio = distance(d1, d2_1) / distance(d1, d2_2)
     If ratio < 0.8, accept match
   ```
   
   **3. Cross-Check**
   ```
   Accept match (d1, d2) only if:
     d2 is nearest neighbor of d1 AND
     d1 is nearest neighbor of d2
   ```
   
   ## Type Support
   
   Input image types:
   - **f32** (float): Single precision (recommended)
   - **f64** (double): Double precision
   
   Note: Integer types not supported, convert to float first
   
   Output types:
   - Features: Always float (f32)
   - Descriptors: Always unsigned 32-bit (u32)
   
   Input constraints:
   - Must be grayscale (single channel)
   - Minimum size: 7×7 pixels
   - 2D only (no batch processing)
   
   ## Applications
   
   ORB is widely used in computer vision:
   
   **1. Image Matching and Alignment**
   - Panorama stitching
   - Image registration
   - Photo organization
   - 3D reconstruction
   
   **2. Object Recognition**
   - Logo detection
   - Product identification
   - Landmark recognition
   - Object categorization
   
   **3. Visual SLAM**
   - Simultaneous Localization and Mapping
   - Robot navigation
   - Augmented reality tracking
   - Autonomous vehicles
   
   **4. Video Tracking**
   - Object tracking across frames
   - Motion estimation
   - Visual odometry
   - Camera pose estimation
   
   **5. Mobile Applications**
   - Real-time on smartphones (very fast)
   - Augmented reality apps
   - Visual search
   - QR code alternatives
   
   **6. 3D Reconstruction**
   - Structure from Motion (SfM)
   - Multi-view stereo
   - Point cloud generation
   - 3D model building
   
   ## Comparison with Other Methods
   
   ### ORB vs SIFT
   - **Speed**: ORB 10-100× faster
   - **Memory**: ORB 4× smaller descriptors (256 bits vs 128 floats)
   - **Matching**: ORB much faster (Hamming vs Euclidean)
   - **Accuracy**: SIFT slightly more robust, ORB sufficient for most uses
   - **Patents**: SIFT patented (expired 2020), ORB always free
   
   ### ORB vs SURF
   - **Speed**: ORB 5-50× faster
   - **Memory**: ORB smaller descriptors
   - **Matching**: ORB faster matching
   - **Accuracy**: Similar robustness
   - **Patents**: SURF patented, ORB free
   
   ### ORB vs AKAZE
   - **Speed**: ORB 2-5× faster
   - **Accuracy**: AKAZE more robust to illumination/blur
   - **Memory**: Similar
   - **Use case**: AKAZE for challenging conditions, ORB for speed
   
   ### ORB vs BRISK
   - **Speed**: Similar (both very fast)
   - **Accuracy**: Similar
   - **Scale**: BRISK more levels by default
   - **Use case**: Both good choices, slight implementation differences
   
   ## Design Patterns
   
   ### Pattern 1: Feature Matching with Ratio Test
   
   ```clojure
   (defn match-orb-features
     \"Match ORB features between two images using ratio test.\"
     [img1 img2]
     (let [;; Extract ORB features
           feat1-ptr (mem/alloc-pointer ::mem/pointer)
           desc1-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-orb feat1-ptr desc1-ptr img1 20.0 400 1.5 4 false)
           feat1 (mem/read-pointer feat1-ptr ::mem/pointer)
           desc1 (mem/read-pointer desc1-ptr ::mem/pointer)
           
           feat2-ptr (mem/alloc-pointer ::mem/pointer)
           desc2-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-orb feat2-ptr desc2-ptr img2 20.0 400 1.5 4 false)
           feat2 (mem/read-pointer feat2-ptr ::mem/pointer)
           desc2 (mem/read-pointer desc2-ptr ::mem/pointer)
           
           ;; Find matches using k-NN (k=2 for ratio test)
           idx-ptr (mem/alloc-pointer ::mem/pointer)
           dist-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-nearest-neighbour idx-ptr dist-ptr 
                                   desc1 desc2 1 2 AF_SHD) ; Hamming distance
           idx (mem/read-pointer idx-ptr ::mem/pointer)
           dist (mem/read-pointer dist-ptr ::mem/pointer)
           
           ;; Apply ratio test (Lowe's criterion)
           matches (filter-matches-ratio-test dist 0.8)]
       {:feat1 feat1 :feat2 feat2
        :matches matches
        :num-matches (count matches)}))
   
   (defn filter-matches-ratio-test
     \"Accept match if distance to nearest < 0.8 × distance to 2nd nearest.\"
     [distances ratio-threshold]
     ;; distances: [2, n_features] array
     ;; Row 0: distance to nearest
     ;; Row 1: distance to 2nd nearest
     (let [dist-data (get-array-data distances)
           n-feat (second (get-dims distances))]
       (for [i (range n-feat)
             :let [d1 (nth dist-data i)
                   d2 (nth dist-data (+ i n-feat))
                   ratio (/ d1 d2)]
             :when (< ratio ratio-threshold)]
         i)))
   ```
   
   ### Pattern 2: Real-Time Video Tracking
   
   ```clojure
   (defn track-object-orb
     \"Track object across video frames using ORB features.\"
     [reference-frame video-frames]
     (let [;; Extract reference features once
           ref-feat-ptr (mem/alloc-pointer ::mem/pointer)
           ref-desc-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-orb ref-feat-ptr ref-desc-ptr reference-frame
                     20.0 400 1.5 4 false)
           ref-feat (mem/read-pointer ref-feat-ptr ::mem/pointer)
           ref-desc (mem/read-pointer ref-desc-ptr ::mem/pointer)]
       
       ;; Process each frame
       (map (fn [frame]
              (let [;; Extract frame features
                    feat-ptr (mem/alloc-pointer ::mem/pointer)
                    desc-ptr (mem/alloc-pointer ::mem/pointer)
                    _ (af-orb feat-ptr desc-ptr frame 20.0 400 1.5 4 false)
                    feat (mem/read-pointer feat-ptr ::mem/pointer)
                    desc (mem/read-pointer desc-ptr ::mem/pointer)
                    
                    ;; Match features
                    matches (match-features ref-desc desc)
                    
                    ;; Estimate transformation (homography)
                    transform (estimate-homography 
                               ref-feat feat matches)]
                {:frame frame
                 :matches (count matches)
                 :transform transform}))
            video-frames)))
   ```
   
   ### Pattern 3: Panorama Stitching
   
   ```clojure
   (defn stitch-panorama
     \"Create panorama from multiple images using ORB.\"
     [images]
     (reduce (fn [panorama img]
               (let [;; Extract features from both
                     pan-feat-ptr (mem/alloc-pointer ::mem/pointer)
                     pan-desc-ptr (mem/alloc-pointer ::mem/pointer)
                     _ (af-orb pan-feat-ptr pan-desc-ptr panorama
                              20.0 800 1.5 6 true) ; More features for alignment
                     pan-feat (mem/read-pointer pan-feat-ptr ::mem/pointer)
                     pan-desc (mem/read-pointer pan-desc-ptr ::mem/pointer)
                     
                     img-feat-ptr (mem/alloc-pointer ::mem/pointer)
                     img-desc-ptr (mem/alloc-pointer ::mem/pointer)
                     _ (af-orb img-feat-ptr img-desc-ptr img
                              20.0 800 1.5 6 true)
                     img-feat (mem/read-pointer img-feat-ptr ::mem/pointer)
                     img-desc (mem/read-pointer img-desc-ptr ::mem/pointer)
                     
                     ;; Find overlap and align
                     matches (match-features pan-desc img-desc)
                     homography (ransac-homography pan-feat img-feat matches)
                     
                     ;; Warp and blend
                     warped (warp-perspective img homography)
                     blended (blend-images panorama warped)]
                 blended))
             (first images)
             (rest images)))
   ```
   
   ### Pattern 4: Adaptive Parameter Selection
   
   ```clojure
   (defn adaptive-orb
     \"Automatically adjust ORB parameters based on image characteristics.\"
     [img]
     (let [;; Analyze image characteristics
           stats (compute-image-stats img)
           contrast (compute-contrast img)
           texture (compute-texture-score img)
           
           ;; Adjust parameters
           fast-thr (cond
                      (< contrast 0.2) 10.0  ; Low contrast
                      (< contrast 0.5) 20.0  ; Medium contrast
                      :else 30.0)             ; High contrast
           
           max-feat (cond
                      (< texture 0.3) 200    ; Smooth image
                      (< texture 0.7) 400    ; Normal texture
                      :else 800)             ; Highly textured
           
           blur-img (< contrast 0.3) ; Blur for low contrast
           
           ;; Extract features with adapted parameters
           feat-ptr (mem/alloc-pointer ::mem/pointer)
           desc-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-orb feat-ptr desc-ptr img fast-thr max-feat 1.5 6 blur-img)
           feat (mem/read-pointer feat-ptr ::mem/pointer)
           desc (mem/read-pointer desc-ptr ::mem/pointer)]
       {:features feat
        :descriptors desc
        :parameters {:fast-thr fast-thr
                    :max-feat max-feat
                    :blur-img blur-img}}))
   ```
   
   ## When to Use ORB
   
   **Good fit:**
   - Real-time applications (very fast)
   - Mobile/embedded systems (efficient)
   - Large-scale matching (fast Hamming distance)
   - Rotation invariance needed
   - Free software requirement (no patents)
   - Textured scenes
   
   **Not ideal:**
   - Extreme scale changes (use SIFT/SURF)
   - Severe illumination changes (use AKAZE)
   - Very smooth/low-texture objects (consider learned features)
   - High precision required (SIFT more accurate)
   - Blurry images (AKAZE more robust)
   
   ## Common Issues and Solutions
   
   ### Issue 1: Too Few Features Detected
   
   **Symptoms:**
   - Very few or no features returned
   - Poor matching performance
   
   **Causes:**
   - fast_thr too high for image contrast
   - Image too smooth/uniform
   - Image too small for pyramid levels
   
   **Solutions:**
   - Reduce fast_thr (try 10-15)
   - Increase max_feat
   - Reduce number of levels
   - Pre-process: enhance contrast, sharpen
   
   ### Issue 2: Too Many Features (Slow Matching)
   
   **Symptoms:**
   - Matching takes too long
   - Memory usage high
   
   **Causes:**
   - fast_thr too low
   - max_feat too high
   - Highly textured image
   
   **Solutions:**
   - Increase fast_thr (try 25-30)
   - Reduce max_feat (try 200-400)
   - Use spatial binning to distribute features
   - Consider approximate matching (LSH)
   
   ### Issue 3: Poor Matching Across Scales
   
   **Symptoms:**
   - Matches fail when object size changes
   - Features detected but not matched
   
   **Causes:**
   - Not enough pyramid levels
   - scl_fctr too large (gaps in scale coverage)
   
   **Solutions:**
   - Increase levels (try 6-8)
   - Reduce scl_fctr (try 1.2-1.3)
   - Ensure image large enough for desired levels
   
   ### Issue 4: Noisy/Unstable Features
   
   **Symptoms:**
   - Features jump around in similar images
   - Poor repeatability
   
   **Causes:**
   - Image noise
   - blur_img=false on noisy images
   - fast_thr too low
   
   **Solutions:**
   - Set blur_img=true
   - Increase fast_thr
   - Pre-blur input image (Gaussian σ=1)
   - Use AKAZE for noisy conditions
   
   ### Issue 5: Features Only at Certain Scales
   
   **Symptoms:**
   - Features concentrated in few pyramid levels
   - Uneven feature distribution
   
   **Causes:**
   - Image characteristics vary by scale
   - Pyramid computation stops early
   
   **Solutions:**
   - Check actual levels computed (may be less than requested)
   - Adjust scl_fctr
   - Ensure image large enough (min_side > 31)
   
   ## Best Practices
   
   1. **Start with defaults**: fast_thr=20, max_feat=400, scl_fctr=1.5, levels=4
   
   2. **Grayscale conversion**: Convert RGB to grayscale before ORB
      - Use af-rgb2gray with standard weights (0.299, 0.587, 0.114)
   
   3. **Feature distribution**: For panoramas/large scenes, increase max_feat
      - 800-1000 features for better coverage
   
   4. **Matching strategy**: Always use ratio test or cross-check
      - Ratio threshold 0.7-0.8 works well
      - Cross-check eliminates ambiguous matches
   
   5. **Robustness vs speed**: Use blur_img=true for noisy/low-quality images
      - Cost: ~20-30% slower
      - Benefit: More repeatable features
   
   6. **Scale coverage**: For large scale changes, increase levels
      - levels=8 with scl_fctr=1.2 covers wide range
      - Check image size allows desired levels
   
   7. **Parameter tuning**: Adjust based on image characteristics
      - Low contrast → lower fast_thr
      - High texture → more max_feat
      - Clean images → blur_img=false
   
   8. **Memory management**: Release features and descriptors when done
      - Use af-release-features for features
      - Use af-release-array for descriptors
   
   9. **Batch processing**: For video, consider tracking instead of re-detection
      - Detect in keyframes only
      - Track features between keyframes
   
   10. **Performance profiling**: Measure actual computation time
       - Detection, orientation, and descriptor separate steps
       - Identify bottleneck for optimization
   
   ## Mathematical Properties
   
   ### Rotation Invariance
   
   ORB achieves rotation invariance through:
   - Orientation computation (intensity centroid)
   - Pattern rotation during descriptor extraction
   
   Limitation: Orientation estimation less stable than gradient-based (SIFT)
   
   ### Scale Invariance (Pyramid-based)
   
   Each pyramid level handles specific scale range:
   - Level i detects features at scale s^i
   - Feature matching works across levels
   - Coarse scale quantization (unlike SIFT continuous scale)
   
   ### Descriptor Properties
   
   **Discriminative power:**
   - 256 bits provide 2^256 possible descriptors
   - Practical discrimination: ~10^6-10^7 unique objects
   - Collision probability very low
   
   **Hamming distance distribution:**
   - Random descriptors: mean distance ≈ 128
   - Similar patches: distance < 50 typical
   - Good separation for matching
   
   ### Repeatability
   
   Measured as percentage of features detected in both images:
   - Viewpoint change: 70-85%
   - Scale change: 65-80%
   - Rotation: 75-90%
   - Illumination: 60-75%
   - Blur: 50-70%
   
   SIFT slightly better but ORB sufficient for most uses
   
   ## References
   
   **Original Paper:**
   - Rublee, Ethan, et al. \"ORB: An efficient alternative to SIFT or SURF.\"
     ICCV 2011.
   
   **FAST Corner Detection:**
   - Rosten, Edward, and Tom Drummond. \"Machine learning for high-speed corner detection.\"
     ECCV 2006.
   
   **BRIEF Descriptor:**
   - Calonder, Michael, et al. \"BRIEF: Binary robust independent elementary features.\"
     ECCV 2010.
   
   **Harris Corner Detector:**
   - Harris, Chris, and Mike Stephens. \"A combined corner and edge detector.\"
     Alvey vision conference 1988.
   
   See also:
   - af-fast: Just the FAST corner detector
   - af-harris: Harris corner detector
   - af-sift: SIFT features (more robust, slower)
   - af-nearest-neighbour: Feature matching with Hamming distance
   - af-hamming-matcher: Specialized ORB descriptor matcher"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; af_err af_orb(af_features *feat, af_array *desc, const af_array in, const float fast_thr, const unsigned max_feat, const float scl_fctr, const unsigned levels, const bool blur_img)
(defcfn af-orb
  "Extract ORB (Oriented FAST and Rotated BRIEF) features and descriptors.
   
   ORB is a fast, robust feature detector and descriptor for computer vision
   applications. It combines FAST keypoint detection with the BRIEF descriptor,
   adding orientation for rotation invariance and using a pyramid for scale
   invariance.
   
   Parameters:
   - feat: out pointer for af_features struct
     * Contains arrays for x, y, score, orientation, size
     * n field has number of features detected
   - desc: out pointer for descriptor array
     * Array dimensions: [8, n] where n is number of features
     * Type: unsigned 32-bit integers (u32)
     * Each feature: 256-bit binary descriptor (8 × 32 bits)
   - in: input grayscale image array
     * Must be 2D grayscale (single channel)
     * Type: f32 or f64
     * Minimum size: 7×7 pixels
   - fast-thr: FAST threshold for corner detection
     * Range: typically 5-50
     * Lower: more features, more noise
     * Higher: fewer features, higher quality
     * Default: 20.0
   - max-feat: maximum number of features to retain
     * Range: 100-1000+ typical
     * Features sorted by Harris corner response
     * Distributed across pyramid levels
     * Default: 400
   - scl-fctr: scale factor between pyramid levels
     * Range: typically 1.1-2.0
     * Lower: more levels, finer scale coverage
     * Higher: fewer levels, faster computation
     * Original paper suggests: 1.2
     * Default: 1.5
   - levels: number of pyramid levels
     * Range: 1-12 typical
     * Actual levels: min(levels, max_computable)
     * More levels: wider scale coverage
     * Default: 4
   - blur-img: whether to blur before descriptor extraction
     * false: faster (default)
     * true: more robust to noise, better repeatability
     * Uses Gaussian blur σ=2
   
   Algorithm:
   For each pyramid level:
   1. Downsample image by scl_fctr^level
   2. Detect FAST corners (threshold fast_thr)
   3. Score corners using Harris corner measure
   4. Keep top features by Harris score
   5. Compute orientation (intensity centroid)
   6. Optionally blur image (if blur_img=true)
   7. Extract rotated BRIEF descriptor (256 bits)
   8. Scale coordinates back to original image space
   
   Output Features (feat):
   - n: Number of features detected
   - x: X coordinates [n] (scaled to original image)
   - y: Y coordinates [n] (scaled to original image)
   - score: Harris corner response [n]
   - orientation: Angle in radians [n] (range: -π to π)
   - size: Feature size in pixels [n] (patch_size × scale_factor)
   
   Output Descriptors (desc):
   - Dimensions: [8, n]
   - Type: u32 (unsigned 32-bit)
   - Storage: Column-major, feature i in column i
   - Bit layout: 256 bits per feature
     * Element 0: bits 0-31
     * Element 1: bits 32-63
     * ...
     * Element 7: bits 224-255
   - Matching: Use Hamming distance (XOR + popcount)
   
   Performance:
   - Time: O(L × W×H + N × 1266)
     where L=levels, W×H=image size, N=features
   - GPU speedup: 15-50× typical
   - Typical: 3-7 ms for 640×480, 400 features, 4 levels
   
   Example 1: Basic ORB feature extraction
   ```clojure
   ;; Load grayscale image
   (let [img-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-load-image img-ptr \"scene.jpg\" false) ; false = grayscale
         img (mem/read-pointer img-ptr ::mem/pointer)
         
         ;; Extract ORB features with defaults
         feat-ptr (mem/alloc-pointer ::mem/pointer)
         desc-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-orb feat-ptr desc-ptr img
                   20.0    ; fast_thr - medium sensitivity
                   400     ; max_feat - reasonable number
                   1.5     ; scl_fctr - good balance
                   4       ; levels - limited scale range
                   false)  ; blur_img - speed priority
         feat (mem/read-pointer feat-ptr ::mem/pointer)
         desc (mem/read-pointer desc-ptr ::mem/pointer)
         
         ;; Get number of features
         n-ptr (mem/alloc-pointer ::mem/long)
         _ (af-get-features-num n-ptr feat)
         n (mem/read-pointer n-ptr ::mem/long)]
     
     (println \"Detected\" n \"ORB features\")
     {:features feat :descriptors desc :count n}))
   ```
   
   Example 2: High-quality feature extraction for matching
   ```clojure
   ;; Extract features with quality priority
   (let [img1 (load-grayscale-image \"image1.jpg\")
         img2 (load-grayscale-image \"image2.jpg\")
         
         ;; Extract from first image
         feat1-ptr (mem/alloc-pointer ::mem/pointer)
         desc1-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-orb feat1-ptr desc1-ptr img1
                   25.0    ; Higher threshold for quality
                   800     ; More features for better coverage
                   1.2     ; Finer scale steps
                   8       ; More levels for scale invariance
                   true)   ; Blur for repeatability
         feat1 (mem/read-pointer feat1-ptr ::mem/pointer)
         desc1 (mem/read-pointer desc1-ptr ::mem/pointer)
         
         ;; Extract from second image (same parameters)
         feat2-ptr (mem/alloc-pointer ::mem/pointer)
         desc2-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-orb feat2-ptr desc2-ptr img2 25.0 800 1.2 8 true)
         feat2 (mem/read-pointer feat2-ptr ::mem/pointer)
         desc2 (mem/read-pointer desc2-ptr ::mem/pointer)
         
         ;; Match descriptors using Hamming distance
         idx-ptr (mem/alloc-pointer ::mem/pointer)
         dist-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-nearest-neighbour idx-ptr dist-ptr
                                desc1 desc2
                                1      ; dist_dim - features along dim1
                                1      ; n_dist - nearest neighbor
                                2)     ; dist_type - AF_SHD (Hamming)
         matches (mem/read-pointer idx-ptr ::mem/pointer)
         distances (mem/read-pointer dist-ptr ::mem/pointer)]
     
     {:feat1 feat1 :desc1 desc1
      :feat2 feat2 :desc2 desc2
      :matches matches :distances distances}))
   ```
   
   Example 3: Real-time video feature tracking
   ```clojure
   ;; Fast extraction for real-time application
   (defn track-features-video
     [video-frames]
     (map (fn [frame]
            (let [;; Convert to grayscale if needed
                  gray (if (is-color? frame)
                        (rgb-to-gray frame)
                        frame)
                  
                  ;; Fast extraction
                  feat-ptr (mem/alloc-pointer ::mem/pointer)
                  desc-ptr (mem/alloc-pointer ::mem/pointer)
                  _ (af-orb feat-ptr desc-ptr gray
                           15.0    ; Lower threshold for more features
                           300     ; Moderate number (speed)
                           1.5     ; Standard scale factor
                           3       ; Fewer levels (speed)
                           false)  ; No blur (speed)
                  feat (mem/read-pointer feat-ptr ::mem/pointer)
                  desc (mem/read-pointer desc-ptr ::mem/pointer)
                  
                  ;; Get feature count
                  n-ptr (mem/alloc-pointer ::mem/long)
                  _ (af-get-features-num n-ptr feat)
                  n (mem/read-pointer n-ptr ::mem/long)]
              
              {:frame-features feat
               :frame-descriptors desc
               :num-features n}))
          video-frames))
   ```
   
   Example 4: Panorama stitching
   ```clojure
   ;; Extract features for image stitching
   (let [img-left (load-grayscale-image \"left.jpg\")
         img-right (load-grayscale-image \"right.jpg\")
         
         ;; Left image features
         feat-l-ptr (mem/alloc-pointer ::mem/pointer)
         desc-l-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-orb feat-l-ptr desc-l-ptr img-left
                   20.0    ; Standard threshold
                   1000    ; Many features for overlap detection
                   1.5     ; Standard scale
                   6       ; More levels for robustness
                   true)   ; Blur for stability
         feat-left (mem/read-pointer feat-l-ptr ::mem/pointer)
         desc-left (mem/read-pointer desc-l-ptr ::mem/pointer)
         
         ;; Right image features (same parameters)
         feat-r-ptr (mem/alloc-pointer ::mem/pointer)
         desc-r-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-orb feat-r-ptr desc-r-ptr img-right 20.0 1000 1.5 6 true)
         feat-right (mem/read-pointer feat-r-ptr ::mem/pointer)
         desc-right (mem/read-pointer desc-r-ptr ::mem/pointer)
         
         ;; Find matches in overlap region
         ;; (Use spatial constraints to filter)
         matches (find-overlap-matches feat-left feat-right
                                       desc-left desc-right)
         
         ;; Estimate homography from matches
         homography (estimate-homography-ransac
                     feat-left feat-right matches)]
     
     {:features-left feat-left
      :features-right feat-right
      :matches matches
      :homography homography}))
   ```
   
   Example 5: Adaptive parameter selection
   ```clojure
   ;; Adapt parameters based on image characteristics
   (let [img (load-grayscale-image \"adaptive.jpg\")
         
         ;; Analyze image
         contrast (compute-image-contrast img)
         texture (compute-texture-score img)
         noise-level (estimate-noise img)
         
         ;; Select parameters
         fast-thr (cond
                    (< contrast 0.2) 10.0   ; Low contrast
                    (< contrast 0.5) 20.0   ; Medium
                    :else 30.0)              ; High contrast
         
         max-feat (if (> texture 0.5) 600 400)
         
         blur-img (or (> noise-level 0.1)
                      (< contrast 0.3))
         
         ;; Extract with adapted parameters
         feat-ptr (mem/alloc-pointer ::mem/pointer)
         desc-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-orb feat-ptr desc-ptr img
                   fast-thr max-feat 1.5 6 blur-img)
         feat (mem/read-pointer feat-ptr ::mem/pointer)
         desc (mem/read-pointer desc-ptr ::mem/pointer)]
     
     (println \"Adapted: threshold=\" fast-thr
              \"max_feat=\" max-feat
              \"blur=\" blur-img)
     {:features feat :descriptors desc}))
   ```
   
   Example 6: Mobile AR application
   ```clojure
   ;; Lightweight extraction for mobile/AR
   (defn extract-ar-markers
     [camera-frame]
     (let [;; Very fast parameters for real-time
           feat-ptr (mem/alloc-pointer ::mem/pointer)
           desc-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-orb feat-ptr desc-ptr camera-frame
                    20.0    ; Standard threshold
                    200     ; Fewer features (speed)
                    1.8     ; Larger steps (speed)
                    3       ; Fewer levels (speed)
                    false)  ; No blur (speed)
           feat (mem/read-pointer feat-ptr ::mem/pointer)
           desc (mem/read-pointer desc-ptr ::mem/pointer)
           
           ;; Match against known markers
           matches (match-with-database desc marker-database)]
       
       ;; Process matches for AR overlay
       (when (> (count matches) 4)
         (let [pose (estimate-camera-pose feat matches)]
           {:detected true
            :pose pose
            :num-matches (count matches)}))))
   ```
   
   Example 7: 3D reconstruction
   ```clojure
   ;; Extract features for Structure from Motion
   (let [images (load-image-sequence \"sequence/*.jpg\")
         
         ;; Extract features from all images
         all-features
         (mapv (fn [img]
                 (let [gray (rgb-to-gray img)
                       feat-ptr (mem/alloc-pointer ::mem/pointer)
                       desc-ptr (mem/alloc-pointer ::mem/pointer)
                       _ (af-orb feat-ptr desc-ptr gray
                                25.0    ; High quality
                                1000    ; Many features
                                1.2     ; Fine scale
                                8       ; Many levels
                                true)   ; Stable features
                       feat (mem/read-pointer feat-ptr ::mem/pointer)
                       desc (mem/read-pointer desc-ptr ::mem/pointer)]
                   {:features feat :descriptors desc}))
               images)
         
         ;; Match features across image pairs
         pair-matches (for [i (range (dec (count images)))]
                        (match-image-pair
                         (nth all-features i)
                         (nth all-features (inc i))))
         
         ;; Build feature tracks across multiple views
         tracks (build-feature-tracks pair-matches)
         
         ;; Triangulate 3D points
         points-3d (triangulate-tracks tracks all-features)]
     
     {:num-images (count images)
      :num-features (map #(get-feature-count (:features %))
                         all-features)
      :num-tracks (count tracks)
      :points-3d points-3d}))
   ```
   
   Common Patterns:
   
   1. **Match quality filtering:**
      ```clojure
      ;; Ratio test (Lowe's criterion)
      (defn ratio-test-filter [distances ratio-threshold]
        ;; Keep match if d1/d2 < threshold
        ;; distances: [2, n] array with nearest and 2nd-nearest
        (filter #(< (/ (first %) (second %)) ratio-threshold)
                (partition 2 distances)))
      ```
   
   2. **Cross-check matching:**
      ```clojure
      ;; Only accept mutual nearest neighbors
      (defn cross-check-matches [matches-1-to-2 matches-2-to-1]
        (intersection
         (set (map vector (range) matches-1-to-2))
         (set (map (fn [[j i]] [i j])
                   (map vector (range) matches-2-to-1)))))
      ```
   
   3. **Spatial verification (homography):**
      ```clojure
      ;; Use RANSAC to filter outliers
      (defn verify-matches-ransac
        [feat1 feat2 matches]
        (let [homography (ransac-fit-homography
                          feat1 feat2 matches
                          {:threshold 3.0 :iterations 1000})
              inliers (filter #(< (reprojection-error % homography) 3.0)
                             matches)]
          {:homography homography
           :inliers inliers
           :inlier-ratio (/ (count inliers) (count matches))}))
      ```
   
   When to use different parameter settings:
   
   - **High speed (video, mobile):**
     fast_thr=15-20, max_feat=200-300, scl_fctr=1.5-2.0, levels=3-4, blur=false
   
   - **High quality (matching, stitching):**
     fast_thr=25-30, max_feat=800-1000, scl_fctr=1.2-1.3, levels=6-8, blur=true
   
   - **Balanced (general use):**
     fast_thr=20, max_feat=400, scl_fctr=1.5, levels=4, blur=false (defaults)
   
   - **Low-texture scenes:**
     fast_thr=10-15, max_feat=600-800, increase levels
   
   - **Noisy images:**
     Enable blur=true, increase fast_thr slightly
   
   When NOT to use ORB:
   - Need scale invariance within levels (use SIFT)
   - Extreme illumination changes (use AKAZE)
   - Very smooth objects (use learned features)
   - High-precision required (SIFT more accurate)
   
   Type Support:
   - Input: f32, f64 (grayscale only)
   - Features: Always f32
   - Descriptors: Always u32
   
   Gotchas:
   - Image must be grayscale (convert RGB first)
   - Minimum image size 7×7 (for FAST)
   - Actual levels may be less than requested (image size limit)
   - Descriptors are binary (use Hamming distance, not Euclidean)
   - Feature coordinates scaled to original image
   - Orientation in radians, not degrees
   - Descriptor matching requires af-nearest-neighbour with AF_SHD
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-fast: FAST corner detector only
   - af-harris: Harris corner detector
   - af-sift: SIFT features (more robust, slower)
   - af-nearest-neighbour: Descriptor matching
   - af-hamming-matcher: Optimized ORB matching"
  "af_orb" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/float ::mem/int ::mem/float ::mem/int ::mem/int] ::mem/int)
