(ns org.soulspace.arrayfire.ffi.c-api.hist
  "Bindings for the ArrayFire histogram graphics/visualization functions."
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.loader]))

;; af_err af_draw_hist(const af_window wind, const af_array X, const double minval, const double maxval, const af_cell* const props)
(defcfn af-draw-hist
  "Draw a histogram visualization to an ArrayFire window.
   
   This function renders a pre-computed histogram array as a bar chart
   visualization in an ArrayFire graphics window. It is part of ArrayFire's
   Forge-based visualization system and provides real-time interactive histogram
   display capabilities for data analysis and debugging.
   
   Overview
   --------
   
   af-draw-hist is a **graphics rendering function**, not a histogram computation
   function. It takes histogram data (typically from af-histogram) and displays
   it as a bar chart in a window. This is distinct from:
   
   - **af-histogram**: Computes histogram bin counts from raw data
   - **af-hist-equal**: Performs histogram equalization for image enhancement
   - **af-draw-hist**: THIS FUNCTION - visualizes histogram data
   
   The function is designed for:
   - Interactive data exploration and visualization
   - Real-time monitoring of data distributions
   - Debugging and algorithm development
   - Educational demonstrations
   - Quality assessment of image processing pipelines
   
   Prerequisites
   -------------
   
   1. **ArrayFire with Graphics Support**
      - ArrayFire must be built with Forge graphics library
      - Requires OpenGL support on the system
      - Check with af_get_available_backends for graphics capability
   
   2. **Window Creation**
      - Must create af_window first using af_create_window
      - Window dimensions determine visualization canvas size
      - Multiple histograms can be drawn to grid-layout windows
   
   3. **Histogram Data**
      - Input must be 1D array (vector) of bin counts
      - Typically the output from af_histogram function
      - Array type: u32 (unsigned 32-bit integer) is standard
      - Other numeric types also supported (float, int, etc.)
   
   Parameters
   ----------
   
   - **wind**: ::mem/pointer (af_window handle)
     The window handle where the histogram will be drawn.
     Obtained from af_create_window.
     
     Window lifecycle:
     1. Create: af_create_window(width, height, title)
     2. Use: af_draw_hist repeatedly in render loop
     3. Destroy: af_destroy_window when done
   
   - **X**: ::mem/pointer (af_array handle)
     The histogram data array to visualize. Must be a 1D vector.
     
     Structure: [bin_0_count, bin_1_count, ..., bin_n_count]
     
     Where each element represents the frequency/count for that bin.
     The number of elements determines the number of bars rendered.
     
     Common sources:
     - Output from af_histogram(data, nbins, min, max)
     - Manually constructed frequency counts
     - Pre-computed probability distributions
   
   - **minval**: ::mem/double
     The data value corresponding to the first bin (left edge of histogram).
     
     This is NOT the minimum count in the histogram array X.
     Rather, it's the minimum value in the **original data** that was binned.
     
     Example: For image histogram with pixel values [0-255]:
     - minval = 0.0 (darkest pixel value)
     - Bin 0 represents pixels in range [0, step)
   
   - **maxval**: ::mem/double
     The data value corresponding to the last bin (right edge of histogram).
     
     Similar to minval, this is the maximum value from the original data
     that was binned, not the maximum count.
     
     Example: For image histogram with pixel values [0-255]:
     - maxval = 255.0 (brightest pixel value)
     - Last bin represents pixels in range [255-step, 255]
   
   - **props**: ::mem/pointer (af_cell structure)
     Rendering properties for chart customization.
     
     Structure members:
     - row: Grid row position (-1 for default/no grid)
     - col: Grid column position (-1 for default/no grid)
     - title: Chart title string (NULL or empty for no title)
     - cmap: Color map (AF_COLORMAP_DEFAULT, etc.)
     
     Grid mode: When window is divided into grid layout using
     af_grid(window, rows, cols), specify row/col to render
     histogram in specific cell.
   
   Returns
   -------
   
   - **::mem/int** (af_err)
     AF_SUCCESS (0) on successful rendering, or an error code:
     - AF_ERR_ARG: Invalid window or array handle
     - AF_ERR_SIZE: X is not a vector (multi-dimensional)
     - AF_ERR_TYPE: Unsupported array type
     - AF_ERR_NOT_SUPPORTED: Graphics not available (Forge not built)
     - AF_ERR_INTERNAL: OpenGL or windowing system error
   
   Visualization Details
   ---------------------
   
   1. **Bar Chart Rendering**
      - Each bin rendered as a vertical bar
      - Bar height proportional to bin count (frequency)
      - Bar width = (maxval - minval) / nbins
      - Bar color: ArrayFire orange (0.929, 0.486, 0.2745) by default
   
   2. **Axes Configuration**
      - X-axis: Data value range [minval, maxval]
      - Y-axis: Frequency range [0, max(X)]
      - Axes automatically scaled to fit data
      - Grid lines and tick marks included
   
   3. **Automatic Scaling**
      - X-axis limits: exactly [minval, maxval] (no rounding)
      - Y-axis limits: [0, rounded_max(X)]
        * max(X) rounded up to next power of 2 for cleaner display
        * Unless manual override set with af_set_axes_limits
   
   4. **Interactive Features** (if window supports)
      - Pan: Mouse drag to move view
      - Zoom: Mouse scroll or pinch gesture
      - Reset: Double-click to restore original view
      - Close: ESC key or window close button
   
   Typical Workflow
   ----------------
   
   Basic histogram visualization:
   
   ```clojure
   ;; 1. Compute histogram from image data
   (let [img (load-image \"photo.jpg\" false)
         hist-data (af-histogram img 256 0.0 255.0)
         
         ;; 2. Create window
         window (af-create-window 800 600 \"Image Histogram\")
         
         ;; 3. Setup properties
         props {:row -1 :col -1 :title \"Pixel Intensity Distribution\" :cmap AF_COLORMAP_DEFAULT}
         
         ;; 4. Render loop
         _ (while (not (af-is-window-closed window))
             (af-draw-hist window hist-data 0.0 255.0 props)
             (af-show window))
         
         ;; 5. Cleanup
         _ (af-destroy-window window)]
     nil)
   ```
   
   Real-time histogram updates:
   
   ```clojure
   (let [window (af-create-window 800 600 \"Live Histogram\")
         props {:row -1 :col -1 :title nil :cmap AF_COLORMAP_DEFAULT}]
     
     ;; Process video stream with live histogram
     (doseq [frame video-frames]
       (let [processed (process-frame frame)
             hist (af-histogram processed 256 0.0 255.0)]
         
         ;; Draw updated histogram
         (af-draw-hist window hist 0.0 255.0 props)
         (af-show window)
         
         ;; Check for window close
         (when (af-is-window-closed window)
           (break))))
     
     (af-destroy-window window))
   ```
   
   Multi-channel histogram (grid layout):
   
   ```clojure
   (let [rgb-img (load-image \"color.jpg\" true)
         [r g b] (split-channels rgb-img)
         
         ;; Create histograms for each channel
         hist-r (af-histogram r 256 0.0 255.0)
         hist-g (af-histogram g 256 0.0 255.0)
         hist-b (af-histogram b 256 0.0 255.0)
         
         ;; Create window with 1x3 grid
         window (af-create-window 1200 400 \"RGB Histograms\")
         _ (af-grid window 1 3)
         
         ;; Properties for each cell
         props-r {:row 0 :col 0 :title \"Red Channel\" :cmap AF_COLORMAP_DEFAULT}
         props-g {:row 0 :col 1 :title \"Green Channel\" :cmap AF_COLORMAP_DEFAULT}
         props-b {:row 0 :col 2 :title \"Blue Channel\" :cmap AF_COLORMAP_DEFAULT}]
     
     ;; Render all three histograms
     (while (not (af-is-window-closed window))
       (af-draw-hist window hist-r 0.0 255.0 props-r)
       (af-draw-hist window hist-g 0.0 255.0 props-g)
       (af-draw-hist window hist-b 0.0 255.0 props-b)
       (af-show window))
     
     (af-destroy-window window))
   ```
   
   Use Cases
   ---------
   
   1. **Image Analysis**
      - Assess brightness and contrast distribution
      - Identify under/over-exposed regions
      - Detect bimodal distributions (foreground/background)
      - Monitor histogram equalization results
   
      Example: Photography workflow
      ```clojure
      (defn analyze-photo [path]
        (let [img (load-image path false)
              hist (af-histogram img 256 0.0 255.0)
              window (af-create-window 800 600 \"Photo Analysis\")]
          
          ;; Display original histogram
          (af-draw-hist window hist 0.0 255.0 default-props)
          (af-show window)
          
          ;; Analyze distribution
          (let [mean (histogram-mean hist)
                variance (histogram-variance hist)
                entropy (histogram-entropy hist)]
            (println \"Mean brightness:\" mean)
            (println \"Contrast (variance):\" variance)
            (println \"Information (entropy):\" entropy))
          
          ;; Wait for user to close
          (while (not (af-is-window-closed window))
            (Thread/sleep 100))
          
          (af-destroy-window window)))
      ```
   
   2. **Algorithm Development**
      - Debug image processing pipelines
      - Visualize intermediate results
      - Compare before/after distributions
      - Validate threshold selection
   
      Example: Iterative thresholding
      ```clojure
      (defn find-optimal-threshold [img]
        (let [window (af-create-window 800 600 \"Threshold Selection\")
              hist (af-histogram img 256 0.0 255.0)
              props {:row -1 :col -1 :title \"Adjust Threshold\" :cmap AF_COLORMAP_DEFAULT}]
          
          (loop [threshold 128]
            ;; Draw histogram with threshold line
            (af-draw-hist window hist 0.0 255.0 props)
            ;; TODO: Draw vertical line at threshold
            (af-show window)
            
            ;; Compute binary result
            (let [binary (af-threshold img threshold)
                  foreground-pixels (af-sum binary)
                  background-pixels (- (af-get-elements img) foreground-pixels)]
              
              (println \"Threshold:\" threshold)
              (println \"Foreground:\" foreground-pixels \"Background:\" background-pixels))
            
            ;; User adjusts threshold via keyboard
            (if (af-is-window-closed window)
              threshold
              (recur (read-threshold-from-user))))
          
          (af-destroy-window window)))
      ```
   
   3. **Data Science Exploration**
      - Visualize dataset distributions
      - Identify outliers and anomalies
      - Compare distributions across groups
      - Validate data preprocessing
   
      Example: Dataset comparison
      ```clojure
      (defn compare-distributions [data1 data2]
        (let [;; Compute histograms with same bins
              hist1 (af-histogram data1 50 0.0 100.0)
              hist2 (af-histogram data2 50 0.0 100.0)
              
              ;; Create grid window
              window (af-create-window 1200 600 \"Distribution Comparison\")
              _ (af-grid window 1 2)
              
              props1 {:row 0 :col 0 :title \"Dataset 1\" :cmap AF_COLORMAP_DEFAULT}
              props2 {:row 0 :col 1 :title \"Dataset 2\" :cmap AF_COLORMAP_DEFAULT}]
          
          ;; Display both histograms
          (while (not (af-is-window-closed window))
            (af-draw-hist window hist1 0.0 100.0 props1)
            (af-draw-hist window hist2 0.0 100.0 props2)
            (af-show window))
          
          (af-destroy-window window)))
      ```
   
   4. **Quality Control**
      - Monitor manufacturing processes
      - Detect distribution shifts
      - Validate measurement systems
      - Real-time statistical process control
   
      Example: Production monitoring
      ```clojure
      (defn monitor-production [sensor-stream]
        (let [window (af-create-window 800 600 \"Production Monitor\")
              props {:row -1 :col -1 :title \"Measurement Distribution\" :cmap AF_COLORMAP_DEFAULT}
              target-mean 50.0
              target-std 5.0]
          
          (doseq [batch sensor-stream]
            ;; Compute histogram of measurements
            (let [hist (af-histogram batch 100 0.0 100.0)
                  mean (af-mean batch)
                  std (af-std batch)]
              
              ;; Draw histogram
              (af-draw-hist window hist 0.0 100.0 props)
              (af-show window)
              
              ;; Check if process is in control
              (when (or (> (Math/abs (- mean target-mean)) (* 3 target-std))
                       (> std (* 2 target-std)))
                (println \"WARNING: Process out of control!\")
                (println \"Mean:\" mean \"Target:\" target-mean)
                (println \"Std:\" std \"Target:\" target-std))
              
              (when (af-is-window-closed window)
                (break))))
          
          (af-destroy-window window)))
      ```
   
   5. **Educational Demonstrations**
      - Teach statistical concepts
      - Illustrate probability distributions
      - Demonstrate central limit theorem
      - Show effects of transformations
   
      Example: Random distribution demo
      ```clojure
      (defn demo-distributions []
        (let [window (af-create-window 1200 900 \"Distribution Gallery\")
              _ (af-grid window 2 2)
              n-samples 100000
              
              ;; Generate different distributions
              uniform (af-randu n-samples)
              normal (af-randn n-samples)
              exponential (af-neg (af-log uniform))
              
              ;; Compute histograms
              hist-u (af-histogram uniform 50 0.0 1.0)
              hist-n (af-histogram normal 50 -4.0 4.0)
              hist-e (af-histogram exponential 50 0.0 5.0)
              
              props-u {:row 0 :col 0 :title \"Uniform [0,1]\" :cmap AF_COLORMAP_DEFAULT}
              props-n {:row 0 :col 1 :title \"Normal (0,1)\" :cmap AF_COLORMAP_DEFAULT}
              props-e {:row 1 :col 0 :title \"Exponential (λ=1)\" :cmap AF_COLORMAP_DEFAULT}]
          
          ;; Display all distributions
          (while (not (af-is-window-closed window))
            (af-draw-hist window hist-u 0.0 1.0 props-u)
            (af-draw-hist window hist-n -4.0 4.0 props-n)
            (af-draw-hist window hist-e 0.0 5.0 props-e)
            (af-show window))
          
          (af-destroy-window window)))
      ```
   
   Advanced Features
   -----------------
   
   1. **Custom Axes Limits**
      Override automatic scaling to focus on specific ranges:
      
      ```clojure
      ;; Set custom Y-axis limit to zoom into low frequencies
      (af-set-axes-limits window hist 0.0 255.0 0.0 1000.0 false)
      (af-draw-hist window hist 0.0 255.0 props)
      ```
   
   2. **Axes Labels and Titles**
      Customize chart appearance:
      
      ```clojure
      ;; Set axes titles
      (af-set-axes-titles window \"Pixel Intensity\" \"Frequency\" nil)
      
      ;; Set number format for axes labels
      (af-set-axes-label-format window \"%.0f\" \"%.0f\" nil)
      ```
   
   3. **Multiple Windows**
      Display different visualizations simultaneously:
      
      ```clojure
      (let [win1 (af-create-window 800 600 \"Original\")
            win2 (af-create-window 800 600 \"Processed\")
            
            hist1 (af-histogram img1 256 0.0 255.0)
            hist2 (af-histogram img2 256 0.0 255.0)
            
            props {:row -1 :col -1 :title nil :cmap AF_COLORMAP_DEFAULT}]
        
        (while (and (not (af-is-window-closed win1))
                   (not (af-is-window-closed win2)))
          (af-draw-hist win1 hist1 0.0 255.0 props)
          (af-draw-hist win2 hist2 0.0 255.0 props)
          (af-show win1)
          (af-show win2))
        
        (af-destroy-window win1)
        (af-destroy-window win2))
      ```
   
   4. **Combined Visualizations**
      Show image alongside its histogram:
      
      ```clojure
      (let [window (af-create-window 1200 600 \"Image & Histogram\")
            _ (af-grid window 1 2)
            
            img (load-image \"photo.jpg\" false)
            hist (af-histogram img 256 0.0 255.0)
            
            props-img {:row 0 :col 0 :title \"Image\" :cmap AF_COLORMAP_DEFAULT}
            props-hist {:row 0 :col 1 :title \"Histogram\" :cmap AF_COLORMAP_DEFAULT}]
        
        (while (not (af-is-window-closed window))
          (af-draw-image window img props-img)
          (af-draw-hist window hist 0.0 255.0 props-hist)
          (af-show window))
        
        (af-destroy-window window))
      ```
   
   Performance Considerations
   --------------------------
   
   1. **Rendering Performance**
      - OpenGL-accelerated rendering: typically 60+ FPS
      - Bar count impact: 10-1000 bins renders efficiently
      - Very high bin counts (>1000) may reduce frame rate
      - GPU overhead: ~1-5ms per histogram
   
   2. **Memory Usage**
      - Histogram data copied to GPU: size = nbins × sizeof(type)
      - Window buffers: ~1-10 MB depending on size
      - Multiple histograms share GPU context
   
   3. **Optimization Tips**
      - Reuse windows across frames (don't recreate)
      - Update histogram data in-place when possible
      - Use lower bin counts for real-time visualization (e.g., 64-128)
      - Limit window update rate if CPU/GPU bound (e.g., 30 FPS)
   
   4. **Real-Time Constraints**
      - Interactive visualization: target 30-60 FPS
      - High-speed data: consider downsampling or update throttling
      - Network streaming: buffer to smooth frame delivery
   
   Troubleshooting
   ---------------
   
   1. **Problem: AF_ERR_NOT_SUPPORTED**
      - Cause: ArrayFire not built with Forge graphics support
      - Solution: Rebuild ArrayFire with -DBUILD_FORGE=ON
      - Workaround: Use external plotting library (e.g., matplotlib via Python interop)
   
   2. **Problem: Window doesn't appear**
      - Cause: af_show not called, or called before af_draw_hist
      - Solution: Always call af_show after all drawing operations
      - Pattern: draw → draw → ... → show (batch draws, single show)
   
   3. **Problem: Histogram bars too small or too large**
      - Cause: Incorrect minval/maxval range
      - Solution: Use actual data range, not histogram count range
      - Check: minval and maxval should match values used in af_histogram
   
   4. **Problem: X is not a vector error**
      - Cause: Multi-dimensional array passed as histogram data
      - Solution: Flatten or ensure af_histogram returned 1D array
      - Example: Use af_flat if needed, though af_histogram should return 1D
   
   5. **Problem: Poor performance / low FPS**
      - Cause: Too many bins, inefficient render loop, or GPU bottleneck
      - Solutions:
        * Reduce bin count (e.g., 256 → 128 or 64)
        * Limit update rate (e.g., render every Nth frame)
        * Profile with af_sync to identify bottlenecks
        * Check GPU utilization
   
   Comparison with Other Visualization Approaches
   -----------------------------------------------
   
   | Approach              | Speed    | Interactivity | Dependencies | Use Case              |
   |-----------------------|----------|---------------|--------------|----------------------|
   | af_draw_hist (Forge)  | Very Fast| High          | ArrayFire+GL | Real-time, embedded  |
   | Python matplotlib     | Slow     | Low           | Python       | Static publication   |
   | R ggplot2             | Slow     | Low           | R            | Statistical analysis |
   | D3.js (web)           | Medium   | High          | Browser      | Web dashboards       |
   | gnuplot               | Medium   | Medium        | gnuplot      | Scripting automation |
   
   Integration with Other ArrayFire Visualization
   ----------------------------------------------
   
   af_draw_hist works seamlessly with other Forge visualization functions
   in the same window:
   
   - **af_draw_image**: Display images
   - **af_draw_plot**: Line plots, scatter plots
   - **af_draw_surface**: 3D surface plots
   - **af_draw_vector_field**: Vector field visualization
   
   All can be combined using grid layout for comprehensive dashboards.
   
   References
   ----------
   
   - ArrayFire Forge Documentation: https://arrayfire.org/docs/group__gfx__func__draw.htm
   - Forge Graphics Library: https://github.com/arrayfire/forge
   - OpenGL Specification: https://www.opengl.org/documentation
   - ArrayFire Examples: arrayfire/examples/graphics/histogram.cpp
   
   See Also
   --------
   
   Related ArrayFire functions:
   - af-histogram: Compute histogram bin counts (required input)
   - af-hist-equal: Histogram equalization (image enhancement)
   - af-create-window: Create visualization window (required)
   - af-grid: Setup multi-chart grid layout
   - af-set-axes-limits: Override automatic axes scaling
   - af-set-axes-titles: Set axes labels
   - af-show: Display window with rendered content
   - af-is-window-closed: Check if window should close
   - af-destroy-window: Clean up window resources
   
   API Version: >= 1.0
   Introduced: ArrayFire 1.0 (initial graphics support)
   "
  "af_draw_hist" [::mem/pointer ::mem/pointer ::mem/double ::mem/double ::mem/pointer] ::mem/int)
