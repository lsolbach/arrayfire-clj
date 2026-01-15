(ns org.soulspace.arrayfire.ffi.device
  "Bindings for the ArrayFire device functions."
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; Device management functions
(defcfn af-init
  "Initialize ArrayFire runtime.
   
   Returns:
   ArrayFire error code"
  "af_init" [] ::mem/int)

(defcfn af-info
  "Print ArrayFire device information.
   
   Returns:
   ArrayFire error code"
  "af_info" [] ::mem/int)

;; af_err af_eval(af_array arr)
(defcfn af-eval
  "Evaluate any pending operations on the array.
   This ensures the array is computed before data transfer.
   Required for asynchronous backends like CUDA.
   
   Parameters:
   - arr: array handle

   Returns:
   ArrayFire error code"
  "af_eval" [::mem/pointer] ::mem/int)

;; af_err af_sync(const int device)
(defcfn af-sync
  "Blocks until all operations on the specified device are complete.
   Use -1 to sync all devices.
   
   Parameters:
   - device: device ID or -1 for all

   Returns:
   ArrayFire error code"
  "af_sync" [::mem/int] ::mem/int)
