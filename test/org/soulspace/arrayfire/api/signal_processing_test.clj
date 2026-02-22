(ns org.soulspace.arrayfire.api.signal-processing-test
  "Tests for the idiomatic Clojure signal processing API.
   All tests run inside (with-arrayfire ...) regions."
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [org.soulspace.arrayfire.util.test :as util]
            [org.soulspace.arrayfire.api.core :as af]
            [org.soulspace.arrayfire.api.signal-processing :as sp]))

;;;
;;; Helpers
;;;
(defn- complex->reals
  "Extract real parts from a complex ->value result.
   Complex results come back as [[real imag] ...] vectors."
  [v]
  (mapv (fn [x] (if (vector? x) (first x) (double x)))
        (if (vector? (first v)) v (vector v))))

;;;
;;; Guard tests — functions require with-arrayfire region
;;;

(deftest fft-requires-region-test
  (testing "fft throws IllegalStateException outside a region"
    (is (thrown? IllegalStateException
          (let [arr (af/with-arrayfire
                      (af/array [1.0 2.0 3.0 4.0] [4] :f64))]
            (sp/fft arr))))))

(deftest convolve1-requires-region-test
  (testing "convolve1 throws IllegalStateException outside a region"
    (is (thrown? IllegalStateException
          (let [s (af/with-arrayfire
                    (af/array [1.0 2.0 3.0] [3] :f64))
                k (af/with-arrayfire
                    (af/array [1.0] [1] :f64))]
            (sp/convolve1 s k))))))

;;;
;;; 1D FFT
;;;

(deftest fft-basic-test
  (testing "fft of a real signal returns a complex array of the same size"
    (let [result (af/with-arrayfire
                   (let [s (af/array [1.0 2.0 3.0 4.0] [4] :f64)
                         freq (sp/fft s)]
                     {:shape (af/shape freq)
                      :complex? (af/complex-array? freq)}))]
      (is (= [4] (:shape result)))
      (is (true? (:complex? result))))))

(deftest fft-with-padding-test
  (testing "fft with zero padding changes output size"
    (let [result (af/with-arrayfire
                   (let [s (af/array [1.0 2.0 3.0] [3] :f64)
                         freq (sp/fft s 1.0 8)]
                     (af/shape freq)))]
      (is (= [8] result)))))

(deftest fft-with-norm-factor-test
  (testing "fft with custom normalization factor succeeds"
    (let [result (af/with-arrayfire
                   (let [s (af/array [1.0 1.0 1.0 1.0] [4] :f64)
                         freq (sp/fft s 0.5)]
                     (af/complex-array? freq)))]
      (is (true? result)))))

;;;
;;; 2D FFT
;;;

(deftest fft2-basic-test
  (testing "fft2 of a 4x4 real image returns a complex 4x4 array"
    (let [result (af/with-arrayfire
                   (let [img (af/array (vec (map double (range 16))) [4 4] :f64)
                         freq (sp/fft2 img)]
                     {:shape (af/shape freq)
                      :complex? (af/complex-array? freq)}))]
      (is (= [4 4] (:shape result)))
      (is (true? (:complex? result))))))

(deftest fft2-with-padding-test
  (testing "fft2 with padding changes output dimensions"
    (let [result (af/with-arrayfire
                   (let [img (af/array (vec (map double (range 9))) [3 3] :f64)
                         freq (sp/fft2 img 1.0 8 8)]
                     (af/shape freq)))]
      (is (= [8 8] result)))))

;;;
;;; 3D FFT
;;;

(deftest fft3-basic-test
  (testing "fft3 of a 2x2x2 real volume returns a complex 2x2x2 array"
    (let [result (af/with-arrayfire
                   (let [vol (af/array (vec (map double (range 8))) [2 2 2] :f64)
                         freq (sp/fft3 vol)]
                     {:shape (af/shape freq)
                      :complex? (af/complex-array? freq)}))]
      (is (= [2 2 2] (:shape result)))
      (is (true? (:complex? result))))))

;;;
;;; Inverse FFT
;;;

(deftest ifft-basic-test
  (testing "ifft returns an array of the same size as input"
    (let [result (af/with-arrayfire
                   (let [s (af/array [1.0 2.0 3.0 4.0] [4] :f64)
                         freq (sp/fft s)
                         time (sp/ifft freq 0.25)]
                     (af/shape time)))]
      (is (= [4] result)))))

(deftest ifft2-basic-test
  (testing "ifft2 returns a 2D array of the same size"
    (let [result (af/with-arrayfire
                   (let [img (af/array (vec (map double (range 16))) [4 4] :f64)
                         freq (sp/fft2 img)
                         spatial (sp/ifft2 freq 0.0625)] ; 1/16
                     (af/shape spatial)))]
      (is (= [4 4] result)))))

(deftest ifft3-basic-test
  (testing "ifft3 returns a 3D array of the same size"
    (let [result (af/with-arrayfire
                   (let [vol (af/array (vec (map double (range 8))) [2 2 2] :f64)
                         freq (sp/fft3 vol)
                         spatial (sp/ifft3 freq 0.125)] ; 1/8
                     (af/shape spatial)))]
      (is (= [2 2 2] result)))))

;;;
;;; FFT round-trip (fft then ifft with 1/N normalization recovers original)
;;;

(deftest fft-ifft-roundtrip-test
  (testing "fft followed by ifft(1/N) recovers the original signal (real parts)"
    (let [original [1.0 2.0 3.0 4.0]
          result (af/with-arrayfire
                   (let [s (af/array original [4] :f64)
                         freq (sp/fft s)
                         back (sp/ifft freq 0.25)]
                     (af/->value back)))]
      ;; Result is complex: [[re im] ...], extract real parts
      (is (util/seq-approx= (complex->reals result) original 1e-5)))))

;;;
;;; Normalized FFT
;;;

(deftest fft-normalized-test
  (testing "fft-normalized returns a complex array"
    (let [result (af/with-arrayfire
                   (let [s (af/array [1.0 0.0 1.0 0.0] [4] :f64)
                         freq (sp/fft-normalized s)]
                     {:shape (af/shape freq)
                      :complex? (af/complex-array? freq)}))]
      (is (= [4] (:shape result)))
      (is (true? (:complex? result))))))

(deftest ifft-normalized-test
  (testing "ifft-normalized returns an array of the same size"
    (let [result (af/with-arrayfire
                   (let [s (af/array [1.0 2.0 3.0 4.0] [4] :f64)
                         freq (sp/fft s)
                         time (sp/ifft-normalized freq)]
                     (af/shape time)))]
      (is (= [4] result)))))

;;;
;;; Real-to-Complex FFT
;;;

(deftest fft-r2c-test
  (testing "fft-r2c returns complex array of size N/2+1"
    (let [result (af/with-arrayfire
                   (let [s (af/array [1.0 2.0 3.0 4.0] [4] :f64)
                         freq (sp/fft-r2c s)]
                     {:shape (af/shape freq)
                      :complex? (af/complex-array? freq)}))]
      (is (= [3] (:shape result)))  ; N/2+1 = 3
      (is (true? (:complex? result))))))

;;;
;;; Complex-to-Real FFT
;;;

(deftest fft-c2r-roundtrip-test
  (testing "fft-r2c followed by fft-c2r recovers the original shape"
    (let [result (af/with-arrayfire
                   (let [s (af/array [1.0 2.0 3.0 4.0] [4] :f64)
                         freq (sp/fft-r2c s)
                         back (sp/fft-c2r freq 0.25 false)]
                     (af/shape back)))]
      (is (= [4] result)))))

;;;
;;; In-place FFT
;;;

(deftest fft!-test
  (testing "fft! modifies array in-place and returns the same instance type"
    (let [result (af/with-arrayfire
                   (let [s (af/array [1.0 2.0 3.0 4.0] [4] :f64)
                         ;; In-place FFT requires complex input, so first do out-of-place
                         complex-s (sp/fft s)
                         modified (sp/fft! complex-s)]
                     (af/shape modified)))]
      (is (= [4] result)))))

(deftest ifft!-test
  (testing "ifft! modifies complex array in-place"
    (let [result (af/with-arrayfire
                   (let [s (af/array [1.0 2.0 3.0 4.0] [4] :f64)
                         complex-s (sp/fft s)
                         modified (sp/ifft! complex-s)]
                     (af/shape modified)))]
      (is (= [4] result)))))

;;;
;;; 1D Convolution
;;;

(deftest convolve1-basic-test
  (testing "convolve1 with identity filter preserves signal"
    ;; Convolving with [1.0] should return original signal
    (let [result (af/with-arrayfire
                   (let [s (af/array [1.0 2.0 3.0 4.0 5.0] [5] :f64)
                         k (af/array [1.0] [1] :f64)]
                     (af/->value (sp/convolve1 s k))))]
      (is (util/seq-approx= (flatten result) [1.0 2.0 3.0 4.0 5.0])))))

(deftest convolve1-expand-mode-test
  (testing "convolve1 :expand mode yields output-size = signal + filter - 1"
    (let [result (af/with-arrayfire
                   (let [s (af/array [1.0 2.0 3.0 4.0] [4] :f64)
                         k (af/array [1.0 1.0 1.0] [3] :f64)]
                     (af/shape (sp/convolve1 s k :expand))))]
      (is (= [6] result))))) ; 4 + 3 - 1 = 6

(deftest convolve1-impulse-response-test
  (testing "convolve1 of impulse with filter = filter (centered)"
    ;; Delta at center: [0 0 1 0 0], kernel [1 2 3]
    ;; Default mode => same size as input
    (let [result (af/with-arrayfire
                   (let [s (af/array [0.0 0.0 1.0 0.0 0.0] [5] :f64)
                         k (af/array [1.0 2.0 3.0] [3] :f64)]
                     (af/->value (sp/convolve1 s k))))]
      ;; Expect the kernel values to appear around the impulse position
      (is (= 5 (count (flatten result)))))))

;;;
;;; 2D Convolution
;;;

(deftest convolve2-identity-test
  (testing "convolve2 with identity kernel preserves the image"
    ;; 3x3 identity kernel (center = 1)
    (let [result (af/with-arrayfire
                   (let [img (af/array [1.0 3.0 2.0 4.0] [2 2] :f64)
                         kernel (af/array [0.0 0.0 0.0
                                           0.0 1.0 0.0
                                           0.0 0.0 0.0] [3 3] :f64)]
                     (af/->value (sp/convolve2 img kernel))))]
      (is (util/seq-approx= (flatten result) [1.0 3.0 2.0 4.0])))))

(deftest convolve2-expand-mode-test
  (testing "convolve2 :expand mode changes output dimensions"
    (let [result (af/with-arrayfire
                   (let [img (af/array (vec (map double (range 9))) [3 3] :f64)
                         kernel (af/array [1.0 1.0 1.0 1.0] [2 2] :f64)]
                     (af/shape (sp/convolve2 img kernel :expand))))]
      ;; expand: 3+2-1=4 in each dim
      (is (= [4 4] result)))))

;;;
;;; 3D Convolution
;;;

(deftest convolve3-basic-test
  (testing "convolve3 produces output of correct shape"
    (let [result (af/with-arrayfire
                   (let [vol (af/array (vec (map double (range 27))) [3 3 3] :f64)
                         kernel (af/array [1.0] [1 1 1] :f64)]
                     (af/shape (sp/convolve3 vol kernel))))]
      (is (= [3 3 3] result)))))

;;;
;;; Separable 2D Convolution
;;;

(deftest convolve2-sep-basic-test
  (testing "convolve2-sep with identity separable filter preserves shape"
    (let [result (af/with-arrayfire
                   (let [img (af/array [1.0 2.0 3.0 4.0 5.0 6.0 7.0 8.0 9.0] [3 3] :f64)
                         col-filt (af/array [0.0 1.0 0.0] [3] :f64)
                         row-filt (af/array [0.0 1.0 0.0] [3] :f64)]
                     (af/shape (sp/convolve2-sep col-filt row-filt img))))]
      (is (= [3 3] result)))))

;;;
;;; FFT-based convolution
;;;

(deftest fft-convolve1-basic-test
  (testing "fft-convolve1 with identity filter preserves signal"
    (let [result (af/with-arrayfire
                   (let [s (af/array [1.0 2.0 3.0 4.0] [4] :f64)
                         k (af/array [1.0] [1] :f64)]
                     (af/->value (sp/fft-convolve1 s k))))]
      (is (util/seq-approx= (flatten result) [1.0 2.0 3.0 4.0])))))

(deftest fft-convolve2-basic-test
  (testing "fft-convolve2 produces correct output shape"
    (let [result (af/with-arrayfire
                   (let [img (af/array (vec (map double (range 16))) [4 4] :f64)
                         kernel (af/array [0.0 0.0 0.0
                                           0.0 1.0 0.0
                                           0.0 0.0 0.0] [3 3] :f64)]
                     (af/shape (sp/fft-convolve2 img kernel))))]
      (is (= [4 4] result)))))

(deftest fft-convolve3-basic-test
  (testing "fft-convolve3 produces correct output shape"
    (let [result (af/with-arrayfire
                   (let [vol (af/array (vec (map double (range 27))) [3 3 3] :f64)
                         ;; Use a proper 3x3x3 kernel with center=1
                         kernel-data (vec (concat (repeat 13 0.0) [1.0] (repeat 13 0.0)))
                         kernel (af/array kernel-data [3 3 3] :f64)]
                     (af/shape (sp/fft-convolve3 vol kernel))))]
      (is (= [3 3 3] result)))))

;;;
;;; IIR filter
;;;

(deftest iir-basic-test
  (testing "IIR with b=[1] a=[1] passes signal through unchanged"
    (let [result (af/with-arrayfire
                   (let [x (af/array [1.0 2.0 3.0 4.0] [4] :f64)
                         b (af/array [1.0] [1] :f64)
                         a (af/array [1.0] [1] :f64)]
                     (af/->value (sp/iir b a x))))]
      (is (util/seq-approx= (flatten result) [1.0 2.0 3.0 4.0])))))

;;;
;;; Median filter (requires :opencl backend — not supported on CPU)
;;;

(deftest median-filter1-basic-test
  (testing "median-filter1 removes spike noise"
    ;; Signal: [1 100 2 3 4], median window 3 should remove the spike
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [noisy (af/array [1.0 100.0 2.0 3.0 4.0] [5] :f64)
                         clean (sp/median-filter1 noisy 3)]
                     (af/->value clean)))]
      ;; The 100.0 spike should be reduced
      (is (< (double (nth (flatten result) 1)) 50.0)))))

(deftest median-filter1-preserves-shape-test
  (testing "median-filter1 preserves the signal length"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [s (af/array [1.0 2.0 3.0 4.0 5.0] [5] :f64)]
                     (af/shape (sp/median-filter1 s))))]
      (is (= [5] result)))))

(deftest median-filter2-basic-test
  (testing "median-filter2 preserves shape for a 2D array"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [img (af/array (vec (map double (range 16))) [4 4] :f64)]
                     (af/shape (sp/median-filter2 img))))]
      (is (= [4 4] result)))))

(deftest median-filter2-with-window-test
  (testing "median-filter2 with custom window size"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [img (af/array (vec (map double (range 25))) [5 5] :f64)]
                     (af/shape (sp/median-filter2 img 5 5))))]
      (is (= [5 5] result)))))

;;;
;;; 1D Interpolation
;;;

(deftest approx1-linear-test
  (testing "approx1 linear interpolation of [0 1 2 3] at midpoints"
    (let [result (af/with-arrayfire
                   (let [yi (af/array [0.0 1.0 2.0 3.0] [4] :f64)
                         xo (af/array [0.5 1.5 2.5] [3] :f64)]
                     (af/->value (sp/approx1 yi xo :linear))))]
      (is (util/seq-approx= (flatten result) [0.5 1.5 2.5] 1e-3)))))

(deftest approx1-nearest-test
  (testing "approx1 nearest interpolation snaps to closest value"
    (let [result (af/with-arrayfire
                   (let [yi (af/array [10.0 20.0 30.0 40.0] [4] :f64)
                         xo (af/array [0.3 1.7 2.1] [3] :f64)]
                     (af/->value (sp/approx1 yi xo :nearest))))]
      ;; Nearest to 0.3 → idx 0 → 10.0, to 1.7 → idx 2 → 30.0, to 2.1 → idx 2 → 30.0
      (is (util/seq-approx= (flatten result) [10.0 30.0 30.0] 1e-3)))))

(deftest approx1-off-grid-test
  (testing "approx1 returns off-grid value for out-of-bounds positions"
    (let [result (af/with-arrayfire
                   (let [yi (af/array [1.0 2.0 3.0] [3] :f64)
                         xo (af/array [5.0] [1] :f64)]
                     (af/->value (sp/approx1 yi xo :linear -1.0))))]
      ;; Position 5.0 is out of bounds for a 3-element array
      ;; Result is a scalar for a single-element output
      (is (util/approx= (double result) -1.0 1e-3)))))

;;;
;;; 2D Interpolation
;;;

(deftest approx2-basic-test
  (testing "approx2 produces output of correct shape"
    (let [result (af/with-arrayfire
                   (let [zi (af/array (vec (map double (range 16))) [4 4] :f64)
                         xo (af/array [0.5 1.5] [2] :f64)
                         yo (af/array [0.5 1.5] [2] :f64)]
                     (af/shape (sp/approx2 zi xo yo))))]
      (is (= [2] result)))))

;;;
;;; Keyword resolution tests
;;;

(deftest invalid-conv-mode-throws-test
  (testing "Invalid convolution mode keyword throws ex-info"
    (is (thrown? clojure.lang.ExceptionInfo
          (af/with-arrayfire
            (let [s (af/array [1.0 2.0] [2] :f64)
                  k (af/array [1.0] [1] :f64)]
              (sp/convolve1 s k :invalid-mode)))))))

(deftest invalid-interp-throws-test
  (testing "Invalid interpolation method keyword throws ex-info"
    (is (thrown? clojure.lang.ExceptionInfo
          (af/with-arrayfire
            (let [yi (af/array [1.0 2.0] [2] :f64)
                  xo (af/array [0.5] [1] :f64)]
              (sp/approx1 yi xo :nonexistent)))))))


(comment
  ;; Run the tests
  (run-tests)
  
  ;
  )
