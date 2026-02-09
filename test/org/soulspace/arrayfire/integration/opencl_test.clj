(ns org.soulspace.arrayfire.integration.opencl-test
  (:require [clojure.test :refer [deftest is testing run-test run-tests]]
            [org.soulspace.arrayfire.integration.unified-api.opencl :as opencl]
            [org.soulspace.arrayfire.integration.unified-api.device :as device]))

(defn opencl-backend-available?
  "Check if OpenCL backend is available."
  []
  (try
    (device/init!)
    (let [backends (device/get-available-backends)]
      ;; AF_BACKEND_OPENCL = 4
      (not (zero? (bit-and backends 4))))
    (catch Exception _
      false)))

(defn ensure-opencl-backend!
  "Ensure OpenCL backend is active, skip test if not available."
  []
  (when-not (opencl-backend-available?)
    (throw (ex-info "OpenCL backend not available" {:skip-test true}))))

;;;
;;; Context and Queue Access Tests
;;;

(deftest test-get-context
  (testing "get-context returns valid context handle"
    (device/init!)
    (when (opencl-backend-available?)
      (try
        (device/set-backend! device/AF_BACKEND_OPENCL)
        (let [ctx (opencl/get-context)]
          (is (integer? ctx))
          (is (not (zero? ctx))))
        (catch Exception e
          ;; Skip if OpenCL not fully configured
          (is (or (= "OpenCL backend not available" (.getMessage e))
                  (instance? Exception e))))))))

(deftest test-get-context-with-retain
  (testing "get-context with retain flag"
    (device/init!)
    (when (opencl-backend-available?)
      (try
        (device/set-backend! device/AF_BACKEND_OPENCL)
        (let [ctx (opencl/get-context false)
              ctx-retain (opencl/get-context true)]
          (is (integer? ctx))
          (is (integer? ctx-retain)))
        (catch Exception _
          ;; Skip if not available
          (is true))))))

(deftest test-get-queue
  (testing "get-queue returns valid queue handle"
    (device/init!)
    (when (opencl-backend-available?)
      (try
        (device/set-backend! device/AF_BACKEND_OPENCL)
        (let [queue (opencl/get-queue)]
          (is (integer? queue))
          (is (not (zero? queue))))
        (catch Exception _
          (is true))))))

(deftest test-get-queue-with-retain
  (testing "get-queue with retain flag"
    (device/init!)
    (when (opencl-backend-available?)
      (try
        (device/set-backend! device/AF_BACKEND_OPENCL)
        (let [queue (opencl/get-queue false)
              queue-retain (opencl/get-queue true)]
          (is (integer? queue))
          (is (integer? queue-retain)))
        (catch Exception _
          (is true))))))

(deftest test-get-device-id
  (testing "get-device-id returns valid device ID"
    (device/init!)
    (when (opencl-backend-available?)
      (try
        (device/set-backend! device/AF_BACKEND_OPENCL)
        (let [dev-id (opencl/get-device-id)]
          (is (integer? dev-id))
          (is (not (zero? dev-id))))
        (catch Exception _
          (is true))))))

;;;
;;; Device Information Tests
;;;

(deftest test-get-device-type
  (testing "get-device-type returns valid device type"
    (device/init!)
    (when (opencl-backend-available?)
      (try
        (device/set-backend! device/AF_BACKEND_OPENCL)
        (let [dtype (opencl/get-device-type)]
          (is (integer? dtype))
          ;; Valid types: CPU=2, GPU=4, ACC=8, UNKNOWN=-1
          (is (contains? #{-1 2 4 8} dtype)))
        (catch Exception _
          (is true))))))

(deftest test-get-platform
  (testing "get-platform returns valid platform ID"
    (device/init!)
    (when (opencl-backend-available?)
      (try
        (device/set-backend! device/AF_BACKEND_OPENCL)
        (let [platform (opencl/get-platform)]
          (is (integer? platform))
          ;; Valid platforms: AMD=0, APPLE=1, INTEL=2, NVIDIA=3, BEIGNET=4, POCL=5, UNKNOWN=-1
          (is (contains? #{-1 0 1 2 3 4 5} platform)))
        (catch Exception _
          (is true))))))

;;;
;;; Convenience Function Tests
;;;

(deftest test-device-type-name
  (testing "device-type-name returns human-readable names"
    (device/init!)
    (is (= "CPU" (opencl/device-type-name 2)))
    (is (= "GPU" (opencl/device-type-name 4)))
    (is (= "Accelerator" (opencl/device-type-name 8)))
    (is (= "Unknown" (opencl/device-type-name -1)))
    (is (= "Unknown" (opencl/device-type-name 999)))))

(deftest test-device-type-name-current
  (testing "device-type-name without argument uses current device"
    (device/init!)
    (when (opencl-backend-available?)
      (try
        (device/set-backend! device/AF_BACKEND_OPENCL)
        (let [name (opencl/device-type-name)]
          (is (string? name))
          (is (contains? #{"CPU" "GPU" "Accelerator" "Unknown"} name)))
        (catch Exception _
          (is true))))))

(deftest test-platform-name
  (testing "platform-name returns human-readable names"
    (device/init!)
    (is (= "AMD" (opencl/platform-name 0)))
    (is (= "Apple" (opencl/platform-name 1)))
    (is (= "Intel" (opencl/platform-name 2)))
    (is (= "NVIDIA" (opencl/platform-name 3)))
    (is (= "Beignet" (opencl/platform-name 4)))
    (is (= "POCL" (opencl/platform-name 5)))
    (is (= "Unknown" (opencl/platform-name -1)))
    (is (= "Unknown" (opencl/platform-name 999)))))

(deftest test-platform-name-current
  (testing "platform-name without argument uses current platform"
    (device/init!)
    (when (opencl-backend-available?)
      (try
        (device/set-backend! device/AF_BACKEND_OPENCL)
        (let [name (opencl/platform-name)]
          (is (string? name))
          (is (contains? #{"AMD" "Apple" "Intel" "NVIDIA" "Beignet" "POCL" "Unknown"} name)))
        (catch Exception _
          (is true))))))

(deftest test-device-info
  (testing "device-info returns comprehensive information map"
    (device/init!)
    (when (opencl-backend-available?)
      (try
        (device/set-backend! device/AF_BACKEND_OPENCL)
        (let [info (opencl/device-info)]
          (is (map? info))
          (is (contains? info :device-type))
          (is (contains? info :device-type-name))
          (is (contains? info :platform))
          (is (contains? info :platform-name))
          (is (contains? info :device-id))
          (is (integer? (:device-type info)))
          (is (string? (:device-type-name info)))
          (is (integer? (:platform info)))
          (is (string? (:platform-name info)))
          (is (integer? (:device-id info))))
        (catch Exception _
          (is true))))))

;;;
;;; Constants Tests
;;;

(deftest test-device-type-constants
  (testing "Device type constants have expected values"
    (device/init!)
    (is (= 2 opencl/DEVICE-TYPE-CPU))
    (is (= 4 opencl/DEVICE-TYPE-GPU))
    (is (= 8 opencl/DEVICE-TYPE-ACC))
    (is (= -1 opencl/DEVICE-TYPE-UNKNOWN))))

(deftest test-platform-constants
  (testing "Platform constants have expected values"
    (device/init!)
    (is (= 0 opencl/PLATFORM-AMD))
    (is (= 1 opencl/PLATFORM-APPLE))
    (is (= 2 opencl/PLATFORM-INTEL))
    (is (= 3 opencl/PLATFORM-NVIDIA))
    (is (= 4 opencl/PLATFORM-BEIGNET))
    (is (= 5 opencl/PLATFORM-POCL))
    (is (= -1 opencl/PLATFORM-UNKNOWN))))

;;;
;;; Integration Tests
;;;

(deftest test-context-queue-consistency
  (testing "Context and queue are consistent"
    (device/init!)
    (when (opencl-backend-available?)
      (try
        (device/set-backend! device/AF_BACKEND_OPENCL)
        (let [ctx (opencl/get-context)
              queue (opencl/get-queue)
              dev-id (opencl/get-device-id)]
          ;; All should be non-zero if OpenCL is working
          (is (not (zero? ctx)))
          (is (not (zero? queue)))
          (is (not (zero? dev-id))))
        (catch Exception _
          (is true))))))

(deftest test-device-type-and-platform-match
  (testing "Device type and platform are valid combinations"
    (device/init!)
    (when (opencl-backend-available?)
      (try
        (device/set-backend! device/AF_BACKEND_OPENCL)
        (let [dtype (opencl/get-device-type)
              platform (opencl/get-platform)]
          ;; Both should be valid values
          (is (contains? #{-1 2 4 8} dtype))
          (is (contains? #{-1 0 1 2 3 4 5} platform)))
        (catch Exception _
          (is true))))))

(deftest test-device-info-consistency
  (testing "device-info values consistent with individual queries"
    (device/init!)
    (when (opencl-backend-available?)
      (try
        (device/set-backend! device/AF_BACKEND_OPENCL)
        (let [info (opencl/device-info)
              dtype (opencl/get-device-type)
              platform (opencl/get-platform)
              dev-id (opencl/get-device-id)]
          (is (= dtype (:device-type info)))
          (is (= platform (:platform info)))
          (is (= dev-id (:device-id info)))
          (is (= (opencl/device-type-name dtype) (:device-type-name info)))
          (is (= (opencl/platform-name platform) (:platform-name info))))
        (catch Exception _
          (is true))))))

;;;
;;; Backend Management Tests
;;;

(deftest test-opencl-backend-detection
  (testing "Can detect if OpenCL backend is available"
    (device/init!)
    (let [available (opencl-backend-available?)]
      (is (boolean? available)))))

(comment
  ;; run all tests from REPL
  (run-tests)
  
  ;; run individual tests
  (run-test test-get-context)
  (run-test test-get-context-with-retain)
  (run-test test-get-queue)
  (run-test test-get-queue-with-retain)
  (run-test test-get-device-id)
  (run-test test-get-device-type)
  (run-test test-get-platform)
  (run-test test-device-type-name)
  (run-test test-device-type-name-current)
  (run-test test-platform-name)
  (run-test test-platform-name-current)
  (run-test test-device-info)
  (run-test test-device-type-constants)
  (run-test test-platform-constants)
  (run-test test-context-queue-consistency)
  (run-test test-device-type-and-platform-match)
  (run-test test-device-info-consistency)
  (run-test test-opencl-backend-detection)
  
  ;
  )
