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

;;
;; Type-specific memory operations
;;

(defn write-float!
  "Write a float value to buffer at offset"
  [buf offset value]
  (mem/write-float buf offset (float value)))

(defn read-float
  "Read a float value from buffer at offset"
  [buf offset]
  (mem/read-float buf offset))

(defn write-double!
  "Write a double value to buffer at offset"
  [buf offset value]
  (mem/write-double buf offset (double value)))

(defn read-double
  "Read a double value from buffer at offset"
  [buf offset]
  (mem/read-double buf offset))

(defn write-int!
  "Write an int value to buffer at offset"
  [buf offset value]
  (mem/write-int buf offset (int value)))

(defn read-int
  "Read an int value from buffer at offset"
  [buf offset]
  (mem/read-int buf offset))

(defn write-long!
  "Write a long value to buffer at offset"
  [buf offset value]
  (mem/write-long buf offset (long value)))

(defn read-long
  "Read a long value from buffer at offset"
  [buf offset]
  (mem/read-long buf offset))

(defn write-short!
  "Write a short value to buffer at offset"
  [buf offset value]
  (mem/write-short buf offset (short value)))

(defn read-short
  "Read a short value from buffer at offset"
  [buf offset]
  (mem/read-short buf offset))

(defn write-byte!
  "Write a byte value to buffer at offset"
  [buf offset value]
  (mem/write-byte buf offset (byte value)))

(defn read-byte
  "Read a byte value from buffer at offset"
  [buf offset]
  (mem/read-byte buf offset))

;; Complex number operations
;; Complex numbers are represented as [real imag] vectors
;; In memory, they're stored as consecutive real/imag pairs

(defn write-complex-float!
  "Write a complex float to buffer at offset.
   value is [real imag] vector"
  [buf offset [real imag]]
  (mem/write-float buf offset (float real))
  (mem/write-float buf (+ offset 4) (float imag)))

(defn read-complex-float
  "Read a complex float from buffer at offset.
   Returns [real imag] vector"
  [buf offset]
  [(mem/read-float buf offset)
   (mem/read-float buf (+ offset 4))])

(defn write-complex-double!
  "Write a complex double to buffer at offset.
   value is [real imag] vector"
  [buf offset [real imag]]
  (mem/write-double buf offset (double real))
  (mem/write-double buf (+ offset 8) (double imag)))

(defn read-complex-double
  "Read a complex double from buffer at offset.
   Returns [real imag] vector"
  [buf offset]
  [(mem/read-double buf offset)
   (mem/read-double buf (+ offset 8))])

;; Type size lookup
(def type-sizes
  "Size in bytes for each ArrayFire dtype"
  {AF_DTYPE_F32 4    ; float
   AF_DTYPE_C32 8    ; complex float (2 floats)
   AF_DTYPE_F64 8    ; double
   AF_DTYPE_C64 16   ; complex double (2 doubles)
   AF_DTYPE_B8  1    ; bool
   AF_DTYPE_S32 4    ; int
   AF_DTYPE_U32 4    ; unsigned int
   AF_DTYPE_U8  1    ; unsigned char
   AF_DTYPE_S64 8    ; long long
   AF_DTYPE_U64 8    ; unsigned long long
   AF_DTYPE_S16 2    ; short
   AF_DTYPE_U16 2})  ; unsigned short