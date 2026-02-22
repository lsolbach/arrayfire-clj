(ns org.soulspace.arrayfire.api.image-processing
  "Idiomatic Clojure image processing API for ArrayFire arrays.
   
   This namespace includes functions for
   - image filtering
   - edge and corner detection
   - morphological operations
   - color space conversion
   - geometric transformations such as rotation and scaling
   - image IO

   These functions leverage ArrayFire's GPU acceleration to efficiently process
   large images and perform complex operations in real-time."
  (:require [org.soulspace.arrayfire.integration.base.definitions :as defs]
            [org.soulspace.arrayfire.api.core :as af]))

