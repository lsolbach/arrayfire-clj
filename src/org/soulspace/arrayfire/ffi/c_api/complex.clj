(ns org.soulspace.arrayfire.ffi.c-api.complex
  "Bindings for ArrayFire complex number functions.
   
   Maps to src/api/c/complex.cpp in ArrayFire."
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.loader]))

;; Complex number creation and manipulation functions

;; af_err af_cplx(af_array *out, const af_array in)
(defcfn af-cplx
  "Create a complex array from a single real array.
   
   Takes a real array and creates a complex array with the input as the real
   component and zeros as the imaginary component.
   
   Parameters:
   - out: out pointer for complex array
   - in: real array handle (must be f32 or f64, not already complex)

   Returns:
   ArrayFire error code (AF_SUCCESS on success)
   
   Note: If input is already complex (c32/c64), returns an error."
  "af_cplx" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_cplx2(af_array *out, const af_array real, const af_array imag, const bool batch)
(defcfn af-cplx2
  "Create a complex array from two real arrays.
   
   Combines separate real and imaginary component arrays into a single complex
   array. Supports batch mode for broadcasting.
   
   Parameters:
   - out: out pointer for complex array
   - real: real component array handle (must be f32 or f64)
   - imag: imaginary component array handle (must be f32 or f64)
   - batch: batch mode flag (0=false, 1=true) - enables broadcasting

   Returns:
   ArrayFire error code (AF_SUCCESS on success)
   
   Output type:
   - f32 inputs -> c32 output
   - f64 inputs -> c64 output
   
   Note: Both inputs must be real (not complex) arrays."
  "af_cplx2" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_real(af_array *out, const af_array in)
(defcfn af-real
  "Extract the real part of a complex array.
   
   Returns the real component of a complex array as a real array.
   For non-complex inputs, returns a retained reference to the input.
   
   Parameters:
   - out: out pointer for real component array
   - in: complex array handle (c32 or c64)

   Returns:
   ArrayFire error code (AF_SUCCESS on success)
   
   Behavior:
   - c32 input -> f32 output
   - c64 input -> f64 output
   - Real input -> retained reference (no copy)"
  "af_real" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_imag(af_array *out, const af_array in)
(defcfn af-imag
  "Extract the imaginary part of a complex array.
   
   Returns the imaginary component of a complex array as a real array.
   For non-complex inputs, returns an array of zeros with the same dimensions.
   
   Parameters:
   - out: out pointer for imaginary component array
   - in: complex array handle (c32 or c64)

   Returns:
   ArrayFire error code (AF_SUCCESS on success)
   
   Behavior:
   - c32 input -> f32 output
   - c64 input -> f64 output
   - Real input -> zeros array with same dimensions and type"
  "af_imag" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_conjg(af_array *out, const af_array in)
(defcfn af-conjg
  "Calculate the complex conjugate of an array.
   
   Returns the complex conjugate by negating the imaginary part.
   For non-complex inputs, returns a retained reference to the input.
   
   Complex conjugate definition: conj(a + bi) = a - bi
   
   Parameters:
   - out: out pointer for conjugate array
   - in: complex array handle (c32 or c64)

   Returns:
   ArrayFire error code (AF_SUCCESS on success)
   
   Behavior:
   - Complex input (c32/c64) -> conjugated complex output
   - Real input -> retained reference (no copy)
   
   Use cases:
   - Computing Hermitian transpose (conjugate transpose)
   - Complex number division
   - Signal processing (frequency domain operations)"
  "af_conjg" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_abs(af_array *out, const af_array in)
(defcfn af-abs
  "Calculate the absolute value (magnitude) of array elements.
   
   For complex numbers, computes the magnitude: |a + bi| = sqrt(a^2 + b^2)
   For real numbers, computes the absolute value: |x|
   
   Parameters:
   - out: out pointer for magnitude array
   - in: array handle (any numeric type)

   Returns:
   ArrayFire error code (AF_SUCCESS on success)
   
   Output types:
   - c32 input -> f32 output
   - c64 input -> f64 output
   - f16 input -> f16 output
   - Other real inputs -> promoted to f32 or f64
   
   Note: This is the magnitude function for complex numbers. To convert
   complex to real while preserving phase information, use af-arg for the
   phase angle."
  "af_abs" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_arg(af_array *out, const af_array in)
(defcfn af-arg
  "Calculate the phase angle (argument) of complex numbers.
   
   Computes the angle in radians in the range [-π, π].
   For complex z = a + bi, returns atan2(b, a).
   For real numbers, returns 0.
   
   Parameters:
   - out: out pointer for phase angle array (in radians)
   - in: array handle (typically complex)

   Returns:
   ArrayFire error code (AF_SUCCESS on success)
   
   Output:
   - Complex input -> angle in radians [-π, π]
   - Real input -> constant array of zeros
   
   Relationship to polar form:
   - Any complex number z can be written as: z = |z| * e^(i*arg(z))
   - Where |z| is computed by af-abs and arg(z) by this function
   
   Use cases:
   - Converting Cartesian to polar coordinates
   - Signal processing (extracting phase information)
   - Complex number analysis"
  "af_arg" [::mem/pointer ::mem/pointer] ::mem/int)
