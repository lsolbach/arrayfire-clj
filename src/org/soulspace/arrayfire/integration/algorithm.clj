(ns org.soulspace.arrayfire.integration.algorithm
  "Integration of the ArrayFire algorithm related FFI bindings with the error
   handling and resource management on the JVM."
  (:require [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.lu :as lu]
            [org.soulspace.arrayfire.ffi.qr :as qr]
            [org.soulspace.arrayfire.ffi.svd :as svd]
            [org.soulspace.arrayfire.integration.jvm-integration :as jvm])
  (:import (org.soulspace.arrayfire.integration.jvm_integration AFArray)))


(defn lu
  "Compute the LU decomposition of a matrix.
   
   Parameters:
   - a: Input matrix (AFArray)
   
   Returns:
   A vector containing three AFArray instances:
   - L: Lower triangular matrix
   - U: Upper triangular matrix
   - P: Pivot indices as a permutation matrix"
  [^AFArray a]
  (let [l (jvm/native-af-array-pointer)
        u (jvm/native-af-array-pointer)
        p (jvm/native-af-array-pointer)]
    (jvm/check! (lu/af-lu l u p (jvm/af-handle a)) "af-lu")
    [(jvm/af-array-new (jvm/deref-af-array l))
     (jvm/af-array-new (jvm/deref-af-array u))
     (jvm/af-array-new (jvm/deref-af-array p))]))

(defn qr
  "Compute the QR decomposition of a matrix.
   
   Parameters:
   - a: Input matrix (AFArray)
   
   Returns:
   A vector containing two AFArray instances:
   - Q: Orthogonal matrix
   - R: Upper triangular matrix"
  [^AFArray a]
  (let [q (jvm/native-af-array-pointer)
        r (jvm/native-af-array-pointer)
        tau (jvm/native-af-array-pointer)]
    (jvm/check! (qr/af-qr q r tau (jvm/af-handle a)) "af-qr")
    [(jvm/af-array-new (jvm/deref-af-array q))
     (jvm/af-array-new (jvm/deref-af-array r))
     (jvm/af-array-new (jvm/deref-af-array tau))]))

(defn svd
  "Compute the Singular Value Decomposition (SVD) of a matrix.
   
   Parameters:
   - a: Input matrix (AFArray)
   
   Returns:
   A vector containing three AFArray instances:
   - U: Left singular vectors
   - S: Singular values (as a diagonal matrix)
   - VT: Right singular vectors (transposed)"
  [^AFArray a]
  (let [u  (jvm/native-af-array-pointer)
        s  (jvm/native-af-array-pointer)
        vt (jvm/native-af-array-pointer)]
    (jvm/check! (svd/af-svd u s vt (jvm/af-handle a)) "af-svd")
    [(jvm/af-array-new (jvm/deref-af-array u))
     (jvm/af-array-new (jvm/deref-af-array s))
     (jvm/af-array-new (jvm/deref-af-array vt))]))
