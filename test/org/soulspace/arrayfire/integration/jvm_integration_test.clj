(ns org.soulspace.arrayfire.integration.jvm-integration-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [org.soulspace.arrayfire.integration.jvm-integration :as jvm]
            [org.soulspace.arrayfire.integration.device :as device]
            [org.soulspace.arrayfire.ffi.array :as af-array]
            [coffi.mem :as mem])
  (:import [org.soulspace.arrayfire.integration.jvm_integration AFArray]))

;;;
;;; Error Handling Tests
;;;

(deftest test-check-success
  (testing "check! succeeds with AF_SUCCESS (0)"
    (is (nil? (jvm/check! jvm/AF_SUCCESS "test-operation")))))

(deftest test-check-error
  (testing "check! throws exception on non-zero error code"
    (is (thrown-with-msg? 
         clojure.lang.ExceptionInfo
         #"ArrayFire error at test-error"
         (jvm/check! jvm/AF_ERR_NO_MEM "test-error")))))

(deftest test-check-error-details
  (testing "check! includes error code and location in exception data"
    (try
      (jvm/check! jvm/AF_ERR_INVALID_ARRAY "test-location")
      (is false "Should have thrown exception")
      (catch clojure.lang.ExceptionInfo e
        (let [data (ex-data e)]
          (is (= jvm/AF_ERR_INVALID_ARRAY (:code data)))
          (is (= "test-location" (:where data))))))))

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
              _ (jvm/check! (af-array/af-create-array out-ptr data-seg 1 dims-seg jvm/AF_DTYPE_F32)
                           "test-create-array")
              handle (jvm/deref-af-array out-ptr)
              af-arr (jvm/af-array-new handle)]
          (is (instance? AFArray af-arr))
              (is (number? (jvm/af-handle-value af-arr)))
              (is (pos? (jvm/af-handle-value af-arr)))
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
              _ (jvm/check! (af-array/af-create-array out-ptr data-seg 1 dims-seg jvm/AF_DTYPE_F32)
                           "test-create-array")
              handle (jvm/deref-af-array out-ptr)]
          ;; Create AFArray and let it go out of scope
          (jvm/af-array-new handle)
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
              _ (jvm/check! (af-array/af-create-array out-ptr data-seg 1 dims-seg jvm/AF_DTYPE_F32)
                           "test-create-array")
              handle (jvm/deref-af-array out-ptr)
              af-arr (jvm/af-array-new handle)]
          (.close af-arr)
              (is (thrown? IllegalStateException (jvm/af-handle-value af-arr))))
        (finally
          (.close arena))))))

;;;
;;; Type Conversion Tests
;;;

(deftest test-dims-to-segment
  (testing "dims->segment converts Clojure vector to MemorySegment"
    (let [dims [3 4 5]
        seg (jvm/dims->segment dims)]
      (is (instance? java.lang.foreign.MemorySegment seg))
      (is (= 3 (mem/read-long seg 0)))
      (is (= 4 (mem/read-long seg 8)))
      (is (= 5 (mem/read-long seg 16))))))

(deftest test-float-array-to-segment
  (testing "float-array->segment converts float array to MemorySegment"
    (let [data (float-array [1.0 2.0 3.0])
        seg (jvm/float-array->segment data)]
      (is (instance? java.lang.foreign.MemorySegment seg))
      (is (= 1.0 (mem/read-float seg 0)))
      (is (= 2.0 (mem/read-float seg 4)))
      (is (= 3.0 (mem/read-float seg 8))))))

(deftest test-double-array-to-segment
  (testing "double-array->segment converts double array to MemorySegment"
    (let [data (double-array [1.0 2.0 3.0])
        seg (jvm/double-array->segment data)]
      (is (instance? java.lang.foreign.MemorySegment seg))
      (is (= 1.0 (mem/read-double seg 0)))
      (is (= 2.0 (mem/read-double seg 8)))
      (is (= 3.0 (mem/read-double seg 16))))))

(deftest test-complex-float-array-to-segment
  (testing "complex-float-array->segment converts [real imag] pairs to interleaved floats"
    (let [data [[1.0 2.0] [3.0 4.0]]
        seg (jvm/complex-float-array->segment data)]
      (is (instance? java.lang.foreign.MemorySegment seg))
      ;; Check interleaved format: [real1 imag1 real2 imag2]
      (is (= 1.0 (mem/read-float seg 0)))
      (is (= 2.0 (mem/read-float seg 4)))
      (is (= 3.0 (mem/read-float seg 8)))
      (is (= 4.0 (mem/read-float seg 12))))))

(deftest test-complex-double-array-to-segment
  (testing "complex-double-array->segment converts [real imag] pairs to interleaved doubles"
    (let [data [[1.0 2.0] [3.0 4.0]]
        seg (jvm/complex-double-array->segment data)]
      (is (instance? java.lang.foreign.MemorySegment seg))
      ;; Check interleaved format: [real1 imag1 real2 imag2]
      (is (= 1.0 (mem/read-double seg 0)))
      (is (= 2.0 (mem/read-double seg 8)))
      (is (= 3.0 (mem/read-double seg 16)))
      (is (= 4.0 (mem/read-double seg 24))))))

(deftest test-string-conversions
  (testing "String to C string and back preserves content"
    (let [original "Hello ArrayFire"
          c-str (jvm/string->c-string original)
          roundtrip (jvm/c-string->string c-str)]
      (is (= original roundtrip)))))

;;;
;;; Memory Operation Tests
;;;

(deftest test-read-write-float
  (testing "Write and read float values"
    (let [buf (mem/alloc 4)]
      (jvm/write-float! buf 0 3.14)
      (is (<= (Math/abs (- 3.14 (jvm/read-float buf 0))) 0.001)))))

(deftest test-read-write-double
  (testing "Write and read double values"
    (let [buf (mem/alloc 8)]
      (jvm/write-double! buf 0 3.14159)
      (is (<= (Math/abs (- 3.14159 (jvm/read-double buf 0))) 0.00001)))))

(deftest test-read-write-int
  (testing "Write and read int values"
    (let [buf (mem/alloc 4)]
      (jvm/write-int! buf 0 42)
      (is (= 42 (jvm/read-int buf 0))))))

(deftest test-read-write-long
  (testing "Write and read long values"
    (let [buf (mem/alloc 8)]
      (jvm/write-long! buf 0 9876543210)
      (is (= 9876543210 (jvm/read-long buf 0))))))

(deftest test-read-write-complex-float
  (testing "Write and read complex float values"
    (let [buf (mem/alloc 8)
          value [3.14 2.71]]
      (jvm/write-complex-float! buf 0 value)
      (let [[real imag] (jvm/read-complex-float buf 0)]
        (is (<= (Math/abs (- 3.14 real)) 0.001))
        (is (<= (Math/abs (- 2.71 imag)) 0.001))))))

(deftest test-read-write-complex-double
  (testing "Write and read complex double values"
    (let [buf (mem/alloc 16)
          value [3.14159 2.71828]]
      (jvm/write-complex-double! buf 0 value)
      (let [[real imag] (jvm/read-complex-double buf 0)]
        (is (<= (Math/abs (- 3.14159 real)) 0.00001))
        (is (<= (Math/abs (- 2.71828 imag)) 0.00001))))))

(comment
  ;; run tests from REPL
  (run-tests)
  ;
  )
