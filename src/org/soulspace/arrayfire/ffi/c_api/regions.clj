(ns org.soulspace.arrayfire.ffi.c-api.regions
  "Bindings for the ArrayFire connected components (regions) functions.
   
   Connected component labeling is a fundamental operation in image analysis
   that identifies and labels distinct regions of connected pixels in binary images.
   Each region (set of connected pixels) is assigned a unique label, enabling
   analysis of individual objects within an image.
   
   ## What is Connected Component Labeling?
   
   Connected component labeling (also called connected component analysis or
   region labeling) processes a binary image to identify groups of adjacent pixels
   that share the same value. Each group is assigned a unique integer label.
   
   **Input**: Binary image (0 = background, non-zero = foreground)
   **Output**: Label image where each connected region has a unique positive integer
   
   ## Connectivity Types
   
   ArrayFire supports two types of pixel connectivity:
   
   ### 4-Connectivity (AF_CONNECTIVITY_4)
   A pixel is connected to its 4 orthogonal neighbors:
   ```
        N
      W C E    C = center pixel
        S      N/S/E/W = neighbors
   ```
   
   Two pixels are connected if they:
   - Have non-zero values
   - Are horizontally or vertically adjacent
   
   ### 8-Connectivity (AF_CONNECTIVITY_8)
   A pixel is connected to its 8 neighbors (orthogonal + diagonal):
   ```
      NW N NE
      W  C  E    C = center pixel
      SW S SE    All 8 surrounding pixels are neighbors
   ```
   
   Two pixels are connected if they:
   - Have non-zero values
   - Are adjacent horizontally, vertically, or diagonally
   
   ## Algorithm Overview
   
   ArrayFire uses a union-find (disjoint-set) algorithm with path compression
   for efficient connected component labeling:
   
   1. **Initial Labeling**: Scan image, assign provisional labels to foreground pixels
   2. **Neighbor Analysis**: Check connectivity with already-labeled neighbors
   3. **Label Merging**: Merge labels when pixels belong to same region (union operation)
   4. **Path Compression**: Optimize label lookups for faster convergence
   5. **Label Compaction**: Renumber labels to be sequential (1, 2, 3, ...)
   
   **Complexity**: O(N × α(N)) where N = number of pixels, α = inverse Ackermann
   function (effectively constant for all practical purposes)
   
   ## Mathematical Foundation
   
   **Connected Components as Equivalence Relation**:
   
   Connectivity defines an equivalence relation on the set of foreground pixels:
   - **Reflexive**: pixel p is connected to itself
   - **Symmetric**: if p is connected to q, then q is connected to p
   - **Transitive**: if p connected to q and q connected to r, then p connected to r
   
   This partitions the foreground pixels into disjoint equivalence classes (regions).
   
   **Union-Find Operations**:
   - **Find(x)**: Returns the representative (root) of the set containing x
   - **Union(x, y)**: Merges the sets containing x and y
   - **Path Compression**: During Find, make all nodes point directly to root
   
   ## Examples
   
   ### Example 1: Basic 4-Connectivity
   ```clojure
   ;; Input binary image (0 = background, 1 = foreground)
   ;; [[0 0 0 0 1 0 1 0]
   ;;  [0 0 0 0 0 0 1 1]
   ;;  [0 1 0 1 0 0 0 0]
   ;;  [0 0 1 0 1 1 0 1]
   ;;  [1 1 0 0 0 1 0 1]
   ;;  [0 0 0 1 0 0 0 1]
   ;;  [0 0 0 0 1 0 0 1]
   ;;  [0 1 0 0 0 1 0 0]]
   
   (let [img-data (byte-array [0 0 0 0 1 0 1 0
                                0 0 0 0 0 0 1 1
                                0 1 0 1 0 0 0 0
                                0 0 1 0 1 1 0 1
                                1 1 0 0 0 1 0 1
                                0 0 0 1 0 0 0 1
                                0 0 0 0 1 0 0 1
                                0 1 0 0 0 1 0 0])
         img (create-array img-data [8 8])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         err (af-regions out-ptr img 0 2)]  ;; 0=AF_CONNECTIVITY_4, 2=f32
     (when (zero? err)
       (mem/read-pointer out-ptr ::mem/pointer)))
   
   ;; Output (4-connectivity): Regions separated by gaps
   ;; [[0 0 0 0 1 0 2 0]
   ;;  [0 0 0 0 0 0 2 2]
   ;;  [0 3 0 4 0 0 0 0]
   ;;  [0 0 4 0 5 5 0 6]
   ;;  [7 7 0 0 0 5 0 6]
   ;;  [0 0 0 8 0 0 0 6]
   ;;  [0 0 0 0 8 0 0 6]
   ;;  [0 9 0 0 0 8 0 0]]
   ```
   
   ### Example 2: 8-Connectivity (More Connected)
   ```clojure
   ;; Same input image, but using 8-connectivity
   (let [img-data (byte-array [0 0 0 0 1 0 1 0
                                0 0 0 0 0 0 1 1
                                0 1 0 1 0 0 0 0
                                0 0 1 0 1 1 0 1
                                1 1 0 0 0 1 0 1
                                0 0 0 1 0 0 0 1
                                0 0 0 0 1 0 0 1
                                0 1 0 0 0 1 0 0])
         img (create-array img-data [8 8])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         err (af-regions out-ptr img 1 2)]  ;; 1=AF_CONNECTIVITY_8, 2=f32
     (when (zero? err)
       (mem/read-pointer out-ptr ::mem/pointer)))
   
   ;; Output (8-connectivity): Diagonal connections merge regions
   ;; [[0 0 0 0 1 0 1 0]
   ;;  [0 0 0 0 0 0 1 1]
   ;;  [0 2 0 2 0 0 0 0]
   ;;  [0 0 2 0 2 2 0 3]
   ;;  [2 2 0 0 0 2 0 3]
   ;;  [0 0 0 2 0 0 0 3]
   ;;  [0 0 0 0 2 0 0 3]
   ;;  [0 2 0 0 0 2 0 0]]
   ;; Notice: Region 2 is much larger due to diagonal connections
   ```
   
   ### Example 3: Object Counting
   ```clojure
   ;; Count distinct objects in a microscopy image
   (defn count-objects [binary-img connectivity]
     (let [out-ptr (mem/alloc-pointer ::mem/pointer)
           ;; Use f32 output type for compatibility
           err (af-regions out-ptr binary-img connectivity 2)]
       (when (zero? err)
         (let [labels (mem/read-pointer out-ptr ::mem/pointer)
               max-ptr (mem/alloc-pointer ::mem/double)
               imag-ptr (mem/alloc-pointer ::mem/double)
               ;; Find maximum label value
               err2 (af-max-all max-ptr imag-ptr labels)]
           (when (zero? err2)
             ;; Maximum label = number of regions
             (int (mem/read-double max-ptr)))))))
   
   ;; Usage:
   ;; (count-objects thresholded-img AF_CONNECTIVITY_8)
   ;; => 42  ;; Found 42 separate objects
   ```
   
   ### Example 4: Region Filtering by Size
   ```clojure
   ;; Remove small regions (noise) from binary image
   (defn filter-small-regions [binary-img min-size connectivity]
     (let [labels-ptr (mem/alloc-pointer ::mem/pointer)
           err (af-regions labels-ptr binary-img connectivity 0)]  ;; 0=s32
       (when (zero? err)
         (let [labels (mem/read-pointer labels-ptr ::mem/pointer)
               ;; For each label, count pixels and filter
               ;; (Implementation would use histogram and masking)
               ]
           ;; Return filtered binary image
           ))))
   
   ;; Usage: Remove regions smaller than 100 pixels
   ;; (filter-small-regions noisy-img 100 AF_CONNECTIVITY_4)
   ```
   
   ### Example 5: Bounding Box Extraction
   ```clojure
   ;; Find bounding box for each labeled region
   (defn extract-bounding-boxes [labels num-regions]
     ;; For each region label (1 to num-regions):
     ;; 1. Create mask where pixels == label
     ;; 2. Find min/max coordinates using where + reduction
     ;; 3. Store {label, x_min, x_max, y_min, y_max}
     (for [label (range 1 (inc num-regions))]
       (let [mask (create-mask labels label)
             coords (where mask)  ;; Get (x, y) coordinates
             x-coords (slice coords 0)
             y-coords (slice coords 1)
             bbox {:label label
                   :x-min (reduce-min x-coords)
                   :x-max (reduce-max x-coords)
                   :y-min (reduce-min y-coords)
                   :y-max (reduce-max y-coords)}]
         bbox)))
   ```
   
   ### Example 6: Feature Extraction per Region
   ```clojure
   ;; Calculate properties for each labeled region
   (defn analyze-regions [original-img labels num-regions]
     (for [label (range 1 (inc num-regions))]
       (let [mask (create-mask labels label)
             region-pixels (select original-img mask)
             area (count-nonzero mask)
             mean-intensity (mean region-pixels)
             std-intensity (std region-pixels)
             perimeter (calculate-perimeter mask)]
         {:label label
          :area area
          :mean-intensity mean-intensity
          :std-intensity std-intensity
          :perimeter perimeter
          :circularity (circularity area perimeter)})))
   ```
   
   ### Example 7: Watershed Segmentation Preprocessing
   ```clojure
   ;; Use regions as markers for watershed algorithm
   (defn create-watershed-markers [binary-seeds]
     (let [markers-ptr (mem/alloc-pointer ::mem/pointer)
           ;; Label each separate seed region
           err (af-regions markers-ptr binary-seeds 
                           1  ;; AF_CONNECTIVITY_8
                           0)] ;; s32 output for marker indices
       (when (zero? err)
         (let [markers (mem/read-pointer markers-ptr ::mem/pointer)]
           ;; markers now contains unique labels for each seed
           ;; Use with watershed algorithm for controlled flooding
           markers))))
   ```
   
   ### Example 8: Defect Detection in Manufacturing
   ```clojure
   ;; Detect and classify defects in product images
   (defn detect-defects [product-img threshold]
     (let [;; Threshold to find defects
           defects (threshold-image product-img threshold)
           ;; Label each defect
           labels-ptr (mem/alloc-pointer ::mem/pointer)
           err (af-regions labels-ptr defects 
                           0  ;; AF_CONNECTIVITY_4
                           0)] ;; s32 output
       (when (zero? err)
         (let [labels (mem/read-pointer labels-ptr ::mem/pointer)
               max-label (get-max-label labels)]
           ;; Analyze each defect
           (for [label (range 1 (inc max-label))]
             (let [defect-mask (create-mask labels label)
                   area (count-pixels defect-mask)
                   bbox (get-bounding-box defect-mask)
                   aspect-ratio (/ (:width bbox) (:height bbox))]
               {:type (classify-defect area aspect-ratio)
                :location bbox
                :severity (severity-score area)}))))))
   ```
   
   ## Output Type Selection
   
   The output type parameter determines the numeric type of region labels:
   
   - **f32 (float)**: Default, most compatible
   - **f64 (double)**: High precision (rarely needed for labels)
   - **s32 (int32)**: Standard integer labels
   - **u32 (uint32)**: Unsigned, supports more regions
   - **s16 (int16)**: Compact, max 32,767 regions
   - **u16 (uint16)**: Compact unsigned, max 65,535 regions
   
   **Choosing Output Type**:
   - Small images (< 100 regions): s16 or u16 (memory efficient)
   - Medium images (< 32K regions): s32 (standard)
   - Large images (> 32K regions): u32 or f32
   - Integration with float pipelines: f32
   
   ## Performance Characteristics
   
   **Time Complexity**: O(N × α(N)) ≈ O(N)
   - N = number of pixels
   - α(N) = inverse Ackermann (< 5 for all practical N)
   - Effectively linear time
   
   **Space Complexity**: O(N)
   - Output label image: same size as input
   - Internal label map: proportional to number of regions
   
   **GPU Acceleration**:
   - Parallel initial labeling: O(N/P) where P = processors
   - Union-find with atomic operations
   - Label compaction parallelized
   - Speedup: 10-100× over CPU for large images
   
   **Memory Layout**:
   - Column-major traversal optimal (ArrayFire default)
   - Cache-friendly neighbor access
   - Coalesced GPU memory access
   
   ## Applications
   
   ### Medical Imaging
   - Cell counting in microscopy
   - Tumor detection and measurement
   - Organ segmentation
   - Lesion analysis
   
   ### Computer Vision
   - Object detection and tracking
   - Optical character recognition (letter segmentation)
   - Gesture recognition (hand/finger segmentation)
   - Scene understanding
   
   ### Industrial Inspection
   - Defect detection in manufacturing
   - Quality control (particle counting)
   - PCB inspection (component verification)
   - Fabric inspection (flaw detection)
   
   ### Document Analysis
   - Text line segmentation
   - Character isolation for OCR
   - Table structure detection
   - Signature verification
   
   ### Remote Sensing
   - Land cover classification
   - Building detection
   - Crop field delineation
   - Change detection
   
   ### Scientific Analysis
   - Particle tracking in physics
   - Crystal structure analysis
   - Pore analysis in materials
   - Colony counting in microbiology
   
   ## Best Practices
   
   1. **Preprocessing**:
      - Apply noise reduction before labeling
      - Use morphological operations to close gaps
      - Threshold carefully to get clean binary images
   
   2. **Connectivity Selection**:
      - Use 4-connectivity for well-separated objects
      - Use 8-connectivity for touching/diagonal objects
      - Consider morphological closing before 4-connectivity
   
   3. **Type Selection**:
      - Use smallest type that can hold max expected regions
      - s16 sufficient for most images (< 32K regions)
      - Use s32 for safety in production code
   
   4. **Post-Processing**:
      - Filter by size to remove noise
      - Merge regions based on distance/similarity
      - Calculate region properties for classification
   
   5. **Memory Management**:
      - Release label arrays when done
      - For batch processing, reuse output arrays
      - Consider downsampling very large images
   
   6. **Validation**:
      - Verify expected number of regions
      - Check for over-segmentation (too many regions)
      - Check for under-segmentation (merged objects)
   
   ## Common Pitfalls
   
   1. **Wrong Input Type**:
      - Must be binary (b8/boolean) image
      - Non-binary inputs will produce errors
      - Convert grayscale to binary first (threshold)
   
   2. **Noise Sensitivity**:
      - Single-pixel noise becomes separate region
      - Solution: Apply median filter or morphological opening
   
   3. **Connectivity Mismatch**:
      - 4-connectivity may split diagonal objects
      - 8-connectivity may merge close objects
      - Test both to determine best for your data
   
   4. **Type Overflow**:
      - Too many regions for output type (e.g., > 32K for s16)
      - Solution: Use larger type or reduce regions via filtering
   
   5. **Memory Leaks**:
      - Always release output array handle
      - Use resource management (with-open, try-finally)
   
   6. **Non-Sequential Labels**:
      - After filtering regions, labels may have gaps
      - Solution: Relabel to make sequential if needed
   
   7. **Border Handling**:
      - Objects touching image border may be incomplete
      - Consider padding image before processing
   
   ## Implementation Notes
   
   ArrayFire's regions implementation uses:
   - **CPU Backend**: Sequential union-find with path compression
   - **CUDA Backend**: Parallel label propagation + equivalence resolution
   - **OpenCL Backend**: Work-efficient parallel connected components
   - **OneAPI Backend**: Not supported (will return error)
   
   The algorithm guarantees:
   - Sequential output labels starting from 1
   - Background (zero input) remains 0 in output
   - Deterministic results (same input → same output)
   - Connected pixels receive same label
   
   ## Type Support
   
   **Input Type**:
   - b8 (boolean): REQUIRED - function will error on other types
   
   **Output Types**:
   - f32 (float): Supported
   - f64 (double): Supported
   - s32 (int32): Supported
   - u32 (uint32): Supported
   - s16 (int16): Supported
   - u16 (uint16): Supported
   - Other types: Will produce type error
   
   ## Connectivity Constants
   
   - AF_CONNECTIVITY_4 = 4 (integer value)
   - AF_CONNECTIVITY_8 = 8 (integer value)
   
   ## Error Conditions
   
   - AF_ERR_ARG: Invalid connectivity (not 4 or 8)
   - AF_ERR_TYPE: Wrong input type (not b8) or unsupported output type
   - AF_ERR_SIZE: Input not 2D array
   - AF_ERR_NOT_SUPPORTED: OneAPI backend (not implemented)
   
   ## See Also
   
   Related image processing functions:
   - Thresholding: Convert grayscale to binary
   - Morphological operations: Clean up binary images before labeling
   - Filtering: Noise reduction preprocessing
   - Histogram: Analyze region size distribution
   - Where: Extract pixel coordinates for each region
   - Moments: Calculate region centroids and orientation
   
   Related analysis:
   - Contour extraction: Find region boundaries
   - Distance transform: Measure distances within regions
   - Watershed: Alternative segmentation method
   - Blob detection: Feature-based region detection"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.loader]))

;; Connected component labeling

;; af_err af_regions(af_array *out, const af_array in, const af_connectivity connectivity, const af_dtype ty)
(defcfn af-regions
  "Identify and label connected regions in a binary image.
   
   Performs connected component labeling (also called connected component
   analysis) on a binary image. Each connected region of foreground pixels
   (non-zero values) is assigned a unique positive integer label. Background
   pixels (zero values) remain zero.
   
   Parameters:
   - out: out pointer for label array
   - in: input binary image (must be type b8/boolean)
   - connectivity: connectivity type (integer)
     * 4 (AF_CONNECTIVITY_4): 4-way connectivity (orthogonal neighbors)
     * 8 (AF_CONNECTIVITY_8): 8-way connectivity (includes diagonals)
   - ty: output label type (integer, af_dtype enum)
     * 0 (f32): float labels
     * 1 (c32): complex float (not recommended for labels)
     * 2 (f64): double labels
     * 3 (c64): complex double (not recommended for labels)
     * 4 (b8): boolean (not suitable for multiple labels)
     * 5 (s32): int32 labels (standard choice)
     * 6 (u32): uint32 labels
     * 7 (u8): uint8 labels (max 255 regions)
     * 8 (s64): int64 labels
     * 9 (u64): uint64 labels
     * 10 (s16): int16 labels (max 32,767 regions)
     * 11 (u16): uint16 labels (max 65,535 regions)
   
   Connectivity Details:
   
   **4-Connectivity (AF_CONNECTIVITY_4 = 4)**:
   Two pixels are connected if they are:
   - Both non-zero
   - Horizontally or vertically adjacent (not diagonal)
   
   Neighbor pattern:
   ```
        N
      W C E    C = center, N/S/E/W = 4 neighbors
        S
   ```
   
   Results in more, smaller regions. Use when objects are well-separated
   or you want to avoid diagonal connections.
   
   **8-Connectivity (AF_CONNECTIVITY_8 = 8)**:
   Two pixels are connected if they are:
   - Both non-zero
   - Adjacent in any direction (horizontal, vertical, or diagonal)
   
   Neighbor pattern:
   ```
      NW N NE
      W  C  E    C = center, all 8 surrounding pixels are neighbors
      SW S SE
   ```
   
   Results in fewer, larger regions. Use when objects may touch diagonally
   or you want more robust region merging.
   
   Algorithm:
   Uses union-find (disjoint-set) data structure with path compression:
   1. Scan image row by row
   2. For each foreground pixel, check already-labeled neighbors
   3. If no labeled neighbors, assign new label
   4. If labeled neighbors exist, assign minimum label and merge sets
   5. Apply path compression for efficiency
   6. Compact labels to be sequential (1, 2, 3, ...)
   
   Output:
   - Background (zero) pixels → 0 in output
   - Each connected region → unique positive integer (1, 2, 3, ...)
   - Labels are sequential with no gaps
   - Maximum label = number of regions found
   
   Performance:
   - Time complexity: O(N × α(N)) ≈ O(N) where α = inverse Ackermann (< 5)
   - Space complexity: O(N) for output + O(R) for label map (R = regions)
   - GPU parallelized: 10-100× faster than CPU
   - Efficient for images with hundreds to millions of pixels
   
   Type Support:
   - **Input**: Must be b8 (boolean) type - errors on other types
   - **Output**: f32, f64, s32, u32, s16, u16
   - Recommended output: s32 (supports up to 2.1 billion regions)
   
   Applications:
   - Object counting: Number of cells, particles, defects
   - Object measurement: Size, shape, position of each region
   - Segmentation: Separate touching objects
   - Tracking: Link regions across frames
   - Feature extraction: Analyze properties per region
   
   Common Workflow:
   1. Acquire grayscale image
   2. Apply preprocessing (noise reduction, enhancement)
   3. Threshold to create binary image
   4. (Optional) Morphological operations to clean up
   5. Apply af-regions to label objects
   6. Filter regions by size, shape, or other criteria
   7. Extract features from each region
   8. Classify or measure regions
   
   Example:
   ```clojure
   ;; Label connected regions in binary image
   (let [binary-img (create-array img-data [512 512])  ;; Must be b8 type
         out-ptr (mem/alloc-pointer ::mem/pointer)
         err (af-regions out-ptr binary-img 
                         8    ;; AF_CONNECTIVITY_8
                         5)]  ;; s32 output type
     (when (zero? err)
       (let [labels (mem/read-pointer out-ptr ::mem/pointer)]
         ;; labels array now contains unique integers for each region
         ;; Use max to find number of regions:
         ;; (af-max-all max-ptr imag-ptr labels)
         labels)))
   ```
   
   Notes:
   - Input MUST be binary (b8) - other types will error
   - Larger connectivity value (8) results in fewer regions
   - Choose output type based on expected max number of regions
   - Label value 0 is always background (zero input pixels)
   - Labels are deterministic (same input → same output)
   - Not supported on OneAPI backend (will return error)
   
   Error Conditions:
   - AF_ERR_ARG: Invalid connectivity value (not 4 or 8)
   - AF_ERR_TYPE: Input not b8 type, or unsupported output type
   - AF_ERR_SIZE: Input array not 2D
   - AF_ERR_NOT_SUPPORTED: OneAPI backend
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - Thresholding: Create binary images from grayscale
   - af-morph-open: Remove small objects before labeling
   - af-morph-close: Fill holes before labeling
   - af-medfilt: Reduce noise before thresholding
   - af-max-all: Find maximum label (number of regions)
   - af-where: Extract coordinates of pixels in each region"
  "af_regions" [::mem/pointer ::mem/pointer ::mem/int ::mem/int] ::mem/int)
