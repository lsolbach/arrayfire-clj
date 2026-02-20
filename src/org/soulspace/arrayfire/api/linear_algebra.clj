(ns org.soulspace.arrayfire.api.linear-algebra
  "Linear algebra functions for ArrayFire arrays.
  
   This namespace includes functions for
   - matrix multiplication
   - matrix decomposition (LU, QR, SVD)
   - solving linear systems
   - matrix inversion
   
   These functions leverage ArrayFire's GPU acceleration to efficiently process
   large matrices and perform complex operations in real-time."
  (:require [org.soulspace.arrayfire.api.core :as af]))

