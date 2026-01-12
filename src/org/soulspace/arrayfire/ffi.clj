(ns org.soulspace.arrayfire.ffi
  "ArrayFire FFI declarations"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;;
;; Load ArrayFire library
;;
(def ^:private lib-name
  (or (System/getenv "ARRAYFIRE_LIB") "af"))

(ffi/load-system-library lib-name)

;;
;; ArrayFire dtype constants
;; From af/defines.h enum af_dtype
;;
(def AF_DTYPE_F32 0)   ; float
(def AF_DTYPE_C32 1)   ; complex float
(def AF_DTYPE_F64 2)   ; double
(def AF_DTYPE_C64 3)   ; complex double
(def AF_DTYPE_B8  4)   ; bool
(def AF_DTYPE_S32 5)   ; int
(def AF_DTYPE_U32 6)   ; unsigned int
(def AF_DTYPE_U8  7)   ; unsigned char
(def AF_DTYPE_S64 8)   ; long long
(def AF_DTYPE_U64 9)   ; unsigned long long
(def AF_DTYPE_S16 10)  ; short
(def AF_DTYPE_U16 11)  ; unsigned short

;;
;; API declarations
;; Each function is declared separately with correct coffi syntax
;;

;; Device management functions
(defcfn af-init
  "Initialize ArrayFire runtime"
  "af_init" [] ::mem/int)

(defcfn af-info
  "Print ArrayFire device information"
  "af_info" [] ::mem/int)

;; Array management functions
;; af_err af_create_array(af_array *arr, const void *data, unsigned ndims, const dim_t *dims, af_dtype type)
(defcfn af-create-array
  "Create an ArrayFire array from host data.
   Parameters: arr (out pointer), data (in pointer), ndims (unsigned), dims (pointer to dim_t), type (int)"
  "af_create_array" [::mem/pointer ::mem/pointer ::mem/int ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_release_array(af_array arr)
(defcfn af-release-array
  "Release an ArrayFire array handle"
  "af_release_array" [::mem/pointer] ::mem/int)

;; Arithmetic operations
;; af_err af_add(af_array *out, const af_array lhs, const af_array rhs, const bool batch)
(defcfn af-add
  "Add two arrays element-wise.
   Parameters: out (out pointer), lhs (array handle), rhs (array handle), batch (bool as int)"
  "af_add" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; Data transfer functions
;; af_err af_get_data_ptr(void *data, const af_array arr)
(defcfn af-get-data-ptr
  "Copy array data to host memory.
   Parameters: data (out pointer to host buffer), arr (array handle)"
  "af_get_data_ptr" [::mem/pointer ::mem/pointer] ::mem/int)

;;
;; Helper functions
;;
(defn check!
  "Check ArrayFire error code and throw exception if non-zero"
  [rc where]
  (when (not (zero? rc))
    (throw (ex-info (str "ArrayFire error at " where) {:code rc :where where}))))

(defn dims->native
  "Convert Clojure vector of dimensions to native dim_t array.
   dim_t is typically long long (64-bit) on most platforms."
  [dims]
  (let [buf (mem/alloc (* 8 (count dims)))] ; dim_t is 64-bit
    (doseq [i (range (count dims))]
      (mem/write-long buf (* i 8) (long (nth dims i))))
    buf))