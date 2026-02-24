(ns org.soulspace.arrayfire.ffi.c-api.filters
  "Bindings for the ArrayFire image filtering functions.
   
   Image filters are fundamental operations in image processing and computer
   vision that transform images by applying various mathematical operations
   to neighborhoods of pixels.
   
   Categories of Filters:
   
   1. **Median Filters** (Non-linear):
      - Replace each pixel with median of neighborhood
      - Excellent for salt-and-pepper noise removal
      - Preserves edges better than mean filters
      - O(N log N) complexity per pixel (sorting)
   
   2. **Morphological Filters**:
      - Minimum filter (erosion): min value in window
      - Maximum filter (dilation): max value in window
      - Binary morphology: shape operations
      - O(N) complexity with separable kernels
   
   3. **Bilateral Filter** (Edge-preserving):
      - Combines spatial and range (intensity) filtering
      - Gaussian-weighted average based on:
        * Spatial distance (spatial_sigma)
        * Intensity difference (chromatic_sigma)
      - Preserves edges while smoothing
      - O(N * kernel_size²) per pixel
   
   4. **Mean Shift Filter**:
      - Iterative mode-seeking algorithm
      - Moves each pixel toward local mode
      - Used for segmentation and denoising
      - O(N * iterations) per pixel
   
   5. **Sobel Filter** (Edge detection):
      - Computes image gradient approximation
      - Separate X and Y derivatives
      - Uses 3×3 or larger kernels
      - Foundation for edge detection
   
   6. **Color Space Conversion**:
      - RGB ↔ Grayscale conversion
      - Weighted combination of channels
   
   Mathematical Foundations:
   
   **Median Filter**:
   Output[x,y] = median{Input[x+i, y+j] | (i,j) ∈ Window}
   
   **Bilateral Filter**:
   Output[x,y] = (1/W) * Σ Input[i,j] * w_spatial[i,j] * w_range[i,j]
   where:
   - w_spatial[i,j] = exp(-||p - q||² / (2 * σ_s²))  (Gaussian spatial weight)
   - w_range[i,j] = exp(-||I(p) - I(q)||² / (2 * σ_r²))  (Gaussian range weight)
   - W = normalization factor (sum of all weights)
   
   **Sobel Operator**:
   Gx = [[-1 0 1]    Gy = [[-1 -2 -1]
         [-2 0 2]           [ 0  0  0]
         [-1 0 1]]          [ 1  2  1]]
   
   Gradient magnitude: G = √(Gx² + Gy²)
   Gradient direction: θ = atan2(Gy, Gx)
   
   Performance Considerations:
   - GPU acceleration provides 10-100× speedup
   - Median filter: Consider approximations for very large windows
   - Bilateral filter: Computation grows with kernel size
   - Mean shift: Iterations control quality vs speed tradeoff
   
   Applications:
   - Medical imaging: Noise reduction while preserving anatomy
   - Photography: Smoothing, sharpening, edge detection
   - Computer vision: Preprocessing for feature detection
   - Video processing: Temporal filtering
   - Microscopy: Denoising while preserving structures
   
   See also:
   - Convolve functions for custom kernel filtering
   - Morphological operations (dilate/erode) in image.clj"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.loader]))

;; Median filters

;; af_err af_medfilt(af_array *out, const af_array in, const dim_t wind_length, const dim_t wind_width, const af_border_type edge_pad)
(defcfn af-medfilt
  "Apply 2D median filter to an image.
   
   Median filter replaces each pixel with the median value from its
   neighborhood window. This is particularly effective for removing
   salt-and-pepper noise while preserving edges.
   
   Parameters:
   - out: out pointer for filtered image
   - in: input image array
   - wind-length: window height (kernel size in y direction)
   - wind-width: window width (kernel size in x direction)
   - edge-pad: border handling mode (af_border_type enum)
     * AF_PAD_ZERO (0): Zero padding at borders
     * AF_PAD_SYM (1): Symmetric reflection at borders
   
   Algorithm:
   For each pixel (x, y):
   1. Extract window of size wind_width × wind_length centered at (x,y)
   2. Sort all values in the window
   3. Select median value (middle element)
   4. Replace pixel with median
   
   Performance:
   - Complexity: O(W*H * w*h * log(w*h))
     where W×H = image size, w×h = window size
   - GPU parallelizes across all pixels
   - For small windows (3×3, 5×5), very efficient
   - Larger windows become slower due to sorting
   
   Window Size Selection:
   - 3×3: Fast, light smoothing, preserves details
   - 5×5: Moderate smoothing, good noise removal
   - 7×7 or larger: Heavy smoothing, may blur edges
   - Odd sizes recommended (ensures center pixel)
   
   Type Support:
   - All numeric types: f32, f64, s32, u32, s16, u16, s8, u8
   - Complex types: Not supported (use magnitude first)
   - Boolean: Supported (b8)
   
   Example:
   ```clojure
   ;; Remove salt-and-pepper noise from image
   (let [noisy-img (create-array img-data [512 512])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         wind-len 5
         wind-wid 5
         edge-pad 0]  ; AF_PAD_ZERO
     (af-medfilt out-ptr noisy-img wind-len wind-wid edge-pad)
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Common Use Cases:
   - Salt-and-pepper noise removal
   - Impulse noise filtering
   - Image preprocessing for feature detection
   - Medical image denoising
   
   Notes:
   - Non-linear filter (unlike Gaussian blur)
   - Preserves edges better than linear smoothing
   - Does not create new values (output is from input)
   - For color images, apply to each channel separately
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-medfilt1: 1D median filter
   - af-medfilt2: Alias for 2D median filter
   - af-bilateral: Edge-preserving smooth filter"
  "af_medfilt" [::mem/pointer ::mem/pointer ::mem/long ::mem/long ::mem/int] ::mem/int)

;; af_err af_medfilt1(af_array *out, const af_array in, const dim_t wind_width, const af_border_type edge_pad)
(defcfn af-medfilt1
  "Apply 1D median filter to a signal or each row of an array.
   
   One-dimensional median filter for signal processing or row-wise
   filtering of images.
   
   Parameters:
   - out: out pointer for filtered signal
   - in: input signal/array
   - wind-width: window width (kernel size)
   - edge-pad: border handling mode
     * AF_PAD_ZERO (0): Zero padding
     * AF_PAD_SYM (1): Symmetric reflection
   
   Behavior:
   - For 1D arrays: Filters the signal
   - For 2D arrays: Filters each row independently
   - For higher dimensions: Processes along first dimension
   
   Algorithm:
   For each position i:
   1. Extract window of size wind_width centered at i
   2. Sort values in window
   3. Select median (middle element)
   4. Replace value with median
   
   Performance:
   - Complexity: O(N * w * log(w))
     where N = signal length, w = window size
   - Much faster than 2D median filter
   - Parallel across all rows/signals
   
   Example (1D signal):
   ```clojure
   ;; Remove spikes from time series
   (let [signal (create-array data [1000])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         window 7
         edge-pad 1]  ; AF_PAD_SYM
     (af-medfilt1 out-ptr signal window edge-pad)
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (2D - row-wise):
   ```clojure
   ;; Filter each row of data matrix
   (let [data (create-array matrix [100 500])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         window 5
         edge-pad 0]
     (af-medfilt1 out-ptr data window edge-pad)
     ;; Each of 100 rows filtered independently
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Applications:
   - Time series spike removal
   - Audio click/pop removal
   - Baseline correction
   - Sensor data cleaning
   
   Returns:
   ArrayFire error code (af_err enum)"
  "af_medfilt1" [::mem/pointer ::mem/pointer ::mem/long ::mem/int] ::mem/int)

;; af_err af_medfilt2(af_array *out, const af_array in, const dim_t wind_length, const dim_t wind_width, const af_border_type edge_pad)
(defcfn af-medfilt2
  "Apply 2D median filter to an image (explicit 2D version).
   
   Explicitly applies 2D median filter. Functionally identical to
   af-medfilt but more explicit about dimensionality.
   
   Parameters:
   - out: out pointer for filtered image
   - in: input image array (2D or higher)
   - wind-length: window height
   - wind-width: window width
   - edge-pad: border handling (0=zero, 1=symmetric)
   
   This function is identical to af-medfilt.
   Use af-medfilt2 when you want to emphasize 2D operation.
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-medfilt: General median filter (same as this)"
  "af_medfilt2" [::mem/pointer ::mem/pointer ::mem/long ::mem/long ::mem/int] ::mem/int)

;; Morphological filters (min/max)

;; af_err af_minfilt(af_array *out, const af_array in, const dim_t wind_length, const dim_t wind_width, const af_border_type edge_pad)
(defcfn af-minfilt
  "Apply minimum filter (morphological erosion) to an image.
   
   Minimum filter replaces each pixel with the minimum value in its
   neighborhood. This is equivalent to morphological erosion with a
   flat structuring element.
   
   Parameters:
   - out: out pointer for filtered image
   - in: input image array
   - wind-length: window height (must equal wind-width currently)
   - wind-width: window width (must equal wind-length currently)
   - edge-pad: border handling (must be AF_PAD_ZERO = 0 currently)
   
   Operation:
   Output[x,y] = min{Input[x+i, y+j] | (i,j) ∈ Window}
   
   Effects:
   - Erodes bright regions (shrinks them)
   - Expands dark regions (grows them)
   - Removes small bright details (noise)
   - Darkens the image overall
   
   Implementation:
   Internally uses af_erode with a constant mask of ones.
   Equivalent to: af_erode(in, constant_mask_of_ones)
   
   Performance:
   - Complexity: O(W*H * w*h) with naive implementation
   - Separable kernels: O(W*H * (w + h))
   - GPU parallelizes across pixels
   
   Window Size Effects:
   - Small (3×3): Removes small noise, slight erosion
   - Medium (5×5, 7×7): More aggressive erosion
   - Large: Significant structural changes
   
   Type Support:
   - All numeric types supported
   - Works on grayscale or per-channel basis
   
   Example:
   ```clojure
   ;; Remove small bright spots and thin bright lines
   (let [img (create-array img-data [512 512])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         size 5]
     (af-minfilt out-ptr img size size 0)  ; 5×5 window
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Applications:
   - Noise removal (small bright spots)
   - Object separation
   - Feature thinning
   - Morphological opening (minfilt + maxfilt)
   
   Constraints:
   - wind-length must equal wind-width (square window)
   - edge-pad must be AF_PAD_ZERO (0)
   - These may be relaxed in future versions
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-maxfilt: Maximum filter (dilation)
   - af-erode: Full morphological erosion with custom mask"
  "af_minfilt" [::mem/pointer ::mem/pointer ::mem/long ::mem/long ::mem/int] ::mem/int)

;; af_err af_maxfilt(af_array *out, const af_array in, const dim_t wind_length, const dim_t wind_width, const af_border_type edge_pad)
(defcfn af-maxfilt
  "Apply maximum filter (morphological dilation) to an image.
   
   Maximum filter replaces each pixel with the maximum value in its
   neighborhood. This is equivalent to morphological dilation with a
   flat structuring element.
   
   Parameters:
   - out: out pointer for filtered image
   - in: input image array
   - wind-length: window height (must equal wind-width currently)
   - wind-width: window width (must equal wind-length currently)
   - edge-pad: border handling (must be AF_PAD_ZERO = 0 currently)
   
   Operation:
   Output[x,y] = max{Input[x+i, y+j] | (i,j) ∈ Window}
   
   Effects:
   - Expands bright regions (grows them)
   - Erodes dark regions (shrinks them)
   - Removes small dark details (noise)
   - Brightens the image overall
   
   Implementation:
   Internally uses af_dilate with a constant mask of ones.
   Equivalent to: af_dilate(in, constant_mask_of_ones)
   
   Performance:
   - Complexity: O(W*H * w*h) naive
   - Separable: O(W*H * (w + h))
   - Highly parallel on GPU
   
   Window Size Effects:
   - Small (3×3): Fills small holes, slight dilation
   - Medium (5×5, 7×7): More aggressive expansion
   - Large: Significant structural changes
   
   Type Support:
   - All numeric types supported
   - Apply per-channel for color images
   
   Example:
   ```clojure
   ;; Fill small holes and connect nearby features
   (let [binary-img (create-array img-data [512 512])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         size 3]
     (af-maxfilt out-ptr binary-img size size 0)  ; 3×3 window
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Morphological Operations:
   - **Opening** = minfilt followed by maxfilt
     * Removes small bright objects
     * Smooths object contours
   - **Closing** = maxfilt followed by minfilt
     * Fills small holes
     * Connects nearby objects
   - **Gradient** = maxfilt - minfilt
     * Edge detection
     * Boundary extraction
   
   Applications:
   - Hole filling in binary images
   - Object expansion
   - Gap bridging
   - Peak detection
   
   Constraints:
   - wind-length must equal wind-width (square window)
   - edge-pad must be AF_PAD_ZERO (0)
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-minfilt: Minimum filter (erosion)
   - af-dilate: Full morphological dilation with custom mask"
  "af_maxfilt" [::mem/pointer ::mem/pointer ::mem/long ::mem/long ::mem/int] ::mem/int)

;; Edge-preserving filters

;; af_err af_bilateral(af_array *out, const af_array in, const float spatial_sigma, const float chromatic_sigma, const bool isColor)
(defcfn af-bilateral
  "Apply bilateral filter for edge-preserving smoothing.
   
   Bilateral filter is a non-linear, edge-preserving smoothing filter.
   It averages pixels based on both their spatial distance and intensity
   similarity, resulting in smoothing that preserves edges.
   
   Parameters:
   - out: out pointer for filtered image
   - in: input image array
   - spatial-sigma: spatial standard deviation (controls spatial extent)
   - chromatic-sigma: chromatic/range standard deviation (controls intensity)
   - is-color: boolean (int) indicating color vs grayscale
     * 0 (false): Grayscale image
     * 1 (true): Color image (RGB)
   
   Mathematical Formula:
   Output[p] = (1/W_p) * Σ_q Input[q] * w_s(p,q) * w_r(p,q)
   
   Where:
   - w_s(p,q) = exp(-||p - q||² / (2 * σ_s²))  [spatial weight]
   - w_r(p,q) = exp(-||I(p) - I(q)||² / (2 * σ_r²))  [range weight]
   - W_p = Σ_q w_s(p,q) * w_r(p,q)  [normalization]
   - p, q are pixel positions
   - ||p - q|| is Euclidean distance between positions
   - ||I(p) - I(q)|| is intensity difference
   
   Parameter Selection:
   
   **spatial_sigma** (σ_s):
   - Small (1-3): Small spatial neighborhood, less smoothing
   - Medium (5-10): Moderate smoothing
   - Large (15+): Large neighborhood, more smoothing
   - Controls how far pixels influence each other spatially
   
   **chromatic_sigma** (σ_r):
   - Small (10-30): Only similar intensities averaged, sharp edges
   - Medium (30-70): Moderate edge preservation
   - Large (100+): Less selective, approaches Gaussian blur
   - Controls sensitivity to intensity differences
   
   Typical Combinations:
   - Portrait smoothing: spatial=3, chromatic=50
   - Edge-preserving denoise: spatial=5, chromatic=30
   - Strong smoothing: spatial=10, chromatic=80
   
   Performance:
   - Complexity: O(W*H * kernel_size²)
   - More expensive than Gaussian blur
   - GPU provides significant speedup
   - Larger σ_s increases computation
   
   Type Support:
   - Input types: All numeric types (f32, f64, s32, u32, s16, u16, s8, u8)
   - Output type: float (f32) or double (f64) based on input
   - Integer inputs converted to float internally
   
   Example (Grayscale):
   ```clojure
   ;; Smooth while preserving edges
   (let [img (create-array img-data [512 512])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         spatial 5.0
         chromatic 50.0
         is-color 0]  ; Grayscale
     (af-bilateral out-ptr img spatial chromatic is-color)
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (Color):
   ```clojure
   ;; Color image smoothing
   (let [rgb-img (create-array img-data [512 512 3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         spatial 3.0
         chromatic 40.0
         is-color 1]  ; Color
     (af-bilateral out-ptr rgb-img spatial chromatic is-color)
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Applications:
   - Portrait photography (skin smoothing)
   - HDR tone mapping (preserve edges while compressing range)
   - Denoising medical images (preserve anatomy)
   - Stylization effects
   - Preprocessing for segmentation
   
   Advantages:
   - Preserves edges while smoothing
   - No ringing artifacts
   - Adapts to local image structure
   
   Limitations:
   - Computationally expensive
   - Parameter selection can be tricky
   - Not good for high-frequency texture
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-mean-shift: Iterative mode-seeking filter
   - af-medfilt: Median filter (also edge-preserving)"
  "af_bilateral" [::mem/pointer ::mem/pointer ::mem/float ::mem/float ::mem/int] ::mem/int)

;; af_err af_mean_shift(af_array *out, const af_array in, const float spatial_sigma, const float chromatic_sigma, const unsigned iter, const bool is_color)
(defcfn af-mean-shift
  "Apply mean shift filter for segmentation and denoising.
   
   Mean shift is an iterative mode-seeking algorithm that moves each
   pixel toward the mode (peak) of the local density. This results in
   piecewise-constant regions useful for segmentation.
   
   Parameters:
   - out: out pointer for filtered image
   - in: input image array
   - spatial-sigma: spatial standard deviation (window size)
   - chromatic-sigma: chromatic variance (color/intensity tolerance)
   - iter: number of iterations to perform
   - is-color: boolean (int) for color (1) vs grayscale (0)
   
   Algorithm:
   For each pixel, repeat 'iter' times:
   1. Define search window using spatial_sigma
   2. Compute weighted mean of pixels in window
   3. Weights based on chromatic_sigma (intensity similarity)
   4. Move pixel value toward the mean
   5. Converge to local mode
   
   After convergence, nearby pixels with similar colors and positions
   will have identical values, creating piecewise-constant regions.
   
   Parameter Selection:
   
   **spatial_sigma**:
   - Small (3-7): Fine segmentation, preserves details
   - Medium (10-20): Moderate region size
   - Large (20-40): Coarse segmentation, large regions
   
   **chromatic_sigma**:
   - Small (5-20): Color-sensitive, more segments
   - Medium (20-50): Moderate color tolerance
   - Large (50-100): Color-insensitive, fewer segments
   
   **iterations**:
   - Few (1-3): Partial convergence, faster
   - Moderate (5-10): Good convergence, typical
   - Many (15+): Full convergence, slower, may over-segment
   
   Performance:
   - Complexity: O(W*H * iter * kernel_size²)
   - Expensive, especially with many iterations
   - GPU parallelization crucial
   - Consider reducing iterations for speed
   
   Typical Settings:
   - General segmentation: spatial=10, chromatic=30, iter=5
   - Fine detail: spatial=5, chromatic=15, iter=3
   - Coarse regions: spatial=20, chromatic=50, iter=8
   
   Example (Segmentation):
   ```clojure
   ;; Segment image into regions
   (let [img (create-array img-data [512 512 3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         spatial 12.0
         chromatic 30.0
         iterations 5
         is-color 1]  ; RGB
     (af-mean-shift out-ptr img spatial chromatic iterations is-color)
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (Denoising):
   ```clojure
   ;; Denoise with fewer iterations
   (let [noisy-img (create-array img-data [512 512])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         spatial 8.0
         chromatic 20.0
         iterations 3
         is-color 0]  ; Grayscale
     (af-mean-shift out-ptr noisy-img spatial chromatic iterations is-color)
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Applications:
   - Image segmentation (region-based)
   - Object tracking (clustering)
   - Edge-preserving smoothing
   - Cartoon effects (posterization)
   - Preprocessing for recognition
   
   Output Characteristics:
   - Piecewise constant (flat regions)
   - Sharp boundaries preserved
   - Similar to cartoon/painting effect
   - Reduces color palette
   
   Advantages:
   - No assumption about region shape
   - Preserves discontinuities
   - Automatic mode detection
   - Robust to noise
   
   Limitations:
   - Computationally expensive
   - May over/under-segment
   - Parameter sensitive
   - Slow convergence possible
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-bilateral: Similar edge-preserving smoothing
   - Segmentation algorithms for region analysis"
  "af_mean_shift" [::mem/pointer ::mem/pointer ::mem/float ::mem/float ::mem/int ::mem/int] ::mem/int)

;; Edge detection

;; af_err af_sobel_operator(af_array *dx, af_array *dy, const af_array img, const unsigned ker_size)
(defcfn af-sobel-operator
  "Compute Sobel edge detection gradients.
   
   Sobel operator computes approximation of image gradients in X and Y
   directions using separable convolution. This is fundamental for edge
   detection and gradient-based image analysis.
   
   Parameters:
   - dx: out pointer for X gradient (horizontal edges)
   - dy: out pointer for Y gradient (vertical edges)
   - img: input image array
   - kernel-size: Sobel kernel size (3, 5, 7, etc. - must be odd)
   
   Standard 3×3 Sobel Kernels:
   
   Gx (X gradient):        Gy (Y gradient):
   [[-1  0  1]            [[-1 -2 -1]
    [-2  0  2]             [ 0  0  0]
    [-1  0  1]]            [ 1  2  1]]
   
   The kernels are separable:
   Gx = [-1 0 1] ⊗ [1 2 1]ᵀ
   Gy = [1 2 1] ⊗ [-1 0 1]ᵀ
   
   where ⊗ denotes outer product.
   
   Output Interpretation:
   - **dx** (Gx): Responds to vertical edges (intensity changes in X)
     * Positive: dark→bright (left to right)
     * Negative: bright→dark (left to right)
   - **dy** (Gy): Responds to horizontal edges (intensity changes in Y)
     * Positive: dark→bright (top to bottom)
     * Negative: bright→dark (top to bottom)
   
   Gradient Magnitude and Direction:
   magnitude = √(dx² + dy²)
   direction = atan2(dy, dx)  [angle in radians, -π to π]
   
   Kernel Size Effects:
   - 3×3: Standard, fast, good for sharp edges
   - 5×5: Smoother, less noise sensitivity
   - 7×7 or larger: Very smooth, blurred edges
   
   Performance:
   - Complexity: O(W*H * k) where k = kernel size
   - Separable implementation: 2 × 1D convolutions
   - Highly parallel on GPU
   - Fast for typical kernel sizes (3, 5)
   
   Type Support:
   - Input: All numeric types
   - Output: Float or double (preserves sign)
   
   Example (Basic edge detection):
   ```clojure
   (let [img (create-array img-data [512 512])
         dx-ptr (mem/alloc-pointer ::mem/pointer)
         dy-ptr (mem/alloc-pointer ::mem/pointer)
         kernel-size 3]
     (af-sobel-operator dx-ptr dy-ptr img kernel-size)
     (let [dx (mem/read-pointer dx-ptr ::mem/pointer)
           dy (mem/read-pointer dy-ptr ::mem/pointer)]
       ;; Compute magnitude: mag = sqrt(dx² + dy²)
       ;; Compute direction: dir = atan2(dy, dx)
       {:dx dx :dy dy}))
   ```
   
   Example (Edge magnitude):
   ```clojure
   ;; Get edge strength
   (let [img (create-array gray-img [512 512])
         dx-ptr (mem/alloc-pointer ::mem/pointer)
         dy-ptr (mem/alloc-pointer ::mem/pointer)]
     (af-sobel-operator dx-ptr dy-ptr img 3)
     (let [dx (mem/read-pointer dx-ptr ::mem/pointer)
           dy (mem/read-pointer dy-ptr ::mem/pointer)]
       ;; magnitude = hypot(dx, dy) = sqrt(dx² + dy²)
       (af-hypot mag-ptr dx dy 0)  ; Compute gradient magnitude
       (mem/read-pointer mag-ptr ::mem/pointer)))
   ```
   
   Applications:
   - Edge detection (with thresholding)
   - Feature detection (corners, lines)
   - Image segmentation
   - Motion detection (temporal gradient)
   - Texture analysis
   - Image sharpening (unsharp mask)
   
   Edge Detection Pipeline:
   1. Apply Sobel to get dx, dy
   2. Compute magnitude: mag = √(dx² + dy²)
   3. Threshold magnitude for edges
   4. Optional: Non-maximum suppression
   5. Optional: Edge linking/tracking
   
   Notes:
   - For color images, apply to each channel or convert to grayscale
   - Sobel is first-order derivative approximation
   - Consider Gaussian smoothing before Sobel for noisy images
   - Canny edge detector uses Sobel as a component
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-canny: Complete Canny edge detection
   - Convolution functions for custom gradient kernels"
  "af_sobel_operator" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; Color space conversion

;; af_err af_rgb2gray(af_array *out, const af_array in, const float rPercent, const float gPercent, const float bPercent)
(defcfn af-rgb2gray
  "Convert RGB image to grayscale.
   
   Converts a color (RGB) image to grayscale using weighted combination
   of red, green, and blue channels.
   
   Parameters:
   - out: out pointer for grayscale image
   - in: input RGB image [H × W × 3] or [H × W × 3 × N] for batch
   - r-percent: weight for red channel (typically 0.299)
   - g-percent: weight for green channel (typically 0.587)
   - b-percent: weight for blue channel (typically 0.114)
   
   Formula:
   Gray = r_percent * R + g_percent * G + b_percent * B
   
   Standard Weights (ITU-R BT.601):
   - Red: 0.299 (30%)
   - Green: 0.587 (59%)
   - Blue: 0.114 (11%)
   
   These weights reflect human perception - we're most sensitive to
   green, less to red, and least to blue.
   
   Alternative Weight Sets:
   - **ITU-R BT.709** (HDTV): R=0.2126, G=0.7152, B=0.0722
   - **ITU-R BT.2100** (HDR): R=0.2627, G=0.6780, B=0.0593
   - **Equal weights**: R=0.333, G=0.333, B=0.333 (simple average)
   - **Custom**: Any combination summing to ~1.0
   
   Input Format:
   - 2D color: [H × W × 3] → Output: [H × W]
   - Batch: [H × W × 3 × N] → Output: [H × W × N]
   - Channel order: RGB (not BGR)
   
   Performance:
   - Complexity: O(W*H*3) - processes all pixels once
   - Very fast, simple weighted sum
   - GPU parallelizes across all pixels
   
   Example (Standard conversion):
   ```clojure
   ;; Convert RGB to grayscale using ITU-R BT.601
   (let [rgb-img (create-array img-data [512 512 3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         r 0.299
         g 0.587
         b 0.114]
     (af-rgb2gray out-ptr rgb-img r g b)
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (HDTV weights):
   ```clojure
   ;; Use BT.709 weights for HDTV content
   (let [rgb-img (create-array hdtv-data [1920 1080 3])
         out-ptr (mem/alloc-pointer ::mem/pointer)]
     (af-rgb2gray out-ptr rgb-img 0.2126 0.7152 0.0722)
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (Equal weights):
   ```clojure
   ;; Simple average (non-perceptual)
   (let [rgb-img (create-array img-data [512 512 3])
         out-ptr (mem/alloc-pointer ::mem/pointer)]
     (af-rgb2gray out-ptr rgb-img 0.333 0.333 0.334)
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Applications:
   - Preprocessing for algorithms requiring grayscale
   - Reducing data dimensionality (3 channels → 1)
   - Display on grayscale devices
   - Simplifying visualization
   - Feature extraction (many algorithms work on gray)
   
   Type Preservation:
   - Input type determines output type
   - Float input → Float output
   - Integer input → Integer output (may clip)
   
   Notes:
   - Weights should typically sum to 1.0 for proper scaling
   - If weights don't sum to 1.0, output range may differ from input
   - For perceptual accuracy, use standard weights
   - Consider input color space (sRGB vs linear RGB)
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-gray2rgb: Convert grayscale to RGB
   - af-color-space: General color space conversion"
  "af_rgb2gray" [::mem/pointer ::mem/pointer ::mem/float ::mem/float ::mem/float] ::mem/int)

;; af_err af_gray2rgb(af_array *out, const af_array in, const float rFactor, const float gFactor, const float bFactor)
(defcfn af-gray2rgb
  "Convert grayscale image to RGB.
   
   Converts a grayscale image to 3-channel RGB by replicating the
   grayscale values with optional per-channel scaling factors.
   
   Parameters:
   - out: out pointer for RGB image
   - in: input grayscale image [H × W] or [H × W × N] for batch
   - r-factor: scaling factor for red channel (typically 1.0)
   - g-factor: scaling factor for green channel (typically 1.0)
   - b-factor: scaling factor for blue channel (typically 1.0)
   
   Formula:
   R = gray * r_factor
   G = gray * g_factor
   B = gray * b_factor
   
   Standard Usage (factors = 1.0):
   Simply replicates grayscale to all three channels, creating
   a neutral gray RGB image.
   
   Custom Factors:
   - Tinting: Use different factors to add color tone
   - Sepia: R=1.0, G=0.95, B=0.82 (warm brownish tone)
   - Cool tone: R=0.9, G=0.95, B=1.0 (bluish)
   - Contrast per channel: Adjust individual channels
   
   Output Format:
   - Grayscale: [H × W] → RGB: [H × W × 3]
   - Batch: [H × W × N] → RGB: [H × W × 3 × N]
   
   Performance:
   - Complexity: O(W*H*3) - creates 3 channels
   - Fast, simple replication and scaling
   - Memory increases 3× (1 channel → 3 channels)
   
   Example (Standard conversion):
   ```clojure
   ;; Convert grayscale to neutral RGB
   (let [gray-img (create-array img-data [512 512])
         out-ptr (mem/alloc-pointer ::mem/pointer)]
     (af-gray2rgb out-ptr gray-img 1.0 1.0 1.0)
     ;; Output: [512 512 3] with R=G=B=gray
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (Sepia tone):
   ```clojure
   ;; Create sepia-toned image from grayscale
   (let [gray-img (create-array img-data [512 512])
         out-ptr (mem/alloc-pointer ::mem/pointer)]
     (af-gray2rgb out-ptr gray-img 1.0 0.95 0.82)
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (Cool tone):
   ```clojure
   ;; Add bluish tint
   (let [gray-img (create-array img-data [512 512])
         out-ptr (mem/alloc-pointer ::mem/pointer)]
     (af-gray2rgb out-ptr gray-img 0.9 0.95 1.0)
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Applications:
   - Displaying grayscale in RGB context
   - Tinting/colorization effects
   - Preprocessing for RGB-only algorithms
   - Creating false color visualizations
   - Compositing with color images
   
   Use Cases:
   - Medical imaging: Colorize different modalities
   - Visualization: Add color coding to scalar fields
   - Video processing: Mix grayscale with color footage
   - Artistic effects: Selective colorization
   
   Notes:
   - Not a true colorization (no color information added)
   - Simply replicates intensity to all channels
   - Factors should typically be near 1.0 for natural appearance
   - Factors > 1.0 may cause clipping with integer types
   - For true colorization, use machine learning approaches
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-rgb2gray: Convert RGB to grayscale
   - af-color-space: General color space conversion"
  "af_gray2rgb" [::mem/pointer ::mem/pointer ::mem/float ::mem/float ::mem/float] ::mem/int)
