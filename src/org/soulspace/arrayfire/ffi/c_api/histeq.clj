(ns org.soulspace.arrayfire.ffi.c-api.histeq
  "Bindings for the ArrayFire histogram equalization functions."
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.loader]))

;; af_err af_hist_equal(af_array *out, const af_array in, const af_array hist)
(defcfn af-hist-equal
  "Perform histogram equalization on an input array.
   
   Histogram equalization is a fundamental image enhancement technique that
   adjusts the intensity distribution of an image to improve its contrast and
   visual quality. This is particularly valuable in medical imaging, photography,
   and computer vision applications where image quality is critical.
   
   Mathematical Foundation
   -----------------------
   
   Histogram equalization transforms the input intensities using a mapping
   function derived from the cumulative distribution function (CDF) of the
   histogram:
   
   Let H[i] be the histogram (frequency count for intensity level i)
   Let N be the total number of pixels
   Let L be the number of intensity levels (e.g., 256 for 8-bit images)
   
   1. Probability Mass Function (PMF):
      P[i] = H[i] / N
   
   2. Cumulative Distribution Function (CDF):
      CDF[i] = Σ(j=0 to i) P[j]
      
   3. Normalized CDF:
      CDF_norm[i] = (CDF[i] - CDF_min) / (CDF_max - CDF_min)
      
      where:
      - CDF_min is the minimum non-zero value in CDF
      - CDF_max is typically 1.0 (or the maximum CDF value)
   
   4. Mapping Function:
      output[i] = round(CDF_norm[input[i]] × (L - 1))
   
   The result is an image whose histogram approximates a uniform distribution,
   meaning all intensity levels appear with roughly equal frequency.
   
   Algorithm Overview
   ------------------
   
   ArrayFire's histogram equalization follows these steps:
   
   1. **Input Validation**
      - Verify input array is non-empty
      - Verify histogram is 1D array
      - Check type compatibility
   
   2. **Flatten Input** (if multi-dimensional)
      - Convert to 1D vector for lookup operations
      - Preserve original dimensions for output reshaping
   
   3. **Convert Histogram to Float**
      - Cast histogram to f32 for precise calculations
      - Ensure numeric stability in subsequent operations
   
   4. **Compute CDF via Scan**
      - Apply prefix sum (cumulative sum) operation
      - CDF[i] = H[0] + H[1] + ... + H[i]
      - Uses efficient parallel scan on GPU
   
   5. **Find CDF Range**
      - Compute min(CDF) - typically CDF[0] for natural images
      - Compute max(CDF) - should equal total pixel count
   
   6. **Normalize CDF**
      - factor = (L - 1) / (max(CDF) - min(CDF))
      - normalized_CDF = (CDF - min) × factor
      - Maps CDF range to [0, L-1]
   
   7. **Lookup and Map**
      - For each pixel value v in input:
        output_value = normalized_CDF[v]
      - Uses efficient lookup table operation
   
   8. **Type Cast and Reshape**
      - Cast result back to input type
      - Reshape to original dimensions
   
   Parameters
   ----------
   
   - **out**: ::mem/pointer
     Output array handle. Will contain the histogram-equalized result.
     Same dimensions and type as input array.
   
   - **in**: ::mem/pointer
     Input array handle. Must be 2D for images (typical use case).
     Values should be in range [0, 255] for 8-bit images or appropriate
     range for other bit depths.
     
     Important: ArrayFire assumes non-normalized input values. For float
     images in [0.0, 1.0], scale to [0, 255] before equalization.
   
   - **hist**: ::mem/pointer
     Target histogram array handle. Must be 1D.
     
     The number of bins in the histogram determines the intensity levels (L).
     For 8-bit images, typically 256 bins.
     For 16-bit images, may use 256-4096 bins depending on requirements.
     
     This is typically the output from af-histogram, but can be any 1D
     array defining the target distribution shape.
   
   Returns
   -------
   
   - **::mem/int** (af_err)
     AF_SUCCESS (0) on success, or an error code:
     - AF_ERR_ARG: Invalid arguments (e.g., hist not 1D, incompatible types)
     - AF_ERR_SIZE: Dimension mismatch
     - AF_ERR_TYPE: Unsupported data type
     - AF_ERR_NO_MEM: Insufficient memory
   
   Histogram Equalization Properties
   ----------------------------------
   
   1. **Contrast Enhancement**
      - Spreads out most frequent intensity values
      - Compresses less frequent intensity values
      - Overall effect: enhanced contrast in dominant regions
   
   2. **Histogram Shape**
      - Output histogram approximates uniform distribution
      - Not perfectly uniform due to discrete binning
      - More uniform = better contrast spread
   
   3. **Monotonic Transformation**
      - Mapping function is monotonically increasing
      - Preserves ordering: if pixel_a < pixel_b, then output_a ≤ output_b
      - No pixel value reversals or discontinuities
   
   4. **Information Preservation**
      - No information loss in theory (bijective mapping)
      - Some quantization effects in practice due to rounding
      - Reversible if CDF is stored (rare in practice)
   
   5. **Global Operation**
      - Uses entire image statistics (global histogram)
      - Same mapping applied to all pixels
      - May over-enhance some regions while under-enhancing others
   
   Common Use Cases
   ----------------
   
   1. **Medical Imaging**
      - X-ray enhancement: improve bone/tissue contrast
      - CT/MRI visualization: reveal subtle features
      - Mammography: detect microcalcifications
      - Ultrasound: enhance organ boundaries
      
      Example: Chest X-ray
      ```clojure
      (let [xray (load-image \"chest-xray.png\" false)
            hist (af-histogram xray 256 0.0 255.0)
            enhanced (af-hist-equal xray hist)]
        (save-image \"chest-xray-enhanced.png\" enhanced))
      ```
   
   2. **Photography and Image Editing**
      - Auto-fix underexposed/overexposed photos
      - Enhance low-light images
      - Improve foggy/hazy image visibility
      - Preprocessing for other enhancement techniques
      
      Example: Underexposed photo correction
      ```clojure
      (let [photo (load-image \"underexposed.jpg\" false)
            ;; Convert to grayscale for processing
            gray (af-rgb-to-gray photo)
            hist (af-histogram gray 256 0.0 255.0)
            enhanced (af-hist-equal gray hist)]
        enhanced)
      ```
   
   3. **Document Image Processing**
      - OCR preprocessing: improve text recognition accuracy
      - Scanned document enhancement: compensate for lighting
      - Faded document restoration: boost contrast
      - Historical document digitization
      
      Example: Scanned document enhancement
      ```clojure
      (let [scan (load-image \"scanned-doc.png\" false)
            hist (af-histogram scan 256 0.0 255.0)
            enhanced (af-hist-equal scan hist)
            ;; Apply threshold for binary output
            binary (af-threshold enhanced 128)]
        binary)
      ```
   
   4. **Computer Vision Preprocessing**
      - Feature detection: improve edge/corner detection
      - Object detection: normalize illumination variations
      - Face recognition: compensate for lighting differences
      - Texture analysis: standardize intensity distributions
      
      Example: Illumination normalization for face recognition
      ```clojure
      (let [face (load-image \"face.jpg\" false)
            gray (af-rgb-to-gray face)
            hist (af-histogram gray 256 0.0 255.0)
            normalized (af-hist-equal gray hist)
            ;; Now feed to recognition pipeline
            features (extract-face-features normalized)]
        features)
      ```
   
   5. **Scientific Imaging**
      - Astronomy: enhance faint stellar objects
      - Microscopy: reveal cellular structures
      - Satellite imagery: improve terrain/cloud contrast
      - Fluorescence microscopy: enhance signal intensity
      
      Example: Astronomical image enhancement
      ```clojure
      (let [galaxy (load-image \"m31.fits\" false)
            ;; Use logarithmic stretch first for wide dynamic range
            log-stretched (af-log galaxy)
            hist (af-histogram log-stretched 256 0.0 255.0)
            enhanced (af-hist-equal log-stretched hist)]
        enhanced)
      ```
   
   Advanced Techniques and Variations
   -----------------------------------
   
   1. **Adaptive Histogram Equalization (AHE)**
      - Divide image into tiles (e.g., 8×8 grid)
      - Apply histogram equalization independently to each tile
      - Interpolate at tile boundaries for smooth transitions
      - Better local contrast enhancement than global method
      
      Not directly provided by af-hist-equal, but can be implemented:
      ```clojure
      (defn adaptive-hist-equal [img tile-size]
        (let [tiles (partition-image img tile-size)
              equalized (map #(let [h (af-histogram % 256 0 255)]
                               (af-hist-equal % h))
                            tiles)]
          (merge-tiles equalized tile-size)))
      ```
   
   2. **Contrast Limited AHE (CLAHE)**
      - Extension of AHE with clipping threshold
      - Limits maximum histogram bin height before equalization
      - Prevents over-amplification of noise
      - Industry standard for medical imaging
      
      Histogram clipping:
      ```clojure
      (defn clip-histogram [hist clip-limit]
        (let [clipped (af-min hist clip-limit)
              excess (af-sum (af-sub hist clipped))
              redistribution (/ excess (af-get-elements hist))]
          (af-add clipped redistribution)))
      ```
   
   3. **Histogram Matching/Specification**
      - Instead of uniform distribution, match a specific target histogram
      - Useful for color standardization across image datasets
      - Transfer tone/mood from one image to another
      
      Process:
      ```clojure
      (defn histogram-matching [source target-hist]
        (let [source-hist (af-histogram source 256 0 255)
              source-cdf (cumsum source-hist)
              target-cdf (cumsum target-hist)
              ;; Build mapping: source-cdf -> target intensity
              mapping (build-mapping-lut source-cdf target-cdf)]
          (af-lookup mapping source)))
      ```
   
   4. **Color Image Histogram Equalization**
      - Challenge: equalizing RGB channels independently causes color shifts
      - Solution 1: Convert to HSV, equalize V (value) channel only
      - Solution 2: Convert to YCbCr, equalize Y (luminance) only
      - Solution 3: Equalize L* channel in LAB color space
      
      Example: HSV approach
      ```clojure
      (let [rgb-img (load-image \"color.jpg\" true)
            hsv-img (af-rgb-to-hsv rgb-img)
            [h s v] (split-channels hsv-img)
            v-hist (af-histogram v 256 0 255)
            v-eq (af-hist-equal v v-hist)
            hsv-eq (join-channels h s v-eq)
            rgb-eq (af-hsv-to-rgb hsv-eq)]
        rgb-eq)
      ```
   
   Performance Characteristics
   ---------------------------
   
   1. **Time Complexity**
      - Histogram computation: O(N) where N = number of pixels
      - CDF computation (scan): O(L) where L = number of bins
      - Lookup operation: O(N) parallel lookup
      - Overall: O(N) with efficient GPU parallelization
   
   2. **Space Complexity**
      - Histogram array: O(L) - typically 256-4096 bins
      - CDF array: O(L) - same as histogram
      - Temporary buffers: O(N) - for flattened input
      - Total: O(N + L) ≈ O(N) since N >> L typically
   
   3. **GPU Acceleration**
      - Parallel histogram binning (atomic operations)
      - Parallel scan for CDF (work-efficient algorithm)
      - Parallel lookup (massively parallel, no contention)
      - Expected speedup: 10-100× vs CPU for large images
   
   4. **Typical Performance**
      - 512×512 image (256K pixels): 0.5-2 ms on GPU
      - 1024×1024 image (1M pixels): 1-4 ms on GPU
      - 4096×4096 image (16M pixels): 10-30 ms on GPU
      - Performance scales linearly with image size
   
   5. **Memory Bandwidth**
      - Bottleneck: memory access, not computation
      - Three memory passes: histogram → scan → lookup
      - Optimization: coalesce memory accesses
      - Shared memory used for histogram accumulation
   
   Limitations and Considerations
   -------------------------------
   
   1. **Over-Enhancement**
      - Can amplify noise in smooth regions (e.g., sky, water)
      - Solution: Apply median/bilateral filter before equalization
      - Solution: Use CLAHE instead of global equalization
   
   2. **Loss of Local Contrast**
      - Global equalization may reduce local detail
      - Regions with narrow intensity range get stretched
      - Regions with wide range get compressed
      - Solution: Use adaptive methods (AHE, CLAHE)
   
   3. **Histogram Artifacts**
      - Creates intensity gaps (missing gray levels)
      - Caused by discrete binning and rounding
      - More pronounced with fewer bins or smaller images
      - Generally not visually significant
   
   4. **Color Shift in RGB**
      - Direct RGB equalization changes color balance
      - Red, green, blue channels equalized independently
      - Results in unnatural colors
      - Solution: Use HSV/LAB and equalize only intensity channel
   
   5. **Non-Reversibility**
      - Information loss due to rounding in lookup
      - Multiple input values may map to same output
      - Not suitable if original must be recovered
      - Typically not an issue for visualization tasks
   
   Quality Metrics
   ---------------
   
   Evaluate histogram equalization quality:
   
   1. **Histogram Uniformity**
      - Chi-square test: χ² = Σ((observed - expected)² / expected)
      - Lower χ² indicates more uniform distribution
      - Perfect uniform: χ² = 0 (impossible in practice)
   
   2. **Entropy**
      - H = -Σ(p[i] × log₂(p[i]))
      - Higher entropy indicates more information
      - Maximum entropy = log₂(L) for uniform distribution
   
   3. **Contrast**
      - RMS contrast: √(Σ(I[i,j] - mean)² / N)
      - Higher RMS indicates better contrast
      - Equalization typically increases RMS
   
   4. **Dynamic Range**
      - Range = max(image) - min(image)
      - After equalization, should span [0, L-1]
      - Partial range indicates clipping or limited variation
   
   5. **Visual Quality**
      - Subjective assessment (most important!)
      - Check for artifacts, noise amplification
      - Verify no loss of important detail
      - Compare with original and ground truth (if available)
   
   Comparison: Histogram Equalization vs Other Techniques
   -------------------------------------------------------
   
   | Technique              | Contrast | Noise | Local Detail | Color | Speed   |
   |------------------------|----------|-------|--------------|-------|---------|
   | Histogram Equalization | High     | High  | Low          | Poor  | Fast    |
   | CLAHE                  | High     | Low   | High         | Good  | Medium  |
   | Gamma Correction       | Medium   | Low   | Medium       | Good  | Fast    |
   | Histogram Matching     | Medium   | Low   | Medium       | Good  | Medium  |
   | Tone Mapping (HDR)     | High     | Low   | High         | Good  | Slow    |
   | Unsharp Masking        | Medium   | Medium| High         | Good  | Medium  |
   
   Domain-Specific Applications
   -----------------------------
   
   1. **Radiology**
      - Bone imaging: enhance fracture visibility
      - Soft tissue imaging: improve tumor detection
      - Dental X-rays: reveal cavities and root canals
      - Typical workflow: CLAHE → edge enhancement → diagnosis
   
   2. **Surveillance**
      - Night vision enhancement: boost low-light scenes
      - License plate recognition: improve OCR accuracy
      - Face detection in varying lighting
      - Real-time processing: hardware acceleration critical
   
   3. **Remote Sensing**
      - Land cover classification: normalize illumination
      - Change detection: align temporal images
      - Cloud/haze removal: as preprocessing step
      - Multispectral: apply to each band independently
   
   4. **Quality Control**
      - Defect detection: enhance subtle surface flaws
      - Print inspection: improve contrast for OCR
      - Texture classification: standardize intensity
      - Automated optical inspection (AOI)
   
   5. **Artistic Effects**
      - HDR-like effect: exaggerate local contrast (via CLAHE)
      - Vintage photo restoration: recover faded images
      - Silhouette extraction: enhance subject/background separation
      - Creative grading: transfer mood via histogram matching
   
   Implementation Details
   ----------------------
   
   Internal ArrayFire implementation (src/api/c/histeq.cpp):
   
   1. **Type Handling**
      - Supports: f32, f64, s32, u32, s16, u16, s64, u64, s8, u8
      - Histogram type: typically u32 (unsigned 32-bit counts)
      - Computation type: f32 (single precision float)
   
   2. **Memory Layout**
      - Input flattened to 1D for efficient lookup
      - CDF computed in-place when possible
      - Output reshaped to match input dimensions
   
   3. **Numerical Stability**
      - All CDF calculations in float (32-bit)
      - Division by (max_cdf - min_cdf) checked for zero
      - Clamping output to [0, L-1] range
   
   4. **Backend Optimization**
      - CUDA: uses texture memory for histogram lookup
      - OpenCL: uses local memory for CDF computation
      - CPU: vectorized operations where possible
      - All backends use parallel scan for CDF
   
   Example: Complete Medical Imaging Workflow
   -------------------------------------------
   
   ```clojure
   (defn enhance-medical-image
     \"Complete preprocessing pipeline for medical images.\"
     [image-path]
     (let [;; Load original image (assume 8-bit grayscale)
           original (load-image image-path false)
           
           ;; Step 1: Noise reduction (preserve edges)
           denoised (af-bilateral original 3.0 25.0)
           
           ;; Step 2: Compute histogram
           hist (af-histogram denoised 256 0.0 255.0)
           
           ;; Step 3: Histogram equalization for contrast
           equalized (af-hist-equal denoised hist)
           
           ;; Step 4: Optional - Adaptive sharpening
           sharpened (unsharp-mask equalized 1.0 0.5)
           
           ;; Step 5: Display with annotations
           annotated (overlay-markers sharpened detected-features)]
       
       {:original original
        :denoised denoised
        :equalized equalized
        :final annotated}))
   
   ;; Process entire DICOM series
   (defn process-ct-series [series-dir]
     (let [slices (load-dicom-series series-dir)
           ;; Equalize each slice with consistent histogram
           reference-hist (af-histogram (first slices) 256 0 4095) ; 12-bit CT
           equalized-slices (map #(af-hist-equal % reference-hist) slices)]
       equalized-slices))
   ```
   
   Example: Automated Quality Control
   -----------------------------------
   
   ```clojure
   (defn detect-print-defects
     \"Inspect printed circuit boards for defects.\"
     [pcb-image]
     (let [;; Convert to grayscale
           gray (af-rgb-to-gray pcb-image)
           
           ;; Histogram equalization for uniform lighting
           hist (af-histogram gray 256 0 255)
           normalized (af-hist-equal gray hist)
           
           ;; Edge detection for trace inspection
           edges (af-sobel normalized)
           
           ;; Threshold to find discontinuities
           defects (af-threshold edges 128)
           
           ;; Morphological closing to connect nearby defects
           closed (af-dilate (af-erode defects kernel) kernel)
           
           ;; Label connected components (defect regions)
           labels (af-regions closed)]
       
       {:num-defects (af-max labels)
        :defect-mask labels
        :enhanced normalized}))
   ```
   
   Troubleshooting Common Issues
   ------------------------------
   
   1. **Problem: Output too dark or too bright**
      - Cause: Input not in expected range [0, 255]
      - Solution: Normalize input before equalization
      ```clojure
      (let [normalized (af-mul (af-div img (af-max img)) 255.0)
            hist (af-histogram normalized 256 0 255)
            result (af-hist-equal normalized hist)]
        result)
      ```
   
   2. **Problem: Excessive noise amplification**
      - Cause: Equalization enhances noise in smooth regions
      - Solution: Apply denoising filter first
      ```clojure
      (let [denoised (af-bilateral img 5.0 50.0)
            hist (af-histogram denoised 256 0 255)
            result (af-hist-equal denoised hist)]
        result)
      ```
   
   3. **Problem: Unnatural colors in RGB images**
      - Cause: RGB channels equalized independently
      - Solution: Equalize only intensity/value channel
      ```clojure
      (let [hsv (af-rgb-to-hsv img)
            [h s v] (split-channels hsv)
            hist (af-histogram v 256 0 255)
            v-eq (af-hist-equal v hist)
            hsv-eq (join-channels h s v-eq)]
        (af-hsv-to-rgb hsv-eq))
      ```
   
   4. **Problem: Poor results on high-bit-depth images**
      - Cause: Too many intensity levels (e.g., 16-bit = 65536 levels)
      - Solution: Reduce histogram bins or normalize to 8-bit range
      ```clojure
      ;; Option 1: Fewer bins
      (let [hist (af-histogram img-16bit 1024 0 65535)
            result (af-hist-equal img-16bit hist)]
        result)
      
      ;; Option 2: Normalize to 8-bit
      (let [img-8bit (af-mul (af-div img-16bit 65535.0) 255.0)
            hist (af-histogram img-8bit 256 0 255)
            result (af-hist-equal img-8bit hist)
            ;; Scale back to 16-bit
            result-16bit (af-mul (af-div result 255.0) 65535.0)]
        result-16bit)
      ```
   
   Best Practices
   --------------
   
   1. **Always Inspect Results**
      - Display before/after images side-by-side
      - Check histogram plots to verify uniformity
      - Assess perceptual quality, not just metrics
   
   2. **Consider Domain Requirements**
      - Medical: diagnostic quality paramount, CLAHE preferred
      - Surveillance: real-time constraint, simple equalization
      - Photography: aesthetic quality, manual adjustment may be better
   
   3. **Preprocessing Matters**
      - Denoise before equalization to reduce artifact amplification
      - Crop or mask irrelevant regions (e.g., black borders)
      - For color, convert to appropriate color space first
   
   4. **Validate on Representative Data**
      - Test on diverse lighting conditions
      - Include worst-case scenarios (very dark/bright)
      - Measure performance on target hardware
   
   5. **Documentation**
      - Record equalization parameters (bins, range)
      - Note any preprocessing steps applied
      - Essential for reproducibility and quality assurance
   
   See Also
   --------
   
   - af-histogram: Compute histogram (required input for af-hist-equal)
   - af-bilateral: Denoise while preserving edges (recommended preprocessing)
   - af-rgb-to-hsv / af-hsv-to-rgb: Color space conversion for color images
   - af-threshold: Binarization after equalization (e.g., for OCR)
   - af-regions: Connected component labeling (defect detection)
   
   References
   ----------
   
   1. Gonzalez & Woods, \"Digital Image Processing\", 4th ed., Chapter 3
   2. Pizer et al., \"Adaptive Histogram Equalization and Its Variations\" (1987)
   3. Zuiderveld, \"Contrast Limited Adaptive Histogram Equalization\" (1994)
   4. ArrayFire Documentation: https://arrayfire.org/docs/group__image__func__histequal.htm
   5. Wikipedia: https://en.wikipedia.org/wiki/Histogram_equalization
   
   API Version: >= 1.0
   Introduced: ArrayFire 1.0 (initial release)
   "
  "af_hist_equal" [::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)
