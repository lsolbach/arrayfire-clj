(ns org.soulspace.arrayfire.ffi.c-api.sobel
  "ArrayFire FFI bindings for Sobel edge detection.

  The Sobel operator computes an approximation of the gradient of an image by
  applying discrete differentiation operators in both horizontal and vertical
  directions. It uses small, separable convolution kernels to compute derivatives.

  ## Mathematical Foundation

  The Sobel operator uses two 3×3 kernels which are convolved with the image:

  Horizontal (x) derivative Gx:
  ```
  [-1  0  1]
  [-2  0  2]
  [-1  0  1]
  ```

  Vertical (y) derivative Gy:
  ```
  [-1 -2 -1]
  [ 0  0  0]
  [ 1  2  1]
  ```

  These kernels combine Gaussian smoothing with differentiation, providing some
  noise reduction while detecting edges. The magnitude of the gradient at each
  pixel is given by: G = √(Gx² + Gy²), and the direction by: θ = atan2(Gy, Gx).

  ## Supported Types

  - Floating point: f32, f64
  - Integer types: s32, u32, s16, u16, s8, u8
  - Binary: b8

  Note: Integer input types are promoted to 32-bit signed integer output.

  ## Batch Processing

  If the input is a 3D array, Sobel filtering is applied to each 2D slice,
  enabling efficient batch processing of multiple images.

  ## Performance Notes

  - Sobel filtering is implemented as a GPU-accelerated operation
  - Uses separable convolution for efficient computation
  - Currently only ker_size=3 is supported
  - Suitable for real-time edge detection applications"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;;
;; Sobel Edge Detection
;;

(defcfn af-sobel-operator
  "Compute the Sobel gradients of an image.

  The Sobel operator computes directional derivatives using small separable
  convolution kernels. It returns two arrays containing the horizontal (dx)
  and vertical (dy) gradient approximations.

  Parameters:
  - dx: Output array for horizontal (x-direction) derivative
  - dy: Output array for vertical (y-direction) derivative
  - img: Input image array
  - ker-size: Sobel kernel size (currently only 3 is supported)

  Returns:
  Error code indicating success or failure.

  Example:
  ```clojure
  (let [img (af-constant [512 512] 0.0 :f32)
        dx (mem/alloc-instance ::mem/pointer)
        dy (mem/alloc-instance ::mem/pointer)]
    (af-sobel-operator dx dy img 3)
    ;; dx contains horizontal edges, dy contains vertical edges
    )
  ```

  Note:
  - If img is 3D, batch operation is performed on all slices
  - Integer types are promoted to s32 output
  - Currently only ker_size=3 (3×3 Sobel kernels) is supported

  See also:
  - af_sobel_operator (ArrayFire C API)"
  "af_sobel_operator" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)
