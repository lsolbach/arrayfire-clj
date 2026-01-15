(ns org.soulspace.arrayfire.ffi.array
  "Bindings for the ArrayFire array functions."
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; Array management functions

;; af_err af_get_data_ptr(void *data, const af_array arr)
(defcfn af-get-data-ptr
  "Copy array data to host memory.
   
   Parameters:
   - data: out pointer to host buffer
   - arr: array handle

   Returns:
   ArrayFire error code"
  "af_get_data_ptr" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_create_array(af_array *arr, const void *data, unsigned ndims, const dim_t *dims, af_dtype type)
(defcfn af-create-array
  "Create an ArrayFire array from host data.
   
   Parameters:
   - arr: out pointer
   - data: in pointer
   - ndims: unsigned
   - dims: pointer to dim_t
   - type: int
   
   Returns:
   ArrayFire error code"
  "af_create_array" [::mem/pointer ::mem/pointer ::mem/int ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_release_array(af_array arr)
(defcfn af-release-array
  "Release an ArrayFire array handle.
   
   Parameters:
   - arr: array handle
   
   Returns:
   ArrayFire error code"
  "af_release_array" [::mem/pointer] ::mem/int)

