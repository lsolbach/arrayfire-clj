(ns org.soulspace.arrayfire.ffi.c-api.clamp
  "Bindings for ArrayFire clamp function.
   
   Maps to src/api/c/clamp.cpp in ArrayFire."
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.loader]))

;; af_err af_clamp(af_array *out, const af_array in, const af_array lo, const af_array hi, const bool batch)
(defcfn af-clamp
  "Clamp an array between lower and upper limits.
   
   For each element in the input array, this function returns:
   - lo[i] if in[i] < lo[i]
   - hi[i] if in[i] > hi[i]
   - in[i] otherwise
   
   Effectively: out[i] = max(lo[i], min(in[i], hi[i]))
   
   The clamp operation constrains array elements to lie within a specified range,
   useful for:
   - Image processing (e.g., constraining pixel values to valid ranges)
   - Signal processing (limiting amplitude)
   - Machine learning (gradient clipping)
   - Physics simulations (enforcing physical constraints)
   
   All three arrays (in, lo, hi) can be:
   - Full arrays (same size)
   - Scalar arrays (single element, broadcasted)
   - Different sizes when batch mode is enabled
   
   Requirements:
   - lo and hi arrays must have the same dimensions
   - lo and hi arrays must have the same type
   - Output type is determined by implicit type promotion between in and lo/hi
   
   Batch mode:
   - When batch=false: All arrays must have compatible dimensions (same or broadcast)
   - When batch=true: Allows different batch dimensions for broadcasting
   
   Supported types: All numeric types (f16, f32, f64, c32, c64, s8, u8, s16, u16,
                    s32, u32, s64, u64, b8)
   
   Parameters:
   - out: out pointer for clamped array
   - in: array handle for input values to clamp
   - lo: array handle for lower limit values
   - hi: array handle for upper limit values
   - batch: bool as int (batch mode for broadcasting)

   Returns:
   ArrayFire error code (AF_SUCCESS on success)"
  "af_clamp" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)
