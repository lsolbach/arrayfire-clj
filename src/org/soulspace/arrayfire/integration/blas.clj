(ns org.soulspace.arrayfire.integration.blas
  "Integration of the ArrayFire BLAS related FFI bindings with the error
   handling and resource management on the JVM."
  (:require [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.blas :as blas]
            [org.soulspace.arrayfire.integration.jvm-integration :as jvm])
  (:import (org.soulspace.arrayfire.integration.jvm_integration AFArray)))

;;;
;;; Matrix Operations
;;;

(defn gemm
  "General matrix multiply (GEMM) operation.
   
   Performs: C = alpha * opA(A) * opB(B) + beta * C
   
   Parameters:
   - op-a: Matrix property for A (0=none, 1=transpose, 2=conjugate-transpose)
   - op-b: Matrix property for B (0=none, 1=transpose, 2=conjugate-transpose)
   - alpha: Scalar multiplier for A*B (as double)
   - a: Left-hand matrix (AFArray)
   - b: Right-hand matrix (AFArray)
   - beta: Scalar multiplier for C (as double)
   
   Returns:
   AFArray containing the result C"
  [op-a op-b alpha ^AFArray a ^AFArray b beta]
  (let [out (jvm/native-af-array-pointer)
        alpha-buf (mem/alloc 8)
        beta-buf (mem/alloc 8)
        _ (jvm/write-double! alpha-buf 0 alpha)
        _ (jvm/write-double! beta-buf 0 beta)]
    (jvm/check! (blas/af-gemm out (int op-a) (int op-b) 
                              alpha-buf (jvm/af-handle a) (jvm/af-handle b) beta-buf)
                "af-gemm")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn matmul
  "Matrix multiplication.
   
   Performs: out = optLhs(lhs) * optRhs(rhs)
   
   Simplified matrix multiply with implicit alpha=1 and beta=0.
   Supports sparse-dense multiplication when lhs is sparse (CSR format only).
   
   Parameters:
   - lhs: Left-hand matrix (AFArray)
   - rhs: Right-hand matrix (AFArray)
   - opt-lhs: Matrix property for lhs (0=none, 1=transpose, 2=conjugate-transpose), default 0
   - opt-rhs: Matrix property for rhs (0=none, 1=transpose, 2=conjugate-transpose), default 0
   
   Returns:
   AFArray containing the result"
  ([^AFArray lhs ^AFArray rhs]
   (matmul lhs rhs 0 0))
  ([^AFArray lhs ^AFArray rhs opt-lhs opt-rhs]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (blas/af-matmul out (jvm/af-handle lhs) (jvm/af-handle rhs) 
                                  (int opt-lhs) (int opt-rhs))
                 "af-matmul")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn dot
  "Dot product (inner product) of two vectors.
   
   Computes the scalar dot product between two vectors.
   Returns a scalar array (single element).
   
   Parameters:
   - lhs: Left-hand vector (AFArray)
   - rhs: Right-hand vector (AFArray)
   - opt-lhs: Matrix property for lhs (0=none, 4=conjugate), default 0
   - opt-rhs: Matrix property for rhs (0=none, 4=conjugate), default 0
   
   Returns:
   AFArray containing the scalar dot product result"
  ([^AFArray lhs ^AFArray rhs]
   (dot lhs rhs 0 0))
  ([^AFArray lhs ^AFArray rhs opt-lhs opt-rhs]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (blas/af-dot out (jvm/af-handle lhs) (jvm/af-handle rhs) 
                              (int opt-lhs) (int opt-rhs))
                 "af-dot")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn dot-all
  "Dot product with immediate result extraction.
   
   Computes dot product and returns the scalar result directly.
   For complex arrays, returns both real and imaginary components.
   
   Parameters:
   - lhs: Left-hand vector (AFArray)
   - rhs: Right-hand vector (AFArray)
   - opt-lhs: Matrix property for lhs (0=none, 4=conjugate), default 0
   - opt-rhs: Matrix property for rhs (0=none, 4=conjugate), default 0
   
   Returns:
   For real arrays: the scalar result as a double
   For complex arrays: [real imag] vector"
  ([^AFArray lhs ^AFArray rhs]
   (dot-all lhs rhs 0 0))
  ([^AFArray lhs ^AFArray rhs opt-lhs opt-rhs]
   (let [real-buf (mem/alloc 8)
         imag-buf (mem/alloc 8)]
     (jvm/check! (blas/af-dot-all real-buf imag-buf (jvm/af-handle lhs) (jvm/af-handle rhs)
                                  (int opt-lhs) (int opt-rhs))
                 "af-dot-all")
     (let [real (jvm/read-double real-buf 0)
           imag (jvm/read-double imag-buf 0)]
       (if (zero? imag)
         real
         [real imag])))))

(defn transpose
  "Transpose a matrix.
   
   Transposes the input matrix. Optionally performs conjugate transpose
   for complex arrays.
   
   Parameters:
   - in: Input matrix (AFArray)
   - conjugate: Boolean, true for conjugate transpose, false for regular transpose (default false)
   
   Returns:
   AFArray containing the transposed matrix"
  ([^AFArray in]
   (transpose in false))
  ([^AFArray in conjugate]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (blas/af-transpose out (jvm/af-handle in) (if conjugate 1 0))
                 "af-transpose")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn transpose!
  "Transpose a matrix in-place.
   
   Transposes the input matrix in-place (modifies the input).
   Optionally performs conjugate transpose for complex arrays.
   
   Parameters:
   - in: Input matrix (AFArray) to be transposed in-place
   - conjugate: Boolean, true for conjugate transpose, false for regular transpose (default false)
   
   Returns:
   The same AFArray (for chaining)"
  ([^AFArray in]
   (transpose! in false))
  ([^AFArray in conjugate]
   (jvm/check! (blas/af-transpose-inplace (jvm/af-handle in) (if conjugate 1 0))
               "af-transpose-inplace")
   in))

;;;
;;; Matrix Multiplication Convenience Functions
;;;

(defn matmul-nt
  "Matrix multiplication: A * B^T (B transposed).
   
   Convenience wrapper for matmul with second matrix transposed.
   Equivalent to: matmul(A, B, AF_MAT_NONE, AF_MAT_TRANS)
   
   Parameters:
   - lhs: Left-hand matrix A (AFArray)
   - rhs: Right-hand matrix B (AFArray), will be transposed
   
   Returns:
   AFArray containing A * B^T
   
   Example:
   ```clojure
   ;; A is [m x k], B is [n x k]
   ;; Result is [m x n]
   (let [a (array [[1 2]    ; 2x2
                   [3 4]])
         b (array [[5 6]    ; 2x2
                   [7 8]])]
     (matmul-nt a b))       ; A * B^T = [m x k] * [k x n] = [m x n]
   ```
   
   Use when:
   - B is stored in transposed form
   - Avoiding explicit transpose operation for efficiency"
  [^AFArray lhs ^AFArray rhs]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (blas/af-matmul out (jvm/af-handle lhs) (jvm/af-handle rhs)
                                0 1)  ; AF_MAT_NONE, AF_MAT_TRANS
                "af-matmul")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn matmul-tn
  "Matrix multiplication: A^T * B (A transposed).
   
   Convenience wrapper for matmul with first matrix transposed.
   Equivalent to: matmul(A, B, AF_MAT_TRANS, AF_MAT_NONE)
   
   Parameters:
   - lhs: Left-hand matrix A (AFArray), will be transposed
   - rhs: Right-hand matrix B (AFArray)
   
   Returns:
   AFArray containing A^T * B
   
   Example:
   ```clojure
   ;; A is [k x m], B is [k x n]
   ;; Result is [m x n]
   (let [a (array [[1 3]    ; 2x2 
                   [2 4]])
         b (array [[5 6]    ; 2x2
                   [7 8]])]
     (matmul-tn a b))       ; A^T * B = [m x k] * [k x n] = [m x n]
   ```
   
   Use when:
   - A is stored in transposed form
   - Computing Gram matrix: A^T * A
   - Normal equations: A^T * A * x = A^T * b"
  [^AFArray lhs ^AFArray rhs]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (blas/af-matmul out (jvm/af-handle lhs) (jvm/af-handle rhs)
                                1 0)  ; AF_MAT_TRANS, AF_MAT_NONE
                "af-matmul")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn matmul-tt
  "Matrix multiplication: A^T * B^T (both transposed).
   
   Convenience wrapper for matmul with both matrices transposed.
   Equivalent to: matmul(A, B, AF_MAT_TRANS, AF_MAT_TRANS)
   
   Parameters:
   - lhs: Left-hand matrix A (AFArray), will be transposed
   - rhs: Right-hand matrix B (AFArray), will be transposed
   
   Returns:
   AFArray containing A^T * B^T
   
   Example:
   ```clojure
   ;; A is [k x m], B is [n x k]
   ;; Result is [m x n]
   (let [a (array [[1 3]    ; 2x2
                   [2 4]])
         b (array [[5 7]    ; 2x2
                   [6 8]])]
     (matmul-tt a b))       ; A^T * B^T = [m x k] * [k x n] = [m x n]
   ```
   
   Use when:
   - Both matrices stored in transposed form
   - Specific computational patterns in algorithms"
  [^AFArray lhs ^AFArray rhs]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (blas/af-matmul out (jvm/af-handle lhs) (jvm/af-handle rhs)
                                1 1)  ; AF_MAT_TRANS, AF_MAT_TRANS
                "af-matmul")
    (jvm/af-array-new (jvm/deref-af-array out))))

;;;
;;; Chain Matrix Multiplication
;;;

(defn matmul3
  "Chain matrix multiplication for 3 matrices: A * B * C.
   
   Efficiently computes the product of three matrices by performing
   two sequential matrix multiplications: (A * B) * C.
   
   Parameters:
   - a: First matrix (AFArray) [m x k]
   - b: Second matrix (AFArray) [k x n]
   - c: Third matrix (AFArray) [n x p]
   
   Returns:
   AFArray containing A * B * C [m x p]
   
   Example:
   ```clojure
   (let [a (array [[1 2]      ; 2x2
                   [3 4]])
         b (array [[5 6]      ; 2x2
                   [7 8]])
         c (array [[1 0]      ; 2x2
                   [0 1]])]
     (matmul3 a b c))         ; (A * B) * C
   ```
   
   Use cases:
   - Deep learning: Chained weight transformations
   - Graphics: Composed transformation matrices
   - Physics: Sequential operator applications"
  [^AFArray a ^AFArray b ^AFArray c]
  (let [temp (matmul a b)]
    (try
      (matmul temp c)
      (finally
        (.close temp)))))

(defn matmul4
  "Chain matrix multiplication for 4 matrices: A * B * C * D.
   
   Efficiently computes the product of four matrices by performing
   three sequential matrix multiplications: ((A * B) * C) * D.
   
   Parameters:
   - a: First matrix (AFArray) [m x k]
   - b: Second matrix (AFArray) [k x n]
   - c: Third matrix (AFArray) [n x p]
   - d: Fourth matrix (AFArray) [p x q]
   
   Returns:
   AFArray containing A * B * C * D [m x q]
   
   Example:
   ```clojure
   (let [a (array [[1 2]      ; 2x2
                   [3 4]])
         b (array [[5 6]      ; 2x2
                   [7 8]])
         c (array [[1 0]      ; 2x2
                   [0 1]])
         d (array [[2 0]      ; 2x2
                   [0 2]])]
     (matmul4 a b c d))       ; ((A * B) * C) * D
   ```
   
   Use cases:
   - Complex transformation pipelines
   - Multi-layer neural networks
   - Quantum gate sequences"
  [^AFArray a ^AFArray b ^AFArray c ^AFArray d]
  (let [temp1 (matmul a b)
        temp2 (try
                (matmul temp1 c)
                (finally
                  (.close temp1)))]
    (try
      (matmul temp2 d)
      (finally
        (.close temp2)))))
