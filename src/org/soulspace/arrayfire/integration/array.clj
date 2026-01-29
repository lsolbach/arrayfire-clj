(ns org.soulspace.arrayfire.integration.array
  "Integration of the ArrayFire array related FFI bindings with the error
   handling and resource management on the JVM."
  (:require [org.soulspace.arrayfire.ffi.array :as array]
            [org.soulspace.arrayfire.integration.jvm-integration :as int]
            [coffi.mem :as mem])
  (:import (org.soulspace.arrayfire.integration.jvm_integration AFArray)))

(defn floats?
  "Check if data is a float array or a collection of floats."
  [data]
  (or (instance? (Class/forName "[F") data)
      (and (coll? data)
           (every? float? data))))

(defn ints?
  "Check if data is an int array or a collection of ints."
  [data]
  (or (instance? (Class/forName "[I") data)
      (and (coll? data)
           (every? int? data))))

(defn short?
  "Check if x is a short."
  [x]
  (and (integer? x)
       (<= Short/MIN_VALUE x Short/MAX_VALUE)))

(defn shorts?
  "Check if data is a short array or a collection of shorts."
  [data]
  (or (instance? (Class/forName "[S") data)
      (and (coll? data)
           (every? short? data))))

;;;
;;; ArrayFire array integration on the JVM
;;; 
; TODO: generalize data handling and add type specific cases in other functions
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
