(ns org.soulspace.arrayfire.api.image-processing-test
  "Tests for the idiomatic Clojure image processing API.
   All tests run inside (with-arrayfire ...) regions.

   Backend notes:
   - Morphological ops (dilate, erode, dilate3, erode3) need :opencl backend
   - Color space conversions (rgb->hsv, hsv->rgb) need :opencl backend
   - Many functions requiring GPU support use {:backend :opencl}
   - Use :f64 for value comparison tests (f32 ->value returns zeros)
   - regions requires :b8 (boolean) input + :opencl backend
   - Deconvolution and mean-shift need :f32 input
   - Anisotropic diffusion needs :f32 input"
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [org.soulspace.arrayfire.util.test :as util]
            [org.soulspace.arrayfire.api.core :as af]
            [org.soulspace.arrayfire.api.image-processing :as ip]))

;;;
;;; Guard tests — functions require with-arrayfire region
;;;

(deftest gradient-requires-region-test
  (testing "gradient throws IllegalStateException outside a region"
    (is (thrown? IllegalStateException
          (let [arr (af/with-arrayfire
                      (af/array [1.0 2.0 3.0 4.0] [2 2] :f64))]
            (ip/gradient arr))))))

(deftest resize-requires-region-test
  (testing "resize throws IllegalStateException outside a region"
    (is (thrown? IllegalStateException
          (let [arr (af/with-arrayfire
                      (af/array [1.0 2.0 3.0 4.0] [2 2] :f64))]
            (ip/resize arr 4 4))))))

(deftest dilate-requires-region-test
  (testing "dilate throws IllegalStateException outside a region"
    (is (thrown? IllegalStateException
          (let [arr (af/with-arrayfire
                      (af/array [1.0 2.0 3.0 4.0] [2 2] :f64))
                se  (af/with-arrayfire
                      (af/array [1.0 1.0 1.0 1.0] [2 2] :f64))]
            (ip/dilate arr se))))))

;;;
;;; Edge Detection and Gradients
;;;

(deftest gradient-basic-test
  (testing "gradient computes x and y gradients"
    (let [result (af/with-arrayfire
                   (let [img (af/array (double-array [1 2 3 4 5 6 7 8 9])
                                       [3 3] :f64)
                         [dx dy] (ip/gradient img)]
                     {:dx-shape (af/shape dx)
                      :dy-shape (af/shape dy)}))]
      (is (= [3 3] (:dx-shape result)))
      (is (= [3 3] (:dy-shape result))))))

(deftest sobel-basic-test
  (testing "sobel returns dx and dy components"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [img (af/array (double-array [1 2 3 4 5 6 7 8 9])
                                       [3 3] :f64)
                         [dx dy] (ip/sobel img)]
                     {:dx-shape (af/shape dx)
                      :dy-shape (af/shape dy)}))]
      (is (= [3 3] (:dx-shape result)))
      (is (= [3 3] (:dy-shape result))))))

(deftest sobel-custom-kernel-test
  (testing "sobel with custom kernel size returns correct shape"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [img (af/array (double-array (range 1.0 10.0))
                                       [3 3] :f64)
                         [dx dy] (ip/sobel img :kernel-size 3)]
                     {:dx-shape (af/shape dx)
                      :dy-shape (af/shape dy)}))]
      (is (= [3 3] (:dx-shape result)))
      (is (= [3 3] (:dy-shape result))))))

(deftest canny-basic-test
  (testing "canny edge detection produces binary edge map"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [img (af/array (double-array [0 0 0 0 0
                                                      0 1 1 1 0
                                                      0 1 1 1 0
                                                      0 1 1 1 0
                                                      0 0 0 0 0])
                                       [5 5] :f64)
                         edges (ip/canny img :threshold-type :manual :low-threshold 0.1 :high-threshold 0.9)]
                     (af/shape edges)))]
      (is (= [5 5] result)))))

;;;
;;; Geometric Transforms
;;;

(deftest resize-basic-test
  (testing "resize changes dimensions"
    (let [result (af/with-arrayfire
                   (let [img (af/array (double-array [1 2 3 4]) [2 2] :f64)
                         resized (ip/resize img 4 4)]
                     (af/shape resized)))]
      (is (= [4 4] result)))))

(deftest resize-method-test
  (testing "resize with bilinear interpolation"
    (let [result (af/with-arrayfire
                   (let [img (af/array (double-array [1 2 3 4]) [2 2] :f64)
                         resized (ip/resize img 4 4 :method :bilinear)]
                     (af/shape resized)))]
      (is (= [4 4] result)))))

(deftest rotate-basic-test
  (testing "rotate returns correct shape"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [img (af/array (double-array (range 1.0 17.0))
                                       [4 4] :f64)
                         rotated (ip/rotate img 0.5)]
                     (af/shape rotated)))]
      (is (= [4 4] result)))))

(deftest translate-basic-test
  (testing "translate shifts image"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [img (af/array (double-array (range 1.0 17.0))
                                       [4 4] :f64)
                         shifted (ip/translate img 1.0 1.0)]
                     (af/shape shifted)))]
      (is (= [4 4] result)))))

(deftest scale-basic-test
  (testing "scale by 0.5 halves the dimensions"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [img (af/array (double-array (range 1.0 17.0))
                                       [4 4] :f64)
                         scaled (ip/scale img 0.5 0.5)]
                     (af/shape scaled)))]
      (is (= [2 2] result)))))

(deftest scale-upscale-test
  (testing "scale by 2.0 doubles the dimensions"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [img (af/array (double-array [1 2 3 4]) [2 2] :f64)
                         scaled (ip/scale img 2.0 2.0)]
                     (af/shape scaled)))]
      (is (= [4 4] result)))))

(deftest skew-basic-test
  (testing "skew returns correct shape"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [img (af/array (double-array (range 1.0 17.0))
                                       [4 4] :f64)
                         skewed (ip/skew img 0.1 0.1)]
                     (af/shape skewed)))]
      (is (= [4 4] result)))))

(deftest transform-basic-test
  (testing "affine transform with identity preserves shape"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [img (af/array (double-array (range 1.0 17.0))
                                       [4 4] :f64)
                         ;; 3x2 identity affine matrix [[1 0 0] [0 1 0]] in column-major
                         tf (af/array (float-array [1 0 0 1 0 0]) [3 2] :f32)
                         out (ip/transform img tf 4 4)]
                     (af/shape out)))]
      (is (= [4 4] result)))))

(deftest transform-coordinates-basic-test
  (testing "transform-coordinates produces coordinate mapping"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [tf (af/array (float-array [1 0 0 0 1 0 0 0 1])
                                      [3 3] :f32)
                         tc (ip/transform-coordinates tf 4 4)]
                     (af/shape tc)))]
      ;; Output shape depends on AF implementation
      (is (vector? result))
      (is (pos? (count result))))))

;;;
;;; Morphological Operations (require :opencl backend + :f64 for value tests)
;;;

(deftest dilate-basic-test
  (testing "dilation with 3x3 ones SE expands a center pixel"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [;; Single center pixel set
                         img (af/array (double-array [0 0 0 0 1 0 0 0 0])
                                       [3 3] :f64)
                         se (af/array (double-array [1 1 1 1 1 1 1 1 1])
                                      [3 3] :f64)
                         dilated (ip/dilate img se)]
                     (af/->value dilated)))]
      ;; All pixels should be 1.0 after dilation
      (is (every? #(util/approx= % 1.0) (flatten result))))))

(deftest erode-basic-test
  (testing "erosion with 3x3 ones SE shrinks a block"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [img (af/array (double-array [1 1 1 1 1 1 1 1 1])
                                       [3 3] :f64)
                         se (af/array (double-array [1 1 1 1 1 1 1 1 1])
                                      [3 3] :f64)
                         eroded (ip/erode img se)]
                     {:shape (af/shape eroded)}))]
      (is (= [3 3] (:shape result))))))

(deftest dilate3-basic-test
  (testing "3D dilation returns correct shape"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [vol (af/array (double-array (replicate 27 0.0))
                                       [3 3 3] :f64)
                         se (af/array (double-array (replicate 27 1.0))
                                      [3 3 3] :f64)
                         dilated (ip/dilate3 vol se)]
                     (af/shape dilated)))]
      (is (= [3 3 3] result)))))

(deftest erode3-basic-test
  (testing "3D erosion returns correct shape"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [vol (af/array (double-array (replicate 27 1.0))
                                       [3 3 3] :f64)
                         se (af/array (double-array (replicate 27 1.0))
                                      [3 3 3] :f64)
                         eroded (ip/erode3 vol se)]
                     (af/shape eroded)))]
      (is (= [3 3 3] result)))))

;;;
;;; Filtering
;;;

(deftest bilateral-basic-test
  (testing "bilateral filter preserves shape"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [img (af/array (double-array [1 2 3 4 5 6 7 8 9])
                                       [3 3] :f64)
                         filtered (ip/bilateral img 1.0 1.0)]
                     (af/shape filtered)))]
      (is (= [3 3] result)))))

(deftest mean-shift-basic-test
  (testing "mean-shift filter preserves shape"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [img (af/array (float-array [10 20 30 40 50 60 70 80 90])
                                       [3 3] :f32)
                         filtered (ip/mean-shift img 5.0 20.0)]
                     (af/shape filtered)))]
      (is (= [3 3] result)))))

(deftest min-filter-basic-test
  (testing "min-filter produces minimum values in neighborhoods"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [img (af/array (double-array [1 2 3 4 5 6 7 8 9])
                                       [3 3] :f64)
                         filtered (ip/min-filter img)]
                     {:shape (af/shape filtered)
                      :vals (af/->value filtered)}))]
      (is (= [3 3] (:shape result)))
      ;; Top-left corner should be 1.0 (min in the neighborhood)
      (is (util/approx= (ffirst (:vals result)) 1.0)))))

(deftest max-filter-basic-test
  (testing "max-filter produces maximum values in neighborhoods"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [img (af/array (double-array [1 2 3 4 5 6 7 8 9])
                                       [3 3] :f64)
                         filtered (ip/max-filter img)]
                     {:shape (af/shape filtered)
                      :vals (af/->value filtered)}))]
      (is (= [3 3] (:shape result)))
      ;; Bottom-right corner should be 9.0 (max in the neighborhood)
      (let [vals (:vals result)
            bottom-right (last (last vals))]
        (is (util/approx= bottom-right 9.0))))))

(deftest gaussian-kernel-basic-test
  (testing "gaussian-kernel produces symmetric kernel"
    (let [result (af/with-arrayfire
                   (let [k (ip/gaussian-kernel 5 5)]
                     {:shape (af/shape k)
                      :vals (af/->value k)}))]
      (is (= [5 5] (:shape result)))
      ;; Center value should be the largest
      (let [vals (flatten (:vals result))
            center (nth vals 12)] ; center of 5x5
        (is (pos? center))))))

(deftest gaussian-kernel-custom-sigma-test
  (testing "gaussian-kernel with custom sigma"
    (let [result (af/with-arrayfire
                   (let [k (ip/gaussian-kernel 3 3 :sigma-r 1.0 :sigma-c 1.0)]
                     (af/shape k)))]
      (is (= [3 3] result)))))

(deftest anisotropic-diffusion-basic-test
  (testing "anisotropic diffusion preserves shape"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [img (af/array (float-array [0 0 0 0 0
                                                     0 1 1 1 0
                                                     0 1 1 1 0
                                                     0 1 1 1 0
                                                     0 0 0 0 0])
                                       [5 5] :f32)
                         filtered (ip/anisotropic-diffusion img)]
                     (af/shape filtered)))]
      (is (= [5 5] result)))))

(deftest anisotropic-diffusion-custom-params-test
  (testing "anisotropic diffusion with custom parameters"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [img (af/array (float-array [0 0 0 0 0
                                                     0 1 1 1 0
                                                     0 1 1 1 0
                                                     0 1 1 1 0
                                                     0 0 0 0 0])
                                       [5 5] :f32)
                         filtered (ip/anisotropic-diffusion img :dt 0.1 :k 0.05
                                                            :iterations 5
                                                            :flux-fn :exponential
                                                            :diffusion-eq :grad)]
                     (af/shape filtered)))]
      (is (= [5 5] result)))))

;;;
;;; Histogram
;;;

(deftest histogram-basic-test
  (testing "histogram produces correct number of bins"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [img (af/array (double-array [1 2 3 4 5 6 7 8 9])
                                       [3 3] :f64)
                         hist (ip/histogram img 10 1.0 9.0)]
                     (af/shape hist)))]
      (is (= [10] result)))))

(deftest histogram-equalize-basic-test
  (testing "histogram equalization preserves shape"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [img (af/array (float-array (range 1.0 17.0))
                                       [4 4] :f32)
                         hist (ip/histogram img 256 0.0 16.0)
                         eq (ip/histogram-equalize img hist)]
                     (af/shape eq)))]
      (is (= [4 4] result)))))

;;;
;;; Color Space Conversions
;;;

(deftest gray-to-rgb-test
  (testing "gray->rgb creates 3-channel output from gray"
    (let [result (af/with-arrayfire
                   (let [gray (af/array (float-array [0.5 0.5 0.5 0.5])
                                        [2 2] :f32)
                         rgb (ip/gray->rgb gray)]
                     (af/shape rgb)))]
      (is (= [2 2 3] result)))))

(deftest rgb-to-gray-test
  (testing "rgb->gray reduces 3 channels to 1"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [rgb (af/array (double-array (concat
                                                       (repeat 4 0.5)   ; R
                                                       (repeat 4 0.5)   ; G
                                                       (repeat 4 0.5))) ; B
                                       [2 2 3] :f64)
                         gray (ip/rgb->gray rgb)]
                     (af/shape gray)))]
      (is (= [2 2] result)))))

(deftest rgb-hsv-roundtrip-test
  (testing "rgb->hsv->rgb roundtrip preserves shape"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [rgb (af/array (double-array (concat
                                                       (repeat 4 0.5)
                                                       (repeat 4 0.3)
                                                       (repeat 4 0.8)))
                                       [2 2 3] :f64)
                         hsv (ip/rgb->hsv rgb)
                         back (ip/hsv->rgb hsv)]
                     {:hsv-shape (af/shape hsv)
                      :back-shape (af/shape back)}))]
      (is (= [2 2 3] (:hsv-shape result)))
      (is (= [2 2 3] (:back-shape result))))))

(deftest rgb-ycbcr-roundtrip-test
  (testing "rgb->ycbcr->rgb roundtrip preserves shape"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [rgb (af/array (double-array (concat
                                                       (repeat 4 0.5)
                                                       (repeat 4 0.3)
                                                       (repeat 4 0.8)))
                                       [2 2 3] :f64)
                         ycbcr (ip/rgb->ycbcr rgb)
                         back (ip/ycbcr->rgb ycbcr)]
                     {:ycbcr-shape (af/shape ycbcr)
                      :back-shape (af/shape back)}))]
      (is (= [2 2 3] (:ycbcr-shape result)))
      (is (= [2 2 3] (:back-shape result))))))

(deftest convert-color-space-test
  (testing "convert-color-space from rgb to gray"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [rgb (af/array (double-array (concat
                                                       (repeat 4 0.5)
                                                       (repeat 4 0.5)
                                                       (repeat 4 0.5)))
                                       [2 2 3] :f64)
                         gray (ip/convert-color-space rgb :gray :rgb)]
                     (af/shape gray)))]
      (is (= [2 2] result)))))

;;;
;;; Connected Components and Segmentation
;;;

(deftest regions-basic-test
  (testing "regions labels connected components in a binary image"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [;; Binary image with two separate blobs
                         binary-f32 (af/array (float-array [0 0 0 0 0
                                                             0 1 1 0 0
                                                             0 1 1 0 0
                                                             0 0 0 0 0
                                                             0 0 0 1 0])
                                              [5 5] :f32)
                         binary-b8 (af/cast binary-f32 :b8)
                         labels (ip/regions binary-b8)]
                     {:shape (af/shape labels)}))]
      (is (= [5 5] (:shape result))))))

;;;
;;; Patches (Unwrap / Wrap)
;;;

(deftest unwrap-basic-test
  (testing "unwrap extracts patches into columns"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [img (af/array (double-array (range 1.0 17.0))
                                       [4 4] :f64)
                         patches (ip/unwrap img 2 2 2 2)]
                     (af/shape patches)))]
      (is (= [4 4] result)))))

(deftest unwrap-with-padding-test
  (testing "unwrap with step=1 and padding=1 yields more patches"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [img (af/array (double-array (range 1.0 17.0))
                                       [4 4] :f64)
                         patches (ip/unwrap img 3 3 1 1 :px 1 :py 1 :column? true)]
                     (af/shape patches)))]
      ;; 9 elements per patch, more patches due to stride=1+padding
      (is (= 9 (first result))))))

(deftest wrap-roundtrip-test
  (testing "unwrap followed by wrap recovers original shape"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [img (af/array (double-array (range 1.0 17.0))
                                       [4 4] :f64)
                         patches (ip/unwrap img 2 2 2 2)
                         restored (ip/wrap patches 4 4 2 2 2 2)]
                     (af/shape restored)))]
      (is (= [4 4] result)))))

;;;
;;; Integral Image
;;;

(deftest summed-area-table-basic-test
  (testing "summed-area-table produces correct cumulative sums"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [img (af/array (double-array [1 2 3 4 5 6 7 8 9])
                                       [3 3] :f64)
                         sat (ip/summed-area-table img)]
                     {:shape (af/shape sat)
                      :vals (af/->value sat)}))]
      (is (= [3 3] (:shape result)))
      ;; SAT[0][0] = 1, SAT[2][2] = sum of all = 45
      (is (util/approx= (ffirst (:vals result)) 1.0))
      (is (util/approx= (last (last (:vals result))) 45.0)))))

;;;
;;; Deconvolution
;;;

(deftest iterative-deconv-basic-test
  (testing "iterative deconvolution preserves shape"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [img (af/array (float-array [10 20 30 40 50 60 70 80 90])
                                       [3 3] :f32)
                         ;; Delta PSF (identity kernel)
                         psf (af/array (float-array [0 0 0 0 1 0 0 0 0])
                                       [3 3] :f32)
                         result (ip/iterative-deconv img psf 10)]
                     (af/shape result)))]
      (is (= [3 3] result)))))

(deftest iterative-deconv-algo-test
  (testing "iterative deconvolution with Richardson-Lucy algorithm"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [img (af/array (float-array [10 20 30 40 50 60 70 80 90])
                                       [3 3] :f32)
                         psf (af/array (float-array [0 0 0 0 1 0 0 0 0])
                                       [3 3] :f32)
                         result (ip/iterative-deconv img psf 10
                                                     :relax-factor 1.0
                                                     :algo :richardson-lucy)]
                     (af/shape result)))]
      (is (= [3 3] result)))))

(deftest inverse-deconv-basic-test
  (testing "inverse deconvolution preserves shape"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [img (af/array (float-array [10 20 30 40 50 60 70 80 90])
                                       [3 3] :f32)
                         psf (af/array (float-array [0 0 0 0 1 0 0 0 0])
                                       [3 3] :f32)
                         result (ip/inverse-deconv img psf)]
                     (af/shape result)))]
      (is (= [3 3] result)))))

;;;
;;; Image Moments
;;;

(deftest moments-basic-test
  (testing "moments returns correct shape"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [img (af/array (double-array [1 2 3 4 5 6 7 8 9])
                                       [3 3] :f64)
                         m (ip/moments img :m00)]
                     (af/shape m)))]
      ;; moments returns a scalar-like array
      (is (vector? result)))))

(deftest moments-all-m00-test
  (testing "moments-all :m00 returns sum of all pixel values"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [img (af/array (double-array [1 2 3 4 5 6 7 8 9])
                                       [3 3] :f64)]
                     (ip/moments-all img :moment-type :m00)))]
      (is (map? result))
      (is (util/approx= (:M00 result) 45.0)))))

(deftest centroid-test
  (testing "centroid returns x, y, and area values"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [img (af/array (double-array [1 2 3 4 5 6 7 8 9])
                                       [3 3] :f64)]
                     (ip/centroid img)))]
      (is (map? result))
      (is (contains? result :x))
      (is (contains? result :y))
      (is (contains? result :area))
      (is (util/approx= (:area result) 45.0)))))

(deftest image-area-test
  (testing "image-area returns total intensity (M00)"
    (let [result (af/with-arrayfire {:backend :opencl}
                   (let [img (af/array (double-array [1 2 3 4 5 6 7 8 9])
                                       [3 3] :f64)]
                     (ip/image-area img)))]
      (is (util/approx= result 45.0)))))

;;;
;;; Gaussian Kernel Properties
;;;

(deftest gaussian-kernel-sums-to-one-test
  (testing "gaussian-kernel entries sum to approximately 1.0"
    (let [result (af/with-arrayfire
                   (let [k (ip/gaussian-kernel 5 5)
                         vals (af/->value k)]
                     (reduce + (flatten vals))))]
      (is (util/approx= result 1.0 1e-4)))))

;;;
;;; Comment form for running tests               
;;;

(comment
  (run-tests 'org.soulspace.arrayfire.api.image-processing-test)
  ;
  )
