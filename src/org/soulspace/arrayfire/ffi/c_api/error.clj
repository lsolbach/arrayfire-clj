(ns org.soulspace.arrayfire.ffi.c-api.error
  "Bindings for the ArrayFire error handling functions.
   
   ArrayFire uses a comprehensive error handling system based on error codes
   (af_err enum) and error messages. All ArrayFire C API functions return an
   af_err error code, with AF_SUCCESS (0) indicating success.
   
   Error System Design:
   
   1. Error Codes (af_err enum):
      - AF_SUCCESS (0): Operation completed successfully
      - 100-199: Environment errors (memory, driver, runtime)
        * AF_ERR_NO_MEM (101): Out of memory
        * AF_ERR_DRIVER (102): Driver error
        * AF_ERR_RUNTIME (103): Runtime error
      - 200-299: Input parameter errors
        * AF_ERR_INVALID_ARRAY (201): Invalid array handle
        * AF_ERR_ARG (202): Invalid argument
        * AF_ERR_SIZE (203): Invalid size
        * AF_ERR_TYPE (204): Unsupported type
        * AF_ERR_DIFF_TYPE (205): Mismatched types
        * AF_ERR_BATCH (207): Batch mode error
        * AF_ERR_DEVICE (208): Wrong device
      - 300-399: Missing software features
        * AF_ERR_NOT_SUPPORTED (301): Feature not supported
        * AF_ERR_NOT_CONFIGURED (302): Feature not configured
        * AF_ERR_NONFREE (303): Non-free algorithm unavailable
      - 400-499: Missing hardware features
        * AF_ERR_NO_DBL (401): Double precision not supported
        * AF_ERR_NO_GFX (402): Graphics not supported
        * AF_ERR_NO_HALF (403): Half precision not supported
      - 500-599: Heterogeneous API errors
        * AF_ERR_LOAD_LIB (501): Library loading failed
        * AF_ERR_LOAD_SYM (502): Symbol loading failed
        * AF_ERR_ARR_BKND_MISMATCH (503): Backend mismatch
      - 900-999: Internal and unknown errors
        * AF_ERR_INTERNAL (998): Internal error
        * AF_ERR_UNKNOWN (999): Unknown error
   
   2. Error Messages:
      - Global error string: Thread-local storage for detailed messages
      - Stack traces: Optional (enabled by default, controllable)
      - Error context: Function name, file, line number
   
   3. Error Propagation:
      - C API: Functions return af_err, detailed message via af_get_last_error
      - C++ API: Throws af::exception with embedded error code and message
      - Internal: AfError class hierarchy for different error types
   
   Error Handling Workflow:
   
   1. Call ArrayFire function → returns af_err code
   2. Check if code != AF_SUCCESS
   3. If error, call af_get_last_error for detailed message
   4. Message includes:
      - Error type and code
      - Function name where error occurred
      - File and line number
      - Descriptive message
      - Stack trace (if enabled)
   
   Error Types (Internal C++ Classes):
   
   1. AfError: Base class for all ArrayFire errors
      - Contains: function name, file, line, message, error code, stack trace
      - Methods: getFunctionName(), getFileName(), getLine(), getError()
   
   2. TypeError: Type mismatch errors
      - Additional: type name, argument index
      - Example: passing f32 when f64 required
   
   3. ArgumentError: Invalid argument errors
      - Additional: expected condition, argument index
      - Example: negative dimension, out-of-range index
   
   4. DimensionError: Invalid dimensions
      - Additional: expected condition, argument index
      - Example: matrix multiply with incompatible sizes
   
   5. SupportError: Unsupported features
      - Additional: backend name
      - Example: feature only available on CUDA
   
   Common Error Scenarios:
   
   1. Out of Memory (AF_ERR_NO_MEM):
      - GPU memory exhausted
      - Solution: Reduce data size, use batching, call af_device_gc()
   
   2. Invalid Array (AF_ERR_INVALID_ARRAY):
      - Using released or null array handle
      - Solution: Ensure array is properly created and not released
   
   3. Type Mismatch (AF_ERR_TYPE, AF_ERR_DIFF_TYPE):
      - Function doesn't support data type
      - Solution: Cast arrays to compatible types
   
   4. Size Mismatch (AF_ERR_SIZE):
      - Incompatible array dimensions
      - Solution: Reshape or use broadcasting
   
   5. Unsupported Feature (AF_ERR_NOT_SUPPORTED):
      - Feature not available on current backend
      - Solution: Switch backend or use alternative function
   
   6. Driver Error (AF_ERR_DRIVER):
      - GPU driver issue or version incompatibility
      - Solution: Update GPU drivers
   
   Error Checking Macros (Internal):
   
   1. AF_CHECK(fn): Check af_err, throw AfError on failure
   2. CATCHALL: Catch all exceptions, convert to af_err
   3. TYPE_ERROR(index, type): Throw TypeError
   4. ARG_ASSERT(index, cond): Throw ArgumentError if condition false
   5. DIM_ASSERT(index, cond): Throw DimensionError if condition false
   
   Performance Considerations:
   
   - Error checking: Minimal overhead (single integer comparison)
   - Error messages: Only constructed when error occurs
   - Stack traces: Capture has overhead, disable in production if needed
   - Thread safety: Error strings are thread-local
   
   Environment Variable:
   
   - AF_PRINT_ERRORS: If set to non-zero, errors printed to stderr
   
   Best Practices:
   
   1. Always check return codes from C API functions
   2. Use try-catch blocks when using C++ API
   3. Call af_get_last_error for detailed diagnostics
   4. Free error message string with af_free_host after use
   5. Use af_err_to_string for quick error code lookups
   6. Enable stack traces during development
   7. Consider disabling stack traces in production for performance
   8. Check for AF_ERR_NO_MEM and handle gracefully
   9. Validate inputs before calling ArrayFire functions
   10. Use appropriate error recovery strategies per error type
   
   Integration with Clojure:
   
   - Use af-get-last-error after non-zero af_err returns
   - Convert error codes to Clojure exceptions
   - Include stack trace in exception data
   - Provide meaningful error context
   - Ensure proper cleanup of error message strings
   
   Related Functions:
   - af_device_gc: Free unused GPU memory (helps with AF_ERR_NO_MEM)
   - af_info: Display device information (diagnose capability issues)
   - af_init: Initialize ArrayFire (call before other functions)"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; Error handling functions

;; void af_get_last_error(char **str, dim_t *len)
(defcfn af-get-last-error
  "Get the last error message that occurred.
   
   Retrieves the detailed error message for the most recent ArrayFire error.
   This function provides the complete diagnostic information including:
   - Error type and code
   - Function name where the error occurred
   - Source file and line number
   - Descriptive error message
   - Stack trace (if enabled with af_set_enable_stacktrace)
   
   The error message is stored in thread-local storage, so each thread has its
   own error state. After retrieving the error message, the global error string
   is cleared.
   
   Memory Management:
   - The function allocates memory for the error message using af_alloc_host
   - The caller is responsible for freeing this memory with af_free_host
   - If no error has occurred, str will be set to NULL and len to 0
   
   Thread Safety:
   - Thread-safe: Each thread maintains its own error state
   - Error messages do not propagate between threads
   
   Usage Pattern:
   1. Call ArrayFire function, check return code != AF_SUCCESS
   2. Call af_get_last_error to get detailed message
   3. Process/log the error message
   4. Free the message string with af_free_host
   
   Example Error Message Format:
   ```
   ArrayFire Exception (Invalid size:203):
   In function af_matmul
   In file src/api/c/blas.cpp:45
   Invalid dimension for argument 1
   Expected: Compatible matrix dimensions for multiplication
   [Stack trace if enabled]
   ```
   
   Parameters:
   - str: out pointer to char* (error message, NULL-terminated)
   - len: out pointer to dim_t (message length, excluding null terminator)
   
   Returns:
   void (function does not return error code)
   
   Note: This function cannot fail and does not return an error code.
   If no error has occurred, it returns NULL and length 0."
  "af_get_last_error" [::mem/pointer ::mem/pointer] ::mem/void)

;; const char* af_err_to_string(const af_err err)
(defcfn af-err-to-string
  "Convert an ArrayFire error code to its string representation.
   
   Converts an af_err enum value to a human-readable error description.
   This is useful for quick error code lookups and logging without needing
   the full detailed message from af_get_last_error.
   
   Returns a static constant string that does not need to be freed by the
   caller. The string remains valid for the lifetime of the program.
   
   Error Code Mappings:
   - AF_SUCCESS (0): \"Success\"
   - AF_ERR_NO_MEM (101): \"Device out of memory\"
   - AF_ERR_DRIVER (102): \"Driver not available or incompatible\"
   - AF_ERR_RUNTIME (103): \"Runtime error\"
   - AF_ERR_INVALID_ARRAY (201): \"Invalid array\"
   - AF_ERR_ARG (202): \"Invalid input argument\"
   - AF_ERR_SIZE (203): \"Invalid input size\"
   - AF_ERR_TYPE (204): \"Function does not support this data type\"
   - AF_ERR_DIFF_TYPE (205): \"Input types are not the same\"
   - AF_ERR_BATCH (207): \"Invalid batch configuration\"
   - AF_ERR_DEVICE (208): \"Input does not belong to the current device\"
   - AF_ERR_NOT_SUPPORTED (301): \"Function not supported\"
   - AF_ERR_NOT_CONFIGURED (302): \"Function not configured to build\"
   - AF_ERR_NONFREE (303): \"Function unavailable. ArrayFire compiled without Non-Free algorithms support\"
   - AF_ERR_NO_DBL (401): \"Double precision floating point not supported for this device\"
   - AF_ERR_NO_GFX (402): \"Graphics functionality unavailable for this device\"
   - AF_ERR_NO_HALF (403): \"Half precision floating point not supported for this device\"
   - AF_ERR_LOAD_LIB (501): \"Failed to load dynamic library\"
   - AF_ERR_LOAD_SYM (502): \"Failed to load symbol from dynamic library\"
   - AF_ERR_ARR_BKND_MISMATCH (503): \"Array backend mismatch\"
   - AF_ERR_INTERNAL (998): \"Internal error\"
   - AF_ERR_UNKNOWN (999): \"Unknown error\"
   
   Use Cases:
   - Quick error identification in logs
   - Error code to string conversion for display
   - Debugging and diagnostics
   - Error categorization
   
   Performance:
   - O(1) switch statement lookup
   - Returns static constant string (no allocation)
   - Thread-safe (read-only static data)
   
   Parameters:
   - err: af_err enum value (as int)
   
   Returns:
   const char* pointer to static error description string
   
   Note: The returned string is a compile-time constant and must not be
   modified or freed by the caller."
  "af_err_to_string" [::mem/int] ::mem/pointer)

;; af_err af_set_enable_stacktrace(int is_enabled)
(defcfn af-set-enable-stacktrace
  "Enable or disable stack trace capture in error messages.
   
   Controls whether detailed stack traces are included in error messages
   returned by af_get_last_error. Stack traces provide valuable debugging
   information by showing the call chain leading to an error.
   
   Stack traces are captured using the Boost.Stacktrace library and include:
   - Function names
   - Source file paths
   - Line numbers
   - Call hierarchy
   
   Default Behavior:
   - Stack traces are ENABLED by default
   - Provides detailed debugging information out of the box
   
   When to Disable:
   - Production environments where performance is critical
   - When stack trace capture overhead is unacceptable
   - Embedded systems with limited memory
   - When error messages are logged but not displayed
   
   When to Enable:
   - Development and debugging
   - Troubleshooting production issues
   - When detailed error context is needed
   - Submitting bug reports
   
   Performance Impact:
   - Enabled: Additional overhead when errors occur (stack capture)
   - Disabled: Minimal overhead, only error message construction
   - Normal execution (no errors): No overhead either way
   
   Stack Trace Example (when enabled):
   ```
   0# af::exception::exception() at /path/to/exception.cpp:45
   1# af::matmul() at /path/to/blas.cpp:123
   2# my_computation() at /path/to/main.cpp:89
   3# main at /path/to/main.cpp:150
   ```
   
   Thread Safety:
   - Affects global state (all threads)
   - Not thread-safe during the call itself
   - Should be set once at program initialization
   
   Storage:
   - Stack trace state is stored globally
   - Applied to all subsequent errors across all threads
   
   Parameters:
   - is-enabled: int (0 = disable stack traces, non-zero = enable)
   
   Returns:
   AF_SUCCESS always (this function cannot fail)
   
   Usage Pattern:
   ```clojure
   ;; Disable stack traces for production
   (af-set-enable-stacktrace 0)
   
   ;; Enable stack traces for debugging
   (af-set-enable-stacktrace 1)
   ```
   
   Note: This function always succeeds and returns AF_SUCCESS.
   It affects error messages generated after the call."
  "af_set_enable_stacktrace" [::mem/int] ::mem/int)
