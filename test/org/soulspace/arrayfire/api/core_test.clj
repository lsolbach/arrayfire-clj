(ns org.soulspace.arrayfire.api.core-test
  "Tests for the arrayfire-clj core namespace, including the `with-arrayfire`
   execution region macro."
  (:require [clojure.test :refer [deftest is testing]]
            [org.soulspace.arrayfire.api.core :as core]
            [org.soulspace.arrayfire.ffi.base.definitions :as defs]
            [org.soulspace.arrayfire.integration.base.memory :as bmem]
            [org.soulspace.arrayfire.integration.base.resource :as res]
            [org.soulspace.arrayfire.integration.unified-api.array :as ua-array]
            [org.soulspace.arrayfire.integration.unified-api.device :as device])
  (:import (org.soulspace.arrayfire.integration.base.resource AFArray)))

;;;
;;; result-convert tests
;;;
(deftest result-convert-passthrough-test
  (testing "Non-AFArray values pass through unchanged"
    (is (= 42 (core/result-convert identity 42)))
    (is (= "hello" (core/result-convert identity "hello")))
    (is (nil? (core/result-convert identity nil)))))

(deftest result-convert-map-test
  (testing "Maps are walked recursively"
    (is (= {:a 1 :b 2} (core/result-convert identity {:a 1 :b 2})))))

(deftest result-convert-vector-test
  (testing "Vectors are walked recursively"
    (is (= [1 2 3] (core/result-convert identity [1 2 3])))))

(deftest result-convert-set-test
  (testing "Sets are walked recursively"
    (is (= #{1 2 3} (core/result-convert identity #{1 2 3})))))

;;;
;;; with-arrayfire macro tests
;;;
(deftest with-arrayfire-basic-test
  (testing "Basic with-arrayfire region with explicit host conversion"
    (let [result (core/with-arrayfire
                   (let [a (core/create-array [1.0 2.0 3.0 4.0] [2 2])]
                     ;; create-array returns AFArray; to-host accepts AFArray
                     (vec (core/to-host a 4))))]
      (is (= [1.0 2.0 3.0 4.0] result)))))

(deftest with-arrayfire-auto-convert-test
  (testing "AFArray result is auto-converted to native buffer"
    (let [result (core/with-arrayfire
                   (ua-array/create-array
                     (bmem/double-array->segment (double-array [10.0 20.0 30.0]))
                     [3]
                     defs/AF_DTYPE_F64))]
      (is (not (instance? AFArray result)))
      (is (= [10.0 20.0 30.0] (vec result))))))

(deftest with-arrayfire-deep-convert-map-test
  (testing "Map containing AFArray is deep-converted"
    (let [result (core/with-arrayfire
                   {:data (ua-array/create-array
                            (bmem/double-array->segment (double-array [1.0 2.0]))
                            [2]
                            defs/AF_DTYPE_F64)
                    :scalar 42})]
      (is (map? result))
      (is (= [1.0 2.0] (vec (:data result))))
      (is (= 42 (:scalar result))))))

(deftest with-arrayfire-deep-convert-vector-test
  (testing "Vector containing AFArray is deep-converted"
    (let [result (core/with-arrayfire
                   [(ua-array/create-array
                      (bmem/double-array->segment (double-array [1.0]))
                      [1]
                      defs/AF_DTYPE_F64)
                    42])]
      (is (vector? result))
      (is (= [1.0] (vec (first result))))
      (is (= 42 (second result))))))

(deftest with-arrayfire-backend-test
  (testing "Backend option sets and restores backend"
    (core/ensure-af-init!)
    (let [original-backend (device/get-active-backend)]
      (core/with-arrayfire {:backend :cpu}
        (is (= defs/AF_BACKEND_CPU (device/get-active-backend))))
      (is (= original-backend (device/get-active-backend))))))

(deftest with-arrayfire-exception-safety-test
  (testing "Backend/device restored after exception"
    (core/ensure-af-init!)
    (let [original-backend (device/get-active-backend)]
      (is (thrown? Exception
            (core/with-arrayfire {:backend :cpu}
              (throw (Exception. "test error")))))
      (is (= original-backend (device/get-active-backend))))))

(deftest with-arrayfire-nested-test
  (testing "Nested with-arrayfire regions work correctly"
    (let [result (core/with-arrayfire
                   (core/with-arrayfire
                     ;; create-array returns AFArray; to-host accepts AFArray
                     (vec (core/to-host (core/create-array [42.0] [1]) 1))))]
      (is (= [42.0] result)))))

(deftest with-arrayfire-converter-fn-test
  (testing "Custom converter-fn is used"
    (let [result (core/with-arrayfire {:converter-fn (fn [_arr] :converted)}
                   (ua-array/create-array
                     (bmem/double-array->segment (double-array [1.0]))
                     [1]
                     defs/AF_DTYPE_F64))]
      (is (= :converted result)))))

(deftest with-arrayfire-identity-converter-test
  (testing "Identity converter skips auto-conversion"
    (let [result (core/with-arrayfire {:converter-fn identity}
                   42)]
      (is (= 42 result)))))

(deftest with-arrayfire-nil-result-test
  (testing "Nil result passes through"
    (let [result (core/with-arrayfire nil)]
      (is (nil? result)))))

(deftest with-arrayfire-map-not-treated-as-opts-test
  (testing "Map literal body without known option keys is NOT treated as options"
    (let [result (core/with-arrayfire
                   {:x 1 :y 2})]
      (is (= {:x 1 :y 2} result)))))

;;;
;;; within-arrayfire? and assert-within-arrayfire! tests
;;;
(deftest within-arrayfire-outside-test
  (testing "within-arrayfire? returns false when no region is active"
    (is (false? (core/within-arrayfire?)))))

(deftest within-arrayfire-inside-no-opts-test
  (testing "within-arrayfire? returns true inside with-arrayfire (no options)"
    (let [result (core/with-arrayfire
                   (core/within-arrayfire?))]
      (is (true? result)))))

(deftest within-arrayfire-inside-with-backend-test
  (testing "within-arrayfire? returns true inside with-arrayfire with backend option"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (core/within-arrayfire?))]
      (is (true? result)))))

(deftest within-arrayfire-after-region-test
  (testing "within-arrayfire? returns false after with-arrayfire exits"
    (core/with-arrayfire nil)
    (is (false? (core/within-arrayfire?)))))

(deftest within-arrayfire-nested-test
  (testing "within-arrayfire? returns true in nested with-arrayfire regions"
    (let [result (core/with-arrayfire
                   (core/with-arrayfire
                     (core/within-arrayfire?)))]
      (is (true? result)))))

(deftest assert-within-arrayfire-outside-test
  (testing "assert-within-arrayfire! throws IllegalStateException outside a region"
    (is (thrown? IllegalStateException
          (core/assert-within-arrayfire! "test-fn")))))

(deftest assert-within-arrayfire-message-test
  (testing "assert-within-arrayfire! error message includes function name"
    (is (thrown-with-msg? IllegalStateException #"test-fn"
          (core/assert-within-arrayfire! "test-fn")))))

(deftest assert-within-arrayfire-inside-test
  (testing "assert-within-arrayfire! does not throw inside a region"
    (is (nil? (core/with-arrayfire
                (core/assert-within-arrayfire! "test-fn"))))))
