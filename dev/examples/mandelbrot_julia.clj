(ns examples.mandelbrot-julia
  "Mandelbrot & Julia fractal — animated GPU-accelerated showcases.

   mandelbrot-zoom:  Continuously zooms into the Seahorse Valley of the
                     Mandelbrot set, revealing infinite fractal detail.
   julia-dance:      Morphs a Julia set by rotating the c parameter along
                     a circle in the complex plane — a mesmerizing animation.

   Both run entirely on the GPU using complex-valued arrays.

   Usage (REPL):
     (mandelbrot-zoom 800)  ; zoom into Seahorse Valley
     (julia-dance 800)      ; rotating Julia set animation"
  (:require [org.soulspace.arrayfire.api.core :as af]
            [org.soulspace.arrayfire.api.graphic :as gfx]))

(defn- build-complex-grid
  "Build an N×N complex grid covering [x0,x1] × [y0,y1]."
  [n x0 x1 y0 y1]
  (let [dx   (af// (af/- x1 x0) (double n))
        dy   (af// (af/- y1 y0) (double n))
        idx  (af/iota [n] [1] :f64)
        xs   (af/+ (af/* idx (af/constant dx [n])) (af/constant x0 [n]))
        ys   (af/+ (af/* idx (af/constant dy [n])) (af/constant y0 [n]))
        xx   (af/tile xs [1 n])
        yy   (af/tile (af/reshape ys [1 n]) [n 1])]
    (af/complex xx yy)))

(defn- escape-time
  "Run escape-time iteration.  z₀ and c are N×N complex AFArrays.
   Returns a float array of per-pixel iteration counts."
  [z0 c n max-iter]
  (loop [z      z0
         counts (af/constant 0.0 [n n])
         i      0]
    (if (>= i max-iter)
      (af/cast counts :f32)
      (let [z-new   (af/+ (af/* z z) c)
            alive   (af/lt (af/abs z-new) 2.0)
            z-next  (af/select-where alive z-new
                      (af/constant-complex 0.0 0.0 [n n] :c64))
            counts+ (af/+ counts (af/cast alive :f64))]
        (recur z-next counts+ (inc i))))))

(defn- compute-frame
  "Compute one escape-time frame and normalize to [0,1] for display."
  [n max-iter x0 x1 y0 y1 mode-c]
  (let [c-grid (build-complex-grid n x0 x1 y0 y1)
        z0     (if mode-c
                 c-grid
                 (af/constant-complex 0.0 0.0 [n n] :c64))
        c      (if mode-c
                 (af/constant-complex (first mode-c) (second mode-c) [n n] :c64)
                 c-grid)
        counts (escape-time z0 c n max-iter)
        peak   (af/max (af/max counts))
        safe-p (af/maxof peak (af/constant 1.0 [1]))] ;; avoid / 0
    (af// counts safe-p)))

;; --- Mandelbrot Zoom ---

(def ^:private zoom-center
  "Seahorse Valley — a famously beautiful Mandelbrot region."
  [-0.745 0.186])

(defn mandelbrot-zoom
  "Animated zoom into the Seahorse Valley of the Mandelbrot set.
   Each frame zooms 3% deeper, increasing iteration depth with zoom.
   Close the window to stop."
  [n]
  (af/with-arrayfire {:backend :opencl :converter-fn af/->value}
    (gfx/with-window [w n n "Mandelbrot Zoom (arrayfire-clj)"]
      (gfx/set-visibility! w true)
      (let [[cx cy] zoom-center]
        (loop [frame 0]
          (when-not (gfx/window-closed? w)
            (let [zoom    (Math/pow 0.97 frame)
                  half    (af/* 1.5 zoom)
                  max-it  (af/+ 50 (af/* frame 2))
                  img     (compute-frame n max-it
                            (af/- cx half) (af/+ cx half)
                            (af/- cy half) (af/+ cy half)
                            nil)]
              (gfx/draw-image! w img)
              (gfx/show! w)
              (recur (inc frame)))))))))

;; --- Julia Dance ---

(defn julia-dance
  "Animated Julia set — c rotates along a circle in the complex plane,
   producing a continuously morphing fractal dance.
   Close the window to stop."
  ([n] (julia-dance n 0.7885))
  ([n radius]
   (af/with-arrayfire {:backend :opencl :converter-fn af/->value}
     (gfx/with-window [w n n "Julia Dance (arrayfire-clj)"]
       (gfx/set-visibility! w true)
       (loop [theta 0.0]
         (when-not (gfx/window-closed? w)
           (let [cr  (af/* radius (Math/cos theta))
                 ci  (af/* radius (Math/sin theta))
                 img (compute-frame n 80 -2.0 2.0 -2.0 2.0 [cr ci])]
             (gfx/draw-image! w img)
             (gfx/show! w)
             (recur (af/+ theta 0.02)))))))))

(comment
  ;; Mandelbrot zoom — watch it dive into the Seahorse Valley
  (mandelbrot-zoom 800)

  ;; Julia dance — mesmerizing morphing fractal
  (julia-dance 800)

  ;; Julia dance with different radius (try 0.5 to 0.9)
  (julia-dance 800 0.75)

  ;;
  )
