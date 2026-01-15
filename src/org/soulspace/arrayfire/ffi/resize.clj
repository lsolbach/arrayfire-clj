(ns org.soulspace.arrayfire.ffi.resize
  "Bindings for the ArrayFire image resizing function.
   
   Image resizing is a fundamental operation in image processing that changes
   the dimensions of an image by resampling its pixels using various
   interpolation methods.
   
   What is Image Resizing?
   
   Image resizing changes the spatial resolution of an image by computing new
   pixel values at target positions through interpolation. The quality and
   characteristics of the result depend heavily on the interpolation method
   chosen.
   
   **Key Concepts**:
   - **Upsampling**: Increasing image dimensions (requires interpolation)
   - **Downsampling**: Decreasing image dimensions (may cause aliasing)
   - **Aspect Ratio**: Ratio of width to height (can be preserved or changed)
   - **Interpolation**: Method for estimating pixel values at non-integer positions
   
   Mathematical Foundation:
   
   For a 2D image, resizing maps input coordinates (x_in, y_in) to output
   coordinates (x_out, y_out):
   
   x_in = x_out * (width_in / width_out)
   y_in = y_out * (height_in / height_out)
   
   The value at (x_out, y_out) is computed from surrounding pixels using the
   chosen interpolation method.
   
   Interpolation Methods:
   
   1. **Nearest Neighbor (AF_INTERP_NEAREST)**:
      - Algorithm: Use value of closest pixel
      - Formula: Output[x,y] = Input[round(x_in), round(y_in)]
      - Characteristics:
        * Fastest method (no arithmetic operations)
        * Preserves exact input values
        * Creates blocky/pixelated appearance when upsampling
        * No new colors introduced
        * Good for binary images, masks, label maps
      - Complexity: O(N) where N = output pixels
      - Use when: Speed critical, exact values needed, categorical data
   
   2. **Bilinear (AF_INTERP_BILINEAR)**:
      - Algorithm: Linear interpolation in both dimensions
      - Formula:
        ```
        f(x,y) = f(0,0)(1-x)(1-y) + f(1,0)x(1-y) + 
                 f(0,1)(1-x)y + f(1,1)xy
        ```
      - Process:
        1. Find 4 surrounding pixels
        2. Interpolate horizontally (2 values)
        3. Interpolate vertically (final value)
      - Characteristics:
        * Good balance of speed and quality
        * Smooth gradients
        * Slight blur when downsampling
        * Most common general-purpose method
      - Complexity: O(4N) - 4 pixel reads + 3 linear interpolations
      - Use when: General-purpose resizing, reasonable quality needed
   
   3. **Lower (AF_INTERP_LOWER)**:
      - Algorithm: Floor of coordinate (always round down)
      - Formula: Output[x,y] = Input[floor(x_in), floor(y_in)]
      - Characteristics:
        * Similar to nearest neighbor but deterministic
        * Always selects lower-left pixel
        * Faster than bilinear, slower than nearest
        * Useful for specific sampling patterns
      - Complexity: O(N)
      - Use when: Specific sampling bias needed
   
   4. **Bicubic/Cosine/Spline (via fallback to af_scale)**:
      - Algorithm: Higher-order polynomial interpolation
      - Uses 16 surrounding pixels (4×4 neighborhood)
      - Smoother results than bilinear
      - More computationally expensive
      - Better for high-quality upsampling
      - Note: Not directly supported by af_resize, uses af_scale
   
   Implementation Strategy:
   
   ArrayFire resize uses an optimized GPU parallel approach:
   
   ```cpp
   // CPU Backend (simplified)
   for (y = 0; y < output_height; y++) {
       for (x = 0; x < output_width; x++) {
           float x_in = x * x_scale;  // x_scale = in_width / out_width
           float y_in = y * y_scale;  // y_scale = in_height / out_height
           output[x,y] = interpolate(input, x_in, y_in, method);
       }
   }
   
   // GPU Backend (CUDA/OpenCL/OneAPI)
   // Thread block: 16×16 threads
   // Each thread computes one output pixel
   // Coalesced memory access for efficiency
   ```
   
   GPU Optimization Details:
   
   - **CUDA/OpenCL/OneAPI**:
     * Thread block: 16×16 (256 threads)
     * Each thread: One output pixel
     * Texture memory: Optional (hardware interpolation)
     * Coalesced reads: Optimized memory access pattern
   
   Performance Characteristics:
   
   **Time Complexity**:
   - Nearest/Lower: O(N) where N = output pixels
   - Bilinear: O(4N) for memory reads, O(N) for computation
   - GPU: Highly parallel, ~1000× faster than CPU for large images
   
   **Space Complexity**:
   - O(M + N) where M = input size, N = output size
   - Creates new output array
   - Input remains unchanged
   
   **Throughput** (typical GPU, 512×512 image):
   - Nearest: ~5000 fps
   - Bilinear: ~3000 fps
   - Bicubic: ~500 fps (via af_scale)
   
   Type Support:
   
   All numeric ArrayFire types:
   - Floating point: f32, f64
   - Complex: c32, c64
   - Integer: s32, u32, s64, u64, s16, u16, s8, u8
   - Boolean: b8
   
   For complex numbers:
   - Interpolates real and imaginary parts independently
   - Maintains phase information
   
   Common Resizing Patterns:
   
   1. **Scale by Factor**:
      ```
      new_width = width × scale_x
      new_height = height × scale_y
      ```
   
   2. **Fit to Target (preserve aspect)**:
      ```
      scale = min(target_w/width, target_h/height)
      new_w = width × scale
      new_h = height × scale
      ```
   
   3. **Fill Target (may crop)**:
      ```
      scale = max(target_w/width, target_h/height)
      new_w = width × scale
      new_h = height × scale
      ```
   
   4. **Thumbnail Generation**:
      ```
      max_dim = 256
      scale = max_dim / max(width, height)
      ```
   
   Aliasing and Quality Considerations:
   
   **Downsampling Issues**:
   - High frequencies can cause aliasing (Moiré patterns)
   - Solution: Apply low-pass filter before downsampling
   - Example: Gaussian blur with σ ≈ 0.5 × downsample_factor
   
   **Upsampling Issues**:
   - Cannot add missing detail
   - Interpolation smooths but doesn't enhance
   - Consider bicubic for smoother results
   - For large factors (>4×), consider iterative upsampling
   
   **Aspect Ratio**:
   - Changing aspect ratio causes distortion
   - Consider cropping or padding to maintain ratio
   - For thumbnails, pad with constant color
   
   Applications:
   
   1. **Image Processing**:
      - Thumbnail generation
      - Image pyramids (multiscale processing)
      - Resolution matching between sources
   
   2. **Computer Vision**:
      - Neural network input preparation (resize to fixed size)
      - Feature detection at multiple scales
      - Image registration (align different resolution images)
   
   3. **Machine Learning**:
      - Data augmentation (random scale)
      - Input normalization (fixed network input size)
      - Multi-scale training
   
   4. **Display and Rendering**:
      - Screen resolution adaptation
      - Print resolution conversion
      - Video scaling
   
   5. **Medical Imaging**:
      - DICOM image standardization
      - Anisotropic voxel spacing correction
      - Multi-modal registration
   
   6. **Microscopy**:
      - Resolution matching between modalities
      - Digital zoom
      - Tiled image assembly
   
   Best Practices:
   
   1. **Choose Appropriate Interpolation**:
      - Nearest: Binary images, masks, labels, speed critical
      - Bilinear: Natural images, general purpose
      - Bicubic: High-quality photographic images
   
   2. **Handle Aspect Ratio**:
      ```clojure
      ;; Preserve aspect ratio
      (let [scale (min (/ target-w width) (/ target-h height))
            new-w (* width scale)
            new-h (* height scale)]
        (af-resize img new-w new-h method))
      ```
   
   3. **Anti-Aliasing for Downsampling**:
      ```clojure
      ;; For significant downsampling (>2×)
      (when (< scale 0.5)
        (let [sigma (* 0.5 (/ 1.0 scale))
              blurred (gaussian-filter img sigma)]
          (af-resize blurred new-w new-h method)))
      ```
   
   4. **Validate Dimensions**:
      ```clojure
      (assert (> odim0 0) \"Output width must be positive\")
      (assert (> odim1 0) \"Output height must be positive\")
      ```
   
   5. **Batch Processing**:
      - Resize operates on higher dimensions automatically
      - Input [w, h, channels, batch] → Output [w', h', channels, batch]
      - All images in batch resized identically
   
   6. **Memory Management**:
      - Resizing creates new array
      - Remember to release input if no longer needed
      - Use resource management for multiple operations
   
   7. **Consider Iterative Scaling**:
      - For large scale factors (>4×), consider multiple steps
      - Reduces interpolation artifacts
      - Each step: 2× scale with bilinear interpolation
   
   Common Pitfalls:
   
   1. **Forgetting Positive Dimensions**:
      ```clojure
      ;; Error: Zero or negative dimension
      (af-resize img 0 256 method) ; Wrong!
      ;; Correct:
      (af-resize img 256 256 method)
      ```
   
   2. **Wrong Method for Data Type**:
      ```clojure
      ;; Problematic: Bilinear on label map
      (af-resize label-map w h AF_INTERP_BILINEAR) ; Creates invalid labels!
      ;; Correct:
      (af-resize label-map w h AF_INTERP_NEAREST) ; Preserves labels
      ```
   
   3. **Ignoring Aliasing**:
      ```clojure
      ;; Bad: Severe downsampling without filtering
      (af-resize [2048,2048] 128 128 method) ; 16× downsample, aliasing!
      ;; Better:
      (-> img
          (gaussian-blur sigma)
          (af-resize 128 128 method))
      ```
   
   4. **Aspect Ratio Distortion**:
      ```clojure
      ;; Distorts: 4:3 → 16:9
      (af-resize [1024,768] 1920 1080 method) ; Stretched!
      ;; Better: Crop or pad to match aspect
      ```
   
   5. **Type Mismatch Expectations**:
      ```clojure
      ;; Integer types may lose precision with interpolation
      (let [uint8-img (create-array data [256 256] :u8)]
        ;; Bilinear creates fractional values, truncated for u8
        (af-resize uint8-img 512 512 AF_INTERP_BILINEAR))
      ;; Consider converting to float first for better quality
      ```
   
   6. **Forgetting Batch Dimension**:
      ```clojure
      ;; Input: [w, h, c, batch=10]
      ;; Resizes all 10 images, not just first one
      (af-resize batch-imgs new-w new-h method)
      ```
   
   7. **Not Checking Method Support**:
      ```clojure
      ;; AF_INTERP_BICUBIC not directly supported by af_resize
      ;; Falls back to af_scale (different function)
      (af-resize img w h AF_INTERP_BICUBIC) ; Uses af_scale internally
      ```
   
   8. **Memory Leaks**:
      ```clojure
      ;; Multiple resizes without cleanup
      (let [r1 (af-resize img 512 512 method)
            r2 (af-resize r1 256 256 method)
            r3 (af-resize r2 128 128 method)]
        ;; Forgot to release r1, r2!
        r3)
      ```
      Solution: Use resource management (tech.resource)
   
   Relationship to Other Operations:
   
   - **af_scale**: Resize with scaling factors (af_resize uses target dimensions)
   - **af_transform**: Arbitrary affine transformation (includes resize)
   - **af_rotate**: Rotation (may change dimensions if crop=false)
   - **af_approx1/2**: General interpolation at arbitrary positions
   - **Image pyramids**: Multiple scaled versions for multiscale processing
   
   Example Workflow (Thumbnail Generation):
   
   ```clojure
   (defn create-thumbnail [img max-size]
     (let [[h w] (get-dims img)
           scale (min (/ max-size w) (/ max-size h))
           new-w (long (* w scale))
           new-h (long (* h scale))]
       ;; Anti-aliasing for significant downsampling
       (if (< scale 0.5)
         (let [sigma (* 0.5 (/ 1.0 scale))
               blurred (gaussian img sigma)]
           (af-resize blurred new-w new-h AF_INTERP_BILINEAR))
         (af-resize img new-w new-h AF_INTERP_BILINEAR))))
   ```
   
   Fallback Behavior:
   
   ArrayFire resize supports only NEAREST, BILINEAR, and LOWER methods directly.
   For other interpolation methods (BICUBIC, BICUBIC_SPLINE, BILINEAR_COSINE),
   it automatically falls back to af_scale, which supports additional methods
   but uses a transform-based approach.
   
   See also:
   - af_scale: Resize with scaling factors
   - af_transform: General affine transformation
   - Image pyramid functions for multiscale processing"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; Image resizing function

;; af_err af_resize(af_array *out, const af_array in, const dim_t odim0, const dim_t odim1, const af_interp_type method)
(defcfn af-resize
  "Resize an image to specified output dimensions using interpolation.
   
   Changes the spatial resolution of an image by resampling pixels at new
   positions using the specified interpolation method. Resizing is performed
   on the first two dimensions; higher dimensions (channels, batches) are
   preserved.
   
   Parameters:
   - out: out pointer for resized image
   - in: input image array
   - odim0: output size for first dimension (width)
   - odim1: output size for second dimension (height)
   - method: interpolation method (af_interp_type enum)
   
   Supported Interpolation Methods:
   - AF_INTERP_NEAREST (0): Nearest neighbor (fastest, blocky)
   - AF_INTERP_BILINEAR (1): Bilinear (good quality/speed balance)
   - AF_INTERP_LOWER (2): Lower interpolation (floor of coordinates)
   - AF_INTERP_BILINEAR_COSINE (3): Falls back to af_scale
   - AF_INTERP_BICUBIC (4): Falls back to af_scale
   - AF_INTERP_BICUBIC_SPLINE (5): Falls back to af_scale
   
   Dimension Behavior:
   - Input: [w_in, h_in, channels, batch]
   - Output: [odim0, odim1, channels, batch]
   - Only first two dimensions resized
   - Channels and batch dimensions preserved
   
   Interpolation Quality vs Speed:
   - **Nearest**: Fastest, preserves exact values, blocky appearance
   - **Lower**: Fast, deterministic sampling (always floor)
   - **Bilinear**: Best general purpose, smooth gradients
   - **Bicubic** (via scale): Highest quality, slowest, smoother upsampling
   
   Type Support:
   All types: f32, f64, c32, c64, s32, u32, s64, u64, s16, u16, s8, u8, b8
   
   Example 1 - Simple resize to target dimensions:
   ```clojure
   (let [img (create-array img-data [1024 768])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         
         ;; Resize to 512×512
         _ (af-resize out-ptr img 512 512 AF_INTERP_BILINEAR)
         resized (mem/read-pointer out-ptr ::mem/pointer)]
     resized)
   ```
   
   Example 2 - Scale by factor (2× upsampling):
   ```clojure
   (let [img (create-array img-data [256 256])
         [h w] [256 256]
         scale 2.0
         
         new-w (long (* w scale))  ; 512
         new-h (long (* h scale))  ; 512
         
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-resize out-ptr img new-w new-h AF_INTERP_BILINEAR)
         scaled (mem/read-pointer out-ptr ::mem/pointer)]
     scaled) ;; [512 512]
   ```
   
   Example 3 - Thumbnail (preserve aspect ratio):
   ```clojure
   (let [img (create-array img-data [1920 1080])
         [h w] [1080 1920]
         max-size 256
         
         ;; Compute scale preserving aspect ratio
         scale (min (/ max-size w) (/ max-size h))
         new-w (long (* w scale))  ; 256
         new-h (long (* h scale))  ; 144
         
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-resize out-ptr img new-w new-h AF_INTERP_BILINEAR)
         thumb (mem/read-pointer out-ptr ::mem/pointer)]
     thumb) ;; [256 144] - aspect ratio preserved
   ```
   
   Example 4 - Batch resize (all images):
   ```clojure
   (let [batch (create-array batch-data [512 512 3 10]) ; 10 RGB images
         out-ptr (mem/alloc-pointer ::mem/pointer)
         
         ;; Resize all 10 images to 256×256
         _ (af-resize out-ptr batch 256 256 AF_INTERP_BILINEAR)
         resized-batch (mem/read-pointer out-ptr ::mem/pointer)]
     resized-batch) ;; [256 256 3 10] - all 10 images resized
   ```
   
   Example 5 - Anti-aliasing for downsampling:
   ```clojure
   (let [img (create-array img-data [2048 2048])
         target-size 256
         scale (/ target-size 2048.0)  ; 0.125 (8× downsample)
         
         ;; Apply Gaussian blur before downsampling
         sigma (if (< scale 0.5)
                 (* 0.5 (/ 1.0 scale))  ; σ = 4.0
                 0.0)
         
         blurred (if (> sigma 0)
                   (gaussian-filter img sigma)
                   img)
         
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-resize out-ptr blurred target-size target-size 
                     AF_INTERP_BILINEAR)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result) ;; [256 256] without aliasing artifacts
   ```
   
   Example 6 - Neural network input preparation:
   ```clojure
   (let [images (load-images img-paths)  ; Variable sizes
         net-input-size 224  ; ResNet/VGG input size
         
         ;; Resize all to network input dimensions
         resized (map (fn [img]
                       (let [out-ptr (mem/alloc-pointer ::mem/pointer)
                             _ (af-resize out-ptr img 
                                         net-input-size net-input-size
                                         AF_INTERP_BILINEAR)]
                         (mem/read-pointer out-ptr ::mem/pointer)))
                     images)]
     resized) ;; All images now [224 224 3]
   ```
   
   Example 7 - Nearest neighbor for label maps:
   ```clojure
   (let [labels (create-array label-data [512 512])  ; Integer labels
         out-ptr (mem/alloc-pointer ::mem/pointer)
         
         ;; Use nearest to preserve exact label values
         _ (af-resize out-ptr labels 256 256 AF_INTERP_NEAREST)
         resized-labels (mem/read-pointer out-ptr ::mem/pointer)]
     resized-labels) ;; [256 256] with preserved integer labels
   ```
   
   Example 8 - Iterative upsampling for quality:
   ```clojure
   (defn iterative-resize [img target-w target-h]
     (let [[h w] (get-dims img)
           scale-x (/ target-w w)
           scale-y (/ target-h h)]
       ;; For large scale factors (>4×), use multiple 2× steps
       (if (> (max scale-x scale-y) 4.0)
         (loop [current img
                curr-w w
                curr-h h]
           (if (and (>= curr-w target-w) (>= curr-h target-h))
             ;; Final adjustment to exact target
             (let [out-ptr (mem/alloc-pointer ::mem/pointer)
                   _ (af-resize out-ptr current target-w target-h 
                               AF_INTERP_BILINEAR)]
               (mem/read-pointer out-ptr ::mem/pointer))
             ;; Next 2× step
             (let [next-w (min (* curr-w 2) target-w)
                   next-h (min (* curr-h 2) target-h)
                   out-ptr (mem/alloc-pointer ::mem/pointer)
                   _ (af-resize out-ptr current next-w next-h 
                               AF_INTERP_BILINEAR)
                   scaled (mem/read-pointer out-ptr ::mem/pointer)]
               (recur scaled next-w next-h))))
         ;; Single-step resize for small factors
         (let [out-ptr (mem/alloc-pointer ::mem/pointer)
               _ (af-resize out-ptr img target-w target-h 
                           AF_INTERP_BILINEAR)]
           (mem/read-pointer out-ptr ::mem/pointer)))))
   
   ;; Usage: 64×64 → 1024×1024 (16× upsampling)
   (iterative-resize small-img 1024 1024)
   ```
   
   Performance:
   - Time: O(odim0 × odim1) per image
   - Space: O(odim0 × odim1 × channels × batch)
   - GPU: Highly parallel (16×16 thread blocks)
   - Nearest: ~5000 fps for 512×512
   - Bilinear: ~3000 fps for 512×512
   - Memory: Creates new output array
   
   Notes:
   - Output dimensions must be positive (> 0)
   - Input array unchanged (creates new output)
   - For methods other than NEAREST/BILINEAR/LOWER, falls back to af_scale
   - Batch processing: All images in batch resized identically
   - Complex numbers: Real and imaginary parts interpolated independently
   - For aspect-preserving resize, compute dimensions before calling
   
   Returns:
   ArrayFire error code (af_err enum):
   - AF_SUCCESS: Resize successful
   - AF_ERR_SIZE: Invalid output dimensions (odim0 ≤ 0 or odim1 ≤ 0)
   - AF_ERR_ARG: Invalid interpolation method
   - AF_ERR_TYPE: Unsupported input type
   - AF_ERR_NO_MEM: Insufficient memory
   
   See also:
   - af-scale: Resize with scaling factors (alternative interface)
   - af-transform: General affine transformation
   - af-rotate: Image rotation (may change dimensions)
   - af-approx1/2: Arbitrary position interpolation"
  "af_resize" [::mem/pointer ::mem/pointer ::mem/long ::mem/long ::mem/int] ::mem/int)
