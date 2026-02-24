(ns readme
  "Namespace for README code snippets.")

;; Require the arrayfire API core namespace.
(require '[org.soulspace.arrayfire.api.core :as af])

;; with in the arrayfire context do some array math 
(af/with-arrayfire {:backend :opencl
                    :converter-fn af/->value}
  (-> (af/array [1.0 2.0 3.0 4.0] [2 2])
      (af/* (af/array [1.0 2.0 4.0 8.0] [2 2]))
      (af/sin)))

