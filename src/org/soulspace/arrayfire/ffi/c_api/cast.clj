(ns org.soulspace.arrayfire.ffi.c-api.cast
  "Bindings for ArrayFire type casting function.
   
   Maps to src/api/c/cast.cpp in ArrayFire."
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; af_err af_cast(af_array *out, const af_array in, const af_dtype type)
(defcfn af-cast
  "Cast an array from one type to another.
   
   Converts array elements from one data type to another. The function performs
   element-wise type conversion and creates a new array with the target type.
   
   Important: Casting from complex (c32/c64) to real (f16/f32/f64) is not allowed.
   Use abs, real, imag, etc. to convert complex to real types instead.
   
   Supported types:
   - f32 (0): 32-bit floating point
   - c32 (1): 32-bit complex floating point
   - f64 (2): 64-bit floating point
   - c64 (3): 64-bit complex floating point
   - b8 (4): 8-bit boolean
   - s32 (5): 32-bit signed integer
   - u32 (6): 32-bit unsigned integer
   - u8 (7): 8-bit unsigned integer
   - s64 (8): 64-bit signed integer
   - u64 (9): 64-bit unsigned integer
   - s16 (10): 16-bit signed integer
   - u16 (11): 16-bit unsigned integer
   - s8 (12): 8-bit signed integer
   - f16 (13): 16-bit floating point (half precision)
   
   For sparse arrays, only floating point types (f32, f64, c32, c64) are supported.
   
   Performance note: The function optimizes sequential casts by eliminating
   redundant conversions during JIT compilation.
   
   Parameters:
   - out: out pointer for new casted array
   - in: array handle to cast
   - type: target data type (af_dtype enum value)

   Returns:
   ArrayFire error code (AF_SUCCESS on success)"
  "af_cast" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)
