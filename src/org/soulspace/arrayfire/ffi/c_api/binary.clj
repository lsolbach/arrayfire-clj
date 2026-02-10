(ns org.soulspace.arrayfire.ffi.c-api.binary
  "Bindings for the ArrayFire binary functions."
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.loader]))

;; Arithmetic operations
;; af_err af_add(af_array *out, const af_array lhs, const af_array rhs, const bool batch)
(defcfn af-add
  "Add two arrays element-wise.
   
   Parameters:
   - out: out pointer
   - lhs: array handle
   - rhs: array handle
   - batch: bool as int (batch mode)

   Returns:
   ArrayFire error code"
  "af_add" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_sub(af_array *out, const af_array lhs, const af_array rhs, const bool batch)
(defcfn af-sub
  "Subtract two arrays element-wise.
   
   Parameters:
   - out: out pointer
   - lhs: array handle
   - rhs: array handle
   - batch: bool as int (batch mode)

   Returns:
   ArrayFire error code"
  "af_sub" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_mul(af_array *out, const af_array lhs, const af_array rhs, const bool batch)
(defcfn af-mul
  "Multiply two arrays element-wise.
   
   Parameters:
   - out: out pointer
   - lhs: array handle
   - rhs: array handle
   - batch: bool as int (batch mode)

   Returns:
   ArrayFire error code"
  "af_mul" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_div(af_array *out, const af_array lhs, const af_array rhs, const bool batch)
(defcfn af-div
  "Divide two arrays element-wise.
   
   Parameters:
   - out: out pointer
   - lhs: array handle
   - rhs: array handle
   - batch: bool as int (batch mode)

   Returns:
   ArrayFire error code"
  "af_div" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_mod(af_array *out, const af_array lhs, const af_array rhs, const bool batch)
(defcfn af-mod
  "Modulo operation on two arrays element-wise.
   
   Parameters:
   - out: out pointer
   - lhs: array handle
   - rhs: array handle
   - batch: bool as int (batch mode)

   Returns:
   ArrayFire error code"
  "af_mod" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_rem(af_array *out, const af_array lhs, const af_array rhs, const bool batch)
(defcfn af-rem
  "Remainder operation on two arrays element-wise.
   
   Parameters:
   - out: out pointer
   - lhs: array handle
   - rhs: array handle
   - batch: bool as int (batch mode)

   Returns:
   ArrayFire error code"
  "af_rem" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_pow(af_array *out, const af_array lhs, const af_array rhs, const bool batch)
(defcfn af-pow
  "Raise lhs to the power of rhs element-wise.
   
   Parameters:
   - out: out pointer
   - lhs: base array handle
   - rhs: exponent array handle
   - batch: bool as int (batch mode)

   Returns:
   ArrayFire error code"
  "af_pow" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_root(af_array *out, const af_array lhs, const af_array rhs, const bool batch)
(defcfn af-root
  "Calculate the nth root of rhs with lhs as the root.
   
   Parameters:
   - out: out pointer
   - lhs: nth root
   - rhs: value array handle
   - batch: bool as int (batch mode)

   Returns:
   ArrayFire error code"
  "af_root" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_minof(af_array *out, const af_array lhs, const af_array rhs, const bool batch)
(defcfn af-minof
  "Element-wise minimum of two arrays.
   
   Parameters:
   - out: out pointer
   - lhs: array handle
   - rhs: array handle
   - batch: bool as int (batch mode)

   Returns:
   ArrayFire error code"
  "af_minof" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_maxof(af_array *out, const af_array lhs, const af_array rhs, const bool batch)
(defcfn af-maxof
  "Element-wise maximum of two arrays.
   
   Parameters:
   - out: out pointer
   - lhs: array handle
   - rhs: array handle
   - batch: bool as int (batch mode)

   Returns:
   ArrayFire error code"
  "af_maxof" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; Comparison operations
;; af_err af_eq(af_array *out, const af_array lhs, const af_array rhs, const bool batch)
(defcfn af-eq
  "Element-wise equality comparison.
   
   Parameters:
   - out: out pointer to bool array
   - lhs: array handle
   - rhs: array handle
   - batch: bool as int (batch mode)

   Returns:
   ArrayFire error code"
  "af_eq" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_neq(af_array *out, const af_array lhs, const af_array rhs, const bool batch)
(defcfn af-neq
  "Element-wise inequality comparison.
   
   Parameters:
   - out: out pointer to bool array
   - lhs: array handle
   - rhs: array handle
   - batch: bool as int (batch mode)

   Returns:
   ArrayFire error code"
  "af_neq" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_lt(af_array *out, const af_array lhs, const af_array rhs, const bool batch)
(defcfn af-lt
  "Element-wise less than comparison.
   
   Parameters:
   - out: out pointer to bool array
   - lhs: array handle
   - rhs: array handle
   - batch: bool as int (batch mode)

   Returns:
   ArrayFire error code"
  "af_lt" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_le(af_array *out, const af_array lhs, const af_array rhs, const bool batch)
(defcfn af-le
  "Element-wise less than or equal comparison.
   
   Parameters:
   - out: out pointer to bool array
   - lhs: array handle
   - rhs: array handle
   - batch: bool as int (batch mode)

   Returns:
   ArrayFire error code"
  "af_le" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_gt(af_array *out, const af_array lhs, const af_array rhs, const bool batch)
(defcfn af-gt
  "Element-wise greater than comparison.
   
   Parameters:
   - out: out pointer to bool array
   - lhs: array handle
   - rhs: array handle
   - batch: bool as int (batch mode)

   Returns:
   ArrayFire error code"
  "af_gt" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_ge(af_array *out, const af_array lhs, const af_array rhs, const bool batch)
(defcfn af-ge
  "Element-wise greater than or equal comparison.
   
   Parameters:
   - out: out pointer to bool array
   - lhs: array handle
   - rhs: array handle
   - batch: bool as int (batch mode)

   Returns:
   ArrayFire error code"
  "af_ge" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; Logical operations
;; af_err af_and(af_array *out, const af_array lhs, const af_array rhs, const bool batch)
(defcfn af-and
  "Element-wise logical AND.
   
   Parameters:
   - out: out pointer to bool array
   - lhs: array handle
   - rhs: array handle
   - batch: bool as int (batch mode)

   Returns:
   ArrayFire error code"
  "af_and" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_or(af_array *out, const af_array lhs, const af_array rhs, const bool batch)
(defcfn af-or
  "Element-wise logical OR.
   
   Parameters:
   - out: out pointer to bool array
   - lhs: array handle
   - rhs: array handle
   - batch: bool as int (batch mode)

   Returns:
   ArrayFire error code"
  "af_or" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; Bitwise operations
;; af_err af_bitand(af_array *out, const af_array lhs, const af_array rhs, const bool batch)
(defcfn af-bitand
  "Element-wise bitwise AND.
   
   Parameters:
   - out: out pointer
   - lhs: array handle
   - rhs: array handle
   - batch: bool as int (batch mode)

   Returns:
   ArrayFire error code"
  "af_bitand" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_bitor(af_array *out, const af_array lhs, const af_array rhs, const bool batch)
(defcfn af-bitor
  "Element-wise bitwise OR.
   
   Parameters:
   - out: out pointer
   - lhs: array handle
   - rhs: array handle
   - batch: bool as int (batch mode)

   Returns:
   ArrayFire error code"
  "af_bitor" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_bitxor(af_array *out, const af_array lhs, const af_array rhs, const bool batch)
(defcfn af-bitxor
  "Element-wise bitwise XOR.
   
   Parameters:
   - out: out pointer
   - lhs: array handle
   - rhs: array handle
   - batch: bool as int (batch mode)

   Returns:
   ArrayFire error code"
  "af_bitxor" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_bitshiftl(af_array *out, const af_array lhs, const af_array rhs, const bool batch)
(defcfn af-bitshiftl
  "Element-wise bitwise left shift.
   
   Parameters:
   - out: out pointer
   - lhs: array handle (value to shift)
   - rhs: array handle (shift amount)
   - batch: bool as int (batch mode)

   Returns:
   ArrayFire error code"
  "af_bitshiftl" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_bitshiftr(af_array *out, const af_array lhs, const af_array rhs, const bool batch)
(defcfn af-bitshiftr
  "Element-wise bitwise right shift.
   
   Parameters:
   - out: out pointer
   - lhs: array handle (value to shift)
   - rhs: array handle (shift amount)
   - batch: bool as int (batch mode)

   Returns:
   ArrayFire error code"
  "af_bitshiftr" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; Complex and math operations
;; af_err af_cplx2(af_array *out, const af_array real, const af_array imag, const bool batch)
(defcfn af-cplx2
  "Create a complex array from real and imaginary parts.
   
   Parameters:
   - out: out pointer to complex array
   - real: array handle for real part
   - imag: array handle for imaginary part
   - batch: bool as int (batch mode)

   Returns:
   ArrayFire error code"
  "af_cplx2" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_atan2(af_array *out, const af_array lhs, const af_array rhs, const bool batch)
(defcfn af-atan2
  "Element-wise arctangent of y/x (two-argument arctangent).
   
   Parameters:
   - out: out pointer
   - lhs: array handle (y values)
   - rhs: array handle (x values)
   - batch: bool as int (batch mode)

   Returns:
   ArrayFire error code"
  "af_atan2" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_hypot(af_array *out, const af_array lhs, const af_array rhs, const bool batch)
(defcfn af-hypot
  "Element-wise hypotenuse: sqrt(lhs^2 + rhs^2).
   
   Parameters:
   - out: out pointer
   - lhs: array handle
   - rhs: array handle
   - batch: bool as int (batch mode)

   Returns:
   ArrayFire error code"
  "af_hypot" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

