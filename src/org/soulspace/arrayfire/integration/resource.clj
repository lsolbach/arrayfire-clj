(ns org.soulspace.arrayfire.integration.resource
  "Resource management for ArrayFire resources on the JVM.
   
   ArrayFire uses reference counting for its array resources (af_array).
   This module provides wrappers to manage the lifecycle of these resources
   by integrating with AutoCloseable and Java's Cleaner mechanism to ensure
   proper release of resources when they are no longer needed.
   
   ArrayFire provides reference counting for its resources (e.g. af_array)
   and functions for the creation and release of these resources. 
   
   This namespace defines the AFArray type which encapsulates an af_array
   handle along with automatic cleanup. It relies on the reference counting
   functions provided by ArrayFire to manage the lifecycle of the resources
   and does not introduce additional reference counting."
  (:require [tech.v3.resource :as tr]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.array :refer [af-release-array af-retain-array]])
  (:import [java.lang AutoCloseable]
           [java.lang.ref Cleaner Cleaner$Cleanable]
           [java.util.concurrent.atomic AtomicBoolean]
           [java.lang.foreign Arena MemorySegment ValueLayout]))


;;;
;;; AFArray resource management
;;;
(defn af-release-array!
  "Release one reference to an af_array.
   
   Parameters:
   - handle: af_array* handle
   
   Returns: nil"
  [^long handle]
  (let [seg (MemorySegment/ofAddress handle 0 mem/global-arena)
        err (af-release-array seg)]
    (when-not (zero? err)
      (throw (ex-info "af_release_array failed"
                      {:error-code err
                       :handle handle})))
    nil))

(defn af-retain-array!
  "Increment the refcount of an af_array.
   
   Parameters:
   - handle: af_array* handle
   
   Returns: nil"
  [^long handle]
  (let [arena (Arena/ofConfined)]
    (try
      (let [out (MemorySegment/allocateNative ValueLayout/ADDRESS arena)
            in  (MemorySegment/ofAddress handle 0 arena)
            err (af-retain-array out in)]
        (when-not (zero? err)
          (throw (ex-info "af_retain_array failed"
                          {:error-code err
                           :handle handle})))
        ;; IMPORTANT:
        ;; Intentionally discard `out`
        ;; af_retain_array returns a *new handle* in `out`,
        ;; but for refcounting purposes we DO NOT replace the handle.
        ;; ArrayFire treats both as equivalent aliases.
        nil)
      (finally
        (.close arena)))))

(def ^Cleaner cleaner
  "A Cleaner instance for cleaning up ArrayFire resources."
  (Cleaner/create))

(deftype AFArrayCleanup [^long handle ^AtomicBoolean released]
  Runnable
  (run [_]
    ;; Ensure exactly-once release
    (when (.compareAndSet released false true)
      (af-release-array! handle))))

(deftype AFArray
         [^long handle                      ;; native af_array*
          ^AtomicBoolean released
          ^Cleaner$Cleanable cleanable]

  AutoCloseable
  (close [_]
    ;; Deterministic release
    (.clean cleanable))

  Object
  (toString [_]
    (if (.get released)
      "#<AFArray CLOSED>"
      (str "#<AFArray 0x" (Long/toHexString handle) ">"))))

(defn af-array-new
  "Wrap an af_array returned from ArrayFire (refcount = 1).
   
   Parameters:
   - handle: af_array* handle
   
   Returns:
   AFArray instance"
  ^AFArray
  [^long handle]
  (let [released (AtomicBoolean. false)
        cleanup  (AFArrayCleanup. handle released)
        cleanable (.register cleaner cleanup)]
    (AFArray. handle released cleanable)))

(defn af-array-retained
  "Wrap an existing af_array; retains before wrapping.
   
   Parameters:
   - handle: af_array* handle
   
   Returns:
   AFArray instance"
  ^AFArray
  [^long handle]
  (af-retain-array! handle)
  (af-array-new handle))

(defn af-handle
  "Get the native af_array* handle from AFArray.
   
   Parameters:
   - arr: AFArray instance
   
   Returns:
   af_array* handle as long"
  ^long [^AFArray arr]
  (when (.get ^AtomicBoolean (.-released arr))
    (throw (IllegalStateException.
            "AFArray has already been closed")))
  (.-handle arr))

