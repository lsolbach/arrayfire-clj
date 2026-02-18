(ns org.soulspace.arrayfire.integration.unified-api.internal-test
  (:require [clojure.test :refer [deftest is testing run-test run-tests]]
            [tech.v3.resource :refer [releasing!]]
            [org.soulspace.arrayfire.ffi.base.definitions :as defs]
            [org.soulspace.arrayfire.integration.base.resource :as res]
            [org.soulspace.arrayfire.integration.unified-api.internal :as internal]
            [org.soulspace.arrayfire.integration.unified-api.array :as array]
            [org.soulspace.arrayfire.integration.unified-api.data :as data]
            [org.soulspace.arrayfire.integration.unified-api.device :as device]
            [coffi.mem :as mem])
  (:import [org.soulspace.arrayfire.integration.base.resource AFArray]))

;;;
;;; Strided Array Creation Tests
;;;

(deftest test-create-strided-array-simple
  (testing "create-strided-array with standard row-major layout"
    (device/init!)
    (releasing!
      (let [data-seg (mem/alloc (* 100 4))
            arr (internal/create-strided-array data-seg 0 [10 10] [1 10] defs/AF_DTYPE_F32 0)]
        (is (instance? AFArray arr))
        (is (= [10 10] (array/get-dims arr)))))))

(deftest test-create-strided-array-with-offset
  (testing "create-strided-array with non-zero offset"
    (device/init!)
    (releasing!
      (let [data-seg (mem/alloc (* 200 4))
            arr (internal/create-strided-array data-seg 50 [10 10] [1 10] defs/AF_DTYPE_F32 0)]
        (is (instance? AFArray arr))
        (is (= [10 10] (array/get-dims arr)))))))

(deftest test-create-strided-array-transposed
  (testing "create-strided-array with transposed stride pattern"
    (device/init!)
    (releasing!
      (let [data-seg (mem/alloc (* 100 4))
            arr (internal/create-strided-array data-seg 0 [10 10] [10 1] defs/AF_DTYPE_F32 0)]
        (is (instance? AFArray arr))
        (is (= [10 10] (array/get-dims arr)))))))

(deftest test-create-strided-array-3d
  (testing "create-strided-array with 3D dimensions"
    (device/init!)
    (releasing!
      (let [data-seg (mem/alloc (* 1000 4))
            arr (internal/create-strided-array data-seg 0 [10 10 10] [1 10 100] defs/AF_DTYPE_F32 0)]
        (is (instance? AFArray arr))
        (is (= [10 10 10] (array/get-dims arr)))))))

(deftest test-create-strided-array-double
  (testing "create-strided-array with double precision"
    (device/init!)
    (releasing!
      (let [data-seg (mem/alloc (* 100 8))
            arr (internal/create-strided-array data-seg 0 [10 10] [1 10] defs/AF_DTYPE_F64 0)]
        (is (instance? AFArray arr))
        (is (= defs/AF_DTYPE_F64 (array/get-type arr)))))))

;;;
;;; Memory Layout Inspection Tests
;;;

(deftest test-get-strides
  (testing "get-strides returns stride information"
    (device/init!)
    (releasing!
      (let [arr (data/range [20 30] 0 defs/AF_DTYPE_F32)
            strides (internal/get-strides arr)]
        (is (vector? strides))
        (is (= 4 (count strides)))
        (is (every? integer? strides))
        ;; Standard row-major: first stride should be 1
        (is (= 1 (first strides)))))))

(deftest test-get-strides-3d-array
  (testing "get-strides for 3D array"
    (device/init!)
    (releasing!
      (let [arr (data/range [10 20 30] 0 defs/AF_DTYPE_F32)
            [s0 s1 s2 s3] (internal/get-strides arr)]
        (is (= 1 s0))
        (is (= 10 s1))
        (is (= 200 s2))))))

(deftest test-get-offset
  (testing "get-offset returns array offset from base pointer"
    (device/init!)
    (releasing!
      (let [arr (data/range [100] 0 defs/AF_DTYPE_F32)
            offset (internal/get-offset arr)]
        (is (integer? offset))
        (is (>= offset 0))))))

(deftest test-get-offset-full-array
  (testing "get-offset is 0 for non-view arrays"
    (device/init!)
    (releasing!
      (let [arr (data/constant 1.0 [50 50] defs/AF_DTYPE_F32)
            offset (internal/get-offset arr)]
        (is (= 0 offset))))))

(deftest test-is-linear-contiguous-array
  (testing "is-linear? returns true for contiguous arrays"
    (device/init!)
    (releasing!
      (let [arr (data/range [100 100] 0 defs/AF_DTYPE_F32)]
        (is (true? (internal/is-linear? arr)))))))

(deftest test-is-linear-predicate
  (testing "is-linear? returns boolean"
    (device/init!)
    (releasing!
      (let [arr (data/constant 1.0 [50] defs/AF_DTYPE_F32)]
        (is (boolean? (internal/is-linear? arr)))))))

(deftest test-is-owner-owner-array
  (testing "is-owner? returns true for arrays that own their memory"
    (device/init!)
    (releasing!
      (let [arr (data/range [100] 0 defs/AF_DTYPE_F32)]
        (is (true? (internal/is-owner? arr)))))))

(deftest test-is-owner-predicate
  (testing "is-owner? returns boolean"
    (device/init!)
    (releasing!
      (let [arr (data/constant 5.0 [10 10] defs/AF_DTYPE_F32)]
        (is (boolean? (internal/is-owner? arr)))))))

;;;
;;; Memory Access Tests
;;;

(deftest test-get-raw-ptr
  (testing "get-raw-ptr returns device pointer"
    (device/init!)
    (releasing!
      (let [arr (data/range [100] 0 defs/AF_DTYPE_F32)
            ptr (internal/get-raw-ptr arr)]
        (is (some? ptr))))))

(deftest test-get-raw-ptr-non-zero
  (testing "get-raw-ptr returns non-null pointer for allocated array"
    (device/init!)
    (releasing!
      (let [arr (data/constant 1.0 [50 50] defs/AF_DTYPE_F32)
            ptr (internal/get-raw-ptr arr)]
        (is (some? ptr))
        (is (not (zero? (.address ptr))))))))

(deftest test-get-allocated-bytes
  (testing "get-allocated-bytes returns memory size"
    (device/init!)
    (releasing!
      (let [arr (data/range [100] 0 defs/AF_DTYPE_F32)
            bytes (internal/get-allocated-bytes arr)]
        (is (integer? bytes))
        (is (pos? bytes))
        ;; Should be at least 100 * 4 bytes for 100 float32 elements
        (is (>= bytes 400))))))

(deftest test-get-allocated-bytes-large-array
  (testing "get-allocated-bytes for large array"
    (device/init!)
    (releasing!
      (let [arr (data/constant 1.0 [1000 1000] defs/AF_DTYPE_F32)
            bytes (internal/get-allocated-bytes arr)]
        (is (>= bytes (* 1000 1000 4)))))))

(deftest test-get-allocated-bytes-double
  (testing "get-allocated-bytes for double precision array"
    (device/init!)
    (releasing!
      (let [arr (data/constant 1.0 [100] defs/AF_DTYPE_F64)
            bytes (internal/get-allocated-bytes arr)]
        ;; Double precision: 8 bytes per element
        (is (>= bytes 800))))))

;;;
;;; Convenience Functions Tests
;;;

(deftest test-array-info-complete
  (testing "array-info returns comprehensive information map"
    (device/init!)
    (releasing!
      (let [arr (data/range [50 100] 0 defs/AF_DTYPE_F32)
            info (internal/array-info arr)]
        (is (map? info))
        (is (contains? info :strides))
        (is (contains? info :offset))
        (is (contains? info :is-linear))
        (is (contains? info :is-owner))
        (is (contains? info :allocated-bytes))
        (is (contains? info :raw-ptr))))))

(deftest test-array-info-strides
  (testing "array-info :strides field"
    (device/init!)
    (releasing!
      (let [arr (data/range [10 20] 0 defs/AF_DTYPE_F32)
            info (internal/array-info arr)]
        (is (vector? (:strides info)))
        (is (= 4 (count (:strides info))))
        (is (= 1 (first (:strides info))))))))

(deftest test-array-info-offset
  (testing "array-info :offset field"
    (device/init!)
    (releasing!
      (let [arr (data/constant 3.0 [100] defs/AF_DTYPE_F32)
            info (internal/array-info arr)]
        (is (integer? (:offset info)))
        (is (>= (:offset info) 0))))))

(deftest test-array-info-is-linear
  (testing "array-info :is-linear field"
    (device/init!)
    (releasing!
      (let [arr (data/range [100 100] 0 defs/AF_DTYPE_F32)
            info (internal/array-info arr)]
        (is (boolean? (:is-linear info)))
        (is (true? (:is-linear info)))))))

(deftest test-array-info-is-owner
  (testing "array-info :is-owner field"
    (device/init!)
    (releasing!
      (let [arr (data/constant 7.0 [50 50] defs/AF_DTYPE_F32)
            info (internal/array-info arr)]
        (is (boolean? (:is-owner info)))
        (is (true? (:is-owner info)))))))

(deftest test-array-info-allocated-bytes
  (testing "array-info :allocated-bytes field"
    (device/init!)
    (releasing!
      (let [arr (data/range [200] 0 defs/AF_DTYPE_F32)
            info (internal/array-info arr)]
        (is (integer? (:allocated-bytes info)))
        (is (>= (:allocated-bytes info) 800))))))

(deftest test-array-info-raw-ptr
  (testing "array-info :raw-ptr field"
    (device/init!)
    (releasing!
      (let [arr (data/constant 2.0 [30 30] defs/AF_DTYPE_F32)
            info (internal/array-info arr)]
        (is (some? (:raw-ptr info)))))))

;;;
;;; Memory Layout Verification Tests
;;;

(deftest test-standard-row-major-layout
  (testing "standard row-major arrays have expected stride pattern"
    (device/init!)
    (releasing!
      (let [arr (data/range [10 20 30] 0 defs/AF_DTYPE_F32)
            [s0 s1 s2 _] (internal/get-strides arr)]
        (is (= 1 s0))
        (is (= 10 s1))
        (is (= (* 10 20) s2))))))

(deftest test-array-properties-consistency
  (testing "array properties are internally consistent"
    (device/init!)
    (releasing!
      (let [arr (data/constant 1.0 [100 200] defs/AF_DTYPE_F32)
            info (internal/array-info arr)
            dims (array/get-dims arr)
            numel (array/get-elements arr)]
        (is (= [100 200] dims))
        (is (= 20000 numel))
        (is (>= (:allocated-bytes info) (* numel 4)))))))

(deftest test-memory-efficiency
  (testing "memory allocation is reasonable for array size"
    (device/init!)
    (releasing!
      (let [arr (data/range [500 500] 0 defs/AF_DTYPE_F32)
            bytes (internal/get-allocated-bytes arr)
            expected-min (* 500 500 4)]
        ;; Should allocate at least the minimum required
        (is (>= bytes expected-min))
        ;; But not excessively more (allow 50% overhead for alignment/padding)
        (is (<= bytes (* expected-min 1.5)))))))

(comment
  ;; run all tests from REPL
  (run-tests)
  
  ;; run individual tests
  (run-test test-create-strided-array-simple)
  (run-test test-create-strided-array-with-offset)
  (run-test test-create-strided-array-transposed)
  (run-test test-create-strided-array-3d)
  (run-test test-create-strided-array-double)
  (run-test test-get-strides)
  (run-test test-get-strides-3d-array)
  (run-test test-get-offset)
  (run-test test-get-offset-full-array)
  (run-test test-is-linear-contiguous-array)
  (run-test test-is-linear-predicate)
  (run-test test-is-owner-owner-array)
  (run-test test-is-owner-predicate)
  (run-test test-get-raw-ptr)
  (run-test test-get-raw-ptr-non-zero)
  (run-test test-get-allocated-bytes)
  (run-test test-get-allocated-bytes-large-array)
  (run-test test-get-allocated-bytes-double)
  (run-test test-array-info-complete)
  (run-test test-array-info-strides)
  (run-test test-array-info-offset)
  (run-test test-array-info-is-linear)
  (run-test test-array-info-is-owner)
  (run-test test-array-info-allocated-bytes)
  (run-test test-array-info-raw-ptr)
  (run-test test-standard-row-major-layout)
  (run-test test-array-properties-consistency)
  (run-test test-memory-efficiency)
  
  ;
  )
