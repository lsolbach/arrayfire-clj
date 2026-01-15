(ns org.soulspace.arrayfire.ffi.memory
  "Bindings for the ArrayFire memory functions."
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; Memory management functions
;; af_err af_alloc_pinned(void **ptr, const dim_t bytes)
(defcfn af-alloc-pinned
  "Allocate pinned (page-locked) memory for efficient device-host transfer.
   
   Parameters:
   - ptr: out pointer to pointer
   - bytes: size in bytes
   
   Returns:
   ArrayFire error code"
  "af_alloc_pinned" [::mem/pointer ::mem/long] ::mem/int)

;; af_err af_free_pinned(void *ptr)
(defcfn af-free-pinned
  "Free pinned memory allocated by af_alloc_pinned.
   
   Parameters:
   - ptr: pointer to free
   
   Returns:
   ArrayFire error code"
  "af_free_pinned" [::mem/pointer] ::mem/int)

;; af_err af_alloc_host(void **ptr, const dim_t bytes)
(defcfn af-alloc-host
  "Allocate host-accessible memory.
   
   Parameters:
   - ptr: out pointer to pointer
   - bytes: size in bytes

   Returns:
   ArrayFire error code"
  "af_alloc_host" [::mem/pointer ::mem/long] ::mem/int)

;; af_err af_free_host(void *ptr)
(defcfn af-free-host
  "Free host memory allocated by af_alloc_host.
   
   Parameters:
   - ptr: pointer to free
   
   Returns:
   ArrayFire error code"
  "af_free_host" [::mem/pointer] ::mem/int)

;; af_err af_alloc_device(void **ptr, const dim_t bytes)
(defcfn af-alloc-device
  "Allocate device memory using ArrayFire's memory manager.
   Deprecated: Use af_alloc_device_v2 instead.
   
   Parameters:
   - ptr: out pointer to pointer
   - bytes: size in bytes to allocate

   Returns:
   ArrayFire error code"
  "af_alloc_device" [::mem/pointer ::mem/long] ::mem/int)

;; af_err af_alloc_device_v2(void **ptr, const dim_t bytes)
(defcfn af-alloc-device-v2
  "Allocate device memory using ArrayFire's memory manager (v2).
   Returns cl_mem for OpenCL backend, plain pointer for others.
   
   Parameters:
   - ptr: out pointer to pointer
   - bytes: size in bytes to allocate

   Returns:
   ArrayFire error code"
  "af_alloc_device_v2" [::mem/pointer ::mem/long] ::mem/int)

;; af_err af_free_device(void *ptr)
(defcfn af-free-device
  "Free device memory allocated by af_alloc_device.
   Deprecated: Use af_free_device_v2 instead.
   
   Parameters:
   - ptr: pointer to free

   Returns:
   ArrayFire error code"
  "af_free_device" [::mem/pointer] ::mem/int)

;; af_err af_free_device_v2(void *ptr)
(defcfn af-free-device-v2
  "Free device memory allocated by af_alloc_device_v2.
   
   Parameters:
   - ptr: pointer to free

   Returns:
   ArrayFire error code"
  "af_free_device_v2" [::mem/pointer] ::mem/int)

;; af_err af_device_mem_info(size_t *alloc_bytes, size_t *alloc_buffers, size_t *lock_bytes, size_t *lock_buffers)
(defcfn af-device-mem-info
  "Get memory information from the ArrayFire memory manager.
   
   Parameters:
   - alloc-bytes: out pointer for total allocated bytes
   - alloc-buffers: out pointer for number of allocated buffers
   - lock-bytes: out pointer for locked bytes (in use)
   - lock-buffers: out pointer for number of locked buffers

   Returns:
   ArrayFire error code"
  "af_device_mem_info" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_print_mem_info(const char *msg, const int device_id)
(defcfn af-print-mem-info
  "Print detailed memory information from the ArrayFire Device Manager.
   Prints a table with pointer addresses, sizes, and lock status.
   
   Parameters:
   - msg: message to print before the table (can be NULL/0)
   - device-id: device ID to query (-1 for active device)

   Returns:
   ArrayFire error code"
  "af_print_mem_info" [::mem/pointer ::mem/int] ::mem/int)

;; af_err af_device_gc()
(defcfn af-device-gc
  "Call the garbage collection routine in the memory manager.
   Forces cleanup of unused memory.
   
   Returns:
   ArrayFire error code"
  "af_device_gc" [] ::mem/int)

;; af_err af_set_mem_step_size(const size_t step_bytes)
(defcfn af-set-mem-step-size
  "Set the minimum memory chunk size for the memory manager.
   Only works with the default memory manager.
   
   Parameters:
   - step-bytes: minimum chunk size in bytes

   Returns:
   ArrayFire error code"
  "af_set_mem_step_size" [::mem/long] ::mem/int)

;; af_err af_get_mem_step_size(size_t *step_bytes)
(defcfn af-get-mem-step-size
  "Get the minimum memory chunk size from the memory manager.
   Only works with the default memory manager.
   
   Parameters:
   - step-bytes: out pointer for step size in bytes

   Returns:
   ArrayFire error code"
  "af_get_mem_step_size" [::mem/pointer] ::mem/int)

;; af_err af_lock_array(const af_array arr)
(defcfn af-lock-array
  "Lock the device buffer in the memory manager.
   Locked buffers are not freed until af_unlock_array is called.
   
   Parameters:
   - arr: array handle

   Returns:
   ArrayFire error code"
  "af_lock_array" [::mem/pointer] ::mem/int)

;; af_err af_unlock_array(const af_array arr)
(defcfn af-unlock-array
  "Unlock device buffer in the memory manager.
   Returns control over the device pointer to the memory manager.
   
   Parameters:
   - arr: array handle

   Returns:
   ArrayFire error code"
  "af_unlock_array" [::mem/pointer] ::mem/int)

;; af_err af_is_locked_array(bool *res, const af_array arr)
(defcfn af-is-locked-array
  "Query if the array has been locked by the user.
   An array is locked when af_lock_array, af_get_device_ptr, or af_get_raw_ptr is called.
   
   Parameters:
   - res: out pointer to bool result (as int)
   - arr: array handle

   Returns:
   ArrayFire error code"
  "af_is_locked_array" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_get_device_ptr(void **ptr, const af_array arr)
(defcfn af-get-device-ptr
  "Get the device pointer and lock the buffer in memory manager.
   The pointer is not freed until af_unlock_device_ptr is called.
   Note: For OpenCL backend, cast to cl_mem.
   
   Parameters:
   - ptr: out pointer to device pointer
   - arr: array handle

   Returns:
   ArrayFire error code"
  "af_get_device_ptr" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_lock_device_ptr(const af_array arr)
(defcfn af-lock-device-ptr
  "Lock the device buffer in the memory manager (deprecated).
   Deprecated: Use af_lock_array instead.
   
   Parameters:
   - arr: array handle

   Returns:
   ArrayFire error code"
  "af_lock_device_ptr" [::mem/pointer] ::mem/int)

;; af_err af_unlock_device_ptr(const af_array arr)
(defcfn af-unlock-device-ptr
  "Unlock device buffer in the memory manager (deprecated).
   Deprecated: Use af_unlock_array instead.
   
   Parameters:
   - arr: array handle

   Returns:
   ArrayFire error code"
  "af_unlock_device_ptr" [::mem/pointer] ::mem/int)

