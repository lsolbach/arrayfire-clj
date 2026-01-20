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

   Parameters:
   - a: Input array (AFArray)

   Returns:
   An AFArray where each element is the factorial of the corresponding
   element in 'a'."
  [^AFArray a]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (unary/af-factorial out (jvm/af-handle a)) "af-factorial")
    (jvm/af-array-new (jvm/deref-af-array out))))
