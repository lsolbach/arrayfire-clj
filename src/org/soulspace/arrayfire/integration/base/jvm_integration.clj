(ns org.soulspace.arrayfire.integration.base.jvm-integration
  "This namespace contains the JVM integration of ArrayFire.
   
   It contains functions and types for:
   * Loading the ArrayFire library
   * Type Definitions
   * Resource Management

   Resource Management for ArrayFire Resources:
   
   ArrayFire uses reference counting for its array resources (af_array) and
   provides functions for the creation and release of these resources.
   This module provides wrappers to manage the lifecycle of these resources
   by integrating with AutoCloseable and Java's Cleaner mechanism to ensure
   proper release of resources when they are no longer needed.
      
   This namespace defines the AFArray type which encapsulates an af_array
   handle along with automatic cleanup. It relies on the reference counting
   functions provided by ArrayFire to manage the lifecycle of the resources
   and does not introduce additional reference counting.
   
   The AFArray type implements AutoCloseable, allowing users to explicitly
   release resources when done. Additionally, it uses Java's Cleaner to ensure
   that resources are released when the AFArray instance is garbage collected,
   preventing memory leaks."
  (:require [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.c-api.array :refer [af-release-array af-retain-array]]
            [org.soulspace.arrayfire.integration.base.error :refer [check!]])
  (:import [java.lang AutoCloseable]
           [java.lang.ref Cleaner Cleaner$Cleanable]
           [java.util.concurrent.atomic AtomicBoolean]
           [java.lang.foreign Arena MemorySegment ValueLayout]
           [java.nio.charset StandardCharsets]))

;;;
;;; Definitions
;;;

;;
;; Null pointer helper for optional FFI parameters
;;
(def null-ptr
  "A null MemorySegment (address 0) for passing NULL to FFI functions.
   Many ArrayFire functions accept NULL for optional parameters."
  (mem/as-segment 0))

;;
;; ArrayFire dtype constants (af/defines.h - enum af_dtype)
;;
(def AF_DTYPE_F32 0)   ; float
(def AF_DTYPE_C32 1)   ; complex float
(def AF_DTYPE_F64 2)   ; double
(def AF_DTYPE_C64 3)   ; complex double
(def AF_DTYPE_B8  4)   ; bool
(def AF_DTYPE_S32 5)   ; int
(def AF_DTYPE_U32 6)   ; unsigned int
(def AF_DTYPE_U8  7)   ; unsigned char
(def AF_DTYPE_S64 8)   ; long long
(def AF_DTYPE_U64 9)   ; unsigned long long
(def AF_DTYPE_S16 10)  ; short
(def AF_DTYPE_U16 11)  ; unsigned short

;; Type size lookup
(def type-sizes
  "Size in bytes for each ArrayFire dtype."
  {AF_DTYPE_F32 4    ; float
   AF_DTYPE_C32 8    ; complex float (2 floats)
   AF_DTYPE_F64 8    ; double
   AF_DTYPE_C64 16   ; complex double (2 doubles)
   AF_DTYPE_B8  1    ; bool
   AF_DTYPE_S32 4    ; int
   AF_DTYPE_U32 4    ; unsigned int
   AF_DTYPE_U8  1    ; unsigned char
   AF_DTYPE_S64 8    ; long long
   AF_DTYPE_U64 8    ; unsigned long long
   AF_DTYPE_S16 2    ; short
   AF_DTYPE_U16 2})  ; unsigned short
   
;;
;; ArrayFire error codes (af/defines.h - enum af_err)
;;
(def AF_SUCCESS 0)
(def AF_ERR_NO_MEM 101)
(def AF_ERR_DRIVER 102)
(def AF_ERR_RUNTIME 103)
(def AF_ERR_INVALID_ARRAY 201)
(def AF_ERR_ARG 202)
(def AF_ERR_SIZE 203)
(def AF_ERR_TYPE 204)
(def AF_ERR_DIFF_TYPE 205)
(def AF_ERR_BATCH 207)
(def AF_ERR_DEVICE 208)
(def AF_ERR_NOT_SUPPORTED 301)
(def AF_ERR_NOT_CONFIGURED 302)
(def AF_ERR_NONFREE 303)
(def AF_ERR_NO_DBL 401)
(def AF_ERR_NO_GFX 402)
(def AF_ERR_NO_HALF 403)
(def AF_ERR_LOAD_LIB 501)
(def AF_ERR_LOAD_SYM 502)
(def AF_ERR_ARR_BKND_MISMATCH 503)
(def AF_ERR_INTERNAL 998)
(def AF_ERR_UNKNOWN 999)

;;;
;;; AFArray resource management
;;;
(defn native-af-array-pointer
  "Allocate an af_array* for use as an out-parameter.
   Must be used within a dynamic Arena."
  ^MemorySegment
  ([]
   ;; One pointer-sized slot
   (.allocate (Arena/ofAuto) ValueLayout/ADDRESS))
  ([^Arena arena]
   ;; One pointer-sized slot
   (.allocate arena ValueLayout/ADDRESS)))

(defn deref-af-array
  "Read an af_array value from an af_array* out-parameter
   and return it as a raw address (long)."
  ^long
  [^MemorySegment af-array-ptr]
  ;; Read the pointer stored at offset 0
  (let [addr (mem/read-address af-array-ptr)]
    (if (instance? MemorySegment addr)
      (mem/address-of addr)
      (long addr))))

(defn address->long
  "Normalize a MemorySegment or numeric address into a long value."
  ^long
  [addr]
  (cond
    (instance? MemorySegment addr) (mem/address-of addr)
    (number? addr) (long addr)
    :else (throw (IllegalArgumentException.
                  (str "Unsupported address type: " (type addr))))))

(defn af-release-array!
  "Release one reference to an af_array.
   
   Parameters:
   - handle: af_array* handle
   
   Returns: nil"
  [^long handle]
  (let [seg (MemorySegment/ofAddress handle)]
    (check! (af-release-array seg) "af_release_array")
    nil))

(defn af-retain-array!
  "Increment the refcount of an af_array.
   
   Parameters:
   - handle: af_array* handle
   
   Returns: nil"
  [^long handle]
  (let [arena (Arena/ofConfined)]
    (try 
      (let [out (.allocate arena ValueLayout/ADDRESS)
            in  (MemorySegment/ofAddress handle)]
        (check! (af-retain-array out in) "af_retain_array")
        ;; IMPORTANT:
        ;; Intentionally discarded `out`
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
          ^Cleaner$Cleanable cleanable
          ^Object cleanup-key]

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
  "Wrap an af_array returned from ArrayFire (refcount = 1).
   
   Parameters:
   - handle: af_array* handle
   
   Returns:
   AFArray instance"
  ^AFArray
  [^long handle]
  (let [released (AtomicBoolean. false)
        cleanup  (AFArrayCleanup. handle released)
        cleanup-key (Object.)
        cleanable (.register cleaner cleanup-key cleanup)]
    (AFArray. handle released cleanable cleanup-key)))

(defn af-array-retained
  "Wrap an existing af_array; retains before wrapping.
   
   Parameters:
   - handle: af_array* handle
   
   Returns:
   AFArray instance"
  ^AFArray
  [^long handle]
  (af-retain-array! handle)
  (af-array-new handle))

(defn af-handle
  "Get the native af_array* handle from AFArray.
   
   Parameters:
   - arr: AFArray instance
   
   Returns:
   af_array* handle as MemorySegment"
  ^MemorySegment [arr]
  (let [released (clojure.lang.Reflector/getInstanceField arr "released")
        handle   (clojure.lang.Reflector/getInstanceField arr "handle")
        handle-value (long handle)]
    (when (and released (.get ^AtomicBoolean released))
      (throw (IllegalStateException.
              "AFArray has already been closed")))
    (MemorySegment/ofAddress handle-value)))

(defn af-handle-value
  "Get the native af_array* handle from AFArray.
   
   Parameters:
   - arr: AFArray instance
   
   Returns:
   af_array* handle as long"
  ^long [arr]
  (mem/address-of (af-handle arr)))

(defn duplicate-array
  "Create a new AFArray with independent lifetime.
   
   Parameters:
   - a: AFArray to duplicate
   
   Returns:
   New AFArray instance with its own reference count"
  [^AFArray a]
  (af-array-retained (af-handle a)))

;;;
;;; Type-specific memory operations
;;;
(defn write-float!
  "Write a float value to buffer at offset.
   
   Parameters:
   - buf: buffer pointer
   - offset: byte offset
   - value: float value to write

   Returns:
   nil"
  [buf offset value]
  (mem/write-float buf offset (float value)))

(defn read-float
  "Read a float value from buffer at offset.
   
   Parameters:
   - buf: buffer pointer
   - offset: byte offset

   Returns:
   float value read from buffer"
  [buf offset]
  (mem/read-float buf offset))

(defn write-double!
  "Write a double value to buffer at offset.
   
   Parameters:
   - buf: buffer pointer
   - offset: byte offset
   - value: double value to write

   Returns:
   nil"
  [buf offset value]
  (mem/write-double buf offset (double value)))

(defn read-double
  "Read a double value from buffer at offset.
   
   Parameters:
   - buf: buffer pointer
   - offset: byte offset

   Returns:
   double value read from buffer"
  [buf offset]
  (mem/read-double buf offset))

(defn write-int!
  "Write an int value to buffer at offset.
   
   Parameters:
   - buf: buffer pointer
   - offset: byte offset
   - value: int value to write

   Returns:
   nil"
  [buf offset value]
  (mem/write-int buf offset (int value)))

(defn read-int
  "Read an int value from buffer at offset.
   
   Parameters:
   - buf: buffer pointer
   - offset: byte offset

   Returns:
   int value read from buffer"
  [buf offset]
  (mem/read-int buf offset))

(defn write-long!
  "Write a long value to buffer at offset.
   
   Parameters:
   - buf: buffer pointer
   - offset: byte offset
   - value: long value to write

   Returns:
   nil"
  [buf offset value]
  (mem/write-long buf offset (long value)))

(defn read-long
  "Read a long value from buffer at offset.
   
   Parameters:
   - buf: buffer pointer
   - offset: byte offset

   Returns:
   long value read from buffer"
  [buf offset]
  (mem/read-long buf offset))

(defn write-short!
  "Write a short value to buffer at offset.
   
   Parameters:
    - buf: buffer pointer
    - offset: byte offset
    - value: short value to write

    Returns:
    nil"
  [buf offset value]
  (mem/write-short buf offset (short value)))

(defn read-short
  "Read a short value from buffer at offset.
   
   Parameters:
    - buf: buffer pointer
    - offset: byte offset

    Returns:
    short value read from buffer"
  [buf offset]
  (mem/read-short buf offset))

(defn write-byte!
  "Write a byte value to buffer at offset.
   
   Parameters:
   - buf: buffer pointer
   - offset: byte offset
   - value: byte value to write

   Returns:
   nil"
  [buf offset value]
  (mem/write-byte buf offset (byte value)))

(defn read-byte
  "Read a byte value from buffer at offset.
   
   Parameters:
   - buf: buffer pointer
   - offset: byte offset

   Returns:
   byte value read from buffer"
  [buf offset]
  (mem/read-byte buf offset))

;; Complex number operations
;; Complex numbers are represented as [real imag] vectors
;; In memory, they're stored as consecutive real/imag pairs

(defn write-complex-float!
  "Write a complex float to buffer at offset.
   
   Parameters:
   - buf: buffer pointer
   - offset: byte offset
   - value: [real imag] vector to write

   Returns:
   nil"
  [buf offset [real imag]]
  (mem/write-float buf offset (float real))
  (mem/write-float buf (+ offset 4) (float imag)))

(defn read-complex-float
  "Read a complex float from buffer at offset.
   
   Parameters:
   - buf: buffer pointer
   - offset: byte offset

   Returns:
   [real imag] vector"
  [buf offset]
  [(mem/read-float buf offset)
   (mem/read-float buf (+ offset 4))])

(defn write-complex-double!
  "Write a complex double to buffer at offset.
   
   Parameters:
   - buf: buffer pointer
   - offset: byte offset
   - value: [real imag] vector to write

   Returns:
   nil"
  [buf offset [real imag]]
  (mem/write-double buf offset (double real))
  (mem/write-double buf (+ offset 8) (double imag)))

(defn read-complex-double
  "Read a complex double from buffer at offset.
   
   Parameters:
   - buf: buffer pointer
   - offset: byte offset

   Returns:
   [real imag] vector"
  [buf offset]
  [(mem/read-double buf offset)
   (mem/read-double buf (+ offset 8))])

;;;
;;; Conversion functions
;;;

;;
;; C String conversions
;;

(defn string->c-string
  "Allocate a null-terminated C string from a Clojure string.
   
   Parameters:
   - s: Clojure string
   - arena: Arena for allocation (optional, defaults to auto arena)
   
   Returns:
   MemorySegment containing the null-terminated C string"
  ([^String s]
   (string->c-string s (Arena/ofAuto)))
  ([^String s ^Arena arena]
   (let [bytes (.getBytes s StandardCharsets/UTF_8)
         len   (alength bytes)
         seg   (.allocate arena (long (inc len)))]
     (dotimes [idx len]
       (mem/write-byte seg idx (aget bytes idx)))
     (mem/write-byte seg len (byte 0))
     seg)))

(defn c-string->string
  "Read a null-terminated C string from a MemorySegment.
   
   Parameters:
   - segment: MemorySegment containing the C string
   
   Returns:
   Clojure string"
  ([^MemorySegment segment]
   (c-string->string segment 4096))
  ([^MemorySegment segment max-bytes]
   (let [bounded-seg (if (pos? (.byteSize segment))
                       segment
                       (mem/reinterpret segment max-bytes))
         buffer (java.io.ByteArrayOutputStream.)]
     (loop [idx 0]
       (when (< idx max-bytes)
         (let [b (mem/read-byte bounded-seg idx)]
           (if (zero? b)
             (String. (.toByteArray buffer) StandardCharsets/UTF_8)
             (do
               (.write buffer (bit-and 0xFF b))
               (recur (inc idx))))))))))


; TODO define dim_t type via coffi
(defn dims->native
  "Convert Clojure vector of dimensions to native dim_t array.
   dim_t is typically long long (64-bit) on most platforms.
   
   Parameters:
   - dims: vector of dimension sizes
   
   Returns:
   pointer to native dim_t array"
  [dims]
  (let [buf (mem/alloc (* 8 (count dims)))] ; dim_t is 64-bit
    (doseq [i (range (count dims))]
      (mem/write-long buf (* i 8) (long (nth dims i))))
    buf))

(defn dims->segment
  "Convert Clojure vector of dimensions to MemorySegment of dim_t array.
   dim_t is typically long long (64-bit) on most platforms.
   
   Parameters:
   - dims: vector of dimension sizes
   
   Returns:
   MemorySegment containing dim_t array"
  ^MemorySegment
  [dims]
  (let [arena (Arena/ofAuto)
        n     (long (count dims))
        seg   (.allocate arena (long (* n (.byteSize ValueLayout/JAVA_LONG))))]
    (dotimes [i n]
      (mem/write-long seg (* i (.byteSize ValueLayout/JAVA_LONG))
                      (long (nth dims i))))
    seg))

(defn float-array->segment
  "Convert Clojure float array to MemorySegment.
   
   Parameters:
   - data: Clojure float array
   
   Returns:
   MemorySegment containing float array"
  ^MemorySegment
  [^floats data]
  (let [n (alength data)
        buf (mem/alloc (* n 4))]
    (dotimes [i n]
      (mem/write-float buf (* i 4) (aget data i)))
    buf))

(defn double-array->segment
  "Convert Clojure double array to MemorySegment.
   
   Parameters:
   - data: Clojure double array
   
   Returns:
   MemorySegment containing double array"
  ^MemorySegment
  [^doubles data]
  (let [n (alength data)
        buf (mem/alloc (* n 8))]
    (dotimes [i n]
      (mem/write-double buf (* i 8) (aget data i)))
    buf))

(defn int-array->segment
  "Convert Clojure int array to MemorySegment.
   
   Parameters:
   - data: Clojure int array
   
   Returns:
   MemorySegment containing int array"
  ^MemorySegment
  [^ints data]
  (let [n (alength data)
        buf (mem/alloc (* n 4))]
    (dotimes [i n]
      (mem/write-int buf (* i 4) (aget data i)))
    buf))

(defn long-array->segment
  "Convert Clojure long array to MemorySegment.
   
   Parameters:
   - data: Clojure long array
   
   Returns:
   MemorySegment containing long array"
  ^MemorySegment
  [^longs data]
  (let [n (alength data)
        buf (mem/alloc (* n 8))]
    (dotimes [i n]
      (mem/write-long buf (* i 8) (aget data i)))
    buf))

(defn short-array->segment
  "Convert Clojure short array to MemorySegment.
   
   Parameters:
   - data: Clojure short array
   
   Returns:
   MemorySegment containing short array"
  ^MemorySegment
  [^shorts data]
  (let [n (alength data)
        buf (mem/alloc (* n 2))]
    (dotimes [i n]
      (mem/write-short buf (* i 2) (aget data i)))
    buf))

(defn byte-array->segment
  "Convert Clojure byte array to MemorySegment.
   
   Parameters:
   - data: Clojure byte array
   
   Returns:
   MemorySegment containing byte array"
  ^MemorySegment
  [^bytes data]
  (let [n (alength data)
        buf (mem/alloc n)]
    (dotimes [i n]
      (mem/write-byte buf i (aget data i)))
    buf))

(defn complex-float-array->segment
  "Convert collection of [real imag] pairs to interleaved float array MemorySegment.
   
   Parameters:
   - data: Collection of [real imag] pairs for complex numbers
   
   Returns:
   MemorySegment containing interleaved float array [real1 imag1 real2 imag2 ...]"
  ^MemorySegment
  [data]
  (let [pairs (vec data)
        n (* 2 (count pairs))
        buf (mem/alloc (* n 4))]
    (doseq [[idx [real imag]] (map-indexed vector pairs)]
      (let [base (* idx 8)]
        (mem/write-float buf base (float real))
        (mem/write-float buf (+ base 4) (float imag))))
    buf))

(defn complex-double-array->segment
  "Convert collection of [real imag] pairs to interleaved double array MemorySegment.
   
   Parameters:
   - data: Collection of [real imag] pairs for complex numbers
   
   Returns:
   MemorySegment containing interleaved double array [real1 imag1 real2 imag2 ...]"
  ^MemorySegment
  [data]
  (let [pairs (vec data)
        n (* 2 (count pairs))
        buf (mem/alloc (* n 8))]
    (doseq [[idx [real imag]] (map-indexed vector pairs)]
      (let [base (* idx 16)]
        (mem/write-double buf base (double real))
        (mem/write-double buf (+ base 8) (double imag))))
    buf))