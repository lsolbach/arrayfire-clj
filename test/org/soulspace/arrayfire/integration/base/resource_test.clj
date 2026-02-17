(ns org.soulspace.arrayfire.integration.base.resource-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.definitions :as defs]
            [org.soulspace.arrayfire.ffi.c-api.array :as af-array]
            [org.soulspace.arrayfire.integration.base.error :refer [check!]]
            [org.soulspace.arrayfire.integration.base.resource :as res]
            [org.soulspace.arrayfire.integration.unified-api.device :as device])
  (:import [org.soulspace.arrayfire.integration.base.resource AFArray]))

;;;
;;; Resource Management Tests
;;;

(deftest test-af-array-new
  (testing "af-array-new creates AFArray instance"
    (device/init!)
    (let [arena (java.lang.foreign.Arena/ofConfined)]
      (try
        ;; Create a simple array
        (let [dims-seg (.allocate arena (long (* 1 (.byteSize java.lang.foreign.ValueLayout/JAVA_LONG))))
            _ (mem/write-long dims-seg 0 3)
            data-seg (.allocate arena (long (* 3 (.byteSize java.lang.foreign.ValueLayout/JAVA_FLOAT))))
            _ (dotimes [i 3]
              (mem/write-float data-seg (* i (.byteSize java.lang.foreign.ValueLayout/JAVA_FLOAT))
                       (float (inc i))))
              out-ptr (.allocate arena java.lang.foreign.ValueLayout/ADDRESS)
              _ (check! (af-array/af-create-array out-ptr data-seg 1 dims-seg defs/AF_DTYPE_F32)
                           "test-create-array")
              handle (res/deref-af-array out-ptr)
              af-arr (res/af-array-new handle)]
          (is (instance? AFArray af-arr))
              (is (number? (res/af-handle-value af-arr)))
              (is (pos? (res/af-handle-value af-arr)))
          (.close af-arr))
        (finally
          (.close arena))))))

(deftest test-af-array-auto-cleanup
  (testing "AFArray automatically cleans up when garbage collected"
    (device/init!)
    (let [arena (java.lang.foreign.Arena/ofConfined)]
      (try
        (let [dims-seg (.allocate arena (long (* 1 (.byteSize java.lang.foreign.ValueLayout/JAVA_LONG))))
            _ (mem/write-long dims-seg 0 3)
            data-seg (.allocate arena (long (* 3 (.byteSize java.lang.foreign.ValueLayout/JAVA_FLOAT))))
            _ (dotimes [i 3]
              (mem/write-float data-seg (* i (.byteSize java.lang.foreign.ValueLayout/JAVA_FLOAT))
                       (float (inc i))))
              out-ptr (.allocate arena java.lang.foreign.ValueLayout/ADDRESS)
              _ (check! (af-array/af-create-array out-ptr data-seg 1 dims-seg defs/AF_DTYPE_F32)
                           "test-create-array")
              handle (res/deref-af-array out-ptr)]
          ;; Create AFArray and let it go out of scope
          (res/af-array-new handle)
          ;; Force GC to trigger cleanup
          (System/gc)
          (Thread/sleep 100)
          ;; If we get here without error, cleanup worked
          (is true))
        (finally
          (.close arena))))))

(deftest test-af-array-closed-access
  (testing "Accessing closed AFArray throws exception"
    (device/init!)
    (let [arena (java.lang.foreign.Arena/ofConfined)]
      (try
        (let [dims-seg (.allocate arena (long (* 1 (.byteSize java.lang.foreign.ValueLayout/JAVA_LONG))))
            _ (mem/write-long dims-seg 0 3)
            data-seg (.allocate arena (long (* 3 (.byteSize java.lang.foreign.ValueLayout/JAVA_FLOAT))))
            _ (dotimes [i 3]
              (mem/write-float data-seg (* i (.byteSize java.lang.foreign.ValueLayout/JAVA_FLOAT))
                       (float (inc i))))
              out-ptr (.allocate arena java.lang.foreign.ValueLayout/ADDRESS)
              _ (check! (af-array/af-create-array out-ptr data-seg 1 dims-seg defs/AF_DTYPE_F32)
                           "test-create-array")
              handle (res/deref-af-array out-ptr)
              af-arr (res/af-array-new handle)]
          (.close af-arr)
              (is (thrown? IllegalStateException (res/af-handle-value af-arr))))
        (finally
          (.close arena))))))

(comment
  ;; run tests from REPL
  (run-tests)
  ;
  )
