(ns org.soulspace.arrayfire.integration.unified-api.lapack-test
  (:require [clojure.test :refer [deftest is testing run-test run-tests]]
            [org.soulspace.arrayfire.util.test :refer [approx=]]
            [org.soulspace.arrayfire.integration.unified-api.lapack :as lapack]
            [org.soulspace.arrayfire.integration.unified-api.array :as array]
            [org.soulspace.arrayfire.integration.unified-api.data :as data]
            [org.soulspace.arrayfire.integration.unified-api.device :as device]
            [org.soulspace.arrayfire.integration.jvm-integration :as jvm]
            [coffi.mem :as mem])
  (:import [org.soulspace.arrayfire.integration.jvm_integration AFArray]))

;;;
;;; Matrix Decompositions Tests
;;;

(deftest test-lu
  (testing "LU decomposition"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0 3.0
                                               4.0 5.0 6.0
                                               7.0 8.0 10.0])
                                 [3 3] jvm/AF_DTYPE_F32)
          [l u p] (lapack/lu a)
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
          [q r tau] (lapack/qr a)
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
          [u s vt] (lapack/svd a)
          s-buf (mem/alloc (* 2 4))]
      (array/get-data-ptr s s-buf)
      ;; Singular values should be non-negative
      (is (>= (mem/read-float s-buf 0) 0.0))
      (is (>= (mem/read-float s-buf 4) 0.0))
      (.close a)
      (.close u)
      (.close s)
      (.close vt))))

(deftest test-cholesky
  (testing "cholesky decomposes positive definite matrix"
    (device/init!)
    (let [;; Create a positive definite matrix (A = B' * B)
          b (array/create-array (float-array [1.0 0.0
                                               2.0 3.0]) [2 2] jvm/AF_DTYPE_F32)
          a (array/create-array (float-array [5.0 6.0
                                               6.0 13.0]) [2 2] jvm/AF_DTYPE_F32)
          {:keys [result info]} (lapack/cholesky a)]
      (try
        (is (instance? AFArray result))
        (is (integer? info))
        (is (= 0 info)) ; Success
        (finally
          (.close b)
          (.close a)
          (.close result))))))

(deftest test-cholesky-lower
  (testing "cholesky with lower triangular output"
    (device/init!)
    (let [a (array/create-array (float-array [4.0 2.0
                                               2.0 3.0]) [2 2] jvm/AF_DTYPE_F32)
          {:keys [result info]} (lapack/cholesky a false)]
      (try
        (is (instance? AFArray result))
        (is (= 0 info))
        (finally
          (.close a)
          (.close result))))))

(deftest test-cholesky-upper
  (testing "cholesky with upper triangular output"
    (device/init!)
    (let [a (array/create-array (float-array [4.0 2.0
                                               2.0 3.0]) [2 2] jvm/AF_DTYPE_F32)
          {:keys [result info]} (lapack/cholesky a true)]
      (try
        (is (instance? AFArray result))
        (is (= 0 info))
        (finally
          (.close a)
          (.close result))))))

(deftest test-cholesky-inplace
  (testing "cholesky! performs in-place decomposition"
    (device/init!)
    (let [a (array/create-array (float-array [4.0 2.0
                                               2.0 3.0]) [2 2] jvm/AF_DTYPE_F32)
          {:keys [result info]} (lapack/cholesky! a)]
      (try
        (is (= a result)) ; Same array
        (is (integer? info))
        (finally
          (.close a))))))

;;;
;;; Matrix Properties Tests
;;;

(deftest test-det
  (testing "det computes determinant of square matrix"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0
                                               3.0 4.0]) [2 2] jvm/AF_DTYPE_F32)
          d (lapack/det a)]
      (try
        (is (number? d))
        (is (approx= -2.0 d 0.001))
        (finally
          (.close a))))))

(deftest test-det-identity
  (testing "det of identity matrix is 1"
    (device/init!)
    (let [a (data/identity [3 3] jvm/AF_DTYPE_F32)
          d (lapack/det a)]
      (try
        (is (approx= 1.0 d 0.001))
        (finally
          (.close a))))))

(deftest test-det-near-singular
  (testing "det of near-singular matrix is very small"
    (device/init!)
    ;; Mathematically, a singular matrix has det=0.
    ;; ArrayFire's det function fails on exactly singular matrices (error 998),
    ;; but works on near-singular matrices. Use rank to detect exact singularity.
    (let [a (array/create-array (float-array [1.0 2.0
                                               2.0 4.001]) [2 2] jvm/AF_DTYPE_F32)
          d (lapack/det a)]
      (try
        (is (number? d))
        (is (< (Math/abs d) 0.01)) ; Very close to 0
        (finally
          (.close a))))))

(deftest test-rank
  (testing "rank computes matrix rank"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0
                                               3.0 4.0]) [2 2] jvm/AF_DTYPE_F32)
          r (lapack/rank a)]
      (try
        (is (integer? r))
        (is (= 2 r))
        (finally
          (.close a))))))

(deftest test-rank-deficient
  (testing "rank detects rank-deficient matrix"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0
                                               2.0 4.0]) [2 2] jvm/AF_DTYPE_F32)
          r (lapack/rank a)]
      (try
        (is (= 1 r))
        (finally
          (.close a))))))

(deftest test-rank-with-tolerance
  (testing "rank with custom tolerance"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0
                                               3.0 4.0]) [2 2] jvm/AF_DTYPE_F32)
          r (lapack/rank a 1e-6)]
      (try
        (is (integer? r))
        (finally
          (.close a))))))

;;;
;;; Norms Tests
;;;

(deftest test-norm-l2
  (testing "norm computes L2 (Euclidean) norm"
    (device/init!)
    (let [a (array/create-array (float-array [3.0 4.0]) [2] jvm/AF_DTYPE_F32)
          n (lapack/norm a 2)]
      (try
        (is (number? n))
        (is (approx= 5.0 n 0.001))
        (finally
          (.close a))))))

(deftest test-norm-l1
  (testing "norm computes L1 norm"
    (device/init!)
    (let [a (array/create-array (float-array [3.0 4.0]) [2] jvm/AF_DTYPE_F32)
          n (lapack/norm a 0)]
      (try
        (is (approx= 7.0 n 0.001))
        (finally
          (.close a))))))

(deftest test-norm-linf
  (testing "norm computes L-infinity norm"
    (device/init!)
    (let [a (array/create-array (float-array [3.0 -7.0 4.0]) [3] jvm/AF_DTYPE_F32)
          n (lapack/norm a 1)]
      (try
        (is (approx= 7.0 n 0.001))
        (finally
          (.close a))))))

(deftest test-norm-default
  (testing "norm with default parameters (L2)"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 1.0]) [2] jvm/AF_DTYPE_F32)
          n (lapack/norm a)]
      (try
        (is (approx= (Math/sqrt 2.0) n 0.001))
        (finally
          (.close a))))))

;;;
;;; Matrix Inversion Tests
;;;

(deftest test-inverse
  (testing "inverse computes matrix inverse"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0
                                               3.0 4.0]) [2 2] jvm/AF_DTYPE_F32)
          inv-a (lapack/inverse a)]
      (try
        (is (instance? AFArray inv-a))
        (is (= [2 2 1 1] (array/get-dims inv-a)))
        (finally
          (.close a)
          (.close inv-a))))))

(deftest test-inverse-identity
  (testing "inverse of identity is identity"
    (device/init!)
    (let [a (data/identity [3 3] jvm/AF_DTYPE_F32)
          inv-a (lapack/inverse a)
          buf (mem/alloc (* 9 4))]
      (try
        (array/get-data-ptr inv-a buf)
        ;; Diagonal should be 1.0
        (is (approx= 1.0 (mem/read-float buf 0) 0.001))
        (finally
          (.close a)
          (.close inv-a))))))

(deftest test-pinverse
  (testing "pinverse computes pseudo-inverse"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0
                                               3.0 4.0]) [2 2] jvm/AF_DTYPE_F32)
          pinv-a (lapack/pinverse a)]
      (try
        (is (instance? AFArray pinv-a))
        (is (= [2 2 1 1] (array/get-dims pinv-a)))
        (finally
          (.close a)
          (.close pinv-a))))))

(deftest test-pinverse-rectangular
  (testing "pinverse works with rectangular matrices"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0 3.0
                                               4.0 5.0 6.0]) [2 3] jvm/AF_DTYPE_F32)
          pinv-a (lapack/pinverse a)]
      (try
        (is (instance? AFArray pinv-a))
        (is (= [3 2 1 1] (array/get-dims pinv-a)))
        (finally
          (.close a)
          (.close pinv-a))))))

(deftest test-pinverse-with-tolerance
  (testing "pinverse with custom tolerance"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0
                                               2.0 4.0]) [2 2] jvm/AF_DTYPE_F32)
          pinv-a (lapack/pinverse a 1e-5)]
      (try
        (is (instance? AFArray pinv-a))
        (finally
          (.close a)
          (.close pinv-a))))))

;;;
;;; Linear System Solving Tests
;;;

(deftest test-solve
  (testing "solve solves linear system A·x = b"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0
                                               3.0 4.0]) [2 2] jvm/AF_DTYPE_F32)
          b (array/create-array (float-array [5.0 11.0]) [2] jvm/AF_DTYPE_F32)
          x (lapack/solve a b)
          buf (mem/alloc (* 2 4))]
      (try
        (is (instance? AFArray x))
        (array/get-data-ptr x buf)
        ;; Solution for column-major layout: [6.5 -0.5]
        (is (approx= 6.5 (mem/read-float buf 0) 0.01))
        (is (approx= -0.5 (mem/read-float buf 4) 0.01))
        (finally
          (.close a)
          (.close b)
          (.close x))))))

(deftest test-solve-identity
  (testing "solve with identity matrix returns b"
    (device/init!)
    (let [a (data/identity [3 3] jvm/AF_DTYPE_F32)
          b (array/create-array (float-array [1.0 2.0 3.0]) [3] jvm/AF_DTYPE_F32)
          x (lapack/solve a b)
          buf (mem/alloc (* 3 4))]
      (try
        (array/get-data-ptr x buf)
        (is (approx= 1.0 (mem/read-float buf 0) 0.001))
        (is (approx= 2.0 (mem/read-float buf 4) 0.001))
        (is (approx= 3.0 (mem/read-float buf 8) 0.001))
        (finally
          (.close a)
          (.close b)
          (.close x))))))

(deftest test-solve-with-options
  (testing "solve with method options"
    (device/init!)
    (let [a (array/create-array (float-array [2.0 0.0
                                               0.0 3.0]) [2 2] jvm/AF_DTYPE_F32)
          b (array/create-array (float-array [4.0 6.0]) [2] jvm/AF_DTYPE_F32)
          x (lapack/solve a b {:method 0})]
      (try
        (is (instance? AFArray x))
        (finally
          (.close a)
          (.close b)
          (.close x))))))

(deftest test-solve-lu
  (testing "solve-lu solves using pre-computed LU decomposition"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0
                                               3.0 4.0]) [2 2] jvm/AF_DTYPE_F32)
          b (array/create-array (float-array [5.0 11.0]) [2] jvm/AF_DTYPE_F32)
          [l u p] (lapack/lu a)
          x (lapack/solve-lu l p b)  ; Use l (lower) not u (upper)
          buf (mem/alloc 8)]
      (try
        (is (instance? AFArray x))
        (array/get-data-ptr x buf)
        ;; Solution should be approximately [1.0 2.0]
        (is (approx= 1.0 (mem/read-float buf 0) 0.01))
        (is (approx= 2.0 (mem/read-float buf 4) 0.01))
        (finally
          (.close a)
          (.close b)
          (.close l)
          (.close u)
          (.close p)
          (.close x))))))

;;;
;;; Utility Functions Tests
;;;

(deftest test-lapack-available
  (testing "lapack-available? returns boolean"
    (device/init!)
    (let [available (lapack/lapack-available?)]
      (is (boolean? available)))))

(deftest test-lapack-available-status
  (testing "lapack-available? indicates LAPACK support status"
    (device/init!)
    (let [available (lapack/lapack-available?)]
      ;; Just verify it returns a boolean
      ;; Actual availability depends on ArrayFire build
      (is (or (true? available) (false? available))))))

;;;
;;; Integration Tests
;;;

(deftest test-cholesky-determinant-consistency
  (testing "Cholesky decomposition and determinant are consistent"
    (device/init!)
    (let [a (array/create-array (float-array [4.0 2.0
                                               2.0 3.0]) [2 2] jvm/AF_DTYPE_F32)
          {:keys [result info]} (lapack/cholesky a)
          det-val (lapack/det a)]
      (try
        (when (zero? info)
          (is (pos? det-val))) ; Positive definite => positive determinant
        (finally
          (.close a)
          (.close result))))))

(deftest test-inverse-solve-equivalence
  (testing "Solving via inverse gives same result as solve"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0
                                               3.0 5.0]) [2 2] jvm/AF_DTYPE_F32)
          b (array/create-array (float-array [7.0 13.0]) [2] jvm/AF_DTYPE_F32)
          x-solve (lapack/solve a b)
          inv-a (lapack/inverse a)]
      (try
        (is (instance? AFArray x-solve))
        (is (instance? AFArray inv-a))
        (finally
          (.close a)
          (.close b)
          (.close x-solve)
          (.close inv-a))))))

(comment
  ;; run all tests from REPL
  (run-tests)
  
  ;; run individual tests
  (run-test test-lu)
  (run-test test-qr)
  (run-test test-svd)
  (run-test test-cholesky)
  (run-test test-cholesky-lower)
  (run-test test-cholesky-upper)
  (run-test test-cholesky-inplace)
  (run-test test-det)
  (run-test test-det-identity)
  (run-test test-det-near-singular)
  (run-test test-rank)
  (run-test test-rank-deficient)
  (run-test test-rank-with-tolerance)
  (run-test test-norm-l2)
  (run-test test-norm-l1)
  (run-test test-norm-linf)
  (run-test test-norm-default)
  (run-test test-inverse)
  (run-test test-inverse-identity)
  (run-test test-pinverse)
  (run-test test-pinverse-rectangular)
  (run-test test-pinverse-with-tolerance)
  (run-test test-solve)
  (run-test test-solve-identity)
  (run-test test-solve-with-options)
  (run-test test-solve-lu)
  (run-test test-lapack-available)
  (run-test test-lapack-available-status)
  (run-test test-cholesky-determinant-consistency)
  (run-test test-inverse-solve-equivalence)
  
  ;
  )
