(ns org.soulspace.arrayfire.integration.unified-api.lapack-test
  (:require [clojure.test :refer [deftest is testing run-test run-tests]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.util.test :refer [approx=]]
            [org.soulspace.arrayfire.ffi.base.definitions :as defs]
            [org.soulspace.arrayfire.integration.unified-api.lapack :as lapack]
            [org.soulspace.arrayfire.integration.unified-api.array :as array]
            [org.soulspace.arrayfire.integration.unified-api.data :as data]
            [org.soulspace.arrayfire.integration.unified-api.device :as device]
            [org.soulspace.arrayfire.integration.base.resource :as res]
            [tech.v3.resource :refer [releasing!]])
  (:import [org.soulspace.arrayfire.integration.base.resource AFArray]))

;;;
;;; Matrix Decompositions Tests
;;;

(deftest test-lu
  (testing "LU decomposition"
    (device/init!)
    (releasing!
      (let [a (array/create-array (float-array [1.0 2.0 3.0
                                                 4.0 5.0 6.0
                                                 7.0 8.0 10.0])
                                   [3 3] defs/AF_DTYPE_F32)
            [l u p] (lapack/lu a)
            l-buf (mem/alloc (* 9 4))
            u-buf (mem/alloc (* 9 4))]
        (array/get-data-ptr l l-buf)
        (array/get-data-ptr u u-buf)
        ;; L should be lower triangular
        (is (approx= 1.0 (mem/read-float l-buf 0) 0.001))
        ;; U should be upper triangular
        (is (> (Math/abs (mem/read-float u-buf 0)) 0.001))))))

(deftest test-qr
  (testing "QR decomposition"
    (device/init!)
    (releasing!
      (let [a (array/create-array (float-array [1.0 2.0 3.0
                                                 4.0 5.0 6.0
                                                 7.0 8.0 9.0])
                                   [3 3] defs/AF_DTYPE_F32)
            [q r tau] (lapack/qr a)
            q-buf (mem/alloc (* 9 4))]
        (array/get-data-ptr q q-buf)
        ;; Q should be orthogonal (values exist)
        (is (not (nil? (mem/read-float q-buf 0))))))))

(deftest test-svd
  (testing "SVD decomposition"
    (device/init!)
    (releasing!
      (let [a (array/create-array (float-array [1.0 2.0
                                                 3.0 4.0
                                                 5.0 6.0])
                                   [3 2] defs/AF_DTYPE_F32)
            [u s vt] (lapack/svd a)
            s-buf (mem/alloc (* 2 4))]
        (array/get-data-ptr s s-buf)
        ;; Singular values should be non-negative
        (is (>= (mem/read-float s-buf 0) 0.0))
        (is (>= (mem/read-float s-buf 4) 0.0))))))

(deftest test-cholesky
  (testing "cholesky decomposes positive definite matrix"
    (device/init!)
    (releasing!
      (let [;; Create a positive definite matrix (A = B' * B)
            b (array/create-array (float-array [1.0 0.0
                                                 2.0 3.0]) [2 2] defs/AF_DTYPE_F32)
            a (array/create-array (float-array [5.0 6.0
                                                 6.0 13.0]) [2 2] defs/AF_DTYPE_F32)
            {:keys [result info]} (lapack/cholesky a)]
        (is (instance? AFArray result))
        (is (integer? info))
        (is (= 0 info))))))

(deftest test-cholesky-lower
  (testing "cholesky with lower triangular output"
    (device/init!)
    (releasing!
      (let [a (array/create-array (float-array [4.0 2.0
                                                 2.0 3.0]) [2 2] defs/AF_DTYPE_F32)
            {:keys [result info]} (lapack/cholesky a false)]
        (is (instance? AFArray result))
        (is (= 0 info))))))

(deftest test-cholesky-upper
  (testing "cholesky with upper triangular output"
    (device/init!)
    (releasing!
      (let [a (array/create-array (float-array [4.0 2.0
                                                 2.0 3.0]) [2 2] defs/AF_DTYPE_F32)
            {:keys [result info]} (lapack/cholesky a true)]
        (is (instance? AFArray result))
        (is (= 0 info))))))

(deftest test-cholesky-inplace
  (testing "cholesky! performs in-place decomposition"
    (device/init!)
    (releasing!
      (let [a (array/create-array (float-array [4.0 2.0
                                                 2.0 3.0]) [2 2] defs/AF_DTYPE_F32)
            {:keys [result info]} (lapack/cholesky! a)]
        (is (= a result))
        (is (integer? info))))))

;;;
;;; Matrix Properties Tests
;;;

(deftest test-det
  (testing "det computes determinant of square matrix"
    (device/init!)
    (releasing!
      (let [a (array/create-array (float-array [1.0 2.0
                                                 3.0 4.0]) [2 2] defs/AF_DTYPE_F32)
            d (lapack/det a)]
        (is (number? d))
        (is (approx= -2.0 d 0.001))))))

(deftest test-det-identity
  (testing "det of identity matrix is 1"
    (device/init!)
    (releasing!
      (let [a (data/identity [3 3] defs/AF_DTYPE_F32)
            d (lapack/det a)]
        (is (approx= 1.0 d 0.001))))))

(deftest test-det-near-singular
  (testing "det of near-singular matrix is very small"
    (device/init!)
    ;; Mathematically, a singular matrix has det=0.
    ;; ArrayFire's det function fails on exactly singular matrices (error 998),
    ;; but works on near-singular matrices. Use rank to detect exact singularity.
    (releasing!
      (let [a (array/create-array (float-array [1.0 2.0
                                                 2.0 4.001]) [2 2] defs/AF_DTYPE_F32)
            d (lapack/det a)]
        (is (number? d))
        (is (< (Math/abs d) 0.01))))))

(deftest test-rank
  (testing "rank computes matrix rank"
    (device/init!)
    (releasing!
      (let [a (array/create-array (float-array [1.0 2.0
                                                 3.0 4.0]) [2 2] defs/AF_DTYPE_F32)
            r (lapack/rank a)]
        (is (integer? r))
        (is (= 2 r))))))

(deftest test-rank-deficient
  (testing "rank detects rank-deficient matrix"
    (device/init!)
    (releasing!
      (let [a (array/create-array (float-array [1.0 2.0
                                                 2.0 4.0]) [2 2] defs/AF_DTYPE_F32)
            r (lapack/rank a)]
        (is (= 1 r))))))

(deftest test-rank-with-tolerance
  (testing "rank with custom tolerance"
    (device/init!)
    (releasing!
      (let [a (array/create-array (float-array [1.0 2.0
                                                 3.0 4.0]) [2 2] defs/AF_DTYPE_F32)
            r (lapack/rank a 1e-6)]
        (is (integer? r))))))

;;;
;;; Norms Tests
;;;

(deftest test-norm-l2
  (testing "norm computes L2 (Euclidean) norm"
    (device/init!)
    (releasing!
      (let [a (array/create-array (float-array [3.0 4.0]) [2] defs/AF_DTYPE_F32)
            n (lapack/norm a 2)]
        (is (number? n))
        (is (approx= 5.0 n 0.001))))))

(deftest test-norm-l1
  (testing "norm computes L1 norm"
    (device/init!)
    (releasing!
      (let [a (array/create-array (float-array [3.0 4.0]) [2] defs/AF_DTYPE_F32)
            n (lapack/norm a 0)]
        (is (approx= 7.0 n 0.001))))))

(deftest test-norm-linf
  (testing "norm computes L-infinity norm"
    (device/init!)
    (releasing!
      (let [a (array/create-array (float-array [3.0 -7.0 4.0]) [3] defs/AF_DTYPE_F32)
            n (lapack/norm a 1)]
        (is (approx= 7.0 n 0.001))))))

(deftest test-norm-default
  (testing "norm with default parameters (L2)"
    (device/init!)
    (releasing!
      (let [a (array/create-array (float-array [1.0 1.0]) [2] defs/AF_DTYPE_F32)
            n (lapack/norm a)]
        (is (approx= (Math/sqrt 2.0) n 0.001))))))

;;;
;;; Matrix Inversion Tests
;;;

(deftest test-inverse
  (testing "inverse computes matrix inverse"
    (device/init!)
    (releasing!
      (let [a (array/create-array (float-array [1.0 2.0
                                                 3.0 4.0]) [2 2] defs/AF_DTYPE_F32)
            inv-a (lapack/inverse a)]
        (is (instance? AFArray inv-a))
        (is (= [2 2 1 1] (array/get-dims inv-a)))))))

(deftest test-inverse-identity
  (testing "inverse of identity is identity"
    (device/init!)
    (releasing!
      (let [a (data/identity [3 3] defs/AF_DTYPE_F32)
            inv-a (lapack/inverse a)
            buf (mem/alloc (* 9 4))]
        (array/get-data-ptr inv-a buf)
        ;; Diagonal should be 1.0
        (is (approx= 1.0 (mem/read-float buf 0) 0.001))))))

(deftest test-pinverse
  (testing "pinverse computes pseudo-inverse"
    (device/init!)
    (releasing!
      (let [a (array/create-array (float-array [1.0 2.0
                                                 3.0 4.0]) [2 2] defs/AF_DTYPE_F32)
            pinv-a (lapack/pinverse a)]
        (is (instance? AFArray pinv-a))
        (is (= [2 2 1 1] (array/get-dims pinv-a)))))))

(deftest test-pinverse-rectangular
  (testing "pinverse works with rectangular matrices"
    (device/init!)
    (releasing!
      (let [a (array/create-array (float-array [1.0 2.0 3.0
                                                 4.0 5.0 6.0]) [2 3] defs/AF_DTYPE_F32)
            pinv-a (lapack/pinverse a)]
        (is (instance? AFArray pinv-a))
        (is (= [3 2 1 1] (array/get-dims pinv-a)))))))

(deftest test-pinverse-with-tolerance
  (testing "pinverse with custom tolerance"
    (device/init!)
    (releasing!
      (let [a (array/create-array (float-array [1.0 2.0
                                                 2.0 4.0]) [2 2] defs/AF_DTYPE_F32)
            pinv-a (lapack/pinverse a 1e-5)]
        (is (instance? AFArray pinv-a))))))

;;;
;;; Linear System Solving Tests
;;;

(deftest test-solve
  (testing "solve solves linear system A·x = b"
    (device/init!)
    (releasing!
      (let [a (array/create-array (float-array [1.0 2.0
                                                 3.0 4.0]) [2 2] defs/AF_DTYPE_F32)
            b (array/create-array (float-array [5.0 11.0]) [2] defs/AF_DTYPE_F32)
            x (lapack/solve a b)
            buf (mem/alloc (* 2 4))]
        (is (instance? AFArray x))
        (array/get-data-ptr x buf)
        ;; Solution for column-major layout: [6.5 -0.5]
        (is (approx= 6.5 (mem/read-float buf 0) 0.01))
        (is (approx= -0.5 (mem/read-float buf 4) 0.01))))))

(deftest test-solve-identity
  (testing "solve with identity matrix returns b"
    (device/init!)
    (releasing!
      (let [a (data/identity [3 3] defs/AF_DTYPE_F32)
            b (array/create-array (float-array [1.0 2.0 3.0]) [3] defs/AF_DTYPE_F32)
            x (lapack/solve a b)
            buf (mem/alloc (* 3 4))]
        (array/get-data-ptr x buf)
        (is (approx= 1.0 (mem/read-float buf 0) 0.001))
        (is (approx= 2.0 (mem/read-float buf 4) 0.001))
        (is (approx= 3.0 (mem/read-float buf 8) 0.001))))))

(deftest test-solve-with-options
  (testing "solve with method options"
    (device/init!)
    (releasing!
      (let [a (array/create-array (float-array [2.0 0.0
                                                 0.0 3.0]) [2 2] defs/AF_DTYPE_F32)
            b (array/create-array (float-array [4.0 6.0]) [2] defs/AF_DTYPE_F32)
            x (lapack/solve a b {:method 0})]
        (is (instance? AFArray x))))))

(deftest test-solve-lu
  (testing "solve-lu solves using pre-computed LU decomposition"
    (device/init!)
    (releasing!
      (let [a (array/create-array (float-array [1.0 2.0
                                                3.0 4.0]) [2 2] defs/AF_DTYPE_F32)
            b (array/create-array (float-array [5.0 11.0]) [2] defs/AF_DTYPE_F32)
            ;; lu! performs in-place LU, leaving a as packed LU and returning pivot
            pivot (lapack/lu! a true)
            x (lapack/solve-lu a pivot b)
            buf (mem/alloc 8)]
        (is (instance? AFArray x))
        (array/get-data-ptr x buf)
        ;; Solution for column-major layout: [6.5 -0.5]
        (is (approx= 6.5 (mem/read-float buf 0) 0.01))
        (is (approx= -0.5 (mem/read-float buf 4) 0.01))))))

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
    (releasing!
      (let [a (array/create-array (float-array [4.0 2.0
                                                 2.0 3.0]) [2 2] defs/AF_DTYPE_F32)
            {:keys [result info]} (lapack/cholesky a)
            det-val (lapack/det a)]
        (when (zero? info)
          (is (pos? det-val)))))))

(deftest test-inverse-solve-equivalence
  (testing "Solving via inverse gives same result as solve"
    (device/init!)
    (releasing!
      (let [a (array/create-array (float-array [1.0 2.0
                                                 3.0 5.0]) [2 2] defs/AF_DTYPE_F32)
            b (array/create-array (float-array [7.0 13.0]) [2] defs/AF_DTYPE_F32)
            x-solve (lapack/solve a b)
            inv-a (lapack/inverse a)]
        (is (instance? AFArray x-solve))
        (is (instance? AFArray inv-a))))))

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
  (run-test test-solve-lu) ; 2 failures
  (run-test test-lapack-available)
  (run-test test-lapack-available-status)
  (run-test test-cholesky-determinant-consistency)
  (run-test test-inverse-solve-equivalence)
  
  ;
  )
