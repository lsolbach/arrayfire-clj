(ns org.soulspace.arrayfire.integration.unified-api.graphic-test
  (:require [clojure.test :refer [deftest is testing run-test run-tests]]
            [org.soulspace.arrayfire.ffi.base.definitions :as defs]
            [org.soulspace.arrayfire.integration.base.resource :as res]
            [org.soulspace.arrayfire.integration.unified-api.graphic :as graphic]
            [org.soulspace.arrayfire.integration.unified-api.data :as data]
            [org.soulspace.arrayfire.integration.unified-api.device :as device]))

;;;
;;; Note: Graphics tests require ArrayFire built with Forge support.
;;; These tests may be skipped in headless environments.
;;;
;;; The oneAPI backend has OpenGL interop issues that cause chart-based
;;; draw functions (plot, scatter, hist, surface, vector field) to hang.
;;; The internal `fg_draw_chart` path performs ArrayFire compute operations
;;; (copy_plot, reduce_all for axis limits) while the OpenGL context is
;;; current, causing a deadlock on the oneAPI backend.
;;; The `draw-image!` function uses a simpler `fg_draw_image` path that
;;; does not trigger this issue.
;;; The `with-graphics-backend` macro switches to the CPU backend for
;;; chart-based draw tests when the oneAPI backend is active.
;;;

(defn has-forge-support?
  "Check if ArrayFire was built with Forge graphics support."
  []
  (try
    (device/init!)
    (let [window (graphic/create-window 100 100 "Test")]
      (graphic/destroy-window! window)
      true)
    (catch Exception e
      false)))

(defmacro with-graphics-backend
  "Execute body with a Forge-compatible backend for chart-based rendering.
   Switches to CPU backend when oneAPI is active (due to OpenGL interop issues),
   and restores the original backend afterward."
  [& body]
  `(let [original-backend# (device/get-active-backend)]
     (when (= defs/AF_BACKEND_ONEAPI original-backend#)
       (device/set-backend! defs/AF_BACKEND_CPU))
     (try
       ~@body
       (finally
         (when (not= original-backend# (device/get-active-backend))
           (device/set-backend! original-backend#))))))

;;;
;;; Window Management Tests
;;;

(deftest test-create-window
  (testing "create-window creates new graphics window"
    (when (has-forge-support?)
      (device/init!)
      (let [window (graphic/create-window 800 600 "Test Window")]
        (is (integer? window))
        (is (not (zero? window)))
        (graphic/destroy-window! window)))))

(deftest test-create-window-various-sizes
  (testing "create-window with different dimensions"
    (when (has-forge-support?)
      (device/init!)
      (let [small (graphic/create-window 320 240 "Small")
            large (graphic/create-window 1920 1080 "Large")]
        (is (integer? small))
        (is (integer? large))
        (graphic/destroy-window! small)
        (graphic/destroy-window! large)))))

(deftest test-set-position
  (testing "set-position! moves window to screen coordinates"
    (when (has-forge-support?)
      (device/init!)
      (let [window (graphic/create-window 640 480 "Position Test")]
        (is (nil? (graphic/set-position! window 100 100)))
        (graphic/destroy-window! window)))))

(deftest test-set-title
  (testing "set-title! changes window title"
    (when (has-forge-support?)
      (device/init!)
      (let [window (graphic/create-window 640 480 "Initial Title")]
        (is (nil? (graphic/set-title! window "Updated Title")))
        (graphic/show! window)
        (graphic/destroy-window! window)))))

(deftest test-set-size
  (testing "set-size! resizes window"
    (when (has-forge-support?)
      (device/init!)
      (let [window (graphic/create-window 640 480 "Resize Test")]
        (is (nil? (graphic/set-size! window 800 600)))
        (graphic/destroy-window! window)))))

(deftest test-grid
  (testing "grid! sets up subplot layout"
    (when (has-forge-support?)
      (device/init!)
      (let [window (graphic/create-window 800 600 "Grid Test")]
        (is (nil? (graphic/grid! window 2 2)))
        (is (nil? (graphic/grid! window 1 3)))
        (graphic/destroy-window! window)))))

(deftest test-set-visibility
  (testing "set-visibility! controls window visibility"
    (when (has-forge-support?)
      (device/init!)
      (let [window (graphic/create-window 640 480 "Visibility Test")]
        (is (nil? (graphic/set-visibility! window true)))
        (is (nil? (graphic/set-visibility! window false)))
        (graphic/destroy-window! window)))))

(deftest test-is-window-closed
  (testing "is-window-closed? checks window state"
    (when (has-forge-support?)
      (device/init!)
      (let [window (graphic/create-window 640 480 "Close Test")]
        (is (boolean? (graphic/is-window-closed? window)))
        (graphic/destroy-window! window)))))

(deftest test-destroy-window
  (testing "destroy-window! frees window resources"
    (when (has-forge-support?)
      (device/init!)
      (let [window (graphic/create-window 640 480 "Destroy Test")]
        (is (nil? (graphic/destroy-window! window)))))))

(deftest test-destroy-window-nil
  (testing "destroy-window! handles nil gracefully"
    (is (nil? (graphic/destroy-window! nil)))))

;;;
;;; Axis Configuration Tests
;;;

(deftest test-set-axes-limits-2d
  (testing "set-axes-limits-2d! sets explicit 2D axis ranges"
    (when (has-forge-support?)
      (device/init!)
      (let [window (graphic/create-window 640 480 "Axes 2D Test")]
        (is (nil? (graphic/set-axes-limits-2d! window 0.0 10.0 -1.0 1.0 true nil)))
        (graphic/destroy-window! window)))))

(deftest test-set-axes-limits-3d
  (testing "set-axes-limits-3d! sets explicit 3D axis ranges"
    (when (has-forge-support?)
      (device/init!)
      (let [window (graphic/create-window 640 480 "Axes 3D Test")]
        (is (nil? (graphic/set-axes-limits-3d! window 
                                               -10.0 10.0 
                                               -10.0 10.0 
                                               0.0 5.0 
                                               true nil)))
        (graphic/destroy-window! window)))))

(deftest test-set-axes-titles
  (testing "set-axes-titles! sets axis labels"
    (when (has-forge-support?)
      (device/init!)
      (let [window (graphic/create-window 640 480 "Titles Test")]
        (is (nil? (graphic/set-axes-titles! window "X Axis" "Y Axis" nil nil)))
        (is (nil? (graphic/set-axes-titles! window "Time" "Amplitude" "Phase" nil)))
        (graphic/destroy-window! window)))))

(deftest test-set-axes-label-format
  (testing "set-axes-label-format! sets numeric format for labels"
    (when (has-forge-support?)
      (device/init!)
      (let [window (graphic/create-window 640 480 "Format Test")]
        (is (nil? (graphic/set-axes-label-format! window "%.2f" "%.2f" nil nil)))
        (is (nil? (graphic/set-axes-label-format! window "%.2e" "%.2e" "%.2e" nil)))
        (graphic/destroy-window! window)))))

;;;
;;; Drawing Functions Tests
;;;

(deftest test-draw-image
  (testing "draw-image! displays array as image"
    (when (has-forge-support?)
      (device/init!)
      (with-graphics-backend
        (let [window (graphic/create-window 640 480 "Image Test")
              img (data/constant 0.5 [100 100] defs/AF_DTYPE_F32)]
          (try
            (is (nil? (graphic/draw-image! window img nil)))
            (graphic/show! window)
            (finally
              (.close img)
              (graphic/destroy-window! window))))))))

(deftest test-draw-plot-2d
  (testing "draw-plot-2d! draws 2D line plot"
    (when (has-forge-support?)
      (device/init!)
      (with-graphics-backend
        (let [window (graphic/create-window 640 480 "Plot 2D Test")
              x (data/range [100] 0 defs/AF_DTYPE_F32)
              y (data/constant 1.0 [100] defs/AF_DTYPE_F32)]
          (try
            (is (nil? (graphic/draw-plot-2d! window x y nil)))
            (graphic/show! window)
            (finally
              (.close x)
              (.close y)
              (graphic/destroy-window! window))))))))

(deftest test-draw-plot-3d
  (testing "draw-plot-3d! draws 3D line plot"
    (when (has-forge-support?)
      (device/init!)
      (with-graphics-backend
        (let [window (graphic/create-window 640 480 "Plot 3D Test")
              x (data/range [50] 0 defs/AF_DTYPE_F32)
              y (data/range [50] 0 defs/AF_DTYPE_F32)
              z (data/range [50] 0 defs/AF_DTYPE_F32)]
          (try
            (is (nil? (graphic/draw-plot-3d! window x y z nil)))
            (graphic/show! window)
            (finally
              (.close x)
              (.close y)
              (.close z)
              (graphic/destroy-window! window))))))))

(deftest test-draw-plot-nd
  (testing "draw-plot-nd! draws N-dimensional line plot"
    (when (has-forge-support?)
      (device/init!)
      (with-graphics-backend
        (let [window (graphic/create-window 640 480 "Plot ND Test")
              points (data/constant 1.0 [50 2] defs/AF_DTYPE_F32)]
          (try
            (is (nil? (graphic/draw-plot-nd! window points nil)))
            (graphic/show! window)
            (finally
              (.close points)
              (graphic/destroy-window! window))))))))

(deftest test-draw-scatter-2d
  (testing "draw-scatter-2d! draws 2D scatter plot"
    (when (has-forge-support?)
      (device/init!)
      (with-graphics-backend
        (let [window (graphic/create-window 640 480 "Scatter 2D Test")
              x (data/range [50] 0 defs/AF_DTYPE_F32)
              y (data/range [50] 0 defs/AF_DTYPE_F32)
              marker 0] ; Marker type constant
          (try
            (is (nil? (graphic/draw-scatter-2d! window x y marker nil)))
            (graphic/show! window)
            (finally
              (.close x)
              (.close y)
              (graphic/destroy-window! window))))))))

(deftest test-draw-scatter-3d
  (testing "draw-scatter-3d! draws 3D scatter plot"
    (when (has-forge-support?)
      (device/init!)
      (with-graphics-backend
        (let [window (graphic/create-window 640 480 "Scatter 3D Test")
              x (data/range [30] 0 defs/AF_DTYPE_F32)
              y (data/range [30] 0 defs/AF_DTYPE_F32)
              z (data/range [30] 0 defs/AF_DTYPE_F32)
              marker 0]
          (try
            (is (nil? (graphic/draw-scatter-3d! window x y z marker nil)))
            (graphic/show! window)
            (finally
              (.close x)
              (.close y)
              (.close z)
              (graphic/destroy-window! window))))))))

(deftest test-draw-scatter-nd
  (testing "draw-scatter-nd! draws N-dimensional scatter plot"
    (when (has-forge-support?)
      (device/init!)
      (with-graphics-backend
        (let [window (graphic/create-window 640 480 "Scatter ND Test")
              points (data/constant 1.0 [30 2] defs/AF_DTYPE_F32)
              marker 0]
          (try
            (is (nil? (graphic/draw-scatter-nd! window points marker nil)))
            (graphic/show! window)
            (finally
              (.close points)
              (graphic/destroy-window! window))))))))

(deftest test-draw-hist
  (testing "draw-hist! draws histogram"
    (when (has-forge-support?)
      (device/init!)
      (with-graphics-backend
        (let [window (graphic/create-window 640 480 "Histogram Test")
              hist-data (data/constant 10.0 [256] defs/AF_DTYPE_F32)]
          (try
            (is (nil? (graphic/draw-hist! window hist-data 0.0 255.0 nil)))
            (graphic/show! window)
            (finally
              (.close hist-data)
              (graphic/destroy-window! window))))))))

(deftest test-draw-surface
  (testing "draw-surface! draws 3D surface plot"
    (when (has-forge-support?)
      (device/init!)
      (with-graphics-backend
        (let [window (graphic/create-window 640 480 "Surface Test")
              x (data/range [20 20] 0 defs/AF_DTYPE_F32)
              y (data/range [20 20] 1 defs/AF_DTYPE_F32)
              z (data/constant 0.5 [20 20] defs/AF_DTYPE_F32)]
          (try
            (is (nil? (graphic/draw-surface! window x y z nil)))
            (graphic/show! window)
            (finally
              (.close x)
              (.close y)
              (.close z)
              (graphic/destroy-window! window))))))))

(deftest test-draw-vector-field-2d
  (testing "draw-vector-field-2d! draws 2D vector field"
    (when (has-forge-support?)
      (device/init!)
      (with-graphics-backend
        (let [window (graphic/create-window 640 480 "Vector Field 2D Test")
              x-points (data/range [100] 0 defs/AF_DTYPE_F32)
              y-points (data/range [100] 0 defs/AF_DTYPE_F32)
              x-dirs (data/constant 0.1 [100] defs/AF_DTYPE_F32)
              y-dirs (data/constant 0.1 [100] defs/AF_DTYPE_F32)]
          (try
            (is (nil? (graphic/draw-vector-field-2d! window x-points y-points x-dirs y-dirs nil)))
            (graphic/show! window)
            (finally
              (.close x-points)
              (.close y-points)
              (.close x-dirs)
              (.close y-dirs)
              (graphic/destroy-window! window))))))))

(deftest test-draw-vector-field-3d
  (testing "draw-vector-field-3d! draws 3D vector field"
    (when (has-forge-support?)
      (device/init!)
      (with-graphics-backend
        (let [window (graphic/create-window 640 480 "Vector Field 3D Test")
              x-points (data/range [50] 0 defs/AF_DTYPE_F32)
              y-points (data/range [50] 0 defs/AF_DTYPE_F32)
              z-points (data/range [50] 0 defs/AF_DTYPE_F32)
              x-dirs (data/constant 0.1 [50] defs/AF_DTYPE_F32)
              y-dirs (data/constant 0.1 [50] defs/AF_DTYPE_F32)
              z-dirs (data/constant 0.1 [50] defs/AF_DTYPE_F32)]
          (try
            (is (nil? (graphic/draw-vector-field-3d! window 
                                                     x-points y-points z-points 
                                                     x-dirs y-dirs z-dirs nil)))
            (graphic/show! window)
            (finally
              (.close x-points)
              (.close y-points)
              (.close z-points)
              (.close x-dirs)
              (.close y-dirs)
              (.close z-dirs)
              (graphic/destroy-window! window))))))))

(deftest test-draw-vector-field-nd
  (testing "draw-vector-field-nd! draws N-dimensional vector field"
    (when (has-forge-support?)
      (device/init!)
      (with-graphics-backend
        (let [window (graphic/create-window 640 480 "Vector Field ND Test")
              points (data/constant 1.0 [20 2] defs/AF_DTYPE_F32)
              directions (data/constant 0.1 [20 2] defs/AF_DTYPE_F32)]
          (try
            (is (nil? (graphic/draw-vector-field-nd! window points directions nil)))
            (graphic/show! window)
            (finally
              (.close points)
              (.close directions)
              (graphic/destroy-window! window))))))))

;;;
;;; Window Lifecycle Tests
;;;

(deftest test-window-complete-lifecycle
  (testing "complete window lifecycle: create, configure, draw, destroy"
    (when (has-forge-support?)
      (device/init!)
      (with-graphics-backend
        (let [window (graphic/create-window 800 600 "Lifecycle Test")]
          (try
            (graphic/set-position! window 100 100)
            (graphic/set-title! window "Updated")
            (graphic/grid! window 2 2)
            (let [data (data/constant 1.0 [50] defs/AF_DTYPE_F32)]
              (try
                (graphic/draw-plot-2d! window data data nil)
                (graphic/show! window)
                (finally
                  (.close data))))
            (finally
              (graphic/destroy-window! window))))))))

(deftest test-multiple-windows
  (testing "multiple windows can coexist"
    (when (has-forge-support?)
      (device/init!)
      (let [win1 (graphic/create-window 400 300 "Window 1")
            win2 (graphic/create-window 400 300 "Window 2")]
        (try
          (is (not= win1 win2))
          (is (integer? win1))
          (is (integer? win2))
          (finally
            (graphic/destroy-window! win1)
            (graphic/destroy-window! win2)))))))

(comment
  ;; run all tests from REPL
  (run-tests)
  
  ;; run individual tests
  (run-test test-create-window)
  (run-test test-create-window-various-sizes)
  (run-test test-set-position)
  (run-test test-set-title)
  (run-test test-set-size)
  (run-test test-grid)
  (run-test test-set-visibility)
  (run-test test-is-window-closed)
  (run-test test-destroy-window)
  (run-test test-destroy-window-nil)
  (run-test test-set-axes-limits-2d)
  (run-test test-set-axes-limits-3d)
  (run-test test-set-axes-titles)
  (run-test test-set-axes-label-format)
  (run-test test-draw-image)
  ;; Chart-based draw tests use with-graphics-backend macro
  ;; to switch to CPU backend on oneAPI (due to OpenGL interop issues)
  (run-test test-draw-plot-2d)
  (run-test test-draw-plot-3d)
  (run-test test-draw-plot-nd)
  (run-test test-draw-scatter-2d)
  (run-test test-draw-scatter-3d)
  (run-test test-draw-scatter-nd)
  (run-test test-draw-hist)
  (run-test test-draw-surface)
  (run-test test-draw-vector-field-2d)
  (run-test test-draw-vector-field-3d)
  (run-test test-draw-vector-field-nd)
  (run-test test-window-complete-lifecycle)
  (run-test test-multiple-windows)
  
  ;
  )
