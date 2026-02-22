(ns org.soulspace.arrayfire.api.machine-learning-test
  "Tests for the idiomatic Clojure machine-learning API.
   All tests run inside (with-arrayfire ...) regions.

   Backend notes:
   - CNN convolution (convolve2-nn, convolve2-gradient) requires the filter's
     3rd dimension (input channels) to match the signal's 3rd dimension:
     signal [h w in_ch batch], filter [kh kw in_ch out_ch].
   - Use :f64 arrays (:f32 ->value returns zeros on some backends).
   - Gradient shape assertions are the primary correctness signal."
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [org.soulspace.arrayfire.api.core :as af]
            [org.soulspace.arrayfire.api.signal-processing :as sp]
            [org.soulspace.arrayfire.api.machine-learning :as ml])
  (:import (org.soulspace.arrayfire.integration.base.resource AFArray)))

;;;
;;; Helpers
;;;

(defn- make-ones-array
  "Create an array of all 1.0 values with the given shape."
  [shape]
  (af/array (vec (repeat (reduce * shape) 1.0)) shape :f64))

(defn- nn-forward-setup
  "Run a small CNN forward pass and return {:signal :filter :output :out-shape}.
   Dimensions chosen so input channels match filter channels:
   signal [2 2 2 1] (h=2 w=2 in_ch=2 batch=1),
   filter [2 2 2 1] (kh=2 kw=2 in_ch=2 out_filters=1)."
  []
  (let [signal (af/array [1.0 0.0 0.0 1.0
                           0.0 1.0 1.0 0.0] [2 2 2 1] :f64)
        filt   (af/array [1.0 0.0 0.0 1.0
                           0.0 1.0 1.0 0.0] [2 2 2 1] :f64)
        output (sp/convolve2-nn signal filt [1 1] [1 1] [1 1])]
    {:signal signal :filter filt :output output
     :out-shape (af/shape output)}))

;;;
;;; Guard tests — functions require with-arrayfire region
;;; (Use simple dummy 4D arrays; no convolve2-nn call needed to test the guard.)
;;;

(defn- make-dummy-4d
  "Create a dummy :f64 AFArray inside a with-arrayfire region.
   Used only to produce valid AFArray objects for guard tests."
  [shape]
  (af/with-arrayfire
    (af/array (vec (repeat (reduce * shape) 1.0)) shape :f64)))

(deftest convolve2-gradient-requires-region-test
  (testing "convolve2-gradient throws IllegalStateException outside a region"
    (is (thrown? IllegalStateException
          (let [s (make-dummy-4d [2 2 2 1])
                f (make-dummy-4d [2 2 2 1])
                o (make-dummy-4d [3 3])
                g (make-dummy-4d [3 3])]
            (ml/convolve2-gradient g s f o [1 1] [1 1] [1 1] :filter))))))

(deftest filter-gradient-requires-region-test
  (testing "filter-gradient throws IllegalStateException outside a region"
    (is (thrown? IllegalStateException
          (let [s (make-dummy-4d [2 2 2 1])
                f (make-dummy-4d [2 2 2 1])
                o (make-dummy-4d [3 3])
                g (make-dummy-4d [3 3])]
            (ml/filter-gradient g s f o [1 1] [1 1] [1 1]))))))

(deftest data-gradient-requires-region-test
  (testing "data-gradient throws IllegalStateException outside a region"
    (is (thrown? IllegalStateException
          (let [s (make-dummy-4d [2 2 2 1])
                f (make-dummy-4d [2 2 2 1])
                o (make-dummy-4d [3 3])
                g (make-dummy-4d [3 3])]
            (ml/data-gradient g s f o [1 1] [1 1] [1 1]))))))

(deftest bias-gradient-requires-region-test
  (testing "bias-gradient throws IllegalStateException outside a region"
    (is (thrown? IllegalStateException
          (let [s (make-dummy-4d [2 2 2 1])
                f (make-dummy-4d [2 2 2 1])
                o (make-dummy-4d [3 3])
                g (make-dummy-4d [3 3])]
            (ml/bias-gradient g s f o [1 1] [1 1] [1 1]))))))

(deftest all-gradients-requires-region-test
  (testing "all-gradients throws IllegalStateException outside a region"
    (is (thrown? IllegalStateException
          (let [s (make-dummy-4d [2 2 2 1])
                f (make-dummy-4d [2 2 2 1])
                o (make-dummy-4d [3 3])
                g (make-dummy-4d [3 3])]
            (ml/all-gradients g s f o [1 1] [1 1] [1 1]))))))

;;;
;;; convolve2-gradient — parametric gradient type dispatch
;;;

(deftest convolve2-gradient-filter-type-test
  (testing "convolve2-gradient with :filter produces an AFArray"
    (let [result (af/with-arrayfire
                   (let [{:keys [signal filter output out-shape]} (nn-forward-setup)
                         grad  (make-ones-array out-shape)
                         dW    (ml/convolve2-gradient grad signal filter output
                                                      [1 1] [1 1] [1 1] :filter)]
                     (instance? AFArray dW)))]
      (is (true? result)))))

(deftest convolve2-gradient-data-type-test
  (testing "convolve2-gradient with :data produces an AFArray"
    (let [result (af/with-arrayfire
                   (let [{:keys [signal filter output out-shape]} (nn-forward-setup)
                         grad  (make-ones-array out-shape)
                         dX    (ml/convolve2-gradient grad signal filter output
                                                      [1 1] [1 1] [1 1] :data)]
                     (instance? AFArray dX)))]
      (is (true? result)))))

(deftest convolve2-gradient-bias-type-test
  (testing "convolve2-gradient with :bias produces an AFArray"
    (let [result (af/with-arrayfire
                   (let [signal (af/array [1.0 0.0 0.0 1.0
                                           0.0 1.0 1.0 0.0] [2 2 2 1] :f64)
                         filt   (af/array [1.0 0.0 0.0 1.0
                                           0.0 1.0 1.0 0.0
                                           1.0 0.0 0.0 1.0
                                           0.0 1.0 1.0 0.0] [2 2 2 2] :f64)
                         output (sp/convolve2-nn signal filt [1 1] [1 1] [1 1])
                         grad   (make-ones-array (af/shape output))
                         db     (ml/convolve2-gradient grad signal filt output
                                                       [1 1] [1 1] [1 1] :bias)]
                     (instance? AFArray db)))]
      (is (true? result)))))

(deftest convolve2-gradient-default-type-test
  (testing "convolve2-gradient with :default is same as :filter — produces AFArray"
    (let [result (af/with-arrayfire
                   (let [{:keys [signal filter output out-shape]} (nn-forward-setup)
                         grad  (make-ones-array out-shape)
                         dW    (ml/convolve2-gradient grad signal filter output
                                                      [1 1] [1 1] [1 1] :default)]
                     (instance? AFArray dW)))]
      (is (true? result)))))

;;;
;;; filter-gradient — shape and instance checks
;;;

(deftest filter-gradient-returns-afarray-test
  (testing "filter-gradient returns an AFArray"
    (let [result (af/with-arrayfire
                   (let [{:keys [signal filter output out-shape]} (nn-forward-setup)
                         grad (make-ones-array out-shape)]
                     (instance? AFArray (ml/filter-gradient grad signal filter output
                                                            [1 1] [1 1] [1 1]))))]
      (is (true? result)))))

(deftest filter-gradient-shape-test
  (testing "filter-gradient first two spatial dims match kernel size"
    ;; filter is [kh=2 kw=2 in_ch=2 out_filters=1]; the result spatial dims [kh kw] = [2 2]
    (let [kh-kw (af/with-arrayfire
                  (let [{:keys [signal filter output out-shape]} (nn-forward-setup)
                        grad (make-ones-array out-shape)
                        dW   (ml/filter-gradient grad signal filter output
                                                 [1 1] [1 1] [1 1])]
                    (take 2 (af/shape dW))))]
      (is (= [2 2] (vec kh-kw))))))

;;;
;;; data-gradient — instance check
;;;

(deftest data-gradient-returns-afarray-test
  (testing "data-gradient returns an AFArray"
    (let [result (af/with-arrayfire
                   (let [{:keys [signal filter output out-shape]} (nn-forward-setup)
                         grad (make-ones-array out-shape)]
                     (instance? AFArray (ml/data-gradient grad signal filter output
                                                          [1 1] [1 1] [1 1]))))]
      (is (true? result)))))

;;;
;;; bias-gradient — instance and basic shape check
;;;

(deftest bias-gradient-returns-afarray-test
  (testing "bias-gradient returns an AFArray"
    (let [result (af/with-arrayfire
                   (let [signal (af/array [1.0 0.0 0.0 1.0
                                           0.0 1.0 1.0 0.0] [2 2 2 1] :f64)
                         filt   (af/array [1.0 0.0 0.0 1.0
                                           0.0 1.0 1.0 0.0
                                           1.0 0.0 0.0 1.0
                                           0.0 1.0 1.0 0.0] [2 2 2 2] :f64)
                         output (sp/convolve2-nn signal filt [1 1] [1 1] [1 1])
                         grad   (make-ones-array (af/shape output))
                         db     (ml/bias-gradient grad signal filt output
                                                  [1 1] [1 1] [1 1])]
                     (instance? AFArray db)))]
      (is (true? result)))))

(deftest bias-gradient-has-dimensions-test
  (testing "bias-gradient result has at least 1 dimension"
    (let [ndims (af/with-arrayfire
                  (let [signal (af/array [1.0 0.0 0.0 1.0
                                          0.0 1.0 1.0 0.0] [2 2 2 1] :f64)
                        filt   (af/array [1.0 0.0 0.0 1.0
                                          0.0 1.0 1.0 0.0
                                          1.0 0.0 0.0 1.0
                                          0.0 1.0 1.0 0.0] [2 2 2 2] :f64)
                        output (sp/convolve2-nn signal filt [1 1] [1 1] [1 1])
                        grad   (make-ones-array (af/shape output))
                        db     (ml/bias-gradient grad signal filt output
                                                 [1 1] [1 1] [1 1])]
                    (count (af/shape db))))]
      (is (pos? ndims)))))

;;;
;;; all-gradients — map keys and value types
;;;

(deftest all-gradients-returns-map-test
  (testing "all-gradients returns a map"
    (let [result (af/with-arrayfire
                   (let [{:keys [signal filter output out-shape]} (nn-forward-setup)
                         grad (make-ones-array out-shape)]
                     (map? (ml/all-gradients grad signal filter output
                                             [1 1] [1 1] [1 1]))))]
      (is (true? result)))))

(deftest all-gradients-has-required-keys-test
  (testing "all-gradients map contains :filter :data :bias keys"
    (let [result-keys (af/with-arrayfire
                        (let [{:keys [signal filter output out-shape]} (nn-forward-setup)
                              grad (make-ones-array out-shape)
                              grads (ml/all-gradients grad signal filter output
                                                      [1 1] [1 1] [1 1])]
                          (set (keys grads))))]
      (is (= #{:filter :data :bias} result-keys)))))

(deftest all-gradients-values-are-afarrays-test
  (testing "all-gradients values are all AFArray instances"
    (let [result (af/with-arrayfire
                   (let [{:keys [signal filter output out-shape]} (nn-forward-setup)
                         grad (make-ones-array out-shape)
                         grads (ml/all-gradients grad signal filter output
                                                 [1 1] [1 1] [1 1])]
                     (every? #(instance? AFArray %) (vals grads))))]
      (is (true? result)))))

;;;
;;; Stride / padding / dilation variants
;;;

(deftest filter-gradient-stride2-test
  (testing "filter-gradient respects stride-2 forward pass"
    (let [result (af/with-arrayfire
                   (let [signal (af/array (vec (map double (range 16)))
                                          [4 4 1 1] :f64)
                         filt   (af/array [1.0 1.0 1.0 1.0] [2 2 1 1] :f64)
                         output (sp/convolve2-nn signal filt [2 2] [1 1] [1 1])
                         grad   (make-ones-array (af/shape output))
                         dW     (ml/filter-gradient grad signal filt output
                                                    [2 2] [1 1] [1 1])]
                     (instance? AFArray dW)))]
      (is (true? result)))))

(deftest data-gradient-larger-padding-test
  (testing "data-gradient handles extra padding"
    (let [result (af/with-arrayfire
                   (let [signal (af/array [1.0 0.0 0.0 1.0] [2 2 1 1] :f64)
                         filt   (af/array [1.0 0.0 0.0 1.0] [2 2 1 1] :f64)
                         output (sp/convolve2-nn signal filt [1 1] [2 2] [1 1])
                         grad   (make-ones-array (af/shape output))
                         dX     (ml/data-gradient grad signal filt output
                                                  [1 1] [2 2] [1 1])]
                     (instance? AFArray dX)))]
      (is (true? result)))))

(comment
  ;; ---------------------------------------------------------------------------
  ;; Run all tests from the REPL
  ;; ---------------------------------------------------------------------------
  (require '[org.soulspace.arrayfire.api.machine-learning-test] :reload)
  (run-tests 'org.soulspace.arrayfire.api.machine-learning-test)
  )
