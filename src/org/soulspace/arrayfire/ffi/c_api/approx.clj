(ns org.soulspace.arrayfire.ffi.c-api.approx
  "Bindings for the ArrayFire approximation/interpolation functions.
  
  Corresponds to src/api/c/approx.cpp in ArrayFire."
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; Approximation/Interpolation functions

;; af_err af_approx1(af_array *yo, const af_array yi, const af_array xo, const af_interp_type method, const float offGrid)
(defcfn af-approx1
  "1D interpolation on uniformly spaced data.
  
  Parameters:
  yo - pointer to output array
  yi - input array (values at uniformly spaced points)
  xo - positions for interpolation
  method - interpolation method (int: nearest, linear, cubic, etc.)
  off_grid - value for indices outside valid range
  
  Returns:
  af_err error code"
  "af_approx1" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/float] ::mem/int)

;; af_err af_approx1_v2(af_array *yo, const af_array yi, const af_array xo, const af_interp_type method, const float offGrid)
(defcfn af-approx1-v2
  "1D interpolation with preallocated output array.
  
  Parameters:
  yo - pointer to output array (can be preallocated)
  yi - input array (values at uniformly spaced points)
  xo - positions for interpolation
  method - interpolation method (int: nearest, linear, cubic, etc.)
  off_grid - value for indices outside valid range
  
  Returns:
  af_err error code"
  "af_approx1_v2" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/float] ::mem/int)

;; af_err af_approx1_uniform(af_array *yo, const af_array yi, const af_array xo, const int xdim, const double xi_beg, const double xi_step, const af_interp_type method, const float offGrid)
(defcfn af-approx1-uniform
  "1D interpolation with explicit uniform input spacing.
  
  Parameters:
  yo - pointer to output array
  yi - input array
  xo - positions for interpolation
  xdim - dimension along which to interpolate
  xi_beg - starting coordinate of input
  xi_step - spacing between input coordinates
  method - interpolation method (int)
  off_grid - value for indices outside valid range
  
  Returns:
  af_err error code"
  "af_approx1_uniform" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/double ::mem/double ::mem/int ::mem/float] ::mem/int)

;; af_err af_approx1_uniform_v2(af_array *yo, const af_array yi, const af_array xo, const int xdim, const double xi_beg, const double xi_step, const af_interp_type method, const float offGrid)
(defcfn af-approx1-uniform-v2
  "1D interpolation with explicit uniform spacing and preallocated output.
  
  Parameters:
  yo - pointer to output array (can be preallocated)
  yi - input array
  xo - positions for interpolation
  xdim - dimension along which to interpolate
  xi_beg - starting coordinate of input
  xi_step - spacing between input coordinates
  method - interpolation method (int)
  off_grid - value for indices outside valid range
  
  Returns:
  af_err error code"
  "af_approx1_uniform_v2" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/double ::mem/double ::mem/int ::mem/float] ::mem/int)

;; af_err af_approx2(af_array *zo, const af_array zi, const af_array xo, const af_array yo, const af_interp_type method, const float offGrid)
(defcfn af-approx2
  "2D interpolation on uniformly spaced data.
  
  Parameters:
  zo - pointer to output array
  zi - input array (values at uniformly spaced 2D grid)
  xo - x-positions for interpolation
  yo - y-positions for interpolation
  method - interpolation method (int: nearest, bilinear, bicubic, etc.)
  off_grid - value for indices outside valid range
  
  Returns:
  af_err error code"
  "af_approx2" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/float] ::mem/int)

;; af_err af_approx2_v2(af_array *zo, const af_array zi, const af_array xo, const af_array yo, const af_interp_type method, const float offGrid)
(defcfn af-approx2-v2
  "2D interpolation with preallocated output array.
  
  Parameters:
  zo - pointer to output array (can be preallocated)
  zi - input array (values at uniformly spaced 2D grid)
  xo - x-positions for interpolation
  yo - y-positions for interpolation
  method - interpolation method (int: nearest, bilinear, bicubic, etc.)
  off_grid - value for indices outside valid range
  
  Returns:
  af_err error code"
  "af_approx2_v2" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/float] ::mem/int)

;; af_err af_approx2_uniform(af_array *zo, const af_array zi, const af_array xo, const int xdim, const double xi_beg, const double xi_step, const af_array yo, const int ydim, const double yi_beg, const double yi_step, const af_interp_type method, const float offGrid)
(defcfn af-approx2-uniform
  "2D interpolation with explicit uniform input spacing.
  
  Parameters:
  zo - pointer to output array
  zi - input array
  xo - x-positions for interpolation
  xdim - dimension along x-axis
  xi_beg - starting x-coordinate of input
  xi_step - spacing between x-coordinates
  yo - y-positions for interpolation
  ydim - dimension along y-axis
  yi_beg - starting y-coordinate of input
  yi_step - spacing between y-coordinates
  method - interpolation method (int)
  off_grid - value for indices outside valid range
  
  Returns:
  af_err error code"
  "af_approx2_uniform" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/double ::mem/double ::mem/pointer ::mem/int ::mem/double ::mem/double ::mem/int ::mem/float] ::mem/int)

;; af_err af_approx2_uniform_v2(af_array *zo, const af_array zi, const af_array xo, const int xdim, const double xi_beg, const double xi_step, const af_array yo, const int ydim, const double yi_beg, const double yi_step, const af_interp_type method, const float offGrid)
(defcfn af-approx2-uniform-v2
  "2D interpolation with explicit uniform spacing and preallocated output.
  
  Parameters:
  zo - pointer to output array (can be preallocated)
  zi - input array
  xo - x-positions for interpolation
  xdim - dimension along x-axis
  xi_beg - starting x-coordinate of input
  xi_step - spacing between x-coordinates
  yo - y-positions for interpolation
  ydim - dimension along y-axis
  yi_beg - starting y-coordinate of input
  yi_step - spacing between y-coordinates
  method - interpolation method (int)
  off_grid - value for indices outside valid range
  
  Returns:
  af_err error code"
  "af_approx2_uniform_v2" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/double ::mem/double ::mem/pointer ::mem/int ::mem/double ::mem/double ::mem/int ::mem/float] ::mem/int)
