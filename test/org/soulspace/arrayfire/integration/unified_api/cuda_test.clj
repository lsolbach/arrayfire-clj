(ns org.soulspace.arrayfire.integration.unified-api.cuda-test
  (:require [clojure.test :refer [deftest is testing run-test run-tests]]
            [org.soulspace.arrayfire.integration.unified-api.cuda :as cuda]
            [org.soulspace.arrayfire.integration.unified-api.device :as device]))

;;;
;;; Helper Functions
;;;

(defn cuda-backend?
  "Check if CUDA backend is active."
  []
  (try
    (device/init!)
    (= device/AF_BACKEND_CUDA (device/get-active-backend))
    (catch Exception _e
      false)))

(defn skip-if-not-cuda
  "Skip test if CUDA backend is not available."
  []
  (when-not (cuda-backend?)
    (throw (ex-info "Test skipped: CUDA backend not available" {:skip true}))))

;;;
;;; CUDA Stream Management Tests
;;;

(deftest test-get-stream
  (testing "get-stream returns CUDA stream for device"
    (try
      (skip-if-not-cuda)
      (let [stream (cuda/get-stream 0)]
        (is (integer? stream))
        (is (>= stream 0)))
      (catch clojure.lang.ExceptionInfo e
        (when-not (:skip (ex-data e))
          (throw e))))))

(deftest test-get-stream-current-device
  (testing "get-stream for current device"
    (try
      (skip-if-not-cuda)
      (let [dev-id (device/get-device)
            stream (cuda/get-stream dev-id)]
        (is (integer? stream)))
      (catch clojure.lang.ExceptionInfo e
        (when-not (:skip (ex-data e))
          (throw e))))))

(deftest test-get-native-id
  (testing "get-native-id returns native CUDA device id"
    (try
      (skip-if-not-cuda)
      (let [native-id (cuda/get-native-id 0)]
        (is (integer? native-id))
        (is (>= native-id 0)))
      (catch clojure.lang.ExceptionInfo e
        (when-not (:skip (ex-data e))
          (throw e))))))

(deftest test-get-native-id-current-device
  (testing "get-native-id for current device"
    (try
      (skip-if-not-cuda)
      (let [af-id (device/get-device)
            native-id (cuda/get-native-id af-id)]
        (is (integer? native-id))
        (is (>= native-id 0)))
      (catch clojure.lang.ExceptionInfo e
        (when-not (:skip (ex-data e))
          (throw e))))))

(deftest test-set-native-id
  (testing "set-native-id! sets active device by native id"
    (try
      (skip-if-not-cuda)
      (let [original-device (device/get-device)
            native-id (cuda/get-native-id original-device)]
        (cuda/set-native-id! native-id)
        (is (= original-device (device/get-device))))
      (catch clojure.lang.ExceptionInfo e
        (when-not (:skip (ex-data e))
          (throw e))))))

(deftest test-set-native-id-switch-device
  (testing "set-native-id! can switch between devices"
    (try
      (skip-if-not-cuda)
      (let [device-count (device/get-device-count)]
        (when (> device-count 1)
          (let [native-id-0 (cuda/get-native-id 0)
                native-id-1 (cuda/get-native-id 1)]
            (cuda/set-native-id! native-id-1)
            (is (= 1 (device/get-device)))
            ;; Restore original device
            (cuda/set-native-id! native-id-0)
            (is (= 0 (device/get-device))))))
      (catch clojure.lang.ExceptionInfo e
        (when-not (:skip (ex-data e))
          (throw e))))))

;;;
;;; cuBLAS Configuration Tests
;;;

(deftest test-cublas-set-math-mode-default
  (testing "cublas-set-math-mode! sets default math mode"
    (try
      (skip-if-not-cuda)
      (is (nil? (cuda/cublas-set-math-mode! cuda/CUBLAS_DEFAULT_MATH)))
      (catch clojure.lang.ExceptionInfo e
        (when-not (:skip (ex-data e))
          (throw e))))))

(deftest test-cublas-set-math-mode-tensor
  (testing "cublas-set-math-mode! sets tensor op math mode"
    (try
      (skip-if-not-cuda)
      (is (nil? (cuda/cublas-set-math-mode! cuda/CUBLAS_TENSOR_OP_MATH)))
      ;; Restore default mode
      (cuda/cublas-set-math-mode! cuda/CUBLAS_DEFAULT_MATH)
      (catch clojure.lang.ExceptionInfo e
        (when-not (:skip (ex-data e))
          (throw e))))))

(deftest test-cublas-constants
  (testing "cuBLAS constants have correct values"
    (is (= 0 cuda/CUBLAS_DEFAULT_MATH))
    (is (= 1 cuda/CUBLAS_TENSOR_OP_MATH))))

;;;
;;; Integration Tests
;;;

(deftest test-stream-native-id-consistency
  (testing "stream and native-id operations are consistent"
    (try
      (skip-if-not-cuda)
      (let [af-id 0
            stream (cuda/get-stream af-id)
            native-id (cuda/get-native-id af-id)]
        ;; Both should return valid non-negative values
        (is (>= stream 0))
        (is (>= native-id 0))
        ;; Setting native-id should work
        (cuda/set-native-id! native-id)
        (is (= af-id (device/get-device))))
      (catch clojure.lang.ExceptionInfo e
        (when-not (:skip (ex-data e))
          (throw e))))))

(deftest test-multiple-devices-native-ids
  (testing "native-id mapping for multiple devices"
    (try
      (skip-if-not-cuda)
      (let [device-count (device/get-device-count)]
        (when (> device-count 1)
          (let [native-ids (mapv cuda/get-native-id (range device-count))]
            ;; All native ids should be distinct
            (is (= (count native-ids) (count (set native-ids))))
            ;; All should be non-negative
            (is (every? #(>= % 0) native-ids)))))
      (catch clojure.lang.ExceptionInfo e
        (when-not (:skip (ex-data e))
          (throw e))))))

(deftest test-stream-per-device
  (testing "each device has its own stream"
    (try
      (skip-if-not-cuda)
      (let [device-count (device/get-device-count)]
        (when (> device-count 1)
          (let [streams (mapv cuda/get-stream (range device-count))]
            ;; Streams should exist for all devices
            (is (= (count streams) device-count))
            ;; All should be valid integers
            (is (every? integer? streams)))))
      (catch clojure.lang.ExceptionInfo e
        (when-not (:skip (ex-data e))
          (throw e))))))

(comment
  ;; run all tests from REPL
  (run-tests)
  
  ;; run individual tests
  (run-test test-get-stream)
  (run-test test-get-stream-current-device)
  (run-test test-get-native-id)
  (run-test test-get-native-id-current-device)
  (run-test test-set-native-id)
  (run-test test-set-native-id-switch-device)
  (run-test test-cublas-set-math-mode-default)
  (run-test test-cublas-set-math-mode-tensor)
  (run-test test-cublas-constants)
  (run-test test-stream-native-id-consistency)
  (run-test test-multiple-devices-native-ids)
  (run-test test-stream-per-device)
  
  ;
  )
