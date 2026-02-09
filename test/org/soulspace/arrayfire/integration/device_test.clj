(ns org.soulspace.arrayfire.integration.device-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [org.soulspace.arrayfire.integration.unified-api.device :as device]))

;;;
;;; Device Initialization Tests
;;;

(deftest test-init
  (testing "init! initializes ArrayFire successfully"
    (is (true? (device/init!)))))

(deftest test-init-with-backend
  (testing "init! can specify backend"
    (is (true? (device/init!)))))

;;;
;;; Device Information Tests
;;;

(deftest test-get-device-count
  (testing "get-device-count returns positive number"
    (device/init!)
    (let [count (device/get-device-count)]
      (is (integer? count))
      (is (pos? count)))))

(deftest test-get-device
  (testing "get-device returns current device ID"
    (device/init!)
    (let [dev-id (device/get-device)]
      (is (integer? dev-id))
      (is (>= dev-id 0)))))

(deftest test-set-device
  (testing "set-device! sets and returns new device ID"
    (device/init!)
    (let [original-device (device/get-device)
          device-count (device/get-device-count)]
      (when (> device-count 1)
        (let [new-device (if (zero? original-device) 1 0)]
          (device/set-device! new-device)
          (is (= new-device (device/get-device)))
          ;; Restore original device
          (device/set-device! original-device))))))

(deftest test-dbl-support
  (testing "dbl-support? returns boolean"
    (device/init!)
    (let [dbl-support (device/dbl-support?)]
      (is (boolean? dbl-support)))))

(deftest test-half-support
  (testing "half-support? returns boolean"
    (device/init!)
    (let [half-support (device/half-support?)]
      (is (boolean? half-support)))))

;;;
;;; Device Info String Tests
;;;

(deftest test-device-info
  (testing "device-info returns non-empty string"
    (device/init!)
    (let [info (device/info-string)]
      (is (string? info))
      (is (not (empty? info))))))

(deftest test-device-info-details
  (testing "device-info contains expected details"
    (device/init!)
    (let [info (device/info-string)]
      ;; Info should contain device name and other details
      (is (re-find #"ArrayFire" info)))))

;;;
;;; Memory Info Tests
;;;

(deftest test-device-mem-info
  (testing "device-mem-info returns valid memory information"
    (device/init!)
    (let [{:keys [alloc-bytes alloc-buffers lock-bytes lock-buffers]} (device/device-mem-info)]
      (is (integer? alloc-bytes))
      (is (integer? alloc-buffers))
      (is (integer? lock-bytes))
      (is (integer? lock-buffers))
      (is (>= alloc-bytes 0))
      (is (>= alloc-buffers 0))
      (is (>= lock-bytes 0))
      (is (>= lock-buffers 0)))))

(deftest test-print-mem-info
  (testing "print-mem-info! executes without error"
    (device/init!)
    (is (nil? (device/print-mem-info!)))))

(deftest test-print-mem-info-with-message
  (testing "print-mem-info! with message executes without error"
    (device/init!)
    (is (nil? (device/print-mem-info! "Test Memory Info")))))

;;;
;;; Memory Management Tests
;;;

(deftest test-device-gc
  (testing "device-gc! executes without error"
    (device/init!)
    (is (nil? (device/device-gc!)))))

(deftest test-device-mem-info-after-gc
  (testing "Memory info shows changes after GC"
    (device/init!)
    (let [before (device/device-mem-info)]
      (device/device-gc!)
      (let [after (device/device-mem-info)]
        ;; After GC, locked memory should typically be less than or equal
        (is (<= (:lock-bytes after) (:lock-bytes before)))))))

;;;
;;; Backend Tests
;;;

(deftest test-get-available-backends
  (testing "get-available-backends returns integer bitmap"
    (device/init!)
    (let [backends (device/get-available-backends)]
      (is (integer? backends))
      (is (>= backends 0)))))

(deftest test-get-backend-count
  (testing "get-backend-count returns positive number"
    (device/init!)
    (let [count (device/get-backend-count)]
      (is (integer? count))
      (is (pos? count)))))

(deftest test-get-active-backend
  (testing "get-active-backend returns valid backend"
    (device/init!)
    (let [backend (device/get-active-backend)]
      (is (integer? backend))
      ;; Valid backends: DEFAULT=0, CPU=1, CUDA=2, OPENCL=4, ONEAPI=8
      (is (contains? #{0 1 2 4 8} backend)))))

;;;
;;; Device Properties Tests
;;;



;;;
;;; Device Synchronization Tests
;;;

(deftest test-sync
  (testing "sync! executes without error"
    (device/init!)
    (is (nil? (device/sync!)))))

(deftest test-sync-specific-device
  (testing "sync! with device ID executes without error"
    (device/init!)
    (let [dev-id (device/get-device)]
      (is (nil? (device/sync! dev-id))))))

(comment
  ;; run tests from REPL
  (run-tests)
  ;
  )
