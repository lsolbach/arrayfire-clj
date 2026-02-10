(ns org.soulspace.arrayfire.integration.base.error
  "Base error handling utilities for ArrayFire integration."
  )

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
    (throw (ex-info (str "ArrayFire error at " where) {:code rc :where where}))))
