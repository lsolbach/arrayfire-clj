(ns org.soulspace.arrayfire.ffi.c-api.opencl
  "FFI bindings for ArrayFire OpenCL backend interoperability.
   
   This namespace provides low-level bindings to ArrayFire's OpenCL backend API,
   enabling direct access to OpenCL contexts, command queues, and device IDs.
   This allows integration with existing OpenCL code and fine-grained control
   over device and context management.
   
   ## OpenCL Backend Overview
   
   ArrayFire's OpenCL backend exposes the underlying OpenCL infrastructure,
   allowing you to:
   
   - Access ArrayFire's OpenCL context and command queue
   - Integrate with existing OpenCL code
   - Create ArrayFire arrays from OpenCL cl_mem buffers
   - Manage multiple devices and contexts
   - Query device type and platform information
   
   ## Device Type Enum
   
   ```
   AFCL_DEVICE_TYPE_CPU     - OpenCL CPU device
   AFCL_DEVICE_TYPE_GPU     - OpenCL GPU device
   AFCL_DEVICE_TYPE_ACC     - OpenCL Accelerator device
   AFCL_DEVICE_TYPE_UNKNOWN - Unknown device type
   ```
   
   ## Platform Enum
   
   ```
   AFCL_PLATFORM_AMD     - AMD platform
   AFCL_PLATFORM_APPLE   - Apple platform
   AFCL_PLATFORM_INTEL   - Intel platform
   AFCL_PLATFORM_NVIDIA  - NVIDIA platform
   AFCL_PLATFORM_BEIGNET - Beignet platform
   AFCL_PLATFORM_POCL    - Portable Computing Language (POCL)
   AFCL_PLATFORM_UNKNOWN - Unknown platform
   ```
   
   ## Common Usage Pattern
   
   ```clojure
   ;; Get OpenCL context for interop
   (let [ctx-ptr (mem/alloc 8)]
     (afcl-get-context ctx-ptr false)
     (let [ctx (mem/read-long ctx-ptr 0)]
       ;; Use OpenCL context with custom kernels
       ))
   
   ;; Get command queue
   (let [queue-ptr (mem/alloc 8)]
     (afcl-get-queue queue-ptr false)
     (let [queue (mem/read-long queue-ptr 0)]
       ;; Enqueue custom OpenCL operations
       ))
   
   ;; Query device information
   (let [dtype-ptr (mem/alloc 4)]
     (afcl-get-device-type dtype-ptr)
     (let [dtype (mem/read-int dtype-ptr 0)]
       (case dtype
         2 (println \"CPU device\")
         4 (println \"GPU device\")
         (println \"Other device\"))))
   ```
   
   ## Integration with Custom OpenCL Code
   
   ArrayFire's OpenCL backend allows seamless integration:
   
   1. **Context Sharing**: Use ArrayFire's context for custom kernels
   2. **Queue Reuse**: Submit operations to ArrayFire's command queue
   3. **Memory Interop**: Create af::array from cl_mem buffers
   4. **Multi-Context**: Manage multiple contexts for different devices
   
   ## Important Notes
   
   - Functions only work when AF_BACKEND_OPENCL is the active backend
   - Returns AF_ERR_NOT_SUPPORTED if OpenCL backend is not active
   - Set `retain` parameter to true if passing objects to cl:: constructors
   - User is responsible for releasing user-provided contexts
   - ArrayFire manages its own internal OpenCL objects
   
   See also:
   - af/device.h for backend management
   - af/array.h for array creation from cl_mem"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.loader]))

;;;
;;; Device Type Constants (afcl_device_type enum)
;;;

(def ^:const AFCL_DEVICE_TYPE_CPU 2)     ; CL_DEVICE_TYPE_CPU
(def ^:const AFCL_DEVICE_TYPE_GPU 4)     ; CL_DEVICE_TYPE_GPU  
(def ^:const AFCL_DEVICE_TYPE_ACC 8)     ; CL_DEVICE_TYPE_ACCELERATOR
(def ^:const AFCL_DEVICE_TYPE_UNKNOWN -1)

;;;
;;; Platform Constants (afcl_platform enum)
;;;

(def ^:const AFCL_PLATFORM_AMD 0)
(def ^:const AFCL_PLATFORM_APPLE 1)
(def ^:const AFCL_PLATFORM_INTEL 2)
(def ^:const AFCL_PLATFORM_NVIDIA 3)
(def ^:const AFCL_PLATFORM_BEIGNET 4)
(def ^:const AFCL_PLATFORM_POCL 5)
(def ^:const AFCL_PLATFORM_UNKNOWN -1)

;;;
;;; OpenCL Interop Functions
;;;

;; af_err afcl_get_context(cl_context *ctx, const bool retain)
(defcfn afcl-get-context
  "Get a handle to ArrayFire's OpenCL context.
   
   Returns the cl_context currently being used by ArrayFire for OpenCL
   operations. This allows integration with existing OpenCL code.
   
   Parameters:
   
   - ctx: cl_context* output pointer
     * Pointer to receive the OpenCL context handle
     * Type: void* (opaque OpenCL context)
     * Valid only while ArrayFire session is active
   
   - retain: bool, whether to retain the context
     * true: calls clRetainContext before returning
     * false: returns without incrementing reference count
     * Set to true if passing to cl::Context constructor
   
   Returns:
   AF_SUCCESS (0) on success, error code otherwise:
   - AF_ERR_NOT_SUPPORTED: OpenCL backend not active
   - AF_ERR_RUNTIME: OpenCL runtime error
   
   Context Lifetime:
   
   The returned context is owned by ArrayFire. If retain=false, the
   context remains valid until ArrayFire is shut down or the device
   changes. If retain=true, you must call clReleaseContext when done.
   
   Usage:
   ```clojure
   ;; Get context without retaining
   (let [ctx-ptr (mem/alloc 8)]
     (afcl-get-context ctx-ptr false)
     (let [ctx (mem/read-long ctx-ptr 0)]
       ;; Use context for ArrayFire session
       ;; No need to release
       ))
   
   ;; Get context with retain (for cl::Context)
   (let [ctx-ptr (mem/alloc 8)]
     (afcl-get-context ctx-ptr true)
     (let [ctx (mem/read-long ctx-ptr 0)]
       ;; Pass to cl::Context constructor
       ;; Must call clReleaseContext later
       ))
   ```
   
   Integration Example:
   ```clojure
   ;; Share context with custom OpenCL code
   (defn run-custom-kernel [input-array]
     (let [ctx-ptr (mem/alloc 8)
           queue-ptr (mem/alloc 8)]
       (afcl-get-context ctx-ptr false)
       (afcl-get-queue queue-ptr false)
       (let [ctx (mem/read-long ctx-ptr 0)
             queue (mem/read-long queue-ptr 0)]
         ;; Create kernel using ArrayFire's context
         ;; Enqueue operations on ArrayFire's queue
         ;; Ensures correct synchronization
         )))
   ```
   
   See also:
   - afcl-get-queue: Get command queue
   - afcl-get-device-id: Get current device ID"
  "afcl_get_context" [::mem/pointer ::mem/int] ::mem/int)

;; af_err afcl_get_queue(cl_command_queue *queue, const bool retain)
(defcfn afcl-get-queue
  "Get a handle to ArrayFire's OpenCL command queue.
   
   Returns the cl_command_queue currently being used by ArrayFire.
   All ArrayFire operations are enqueued to this queue, enabling
   proper synchronization with custom OpenCL operations.
   
   Parameters:
   
   - queue: cl_command_queue* output pointer
     * Pointer to receive the command queue handle
     * Type: void* (opaque OpenCL queue)
     * Associated with ArrayFire's current context
   
   - retain: bool, whether to retain the queue
     * true: calls clRetainCommandQueue before returning
     * false: returns without incrementing reference count
     * Set to true if passing to cl::CommandQueue constructor
   
   Returns:
   AF_SUCCESS (0) on success, error code otherwise:
   - AF_ERR_NOT_SUPPORTED: OpenCL backend not active
   - AF_ERR_RUNTIME: OpenCL runtime error
   
   Queue Lifetime:
   
   The returned queue is owned by ArrayFire. If retain=false, valid
   until device changes or shutdown. If retain=true, you must call
   clReleaseCommandQueue when done.
   
   Usage:
   ```clojure
   ;; Get queue for synchronization
   (let [queue-ptr (mem/alloc 8)]
     (afcl-get-queue queue-ptr false)
     (let [queue (mem/read-long queue-ptr 0)]
       ;; Enqueue custom operations
       ;; Will synchronize with ArrayFire ops
       ))
   ```
   
   Synchronization Pattern:
   ```clojure
   (defn run-and-sync [af-array]
     ;; ArrayFire operations
     (let [result (af/sin af-array)]
       ;; Get queue for custom work
       (let [queue-ptr (mem/alloc 8)]
         (afcl-get-queue queue-ptr false)
         (let [queue (mem/read-long queue-ptr 0)]
           ;; Custom OpenCL kernel
           ;; Uses same queue = automatic sync
           ;; clEnqueueNDRangeKernel(queue, ...)
           result))))
   ```
   
   See also:
   - afcl-get-context: Get OpenCL context
   - clFinish/clFlush: Queue synchronization"
  "afcl_get_queue" [::mem/pointer ::mem/int] ::mem/int)

;; af_err afcl_get_device_id(cl_device_id *id)
(defcfn afcl-get-device-id
  "Get the OpenCL device ID for ArrayFire's current active device.
   
   Returns the cl_device_id of the device currently active in ArrayFire.
   Useful for querying device capabilities or setting up custom contexts.
   
   Parameters:
   
   - id: cl_device_id* output pointer
     * Pointer to receive the device ID
     * Type: void* (opaque OpenCL device identifier)
     * Identifies specific OpenCL device
   
   Returns:
   AF_SUCCESS (0) on success, error code otherwise:
   - AF_ERR_NOT_SUPPORTED: OpenCL backend not active
   - AF_ERR_DEVICE: Invalid device
   
   Usage:
   ```clojure
   ;; Get current device ID
   (let [id-ptr (mem/alloc 8)]
     (afcl-get-device-id id-ptr)
     (let [dev-id (mem/read-long id-ptr 0)]
       ;; Query device properties
       ;; clGetDeviceInfo(dev-id, ...)
       ))
   ```
   
   Query Device Info:
   ```clojure
   (defn get-device-name []
     (let [id-ptr (mem/alloc 8)]
       (afcl-get-device-id id-ptr)
       (let [dev-id (mem/read-long id-ptr 0)]
         ;; Use clGetDeviceInfo to query:
         ;; - CL_DEVICE_NAME
         ;; - CL_DEVICE_MAX_COMPUTE_UNITS
         ;; - CL_DEVICE_GLOBAL_MEM_SIZE
         ;; - etc.
         )))
   ```
   
   See also:
   - afcl-set-device-id: Set active device
   - af-set-device: ArrayFire device management"
  "afcl_get_device_id" [::mem/pointer] ::mem/int)

;; af_err afcl_set_device_id(cl_device_id id)
(defcfn afcl-set-device-id
  "Set ArrayFire's active device using an OpenCL device ID.
   
   Changes the current active device to the one specified by the
   cl_device_id. The device must be available in the system.
   
   Parameters:
   
   - id: cl_device_id (void*)
     * OpenCL device identifier
     * Must be valid device from clGetDeviceIDs
     * Device must support required features
   
   Returns:
   AF_SUCCESS (0) on success, error code otherwise:
   - AF_ERR_NOT_SUPPORTED: OpenCL backend not active
   - AF_ERR_DEVICE: Invalid or unavailable device
   - AF_ERR_ARG: Invalid device ID
   
   Usage:
   ```clojure
   ;; Switch to specific OpenCL device
   (let [dev-id (get-opencl-device-by-name \"GPU\")]
     (afcl-set-device-id dev-id))
   ```
   
   Multi-Device Pattern:
   ```clojure
   (defn run-on-different-devices [gpu-id cpu-id]
     ;; GPU computation
     (afcl-set-device-id gpu-id)
     (let [gpu-result (af/matmul a b)]
       ;; CPU computation
       (afcl-set-device-id cpu-id)
       (let [cpu-result (af/fft data)]
         [gpu-result cpu-result])))
   ```
   
   Important: Changing devices may invalidate existing array references.
   Ensure proper synchronization before switching.
   
   See also:
   - afcl-get-device-id: Query current device
   - af-set-device: Set by ArrayFire device index"
  "afcl_set_device_id" [::mem/pointer] ::mem/int)

;; af_err afcl_add_device_context(cl_device_id dev, cl_context ctx, cl_command_queue que)
(defcfn afcl-add-device-context
  "Add user-provided OpenCL context and queue to ArrayFire's device pool.
   
   Registers an externally created OpenCL context with ArrayFire, allowing
   ArrayFire to use your custom OpenCL setup. This enables:
   
   - Integration with existing OpenCL applications
   - Custom context creation with specific properties
   - Sharing contexts between multiple libraries
   - Fine-grained control over device management
   
   Parameters:
   
   - dev: cl_device_id (void*)
     * OpenCL device for this context
     * Must match the device used to create ctx
     * Device must be available in system
   
   - ctx: cl_context (void*)
     * User-created OpenCL context
     * ArrayFire will use but not own this context
     * Must remain valid during ArrayFire operations
   
   - que: cl_command_queue (void*), can be NULL
     * User-created command queue (optional)
     * If NULL, ArrayFire creates queue for you
     * If provided, must be compatible with ctx and dev
   
   Returns:
   AF_SUCCESS (0) on success, error code otherwise:
   - AF_ERR_NOT_SUPPORTED: OpenCL backend not active
   - AF_ERR_ARG: Invalid device, context, or queue
   - AF_ERR_DEVICE: Device/context mismatch
   
   Memory Management:
   
   **CRITICAL**: ArrayFire does NOT take ownership of user objects.
   You must:
   - Keep context and queue alive during use
   - Call afcl-delete-device-context before releasing
   - Release context and queue yourself afterward
   
   Usage:
   ```clojure
   ;; Add custom context to ArrayFire
   (let [dev-id (create-opencl-device)
         ctx (create-opencl-context dev-id)
         queue (create-opencl-queue ctx dev-id)]
     ;; Register with ArrayFire
     (afcl-add-device-context dev-id ctx queue)
     
     ;; Now ArrayFire uses your context
     (let [result (af/matmul a b)]
       ;; Operations use your queue
       result)
     
     ;; When done, remove from pool
     (afcl-delete-device-context dev-id ctx)
     
     ;; Now safe to release OpenCL objects
     ;; clReleaseCommandQueue(queue)
     ;; clReleaseContext(ctx)
     )
   ```
   
   Multi-Library Integration:
   ```clojure
   ;; Share context with other OpenCL library
   (let [ctx (other-lib/get-context)
         dev (other-lib/get-device)]
     ;; Use same context in ArrayFire
     (afcl-add-device-context dev ctx nil)
     
     ;; Both libraries use same context
     (let [af-result (af/process data)
           lib-result (other-lib/process data)]
       ;; Implicit synchronization
       [af-result lib-result]))
   ```
   
   See also:
   - afcl-delete-device-context: Remove custom context
   - afcl-set-device-context: Set active device/context pair"
  "afcl_add_device_context" [::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; af_err afcl_set_device_context(cl_device_id dev, cl_context ctx)
(defcfn afcl-set-device-context
  "Set the active device using OpenCL device ID and context pair.
   
   Switches ArrayFire to use a specific device/context combination that
   has been previously registered with afcl-add-device-context.
   
   Parameters:
   
   - dev: cl_device_id (void*)
     * OpenCL device to activate
     * Must be previously registered
     * Must match the context
   
   - ctx: cl_context (void*)
     * OpenCL context to activate
     * Must be previously registered with afcl-add-device-context
     * Must be compatible with device
   
   Returns:
   AF_SUCCESS (0) on success, error code otherwise:
   - AF_ERR_NOT_SUPPORTED: OpenCL backend not active
   - AF_ERR_ARG: Invalid device or context
   - AF_ERR_DEVICE: Device/context not registered or mismatch
   
   Usage:
   ```clojure
   ;; Register multiple contexts
   (afcl-add-device-context gpu-dev gpu-ctx nil)
   (afcl-add-device-context cpu-dev cpu-ctx nil)
   
   ;; Switch between them
   (afcl-set-device-context gpu-dev gpu-ctx)
   (let [gpu-result (af/fft data)])
   
   (afcl-set-device-context cpu-dev cpu-ctx)
   (let [cpu-result (af/sort data)])
   ```
   
   Multi-Context Application:
   ```clojure
   (defn process-with-different-contexts []
     ;; Setup contexts
     (let [viz-ctx (create-viz-context)    ; Context with GL sharing
           comp-ctx (create-compute-context)] ; Compute-only context
       
       ;; Register both
       (afcl-add-device-context gpu-dev viz-ctx nil)
       (afcl-add-device-context gpu-dev comp-ctx nil)
       
       ;; Heavy computation on compute context
       (afcl-set-device-context gpu-dev comp-ctx)
       (let [processed (af/complex-computation data)])
       
       ;; Visualization on viz context (OpenGL interop)
       (afcl-set-device-context gpu-dev viz-ctx)
       (render-with-opengl processed)))
   ```
   
   See also:
   - afcl-add-device-context: Register device/context
   - afcl-get-context: Query active context"
  "afcl_set_device_context" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err afcl_delete_device_context(cl_device_id dev, cl_context ctx)
(defcfn afcl-delete-device-context
  "Remove user-provided OpenCL context from ArrayFire's device pool.
   
   Unregisters a device/context pair that was previously added with
   afcl-add-device-context. After this call, ArrayFire will no longer
   use the context, and you can safely release the OpenCL objects.
   
   Parameters:
   
   - dev: cl_device_id (void*)
     * OpenCL device ID
     * Must match device from afcl-add-device-context
   
   - ctx: cl_context (void*)
     * OpenCL context to remove
     * Must match context from afcl-add-device-context
     * Must not be currently active
   
   Returns:
   AF_SUCCESS (0) on success, error code otherwise:
   - AF_ERR_NOT_SUPPORTED: OpenCL backend not active
   - AF_ERR_ARG: Invalid device or context
   - AF_ERR_DEVICE: Context not found or currently active
   
   Memory Management:
   
   This function only removes the context from ArrayFire's pool.
   You are still responsible for releasing the OpenCL objects:
   ```clojure
   ;; Proper cleanup sequence
   (afcl-delete-device-context dev ctx)  ; Remove from ArrayFire
   ;; (clReleaseCommandQueue queue)      ; Release queue
   ;; (clReleaseContext ctx)              ; Release context
   ```
   
   Usage:
   ```clojure
   ;; Complete lifecycle
   (let [dev (get-opencl-device)
         ctx (create-context dev)
         queue (create-queue ctx dev)]
     
     ;; Register with ArrayFire
     (afcl-add-device-context dev ctx queue)
     
     ;; Use ArrayFire with custom context
     (process-with-arrayfire data)
     
     ;; Cleanup sequence
     (afcl-delete-device-context dev ctx)
     
     ;; Now safe to release OpenCL objects
     ;; Release queue and context
     )
   ```
   
   Error Handling:
   ```clojure
   (try
     (afcl-add-device-context dev ctx queue)
     (process-data)
     (finally
       ;; Always cleanup
       (afcl-delete-device-context dev ctx)))
   ```
   
   Important: Cannot delete the currently active context. Switch to
   a different device/context before calling this function.
   
   See also:
   - afcl-add-device-context: Register context
   - afcl-set-device-context: Change active context"
  "afcl_delete_device_context" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err afcl_get_device_type(afcl_device_type *res)
(defcfn afcl-get-device-type
  "Get the OpenCL device type of the current device.
   
   Returns whether the current device is a CPU, GPU, or accelerator.
   Useful for adapting algorithms based on device characteristics.
   
   Parameters:
   
   - res: afcl_device_type* output pointer (int*)
     * Receives device type enum value
     * One of: AFCL_DEVICE_TYPE_* constants
   
   Returns:
   AF_SUCCESS (0) on success, error code otherwise:
   - AF_ERR_NOT_SUPPORTED: OpenCL backend not active
   - AF_ERR_DEVICE: Cannot determine device type
   
   Device Types:
   
   - AFCL_DEVICE_TYPE_CPU (2): CPU device
     * Good for: Small data, complex algorithms
     * Characteristics: High memory, low parallelism
   
   - AFCL_DEVICE_TYPE_GPU (4): GPU device
     * Good for: Large parallel workloads
     * Characteristics: High throughput, limited memory per work-item
   
   - AFCL_DEVICE_TYPE_ACC (8): Accelerator device
     * Good for: Specialized computations (e.g., FPGAs)
     * Characteristics: Varies by device
   
   - AFCL_DEVICE_TYPE_UNKNOWN (-1): Unknown type
   
   Usage:
   ```clojure
   ;; Query device type
   (let [dtype-ptr (mem/alloc 4)]
     (afcl-get-device-type dtype-ptr)
     (let [dtype (mem/read-int dtype-ptr 0)]
       (case dtype
         2 (println \"Running on CPU\")
         4 (println \"Running on GPU\")
         8 (println \"Running on Accelerator\")
         (println \"Unknown device type\"))))
   ```
   
   Adaptive Algorithm Selection:
   ```clojure
   (defn adaptive-fft [data]
     (let [dtype-ptr (mem/alloc 4)]
       (afcl-get-device-type dtype-ptr)
       (let [dtype (mem/read-int dtype-ptr 0)]
         (if (= dtype AFCL_DEVICE_TYPE_GPU)
           ;; GPU: Use large batch sizes
           (af/fft data :batch-size 1024)
           ;; CPU: Use smaller batches
           (af/fft data :batch-size 64)))))
   ```
   
   See also:
   - afcl-get-platform: Get OpenCL vendor platform
   - CL_DEVICE_TYPE from OpenCL spec"
  "afcl_get_device_type" [::mem/pointer] ::mem/int)

;; af_err afcl_get_platform(afcl_platform *res)
(defcfn afcl-get-platform
  "Get the OpenCL platform vendor of the current device.
   
   Returns which OpenCL implementation/vendor is being used. Different
   platforms may have different performance characteristics and features.
   
   Parameters:
   
   - res: afcl_platform* output pointer (int*)
     * Receives platform enum value
     * One of: AFCL_PLATFORM_* constants
   
   Returns:
   AF_SUCCESS (0) on success, error code otherwise:
   - AF_ERR_NOT_SUPPORTED: OpenCL backend not active
   - AF_ERR_DEVICE: Cannot determine platform
   
   Platform Values:
   
   - AFCL_PLATFORM_AMD (0): AMD OpenCL
   - AFCL_PLATFORM_APPLE (1): Apple OpenCL
   - AFCL_PLATFORM_INTEL (2): Intel OpenCL
   - AFCL_PLATFORM_NVIDIA (3): NVIDIA OpenCL
   - AFCL_PLATFORM_BEIGNET (4): Beignet (open-source Intel)
   - AFCL_PLATFORM_POCL (5): POCL (Portable OpenCL)
   - AFCL_PLATFORM_UNKNOWN (-1): Unknown platform
   
   Usage:
   ```clojure
   ;; Query platform
   (let [plat-ptr (mem/alloc 4)]
     (afcl-get-platform plat-ptr)
     (let [platform (mem/read-int plat-ptr 0)]
       (case platform
         0 (println \"AMD OpenCL\")
         1 (println \"Apple OpenCL\")
         2 (println \"Intel OpenCL\")
         3 (println \"NVIDIA OpenCL\")
         4 (println \"Beignet\")
         5 (println \"POCL\")
         (println \"Unknown platform\"))))
   ```
   
   Platform-Specific Optimizations:
   ```clojure
   (defn optimized-conv [input kernel]
     (let [plat-ptr (mem/alloc 4)]
       (afcl-get-platform plat-ptr)
       (let [platform (mem/read-int plat-ptr 0)]
         (cond
           ;; NVIDIA: Use larger work groups
           (= platform AFCL_PLATFORM_NVIDIA)
           (af/convolve input kernel :work-group 256)
           
           ;; Intel: Optimize for cache
           (= platform AFCL_PLATFORM_INTEL)
           (af/convolve input kernel :work-group 64)
           
           ;; Default
           :else
           (af/convolve input kernel)))))
   ```
   
   Device Information:
   ```clojure
   (defn print-device-info []
     (let [dtype-ptr (mem/alloc 4)
           plat-ptr (mem/alloc 4)]
       (afcl-get-device-type dtype-ptr)
       (afcl-get-platform plat-ptr)
       (let [dtype (mem/read-int dtype-ptr 0)
             platform (mem/read-int plat-ptr 0)]
         (println \"Device Type:\" dtype)
         (println \"Platform:\" platform))))
   ```
   
   See also:
   - afcl-get-device-type: Query device type
   - CL_PLATFORM_VENDOR from OpenCL spec"
  "afcl_get_platform" [::mem/pointer] ::mem/int)
