(ns org.soulspace.arrayfire.ffi.c-api.plot
  "Bindings for the ArrayFire plotting and scatter plot functions.
   
   ArrayFire provides GPU-accelerated visualization capabilities through
   Forge, an OpenGL-based graphics library. These functions render line
   plots and scatter plots in interactive windows.
   
   **Plot Types**:
   - **Line Plots**: Connect data points with lines
   - **Scatter Plots**: Display data points with markers (no lines)
   
   **Dimensionality**:
   - **2D Plots**: X-Y plane visualization
   - **3D Plots**: X-Y-Z space visualization
   
   **Data Formats**:
   
   1. **Separate Coordinates** (plot_2d, plot_3d, scatter_2d, scatter_3d):
      - X, Y (and Z for 3D) as separate vectors
      - All must be same length
      - Most explicit and readable
   
   2. **Combined Matrix** (plot_nd, scatter_nd):
      - Single array of shape [n, order] where order = 2 or 3
      - [n, 2] for 2D: rows are (x, y) points
      - [n, 3] for 3D: rows are (x, y, z) points
   
   **Marker Types** (for scatter plots):
   - AF_MARKER_NONE (0): No markers (line plot style)
   - AF_MARKER_POINT (1): Small dots
   - AF_MARKER_CIRCLE (2): Circle outline
   - AF_MARKER_SQUARE (3): Square outline
   - AF_MARKER_TRIANGLE (4): Triangle outline
   - AF_MARKER_CROSS (5): X-shaped cross
   - AF_MARKER_PLUS (6): Plus sign
   - AF_MARKER_STAR (7): Star shape
   
   **Window and Cell Properties**:
   
   The `props` parameter (af_cell*) controls rendering location:
   - Row/column in multi-chart grid layout
   - Chart title
   - Set row=-1, col=-1 for single chart in window
   
   **Usage Pattern**:
   
   ```clojure
   ;; Create window
   (let [window-ptr (mem/alloc-pointer ::mem/pointer)]
     (af-create-window window-ptr 800 600 \"My Plot\")
     (let [window (mem/read-pointer window-ptr ::mem/pointer)]
       
       ;; Create data
       (af-randu x-ptr 1 dims-100 af-dtype-f32)
       (af-randu y-ptr 1 dims-100 af-dtype-f32)
       
       ;; Setup cell properties
       (let [props (create-cell-struct -1 -1 \"Data Plot\")]
         
         ;; Draw 2D line plot
         (af-draw-plot-2d window x y props)
         
         ;; Or draw 2D scatter plot with circle markers
         (af-draw-scatter-2d window x y 2 props))
       
       ;; Show and clean up
       (af-show window)
       (af-destroy-window window)))
   ```
   
   **Performance**:
   - GPU-accelerated rendering
   - Interactive: zoom, pan, rotate (3D)
   - Multiple charts per window with grid layout
   - Real-time updates for animations
   
   **Type Support**:
   - Numeric types: f32, s32, u32, s16, u16, s8, u8
   - f32 recommended for smooth visualization
   - Integer types automatically converted for rendering
   
   **Notes**:
   - Requires Forge graphics library
   - Windows must be created before plotting
   - Axes auto-scale to data unless manually set
   - Color defaults to ArrayFire orange (0.929, 0.529, 0.212)
   
   See also:
   - Window management functions (af-create-window, af-show, etc.)
   - Image rendering functions (af-draw-image)
   - Histogram functions (af-draw-hist)"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; 2D/3D Plot functions (current API)

;; af_err af_draw_plot_nd(const af_window wind, const af_array P, const af_cell* const props)
(defcfn af-draw-plot-nd
  "Draw a 2D or 3D line plot from a combined coordinate matrix.
   
   Renders data points connected by lines. Dimensionality (2D or 3D)
   is determined by the second dimension of the input array.
   
   Parameters:
   - wind: Window handle from af-create-window
   - P: Input array of shape [n, order] where:
     * [n, 2] for 2D plots (x, y coordinates)
     * [n, 3] for 3D plots (x, y, z coordinates)
   - props: Cell properties pointer (af_cell*) for grid position and title
   
   Example:
   ```clojure
   ;; 2D plot: data is [100, 2] array
   (let [data (create-2d-points 100)]
     (af-draw-plot-nd window data props))
   
   ;; 3D plot: data is [100, 3] array
   (let [data (create-3d-points 100)]
     (af-draw-plot-nd window data props))
   ```
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-draw-plot-2d: Separate X, Y vectors for 2D
   - af-draw-plot-3d: Separate X, Y, Z vectors for 3D"
  "af_draw_plot_nd" [::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_draw_plot_2d(const af_window wind, const af_array X, const af_array Y, const af_cell* const props)
(defcfn af-draw-plot-2d
  "Draw a 2D line plot from separate X and Y coordinate vectors.
   
   Renders 2D line plot connecting (X[i], Y[i]) points.
   
   Parameters:
   - wind: Window handle
   - X: X-axis coordinates (vector)
   - Y: Y-axis coordinates (vector, same length as X)
   - props: Cell properties pointer for layout and title
   
   Example:
   ```clojure
   ;; Plot y = sin(x)
   (let [x (linspace 0 (* 2 Math/PI) 100)
         y (sin x)]
     (af-draw-plot-2d window x y props))
   ```
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-draw-plot-nd: Combined matrix format
   - af-draw-scatter-2d: Scatter plot version"
  "af_draw_plot_2d" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_draw_plot_3d(const af_window wind, const af_array X, const af_array Y, const af_array Z, const af_cell* const props)
(defcfn af-draw-plot-3d
  "Draw a 3D line plot from separate X, Y, and Z coordinate vectors.
   
   Renders 3D line plot connecting (X[i], Y[i], Z[i]) points in 3D space.
   
   Parameters:
   - wind: Window handle
   - X: X-axis coordinates (vector)
   - Y: Y-axis coordinates (vector, same length as X)
   - Z: Z-axis coordinates (vector, same length as X)
   - props: Cell properties pointer for layout and title
   
   Example:
   ```clojure
   ;; Plot 3D helix
   (let [t (linspace 0 (* 4 Math/PI) 200)
         x (mul t (cos t))
         y (mul t (sin t))
         z t]
     (af-draw-plot-3d window x y z props))
   ```
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-draw-plot-nd: Combined matrix format
   - af-draw-scatter-3d: Scatter plot version"
  "af_draw_plot_3d" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; Scatter plot functions (current API)

;; af_err af_draw_scatter_nd(const af_window wind, const af_array P, const af_marker_type marker, const af_cell* const props)
(defcfn af-draw-scatter-nd
  "Draw a 2D or 3D scatter plot from a combined coordinate matrix.
   
   Renders data points with markers (no connecting lines).
   
   Parameters:
   - wind: Window handle
   - P: Input array of shape [n, order] where order = 2 or 3
   - marker: Marker type (af_marker_type enum, 0-7)
     * 0=none, 1=point, 2=circle, 3=square, 4=triangle,
     * 5=cross, 6=plus, 7=star
   - props: Cell properties pointer
   
   Example:
   ```clojure
   ;; 2D scatter with circle markers
   (af-draw-scatter-nd window data 2 props)
   ```
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-draw-scatter-2d: Separate X, Y vectors
   - af-draw-scatter-3d: Separate X, Y, Z vectors"
  "af_draw_scatter_nd" [::mem/pointer ::mem/pointer ::mem/int ::mem/pointer] ::mem/int)

;; af_err af_draw_scatter_2d(const af_window wind, const af_array X, const af_array Y, const af_marker_type marker, const af_cell* const props)
(defcfn af-draw-scatter-2d
  "Draw a 2D scatter plot from separate X and Y coordinate vectors.
   
   Renders 2D scatter plot with markers at (X[i], Y[i]) points.
   
   Parameters:
   - wind: Window handle
   - X: X-axis coordinates (vector)
   - Y: Y-axis coordinates (vector, same length as X)
   - marker: Marker type (0=none, 1=point, 2=circle, 3=square,
     4=triangle, 5=cross, 6=plus, 7=star)
   - props: Cell properties pointer
   
   Example:
   ```clojure
   ;; Scatter plot with triangle markers
   (af-draw-scatter-2d window x y 4 props)
   ```
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-draw-plot-2d: Line plot version
   - af-draw-scatter-nd: Combined matrix format"
  "af_draw_scatter_2d" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/pointer] ::mem/int)

;; af_err af_draw_scatter_3d(const af_window wind, const af_array X, const af_array Y, const af_array Z, const af_marker_type marker, const af_cell* const props)
(defcfn af-draw-scatter-3d
  "Draw a 3D scatter plot from separate X, Y, and Z coordinate vectors.
   
   Renders 3D scatter plot with markers at (X[i], Y[i], Z[i]) points.
   
   Parameters:
   - wind: Window handle
   - X: X-axis coordinates (vector)
   - Y: Y-axis coordinates (vector, same length as X)
   - Z: Z-axis coordinates (vector, same length as X)
   - marker: Marker type (0-7, see af_marker_type enum)
   - props: Cell properties pointer
   
   Example:
   ```clojure
   ;; 3D scatter with star markers
   (af-draw-scatter-3d window x y z 7 props)
   ```
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-draw-plot-3d: Line plot version
   - af-draw-scatter-nd: Combined matrix format"
  "af_draw_scatter_3d" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/pointer] ::mem/int)

;; Deprecated plot functions (for backward compatibility)

;; af_err af_draw_plot(const af_window wind, const af_array X, const af_array Y, const af_cell* const props)
(defcfn af-draw-plot
  "Draw a 2D line plot from X and Y vectors (DEPRECATED).
   
   **DEPRECATED**: Use af-draw-plot-2d instead.
   
   This function is provided for backward compatibility only.
   
   Parameters:
   - wind: Window handle
   - X: X-axis coordinates (vector)
   - Y: Y-axis coordinates (vector)
   - props: Cell properties pointer
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-draw-plot-2d: Preferred replacement"
  "af_draw_plot" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_draw_plot3(const af_window wind, const af_array P, const af_cell* const props)
(defcfn af-draw-plot3
  "Draw a 3D line plot from combined coordinate matrix (DEPRECATED).
   
   **DEPRECATED**: Use af-draw-plot-nd or af-draw-plot-3d instead.
   
   Accepts flexible input formats:
   - [n, 3] matrix: n points with (x, y, z) coordinates
   - [3, n] matrix: transposed format
   - [3n, 1] vector: flattened (x, y, z) triplets
   
   Parameters:
   - wind: Window handle
   - P: Input array (see formats above)
   - props: Cell properties pointer
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-draw-plot-nd: Preferred replacement for matrix input
   - af-draw-plot-3d: Preferred replacement for separate vectors"
  "af_draw_plot3" [::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; Deprecated scatter functions (for backward compatibility)

;; af_err af_draw_scatter(const af_window wind, const af_array X, const af_array Y, const af_marker_type marker, const af_cell* const props)
(defcfn af-draw-scatter
  "Draw a 2D scatter plot from X and Y vectors (DEPRECATED).
   
   **DEPRECATED**: Use af-draw-scatter-2d instead.
   
   This function is provided for backward compatibility only.
   
   Parameters:
   - wind: Window handle
   - X: X-axis coordinates (vector)
   - Y: Y-axis coordinates (vector)
   - marker: Marker type (0-7)
   - props: Cell properties pointer
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-draw-scatter-2d: Preferred replacement"
  "af_draw_scatter" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/pointer] ::mem/int)

;; af_err af_draw_scatter3(const af_window wind, const af_array P, const af_marker_type marker, const af_cell* const props)
(defcfn af-draw-scatter3
  "Draw a 3D scatter plot from combined coordinate matrix (DEPRECATED).
   
   **DEPRECATED**: Use af-draw-scatter-nd or af-draw-scatter-3d instead.
   
   Accepts flexible input formats:
   - [n, 3] matrix: n points with (x, y, z) coordinates
   - [3, n] matrix: transposed format
   - [3n, 1] vector: flattened (x, y, z) triplets
   
   Parameters:
   - wind: Window handle
   - P: Input array (see formats above)
   - marker: Marker type (0-7)
   - props: Cell properties pointer
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-draw-scatter-nd: Preferred replacement for matrix input
   - af-draw-scatter-3d: Preferred replacement for separate vectors"
  "af_draw_scatter3" [::mem/pointer ::mem/pointer ::mem/int ::mem/pointer] ::mem/int)
