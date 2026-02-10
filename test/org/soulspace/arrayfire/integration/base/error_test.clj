(ns org.soulspace.arrayfire.integration.base.error-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [org.soulspace.arrayfire.integration.base.error :refer [check!]]
            [org.soulspace.arrayfire.integration.base.jvm-integration :as jvm]
            [coffi.mem :as mem]))

;;;
;;; Error Handling Tests
;;;

(deftest test-check-success
  (testing "check! succeeds with AF_SUCCESS (0)"
    (is (nil? (check! jvm/AF_SUCCESS "test-operation")))))

(deftest test-check-error
  (testing "check! throws exception on non-zero error code"
    (is (thrown-with-msg? 
         clojure.lang.ExceptionInfo
         #"ArrayFire error at test-error"
         (check! jvm/AF_ERR_NO_MEM "test-error")))))

(deftest test-check-error-details
  (testing "check! includes error code and location in exception data"
    (try
      (check! jvm/AF_ERR_INVALID_ARRAY "test-location")
      (is false "Should have thrown exception")
      (catch clojure.lang.ExceptionInfo e
        (let [data (ex-data e)]
          (is (= jvm/AF_ERR_INVALID_ARRAY (:code data)))
          (is (= "test-location" (:where data))))))))

(comment
  ;; run tests from REPL
  (run-tests)
  ;
  )
