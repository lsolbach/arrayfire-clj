(ns org.soulspace.arrayfire.integration.base.resource
  "This namespace contains the resource management for ArrayFire resources.
   
   ArrayFire uses reference counting for its array resources (af_array) and
   provides functions for the creation and release of these resources.
   This module provides wrappers to manage the lifecycle of these resources
   by integrating with AutoCloseable and Java's Cleaner mechanism to ensure
   proper release of resources when they are no longer needed.
      
   This namespace defines the AFArray type which encapsulates an af_array
   handle along with automatic cleanup. It relies on the reference counting
   functions provided by ArrayFire to manage the lifecycle of the resources
   and does not introduce additional reference counting.
   
   The AFArray type implements AutoCloseable, allowing users to explicitly
   release resources when done. Additionally, it uses Java's Cleaner to ensure
   that resources are released when the AFArray instance is garbage collected,
   preventing memory leaks."
  (:require [coffi.mem :as mem]
            [tech.v3.resource :as resource]
            [org.soulspace.arrayfire.ffi.c-api.array :refer [af-release-array af-retain-array]]
            [org.soulspace.arrayfire.integration.base.error :refer [check!]])
  (:import [java.lang AutoCloseable]
           [java.lang.ref Cleaner Cleaner$Cleanable]
           [java.util.concurrent.atomic AtomicBoolean]
           [java.lang.foreign Arena MemorySegment ValueLayout]))

;;;
;;; AFArray resource management
;;;
(defn native-af-array-pointer
  "Allocate an af_array* for use as an out-parameter.
   Must be used within a dynamic Arena."
  ^MemorySegment
  ([]
   ;; One pointer-sized slot
   (.allocate (Arena/ofAuto) ValueLayout/ADDRESS))
  ([^Arena arena]
   ;; One pointer-sized slot
   (.allocate arena ValueLayout/ADDRESS)))

(defn deref-af-array
  "Read an af_array value from an af_array* out-parameter
   and return it as a raw address (long)."
  ^long
  [^MemorySegment af-array-ptr]
  ;; Read the pointer stored at offset 0
  (let [addr (mem/read-address af-array-ptr)]
    (if (instance? MemorySegment addr)
      (mem/address-of addr)
      (long addr))))

(defn address->long
  "Normalize a MemorySegment or numeric address into a long value."
  ^long
  [addr]
  (cond
    (instance? MemorySegment addr) (mem/address-of addr)
    (number? addr) (long addr)
    :else (throw (IllegalArgumentException.
                  (str "Unsupported address type: " (type addr))))))

(defn af-release-array!
  "Release one reference to an af_array.
   
   Parameters:
   - handle: af_array* handle
   
   Returns: nil"
  [^long handle]
  (let [seg (MemorySegment/ofAddress handle)]
    (check! (af-release-array seg) "af_release_array")
    nil))

(defn af-retain-array!
  "Increment the refcount of an af_array.
   
   Parameters:
   - handle: af_array* handle
   
   Returns: nil"
  [^long handle]
  (let [arena (Arena/ofConfined)]
    (try
      (let [out (.allocate arena ValueLayout/ADDRESS)
            in  (MemorySegment/ofAddress handle)]
        (check! (af-retain-array out in) "af_retain_array")
        ;; IMPORTANT:
        ;; Intentionally discarded `out`
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
          ^Cleaner$Cleanable cleanable
          ^Object cleanup-key]

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
  (when (zero? handle)
    (throw (IllegalStateException. "Invalid AF handle")))
  (let [released (AtomicBoolean. false)
        cleanup  (AFArrayCleanup. handle released)
        cleanup-key (Object.)
        cleanable (.register cleaner cleanup-key cleanup)
        array (AFArray. handle released cleanable cleanup-key)]
    (resource/track array {:track-type :stack})
    array))

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
   af_array* handle as MemorySegment"
  ^MemorySegment [arr]
  (let [released (clojure.lang.Reflector/getInstanceField arr "released")
        handle   (clojure.lang.Reflector/getInstanceField arr "handle")
        handle-value (long handle)]
    (when (and released (.get ^AtomicBoolean released))
      (throw (IllegalStateException.
              "AFArray has already been closed")))
    (MemorySegment/ofAddress handle-value)))

(defn af-handle-value
  "Get the native af_array* handle from AFArray.
   
   Parameters:
   - arr: AFArray instance
   
   Returns:
   af_array* handle as long"
  ^long [arr]
  (mem/address-of (af-handle arr)))

(defn duplicate-array
  "Create a new AFArray with independent lifetime.
   
   Parameters:
   - a: AFArray to duplicate
   
   Returns:
   New AFArray instance with its own reference count"
  [^AFArray a]
  (af-array-retained (af-handle a)))

