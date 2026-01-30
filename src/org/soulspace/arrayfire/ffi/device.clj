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

;; Backend management functions

;; af_err af_set_backend(const af_backend bknd)
(defcfn af-set-backend
  "Set the current backend.
   
   Parameters:
   - backend: Backend ID (0=default, 1=CPU, 2=CUDA, 3=OpenCL)
   
   Returns:
   ArrayFire error code"
  "af_set_backend" [::mem/int] ::mem/int)

;; af_err af_get_backend_count(unsigned* num_backends)
(defcfn af-get-backend-count
  "Get the number of available backends.
   
   Parameters:
   - num-backends: out pointer to unsigned int
   
   Returns:
   ArrayFire error code"
  "af_get_backend_count" [::mem/pointer] ::mem/int)

;; af_err af_get_available_backends(int* result)
(defcfn af-get-available-backends
  "Get a bitmask of available backends.
   
   Parameters:
   - result: out pointer to int (bitmask)
   
   Returns:
   ArrayFire error code"
  "af_get_available_backends" [::mem/pointer] ::mem/int)

;; af_err af_get_backend_id(af_backend* result, const af_array in)
(defcfn af-get-backend-id
  "Get the backend ID of an array.
   
   Parameters:
   - result: out pointer to backend ID
   - in: array handle
   
   Returns:
   ArrayFire error code"
  "af_get_backend_id" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_get_device_id(int* device, const af_array in)
(defcfn af-get-device-id
  "Get the device ID of an array.
   
   Parameters:
   - device: out pointer to device ID
   - in: array handle
   
   Returns:
   ArrayFire error code"
  "af_get_device_id" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_get_active_backend(af_backend* result)
(defcfn af-get-active-backend
  "Get the current active backend.
   
   Parameters:
   - result: out pointer to backend ID
   
   Returns:
   ArrayFire error code"
  "af_get_active_backend" [::mem/pointer] ::mem/int)

;; Device array functions

;; af_err af_device_array(af_array* arr, void* data, const unsigned ndims, const dim_t* const dims, const af_dtype type)
(defcfn af-device-array
  "Create an array from existing device memory.
   
   Parameters:
   - arr: out pointer to array handle
   - data: pointer to device memory
   - ndims: number of dimensions
   - dims: pointer to dimensions array
   - type: data type
   
   Returns:
   ArrayFire error code"
  "af_device_array" [::mem/pointer ::mem/pointer ::mem/int ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_get_device_ptr(void** ptr, const af_array arr)
(defcfn af-get-device-ptr
  "Get the device pointer from an array.
   
   Parameters:
   - ptr: out pointer to device pointer
   - arr: array handle
   
   Returns:
   ArrayFire error code"
  "af_get_device_ptr" [::mem/pointer ::mem/pointer] ::mem/int)

;; Array locking functions

;; af_err af_lock_array(const af_array arr)
(defcfn af-lock-array
  "Lock an array to prevent memory reuse.
   
   Parameters:
   - arr: array handle
   
   Returns:
   ArrayFire error code"
  "af_lock_array" [::mem/pointer] ::mem/int)

;; af_err af_unlock_array(const af_array arr)
(defcfn af-unlock-array
  "Unlock a previously locked array.
   
   Parameters:
   - arr: array handle
   
   Returns:
   ArrayFire error code"
  "af_unlock_array" [::mem/pointer] ::mem/int)

;; af_err af_is_locked_array(bool* res, const af_array arr)
(defcfn af-is-locked-array
  "Check if an array is locked.
   
   Parameters:
   - res: out pointer to bool (int)
   - arr: array handle
   
   Returns:
   ArrayFire error code"
  "af_is_locked_array" [::mem/pointer ::mem/pointer] ::mem/int)

;; Evaluation control functions

;; af_err af_eval_multiple(const int num, af_array* arrays)
(defcfn af-eval-multiple
  "Evaluate multiple arrays at once.
   
   Parameters:
   - num: number of arrays
   - arrays: pointer to array of array handles
   
   Returns:
   ArrayFire error code"
  "af_eval_multiple" [::mem/int ::mem/pointer] ::mem/int)

;; af_err af_set_manual_eval_flag(bool flag)
(defcfn af-set-manual-eval-flag
  "Enable or disable manual evaluation mode.
   
   Parameters:
   - flag: boolean flag (as int: 0 or 1)
   
   Returns:
   ArrayFire error code"
  "af_set_manual_eval_flag" [::mem/int] ::mem/int)

;; af_err af_get_manual_eval_flag(bool* flag)
(defcfn af-get-manual-eval-flag
  "Get the current manual evaluation mode flag.
   
   Parameters:
   - flag: out pointer to bool (int)
   
   Returns:
   ArrayFire error code"
  "af_get_manual_eval_flag" [::mem/pointer] ::mem/int)

;; Kernel cache functions

;; af_err af_set_kernel_cache_directory(const char* path, int override_eval)
(defcfn af-set-kernel-cache-directory
  "Set the directory for kernel cache files.
   
   Parameters:
   - path: pointer to path string
   - override-eval: whether to override existing cache (as int: 0 or 1)
   
   Returns:
   ArrayFire error code"
  "af_set_kernel_cache_directory" [::mem/pointer ::mem/int] ::mem/int)

;; af_err af_get_kernel_cache_directory(size_t* length, char* path)
(defcfn af-get-kernel-cache-directory
  "Get the current kernel cache directory.
   
   Parameters:
   - length: out pointer to length of path
   - path: buffer to store path (can be NULL to query length)
   
   Returns:
   ArrayFire error code"
  "af_get_kernel_cache_directory" [::mem/pointer ::mem/pointer] ::mem/int)
