(ns org.soulspace.arrayfire.api.sparse-test
  "Tests for the arrayfire-clj sparse matrix API namespace."
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [org.soulspace.arrayfire.ffi.base.definitions :as ffi-defs] 
            [org.soulspace.arrayfire.integration.base.memory :as bmem]
            [org.soulspace.arrayfire.integration.unified-api.array :as array]
            [org.soulspace.arrayfire.api.core   :as core]
            [org.soulspace.arrayfire.api.sparse  :as sparse]))

;;;
;;; Guard tests
;;;

(deftest sparse-functions-require-region-test
  (testing "from-dense throws IllegalStateException outside a region"
    (is (thrown? IllegalStateException
          (let [arr (core/with-arrayfire {:backend :cpu}
                      (core/array [1.0 0.0 0.0
                                   0.0 2.0 0.0
                                   0.0 0.0 3.0] [3 3]))]
            (sparse/from-dense arr))))))

;;;
;;; from-dense / to-dense roundtrip
;;;

(deftest from-dense-to-dense-roundtrip-test
  (testing "from-dense and to-dense form an identity roundtrip"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a     (core/array [1.0 0.0 0.0
                                            0.0 2.0 0.0
                                            0.0 0.0 3.0] [3 3])
                         sp    (sparse/from-dense a :coo)
                         dense (sparse/to-dense sp)]
                     (core/->value dense)))]
      ;; Column-major layout: col0=[1 0 0], col1=[0 2 0], col2=[0 0 3]
      (is (= 3 (count result)))
      (is (<= (Math/abs (- 1.0 (get-in result [0 0]))) 0.001))
      (is (<= (Math/abs (- 2.0 (get-in result [1 1]))) 0.001))
      (is (<= (Math/abs (- 3.0 (get-in result [2 2]))) 0.001)))))

;;;
;;; sparse-nnz tests
;;;

(deftest sparse-nnz-diagonal-test
  (testing "sparse-nnz counts non-zero elements correctly"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a  (core/array [1.0 0.0 0.0
                                         0.0 2.0 0.0
                                         0.0 0.0 3.0] [3 3])
                         sp (sparse/from-dense a :coo)]
                     (sparse/sparse-nnz sp)))]
      (is (= 3 result)))))

(deftest sparse-nnz-all-zeros-test
  (testing "sparse-nnz returns 0 for a zero matrix"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a  (core/zeros [3 3])
                         sp (sparse/from-dense a :coo)]
                     (sparse/sparse-nnz sp)))]
      (is (= 0 result)))))

;;;
;;; sparse-storage tests
;;;

(deftest sparse-storage-coo-test
  (testing "sparse-storage returns :coo for COO arrays"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a  (core/array [1.0 0.0 2.0
                                         0.0 3.0 0.0] [2 3])
                         sp (sparse/from-dense a :coo)]
                     (sparse/sparse-storage sp)))]
      (is (= :coo result)))))

(deftest sparse-storage-csr-test
  (testing "sparse-storage returns :csr for CSR arrays"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a   (core/array [1.0 0.0 2.0
                                          0.0 3.0 0.0] [2 3])
                         sp  (sparse/from-dense a :coo)
                         csr (sparse/convert-sparse sp :csr)]
                     (sparse/sparse-storage csr)))]
      (is (= :csr result)))))

;;;
;;; convert-sparse tests
;;;

(deftest convert-sparse-coo-to-csr-test
  (testing "convert-sparse converts from COO to CSR preserving non-zeros"
    (let [[nnz-before nnz-after storage]
          (core/with-arrayfire {:backend :cpu}
            (let [a   (core/array [1.0 0.0 0.0
                                   0.0 2.0 0.0
                                   0.0 0.0 3.0] [3 3])
                  coo (sparse/from-dense a :coo)
                  csr (sparse/convert-sparse coo :csr)]
              [(sparse/sparse-nnz coo)
               (sparse/sparse-nnz csr)
               (sparse/sparse-storage csr)]))]
      (is (= nnz-before nnz-after))
      (is (= :csr storage)))))

;;;
;;; sparse-values / sparse-row-indices / sparse-col-indices
;;;

(deftest sparse-values-test
  (testing "sparse-values extracts the non-zero values array"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a  (core/array [1.0 0.0
                                         0.0 2.0] [2 2])
                         sp (sparse/from-dense a :coo)]
                     (core/->value (sparse/sparse-values sp))))]
      ;; Non-zero values should be a sequence of length 2 containing 1.0 and 2.0
      (is (= 2 (count result)))
      (is (some #(<= (Math/abs (- 1.0 %)) 0.001) result))
      (is (some #(<= (Math/abs (- 2.0 %)) 0.001) result)))))

(deftest sparse-row-indices-test
  (testing "sparse-row-indices returns a 1D index array of correct length"
    (let [nnz-count
          (core/with-arrayfire {:backend :cpu}
            (let [a  (core/array [1.0 0.0 2.0
                                  0.0 3.0 0.0] [2 3])
                  sp (sparse/from-dense a :coo)]
              (count (core/->value (sparse/sparse-row-indices sp)))))]
      ;; COO format: row-indices has as many elements as nnz
      (is (= 3 nnz-count)))))

(deftest sparse-col-indices-test
  (testing "sparse-col-indices returns a 1D index array of correct length"
    (let [nnz-count
          (core/with-arrayfire {:backend :cpu}
            (let [a  (core/array [1.0 0.0 2.0
                                  0.0 3.0 0.0] [2 3])
                  sp (sparse/from-dense a :coo)]
              (count (core/->value (sparse/sparse-col-indices sp)))))]
      ;; COO format: col-indices has as many elements as nnz
      (is (= 3 nnz-count)))))

;;;
;;; create-sparse tests
;;;

(deftest create-sparse-produces-correct-nnz-test
  (testing "create-sparse builds a sparse matrix with the correct number of non-zeros"
    (let [nnz-result (core/with-arrayfire {:backend :cpu}
                       (let [vals (core/array [1.0 2.0 3.0] [3])
                             rows (array/create-array (bmem/int-array->segment (int-array [0 1 2])) [3] ffi-defs/AF_DTYPE_S32)
                             cols (array/create-array (bmem/int-array->segment (int-array [0 1 2])) [3] ffi-defs/AF_DTYPE_S32)
                             sp   (sparse/create-sparse 3 3 vals rows cols :coo)]
                         (sparse/sparse-nnz sp)))]
      (is (= 3 nnz-result)))))

(deftest create-sparse-roundtrip-test
  (testing "create-sparse → to-dense roundtrip preserves diagonal values"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [vals  (core/array [5.0 6.0 7.0] [3])
                         rows  (array/create-array (bmem/int-array->segment (int-array [0 1 2])) [3] ffi-defs/AF_DTYPE_S32)
                         cols  (array/create-array (bmem/int-array->segment (int-array [0 1 2])) [3] ffi-defs/AF_DTYPE_S32)
                         sp    (sparse/create-sparse 3 3 vals rows cols :coo)
                         dense (sparse/to-dense sp)]
                     (core/->value dense)))]
      ;; column-major: [0][0]=5.0, [1][1]=6.0, [2][2]=7.0
      (is (<= (Math/abs (- 5.0 (get-in result [0 0]))) 0.001))
      (is (<= (Math/abs (- 6.0 (get-in result [1 1]))) 0.001))
      (is (<= (Math/abs (- 7.0 (get-in result [2 2]))) 0.001)))))

;;;
;;; resolve-storage keyword validation
;;;

(deftest invalid-storage-keyword-throws-test
  (testing "resolve-storage throws ExceptionInfo for unknown storage keyword"
    (is (thrown? clojure.lang.ExceptionInfo
          (core/with-arrayfire {:backend :cpu}
            (let [a (core/array [1.0 0.0 0.0] [1 3])]
              (sparse/from-dense a :unknown-format)))))))

(comment
  ;; Run sparse tests interactively
  (run-tests)

  ;
  )
