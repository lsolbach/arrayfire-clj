(ns org.soulspace.arrayfire.ffi.c-api.moments
  "Bindings for the ArrayFire image moments functions.
   
   Image moments are quantitative measures of the shape and distribution of
   intensity in an image. They are fundamental to many computer vision and
   image processing tasks.
   
   # What are Image Moments?
   
   An **image moment** is a weighted average (moment) of image pixel intensities,
   often chosen to have some attractive property or interpretation.
   
   For a 2D continuous function f(x,y), the moment of order (p+q) is:
   
   ```
   M_pq = ∫∫ x^p * y^q * f(x,y) dx dy
   ```
   
   For discrete images (pixels):
   
   ```
   M_pq = Σ_x Σ_y x^p * y^q * I(x,y)
   ```
   
   where I(x,y) is the pixel intensity at position (x,y).
   
   # Moment Types in ArrayFire
   
   ArrayFire supports first-order moments (up to order 1):
   
   ## M00 - Zeroth Moment (Total Mass)
   ```
   M00 = Σ_x Σ_y I(x,y)
   ```
   - Sum of all pixel intensities
   - Total 'mass' or 'area' of the image
   - For binary images: number of foreground pixels
   - Always positive (assuming non-negative intensities)
   
   **Applications:**
   - Area calculation for segmented regions
   - Normalization constant for other moments
   - Object presence detection (M00 = 0 means no object)
   
   ## M01 - First Moment about Y-axis
   ```
   M01 = Σ_x Σ_y x * I(x,y)
   ```
   - Weighted sum along x-direction
   - Combined with M00, gives x-coordinate of centroid
   - Centroid x-coordinate: x̄ = M01 / M00
   
   **Applications:**
   - Centroid calculation (object center)
   - Horizontal position of mass center
   - Balance point along x-axis
   
   ## M10 - First Moment about X-axis
   ```
   M10 = Σ_x Σ_y y * I(x,y)
   ```
   - Weighted sum along y-direction
   - Combined with M00, gives y-coordinate of centroid
   - Centroid y-coordinate: ȳ = M10 / M00
   
   **Applications:**
   - Centroid calculation (object center)
   - Vertical position of mass center
   - Balance point along y-axis
   
   ## M11 - Second Mixed Moment
   ```
   M11 = Σ_x Σ_y x * y * I(x,y)
   ```
   - Correlation between x and y weighted by intensity
   - Used in orientation and shape analysis
   - Part of covariance matrix calculation
   
   **Applications:**
   - Object orientation calculation
   - Shape analysis (elongation, eccentricity)
   - Covariance computation for principal axes
   
   # Computing Centroids
   
   The centroid (center of mass) is the most common use of moments:
   
   ```
   x̄ = M01 / M00  (x-coordinate of centroid)
   ȳ = M10 / M00  (y-coordinate of centroid)
   ```
   
   For a uniform object (all pixels same intensity), the centroid is the
   geometric center.
   
   # Computing Orientation
   
   Using central moments (moments relative to centroid), you can compute
   object orientation:
   
   ```
   μ11 = M11 - x̄ * M10 = M11 - (M01 * M10) / M00
   μ20 = M20 - x̄ * M10  (requires M20, not in first-order)
   μ02 = M02 - ȳ * M01  (requires M02, not in first-order)
   
   θ = 0.5 * atan2(2 * μ11, μ20 - μ02)
   ```
   
   Note: Full orientation requires second-order moments (M20, M02), which
   ArrayFire currently doesn't expose through af_moment_type enum.
   
   # Batch Processing
   
   ArrayFire's moments functions support batch processing:
   - Input dimensions [width, height, batch_z, batch_w]
   - Output dimensions [num_moments, 1, batch_z, batch_w]
   - Each image in batch processed independently
   - Highly parallelized on GPU
   
   # Combined Moment Calculation
   
   The `AF_MOMENT_FIRST_ORDER` flag combines all four first-order moments:
   ```
   AF_MOMENT_FIRST_ORDER = AF_MOMENT_M00 | AF_MOMENT_M01 | 
                           AF_MOMENT_M10 | AF_MOMENT_M11
   ```
   
   Output array structure when using AF_MOMENT_FIRST_ORDER:
   - out[0] = M00
   - out[1] = M01
   - out[2] = M10
   - out[3] = M11
   
   Computing all moments together is more efficient than computing them
   separately, as it requires only one pass over the image data.
   
   # Mathematical Properties
   
   ## Linearity
   Moments are linear transformations:
   ```
   M_pq(a*f + b*g) = a*M_pq(f) + b*M_pq(g)
   ```
   
   ## Translation
   Moments change predictably under translation:
   ```
   If g(x,y) = f(x-a, y-b), then:
   M'_pq = Σ_i Σ_j C(p,i)*C(q,j) * (-a)^(p-i) * (-b)^(q-j) * M_ij
   ```
   where C(n,k) is binomial coefficient.
   
   Central moments (relative to centroid) are translation-invariant.
   
   ## Scaling
   Under uniform scaling s:
   ```
   M'_pq = s^(p+q+2) * M_pq
   ```
   
   Normalized moments can be made scale-invariant.
   
   # Performance Characteristics
   
   - **Complexity:** O(W × H) per image, where W×H is image size
   - **GPU Acceleration:** Highly parallel, typically 20-100× faster than CPU
   - **Memory Access:** Sequential read of image pixels
   - **Computation:** Simple arithmetic (multiplication, addition)
   - **Batch Performance:** Linear scaling with number of images
   
   Typical performance:
   - 512×512 image: ~0.1 ms (GPU), ~10 ms (CPU)
   - Batch of 100 images: ~1 ms (GPU), ~1 s (CPU)
   - All four moments computed in single pass
   
   # Type Support
   
   Input types supported:
   - Floating point: f32, f64
   - Integer: s32, u32, s16, u16
   - Boolean: b8 (treated as 0 or 1)
   
   Output type:
   - Always float (f32) in af_moments
   - Always double in af_moments_all
   
   Integer inputs are processed without conversion, but output is float.
   
   # Common Applications
   
   ## 1. Object Detection and Localization
   - M00 = 0 means no object present
   - (x̄, ȳ) = (M01/M00, M10/M00) gives object center
   - Robust to noise in boundary
   
   ## 2. Shape Analysis
   - M00 gives area/size
   - M11 combined with M00 gives orientation
   - Moment ratios characterize shape
   
   ## 3. Pattern Recognition
   - Moments as feature vector
   - Translation-invariant features using central moments
   - Scale-invariant features using normalized moments
   
   ## 4. Image Registration
   - Align images by matching centroids
   - Match orientations using principal axes
   - Quick initial alignment before fine registration
   
   ## 5. Tracking
   - Track object center over time using centroid
   - Detect rotation using orientation
   - Detect scale changes using M00
   
   ## 6. Medical Imaging
   - Tumor location (centroid)
   - Organ size (M00)
   - Lesion shape analysis (moment ratios)
   
   ## 7. Quality Control
   - Part positioning (centroid vs expected)
   - Size verification (M00 vs specification)
   - Orientation check
   
   # Limitations
   
   1. **First-order only:** ArrayFire's enum only exposes M00, M01, M10, M11
      - Cannot directly compute second-order moments (M20, M02)
      - Cannot compute full orientation without additional code
      - Cannot compute elongation/eccentricity directly
   
   2. **2D only:** Works on 2D images
      - 3D volume moments not supported through this API
      - Must process volume slices individually if needed
   
   3. **No central moments:** Output is raw moments
      - Need to manually compute central moments (relative to centroid)
      - Need to manually compute normalized moments (scale-invariant)
   
   4. **Integer overflow:** Large images with integer types
      - M01 and M10 can overflow for large images
      - Use float images for large sizes or high intensities
   
   # Usage Patterns
   
   ## Pattern 1: Compute Centroid
   ```clojure
   ;; Get all first-order moments
   (let [out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-moments out-ptr img-array AF_MOMENT_FIRST_ORDER)
         moments-array (mem/read-pointer out-ptr ::mem/pointer)
         ;; Read moment values
         moments (read-moments-to-vector moments-array)]
     (let [M00 (nth moments 0)
           M01 (nth moments 1)
           M10 (nth moments 2)]
       {:centroid-x (/ M01 M00)
        :centroid-y (/ M10 M00)
        :area M00}))
   ```
   
   ## Pattern 2: Batch Processing
   ```clojure
   ;; Compute centroids for batch of images [W,H,N,1]
   (let [out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-moments out-ptr img-batch AF_MOMENT_FIRST_ORDER)
         moments-array (mem/read-pointer out-ptr ::mem/pointer)
         ;; moments-array shape: [4, 1, N, 1]
         ;; Each image has 4 moments: M00, M01, M10, M11
         ]
     ;; Extract centroids for each image
     (map-indexed
       (fn [i _]
         (let [M00 (read-element moments-array [0 0 i 0])
               M01 (read-element moments-array [1 0 i 0])
               M10 (read-element moments-array [2 0 i 0])]
           {:image i
            :centroid [(/ M01 M00) (/ M10 M00)]
            :area M00}))
       (range N)))
   ```
   
   ## Pattern 3: Simple 2D Image Centroid
   ```clojure
   ;; For single 2D image, use af_moments_all for convenience
   (let [moments (double-array 4)  ; Space for all 4 first-order moments
         _ (af-moments-all moments img-array AF_MOMENT_FIRST_ORDER)
         M00 (aget moments 0)
         M01 (aget moments 1)
         M10 (aget moments 2)]
     {:x (/ M01 M00)
      :y (/ M10 M00)})
   ```
   
   ## Pattern 4: Object Detection
   ```clojure
   ;; Detect if object present in image
   (let [m00-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-moments m00-ptr binary-img AF_MOMENT_M00)
         m00-array (mem/read-pointer m00-ptr ::mem/pointer)
         m00-val (read-scalar m00-array)]
     (if (> m00-val threshold)
       :object-detected
       :no-object))
   ```
   
   # Best Practices
   
   1. **Normalize intensities:** For consistent results, normalize pixel values
      to [0, 1] range before computing moments
   
   2. **Use appropriate types:** For large images, use f32 or f64 to avoid
      integer overflow in M01, M10
   
   3. **Compute all at once:** Use AF_MOMENT_FIRST_ORDER to compute all four
      moments in one pass for efficiency
   
   4. **Binary images:** For shape analysis, threshold to binary first:
      - More stable centroid
      - M00 directly gives area in pixels
      - Less sensitive to illumination
   
   5. **Check for empty:** Always verify M00 > 0 before dividing by it to
      compute centroid (avoid division by zero)
   
   6. **Batch when possible:** Process multiple images together for better
      GPU utilization
   
   # Error Handling
   
   Potential errors:
   - **AF_ERR_ARG:** Invalid moment type flag
   - **AF_ERR_SIZE:** Input dimensions [2] and [3] must be 1 for af_moments_all
   - **AF_ERR_TYPE:** Unsupported input type (e.g., complex numbers)
   - **AF_ERR_ARR:** Invalid array handle
   
   # Comparison with Alternatives
   
   ## vs Manual Computation
   - **ArrayFire moments:** Optimized GPU kernel, single pass
   - **Manual loops:** Multiple array operations, slower
   - **Trade-off:** ArrayFire is 10-100× faster
   
   ## vs OpenCV moments
   - **ArrayFire:** GPU-accelerated, batch processing
   - **OpenCV:** CPU-based (mostly), more moment types (up to order 3)
   - **Trade-off:** ArrayFire faster, OpenCV more complete
   
   ## vs Reduction Operations
   - **Moments:** Structured computation with spatial weights
   - **Reductions (sum, mean):** Simple aggregation
   - **Trade-off:** Moments provide spatial information, reductions faster
   
   # See Also
   
   - Histogram functions for intensity distribution
   - Connected components for object labeling
   - Contours for boundary analysis
   - Feature detectors (Harris, FAST) for keypoint moments"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.loader]))

;; Moment type enum values (from af/defines.h)
;; typedef enum {
;;     AF_MOMENT_M00 = 1,
;;     AF_MOMENT_M01 = 2,
;;     AF_MOMENT_M10 = 4,
;;     AF_MOMENT_M11 = 8,
;;     AF_MOMENT_FIRST_ORDER = AF_MOMENT_M00 | AF_MOMENT_M01 | AF_MOMENT_M10 | AF_MOMENT_M11
;; } af_moment_type;

(def AF_MOMENT_M00 1)
(def AF_MOMENT_M01 2)
(def AF_MOMENT_M10 4)
(def AF_MOMENT_M11 8)
(def AF_MOMENT_FIRST_ORDER 15) ; 1 | 2 | 4 | 8

;; af_err af_moments(af_array *out, const af_array in, const af_moment_type moment)
(defcfn af-moments
  "Calculate image moments for batch of images.
   
   Image moments are weighted averages of pixel intensities that provide
   quantitative measures of shape, position, and distribution. This function
   computes first-order moments (M00, M01, M10, M11) which are fundamental
   to computer vision tasks like object localization and shape analysis.
   
   Parameters:
   - out: out pointer for moment array
     * Shape: [num_moments, 1, batch_z, batch_w]
     * num_moments = bit count of moment parameter (1-4)
     * Type: float (f32)
   - in: input image(s) array
     * Shape: [width, height, batch_z, batch_w]
     * Batch processing: each image computed independently
   - moment: moment type(s) to calculate (af_moment_type flags)
     * AF_MOMENT_M00 (1): Zeroth moment (total mass/area)
     * AF_MOMENT_M01 (2): First moment about y-axis
     * AF_MOMENT_M10 (4): First moment about x-axis
     * AF_MOMENT_M11 (8): Second mixed moment
     * AF_MOMENT_FIRST_ORDER (15): All four moments (bitwise OR)
   
   # Moment Definitions
   
   For an image I(x, y) with dimensions W×H:
   
   **M00 - Zeroth Moment (Total Mass):**
   ```
   M00 = Σ_{x=0}^{W-1} Σ_{y=0}^{H-1} I(x,y)
   ```
   - Sum of all pixel intensities
   - For binary images: number of foreground pixels
   - Always ≥ 0 (assuming non-negative intensities)
   - Interpretation: 'mass' or 'area' of the image region
   
   **M01 - First Moment about Y-axis:**
   ```
   M01 = Σ_{x=0}^{W-1} Σ_{y=0}^{H-1} x * I(x,y)
   ```
   - X-weighted sum of intensities
   - Centroid x-coordinate: x̄ = M01 / M00
   - Zero if object centered at x=0
   - Units: pixels × intensity
   
   **M10 - First Moment about X-axis:**
   ```
   M10 = Σ_{x=0}^{W-1} Σ_{y=0}^{H-1} y * I(x,y)
   ```
   - Y-weighted sum of intensities
   - Centroid y-coordinate: ȳ = M10 / M00
   - Zero if object centered at y=0
   - Units: pixels × intensity
   
   **M11 - Second Mixed Moment:**
   ```
   M11 = Σ_{x=0}^{W-1} Σ_{y=0}^{H-1} x * y * I(x,y)
   ```
   - XY-weighted sum (correlation term)
   - Used for orientation and shape analysis
   - Part of covariance matrix
   - Units: pixels² × intensity
   
   # Output Structure
   
   When multiple moments are requested, they are packed in the order:
   M00, M01, M10, M11 (in order of bit position in flags).
   
   Example with AF_MOMENT_FIRST_ORDER:
   ```
   out[0, 0, z, w] = M00 of image[z, w]
   out[1, 0, z, w] = M01 of image[z, w]
   out[2, 0, z, w] = M10 of image[z, w]
   out[3, 0, z, w] = M11 of image[z, w]
   ```
   
   If only M00 and M10 requested (flags 1 | 4 = 5):
   ```
   out[0, 0, z, w] = M00 of image[z, w]
   out[1, 0, z, w] = M10 of image[z, w]
   ```
   
   # Centroid Calculation
   
   The centroid (center of mass) is the most common use:
   ```
   x̄ = M01 / M00  (x-coordinate)
   ȳ = M10 / M00  (y-coordinate)
   ```
   
   Always check M00 ≠ 0 before dividing to avoid division by zero.
   
   # Algorithm
   
   Single-pass computation over image pixels:
   ```
   For each pixel (x, y) in image:
     if AF_MOMENT_M00: accumulate I(x,y)
     if AF_MOMENT_M01: accumulate x * I(x,y)
     if AF_MOMENT_M10: accumulate y * I(x,y)
     if AF_MOMENT_M11: accumulate x * y * I(x,y)
   ```
   
   All requested moments computed in one pass for efficiency.
   
   # Performance
   
   - **Complexity:** O(W × H) per image
   - **GPU:** Highly parallel, 20-100× faster than CPU
   - **Batch:** Processes all images in parallel
   - **Memory:** Sequential read of pixels
   
   Typical timings (NVIDIA GPU):
   - 512×512 image, 1 moment: ~0.05 ms
   - 512×512 image, 4 moments: ~0.1 ms (marginal cost)
   - Batch of 100 images: ~1 ms total
   
   Computing all four moments together is only slightly slower than
   computing one, as memory bandwidth dominates.
   
   # Type Support
   
   **Input types:**
   - Floating point: f32, f64
   - Signed integers: s32, s16
   - Unsigned integers: u32, u16
   - Boolean: b8 (0 or 1)
   
   **Output type:**
   - Always float (f32)
   
   Integer inputs are processed as-is, but output is float.
   For large images with integers, consider using float input to
   avoid potential overflow in M01, M10.
   
   # Example 1: Single Image Centroid
   ```clojure
   ;; Compute centroid of a 512×512 grayscale image
   (let [img-array (create-array img-data [512 512])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         ;; Request all first-order moments
         _ (af-moments out-ptr img-array AF_MOMENT_FIRST_ORDER)
         moments-array (mem/read-pointer out-ptr ::mem/pointer)
         ;; Read moments as vector [M00, M01, M10, M11]
         moments (read-array-to-vector moments-array)]
     (let [M00 (nth moments 0)
           M01 (nth moments 1)
           M10 (nth moments 2)
           M11 (nth moments 3)]
       (if (> M00 0)
         {:centroid-x (/ M01 M00)
          :centroid-y (/ M10 M00)
          :area M00
          :moment-xy M11}
         {:error \"Empty image (M00 = 0)\"})))
   
   ;; Result:
   ;; {:centroid-x 255.3, :centroid-y 256.1, :area 245328.5, :moment-xy 1.634e10}
   ```
   
   # Example 2: Object Detection in Binary Image
   ```clojure
   ;; Detect if object present and find its center
   (let [;; Threshold to binary (0 or 1)
         binary-img (> img threshold)
         out-ptr (mem/alloc-pointer ::mem/pointer)
         ;; Only need M00, M01, M10 for centroid
         moment-flags (bit-or AF_MOMENT_M00 AF_MOMENT_M01 AF_MOMENT_M10)
         _ (af-moments out-ptr binary-img moment-flags)
         moments-array (mem/read-pointer out-ptr ::mem/pointer)
         [M00 M01 M10] (read-array-to-vector moments-array)]
     (if (> M00 10)  ; At least 10 pixels
       {:detected true
        :center [(/ M01 M00) (/ M10 M00)]
        :num-pixels M00}
       {:detected false}))
   
   ;; Result for object present:
   ;; {:detected true, :center [127.3 189.6], :num-pixels 1523.0}
   ```
   
   # Example 3: Batch Processing Multiple Images
   ```clojure
   ;; Find centroids for 20 images simultaneously
   (let [;; Images stacked: [512, 512, 20, 1]
         img-batch (create-batch images [512 512 20])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-moments out-ptr img-batch AF_MOMENT_FIRST_ORDER)
         moments-array (mem/read-pointer out-ptr ::mem/pointer)
         ;; Output shape: [4, 1, 20, 1] - 4 moments per image
         ]
     ;; Extract centroid for each image
     (for [i (range 20)]
       (let [;; Get moments for image i
             M00 (read-element moments-array [0 0 i 0])
             M01 (read-element moments-array [1 0 i 0])
             M10 (read-element moments-array [2 0 i 0])]
         {:image-id i
          :centroid [(/ M01 M00) (/ M10 M00)]
          :area M00})))
   
   ;; Result: List of 20 centroids computed in parallel
   ;; ({:image-id 0, :centroid [234.5 189.2], :area 34982.1}
   ;;  {:image-id 1, :centroid [189.3 245.7], :area 38201.5}
   ;;  ...)
   ```
   
   # Example 4: Tracking Object Over Time
   ```clojure
   ;; Track object center across video frames
   (let [video-frames (load-video \"path/to/video.mp4\")
         ;; Shape: [W, H, num_frames, 1]
         out-ptr (mem/alloc-pointer ::mem/pointer)
         moment-flags (bit-or AF_MOMENT_M00 AF_MOMENT_M01 AF_MOMENT_M10)
         _ (af-moments out-ptr video-frames moment-flags)
         moments (mem/read-pointer out-ptr ::mem/pointer)]
     ;; Compute trajectory
     (map-indexed
       (fn [frame-idx _]
         (let [M00 (read-element moments [0 0 frame-idx 0])
               M01 (read-element moments [1 0 frame-idx 0])
               M10 (read-element moments [2 0 frame-idx 0])]
           {:frame frame-idx
            :time (* frame-idx (/ 1.0 30.0))  ; Assume 30 fps
            :position [(/ M01 M00) (/ M10 M00)]
            :visible (> M00 100)}))
       (range num-frames)))
   
   ;; Result: Trajectory of object over time
   ;; ({:frame 0, :time 0.0, :position [120.3 200.5], :visible true}
   ;;  {:frame 1, :time 0.033, :position [122.1 201.8], :visible true}
   ;;  ...)
   ```
   
   # Example 5: Shape Classification by Moments
   ```clojure
   ;; Classify shapes using moment ratios
   (defn compute-moment-features [img]
     (let [out-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-moments out-ptr img AF_MOMENT_FIRST_ORDER)
           moments (mem/read-pointer out-ptr ::mem/pointer)
           [M00 M01 M10 M11] (read-array-to-vector moments)
           ;; Centroid
           cx (/ M01 M00)
           cy (/ M10 M00)
           ;; Central moment (relative to centroid)
           mu11 (- M11 (* cx M10))]
       {:area M00
        :centroid [cx cy]
        :normalized-mu11 (/ mu11 (Math/pow M00 2))  ; Scale invariant
        }))
   
   ;; Classify multiple shapes
   (let [shapes (load-shape-images [\"circle.png\" \"square.png\" \"ellipse.png\"])
         features (map compute-moment-features shapes)]
     (map classify-shape features))
   
   ;; Normalized mu11 helps distinguish:
   ;; - Circle: ~0 (symmetric)
   ;; - Vertical ellipse: negative
   ;; - Horizontal ellipse: positive
   ;; - Square: ~0 (symmetric)
   ```
   
   # Example 6: Image Registration by Centroids
   ```clojure
   ;; Align two images by matching their centroids
   (defn compute-centroid [img]
     (let [out-ptr (mem/alloc-pointer ::mem/pointer)
           flags (bit-or AF_MOMENT_M00 AF_MOMENT_M01 AF_MOMENT_M10)
           _ (af-moments out-ptr img flags)
           [M00 M01 M10] (-> out-ptr
                             (mem/read-pointer ::mem/pointer)
                             read-array-to-vector)]
       [(/ M01 M00) (/ M10 M00)]))
   
   (let [ref-img (load-image \"reference.png\")
         target-img (load-image \"target.png\")
         [ref-cx ref-cy] (compute-centroid ref-img)
         [tgt-cx tgt-cy] (compute-centroid target-img)
         ;; Translation to align centroids
         dx (- ref-cx tgt-cx)
         dy (- ref-cy tgt-cy)
         ;; Apply translation
         aligned-img (translate target-img dx dy)]
     {:translation [dx dy]
      :aligned aligned-img})
   
   ;; Result: target-img translated so centroids match
   ;; Useful as initial alignment before fine registration
   ```
   
   # Example 7: Medical Image - Tumor Localization
   ```clojure
   ;; Locate tumor in MRI slice batch
   (let [;; Load batch of MRI slices [512, 512, 30, 1]
         mri-slices (load-mri-volume \"patient001.nii\")
         ;; Segment tumors (threshold + morphology)
         tumor-masks (segment-tumors mri-slices)
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-moments out-ptr tumor-masks AF_MOMENT_FIRST_ORDER)
         moments (mem/read-pointer out-ptr ::mem/pointer)]
     ;; Extract tumor properties per slice
     (for [slice-idx (range 30)]
       (let [M00 (read-element moments [0 0 slice-idx 0])
             M01 (read-element moments [1 0 slice-idx 0])
             M10 (read-element moments [2 0 slice-idx 0])]
         (if (> M00 50)  ; Tumor present if >50 pixels
           {:slice slice-idx
            :tumor-present true
            :center-mm [(* (/ M01 M00) 0.5)   ; Convert pixels to mm
                        (* (/ M10 M00) 0.5)]
            :volume-mm3 (* M00 0.25 5.0)}  ; Pixel area × slice thickness
           {:slice slice-idx
            :tumor-present false}))))
   
   ;; Result: Tumor location and size in each slice
   ;; Helps physician localize and measure lesion
   ```
   
   # Common Patterns
   
   **Pattern: Safe Centroid Computation**
   ```clojure
   (defn safe-centroid [moments-array]
     (let [[M00 M01 M10] (take 3 (read-array-to-vector moments-array))]
       (if (> M00 1e-6)  ; Avoid division by near-zero
         [(/ M01 M00) (/ M10 M00)]
         nil)))  ; Return nil for empty image
   ```
   
   **Pattern: Batch Centroid Extraction**
   ```clojure
   (defn extract-batch-centroids [moments-array batch-size]
     (for [i (range batch-size)]
       (let [M00 (read-element moments-array [0 0 i 0])
             M01 (read-element moments-array [1 0 i 0])
             M10 (read-element moments-array [2 0 i 0])]
         (when (> M00 0)
           [(/ M01 M00) (/ M10 M00)]))))
   ```
   
   **Pattern: Moment-Based Features**
   ```clojure
   (defn moment-features [img]
     (let [moments (compute-all-moments img)
           [M00 M01 M10 M11] moments
           cx (/ M01 M00)
           cy (/ M10 M00)]
       {:area M00
        :centroid [cx cy]
        :central-m11 (- M11 (* cx M10))}))
   ```
   
   # When to Use
   
   **Use moments when you need:**
   - Object localization (centroid)
   - Area/size measurement
   - Basic shape features
   - Batch processing (many images)
   - GPU acceleration
   
   **Don't use moments when:**
   - You need detailed shape analysis (use contours or descriptors)
   - Sub-pixel precision centroid needed (use weighted Gaussian)
   - You need rotation/scale invariance (use normalized central moments)
   - Only pixel counting needed (use sum/count reductions)
   
   # Gotchas
   
   1. **Empty images:** Always check M00 > 0 before computing centroid
   2. **Integer overflow:** Large images with integer types can overflow in M01, M10
   3. **Memory layout:** Moment order in output follows bit order: M00, M01, M10, M11
   4. **Batch dimensions:** For af_moments_all, batch dims must be 1
   5. **Coordinate system:** (0,0) is top-left, x increases right, y increases down
   
   # Errors
   
   - AF_ERR_ARG: Invalid moment type (not combination of valid flags)
   - AF_ERR_TYPE: Unsupported input type (e.g., complex)
   - AF_ERR_ARR: Invalid array handle
   - AF_ERR_NO_MEM: Insufficient memory for output array
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-moments-all: Compute moments for single 2D image (convenience)
   - af-histogram: For intensity distribution
   - Contour functions: For detailed boundary analysis
   - Feature detectors: For keypoint-based moments"
  "af_moments" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_moments_all(double* out, const af_array in, const af_moment_type moment)
(defcfn af-moments-all
  "Calculate image moments for a single 2D image (convenience function).
   
   This is a convenience wrapper around af-moments specifically for single
   2D images (not batches). It computes moments and directly returns them
   as an array of doubles, avoiding the need to extract from an af_array.
   
   Parameters:
   - out: pointer to pre-allocated double array for results
     * Size must be ≥ number of moments requested (1-4 doubles)
     * Order: M00, M01, M10, M11 (in bit order)
   - in: input image array
     * Must be 2D: dimensions [2] and [3] must equal 1
     * Shape: [width, height, 1, 1] or [width, height]
   - moment: moment type(s) to calculate (same flags as af-moments)
     * AF_MOMENT_M00 (1): One result
     * AF_MOMENT_M01 (2): One result
     * AF_MOMENT_M00 | AF_MOMENT_M01 (3): Two results
     * AF_MOMENT_FIRST_ORDER (15): Four results
   
   # Difference from af-moments
   
   **af-moments:**
   - Returns af_array (must read back to host)
   - Supports batches (multiple images)
   - Output type: float (f32)
   
   **af-moments-all:**
   - Returns directly to double array (already on host)
   - Single image only (2D)
   - Output type: double (f64)
   - More convenient for simple cases
   
   # Output Layout
   
   Results are written to the `out` array in order of bit position:
   
   ```
   AF_MOMENT_M00 set → out[0] = M00
   AF_MOMENT_M01 set → out[1] = M01  (after M00 if both)
   AF_MOMENT_M10 set → out[2] = M10  (after M00, M01 if all three)
   AF_MOMENT_M11 set → out[3] = M11  (after M00, M01, M10 if all four)
   ```
   
   With AF_MOMENT_FIRST_ORDER (all four):
   ```
   out[0] = M00  (total intensity/area)
   out[1] = M01  (x-weighted sum)
   out[2] = M10  (y-weighted sum)
   out[3] = M11  (xy-weighted sum)
   ```
   
   # Memory Management
   
   Caller must pre-allocate the `out` array:
   ```clojure
   (let [moments (double-array 4)  ; Pre-allocate for all 4 moments
         _ (af-moments-all moments img AF_MOMENT_FIRST_ORDER)
         M00 (aget moments 0)
         M01 (aget moments 1)
         M10 (aget moments 2)
         M11 (aget moments 3)]
     ...)
   ```
   
   For fewer moments, allocate only what's needed:
   ```clojure
   (let [moments (double-array 1)  ; Only M00
         _ (af-moments-all moments img AF_MOMENT_M00)
         area (aget moments 0)]
     ...)
   ```
   
   # Algorithm
   
   Same as af-moments, but with additional host transfer:
   1. Compute moments on GPU (same as af-moments)
   2. Copy results to host memory
   3. Convert float to double
   4. Write to caller's array
   
   # Performance
   
   - **GPU Computation:** Same as af-moments (~0.1 ms for 512×512)
   - **Host Transfer:** Additional ~0.01 ms for small result array
   - **Total:** Slightly slower than af-moments, but negligible
   
   For batch processing, af-moments is more efficient as it avoids
   multiple host transfers.
   
   # Type Support
   
   **Input:** Same as af-moments (f32, f64, s32, u32, s16, u16, b8)
   **Output:** Always double (f64)
   
   Note the type difference from af-moments (which outputs f32).
   
   # Example 1: Simple Centroid
   ```clojure
   ;; Most straightforward way to get centroid
   (defn compute-centroid [img-array]
     (let [moments (double-array 4)
           _ (af-moments-all moments img-array AF_MOMENT_FIRST_ORDER)
           M00 (aget moments 0)
           M01 (aget moments 1)
           M10 (aget moments 2)]
       (when (> M00 0)
         {:x (/ M01 M00)
          :y (/ M10 M00)
          :area M00})))
   
   ;; Usage:
   (let [img (create-array data [512 512])]
     (compute-centroid img))
   ;; => {:x 256.3, :y 189.7, :area 54892.0}
   ```
   
   # Example 2: Object Detection
   ```clojure
   ;; Quick check if object present and where
   (let [binary-img (threshold img 0.5)
         moments (double-array 3)  ; M00, M01, M10
         flags (bit-or AF_MOMENT_M00 AF_MOMENT_M01 AF_MOMENT_M10)
         _ (af-moments-all moments binary-img flags)
         M00 (aget moments 0)]
     (if (> M00 100)  ; At least 100 pixels
       (let [M01 (aget moments 1)
             M10 (aget moments 2)]
         {:detected true
          :center [(/ M01 M00) (/ M10 M00)]
          :pixels M00})
       {:detected false}))
   
   ;; Result:
   ;; {:detected true, :center [234.5 189.2], :pixels 1523.0}
   ```
   
   # Example 3: Area Measurement
   ```clojure
   ;; Measure area of segmented region
   (defn measure-region-area [segmented-img pixel-size-mm]
     (let [moments (double-array 1)  ; Only need M00
           _ (af-moments-all moments segmented-img AF_MOMENT_M00)
           pixel-count (aget moments 0)
           area-mm2 (* pixel-count pixel-size-mm pixel-size-mm)]
       {:pixels pixel-count
        :area-mm2 area-mm2}))
   
   ;; Usage:
   (measure-region-area tumor-mask 0.5)  ; 0.5 mm per pixel
   ;; => {:pixels 1234.0, :area-mm2 308.5}
   ```
   
   # Example 4: Centroid Tracking Loop
   ```clojure
   ;; Track object across video frames
   (defn track-centroids [video-frames]
     (let [num-frames (get-num-frames video-frames)
           moments (double-array 3)  ; Reuse array
           flags (bit-or AF_MOMENT_M00 AF_MOMENT_M01 AF_MOMENT_M10)]
       (for [i (range num-frames)]
         (let [frame (get-frame video-frames i)
               _ (af-moments-all moments frame flags)
               M00 (aget moments 0)
               M01 (aget moments 1)
               M10 (aget moments 2)]
           {:frame i
            :centroid (when (> M00 0)
                       [(/ M01 M00) (/ M10 M00)])}))))
   
   ;; Result: Sequence of centroids
   ;; ({:frame 0, :centroid [120.3 200.5]}
   ;;  {:frame 1, :centroid [122.1 201.8]}
   ;;  ...)
   ```
   
   # Example 5: Shape Properties
   ```clojure
   ;; Compute basic shape descriptors
   (defn shape-properties [binary-shape]
     (let [moments (double-array 4)
           _ (af-moments-all moments binary-shape AF_MOMENT_FIRST_ORDER)
           M00 (aget moments 0)
           M01 (aget moments 1)
           M10 (aget moments 2)
           M11 (aget moments 3)
           cx (/ M01 M00)
           cy (/ M10 M00)
           ;; Central moment (relative to centroid)
           mu11 (- M11 (* cx M10))]
       {:area M00
        :centroid [cx cy]
        :central-m11 mu11
        :normalized-m11 (/ mu11 (Math/pow M00 2))}))
   
   ;; Usage:
   (shape-properties circle-mask)
   ;; => {:area 314.16, :centroid [10.0 10.0], :central-m11 0.02, ...}
   ```
   
   # Example 6: Batch Alternative (Not Recommended)
   ```clojure
   ;; While you can loop over batch with af-moments-all,
   ;; it's much slower than using af-moments directly
   
   ;; SLOW - Don't do this for batches:
   (defn centroids-slow [img-batch batch-size]
     (let [moments (double-array 3)]
       (for [i (range batch-size)]
         (let [img (get-slice img-batch i)
               _ (af-moments-all moments img AF_MOMENT_FIRST_ORDER)
               M00 (aget moments 0)
               M01 (aget moments 1)
               M10 (aget moments 2)]
           [(/ M01 M00) (/ M10 M00)]))))
   
   ;; FAST - Use af-moments instead for batches:
   (defn centroids-fast [img-batch]
     ;; Process all images in one call
     (let [out-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-moments out-ptr img-batch AF_MOMENT_FIRST_ORDER)]
       ;; Extract results...
       ))
   ```
   
   # Example 7: Medical Imaging - Lesion Analysis
   ```clojure
   ;; Analyze lesion in medical image slice
   (defn analyze-lesion [mri-slice lesion-mask pixel-spacing-mm]
     (let [moments (double-array 4)
           _ (af-moments-all moments lesion-mask AF_MOMENT_FIRST_ORDER)
           M00 (aget moments 0)
           M01 (aget moments 1)
           M10 (aget moments 2)
           M11 (aget moments 3)]
       (if (> M00 0)
         (let [cx-px (/ M01 M00)
               cy-px (/ M10 M00)
               cx-mm (* cx-px pixel-spacing-mm)
               cy-mm (* cy-px pixel-spacing-mm)
               area-mm2 (* M00 pixel-spacing-mm pixel-spacing-mm)]
           {:lesion-found true
            :center-mm [cx-mm cy-mm]
            :area-mm2 area-mm2
            :num-pixels M00})
         {:lesion-found false})))
   
   ;; Usage:
   (analyze-lesion mri-slice segmentation 0.5)
   ;; => {:lesion-found true, :center-mm [23.5 41.2], :area-mm2 45.3, ...}
   ```
   
   # Common Patterns
   
   **Pattern: Centroid Helper**
   ```clojure
   (defn centroid [img]
     (let [m (double-array 3)
           flags (bit-or AF_MOMENT_M00 AF_MOMENT_M01 AF_MOMENT_M10)
           _ (af-moments-all m img flags)
           M00 (aget m 0)]
       (when (> M00 0)
         [(/ (aget m 1) M00) (/ (aget m 2) M00)])))
   ```
   
   **Pattern: Area Helper**
   ```clojure
   (defn area [img]
     (let [m (double-array 1)
           _ (af-moments-all m img AF_MOMENT_M00)]
       (aget m 0)))
   ```
   
   **Pattern: Reusable Buffer**
   ```clojure
   ;; For processing many images, reuse buffer
   (let [moments (double-array 4)]
     (doseq [img images]
       (af-moments-all moments img AF_MOMENT_FIRST_ORDER)
       (process-moments moments)))
   ```
   
   # When to Use
   
   **Use af-moments-all when:**
   - Processing single 2D image
   - Want simple double array output
   - Don't need to keep moments on GPU
   - Convenience over performance
   
   **Use af-moments instead when:**
   - Processing batches (multiple images)
   - Need to chain GPU operations
   - Want float precision (not double)
   - Performance critical
   
   # Gotchas
   
   1. **Pre-allocate:** Must allocate `out` array before calling
   2. **Size matters:** Allocate enough space for requested moments
   3. **2D only:** Input must be 2D (dims[2] = dims[3] = 1)
   4. **Double precision:** Output is double, input processed as-is
   5. **No batches:** For batches, use af-moments instead
   
   # Errors
   
   - AF_ERR_SIZE: Input has batch dimensions (dims[2] or dims[3] ≠ 1)
   - AF_ERR_ARG: Invalid moment type or null pointer
   - AF_ERR_TYPE: Unsupported input type
   - AF_ERR_ARR: Invalid array handle
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-moments: Batch version returning af_array
   - af-histogram: For intensity distribution
   - Reduction functions: For simpler aggregations"
  "af_moments_all" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)
