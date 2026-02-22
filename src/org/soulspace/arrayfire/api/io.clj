(ns org.soulspace.arrayfire.api.io
  "Idiomatic Clojure API for ArrayFire array I/O operations.

   Provides functions for saving and loading ArrayFire arrays to and from
   disk using ArrayFire's native binary `.af` format.

   ## File format

   The `.af` format is ArrayFire's own binary format.  It preserves:
   - The exact data type (no conversion or precision loss)
   - Array shape / dimensions
   - All element values
   - String key (tag) for named lookup

   Multiple arrays may be stored in a single file using `append? true`.

   ## Quick start

   ```clojure
   (require '[org.soulspace.arrayfire.api.core :as af]
            '[org.soulspace.arrayfire.api.io   :as io])

   ;; Save a single array
   (af/with-arrayfire {:backend :cpu}
     (let [w (af/random-normal [256 128] :f32)]
       (io/save-array \"model.af\" \"layer1\" w)))

   ;; Append a second array to the same file
   (af/with-arrayfire {:backend :cpu}
     (let [b (af/zeros [128] :f32)]
       (io/save-array \"model.af\" \"bias1\" b {:append? true})))

   ;; Load by key
   (af/with-arrayfire {:backend :cpu}
     (io/load-array \"model.af\" \"layer1\"))

   ;; Save a whole map of arrays
   (af/with-arrayfire {:backend :cpu}
     (io/save-array-map \"model.af\" {\"layer1\" w \"bias1\" b}))
   ```

   All functions **must** be called inside a `with-arrayfire` region."
  (:require [org.soulspace.arrayfire.integration.unified-api.util :as util]
            [org.soulspace.arrayfire.api.core :refer [assert-within-arrayfire!]])
  (:import (org.soulspace.arrayfire.integration.base.resource AFArray)))

;;;
;;; Saving
;;;

(defn save-array
  "Save `arr` to an ArrayFire binary file at `path` under the given `key`.

   Parameters:
   - path: File path string (e.g. \"checkpoint.af\")
   - key:  String name/tag for the array within the file
   - arr:  AFArray to save

   Options map (optional fourth argument):
   - :append?  — if true, append to an existing file instead of overwriting
                 (default false)

   Returns:
   Integer index (0-based) of the saved array within the file.
   Requires an active `with-arrayfire` region.

   Examples:
   (save-array \"model.af\" \"weights\" w)
   (save-array \"model.af\" \"bias\" b {:append? true})"
  ([path key ^AFArray arr]
   (save-array path key arr {}))
  ([path key ^AFArray arr {:keys [append?] :or {append? false}}]
   (assert-within-arrayfire! "save-array")
   (util/save-array key arr path append?)))

(defn save-array-map
  "Save a map of string→AFArray pairs to a single file at `path`.

   The first array in the map is written to a new file (or overwrites an
   existing one); all subsequent arrays are appended.

   Parameters:
   - path:      File path string
   - array-map: Map of {string-key AFArray} pairs

   Returns:
   Map of {key index} with the saved index for each key.
   Requires an active `with-arrayfire` region.

   Example:
   (save-array-map \"model.af\" {\"w1\" layer1 \"b1\" bias1})"
  [path array-map]
  (assert-within-arrayfire! "save-array-map")
  (let [entries (seq array-map)]
    (when (empty? entries)
      (throw (ex-info "save-array-map: array-map must not be empty" {:path path})))
    (reduce
     (fn [acc [i [k arr]]]
       (assoc acc k (util/save-array k arr path (pos? i))))
     {}
     (map-indexed vector entries))))

;;;
;;; Loading
;;;

(defn load-array
  "Load an array from the `.af` file at `path` by its string `key`.

   Performs a linear key search through the file. Use `load-array-at`
   when you already know the array index for faster access.

   Parameters:
   - path: File path string
   - key:  String name/tag identifying the array

   Returns:
   AFArray loaded from the file.
   Requires an active `with-arrayfire` region.

   Throws:
   Exception if `key` is not found in the file.

   Example:
   (load-array \"model.af\" \"weights\")"
  [path key]
  (assert-within-arrayfire! "load-array")
  (util/read-array-key path key))

(defn load-array-at
  "Load an array from the `.af` file at `path` by its integer `index`.

   O(1) access — faster than `load-array` when the index is already known.
   Arrays are indexed 0-based in the order they were saved.

   Parameters:
   - path:  File path string
   - index: 0-based integer position within the file

   Returns:
   AFArray loaded from the file.
   Requires an active `with-arrayfire` region.

   Example:
   (load-array-at \"model.af\" 0)  ; load the first saved array"
  [path index]
  (assert-within-arrayfire! "load-array-at")
  (util/read-array-index path index))

;;;
;;; Introspection
;;;

(defn array-key-index
  "Return the 0-based index of the array tagged `key` in the file at `path`,
   or `nil` if the key is not found.

   Useful for verifying key existence and converting a key to an index
   before calling `load-array-at` for repeated fast access.

   Parameters:
   - path: File path string
   - key:  String name/tag to look up

   Returns:
   Non-negative integer index if found, `nil` otherwise.
   Requires an active `with-arrayfire` region.

   Example:
   (array-key-index \"model.af\" \"weights\")  ; => 0 or nil"
  [path key]
  (assert-within-arrayfire! "array-key-index")
  (let [idx (util/read-array-key-check path key)]
    (when (>= idx 0) idx)))

(defn array-exists?
  "Return `true` if an array tagged `key` exists in the file at `path`.

   Parameters:
   - path: File path string
   - key:  String name/tag to look up

   Returns:
   Boolean.
   Requires an active `with-arrayfire` region.

   Example:
   (array-exists? \"checkpoint.af\" \"epoch-42\")  ; => true / false"
  [path key]
  (assert-within-arrayfire! "array-exists?")
  (boolean (array-key-index path key)))

(comment
  ;; api.io REPL experiments — must be run inside with-arrayfire.
  (require '[org.soulspace.arrayfire.api.core :as af]
           '[org.soulspace.arrayfire.api.io   :as io])

  ;; Round-trip: save then load
  (af/with-arrayfire {:backend :cpu :converter-fn af/->value}
    (let [w (af/array [1.0 2.0 3.0 4.0] [2 2])
          _  (io/save-array "/tmp/test.af" "weights" w)
          w2 (io/load-array "/tmp/test.af" "weights")]
      {:saved  (af/->value w)
       :loaded (af/->value w2)}))
  ;; => {:saved [[1.0 2.0] [3.0 4.0]], :loaded [[1.0 2.0] [3.0 4.0]]}

  ;; Save a map and verify existence
  (af/with-arrayfire {:backend :cpu}
    (let [w (af/array [1.0 2.0] [2])
          b (af/zeros [2])]
      (io/save-array-map "/tmp/model.af" {"w" w "b" b})
      {:w-exists? (io/array-exists? "/tmp/model.af" "w")
       :b-index   (io/array-key-index "/tmp/model.af" "b")
       :missing?  (io/array-exists? "/tmp/model.af" "no-such-key")}))
  ;
  )