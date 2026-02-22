(ns org.soulspace.arrayfire.api.statistics
  "Idiomatic Clojure Statistical API for ArrayFire arrays.
  
   This namespace includes functions for
   - mean
   - median
   - variance
   - standard deviation
   - covariance
   - correlation coefficients
   - histogram computation
   
   These functions leverage ArrayFire's GPU acceleration to efficiently process
   large datasets and perform complex operations in real-time."
  (:require [org.soulspace.arrayfire.integration.base.definitions :as defs]
            [org.soulspace.arrayfire.api.core :as af]))