(ns org.soulspace.arrayfire.ffi.gradient
  "Bindings for the ArrayFire gradient function."
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; af_err af_gradient(af_array *dx, af_array *dy, const af_array in)
(defcfn af-gradient
  "Calculate the gradients of an array.
   
   This function computes the numerical gradients (first-order derivatives) of
   an input array along both dimensions (rows and columns). The gradients are
   calculated using central differences for interior points and forward/backward
   differences at the boundaries.
   
   Mathematical Formulation:
   For interior points (indices 1 to N-2):
     dx[i,j] = (in[i+1,j] - in[i-1,j]) / 2
     dy[i,j] = (in[i,j+1] - in[i,j-1]) / 2
   
   For boundary points:
     dx[0,j] = in[1,j] - in[0,j]           (forward difference)
     dx[N-1,j] = in[N-1,j] - in[N-2,j]     (backward difference)
     dy[i,0] = in[i,1] - in[i,0]           (forward difference)
     dy[i,M-1] = in[i,M-1] - in[i,M-2]     (backward difference)
   
   This is a standard finite difference approximation commonly used in:
   - Image processing (edge detection, feature extraction)
   - Numerical analysis (partial differential equations)
   - Computer vision (optical flow, structure from motion)
   - Machine learning (backpropagation, gradient-based optimization)
   
   Gradient Interpretation:
   
   1. **dx (gradient along rows)**:
      - Measures rate of change in the vertical direction
      - Horizontal edges produce large values
      - Zero where image is constant vertically
   
   2. **dy (gradient along columns)**:
      - Measures rate of change in the horizontal direction
      - Vertical edges produce large values
      - Zero where image is constant horizontally
   
   3. **Gradient Magnitude**:
      - Combined strength: √(dx² + dy²)
      - Indicates edge strength regardless of direction
      - Used in edge detection algorithms
   
   4. **Gradient Direction**:
      - Orientation: θ = atan2(dy, dx)
      - Points perpendicular to edge direction
      - Used in oriented gradient features (HOG, SIFT)
   
   Common Applications:
   
   1. **Edge Detection**:
      - Sobel, Prewitt, Canny algorithms
      - Gradient magnitude highlights edges
      - Gradient direction helps classify edge orientation
   
   2. **Feature Extraction**:
      - Histogram of Oriented Gradients (HOG)
      - Scale-Invariant Feature Transform (SIFT)
      - Gradient-based texture descriptors
   
   3. **Image Sharpening**:
      - Unsharp masking: I' = I + α∇I
      - Enhances high-frequency details
      - α controls sharpening strength
   
   4. **Motion Estimation**:
      - Optical flow computation
      - Lucas-Kanade algorithm
      - Structure tensor analysis
   
   5. **Numerical PDEs**:
      - Finite difference methods
      - Heat equation, wave equation
      - Gradient descent optimization
   
   6. **Computer Vision**:
      - Corner detection (Harris, Shi-Tomasi)
      - Line detection (Hough transform)
      - Image registration
   
   Numerical Properties:
   
   1. **Accuracy**:
      - Central difference: O(h²) error (second-order accurate)
      - Forward/backward: O(h) error (first-order accurate)
      - More accurate than simple forward differences
   
   2. **Stability**:
      - Bounded operator (no amplification)
      - Numerically stable for well-conditioned data
      - May amplify noise in noisy images
   
   3. **Noise Sensitivity**:
      - Derivatives amplify high-frequency noise
      - Common practice: smooth before gradient (Gaussian blur)
      - Gaussian derivative = blur + gradient
   
   Processing Workflow:
   
   1. **Preprocessing** (recommended):
      - Smooth with Gaussian filter to reduce noise
      - Normalize intensity range if needed
      - Convert to appropriate data type
   
   2. **Gradient Computation**:
      - Call af-gradient to get dx and dy
      - Results have same dimensions as input
   
   3. **Postprocessing** (optional):
      - Compute gradient magnitude: √(dx² + dy²)
      - Compute gradient direction: atan2(dy, dx)
      - Threshold for edge detection
      - Non-maximum suppression for thin edges
   
   Edge Detection Pipeline Example:
   ```clojure
   ;; Canny-style edge detection
   (defn detect-edges [image]
     (let [;; 1. Smooth to reduce noise
           sigma 1.4
           kernel-size 5
           gauss-kernel (af-gaussian-kernel kernel-size kernel-size sigma sigma)
           smoothed (af-convolve2 image gauss-kernel)
           
           ;; 2. Compute gradients
           [dx dy] (af-gradient smoothed)
           
           ;; 3. Compute magnitude and direction
           magnitude (af-sqrt (af-add (af-mul dx dx) (af-mul dy dy)))
           direction (af-atan2 dy dx)
           
           ;; 4. Threshold
           threshold 0.1
           edges (af-gt magnitude threshold)]
       edges))
   ```
   
   Examples:
   
   1. Basic gradient computation:
      ```clojure
      (let [image (af-randu 512 512 AF-DTYPE-F32)
            [dx dy] (af-gradient image)]
        ;; dx: vertical gradient (change along rows)
        ;; dy: horizontal gradient (change along columns)
        )
      ```
   
   2. Gradient magnitude (edge strength):
      ```clojure
      (defn gradient-magnitude [image]
        (let [[dx dy] (af-gradient image)
              dx2 (af-mul dx dx)
              dy2 (af-mul dy dy)
              sum (af-add dx2 dy2)]
          (af-sqrt sum)))
      ```
   
   3. Gradient direction (edge orientation):
      ```clojure
      (defn gradient-direction [image]
        (let [[dx dy] (af-gradient image)]
          (af-atan2 dy dx)))  ; Returns angle in radians
      ```
   
   4. Smoothed gradient (noise-robust):
      ```clojure
      (defn smoothed-gradient [image sigma]
        (let [size (int (+ (* 6 sigma) 1))
              kernel (af-gaussian-kernel size size sigma sigma)
              smoothed (af-convolve2 image kernel)]
          (af-gradient smoothed)))
      ```
   
   5. Laplacian approximation (second derivative):
      ```clojure
      (defn laplacian-from-gradient [image]
        ;; ∇²f ≈ ∇·∇f = d²f/dx² + d²f/dy²
        (let [[dx1 dy1] (af-gradient image)
              [dx2 _] (af-gradient dx1)
              [_ dy2] (af-gradient dy1)]
          (af-add dx2 dy2)))
      ```
   
   6. Structure tensor (local orientation):
      ```clojure
      (defn structure-tensor [image sigma]
        ;; Used in corner detection and orientation analysis
        (let [[dx dy] (af-gradient image)
              kernel (af-gaussian-kernel 7 7 sigma sigma)
              
              ;; Tensor components
              dx2 (af-convolve2 (af-mul dx dx) kernel)
              dy2 (af-convolve2 (af-mul dy dy) kernel)
              dxdy (af-convolve2 (af-mul dx dy) kernel)]
          {:dx2 dx2 :dy2 dy2 :dxdy dxdy}))
      ```
   
   7. Corner detection (Harris):
      ```clojure
      (defn harris-corners [image k threshold]
        ;; Harris corner detector
        (let [{:keys [dx2 dy2 dxdy]} (structure-tensor image 1.5)
              ;; Harris response: det(M) - k*trace(M)²
              det (af-sub (af-mul dx2 dy2) (af-mul dxdy dxdy))
              trace (af-add dx2 dy2)
              trace2 (af-mul trace trace)
              response (af-sub det (af-mul-scalar trace2 k))
              corners (af-gt response threshold)]
          corners))
      ```
   
   8. Optical flow (Lucas-Kanade):
      ```clojure
      (defn optical-flow [img1 img2]
        ;; Simplified Lucas-Kanade optical flow
        (let [[dx _] (af-gradient img1)
              [_ dy] (af-gradient img1)
              dt (af-sub img2 img1)  ; Temporal derivative
              
              ;; Solve Ax = b where A = [dx dy], b = -dt
              ;; Using least squares approach
              ]
          ;; Returns velocity field (u, v)
          ))
      ```
   
   Performance Considerations:
   
   1. **Memory Overhead**:
      - Creates two new arrays (dx and dy)
      - Same size as input array
      - Consider memory for large images
   
   2. **Computation Cost**:
      - O(N) complexity for N pixels
      - GPU-accelerated (parallel computation)
      - Fast for typical image sizes
   
   3. **Batching**:
      - Can process multiple images in batch
      - Third and fourth dimensions batched automatically
      - Efficient for video processing
   
   Comparison with Sobel Operator:
   
   | Feature          | af-gradient       | af-sobel-operator |
   |-----------------|-------------------|-------------------|
   | Filter size      | 3×1, 1×3 implicit | 3×3 standard      |
   | Smoothing        | None              | Built-in (1-2-1)  |
   | Noise robustness | Lower             | Higher            |
   | Speed            | Faster            | Slightly slower   |
   | Accuracy         | Good for clean    | Better for noisy  |
   | Use case         | General purpose   | Edge detection    |
   
   Type Support:
   - Floating point: f32, f64 (recommended for accuracy)
   - Complex: c32, c64 (gradients of complex fields)
   - Integer types: Not directly supported
   
   Limitations:
   1. No smoothing applied (use Gaussian blur first for noisy data)
   2. Simple finite differences (not adaptive)
   3. Boundary handling: forward/backward differences (less accurate at edges)
   
   Related Functions:
   - af-sobel-operator: Gradient with built-in smoothing (3×3 Sobel kernel)
   - af-diff1: First-order difference along single dimension
   - af-diff2: Second-order difference along single dimension
   - af-gaussian-kernel: Create smoothing kernel for preprocessing
   - af-convolve2: Apply custom derivative kernels
   
   Parameters:
   - dx: out pointer for gradient along first dimension (rows/vertical)
   - dy: out pointer for gradient along second dimension (columns/horizontal)
   - in: input array handle (must be 2D or higher)
   
   Returns:
   ArrayFire error code
   
   Notes:
   - Input must have at least 2 dimensions
   - Output dimensions match input dimensions
   - Gradients preserve input data type
   - For batch processing: operates on first 2 dimensions, batches over dims 3-4
   - Central differences provide better accuracy than one-sided
   - Consider smoothing noisy input before gradient computation
   - Gradient direction perpendicular to edge, not along it"
  "af_gradient" [::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)
