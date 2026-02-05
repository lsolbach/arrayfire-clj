(ns org.soulspace.arrayfire.integration.blas-test
  (:require [clojure.test :refer [deftest is testing run-test run-tests]]
            [org.soulspace.arrayfire.integration.blas :as blas]
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
;;; Matrix Operations Tests
;;;

(deftest test-gemm
  (testing "gemm performs general matrix multiply"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] jvm/AF_DTYPE_F32)
          b (array/create-array (float-array [5.0 6.0 7.0 8.0]) [2 2] jvm/AF_DTYPE_F32)
          ;; C = 2.0 * A * B + 0.0
          result (blas/gemm 0 0 2.0 a b 0.0)]
      (is (instance? AFArray result))
      (is (= [2 2 1 1] (array/get-dims result)))
      (.close result)
      (.close b)
      (.close a))))

(deftest test-gemm-with-transpose
  (testing "gemm with transpose operation"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] jvm/AF_DTYPE_F32)
          b (array/create-array (float-array [5.0 6.0 7.0 8.0]) [2 2] jvm/AF_DTYPE_F32)
          ;; C = 1.0 * A^T * B + 0.0
          result (blas/gemm 1 0 1.0 a b 0.0)]
      (is (instance? AFArray result))
      (is (= [2 2 1 1] (array/get-dims result)))
      (.close result)
      (.close b)
      (.close a))))

(deftest test-gemm-with-beta
  (testing "gemm with non-zero beta"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] jvm/AF_DTYPE_F32)
          b (array/create-array (float-array [1.0 0.0 0.0 1.0]) [2 2] jvm/AF_DTYPE_F32)
          ;; C = 1.0 * A * B + 2.0 * C (beta effectively scales the result)
          result (blas/gemm 0 0 1.0 a b 2.0)]
      (is (instance? AFArray result))
      (is (= [2 2 1 1] (array/get-dims result)))
      (.close result)
      (.close b)
      (.close a))))

(deftest test-matmul
  (testing "matmul performs basic matrix multiplication"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] jvm/AF_DTYPE_F32)
          b (array/create-array (float-array [5.0 6.0 7.0 8.0]) [2 2] jvm/AF_DTYPE_F32)
          result (blas/matmul a b)
          buf (mem/alloc (* 4 4))]
      (is (instance? AFArray result))
      (is (= [2 2 1 1] (array/get-dims result)))
      (array/get-data-ptr result buf)
      ;; First element should be 1*5 + 3*6 = 23 (column-major)
      (is (approx= 23.0 (mem/read-float buf 0) 0.001))
      (.close result)
      (.close b)
      (.close a))))

(deftest test-matmul-with-transpose
  (testing "matmul with transpose options"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] jvm/AF_DTYPE_F32)
          b (array/create-array (float-array [5.0 6.0 7.0 8.0]) [2 2] jvm/AF_DTYPE_F32)
          result (blas/matmul a b 1 0)] ; transpose a
      (is (instance? AFArray result))
      (is (= [2 2 1 1] (array/get-dims result)))
      (.close result)
      (.close b)
      (.close a))))

(deftest test-matmul-rectangle
  (testing "matmul with non-square matrices"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0 6.0]) [2 3] jvm/AF_DTYPE_F32)
          b (array/create-array (float-array [7.0 8.0 9.0 10.0 11.0 12.0]) [3 2] jvm/AF_DTYPE_F32)
          result (blas/matmul a b)]
      (is (instance? AFArray result))
      (is (= [2 2 1 1] (array/get-dims result)))
      (.close result)
      (.close b)
      (.close a))))

(deftest test-dot
  (testing "dot computes dot product of vectors"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0 3.0]) [3] jvm/AF_DTYPE_F32)
          b (array/create-array (float-array [4.0 5.0 6.0]) [3] jvm/AF_DTYPE_F32)
          result (blas/dot a b)
          buf (mem/alloc 4)]
      (is (instance? AFArray result))
      (is (= [1 1 1 1] (array/get-dims result)))
      (array/get-data-ptr result buf)
      ;; 1*4 + 2*5 + 3*6 = 32
      (is (approx= 32.0 (mem/read-float buf 0) 0.001))
      (.close result)
      (.close b)
      (.close a))))

(deftest test-dot-with-options
  (testing "dot with conjugate options"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0 3.0]) [3] jvm/AF_DTYPE_F32)
          b (array/create-array (float-array [4.0 5.0 6.0]) [3] jvm/AF_DTYPE_F32)
          result (blas/dot a b 0 0)]
      (is (instance? AFArray result))
      (is (= [1 1 1 1] (array/get-dims result)))
      (.close result)
      (.close b)
      (.close a))))

(deftest test-dot-all
  (testing "dot-all returns scalar result directly"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0 3.0]) [3] jvm/AF_DTYPE_F32)
          b (array/create-array (float-array [4.0 5.0 6.0]) [3] jvm/AF_DTYPE_F32)
          result (blas/dot-all a b)]
      (is (number? result))
      ;; 1*4 + 2*5 + 3*6 = 32
      (is (approx= 32.0 result 0.001))
      (.close b)
      (.close a))))

(deftest test-dot-all-with-options
  (testing "dot-all with conjugate options"
    (device/init!)
    (let [a (array/create-array (float-array [2.0 3.0]) [2] jvm/AF_DTYPE_F32)
          b (array/create-array (float-array [4.0 5.0]) [2] jvm/AF_DTYPE_F32)
          result (blas/dot-all a b 0 0)]
      (is (number? result))
      ;; 2*4 + 3*5 = 23
      (is (approx= 23.0 result 0.001))
      (.close b)
      (.close a))))

(deftest test-dot-all-complex
  (testing "dot-all with complex arrays returns complex result"
    (device/init!)
    (let [a (array/create-array [[1.0 2.0] [3.0 4.0]] [2] jvm/AF_DTYPE_C32)
          b (array/create-array [[5.0 0.0] [6.0 0.0]] [2] jvm/AF_DTYPE_C32)
          result (blas/dot-all a b)]
      ;; Result could be a number or [real imag] vector depending on imaginary part
      (is (or (number? result) (vector? result)))
      (.close b)
      (.close a))))

(deftest test-transpose
  (testing "transpose transposes a matrix"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0 6.0]) [2 3] jvm/AF_DTYPE_F32)
          result (blas/transpose a)]
      (is (instance? AFArray result))
      (is (= [3 2 1 1] (array/get-dims result)))
      (.close result)
      (.close a))))

(deftest test-transpose-square
  (testing "transpose of square matrix"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] jvm/AF_DTYPE_F32)
          result (blas/transpose a)
          buf (mem/alloc (* 4 4))]
      (is (instance? AFArray result))
      (is (= [2 2 1 1] (array/get-dims result)))
      (array/get-data-ptr result buf)
      ;; After transpose in column-major: [1.0 3.0 2.0 4.0], element at index 1 is 3.0
      (is (approx= 3.0 (mem/read-float buf 4) 0.001))
      (.close result)
      (.close a))))

(deftest test-transpose-with-conjugate
  (testing "transpose with conjugate flag"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] jvm/AF_DTYPE_F32)
          result (blas/transpose a false)]
      (is (instance? AFArray result))
      (is (= [2 2 1 1] (array/get-dims result)))
      (.close result)
      (.close a))))

(deftest test-transpose-inplace
  (testing "transpose! modifies matrix in-place"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] jvm/AF_DTYPE_F32)
          result (blas/transpose! a)]
      (is (identical? result a))
      (is (= [2 2 1 1] (array/get-dims a)))
      (.close a))))

(deftest test-transpose-inplace-with-conjugate
  (testing "transpose! with conjugate flag"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] jvm/AF_DTYPE_F32)
          result (blas/transpose! a false)]
      (is (identical? result a))
      (.close a))))

(comment
  ;; run all tests from REPL
  (run-tests)
  
  ;; run individual tests
  (run-test test-gemm)
  (run-test test-gemm-with-transpose)
  (run-test test-gemm-with-beta)
  (run-test test-matmul)
  (run-test test-matmul-with-transpose)
  (run-test test-matmul-rectangle)
  (run-test test-dot)
  (run-test test-dot-with-options)
  (run-test test-dot-all)
  (run-test test-dot-all-with-options)
  (run-test test-dot-all-complex)
  (run-test test-transpose)
  (run-test test-transpose-square)
  (run-test test-transpose-with-conjugate)
  (run-test test-transpose-inplace)
  (run-test test-transpose-inplace-with-conjugate)
  
  ;
  )
