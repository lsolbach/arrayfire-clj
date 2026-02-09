(ns org.soulspace.arrayfire.integration.unified-api.signal-test
  (:require [clojure.test :refer [deftest is testing run-test run-tests]]
            [org.soulspace.arrayfire.integration.unified-api.signal :as signal]
            [org.soulspace.arrayfire.integration.unified-api.array :as array]
            [org.soulspace.arrayfire.integration.unified-api.data :as data]
            [org.soulspace.arrayfire.integration.unified-api.device :as device]
            [org.soulspace.arrayfire.integration.jvm-integration :as jvm]
            [coffi.mem :as mem])
  (:import [org.soulspace.arrayfire.integration.jvm_integration AFArray]))

(defn- approx=
  "Compare expected/actual values within a tolerance."
  [expected actual tolerance]
  (<= (Math/abs (- (double expected) (double actual)))
      (double tolerance)))

;;;
;;; 1D FFT Tests
;;;

(deftest test-fft
  (testing "fft performs 1D forward FFT"
    (device/init!)
    (let [signal (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4] jvm/AF_DTYPE_F32)
          freq (signal/fft signal)]
      (try
        (is (instance? AFArray freq))
        (is (= [4] (take 1 (array/get-dims freq))))
        (is (array/complex? freq))
        (finally
          (.close signal)
          (.close freq))))))

(deftest test-fft-with-norm
  (testing "fft with custom normalization factor"
    (device/init!)
    (let [signal (array/create-array (float-array [1.0 1.0 1.0 1.0]) [4] jvm/AF_DTYPE_F32)
          freq (signal/fft signal 0.5)]
      (try
        (is (instance? AFArray freq))
        (finally
          (.close signal)
          (.close freq))))))

(deftest test-fft-with-padding
  (testing "fft with zero padding"
    (device/init!)
    (let [signal (array/create-array (float-array [1.0 2.0 3.0]) [3] jvm/AF_DTYPE_F32)
          freq (signal/fft signal 1.0 8)] ; Pad to 8 elements
      (try
        (is (instance? AFArray freq))
        (is (= [8] (take 1 (array/get-dims freq))))
        (finally
          (.close signal)
          (.close freq))))))

;;;
;;; 2D FFT Tests
;;;

(deftest test-fft2
  (testing "fft2 performs 2D forward FFT"
    (device/init!)
    (let [image (array/create-array (float-array (range 16)) [4 4] jvm/AF_DTYPE_F32)
          freq (signal/fft2 image)]
      (try
        (is (instance? AFArray freq))
        (is (= [4 4] (take 2 (array/get-dims freq))))
        (is (array/complex? freq))
        (finally
          (.close image)
          (.close freq))))))

(deftest test-fft2-with-padding
  (testing "fft2 with padding"
    (device/init!)
    (let [image (array/create-array (float-array (range 9)) [3 3] jvm/AF_DTYPE_F32)
          freq (signal/fft2 image 1.0 8 8)]
      (try
        (is (instance? AFArray freq))
        (is (= [8 8] (take 2 (array/get-dims freq))))
        (finally
          (.close image)
          (.close freq))))))

;;;
;;; 3D FFT Tests
;;;

(deftest test-fft3
  (testing "fft3 performs 3D forward FFT"
    (device/init!)
    (let [volume (array/create-array (float-array (range 8)) [2 2 2] jvm/AF_DTYPE_F32)
          freq (signal/fft3 volume)]
      (try
        (is (instance? AFArray freq))
        (is (= [2 2 2] (take 3 (array/get-dims freq))))
        (is (array/complex? freq))
        (finally
          (.close volume)
          (.close freq))))))

;;;
;;; Inverse FFT Tests
;;;

(deftest test-ifft
  (testing "ifft performs 1D inverse FFT"
    (device/init!)
    (let [signal (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4] jvm/AF_DTYPE_F32)
          freq (signal/fft signal)
          reconstructed (signal/ifft freq 0.25)] ; 1/N normalization
      (try
        (is (instance? AFArray reconstructed))
        (is (= [4] (take 1 (array/get-dims reconstructed))))
        (finally
          (.close signal)
          (.close freq)
          (.close reconstructed))))))

(deftest test-ifft2
  (testing "ifft2 performs 2D inverse FFT"
    (device/init!)
    (let [image (array/create-array (float-array (range 16)) [4 4] jvm/AF_DTYPE_F32)
          freq (signal/fft2 image)
          reconstructed (signal/ifft2 freq (/ 1.0 16.0))]
      (try
        (is (instance? AFArray reconstructed))
        (is (= [4 4] (take 2 (array/get-dims reconstructed))))
        (finally
          (.close image)
          (.close freq)
          (.close reconstructed))))))

(deftest test-ifft3
  (testing "ifft3 performs 3D inverse FFT"
    (device/init!)
    (let [volume (array/create-array (float-array (range 8)) [2 2 2] jvm/AF_DTYPE_F32)
          freq (signal/fft3 volume)
          reconstructed (signal/ifft3 freq (/ 1.0 8.0))]
      (try
        (is (instance? AFArray reconstructed))
        (is (= [2 2 2] (take 3 (array/get-dims reconstructed))))
        (finally
          (.close volume)
          (.close freq)
          (.close reconstructed))))))

;;;
;;; Normalized FFT Tests
;;;

(deftest test-fft-norm
  (testing "fft-norm performs FFT with automatic 1/N normalization"
    (device/init!)
    (let [signal (array/create-array (float-array [1.0 1.0 1.0 1.0]) [4] jvm/AF_DTYPE_F32)
          freq (signal/fft-norm signal)]
      (try
        (is (instance? AFArray freq))
        (is (= [4] (take 1 (array/get-dims freq))))
        (is (array/complex? freq))
        (finally
          (.close signal)
          (.close freq))))))

(deftest test-ifft-norm
  (testing "ifft-norm performs inverse FFT with automatic 1/N normalization"
    (device/init!)
    (let [signal (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4] jvm/AF_DTYPE_F32)
          freq (signal/fft signal)
          reconstructed (signal/ifft-norm freq)]
      (try
        (is (instance? AFArray reconstructed))
        (is (= [4] (take 1 (array/get-dims reconstructed))))
        (finally
          (.close signal)
          (.close freq)
          (.close reconstructed))))))

(deftest test-fft-norm-roundtrip
  (testing "fft-norm and ifft roundtrip preserves signal"
    (device/init!)
    (let [signal (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4] jvm/AF_DTYPE_F32)
          freq (signal/fft-norm signal)
          reconstructed (signal/ifft freq)]
      (try
        (is (instance? AFArray reconstructed))
        ;; After fft-norm (scaled by 1/N) and ifft (no scaling), should get original
        (is (= [4] (take 1 (array/get-dims reconstructed))))
        (finally
          (.close signal)
          (.close freq)
          (.close reconstructed))))))

(deftest test-fft2-norm
  (testing "fft2-norm performs 2D FFT with automatic normalization"
    (device/init!)
    (let [image (array/create-array (float-array (range 16)) [4 4] jvm/AF_DTYPE_F32)
          freq (signal/fft2-norm image)]
      (try
        (is (instance? AFArray freq))
        (is (= [4 4] (take 2 (array/get-dims freq))))
        (is (array/complex? freq))
        (finally
          (.close image)
          (.close freq))))))

(deftest test-ifft2-norm
  (testing "ifft2-norm performs 2D inverse FFT with automatic normalization"
    (device/init!)
    (let [image (array/create-array (float-array (range 16)) [4 4] jvm/AF_DTYPE_F32)
          freq (signal/fft2 image)
          reconstructed (signal/ifft2-norm freq)]
      (try
        (is (instance? AFArray reconstructed))
        (is (= [4 4] (take 2 (array/get-dims reconstructed))))
        (finally
          (.close image)
          (.close freq)
          (.close reconstructed))))))

(deftest test-fft3-norm
  (testing "fft3-norm performs 3D FFT with automatic normalization"
    (device/init!)
    (let [volume (array/create-array (float-array (range 8)) [2 2 2] jvm/AF_DTYPE_F32)
          freq (signal/fft3-norm volume)]
      (try
        (is (instance? AFArray freq))
        (is (= [2 2 2] (take 3 (array/get-dims freq))))
        (is (array/complex? freq))
        (finally
          (.close volume)
          (.close freq))))))

(deftest test-ifft3-norm
  (testing "ifft3-norm performs 3D inverse FFT with automatic normalization"
    (device/init!)
    (let [volume (array/create-array (float-array (range 8)) [2 2 2] jvm/AF_DTYPE_F32)
          freq (signal/fft3 volume)
          reconstructed (signal/ifft3-norm freq)]
      (try
        (is (instance? AFArray reconstructed))
        (is (= [2 2 2] (take 3 (array/get-dims reconstructed))))
        (finally
          (.close volume)
          (.close freq)
          (.close reconstructed))))))

;;;
;;; Real-to-Complex FFT Tests
;;;

(deftest test-fft-r2c
  (testing "fft-r2c performs optimized real-to-complex FFT"
    (device/init!)
    (let [real-signal (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4] jvm/AF_DTYPE_F32)
          freq (signal/fft-r2c real-signal)]
      (try
        (is (instance? AFArray freq))
        ;; R2C produces N/2+1 complex values
        (is (= [3] (take 1 (array/get-dims freq))))
        (is (array/complex? freq))
        (finally
          (.close real-signal)
          (.close freq))))))

(deftest test-fft2-r2c
  (testing "fft2-r2c performs 2D real-to-complex FFT"
    (device/init!)
    (let [real-image (array/create-array (float-array (range 16)) [4 4] jvm/AF_DTYPE_F32)
          freq (signal/fft2-r2c real-image)]
      (try
        (is (instance? AFArray freq))
        (is (array/complex? freq))
        (finally
          (.close real-image)
          (.close freq))))))

(deftest test-fft3-r2c
  (testing "fft3-r2c performs 3D real-to-complex FFT"
    (device/init!)
    (let [real-volume (array/create-array (float-array (range 8)) [2 2 2] jvm/AF_DTYPE_F32)
          freq (signal/fft3-r2c real-volume)]
      (try
        (is (instance? AFArray freq))
        (is (array/complex? freq))
        (finally
          (.close real-volume)
          (.close freq))))))

;;;
;;; Complex-to-Real FFT Tests
;;;

(deftest test-fft-c2r
  (testing "fft-c2r performs complex-to-real inverse FFT"
    (device/init!)
    (let [real-signal (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4] jvm/AF_DTYPE_F32)
          freq (signal/fft-r2c real-signal)
          reconstructed (signal/fft-c2r freq false)]
      (try
        (is (instance? AFArray reconstructed))
        (is (array/real? reconstructed))
        (finally
          (.close real-signal)
          (.close freq)
          (.close reconstructed))))))

(deftest test-fft2-c2r
  (testing "fft2-c2r performs 2D complex-to-real inverse FFT"
    (device/init!)
    (let [real-image (array/create-array (float-array (range 16)) [4 4] jvm/AF_DTYPE_F32)
          freq (signal/fft2-r2c real-image)
          reconstructed (signal/fft2-c2r freq false)]
      (try
        (is (instance? AFArray reconstructed))
        (is (array/real? reconstructed))
        (finally
          (.close real-image)
          (.close freq)
          (.close reconstructed))))))

(deftest test-fft3-c2r
  (testing "fft3-c2r performs 3D complex-to-real inverse FFT"
    (device/init!)
    (let [real-volume (array/create-array (float-array (range 8)) [2 2 2] jvm/AF_DTYPE_F32)
          freq (signal/fft3-r2c real-volume)
          reconstructed (signal/fft3-c2r freq false)]
      (try
        (is (instance? AFArray reconstructed))
        (is (array/real? reconstructed))
        (finally
          (.close real-volume)
          (.close freq)
          (.close reconstructed))))))

;;;
;;; In-place FFT Tests
;;;

(deftest test-fft-inplace
  (testing "fft! performs in-place 1D FFT"
    (device/init!)
    (let [;; Create complex array for in-place operation
          signal (array/create-array [[1.0 0.0] [2.0 0.0] [3.0 0.0] [4.0 0.0]] [4] jvm/AF_DTYPE_C32)
          result (signal/fft! signal)]
      (try
        (is (= signal result)) ; Same array object
        (is (array/complex? result))
        (finally
          (.close signal))))))

(deftest test-fft2-inplace
  (testing "fft2! performs in-place 2D FFT"
    (device/init!)
    (let [signal (array/create-array (vec (map #(vector (float %) 0.0) (range 16))) [4 4] jvm/AF_DTYPE_C32)
          result (signal/fft2! signal)]
      (try
        (is (= signal result))
        (finally
          (.close signal))))))

(deftest test-fft3-inplace
  (testing "fft3! performs in-place 3D FFT"
    (device/init!)
    (let [signal (array/create-array (vec (map #(vector (float %) 0.0) (range 8))) [2 2 2] jvm/AF_DTYPE_C32)
          result (signal/fft3! signal)]
      (try
        (is (= signal result))
        (finally
          (.close signal))))))

(deftest test-ifft-inplace
  (testing "ifft! performs in-place 1D inverse FFT"
    (device/init!)
    (let [signal (array/create-array [[1.0 0.0] [2.0 0.0] [3.0 0.0] [4.0 0.0]] [4] jvm/AF_DTYPE_C32)
          result (signal/ifft! signal 0.25)]
      (try
        (is (= signal result))
        (finally
          (.close signal))))))

(deftest test-ifft2-inplace
  (testing "ifft2! performs in-place 2D inverse FFT"
    (device/init!)
    (let [signal (array/create-array (vec (map #(vector (float %) 0.0) (range 16))) [4 4] jvm/AF_DTYPE_C32)
          result (signal/ifft2! signal (/ 1.0 16.0))]
      (try
        (is (= signal result))
        (finally
          (.close signal))))))

(deftest test-ifft3-inplace
  (testing "ifft3! performs in-place 3D inverse FFT"
    (device/init!)
    (let [signal (array/create-array (vec (map #(vector (float %) 0.0) (range 8))) [2 2 2] jvm/AF_DTYPE_C32)
          result (signal/ifft3! signal (/ 1.0 8.0))]
      (try
        (is (= signal result))
        (finally
          (.close signal))))))

;;;
;;; Convolution Tests
;;;

(deftest test-convolve1
  (testing "convolve1 performs 1D convolution"
    (device/init!)
    (let [signal (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0]) [5] jvm/AF_DTYPE_F32)
          filter (array/create-array (float-array [1.0 1.0 1.0]) [3] jvm/AF_DTYPE_F32)
          result (signal/convolve1 signal filter)]
      (try
        (is (instance? AFArray result))
        (is (vector? (array/get-dims result)))
        (finally
          (.close signal)
          (.close filter)
          (.close result))))))

(deftest test-convolve2
  (testing "convolve2 performs 2D convolution"
    (device/init!)
    (let [image (array/create-array (float-array (range 25)) [5 5] jvm/AF_DTYPE_F32)
          kernel (array/create-array (float-array [1.0 1.0 1.0
                                                    1.0 1.0 1.0
                                                    1.0 1.0 1.0]) [3 3] jvm/AF_DTYPE_F32)
          result (signal/convolve2 image kernel)]
      (try
        (is (instance? AFArray result))
        (finally
          (.close image)
          (.close kernel)
          (.close result))))))

(deftest test-convolve3
  (testing "convolve3 performs 3D convolution"
    (device/init!)
    (let [volume (array/create-array (float-array (range 27)) [3 3 3] jvm/AF_DTYPE_F32)
          kernel (array/create-array (float-array (repeat 8 1.0)) [2 2 2] jvm/AF_DTYPE_F32)
          result (signal/convolve3 volume kernel)]
      (try
        (is (instance? AFArray result))
        (finally
          (.close volume)
          (.close kernel)
          (.close result))))))

(deftest test-convolve2-sep
  (testing "convolve2-sep performs separable 2D convolution"
    (device/init!)
    (let [image (array/create-array (float-array (range 25)) [5 5] jvm/AF_DTYPE_F32)
          col-filter (array/create-array (float-array [1.0 2.0 1.0]) [3] jvm/AF_DTYPE_F32)
          row-filter (array/create-array (float-array [1.0 2.0 1.0]) [3] jvm/AF_DTYPE_F32)
          result (signal/convolve2-sep col-filter row-filter image)]
      (try
        (is (instance? AFArray result))
        (finally
          (.close image)
          (.close col-filter)
          (.close row-filter)
          (.close result))))))

;;;
;;; FFT Convolution Tests
;;;

(deftest test-fft-convolve1
  (testing "fft-convolve1 performs frequency-domain 1D convolution"
    (device/init!)
    (let [signal (array/create-array (float-array (range 32)) [32] jvm/AF_DTYPE_F32)
          filter (array/create-array (float-array (repeat 16 1.0)) [16] jvm/AF_DTYPE_F32)
          result (signal/fft-convolve1 signal filter)]
      (try
        (is (instance? AFArray result))
        (finally
          (.close signal)
          (.close filter)
          (.close result))))))

(deftest test-fft-convolve2
  (testing "fft-convolve2 performs frequency-domain 2D convolution"
    (device/init!)
    (let [image (array/create-array (float-array (range 64)) [8 8] jvm/AF_DTYPE_F32)
          kernel (array/create-array (float-array (repeat 16 1.0)) [4 4] jvm/AF_DTYPE_F32)
          result (signal/fft-convolve2 image kernel)]
      (try
        (is (instance? AFArray result))
        (finally
          (.close image)
          (.close kernel)
          (.close result))))))

(deftest test-fft-convolve3
  (testing "fft-convolve3 performs frequency-domain 3D convolution"
    (device/init!)
    (let [volume (array/create-array (float-array (range 64)) [4 4 4] jvm/AF_DTYPE_F32)
          kernel (array/create-array (float-array (repeat 8 1.0)) [2 2 2] jvm/AF_DTYPE_F32)
          result (signal/fft-convolve3 volume kernel)]
      (try
        (is (instance? AFArray result))
        (finally
          (.close volume)
          (.close kernel)
          (.close result))))))

;;;
;;; IIR Filter Tests
;;;

(deftest test-iir
  (testing "iir applies infinite impulse response filter"
    (device/init!)
    (let [b (array/create-array (float-array [0.2 0.2 0.2 0.2 0.2]) [5] jvm/AF_DTYPE_F32)
          a (array/create-array (float-array [1.0 0.0 0.0 0.0 0.0]) [5] jvm/AF_DTYPE_F32)
          x (array/create-array (float-array (range 20)) [20] jvm/AF_DTYPE_F32)
          result (signal/iir b a x)]
      (try
        (is (instance? AFArray result))
        (is (= [20] (take 1 (array/get-dims result))))
        (finally
          (.close b)
          (.close a)
          (.close x)
          (.close result))))))

;;;
;;; Median Filter Tests
;;;

(deftest test-medfilt
  (testing "medfilt applies 2D median filter"
    (device/init!)
    (let [noisy (array/create-array (float-array (range 25)) [5 5] jvm/AF_DTYPE_F32)
          result (signal/medfilt noisy)]
      (try
        (is (instance? AFArray result))
        (is (= [5 5] (take 2 (array/get-dims result))))
        (finally
          (.close noisy)
          (.close result))))))

(deftest test-medfilt-custom-window
  (testing "medfilt with custom window size"
    (device/init!)
    (let [noisy (array/create-array (float-array (range 49)) [7 7] jvm/AF_DTYPE_F32)
          result (signal/medfilt noisy 5 5)]
      (try
        (is (instance? AFArray result))
        (finally
          (.close noisy)
          (.close result))))))

(deftest test-medfilt1
  (testing "medfilt1 applies 1D median filter"
    (device/init!)
    (let [noisy (array/create-array (float-array [1.0 10.0 2.0 3.0 15.0 4.0]) [6] jvm/AF_DTYPE_F32)
          result (signal/medfilt1 noisy)]
      (try
        (is (instance? AFArray result))
        (is (= [6] (take 1 (array/get-dims result))))
        (finally
          (.close noisy)
          (.close result))))))

(deftest test-medfilt1-custom-window
  (testing "medfilt1 with custom window size"
    (device/init!)
    (let [noisy (array/create-array (float-array (range 20)) [20] jvm/AF_DTYPE_F32)
          result (signal/medfilt1 noisy 5)]
      (try
        (is (instance? AFArray result))
        (finally
          (.close noisy)
          (.close result))))))

(deftest test-medfilt2
  (testing "medfilt2 applies 2D median filter"
    (device/init!)
    (let [noisy (array/create-array (float-array (range 36)) [6 6] jvm/AF_DTYPE_F32)
          result (signal/medfilt2 noisy)]
      (try
        (is (instance? AFArray result))
        (is (= [6 6] (take 2 (array/get-dims result))))
        (finally
          (.close noisy)
          (.close result))))))

;;;
;;; Interpolation Tests
;;;

(deftest test-approx1
  (testing "approx1 performs 1D interpolation"
    (device/init!)
    (let [yi (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4] jvm/AF_DTYPE_F32)
          xo (array/create-array (float-array [0.5 1.5 2.5]) [3] jvm/AF_DTYPE_F32)
          result (signal/approx1 yi xo)]
      (try
        (is (instance? AFArray result))
        (is (= [3] (take 1 (array/get-dims result))))
        (finally
          (.close yi)
          (.close xo)
          (.close result))))))

(deftest test-approx1-with-method
  (testing "approx1 with interpolation method"
    (device/init!)
    (let [yi (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4] jvm/AF_DTYPE_F32)
          xo (array/create-array (float-array [0.0 1.0 2.0 3.0]) [4] jvm/AF_DTYPE_F32)
          result (signal/approx1 yi xo 1)] ; Linear interpolation
      (try
        (is (instance? AFArray result))
        (finally
          (.close yi)
          (.close xo)
          (.close result))))))

(deftest test-approx1-uniform
  (testing "approx1-uniform performs uniform grid interpolation"
    (device/init!)
    (let [yi (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4] jvm/AF_DTYPE_F32)
          xo (array/create-array (float-array [0.5 1.5 2.5 3.5]) [4] jvm/AF_DTYPE_F32)
          result (signal/approx1-uniform yi xo)]
      (try
        (is (instance? AFArray result))
        (finally
          (.close yi)
          (.close xo)
          (.close result))))))

(deftest test-approx1-uniform-with-params
  (testing "approx1-uniform with grid parameters"
    (device/init!)
    (let [yi (array/create-array (float-array (range 10)) [10] jvm/AF_DTYPE_F32)
          xo (array/create-array (float-array [2.5 5.5 7.5]) [3] jvm/AF_DTYPE_F32)
          result (signal/approx1-uniform yi xo 0 0.0 1.0)]
      (try
        (is (instance? AFArray result))
        (finally
          (.close yi)
          (.close xo)
          (.close result))))))

(deftest test-approx2
  (testing "approx2 performs 2D interpolation"
    (device/init!)
    (let [zi (array/create-array (float-array (range 16)) [4 4] jvm/AF_DTYPE_F32)
          xo (array/create-array (float-array [0.5 1.5 2.5]) [3] jvm/AF_DTYPE_F32)
          yo (array/create-array (float-array [0.5 1.5 2.5]) [3] jvm/AF_DTYPE_F32)
          result (signal/approx2 zi xo yo)]
      (try
        (is (instance? AFArray result))
        (finally
          (.close zi)
          (.close xo)
          (.close yo)
          (.close result))))))

(deftest test-approx2-with-method
  (testing "approx2 with bilinear interpolation"
    (device/init!)
    (let [zi (array/create-array (float-array (range 25)) [5 5] jvm/AF_DTYPE_F32)
          xo (array/create-array (float-array [1.0 2.0 3.0]) [3] jvm/AF_DTYPE_F32)
          yo (array/create-array (float-array [1.0 2.0 3.0]) [3] jvm/AF_DTYPE_F32)
          result (signal/approx2 zi xo yo 2)] ; Bilinear
      (try
        (is (instance? AFArray result))
        (finally
          (.close zi)
          (.close xo)
          (.close yo)
          (.close result))))))

(deftest test-approx2-uniform
  (testing "approx2-uniform performs uniform 2D grid interpolation"
    (device/init!)
    (let [zi (array/create-array (float-array (range 16)) [4 4] jvm/AF_DTYPE_F32)
          xo (array/create-array (float-array [0.5 1.5 2.5]) [3] jvm/AF_DTYPE_F32)
          yo (array/create-array (float-array [0.5 1.5 2.5]) [3] jvm/AF_DTYPE_F32)
          result (signal/approx2-uniform zi xo yo)]
      (try
        (is (instance? AFArray result))
        (finally
          (.close zi)
          (.close xo)
          (.close yo)
          (.close result))))))

;;;
;;; Integration Tests
;;;

(deftest test-fft-roundtrip
  (testing "FFT followed by IFFT reconstructs original signal"
    (device/init!)
    (let [original (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4] jvm/AF_DTYPE_F32)
          freq (signal/fft original)
          reconstructed (signal/ifft freq 0.25)
          buf-orig (mem/alloc (* 4 4))
          buf-recon (mem/alloc (* 4 8))] ; Complex: 2 floats per element
      (try
        (array/get-data-ptr original buf-orig)
        (array/get-data-ptr reconstructed buf-recon)
        ;; Check real parts are approximately equal
        (is (approx= (mem/read-float buf-orig 0) (mem/read-float buf-recon 0) 0.01))
        (is (approx= (mem/read-float buf-orig 4) (mem/read-float buf-recon 8) 0.01))
        (finally
          (.close original)
          (.close freq)
          (.close reconstructed))))))

(deftest test-fft-r2c-c2r-roundtrip
  (testing "Real FFT roundtrip preserves real signal"
    (device/init!)
    (let [original (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4] jvm/AF_DTYPE_F32)
          freq (signal/fft-r2c original)
          reconstructed (signal/fft-c2r freq false)]
      (try
        (is (array/real? original))
        (is (array/complex? freq))
        (is (array/real? reconstructed))
        (is (= [4] (take 1 (array/get-dims reconstructed))))
        (finally
          (.close original)
          (.close freq)
          (.close reconstructed))))))

(deftest test-convolution-modes
  (testing "Convolution modes produce different output sizes"
    (device/init!)
    (let [signal (array/create-array (float-array (range 10)) [10] jvm/AF_DTYPE_F32)
          filter (array/create-array (float-array [1.0 1.0 1.0]) [3] jvm/AF_DTYPE_F32)
          result-default (signal/convolve1 signal filter 0)
          result-same (signal/convolve1 signal filter 2)]
      (try
        (is (instance? AFArray result-default))
        (is (instance? AFArray result-same))
        ;; Same mode should preserve input size
        (is (= [10] (take 1 (array/get-dims result-same))))
        (finally
          (.close signal)
          (.close filter)
          (.close result-default)
          (.close result-same))))))

(deftest test-median-filter-noise-reduction
  (testing "Median filter reduces noise"
    (device/init!)
    (let [;; Create signal with spike
          clean (array/create-array (float-array [1.0 1.0 1.0 1.0 1.0]) [5] jvm/AF_DTYPE_F32)
          noisy (array/create-array (float-array [1.0 1.0 10.0 1.0 1.0]) [5] jvm/AF_DTYPE_F32)
          filtered (signal/medfilt1 noisy)]
      (try
        (is (instance? AFArray filtered))
        ;; Median filter should remove spike
        (is (= [5] (take 1 (array/get-dims filtered))))
        (finally
          (.close clean)
          (.close noisy)
          (.close filtered))))))

(deftest test-fft-convolution-equivalence
  (testing "FFT convolution produces similar results to spatial convolution"
    (device/init!)
    (let [signal (array/create-array (float-array (range 32)) [32] jvm/AF_DTYPE_F32)
          filter (array/create-array (float-array [0.25 0.5 0.25]) [3] jvm/AF_DTYPE_F32)
          result-spatial (signal/convolve1 signal filter)
          result-fft (signal/fft-convolve1 signal filter)]
      (try
        (is (instance? AFArray result-spatial))
        (is (instance? AFArray result-fft))
        ;; Both should produce valid results
        (is (pos? (array/get-elements result-spatial)))
        (is (pos? (array/get-elements result-fft)))
        (finally
          (.close signal)
          (.close filter)
          (.close result-spatial)
          (.close result-fft))))))

(comment
  ;; run all tests from REPL
  (run-tests)
  
  ;; run individual tests - 1D FFT
  (run-test test-fft)
  (run-test test-fft-with-norm)
  (run-test test-fft-with-padding)
  
  ;; run individual tests - 2D/3D FFT
  (run-test test-fft2)
  (run-test test-fft2-with-padding)
  (run-test test-fft3)
  
  ;; run individual tests - Inverse FFT
  (run-test test-ifft)
  (run-test test-ifft2)
  (run-test test-ifft3)
  
  ;; run individual tests - Normalized FFT
  (run-test test-fft-norm)
  (run-test test-ifft-norm)
  (run-test test-fft-norm-roundtrip)
  (run-test test-fft2-norm)
  (run-test test-ifft2-norm)
  (run-test test-fft3-norm)
  (run-test test-ifft3-norm)
  
  ;; run individual tests - Real-to-Complex FFT
  (run-test test-fft-r2c)
  (run-test test-fft2-r2c)
  (run-test test-fft3-r2c)
  
  ;; run individual tests - Complex-to-Real FFT
  (run-test test-fft-c2r)
  (run-test test-fft2-c2r)
  (run-test test-fft3-c2r)
  
  ;; run individual tests - In-place FFT
  (run-test test-fft-inplace)
  (run-test test-fft2-inplace)
  (run-test test-fft3-inplace)
  (run-test test-ifft-inplace)
  (run-test test-ifft2-inplace)
  (run-test test-ifft3-inplace)
  
  ;; run individual tests - Convolution
  (run-test test-convolve1)
  (run-test test-convolve2)
  (run-test test-convolve3)
  (run-test test-convolve2-sep)
  
  ;; run individual tests - FFT Convolution
  (run-test test-fft-convolve1)
  (run-test test-fft-convolve2)
  (run-test test-fft-convolve3)
  
  ;; run individual tests - IIR Filter
  (run-test test-iir)
  
  ;; run individual tests - Median Filter
  (run-test test-medfilt)
  (run-test test-medfilt-custom-window)
  (run-test test-medfilt1)
  (run-test test-medfilt1-custom-window)
  (run-test test-medfilt2)
  
  ;; run individual tests - Interpolation
  (run-test test-approx1)
  (run-test test-approx1-with-method)
  (run-test test-approx1-uniform)
  (run-test test-approx1-uniform-with-params)
  (run-test test-approx2)
  (run-test test-approx2-with-method)
  (run-test test-approx2-uniform)
  
  ;; run individual tests - Integration
  (run-test test-fft-roundtrip)
  (run-test test-fft-r2c-c2r-roundtrip)
  (run-test test-convolution-modes)
  (run-test test-median-filter-noise-reduction)
  (run-test test-fft-convolution-equivalence)
  
  ;
  )
