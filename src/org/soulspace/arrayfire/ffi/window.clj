(ns org.soulspace.arrayfire.ffi.window
  "Bindings for the ArrayFire window and graphics visualization functions.
   
   Window Management and Visualization:
   
   ArrayFire provides a window system for real-time visualization of arrays,
   plots, images, and other graphical representations. These functions leverage
   the Forge graphics library to create interactive, hardware-accelerated
   visualizations.
   
   Key Capabilities:
   
   1. **Window Creation and Management**:
      - Create windows with specified dimensions and titles
      - Control window visibility, position, and size
      - Organize multiple visualizations in grid layouts
      - Handle window lifecycle (create, show, close, destroy)
   
   2. **Axes Configuration**:
      - Set axes limits (computed from data or explicit values)
      - Configure axes titles and labels
      - Customize label formatting (printf-style)
      - Support for 2D and 3D charts
   
   3. **Multi-viewport Support**:
      - Grid-based layout (rows × columns)
      - Cell-specific property targeting via af_cell structure
      - Independent chart configuration per cell
   
   Window Handle (af_window):
   - Opaque handle to underlying Forge window
   - Must be created before use (af-create-window)
   - Should be destroyed when done (af-destroy-window)
   - Single window can contain multiple charts in grid layout
   
   Cell Properties (af_cell):
   - Structure specifying row and column in grid layout
   - Pass NULL or {row: -1, col: -1} for single-chart windows
   - Enables targeting specific cells in multi-viewport setups
   
   Typical Workflow:
   1. Create window with af-create-window
   2. Optionally set up grid layout with af-grid
   3. Configure axes, titles, and formatting
   4. Draw data using rendering functions (image, plot, surface, etc.)
   5. Show window with af-show
   6. Check for close event with af-is-window-closed
   7. Destroy window with af-destroy-window when done
   
   Performance:
   - GPU-accelerated rendering via Forge
   - Efficient buffer swapping for smooth animations
   - Minimal CPU-GPU transfer overhead
   
   Use Cases:
   - Real-time algorithm visualization
   - Interactive data exploration
   - Scientific plotting and charting
   - Image processing pipeline visualization
   - Debugging and development workflows
   
   Note: Window functions require ArrayFire to be built with Forge support.
   If Forge is not available, these functions will return AF_ERR_NOT_SUPPORTED.
   
   See also:
   - Drawing functions: af_draw_image, af_draw_plot, af_draw_surface
   - Vector field rendering in vector_field.clj"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; Window creation and lifecycle

;; af_err af_create_window(af_window* out, const int width, const int height, const char* const title)
(defcfn af-create-window
  "Create a new window for visualization.
   
   Creates a window handle for displaying graphics. The window is not
   immediately visible; use af-show to display it.
   
   Parameters:
   - out: Output pointer for window handle
   - width: Window width in pixels
   - height: Window height in pixels
   - title: Window title string (C string pointer)
   
   Returns window handle that can be used with other graphics functions.
   
   Example:
   ```clojure
   (let [out-ptr (mem/alloc-pointer ::mem/pointer)
         title-ptr (mem/serialize \"My Visualization\" ::mem/c-string)
         _ (af-create-window out-ptr 800 600 title-ptr)
         window (mem/read-pointer out-ptr ::mem/pointer)]
     ;; Use window for rendering
     )
   ```
   
   Returns: AF_SUCCESS or error code
   
   See also:
   - af-destroy-window: Clean up window resources
   - af-show: Display the window"
  "af_create_window" [::mem/pointer ::mem/int ::mem/int ::mem/pointer] ::mem/int)

;; Window properties

;; af_err af_set_position(const af_window wind, const unsigned x, const unsigned y)
(defcfn af-set-position
  "Set window position on screen.
   
   Positions the window at specified screen coordinates (top-left corner).
   
   Parameters:
   - wind: Window handle
   - x: Horizontal position in pixels (from left)
   - y: Vertical position in pixels (from top)
   
   Returns: AF_SUCCESS or error code"
  "af_set_position" [::mem/pointer ::mem/int ::mem/int] ::mem/int)

;; af_err af_set_title(const af_window wind, const char* const title)
(defcfn af-set-title
  "Set or update window title.
   
   Parameters:
   - wind: Window handle
   - title: New title string (C string pointer)
   
   Returns: AF_SUCCESS or error code"
  "af_set_title" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_set_size(const af_window wind, const unsigned w, const unsigned h)
(defcfn af-set-size
  "Set window size.
   
   Resizes the window to specified dimensions.
   
   Parameters:
   - wind: Window handle
   - w: New width in pixels
   - h: New height in pixels
   
   Available in API version 3.1 and later.
   
   Returns: AF_SUCCESS or error code"
  "af_set_size" [::mem/pointer ::mem/int ::mem/int] ::mem/int)

;; Grid layout

;; af_err af_grid(const af_window wind, const int rows, const int cols)
(defcfn af-grid
  "Configure window grid layout for multiple charts.
   
   Sets up a grid layout allowing multiple independent charts in one window.
   Each cell can contain a separate chart with its own axes and data.
   
   Parameters:
   - wind: Window handle
   - rows: Number of rows in grid
   - cols: Number of columns in grid
   
   Grid cells are indexed starting at (0, 0) for top-left.
   Use af_cell structure to target specific cells when rendering.
   
   Example:
   ```clojure
   ;; Create 2x2 grid for four charts
   (af-grid window 2 2)
   ```
   
   Returns: AF_SUCCESS or error code"
  "af_grid" [::mem/pointer ::mem/int ::mem/int] ::mem/int)

;; Axes limits

;; af_err af_set_axes_limits_compute(const af_window window, const af_array x, const af_array y, const af_array z, const bool exact, const af_cell* const props)
(defcfn af-set-axes-limits-compute
  "Set axes limits by computing from data arrays.
   
   Automatically determines and sets axes limits based on min/max values
   in the provided data arrays. Optionally rounds limits for cleaner display.
   
   Parameters:
   - window: Window handle
   - x: Array for computing x-axis limits
   - y: Array for computing y-axis limits
   - z: Array for computing z-axis limits (NULL for 2D charts)
   - exact: If false, rounds to next power of 2 for cleaner limits
   - props: Cell properties (NULL for single chart)
   
   The function computes min and max for each dimension. When exact=false,
   limits are rounded to make the display more aesthetically pleasing.
   
   Available in API version 3.4 and later.
   
   Returns: AF_SUCCESS or error code"
  "af_set_axes_limits_compute" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/pointer] ::mem/int)

;; af_err af_set_axes_limits_2d(const af_window window, const float xmin, const float xmax, const float ymin, const float ymax, const bool exact, const af_cell* const props)
(defcfn af-set-axes-limits-2d
  "Set explicit axes limits for 2D charts.
   
   Directly sets the x and y axes ranges to specified values.
   
   Parameters:
   - window: Window handle
   - xmin: Minimum x-axis value
   - xmax: Maximum x-axis value
   - ymin: Minimum y-axis value
   - ymax: Maximum y-axis value
   - exact: If false, rounds limits for cleaner display
   - props: Cell properties (NULL for single chart)
   
   Use this when you want precise control over the visible range,
   rather than computing from data.
   
   Available in API version 3.4 and later.
   
   Returns: AF_SUCCESS or error code"
  "af_set_axes_limits_2d" [::mem/pointer ::mem/float ::mem/float ::mem/float ::mem/float ::mem/int ::mem/pointer] ::mem/int)

;; af_err af_set_axes_limits_3d(const af_window window, const float xmin, const float xmax, const float ymin, const float ymax, const float zmin, const float zmax, const bool exact, const af_cell* const props)
(defcfn af-set-axes-limits-3d
  "Set explicit axes limits for 3D charts.
   
   Directly sets the x, y, and z axes ranges to specified values.
   
   Parameters:
   - window: Window handle
   - xmin: Minimum x-axis value
   - xmax: Maximum x-axis value
   - ymin: Minimum y-axis value
   - ymax: Maximum y-axis value
   - zmin: Minimum z-axis value
   - zmax: Maximum z-axis value
   - exact: If false, rounds limits for cleaner display
   - props: Cell properties (NULL for single chart)
   
   Available in API version 3.4 and later.
   
   Returns: AF_SUCCESS or error code"
  "af_set_axes_limits_3d" [::mem/pointer ::mem/float ::mem/float ::mem/float ::mem/float ::mem/float ::mem/float ::mem/int ::mem/pointer] ::mem/int)

;; Axes titles and labels

;; af_err af_set_axes_titles(const af_window window, const char* const xtitle, const char* const ytitle, const char* const ztitle, const af_cell* const props)
(defcfn af-set-axes-titles
  "Set titles for chart axes.
   
   Configures text labels for each axis. Pass NULL to ztitle for 2D charts.
   
   Parameters:
   - window: Window handle
   - xtitle: X-axis title (C string pointer)
   - ytitle: Y-axis title (C string pointer)
   - ztitle: Z-axis title (C string pointer, NULL for 2D)
   - props: Cell properties (NULL for single chart)
   
   The ztitle parameter determines chart dimensionality:
   - NULL: Creates/targets 2D chart
   - Non-NULL: Creates/targets 3D chart
   
   Available in API version 3.4 and later.
   
   Returns: AF_SUCCESS or error code"
  "af_set_axes_titles" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_set_axes_label_format(const af_window window, const char* const xformat, const char* const yformat, const char* const zformat, const af_cell* const props)
(defcfn af-set-axes-label-format
  "Set printf-style format strings for axes labels.
   
   Customizes how numeric values are displayed on axes using printf format
   specifiers. Default format is \"%4.1f\".
   
   Parameters:
   - window: Window handle
   - xformat: Printf format for x-axis labels (e.g., \"%.2f\", \"%d\")
   - yformat: Printf format for y-axis labels
   - zformat: Printf format for z-axis labels (NULL for 2D)
   - props: Cell properties (NULL for single chart)
   
   Example formats:
   - \"%.2f\": 2 decimal places (1.23)
   - \"%d\": Integers (42)
   - \"%.2e\": Scientific notation (1.23e+05)
   - \"%6.3f\": 6 total width, 3 decimals ( 1.234)
   
   Available in API version 3.7 and later.
   
   Returns: AF_SUCCESS or error code"
  "af_set_axes_label_format" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; Window display and lifecycle

;; af_err af_show(const af_window wind)
(defcfn af-show
  "Display window and swap rendering buffers.
   
   Makes the window visible and presents the current frame buffer.
   This function should be called after all rendering operations to
   display the results.
   
   Parameters:
   - wind: Window handle
   
   Typical usage in rendering loop:
   1. Draw data (image, plot, surface, etc.)
   2. Call af-show to display
   3. Check af-is-window-closed for exit condition
   
   Returns: AF_SUCCESS or error code"
  "af_show" [::mem/pointer] ::mem/int)

;; af_err af_is_window_closed(bool* out, const af_window wind)
(defcfn af-is-window-closed
  "Check if window has been marked for closing.
   
   Queries whether the user has requested to close the window
   (typically by pressing ESC or clicking close button).
   
   Parameters:
   - out: Output pointer for boolean result (0=open, 1=closed)
   - wind: Window handle
   
   Use in rendering loops to detect when to stop:
   ```clojure
   (loop []
     ;; Render frame
     (af-show window)
     (let [closed-ptr (mem/alloc-pointer ::mem/int)
           _ (af-is-window-closed closed-ptr window)
           closed? (not= 0 (mem/read-pointer closed-ptr ::mem/int))]
       (when-not closed?
         (recur))))
   ```
   
   Returns: AF_SUCCESS or error code"
  "af_is_window_closed" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_set_visibility(const af_window wind, const bool is_visible)
(defcfn af-set-visibility
  "Show or hide window.
   
   Controls window visibility without destroying it.
   
   Parameters:
   - wind: Window handle
   - is-visible: True to show, false to hide (use 1/0 for bool)
   
   Available in API version 3.3 and later.
   
   Returns: AF_SUCCESS or error code"
  "af_set_visibility" [::mem/pointer ::mem/int] ::mem/int)

;; af_err af_destroy_window(const af_window wind)
(defcfn af-destroy-window
  "Destroy window and free resources.
   
   Releases all resources associated with the window. The window handle
   becomes invalid after this call.
   
   Parameters:
   - wind: Window handle
   
   Should be called when visualization is complete to prevent resource leaks.
   
   Returns: AF_SUCCESS or error code"
  "af_destroy_window" [::mem/pointer] ::mem/int)
