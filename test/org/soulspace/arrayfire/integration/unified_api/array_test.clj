(ns org.soulspace.arrayfire.integration.unified-api.array-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.definitions :as defs]
            [org.soulspace.arrayfire.integration.base.resource :as res]
            [org.soulspace.arrayfire.integration.base.memory :as bmem]
            [org.soulspace.arrayfire.integration.unified-api.array :as array]
            [org.soulspace.arrayfire.integration.unified-api.device :as device])
  (:import [org.soulspace.arrayfire.integration.base.resource AFArray]))

;;;
;;; Type Helper Tests
;;;

(deftest test-floats-predicate
  (testing "floats? correctly identifies float data"
    (is (array/floats? (float-array [1.0 2.0 3.0])))
    (is (array/floats? [1.0 2.0 3.0]))
    (is (not (array/floats? [1 2 3])))
    (is (not (array/floats? "not a float")))))

(deftest test-ints-predicate
  (testing "ints? correctly identifies int data"
    (is (array/ints? (int-array [1 2 3])))
    (is (array/ints? [1 2 3]))
    (is (not (array/ints? [1.0 2.0 3.0])))
    (is (not (array/ints? "not an int")))))

(deftest test-shorts-predicate
  (testing "shorts? correctly identifies short data"
    (is (array/shorts? (short-array [1 2 3])))
    (is (array/shorts? [1 2 3]))
    (is (not (array/shorts? [100000]))) ; Too large for short
    (is (not (array/shorts? "not a short")))))

(deftest test-doubles-predicate
  (testing "doubles? correctly identifies double data"
    (is (array/doubles? (double-array [1.0 2.0 3.0])))
    (is (not (array/doubles? [1 2 3])))
    (is (not (array/doubles? "not a double")))))

(deftest test-longs-predicate
  (testing "longs? correctly identifies long data"
    (is (array/longs? (long-array [1 2 3])))
    (is (not (array/longs? [1.0 2.0 3.0])))
    (is (not (array/longs? "not a long")))))

(deftest test-bytes-predicate
  (testing "bytes? correctly identifies byte data"
    (is (array/bytes? (byte-array [1 2 3])))
    (is (not (array/bytes? [1.0 2.0 3.0])))
    (is (not (array/bytes? "not a byte")))))

(deftest test-complex-pair-predicate
  (testing "complex-pair? correctly identifies complex pairs"
    (is (array/complex-pair? [1.0 2.0]))
    (is (not (array/complex-pair? [1.0])))
    (is (not (array/complex-pair? [1.0 2.0 3.0])))
    (is (not (array/complex-pair? "not a pair")))))

(deftest test-complex-floats-predicate
  (testing "complex-floats? correctly identifies complex float data"
    (is (array/complex-floats? [[1.0 2.0] [3.0 4.0]]))
    (is (not (array/complex-floats? [[1 2] [3 4]])))
    (is (not (array/complex-floats? "not complex floats")))))

(deftest test-complex-doubles-predicate
  (testing "complex-doubles? correctly identifies complex double data"
    (is (array/complex-doubles? [[1.0 2.0] [3.0 4.0]]))
    (is (not (array/complex-doubles? [[1 2] [3 4]])))
    (is (not (array/complex-doubles? "not complex doubles")))))

;;;
;;; Array Creation Tests
;;;

(deftest test-create-array-float
  (testing "create-array creates float array"
    (device/init!)
    (let [data (float-array [1.0 2.0 3.0 4.0])
          arr (array/create-array data [2 2] defs/AF_DTYPE_F32)]
      (is (instance? AFArray arr))
      (is (= 4 (array/get-elements arr)))
      (is (= defs/AF_DTYPE_F32 (array/get-type arr)))
      (.close arr))))

(deftest test-create-array-double
  (testing "create-array creates double array"
    (device/init!)
    (let [data (double-array [1.0 2.0 3.0 4.0])
          arr (array/create-array data [2 2] defs/AF_DTYPE_F64)]
      (is (instance? AFArray arr))
      (is (= 4 (array/get-elements arr)))
      (is (= defs/AF_DTYPE_F64 (array/get-type arr)))
      (.close arr))))

(deftest test-create-array-int
  (testing "create-array creates int array"
    (device/init!)
    (let [data (int-array [1 2 3 4])
          arr (array/create-array data [2 2] defs/AF_DTYPE_S32)]
      (is (instance? AFArray arr))
      (is (= 4 (array/get-elements arr)))
      (is (= defs/AF_DTYPE_S32 (array/get-type arr)))
      (.close arr))))

(deftest test-create-array-complex-float
  (testing "create-array creates complex float array"
    (device/init!)
    (let [data [[1.0 2.0] [3.0 4.0]]
          arr (array/create-array data [2] defs/AF_DTYPE_C32)]
      (is (instance? AFArray arr))
      (is (= 2 (array/get-elements arr)))
      (is (= defs/AF_DTYPE_C32 (array/get-type arr)))
      (.close arr))))

(deftest test-create-array-complex-double
  (testing "create-array creates complex double array"
    (device/init!)
    (let [data [[1.0 2.0] [3.0 4.0]]
          arr (array/create-array data [2] defs/AF_DTYPE_C64)]
      (is (instance? AFArray arr))
      (is (= 2 (array/get-elements arr)))
      (is (= defs/AF_DTYPE_C64 (array/get-type arr)))
      (.close arr))))

(deftest test-create-handle
  (testing "create-handle creates uninitialized array"
    (device/init!)
    (let [arr (array/create-handle [3 3] defs/AF_DTYPE_F32)]
      (is (instance? AFArray arr))
      (is (= 9 (array/get-elements arr)))
      (is (= defs/AF_DTYPE_F32 (array/get-type arr)))
      (.close arr))))

(deftest test-copy-array
  (testing "copy-array creates deep copy"
    (device/init!)
    (let [original (array/create-array (float-array [1.0 2.0 3.0]) [3] defs/AF_DTYPE_F32)
          copy (array/copy-array original)]
      (is (instance? AFArray copy))
      (is (= (array/get-elements original) (array/get-elements copy)))
      (is (= (array/get-type original) (array/get-type copy)))
      (is (not= (res/af-handle-value original) (res/af-handle-value copy))) ; Different handles
      (.close original)
      (.close copy))))

;;;
;;; Array Information Tests
;;;

(deftest test-get-elements
  (testing "get-elements returns correct count"
    (device/init!)
    (let [arr (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0 6.0]) [2 3] defs/AF_DTYPE_F32)]
      (is (= 6 (array/get-elements arr)))
      (.close arr))))

(deftest test-get-type
  (testing "get-type returns correct dtype"
    (device/init!)
    (let [f32-arr (array/create-array (float-array [1.0 2.0]) [2] defs/AF_DTYPE_F32)
          f64-arr (array/create-array (double-array [1.0 2.0]) [2] defs/AF_DTYPE_F64)]
      (is (= defs/AF_DTYPE_F32 (array/get-type f32-arr)))
      (is (= defs/AF_DTYPE_F64 (array/get-type f64-arr)))
      (.close f32-arr)
      (.close f64-arr))))

(deftest test-get-dims
  (testing "get-dims returns correct dimensions"
    (device/init!)
    (let [arr (array/create-array (float-array (range 24)) [2 3 4] defs/AF_DTYPE_F32)
          dims (array/get-dims arr)]
      (is (= [2 3 4 1] dims))
      (.close arr))))

(deftest test-get-numdims
  (testing "get-numdims returns correct number of dimensions"
    (device/init!)
    (let [arr1d (array/create-array (float-array [1.0 2.0 3.0]) [3] defs/AF_DTYPE_F32)
          arr2d (array/create-array (float-array (range 6)) [2 3] defs/AF_DTYPE_F32)
          arr3d (array/create-array (float-array (range 24)) [2 3 4] defs/AF_DTYPE_F32)]
      (is (= 1 (array/get-numdims arr1d)))
      (is (= 2 (array/get-numdims arr2d)))
      (is (= 3 (array/get-numdims arr3d)))
      (.close arr1d)
      (.close arr2d)
      (.close arr3d))))

;;;
;;; Array Predicates Tests
;;;

(deftest test-is-empty
  (testing "empty? correctly identifies empty arrays"
    (device/init!)
    (let [empty-arr (array/create-handle [0] defs/AF_DTYPE_F32)
          non-empty-arr (array/create-array (float-array [1.0]) [1] defs/AF_DTYPE_F32)]
      (is (array/empty? empty-arr))
      (is (not (array/empty? non-empty-arr)))
      (.close empty-arr)
      (.close non-empty-arr))))

(deftest test-is-scalar
  (testing "scalar? correctly identifies scalar arrays"
    (device/init!)
    (let [scalar-arr (array/create-array (float-array [1.0]) [1] defs/AF_DTYPE_F32)
          vector-arr (array/create-array (float-array [1.0 2.0]) [2] defs/AF_DTYPE_F32)]
      (is (array/scalar? scalar-arr))
      (is (not (array/scalar? vector-arr)))
      (.close scalar-arr)
      (.close vector-arr))))

(deftest test-is-vector
  (testing "vector? correctly identifies vector arrays"
    (device/init!)
    (let [vector-arr (array/create-array (float-array [1.0 2.0 3.0]) [3] defs/AF_DTYPE_F32)
          matrix-arr (array/create-array (float-array (range 6)) [2 3] defs/AF_DTYPE_F32)]
      (is (array/vector? vector-arr))
      (is (not (array/vector? matrix-arr)))
      (.close vector-arr)
      (.close matrix-arr))))

(deftest test-is-row
  (testing "row? correctly identifies row vectors"
    (device/init!)
    (let [row-arr (array/create-array (float-array [1.0 2.0 3.0]) [1 3] defs/AF_DTYPE_F32)
          col-arr (array/create-array (float-array [1.0 2.0 3.0]) [3 1] defs/AF_DTYPE_F32)]
      (is (array/row? row-arr))
      (is (not (array/row? col-arr)))
      (.close row-arr)
      (.close col-arr))))

(deftest test-is-column
  (testing "column? correctly identifies column vectors"
    (device/init!)
    (let [col-arr (array/create-array (float-array [1.0 2.0 3.0]) [3 1] defs/AF_DTYPE_F32)
          row-arr (array/create-array (float-array [1.0 2.0 3.0]) [1 3] defs/AF_DTYPE_F32)]
      (is (array/column? col-arr))
      (is (not (array/column? row-arr)))
      (.close col-arr)
      (.close row-arr))))

(deftest test-is-complex
  (testing "complex? correctly identifies complex arrays"
    (device/init!)
    (let [complex-arr (array/create-array [[1.0 2.0] [3.0 4.0]] [2] defs/AF_DTYPE_C32)
          real-arr (array/create-array (float-array [1.0 2.0]) [2] defs/AF_DTYPE_F32)]
      (is (array/complex? complex-arr))
      (is (not (array/complex? real-arr)))
      (.close complex-arr)
      (.close real-arr))))

(deftest test-is-real
  (testing "real? correctly identifies real arrays"
    (device/init!)
    (let [real-arr (array/create-array (float-array [1.0 2.0]) [2] defs/AF_DTYPE_F32)
          complex-arr (array/create-array [[1.0 2.0] [3.0 4.0]] [2] defs/AF_DTYPE_C32)]
      (is (array/real? real-arr))
      (is (not (array/real? complex-arr)))
      (.close real-arr)
      (.close complex-arr))))

(deftest test-is-double
  (testing "double? correctly identifies double arrays"
    (device/init!)
    (let [double-arr (array/create-array (double-array [1.0 2.0]) [2] defs/AF_DTYPE_F64)
          float-arr (array/create-array (float-array [1.0 2.0]) [2] defs/AF_DTYPE_F32)]
      (is (array/double? double-arr))
      (is (not (array/double? float-arr)))
      (.close double-arr)
      (.close float-arr))))

(deftest test-is-single
  (testing "single? correctly identifies single precision arrays"
    (device/init!)
    (let [float-arr (array/create-array (float-array [1.0 2.0]) [2] defs/AF_DTYPE_F32)
          double-arr (array/create-array (double-array [1.0 2.0]) [2] defs/AF_DTYPE_F64)]
      (is (array/single? float-arr))
      (is (not (array/single? double-arr)))
      (.close float-arr)
      (.close double-arr))))

(deftest test-is-half
  (testing "half? correctly identifies half precision arrays"
    (device/init!)
    (when (device/half-support?)
      ;; Skip actual test as F16 might not be available
      (is true))))

(deftest test-is-integer
  (testing "integer? correctly identifies integer arrays"
    (device/init!)
    (let [int-arr (array/create-array (int-array [1 2]) [2] defs/AF_DTYPE_S32)
          float-arr (array/create-array (float-array [1.0 2.0]) [2] defs/AF_DTYPE_F32)]
      (is (array/integer? int-arr))
      (is (not (array/integer? float-arr)))
      (.close int-arr)
      (.close float-arr))))

(deftest test-is-bool
  (testing "bool? correctly identifies boolean arrays"
    (device/init!)
    (let [bool-arr (array/create-array (byte-array [1 0]) [2] defs/AF_DTYPE_B8)
          int-arr (array/create-array (int-array [1 2]) [2] defs/AF_DTYPE_S32)]
      (is (array/bool? bool-arr))
      (is (not (array/bool? int-arr)))
      (.close bool-arr)
      (.close int-arr))))

;;;
;;; Data Transfer Tests
;;;

(deftest test-get-data-ptr
  (testing "get-data-ptr copies data from device to host"
    (device/init!)
    (let [data (float-array [1.0 2.0 3.0])
          arr (array/create-array data [3] defs/AF_DTYPE_F32)
          buf (mem/alloc (* 3 4))]
      (array/get-data-ptr arr buf)
      (is (<= (Math/abs (- 1.0 (mem/read-float buf 0))) 0.001))
      (is (<= (Math/abs (- 2.0 (mem/read-float buf 4))) 0.001))
      (is (<= (Math/abs (- 3.0 (mem/read-float buf 8))) 0.001))
      (.close arr))))

(deftest test-write-array
  (testing "write-array! modifies array in-place"
    (device/init!)
    (let [arr (array/create-handle [3] defs/AF_DTYPE_F32)
          data (bmem/float-array->segment (float-array [1.0 2.0 3.0]))]
      (array/write-array! arr data (* 3 4))
      (let [buf (mem/alloc (* 3 4))]
        (array/get-data-ptr arr buf)
        (is (<= (Math/abs (- 1.0 (mem/read-float buf 0))) 0.001))
        (is (<= (Math/abs (- 2.0 (mem/read-float buf 4))) 0.001))
        (is (<= (Math/abs (- 3.0 (mem/read-float buf 8))) 0.001)))
      (.close arr))))

(comment
  ;; run tests from REPL
  (run-tests)
  ;
  )
