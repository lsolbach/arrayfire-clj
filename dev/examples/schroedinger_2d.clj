(ns examples.schroedinger-2d
  "2D Schrödinger time evolution — GPU-accelerated split-operator FFT.

   Evolves a Gaussian wavepacket with initial momentum through a harmonic
   potential V(x,y) = ½(x² + y²) using the split-operator Fourier method.
   All complex FFT and element-wise phase operations run on the GPU.

   The split-operator step is exactly unitary, so ∑|ψ|² = 1 is preserved
   to machine precision across arbitrarily many time steps.

   Usage (REPL):
     (run-schroedinger 128 200)   ; 128×128 grid, 200 steps (headless)
     (animate-schroedinger 256)   ; 256×256 with live Forge window"
  (:require [org.soulspace.arrayfire.api.core :as af]
            [org.soulspace.arrayfire.api.signal-processing :as sp]
            [org.soulspace.arrayfire.api.graphic :as gfx]))

(def ^:private L 10.0)
(def ^:private dt 0.01)
(def ^:private sigma 1.0)
(def ^:private k0x 3.0)

(defn- build-grids
  "Build x/y position grids and kx/ky momentum grids (FFT-ordered)."
  [N]
  (let [dx (af// L N)
        dk (af// (af/* 2.0 Math/PI) L)
        idx (af/iota [N] [1] :f64)
        x-scaled (af/* idx (af/constant dx [N]))
        x-off (af/constant (af// L -2.0) [N])
        coords (af/+ x-scaled x-off)
        k-off (af/+ idx (af/constant (af// N -2.0) [N]))
        k-fft (af/shift (af/* k-off (af/constant dk [N]))
                        [(af// N 2)])]
    {:xg  (af/tile coords [1 N])
     :yg  (af/tile (af/reshape coords [1 N]) [N 1])
     :kxg (af/tile k-fft [1 N])
     :kyg (af/tile (af/reshape k-fft [1 N]) [N 1])}))

(defn- phase-op
  "exp(-i·phi) as a complex AFArray from a real phase array."
  [phi]
  (af/complex (af/cos phi) (af/sin phi)))

(defn- build-operators
  "Pre-compute the half-step V and full-step K phase operators."
  [N {:keys [xg yg kxg kyg]}]
  (let [r2 (af/+ (af/* xg xg) (af/* yg yg))
        V  (af/* (af/constant 0.5 [N N]) r2)
        half-dt-neg (af/- (af// dt 2.0))
        k2 (af/+ (af/* kxg kxg) (af/* kyg kyg))]
    {:phase-V (phase-op (af/* V (af/constant half-dt-neg [N N])))
     :phase-K (phase-op (af/* k2 (af/constant half-dt-neg [N N])))}))

(defn- init-wavepacket
  "Normalized Gaussian wavepacket with momentum k0x in the x-direction."
  [N {:keys [xg yg]}]
  ; TODO scope with result
  (let [r2 (af/+ (af/* xg xg) (af/* yg yg))
        gauss-c (af// -1.0 (af/* 2.0 sigma sigma))
        gauss (af/exp (af/* (af/constant gauss-c [N N]) r2))
        ph (af/* (af/constant k0x [N N]) xg)
        psi-raw (af/* (af/complex gauss (af/constant 0.0 [N N]))
                      (af/complex (af/cos ph) (af/sin ph)))
        mag (af/abs psi-raw)
        nv (Math/sqrt (double (af/->value (af/sum (af/sum (af/* mag mag))))))]
    (af/* psi-raw (af/constant-complex (af// 1.0 nv) 0.0 [N N]))))

(defn- split-step
  "One split-operator time step: half-V -> FFT -> K -> IFFT -> half-V."
  [psi phase-V phase-K]
  ; TODO scope with result
  (let [a (af/* phase-V psi)
        b (sp/fft2 a)
        c (af/* phase-K b)
        d (sp/ifft2-normalized c)]
    (af/* phase-V d)))

(defn run-schroedinger
  "Evolve wavepacket for n-steps, return final probability density and norm.
   Headless (no graphics)."
  [N n-steps]
  (af/with-arrayfire {:backend :cpu :converter-fn af/->value}
    (let [grids (build-grids N)
          {:keys [phase-V phase-K]} (build-operators N grids)
          psi0 (init-wavepacket N grids)
          psi-f (loop [psi psi0 i 0]
                  (if (>= i n-steps) psi
                      (recur (split-step psi phase-V phase-K)
                             (inc i))))
          mag (af/abs psi-f)]
      {:density (af/* mag mag)
       :norm (af/sum (af/sum (af/* mag mag)))
       :grid [N N]
       :steps n-steps})))

(defn- normalize-density
  "Compute |psi|^2 normalized to [0,1] for display."
  [psi]
  ; TODO scope with result
  (let [mag (af/abs psi)
        density (af/* mag mag)
        peak (af/max (af/max density))]
    (af/cast (af// density peak) :f32)))

(defn animate-schroedinger
  "Evolve wavepacket with live Forge graphics window.
   Shows probability density |psi(x,y,t)|^2 as a heatmap."
  [N]
  (af/with-arrayfire {:backend :opencl :converter-fn af/->value}
    (if (and (gfx/forge-available?) (gfx/forge-draw-available?))
      (let [grids (build-grids N)
            {:keys [phase-V phase-K]} (build-operators N grids)
            psi0 (init-wavepacket N grids)]
        (gfx/with-window [w N N "Schroedinger 2D (arrayfire-clj)"]
          (gfx/set-visibility! w true)
          (loop [psi psi0]
            (when-not (gfx/window-closed? w)
              (gfx/draw-image! w (normalize-density psi))
              (gfx/show! w)
              (recur (split-step psi phase-V phase-K))))))
      (println "Forge not available, skipping!"))))

(comment
  ;; Headless test: verify norm preservation
  (time (let [r (run-schroedinger 128 200)]
          (select-keys r [:norm :grid :steps])))

  ;; Animated (requires display server + Forge)
  (animate-schroedinger 256)
  
  ;
  )
