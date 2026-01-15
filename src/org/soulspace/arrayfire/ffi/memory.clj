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

;; af_err af-free_host(void *ptr)
(defcfn af-free-host
  "Free host memory allocated by af_alloc_host.
   
   Parameters:
   - ptr: pointer to free
   
   Returns:
   ArrayFire error code"
  "af_free_host" [::mem/pointer] ::mem/int)

