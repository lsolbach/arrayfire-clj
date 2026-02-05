(ns org.soulspace.arrayfire.integration.random-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [org.soulspace.arrayfire.integration.random :as rand]
            [org.soulspace.arrayfire.integration.array :as array]
            [org.soulspace.arrayfire.integration.device :as device]
            [org.soulspace.arrayfire.integration.jvm-integration :as jvm]
            [coffi.mem :as mem])
  (:import [org.soulspace.arrayfire.integration.jvm_integration AFArray]))

;;;
;;; Random Engine Management Tests
;;;

(deftest test-create-release-engine
  (testing "create and release random engine"
    (device/init!)
    (let [engine (rand/create-engine :philox 42)]
      (is (not (nil? engine)))
      (rand/release-engine! engine)
      (is true)))) ; If we get here, release succeeded

(deftest test-engine-type
  (testing "get and set engine type"
    (device/init!)
    (let [engine (rand/create-engine :philox 42)]
      (is (= rand/RANDOM-ENGINE-PHILOX (rand/get-engine-type engine)))
      (rand/set-engine-type! engine :mersenne)
      (is (= rand/RANDOM-ENGINE-MERSENNE (rand/get-engine-type engine)))
      (rand/release-engine! engine))))

(deftest test-engine-seed
  (testing "get and set engine seed"
    (device/init!)
    (let [engine (rand/create-engine :philox 12345)]
      (is (= 12345 (rand/get-engine-seed engine)))
      (rand/set-engine-seed! engine 99999)
      (is (= 99999 (rand/get-engine-seed engine)))
      (rand/release-engine! engine))))

(deftest test-retain-engine
  (testing "retain engine increments reference count"
    (device/init!)
    (let [engine (rand/create-engine :philox 42)
          engine2 (rand/retain-engine engine)]
      (is (not (nil? engine2)))
      (rand/release-engine! engine2)
      (rand/release-engine! engine))))

(deftest test-engine-type-name
  (testing "get engine type name"
    (device/init!)
    (is (= "Philox" (rand/engine-type-name rand/RANDOM-ENGINE-PHILOX)))
    (is (= "Threefry" (rand/engine-type-name rand/RANDOM-ENGINE-THREEFRY)))
    (is (= "Mersenne" (rand/engine-type-name rand/RANDOM-ENGINE-MERSENNE)))))

;;;
;;; Default Engine Tests
;;;

(deftest test-get-default-engine
  (testing "get default random engine"
    (device/init!)
    (let [engine (rand/get-default-engine)]
      (is (not (nil? engine)))
      ;; Should NOT release default engine
      )))

(deftest test-set-default-engine-type
  (testing "set default engine type"
    (device/init!)
    (rand/set-default-engine-type! :philox)
    (is true))) ; If we get here, it succeeded

(deftest test-seed-management
  (testing "set and get seed for default engine"
    (device/init!)
    (rand/set-seed! 42)
    (is (= 42 (rand/get-seed)))
    (rand/set-seed! 123)
    (is (= 123 (rand/get-seed)))))

;;;
;;; Random Generation with Default Engine Tests
;;;

(deftest test-randu
  (testing "randu generates uniform random array"
    (device/init!)
    (rand/set-seed! 42)
    (let [a (rand/randu [10] jvm/AF_DTYPE_F32)
          buf (mem/alloc (* 10 4))]
      (array/get-data-ptr a buf)
      ;; Check that values are in [0, 1) range
      (dotimes [i 10]
        (let [val (mem/read-float buf (* i 4))]
          (is (>= val 0.0))
          (is (< val 1.0))))
      (.close a))))

(deftest test-randu-2d
  (testing "randu with 2D dimensions"
    (device/init!)
    (rand/set-seed! 100)
    (let [a (rand/randu [5 5] jvm/AF_DTYPE_F32)]
      (is (instance? AFArray a))
      (is (= [5 5 1 1] (vec (array/get-dims a))))
      (.close a))))

(deftest test-randn
  (testing "randn generates normal random array"
    (device/init!)
    (rand/set-seed! 42)
    (let [a (rand/randn [100] jvm/AF_DTYPE_F32)
          buf (mem/alloc (* 100 4))]
      (array/get-data-ptr a buf)
      ;; Check that values exist (normal distribution can be any value)
      (let [val (mem/read-float buf 0)]
        (is (not (nil? val))))
      (.close a))))

(deftest test-randn-reproducibility
  (testing "same seed produces same random sequence"
    (device/init!)
    (rand/set-seed! 42)
    (let [a1 (rand/randu [5] jvm/AF_DTYPE_F32)
          buf1 (mem/alloc (* 5 4))]
      (array/get-data-ptr a1 buf1)
      (let [val1 (mem/read-float buf1 0)]
        (rand/set-seed! 42)
        (let [a2 (rand/randu [5] jvm/AF_DTYPE_F32)
              buf2 (mem/alloc (* 5 4))]
          (array/get-data-ptr a2 buf2)
          (let [val2 (mem/read-float buf2 0)]
            (is (= val1 val2)))
          (.close a2)))
      (.close a1))))

;;;
;;; Random Generation with Custom Engine Tests
;;;

(deftest test-random-uniform-with-engine
  (testing "random-uniform with custom engine"
    (device/init!)
    (let [engine (rand/create-engine :philox 42)
          a (rand/random-uniform [10] jvm/AF_DTYPE_F32 engine)
          buf (mem/alloc (* 10 4))]
      (array/get-data-ptr a buf)
      ;; Check that values are in [0, 1) range
      (let [val (mem/read-float buf 0)]
        (is (>= val 0.0))
        (is (< val 1.0)))
      (.close a)
      (rand/release-engine! engine))))

(deftest test-random-normal-with-engine
  (testing "random-normal with custom engine"
    (device/init!)
    (let [engine (rand/create-engine :mersenne 123)
          a (rand/random-normal [20] jvm/AF_DTYPE_F64 engine)
          buf (mem/alloc (* 20 8))]
      (array/get-data-ptr a buf)
      ;; Check that values exist
      (let [val (mem/read-double buf 0)]
        (is (not (nil? val))))
      (.close a)
      (rand/release-engine! engine))))

(deftest test-multiple-engines
  (testing "multiple independent engines"
    (device/init!)
    (let [engine1 (rand/create-engine :philox 42)
          engine2 (rand/create-engine :philox 42)
          a1 (rand/random-uniform [5] jvm/AF_DTYPE_F32 engine1)
          a2 (rand/random-uniform [5] jvm/AF_DTYPE_F32 engine2)
          buf1 (mem/alloc (* 5 4))
          buf2 (mem/alloc (* 5 4))]
      (array/get-data-ptr a1 buf1)
      (array/get-data-ptr a2 buf2)
      ;; Same seed should produce same values
      (is (= (mem/read-float buf1 0) (mem/read-float buf2 0)))
      (.close a1)
      (.close a2)
      (rand/release-engine! engine1)
      (rand/release-engine! engine2))))

(deftest test-engine-types
  (testing "different engine types produce different sequences"
    (device/init!)
    (let [engine-philox (rand/create-engine :philox 42)
          engine-mersenne (rand/create-engine :mersenne 42)
          a-philox (rand/random-uniform [5] jvm/AF_DTYPE_F32 engine-philox)
          a-mersenne (rand/random-uniform [5] jvm/AF_DTYPE_F32 engine-mersenne)
          buf-philox (mem/alloc (* 5 4))
          buf-mersenne (mem/alloc (* 5 4))]
      (array/get-data-ptr a-philox buf-philox)
      (array/get-data-ptr a-mersenne buf-mersenne)
      ;; Different engines should produce different values
      (is (not= (mem/read-float buf-philox 0) (mem/read-float buf-mersenne 0)))
      (.close a-philox)
      (.close a-mersenne)
      (rand/release-engine! engine-philox)
      (rand/release-engine! engine-mersenne))))

;;;
;;; Data Type Tests
;;;

(deftest test-randu-integer
  (testing "randu with integer types"
    (device/init!)
    (rand/set-seed! 42)
    (let [a (rand/randu [10] jvm/AF_DTYPE_S32)
          buf (mem/alloc (* 10 4))]
      (array/get-data-ptr a buf)
      ;; Integer uniform should span full range
      (is (not (nil? (mem/read-int buf 0))))
      (.close a))))

(deftest test-randn-double
  (testing "randn with double precision"
    (device/init!)
    (rand/set-seed! 42)
    (let [a (rand/randn [10] jvm/AF_DTYPE_F64)
          buf (mem/alloc (* 10 8))]
      (array/get-data-ptr a buf)
      (is (not (nil? (mem/read-double buf 0))))
      (.close a))))

(comment
  ;; run tests from REPL
  (run-tests)

  ;; run individual tests
  (run-test test-create-release-engine)
  (run-test test-engine-type)
  (run-test test-engine-seed)
  (run-test test-retain-engine)
  (run-test test-engine-type-name)
  (run-test test-get-default-engine)
  (run-test test-set-default-engine-type)
  (run-test test-seed-management)
  (run-test test-randu)
  (run-test test-randu-2d)
  (run-test test-randn)
  (run-test test-randn-reproducibility)
  (run-test test-random-uniform-with-engine)
  (run-test test-random-normal-with-engine)
  (run-test test-multiple-engines)
  (run-test test-engine-types)
  (run-test test-randu-integer)
  (run-test test-randn-double)
  
  ;
  )
