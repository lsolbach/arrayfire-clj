(ns org.soulspace.arrayfire.integration.complex-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [org.soulspace.arrayfire.integration.complex :as complex]
            [org.soulspace.arrayfire.integration.array :as array]
            [org.soulspace.arrayfire.integration.device :as device]
            [org.soulspace.arrayfire.integration.jvm-integration :as jvm]
            [coffi.mem :as mem]))

(defn- approx=
  "Compare expected/actual values within a tolerance."
  [expected actual tolerance]
  (<= (Math/abs (- (double expected) (double actual)))
      (double tolerance)))

;;;
;;; Complex Number Creation Tests
;;;

(deftest test-cplx
  (testing "cplx creates complex array from real array"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0 3.0]) [3] jvm/AF_DTYPE_F32)
          b (complex/cplx a)
          buf (mem/alloc (* 3 8))]  ; 3 elements * 8 bytes (2 floats)
      (array/get-data-ptr b buf)
      ;; Check real parts
      (is (approx= 1.0 (mem/read-float buf 0) 0.001))
      (is (approx= 2.0 (mem/read-float buf 8) 0.001))
      (is (approx= 3.0 (mem/read-float buf 16) 0.001))
      ;; Check imaginary parts are zero
      (is (approx= 0.0 (mem/read-float buf 4) 0.001))
      (is (approx= 0.0 (mem/read-float buf 12) 0.001))
      (is (approx= 0.0 (mem/read-float buf 20) 0.001))
      (.close a)
      (.close b))))

(deftest test-cplx2
  (testing "cplx2 creates complex array from real and imaginary arrays"
    (device/init!)
    (let [real-part (array/create-array (float-array [1.0 2.0 3.0]) [3] jvm/AF_DTYPE_F32)
          imag-part (array/create-array (float-array [4.0 5.0 6.0]) [3] jvm/AF_DTYPE_F32)
          c (complex/cplx2 real-part imag-part)
          buf (mem/alloc (* 3 8))]
      (array/get-data-ptr c buf)
      ;; Check real parts
      (is (approx= 1.0 (mem/read-float buf 0) 0.001))
      (is (approx= 2.0 (mem/read-float buf 8) 0.001))
      (is (approx= 3.0 (mem/read-float buf 16) 0.001))
      ;; Check imaginary parts
      (is (approx= 4.0 (mem/read-float buf 4) 0.001))
      (is (approx= 5.0 (mem/read-float buf 12) 0.001))
      (is (approx= 6.0 (mem/read-float buf 20) 0.001))
      (.close real-part)
      (.close imag-part)
      (.close c))))

(deftest test-cplx2-batch
  (testing "cplx2 with batch mode"
    (device/init!)
    (let [real-part (array/create-array (float-array [1.0 2.0]) [2] jvm/AF_DTYPE_F32)
          imag-part (array/create-array (float-array [3.0 4.0]) [2] jvm/AF_DTYPE_F32)
          c (complex/cplx2 real-part imag-part true)
          buf (mem/alloc (* 2 8))]
      (array/get-data-ptr c buf)
      ;; Check real parts
      (is (approx= 1.0 (mem/read-float buf 0) 0.001))
      (is (approx= 2.0 (mem/read-float buf 8) 0.001))
      ;; Check imaginary parts
      (is (approx= 3.0 (mem/read-float buf 4) 0.001))
      (is (approx= 4.0 (mem/read-float buf 12) 0.001))
      (.close real-part)
      (.close imag-part)
      (.close c))))

;;;
;;; Complex Component Extraction Tests
;;;

(deftest test-real
  (testing "real extracts real part from complex array"
    (device/init!)
    (let [real-part (array/create-array (float-array [1.0 2.0 3.0]) [3] jvm/AF_DTYPE_F32)
          imag-part (array/create-array (float-array [4.0 5.0 6.0]) [3] jvm/AF_DTYPE_F32)
          c (complex/cplx2 real-part imag-part)
          r (complex/real c)
          buf (mem/alloc (* 3 4))]
      (array/get-data-ptr r buf)
      (is (approx= 1.0 (mem/read-float buf 0) 0.001))
      (is (approx= 2.0 (mem/read-float buf 4) 0.001))
      (is (approx= 3.0 (mem/read-float buf 8) 0.001))
      (.close real-part)
      (.close imag-part)
      (.close c)
      (.close r))))

(deftest test-imag
  (testing "imag extracts imaginary part from complex array"
    (device/init!)
    (let [real-part (array/create-array (float-array [1.0 2.0 3.0]) [3] jvm/AF_DTYPE_F32)
          imag-part (array/create-array (float-array [4.0 5.0 6.0]) [3] jvm/AF_DTYPE_F32)
          c (complex/cplx2 real-part imag-part)
          im (complex/imag c)
          buf (mem/alloc (* 3 4))]
      (array/get-data-ptr im buf)
      (is (approx= 4.0 (mem/read-float buf 0) 0.001))
      (is (approx= 5.0 (mem/read-float buf 4) 0.001))
      (is (approx= 6.0 (mem/read-float buf 8) 0.001))
      (.close real-part)
      (.close imag-part)
      (.close c)
      (.close im))))

;;;
;;; Complex Operations Tests
;;;

(deftest test-conjg
  (testing "conjg computes complex conjugate"
    (device/init!)
    (let [real-part (array/create-array (float-array [1.0 2.0]) [2] jvm/AF_DTYPE_F32)
          imag-part (array/create-array (float-array [3.0 4.0]) [2] jvm/AF_DTYPE_F32)
          c (complex/cplx2 real-part imag-part)
          conj-c (complex/conjg c)
          buf (mem/alloc (* 2 8))]
      (array/get-data-ptr conj-c buf)
      ;; Real parts should remain the same
      (is (approx= 1.0 (mem/read-float buf 0) 0.001))
      (is (approx= 2.0 (mem/read-float buf 8) 0.001))
      ;; Imaginary parts should be negated
      (is (approx= -3.0 (mem/read-float buf 4) 0.001))
      (is (approx= -4.0 (mem/read-float buf 12) 0.001))
      (.close real-part)
      (.close imag-part)
      (.close c)
      (.close conj-c))))

(deftest test-abs
  (testing "abs computes magnitude of complex numbers"
    (device/init!)
    (let [real-part (array/create-array (float-array [3.0 4.0]) [2] jvm/AF_DTYPE_F32)
          imag-part (array/create-array (float-array [4.0 3.0]) [2] jvm/AF_DTYPE_F32)
          c (complex/cplx2 real-part imag-part)
          mag (complex/abs c)
          buf (mem/alloc (* 2 4))]
      (array/get-data-ptr mag buf)
      ;; |3+4i| = sqrt(9+16) = 5
      (is (approx= 5.0 (mem/read-float buf 0) 0.001))
      ;; |4+3i| = sqrt(16+9) = 5
      (is (approx= 5.0 (mem/read-float buf 4) 0.001))
      (.close real-part)
      (.close imag-part)
      (.close c)
      (.close mag))))

(deftest test-arg
  (testing "arg computes phase angle of complex numbers"
    (device/init!)
    (let [real-part (array/create-array (float-array [1.0 0.0 -1.0]) [3] jvm/AF_DTYPE_F32)
          imag-part (array/create-array (float-array [0.0 1.0 0.0]) [3] jvm/AF_DTYPE_F32)
          c (complex/cplx2 real-part imag-part)
          phase (complex/arg c)
          buf (mem/alloc (* 3 4))]
      (array/get-data-ptr phase buf)
      ;; arg(1+0i) = 0
      (is (approx= 0.0 (mem/read-float buf 0) 0.001))
      ;; arg(0+1i) = π/2 ≈ 1.5708
      (is (approx= (/ Math/PI 2) (mem/read-float buf 4) 0.001))
      ;; arg(-1+0i) = π ≈ 3.1416
      (is (approx= Math/PI (mem/read-float buf 8) 0.001))
      (.close real-part)
      (.close imag-part)
      (.close c)
      (.close phase))))

;;;
;;; Edge Cases and Integration Tests
;;;

(deftest test-real-on-real-array
  (testing "real on real array returns retained reference"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0 3.0]) [3] jvm/AF_DTYPE_F32)
          r (complex/real a)
          buf (mem/alloc (* 3 4))]
      (array/get-data-ptr r buf)
      (is (approx= 1.0 (mem/read-float buf 0) 0.001))
      (is (approx= 2.0 (mem/read-float buf 4) 0.001))
      (is (approx= 3.0 (mem/read-float buf 8) 0.001))
      (.close a)
      (.close r))))

(deftest test-imag-on-real-array
  (testing "imag on real array returns zeros"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0 3.0]) [3] jvm/AF_DTYPE_F32)
          im (complex/imag a)
          buf (mem/alloc (* 3 4))]
      (array/get-data-ptr im buf)
      (is (approx= 0.0 (mem/read-float buf 0) 0.001))
      (is (approx= 0.0 (mem/read-float buf 4) 0.001))
      (is (approx= 0.0 (mem/read-float buf 8) 0.001))
      (.close a)
      (.close im))))

(deftest test-conjg-on-real-array
  (testing "conjg on real array returns retained reference"
    (device/init!)
    (let [a (array/create-array (float-array [1.0 2.0 3.0]) [3] jvm/AF_DTYPE_F32)
          c (complex/conjg a)
          buf (mem/alloc (* 3 4))]
      (array/get-data-ptr c buf)
      (is (approx= 1.0 (mem/read-float buf 0) 0.001))
      (is (approx= 2.0 (mem/read-float buf 4) 0.001))
      (is (approx= 3.0 (mem/read-float buf 8) 0.001))
      (.close a)
      (.close c))))

;;;
;;; Arithmetic Operator Tests
;;;

(deftest test-add
  (testing "add performs complex addition"
    (device/init!)
    (let [real1 (array/create-array (float-array [1.0 2.0]) [2] jvm/AF_DTYPE_F32)
          imag1 (array/create-array (float-array [3.0 4.0]) [2] jvm/AF_DTYPE_F32)
          real2 (array/create-array (float-array [5.0 6.0]) [2] jvm/AF_DTYPE_F32)
          imag2 (array/create-array (float-array [7.0 8.0]) [2] jvm/AF_DTYPE_F32)
          z1 (complex/cplx2 real1 imag1)
          z2 (complex/cplx2 real2 imag2)
          result (complex/add z1 z2)
          buf (mem/alloc (* 2 8))]
      (array/get-data-ptr result buf)
      ;; (1+3i)+(5+7i) = 6+10i
      (is (approx= 6.0 (mem/read-float buf 0) 0.001))
      (is (approx= 10.0 (mem/read-float buf 4) 0.001))
      ;; (2+4i)+(6+8i) = 8+12i
      (is (approx= 8.0 (mem/read-float buf 8) 0.001))
      (is (approx= 12.0 (mem/read-float buf 12) 0.001))
      (.close real1)
      (.close imag1)
      (.close real2)
      (.close imag2)
      (.close z1)
      (.close z2)
      (.close result))))

(deftest test-sub
  (testing "sub performs complex subtraction"
    (device/init!)
    (let [real1 (array/create-array (float-array [5.0 6.0]) [2] jvm/AF_DTYPE_F32)
          imag1 (array/create-array (float-array [7.0 8.0]) [2] jvm/AF_DTYPE_F32)
          real2 (array/create-array (float-array [1.0 2.0]) [2] jvm/AF_DTYPE_F32)
          imag2 (array/create-array (float-array [3.0 4.0]) [2] jvm/AF_DTYPE_F32)
          z1 (complex/cplx2 real1 imag1)
          z2 (complex/cplx2 real2 imag2)
          result (complex/sub z1 z2)
          buf (mem/alloc (* 2 8))]
      (array/get-data-ptr result buf)
      ;; (5+7i)-(1+3i) = 4+4i
      (is (approx= 4.0 (mem/read-float buf 0) 0.001))
      (is (approx= 4.0 (mem/read-float buf 4) 0.001))
      ;; (6+8i)-(2+4i) = 4+4i
      (is (approx= 4.0 (mem/read-float buf 8) 0.001))
      (is (approx= 4.0 (mem/read-float buf 12) 0.001))
      (.close real1)
      (.close imag1)
      (.close real2)
      (.close imag2)
      (.close z1)
      (.close z2)
      (.close result))))

(deftest test-mul
  (testing "mul performs complex multiplication"
    (device/init!)
    (let [real1 (array/create-array (float-array [1.0 2.0]) [2] jvm/AF_DTYPE_F32)
          imag1 (array/create-array (float-array [2.0 3.0]) [2] jvm/AF_DTYPE_F32)
          real2 (array/create-array (float-array [3.0 4.0]) [2] jvm/AF_DTYPE_F32)
          imag2 (array/create-array (float-array [4.0 5.0]) [2] jvm/AF_DTYPE_F32)
          z1 (complex/cplx2 real1 imag1)
          z2 (complex/cplx2 real2 imag2)
          result (complex/mul z1 z2)
          buf (mem/alloc (* 2 8))]
      (array/get-data-ptr result buf)
      ;; (1+2i)*(3+4i) = (3-8)+(4+6)i = -5+10i
      (is (approx= -5.0 (mem/read-float buf 0) 0.001))
      (is (approx= 10.0 (mem/read-float buf 4) 0.001))
      ;; (2+3i)*(4+5i) = (8-15)+(10+12)i = -7+22i
      (is (approx= -7.0 (mem/read-float buf 8) 0.001))
      (is (approx= 22.0 (mem/read-float buf 12) 0.001))
      (.close real1)
      (.close imag1)
      (.close real2)
      (.close imag2)
      (.close z1)
      (.close z2)
      (.close result))))

(deftest test-div
  (testing "div performs complex division"
    (device/init!)
    (let [real1 (array/create-array (float-array [1.0]) [1] jvm/AF_DTYPE_F32)
          imag1 (array/create-array (float-array [2.0]) [1] jvm/AF_DTYPE_F32)
          real2 (array/create-array (float-array [3.0]) [1] jvm/AF_DTYPE_F32)
          imag2 (array/create-array (float-array [4.0]) [1] jvm/AF_DTYPE_F32)
          z1 (complex/cplx2 real1 imag1)
          z2 (complex/cplx2 real2 imag2)
          result (complex/div z1 z2)
          buf (mem/alloc 8)]
      (array/get-data-ptr result buf)
      ;; (1+2i)/(3+4i) = (1+2i)*(3-4i)/(3²+4²) = (3+8+i(6-4))/25 = 11/25 + 2i/25 = 0.44 + 0.08i
      (is (approx= 0.44 (mem/read-float buf 0) 0.001))
      (is (approx= 0.08 (mem/read-float buf 4) 0.001))
      (.close real1)
      (.close imag1)
      (.close real2)
      (.close imag2)
      (.close z1)
      (.close z2)
      (.close result))))

;;;
;;; Comparison Operator Tests
;;;

(deftest test-eq
  (testing "eq performs complex equality comparison"
    (device/init!)
    (let [real1 (array/create-array (float-array [1.0 2.0 3.0]) [3] jvm/AF_DTYPE_F32)
          imag1 (array/create-array (float-array [3.0 4.0 5.0]) [3] jvm/AF_DTYPE_F32)
          real2 (array/create-array (float-array [1.0 5.0 3.0]) [3] jvm/AF_DTYPE_F32)
          imag2 (array/create-array (float-array [3.0 6.0 5.0]) [3] jvm/AF_DTYPE_F32)
          z1 (complex/cplx2 real1 imag1)
          z2 (complex/cplx2 real2 imag2)
          result (complex/eq z1 z2)
          buf (mem/alloc (* 3 1))]
      (array/get-data-ptr result buf)
      ;; (1+3i)==(1+3i) -> true (1)
      (is (not= 0 (mem/read-byte buf 0)))
      ;; (2+4i)!=(5+6i) -> false (0)
      (is (= 0 (mem/read-byte buf 1)))
      ;; (3+5i)==(3+5i) -> true (1)
      (is (not= 0 (mem/read-byte buf 2)))
      (.close real1)
      (.close imag1)
      (.close real2)
      (.close imag2)
      (.close z1)
      (.close z2)
      (.close result))))

(deftest test-neq
  (testing "neq performs complex inequality comparison"
    (device/init!)
    (let [real1 (array/create-array (float-array [1.0 2.0 3.0]) [3] jvm/AF_DTYPE_F32)
          imag1 (array/create-array (float-array [3.0 4.0 5.0]) [3] jvm/AF_DTYPE_F32)
          real2 (array/create-array (float-array [1.0 5.0 3.0]) [3] jvm/AF_DTYPE_F32)
          imag2 (array/create-array (float-array [3.0 6.0 5.0]) [3] jvm/AF_DTYPE_F32)
          z1 (complex/cplx2 real1 imag1)
          z2 (complex/cplx2 real2 imag2)
          result (complex/neq z1 z2)
          buf (mem/alloc (* 3 1))]
      (array/get-data-ptr result buf)
      ;; (1+3i)!=(1+3i) -> false (0)
      (is (= 0 (mem/read-byte buf 0)))
      ;; (2+4i)!=(5+6i) -> true (1)
      (is (not= 0 (mem/read-byte buf 1)))
      ;; (3+5i)!=(3+5i) -> false (0)
      (is (= 0 (mem/read-byte buf 2)))
      (.close real1)
      (.close imag1)
      (.close real2)
      (.close imag2)
      (.close z1)
      (.close z2)
      (.close result))))

;;;
;;; Run all tests
;;;

(defn run-all-tests []
  (run-tests 'org.soulspace.arrayfire.integration.complex-test))
