(ns org.soulspace.arrayfire.integration.unified-api.graphic
  "Integration of the ArrayFire graphics related FFI bindings with the error
   handling and resource management on the JVM.
   
   This namespace provides idiomatic Clojure wrappers around ArrayFire's Forge
   graphics library, enabling visualization of arrays as images, plots, histograms,
   surfaces, and vector fields.
   
   ArrayFire Graphics System:
   
   The graphics system uses the Forge library for hardware-accelerated rendering.
   It supports windows, subplots, and various plot types for scientific visualization.
   
   Core Concepts:
   
   1. **Windows**: Container for graphics, created with create-window
   2. **Grid Layout**: Organize multiple plots in rows/columns with grid
   3. **Cell Properties**: Optional configuration (position, title, colormap)
   4. **Drawing**: Render arrays as various visualization types
   
   Basic Workflow:
   
   ```clojure
   ;; Create window
   (let [window (create-window 800 600 \"My Visualization\")]
     (try
       ;; Draw content
       (draw-image! window image-array nil)
       
       ;; Display window (blocks until closed)
       (show! window)
       (finally
         (destroy-window! window))))
   ```
   
   Multi-Plot Layout:
   
   ```clojure
   (let [window (create-window 1024 768 \"Multi-Plot\")]
     (try
       ;; Set 2x2 grid
       (grid! window 2 2)
       
       ;; Draw to different cells
       (draw-plot-2d! window x1 y1 (cell-props 0 0))
       (draw-plot-2d! window x2 y2 (cell-props 0 1))
       (draw-hist! window data1 0.0 100.0 (cell-props 1 0))
       (draw-hist! window data2 0.0 100.0 (cell-props 1 1))
       
       (show! window)
       (finally
         (destroy-window! window))))
   ```
   
   Available Visualizations:
   
   - **Images**: draw-image! - Display 2D/3D image arrays
   - **Line Plots**: draw-plot-nd!, draw-plot-2d!, draw-plot-3d!
   - **Scatter Plots**: draw-scatter-nd!, draw-scatter-2d!, draw-scatter-3d!
   - **Histograms**: draw-hist! - Display frequency distributions
   - **Surfaces**: draw-surface! - 3D surface plots
   - **Vector Fields**: draw-vector-field-nd!, draw-vector-field-2d!, draw-vector-field-3d!
   
   Window Management:
   
   - create-window - Allocate window resource
   - set-position! - Position window on screen
   - set-title! - Change window title
   - set-size! - Resize window
   - set-visibility! - Show/hide window
   - is-window-closed? - Check if window was closed by user
   - show! - Display window and wait
   - destroy-window! - Free window resources
   
   Axis Configuration:
   
   - set-axes-limits-compute! - Auto-compute limits from data
   - set-axes-limits-2d! - Set explicit 2D axis ranges
   - set-axes-limits-3d! - Set explicit 3D axis ranges
   - set-axes-titles! - Set axis labels
   - set-axes-label-format! - Set numeric format for labels
   
   Notes:
   
   - Requires ArrayFire built with Forge support
   - Graphics run on GPU for performance
   - Windows must be explicitly destroyed to prevent leaks
   - show! blocks the calling thread until window closes
   - Use grid! for subplot layouts
   - Cell properties (af_cell) control plot positioning in grids"
  (:require [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.c-api.window :as window-ffi]
            [org.soulspace.arrayfire.ffi.c-api.image :as image-ffi]
            [org.soulspace.arrayfire.ffi.c-api.plot :as plot-ffi]
            [org.soulspace.arrayfire.ffi.c-api.hist :as hist-ffi]
            [org.soulspace.arrayfire.ffi.c-api.surface :as surface-ffi]
            [org.soulspace.arrayfire.ffi.c-api.vector-field :as vector-ffi]
            [org.soulspace.arrayfire.integration.base.jvm-integration :as jvm])
  (:import (java.lang.foreign MemorySegment)))

;;;
;;; Window Management
;;;

(defn create-window
  "Create a new graphics window for visualization.
   
   Allocates a window resource that can display various plot types. The
   window is not immediately visible; use show! to display it.
   
   Parameters:
   - width: Window width in pixels (integer)
   - height: Window height in pixels (integer)
   - title: Window title (string)
   
   Returns:
   Long integer representing the window handle (native pointer)
   
   Example:
   ```clojure
   ;; Basic window
   (let [window (create-window 800 600 \"Visualization\")]
     (try
       (draw-image! window img nil)
       (show! window)
       (finally
         (destroy-window! window))))
   
   ;; Full HD window
   (let [window (create-window 1920 1080 \"High Resolution Plot\")]
     ;; ... render content
     (destroy-window! window))
   ```
   
   Notes:
   - Window must be destroyed with destroy-window! to prevent leaks
   - Initial state is hidden; call show! or set-visibility! to display
   - Requires ArrayFire built with Forge graphics support"
  [width height title]
  (let [window-ptr-buf (mem/alloc 8)
        title-c-str (jvm/string->c-string title)]
    (jvm/check! (window-ffi/af-create-window window-ptr-buf 
                                              (int width) 
                                              (int height) 
                                              title-c-str)
                "af-create-window")
    (mem/read-long window-ptr-buf 0)))

(defn set-position!
  "Set window position on screen.
   
   Moves the window to specified screen coordinates. Origin (0,0) is
   the top-left corner of the screen.
   
   Parameters:
   - window: Window handle (long integer)
   - x: Horizontal position in pixels from left edge
   - y: Vertical position in pixels from top edge
   
   Returns:
   nil
   
   Example:
   ```clojure
   ;; Position in top-left
   (set-position! window 100 100)
   
   ;; Center on 1920x1080 screen (approximate)
   (set-position! window 460 240)  ; For 1000x600 window
   ```"
  [window x y]
  (let [window-segment (mem/as-segment window)]
    (jvm/check! (window-ffi/af-set-position window-segment (int x) (int y))
                "af-set-position"))
  nil)

(defn set-title!
  "Set or change the window title.
   
   Updates the window's title bar text.
   
   Parameters:
   - window: Window handle (long integer)
   - title: New title string
   
   Returns:
   nil
   
   Example:
   ```clojure
   (set-title! window \"Processing frame 1/100\")
   (set-title! window \"Computation complete\")
   ```"
  [window title]
  (let [window-segment (mem/as-segment window)
        title-c-str (jvm/string->c-string title)]
    (jvm/check! (window-ffi/af-set-title window-segment title-c-str)
                "af-set-title"))
  nil)

(defn set-size!
  "Resize the window.
   
   Changes the window dimensions.
   
   Parameters:
   - window: Window handle (long integer)
   - width: New width in pixels
   - height: New height in pixels
   
   Returns:
   nil
   
   Example:
   ```clojure
   (set-size! window 1024 768)
   (set-size! window 640 480)  ; Smaller for embedded display
   ```"
  [window width height]
  (let [window-segment (mem/as-segment window)]
    (jvm/check! (window-ffi/af-set-size window-segment (int width) (int height))
                "af-set-size"))
  nil)

(defn grid!
  "Set the window grid layout for multiple subplots.
   
   Divides the window into a grid of rows × cols cells. Each cell can
   contain one plot. Use cell properties when drawing to specify which
   cell receives the content.
   
   Parameters:
   - window: Window handle (long integer)
   - rows: Number of rows in grid
   - cols: Number of columns in grid
   
   Returns:
   nil
   
   Example:
   ```clojure
   ;; 2x2 grid for 4 plots
   (grid! window 2 2)
   (draw-plot-2d! window x1 y1 (cell-props 0 0))
   (draw-plot-2d! window x2 y2 (cell-props 0 1))
   (draw-plot-2d! window x3 y3 (cell-props 1 0))
   (draw-plot-2d! window x4 y4 (cell-props 1 1))
   
   ;; Single row dashboard
   (grid! window 1 3)
   ```
   
   Notes:
   - Default is 1x1 (single plot)
   - Cell indices are 0-based
   - Cell (row, col) specified in cell properties when drawing"
  [window rows cols]
  (let [window-segment (mem/as-segment window)]
    (jvm/check! (window-ffi/af-grid window-segment (int rows) (int cols))
                "af-grid"))
  nil)

(defn show!
  "Display the window and wait for it to close.
   
   Makes the window visible and blocks the calling thread until the
   user closes the window. This is the standard way to display
   visualizations and wait for user interaction.
   
   Parameters:
   - window: Window handle (long integer)
   
   Returns:
   nil
   
   Example:
   ```clojure
   (let [window (create-window 800 600 \"Plot\")]
     (try
       (draw-plot-2d! window x y nil)
       (show! window)  ; Blocks here until window closed
       (println \"Window closed by user\")
       (finally
         (destroy-window! window))))
   ```
   
   Notes:
   - Blocks until window is closed
   - Window becomes visible if previously hidden
   - For non-blocking operation, use set-visibility! and poll is-window-closed?"
  [window]
  (let [window-segment (mem/as-segment window)]
    (jvm/check! (window-ffi/af-show window-segment)
                "af-show"))
  nil)

(defn is-window-closed?
  "Check if the window was closed by the user.
   
   Returns true if the user has closed the window, false otherwise.
   Useful for non-blocking visualization loops.
   
   Parameters:
   - window: Window handle (long integer)
   
   Returns:
   Boolean - true if window is closed
   
   Example:
   ```clojure
   ;; Animation loop
   (let [window (create-window 800 600 \"Animation\")]
     (set-visibility! window true)
     (loop [frame 0]
       (when-not (is-window-closed? window)
         (draw-plot-2d! window (gen-data frame) nil)
         (Thread/sleep 16)  ; ~60 FPS
         (recur (inc frame))))
     (destroy-window! window))
   ```"
  [window]
  (let [closed-buf (mem/alloc 1)
        window-segment (mem/as-segment window)]
    (jvm/check! (window-ffi/af-is-window-closed closed-buf window-segment)
                "af-is-window-closed")
    (not (zero? (mem/read-byte closed-buf 0)))))

(defn set-visibility!
  "Show or hide the window.
   
   Controls window visibility without blocking. Useful for animations
   where you want to update content without blocking the thread.
   
   Parameters:
   - window: Window handle (long integer)
   - visible: Boolean - true to show, false to hide
   
   Returns:
   nil
   
   Example:
   ```clojure
   ;; Show window non-blocking
   (set-visibility! window true)
   
   ;; Hide temporarily
   (set-visibility! window false)
   ;; ... update content ...
   (set-visibility! window true)
   ```"
  [window visible]
  (let [window-segment (mem/as-segment window)]
    (jvm/check! (window-ffi/af-set-visibility window-segment (if visible 1 0))
                "af-set-visibility"))
  nil)

(defn destroy-window!
  "Destroy a window and free its resources.
   
   Releases all resources associated with the window. The window handle
   becomes invalid and must not be used after this call.
   
   Parameters:
   - window: Window handle (long integer)
   
   Returns:
   nil
   
   Example:
   ```clojure
   (let [window (create-window 800 600 \"Plot\")]
     (try
       (draw-plot-2d! window x y nil)
       (show! window)
       (finally
         (destroy-window! window))))
   ```
   
   Notes:
   - Always call in finally block to ensure cleanup
   - Window handle becomes invalid after destruction
   - Safe to call with nil/0 (no-op)"
  [window]
  (when window
    (let [window-segment (mem/as-segment window)]
      (jvm/check! (window-ffi/af-destroy-window window-segment)
                  "af-destroy-window")))
  nil)

;;;
;;; Axis Configuration
;;;

(defn set-axes-limits-compute!
  "Automatically compute and set axis limits from data arrays.
   
   Analyzes the data arrays and sets appropriate axis ranges. Optionally
   enforces exact limits (no padding) or allows automatic padding.
   
   Parameters:
   - window: Window handle (long integer)
   - x: X-axis data (AFArray)
   - y: Y-axis data (AFArray)
   - z: Z-axis data (AFArray, optional - can be nil for 2D)
   - exact: Boolean - true for exact limits, false for auto-padding
   - props: Cell properties pointer (or nil for default cell)
   
   Returns:
   nil
   
   Example:
   ```clojure
   ;; 2D with auto-padding
   (set-axes-limits-compute! window x-data y-data nil false nil)
   
   ;; 3D with exact limits
   (set-axes-limits-compute! window x y z true nil)
   ```"
  [window x y z exact props]
  (let [window-segment (mem/as-segment window)
        x-handle (jvm/af-handle x)
        y-handle (jvm/af-handle y)
        z-handle (if z (jvm/af-handle z) MemorySegment/NULL)
        props-ptr (or props MemorySegment/NULL)]
    (jvm/check! (window-ffi/af-set-axes-limits-compute window-segment 
                                                        x-handle 
                                                        y-handle 
                                                        z-handle
                                                        (if exact 1 0)
                                                        props-ptr)
                "af-set-axes-limits-compute"))
  nil)

(defn set-axes-limits-2d!
  "Set explicit 2D axis limits.
   
   Manually specifies the X and Y axis ranges for 2D plots.
   
   Parameters:
   - window: Window handle (long integer)
   - xmin: Minimum X value (float)
   - xmax: Maximum X value (float)
   - ymin: Minimum Y value (float)
   - ymax: Maximum Y value (float)
   - exact: Boolean - true for exact limits, false for padding
   - props: Cell properties pointer (or nil)
   
   Returns:
   nil
   
   Example:
   ```clojure
   ;; Standard plot range
   (set-axes-limits-2d! window 0.0 10.0 -1.0 1.0 true nil)
   
   ;; For specific cell in grid
   (set-axes-limits-2d! window -5.0 5.0 0.0 100.0 false (cell-props 0 1))
   ```"
  [window xmin xmax ymin ymax exact props]
  (let [window-segment (mem/as-segment window)
        props-ptr (or props jvm/null-ptr)]
    (jvm/check! (window-ffi/af-set-axes-limits-2d window-segment
                                                   (float xmin) (float xmax)
                                                   (float ymin) (float ymax)
                                                   (if exact 1 0)
                                                   props-ptr)
                "af-set-axes-limits-2d"))
  nil)

(defn set-axes-limits-3d!
  "Set explicit 3D axis limits.
   
   Manually specifies the X, Y, and Z axis ranges for 3D plots.
   
   Parameters:
   - window: Window handle (long integer)
   - xmin, xmax: X-axis range (floats)
   - ymin, ymax: Y-axis range (floats)
   - zmin, zmax: Z-axis range (floats)
   - exact: Boolean - true for exact limits, false for padding
   - props: Cell properties pointer (or nil)
   
   Returns:
   nil
   
   Example:
   ```clojure
   (set-axes-limits-3d! window 
                        -10.0 10.0   ; X range
                        -10.0 10.0   ; Y range
                        0.0 5.0      ; Z range
                        true nil)
   ```"
  [window xmin xmax ymin ymax zmin zmax exact props]
  (let [window-segment (mem/as-segment window)
        props-ptr (or props MemorySegment/NULL)]
    (jvm/check! (window-ffi/af-set-axes-limits-3d window-segment
                                                   (float xmin) (float xmax)
                                                   (float ymin) (float ymax)
                                                   (float zmin) (float zmax)
                                                   (if exact 1 0)
                                                   props-ptr)
                "af-set-axes-limits-3d"))
  nil)

(defn set-axes-titles!
  "Set axis labels for plots.
   
   Sets the text labels for X, Y, and optionally Z axes.
   
   Parameters:
   - window: Window handle (long integer)
   - xtitle: X-axis label (string or nil)
   - ytitle: Y-axis label (string or nil)
   - ztitle: Z-axis label (string or nil for 2D plots)
   - props: Cell properties pointer (or nil)
   
   Returns:
   nil
   
   Example:
   ```clojure
   ;; 2D plot labels
   (set-axes-titles! window \"Time (s)\" \"Amplitude (V)\" nil nil)
   
   ;; 3D plot labels
   (set-axes-titles! window \"X\" \"Y\" \"Z\" nil)
   ```"
  [window xtitle ytitle ztitle props]
  (let [window-segment (mem/as-segment window)
        x-c-str (if xtitle (jvm/string->c-string xtitle) MemorySegment/NULL)
        y-c-str (if ytitle (jvm/string->c-string ytitle) MemorySegment/NULL)
        z-c-str (if ztitle (jvm/string->c-string ztitle) MemorySegment/NULL)
        props-ptr (or props MemorySegment/NULL)]
    (jvm/check! (window-ffi/af-set-axes-titles window-segment
                                                x-c-str y-c-str z-c-str
                                                props-ptr)
                "af-set-axes-titles"))
  nil)

(defn set-axes-label-format!
  "Set numeric format for axis labels.
   
   Controls how axis tick labels are formatted (e.g., decimal places,
   scientific notation).
   
   Parameters:
   - window: Window handle (long integer)
   - xformat: X-axis format string (printf-style, or nil)
   - yformat: Y-axis format string (printf-style, or nil)
   - zformat: Z-axis format string (printf-style, or nil)
   - props: Cell properties pointer (or nil)
   
   Returns:
   nil
   
   Example:
   ```clojure
   ;; Two decimal places
   (set-axes-label-format! window \"%.2f\" \"%.2f\" nil nil)
   
   ;; Scientific notation
   (set-axes-label-format! window \"%.2e\" \"%.2e\" \"%.2e\" nil)
   ```"
  [window xformat yformat zformat props]
  (let [window-segment (mem/as-segment window)
        x-c-str (if xformat (jvm/string->c-string xformat) MemorySegment/NULL)
        y-c-str (if yformat (jvm/string->c-string yformat) MemorySegment/NULL)
        z-c-str (if zformat (jvm/string->c-string zformat) MemorySegment/NULL)
        props-ptr (or props MemorySegment/NULL)]
    (jvm/check! (window-ffi/af-set-axes-label-format window-segment
                                                      x-c-str y-c-str z-c-str
                                                      props-ptr)
                "af-set-axes-label-format"))
  nil)

;;;
;;; Drawing Functions
;;;

(defn draw-image!
  "Draw an image to the window.
   
   Displays an array as an image. Supports grayscale (1 channel),
   RGB (3 channels), or RGBA (4 channels).
   
   Parameters:
   - window: Window handle (long integer)
   - image: Image array (AFArray) - shape [height, width] or [height, width, channels]
   - props: Cell properties pointer (or nil)
   
   Returns:
   nil
   
   Example:
   ```clojure
   (draw-image! window grayscale-img nil)
   (draw-image! window rgb-img (cell-props 0 0))
   ```"
  [window image props]
  (let [window-segment (mem/as-segment window)
        image-handle (jvm/af-handle image)
        props-ptr (or props MemorySegment/NULL)]
    (jvm/check! (image-ffi/af-draw-image window-segment image-handle props-ptr)
                "af-draw-image"))
  nil)

(defn draw-plot-nd!
  "Draw a line plot from combined coordinate matrix.
   
   Plots data where dimensionality is determined by array shape:
   - [n, 2] for 2D plots (x, y)
   - [n, 3] for 3D plots (x, y, z)
   
   Parameters:
   - window: Window handle (long integer)
   - points: Point array (AFArray) - shape [n, 2] or [n, 3]
   - props: Cell properties pointer (or nil)
   
   Returns:
   nil
   
   Example:
   ```clojure
   ;; 2D plot
   (let [pts (create-2d-curve 100)]
     (draw-plot-nd! window pts nil))
   ```"
  [window points props]
  (let [window-segment (mem/as-segment window)
        points-handle (jvm/af-handle points)
        props-ptr (or props jvm/null-ptr)]
    (jvm/check! (plot-ffi/af-draw-plot-nd window-segment points-handle props-ptr)
                "af-draw-plot-nd"))
  nil)

(defn draw-plot-2d!
  "Draw a 2D line plot from separate X and Y arrays.
   
   Connects points (X[i], Y[i]) with lines.
   
   Parameters:
   - window: Window handle (long integer)
   - x: X coordinates (AFArray vector)
   - y: Y coordinates (AFArray vector, same length)
   - props: Cell properties pointer (or nil)
   
   Returns:
   nil
   
   Example:
   ```clojure
   (let [x (linspace 0.0 (* 2 Math/PI) 100)
         y (sin x)]
     (draw-plot-2d! window x y nil))
   ```"
  [window x y props]
  (let [window-segment (mem/as-segment window)
        x-handle (jvm/af-handle x)
        y-handle (jvm/af-handle y)
        props-ptr (or props jvm/null-ptr)]
    (jvm/check! (plot-ffi/af-draw-plot-2d window-segment x-handle y-handle props-ptr)
                "af-draw-plot-2d"))
  nil)

(defn draw-plot-3d!
  "Draw a 3D line plot from separate X, Y, Z arrays.
   
   Connects points (X[i], Y[i], Z[i]) with lines in 3D space.
   
   Parameters:
   - window: Window handle (long integer)
   - x: X coordinates (AFArray vector)
   - y: Y coordinates (AFArray vector)
   - z: Z coordinates (AFArray vector)
   - props: Cell properties pointer (or nil)
   
   Returns:
   nil
   
   Example:
   ```clojure
   ;; 3D spiral
   (let [t (linspace 0.0 (* 4 Math/PI) 200)
         x (mul t (cos t))
         y (mul t (sin t))
         z t]
     (draw-plot-3d! window x y z nil))
   ```"
  [window x y z props]
  (let [window-segment (mem/as-segment window)
        x-handle (jvm/af-handle x)
        y-handle (jvm/af-handle y)
        z-handle (jvm/af-handle z)
        props-ptr (or props jvm/null-ptr)]
    (jvm/check! (plot-ffi/af-draw-plot-3d window-segment x-handle y-handle z-handle props-ptr)
                "af-draw-plot-3d"))
  nil)

(defn draw-scatter-nd!
  "Draw a scatter plot from combined coordinate matrix.
   
   Displays individual points without connecting lines.
   
   Parameters:
   - window: Window handle (long integer)
   - points: Point array (AFArray) - shape [n, 2] or [n, 3]
   - marker: Marker type (integer constant, e.g., AF_MARKER_CIRCLE)
   - props: Cell properties pointer (or nil)
   
   Returns:
   nil"
  [window points marker props]
  (let [window-segment (mem/as-segment window)
        points-handle (jvm/af-handle points)
        props-ptr (or props MemorySegment/NULL)]
    (jvm/check! (plot-ffi/af-draw-scatter-nd window-segment points-handle (int marker) props-ptr)
                "af-draw-scatter-nd"))
  nil)

(defn draw-scatter-2d!
  "Draw a 2D scatter plot from separate X and Y arrays.
   
   Displays individual points (X[i], Y[i]) without connecting lines.
   
   Parameters:
   - window: Window handle (long integer)
   - x: X coordinates (AFArray vector)
   - y: Y coordinates (AFArray vector)
   - marker: Marker type (integer constant)
   - props: Cell properties pointer (or nil)
   
   Returns:
   nil
   
   Example:
   ```clojure
   (draw-scatter-2d! window x-data y-data AF_MARKER_CIRCLE nil)
   ```"
  [window x y marker props]
  (let [window-segment (mem/as-segment window)
        x-handle (jvm/af-handle x)
        y-handle (jvm/af-handle y)
        props-ptr (or props MemorySegment/NULL)]
    (jvm/check! (plot-ffi/af-draw-scatter-2d window-segment x-handle y-handle (int marker) props-ptr)
                "af-draw-scatter-2d"))
  nil)

(defn draw-scatter-3d!
  "Draw a 3D scatter plot from separate X, Y, Z arrays.
   
   Displays individual points (X[i], Y[i], Z[i]) in 3D space.
   
   Parameters:
   - window: Window handle (long integer)
   - x: X coordinates (AFArray vector)
   - y: Y coordinates (AFArray vector)
   - z: Z coordinates (AFArray vector)
   - marker: Marker type (integer constant)
   - props: Cell properties pointer (or nil)
   
   Returns:
   nil"
  [window x y z marker props]
  (let [window-segment (mem/as-segment window)
        x-handle (jvm/af-handle x)
        y-handle (jvm/af-handle y)
        z-handle (jvm/af-handle z)
        props-ptr (or props MemorySegment/NULL)]
    (jvm/check! (plot-ffi/af-draw-scatter-3d window-segment x-handle y-handle z-handle (int marker) props-ptr)
                "af-draw-scatter-3d"))
  nil)

(defn draw-hist!
  "Draw a histogram.
   
   Displays frequency distribution as a bar chart.
   
   Parameters:
   - window: Window handle (long integer)
   - data: Histogram data (AFArray) - bin counts
   - minval: Minimum value for histogram range (double)
   - maxval: Maximum value for histogram range (double)
   - props: Cell properties pointer (or nil)
   
   Returns:
   nil
   
   Example:
   ```clojure
   ;; Image histogram
   (let [hist (histogram image 256)]
     (draw-hist! window hist 0.0 255.0 nil))
   ```"
  [window data minval maxval props]
  (let [window-segment (mem/as-segment window)
        data-handle (jvm/af-handle data)
        props-ptr (or props MemorySegment/NULL)]
    (jvm/check! (hist-ffi/af-draw-hist window-segment data-handle (double minval) (double maxval) props-ptr)
                "af-draw-hist"))
  nil)

(defn draw-surface!
  "Draw a 3D surface plot.
   
   Displays a surface defined by Z = f(X, Y).
   
   Parameters:
   - window: Window handle (long integer)
   - x-vals: X coordinate grid (AFArray)
   - y-vals: Y coordinate grid (AFArray)
   - z-vals: Z values (surface height, AFArray)
   - props: Cell properties pointer (or nil)
   
   Returns:
   nil
   
   Example:
   ```clojure
   ;; 3D function plot
   (let [x (linspace -5.0 5.0 50)
         y (linspace -5.0 5.0 50)
         [xx yy] (meshgrid x y)
         z (sin (sqrt (add (pow xx 2) (pow yy 2))))]
     (draw-surface! window xx yy z nil))
   ```"
  [window x-vals y-vals z-vals props]
  (let [window-segment (mem/as-segment window)
        x-handle (jvm/af-handle x-vals)
        y-handle (jvm/af-handle y-vals)
        z-handle (jvm/af-handle z-vals)
        props-ptr (or props MemorySegment/NULL)]
    (jvm/check! (surface-ffi/af-draw-surface window-segment x-handle y-handle z-handle props-ptr)
                "af-draw-surface"))
  nil)

(defn draw-vector-field-nd!
  "Draw a vector field from combined point and direction matrices.
   
   Displays vectors at specified positions.
   
   Parameters:
   - window: Window handle (long integer)
   - points: Point positions (AFArray) - shape [n, 2] or [n, 3]
   - directions: Vector directions (AFArray) - same shape as points
   - props: Cell properties pointer (or nil)
   
   Returns:
   nil"
  [window points directions props]
  (let [window-segment (mem/as-segment window)
        points-handle (jvm/af-handle points)
        directions-handle (jvm/af-handle directions)
        props-ptr (or props MemorySegment/NULL)]
    (jvm/check! (vector-ffi/af-draw-vector-field-nd window-segment points-handle directions-handle props-ptr)
                "af-draw-vector-field-nd"))
  nil)

(defn draw-vector-field-2d!
  "Draw a 2D vector field from separate position and direction arrays.
   
   Displays 2D vectors at grid points.
   
   Parameters:
   - window: Window handle (long integer)
   - x-points: X positions (AFArray)
   - y-points: Y positions (AFArray)
   - x-dirs: X components of vectors (AFArray)
   - y-dirs: Y components of vectors (AFArray)
   - props: Cell properties pointer (or nil)
   
   Returns:
   nil
   
   Example:
   ```clojure
   ;; Gradient field
   (let [[x y] (meshgrid (range -5 5 0.5) (range -5 5 0.5))
         dx (gradient-x potential)
         dy (gradient-y potential)]
     (draw-vector-field-2d! window x y dx dy nil))
   ```"
  [window x-points y-points x-dirs y-dirs props]
  (let [window-segment (mem/as-segment window)
        xp-handle (jvm/af-handle x-points)
        yp-handle (jvm/af-handle y-points)
        xd-handle (jvm/af-handle x-dirs)
        yd-handle (jvm/af-handle y-dirs)
        props-ptr (or props MemorySegment/NULL)]
    (jvm/check! (vector-ffi/af-draw-vector-field-2d window-segment xp-handle yp-handle xd-handle yd-handle props-ptr)
                "af-draw-vector-field-2d"))
  nil)

(defn draw-vector-field-3d!
  "Draw a 3D vector field from separate position and direction arrays.
   
   Displays 3D vectors at grid points.
   
   Parameters:
   - window: Window handle (long integer)
   - x-points, y-points, z-points: Position coordinates (AFArrays)
   - x-dirs, y-dirs, z-dirs: Vector components (AFArrays)
   - props: Cell properties pointer (or nil)
   
   Returns:
   nil
   
   Example:
   ```clojure
   ;; Magnetic field visualization
   (let [[x y z] (meshgrid3 ...)
         [bx by bz] (compute-b-field x y z)]
     (draw-vector-field-3d! window x y z bx by bz nil))
   ```"
  [window x-points y-points z-points x-dirs y-dirs z-dirs props]
  (let [window-segment (mem/as-segment window)
        xp-handle (jvm/af-handle x-points)
        yp-handle (jvm/af-handle y-points)
        zp-handle (jvm/af-handle z-points)
        xd-handle (jvm/af-handle x-dirs)
        yd-handle (jvm/af-handle y-dirs)
        zd-handle (jvm/af-handle z-dirs)
        props-ptr (or props MemorySegment/NULL)]
    (jvm/check! (vector-ffi/af-draw-vector-field-3d window-segment 
                                                     xp-handle yp-handle zp-handle
                                                     xd-handle yd-handle zd-handle
                                                     props-ptr)
                "af-draw-vector-field-3d"))
  nil)
