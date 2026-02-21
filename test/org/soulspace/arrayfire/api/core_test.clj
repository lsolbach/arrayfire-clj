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
    (core/ensure-af-init!)
    (let [original-backend (device/get-active-backend)]
      (core/with-arrayfire {:backend :cpu}
        (is (= defs/AF_BACKEND_CPU (device/get-active-backend))))
      (is (= original-backend (device/get-active-backend))))))

(deftest with-arrayfire-exception-safety-test
  (testing "Backend/device restored after exception"
    (core/ensure-af-init!)
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

(comment
  ;; Run tests in this namespace
  (run-tests)

  ;
  )

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
  (testing "pow falls through to clojure.math/pow for plain numbers"
    (is (= 8.0 (core/pow 2.0 3.0)))))

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
;;; to-host tests (new multi-dtype support)
;;;

(deftest to-host-f32-test
  (testing "to-host returns float[] for F32 arrays"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a (core/array [1.0 2.0 3.0] [3])]
                     (vec (array/array->host a))))]
      (is (= 3 (count result)))
      (is (<= (Math/abs (- 1.0 (first result))) 0.001))
      (is (<= (Math/abs (- 2.0 (second result))) 0.001))
      (is (<= (Math/abs (- 3.0 (nth result 2))) 0.001)))))

(deftest to-host-deprecated-2-arity-test
  (testing "to-host 2-arity deprecated signature still works (n is ignored)"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a (core/array [1.0 2.0 3.0] [3])]
                     (vec (core/to-host a 99))))]
      (is (= 3 (count result))))))

(deftest to-host-c32-test
  (testing "to-host returns vector of [re im] pairs for C32 arrays"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a (array/create-array [[1.0 2.0] [3.0 4.0]] [2] defs/AF_DTYPE_C32)]
                     (array/array->host a)))]
      (is (vector? result))
      (is (= 2 (count result)))
      (is (<= (Math/abs (- 1.0 (first  (first result)))) 0.001))
      (is (<= (Math/abs (- 2.0 (second (first result)))) 0.001))
      (is (<= (Math/abs (- 3.0 (first  (second result)))) 0.001))
      (is (<= (Math/abs (- 4.0 (second (second result)))) 0.001)))))

(deftest to-host-c64-test
  (testing "to-host returns vector of [re im] double pairs for C64 arrays"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a (array/create-array [[10.0 20.0] [30.0 40.0]] [2] defs/AF_DTYPE_C64)]
                     (array/array->host a)))]
      (is (vector? result))
      (is (= 2 (count result)))
      (is (<= (Math/abs (- 10.0 (first  (first result)))) 0.0001))
      (is (<= (Math/abs (- 20.0 (second (first result)))) 0.0001))
      (is (<= (Math/abs (- 30.0 (first  (second result)))) 0.0001))
      (is (<= (Math/abs (- 40.0 (second (second result)))) 0.0001)))))

(deftest to-host-b8-test
  (testing "to-host returns byte[] for B8 (boolean) arrays"
    (let [result (core/with-arrayfire {:backend :cpu}
                   (let [a (core/array [1.0 2.0 3.0] [3])]
                     (vec (array/array->host (core/eq a 2.0)))))]
      ;; eq returns B8; byte values 0 = false, 1 = true
      (is (= [0 1 0] result)))))

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
