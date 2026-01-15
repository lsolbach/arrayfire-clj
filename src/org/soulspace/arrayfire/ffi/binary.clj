(ns org.soulspace.arrayfire.ffi.binary
  "Bindings for the ArrayFire binary functions."
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; Arithmetic operations
;; af_err af_add(af_array *out, const af_array lhs, const af_array rhs, const bool batch)
(defcfn af-add
  "Add two arrays element-wise.
   
   Parameters:
   - out: out pointer
   - lhs: array handle
   - rhs: array handle
   - batch: bool as int

   Returns:
   ArrayFire error code"
  "af_add" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

