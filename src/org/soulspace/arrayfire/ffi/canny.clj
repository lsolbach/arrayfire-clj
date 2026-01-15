(ns org.soulspace.arrayfire.ffi.canny
  "Bindings for ArrayFire Canny edge detection functions.
   Corresponds to src/api/c/canny.cpp in ArrayFire."
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; Canny edge detection

;; af_err af_canny(af_array *out, const af_array in, const af_canny_threshold threshold_type, const float low_threshold_ratio, const float high_threshold_ratio, const unsigned sobel_window, const bool is_fast)
(defcfn af-canny
  "Canny edge detector.
   
   Performs edge detection using the Canny algorithm, which uses a multi-stage
   algorithm including Gaussian smoothing, gradient calculation, non-maximum
   suppression, and hysteresis thresholding.
   
   Parameters:
   - out: out pointer for binary array containing detected edges
   - in: input grayscale image array handle (color images not supported)
   - threshold-type: af_canny_threshold enum value:
     * AF_CANNY_THRESHOLD_MANUAL=0: Use user-provided threshold ratios
     * AF_CANNY_THRESHOLD_AUTO_OTSU=1: Automatically compute thresholds using Otsu method
   - low-threshold-ratio: lower threshold as % of max/auto-derived high threshold
     (e.g., 0.08 for 8% of high threshold)
   - high-threshold-ratio: higher threshold as % of maximum gradient value
     (e.g., 0.32 for 32% of max, ignored if AUTO_OTSU is used)
   - sobel-window: sobel kernel window size for gradient computation (only 3 supported)
   - is-fast: bool as int (true=L1 norm faster, false=L2 norm more accurate)

   Returns:
   ArrayFire error code
   
   Note: Input image should be at least 5x5 (size of Gaussian smoothing filter)"
  "af_canny" [::mem/pointer ::mem/pointer ::mem/int ::mem/float ::mem/float ::mem/int ::mem/int] ::mem/int)
