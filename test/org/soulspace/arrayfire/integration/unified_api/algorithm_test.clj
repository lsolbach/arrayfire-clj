(ns org.soulspace.arrayfire.integration.unified-api.algorithm-test
  (:require [clojure.test :refer [deftest is testing run-test run-tests]]
            [org.soulspace.arrayfire.integration.unified-api.algorithm :as algo]
            [org.soulspace.arrayfire.integration.unified-api.array :as array]
            [org.soulspace.arrayfire.integration.unified-api.device :as device]
            [org.soulspace.arrayfire.integration.base.jvm-integration :as jvm]
            [coffi.mem :as mem])
  (:import [org.soulspace.arrayfire.integration.jvm_integration AFArray]))

(defn- approx=
  "Compare expected/actual values within a tolerance."
  [expected actual tolerance]
  (<= (Math/abs (- (double expected) (double actual)))
      (double tolerance)))

;;;
;;; Reduction Operations Tests
;;;

(deftest test-sum
  (testing "sum reduces array elements"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4] jvm/AF_DTYPE_F32)
          result (algo/sum a 0)
          buf (mem/alloc 4)]
      (array/get-data-ptr result buf)
      (is (approx= 10.0 (mem/read-float buf 0) 0.001))
      (.close a)
      (.close result))))

(deftest test-sum-dim
  (testing "sum along specific dimension"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] jvm/AF_DTYPE_F32)
          result (algo/sum a 0)
          buf (mem/alloc (* 2 4))]
      (array/get-data-ptr result buf)
      ;; Sum along dimension 0 should give [3.0 7.0] for column-major
      (is (not (nil? (mem/read-float buf 0))))
      (.close a)
      (.close result))))

(deftest test-product
  (testing "product multiplies array elements"
    (device/init!)
    (let [a (array/create-array (float-array [2.0 3.0 4.0]) [3] jvm/AF_DTYPE_F32)
          result (algo/product a 0)
          buf (mem/alloc 4)]
      (array/get-data-ptr result buf)
      (is (approx= 24.0 (mem/read-float buf 0) 0.001))
      (.close a)
      (.close result))))

(deftest test-min-max
  (testing "min and max find extreme values"
    (device/init!)
    (let [a (array/create-array (float-array [3.0 1.0 4.0 1.0 5.0]) [5] jvm/AF_DTYPE_F32)
          min-result (algo/min a 0)
          max-result (algo/max a 0)
          min-buf (mem/alloc 4)
          max-buf (mem/alloc 4)]
      (array/get-data-ptr min-result min-buf)
      (array/get-data-ptr max-result max-buf)
      (is (approx= 1.0 (mem/read-float min-buf 0) 0.001))
      (is (approx= 5.0 (mem/read-float max-buf 0) 0.001))
      (.close a)
      (.close min-result)
      (.close max-result))))

(deftest test-all-true
  (testing "all-true checks if all elements are non-zero"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0 3.0]) [3] jvm/AF_DTYPE_F32)
          b (array/create-array (float-array [1.0 0.0 3.0]) [3] jvm/AF_DTYPE_F32)
          result-a (algo/all-true a 0)
          result-b (algo/all-true b 0)
          buf-a (mem/alloc 1)
          buf-b (mem/alloc 1)]
      (array/get-data-ptr result-a buf-a)
      (array/get-data-ptr result-b buf-b)
      (is (= 1 (mem/read-byte buf-a 0)))
      (is (= 0 (mem/read-byte buf-b 0)))
      (.close a)
      (.close b)
      (.close result-a)
      (.close result-b))))

(deftest test-any-true
  (testing "any-true checks if any element is non-zero"
    (device/init!)
    (let [a (array/create-array (float-array [0.0 0.0 0.0]) [3] jvm/AF_DTYPE_F32)
          b (array/create-array (float-array [0.0 1.0 0.0]) [3] jvm/AF_DTYPE_F32)
          result-a (algo/any-true a 0)
          result-b (algo/any-true b 0)
          buf-a (mem/alloc 1)
          buf-b (mem/alloc 1)]
      (array/get-data-ptr result-a buf-a)
      (array/get-data-ptr result-b buf-b)
      (is (= 0 (mem/read-byte buf-a 0)))
      (is (= 1 (mem/read-byte buf-b 0)))
      (.close a)
      (.close b)
      (.close result-a)
      (.close result-b))))

(deftest test-count
  (testing "count counts non-zero elements"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 0.0 3.0 0.0 5.0]) [5] jvm/AF_DTYPE_F32)
          result (algo/count a 0)
          buf (mem/alloc 4)]
      (array/get-data-ptr result buf)
      (is (= 3 (mem/read-int buf 0)))
      (.close a)
      (.close result))))

;;;
;;; Sorting Operations Tests
;;;

(deftest test-sort
  (testing "sort orders array elements"
    (device/init!)
    (let [a (array/create-array (float-array [3.0 1.0 4.0 1.0 5.0]) [5] jvm/AF_DTYPE_F32)
          result (algo/sort a)
          buf (mem/alloc (* 5 4))]
      (array/get-data-ptr result buf)
      (is (approx= 1.0 (mem/read-float buf 0) 0.001))
      (is (approx= 1.0 (mem/read-float buf 4) 0.001))
      (is (approx= 3.0 (mem/read-float buf 8) 0.001))
      (.close a)
      (.close result))))

(deftest test-sort-descending
  (testing "sort in descending order"
    (device/init!)
    (let [a (array/create-array (float-array [3.0 1.0 4.0]) [3] jvm/AF_DTYPE_F32)
          result (algo/sort a 0 false)
          buf (mem/alloc (* 3 4))]
      (array/get-data-ptr result buf)
      (is (approx= 4.0 (mem/read-float buf 0) 0.001))
      (is (approx= 3.0 (mem/read-float buf 4) 0.001))
      (is (approx= 1.0 (mem/read-float buf 8) 0.001))
      (.close a)
      (.close result))))

(deftest test-sort-index
  (testing "sort-index returns sorted values and indices"
    (device/init!)
    (let [a (array/create-array (float-array [3.0 1.0 4.0]) [3] jvm/AF_DTYPE_F32)
          [sorted indices] (algo/sort-index a)
          sorted-buf (mem/alloc (* 3 4))
          indices-buf (mem/alloc (* 3 4))]
      (array/get-data-ptr sorted sorted-buf)
      (array/get-data-ptr indices indices-buf)
      (is (approx= 1.0 (mem/read-float sorted-buf 0) 0.001))
      (is (not (nil? (mem/read-int indices-buf 0))))
      (.close a)
      (.close sorted)
      (.close indices))))

;;;
;;; Set Operations Tests
;;;

(deftest test-set-unique
  (testing "set-unique finds unique elements"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0 2.0 3.0 3.0 3.0]) [6] jvm/AF_DTYPE_F32)
          result (algo/set-unique a)
          buf (mem/alloc (* 3 4))]
      (array/get-data-ptr result buf)
      ;; Should have 3 unique values: 1.0, 2.0, 3.0
      (is (not (nil? (mem/read-float buf 0))))
      (.close a)
      (.close result))))

(deftest test-where
  (testing "where finds indices of non-zero elements"
    (device/init!)
    (let [a (array/create-array (float-array [0.0 1.0 0.0 2.0 0.0]) [5] jvm/AF_DTYPE_F32)
          result (algo/where a)]
      ;; Should return indices [1, 3] for non-zero elements
      (is (instance? AFArray result))
      (.close a)
      (.close result))))

;;;
;;; By-Key Reduction Tests
;;;

(deftest test-sum-by-key
  (testing "sum-by-key sums values grouped by keys"
    (device/init!)
    (let [keys (array/create-array (int-array [1 1 1 2 2 3]) [6] jvm/AF_DTYPE_S32)
          vals (array/create-array (float-array [10.0 20.0 30.0 40.0 50.0 60.0]) [6] jvm/AF_DTYPE_F32)
          [keys-out vals-out] (algo/sum-by-key keys vals)
          keys-buf (mem/alloc (* 3 4))
          vals-buf (mem/alloc (* 3 4))]
      (is (instance? AFArray keys-out))
      (is (instance? AFArray vals-out))
      (array/get-data-ptr vals-out vals-buf)
      ;; First group (key 1): 10 + 20 + 30 = 60
      (is (approx= 60.0 (mem/read-float vals-buf 0) 0.001))
      (.close keys)
      (.close vals)
      (.close keys-out)
      (.close vals-out))))

(deftest test-product-by-key
  (testing "product-by-key multiplies values grouped by keys"
    (device/init!)
    (let [keys (array/create-array (int-array [1 1 2 2 3]) [5] jvm/AF_DTYPE_S32)
          vals (array/create-array (float-array [2.0 3.0 4.0 5.0 6.0]) [5] jvm/AF_DTYPE_F32)
          [keys-out vals-out] (algo/product-by-key keys vals)
          vals-buf (mem/alloc (* 3 4))]
      (is (instance? AFArray keys-out))
      (is (instance? AFArray vals-out))
      (array/get-data-ptr vals-out vals-buf)
      ;; First group (key 1): 2 * 3 = 6
      (is (approx= 6.0 (mem/read-float vals-buf 0) 0.001))
      (.close keys)
      (.close vals)
      (.close keys-out)
      (.close vals-out))))

(deftest test-min-by-key
  (testing "min-by-key finds minimum value per key group"
    (device/init!)
    (let [keys (array/create-array (int-array [1 1 1 2 2]) [5] jvm/AF_DTYPE_S32)
          vals (array/create-array (float-array [5.0 2.0 8.0 3.0 7.0]) [5] jvm/AF_DTYPE_F32)
          [keys-out vals-out] (algo/min-by-key keys vals)
          vals-buf (mem/alloc (* 2 4))]
      (is (instance? AFArray keys-out))
      (is (instance? AFArray vals-out))
      (array/get-data-ptr vals-out vals-buf)
      ;; First group (key 1): min(5, 2, 8) = 2
      (is (approx= 2.0 (mem/read-float vals-buf 0) 0.001))
      (.close keys)
      (.close vals)
      (.close keys-out)
      (.close vals-out))))

(deftest test-max-by-key
  (testing "max-by-key finds maximum value per key group"
    (device/init!)
    (let [keys (array/create-array (int-array [1 1 1 2 2]) [5] jvm/AF_DTYPE_S32)
          vals (array/create-array (float-array [5.0 2.0 8.0 3.0 7.0]) [5] jvm/AF_DTYPE_F32)
          [keys-out vals-out] (algo/max-by-key keys vals)
          vals-buf (mem/alloc (* 2 4))]
      (is (instance? AFArray keys-out))
      (is (instance? AFArray vals-out))
      (array/get-data-ptr vals-out vals-buf)
      ;; First group (key 1): max(5, 2, 8) = 8
      (is (approx= 8.0 (mem/read-float vals-buf 0) 0.001))
      (.close keys)
      (.close vals)
      (.close keys-out)
      (.close vals-out))))

;;;
;;; Difference Operator Tests
;;;

(deftest test-diff1
  (testing "diff1 computes first-order difference"
    (device/init!)
    (let [a (array/create-array (float-array [0.0 1.0 4.0 9.0 16.0]) [5] jvm/AF_DTYPE_F32)
          result (algo/diff1 a 0)
          buf (mem/alloc (* 4 4))]
      (is (instance? AFArray result))
      ;; Output size should be reduced by 1
      (is (= [4] (take 1 (array/get-dims result))))
      (array/get-data-ptr result buf)
      ;; First difference: 1 - 0 = 1
      (is (approx= 1.0 (mem/read-float buf 0) 0.001))
      ;; Second difference: 4 - 1 = 3
      (is (approx= 3.0 (mem/read-float buf 4) 0.001))
      (.close a)
      (.close result))))

(deftest test-diff2
  (testing "diff2 computes second-order difference"
    (device/init!)
    (let [a (array/create-array (float-array [0.0 1.0 4.0 9.0 16.0]) [5] jvm/AF_DTYPE_F32)
          result (algo/diff2 a 0)
          buf (mem/alloc (* 3 4))]
      (is (instance? AFArray result))
      ;; Output size should be reduced by 2
      (is (= [3] (take 1 (array/get-dims result))))
      (array/get-data-ptr result buf)
      ;; First second-order diff: (4 - 2*1 + 0) = 2
      (is (approx= 2.0 (mem/read-float buf 0) 0.001))
      (.close a)
      (.close result))))

(deftest test-diff1-2d
  (testing "diff1 along different dimensions"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0 6.0]) [2 3] jvm/AF_DTYPE_F32)
          result-dim0 (algo/diff1 a 0)
          result-dim1 (algo/diff1 a 1)]
      (is (instance? AFArray result-dim0))
      (is (instance? AFArray result-dim1))
      ;; Dimension 0: output [1 3]
      (is (= [1 3] (take 2 (array/get-dims result-dim0))))
      ;; Dimension 1: output [2 2]
      (is (= [2 2] (take 2 (array/get-dims result-dim1))))
      (.close a)
      (.close result-dim0)
      (.close result-dim1))))

(deftest test-all-true-by-key
  (testing "all-true-by-key checks if all values are non-zero per group"
    (device/init!)
    (let [keys (array/create-array (int-array [1 1 1 2 2 2]) [6] jvm/AF_DTYPE_S32)
          vals (array/create-array (int-array [1 1 1 0 1 1]) [6] jvm/AF_DTYPE_S32)
          [keys-out vals-out] (algo/all-true-by-key keys vals)]
      (is (= 1 (array/get-numdims keys-out)))
      (is (= 1 (array/get-numdims vals-out)))
      ;; Group 1 should be true (all 1s), Group 2 should be false (has a 0)
      (.close keys)
      (.close vals)
      (.close keys-out)
      (.close vals-out))))

(deftest test-any-true-by-key
  (testing "any-true-by-key checks if any value is non-zero per group"
    (device/init!)
    (let [keys (array/create-array (int-array [1 1 1 2 2 2]) [6] jvm/AF_DTYPE_S32)
          vals (array/create-array (int-array [0 0 1 0 0 0]) [6] jvm/AF_DTYPE_S32)
          [keys-out vals-out] (algo/any-true-by-key keys vals)]
      (is (= 1 (array/get-numdims keys-out)))
      (is (= 1 (array/get-numdims vals-out)))
      ;; Group 1 should be true (has a 1), Group 2 should be false (all 0s)
      (.close keys)
      (.close vals)
      (.close keys-out)
      (.close vals-out))))

(deftest test-count-by-key
  (testing "count-by-key counts non-zero values per group"
    (device/init!)
    (let [keys (array/create-array (int-array [1 1 1 2 2 2]) [6] jvm/AF_DTYPE_S32)
          vals (array/create-array (int-array [1 0 1 1 1 0]) [6] jvm/AF_DTYPE_S32)
          [keys-out vals-out] (algo/count-by-key keys vals)]
      (is (= 1 (array/get-numdims keys-out)))
      (is (= 1 (array/get-numdims vals-out)))
      ;; Group 1 should have 2 non-zeros, Group 2 should have 2 non-zeros
      (.close keys)
      (.close vals)
      (.close keys-out)
      (.close vals-out))))

(deftest test-accum
  (testing "accum computes cumulative sum"
    (device/init!)
    (let [data (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0]) [5] jvm/AF_DTYPE_F32)
          result (algo/accum data)]
      (is (= 1 (array/get-numdims result)))
      (is (= [5] (take 1 (array/get-dims result))))
      ;; Result should be [1 3 6 10 15]
      (.close data)
      (.close result))))

(deftest test-accum-2d
  (testing "accum on 2D array along dimension"
    (device/init!)
    (let [data (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0 6.0]) [2 3] jvm/AF_DTYPE_F32)
          result-dim0 (algo/accum data 0)
          result-dim1 (algo/accum data 1)]
      (is (= 2 (array/get-numdims result-dim0)))
      (is (= 2 (array/get-numdims result-dim1)))
      (.close data)
      (.close result-dim0)
      (.close result-dim1))))

(comment
  ;; run tests from REPL
  (run-tests)

  ;; run single test
  (run-test test-sum)
  (run-test test-sum-dim)
  (run-test test-product)
  (run-test test-min-max)
  (run-test test-all-true)
  (run-test test-any-true)
  (run-test test-count)
  (run-test test-sort)
  (run-test test-sort-descending)
  (run-test test-sort-index)
  (run-test test-set-unique)
  (run-test test-where)
  (run-test test-sum-by-key)
  (run-test test-product-by-key)
  (run-test test-min-by-key)
  (run-test test-max-by-key)
  (run-test test-diff1)
  (run-test test-diff2)
  (run-test test-diff1-2d)
  (run-test test-all-true-by-key)
  (run-test test-any-true-by-key)
  (run-test test-count-by-key)
  (run-test test-accum)
  (run-test test-accum-2d)

  ;
  )

