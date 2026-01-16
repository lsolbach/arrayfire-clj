(ns org.soulspace.arrayfire.ffi.transform
  "Bindings for the ArrayFire image transformation functions.
   
   Image transformations are fundamental operations in computer vision and
   image processing that modify the spatial configuration of images through
   geometric transformations such as rotation, translation, scaling, and skewing.
   
   Transformation Categories:
   
   1. **General Transformation** (af-transform):
      - Applies arbitrary affine or perspective transformations
      - Uses 3×2 (affine) or 3×3 (perspective) transformation matrices
      - Most flexible, supports all geometric transformations
   
   2. **Translation** (af-translate):
      - Shifts image by specified offset in x and y directions
      - Simple displacement without rotation or scaling
      - Preserves image orientation and size
   
   3. **Scaling** (af-scale):
      - Resizes image by scale factors
      - Can be uniform (same scale both dimensions) or non-uniform
      - Supports upsampling and downsampling
   
   4. **Skewing** (af-skew):
      - Shear transformation along x and y axes
      - Creates parallelogram effect
      - Supports forward and inverse skewing
   
   Mathematical Foundations:
   
   **Homogeneous Coordinates**:
   2D transformations use homogeneous coordinates where point (x, y)
   becomes [x, y, 1]ᵀ. This enables representation of all affine
   transformations as matrix multiplications.
   
   **Affine Transformation (3×2 matrix)**:
   ```
       ┌         ┐
       │ a  b  tx│  where:
   T = │ c  d  ty│  - [a,b; c,d] is 2×2 linear transformation
       └         ┘  - [tx, ty] is translation vector
   ```
   
   Transformation equation:
   ```
   ┌    ┐     ┌         ┐ ┌   ┐
   │ x' │     │ a  b  tx│ │ x │
   │ y' │  =  │ c  d  ty│ │ y │
   └    ┘     └         ┘ │ 1 │
                          └   ┘
   ```
   
   **Perspective Transformation (3×3 matrix)**:
   ```
       ┌           ┐
       │ a  b  tx  │  where:
   T = │ c  d  ty  │  - [a,b; c,d] is 2×2 linear part
       │ p  q  s   │  - [tx, ty] is translation
       └           ┘  - [p, q, s] enables perspective effects
   ```
   
   With perspective division: x' = x'/w, y' = y'/w where w = px + qy + s
   
   **Specific Transformation Matrices**:
   
   1. Translation by (tx, ty):
   ```
   ┌          ┐
   │ 1  0  tx │
   │ 0  1  ty │
   └          ┘
   ```
   
   2. Scaling by (sx, sy):
   ```
   ┌          ┐
   │ sx 0  0  │
   │ 0  sy 0  │
   └          ┘
   ```
   
   3. Rotation by θ:
   ```
   ┌                    ┐
   │ cos(θ) -sin(θ)  0  │
   │ sin(θ)  cos(θ)  0  │
   └                    ┘
   ```
   
   4. Shear (skew) by (kx, ky):
   ```
   ┌          ┐
   │ 1  kx  0 │
   │ ky  1  0 │
   └          ┘
   ```
   where kx = tan(skew_x), ky = tan(skew_y)
   
   **Forward vs Inverse Transformation**:
   
   - Forward (inverse=false): Maps source coordinates to destination
     * Output pixel (x', y') samples from input at transformed location
     * Can create holes in output if transformation expands image
   
   - Inverse (inverse=true): Maps destination coordinates to source
     * For each output pixel, compute where to sample in input
     * More common, avoids holes, default for most operations
     * Output[x', y'] = Input[T⁻¹(x', y')]
   
   Interpolation Methods:
   
   ArrayFire supports multiple interpolation methods for resampling:
   
   1. **AF_INTERP_NEAREST (0)**: Nearest neighbor
      - Fastest but lowest quality
      - Creates blocky artifacts
      - Suitable for: Binary images, label maps, categorical data
   
   2. **AF_INTERP_LINEAR (1)** or **AF_INTERP_BILINEAR**: Linear interpolation
      - Good balance of speed and quality
      - Smooth results, slightly blurred
      - Suitable for: Most photographic images
   
   3. **AF_INTERP_BILINEAR_COSINE (2)**: Bilinear with cosine weighting
      - Similar to bilinear but smoother transitions
      - Slightly slower than standard bilinear
   
   4. **AF_INTERP_BICUBIC (3)**: Bicubic interpolation
      - Higher quality, sharper than bilinear
      - Slower computation
      - Can create slight ringing artifacts
      - Suitable for: High-quality image processing
   
   5. **AF_INTERP_BICUBIC_SPLINE (4)**: Bicubic spline interpolation
      - Smoothest interpolation
      - Best quality but slowest
      - Suitable for: Professional image editing, medical imaging
   
   6. **AF_INTERP_LOWER (5)**: Lower interpolation
      - Specialized method
      - Implementation-specific behavior
   
   Performance Characteristics:
   
   GPU Acceleration:
   - 10-100× speedup vs CPU implementations
   - Parallel processing of all output pixels
   - Texture memory optimization for interpolation
   - Efficient memory coalescing
   
   Complexity:
   - Time: O(W*H) for output dimensions W×H
   - Space: O(W*H) for output array
   - Interpolation adds constant factor per pixel
   
   Interpolation Performance Ranking (fastest to slowest):
   1. Nearest neighbor (1.0×)
   2. Bilinear (1.5×)
   3. Bilinear cosine (1.7×)
   4. Bicubic (2.5×)
   5. Bicubic spline (3.0×)
   
   Batching Support:
   
   ArrayFire supports batched transformations where:
   - Single image + multiple transforms → multiple outputs
   - Multiple images + single transform → multiple outputs
   - Multiple images + multiple transforms → matched pairs
   
   Batch modes:
   - AF_BATCH_NONE: Single image, single transform
   - AF_BATCH_LHS: Multiple images (dim 2/3), single transform
   - AF_BATCH_RHS: Single image, multiple transforms (dim 2/3)
   - AF_BATCH_SAME: Multiple images and transforms with same dimensions
   - AF_BATCH_DIFF: Images and transforms can broadcast
   
   Use Cases:
   
   **Computer Vision**:
   - Image registration and alignment
   - Geometric calibration
   - Augmented reality (AR)
   - Panorama stitching
   - 3D reconstruction
   
   **Image Processing**:
   - Rotation correction
   - Perspective correction
   - Lens distortion correction
   - Image resizing and cropping
   
   **Document Processing**:
   - Document deskewing
   - OCR preprocessing
   - Page alignment
   
   **Medical Imaging**:
   - Multi-modal registration
   - Atlas alignment
   - Motion correction
   
   **Machine Learning**:
   - Data augmentation for training
   - Test-time augmentation
   - Spatial transformer networks
   
   **Robotics**:
   - Visual odometry
   - SLAM (Simultaneous Localization and Mapping)
   - Camera calibration
   
   Common Patterns:
   
   **Pattern 1: Image Registration**
   ```clojure
   ;; Align two images using estimated homography
   (defn register-images [src-img dst-img src-pts dst-pts]
     (let [H (estimate-homography src-pts dst-pts)
           [h w] (get-dimensions dst-img)
           out-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-transform out-ptr src-img H h w 
                          AF_INTERP_BILINEAR true)]
       (mem/read-pointer out-ptr ::mem/pointer)))
   ```
   
   **Pattern 2: Data Augmentation**
   ```clojure
   ;; Random rotation and translation for training data
   (defn augment-image [img]
     (let [angle (- (rand (* 2 Math/PI)) Math/PI)  ; ±180°
           tx (- (rand 20) 10)  ; ±10 pixels
           ty (- (rand 20) 10)
           ;; Compose rotation and translation
           rot-matrix (create-rotation-matrix angle)
           trans-matrix (create-translation-matrix tx ty)
           combined (af-matmul trans-matrix rot-matrix)
           [h w] (get-dimensions img)
           out-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-transform out-ptr img combined h w
                          AF_INTERP_BILINEAR true)]
       (mem/read-pointer out-ptr ::mem/pointer)))
   ```
   
   **Pattern 3: Batch Transformation**
   ```clojure
   ;; Apply same transform to batch of images
   (defn batch-transform [images transform-matrix]
     (let [[h w c n] (get-dimensions images)  ; n images in batch
           out-ptr (mem/alloc-pointer ::mem/pointer)
           ;; Single transform applies to all images in batch
           _ (af-transform out-ptr images transform-matrix h w
                          AF_INTERP_BILINEAR true)]
       (mem/read-pointer out-ptr ::mem/pointer)))
   ```
   
   **Pattern 4: Perspective Correction**
   ```clojure
   ;; Correct perspective distortion (document scanning)
   (defn correct-perspective [img corner-points]
     (let [;; Define target rectangle (desired output)
           [h w] [800 600]  ; target dimensions
           target-corners [[0 0] [w 0] [w h] [0 h]]
           ;; Estimate homography from corners to target
           H (estimate-homography corner-points target-corners)
           out-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-transform out-ptr img H h w
                          AF_INTERP_BICUBIC true)]
       (mem/read-pointer out-ptr ::mem/pointer)))
   ```
   
   **Pattern 5: Multi-Scale Processing**
   ```clojure
   ;; Create image pyramid with scale factors
   (defn create-pyramid [img scales]
     (for [scale scales]
       (let [[h w] (get-dimensions img)
             new-h (long (* h scale))
             new-w (long (* w scale))
             out-ptr (mem/alloc-pointer ::mem/pointer)
             _ (af-scale out-ptr img scale scale new-h new-w
                        AF_INTERP_BILINEAR)]
         (mem/read-pointer out-ptr ::mem/pointer))))
   ```
   
   **Pattern 6: Rotation About Center**
   ```clojure
   ;; Rotate image about its center point
   (defn rotate-centered [img angle]
     (let [[h w] (get-dimensions img)
           cx (/ w 2.0)
           cy (/ h 2.0)
           ;; Compose: translate to origin, rotate, translate back
           T1 (create-translation-matrix (- cx) (- cy))
           R  (create-rotation-matrix angle)
           T2 (create-translation-matrix cx cy)
           transform (af-matmul (af-matmul T2 R) T1)
           ;; Determine output size for full rotated image
           corners-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-transform-coordinates corners-ptr transform h w)
           corners (mem/read-pointer corners-ptr ::mem/pointer)
           [out-h out-w] (compute-bounding-box corners)
           out-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-transform out-ptr img transform out-h out-w
                          AF_INTERP_BILINEAR true)]
       (mem/read-pointer out-ptr ::mem/pointer)))
   ```
   
   Type Support:
   
   All transformation functions support:
   - Floating-point: f32, f64
   - Complex: c32, c64
   - Signed integers: s8, s16, s32, s64
   - Unsigned integers: u8, u16, u32, u64
   - Boolean: b8
   
   Note: Transform matrix must always be f32 regardless of image type.
   
   Output Dimensions:
   
   Output dimensions can be specified in two ways:
   
   1. **Explicit dimensions** (odim0, odim1 > 0):
      - Output size is exactly as specified
      - Image may be clipped if transformation produces larger result
      - Use for fixed output size requirements
   
   2. **Automatic dimensions** (odim0 = 0, odim1 = 0):
      - Output size matches input size
      - Suitable when transformation preserves dimensions
      - Default for translation, rotation with crop
   
   3. **Computed dimensions**:
      - Use af-transform-coordinates to compute required output size
      - Ensures full transformed image is captured
      - Prevents clipping of rotated or skewed images
   
   Border Handling:
   
   Pixels outside input image boundaries return zero (black padding).
   For other border handling modes, pre-process image with padding
   functions before transformation.
   
   Error Handling:
   
   Common errors:
   - **AF_ERR_SIZE**: Transform matrix wrong size or dimension mismatch
   - **AF_ERR_TYPE**: Transform matrix not f32
   - **AF_ERR_ARG**: Invalid interpolation method
   - **AF_ERR_NOT_SUPPORTED**: Unsupported batching combination
   
   Best Practices:
   
   1. **Choose appropriate interpolation**:
      - Use bilinear for most cases (good speed/quality balance)
      - Use nearest for binary/categorical data
      - Use bicubic for high-quality requirements
   
   2. **Compute output dimensions**:
      - Use af-transform-coordinates for rotations/skewing
      - Avoid clipping by computing required size
   
   3. **Use inverse transformations**:
      - Set inverse=true (default) for most operations
      - Avoids holes in output image
   
   4. **Compose transformations**:
      - Pre-multiply transformation matrices
      - Single transform more efficient than chained transforms
   
   5. **Consider specialized functions**:
      - Use af-rotate for simple rotations
      - Use af-resize for pure scaling
      - Use af-translate for shifts
      - Only use af-transform for complex cases
   
   6. **Batch processing**:
      - Process multiple images in single call when possible
      - Leverage GPU parallelism across batch dimension
   
   7. **Memory management**:
      - Release intermediate transformation matrices
      - Consider memory usage for large batches
   
   Performance Optimization:
   
   - Cache transformation matrices when reusing
   - Use lower precision (f32) unless f64 required
   - Profile interpolation methods for speed/quality tradeoff
   - Batch process multiple images for better GPU utilization
   - Pre-compute composed transformations
   
   Limitations:
   
   - Transform matrix must be f32 (not f64 or other types)
   - 3D transformations not directly supported
   - Limited border handling options (zero padding only)
   - Perspective transformation requires 3×3 matrix
   - Maximum output dimensions limited by GPU memory
   
   See also:
   - af-transform-coordinates: Compute transformed corner positions
   - af-rotate: Specialized rotation function
   - af-resize: Simple resizing without transformation matrix
   - af-warp-affine: Alternative affine transformation (if available)
   - af-warp-perspective: Alternative perspective transformation (if available)"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; General transformation

;; af_err af_transform(af_array *out, const af_array in, const af_array transform, const dim_t odim0, const dim_t odim1, const af_interp_type method, const bool inverse)
(defcfn af-transform
  "Apply general affine or perspective transformation to an image.
   
   Performs geometric transformation using a transformation matrix, supporting
   both affine (3×2 matrix) and perspective (3×3 matrix) transformations.
   This is the most general transformation function in ArrayFire.
   
   Parameters:
   - out: Output pointer for transformed image
   - in: Input image array (2D or higher)
   - transform: Transformation matrix (must be f32)
     * 3×2 for affine transformation
     * 3×3 for perspective transformation
   - odim0: Output height (first dimension)
     * If 0, uses input height
   - odim1: Output width (second dimension)
     * If 0, uses input width
   - method: Interpolation method (af_interp_type enum)
     * 0 (AF_INTERP_NEAREST): Nearest neighbor
     * 1 (AF_INTERP_LINEAR/BILINEAR): Linear interpolation
     * 2 (AF_INTERP_BILINEAR_COSINE): Bilinear with cosine
     * 3 (AF_INTERP_BICUBIC): Bicubic interpolation
     * 4 (AF_INTERP_BICUBIC_SPLINE): Bicubic spline
     * 5 (AF_INTERP_LOWER): Lower interpolation
   - inverse: Transformation direction (bool, use 1 or 0)
     * true (1): Inverse transform (default, recommended)
     * false (0): Forward transform
   
   Transformation Matrix Formats:
   
   **Affine (3×2)**:
   ```
   ┌         ┐
   │ a  b  tx│  where (x', y') = (ax + by + tx, cx + dy + ty)
   │ c  d  ty│
   └         ┘
   ```
   
   **Perspective (3×3)**:
   ```
   ┌           ┐
   │ a  b  tx  │  with perspective division:
   │ c  d  ty  │  w = px + qy + s
   │ p  q  s   │  x' = (ax + by + tx) / w
   └           ┘  y' = (cx + dy + ty) / w
   ```
   
   Inverse vs Forward:
   - **Inverse (true)**: For each output pixel, find source location
     * Avoids holes in output
     * Most common usage
     * Output[x', y'] = Input[T⁻¹(x', y')]
   
   - **Forward (false)**: Map source pixels to destination
     * May create holes if transformation expands
     * Less commonly used
     * Output[T(x, y)] = Input[x, y]
   
   Batching Support:
   Can process batches of images and/or transformations:
   - Image dims: [h, w, c, n] → n images
   - Transform dims: [3, 2/3, 1, m] → m transforms
   - Output: Matched or broadcast according to batch rules
   
   Type Support:
   - Input image: All types (f32, f64, c32, c64, integers, b8)
   - Transform matrix: Must be f32
   - Output: Same type as input
   
   Performance:
   - GPU accelerated with 10-100× speedup
   - Time complexity: O(odim0 * odim1)
   - Interpolation adds constant factor per pixel
   - Batch processing leverages parallelism
   
   Example (Image Registration):
   ```clojure
   ;; Apply homography for image alignment
   (let [src-img (load-image \"source.jpg\")
         dst-img (load-image \"target.jpg\")
         H (estimate-homography src-pts dst-pts)  ; 3×3 matrix
         [h w] (get-dimensions dst-img)
         out-ptr (mem/alloc-pointer ::mem/pointer)
         status (af-transform out-ptr src-img H h w 1 1)  ; bilinear, inverse
         aligned (mem/read-pointer out-ptr ::mem/pointer)]
     aligned)
   ```
   
   Example (Rotation with Auto-Size):
   ```clojure
   ;; Rotate image, compute required output size
   (let [img (load-image \"input.jpg\")
         angle (/ Math/PI 4)  ; 45 degrees
         rot-matrix (create-rotation-matrix-3x2 angle)
         [h w] (get-dimensions img)
         ;; Compute corners after transformation
         corners-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-transform-coordinates corners-ptr 
                                     (to-3x3 rot-matrix) h w)
         corners (mem/read-pointer corners-ptr ::mem/pointer)
         [out-h out-w min-x min-y] (compute-bounds corners)
         ;; Adjust transform to shift into positive space
         shift-matrix (create-translation-matrix-3x2 (- min-x) (- min-y))
         adjusted (af-matmul shift-matrix rot-matrix)
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-transform out-ptr img adjusted out-h out-w 1 1)]
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (Batch Transformation):
   ```clojure
   ;; Apply same transform to batch of images
   (let [images (create-array batch-data [256 256 3 10])  ; 10 images
         transform (create-affine-matrix rotation scale translation)
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-transform out-ptr images transform 256 256 1 1)
         transformed-batch (mem/read-pointer out-ptr ::mem/pointer)]
     transformed-batch)
   ```
   
   Output Dimensions:
   - If odim0=0 and odim1=0: Output size = input size
   - If specified: Output size = [odim0, odim1]
   - Use af-transform-coordinates to compute required size
   
   Border Handling:
   - Pixels outside input bounds return 0 (zero padding)
   - For other border modes, pre-pad input image
   
   Common Transformations:
   - Rotation: Use cos(θ), -sin(θ), sin(θ), cos(θ) in linear part
   - Translation: Set tx, ty to displacement values
   - Scaling: Set diagonal elements to scale factors
   - Shear: Set off-diagonal elements to shear factors
   - Composed: Pre-multiply matrices (T = T₃ × T₂ × T₁)
   
   Constraints:
   - Transform matrix must be f32 type
   - Transform must be 3×2 or 3×3
   - Input must be at least 2D
   - Interpolation method must be valid (0-5)
   - Batch dimensions must be compatible
   
   Common Errors:
   - AF_ERR_SIZE: Wrong transform matrix size
   - AF_ERR_TYPE: Transform matrix not f32
   - AF_ERR_ARG: Invalid interpolation method
   - AF_ERR_NOT_SUPPORTED: Unsupported batch combination
   
   Best Practices:
   - Use inverse=true (1) for most cases
   - Compute output dimensions with af-transform-coordinates
   - Pre-compose multiple transformations into single matrix
   - Choose interpolation based on speed/quality needs
   - Use bilinear (1) for general purpose
   - Use bicubic (3) for high quality
   - Release transformation matrices after use
   
   Notes:
   - More general than af-rotate, af-translate, af-scale
   - Supports both affine and perspective transformations
   - Zero padding for out-of-bounds pixels
   - GPU memory limits maximum output size
   - Batch processing improves throughput
   
   Returns:
   ArrayFire error code (af_err enum):
   - AF_SUCCESS (0): Transformation successful
   - AF_ERR_SIZE: Invalid matrix or dimension sizes
   - AF_ERR_TYPE: Invalid transformation matrix type
   - AF_ERR_ARG: Invalid parameter values
   - AF_ERR_NOT_SUPPORTED: Unsupported operation
   
   See also:
   - af-transform-v2: Version with preallocated output
   - af-transform-coordinates: Compute transformed corners
   - af-rotate: Specialized rotation function
   - af-translate: Translation only
   - af-scale: Scaling only
   - af-skew: Shear transformation"
  "af_transform" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/long ::mem/long ::mem/int ::mem/int] ::mem/int)

;; af_err af_transform_v2(af_array *out, const af_array in, const af_array transform, const dim_t odim0, const dim_t odim1, const af_interp_type method, const bool inverse)
(defcfn af-transform-v2
  "Apply transformation with preallocated output array.
   
   Version of af-transform that accepts a preallocated output array,
   allowing in-place updates of sub-arrays or reuse of existing arrays.
   
   Parameters:
   - out: Pointer to output array (can be null or existing af_array)
     * If null: New array allocated (same as af-transform)
     * If existing: Overwrites the array content
     * If sub-array: Only updates corresponding portion
   - in: Input image array
   - transform: Transformation matrix (f32, 3×2 or 3×3)
   - odim0: Output height
   - odim1: Output width
   - method: Interpolation method (0-5)
   - inverse: Transformation direction (1=inverse, 0=forward)
   
   Differences from af-transform:
   - Can reuse existing output arrays
   - Can update sub-arrays in-place
   - Avoids allocation overhead for repeated transforms
   - Useful for video processing or iterative algorithms
   
   Example (Video Processing):
   ```clojure
   ;; Reuse output buffer for video frames
   (let [transform (create-stabilization-matrix)
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (mem/write-pointer out-ptr (mem/nullptr) ::mem/pointer)]
     (doseq [frame video-frames]
       ;; Reuses/updates same output array
       (af-transform-v2 out-ptr frame transform 720 1280 1 1))
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (Sub-Array Update):
   ```clojure
   ;; Update portion of larger array
   (let [large-img (create-array zeros [2000 2000])
         sub-array (af-index large-img region-indices)
         small-img (load-image \"small.jpg\")
         transform (create-identity-matrix-3x2)
         ;; Updates only the sub-array portion
         _ (af-transform-v2 sub-array small-img transform 0 0 1 1)]
     large-img)  ; Contains updated region
   ```
   
   Warning:
   - Passing uninitialized af_array to out causes undefined behavior
   - Ensure proper initialization or use nullptr
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-transform: Standard version with automatic allocation"
  "af_transform_v2" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/long ::mem/long ::mem/int ::mem/int] ::mem/int)

;; af_err af_translate(af_array *out, const af_array in, const float trans0, const float trans1, const dim_t odim0, const dim_t odim1, const af_interp_type method)
(defcfn af-translate
  "Translate (shift) an image by specified offset.
   
   Shifts image by trans0 pixels in the first dimension (vertical) and
   trans1 pixels in the second dimension (horizontal). This is a
   specialized version of af-transform for pure translation.
   
   Parameters:
   - out: Output pointer for translated image
   - in: Input image array
   - trans0: Translation in first dimension (y-axis, rows)
     * Positive: shift downward
     * Negative: shift upward
   - trans1: Translation in second dimension (x-axis, columns)
     * Positive: shift rightward
     * Negative: shift leftward
   - odim0: Output height (0 = use input height)
   - odim1: Output width (0 = use input width)
   - method: Interpolation method (0-5)
   
   Translation Matrix Used:
   ```
   ┌             ┐
   │ 1  0  trans1│  (note: trans1 is x-translation)
   │ 0  1  trans0│  (note: trans0 is y-translation)
   └             ┘
   ```
   
   Behavior:
   - Applies inverse transformation by default
   - Output pixel (x', y') samples from input at (x'-trans1, y'-trans0)
   - Pixels shifted out of bounds are lost
   - New areas filled with zero (black padding)
   
   Type Support:
   - All image types supported
   - Translation values are float
   - Sub-pixel translations use interpolation
   
   Performance:
   - Faster than general af-transform
   - GPU accelerated
   - O(odim0 * odim1) complexity
   
   Example (Simple Shift):
   ```clojure
   ;; Shift image 10 pixels right, 5 pixels down
   (let [img (load-image \"input.jpg\")
         [h w] (get-dimensions img)
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-translate out-ptr img 5.0 10.0 h w 1)  ; bilinear
         shifted (mem/read-pointer out-ptr ::mem/pointer)]
     shifted)
   ```
   
   Example (Video Stabilization):
   ```clojure
   ;; Compensate for camera shake
   (defn stabilize-frame [frame motion-vector]
     (let [[dy dx] motion-vector  ; Detected motion
           [h w] (get-dimensions frame)
           out-ptr (mem/alloc-pointer ::mem/pointer)
           ;; Translate by negative motion to compensate
           _ (af-translate out-ptr frame (- dy) (- dx) h w 1)]
       (mem/read-pointer out-ptr ::mem/pointer)))
   ```
   
   Example (Image Mosaicing):
   ```clojure
   ;; Place multiple images on larger canvas
   (defn place-on-canvas [img x-offset y-offset canvas-h canvas-w]
     (let [out-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-translate out-ptr img 
                          (float y-offset) 
                          (float x-offset)
                          canvas-h canvas-w 0)]  ; nearest
       (mem/read-pointer out-ptr ::mem/pointer)))
   ```
   
   Use Cases:
   - Image registration (coarse alignment)
   - Video stabilization
   - Pan and scan effects
   - Image mosaicing/montage
   - Correcting camera motion
   - Digital image stabilization
   
   Notes:
   - Uses inverse transformation internally
   - More efficient than creating translation matrix manually
   - Fractional pixel shifts use interpolation
   - Zero padding for out-of-bounds regions
   
   Returns:
   ArrayFire error code (af_err enum):
   - AF_SUCCESS (0): Translation successful
   - AF_ERR_SIZE: Invalid dimensions
   - AF_ERR_ARG: Invalid parameters
   
   See also:
   - af-transform: General transformation with matrix
   - af-rotate: Rotation transformation
   - af-scale: Scaling transformation"
  "af_translate" [::mem/pointer ::mem/pointer ::mem/float ::mem/float ::mem/long ::mem/long ::mem/int] ::mem/int)

;; af_err af_scale(af_array *out, const af_array in, const float scale0, const float scale1, const dim_t odim0, const dim_t odim1, const af_interp_type method)
(defcfn af-scale
  "Scale (resize) an image by specified factors.
   
   Resizes image by scale factors along each dimension. This is a
   specialized version of af-transform for pure scaling operations.
   Supports both upsampling and downsampling.
   
   Parameters:
   - out: Output pointer for scaled image
   - in: Input image array
   - scale0: Scale factor for first dimension (height)
     * > 1.0: Upsampling (enlarge)
     * < 1.0: Downsampling (shrink)
     * 0: Computed from odim0
   - scale1: Scale factor for second dimension (width)
     * > 1.0: Upsampling (enlarge)
     * < 1.0: Downsampling (shrink)
     * 0: Computed from odim1
   - odim0: Output height
     * If 0: Computed as input_height / scale0
   - odim1: Output width
     * If 0: Computed as input_width / scale1
   - method: Interpolation method (0-5)
   
   Parameter Combinations:
   
   1. **Scale factors only** (odim0=0, odim1=0):
      - scale0, scale1 specify resize factors
      - Output size = input size / scale factors
   
   2. **Dimensions only** (scale0=0, scale1=0):
      - odim0, odim1 specify exact output size
      - Scale factors computed automatically
   
   3. **Both specified** (all non-zero):
      - scale0, scale1 used for transformation
      - odim0, odim1 used for output size
      - Warning: May cause aspect ratio changes
   
   Scaling Matrix Used:
   ```
   ┌           ┐
   │ sx  0   0 │  where sx = 1/scale0, sy = 1/scale1
   │  0  sy  0 │  (inverse scaling for inverse transform)
   └           ┘
   ```
   
   Type Support:
   - All image types supported
   - Scale factors are float
   - Interpolation quality affects result
   
   Interpolation Recommendations:
   - Upsampling (scale > 1): Use bilinear or bicubic
   - Downsampling (scale < 1): Use bilinear with anti-aliasing
   - Small scale factors: Consider bicubic for quality
   
   Performance:
   - GPU accelerated
   - O(odim0 * odim1) complexity
   - Faster than general af-transform
   - Bilinear: Good speed/quality balance
   
   Example (Uniform Scaling):
   ```clojure
   ;; Scale image to 2× size (double dimensions)
   (let [img (load-image \"input.jpg\")
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-scale out-ptr img 0.5 0.5 0 0 1)  ; scale=0.5 means 2× larger
         enlarged (mem/read-pointer out-ptr ::mem/pointer)]
     enlarged)
   ```
   
   Example (Exact Dimensions):
   ```clojure
   ;; Resize to exact dimensions (thumbnail)
   (let [img (load-image \"photo.jpg\")
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-scale out-ptr img 0.0 0.0 128 128 1)  ; 128×128 thumbnail
         thumbnail (mem/read-pointer out-ptr ::mem/pointer)]
     thumbnail)
   ```
   
   Example (Pyramid Construction):
   ```clojure
   ;; Create image pyramid for multi-scale processing
   (defn create-pyramid [img levels]
     (loop [level 0
            current img
            pyramid []]
       (if (>= level levels)
         pyramid
         (let [scale (Math/pow 0.5 level)  ; 1.0, 0.5, 0.25, 0.125...
               out-ptr (mem/alloc-pointer ::mem/pointer)
               _ (af-scale out-ptr current scale scale 0 0 1)
               scaled (mem/read-pointer out-ptr ::mem/pointer)]
           (recur (inc level) current (conj pyramid scaled))))))
   ```
   
   Example (Aspect Ratio Preservation):
   ```clojure
   ;; Scale to fit within maximum dimensions, preserve aspect ratio
   (defn scale-to-fit [img max-h max-w]
     (let [[h w] (get-dimensions img)
           scale-h (/ max-h h)
           scale-w (/ max-w w)
           scale (min scale-h scale-w)  ; Use smaller scale to fit
           out-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-scale out-ptr img scale scale 0 0 3)  ; bicubic
           scaled (mem/read-pointer out-ptr ::mem/pointer)]
       scaled))
   ```
   
   Use Cases:
   - Image resizing for display
   - Thumbnail generation
   - Multi-scale processing (image pyramids)
   - Data preprocessing for ML
   - Resolution adjustment
   - Image compression preprocessing
   
   Notes:
   - Uses inverse transformation internally
   - scale < 1.0 means larger output (counter-intuitive)
   - Consider anti-aliasing filter for downsampling
   - Bicubic recommended for upsampling quality
   - Zero padding if output larger than scaled result
   
   Returns:
   ArrayFire error code (af_err enum):
   - AF_SUCCESS (0): Scaling successful
   - AF_ERR_SIZE: Invalid dimensions
   - AF_ERR_ARG: Invalid scale factors or dimensions
   
   See also:
   - af-resize: Alternative resizing function
   - af-transform: General transformation
   - af-translate: Translation only
   - af-skew: Shear transformation"
  "af_scale" [::mem/pointer ::mem/pointer ::mem/float ::mem/float ::mem/long ::mem/long ::mem/int] ::mem/int)

;; af_err af_skew(af_array *out, const af_array in, const float skew0, const float skew1, const dim_t odim0, const dim_t odim1, const af_interp_type method, const bool inverse)
(defcfn af-skew
  "Skew (shear) an image along specified axes.
   
   Applies shear transformation along first and second dimensions,
   creating a parallelogram effect. This is a specialized version
   of af-transform for skewing operations.
   
   Parameters:
   - out: Output pointer for skewed image
   - in: Input image array
   - skew0: Skew angle for first dimension (radians)
     * Shears along y-axis
   - skew1: Skew angle for second dimension (radians)
     * Shears along x-axis
   - odim0: Output height (0 = use input height)
   - odim1: Output width (0 = use input width)
   - method: Interpolation method (0-5)
   - inverse: Transformation direction (bool, use 1 or 0)
     * true (1): Apply inverse skew
     * false (0): Apply forward skew
   
   Skew Matrix:
   ```
   ┌              ┐
   │  1   ky   0  │  where kx = tan(skew0), ky = tan(skew1)
   │  kx   1   0  │
   └              ┘
   ```
   
   For inverse transformation (inverse=true):
   ```
   ┌                      ┐
   │  1/(1-kx·ky)  -ky/(1-kx·ky)  0 │
   │  -kx/(1-kx·ky)  1/(1-kx·ky)  0 │
   └                      ┘
   ```
   
   Behavior:
   - Positive skew0: Shears rows rightward (top stays, bottom shifts right)
   - Negative skew0: Shears rows leftward
   - Positive skew1: Shears columns downward (left stays, right shifts down)
   - Negative skew1: Shears columns upward
   - Small angles (< 30°) typically produce reasonable results
   
   Type Support:
   - All image types supported
   - Skew angles are float (radians)
   - Sub-pixel shifts use interpolation
   
   Performance:
   - GPU accelerated
   - O(odim0 * odim1) complexity
   - Similar speed to af-transform
   
   Example (Horizontal Shear):
   ```clojure
   ;; Create italic effect (shear rows)
   (let [img (load-image \"text.jpg\")
         skew-angle (/ Math/PI 12)  ; 15 degrees
         [h w] (get-dimensions img)
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-skew out-ptr img skew-angle 0.0 h w 1 1)  ; skew0 only
         italicized (mem/read-pointer out-ptr ::mem/pointer)]
     italicized)
   ```
   
   Example (Perspective Correction):
   ```clojure
   ;; Correct trapezoidal distortion
   (defn correct-trapezoid [img angle]
     (let [[h w] (get-dimensions img)
           ;; First estimate required output size
           corners-ptr (mem/alloc-pointer ::mem/pointer)
           skew-matrix (create-skew-matrix-3x3 angle 0)
           _ (af-transform-coordinates corners-ptr skew-matrix h w)
           corners (mem/read-pointer corners-ptr ::mem/pointer)
           [out-h out-w] (compute-bounding-box corners)
           out-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-skew out-ptr img angle 0.0 out-h out-w 1 1)]
       (mem/read-pointer out-ptr ::mem/pointer)))
   ```
   
   Example (Document Deskew):
   ```clojure
   ;; Correct skewed scanned document
   (defn deskew-document [img skew-angle]
     (let [[h w] (get-dimensions img)
           out-ptr (mem/alloc-pointer ::mem/pointer)
           ;; Use inverse skew to correct
           _ (af-skew out-ptr img (- skew-angle) 0.0 h w 1 1)]
       (mem/read-pointer out-ptr ::mem/pointer)))
   ```
   
   Example (Artistic Effect):
   ```clojure
   ;; Create dynamic motion effect
   (defn create-motion-effect [img]
     (let [[h w] (get-dimensions img)
           skew-x (/ Math/PI 24)  ; Slight horizontal skew
           skew-y (/ Math/PI 32)  ; Slight vertical skew
           out-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-skew out-ptr img skew-x skew-y h w 1 0)]  ; forward
       (mem/read-pointer out-ptr ::mem/pointer)))
   ```
   
   Use Cases:
   - Document deskewing (OCR preprocessing)
   - Perspective correction (mild cases)
   - Artistic effects (italicization, motion)
   - Geometric distortion correction
   - Parallelogram transformations
   - Testing geometric invariance
   
   Forward vs Inverse:
   - **Inverse (true)**: Recommended for most cases
     * Avoids holes in output
     * More predictable results
   - **Forward (false)**: Use when specifically needed
     * May create sampling artifacts
   
   Output Dimensions:
   - If odim0=0, odim1=0: Uses input dimensions
   - May need larger dimensions to capture full skewed image
   - Use af-transform-coordinates to compute required size
   - Clipping occurs if output dimensions too small
   
   Special Cases:
   - skew0=0, skew1=0: Identity transformation (no change)
   - Large angles (> 45°): May cause severe distortion
   - kx·ky ≈ 1: Transformation becomes singular (avoid)
   
   Notes:
   - Angles in radians, not degrees
   - tan() used internally to compute shear factors
   - Inverse computation handles special case when kx=0 or ky=0
   - Zero padding for out-of-bounds regions
   
   Returns:
   ArrayFire error code (af_err enum):
   - AF_SUCCESS (0): Skew successful
   - AF_ERR_SIZE: Invalid dimensions
   - AF_ERR_ARG: Invalid parameters
   
   See also:
   - af-transform: General transformation with matrix
   - af-rotate: Rotation transformation
   - af-translate: Translation only
   - af-scale: Scaling transformation"
  "af_skew" [::mem/pointer ::mem/pointer ::mem/float ::mem/float ::mem/long ::mem/long ::mem/int ::mem/int] ::mem/int)
