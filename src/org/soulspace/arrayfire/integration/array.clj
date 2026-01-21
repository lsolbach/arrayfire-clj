(ns org.soulspace.arrayfire.integration.array
  "Integration of the ArrayFire array related FFI bindings with the error
   handling and resource management on the JVM."
  (:require [org.soulspace.arrayfire.ffi.array :as array]
            [org.soulspace.arrayfire.integration.jvm-integration :as int]
            [coffi.mem :as mem])
  (:import (org.soulspace.arrayfire.integration.jvm_integration AFArray)))

;;;
;;; ArrayFire array integration on the JVM
;;; 
(defn create-array
  "Create a new ArrayFire array from host data.
   Owns exactly one af_array reference."
  ^AFArray
  [data dims dtype]
  (let [out      (int/native-af-array-pointer)
        data-seg (cond
                   (floats? data) (int/float-array->segment data)
                   ;; add other primitive cases here
                   :else (throw (IllegalArgumentException.
                                 "Unsupported data type")))
        dims-seg (int/dims->segment dims)]
    (int/check!
     (array/af-create-array out data-seg (count dims)
      dims-seg dtype)
     "af-create-array")
    ;; af_create_array returns a NEW array (refcount = 1)
    (int/af-array-new (int/deref-af-array out))))
