(ns org.soulspace.arrayfire.api.statistics-test
  "Comprehensive tests for the idiomatic Clojure statistics API.

   Tests are grouped by function and cover:
   - Correct scalar results for known inputs
   - Dimension-reduction shape and value correctness
   - Bias (:sample vs :population) for variance / stdev
   - Weighted variants
   - mean-and-variance single-pass consistency
   - Covariance and Pearson correlation
   - top-k selection (max and min order)
   - Guard: IllegalStateException when called outside a region
   - Validation: ExceptionInfo for unknown bias / order keywords

   All computation is done on the :cpu backend so no GPU is required."
  (:require [clojure.test :refer [deftest is testing  run-test run-tests]]
            [org.soulspace.arrayfire.util.test      :as util]
            [org.soulspace.arrayfire.api.core       :as af]
            [org.soulspace.arrayfire.api.statistics :as stat]))

;;;
;;; Guard tests — must fail outside a region
;;;

(deftest mean-all-requires-region-test
  (testing "mean-all throws IllegalStateException when called outside with-arrayfire"
    (let [arr (af/with-arrayfire {:backend :cpu}
                (af/array [1.0 2.0 3.0] [3] :f64))]
      (is (thrown? IllegalStateException (stat/mean-all arr))))))

(deftest variance-all-requires-region-test
  (testing "variance-all throws IllegalStateException when called outside with-arrayfire"
    (let [arr (af/with-arrayfire {:backend :cpu}
                (af/array [1.0 2.0 3.0] [3] :f64))]
      (is (thrown? IllegalStateException (stat/variance-all arr))))))

;;;
;;; Validation tests — unknown keywords
;;;

(deftest unknown-bias-keyword-throws-test
  (testing "variance-all throws ExceptionInfo for an unknown bias keyword"
    (is (thrown? clojure.lang.ExceptionInfo
          (af/with-arrayfire {:backend :cpu}
            (let [arr (af/array [1.0 2.0 3.0] [3] :f64)]
              (stat/variance-all arr :something-invalid)))))))

(deftest unknown-topk-order-throws-test
  (testing "topk throws ExceptionInfo for an unknown order keyword"
    (is (thrown? clojure.lang.ExceptionInfo
          (af/with-arrayfire {:backend :cpu}
            (let [arr (af/array [1.0 2.0 3.0 4.0 5.0] [5] :f64)]
              (stat/topk arr 2 -1 :bad-order)))))))

;;;
;;; mean
;;;

(deftest mean-all-scalar-test
  (testing "mean of [1 2 3 4 5] is 3.0"
    (let [result (af/with-arrayfire {:backend :cpu}
                   (stat/mean-all (af/array [1.0 2.0 3.0 4.0 5.0] [5] :f64)))]
      (is (util/approx= 3.0 result)))))

(deftest mean-dim0-test
  (testing "column-wise mean of a 3x2 matrix (dim 0)"
    ;; Col-major layout for [[1 4][2 5][3 6]]: stored as [1 2 3 4 5 6]
    ;; Col 0 mean = (1+2+3)/3 = 2.0, Col 1 mean = (4+5+6)/3 = 5.0
    (let [result (af/with-arrayfire {:backend :cpu}
                   (af/->value
                     (stat/mean
                       (af/array [1.0 2.0 3.0 4.0 5.0 6.0] [3 2] :f64)
                       0)))]
      (is (util/seq-approx= [2.0 5.0] (flatten result))))))

(deftest mean-dim1-test
  (testing "row-wise mean of a 3x2 matrix (dim 1)"
    ;; Row 0: (1+4)/2=2.5, Row 1: (2+5)/2=3.5, Row 2: (3+6)/2=4.5
    (let [result (af/with-arrayfire {:backend :cpu}
                   (af/->value
                     (stat/mean
                       (af/array [1.0 2.0 3.0 4.0 5.0 6.0] [3 2] :f64)
                       1)))]
      (is (util/seq-approx= [2.5 3.5 4.5] (flatten result))))))

(deftest mean-all-weighted-test
  (testing "weighted mean with single non-zero weight returns that element's value"
    (let [result (af/with-arrayfire {:backend :cpu}
                   (stat/mean-all-weighted
                     (af/array [1.0 2.0 3.0 4.0] [4] :f64)
                     (af/array [0.0 0.0 0.0 1.0] [4] :f64)))]
      (is (util/approx= 4.0 result))))
  (testing "heavier weight pulls the mean toward that element"
    (let [[low high] (af/with-arrayfire {:backend :cpu}
                       (let [data   (af/array [1.0 100.0] [2] :f64)
                             w-low  (af/array [10.0 1.0] [2] :f64)
                             w-high (af/array [1.0 10.0] [2] :f64)]
                         [(stat/mean-all-weighted data w-low)
                          (stat/mean-all-weighted data w-high)]))]
      (is (< low high)))))

(deftest mean-weighted-no-dim-test
  (testing "weighted mean with zero weights excludes elements"
    (let [result (af/with-arrayfire {:backend :cpu}
                   (let [data    (af/array [2.0 4.0 6.0] [3] :f64)
                         weights (af/array [0.0 1.0 0.0] [3] :f64)]
                     (stat/mean-all-weighted data weights)))]
      (is (util/approx= 4.0 result)))))

;;;
;;; variance
;;;

(deftest variance-all-sample-test
  (testing "sample variance of [1 2 3 4 5] = 2.5 (Bessel corrected)"
    ;; s^2 = [(1-3)^2 + (2-3)^2 + ... + (5-3)^2] / 4 = 10/4 = 2.5
    (let [result (af/with-arrayfire {:backend :cpu}
                   (stat/variance-all (af/array [1.0 2.0 3.0 4.0 5.0] [5] :f64)))]
      (is (util/approx= 2.5 result)))))

(deftest variance-all-sample-explicit-test
  (testing "explicit :sample bias yields the same result as default"
    (let [result (af/with-arrayfire {:backend :cpu}
                   (stat/variance-all (af/array [1.0 2.0 3.0 4.0 5.0] [5] :f64) :sample))]
      (is (util/approx= 2.5 result)))))

(deftest variance-all-population-test
  (testing "population variance of [1 2 3 4 5] = 2.0"
    ;; sigma^2 = [(-2)^2+(-1)^2+0^2+1^2+2^2] / 5 = 10/5 = 2.0
    (let [result (af/with-arrayfire {:backend :cpu}
                   (stat/variance-all (af/array [1.0 2.0 3.0 4.0 5.0] [5] :f64) :population))]
      (is (util/approx= 2.0 result)))))

(deftest variance-all-constant-test
  (testing "variance of a constant array is 0.0"
    (let [result (af/with-arrayfire {:backend :cpu}
                   (stat/variance-all (af/constant 7.0 [5] :f64)))]
      (is (util/approx= 0.0 result)))))

(deftest variance-dim0-shape-test
  (testing "variance along dim 0 reduces first dimension of a 4x3 matrix to [1 3]"
    (let [result (af/with-arrayfire {:backend :cpu}
                   (af/shape
                     (stat/variance (af/random-normal [4 3] :f64) :sample 0)))]
      (is (= [1 3] result)))))

(deftest variance-all-weighted-test
  (testing "weighted variance of constant array is 0.0"
    (let [result (af/with-arrayfire {:backend :cpu}
                   (stat/variance-all-weighted
                     (af/constant 5.0 [5] :f64)
                     (af/ones [5] :f64)))]
      (is (util/approx= 0.0 result))))
  (testing "weighted variance of non-constant array is positive"
    (let [result (af/with-arrayfire {:backend :cpu}
                   (stat/variance-all-weighted
                     (af/array [1.0 2.0 3.0] [3] :f64)
                     (af/ones [3] :f64)))]
      (is (pos? result)))))

;;;
;;; stdev
;;;

(deftest stdev-all-sample-test
  (testing "sample stdev of [1 2 3 4 5] = sqrt(2.5)"
    (let [result (af/with-arrayfire {:backend :cpu}
                   (stat/stdev-all (af/array [1.0 2.0 3.0 4.0 5.0] [5] :f64)))]
      (is (util/approx= (Math/sqrt 2.5) result)))))

(deftest stdev-all-population-test
  (testing "population stdev of [1 2 3 4 5] = sqrt(2.0)"
    (let [result (af/with-arrayfire {:backend :cpu}
                   (stat/stdev-all (af/array [1.0 2.0 3.0 4.0 5.0] [5] :f64) :population))]
      (is (util/approx= (Math/sqrt 2.0) result)))))

(deftest stdev-all-constant-test
  (testing "stdev of a constant array is 0.0"
    (let [result (af/with-arrayfire {:backend :cpu}
                   (stat/stdev-all (af/constant 42.0 [10] :f64)))]
      (is (util/approx= 0.0 result)))))

(deftest stdev-dim0-shape-test
  (testing "stdev along dim 0 of a 6x4 array produces shape [1 4]"
    (let [result (af/with-arrayfire {:backend :cpu}
                   (af/shape
                     (stat/stdev (af/random-normal [6 4] :f64) :sample 0)))]
      (is (= [1 4] result)))))

(deftest stdev-squared-equals-variance-test
  (testing "stdev squared equals sample variance"
    (let [[s v] (af/with-arrayfire {:backend :cpu}
                  (let [arr (af/array [1.0 2.0 3.0 4.0 5.0] [5] :f64)]
                    [(stat/stdev-all arr)
                     (stat/variance-all arr)]))]
      (is (util/approx= (* s s) v)))))

;;;
;;; median
;;;

(deftest median-all-odd-test
  (testing "median of [5 1 3 2 4] (unsorted) = 3.0"
    (let [result (af/with-arrayfire {:backend :cpu}
                   (stat/median-all (af/array [5.0 1.0 3.0 2.0 4.0] [5] :f64)))]
      (is (util/approx= 3.0 result)))))

(deftest median-all-even-test
  (testing "median of [1 2 3 4] = 2.5"
    (let [result (af/with-arrayfire {:backend :cpu}
                   (stat/median-all (af/array [1.0 2.0 3.0 4.0] [4] :f64)))]
      (is (util/approx= 2.5 result)))))

(deftest median-robust-to-outlier-test
  (testing "median of [1 2 3 4 1000] = 3.0 (unaffected by outlier)"
    (let [result (af/with-arrayfire {:backend :cpu}
                   (stat/median-all (af/array [1.0 2.0 3.0 4.0 1000.0] [5] :f64)))]
      (is (util/approx= 3.0 result)))))

(deftest median-vs-mean-outlier-test
  (testing "median is much smaller than mean when an outlier is present"
    (let [[med avg] (af/with-arrayfire {:backend :cpu}
                      (let [v (af/array [1.0 2.0 3.0 4.0 1000.0] [5] :f64)]
                        [(stat/median-all v)
                         (stat/mean-all v)]))]
      (is (< med 10.0))   ; median = 3.0
      (is (> avg 100.0))  ; mean = 202.0
      (is (< med avg)))))

(deftest median-dim0-shape-test
  (testing "median along dim 0 of a 5x3 array produces shape [1 3]"
    (let [result (af/with-arrayfire {:backend :cpu}
                   (af/shape
                     (stat/median (af/random-normal [5 3] :f64) 0)))]
      (is (= [1 3] result)))))

;;;
;;; mean-and-variance
;;;

(deftest mean-and-variance-returns-map-test
  (testing "mean-and-variance returns a map with :mean and :var keys"
    (let [result (af/with-arrayfire {:backend :cpu}
                   (let [v (af/array [1.0 2.0 3.0 4.0 5.0] [5] :f64)
                         w (af/ones  [5] :f64)]
                     (keys (stat/mean-and-variance v w))))]
      (is (= #{:mean :var} (set result))))))

(deftest mean-and-variance-values-test
  (testing "mean-and-variance mean and var agree with individual functions"
    ;; [1 2 3 4 5]: mu=3.0, s^2=2.5
    (let [[mv-mean mv-var indep-mean indep-var]
          (af/with-arrayfire {:backend :cpu}
            (let [v (af/array [1.0 2.0 3.0 4.0 5.0] [5] :f64)
                  w (af/ones  [5] :f64)
                  {:keys [mean var]} (stat/mean-and-variance v w :sample)]
              [(af/->value mean)         ; scalar from the mean result
               (af/->value var)          ; scalar from the var result
               (stat/mean-all v)
               (stat/variance-all v)]))]
      (is (util/approx= mv-mean indep-mean))
      (is (util/approx= mv-var indep-var)))))

;;;
;;; covariance
;;;

(deftest covariance-positive-test
  (testing "covariance is positive for x=[1 2 3 4], y=[2 4 6 8]"
    (let [result (af/with-arrayfire {:backend :cpu}
                   (let [x (af/array [1.0 2.0 3.0 4.0] [4] :f64)
                         y (af/array [2.0 4.0 6.0 8.0] [4] :f64)]
                     (af/->value (stat/covariance x y))))]
      (is (pos? result)))))

(deftest covariance-negative-test
  (testing "covariance is negative for inversely related x and y"
    (let [result (af/with-arrayfire {:backend :cpu}
                   (let [x (af/array [1.0 2.0 3.0 4.0] [4] :f64)
                         y (af/array [8.0 6.0 4.0 2.0] [4] :f64)]
                     (af/->value (stat/covariance x y))))]
      (is (neg? result)))))

(deftest covariance-sample-value-test
  (testing "sample covariance of [1 2 3] and [2 4 6] = 2.0"
    ;; Cov = [(1-2)(2-4)+(2-2)(4-4)+(3-2)(6-4)] / (3-1) = (2+0+2)/2 = 2.0
    (let [result (af/with-arrayfire {:backend :cpu}
                   (let [x (af/array [1.0 2.0 3.0] [3] :f64)
                         y (af/array [2.0 4.0 6.0] [3] :f64)]
                     (af/->value (stat/covariance x y :sample))))]
      (is (util/approx= 2.0 result)))))

;;;
;;; correlation
;;;

(deftest correlation-perfect-positive-test
  (testing "correlation is approximately 1.0 for perfectly correlated variables"
    (let [result (af/with-arrayfire {:backend :cpu}
                   (let [x (af/array [1.0 2.0 3.0 4.0] [4] :f64)
                         y (af/array [2.0 4.0 6.0 8.0] [4] :f64)]
                     (stat/correlation x y)))]
      (is (util/approx= 1.0 result 1e-5)))))

(deftest correlation-perfect-negative-test
  (testing "correlation is approximately -1.0 for perfectly anti-correlated variables"
    (let [result (af/with-arrayfire {:backend :cpu}
                   (let [x (af/array [1.0 2.0 3.0 4.0] [4] :f64)
                         y (af/array [4.0 3.0 2.0 1.0] [4] :f64)]
                     (stat/correlation x y)))]
      (is (util/approx= -1.0 result 1e-5)))))

(deftest correlation-self-test
  (testing "correlation of a variable with itself is 1.0"
    (let [result (af/with-arrayfire {:backend :cpu}
                   (let [x (af/array [3.0 1.0 4.0 1.0 5.0 9.0] [6] :f64)]
                     (stat/correlation x x)))]
      (is (util/approx= 1.0 result 1e-5)))))

(deftest correlation-bounded-test
  (testing "correlation of two independent random arrays is bounded in [-1, 1]"
    (let [result (af/with-arrayfire {:backend :cpu}
                   (stat/correlation (af/random-normal [100] :f64)
                                     (af/random-normal [100] :f64)))]
      (is (<= -1.0 result 1.0)))))

;;;
;;; topk
;;;

(deftest topk-max-values-test
  (testing "topk max-3 of [3 1 4 1 5 9] returns three largest values"
    (let [result (af/with-arrayfire {:backend :cpu}
                   (let [v           (af/array [3.0 1.0 4.0 1.0 5.0 9.0] [6] :f64)
                         [vals _idx] (stat/topk v 3)]
                     (af/->value vals)))]
      (is (= 3 (count result)))
      (is (util/approx= 9.0 (apply max result))))))

(deftest topk-min-values-test
  (testing "topk min-3 of [3 1 4 1 5 9] returns three smallest values"
    (let [result (af/with-arrayfire {:backend :cpu}
                   (let [v           (af/array [3.0 1.0 4.0 1.0 5.0 9.0] [6] :f64)
                         [vals _idx] (stat/topk v 3 -1 :min)]
                     (af/->value vals)))]
      (is (= 3 (count result)))
      (is (util/approx= 1.0 (apply min result))))))

(deftest topk-max-returns-correct-index-test
  (testing "topk max-1 of [3 1 4 1 5 9] returns index 5 (position of 9)"
    ;; k=1 returns a scalar-shaped AFArray; ->value yields a raw integer
    (let [result (af/with-arrayfire {:backend :cpu}
                   (let [v           (af/array [3.0 1.0 4.0 1.0 5.0 9.0] [6] :f64)
                         [_vals idx] (stat/topk v 1)]
                     (af/->value idx)))]
      (is (= 5 (int result))))))

(deftest topk-sum-consistency-test
  (testing "sum of top-3 values of [3 1 4 1 5 9] = 9+5+4 = 18.0"
    (let [result (af/with-arrayfire {:backend :cpu}
                   (let [v           (af/array [3.0 1.0 4.0 1.0 5.0 9.0] [6] :f64)
                         [vals _idx] (stat/topk v 3)]
                     (reduce + (af/->value vals))))]
      (is (util/approx= 18.0 result)))))

(deftest topk-k-equals-length-test
  (testing "topk with k = length returns all elements"
    (let [result (af/with-arrayfire {:backend :cpu}
                   (let [v           (af/array [3.0 1.0 4.0 1.0 5.0 9.0] [6] :f64)
                         [vals _idx] (stat/topk v 6)]
                     (count (af/->value vals))))]
      (is (= 6 result)))))

(deftest topk-dim0-shape-test
  (testing "topk along dim 0 of a 6x1 array returns shape [3]"
    ;; ArrayFire collapses trailing singleton dimensions
    (let [result (af/with-arrayfire {:backend :cpu}
                   (let [v           (af/array [3.0 1.0 4.0 1.0 5.0 9.0] [6 1] :f64)
                         [vals _idx] (stat/topk v 3 0)]
                     (af/shape vals)))]
      (is (= [3] result)))))

(comment
  ;; Run all statistics tests interactively
  (run-tests)

  ;; run-test for all the specific tests from above
  (run-test mean-all-requires-region-test)
  (run-test variance-all-requires-region-test)
  (run-test unknown-bias-keyword-throws-test)
  (run-test unknown-topk-order-throws-test)
  (run-test mean-all-scalar-test)
  (run-test mean-dim0-test)
  (run-test mean-dim1-test)
  (run-test mean-all-weighted-test)
  (run-test mean-weighted-no-dim-test)
  (run-test variance-all-sample-test)
  (run-test variance-all-sample-explicit-test)
  (run-test variance-all-population-test)
  (run-test variance-all-constant-test)
  (run-test variance-dim0-shape-test)
  (run-test variance-all-weighted-test)
  (run-test stdev-all-sample-test)
  (run-test stdev-all-population-test)
  (run-test stdev-all-constant-test)
  (run-test stdev-dim0-shape-test)
  (run-test stdev-squared-equals-variance-test)
  (run-test median-all-odd-test)
  (run-test median-all-even-test)
  (run-test median-robust-to-outlier-test)
  (run-test median-vs-mean-outlier-test)
  (run-test median-dim0-shape-test)
  (run-test mean-and-variance-returns-map-test)
  (run-test mean-and-variance-values-test)
  (run-test covariance-positive-test)
  (run-test covariance-negative-test)
  (run-test covariance-sample-value-test)
  (run-test correlation-perfect-positive-test)
  (run-test correlation-perfect-negative-test)
  (run-test correlation-self-test)
  (run-test correlation-bounded-test)
  (run-test topk-max-values-test)
  (run-test topk-min-values-test)
  (run-test topk-max-returns-correct-index-test)
  (run-test topk-sum-consistency-test)
  (run-test topk-k-equals-length-test)
  (run-test topk-dim0-shape-test)
  
  ;
  )
