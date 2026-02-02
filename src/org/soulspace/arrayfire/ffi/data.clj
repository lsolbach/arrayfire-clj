(ns org.soulspace.arrayfire.ffi.data
  "Bindings for the ArrayFire data functions."
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; Data generation functions

;; af_err af_constant(af_array *result, const double value, const unsigned ndims, const dim_t *const dims, const af_dtype type)
(defcfn af-constant
  "Generate an array with elements set to a specified value.
   
   Parameters:
   - result: out pointer for array
   - value: constant value (double)
   - ndims: number of dimensions
   - dims: pointer to dimension sizes
   - type: data type

   Returns:
   ArrayFire error code"
  "af_constant" [::mem/pointer ::mem/double ::mem/int ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_constant_complex(af_array *arr, const double real, const double imag, const unsigned ndims, const dim_t *const dims, const af_dtype type)
(defcfn af-constant-complex
  "Generate an array with complex elements set to a specified value.
   
   Parameters:
   - arr: out pointer for array
   - real: real part (double)
   - imag: imaginary part (double)
   - ndims: number of dimensions
   - dims: pointer to dimension sizes
   - type: data type

   Returns:
   ArrayFire error code"
  "af_constant_complex" [::mem/pointer ::mem/double ::mem/double ::mem/int ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_constant_long(af_array *arr, const long long val, const unsigned ndims, const dim_t *const dims)
(defcfn af-constant-long
  "Generate an array with long long elements set to a specified value.
   
   Parameters:
   - arr: out pointer for array
   - val: constant value (long long)
   - ndims: number of dimensions
   - dims: pointer to dimension sizes

   Returns:
   ArrayFire error code"
  "af_constant_long" [::mem/pointer ::mem/long ::mem/int ::mem/pointer] ::mem/int)

;; af_err af_constant_ulong(af_array *arr, const unsigned long long val, const unsigned ndims, const dim_t *const dims)
(defcfn af-constant-ulong
  "Generate an array with unsigned long long elements set to a specified value.
   
   Parameters:
   - arr: out pointer for array
   - val: constant value (unsigned long long)
   - ndims: number of dimensions
   - dims: pointer to dimension sizes

   Returns:
   ArrayFire error code"
  "af_constant_ulong" [::mem/pointer ::mem/long ::mem/int ::mem/pointer] ::mem/int)

;; af_err af_identity(af_array* out, const unsigned ndims, const dim_t* const dims, const af_dtype type)
(defcfn af-identity
  "Generate an identity array.
   
   Parameters:
   - out: out pointer for array
   - ndims: number of dimensions
   - dims: pointer to dimension sizes
   - type: data type

   Returns:
   ArrayFire error code"
  "af_identity" [::mem/pointer ::mem/int ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_range(af_array *out, const unsigned ndims, const dim_t * const dims, const int seq_dim, const af_dtype type)
(defcfn af-range
  "Generate an array with [0, n-1] values along seq_dim and tiled across other dimensions.
   
   Parameters:
   - out: out pointer for array
   - ndims: number of dimensions
   - dims: pointer to dimension sizes
   - seq-dim: dimension along which the range is created
   - type: data type

   Returns:
   ArrayFire error code"
  "af_range" [::mem/pointer ::mem/int ::mem/pointer ::mem/int ::mem/int] ::mem/int)

;; af_err af_iota(af_array *out, const unsigned ndims, const dim_t * const dims, const unsigned t_ndims, const dim_t * const tdims, const af_dtype type)
(defcfn af-iota
  "Generate an array with [0, n-1] values modified to specified dimensions and tiling.
   
   Parameters:
   - out: out pointer for array
   - ndims: number of dimensions
   - dims: pointer to dimension sizes
   - t-ndims: number of tiled dimensions
   - tdims: pointer to tile dimension sizes
   - type: data type

   Returns:
   ArrayFire error code"
  "af_iota" [::mem/pointer ::mem/int ::mem/pointer ::mem/int ::mem/pointer ::mem/int] ::mem/int)

;; Diagonal operations

;; af_err af_diag_create(af_array *out, const af_array in, const int num)
(defcfn af-diag-create
  "Create a diagonal matrix from an array.
   
   Parameters:
   - out: out pointer for diagonal matrix
   - in: input array (diagonal elements)
   - num: diagonal index (0=main, >0=upper, <0=lower)

   Returns:
   ArrayFire error code"
  "af_diag_create" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_diag_extract(af_array *out, const af_array in, const int num)
(defcfn af-diag-extract
  "Extract the diagonal from an array.
   
   Parameters:
   - out: out pointer for extracted diagonal
   - in: input array
   - num: diagonal index (0=main, >0=upper, <0=lower)

   Returns:
   ArrayFire error code"
  "af_diag_extract" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; Array manipulation functions

;; af_err af_join(af_array *out, const int dim, const af_array first, const af_array second)
(defcfn af-join
  "Join 2 arrays along a dimension.
   
   Parameters:
   - out: out pointer for joined array
   - dim: dimension along which to join
   - first: first input array
   - second: second input array

   Returns:
   ArrayFire error code"
  "af_join" [::mem/pointer ::mem/int ::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_join_many(af_array *out, const int dim, const unsigned n_arrays, const af_array *inputs)
(defcfn af-join-many
  "Join many arrays along a dimension (up to 10).
   
   Parameters:
   - out: out pointer for joined array
   - dim: dimension along which to join
   - n-arrays: number of arrays to join
   - inputs: pointer to array of array handles

   Returns:
   ArrayFire error code"
  "af_join_many" [::mem/pointer ::mem/int ::mem/int ::mem/pointer] ::mem/int)

;; af_err af_tile(af_array *out, const af_array in, const unsigned x, const unsigned y, const unsigned z, const unsigned w)
(defcfn af-tile
  "Generate a tiled array by repeating input along dimensions.
   
   Parameters:
   - out: out pointer for tiled array
   - in: input array
   - x: number of tiles along first dimension
   - y: number of tiles along second dimension
   - z: number of tiles along third dimension
   - w: number of tiles along fourth dimension

   Returns:
   ArrayFire error code"
  "af_tile" [::mem/pointer ::mem/pointer ::mem/int ::mem/int ::mem/int ::mem/int] ::mem/int)

;; af_err af_reorder(af_array *out, const af_array in, const unsigned x, const unsigned y, const unsigned z, const unsigned w)
(defcfn af-reorder
  "Reorder an array by changing dimension order.
   
   Parameters:
   - out: out pointer for reordered array
   - in: input array
   - x: specifies which dimension should be first
   - y: specifies which dimension should be second
   - z: specifies which dimension should be third
   - w: specifies which dimension should be fourth

   Returns:
   ArrayFire error code"
  "af_reorder" [::mem/pointer ::mem/pointer ::mem/int ::mem/int ::mem/int ::mem/int] ::mem/int)

;; af_err af_shift(af_array *out, const af_array in, const int x, const int y, const int z, const int w)
(defcfn af-shift
  "Shift an array along dimensions.
   
   Parameters:
   - out: out pointer for shifted array
   - in: input array
   - x: shift along first dimension
   - y: shift along second dimension
   - z: shift along third dimension
   - w: shift along fourth dimension

   Returns:
   ArrayFire error code"
  "af_shift" [::mem/pointer ::mem/pointer ::mem/int ::mem/int ::mem/int ::mem/int] ::mem/int)

;; af_err af_moddims(af_array *out, const af_array in, const unsigned ndims, const dim_t * const dims)
(defcfn af-moddims
  "Modify the dimensions of an array to a specified shape.
   
   Parameters:
   - out: out pointer for reshaped array
   - in: input array
   - ndims: number of dimensions
   - dims: pointer to new dimension sizes

   Returns:
   ArrayFire error code"
  "af_moddims" [::mem/pointer ::mem/pointer ::mem/int ::mem/pointer] ::mem/int)

;; af_err af_flat(af_array *out, const af_array in)
(defcfn af-flat
  "Flatten an array to one dimension.
   
   Parameters:
   - out: out pointer for flattened array
   - in: input array

   Returns:
   ArrayFire error code"
  "af_flat" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_flip(af_array *out, const af_array in, const unsigned dim)
(defcfn af-flip
  "Flip an array along a dimension.
   
   Parameters:
   - out: out pointer for flipped array
   - in: input array
   - dim: dimension to flip

   Returns:
   ArrayFire error code"
  "af_flip" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; Triangle operations

;; af_err af_lower(af_array *out, const af_array in, bool is_unit_diag)
(defcfn af-lower
  "Return the lower triangle array.
   
   Parameters:
   - out: out pointer for lower triangle array
   - in: input array
   - is-unit-diag: boolean (as int) specifying if diagonal elements are 1's

   Returns:
   ArrayFire error code"
  "af_lower" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_upper(af_array *out, const af_array in, bool is_unit_diag)
(defcfn af-upper
  "Return the upper triangle array.
   
   Parameters:
   - out: out pointer for upper triangle array
   - in: input array
   - is-unit-diag: boolean (as int) specifying if diagonal elements are 1's

   Returns:
   ArrayFire error code"
  "af_upper" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; Conditional operations

;; af_err af_select(af_array *out, const af_array cond, const af_array a, const af_array b)
(defcfn af-select
  "Select elements based on a conditional array.
   
   Parameters:
   - out: out pointer for result array
   - cond: conditional array
   - a: array to select from when condition is true
   - b: array to select from when condition is false

   Returns:
   ArrayFire error code"
  "af_select" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_select_scalar_r(af_array *out, const af_array cond, const af_array a, const double b)
(defcfn af-select-scalar-r
  "Select between array and scalar (scalar on right).
   
   Parameters:
   - out: out pointer for result array
   - cond: conditional array
   - a: array to select from when condition is true
   - b: scalar value when condition is false

   Returns:
   ArrayFire error code"
  "af_select_scalar_r" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/double] ::mem/int)

;; af_err af_select_scalar_l(af_array *out, const af_array cond, const double a, const af_array b)
(defcfn af-select-scalar-l
  "Select between scalar and array (scalar on left).
   
   Parameters:
   - out: out pointer for result array
   - cond: conditional array
   - a: scalar value when condition is true
   - b: array to select from when condition is false

   Returns:
   ArrayFire error code"
  "af_select_scalar_l" [::mem/pointer ::mem/pointer ::mem/double ::mem/pointer] ::mem/int)

;; af_err af_replace(af_array a, const af_array cond, const af_array b)
(defcfn af-replace
  "Replace elements in array based on condition.
   
   Parameters:
   - a: array to modify (in-place)
   - cond: conditional array
   - b: replacement values

   Returns:
   ArrayFire error code"
  "af_replace" [::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_replace_scalar(af_array a, const af_array cond, const double b)
(defcfn af-replace-scalar
  "Replace elements in array with scalar based on condition.
   
   Parameters:
   - a: array to modify (in-place)
   - cond: conditional array
   - b: replacement scalar value

   Returns:
   ArrayFire error code"
  "af_replace_scalar" [::mem/pointer ::mem/pointer ::mem/double] ::mem/int)

;; af_err af_pad(af_array *out, const af_array in, const unsigned b_ndims, const dim_t *const b_dims, const unsigned e_ndims, const dim_t *const e_dims, const af_border_type ptype)
(defcfn af-pad
  "Pad an array with specified border type.
   
   Parameters:
   - out: out pointer for padded array
   - in: input array
   - b-ndims: number of dimensions for begin padding
   - b-dims: pointer to begin padding sizes
   - e-ndims: number of dimensions for end padding
   - e-dims: pointer to end padding sizes
   - ptype: border type (0=AF_PAD_ZERO, 1=AF_PAD_SYM, etc.)

   Returns:
   ArrayFire error code"
  "af_pad" [::mem/pointer ::mem/pointer ::mem/int ::mem/pointer ::mem/int ::mem/pointer ::mem/int] ::mem/int)
