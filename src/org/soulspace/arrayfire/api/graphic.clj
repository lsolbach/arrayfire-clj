(ns org.soulspace.arrayfire.api.graphic
  "Idiomatic Clojure API for ArrayFire's Forge graphics functions.

   Provides hardware-accelerated scientific visualization via the Forge library.
   All drawing functions require an active `with-arrayfire` region from
   `org.soulspace.arrayfire.api.core`.

   Window Lifecycle:
   - Use `with-window` macro for automatic resource cleanup (recommended).
   - Use `create-window` / `destroy-window!` for manual lifecycle management.

   Grid Layouts:
   - Call `grid!` after window creation to set up subplot grids.
   - Pass a `:row` / `:col` map to drawing functions to target a specific cell.

   Cell Properties Map Keys:
   | Key        | Type    | Description                                   |
   |------------|---------|-----------------------------------------------|
   | `:row`     | int     | Grid row (0-based); omit for single-plot use  |
   | `:col`     | int     | Grid column (0-based)                         |
   | `:title`   | string  | Cell title                                    |
   | `:colormap`| keyword | Colormap keyword (see resolve-colormap)       |

   Colormap Keywords:
   | Keyword    | Description            |
   |------------|------------------------|
   | `:default` | Default colormap       |
   | `:spectrum` | Spectral              |
   | `:colors`  | Colors                 |
   | `:red`     | Red                    |
   | `:mood`    | Mood                   |
   | `:heat`    | Heat / infrared        |
   | `:blue`    | Blue                   |
   | `:inferno` | Perceptual: inferno    |
   | `:magma`   | Perceptual: magma      |
   | `:plasma`  | Perceptual: plasma     |
   | `:viridis` | Perceptual: viridis    |

   Marker Type Keywords (for scatter plots):
   | Keyword    | Description    |
   |------------|----------------|
   | `:none`    | No marker      |
   | `:point`   | Point          |
   | `:circle`  | Circle         |
   | `:square`  | Square         |
   | `:triangle`| Triangle       |
   | `:cross`   | Cross          |
   | `:plus`    | Plus sign      |
   | `:star`    | Star           |

   Example — basic plot:
   ```clojure
   (with-arrayfire
     (with-window [w 800 600 \"Sine Wave\"]
       (let [x (linspace 0.0 (* 2 Math/PI) 200)
             y (sin x)]
         (draw-plot-2d! w :x x :y y)
         (show! w))))
   ```

   Example — 2x2 grid:
   ```clojure
   (with-arrayfire
     (with-window [w 1024 768 \"Dashboard\"]
       (grid! w 2 2)
       (draw-plot-2d! w :x x1 :y y1 :props {:row 0 :col 0 :title \"Signal A\"})
       (draw-plot-2d! w :x x2 :y y2 :props {:row 0 :col 1 :title \"Signal B\"})
       (draw-histogram! w :data hist1 :min 0.0 :max 1.0 :props {:row 1 :col 0})
       (draw-scatter-2d! w :x sx :y sy
                           :marker :circle
                           :props {:row 1 :col 1 :title \"Scatter\"})
       (show! w)))
   ```"
  (:require [org.soulspace.arrayfire.api.core :as af :refer [assert-within-arrayfire!]]
            [org.soulspace.arrayfire.integration.base.definitions :as defs]
            [org.soulspace.arrayfire.integration.unified-api.graphic :as graphic]
            [org.soulspace.arrayfire.api.image-processing :as ip]))


;;; TODO refactor arguments of set axes-* and draw-* functions to use 
;;;      positional args for required parameters (e.g. window handle)
;;;      and keyword args for optional parameters (e.g. cell props,
;;;      marker type, etc.
 

;;;
;;; Private helper: cell-props-seg
;;;

(defn- cell-props-seg
  "Convert a Clojure cell-props map (or nil) to the MemorySegment
   expected by the integration layer's drawing functions.

   Accepted map keys:
   - :row      (int, default -1)
   - :col      (int, default -1)
   - :title    (string, optional)
   - :colormap (keyword or integer, optional)

   When props is nil, a default (single-chart) cell props segment is used."
  [props]
  (if (nil? props)
    (graphic/make-cell-props -1 -1)
    (let [row      (int (get props :row -1))
          col      (int (get props :col -1))
          title    (get props :title nil)
          colormap (some-> (get props :colormap) defs/resolve-colormap)]
      (cond
        colormap (graphic/make-cell-props row col (or title "") colormap)
        title    (graphic/make-cell-props row col title)
        :else    (graphic/make-cell-props row col)))))

;;;
;;; Window management functions
;;;

(defn create-window
  "Create a graphics window.

   Prefer `with-window` for automatic cleanup. Must be called within
   a `with-arrayfire` region.

   Parameters:
   - width  — window width in pixels
   - height — window height in pixels
   - title  — window title string

   Returns:
   Long integer window handle."
  [width height title]
  (assert-within-arrayfire! "create-window")
  (graphic/create-window width height title))

(defn destroy-window!
  "Destroy a window and free its resources.

   Safe to call from a `finally` block—does NOT assert `within-arrayfire?`
   so cleanup works even if the ArrayFire context has been torn down.

   Parameters:
   - window — window handle (long integer, or nil for no-op)

   Returns:
   nil"
  [window]
  (graphic/destroy-window! window))

(defn show!
  "Display the window and process window events.

   Makes the window visible. On most platforms this blocks until the
   window is closed by the user.

   Parameters:
   - window — window handle

   Returns:
   nil"
  [window]
  (assert-within-arrayfire! "show!")
  (graphic/show! window))

(defn window-closed?
  "Return true if the window was closed by the user.

   Useful for animation loops where you want to stop rendering
   when the user closes the window.

   Parameters:
   - window — window handle

   Returns:
   Boolean."
  [window]
  (assert-within-arrayfire! "window-closed?")
  (graphic/is-window-closed? window))

(defn set-visibility!
  "Show or hide the window without blocking.

   Parameters:
   - window  — window handle
   - visible — true to show, false to hide

   Returns:
   nil"
  [window visible]
  (assert-within-arrayfire! "set-visibility!")
  (graphic/set-visibility! window (boolean visible)))

(defn set-title!
  "Change the window's title bar text.

   Parameters:
   - window — window handle
   - title  — new title string

   Returns:
   nil"
  [window title]
  (assert-within-arrayfire! "set-title!")
  (graphic/set-title! window title))

(defn set-position!
  "Move the window to screen coordinates (x, y).

   Origin (0, 0) is the top-left corner of the screen.

   Parameters:
   - window — window handle
   - x      — horizontal screen position in pixels
   - y      — vertical screen position in pixels

   Returns:
   nil"
  [window x y]
  (assert-within-arrayfire! "set-position!")
  (graphic/set-position! window x y))

(defn set-size!
  "Resize the window.

   Parameters:
   - window — window handle
   - width  — new width in pixels
   - height — new height in pixels

   Returns:
   nil"
  [window width height]
  (assert-within-arrayfire! "set-size!")
  (graphic/set-size! window width height))

(defn grid!
  "Divide the window into a rows x cols subplot grid.

   After calling `grid!`, specify the target cell via the props map
   (`:row` / `:col` keys) when drawing.

   Parameters:
   - window — window handle
   - rows   — number of rows
   - cols   — number of columns

   Returns:
   nil

   Example:
   ```clojure
   (grid! w 2 3)  ; 2 rows, 3 columns -> 6 subplot cells
   ```"
  [window rows cols]
  (assert-within-arrayfire! "grid!")
  (graphic/grid! window rows cols))

;;;
;;; Window lifecycle macro
;;;

(defmacro with-window
  "Execute `body` with a managed graphics window bound to `window-sym`.

   Creates the window before entering the body and guarantees that
   `destroy-window!` is called in a `finally` block, even if an exception
   is thrown.  Must be used inside a `with-arrayfire` region.

   Binding vector:
     [window-sym width height title]

   Parameters:
   - window-sym — symbol bound to the window handle
   - width      — window width in pixels (integer)
   - height     — window height in pixels (integer)
   - title      — window title (string)

   Example:
   ```clojure
   (with-arrayfire
     (with-window [w 800 600 \"My Plot\"]
       (draw-plot-2d! w x y nil)
       (show! w)))
   ```"
  [[window-sym width height title] & body]
  `(let [~window-sym (create-window ~width ~height ~title)]
     (try
       ~@body
       (finally
         (destroy-window! ~window-sym)))))

;;;
;;; Axis configuration
;;;

(defn set-axes-limits!
  "Set axis limits for the current plot.

   Dispatches to 2D or 3D depending on whether `:z-range` is provided.

   Parameters:
   - window    — window handle (positional)
   - :x-range  — [xmin xmax] vector (required)
   - :y-range  — [ymin ymax] vector (required)
   - :z-range  — [zmin zmax] vector (optional, triggers 3D mode)
   - :exact?   — true for exact limits, false for auto-padding (default false)
   - :props    — cell-props map (default nil)

   Returns:
   nil

   Example:
   ```clojure
   ;; 2D
   (set-axes-limits! w :x-range [0.0 10.0] :y-range [-1.0 1.0])

   ;; 3D with exact limits
   (set-axes-limits! w :x-range [-5.0 5.0] :y-range [-5.0 5.0]
                       :z-range [0.0 100.0] :exact? true)
   ```"
  [window & {:keys [x-range y-range z-range exact? props]
             :or   {exact? false props nil}}]
  (assert-within-arrayfire! "set-axes-limits!")
  (let [[xmin xmax] x-range
        [ymin ymax] y-range]
    (if z-range
      (let [[zmin zmax] z-range]
        (graphic/set-axes-limits-3d! window
                                     (float xmin) (float xmax)
                                     (float ymin) (float ymax)
                                     (float zmin) (float zmax)
                                     (boolean exact?)
                                     (cell-props-seg props)))
      (graphic/set-axes-limits-2d! window
                                   (float xmin) (float xmax)
                                   (float ymin) (float ymax)
                                   (boolean exact?)
                                   (cell-props-seg props)))))

(defn set-axes-limits-from-data!
  "Auto-compute and set axis limits from data arrays.

   Analyzes the provided data arrays to determine suitable axis ranges.

   Parameters:
   - window  — window handle (positional)
   - :x      — x-axis data AFArray (required)
   - :y      — y-axis data AFArray (required)
   - :z      — z-axis data AFArray (default nil for 2D)
   - :exact? — true for exact limits, false for auto-padding (default false)
   - :props  — cell-props map (default nil)

   Returns:
   nil

   Example:
   ```clojure
   (set-axes-limits-from-data! w :x x-arr :y y-arr)
   (set-axes-limits-from-data! w :x x-arr :y y-arr :z z-arr :exact? true)
   ```"
  [window & {:keys [x y z exact? props]
             :or   {z nil exact? false props nil}}]
  (assert-within-arrayfire! "set-axes-limits-from-data!")
  (graphic/set-axes-limits-compute! window x y z (boolean exact?) (cell-props-seg props)))

(defn set-axes-titles!
  "Set axis label text.

   Parameters:
   - window — window handle (positional)
   - :x     — X-axis label string (default nil)
   - :y     — Y-axis label string (default nil)
   - :z     — Z-axis label string (default nil, omit for 2D)
   - :props — cell-props map (default nil)

   Returns:
   nil

   Example:
   ```clojure
   (set-axes-titles! w :x \"Time (s)\" :y \"Voltage (V)\")
   (set-axes-titles! w :x \"X\" :y \"Y\" :z \"Z\" :props {:row 0 :col 0})
   ```"
  [window & {:keys [x y z props]
             :or   {x nil y nil z nil props nil}}]
  (assert-within-arrayfire! "set-axes-titles!")
  (graphic/set-axes-titles! window x y z (cell-props-seg props)))

(defn set-axes-label-format!
  "Set printf-style numeric format strings for axis tick labels.

   Parameters:
   - window  — window handle (positional)
   - :x      — X-axis printf format (e.g. \"%.2f\") (default nil)
   - :y      — Y-axis printf format (default nil)
   - :z      — Z-axis printf format (default nil)
   - :props  — cell-props map (default nil)

   Returns:
   nil

   Example:
   ```clojure
   (set-axes-label-format! w :x \"%.2f\" :y \"%.4e\")
   (set-axes-label-format! w :x \"%.1f\" :y \"%.1f\" :z \"%.2f\")
   ```"
  [window & {:keys [x y z props]
             :or   {x nil y nil z nil props nil}}]
  (assert-within-arrayfire! "set-axes-label-format!")
  (graphic/set-axes-label-format! window x y z (cell-props-seg props)))

;;;
;;; Drawing functions
;;;

(defn draw-image!
  "Draw an AFArray as an image.

   Supports grayscale ([height width]), RGB ([height width 3]), and
   RGBA ([height width 4]) arrays.

   Parameters:
   - window — window handle (positional)
   - :image — AFArray image data (required)
   - :props — cell-props map (default nil)

   Returns:
   nil"
  [window & {:keys [image props]}]
  (assert-within-arrayfire! "draw-image!")
  (graphic/draw-image! window image (cell-props-seg props)))

(defn draw-plot!
  "Draw a line plot from a combined coordinate matrix.

   Dimensionality is determined by the shape of `points`:
   - [n 2] -> 2D line plot
   - [n 3] -> 3D line plot

   Parameters:
   - window  — window handle (positional)
   - :points — AFArray of shape [n 2] or [n 3] (required)
   - :props  — cell-props map (default nil)

   Returns:
   nil"
  [window & {:keys [points props]}]
  (assert-within-arrayfire! "draw-plot!")
  (graphic/draw-plot-nd! window points (cell-props-seg props)))

(defn draw-plot-2d!
  "Draw a 2D line plot from separate X and Y arrays.

   Connects points (X[i], Y[i]) with lines.

   Parameters:
   - window — window handle (positional)
   - :x     — X coordinates AFArray (required)
   - :y     — Y coordinates AFArray, same length (required)
   - :props — cell-props map (default nil)

   Returns:
   nil

   Example:
   ```clojure
   (draw-plot-2d! w :x x-arr :y y-arr :props {:title \"Signal\"})
   ```"
  [window & {:keys [x y props]}]
  (assert-within-arrayfire! "draw-plot-2d!")
  (graphic/draw-plot-2d! window x y (cell-props-seg props)))

(defn draw-plot-3d!
  "Draw a 3D line plot from separate X, Y, Z arrays.

   Connects points (X[i], Y[i], Z[i]) with lines in 3D space.

   Parameters:
   - window — window handle (positional)
   - :x     — X coordinates AFArray (required)
   - :y     — Y coordinates AFArray (required)
   - :z     — Z coordinates AFArray (required)
   - :props — cell-props map (default nil)

   Returns:
   nil"
  [window & {:keys [x y z props]}]
  (assert-within-arrayfire! "draw-plot-3d!")
  (graphic/draw-plot-3d! window x y z (cell-props-seg props)))

(defn draw-scatter!
  "Draw a scatter plot from a combined coordinate matrix.

   Dimensionality is determined by the shape of `points`:
   - [n 2] -> 2D scatter
   - [n 3] -> 3D scatter

   Parameters:
   - window  — window handle (positional)
   - :points — AFArray of shape [n 2] or [n 3] (required)
   - :marker — marker keyword (e.g. :circle) or integer (default :point)
   - :props  — cell-props map (default nil)

   Returns:
   nil

   Example:
   ```clojure
   (draw-scatter! w :points pts :marker :circle)
   (draw-scatter! w :points pts)  ; default :point marker
   ```"
  [window & {:keys [points marker props]
             :or   {marker :point}}]
  (assert-within-arrayfire! "draw-scatter!")
  (graphic/draw-scatter-nd! window points
                            (defs/resolve-marker-type marker)
                            (cell-props-seg props)))

(defn draw-scatter-2d!
  "Draw a 2D scatter plot from separate X and Y arrays.

   Parameters:
   - window  — window handle (positional)
   - :x      — X coordinates AFArray (required)
   - :y      — Y coordinates AFArray (required)
   - :marker — marker keyword or integer (default :point)
   - :props  — cell-props map (default nil)

   Example:
   ```clojure
   (draw-scatter-2d! w :x x-arr :y y-arr)
   (draw-scatter-2d! w :x x-arr :y y-arr :marker :circle :props {:row 1 :col 0})
   ```"
  [window & {:keys [x y marker props]
             :or   {marker :point}}]
  (assert-within-arrayfire! "draw-scatter-2d!")
  (graphic/draw-scatter-2d! window x y
                            (defs/resolve-marker-type marker)
                            (cell-props-seg props)))

(defn draw-scatter-3d!
  "Draw a 3D scatter plot from separate X, Y, Z arrays.

   Parameters:
   - window  — window handle (positional)
   - :x      — X coordinates AFArray (required)
   - :y      — Y coordinates AFArray (required)
   - :z      — Z coordinates AFArray (required)
   - :marker — marker keyword or integer (default :point)
   - :props  — cell-props map (default nil)

   Returns:
   nil"
  [window & {:keys [x y z marker props]
             :or   {marker :point}}]
  (assert-within-arrayfire! "draw-scatter-3d!")
  (graphic/draw-scatter-3d! window x y z
                            (defs/resolve-marker-type marker)
                            (cell-props-seg props)))

(defn draw-histogram!
  "Draw a histogram (frequency distribution bar chart).

   Parameters:
   - window — window handle (positional)
   - :data  — AFArray of histogram bin counts (required)
   - :min   — minimum value of the histogram range (required, double)
   - :max   — maximum value of the histogram range (required, double)
   - :props — cell-props map (default nil)

   Returns:
   nil

   Example:
   ```clojure
   (let [hist (histogram image-arr 256)]
     (draw-histogram! w :data hist :min 0.0 :max 255.0
                        :props {:title \"Pixel Distribution\"}))
   ```"
  [window & {:keys [data min max props]}]
  (assert-within-arrayfire! "draw-histogram!")
  (graphic/draw-hist! window data (double min) (double max) (cell-props-seg props)))

(defn draw-surface!
  "Draw a 3D surface plot Z = f(X, Y).

   Parameters:
   - window — window handle (positional)
   - :x     — X coordinate grid AFArray (required)
   - :y     — Y coordinate grid AFArray (required)
   - :z     — Surface heights AFArray (required)
   - :props — cell-props map (default nil)

   Returns:
   nil

   Example:
   ```clojure
   (draw-surface! w :x x-grid :y y-grid :z z-grid :props {:colormap :viridis})
   ```"
  [window & {:keys [x y z props]}]
  (assert-within-arrayfire! "draw-surface!")
  (graphic/draw-surface! window x y z (cell-props-seg props)))

(defn draw-vector-field!
  "Draw a vector field from a combined coordinate+direction matrix.

   Dimensionality is determined by the shape of `points`:
   - [n 2] -> 2D vector field
   - [n 3] -> 3D vector field

   Parameters:
   - window      — window handle (positional)
   - :points     — position AFArray [n 2] or [n 3] (required)
   - :directions — direction AFArray, same shape as points (required)
   - :props      — cell-props map (default nil)

   Returns:
   nil"
  [window & {:keys [points directions props]}]
  (assert-within-arrayfire! "draw-vector-field!")
  (graphic/draw-vector-field-nd! window points directions (cell-props-seg props)))

(defn draw-vector-field-2d!
  "Draw a 2D vector field from separate position and direction arrays.

   Parameters:
   - window  — window handle (positional)
   - :x      — X positions AFArray (required)
   - :y      — Y positions AFArray (required)
   - :x-dirs — X direction components AFArray (required)
   - :y-dirs — Y direction components AFArray (required)
   - :props  — cell-props map (default nil)

   Returns:
   nil

   Example:
   ```clojure
   (draw-vector-field-2d! w :x x-pos :y y-pos :x-dirs dx :y-dirs dy
                            :props {:title \"Flow Field\"})
   ```"
  [window & {:keys [x y x-dirs y-dirs props]}]
  (assert-within-arrayfire! "draw-vector-field-2d!")
  (graphic/draw-vector-field-2d! window x y x-dirs y-dirs (cell-props-seg props)))

(defn draw-vector-field-3d!
  "Draw a 3D vector field from separate position and direction arrays.

   Parameters:
   - window  — window handle (positional)
   - :x      — X positions AFArray (required)
   - :y      — Y positions AFArray (required)
   - :z      — Z positions AFArray (required)
   - :x-dirs — X direction components AFArray (required)
   - :y-dirs — Y direction components AFArray (required)
   - :z-dirs — Z direction components AFArray (required)
   - :props  — cell-props map (default nil)

   Returns:
   nil"
  [window & {:keys [x y z x-dirs y-dirs z-dirs props]}]
  (assert-within-arrayfire! "draw-vector-field-3d!")
  (graphic/draw-vector-field-3d! window x y z x-dirs y-dirs z-dirs (cell-props-seg props)))

(comment
  ;; Graphic API REPL experiments
  ;; All forms must be evaluated inside (with-arrayfire ...).
  ;;
  ;; NOTE: Forge graphics require ArrayFire built with --BUILD_GRAPHICS=ON.
  ;;       On headless CI systems, these calls will throw AF_ERR_NO_GFX.

  ;; 1. Basic single-plot line plot
  (af/with-arrayfire {:backend :cpu}
    (with-window [win 800 600 "Sine Wave"]
      (let [x (af/range [100] :f32)
            y (af/sin x)]
        (set-axes-titles! win :x "x" :y "sin(x)")
        (draw-plot-2d! win :x x :y y)
        (show! win)
        (Thread/sleep 5000))))

  ;; 2. 2×2 grid layout
  (af/with-arrayfire {:backend :cpu}
    (with-window [win 800 600 "Dashboard"]
      (grid! win 2 2)
      (let [x  (af/range [200] :f32)
            y1 (af/sin x)
            y2 (af/cos x)
            h  (ip/histogram (af/random-normal [1000] :f32) 32 -1 1)]
        (draw-plot-2d!     win :x x :y y1
                               :props {:row 0 :col 0 :title "sin(x)"})
        (draw-plot-2d!     win :x x :y y2
                               :props {:row 0 :col 1 :title "cos(x)"})
        (draw-scatter-2d!  win :x x :y y1 :marker :circle
                               :props {:row 1 :col 0})
        (draw-histogram!   win :data h :min -3.0 :max 3.0
                               :props {:row 1 :col 1 :title "Normal dist"})
        (show! win)
        (Thread/sleep 5000))))

  ;; 3. Scatter plot with marker keyword
  (af/with-arrayfire {:backend :cpu}
    (with-window [win 600 600 "Scatter"]
      (let [x (af/random-normal [100] :f32)
            y (af/random-normal [100] :f32)]
        (draw-scatter-2d! win :x x :y y :marker :star)
        (show! win)
        (Thread/sleep 5000))))

  ;; 4. Image display
  ;; Needs libfreeimage
  (af/with-arrayfire {:backend :cpu}
    (with-window [win 480 640 "Sens"]
      (let [img (ip/load-image "sens.jpg")]
        (draw-image! win :image img)
        (show! win)
        (Thread/sleep 5000))))

  ;; 5. Surface plot
  (af/with-arrayfire {:backend :cpu}
    (with-window [win 800 600 "Surface"]
      (let [x  (af/range [50] :f32)
            y  (af/range [50] :f32)
            xx (af/tile x [1 50])
            yy (af/tile (af/reshape y [1 50]) [50 1])
            z  (af/sin (af/+ xx yy))]
        (set-axes-titles! win :x "X" :y "Y" :z "Z")
        (draw-surface! win :x xx :y yy :z z)
        (show! win)
        (Thread/sleep 5000))))

  ;; 6. Animation loop (non-blocking)
  (af/with-arrayfire {:backend :cpu}
    (with-window [win 800 400 "Animation"]
      (set-visibility! win true)
      (loop [i 0]
        (when (and (< i 100) (not (window-closed? win)))
          (let [x  (af/range [100] :f32)
                y  (af/sin (af/+ x (float i)))]
            (draw-plot-2d! win :x x :y y)
            (show! win)
            (Thread/sleep 33))
          (recur (inc i))))))

  ;
  )
