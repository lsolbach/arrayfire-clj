(ns org.soulspace.arrayfire.ffi.c-api.rotate
  "Bindings for the ArrayFire image rotation function.
   
   Image rotation is a fundamental geometric transformation in image processing
   that rotates an image around its center by a specified angle.
   
   Mathematical Foundation:
   
   **Rotation Matrix** (2D):
   For rotation by angle θ (counter-clockwise):
   
   R(θ) = [cos(θ)  -sin(θ)]
          [sin(θ)   cos(θ)]
   
   **Coordinate Transformation**:
   For a point (x, y) rotated by angle θ:
   x' = x*cos(θ) - y*sin(θ)
   y' = x*sin(θ) + y*cos(θ)
   
   **Output Dimensions**:
   When crop=false, output dimensions are calculated to fit entire rotated image:
   - width'  = width*|cos(θ)| + height*|sin(θ)|
   - height' = height*|cos(θ)| + width*|sin(θ)|
   
   When crop=true, output dimensions match input (rotated content may be clipped).
   
   Interpolation Methods:
   
   Rotation requires interpolation since rotated pixel positions rarely align
   with the original grid. Available methods (af_interp_type):
   
   - **AF_INTERP_NEAREST (0)**: Nearest neighbor
     * Fastest method
     * No smoothing, preserves sharp edges
     * Can introduce aliasing/jaggedness
     * Best for: Binary images, categorical data
   
   - **AF_INTERP_BILINEAR (2)**: Bilinear interpolation
     * Good balance of speed and quality
     * Smooth results, minimal artifacts
     * Weighted average of 4 nearest pixels
     * Best for: General purpose, photographs
   
   - **AF_INTERP_LOWER (4)**: Floor indexed
     * Similar to nearest but uses floor instead of round
     * Specific use cases in signal processing
   
   - **AF_INTERP_BILINEAR_COSINE (5)**: Bilinear with cosine smoothing
     * Smoother than standard bilinear
     * Reduces blocking artifacts
     * Slightly slower than bilinear
   
   - **AF_INTERP_BICUBIC (6)**: Bicubic interpolation
     * High quality, smooth results
     * Uses 16 nearest pixels (4×4 kernel)
     * Slower than bilinear
     * Best for: High-quality image processing
   
   - **AF_INTERP_BICUBIC_SPLINE (7)**: Bicubic with Catmull-Rom splines
     * Highest quality interpolation
     * Excellent edge preservation
     * Slowest method
     * Best for: Professional image editing, medical imaging
   
   Performance Considerations:
   
   - GPU acceleration provides 10-100× speedup over CPU
   - Complexity: O(W*H) for nearest neighbor
   - Complexity: O(W*H * kernel_size²) for bicubic methods
   - Crop=true is slightly faster (smaller output)
   - Small angles (< 10°) are faster than large rotations
   - Multiple 90° rotations can use specialized fast paths
   
   Type Support:
   - All numeric types: f32, f64, s32, u32, s64, u64, s16, u16, s8, u8
   - Complex types: c32, c64 (rotates real and imaginary parts)
   - Boolean: b8
   
   Applications:
   - Image alignment and registration
   - Augmentation for machine learning training
   - Document scanning (deskewing)
   - Computer vision (orientation normalization)
   - Astronomy (telescope image alignment)
   - Medical imaging (patient positioning correction)
   - Robotics (coordinate frame transformations)
   
   Common Use Cases:
   
   1. **Document Deskewing**:
      Correct slight angular misalignment in scanned documents
      ```clojure
      ;; Rotate by small angle to straighten text
      (af-rotate out-ptr doc-img (* 2.5 (/ Math/PI 180.0)) true AF_INTERP_BILINEAR)
      ```
   
   2. **Data Augmentation**:
      Generate rotated variations for ML training
      ```clojure
      ;; Random rotations for training set
      (doseq [angle (range 0 360 15)]
        (af-rotate out-ptr img (* angle (/ Math/PI 180.0)) true AF_INTERP_NEAREST))
      ```
   
   3. **Image Alignment**:
      Align images from different sources or times
      ```clojure
      ;; Align to reference orientation
      (af-rotate out-ptr img correction-angle false AF_INTERP_BICUBIC)
      ```
   
   Performance Tips:
   - Use AF_INTERP_NEAREST for real-time applications
   - Use AF_INTERP_BILINEAR for good speed/quality balance
   - Use AF_INTERP_BICUBIC_SPLINE for highest quality (batch processing)
   - Set crop=true when preserving dimensions is important
   - Consider caching commonly used rotation angles
   - For 90°, 180°, 270° rotations, use flip/transpose instead (much faster)
   
   Notes:
   - Rotation angle θ is in **radians** (not degrees)
   - To convert degrees to radians: radians = degrees * π/180
   - Positive angles rotate counter-clockwise
   - Negative angles rotate clockwise
   - Center of rotation is the image center
   - Boundary regions use interpolation method's edge handling
   
   See also:
   - af-transform: General affine transformation (includes rotation)
   - af-translate: Translation without rotation
   - af-scale: Scaling without rotation
   - af-skew: Skew transformation
   - Flip functions for 90° rotations (faster)"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.loader]))

;; af_err af_rotate(af_array *out, const af_array in, const float theta, const bool crop, const af_interp_type method)
(defcfn af-rotate
  "Rotate an image by a specified angle.
   
   Rotates the input image around its center by the specified angle in radians.
   Supports multiple interpolation methods for handling sub-pixel positions.
   
   Parameters:
   - out: Output pointer for the rotated image
   - in: Input image array (2D or higher dimensional)
   - theta: Rotation angle in **radians** (counter-clockwise)
     * Positive: Counter-clockwise rotation
     * Negative: Clockwise rotation
     * Convert degrees to radians: theta = degrees * π/180
   - crop: Controls output dimensions
     * true: Output has same dimensions as input (may clip rotated content)
     * false: Output dimensions expand to fit entire rotated image
   - method: Interpolation method (af_interp_type enum)
     * 0 (AF_INTERP_NEAREST): Nearest neighbor - fastest, can be jagged
     * 2 (AF_INTERP_BILINEAR): Bilinear - good balance of speed/quality
     * 4 (AF_INTERP_LOWER): Floor indexed
     * 5 (AF_INTERP_BILINEAR_COSINE): Bilinear with cosine smoothing
     * 6 (AF_INTERP_BICUBIC): Bicubic - high quality, slower
     * 7 (AF_INTERP_BICUBIC_SPLINE): Catmull-Rom splines - highest quality
   
   Rotation Matrix:
   Applied transformation for angle θ:
   [cos(θ)  -sin(θ)]   [x]   [x']
   [sin(θ)   cos(θ)] × [y] = [y']
   
   Output Dimensions:
   When crop=false:
   - width'  = width*|cos(θ)| + height*|sin(θ)|
   - height' = height*|cos(θ)| + width*|sin(θ)|
   
   When crop=true:
   - width'  = width
   - height' = height
   
   Type Support:
   - Real types: f32, f64, s32, u32, s64, u64, s16, u16, s8, u8, b8
   - Complex types: c32, c64 (rotates both real and imaginary components)
   
   Interpolation Quality vs Speed:
   - NEAREST: Fastest, preserves exact pixel values, aliasing artifacts
   - BILINEAR: Fast, smooth, good for most uses
   - BICUBIC: Slower, very smooth, professional quality
   - BICUBIC_SPLINE: Slowest, highest quality, best edge preservation
   
   Example (rotate by 45 degrees counter-clockwise):
   ```clojure
   (let [img (create-array img-data [512 512])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         theta (* 45.0 (/ Math/PI 180.0))  ; Convert degrees to radians
         _ (af-rotate out-ptr img theta false 2)]  ; false=expand, 2=bilinear
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (rotate 30 degrees with cropping):
   ```clojure
   (let [img (create-array img-data [512 512])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         theta (* 30.0 (/ Math/PI 180.0))
         _ (af-rotate out-ptr img theta true 6)]  ; true=crop, 6=bicubic
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (data augmentation - multiple rotations):
   ```clojure
   ;; Generate 24 rotated versions (every 15 degrees)
   (for [deg (range 0 360 15)]
     (let [out-ptr (mem/alloc-pointer ::mem/pointer)
           theta (* deg (/ Math/PI 180.0))
           _ (af-rotate out-ptr img theta true 0)]  ; crop=true, nearest neighbor
       (mem/read-pointer out-ptr ::mem/pointer)))
   ```
   
   Example (document deskewing):
   ```clojure
   ;; Correct 2.3 degree tilt in scanned document
   (let [doc-scan (create-array scan-data [2000 1500])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         correction-angle (* -2.3 (/ Math/PI 180.0))  ; Negative = clockwise
         _ (af-rotate out-ptr doc-scan correction-angle true 2)]
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Common Angles:
   - 0°   = 0 radians: No rotation
   - 45°  = π/4 ≈ 0.785 radians
   - 90°  = π/2 ≈ 1.571 radians (consider using flip+transpose instead)
   - 180° = π ≈ 3.142 radians (consider using flip instead)
   - 270° = 3π/2 ≈ 4.712 radians
   - 360° = 2π ≈ 6.283 radians: Full rotation (back to original)
   
   Performance Notes:
   - GPU provides massive parallelization (each output pixel computed independently)
   - Small rotations (< 15°) process faster than large rotations
   - Nearest neighbor is ~3-5× faster than bicubic
   - crop=true is slightly faster (smaller output array)
   - For exact 90° multiples, use flip/transpose (10-100× faster)
   
   Edge Handling:
   - Pixels that rotate outside original bounds are handled by interpolation
   - When crop=false, expanded region is filled with interpolated values
   - When crop=true, rotated pixels outside bounds are clipped
   
   Multi-dimensional Arrays:
   - For 3D+ arrays (e.g., color images or image stacks):
     * Rotation applied to first two dimensions
     * Third dimension typically represents color channels (RGB, etc.)
     * Fourth dimension represents batches
     * All slices rotated with same parameters
   
   Color Images:
   - Each channel is rotated independently
   - RGB image: Rotates R, G, and B channels by same angle
   - Results in properly rotated color image
   - Use same interpolation method for all channels
   
   Precision:
   - Rotation uses floating-point arithmetic internally
   - Integer types are converted, rotated, then converted back
   - May introduce rounding for integer types
   - Use float/double types for maximum precision
   
   Returns:
   ArrayFire error code (af_err enum)
   - AF_SUCCESS (0): Rotation successful
   - AF_ERR_ARG: Invalid arguments
   - AF_ERR_SIZE: Invalid dimensions
   - AF_ERR_TYPE: Unsupported data type
   
   See also:
   - af-transform: General affine transformation (rotation, scale, translation)
   - af-translate: Shift image without rotation
   - af-scale: Resize image
   - af-skew: Skew transformation"
  "af_rotate" [::mem/pointer ::mem/pointer ::mem/float ::mem/int ::mem/int] ::mem/int)
