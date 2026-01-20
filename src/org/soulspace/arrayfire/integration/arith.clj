(ns org.soulspace.arrayfire.integration.arith
  (:require [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.unary :as unary]
            [org.soulspace.arrayfire.ffi.binary :as binary]
            [org.soulspace.arrayfire.integration.jvm-integration :as jvm])
  (:import (org.soulspace.arrayfire.integration.jvm_integration AFArray)))

;;;
;;; Unary Arithmetic Operations
;;;

;;
;; Rounding and Truncation
;;
(defn trunc
  "Truncate the values of an array to their integer parts.

   Supported types: f32, f64

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray with values truncated to their integer parts."
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-trunc out (jvm/af-handle a)) "af-trunc")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn sign
  "Compute the sign of each element in the array.

   Supported types: f32, f64, s32, s64

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray where each element is:
   - 1 if the corresponding element in 'a' is positive
   - -1 if the corresponding element in 'a' is negative
   - 0 if the corresponding element in 'a' is zero."
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-sign out (jvm/af-handle a)) "af-sign")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn round
  "Round the elements of the array to the nearest integer.

   Supported types: f32, f64

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray with values rounded to the nearest integer."
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-round out (jvm/af-handle a)) "af-round")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn floor
  "Compute the floor of each element in the array.

   Supported types: f32, f64

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray with the largest integer less than or equal to each element."
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-floor out (jvm/af-handle a)) "af-floor")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn ceil
  "Compute the ceiling of each element in the array.
   
   Supported types: f32, f64

   Parameters:
   - a: Input array (AFArray)
    
   Returns:
   An AFArray with the smallest integer greater than or equal to each element."
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-ceil out (jvm/af-handle a)) "af-ceil")
    (jvm/af-array-new (jvm/deref-af-array out))))

;;
;; Activation and special functions
;;

(defn sigmoid
  "Compute the sigmoid activation function for each element in the array.

   Supported types: f32, f64

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray where each element is transformed by the sigmoid function:
   sigmoid(x) = 1 / (1 + exp(-x))"
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-sigmoid out (jvm/af-handle a)) "af-sigmoid")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn expm1
  "Compute exp(x) - 1 for each element in the array.

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray where each element is transformed by the expm1 function:
   expm1(x) = exp(x) - 1"
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-expm1 out (jvm/af-handle a)) "af-expm1")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn erf
  "Compute the error function for each element in the array.

   Supported types: f32, f64

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray where each element is transformed by the error function:
   erf(x) = (2/sqrt(π)) * ∫[0 to x] exp(-t²) dt"
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-erf out (jvm/af-handle a)) "af-erf")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn erfc
  "Compute the complementary error function for each element in the array.

   Supported types: f32, f64

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray where each element is transformed by the complementary error function:
   erfc(x) = 1 - erf(x)"
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-erfc out (jvm/af-handle a)) "af-erfc")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn log10
  "Compute the base-10 logarithm for each element in the array.

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray where each element is transformed by the base-10 logarithm:
   log10(x) = log(x) / log(10)"
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-log10 out (jvm/af-handle a)) "af-log10")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn log1p
  "Compute log(1 + x) for each element in the array.

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray where each element is transformed by the log1p function:
   log1p(x) = log(1 + x)"
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-log1p out (jvm/af-handle a)) "af-log1p")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn log2
  "Compute the base-2 logarithm for each element in the array.

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray where each element is transformed by the base-2 logarithm:
   log2(x) = log(x) / log(2)"
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-log2 out (jvm/af-handle a)) "af-log2")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn cbrt
  "Compute the cube root for each element in the array.

   Supported types: f32, f64

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray where each element is transformed by the cube root function:
   cbrt(x) = x^(1/3)"
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-cbrt out (jvm/af-handle a)) "af-cbrt")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn rsqrt
  "Compute the reciprocal square root for each element in the array.

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray where each element is transformed by the reciprocal square root function:
   rsqrt(x) = 1 / sqrt(x)"
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-rsqrt out (jvm/af-handle a)) "af-rsqrt")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn tgamma
  "Compute the truncated gamma function for each element in the array.

   Supported types: f32, f64

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray where each element is transformed by the truncated gamma function."
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-tgamma out (jvm/af-handle a)) "af-tgamma")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn lgamma
  "Compute the natural logarithm of the absolute value of the gamma function
   for each element in the array.

   Supported types: f32, f64

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray where each element is transformed by the lgamma function:
   lgamma(x) = log(|gamma(x)|)"
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-lgamma out (jvm/af-handle a)) "af-lgamma")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn sin
  "Compute the sine of each element in the array.

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray where each element is transformed by the sine function:
   sin(x) = sin(x)"
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-sin out (jvm/af-handle a)) "af-sin")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn cos
  "Compute the cosine of each element in the array.
   
   Supported types: f32, f64, c32, c64

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray where each element is transformed by the cosine function:
   cos(x) = cos(x)"
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-cos out (jvm/af-handle a)) "af-cos")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn tan
  "Compute the tangent of each element in the array.
   
   Supported types: f32, f64, c32, c64

   Parameters:
   - a: Input array (AFArray)
    
   Returns:
   An AFArray where each element is transformed by the tangent function:
   tan(x) = tan(x)"
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-tan out (jvm/af-handle a)) "af-tan")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn asin
  "Compute the arcsine of each element in the array.

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray where each element is transformed by the arcsine function:
   asin(x) = arcsin(x)"
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-asin out (jvm/af-handle a)) "af-asin")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn acos
  "Compute the arccosine of each element in the array.
   
   Supported types: f32, f64, c32, c64

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray where each element is transformed by the arccosine function:
   acos(x) = arccos(x)"
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-acos out (jvm/af-handle a)) "af-acos")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn atan
  "Compute the arctangent of each element in the array.
   
   Supported types: f32, f64, c32, c64

   Parameters:
   - a: Input array (AFArray)
    
   Returns:
   An AFArray where each element is transformed by the arctangent function:
   atan(x) = arctan(x)"
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-atan out (jvm/af-handle a)) "af-atan")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn sinh
  "Compute the hyperbolic sine of each element in the array.

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray where each element is transformed by the hyperbolic sine function:
   sinh(x) = (exp(x) - exp(-x)) / 2"
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-sinh out (jvm/af-handle a)) "af-sinh")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn cosh
  "Compute the hyperbolic cosine of each element in the array.
   
   Supported types: f32, f64, c32, c64

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray where each element is transformed by the hyperbolic cosine function:
   cosh(x) = (exp(x) + exp(-x)) / 2"
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-cosh out (jvm/af-handle a)) "af-cosh")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn tanh
  "Compute the hyperbolic tangent of each element in the array.
   
   Supported types: f32, f64, c32, c64

   Parameters:
   - a: Input array (AFArray)
     
   Returns:
   An AFArray where each element is transformed by the hyperbolic tangent function:
   tanh(x) = sinh(x) / cosh(x)"
  [^AFArray a]
    (let [out (jvm/native-af-array-pointer)]
        (jvm/check! (unary/af-tanh out (jvm/af-handle a)) "af-tanh")
        (jvm/af-array-new (jvm/deref-af-array out))))

(defn asinh
  "Compute the inverse hyperbolic sine of each element in the array.

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray where each element is transformed by the inverse hyperbolic sine function:
   asinh(x) = log(x + sqrt(x² + 1))"
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-asinh out (jvm/af-handle a)) "af-asinh")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn acosh
  "Compute the inverse hyperbolic cosine of each element in the array.
   
   Supported types: f32, f64, c32, c64

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray where each element is transformed by the inverse hyperbolic cosine function:
   acosh(x) = log(x + sqrt(x² - 1))"
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-acosh out (jvm/af-handle a)) "af-acosh")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn atanh
  "Compute the inverse hyperbolic tangent of each element in the array.
    
   Supported types: f32, f64, c32, c64

   Parameters:
   - a: Input array (AFArray)
     
   Returns:
   An AFArray where each element is transformed by the inverse hyperbolic tangent function:
   atanh(x) = 0.5 * log((1 + x) / (1 - x))"
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-atanh out (jvm/af-handle a)) "af-atanh")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn exp
  "Compute the exponential of each element in the array.

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray where each element is transformed by the exponential function:
   exp(x) = e^x"
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-exp out (jvm/af-handle a)) "af-exp")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn log
  "Compute the natural logarithm of each element in the array.

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray where each element is transformed by the natural logarithm function:
   log(x) = ln(x)"
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-log out (jvm/af-handle a)) "af-log")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn sqrt
  "Compute the square root of each element in the array.

   Supported types: f32, f64, c32, c64

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray where each element is transformed by the square root function:
   sqrt(x) = √x"
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-sqrt out (jvm/af-handle a)) "af-sqrt")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn nan?
  "Check for NaN (Not a Number) values in the array.

   Supported types: f32, f64, c32, c64
   Output type: b8 (boolean)

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray of boolean values where each element indicates whether the
   corresponding element in 'a' is NaN (true) or not (false)."
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-isnan out (jvm/af-handle a)) "af-isnan")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn inf?
  "Check for infinite values in the array.

   Supported types: f32, f64, c32, c64
   Output type: b8 (boolean)

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray of boolean values where each element indicates whether the
   corresponding element in 'a' is infinite (true) or not (false)."
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-isinf out (jvm/af-handle a)) "af-isinf")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn zero?
  "Check for zero values in the array.

   Supported types: f32, f64, c32, c64, b8, s32, u32, u8, s64, u64, s16, u16
   Output type: b8 (boolean)

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray of boolean values where each element indicates whether the
   corresponding element in 'a' is zero (true) or not (false)."
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-iszero out (jvm/af-handle a)) "af-iszero")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn not
  "Compute the logical NOT of each element in the array.

   Supported types: b8
   Output type: b8 (boolean)

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray where each element is the logical NOT of the corresponding
   element in 'a'."
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-not out (jvm/af-handle a)) "af-not")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn bitnot
  "Compute the bitwise NOT of each element in the array.

   Supported types: b8, s32, u32, u8, s64, u64, s16, u16

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray where each element is the bitwise NOT of the corresponding
   element in 'a'."
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-bitnot out (jvm/af-handle a)) "af-bitnot")
    (jvm/af-array-new (jvm/deref-af-array out))))

;;;
;;; Complex and power functions
;;;

(defn arg
  "Compute the argument (phase angle) of each complex element in the array.

   Supported types: c32, c64
   Output types: f32 (from c32), f64 (from c64)

   Parameters:
   - a: Input complex array (AFArray)

   Returns:
   An AFArray where each element is the argument (phase angle) of the
   corresponding complex element in 'a'."
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-arg out (jvm/af-handle a)) "af-arg")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn pow2
  "Compute 2 raised to the power of each element in the array.

   Supported types: f32, f64

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray where each element is computed as:
   pow2(x) = 2^x"
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-pow2 out (jvm/af-handle a)) "af-pow2")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn factorial
  "Compute the factorial of each element in the array.

   Supported types: f32, f64

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray where each element is the factorial of the corresponding
   element in 'a'."
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-factorial out (jvm/af-handle a)) "af-factorial")
    (jvm/af-array-new (jvm/deref-af-array out))))

;;;
;;; Binary Arithmetic Operations
;;;

;;
;; Arithmetic operations
;;
(defn add
  "Add two arrays element-wise.

   Supported types: f32, f64, c32, c64, s32, u32, u8, s64, u64, s16, u16

   Parameters:
   - lhs: Left-hand side array (AFArray)
   - rhs: Right-hand side array (AFArray)
   - batch: Boolean indicating batch mode (optional, defaults to false)

   Returns:
   An AFArray containing the element-wise sum of lhs and rhs.

   Example:
   (let [a (af/array [1 2 3])
         b (af/array [4 5 6])
         result (add a b)]
     result) ; => [5 7 9]"
  ([^AFArray lhs ^AFArray rhs]
   (add lhs rhs false))
  ([^AFArray lhs ^AFArray rhs batch]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (binary/af-add out (jvm/af-handle lhs) (jvm/af-handle rhs) (if batch 1 0)) "af-add")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn sub
  "Subtract two arrays element-wise.

   Supported types: f32, f64, c32, c64, s32, u32, u8, s64, u64, s16, u16

   Parameters:
   - lhs: Left-hand side array (AFArray)
   - rhs: Right-hand side array (AFArray)
   - batch: Boolean indicating batch mode (optional, defaults to false)

   Returns:
   An AFArray containing the element-wise difference (lhs - rhs).

   Example:
   (let [a (af/array [5 7 9])
         b (af/array [1 2 3])
         result (sub a b)]
     result) ; => [4 5 6]"
  ([^AFArray lhs ^AFArray rhs]
   (sub lhs rhs false))
  ([^AFArray lhs ^AFArray rhs batch]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (binary/af-sub out (jvm/af-handle lhs) (jvm/af-handle rhs) (if batch 1 0)) "af-sub")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn mul
  "Multiply two arrays element-wise.

   Supported types: f32, f64, c32, c64, s32, u32, u8, s64, u64, s16, u16

   Parameters:
   - lhs: Left-hand side array (AFArray)
   - rhs: Right-hand side array (AFArray)
   - batch: Boolean indicating batch mode (optional, defaults to false)

   Returns:
   An AFArray containing the element-wise product of lhs and rhs.

   Example:
   (let [a (af/array [2 3 4])
         b (af/array [5 6 7])
         result (mul a b)]
     result) ; => [10 18 28]"
  ([^AFArray lhs ^AFArray rhs]
   (mul lhs rhs false))
  ([^AFArray lhs ^AFArray rhs batch]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (binary/af-mul out (jvm/af-handle lhs) (jvm/af-handle rhs) (if batch 1 0)) "af-mul")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn div
  "Divide two arrays element-wise.

   Supported types: f32, f64, c32, c64, s32, u32, u8, s64, u64, s16, u16

   Parameters:
   - lhs: Left-hand side array (AFArray)
   - rhs: Right-hand side array (AFArray)
   - batch: Boolean indicating batch mode (optional, defaults to false)

   Returns:
   An AFArray containing the element-wise quotient (lhs / rhs).

   Example:
   (let [a (af/array [10 20 30])
         b (af/array [2 4 5])
         result (div a b)]
     result) ; => [5 5 6]"
  ([^AFArray lhs ^AFArray rhs]
   (div lhs rhs false))
  ([^AFArray lhs ^AFArray rhs batch]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (binary/af-div out (jvm/af-handle lhs) (jvm/af-handle rhs) (if batch 1 0)) "af-div")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn mod
  "Compute the modulo operation on two arrays element-wise.

   Supported types: f32, f64, s32, u32, u8, s64, u64, s16, u16

   Parameters:
   - lhs: Left-hand side array (AFArray)
   - rhs: Right-hand side array (AFArray)
   - batch: Boolean indicating batch mode (optional, defaults to false)

   Returns:
   An AFArray containing the element-wise modulo (lhs mod rhs).

   Example:
   (let [a (af/array [10 11 12])
         b (af/array [3 3 3])
         result (mod a b)]
     result) ; => [1 2 0]"
  ([^AFArray lhs ^AFArray rhs]
   (mod lhs rhs false))
  ([^AFArray lhs ^AFArray rhs batch]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (binary/af-mod out (jvm/af-handle lhs) (jvm/af-handle rhs) (if batch 1 0)) "af-mod")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn rem
  "Compute the remainder operation on two arrays element-wise.

   Supported types: f32, f64, s32, u32, u8, s64, u64, s16, u16

   Parameters:
   - lhs: Left-hand side array (AFArray)
   - rhs: Right-hand side array (AFArray)
   - batch: Boolean indicating batch mode (optional, defaults to false)

   Returns:
   An AFArray containing the element-wise remainder (lhs rem rhs).

   Note: The difference between rem and mod is in how they handle negative numbers.
   rem uses truncated division while mod uses floored division."
  ([^AFArray lhs ^AFArray rhs]
   (rem lhs rhs false))
  ([^AFArray lhs ^AFArray rhs batch]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (binary/af-rem out (jvm/af-handle lhs) (jvm/af-handle rhs) (if batch 1 0)) "af-rem")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn pow
  "Raise elements of lhs to the power of elements in rhs.

   Supported types: f32, f64, c32, c64

   Parameters:
   - lhs: Base array (AFArray)
   - rhs: Exponent array (AFArray)
   - batch: Boolean indicating batch mode (optional, defaults to false)

   Returns:
   An AFArray where each element is lhs[i]^rhs[i].

   Example:
   (let [a (af/array [2 3 4])
         b (af/array [2 3 2])
         result (pow a b)]
     result) ; => [4 27 16]"
  ([^AFArray lhs ^AFArray rhs]
   (pow lhs rhs false))
  ([^AFArray lhs ^AFArray rhs batch]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (binary/af-pow out (jvm/af-handle lhs) (jvm/af-handle rhs) (if batch 1 0)) "af-pow")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn root
  "Calculate the nth root element-wise.

   Supported types: f32, f64, c32, c64

   Parameters:
   - lhs: Array containing the root order (AFArray)
   - rhs: Array containing the values (AFArray)
   - batch: Boolean indicating batch mode (optional, defaults to false)

   Returns:
   An AFArray where each element is the lhs[i]-th root of rhs[i].

   Example:
   (let [n (af/array [2 3 2])
         v (af/array [4 27 16])
         result (root n v)]
     result) ; => [2 3 4]"
  ([^AFArray lhs ^AFArray rhs]
   (root lhs rhs false))
  ([^AFArray lhs ^AFArray rhs batch]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (binary/af-root out (jvm/af-handle lhs) (jvm/af-handle rhs) (if batch 1 0)) "af-root")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn minof
  "Compute the element-wise minimum of two arrays.

   Supported types: f32, f64, s32, u32, u8, s64, u64, s16, u16

   Parameters:
   - lhs: Left-hand side array (AFArray)
   - rhs: Right-hand side array (AFArray)
   - batch: Boolean indicating batch mode (optional, defaults to false)

   Returns:
   An AFArray containing the element-wise minimum values.

   Example:
   (let [a (af/array [1 5 3])
         b (af/array [2 4 6])
         result (minof a b)]
     result) ; => [1 4 3]"
  ([^AFArray lhs ^AFArray rhs]
   (minof lhs rhs false))
  ([^AFArray lhs ^AFArray rhs batch]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (binary/af-minof out (jvm/af-handle lhs) (jvm/af-handle rhs) (if batch 1 0)) "af-minof")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn maxof
  "Compute the element-wise maximum of two arrays.

   Supported types: f32, f64, s32, u32, u8, s64, u64, s16, u16

   Parameters:
   - lhs: Left-hand side array (AFArray)
   - rhs: Right-hand side array (AFArray)
   - batch: Boolean indicating batch mode (optional, defaults to false)

   Returns:
   An AFArray containing the element-wise maximum values.

   Example:
   (let [a (af/array [1 5 3])
         b (af/array [2 4 6])
         result (maxof a b)]
     result) ; => [2 5 6]"
  ([^AFArray lhs ^AFArray rhs]
   (maxof lhs rhs false))
  ([^AFArray lhs ^AFArray rhs batch]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (binary/af-maxof out (jvm/af-handle lhs) (jvm/af-handle rhs) (if batch 1 0)) "af-maxof")
     (jvm/af-array-new (jvm/deref-af-array out)))))

;;
;; Comparison operations
;;
(defn eq
  "Element-wise equality comparison.

   Supported types: f32, f64, c32, c64, b8, s32, u32, u8, s64, u64, s16, u16
   Output type: b8 (boolean)

   Parameters:
   - lhs: Left-hand side array (AFArray)
   - rhs: Right-hand side array (AFArray)
   - batch: Boolean indicating batch mode (optional, defaults to false)

   Returns:
   An AFArray of boolean values where each element is true if lhs[i] == rhs[i].

   Example:
   (let [a (af/array [1 2 3])
         b (af/array [1 5 3])
         result (eq a b)]
     result) ; => [true false true]"
  ([^AFArray lhs ^AFArray rhs]
   (eq lhs rhs false))
  ([^AFArray lhs ^AFArray rhs batch]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (binary/af-eq out (jvm/af-handle lhs) (jvm/af-handle rhs) (if batch 1 0)) "af-eq")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn neq
  "Element-wise inequality comparison.

   Supported types: f32, f64, c32, c64, b8, s32, u32, u8, s64, u64, s16, u16
   Output type: b8 (boolean)

   Parameters:
   - lhs: Left-hand side array (AFArray)
   - rhs: Right-hand side array (AFArray)
   - batch: Boolean indicating batch mode (optional, defaults to false)

   Returns:
   An AFArray of boolean values where each element is true if lhs[i] != rhs[i].

   Example:
   (let [a (af/array [1 2 3])
         b (af/array [1 5 3])
         result (neq a b)]
     result) ; => [false true false]"
  ([^AFArray lhs ^AFArray rhs]
   (neq lhs rhs false))
  ([^AFArray lhs ^AFArray rhs batch]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (binary/af-neq out (jvm/af-handle lhs) (jvm/af-handle rhs) (if batch 1 0)) "af-neq")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn lt
  "Element-wise less than comparison.

   Supported types: f32, f64, s32, u32, u8, s64, u64, s16, u16
   Output type: b8 (boolean)

   Parameters:
   - lhs: Left-hand side array (AFArray)
   - rhs: Right-hand side array (AFArray)
   - batch: Boolean indicating batch mode (optional, defaults to false)

   Returns:
   An AFArray of boolean values where each element is true if lhs[i] < rhs[i].

   Example:
   (let [a (af/array [1 5 3])
         b (af/array [2 4 3])
         result (lt a b)]
     result) ; => [true false false]"
  ([^AFArray lhs ^AFArray rhs]
   (lt lhs rhs false))
  ([^AFArray lhs ^AFArray rhs batch]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (binary/af-lt out (jvm/af-handle lhs) (jvm/af-handle rhs) (if batch 1 0)) "af-lt")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn le
  "Element-wise less than or equal comparison.

   Supported types: f32, f64, s32, u32, u8, s64, u64, s16, u16
   Output type: b8 (boolean)

   Parameters:
   - lhs: Left-hand side array (AFArray)
   - rhs: Right-hand side array (AFArray)
   - batch: Boolean indicating batch mode (optional, defaults to false)

   Returns:
   An AFArray of boolean values where each element is true if lhs[i] <= rhs[i].

   Example:
   (let [a (af/array [1 5 3])
         b (af/array [2 4 3])
         result (le a b)]
     result) ; => [true false true]"
  ([^AFArray lhs ^AFArray rhs]
   (le lhs rhs false))
  ([^AFArray lhs ^AFArray rhs batch]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (binary/af-le out (jvm/af-handle lhs) (jvm/af-handle rhs) (if batch 1 0)) "af-le")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn gt
  "Element-wise greater than comparison.

   Supported types: f32, f64, s32, u32, u8, s64, u64, s16, u16
   Output type: b8 (boolean)

   Parameters:
   - lhs: Left-hand side array (AFArray)
   - rhs: Right-hand side array (AFArray)
   - batch: Boolean indicating batch mode (optional, defaults to false)

   Returns:
   An AFArray of boolean values where each element is true if lhs[i] > rhs[i].

   Example:
   (let [a (af/array [3 5 3])
         b (af/array [2 6 3])
         result (gt a b)]
     result) ; => [true false false]"
  ([^AFArray lhs ^AFArray rhs]
   (gt lhs rhs false))
  ([^AFArray lhs ^AFArray rhs batch]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (binary/af-gt out (jvm/af-handle lhs) (jvm/af-handle rhs) (if batch 1 0)) "af-gt")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn ge
  "Element-wise greater than or equal comparison.

   Supported types: f32, f64, s32, u32, u8, s64, u64, s16, u16
   Output type: b8 (boolean)

   Parameters:
   - lhs: Left-hand side array (AFArray)
   - rhs: Right-hand side array (AFArray)
   - batch: Boolean indicating batch mode (optional, defaults to false)

   Returns:
   An AFArray of boolean values where each element is true if lhs[i] >= rhs[i].

   Example:
   (let [a (af/array [3 5 3])
         b (af/array [2 6 3])
         result (ge a b)]
     result) ; => [true false true]"
  ([^AFArray lhs ^AFArray rhs]
   (ge lhs rhs false))
  ([^AFArray lhs ^AFArray rhs batch]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (binary/af-ge out (jvm/af-handle lhs) (jvm/af-handle rhs) (if batch 1 0)) "af-ge")
     (jvm/af-array-new (jvm/deref-af-array out)))))

;;
;; Logical operations
;;
(defn and
  "Element-wise logical AND.

   Supported types: b8
   Output type: b8 (boolean)

   Parameters:
   - lhs: Left-hand side array (AFArray)
   - rhs: Right-hand side array (AFArray)
   - batch: Boolean indicating batch mode (optional, defaults to false)

   Returns:
   An AFArray of boolean values where each element is (lhs[i] && rhs[i]).

   Example:
   (let [a (af/array [true true false false])
         b (af/array [true false true false])
         result (and a b)]
     result) ; => [true false false false]"
  ([^AFArray lhs ^AFArray rhs]
   (and lhs rhs false))
  ([^AFArray lhs ^AFArray rhs batch]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (binary/af-and out (jvm/af-handle lhs) (jvm/af-handle rhs) (if batch 1 0)) "af-and")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn or
  "Element-wise logical OR.

   Supported types: b8
   Output type: b8 (boolean)

   Parameters:
   - lhs: Left-hand side array (AFArray)
   - rhs: Right-hand side array (AFArray)
   - batch: Boolean indicating batch mode (optional, defaults to false)

   Returns:
   An AFArray of boolean values where each element is (lhs[i] || rhs[i]).

   Example:
   (let [a (af/array [true true false false])
         b (af/array [true false true false])
         result (or a b)]
     result) ; => [true true true false]"
  ([^AFArray lhs ^AFArray rhs]
   (or lhs rhs false))
  ([^AFArray lhs ^AFArray rhs batch]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (binary/af-or out (jvm/af-handle lhs) (jvm/af-handle rhs) (if batch 1 0)) "af-or")
     (jvm/af-array-new (jvm/deref-af-array out)))))

;;
;; Bitwise operations
;;
(defn bitand
  "Element-wise bitwise AND.

   Supported types: b8, s32, u32, u8, s64, u64, s16, u16

   Parameters:
   - lhs: Left-hand side array (AFArray)
   - rhs: Right-hand side array (AFArray)
   - batch: Boolean indicating batch mode (optional, defaults to false)

   Returns:
   An AFArray where each element is the bitwise AND of lhs[i] and rhs[i].

   Example:
   (let [a (af/array [0b1100 0b1010])
         b (af/array [0b1010 0b1100])
         result (bitand a b)]
     result) ; => [0b1000 0b1000] or [8 8]"
  ([^AFArray lhs ^AFArray rhs]
   (bitand lhs rhs false))
  ([^AFArray lhs ^AFArray rhs batch]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (binary/af-bitand out (jvm/af-handle lhs) (jvm/af-handle rhs) (if batch 1 0)) "af-bitand")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn bitor
  "Element-wise bitwise OR.

   Supported types: b8, s32, u32, u8, s64, u64, s16, u16

   Parameters:
   - lhs: Left-hand side array (AFArray)
   - rhs: Right-hand side array (AFArray)
   - batch: Boolean indicating batch mode (optional, defaults to false)

   Returns:
   An AFArray where each element is the bitwise OR of lhs[i] and rhs[i].

   Example:
   (let [a (af/array [0b1100 0b1010])
         b (af/array [0b1010 0b1100])
         result (bitor a b)]
     result) ; => [0b1110 0b1110] or [14 14]"
  ([^AFArray lhs ^AFArray rhs]
   (bitor lhs rhs false))
  ([^AFArray lhs ^AFArray rhs batch]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (binary/af-bitor out (jvm/af-handle lhs) (jvm/af-handle rhs) (if batch 1 0)) "af-bitor")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn bitxor
  "Element-wise bitwise XOR.

   Supported types: b8, s32, u32, u8, s64, u64, s16, u16

   Parameters:
   - lhs: Left-hand side array (AFArray)
   - rhs: Right-hand side array (AFArray)
   - batch: Boolean indicating batch mode (optional, defaults to false)

   Returns:
   An AFArray where each element is the bitwise XOR of lhs[i] and rhs[i].

   Example:
   (let [a (af/array [0b1100 0b1010])
         b (af/array [0b1010 0b1100])
         result (bitxor a b)]
     result) ; => [0b0110 0b0110] or [6 6]"
  ([^AFArray lhs ^AFArray rhs]
   (bitxor lhs rhs false))
  ([^AFArray lhs ^AFArray rhs batch]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (binary/af-bitxor out (jvm/af-handle lhs) (jvm/af-handle rhs) (if batch 1 0)) "af-bitxor")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn bitshiftl
  "Element-wise bitwise left shift.

   Supported types: s32, u32, u8, s64, u64, s16, u16

   Parameters:
   - lhs: Array containing values to shift (AFArray)
   - rhs: Array containing shift amounts (AFArray)
   - batch: Boolean indicating batch mode (optional, defaults to false)

   Returns:
   An AFArray where each element is lhs[i] << rhs[i].

   Example:
   (let [a (af/array [4 8 16])
         b (af/array [1 2 1])
         result (bitshiftl a b)]
     result) ; => [8 32 32]"
  ([^AFArray lhs ^AFArray rhs]
   (bitshiftl lhs rhs false))
  ([^AFArray lhs ^AFArray rhs batch]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (binary/af-bitshiftl out (jvm/af-handle lhs) (jvm/af-handle rhs) (if batch 1 0)) "af-bitshiftl")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn bitshiftr
  "Element-wise bitwise right shift.

   Supported types: s32, u32, u8, s64, u64, s16, u16

   Parameters:
   - lhs: Array containing values to shift (AFArray)
   - rhs: Array containing shift amounts (AFArray)
   - batch: Boolean indicating batch mode (optional, defaults to false)

   Returns:
   An AFArray where each element is lhs[i] >> rhs[i].

   Example:
   (let [a (af/array [8 32 32])
         b (af/array [1 2 1])
         result (bitshiftr a b)]
     result) ; => [4 8 16]"
  ([^AFArray lhs ^AFArray rhs]
   (bitshiftr lhs rhs false))
  ([^AFArray lhs ^AFArray rhs batch]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (binary/af-bitshiftr out (jvm/af-handle lhs) (jvm/af-handle rhs) (if batch 1 0)) "af-bitshiftr")
     (jvm/af-array-new (jvm/deref-af-array out)))))

;;
;; Complex and math operations
;;
(defn cplx2
  "Create a complex array from real and imaginary parts.

   Supported types: f32, f64
   Output types: c32 (from f32 inputs), c64 (from f64 inputs)

   Parameters:
   - real: Array containing real parts (AFArray)
   - imag: Array containing imaginary parts (AFArray)
   - batch: Boolean indicating batch mode (optional, defaults to false)

   Returns:
   An AFArray of complex numbers where result[i] = real[i] + imag[i]*j.

   Example:
   (let [r (af/array [1.0 2.0 3.0])
         i (af/array [4.0 5.0 6.0])
         result (cplx2 r i)]
     result) ; => [1+4j, 2+5j, 3+6j]"
  ([^AFArray real ^AFArray imag]
   (cplx2 real imag false))
  ([^AFArray real ^AFArray imag batch]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (binary/af-cplx2 out (jvm/af-handle real) (jvm/af-handle imag) (if batch 1 0)) "af-cplx2")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn atan2
  "Element-wise two-argument arctangent (atan2).

   Supported types: f32, f64

   Parameters:
   - y: Array containing y values (AFArray)
   - x: Array containing x values (AFArray)
   - batch: Boolean indicating batch mode (optional, defaults to false)

   Returns:
   An AFArray where each element is the angle θ in radians such that
   x[i] = r*cos(θ) and y[i] = r*sin(θ).

   The result is in the range [-π, π].

   Example:
   (let [y (af/array [1.0 1.0 -1.0])
         x (af/array [1.0 -1.0 1.0])
         result (atan2 y x)]
     result) ; => [π/4, 3π/4, -π/4]"
  ([^AFArray y ^AFArray x]
   (atan2 y x false))
  ([^AFArray y ^AFArray x batch]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (binary/af-atan2 out (jvm/af-handle y) (jvm/af-handle x) (if batch 1 0)) "af-atan2")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn hypot
  "Element-wise hypotenuse calculation.

   Supported types: f32, f64

   Parameters:
   - lhs: Left-hand side array (AFArray)
   - rhs: Right-hand side array (AFArray)
   - batch: Boolean indicating batch mode (optional, defaults to false)

   Returns:
   An AFArray where each element is sqrt(lhs[i]² + rhs[i]²).

   This function avoids overflow/underflow for large/small values.

   Example:
   (let [a (af/array [3.0 5.0 8.0])
         b (af/array [4.0 12.0 15.0])
         result (hypot a b)]
     result) ; => [5.0 13.0 17.0]"
  ([^AFArray lhs ^AFArray rhs]
   (hypot lhs rhs false))
  ([^AFArray lhs ^AFArray rhs batch]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (binary/af-hypot out (jvm/af-handle lhs) (jvm/af-handle rhs) (if batch 1 0)) "af-hypot")
     (jvm/af-array-new (jvm/deref-af-array out)))))
