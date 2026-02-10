(ns org.soulspace.arrayfire.ffi.base.loader
  "ArrayFire library loader.
   
   This namespace is responsible for loading the ArrayFire native library.
   It must be required before any other FFI namespace to ensure the library
   is loaded before any FFI bindings (defcfn) are created.
   
   This is the foundational namespace of the FFI layer with no dependencies
   on other arrayfire-clj namespaces."
  (:require [coffi.ffi :as ffi]))

;;;
;;; Load ArrayFire library
;;;

(def ^:private lib-name
  "Name of the ArrayFire library to load.
   Can be overridden via ARRAYFIRE_LIB environment variable."
  (or (System/getenv "ARRAYFIRE_LIB") "af"))

;; Load the library immediately when this namespace is loaded
(ffi/load-system-library lib-name)

;; Export lib-name for reference
(def library-name
  "The name of the loaded ArrayFire library."
  lib-name)
