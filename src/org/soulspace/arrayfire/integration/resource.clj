(ns org.soulspace.arrayfire.integration.resource
  "Resource management for ArrayFire objects using Tech Resource."
  (:require [tech.v3.resource :as tr]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.array :refer [af-release-array af-retain-array]])
  (:import [java.lang AutoCloseable]
           [java.lang.ref Cleaner Cleaner$Cleanable]
           [java.util.concurrent.atomic AtomicBoolean]
           [java.lang.foreign Arena MemorySegment ValueLayout]))


(defn af-release-array!
  "Release one reference to an af_array."
  [^long handle]
  (let [seg (MemorySegment/ofAddress handle 0 mem/global-arena)
        err (af-release-array seg)]
    (when-not (zero? err)
      (throw (ex-info "af_release_array failed"
                      {:error-code err
                       :handle handle})))
    nil))

(defn af-retain-array!
  "Increment the refcount of an af_array."
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
  "Wrap an af_array returned from ArrayFire (refcount = 1)."
  ^AFArray
  [^long handle]
  (let [released (AtomicBoolean. false)
        cleanup  (AFArrayCleanup. handle released)
        cleanable (.register cleaner cleanup)]
    (AFArray. handle released cleanable)))

(defn af-array-retained
  "Wrap an existing af_array; retains before wrapping."
  ^AFArray
  [^long handle]
  (af-retain-array! handle)
  (af-array-new handle))

(defn af-handle
  ^long [^AFArray arr]
  (when (.get ^AtomicBoolean (.-released arr))
    (throw (IllegalStateException.
            "AFArray has already been closed")))
  (.-handle arr))

