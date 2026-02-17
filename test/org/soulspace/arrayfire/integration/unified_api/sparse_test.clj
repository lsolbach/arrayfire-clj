(ns org.soulspace.arrayfire.integration.unified-api.sparse-test
  (:require [clojure.test :refer [deftest is testing run-test run-tests]]
            [org.soulspace.arrayfire.ffi.base.definitions :as defs]
            [org.soulspace.arrayfire.integration.base.resource :as res]
            [org.soulspace.arrayfire.integration.unified-api.sparse :as sparse]
            [org.soulspace.arrayfire.integration.unified-api.array :as array]
            [org.soulspace.arrayfire.integration.unified-api.device :as device]
            [tech.v3.resource :refer [releasing!]])
  (:import [org.soulspace.arrayfire.integration.base.resource AFArray]))

;;;
;;; Sparse Matrix Creation Tests
;;;

(deftest test-create-coo
  (testing "create sparse matrix in COO format"
    (device/init!)
    (releasing!
      (let [values (array/create-array (float-array [1.0 2.0 3.0]) [3] defs/AF_DTYPE_F32)
            rows (array/create-array (int-array [0 1 2]) [3] defs/AF_DTYPE_S32)
            cols (array/create-array (int-array [0 1 2]) [3] defs/AF_DTYPE_S32)
            sparse-mat (sparse/create 3 3 values rows cols defs/AF_STORAGE_COO)]
        (is (instance? AFArray sparse-mat))
        (is (= 3 (sparse/nnz sparse-mat)))
        (is (= defs/AF_STORAGE_COO (sparse/storage-format sparse-mat)))))))

(deftest test-create-csr
  (testing "create sparse matrix in CSR format"
    (device/init!)
    (releasing!
      (let [values (array/create-array (float-array [1.0 2.0 3.0]) [3] defs/AF_DTYPE_F32)
            rows (array/create-array (int-array [0 1 2 3]) [4] defs/AF_DTYPE_S32) ; rowPtr has nRows+1 elements
            cols (array/create-array (int-array [0 1 2]) [3] defs/AF_DTYPE_S32)
            sparse-mat (sparse/create 3 3 values rows cols defs/AF_STORAGE_CSR)]
        (is (instance? AFArray sparse-mat))
        (is (= defs/AF_STORAGE_CSR (sparse/storage-format sparse-mat)))))))

(deftest test-create-csc
  (testing "create sparse matrix in CSC format"
    (device/init!)
    (releasing!
      (let [values (array/create-array (float-array [1.0 2.0 3.0]) [3] defs/AF_DTYPE_F32)
            rows (array/create-array (int-array [0 1 2]) [3] defs/AF_DTYPE_S32)
            cols (array/create-array (int-array [0 1 2 3]) [4] defs/AF_DTYPE_S32) ; colPtr has nCols+1 elements
            sparse-mat (sparse/create 3 3 values rows cols defs/AF_STORAGE_CSC)]
        (is (instance? AFArray sparse-mat))
        (is (= defs/AF_STORAGE_CSC (sparse/storage-format sparse-mat)))))))

;;;
;;; Dense to Sparse Conversion Tests
;;;

(deftest test-from-dense-coo
  (testing "convert dense matrix to COO sparse"
    (device/init!)
    (releasing!
      (let [dense (array/create-array (float-array [1.0 0.0 0.0
                                                     0.0 2.0 0.0
                                                     0.0 0.0 3.0]) [3 3] defs/AF_DTYPE_F32)
            sparse-mat (sparse/from-dense dense defs/AF_STORAGE_COO)]
        (is (instance? AFArray sparse-mat))
        (is (= 3 (sparse/nnz sparse-mat)))
        (is (= defs/AF_STORAGE_COO (sparse/storage-format sparse-mat)))))))

(deftest test-from-dense-csr
  (testing "convert dense matrix to CSR sparse"
    (device/init!)
    (releasing!
      (let [dense (array/create-array (float-array [1.0 2.0 0.0
                                                     0.0 3.0 0.0]) [2 3] defs/AF_DTYPE_F32)
            sparse-mat (sparse/from-dense dense defs/AF_STORAGE_CSR)]
        (is (instance? AFArray sparse-mat))
        (is (= 3 (sparse/nnz sparse-mat)))
        (is (= defs/AF_STORAGE_CSR (sparse/storage-format sparse-mat)))))))

;;;
;;; Format Conversion Tests
;;;

(deftest test-convert-to-csr
  (testing "convert COO to CSR format"
    (device/init!)
    (releasing!
      (let [values (array/create-array (float-array [1.0 2.0 3.0]) [3] defs/AF_DTYPE_F32)
            rows (array/create-array (int-array [0 1 2]) [3] defs/AF_DTYPE_S32)
            cols (array/create-array (int-array [0 1 2]) [3] defs/AF_DTYPE_S32)
            coo-mat (sparse/create 3 3 values rows cols defs/AF_STORAGE_COO)
            csr-mat (sparse/convert-to coo-mat defs/AF_STORAGE_CSR)]
        (is (instance? AFArray csr-mat))
        (is (= defs/AF_STORAGE_CSR (sparse/storage-format csr-mat)))
        (is (= 3 (sparse/nnz csr-mat)))))))

(deftest test-convert-to-coo
  (testing "convert CSR to COO format"
    (device/init!)
    (releasing!
      (let [dense (array/create-array (float-array [1.0 0.0
                                                     0.0 2.0]) [2 2] defs/AF_DTYPE_F32)
            csr-mat (sparse/from-dense dense defs/AF_STORAGE_CSR)
            coo-mat (sparse/convert-to csr-mat defs/AF_STORAGE_COO)]
        (is (instance? AFArray coo-mat))
        (is (= defs/AF_STORAGE_COO (sparse/storage-format coo-mat)))))))

;;;
;;; Sparse to Dense Conversion Tests
;;;

(deftest test-to-dense
  (testing "convert sparse matrix back to dense"
    (device/init!)
    (releasing!
      (let [dense-orig (array/create-array (float-array [1.0 0.0
                                                          0.0 2.0]) [2 2] defs/AF_DTYPE_F32)
            sparse-mat (sparse/from-dense dense-orig defs/AF_STORAGE_COO)
            dense-result (sparse/to-dense sparse-mat)]
        (is (instance? AFArray dense-result))
        (is (= [2 2] (take 2 (array/get-dims dense-result))))))))

;;;
;;; Sparse Matrix Information Tests
;;;

(deftest test-info
  (testing "get sparse matrix information"
    (device/init!)
    (releasing!
      (let [values (array/create-array (float-array [1.0 2.0 3.0]) [3] defs/AF_DTYPE_F32)
            rows (array/create-array (int-array [0 1 2]) [3] defs/AF_DTYPE_S32)
            cols (array/create-array (int-array [0 1 2]) [3] defs/AF_DTYPE_S32)
            sparse-mat (sparse/create 3 3 values rows cols defs/AF_STORAGE_COO)
            info (sparse/info sparse-mat)]
        (is (map? info))
        (is (contains? info :nrows))
        (is (contains? info :ncols))
        (is (contains? info :nnz))
        (is (contains? info :storage))
        (is (= 3 (:nrows info)))
        (is (= 3 (:ncols info)))
        (is (= 3 (:nnz info)))
        (is (= defs/AF_STORAGE_COO (:storage info)))))))

;;;
;;; Component Extraction Tests
;;;

(deftest test-values
  (testing "extract values array from sparse matrix"
    (device/init!)
    (releasing!
      (let [vals (array/create-array (float-array [1.0 2.0 3.0]) [3] defs/AF_DTYPE_F32)
            rows (array/create-array (int-array [0 1 2]) [3] defs/AF_DTYPE_S32)
            cols (array/create-array (int-array [0 1 2]) [3] defs/AF_DTYPE_S32)
            sparse-mat (sparse/create 3 3 vals rows cols defs/AF_STORAGE_COO)
            extracted-vals (sparse/values sparse-mat)]
        (is (instance? AFArray extracted-vals))
        (is (= 3 (array/get-elements extracted-vals)))))))

(deftest test-row-indices
  (testing "extract row indices from sparse matrix"
    (device/init!)
    (releasing!
      (let [values (array/create-array (float-array [1.0 2.0 3.0]) [3] defs/AF_DTYPE_F32)
            rows (array/create-array (int-array [0 1 2]) [3] defs/AF_DTYPE_S32)
            cols (array/create-array (int-array [0 1 2]) [3] defs/AF_DTYPE_S32)
            sparse-mat (sparse/create 3 3 values rows cols defs/AF_STORAGE_COO)
            extracted-rows (sparse/row-indices sparse-mat)]
        (is (instance? AFArray extracted-rows))
        (is (= 3 (array/get-elements extracted-rows)))))))

(deftest test-col-indices
  (testing "extract column indices from sparse matrix"
    (device/init!)
    (releasing!
      (let [values (array/create-array (float-array [1.0 2.0 3.0]) [3] defs/AF_DTYPE_F32)
            rows (array/create-array (int-array [0 1 2]) [3] defs/AF_DTYPE_S32)
            cols (array/create-array (int-array [0 1 2]) [3] defs/AF_DTYPE_S32)
            sparse-mat (sparse/create 3 3 values rows cols defs/AF_STORAGE_COO)
            extracted-cols (sparse/col-indices sparse-mat)]
        (is (instance? AFArray extracted-cols))
        (is (= 3 (array/get-elements extracted-cols)))))))

;;;
;;; Non-Zero Count Tests
;;;

(deftest test-nnz
  (testing "count non-zero elements in sparse matrix"
    (device/init!)
    (releasing!
      (let [dense (array/create-array (float-array [1.0 0.0 2.0
                                                     0.0 0.0 3.0]) [2 3] defs/AF_DTYPE_F32)
            sparse-mat (sparse/from-dense dense defs/AF_STORAGE_COO)]
        (is (= 3 (sparse/nnz sparse-mat)))))))

(deftest test-nnz-empty
  (testing "nnz returns 0 for matrix with all zeros"
    (device/init!)
    (releasing!
      (let [dense (array/create-array (float-array [0.0 0.0 0.0 0.0]) [2 2] defs/AF_DTYPE_F32)
            sparse-mat (sparse/from-dense dense defs/AF_STORAGE_COO)]
        (is (= 0 (sparse/nnz sparse-mat)))))))

;;;
;;; Storage Format Tests
;;;

(deftest test-storage-format-coo
  (testing "storage-format returns COO"
    (device/init!)
    (releasing!
      (let [values (array/create-array (float-array [1.0]) [1] defs/AF_DTYPE_F32)
            rows (array/create-array (int-array [0]) [1] defs/AF_DTYPE_S32)
            cols (array/create-array (int-array [0]) [1] defs/AF_DTYPE_S32)
            sparse-mat (sparse/create 1 1 values rows cols defs/AF_STORAGE_COO)]
        (is (= defs/AF_STORAGE_COO (sparse/storage-format sparse-mat)))))))

(deftest test-storage-format-csr
  (testing "storage-format returns CSR"
    (device/init!)
    (releasing!
      (let [dense (array/create-array (float-array [1.0]) [1 1] defs/AF_DTYPE_F32)
            sparse-mat (sparse/from-dense dense defs/AF_STORAGE_CSR)]
        (is (= defs/AF_STORAGE_CSR (sparse/storage-format sparse-mat)))))))

;;;
;;; Integration Tests
;;;

(deftest test-dense-sparse-dense-roundtrip
  (testing "dense -> sparse -> dense roundtrip preserves data"
    (device/init!)
    (releasing!
      (let [original (array/create-array (float-array [1.0 0.0
                                                        0.0 2.0]) [2 2] defs/AF_DTYPE_F32)
            sparse-mat (sparse/from-dense original defs/AF_STORAGE_COO)
            reconstructed (sparse/to-dense sparse-mat)]
        (is (= (array/get-dims original) (array/get-dims reconstructed)))))))

(deftest test-format-conversion-preserves-data
  (testing "converting between formats preserves non-zero count"
    (device/init!)
    (releasing!
      (let [dense (array/create-array (float-array [1.0 2.0 0.0
                                                     0.0 3.0 0.0]) [2 3] defs/AF_DTYPE_F32)
            coo-mat (sparse/from-dense dense defs/AF_STORAGE_COO)
            csr-mat (sparse/convert-to coo-mat defs/AF_STORAGE_CSR)]
        (is (= (sparse/nnz coo-mat) (sparse/nnz csr-mat)))))))

(deftest test-sparse-matrix-dimensions
  (testing "sparse matrix maintains correct dimensions"
    (device/init!)
    (releasing!
      (let [values (array/create-array (float-array [1.0 2.0]) [2] defs/AF_DTYPE_F32)
            rows (array/create-array (int-array [0 2]) [2] defs/AF_DTYPE_S32)
            cols (array/create-array (int-array [1 3]) [2] defs/AF_DTYPE_S32)
            sparse-mat (sparse/create 5 5 values rows cols defs/AF_STORAGE_COO)
            info (sparse/info sparse-mat)]
        (is (= 5 (:nrows info)))
        (is (= 5 (:ncols info)))
        (is (= 2 (:nnz info)))))))

(comment
  ;; run all tests from REPL
  (run-tests)
  
  ;; run individual tests - Creation
  (run-test test-create-coo)
  (run-test test-create-csr)
  (run-test test-create-csc)
  
  ;; run individual tests - Dense to Sparse
  (run-test test-from-dense-coo)
  (run-test test-from-dense-csr)
  
  ;; run individual tests - Format Conversion
  (run-test test-convert-to-csr)
  (run-test test-convert-to-coo)
  
  ;; run individual tests - Sparse to Dense
  (run-test test-to-dense)
  
  ;; run individual tests - Information
  (run-test test-info)
  
  ;; run individual tests - Component Extraction
  (run-test test-values)
  (run-test test-row-indices)
  (run-test test-col-indices)
  
  ;; run individual tests - Non-Zero Count
  (run-test test-nnz)
  (run-test test-nnz-empty)
  
  ;; run individual tests - Storage Format
  (run-test test-storage-format-coo)
  (run-test test-storage-format-csr)
  
  ;; run individual tests - Integration
  (run-test test-dense-sparse-dense-roundtrip)
  (run-test test-format-conversion-preserves-data)
  (run-test test-sparse-matrix-dimensions)
  
  ;
  )
