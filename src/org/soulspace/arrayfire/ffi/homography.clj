(ns org.soulspace.arrayfire.ffi.homography
  "Bindings for the ArrayFire homography estimation functions.
   
   ## Overview
   
   This namespace provides FFI bindings for ArrayFire's homography estimation
   functions, which compute the perspective transformation (homography matrix)
   between two sets of corresponding 2D points. This is a fundamental operation
   in computer vision for tasks like image alignment, panorama stitching,
   augmented reality, and camera calibration.
   
   ## Homography
   
   A homography is a 3x3 transformation matrix that maps points from one plane
   to another. It represents a perspective transformation and is defined by:
   
   ```
   [x']   [h11 h12 h13] [x]
   [y'] = [h21 h22 h23] [y]
   [w']   [h31 h32 h33] [w]
   ```
   
   where (x, y, w) are homogeneous coordinates in the source plane and
   (x', y', w') are homogeneous coordinates in the destination plane.
   The Euclidean coordinates are obtained by dividing: x_euclid = x'/w'.
   
   ### Mathematical Formulation
   
   Given n pairs of corresponding points (xᵢ, yᵢ) → (x'ᵢ, y'ᵢ), the homography
   H satisfies:
   
   ```
   x'ᵢ = (h₁₁xᵢ + h₁₂yᵢ + h₁₃) / (h₃₁xᵢ + h₃₂yᵢ + h₃₃)
   y'ᵢ = (h₂₁xᵢ + h₂₂yᵢ + h₂₃) / (h₃₁xᵢ + h₃₂yᵢ + h₃₃)
   ```
   
   This can be rewritten as a linear system Ah = 0, where h is the vector
   of homography coefficients and A is constructed from the point correspondences.
   The solution is typically found using Singular Value Decomposition (SVD).
   
   ### Degrees of Freedom
   
   A homography has 8 degrees of freedom (9 coefficients, but scale invariant),
   so at minimum 4 point correspondences are required to compute it. More points
   can be used with robust estimation methods.
   
   ## Robust Estimation Methods
   
   ### RANSAC (RANdom SAmple Consensus)
   
   RANSAC is an iterative method to estimate parameters of a mathematical model
   from a dataset containing outliers. For homography estimation:
   
   **Algorithm:**
   1. Randomly select 4 point pairs (minimum required for homography)
   2. Compute homography H from these 4 pairs using DLT (Direct Linear Transform)
   3. Count inliers: points where the reprojection error is below threshold
   4. Repeat for N iterations, keeping the H with most inliers
   5. Optionally refine H using all inliers
   
   **Characteristics:**
   - Fast convergence with low outlier ratio
   - Number of iterations depends on desired confidence and outlier ratio
   - Provides number of inliers as quality metric
   - Good for real-time applications
   
   **Iteration Formula:**
   ```
   N = log(1 - confidence) / log(1 - (1 - outlier_ratio)⁴)
   ```
   
   For 99% confidence and 40% outliers: N ≈ 69 iterations
   
   **Advantages:**
   - Simple and efficient
   - Works well with up to 50% outliers
   - Provides inlier count
   
   **Disadvantages:**
   - Non-deterministic (uses random sampling)
   - May need many iterations with high outlier ratios
   - Quality depends on inlier threshold choice
   
   ### LMedS (Least Median of Squares)
   
   LMedS minimizes the median of squared residuals rather than the mean,
   making it more robust to outliers than least squares.
   
   **Algorithm:**
   1. For each iteration:
      a. Randomly select 4 point pairs
      b. Compute homography H
      c. Compute reprojection errors for all points
      d. Calculate median of squared errors
   2. Select H with smallest median error
   3. Compute threshold from median: σ = 1.4826 × (1 + 5/(n-4)) × √median
   4. Classify inliers using threshold
   
   **Characteristics:**
   - More robust than RANSAC for high outlier ratios (up to ~50%)
   - Deterministic given same random seed
   - No explicit inlier threshold needed
   - Automatically estimates threshold from data
   
   **Advantages:**
   - No threshold parameter required
   - Robust to ~50% outliers
   - Automatic inlier/outlier classification
   
   **Disadvantages:**
   - Computationally more expensive than RANSAC
   - Requires more iterations for same confidence
   - May be slower for low outlier ratios
   
   ## Implementation Details
   
   ### Point Normalization
   
   ArrayFire implements the normalized DLT algorithm, which improves numerical
   stability by normalizing point coordinates before computing the homography:
   
   1. **Translate**: Center points at origin
      - mean_x = mean(x), mean_y = mean(y)
      - x' = x - mean_x, y' = y - mean_y
   
   2. **Scale**: Normalize so average distance from origin is √2
      - variance = mean(x'² + y'²)
      - scale = √2 / √variance
      - x'' = x' × scale, y'' = y' × scale
   
   3. **Compute H'**: Solve for homography using normalized points
   
   4. **Denormalize**: Transform back to original coordinate system
      - H = T_dst⁻¹ × H' × T_src
   
   This normalization prevents numerical issues when point coordinates have
   large magnitudes or widely different scales.
   
   ### GPU Acceleration
   
   The ArrayFire implementation leverages GPU parallelization:
   
   - **Parallel SVD**: Each iteration's homography computation uses parallel SVD
   - **Batch evaluation**: All iterations evaluated in parallel
   - **Parallel reduction**: Finding best homography uses parallel reduction
   
   **Performance (typical):**
   - 1000 iterations: 1-10ms on GPU vs 50-200ms on CPU
   - 10,000 iterations: 10-50ms on GPU vs 500-2000ms on CPU
   - Speedup: 10-50× depending on problem size
   
   ## Function: af-homography
   
   Estimate homography between two sets of 2D point correspondences.
   
   ### Parameters
   
   - **H** (out): `::mem/pointer`
     Pointer to output homography matrix (af_array handle).
     The returned array has dimensions 3×3 and contains the homography
     transformation matrix in row-major order:
     ```
     [h11 h12 h13]
     [h21 h22 h23]
     [h31 h32 h33]
     ```
     The homography maps source points to destination points via
     perspective transformation. Type matches `type` parameter (f32 or f64).
   
   - **inliers** (out): `::mem/pointer`
     Pointer to integer receiving the number of inliers.
     For RANSAC: number of points within `inlier_thr` distance for best H.
     For LMedS: number of points classified as inliers after threshold estimation.
     Typical values: 50-100% of input points for good matches, <30% indicates
     poor correspondence or excessive outliers.
   
   - **x-src** (in): `::mem/pointer`
     X coordinates of source points (af_array handle).
     Must be 1D array of type f32, length N (number of point pairs).
     Points should be in pixel coordinates, typically [0, width).
   
   - **y-src** (in): `::mem/pointer`
     Y coordinates of source points (af_array handle).
     Must be 1D array of type f32, length N (same as x-src).
     Points should be in pixel coordinates, typically [0, height).
   
   - **x-dst** (in): `::mem/pointer`
     X coordinates of destination points (af_array handle).
     Must be 1D array of type f32, length N (same as x-src).
     These are the target positions corresponding to source points.
   
   - **y-dst** (in): `::mem/pointer`
     Y coordinates of destination points (af_array handle).
     Must be 1D array of type f32, length N (same as x-src).
     These are the target positions corresponding to source points.
   
   - **htype** (in): `::mem/int`
     Homography estimation method:
     - `0` = AF_HOMOGRAPHY_RANSAC: RANSAC method (default, faster)
     - `1` = AF_HOMOGRAPHY_LMEDS: LMedS method (more robust, slower)
   
   - **inlier-thr** (in): `::mem/float`
     Inlier threshold in pixels (RANSAC only).
     Maximum reprojection error for a point to be considered an inlier.
     Typical values:
     - 1.0: Very strict, for high-precision applications
     - 3.0: Standard choice (recommended default)
     - 5.0: Lenient, for noisy or imprecise correspondences
     - 10.0: Very lenient, for extremely noisy data
     For LMedS, this parameter is ignored as threshold is estimated automatically.
   
   - **iterations** (in): `::mem/int`
     Number of iterations to perform.
     **CPU backend**: Maximum iterations, stops early if good H found.
     **GPU backend (CUDA/OpenCL)**: Exact number of iterations performed.
     Typical values:
     - 100: Quick but may miss best solution
     - 1000: Standard choice (recommended default)
     - 10000: High quality, thorough search
     More iterations increase robustness but also computation time.
   
   - **type** (in): `::mem/int`
     Output homography data type:
     - `0` = f32 (32-bit float): faster, sufficient for most applications
     - `1` = f64 (64-bit double): higher precision for demanding applications
   
   ### Returns
   
   `::mem/int` - ArrayFire error code:
   - `0` (AF_SUCCESS): Homography computed successfully
   - `201` (AF_ERR_SIZE): Input arrays have mismatched sizes
   - `202` (AF_ERR_TYPE): Input arrays not f32 type
   - `203` (AF_ERR_ARG): Invalid htype or fewer than 4 point pairs
   - `205` (AF_ERR_NOT_SUPPORTED): Backend doesn't support homography (e.g., oneAPI)
   
   ### Algorithm Details
   
   #### Direct Linear Transform (DLT)
   
   For each iteration, ArrayFire computes homography using normalized DLT:
   
   1. **Select 4 random point pairs** (minimum required for homography)
   2. **Normalize coordinates** (mean centering and scaling)
   3. **Build linear system**:
      For each point pair (xᵢ, yᵢ) → (x'ᵢ, y'ᵢ), add two equations to A:
      ```
      [-xᵢ  -yᵢ  -1   0    0    0   x'ᵢxᵢ  x'ᵢyᵢ  x'ᵢ]     [0]
      [ 0    0    0  -xᵢ  -yᵢ  -1   y'ᵢxᵢ  y'ᵢyᵢ  y'ᵢ] h = [0]
      ```
   4. **Solve using SVD**: h is the right singular vector corresponding to smallest singular value
   5. **Denormalize**: Transform H back to original coordinate system
   6. **Evaluate quality**:
      - RANSAC: Count points where distance(H(src), dst) < threshold
      - LMedS: Compute median of squared distances
   
   #### Reprojection Error
   
   For a point (x, y) and homography H, the reprojection error is:
   ```
   x' = (h₁₁x + h₁₂y + h₁₃) / (h₃₁x + h₃₂y + h₃₃)
   y' = (h₂₁x + h₂₂y + h₂₃) / (h₃₁x + h₃₂y + h₃₃)
   error = √((x' - x_dst)² + (y' - y_dst)²)
   ```
   
   Points with error < inlier_thr are classified as inliers.
   
   ## Use Cases
   
   ### 1. Image Stitching / Panorama Creation
   
   Align multiple overlapping images into a seamless panorama.
   
   **Workflow:**
   1. Detect features in each image pair (SIFT, SURF, ORB)
   2. Match features between images
   3. Estimate homography from feature correspondences
   4. Warp images using homography
   5. Blend images in overlap regions
   
   **Example Scenario:**
   - Input: 3 photos of a landscape, each 3000×2000 pixels
   - Feature detection: 500-2000 keypoints per image
   - Feature matching: 200-800 correspondences per pair
   - Homography estimation: RANSAC with inlier_thr=3.0
   - Result: 8000×2000 panorama
   
   **Parameters:**
   - htype: AF_HOMOGRAPHY_RANSAC (faster for clean matches)
   - inlier_thr: 3.0-5.0 (depends on image resolution)
   - iterations: 1000-5000 (higher for better quality)
   
   **Quality Metrics:**
   - Inlier ratio: >60% indicates good alignment
   - Reprojection error: <2-3 pixels for clean stitching
   - Visual assessment: check for ghosting or misalignment
   
   ### 2. Planar Object Tracking
   
   Track a planar object (e.g., book cover, poster) in video frames.
   
   **Workflow:**
   1. Extract and store reference features from object in first frame
   2. For each subsequent frame:
      a. Detect features
      b. Match with reference features
      c. Estimate homography
      d. Determine object pose and position
   3. Draw bounding box or augment object
   
   **Example Scenario:**
   - Reference object: 20cm × 30cm book cover
   - Camera: 1920×1080 at 30fps
   - Features: 100-300 on object
   - Matches: 20-100 per frame
   - Real-time requirement: <33ms per frame
   
   **Parameters:**
   - htype: AF_HOMOGRAPHY_RANSAC (for real-time performance)
   - inlier_thr: 5.0 (lenient for fast-moving objects)
   - iterations: 100-500 (balance speed vs robustness)
   
   **Challenges:**
   - Occlusion: track may be lost if object partially hidden
   - Motion blur: increase inlier_thr or reduce camera speed
   - Lighting changes: use illumination-invariant features
   
   ### 3. Document Rectification
   
   Transform a perspective view of a document into a frontal, rectangular view.
   
   **Workflow:**
   1. Detect document corners (Harris, contour detection, or manual selection)
   2. Define destination corners (rectangular layout)
   3. Estimate homography from corner correspondences
   4. Warp document image using homography
   5. Apply sharpening or adaptive thresholding for OCR
   
   **Example Scenario:**
   - Input: Smartphone photo of A4 document, tilted at 30° angle
   - Document size: ~800×600 pixels in image
   - Corner detection: 4 corners (exact requirement for homography)
   - Output: 2480×3508 pixels (300 DPI A4)
   
   **Parameters:**
   - htype: AF_HOMOGRAPHY_RANSAC (though 4 points = no outliers)
   - iterations: Can be 1 if using exact 4 corner points
   - With automatic corner detection, use iterations=100, inlier_thr=10.0
   
   **Quality Considerations:**
   - Corner accuracy: 1-2 pixel error acceptable for OCR
   - Aspect ratio: maintain correct proportions (e.g., √2:1 for A4)
   - Resolution: target 200-300 DPI for reliable OCR
   
   ### 4. Camera Calibration
   
   Estimate camera intrinsic and extrinsic parameters using planar calibration pattern.
   
   **Workflow:**
   1. Capture images of checkerboard from multiple viewpoints
   2. Detect checkerboard corners in each image
   3. For each image:
      a. Estimate homography from world plane to image plane
      b. Extract camera parameters from homography
   4. Refine parameters using bundle adjustment
   
   **Example Scenario:**
   - Calibration target: 8×6 checkerboard, 25mm squares
   - Images: 10-20 from different angles
   - Corners per image: 48 (8×6 interior corners)
   - Camera: 1920×1080, focal length ~1000 pixels
   
   **Parameters:**
   - htype: AF_HOMOGRAPHY_LMEDS (robust to detection errors)
   - inlier_thr: Not used (LMedS)
   - iterations: 1000-5000 (high quality required)
   
   **Calibration Quality:**
   - Reprojection error: <0.5 pixels for good calibration
   - Multiple viewpoints: ensures parameter observability
   - Coverage: target should appear at different depths and angles
   
   ### 5. Augmented Reality
   
   Overlay virtual content onto planar surfaces in real-world scenes.
   
   **Workflow:**
   1. Detect and track planar marker or surface
   2. Extract feature correspondences
   3. Estimate homography continuously (per frame)
   4. Decompose homography into camera pose
   5. Render 3D content using estimated pose
   
   **Example Scenario:**
   - Marker: 10cm × 10cm printed pattern
   - Camera: 1280×720 at 30fps
   - Features on marker: 50-200
   - Virtual content: 3D model, text, or video
   - Latency requirement: <50ms end-to-end
   
   **Parameters:**
   - htype: AF_HOMOGRAPHY_RANSAC (real-time performance)
   - inlier_thr: 3.0-5.0 (balance precision and robustness)
   - iterations: 100-1000 (depends on frame rate requirement)
   
   **Performance Optimization:**
   - Temporal coherence: use previous H as prior
   - Adaptive iterations: reduce when tracking is stable
   - GPU processing: essential for real-time performance
   
   **Challenges:**
   - Jitter: smooth pose estimates using Kalman filtering
   - Occlusion: robust feature tracking required
   - Lighting: affects feature detection quality
   
   ## Advanced Techniques
   
   ### Pose Decomposition
   
   Given homography H and camera intrinsics K, decompose into rotation R and
   translation t:
   
   ```
   H = K [r₁ r₂ t]
   ```
   
   where r₁ and r₂ are first two columns of rotation matrix.
   
   **Process:**
   1. Compute H_normalized = K⁻¹ × H
   2. Extract r₁ = H_normalized[:, 0]
   3. Extract r₂ = H_normalized[:, 1]
   4. Compute r₃ = r₁ × r₂ (cross product)
   5. Extract t = H_normalized[:, 2]
   6. Orthogonalize R = [r₁ r₂ r₃] using SVD
   
   This provides camera pose relative to the planar surface.
   
   ### Homography Refinement
   
   After initial RANSAC/LMedS estimation, refine using all inliers:
   
   1. Classify inliers using initial H
   2. Re-estimate H using only inliers with weighted least squares
   3. Optionally iterate: classify → re-estimate
   
   **Benefits:**
   - Improved accuracy (uses all inlier information)
   - Lower reprojection error
   - Better for applications requiring high precision
   
   **Implementation:**
   ```clojure
   (let [initial-H (estimate-homography-ransac ...)
         inlier-mask (classify-inliers initial-H src dst threshold)
         refined-H (estimate-homography-lsq (filter-inliers src inlier-mask)
                                            (filter-inliers dst inlier-mask))]
     refined-H)
   ```
   
   ### Multi-Plane Homography
   
   For scenes with multiple planes, estimate separate homography for each:
   
   1. Segment points by plane (clustering or prior knowledge)
   2. Estimate homography for each plane
   3. Blend or select appropriate H based on spatial location
   
   **Use cases:**
   - Building facades with multiple walls
   - Documents with folds or multiple pages
   - Mixed indoor/outdoor scenes
   
   ### Homography Chaining
   
   Combine multiple homographies for indirect transformations:
   ```
   H_AC = H_AB × H_BC
   ```
   
   **Applications:**
   - Multi-image alignment (transitivity)
   - Coordinate system transformations
   - Error propagation analysis
   
   ## Performance Characteristics
   
   ### Computational Complexity
   
   **Per iteration:**
   - Point selection: O(1)
   - SVD for 9×9 matrix: O(1) (constant size)
   - Inlier counting: O(N) where N = number of points
   
   **Total complexity:**
   - RANSAC: O(iterations × N)
   - LMedS: O(iterations × N log N) (due to median computation)
   
   ### Timing Benchmarks (typical)
   
   **GPU (CUDA/OpenCL):**
   - 100 points, 1000 iterations: 2-5ms
   - 500 points, 1000 iterations: 5-10ms
   - 1000 points, 10000 iterations: 20-50ms
   
   **CPU:**
   - 100 points, 1000 iterations: 20-100ms
   - 500 points, 1000 iterations: 50-200ms
   - 1000 points, 10000 iterations: 500-2000ms
   
   **Speedup:** GPU is typically 10-50× faster than CPU
   
   ### Memory Usage
   
   - Input points: 4N floats (N point pairs)
   - Temporary homographies: 9 × iterations floats
   - Error arrays: iterations floats (LMedS) or iterations integers (RANSAC)
   - Output: 9 floats (3×3 homography)
   
   **Example:** 1000 points, 10000 iterations
   - Input: ~16KB
   - Temporary: ~360KB
   - Total: ~500KB (fits easily in GPU memory)
   
   ### Optimization Tips
   
   1. **Reduce iterations** for real-time applications:
      - Use adaptive iteration count based on inlier ratio
      - Stop early when good solution found (CPU backend)
   
   2. **Downsample points** if too many correspondences:
      - Use spatial binning to select representative points
      - Reduces N, proportionally speeds up computation
   
   3. **Adjust threshold** appropriately:
      - Too strict: many outliers, poor H estimate
      - Too lenient: includes outliers, degrades accuracy
   
   4. **Use RANSAC** when:
      - Outlier ratio <40%
      - Real-time performance required
      - Inlier count is meaningful metric
   
   5. **Use LMedS** when:
      - Outlier ratio 40-50%
      - No good threshold estimate available
      - Deterministic behavior preferred
   
   ## Error Handling
   
   ### Common Errors
   
   1. **AF_ERR_SIZE (201)**: Input array dimension mismatch
      - Ensure x_src, y_src, x_dst, y_dst all have same length
      - Check that arrays are 1D (not 2D or higher)
   
   2. **AF_ERR_TYPE (202)**: Input arrays not f32 type
      - All coordinate arrays must be f32 (32-bit float)
      - Cast arrays if necessary before calling
   
   3. **AF_ERR_ARG (203)**: Invalid arguments
      - Fewer than 4 point pairs provided
      - Invalid htype value (not 0 or 1)
      - Invalid iterations count (must be >0)
   
   4. **Poor homography** (returned but incorrect):
      - Too few inliers: <30% suggests poor correspondences
      - Try increasing iterations or adjusting threshold
      - Check input point quality (feature detection/matching)
   
   ### Debugging Strategies
   
   1. **Visualize correspondences**:
      - Draw lines from src to transformed src using H
      - Inliers should have short lines, outliers long lines
   
   2. **Check inlier ratio**:
      - <30%: Poor matches, try different features or threshold
      - 30-60%: Acceptable, may benefit from more iterations
      - >60%: Good matches
   
   3. **Evaluate reprojection error**:
      - Compute error for all inliers
      - Mean/median should be <2-3 pixels for good H
   
   4. **Test with synthetic data**:
      - Generate known H, transform points, add noise
      - Should recover H within numerical precision
   
   ## Integration Patterns
   
   ### Feature Detection and Matching Pipeline
   
   ```clojure
   (defn estimate-homography-from-images
     [img1 img2]
     (let [;; Feature detection
           features1 (af-orb img1 ...)
           features2 (af-orb img2 ...)
           
           ;; Feature matching
           [idx nn-dist] (af-nearest-neighbor features1 features2 ...)
           
           ;; Filter matches by distance ratio
           good-matches (filter-matches idx nn-dist 0.8)
           
           ;; Extract point coordinates
           pts1 (extract-keypoint-locations features1 good-matches)
           pts2 (extract-keypoint-locations features2 good-matches)
           
           ;; Estimate homography
           [x1 y1] (split-points pts1)
           [x2 y2] (split-points pts2)
           H (af-homography x1 y1 x2 y2
                           AF_HOMOGRAPHY_RANSAC
                           3.0 1000 f32)]
       H))
   ```
   
   ### Real-time Tracking Loop
   
   ```clojure
   (defn tracking-loop
     [reference-features video-stream]
     (loop [frame (get-frame video-stream)]
       (when frame
         (let [current-features (af-orb frame ...)
               matches (af-nearest-neighbor reference-features current-features ...)
               [x-ref y-ref x-cur y-cur] (extract-match-coordinates matches)
               
               ;; Fast homography for real-time
               H (af-homography x-ref y-ref x-cur y-cur
                               AF_HOMOGRAPHY_RANSAC
                               5.0 100 f32)
               
               ;; Render overlay
               _ (render-overlay frame H)]
           (recur (get-frame video-stream))))))
   ```
   
   ### Batch Processing Pipeline
   
   ```clojure
   (defn stitch-panorama
     [images]
     (reduce
       (fn [panorama next-img]
         (let [[pts-pano pts-next] (find-correspondences panorama next-img)
               [xp yp xn yn] (split-point-arrays pts-pano pts-next)
               
               ;; High-quality homography
               H (af-homography xp yp xn yn
                               AF_HOMOGRAPHY_LMEDS
                               0.0 5000 f64)
               
               warped (af-transform next-img H ...)
               blended (blend-images panorama warped ...)]
           blended))
       (first images)
       (rest images)))
   ```
   
   ## Comparison with Alternatives
   
   ### vs. Affine Transform
   
   **Affine:**
   - 6 DOF (rotation, translation, scale, shear)
   - Preserves parallel lines
   - Requires 3 point pairs
   - Faster computation
   - Use when: object not rotated in depth
   
   **Homography:**
   - 8 DOF (adds perspective)
   - Preserves straight lines (but not parallelism)
   - Requires 4 point pairs
   - Handles perspective distortion
   - Use when: viewing angle matters
   
   ### vs. Essential Matrix (Epipolar Geometry)
   
   **Essential Matrix:**
   - For 3D scenes, multiple planes
   - Encodes camera rotation and translation
   - Requires calibrated cameras
   - Use for: Structure from Motion, 3D reconstruction
   
   **Homography:**
   - For planar scenes or dominant plane
   - Direct pixel-to-pixel mapping
   - Works with uncalibrated cameras
   - Use for: planar tracking, image alignment
   
   ### vs. Optical Flow
   
   **Optical Flow:**
   - Dense correspondence (all pixels)
   - Assumes small motion between frames
   - Faster for video tracking
   - Use for: dense motion fields, small displacements
   
   **Homography:**
   - Sparse correspondence (keypoints)
   - Handles large motions and scale changes
   - Global transformation model
   - Use for: large motions, wide baselines
   
   ## Related ArrayFire Functions
   
   - `af_orb`: Feature detection for finding keypoints
   - `af_hamming_matcher`: Match binary features (ORB descriptors)
   - `af_nearest_neighbor`: Match SIFT/SURF features
   - `af_transform`: Apply homography to warp images
   - `af_translate`, `af_scale`, `af_rotate`: Simpler transformations
   - `af_approx1`, `af_approx2`: Interpolation for warping
   
   ## References
   
   - **Original Paper**: Hartley & Zisserman, \"Multiple View Geometry in Computer Vision\", 2003
   - **RANSAC**: Fischler & Bolles, \"Random Sample Consensus\", 1981
   - **LMedS**: Rousseeuw & Leroy, \"Robust Regression and Outlier Detection\", 1987
   - **Normalized DLT**: Hartley, \"In Defense of the Eight-Point Algorithm\", 1997
   - **ArrayFire Documentation**: https://arrayfire.org/docs/group__cv__func__homography.htm
   - **OpenCV Tutorial**: https://docs.opencv.org/master/d9/dab/tutorial_homography.html
   
   ## Implementation Notes
   
   ### Backend Differences
   
   **CPU:**
   - Sequential iteration evaluation
   - Early termination possible based on iteration quality
   - Uses Eigen for linear algebra
   
   **CUDA/OpenCL:**
   - Parallel evaluation of all iterations
   - No early termination (all iterations always run)
   - Uses custom CUDA/OpenCL kernels for SVD
   
   **oneAPI:**
   - Currently not supported
   - Will return AF_ERR_NOT_SUPPORTED
   
   ### Numerical Considerations
   
   - **Conditioning**: Point normalization improves condition number by ~3-4 orders of magnitude
   - **Precision**: f32 sufficient for most applications (error ~1e-6)
   - **Degenerate cases**: Collinear points or duplicate points will fail
   - **Scale**: Works best when source and destination are similar scale (within 10×)
   
   ### Thread Safety
   
   - Function is thread-safe when operating on different ArrayFire arrays
   - Uses ThreadLocal random number generator state
   - Multiple simultaneous calls OK with different data"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; af_err af_homography(af_array *H, int *inliers, const af_array x_src,
;;                      const af_array y_src, const af_array x_dst,
;;                      const af_array y_dst, const af_homography_type htype,
;;                      const float inlier_thr, const unsigned iterations,
;;                      const af_dtype type)
(defcfn af-homography
  "Estimate homography between two sets of 2D point correspondences.
   
   Parameters:
   - H: out pointer for 3×3 homography matrix (af_array)
   - inliers: out pointer for number of inliers (int)
   - x-src: source x coordinates array (f32, 1D, length N)
   - y-src: source y coordinates array (f32, 1D, length N)
   - x-dst: destination x coordinates array (f32, 1D, length N)
   - y-dst: destination y coordinates array (f32, 1D, length N)
   - htype: homography type (0=RANSAC, 1=LMedS)
   - inlier-thr: inlier threshold in pixels (RANSAC only, typically 3.0)
   - iterations: number of iterations (typically 1000)
   - type: output array type (0=f32, 1=f64)
   
   Returns:
   ArrayFire error code (0=success)"
  "af_homography" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer
                   ::mem/pointer ::mem/pointer ::mem/int ::mem/float
                   ::mem/int ::mem/int] ::mem/int)
