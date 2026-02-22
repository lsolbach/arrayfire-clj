(ns org.soulspace.arrayfire.integration.unified-api.index-test
  (:require [clojure.test :refer [deftest is testing run-test run-tests]]
            [tech.v3.resource :refer [releasing!]]
            [org.soulspace.arrayfire.ffi.base.definitions :as defs]
            [org.soulspace.arrayfire.integration.base.resource :as res]
            [org.soulspace.arrayfire.integration.unified-api.index :as idx]
            [org.soulspace.arrayfire.integration.unified-api.array :as array]
            [org.soulspace.arrayfire.integration.unified-api.data :as data]
            [org.soulspace.arrayfire.integration.unified-api.device :as device])
  (:import [org.soulspace.arrayfire.integration.base.resource AFArray]))

;;;
;;; Sequence Utilities Tests
;;;

(deftest test-make-seq
  (testing "make-seq creates ArrayFire sequence"
    (device/init!)
    (let [seq (idx/make-seq 0 9 1)]
      (is (some? seq)))))

(deftest test-make-seq-default-step
  (testing "make-seq with default step of 1"
    (device/init!)
    (let [seq (idx/make-seq 0 9)]
      (is (some? seq)))))

(deftest test-make-seq-all-elements
  (testing "make-seq for selecting all elements (0 to end)"
    (device/init!)
    (let [seq (idx/make-seq 0 -1 1)]
      (is (some? seq)))))

(deftest test-make-seq-reverse
  (testing "make-seq with negative step for reverse order"
    (device/init!)
    (let [seq (idx/make-seq -1 0 -1)]
      (is (some? seq)))))

(deftest test-make-seq-skip-elements
  (testing "make-seq with step > 1 to skip elements"
    (device/init!)
    (let [seq (idx/make-seq 0 -1 2)]
      (is (some? seq)))))

;;;
;;; Basic Indexing Tests
;;;

(deftest test-index-single-dim
  (testing "index with single dimension sequence"
    (device/init!)
    (releasing!
      (let [arr (data/range [100] 0 defs/AF_DTYPE_F32)
            s0 (idx/make-seq 0 9 1)
            result (idx/index arr [s0])]
        (is (instance? AFArray result))
        (is (= [10 1 1 1] (array/get-dims result)))))))

(deftest test-index-two-dim
  (testing "index with two dimension sequences"
    (device/init!)
    (releasing!
      (let [arr (data/range [10 10] 0 defs/AF_DTYPE_F32)
            s0 (idx/make-seq 0 4 1)
            s1 (idx/make-seq 0 4 1)
            result (idx/index arr [s0 s1])]
        (is (instance? AFArray result))
        (is (= [5 5 1 1] (array/get-dims result)))))))

(deftest test-index-all-elements
  (testing "index selecting all elements in dimension"
    (device/init!)
    (releasing!
      (let [arr (data/range [20 30] 0 defs/AF_DTYPE_F32)
            s0 (idx/make-seq 0 -1 1)
            s1 (idx/make-seq 0 9 1)
            result (idx/index arr [s0 s1])]
        (is (instance? AFArray result))
        (is (= [20 10 1 1] (array/get-dims result)))))))

(deftest test-index-with-step
  (testing "index with step to select every nth element"
    (device/init!)
    (releasing!
      (let [arr (data/range [100] 0 defs/AF_DTYPE_F32)
            s0 (idx/make-seq 0 -1 2)
            result (idx/index arr [s0])]
        (is (instance? AFArray result))
        (is (= [50 1 1 1] (array/get-dims result)))))))

(deftest test-lookup-1d
  (testing "lookup extracts specific elements by index array"
    (device/init!)
    (releasing!
      (let [arr (data/range [100] 0 defs/AF_DTYPE_F32)
            indices (array/create-array (int-array [0 10 20 30]) [4] defs/AF_DTYPE_S32)
            result (idx/lookup arr indices)]
        (is (instance? AFArray result))
        (is (= [4 1 1 1] (array/get-dims result)))))))

(deftest test-lookup-with-dim
  (testing "lookup along specific dimension"
    (device/init!)
    (releasing!
      (let [arr (data/range [10 20] 0 defs/AF_DTYPE_F32)
            indices (array/create-array (int-array [0 2 5]) [3] defs/AF_DTYPE_S32)
            result-dim0 (idx/lookup arr indices 0)
            result-dim1 (idx/lookup arr indices 1)]
        (is (instance? AFArray result-dim0))
        (is (instance? AFArray result-dim1))
        (is (= [3 20 1 1] (array/get-dims result-dim0)))
        (is (= [10 3 1 1] (array/get-dims result-dim1)))))))

;;;
;;; Generalized Indexing Tests
;;;

(deftest test-index-gen-with-seq
  (testing "index-gen with sequence indexers"
    (device/init!)
    (releasing!
      (let [arr (data/range [20 20] 0 defs/AF_DTYPE_F32)
            indexers (idx/create-indexers)
            seq0 (idx/make-seq 0 9 1)]
        (try
          (idx/set-seq-indexer! indexers seq0 0 false)
          (idx/set-seq-indexer! indexers (idx/make-seq 0 -1 1) 1 false)
          (let [result (idx/index-gen arr indexers 2)]
            (is (instance? AFArray result))
            (is (= [10 20 1 1] (array/get-dims result))))
          (finally
            (idx/release-indexers! indexers)))))))

(deftest test-index-gen-with-array
  (testing "index-gen with array indexers"
    (device/init!)
    (releasing!
      (let [arr (data/range [50] 0 defs/AF_DTYPE_F32)
            indexers (idx/create-indexers)
            idx-arr (array/create-array (int-array [5 15 25 35 45]) [5] defs/AF_DTYPE_S32)]
        (try
          (idx/set-array-indexer! indexers idx-arr 0)
          (let [result (idx/index-gen arr indexers 1)]
            (is (instance? AFArray result))
            (is (= [5 1 1 1] (array/get-dims result))))
          (finally
            (idx/release-indexers! indexers)))))))

;;;
;;; Assignment Operations Tests
;;;

(deftest test-assign-seq-simple
  (testing "assign-seq assigns values to subarray"
    (device/init!)
    (releasing!
      (let [arr (data/constant 1.0 [20 20] defs/AF_DTYPE_F32)
            s0 (idx/make-seq 0 4 1)
            s1 (idx/make-seq 0 4 1)
            vals (data/constant 99.0 [5 5] defs/AF_DTYPE_F32)
            result (idx/assign-seq arr [s0 s1] vals)]
        (is (instance? AFArray result))
        (is (= [20 20 1 1] (array/get-dims result)))))))

(deftest test-assign-seq-full-dimension
  (testing "assign-seq to full dimension"
    (device/init!)
    (releasing!
      (let [arr (data/constant 1.0 [100] defs/AF_DTYPE_F32)
            s0 (idx/make-seq 10 19 1)
            vals (data/constant 42.0 [10] defs/AF_DTYPE_F32)
            result (idx/assign-seq arr [s0] vals)]
        (is (instance? AFArray result))
        (is (= [100 1 1 1] (array/get-dims result)))))))

(deftest test-assign-gen-with-indexers
  (testing "assign-gen with generalized indexers"
    (device/init!)
    (releasing!
      (let [arr (data/constant 1.0 [30 30] defs/AF_DTYPE_F32)
            indexers (idx/create-indexers)
            idx-arr (array/create-array (int-array [5 10 15 20]) [4] defs/AF_DTYPE_S32)
            vals (data/constant 7.0 [4 30] defs/AF_DTYPE_F32)]
        (try
          (idx/set-array-indexer! indexers idx-arr 0)
          (idx/set-seq-indexer! indexers (idx/make-seq 0 -1 1) 1 false)
          (let [result (idx/assign-gen arr indexers 2 vals)]
            (is (instance? AFArray result))
            (is (= [30 30 1 1] (array/get-dims result))))
          (finally
            (idx/release-indexers! indexers)))))))

;;;
;;; Indexer Management Tests
;;;

(deftest test-create-indexers
  (testing "create-indexers allocates indexer array"
    (device/init!)
    (let [indexers (idx/create-indexers)]
      (is (some? indexers))
      (idx/release-indexers! indexers))))

(deftest test-set-array-indexer
  (testing "set-array-indexer! configures array-based indexing"
    (device/init!)
    (releasing!
      (let [indexers (idx/create-indexers)
            idx-arr (array/create-array (int-array [0 5 10]) [3] defs/AF_DTYPE_S32)]
        (try
          (is (nil? (idx/set-array-indexer! indexers idx-arr 0)))
          (finally
            (idx/release-indexers! indexers)))))))

(deftest test-set-seq-indexer
  (testing "set-seq-indexer! configures sequence-based indexing"
    (device/init!)
    (let [indexers (idx/create-indexers)
          seq (idx/make-seq 0 9 1)]
      (try
        (is (nil? (idx/set-seq-indexer! indexers seq 0 false)))
        (finally
          (idx/release-indexers! indexers))))))

(deftest test-set-seq-indexer-batch
  (testing "set-seq-indexer! with batch flag"
    (device/init!)
    (let [indexers (idx/create-indexers)
          seq (idx/make-seq 0 4 1)]
      (try
        (is (nil? (idx/set-seq-indexer! indexers seq 0 true)))
        (finally
          (idx/release-indexers! indexers))))))

(deftest test-set-seq-param-indexer
  (testing "set-seq-param-indexer! creates sequence inline"
    (device/init!)
    (let [indexers (idx/create-indexers)]
      (try
        (is (nil? (idx/set-seq-param-indexer! indexers 0 9 1 0 false)))
        (finally
          (idx/release-indexers! indexers))))))

(deftest test-set-seq-param-indexer-batch
  (testing "set-seq-param-indexer! with batch parameter"
    (device/init!)
    (let [indexers (idx/create-indexers)]
      (try
        (is (nil? (idx/set-seq-param-indexer! indexers 0 4 1 0 true)))
        (finally
          (idx/release-indexers! indexers))))))

(deftest test-release-indexers
  (testing "release-indexers! deallocates indexer array"
    (device/init!)
    (let [indexers (idx/create-indexers)]
      (is (nil? (idx/release-indexers! indexers))))))

;;;
;;; Helper Functions Tests
;;;

(deftest test-slice-simple
  (testing "slice convenience function for simple slicing"
    (device/init!)
    (releasing!
      (let [arr (data/range [20 30] 0 defs/AF_DTYPE_F32)
            result (idx/slice arr [[0 9] nil])]
        (is (instance? AFArray result))
        (is (= [10 30 1 1] (array/get-dims result)))))))

(deftest test-slice-with-step
  (testing "slice with step parameter"
    (device/init!)
    (releasing!
      (let [arr (data/range [100] 0 defs/AF_DTYPE_F32)
            result (idx/slice arr [[0 -1 2]])]
        (is (instance? AFArray result))
        (is (= [50 1 1 1] (array/get-dims result)))))))

(deftest test-slice-two-dimensions
  (testing "slice with two dimension ranges"
    (device/init!)
    (releasing!
      (let [arr (data/range [50 60] 0 defs/AF_DTYPE_F32)
            result (idx/slice arr [[5 14] [10 19]])]
        (is (instance? AFArray result))
        (is (= [10 10 1 1] (array/get-dims result)))))))

(deftest test-slice-nil-dimension
  (testing "slice with nil for full dimension"
    (device/init!)
    (releasing!
      (let [arr (data/range [20 30 40] 0 defs/AF_DTYPE_F32)
            result (idx/slice arr [nil [5 14] nil])]
        (is (instance? AFArray result))
        (is (= [20 10 40 1] (array/get-dims result)))))))

;;;
;;; Complex Indexing Tests
;;;

(deftest test-mixed-indexing
  (testing "mixed sequence and array indexing"
    (device/init!)
    (releasing!
      (let [arr (data/range [100 100] 0 defs/AF_DTYPE_F32)
            indexers (idx/create-indexers)
            idx-arr (array/create-array (int-array [10 20 30 40 50]) [5] defs/AF_DTYPE_S32)]
        (try
          (idx/set-array-indexer! indexers idx-arr 0)
          (idx/set-seq-param-indexer! indexers 0 49 1 1 false)
          (let [result (idx/index-gen arr indexers 2)]
            (is (instance? AFArray result))
            (is (= [5 50 1 1] (array/get-dims result))))
          (finally
            (idx/release-indexers! indexers)))))))

(deftest test-indexer-reuse
  (testing "indexers can be reused for multiple operations"
    (device/init!)
    (releasing!
      (let [arr1 (data/range [50] 0 defs/AF_DTYPE_F32)
            arr2 (data/range [50] 0 defs/AF_DTYPE_F32)
            indexers (idx/create-indexers)]
        (try
          (idx/set-seq-param-indexer! indexers 0 9 1 0 false)
          (let [result1 (idx/index-gen arr1 indexers 1)
                result2 (idx/index-gen arr2 indexers 1)]
            (is (instance? AFArray result1))
            (is (instance? AFArray result2)))
          (finally
            (idx/release-indexers! indexers)))))))

(comment
  ;; run all tests from REPL
  (run-tests)
  
  ;; run individual tests
  (run-test test-make-seq)
  (run-test test-make-seq-default-step)
  (run-test test-make-seq-all-elements)
  (run-test test-make-seq-reverse)
  (run-test test-make-seq-skip-elements)
  (run-test test-index-single-dim)
  (run-test test-index-two-dim)
  (run-test test-index-all-elements)
  (run-test test-index-with-step)
  (run-test test-lookup-1d)
  (run-test test-lookup-with-dim)
  (run-test test-index-gen-with-seq)
  (run-test test-index-gen-with-array)
  (run-test test-assign-seq-simple)
  (run-test test-assign-seq-full-dimension)
  (run-test test-assign-gen-with-indexers)
  (run-test test-create-indexers)
  (run-test test-set-array-indexer)
  (run-test test-set-seq-indexer)
  (run-test test-set-seq-indexer-batch)
  (run-test test-set-seq-param-indexer)
  (run-test test-set-seq-param-indexer-batch)
  (run-test test-release-indexers)
  (run-test test-slice-simple)
  (run-test test-slice-with-step)
  (run-test test-slice-two-dimensions)
  (run-test test-slice-nil-dimension)
  (run-test test-mixed-indexing)
  (run-test test-indexer-reuse)
  
  ;
  )
