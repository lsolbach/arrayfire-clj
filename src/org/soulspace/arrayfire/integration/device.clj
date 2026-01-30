(ns org.soulspace.arrayfire.integration.device
  "Integration of the ArrayFire device management related FFI bindings with the error
   handling and resource management on the JVM."
  (:require [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.device :as device-ffi]
            [org.soulspace.arrayfire.ffi.memory :as memory-ffi]
            [org.soulspace.arrayfire.integration.jvm-integration :as jvm])
  (:import (org.soulspace.arrayfire.integration.jvm_integration AFArray)
           (java.lang.foreign Arena MemorySegment)))

;;;
;;; ArrayFire Initialization
;;;

(defn init!
  "Initialize the ArrayFire runtime.
   
   This function should be called before using any other ArrayFire functions.
   It initializes the runtime and selects an appropriate backend.
   
   Returns:
   nil
   
   Example:
   (init!)"
  []
  (jvm/check! (device-ffi/af-init) "af-init")
  nil)

(defn info
  "Print ArrayFire device information to standard output.
   
   Displays information about the current device including name, platform,
   toolkit version, and compute capabilities.
   
   Returns:
   nil
   
   Example:
   (info)"
  []
  (jvm/check! (device-ffi/af-info) "af-info")
  nil)

(defn info-string
  "Get ArrayFire device information as a string.
   
   Parameters:
   - verbose: Boolean, if true provides detailed information (default false)
   
   Returns:
   String containing device information
   
   Example:
   (let [info-str (info-string true)]
     (println info-str))"
  ([]
   (info-string false))
  ([verbose]
   (let [arena (Arena/ofConfined)]
     (try
       (let [str-ptr-buf (mem/alloc 8)] ; Buffer to hold char** pointer
         (jvm/check! (device-ffi/af-info-string str-ptr-buf (if verbose 1 0))
                     "af-info-string")
         ;; Read the char* pointer from the buffer
         (let [str-address (mem/read-long str-ptr-buf 0)
               str-segment (MemorySegment/ofAddress str-address)]
           (jvm/c-string->string str-segment)))
       (finally
         (.close arena))))))

;;;
;;; Device Management
;;;

(defn get-device-count
  "Get the number of compute devices available in the system.
   
   Returns:
   Integer representing the number of devices
   
   Example:
   (let [count (get-device-count)]
     (println \"Found\" count \"devices\"))"
  []
  (let [count-buf (mem/alloc 4)]
    (jvm/check! (device-ffi/af-get-device-count count-buf)
                "af-get-device-count")
    (mem/read-int count-buf 0)))

(defn get-device
  "Get the ID of the currently active device.
   
   Returns:
   Integer representing the current device ID
   
   Example:
   (let [device-id (get-device)]
     (println \"Active device:\" device-id))"
  []
  (let [device-buf (mem/alloc 4)]
    (jvm/check! (device-ffi/af-get-device device-buf)
                "af-get-device")
    (mem/read-int device-buf 0)))

(defn set-device!
  "Set the active device for ArrayFire operations.
   
   All subsequent ArrayFire operations will use the specified device
   until changed again or until the current thread exits.
   
   Parameters:
   - device-id: Integer device ID (0 to device-count - 1)
   
   Returns:
   nil
   
   Example:
   (set-device! 0) ; Set device 0 as active"
  [device-id]
  (jvm/check! (device-ffi/af-set-device (int device-id))
              "af-set-device")
  nil)

(defn device-info
  "Get detailed information about a device.
   
   Parameters:
   - device-id: Optional device ID to query (default: current device)
   
   Returns:
   Map with :device-id, :name, :platform, :toolkit, and :compute keys
   
   Example:
   (device-info 0)
   ;; => {:device-id 0 :name "..." :platform "..." :toolkit "..." :compute "..."}"
  ([]
   (device-info (get-device)))
  ([device-id]
   ;; Switch to device to query
   (let [current-device (get-device)]
     (when (not= current-device device-id)
       (set-device! device-id))
     (try
       (let [arena (Arena/ofConfined)]
         (try
           ;; Allocate buffers for the output strings (256 bytes each should be enough)
           (let [name-buf (mem/alloc 256 arena)
                 platform-buf (mem/alloc 256 arena)
                 toolkit-buf (mem/alloc 256 arena)
                 compute-buf (mem/alloc 256 arena)]
             (jvm/check! (device-ffi/af-device-info name-buf platform-buf toolkit-buf compute-buf)
                         "af-device-info")
             {:device-id device-id
              :name (jvm/c-string->string name-buf)
              :platform (jvm/c-string->string platform-buf)
              :toolkit (jvm/c-string->string toolkit-buf)
              :compute (jvm/c-string->string compute-buf)})
           (finally
             (.close arena))))
       (finally
         (when (not= current-device device-id)
           (set-device! current-device)))))))

;;;
;;; Device Capabilities
;;;

(defn dbl-support?
  "Check if a device supports double precision floating point.
   
   Parameters:
   - device-id: Device ID to check (default: current device)
   
   Returns:
   Boolean, true if double precision is supported
   
   Example:
   (if (dbl-support? 0)
     (println \"Device supports double precision\")
     (println \"Device does NOT support double precision\"))"
  ([]
   (dbl-support? (get-device)))
  ([device-id]
   (let [available-buf (mem/alloc 4)]
     (jvm/check! (device-ffi/af-get-dbl-support available-buf (int device-id))
                 "af-get-dbl-support")
     (not (zero? (mem/read-int available-buf 0))))))

(defn half-support?
  "Check if a device supports half precision (FP16) floating point.
   
   Parameters:
   - device-id: Device ID to check (default: current device)
   
   Returns:
   Boolean, true if half precision is supported
   
   Example:
   (if (half-support? 0)
     (println \"Device supports half precision\")
     (println \"Device does NOT support half precision\"))"
  ([]
   (half-support? (get-device)))
  ([device-id]
   (let [available-buf (mem/alloc 4)]
     (jvm/check! (device-ffi/af-get-half-support available-buf (int device-id))
                 "af-get-half-support")
     (not (zero? (mem/read-int available-buf 0))))))

;;;
;;; Synchronization
;;;

(defn sync!
  "Block until all operations on the specified device are complete.
   
   This is important for accurate timing and ensuring operations complete
   before accessing results, especially on asynchronous backends like CUDA.
   
   Parameters:
   - device-id: Device ID to synchronize, or -1 for all devices (default: current device)
   
   Returns:
   nil
   
   Example:
   (sync! -1) ; Synchronize all devices
   (sync! 0)  ; Synchronize device 0"
  ([]
   (sync! (get-device)))
  ([device-id]
   (jvm/check! (device-ffi/af-sync (int device-id))
               "af-sync")
   nil))

;;;
;;; Memory Management
;;;

(defn device-mem-info
  "Get memory usage statistics from the ArrayFire memory manager.
   
   Returns:
   Map with keys:
   - :alloc-bytes - Total bytes allocated by the memory manager
   - :alloc-buffers - Number of buffers allocated
   - :lock-bytes - Bytes currently locked (in use)
   - :lock-buffers - Number of buffers currently locked
   
   Example:
   (let [{:keys [alloc-bytes lock-bytes]} (device-mem-info)]
     (println \"Using\" lock-bytes \"of\" alloc-bytes \"bytes\"))"
  []
  (let [alloc-bytes-buf (mem/alloc 8)
        alloc-buffers-buf (mem/alloc 8)
        lock-bytes-buf (mem/alloc 8)
        lock-buffers-buf (mem/alloc 8)]
    (jvm/check! (memory-ffi/af-device-mem-info alloc-bytes-buf alloc-buffers-buf
                                               lock-bytes-buf lock-buffers-buf)
                "af-device-mem-info")
    {:alloc-bytes (mem/read-long alloc-bytes-buf 0)
     :alloc-buffers (mem/read-long alloc-buffers-buf 0)
     :lock-bytes (mem/read-long lock-bytes-buf 0)
     :lock-buffers (mem/read-long lock-buffers-buf 0)}))

(defn print-mem-info!
  "Print detailed memory manager information to standard output.
   
   Displays a table showing all allocated buffers with their addresses,
   sizes, and lock status. Useful for debugging memory issues.
   
   Parameters:
   - msg: Optional message to print before the table (default: nil)
   - device-id: Device ID to query, or -1 for active device (default: -1)
   
   Returns:
   nil
   
   Example:
   (print-mem-info! \"Before allocation\" 0)"
  ([]
   (print-mem-info! nil -1))
  ([msg]
   (print-mem-info! msg -1))
  ([msg device-id]
   (let [arena (Arena/ofConfined)]
     (try
       (let [msg-segment (if msg
                           (jvm/string->c-string msg arena)
                           mem/null)]
         (jvm/check! (memory-ffi/af-print-mem-info msg-segment (int device-id))
                     "af-print-mem-info")
         nil)
       (finally
         (.close arena))))))

(defn device-gc!
  "Run garbage collection on the device memory manager.
   
   Forces cleanup of unused memory buffers. This can be useful to free
   memory that is no longer needed but hasn't been automatically released yet.
   
   Returns:
   nil
   
   Example:
   (device-gc!) ; Free unused device memory"
  []
  (jvm/check! (memory-ffi/af-device-gc) "af-device-gc")
  nil)

(defn set-mem-step-size!
  "Set the memory chunk size for the default memory manager.
   
   The memory manager allocates memory in chunks of at least this size
   to reduce allocation overhead. Only works with the default memory manager.
   
   Parameters:
   - step-bytes: Minimum chunk size in bytes
   
   Returns:
   nil
   
   Example:
   (set-mem-step-size! (* 1024 1024)) ; 1MB chunks"
  [step-bytes]
  (jvm/check! (memory-ffi/af-set-mem-step-size (long step-bytes))
              "af-set-mem-step-size")
  nil)

(defn get-mem-step-size
  "Get the current memory chunk size for the default memory manager.
   
   Returns:
   Long integer representing the chunk size in bytes
   
   Example:
   (let [step-size (get-mem-step-size)]
     (println \"Memory step size:\" step-size \"bytes\"))"
  []
  (let [step-buf (mem/alloc 8)]
    (jvm/check! (memory-ffi/af-get-mem-step-size step-buf)
                "af-get-mem-step-size")
    (mem/read-long step-buf 0)))

;;;
;;; Array Evaluation Control
;;;

(defn eval-array!
  "Evaluate any pending lazy operations on an array.
   
   Forces computation of an array that may have been constructed lazily.
   This is important on asynchronous backends like CUDA to ensure the
   array is ready before accessing its data.
   
   Parameters:
   - arr: AFArray to evaluate
   
   Returns:
   nil
   
   Example:
   (let [a (create-array data dims dtype)]
     (eval-array! a)
     ;; Now safe to access data"
  [^AFArray arr]
  (jvm/check! (device-ffi/af-eval (jvm/af-handle arr))
              "af-eval")
  nil)

(defn eval-multiple!
  "Evaluate multiple arrays at once for better performance.
   
   This is more efficient than calling eval-array! on each array individually
   as it allows the JIT compiler to optimize across all arrays.
   
   Parameters:
   - arrays: Collection of AFArray instances to evaluate
   
   Returns:
   nil
   
   Example:
   (let [a (create-array data1 dims dtype)
         b (create-array data2 dims dtype)]
     (eval-multiple! [a b]))"
  [arrays]
  (let [n (count arrays)
        ;; Create array of array handles
        handles-buf (mem/alloc (* n 8))] ; 8 bytes per pointer
    (doseq [[i arr] (map-indexed vector arrays)]
      (mem/write-long handles-buf (* i 8) (jvm/af-handle arr)))
    (jvm/check! (device-ffi/af-eval-multiple n handles-buf)
                "af-eval-multiple")
    nil))

;;;
;;; Backend Management
;;;

;; Backend constants
(def AF_BACKEND_DEFAULT 0)
(def AF_BACKEND_CPU 1)
(def AF_BACKEND_CUDA 2)
(def AF_BACKEND_OPENCL 3)

(defn set-backend!
  "Set the active backend for ArrayFire operations.
   
   Parameters:
   - backend: Backend constant (AF_BACKEND_DEFAULT, AF_BACKEND_CPU, 
              AF_BACKEND_CUDA, or AF_BACKEND_OPENCL)
   
   Returns:
   nil
   
   Example:
   (set-backend! AF_BACKEND_CUDA)"
  [backend]
  (jvm/check! (device-ffi/af-set-backend (int backend))
              "af-set-backend")
  nil)

(defn get-backend-count
  "Get the number of backends available on this system.
   
   Returns:
   Integer representing the number of available backends
   
   Example:
   (let [count (get-backend-count)]
     (println \"Available backends:\" count))"
  []
  (let [count-buf (mem/alloc 4)]
    (jvm/check! (device-ffi/af-get-backend-count count-buf)
                "af-get-backend-count")
    (mem/read-int count-buf 0)))

(defn get-available-backends
  "Get a bitmask of available backends.
   
   The returned integer is a bitmask where each bit represents a backend:
   - Bit 0: Default backend
   - Bit 1: CPU backend
   - Bit 2: CUDA backend
   - Bit 3: OpenCL backend
   
   Returns:
   Integer bitmask
   
   Example:
   (let [mask (get-available-backends)]
     (when (pos? (bit-and mask (bit-shift-left 1 AF_BACKEND_CUDA)))
       (println \"CUDA backend available\")))"
  []
  (let [result-buf (mem/alloc 4)]
    (jvm/check! (device-ffi/af-get-available-backends result-buf)
                "af-get-available-backends")
    (mem/read-int result-buf 0)))

(defn get-active-backend
  "Get the currently active backend.
   
   Returns:
   Integer backend constant (AF_BACKEND_CPU, AF_BACKEND_CUDA, or AF_BACKEND_OPENCL)
   
   Example:
   (let [backend (get-active-backend)]
     (case backend
       1 (println \"CPU backend active\")
       2 (println \"CUDA backend active\")
       3 (println \"OpenCL backend active\")))"
  []
  (let [result-buf (mem/alloc 4)]
    (jvm/check! (device-ffi/af-get-active-backend result-buf)
                "af-get-active-backend")
    (mem/read-int result-buf 0)))

(defn get-backend-id
  "Get the backend ID of an array.
   
   Parameters:
   - arr: AFArray to query
   
   Returns:
   Integer backend constant
   
   Example:
   (let [backend-id (get-backend-id my-array)]
     (println \"Array is on backend\" backend-id))"
  [^AFArray arr]
  (let [result-buf (mem/alloc 4)]
    (jvm/check! (device-ffi/af-get-backend-id result-buf (jvm/af-handle arr))
                "af-get-backend-id")
    (mem/read-int result-buf 0)))

(defn get-device-id
  "Get the device ID of an array.
   
   Parameters:
   - arr: AFArray to query
   
   Returns:
   Integer device ID
   
   Example:
   (let [device-id (get-device-id my-array)]
     (println \"Array is on device\" device-id))"
  [^AFArray arr]
  (let [device-buf (mem/alloc 4)]
    (jvm/check! (device-ffi/af-get-device-id device-buf (jvm/af-handle arr))
                "af-get-device-id")
    (mem/read-int device-buf 0)))

;;;
;;; Array Locking
;;;

(defn lock-array!
  "Lock an array to prevent its memory from being reused.
   
   Locking an array prevents the memory manager from reusing its memory
   buffer for other arrays. This is useful when you need to ensure an
   array's data remains valid for an extended period.
   
   Parameters:
   - arr: AFArray to lock
   
   Returns:
   nil
   
   Example:
   (lock-array! my-array)
   ;; Array memory is now locked
   (unlock-array! my-array) ; Don't forget to unlock!"
  [^AFArray arr]
  (jvm/check! (device-ffi/af-lock-array (jvm/af-handle arr))
              "af-lock-array")
  nil)

(defn unlock-array!
  "Unlock a previously locked array.
   
   Unlocking an array allows the memory manager to reuse its memory
   buffer if needed. Always unlock arrays after you're done with them.
   
   Parameters:
   - arr: AFArray to unlock
   
   Returns:
   nil
   
   Example:
   (unlock-array! my-array)"
  [^AFArray arr]
  (jvm/check! (device-ffi/af-unlock-array (jvm/af-handle arr))
              "af-unlock-array")
  nil)

(defn locked-array?
  "Check if an array is currently locked.
   
   Parameters:
   - arr: AFArray to check
   
   Returns:
   Boolean, true if the array is locked
   
   Example:
   (if (locked-array? my-array)
     (println \"Array is locked\")
     (println \"Array is not locked\"))"
  [^AFArray arr]
  (let [result-buf (mem/alloc 4)]
    (jvm/check! (device-ffi/af-is-locked-array result-buf (jvm/af-handle arr))
                "af-is-locked-array")
    (not (zero? (mem/read-int result-buf 0)))))

(defn get-device-ptr
  "Get the raw device pointer from an array.
   
   WARNING: This is an advanced function. The returned pointer should not
   be freed and its validity is tied to the array's lifetime.
   
   Parameters:
   - arr: AFArray to get pointer from
   
   Returns:
   Long integer representing the device pointer address
   
   Example:
   (let [ptr (get-device-ptr my-array)]
     ;; Use ptr with native libraries
     ptr)"
  [^AFArray arr]
  (let [ptr-buf (mem/alloc 8)]
    (jvm/check! (device-ffi/af-get-device-ptr ptr-buf (jvm/af-handle arr))
                "af-get-device-ptr")
    (mem/read-long ptr-buf 0)))

;;;
;;; Manual Evaluation Control
;;;

(defn set-manual-eval-flag!
  "Enable or disable manual evaluation mode.
   
   In manual evaluation mode, operations are not automatically evaluated.
   You must explicitly call eval-array! or eval-multiple! to compute results.
   This can improve performance by reducing redundant computations.
   
   Parameters:
   - flag: Boolean, true to enable manual evaluation, false to disable
   
   Returns:
   nil
   
   Example:
   (set-manual-eval-flag! true)  ; Enable manual mode
   ;; Build computation graph
   (set-manual-eval-flag! false) ; Re-enable automatic evaluation"
  [flag]
  (jvm/check! (device-ffi/af-set-manual-eval-flag (if flag 1 0))
              "af-set-manual-eval-flag")
  nil)

(defn get-manual-eval-flag
  "Get the current manual evaluation mode state.
   
   Returns:
   Boolean, true if manual evaluation is enabled
   
   Example:
   (if (get-manual-eval-flag)
     (println \"Manual evaluation enabled\")
     (println \"Automatic evaluation enabled\"))"
  []
  (let [flag-buf (mem/alloc 4)]
    (jvm/check! (device-ffi/af-get-manual-eval-flag flag-buf)
                "af-get-manual-eval-flag")
    (not (zero? (mem/read-int flag-buf 0)))))

;;;
;;; Kernel Cache Management
;;;

(defn set-kernel-cache-directory!
  "Set the directory for caching compiled kernels.
   
   Kernel caching can significantly reduce startup time by reusing
   previously compiled kernels.
   
   Parameters:
   - path: String path to cache directory
   - override-eval: Boolean, whether to override existing cache (default false)
   
   Returns:
   nil
   
   Example:
   (set-kernel-cache-directory! \"/tmp/af_cache\" false)"
  ([path]
   (set-kernel-cache-directory! path false))
  ([path override-eval]
   (let [arena (Arena/ofConfined)]
     (try
       (let [path-segment (jvm/string->c-string path arena)]
         (jvm/check! (device-ffi/af-set-kernel-cache-directory
                      path-segment
                      (if override-eval 1 0))
                     "af-set-kernel-cache-directory")
         nil)
       (finally
         (.close arena))))))

(defn get-kernel-cache-directory
  "Get the current kernel cache directory path.
   
   Returns:
   String path to the cache directory
   
   Example:
   (let [cache-dir (get-kernel-cache-directory)]
     (println \"Kernel cache:\" cache-dir))"
  []
  (let [arena (Arena/ofConfined)]
    (try
      (let [len-buf (mem/alloc 8) ; Buffer to hold size_t* length
            ;; First call to get the required buffer size
            _ (jvm/check! (device-ffi/af-get-kernel-cache-directory len-buf mem/null)
                          "af-get-kernel-cache-directory (get length)")
            length (mem/read-long len-buf 0)
            ;; Allocate buffer for the string (length includes null terminator)
            str-buf (mem/alloc length arena)]
        ;; Second call to get the actual string
        (jvm/check! (device-ffi/af-get-kernel-cache-directory len-buf str-buf)
                    "af-get-kernel-cache-directory (get string)")
        (jvm/c-string->string str-buf))
      (finally
        (.close arena)))))
