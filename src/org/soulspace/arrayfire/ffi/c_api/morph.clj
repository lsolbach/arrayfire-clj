(ns org.soulspace.arrayfire.ffi.c-api.morph
  "Bindings for the ArrayFire morphological operations.
   
   Morphological operations are fundamental image processing techniques that
   process images based on shapes. They apply structuring elements to input
   images to extract or modify structural features.
   
   # What are Morphological Operations?
   
   **Mathematical Morphology** is a theory and technique for analyzing and
   processing geometrical structures, based on set theory, lattice theory,
   topology, and random functions.
   
   Morphological operations process images using a **structuring element** (also
   called mask or kernel) - a small binary or grayscale image that probes the
   input image.
   
   # Core Operations
   
   ## Erosion (Minimum Filter)
   
   **Definition:**
   ```
   (A ⊖ B)(x,y) = min{A(x+i, y+j) | B(i,j) = 1}
   ```
   
   Erosion computes the minimum value over the neighborhood defined by the
   structuring element.
   
   **Effects:**
   - Shrinks bright regions
   - Expands dark regions
   - Removes small bright details (noise)
   - Thins object boundaries
   - Separates connected objects
   
   **Visual Analogy:** \"Acid etching\" - bright features are eaten away
   
   ## Dilation (Maximum Filter)
   
   **Definition:**
   ```
   (A ⊕ B)(x,y) = max{A(x+i, y+j) | B(i,j) = 1}
   ```
   
   Dilation computes the maximum value over the neighborhood defined by the
   structuring element.
   
   **Effects:**
   - Expands bright regions
   - Shrinks dark regions
   - Fills small holes
   - Connects nearby objects
   - Thickens object boundaries
   
   **Visual Analogy:** \"Growing\" - bright features expand outward
   
   # Composite Operations
   
   While not directly provided by this API, these operations can be composed
   from erosion and dilation:
   
   ## Opening
   ```clojure
   (defn opening [img mask]
     (dilate (erode img mask) mask))
   ```
   
   - Erosion followed by dilation
   - Removes small bright objects
   - Smooths object contours
   - Preserves larger structures
   
   ## Closing
   ```clojure
   (defn closing [img mask]
     (erode (dilate img mask) mask))
   ```
   
   - Dilation followed by erosion
   - Fills small holes
   - Connects nearby objects
   - Preserves overall size
   
   ## Morphological Gradient
   ```clojure
   (defn morph-gradient [img mask]
     (- (dilate img mask) (erode img mask)))
   ```
   
   - Difference between dilation and erosion
   - Highlights boundaries
   - Edge detection
   
   ## Top Hat
   ```clojure
   (defn top-hat [img mask]
     (- img (opening img mask)))
   ```
   
   - Original minus opening
   - Extracts small bright features
   - Background correction
   
   ## Bottom Hat
   ```clojure
   (defn bottom-hat [img mask]
     (- (closing img mask) img))
   ```
   
   - Closing minus original
   - Extracts small dark features
   - Background correction
   
   # Structuring Elements
   
   The structuring element (mask) defines the neighborhood shape and size.
   
   ## Common Shapes
   
   **Square (Box):**
   ```clojure
   (constant 1 3 3)  ; 3×3 square
   [1 1 1]
   [1 1 1]
   [1 1 1]
   ```
   
   **Cross:**
   ```clojure
   [0 1 0]
   [1 1 1]
   [0 1 0]
   ```
   
   **Diamond:**
   ```clojure
   [0 0 1 0 0]
   [0 1 1 1 0]
   [1 1 1 1 1]
   [0 1 1 1 0]
   [0 0 1 0 0]
   ```
   
   **Disk (Circular):**
   ```clojure
   ;; Approximate circle, radius 3
   [0 0 1 1 1 0 0]
   [0 1 1 1 1 1 0]
   [1 1 1 1 1 1 1]
   [1 1 1 1 1 1 1]
   [1 1 1 1 1 1 1]
   [0 1 1 1 1 1 0]
   [0 0 1 1 1 0 0]
   ```
   
   **Line (Oriented):**
   ```clojure
   ;; Horizontal line
   [1 1 1 1 1]
   
   ;; Vertical line
   [1]
   [1]
   [1]
   [1]
   [1]
   ```
   
   ## Size Selection
   
   - **Small (3×3, 5×5):** Fast, preserves details
   - **Medium (7×7, 9×9):** Moderate effect
   - **Large (11×11+):** Strong effect, may over-smooth
   
   ## Binary vs Grayscale Structuring Elements
   
   - **Binary (0/1):** Classic morphology, most common
   - **Grayscale:** Weighted operations, more flexible
   
   For ArrayFire, non-zero values participate in the operation.
   
   # 2D vs 3D Operations
   
   ## 2D Operations (af-erode, af-dilate)
   - Input: 2D images (width × height × channels × batch)
   - Mask: 2D structuring element
   - Processes each 2D slice independently
   
   ## 3D Operations (af-erode3, af-dilate3)
   - Input: 3D volumes (width × height × depth × batch)
   - Mask: 3D structuring element
   - True volumetric processing
   
   # Mathematical Properties
   
   ## Duality
   Erosion and dilation are dual operations:
   ```
   (A ⊖ B)^c = A^c ⊕ B̆
   ```
   where ^c is complement and B̆ is reflection of B
   
   ## Idempotence
   Opening and closing are idempotent:
   ```
   Opening(Opening(A)) = Opening(A)
   Closing(Closing(A)) = Closing(A)
   ```
   
   ## Monotonicity
   If A ⊆ C, then:
   ```
   A ⊖ B ⊆ C ⊖ B
   A ⊕ B ⊆ C ⊕ B
   ```
   
   ## Translation Invariance
   ```
   (A + x) ⊖ B = (A ⊖ B) + x
   (A + x) ⊕ B = (A ⊕ B) + x
   ```
   
   # Performance Characteristics
   
   - **Complexity:** O(W × H × M_w × M_h) per image
     - W, H: Image dimensions
     - M_w, M_h: Mask dimensions
   - **Optimization:** For large masks (>17×17), ArrayFire may use FFT-based
     convolution approach (much faster: O(W × H × log(W × H)))
   - **GPU Acceleration:** 10-100× speedup over CPU
   - **Batch Processing:** Highly parallel across multiple images
   
   Typical timings (NVIDIA GPU):
   - 512×512 image, 3×3 mask: ~0.1 ms
   - 512×512 image, 11×11 mask: ~0.3 ms
   - 512×512 image, 33×33 mask: ~2 ms (direct) or ~0.5 ms (FFT-based)
   - 3D: 128×128×128 volume, 3×3×3 mask: ~1 ms
   
   # Type Support
   
   **Input types:**
   - Floating point: f32, f64
   - Signed integers: s32, s16, s8
   - Unsigned integers: u32, u16, u8
   - Boolean: b8 (binary morphology)
   
   **Output type:** Same as input type
   
   **Note:** For boolean (b8) type, 1 represents object, 0 represents background
   
   # Common Applications
   
   ## 1. Noise Removal
   - **Salt noise (bright spots):** Opening = Erosion → Dilation
   - **Pepper noise (dark spots):** Closing = Dilation → Erosion
   - **Salt-and-pepper:** Opening → Closing or Closing → Opening
   
   ## 2. Object Separation
   - **Separate touching objects:** Erosion
   - **Reconnect after separation:** Dilation (carefully controlled)
   
   ## 3. Hole Filling
   - **Fill small holes:** Closing
   - **Fill all holes:** Special flood-fill based algorithm
   
   ## 4. Feature Extraction
   - **Boundaries:** Morphological gradient
   - **Skeleton:** Iterative thinning (erosion with hit-or-miss)
   - **Convex hull:** Iterative dilation with hit-or-miss
   
   ## 5. Image Segmentation
   - **Watershed:** Based on morphological gradient
   - **Region growing:** Seeds from morphological operations
   
   ## 6. Preprocessing
   - **Smoothing:** Opening or closing
   - **Edge sharpening:** Add morphological gradient
   - **Background subtraction:** Top hat or bottom hat
   
   ## 7. Medical Imaging
   - **Bone segmentation:** Threshold + closing
   - **Vessel enhancement:** Top hat with line structuring elements
   - **Tumor detection:** Multiple morphological operations
   
   ## 8. Quality Control
   - **Defect detection:** Morphological gradient
   - **Size filtering:** Opening with calibrated structuring element
   - **Gap detection:** Closing followed by comparison
   
   # Design Patterns
   
   ## Pattern 1: Noise Removal
   ```clojure
   (defn remove-noise [img mask]
     ;; Opening removes bright noise, closing removes dark noise
     (-> img
         (erode mask)
         (dilate mask)
         (dilate mask)
         (erode mask)))
   ```
   
   ## Pattern 2: Boundary Extraction
   ```clojure
   (defn extract-boundary [img mask]
     ;; Internal boundary: original - erosion
     (let [eroded (erode img mask)]
       (- img eroded)))
   
   (defn extract-boundary-external [img mask]
     ;; External boundary: dilation - original
     (let [dilated (dilate img mask)]
       (- dilated img)))
   ```
   
   ## Pattern 3: Adaptive Structuring Element
   ```clojure
   (defn process-with-scale [img base-size feature-size]
     ;; Scale structuring element based on feature size
     (let [scale (/ feature-size base-size)
           se-size (Math/round (* base-size scale))
           mask (constant 1 se-size se-size)]
       (opening img mask)))
   ```
   
   ## Pattern 4: Iterative Morphology
   ```clojure
   (defn iterative-erode [img mask n]
     ;; Apply erosion n times
     (reduce (fn [acc _] (erode acc mask))
             img
             (range n)))
   ```
   
   # Limitations and Gotchas
   
   1. **Mask Dimensions:**
      - 2D functions require 2D mask (error if 3D mask provided)
      - 3D functions require 3D mask (error if 2D mask provided)
      - Cannot have 4D masks
   
   2. **Input Dimensions:**
      - 2D functions require at least 2D input
      - 3D functions require at least 3D input
   
   3. **Large Masks:**
      - Very large masks (>33×33) can be slow with direct method
      - ArrayFire automatically switches to FFT for large masks
      - FFT method has slight precision differences
   
   4. **Edge Handling:**
      - Implicitly uses zero-padding at borders
      - Edge pixels may have different behavior
      - Consider pre-padding for controlled boundary behavior
   
   5. **Binary Operations:**
      - For binary morphology, use b8 type
      - Non-zero values treated as \"object\"
      - Zero values treated as \"background\"
   
   6. **Memory:**
      - Output size equals input size (not expanded)
      - Temporary arrays created during processing
      - Batch processing more memory-efficient than loops
   
   # Error Handling
   
   Potential errors:
   - **AF_ERR_SIZE:** Mask has wrong dimensions (e.g., 3D mask for 2D operation)
   - **AF_ERR_ARG:** Invalid array handles
   - **AF_ERR_TYPE:** Unsupported type (e.g., complex numbers)
   - **AF_ERR_NO_MEM:** Insufficient GPU memory
   
   # Comparison with Other Operations
   
   ## vs Median Filter
   - **Morphology:** Removes/adds based on structure, uses max/min
   - **Median:** Removes noise based on statistics
   - **Trade-off:** Morphology preserves sharp edges better
   
   ## vs Gaussian Blur
   - **Morphology:** Non-linear, structure-based
   - **Gaussian:** Linear, weighted average
   - **Trade-off:** Morphology better for binary/segmentation, Gaussian for smoothing
   
   ## vs Convolution
   - **Morphology:** Max/min operations (order statistics)
   - **Convolution:** Weighted sum (linear operation)
   - **Trade-off:** Morphology for shape analysis, convolution for filtering
   
   # Best Practices
   
   1. **Start Small:** Use small structuring elements (3×3, 5×5) first
   
   2. **Compose Operations:** Build complex operations from erosion/dilation
   
   3. **Use Appropriate Shape:** 
      - Square: General purpose
      - Cross: 4-connectivity
      - Disk: Isotropic (equal in all directions)
      - Line: Directional features
   
   4. **Consider Iterative Operations:** Multiple small operations often better
      than one large operation
   
   5. **Binary Preprocessing:** Threshold to binary before morphology for
      cleaner results
   
   6. **Batch Processing:** Process multiple images together for efficiency
   
   7. **Monitor Size Changes:** Opening/closing preserves approximate size,
      but erosion/dilation changes it
   
   8. **Use 3D Operations:** For volumetric data, use 3D operations for true
      volumetric processing (not slice-by-slice)
   
   # See Also
   
   - Convolution functions for linear filtering
   - Median filter for non-linear noise removal
   - Min/max filters (similar to erosion/dilation with all-ones mask)
   - Connected components for region analysis
   - Watershed for segmentation"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; af_err af_dilate(af_array *out, const af_array in, const af_array mask)
(defcfn af-dilate
  "Morphological dilation (2D maximum filter with structuring element).
   
   Dilation is a fundamental morphological operation that expands bright regions
   in an image. It computes the maximum value over the neighborhood defined by
   the structuring element (mask).
   
   Parameters:
   - out: out pointer for dilated image/volume
   - in: input image array
     * Minimum dimensions: 2D ([width, height, channels, batch])
     * Can be grayscale or color (processed per-channel)
     * Can include batch dimension for processing multiple images
   - mask: structuring element (neighborhood window)
     * Must be 2D ([mask_width, mask_height])
     * Typical sizes: 3×3, 5×5, 7×7
     * Non-zero values define the neighborhood shape
     * Usually all ones, but can be weighted
   
   # Mathematical Definition
   
   For each pixel position (x, y):
   ```
   Output(x,y) = max{Input(x+i, y+j) | Mask(i,j) ≠ 0}
   ```
   
   where (i,j) ranges over the mask dimensions.
   
   # Visual Effect
   
   **Bright regions expand:**
   - White objects grow larger
   - Small dark gaps are filled
   - Object boundaries move outward
   - Nearby objects may merge
   
   **Example transformation (binary image):**
   ```
   Input:          Mask:       Output:
   0 0 0 0 0      1 1 1        0 0 0 0 0
   0 0 1 0 0      1 1 1   →    0 1 1 1 0
   0 1 1 1 0      1 1 1        1 1 1 1 1
   0 0 1 0 0                   0 1 1 1 0
   0 0 0 0 0                   0 0 1 0 0
   ```
   
   # Algorithm
   
   For small masks (typically ≤17×17):
   ```
   For each output pixel (x, y):
     max_val = -∞
     For each mask element (i, j) where Mask(i,j) ≠ 0:
       offset_x = x + i - mask_center_x
       offset_y = y + j - mask_center_y
       if within_bounds(offset_x, offset_y):
         max_val = max(max_val, Input(offset_x, offset_y))
     Output(x,y) = max_val
   ```
   
   For large masks (>17×17), ArrayFire may use FFT-based approach:
   - Convert to frequency domain
   - Perform equivalent operation
   - Convert back to spatial domain
   - Much faster for large kernels
   
   # Effects and Applications
   
   **Geometric Effects:**
   - Expands foreground (bright) regions
   - Shrinks background (dark) regions
   - Fills small holes (smaller than structuring element)
   - Connects nearby components
   - Smooths object contours (removes concave corners)
   
   **Common Uses:**
   - **Hole filling:** Close small gaps in objects
   - **Object connection:** Bridge nearby components
   - **Noise removal:** Part of closing operation (dilate → erode)
   - **Feature enhancement:** Emphasize bright features
   - **Segmentation:** Expand seed regions
   
   # Structuring Element Selection
   
   **3×3 Square (most common):**
   ```
   [1 1 1]
   [1 1 1]
   [1 1 1]
   ```
   - Fast computation
   - Uniform expansion
   - 8-connectivity
   
   **5×5 Square:**
   ```
   [1 1 1 1 1]
   [1 1 1 1 1]
   [1 1 1 1 1]
   [1 1 1 1 1]
   [1 1 1 1 1]
   ```
   - Stronger effect
   - Fills larger gaps
   
   **3×3 Cross (4-connectivity):**
   ```
   [0 1 0]
   [1 1 1]
   [0 1 0]
   ```
   - Directional control
   - Less aggressive than square
   
   **Disk (isotropic):**
   - Circular structuring element
   - Equal expansion in all directions
   - Natural for round objects
   
   # Performance
   
   - **Complexity:** O(W × H × M_w × M_h) for small masks
     - W, H: Image dimensions
     - M_w, M_h: Mask dimensions
   - **GPU Acceleration:** 20-100× faster than CPU
   - **Batch Processing:** Processes all images in parallel
   
   Typical timings (NVIDIA GPU):
   - 512×512 image, 3×3 mask: ~0.08 ms
   - 512×512 image, 7×7 mask: ~0.2 ms
   - 1024×1024 image, 11×11 mask: ~0.6 ms
   - Batch of 10 images: ~1 ms total (not 10× single image)
   
   For masks >17×17, FFT-based method is faster:
   - 512×512 image, 33×33 mask: ~0.5 ms (FFT) vs ~3 ms (direct)
   
   # Type Support
   
   **Input types:**
   - Floating point: f32, f64
   - Signed integers: s32, s16, s8
   - Unsigned integers: u32, u16, u8
   - Boolean: b8 (binary morphology)
   
   **Output type:** Same as input type
   
   **Note:** For binary morphology, use b8 type with 0 (background) and 1 (object).
   
   # Example 1: Basic Dilation
   ```clojure
   ;; Dilate binary image to fill small gaps
   (let [binary-img (> img threshold)  ; Threshold to binary
         mask (constant 1 3 3)          ; 3×3 all-ones mask
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-dilate out-ptr binary-img mask)
         dilated (mem/read-pointer out-ptr ::mem/pointer)]
     dilated)
   
   ;; Result: Small gaps filled, objects slightly larger
   ```
   
   # Example 2: Closing (Hole Filling)
   ```clojure
   ;; Morphological closing: fill holes and smooth contours
   (defn closing [img mask]
     (let [;; Step 1: Dilate (expand)
           dilated-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-dilate dilated-ptr img mask)
           dilated (mem/read-pointer dilated-ptr ::mem/pointer)
           ;; Step 2: Erode (shrink back)
           closed-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-erode closed-ptr dilated mask)]
       (mem/read-pointer closed-ptr ::mem/pointer)))
   
   ;; Usage:
   (let [mask (constant 1 5 5)]
     (closing noisy-binary-img mask))
   
   ;; Result: Holes filled, overall size preserved
   ```
   
   # Example 3: Object Connection
   ```clojure
   ;; Connect nearby objects that should be one component
   (let [segmented (threshold img)
         ;; Use elongated mask to bridge horizontal gaps
         h-line (constant 1 7 1)  ; Horizontal line
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-dilate out-ptr segmented h-line)
         connected (mem/read-pointer out-ptr ::mem/pointer)]
     connected)
   
   ;; Result: Objects separated by small horizontal gaps now connected
   ```
   
   # Example 4: Batch Processing
   ```clojure
   ;; Process multiple images simultaneously
   (let [;; Batch of images: [512, 512, 1, 20]
         img-batch (stack-images images 3)
         mask (constant 1 5 5)
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-dilate out-ptr img-batch mask)
         results (mem/read-pointer out-ptr ::mem/pointer)]
     ;; Results: [512, 512, 1, 20] - all dilated
     (split-images results 3))
   
   ;; Result: All 20 images processed in ~0.5 ms
   ```
   
   # Example 5: Morphological Gradient (Edge Detection)
   ```clojure
   ;; Extract edges using morphological gradient
   (defn morph-gradient [img mask]
     (let [dilated-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-dilate dilated-ptr img mask)
           dilated (mem/read-pointer dilated-ptr ::mem/pointer)
           eroded-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-erode eroded-ptr img mask)
           eroded (mem/read-pointer eroded-ptr ::mem/pointer)]
       ;; Gradient = dilation - erosion
       (- dilated eroded)))
   
   ;; Usage:
   (let [mask (constant 1 3 3)]
     (morph-gradient img mask))
   
   ;; Result: Edges highlighted (boundary thickness = mask size)
   ```
   
   # Example 6: Multi-scale Processing
   ```clojure
   ;; Extract features at different scales
   (defn multi-scale-dilation [img scales]
     (for [size scales]
       (let [mask (constant 1 size size)
             out-ptr (mem/alloc-pointer ::mem/pointer)
             _ (af-dilate out-ptr img mask)]
         {:scale size
          :result (mem/read-pointer out-ptr ::mem/pointer)})))
   
   ;; Usage:
   (multi-scale-dilation img [3 7 11 15])
   
   ;; Result: 4 images showing features at different scales
   ;; - size 3: Small features
   ;; - size 7: Medium features  
   ;; - size 11: Large features
   ;; - size 15: Very large features
   ```
   
   # Example 7: Medical Imaging - Vessel Enhancement
   ```clojure
   ;; Enhance vessel structures in medical images
   (defn enhance-vessels [img]
     (let [;; Create oriented line structuring elements
           h-line (constant 1 1 11)    ; Horizontal
           v-line (constant 1 11 1)    ; Vertical
           ;; Dilate with each orientation
           h-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-dilate h-ptr img h-line)
           h-enhanced (mem/read-pointer h-ptr ::mem/pointer)
           v-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-dilate v-ptr img v-line)
           v-enhanced (mem/read-pointer v-ptr ::mem/pointer)]
       ;; Combine results
       (max h-enhanced v-enhanced)))
   
   ;; Result: Vessel structures enhanced
   ```
   
   # Common Patterns
   
   **Pattern: Safe Dilation with Checking**
   ```clojure
   (defn safe-dilate [img mask]
     (when (and (valid-array? img)
                (valid-array? mask)
                (= 2 (get-ndims mask)))
       (let [out-ptr (mem/alloc-pointer ::mem/pointer)
             err (af-dilate out-ptr img mask)]
         (when (= err AF_SUCCESS)
           (mem/read-pointer out-ptr ::mem/pointer)))))
   ```
   
   **Pattern: Iterative Dilation**
   ```clojure
   (defn iterative-dilate [img mask n]
     (reduce (fn [acc _]
               (let [out-ptr (mem/alloc-pointer ::mem/pointer)
                     _ (af-dilate out-ptr acc mask)]
                 (mem/read-pointer out-ptr ::mem/pointer)))
             img
             (range n)))
   ```
   
   **Pattern: Conditional Dilation**
   ```clojure
   (defn dilate-until-connected [img mask max-iters]
     (loop [current img
            iter 0]
       (if (or (connected? current) (>= iter max-iters))
         current
         (let [out-ptr (mem/alloc-pointer ::mem/pointer)
               _ (af-dilate out-ptr current mask)]
           (recur (mem/read-pointer out-ptr ::mem/pointer)
                  (inc iter))))))
   ```
   
   # When to Use
   
   **Use dilation when you need to:**
   - Fill holes in objects
   - Connect nearby components
   - Expand regions
   - Smooth concave boundaries
   - Part of closing operation
   
   **Don't use dilation when:**
   - You need to preserve exact sizes (use other techniques)
   - Small features must be preserved (they'll be expanded)
   - You need linear smoothing (use Gaussian blur)
   - Only noise removal needed without size change (use median filter)
   
   # Gotchas
   
   1. **Size Changes:** Dilation makes objects larger - if size is critical,
      follow with erosion (closing)
   
   2. **Mask Must Be 2D:** Using 3D mask will error - use af-dilate3 instead
   
   3. **Edge Effects:** Pixels at image borders may behave differently due to
      zero-padding
   
   4. **Merging Objects:** Nearby objects may merge - adjust mask size if this
      is undesirable
   
   5. **Type Consistency:** Mask and input should have compatible types
   
   6. **Memory:** Creates output array same size as input
   
   # Errors
   
   - AF_ERR_SIZE: Mask is not 2D (wrong dimensions)
   - AF_ERR_ARG: Invalid array handle (null pointer)
   - AF_ERR_TYPE: Unsupported type (e.g., complex numbers)
   - AF_ERR_NO_MEM: Insufficient GPU memory
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-erode: Morphological erosion (dual operation)
   - af-dilate3: 3D dilation for volumetric data
   - af-maxfilt: Maximum filter (dilation with all-ones mask)
   - Opening/closing: Composite operations"
  "af_dilate" [::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_erode(af_array *out, const af_array in, const af_array mask)
(defcfn af-erode
  "Morphological erosion (2D minimum filter with structuring element).
   
   Erosion is a fundamental morphological operation that shrinks bright regions
   in an image. It computes the minimum value over the neighborhood defined by
   the structuring element (mask).
   
   Parameters:
   - out: out pointer for eroded image
   - in: input image array
     * Minimum dimensions: 2D ([width, height, channels, batch])
     * Can be grayscale or color (processed per-channel)
     * Can include batch dimension
   - mask: structuring element (neighborhood window)
     * Must be 2D ([mask_width, mask_height])
     * Typical sizes: 3×3, 5×5, 7×7
     * Non-zero values define the neighborhood
     * Usually all ones
   
   # Mathematical Definition
   
   For each pixel position (x, y):
   ```
   Output(x,y) = min{Input(x+i, y+j) | Mask(i,j) ≠ 0}
   ```
   
   where (i,j) ranges over the mask dimensions.
   
   # Visual Effect
   
   **Bright regions shrink:**
   - White objects become smaller
   - Dark gaps expand
   - Object boundaries move inward
   - Thin protrusions are removed
   
   **Example transformation (binary image):**
   ```
   Input:          Mask:       Output:
   0 0 0 0 0      1 1 1        0 0 0 0 0
   0 1 1 1 0      1 1 1   →    0 0 0 0 0
   0 1 1 1 0      1 1 1        0 0 1 0 0
   0 1 1 1 0                   0 0 0 0 0
   0 0 0 0 0                   0 0 0 0 0
   ```
   
   # Algorithm
   
   Similar to dilation but using minimum instead of maximum:
   ```
   For each output pixel (x, y):
     min_val = +∞
     For each mask element (i, j) where Mask(i,j) ≠ 0:
       offset_x = x + i - mask_center_x
       offset_y = y + j - mask_center_y
       if within_bounds(offset_x, offset_y):
         min_val = min(min_val, Input(offset_x, offset_y))
     Output(x,y) = min_val
   ```
   
   # Effects and Applications
   
   **Geometric Effects:**
   - Shrinks foreground (bright) regions
   - Expands background (dark) regions
   - Removes small bright objects (smaller than structuring element)
   - Separates touching objects
   - Smooths object contours (removes convex protrusions)
   
   **Common Uses:**
   - **Noise removal:** Remove small bright spots
   - **Object separation:** Separate touching components
   - **Feature removal:** Eliminate thin protrusions
   - **Opening:** Erosion followed by dilation
   - **Boundary detection:** Subtract eroded from original
   
   # Duality with Dilation
   
   Erosion and dilation are dual operations:
   ```
   erode(img, mask) = NOT(dilate(NOT(img), mask))
   ```
   
   This means eroding foreground is equivalent to dilating background.
   
   # Performance
   
   Same as af-dilate:
   - **Complexity:** O(W × H × M_w × M_h) for small masks
   - **GPU Acceleration:** 20-100× speedup
   - **Batch Processing:** Highly parallel
   
   # Type Support
   
   Same as af-dilate:
   - f32, f64, s32, u32, s16, u16, s8, u8, b8
   - Output type matches input type
   
   # Example 1: Basic Erosion
   ```clojure
   ;; Remove small bright noise from binary image
   (let [noisy-img (> img threshold)
         mask (constant 1 3 3)
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-erode out-ptr noisy-img mask)
         clean (mem/read-pointer out-ptr ::mem/pointer)]
     clean)
   
   ;; Result: Small bright spots removed, objects slightly smaller
   ```
   
   # Example 2: Opening (Noise Removal)
   ```clojure
   ;; Morphological opening: remove bright noise while preserving size
   (defn opening [img mask]
     (let [;; Step 1: Erode (remove noise)
           eroded-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-erode eroded-ptr img mask)
           eroded (mem/read-pointer eroded-ptr ::mem/pointer)
           ;; Step 2: Dilate (restore size)
           opened-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-dilate opened-ptr eroded mask)]
       (mem/read-pointer opened-ptr ::mem/pointer)))
   
   ;; Usage:
   (let [mask (constant 1 5 5)]
     (opening noisy-img mask))
   
   ;; Result: Small bright objects removed, large objects preserved
   ```
   
   # Example 3: Object Separation
   ```clojure
   ;; Separate touching objects in binary image
   (let [touching-objects (segment-image img)
         ;; Erode to separate
         mask (constant 1 5 5)
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-erode out-ptr touching-objects mask)
         separated (mem/read-pointer out-ptr ::mem/pointer)]
     ;; Now can label individual objects
     (label-components separated))
   
   ;; Result: Touching objects now separated
   ```
   
   # Example 4: Internal Boundary Extraction
   ```clojure
   ;; Extract internal boundary of objects
   (defn internal-boundary [img mask]
     (let [eroded-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-erode eroded-ptr img mask)
           eroded (mem/read-pointer eroded-ptr ::mem/pointer)]
       ;; Boundary = original - eroded
       (- img eroded)))
   
   ;; Usage:
   (let [mask (constant 1 3 3)]
     (internal-boundary binary-img mask))
   
   ;; Result: Only internal boundary pixels remain
   ```
   
   # Example 5: Skeleton (Simplified)
   ```clojure
   ;; Iterative thinning (simplified skeletonization)
   (defn simple-skeleton [img mask max-iters]
     (loop [current img
            iter 0]
       (let [eroded-ptr (mem/alloc-pointer ::mem/pointer)
             _ (af-erode eroded-ptr current mask)
             eroded (mem/read-pointer eroded-ptr ::mem/pointer)
             ;; Check if still shrinking
             changed (not= current eroded)]
         (if (or (not changed) (>= iter max-iters))
           current
           (recur eroded (inc iter))))))
   
   ;; Result: Thinned representation of objects
   ```
   
   # Example 6: Defect Detection
   ```clojure
   ;; Detect defects by comparing expected vs actual after erosion
   (defn detect-defects [actual expected]
     (let [mask (constant 1 3 3)
           ;; Erode both images
           actual-eroded-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-erode actual-eroded-ptr actual mask)
           actual-eroded (mem/read-pointer actual-eroded-ptr ::mem/pointer)
           expected-eroded-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-erode expected-eroded-ptr expected mask)
           expected-eroded (mem/read-pointer expected-eroded-ptr ::mem/pointer)]
       ;; Defects show up as differences
       (abs (- actual-eroded expected-eroded))))
   
   ;; Result: Defects highlighted
   ```
   
   # Example 7: Medical Imaging - Bone Segmentation
   ```clojure
   ;; Segment bone structures using erosion
   (defn segment-bone [ct-scan]
     (let [;; Threshold to get bone candidates
           bone-mask (> ct-scan bone-threshold)
           ;; Erode to remove thin false positives
           mask (constant 1 5 5)
           eroded-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-erode eroded-ptr bone-mask mask)
           clean-bone (mem/read-pointer eroded-ptr ::mem/pointer)
           ;; Dilate back to restore size
           final-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-dilate final-ptr clean-bone mask)]
       (mem/read-pointer final-ptr ::mem/pointer)))
   
   ;; Result: Clean bone segmentation
   ```
   
   # Common Patterns
   
   **Pattern: Noise Removal Pipeline**
   ```clojure
   (defn remove-salt-pepper [img]
     (let [mask (constant 1 3 3)]
       (-> img
           (opening mask)   ; Remove salt (bright noise)
           (closing mask)))) ; Remove pepper (dark noise)
   ```
   
   **Pattern: Size Filtering**
   ```clojure
   (defn filter-small-objects [img min-size]
     (let [;; Size of structuring element related to min feature size
           mask-size (int (Math/sqrt min-size))
           mask (constant 1 mask-size mask-size)]
       (opening img mask)))
   ```
   
   **Pattern: Gap Detection**
   ```clojure
   (defn detect-gaps [img mask-size]
     (let [mask (constant 1 mask-size mask-size)
           ;; Close gaps
           closed (closing img mask)]
       ;; Gaps = closed - original
       (- closed img)))
   ```
   
   # When to Use
   
   **Use erosion when you need to:**
   - Remove small bright features (noise)
   - Separate touching objects
   - Thin object boundaries
   - Part of opening operation
   - Detect boundaries
   
   **Don't use erosion when:**
   - Small features must be preserved (they'll be removed)
   - Objects are already thin (may disappear completely)
   - You need exact size preservation alone
   - Linear operations suffice
   
   # Gotchas
   
   1. **Object Disappearance:** Small objects may completely disappear
   2. **Over-erosion:** Too many iterations or too large mask removes everything
   3. **Mask Must Be 2D:** Using 3D mask will error
   4. **Separation May Be Excessive:** Objects may break into multiple pieces
   5. **Edge Effects:** Border pixels affected by zero-padding
   
   # Errors
   
   - AF_ERR_SIZE: Mask is not 2D
   - AF_ERR_ARG: Invalid array handle
   - AF_ERR_TYPE: Unsupported type
   - AF_ERR_NO_MEM: Insufficient memory
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-dilate: Morphological dilation (dual operation)
   - af-erode3: 3D erosion for volumetric data
   - af-minfilt: Minimum filter (erosion with all-ones mask)"
  "af_erode" [::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_dilate3(af_array *out, const af_array in, const af_array mask)
(defcfn af-dilate3
  "Morphological dilation for 3D volumes (3D maximum filter).
   
   3D dilation extends the 2D dilation operation to volumetric data, computing
   the maximum value over a 3D neighborhood defined by a 3D structuring element.
   
   Parameters:
   - out: out pointer for dilated volume
   - in: input 3D volume array
     * Minimum dimensions: 3D ([width, height, depth, batch])
     * Can include batch dimension for multiple volumes
   - mask: 3D structuring element
     * Must be 3D ([mask_width, mask_height, mask_depth])
     * Typical sizes: 3×3×3, 5×5×5
     * Non-zero values define 3D neighborhood
   
   # Mathematical Definition
   
   For each voxel position (x, y, z):
   ```
   Output(x,y,z) = max{Input(x+i, y+j, z+k) | Mask(i,j,k) ≠ 0}
   ```
   
   where (i,j,k) ranges over the 3D mask dimensions.
   
   # 3D Structuring Elements
   
   **3×3×3 Cube (most common):**
   - All 27 voxels set to 1
   - Uniform expansion in all directions
   - 26-connectivity
   
   **3D Cross:**
   - Only 6-connected neighbors (faces)
   - Center plane plus neighbors above/below
   
   **3D Sphere:**
   - Approximately spherical shape
   - Isotropic (uniform in all directions)
   
   # Applications
   
   **Medical Imaging:**
   - Vessel dilation in CT/MRI
   - Bone structure analysis
   - Tumor segmentation
   - Gap filling in 3D reconstructions
   
   **Materials Science:**
   - Pore analysis in 3D microscopy
   - Grain boundary detection
   - Defect characterization
   
   **Seismic Data:**
   - Horizon tracking
   - Fault detection
   - 3D structural analysis
   
   # Performance
   
   - **Complexity:** O(W × H × D × M_w × M_h × M_d)
     - W, H, D: Volume dimensions
     - M_w, M_h, M_d: Mask dimensions
   - **GPU Acceleration:** Crucial for 3D (100-1000× speedup)
   - **Memory:** Requires 3D volume in GPU memory
   
   Typical timings (NVIDIA GPU):
   - 128×128×128 volume, 3×3×3 mask: ~0.8 ms
   - 256×256×256 volume, 5×5×5 mask: ~8 ms
   - Batch of 4 volumes: ~4× single volume
   
   # Example 1: 3D Hole Filling
   ```clojure
   ;; Fill holes in 3D segmented volume
   (let [segmented-volume (load-volume \"scan.nii\")
         mask (constant 1 5 5 5)  ; 5×5×5 cube
         ;; Closing: dilate then erode
         dilated-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-dilate3 dilated-ptr segmented-volume mask)
         dilated (mem/read-pointer dilated-ptr ::mem/pointer)
         closed-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-erode3 closed-ptr dilated mask)]
     (mem/read-pointer closed-ptr ::mem/pointer))
   
   ;; Result: 3D holes filled, structure preserved
   ```
   
   # Example 2: Vessel Enhancement
   ```clojure
   ;; Enhance blood vessels in CT angiography
   (let [ct-volume (load-ct \"angio.dcm\")
         ;; Use small isotropic mask
         mask (constant 1 3 3 3)
         ;; Top-hat: original - opening
         eroded-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-erode3 eroded-ptr ct-volume mask)
         eroded (mem/read-pointer eroded-ptr ::mem/pointer)
         opened-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-dilate3 opened-ptr eroded mask)
         opened (mem/read-pointer opened-ptr ::mem/pointer)]
     ;; Vessels = original - opened
     (- ct-volume opened))
   
   ;; Result: Vessel structures enhanced
   ```
   
   # Example 3: 3D Connected Components
   ```clojure
   ;; Connect nearby voxels in 3D
   (let [sparse-volume (threshold-3d scan)
         mask (constant 1 7 7 7)  ; Larger mask to bridge gaps
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-dilate3 out-ptr sparse-volume mask)
         connected (mem/read-pointer out-ptr ::mem/pointer)]
     ;; Now label connected components
     (label-3d-components connected))
   
   ;; Result: Nearby voxels connected into single components
   ```
   
   # Gotchas
   
   1. **Memory Usage:** 3D volumes consume significant memory
   2. **Mask Must Be 3D:** Using 2D mask will error
   3. **Computation Time:** Much slower than 2D (cubic complexity)
   4. **Edge Effects:** 3D borders affected by zero-padding
   
   # Errors
   
   - AF_ERR_SIZE: Mask is not 3D, or input not at least 3D
   - AF_ERR_ARG: Invalid array handle
   - AF_ERR_TYPE: Unsupported type
   - AF_ERR_NO_MEM: Insufficient GPU memory (common with large volumes)
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-erode3: 3D morphological erosion
   - af-dilate: 2D dilation"
  "af_dilate3" [::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_erode3(af_array *out, const af_array in, const af_array mask)
(defcfn af-erode3
  "Morphological erosion for 3D volumes (3D minimum filter).
   
   3D erosion extends the 2D erosion operation to volumetric data, computing
   the minimum value over a 3D neighborhood defined by a 3D structuring element.
   
   Parameters:
   - out: out pointer for eroded volume
   - in: input 3D volume array
     * Minimum dimensions: 3D ([width, height, depth, batch])
     * Can include batch dimension
   - mask: 3D structuring element
     * Must be 3D ([mask_width, mask_height, mask_depth])
     * Typical sizes: 3×3×3, 5×5×5
     * Non-zero values define neighborhood
   
   # Mathematical Definition
   
   For each voxel position (x, y, z):
   ```
   Output(x,y,z) = min{Input(x+i, y+j, z+k) | Mask(i,j,k) ≠ 0}
   ```
   
   # Effects
   
   - Shrinks bright regions in 3D
   - Removes small bright 3D structures
   - Separates touching 3D objects
   - Smooths 3D surfaces
   
   # Applications
   
   **Medical Imaging:**
   - Noise removal in 3D scans
   - Object separation
   - Surface smoothing
   - Feature extraction
   
   **Materials Science:**
   - Pore size analysis
   - Grain separation
   - Surface characterization
   
   # Performance
   
   Same considerations as af-dilate3:
   - O(W × H × D × M_w × M_h × M_d) complexity
   - GPU essential for acceptable performance
   - Memory intensive
   
   # Example: 3D Opening
   ```clojure
   ;; Remove 3D noise while preserving structure
   (defn opening-3d [volume mask]
     (let [;; Erode first
           eroded-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-erode3 eroded-ptr volume mask)
           eroded (mem/read-pointer eroded-ptr ::mem/pointer)
           ;; Then dilate
           opened-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-dilate3 opened-ptr eroded mask)]
       (mem/read-pointer opened-ptr ::mem/pointer)))
   
   (let [mask (constant 1 5 5 5)]
     (opening-3d noisy-volume mask))
   
   ;; Result: Small 3D noise removed, structures preserved
   ```
   
   # Gotchas
   
   1. **Slow for Large Volumes:** Consider downsampling first
   2. **Memory Intensive:** May need to process in chunks
   3. **Over-erosion:** Can make thin structures disappear
   4. **Mask Must Be 3D:** Using 2D mask will error
   
   # Errors
   
   - AF_ERR_SIZE: Mask is not 3D, or input not at least 3D
   - AF_ERR_ARG: Invalid array handle
   - AF_ERR_TYPE: Unsupported type
   - AF_ERR_NO_MEM: Insufficient GPU memory
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-dilate3: 3D morphological dilation
   - af-erode: 2D erosion"
  "af_erode3" [::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)
