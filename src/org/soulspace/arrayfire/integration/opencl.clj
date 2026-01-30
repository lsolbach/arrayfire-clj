(ns org.soulspace.arrayfire.integration.opencl
  "Integration of the ArrayFire OpenCL backend FFI bindings with the error
   handling and resource management on the JVM.
   
   This namespace provides high-level Clojure functions for ArrayFire's OpenCL
   backend, enabling integration with existing OpenCL code and fine-grained
   control over device and context management.
   
   ## OpenCL Backend Overview
   
   ArrayFire's OpenCL backend allows:
   - Access to underlying OpenCL context and command queue
   - Integration with custom OpenCL kernels
   - Multi-device and multi-context management
   - Platform and device type queries
   
   ## Prerequisites
   
   These functions only work when AF_BACKEND_OPENCL is the active backend:
   ```clojure
   (require '[org.soulspace.arrayfire.integration.device :as device])
   (device/set-backend! device/AF_BACKEND_OPENCL)
   ```
   
   ## Common Usage Patterns
   
   **Get OpenCL Context:**
   ```clojure
   (let [ctx (get-context)]
     ;; Use context with custom OpenCL code
     ;; Context is valid until device changes
     )
   ```
   
   **Query Device Information:**
   ```clojure
   (let [dtype (get-device-type)
         platform (get-platform)]
     (println \"Device:\" dtype)
     (println \"Platform:\" platform))
   ```
   
   **Custom Context Management:**
   ```clojure
   ;; Add your own OpenCL context
   (add-device-context! my-dev my-ctx my-queue)
   
   ;; Switch to it
   (set-device-context! my-dev my-ctx)
   
   ;; Remove when done
   (delete-device-context! my-dev my-ctx)
   ```
   
   ## Important Notes
   
   - All functions require OpenCL backend to be active
   - User-provided contexts must be managed by user
   - ArrayFire-owned objects don't need manual release
   - Set retain=true when passing to cl::Context/Queue constructors
   
   See also:
   - org.soulspace.arrayfire.integration.device: Backend management"
  (:require [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.opencl :as opencl]
            [org.soulspace.arrayfire.integration.jvm-integration :as jvm]))

;;;
;;; Device Type and Platform Constants
;;;

(def DEVICE-TYPE-CPU opencl/AFCL_DEVICE_TYPE_CPU)
(def DEVICE-TYPE-GPU opencl/AFCL_DEVICE_TYPE_GPU)
(def DEVICE-TYPE-ACC opencl/AFCL_DEVICE_TYPE_ACC)
(def DEVICE-TYPE-UNKNOWN opencl/AFCL_DEVICE_TYPE_UNKNOWN)

(def PLATFORM-AMD opencl/AFCL_PLATFORM_AMD)
(def PLATFORM-APPLE opencl/AFCL_PLATFORM_APPLE)
(def PLATFORM-INTEL opencl/AFCL_PLATFORM_INTEL)
(def PLATFORM-NVIDIA opencl/AFCL_PLATFORM_NVIDIA)
(def PLATFORM-BEIGNET opencl/AFCL_PLATFORM_BEIGNET)
(def PLATFORM-POCL opencl/AFCL_PLATFORM_POCL)
(def PLATFORM-UNKNOWN opencl/AFCL_PLATFORM_UNKNOWN)

;;;
;;; OpenCL Context and Queue Access
;;;

(defn get-context
  "Get ArrayFire's current OpenCL context.
   
   Returns the cl_context handle currently being used by ArrayFire.
   Useful for integration with existing OpenCL code.
   
   Parameters:
   - retain: Boolean, whether to retain the context (default false)
     * true: increments reference count (for cl::Context)
     * false: no reference count change
   
   Returns:
   Long value representing cl_context handle (opaque pointer)
   
   Context Lifetime:
   - If retain=false: valid until device changes or shutdown
   - If retain=true: must call clReleaseContext when done
   
   Examples:
   ```clojure
   ;; Get context for current session
   (let [ctx (get-context)]
     ;; Use with custom OpenCL operations
     ;; No need to release
     ctx)
   
   ;; Get context with retain for cl::Context
   (let [ctx (get-context true)]
     ;; Pass to cl::Context constructor
     ;; Must call clReleaseContext later
     ctx)
   
   ;; Share context with custom kernel
   (defn run-custom-kernel [input]
     (let [ctx (get-context)
           queue (get-queue)]
       ;; Create and run custom OpenCL kernel
       ;; using ArrayFire's context and queue
       result))
   ```
   
   See also:
   - get-queue: Get command queue
   - get-device-id: Get current device ID"
  ([]
   (get-context false))
  ([retain]
   (let [ctx-ptr (mem/alloc 8)]
     (jvm/check! (opencl/afcl-get-context ctx-ptr (if retain 1 0))
                 "afcl-get-context")
     (mem/read-long ctx-ptr 0))))

(defn get-queue
  "Get ArrayFire's current OpenCL command queue.
   
   Returns the cl_command_queue handle currently being used.
   All ArrayFire operations are enqueued to this queue.
   
   Parameters:
   - retain: Boolean, whether to retain the queue (default false)
     * true: increments reference count (for cl::CommandQueue)
     * false: no reference count change
   
   Returns:
   Long value representing cl_command_queue handle (opaque pointer)
   
   Queue Lifetime:
   - If retain=false: valid until device changes or shutdown
   - If retain=true: must call clReleaseCommandQueue when done
   
   Examples:
   ```clojure
   ;; Get queue for synchronization
   (let [queue (get-queue)]
     ;; Enqueue custom operations
     ;; Will synchronize with ArrayFire
     queue)
   
   ;; Ensure synchronization
   (defn sync-and-process [af-result]
     (let [queue (get-queue)]
       ;; clFinish(queue) ensures ArrayFire ops complete
       ;; before proceeding
       af-result))
   ```
   
   Synchronization Pattern:
   ```clojure
   (defn mixed-operations [data]
     ;; ArrayFire operation
     (let [af-result (process-with-arrayfire data)
           queue (get-queue)]
       ;; Custom OpenCL operation on same queue
       ;; Automatically synchronized
       (custom-opencl-kernel queue af-result)))
   ```
   
   See also:
   - get-context: Get OpenCL context"
  ([]
   (get-queue false))
  ([retain]
   (let [queue-ptr (mem/alloc 8)]
     (jvm/check! (opencl/afcl-get-queue queue-ptr (if retain 1 0))
                 "afcl-get-queue")
     (mem/read-long queue-ptr 0))))

(defn get-device-id
  "Get the OpenCL device ID for ArrayFire's current device.
   
   Returns the cl_device_id of the currently active device.
   Useful for querying device properties.
   
   Returns:
   Long value representing cl_device_id handle (opaque pointer)
   
   Examples:
   ```clojure
   ;; Get current device ID
   (let [dev-id (get-device-id)]
     ;; Query device properties
     ;; clGetDeviceInfo(dev-id, CL_DEVICE_NAME, ...)
     dev-id)
   
   ;; Check device capabilities
   (defn check-double-support []
     (let [dev-id (get-device-id)]
       ;; Query CL_DEVICE_DOUBLE_FP_CONFIG
       ;; to check double precision support
       ))
   ```
   
   See also:
   - set-device-id!: Change active device
   - get-device-type: Query device type"
  []
  (let [id-ptr (mem/alloc 8)]
    (jvm/check! (opencl/afcl-get-device-id id-ptr)
                "afcl-get-device-id")
    (mem/read-long id-ptr 0)))

;;;
;;; Device Management
;;;

(defn set-device-id!
  "Set ArrayFire's active device using an OpenCL device ID.
   
   Changes the current device to the one specified by the cl_device_id.
   
   Parameters:
   - id: cl_device_id (long), OpenCL device identifier
   
   Returns:
   nil
   
   Important: Changing devices may invalidate existing arrays.
   Ensure proper synchronization before switching.
   
   Examples:
   ```clojure
   ;; Switch to specific OpenCL device
   (let [gpu-id (find-gpu-device)]
     (set-device-id! gpu-id))
   
   ;; Multi-device computation
   (defn compute-on-multiple-devices [gpu-id cpu-id]
     ;; GPU work
     (set-device-id! gpu-id)
     (let [gpu-result (heavy-computation data)])
     
     ;; CPU work
     (set-device-id! cpu-id)
     (let [cpu-result (light-computation data)])
     
     [gpu-result cpu-result])
   ```
   
   See also:
   - get-device-id: Query current device"
  [id]
  (let [id-seg (mem/as-segment (long id))]
    (jvm/check! (opencl/afcl-set-device-id id-seg)
                "afcl-set-device-id")
    nil))

;;;
;;; Custom Context Management
;;;

(defn add-device-context!
  "Add user-provided OpenCL context to ArrayFire's device pool.
   
   Registers an externally created OpenCL context with ArrayFire,
   allowing ArrayFire to use your custom OpenCL setup.
   
   Parameters:
   - dev: cl_device_id (long), OpenCL device
   - ctx: cl_context (long), user-created context
   - queue: cl_command_queue (long or nil), optional queue
     * If nil, ArrayFire creates queue automatically
   
   Returns:
   nil
   
   Memory Management:
   **CRITICAL**: ArrayFire does NOT own user objects.
   - Keep context and queue valid during use
   - Call delete-device-context! before releasing
   - You must release OpenCL objects yourself
   
   Examples:
   ```clojure
   ;; Add custom context
   (let [dev (create-opencl-device)
         ctx (create-opencl-context dev)
         queue (create-opencl-queue ctx dev)]
     ;; Register with ArrayFire
     (add-device-context! dev ctx queue)
     
     ;; ArrayFire now uses your context
     (process-with-arrayfire data)
     
     ;; Cleanup
     (delete-device-context! dev ctx)
     ;; Now safe to release context and queue
     )
   
   ;; Share context with another library
   (let [ctx (other-lib/get-context)
         dev (other-lib/get-device)]
     (add-device-context! dev ctx nil)
     ;; Both libraries share the context
     )
   ```
   
   See also:
   - delete-device-context!: Remove custom context
   - set-device-context!: Set active device/context"
  [dev ctx queue]
  (let [dev-seg (mem/as-segment (long dev))
        ctx-seg (mem/as-segment (long ctx))
        queue-seg (if queue
                    (mem/as-segment (long queue))
                    jvm/null-ptr)]
    (jvm/check! (opencl/afcl-add-device-context dev-seg ctx-seg queue-seg)
                "afcl-add-device-context")
    nil))

(defn set-device-context!
  "Set active device using OpenCL device ID and context pair.
   
   Switches ArrayFire to use a specific device/context combination
   that was previously registered with add-device-context!.
   
   Parameters:
   - dev: cl_device_id (long), device to activate
   - ctx: cl_context (long), context to activate
   
   Returns:
   nil
   
   Examples:
   ```clojure
   ;; Register and switch between contexts
   (add-device-context! gpu-dev gpu-ctx nil)
   (add-device-context! cpu-dev cpu-ctx nil)
   
   ;; Use GPU context
   (set-device-context! gpu-dev gpu-ctx)
   (let [result (gpu-computation data)])
   
   ;; Switch to CPU context
   (set-device-context! cpu-dev cpu-ctx)
   (let [result (cpu-computation data)])
   ```
   
   Multi-Context Application:
   ```clojure
   (defn render-and-compute []
     (let [viz-ctx (create-viz-context)
           compute-ctx (create-compute-context)]
       ;; Register both
       (add-device-context! dev viz-ctx nil)
       (add-device-context! dev compute-ctx nil)
       
       ;; Computation
       (set-device-context! dev compute-ctx)
       (compute-heavy-workload)
       
       ;; Visualization (OpenGL interop)
       (set-device-context! dev viz-ctx)
       (render-results)))
   ```
   
   See also:
   - add-device-context!: Register device/context
   - get-context: Query active context"
  [dev ctx]
  (let [dev-seg (mem/as-segment (long dev))
        ctx-seg (mem/as-segment (long ctx))]
    (jvm/check! (opencl/afcl-set-device-context dev-seg ctx-seg)
                "afcl-set-device-context")
    nil))

(defn delete-device-context!
  "Remove user-provided OpenCL context from ArrayFire's pool.
   
   Unregisters a device/context pair previously added with
   add-device-context!. After this, you can safely release
   the OpenCL objects.
   
   Parameters:
   - dev: cl_device_id (long), device ID
   - ctx: cl_context (long), context to remove
   
   Returns:
   nil
   
   Memory Management:
   This only removes context from ArrayFire. You must still
   release OpenCL objects:
   ```clojure
   (delete-device-context! dev ctx)  ; Remove from ArrayFire
   ;; clReleaseCommandQueue(queue)   ; Your responsibility
   ;; clReleaseContext(ctx)           ; Your responsibility
   ```
   
   Examples:
   ```clojure
   ;; Complete lifecycle
   (let [dev (get-device)
         ctx (create-context dev)
         queue (create-queue ctx dev)]
     (try
       (add-device-context! dev ctx queue)
       (process-data)
       (finally
         (delete-device-context! dev ctx)
         ;; Release queue and context
         )))
   ```
   
   Important: Cannot delete currently active context.
   Switch to different device/context first.
   
   See also:
   - add-device-context!: Register context
   - set-device-context!: Change active context"
  [dev ctx]
  (let [dev-seg (mem/as-segment (long dev))
        ctx-seg (mem/as-segment (long ctx))]
    (jvm/check! (opencl/afcl-delete-device-context dev-seg ctx-seg)
                "afcl-delete-device-context")
    nil))

;;;
;;; Device Information
;;;

(defn get-device-type
  "Get the OpenCL device type of the current device.
   
   Returns the type of the currently active device.
   
   Returns:
   Integer constant:
   - DEVICE-TYPE-CPU (2): CPU device
   - DEVICE-TYPE-GPU (4): GPU device
   - DEVICE-TYPE-ACC (8): Accelerator device
   - DEVICE-TYPE-UNKNOWN (-1): Unknown type
   
   Examples:
   ```clojure
   ;; Query device type
   (let [dtype (get-device-type)]
     (case dtype
       2 (println \"Running on CPU\")
       4 (println \"Running on GPU\")
       8 (println \"Running on Accelerator\")
       (println \"Unknown device\")))
   
   ;; Adaptive algorithm
   (defn adaptive-fft [data]
     (if (= (get-device-type) DEVICE-TYPE-GPU)
       (fft data :batch-size 1024)  ; GPU: large batches
       (fft data :batch-size 64)))  ; CPU: small batches
   
   ;; Device info map
   (defn device-info []
     {:type (get-device-type)
      :platform (get-platform)
      :type-name (case (get-device-type)
                   2 \"CPU\"
                   4 \"GPU\"
                   8 \"Accelerator\"
                   \"Unknown\")})
   ```
   
   See also:
   - get-platform: Query platform vendor"
  []
  (let [dtype-ptr (mem/alloc 4)]
    (jvm/check! (opencl/afcl-get-device-type dtype-ptr)
                "afcl-get-device-type")
    (mem/read-int dtype-ptr 0)))

(defn get-platform
  "Get the OpenCL platform vendor of the current device.
   
   Returns which OpenCL implementation/vendor is being used.
   
   Returns:
   Integer constant:
   - PLATFORM-AMD (0): AMD OpenCL
   - PLATFORM-APPLE (1): Apple OpenCL
   - PLATFORM-INTEL (2): Intel OpenCL
   - PLATFORM-NVIDIA (3): NVIDIA OpenCL
   - PLATFORM-BEIGNET (4): Beignet
   - PLATFORM-POCL (5): POCL
   - PLATFORM-UNKNOWN (-1): Unknown
   
   Examples:
   ```clojure
   ;; Query platform
   (let [platform (get-platform)]
     (case platform
       0 (println \"AMD\")
       1 (println \"Apple\")
       2 (println \"Intel\")
       3 (println \"NVIDIA\")
       4 (println \"Beignet\")
       5 (println \"POCL\")
       (println \"Unknown\")))
   
   ;; Platform-specific optimization
   (defn optimized-conv [input kernel]
     (let [platform (get-platform)]
       (cond
         (= platform PLATFORM-NVIDIA)
         (convolve input kernel :work-group 256)
         
         (= platform PLATFORM-INTEL)
         (convolve input kernel :work-group 64)
         
         :else
         (convolve input kernel))))
   
   ;; Complete device info
   (defn print-device-info []
     (let [dtype (get-device-type)
           platform (get-platform)]
       (println \"Device Type:\" 
                (case dtype
                  2 \"CPU\" 4 \"GPU\" 8 \"Accelerator\" \"Unknown\"))
       (println \"Platform:\"
                (case platform
                  0 \"AMD\" 1 \"Apple\" 2 \"Intel\" 
                  3 \"NVIDIA\" 4 \"Beignet\" 5 \"POCL\" 
                  \"Unknown\"))))
   ```
   
   See also:
   - get-device-type: Query device type"
  []
  (let [plat-ptr (mem/alloc 4)]
    (jvm/check! (opencl/afcl-get-platform plat-ptr)
                "afcl-get-platform")
    (mem/read-int plat-ptr 0)))

;;;
;;; Convenience Functions
;;;

(defn device-type-name
  "Get human-readable name for device type.
   
   Parameters:
   - dtype: Device type constant (default: current device type)
   
   Returns:
   String name of device type
   
   Example:
   ```clojure
   (device-type-name (get-device-type))  ; => \"GPU\"
   ```"
  ([]
   (device-type-name (get-device-type)))
  ([dtype]
   (case dtype
     2 "CPU"
     4 "GPU"
     8 "Accelerator"
     "Unknown")))

(defn platform-name
  "Get human-readable name for platform.
   
   Parameters:
   - platform: Platform constant (default: current platform)
   
   Returns:
   String name of platform
   
   Example:
   ```clojure
   (platform-name (get-platform))  ; => \"NVIDIA\"
   ```"
  ([]
   (platform-name (get-platform)))
  ([platform]
   (case platform
     0 "AMD"
     1 "Apple"
     2 "Intel"
     3 "NVIDIA"
     4 "Beignet"
     5 "POCL"
     "Unknown")))

(defn device-info
  "Get comprehensive device information.
   
   Returns:
   Map with device information:
   - :device-type - Type constant
   - :device-type-name - Type name string
   - :platform - Platform constant
   - :platform-name - Platform name string
   - :device-id - OpenCL device ID
   
   Example:
   ```clojure
   (device-info)
   ; => {:device-type 4
   ;     :device-type-name \"GPU\"
   ;     :platform 3
   ;     :platform-name \"NVIDIA\"
   ;     :device-id 140234567890}
   ```"
  []
  (let [dtype (get-device-type)
        platform (get-platform)
        dev-id (get-device-id)]
    {:device-type dtype
     :device-type-name (device-type-name dtype)
     :platform platform
     :platform-name (platform-name platform)
     :device-id dev-id}))
