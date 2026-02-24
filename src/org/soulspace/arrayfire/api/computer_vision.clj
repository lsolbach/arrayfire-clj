(ns org.soulspace.arrayfire.api.computer-vision
  "Idiomatic Clojure API for ArrayFire's computer vision functions.

   This namespace provides high-level, user-friendly functions that wrap the
   lower-level ArrayFire computer vision API, following the same conventions
   as `org.soulspace.arrayfire.api.image-processing`.

   ## Feature Lifecycle

   Corner detectors (`detect-fast`, `detect-harris`, `detect-susan`) return raw
   feature handles (long integers). These must be released when no longer needed.
   Use the `with-features` macro for safe, scope-bound lifecycle management:

   ```clojure
   (core/with-arrayfire {:backend :opencl}
     (with-features [corners (detect-fast gray-img :threshold 20.0)]
       (features-count corners)))
   ```

   ## Feature Extraction

   `extract-orb`, `extract-sift`, and `extract-gloh` return maps:
   `{:features <handle> :descriptors <AFArray>}`.

   ## Feature Matching

   `match-binary-descriptors` and `match-float-descriptors` return maps:
   `{:indices <AFArray> :distances <AFArray>}`.

   ## All functions require a `with-arrayfire` region.

   It includes functions from the `org.soulspace.arrayfire.integration.unified-api.vision`
   and `org.soulspace.arrayfire.integration.unified-api.features` namespaces."
  (:require [org.soulspace.arrayfire.integration.unified-api.features :as features]
            [org.soulspace.arrayfire.integration.unified-api.vision :as vision]
            [org.soulspace.arrayfire.api.core :as core :refer [assert-within-arrayfire!]])
  (:import (org.soulspace.arrayfire.integration.base.resource AFArray)))

;;;
;;; Feature Lifecycle
;;;

(defmacro with-features
  "Execute body with a features handle, automatically releasing it afterward.

   Binds `sym` to the result of `features-expr` within the scope of body,
   then calls `release-features!` in a `finally` block to free the handle.

   Parameters:
   - binding: `[sym features-expr]` — binds sym to the features handle
   - body: forms evaluated with the binding in scope

   Returns:
   The result of the last body form.

   Example:
   ```clojure
   (core/with-arrayfire {:backend :opencl}
     (let [img (core/array ...)]
       (with-features [corners (detect-fast img)]
         {:count (features-count corners)
          :x     (features-xpos corners)})))
   ```"
  [[sym features-expr] & body]
  `(let [~sym ~features-expr]
     (try
       ~@body
       (finally
         (features/release-features! ~sym)))))

;;;
;;; Feature Detection — Corner Detectors
;;;

(defn detect-fast
  "Detect FAST (Features from Accelerated Segment Test) corners.

   Returns a features handle (long). Use `with-features` or call
   `features/release-features!` when done.

   Parameters:
   - img: Input grayscale image (AFArray)

   Keyword options:
   - `:threshold`     — Intensity threshold for corner detection (default 20.0).
                        Higher = fewer, stronger corners.
   - `:arc-length`    — Number of contiguous pixels required (default 9).
                        Valid values: 9, 11, 12, 16.
   - `:non-max?`      — Apply non-maximum suppression (default true).
   - `:feature-ratio` — Maximum fraction of pixels returned as features (default 0.05).
   - `:edge`          — Border width excluded from detection (default 3).

   Returns: Features handle (long). Release with `with-features` or `release-features!`.

   Example:
   ```clojure
   (core/with-arrayfire {:backend :opencl}
     (with-features [corners (detect-fast gray-img :threshold 15.0 :arc-length 9)]
       (features-count corners)))
   ```"
  [^AFArray img & {:keys [threshold arc-length non-max? feature-ratio edge]
                   :or {threshold 20.0 arc-length 9 non-max? true
                        feature-ratio 0.05 edge 3}}]
  (assert-within-arrayfire! "detect-fast")
  (vision/fast img threshold arc-length non-max? feature-ratio edge))

(defn detect-harris
  "Detect Harris corners.

   Returns a features handle (long). Use `with-features` or call
   `features/release-features!` when done.

   Parameters:
   - img: Input grayscale image (AFArray)

   Keyword options:
   - `:max-corners`    — Maximum number of corners to detect (default 500).
   - `:min-response`   — Minimum corner response threshold (default 1e6).
                         Higher = fewer, stronger corners.
   - `:sigma`          — Gaussian smoothing parameter (default 1.0).
   - `:block-size`     — Neighborhood size for gradient computation (default 3).
   - `:k`              — Harris sensitivity parameter (default 0.04). Range: [0.04, 0.06].

   Returns: Features handle (long). Release with `with-features` or `release-features!`.

   Example:
   ```clojure
   (core/with-arrayfire {:backend :opencl}
     (with-features [corners (detect-harris gray-img :max-corners 300 :min-response 1e7)]
       (features-count corners)))
   ```"
  [^AFArray img & {:keys [max-corners min-response sigma block-size k]
                   :or {max-corners 500 min-response 1e6 sigma 1.0 block-size 3 k 0.04}}]
  (assert-within-arrayfire! "detect-harris")
  (vision/harris img max-corners min-response sigma block-size k))

(defn detect-susan
  "Detect SUSAN (Smallest Univalue Segment Assimilating Nucleus) corners.

   SUSAN is robust to noise (no derivative computation required).

   Returns a features handle (long). Use `with-features` or call
   `features/release-features!` when done.

   Parameters:
   - img: Input grayscale image (AFArray)

   Keyword options:
   - `:radius`          — Circular mask radius in pixels (default 3). Must be < 10.
   - `:diff-threshold`  — Intensity difference threshold (default 20.0).
   - `:geom-threshold`  — Geometric threshold for corner response (default 14.0).
   - `:feature-ratio`   — Fraction of corners to retain (default 0.15).
   - `:edge`            — Border exclusion size (default 3, must be >= radius).

   Returns: Features handle (long). Release with `with-features` or `release-features!`.

   Example:
   ```clojure
   (core/with-arrayfire {:backend :opencl}
     (with-features [corners (detect-susan gray-img :radius 3 :diff-threshold 20.0)]
       (features-count corners)))
   ```"
  [^AFArray img & {:keys [radius diff-threshold geom-threshold feature-ratio edge]
                   :or {radius 3 diff-threshold 20.0 geom-threshold 14.0
                        feature-ratio 0.15 edge 3}}]
  (assert-within-arrayfire! "detect-susan")
  (vision/susan img radius diff-threshold geom-threshold feature-ratio edge))

;;;
;;; Feature Property Accessors
;;;

(defn features-count
  "Return the number of features in a features handle.

  Parameters:
  - features-handle: Features handle (long) returned by a detector.

  Returns: Long integer — number of detected features (>= 0).

  Example:
  ```clojure
  (core/with-arrayfire {:backend :opencl}
    (with-features [f (detect-fast img)]
      (features-count f)))
  ```"
  [features-handle]
  (assert-within-arrayfire! "features-count")
  (features/get-features-num features-handle))

(defn features-xpos
  "Return the X coordinate array from a features handle.

  The returned AFArray is a non-owning reference — do not release it separately.

  Parameters:
  - features-handle: Features handle (long).

  Returns: AFArray of float32 — X pixel coordinates for each feature.

  Example:
  ```clojure
  (core/with-arrayfire {:backend :opencl}
    (with-features [f (detect-fast img)]
      (core/->value (features-xpos f))))
  ```"
  [features-handle]
  (assert-within-arrayfire! "features-xpos")
  (features/get-features-xpos features-handle))

(defn features-ypos
  "Return the Y coordinate array from a features handle.

  The returned AFArray is a non-owning reference — do not release it separately.

  Parameters:
  - features-handle: Features handle (long).

  Returns: AFArray of float32 — Y pixel coordinates for each feature."
  [features-handle]
  (assert-within-arrayfire! "features-ypos")
  (features/get-features-ypos features-handle))

(defn features-score
  "Return the score/response array from a features handle.

  Score meaning is algorithm-specific: higher = stronger feature.

  Parameters:
  - features-handle: Features handle (long).

  Returns: AFArray of float32 — score for each feature."
  [features-handle]
  (assert-within-arrayfire! "features-score")
  (features/get-features-score features-handle))

(defn features-orientation
  "Return the orientation array from a features handle (in radians).

  FAST, Harris, and SUSAN return all zeros (no orientation).
  SIFT and ORB compute orientation.

  Parameters:
  - features-handle: Features handle (long).

  Returns: AFArray of float32 — orientation in radians for each feature."
  [features-handle]
  (assert-within-arrayfire! "features-orientation")
  (features/get-features-orientation features-handle))

(defn features-size
  "Return the size/scale array from a features handle (in pixels).

  FAST, Harris, and SUSAN return all 1.0 (single scale).
  SIFT and ORB compute scale.

  Parameters:
  - features-handle: Features handle (long).

  Returns: AFArray of float32 — size/scale for each feature."
  [features-handle]
  (assert-within-arrayfire! "features-size")
  (features/get-features-size features-handle))

;;;
;;; Feature Convenience
;;;

(defn features->map
  "Convert a features handle to a Clojure map of AFArray properties.

  Returns a map with:
  - `:count`       — number of features (long)
  - `:x`           — X coordinates (AFArray float32)
  - `:y`           — Y coordinates (AFArray float32)
  - `:score`       — feature scores (AFArray float32)
  - `:orientation` — orientations in radians (AFArray float32)
  - `:size`        — sizes/scales (AFArray float32)

  All AFArray values are non-owning references; do not release them separately.
  The features handle must remain valid while the returned arrays are in use.

  Parameters:
  - features-handle: Features handle (long).

  Returns: Map with keys `:count`, `:x`, `:y`, `:score`, `:orientation`, `:size`.

  Example:
  ```clojure
  (core/with-arrayfire {:backend :opencl}
    (with-features [f (detect-fast img)]
      (let [{:keys [count x y]} (features->map f)]
        {:n count
         :x-vals (core/->value x)
         :y-vals (core/->value y)})))
  ```"
  [features-handle]
  (assert-within-arrayfire! "features->map")
  {:count       (features/get-features-num features-handle)
   :x           (features/get-features-xpos features-handle)
   :y           (features/get-features-ypos features-handle)
   :score       (features/get-features-score features-handle)
   :orientation (features/get-features-orientation features-handle)
   :size        (features/get-features-size features-handle)})

;;;
;;; Feature Extraction — Detectors with Descriptors
;;;

(defn extract-orb
  "Extract ORB (Oriented FAST and Rotated BRIEF) features and descriptors.

   ORB is a fast binary descriptor algorithm (rotation invariant).
   Descriptors are 256-bit (32-byte uint8) per feature.

   Parameters:
   - img: Input grayscale image (AFArray)

   Keyword options:
   - `:fast-threshold` — FAST corner threshold (default 20.0). Lower = more features.
   - `:max-features`   — Maximum features to extract (default 400).
   - `:scale-factor`   — Image pyramid scale factor (default 1.5).
   - `:levels`         — Number of pyramid levels (default 4).
   - `:blur?`          — Apply Gaussian blur before detection (default true).

   Returns: Map with:
   - `:features`    — Features handle (long). Must be released (use `with-features`).
   - `:descriptors` — AFArray (N × 32 uint8) of binary descriptors.

   Example:
   ```clojure
   (core/with-arrayfire {:backend :opencl}
     (let [{:keys [features descriptors]} (extract-orb gray-img)]
       (with-features [f features]
         {:count (features-count f)
          :desc-shape (core/shape descriptors)})))
   ```"
  [^AFArray img & {:keys [fast-threshold max-features scale-factor levels blur?]
                   :or {fast-threshold 20.0 max-features 400
                        scale-factor 1.5 levels 4 blur? true}}]
  (assert-within-arrayfire! "extract-orb")
  (let [[feat-handle desc] (vision/orb img fast-threshold max-features
                                       scale-factor levels blur?)]
    {:features feat-handle :descriptors desc}))

(defn extract-sift
  "Extract SIFT (Scale-Invariant Feature Transform) features and descriptors.

   SIFT is the gold standard for feature extraction — scale and rotation invariant.
   Descriptors are 128-dimensional float32 per feature.

   Parameters:
   - img: Input grayscale image (AFArray)

   Keyword options:
   - `:n-layers`           — Layers per octave (default 3).
   - `:contrast-threshold` — Contrast threshold (default 0.04). Higher = fewer features.
   - `:edge-threshold`     — Edge response threshold (default 10.0).
   - `:init-sigma`         — Initial Gaussian smoothing (default 1.6).
   - `:double-input?`      — Upsample image 2× before processing (default false).
   - `:intensity-scale`    — Descriptor normalization (default 0.00390625 = 1/256).
   - `:feature-ratio`      — Max fraction of pixels as features (default 0.05).

   Returns: Map with:
   - `:features`    — Features handle (long). Must be released (use `with-features`).
   - `:descriptors` — AFArray (N × 128 float32) of SIFT descriptors.

   Example:
   ```clojure
   (core/with-arrayfire {:backend :opencl}
     (let [{:keys [features descriptors]} (extract-sift gray-img)]
       (with-features [f features]
         {:count (features-count f)
          :desc-cols (second (core/shape descriptors))})))
   ```"
  [^AFArray img & {:keys [n-layers contrast-threshold edge-threshold init-sigma
                           double-input? intensity-scale feature-ratio]
                   :or {n-layers 3 contrast-threshold 0.04 edge-threshold 10.0
                        init-sigma 1.6 double-input? false intensity-scale 0.00390625
                        feature-ratio 0.05}}]
  (assert-within-arrayfire! "extract-sift")
  (let [[feat-handle desc] (vision/sift img n-layers contrast-threshold edge-threshold
                                        init-sigma double-input? intensity-scale
                                        feature-ratio)]
    {:features feat-handle :descriptors desc}))

(defn extract-gloh
  "Extract GLOH (Gradient Location and Orientation Histogram) features and descriptors.

   GLOH extends SIFT using a log-polar spatial grid, producing more distinctive
   272-dimensional float32 descriptors.

   Parameters:
   - img: Input grayscale image (AFArray)

   Keyword options: Same as `extract-sift`.
   - `:n-layers`           — Layers per octave (default 3).
   - `:contrast-threshold` — Contrast threshold (default 0.04).
   - `:edge-threshold`     — Edge response threshold (default 10.0).
   - `:init-sigma`         — Initial Gaussian smoothing (default 1.6).
   - `:double-input?`      — Upsample image 2× before processing (default false).
   - `:intensity-scale`    — Descriptor normalization (default 0.00390625).
   - `:feature-ratio`      — Max fraction of pixels as features (default 0.05).

   Returns: Map with:
   - `:features`    — Features handle (long). Must be released (use `with-features`).
   - `:descriptors` — AFArray (N × 272 float32) of GLOH descriptors.

   Example:
   ```clojure
   (core/with-arrayfire {:backend :opencl}
     (let [{:keys [features descriptors]} (extract-gloh gray-img)]
       (with-features [f features]
         {:count (features-count f)})))
   ```"
  [^AFArray img & {:keys [n-layers contrast-threshold edge-threshold init-sigma
                           double-input? intensity-scale feature-ratio]
                   :or {n-layers 3 contrast-threshold 0.04 edge-threshold 10.0
                        init-sigma 1.6 double-input? false intensity-scale 0.00390625
                        feature-ratio 0.05}}]
  (assert-within-arrayfire! "extract-gloh")
  (let [[feat-handle desc] (vision/gloh img n-layers contrast-threshold edge-threshold
                                        init-sigma double-input? intensity-scale
                                        feature-ratio)]
    {:features feat-handle :descriptors desc}))

;;;
;;; Feature Matching
;;;

(defn match-binary-descriptors
  "Match binary descriptors using Hamming distance.

   Optimal for ORB binary descriptors (256-bit uint8 per feature).
   Uses XOR + popcount for extremely fast matching.

   Parameters:
   - query: Query descriptors (AFArray, Nq × D uint8)
   - train: Training descriptors (AFArray, Nt × D uint8)

   Keyword options:
   - `:dist-dim`    — Dimension along which to compute distances (default 0).
   - `:n-neighbors` — Number of nearest neighbors to return per query (default 1).
                      Use 2 for Lowe's ratio test.

   Returns: Map with:
   - `:indices`   — AFArray (Nq × n-neighbors int) of neighbor indices in `train`.
   - `:distances` — AFArray (Nq × n-neighbors int) of Hamming distances.

   Self-match distances are 0.

   Example:
   ```clojure
   (core/with-arrayfire {:backend :opencl}
     (let [{:keys [features descriptors]} (extract-orb img1)
           {:keys [features2 descriptors2]} (extract-orb img2)
           {:keys [indices distances]} (match-binary-descriptors descriptors descriptors2)]
       (core/shape indices)))
   ```"
  [^AFArray query ^AFArray train & {:keys [dist-dim n-neighbors]
                                     :or {dist-dim 0 n-neighbors 1}}]
  (assert-within-arrayfire! "match-binary-descriptors")
  (let [[idx dist] (vision/hamming-matcher query train dist-dim n-neighbors)]
    {:indices idx :distances dist}))

(defn match-float-descriptors
  "Match floating-point descriptors using nearest neighbor search.

   Optimal for SIFT and GLOH descriptors. Supports SSD and SAD distance metrics.

   Parameters:
   - query: Query descriptors (AFArray, Nq × D float32)
   - train: Training descriptors (AFArray, Nt × D float32)

   Keyword options:
   - `:dist-dim`    — Dimension along which to compute distances (default 0).
   - `:n-neighbors` — Number of nearest neighbors per query (default 1).
   - `:dist-type`   — Distance metric keyword (default `:ssd`):
                      - `:ssd` — Sum of Squared Differences
                      - `:sad` — Sum of Absolute Differences

   Returns: Map with:
   - `:indices`   — AFArray (Nq × n-neighbors int) of neighbor indices in `train`.
   - `:distances` — AFArray (Nq × n-neighbors float) of distances.

   Example:
   ```clojure
   (core/with-arrayfire {:backend :opencl}
     (let [{:keys [features descriptors]} (extract-sift img1)
           {:keys [features2 descriptors2]} (extract-sift img2)
           {:keys [indices distances]} (match-float-descriptors descriptors descriptors2
                                                                :n-neighbors 2 :dist-type :ssd)]
       ;; Apply Lowe's ratio test using distances
       ...))
   ```"
  [^AFArray query ^AFArray train & {:keys [dist-dim n-neighbors dist-type]
                                     :or {dist-dim 0 n-neighbors 1 dist-type :ssd}}]
  (assert-within-arrayfire! "match-float-descriptors")
  (let [[idx dist] (vision/nearest-neighbour query train dist-dim n-neighbors dist-type)]
    {:indices idx :distances dist}))

;;;
;;; Template Matching
;;;

(defn match-template
  "Find occurrences of a template image within a search image.

   Slides the template over the search image and computes a similarity or distance
   measure at each position. Returns a correlation map.

   Finding the best match location:
   - SAD, ZSAD, LSAD, SSD, ZSSD, LSSD: find the *minimum* of the result.
   - NCC, ZNCC: find the *maximum* of the result.

   Parameters:
   - search-img:   Large image to search (AFArray)
   - template-img: Small template to find (AFArray)

   Keyword options:
   - `:match-type` — Matching metric keyword (default `:sad`):
       - `:sad`   — Sum of Absolute Differences (fast)
       - `:zsad`  — Zero-mean SAD (brightness invariant)
       - `:lsad`  — Locally scaled SAD (contrast invariant)
       - `:ssd`   — Sum of Squared Differences
       - `:zssd`  — Zero-mean SSD
       - `:lssd`  — Locally scaled SSD
       - `:ncc`   — Normalized Cross-Correlation (robust)
       - `:zncc`  — Zero-mean NCC (most robust, slowest)

   Returns: AFArray correlation map with shape:
     `[(search-h - template-h + 1) (search-w - template-w + 1)]`

   Example:
   ```clojure
   (core/with-arrayfire {:backend :opencl}
     (let [scene    (ip/load-image \"scene.jpg\" :color? false)
           template (ip/load-image \"object.jpg\" :color? false)
           corr     (match-template scene template :match-type :zncc)]
       ;; Maximum of corr = best match location
       (core/shape corr)))
   ```"
  [^AFArray search-img ^AFArray template-img & {:keys [match-type]
                                                  :or {match-type :sad}}]
  (assert-within-arrayfire! "match-template")
  (vision/match-template search-img template-img match-type))

;;;
;;; Image Processing for Vision
;;;

(defn difference-of-gaussians
  "Compute the Difference of Gaussians (DOG) of an image.

   DOG approximates the Laplacian of Gaussian and is used for blob detection
   and scale-space construction (e.g., within SIFT).

   DOG(x,y) = Gaussian(x,y,σ₁) - Gaussian(x,y,σ₂)  where σ₁ < σ₂

   Parameters:
   - img: Input image (AFArray)

   Keyword options:
   - `:radius1` — Radius for the first (smaller) Gaussian (default 3).
   - `:radius2` — Radius for the second (larger) Gaussian (default 6).

   Returns: AFArray of same shape as `img` containing the DOG response.

   Example:
   ```clojure
   (core/with-arrayfire {:backend :opencl}
     (let [img (ip/load-image \"scene.jpg\" :color? false)
           dog (difference-of-gaussians img :radius1 3 :radius2 6)]
       (core/shape dog)))
   ```"
  [^AFArray img & {:keys [radius1 radius2] :or {radius1 3 radius2 6}}]
  (assert-within-arrayfire! "difference-of-gaussians")
  (vision/dog img radius1 radius2))

;;;
;;; Geometric Estimation
;;;

(defn estimate-homography
  "Estimate a homography transformation between two sets of corresponding points.

   A homography H is a 3×3 matrix mapping points from one plane to another,
   used for image stitching, augmented reality, and perspective correction.

   RANSAC automatically handles outlier correspondences.
   Requires at least 4 corresponding point pairs.

   Parameters:
   - x-src: Source X coordinates (AFArray float32, N elements)
   - y-src: Source Y coordinates (AFArray float32, N elements)
   - x-dst: Destination X coordinates (AFArray float32, N elements)
   - y-dst: Destination Y coordinates (AFArray float32, N elements)

   Keyword options:
   - `:method`           — Estimation algorithm (default `:ransac`):
                           - `:ransac` — Random Sample Consensus (robust to outliers)
                           - `:lmeds`  — Least Median of Squares (<50% outliers)
   - `:inlier-threshold` — RANSAC inlier distance threshold in pixels (default 3.0).
                           Larger = more inliers, less precise.
   - `:iterations`       — RANSAC iterations (default 1000).
                           More = better with many outliers, slower.

   Returns: Map with:
   - `:homography` — AFArray (3×3 float32) — the estimated homography matrix H.
   - `:inliers`    — Integer — number of inlier point pairs used.

   Example:
   ```clojure
   (core/with-arrayfire {:backend :opencl}
     (let [xs (core/array (float-array [10 20 30 40]) [4] :f32)
           ys (core/array (float-array [10 20 30 40]) [4] :f32)
           {:keys [homography inliers]} (estimate-homography xs ys xs ys)]
       {:h-shape (core/shape homography) :inliers inliers}))
   ```"
  [x-src y-src x-dst y-dst & {:keys [method inlier-threshold iterations]
                                :or {method :ransac inlier-threshold 3.0
                                     iterations 1000}}]
  (assert-within-arrayfire! "estimate-homography")
  (let [[H n-inliers] (vision/homography x-src y-src x-dst y-dst
                                         method inlier-threshold iterations)]
    {:homography H :inliers n-inliers}))

(comment
  ;; Computer Vision REPL experiments
  ;; All examples must be called inside (core/with-arrayfire ...).
  ;;
  ;; Backend: most vision functions require {:backend :opencl}.
  ;;
  ;; Load namespaces:
  ;;   (require '[org.soulspace.arrayfire.api.core :as af])
  ;;   (require '[org.soulspace.arrayfire.api.computer-vision :as cv])
  ;;   (require '[org.soulspace.arrayfire.api.image-processing :as ip])

  ;; --- FAST corner detection ---
  (core/with-arrayfire {:backend :opencl}
    (let [img (core/constant 0.5 [10 10] :f32)]
      (with-features [f (detect-fast img :threshold 10.0)]
        (features-count f))))

  ;; --- Harris corner detection ---
  (core/with-arrayfire {:backend :opencl}
    (let [img (core/constant 0.5 [10 10] :f32)]
      (with-features [f (detect-harris img :max-corners 100)]
        {:count (features-count f)})))

  ;; --- SUSAN corner detection ---
  (core/with-arrayfire {:backend :opencl}
    (let [img (core/constant 0.5 [10 10] :f32)]
      (with-features [f (detect-susan img :radius 3 :diff-threshold 20.0)]
        (features-count f))))

  ;; --- features->map ---
  (core/with-arrayfire {:backend :opencl}
    (let [img (core/constant 0.5 [10 10] :f32)]
      (with-features [f (detect-fast img :threshold 5.0)]
        (let [{:keys [count]} (features->map f)]
          count))))

  ;; --- ORB extraction ---
  (core/with-arrayfire {:backend :opencl}
    (let [img (core/constant 0.5 [64 64] :f32)
          {:keys [features descriptors]} (extract-orb img)]
      (with-features [f features]
        {:count       (features-count f)
         :desc-shape  (core/shape descriptors)})))

  ;; --- SIFT extraction ---
  (core/with-arrayfire {:backend :opencl}
    (let [img (core/constant 0.5 [64 64] :f32)
          {:keys [features descriptors]} (extract-sift img)]
      (with-features [f features]
        {:count (features-count f)
         :desc-cols (second (core/shape descriptors))})))

  ;; --- Binary descriptor matching (ORB self-match) ---
  (core/with-arrayfire {:backend :opencl}
    (let [img (core/constant 0.5 [64 64] :f32)
          {:keys [features descriptors]} (extract-orb img)
          {:keys [indices distances]} (match-binary-descriptors descriptors descriptors)]
      (with-features [f features]
        {:n   (features-count f)
         :idx (core/shape indices)})))

  ;; --- Template matching ---
  (core/with-arrayfire {:backend :opencl}
    (let [scene    (core/constant 0.5 [10 10] :f32)
          template (core/constant 0.5 [3 3] :f32)
          corr     (match-template scene template :match-type :zncc)]
      (core/shape corr)))

  ;; --- Difference of Gaussians ---
  (core/with-arrayfire {:backend :opencl}
    (let [img (core/constant 0.5 [10 10] :f32)
          dog (difference-of-gaussians img :radius1 2 :radius2 4)]
      (core/shape dog)))

  ;; --- Homography estimation ---
  (core/with-arrayfire {:backend :opencl}
    (let [xs (core/array (float-array [10 20 30 40]) [4] :f32)
          ys (core/array (float-array [10 20 30 40]) [4] :f32)
          {:keys [homography inliers]} (estimate-homography xs ys xs ys)]
      {:h-shape (core/shape homography) :inliers inliers})))

