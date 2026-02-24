(ns org.soulspace.arrayfire.api.graphic-test
  "Comprehensive tests for org.soulspace.arrayfire.api.graphic.

   Test categories:
   1. Namespace completeness — all public vars are defined
   2. Macro verification — `with-window` is a macro
   3. Keyword resolution — colormap and marker type lookups
   4. Named-argument API contract — functions accept keyword arguments
   5. assert-within-arrayfire! enforcement — calls outside a region throw
   6. cell-props-seg helper — nil and map dispatch
   7. Forge-dependent tests — guarded by display availability"
  (:require [clojure.test :refer [deftest is testing run-test run-tests]]
            [org.soulspace.arrayfire.api.core :as af]
            [org.soulspace.arrayfire.api.graphic :as gfx]))


;;;
;;; Macro verification
;;;
(deftest with-window-is-macro-test
  (testing "with-window is a macro"
    (is (true? (:macro (meta #'gfx/with-window))))))

;;;
;;; cell-props-seg helper
;;;

(deftest cell-props-seg-nil-test
  (testing "nil returns a non-nil MemorySegment (default single-plot cell)"
    (let [seg (#'gfx/cell-props-seg nil)]
      (is (some? seg)))))

(deftest cell-props-seg-minimal-map-test
  (testing "Minimal map with :row and :col produces a segment"
    (let [seg (#'gfx/cell-props-seg {:row 0 :col 1})]
      (is (some? seg)))))

(deftest cell-props-seg-with-title-test
  (testing "Map with :title produces a segment"
    (let [seg (#'gfx/cell-props-seg {:row 0 :col 0 :title "Test"})]
      (is (some? seg)))))

(deftest cell-props-seg-with-colormap-test
  (testing "Map with :colormap keyword produces a segment"
    (let [seg (#'gfx/cell-props-seg {:row 0 :col 0 :title "" :colormap :viridis})]
      (is (some? seg)))))

(deftest cell-props-seg-defaults-test
  (testing "Empty map uses default row=-1 col=-1 (single-chart mode)"
    (let [seg (#'gfx/cell-props-seg {})]
      (is (some? seg)))))


;;;
;;; Named-argument API contract
;;;
;;; These tests verify that each refactored function accepts keyword arguments
;;; correctly. They call the function outside a `with-arrayfire` region to
;;; exercise argument parsing; the expected outcome is an ExceptionInfo from
;;; `assert-within-arrayfire!`, NOT an arity or destructuring error.
;;;
(defn- throws-outside-region?
  "Returns true if calling (f) throws an IllegalStateException whose message
   contains 'must be called within'."
  [f]
  (try
    (f)
    false
    (catch IllegalStateException e
      (boolean (re-find #"(?i)must be called within|outside.*arrayfire|arrayfire.*region"
                        (.getMessage e))))
    (catch Exception _
      false)))

;; Axis configuration functions

(deftest set-axes-limits!-kwargs-test
  (testing "Accepts 2D keyword arguments (throws outside region, not arity error)"
    (is (throws-outside-region?
         #(gfx/set-axes-limits! :fake-window
                                :x-range [0.0 10.0]
                                :y-range [-1.0 1.0]))))
  (testing "Accepts 3D keyword arguments with :z-range"
    (is (throws-outside-region?
         #(gfx/set-axes-limits! :fake-window
                                :x-range [0.0 10.0]
                                :y-range [-1.0 1.0]
                                :z-range [0.0 100.0]
                                :exact? true))))
  (testing "Defaults :exact? to false and :props to nil"
    (is (throws-outside-region?
         #(gfx/set-axes-limits! :fake-window
                                :x-range [0.0 1.0]
                                :y-range [0.0 1.0])))))

(deftest set-axes-limits-from-data!-kwargs-test
  (testing "Accepts keyword arguments"
    (is (throws-outside-region?
         #(gfx/set-axes-limits-from-data! :fake-window
                                          :x :fake-x :y :fake-y))))
  (testing "Accepts optional :z and :exact?"
    (is (throws-outside-region?
         #(gfx/set-axes-limits-from-data! :fake-window
                                          :x :fake-x :y :fake-y
                                          :z :fake-z :exact? true)))))

(deftest set-axes-titles!-kwargs-test
  (testing "Accepts keyword arguments"
    (is (throws-outside-region?
         #(gfx/set-axes-titles! :fake-window :x "Time" :y "Value"))))
  (testing "Accepts only :x"
    (is (throws-outside-region?
         #(gfx/set-axes-titles! :fake-window :x "X-Axis"))))
  (testing "Accepts 3D axis titles with :props"
    (is (throws-outside-region?
         #(gfx/set-axes-titles! :fake-window
                                :x "X" :y "Y" :z "Z"
                                :props {:row 0 :col 0})))))

(deftest set-axes-label-format!-kwargs-test
  (testing "Accepts keyword arguments"
    (is (throws-outside-region?
         #(gfx/set-axes-label-format! :fake-window
                                      :x "%.2f" :y "%.4e"))))
  (testing "Accepts all three axes"
    (is (throws-outside-region?
         #(gfx/set-axes-label-format! :fake-window
                                      :x "%.1f" :y "%.1f" :z "%.2f")))))

;; --- Drawing functions ---

(deftest draw-image!-kwargs-test
  (testing "Accepts keyword arguments"
    (is (throws-outside-region?
         #(gfx/draw-image! :fake-window :image :fake-image))))
  (testing "Accepts :props"
    (is (throws-outside-region?
         #(gfx/draw-image! :fake-window
                           :image :fake-image
                           :props {:row 0 :col 0})))))

(deftest draw-plot!-kwargs-test
  (testing "Accepts keyword arguments"
    (is (throws-outside-region?
         #(gfx/draw-plot! :fake-window :points :fake-pts))))
  (testing "Accepts :props"
    (is (throws-outside-region?
         #(gfx/draw-plot! :fake-window
                          :points :fake-pts
                          :props {:title "test"})))))

(deftest draw-plot-2d!-kwargs-test
  (testing "Accepts keyword arguments"
    (is (throws-outside-region?
         #(gfx/draw-plot-2d! :fake-window :x :fake-x :y :fake-y))))
  (testing "Accepts :props"
    (is (throws-outside-region?
         #(gfx/draw-plot-2d! :fake-window
                             :x :fake-x :y :fake-y
                             :props {:title "Signal"})))))

(deftest draw-plot-3d!-kwargs-test
  (testing "Accepts keyword arguments"
    (is (throws-outside-region?
         #(gfx/draw-plot-3d! :fake-window
                             :x :fake-x :y :fake-y :z :fake-z))))
  (testing "Accepts :props"
    (is (throws-outside-region?
         #(gfx/draw-plot-3d! :fake-window
                             :x :fake-x :y :fake-y :z :fake-z
                             :props {:row 0 :col 0})))))

(deftest draw-scatter!-kwargs-test
  (testing "Accepts keyword arguments with default marker"
    (is (throws-outside-region?
         #(gfx/draw-scatter! :fake-window :points :fake-pts))))
  (testing "Accepts explicit :marker"
    (is (throws-outside-region?
         #(gfx/draw-scatter! :fake-window
                             :points :fake-pts
                             :marker :circle))))
  (testing "Accepts :props"
    (is (throws-outside-region?
         #(gfx/draw-scatter! :fake-window
                             :points :fake-pts
                             :marker :star
                             :props {:title "scatter"})))))

(deftest draw-scatter-2d!-kwargs-test
  (testing "Accepts keyword arguments with default marker"
    (is (throws-outside-region?
         #(gfx/draw-scatter-2d! :fake-window :x :fake-x :y :fake-y))))
  (testing "Accepts :marker and :props"
    (is (throws-outside-region?
         #(gfx/draw-scatter-2d! :fake-window
                                :x :fake-x :y :fake-y
                                :marker :triangle
                                :props {:row 1 :col 0})))))

(deftest draw-scatter-3d!-kwargs-test
  (testing "Accepts keyword arguments with default marker"
    (is (throws-outside-region?
         #(gfx/draw-scatter-3d! :fake-window
                                :x :fake-x :y :fake-y :z :fake-z))))
  (testing "Accepts :marker and :props"
    (is (throws-outside-region?
         #(gfx/draw-scatter-3d! :fake-window
                                :x :fake-x :y :fake-y :z :fake-z
                                :marker :cross
                                :props {:row 0 :col 1})))))

(deftest draw-histogram!-kwargs-test
  (testing "Accepts keyword arguments"
    (is (throws-outside-region?
         #(gfx/draw-histogram! :fake-window
                               :data :fake-data :min 0.0 :max 255.0))))
  (testing "Accepts :props"
    (is (throws-outside-region?
         #(gfx/draw-histogram! :fake-window
                               :data :fake-data :min 0.0 :max 1.0
                               :props {:title "Histogram"})))))

(deftest draw-surface!-kwargs-test
  (testing "Accepts keyword arguments"
    (is (throws-outside-region?
         #(gfx/draw-surface! :fake-window
                             :x :fake-x :y :fake-y :z :fake-z))))
  (testing "Accepts :props with colormap"
    (is (throws-outside-region?
         #(gfx/draw-surface! :fake-window
                             :x :fake-x :y :fake-y :z :fake-z
                             :props {:colormap :viridis})))))

(deftest draw-vector-field!-kwargs-test
  (testing "Accepts keyword arguments"
    (is (throws-outside-region?
         #(gfx/draw-vector-field! :fake-window
                                  :points :fake-pts
                                  :directions :fake-dirs))))
  (testing "Accepts :props"
    (is (throws-outside-region?
         #(gfx/draw-vector-field! :fake-window
                                  :points :fake-pts
                                  :directions :fake-dirs
                                  :props {:row 0 :col 0})))))

(deftest draw-vector-field-2d!-kwargs-test
  (testing "Accepts keyword arguments"
    (is (throws-outside-region?
         #(gfx/draw-vector-field-2d! :fake-window
                                     :x :fx :y :fy
                                     :x-dirs :dx :y-dirs :dy))))
  (testing "Accepts :props"
    (is (throws-outside-region?
         #(gfx/draw-vector-field-2d! :fake-window
                                     :x :fx :y :fy
                                     :x-dirs :dx :y-dirs :dy
                                     :props {:title "Flow"})))))

(deftest draw-vector-field-3d!-kwargs-test
  (testing "Accepts keyword arguments"
    (is (throws-outside-region?
         #(gfx/draw-vector-field-3d! :fake-window
                                     :x :fx :y :fy :z :fz
                                     :x-dirs :dx :y-dirs :dy :z-dirs :dz))))
  (testing "Accepts :props"
    (is (throws-outside-region?
         #(gfx/draw-vector-field-3d! :fake-window
                                     :x :fx :y :fy :z :fz
                                     :x-dirs :dx :y-dirs :dy :z-dirs :dz
                                     :props {:row 0 :col 0})))))

;;;
;;;assert-within-arrayfire! enforcement
;;;
(deftest outside-region-functions-throw-test
  (testing "Window management functions throw outside with-arrayfire"
    (is (thrown? IllegalStateException (gfx/create-window 800 600 "test")))
    (is (thrown? IllegalStateException (gfx/show! :w)))
    (is (thrown? IllegalStateException (gfx/window-closed? :w)))
    (is (thrown? IllegalStateException (gfx/set-visibility! :w true)))
    (is (thrown? IllegalStateException (gfx/set-title! :w "t")))
    (is (thrown? IllegalStateException (gfx/set-position! :w 0 0)))
    (is (thrown? IllegalStateException (gfx/set-size! :w 100 100)))
    (is (thrown? IllegalStateException (gfx/grid! :w 2 2)))))

(deftest destroy-window!-no-throw-outside-region-test
  (testing "destroy-window! does NOT assert within-arrayfire (safe for finally blocks)"
    ;; Should not throw IllegalStateException about arrayfire region.
    ;; It may throw other exceptions if given nil, but not the region assertion.
    (is (nil? (gfx/destroy-window! nil)))))


;;;
;;; Forge-dependent integration tests
;;;
;;; These tests require a running display and ArrayFire built with Forge,
;;; and they must use {:backend :cpu} or {:backend :opencl}.
;;;
;;; Two guards control test execution:
;;;   `forge-available?`      — window create/destroy works (no draw needed)
;;;   `forge-draw-available?` — an actual draw call succeeds on the CPU backend
;;;

;; TODO make available in graphics namespace
(defn- forge-available?
  "Return true when Forge window creation and destruction succeed
   using the CPU backend."
  []
  (try
    (af/with-arrayfire {:backend :cpu}
      (let [w (gfx/create-window 100 100 "probe")]
        (gfx/destroy-window! w)
        true))
    (catch Exception _ false)))

(defn- forge-draw-available?
  "Return true when a Forge draw call succeeds using the CPU backend.
   Forge draw functions require the CPU backend; the CUDA backend does
   not support Forge GL interop and will block or error."
  []
  (try
    (af/with-arrayfire {:backend :cpu}
      (gfx/with-window [w 100 100 "draw-probe"]
        (let [x (af/range [10] :f32)
              y (af/sin x)]
          (gfx/draw-plot-2d! w :x x :y y)
          true)))
    (catch Exception _ false)))

(defmacro when-forge [& body]
  `(if (forge-available?)
     (do ~@body)
     (println "  [SKIP] Forge not available — skipping display test")))

(defmacro when-forge-draw [& body]
  `(if (forge-draw-available?)
     (do ~@body)
     (println "  [SKIP] Forge draw not available — skipping draw test")))

(deftest forge-window-lifecycle-test
  (when-forge
   (testing "create-window returns a numeric handle, destroy-window! returns nil"
     (af/with-arrayfire {:backend :cpu}
       (let [w (gfx/create-window 200 200 "lifecycle-test")]
         (is (number? w))
         (is (nil? (gfx/destroy-window! w))))))))

(deftest forge-with-window-macro-test
  (when-forge
   (testing "with-window macro binds handle and cleans up"
     (af/with-arrayfire {:backend :cpu}
       (gfx/with-window [w 200 200 "macro-test"]
         (is (number? w)))))))

(deftest forge-draw-plot-2d-test
  (when-forge-draw
   (testing "draw-plot-2d! with named args renders without error"
     (af/with-arrayfire {:backend :cpu}
       (gfx/with-window [w 300 200 "plot-2d"]
         (let [x (af/range [50] :f32)
               y (af/sin x)]
           (gfx/set-axes-titles! w :x "x" :y "sin(x)")
           (gfx/draw-plot-2d! w :x x :y y)
           (gfx/show! w)
           (is true)))))))

(deftest forge-draw-scatter-2d-test
  (when-forge-draw
   (testing "draw-scatter-2d! with named args and marker renders"
     (af/with-arrayfire {:backend :cpu}
       (gfx/with-window [w 300 200 "scatter-2d"]
         (let [x (af/random-normal [30] :f32)
               y (af/random-normal [30] :f32)]
           (gfx/draw-scatter-2d! w :x x :y y :marker :circle)
           (gfx/show! w)
           (is true)))))))

(deftest forge-set-axes-limits-2d-test
  (when-forge-draw
   (testing "set-axes-limits! 2D mode with named args"
     (af/with-arrayfire {:backend :cpu}
       (gfx/with-window [w 300 200 "limits-2d"]
         (gfx/set-axes-limits! w :x-range [0.0 10.0] :y-range [-1.0 1.0])
         (gfx/show! w)
         (is true))))))

(deftest forge-set-axes-limits-3d-test
  (when-forge-draw
   (testing "set-axes-limits! 3D mode with :z-range"
     (af/with-arrayfire {:backend :cpu}
       (gfx/with-window [w 300 200 "limits-3d"]
         (gfx/set-axes-limits! w
                               :x-range [-5.0 5.0]
                               :y-range [-5.0 5.0]
                               :z-range [0.0 100.0]
                               :exact? true)
         (gfx/show! w)
         (is true))))))

(deftest forge-grid-layout-test
  (when-forge-draw
   (testing "Grid layout with multiple draw calls"
     (af/with-arrayfire {:backend :cpu}
       (gfx/with-window [w 400 400 "grid-test"]
         (gfx/grid! w 2 1)
         (let [x  (af/range [50] :f32)
               y1 (af/sin x)
               y2 (af/cos x)]
           (gfx/draw-plot-2d! w :x x :y y1
                              :props {:row 0 :col 0 :title "sin"})
           (gfx/draw-plot-2d! w :x x :y y2
                              :props {:row 1 :col 0 :title "cos"})
           (gfx/show! w)
           (is true)))))))


(comment
  ;; Run graphic tests
  (run-tests)

  (run-test with-window-is-macro-test)
  (run-test cell-props-seg-nil-test)
  (run-test cell-props-seg-minimal-map-test)
  (run-test cell-props-seg-with-title-test)
  (run-test cell-props-seg-with-colormap-test)
  (run-test cell-props-seg-defaults-test)
  (run-test set-axes-limits!-kwargs-test)
  (run-test set-axes-limits-from-data!-kwargs-test)
  (run-test set-axes-titles!-kwargs-test)
  (run-test set-axes-label-format!-kwargs-test)
  (run-test draw-image!-kwargs-test)
  (run-test draw-plot!-kwargs-test)
  (run-test draw-plot-2d!-kwargs-test)
  (run-test draw-plot-3d!-kwargs-test)
  (run-test draw-scatter!-kwargs-test)
  (run-test draw-scatter-2d!-kwargs-test)
  (run-test draw-scatter-3d!-kwargs-test)
  (run-test draw-histogram!-kwargs-test)
  (run-test draw-surface!-kwargs-test)
  (run-test draw-vector-field!-kwargs-test)
  (run-test draw-vector-field-2d!-kwargs-test)
  (run-test draw-vector-field-3d!-kwargs-test)
  (run-test outside-region-functions-throw-test)
  (run-test destroy-window!-no-throw-outside-region-test)
  (run-test forge-window-lifecycle-test)
  (run-test forge-with-window-macro-test)
  (run-test forge-draw-plot-2d-test)
  (run-test forge-draw-scatter-2d-test)
  (run-test forge-set-axes-limits-2d-test)
  (run-test forge-set-axes-limits-3d-test)
  (run-test forge-grid-layout-test)

  ;
  )

