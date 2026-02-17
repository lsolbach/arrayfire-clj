(ns org.soulspace.arrayfire.integration.unified-api.blas-test
  (:require [clojure.test :refer [deftest is testing run-test run-tests]]
            [coffi.mem :as mem]
            [tech.v3.resource :refer [releasing!]]
            [org.soulspace.arrayfire.util.test :refer [approx=]]
            [org.soulspace.arrayfire.ffi.base.definitions :as defs]
            [org.soulspace.arrayfire.integration.unified-api.blas :as blas]
            [org.soulspace.arrayfire.integration.unified-api.array :as array]
            [org.soulspace.arrayfire.integration.unified-api.device :as device]
            [org.soulspace.arrayfire.integration.base.resource :as res])
  (:import [org.soulspace.arrayfire.integration.base.resource AFArray]))

;;;
;;; Matrix Operations Tests
;;;

(deftest test-gemm
  (testing "gemm performs general matrix multiply"
    (device/init!)
    (releasing!
     (let [a (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] defs/AF_DTYPE_F32)
           b (array/create-array (float-array [5.0 6.0 7.0 8.0]) [2 2] defs/AF_DTYPE_F32)
           ;; C = 2.0 * A * B + 0.0
           result (blas/gemm 0 0 2.0 a b 0.0)]
       (is (instance? AFArray result))
       (is (= [2 2 1 1] (array/get-dims result)))))))

(deftest test-gemm-with-transpose
  (testing "gemm with transpose operation"
    (device/init!)
    (releasing!
     (let [a (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] defs/AF_DTYPE_F32)
           b (array/create-array (float-array [5.0 6.0 7.0 8.0]) [2 2] defs/AF_DTYPE_F32)
           ;; C = 1.0 * A^T * B + 0.0
           result (blas/gemm 1 0 1.0 a b 0.0)]
       (is (instance? AFArray result))
       (is (= [2 2 1 1] (array/get-dims result)))))))

(deftest test-gemm-with-beta
  (testing "gemm with non-zero beta"
    (device/init!)
    (releasing!
     (let [a (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] defs/AF_DTYPE_F32)
           b (array/create-array (float-array [1.0 0.0 0.0 1.0]) [2 2] defs/AF_DTYPE_F32)
           ;; C = 1.0 * A * B + 2.0 * C (beta effectively scales the result)
           result (blas/gemm 0 0 1.0 a b 2.0)]
       (is (instance? AFArray result))
       (is (= [2 2 1 1] (array/get-dims result)))))))

(deftest test-matmul
  (testing "matmul performs basic matrix multiplication"
    (device/init!)
    (releasing!
     (let [a (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] defs/AF_DTYPE_F32)
           b (array/create-array (float-array [5.0 6.0 7.0 8.0]) [2 2] defs/AF_DTYPE_F32)
           result (blas/matmul a b)
           buf (mem/alloc (* 4 4))]
       (is (instance? AFArray result))
       (is (= [2 2 1 1] (array/get-dims result)))
       (array/get-data-ptr result buf)
       ;; First element should be 1*5 + 3*6 = 23 (column-major)
       (is (approx= 23.0 (mem/read-float buf 0) 0.001))))))

(deftest test-matmul-with-transpose
  (testing "matmul with transpose options"
    (device/init!)
    (releasing!
     (let [a (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] defs/AF_DTYPE_F32)
           b (array/create-array (float-array [5.0 6.0 7.0 8.0]) [2 2] defs/AF_DTYPE_F32)
           result (blas/matmul a b 1 0)] ; transpose a
       (is (instance? AFArray result))
       (is (= [2 2 1 1] (array/get-dims result)))))))

(deftest test-matmul-rectangle
  (testing "matmul with non-square matrices"
    (device/init!)
    (releasing!
     (let [a (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0 6.0]) [2 3] defs/AF_DTYPE_F32)
           b (array/create-array (float-array [7.0 8.0 9.0 10.0 11.0 12.0]) [3 2] defs/AF_DTYPE_F32)
           result (blas/matmul a b)]
       (is (instance? AFArray result))
       (is (= [2 2 1 1] (array/get-dims result)))))))

(deftest test-dot
  (testing "dot computes dot product of vectors"
    (device/init!)
    (releasing!
     (let [a (array/create-array (float-array [1.0 2.0 3.0]) [3] defs/AF_DTYPE_F32)
           b (array/create-array (float-array [4.0 5.0 6.0]) [3] defs/AF_DTYPE_F32)
           result (blas/dot a b)
           buf (mem/alloc 4)]
       (is (instance? AFArray result))
       (is (= [1 1 1 1] (array/get-dims result)))
       (array/get-data-ptr result buf)
       ;; 1*4 + 2*5 + 3*6 = 32
       (is (approx= 32.0 (mem/read-float buf 0) 0.001))))))

(deftest test-dot-with-options
  (testing "dot with conjugate options"
    (device/init!)
    (releasing!
     (let [a (array/create-array (float-array [1.0 2.0 3.0]) [3] defs/AF_DTYPE_F32)
           b (array/create-array (float-array [4.0 5.0 6.0]) [3] defs/AF_DTYPE_F32)
           result (blas/dot a b 0 0)]
       (is (instance? AFArray result))
       (is (= [1 1 1 1] (array/get-dims result)))))))

(deftest test-dot-all
  (testing "dot-all returns scalar result directly"
    (device/init!)
    (releasing!
     (let [a (array/create-array (float-array [1.0 2.0 3.0]) [3] defs/AF_DTYPE_F32)
           b (array/create-array (float-array [4.0 5.0 6.0]) [3] defs/AF_DTYPE_F32)
           result (blas/dot-all a b)]
       (is (number? result))
       ;; 1*4 + 2*5 + 3*6 = 32
       (is (approx= 32.0 result 0.001))))))

(deftest test-dot-all-with-options
  (testing "dot-all with conjugate options"
    (device/init!)
    (releasing!
     (let [a (array/create-array (float-array [2.0 3.0]) [2] defs/AF_DTYPE_F32)
           b (array/create-array (float-array [4.0 5.0]) [2] defs/AF_DTYPE_F32)
           result (blas/dot-all a b 0 0)]
       (is (number? result))
       ;; 2*4 + 3*5 = 23
       (is (approx= 23.0 result 0.001))))))

(deftest test-dot-all-complex
  (testing "dot-all with complex arrays returns complex result"
    (device/init!)
    (releasing!
     (let [a (array/create-array [[1.0 2.0] [3.0 4.0]] [2] defs/AF_DTYPE_C32)
           b (array/create-array [[5.0 0.0] [6.0 0.0]] [2] defs/AF_DTYPE_C32)
           result (blas/dot-all a b)]
       ;; Result could be a number or [real imag] vector depending on imaginary part
       (is (or (number? result) (vector? result)))))))

(deftest test-transpose
  (testing "transpose transposes a matrix"
    (device/init!)
    (releasing!
     (let [a (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0 6.0]) [2 3] defs/AF_DTYPE_F32)
           result (blas/transpose a)]
       (is (instance? AFArray result))
       (is (= [3 2 1 1] (array/get-dims result)))))))

(deftest test-transpose-square
  (testing "transpose of square matrix"
    (device/init!)
    (releasing!
     (let [a (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] defs/AF_DTYPE_F32)
           result (blas/transpose a)
           buf (mem/alloc (* 4 4))]
       (is (instance? AFArray result))
       (is (= [2 2 1 1] (array/get-dims result)))
       (array/get-data-ptr result buf)
       ;; After transpose in column-major: [1.0 3.0 2.0 4.0], element at index 1 is 3.0
       (is (approx= 3.0 (mem/read-float buf 4) 0.001))))))

(deftest test-transpose-with-conjugate
  (testing "transpose with conjugate flag"
    (device/init!)
    (releasing!
     (let [a (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] defs/AF_DTYPE_F32)
           result (blas/transpose a false)]
       (is (instance? AFArray result))
       (is (= [2 2 1 1] (array/get-dims result)))))))

(deftest test-transpose-inplace
  (testing "transpose! modifies matrix in-place"
    (device/init!)
    (releasing!
     (let [a (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] defs/AF_DTYPE_F32)
           result (blas/transpose! a)]
       (is (identical? result a))
       (is (= [2 2 1 1] (array/get-dims a)))))))

(deftest test-transpose-inplace-with-conjugate
  (testing "transpose! with conjugate flag"
    (device/init!)
    (releasing!
     (let [a (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] defs/AF_DTYPE_F32)
           result (blas/transpose! a false)]
       (is (identical? result a))))))

;;;
;;; Matrix Multiplication Convenience Functions Tests
;;;

(deftest test-matmul-nt
  (testing "matmul-nt multiplies A * B^T"
    (device/init!)
    (releasing!
     (let [a (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] defs/AF_DTYPE_F32)
           b (array/create-array (float-array [5.0 6.0 7.0 8.0]) [2 2] defs/AF_DTYPE_F32)
           result (blas/matmul-nt a b)
           buf (mem/alloc (* 4 4))]
       (is (instance? AFArray result))
       (is (= [2 2 1 1] (array/get-dims result)))
       (array/get-data-ptr result buf)
       ;; Column-major: A * B^T where B^T = [[5 7][6 8]]
       ;; First element: 1*5 + 3*7 = 26
       (is (approx= 26.0 (mem/read-float buf 0) 0.001))))))

(deftest test-matmul-tn
  (testing "matmul-tn multiplies A^T * B"
    (device/init!)
    (releasing!
     (let [a (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] defs/AF_DTYPE_F32)
           b (array/create-array (float-array [5.0 6.0 7.0 8.0]) [2 2] defs/AF_DTYPE_F32)
           result (blas/matmul-tn a b)
           buf (mem/alloc (* 4 4))]
       (is (instance? AFArray result))
       (is (= [2 2 1 1] (array/get-dims result)))
       (array/get-data-ptr result buf)
       ;; Column-major: A^T * B where A^T = [[1 3][2 4]]
       ;; First element: 1*5 + 2*6 = 17
       (is (approx= 17.0 (mem/read-float buf 0) 0.001))))))

(deftest test-matmul-tt
  (testing "matmul-tt multiplies A^T * B^T"
    (device/init!)
    (releasing!
     (let [a (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] defs/AF_DTYPE_F32)
           b (array/create-array (float-array [5.0 6.0 7.0 8.0]) [2 2] defs/AF_DTYPE_F32)
           result (blas/matmul-tt a b)
           buf (mem/alloc (* 4 4))]
       (is (instance? AFArray result))
       (is (= [2 2 1 1] (array/get-dims result)))
       (array/get-data-ptr result buf)
       ;; Column-major: A^T * B^T
       ;; First element: 1*5 + 2*7 = 19
       (is (approx= 19.0 (mem/read-float buf 0) 0.001))))))

(deftest test-matmul-nt-rectangle
  (testing "matmul-nt with rectangular matrices"
    (device/init!)
    (releasing!
     (let [a (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0 6.0]) [2 3] defs/AF_DTYPE_F32)
           b (array/create-array (float-array [7.0 8.0 9.0 10.0 11.0 12.0]) [2 3] defs/AF_DTYPE_F32)
           result (blas/matmul-nt a b)]
       (is (instance? AFArray result))
       ;; A is [2x3], B is [2x3], B^T is [3x2], result is [2x2]
       (is (= [2 2 1 1] (array/get-dims result)))))))

(deftest test-matmul3
  (testing "Chain matrix multiplication with 3 matrices"
    (device/init!)
    (releasing!
     (let [a (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] defs/AF_DTYPE_F32)
           b (array/create-array (float-array [2.0 0.0 0.0 2.0]) [2 2] defs/AF_DTYPE_F32)
           c (array/create-array (float-array [1.0 1.0 1.0 1.0]) [2 2] defs/AF_DTYPE_F32)
           result (blas/matmul3 a b c)]
       (is (= 2 (array/get-numdims result)))
       (is (= [2 2] (take 2 (array/get-dims result))))
       ;; Result should be (A * B) * C = ([2 4 6 8]) * C
       ))))

(deftest test-matmul4
  (testing "Chain matrix multiplication with 4 matrices"
    (device/init!)
    (releasing!
     (let [a (array/create-array (float-array [1.0 0.0 0.0 1.0]) [2 2] defs/AF_DTYPE_F32)  ; Identity
           b (array/create-array (float-array [2.0 0.0 0.0 2.0]) [2 2] defs/AF_DTYPE_F32)  ; Scale by 2
           c (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] defs/AF_DTYPE_F32)
           d (array/create-array (float-array [1.0 0.0 0.0 1.0]) [2 2] defs/AF_DTYPE_F32)  ; Identity
           result (blas/matmul4 a b c d)]
       (is (= 2 (array/get-numdims result)))
       (is (= [2 2] (take 2 (array/get-dims result))))
       ;; Result should be I * 2I * C * I = 2C
       ))))

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
  (run-test test-matmul-nt)
  (run-test test-matmul-tn)
  (run-test test-matmul-tt)
  (run-test test-matmul-nt-rectangle)
  (run-test test-matmul3)
  (run-test test-matmul4)

  ;
  )
