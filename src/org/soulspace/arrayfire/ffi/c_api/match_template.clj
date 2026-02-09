(ns org.soulspace.arrayfire.ffi.c-api.match-template
  "Bindings for the ArrayFire template matching functions.
   
   Template matching is a fundamental technique in computer vision for locating
   a template image within a larger search image. It works by sliding the template
   over the search image and computing a similarity/dissimilarity metric at each
   position, producing a disparity map.
   
   Mathematical Foundation:
   
   Template matching computes a metric M(i,j) for each position (i,j) in the
   search image, where the template would be placed:
   
     M(i,j) = metric(SearchWindow[i:i+h, j:j+w], Template)
   
   Where:
   - SearchWindow is the region of search image under the template
   - Template is the template image (dimensions h × w)
   - metric is one of several matching metrics (SAD, SSD, NCC, etc.)
   - M(i,j) is the resulting disparity value at position (i,j)
   
   Visual Example:
   
   ```
   Search Image (10×10):          Template (3×3):
   ┌───────────┐                  ┌─────┐
   │ . . . . . │                  │ A B │
   │ . A B . . │ ← Template       │ C D │
   │ . C D . . │   found here!    └─────┘
   │ . . . . . │
   │ . . . . . │
   └───────────┘
   
   Result Disparity Map (10×10):
   ┌───────────┐
   │ 8 7 6 7 8 │  ← High values = poor match
   │ 7 0 1 6 7 │  ← Low value at (1,1) = good match
   │ 6 1 2 7 8 │     (for SAD/SSD metrics)
   │ 7 6 7 8 9 │
   │ 8 7 8 9 . │
   └───────────┘
   
   For correlation metrics (NCC, ZNCC), high values indicate good matches.
   ```
   
   Matching Metrics:
   
   ArrayFire provides several matching metrics, each with different properties:
   
   1. **SAD (Sum of Absolute Differences)**:
      ```
      SAD = Σ |T(x,y) - I(x,y)|
      ```
      - Simple, fast computation
      - Range: [0, ∞), 0 = perfect match
      - Not robust to brightness changes
      - Use case: Fast matching with similar lighting
   
   2. **ZSAD (Zero-mean SAD)**:
      ```
      ZSAD = Σ |(T(x,y) - μ_T) - (I(x,y) - μ_I)|
      ```
      Where μ_T, μ_I are means of template and window
      - Robust to additive brightness changes
      - Range: [0, ∞), 0 = perfect match
      - Slightly slower than SAD
      - Use case: Matching under varying brightness
   
   3. **LSAD (Locally scaled SAD)**:
      ```
      LSAD = Σ |T(x,y) - k*I(x,y)|
      ```
      Where k = (Σ T·I) / (Σ I²)
      - Robust to multiplicative brightness (scaling)
      - Range: [0, ∞), 0 = perfect match
      - More expensive than ZSAD
      - Use case: Matching under varying exposure
   
   4. **SSD (Sum of Squared Differences)**:
      ```
      SSD = Σ (T(x,y) - I(x,y))²
      ```
      - Emphasizes large differences (squared)
      - Range: [0, ∞), 0 = perfect match
      - Similar speed to SAD
      - Use case: When large mismatches should dominate
   
   5. **ZSSD (Zero-mean SSD)**:
      ```
      ZSSD = Σ ((T(x,y) - μ_T) - (I(x,y) - μ_I))²
      ```
      - Robust to brightness changes (like ZSAD)
      - Range: [0, ∞), 0 = perfect match
      - Penalizes mismatches more heavily
      - Use case: Robust matching with emphasis on outliers
   
   6. **LSSD (Locally scaled SSD)**:
      ```
      LSSD = Σ (T(x,y) - k*I(x,y))²
      ```
      - Robust to scaling (like LSAD)
      - Range: [0, ∞), 0 = perfect match
      - Most expensive L-metric
      - Use case: Robust to exposure with outlier emphasis
   
   7. **NCC (Normalized Cross-Correlation)**:
      ```
      NCC = (Σ T(x,y)·I(x,y)) / (√(Σ T²) · √(Σ I²))
      ```
      - Correlation-based (not difference)
      - Range: [-1, 1], 1 = perfect match
      - Expensive but robust
      - Use case: When geometric similarity matters most
   
   8. **ZNCC (Zero-mean NCC)**:
      ```
      ZNCC = Σ((T-μ_T)·(I-μ_I)) / (σ_T · σ_I · N)
      ```
      Where σ_T, σ_I are standard deviations, N = number of pixels
      - Most robust metric
      - Range: [-1, 1], 1 = perfect match
      - Most expensive
      - Invariant to linear brightness changes
      - Use case: Best quality matching, can tolerate slow speed
   
   9. **SHD (Sum of Hamming Distances)**:
      ```
      SHD = Σ hamming_distance(T(x,y), I(x,y))
      ```
      - For binary/integer data
      - Bit-level comparison
      - Range: [0, ∞), 0 = perfect match
      - Very fast on modern hardware
      - Use case: Binary descriptors, feature matching
   
   Metric Selection Guide:
   
   | Metric | Speed    | Brightness | Contrast | Best Use Case          |
   |--------|----------|------------|----------|------------------------|
   | SAD    | Fastest  | ❌         | ❌       | Controlled conditions  |
   | ZSAD   | Fast     | ✓          | ❌       | Varying brightness     |
   | LSAD   | Medium   | ✓          | ✓        | Varying exposure       |
   | SSD    | Fast     | ❌         | ❌       | Outlier emphasis       |
   | ZSSD   | Fast     | ✓          | ❌       | Robust with outliers   |
   | LSSD   | Medium   | ✓          | ✓        | Best difference-based  |
   | NCC    | Slow     | ❌         | ✓        | Geometric similarity   |
   | ZNCC   | Slowest  | ✓          | ✓        | Best overall quality   |
   | SHD    | Fastest  | N/A        | N/A      | Binary features        |
   
   Performance Characteristics:
   
   **Computational Complexity**:
   - Let S = search image dimensions (W×H)
   - Let T = template dimensions (w×h)
   - Let N = w×h (template pixels)
   
   | Metric      | Operations per position | Total complexity  |
   |-------------|-------------------------|-------------------|
   | SAD         | O(N)                    | O(W·H·N)          |
   | ZSAD        | O(N) + mean             | O(W·H·N)          |
   | LSAD        | O(N) + division         | O(W·H·N)          |
   | SSD         | O(N)                    | O(W·H·N)          |
   | ZSSD        | O(N) + mean             | O(W·H·N)          |
   | LSSD        | O(N) + division         | O(W·H·N)          |
   | NCC         | O(N) + sqrt             | O(W·H·N)          |
   | ZNCC        | O(N) + mean + std       | O(W·H·N)          |
   | SHD         | O(N) bitwise            | O(W·H·N)          |
   
   **Memory Usage**:
   - Input search image: W×H × bytes_per_pixel
   - Input template: w×h × bytes_per_pixel
   - Output disparity map: W×H × sizeof(float or double)
   - Temporary buffers: Varies by metric (ZNCC uses most)
   
   **GPU Acceleration**:
   - Highly parallelizable across (x,y) positions
   - Each thread processes one output position
   - Template can be cached in shared memory
   - 10-100× speedup typical over CPU
   - Optimal for templates > 5×5 pixels
   
   **Typical Performance** (search: 512×512, template: 32×32):
   - GPU (CUDA/OpenCL):
     * SAD/SSD: ~1-2 ms
     * ZSAD/ZSSD: ~2-3 ms
     * LSAD/LSSD: ~3-5 ms
     * NCC: ~5-8 ms
     * ZNCC: ~8-12 ms
   - CPU (single-threaded):
     * SAD/SSD: ~50-100 ms
     * ZSAD/ZSSD: ~80-150 ms
     * ZNCC: ~300-500 ms
   
   Output Dimensions:
   
   The output disparity map has the **same dimensions** as the search image:
   ```
   output_dims = search_img_dims
   ```
   
   For positions where the template extends beyond the search image boundary,
   zero-padding is used (pixels outside are treated as 0).
   
   This differs from some implementations that return a smaller output.
   ArrayFire's full-size output makes it easier to locate match positions.
   
   Type Support:
   
   **Input Types** (search and template must match):
   - f32: Single precision float
   - f64: Double precision float
   - s32, u32: 32-bit integers
   - s16, u16: 16-bit integers  
   - s8, u8, b8: 8-bit integers/bytes
   
   **Output Types**:
   - f64 → f64 (double input → double output)
   - All others → f32 (integer/float input → float output)
   
   **Type Conversion**:
   Integer inputs are converted to float internally for metric computation,
   ensuring sufficient precision for the disparity calculations.
   
   Applications:
   
   1. **Object Detection**:
      - Locate objects in images/video
      - Traffic sign detection
      - Logo detection
      - Face detection (basic)
      - Part inspection in manufacturing
   
   2. **Quality Control**:
      - Defect detection (compare to reference)
      - Assembly verification
      - PCB inspection
      - Pharmaceutical tablet inspection
   
   3. **Medical Imaging**:
      - Anatomical structure matching
      - Registration of medical scans
      - Tumor detection and tracking
      - Cell counting in microscopy
   
   4. **Video Tracking**:
      - Simple object tracking
      - Motion analysis
      - Surveillance applications
      - Sports analytics
   
   5. **Image Registration**:
      - Panorama stitching
      - Multi-exposure HDR
      - Satellite image alignment
      - Document alignment
   
   6. **Robotics**:
      - Visual servoing
      - Object grasping
      - Navigation landmarks
      - Part identification
   
   7. **Augmented Reality**:
      - Marker detection
      - Planar target tracking
      - Image-based triggers
   
   Finding the Best Match:
   
   After computing the disparity map, find the optimal match location:
   
   **For difference metrics** (SAD, SSD, etc.): Find minimum
   ```clojure
   (let [result (af-match-template ...)]
     ;; Find position of minimum disparity
     (af-min-all result))  ; Returns [min-value, linear-index]
   ```
   
   **For correlation metrics** (NCC, ZNCC): Find maximum
   ```clojure
   (let [result (af-match-template ...)]
     ;; Find position of maximum correlation
     (af-max-all result))  ; Returns [max-value, linear-index]
   ```
   
   **Convert linear index to (x,y)**:
   ```clojure
   (let [width 512
         linear-idx 12345]
     {:x (mod linear-idx width)
      :y (quot linear-idx width)})
   ```
   
   Multi-Scale Matching:
   
   For improved robustness and speed, use image pyramids:
   
   1. **Coarse-to-Fine Search**:
      - Downsample both images by 2×, 4×, etc.
      - Match at coarsest level (fast, approximate)
      - Refine at finer levels in smaller search regions
      - Final match at full resolution
   
   2. **Benefits**:
      - 4-8× speedup typical
      - Better handling of scale variations
      - More robust to noise
   
   3. **Implementation**:
      ```clojure
      ;; Level 2 (coarse): 1/4 resolution
      (let [search-l2 (af-resize search 0.25 0.25)
            template-l2 (af-resize template 0.25 0.25)
            result-l2 (af-match-template search-l2 template-l2 AF_ZNCC)
            [_ pos-l2] (af-min-all result-l2)
            
            ;; Level 1: 1/2 resolution, search near pos-l2×2
            region-l1 (extract-region search-l1 (* 2 pos-l2) margin)
            result-l1 (af-match-template region-l1 template-l1 AF_ZNCC)
            
            ;; Level 0: full resolution, refine
            region-l0 (extract-region search (* 2 pos-l1) margin)
            result-l0 (af-match-template region-l0 template AF_ZNCC)]
        result-l0)
      ```
   
   Batch Processing:
   
   ArrayFire supports batch template matching when the search image is 3D:
   
   **Batch Dimensions**:
   - Search image: [width, height, batch_size]
   - Template: [width, height] (2D only)
   - Output: [width, height, batch_size]
   
   Each slice of the search image is matched independently with the same template.
   This is useful for:
   - Processing video frames
   - Matching across image channels
   - Parallel processing of multiple images
   
   Limitations:
   
   1. **Template Size**:
      - Template must be smaller than search image
      - Very large templates (>128×128) become slow
      - Consider downsampling for large templates
   
   2. **Rotation Invariance**:
      - Template matching is NOT rotation invariant
      - Must match at multiple rotations if needed
      - Consider feature-based methods for rotation
   
   3. **Scale Invariance**:
      - Template matching is NOT scale invariant
      - Must match at multiple scales if needed
      - Use multi-scale approach or feature methods
   
   4. **Occlusion**:
      - Partial occlusion degrades matching
      - Consider part-based or feature methods
   
   5. **Deformation**:
      - Assumes template doesn't deform
      - Rigid or near-rigid objects only
      - Use optical flow or deformable methods otherwise
   
   Best Practices:
   
   1. **Metric Selection**:
      - Start with SAD for speed, ZNCC for quality
      - Use Z-metrics (ZSAD, ZSSD, ZNCC) for robustness
      - Test multiple metrics, choose best for your data
   
   2. **Preprocessing**:
      - Normalize image brightness/contrast
      - Apply histogram equalization if needed
      - Denoise if images are very noisy
      - Convert to grayscale unless color matters
   
   3. **Template Design**:
      - Choose distinctive template regions
      - Avoid textureless or symmetric templates
      - Template should have clear features
      - Minimize background in template
   
   4. **Performance Optimization**:
      - Use faster metrics (SAD, SSD) first
      - Restrict search region if approximate location known
      - Use multi-scale for large images
      - Batch process multiple images
   
   5. **Result Validation**:
      - Check disparity value threshold
      - Verify match makes geometric sense
      - Use peak sharpness to assess confidence
      - Consider multiple top matches (not just best)
   
   6. **Subpixel Refinement**:
      - Fit parabola to peak for subpixel location
      - Improves precision for tracking
      - Important for accurate measurements
   
   Error Handling:
   
   Common error codes:
   - AF_ERR_ARG: Invalid metric type
   - AF_ERR_TYPE: Mismatched input types
   - AF_ERR_SIZE: Template larger than search image
   - AF_ERR_SIZE: Invalid dimensions (search not 2D/3D, template not 2D)
   - AF_ERR_MEM: Insufficient device memory
   
   Comparison with Other Methods:
   
   | Method              | Speed | Rotation | Scale | Occlusion | Deformation |
   |---------------------|-------|----------|-------|-----------|-------------|
   | Template Matching   | Fast  | ❌       | ❌    | ❌        | ❌          |
   | Feature Matching    | Medium| ✓        | ✓     | ✓         | ❌          |
   | Deep Learning       | Slow  | ✓        | ✓     | ✓         | ✓           |
   | Optical Flow        | Fast  | ❌       | ❌    | ❌        | ✓           |
   
   Choose template matching when:
   - Objects are rigid
   - Viewpoint is fixed or nearly fixed
   - Speed is critical
   - Template is distinctive
   - Lighting is reasonably controlled
   
   See also:
   - Feature detectors (FAST, ORB, SIFT) for rotation/scale invariance
   - Feature matching for more robust matching
   - Deep learning object detection for complex scenarios
   - Optical flow for motion estimation"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; Template matching function

;; af_err af_match_template(af_array *out, const af_array search_img, const af_array template_img, const af_match_type m_type)
(defcfn af-match-template
  "Perform template matching to locate a template within a search image.
   
   Slides the template image over the search image, computing a disparity
   metric at each position. The output is a disparity map with the same
   dimensions as the search image, where each value indicates how well the
   template matches at that location.
   
   Parameters:
   - out: out pointer for disparity map array
   - search-img: search image array (2D or 3D for batch)
   - template-img: template image array (must be 2D)
   - m-type: matching metric type (af_match_type enum):
     * AF_SAD (0): Sum of Absolute Differences
     * AF_ZSAD (1): Zero-mean SAD (brightness robust)
     * AF_LSAD (2): Locally scaled SAD (exposure robust)
     * AF_SSD (3): Sum of Squared Differences
     * AF_ZSSD (4): Zero-mean SSD (brightness robust)
     * AF_LSSD (5): Locally scaled SSD (exposure robust)
     * AF_NCC (6): Normalized Cross-Correlation
     * AF_ZNCC (7): Zero-mean NCC (most robust)
     * AF_SHD (8): Sum of Hamming Distances (binary)
   
   Output Interpretation:
   
   **For difference metrics** (SAD, ZSAD, LSAD, SSD, ZSSD, LSSD, SHD):
   - Lower values = better match
   - 0 = perfect match
   - Find minimum: (af-min-all out)
   
   **For correlation metrics** (NCC, ZNCC):
   - Higher values = better match
   - 1 = perfect match, -1 = perfect inverse
   - Find maximum: (af-max-all out)
   
   Dimensions:
   - Search image: [width, height] or [width, height, batch]
   - Template: [t_width, t_height] (must be 2D)
   - Output: Same as search image dimensions
   - Template must be smaller than search in both dimensions
   
   Type Requirements:
   - Both inputs must have same type
   - Supported types: f32, f64, s32, u32, s16, u16, s8, u8, b8
   - Output type: f64 for f64 input, f32 for all others
   
   Batch Processing:
   If search_img is 3D [width, height, batch_size]:
   - Matches template against each slice independently
   - Output is also 3D [width, height, batch_size]
   - Template must still be 2D
   - Useful for video processing or multi-channel matching
   
   Performance:
   - Complexity: O(W×H×w×h) where W×H = search, w×h = template
   - GPU acceleration: 10-100× faster than CPU
   - Metric speed: SAD ≈ SSD > ZSAD ≈ ZSSD > NCC > ZNCC
   - Larger templates are slower (quadratic in template size)
   
   Example 1 (Basic matching with SAD):
   ```clojure
   ;; Find logo in image
   (let [image (load-image \"photo.png\")  ; 1920×1080
         logo (load-image \"logo.png\")    ; 100×50
         
         ;; Perform matching
         result-ptr (mem/alloc-pointer ::mem/pointer)
         err (af-match-template result-ptr image logo 0)  ; AF_SAD = 0
         
         result (mem/read-pointer result-ptr ::mem/pointer)]
     
     (when (zero? err)
       ;; Find best match (minimum for SAD)
       (let [min-ptr (mem/alloc-pointer ::mem/double)
             idx-ptr (mem/alloc-pointer ::mem/int)
             _ (af-min-all min-ptr idx-ptr result)
             
             min-val (mem/read-double min-ptr)
             idx (mem/read-int idx-ptr)
             
             ;; Convert linear index to x,y
             width 1920
             x (mod idx width)
             y (quot idx width)]
         
         (println \"Logo found at:\" x y)
         (println \"Match quality (SAD):\" min-val)
         {:x x :y y :score min-val})))
   ```
   
   Example 2 (Robust matching with ZNCC):
   ```clojure
   ;; Match under varying lighting using ZNCC
   (let [scene (load-image \"scene.png\")
         object (load-image \"object.png\")
         
         result-ptr (mem/alloc-pointer ::mem/pointer)
         ;; AF_ZNCC = 7 (most robust, invariant to linear brightness)
         err (af-match-template result-ptr scene object 7)]
     
     (when (zero? err)
       (let [result (mem/read-pointer result-ptr ::mem/pointer)
             
             ;; For ZNCC, find maximum (high = good match)
             max-ptr (mem/alloc-pointer ::mem/double)
             idx-ptr (mem/alloc-pointer ::mem/int)
             _ (af-max-all max-ptr idx-ptr result)
             
             correlation (mem/read-double max-ptr)
             idx (mem/read-int idx-ptr)]
         
         (if (> correlation 0.8)  ; High correlation = confident match
           (println \"Strong match found, correlation:\" correlation)
           (println \"Weak match, correlation:\" correlation))
         
         {:index idx :correlation correlation})))
   ```
   
   Example 3 (Multi-scale matching for speed):
   ```clojure
   ;; Coarse-to-fine matching for large images
   (defn multiscale-match [search template]
     ;; Level 2: 1/4 resolution (coarse, fast)
     (let [search-l2 (af-resize search 0.25 0.25 AF_INTERP_BILINEAR)
           template-l2 (af-resize template 0.25 0.25 AF_INTERP_BILINEAR)
           
           result-l2-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-match-template result-l2-ptr search-l2 template-l2 7)
           result-l2 (mem/read-pointer result-l2-ptr ::mem/pointer)
           
           ;; Find approximate location
           max-ptr (mem/alloc-pointer ::mem/double)
           idx-ptr (mem/alloc-pointer ::mem/int)
           _ (af-max-all max-ptr idx-ptr result-l2)
           idx-l2 (mem/read-int idx-ptr)
           
           ;; Scale position to original resolution
           width-l0 (get-width search)
           x-l2 (mod idx-l2 (quot width-l0 4))
           y-l2 (quot idx-l2 (quot width-l0 4))
           x-l0 (* 4 x-l2)
           y-l0 (* 4 y-l2)
           
           ;; Extract region around coarse match for fine search
           margin 50
           region (af-slice search
                           (- x-l0 margin) (+ x-l0 margin)
                           (- y-l0 margin) (+ y-l0 margin))
           
           result-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-match-template result-ptr region template 7)
           result (mem/read-pointer result-ptr ::mem/pointer)]
       
       result))
   ```
   
   Example 4 (Batch processing video frames):
   ```clojure
   ;; Match object across multiple frames
   (let [;; Video as 3D array [width, height, num_frames]
         video (load-video \"clip.mp4\")  ; e.g., [640, 480, 100]
         object (load-image \"target.png\")  ; [50, 50]
         
         ;; Match across all frames at once
         result-ptr (mem/alloc-pointer ::mem/pointer)
         err (af-match-template result-ptr video object 0)  ; AF_SAD
         
         result (mem/read-pointer result-ptr ::mem/pointer)]
     
     (when (zero? err)
       ;; Result is [640, 480, 100] - one disparity map per frame
       ;; Extract best match per frame
       (for [frame-idx (range 100)]
         (let [frame-result (af-slice result frame-idx)
               min-ptr (mem/alloc-pointer ::mem/double)
               idx-ptr (mem/alloc-pointer ::mem/int)
               _ (af-min-all min-ptr idx-ptr frame-result)
               
               idx (mem/read-int idx-ptr)
               x (mod idx 640)
               y (quot idx 640)]
           
           {:frame frame-idx :x x :y y}))))
   ```
   
   Example 5 (Compare multiple metrics):
   ```clojure
   ;; Test different metrics to find best for your data
   (defn compare-metrics [search template]
     (let [metrics {:SAD 0, :ZSAD 1, :LSAD 2, 
                    :SSD 3, :ZSSD 4, :LSSD 5,
                    :NCC 6, :ZNCC 7}
           
           results 
           (for [[name metric-id] metrics]
             (let [result-ptr (mem/alloc-pointer ::mem/pointer)
                   start-time (System/currentTimeMillis)
                   err (af-match-template result-ptr search template metric-id)
                   end-time (System/currentTimeMillis)
                   
                   result (mem/read-pointer result-ptr ::mem/pointer)
                   
                   ;; Find best match
                   val-ptr (mem/alloc-pointer ::mem/double)
                   idx-ptr (mem/alloc-pointer ::mem/int)
                   _ (if (#{6 7} metric-id)  ; NCC/ZNCC use max
                       (af-max-all val-ptr idx-ptr result)
                       (af-min-all val-ptr idx-ptr result))
                   
                   match-val (mem/read-double val-ptr)
                   idx (mem/read-int idx-ptr)]
               
               {:metric name
                :value match-val
                :position idx
                :time-ms (- end-time start-time)}))]
       
       ;; Print comparison
       (doseq [r results]
         (println (:metric r) 
                  \"value:\" (:value r)
                  \"time:\" (:time-ms r) \"ms\"))
       
       results))
   ```
   
   Example 6 (Quality control - defect detection):
   ```clojure
   ;; Compare manufactured part to reference template
   (defn detect-defects [part-image reference-template threshold]
     (let [result-ptr (mem/alloc-pointer ::mem/pointer)
           ;; Use ZNCC for robustness to lighting
           _ (af-match-template result-ptr part-image reference-template 7)
           result (mem/read-pointer result-ptr ::mem/pointer)
           
           ;; Find best match
           max-ptr (mem/alloc-pointer ::mem/double)
           idx-ptr (mem/alloc-pointer ::mem/int)
           _ (af-max-all max-ptr idx-ptr result)
           
           correlation (mem/read-double max-ptr)
           idx (mem/read-int idx-ptr)]
       
       (if (>= correlation threshold)
         {:status :pass
          :correlation correlation
          :message \"Part matches reference\"}
         {:status :fail
          :correlation correlation
          :message (str \"Part deviates from reference. \"
                       \"Correlation: \" correlation 
                       \" < threshold: \" threshold)})))
   
   ;; Usage
   (let [part (capture-image-from-camera)
         reference (load-image \"reference-part.png\")
         result (detect-defects part reference 0.95)]
     (when (= :fail (:status result))
       (sound-alarm!)
       (reject-part!)))
   ```
   
   Example 7 (Medical imaging - cell detection):
   ```clojure
   ;; Find all instances of a cell type in microscopy image
   (defn find-all-cells [microscopy-image cell-template threshold]
     (let [result-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-match-template result-ptr microscopy-image cell-template 0)
           result (mem/read-pointer result-ptr ::mem/pointer)
           
           ;; Find all local minima below threshold
           width (get-width microscopy-image)
           height (get-height microscopy-image)
           
           cells (atom [])]
       
       ;; Scan disparity map for local minima
       (doseq [y (range 0 height 5)  ; Step by 5 for efficiency
               x (range 0 width 5)]
         (let [val (get-pixel result x y)]
           (when (< val threshold)
             ;; Check if local minimum (simple non-maximum suppression)
             (let [is-local-min (local-minimum? result x y 10)]
               (when is-local-min
                 (swap! cells conj {:x x :y y :score val}))))))
       
       (println \"Found\" (count @cells) \"cells\")
       @cells))
   ```
   
   Common Errors:
   - AF_ERR_ARG: Invalid metric type (not in range 0-8)
   - AF_ERR_TYPE: Input types don't match
   - AF_ERR_SIZE: Template larger than search image
   - AF_ERR_SIZE: Search image not 2D or 3D
   - AF_ERR_SIZE: Template not 2D
   
   Notes:
   - Output has same dimensions as search image
   - Zero-padding used at boundaries
   - Template must be smaller than search in both dimensions
   - Both inputs must have same type
   - Integer inputs converted to float for computation
   - For video/batch: search_img can be 3D, template must be 2D
   - Metric selection critically affects robustness vs speed
   
   Performance Tips:
   - SAD/SSD fastest, ZNCC slowest but most robust
   - Use multi-scale for large images (4-8× speedup)
   - Restrict search region if approximate location known
   - Batch process frames for video (more efficient)
   - GPU acceleration crucial for real-time (10-100× faster)
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-min-all: Find minimum value and location (for SAD, SSD, etc.)
   - af-max-all: Find maximum value and location (for NCC, ZNCC)
   - Feature matching functions for rotation/scale invariant matching
   - af-nearest-neighbour: For feature descriptor matching"
  "af_match_template" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)
