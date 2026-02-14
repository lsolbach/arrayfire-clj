(ns org.soulspace.arrayfire.integration.unified-api.memory
  "Integration of the ArrayFire memory related FFI bindings with the error
   handling and resource management on the JVM.
   
   This namespace provides memory management functions for ArrayFire:
   
   - Pinned memory: alloc-pinned, free-pinned (page-locked for fast transfers)
   - Host memory: alloc-host, free-host
   - Device memory: alloc-device, free-device (GPU memory)
   - Memory info: device-mem-info, print-mem-info
   - Memory manager: device-gc, set-mem-step-size, get-mem-step-size
   - Array locking: lock-array, unlock-array, is-locked-array?
   - Device pointers: get-device-ptr
   
   Memory Types:
   
   Pinned Memory:
   - Page-locked host memory
   - Fast host<->device transfers (no paging overhead)
   - Limited resource (use sparingly)
   - Best for: frequent data transfers, streaming
   
   Host Memory:
   - Regular CPU/system memory
   - ArrayFire-managed allocation
   - Use for: host-side operations
   
   Device Memory:
   - GPU/accelerator memory
   - Managed by ArrayFire's memory manager
   - Use for: array storage, computations
   
   Memory Manager:
   - Pools and reuses allocations
   - Reduces allocation overhead
   - Automatic garbage collection
   - Configurable chunk sizes"
  (:require [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.c-api.memory :as mem-ffi]
            [org.soulspace.arrayfire.integration.base.error :refer [check!]]
            [org.soulspace.arrayfire.integration.base.memory :as bmem]
            [org.soulspace.arrayfire.integration.base.resource :as res])
  (:import (org.soulspace.arrayfire.integration.base.resource AFArray)))

;;;
;;; Pinned Memory
;;;

(defn alloc-pinned
  "Allocate pinned (page-locked) memory for efficient device-host transfers.
   
   Pinned memory enables faster data transfers between host and device by
   preventing the OS from paging memory to disk. This reduces transfer
   latency and increases bandwidth.
   
   Use cases:
   - Frequent host<->device data transfers
   - Streaming applications
   - Real-time processing pipelines
   
   Caveats:
   - Limited resource (typically < 10% of system RAM)
   - Overuse can degrade system performance
   - Must be freed with free-pinned!
   
   Parameters:
   - bytes: Number of bytes to allocate (long)
   
   Returns:
   MemorySegment pointer to pinned memory
   
   Examples:
   ```clojure
   ;; Allocate 1MB of pinned memory
   (let [pinned (alloc-pinned (* 1024 1024))]
     (try
       ;; Use pinned memory for fast transfers
       (transfer-data-to-device pinned)
       (finally
         (free-pinned! pinned))))
   
   ;; Streaming data processing
   (with-pinned-buffer (* 1024 1024 10)
     (fn [buffer]
       (doseq [chunk data-chunks]
         (copy-to-pinned! buffer chunk)
         (process-on-device buffer))))
   ```"
  [bytes]
  (let [ptr-buf (mem/alloc 8)]
    (check! (mem-ffi/af-alloc-pinned ptr-buf (long bytes))
                "af-alloc-pinned")
    (mem/reinterpret (java.lang.foreign.MemorySegment/ofAddress (res/address->long (mem/read-address ptr-buf)))
                     (long bytes))))

(defn free-pinned!
  "Free pinned memory allocated by alloc-pinned.
   
   Must be called to release pinned memory resources. Pinned memory is
   a limited resource - failing to free it can cause allocation failures.
   
   Parameters:
   - ptr: Pointer to pinned memory (MemorySegment)
   
   Returns:
   nil
   
   Examples:
   ```clojure
   (let [pinned (alloc-pinned 1024)]
     ;; Use pinned memory
     (free-pinned! pinned))
   ```"
  [ptr]
  (check! (mem-ffi/af-free-pinned ptr)
              "af-free-pinned")
  nil)

;;;
;;; Host Memory
;;;

(defn alloc-host
  "Allocate host-accessible memory managed by ArrayFire.
   
   Allocates CPU/system memory for use with ArrayFire operations.
   Useful for host-side computations and data preparation.
   
   Parameters:
   - bytes: Number of bytes to allocate (long)
   
   Returns:
   MemorySegment pointer to host memory
   
   Examples:
   ```clojure
   ;; Allocate host memory for data preparation
   (let [host-mem (alloc-host (* 1000 8))]  ; 1000 doubles
     (try
       ;; Fill with data
       (fill-host-memory! host-mem data)
       ;; Create array from host memory
       (create-array-from-host host-mem [1000])
       (finally
         (free-host! host-mem))))
   ```"
  [bytes]
  (let [ptr-buf (mem/alloc 8)]
    (check! (mem-ffi/af-alloc-host ptr-buf (long bytes))
                "af-alloc-host")
    (mem/reinterpret (java.lang.foreign.MemorySegment/ofAddress (res/address->long (mem/read-address ptr-buf)))
                     (long bytes))))

(defn free-host!
  "Free host memory allocated by alloc-host.
   
   Parameters:
   - ptr: Pointer to host memory (MemorySegment)
   
   Returns:
   nil
   
   Examples:
   ```clojure
   (let [host-mem (alloc-host 1024)]
     ;; Use host memory
     (free-host! host-mem))
   ```"
  [ptr]
  (check! (mem-ffi/af-free-host ptr)
              "af-free-host")
  nil)

;;;
;;; Device Memory
;;;

(defn alloc-device
  "Allocate device memory using ArrayFire's memory manager.
   
   Allocates GPU/accelerator memory. The memory manager pools and reuses
   allocations to minimize overhead. Memory is not immediately freed when
   free-device! is called - it's returned to the pool for reuse.
   
   Note: This function uses the v2 API which is compatible with all backends.
   
   Parameters:
   - bytes: Number of bytes to allocate (long)
   
   Returns:
   MemorySegment pointer to device memory
   
   Examples:
   ```clojure
   ;; Allocate 1MB device memory
   (let [dev-mem (alloc-device (* 1024 1024))]
     (try
       ;; Use device memory
       (process-on-device dev-mem)
       (finally
         (free-device! dev-mem))))
   
   ;; Temporary buffer for computation
   (with-device-buffer (* 1024 1024)
     (fn [buffer]
       (compute-with-buffer buffer)))
   ```"
  [bytes]
  (let [ptr-buf (mem/alloc 8)]
    (check! (mem-ffi/af-alloc-device-v2 ptr-buf (long bytes))
                "af-alloc-device-v2")
    (mem/reinterpret (java.lang.foreign.MemorySegment/ofAddress (res/address->long (mem/read-address ptr-buf)))
                     (long bytes))))

(defn free-device!
  "Free device memory (return to memory manager pool).
   
   Returns device memory to the memory manager's pool for reuse.
   The memory is not immediately freed - it's kept for future allocations
   to avoid allocation overhead.
   
   Parameters:
   - ptr: Pointer to device memory (MemorySegment)
   
   Returns:
   nil
   
   Examples:
   ```clojure
   (let [dev-mem (alloc-device 1024)]
     ;; Use device memory
     (free-device! dev-mem))
   ```"
  [ptr]
  (check! (mem-ffi/af-free-device-v2 ptr)
              "af-free-device-v2")
  nil)

;;;
;;; Memory Information
;;;

(defn device-mem-info
  "Get memory statistics from the ArrayFire memory manager.
   
   Returns detailed information about device memory usage including
   total allocations and locked (in-use) buffers.
   
   Returns:
   Map containing:
   - :alloc-bytes - Total allocated bytes
   - :alloc-buffers - Number of allocated buffers
   - :lock-bytes - Bytes currently locked (in use)
   - :lock-buffers - Number of locked buffers
   
   Examples:
   ```clojure
   ;; Monitor memory usage
   (let [info (device-mem-info)]
     (println (str \"Allocated: \" (/ (:alloc-bytes info) 1024 1024) \" MB\"))
     (println (str \"Locked: \" (/ (:lock-bytes info) 1024 1024) \" MB\"))
     (println (str \"Utilization: \" 
                   (* 100.0 (/ (:lock-bytes info) (:alloc-bytes info))) \"%\")))
   
   ;; Check for memory leaks
   (let [before (device-mem-info)]
     (run-computation)
     (let [after (device-mem-info)]
       (when (> (:lock-buffers after) (:lock-buffers before))
         (println \"Warning: Potential memory leak\"))))
   ```"
  []
  (let [alloc-bytes-buf (mem/alloc 8)
        alloc-buffers-buf (mem/alloc 8)
        lock-bytes-buf (mem/alloc 8)
        lock-buffers-buf (mem/alloc 8)]
    (check! (mem-ffi/af-device-mem-info alloc-bytes-buf alloc-buffers-buf
                                            lock-bytes-buf lock-buffers-buf)
                "af-device-mem-info")
    {:alloc-bytes (mem/read-long alloc-bytes-buf 0)
     :alloc-buffers (mem/read-long alloc-buffers-buf 0)
     :lock-bytes (mem/read-long lock-bytes-buf 0)
     :lock-buffers (mem/read-long lock-buffers-buf 0)}))

(defn print-mem-info
  "Print detailed memory information from ArrayFire Device Manager.
   
   Prints a formatted table showing all allocated buffers with their
   addresses, sizes, and lock status. Useful for debugging memory issues.
   
   Parameters:
   - msg: Optional message to print before the table (string), default \"Memory Info\"
   - device-id: Device ID to query (int), default -1 (active device)
   
   Returns:
   nil
   
   Examples:
   ```clojure
   ;; Print memory info for active device
   (print-mem-info)
   
   ;; Print with custom message
   (print-mem-info \"After computation\")
   
   ;; Query specific device
   (print-mem-info \"Device 0\" 0)
   ```"
  ([]
   (print-mem-info "Memory Info" -1))
  ([msg]
   (print-mem-info msg -1))
  ([msg device-id]
   (let [msg-ptr (if msg
                   (bmem/string->c-string msg)
                   bmem/null-ptr)]
     (check! (mem-ffi/af-print-mem-info msg-ptr (int device-id))
                 "af-print-mem-info")
     nil)))

;;;
;;; Memory Manager Control
;;;

(defn device-gc
  "Call garbage collection in the ArrayFire memory manager.
   
   Forces the memory manager to free unused memory buffers. Normally,
   the memory manager automatically manages memory, but this can be
   called to free memory immediately when needed.
   
   Use cases:
   - Before allocating very large arrays
   - When system memory is constrained
   - Before benchmarking to get clean baseline
   
   Returns:
   nil
   
   Examples:
   ```clojure
   ;; Force cleanup before large allocation
   (device-gc)
   (let [big-array (randu [10000 10000])]
     ...)
   
   ;; Clean memory before benchmark
   (device-gc)
   (time (run-benchmark))
   
   ;; Periodic cleanup in long-running app
   (doseq [batch data-batches]
     (process-batch batch)
     (when (zero? (mod batch 100))
       (device-gc)))
   ```"
  []
  (check! (mem-ffi/af-device-gc)
              "af-device-gc")
  nil)

(defn set-mem-step-size!
  "Set the minimum memory chunk size for the memory manager.
   
   The memory manager allocates memory in chunks to reduce allocation
   overhead. This sets the minimum chunk size. Larger values reduce
   allocation frequency but may waste memory.
   
   Only works with the default memory manager.
   
   Parameters:
   - step-bytes: Minimum chunk size in bytes (long)
   
   Returns:
   nil
   
   Typical values:
   - 1MB (1024*1024): Default, good for most workloads
   - 4MB (4*1024*1024): Reduce overhead for large allocations
   - 256KB (256*1024): Reduce waste for small allocations
   
   Examples:
   ```clojure
   ;; Set 4MB chunks for large array workloads
   (set-mem-step-size! (* 4 1024 1024))
   
   ;; Set 256KB chunks for many small arrays
   (set-mem-step-size! (* 256 1024))
   
   ;; Restore default
   (set-mem-step-size! (* 1024 1024))
   ```"
  [step-bytes]
  (check! (mem-ffi/af-set-mem-step-size (long step-bytes))
              "af-set-mem-step-size")
  nil)

(defn get-mem-step-size
  "Get the minimum memory chunk size from the memory manager.
   
   Returns the current chunk size used by the memory manager.
   Only works with the default memory manager.
   
   Returns:
   Chunk size in bytes (long)
   
   Examples:
   ```clojure
   ;; Query current setting
   (let [step (get-mem-step-size)]
     (println (str \"Chunk size: \" (/ step 1024 1024) \" MB\")))
   
   ;; Save and restore
   (let [saved-step (get-mem-step-size)]
     (try
       (set-mem-step-size! (* 4 1024 1024))
       ;; ... work with larger chunks ...
       (finally
         (set-mem-step-size! saved-step))))
   ```"
  []
  (let [step-buf (mem/alloc 8)]
    (check! (mem-ffi/af-get-mem-step-size step-buf)
                "af-get-mem-step-size")
    (mem/read-long step-buf 0)))

;;;
;;; Array Locking
;;;

(defn lock-array!
  "Lock the device buffer in the memory manager.
   
   Prevents the memory manager from freeing or moving the array's
   device memory. Use when you need stable device pointers (e.g.,
   for custom kernels or external library integration).
   
   Must be unlocked with unlock-array! when done.
   
   Parameters:
   - arr: Array to lock (AFArray)
   
   Returns:
   nil
   
   Examples:
   ```clojure
   ;; Lock array before passing to custom kernel
   (let [arr (randu [1000])]
     (lock-array! arr)
     (try
       (let [ptr (get-device-ptr arr)]
         (run-custom-kernel ptr))
       (finally
         (unlock-array! arr))))
   ```"
  [^AFArray arr]
  (check! (mem-ffi/af-lock-array (res/af-handle arr))
              "af-lock-array")
  nil)

(defn unlock-array!
  "Unlock device buffer in the memory manager.
   
   Returns control of the array's device memory to the memory manager.
   The manager can now free or reuse the memory as needed.
   
   Parameters:
   - arr: Array to unlock (AFArray)
   
   Returns:
   nil
   
   Examples:
   ```clojure
   (unlock-array! arr)
   ```"
  [^AFArray arr]
  (check! (mem-ffi/af-unlock-array (res/af-handle arr))
              "af-unlock-array")
  nil)

(defn is-locked-array?
  "Query if the array has been locked by the user.
   
   An array is locked when lock-array!, get-device-ptr, or similar
   functions are called.
   
   Parameters:
   - arr: Array to query (AFArray)
   
   Returns:
   Boolean - true if locked, false otherwise
   
   Examples:
   ```clojure
   ;; Check lock status
   (when (is-locked-array? arr)
     (println \"Array is locked\"))
   
   ;; Conditional unlock
   (when (is-locked-array? arr)
     (unlock-array! arr))
   ```"
  [^AFArray arr]
  (let [result-buf (mem/alloc 4)]
    (check! (mem-ffi/af-is-locked-array result-buf
                        (res/af-handle arr))
                "af-is-locked-array")
    (not (zero? (mem/read-int result-buf 0)))))

(defn get-device-ptr
  "Get the device pointer and lock the buffer in memory manager.
   
   Returns a pointer to the array's device memory and locks it to
   prevent the memory manager from moving or freeing it.
   
   Must call unlock-array! when done with the pointer.
   
   Note: For OpenCL backend, cast the pointer to cl_mem.
   
   Parameters:
   - arr: Array (AFArray)
   
   Returns:
   MemorySegment containing device pointer
   
   Examples:
   ```clojure
   ;; Get device pointer for custom kernel
   (let [arr (randu [1000])
         ptr (get-device-ptr arr)]
     (try
       (launch-custom-kernel ptr)
       (finally
         (unlock-array! arr))))
   ```"
  [^AFArray arr]
  (let [ptr-buf (mem/alloc 8)]
    (check! (mem-ffi/af-get-device-ptr ptr-buf
                         (res/af-handle arr))
                "af-get-device-ptr")
    (java.lang.foreign.MemorySegment/ofAddress (res/address->long (mem/read-address ptr-buf)))))

;;;
;;; Convenience Functions
;;;

(defmacro with-pinned-memory
  "Allocate pinned memory, execute body, then free.
   
   Parameters:
   - bytes: Number of bytes to allocate
   - binding: Symbol to bind the memory pointer to
   - body: Code to execute
   
   Examples:
   ```clojure
   (with-pinned-memory 1024 pinned
     ;; Use pinned memory
     (copy-to-pinned! pinned data))
   ```"
  [bytes binding & body]
  `(let [~binding (alloc-pinned ~bytes)]
     (try
       ~@body
       (finally
         (free-pinned! ~binding)))))

(defmacro with-device-memory
  "Allocate device memory, execute body, then free.
   
   Parameters:
   - bytes: Number of bytes to allocate
   - binding: Symbol to bind the memory pointer to
   - body: Code to execute
   
   Examples:
   ```clojure
   (with-device-memory (* 1024 1024) dev-mem
     ;; Use device memory
     (process-on-device dev-mem))
   ```"
  [bytes binding & body]
  `(let [~binding (alloc-device ~bytes)]
     (try
       ~@body
       (finally
         (free-device! ~binding)))))

(defmacro with-locked-array
  "Lock array, execute body, then unlock.
   
   Parameters:
   - arr: Array to lock (AFArray)
   - body: Code to execute
   
   Examples:
   ```clojure
   (with-locked-array my-array
     (let [ptr (get-device-ptr my-array)]
       (process-with-pointer ptr)))
   ```"
  [arr & body]
  `(do
     (lock-array! ~arr)
     (try
       ~@body
       (finally
         (unlock-array! ~arr)))))
