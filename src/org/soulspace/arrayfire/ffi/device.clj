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

;; af_err af_get_device_count(int *num_of_devices)
(defcfn af-get-device-count
  "Get the number of compute devices on the system.
   
   Parameters:
   - num-devices: out pointer to int

   Returns:
   ArrayFire error code"
  "af_get_device_count" [::mem/pointer] ::mem/int)

;; af_err af_get_device(int *device)
(defcfn af-get-device
  "Get the current device ID.
   
   Parameters:
   - device: out pointer to int

   Returns:
   ArrayFire error code"
  "af_get_device" [::mem/pointer] ::mem/int)

;; af_err af_set_device(const int device)
(defcfn af-set-device
  "Set the current device.
   
   Parameters:
   - device: device ID to set as active

   Returns:
   ArrayFire error code"
  "af_set_device" [::mem/int] ::mem/int)

;; af_err af_device_info(char* d_name, char* d_platform, char* d_toolkit, char* d_compute)
(defcfn af-device-info
  "Get device information strings.
   
   Parameters:
   - d-name: out pointer for device name (must be pre-allocated buffer)
   - d-platform: out pointer for platform name (must be pre-allocated buffer)
   - d-toolkit: out pointer for toolkit version (must be pre-allocated buffer)
   - d-compute: out pointer for compute version (must be pre-allocated buffer)

   Returns:
   ArrayFire error code"
  "af_device_info" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_get_dbl_support(bool* available, const int device)
(defcfn af-get-dbl-support
  "Check if double precision is supported on a device.
   
   Parameters:
   - available: out pointer to bool (int)
   - device: device ID to query

   Returns:
   ArrayFire error code"
  "af_get_dbl_support" [::mem/pointer ::mem/int] ::mem/int)

;; af_err af_get_half_support(bool* available, const int device)
(defcfn af-get-half-support
  "Check if half precision is supported on a device.
   
   Parameters:
   - available: out pointer to bool (int)
   - device: device ID to query

   Returns:
   ArrayFire error code"
  "af_get_half_support" [::mem/pointer ::mem/int] ::mem/int)

;; af_err af_info_string(char** str, const bool verbose)
(defcfn af-info-string
  "Get ArrayFire device information as a string.
   
   Parameters:
   - str: out pointer to pointer (string will be allocated by ArrayFire)
   - verbose: boolean flag for verbose output (as int: 0 or 1)

   Returns:
   ArrayFire error code"
  "af_info_string" [::mem/pointer ::mem/int] ::mem/int)
