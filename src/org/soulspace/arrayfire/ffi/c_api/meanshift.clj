(ns org.soulspace.arrayfire.ffi.c-api.meanshift
  "Bindings for the ArrayFire mean shift image filter function.
   
   Mean shift is an iterative mode-seeking algorithm used in image processing
   for segmentation, denoising, and feature space analysis. It moves each pixel
   toward the mode (peak) of the local probability density in feature space,
   resulting in piecewise-constant regions.
   
   Mathematical Foundation:
   
   Mean shift is a non-parametric feature-space analysis technique that
   finds modes (local maxima) of a density function. For each point, it
   iteratively moves toward higher density regions until convergence.
   
   **Algorithm Overview**:
   
   For each pixel p with color c and position (x, y):
   1. Define spatial neighborhood using spatial_sigma
   2. Define chromatic (color) neighborhood using chromatic_sigma
   3. Compute weighted mean of neighboring pixels
   4. Move pixel value toward the weighted mean
   5. Repeat for num_iterations or until convergence
   
   **Kernel Function**:
   
   The algorithm uses a kernel to weight nearby pixels:
   
   K(x) = exp(-||x||² / (2σ²))
   
   Two independent kernels:
   - Spatial kernel: K_s based on Euclidean distance
   - Chromatic kernel: K_r based on color/intensity difference
   
   **Weighted Mean Update**:
   
   For pixel i at position p_i with color c_i:
   
   p_new = (Σ_j w_j · p_j) / (Σ_j w_j)
   c_new = (Σ_j w_j · c_j) / (Σ_j w_j)
   
   where:
   - w_j = K_s(||p_i - p_j||) · K_r(||c_i - c_j||)
   - Sum over all pixels j in spatial neighborhood
   - Weights combine spatial proximity and color similarity
   
   **Spatial Kernel**:
   
   w_spatial(p, q) = exp(-||p - q||² / (2 * σ_s²))
   
   where:
   - p, q are pixel positions (x, y coordinates)
   - σ_s = spatial_sigma (controls spatial extent)
   - Larger σ_s → larger spatial neighborhood
   
   **Chromatic Kernel**:
   
   w_chromatic(c_p, c_q) = exp(-||c_p - c_q||² / (2 * σ_c²))
   
   where:
   - c_p, c_q are pixel colors/intensities
   - σ_c = chromatic_sigma (controls color tolerance)
   - Larger σ_c → more permissive to color differences
   
   **Combined Weight**:
   
   w(p, q) = w_spatial(p, q) · w_chromatic(c_p, c_q)
   
   Only pixels similar in both space AND color receive high weights.
   
   **Convergence**:
   
   The algorithm converges when:
   1. Pixel position stops changing (spatial convergence)
   2. Pixel color stops changing (chromatic convergence)
   3. Maximum iterations reached
   
   Typical convergence criterion:
   - Position shift < 1 pixel
   - Color shift < 1 intensity level
   
   **Mode Seeking Property**:
   
   Mean shift has the property that it always moves toward regions of
   higher density in the feature space. This makes it effective for:
   - Finding dominant colors (modes in color space)
   - Identifying homogeneous regions (spatial modes)
   - Segmenting images into distinct regions
   
   Properties of Mean Shift:
   
   1. **Non-parametric**: No assumptions about number or shape of clusters
   2. **Mode-seeking**: Automatically finds local maxima (modes)
   3. **Deterministic**: Same input always gives same output
   4. **Segmentation**: Creates piecewise-constant regions
   5. **Edge-preserving**: Respects color/intensity boundaries
   
   Parameter Selection Guide:
   
   **spatial_sigma** (σ_s):
   - **Small (3-7)**: Fine segmentation, preserves small details
     * Many small regions
     * Good for high-resolution images
     * Faster computation
   - **Medium (10-20)**: Moderate region size
     * Balanced detail/smoothness
     * Most common setting
     * Good general-purpose choice
   - **Large (20-40)**: Coarse segmentation, large uniform regions
     * Few large regions
     * Over-smooths details
     * Good for low-resolution or noisy images
   
   **chromatic_sigma** (σ_c):
   - **Small (5-20)**: Color-sensitive, more segments
     * Preserves color variations
     * Many distinct regions
     * Good for images with subtle color changes
   - **Medium (20-50)**: Moderate color tolerance
     * Merges similar colors
     * Balanced segmentation
     * Most common setting
   - **Large (50-100)**: Color-insensitive, fewer segments
     * Ignores minor color differences
     * Large uniform regions
     * Good for noisy color data
   
   **num_iterations**:
   - **Few (1-3)**: Partial convergence, faster
     * Smoothing effect
     * May not fully segment
     * Good for denoising
   - **Moderate (5-10)**: Good convergence, typical
     * Most pixels converge
     * Balanced speed/quality
     * Recommended for most uses
   - **Many (15+)**: Full convergence, slower
     * Complete segmentation
     * Diminishing returns
     * May over-segment
   
   **is_color**:
   - **false (0)**: Grayscale mode
     * Single intensity channel
     * Faster processing
     * For grayscale images
   - **true (1)**: Color mode (RGB)
     * Three color channels (R, G, B)
     * Color-aware segmentation
     * For RGB images (dims[2] must be 3)
   
   Typical Parameter Combinations:
   
   **General segmentation**:
   - spatial_sigma: 10-15
   - chromatic_sigma: 30-40
   - iterations: 5-7
   - Use case: Natural images, general-purpose
   
   **Fine detail preservation**:
   - spatial_sigma: 5-7
   - chromatic_sigma: 15-25
   - iterations: 3-5
   - Use case: High-resolution images, detailed textures
   
   **Coarse segmentation**:
   - spatial_sigma: 20-30
   - chromatic_sigma: 50-70
   - iterations: 8-10
   - Use case: Finding major regions, simplifying scenes
   
   **Denoising only**:
   - spatial_sigma: 5-10
   - chromatic_sigma: 20-30
   - iterations: 1-3
   - Use case: Noise reduction without full segmentation
   
   **Medical imaging**:
   - spatial_sigma: 7-12
   - chromatic_sigma: 15-30
   - iterations: 5-8
   - Use case: Anatomy segmentation, tissue classification
   
   Performance Characteristics:
   
   **Algorithmic Complexity**:
   - Time: O(N × k × s² × i) where:
     * N = number of pixels (width × height)
     * k = number of channels (1 for grayscale, 3 for color)
     * s = spatial neighborhood size (~3 × spatial_sigma)
     * i = number of iterations
   - Space: O(N) for input and output arrays
   
   **GPU Acceleration**:
   - 10-100× speedup over CPU
   - Parallel processing of all pixels simultaneously
   - Each pixel's convergence computed independently
   - Optimized memory access patterns
   
   **Iteration Impact**:
   - First iteration: Largest effect (most movement)
   - Later iterations: Refinement (small adjustments)
   - Convergence often reached before max iterations
   - Adaptive stopping can reduce computation
   
   **Image Size Impact**:
   - Linear scaling with pixel count
   - Larger images benefit more from GPU
   - Memory requirements moderate (2× input size)
   
   Type Support:
   
   Input Types:
   - f32 (float): Single-precision floating point
   - f64 (double): Double-precision floating point
   - s32 (int): Signed 32-bit integer
   - u32 (uint): Unsigned 32-bit integer
   - s16 (short): Signed 16-bit integer
   - u16 (ushort): Unsigned 16-bit integer
   - s64 (intl): Signed 64-bit integer
   - u64 (uintl): Unsigned 64-bit integer
   - s8 (schar): Signed 8-bit integer
   - u8 (uchar): Unsigned 8-bit integer
   - b8 (char): Boolean/byte
   
   Output type matches input type.
   
   Applications:
   
   1. **Image Segmentation**:
      - Partition image into homogeneous regions
      - Object detection and isolation
      - Scene understanding
      - Foreground/background separation
   
   2. **Denoising**:
      - Remove noise while preserving edges
      - Smooth homogeneous regions
      - Medical image enhancement
      - Photograph cleanup
   
   3. **Color Quantization**:
      - Reduce number of colors
      - Posterization effects
      - Compression preprocessing
      - Artistic effects
   
   4. **Tracking**:
      - Video object tracking (CAMShift)
      - Motion analysis
      - Surveillance applications
      - Gesture recognition
   
   5. **Medical Imaging**:
      - Organ segmentation (CT, MRI)
      - Tumor detection
      - Tissue classification
      - Cell counting in microscopy
   
   6. **Remote Sensing**:
      - Land cover classification
      - Urban area detection
      - Vegetation analysis
      - Change detection
   
   7. **Feature Extraction**:
      - Region-based features
      - Shape analysis
      - Texture characterization
      - Statistical properties
   
   Common Workflows:
   
   **Basic Segmentation**:
   1. Load image
   2. Apply mean shift with appropriate parameters
   3. Extract connected components
   4. Analyze/classify regions
   
   **Hierarchical Segmentation**:
   1. Coarse segmentation (large σ_s, σ_c)
   2. Fine segmentation within regions (small σ_s, σ_c)
   3. Build region hierarchy
   4. Select appropriate granularity level
   
   **Video Tracking (CAMShift)**:
   1. Select region of interest in first frame
   2. Compute color histogram
   3. Apply mean shift in subsequent frames
   4. Update region location and size
   5. Track object through video
   
   **Color Simplification**:
   1. Apply mean shift with moderate spatial, small chromatic sigma
   2. Result has simplified color palette
   3. Useful for compression, artistic effects
   4. Maintains spatial structure
   
   Comparison with Other Methods:
   
   **Mean Shift vs K-Means Clustering**:
   - Mean Shift: Automatically finds number of clusters, modes
   - K-Means: Requires pre-specifying K clusters
   - Mean Shift: Non-parametric, data-driven
   - K-Means: Faster but less flexible
   
   **Mean Shift vs Bilateral Filter**:
   - Mean Shift: Iterative, creates uniform regions (segmentation)
   - Bilateral: Single-pass, smooths while preserving edges
   - Mean Shift: Better for segmentation tasks
   - Bilateral: Better for pure denoising
   
   **Mean Shift vs Graph Cuts**:
   - Mean Shift: Local, independent pixel processing
   - Graph Cuts: Global optimization, energy minimization
   - Mean Shift: Faster, more scalable
   - Graph Cuts: More accurate boundaries, needs user input
   
   **Mean Shift vs Watershed**:
   - Mean Shift: Feature-space analysis, mode-seeking
   - Watershed: Gradient-based, treats image as topography
   - Mean Shift: Better for color/texture segmentation
   - Watershed: Better for well-defined boundaries
   
   **Mean Shift vs Deep Learning**:
   - Mean Shift: No training required, interpretable parameters
   - Deep Learning: Requires training data, black-box
   - Mean Shift: Fast inference, no model loading
   - Deep Learning: More semantic understanding
   
   Advantages:
   
   - **No prior knowledge**: Automatically discovers segments
   - **Non-parametric**: No assumptions about data distribution
   - **Edge-preserving**: Respects color/intensity boundaries
   - **Deterministic**: Reproducible results
   - **Interpretable**: Parameters have clear physical meaning
   - **Versatile**: Works for segmentation, denoising, tracking
   
   Limitations:
   
   - **Computationally intensive**: Iterative, many operations per pixel
   - **Parameter sensitivity**: Results depend on σ_s, σ_c choices
   - **Fixed window**: Doesn't adapt to local image structure
   - **Over-segmentation**: May create too many small regions
   - **No semantic understanding**: Purely low-level, pixel-based
   - **Texture handling**: May break up textured regions
   
   Best Practices:
   
   1. **Start conservative**: Begin with moderate parameters
   2. **Iterate experimentally**: Adjust based on visual results
   3. **Consider image scale**: Parameters depend on resolution
   4. **Limit iterations**: 5-10 usually sufficient
   5. **Preprocess if needed**: Resize, denoise as preprocessing
   6. **Post-process**: Connect components, merge small regions
   7. **Validate scientifically**: Use ground truth for quantitative evaluation
   
   Error Conditions:
   
   - spatial_sigma < 0: Invalid (must be non-negative)
   - chromatic_sigma < 0: Invalid (must be non-negative)
   - num_iterations = 0: Invalid (must be positive)
   - dims.ndims() < 2: Invalid (must be at least 2D)
   - is_color = true AND dims[2] ≠ 3: Invalid (color mode requires 3 channels)
   
   See also:
   - af-bilateral: Single-pass edge-preserving filter
   - af-medfilt: Median filter for salt-and-pepper noise
   - Segmentation functions for post-processing
   - Connected components for region analysis"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.loader]))

;; Mean shift filter

;; af_err af_mean_shift(af_array *out, const af_array in, const float spatial_sigma, const float chromatic_sigma, const unsigned iter, const bool is_color)
(defcfn af-mean-shift
  "Apply mean shift filter for image segmentation and denoising.
   
   Mean shift is an iterative mode-seeking algorithm that moves each pixel
   toward the mode (peak) of the local density in feature space. This results
   in piecewise-constant regions useful for segmentation, denoising, and
   tracking applications.
   
   Parameters:
   - out: out pointer for filtered/segmented image
   - in: input image array (2D grayscale or 3D color)
   - spatial-sigma: spatial standard deviation (controls window size)
   - chromatic-sigma: chromatic variance (controls color/intensity tolerance)
   - iter: number of iterations to perform (convergence control)
   - is-color: boolean (int) indicating color (1) vs grayscale (0)
     * 0 (false): Grayscale mode (single channel)
     * 1 (true): Color mode (RGB, requires dims[2] = 3)
   
   Algorithm:
   For each pixel, iteratively:
   1. Define spatial neighborhood (radius ≈ 1.5 × spatial_sigma)
   2. Compute weights based on spatial distance and color similarity
   3. Calculate weighted mean of neighboring pixel positions and colors
   4. Move pixel toward weighted mean
   5. Repeat until convergence or iter iterations completed
   
   Mathematical Operation:
   
   New position: p' = (Σ w_i · p_i) / (Σ w_i)
   New color: c' = (Σ w_i · c_i) / (Σ w_i)
   
   where:
   - w_i = exp(-d_spatial²/(2σ_s²)) · exp(-d_color²/(2σ_c²))
   - d_spatial = Euclidean distance between positions
   - d_color = Euclidean distance between colors
   - Weights combine spatial proximity AND color similarity
   
   Convergence:
   - Converges when position and color changes < threshold
   - Typically 5-10 iterations sufficient
   - Some pixels may converge earlier
   - Creates piecewise-constant regions (segmentation)
   
   Parameter Selection:
   
   **spatial_sigma** (controls region size):
   - Small (3-7): Fine segmentation, preserves details
   - Medium (10-20): Balanced, most common
   - Large (20-40): Coarse segmentation, large regions
   
   **chromatic_sigma** (controls color tolerance):
   - Small (5-20): Color-sensitive, more segments
   - Medium (20-50): Moderate tolerance, typical
   - Large (50-100): Color-insensitive, fewer segments
   
   **iterations** (controls convergence):
   - Few (1-3): Partial convergence, denoising effect
   - Moderate (5-10): Good convergence, recommended
   - Many (15+): Full convergence, diminishing returns
   
   Typical Combinations:
   - General segmentation: spatial=10, chromatic=30, iter=5
   - Fine detail: spatial=5, chromatic=15, iter=3
   - Coarse regions: spatial=20, chromatic=50, iter=8
   
   Input Constraints:
   - dims.ndims() >= 2 (must be at least 2D image)
   - If is_color = true, dims[2] must equal 3 (RGB channels)
   - spatial_sigma >= 0
   - chromatic_sigma >= 0
   - iter > 0
   
   Performance:
   - Complexity: O(N × iter × window_size²) per pixel
   - window_size ≈ 3 × spatial_sigma
   - GPU provides 10-100× speedup
   - Iterations are the main computational cost
   - Parallel across all pixels
   
   Type Support:
   - All numeric types: f32, f64, s32, u32, s16, u16, s8, u8, s64, u64, b8
   - Output type matches input type
   - Internal computation uses float or double precision
   
   Example (Grayscale Segmentation):
   ```clojure
   ;; Segment grayscale image into homogeneous regions
   (let [gray-img (create-array img-data [512 512])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         status (af-mean-shift out-ptr gray-img
                               10.0   ;; spatial_sigma
                               30.0   ;; chromatic_sigma
                               5      ;; iterations
                               0)]    ;; grayscale mode
     ;; Result: segmented image with piecewise-constant regions
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (Color Segmentation):
   ```clojure
   ;; Segment color image by color and position
   (let [rgb-img (create-array img-data [512 512 3])  ;; RGB image
         out-ptr (mem/alloc-pointer ::mem/pointer)
         status (af-mean-shift out-ptr rgb-img
                               15.0   ;; larger spatial window
                               40.0   ;; moderate color tolerance
                               7      ;; more iterations
                               1)]    ;; color mode (RGB)
     ;; Result: color-based segmentation
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (Fine Segmentation):
   ```clojure
   ;; Preserve fine details with small sigmas
   (let [image (create-array data [1024 768 3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         status (af-mean-shift out-ptr image
                               5.0    ;; small spatial neighborhood
                               15.0   ;; color-sensitive
                               3      ;; few iterations (faster)
                               1)]    ;; color mode
     ;; Result: many small regions, details preserved
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (Coarse Segmentation):
   ```clojure
   ;; Create large uniform regions
   (let [image (create-array data [640 480])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         status (af-mean-shift out-ptr image
                               25.0   ;; large spatial neighborhood
                               60.0   ;; permissive color tolerance
                               10     ;; full convergence
                               0)]    ;; grayscale
     ;; Result: few large homogeneous regions
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (Denoising Only):
   ```clojure
   ;; Remove noise without full segmentation
   (let [noisy-img (create-array data [512 512 3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         status (af-mean-shift out-ptr noisy-img
                               7.0    ;; moderate spatial smoothing
                               25.0   ;; color-aware smoothing
                               2      ;; just 2 iterations
                               1)]    ;; color mode
     ;; Result: denoised image, not fully segmented
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (Medical Imaging):
   ```clojure
   ;; Segment medical image (CT/MRI) for organ detection
   (let [medical-img (create-array ct-data [512 512])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         status (af-mean-shift out-ptr medical-img
                               8.0    ;; anatomical structures
                               20.0   ;; tissue intensity differences
                               6      ;; good convergence
                               0)]    ;; grayscale
     ;; Result: organ/tissue segmentation
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (Video Frame for Tracking):
   ```clojure
   ;; Prepare frame for CAMShift object tracking
   (let [video-frame (create-array frame-data [640 480 3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         status (af-mean-shift out-ptr video-frame
                               12.0   ;; track larger objects
                               35.0   ;; moderate color tolerance
                               5      ;; standard convergence
                               1)]    ;; color mode
     ;; Result: simplified frame for tracking
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Applications:
   - Image segmentation: Partition into homogeneous regions
   - Object detection: Isolate objects by color/texture
   - Denoising: Remove noise while preserving edges
   - Color quantization: Reduce number of distinct colors
   - Video tracking: CAMShift algorithm for object tracking
   - Medical imaging: Organ/tissue segmentation
   - Remote sensing: Land cover classification
   - Feature extraction: Region-based analysis
   
   Workflow Tips:
   
   **For segmentation**:
   1. Apply mean shift with appropriate parameters
   2. Extract connected components
   3. Filter small regions if needed
   4. Analyze region properties
   
   **For tracking (CAMShift)**:
   1. Select region of interest
   2. Apply mean shift to find mode
   3. Update region based on mode location
   4. Iterate for each video frame
   
   **For denoising**:
   1. Use fewer iterations (1-3)
   2. Moderate spatial_sigma
   3. Lower chromatic_sigma than for segmentation
   4. Don't expect full segmentation
   
   Comparison:
   - **vs Bilateral Filter**: Mean shift creates uniform regions (segmentation),
     bilateral just smooths. Mean shift is iterative, bilateral single-pass.
   - **vs K-Means**: Mean shift finds modes automatically, K-means needs K.
     Mean shift is spatial, K-means works in feature space only.
   - **vs Watershed**: Mean shift is feature-based, watershed is gradient-based.
     Mean shift better for color/texture, watershed for boundaries.
   
   Notes:
   - Algorithm is deterministic (same input → same output)
   - Convergence creates piecewise-constant regions
   - Each pixel converges to a local mode independently
   - More iterations give smoother, more segmented results
   - GPU parallelization makes it practical for real-time use
   - Adaptive stopping can reduce computation (check convergence)
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-bilateral: Edge-preserving smoothing filter
   - af-medfilt: Median filter for impulse noise
   - Connected components for post-segmentation analysis
   - Region properties for feature extraction"
  "af_mean_shift" [::mem/pointer ::mem/pointer ::mem/float ::mem/float ::mem/int ::mem/int] ::mem/int)
