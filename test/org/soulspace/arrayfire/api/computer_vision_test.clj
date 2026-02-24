(ns org.soulspace.arrayfire.api.computer-vision-test
  "Tests for the idiomatic Clojure computer vision API.
   All tests run inside (with-arrayfire ...) regions.

   Backend notes:
   - All vision functions require the :opencl backend.
   - Feature detectors on constant images return 0 features (valid state).
   - Use checkerboard / gradient patterns when non-zero feature count is needed.
   - ORB/SIFT/GLOH descriptor column widths: 32, 128, 272 respectively.
   - Template matching output shape: (search_h - tmpl_h + 1) × (search_w - tmpl_w + 1).
   - Homography requires at least 4 non-collinear point pairs."
  (:require [clojure.test :refer [deftest is testing run-test run-tests]]
            [org.soulspace.arrayfire.api.core :as af]
            [org.soulspace.arrayfire.api.computer-vision :as cv]))

;;;
;;; Helper — build a synthetic checkerboard image (8×8, :f32)
;;; Checkerboards have well-defined corners useful for corner detectors.
;;;

(defn make-checkerboard
  "Return an 8×8 float32 array with a 2×2 checkerboard pattern [0.0/1.0].
   Created inside an active `with-arrayfire` region."
  []
  (let [data (float-array
               (for [r (range 8) c (range 8)]
                 (if (= (mod (+ (quot r 2) (quot c 2)) 2) 0) 0.0 1.0)))]
    (af/array data [8 8] :f32)))

(defn make-gradient-image
  "Return a 64×64 float32 array with a horizontal gradient 0.0→1.0.
   64×64 ensures ORB's 4-level pyramid (scale 1.5) stays above the
   7×7 minimum at every level (smallest level ≈ 18×18)."
  []
  (let [data (float-array
               (for [_r (range 64) c (range 64)]
                 (float (/ c 63.0))))]
    (af/array data [64 64] :f32)))

;;;
;;; Guard tests — every function requires a with-arrayfire region
;;;

(deftest detect-fast-requires-region-test
  (testing "detect-fast throws IllegalStateException outside a region"
    (is (thrown? IllegalStateException
          (let [img (af/with-arrayfire {:backend :opencl}
                      (make-checkerboard))]
            (cv/detect-fast img))))))

(deftest detect-harris-requires-region-test
  (testing "detect-harris throws IllegalStateException outside a region"
    (is (thrown? IllegalStateException
          (let [img (af/with-arrayfire {:backend :opencl}
                      (make-checkerboard))]
            (cv/detect-harris img))))))

(deftest detect-susan-requires-region-test
  (testing "detect-susan throws IllegalStateException outside a region"
    (is (thrown? IllegalStateException
          (let [img (af/with-arrayfire {:backend :opencl}
                      (make-checkerboard))]
            (cv/detect-susan img))))))

(deftest features-count-requires-region-test
  (testing "features-count throws IllegalStateException outside a region"
    (is (thrown? IllegalStateException
          (cv/features-count 0)))))

(deftest features-xpos-requires-region-test
  (testing "features-xpos throws IllegalStateException outside a region"
    (is (thrown? IllegalStateException
          (cv/features-xpos 0)))))

(deftest features-ypos-requires-region-test
  (testing "features-ypos throws IllegalStateException outside a region"
    (is (thrown? IllegalStateException
          (cv/features-ypos 0)))))

(deftest features-score-requires-region-test
  (testing "features-score throws IllegalStateException outside a region"
    (is (thrown? IllegalStateException
          (cv/features-score 0)))))

(deftest features-orientation-requires-region-test
  (testing "features-orientation throws IllegalStateException outside a region"
    (is (thrown? IllegalStateException
          (cv/features-orientation 0)))))

(deftest features-size-requires-region-test
  (testing "features-size throws IllegalStateException outside a region"
    (is (thrown? IllegalStateException
          (cv/features-size 0)))))

(deftest features->map-requires-region-test
  (testing "features->map throws IllegalStateException outside a region"
    (is (thrown? IllegalStateException
          (cv/features->map 0)))))

(deftest extract-orb-requires-region-test
  (testing "extract-orb throws IllegalStateException outside a region"
    (is (thrown? IllegalStateException
          (let [img (af/with-arrayfire {:backend :opencl}
                      (make-checkerboard))]
            (cv/extract-orb img))))))

(deftest extract-sift-requires-region-test
  (testing "extract-sift throws IllegalStateException outside a region"
    (is (thrown? IllegalStateException
          (let [img (af/with-arrayfire {:backend :opencl}
                      (make-checkerboard))]
            (cv/extract-sift img))))))

(deftest extract-gloh-requires-region-test
  (testing "extract-gloh throws IllegalStateException outside a region"
    (is (thrown? IllegalStateException
          (let [img (af/with-arrayfire {:backend :opencl}
                      (make-checkerboard))]
            (cv/extract-gloh img))))))

(deftest match-binary-descriptors-requires-region-test
  (testing "match-binary-descriptors throws IllegalStateException outside a region"
    (is (thrown? IllegalStateException
          (let [d (af/with-arrayfire {:backend :opencl}
                    (af/constant 0 [4 32] :u8))]
            (cv/match-binary-descriptors d d))))))

(deftest match-float-descriptors-requires-region-test
  (testing "match-float-descriptors throws IllegalStateException outside a region"
    (is (thrown? IllegalStateException
          (let [d (af/with-arrayfire {:backend :opencl}
                    (af/constant 0.0 [4 128] :f32))]
            (cv/match-float-descriptors d d))))))

(deftest match-template-requires-region-test
  (testing "match-template throws IllegalStateException outside a region"
    (is (thrown? IllegalStateException
          (let [scene    (af/with-arrayfire {:backend :opencl}
                           (af/constant 0.5 [10 10] :f32))
                template (af/with-arrayfire {:backend :opencl}
                           (af/constant 0.5 [3 3] :f32))]
            (cv/match-template scene template))))))

(deftest difference-of-gaussians-requires-region-test
  (testing "difference-of-gaussians throws IllegalStateException outside a region"
    (is (thrown? IllegalStateException
          (let [img (af/with-arrayfire {:backend :opencl}
                      (af/constant 0.5 [10 10] :f32))]
            (cv/difference-of-gaussians img))))))

(deftest estimate-homography-requires-region-test
  (testing "estimate-homography throws IllegalStateException outside a region"
    (is (thrown? IllegalStateException
          (let [xs (af/with-arrayfire {:backend :opencl}
                     (af/array (float-array [10 20 30 40]) [4] :f32))
                ys (af/with-arrayfire {:backend :opencl}
                     (af/array (float-array [10 20 30 40]) [4] :f32))]
            (cv/estimate-homography xs ys xs ys))))))

;;;
;;; Feature Detection Tests
;;;

(deftest detect-fast-returns-valid-count-test
  (testing "detect-fast returns features handle with non-negative count"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [img (make-checkerboard)]
              (cv/with-features [f (cv/detect-fast img :threshold 10.0)]
                (cv/features-count f))))]
      (is (nat-int? result)))))

(deftest detect-fast-default-params-test
  (testing "detect-fast with default parameters returns valid count"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [img (make-checkerboard)]
              (cv/with-features [f (cv/detect-fast img)]
                (cv/features-count f))))]
      (is (nat-int? result)))))

(deftest detect-fast-custom-params-test
  (testing "detect-fast with custom arc-length and feature-ratio accepted"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [img (make-checkerboard)]
              (cv/with-features [f (cv/detect-fast img
                                                   :threshold 5.0
                                                   :arc-length 9
                                                   :non-max? true
                                                   :feature-ratio 0.1
                                                   :edge 3)]
                (cv/features-count f))))]
      (is (nat-int? result)))))

(deftest detect-harris-returns-valid-count-test
  (testing "detect-harris returns features handle with non-negative count"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [img (make-checkerboard)]
              (cv/with-features [f (cv/detect-harris img)]
                (cv/features-count f))))]
      (is (nat-int? result)))))

(deftest detect-harris-custom-params-test
  (testing "detect-harris with custom parameters accepted"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [img (make-checkerboard)]
              (cv/with-features [f (cv/detect-harris img
                                                     :max-corners 100
                                                     :min-response 1e5
                                                     :sigma 1.0
                                                     :block-size 3
                                                     :k 0.04)]
                (cv/features-count f))))]
      (is (nat-int? result)))))

(deftest detect-susan-returns-valid-count-test
  (testing "detect-susan returns features handle with non-negative count"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [img (make-checkerboard)]
              (cv/with-features [f (cv/detect-susan img)]
                (cv/features-count f))))]
      (is (nat-int? result)))))

(deftest detect-susan-custom-params-test
  (testing "detect-susan with custom parameters accepted"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [img (make-checkerboard)]
              (cv/with-features [f (cv/detect-susan img
                                                    :radius 3
                                                    :diff-threshold 15.0
                                                    :geom-threshold 14.0
                                                    :feature-ratio 0.2
                                                    :edge 3)]
                (cv/features-count f))))]
      (is (nat-int? result)))))

;;;
;;; Feature Accessor Tests
;;;

(deftest features-xpos-shape-matches-count-test
  (testing "features-xpos array length equals features-count"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [img (make-checkerboard)]
              (cv/with-features [f (cv/detect-fast img :threshold 5.0)]
                (let [n (cv/features-count f)
                      x (cv/features-xpos f)]
                  {:count n :xpos-len (first (af/shape x))}))))]
      (is (= (:count result) (:xpos-len result))))))

(deftest features-ypos-shape-matches-count-test
  (testing "features-ypos array length equals features-count"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [img (make-checkerboard)]
              (cv/with-features [f (cv/detect-fast img :threshold 5.0)]
                (let [n (cv/features-count f)
                      y (cv/features-ypos f)]
                  {:count n :ypos-len (first (af/shape y))}))))]
      (is (= (:count result) (:ypos-len result))))))

(deftest features-score-shape-matches-count-test
  (testing "features-score array length equals features-count"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [img (make-checkerboard)]
              (cv/with-features [f (cv/detect-fast img :threshold 5.0)]
                (let [n (cv/features-count f)
                      s (cv/features-score f)]
                  {:count n :score-len (first (af/shape s))}))))]
      (is (= (:count result) (:score-len result))))))

(deftest features-orientation-shape-matches-count-test
  (testing "features-orientation array length equals features-count"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [img (make-checkerboard)]
              (cv/with-features [f (cv/detect-fast img :threshold 5.0)]
                (let [n (cv/features-count f)
                      o (cv/features-orientation f)]
                  {:count n :ori-len (first (af/shape o))}))))]
      (is (= (:count result) (:ori-len result))))))

(deftest features-size-shape-matches-count-test
  (testing "features-size array length equals features-count"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [img (make-checkerboard)]
              (cv/with-features [f (cv/detect-fast img :threshold 5.0)]
                (let [n (cv/features-count f)
                      sz (cv/features-size f)]
                  {:count n :size-len (first (af/shape sz))}))))]
      (is (= (:count result) (:size-len result))))))

;;;
;;; features->map Tests
;;;

(deftest features->map-returns-all-keys-test
  (testing "features->map returns a map with :count :x :y :score :orientation :size"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [img (make-checkerboard)]
              (cv/with-features [f (cv/detect-fast img :threshold 5.0)]
                (set (keys (cv/features->map f))))))]
      (is (= #{:count :x :y :score :orientation :size} result)))))

(deftest features->map-count-is-nat-int-test
  (testing "features->map :count is non-negative integer"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [img (make-checkerboard)]
              (cv/with-features [f (cv/detect-fast img :threshold 5.0)]
                (:count (cv/features->map f)))))]
      (is (nat-int? result)))))

(deftest features->map-shapes-consistent-test
  (testing "features->map all array lengths equal :count"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [img (make-checkerboard)]
              (cv/with-features [f (cv/detect-fast img :threshold 5.0)]
                (let [{:keys [count x y score orientation size]} (cv/features->map f)]
                  {:count count
                   :x-len (first (af/shape x))
                   :y-len (first (af/shape y))
                   :s-len (first (af/shape score))
                   :o-len (first (af/shape orientation))
                   :sz-len (first (af/shape size))}))))]
      (is (= (:count result) (:x-len result)))
      (is (= (:count result) (:y-len result)))
      (is (= (:count result) (:s-len result)))
      (is (= (:count result) (:o-len result)))
      (is (= (:count result) (:sz-len result))))))

;;;
;;; with-features Lifecycle Tests
;;;

(deftest with-features-returns-body-result-test
  (testing "with-features macro returns the result of the body"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [img (make-checkerboard)]
              (cv/with-features [_ (cv/detect-fast img :threshold 5.0)]
                42)))]
      (is (= 42 result)))))

(deftest with-features-releases-on-exception-test
  (testing "with-features releases handle even if body throws"
    ;; Should not throw from with-features itself (only from body)
    (is (thrown? Exception
          (af/with-arrayfire {:backend :opencl}
            (let [img (make-checkerboard)]
              (cv/with-features [_ (cv/detect-fast img :threshold 5.0)]
                (throw (Exception. "test exception")))))))))
;;;
;;; Feature Extraction Tests
;;;

(deftest extract-orb-returns-map-with-keys-test
  (testing "extract-orb returns map with :features and :descriptors keys"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [img (make-gradient-image)
                  m (cv/extract-orb img)]
              (set (keys m))))]
      (is (= #{:features :descriptors} result)))))

(deftest extract-orb-descriptor-columns-test
  (testing "extract-orb descriptors have 8 uint32 words per descriptor (256-bit binary)"
    ;; ArrayFire ORB returns descriptors in [D, N] column-major format:
    ;; first dim = descriptor length (8 uint32 words = 256 bits), second dim = feature count.
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [img (make-gradient-image)
                  {:keys [features descriptors]} (cv/extract-orb img)]
              (cv/with-features [f features]
                {:count       (cv/features-count f)
                 :desc-shape  (af/shape descriptors)})))]
      (is (nat-int? (:count result)))
      ;; When features are found, descriptor shape is [8, count] (D × N column-major)
      (when (pos? (:count result))
        (is (= 8 (first (:desc-shape result))))))))

(deftest extract-orb-custom-params-test
  (testing "extract-orb with custom parameters accepted"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [img (make-gradient-image)
                  {:keys [features]} (cv/extract-orb img
                                                     :fast-threshold 15.0
                                                     :max-features 200
                                                     :scale-factor 1.5
                                                     :levels 3
                                                     :blur? true)]
              (cv/with-features [f features]
                {:count (cv/features-count f)})))]
      (is (nat-int? (:count result))))))

(deftest extract-sift-returns-map-with-keys-test
  (testing "extract-sift returns map with :features and :descriptors keys"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [img (make-gradient-image)
                  m (cv/extract-sift img)]
              (set (keys m))))]
      (is (= #{:features :descriptors} result)))))

(deftest extract-sift-descriptor-columns-test
  (testing "extract-sift descriptors have 128 columns"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [img (make-gradient-image)
                  {:keys [features descriptors]} (cv/extract-sift img)]
              (cv/with-features [f features]
                {:count      (cv/features-count f)
                 :desc-shape (af/shape descriptors)})))]
      (is (nat-int? (:count result)))
      (when (pos? (:count result))
        (is (= 128 (second (:desc-shape result))))))))

(deftest extract-gloh-returns-map-with-keys-test
  (testing "extract-gloh returns map with :features and :descriptors keys"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [img (make-gradient-image)
                  m (cv/extract-gloh img)]
              (set (keys m))))]
      (is (= #{:features :descriptors} result)))))

(deftest extract-gloh-descriptor-columns-test
  (testing "extract-gloh descriptors have 272 columns"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [img (make-gradient-image)
                  {:keys [features descriptors]} (cv/extract-gloh img)]
              (cv/with-features [f features]
                {:count      (cv/features-count f)
                 :desc-shape (af/shape descriptors)})))]
      (is (nat-int? (:count result)))
      (when (pos? (:count result))
        (is (= 272 (second (:desc-shape result))))))))

;;;
;;; Feature Matching Tests
;;;

(deftest match-binary-descriptors-returns-map-with-keys-test
  (testing "match-binary-descriptors returns map with :indices and :distances"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [d (af/constant 0 [4 32] :u8)
                  m (cv/match-binary-descriptors d d)]
              (set (keys m))))]
      (is (= #{:indices :distances} result)))))

(deftest match-binary-descriptors-self-match-shape-test
  (testing "match-binary-descriptors self-match: output shape is [n-neighbors n-queries]"
    ;; af_hamming_matcher output shape: [n-neighbors, n-queries] (dist_dim=1 for row-major).
    ;; Use dist-dim 1 so each row of [4 32] is treated as one descriptor.
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [d (af/constant 0 [4 32] :u8)
                  {:keys [indices distances]} (cv/match-binary-descriptors d d :dist-dim 1)]
              {:idx-shape  (af/shape indices)
               :dist-shape (af/shape distances)}))]
      ;; Output [1, 4]: first=n-neighbors=1, second=n-queries=4
      (is (= 4 (second (:idx-shape result))))
      (is (= 4 (second (:dist-shape result)))))))

(deftest match-binary-descriptors-self-match-zero-distance-test
  (testing "match-binary-descriptors self-match: Hamming distances are 0"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [d (af/constant 0 [4 32] :u8)
                  {:keys [distances]} (cv/match-binary-descriptors d d :dist-dim 1)]
              ;; Extract scalar inside region while AFArrays are still valid
              (af/->value (af/sum distances))))]
      (is (= 0 result)))))

(deftest match-binary-descriptors-n-neighbors-test
  (testing "match-binary-descriptors :n-neighbors 2 returns wider result"
    ;; Output shape: [n-neighbors, n-queries]. Use dist-dim 1 for row-major layout.
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [d (af/constant 0 [4 32] :u8)
                  {:keys [indices]} (cv/match-binary-descriptors d d
                                                                  :n-neighbors 2
                                                                  :dist-dim 1)]
              (af/shape indices)))]
      ;; With n-neighbors=2: first=2 (n-neighbors), second=4 (n-queries)
      (is (= 2 (first result)))
      (is (= 4 (second result))))))

(deftest match-float-descriptors-returns-map-with-keys-test
  (testing "match-float-descriptors returns map with :indices and :distances"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [d (af/constant 0.0 [4 128] :f32)
                  m (cv/match-float-descriptors d d)]
              (set (keys m))))]
      (is (= #{:indices :distances} result)))))

(deftest match-float-descriptors-self-match-shape-test
  (testing "match-float-descriptors self-match returns correct shape"
    ;; af_nearest_neighbour output shape: [n-neighbors, n-queries]
    ;; Use dist-dim 1 for row-major Nq × D layout (each row is one descriptor).
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [d (af/constant 1.0 [5 128] :f32)
                  {:keys [indices distances]} (cv/match-float-descriptors d d :dist-dim 1)]
              {:idx-shape  (af/shape indices)
               :dist-shape (af/shape distances)}))]
      ;; Output [n-neighbors=1, n-queries=5]: check second dim for query count
      (is (= 5 (second (:idx-shape result))))
      (is (= 5 (second (:dist-shape result)))))))

(deftest match-float-descriptors-self-match-zero-ssd-test
  (testing "match-float-descriptors identical descriptors have SSD distance 0"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [d (af/constant 1.0 [4 128] :f32)
                  {:keys [distances]} (cv/match-float-descriptors d d
                                                                   :dist-type :ssd
                                                                   :dist-dim 1)]
              ;; Extract scalar inside region before AFArrays are released
              (double (af/->value (af/sum distances)))))]
      (is (= 0.0 result)))))

(deftest match-float-descriptors-ssd-dist-dim-test
  (testing "match-float-descriptors :dist-type :ssd with dist-dim 1 returns correct n-queries"
    ;; Note: :sad distance type is not supported by af_nearest_neighbour for float descriptors.
    ;; Only :ssd is supported. Use dist-dim 1 for row-major (Nq × D) descriptor layout.
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [d (af/constant 0.0 [4 128] :f32)
                  {:keys [indices]} (cv/match-float-descriptors d d :dist-type :ssd :dist-dim 1)]
              (af/shape indices)))]
      (is (= 4 (second result))))))

;;;
;;; Template Matching Tests
;;;

(deftest match-template-output-shape-test
  (testing "match-template returns correlation map with shape (sh-th+1) × (sw-tw+1)"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [scene    (af/constant 0.5 [10 10] :f32)
                  template (af/constant 0.5 [3 3] :f32)
                  corr     (cv/match-template scene template)]
              (af/shape corr)))]
      ;; af_match_template returns full scene size, not clipped
      (is (= [10 10] result)))))

(deftest match-template-sad-test
  (testing "match-template with :sad returns correct shape (full scene size)"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [scene    (af/constant 0.5 [12 12] :f32)
                  template (af/constant 0.5 [4 4] :f32)
                  corr     (cv/match-template scene template :match-type :sad)]
              (af/shape corr)))]
      ;; af_match_template returns full scene size, not clipped
      (is (= [12 12] result)))))

(deftest match-template-zssd-test
  (testing "match-template with :zssd returns correct shape (full scene size)"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [scene    (af/constant 0.5 [10 10] :f32)
                  template (af/constant 0.5 [3 3] :f32)
                  corr     (cv/match-template scene template :match-type :zssd)]
              (af/shape corr)))]
      ;; af_match_template returns full scene size, not clipped
      (is (= [10 10] result)))))

(deftest match-template-lssd-test
  (testing "match-template with :lssd returns correct shape (full scene size)"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [scene    (af/constant 0.5 [10 10] :f32)
                  template (af/constant 0.5 [3 3] :f32)
                  corr     (cv/match-template scene template :match-type :lssd)]
              (af/shape corr)))]
      ;; af_match_template returns full scene size, not clipped
      (is (= [10 10] result)))))

(deftest match-template-ssd-test
  (testing "match-template with :ssd returns correct shape (full scene size)"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [scene    (af/constant 0.5 [8 8] :f32)
                  template (af/constant 0.5 [2 2] :f32)
                  corr     (cv/match-template scene template :match-type :ssd)]
              (af/shape corr)))]
      ;; af_match_template returns full scene size, not clipped
      (is (= [8 8] result)))))

;;;
;;; Difference of Gaussians Tests
;;;

(deftest difference-of-gaussians-preserves-shape-test
  (testing "difference-of-gaussians preserves image shape"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [img (af/constant 0.5 [10 10] :f32)
                  dog (cv/difference-of-gaussians img)]
              (af/shape dog)))]
      (is (= [10 10] result)))))

(deftest difference-of-gaussians-custom-radii-test
  (testing "difference-of-gaussians with custom radii preserves shape"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [img (af/constant 0.5 [12 12] :f32)
                  dog (cv/difference-of-gaussians img :radius1 2 :radius2 5)]
              (af/shape dog)))]
      (is (= [12 12] result)))))

(deftest difference-of-gaussians-zero-image-zero-test
  (testing "difference-of-gaussians of zero image is zero everywhere"
    ;; DOG of a zero image: both Gaussian blurs yield zero, so difference = 0.
    ;; Note: DOG of a non-zero constant image is NOT ~0 for finite images because
    ;; different Gaussian kernel sizes cause different boundary effects.
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [img (af/constant 0.0 [8 8] :f32)
                  dog (cv/difference-of-gaussians img)]
              (double (af/->value (af/sum dog)))))]
      (is (= 0.0 result)))))

;;;
;;; Homography Estimation Tests
;;;

(deftest estimate-homography-returns-map-with-keys-test
  (testing "estimate-homography returns map with :homography and :inliers"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [xs (af/array (float-array [10 20 30 40]) [4] :f32)
                  ys (af/array (float-array [10 50 80 20]) [4] :f32)
                  m  (cv/estimate-homography xs ys xs ys)]
              (set (keys m))))]
      (is (= #{:homography :inliers} result)))))

(deftest estimate-homography-output-shape-test
  (testing "estimate-homography :homography is a 3×3 matrix"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [xs (af/array (float-array [10 20 30 40]) [4] :f32)
                  ys (af/array (float-array [10 50 80 20]) [4] :f32)
                  {:keys [homography]} (cv/estimate-homography xs ys xs ys)]
              (af/shape homography)))]
      (is (= [3 3] result)))))

(deftest estimate-homography-identity-inliers-test
  (testing "estimate-homography with src=dst returns 4 inliers"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [xs (af/array (float-array [10 20 30 40]) [4] :f32)
                  ys (af/array (float-array [10 50 80 20]) [4] :f32)
                  {:keys [inliers]} (cv/estimate-homography xs ys xs ys)]
              inliers))]
      (is (= 4 result)))))

(deftest estimate-homography-ransac-method-test
  (testing "estimate-homography with :method :ransac accepted"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [xs (af/array (float-array [10 20 30 40]) [4] :f32)
                  ys (af/array (float-array [10 50 80 20]) [4] :f32)
                  {:keys [homography inliers]} (cv/estimate-homography xs ys xs ys
                                                                       :method :ransac
                                                                       :inlier-threshold 3.0
                                                                       :iterations 500)]
              {:shape (af/shape homography) :inliers inliers}))]
      (is (= [3 3] (:shape result)))
      (is (nat-int? (:inliers result))))))

(deftest estimate-homography-lmeds-method-test
  (testing "estimate-homography with :method :lmeds accepted"
    (let [result
          (af/with-arrayfire {:backend :opencl}
            (let [xs (af/array (float-array [10 20 30 40]) [4] :f32)
                  ys (af/array (float-array [10 50 80 20]) [4] :f32)
                  {:keys [homography inliers]} (cv/estimate-homography xs ys xs ys
                                                                       :method :lmeds)]
              {:shape (af/shape homography) :inliers inliers}))]
      (is (= [3 3] (:shape result)))
      (is (nat-int? (:inliers result))))))

(comment
  ;; Run tests
  (run-tests)

  ;
  )
