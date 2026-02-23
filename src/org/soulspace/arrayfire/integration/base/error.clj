(ns org.soulspace.arrayfire.integration.base.error
  "Base error handling utilities for ArrayFire integration."
  (:require [org.soulspace.arrayfire.ffi.c-api.error :as c-error]
            [org.soulspace.arrayfire.integration.base.memory :as memory]))

;;;
;;; Error handling
;;;
(defn check!
  "Check ArrayFire error code and throw exception if non-zero.
   
   Parameters:
   - rc: return code from ArrayFire function
   - where: string indicating where the error occurred
   
   Throws an exception with error code and location if rc is non-zero."
  [rc where]
  (when-not (zero? rc)
    (let [msg (memory/c-string->string (c-error/af-err-to-string (int rc)))]
      (throw (ex-info (str "ArrayFire error at " where ": " msg)
                      {:code rc :where where :message msg})))))
