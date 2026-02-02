(ns readme
  "Namespace for README code snippets."
  (:require [org.soulspace.arrayfire.core :as af]
            [tech.v3.datatype :as dtype]))

;; Example: Simple addition of two arrays

;; Initialize ArrayFire
(af/init!)

;; Print device information
(af/info)

;; Create arrays and perform operations with doubles
(let [a (af/create-array-f64 [1.0 2.0 3.0] [3])
      b (af/create-array-f64 [10.0 20.0 30.0] [3])
      c (af/add a b)]
  (println (af/to-host-f64 c 3))
  ;; => [11.0 22.0 33.0]
  (af/release a)
  (af/release b)
  (af/release c))

;; Complex number operations
(let [a (af/create-array-c64 [[1.0 2.0] [3.0 4.0]] [2])
      b (af/create-array-c64 [[5.0 6.0] [7.0 8.0]] [2])
      c (af/add a b)]
  (println (af/to-host-c64 c 2))
  ;; => [[6.0 8.0] [10.0 12.0]]
  (af/release a)
  (af/release b)
  (af/release c))

;; dtype-next integration (zero-copy on host)
(let [tensor (dtype/make-container :native-heap :float64 [1.0 2.0 3.0])
      arr (af/create-array-from-tensor tensor)
      result (af/to-native-buffer arr :float64 3)]
  (println (vec result))
  ;; => [1.0 2.0 3.0]
  (af/release arr))
