(ns org.soulspace.arrayfire.integration.base.memory-test
  "Tests for memory management utilities in org.soulspace.arrayfire.integration.base.memory."
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.integration.base.memory :as bmem]))

;;;
;;; Type Conversion Tests
;;;

(deftest test-dims-to-segment
  (testing "dims->segment converts Clojure vector to MemorySegment"
    (let [dims [3 4 5]
        seg (bmem/dims->segment dims)]
      (is (instance? java.lang.foreign.MemorySegment seg))
      (is (= 3 (mem/read-long seg 0)))
      (is (= 4 (mem/read-long seg 8)))
      (is (= 5 (mem/read-long seg 16))))))

(deftest test-float-array-to-segment
  (testing "float-array->segment converts float array to MemorySegment"
    (let [data (float-array [1.0 2.0 3.0])
        seg (bmem/float-array->segment data)]
      (is (instance? java.lang.foreign.MemorySegment seg))
      (is (= 1.0 (mem/read-float seg 0)))
      (is (= 2.0 (mem/read-float seg 4)))
      (is (= 3.0 (mem/read-float seg 8))))))

(deftest test-double-array-to-segment
  (testing "double-array->segment converts double array to MemorySegment"
    (let [data (double-array [1.0 2.0 3.0])
        seg (bmem/double-array->segment data)]
      (is (instance? java.lang.foreign.MemorySegment seg))
      (is (= 1.0 (mem/read-double seg 0)))
      (is (= 2.0 (mem/read-double seg 8)))
      (is (= 3.0 (mem/read-double seg 16))))))

(deftest test-complex-float-array-to-segment
  (testing "complex-float-array->segment converts [real imag] pairs to interleaved floats"
    (let [data [[1.0 2.0] [3.0 4.0]]
        seg (bmem/complex-float-array->segment data)]
      (is (instance? java.lang.foreign.MemorySegment seg))
      ;; Check interleaved format: [real1 imag1 real2 imag2]
      (is (= 1.0 (mem/read-float seg 0)))
      (is (= 2.0 (mem/read-float seg 4)))
      (is (= 3.0 (mem/read-float seg 8)))
      (is (= 4.0 (mem/read-float seg 12))))))

(deftest test-complex-double-array-to-segment
  (testing "complex-double-array->segment converts [real imag] pairs to interleaved doubles"
    (let [data [[1.0 2.0] [3.0 4.0]]
        seg (bmem/complex-double-array->segment data)]
      (is (instance? java.lang.foreign.MemorySegment seg))
      ;; Check interleaved format: [real1 imag1 real2 imag2]
      (is (= 1.0 (mem/read-double seg 0)))
      (is (= 2.0 (mem/read-double seg 8)))
      (is (= 3.0 (mem/read-double seg 16)))
      (is (= 4.0 (mem/read-double seg 24))))))

(deftest test-string-conversions
  (testing "String to C string and back preserves content"
    (let [original "Hello ArrayFire"
          c-str (bmem/string->c-string original)
          roundtrip (bmem/c-string->string c-str)]
      (is (= original roundtrip)))))

;;;
;;; Memory Operation Tests
;;;

(deftest test-read-write-float
  (testing "Write and read float values"
    (let [buf (mem/alloc 4)]
      (bmem/write-float! buf 0 3.14)
      (is (<= (Math/abs (- 3.14 (bmem/read-float buf 0))) 0.001)))))

(deftest test-read-write-double
  (testing "Write and read double values"
    (let [buf (mem/alloc 8)]
      (bmem/write-double! buf 0 3.14159)
      (is (<= (Math/abs (- 3.14159 (bmem/read-double buf 0))) 0.00001)))))

(deftest test-read-write-int
  (testing "Write and read int values"
    (let [buf (mem/alloc 4)]
      (bmem/write-int! buf 0 42)
      (is (= 42 (bmem/read-int buf 0))))))

(deftest test-read-write-long
  (testing "Write and read long values"
    (let [buf (mem/alloc 8)]
      (bmem/write-long! buf 0 9876543210)
      (is (= 9876543210 (bmem/read-long buf 0))))))

(deftest test-read-write-complex-float
  (testing "Write and read complex float values"
    (let [buf (mem/alloc 8)
          value [3.14 2.71]]
      (bmem/write-complex-float! buf 0 value)
      (let [[real imag] (bmem/read-complex-float buf 0)]
        (is (<= (Math/abs (- 3.14 real)) 0.001))
        (is (<= (Math/abs (- 2.71 imag)) 0.001))))))

(deftest test-read-write-complex-double
  (testing "Write and read complex double values"
    (let [buf (mem/alloc 16)
          value [3.14159 2.71828]]
      (bmem/write-complex-double! buf 0 value)
      (let [[real imag] (bmem/read-complex-double buf 0)]
        (is (<= (Math/abs (- 3.14159 real)) 0.00001))
        (is (<= (Math/abs (- 2.71828 imag)) 0.00001))))))

(comment
  ;; run tests from REPL
  (run-tests)
  ;
  )
