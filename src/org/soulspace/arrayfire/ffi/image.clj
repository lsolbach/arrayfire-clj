(ns org.soulspace.arrayfire.ffi.image
  "Bindings for the ArrayFire image visualization functions."
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; af_err af_draw_image(const af_window window, const af_array in, const af_cell *const props)
(defcfn af-draw-image
  "Draw an image to a window.
   
   Displays an image array in a Forge graphics window. The image can be 
   grayscale (1 channel), RGB (3 channels), or RGBA (4 channels).
   
   Parameters:
   - window: window handle
   - in: array handle (image to display)
   - props: pointer to af_cell structure (grid position, title, colormap)

   Returns:
   ArrayFire error code
   
   Note: This is a visualization function requiring Forge graphics library.
   The input array should be 2D (grayscale) or 3D with 3-4 channels (color).
   
   See: src/api/c/image.cpp in ArrayFire source"
  "af_draw_image" [::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)
