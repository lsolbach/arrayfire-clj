(ns org.soulspace.arrayfire.integration.statistics-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [org.soulspace.arrayfire.integration.statistics :as stats]
            [org.soulspace.arrayfire.integration.array :as array]
            [org.soulspace.arrayfire.integration.data :as data]
            [org.soulspace.arrayfire.integration.device :as device]
            [org.soulspace.arrayfire.integration.jvm-integration :as jvm])
  (:import [org.soulspace.arrayfire.integration.jvm_integration AFArray]))

;;;
;;; Mean (Central Tendency) Tests
;;;

(deftest test-mean
  (testing "mean computes arithmetic mean along default dimension"
    (device/init!)
    (let [data (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0 6.0]) [2 3] jvm/AF_DTYPE_F32)
          result (stats/mean data)]
      (is (instance? AFArray result))
      (is (= [1 3] (array/get-dims result)))
      (.close result)
      (.close data))))

(deftest test-mean-with-dimension
  (testing "mean computes along specified dimension"
    (device/init!)
    (let [data (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0 6.0]) [2 3] jvm/AF_DTYPE_F32)
          result-dim0 (stats/mean data 0)
          result-dim1 (stats/mean data 1)]
      (is (instance? AFArray result-dim0))
      (is (instance? AFArray result-dim1))
      (is (= [1 3] (array/get-dims result-dim0)))
      (is (= [2 1] (array/get-dims result-dim1)))
      (.close result-dim1)
      (.close result-dim0)
      (.close data))))

(deftest test-mean-weighted
  (testing "mean-weighted computes weighted mean"
    (device/init!)
    (let [values (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4] jvm/AF_DTYPE_F32)
          weights (array/create-array (float-array [0.1 0.2 0.3 0.4]) [4] jvm/AF_DTYPE_F32)
          result (stats/mean-weighted values weights)]
      (is (instance? AFArray result))
      (.close result)
      (.close weights)
      (.close values))))

(deftest test-mean-weighted-with-dimension
  (testing "mean-weighted computes along specified dimension"
    (device/init!)
    (let [values (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0 6.0]) [2 3] jvm/AF_DTYPE_F32)
          weights (array/create-array (float-array [0.4 0.6]) [2] jvm/AF_DTYPE_F32)
          result (stats/mean-weighted values weights 0)]
      (is (instance? AFArray result))
      (.close result)
      (.close weights)
      (.close values))))

(deftest test-mean-all
  (testing "mean-all computes scalar mean of all elements"
    (device/init!)
    (let [data (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4] jvm/AF_DTYPE_F32)
          result (stats/mean-all data)]
      (is (map? result))
      (is (contains? result :real))
      (is (contains? result :imag))
      (is (number? (:real result)))
      (is (number? (:imag result)))
      (is (= 2.5 (:real result)))
      (.close data))))

(deftest test-mean-all-weighted
  (testing "mean-all-weighted computes weighted scalar mean"
    (device/init!)
    (let [values (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4] jvm/AF_DTYPE_F32)
          weights (array/create-array (float-array [0.1 0.2 0.3 0.4]) [4] jvm/AF_DTYPE_F32)
          result (stats/mean-all-weighted values weights)]
      (is (map? result))
      (is (contains? result :real))
      (is (contains? result :imag))
      (is (number? (:real result)))
      (.close weights)
      (.close values))))

;;;
;;; Variance (Dispersion) Tests
;;;

(deftest test-var
  (testing "var computes variance with default bias"
    (device/init!)
    (let [data (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0 6.0]) [2 3] jvm/AF_DTYPE_F32)
          result (stats/var data)]
      (is (instance? AFArray result))
      (.close result)
      (.close data))))

(deftest test-var-with-bias
  (testing "var computes variance with specified bias"
    (device/init!)
    (let [data (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4] jvm/AF_DTYPE_F32)
          result-sample (stats/var data stats/VARIANCE_SAMPLE)
          result-pop (stats/var data stats/VARIANCE_POPULATION)]
      (is (instance? AFArray result-sample))
      (is (instance? AFArray result-pop))
      (.close result-pop)
      (.close result-sample)
      (.close data))))

(deftest test-var-with-dimension
  (testing "var computes variance along specified dimension"
    (device/init!)
    (let [data (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0 6.0]) [2 3] jvm/AF_DTYPE_F32)
          result (stats/var data stats/VARIANCE_DEFAULT 1)]
      (is (instance? AFArray result))
      (is (= [2 1] (array/get-dims result)))
      (.close result)
      (.close data))))

(deftest test-var-weighted
  (testing "var-weighted computes weighted variance"
    (device/init!)
    (let [values (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4] jvm/AF_DTYPE_F32)
          weights (array/create-array (float-array [0.25 0.25 0.25 0.25]) [4] jvm/AF_DTYPE_F32)
          result (stats/var-weighted values weights)]
      (is (instance? AFArray result))
      (.close result)
      (.close weights)
      (.close values))))

(deftest test-var-weighted-with-dimension
  (testing "var-weighted computes along specified dimension"
    (device/init!)
    (let [values (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0 6.0]) [2 3] jvm/AF_DTYPE_F32)
          weights (array/create-array (float-array [0.5 0.5]) [2] jvm/AF_DTYPE_F32)
          result (stats/var-weighted values weights 0)]
      (is (instance? AFArray result))
      (.close result)
      (.close weights)
      (.close values))))

(deftest test-var-all
  (testing "var-all computes scalar variance"
    (device/init!)
    (let [data (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4] jvm/AF_DTYPE_F32)
          result (stats/var-all data)]
      (is (map? result))
      (is (contains? result :real))
      (is (contains? result :imag))
      (is (number? (:real result)))
      (.close data))))

(deftest test-var-all-weighted
  (testing "var-all-weighted computes weighted scalar variance"
    (device/init!)
    (let [values (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4] jvm/AF_DTYPE_F32)
          weights (array/create-array (float-array [0.25 0.25 0.25 0.25]) [4] jvm/AF_DTYPE_F32)
          result (stats/var-all-weighted values weights)]
      (is (map? result))
      (is (contains? result :real))
      (.close weights)
      (.close values))))

;;;
;;; Standard Deviation (Dispersion) Tests
;;;

(deftest test-stdev
  (testing "stdev computes standard deviation"
    (device/init!)
    (let [data (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0 6.0]) [2 3] jvm/AF_DTYPE_F32)
          result (stats/stdev data)]
      (is (instance? AFArray result))
      (.close result)
      (.close data))))

(deftest test-stdev-with-dimension
  (testing "stdev computes along specified dimension"
    (device/init!)
    (let [data (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0 6.0]) [2 3] jvm/AF_DTYPE_F32)
          result (stats/stdev data 1)]
      (is (instance? AFArray result))
      (is (= [2 1] (array/get-dims result)))
      (.close result)
      (.close data))))

(deftest test-stdev-all
  (testing "stdev-all computes scalar standard deviation"
    (device/init!)
    (let [data (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4] jvm/AF_DTYPE_F32)
          result (stats/stdev-all data)]
      (is (map? result))
      (is (contains? result :real))
      (is (contains? result :imag))
      (is (number? (:real result)))
      (.close data))))

;;;
;;; Median (Robust Central Tendency) Tests
;;;

(deftest test-median
  (testing "median computes median value"
    (device/init!)
    (let [data (array/create-array (float-array [1.0 3.0 2.0 5.0 4.0 6.0]) [2 3] jvm/AF_DTYPE_F32)
          result (stats/median data)]
      (is (instance? AFArray result))
      (.close result)
      (.close data))))

(deftest test-median-with-dimension
  (testing "median computes along specified dimension"
    (device/init!)
    (let [data (array/create-array (float-array [1.0 3.0 2.0 5.0 4.0 6.0]) [2 3] jvm/AF_DTYPE_F32)
          result (stats/median data 1)]
      (is (instance? AFArray result))
      (is (= [2 1] (array/get-dims result)))
      (.close result)
      (.close data))))

(deftest test-median-all
  (testing "median-all computes scalar median"
    (device/init!)
    (let [data (array/create-array (float-array [1.0 3.0 2.0 5.0 4.0]) [5] jvm/AF_DTYPE_F32)
          result (stats/median-all data)]
      (is (map? result))
      (is (contains? result :real))
      (is (contains? result :imag))
      (is (number? (:real result)))
      (is (= 3.0 (:real result)))
      (.close data))))

;;;
;;; Combined Mean and Variance Tests
;;;

(deftest test-meanvar
  (testing "meanvar computes both mean and variance"
    (device/init!)
    (let [data (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0 6.0]) [2 3] jvm/AF_DTYPE_F32)
          weights (data/constant 1.0 [2 3])
          result (stats/meanvar data weights)
          mean-arr (:mean result)
          var-arr (:var result)]
      (is (map? result))
      (is (instance? AFArray mean-arr))
      (is (instance? AFArray var-arr))
      (.close var-arr)
      (.close mean-arr)
      (.close weights)
      (.close data))))

(deftest test-meanvar-with-bias-and-dimension
  (testing "meanvar computes with bias and dimension"
    (device/init!)
    (let [data (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0 6.0]) [2 3] jvm/AF_DTYPE_F32)
          weights (data/constant 1.0 [2 3])
          result (stats/meanvar data weights stats/VARIANCE_POPULATION 1)
          mean-arr (:mean result)
          var-arr (:var result)]
      (is (map? result))
      (is (instance? AFArray mean-arr))
      (is (instance? AFArray var-arr))
      (is (= [2 1] (array/get-dims mean-arr)))
      (is (= [2 1] (array/get-dims var-arr)))
      (.close var-arr)
      (.close mean-arr)
      (.close weights)
      (.close data))))

;;;
;;; Covariance Tests
;;;

(deftest test-cov
  (testing "cov computes covariance between two variables"
    (device/init!)
    (let [x (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0]) [5] jvm/AF_DTYPE_F32)
          y (array/create-array (float-array [2.0 4.0 6.0 8.0 10.0]) [5] jvm/AF_DTYPE_F32)
          result (stats/cov x y)]
      (is (instance? AFArray result))
      (.close result)
      (.close y)
      (.close x))))

(deftest test-cov-with-bias
  (testing "cov computes covariance with specified bias"
    (device/init!)
    (let [x (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4] jvm/AF_DTYPE_F32)
          y (array/create-array (float-array [2.0 3.0 4.0 5.0]) [4] jvm/AF_DTYPE_F32)
          result (stats/cov x y stats/VARIANCE_SAMPLE)]
      (is (instance? AFArray result))
      (.close result)
      (.close y)
      (.close x))))

;;;
;;; Correlation Coefficient Tests
;;;

(deftest test-corrcoef
  (testing "corrcoef computes correlation coefficient"
    (device/init!)
    (let [x (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0]) [5] jvm/AF_DTYPE_F32)
          y (array/create-array (float-array [2.0 4.0 6.0 8.0 10.0]) [5] jvm/AF_DTYPE_F32)
          result (stats/corrcoef x y)]
      (is (map? result))
      (is (contains? result :real))
      (is (contains? result :imag))
      (is (number? (:real result)))
      ;; Perfect positive correlation should be close to 1.0
      (is (> (:real result) 0.99))
      (.close y)
      (.close x))))

;;;
;;; Top-K Selection Tests
;;;

(deftest test-topk-max
  (testing "topk finds k largest values"
    (device/init!)
    (let [data (array/create-array (float-array [5.0 2.0 8.0 1.0 9.0 3.0 7.0 4.0]) [8] jvm/AF_DTYPE_F32)
          [values indices] (stats/topk data 3 stats/TOPK_MAX 0)]
      (is (instance? AFArray values))
      (is (instance? AFArray indices))
      (is (= [3] (array/get-dims values)))
      (is (= [3] (array/get-dims indices)))
      (.close indices)
      (.close values)
      (.close data))))

(deftest test-topk-min
  (testing "topk finds k smallest values"
    (device/init!)
    (let [data (array/create-array (float-array [5.0 2.0 8.0 1.0 9.0 3.0]) [6] jvm/AF_DTYPE_F32)
          [values indices] (stats/topk data 3 stats/TOPK_MIN 0)]
      (is (instance? AFArray values))
      (is (instance? AFArray indices))
      (is (= [3] (array/get-dims values)))
      (.close indices)
      (.close values)
      (.close data))))

(deftest test-topk-default
  (testing "topk uses default order (MAX)"
    (device/init!)
    (let [data (array/create-array (float-array [5.0 2.0 8.0 1.0 9.0]) [5] jvm/AF_DTYPE_F32)
          [values indices] (stats/topk data 2)]
      (is (instance? AFArray values))
      (is (instance? AFArray indices))
      (.close indices)
      (.close values)
      (.close data))))

(deftest test-topk-with-dimension
  (testing "topk works along specified dimension"
    (device/init!)
    (let [data (array/create-array (float-array [1.0 5.0 3.0 7.0 2.0 8.0 4.0 6.0]) [2 4] jvm/AF_DTYPE_F32)
          [values indices] (stats/topk data 2 stats/TOPK_MAX 1)]
      (is (instance? AFArray values))
      (is (instance? AFArray indices))
      (is (= [2 2] (array/get-dims values)))
      (.close indices)
      (.close values)
      (.close data))))

(comment
  ;; run tests from REPL
  (run-tests)
  ;
  )
