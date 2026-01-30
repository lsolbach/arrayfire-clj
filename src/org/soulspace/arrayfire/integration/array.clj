(ns org.soulspace.arrayfire.integration.array
  "Integration of the ArrayFire array related FFI bindings with the error
   handling and resource management on the JVM."
  (:refer-clojure :exclude [empty? vector? double? integer? bytes?])
  (:require [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.array :as array-ffi]
            [org.soulspace.arrayfire.integration.jvm-integration :as jvm])
  (:import (org.soulspace.arrayfire.integration.jvm_integration AFArray)))

;;;
;;; Type Helper Functions
;;;

(defn floats?
  "Check if data is a float array or a collection of floats."
  [data]
  (or (instance? (Class/forName "[F") data)
      (and (coll? data)
           (every? float? data))))

(defn ints?
  "Check if data is an int array or a collection of ints."
  [data]
  (or (instance? (Class/forName "[I") data)
      (and (coll? data)
           (every? int? data))))

(defn short?
  "Check if x is a short."
  [x]
  (and (clojure.core/integer? x)
       (<= Short/MIN_VALUE x Short/MAX_VALUE)))

(defn shorts?
  "Check if data is a short array or a collection of shorts."
  [data]
  (or (instance? (Class/forName "[S") data)
      (and (coll? data)
           (every? short? data))))

(defn doubles?
  "Check if data is a double array or a collection of doubles."
  [data]
  (or (instance? (Class/forName "[D") data)
      (and (coll? data)
           (every? #(instance? Double %) data))))

(defn longs?
  "Check if data is a long array or a collection of longs."
  [data]
  (or (instance? (Class/forName "[J") data)
      (and (coll? data)
           (every? #(instance? Long %) data))))

(defn bytes?
  "Check if data is a byte array or a collection of bytes."
  [data]
  (or (instance? (Class/forName "[B") data)
      (and (coll? data)
           (every? #(instance? Byte %) data))))

(defn complex-pair?
  "Check if x is a valid complex number pair [real imag]."
  [x]
  (and (clojure.core/vector? x)
       (= 2 (clojure.core/count x))
       (number? (first x))
       (number? (second x))))

(defn complex-floats?
  "Check if data is a collection of complex float pairs."
  [data]
  (and (coll? data)
       (every? complex-pair? data)
       (every? #(every? float? %) data)))

(defn complex-doubles?
  "Check if data is a collection of complex double pairs."
  [data]
  (and (coll? data)
       (every? complex-pair? data)
       (every? #(every? clojure.core/double? %) data)))

;;;
;;; Array Creation
;;;

(defn create-array
  "Create an ArrayFire array from host data.
   
   Parameters:
   - data: Native memory segment or primitive array containing the data
   - dims: Vector of dimensions [d0 d1 d2 d3]
   - dtype: ArrayFire data type constant (e.g., AF_DTYPE_F32)
   
   Returns:
   AFArray instance
   
   Example:
   (let [data (float-array [1.0 2.0 3.0 4.0])
         arr (create-array data [2 2] jvm/AF_DTYPE_F32)]
     arr)"
  ^AFArray
  [data dims dtype]
  (let [out (jvm/native-af-array-pointer)
        data-seg (cond
                   (floats? data) (jvm/float-array->segment data)
                   (doubles? data) (jvm/double-array->segment data)
                   (ints? data) (jvm/int-array->segment data)
                   (longs? data) (jvm/long-array->segment data)
                   (shorts? data) (jvm/short-array->segment data)
                   (bytes? data) (jvm/byte-array->segment data)
                   (complex-floats? data) (jvm/complex-float-array->segment data)
                   (complex-doubles? data) (jvm/complex-double-array->segment data)
                   :else data)
        dims-seg (jvm/dims->segment dims)]
    (jvm/check! (array-ffi/af-create-array out data-seg (clojure.core/count dims) dims-seg (int dtype))
                "af-create-array")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn create-handle
  "Create an empty array with specified dimensions and type.
   
   This allocates memory but does not initialize it. Use write-array
   to populate the data.
   
   Parameters:
   - dims: Vector of dimensions [d0 d1 d2 d3]
   - dtype: ArrayFire data type constant
   
   Returns:
   AFArray instance
   
   Example:
   (let [arr (create-handle [10 10] jvm/AF_DTYPE_F64)]
     arr)"
  ^AFArray
  [dims dtype]
  (let [out (jvm/native-af-array-pointer)
        ndims (clojure.core/count dims)
        dims-seg (jvm/dims->segment dims)]
    (jvm/check! (array-ffi/af-create-handle out (int ndims) dims-seg (int dtype))
                "af-create-handle")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn copy-array
  "Create a deep copy of an array.
   
   Parameters:
   - in: Input array (AFArray)
   
   Returns:
   New AFArray instance with copied data
   
   Example:
   (let [original (af/array [[1.0 2.0] [3.0 4.0]])
         copy (copy-array original)]
     copy)"
  ^AFArray
  [^AFArray in]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (array-ffi/af-copy-array out (jvm/af-handle in))
                "af-copy-array")
    (jvm/af-array-new (jvm/deref-af-array out))))

;;;
;;; Array Data Transfer
;;;

(defn write-array!
  "Write data from host/device memory to an existing array.
   
   This modifies the array in-place.
   
   Parameters:
   - arr: Target array (AFArray)
   - data: Native memory segment containing the data
   - bytes: Number of bytes to write
   - src: Source type (0 for host, 1 for device, default 0)
   
   Returns:
   The modified array
   
   Example:
   (let [arr (create-handle [4] jvm/AF_DTYPE_F32)
         data (float-array [1.0 2.0 3.0 4.0])]
     (write-array! arr data (* 4 4) 0))"
  ([^AFArray arr data bytes]
   (write-array! arr data bytes 0))
  ([^AFArray arr data bytes src]
   (jvm/check! (array-ffi/af-write-array (jvm/af-handle arr) data (long bytes) (int src))
               "af-write-array")
   arr))

(defn get-data-ptr
  "Copy array data from device to host memory.
   
   Parameters:
   - arr: Array to read from (AFArray)
   - data: Native memory segment to write data into
   
   Returns:
   nil (data is written to the provided buffer)
   
   Example:
   (let [arr (af/array [1.0 2.0 3.0])
         buf (mem/alloc (* 3 4))]
     (get-data-ptr arr buf)
     ;; buf now contains the array data
     )"
  [^AFArray arr data]
  (jvm/check! (array-ffi/af-get-data-ptr data (jvm/af-handle arr))
              "af-get-data-ptr")
  nil)

;;;
;;; Array Information
;;;

(defn get-elements
  "Get the total number of elements in an array.
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Long integer representing total elements
   
   Example:
   (let [arr (af/array [[1 2 3] [4 5 6]])
         n (get-elements arr)]
     n) ; => 6"
  [^AFArray arr]
  (let [elems-buf (mem/alloc 8)]
    (jvm/check! (array-ffi/af-get-elements elems-buf (jvm/af-handle arr))
                "af-get-elements")
    (mem/read-long elems-buf 0)))

(defn get-type
  "Get the data type of an array.
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Integer representing the ArrayFire dtype constant
   
   Example:
   (let [arr (af/array [1.0 2.0])
         dtype (get-type arr)]
     dtype) ; => AF_DTYPE_F32 or AF_DTYPE_F64"
  [^AFArray arr]
  (let [type-buf (mem/alloc 4)]
    (jvm/check! (array-ffi/af-get-type type-buf (jvm/af-handle arr))
                "af-get-type")
    (mem/read-int type-buf 0)))

(defn get-dims
  "Get the dimensions of an array.
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Vector of four dimension values [d0 d1 d2 d3]
   
   Example:
   (let [arr (af/array [[1 2 3] [4 5 6]])
         dims (get-dims arr)]
     dims) ; => [3 2 1 1] or similar"
  [^AFArray arr]
  (let [d0 (mem/alloc 8)
        d1 (mem/alloc 8)
        d2 (mem/alloc 8)
        d3 (mem/alloc 8)]
    (jvm/check! (array-ffi/af-get-dims d0 d1 d2 d3 (jvm/af-handle arr))
                "af-get-dims")
    [(mem/read-long d0 0)
     (mem/read-long d1 0)
     (mem/read-long d2 0)
     (mem/read-long d3 0)]))

(defn get-numdims
  "Get the number of dimensions of an array.
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Integer representing number of dimensions (1-4)
   
   Example:
   (let [arr (af/array [[1 2] [3 4]])
         ndims (get-numdims arr)]
     ndims) ; => 2"
  [^AFArray arr]
  (let [result-buf (mem/alloc 4)]
    (jvm/check! (array-ffi/af-get-numdims result-buf (jvm/af-handle arr))
                "af-get-numdims")
    (mem/read-int result-buf 0)))

(defn get-data-ref-count
  "Get the reference count of the array's underlying data.
   
   This indicates how many arrays share the same data buffer.
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Integer reference count
   
   Example:
   (let [arr (af/array [1 2 3])
         count (get-data-ref-count arr)]
     count)"
  [^AFArray arr]
  (let [count-buf (mem/alloc 4)]
    (jvm/check! (array-ffi/af-get-data-ref-count count-buf (jvm/af-handle arr))
                "af-get-data-ref-count")
    (mem/read-int count-buf 0)))

;;;
;;; Array Type Predicates
;;;

(defn empty?
  "Check if an array is empty (has zero elements).
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean"
  [^AFArray arr]
  (let [result-buf (mem/alloc 1)]
    (jvm/check! (array-ffi/af-is-empty result-buf (jvm/af-handle arr))
                "af-is-empty")
    (not (zero? (mem/read-byte result-buf 0)))))

(defn scalar?
  "Check if an array is a scalar (single element).
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean"
  [^AFArray arr]
  (let [result-buf (mem/alloc 1)]
    (jvm/check! (array-ffi/af-is-scalar result-buf (jvm/af-handle arr))
                "af-is-scalar")
    (not (zero? (mem/read-byte result-buf 0)))))

(defn row?
  "Check if an array is a row vector (1 x n).
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean"
  [^AFArray arr]
  (let [result-buf (mem/alloc 1)]
    (jvm/check! (array-ffi/af-is-row result-buf (jvm/af-handle arr))
                "af-is-row")
    (not (zero? (mem/read-byte result-buf 0)))))

(defn column?
  "Check if an array is a column vector (n x 1).
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean"
  [^AFArray arr]
  (let [result-buf (mem/alloc 1)]
    (jvm/check! (array-ffi/af-is-column result-buf (jvm/af-handle arr))
                "af-is-column")
    (not (zero? (mem/read-byte result-buf 0)))))

(defn vector?
  "Check if an array is a vector (either row or column).
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean"
  [^AFArray arr]
  (let [result-buf (mem/alloc 1)]
    (jvm/check! (array-ffi/af-is-vector result-buf (jvm/af-handle arr))
                "af-is-vector")
    (not (zero? (mem/read-byte result-buf 0)))))

(defn complex?
  "Check if an array has complex data type.
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean"
  [^AFArray arr]
  (let [result-buf (mem/alloc 1)]
    (jvm/check! (array-ffi/af-is-complex result-buf (jvm/af-handle arr))
                "af-is-complex")
    (not (zero? (mem/read-byte result-buf 0)))))

(defn real?
  "Check if an array has real (non-complex) data type.
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean"
  [^AFArray arr]
  (let [result-buf (mem/alloc 1)]
    (jvm/check! (array-ffi/af-is-real result-buf (jvm/af-handle arr))
                "af-is-real")
    (not (zero? (mem/read-byte result-buf 0)))))

(defn double?
  "Check if an array has double precision floating point type.
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean"
  [^AFArray arr]
  (let [result-buf (mem/alloc 1)]
    (jvm/check! (array-ffi/af-is-double result-buf (jvm/af-handle arr))
                "af-is-double")
    (not (zero? (mem/read-byte result-buf 0)))))

(defn single?
  "Check if an array has single precision floating point type.
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean"
  [^AFArray arr]
  (let [result-buf (mem/alloc 1)]
    (jvm/check! (array-ffi/af-is-single result-buf (jvm/af-handle arr))
                "af-is-single")
    (not (zero? (mem/read-byte result-buf 0)))))

(defn half?
  "Check if an array has half precision floating point type.
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean"
  [^AFArray arr]
  (let [result-buf (mem/alloc 1)]
    (jvm/check! (array-ffi/af-is-half result-buf (jvm/af-handle arr))
                "af-is-half")
    (not (zero? (mem/read-byte result-buf 0)))))

(defn realfloating?
  "Check if an array has real floating point type (float, double, half).
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean"
  [^AFArray arr]
  (let [result-buf (mem/alloc 1)]
    (jvm/check! (array-ffi/af-is-realfloating result-buf (jvm/af-handle arr))
                "af-is-realfloating")
    (not (zero? (mem/read-byte result-buf 0)))))

(defn floating?
  "Check if an array has floating point type (real or complex).
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean"
  [^AFArray arr]
  (let [result-buf (mem/alloc 1)]
    (jvm/check! (array-ffi/af-is-floating result-buf (jvm/af-handle arr))
                "af-is-floating")
    (not (zero? (mem/read-byte result-buf 0)))))

(defn integer?
  "Check if an array has integer data type.
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean"
  [^AFArray arr]
  (let [result-buf (mem/alloc 1)]
    (jvm/check! (array-ffi/af-is-integer result-buf (jvm/af-handle arr))
                "af-is-integer")
    (not (zero? (mem/read-byte result-buf 0)))))

(defn bool?
  "Check if an array has boolean data type.
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean"
  [^AFArray arr]
  (let [result-buf (mem/alloc 1)]
    (jvm/check! (array-ffi/af-is-bool result-buf (jvm/af-handle arr))
                "af-is-bool")
    (not (zero? (mem/read-byte result-buf 0)))))

(defn sparse?
  "Check if an array is stored in sparse format.
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   Boolean"
  [^AFArray arr]
  (let [result-buf (mem/alloc 1)]
    (jvm/check! (array-ffi/af-is-sparse result-buf (jvm/af-handle arr))
                "af-is-sparse")
    (not (zero? (mem/read-byte result-buf 0)))))

;;;
;;; Scalar Access
;;;

(defn get-scalar
  "Get the scalar value from a single-element array.
   
   The array must contain exactly one element.
   
   Parameters:
   - arr: Input array (AFArray) with one element
   - output-buffer: Native memory segment to write the value into
   
   Returns:
   nil (value is written to the buffer)
   
   Example:
   (let [arr (af/array [42.0])
         buf (mem/alloc 8)]
     (get-scalar arr buf)
     (mem/read-double buf 0)) ; => 42.0"
  [^AFArray arr output-buffer]
  (jvm/check! (array-ffi/af-get-scalar output-buffer (jvm/af-handle arr))
              "af-get-scalar")
  nil)



