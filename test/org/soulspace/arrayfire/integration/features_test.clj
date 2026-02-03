(ns org.soulspace.arrayfire.integration.features-test
  (:require [clojure.test :refer [deftest is testing run-test run-tests]]
            [org.soulspace.arrayfire.integration.features :as features]
            [org.soulspace.arrayfire.integration.array :as array]
            [org.soulspace.arrayfire.integration.device :as device]
            [org.soulspace.arrayfire.integration.jvm-integration :as jvm])
  (:import [org.soulspace.arrayfire.integration.jvm_integration AFArray]))

;;;
;;; Feature Lifecycle Management Tests
;;;

(deftest test-create-features
  (testing "create-features creates new features handle"
    (device/init!)
    (let [feat (features/create-features 10)]
      (is (integer? feat))
      (is (not (zero? feat)))
      (features/release-features! feat))))

(deftest test-create-features-zero
  (testing "create-features can create empty features structure"
    (device/init!)
    (let [feat (features/create-features 0)]
      (is (integer? feat))
      (features/release-features! feat))))

(deftest test-create-features-large
  (testing "create-features can allocate large feature set"
    (device/init!)
    (let [feat (features/create-features 1000)]
      (is (integer? feat))
      (features/release-features! feat))))

(deftest test-retain-features
  (testing "retain-features creates shallow copy with shared data"
    (device/init!)
    (let [feat1 (features/create-features 5)
          feat2 (features/retain-features feat1)]
      (is (integer? feat1))
      (is (integer? feat2))
      (is (not= feat1 feat2)) ; Different handles
      (features/release-features! feat1)
      (features/release-features! feat2))))

(deftest test-retain-features-independent-release
  (testing "retained features can be released independently"
    (device/init!)
    (let [feat1 (features/create-features 3)]
      (try
        (let [feat2 (features/retain-features feat1)]
          (features/release-features! feat2))
        ;; feat1 should still be valid
        (is (integer? feat1))
        (finally
          (features/release-features! feat1))))))

(deftest test-release-features
  (testing "release-features! frees feature resources"
    (device/init!)
    (let [feat (features/create-features 10)]
      (is (nil? (features/release-features! feat))))))

(deftest test-release-features-nil
  (testing "release-features! handles nil safely"
    (device/init!)
    (is (nil? (features/release-features! nil)))))

(deftest test-features-lifecycle
  (testing "complete features lifecycle: create, use, release"
    (device/init!)
    (let [feat (features/create-features 5)]
      (try
        (is (integer? feat))
        (let [n (features/get-features-num feat)]
          (is (= 5 n)))
        (finally
          (features/release-features! feat))))))

;;;
;;; Feature Property Access Tests
;;;

(deftest test-get-features-num
  (testing "get-features-num returns correct count"
    (device/init!)
    (let [feat (features/create-features 42)]
      (try
        (let [n (features/get-features-num feat)]
          (is (integer? n))
          (is (= 42 n)))
        (finally
          (features/release-features! feat))))))

(deftest test-get-features-num-zero
  (testing "get-features-num returns 0 for empty features"
    (device/init!)
    (let [feat (features/create-features 0)]
      (try
        (let [n (features/get-features-num feat)]
          (is (= 0 n)))
        (finally
          (features/release-features! feat))))))

(deftest test-get-features-xpos
  (testing "get-features-xpos returns AFArray"
    (device/init!)
    (let [feat (features/create-features 10)]
      (try
        (let [x (features/get-features-xpos feat)]
          (is (instance? AFArray x))
          (is (= [10] (array/get-dims x)))
          (is (= jvm/AF_DTYPE_F32 (array/get-type x))))
        (finally
          (features/release-features! feat))))))

(deftest test-get-features-ypos
  (testing "get-features-ypos returns AFArray"
    (device/init!)
    (let [feat (features/create-features 10)]
      (try
        (let [y (features/get-features-ypos feat)]
          (is (instance? AFArray y))
          (is (= [10] (array/get-dims y)))
          (is (= jvm/AF_DTYPE_F32 (array/get-type y))))
        (finally
          (features/release-features! feat))))))

(deftest test-get-features-score
  (testing "get-features-score returns AFArray"
    (device/init!)
    (let [feat (features/create-features 10)]
      (try
        (let [score (features/get-features-score feat)]
          (is (instance? AFArray score))
          (is (= [10] (array/get-dims score)))
          (is (= jvm/AF_DTYPE_F32 (array/get-type score))))
        (finally
          (features/release-features! feat))))))

(deftest test-get-features-orientation
  (testing "get-features-orientation returns AFArray"
    (device/init!)
    (let [feat (features/create-features 10)]
      (try
        (let [ori (features/get-features-orientation feat)]
          (is (instance? AFArray ori))
          (is (= [10] (array/get-dims ori)))
          (is (= jvm/AF_DTYPE_F32 (array/get-type ori))))
        (finally
          (features/release-features! feat))))))

(deftest test-get-features-size
  (testing "get-features-size returns AFArray"
    (device/init!)
    (let [feat (features/create-features 10)]
      (try
        (let [size (features/get-features-size feat)]
          (is (instance? AFArray size))
          (is (= [10] (array/get-dims size)))
          (is (= jvm/AF_DTYPE_F32 (array/get-type size))))
        (finally
          (features/release-features! feat))))))

;;;
;;; Feature Property Consistency Tests
;;;

(deftest test-all-arrays-same-length
  (testing "all property arrays have same length as feature count"
    (device/init!)
    (let [feat (features/create-features 15)]
      (try
        (let [n (features/get-features-num feat)
              x (features/get-features-xpos feat)
              y (features/get-features-ypos feat)
              score (features/get-features-score feat)
              ori (features/get-features-orientation feat)
              size (features/get-features-size feat)]
          (is (= n 15))
          (is (= [n] (array/get-dims x)))
          (is (= [n] (array/get-dims y)))
          (is (= [n] (array/get-dims score)))
          (is (= [n] (array/get-dims ori)))
          (is (= [n] (array/get-dims size))))
        (finally
          (features/release-features! feat))))))

(deftest test-all-arrays-float32
  (testing "all property arrays are float32 type"
    (device/init!)
    (let [feat (features/create-features 5)]
      (try
        (let [x (features/get-features-xpos feat)
              y (features/get-features-ypos feat)
              score (features/get-features-score feat)
              ori (features/get-features-orientation feat)
              size (features/get-features-size feat)]
          (is (= jvm/AF_DTYPE_F32 (array/get-type x)))
          (is (= jvm/AF_DTYPE_F32 (array/get-type y)))
          (is (= jvm/AF_DTYPE_F32 (array/get-type score)))
          (is (= jvm/AF_DTYPE_F32 (array/get-type ori)))
          (is (= jvm/AF_DTYPE_F32 (array/get-type size))))
        (finally
          (features/release-features! feat))))))

;;;
;;; Multiple Features Tests
;;;

(deftest test-multiple-features-independent
  (testing "multiple features structures are independent"
    (device/init!)
    (let [feat1 (features/create-features 10)
          feat2 (features/create-features 20)]
      (try
        (is (= 10 (features/get-features-num feat1)))
        (is (= 20 (features/get-features-num feat2)))
        (finally
          (features/release-features! feat1)
          (features/release-features! feat2))))))

(deftest test-features-with-try-finally
  (testing "features properly managed with try-finally"
    (device/init!)
    (let [feat (features/create-features 7)]
      (try
        (let [n (features/get-features-num feat)
              x (features/get-features-xpos feat)
              y (features/get-features-ypos feat)]
          (is (= 7 n))
          (is (instance? AFArray x))
          (is (instance? AFArray y)))
        (finally
          (features/release-features! feat))))))

(comment
  ;; run all tests from REPL
  (run-tests)
  
  ;; run individual tests
  (run-test test-create-features)
  (run-test test-create-features-zero)
  (run-test test-create-features-large)
  (run-test test-retain-features)
  (run-test test-retain-features-independent-release)
  (run-test test-release-features)
  (run-test test-release-features-nil)
  (run-test test-features-lifecycle)
  (run-test test-get-features-num)
  (run-test test-get-features-num-zero)
  (run-test test-get-features-xpos)
  (run-test test-get-features-ypos)
  (run-test test-get-features-score)
  (run-test test-get-features-orientation)
  (run-test test-get-features-size)
  (run-test test-all-arrays-same-length)
  (run-test test-all-arrays-float32)
  (run-test test-multiple-features-independent)
  (run-test test-features-with-try-finally)
  
  ;
  )
