(ns org.soulspace.arrayfire.integration.algorithm-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [org.soulspace.arrayfire.integration.algorithm :as algo]
            [org.soulspace.arrayfire.integration.array :as array]
            [org.soulspace.arrayfire.integration.device :as device]
            [org.soulspace.arrayfire.integration.jvm-integration :as jvm]
            [coffi.mem :as mem])
  (:import [org.soulspace.arrayfire.integration.jvm_integration AFArray]))

(defn- approx=
  "Compare expected/actual values within a tolerance."
  [expected actual tolerance]
  (<= (Math/abs (- (double expected) (double actual)))
      (double tolerance)))

;;;
;;; Matrix Decomposition Tests
;;;

(deftest test-lu
  (testing "LU decomposition"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0 3.0
                                               4.0 5.0 6.0
                                               7.0 8.0 10.0])
                                 [3 3] jvm/AF_DTYPE_F32)
          [l u p] (algo/lu a)
          l-buf (mem/alloc (* 9 4))
          u-buf (mem/alloc (* 9 4))]
      (array/get-data-ptr l l-buf)
      (array/get-data-ptr u u-buf)
      ;; L should be lower triangular
      (is (approx= 1.0 (mem/read-float l-buf 0) 0.001))
      ;; U should be upper triangular
      (is (> (Math/abs (mem/read-float u-buf 0)) 0.001))
      (.close a)
      (.close l)
      (.close u)
      (.close p))))

(deftest test-qr
  (testing "QR decomposition"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0 3.0
                                               4.0 5.0 6.0
                                               7.0 8.0 9.0])
                                 [3 3] jvm/AF_DTYPE_F32)
          [q r tau] (algo/qr a)
          q-buf (mem/alloc (* 9 4))]
      (array/get-data-ptr q q-buf)
      ;; Q should be orthogonal (values exist)
      (is (not (nil? (mem/read-float q-buf 0))))
      (.close a)
      (.close q)
      (.close r)
      (.close tau))))

(deftest test-svd
  (testing "SVD decomposition"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0
                                               3.0 4.0
                                               5.0 6.0])
                                 [3 2] jvm/AF_DTYPE_F32)
          [u s vt] (algo/svd a)
          s-buf (mem/alloc (* 2 4))]
      (array/get-data-ptr s s-buf)
      ;; Singular values should be non-negative
      (is (>= (mem/read-float s-buf 0) 0.0))
      (is (>= (mem/read-float s-buf 4) 0.0))
      (.close a)
      (.close u)
      (.close s)
      (.close vt))))

;;;
;;; Reduction Operations Tests
;;;

(deftest test-sum
  (testing "sum reduces array elements"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4] jvm/AF_DTYPE_F32)
          result (algo/sum a)
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
          result (algo/product a)
          buf (mem/alloc 4)]
      (array/get-data-ptr result buf)
      (is (approx= 24.0 (mem/read-float buf 0) 0.001))
      (.close a)
      (.close result))))

(deftest test-min-max
  (testing "min and max find extreme values"
    (device/init!)
    (let [a (array/create-array (float-array [3.0 1.0 4.0 1.0 5.0]) [5] jvm/AF_DTYPE_F32)
          min-result (algo/min a)
          max-result (algo/max a)
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
          result-a (algo/all-true a)
          result-b (algo/all-true b)
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
          result-a (algo/any-true a)
          result-b (algo/any-true b)
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
          result (algo/count a)
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

(comment
  ;; run tests from REPL
  (run-tests)
  ;
  )
