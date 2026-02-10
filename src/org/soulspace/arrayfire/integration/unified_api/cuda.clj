(ns org.soulspace.arrayfire.integration.unified-api.cuda
  "Integration of the ArrayFire CUDA backend related FFI bindings with the error
   handling and resource management on the JVM.
   
   These functions are specific to the CUDA backend and will return
   AF_ERR_NOT_SUPPORTED if the current backend is not CUDA."
  (:require [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.c-api.cuda :as cuda-ffi]
            [org.soulspace.arrayfire.integration.base.error :refer [check!]]
            [org.soulspace.arrayfire.integration.base.jvm-integration :as jvm]))

;;;
;;; CUDA Backend Constants
;;;

;; cuBLAS Math Mode constants
(def CUBLAS_DEFAULT_MATH 0)
(def CUBLAS_TENSOR_OP_MATH 1)

;;;
;;; CUDA Stream Management
;;;

(defn get-stream
  "Get the CUDA stream for the device with the given ArrayFire device id.
   
   This function returns the CUDA stream used internally by ArrayFire for
   the specified device. This is useful when you need to synchronize ArrayFire
   operations with other CUDA operations or libraries.
   
   Parameters:
   - id: ArrayFire device id (integer)
   
   Returns:
   Long integer representing the native CUDA stream pointer
   
   Throws:
   Exception if not using CUDA backend or if the operation fails
   
   Example:
   (let [stream (get-stream 0)]
     ;; Use stream with other CUDA libraries
     stream)"
  [id]
  (let [stream-buf (mem/alloc 8)] ; pointer size
    (check! (cuda-ffi/afcu-get-stream stream-buf (int id))
                "afcu-get-stream")
    (mem/read-long stream-buf 0)))

(defn get-native-id
  "Get the native CUDA device id for the given ArrayFire device id.
   
   ArrayFire may use a different device numbering scheme than the native
   CUDA runtime. This function returns the native CUDA device id that
   corresponds to the given ArrayFire device id.
   
   Parameters:
   - id: ArrayFire device id (integer)
   
   Returns:
   Integer representing the native CUDA device id
   
   Throws:
   Exception if not using CUDA backend or if the operation fails
   
   Example:
   (let [native-id (get-native-id 0)]
     ;; Use native-id with CUDA runtime functions
     native-id)"
  [id]
  (let [native-id-buf (mem/alloc 4)]
    (check! (cuda-ffi/afcu-get-native-id native-id-buf (int id))
                "afcu-get-native-id")
    (mem/read-int native-id-buf 0)))

(defn set-native-id!
  "Set the active ArrayFire device using a native CUDA device id.
   
   This function allows you to set the active ArrayFire device using the
   native CUDA device numbering scheme rather than ArrayFire's device ids.
   
   Parameters:
   - native-id: Native CUDA device id (integer)
   
   Returns:
   nil
   
   Throws:
   Exception if not using CUDA backend or if the operation fails
   
   Example:
   (set-native-id! 0) ; Set device 0 as active using native CUDA id"
  [native-id]
  (check! (cuda-ffi/afcu-set-native-id (int native-id))
              "afcu-set-native-id")
  nil)

;;;
;;; cuBLAS Configuration
;;;

(defn cublas-set-math-mode!
  "Set the cuBLAS math mode for ArrayFire's internal cuBLAS handle.
   
   This controls whether cuBLAS uses Tensor Cores (on supported hardware)
   for BLAS operations. Tensor Cores can provide significant speedups for
   certain operations, especially on Volta, Turing, and newer architectures.
   
   Parameters:
   - mode: cuBLAS math mode constant
     - CUBLAS_DEFAULT_MATH (0): Standard math operations
     - CUBLAS_TENSOR_OP_MATH (1): Use Tensor Cores when available
   
   Returns:
   nil
   
   Throws:
   Exception if not using CUDA backend or if the operation fails
   
   Example:
   ;; Enable Tensor Core operations
   (cublas-set-math-mode! CUBLAS_TENSOR_OP_MATH)
   
   ;; Revert to default math mode
   (cublas-set-math-mode! CUBLAS_DEFAULT_MATH)"
  [mode]
  (check! (cuda-ffi/afcu-cublas-set-math-mode (int mode))
              "afcu-cublas-set-math-mode")
  nil)
