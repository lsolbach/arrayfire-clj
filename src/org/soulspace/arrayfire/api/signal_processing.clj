(ns org.soulspace.arrayfire.api.signal-processing
  "Signal processing functions for ArrayFire arrays.
  
   This namespace includes functions for
   - convolution
   - correlation
   - Fourier transforms
   - filtering operations.
   
   These functions leverage ArrayFire's GPU acceleration to efficiently process
   large signals and perform complex operations in real-time."
  (:require [org.soulspace.arrayfire.api.core :as af]))