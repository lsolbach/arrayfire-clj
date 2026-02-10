(ns org.soulspace.arrayfire.ffi.c-api.surface
  "ArrayFire FFI bindings for 3D surface plotting.

  Surface plots visualize 3D data as a continuous surface in 3D space, where
  z-values define the height of the surface at each (x,y) coordinate. This is
  essential for visualizing mathematical functions, terrain data, and
  multi-dimensional relationships.

  ## Mathematical Foundation

  A surface plot represents a function z = f(x, y) where:
  - **x-axis**: Independent variable (horizontal)
  - **y-axis**: Independent variable (horizontal)
  - **z-axis**: Dependent variable (vertical/height)

  The surface S is defined by a grid of points:
  ```
  S = {(x_i, y_j, z_{ij}) | i ∈ [0,m), j ∈ [0,n)}
  ```

  where z_{ij} = f(x_i, y_j) for some function f.

  ## Input Formats

  ### Format 1: Meshgrid (Full X,Y Specification)
  ```
  xVals: m×n matrix or vector of length m
  yVals: m×n matrix or vector of length n
  S:     m×n matrix of z-values
  ```

  If xVals and yVals are vectors:
  - xVals (length m) → tiled along y-dimension to create m×n grid
  - yVals (length n) → tiled along x-dimension to create m×n grid
  - S (m×n) → height values at each grid point

  ### Format 2: Regular Grid (Implicit)
  ```
  S: m×n matrix (x,y coordinates implicit: 0..m-1, 0..n-1)
  ```

  ## Coordinate System

  **Standard Mathematical Convention:**
  ```
  z (height)
  ↑
  |
  |     / y
  |    /
  |   /
  |  /
  | /
  |/________→ x
  ```

  ## Use Cases

  ### Mathematics & Education
  - **Function Visualization**: Plot z = sin(√(x² + y²)) (radial waves)
  - **Optimization**: Visualize cost/error surfaces
  - **Calculus**: Explore partial derivatives, gradients
  - **Complex Functions**: Magnitude/phase of complex-valued functions

  ### Scientific Computing
  - **Terrain/Topography**: Elevation maps, bathymetry
  - **Physics Simulations**: Wave propagation, electromagnetic fields
  - **Quantum Mechanics**: Probability density surfaces
  - **Chemistry**: Molecular orbitals, potential energy surfaces

  ### Engineering & Analysis
  - **Response Surfaces**: System output as function of two parameters
  - **Heat Maps (3D)**: Temperature distribution
  - **Signal Processing**: 2D frequency response, spectrograms
  - **Finite Element Analysis**: Stress/strain distributions

  ### Data Science
  - **Multi-variate Analysis**: Relationships between 3 variables
  - **Interpolation Visualization**: Surface fitting quality
  - **Machine Learning**: Loss landscapes, decision boundaries
  - **Statistical Surfaces**: Probability density, regression surfaces

  ## Rendering Features

  ### Surface Representation
  - **Mesh**: Grid of interconnected quadrilaterals
  - **Color**: Mapped to height (z-value) or custom colormap
  - **Shading**: Smooth interpolation between grid points
  - **Transparency**: Optional for overlapping surfaces

  ### Interaction (Window API)
  - **Rotation**: Mouse drag to rotate viewpoint
  - **Zoom**: Scroll wheel or pinch to zoom
  - **Pan**: Shift+drag to translate view
  - **Axes**: Labeled x, y, z axes with tick marks

  ## Performance Considerations

  ### GPU Acceleration
  - Data stays on GPU until rendering
  - Efficient for large surfaces (e.g., 1000×1000 grids)
  - Real-time updates for animations

  ### Grid Size Impact
  - **Small grids** (10×10 to 100×100): Very fast, coarse detail
  - **Medium grids** (100×100 to 500×500): Good detail, still interactive
  - **Large grids** (500×500 to 2000×2000): High detail, may impact frame rate
  - **Huge grids** (>2000×2000): Consider downsampling or level-of-detail

  ### Memory Usage
  - Float32: m×n × 4 bytes per array (x, y, z = 3 arrays)
  - Float64: m×n × 8 bytes per array
  - Example: 1000×1000 f32 surface ≈ 12 MB

  ## Common Patterns

  ### Pattern 1: Mathematical Function
  ```clojure
  ;; Visualize z = sin(x) * cos(y)
  (let [n 100
        x (af-range [n 1] 0) ;; 0 to n-1
        y (af-range [1 n] 0)
        ;; Scale to [-2π, 2π]
        x-scaled (af-mul x (/ (* 4 Math/PI) n))
        y-scaled (af-mul y (/ (* 4 Math/PI) n))
        ;; Compute surface
        z (af-mul (af-sin x-scaled) (af-cos y-scaled))]
    (af-draw-surface window x-scaled y-scaled z props))
  ```

  ### Pattern 2: Radial Function
  ```clojure
  ;; Mexican hat wavelet: z = (1 - r²/2) * exp(-r²/2)
  (let [n 100
        x (af-range [n n] 0)
        y (af-range [n n] 1)
        ;; Center and scale
        x-centered (af-sub x (/ n 2))
        y-centered (af-sub y (/ n 2))
        ;; Radius
        r-squared (af-add (af-pow x-centered 2) (af-pow y-centered 2))
        r (af-sqrt r-squared)
        ;; Mexican hat
        z (af-mul (af-sub 1 (af-div r-squared 2))
                  (af-exp (af-div (af-neg r-squared) 2)))]
    (af-draw-surface window x-centered y-centered z props))
  ```

  ### Pattern 3: Data Surface
  ```clojure
  ;; Interpolated measurement data
  (let [x-coords (af-create-array x-data [m])      ;; measurement x-positions
        y-coords (af-create-array y-data [n])      ;; measurement y-positions
        z-values (af-create-array measurements [m n])] ;; measured values
    (af-draw-surface window x-coords y-coords z-values props))
  ```

  ### Pattern 4: Parametric Surface
  ```clojure
  ;; Sphere: x = r*cos(θ)*sin(φ), y = r*sin(θ)*sin(φ), z = r*cos(φ)
  (let [n 50
        theta (af-range [n n] 0)  ;; azimuthal angle
        phi (af-range [n n] 1)    ;; polar angle
        ;; Scale to [0, 2π] and [0, π]
        theta-scaled (af-mul theta (/ (* 2 Math/PI) n))
        phi-scaled (af-mul phi (/ Math/PI n))
        ;; Sphere equations
        x (af-mul (af-cos theta-scaled) (af-sin phi-scaled))
        y (af-mul (af-sin theta-scaled) (af-sin phi-scaled))
        z (af-cos phi-scaled)]
    (af-draw-surface window x y z props))
  ```

  ## Comparison with Other Plots

  | Plot Type | Dimensionality | Use Case |
  |-----------|---------------|----------|
  | Line Plot | 1D (y vs x) | Time series, functions |
  | Scatter | 2D/3D points | Point clouds, correlations |
  | **Surface** | **3D continuous** | **Functions z=f(x,y), fields** |
  | Contour | 2D projection | Level sets, optimization |
  | Vector Field | Direction+magnitude | Flow, gradients |

  ## Integration with ArrayFire Ecosystem

  ### Preprocessing
  - **Smoothing**: Use `af-gaussian-kernel`, `af-convolve` for noisy data
  - **Interpolation**: Use `af-approx1`/`af-approx2` for sparse data
  - **Normalization**: Scale z-values for better visualization

  ### Animation
  ```clojure
  ;; Animated wave propagation
  (loop [t 0]
    (let [phase (* 2 Math/PI t 0.01)
          z (af-mul (af-sin (af-add x phase)) (af-cos y))]
      (af-draw-surface window x y z props)
      (Thread/sleep 16) ;; ~60 FPS
      (recur (inc t))))
  ```

  ### Multi-surface Comparison
  - Use window grid mode to show multiple surfaces side-by-side
  - Set different colormaps via af_cell props

  ## Type Support

  **Supported Types** (all numeric except complex):
  - **Float**: f32 (recommended for performance)
  - **Integers**: s8, s16, s32, u8, u16, u32

  **Not Supported**:
  - Complex (c32, c64) - use magnitude/phase separately
  - Boolean (b8)
  - Float64 on some backends

  ## Window and Cell Properties

  The `af_cell` struct controls rendering:
  - **row, col**: Grid position (-1, -1 for single chart)
  - **title**: Chart title string
  - **cmap**: Color map (e.g., AF_COLORMAP_DEFAULT, AF_COLORMAP_HEAT)

  ## Error Handling

  Common errors:
  - **Dimension mismatch**: Ensure x, y vectors match S dimensions
  - **Invalid window**: Check window is created and not closed
  - **Type mismatch**: All arrays must have same data type
  - **Empty arrays**: Check arrays have non-zero size

  ## Best Practices

  1. **Grid Resolution**: Start with 50×50, increase if needed
  2. **Data Type**: Use f32 for real-time performance
  3. **Colormaps**: Choose based on data (sequential, diverging, qualitative)
  4. **Axes Limits**: Set explicitly for consistent multi-frame animations
  5. **Memory**: Release arrays after rendering with af-release-array

  ## Resources

  - ArrayFire Graphics Docs: https://arrayfire.org/docs/group__graphics__func__draw.htm
  - Forge Backend: https://github.com/arrayfire/forge
  - Examples: ArrayFire examples/graphics directory

  See also:
  - Plot functions: af_draw_plot_2d, af_draw_plot_3d
  - Scatter plots: af_draw_scatter_2d, af_draw_scatter_3d
  - Vector fields: af_draw_vector_field_2d, af_draw_vector_field_3d
  - Image display: af_draw_image
  - Window management: af_create_window, af_set_visibility"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.loader]))

;;
;; Surface Plotting Functions
;;

(defcfn af-draw-surface
  "Render arrays as a 3D surface plot in a window.

  Visualizes 3D data as a continuous surface where z-values define the height
  of the surface at each (x,y) coordinate. The surface is rendered with shading,
  color mapping, and optional grid lines.

  Parameters:
  - wind: Window handle (af_window)
  - xVals: X-axis coordinates (vector or 2D array)
  - yVals: Y-axis coordinates (vector or 2D array)
  - S: Z-axis values (2D array, m×n)
  - props: Cell properties (af_cell*) for rendering configuration

  Returns:
  Error code indicating success or failure.

  ## Coordinate Interpretation

  ### Vector Inputs (Recommended)
  When xVals and yVals are vectors:
  ```clojure
  xVals: [m] - x coordinates (one per row)
  yVals: [n] - y coordinates (one per column)
  S:     [m n] - z values at each (x,y) point
  ```

  Internally, vectors are tiled to create full meshgrid:
  - xVals → m×n grid (tiled along columns)
  - yVals → m×n grid (tiled along rows)
  - S → m×n grid (already correct shape)

  ### 2D Array Inputs (Advanced)
  When xVals and yVals are 2D arrays:
  ```clojure
  xVals: [m n] - explicit x coordinate at each point
  yVals: [m n] - explicit y coordinate at each point
  S:     [m n] - z value at each point
  ```

  This allows non-uniform grids and parametric surfaces.

  ## Basic Example: Mathematical Function
  ```clojure
  ;; Plot z = sin(x) * cos(y) over [-π, π] × [-π, π]
  (require '[org.soulspace.arrayfire.ffi.core :as af])
  (require '[org.soulspace.arrayfire.ffi.surface :as surf])
  (require '[coffi.mem :as mem])

  ;; Create window
  (let [window (af/af-create-window 800 600 \"Surface Plot\")]
    ;; Generate grid: 100×100 points
    (let [n 100
          ;; X coordinates: -π to π
          x-arr (af/af-range [n 1] 0)
          x-scaled (af/af-mul x-arr (/ (* 2 Math/PI) n))
          x-centered (af/af-sub x-scaled Math/PI)
          
          ;; Y coordinates: -π to π
          y-arr (af/af-range [1 n] 0)
          y-scaled (af/af-mul y-arr (/ (* 2 Math/PI) n))
          y-centered (af/af-sub y-scaled Math/PI)
          
          ;; Z = sin(x) * cos(y)
          z (af/af-mul (af/af-sin x-centered) (af/af-cos y-centered))
          
          ;; Setup cell properties
          props (mem/alloc-instance ::af-cell)
          _ (mem/write-int props 0 -1)  ;; row = -1 (single chart)
          _ (mem/write-int props 4 -1)  ;; col = -1
          _ (mem/write-pointer props 8 (mem/c-string \"z = sin(x)*cos(y)\"))]
      
      ;; Render surface
      (surf/af-draw-surface window x-centered y-centered z props)
      (af/af-show-window window)))
  ```

  ## Example: Radial Wave (Mexican Hat)
  ```clojure
  ;; Create radial wave pattern: z = (1 - r²/2) * exp(-r²/2)
  (let [n 80
        ;; Create coordinate grids
        x (af/af-range [n n] 0)
        y (af/af-range [n n] 1)
        
        ;; Center coordinates
        x-c (af/af-sub x (/ n 2))
        y-c (af/af-sub y (/ n 2))
        
        ;; Compute radius squared
        r2 (af/af-add (af/af-pow x-c 2) (af/af-pow y-c 2))
        
        ;; Mexican hat formula
        term1 (af/af-sub 1.0 (af/af-div r2 2))
        term2 (af/af-exp (af/af-div (af/af-neg r2) 2))
        z (af/af-mul term1 term2)
        
        props (create-cell-props -1 -1 \"Mexican Hat Wavelet\")]
    (surf/af-draw-surface window x-c y-c z props))
  ```

  ## Example: Parametric Surface (Torus)
  ```clojure
  ;; Torus: parameterized by angles θ and φ
  ;; x = (R + r*cos(φ)) * cos(θ)
  ;; y = (R + r*cos(φ)) * sin(θ)
  ;; z = r * sin(φ)
  ;; where R = major radius, r = minor radius
  
  (let [n 60
        R 3.0  ;; major radius
        r 1.0  ;; minor radius
        
        ;; Generate parameter grids
        theta (af/af-range [n n] 0)
        phi (af/af-range [n n] 1)
        
        ;; Scale to [0, 2π]
        theta-s (af/af-mul theta (/ (* 2 Math/PI) n))
        phi-s (af/af-mul phi (/ (* 2 Math/PI) n))
        
        ;; Torus equations
        cos-phi (af/af-cos phi-s)
        sin-phi (af/af-sin phi-s)
        cos-theta (af/af-cos theta-s)
        sin-theta (af/af-sin theta-s)
        
        radius (af/af-add R (af/af-mul r cos-phi))
        x (af/af-mul radius cos-theta)
        y (af/af-mul radius sin-theta)
        z (af/af-mul r sin-phi)
        
        props (create-cell-props -1 -1 \"Torus Surface\")]
    (surf/af-draw-surface window x y z props))
  ```

  ## Example: Data Visualization
  ```clojure
  ;; Visualize 2D measurement data as surface
  (let [measurements [[1.2 1.5 1.8 2.1]
                      [1.4 1.9 2.3 2.6]
                      [1.3 1.7 2.2 2.7]
                      [1.1 1.6 2.1 2.5]]
        z-data (af/af-create-array (flatten measurements) [4 4])
        
        ;; X and Y coordinates (measurement positions)
        x-coords (af/af-create-array [0.0 1.0 2.0 3.0] [4])
        y-coords (af/af-create-array [0.0 0.5 1.0 1.5] [4])
        
        props (create-cell-props -1 -1 \"Measurement Data\")]
    (surf/af-draw-surface window x-coords y-coords z-data props))
  ```

  ## Example: Animation
  ```clojure
  ;; Animated traveling wave
  (defn animate-wave [window duration-ms]
    (let [n 100
          x (af/af-range [n n] 0)
          y (af/af-range [n n] 1)
          x-scaled (af/af-div x (/ n (* 2 Math/PI)))
          y-scaled (af/af-div y (/ n (* 2 Math/PI)))
          props (create-cell-props -1 -1 \"Traveling Wave\")
          start-time (System/currentTimeMillis)]
      
      (while (< (- (System/currentTimeMillis) start-time) duration-ms)
        ;; Time-varying phase
        (let [t (/ (- (System/currentTimeMillis) start-time) 1000.0)
              phase (* 2 Math/PI t)
              
              ;; z = sin(x + phase) * cos(y)
              x-wave (af/af-add x-scaled phase)
              z (af/af-mul (af/af-sin x-wave) (af/af-cos y-scaled))]
          
          (surf/af-draw-surface window x-scaled y-scaled z props)
          (Thread/sleep 16)  ;; ~60 FPS
          (af/af-release-array z)))))
  
  (animate-wave my-window 10000) ;; 10 second animation
  ```

  ## Grid Mode Example
  ```clojure
  ;; Multiple surfaces in one window
  (let [window (af/af-create-window 1200 600 \"Surface Comparison\")]
    ;; Setup 1×2 grid
    (af/af-set-window-grid window 1 2)
    
    (let [n 50
          x (af/af-range [n n] 0)
          y (af/af-range [n n] 1)
          
          ;; Surface 1: sin(x) * cos(y)
          z1 (af/af-mul (af/af-sin x) (af/af-cos y))
          props1 (create-cell-props 0 0 \"Sin*Cos\")
          
          ;; Surface 2: exp(-(x² + y²))
          r2 (af/af-add (af/af-pow x 2) (af/af-pow y 2))
          z2 (af/af-exp (af/af-neg (af/af-div r2 100)))
          props2 (create-cell-props 0 1 \"Gaussian\")]
      
      (surf/af-draw-surface window x y z1 props1)
      (surf/af-draw-surface window x y z2 props2)
      (af/af-show-window window)))
  ```

  ## Type Requirements

  **All arrays must have the same type**:
  - xVals, yVals, and S must be same af_dtype
  - Supported types: f32, s32, u32, s16, u16, s8, u8
  - f32 recommended for best performance and compatibility

  **Dimension Requirements**:
  - If xVals and yVals are vectors:
    * xVals: [m] or [m 1]
    * yVals: [n] or [1 n]
    * S: [m n]
    * Result: xVals.length * yVals.length == S.elements()
  - If xVals and yVals are 2D:
    * xVals: [m n]
    * yVals: [m n]
    * S: [m n]
    * All must have identical dimensions

  ## Cell Properties Structure

  The af_cell struct configures rendering:
  ```c
  typedef struct {
      int row;              // Grid row (-1 for single chart)
      int col;              // Grid column (-1 for single chart)
      const char* title;    // Chart title
      af_colormap cmap;     // Color map
  } af_cell;
  ```

  **Grid Position**:
  - row = -1, col = -1: Single chart (no grid)
  - row ≥ 0, col ≥ 0: Position in window grid

  **Color Maps** (af_colormap enum):
  - AF_COLORMAP_DEFAULT (0): Rainbow
  - AF_COLORMAP_SPECTRUM (1): Spectral
  - AF_COLORMAP_COLORS (2): Primary colors
  - AF_COLORMAP_RED (3): Red gradient
  - AF_COLORMAP_MOOD (4): Blue-purple
  - AF_COLORMAP_HEAT (5): Black-red-yellow
  - AF_COLORMAP_BLUE (6): Blue gradient

  ## Rendering Details

  **Surface Construction**:
  1. Arrays tiled/reshaped to m×n grids
  2. Joined along first dimension: [3, m*n] (x,y,z rows)
  3. Sent to Forge graphics backend
  4. Rendered as quadrilateral mesh

  **Auto-scaling**:
  - If axes limits not manually set:
    * Computes min/max for x, y, z
    * Rounds to nice values (power-of-2 steps)
    * Sets axes limits automatically
  - Manual override via setAxesLimits (C++ API)

  **Color Mapping**:
  - Default: Color mapped to z-value (height)
  - Higher z → warmer colors (red/yellow)
  - Lower z → cooler colors (blue/purple)
  - Colormap customizable via af_cell.cmap

  ## Performance

  **Optimization Tips**:
  - Use f32 instead of f64 for faster rendering
  - Keep grid size reasonable (50×50 to 200×200 for real-time)
  - Reuse arrays when animating (update in place)
  - Release arrays after rendering to free GPU memory

  **Frame Rate Guidelines**:
  - 50×50 grid: 200+ FPS (very smooth)
  - 100×100 grid: 100+ FPS (smooth)
  - 200×200 grid: 30-60 FPS (acceptable)
  - 500×500 grid: 10-30 FPS (may lag)
  - 1000×1000 grid: <10 FPS (slideshow)

  **Memory Usage** (per surface):
  - f32: 3 × m × n × 4 bytes (x, y, z arrays)
  - f64: 3 × m × n × 8 bytes
  - Example: 100×100 f32 ≈ 120 KB

  ## Error Conditions

  Common errors and solutions:
  - **AF_ERR_ARG (invalid argument)**:
    * Check arrays are not null
    * Verify dimension compatibility
    * Ensure window is valid
  - **AF_ERR_TYPE (type mismatch)**:
    * All arrays must have same dtype
  - **AF_ERR_SIZE (size mismatch)**:
    * Check xVals.length * yVals.length == S.elements() (vector mode)
    * Check xVals.dims == yVals.dims == S.dims (2D mode)
  - **AF_ERR_NOT_SUPPORTED**:
    * Graphics backend not available
    * Install Forge library

  ## Integration Notes

  **Backend Requirements**:
  - Requires Forge graphics library
  - OpenGL or similar graphics API
  - X11/Wayland (Linux), Cocoa (macOS), Win32 (Windows)

  **Thread Safety**:
  - Window operations should be on main thread (platform-dependent)
  - ArrayFire arrays are thread-safe

  **Multiple Windows**:
  - Can create multiple windows
  - Each window maintains independent state
  - Rendering to different windows can be interleaved

  ## Advanced Techniques

  **Custom Colormaps**:
  - Set via af_cell.cmap field
  - Affects how z-values map to colors

  **Multi-layer Surfaces**:
  - Use transparency (alpha channel)
  - Render multiple surfaces in same window
  - Useful for comparing models vs data

  **Contour Lines**:
  - Not directly supported in surface plot
  - Use separate contour plot function

  ## Notes

  - Surface color defaults to green [RGBA: 0.0, 1.0, 0.0, 1.0]
  - Can be customized via Forge API (fg_set_surface_color)
  - Interactive rotation/zoom via mouse (window API)
  - Axes labels and title configurable

  See also:
  - af_draw_surface (ArrayFire C API)
  - Window management: af_create_window, af_destroy_window
  - Grid setup: af_set_window_grid
  - Axes configuration: af_set_axes_limits, af_set_axes_titles
  - Other plot types: af_draw_plot_3d, af_draw_scatter_3d"
  "af_draw_surface" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)
