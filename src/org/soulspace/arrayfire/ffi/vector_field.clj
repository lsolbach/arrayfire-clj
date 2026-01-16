(ns org.soulspace.arrayfire.ffi.vector-field
  "Bindings for the ArrayFire vector field visualization functions.
   
   Vector field visualization displays directional data as arrows or vectors
   at specified points in 2D or 3D space. This is essential for visualizing
   flow fields, gradients, forces, velocities, and other directional quantities.
   
   Mathematical Foundation:
   
   A vector field is a function that assigns a vector to each point in space:
   
   **2D Vector Field**:
   F(x, y) = (u(x, y), v(x, y))
   
   where (x, y) are positions and (u, v) are direction components
   
   **3D Vector Field**:
   F(x, y, z) = (u(x, y, z), v(x, y, z), w(x, y, z))
   
   where (x, y, z) are positions and (u, v, w) are direction components
   
   Common Vector Field Properties:
   
   1. **Magnitude**: |F| = √(u² + v² + w²)
   2. **Direction**: Unit vector = F / |F|
   3. **Divergence**: ∇·F = ∂u/∂x + ∂v/∂y + ∂w/∂z
   4. **Curl**: ∇×F (rotation at each point)
   
   Visualization Components:
   
   1. **Arrow Representation**:
      - Tail: Position (x, y) or (x, y, z)
      - Head: Position + scaled direction vector
      - Length: Proportional to magnitude (optional)
      - Color: Can encode magnitude or other properties
   
   2. **Sampling**:
      - Dense fields need subsampling for clarity
      - Uniform grid sampling common
      - Adaptive sampling based on field complexity
   
   3. **Scaling**:
      - Arrow length scaled for visibility
      - Automatic normalization available
      - Manual scaling for control
   
   API Design:
   
   ArrayFire provides three vector field functions:
   
   1. **af-draw-vector-field-nd**: Generic N-dimensional (2D or 3D)
      - Points and directions as [N, D] arrays
      - D = 2 for 2D, D = 3 for 3D
      - Most flexible, single function for both
   
   2. **af-draw-vector-field-3d**: Explicit 3D
      - Separate arrays for x, y, z coordinates
      - Separate arrays for u, v, w components
      - Clearer parameter names
   
   3. **af-draw-vector-field-2d**: Explicit 2D
      - Separate arrays for x, y coordinates
      - Separate arrays for u, v components
      - Simpler 2D-specific interface
   
   Performance Considerations:
   
   - GPU-accelerated rendering via OpenGL/Forge
   - Real-time visualization possible for moderate-sized fields
   - Subsampling recommended for dense fields (>10,000 vectors)
   - Arrow rendering: O(N) where N = number of vectors
   
   Applications:
   
   1. **Fluid Dynamics**:
      - Velocity fields in CFD simulations
      - Vorticity visualization
      - Streamline generation
      - Turbulence analysis
   
   2. **Electromagnetic Fields**:
      - Electric field E(x, y, z)
      - Magnetic field B(x, y, z)
      - Poynting vector (energy flow)
      - Field line visualization
   
   3. **Computer Vision**:
      - Optical flow (motion fields)
      - Gradient fields (edge direction)
      - Displacement fields (registration)
      - Feature motion tracking
   
   4. **Robotics**:
      - Velocity fields for path planning
      - Force fields for control
      - Potential fields for navigation
      - Sensor data visualization
   
   5. **Climate Science**:
      - Wind fields (meteorology)
      - Ocean currents (oceanography)
      - Heat flux visualization
      - Climate model output
   
   6. **Medical Imaging**:
      - Blood flow velocity (ultrasound Doppler)
      - Cardiac motion (MRI)
      - Diffusion tensor imaging (DTI)
      - Tissue deformation
   
   Common Patterns:
   
   ```clojure
   ;; 1. 2D gradient field visualization
   (let [img (create-array image-data [512 512])
         ;; Compute gradient (Sobel operator)
         gx-ptr (mem/alloc-pointer ::mem/pointer)
         gy-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-sobel-operator gx-ptr gy-ptr img 3)
         gx (mem/read-pointer gx-ptr ::mem/pointer)
         gy (mem/read-pointer gy-ptr ::mem/pointer)
         ;; Create sampling grid (subsample for clarity)
         step 16
         [h w] [512 512]
         y-coords (range 0 h step)
         x-coords (range 0 w step)
         y-grid (af-tile (create-array y-coords) [1 (count x-coords)])
         x-grid (af-tile (create-array x-coords) [(count y-coords) 1])
         ;; Sample gradient at grid points
         gx-sampled (af-index gx y-grid x-grid)
         gy-sampled (af-index gy y-grid x-grid)
         ;; Flatten for visualization
         x-flat (af-flat x-grid)
         y-flat (af-flat y-grid)
         gx-flat (af-flat gx-sampled)
         gy-flat (af-flat gy-sampled)
         ;; Visualize
         window (af-create-window 1024 768 \"Gradient Field\")
         props (create-cell-props 0 0)]
     (af-draw-vector-field-2d window x-flat y-flat gx-flat gy-flat props))
   
   ;; 2. 3D electromagnetic field
   (let [[nx ny nz] [50 50 50]
         ;; Generate field positions
         x (af-range [nx ny nz] 0)  ; x coordinates
         y (af-range [nx ny nz] 1)  ; y coordinates
         z (af-range [nx ny nz] 2)  ; z coordinates
         ;; Compute field (e.g., dipole field)
         r-squared (af-add (af-add (af-pow x 2) (af-pow y 2)) (af-pow z 2))
         r-cubed (af-pow r-squared 1.5)
         Ex (af-div x r-cubed)
         Ey (af-div y r-cubed)
         Ez (af-div z r-cubed)
         ;; Subsample for clarity
         step 5
         x-sub (af-index x (range 0 nx step) (range 0 ny step) (range 0 nz step))
         y-sub (af-index y (range 0 nx step) (range 0 ny step) (range 0 nz step))
         z-sub (af-index z (range 0 nx step) (range 0 ny step) (range 0 nz step))
         Ex-sub (af-index Ex (range 0 nx step) (range 0 ny step) (range 0 nz step))
         Ey-sub (af-index Ey (range 0 nx step) (range 0 ny step) (range 0 nz step))
         Ez-sub (af-index Ez (range 0 nx step) (range 0 ny step) (range 0 nz step))
         ;; Flatten
         x-flat (af-flat x-sub)
         y-flat (af-flat y-sub)
         z-flat (af-flat z-sub)
         Ex-flat (af-flat Ex-sub)
         Ey-flat (af-flat Ey-sub)
         Ez-flat (af-flat Ez-sub)
         window (af-create-window 1024 768 \"E-Field\")
         props (create-cell-props 0 0)]
     (af-draw-vector-field-3d window x-flat y-flat z-flat 
                              Ex-flat Ey-flat Ez-flat props))
   
   ;; 3. Optical flow visualization
   (let [frame1 (create-array img1-data [480 640])
         frame2 (create-array img2-data [480 640])
         ;; Compute optical flow (Lucas-Kanade or similar)
         [flow-x flow-y] (compute-optical-flow frame1 frame2)
         ;; Create visualization grid
         step 20
         [h w] [480 640]
         y-pts (range 0 h step)
         x-pts (range 0 w step)
         y-grid (af-tile (create-array y-pts) [1 (count x-pts)])
         x-grid (af-tile (create-array x-pts) [(count y-pts) 1])
         ;; Sample flow
         u (af-index flow-x y-grid x-grid)
         v (af-index flow-y y-grid x-grid)
         window (af-create-window 1024 768 \"Optical Flow\")
         props (create-cell-props 0 0)]
     (af-draw-vector-field-2d window 
                              (af-flat x-grid) (af-flat y-grid)
                              (af-flat u) (af-flat v) props))
   
   ;; 4. Using nd version with combined arrays
   (let [[n d] [1000 2]  ; 1000 points in 2D
         ;; Points: [N, 2] array with (x, y) coordinates
         points (create-array point-data [n d])
         ;; Directions: [N, 2] array with (u, v) components
         directions (create-array direction-data [n d])
         window (af-create-window 800 600 \"Vector Field\")
         props (create-cell-props 0 0)]
     (af-draw-vector-field-nd window points directions props))
   
   ;; 5. Animated vector field evolution
   (let [window (af-create-window 1024 768 \"Time-Varying Field\")
         props (create-cell-props 0 0)
         [nx ny] [40 40]
         x-grid (create-meshgrid-x nx ny)
         y-grid (create-meshgrid-y nx ny)]
     (doseq [t (range 0 100 0.1)]
       ;; Compute time-varying field
       (let [u (af-mul (af-sin (af-add x-grid t)) (af-cos y-grid))
             v (af-mul (af-cos x-grid) (af-sin (af-add y-grid t)))
             x-flat (af-flat x-grid)
             y-flat (af-flat y-grid)
             u-flat (af-flat u)
             v-flat (af-flat v)]
         (af-draw-vector-field-2d window x-flat y-flat u-flat v-flat props)
         (Thread/sleep 50))))  ; 20 FPS
   ```
   
   Sampling Strategies:
   
   1. **Uniform Grid**:
      - Regular spacing in x, y, (z)
      - Simple, predictable
      - May miss features in sparse regions
   
   2. **Adaptive Sampling**:
      - Denser sampling where field varies rapidly
      - Based on magnitude or curl
      - More complex but efficient
   
   3. **Random Sampling**:
      - Avoid aliasing artifacts
      - Good for very dense fields
      - Less structured appearance
   
   4. **Critical Point Sampling**:
      - Focus on sources, sinks, vortices
      - Based on vector field topology
      - Highlights important features
   
   Visualization Best Practices:
   
   1. **Arrow Scaling**:
      - Normalize vectors if magnitude varies greatly
      - Scale arrow length for visibility
      - Consider logarithmic scaling for wide magnitude ranges
   
   2. **Density**:
      - Too dense: Cluttered, hard to read
      - Too sparse: Miss important features
      - Typical: 20-100 arrows per dimension
   
   3. **Color Encoding**:
      - Arrow color can encode magnitude
      - Direction-based coloring (HSV wheel)
      - Property-based (temperature, pressure)
   
   4. **Integration with Other Visualizations**:
      - Overlay on images (background context)
      - Combine with streamlines
      - Show alongside scalar fields
   
   Window and Cell Properties:
   
   - Window: Created with af-create-window
   - Cell props: Grid position (row, col) for multi-plot layouts
   - Can display multiple vector fields in same window
   - Interactive rotation/zoom for 3D fields
   
   Type Support:
   - Input arrays: f32, f64 (float types)
   - All coordinate and direction arrays must have same type
   - Window handle: Opaque pointer type
   
   Related Visualizations:
   - Streamlines: Integral curves following field
   - Line Integral Convolution (LIC): Texture-based field visualization
   - Glyph-based: 3D glyphs oriented by field
   - Particle advection: Animated particles following flow
   
   See also:
   - af-create-window: Create visualization window
   - af-draw-plot, af-draw-plot3: Plot functions
   - af-draw-image: Image display
   - af-draw-surface: Surface visualization"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; Vector field visualization

;; af_err af_draw_vector_field_nd(const af_window wind, const af_array points, const af_array directions, const af_cell* const props)
(defcfn af-draw-vector-field-nd
  "Draw N-dimensional (2D or 3D) vector field in a window.
   
   Generic vector field visualization that handles both 2D and 3D cases.
   Dimensionality is determined by the number of columns in the input arrays.
   
   Parameters:
   - wind: Window handle (from af-create-window)
   - points: Position array [N, D] where N = number of vectors, D = dimensions
     * D = 2: 2D vector field (columns are x, y)
     * D = 3: 3D vector field (columns are x, y, z)
   - directions: Direction array [N, D] with vector components
     * D = 2: Components are (u, v)
     * D = 3: Components are (u, v, w)
   - props: Cell properties pointer for grid layout (af_cell struct)
     * Specifies row/col position in multi-plot grid
     * NULL or {-1, -1} for single plot
   
   Array layout:
   ```
   points = [[x₁, y₁],      directions = [[u₁, v₁],
             [x₂, y₂],                    [u₂, v₂],
             ...                          ...
             [xₙ, yₙ]]                    [uₙ, vₙ]]
   
   Or for 3D:
   points = [[x₁, y₁, z₁],  directions = [[u₁, v₁, w₁],
             [x₂, y₂, z₂],                [u₂, v₂, w₂],
             ...                          ...
             [xₙ, yₙ, zₙ]]                [uₙ, vₙ, wₙ]]
   ```
   
   Rendering behavior:
   - Each row defines one vector
   - Arrow drawn from point[i] in direction[i]
   - Arrow length proportional to |direction[i]|
   - Automatic axis scaling based on data range
   - Default arrow color: ArrayFire blue (0.13, 0.173, 0.263)
   
   Requirements:
   - points and directions must have same shape [N, D]
   - D must be 2 or 3
   - Arrays must be 2D (even for single vector, use [1, D])
   - Type must be f32 or f64
   
   Performance:
   - GPU-accelerated rendering via Forge
   - Real-time updates possible for N < 10,000
   - For larger N, consider subsampling
   - Arrow rendering: O(N) complexity
   
   Example (2D gradient field):
   ```clojure
   ;; Visualize gradient of an image
   (let [;; Compute gradients
         gx (compute-gradient-x img)
         gy (compute-gradient-y img)
         ;; Create sampling grid (subsample dense gradient)
         step 16
         [h w] (af-get-dims img)
         n-rows (quot h step)
         n-cols (quot w step)
         n-points (* n-rows n-cols)
         ;; Generate point coordinates
         y-indices (af-range [n-rows] 0)
         x-indices (af-range [n-cols] 0)
         y-grid (af-tile y-indices [1 n-cols])
         x-grid (af-tile x-indices [n-rows 1])
         y-flat (af-mul (af-flat y-grid) step)
         x-flat (af-mul (af-flat x-grid) step)
         ;; Combine into [N, 2] arrays
         points (af-join 1 (af-moddims x-flat [n-points 1])
                          (af-moddims y-flat [n-points 1]))
         ;; Sample directions at these points
         gx-sampled (af-flat (af-index gx y-grid x-grid))
         gy-sampled (af-flat (af-index gy y-grid x-grid))
         directions (af-join 1 (af-moddims gx-sampled [n-points 1])
                              (af-moddims gy-sampled [n-points 1]))
         ;; Visualize
         window (af-create-window 1024 768 \"Gradient Field\")
         props (mem/alloc ::cell-props)
         _ (mem/serialize props {:row -1 :col -1})]
     (af-draw-vector-field-nd window points directions props))
   ```
   
   Example (3D magnetic field):
   ```clojure
   ;; Dipole magnetic field
   (let [[nx ny nz] [30 30 30]
         total (* nx ny nz)
         ;; Generate 3D grid
         x (af-flat (af-range [nx ny nz] 0))
         y (af-flat (af-range [nx ny nz] 1))
         z (af-flat (af-range [nx ny nz] 2))
         ;; Center coordinates
         x (af-sub x (/ nx 2))
         y (af-sub y (/ ny 2))
         z (af-sub z (/ nz 2))
         ;; Compute dipole field components
         r-squared (af-add (af-add (af-mul x x) (af-mul y y)) (af-mul z z))
         r-fifth (af-pow r-squared 2.5)
         ;; B_dipole ∝ (3(m·r)r - r²m) / r⁵
         ;; Simplified for m along z-axis
         Bx (af-div (af-mul x z) r-fifth)
         By (af-div (af-mul y z) r-fifth)
         Bz (af-div (af-sub (af-mul 3 (af-mul z z)) r-squared) r-fifth)
         ;; Subsample for clarity
         step 3
         keep-idx (af-range (quot total step) 0)
         keep-idx (af-mul keep-idx step)
         x-sub (af-index x keep-idx)
         y-sub (af-index y keep-idx)
         z-sub (af-index z keep-idx)
         Bx-sub (af-index Bx keep-idx)
         By-sub (af-index By keep-idx)
         Bz-sub (af-index Bz keep-idx)
         ;; Combine into [N, 3] arrays
         n-sub (af-get-elements x-sub)
         points (af-join 1 (af-join 1 (af-moddims x-sub [n-sub 1])
                                       (af-moddims y-sub [n-sub 1]))
                          (af-moddims z-sub [n-sub 1]))
         directions (af-join 1 (af-join 1 (af-moddims Bx-sub [n-sub 1])
                                          (af-moddims By-sub [n-sub 1]))
                              (af-moddims Bz-sub [n-sub 1]))
         window (af-create-window 1024 768 \"B-Field\")
         props (mem/alloc ::cell-props)
         _ (mem/serialize props {:row -1 :col -1})]
     (af-draw-vector-field-nd window points directions props))
   ```
   
   Advantages of nd version:
   - Single function for 2D and 3D
   - Compact data representation
   - Easy to generate from matrix operations
   - Natural for array-oriented processing
   
   Disadvantages:
   - Less intuitive parameter names
   - Requires array joining/reshaping
   - Less clear dimensionality at call site
   
   Use cases:
   - Generic vector field code (works for 2D or 3D)
   - Data already in [N, D] format
   - Functional/generic implementations
   - When dimensionality determined at runtime
   
   Returns:
   ArrayFire error code (AF_SUCCESS on success)
   
   See also:
   - af-draw-vector-field-2d: Explicit 2D version
   - af-draw-vector-field-3d: Explicit 3D version
   - af-create-window: Create visualization window"
  "af_draw_vector_field_nd" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_draw_vector_field_3d(const af_window wind, const af_array xPoints, const af_array yPoints, const af_array zPoints, const af_array xDirs, const af_array yDirs, const af_array zDirs, const af_cell* const props)
(defcfn af-draw-vector-field-3d
  "Draw 3D vector field with separate coordinate arrays.
   
   Explicit 3D vector field visualization with individual arrays for each
   coordinate and direction component. More intuitive parameter names than
   the nd version.
   
   Parameters:
   - wind: Window handle (from af-create-window)
   - xPoints: X-coordinates of vector origins (1D array, length N)
   - yPoints: Y-coordinates of vector origins (1D array, length N)
   - zPoints: Z-coordinates of vector origins (1D array, length N)
   - xDirs: X-components of directions (1D array, length N)
   - yDirs: Y-components of directions (1D array, length N)
   - zDirs: Z-components of directions (1D array, length N)
   - props: Cell properties pointer for grid layout
   
   Vector representation:
   - Vector i originates at point (xPoints[i], yPoints[i], zPoints[i])
   - Vector i points in direction (xDirs[i], yDirs[i], zDirs[i])
   - Arrow length proportional to √(xDirs² + yDirs² + zDirs²)
   
   Requirements:
   - All 6 arrays must have same length N
   - All arrays should be 1D vectors
   - All arrays must have same type (f32 or f64)
   - N vectors will be rendered
   
   Rendering:
   - 3D chart with interactive rotation/zoom
   - Arrows rendered as 3D objects
   - Automatic axis limits unless overridden
   - Perspective projection for depth perception
   
   Performance:
   - Optimal for N < 10,000 vectors
   - For dense fields, subsample before visualization
   - GPU-accelerated 3D rendering
   - Interactive frame rate depends on N
   
   Example (Velocity field in fluid simulation):
   ```clojure
   ;; CFD velocity field visualization
   (let [;; Grid dimensions
         [nx ny nz] [40 40 40]
         ;; Create coordinate arrays
         x (af-flat (af-range [nx ny nz] 0))
         y (af-flat (af-range [nx ny nz] 1))
         z (af-flat (af-range [nx ny nz] 2))
         ;; Compute velocity field (example: rotating flow)
         ;; v = ω × r (angular velocity cross position)
         omega-x 0.0
         omega-y 0.0
         omega-z 1.0
         ;; v_x = ω_y*z - ω_z*y
         ;; v_y = ω_z*x - ω_x*z
         ;; v_z = ω_x*y - ω_y*x
         vx (af-mul omega-z (af-neg y))
         vy (af-mul omega-z x)
         vz (create-constant-array 0.0 (* nx ny nz))
         ;; Subsample for visualization
         step 4
         n-total (* nx ny nz)
         keep-indices (af-range (quot n-total step) 0)
         keep-indices (af-mul keep-indices step)
         x-sub (af-index x keep-indices)
         y-sub (af-index y keep-indices)
         z-sub (af-index z keep-indices)
         vx-sub (af-index vx keep-indices)
         vy-sub (af-index vy keep-indices)
         vz-sub (af-index vz keep-indices)
         ;; Visualize
         window (af-create-window 1024 768 \"Velocity Field\")
         props (mem/alloc ::cell-props)
         _ (mem/serialize props {:row -1 :col -1})]
     (af-draw-vector-field-3d window 
                              x-sub y-sub z-sub
                              vx-sub vy-sub vz-sub
                              props))
   ```
   
   Example (Electric field of point charge):
   ```clojure
   ;; E = kq * r / |r|³
   (let [[n-theta n-phi] [20 20]
         radius 5.0
         ;; Spherical sampling
         theta (af-flat (af-range [n-theta n-phi] 0))
         theta (af-div (af-mul theta Math/PI) n-theta)
         phi (af-flat (af-range [n-theta n-phi] 1))
         phi (af-div (af-mul phi 2 Math/PI) n-phi)
         ;; Convert to Cartesian
         x (af-mul radius (af-mul (af-sin theta) (af-cos phi)))
         y (af-mul radius (af-mul (af-sin theta) (af-sin phi)))
         z (af-mul radius (af-cos theta))
         ;; Field components (radial from origin)
         r-cubed (af-pow (af-mul radius radius radius) 1.0)
         Ex (af-div x r-cubed)
         Ey (af-div y r-cubed)
         Ez (af-div z r-cubed)
         ;; Visualize
         window (af-create-window 1024 768 \"E-Field\")
         props (mem/alloc ::cell-props)
         _ (mem/serialize props {:row -1 :col -1})]
     (af-draw-vector-field-3d window x y z Ex Ey Ez props))
   ```
   
   Example (Gradient of 3D scalar field):
   ```clojure
   ;; Visualize ∇f for scalar field f(x,y,z)
   (let [[nx ny nz] [30 30 30]
         ;; Create scalar field (e.g., Gaussian)
         x (af-range [nx ny nz] 0)
         y (af-range [nx ny nz] 1)
         z (af-range [nx ny nz] 2)
         x-centered (af-sub x (/ nx 2))
         y-centered (af-sub y (/ ny 2))
         z-centered (af-sub z (/ nz 2))
         r-squared (af-add (af-add (af-pow x-centered 2)
                                   (af-pow y-centered 2))
                          (af-pow z-centered 2))
         field (af-exp (af-mul -0.1 r-squared))
         ;; Compute gradient using finite differences
         [grad-x grad-y grad-z] (compute-gradient-3d field)
         ;; Subsample
         step 3
         x-sub (af-flat (af-index x (range 0 nx step)
                                    (range 0 ny step)
                                    (range 0 nz step)))
         y-sub (af-flat (af-index y (range 0 nx step)
                                    (range 0 ny step)
                                    (range 0 nz step)))
         z-sub (af-flat (af-index z (range 0 nx step)
                                    (range 0 ny step)
                                    (range 0 nz step)))
         gx-sub (af-flat (af-index grad-x (range 0 nx step)
                                          (range 0 ny step)
                                          (range 0 nz step)))
         gy-sub (af-flat (af-index grad-y (range 0 nx step)
                                          (range 0 ny step)
                                          (range 0 nz step)))
         gz-sub (af-flat (af-index grad-z (range 0 nx step)
                                          (range 0 ny step)
                                          (range 0 nz step)))
         window (af-create-window 1024 768 \"∇f\")
         props (mem/alloc ::cell-props)
         _ (mem/serialize props {:row -1 :col -1})]
     (af-draw-vector-field-3d window 
                              x-sub y-sub z-sub
                              gx-sub gy-sub gz-sub
                              props))
   ```
   
   Advantages:
   - Clearer parameter names (x/y/z, not columns)
   - No array joining/reshaping required
   - Explicit 3D in function name
   - Natural for separately computed components
   
   Interaction:
   - Mouse drag: Rotate view
   - Scroll wheel: Zoom in/out
   - Arrow keys: Pan view
   - R key: Reset view
   
   Use cases:
   - Fluid dynamics (velocity, vorticity)
   - Electromagnetics (E, B, S fields)
   - Mechanical engineering (stress, force)
   - Climate science (wind, ocean currents)
   - Medical imaging (blood flow, diffusion)
   
   Returns:
   ArrayFire error code (AF_SUCCESS on success)
   
   See also:
   - af-draw-vector-field-nd: Generic N-D version
   - af-draw-vector-field-2d: 2D version"
  "af_draw_vector_field_3d" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_draw_vector_field_2d(const af_window wind, const af_array xPoints, const af_array yPoints, const af_array xDirs, const af_array yDirs, const af_cell* const props)
(defcfn af-draw-vector-field-2d
  "Draw 2D vector field with separate coordinate arrays.
   
   Explicit 2D vector field visualization with individual arrays for each
   coordinate and direction component. Simpler interface for 2D-specific use.
   
   Parameters:
   - wind: Window handle (from af-create-window)
   - xPoints: X-coordinates of vector origins (1D array, length N)
   - yPoints: Y-coordinates of vector origins (1D array, length N)
   - xDirs: X-components of directions (1D array, length N)
   - yDirs: Y-components of directions (1D array, length N)
   - props: Cell properties pointer for grid layout
   
   Vector representation:
   - Vector i originates at point (xPoints[i], yPoints[i])
   - Vector i points in direction (xDirs[i], yDirs[i])
   - Arrow length proportional to √(xDirs² + yDirs²)
   
   Requirements:
   - All 4 arrays must have same length N
   - All arrays should be 1D vectors
   - All arrays must have same type (f32 or f64)
   - N vectors will be rendered
   
   Rendering:
   - 2D chart (no depth, no rotation)
   - Arrows rendered as 2D lines with heads
   - Automatic axis limits from data range
   - Orthographic projection (no perspective)
   
   Performance:
   - Very efficient, can handle N < 50,000 vectors
   - For very dense fields, still subsample for clarity
   - GPU-accelerated 2D rendering
   - Fast interactive updates
   
   Example (Optical flow):
   ```clojure
   ;; Motion between consecutive video frames
   (let [frame1 (load-image \"frame001.png\")
         frame2 (load-image \"frame002.png\")
         ;; Compute optical flow (e.g., Lucas-Kanade)
         [flow-x flow-y] (compute-optical-flow frame1 frame2)
         ;; Subsample for visualization
         [h w] (af-get-dims frame1)
         step 20
         n-rows (quot h step)
         n-cols (quot w step)
         ;; Create sampling grid
         y-indices (af-range [n-rows] 0)
         x-indices (af-range [n-cols] 0)
         y-grid (af-tile y-indices [1 n-cols])
         x-grid (af-tile x-indices [n-rows 1])
         ;; Scale to image coordinates
         y-coords (af-mul (af-flat y-grid) step)
         x-coords (af-mul (af-flat x-grid) step)
         ;; Sample flow at these points
         flow-x-sampled (af-flat (af-index flow-x y-grid x-grid))
         flow-y-sampled (af-flat (af-index flow-y y-grid x-grid))
         ;; Visualize
         window (af-create-window 1024 768 \"Optical Flow\")
         props (mem/alloc ::cell-props)
         _ (mem/serialize props {:row -1 :col -1})]
     ;; Optional: display frame1 as background
     (af-draw-image window frame1 props)
     ;; Overlay flow vectors
     (af-draw-vector-field-2d window 
                              x-coords y-coords
                              flow-x-sampled flow-y-sampled
                              props))
   ```
   
   Example (Gradient field of image):
   ```clojure
   ;; Image gradient visualization
   (let [img (load-image \"photo.jpg\")
         ;; Convert to grayscale if needed
         gray (af-rgb2gray img)
         ;; Compute gradient using Sobel
         gx-ptr (mem/alloc-pointer ::mem/pointer)
         gy-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-sobel-operator gx-ptr gy-ptr gray 3)
         gx (mem/read-pointer gx-ptr ::mem/pointer)
         gy (mem/read-pointer gy-ptr ::mem/pointer)
         ;; Create sampling grid
         [h w] (af-get-dims gray)
         step 16
         n-rows (quot h step)
         n-cols (quot w step)
         y-pts (range 0 h step)
         x-pts (range 0 w step)
         y-grid (af-tile (create-array y-pts) [1 n-cols])
         x-grid (af-tile (create-array x-pts) [n-rows 1])
         ;; Sample gradient
         gx-sampled (af-flat (af-index gx y-grid x-grid))
         gy-sampled (af-flat (af-index gy y-grid x-grid))
         x-flat (af-flat x-grid)
         y-flat (af-flat y-grid)
         window (af-create-window 1024 768 \"Gradient\")
         props (mem/alloc ::cell-props)
         _ (mem/serialize props {:row -1 :col -1})]
     (af-draw-vector-field-2d window x-flat y-flat 
                              gx-sampled gy-sampled props))
   ```
   
   Example (Wind field from meteorological data):
   ```clojure
   ;; Visualize wind vectors
   (let [;; Load wind data (u = eastward, v = northward components)
         u-wind (load-wind-data-u)  ; [lat, lon] grid
         v-wind (load-wind-data-v)
         [n-lat n-lon] (af-get-dims u-wind)
         ;; Create lat/lon grids
         lat (af-range [n-lat n-lon] 0)
         lon (af-range [n-lat n-lon] 1)
         ;; Subsample
         step 5
         lat-sub (af-flat (af-index lat (range 0 n-lat step)
                                        (range 0 n-lon step)))
         lon-sub (af-flat (af-index lon (range 0 n-lat step)
                                        (range 0 n-lon step)))
         u-sub (af-flat (af-index u-wind (range 0 n-lat step)
                                         (range 0 n-lon step)))
         v-sub (af-flat (af-index v-wind (range 0 n-lat step)
                                         (range 0 n-lon step)))
         ;; Normalize vectors for better visibility
         magnitude (af-sqrt (af-add (af-pow u-sub 2) (af-pow v-sub 2)))
         scale-factor 2.0
         u-norm (af-div (af-mul u-sub scale-factor) magnitude)
         v-norm (af-div (af-mul v-sub scale-factor) magnitude)
         window (af-create-window 1600 800 \"Wind Field\")
         props (mem/alloc ::cell-props)
         _ (mem/serialize props {:row -1 :col -1})]
     (af-draw-vector-field-2d window lon-sub lat-sub u-norm v-norm props))
   ```
   
   Example (Interactive animation):
   ```clojure
   ;; Time-varying field animation
   (let [window (af-create-window 800 600 \"Animated Field\")
         props (mem/alloc ::cell-props)
         _ (mem/serialize props {:row -1 :col -1})
         [nx ny] [30 30]
         ;; Fixed grid
         x (af-flat (af-range [nx ny] 1))
         y (af-flat (af-range [nx ny] 0))]
     (doseq [t (range 0 100 0.1)]
       ;; Compute time-varying direction field
       ;; Example: rotating and pulsating pattern
       (let [freq 0.5
             phase (* freq t)
             u (af-mul (af-sin (af-add x phase))
                      (af-cos y))
             v (af-mul (af-cos x)
                      (af-sin (af-add y phase)))]
         (af-draw-vector-field-2d window x y u v props)
         (Thread/sleep 50))))  ; ~20 FPS
   ```
   
   Advantages:
   - Simplest interface for 2D
   - Clear x/y naming convention
   - No 3D overhead
   - Ideal for planar data
   
   Common 2D applications:
   - Image gradients (edge direction)
   - Optical flow (motion estimation)
   - 2D fluid simulation (velocity)
   - Heat flux (2D conduction)
   - Stress/strain fields (mechanics)
   - Wind patterns (meteorology)
   - Electric/magnetic fields (2D problems)
   
   Visualization tips:
   - Overlay on image/heatmap for context
   - Color-code by magnitude
   - Normalize if magnitude varies widely
   - Use logarithmic scaling for extreme ranges
   - Annotate critical points (sources, sinks, saddles)
   
   Returns:
   ArrayFire error code (AF_SUCCESS on success)
   
   See also:
   - af-draw-vector-field-nd: Generic N-D version
   - af-draw-vector-field-3d: 3D version
   - af-draw-image: Display image background
   - af-sobel-operator: Compute image gradients"
  "af_draw_vector_field_2d" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)
