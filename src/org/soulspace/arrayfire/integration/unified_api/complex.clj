(ns org.soulspace.arrayfire.integration.unified-api.complex
  "Integration of the ArrayFire complex related FFI bindings with the error
   handling and resource management on the JVM."
  (:refer-clojure :exclude [abs])
  (:require [org.soulspace.arrayfire.ffi.c-api.complex :as complex-ffi]
            [org.soulspace.arrayfire.integration.base.error :refer [check!]]
            [org.soulspace.arrayfire.integration.base.resource :as res]
            [org.soulspace.arrayfire.integration.unified-api.arith :as arith])
  (:import (org.soulspace.arrayfire.integration.base.resource AFArray)))

;;;
;;; Complex Number Creation
;;;

(defn cplx
  "Create a complex array from a single real array.
   
   Takes a real array and creates a complex array with the input as the real
   component and zeros as the imaginary component.
   
   Parameters:
   - a: AFArray containing real values (must be f32 or f64, not already complex)
   
   Returns:
   AFArray containing complex values with input as real part and zeros as imaginary part.
   
   Output type:
   - f32 input -> c32 output
   - f64 input -> c64 output
   
   Note: If input is already complex (c32/c64), an error will be thrown.
   
   Example:
   ```clojure
   (def real-arr (array/create-array [1.0 2.0 3.0]))
   (def complex-arr (cplx real-arr))  ; => [1+0i, 2+0i, 3+0i]
   ```"
  [^AFArray a]
  (let [out (res/native-af-array-pointer)]
    (check! (complex-ffi/af-cplx out (res/af-handle a)) "af-cplx")
    (res/af-array-new (res/deref-af-array out))))

(defn cplx2
  "Create a complex array from two real arrays (real and imaginary parts).
   
   Combines separate real and imaginary component arrays into a single complex
   array. Supports batch mode for broadcasting.
   
   Parameters:
   - real: AFArray containing real component values (must be f32 or f64)
   - imag: AFArray containing imaginary component values (must be f32 or f64)
   - batch: boolean for batch mode (default false) - enables broadcasting
   
   Returns:
   AFArray containing complex values constructed from real and imaginary parts.
   
   Output type:
   - f32 inputs -> c32 output
   - f64 inputs -> c64 output
   
   Note: Both inputs must be real (not complex) arrays.
   
   Example:
   ```clojure
   (def real-part (array/create-array [1.0 2.0 3.0]))
   (def imag-part (array/create-array [4.0 5.0 6.0]))
   (def complex-arr (cplx2 real-part imag-part))  ; => [1+4i, 2+5i, 3+6i]
   ```"
  ([^AFArray real ^AFArray imag]
   (cplx2 real imag false))
  ([^AFArray real ^AFArray imag batch]
   (let [out (res/native-af-array-pointer)
         batch-flag (if batch 1 0)]
     (check! (complex-ffi/af-cplx2 out (res/af-handle real) (res/af-handle imag) batch-flag) "af-cplx2")
     (res/af-array-new (res/deref-af-array out)))))

;;;
;;; Complex Component Extraction
;;;

(defn real
  "Extract the real part of a complex array.
   
   Returns the real component of a complex array as a real array.
   For non-complex inputs, returns a retained reference to the input.
   
   Parameters:
   - a: AFArray (typically c32 or c64)
   
   Returns:
   AFArray containing the real component values.
   
   Output type:
   - c32 input -> f32 output
   - c64 input -> f64 output
   - Real input -> retained reference (no copy)
   
   Example:
   ```clojure
   (def complex-arr (cplx2 (array/create-array [1.0 2.0]) 
                            (array/create-array [3.0 4.0])))
   (def real-part (real complex-arr))  ; => [1.0, 2.0]
   ```"
  [^AFArray a]
  (let [out (res/native-af-array-pointer)]
    (check! (complex-ffi/af-real out (res/af-handle a)) "af-real")
    (res/af-array-new (res/deref-af-array out))))

(defn imag
  "Extract the imaginary part of a complex array.
   
   Returns the imaginary component of a complex array as a real array.
   For non-complex inputs, returns an array of zeros with the same dimensions.
   
   Parameters:
   - a: AFArray (typically c32 or c64)
   
   Returns:
   AFArray containing the imaginary component values.
   
   Output type:
   - c32 input -> f32 output
   - c64 input -> f64 output
   - Real input -> zeros array with same dimensions and type
   
   Example:
   ```clojure
   (def complex-arr (cplx2 (array/create-array [1.0 2.0]) 
                            (array/create-array [3.0 4.0])))
   (def imag-part (imag complex-arr))  ; => [3.0, 4.0]
   ```"
  [^AFArray a]
  (let [out (res/native-af-array-pointer)]
    (check! (complex-ffi/af-imag out (res/af-handle a)) "af-imag")
    (res/af-array-new (res/deref-af-array out))))

;;;
;;; Complex Operations
;;;

(defn conjg
  "Calculate the complex conjugate of an array.
   
   Returns the complex conjugate by negating the imaginary part.
   For non-complex inputs, returns a retained reference to the input.
   
   Complex conjugate definition: conj(a + bi) = a - bi
   
   Parameters:
   - a: AFArray (typically c32 or c64)
   
   Returns:
   AFArray containing the conjugated complex values.
   
   Behavior:
   - Complex input (c32/c64) -> conjugated complex output
   - Real input -> retained reference (no copy)
   
   Use cases:
   - Computing Hermitian transpose (conjugate transpose)
   - Complex number division
   - Signal processing (frequency domain operations)
   
   Example:
   ```clojure
   (def z (cplx2 (array/create-array [1.0]) (array/create-array [2.0])))  ; 1+2i
   (def z-conj (conjg z))  ; => 1-2i
   ```"
  [^AFArray a]
  (let [out (res/native-af-array-pointer)]
    (check! (complex-ffi/af-conjg out (res/af-handle a)) "af-conjg")
    (res/af-array-new (res/deref-af-array out))))

(defn abs
  "Calculate the absolute value (magnitude) of array elements.
   
   For complex numbers, computes the magnitude: |a + bi| = sqrt(a^2 + b^2)
   For real numbers, computes the absolute value: |x|
   
   Parameters:
   - a: AFArray (any numeric type)
   
   Returns:
   AFArray containing the magnitude values (always real).
   
   Output types:
   - c32 input -> f32 output
   - c64 input -> f64 output
   - f16 input -> f16 output
   - Other real inputs -> promoted to f32 or f64
   
   Note: This is the magnitude function for complex numbers. To convert
   complex to real while preserving phase information, use arg for the
   phase angle.
   
   Example:
   ```clojure
   (def z (cplx2 (array/create-array [3.0]) (array/create-array [4.0])))  ; 3+4i
   (def magnitude (abs z))  ; => 5.0  (sqrt(3^2 + 4^2))
   ```"
  [^AFArray a]
  (let [out (res/native-af-array-pointer)]
    (check! (complex-ffi/af-abs out (res/af-handle a)) "af-abs")
    (res/af-array-new (res/deref-af-array out))))

(defn arg
  "Calculate the phase angle (argument) of complex numbers.
   
   Computes the angle in radians in the range [-π, π].
   For complex z = a + bi, returns atan2(b, a).
   For real numbers, returns 0.
   
   Parameters:
   - a: AFArray (typically complex)
   
   Returns:
   AFArray containing phase angles in radians (always real).
   
   Output:
   - Complex input -> angle in radians [-π, π]
   - Real input -> constant array of zeros
   
   Relationship to polar form:
   Any complex number z can be written as: z = |z| * e^(i*arg(z))
   where |z| is computed by abs and arg(z) by this function.
   
   Use cases:
   - Converting Cartesian to polar coordinates
   - Signal processing (extracting phase information)
   - Complex number analysis
   
   Example:
   ```clojure
   (def z (cplx2 (array/create-array [1.0]) (array/create-array [1.0])))  ; 1+1i
   (def phase (arg z))  ; => π/4  (45 degrees)
   ```"
  [^AFArray a]
  (let [out (res/native-af-array-pointer)]
    (check! (complex-ffi/af-arg out (res/af-handle a)) "af-arg")
    (res/af-array-new (res/deref-af-array out))))

;;;
;;; Complex Arithmetic Operators
;;; (Wrappers matching the C++ Unified API operators)
;;;

(defn add
  "Add two complex arrays element-wise (complex + operator wrapper).
   
   Implements the C++ operator+ from the Unified API.
   Supports complex-complex, complex-scalar, and scalar-complex operations.
   
   Parameters:
   - lhs: Left-hand side AFArray (complex or real)
   - rhs: Right-hand side AFArray (complex or real)
   - batch: Boolean for batch mode (default false) - enables broadcasting
   
   Returns:
   AFArray containing the element-wise sum.
   
   Type promotion:
   - c32 + c32 -> c32
   - c64 + c64 -> c64
   - c32 + f32 -> c32
   - c64 + f64 -> c64
   - Mixed precision -> promotes to higher precision
   
   Example:
   ```clojure
   (def z1 (cplx2 (array/create-array [1.0 2.0]) (array/create-array [3.0 4.0])))
   (def z2 (cplx2 (array/create-array [5.0 6.0]) (array/create-array [7.0 8.0])))
   (def sum (add z1 z2))  ; => [(1+3i)+(5+7i), (2+4i)+(6+8i)] = [6+10i, 8+12i]
   ```"
  ([^AFArray lhs ^AFArray rhs]
   (arith/add lhs rhs))
  ([^AFArray lhs ^AFArray rhs batch]
   (arith/add lhs rhs batch)))

(defn sub
  "Subtract two complex arrays element-wise (complex - operator wrapper).
   
   Implements the C++ operator- from the Unified API.
   Supports complex-complex, complex-scalar, and scalar-complex operations.
   
   Parameters:
   - lhs: Left-hand side AFArray (complex or real)
   - rhs: Right-hand side AFArray (complex or real)
   - batch: Boolean for batch mode (default false) - enables broadcasting
   
   Returns:
   AFArray containing the element-wise difference (lhs - rhs).
   
   Type promotion:
   - c32 - c32 -> c32
   - c64 - c64 -> c64
   - c32 - f32 -> c32
   - c64 - f64 -> c64
   - Mixed precision -> promotes to higher precision
   
   Example:
   ```clojure
   (def z1 (cplx2 (array/create-array [5.0 6.0]) (array/create-array [7.0 8.0])))
   (def z2 (cplx2 (array/create-array [1.0 2.0]) (array/create-array [3.0 4.0])))
   (def diff (sub z1 z2))  ; => [(5+7i)-(1+3i), (6+8i)-(2+4i)] = [4+4i, 4+4i]
   ```"
  ([^AFArray lhs ^AFArray rhs]
   (arith/sub lhs rhs))
  ([^AFArray lhs ^AFArray rhs batch]
   (arith/sub lhs rhs batch)))

(defn mul
  "Multiply two complex arrays element-wise (complex * operator wrapper).
   
   Implements the C++ operator* from the Unified API.
   Performs complex multiplication: (a+bi)*(c+di) = (ac-bd)+(ad+bc)i
   Supports complex-complex, complex-scalar, and scalar-complex operations.
   
   Parameters:
   - lhs: Left-hand side AFArray (complex or real)
   - rhs: Right-hand side AFArray (complex or real)
   - batch: Boolean for batch mode (default false) - enables broadcasting
   
   Returns:
   AFArray containing the element-wise product.
   
   Type promotion:
   - c32 * c32 -> c32
   - c64 * c64 -> c64
   - c32 * f32 -> c32
   - c64 * f64 -> c64
   - Mixed precision -> promotes to higher precision
   
   Example:
   ```clojure
   (def z1 (cplx2 (array/create-array [1.0]) (array/create-array [2.0])))  ; 1+2i
   (def z2 (cplx2 (array/create-array [3.0]) (array/create-array [4.0])))  ; 3+4i
   (def prod (mul z1 z2))  ; => (1+2i)*(3+4i) = (3-8)+(4+6)i = -5+10i
   ```"
  ([^AFArray lhs ^AFArray rhs]
   (arith/mul lhs rhs))
  ([^AFArray lhs ^AFArray rhs batch]
   (arith/mul lhs rhs batch)))

(defn div
  "Divide two complex arrays element-wise (complex / operator wrapper).
   
   Implements the C++ operator/ from the Unified API.
   Performs complex division: (a+bi)/(c+di) = [(a+bi)*(c-di)]/[c²+d²]
   Supports complex-complex, complex-scalar, and scalar-complex operations.
   
   Parameters:
   - lhs: Left-hand side AFArray (complex or real)
   - rhs: Right-hand side AFArray (complex or real)
   - batch: Boolean for batch mode (default false) - enables broadcasting
   
   Returns:
   AFArray containing the element-wise quotient (lhs / rhs).
   
   Type promotion:
   - c32 / c32 -> c32
   - c64 / c64 -> c64
   - c32 / f32 -> c32
   - c64 / f64 -> c64
   - Mixed precision -> promotes to higher precision
   
   Example:
   ```clojure
   (def z1 (cplx2 (array/create-array [1.0]) (array/create-array [2.0])))  ; 1+2i
   (def z2 (cplx2 (array/create-array [3.0]) (array/create-array [4.0])))  ; 3+4i
   (def quot (div z1 z2))  ; => (1+2i)/(3+4i) = 0.44+0.08i
   ```"
  ([^AFArray lhs ^AFArray rhs]
   (arith/div lhs rhs))
  ([^AFArray lhs ^AFArray rhs batch]
   (arith/div lhs rhs batch)))

;;;
;;; Complex Comparison Operators
;;; (Wrappers matching the C++ Unified API operators)
;;;

(defn eq
  "Element-wise equality comparison for complex arrays (== operator wrapper).
   
   Implements the C++ operator== from the Unified API.
   Two complex numbers are equal if both real and imaginary parts are equal.
   
   Parameters:
   - lhs: Left-hand side AFArray (complex or real)
   - rhs: Right-hand side AFArray (complex or real)
   - batch: Boolean for batch mode (default false) - enables broadcasting
   
   Returns:
   AFArray of boolean values (b8) where each element is true if lhs[i] == rhs[i].
   
   Comparison rules:
   - (a+bi) == (c+di) if and only if a==c AND b==d
   - Supports comparing complex with real (imaginary part assumed 0)
   
   Example:
   ```clojure
   (def z1 (cplx2 (array/create-array [1.0 2.0]) (array/create-array [3.0 4.0])))
   (def z2 (cplx2 (array/create-array [1.0 5.0]) (array/create-array [3.0 6.0])))
   (def result (eq z1 z2))  ; => [true false]
   ```"
  ([^AFArray lhs ^AFArray rhs]
   (arith/eq lhs rhs))
  ([^AFArray lhs ^AFArray rhs batch]
   (arith/eq lhs rhs batch)))

(defn neq
  "Element-wise inequality comparison for complex arrays (!= operator wrapper).
   
   Implements the C++ operator!= from the Unified API.
   Two complex numbers are not equal if either real or imaginary parts differ.
   
   Parameters:
   - lhs: Left-hand side AFArray (complex or real)
   - rhs: Right-hand side AFArray (complex or real)
   - batch: Boolean for batch mode (default false) - enables broadcasting
   
   Returns:
   AFArray of boolean values (b8) where each element is true if lhs[i] != rhs[i].
   
   Comparison rules:
   - (a+bi) != (c+di) if a!=c OR b!=d
   - Supports comparing complex with real (imaginary part assumed 0)
   
   Example:
   ```clojure
   (def z1 (cplx2 (array/create-array [1.0 2.0]) (array/create-array [3.0 4.0])))
   (def z2 (cplx2 (array/create-array [1.0 5.0]) (array/create-array [3.0 6.0])))
   (def result (neq z1 z2))  ; => [false true]
   ```"
  ([^AFArray lhs ^AFArray rhs]
   (arith/neq lhs rhs))
  ([^AFArray lhs ^AFArray rhs batch]
   (arith/neq lhs rhs batch)))