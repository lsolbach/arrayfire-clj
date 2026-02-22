(ns org.soulspace.arrayfire.api.device
  "Idiomatic Clojure API for querying ArrayFire devices and contexts.

   This namespace includes read-only functions for:
   - querying the current device and backend
   - inspecting device capabilities (double, half precision)
   - querying memory usage statistics
   - querying per-array device and backend identity
   - checking whether an array's memory is locked

   Setting the active device and backend is handled by the `with-arrayfire` macro
   in `org.soulspace.arrayfire.api.core`, which ensures that all operations
   within its scope use the specified device and backend.

   All functions in this namespace must be called within a `with-arrayfire` region.

   If you need to manage devices and contexts more directly, you can use the
   functions of the `org.soulspace.arrayfire.integration.unified-api.device`
   namespace, which provide lower-level access to device management and context
   handling."
  (:require [org.soulspace.arrayfire.integration.base.definitions :as defs]
            [org.soulspace.arrayfire.integration.unified-api.device :as device]
            [org.soulspace.arrayfire.api.core :refer [assert-within-arrayfire!]])
  (:import (org.soulspace.arrayfire.integration.base.resource AFArray)))


;;;
;;; Current device and backend
;;;

(defn current-device
  "Return the integer ID of the currently active device.

   Returns:
     Integer device ID (0-based index).

   Example:
     (af/with-arrayfire
       (dev/current-device)) ; => 0"
  []
  (assert-within-arrayfire! "current-device")
  (device/get-device))

(defn active-backend
  "Return the currently active backend as a keyword.

   Returns:
     One of `:default`, `:cpu`, `:cuda`, `:opencl`, `:oneapi`.

   Example:
     (af/with-arrayfire
       (dev/active-backend)) ; => :cpu"
  []
  (assert-within-arrayfire! "active-backend")
  (defs/backend-const->kw (device/get-active-backend)))

(defn backend-count
  "Return the number of ArrayFire backends available on this system.

   Returns:
     Integer count of available backends.

   Example:
     (af/with-arrayfire
       (dev/backend-count)) ; => 1"
  []
  (assert-within-arrayfire! "backend-count")
  (device/get-backend-count))

;;;
;;; Device capability queries
;;;

(defn double-available?
  "Return true if the specified device supports double-precision (`:f64`) floating point.

   Parameters:
     device-id — (optional) integer device ID to query; defaults to the current device.

   Returns:
     `true` if `:f64` is available, `false` otherwise.

   Example:
     (af/with-arrayfire
       (dev/double-available?))       ; current device
     (af/with-arrayfire
       (dev/double-available? 0))     ; device 0 explicitly"
  ([]
   (assert-within-arrayfire! "double-available?")
   (device/dbl-support?))
  ([device-id]
   (assert-within-arrayfire! "double-available?")
   (device/dbl-support? device-id)))

(defn half-available?
  "Return true if the specified device supports half-precision (`:f16`) floating point.

   Parameters:
     device-id — (optional) integer device ID to query; defaults to the current device.

   Returns:
     `true` if `:f16` is available, `false` otherwise.

   Example:
     (af/with-arrayfire
       (dev/half-available?))         ; current device
     (af/with-arrayfire
       (dev/half-available? 0))       ; device 0 explicitly"
  ([]
   (assert-within-arrayfire! "half-available?")
   (device/half-support?))
  ([device-id]
   (assert-within-arrayfire! "half-available?")
   (device/half-support? device-id)))

;;;
;;; Memory statistics
;;;

(defn device-mem-info
  "Return memory usage statistics from the ArrayFire memory manager.

   The returned map contains:
   - `:alloc-bytes`   — total bytes allocated by the memory manager
   - `:alloc-buffers` — number of allocated buffers
   - `:lock-bytes`    — bytes currently locked (in active use)
   - `:lock-buffers`  — number of currently locked buffers

   Returns:
     Map with the above keys.

   Example:
     (af/with-arrayfire
       (let [{:keys [alloc-bytes lock-bytes]} (dev/device-mem-info)]
         (println \"Using\" lock-bytes \"of\" alloc-bytes \"bytes\")))"
  []
  (assert-within-arrayfire! "device-mem-info")
  (device/device-mem-info))

;;;
;;; Per-array device and backend identity
;;;

(defn array-backend
  "Return the backend keyword of the backend that owns `arr`.

   Parameters:
     arr — AFArray to query.

   Returns:
     One of `:default`, `:cpu`, `:cuda`, `:opencl`, `:oneapi`.

   Example:
     (af/with-arrayfire
       (let [a (af/array [1.0 2.0] [2] :f64)]
         (dev/array-backend a))) ; => :cpu"
  [^AFArray arr]
  (assert-within-arrayfire! "array-backend")
  (defs/backend-const->kw (device/get-backend-id arr)))

(defn array-device
  "Return the integer device ID of the device that owns `arr`.

   Parameters:
     arr — AFArray to query.

   Returns:
     Integer device ID.

   Example:
     (af/with-arrayfire
       (let [a (af/array [1.0 2.0] [2] :f64)]
         (dev/array-device a))) ; => 0"
  [^AFArray arr]
  (assert-within-arrayfire! "array-device")
  (device/get-device-id arr))

;;;
;;; Array lock state
;;;

(defn locked-array?
  "Return true if the memory of `arr` is currently locked by the memory manager.

   Locked arrays have their memory buffers reserved and will not be reclaimed
   or reused until explicitly unlocked. Locking is an advanced technique used
   to keep an array's buffer valid while sharing it with external libraries.

   Parameters:
     arr — AFArray to query.

   Returns:
     Boolean.

   Example:
     (af/with-arrayfire
       (let [a (af/array [1.0 2.0] [2] :f64)]
         (dev/locked-array? a))) ; => false"
  [^AFArray arr]
  (assert-within-arrayfire! "locked-array?")
  (device/locked-array? arr))

(comment
  ;; REPL exploration
  (af/with-arrayfire
    {:current-device  (dev/current-device)
     :active-backend  (dev/active-backend)
     :backend-count   (dev/backend-count)
     :double?         (dev/double-available?)
     :half?           (dev/half-available?)
     :mem-info        (dev/device-mem-info)})

  (af/with-arrayfire
    (let [a (af/array [1.0 2.0 3.0] [3] :f64)]
      {:backend (dev/array-backend a)
       :device  (dev/array-device a)
       :locked? (dev/locked-array? a)}))
  
  ;
  )
