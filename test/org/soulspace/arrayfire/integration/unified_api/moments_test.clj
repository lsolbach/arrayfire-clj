(ns org.soulspace.arrayfire.integration.unified-api.moments-test
  (:require [clojure.test :refer [deftest is testing run-test run-tests]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.util.test :refer [approx=]]
            [org.soulspace.arrayfire.ffi.base.definitions :as defs]
            [org.soulspace.arrayfire.integration.base.resource :as res]
            [org.soulspace.arrayfire.integration.unified-api.moments :as moments]
            [org.soulspace.arrayfire.integration.unified-api.array :as array]
            [org.soulspace.arrayfire.integration.unified-api.device :as device])
  (:import [org.soulspace.arrayfire.integration.base.resource AFArray]))

;;;
;;; Moment Type Conversion Tests
;;;

(deftest test-moment-type-to-int-m00
  (testing "moment-type->int converts :m00 to correct integer"
    (device/init!)
    (is (= 1 (moments/moment-type->int :m00)))))

(deftest test-moment-type-to-int-m01
  (testing "moment-type->int converts :m01 to correct integer"
    (device/init!)
    (is (= 2 (moments/moment-type->int :m01)))))

(deftest test-moment-type-to-int-m10
  (testing "moment-type->int converts :m10 to correct integer"
    (device/init!)
    (is (= 4 (moments/moment-type->int :m10)))))

(deftest test-moment-type-to-int-m11
  (testing "moment-type->int converts :m11 to correct integer"
    (device/init!)
    (is (= 8 (moments/moment-type->int :m11)))))

(deftest test-moment-type-to-int-first-order
  (testing "moment-type->int converts :first-order to correct integer"
    (device/init!)
    (is (= 15 (moments/moment-type->int :first-order)))))

(deftest test-moment-type-to-int-set
  (testing "moment-type->int converts set of moment types"
    (device/init!)
    (is (= 3 (moments/moment-type->int #{:m00 :m01})))
    (is (= 5 (moments/moment-type->int #{:m00 :m10})))))

(deftest test-moment-type-to-int-integer
  (testing "moment-type->int passes through integer values"
    (device/init!)
    (is (= 7 (moments/moment-type->int 7)))))

;;;
;;; Image Moments Tests
;;;

(deftest test-moments-m00
  (testing "moments computes M00 (zeroth moment)"
    (device/init!)
    (let [img (array/create-array (float-array [1.0 1.0 1.0 1.0]) [2 2] defs/AF_DTYPE_F32)
          result (moments/moments img :m00)
          buf (mem/alloc 4)]
      (try
        (is (instance? AFArray result))
        (array/get-data-ptr result buf)
        ;; M00 should be sum of all pixels = 4.0
        (is (approx= 4.0 (mem/read-float buf 0) 0.01))
        (finally
          (.close img)
          (.close result))))))

(deftest test-moments-first-order
  (testing "moments computes all first-order moments"
    (device/init!)
    (let [img (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] defs/AF_DTYPE_F32)
          result (moments/moments img :first-order)
          buf (mem/alloc (* 4 4))]
      (try
        (is (instance? AFArray result))
        (is (= [4] (take 1 (array/get-dims result)))) ; 4 moments
        (array/get-data-ptr result buf)
        ;; M00 should be sum of all pixels = 10.0
        (is (approx= 10.0 (mem/read-float buf 0) 0.01))
        (finally
          (.close img)
          (.close result))))))

(deftest test-moments-binary-image
  (testing "moments on binary image counts pixels"
    (device/init!)
    (let [;; 3x3 binary image with 5 foreground pixels
          img (array/create-array (float-array [1.0 0.0 1.0
                                                 0.0 1.0 0.0
                                                 1.0 0.0 1.0]) [3 3] defs/AF_DTYPE_F32)
          result (moments/moments img :m00)
          buf (mem/alloc 4)]
      (try
        (array/get-data-ptr result buf)
        (is (approx= 5.0 (mem/read-float buf 0) 0.01))
        (finally
          (.close img)
          (.close result))))))

(deftest test-moments-multiple-types
  (testing "moments computes multiple specific moment types"
    (device/init!)
    (let [img (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] defs/AF_DTYPE_F32)
          result (moments/moments img #{:m00 :m01})]
      (try
        (is (instance? AFArray result))
        (is (= [2] (take 1 (array/get-dims result)))) ; 2 moments requested
        (finally
          (.close img)
          (.close result))))))

;;;
;;; Moments-All Tests
;;;

(deftest test-moments-all-default
  (testing "moments-all returns map with all first-order moments"
    (device/init!)
    (let [img (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] defs/AF_DTYPE_F32)
          result (moments/moments-all img)]
      (try
        (is (map? result))
        (is (contains? result :M00))
        (is (contains? result :M01))
        (is (contains? result :M10))
        (is (contains? result :M11))
        (is (approx= 10.0 (:M00 result) 0.01)) ; Sum of 1+2+3+4
        (finally
          (.close img))))))

(deftest test-moments-all-m00
  (testing "moments-all computes only M00 when requested"
    (device/init!)
    (let [img (array/create-array (float-array [2.0 2.0 2.0 2.0]) [2 2] defs/AF_DTYPE_F32)
          result (moments/moments-all img :m00)]
      (try
        (is (map? result))
        (is (contains? result :M00))
        (is (not (contains? result :M01)))
        (is (approx= 8.0 (:M00 result) 0.01))
        (finally
          (.close img))))))

(deftest test-moments-all-subset
  (testing "moments-all computes subset of moments"
    (device/init!)
    (let [img (array/create-array (float-array [1.0 1.0 1.0 1.0]) [2 2] defs/AF_DTYPE_F32)
          result (moments/moments-all img #{:m00 :m10})]
      (try
        (is (map? result))
        (is (contains? result :M00))
        (is (not (contains? result :M01)))
        (is (contains? result :M10))
        (is (not (contains? result :M11)))
        (finally
          (.close img))))))

;;;
;;; Convenience Functions Tests
;;;

(deftest test-centroid
  (testing "centroid computes center of mass"
    (device/init!)
    (let [;; Simple 3x3 image with center pixel set
          img (array/create-array (float-array [0.0 0.0 0.0
                                                 0.0 1.0 0.0
                                                 0.0 0.0 0.0]) [3 3] defs/AF_DTYPE_F32)
          result (moments/centroid img)]
      (try
        (is (map? result))
        (is (contains? result :x))
        (is (contains? result :y))
        (is (contains? result :area))
        (is (approx= 1.0 (:area result) 0.01))
        ;; Center should be at (1, 1) in 0-indexed coordinates
        (is (approx= 1.0 (:x result) 0.1))
        (is (approx= 1.0 (:y result) 0.1))
        (finally
          (.close img))))))

(deftest test-centroid-empty
  (testing "centroid returns nil for empty image"
    (device/init!)
    (let [img (array/create-array (float-array [0.0 0.0 0.0 0.0]) [2 2] defs/AF_DTYPE_F32)
          result (moments/centroid img)]
      (try
        (is (nil? result))
        (finally
          (.close img))))))

(deftest test-centroid-uniform
  (testing "centroid of uniform image is at geometric center"
    (device/init!)
    (let [img (array/create-array (float-array [1.0 1.0 1.0 1.0]) [2 2] defs/AF_DTYPE_F32)
          result (moments/centroid img)]
      (try
        (is (map? result))
        (is (approx= 4.0 (:area result) 0.01))
        ;; Centroid should be at center of 2x2 grid
        (is (approx= 0.5 (:x result) 0.1))
        (is (approx= 0.5 (:y result) 0.1))
        (finally
          (.close img))))))

(deftest test-area
  (testing "area computes total mass (M00)"
    (device/init!)
    (let [img (array/create-array (float-array [2.0 3.0 4.0 5.0]) [2 2] defs/AF_DTYPE_F32)
          result (moments/area img)]
      (try
        (is (number? result))
        (is (approx= 14.0 result 0.01)) ; Sum of 2+3+4+5
        (finally
          (.close img))))))

(deftest test-area-binary
  (testing "area counts pixels in binary image"
    (device/init!)
    (let [img (array/create-array (float-array [1.0 1.0 0.0 1.0 1.0 1.0]) [2 3] defs/AF_DTYPE_F32)
          result (moments/area img)]
      (try
        (is (approx= 5.0 result 0.01)) ; 5 pixels set to 1.0
        (finally
          (.close img))))))

(deftest test-area-zero
  (testing "area returns 0 for empty image"
    (device/init!)
    (let [img (array/create-array (float-array [0.0 0.0 0.0 0.0]) [2 2] defs/AF_DTYPE_F32)
          result (moments/area img)]
      (try
        (is (approx= 0.0 result 0.01))
        (finally
          (.close img))))))

(deftest test-moments-first-order-fn
  (testing "moments-first-order returns all first-order moments as map"
    (device/init!)
    (let [img (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] defs/AF_DTYPE_F32)
          result (moments/moments-first-order img)]
      (try
        (is (map? result))
        (is (contains? result :M00))
        (is (contains? result :M01))
        (is (contains? result :M10))
        (is (contains? result :M11))
        (is (approx= 10.0 (:M00 result) 0.01))
        (finally
          (.close img))))))

;;;
;;; Integration Tests
;;;

(deftest test-centroid-from-moments
  (testing "Centroid calculated from moments matches centroid function"
    (device/init!)
    (let [img (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] defs/AF_DTYPE_F32)
          direct (moments/centroid img)
          from-moments (moments/moments-first-order img)
          cx (/ (:M01 from-moments) (:M00 from-moments))
          cy (/ (:M10 from-moments) (:M00 from-moments))]
      (try
        (is (approx= (:x direct) cx 0.01))
        (is (approx= (:y direct) cy 0.01))
        (is (approx= (:area direct) (:M00 from-moments) 0.01))
        (finally
          (.close img))))))

(deftest test-moments-different-dtypes
  (testing "moments works with different data types"
    (device/init!)
    (let [img-f32 (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] defs/AF_DTYPE_F32)
          img-f64 (array/create-array (double-array [1.0 2.0 3.0 4.0]) [2 2] defs/AF_DTYPE_F64)
          result-f32 (moments/area img-f32)
          result-f64 (moments/area img-f64)]
      (try
        (is (approx= 10.0 result-f32 0.01))
        (is (approx= 10.0 result-f64 0.01))
        (finally
          (.close img-f32)
          (.close img-f64))))))

(comment
  ;; run all tests from REPL
  (run-tests)
  
  ;; run individual tests
  (run-test test-moment-type-to-int-m00)
  (run-test test-moment-type-to-int-m01)
  (run-test test-moment-type-to-int-m10)
  (run-test test-moment-type-to-int-m11)
  (run-test test-moment-type-to-int-first-order)
  (run-test test-moment-type-to-int-set)
  (run-test test-moment-type-to-int-integer)
  (run-test test-moments-m00)
  (run-test test-moments-first-order)
  (run-test test-moments-binary-image)
  (run-test test-moments-multiple-types)
  (run-test test-moments-all-default)
  (run-test test-moments-all-m00)
  (run-test test-moments-all-subset)
  (run-test test-centroid)
  (run-test test-centroid-empty)
  (run-test test-centroid-uniform)
  (run-test test-area)
  (run-test test-area-binary)
  (run-test test-area-zero)
  (run-test test-moments-first-order-fn)
  (run-test test-centroid-from-moments)
  (run-test test-moments-different-dtypes)
  
  ;
  )
