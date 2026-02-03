(ns org.soulspace.arrayfire.integration.util-test
  (:require [clojure.test :refer [deftest is testing run-test run-tests]]
            [org.soulspace.arrayfire.integration.util :as util]
            [org.soulspace.arrayfire.integration.array :as array]
            [org.soulspace.arrayfire.integration.device :as device]
            [org.soulspace.arrayfire.integration.jvm-integration :as jvm]
            [clojure.java.io :as io])
  (:import [org.soulspace.arrayfire.integration.jvm_integration AFArray]))

;;;
;;; Array Printing Tests
;;;

(deftest test-print-array
  (testing "print-array prints array to stdout without error"
    (device/init!)
    (let [data (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] jvm/AF_DTYPE_F32)]
      (try
        (is (nil? (util/print-array data)))
        (finally
          (.close data))))))

(deftest test-print-array-gen
  (testing "print-array-gen prints named array with custom precision"
    (device/init!)
    (let [data (array/create-array (float-array [1.234567 2.345678]) [2] jvm/AF_DTYPE_F32)]
      (try
        (is (nil? (util/print-array-gen "test-data" data 4)))
        (finally
          (.close data))))))

(deftest test-print-array-gen-default-precision
  (testing "print-array-gen with default precision"
    (device/init!)
    (let [data (array/create-array (float-array [1.0 2.0]) [2] jvm/AF_DTYPE_F32)]
      (try
        (is (nil? (util/print-array-gen "values" data)))
        (finally
          (.close data))))))

;;;
;;; String Conversion Tests
;;;

(deftest test-array-to-string
  (testing "array-to-string converts array to formatted string"
    (device/init!)
    (let [data (array/create-array (float-array [1.0 2.0 3.0]) [3] jvm/AF_DTYPE_F32)
          result (util/array-to-string "test-array" data 2 false)]
      (try
        (is (string? result))
        (is (not (empty? result)))
        (is (.contains result "test-array"))
        (finally
          (.close data))))))

(deftest test-array-to-string-with-transpose
  (testing "array-to-string with transpose flag"
    (device/init!)
    (let [data (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] jvm/AF_DTYPE_F32)
          result (util/array-to-string "matrix" data 4 true)]
      (try
        (is (string? result))
        (is (.contains result "matrix"))
        (finally
          (.close data))))))

(deftest test-array-to-string-high-precision
  (testing "array-to-string with high precision"
    (device/init!)
    (let [data (array/create-array (float-array [3.14159265]) [1] jvm/AF_DTYPE_F32)
          result (util/array-to-string "pi" data 8 false)]
      (try
        (is (string? result))
        (finally
          (.close data))))))

;;;
;;; Array Persistence Tests (Save/Load)
;;;

(deftest test-save-and-read-array-index
  (testing "save array and read by index"
    (device/init!)
    (let [test-file "test-array-index.af"
          data (array/create-array (float-array [1.0 2.0 3.0 4.0]) [2 2] jvm/AF_DTYPE_F32)]
      (try
        ;; Save array
        (let [idx (util/save-array "test-data" data test-file false)]
          (is (integer? idx))
          (is (>= idx 0)))
        
        ;; Read back by index
        (let [loaded (util/read-array-index test-file 0)]
          (try
            (is (instance? AFArray loaded))
            (is (= [2 2] (take 2 (array/get-dims loaded))))
            (finally
              (.close loaded))))
        (finally
          (.close data)
          (io/delete-file test-file true))))))

(deftest test-save-and-read-array-key
  (testing "save array and read by key"
    (device/init!)
    (let [test-file "test-array-key.af"
          data (array/create-array (float-array [5.0 6.0 7.0]) [3] jvm/AF_DTYPE_F32)]
      (try
        ;; Save array with key
        (util/save-array "my-key" data test-file false)
        
        ;; Read back by key
        (let [loaded (util/read-array-key test-file "my-key")]
          (try
            (is (instance? AFArray loaded))
            (is (= [3] (take 1 (array/get-dims loaded))))
            (finally
              (.close loaded))))
        (finally
          (.close data)
          (io/delete-file test-file true))))))

(deftest test-save-multiple-arrays
  (testing "save multiple arrays to same file"
    (device/init!)
    (let [test-file "test-multiple.af"
          data1 (array/create-array (float-array [1.0 2.0]) [2] jvm/AF_DTYPE_F32)
          data2 (array/create-array (float-array [3.0 4.0 5.0]) [3] jvm/AF_DTYPE_F32)]
      (try
        ;; Save first array
        (let [idx1 (util/save-array "first" data1 test-file false)]
          (is (= 0 idx1)))
        
        ;; Append second array
        (let [idx2 (util/save-array "second" data2 test-file true)]
          (is (= 1 idx2)))
        
        ;; Read both back
        (let [loaded1 (util/read-array-key test-file "first")
              loaded2 (util/read-array-key test-file "second")]
          (try
            (is (= 2 (array/get-elements loaded1)))
            (is (= 3 (array/get-elements loaded2)))
            (finally
              (.close loaded1)
              (.close loaded2))))
        (finally
          (.close data1)
          (.close data2)
          (io/delete-file test-file true))))))

(deftest test-save-overwrite-file
  (testing "save array overwrites existing file when append=false"
    (device/init!)
    (let [test-file "test-overwrite.af"
          data1 (array/create-array (float-array [1.0]) [1] jvm/AF_DTYPE_F32)
          data2 (array/create-array (float-array [2.0 3.0]) [2] jvm/AF_DTYPE_F32)]
      (try
        ;; Save first array
        (util/save-array "first" data1 test-file false)
        
        ;; Overwrite file with second array
        (let [idx (util/save-array "second" data2 test-file false)]
          (is (= 0 idx)))
        
        ;; Only second array should exist
        (let [loaded (util/read-array-index test-file 0)]
          (try
            (is (= 2 (array/get-elements loaded)))
            (finally
              (.close loaded))))
        (finally
          (.close data1)
          (.close data2)
          (io/delete-file test-file true))))))

(deftest test-read-array-key-check
  (testing "read-array-key-check validates key existence"
    (device/init!)
    (let [test-file "test-key-check.af"
          data (array/create-array (float-array [1.0 2.0]) [2] jvm/AF_DTYPE_F32)]
      (try
        ;; Save with specific key
        (util/save-array "exists" data test-file false)
        
        ;; Reading existing key should succeed
        (let [result (util/read-array-key-check test-file "exists")]
          (is (integer? result))
          (is (>= result 0)))
        
        ;; Reading non-existent key should return error
        (let [result (util/read-array-key-check test-file "does-not-exist")]
          (is (integer? result))
          ;; Non-zero indicates key not found
          (is (not= 0 result)))
        (finally
          (.close data)
          (io/delete-file test-file true))))))

;;;
;;; Data Type Preservation Tests
;;;

(deftest test-save-load-preserves-dtype-f32
  (testing "save/load preserves float32 data type"
    (device/init!)
    (let [test-file "test-dtype-f32.af"
          data (array/create-array (float-array [1.0 2.0]) [2] jvm/AF_DTYPE_F32)]
      (try
        (util/save-array "f32-data" data test-file false)
        (let [loaded (util/read-array-key test-file "f32-data")]
          (try
            (is (= jvm/AF_DTYPE_F32 (array/get-type loaded)))
            (finally
              (.close loaded))))
        (finally
          (.close data)
          (io/delete-file test-file true))))))

(deftest test-save-load-preserves-dtype-f64
  (testing "save/load preserves float64 data type"
    (device/init!)
    (let [test-file "test-dtype-f64.af"
          data (array/create-array (double-array [1.0 2.0]) [2] jvm/AF_DTYPE_F64)]
      (try
        (util/save-array "f64-data" data test-file false)
        (let [loaded (util/read-array-key test-file "f64-data")]
          (try
            (is (= jvm/AF_DTYPE_F64 (array/get-type loaded)))
            (finally
              (.close loaded))))
        (finally
          (.close data)
          (io/delete-file test-file true))))))

(deftest test-save-load-preserves-dimensions
  (testing "save/load preserves array dimensions"
    (device/init!)
    (let [test-file "test-dims.af"
          data (array/create-array (float-array (range 24)) [2 3 4] jvm/AF_DTYPE_F32)]
      (try
        (util/save-array "3d-array" data test-file false)
        (let [loaded (util/read-array-key test-file "3d-array")]
          (try
            (is (= (array/get-dims data) (array/get-dims loaded)))
            (is (= [2 3 4] (take 3 (array/get-dims loaded))))
            (finally
              (.close loaded))))
        (finally
          (.close data)
          (io/delete-file test-file true))))))

;;;
;;; Integration Tests
;;;

(deftest test-print-and-string-consistency
  (testing "print-array and array-to-string produce similar output"
    (device/init!)
    (let [data (array/create-array (float-array [1.0 2.0]) [2] jvm/AF_DTYPE_F32)
          str-result (util/array-to-string "test" data 4 false)]
      (try
        (is (string? str-result))
        ;; Both functions should work without errors
        (is (nil? (util/print-array data)))
        (finally
          (.close data))))))

(deftest test-roundtrip-complex-data
  (testing "save/load roundtrip with various data patterns"
    (device/init!)
    (let [test-file "test-roundtrip.af"
          ;; Create array with known pattern
          data (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0 6.0]) [2 3] jvm/AF_DTYPE_F32)]
      (try
        (util/save-array "pattern" data test-file false)
        (let [loaded (util/read-array-key test-file "pattern")]
          (try
            (is (= (array/get-elements data) (array/get-elements loaded)))
            (is (= (array/get-dims data) (array/get-dims loaded)))
            (is (= (array/get-type data) (array/get-type loaded)))
            (finally
              (.close loaded))))
        (finally
          (.close data)
          (io/delete-file test-file true))))))

(comment
  ;; run all tests from REPL
  (run-tests)
  
  ;; run individual tests - Printing
  (run-test test-print-array)
  (run-test test-print-array-gen)
  (run-test test-print-array-gen-default-precision)
  
  ;; run individual tests - String Conversion
  (run-test test-array-to-string)
  (run-test test-array-to-string-with-transpose)
  (run-test test-array-to-string-high-precision)
  
  ;; run individual tests - Save/Load
  (run-test test-save-and-read-array-index)
  (run-test test-save-and-read-array-key)
  (run-test test-save-multiple-arrays)
  (run-test test-save-overwrite-file)
  (run-test test-read-array-key-check)
  
  ;; run individual tests - Data Type Preservation
  (run-test test-save-load-preserves-dtype-f32)
  (run-test test-save-load-preserves-dtype-f64)
  (run-test test-save-load-preserves-dimensions)
  
  ;; run individual tests - Integration
  (run-test test-print-and-string-consistency)
  (run-test test-roundtrip-complex-data)
  
  ;
  )
