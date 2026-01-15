(ns org.soulspace.arrayfire.ffi.cholesky
  "Bindings for ArrayFire Cholesky decomposition functions.
   
   Maps to src/api/c/cholesky.cpp in ArrayFire."
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; af_err af_cholesky(af_array *out, int *info, const af_array in, const bool is_upper)
(defcfn af-cholesky
  "Perform Cholesky decomposition of a positive definite matrix.
   
   The Cholesky decomposition factors a positive definite matrix A into:
   - A = L * L^H (lower triangular, when is_upper = false)
   - A = U^H * U (upper triangular, when is_upper = true)
   
   where L is a lower triangular matrix, U is an upper triangular matrix,
   and ^H denotes the conjugate transpose.
   
   Multiplying `out` with its conjugate transpose reproduces the input `in`.
   
   The input matrix must be:
   - Square (dimensions[0] == dimensions[1])
   - Positive definite
   - Floating point or complex type (f32, f64, c32, c64)
   
   This function creates a new array and returns only the triangular portion
   (upper or lower) of the decomposition. Use af-cholesky-inplace for in-place
   decomposition.
   
   This function is not supported in batch mode (ndims must be <= 2).
   This function is not supported in GFOR.
   
   Parameters:
   - out: out pointer for triangular matrix result
   - info: out pointer to int status (0 if successful, otherwise returns
           the rank at which decomposition fails)
   - in: array handle for input positive definite matrix
   - is_upper: bool as int (true=1 for upper triangular U, false=0 for lower triangular L)

   Returns:
   ArrayFire error code (AF_SUCCESS on success)"
  "af_cholesky" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_cholesky_inplace(int *info, af_array in, const bool is_upper)
(defcfn af-cholesky-inplace
  "Perform in-place Cholesky decomposition of a positive definite matrix.
   
   This function performs the Cholesky decomposition in-place, modifying
   the input array directly. The output overwrites the input array with
   the triangular matrix (upper or lower portion).
   
   The input matrix must be:
   - Square (dimensions[0] == dimensions[1])
   - Positive definite
   - Floating point or complex type (f32, f64, c32, c64)
   
   On exit, the input array is replaced with the triangular Cholesky factor.
   
   This is more memory-efficient than af-cholesky as it does not create
   a new array, but it destroys the original input data.
   
   This function is not supported in batch mode (ndims must be <= 2).
   This function is not supported in GFOR.
   
   Parameters:
   - info: out pointer to int status (0 if successful, otherwise returns
           the rank at which decomposition fails)
   - in: array handle (input positive definite matrix on entry;
         triangular matrix on exit)
   - is_upper: bool as int (true=1 for upper triangular U, false=0 for lower triangular L)

   Returns:
   ArrayFire error code (AF_SUCCESS on success)"
  "af_cholesky_inplace" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)
