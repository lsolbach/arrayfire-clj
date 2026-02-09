(ns org.soulspace.arrayfire.integration.jit-test-api-test
  (:require [clojure.test :refer [deftest is testing run-test run-tests]]
            [org.soulspace.arrayfire.integration.unified-api.jit-test-api :as jit]
            [org.soulspace.arrayfire.integration.unified-api.device :as device]))

;;;
;;; JIT Control Functions Tests
;;;

(deftest test-get-max-jit-len
  (testing "get-max-jit-len returns integer JIT depth"
    (device/init!)
    (let [len (jit/get-max-jit-len)]
      (is (integer? len))
      (is (pos? len)))))

(deftest test-get-max-jit-len-default
  (testing "get-max-jit-len returns default value (typically 20)"
    (device/init!)
    (let [len (jit/get-max-jit-len)]
      ;; Default is 20 for most backends
      (is (>= len 1))
      (is (<= len 100)))))

(deftest test-set-max-jit-len
  (testing "set-max-jit-len! changes JIT depth"
    (device/init!)
    (let [original (jit/get-max-jit-len)]
      (try
        (jit/set-max-jit-len! 10)
        (is (= 10 (jit/get-max-jit-len)))
        (finally
          (jit/set-max-jit-len! original))))))

(deftest test-set-max-jit-len-various-values
  (testing "set-max-jit-len! accepts various valid depths"
    (device/init!)
    (let [original (jit/get-max-jit-len)]
      (try
        (doseq [depth [1 5 10 20 50 100]]
          (jit/set-max-jit-len! depth)
          (is (= depth (jit/get-max-jit-len))))
        (finally
          (jit/set-max-jit-len! original))))))

(deftest test-set-max-jit-len-restore
  (testing "set-max-jit-len! can restore original value"
    (device/init!)
    (let [original (jit/get-max-jit-len)]
      (jit/set-max-jit-len! 5)
      (jit/set-max-jit-len! original)
      (is (= original (jit/get-max-jit-len))))))

(deftest test-set-max-jit-len-invalid
  (testing "set-max-jit-len! rejects invalid values"
    (device/init!)
    (is (thrown? Exception (jit/set-max-jit-len! 0)))
    (is (thrown? Exception (jit/set-max-jit-len! -1)))))

;;;
;;; Convenience Functions Tests
;;;

(deftest test-with-jit-len-function
  (testing "with-jit-len temporarily changes JIT depth"
    (device/init!)
    (let [original (jit/get-max-jit-len)
          result (jit/with-jit-len 5
                   (fn []
                     (is (= 5 (jit/get-max-jit-len)))
                     :test-result))]
      (is (= :test-result result))
      (is (= original (jit/get-max-jit-len))))))

(deftest test-with-jit-len-restores-on-exception
  (testing "with-jit-len restores original depth even on exception"
    (device/init!)
    (let [original (jit/get-max-jit-len)]
      (try
        (jit/with-jit-len 10
          (fn []
            (throw (ex-info "Test error" {}))))
        (catch Exception _))
      (is (= original (jit/get-max-jit-len))))))

(deftest test-with-jit-depth-macro
  (testing "with-jit-depth macro temporarily changes JIT depth"
    (device/init!)
    (let [original (jit/get-max-jit-len)]
      (jit/with-jit-depth 7
        (is (= 7 (jit/get-max-jit-len))))
      (is (= original (jit/get-max-jit-len))))))

(deftest test-with-jit-depth-returns-value
  (testing "with-jit-depth macro returns body value"
    (device/init!)
    (let [result (jit/with-jit-depth 3
                   (+ 1 2 3))]
      (is (= 6 result)))))

(deftest test-with-jit-depth-nested
  (testing "with-jit-depth can be nested"
    (device/init!)
    (let [original (jit/get-max-jit-len)]
      (jit/with-jit-depth 10
        (is (= 10 (jit/get-max-jit-len)))
        (jit/with-jit-depth 5
          (is (= 5 (jit/get-max-jit-len))))
        (is (= 10 (jit/get-max-jit-len))))
      (is (= original (jit/get-max-jit-len))))))

(deftest test-jit-info-structure
  (testing "jit-info returns complete information map"
    (device/init!)
    (let [info (jit/jit-info)]
      (is (map? info))
      (is (contains? info :max-jit-len))
      (is (contains? info :fusion-level))
      (is (contains? info :recommendation)))))

(deftest test-jit-info-max-jit-len
  (testing "jit-info :max-jit-len matches get-max-jit-len"
    (device/init!)
    (let [info (jit/jit-info)
          direct (jit/get-max-jit-len)]
      (is (= direct (:max-jit-len info))))))

(deftest test-jit-info-fusion-level
  (testing "jit-info :fusion-level is valid keyword"
    (device/init!)
    (let [info (jit/jit-info)
          level (:fusion-level info)]
      (is (keyword? level))
      (is (contains? #{:none :conservative :default :aggressive} level)))))

(deftest test-jit-info-recommendation
  (testing "jit-info :recommendation is non-empty string"
    (device/init!)
    (let [info (jit/jit-info)
          rec (:recommendation info)]
      (is (string? rec))
      (is (not (empty? rec))))))

(deftest test-jit-info-fusion-none
  (testing "jit-info detects :none fusion level"
    (device/init!)
    (let [original (jit/get-max-jit-len)]
      (try
        (jit/set-max-jit-len! 1)
        (let [info (jit/jit-info)]
          (is (= :none (:fusion-level info))))
        (finally
          (jit/set-max-jit-len! original))))))

(deftest test-jit-info-fusion-conservative
  (testing "jit-info detects :conservative fusion level"
    (device/init!)
    (let [original (jit/get-max-jit-len)]
      (try
        (jit/set-max-jit-len! 5)
        (let [info (jit/jit-info)]
          (is (= :conservative (:fusion-level info))))
        (finally
          (jit/set-max-jit-len! original))))))

(deftest test-jit-info-fusion-default
  (testing "jit-info detects :default fusion level"
    (device/init!)
    (let [original (jit/get-max-jit-len)]
      (try
        (jit/set-max-jit-len! 20)
        (let [info (jit/jit-info)]
          (is (= :default (:fusion-level info))))
        (finally
          (jit/set-max-jit-len! original))))))

(deftest test-jit-info-fusion-aggressive
  (testing "jit-info detects :aggressive fusion level"
    (device/init!)
    (let [original (jit/get-max-jit-len)]
      (try
        (jit/set-max-jit-len! 100)
        (let [info (jit/jit-info)]
          (is (= :aggressive (:fusion-level info))))
        (finally
          (jit/set-max-jit-len! original))))))

;;;
;;; JIT Behavior Tests
;;;

(deftest test-jit-changes-persist
  (testing "JIT length changes persist across operations"
    (device/init!)
    (let [original (jit/get-max-jit-len)]
      (try
        (jit/set-max-jit-len! 15)
        (is (= 15 (jit/get-max-jit-len)))
        ;; Do some work
        (Thread/sleep 10)
        (is (= 15 (jit/get-max-jit-len)))
        (finally
          (jit/set-max-jit-len! original))))))

(deftest test-multiple-jit-modifications
  (testing "JIT length can be modified multiple times"
    (device/init!)
    (let [original (jit/get-max-jit-len)]
      (try
        (jit/set-max-jit-len! 5)
        (is (= 5 (jit/get-max-jit-len)))
        (jit/set-max-jit-len! 10)
        (is (= 10 (jit/get-max-jit-len)))
        (jit/set-max-jit-len! 20)
        (is (= 20 (jit/get-max-jit-len)))
        (finally
          (jit/set-max-jit-len! original))))))

(comment
  ;; run all tests from REPL
  (run-tests)
  
  ;; run individual tests
  (run-test test-get-max-jit-len)
  (run-test test-get-max-jit-len-default)
  (run-test test-set-max-jit-len)
  (run-test test-set-max-jit-len-various-values)
  (run-test test-set-max-jit-len-restore)
  (run-test test-set-max-jit-len-invalid)
  (run-test test-with-jit-len-function)
  (run-test test-with-jit-len-restores-on-exception)
  (run-test test-with-jit-depth-macro)
  (run-test test-with-jit-depth-returns-value)
  (run-test test-with-jit-depth-nested)
  (run-test test-jit-info-structure)
  (run-test test-jit-info-max-jit-len)
  (run-test test-jit-info-fusion-level)
  (run-test test-jit-info-recommendation)
  (run-test test-jit-info-fusion-none)
  (run-test test-jit-info-fusion-conservative)
  (run-test test-jit-info-fusion-default)
  (run-test test-jit-info-fusion-aggressive)
  (run-test test-jit-changes-persist)
  (run-test test-multiple-jit-modifications)
  
  ;
  )
