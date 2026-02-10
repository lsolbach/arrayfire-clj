(ns org.soulspace.arrayfire.integration.unified-api.arith-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [org.soulspace.arrayfire.integration.unified-api.arith :as arith]
            [org.soulspace.arrayfire.integration.unified-api.array :as array]
            [org.soulspace.arrayfire.integration.unified-api.device :as device]
            [org.soulspace.arrayfire.integration.base.jvm-integration :as jvm]
            [coffi.mem :as mem])
  (:import [org.soulspace.arrayfire.integration.base.jvm_integration AFArray]))

(defn- approx=
  "Compare expected/actual values within a tolerance."
  [expected actual tolerance]
  (<= (Math/abs (- (double expected) (double actual)))
      (double tolerance)))

;;;
;;; Unary Arithmetic Operations Tests
;;;

(deftest test-trunc
  (testing "trunc truncates values to integer parts"
    (device/init!)
    (let [a (array/create-array (float-array [1.7 -2.3 3.9]) [3] jvm/AF_DTYPE_F32)
          b (arith/trunc a)
          buf (mem/alloc (* 3 4))]
      (array/get-data-ptr b buf)
      (is (approx= 1.0 (mem/read-float buf 0) 0.001))
      (is (approx= -2.0 (mem/read-float buf 4) 0.001))
      (is (approx= 3.0 (mem/read-float buf 8) 0.001))
      (.close a)
      (.close b))))

(deftest test-sign
  (testing "sign returns sign of each element"
    (device/init!)
    (let [a (array/create-array (float-array [1.5 -2.5 0.0]) [3] jvm/AF_DTYPE_F32)
          b (arith/sign a)
          buf (mem/alloc (* 3 4))]
      (array/get-data-ptr b buf)
      (is (approx= 0.0 (mem/read-float buf 0) 0.001))
      (is (approx= 1.0 (mem/read-float buf 4) 0.001))
      (is (approx= 0.0 (mem/read-float buf 8) 0.001))
      (.close a)
      (.close b))))

(deftest test-round
  (testing "round rounds values to nearest integer"
    (device/init!)
    (let [a (array/create-array (float-array [1.4 2.6 -3.5]) [3] jvm/AF_DTYPE_F32)
          b (arith/round a)
          buf (mem/alloc (* 3 4))]
      (array/get-data-ptr b buf)
      (is (approx= 1.0 (mem/read-float buf 0) 0.001))
      (is (approx= 3.0 (mem/read-float buf 4) 0.001))
      (is (approx= -4.0 (mem/read-float buf 8) 0.001))
      (.close a)
      (.close b))))

(deftest test-floor
  (testing "floor computes floor of each element"
    (device/init!)
    (let [a (array/create-array (float-array [1.7 2.3 -3.2]) [3] jvm/AF_DTYPE_F32)
          b (arith/floor a)
          buf (mem/alloc (* 3 4))]
      (array/get-data-ptr b buf)
      (is (approx= 1.0 (mem/read-float buf 0) 0.001))
      (is (approx= 2.0 (mem/read-float buf 4) 0.001))
      (is (approx= -4.0 (mem/read-float buf 8) 0.001))
      (.close a)
      (.close b))))

(deftest test-ceil
  (testing "ceil computes ceiling of each element"
    (device/init!)
    (let [a (array/create-array (float-array [1.2 2.7 -3.8]) [3] jvm/AF_DTYPE_F32)
          b (arith/ceil a)
          buf (mem/alloc (* 3 4))]
      (array/get-data-ptr b buf)
      (is (approx= 2.0 (mem/read-float buf 0) 0.001))
      (is (approx= 3.0 (mem/read-float buf 4) 0.001))
      (is (approx= -3.0 (mem/read-float buf 8) 0.001))
      (.close a)
      (.close b))))

(deftest test-sqrt
  (testing "sqrt computes square root"
    (device/init!)
    (let [a (array/create-array (float-array [4.0 9.0 16.0]) [3] jvm/AF_DTYPE_F32)
          b (arith/sqrt a)
          buf (mem/alloc (* 3 4))]
      (array/get-data-ptr b buf)
      (is (approx= 2.0 (mem/read-float buf 0) 0.001))
      (is (approx= 3.0 (mem/read-float buf 4) 0.001))
      (is (approx= 4.0 (mem/read-float buf 8) 0.001))
      (.close a)
      (.close b))))

(deftest test-exp
  (testing "exp computes exponential"
    (device/init!)
    (let [a (array/create-array (float-array [0.0 1.0 2.0]) [3] jvm/AF_DTYPE_F32)
          b (arith/exp a)
          buf (mem/alloc (* 3 4))]
      (array/get-data-ptr b buf)
      (is (approx= 1.0 (mem/read-float buf 0) 0.001))
      (is (approx= 2.718 (mem/read-float buf 4) 0.01))
      (is (approx= 7.389 (mem/read-float buf 8) 0.01))
      (.close a)
      (.close b))))

(deftest test-log
  (testing "log computes natural logarithm"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.718 7.389]) [3] jvm/AF_DTYPE_F32)
          b (arith/log a)
          buf (mem/alloc (* 3 4))]
      (array/get-data-ptr b buf)
      (is (approx= 0.0 (mem/read-float buf 0) 0.001))
      (is (approx= 1.0 (mem/read-float buf 4) 0.01))
      (is (approx= 2.0 (mem/read-float buf 8) 0.01))
      (.close a)
      (.close b))))

(deftest test-sin
  (testing "sin computes sine"
    (device/init!)
    (let [a (array/create-array (float-array [0.0 1.5708 3.1416]) [3] jvm/AF_DTYPE_F32)
          b (arith/sin a)
          buf (mem/alloc (* 3 4))]
      (array/get-data-ptr b buf)
      (is (approx= 0.0 (mem/read-float buf 0) 0.001))
      (is (approx= 1.0 (mem/read-float buf 4) 0.001))
      (is (approx= 0.0 (mem/read-float buf 8) 0.001))
      (.close a)
      (.close b))))

(deftest test-cos
  (testing "cos computes cosine"
    (device/init!)
    (let [a (array/create-array (float-array [0.0 1.5708 3.1416]) [3] jvm/AF_DTYPE_F32)
          b (arith/cos a)
          buf (mem/alloc (* 3 4))]
      (array/get-data-ptr b buf)
      (is (approx= 1.0 (mem/read-float buf 0) 0.001))
      (is (approx= 0.0 (mem/read-float buf 4) 0.001))
      (is (approx= -1.0 (mem/read-float buf 8) 0.001))
      (.close a)
      (.close b))))

;;;
;;; Binary Arithmetic Operations Tests
;;;

(deftest test-add
  (testing "add performs element-wise addition"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0 3.0]) [3] jvm/AF_DTYPE_F32)
          b (array/create-array (float-array [4.0 5.0 6.0]) [3] jvm/AF_DTYPE_F32)
          c (arith/add a b false)
          buf (mem/alloc (* 3 4))]
      (array/get-data-ptr c buf)
      (is (approx= 5.0 (mem/read-float buf 0) 0.001))
      (is (approx= 7.0 (mem/read-float buf 4) 0.001))
      (is (approx= 9.0 (mem/read-float buf 8) 0.001))
      (.close a)
      (.close b)
      (.close c))))

(deftest test-sub
  (testing "sub performs element-wise subtraction"
    (device/init!)
    (let [a (array/create-array (float-array [5.0 7.0 9.0]) [3] jvm/AF_DTYPE_F32)
          b (array/create-array (float-array [1.0 2.0 3.0]) [3] jvm/AF_DTYPE_F32)
          c (arith/sub a b false)
          buf (mem/alloc (* 3 4))]
      (array/get-data-ptr c buf)
      (is (approx= 4.0 (mem/read-float buf 0) 0.001))
      (is (approx= 5.0 (mem/read-float buf 4) 0.001))
      (is (approx= 6.0 (mem/read-float buf 8) 0.001))
      (.close a)
      (.close b)
      (.close c))))

(deftest test-mul
  (testing "mul performs element-wise multiplication"
    (device/init!)
    (let [a (array/create-array (float-array [2.0 3.0 4.0]) [3] jvm/AF_DTYPE_F32)
          b (array/create-array (float-array [5.0 6.0 7.0]) [3] jvm/AF_DTYPE_F32)
          c (arith/mul a b false)
          buf (mem/alloc (* 3 4))]
      (array/get-data-ptr c buf)
      (is (approx= 10.0 (mem/read-float buf 0) 0.001))
      (is (approx= 18.0 (mem/read-float buf 4) 0.001))
      (is (approx= 28.0 (mem/read-float buf 8) 0.001))
      (.close a)
      (.close b)
      (.close c))))

(deftest test-div
  (testing "div performs element-wise division"
    (device/init!)
    (let [a (array/create-array (float-array [10.0 20.0 30.0]) [3] jvm/AF_DTYPE_F32)
          b (array/create-array (float-array [2.0 4.0 5.0]) [3] jvm/AF_DTYPE_F32)
          c (arith/div a b false)
          buf (mem/alloc (* 3 4))]
      (array/get-data-ptr c buf)
      (is (approx= 5.0 (mem/read-float buf 0) 0.001))
      (is (approx= 5.0 (mem/read-float buf 4) 0.001))
      (is (approx= 6.0 (mem/read-float buf 8) 0.001))
      (.close a)
      (.close b)
      (.close c))))

(deftest test-mod
  (testing "mod performs element-wise modulo"
    (device/init!)
    (let [a (array/create-array (float-array [10.0 11.0 12.0]) [3] jvm/AF_DTYPE_F32)
          b (array/create-array (float-array [3.0 4.0 5.0]) [3] jvm/AF_DTYPE_F32)
          c (arith/mod a b false)
          buf (mem/alloc (* 3 4))]
      (array/get-data-ptr c buf)
      (is (approx= 1.0 (mem/read-float buf 0) 0.001))
      (is (approx= 3.0 (mem/read-float buf 4) 0.001))
      (is (approx= 2.0 (mem/read-float buf 8) 0.001))
      (.close a)
      (.close b)
      (.close c))))

(deftest test-pow
  (testing "pow performs element-wise power"
    (device/init!)
    (let [a (array/create-array (float-array [2.0 3.0 4.0]) [3] jvm/AF_DTYPE_F32)
          b (array/create-array (float-array [3.0 2.0 2.0]) [3] jvm/AF_DTYPE_F32)
          c (arith/pow a b false)
          buf (mem/alloc (* 3 4))]
      (array/get-data-ptr c buf)
      (is (approx= 8.0 (mem/read-float buf 0) 0.001))
      (is (approx= 9.0 (mem/read-float buf 4) 0.001))
      (is (approx= 16.0 (mem/read-float buf 8) 0.001))
      (.close a)
      (.close b)
      (.close c))))

(deftest test-minof
  (testing "minof returns element-wise minimum"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 5.0 3.0]) [3] jvm/AF_DTYPE_F32)
          b (array/create-array (float-array [2.0 4.0 6.0]) [3] jvm/AF_DTYPE_F32)
          c (arith/minof a b false)
          buf (mem/alloc (* 3 4))]
      (array/get-data-ptr c buf)
      (is (approx= 1.0 (mem/read-float buf 0) 0.001))
      (is (approx= 4.0 (mem/read-float buf 4) 0.001))
      (is (approx= 3.0 (mem/read-float buf 8) 0.001))
      (.close a)
      (.close b)
      (.close c))))

(deftest test-maxof
  (testing "maxof returns element-wise maximum"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 5.0 3.0]) [3] jvm/AF_DTYPE_F32)
          b (array/create-array (float-array [2.0 4.0 6.0]) [3] jvm/AF_DTYPE_F32)
          c (arith/maxof a b false)
          buf (mem/alloc (* 3 4))]
      (array/get-data-ptr c buf)
      (is (approx= 2.0 (mem/read-float buf 0) 0.001))
      (is (approx= 5.0 (mem/read-float buf 4) 0.001))
      (is (approx= 6.0 (mem/read-float buf 8) 0.001))
      (.close a)
      (.close b)
      (.close c))))

;;;
;;; Comparison Operations Tests
;;;

(deftest test-lt
  (testing "lt performs element-wise less than comparison"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 5.0 3.0]) [3] jvm/AF_DTYPE_F32)
          b (array/create-array (float-array [2.0 4.0 3.0]) [3] jvm/AF_DTYPE_F32)
          c (arith/lt a b false)
          buf (mem/alloc (* 3 1))]
      (array/get-data-ptr c buf)
      (is (= 1 (mem/read-byte buf 0))) ; 1.0 < 2.0 => true
      (is (= 0 (mem/read-byte buf 1))) ; 5.0 < 4.0 => false
      (is (= 0 (mem/read-byte buf 2))) ; 3.0 < 3.0 => false
      (.close a)
      (.close b)
      (.close c))))

(deftest test-gt
  (testing "gt performs element-wise greater than comparison"
    (device/init!)
    (let [a (array/create-array (float-array [5.0 2.0 3.0]) [3] jvm/AF_DTYPE_F32)
          b (array/create-array (float-array [2.0 4.0 3.0]) [3] jvm/AF_DTYPE_F32)
          c (arith/gt a b false)
          buf (mem/alloc (* 3 1))]
      (array/get-data-ptr c buf)
      (is (= 1 (mem/read-byte buf 0))) ; 5.0 > 2.0 => true
      (is (= 0 (mem/read-byte buf 1))) ; 2.0 > 4.0 => false
      (is (= 0 (mem/read-byte buf 2))) ; 3.0 > 3.0 => false
      (.close a)
      (.close b)
      (.close c))))

(deftest test-eq
  (testing "eq performs element-wise equality comparison"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0 3.0]) [3] jvm/AF_DTYPE_F32)
          b (array/create-array (float-array [1.0 4.0 3.0]) [3] jvm/AF_DTYPE_F32)
          c (arith/eq a b false)
          buf (mem/alloc (* 3 1))]
      (array/get-data-ptr c buf)
      (is (= 1 (mem/read-byte buf 0))) ; 1.0 == 1.0 => true
      (is (= 0 (mem/read-byte buf 1))) ; 2.0 == 4.0 => false
      (is (= 1 (mem/read-byte buf 2))) ; 3.0 == 3.0 => true
      (.close a)
      (.close b)
      (.close c))))

;;;
;;; Logical Operations Tests
;;;

(deftest test-and
  (testing "and performs element-wise logical AND"
    (device/init!)
    (let [a (array/create-array (byte-array [1 0 1]) [3] jvm/AF_DTYPE_B8)
          b (array/create-array (byte-array [1 1 0]) [3] jvm/AF_DTYPE_B8)
          c (arith/and a b false)
          buf (mem/alloc (* 3 1))]
      (array/get-data-ptr c buf)
      (is (= 1 (mem/read-byte buf 0))) ; 1 AND 1 => 1
      (is (= 0 (mem/read-byte buf 1))) ; 0 AND 1 => 0
      (is (= 0 (mem/read-byte buf 2))) ; 1 AND 0 => 0
      (.close a)
      (.close b)
      (.close c))))

(deftest test-or
  (testing "or performs element-wise logical OR"
    (device/init!)
    (let [a (array/create-array (byte-array [1 0 0]) [3] jvm/AF_DTYPE_B8)
          b (array/create-array (byte-array [1 1 0]) [3] jvm/AF_DTYPE_B8)
          c (arith/or a b false)
          buf (mem/alloc (* 3 1))]
      (array/get-data-ptr c buf)
      (is (= 1 (mem/read-byte buf 0))) ; 1 OR 1 => 1
      (is (= 1 (mem/read-byte buf 1))) ; 0 OR 1 => 1
      (is (= 0 (mem/read-byte buf 2))) ; 0 OR 0 => 0
      (.close a)
      (.close b)
      (.close c))))

;;;
;;; Bitwise Operations Tests
;;;

(deftest test-bitand
  (testing "bitand performs element-wise bitwise AND"
    (device/init!)
    (let [a (array/create-array (int-array [5 3 7]) [3] jvm/AF_DTYPE_S32)
          b (array/create-array (int-array [3 5 2]) [3] jvm/AF_DTYPE_S32)
          c (arith/bitand a b false)
          buf (mem/alloc (* 3 4))]
      (array/get-data-ptr c buf)
      (is (= 1 (mem/read-int buf 0)))  ; 5 & 3 = 1
      (is (= 1 (mem/read-int buf 4)))  ; 3 & 5 = 1
      (is (= 2 (mem/read-int buf 8)))  ; 7 & 2 = 2
      (.close a)
      (.close b)
      (.close c))))

(deftest test-bitor
  (testing "bitor performs element-wise bitwise OR"
    (device/init!)
    (let [a (array/create-array (int-array [5 3 7]) [3] jvm/AF_DTYPE_S32)
          b (array/create-array (int-array [3 5 2]) [3] jvm/AF_DTYPE_S32)
          c (arith/bitor a b false)
          buf (mem/alloc (* 3 4))]
      (array/get-data-ptr c buf)
      (is (= 7 (mem/read-int buf 0)))  ; 5 | 3 = 7
      (is (= 7 (mem/read-int buf 4)))  ; 3 | 5 = 7
      (is (= 7 (mem/read-int buf 8)))  ; 7 | 2 = 7
      (.close a)
      (.close b)
      (.close c))))

;;;
;;; Cast and Clamp Tests
;;;

(deftest test-cast-float-to-double
  (testing "cast converts float array to double"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0 3.0]) [3] jvm/AF_DTYPE_F32)
          b (arith/cast a jvm/AF_DTYPE_F64)
          buf (mem/alloc (* 3 8))]
      (array/get-data-ptr b buf)
      (is (approx= 1.0 (mem/read-double buf 0) 0.001))
      (is (approx= 2.0 (mem/read-double buf 8) 0.001))
      (is (approx= 3.0 (mem/read-double buf 16) 0.001))
      (is (= jvm/AF_DTYPE_F64 (array/get-type b)))
      (.close a)
      (.close b))))

(deftest test-cast-int-to-float
  (testing "cast converts int array to float"
    (device/init!)
    (let [a (array/create-array (int-array [1 2 3]) [3] jvm/AF_DTYPE_S32)
          b (arith/cast a jvm/AF_DTYPE_F32)
          buf (mem/alloc (* 3 4))]
      (array/get-data-ptr b buf)
      (is (approx= 1.0 (mem/read-float buf 0) 0.001))
      (is (approx= 2.0 (mem/read-float buf 4) 0.001))
      (is (approx= 3.0 (mem/read-float buf 8) 0.001))
      (is (= jvm/AF_DTYPE_F32 (array/get-type b)))
      (.close a)
      (.close b))))

(deftest test-clamp
  (testing "clamp limits values to range"
    (device/init!)
        (let [a (array/create-array (float-array [0.5 5.0 10.5]) [3] jvm/AF_DTYPE_F32)
          lo (array/create-array (float-array [1.0 1.0 1.0]) [3] jvm/AF_DTYPE_F32)
          hi (array/create-array (float-array [10.0 10.0 10.0]) [3] jvm/AF_DTYPE_F32)
          c (arith/clamp a lo hi false)
          buf (mem/alloc (* 3 4))]
      (array/get-data-ptr c buf)
      (is (approx= 1.0 (mem/read-float buf 0) 0.001))   ; 0.5 clamped to 1.0
      (is (approx= 5.0 (mem/read-float buf 4) 0.001))   ; 5.0 unchanged
      (is (approx= 10.0 (mem/read-float buf 8) 0.001))  ; 10.5 clamped to 10.0
      (.close a)
      (.close lo)
      (.close hi)
      (.close c))))

(comment
  ;; run tests from REPL
  (run-tests)
  ;
  )
