(ns org.soulspace.arrayfire.ffi.c-api.bilateral
  "Bindings for ArrayFire bilateral filter functions.
   Corresponds to src/api/c/bilateral.cpp in ArrayFire."
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.loader]))

;; Bilateral filter

;; af_err af_bilateral(af_array *out, const af_array in, const float spatial_sigma, const float chromatic_sigma, const bool isColor)
(defcfn af-bilateral
  "Apply bilateral filter to an image.
   
   The bilateral filter is an edge-preserving smoothing filter that uses both
   spatial and chromatic variance to determine filter weights. It preserves edges
   while smoothing regions of similar color/intensity.
   
   Parameters:
   - out: out pointer for filtered array
   - in: input image array handle
   - spatial-sigma: spatial variance parameter (controls filter window size)
   - chromatic-sigma: chromatic variance parameter (color/intensity similarity)
   - is-color: bool as int (true for color images, false for grayscale)

   Returns:
   ArrayFire error code"
  "af_bilateral" [::mem/pointer ::mem/pointer ::mem/float ::mem/float ::mem/int] ::mem/int)
