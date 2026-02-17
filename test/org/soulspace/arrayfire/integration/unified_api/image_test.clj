(ns org.soulspace.arrayfire.integration.unified-api.image-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [org.soulspace.arrayfire.util.test :refer [approx=]]
            [org.soulspace.arrayfire.ffi.base.definitions :as defs]
            [org.soulspace.arrayfire.integration.unified-api.image :as img]
            [org.soulspace.arrayfire.integration.unified-api.array :as array]
            [org.soulspace.arrayfire.integration.unified-api.device :as device])
  (:import [org.soulspace.arrayfire.integration.base.resource AFArray]))

;;;
;;; Gradient and Edge Detection Tests
;;;

(deftest test-gradient
  (testing "gradient computes image gradients"
    (device/init!)
    (let [img (array/create-array (float-array [1.0 2.0 3.0
                                                 4.0 5.0 6.0
                                                 7.0 8.0 9.0])
                                   [3 3] defs/AF_DTYPE_F32)
          [dx dy] (img/gradient img)]
      (is (instance? AFArray dx))
      (is (instance? AFArray dy))
      (is (= [3 3 1 1] (vec (array/get-dims dx))))
      (is (= [3 3 1 1] (vec (array/get-dims dy))))
      (.close img)
      (.close dx)
      (.close dy))))

(deftest test-sobel
  (testing "sobel edge detection"
    (device/init!)
    (let [img (array/create-array (float-array [1.0 2.0 3.0
                                                 4.0 5.0 6.0
                                                 7.0 8.0 9.0])
                                   [3 3] defs/AF_DTYPE_F32)]
      (try
        (let [[dx dy] (img/sobel img 3)]
          (try
            (is (instance? AFArray dx))
            (is (instance? AFArray dy))
            (finally
              (.close dx)
              (.close dy))))
        (finally
          (.close img))))))

;;;
;;; Geometric Transformation Tests
;;;

(deftest test-resize
  (testing "resize changes image dimensions"
    (device/init!)
    (let [img (array/create-array (float-array [1.0 2.0 3.0 4.0])
                                   [2 2] defs/AF_DTYPE_F32)]
      (try
        (let [resized (img/resize img 4 4 0)] ; AF_INTERP_NEAREST supported by all backends
          (try
            (is (instance? AFArray resized))
            (is (= [4 4 1 1] (vec (array/get-dims resized))))
            (finally
              (.close resized))))
        (finally
          (.close img))))))

(deftest test-rotate
  (testing "rotate rotates image"
    (device/init!)
    (let [img (array/create-array (float-array [1.0 2.0 3.0 4.0])
                                   [2 2] defs/AF_DTYPE_F32)]
      (try
        (let [rotated (img/rotate img 0.785398 true 0)] ; 45 degrees in radians, AF_INTERP_NEAREST
          (try
            (is (instance? AFArray rotated))
            (finally
              (.close rotated))))
        (finally
          (.close img))))))

(deftest test-translate
  (testing "translate shifts image"
    (device/init!)
    (let [img (array/create-array (float-array [1.0 2.0 3.0 4.0])
                                   [2 2] defs/AF_DTYPE_F32)]
      (try
        (let [translated (img/translate img 1.0 1.0 2 2 2)] ; AF_INTERP_BILINEAR
          (try
            (is (instance? AFArray translated))
            (finally
              (.close translated))))
        (finally
          (.close img))))))

(deftest test-scale
  (testing "scale resizes by factors"
    (device/init!)
    (let [img (array/create-array (float-array [1.0 2.0 3.0 4.0])
                                   [2 2] defs/AF_DTYPE_F32)]
      (try
        (let [scaled (img/scale img 2.0 2.0 4 4 2)] ; AF_INTERP_BILINEAR
          (try
            (is (instance? AFArray scaled))
            (finally
              (.close scaled))))
        (finally
          (.close img))))))

(deftest test-skew
  (testing "skew transforms image"
    (device/init!)
    (let [img (array/create-array (float-array [1.0 2.0 3.0 4.0])
                                   [2 2] defs/AF_DTYPE_F32)]
      (try
        (let [skewed (img/skew img 0.5 0.5 2 2 0 false)] ; AF_INTERP_NEAREST
          (try
            (is (instance? AFArray skewed))
            (finally
              (.close skewed))))
        (finally
          (.close img))))))

;;;
;;; Morphological Operations Tests
;;;

(deftest test-dilate
  (testing "dilate performs morphological dilation"
    (device/init!)
    (let [img (array/create-array (byte-array [0 0 0
                                                0 1 0
                                                0 0 0])
                                   [3 3] defs/AF_DTYPE_B8)
          mask (array/create-array (byte-array [1 1 1
                                                 1 1 1
                                                 1 1 1])
                                    [3 3] defs/AF_DTYPE_B8)]
      (try
        (let [dilated (img/dilate img mask)]
          (try
            (is (instance? AFArray dilated))
            (is (= [3 3 1 1] (vec (array/get-dims dilated))))
            (finally
              (.close dilated))))
        (finally
          (.close img)
          (.close mask))))))

(deftest test-erode
  (testing "erode performs morphological erosion"
    (device/init!)
    (let [img (array/create-array (byte-array [1 1 1
                                                1 1 1
                                                1 1 1])
                                   [3 3] defs/AF_DTYPE_B8)
          mask (array/create-array (byte-array [1 1 1
                                                 1 1 1
                                                 1 1 1])
                                    [3 3] defs/AF_DTYPE_B8)]
      (try
        (let [eroded (img/erode img mask)]
          (try
            (is (instance? AFArray eroded))
            (is (= [3 3 1 1] (vec (array/get-dims eroded))))
            (finally
              (.close eroded))))
        (finally
          (.close img)
          (.close mask))))))

;;;
;;; Filtering Tests
;;;

(deftest test-bilateral
  (testing "bilateral filter for edge-preserving smoothing"
    (device/init!)
    (let [img (array/create-array (float-array [1.0 2.0 3.0
                                                 4.0 5.0 6.0
                                                 7.0 8.0 9.0])
                                   [3 3] defs/AF_DTYPE_F32)
          filtered (img/bilateral img 2.0 2.0)]
      (is (instance? AFArray filtered))
      (is (= [3 3 1 1] (vec (array/get-dims filtered))))
      (.close img)
      (.close filtered))))

(deftest test-minfilt
  (testing "minfilt minimum filter"
    (device/init!)
    (let [img (array/create-array (float-array [1.0 5.0 3.0
                                                 7.0 2.0 8.0
                                                 4.0 6.0 9.0])
                                   [3 3] defs/AF_DTYPE_F32)]
      (try
        (let [filtered (img/minfilt img 3 3 0)]
          (try
            (is (instance? AFArray filtered))
            (finally
              (.close filtered))))
        (finally
          (.close img))))))

(deftest test-maxfilt
  (testing "maxfilt maximum filter"
    (device/init!)
    (let [img (array/create-array (float-array [1.0 5.0 3.0
                                                 7.0 2.0 8.0
                                                 4.0 6.0 9.0])
                                   [3 3] defs/AF_DTYPE_F32)]
      (try
        (let [filtered (img/maxfilt img 3 3 0)]
          (try
            (is (instance? AFArray filtered))
            (finally
              (.close filtered))))
        (finally
          (.close img))))))

(deftest test-gaussian-kernel
  (testing "gaussian-kernel generates Gaussian kernel"
    (device/init!)
    (let [kernel (img/gaussian-kernel 5 5 1.0 1.0)]
      (is (instance? AFArray kernel))
      (is (= [5 5 1 1] (vec (array/get-dims kernel))))
      (.close kernel))))

;;;
;;; Histogram Tests
;;;

(deftest test-histogram
  (testing "histogram computes value distribution"
    (device/init!)
    (let [img (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0
                                                 1.0 2.0 3.0 4.0 5.0])
                                   [10] defs/AF_DTYPE_F32)
          hist (img/histogram img 5 1.0 5.0)]
      (is (instance? AFArray hist))
      ;; Should have 5 bins
      (let [dims (array/get-dims hist)]
        (is (= 5 (first dims))))
      (.close img)
      (.close hist))))

(deftest test-hist-equal
  (testing "hist-equal performs histogram equalization"
    (device/init!)
    (let [img (array/create-array (float-array [1.0 2.0 3.0 4.0 5.0
                                                 1.0 2.0 3.0 4.0 5.0])
                                   [10] defs/AF_DTYPE_F32)
          hist (img/histogram img 5 1.0 5.0)
          equalized (img/hist-equal img hist)]
      (is (instance? AFArray equalized))
      (.close img)
      (.close hist)
      (.close equalized))))

;;;
;;; Color Space Conversion Tests
;;;

(deftest test-rgb-gray
  (testing "rgb->gray converts RGB to grayscale"
    (device/init!)
    ;; Create 2x2 RGB image (3 channels)
    (let [rgb (array/create-array (float-array [1.0 0.5
                                                 0.0 1.0
                                                 0.5 0.0
                                                 1.0 0.5
                                                 0.0 1.0
                                                 0.5 0.0])
                                   [2 2 3] defs/AF_DTYPE_F32)
          gray (img/rgb->gray rgb)]
      (is (instance? AFArray gray))
      ;; Gray should have only 1 channel
      (let [dims (array/get-dims gray)]
        (is (= 1 (nth dims 2))))
      (.close rgb)
      (.close gray))))

(deftest test-gray-rgb
  (testing "gray->rgb converts grayscale to RGB"
    (device/init!)
    (let [gray (array/create-array (float-array [0.5 0.5 0.5 0.5])
                                    [2 2] defs/AF_DTYPE_F32)
          rgb (img/gray->rgb gray)]
      (is (instance? AFArray rgb))
      ;; RGB should have 3 channels
      (let [dims (array/get-dims rgb)]
        (is (= 3 (nth dims 2))))
      (.close gray)
      (.close rgb))))

(deftest test-rgb-hsv
  (testing "rgb->hsv converts RGB to HSV"
    (device/init!)
    (let [rgb (array/create-array (float-array [1.0 0.5
                                                 0.0 1.0
                                                 0.5 0.0
                                                 1.0 0.5
                                                 0.0 1.0
                                                 0.5 0.0])
                                   [2 2 3] defs/AF_DTYPE_F32)]
      (try
        (let [hsv (img/rgb->hsv rgb)]
          (try
            (is (instance? AFArray hsv))
            (is (= 3 (nth (array/get-dims hsv) 2)))
            (finally
              (.close hsv))))
        (finally
          (.close rgb))))))

(deftest test-hsv-rgb
  (testing "hsv->rgb converts HSV to RGB"
    (device/init!)
    ;; HSV components in [0,1] range
    (let [hsv (array/create-array (float-array [0.0 0.5
                                                 0.25 0.75
                                                 0.1 0.9
                                                 0.2 0.8
                                                 0.3 0.7
                                                 0.4 0.6])
                                   [2 2 3] defs/AF_DTYPE_F32)]
      (try
        (let [rgb (img/hsv->rgb hsv)]
          (try
            (is (instance? AFArray rgb))
            (is (= 3 (nth (array/get-dims rgb) 2)))
            (finally
              (.close rgb))))
        (finally
          (.close hsv))))))

;;;
;;; Region and Segmentation Tests
;;;

(deftest test-regions
  (testing "regions labels connected components"
    (device/init!)
    ;; regions requires binary input of type b8 (AF_DTYPE_B8 = 4)
    (let [binary (array/create-array (byte-array [1 1 0
                                                   1 0 0
                                                   0 0 1])
                                      [3 3] defs/AF_DTYPE_B8)]
      (try
        (let [labeled (img/regions binary 4 5)] ; connectivity 4 = AF_CONNECTIVITY_4, dtype 5 = AF_DTYPE_S32
          (try
            (is (instance? AFArray labeled))
            (is (= [3 3 1 1] (vec (array/get-dims labeled))))
            (finally
              (.close labeled))))
        (finally
          (.close binary))))))

;;;
;;; Advanced Image Processing Tests
;;;

(deftest test-sat
  (testing "sat computes summed area table"
    (device/init!)
    (let [img (array/create-array (float-array [1.0 2.0 3.0
                                                 4.0 5.0 6.0
                                                 7.0 8.0 9.0])
                                   [3 3] defs/AF_DTYPE_F32)
          sat-img (img/sat img)]
      (is (instance? AFArray sat-img))
      (is (= [3 3 1 1] (vec (array/get-dims sat-img))))
      (.close img)
      (.close sat-img))))

(deftest test-unwrap-wrap
  (testing "unwrap and wrap for patch operations"
    (device/init!)
    (let [img (array/create-array (float-array [1.0 2.0 3.0 4.0
                                                 5.0 6.0 7.0 8.0
                                                 9.0 10.0 11.0 12.0
                                                 13.0 14.0 15.0 16.0])
                                   [4 4] defs/AF_DTYPE_F32)
          ;; Unwrap 2x2 patches with stride 2
          unwrapped (img/unwrap img 2 2 2 2)
          ;; Wrap back to original
          wrapped (img/wrap unwrapped 4 4 2 2 2 2)]
      (is (instance? AFArray unwrapped))
      (is (instance? AFArray wrapped))
      (.close img)
      (.close unwrapped)
      (.close wrapped))))

(comment
  ;; run tests from REPL
  (run-tests)
  ;
  )
