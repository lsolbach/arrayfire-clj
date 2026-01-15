(ns org.soulspace.arrayfire.ffi.handle
  "Bindings for ArrayFire handle and memory locking functions.
   
   These functions provide low-level control over ArrayFire's memory management
   system, allowing you to lock device buffers, access raw device pointers, and
   query the lock state of arrays. This is essential for advanced use cases where
   you need direct device memory access or interoperability with other libraries.
   
   Memory Management Overview:
   
   ArrayFire uses a sophisticated memory manager that handles GPU memory
   allocation, reuse, and garbage collection automatically. However, there are
   scenarios where you need direct access to the underlying device pointers:
   
   1. **Interoperability**: Interfacing with CUDA, OpenCL, or other GPU libraries
   2. **Zero-Copy**: Avoiding data transfers by working directly with device memory
   3. **Custom Kernels**: Passing ArrayFire arrays to custom GPU kernels
   4. **Performance**: Fine-grained control over memory lifecycle
   
   Memory Locking:
   
   When you lock an array, you prevent ArrayFire's memory manager from
   freeing or reusing that memory buffer. This is crucial when:
   - Passing device pointers to external libraries
   - Holding pointers across multiple operations
   - Ensuring memory stability during async operations
   
   Lock Lifecycle:
   1. **Lock**: af_lock_array or af_get_device_ptr (implicit lock)
   2. **Use**: Access and manipulate the device pointer
   3. **Unlock**: af_unlock_array (return control to memory manager)
   
   Important: Always unlock arrays when done to avoid memory leaks!
   
   Memory Manager Behavior:
   
   - **Unlocked arrays**: Memory manager can free/reuse the buffer
   - **Locked arrays**: Memory manager preserves the buffer
   - **Reference counting**: Multiple locks require multiple unlocks
   - **Automatic unlock**: Array destruction unlocks automatically
   
   Device Pointer Types:
   
   - **CUDA backend**: `void*` CUDA device pointer
   - **OpenCL backend**: `cl_mem` OpenCL memory object
   - **CPU backend**: `void*` C pointer to host memory
   
   For OpenCL, cast the returned pointer to `cl_mem`:
   ```c
   void* ptr;
   af_get_device_ptr(&ptr, array);
   cl_mem mem = (cl_mem)ptr;
   ```
   
   Common Patterns:
   
   1. **External Library Integration**:
   ```clojure
   (let [arr (create-array data [1024])
         ptr-container (mem/alloc-instance ::mem/pointer)]
     ;; Get device pointer (locks automatically)
     (af-get-device-ptr ptr-container arr)
     (let [dev-ptr (mem/read-ptr ptr-container)]
       ;; Pass dev-ptr to CUDA/OpenCL library
       (external-gpu-operation dev-ptr)
       ;; Unlock when done
       (af-unlock-array arr)))
   ```
   
   2. **Custom CUDA Kernel**:
   ```clojure
   (let [input (create-array input-data dims)
         output (create-array output-data dims)
         in-ptr-container (mem/alloc-instance ::mem/pointer)
         out-ptr-container (mem/alloc-instance ::mem/pointer)]
     (af-get-device-ptr in-ptr-container input)
     (af-get-device-ptr out-ptr-container output)
     (let [in-ptr (mem/read-ptr in-ptr-container)
           out-ptr (mem/read-ptr out-ptr-container)]
       ;; Launch custom CUDA kernel
       (launch-cuda-kernel in-ptr out-ptr params)
       (af-unlock-array input)
       (af-unlock-array output)))
   ```
   
   3. **Zero-Copy Data Sharing**:
   ```clojure
   (let [shared-array (create-array data dims)]
     ;; Lock to share with another library
     (af-lock-array shared-array)
     ;; Other library uses the data
     (process-with-external-lib shared-array)
     ;; Unlock when external processing is done
     (af-unlock-array shared-array))
   ```
   
   4. **Manual Memory Management**:
   ```clojure
   (let [persistent-array (create-array data dims)
         locked-container (mem/alloc-instance ::mem/int)]
     ;; Check if already locked
     (af-is-locked-array locked-container persistent-array)
     (when (zero? (mem/read-int locked-container))
       ;; Not locked, lock it now
       (af-lock-array persistent-array))
     ;; Use array with guaranteed stable pointer
     (long-running-operation persistent-array)
     ;; Unlock when done
     (af-unlock-array persistent-array))
   ```
   
   Best Practices:
   
   1. **Always Unlock**: Use try/finally to ensure unlocking
   ```clojure
   (let [arr (create-array data dims)]
     (try
       (af-lock-array arr)
       ;; Use locked array
       (finally
         (af-unlock-array arr))))
   ```
   
   2. **Check Lock State**: Query before locking/unlocking
   ```clojure
   (let [locked-container (mem/alloc-instance ::mem/int)]
     (af-is-locked-array locked-container arr)
     (let [is-locked (not (zero? (mem/read-int locked-container)))]
       (when-not is-locked
         (af-lock-array arr))))
   ```
   
   3. **Minimize Lock Duration**: Lock only when necessary
   ```clojure
   ;; Bad: Lock early, unlock late
   (af-lock-array arr)
   (do-arrayfire-ops arr)  ; Doesn't need lock
   (external-gpu-op arr)   ; Needs lock
   (af-unlock-array arr)
   
   ;; Good: Lock just before external use
   (do-arrayfire-ops arr)
   (af-lock-array arr)
   (external-gpu-op arr)
   (af-unlock-array arr)
   ```
   
   4. **Avoid Double Locking**: Track lock state
   ```clojure
   (defn with-locked-array [arr f]
     (let [locked-container (mem/alloc-instance ::mem/int)]
       (af-is-locked-array locked-container arr)
       (let [was-locked (not (zero? (mem/read-int locked-container)))]
         (try
           (when-not was-locked
             (af-lock-array arr))
           (f arr)
           (finally
             (when-not was-locked
               (af-unlock-array arr)))))))
   ```
   
   Performance Considerations:
   
   1. **Lock Overhead**: Minimal, but avoid in tight loops
   2. **Memory Fragmentation**: Locked buffers can't be reused
   3. **Garbage Collection**: Locked buffers prevent GC optimization
   4. **Synchronization**: Locking may trigger implicit GPU synchronization
   
   Thread Safety:
   
   ArrayFire is generally thread-safe, but locking introduces complexity:
   - Each thread should manage its own array locks
   - Avoid sharing locked arrays between threads
   - Use proper synchronization when needed
   
   Deprecation Notes:
   
   - af_lock_device_ptr: Deprecated, use af_lock_array (AF_API_VERSION >= 33)
   - af_unlock_device_ptr: Deprecated, use af_unlock_array (AF_API_VERSION >= 33)
   
   The old functions are convenience wrappers that call the new functions.
   
   Troubleshooting:
   
   1. **Memory Leaks**: Ensure all locks have matching unlocks
      - Use memory profiling tools
      - Check af_print_mem_info for locked buffer counts
   
   2. **Invalid Pointers**: Don't use pointers after unlock
      - Memory manager may reuse/free the buffer
      - Re-lock if you need the pointer again
   
   3. **Deadlocks**: Avoid circular dependencies
      - Don't lock while holding other locks
      - Use consistent lock ordering
   
   4. **Performance Degradation**: Too many locked buffers
      - Memory manager can't optimize
      - Monitor locked buffer count
      - Unlock aggressively
   
   Related Functions:
   - af_get_raw_ptr: Get pointer without locking (dangerous!)
   - af_device_array: Create array from device memory
   - af_device_mem_info: Query memory usage including locks
   - af_print_mem_info: Print detailed memory manager state"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; Memory locking functions

;; af_err af_lock_array(const af_array arr)
(defcfn af-lock-array
  "Lock the device buffer in the memory manager.
   
   Prevents ArrayFire's memory manager from freeing or reusing the buffer
   associated with this array. The buffer remains locked until af_unlock_array
   is called. This is essential when passing device pointers to external libraries
   or when you need guaranteed pointer stability across operations.
   
   Parameters:
   
   - arr: af_array handle to lock
     * Must be a valid array handle
     * Can be called multiple times (reference counted)
     * Each lock requires a corresponding unlock
   
   Returns:
   AF_SUCCESS (0) on success, error code otherwise:
   - AF_ERR_ARG: Invalid array handle
   - AF_ERR_NO_MEM: Out of memory
   
   Locking Behavior:
   
   When an array is locked:
   1. Memory manager marks the buffer as user-locked
   2. Buffer cannot be freed during garbage collection
   3. Buffer cannot be reused for other allocations
   4. Device pointer remains valid until unlock
   5. Reference count is incremented
   
   Multiple locks on the same array are allowed and tracked via reference
   counting. You must call af_unlock_array the same number of times.
   
   Use Cases:
   
   1. External Library Integration:
      Lock before passing to CUDA/OpenCL/other GPU libraries
   
   2. Long-Running Operations:
      Ensure pointer stability across async operations
   
   3. Zero-Copy Sharing:
      Share device memory with other frameworks
   
   4. Custom Kernels:
      Pass stable pointers to user-written GPU kernels
   
   Example:
   ```clojure
   ;; Lock array before external use
   (let [arr (create-array data [1024])
         locked-status (mem/alloc-instance ::mem/int)]
     ;; Check if already locked
     (af-is-locked-array locked-status arr)
     (when (zero? (mem/read-int locked-status))
       ;; Not locked, lock it
       (af-lock-array arr))
     
     ;; Now safe to use with external library
     (external-gpu-operation arr)
     
     ;; Unlock when done
     (af-unlock-array arr))
   ```
   
   Best Practices:
   - Always pair with af_unlock_array
   - Use try/finally to ensure unlock
   - Minimize lock duration
   - Check lock state before locking
   
   Performance Impact:
   - Minimal overhead for lock/unlock calls
   - Locked buffers reduce memory manager efficiency
   - Can cause memory fragmentation if overused
   
   Thread Safety:
   - Lock/unlock must be on the same thread
   - Don't share locked arrays between threads without synchronization
   
   See Also:
   - af_unlock_array: Release the lock
   - af_is_locked_array: Query lock status
   - af_get_device_ptr: Get pointer (locks implicitly)
   - af_lock_device_ptr: Deprecated, use af_lock_array instead"
  "af_lock_array" [::mem/pointer] ::mem/int)

;; af_err af_unlock_array(const af_array arr)
(defcfn af-unlock-array
  "Unlock device buffer in the memory manager.
   
   Returns control of the device buffer to ArrayFire's memory manager.
   After unlocking, the memory manager can free or reuse the buffer as needed.
   This must be called after af_lock_array or af_get_device_ptr to avoid
   memory leaks.
   
   Parameters:
   
   - arr: af_array handle to unlock
     * Must be a currently locked array
     * Reference count is decremented
     * Buffer freed only when count reaches zero
   
   Returns:
   AF_SUCCESS (0) on success, error code otherwise:
   - AF_ERR_ARG: Invalid array handle
   - AF_ERR_LOCK: Array not locked
   
   Unlocking Behavior:
   
   When an array is unlocked:
   1. Reference count is decremented
   2. If count reaches zero:
      - User lock is removed
      - Memory manager regains full control
      - Buffer may be freed or reused
   3. Device pointer may become invalid
   
   Important: After unlocking, the device pointer obtained from
   af_get_device_ptr should be considered invalid. Do not use it!
   
   Reference Counting:
   
   If you called af_lock_array or af_get_device_ptr multiple times,
   you must call af_unlock_array the same number of times:
   
   ```clojure
   (af-lock-array arr)      ; ref count = 1
   (af-lock-array arr)      ; ref count = 2
   (af-unlock-array arr)    ; ref count = 1 (still locked)
   (af-unlock-array arr)    ; ref count = 0 (unlocked)
   ```
   
   Use Cases:
   
   1. After External Library Use:
      Unlock after GPU library is done with the pointer
   
   2. Resource Cleanup:
      Free memory manager resources
   
   3. Enable GC:
      Allow memory manager to optimize
   
   4. End of Scope:
      Release locks when array is no longer needed
   
   Example:
   ```clojure
   ;; Safe unlocking with try/finally
   (let [arr (create-array data [1024])]
     (try
       (af-lock-array arr)
       ;; Use locked array with external library
       (external-gpu-kernel arr)
       (finally
         ;; Always unlock, even if exception occurs
         (af-unlock-array arr))))
   ```
   
   Best Practices:
   
   1. Always Unlock:
      Every lock must have a matching unlock
   
   2. Use Try/Finally:
      Ensure unlock happens even on error
   
   3. Check Lock State:
      Use af_is_locked_array before unlock
   
   4. Don't Use After Unlock:
      Device pointers invalid after unlock
   
   5. Match Lock Count:
      Unlock same number of times as locked
   
   Common Pitfalls:
   
   1. Using Pointer After Unlock:
      ```clojure
      ;; WRONG!
      (let [ptr-container (mem/alloc-instance ::mem/pointer)]
        (af-get-device-ptr ptr-container arr)
        (let [dev-ptr (mem/read-ptr ptr-container)]
          (af-unlock-array arr)
          (use-pointer dev-ptr))) ; DANGER: Invalid pointer!
      
      ;; RIGHT
      (let [ptr-container (mem/alloc-instance ::mem/pointer)]
        (af-get-device-ptr ptr-container arr)
        (let [dev-ptr (mem/read-ptr ptr-container)]
          (use-pointer dev-ptr)
          (af-unlock-array arr))) ; Unlock after use
      ```
   
   2. Forgetting to Unlock:
      ```clojure
      ;; WRONG: Memory leak
      (af-lock-array arr)
      (external-operation arr)
      ;; Forgot to unlock!
      
      ;; RIGHT: Always unlock
      (try
        (af-lock-array arr)
        (external-operation arr)
        (finally
          (af-unlock-array arr)))
      ```
   
   3. Double Unlock:
      ```clojure
      ;; WRONG: Will cause error
      (af-lock-array arr)
      (af-unlock-array arr)
      (af-unlock-array arr) ; ERROR: Not locked!
      
      ;; RIGHT: Track state
      (let [locked-container (mem/alloc-instance ::mem/int)]
        (af-is-locked-array locked-container arr)
        (when-not (zero? (mem/read-int locked-container))
          (af-unlock-array arr)))
      ```
   
   Performance Impact:
   - Minimal overhead for unlock operation
   - Enables memory manager optimizations
   - Allows buffer reuse and garbage collection
   
   Memory Manager Behavior After Unlock:
   - May immediately free the buffer if no references
   - May keep buffer for future reuse
   - May merge with adjacent free buffers
   - May trigger garbage collection
   
   See Also:
   - af_lock_array: Lock the array
   - af_is_locked_array: Check lock status
   - af_get_device_ptr: Get pointer (locks implicitly)
   - af_unlock_device_ptr: Deprecated, use af_unlock_array instead"
  "af_unlock_array" [::mem/pointer] ::mem/int)

;; af_err af_is_locked_array(bool *res, const af_array arr)
(defcfn af-is-locked-array
  "Query if the array has been locked by the user.
   
   Checks whether an array's device buffer is currently locked. An array can
   be locked explicitly via af_lock_array or implicitly via af_get_device_ptr
   or af_get_raw_ptr. This function helps you track lock state and avoid
   double locking or unlocking errors.
   
   Parameters:
   
   - res: bool* (as int*) output lock status
     * 0 (false): Array is not locked
     * Non-zero (true): Array is locked by user
     * Modified in-place
   
   - arr: af_array handle to query
     * Must be a valid array handle
   
   Returns:
   AF_SUCCESS (0) on success, error code otherwise:
   - AF_ERR_ARG: Invalid array handle or null result pointer
   
   Lock Detection:
   
   This function returns true if the array was locked by:
   1. af_lock_array - Explicit user lock
   2. af_get_device_ptr - Implicit lock when getting pointer
   3. af_get_raw_ptr - Low-level pointer access (locks array)
   4. C++ array.device<T>() - C++ API device pointer method
   5. C++ array.lock() - C++ API lock method
   
   Internal vs User Locks:
   
   ArrayFire distinguishes between:
   - **Internal locks**: Temporary locks by ArrayFire operations (transparent)
   - **User locks**: Locks created by user code (tracked by this function)
   
   This function only returns true for user locks.
   
   Reference Counting:
   
   The function returns true if the user lock count > 0.
   Multiple locks are tracked via reference counting:
   
   ```clojure
   (let [locked-container (mem/alloc-instance ::mem/int)]
     (af-is-locked-array locked-container arr)
     (println \"Initially:\" (mem/read-int locked-container)) ; 0
     
     (af-lock-array arr)
     (af-is-locked-array locked-container arr)
     (println \"After 1 lock:\" (mem/read-int locked-container)) ; 1
     
     (af-lock-array arr)
     (af-is-locked-array locked-container arr)
     (println \"After 2 locks:\" (mem/read-int locked-container)) ; 1
     
     (af-unlock-array arr)
     (af-is-locked-array locked-container arr)
     (println \"After 1 unlock:\" (mem/read-int locked-container)) ; 1
     
     (af-unlock-array arr)
     (af-is-locked-array locked-container arr)
     (println \"After 2 unlocks:\" (mem/read-int locked-container))) ; 0
   ```
   
   Use Cases:
   
   1. **Conditional Locking**:
      Lock only if not already locked
   
   2. **Debug/Validation**:
      Assert expected lock state
   
   3. **Resource Tracking**:
      Monitor which arrays are locked
   
   4. **Smart Unlock**:
      Unlock only if locked
   
   5. **State Management**:
      Track lock lifecycle in complex code
   
   Examples:
   
   1. Conditional Locking:
   ```clojure
   (defn ensure-locked [arr]
     (let [locked-container (mem/alloc-instance ::mem/int)]
       (af-is-locked-array locked-container arr)
       (when (zero? (mem/read-int locked-container))
         (af-lock-array arr))))
   ```
   
   2. Safe Unlocking:
   ```clojure
   (defn safe-unlock [arr]
     (let [locked-container (mem/alloc-instance ::mem/int)]
       (af-is-locked-array locked-container arr)
       (when-not (zero? (mem/read-int locked-container))
         (af-unlock-array arr))))
   ```
   
   3. Lock Guard Pattern:
   ```clojure
   (defn with-locked-array [arr f]
     (let [locked-container (mem/alloc-instance ::mem/int)]
       (af-is-locked-array locked-container arr)
       (let [was-locked (not (zero? (mem/read-int locked-container)))]
         (try
           (when-not was-locked
             (af-lock-array arr))
           (f arr)
           (finally
             (when-not was-locked
               (af-unlock-array arr)))))))
   ```
   
   4. Debug Assertion:
   ```clojure
   (defn assert-locked [arr]
     (let [locked-container (mem/alloc-instance ::mem/int)]
       (af-is-locked-array locked-container arr)
       (assert (not (zero? (mem/read-int locked-container)))
               \"Array must be locked at this point\")))
   ```
   
   5. Resource Audit:
   ```clojure
   (defn count-locked-arrays [arrays]
     (let [locked-container (mem/alloc-instance ::mem/int)]
       (count (filter (fn [arr]
                        (af-is-locked-array locked-container arr)
                        (not (zero? (mem/read-int locked-container))))
                      arrays))))
   ```
   
   Best Practices:
   
   1. **Check Before Lock/Unlock**:
      Avoid double lock/unlock errors
   
   2. **Use for Debugging**:
      Add assertions in development
   
   3. **Track Complex Flows**:
      Use when lock lifetime spans multiple functions
   
   4. **Implement Lock Guards**:
      RAII-style lock management in Clojure
   
   Performance:
   - Very fast (simple flag check)
   - No GPU synchronization required
   - Negligible overhead
   
   API Version:
   Available since AF_API_VERSION >= 34
   
   See Also:
   - af_lock_array: Lock an array
   - af_unlock_array: Unlock an array
   - af_get_device_ptr: Get device pointer (locks array)
   - af_device_mem_info: Get memory usage including lock counts"
  "af_is_locked_array" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_get_device_ptr(void **ptr, const af_array arr)
(defcfn af-get-device-ptr
  "Get the device pointer and lock the buffer in memory manager.
   
   Retrieves the raw device pointer for an array's data and automatically locks
   the buffer to prevent the memory manager from freeing it. The device pointer
   remains valid until af_unlock_array is called. This is the primary method
   for interfacing ArrayFire with external GPU libraries.
   
   Parameters:
   
   - ptr: void** output device pointer
     * CUDA backend: CUDA device pointer (use directly in CUDA)
     * OpenCL backend: cl_mem handle (cast from void*)
     * CPU backend: Host memory pointer
     * Pointer is valid until unlock
     * Must not be used after array is unlocked
   
   - arr: af_array source array
     * Must be a valid array handle
     * Array is locked automatically
     * Must call af_unlock_array when done
   
   Returns:
   AF_SUCCESS (0) on success, error code otherwise:
   - AF_ERR_ARG: Invalid array handle or null pointer
   - AF_ERR_NO_MEM: Out of memory
   
   Locking Behavior:
   
   This function automatically locks the array, equivalent to:
   1. af_lock_array(arr)
   2. Get device pointer
   3. Return pointer
   
   The lock ensures the pointer remains valid. You MUST call af_unlock_array
   when finished to avoid memory leaks.
   
   Backend-Specific Pointer Types:
   
   1. **CUDA Backend**:
      ```clojure
      (let [ptr-container (mem/alloc-instance ::mem/pointer)]
        (af-get-device-ptr ptr-container arr)
        (let [cuda-ptr (mem/read-ptr ptr-container)]
          ;; cuda-ptr is a CUDA device pointer
          ;; Can be passed directly to CUDA kernels
          (launch-cuda-kernel cuda-ptr ...)))
      ```
   
   2. **OpenCL Backend**:
      ```clojure
      (let [ptr-container (mem/alloc-instance ::mem/pointer)]
        (af-get-device-ptr ptr-container arr)
        (let [void-ptr (mem/read-ptr ptr-container)]
          ;; Cast to cl_mem
          ;; cl_mem mem = (cl_mem)void-ptr;
          (use-opencl-buffer void-ptr ...)))
      ```
   
   3. **CPU Backend**:
      ```clojure
      (let [ptr-container (mem/alloc-instance ::mem/pointer)]
        (af-get-device-ptr ptr-container arr)
        (let [host-ptr (mem/read-ptr ptr-container)]
          ;; Regular C pointer to host memory
          (process-host-memory host-ptr ...)))
      ```
   
   Use Cases:
   
   1. **Custom CUDA Kernels**:
   ```clojure
   (let [input (create-array input-data [1024])
         output (create-empty-array [1024])
         in-ptr-container (mem/alloc-instance ::mem/pointer)
         out-ptr-container (mem/alloc-instance ::mem/pointer)]
     (try
       (af-get-device-ptr in-ptr-container input)
       (af-get-device-ptr out-ptr-container output)
       (let [in-ptr (mem/read-ptr in-ptr-container)
             out-ptr (mem/read-ptr out-ptr-container)]
         ;; Launch custom CUDA kernel
         (custom-cuda-kernel in-ptr out-ptr 1024))
       (finally
         (af-unlock-array output)
         (af-unlock-array input))))
   ```
   
   2. **OpenCL Interop**:
   ```clojure
   (let [arr (create-array data dims)
         ptr-container (mem/alloc-instance ::mem/pointer)]
     (try
       (af-get-device-ptr ptr-container arr)
       (let [cl-mem-ptr (mem/read-ptr ptr-container)]
         ;; Use with OpenCL
         (clEnqueueNDRangeKernel queue kernel ... cl-mem-ptr))
       (finally
         (af-unlock-array arr))))
   ```
   
   3. **External Library Integration**:
   ```clojure
   (let [matrix (create-array matrix-data [rows cols])
         ptr-container (mem/alloc-instance ::mem/pointer)]
     (try
       (af-get-device-ptr ptr-container matrix)
       (let [dev-ptr (mem/read-ptr ptr-container)]
         ;; Pass to cuBLAS, cuDNN, etc.
         (external-gpu-gemm dev-ptr ...))
       (finally
         (af-unlock-array matrix))))
   ```
   
   4. **Zero-Copy Data Sharing**:
   ```clojure
   (let [shared-buffer (create-array data dims)
         ptr-container (mem/alloc-instance ::mem/pointer)]
     (af-get-device-ptr ptr-container shared-buffer)
     (let [device-memory (mem/read-ptr ptr-container)]
       ;; Share with multiple operations
       (operation-1 device-memory)
       (operation-2 device-memory)
       (af-unlock-array shared-buffer)))
   ```
   
   Best Practices:
   
   1. **Always Unlock**:
      Use try/finally to ensure unlock
   
   2. **Don't Use After Unlock**:
      Pointer becomes invalid after unlock
   
   3. **Minimize Lock Duration**:
      Lock just before use, unlock immediately after
   
   4. **Check Backend**:
      Handle pointer type appropriately per backend
   
   5. **Synchronization**:
      May need GPU sync before external use
   
   Common Pitfalls:
   
   1. **Using After Unlock**:
      ```clojure
      ;; DANGER: Invalid pointer!
      (let [ptr-container (mem/alloc-instance ::mem/pointer)]
        (af-get-device-ptr ptr-container arr)
        (af-unlock-array arr)  ; Pointer now invalid!
        (use-pointer (mem/read-ptr ptr-container))) ; CRASH
      ```
   
   2. **Forgetting to Unlock**:
      ```clojure
      ;; Memory leak!
      (let [ptr-container (mem/alloc-instance ::mem/pointer)]
        (af-get-device-ptr ptr-container arr)
        ;; Use pointer
        ;; Forgot af-unlock-array!
      )
      ```
   
   3. **Wrong Cast (OpenCL)**:
      ```clojure
      ;; Wrong for OpenCL backend
      (let [ptr-container (mem/alloc-instance ::mem/pointer)]
        (af-get-device-ptr ptr-container arr)
        ;; Need to cast to cl_mem in C/C++ code
        (use-as-opencl-buffer (mem/read-ptr ptr-container)))
      ```
   
   Performance Considerations:
   
   - **Lock overhead**: Minimal (flag set)
   - **Synchronization**: May trigger GPU sync
   - **Memory**: Locked buffers can't be reused
   - **Fragmentation**: Too many locks hurt performance
   
   Thread Safety:
   - Get pointer and unlock on same thread
   - Don't share pointers between threads without sync
   - Each thread should manage its own locks
   
   Comparison with af_get_raw_ptr:
   
   | Function           | Locks Array | Safe | Use Case                    |
   |--------------------|-------------|------|-----------------------------|
   | af_get_device_ptr  | Yes         | Safe | External library interop    |
   | af_get_raw_ptr     | No          | RISKY| Advanced low-level access   |
   
   Always prefer af_get_device_ptr unless you have specific reasons to use
   the raw pointer (which doesn't lock).
   
   See Also:
   - af_unlock_array: Release the lock
   - af_is_locked_array: Check if locked
   - af_lock_array: Lock without getting pointer
   - af_get_raw_ptr: Get pointer without locking (unsafe)
   - af_device_array: Create array from device pointer"
  "af_get_device_ptr" [::mem/pointer ::mem/pointer] ::mem/int)

;; Deprecated functions (for backward compatibility)

;; af_err af_lock_device_ptr(const af_array arr)
(defcfn af-lock-device-ptr
  "Lock the device buffer in the memory manager.
   
   DEPRECATED: Use af_lock_array instead (AF_API_VERSION >= 33)
   
   This function is a legacy wrapper around af_lock_array and is maintained
   only for backward compatibility. New code should use af_lock_array.
   
   Parameters:
   - arr: af_array handle to lock
   
   Returns:
   ArrayFire error code
   
   See Also:
   - af_lock_array: Preferred function (AF_API_VERSION >= 33)"
  "af_lock_device_ptr" [::mem/pointer] ::mem/int)

;; af_err af_unlock_device_ptr(const af_array arr)
(defcfn af-unlock-device-ptr
  "Unlock device buffer in the memory manager.
   
   DEPRECATED: Use af_unlock_array instead (AF_API_VERSION >= 33)
   
   This function is a legacy wrapper around af_unlock_array and is maintained
   only for backward compatibility. New code should use af_unlock_array.
   
   Parameters:
   - arr: af_array handle to unlock
   
   Returns:
   ArrayFire error code
   
   See Also:
   - af_unlock_array: Preferred function (AF_API_VERSION >= 33)"
  "af_unlock_device_ptr" [::mem/pointer] ::mem/int)
