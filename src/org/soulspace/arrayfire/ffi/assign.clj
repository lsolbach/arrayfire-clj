(ns org.soulspace.arrayfire.ffi.assign
  "Bindings for the ArrayFire array assignment functions.
  
  Corresponds to src/api/c/assign.cpp in ArrayFire."
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; Assignment functions

;; af_err af_assign_seq(af_array *out, const af_array lhs, const unsigned ndims, const af_seq* const indices, const af_array rhs)
(defcfn af-assign-seq
  "Assign values to an array using sequences.
  
  Parameters:
  out - pointer to output array
  lhs - array whose values are used for unindexed locations
  ndims - number of dimensions to assign
  indices - pointer to array of af_seq sequences
  rhs - array whose values are used for indexed locations
  
  Returns:
  af_err error code"
  "af_assign_seq" [::mem/pointer ::mem/pointer ::mem/int ::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_assign_gen(af_array *out, const af_array lhs, const dim_t ndims, const af_index_t* indices, const af_array rhs)
(defcfn af-assign-gen
  "Generalized assignment using af_index_t (can be sequences or arrays).
  
  Parameters:
  out - pointer to output array
  lhs - array whose values are used for unindexed locations
  ndims - number of dimensions to assign
  indices - pointer to array of af_index_t objects
  rhs - array whose values are used for indexed locations
  
  Returns:
  af_err error code"
  "af_assign_gen" [::mem/pointer ::mem/pointer ::mem/long ::mem/pointer ::mem/pointer] ::mem/int)

