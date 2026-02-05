(ns org.soulspace.arrayfire.integration.data-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [org.soulspace.arrayfire.integration.data :as data]
            [org.soulspace.arrayfire.integration.array :as array]
            [org.soulspace.arrayfire.integration.device :as device]
            [org.soulspace.arrayfire.integration.jvm-integration :as jvm])
  (:import [org.soulspace.arrayfire.integration.jvm_integration AFArray]))

;;;
;;; Data Generation Functions Tests
;;;

(deftest test-constant
  (testing "constant creates array with constant value"
    (device/init!)
    (let [arr (data/constant 5.0 [3 3])]
      (is (instance? AFArray arr))
      (is (= [3 3 1 1] (array/get-dims arr)))
      (is (= 9 (array/get-elements arr)))
      (.close arr))))

(deftest test-constant-with-dtype
  (testing "constant creates array with specified dtype"
    (device/init!)
    (let [f32-arr (data/constant 2.5 [2 2] jvm/AF_DTYPE_F32)
          f64-arr (data/constant 2.5 [2 2] jvm/AF_DTYPE_F64)]
      (is (instance? AFArray f32-arr))
      (is (instance? AFArray f64-arr))
      (is (= jvm/AF_DTYPE_F32 (array/get-type f32-arr)))
      (is (= jvm/AF_DTYPE_F64 (array/get-type f64-arr)))
      (.close f64-arr)
      (.close f32-arr))))

(deftest test-constant-complex
  (testing "constant-complex creates complex array"
    (device/init!)
    (let [arr (data/constant-complex 3.0 4.0 [2 2])]
      (is (instance? AFArray arr))
      (is (= [2 2 1 1] (array/get-dims arr)))
      (is (array/complex? arr))
      (.close arr))))

(deftest test-constant-complex-with-dtype
  (testing "constant-complex creates array with specified complex dtype"
    (device/init!)
    (let [c32-arr (data/constant-complex 1.0 2.0 [2 2] jvm/AF_DTYPE_C32)
          c64-arr (data/constant-complex 1.0 2.0 [2 2] jvm/AF_DTYPE_C64)]
      (is (instance? AFArray c32-arr))
      (is (instance? AFArray c64-arr))
      (is (= jvm/AF_DTYPE_C32 (array/get-type c32-arr)))
      (is (= jvm/AF_DTYPE_C64 (array/get-type c64-arr)))
      (.close c64-arr)
      (.close c32-arr))))

(deftest test-constant-long
  (testing "constant-long creates long array"
    (device/init!)
    (let [arr (data/constant-long 42 [3 3])]
      (is (instance? AFArray arr))
      (is (= [3 3 1 1] (array/get-dims arr)))
      (.close arr))))

(deftest test-constant-ulong
  (testing "constant-ulong creates unsigned long array"
    (device/init!)
    (let [arr (data/constant-ulong 100 [2 3])]
      (is (instance? AFArray arr))
      (is (= [2 3 1 1] (array/get-dims arr)))
      (.close arr))))

(deftest test-identity
  (testing "identity creates identity matrix"
    (device/init!)
    (let [arr (data/identity [4 4])]
      (is (instance? AFArray arr))
      (is (= [4 4 1 1] (array/get-dims arr)))
      (.close arr))))

(deftest test-identity-with-dtype
  (testing "identity creates identity matrix with specified dtype"
    (device/init!)
    (let [f32-arr (data/identity [3 3] jvm/AF_DTYPE_F32)
          f64-arr (data/identity [3 3] jvm/AF_DTYPE_F64)]
      (is (instance? AFArray f32-arr))
      (is (instance? AFArray f64-arr))
      (is (= jvm/AF_DTYPE_F32 (array/get-type f32-arr)))
      (is (= jvm/AF_DTYPE_F64 (array/get-type f64-arr)))
      (.close f64-arr)
      (.close f32-arr))))

(deftest test-range
  (testing "range creates array with sequential values"
    (device/init!)
    (let [arr (data/range [5] 0)]
      (is (instance? AFArray arr))
      (is (= [5 1 1 1] (array/get-dims arr)))
      (.close arr))))

(deftest test-range-with-dimension
  (testing "range creates array along specified dimension"
    (device/init!)
    (let [arr (data/range [3 4] 1)]
      (is (instance? AFArray arr))
      (is (= [3 4 1 1] (array/get-dims arr)))
      (.close arr))))

(deftest test-range-with-dtype
  (testing "range creates array with specified dtype"
    (device/init!)
    (let [arr (data/range [5] 0 jvm/AF_DTYPE_F64)]
      (is (instance? AFArray arr))
      (is (= jvm/AF_DTYPE_F64 (array/get-type arr)))
      (.close arr))))

(deftest test-iota
  (testing "iota creates array with sequential values and tiling"
    (device/init!)
    (let [arr (data/iota [3 4] [1 1])]
      (is (instance? AFArray arr))
      (is (= [3 4 1 1] (array/get-dims arr)))
      (.close arr))))

(deftest test-iota-with-dtype
  (testing "iota creates array with specified dtype"
    (device/init!)
    (let [arr (data/iota [3 4] [1 1] jvm/AF_DTYPE_F32)]
      (is (instance? AFArray arr))
      (is (= jvm/AF_DTYPE_F32 (array/get-type arr)))
      (.close arr))))

;;;
;;; Diagonal Operations Tests
;;;

(deftest test-diag-create
  (testing "diag-create creates diagonal matrix from vector"
    (device/init!)
    (let [vec (array/create-array (float-array [1.0 2.0 3.0]) [3] jvm/AF_DTYPE_F32)
          diag-arr (data/diag-create vec)]
      (is (instance? AFArray diag-arr))
      (is (= [3 3 1 1] (array/get-dims diag-arr)))
      (.close diag-arr)
      (.close vec))))

(deftest test-diag-create-with-offset
  (testing "diag-create creates diagonal matrix with offset"
    (device/init!)
    (let [vec (array/create-array (float-array [1.0 2.0]) [2] jvm/AF_DTYPE_F32)
          diag-arr (data/diag-create vec 1)]
      (is (instance? AFArray diag-arr))
      (is (= [3 3 1 1] (array/get-dims diag-arr)))
      (.close diag-arr)
      (.close vec))))

(deftest test-diag-extract
  (testing "diag-extract extracts diagonal from matrix"
    (device/init!)
    (let [mat (data/identity [4 4])
          diag-vec (data/diag-extract mat)]
      (is (instance? AFArray diag-vec))
      (is (= [4 1 1 1] (array/get-dims diag-vec)))
      (.close diag-vec)
      (.close mat))))

(deftest test-diag-extract-with-offset
  (testing "diag-extract extracts diagonal with offset"
    (device/init!)
    (let [mat (data/identity [4 4])
          diag-vec (data/diag-extract mat 1)]
      (is (instance? AFArray diag-vec))
      (is (= [3 1 1 1] (array/get-dims diag-vec)))
      (.close diag-vec)
      (.close mat))))

;;;
;;; Array Manipulation Functions Tests
;;;

(deftest test-join
  (testing "join concatenates two arrays along dimension"
    (device/init!)
    (let [a (data/constant 1.0 [2 3])
          b (data/constant 2.0 [2 3])
          result (data/join 0 a b)]
      (is (instance? AFArray result))
      (is (= [4 3 1 1] (array/get-dims result)))
      (.close result)
      (.close b)
      (.close a))))

(deftest test-join-along-dim1
  (testing "join concatenates along dimension 1"
    (device/init!)
    (let [a (data/constant 1.0 [2 3])
          b (data/constant 2.0 [2 2])
          result (data/join 1 a b)]
      (is (instance? AFArray result))
      (is (= [2 5 1 1] (array/get-dims result)))
      (.close result)
      (.close b)
      (.close a))))

(deftest test-join-many
  (testing "join-many concatenates multiple arrays"
    (device/init!)
    (let [a (data/constant 1.0 [2 2])
          b (data/constant 2.0 [2 2])
          c (data/constant 3.0 [2 2])
          result (data/join-many 0 [a b c])]
      (is (instance? AFArray result))
      (is (= [6 2 1 1] (array/get-dims result)))
      (.close result)
      (.close c)
      (.close b)
      (.close a))))

(deftest test-tile
  (testing "tile replicates array along dimensions"
    (device/init!)
    (let [arr (data/constant 5.0 [2 2])
          tiled (data/tile arr 2 3)]
      (is (instance? AFArray tiled))
      (is (= [4 6 1 1] (array/get-dims tiled)))
      (.close tiled)
      (.close arr))))

(deftest test-tile-all-dimensions
  (testing "tile replicates along all dimensions"
    (device/init!)
    (let [arr (data/constant 1.0 [2 2])
          tiled (data/tile arr 2 2 2 2)]
      (is (instance? AFArray tiled))
      (is (= [4 4 2 2] (array/get-dims tiled)))
      (.close tiled)
      (.close arr))))

(deftest test-reorder
  (testing "reorder changes dimension order"
    (device/init!)
    (let [arr (data/range [2 3 4] 0)
          reordered (data/reorder arr 1 0 2)]
      (is (instance? AFArray reordered))
      (is (= [3 2 4 1] (array/get-dims reordered)))
      (.close reordered)
      (.close arr))))

(deftest test-reorder-transpose
  (testing "reorder can perform transpose"
    (device/init!)
    (let [arr (data/constant 1.0 [3 4])
          transposed (data/reorder arr 1 0)]
      (is (instance? AFArray transposed))
      (is (= [4 3 1 1] (array/get-dims transposed)))
      (.close transposed)
      (.close arr))))

(deftest test-shift
  (testing "shift moves array elements along dimensions"
    (device/init!)
    (let [arr (data/range [5] 0)
          shifted (data/shift arr 2)]
      (is (instance? AFArray shifted))
      (is (= [5 1 1 1] (array/get-dims shifted)))
      (.close shifted)
      (.close arr))))

(deftest test-shift-multiple-dimensions
  (testing "shift works along multiple dimensions"
    (device/init!)
    (let [arr (data/range [3 4] 0)
          shifted (data/shift arr 1 2)]
      (is (instance? AFArray shifted))
      (is (= [3 4 1 1] (array/get-dims shifted)))
      (.close shifted)
      (.close arr))))

(deftest test-moddims
  (testing "moddims reshapes array"
    (device/init!)
    (let [arr (data/range [12] 0)
          reshaped (data/moddims arr [3 4])]
      (is (instance? AFArray reshaped))
      (is (= [3 4 1 1] (array/get-dims reshaped)))
      (is (= 12 (array/get-elements reshaped)))
      (.close reshaped)
      (.close arr))))

(deftest test-moddims-preserves-elements
  (testing "moddims preserves total element count"
    (device/init!)
    (let [arr (data/constant 5.0 [2 3 4])
          reshaped (data/moddims arr [6 4])]
      (is (instance? AFArray reshaped))
      (is (= [6 4 1 1] (array/get-dims reshaped)))
      (is (= 24 (array/get-elements reshaped)))
      (.close reshaped)
      (.close arr))))

(deftest test-flat
  (testing "flat flattens array to 1D"
    (device/init!)
    (let [arr (data/constant 1.0 [2 3 4])
          flattened (data/flat arr)]
      (is (instance? AFArray flattened))
      (is (= [24 1 1 1] (array/get-dims flattened)))
      (is (= 24 (array/get-elements flattened)))
      (.close flattened)
      (.close arr))))

(deftest test-flip
  (testing "flip reverses array along dimension"
    (device/init!)
    (let [arr (data/range [5] 0)
          flipped (data/flip arr 0)]
      (is (instance? AFArray flipped))
      (is (= [5 1 1 1] (array/get-dims flipped)))
      (.close flipped)
      (.close arr))))

(deftest test-flip-matrix
  (testing "flip reverses matrix rows or columns"
    (device/init!)
    (let [arr (data/range [3 4] 0)
          flipped-rows (data/flip arr 0)
          flipped-cols (data/flip arr 1)]
      (is (instance? AFArray flipped-rows))
      (is (instance? AFArray flipped-cols))
      (is (= [3 4 1 1] (array/get-dims flipped-rows)))
      (is (= [3 4 1 1] (array/get-dims flipped-cols)))
      (.close flipped-cols)
      (.close flipped-rows)
      (.close arr))))

;;;
;;; Triangle Operations Tests
;;;

(deftest test-lower
  (testing "lower extracts lower triangle"
    (device/init!)
    (let [arr (data/constant 1.0 [4 4])
          lower-tri (data/lower arr)]
      (is (instance? AFArray lower-tri))
      (is (= [4 4 1 1] (array/get-dims lower-tri)))
      (.close lower-tri)
      (.close arr))))

(deftest test-lower-unit-diag
  (testing "lower with unit diagonal"
    (device/init!)
    (let [arr (data/constant 2.0 [3 3])
          lower-tri (data/lower arr true)]
      (is (instance? AFArray lower-tri))
      (is (= [3 3 1 1] (array/get-dims lower-tri)))
      (.close lower-tri)
      (.close arr))))

(deftest test-upper
  (testing "upper extracts upper triangle"
    (device/init!)
    (let [arr (data/constant 1.0 [4 4])
          upper-tri (data/upper arr)]
      (is (instance? AFArray upper-tri))
      (is (= [4 4 1 1] (array/get-dims upper-tri)))
      (.close upper-tri)
      (.close arr))))

(deftest test-upper-unit-diag
  (testing "upper with unit diagonal"
    (device/init!)
    (let [arr (data/constant 2.0 [3 3])
          upper-tri (data/upper arr true)]
      (is (instance? AFArray upper-tri))
      (is (= [3 3 1 1] (array/get-dims upper-tri)))
      (.close upper-tri)
      (.close arr))))

;;;
;;; Conditional Operations Tests
;;;

(deftest test-select
  (testing "select chooses elements based on condition"
    (device/init!)
    (let [cond (array/create-array (byte-array [1 0 1 0]) [4] jvm/AF_DTYPE_B8)
          a (data/constant 1.0 [4])
          b (data/constant 2.0 [4])
          result (data/select cond a b)]
      (is (instance? AFArray result))
      (is (= [4 1 1 1] (array/get-dims result)))
      (.close result)
      (.close b)
      (.close a)
      (.close cond))))

(deftest test-select-scalar-r
  (testing "select-scalar-r chooses between array and scalar"
    (device/init!)
    (let [cond (array/create-array (byte-array [1 0 1 0]) [4] jvm/AF_DTYPE_B8)
          a (data/constant 1.0 [4])
          result (data/select-scalar-r cond a 5.0)]
      (is (instance? AFArray result))
      (is (= [4 1 1 1] (array/get-dims result)))
      (.close result)
      (.close a)
      (.close cond))))

(deftest test-select-scalar-l
  (testing "select-scalar-l chooses between scalar and array"
    (device/init!)
    (let [cond (array/create-array (byte-array [1 0 1 0]) [4] jvm/AF_DTYPE_B8)
          b (data/constant 2.0 [4])
          result (data/select-scalar-l cond 5.0 b)]
      (is (instance? AFArray result))
      (is (= [4 1 1 1] (array/get-dims result)))
      (.close result)
      (.close b)
      (.close cond))))

(deftest test-replace
  (testing "replace! modifies array in place"
    (device/init!)
    (let [a (data/constant 1.0 [4])
          cond (array/create-array (byte-array [0 1 0 1]) [4] jvm/AF_DTYPE_B8)
          b (data/constant 5.0 [4])
          result (data/replace! a cond b)]
      (is (identical? result a))
      (.close b)
      (.close cond)
      (.close a))))

(deftest test-replace-scalar
  (testing "replace-scalar! replaces with scalar value"
    (device/init!)
    (let [a (data/constant 1.0 [4])
          cond (array/create-array (byte-array [0 1 0 1]) [4] jvm/AF_DTYPE_B8)
          result (data/replace-scalar! a cond 10.0)]
      (is (identical? result a))
      (.close cond)
      (.close a))))

(deftest test-pad
  (testing "pad adds borders to array"
    (device/init!)
    (let [arr (data/constant 1.0 [3 3])
          padded (data/pad arr [1 1] [1 1])]
      (is (instance? AFArray padded))
      (is (= [5 5 1 1] (array/get-dims padded)))
      (.close padded)
      (.close arr))))

(deftest test-pad-with-border-type
  (testing "pad with specified border type"
    (device/init!)
    (let [arr (data/constant 1.0 [3 3])
          padded (data/pad arr [2 2] [2 2] 0)]
      (is (instance? AFArray padded))
      (is (= [7 7 1 1] (array/get-dims padded)))
      (.close padded)
      (.close arr))))

(comment
  ;; run tests from REPL
  (run-tests)
  ;
  )
