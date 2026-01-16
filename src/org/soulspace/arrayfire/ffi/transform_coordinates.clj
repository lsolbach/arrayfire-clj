(ns org.soulspace.arrayfire.ffi.transform-coordinates
  "Bindings for the ArrayFire coordinate transformation functions.
   
   Coordinate transformation is a fundamental operation in computer vision
   and image processing for converting image coordinates from one coordinate
   system to another using homogeneous transformation matrices.
   
   Mathematical Foundation:
   
   **Homogeneous Coordinates**:
   In 2D image processing, homogeneous coordinates extend 2D points [x, y]
   to 3D points [x, y, 1], enabling representation of translation, rotation,
   scaling, and perspective transformations as matrix multiplications.
   
   A 2D point (x, y) in homogeneous coordinates: [x, y, 1]ᵀ
   
   **Transformation Matrix**:
   A 3×3 transformation matrix T represents general 2D transformations:
   
       ┌           ┐
       │ a  b  tx │  where:
   T = │ c  d  ty │  - [a b; c d] is the 2×2 linear transformation (rotation/scale/shear)
       │ p  q  s  │  - [tx, ty] is the translation vector
       └           ┘  - [p, q, s] enables perspective projection
   
   **Forward Transformation**:
   For a point [x, y] in the source image, the transformed point [x', y'] is:
   
       ┌    ┐       ┌           ┐ ┌   ┐
       │ x' │       │ a  b  tx │ │ x │
       │ y' │ = T × │ c  d  ty │ │ y │
       │ w  │       │ p  q  s  │ │ 1 │
       └    ┘       └           ┘ └   ┘
   
   Then: x' = x'/w, y' = y'/w (perspective division)
   
   **What This Function Does**:
   af_transform_coordinates computes the corner coordinates of the
   transformed image given:
   - Transformation matrix T (3×3)
   - Input image dimensions d0 (height) and d1 (width)
   
   It transforms the four corners of the input image:
   - (0, 0)       → top-left corner
   - (0, d0)      → bottom-left corner
   - (d1, d0)     → bottom-right corner
   - (d1, 0)      → top-right corner
   
   The output is a [3, 4] array where:
   - First row: x-coordinates of transformed corners
   - Second row: y-coordinates of transformed corners
   - Third row: homogeneous coordinates (typically 1 after division)
   
   Common Transformations:
   
   **1. Identity (No transformation)**:
   ```
   ┌         ┐
   │ 1 0 0  │
   │ 0 1 0  │
   │ 0 0 1  │
   └         ┘
   ```
   
   **2. Translation by (tx, ty)**:
   ```
   ┌          ┐
   │ 1 0 tx  │
   │ 0 1 ty  │
   │ 0 0 1   │
   └          ┘
   ```
   
   **3. Scaling by (sx, sy)**:
   ```
   ┌          ┐
   │ sx 0  0  │
   │ 0  sy 0  │
   │ 0  0  1  │
   └          ┘
   ```
   
   **4. Rotation by θ (radians)**:
   ```
   ┌                    ┐
   │ cos(θ) -sin(θ) 0  │
   │ sin(θ)  cos(θ) 0  │
   │   0       0    1  │
   └                    ┘
   ```
   
   **5. Rotation about center (cx, cy)**:
   Composed transformation:
   - Translate to origin: T₁ = translate(-cx, -cy)
   - Rotate: R = rotate(θ)
   - Translate back: T₂ = translate(cx, cy)
   - Combined: T = T₂ × R × T₁
   
   **6. Shear (horizontal shear by factor sh)**:
   ```
   ┌          ┐
   │ 1  sh 0  │
   │ 0  1  0  │
   │ 0  0  1  │
   └          ┘
   ```
   
   **7. Perspective (homography)**:
   ```
   ┌          ┐
   │ a  b  tx │
   │ c  d  ty │  where p, q ≠ 0 creates perspective distortion
   │ p  q  s  │
   └          ┘
   ```
   
   Algorithm:
   
   The function internally:
   1. Creates a [4, 3] matrix of corner homogeneous coordinates:
      ```
      ┌                    ┐
      │ 0    0   d1   d1  │  (x-coordinates)
      │ 0   d0   d0    0  │  (y-coordinates)
      │ 1    1    1    1  │  (homogeneous)
      └                    ┘
      ```
   
   2. Multiplies by transformation matrix: result = T × corners
   
   3. Performs perspective division: [x/w, y/w] for each corner
   
   4. Returns [3, 4] array with transformed coordinates
   
   Use Cases:
   
   **Computer Vision**:
   - Image registration and alignment
     ```clojure
     ;; Compute transformed corners for image registration
     (let [H (estimate-homography src-pts dst-pts)  ; 3×3 homography
           [h w] (image-dimensions src-img)
           coords-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-transform-coordinates coords-ptr H h w)
           corners (mem/read-pointer coords-ptr ::mem/pointer)
           ;; corners[0,:] = x-coords, corners[1,:] = y-coords
           bounds (compute-bounding-box corners)]
       bounds)
     ```
   
   - Camera calibration
   - Augmented reality (AR) overlay positioning
   - Panorama stitching (determine output canvas size)
   
   **Image Warping**:
   - Determine output image size before transformation
     ```clojure
     ;; Calculate required output dimensions for transformation
     (let [transform-matrix (create-rotation-matrix angle)
           [src-h src-w] (get-image-size src-img)
           corners-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-transform-coordinates corners-ptr transform-matrix 
                                       src-h src-w)
           corners (mem/read-pointer corners-ptr ::mem/pointer)
           x-coords (af-row corners 0)
           y-coords (af-row corners 1)
           min-x (af-min-all x-coords)
           max-x (af-max-all x-coords)
           min-y (af-min-all y-coords)
           max-y (af-max-all y-coords)
           out-w (long (Math/ceil (- max-x min-x)))
           out-h (long (Math/ceil (- max-y min-y)))]
       {:output-width out-w
        :output-height out-h
        :offset-x min-x
        :offset-y min-y})
     ```
   
   - Perspective correction
   - Lens distortion correction
   
   **Document Processing**:
   - Document deskewing (rotation correction)
   - Page boundary detection after transformation
   - Scan alignment
     ```clojure
     ;; Determine canvas size for deskewed document
     (let [skew-angle (detect-skew-angle doc-img)
           correction-matrix (create-rotation-matrix (- skew-angle))
           [h w] (get-dimensions doc-img)
           corners-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-transform-coordinates corners-ptr correction-matrix h w)
           transformed-corners (mem/read-pointer corners-ptr ::mem/pointer)
           canvas-size (compute-canvas-size transformed-corners)]
       canvas-size)
     ```
   
   **Medical Imaging**:
   - Image registration (align scans from different times/modalities)
   - Atlas alignment
   - Multi-modal image fusion
   
   **Robotics and Mapping**:
   - Coordinate frame transformations
   - SLAM (Simultaneous Localization and Mapping)
   - Sensor fusion (camera to world coordinates)
   
   Common Patterns:
   
   **Pattern 1: Calculate Output Canvas Size**
   ```clojure
   ;; Determine minimum bounding box for transformed image
   (defn calculate-output-dimensions
     [transform-matrix input-height input-width]
     (let [coords-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-transform-coordinates coords-ptr transform-matrix 
                                       input-height input-width)
           corners (mem/read-pointer coords-ptr ::mem/pointer)
           x-coords (af-row corners 0)
           y-coords (af-row corners 1)
           ;; Find bounding box
           min-x (af-min-all x-coords)
           max-x (af-max-all x-coords)
           min-y (af-min-all y-coords)
           max-y (af-max-all y-coords)
           ;; Calculate dimensions (rounded up)
           width (long (Math/ceil (- max-x min-x)))
           height (long (Math/ceil (- max-y min-y)))]
       {:width width
        :height height
        :offset-x min-x
        :offset-y min-y
        :corners corners}))
   ```
   
   **Pattern 2: Compose Transformations**
   ```clojure
   ;; Chain multiple transformations: rotate then translate
   (defn rotate-and-translate
     [img angle tx ty]
     (let [[h w] (get-dimensions img)
           ;; Create rotation matrix
           rot-matrix (create-rotation-matrix-3x3 angle)
           ;; Create translation matrix
           trans-matrix (create-translation-matrix-3x3 tx ty)
           ;; Compose: T = Translation × Rotation
           composed (af-matmul trans-matrix rot-matrix)
           ;; Get transformed corners
           corners-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-transform-coordinates corners-ptr composed h w)
           corners (mem/read-pointer corners-ptr ::mem/pointer)]
       {:composed-transform composed
        :transformed-corners corners}))
   ```
   
   **Pattern 3: Rotation About Center**
   ```clojure
   ;; Rotate image about its center point
   (defn rotate-about-center
     [img angle]
     (let [[h w] (get-dimensions img)
           cx (/ w 2.0)
           cy (/ h 2.0)
           ;; T = Translate(cx,cy) × Rotate(angle) × Translate(-cx,-cy)
           T1 (create-translation-matrix-3x3 (- cx) (- cy))
           R  (create-rotation-matrix-3x3 angle)
           T2 (create-translation-matrix-3x3 cx cy)
           ;; Compose transformations
           temp (af-matmul R T1)
           full-transform (af-matmul T2 temp)
           ;; Get output bounds
           corners-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-transform-coordinates corners-ptr full-transform h w)
           corners (mem/read-pointer corners-ptr ::mem/pointer)]
       {:transform full-transform :corners corners}))
   ```
   
   **Pattern 4: Determine If Clipping Needed**
   ```clojure
   ;; Check if transformed image exceeds target canvas
   (defn needs-clipping?
     [transform-matrix src-h src-w target-h target-w]
     (let [corners-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-transform-coordinates corners-ptr transform-matrix 
                                       src-h src-w)
           corners (mem/read-pointer corners-ptr ::mem/pointer)
           x-coords (af-row corners 0)
           y-coords (af-row corners 1)
           min-x (af-min-all x-coords)
           max-x (af-max-all x-coords)
           min-y (af-min-all y-coords)
           max-y (af-max-all y-coords)
           ;; Check bounds
           clips-left (< min-x 0)
           clips-right (> max-x target-w)
           clips-top (< min-y 0)
           clips-bottom (> max-y target-h)]
       {:needs-clipping (or clips-left clips-right clips-top clips-bottom)
        :clips-left clips-left
        :clips-right clips-right
        :clips-top clips-top
        :clips-bottom clips-bottom
        :bounds {:min-x min-x :max-x max-x :min-y min-y :max-y max-y}}))
   ```
   
   **Pattern 5: Panorama Stitching Canvas**
   ```clojure
   ;; Calculate canvas for stitching multiple images
   (defn compute-stitching-canvas
     [images homographies]
     (let [all-corners (for [[img H] (map vector images homographies)]
                         (let [[h w] (get-dimensions img)
                               ptr (mem/alloc-pointer ::mem/pointer)
                               _ (af-transform-coordinates ptr H h w)]
                           (mem/read-pointer ptr ::mem/pointer)))
           ;; Find global bounding box
           all-x (mapcat #(af-to-vector (af-row % 0)) all-corners)
           all-y (mapcat #(af-to-vector (af-row % 1)) all-corners)
           min-x (apply min all-x)
           max-x (apply max all-x)
           min-y (apply min all-y)
           max-y (apply max all-y)
           canvas-w (long (Math/ceil (- max-x min-x)))
           canvas-h (long (Math/ceil (- max-y min-y)))]
       {:canvas-width canvas-w
        :canvas-height canvas-h
        :offset-x min-x
        :offset-y min-y}))
   ```
   
   **Pattern 6: Visualize Transformed Bounds**
   ```clojure
   ;; Draw transformed corner quadrilateral on original image
   (defn visualize-transformation
     [img transform-matrix]
     (let [[h w] (get-dimensions img)
           corners-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-transform-coordinates corners-ptr transform-matrix h w)
           corners (mem/read-pointer corners-ptr ::mem/pointer)
           ;; Extract corner points
           x-coords (af-host (af-row corners 0))  ; [x0 x1 x2 x3]
           y-coords (af-host (af-row corners 1))  ; [y0 y1 y2 y3]
           ;; Draw lines connecting corners
           viz-img (draw-line img [x-coords 0] [y-coords 0] 
                                  [x-coords 1] [y-coords 1])
           viz-img (draw-line viz-img [x-coords 1] [y-coords 1]
                                      [x-coords 2] [y-coords 2])
           viz-img (draw-line viz-img [x-coords 2] [y-coords 2]
                                      [x-coords 3] [y-coords 3])
           viz-img (draw-line viz-img [x-coords 3] [y-coords 3]
                                      [x-coords 0] [y-coords 0])]
       viz-img))
   ```
   
   Input/Output Specifications:
   
   **Input**:
   - tf: 3×3 transformation matrix (f32 or f64)
   - d0: Input image height (float)
   - d1: Input image width (float)
   
   **Output**:
   - out: [3, 4] array containing transformed corner coordinates
     * Row 0: x-coordinates of 4 corners
     * Row 1: y-coordinates of 4 corners
     * Row 2: homogeneous coordinates (typically all 1)
   - Column order: [top-left, bottom-left, bottom-right, top-right]
   
   Visual Example:
   ```
   Input corners (d0=100, d1=200):
   (0, 0)          (200, 0)         (Top-left)     (Top-right)
       +--------------+
       |              |
       |              |            After 45° rotation:
       |              |                  *
       +--------------+              *       *
   (0, 100)      (200, 100)     *               *
                                 *       *
   (Bottom-left) (Bottom-right)     *
   
   Output: [3, 4] array with transformed [x, y, 1] coordinates
   ```
   
   Type Support:
   
   Supported transformation matrix types:
   - f32 (single precision float) - recommended for most use cases
   - f64 (double precision float) - for high-precision requirements
   
   Dimension parameters (d0, d1) are always float type regardless
   of transformation matrix type.
   
   Output array type matches transformation matrix type.
   
   Performance Characteristics:
   
   - Complexity: O(1) - fixed 4 corners, constant work
   - GPU acceleration: Minimal benefit due to small computation
   - Primarily CPU-bound for corner calculation
   - Main use: Pre-computing output dimensions, not pixel processing
   - Typical execution time: < 0.1 ms
   
   Memory Usage:
   - Input: 3×3 matrix (9 elements)
   - Output: 3×4 matrix (12 elements)
   - Minimal memory footprint
   
   Coordinate System Convention:
   
   ArrayFire uses standard image coordinate system:
   - Origin (0, 0) at top-left corner
   - X-axis increases rightward (columns)
   - Y-axis increases downward (rows)
   - d0 = height (number of rows)
   - d1 = width (number of columns)
   
   Relationship with af_transform:
   
   af_transform_coordinates is typically used BEFORE af_transform:
   1. Use af_transform_coordinates to determine output dimensions
   2. Compute bounding box from transformed corners
   3. Create output array with computed dimensions
   4. Call af_transform to perform actual image warping
   
   Example workflow:
   ```clojure
   (defn transform-image-auto-size
     [img transform-matrix]
     ;; Step 1: Get transformed corners
     (let [[h w] (get-dimensions img)
           corners-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-transform-coordinates corners-ptr transform-matrix h w)
           corners (mem/read-pointer corners-ptr ::mem/pointer)
           ;; Step 2: Compute output dimensions
           x-coords (af-row corners 0)
           y-coords (af-row corners 1)
           min-x (af-min-all x-coords)
           max-x (af-max-all x-coords)
           min-y (af-min-all y-coords)
           max-y (af-max-all y-coords)
           out-w (long (Math/ceil (- max-x min-x)))
           out-h (long (Math/ceil (- max-y min-y)))
           ;; Step 3: Create translation to shift into positive coordinates
           shift-matrix (create-translation-matrix-3x3 (- min-x) (- min-y))
           adjusted-transform (af-matmul shift-matrix transform-matrix)
           ;; Step 4: Perform transformation
           out-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-transform out-ptr img adjusted-transform out-h out-w
                          AF_INTERP_BILINEAR false)]
       (mem/read-pointer out-ptr ::mem/pointer)))
   ```
   
   Error Handling:
   
   Common errors:
   - **AF_ERR_SIZE**: Transformation matrix is not 3×3
   - **AF_ERR_TYPE**: Transformation matrix is not f32 or f64
   - **AF_ERR_ARG**: Invalid dimensions (d0 or d1 negative or zero)
   
   Best Practices:
   
   1. **Always check output bounds**: Transformed corners may be negative
      or exceed target canvas size
   
   2. **Use double precision cautiously**: f32 sufficient for most use cases,
      f64 only needed for sub-pixel accuracy requirements
   
   3. **Validate transformation matrix**: Ensure matrix is invertible
      (determinant ≠ 0) if planning inverse transform
   
   4. **Consider translation adjustment**: If corners have negative
      coordinates, translate to positive space before af_transform
   
   5. **Round up dimensions**: Use Math/ceil for output dimensions to
      avoid clipping
   
   6. **Compose transformations once**: Pre-multiply transformation
      matrices rather than applying sequentially
   
   7. **Cache corner calculations**: Reuse computed corners if applying
      same transform to multiple images
   
   Mathematical Properties:
   
   **Transformation Composition**:
   For transformations T₁ and T₂, applying T₁ then T₂:
   - Combined matrix: T = T₂ × T₁ (note: right-to-left multiplication)
   - Single matrix multiplication more efficient than two transforms
   
   **Inverse Transformation**:
   To reverse a transformation T:
   - Compute T⁻¹ (matrix inverse)
   - Apply T⁻¹ to transform back
   - Check: T × T⁻¹ = I (identity matrix)
   
   **Preservation Properties**:
   - Distances: Not preserved (except for rigid transformations)
   - Angles: Preserved by similarity transforms only
   - Parallel lines: Preserved by affine transforms
   - Ratios on lines: Preserved by affine transforms
   - Cross-ratios: Preserved by projective transforms
   
   Limitations:
   
   1. **Only corner coordinates**: Function computes 4 corners only,
      not full boundary or internal points
   
   2. **No interpolation information**: Only geometric transformation,
      no pixel values computed
   
   3. **Assumes planar transformation**: 2D transformation only,
      no 3D perspective handling
   
   4. **Fixed corner count**: Always returns 4 corners regardless
      of transformation complexity
   
   Alternatives and Related Functions:
   
   - **af-transform**: Apply transformation to image pixels
   - **af-rotate**: Specialized rotation transformation
   - **af-translate**: Specialized translation transformation
   - **af-scale**: Specialized scaling transformation
   - **af-skew**: Shear transformation
   - **af-resize**: Simple resize without transformation matrix
   
   When to Use Each:
   - Use af_transform_coordinates: When you need output dimensions
     before transformation, or for coordinate system conversions
   - Use af_transform: When you need to warp/transform actual image pixels
   - Use specialized functions (rotate, translate, scale): When transformation
     is simple and you don't need full matrix control
   
   References:
   
   - ArrayFire Documentation: http://arrayfire.org/docs/group__transform__func__coordinates.html
   - Computer Vision textbooks: Hartley & Zisserman \"Multiple View Geometry\"
   - Homogeneous coordinates: https://en.wikipedia.org/wiki/Homogeneous_coordinates
   - Transformation matrices: OpenCV documentation on geometric transformations
   
   See also:
   - af-transform: Apply transformation to image
   - af-rotate: Rotate image
   - af-translate: Translate image
   - af-scale: Scale image
   - af-perspective: Perspective transformation (if available)
   - af-homography: Homography estimation (if available)"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; Coordinate transformation

;; af_err af_transform_coordinates(af_array *out, const af_array tf, const float d0, const float d1)
(defcfn af-transform-coordinates
  "Transform image corner coordinates using a transformation matrix.
   
   Computes the transformed coordinates of the four corners of an image
   rectangle given a 3×3 homogeneous transformation matrix. This is useful
   for determining output dimensions before applying image transformations.
   
   Parameters:
   - out: Output pointer for transformed corner coordinates [3, 4] array
   - tf: 3×3 transformation matrix (f32 or f64)
   - d0: Input image height (first dimension) as float
   - d1: Input image width (second dimension) as float
   
   Input Corner Points (before transformation):
   The function transforms these four corners of the input image:
   - (0, 0)       → top-left corner
   - (0, d0)      → bottom-left corner  
   - (d1, d0)     → bottom-right corner
   - (d1, 0)      → top-right corner
   
   Output Array Structure:
   Returns a [3, 4] array where:
   - Row 0: x-coordinates of the 4 transformed corners
   - Row 1: y-coordinates of the 4 transformed corners
   - Row 2: homogeneous coordinates (w component, typically 1)
   - Columns: [top-left, bottom-left, bottom-right, top-right]
   
   Transformation Matrix Format:
   The 3×3 matrix tf should be in homogeneous coordinate form:
   ```
       ┌           ┐
       │ a  b  tx │  where:
   T = │ c  d  ty │  - [a,b; c,d] is the 2×2 linear transform
       │ p  q  s  │  - [tx, ty] is the translation
       └           ┘  - [p, q, s] enables perspective effects
   ```
   
   Mathematical Operation:
   For each corner point [x, y], computes:
   ```
       ┌    ┐       ┌           ┐ ┌   ┐
       │ x' │       │ a  b  tx │ │ x │
       │ y' │ = T × │ c  d  ty │ │ y │
       │ w  │       │ p  q  s  │ │ 1 │
       └    ┘       └           ┘ └   ┘
   
   Then: x' = x'/w, y' = y'/w (perspective division)
   ```
   
   Common Use Cases:
   
   1. **Calculate Output Dimensions**:
      ```clojure
      ;; Determine bounding box for rotated image
      (let [angle (/ Math/PI 4)  ; 45 degrees
            rot-matrix (create-rotation-matrix angle)
            [h w] [100 200]  ; input dimensions
            corners-ptr (mem/alloc-pointer ::mem/pointer)
            _ (af-transform-coordinates corners-ptr rot-matrix h w)
            corners (mem/read-pointer corners-ptr ::mem/pointer)
            x-coords (af-row corners 0)
            y-coords (af-row corners 1)
            ;; Find bounding box
            min-x (af-min-all x-coords)
            max-x (af-max-all x-coords)
            min-y (af-min-all y-coords)
            max-y (af-max-all y-coords)
            out-w (long (Math/ceil (- max-x min-x)))
            out-h (long (Math/ceil (- max-y min-y)))]
        {:width out-w :height out-h :offset-x min-x :offset-y min-y})
      ```
   
   2. **Image Registration**:
      ```clojure
      ;; Check if transformed image fits in target canvas
      (let [homography (estimate-homography src-pts dst-pts)
            [src-h src-w] (get-dimensions src-img)
            [dst-h dst-w] (get-dimensions dst-img)
            coords-ptr (mem/alloc-pointer ::mem/pointer)
            _ (af-transform-coordinates coords-ptr homography src-h src-w)
            corners (mem/read-pointer coords-ptr ::mem/pointer)
            x-coords (af-row corners 0)
            y-coords (af-row corners 1)
            fits? (and (>= (af-min-all x-coords) 0)
                      (<= (af-max-all x-coords) dst-w)
                      (>= (af-min-all y-coords) 0)
                      (<= (af-max-all y-coords) dst-h))]
        fits?)
      ```
   
   3. **Panorama Stitching**:
      ```clojure
      ;; Compute global canvas for multiple images
      (defn compute-panorama-canvas [images homographies]
        (let [all-corners (for [[img H] (map vector images homographies)]
                           (let [[h w] (get-dimensions img)
                                 ptr (mem/alloc-pointer ::mem/pointer)
                                 _ (af-transform-coordinates ptr H h w)]
                             (mem/read-pointer ptr ::mem/pointer)))
              all-x (mapcat #(af-host (af-row % 0)) all-corners)
              all-y (mapcat #(af-host (af-row % 1)) all-corners)
              canvas-w (long (Math/ceil (- (apply max all-x) 
                                          (apply min all-x))))
              canvas-h (long (Math/ceil (- (apply max all-y) 
                                          (apply min all-y))))]
          {:canvas-width canvas-w :canvas-height canvas-h}))
      ```
   
   Type Support:
   - Transformation matrix: f32 or f64
   - Dimension parameters: float (can pass int, will be converted)
   - Output array type matches input matrix type
   
   Performance:
   - Very fast: O(1) complexity, only 4 corner points
   - Minimal GPU benefit due to small computation
   - Typical execution: < 0.1 ms
   
   Coordinate System:
   - Origin (0,0) at top-left
   - X-axis: rightward (columns)
   - Y-axis: downward (rows)
   - d0 = height (rows)
   - d1 = width (columns)
   
   Workflow Pattern:
   Typical usage before af_transform:
   1. Use af-transform-coordinates to get transformed corners
   2. Compute bounding box from corners
   3. Adjust transformation to shift to positive coordinates if needed
   4. Create output array with computed dimensions
   5. Call af-transform to warp image pixels
   
   Example (Complete Workflow):
   ```clojure
   (defn transform-with-auto-sizing
     [img transform-matrix]
     ;; Step 1: Get transformed corners
     (let [[h w] (get-dimensions img)
           corners-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-transform-coordinates corners-ptr transform-matrix h w)
           corners (mem/read-pointer corners-ptr ::mem/pointer)
           ;; Step 2: Compute bounds
           x-coords (af-row corners 0)
           y-coords (af-row corners 1)
           min-x (af-min-all x-coords)
           max-x (af-max-all x-coords)
           min-y (af-min-all y-coords)
           max-y (af-max-all y-coords)
           out-w (long (Math/ceil (- max-x min-x)))
           out-h (long (Math/ceil (- max-y min-y)))
           ;; Step 3: Adjust transform for positive coordinates
           shift (create-translation-matrix (- min-x) (- min-y))
           adjusted-tf (af-matmul shift transform-matrix)
           ;; Step 4: Transform image
           out-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-transform out-ptr img adjusted-tf out-h out-w
                          AF_INTERP_BILINEAR false)]
       (mem/read-pointer out-ptr ::mem/pointer)))
   ```
   
   Constraints:
   - tf must be 3×3 matrix
   - d0 and d1 must be positive
   - tf type must be f32 or f64
   
   Common Errors:
   - AF_ERR_SIZE: Matrix is not 3×3
   - AF_ERR_TYPE: Matrix is not f32 or f64
   - AF_ERR_ARG: Negative or zero dimensions
   
   Best Practices:
   - Round output dimensions UP (use Math/ceil) to avoid clipping
   - Check for negative coordinates after transformation
   - Add translation to shift into positive space if needed
   - Cache results if applying same transform to multiple images
   - Compose transformations first, then compute corners once
   
   Notes:
   - Only computes corner positions, not pixel values
   - Does not perform actual image transformation
   - Output type matches transformation matrix type
   - Homogeneous coordinates enable perspective transformations
   - For affine transforms (no perspective), row 2 will be [1,1,1,1]
   
   Returns:
   ArrayFire error code (af_err enum):
   - AF_SUCCESS (0): Coordinates computed successfully
   - AF_ERR_SIZE: Invalid transformation matrix size
   - AF_ERR_TYPE: Unsupported matrix type
   - AF_ERR_ARG: Invalid dimension parameters
   
   See also:
   - af-transform: Apply transformation to image pixels
   - af-rotate: Rotate image by angle
   - af-translate: Translate image by offset
   - af-scale: Scale image by factors
   - af-resize: Resize without transformation matrix"
  "af_transform_coordinates" [::mem/pointer ::mem/pointer ::mem/float ::mem/float] ::mem/int)
