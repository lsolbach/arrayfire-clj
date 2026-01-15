(ns org.soulspace.arrayfire.ffi.blas
  "Bindings for ArrayFire BLAS (Basic Linear Algebra Subprograms) functions.
   Corresponds to src/api/c/blas.cpp in ArrayFire."
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; BLAS operations

;; af_err af_gemm(af_array *C, const af_mat_prop opA, const af_mat_prop opB, const void *alpha, const af_array A, const af_array B, const void *beta)
(defcfn af-gemm
  "General matrix multiply (GEMM) operation.
   
   Performs: C = alpha * opA(A) * opB(B) + beta * C
   
   where alpha and beta are scalars, A and B are the matrix operands,
   and opA/opB specify optional transpose operations.
   
   Parameters:
   - C: out pointer (preallocated output array or pointer to null for new allocation)
   - opA: af_mat_prop for matrix A (AF_MAT_NONE=0, AF_MAT_TRANS=1, AF_MAT_CTRANS=2)
   - opB: af_mat_prop for matrix B (AF_MAT_NONE=0, AF_MAT_TRANS=1, AF_MAT_CTRANS=2)
   - alpha: pointer to scalar alpha value (type must match array dtype)
   - A: array handle for left-hand matrix
   - B: array handle for right-hand matrix
   - beta: pointer to scalar beta value (type must match array dtype)

   Returns:
   ArrayFire error code"
  "af_gemm" [::mem/pointer ::mem/int ::mem/int ::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_matmul(af_array *out, const af_array lhs, const af_array rhs, const af_mat_prop optLhs, const af_mat_prop optRhs)
(defcfn af-matmul
  "Matrix multiplication.
   
   Performs: out = optLhs(lhs) * optRhs(rhs)
   
   Simplified matrix multiply with implicit alpha=1 and beta=0.
   Supports sparse-dense multiplication when lhs is sparse (CSR format only).
   
   Parameters:
   - out: out pointer for result array
   - lhs: array handle for left-hand matrix
   - rhs: array handle for right-hand matrix  
   - optLhs: af_mat_prop for lhs (AF_MAT_NONE=0, AF_MAT_TRANS=1, AF_MAT_CTRANS=2)
   - optRhs: af_mat_prop for rhs (AF_MAT_NONE=0, AF_MAT_TRANS=1, AF_MAT_CTRANS=2)

   Returns:
   ArrayFire error code"
  "af_matmul" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/int] ::mem/int)

;; af_err af_dot(af_array *out, const af_array lhs, const af_array rhs, const af_mat_prop optLhs, const af_mat_prop optRhs)
(defcfn af-dot
  "Dot product (inner product) of two vectors.
   
   Computes the scalar dot product between two vectors.
   Returns a scalar array (single element).
   
   Parameters:
   - out: out pointer for dot product result (scalar array)
   - lhs: array handle for left-hand vector
   - rhs: array handle for right-hand vector
   - optLhs: af_mat_prop for lhs (only AF_MAT_NONE=0 and AF_MAT_CONJ=4 supported)
   - optRhs: af_mat_prop for rhs (only AF_MAT_NONE=0 and AF_MAT_CONJ=4 supported)

   Returns:
   ArrayFire error code"
  "af_dot" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/int] ::mem/int)

;; af_err af_dot_all(double *real, double *imag, const af_array lhs, const af_array rhs, const af_mat_prop optLhs, const af_mat_prop optRhs)
(defcfn af-dot-all
  "Dot product with immediate result extraction.
   
   Computes dot product and returns the scalar result directly via output pointers.
   For complex arrays, returns both real and imaginary components.
   
   Parameters:
   - real: out pointer to double for real component of dot product
   - imag: out pointer to double for imaginary component (0 for real arrays)
   - lhs: array handle for left-hand vector
   - rhs: array handle for right-hand vector
   - optLhs: af_mat_prop for lhs (only AF_MAT_NONE=0 and AF_MAT_CONJ=4 supported)
   - optRhs: af_mat_prop for rhs (only AF_MAT_NONE=0 and AF_MAT_CONJ=4 supported)

   Returns:
   ArrayFire error code"
  "af_dot_all" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/int] ::mem/int)

;; af_err af_transpose(af_array *out, af_array in, const bool conjugate)
(defcfn af-transpose
  "Transpose a matrix.
   
   Transposes the input matrix. Optionally performs conjugate transpose
   for complex arrays.
   
   Parameters:
   - out: out pointer for transposed array
   - in: array handle to transpose
   - conjugate: bool as int (true=conjugate transpose, false=regular transpose)

   Returns:
   ArrayFire error code"
  "af_transpose" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_transpose_inplace(af_array in, const bool conjugate)
(defcfn af-transpose-inplace
  "Transpose a matrix in-place.
   
   Transposes the input matrix in-place (modifies the input).
   Optionally performs conjugate transpose for complex arrays.
   
   Parameters:
   - in: array handle to transpose in-place
   - conjugate: bool as int (true=conjugate transpose, false=regular transpose)

   Returns:
   ArrayFire error code"
  "af_transpose_inplace" [::mem/pointer ::mem/int] ::mem/int)
