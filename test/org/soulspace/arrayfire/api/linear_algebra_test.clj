(ns org.soulspace.arrayfire.api.linear-algebra-test
  "Tests for the idiomatic Clojure linear algebra API.
   All tests run inside (with-arrayfire ...) regions."
  (:require [clojure.test :refer [deftest is testing]]
            [org.soulspace.arrayfire.api.core :as af]
            [org.soulspace.arrayfire.api.linear-algebra :as la]))

;;;
;;; Helpers
;;;

(defn- approx=
  "Returns true if a and b are within tol of each other."
  ([a b] (approx= a b 1e-6))
  ([a b tol]
   (< (Math/abs (double (- a b))) (double tol))))

(defn- seq-approx=
  "Returns true if every element pair in two flat sequences is approximately equal."
  ([xs ys] (seq-approx= xs ys 1e-5))
  ([xs ys tol]
   (and (= (count xs) (count ys))
        (every? true? (map #(approx= %1 %2 tol) xs ys)))))

;;;
;;; lapack-available?
;;;

(deftest lapack-available?-test
  (testing "LAPACK reports availability as boolean"
    (let [result (af/with-arrayfire (la/lapack-available?))]
      (is (boolean? result)))))

;;;
;;; Transpose
;;;

(deftest transpose-test
  (testing "Transpose of a 2×3 matrix yields a 3×2 matrix"
    ;; A = [[1 2 3][4 5 6]]; col-major: [1 4 2 5 3 6]
    (let [result (af/with-arrayfire
                   (let [a (af/array [1.0 4.0 2.0 5.0 3.0 6.0] [2 3] :f64)]
                     (af/shape (la/transpose a))))]
      (is (= [3 2 1 1] result))))
  (testing "Transpose of a 2×2 matrix is correct"
    ;; A = [[1 2][3 4]]; col-major: [1 3 2 4]; A^T col-major: [1 2 3 4] → [[1 3][2 4]]
    (let [result (af/with-arrayfire
                   (let [a (af/array [1.0 3.0 2.0 4.0] [2 2] :f64)]
                     (af/->value (la/transpose a))))]
      (is (seq-approx= (flatten result) [1.0 2.0 3.0 4.0])))))

;;;
;;; Conjugate transpose
;;;

(deftest transpose-conjugate-test
  (testing "Conjugate-transpose of a real 2×3 matrix equals regular transpose (imag=0)"
    ;; For real arrays conjugation is a no-op, so result = regular transpose
    (let [shape-result (af/with-arrayfire
                         (let [a (af/array [1.0 4.0 2.0 5.0 3.0 6.0] [2 3] :f64)]
                           (af/shape (la/transpose-conjugate a))))]
      (is (= [3 2 1 1] shape-result))))
  (testing "Conjugate-transpose of a real 2×2 matrix has same values as regular transpose"
    ;; A = [[1 2][3 4]] col-major: [1 3 2 4]
    (let [result (af/with-arrayfire
                   (let [a (af/array [1.0 3.0 2.0 4.0] [2 2] :f64)]
                     (af/->value (la/transpose-conjugate a))))]
      (is (seq-approx= (flatten result) [1.0 2.0 3.0 4.0])))))

(deftest transpose-conjugate!-test
  (testing "In-place conjugate-transpose of a square real matrix has same values as transpose"
    ;; A = [[1 2][3 4]] col-major: [1 3 2 4] — square, so in-place is valid
    (let [result (af/with-arrayfire
                   (let [a (af/array [1.0 3.0 2.0 4.0] [2 2] :f64)]
                     (af/->value (la/transpose-conjugate! a))))]
      (is (seq-approx= (flatten result) [1.0 2.0 3.0 4.0])))))

;;;
;;; GEMM
;;;

(deftest gemm-test
  (testing "gemm: 2*A*I = 2*A"
    ;; A = [[1 2][3 4]] col-major: [1 3 2 4]; I = [[1 0][0 1]] col-major: [1 0 0 1]
    (let [result (af/with-arrayfire
                   (let [a (af/array [1.0 3.0 2.0 4.0] [2 2] :f64)
                         i (af/array [1.0 0.0 0.0 1.0] [2 2] :f64)]
                     (af/->value (la/gemm :none :none 2.0 a i 0.0))))]
      (is (seq-approx= (flatten result) [2.0 6.0 4.0 8.0]))))
  (testing "gemm: :trans resolves correctly — 1*A^T*I = A^T"
    ;; A = [[1 2][3 4]] col-major: [1 3 2 4]
    ;; A^T = [[1 3][2 4]] — col0: [1,2], col1: [3,4]
    ;; Verifies :trans mat-prop resolves to AF_MAT_TRANS (1), not :upper (32)
    (let [result (af/with-arrayfire
                   (let [a (af/array [1.0 3.0 2.0 4.0] [2 2] :f64)
                         i (af/array [1.0 0.0 0.0 1.0] [2 2] :f64)]
                     (af/->value (la/gemm :trans :none 1.0 a i 0.0))))]
      ;; col-major result: col0 = [1.0, 2.0], col1 = [3.0, 4.0]
      (is (seq-approx= (flatten result) [1.0 2.0 3.0 4.0])))))

;;;
;;; dot-all
;;;

(deftest dot-all-test
  (testing "dot product of [1 2 3] and [4 5 6] = 32"
    (let [result (af/with-arrayfire
                   (let [u (af/array [1.0 2.0 3.0] [3] :f64)
                         v (af/array [4.0 5.0 6.0] [3] :f64)]
                     (la/dot-all u v)))]
      (is (approx= result 32.0)))))

;;;
;;; LU decomposition
;;;

(deftest lu-test
  (testing "LU decomposition returns named map with :lower :upper :pivot keys"
    (let [result (af/with-arrayfire
                   (let [a (af/array [4.0 6.0 3.0 3.0] [2 2] :f64)]
                     (la/lu a)))]
      (is (map? result))
      (is (contains? result :lower))
      (is (contains? result :upper))
      (is (contains? result :pivot))))
  (testing "LU: L*U ≈ P*A (shapes are consistent)"
    (let [result (af/with-arrayfire
                   (let [a (af/array [4.0 6.0 3.0 3.0] [2 2] :f64)
                         {:keys [lower upper]} (la/lu a)]
                     {:lower-shape (af/shape lower)
                      :upper-shape (af/shape upper)}))]
      (is (= [2 2 1 1] (:lower-shape result)))
      (is (= [2 2 1 1] (:upper-shape result))))))

;;;
;;; QR decomposition
;;;

(deftest qr-test
  (testing "QR decomposition returns named map with :q :r :tau keys"
    (let [result (af/with-arrayfire
                   (let [a (af/array [1.0 3.0 5.0 2.0 4.0 6.0] [3 2] :f64)]
                     (la/qr a)))]
      (is (map? result))
      (is (contains? result :q))
      (is (contains? result :r))
      (is (contains? result :tau))))
  (testing "QR: Q shape is square (m×m)"
    (let [result (af/with-arrayfire
                   (let [a (af/array [1.0 3.0 5.0 2.0 4.0 6.0] [3 2] :f64)
                         {:keys [q]} (la/qr a)]
                     (af/shape q)))]
      (is (= 3 (first result)))
      (is (= 3 (second result))))))

;;;
;;; SVD
;;;

(deftest svd-test
  (testing "SVD returns named map with :u :s :vt keys"
    (let [result (af/with-arrayfire
                   (let [a (af/array [1.0 4.0 2.0 5.0 3.0 6.0] [2 3] :f64)]
                     (la/svd a)))]
      (is (map? result))
      (is (contains? result :u))
      (is (contains? result :s))
      (is (contains? result :vt))))
  (testing "SVD: singular values are positive and in descending order"
    (let [result (af/with-arrayfire
                   (let [a (af/array [1.0 4.0 2.0 5.0 3.0 6.0] [2 3] :f64)
                         {:keys [s]} (la/svd a)]
                     (vec (af/->value s))))]
      (is (every? pos? result))
      (is (apply >= result)))))

;;;
;;; Cholesky decomposition
;;;

(deftest cholesky-test
  (testing "Cholesky of SPD matrix returns :result and :info=0"
    ;; A = [[4 2][2 3]] col-major: [4 2 2 3]
    (let [result (af/with-arrayfire
                   (let [a (af/array [4.0 2.0 2.0 3.0] [2 2] :f64)]
                     (la/cholesky a)))]
      (is (map? result))
      (is (= 0 (:info result)))
      (is (contains? result :result))))
  (testing "Cholesky lower factor: first column ≈ [2.0, 1.0]"
    (let [result (af/with-arrayfire
                   (let [a (af/array [4.0 2.0 2.0 3.0] [2 2] :f64)
                         {:keys [result]} (la/cholesky a)]
                     (af/->value result)))]
      ;; col-major: col0 = [2.0, 1.0], col1 = [0.0, ~1.414]
      (is (approx= (ffirst result) 2.0))
      (is (approx= (second (first result)) 1.0)))))

;;;
;;; Solve
;;;

(deftest solve-test
  (testing "Solve Ax=b: [[2 1][1 3]] x = [5 10] → x ≈ [1 3]"
    ;; col-major A: [2 1 1 3]
    (let [result (af/with-arrayfire
                   (let [a (af/array [2.0 1.0 1.0 3.0] [2 2] :f64)
                         b (af/array [5.0 10.0] [2] :f64)]
                     (af/->value (la/solve a b))))]
      (is (seq-approx= (flatten result) [1.0 3.0] 1e-5))))
  (testing "Solve with :none option"
    (let [result (af/with-arrayfire
                   (let [a (af/array [2.0 1.0 1.0 3.0] [2 2] :f64)
                         b (af/array [5.0 10.0] [2] :f64)]
                     (af/->value (la/solve a b :none))))]
      (is (seq-approx= (flatten result) [1.0 3.0] 1e-5)))))

;;;
;;; Inverse
;;;

(deftest inverse-test
  (testing "Inverse of [[1 2][3 4]] ≈ [[-2 1.5][1 -0.5]]"
    ;; col-major A: [1 3 2 4]; inverse col-major: [-2 1 1.5 -0.5] → [[-2 1.5][1 -0.5]]
    (let [result (af/with-arrayfire
                   (let [a (af/array [1.0 3.0 2.0 4.0] [2 2] :f64)]
                     (af/->value (la/inverse a))))]
      (is (seq-approx= (flatten result) [-2.0 1.5 1.0 -0.5] 1e-5)))))

;;;
;;; Pseudo-inverse
;;;

(deftest pinverse-test
  (testing "Pseudo-inverse of a 2×3 matrix has shape [3 2]"
    (let [result (af/with-arrayfire
                   (let [a (af/array [1.0 4.0 2.0 5.0 3.0 6.0] [2 3] :f64)]
                     (af/shape (la/pinverse a))))]
      (is (= 3 (first result)))
      (is (= 2 (second result))))))

;;;
;;; Determinant
;;;

(deftest det-test
  (testing "det([[1 2][3 4]]) = -2"
    (let [result (af/with-arrayfire
                   (let [a (af/array [1.0 3.0 2.0 4.0] [2 2] :f64)]
                     (la/det a)))]
      (is (approx= result -2.0))))
  (testing "det of identity matrix = 1.0"
    (let [result (af/with-arrayfire
                   (let [i (af/array [1.0 0.0 0.0 1.0] [2 2] :f64)]
                     (la/det i)))]
      (is (approx= result 1.0)))))

;;;
;;; Matrix rank
;;;

(deftest matrix-rank-test
  (testing "Full-rank 2×2 matrix has rank 2"
    (let [result (af/with-arrayfire
                   (let [a (af/array [1.0 3.0 2.0 4.0] [2 2] :f64)]
                     (la/matrix-rank a)))]
      (is (= 2 result))))
  (testing "Rank-deficient 3×3 matrix (row2 = 2*row1) has rank 2"
    ;; A = [[1 2 3][2 4 6][1 1 1]] col-major: [1 2 1  2 4 1  3 6 1]
    (let [result (af/with-arrayfire
                   (let [a (af/array [1.0 2.0 1.0 2.0 4.0 1.0 3.0 6.0 1.0] [3 3] :f64)]
                     (la/matrix-rank a)))]
      (is (= 2 result)))))

;;;
;;; Norm
;;;

(deftest norm-vector-test
  (testing "L1 norm of [3 4] = 7.0"
    (let [result (af/with-arrayfire
                   (let [v (af/array [3.0 4.0] [2] :f64)]
                     (la/norm v :vector-1)))]
      (is (approx= result 7.0))))
  (testing "L2 norm of [3 4] = 5.0"
    (let [result (af/with-arrayfire
                   (let [v (af/array [3.0 4.0] [2] :f64)]
                     (la/norm v :vector-2)))]
      (is (approx= result 5.0))))
  (testing "L-inf norm of [3 4] = 4.0"
    (let [result (af/with-arrayfire
                   (let [v (af/array [3.0 4.0] [2] :f64)]
                     (la/norm v :vector-inf)))]
      (is (approx= result 4.0)))))

(deftest norm-matrix-test
  (testing "Matrix 1-norm of [[1 2][3 4]] = max col-sum = max(1+3, 2+4) = 6.0"
    ;; col-major: [1 3 2 4]
    (let [result (af/with-arrayfire
                   (let [m (af/array [1.0 3.0 2.0 4.0] [2 2] :f64)]
                     (la/norm m :matrix-1)))]
      (is (approx= result 6.0))))
  (testing "Matrix inf-norm of [[1 2][3 4]] = max row-sum = max(1+2, 3+4) = 7.0"
    ;; col-major: [1 3 2 4]
    (let [result (af/with-arrayfire
                   (let [m (af/array [1.0 3.0 2.0 4.0] [2 2] :f64)]
                     (la/norm m :matrix-inf)))]
      (is (approx= result 7.0)))))

(comment
  ;; Run the tests interactively
  (require '[org.soulspace.arrayfire.api.linear-algebra-test] :reload)
  (clojure.test/run-tests 'org.soulspace.arrayfire.api.linear-algebra-test))
