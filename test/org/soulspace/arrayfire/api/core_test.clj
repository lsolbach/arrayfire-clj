(ns org.soulspace.arrayfire.api.core-test
  "Tests for the arrayfire-clj core namespace, including the `with-arrayfire`
   execution region macro."
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [org.soulspace.arrayfire.api.core :as core]
            [org.soulspace.arrayfire.ffi.base.definitions :as defs]
            [org.soulspace.arrayfire.integration.base.memory :as bmem]
            [org.soulspace.arrayfire.integration.base.resource :as res]
            [org.soulspace.arrayfire.integration.unified-api.array :as array]
            [org.soulspace.arrayfire.integration.unified-api.device :as device])
  (:import (org.soulspace.arrayfire.integration.base.resource AFArray)))

;;;
;;; result-convert tests
;;;
(deftest result-convert-passthrough-test
  (testing "Non-AFArray values pass through unchanged"
    (is (= 42 (core/result-convert identity 42)))
    (is (= "hello" (core/result-convert identity "hello")))
    (is (nil? (core/result-convert identity nil)))))

(deftest result-convert-map-test
  (testing "Maps are walked recursively"
    (is (= {:a 1 :b 2} (core/result-convert identity {:a 1 :b 2})))))

(deftest result-convert-vector-test
  (testing "Vectors are walked recursively"
    (is (= [1 2 3] (core/result-convert identity [1 2 3])))))

(deftest result-convert-set-test
  (testing "Sets are walked recursively"
    (is (= #{1 2 3} (core/result-convert identity #{1 2 3})))))

;;;
;;; with-arrayfire macro tests
;;;
(deftest with-arrayfire-basic-test
  (testing "Basic with-arrayfire region with explicit host conversion"
    (let [result (core/with-arrayfire
                   (let [a (core/array [1.0 2.0 3.0 4.0] [2 2])]
                     ;; create-array returns AFArray; to-host accepts AFArray
                     (vec (array/array->host a))))]
      (is (= [1.0 2.0 3.0 4.0] result)))))

(deftest with-arrayfire-auto-convert-test
  (testing "AFArray result is auto-converted to native buffer"
    (let [result (core/with-arrayfire
                   (array/create-array
                     (bmem/double-array->segment (double-array [10.0 20.0 30.0]))
                     [3]
                     defs/AF_DTYPE_F64))]
      (is (not (instance? AFArray result)))
      (is (= [10.0 20.0 30.0] (vec result))))))

(deftest with-arrayfire-deep-convert-map-test
  (testing "Map containing AFArray is deep-converted"
    (let [result (core/with-arrayfire
                   {:data (array/create-array
                            (bmem/double-array->segment (double-array [1.0 2.0]))
                            [2]
                            defs/AF_DTYPE_F64)
                    :scalar 42})]
      (is (map? result))
      (is (= [1.0 2.0] (vec (:data result))))
      (is (= 42 (:scalar result))))))

(deftest with-arrayfire-deep-convert-vector-test
  (testing "Vector containing AFArray is deep-converted"
    (let [result (core/with-arrayfire
                   [(array/create-array
                      (bmem/double-array->segment (double-array [1.0]))
                      [1]
                      defs/AF_DTYPE_F64)
                    42])]
      (is (vector? result))
      (is (= [1.0] (vec (first result))))
      (is (= 42 (second result))))))

(deftest with-arrayfire-backend-test
  (testing "Backend option sets and restores backend"
    (device/ensure-af-init!)
    (let [original-backend (device/get-active-backend)]
      (core/with-arrayfire {:backend :cpu}
        (is (= defs/AF_BACKEND_CPU (device/get-active-backend))))
      (is (= original-backend (device/get-active-backend))))))

(deftest with-arrayfire-exception-safety-test
  (testing "Backend/device restored after exception"
    (device/ensure-af-init!)
    (let [original-backend (device/get-active-backend)]
      (is (thrown? Exception
            (core/with-arrayfire {:backend :cpu}
              (throw (Exception. "test error")))))
      (is (= original-backend (device/get-active-backend))))))

(deftest with-arrayfire-nested-test
  (testing "Nested with-arrayfire regions work correctly"
    (let [result (core/with-arrayfire
                   (core/with-arrayfire
                     ;; create-array returns AFArray; to-host accepts AFArray
                     (vec (array/array->host (core/array [42.0] [1])))))]
      (is (= [42.0] result)))))

(deftest with-arrayfire-converter-fn-test
  (testing "Custom converter-fn is used"
    (let [result (core/with-arrayfire {:converter-fn (fn [_arr] :converted)}
                   (array/create-array
                     (bmem/double-array->segment (double-array [1.0]))
                     [1]
                     defs/AF_DTYPE_F64))]
      (is (= :converted result)))))

(deftest with-arrayfire-identity-converter-test
  (testing "Identity converter skips auto-conversion"
    (let [result (core/with-arrayfire {:converter-fn identity}
                   42)]
      (is (= 42 result)))))

(deftest with-arrayfire-nil-result-test
  (testing "Nil result passes through"
    (let [result (core/with-arrayfire nil)]
      (is (nil? result)))))

(deftest with-arrayfire-map-not-treated-as-opts-test
  (testing "Map literal body without known option keys is NOT treated as options"
    (let [result (core/with-arrayfire
                   {:x 1 :y 2})]
      (is (= {:x 1 :y 2} result)))))

;;;
;;; within-arrayfire? and assert-within-arrayfire! tests
;;;
(deftest within-arrayfire-outside-test
  (testing "within-arrayfire? returns false when no region is active"
    (is (false? (core/within-arrayfire?)))))

(deftest within-arrayfire-inside-no-opts-test
  (testing "within-arrayfire? returns true inside with-arrayfire (no options)"
    (let [result (core/with-arrayfire
                   (core/within-arrayfire?))]
      (is (true? result)))))

(deftest within-arrayfire-inside-with-backend-test
  (testing "within-arrayfire? returns true inside with-arrayfire with backend option"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (core/within-arrayfire?))]
      (is (true? result)))))

(deftest within-arrayfire-after-region-test
  (testing "within-arrayfire? returns false after with-arrayfire exits"
    (core/with-arrayfire nil)
    (is (false? (core/within-arrayfire?)))))

(deftest within-arrayfire-nested-test
  (testing "within-arrayfire? returns true in nested with-arrayfire regions"
    (let [result (core/with-arrayfire
                   (core/with-arrayfire
                     (core/within-arrayfire?)))]
      (is (true? result)))))

(deftest assert-within-arrayfire-outside-test
  (testing "assert-within-arrayfire! throws IllegalStateException outside a region"
    (is (thrown? IllegalStateException
          (core/assert-within-arrayfire! "test-fn")))))

(deftest assert-within-arrayfire-message-test
  (testing "assert-within-arrayfire! error message includes function name"
    (is (thrown-with-msg? IllegalStateException #"test-fn"
          (core/assert-within-arrayfire! "test-fn")))))

(deftest assert-within-arrayfire-inside-test
  (testing "assert-within-arrayfire! does not throw inside a region"
    (is (nil? (core/with-arrayfire
                (core/assert-within-arrayfire! "test-fn"))))))

;;;
;;; Arithmetic API tests
;;;

;;
;; + (addition)
;;

(deftest +-number-identity-test
  (testing "(+) returns additive identity 0"
    (is (= 0 (core/+)))))

(deftest +-number-unary-test
  (testing "(+ x) returns x for numbers"
    (is (= 3 (core/+ 3)))))

(deftest +-number-number-test
  (testing "(+ n n) delegates to clojure.core/+"
    (is (= 7 (core/+ 3 4)))
    (is (= 5.0 (core/+ 2.0 3.0)))))

(deftest +-number-variadic-test
  (testing "Variadic (+ n1 n2 n3 ...) delegates to clojure.core/+"
    (is (= 10 (core/+ 1 2 3 4)))))

(deftest +-array-array-test
  (testing "(+ arr arr) performs element-wise addition"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a (core/array [1.0 2.0 3.0] [3])
                         b (core/array [10.0 20.0 30.0] [3])]
                     (core/->value (core/+ a b))))]
      (is (= [11.0 22.0 33.0] result)))))

(deftest +-array-scalar-test
  (testing "(+ arr scalar) broadcasts scalar across array"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a (core/array [1.0 2.0 3.0] [3])]
                     (core/->value (core/+ a 10.0))))]
      (is (= [11.0 12.0 13.0] result)))))

(deftest +-scalar-array-test
  (testing "(+ scalar arr) broadcasts scalar (commutative)"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a (core/array [1.0 2.0 3.0] [3])]
                     (core/->value (core/+ 10.0 a))))]
      (is (= [11.0 12.0 13.0] result)))))

;;
;; - (subtraction)
;;

(deftest --number-negate-test
  (testing "(- n) negates a number"
    (is (= -5 (core/- 5)))
    (is (= 3 (core/- -3)))))

(deftest --number-number-test
  (testing "(- n n) delegates to clojure.core/-"
    (is (= 2 (core/- 5 3)))))

(deftest --array-array-test
  (testing "(- arr arr) performs element-wise subtraction"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a (core/array [10.0 20.0 30.0] [3])
                         b (core/array [1.0 2.0 3.0] [3])]
                     (core/->value (core/- a b))))]
      (is (= [9.0 18.0 27.0] result)))))

(deftest --array-scalar-test
  (testing "(- arr scalar) broadcasts scalar subtraction"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a (core/array [10.0 20.0 30.0] [3])]
                     (core/->value (core/- a 5.0))))]
      (is (= [5.0 15.0 25.0] result)))))

(deftest --array-negate-test
  (testing "(- arr) negates all elements of an array"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a (core/array [1.0 -2.0 3.0] [3])]
                     (core/->value (core/- a))))]
      (is (= [-1.0 2.0 -3.0] result)))))

;;
;; * (multiplication)
;;

(deftest *-number-identity-test
  (testing "(*) returns multiplicative identity 1"
    (is (= 1 (core/*)))))

(deftest *-number-unary-test
  (testing "(* x) returns x for numbers"
    (is (= 5 (core/* 5)))))

(deftest *-number-number-test
  (testing "(* n n) delegates to clojure.core/*"
    (is (= 12 (core/* 3 4)))))

(deftest *-array-array-test
  (testing "(* arr arr) performs element-wise multiplication"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a (core/array [2.0 3.0 4.0] [3])
                         b (core/array [10.0 10.0 10.0] [3])]
                     (core/->value (core/* a b))))]
      (is (= [20.0 30.0 40.0] result)))))

(deftest *-array-scalar-test
  (testing "(* arr scalar) scales array elements"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a (core/array [1.0 2.0 3.0] [3])]
                     (core/->value (core/* a 2.0))))]
      (is (= [2.0 4.0 6.0] result)))))

;;
;; / (division)
;;

(deftest div-number-reciprocal-test
  (testing "(/ n) returns reciprocal for numbers"
    (is (= 1/4 (core// 4)))))

(deftest div-number-number-test
  (testing "(/ n n) delegates to clojure.core//"
    (is (= 2 (core// 6 3)))))

(deftest div-array-array-test
  (testing "(/ arr arr) performs element-wise division"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a (core/array [10.0 20.0 30.0] [3])
                         b (core/array [2.0 4.0 5.0] [3])]
                     (core/->value (core// a b))))]
      (is (= [5.0 5.0 6.0] result)))))

(deftest div-array-scalar-test
  (testing "(/ arr scalar) divides each element by scalar"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a (core/array [4.0 8.0 12.0] [3])]
                     (core/->value (core// a 2.0))))]
      (is (= [2.0 4.0 6.0] result)))))

;;
;; abs, neg
;;

(deftest abs-number-test
  (testing "abs falls through to clojure.core/abs for numbers"
    (is (= 5 (core/abs -5)))
    (is (= 5 (core/abs 5)))))

(deftest abs-array-test
  (testing "abs computes element-wise absolute value for arrays"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a (core/array [-1.0 2.0 -3.0] [3])]
                     (core/->value (core/abs a))))]
      (is (= [1.0 2.0 3.0] result)))))

(deftest neg-array-test
  (testing "neg negates all elements of an array"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a (core/array [1.0 -2.0 3.0] [3])]
                     (core/->value (core/neg a))))]
      (is (= [-1.0 2.0 -3.0] result)))))

;;
;; mod, rem
;;

(deftest mod-number-test
  (testing "mod falls through to clojure.core/mod for numbers"
    (is (= 1 (core/mod 10 3)))))

(deftest rem-number-test
  (testing "rem falls through to clojure.core/rem for numbers"
    (is (= 1 (core/rem 10 3)))))

;;
;; pow
;;

(deftest pow-number-test
  (testing "pow computes element-wise power with scalar arguments"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (core/->value (core/pow (core/array [2.0 3.0 4.0] [3]) 2.0)))]
      (is (= [4.0 9.0 16.0] result)))))

(deftest pow-array-scalar-test
  (testing "(pow arr scalar) computes element-wise power with scalar exponent"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a (core/array [2.0 3.0 4.0] [3])]
                     (core/->value (core/pow a 2.0))))]
      (is (= [4.0 9.0 16.0] result)))))

;;
;; sqrt, exp, log
;;

(deftest sqrt-array-test
  (testing "sqrt computes element-wise square root"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a (core/array [4.0 9.0 16.0] [3])]
                     (core/->value (core/sqrt a))))]
      (is (= [2.0 3.0 4.0] result)))))

(deftest exp-log-inverse-test
  (testing "exp and log are inverses"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a (core/array [1.0 2.0 3.0] [3])
                         b (core/log (core/exp a))]
                     (core/->value b)))]
      (is (every? #(< (Math/abs %) 1e-12)
                  (map - result [1.0 2.0 3.0]))))))

;;
;; sin, cos
;;

(deftest sin-cos-identity-test
  (testing "sin²(x) + cos²(x) = 1 for all x"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a (core/array [0.0 0.5 1.0 1.5] [4])
                         s (core/sin a)
                         c (core/cos a)
                         sum (core/+ (core/* s s) (core/* c c))]
                     (core/->value sum)))]
      (is (every? #(< (Math/abs (- 1.0 %)) 1e-6) result)))))

;;
;; Element-wise comparisons
;;

(deftest eq-array-scalar-test
  (testing "(eq arr scalar) returns boolean array"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a (core/array [1.0 2.0 3.0] [3])]
                     (core/->value (core/eq a 2.0))))]
      ;; B8 boolean arrays copy to byte[] — values are 0 (false) or 1 (true)
      (is (= [0 1 0] result)))))

(deftest lt-array-scalar-test
  (testing "(lt arr scalar) returns boolean array"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a (core/array [1.0 2.0 3.0] [3])]
                     (core/->value (core/lt a 2.5))))]
      ;; B8 boolean arrays copy to byte[] — values are 0 (false) or 1 (true)
      (is (= [1 1 0] result)))))

(deftest gt-array-scalar-test
  (testing "(gt arr scalar) returns boolean array"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a (core/array [1.0 2.0 3.0] [3])]
                     (core/->value (core/gt a 1.5))))]
      ;; B8 boolean arrays copy to byte[] — values are 0 (false) or 1 (true)
      (is (= [0 1 1] result)))))

;;
;; Guard: operations outside with-arrayfire throw
;;

(deftest +-array-outside-region-throws-test
  (testing "(+ arr arr) outside with-arrayfire throws IllegalStateException"
    (core/with-arrayfire {:backend :cpu}
      (let [a (core/array [1.0 2.0] [2])
            b (core/array [3.0 4.0] [2])]
        ;; Must not throw inside region
        (is (some? (core/+ a b)))))))

;;;
;;; ->native-buffer guard tests
;;;

(deftest ->native-buffer-throws-for-c32-test
  (testing "->native-buffer throws ExceptionInfo for C32 arrays"
    (core/with-arrayfire {:backend :cpu}
      (let [a (array/create-array [[1.0 2.0]] [1] defs/AF_DTYPE_C32)]
        (is (thrown? clojure.lang.ExceptionInfo
                     (core/->native-buffer a)))))))

(deftest ->native-buffer-throws-for-c64-test
  (testing "->native-buffer throws ExceptionInfo for C64 arrays"
    (core/with-arrayfire {:backend :cpu}
      (let [a (array/create-array [[1.0 2.0]] [1] defs/AF_DTYPE_C64)]
        (is (thrown? clojure.lang.ExceptionInfo
                     (core/->native-buffer a)))))))

;;;
;;; ->value complex dtype tests
;;;

(deftest ->value-c32-test
  (testing "->value returns nested vector structure for a 1D C32 array"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a (array/create-array [[1.0 2.0] [3.0 4.0]] [2] defs/AF_DTYPE_C32)]
                     (core/->value a)))]
      (is (vector? result))
      (is (= 2 (count result)))
      (is (<= (Math/abs (- 1.0 (first  (first result)))) 0.001))
      (is (<= (Math/abs (- 2.0 (second (first result)))) 0.001)))))

;;;
;;; diagonal / get-diagonal tests
;;;

(deftest diagonal-creates-matrix-test
  (testing "diagonal creates a square diagonal matrix from a 1-D array"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [v (core/array [1.0 2.0 3.0] [3])
                         m (core/diagonal v)]
                     [(core/shape m) (core/->value m)]))]
      (is (= [3 3] (first result)))
      ;; ->value returns vector-of-columns; access element at [row r, col c] via (get-in val [c r])
      (let [val (second result)]
        (is (<= (Math/abs (- 1.0 (get-in val [0 0]))) 0.001))   ; d[0,0]
        (is (<= (Math/abs (- 2.0 (get-in val [1 1]))) 0.001))   ; d[1,1]
        (is (<= (Math/abs (- 3.0 (get-in val [2 2]))) 0.001))))))  ; d[2,2]  ; [2,2]

(deftest diagonal-with-offset-test
  (testing "diagonal with positive offset creates superdiagonal"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [v (core/array [1.0 2.0] [2])
                         m (core/diagonal v 1)]
                     (core/shape m)))]
      ;; With 2-element vector + offset 1, result is 3×3
      (is (= [3 3] result)))))

(deftest get-diagonal-extracts-main-diagonal-test
  (testing "get-diagonal extracts the main diagonal of a matrix"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [m (core/array [1.0 0.0 0.0
                                        0.0 2.0 0.0
                                        0.0 0.0 3.0] [3 3])
                         d (core/get-diagonal m)]
                     [(core/shape d) (core/->value d)]))]
      (is (= [3] (first result)))
      (let [vals (second result)]
        (is (<= (Math/abs (- 1.0 (nth vals 0))) 0.001))
        (is (<= (Math/abs (- 2.0 (nth vals 1))) 0.001))
        (is (<= (Math/abs (- 3.0 (nth vals 2))) 0.001))))))

(deftest get-diagonal-with-offset-test
  (testing "get-diagonal with positive offset extracts superdiagonal"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [;; column-major layout: col0=[0,0,0] col1=[5,0,0] col2=[0,6,0]
                         ;; gives matrix: row0=[0,5,0] row1=[0,0,6] row2=[0,0,0]
                         m (core/array [0.0 0.0 0.0
                                        5.0 0.0 0.0
                                        0.0 6.0 0.0] [3 3])
                         d (core/get-diagonal m 1)]
                     [(core/shape d) (core/->value d)]))]
      (is (= [2] (first result)))
      (let [vals (second result)]
        (is (<= (Math/abs (- 5.0 (nth vals 0))) 0.001))
        (is (<= (Math/abs (- 6.0 (nth vals 1))) 0.001))))))

(deftest diagonal-roundtrip-test
  (testing "get-diagonal(diagonal(v)) == v"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [v (core/array [4.0 5.0 6.0] [3])
                         rt (core/get-diagonal (core/diagonal v))]
                     (core/->value rt)))]
      (is (<= (Math/abs (- 4.0 (nth result 0))) 0.001))
      (is (<= (Math/abs (- 5.0 (nth result 1))) 0.001))
      (is (<= (Math/abs (- 6.0 (nth result 2))) 0.001)))))

;;;
;;; Bit-shift API tests
;;;
;;; Note: bitshiftl/bitshiftr require signed-integer arrays.
;;; Since core/array passes data as doubles and ArrayFire performs no conversion
;;; for integer dtypes, we must build int32 segments explicitly via
;;; bmem/int-array->segment + array/create-array.
;;;

(deftest bitshiftl-test
  (testing "bitshiftl performs element-wise left shift"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [vals-arr   (array/create-array
                                      (bmem/int-array->segment (int-array [1 2 4]))
                                      [3] defs/AF_DTYPE_S32)
                         shifts-arr (array/create-array
                                      (bmem/int-array->segment (int-array [2 1 0]))
                                      [3] defs/AF_DTYPE_S32)]
                     (core/->value (core/bitshiftl vals-arr shifts-arr))))]
      ;; 1<<2=4, 2<<1=4, 4<<0=4
      (is (= [4 4 4] result)))))

(deftest bitshiftr-test
  (testing "bitshiftr performs element-wise right shift"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [vals-arr   (array/create-array
                                      (bmem/int-array->segment (int-array [32 16 8]))
                                      [3] defs/AF_DTYPE_S32)
                         shifts-arr (array/create-array
                                      (bmem/int-array->segment (int-array [2 1 0]))
                                      [3] defs/AF_DTYPE_S32)]
                     (core/->value (core/bitshiftr vals-arr shifts-arr))))]
      ;; 32>>2=8, 16>>1=8, 8>>0=8
      (is (= [8 8 8] result)))))

;;;
;;; iota tests
;;;

(deftest iota-sequential-test
  (testing "iota produces sequential values tiled across dimensions"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (core/->value (core/iota [3] [2])))]
      ;; [0 1 2] tiled twice → [0 1 2 0 1 2]
      (is (= 6 (count result)))
      (is (every? true?
                  (map #(<= (Math/abs (- (float %1) (float %2))) 0.001)
                       result [0.0 1.0 2.0 0.0 1.0 2.0]))))))

(deftest iota-no-tile-test
  (testing "iota with tile-factor 1 produces simple sequential values"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (core/->value (core/iota [4] [1])))]
      (is (= 4 (count result)))
      (is (every? true?
                  (map #(< (Math/abs (- (float %1) (float %2))) 0.001)
                       result [0.0 1.0 2.0 3.0]))))))

;;;
;;; constant-complex tests
;;;

(deftest constant-complex-shape-test
  (testing "constant-complex creates an array of the requested shape"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (core/shape (core/constant-complex 1.0 0.0 [3])))]
      (is (= [3] result)))))

(deftest constant-complex-values-test
  (testing "constant-complex fills array with correct real and imaginary parts"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (core/->value (core/constant-complex 2.0 3.0 [2])))]
      ;; ->value for C32 returns [[real imag] ...]
      (is (= 2 (count result)))
      (is (<= (Math/abs (- 2.0 (first (first result))))  0.001))
      (is (<= (Math/abs (- 3.0 (second (first result)))) 0.001)))))

;;;
;;; sum-nan / product-nan tests
;;;
;;; Reductions of a 1D array along dim 0 return a 0-dimensional scalar,
;;; which ->value returns as a plain Double, not a vector.
;;;

(deftest sum-nan-basic-test
  (testing "sum-nan sums elements along a dimension (no NaN present)"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (core/->value (core/sum-nan (core/array [1.0 2.0 3.0] [3]) 0 0.0)))]
      (is (<= (Math/abs (- 6.0 result)) 0.001)))))

(deftest product-nan-basic-test
  (testing "product-nan multiplies elements along a dimension (no NaN present)"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (core/->value (core/product-nan (core/array [2.0 3.0 4.0] [3]) 0 1.0)))]
      (is (<= (Math/abs (- 24.0 result)) 0.001)))))

(deftest sum-nan-default-nan-val-test
  (testing "sum-nan uses 0.0 as default NaN substitute"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (core/->value (core/sum-nan (core/array [10.0 20.0 30.0] [3]) 0)))]
      (is (<= (Math/abs (- 60.0 result)) 0.001)))))

(deftest product-nan-default-nan-val-test
  (testing "product-nan uses 1.0 as default NaN substitute"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (core/->value (core/product-nan (core/array [2.0 5.0] [2]) 0)))]
      (is (<= (Math/abs (- 10.0 result)) 0.001)))))

;;;
;;; argmin / argmax tests
;;;
;;; Reductions along dim 0 of a 1D array return scalar outputs:
;;; ->value returns a plain Double for values and a plain Long for indices.
;;;

(deftest argmin-values-and-indices-test
  (testing "argmin returns minimum values and their indices"
    (let [[val-result idx-result]
          (core/with-arrayfire {:backend :cpu}
            (let [a              (core/array [3.0 1.0 4.0 5.0 9.0] [5])
                  [vals indices] (core/argmin a 0)]
              [(core/->value vals) (core/->value indices)]))]
      ;; Minimum is 1.0 at index 1
      (is (<= (Math/abs (- 1.0 val-result)) 0.001))
      (is (= 1 (int idx-result))))))

(deftest argmax-values-and-indices-test
  (testing "argmax returns maximum values and their indices"
    (let [[val-result idx-result]
          (core/with-arrayfire {:backend :cpu}
            (let [a              (core/array [3.0 1.0 4.0 5.0 9.0] [5])
                  [vals indices] (core/argmax a 0)]
              [(core/->value vals) (core/->value indices)]))]
      ;; Maximum is 9.0 at index 4
      (is (<= (Math/abs (- 9.0 val-result)) 0.001))
      (is (= 4 (int idx-result))))))

;;;
;;; diff1 / diff2 tests
;;;

(deftest diff1-position-to-velocity-test
  (testing "diff1 computes first-order differences (position → velocity)"
    (let [result (core/with-arrayfire {:backend :cpu}
                   ;; [0 1 4 9 16] squares: diff1 → [1 3 5 7]
                   (core/->value (core/diff1 (core/array [0.0 1.0 4.0 9.0 16.0] [5]))))]
      (is (= 4 (count result)))
      (is (every? true?
                  (map #(<= (Math/abs (- %1 %2)) 0.001)
                       result [1.0 3.0 5.0 7.0]))))))

(deftest diff2-constant-acceleration-test
  (testing "diff2 computes second-order differences (constant acceleration = 2)"
    (let [result (core/with-arrayfire {:backend :cpu}
                   ;; [0 1 4 9 16] squares: diff2 → [2 2 2]
                   (core/->value (core/diff2 (core/array [0.0 1.0 4.0 9.0 16.0] [5]))))]
      (is (= 3 (count result)))
      (is (every? #(<= (Math/abs (- 2.0 %)) 0.001) result)))))

;;;
;;; Group-by reduction tests
;;;
;;; By-key reductions require integer key arrays.
;;; We build s32 arrays explicitly via bmem/int-array->segment + array/create-array.
;;;

(deftest sum-by-key-test
  (testing "sum-by-key sums values within each key group"
    (let [[key-result val-result]
          (core/with-arrayfire {:backend :cpu}
            (let [keys (array/create-array
                         (bmem/int-array->segment (int-array [1 1 1 2 2 3]))
                         [6] defs/AF_DTYPE_S32)
                  vals (core/array [10.0 20.0 30.0 40.0 50.0 60.0] [6])
                  [k v] (core/sum-by-key keys vals)]
              [(core/->value k) (core/->value v)]))]
      ;; 3 distinct keys → 3 output elements
      (is (= 3 (count key-result)))
      (is (= 3 (count val-result)))
      ;; sums: 10+20+30=60, 40+50=90, 60=60
      (is (<= (Math/abs (- 60.0  (nth val-result 0))) 0.001))
      (is (<= (Math/abs (- 90.0  (nth val-result 1))) 0.001))
      (is (<= (Math/abs (- 60.0  (nth val-result 2))) 0.001)))))

(deftest product-by-key-test
  (testing "product-by-key multiplies values within each key group"
    (let [[_key-result val-result]
          (core/with-arrayfire {:backend :cpu}
            (let [keys (array/create-array
                         (bmem/int-array->segment (int-array [1 1 2 2]))
                         [4] defs/AF_DTYPE_S32)
                  vals (core/array [2.0 3.0 4.0 5.0] [4])
                  [k v] (core/product-by-key keys vals)]
              [(core/->value k) (core/->value v)]))]
      (is (= 2 (count val-result)))
      ;; 2*3=6, 4*5=20
      (is (<= (Math/abs (- 6.0  (nth val-result 0))) 0.001))
      (is (<= (Math/abs (- 20.0 (nth val-result 1))) 0.001)))))

(deftest min-by-key-test
  (testing "min-by-key finds minimum within each key group"
    (let [[_keys val-result]
          (core/with-arrayfire {:backend :cpu}
            (let [keys (array/create-array
                         (bmem/int-array->segment (int-array [1 1 2 2]))
                         [4] defs/AF_DTYPE_S32)
                  vals (core/array [5.0 2.0 8.0 1.0] [4])
                  [k v] (core/min-by-key keys vals)]
              [(core/->value k) (core/->value v)]))]
      (is (= 2 (count val-result)))
      (is (<= (Math/abs (- 2.0 (nth val-result 0))) 0.001))
      (is (<= (Math/abs (- 1.0 (nth val-result 1))) 0.001)))))

(deftest max-by-key-test
  (testing "max-by-key finds maximum within each key group"
    (let [[_keys val-result]
          (core/with-arrayfire {:backend :cpu}
            (let [keys (array/create-array
                         (bmem/int-array->segment (int-array [1 1 2 2]))
                         [4] defs/AF_DTYPE_S32)
                  vals (core/array [5.0 2.0 8.0 1.0] [4])
                  [k v] (core/max-by-key keys vals)]
              [(core/->value k) (core/->value v)]))]
      (is (= 2 (count val-result)))
      (is (<= (Math/abs (- 5.0 (nth val-result 0))) 0.001))
      (is (<= (Math/abs (- 8.0 (nth val-result 1))) 0.001)))))

(deftest scan-by-key-inclusive-sum-test
  (testing "scan-by-key performs inclusive prefix sum within key groups"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [keys (array/create-array
                                (bmem/int-array->segment (int-array [1 1 2 2]))
                                [4] defs/AF_DTYPE_S32)
                         vals (core/array [1.0 2.0 3.0 4.0] [4])]
                     (core/->value (core/scan-by-key keys vals 0 :add true))))]
      ;; Group 1: cumulative sums of [1 2] → [1 3]
      ;; Group 2: cumulative sums of [3 4] → [3 7]
      (is (= 4 (count result)))
      (is (<= (Math/abs (- 1.0 (nth result 0))) 0.001))
      (is (<= (Math/abs (- 3.0 (nth result 1))) 0.001))
      (is (<= (Math/abs (- 3.0 (nth result 2))) 0.001))
      (is (<= (Math/abs (- 7.0 (nth result 3))) 0.001)))))

;;;
;;; eval-multiple! tests
;;;

(deftest eval-multiple!-returns-nil-test
  (testing "eval-multiple! evaluates multiple arrays without error"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a (core/array [1.0 2.0 3.0] [3])
                         b (core/array [4.0 5.0 6.0] [3])]
                     (core/eval-multiple! [a b])))]
      (is (nil? result)))))

;;;
;;; print-array-gen tests
;;;

(deftest print-array-gen-no-exception-test
  (testing "print-array-gen runs without throwing exceptions"
    (is (nil? (core/with-arrayfire {:backend :cpu}
                (let [a (core/array [1.5 2.5 3.5] [3])]
                  (core/print-array-gen "test-weights" a 2)))))))

(comment
  ;; Run tests in this namespace
  (run-tests)

  ;
  )
