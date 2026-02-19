(ns org.soulspace.arrayfire.integration.base.memory
    "Utility functions for memory management and data conversion between
     Clojure and native code. This includes functions for writing/reading
     primitive types to/from MemorySegments, converting Clojure data structures
     to native formats, and handling C strings."
  (:require [coffi.mem :as mem])
  (:import [java.lang.foreign Arena MemorySegment ValueLayout]
           [java.nio.charset StandardCharsets]))

(def ^:dynamic *af-arena* nil)

(defn open-arena
  "Open an FFM Arena of the requested type.
  
   Parameters:
   - arena-type: `:confined` (default, thread-local, cheap allocation) or
                 `:shared`   (cross-thread safe, higher overhead)

   Returns:
   An open `java.lang.foreign.Arena` instance (use in `with-open`)."
  [arena-type]
  (case arena-type
    :confined (mem/confined-arena)
    :shared   (mem/shared-arena)
    (throw (ex-info (str "Unknown :arena-type " arena-type
                         ". Valid values: :confined, :shared")
                    {:arena-type arena-type}))))

;;
;; Null pointer helper for optional FFI parameters
;;
(def null-ptr
  "A null MemorySegment (address 0) for passing NULL to FFI functions.
   Many ArrayFire functions accept NULL for optional parameters."
  (mem/as-segment 0))

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