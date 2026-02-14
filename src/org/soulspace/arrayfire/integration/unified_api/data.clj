(ns org.soulspace.arrayfire.integration.unified-api.data
  "Integration of the ArrayFire data related FFI bindings with the error
   handling and resource management on the JVM."
  (:refer-clojure :exclude [identity range])
  (:require [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.c-api.data :as data]
            [org.soulspace.arrayfire.ffi.base.definitions :as defs]
            [org.soulspace.arrayfire.integration.base.error :refer [check!]]
            [org.soulspace.arrayfire.integration.base.memory :as bmem]
            [org.soulspace.arrayfire.integration.base.jvm-integration :as jvm])
  (:import (org.soulspace.arrayfire.integration.base.jvm_integration AFArray)))

;;;
;;; Data Generation Functions
;;;

(defn constant
  "Generate an array with elements set to a specified value.
   
   Parameters:
   - value: The constant value (as double)
   - dims: Vector of dimensions [d0 d1 d2 d3]
   - dtype: ArrayFire dtype constant (default: AF_DTYPE_F32)
   
   Returns:
   AFArray filled with the constant value"
  ([value dims]
   (constant value dims defs/AF_DTYPE_F32))
  ([value dims dtype]
   (let [out (jvm/native-af-array-pointer)
         ndims (count dims)
         dims-seg (bmem/dims->segment dims)]
     (check! (data/af-constant out (double value) ndims dims-seg (int dtype))
                 "af-constant")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn constant-complex
  "Generate an array with complex elements set to a specified value.
   
   Parameters:
   - real: Real part of the constant value
   - imag: Imaginary part of the constant value
   - dims: Vector of dimensions [d0 d1 d2 d3]
   - dtype: ArrayFire complex dtype constant (default: AF_DTYPE_C32)
   
   Returns:
   AFArray filled with the complex constant value"
  ([real imag dims]
   (constant-complex real imag dims defs/AF_DTYPE_C32))
  ([real imag dims dtype]
   (let [out (jvm/native-af-array-pointer)
         ndims (count dims)
         dims-seg (bmem/dims->segment dims)]
     (check! (data/af-constant-complex out (double real) (double imag) ndims dims-seg (int dtype))
                 "af-constant-complex")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn constant-long
  "Generate an array with long long elements set to a specified value.
   
   Parameters:
   - value: The constant long value
   - dims: Vector of dimensions [d0 d1 d2 d3]
   
   Returns:
   AFArray filled with the long constant value"
  [value dims]
  (let [out (jvm/native-af-array-pointer)
        ndims (count dims)
        dims-seg (bmem/dims->segment dims)]
    (check! (data/af-constant-long out (long value) ndims dims-seg)
                "af-constant-long")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn constant-ulong
  "Generate an array with unsigned long long elements set to a specified value.
   
   Parameters:
   - value: The constant unsigned long value
   - dims: Vector of dimensions [d0 d1 d2 d3]
   
   Returns:
   AFArray filled with the unsigned long constant value"
  [value dims]
  (let [out (jvm/native-af-array-pointer)
        ndims (count dims)
        dims-seg (bmem/dims->segment dims)]
    (check! (data/af-constant-ulong out (long value) ndims dims-seg)
                "af-constant-ulong")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn identity
  "Generate an identity array.
   
   Parameters:
   - dims: Vector of dimensions [d0 d1 d2 d3]
   - dtype: ArrayFire dtype constant (default: AF_DTYPE_F32)
   
   Returns:
   AFArray identity matrix/array"
  ([dims]
   (identity dims defs/AF_DTYPE_F32))
  ([dims dtype]
   (let [out (jvm/native-af-array-pointer)
         ndims (count dims)
         dims-seg (bmem/dims->segment dims)]
     (check! (data/af-identity out ndims dims-seg (int dtype))
                 "af-identity")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn range
  "Generate an array with [0, n-1] values along seq_dim and tiled across other dimensions.
   
   Parameters:
   - dims: Vector of dimensions [d0 d1 d2 d3]
   - seq-dim: Dimension along which to create the sequence
   - dtype: ArrayFire dtype constant (default: AF_DTYPE_F32)
   
   Returns:
   AFArray with sequential values along specified dimension"
  ([dims seq-dim]
   (range dims seq-dim defs/AF_DTYPE_F32))
  ([dims seq-dim dtype]
   (let [out (jvm/native-af-array-pointer)
         ndims (count dims)
         dims-seg (bmem/dims->segment dims)]
     (check! (data/af-range out ndims dims-seg (int seq-dim) (int dtype))
                 "af-range")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn iota
  "Generate an array with [0, n-1] values modified to specified dimensions and tiling.
   
   Parameters:
   - dims: Vector of dimensions [d0 d1 d2 d3]
   - tdims: Vector of tiling dimensions [t0 t1 t2 t3]
   - dtype: ArrayFire dtype constant (default: AF_DTYPE_F32)
   
   Returns:
   AFArray with sequential values modified by tiling"
  ([dims tdims]
   (iota dims tdims defs/AF_DTYPE_F32))
  ([dims tdims dtype]
   (let [out (jvm/native-af-array-pointer)
         ndims (count dims)
         dims-seg (bmem/dims->segment dims)
         tndims (count tdims)
         tdims-seg (bmem/dims->segment tdims)]
     (check! (data/af-iota out ndims dims-seg tndims tdims-seg (int dtype))
                 "af-iota")
     (jvm/af-array-new (jvm/deref-af-array out)))))

;;;
;;; Diagonal Operations
;;;

(defn diag-create
  "Create a diagonal matrix from an array.
   
   Parameters:
   - in: Input array (AFArray)
   - num: Diagonal number (0=main, positive=upper, negative=lower), default 0
   
   Returns:
   AFArray diagonal matrix"
  ([^AFArray in]
   (diag-create in 0))
  ([^AFArray in num]
   (let [out (jvm/native-af-array-pointer)]
     (check! (data/af-diag-create out (jvm/af-handle in) (int num))
                 "af-diag-create")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn diag-extract
  "Extract the diagonal from an array.
   
   Parameters:
   - in: Input array (AFArray)
   - num: Diagonal number (0=main, positive=upper, negative=lower), default 0
   
   Returns:
   AFArray containing the extracted diagonal"
  ([^AFArray in]
   (diag-extract in 0))
  ([^AFArray in num]
   (let [out (jvm/native-af-array-pointer)]
     (check! (data/af-diag-extract out (jvm/af-handle in) (int num))
                 "af-diag-extract")
     (jvm/af-array-new (jvm/deref-af-array out)))))

;;;
;;; Array Manipulation Functions
;;;

(defn join
  "Join 2 arrays along a dimension.
   
   Parameters:
   - dim: Dimension along which to join (0-3)
   - first: First array (AFArray)
   - second: Second array (AFArray)
   
   Returns:
   AFArray result of joining"
  [dim ^AFArray first ^AFArray second]
  (let [out (jvm/native-af-array-pointer)]
    (check! (data/af-join out (int dim) (jvm/af-handle first) (jvm/af-handle second))
                "af-join")
    (jvm/af-array-new (jvm/deref-af-array out))))

; TODO use mem/pointer-size, if it works in arithmetic
(def pointer-size 8)

(defn join-many
  "Join many arrays along a dimension (up to 10).
   
   Parameters:
   - dim: Dimension along which to join (0-3)
   - arrays: Vector of AFArray instances to join
   
   Returns:
   AFArray result of joining all arrays"
  [dim arrays]
  (let [out (jvm/native-af-array-pointer)
        n (count arrays)
        handles-buf (mem/alloc (* n pointer-size))]
    (doseq [[i arr] (map-indexed vector arrays)]
      (mem/write-long handles-buf
                      (* i pointer-size)
                      (jvm/af-handle-value arr)))
    (check! (data/af-join-many out (int dim) n handles-buf)
                "af-join-many")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn tile
  "Generate a tiled array by repeating input along dimensions.
   
   Parameters:
   - in: Input array (AFArray)
   - x: Number of repetitions along dimension 0 (default 1)
   - y: Number of repetitions along dimension 1 (default 1)
   - z: Number of repetitions along dimension 2 (default 1)
   - w: Number of repetitions along dimension 3 (default 1)
   
   Returns:
   AFArray tiled result"
  ([^AFArray in x]
   (tile in x 1 1 1))
  ([^AFArray in x y]
   (tile in x y 1 1))
  ([^AFArray in x y z]
   (tile in x y z 1))
  ([^AFArray in x y z w]
   (let [out (jvm/native-af-array-pointer)]
     (check! (data/af-tile out (jvm/af-handle in) (int x) (int y) (int z) (int w))
                 "af-tile")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn reorder
  "Reorder an array by changing dimension order.
   
   Parameters:
   - in: Input array (AFArray)
   - x: New position for dimension 0 (default 0)
   - y: New position for dimension 1 (default 1)
   - z: New position for dimension 2 (default 2)
   - w: New position for dimension 3 (default 3)
   
   Returns:
   AFArray with reordered dimensions"
  ([^AFArray in x]
   (reorder in x 1 2 3))
  ([^AFArray in x y]
   (reorder in x y 2 3))
  ([^AFArray in x y z]
   (reorder in x y z 3))
  ([^AFArray in x y z w]
   (let [out (jvm/native-af-array-pointer)]
     (check! (data/af-reorder out (jvm/af-handle in) (int x) (int y) (int z) (int w))
                 "af-reorder")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn shift
  "Shift an array along dimensions.
   
   Parameters:
   - in: Input array (AFArray)
   - x: Shift amount for dimension 0 (default 0)
   - y: Shift amount for dimension 1 (default 0)
   - z: Shift amount for dimension 2 (default 0)
   - w: Shift amount for dimension 3 (default 0)
   
   Returns:
   AFArray with shifted elements"
  ([^AFArray in x]
   (shift in x 0 0 0))
  ([^AFArray in x y]
   (shift in x y 0 0))
  ([^AFArray in x y z]
   (shift in x y z 0))
  ([^AFArray in x y z w]
   (let [out (jvm/native-af-array-pointer)]
     (check! (data/af-shift out (jvm/af-handle in) (int x) (int y) (int z) (int w))
                 "af-shift")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn moddims
  "Modify the dimensions of an array to a specified shape.
   
   Parameters:
   - in: Input array (AFArray)
   - dims: Vector of new dimensions [d0 d1 d2 d3]
   
   Returns:
   AFArray with modified dimensions (same data, different shape)"
  [^AFArray in dims]
  (let [out (jvm/native-af-array-pointer)
        ndims (count dims)
        dims-seg (bmem/dims->segment dims)]
    (check! (data/af-moddims out (jvm/af-handle in) ndims dims-seg)
                "af-moddims")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn flat
  "Flatten an array to one dimension.
   
   Parameters:
   - in: Input array (AFArray)
   
   Returns:
   AFArray flattened to 1D"
  [^AFArray in]
  (let [out (jvm/native-af-array-pointer)]
    (check! (data/af-flat out (jvm/af-handle in))
                "af-flat")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn flip
  "Flip an array along a dimension.
   
   Parameters:
   - in: Input array (AFArray)
   - dim: Dimension along which to flip (0-3)
   
   Returns:
   AFArray with flipped elements"
  [^AFArray in dim]
  (let [out (jvm/native-af-array-pointer)]
    (check! (data/af-flip out (jvm/af-handle in) (int dim))
                "af-flip")
    (jvm/af-array-new (jvm/deref-af-array out))))

;;;
;;; Triangle Operations
;;;

(defn lower
  "Return the lower triangle array.
   
   Parameters:
   - in: Input array (AFArray)
   - is-unit-diag: Whether to make diagonal unity (boolean, default false)
   
   Returns:
   AFArray with upper triangle zeroed"
  ([^AFArray in]
   (lower in false))
  ([^AFArray in is-unit-diag]
   (let [out (jvm/native-af-array-pointer)]
     (check! (data/af-lower out (jvm/af-handle in) (if is-unit-diag 1 0))
                 "af-lower")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn upper
  "Return the upper triangle array.
   
   Parameters:
   - in: Input array (AFArray)
   - is-unit-diag: Whether to make diagonal unity (boolean, default false)
   
   Returns:
   AFArray with lower triangle zeroed"
  ([^AFArray in]
   (upper in false))
  ([^AFArray in is-unit-diag]
   (let [out (jvm/native-af-array-pointer)]
     (check! (data/af-upper out (jvm/af-handle in) (if is-unit-diag 1 0))
                 "af-upper")
     (jvm/af-array-new (jvm/deref-af-array out)))))

;;;
;;; Conditional Operations
;;;

(defn select
  "Select elements based on a conditional array.
   
   Parameters:
   - cond: Conditional array (AFArray) - where true, select from 'a', else from 'b'
   - a: First array (AFArray)
   - b: Second array (AFArray)
   
   Returns:
   AFArray with elements selected based on condition"
  [^AFArray cond ^AFArray a ^AFArray b]
  (let [out (jvm/native-af-array-pointer)]
    (check! (data/af-select out (jvm/af-handle cond) (jvm/af-handle a) (jvm/af-handle b))
                "af-select")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn select-scalar-r
  "Select between array and scalar (scalar on right).
   
   Parameters:
   - cond: Conditional array (AFArray) - where true, select from 'a', else use scalar 'b'
   - a: Array (AFArray)
   - b: Scalar value (as double)
   
   Returns:
   AFArray with elements selected based on condition"
  [^AFArray cond ^AFArray a b]
  (let [out (jvm/native-af-array-pointer)]
    (check! (data/af-select-scalar-r out (jvm/af-handle cond) (jvm/af-handle a) (double b))
                "af-select-scalar-r")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn select-scalar-l
  "Select between scalar and array (scalar on left).
   
   Parameters:
   - cond: Conditional array (AFArray) - where true, use scalar 'a', else select from 'b'
   - a: Scalar value (as double)
   - b: Array (AFArray)
   
   Returns:
   AFArray with elements selected based on condition"
  [^AFArray cond a ^AFArray b]
  (let [out (jvm/native-af-array-pointer)]
    (check! (data/af-select-scalar-l out (jvm/af-handle cond) (double a) (jvm/af-handle b))
                "af-select-scalar-l")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn replace!
  "Replace elements in array based on condition (modifies input).
   
   Parameters:
   - a: Array to modify (AFArray)
   - cond: Conditional array (AFArray) - where true, replace with elements from 'b'
   - b: Source array (AFArray)
   
   Returns:
   The modified array 'a' (for chaining)"
  [^AFArray a ^AFArray cond ^AFArray b]
  (check! (data/af-replace (jvm/af-handle a) (jvm/af-handle cond) (jvm/af-handle b))
              "af-replace")
  a)

(defn replace-scalar!
  "Replace elements in array with scalar based on condition (modifies input).
   
   Parameters:
   - a: Array to modify (AFArray)
   - cond: Conditional array (AFArray) - where true, replace with scalar 'b'
   - b: Scalar value (as double)
   
   Returns:
   The modified array 'a' (for chaining)"
  [^AFArray a ^AFArray cond b]
  (check! (data/af-replace-scalar (jvm/af-handle a) (jvm/af-handle cond) (double b))
              "af-replace-scalar")
  a)

(defn pad
  "Pad an array with specified border type.
   
   Parameters:
   - in: Input array (AFArray)
   - begin-dims: Vector of padding at beginning [b0 b1 b2 b3]
   - end-dims: Vector of padding at end [e0 e1 e2 e3]
   - border-type: Border padding type (default 0=AF_PAD_ZERO)
                  0=AF_PAD_ZERO, 1=AF_PAD_SYM, 2=AF_PAD_CLAMP_TO_EDGE, 3=AF_PAD_PERIODIC
   
   Returns:
   AFArray with padded borders"
  ([^AFArray in begin-dims end-dims]
   (pad in begin-dims end-dims 0))
  ([^AFArray in begin-dims end-dims border-type]
   (let [out (jvm/native-af-array-pointer)
         b-ndims (count begin-dims)
         b-dims-seg (bmem/dims->segment begin-dims)
         e-ndims (count end-dims)
         e-dims-seg (bmem/dims->segment end-dims)]
     (check! (data/af-pad out (jvm/af-handle in) b-ndims b-dims-seg e-ndims e-dims-seg (int border-type))
                 "af-pad")
     (jvm/af-array-new (jvm/deref-af-array out)))))