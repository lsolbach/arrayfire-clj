(ns org.soulspace.arrayfire.ffi.c-api.cuda
  "Bindings for the ArrayFire CUDA backend functions."
  (:require [coffi.mem :as mem]
            [coffi.ffi :as ffi :refer [defcfn]]
            [org.soulspace.arrayfire.ffi.loader]))

;; CUDA stream and device management functions

;; af_err afcu_get_stream(cudaStream_t* stream, int id)
(defcfn afcu-get-stream
  "Get the stream for the CUDA device with id in ArrayFire context.
   
   Parameters:
   - stream: Pointer to receive the CUDA stream
   - id: ArrayFire device id
   
   Returns:
   ArrayFire error code"
  "afcu_get_stream" [::mem/pointer ::mem/int] ::mem/int)

;; af_err afcu_get_native_id(int* nativeid, int id)
(defcfn afcu-get-native-id
  "Get the native device id of the CUDA device with id in ArrayFire context.
   
   Parameters:
   - nativeid: Pointer to receive the native device id
   - id: ArrayFire device id
   
   Returns:
   ArrayFire error code"
  "afcu_get_native_id" [::mem/pointer ::mem/int] ::mem/int)

;; af_err afcu_set_native_id(int nativeid)
(defcfn afcu-set-native-id
  "Set the CUDA device with given native id as the active device for ArrayFire.
   
   Parameters:
   - nativeid: Native device id of the CUDA device
   
   Returns:
   ArrayFire error code"
  "afcu_set_native_id" [::mem/int] ::mem/int)

;; af_err afcu_cublasSetMathMode(cublasMath_t mode)
(defcfn afcu-cublas-set-math-mode
  "Set the cuBLAS math mode for the internal handle.
   
   Parameters:
   - mode: The cublasMath_t type to set (0=DEFAULT, 1=TENSOR_OP)
   
   Returns:
   ArrayFire error code"
  "afcu_cublasSetMathMode" [::mem/int] ::mem/int)
