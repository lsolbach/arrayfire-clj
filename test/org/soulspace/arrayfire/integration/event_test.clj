(ns org.soulspace.arrayfire.integration.event-test
  (:require [clojure.test :refer [deftest is testing run-test run-tests]]
            [org.soulspace.arrayfire.integration.event :as event]
            [org.soulspace.arrayfire.integration.array :as array]
            [org.soulspace.arrayfire.integration.device :as device]
            [org.soulspace.arrayfire.integration.jvm-integration :as jvm]))

;;;
;;; Event Lifecycle Management Tests
;;;

(deftest test-create-event
  (testing "create-event! creates new event handle"
    (device/init!)
    (let [evt (event/create-event!)]
      (is (integer? evt))
      (is (not (zero? evt)))
      (event/delete-event! evt))))

(deftest test-create-multiple-events
  (testing "create-event! can create multiple independent events"
    (device/init!)
    (let [evt1 (event/create-event!)
          evt2 (event/create-event!)]
      (is (integer? evt1))
      (is (integer? evt2))
      (is (not= evt1 evt2))
      (event/delete-event! evt1)
      (event/delete-event! evt2))))

(deftest test-delete-event
  (testing "delete-event! successfully deletes event"
    (device/init!)
    (let [evt (event/create-event!)]
      (is (nil? (event/delete-event! evt))))))

(deftest test-event-lifecycle
  (testing "complete event lifecycle: create, use, delete"
    (device/init!)
    (let [evt (event/create-event!)]
      (try
        (is (integer? evt))
        (event/mark-event! evt)
        (event/block-event! evt)
        (finally
          (event/delete-event! evt))))))

;;;
;;; Event Marking and Synchronization Tests
;;;

(deftest test-mark-event
  (testing "mark-event! marks event on active queue"
    (device/init!)
    (let [evt (event/create-event!)]
      (try
        (is (nil? (event/mark-event! evt)))
        (finally
          (event/delete-event! evt))))))

(deftest test-mark-event-multiple-times
  (testing "mark-event! can be called multiple times"
    (device/init!)
    (let [evt (event/create-event!)]
      (try
        (event/mark-event! evt)
        (event/mark-event! evt)
        (event/mark-event! evt)
        (is true) ; All marks succeeded
        (finally
          (event/delete-event! evt))))))

(deftest test-enqueue-wait-event
  (testing "enqueue-wait-event! enqueues wait on active queue"
    (device/init!)
    (let [evt (event/create-event!)]
      (try
        (event/mark-event! evt)
        (is (nil? (event/enqueue-wait-event! evt)))
        (finally
          (event/delete-event! evt))))))

(deftest test-block-event
  (testing "block-event! blocks until event completes"
    (device/init!)
    (let [evt (event/create-event!)]
      (try
        (event/mark-event! evt)
        (is (nil? (event/block-event! evt)))
        (finally
          (event/delete-event! evt))))))

(deftest test-block-event-immediate
  (testing "block-event! can be called immediately after mark"
    (device/init!)
    (let [evt (event/create-event!)]
      (try
        (event/mark-event! evt)
        (event/block-event! evt)
        (is true) ; Block completed successfully
        (finally
          (event/delete-event! evt))))))

;;;
;;; Event Synchronization Pattern Tests
;;;

(deftest test-simple-sync-pattern
  (testing "simple CPU-GPU synchronization pattern"
    (device/init!)
    (let [evt (event/create-event!)]
      (try
        ;; Create some GPU work
        (let [arr (array/create-array (float-array [1.0 2.0 3.0 4.0]) 
                                       [4] jvm/AF_DTYPE_F32)]
          ;; Mark event after GPU work
          (event/mark-event! evt)
          ;; CPU waits for GPU
          (event/block-event! evt)
          (.close arr))
        (finally
          (event/delete-event! evt))))))

(deftest test-mark-and-wait-pattern
  (testing "mark event and wait asynchronously"
    (device/init!)
    (let [evt (event/create-event!)]
      (try
        ;; Some GPU operations
        (let [arr (array/create-array (float-array [1.0 2.0]) 
                                       [2] jvm/AF_DTYPE_F32)]
          (event/mark-event! evt)
          (event/enqueue-wait-event! evt)
          (.close arr))
        (finally
          (event/delete-event! evt))))))

(deftest test-multiple-events-sync
  (testing "synchronization with multiple events"
    (device/init!)
    (let [evt1 (event/create-event!)
          evt2 (event/create-event!)]
      (try
        ;; Create work for first event
        (let [arr1 (array/create-array (float-array [1.0 2.0]) 
                                        [2] jvm/AF_DTYPE_F32)]
          (event/mark-event! evt1)
          (.close arr1))
        
        ;; Create work for second event
        (let [arr2 (array/create-array (float-array [3.0 4.0]) 
                                        [2] jvm/AF_DTYPE_F32)]
          (event/mark-event! evt2)
          (.close arr2))
        
        ;; Wait for both events
        (event/block-event! evt1)
        (event/block-event! evt2)
        (finally
          (event/delete-event! evt1)
          (event/delete-event! evt2))))))

(deftest test-event-ordering
  (testing "events maintain operation ordering"
    (device/init!)
    (let [evt (event/create-event!)]
      (try
        ;; Create multiple operations
        (let [arr1 (array/create-array (float-array [1.0]) [1] jvm/AF_DTYPE_F32)
              arr2 (array/create-array (float-array [2.0]) [1] jvm/AF_DTYPE_F32)
              arr3 (array/create-array (float-array [3.0]) [1] jvm/AF_DTYPE_F32)]
          ;; Mark after all operations
          (event/mark-event! evt)
          ;; Block ensures all complete
          (event/block-event! evt)
          (.close arr1)
          (.close arr2)
          (.close arr3))
        (finally
          (event/delete-event! evt))))))

(deftest test-event-reuse
  (testing "event can be reused by marking multiple times"
    (device/init!)
    (let [evt (event/create-event!)]
      (try
        ;; First use
        (event/mark-event! evt)
        (event/block-event! evt)
        
        ;; Second use
        (event/mark-event! evt)
        (event/block-event! evt)
        
        ;; Third use
        (event/mark-event! evt)
        (event/block-event! evt)
        
        (is true) ; All reuses succeeded
        (finally
          (event/delete-event! evt))))))

(comment
  ;; run all tests from REPL
  (run-tests)
  
  ;; run individual tests
  (run-test test-create-event)
  (run-test test-create-multiple-events)
  (run-test test-delete-event)
  (run-test test-event-lifecycle)
  (run-test test-mark-event)
  (run-test test-mark-event-multiple-times)
  (run-test test-enqueue-wait-event)
  (run-test test-block-event)
  (run-test test-block-event-immediate)
  (run-test test-simple-sync-pattern)
  (run-test test-mark-and-wait-pattern)
  (run-test test-multiple-events-sync)
  (run-test test-event-ordering)
  (run-test test-event-reuse)
  
  ;
  )
