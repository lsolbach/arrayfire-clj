(ns org.soulspace.arrayfire.integration.memory-test
  (:require [clojure.test :refer [deftest is testing]]
            [org.soulspace.arrayfire.integration.memory :as memory]
            [org.soulspace.arrayfire.integration.array :as array]
            [org.soulspace.arrayfire.integration.device :as device]
            [org.soulspace.arrayfire.integration.jvm-integration :as jvm]
            [coffi.mem :as mem])
  (:import [org.soulspace.arrayfire.integration.jvm_integration AFArray]))

;;;
;;; Pinned Memory Tests
;;;

(deftest test-alloc-free-pinned
  (testing "alloc-pinned and free-pinned! work correctly"
    (device/init!)
    (let [bytes (* 1024 4) ; 4KB
          pinned (memory/alloc-pinned bytes)]
      (is (not (nil? pinned)))
      (is (instance? java.lang.foreign.MemorySegment pinned))
      (memory/free-pinned! pinned))))

(deftest test-pinned-memory-usage
  (testing "Can write and read data from pinned memory"
    (device/init!)
    (let [bytes (* 4 4) ; 4 floats
          pinned (memory/alloc-pinned bytes)]
      (try
        ;; Write some float values
        (mem/write-float pinned 0 1.0)
        (mem/write-float pinned 4 2.0)
        (mem/write-float pinned 8 3.0)
        (mem/write-float pinned 12 4.0)
        
        ;; Read them back
        (is (<= (Math/abs (- 1.0 (mem/read-float pinned 0))) 0.001))
        (is (<= (Math/abs (- 2.0 (mem/read-float pinned 4))) 0.001))
        (is (<= (Math/abs (- 3.0 (mem/read-float pinned 8))) 0.001))
        (is (<= (Math/abs (- 4.0 (mem/read-float pinned 12))) 0.001))
        (finally
          (memory/free-pinned! pinned))))))

;;;
;;; Host Memory Tests
;;;

(deftest test-alloc-free-host
  (testing "alloc-host and free-host! work correctly"
    (device/init!)
    (let [bytes (* 1024 4) ; 4KB
          host-mem (memory/alloc-host bytes)]
      (is (not (nil? host-mem)))
      (is (instance? java.lang.foreign.MemorySegment host-mem))
      (memory/free-host! host-mem))))

(deftest test-host-memory-usage
  (testing "Can write and read data from host memory"
    (device/init!)
    (let [bytes (* 4 4) ; 4 floats
          host-mem (memory/alloc-host bytes)]
      (try
        ;; Write some float values
        (mem/write-float host-mem 0 5.0)
        (mem/write-float host-mem 4 6.0)
        
        ;; Read them back
        (is (<= (Math/abs (- 5.0 (mem/read-float host-mem 0))) 0.001))
        (is (<= (Math/abs (- 6.0 (mem/read-float host-mem 4))) 0.001))
        (finally
          (memory/free-host! host-mem))))))

;;;
;;; Device Memory Tests
;;;

(deftest test-alloc-free-device
  (testing "alloc-device and free-device! work correctly"
    (device/init!)
    (let [bytes (* 1024 4) ; 4KB
          dev-mem (memory/alloc-device bytes)]
      (is (not (nil? dev-mem)))
      (is (instance? java.lang.foreign.MemorySegment dev-mem))
      (memory/free-device! dev-mem))))

;;;
;;; Array Locking Tests
;;;

(deftest test-lock-unlock-array
  (testing "lock-array! and unlock-array! work correctly"
    (device/init!)
    (let [arr (array/create-array (float-array [1.0 2.0 3.0]) [3] jvm/AF_DTYPE_F32)]
      (try
        ;; Lock the array
        (is (nil? (memory/lock-array! arr)))
        ;; Check if locked
        (is (memory/is-locked-array? arr))
        ;; Unlock the array
        (memory/unlock-array! arr)
        (is (not (memory/is-locked-array? arr)))
        (finally
          (.close arr))))))

(deftest test-lock-array-data-access
  (testing "Can access array data through locked pointer"
    (device/init!)
    (let [data (float-array [1.0 2.0 3.0])
          arr (array/create-array data [3] jvm/AF_DTYPE_F32)]
      (try
        (let [ptr (memory/get-device-ptr arr)]
          (is (instance? java.lang.foreign.MemorySegment ptr))
          (when (= device/AF_BACKEND_CPU (device/get-active-backend))
            (let [host-view (mem/reinterpret ptr 12)]
              (is (<= (Math/abs (- 1.0 (mem/read-float host-view 0))) 0.001))
              (is (<= (Math/abs (- 2.0 (mem/read-float host-view 4))) 0.001))
              (is (<= (Math/abs (- 3.0 (mem/read-float host-view 8))) 0.001))))
          (memory/unlock-array! arr))
        (finally
          (.close arr))))))

;;;
;;; Device Memory Info Tests
;;;

(deftest test-get-mem-step-size
  (testing "get-mem-step-size returns valid size"
    (device/init!)
    (let [step-size (memory/get-mem-step-size)]
      (is (integer? step-size))
      (is (pos? step-size)))))

(deftest test-set-mem-step-size
  (testing "set-mem-step-size! changes memory step size"
    (device/init!)
    (let [original-size (memory/get-mem-step-size)
          new-size (* 1024 1024)] ; 1MB
      (try
        (memory/set-mem-step-size! new-size)
        (is (= new-size (memory/get-mem-step-size)))
        (finally
          ;; Restore original
          (memory/set-mem-step-size! original-size))))))

;;;
;;; Device Pointer Tests
;;;

(deftest test-get-device-ptr
  (testing "get-device-ptr returns device pointer for array"
    (device/init!)
    (let [arr (array/create-array (float-array [1.0 2.0 3.0]) [3] jvm/AF_DTYPE_F32)]
      (try
        (let [ptr (memory/get-device-ptr arr)]
          (is (not (nil? ptr)))
          (is (instance? java.lang.foreign.MemorySegment ptr)))
        (finally
          (.close arr))))))

;;;
;;; Memory Manager Tests
;;;

(deftest test-device-gc
  (testing "device-gc triggers garbage collection"
    (device/init!)
    (memory/device-gc)
    (let [after (device/device-mem-info)]
      ;; Memory info should be accessible after GC
      (is (integer? (:alloc-bytes after)))
      (is (integer? (:lock-bytes after))))))

(deftest test-memory-lifecycle
  (testing "Memory can be allocated and freed multiple times"
    (device/init!)
    (dotimes [_ 10]
      (let [pinned (memory/alloc-pinned 1024)
            host (memory/alloc-host 1024)
            device (memory/alloc-device 1024)]
        (memory/free-pinned! pinned)
        (memory/free-host! host)
        (memory/free-device! device)))
    ;; Should complete without errors
    (is true)))

(deftest test-array-locking-lifecycle
  (testing "Arrays can be locked and unlocked multiple times"
    (device/init!)
    (let [arr (array/create-array (float-array [1.0 2.0 3.0]) [3] jvm/AF_DTYPE_F32)]
      (try
        (dotimes [_ 5]
          (let [_ptr (memory/lock-array! arr)]
            (is (memory/is-locked-array? arr))
            (memory/unlock-array! arr)
            (is (not (memory/is-locked-array? arr)))))
        (finally
          (.close arr))))))

(comment
  ;; To run tests from REPL
  (clojure.test/run-tests)
  )
