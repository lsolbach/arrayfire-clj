(ns org.soulspace.arrayfire.integration.array
  "Integration of the ArrayFire array related FFI bindings with the error
   handling and resource management on the JVM."
  (:require [org.soulspace.arrayfire.ffi.array :as array]
            [org.soulspace.arrayfire.integration.resource :as resource]
            [coffi.mem :as mem]))

;;;
;;; ArrayFire array integration on the JVM
;;; 

