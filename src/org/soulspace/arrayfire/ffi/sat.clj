(ns org.soulspace.arrayfire.ffi.sat
  "Bindings for the ArrayFire Summed Area Table (SAT) function.
   
   A Summed Area Table (also known as Integral Image) is a data structure
   and algorithm for quickly and efficiently computing sum of values in a
   rectangular subset of a grid. It is fundamental to many computer vision
   and image processing algorithms.
   
   Mathematical Definition:
   
   For an input image I[x,y], the summed area table SAT[x,y] is defined as:
   
   SAT[x,y] = Σ(i=0 to x) Σ(j=0 to y) I[i,j]
   
   In other words, each position in the SAT contains the sum of all pixels
   in the rectangle from the origin (0,0) to that position (x,y).
   
   Properties:
   
   1. **Cumulative Sum**: Each element is the sum of all elements above
      and to the left of it (including itself).
   
   2. **Fast Rectangle Summation**: Using the SAT, the sum of any rectangle
      can be computed in O(1) time using just 4 lookups:
      
      Sum(x1,y1 to x2,y2) = SAT[x2,y2] - SAT[x1-1,y2]
                            - SAT[x2,y1-1] + SAT[x1-1,y1-1]
   
   3. **Preprocessing Cost**: Building the SAT takes O(W*H) time for an
      image of size W×H, after which any rectangular sum query is O(1).
   
   Example Computation:
   
   Input Image:
   [1 2 3]
   [4 5 6]
   [7 8 9]
   
   Summed Area Table:
   [ 1   3   6]    (1, 1+2, 1+2+3)
   [ 5  12  21]    (1+4, 1+2+4+5, 1+2+3+4+5+6)
   [12  27  45]    (1+4+7, ..., sum of all)
   
   To compute sum of rectangle (1,1) to (2,2):
   Sum = SAT[2,2] - SAT[0,2] - SAT[2,0] + SAT[0,0]
       = 45 - 12 - 6 + 1 = 28
   Verification: 5+6+8+9 = 28 ✓
   
   Type Conversions:
   
   The SAT function may change the output type to prevent overflow:
   - f64 input → f64 output (no change)
   - f32 input → f32 output (no change)
   - s32 input → s32 output (watch for overflow on large images!)
   - u32 input → u32 output (watch for overflow on large images!)
   - s64 input → s64 output (no change)
   - u64 input → u64 output (no change)
   - s16 input → s32 output (promoted to prevent overflow)
   - u16 input → u32 output (promoted to prevent overflow)
   - s8 input  → s32 output (promoted to prevent overflow)
   - u8 input  → u32 output (promoted to prevent overflow)
   - b8 input  → s32 output (promoted to prevent overflow)
   
   Overflow Considerations:
   
   For integer types, the maximum value in the SAT is the sum of all pixel
   values. Choose appropriate data types:
   
   - u8 image of 1000×1000 with all pixels = 255:
     Max SAT value = 255 * 1,000,000 = 255,000,000
     Requires at least u32 (max 4,294,967,295) ✓
   
   - u8 image of 4096×4096 with all pixels = 255:
     Max SAT value = 255 * 16,777,216 ≈ 4.28 billion
     u32 would overflow! Use u64 or f32/f64
   
   - For safety with integer types, use s64 or u64 for large images
   - Float types (f32, f64) handle large sums but may lose precision
   
   Applications:
   
   1. **Haar-like Features** (Viola-Jones face detection):
      - Compute rectangle feature differences in O(1)
      - Essential for real-time object detection
      - 24×24 pixel window can evaluate 180,000+ features/second
   
   2. **Box Filtering**:
      - Fast mean/average filtering
      - Any kernel size in constant time
      - Used for: blur, smoothing, background estimation
   
   3. **Template Matching**:
      - Fast normalized cross-correlation
      - Compute mean and variance of image patches quickly
   
   4. **Adaptive Thresholding**:
      - Compute local mean for each pixel
      - Used in document processing, QR code detection
   
   5. **Image Pyramids**:
      - Fast downsampling by averaging regions
      - Multi-scale image analysis
   
   6. **Texture Analysis**:
      - Fast computation of local statistics (mean, variance)
      - GLCM (Gray Level Co-occurrence Matrix) features
   
   7. **Optical Flow**:
      - Fast computation of image gradients over windows
      - Motion estimation in video
   
   8. **Dense Stereo Matching**:
      - Sum of absolute differences (SAD) computation
      - Depth estimation from stereo pairs
   
   Performance:
   
   - Complexity: O(W*H) to build SAT
   - GPU parallelization: Each pixel computed independently using prefix sum
   - Memory: Requires additional array of same size as input
   - Cache-friendly: Sequential memory access pattern
   - Typical speedup on GPU: 50-200× faster than CPU
   
   Multi-dimensional Arrays:
   
   - For 2D images: Computes standard 2D SAT
   - For 3D arrays: Computes SAT along first two dimensions for each slice
     * Example: RGB image → SAT computed per color channel
   - For 4D arrays: Batch processing of multiple images
     * Each batch element processed independently
   
   Limitations:
   
   - Input must be at least 2D (ARG_ASSERT will fail for 1D arrays)
   - Only works on first two dimensions (height × width)
   - Higher dimensions are treated as channels/batches
   - No support for complex types (c32, c64)
   - Integer overflow possible for large images with large values
   
   Best Practices:
   
   1. **Choose appropriate output type**:
      - Use f64 for maximum precision and range
      - Use u64/s64 for large integer images
      - Use f32 for good balance (GPU-optimized)
   
   2. **Pre-convert input if needed**:
      - Convert u8 to f32 before SAT for large images
      - This prevents overflow and enables GPU optimizations
   
   3. **Batch processing**:
      - Stack multiple images in higher dimensions
      - GPU processes all batches in parallel
   
   4. **Memory management**:
      - SAT doubles memory usage (input + output)
      - Consider downsampling large images first
   
   Example: Box Filter using SAT
   
   To compute mean of k×k box around each pixel using SAT:
   
   1. Compute SAT once: O(W*H)
   2. For each pixel (x,y):
      sum = SAT[x+k/2, y+k/2] - SAT[x-k/2-1, y+k/2]
            - SAT[x+k/2, y-k/2-1] + SAT[x-k/2-1, y-k/2-1]
      mean = sum / (k * k)
   
   This is O(W*H) total, independent of kernel size k!
   Without SAT, it would be O(W*H*k²).
   
   Historical Note:
   
   The summed area table concept was introduced by Franklin Crow in 1984
   for texture mapping in computer graphics. It was popularized in computer
   vision by Viola and Jones (2001) in their groundbreaking real-time face
   detection algorithm using Haar-like features.
   
   See also:
   - Convolve functions for general filtering
   - Histogram for frequency analysis
   - Integral functions in blas.clj for 1D cumulative sums"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; af_err af_sat(af_array *out, const af_array in)
(defcfn af-sat
  "Compute the Summed Area Table (Integral Image) of an array.
   
   Computes the cumulative sum of all elements from the origin (0,0) to
   each position (x,y) in a 2D array. This enables constant-time computation
   of the sum of any rectangular region.
   
   Parameters:
   - out: Output pointer for the summed area table
   - in: Input array (must be at least 2D)
   
   Mathematical Definition:
   For input I[x,y], output SAT[x,y] = Σ(i≤x, j≤y) I[i,j]
   
   Type Conversions:
   To prevent overflow, some types are automatically promoted:
   - f64, f32, s32, u32, s64, u64: No conversion
   - s16 → s32 (promoted)
   - u16 → u32 (promoted)
   - s8  → s32 (promoted)
   - u8  → u32 (promoted)
   - b8  → s32 (promoted)
   
   Dimensionality:
   - 2D arrays: Standard summed area table
   - 3D arrays: SAT computed for each 2D slice (e.g., per color channel)
   - 4D arrays: Batch processing (SAT for each batch element)
   - SAT always computed along first two dimensions (height × width)
   
   Fast Rectangle Sum:
   After computing SAT, any rectangle sum is O(1):
   
   sum(x1,y1 to x2,y2) = SAT[x2,y2]
                         - SAT[x1-1,y2]
                         - SAT[x2,y1-1]
                         + SAT[x1-1,y1-1]
   
   (Handle boundaries: use 0 for out-of-bounds indices)
   
   Example (basic usage):
   ```clojure
   (let [img (create-array [[1 2 3]
                            [4 5 6]
                            [7 8 9]] [3 3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-sat out-ptr img)]
     (mem/read-pointer out-ptr ::mem/pointer))
   ;; Result: [[1   3   6]
   ;;          [5  12  21]
   ;;          [12 27  45]]
   ```
   
   Example (box filter using SAT):
   ```clojure
   ;; Compute 5×5 box filter (mean) using SAT
   (let [img (create-array img-data [512 512])
         sat-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-sat sat-ptr img)
         sat (mem/read-pointer sat-ptr ::mem/pointer)
         k 5
         half-k (quot k 2)]
     ;; For each pixel, compute mean of k×k box using 4 SAT lookups
     ;; This is O(1) per pixel, independent of k!
     (compute-box-mean sat half-k))
   ```
   
   Example (Haar-like features for face detection):
   ```clojure
   ;; Compute horizontal edge feature: top rectangle - bottom rectangle
   (let [img (create-array img-data [24 24])  ; Face detection window
         sat-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-sat sat-ptr img)
         sat (mem/read-pointer sat-ptr ::mem/pointer)]
     ;; Top half sum - Bottom half sum (O(1) using SAT)
     (- (rect-sum sat 0 0 24 12)
        (rect-sum sat 0 12 24 24)))
   ```
   
   Example (adaptive thresholding):
   ```clojure
   ;; Compute local mean for adaptive threshold
   (let [img (create-array scan-data [2000 1500])
         sat-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-sat sat-ptr img)
         sat (mem/read-pointer sat-ptr ::mem/pointer)
         window-size 15]
     ;; For each pixel, threshold based on local mean
     (adaptive-threshold img sat window-size))
   ```
   
   Example (prevent overflow with type conversion):
   ```clojure
   ;; Large u8 image might overflow u32, so convert to f32 first
   (let [large-img (create-array img-data [4096 4096])  ; u8 type
         ;; Convert to f32 to prevent overflow
         img-f32 (cast large-img AF_DTYPE_F32)
         sat-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-sat sat-ptr img-f32)]
     (mem/read-pointer sat-ptr ::mem/pointer))
   ```
   
   Example (batch processing multiple images):
   ```clojure
   ;; Process batch of images (4D array: batch × height × width × channels)
   (let [batch (create-array batch-data [100 128 128 3])  ; 100 RGB images
         sat-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-sat sat-ptr batch)]
     ;; SAT computed for each of 100 images, 3 channels each
     (mem/read-pointer sat-ptr ::mem/pointer))
   ```
   
   Performance:
   - Complexity: O(W*H) to build SAT
   - GPU highly parallel (prefix sum algorithm)
   - Memory: O(W*H) additional storage
   - After building SAT, rectangle queries are O(1)
   - Typical GPU speedup: 50-200× vs CPU
   
   Applications:
   - **Viola-Jones face detection**: Haar-like features in O(1)
   - **Box filtering**: Fast mean/blur filters of any size
   - **Template matching**: Fast normalized cross-correlation
   - **Adaptive thresholding**: Local statistics for document processing
   - **Stereo matching**: Fast SAD (Sum of Absolute Differences)
   - **Texture analysis**: Fast local mean and variance
   
   Overflow Warning:
   For integer types, maximum SAT value = sum of all pixels.
   
   Example overflow calculation:
   - Image: 4096×4096 pixels, all u8 = 255
   - Max SAT: 255 × 16,777,216 = 4,278,190,080
   - Exceeds u32 max (4,294,967,295) by narrow margin!
   - Solution: Convert to u64, s64, or f32/f64 first
   
   Rule of thumb for u8 images:
   - Width × Height × 255 < 2³² (for u32 output)
   - Width × Height < 16,843,009 pixels for u32 safety
   - For larger images: use f32, f64, or u64
   
   Input Requirements:
   - Must be at least 2D (will assert-fail for 1D arrays)
   - Supported types: f64, f32, s32, u32, s64, u64, s16, u16, s8, u8, b8
   - Complex types (c32, c64) not supported
   
   Notes:
   - SAT is also called \"Integral Image\" in computer vision
   - Introduced by Franklin Crow (1984) for texture mapping
   - Popularized by Viola-Jones (2001) for face detection
   - Essential for many real-time vision algorithms
   - Trade-off: O(W*H) preprocessing for O(1) rectangle queries
   
   Returns:
   ArrayFire error code (af_err enum)
   - AF_SUCCESS (0): SAT computed successfully
   - AF_ERR_ARG: Invalid arguments (e.g., 1D input)
   - AF_ERR_SIZE: Invalid dimensions
   - AF_ERR_TYPE: Unsupported data type (e.g., complex)
   
   See also:
   - Box filtering functions for mean filters
   - Histogram for frequency-based analysis
   - Convolve for general filtering (slower but more flexible)"
  "af_sat" [::mem/pointer ::mem/pointer] ::mem/int)
