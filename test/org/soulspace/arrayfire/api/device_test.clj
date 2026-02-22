(ns org.soulspace.arrayfire.api.device-test
  "Tests for the idiomatic Clojure device-query API.
   All tests run inside (with-arrayfire ...) regions."
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [org.soulspace.arrayfire.api.core :as af]
            [org.soulspace.arrayfire.api.device :as dev]))

(def ^:private valid-backend-keywords
  "Set of all backend keywords that ArrayFire can return."
  #{:default :cpu :cuda :opencl :oneapi})

;;;
;;; current-device
;;;

(deftest current-device-test
  (testing "current-device returns a non-negative integer"
    (let [result (af/with-arrayfire (dev/current-device))]
      (is (integer? result))
      (is (>= result 0)))))

;;;
;;; active-backend
;;;

(deftest active-backend-test
  (testing "active-backend returns a valid backend keyword"
    (let [result (af/with-arrayfire (dev/active-backend))]
      (is (keyword? result))
      (is (contains? valid-backend-keywords result)))))

;;;
;;; backend-count
;;;

(deftest backend-count-test
  (testing "backend-count returns a positive integer"
    (let [result (af/with-arrayfire (dev/backend-count))]
      (is (integer? result))
      (is (pos? result)))))

;;;
;;; double-available?
;;;

(deftest double-available?-test
  (testing "double-available? with no args returns a boolean"
    (let [result (af/with-arrayfire (dev/double-available?))]
      (is (boolean? result))))
  (testing "double-available? with device-id 0 returns a boolean"
    (let [result (af/with-arrayfire (dev/double-available? 0))]
      (is (boolean? result))))
  (testing "double-available? with no args and with device-id 0 agree"
    (let [[arity0 arity1]
          (af/with-arrayfire
            [(dev/double-available?)
             (dev/double-available? (dev/current-device))])]
      (is (= arity0 arity1)))))

;;;
;;; half-available?
;;;

(deftest half-available?-test
  (testing "half-available? with no args returns a boolean"
    (let [result (af/with-arrayfire (dev/half-available?))]
      (is (boolean? result))))
  (testing "half-available? with device-id 0 returns a boolean"
    (let [result (af/with-arrayfire (dev/half-available? 0))]
      (is (boolean? result))))
  (testing "half-available? with no args and with device-id 0 agree"
    (let [[arity0 arity1]
          (af/with-arrayfire
            [(dev/half-available?)
             (dev/half-available? (dev/current-device))])]
      (is (= arity0 arity1)))))

;;;
;;; device-mem-info
;;;

(deftest device-mem-info-test
  (testing "device-mem-info returns a map with the expected keys"
    (let [result (af/with-arrayfire (dev/device-mem-info))]
      (is (map? result))
      (is (contains? result :alloc-bytes))
      (is (contains? result :alloc-buffers))
      (is (contains? result :lock-bytes))
      (is (contains? result :lock-buffers))))
  (testing "device-mem-info values are non-negative numbers"
    (let [{:keys [alloc-bytes alloc-buffers lock-bytes lock-buffers]}
          (af/with-arrayfire (dev/device-mem-info))]
      (is (>= alloc-bytes 0))
      (is (>= alloc-buffers 0))
      (is (>= lock-bytes 0))
      (is (>= lock-buffers 0))))
  (testing "device-mem-info locked bytes do not exceed allocated bytes"
    (let [{:keys [alloc-bytes lock-bytes]}
          (af/with-arrayfire (dev/device-mem-info))]
      (is (<= lock-bytes alloc-bytes)))))

;;;
;;; array-backend
;;;

(deftest array-backend-test
  (testing "array-backend returns a valid backend keyword"
    (let [result (af/with-arrayfire
                   (let [a (af/array [1.0 2.0 3.0] [3] :f64)]
                     (dev/array-backend a)))]
      (is (keyword? result))
      (is (contains? valid-backend-keywords result))))
  (testing "array-backend matches active-backend for an array created in the same region"
    (let [[arr-backend active]
          (af/with-arrayfire
            (let [a (af/array [1.0 2.0] [2] :f64)]
              [(dev/array-backend a)
               (dev/active-backend)]))]
      (is (= arr-backend active)))))

;;;
;;; array-device
;;;

(deftest array-device-test
  (testing "array-device returns a non-negative integer"
    (let [result (af/with-arrayfire
                   (let [a (af/array [1.0 2.0 3.0] [3] :f64)]
                     (dev/array-device a)))]
      (is (integer? result))
      (is (>= result 0))))
  (testing "array-device matches current-device for an array created in the same region"
    (let [[arr-device current]
          (af/with-arrayfire
            (let [a (af/array [1.0 2.0] [2] :f64)]
              [(dev/array-device a)
               (dev/current-device)]))]
      (is (= arr-device current)))))

;;;
;;; locked-array?
;;;

(deftest locked-array?-test
  (testing "freshly created array is not locked"
    (let [result (af/with-arrayfire
                   (let [a (af/array [1.0 2.0 3.0] [3] :f64)]
                     (dev/locked-array? a)))]
      (is (boolean? result))
      (is (false? result)))))

;;;
;;; Guard: functions must be called within with-arrayfire
;;;

(deftest assert-within-arrayfire-guard-test
  (testing "current-device throws outside with-arrayfire"
    (is (thrown? Exception (dev/current-device))))
  (testing "active-backend throws outside with-arrayfire"
    (is (thrown? Exception (dev/active-backend))))
  (testing "double-available? throws outside with-arrayfire"
    (is (thrown? Exception (dev/double-available?))))
  (testing "device-mem-info throws outside with-arrayfire"
    (is (thrown? Exception (dev/device-mem-info)))))
