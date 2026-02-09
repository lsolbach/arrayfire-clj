(ns org.soulspace.arrayfire.integration.unified-api.event
  "Integration of the ArrayFire event related FFI bindings with the error
   handling and resource management on the JVM.
   
   This namespace provides idiomatic Clojure wrappers around ArrayFire's event
   system, which enables fine-grained control over GPU computation synchronization
   and stream dependencies.
   
   ArrayFire Event System:
   
   Events are lightweight synchronization primitives that mark specific points in
   the execution timeline. They enable:
   - Precise synchronization between operations
   - Cross-stream dependencies
   - Asynchronous wait operations
   - CPU-GPU synchronization
   
   Event Lifecycle:
   1. Create: Allocate event resources
   2. Mark: Record event on active computation queue
   3. Use: Wait or block on the event for synchronization
   4. Delete: Free event resources
   
   Synchronization Patterns:
   
   1. Simple CPU-GPU Synchronization:
   ```clojure
   (let [event (create-event!)]
     (try
       ;; GPU operations...
       (mark-event! event)
       ;; CPU waits for GPU
       (block-event! event)
       (finally
         (delete-event! event))))
   ```
   
   2. Cross-Stream Dependencies:
   ```clojure
   (let [event (create-event!)]
     ;; Stream A operations
     (mark-event! event)
     
     ;; Stream B waits for Stream A
     (set-device-queue! 1)
     (enqueue-wait-event! event)
     
     (delete-event! event))
   ```
   
   3. Multiple Stream Coordination:
   ```clojure
   (let [event1 (create-event!)
         event2 (create-event!)]
     ;; Independent streams do work
     (mark-event! event1)
     (set-device-queue! 1)
     (mark-event! event2)
     
     ;; Main stream waits for both
     (set-device-queue! 0)
     (enqueue-wait-event! event1)
     (enqueue-wait-event! event2)
     
     (delete-event! event1)
     (delete-event! event2))
   ```
   
   Best Practices:
   - Use enqueue-wait-event! for GPU-GPU synchronization (non-blocking)
   - Use block-event! only when CPU truly needs to wait (blocking)
   - Create and delete events explicitly to manage resources
   - Consider event pooling for high-frequency usage
   - Minimize synchronization points for better performance"
  (:require [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.c-api.events :as event-ffi]
            [org.soulspace.arrayfire.integration.jvm-integration :as jvm]))

;;;
;;; Event Lifecycle Management
;;;

(defn create-event!
  "Create a new ArrayFire event handle.
   
   Allocates and initializes a new event object that can be used for
   synchronization and profiling of GPU operations. The created event is
   in an uninitialized state and must be marked to represent a specific
   point in the computation timeline.
   
   Returns:
   Long integer representing the event handle (native pointer)
   
   Example:
   (let [event (create-event!)]
     (try
       (mark-event! event)
       (block-event! event)
       (finally
         (delete-event! event))))
   
   Note: Event must be explicitly deleted with delete-event! to prevent
   resource leaks. Consider using a resource management pattern."
  []
  (let [event-ptr-buf (mem/alloc 8)] ; Buffer to hold af_event pointer
    (jvm/check! (event-ffi/af-create-event event-ptr-buf)
                "af-create-event")
    (mem/read-long event-ptr-buf 0)))

(defn delete-event!
  "Delete an ArrayFire event handle and free associated resources.
   
   Destroys the event object and releases all backend-specific resources.
   After this call, the event handle becomes invalid and must not be used.
   
   Parameters:
   - event: Event handle (long integer) to delete
   
   Returns:
   nil
   
   Example:
   (let [event (create-event!)]
     (try
       (mark-event! event)
       (finally
         (delete-event! event))))
   
   Note: Event should be completed before deletion. Consider blocking on
   the event if you need to ensure completion before cleanup."
  [event]
  (let [event-segment (mem/as-segment event)]
    (jvm/check! (event-ffi/af-delete-event event-segment)
                "af-delete-event")
    nil))

;;;
;;; Event Marking and Synchronization
;;;

(defn mark-event!
  "Mark an event on the active computation queue.
   
   Records the event on the currently active ArrayFire computation queue,
   establishing a marker at this specific point in the execution timeline.
   All operations enqueued before this call become implicit dependencies
   of the event.
   
   This is an asynchronous operation - it returns immediately without blocking
   the CPU thread.
   
   Parameters:
   - event: Event handle (long integer) to mark
   
   Returns:
   nil
   
   Example:
   (let [event (create-event!)]
     ;; Perform GPU operations
     (mark-event! event)
     ;; Event now represents completion point of prior operations
     (block-event! event) ; Wait for completion
     (delete-event! event))
   
   Note: The event can be marked multiple times, with each mark updating
   the event to a new timeline position."
  [event]
  (let [event-segment (mem/as-segment event)]
    (jvm/check! (event-ffi/af-mark-event event-segment)
                "af-mark-event")
    nil))

(defn enqueue-wait-event!
  "Make the active queue wait for an event to complete.
   
   Enqueues a wait operation on the currently active computation queue,
   establishing a dependency on the specified event. All operations enqueued
   after this call will not begin execution until the event completes.
   
   This is an asynchronous operation from the host perspective - it inserts
   a wait into the GPU queue but does not block the calling CPU thread.
   This is the primary mechanism for GPU-GPU synchronization across streams.
   
   Parameters:
   - event: Event handle (long integer) to wait for
   
   Returns:
   nil
   
   Example:
   ;; Cross-stream synchronization
   (let [event (create-event!)]
     ;; Stream A operations
     (mark-event! event)
     
     ;; Switch to Stream B
     (set-device-queue! 1)
     
     ;; Stream B waits for Stream A's event
     (enqueue-wait-event! event)
     
     ;; Stream B operations now depend on Stream A
     (delete-event! event))
   
   Note: This does not block the CPU thread - only the GPU queue. Use
   block-event! if you need CPU-GPU synchronization."
  [event]
  (let [event-segment (mem/as-segment event)]
    (jvm/check! (event-ffi/af-enqueue-wait-event event-segment)
                "af-enqueue-wait-event")
    nil))

(defn block-event!
  "Block the calling thread until the event completes.
   
   Synchronously waits for the event to complete, blocking the calling CPU
   thread until all operations associated with the event have finished
   executing on the GPU. This is the primary mechanism for CPU-GPU
   synchronization.
   
   WARNING: This is a blocking operation that will idle the CPU thread.
   Use sparingly and only when the CPU truly needs to wait for GPU results.
   For GPU-GPU synchronization, use enqueue-wait-event! instead.
   
   Parameters:
   - event: Event handle (long integer) to wait for
   
   Returns:
   nil
   
   Example:
   ;; CPU needs GPU results
   (let [event (create-event!)]
     ;; GPU computation
     (mark-event! event)
     
     ;; CPU waits for GPU
     (block-event! event)
     
     ;; Safe to access GPU results now
     (read-array-to-host result)
     (delete-event! event))
   
   Performance Note: This blocks the entire CPU thread. Minimize usage
   and prefer asynchronous patterns when possible."
  [event]
  (let [event-segment (mem/as-segment event)]
    (jvm/check! (event-ffi/af-block-event event-segment)
                "af-block-event")
    nil))
