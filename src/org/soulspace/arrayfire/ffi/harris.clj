(ns org.soulspace.arrayfire.ffi.harris
  "Bindings for ArrayFire Harris corner detector.
   
   The Harris corner detector is a fundamental computer vision algorithm for
   detecting interest points (corners) in images. It identifies regions with
   significant intensity variation in multiple directions, which correspond
   to corners and other distinctive features.
   
   Mathematical Foundation:
   
   The Harris detector analyzes the second-moment matrix (structure tensor):
   
   M = [ Ixx  Ixy ]
       [ Ixy  Iyy ]
   
   Where:
   - Ixx = Σ(Ix²) - sum of squared x-derivatives over a window
   - Iyy = Σ(Iy²) - sum of squared y-derivatives over a window
   - Ixy = Σ(Ix·Iy) - sum of products of x and y derivatives
   
   The Harris response function:
   R = det(M) - k·trace(M)²
   R = (Ixx·Iyy - Ixy²) - k·(Ixx + Iyy)²
   
   Where k is typically 0.04 (empirically determined).
   
   Corner Detection Logic:
   
   - R > 0: Corner detected (large eigenvalues in both directions)
   - R < 0: Edge detected (one large, one small eigenvalue)
   - R ≈ 0: Flat region (both eigenvalues small)
   
   Algorithm Steps:
   
   1. **Compute Gradients**:
      - Calculate Ix and Iy using image gradients
      - Typically using Sobel or similar operators
   
   2. **Compute Second-Order Derivatives**:
      - Ixx = Ix²
      - Iyy = Iy²
      - Ixy = Ix·Iy
   
   3. **Apply Gaussian Window**:
      - Smooth derivatives with Gaussian filter
      - Window size controlled by sigma or block_size
      - Reduces noise sensitivity
   
   4. **Calculate Harris Response**:
      - R = det(M) - k·trace(M)²
      - Compute for each pixel
   
   5. **Non-Maximal Suppression**:
      - Keep only local maxima
      - Suppress weaker responses in neighborhood
   
   6. **Thresholding**:
      - Filter by minimum response
      - Or keep top N corners
   
   Parameters Explained:
   
   **max_corners**:
   - Maximum number of corners to return
   - Algorithm keeps corners with highest responses
   - Set to 0 to use min_response instead
   - Typical values: 100-1000
   
   **min_response**:
   - Minimum Harris response threshold
   - Only used when max_corners = 0
   - Higher values → fewer, stronger corners
   - Typical values: 1e4 to 1e6
   
   **sigma**:
   - Standard deviation of Gaussian window
   - Controls neighborhood size for circular window
   - Only used when block_size = 0
   - Range: 0.5 to 5.0
   - Larger values → more spatial averaging
   
   **block_size**:
   - Square window size for rectangular window
   - Mutually exclusive with sigma
   - Set to 0 to use sigma instead
   - Range: 3 to 31 (must be odd)
   - Larger values → more spatial averaging
   
   **k_thr**:
   - Harris constant (sensitivity parameter)
   - Balances corner vs edge detection
   - Typically 0.04 (empirically determined)
   - Range: 0.01 to 0.10
   - Lower k → more corners detected
   - Higher k → stronger corners required
   
   Output Features:
   
   The function returns a features structure containing:
   - **x**: x-coordinates of detected corners
   - **y**: y-coordinates of detected corners
   - **score**: Harris response values (strength)
   - **orientation**: Always 0 (Harris doesn't compute orientation)
   - **size**: Always 1 (Harris doesn't compute scale)
   
   Use Cases:
   
   1. **Feature Matching**:
      - Detect corners in two images
      - Match corresponding corners
      - Applications: image stitching, panoramas
   
   2. **Object Tracking**:
      - Track corners across video frames
      - Estimate motion and transformation
      - Applications: video stabilization, augmented reality
   
   3. **Structure from Motion**:
      - Reconstruct 3D structure from 2D images
      - Corners provide reliable correspondence points
      - Applications: 3D reconstruction, SLAM
   
   4. **Image Registration**:
      - Align images from different sources
      - Use corners as control points
      - Applications: medical imaging, remote sensing
   
   5. **Calibration**:
      - Camera calibration using checkerboard patterns
      - Corners provide precise geometric features
      - Applications: computer vision system setup
   
   6. **Quality Assessment**:
      - Measure image sharpness
      - More corners → sharper image
      - Applications: autofocus, image quality metrics
   
   Comparison with Other Detectors:
   
   | Feature    | Harris | FAST  | SIFT  | ORB   |
   |------------|--------|-------|-------|-------|
   | Speed      | Medium | Fast  | Slow  | Fast  |
   | Accuracy   | High   | Medium| High  | High  |
   | Scale      | No     | No    | Yes   | Yes   |
   | Rotation   | Partial| No    | Yes   | Yes   |
   | Descriptor | No     | No    | Yes   | Yes   |
   
   Advantages:
   - Rotation invariant (partially)
   - Reliable corner detection
   - Well-understood mathematics
   - Good repeatability
   
   Limitations:
   - Not scale invariant
   - No orientation computation
   - Sensitive to noise (mitigated by Gaussian window)
   - Edge response can interfere (mitigated by k parameter)
   
   Performance Considerations:
   
   **Computational Complexity**:
   - Gradient computation: O(W×H)
   - Convolution: O(W×H×F²) where F is filter size
   - Non-maximal suppression: O(W×H)
   - Overall: ~10-50ms for 640×480 image on GPU
   
   **Memory Usage**:
   - Temporary arrays for derivatives: 3 × image size
   - Response array: 1 × image size
   - Output features: N corners × 5 values
   
   **Optimization Tips**:
   1. Use block_size instead of sigma for speed
   2. Smaller windows → faster computation
   3. GPU acceleration provides 10-100× speedup
   4. Limit max_corners to reduce output size
   
   Example Workflow:
   
   ```clojure
   (require '[org.soulspace.arrayfire.ffi.harris :as harris])
   (require '[coffi.mem :as mem])
   
   ;; Load grayscale image
   (def img (load-image \"image.jpg\" :grayscale true))
   
   ;; Detect corners
   (let [max-corners 500
         min-response 1e5
         sigma 1.0
         block-size 0  ; use sigma instead
         k-thr 0.04
         features-ptr (mem/alloc-instance ::mem/pointer)]
     
     ;; Call Harris detector
     (harris/af-harris features-ptr img max-corners min-response
                      sigma block-size k-thr)
     
     ;; Extract feature data
     (let [features (mem/read-ptr features-ptr)
           n (get-feature-count features)
           x (get-feature-x features)
           y (get-feature-y features)
           scores (get-feature-scores features)]
       
       ;; Use detected corners
       (println \"Detected\" n \"corners\")
       (process-corners x y scores)))
   ```
   
   Common Parameter Combinations:
   
   1. **Fast Detection** (block-based):
      - max_corners: 500
      - block_size: 3
      - k_thr: 0.04
   
   2. **High Quality** (Gaussian-based):
      - max_corners: 1000
      - sigma: 1.0
      - block_size: 0
      - k_thr: 0.04
   
   3. **Sparse Features**:
      - max_corners: 100
      - min_response: 1e6
      - sigma: 2.0
   
   4. **Dense Features**:
      - max_corners: 2000
      - min_response: 1e4
      - sigma: 0.5
   
   Troubleshooting:
   
   **Too Few Corners**:
   - Decrease min_response
   - Increase max_corners
   - Decrease k_thr
   - Decrease sigma/block_size
   
   **Too Many Corners**:
   - Increase min_response
   - Decrease max_corners
   - Increase k_thr
   - Increase sigma/block_size
   
   **False Positives on Edges**:
   - Increase k_thr (more selective)
   - Use larger window (sigma or block_size)
   
   **Missing Corners in Noisy Images**:
   - Increase sigma (more smoothing)
   - Pre-filter image with Gaussian blur
   
   See Also:
   - FAST: Faster corner detection
   - SIFT: Scale and rotation invariant features
   - ORB: Fast alternative to SIFT with binary descriptors
   - Good Features to Track: Similar to Harris but uses min eigenvalue"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; af_err af_harris(af_features *out, const af_array in,
;;                  const unsigned max_corners, const float min_response,
;;                  const float sigma, const unsigned block_size,
;;                  const float k_thr)
(defcfn af-harris
  "Harris corner detector.
   
   Detects corners (interest points) in grayscale images using the Harris
   corner detection algorithm. Returns a features structure containing the
   positions and response strengths of detected corners.
   
   Parameters:
   
   - out: af_features* output features structure containing:
     * n: number of corners detected
     * x: array of x-coordinates (float)
     * y: array of y-coordinates (float)  
     * score: array of Harris response values (float)
     * orientation: array of zeros (Harris doesn't compute orientation)
     * size: array of ones (Harris doesn't compute scale)
   
   - in: af_array input grayscale image
     * Must be 2D (height × width)
     * Color images not supported (convert to grayscale first)
     * Type: typically f32 or f64
   
   - max_corners: unsigned maximum number of corners to detect
     * If > 0: Keep top max_corners with highest responses
     * If = 0: Use min_response threshold instead
     * Typical values: 100-1000
     * Set to 0 to detect all corners above min_response
   
   - min_response: float minimum Harris response threshold
     * Only used when max_corners = 0
     * Corners with response < min_response are discarded
     * Typical values: 1e4 to 1e6
     * Higher values → fewer, stronger corners
   
   - sigma: float standard deviation for Gaussian window
     * Controls circular neighborhood size
     * Only used when block_size = 0
     * Range: 0.5 to 5.0
     * Larger values → more spatial smoothing
     * 0.0 uses rectangular window (block_size must be set)
   
   - block_size: unsigned square window size
     * Controls rectangular neighborhood size
     * Mutually exclusive with sigma (set one to 0)
     * When > 0: Uses rectangular window of size block_size×block_size
     * When = 0: Uses Gaussian window with sigma
     * Range: 3 to 31 (odd numbers preferred)
     * Larger values → more spatial smoothing
   
   - k_thr: float Harris constant (sensitivity parameter)
     * Balances corner vs edge detection
     * Range: 0.01 to 0.10
     * Typical value: 0.04 (empirically determined)
     * Lower k → more corners (less selective)
     * Higher k → fewer corners (more selective)
   
   Returns:
   AF_SUCCESS (0) on success, error code otherwise:
   - AF_ERR_ARG: Invalid arguments (null pointers, invalid dimensions)
   - AF_ERR_SIZE: Invalid window size or sigma
   - AF_ERR_TYPE: Unsupported array type
   - AF_ERR_NO_MEM: Out of memory
   
   Algorithm Overview:
   
   1. Compute image gradients Ix and Iy
   2. Compute second-order derivatives:
      - Ixx = Ix²
      - Iyy = Iy²
      - Ixy = Ix·Iy
   3. Apply Gaussian (sigma) or box (block_size) filter
   4. Calculate Harris response:
      R = det(M) - k·trace(M)²
      Where M = [Ixx Ixy; Ixy Iyy]
   5. Perform non-maximal suppression
   6. Threshold or select top-N responses
   
   Parameter Selection Guide:
   
   **Fast Detection**:
   - max_corners = 500
   - sigma = 0.0
   - block_size = 3
   - k_thr = 0.04
   
   **High Quality**:
   - max_corners = 1000
   - sigma = 1.0
   - block_size = 0
   - k_thr = 0.04
   
   **Few Strong Corners**:
   - max_corners = 0
   - min_response = 1e6
   - sigma = 2.0
   - k_thr = 0.06
   
   **Many Weak Corners**:
   - max_corners = 2000
   - min_response = 1e4
   - sigma = 0.5
   - k_thr = 0.02
   
   Use Cases:
   
   1. **Image Stitching**:
      Detect corners in multiple images for matching and alignment
   
   2. **Object Tracking**:
      Track corner features across video frames for motion estimation
   
   3. **3D Reconstruction**:
      Use corners as correspondence points for structure from motion
   
   4. **Camera Calibration**:
      Detect checkerboard corners for intrinsic parameter estimation
   
   5. **Image Quality**:
      Number of corners correlates with image sharpness
   
   Example:
   ```clojure
   ;; Detect top 500 corners in image
   (let [img (load-grayscale-image \"image.jpg\")
         features-ptr (mem/alloc-instance ::mem/pointer)]
     
     (af-harris features-ptr img
                500    ; max_corners
                0.0    ; min_response (not used)
                1.0    ; sigma (Gaussian window)
                0      ; block_size (use sigma)
                0.04)  ; k_thr
     
     (let [features (mem/read-ptr features-ptr)]
       (println \"Detected\" (get-feature-count features) \"corners\")
       (process-features features)))
   ```
   
   Performance:
   - Typical: 10-50ms for 640×480 on GPU
   - Scales with image size and window size
   - GPU provides 10-100× speedup over CPU
   
   Limitations:
   - Not scale invariant (corners detected at single scale)
   - No orientation computation (set to 0 in output)
   - Sensitive to noise (mitigate with larger sigma/block_size)
   - Grayscale only (convert color images first)
   
   Troubleshooting:
   
   **No corners detected**:
   - Image may be too smooth (low contrast)
   - min_response too high
   - k_thr too high
   - Try decreasing thresholds
   
   **Too many corners**:
   - Increase min_response
   - Decrease max_corners
   - Increase k_thr
   
   **Corners on edges instead of true corners**:
   - Increase k_thr (0.06-0.08)
   - Use larger window (sigma or block_size)
   
   **Missing corners in noisy images**:
   - Increase sigma for more smoothing
   - Pre-filter with Gaussian blur
   
   See Also:
   - af_fast: Faster corner detection algorithm
   - af_sift: Scale-invariant feature detection
   - af_orb: Fast features with binary descriptors
   - af_dog: Difference of Gaussians for scale-space detection"
  "af_harris" [::mem/pointer ::mem/pointer ::mem/int ::mem/double ::mem/double ::mem/int ::mem/double] ::mem/int)
