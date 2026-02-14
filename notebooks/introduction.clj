(ns introduction
  (:require [org.soulspace.arrayfire.ffi.base.definitions :as defs]
            [org.soulspace.arrayfire.integration.base.resource :as jvm]
            [org.soulspace.arrayfire.integration.unified-api.arith :as arith]
            [org.soulspace.arrayfire.integration.unified-api.array :as array]
            [org.soulspace.arrayfire.integration.unified-api.data :as data]
            [org.soulspace.arrayfire.integration.unified-api.device :as device]
            [org.soulspace.arrayfire.integration.unified-api.memory :as memory]
            [org.soulspace.arrayfire.integration.unified-api.lapack :as lapack]
            [org.soulspace.arrayfire.integration.unified-api.random :as random]
            [org.soulspace.arrayfire.integration.unified-api.util :as util]
            [coffi.mem :as mem]))

;; # Introduction to ArrayFire-CLJ
;; This notebook provides an introduction to using the [ArrayFire-CLJ](https://github.com/lsolbach/arrayfire-clj) library,
;; a Clojure wrapper for the [ArrayFire](https://github.com/arrayfire/arrayfire) tensor math library. We will cover the
;; basics of initializing the library, creating and manipulating arrays, and
;; managing resources.
;;
;; ## ArrayFire
;; [ArrayFire](https://github.com/arrayfire/arrayfire) is a general-purpose
;; tensor library that simplifies the software development process for the
;; parallel architectures found in CPUs, GPUs, and other hardware
;; acceleration devices.
;; 
;; ArrayFire provides:
;; * Hundreds of functions in the following categories
;;   * Array handling
;;   * Computer vision
;;   * Image processing
;;   * Linear algebra
;;   * Machine learning
;;   * Standard math
;;   * Signal Processing
;;   * Statistics
;;   * Vector algorithms
;; * Cross-platform compatibility with support for CUDA, oneAPI, OpenCL,
;;   and native CPU on Windows, Mac, and Linux
;;

;; ## ArrayFire-CLJ
;; [ArrayFire-CLJ](https://github.com/lsolbach/arrayfire-clj) is a Clojure
;; wrapper around the ArrayFire library, providing a more idiomatic Clojure
;; interface for working with ArrayFire. It abstracts away the complexities of
;; the underlying C API and allows you to work with ArrayFire arrays using
;; familiar Clojure data structures and functions.
;;
;; The library also includes integration tests to ensure that the core
;; functionality works correctly across different platforms and configurations.
;;
;; In this notebook, we will explore the core features of ArrayFire-CLJ and how
;; to use it effectively in your Clojure projects. We will cover device
;; initialization, array creation and manipulation, memory management,
;; and error handling. By the end of this notebook, you should have a solid
;; understanding of how to use ArrayFire-CLJ to perform high-performance
;; computations in Clojure.
;;
;; ## Initialization
;; Before using ArrayFire, we need to initialize the library and select a
;; device. The `device` namespace provides functions for this purpose.

(device/init!)

;; This initializes ArrayFire and selects the default device. You can also
;; specify a backend (e.g., CUDA, OpenCL) if you have multiple backends
;; available.
;;
;; ## Device Information
;; We can query information about the available devices and their capabilities.

(let [device-count (device/get-device-count)
      current-device (device/get-device)
      device-info (device/info-string)
      dbl-support (device/dbl-support?)
      half-support (device/half-support?)
      mem-info (device/device-mem-info)]
  (println "Device Count:" device-count)
  (println "Current Device ID:" current-device)
  (println "Device Info:\n" device-info)
  (println "Double Precision Support:" dbl-support)
  (println "Half Precision Support:" half-support)
  (println "Memory Info:" mem-info))

;; ## Array Creation and Properties
;; The `array` namespace provides functions for creating ArrayFire arrays.
;; We can create arrays from Clojure data structures or directly from memory.
;; We can also create arrays filled with random or constant values, using
;; the `random` and `data` namespaces.
;;
;; Let's create a random 3x3 array of complex numbers and explore its properties.
;; Note that we use `with-open` to ensure that the array is properly closed and
;; resources are released when we're done with it.

(with-open [array (random/randu [6 6] defs/AF_DTYPE_C64)]

  ;; Array representation
  (println (util/array-to-string "Array" array 2))

  ;; Array metadata
  (println "Array Allocated Bytes:" (array/allocated-bytes array))
  (println "Array Type:" (array/get-type array))
  (println "Array Num Dims:" (array/get-numdims array))
  (println "Array Dims:" (array/get-dims array))
  (println "Array Elements:" (array/get-elements array))

  ;; Array shape predicates
  (println "Array Empty?" (array/empty? array))
  (println "Array Column?" (array/column? array))
  (println "Array Row?" (array/row? array))
  (println "Array Scalar?" (array/scalar? array))
  (println "Array Vector?" (array/vector? array))
  (println "Array Sparse?" (array/sparse? array))

  ;; Array data type predicates
  (println "Array Boolean?" (array/bool? array))
  (println "Array Bytes?" (array/bytes? array))
  (println "Array Complex?" (array/complex? array))
  (println "Array Complex Floats?" (array/complex-floats? array))
  (println "Array Complex Doubles?" (array/complex-doubles? array))
  (println "Array Complex Pair?" (array/complex-pair? array))
  (println "Array Double?" (array/double? array))
  (println "Array Doubles?" (array/doubles? array))
  (println "Array Floating?" (array/floating? array))
  (println "Array Floats?" (array/floats? array))
  (println "Array Half?" (array/half? array))
  (println "Array Integer?" (array/integer? array))
  (println "Array Ints?" (array/ints? array))
  (println "Array Longs?" (array/longs? array))
  (println "Array Real?" (array/real? array))
  (println "Array Real Floating?" (array/realfloating? array))
  (println "Array Short?" (array/short? array))
  (println "Array Shorts?" (array/shorts? array))
  (println "Array Single?" (array/single? array)))

;; Now let's create a 4x4 array of integers filled with the value 42 and explore its properties.
(with-open [array (data/constant 42 [4 4] defs/AF_DTYPE_S32)]

  ;; Array representation
  (println (util/array-to-string "Array" array 2))

  ;; Array metadata
  (println "Array Allocated Bytes:" (array/allocated-bytes array))
  (println "Array Type:" (array/get-type array))
  (println "Array Num Dims:" (array/get-numdims array))
  (println "Array Dims:" (array/get-dims array))
  (println "Array Elements:" (array/get-elements array))

  ;; Array shape predicates
  (println "Array Empty?" (array/empty? array))
  (println "Array Column?" (array/column? array))
  (println "Array Row?" (array/row? array))
  (println "Array Scalar?" (array/scalar? array))
  (println "Array Vector?" (array/vector? array))
  (println "Array Sparse?" (array/sparse? array))

  ;; Array data type predicates
  (println "Array Boolean?" (array/bool? array))
  (println "Array Bytes?" (array/bytes? array))
  (println "Array Complex?" (array/complex? array))
  (println "Array Complex Floats?" (array/complex-floats? array))
  (println "Array Complex Doubles?" (array/complex-doubles? array))
  (println "Array Complex Pair?" (array/complex-pair? array))
  (println "Array Double?" (array/double? array))
  (println "Array Doubles?" (array/doubles? array))
  (println "Array Floating?" (array/floating? array))
  (println "Array Floats?" (array/floats? array))
  (println "Array Half?" (array/half? array))
  (println "Array Integer?" (array/integer? array))
  (println "Array Ints?" (array/ints? array))
  (println "Array Longs?" (array/longs? array))
  (println "Array Real?" (array/real? array))
  (println "Array Real Floating?" (array/realfloating? array))
  (println "Array Short?" (array/short? array))
  (println "Array Shorts?" (array/shorts? array))
  (println "Array Single?" (array/single? array)))

;; ## Array Operations
;; ArrayFire provides a wide range of operations that can be performed on arrays,
;; including element-wise operations, reductions, and linear algebra functions.

;; Let's perform some basic operations on arrays.
(with-open [array1 (data/constant 3.14 [4 4] defs/AF_DTYPE_F32)
            array2 (data/range [4 4 1 1] 0 defs/AF_DTYPE_F32)
            ;; Element-wise addition
            added (arith/add array1 array2)
            ;; Element-wise multiplication
            multiplied (arith/mul array1 array2)
            ;; Element-wise sine
            sine (arith/sin array2)]

  ;; Array representation
  (println (util/array-to-string "Array1" array1 2))
  (println (util/array-to-string "Array2" array2 2))
  (println (util/array-to-string "Added" added 2))
  (println (util/array-to-string "Multiplied" multiplied 2))
  (println (util/array-to-string "Sine" sine 2))

  
  (let [v (lapack/lu array1)]
    (with-open [l (first v)
                u (second v)
                p (nth v 2)]
      (println (util/array-to-string "L" l 2))
      (println (util/array-to-string "U" u 2))
      (println (util/array-to-string "P" p 2))))
  ;
  )
