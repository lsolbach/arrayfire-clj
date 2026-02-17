(ns org.soulspace.arrayfire.integration.unified-api.statistics-test
  (:require [clojure.test :refer [deftest is testing run-test run-tests]]
            [org.soulspace.arrayfire.ffi.base.definitions :as defs]
            [org.soulspace.arrayfire.integration.base.resource :as res]
            [org.soulspace.arrayfire.integration.unified-api.array :as array]
            [org.soulspace.arrayfire.integration.unified-api.data :as data]
            [org.soulspace.arrayfire.integration.unified-api.device :as device]
            [org.soulspace.arrayfire.integration.unified-api.statistics :as stats]
            [tech.v3.resource :refer [releasing!]])
  (:import [org.soulspace.arrayfire.integration.base.resource AFArray]))

;;;
;;; Mean (Central Tendency) Tests
;;;

(deftest test-mean
  (testing "mean computes arithmetic mean along default dimension"
    (device/init!)
    (releasing!
      (let [data (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0 6.0]) [2 3] defs/AF_DTYPE_F32)
            result (stats/mean data)]
        (is (instance? AFArray result))
        (is (= [1 3 1 1] (array/get-dims result)))))))

(deftest test-mean-with-dimension
  (testing "mean computes along specified dimension"
    (device/init!)
    (releasing!
      (let [data (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0 6.0]) [2 3] defs/AF_DTYPE_F32)
            result-dim0 (stats/mean data 0)
            result-dim1 (stats/mean data 1)]
        (is (instance? AFArray result-dim0))
        (is (instance? AFArray result-dim1))
        (is (= [1 3 1 1] (array/get-dims result-dim0)))
        (is (= [2 1 1 1] (array/get-dims result-dim1)))))))

(deftest test-mean-weighted
  (testing "mean-weighted computes weighted mean"
    (device/init!)
    (releasing!
      (let [values (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4] defs/AF_DTYPE_F32)
            weights (array/create-array (float-array [0.1 0.2 0.3 0.4]) [4] defs/AF_DTYPE_F32)
            result (stats/mean-weighted values weights)]
        (is (instance? AFArray result))))))

(deftest test-mean-weighted-with-dimension
  (testing "mean-weighted computes along specified dimension"
    (device/init!)
    (releasing!
      (let [values (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0 6.0]) [2 3] defs/AF_DTYPE_F32)
            weights (array/create-array (float-array [0.4 0.6]) [2] defs/AF_DTYPE_F32)
            result (stats/mean-weighted values weights 0)]
        (is (instance? AFArray result))))))

(deftest test-mean-all
  (testing "mean-all computes scalar mean of all elements"
    (device/init!)
    (releasing!
      (let [data (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4] defs/AF_DTYPE_F32)
            result (stats/mean-all data)]
        (is (number? result))
        (is (= 2.5 result))))))

(deftest test-mean-all-weighted
  (testing "mean-all-weighted computes weighted scalar mean"
    (device/init!)
    (releasing!
      (let [values (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4] defs/AF_DTYPE_F32)
            weights (array/create-array (float-array [0.1 0.2 0.3 0.4]) [4] defs/AF_DTYPE_F32)
            result (stats/mean-all-weighted values weights)]
        (is (number? result))))))

;;;
;;; Variance (Dispersion) Tests
;;;

(deftest test-var
  (testing "var computes variance with default bias"
    (device/init!)
    (releasing!
      (let [data (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0 6.0]) [2 3] defs/AF_DTYPE_F32)
            result (stats/var data)]
        (is (instance? AFArray result))))))

(deftest test-var-with-bias
  (testing "var computes variance with specified bias"
    (device/init!)
    (releasing!
      (let [data (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4] defs/AF_DTYPE_F32)
            result-sample (stats/var data defs/AF_VARIANCE_SAMPLE)
            result-pop (stats/var data defs/AF_VARIANCE_POPULATION)]
        (is (instance? AFArray result-sample))
        (is (instance? AFArray result-pop))))))

(deftest test-var-with-dimension
  (testing "var computes variance along specified dimension"
    (device/init!)
    (releasing!
      (let [data (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0 6.0]) [2 3] defs/AF_DTYPE_F32)
            result (stats/var data defs/AF_VARIANCE_DEFAULT 1)]
        (is (instance? AFArray result))
        (is (= [2 1 1 1] (array/get-dims result)))))))

(deftest test-var-weighted
  (testing "var-weighted computes weighted variance"
    (device/init!)
    (releasing!
      (let [values (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4] defs/AF_DTYPE_F32)
            weights (array/create-array (float-array [0.25 0.25 0.25 0.25]) [4] defs/AF_DTYPE_F32)
            result (stats/var-weighted values weights)]
        (is (instance? AFArray result))))))

(deftest test-var-weighted-with-dimension
  (testing "var-weighted computes along specified dimension"
    (device/init!)
    (releasing!
      (let [values (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0 6.0]) [2 3] defs/AF_DTYPE_F32)
            weights (array/create-array (float-array [0.5 0.5]) [2] defs/AF_DTYPE_F32)
            result (stats/var-weighted values weights 0)]
        (is (instance? AFArray result))))))

(deftest test-var-all
  (testing "var-all computes scalar variance"
    (device/init!)
    (releasing!
      (let [data (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4] defs/AF_DTYPE_F32)
            result (stats/var-all data)]
        (is (number? result))))))

(deftest test-var-all-weighted
  (testing "var-all-weighted computes weighted scalar variance"
    (device/init!)
    (releasing!
      (let [values (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4] defs/AF_DTYPE_F32)
            weights (array/create-array (float-array [0.25 0.25 0.25 0.25]) [4] defs/AF_DTYPE_F32)
            result (stats/var-all-weighted values weights)]
        (is (number? result))))))

;;;
;;; Standard Deviation (Dispersion) Tests
;;;

(deftest test-stdev
  (testing "stdev computes standard deviation"
    (device/init!)
    (releasing!
      (let [data (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0 6.0]) [2 3] defs/AF_DTYPE_F32)
            result (stats/stdev data)]
        (is (instance? AFArray result))))))

(deftest test-stdev-with-dimension
  (testing "stdev computes along specified dimension"
    (device/init!)
    (releasing!
      (let [data (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0 6.0]) [2 3] defs/AF_DTYPE_F32)
            result (stats/stdev data defs/AF_VARIANCE_SAMPLE 1)]
        (is (instance? AFArray result))
        (is (= [2 1 1 1] (array/get-dims result)))))))

(deftest test-stdev-all
  (testing "stdev-all computes scalar standard deviation"
    (device/init!)
    (releasing!
      (let [data (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4] defs/AF_DTYPE_F32)
            result (stats/stdev-all data)]
        (is (number? result))))))

;;;
;;; Median (Robust Central Tendency) Tests
;;;

(deftest test-median
  (testing "median computes median value"
    (device/init!)
    (releasing!
      (let [data (array/create-array (float-array [1.0 3.0 2.0 5.0 4.0 6.0]) [2 3] defs/AF_DTYPE_F32)
            result (stats/median data)]
        (is (instance? AFArray result))))))

(deftest test-median-with-dimension
  (testing "median computes along specified dimension"
    (device/init!)
    (releasing!
      (let [data (array/create-array (float-array [1.0 3.0 2.0 5.0 4.0 6.0]) [2 3] defs/AF_DTYPE_F32)
            result (stats/median data 1)]
        (is (instance? AFArray result))
        (is (= [2 1 1 1] (array/get-dims result)))))))

(deftest test-median-all
  (testing "median-all computes scalar median"
    (device/init!)
    (releasing!
      (let [data (array/create-array (float-array [1.0 3.0 2.0 5.0 4.0]) [5] defs/AF_DTYPE_F32)
            result (stats/median-all data)]
        (is (number? result))
        (is (= 3.0 result))))))

;;;
;;; Combined Mean and Variance Tests
;;;

(deftest test-meanvar
  (testing "meanvar computes both mean and variance"
    (device/init!)
    (releasing!
      (let [data (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0 6.0]) [2 3] defs/AF_DTYPE_F32)
            weights (data/constant 1.0 [2 3])
            result (stats/meanvar data weights)
            mean-arr (:mean result)
            var-arr (:var result)]
        (is (map? result))
        (is (instance? AFArray mean-arr))
        (is (instance? AFArray var-arr))))))

(deftest test-meanvar-with-bias-and-dimension
  (testing "meanvar computes with bias and dimension"
    (device/init!)
    (releasing!
      (let [data (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0 6.0]) [2 3] defs/AF_DTYPE_F32)
            weights (data/constant 1.0 [2 3])
            result (stats/meanvar data weights defs/AF_VARIANCE_POPULATION 1)
            mean-arr (:mean result)
            var-arr (:var result)]
        (is (map? result))
        (is (instance? AFArray mean-arr))
        (is (instance? AFArray var-arr))
        (is (= [2 1 1 1] (array/get-dims mean-arr)))
        (is (= [2 1 1 1] (array/get-dims var-arr)))))))

;;;
;;; Covariance Tests
;;;

(deftest test-cov
  (testing "cov computes covariance between two variables"
    (device/init!)
    (releasing!
      (let [x (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0]) [5] defs/AF_DTYPE_F32)
            y (array/create-array (float-array [2.0 4.0 6.0 8.0 10.0]) [5] defs/AF_DTYPE_F32)
            result (stats/cov x y)]
        (is (instance? AFArray result))))))

(deftest test-cov-with-bias
  (testing "cov computes covariance with specified bias"
    (device/init!)
    (releasing!
      (let [x (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4] defs/AF_DTYPE_F32)
            y (array/create-array (float-array [2.0 3.0 4.0 5.0]) [4] defs/AF_DTYPE_F32)
            result (stats/cov x y defs/AF_VARIANCE_SAMPLE)]
        (is (instance? AFArray result))))))

;;;
;;; Correlation Coefficient Tests
;;;

(deftest test-corrcoef
  (testing "corrcoef computes correlation coefficient"
    (device/init!)
    (releasing!
      (let [x (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0]) [5] defs/AF_DTYPE_F32)
            y (array/create-array (float-array [2.0 4.0 6.0 8.0 10.0]) [5] defs/AF_DTYPE_F32)
            result (stats/corrcoef x y)]
        (is (number? result))
        ;; Perfect positive correlation should be close to 1.0
        (is (> result 0.99))))))

;;;
;;; Top-K Selection Tests
;;;

(deftest test-topk-max
  (testing "topk finds k largest values"
    (device/init!)
    (releasing!
      (let [data (array/create-array (float-array [5.0 2.0 8.0 1.0 9.0 3.0 7.0 4.0]) [8] defs/AF_DTYPE_F32)
            [values indices] (stats/topk data 3 0 defs/AF_TOPK_MAX)]
        (is (instance? AFArray values))
        (is (instance? AFArray indices))
        (is (= [3 1 1 1] (array/get-dims values)))
        (is (= [3 1 1 1] (array/get-dims indices)))))))

(deftest test-topk-min
  (testing "topk finds k smallest values"
    (device/init!)
    (releasing!
      (let [data (array/create-array (float-array [5.0 2.0 8.0 1.0 9.0 3.0]) [6] defs/AF_DTYPE_F32)
            [values indices] (stats/topk data 3 0 defs/AF_TOPK_MIN)]
        (is (instance? AFArray values))
        (is (instance? AFArray indices))
        (is (= [3 1 1 1] (array/get-dims values)))))))

(deftest test-topk-default
  (testing "topk uses default order (MAX)"
    (device/init!)
    (releasing!
      (let [data (array/create-array (float-array [5.0 2.0 8.0 1.0 9.0]) [5] defs/AF_DTYPE_F32)
            [values indices] (stats/topk data 2)]
        (is (instance? AFArray values))
        (is (instance? AFArray indices))))))

(deftest test-topk-with-dimension
  (testing "topk works along specified dimension"
    (device/init!)
    (releasing!
      (let [data (array/create-array (float-array [1.0 5.0 3.0 7.0 2.0 8.0 4.0 6.0]) [2 4] defs/AF_DTYPE_F32)
            [values indices] (stats/topk data 2 1 defs/AF_TOPK_MAX)]
        (is (instance? AFArray values))
        (is (instance? AFArray indices))
        (is (= [2 2 1 1] (array/get-dims values)))))))

(comment
  ;; run tests from REPL
  (run-tests)

  ;; run individual tests
  (run-test test-mean)
  (run-test test-mean-with-dimension)
  (run-test test-mean-weighted)
  (run-test test-mean-weighted-with-dimension)
  (run-test test-mean-all)
  (run-test test-mean-all-weighted)
  (run-test test-var)
  (run-test test-var-with-bias)
  (run-test test-var-with-dimension)
  (run-test test-var-weighted)
  (run-test test-var-weighted-with-dimension)
  (run-test test-var-all)
  (run-test test-var-all-weighted)
  (run-test test-stdev)
  (run-test test-stdev-with-dimension)
  (run-test test-stdev-all)
  (run-test test-median)
  (run-test test-median-with-dimension)
  (run-test test-median-all)
  (run-test test-meanvar)
  (run-test test-meanvar-with-bias-and-dimension)
  (run-test test-cov)
  (run-test test-cov-with-bias)
  (run-test test-corrcoef)
  (run-test test-topk-max)
  (run-test test-topk-min)
  (run-test test-topk-default)
  (run-test test-topk-with-dimension)
  
  ;
  )
