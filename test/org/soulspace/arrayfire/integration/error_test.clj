(ns org.soulspace.arrayfire.integration.error-test
  (:require [clojure.test :refer [deftest is testing run-test run-tests]]
            [org.soulspace.arrayfire.integration.unified-api.error :as error]
            [org.soulspace.arrayfire.integration.unified-api.device :as device]))

;;;
;;; Error Retrieval Tests
;;;

(deftest test-get-last-error-no-error
  (testing "get-last-error returns nil when no error occurred"
    (device/init!)
    (let [err (error/get-last-error)]
      ;; After successful init, there should be no error
      (is (or (nil? err) (string? err))))))

(deftest test-get-last-error-returns-string
  (testing "get-last-error returns string or nil"
    (device/init!)
    (let [err (error/get-last-error)]
      (is (or (nil? err) (string? err))))))

;;;
;;; Error Code Conversion Tests
;;;

(deftest test-err-to-string-success
  (testing "err-to-string converts AF_SUCCESS (0) to string"
    (device/init!)
    (let [msg (error/err-to-string 0)]
      (is (string? msg))
      (is (not (empty? msg)))
      ;; Should contain "Success" or similar
      (is (re-find #"(?i)success" msg)))))

(deftest test-err-to-string-no-mem
  (testing "err-to-string converts AF_ERR_NO_MEM (101) to string"
    (device/init!)
    (let [msg (error/err-to-string 101)]
      (is (string? msg))
      (is (not (empty? msg)))
      ;; Should mention memory
      (is (re-find #"(?i)memory" msg)))))

(deftest test-err-to-string-driver
  (testing "err-to-string converts AF_ERR_DRIVER (102) to string"
    (device/init!)
    (let [msg (error/err-to-string 102)]
      (is (string? msg))
      (is (not (empty? msg))))))

(deftest test-err-to-string-runtime
  (testing "err-to-string converts AF_ERR_RUNTIME (103) to string"
    (device/init!)
    (let [msg (error/err-to-string 103)]
      (is (string? msg))
      (is (not (empty? msg))))))

(deftest test-err-to-string-invalid-array
  (testing "err-to-string converts AF_ERR_INVALID_ARRAY (201) to string"
    (device/init!)
    (let [msg (error/err-to-string 201)]
      (is (string? msg))
      (is (not (empty? msg)))
      ;; Should mention array
      (is (re-find #"(?i)array" msg)))))

(deftest test-err-to-string-arg
  (testing "err-to-string converts AF_ERR_ARG (202) to string"
    (device/init!)
    (let [msg (error/err-to-string 202)]
      (is (string? msg))
      (is (not (empty? msg)))
      ;; Should mention argument
      (is (re-find #"(?i)arg" msg)))))

(deftest test-err-to-string-size
  (testing "err-to-string converts AF_ERR_SIZE (203) to string"
    (device/init!)
    (let [msg (error/err-to-string 203)]
      (is (string? msg))
      (is (not (empty? msg))))))

(deftest test-err-to-string-type
  (testing "err-to-string converts AF_ERR_TYPE (204) to string"
    (device/init!)
    (let [msg (error/err-to-string 204)]
      (is (string? msg))
      (is (not (empty? msg)))
      ;; Should mention type
      (is (re-find #"(?i)type" msg)))))

(deftest test-err-to-string-diff-type
  (testing "err-to-string converts AF_ERR_DIFF_TYPE (205) to string"
    (device/init!)
    (let [msg (error/err-to-string 205)]
      (is (string? msg))
      (is (not (empty? msg))))))

(deftest test-err-to-string-not-supported
  (testing "err-to-string converts AF_ERR_NOT_SUPPORTED (301) to string"
    (device/init!)
    (let [msg (error/err-to-string 301)]
      (is (string? msg))
      (is (not (empty? msg)))
      ;; Should mention support
      (is (re-find #"(?i)support" msg)))))

(deftest test-err-to-string-no-dbl
  (testing "err-to-string converts AF_ERR_NO_DBL (401) to string"
    (device/init!)
    (let [msg (error/err-to-string 401)]
      (is (string? msg))
      (is (not (empty? msg)))
      ;; Should mention double
      (is (re-find #"(?i)double|precision" msg)))))

(deftest test-err-to-string-internal
  (testing "err-to-string converts AF_ERR_INTERNAL (998) to string"
    (device/init!)
    (let [msg (error/err-to-string 998)]
      (is (string? msg))
      (is (not (empty? msg)))
      ;; Should mention internal
      (is (re-find #"(?i)internal" msg)))))

(deftest test-err-to-string-unknown
  (testing "err-to-string converts AF_ERR_UNKNOWN (999) to string"
    (device/init!)
    (let [msg (error/err-to-string 999)]
      (is (string? msg))
      (is (not (empty? msg)))
      ;; Should mention unknown
      (is (re-find #"(?i)unknown" msg)))))

;;;
;;; Stack Trace Control Tests
;;;

(deftest test-set-enable-stacktrace-true
  (testing "set-enable-stacktrace! enables stack traces"
    (device/init!)
    (is (nil? (error/set-enable-stacktrace! true)))))

(deftest test-set-enable-stacktrace-false
  (testing "set-enable-stacktrace! disables stack traces"
    (device/init!)
    (is (nil? (error/set-enable-stacktrace! false)))))

(deftest test-set-enable-stacktrace-toggle
  (testing "set-enable-stacktrace! can toggle multiple times"
    (device/init!)
    (is (nil? (error/set-enable-stacktrace! true)))
    (is (nil? (error/set-enable-stacktrace! false)))
    (is (nil? (error/set-enable-stacktrace! true)))
    ;; Restore default
    (error/set-enable-stacktrace! true)))

(comment
  ;; run all tests from REPL
  (run-tests)
  
  ;; run individual tests
  (run-test test-get-last-error-no-error)
  (run-test test-get-last-error-returns-string)
  (run-test test-err-to-string-success)
  (run-test test-err-to-string-no-mem)
  (run-test test-err-to-string-driver)
  (run-test test-err-to-string-runtime)
  (run-test test-err-to-string-invalid-array)
  (run-test test-err-to-string-arg)
  (run-test test-err-to-string-size)
  (run-test test-err-to-string-type)
  (run-test test-err-to-string-diff-type)
  (run-test test-err-to-string-not-supported)
  (run-test test-err-to-string-no-dbl)
  (run-test test-err-to-string-internal)
  (run-test test-err-to-string-unknown)
  (run-test test-set-enable-stacktrace-true)
  (run-test test-set-enable-stacktrace-false)
  (run-test test-set-enable-stacktrace-toggle)
  
  ;
  )
