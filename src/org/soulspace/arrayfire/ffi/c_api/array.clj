(ns org.soulspace.arrayfire.ffi.c-api.array
  "Bindings for the ArrayFire array functions."
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; Array management functions

;; af_err af_get_data_ptr(void *data, const af_array arr)
(defcfn af-get-data-ptr
  "Copy array data to host memory.
   
   Parameters:
   - data: out pointer to host buffer
   - arr: array handle

   Returns:
   ArrayFire error code"
  "af_get_data_ptr" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_create_array(af_array *arr, const void *data, unsigned ndims, const dim_t *dims, af_dtype type)
(defcfn af-create-array
  "Create an ArrayFire array from host data.
   

   Parameters:
   - arr: out pointer
   - data: in pointer
   - ndims: unsigned
   - dims: pointer to dim_t
   - type: int
   
   Returns:
   ArrayFire error code"
  "af_create_array" [::mem/pointer ::mem/pointer ::mem/int ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_release_array(af_array arr)
(defcfn af-release-array
  "Release an ArrayFire array handle.
   
   Parameters:
   - arr: array handle
   
   Returns:
   ArrayFire error code"
  "af_release_array" [::mem/pointer] ::mem/int)

;; af_err af_retain_array(af_array *out, const af_array in)
(defcfn af-retain-array
  "Increase reference count of the array.
   
   Parameters:
   - out: out pointer to array handle
   - in: array handle to retain

   Returns:
   ArrayFire error code"
  "af_retain_array" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_copy_array(af_array *arr, const af_array in)
(defcfn af-copy-array
  "Deep copy an array to another.
   
   Parameters:
   - arr: out pointer for new array
   - in: array handle to copy

   Returns:
   ArrayFire error code"
  "af_copy_array" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_write_array(af_array arr, const void *data, const size_t bytes, af_source src)
(defcfn af-write-array
  "Copy data from a C pointer (host/device) to an existing array.
   
   Parameters:
   - arr: array handle
   - data: pointer to data
   - bytes: size in bytes
   - src: source type (0=host, 1=device)

   Returns:
   ArrayFire error code"
  "af_write_array" [::mem/pointer ::mem/pointer ::mem/long ::mem/int] ::mem/int)

;; af_err af_get_data_ref_count(int *use_count, const af_array in)
(defcfn af-get-data-ref-count
  "Get the reference count of the array data.
   
   Parameters:
   - use-count: out pointer to int
   - in: array handle

   Returns:
   ArrayFire error code"
  "af_get_data_ref_count" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_create_handle(af_array *arr, const unsigned ndims, const dim_t *const dims, const af_dtype type)
(defcfn af-create-handle
  "Create an empty array with specified dimensions and type.
   
   Parameters:
   - arr: out pointer for array handle
   - ndims: number of dimensions
   - dims: pointer to dimensions array
   - type: data type

   Returns:
   ArrayFire error code"
  "af_create_handle" [::mem/pointer ::mem/int ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_device_array(af_array *arr, void *data, const unsigned ndims, const dim_t *const dims, const af_dtype type)
(defcfn af-device-array
  "Create an array from device memory.
   
   Parameters:
   - arr: out pointer for array handle
   - data: device pointer
   - ndims: number of dimensions
   - dims: pointer to dimensions array
   - type: data type

   Returns:
   ArrayFire error code"
  "af_device_array" [::mem/pointer ::mem/pointer ::mem/int ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_get_elements(dim_t *elems, const af_array arr)
(defcfn af-get-elements
  "Get the total number of elements across all dimensions.
   
   Parameters:
   - elems: out pointer to dim_t (long)
   - arr: array handle

   Returns:
   ArrayFire error code"
  "af_get_elements" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_get_type(af_dtype *type, const af_array arr)
(defcfn af-get-type
  "Get the type of an array.
   
   Parameters:
   - type: out pointer to dtype (int)
   - arr: array handle

   Returns:
   ArrayFire error code"
  "af_get_type" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_get_dims(dim_t *d0, dim_t *d1, dim_t *d2, dim_t *d3, const af_array arr)
(defcfn af-get-dims
  "Get the dimensions of an array.
   
   Parameters:
   - d0: out pointer for first dimension
   - d1: out pointer for second dimension
   - d2: out pointer for third dimension
   - d3: out pointer for fourth dimension
   - arr: array handle

   Returns:
   ArrayFire error code"
  "af_get_dims" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_get_numdims(unsigned *result, const af_array arr)
(defcfn af-get-numdims
  "Get the number of dimensions of an array.
   
   Parameters:
   - result: out pointer to unsigned int
   - arr: array handle

   Returns:
   ArrayFire error code"
  "af_get_numdims" [::mem/pointer ::mem/pointer] ::mem/int)

;; Array property check functions
;; af_err af_is_empty(bool *result, const af_array arr)
(defcfn af-is-empty
  "Check if an array is empty (has 0 elements).
   
   Parameters:
   - result: out pointer to bool (int)
   - arr: array handle

   Returns:
   ArrayFire error code"
  "af_is_empty" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_is_scalar(bool *result, const af_array arr)
(defcfn af-is-scalar
  "Check if an array is a scalar (single element).
   
   Parameters:
   - result: out pointer to bool (int)
   - arr: array handle

   Returns:
   ArrayFire error code"
  "af_is_scalar" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_is_row(bool *result, const af_array arr)
(defcfn af-is-row
  "Check if an array is a row vector.
   
   Parameters:
   - result: out pointer to bool (int)
   - arr: array handle

   Returns:
   ArrayFire error code"
  "af_is_row" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_is_column(bool *result, const af_array arr)
(defcfn af-is-column
  "Check if an array is a column vector.
   
   Parameters:
   - result: out pointer to bool (int)
   - arr: array handle

   Returns:
   ArrayFire error code"
  "af_is_column" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_is_vector(bool *result, const af_array arr)
(defcfn af-is-vector
  "Check if an array is a vector (any 1D arrangement).
   
   Parameters:
   - result: out pointer to bool (int)
   - arr: array handle

   Returns:
   ArrayFire error code"
  "af_is_vector" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_is_complex(bool *result, const af_array arr)
(defcfn af-is-complex
  "Check if an array contains complex numbers.
   
   Parameters:
   - result: out pointer to bool (int)
   - arr: array handle

   Returns:
   ArrayFire error code"
  "af_is_complex" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_is_real(bool *result, const af_array arr)
(defcfn af-is-real
  "Check if an array contains real numbers.
   
   Parameters:
   - result: out pointer to bool (int)
   - arr: array handle

   Returns:
   ArrayFire error code"
  "af_is_real" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_is_double(bool *result, const af_array arr)
(defcfn af-is-double
  "Check if an array is double precision.
   
   Parameters:
   - result: out pointer to bool (int)
   - arr: array handle

   Returns:
   ArrayFire error code"
  "af_is_double" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_is_single(bool *result, const af_array arr)
(defcfn af-is-single
  "Check if an array is single precision.
   
   Parameters:
   - result: out pointer to bool (int)
   - arr: array handle

   Returns:
   ArrayFire error code"
  "af_is_single" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_is_half(bool *result, const af_array arr)
(defcfn af-is-half
  "Check if an array is half precision.
   
   Parameters:
   - result: out pointer to bool (int)
   - arr: array handle

   Returns:
   ArrayFire error code"
  "af_is_half" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_is_realfloating(bool *result, const af_array arr)
(defcfn af-is-realfloating
  "Check if an array is real floating point.
   
   Parameters:
   - result: out pointer to bool (int)
   - arr: array handle

   Returns:
   ArrayFire error code"
  "af_is_realfloating" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_is_floating(bool *result, const af_array arr)
(defcfn af-is-floating
  "Check if an array is floating point (real or complex).
   
   Parameters:
   - result: out pointer to bool (int)
   - arr: array handle

   Returns:
   ArrayFire error code"
  "af_is_floating" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_is_integer(bool *result, const af_array arr)
(defcfn af-is-integer
  "Check if an array is integer type.
   
   Parameters:
   - result: out pointer to bool (int)
   - arr: array handle

   Returns:
   ArrayFire error code"
  "af_is_integer" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_is_bool(bool *result, const af_array arr)
(defcfn af-is-bool
  "Check if an array is boolean type.
   
   Parameters:
   - result: out pointer to bool (int)
   - arr: array handle

   Returns:
   ArrayFire error code"
  "af_is_bool" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_is_sparse(bool *result, const af_array arr)
(defcfn af-is-sparse
  "Check if an array is sparse.
   
   Parameters:
   - result: out pointer to bool (int)
   - arr: array handle

   Returns:
   ArrayFire error code"
  "af_is_sparse" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_get_scalar(void *output_value, const af_array arr)
(defcfn af-get-scalar
  "Get the first element of an array as a scalar value.
   
   Parameters:
   - output-value: out pointer for scalar value
   - arr: array handle (must be scalar)

   Returns:
   ArrayFire error code"
  "af_get_scalar" [::mem/pointer ::mem/pointer] ::mem/int)

