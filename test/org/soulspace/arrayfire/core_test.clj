(ns org.soulspace.arrayfire.core-test
  (:require [clojure.test :refer [deftest is run-tests testing]]
            [org.soulspace.arrayfire.core :as af]
            [tech.v3.datatype :as dtype]))


(deftest simple-add
  (af/init!)
  (is (= :ok (af/info)))
  (let [a (af/create-array [1.0 2.0 3.0] [3])
        b (af/create-array [10.0 20.0 30.0] [3])
        c (af/add a b)
        out (af/to-host c 3)]
    (is (= [11.0 22.0 33.0] (seq out)))
    (af/release a)
    (af/release b)
    (af/release c)))


(deftest test-float-array
  (af/init!)
  (let [a (af/create-array-f32 [1.0 2.0 3.0 4.0] [2 2])
        out (af/to-host-f32 a 4)]
    (is (= [1.0 2.0 3.0 4.0] out))
    (af/release a)))


(deftest test-double-array
  (af/init!)
  (let [a (af/create-array-f64 [1.0 2.0 3.0 4.0] [2 2])
        out (af/to-host-f64 a 4)]
    (is (= [1.0 2.0 3.0 4.0] out))
    (af/release a)))


(deftest test-complex-float-array
  (af/init!)
  (let [a (af/create-array-c32 [[1.0 0.5] [2.0 1.0] [3.0 1.5]] [3])
        out (af/to-host-c32 a 3)]
    (is (= [[1.0 0.5] [2.0 1.0] [3.0 1.5]] out))
    (af/release a)))


(deftest test-complex-double-array
  (af/init!)
  (let [a (af/create-array-c64 [[1.0 0.5] [2.0 1.0] [3.0 1.5]] [3])
        out (af/to-host-c64 a 3)]
    (is (= [[1.0 0.5] [2.0 1.0] [3.0 1.5]] out))
    (af/release a)))


(deftest test-int32-array
  (af/init!)
  (let [a (af/create-array-s32 [1 2 3 4] [2 2])
        out (af/to-host-s32 a 4)]
    (is (= [1 2 3 4] out))
    (af/release a)))


(deftest test-int64-array
  (af/init!)
  (let [a (af/create-array-s64 [1 2 3 4] [2 2])
        out (af/to-host-s64 a 4)]
    (is (= [1 2 3 4] out))
    (af/release a)))


(deftest test-dtype-next-integration
  (af/init!)
  (testing "Create array from native tensor (zero-copy on host)"
    (let [tensor (dtype/make-container :native-heap :float64 [1.0 2.0 3.0 4.0])
          _ (is (some? (dtype/as-native-buffer tensor)) "Tensor should be native-backed")
          arr (af/create-array-from-native tensor [4])
          out (af/to-host-f64 arr 4)]
      (is (= [1.0 2.0 3.0 4.0] out))
      (af/release arr)))
  
  (testing "Create array from tensor with auto-shape"
    (let [tensor (dtype/make-container :native-heap :float64 [5.0 6.0 7.0])
          arr (af/create-array-from-tensor tensor)
          out (af/to-host-f64 arr 3)]
      (is (= [5.0 6.0 7.0] out))
      (af/release arr)))
  
  (testing "To native buffer roundtrip - returns native-backed buffer"
    (let [original-data [1.0 2.0 3.0]
          arr (af/create-array-f64 original-data [3])
          tensor (af/to-native-buffer arr :float64 3)]
      ;; Verify it's a native buffer, not JVM heap
      (is (some? (dtype/as-native-buffer tensor)) "Result should be native-backed")
      (is (= original-data (vec tensor)))
      (af/release arr)))
  
  (testing "Bidirectional conversion preserves data"
    (let [original [10.0 20.0 30.0 40.0]
          ;; dtype → ArrayFire
          tensor1 (dtype/make-container :native-heap :float64 original)
          arr (af/create-array-from-native tensor1 [4])
          ;; ArrayFire → dtype (returns native buffer)
          tensor2 (af/to-native-buffer arr :float64 4)]
      (is (= original (vec tensor1)) "Original tensor unchanged")
      (is (= original (vec tensor2)) "Roundtrip preserved data")
      (is (some? (dtype/as-native-buffer tensor2)) "Result is native-backed")
      (af/release arr))))

(comment
  ;; To run tests from REPL
  (run-tests)
  ;
  )

;; Addition tests for all data types

(deftest test-add-float
  (testing "Addition of float32 arrays"
    (af/init!)
    (let [a (af/create-array-f32 [1.0 2.0 3.0] [3])
          b (af/create-array-f32 [4.0 5.0 6.0] [3])
          c (af/add a b)
          out (af/to-host-f32 c 3)]
      (is (= [5.0 7.0 9.0] out))
      (af/release a)
      (af/release b)
      (af/release c))))


(deftest test-add-double
  (testing "Addition of float64 arrays"
    (af/init!)
    (let [a (af/create-array-f64 [1.5 2.5 3.5] [3])
          b (af/create-array-f64 [4.5 5.5 6.5] [3])
          c (af/add a b)
          out (af/to-host-f64 c 3)]
      (is (= [6.0 8.0 10.0] out))
      (af/release a)
      (af/release b)
      (af/release c))))


(deftest test-add-complex-float
  (testing "Addition of complex32 arrays"
    (af/init!)
    (let [a (af/create-array-c32 [[1.0 2.0] [3.0 4.0]] [2])
          b (af/create-array-c32 [[5.0 6.0] [7.0 8.0]] [2])
          c (af/add a b)
          out (af/to-host-c32 c 2)]
      ;; (1+2i) + (5+6i) = (6+8i)
      ;; (3+4i) + (7+8i) = (10+12i)
      (is (= [[6.0 8.0] [10.0 12.0]] out))
      (af/release a)
      (af/release b)
      (af/release c))))


(deftest test-add-complex-double
  (testing "Addition of complex64 arrays"
    (af/init!)
    (let [a (af/create-array-c64 [[1.5 2.5] [3.5 4.5]] [2])
          b (af/create-array-c64 [[5.5 6.5] [7.5 8.5]] [2])
          c (af/add a b)
          out (af/to-host-c64 c 2)]
      ;; (1.5+2.5i) + (5.5+6.5i) = (7.0+9.0i)
      ;; (3.5+4.5i) + (7.5+8.5i) = (11.0+13.0i)
      (is (= [[7.0 9.0] [11.0 13.0]] out))
      (af/release a)
      (af/release b)
      (af/release c))))


(deftest test-add-int32
  (testing "Addition of int32 arrays"
    (af/init!)
    (let [a (af/create-array-s32 [10 20 30] [3])
          b (af/create-array-s32 [5 15 25] [3])
          c (af/add a b)
          out (af/to-host-s32 c 3)]
      (is (= [15 35 55] out))
      (af/release a)
      (af/release b)
      (af/release c))))


(deftest test-add-uint32
  (testing "Addition of uint32 arrays"
    (af/init!)
    (let [a (af/create-array-u32 [100 200 300] [3])
          b (af/create-array-u32 [50 150 250] [3])
          c (af/add a b)
          out (af/to-host-u32 c 3)]
      (is (= [150 350 550] out))
      (af/release a)
      (af/release b)
      (af/release c))))


(deftest test-add-int64
  (testing "Addition of int64 arrays"
    (af/init!)
    (let [a (af/create-array-s64 [1000 2000 3000] [3])
          b (af/create-array-s64 [500 1500 2500] [3])
          c (af/add a b)
          out (af/to-host-s64 c 3)]
      (is (= [1500 3500 5500] out))
      (af/release a)
      (af/release b)
      (af/release c))))


(deftest test-add-uint64
  (testing "Addition of uint64 arrays"
    (af/init!)
    (let [a (af/create-array-u64 [10000 20000 30000] [3])
          b (af/create-array-u64 [5000 15000 25000] [3])
          c (af/add a b)
          out (af/to-host-u64 c 3)]
      (is (= [15000 35000 55000] out))
      (af/release a)
      (af/release b)
      (af/release c))))

(comment
  ;; To run tests from REPL
  (run-tests)
  ;
  )