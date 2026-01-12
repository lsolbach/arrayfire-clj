(ns org.soulspace.arrayfire.core
  (:require [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi :as ffi]))

(defn init! []
  (ffi/check! (ffi/af-init) "af_init")
  true)


(defn info []
  (ffi/check! (ffi/af-info) "af_info")
  :ok)


(defn create-array
  "Create an ArrayFire array from a Clojure vector of values.
   Values are copied to native memory and dims specifies the array dimensions."
  [values dims]
  (let [n (count values)
        ;; Allocate host buffer for input data
        host (mem/alloc (* n 8)) ; 8 bytes per double
        _ (doseq [i (range n)]
            (mem/write-double host (* i 8) (double (nth values i))))
        dimsbuf (ffi/dims->native dims)
        outptr (mem/alloc mem/pointer-size)]
    (ffi/check!
     (ffi/af-create-array outptr host (int (count dims)) dimsbuf ffi/AF_DTYPE_F64)
     "af_create_array")
    ;; Return the array handle
    (mem/read-address outptr)))


(defn release
  "Release an ArrayFire array handle, freeing GPU memory"
  [handle]
  (ffi/check! (ffi/af-release-array handle) "af_release_array")
  true)


(defn add
  "Add two ArrayFire arrays element-wise"
  [a b]
  (let [outptr (mem/alloc mem/pointer-size)]
    (ffi/check! (ffi/af-add outptr a b 0) "af_add") ; 0 = false for batch parameter
    (mem/read-address outptr)))


(defn to-host
  "Copy ArrayFire array data to host memory, returning a double array.
   Note: n (number of elements) must be provided."
  [handle n]
  (let [buf (mem/alloc (* n 8))] ; 8 bytes per double
    (ffi/check! (ffi/af-get-data-ptr buf handle) "af_get_data_ptr")
    (let [arr (double-array n)]
      (doseq [i (range n)]
        (aset-double arr i (mem/read-double buf (* i 8))))
      arr)))