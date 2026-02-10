(ns org.soulspace.arrayfire.ffi.c-api.anisotropic-diffusion
  "Bindings for the ArrayFire anisotropic diffusion function.
   
   Anisotropic Diffusion for Edge-Preserving Smoothing:
   
   Anisotropic diffusion is an advanced image smoothing technique that reduces
   noise while preserving important features like edges and boundaries. Unlike
   isotropic filters (Gaussian blur), anisotropic diffusion adapts to local
   image structure, smoothing heavily in homogeneous regions while preserving
   sharp transitions.
   
   Mathematical Foundation:
   
   **Perona-Malik Diffusion Equation**:
   ∂I/∂t = div(c(x,y,t) · ∇I)
   
   Where:
   - I: Image intensity
   - t: Time (pseudo-time, controls smoothing strength)
   - ∇I: Image gradient (measures edge strength)
   - c(x,y,t): Diffusion coefficient (conductance function)
   - div: Divergence operator
   
   **Discretized Update**:
   I[n+1] = I[n] + Δt · div(c · ∇I[n])
   
   **Flux Functions** (control edge preservation):
   
   1. **Exponential** (AF_FLUX_EXPONENTIAL = 2, default):
      c(|∇I|) = exp(-(|∇I|/K)²)
      - Smooth decay, gradual edge preservation
      - Better for noisy images
      - More stable numerically
      - Preferred for most applications
   
   2. **Quadratic** (AF_FLUX_QUADRATIC = 1):
      c(|∇I|) = 1 / (1 + (|∇I|/K)²)
      - Sharper transition, stronger edge preservation
      - More sensitive to noise
      - Can produce blocky artifacts
      - Useful for clean images with sharp edges
   
   Where K is the conductance parameter (edge threshold).
   
   **Diffusion Methods**:
   
   1. **Gradient-Based** (AF_DIFFUSION_GRAD = 1, default):
      - Uses gradient magnitude |∇I|
      - Preserves high-gradient regions (edges)
      - Standard Perona-Malik diffusion
      - Best for general-purpose edge-preserving smoothing
   
   2. **Modified Curvature** (AF_DIFFUSION_MCDE = 2):
      - Uses image curvature (second derivative)
      - Preserves corners and fine details better
      - Modified Curvature Diffusion Equation (MCDE)
      - More computationally intensive
      - Better for preserving small structures
   
   Key Parameters:
   
   1. **Timestep (dt)**:
      - Controls stability and smoothing rate per iteration
      - Typical range: [0.05, 0.25]
      - Smaller values: More stable, slower smoothing
      - Larger values: Faster smoothing, risk of instability
      - For stability: dt ≤ 0.25 (2D), dt ≤ 0.17 (3D)
   
   2. **Conductance (K)**:
      - Edge threshold parameter
      - Controls what qualifies as an \"edge\"
      - Typical range: [5, 100] for normalized images
      - Higher K: More smoothing, fewer edges preserved
      - Lower K: Less smoothing, more edges preserved
      - Should be set based on gradient magnitude statistics
   
   3. **Iterations**:
      - Number of diffusion steps
      - Typical range: [5, 50]
      - More iterations: Stronger smoothing
      - Total smoothing ≈ dt × iterations
      - Diminishing returns after ~20-30 iterations
   
   Algorithm Behavior:
   
   - **Homogeneous regions** (low gradient): High diffusion coefficient
     → Strong smoothing (noise reduction)
   
   - **Edge regions** (high gradient): Low diffusion coefficient
     → Minimal smoothing (edge preservation)
   
   - **Adaptive**: Diffusion strength varies spatially based on local structure
   
   Input/Output:
   
   - Input: Non-integral types (f32, f64)
     * Integer inputs (s32, u32, s16, u16, s8, u8) converted to float
   - Output: Same type as input (or float if input was integer)
   - Dimensions: 2D or higher (processes along first two dimensions)
   - Batch processing: Supported via higher dimensions
   
   Performance:
   
   - Complexity: O(iterations × width × height)
   - Each iteration involves:
     * Gradient computation: O(N)
     * Conductance calculation: O(N)
     * Diffusion step: O(N)
   - GPU-accelerated parallel processing
   - Typical timing: ~5-20ms per iteration for 512×512 on modern GPU
   
   Common Applications:
   
   1. **Medical Imaging**:
      - MRI/CT scan denoising
      - Preserving anatomical boundaries
      - Vessel enhancement
      - Tumor boundary preservation
   
   2. **Photography**:
      - Noise reduction without blur
      - Detail preservation while smoothing
      - Pre-processing for edge detection
      - HDR tone mapping
   
   3. **Computer Vision**:
      - Feature extraction preprocessing
      - Edge-aware filtering
      - Segmentation preparation
      - Object boundary refinement
   
   4. **Scientific Imaging**:
      - Microscopy image enhancement
      - Satellite image processing
      - Astronomical image denoising
      - Experimental data smoothing
   
   Advantages over Gaussian Blur:
   
   - Preserves edges and fine details
   - Reduces noise in flat regions
   - Adaptive to local image structure
   - No fixed kernel size (implicit adaptivity)
   - Better for subsequent edge detection
   
   Disadvantages:
   
   - Computationally more expensive
   - More parameters to tune
   - Can create staircasing artifacts
   - Requires multiple iterations
   - Sensitive to parameter selection
   
   Parameter Selection Guidelines:
   
   **For noisy images**:
   - Timestep: 0.1-0.2
   - Conductance: 10-30
   - Iterations: 20-50
   - Flux: Exponential (more stable)
   - Method: Gradient-based
   
   **For clean images**:
   - Timestep: 0.15-0.25
   - Conductance: 5-15
   - Iterations: 5-15
   - Flux: Quadratic (sharper edges)
   - Method: Either (try MCDE for fine details)
   
   **For medical images**:
   - Timestep: 0.05-0.15
   - Conductance: 5-20 (depends on modality)
   - Iterations: 10-30
   - Flux: Exponential
   - Method: MCDE for small structures
   
   Typical Workflow:
   
   ```clojure
   ;; Load and normalize image
   (let [img (load-image \"noisy.png\")
         img-float (af-div img 255.0)
         
         ;; Apply anisotropic diffusion
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-anisotropic-diffusion 
             out-ptr 
             img-float
             0.15              ; timestep
             15.0              ; conductance
             20                ; iterations
             AF_FLUX_EXPONENTIAL
             AF_DIFFUSION_GRAD)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     
     ;; Scale back to [0, 255]
     (af-mul result 255.0))
   ```
   
   Important Notes:
   
   - Convergence: Algorithm may not fully converge in finite iterations
   - Stability: Keep timestep ≤ 0.25 to avoid numerical instability
   - Conductance: Should be proportional to gradient magnitude scale
   - For 8-bit images: Normalize to [0, 1] before processing
   - Multiple passes: Can apply repeatedly with different parameters
   - Edge enhancement: Can be combined with unsharp masking
   
   Related Techniques:
   
   - Bilateral filter: Similar edge-preserving, single-pass
   - Non-local means: More sophisticated, slower
   - Total variation denoising: Optimization-based approach
   - Guided filter: Fast edge-preserving filter
   
   Available in API version 3.6 and later.
   
   See also:
   - af-bilateral: Single-pass edge-preserving filter
   - af-medfilt: Non-linear noise filter
   - af-mean-shift: Segmentation-oriented filter
   - af-gaussian-kernel: Isotropic smoothing kernel"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.loader]))

;; Flux function constants (af_flux_function enum values)
(def AF_FLUX_DEFAULT 0)     ; Default (exponential)
(def AF_FLUX_QUADRATIC 1)   ; Quadratic: c = 1/(1 + (|∇I|/K)²)
(def AF_FLUX_EXPONENTIAL 2) ; Exponential: c = exp(-(|∇I|/K)²)

;; Diffusion equation constants (af_diffusion_eq enum values)
(def AF_DIFFUSION_DEFAULT 0) ; Default (gradient-based)
(def AF_DIFFUSION_GRAD 1)    ; Gradient-based diffusion
(def AF_DIFFUSION_MCDE 2)    ; Modified Curvature Diffusion Equation

;; af_err af_anisotropic_diffusion(af_array* out, const af_array in, const float timestep, const float conductance, const unsigned iterations, const af_flux_function fftype, const af_diffusion_eq diffusion_kind)
(defcfn af-anisotropic-diffusion
  "Apply anisotropic diffusion for edge-preserving smoothing.
   
   Performs iterative anisotropic diffusion to reduce noise while preserving
   edges and important image features. The algorithm adapts smoothing strength
   based on local gradient magnitude, smoothing heavily in flat regions while
   preserving sharp transitions.
   
   Parameters:
   - out: Output pointer for smoothed image
   - in: Input image (f32/f64, or integer types converted to float)
   - timestep: Time step for diffusion (stability: ≤ 0.25)
   - conductance: Edge threshold parameter K (controls edge preservation)
   - iterations: Number of diffusion steps (more = stronger smoothing)
   - fftype: Flux function type:
     * AF_FLUX_DEFAULT (0): Default (exponential)
     * AF_FLUX_QUADRATIC (1): Quadratic flux, sharp edges
     * AF_FLUX_EXPONENTIAL (2): Exponential flux, smooth edges
   - diffusion-kind: Diffusion method:
     * AF_DIFFUSION_DEFAULT (0): Default (gradient)
     * AF_DIFFUSION_GRAD (1): Gradient-based (standard)
     * AF_DIFFUSION_MCDE (2): Modified curvature (fine details)
   
   The diffusion equation iteratively updates the image:
   I[n+1] = I[n] + timestep × div(c(|∇I|) · ∇I)
   
   Where conductance function c(|∇I|) controls edge preservation:
   - Exponential: c = exp(-(|∇I|/K)²)
   - Quadratic: c = 1 / (1 + (|∇I|/K)²)
   
   Parameter Guidelines:
   - timestep: [0.05, 0.25] - Lower is more stable
   - conductance: [5, 100] - Lower preserves more edges
   - iterations: [5, 50] - More gives stronger smoothing
   
   Example (basic denoising):
   ```clojure
   ;; Denoise image while preserving edges
   (let [noisy-img (create-array img-data [512 512])
         ;; Normalize to [0, 1]
         img-norm (af-div noisy-img 255.0)
         
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-anisotropic-diffusion 
             out-ptr img-norm
             0.15              ; stable timestep
             20.0              ; moderate edge preservation
             25                ; iterations
             AF_FLUX_EXPONENTIAL
             AF_DIFFUSION_GRAD)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     
     ;; Convert back to [0, 255]
     (af-mul result 255.0))
   ```
   
   Example (medical image processing):
   ```clojure
   ;; MRI scan denoising with structure preservation
   (let [mri-scan (load-dicom \"scan.dcm\")
         ;; Already normalized [0, 1] typically
         
         out-ptr (mem/alloc-pointer ::mem/pointer)
         ;; Conservative parameters for medical imaging
         _ (af-anisotropic-diffusion
             out-ptr mri-scan
             0.1               ; conservative timestep
             10.0              ; preserve fine structures
             30                ; more iterations for better results
             AF_FLUX_EXPONENTIAL  ; stable for noisy medical data
             AF_DIFFUSION_MCDE)   ; preserve small details
         denoised (mem/read-pointer out-ptr ::mem/pointer)]
     denoised)
   ```
   
   Example (photography enhancement):
   ```clojure
   ;; Clean photo noise without losing details
   (let [photo (load-image \"photo.jpg\")
         photo-float (af-div photo 255.0)
         
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-anisotropic-diffusion
             out-ptr photo-float
             0.2               ; faster smoothing
             15.0              ; preserve visible edges
             15                ; moderate iterations
             AF_FLUX_QUADRATIC  ; sharper edges for clean images
             AF_DIFFUSION_GRAD) ; standard method
         enhanced (mem/read-pointer out-ptr ::mem/pointer)]
     
     ;; Optional: enhance edges after smoothing
     (-> enhanced
         (af-mul 255.0)
         (apply-unsharp-mask)))
   ```
   
   Example (preprocessing for segmentation):
   ```clojure
   ;; Smooth before edge detection
   (let [img (load-grayscale \"object.png\")
         img-norm (af-div img 255.0)
         
         ;; Light smoothing to reduce noise
         smooth-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-anisotropic-diffusion
             smooth-ptr img-norm
             0.1 10.0 10
             AF_FLUX_EXPONENTIAL
             AF_DIFFUSION_GRAD)
         smoothed (mem/read-pointer smooth-ptr ::mem/pointer)
         
         ;; Edge detection on smoothed image
         edges-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-canny edges-ptr smoothed ...)
         edges (mem/read-pointer edges-ptr ::mem/pointer)]
     edges)
   ```
   
   Example (batch processing):
   ```clojure
   ;; Process multiple images at once
   (let [img-batch (create-array batch-data [256 256 10])  ; 10 images
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-anisotropic-diffusion
             out-ptr img-batch
             0.15 20.0 20
             AF_FLUX_EXPONENTIAL
             AF_DIFFUSION_GRAD)
         batch-result (mem/read-pointer out-ptr ::mem/pointer)]
     ;; Result has dims [256 256 10]
     batch-result)
   ```
   
   Example (parameter tuning):
   ```clojure
   ;; Compare different conductance values
   (let [img (load-test-image)]
     (doseq [K [5 10 20 40 80]]
       (let [out-ptr (mem/alloc-pointer ::mem/pointer)
             _ (af-anisotropic-diffusion
                 out-ptr img
                 0.15 K 20
                 AF_FLUX_EXPONENTIAL
                 AF_DIFFUSION_GRAD)
             result (mem/read-pointer out-ptr ::mem/pointer)]
         (save-result result (str \"K_\" K \".png\")))))
   ```
   
   Common Use Cases:
   - Medical image denoising (MRI, CT, ultrasound)
   - Photograph noise reduction
   - Preprocessing for edge detection
   - Segmentation preparation
   - Satellite/aerial image enhancement
   - Microscopy image processing
   - Video frame denoising
   
   Performance Tips:
   - Start with fewer iterations, increase if needed
   - Use exponential flux for noisy images (more stable)
   - Use quadratic flux for clean images (sharper edges)
   - For real-time: Reduce iterations, increase timestep slightly
   - For best quality: Use MCDE with conservative parameters
   
   Stability Considerations:
   - timestep > 0.25 may cause numerical instability
   - Conductance should match gradient magnitude scale
   - Too many iterations can over-smooth
   - Negative conductance triggers error
   
   Comparison with Other Filters:
   - vs Gaussian: Preserves edges, no fixed scale
   - vs Bilateral: Iterative (slower), more parameters
   - vs Median: Better for Gaussian noise, preserves gradients
   - vs Mean-shift: More control, better for noise
   
   Available in API version 3.6 and later.
   
   Returns:
   AF_SUCCESS or error code
   
   See also:
   - af-bilateral: Single-pass edge-preserving filter
   - af-medfilt: Non-linear median filter
   - af-mean-shift: Mode-seeking smoothing
   - af-canny: Edge detection (often used after smoothing)"
  "af_anisotropic_diffusion" [::mem/pointer ::mem/pointer ::mem/float ::mem/float ::mem/int ::mem/int ::mem/int] ::mem/int)
