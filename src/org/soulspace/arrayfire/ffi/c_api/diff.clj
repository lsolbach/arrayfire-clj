(ns org.soulspace.arrayfire.ffi.c-api.diff
  "Bindings for the ArrayFire difference (differentiation) functions.
   
   Numerical differentiation computes discrete approximations to derivatives
   by taking differences of consecutive elements along a specified dimension.
   
   ## First-order difference (diff1)
   
   Computes forward differences:
   ```
   out[i] = in[i+1] - in[i]
   ```
   
   For an array of size n along dimension d, produces output of size n-1.
   
   ## Second-order difference (diff2)
   
   Computes second-order central differences:
   ```
   out[i] = (in[i+2] - in[i+1]) - (in[i+1] - in[i])
          = in[i+2] - 2*in[i+1] + in[i]
   ```
   
   For an array of size n along dimension d, produces output of size n-2.
   
   ## Mathematical Foundation
   
   **First derivative approximation (diff1)**:
   ```
   f'(x) ≈ (f(x+h) - f(x)) / h
   ```
   For unit spacing (h=1): f'(x) ≈ f(x+1) - f(x)
   
   **Second derivative approximation (diff2)**:
   ```
   f''(x) ≈ (f(x+h) - 2f(x) + f(x-h)) / h²
   ```
   For unit spacing (h=1): f''(x) ≈ f(x+1) - 2f(x) + f(x-1)
   
   Note: ArrayFire's diff2 uses forward differences:
   ```
   f''(x) ≈ f(x+2) - 2f(x+1) + f(x)
   ```
   
   ## Dimension Reduction
   
   Both operations reduce the size along the specified dimension:
   - **diff1**: dims[dim] becomes dims[dim] - 1
   - **diff2**: dims[dim] becomes dims[dim] - 2
   
   Other dimensions remain unchanged. For multi-dimensional arrays,
   the operation is applied independently along the specified axis.
   
   ## Applications
   
   **Signal Processing**:
   - Edge detection (image gradients)
   - Change detection in time series
   - Velocity from position (diff1)
   - Acceleration from velocity (diff1 of diff1, or diff2)
   
   **Numerical Analysis**:
   - Finite difference methods
   - Numerical differentiation
   - Discrete calculus operations
   
   **Computer Vision**:
   - Sobel operator components
   - Gradient magnitude and direction
   - Feature detection
   - Optical flow
   
   **Physics Simulations**:
   - Spatial derivatives in PDEs
   - Wave equations
   - Heat diffusion
   
   **Financial Analysis**:
   - Returns from prices (diff1)
   - Rate of change calculations
   - Momentum indicators
   
   **Machine Learning**:
   - Gradient computation in backpropagation
   - Feature engineering (temporal changes)
   - Sequence analysis
   
   ## Implementation Details
   
   **Algorithm**:
   - GPU-accelerated parallel computation
   - In-place operations not supported (creates new array)
   - Memory efficient for large arrays
   - Type-preserving (output type matches input type)
   
   **Boundary Handling**:
   - No padding or extrapolation
   - Output is smaller than input
   - Use interpolation for boundary conditions if needed
   
   ## Relationship to Other Operations
   
   **Gradient (af_gradient)**:
   - Uses central differences internally
   - Returns both x and y derivatives
   - Specialized for image processing
   
   **Convolution**:
   - Can implement arbitrary derivative kernels
   - diff1 equivalent to convolve with [-1, 1]
   - diff2 equivalent to convolve with [1, -2, 1]
   
   **Inverse Operation**:
   - Integration (cumulative sum with af_accum)
   - accum(diff1(x)) ≈ x (up to constant offset)
   
   ## Performance Considerations
   
   **Complexity**:
   - O(n) time where n = array size
   - GPU parallelization across all elements
   - Memory: allocates output array of size n-1 (diff1) or n-2 (diff2)
   
   **Optimization Tips**:
   - Prefer operating on contiguous dimensions (dim=0)
   - Batch multiple arrays when possible
   - Consider using gradient() for 2D image derivatives
   - For repeated operations, reuse output buffers at higher level
   
   ## Common Patterns
   
   **Edge detection (Sobel-like)**:
   ```clojure
   ;; Horizontal edges (vertical differences)
   (def dy (af-diff1 image 0))  ; row differences
   ;; Vertical edges (horizontal differences)  
   (def dx (af-diff1 image 1))  ; column differences
   ;; Gradient magnitude
   (def grad-mag (hypot dx dy))
   ```
   
   **Velocity and acceleration**:
   ```clojure
   ;; Position over time
   (def velocity (af-diff1 position 0))
   ;; First derivative
   (def acceleration (af-diff1 velocity 0))
   ;; Or directly: second derivative
   (def acceleration2 (af-diff2 position 0))
   ```
   
   **Laplacian approximation (2D)**:
   ```clojure
   ;; Second derivative in x
   (def laplacian-x (af-diff2 image 1))
   ;; Second derivative in y
   (def laplacian-y (af-diff2 image 0))
   ;; Approximate Laplacian (requires alignment)
   ;; Note: Need to handle size mismatches
   ```
   
   See also:
   - af_gradient: 2D gradient computation with central differences
   - af_accum: Cumulative sum (inverse of diff1)
   - af_convolve1/2/3: General convolution (can implement custom derivatives)
   - af_sobel: Sobel edge detector (uses gradients internally)"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.loader]))

;; af_err af_diff1(af_array *out, const af_array in, const int dim)
(defcfn af-diff1
  "Compute first-order forward differences along a dimension.
   
   Calculates the discrete first derivative by taking differences between
   consecutive elements: out[i] = in[i+1] - in[i]
   
   This is a forward difference approximation to the first derivative:
   ```
   f'(x) ≈ (f(x+h) - f(x)) / h
   ```
   For unit element spacing (h=1), this simplifies to: f'(x) ≈ f(x+1) - f(x)
   
   Parameters:
   - out: out pointer to array handle (output array)
   - in: input array handle
   - dim: dimension along which to compute differences (0-3)
   
   Dimension behavior:
   - Output size along dim is (input_size - 1)
   - Other dimensions remain unchanged
   - Example: [100, 200, 10] with dim=1 → [100, 199, 10]
   
   Formula:
   For each position i along dimension dim:
   ```
   out[i] = in[i+1] - in[i]
   ```
   
   This gives output indices from 0 to (n-2) inclusive,
   where n is the input size along dim.
   
   Constraints:
   - Input must have at least 2 elements along specified dimension
   - If dimension size < 2, returns empty array (0 elements)
   - Dimension must be in range [0, 3]
   - All data types supported (numeric and boolean)
   
   Type handling:
   - Output type matches input type
   - Integer types: exact integer differences
   - Floating types: floating-point differences
   - Complex types: complex differences (real and imaginary parts)
   - Boolean: treated as 0/1, produces integer result
   
   Applications:
   
   **Signal Processing**:
   - Compute signal derivatives
   - Change detection in time series
   - High-pass filtering effect
   
   **Image Processing**:
   - Edge detection (gradient components)
   - Feature detection
   - Motion detection between frames
   
   **Physics**:
   - Velocity from position: v(t) = (x(t+1) - x(t)) / dt
   - Rate of change calculations
   - Discrete derivatives in simulations
   
   **Financial**:
   - Returns from prices: return(t) = price(t) - price(t-1)
   - First differences for stationarity
   - Change detection
   
   **Machine Learning**:
   - Temporal feature extraction
   - Sequence gradient computation
   - Backpropagation gradients
   
   Edge detection example (image gradients):
   ```clojure
   ;; Compute horizontal edges (differences along rows, dimension 0)
   (let [dx-arr (mem/alloc-instance ::mem/pointer)]
     (af-diff1 dx-arr image-arr 0)
     @dx-arr)  ; Horizontal gradient
   
   ;; Compute vertical edges (differences along columns, dimension 1)
   (let [dy-arr (mem/alloc-instance ::mem/pointer)]
     (af-diff1 dy-arr image-arr 1)
     @dy-arr)  ; Vertical gradient
   
   ;; Gradient magnitude: sqrt(dx^2 + dy^2)
   ;; Use af-hypot for numerical stability
   ```
   
   Velocity from position:
   ```clojure
   ;; Position array over time (dimension 0 is time)
   ;; position: [t0, t1, t2, ..., tn]
   (let [velocity (mem/alloc-instance ::mem/pointer)]
     (af-diff1 velocity position-arr 0)
     ;; velocity: [v0, v1, ..., v(n-1)]
     ;; where v(i) = pos(i+1) - pos(i)
     @velocity)
   ```
   
   Time series analysis:
   ```clojure
   ;; Stock prices over time
   (let [returns (mem/alloc-instance ::mem/pointer)]
     (af-diff1 returns prices-arr 0)
     ;; returns[i] = price[i+1] - price[i]
     @returns)
   ```
   
   Mathematical properties:
   
   **Linearity**:
   ```
   diff1(a*f + b*g) = a*diff1(f) + b*diff1(g)
   ```
   
   **Integration inverse** (up to constant):
   ```
   accum(diff1(f)) ≈ f - f[0]
   ```
   The cumulative sum of differences reconstructs the original
   (minus the first element).
   
   **Second derivative**:
   ```
   diff1(diff1(f)) approximates the second derivative
   ```
   But use af-diff2 for better numerical properties.
   
   Numerical considerations:
   
   **Accuracy**:
   - First-order accurate: error O(h) where h = element spacing
   - Forward difference has truncation error
   - For better accuracy, consider central differences (af_gradient)
   
   **Stability**:
   - Amplifies high-frequency noise
   - Acts as high-pass filter
   - Consider smoothing before differentiation if noisy
   
   **Boundary**:
   - No boundary values computed
   - Output is smaller than input
   - Last element in[n-1] has no successor, not included in output
   
   Comparison with alternatives:
   
   **af_gradient**:
   - Uses central differences: (in[i+1] - in[i-1]) / 2
   - More accurate for smooth functions
   - Returns 2D gradients (dx and dy)
   - Preserves array size (handles boundaries)
   
   **af_convolve1**:
   - General convolution can implement any kernel
   - diff1 ≈ convolve1(in, [-1, 1], mode=AF_CONV_EXPAND)
   - More flexible but potentially slower
   
   **Manual computation**:
   - Can use af-shift and af-sub
   - Less efficient than dedicated diff1
   
   Performance notes:
   - O(n) complexity where n = total elements
   - GPU parallelized across all array elements
   - Memory: allocates output array (n-1 elements along dim)
   - Efficient for large arrays
   - Dimension 0 (contiguous) often fastest
   
   Common pitfalls:
   
   **Size mismatch**:
   Output is 1 element smaller along dimension. Must account for this
   when combining with other operations.
   ```clojure
   ;; ERROR: Can't add dx and dy if computed from different dimensions
   ;; They have different shapes!
   ```
   Solution: Crop or align arrays before combining.
   
   **Empty result**:
   If input dimension size < 2, returns empty array (0 elements).
   Always check input dimensions first.
   
   **Loss of information**:
   First element along dimension has no predecessor in output.
   Cannot reconstruct original exactly from diff1 alone.
   
   **Noise amplification**:
   Differencing amplifies high-frequency noise. Consider smoothing first:
   ```clojure
   ;; Smooth then differentiate
   (let [smoothed (gaussian-blur noisy-signal)]
     (af-diff1 out smoothed dim))
   ```
   
   Example: 1D array
   ```
   Input:  [1, 3, 7, 11, 18]  (size 5)
   Output: [2, 4, 4,  7]       (size 4)
   
   Calculation:
   out[0] = in[1] - in[0] = 3 - 1 = 2
   out[1] = in[2] - in[1] = 7 - 3 = 4
   out[2] = in[3] - in[2] = 11 - 7 = 4
   out[3] = in[4] - in[3] = 18 - 11 = 7
   ```
   
   Example: 2D array along different dimensions
   ```
   Input: [[1, 2, 3],     (3x3)
           [4, 5, 6],
           [7, 8, 9]]
   
   diff1(dim=0) [row differences]:
   [[3, 3, 3],            (2x3)
    [3, 3, 3]]
   
   diff1(dim=1) [column differences]:
   [[1, 1],               (3x2)
    [1, 1],
    [1, 1]]
   ```
   
   See also:
   - af_diff2: Second-order differences (second derivative approximation)
   - af_gradient: 2D gradient with central differences
   - af_accum: Cumulative sum (inverse operation)
   - af_convolve1: General 1D convolution (can implement custom kernels)
   
   Returns:
   ArrayFire error code
   
   Example:
   ```clojure
   ;; Compute differences along first dimension (rows)
   (let [out (mem/alloc-instance ::mem/pointer)]
     (af-diff1 out input-array 0)
     @out)
   
   ;; Edge detection: gradient magnitude
   (let [dx (mem/alloc-instance ::mem/pointer)
         dy (mem/alloc-instance ::mem/pointer)
         grad-mag (mem/alloc-instance ::mem/pointer)]
     (af-diff1 dx image 1)  ; Horizontal differences
     (af-diff1 dy image 0)  ; Vertical differences
     ;; Note: Need to align sizes before combining
     (af-hypot grad-mag @dx @dy 0)
     @grad-mag)
   ```"
  "af_diff1" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_diff2(af_array *out, const af_array in, const int dim)
(defcfn af-diff2
  "Compute second-order forward differences along a dimension.
   
   Calculates the discrete second derivative using forward differences:
   out[i] = in[i+2] - 2*in[i+1] + in[i]
   
   This approximates the second derivative:
   ```
   f''(x) ≈ (f(x+2h) - 2f(x+h) + f(x)) / h²
   ```
   For unit element spacing (h=1): f''(x) ≈ f(x+2) - 2f(x+1) + f(x)
   
   Parameters:
   - out: out pointer to array handle (output array)
   - in: input array handle
   - dim: dimension along which to compute differences (0-3)
   
   Dimension behavior:
   - Output size along dim is (input_size - 2)
   - Other dimensions remain unchanged
   - Example: [100, 200, 10] with dim=1 → [100, 198, 10]
   
   Formula:
   For each position i along dimension dim:
   ```
   out[i] = in[i+2] - 2*in[i+1] + in[i]
          = (in[i+2] - in[i+1]) - (in[i+1] - in[i])
   ```
   
   This is the difference of first differences, giving output
   indices from 0 to (n-3) inclusive, where n is input size.
   
   Constraints:
   - Input must have at least 3 elements along specified dimension
   - If dimension size < 3, returns empty array (0 elements)
   - Dimension must be in range [0, 3]
   - All data types supported (numeric and boolean)
   
   Type handling:
   - Output type matches input type
   - Integer types: exact integer computation
   - Floating types: floating-point computation
   - Complex types: complex computation (both components)
   - Boolean: treated as 0/1
   
   Applications:
   
   **Physics**:
   - Acceleration from position: a(t) = x(t+2) - 2x(t+1) + x(t)
   - Curvature analysis
   - Second-order PDEs (wave equation, diffusion)
   - Force from velocity (F = ma)
   
   **Image Processing**:
   - Laplacian approximation (2D second derivative)
   - Ridge detection
   - Blob detection
   - Curvature of edges
   
   **Signal Processing**:
   - Detect peaks and valleys
   - Inflection point detection
   - Curvature of signals
   - Smoothness analysis
   
   **Financial Analysis**:
   - Acceleration of price changes
   - Momentum indicators
   - Trend curvature
   - Risk analysis (convexity)
   
   **Numerical Methods**:
   - Finite difference methods for PDEs
   - Stability analysis
   - Eigenvalue problems
   - Heat equation, wave equation
   
   Acceleration from position:
   ```clojure
   ;; Position array over time
   (let [accel (mem/alloc-instance ::mem/pointer)]
     (af-diff2 accel position-arr 0)
     ;; accel[i] = pos[i+2] - 2*pos[i+1] + pos[i]
     ;; This is a(t) ≈ x''(t) for unit time steps
     @accel)
   ```
   
   Laplacian approximation (2D):
   ```clojure
   ;; Second derivative in x direction
   (let [d2x (mem/alloc-instance ::mem/pointer)]
     (af-diff2 d2x image 1)
     @d2x)
   
   ;; Second derivative in y direction
   (let [d2y (mem/alloc-instance ::mem/pointer)]
     (af-diff2 d2y image 0)
     @d2y)
   
   ;; Approximate Laplacian: ∇²f ≈ d²f/dx² + d²f/dy²
   ;; Note: Sizes don't match directly, need alignment
   ```
   
   Peak detection:
   ```clojure
   ;; Find local maxima: where second derivative is negative
   (let [d2 (mem/alloc-instance ::mem/pointer)]
     (af-diff2 d2 signal 0)
     ;; Negative values indicate concave down (peaks)
     ;; Positive values indicate concave up (valleys)
     @d2)
   ```
   
   Mathematical properties:
   
   **Linearity**:
   ```
   diff2(a*f + b*g) = a*diff2(f) + b*diff2(g)
   ```
   
   **Relationship to diff1**:
   ```
   diff2(f) ≈ diff1(diff1(f))  (approximately, with alignment)
   ```
   But diff2 is more numerically stable and efficient.
   
   **Symmetry**:
   For smooth functions, second derivative has certain symmetry
   properties that diff2 approximates.
   
   **Sign interpretation**:
   - diff2 > 0: Convex (curving upward), local minimum
   - diff2 < 0: Concave (curving downward), local maximum
   - diff2 ≈ 0: Inflection point or linear region
   
   Numerical considerations:
   
   **Accuracy**:
   - First-order accurate: error O(h) for forward differences
   - For better accuracy, use central differences:
     out[i] = (in[i+1] - 2*in[i] + in[i-1]) / h²
   - But central differences require more boundary handling
   
   **Stability**:
   - Second derivative is more sensitive to noise than first
   - Amplifies high-frequency components significantly
   - Strongly recommended to smooth data first
   - Consider Gaussian smoothing before diff2
   
   **Noise amplification**:
   Second derivatives are notoriously noisy. For noisy data:
   ```clojure
   ;; Smooth with Gaussian first
   (let [smoothed (af-gaussian-kernel sigma)]
     (af-convolve1 smoothed-signal signal smoothed)
     (af-diff2 result smoothed-signal dim))
   ```
   
   **Boundary effects**:
   - Loses 2 elements along dimension
   - No values computed near boundaries
   - May need padding for some applications
   
   Comparison with alternatives:
   
   **diff1(diff1(x))**:
   - Can compute second derivative by applying diff1 twice
   - Loses 2 elements total (1 per diff1)
   - Less numerically stable
   - diff2 is preferred: more efficient and stable
   
   **Convolution**:
   - Can use kernel [1, -2, 1] with af_convolve1
   - More flexible (can adjust coefficients, padding)
   - Potentially slower than specialized diff2
   
   **Laplacian filter**:
   - For 2D: specialized Laplacian kernels available
   - Better for actual Laplacian than summing diff2 in x and y
   
   Performance notes:
   - O(n) complexity where n = total elements
   - GPU parallelized
   - Memory: allocates output (n-2 elements along dim)
   - Efficient for large arrays
   - Single pass more efficient than two diff1 calls
   
   Common pitfalls:
   
   **High noise sensitivity**:
   Second derivatives amplify noise dramatically. Always smooth first
   for real-world noisy data.
   
   **Size reduction**:
   Output is 2 elements smaller. Plan data flow accordingly.
   ```clojure
   ;; Original: size n
   ;; diff1: size n-1
   ;; diff2: size n-2
   ```
   
   **Empty results**:
   If input dimension < 3, returns empty array.
   Check sizes: `(>= (dim-size in dim) 3)`
   
   **Incorrect interpretation**:
   Remember this is forward difference, not central:
   - out[i] uses in[i], in[i+1], in[i+2]
   - Not symmetric around i
   - For centered second derivative, use custom convolution
   
   **Laplacian confusion**:
   Laplacian ∇²f requires summing second derivatives in all directions.
   A single diff2 call only gives one directional component.
   
   Example: 1D array
   ```
   Input:  [1, 3, 7, 11, 18, 20]  (size 6)
   Output: [2, 0, 3, -5]          (size 4)
   
   Calculation:
   out[0] = in[2] - 2*in[1] + in[0] = 7 - 2*3 + 1 = 2
   out[1] = in[3] - 2*in[2] + in[1] = 11 - 2*7 + 3 = 0
   out[2] = in[4] - 2*in[3] + in[2] = 18 - 2*11 + 7 = 3
   out[3] = in[5] - 2*in[4] + in[3] = 20 - 2*18 + 11 = -5
   ```
   
   Interpretation:
   - out[0] = 2 > 0: Convex at position 1 (accelerating upward)
   - out[1] = 0: Linear region around position 2
   - out[2] = 3 > 0: Convex at position 3 (accelerating upward)
   - out[3] = -5 < 0: Concave at position 4 (decelerating)
   
   Example: Parabola
   ```
   Input: y = x² for x = [0, 1, 2, 3, 4]
          [0, 1, 4, 9, 16]
   
   diff1: [1, 3, 5, 7]     (first derivative: 2x)
   diff2: [2, 2, 2]        (second derivative: constant 2)
   
   This correctly identifies constant curvature (parabola).
   ```
   
   Example: 2D Laplacian component
   ```
   Image: [[1, 2, 4],      (3x3)
           [2, 4, 7],
           [4, 7, 11]]
   
   diff2(dim=0) [second derivative in y]:
   [[2, 2, 2]]             (1x3)
   
   diff2(dim=1) [second derivative in x]:
   [[1],                   (3x1)
    [1],
    [1]]
   
   Note: Cannot simply add these - different shapes!
   Need careful alignment for full Laplacian.
   ```
   
   Use cases vs alternatives:
   
   **Use diff2 when**:
   - Computing second derivative along single dimension
   - Analyzing curvature or acceleration
   - Need efficient forward differences
   - Working with 1D signals or separable operations
   
   **Use custom convolution when**:
   - Need central differences (more accurate)
   - Need boundary handling (padding)
   - Want to adjust weights
   - Need other derivative approximations
   
   **Use af_gradient when**:
   - Working with 2D images
   - Need both x and y derivatives
   - Want central differences
   - Need gradient magnitude/direction
   
   See also:
   - af_diff1: First-order differences (first derivative)
   - af_gradient: 2D gradient computation with central differences
   - af_convolve1: General convolution (custom kernels)
   - af_sobel: Edge detection (uses gradients)
   - af_accum: Cumulative sum (integration)
   
   Returns:
   ArrayFire error code
   
   Example:
   ```clojure
   ;; Compute second derivative along time dimension
   (let [out (mem/alloc-instance ::mem/pointer)]
     (af-diff2 out signal 0)
     @out)
   
   ;; Detect peaks (negative second derivative)
   (let [d2 (mem/alloc-instance ::mem/pointer)
         peaks (mem/alloc-instance ::mem/pointer)]
     (af-diff2 d2 signal 0)
     (af-lt peaks @d2 zero-array 0)  ; Find where d2 < 0
     @peaks)
   ```"
  "af_diff2" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)
