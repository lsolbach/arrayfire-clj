(ns org.soulspace.arrayfire.ffi.c-api.events
  "Bindings for the ArrayFire event functions.
   
   ArrayFire events provide fine-grained control over GPU computation
   synchronization and stream dependencies. Events are lightweight synchronization
   primitives that can be inserted into computation streams to:
   
   1. Mark specific points in the execution timeline
   2. Establish dependencies between operations
   3. Enable asynchronous wait operations
   4. Provide precise timing and profiling capabilities
   
   Event System Design:
   
   An event represents a specific point in time on a computation queue/stream.
   When an event is \"marked\", it captures the state of the queue at that moment.
   Other operations can then wait for that event to complete, creating explicit
   dependencies in the execution graph.
   
   Key Concepts:
   
   1. Event Handle (af_event):
      - Opaque pointer to internal event object
      - Backend-specific implementation (CUDA, OpenCL, oneAPI, CPU)
      - Managed through create/delete lifecycle
   
   2. Event States:
      - Created: Event object exists but not yet marked
      - Marked: Event placed on a queue, represents a timeline point
      - Completed: All operations before the mark have finished
      - Destroyed: Event resources have been freed
   
   3. Synchronization Modes:
      - Asynchronous (mark/enqueue): Non-blocking, returns immediately
      - Synchronous (block): Blocks calling thread until completion
   
   Event Operations:
   
   1. af_create_event:
      - Allocates event object
      - Does not mark or record on any stream
      - Event is in \"created\" state
      - Must be explicitly marked to be useful
   
   2. af_delete_event:
      - Frees event resources
      - Destroys underlying native event object
      - Event handle becomes invalid after this call
      - Should not be called while event is in use
   
   3. af_mark_event:
      - Records event on the active computation queue
      - All operations enqueued before this point become dependencies
      - Returns immediately (asynchronous operation)
      - Event can be used to synchronize later operations
      - Multiple marks update the event to new timeline position
   
   4. af_enqueue_wait_event:
      - Makes active queue wait for event completion
      - Asynchronous: Does not block calling thread
      - Blocks the queue/stream itself from progressing
      - Operations after this wait depend on event completion
      - Enables cross-stream synchronization
   
   5. af_block_event:
      - Synchronous blocking call
      - Blocks calling thread until event completes
      - Used for CPU-GPU synchronization
      - Ensures all GPU operations before mark are finished
      - Returns only when event's timeline point is reached
   
   Backend Implementation:
   
   CUDA:
   - Uses CUevent with CU_EVENT_DISABLE_TIMING flag
   - cuEventCreate, cuEventRecord, cuStreamWaitEvent, cuEventSynchronize
   - Supports cross-stream dependencies
   - Events are lightweight (timing disabled for performance)
   
   OpenCL:
   - Uses cl_event objects
   - clEnqueueMarkerWithWaitList for marking
   - clWaitForEvents for blocking
   - Integrates with OpenCL command queue system
   
   oneAPI (SYCL):
   - Uses sycl::event objects
   - ext_oneapi_submit_barrier for marking and waiting
   - Native SYCL event dependencies
   
   CPU:
   - Uses internal queue_event mechanism
   - Synchronous by default on CPU
   - Minimal overhead for CPU computations
   
   Usage Patterns:
   
   1. Simple Synchronization:
      ```clojure
      (let [event (create-event)]
        ;; Perform GPU operations
        (mark-event event)
        ;; Wait for completion
        (block-event event)
        (delete-event event))
      ```
   
   2. Asynchronous Dependencies:
      ```clojure
      (let [event (create-event)]
        ;; Queue A: Perform operations
        (mark-event event)
        
        ;; Switch to Queue B
        (set-device-queue! 1)
        
        ;; Queue B waits for Queue A
        (enqueue-wait-event event)
        
        ;; Queue B operations now depend on Queue A completion
        (delete-event event))
      ```
   
   3. Multi-Stream Coordination:
      ```clojure
      (let [event1 (create-event)
            event2 (create-event)]
        ;; Stream 1 operations
        (mark-event event1)
        
        ;; Stream 2 operations
        (set-device-queue! 1)
        (mark-event event2)
        
        ;; Stream 3 waits for both
        (set-device-queue! 2)
        (enqueue-wait-event event1)
        (enqueue-wait-event event2)
        
        ;; Cleanup
        (block-event event1)
        (block-event event2)
        (delete-event event1)
        (delete-event event2))
      ```
   
   4. Profiling Pattern:
      ```clojure
      (let [start (create-event)
            end (create-event)]
        (mark-event start)
        ;; Operations to time
        (mark-event end)
        (block-event end)
        ;; Calculate elapsed time
        (delete-event start)
        (delete-event end))
      ```
   
   Performance Considerations:
   
   1. Event Creation Overhead:
      - Minimal for most backends
      - CUDA: Fast (no timing data collected)
      - Consider event pooling for very frequent use
   
   2. Mark Operation:
      - Asynchronous, returns immediately
      - O(1) queue insertion
      - No host-device synchronization
   
   3. Enqueue Wait:
      - Asynchronous from host perspective
      - May introduce bubble in GPU pipeline
      - Use only when necessary for correctness
   
   4. Block Operation:
      - Synchronous, blocks calling thread
      - Can be expensive (host-device sync)
      - Use af_sync() for simpler cases
   
   5. Event Reuse:
      - Events can be marked multiple times
      - Each mark updates to new timeline position
      - Previous dependencies are replaced
   
   Common Pitfalls:
   
   1. Forgetting to Mark:
      - Creating event without marking is useless
      - Event must be marked to represent timeline point
      - Solution: Always mark before using for synchronization
   
   2. Blocking Too Often:
      - Excessive blocking defeats asynchronous execution
      - Solution: Use enqueue_wait for GPU-GPU sync
   
   3. Deleting Active Events:
      - Deleting event still in use causes undefined behavior
      - Solution: Block or ensure completion before delete
   
   4. Cross-Backend Events:
      - Events from one backend (CUDA) don't work with another (OpenCL)
      - Solution: Use appropriate backend for each event
   
   5. Memory Leaks:
      - Not deleting events causes resource leaks
      - Solution: Always delete events when done
   
   Thread Safety:
   
   - Events are thread-safe across different threads
   - Single event should not be marked simultaneously from multiple threads
   - Event creation/deletion should be serialized per event
   - Multiple events can be used for multi-threaded coordination
   
   Comparison with Other Synchronization:
   
   1. af_sync():
      - Simpler, synchronizes entire device
      - Less fine-grained than events
      - Use when you need complete device synchronization
   
   2. af_eval():
      - Forces evaluation of lazy operations
      - Does not synchronize (still asynchronous)
      - Use with events for precise control
   
   3. Events:
      - Most fine-grained control
      - Enables complex dependencies
      - Cross-stream synchronization
      - Profiling and timing
   
   Best Practices:
   
   1. Always delete events to prevent leaks
   2. Use RAII pattern in higher-level wrappers
   3. Mark events immediately after operations of interest
   4. Prefer enqueue_wait over block for GPU-GPU sync
   5. Use block only when CPU needs to wait for GPU
   6. Document event dependencies clearly
   7. Consider event pooling for high-frequency use
   8. Test event code with AF_PRINT_ERRORS=1
   9. Use events for cross-stream dependencies
   10. Profile to ensure events help (not hurt) performance
   
   Advanced Use Cases:
   
   1. Pipeline Parallelism:
      - Multiple streams process different stages
      - Events coordinate between stages
      - Maximize GPU utilization
   
   2. Overlapping Computation and Communication:
      - One stream computes while another transfers data
      - Events ensure correctness
   
   3. Dynamic Task Graphs:
      - Build complex dependency graphs at runtime
      - Events represent edges in the graph
   
   4. Priority Scheduling:
      - Use events with different streams/priorities
      - Coordinate high/low priority work
   
   API Version:
   - Events API available from AF_API_VERSION >= 37
   - Check version before using event functions
   
   Related Functions:
   - af_sync: Device-wide synchronization
   - af_eval: Force evaluation of lazy operations
   - af_device_gc: Garbage collection (can benefit from sync)
   - af_set_device: Switch active device
   - af_get_device: Query active device"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.loader]))

;; Event management functions

;; af_err af_create_event(af_event *eventHandle)
(defcfn af-create-event
  "Create a new ArrayFire event handle.
   
   Allocates and initializes a new event object that can be used for
   synchronization and profiling of GPU operations. The created event is in
   an uninitialized state and must be marked (af_mark_event) to represent
   a specific point in the computation timeline.
   
   Event Lifecycle:
   1. Create: Allocate event resources (this function)
   2. Mark: Record event on a computation queue
   3. Use: Wait or block on the event
   4. Delete: Free event resources (af_delete_event)
   
   Backend Behavior:
   - CUDA: Creates CUevent with timing disabled for performance
   - OpenCL: Prepares cl_event structure (marked later)
   - oneAPI: Allocates sycl::event pointer
   - CPU: Creates queue_event wrapper
   
   Memory Management:
   - Allocates native backend event resources
   - Must be freed with af_delete_event to prevent leaks
   - Event handle remains valid until delete call
   
   Thread Safety:
   - Safe to call from multiple threads
   - Creates independent event objects
   - Each thread should manage its own events
   
   Performance:
   - Lightweight allocation on all backends
   - CUDA: Fast (no timing overhead)
   - OpenCL: Minimal overhead
   - CPU: Nearly zero overhead
   
   Usage Pattern:
   ```clojure
   (let [event-ptr (mem/alloc-instance ::mem/pointer)]
     (af-create-event event-ptr)
     (let [event (mem/read-ptr event-ptr)]
       ;; Use event for synchronization
       (af-mark-event event)
       (af-block-event event)
       (af-delete-event event)))
   ```
   
   Error Conditions:
   - AF_ERR_NO_MEM: Out of device memory
   - AF_ERR_RUNTIME: Backend-specific creation failure
   - AF_ERR_DRIVER: GPU driver issue
   
   Parameters:
   - event-handle: out pointer to af_event (will receive event handle)
   
   Returns:
   AF_SUCCESS on success, error code otherwise
   
   Note: The event is not yet marked on any queue. Call af_mark_event
   to place the event on the active computation queue."
  "af_create_event" [::mem/pointer] ::mem/int)

;; af_err af_delete_event(af_event eventHandle)
(defcfn af-delete-event
  "Delete an ArrayFire event handle and free associated resources.
   
   Destroys the event object and releases all backend-specific resources
   including native event handles. After this call, the event handle becomes
   invalid and must not be used.
   
   Resource Cleanup:
   - CUDA: Calls cuEventDestroy
   - OpenCL: Calls clReleaseEvent
   - oneAPI: Deletes sycl::event pointer
   - CPU: Minimal cleanup (no GPU resources)
   
   Safety Requirements:
   - Event should be completed before deletion
   - No operations should depend on the event
   - Deleting an in-use event causes undefined behavior
   - Consider blocking on event before deletion if necessary
   
   Thread Safety:
   - Safe to call from any thread
   - Must not delete same event from multiple threads simultaneously
   - Synchronize deletion with other event operations
   
   Memory Management:
   - Frees all backend resources
   - Event handle pointer becomes dangling after this call
   - Do not attempt to use event after deletion
   
   Best Practices:
   1. Block on event before deletion if completion matters
   2. Use RAII wrappers in higher-level code
   3. Set event variable to null after deletion
   4. Never delete event while operations depend on it
   5. Consider event pooling to reduce allocation overhead
   
   Error Conditions:
   - AF_ERR_RUNTIME: Backend-specific deletion failure (rare)
   - Undefined behavior if event handle is invalid
   
   Usage Pattern:
   ```clojure
   (let [event (create-event!)]
     (try
       (mark-event! event)
       (perform-operations)
       (block-event! event)  ; Ensure completion
       (finally
         (af-delete-event event))))  ; Cleanup
   ```
   
   Performance:
   - Fast on all backends
   - No host-device synchronization
   - O(1) operation
   
   Parameters:
   - event-handle: af_event handle to delete
   
   Returns:
   AF_SUCCESS on success, error code otherwise
   
   Note: This function does not block. If you need to ensure event
   completion before deletion, call af_block_event first."
  "af_delete_event" [::mem/pointer] ::mem/int)

;; af_err af_mark_event(const af_event eventHandle)
(defcfn af-mark-event
  "Mark an event on the active computation queue.
   
   Records the event on the currently active ArrayFire computation queue,
   establishing a marker at this specific point in the execution timeline.
   All operations enqueued before this call become implicit dependencies
   of the event. The event is considered \"complete\" when all prior
   operations have finished executing.
   
   Event Marking Behavior:
   - Asynchronous: Returns immediately without blocking
   - Queue-specific: Event is tied to the active queue at call time
   - Timeline marker: Represents completion point of prior work
   - Non-blocking: CPU continues execution immediately
   
   Dependencies:
   - All operations enqueued before mark are dependencies
   - Operations after mark can execute independently
   - Other queues can wait for this event (cross-stream sync)
   - Event can be marked multiple times (updates timeline position)
   
   Backend Implementation:
   - CUDA: cuEventRecord(event, activeStream)
   - OpenCL: clEnqueueMarkerWithWaitList on active queue
   - oneAPI: queue.ext_oneapi_submit_barrier() returns event
   - CPU: Marks completion point in async queue
   
   Remarking Events:
   - Events can be marked multiple times
   - Each mark updates the event to new timeline position
   - Previous mark's dependencies are effectively replaced
   - Useful for event reuse patterns
   
   Active Queue:
   - Uses the queue set by af_set_device_queue or default queue
   - Different devices have independent queues
   - Query with af_get_device_queue if needed
   
   Performance:
   - Extremely fast (asynchronous queue insertion)
   - O(1) operation
   - No host-device synchronization
   - Minimal overhead on all backends
   
   Synchronization Patterns:
   
   1. Simple Wait Pattern:
   ```clojure
   (mark-event! event)
   (enqueue-wait-event! event)  ; Later operations wait
   ```
   
   2. Cross-Stream Sync:
   ```clojure
   ;; Stream A
   (mark-event! event-a)
   
   ;; Switch to Stream B
   (set-device-queue! 1)
   (enqueue-wait-event! event-a)  ; Stream B waits for A
   ```
   
   3. CPU-GPU Sync:
   ```clojure
   (mark-event! event)
   (block-event! event)  ; CPU waits for GPU
   ```
   
   Error Conditions:
   - AF_ERR_RUNTIME: Backend-specific mark failure
   - AF_ERR_INVALID_ARRAY: Invalid event handle
   
   Use Cases:
   - Establish synchronization points in computation
   - Create dependencies between operations
   - Enable profiling and timing measurements
   - Coordinate multi-stream execution
   - Control execution order explicitly
   
   Parameters:
   - event-handle: af_event to mark on active queue
   
   Returns:
   AF_SUCCESS on success, error code otherwise
   
   Note: This is an asynchronous operation. The event is marked on
   the queue but may not be complete when this function returns.
   Use af_block_event to wait for completion."
  "af_mark_event" [::mem/pointer] ::mem/int)

;; af_err af_enqueue_wait_event(const af_event eventHandle)
(defcfn af-enqueue-wait-event
  "Make the active queue wait for an event to complete.
   
   Enqueues a wait operation on the currently active computation queue,
   establishing a dependency on the specified event. All operations enqueued
   after this call will not begin execution until the event completes.
   This is an asynchronous operation from the host perspective - it inserts
   a wait into the GPU queue but does not block the calling CPU thread.
   
   Asynchronous Behavior:
   - Host: Returns immediately, does not block CPU thread
   - Device: Queue blocks until event completes
   - Pipeline: Creates execution dependency in GPU pipeline
   - CPU continues: Can enqueue more operations after wait
   
   Execution Model:
   ```
   Queue Timeline:
   [Op1] [Op2] [WAIT_EVENT] [Op3] [Op4]
                    ↑
                    └─ Queue pauses here until event completes
   
   - Op1, Op2: Execute immediately if queue is active
   - WAIT: Queue stalls at this point
   - Op3, Op4: Execute only after event completes
   ```
   
   Cross-Stream Synchronization:
   This is the primary mechanism for coordinating work between different
   computation streams/queues:
   
   ```clojure
   ;; Stream A operations
   (af-mark-event event-a)
   
   ;; Switch to Stream B
   (set-device-queue 1)
   
   ;; Stream B waits for Stream A's event
   (af-enqueue-wait-event event-a)
   
   ;; Stream B operations now depend on Stream A completion
   ```
   
   Backend Implementation:
   - CUDA: cuStreamWaitEvent(activeStream, event, 0)
   - OpenCL: clEnqueueMarkerWithWaitList with event as dependency
   - oneAPI: queue.ext_oneapi_submit_barrier({event})
   - CPU: Synchronous wait in async queue
   
   Performance Considerations:
   
   1. Overhead:
      - Minimal host-side overhead (async operation)
      - GPU pipeline may stall waiting for event
      - Use only when dependency is necessary
   
   2. Pipeline Bubbles:
      - Can introduce idle time in GPU pipeline
      - Minimize cross-stream dependencies when possible
      - Organize work to reduce wait operations
   
   3. Multiple Waits:
      - Can wait for multiple events sequentially
      - Each wait adds a dependency to the queue
      - Consider if single event can capture all dependencies
   
   Comparison with af_block_event:
   
   af_enqueue_wait_event:
   - Asynchronous from host perspective
   - Blocks GPU queue, not CPU thread
   - Enables overlap of CPU and GPU work
   - Use for GPU-GPU synchronization
   
   af_block_event:
   - Synchronous, blocks calling thread
   - CPU waits for GPU completion
   - Use for CPU-GPU synchronization
   - Required when CPU needs results
   
   Common Patterns:
   
   1. Producer-Consumer:
   ```clojure
   ;; Producer stream
   (mark-event! producer-event)
   
   ;; Consumer stream waits
   (enqueue-wait-event! producer-event)
   ```
   
   2. Multi-Stream Merge:
   ```clojure
   ;; Multiple streams do independent work
   (mark-event! event1)  ; Stream 1
   (mark-event! event2)  ; Stream 2
   
   ;; Main stream waits for both
   (enqueue-wait-event! event1)
   (enqueue-wait-event! event2)
   ```
   
   3. Pipeline Stage Sync:
   ```clojure
   ;; Stage 1
   (perform-stage1)
   (mark-event! stage1-done)
   
   ;; Stage 2 depends on Stage 1
   (enqueue-wait-event! stage1-done)
   (perform-stage2)
   ```
   
   Error Conditions:
   - AF_ERR_RUNTIME: Backend wait enqueue failed
   - AF_ERR_INVALID_ARRAY: Invalid event handle
   
   Thread Safety:
   - Safe to call from any thread
   - Event must be valid and preferably marked
   - Multiple threads can wait on same event
   
   Parameters:
   - event-handle: af_event to wait for on active queue
   
   Returns:
   AF_SUCCESS on success, error code otherwise
   
   Note: This does not block the calling thread. It only inserts a
   wait operation into the GPU queue. The CPU thread continues
   immediately after this call returns."
  "af_enqueue_wait_event" [::mem/pointer] ::mem/int)

;; af_err af_block_event(const af_event eventHandle)
(defcfn af-block-event
  "Block the calling thread until the event completes.
   
   Synchronously waits for the event to complete, blocking the calling CPU
   thread until all operations associated with the event have finished
   executing on the GPU. This is the primary mechanism for CPU-GPU
   synchronization using events.
   
   Synchronous Behavior:
   - Blocks calling thread
   - Does not return until event completes
   - Ensures all GPU work before event mark is done
   - CPU thread is idle during the wait
   
   Execution Model:
   ```
   CPU Thread Timeline:
   [CPU Work] → [af_block_event] → [Wait...] → [Event Complete] → [Continue]
                                    ↑
                                    CPU thread blocked here
   
   GPU Timeline:
   [GPU Ops before mark] → [Event Mark] → [Event Complete]
                                           ↑
                                           CPU thread unblocks
   ```
   
   Backend Implementation:
   - CUDA: cuEventSynchronize(event)
   - OpenCL: clWaitForEvents(1, &event)
   - oneAPI: event->wait()
   - CPU: Synchronous queue sync
   
   When to Use:
   
   1. CPU Needs GPU Results:
      - Reading data back to host
      - CPU algorithm depends on GPU computation
      - Validation or error checking
   
   2. End of Program:
      - Ensure all GPU work completes before exit
      - Final synchronization point
   
   3. Timing and Profiling:
      - Measure elapsed time between events
      - Ensure operations complete for accurate timing
   
   4. Resource Management:
      - Before freeing GPU memory
      - Before deleting arrays used in computation
      - Ensure operations finish before cleanup
   
   When NOT to Use:
   
   1. GPU-GPU Synchronization:
      - Use af_enqueue_wait_event instead
      - Allows GPU to continue with other work
      - Prevents unnecessary CPU blocking
   
   2. After Every Operation:
      - Defeats asynchronous execution
      - Causes severe performance degradation
      - Use only when truly needed
   
   3. Inside Tight Loops:
      - Blocks on every iteration
      - Prevents operation overlap
      - Consider batching operations instead
   
   Performance Impact:
   
   1. Cost:
      - Expensive: Blocks entire CPU thread
      - Host-device synchronization overhead
      - GPU may be idle while CPU processes
   
   2. Pipeline Stall:
      - Prevents overlapping CPU and GPU work
      - GPU sits idle if no other work available
      - CPU waits even if it has other work
   
   3. Mitigation:
      - Minimize blocking calls
      - Use async mechanisms (enqueue_wait) when possible
      - Batch operations to reduce sync points
      - Use multiple streams for parallelism
   
   Comparison with af_sync():
   
   af_block_event:
   - Fine-grained: Waits for specific event
   - Event-based: Only waits for marked point
   - Cross-stream: Can sync specific stream work
   - More control: Precise synchronization
   
   af_sync():
   - Coarse-grained: Synchronizes entire device
   - Simpler: One call, all streams
   - Less flexible: Can't target specific work
   - Easier to use: No event management needed
   
   Common Patterns:
   
   1. Read Results:
   ```clojure
   (mark-event! event)
   (perform-gpu-computation)
   (block-event! event)
   (read-array-to-host result-array)  ; Safe now
   ```
   
   2. Timing:
   ```clojure
   (mark-event! start)
   (perform-operation)
   (mark-event! end)
   (block-event! end)
   (calculate-elapsed-time start end)
   ```
   
   3. Cleanup:
   ```clojure
   (mark-event! done)
   (block-event! done)
   (release-array temp-array)  ; Safe after blocking
   ```
   
   4. Error Checking:
   ```clojure
   (mark-event! checkpoint)
   (block-event! checkpoint)
   (check-for-gpu-errors)  ; Ensure operations completed
   ```
   
   Thread Safety:
   - Safe to call from any thread
   - Each thread independently blocks
   - Same event can be blocked by multiple threads
   - All threads wake when event completes
   
   Error Conditions:
   - AF_ERR_RUNTIME: Backend synchronization failed
   - AF_ERR_INVALID_ARRAY: Invalid event handle
   
   Best Practices:
   1. Use sparingly - only when CPU truly needs to wait
   2. Prefer enqueue_wait for GPU-GPU dependencies
   3. Batch operations to reduce blocking frequency
   4. Block once after multiple operations when possible
   5. Consider async alternatives first
   6. Use for correctness, not after every operation
   7. Profile to identify unnecessary blocking
   
   Parameters:
   - event-handle: af_event to wait for
   
   Returns:
   AF_SUCCESS on success, error code otherwise
   
   Note: This is a synchronous, blocking call. The function will not
   return until the event has completed. The calling thread will be
   idle during this time."
  "af_block_event" [::mem/pointer] ::mem/int)
