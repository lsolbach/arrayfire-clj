(ns org.soulspace.arrayfire.integration.error
  "Integration of the ArrayFire error handling related FFI bindings with the error
   handling and resource management on the JVM.
   
   This namespace provides idiomatic Clojure wrappers around ArrayFire's error
   handling system, which includes:
   - Retrieving detailed error messages
   - Converting error codes to human-readable strings
   - Controlling stack trace capture in error messages
   
   ArrayFire Error System:
   
   ArrayFire uses a comprehensive error code system where all C API functions
   return an af_err integer. When an error occurs (non-zero code), detailed
   diagnostic information is stored in thread-local storage and can be retrieved
   using get-last-error.
   
   Error Messages Include:
   - Error type and code
   - Function name where error occurred
   - Source file and line number
   - Descriptive message
   - Optional stack trace
   
   Usage Pattern:
   ```clojure
   ;; Check for errors after ArrayFire calls
   (when-let [err-msg (get-last-error)]
     (println \"Error:\" err-msg))
   
   ;; Convert error codes to strings
   (println (err-to-string AF_ERR_NO_MEM))
   ;; => \"Device out of memory\"
   
   ;; Control stack trace capture
   (set-enable-stacktrace! false) ; Disable for production
   (set-enable-stacktrace! true)  ; Enable for debugging
   ```"
  (:require [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.c-api.error :as error-ffi]
            [org.soulspace.arrayfire.integration.jvm-integration :as jvm])
  (:import [java.lang.foreign Arena MemorySegment]))

;;;
;;; Error Retrieval
;;;

(defn get-last-error
  "Get the last error message that occurred in ArrayFire.
   
   Retrieves the detailed error message for the most recent ArrayFire error.
   The error message includes:
   - Error type and code
   - Function name where error occurred
   - Source file and line number
   - Descriptive error message
   - Stack trace (if enabled)
   
   The error message is stored in thread-local storage, so each thread has its
   own error state. After retrieving the message, the global error string is cleared.
   
   Returns:
   String containing the error message, or nil if no error has occurred
   
   Example:
   (when-let [err (get-last-error)]
     (println \"ArrayFire Error:\" err)
     ;; Handle error...
     )
   
   Note: This function is thread-safe. Each thread maintains its own error state."
  []
  (let [arena (Arena/ofConfined)]
    (try
      (let [str-ptr-buf (mem/alloc 8)  ; Buffer to hold char** pointer
            len-buf (mem/alloc 8)]      ; Buffer to hold dim_t* length
        ;; Call af_get_last_error (no return code, it's void)
        (error-ffi/af-get-last-error str-ptr-buf len-buf)
        
        ;; Read the length
        (let [length (mem/read-long len-buf 0)]
          (if (zero? length)
            nil ; No error occurred
            ;; Read the char* pointer from the buffer
            (let [str-address (mem/read-long str-ptr-buf 0)
                  str-segment (MemorySegment/ofAddress str-address)]
              (jvm/c-string->string str-segment)))))
      (finally
        (.close arena)))))

;;;
;;; Error Code Conversion
;;;

(defn err-to-string
  "Convert an ArrayFire error code to its string representation.
   
   Converts an af_err enum value to a human-readable error description.
   This is useful for quick error code lookups and logging.
   
   The function returns a static constant string that describes the error.
   
   Parameters:
   - err-code: Integer error code (af_err enum value)
   
   Returns:
   String description of the error
   
   Example:
   (err-to-string 0)   ; => \"Success\"
   (err-to-string 101) ; => \"Device out of memory\"
   (err-to-string 201) ; => \"Invalid array\"
   (err-to-string 301) ; => \"Function not supported\"
   
   Common Error Codes:
   - 0:   AF_SUCCESS - Success
   - 101: AF_ERR_NO_MEM - Device out of memory
   - 102: AF_ERR_DRIVER - Driver not available or incompatible
   - 103: AF_ERR_RUNTIME - Runtime error
   - 201: AF_ERR_INVALID_ARRAY - Invalid array
   - 202: AF_ERR_ARG - Invalid input argument
   - 203: AF_ERR_SIZE - Invalid input size
   - 204: AF_ERR_TYPE - Function does not support this data type
   - 205: AF_ERR_DIFF_TYPE - Input types are not the same
   - 301: AF_ERR_NOT_SUPPORTED - Function not supported
   - 401: AF_ERR_NO_DBL - Double precision not supported
   - 998: AF_ERR_INTERNAL - Internal error
   - 999: AF_ERR_UNKNOWN - Unknown error
   
   Note: The returned string is a compile-time constant and does not need
   to be freed by the caller."
  [err-code]
  (let [str-ptr (error-ffi/af-err-to-string (int err-code))]
    (jvm/c-string->string str-ptr)))

;;;
;;; Stack Trace Control
;;;

(defn set-enable-stacktrace!
  "Enable or disable stack trace capture in error messages.
   
   Controls whether detailed stack traces are included in error messages
   returned by get-last-error. Stack traces provide valuable debugging
   information by showing the call chain leading to an error.
   
   Stack traces include:
   - Function names
   - Source file paths
   - Line numbers
   - Call hierarchy
   
   Default: Stack traces are ENABLED by default
   
   When to Disable:
   - Production environments where performance is critical
   - When stack trace capture overhead is unacceptable
   - Embedded systems with limited memory
   
   When to Enable:
   - Development and debugging
   - Troubleshooting production issues
   - When detailed error context is needed
   - Submitting bug reports
   
   Performance Impact:
   - Enabled: Additional overhead when errors occur (stack capture)
   - Disabled: Minimal overhead, only error message construction
   - Normal execution (no errors): No overhead either way
   
   Parameters:
   - enabled: Boolean, true to enable stack traces, false to disable
   
   Returns:
   nil
   
   Example:
   ;; Disable for production
   (set-enable-stacktrace! false)
   
   ;; Enable for debugging
   (set-enable-stacktrace! true)
   
   Note: This affects global state and should be set once at program initialization.
   The setting applies to all subsequent errors across all threads."
  [enabled]
  (jvm/check! (error-ffi/af-set-enable-stacktrace (if enabled 1 0))
              "af-set-enable-stacktrace")
  nil)


