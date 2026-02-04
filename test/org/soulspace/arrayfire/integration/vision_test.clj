(ns org.soulspace.arrayfire.integration.vision-test
  (:require [clojure.test :refer [deftest is testing run-test run-tests]]
            [org.soulspace.arrayfire.integration.vision :as vision]
            [org.soulspace.arrayfire.integration.array :as array]
            [org.soulspace.arrayfire.integration.device :as device]
            [org.soulspace.arrayfire.integration.jvm-integration :as jvm])
  (:import [org.soulspace.arrayfire.integration.jvm_integration AFArray]))

;;;
;;; Feature Detection Tests
;;;

(deftest test-fast
  (testing "FAST feature detection on simple image"
    (device/init!)
    (let [;; Create simple 32x32 test image with some corners
          img-data (float-array (* 32 32))
          _ (aset img-data 0 1.0) ;; corner at origin
          _ (aset img-data 1023 1.0) ;; corner at opposite corner
          img (array/create-array img-data [32 32] jvm/AF_DTYPE_F32)
          threshold 20.0
          arc-length 9
          non-max true
          feature-ratio 0.05
          edge 3
          features (vision/fast img threshold arc-length non-max feature-ratio edge)]
      (try
        (is (integer? features))
        (is (not (zero? features)))
        (finally
          (.close img))))))

(deftest test-fast-parameters
  (testing "FAST with different parameter combinations"
    (device/init!)
    (let [img (array/create-array (float-array (repeat (* 16 16) 0.5)) [16 16] jvm/AF_DTYPE_F32)
          ;; Lower threshold for simple test image
          features-low (vision/fast img 10.0 9 true 0.1 3)
          features-high (vision/fast img 30.0 9 true 0.05 3)]
      (try
        (is (integer? features-low))
        (is (integer? features-high))
        (finally
          (.close img))))))

(deftest test-harris
  (testing "Harris corner detection"
    (device/init!)
    (let [;; Create test image with clear corners
          img-data (float-array (* 32 32))
          _ (dotimes [i 10] (aset img-data i 1.0)) ;; horizontal edge
          img (array/create-array img-data [32 32] jvm/AF_DTYPE_F32)
          max-corners 100
          min-response 1.0e5
          sigma 1.0
          block-size 3
          k-thr 0.04
          features (vision/harris img max-corners min-response sigma block-size k-thr)]
      (try
        (is (integer? features))
        (finally
          (.close img))))))

(deftest test-harris-parameters
  (testing "Harris with different sigma and k values"
    (device/init!)
    (let [img (array/create-array (float-array (repeat (* 16 16) 0.5)) [16 16] jvm/AF_DTYPE_F32)
          features-small (vision/harris img 50 1.0e4 0.5 3 0.04)
          features-large (vision/harris img 50 1.0e4 2.0 5 0.06)]
      (try
        (is (integer? features-small))
        (is (integer? features-large))
        (finally
          (.close img))))))

(deftest test-orb
  (testing "ORB feature detection and description"
    (device/init!)
    (let [img (array/create-array (float-array (repeat (* 32 32) 0.5)) [32 32] jvm/AF_DTYPE_F32)
          fast-thr 20.0
          max-feat 100
          scl-fctr 1.5
          levels 4
          blur-img false
          [features descriptors] (vision/orb img fast-thr max-feat scl-fctr levels blur-img)]
      (try
        (is (integer? features))
        (is (instance? AFArray descriptors))
        (finally
          (.close img)
          (.close descriptors))))))

(deftest test-orb-with-blur
  (testing "ORB with image blurring enabled"
    (device/init!)
    (let [img (array/create-array (float-array (repeat (* 32 32) 0.5)) [32 32] jvm/AF_DTYPE_F32)
          [features descriptors] (vision/orb img 15.0 50 1.2 4 true)]
      (try
        (is (integer? features))
        (is (instance? AFArray descriptors))
        (finally
          (.close img)
          (.close descriptors))))))

(deftest test-sift
  (testing "SIFT feature detection and description"
    (device/init!)
    (let [img (array/create-array (float-array (repeat (* 64 64) 0.5)) [64 64] jvm/AF_DTYPE_F32)
          n-layers 3
          contrast-thr 0.04
          edge-thr 10.0
          init-sigma 1.6
          double-input false
          intensity-scale 0.00390625
          feature-ratio 0.05
          [features descriptors] (vision/sift img n-layers contrast-thr edge-thr init-sigma 
                                                    double-input intensity-scale feature-ratio)]
      (try
        (is (integer? features))
        (is (instance? AFArray descriptors))
        (is (= 128 (first (array/get-dims descriptors)))) ;; SIFT produces 128-dim descriptors
        (finally
          (.close img)
          (.close descriptors))))))

(deftest test-sift-double-input
  (testing "SIFT with double input size"
    (device/init!)
    (let [img (array/create-array (float-array (repeat (* 32 32) 0.5)) [32 32] jvm/AF_DTYPE_F32)
          [features descriptors] (vision/sift img 3 0.04 10.0 1.6 true 0.00390625 0.05)]
      (try
        (is (integer? features))
        (is (instance? AFArray descriptors))
        (finally
          (.close img)
          (.close descriptors))))))

(deftest test-gloh
  (testing "GLOH feature detection and description"
    (device/init!)
    (let [img (array/create-array (float-array (repeat (* 64 64) 0.5)) [64 64] jvm/AF_DTYPE_F32)
          n-layers 3
          contrast-thr 0.04
          edge-thr 10.0
          init-sigma 1.6
          double-input false
          intensity-scale 0.00390625
          feature-ratio 0.05
          [features descriptors] (vision/gloh img n-layers contrast-thr edge-thr init-sigma 
                                                     double-input intensity-scale feature-ratio)]
      (try
        (is (integer? features))
        (is (instance? AFArray descriptors))
        (is (= 272 (first (array/get-dims descriptors)))) ;; GLOH produces 272-dim descriptors
        (finally
          (.close img)
          (.close descriptors))))))

;;;
;;; Feature Matching Tests
;;;

(deftest test-hamming-matcher
  (testing "Hamming distance based feature matching"
    (device/init!)
    (let [;; Create simple descriptor arrays for matching
          desc1-data (float-array [1.0 0.0 1.0 0.0])
          desc2-data (float-array [1.0 0.0 1.0 0.0])
          desc1 (array/create-array desc1-data [4 1] jvm/AF_DTYPE_F32)
          desc2 (array/create-array desc2-data [4 1] jvm/AF_DTYPE_F32)
          dist-dim 0
          n-dist 1
          [idx dist] (vision/hamming-matcher desc1 desc2 dist-dim n-dist)]
      (try
        (is (instance? AFArray idx))
        (is (instance? AFArray dist))
        (finally
          (.close desc1)
          (.close desc2)
          (.close idx)
          (.close dist))))))

(deftest test-hamming-matcher-multiple-matches
  (testing "Hamming matcher with multiple nearest neighbors"
    (device/init!)
    (let [desc1 (array/create-array (float-array (repeat 16 0.5)) [8 2] jvm/AF_DTYPE_F32)
          desc2 (array/create-array (float-array (repeat 24 0.5)) [8 3] jvm/AF_DTYPE_F32)
          [idx dist] (vision/hamming-matcher desc1 desc2 0 2)]
      (try
        (is (instance? AFArray idx))
        (is (instance? AFArray dist))
        (finally
          (.close desc1)
          (.close desc2)
          (.close idx)
          (.close dist))))))

(deftest test-nearest-neighbour
  (testing "Nearest neighbor matching between feature sets"
    (device/init!)
    (let [query (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4 1] jvm/AF_DTYPE_F32)
          train (array/create-array (float-array [1.0 2.0 3.0 4.0]) [4 1] jvm/AF_DTYPE_F32)
          dist-dim 0
          n-dist 1
          dist-type :sad ;; Sum of absolute differences
          [idx dist] (vision/nearest-neighbour query train dist-dim n-dist dist-type)]
      (try
        (is (instance? AFArray idx))
        (is (instance? AFArray dist))
        (finally
          (.close query)
          (.close train)
          (.close idx)
          (.close dist))))))

(deftest test-nearest-neighbour-ssd
  (testing "Nearest neighbor with sum of squared differences"
    (device/init!)
    (let [query (array/create-array (float-array (repeat 8 1.0)) [4 2] jvm/AF_DTYPE_F32)
          train (array/create-array (float-array (repeat 12 1.0)) [4 3] jvm/AF_DTYPE_F32)
          [idx dist] (vision/nearest-neighbour query train 0 1 :ssd)]
      (try
        (is (instance? AFArray idx))
        (is (instance? AFArray dist))
        (finally
          (.close query)
          (.close train)
          (.close idx)
          (.close dist))))))

(deftest test-match-template
  (testing "Template matching in search image"
    (device/init!)
    (let [search-img (array/create-array (float-array (repeat (* 32 32) 0.5)) [32 32] jvm/AF_DTYPE_F32)
          template-img (array/create-array (float-array (repeat (* 8 8) 0.5)) [8 8] jvm/AF_DTYPE_F32)
          match-type :sad
          output (vision/match-template search-img template-img match-type)]
      (try
        (is (instance? AFArray output))
        (finally
          (.close search-img)
          (.close template-img)
          (.close output))))))

(deftest test-match-template-zsad
  (testing "Template matching with zero-mean SAD"
    (device/init!)
    (let [search-img (array/create-array (float-array (repeat (* 32 32) 1.0)) [32 32] jvm/AF_DTYPE_F32)
          template-img (array/create-array (float-array (repeat (* 4 4) 0.8)) [4 4] jvm/AF_DTYPE_F32)
          output (vision/match-template search-img template-img :zsad)]
      (try
        (is (instance? AFArray output))
        (finally
          (.close search-img)
          (.close template-img)
          (.close output))))))

;;;
;;; Advanced Feature Processing Tests
;;;

(deftest test-dog
  (testing "Difference of Gaussians pyramid"
    (device/init!)
    (let [img (array/create-array (float-array (repeat (* 64 64) 0.5)) [64 64] jvm/AF_DTYPE_F32)
          radius1 3
          radius2 6
          dog-output (vision/dog img radius1 radius2)]
      (try
        (is (instance? AFArray dog-output))
        (finally
          (.close img)
          (.close dog-output))))))

(deftest test-dog-different-scales
  (testing "DoG with various radius combinations"
    (device/init!)
    (let [img (array/create-array (float-array (repeat (* 32 32) 0.5)) [32 32] jvm/AF_DTYPE_F32)
          dog-fine (vision/dog img 1 2)
          dog-coarse (vision/dog img 4 6)] ; radius must be ≤ 8 on OneAPI
      (try
        (is (instance? AFArray dog-fine))
        (is (instance? AFArray dog-coarse))
        (finally
          (.close img)
          (.close dog-fine)
          (.close dog-coarse))))))

(deftest test-homography
  (testing "Homography estimation from matched features"
    (device/init!)
    (let [;; Create simple point correspondences (4 points minimum)
          x-src-data (float-array [0.0 10.0 10.0 0.0])
          y-src-data (float-array [0.0 0.0 10.0 10.0])
          x-dst-data (float-array [1.0 11.0 11.0 1.0])
          y-dst-data (float-array [1.0 1.0 11.0 11.0])
          x-src (array/create-array x-src-data [4] jvm/AF_DTYPE_F32)
          y-src (array/create-array y-src-data [4] jvm/AF_DTYPE_F32)
          x-dst (array/create-array x-dst-data [4] jvm/AF_DTYPE_F32)
          y-dst (array/create-array y-dst-data [4] jvm/AF_DTYPE_F32)
          hmat-type :ransac
          inlier-thr 3.0
          iterations 1000
          otype jvm/AF_DTYPE_F32
          [H inliers] (vision/homography x-src y-src x-dst y-dst hmat-type inlier-thr iterations otype)]
      (try
        (is (instance? AFArray H))
        (is (integer? inliers))
        (is (>= inliers 0))
        (finally
          (.close x-src)
          (.close y-src)
          (.close x-dst)
          (.close y-dst)
          (.close H))))))

(deftest test-homography-lmeds
  (testing "Homography with LMedS method"
    (device/init!)
    (let [x-src (array/create-array (float-array [0.0 5.0 5.0 0.0]) [4] jvm/AF_DTYPE_F32)
          y-src (array/create-array (float-array [0.0 0.0 5.0 5.0]) [4] jvm/AF_DTYPE_F32)
          x-dst (array/create-array (float-array [0.0 5.0 5.0 0.0]) [4] jvm/AF_DTYPE_F32)
          y-dst (array/create-array (float-array [0.0 0.0 5.0 5.0]) [4] jvm/AF_DTYPE_F32)
          [H inliers] (vision/homography x-src y-src x-dst y-dst :lmeds 3.0 1000 jvm/AF_DTYPE_F32)]
      (try
        (is (instance? AFArray H))
        (is (integer? inliers))
        (finally
          (.close x-src)
          (.close y-src)
          (.close x-dst)
          (.close y-dst)
          (.close H))))))

;;;
;;; Integration Tests
;;;

(deftest test-feature-detection-pipeline
  (testing "Full pipeline: detect features, describe, match"
    (device/init!)
    (let [img1 (array/create-array (float-array (repeat (* 32 32) 0.5)) [32 32] jvm/AF_DTYPE_F32)
          img2 (array/create-array (float-array (repeat (* 32 32) 0.5)) [32 32] jvm/AF_DTYPE_F32)
          [feat1 desc1] (vision/orb img1 20.0 100 1.5 4 false)
          [feat2 desc2] (vision/orb img2 20.0 100 1.5 4 false)
          [idx dist] (vision/hamming-matcher desc1 desc2 0 1)]
      (try
        (is (integer? feat1))
        (is (integer? feat2))
        (is (instance? AFArray desc1))
        (is (instance? AFArray desc2))
        (is (instance? AFArray idx))
        (is (instance? AFArray dist))
        (finally
          (.close img1)
          (.close img2)
          (.close desc1)
          (.close desc2)
          (.close idx)
          (.close dist))))))

(deftest test-multi-scale-detection
  (testing "Feature detection at multiple scales"
    (device/init!)
    (let [img (array/create-array (float-array (repeat (* 64 64) 0.5)) [64 64] jvm/AF_DTYPE_F32)
          fast-feat (vision/fast img 20.0 9 true 0.05 3)
          [orb-feat orb-desc] (vision/orb img 20.0 100 1.5 4 false)
          harris-feat (vision/harris img 100 1.0e5 1.0 3 0.04)]
      (try
        (is (integer? fast-feat))
        (is (integer? orb-feat))
        (is (integer? harris-feat))
        (finally
          (.close img)
          (.close orb-desc))))))

(comment
  ;; run all tests from REPL
  (run-tests)
  
  ;; run individual tests - FAST
  (run-test test-fast)
  (run-test test-fast-parameters)
  
  ;; run individual tests - Harris
  (run-test test-harris)
  (run-test test-harris-parameters)
  
  ;; run individual tests - ORB
  (run-test test-orb)
  (run-test test-orb-with-blur)
  
  ;; run individual tests - SIFT
  (run-test test-sift)
  (run-test test-sift-double-input)
  
  ;; run individual tests - GLOH
  (run-test test-gloh)
  
  ;; run individual tests - Matching
  (run-test test-hamming-matcher)
  (run-test test-hamming-matcher-multiple-matches)
  (run-test test-nearest-neighbour)
  (run-test test-nearest-neighbour-ssd)
  (run-test test-match-template)
  (run-test test-match-template-zsad)
  
  ;; run individual tests - DoG
  (run-test test-dog)
  (run-test test-dog-different-scales)
  
  ;; run individual tests - Homography
  (run-test test-homography)
  (run-test test-homography-lmeds)
  
  ;; run individual tests - Integration
  (run-test test-feature-detection-pipeline)
  (run-test test-multi-scale-detection)
  
  ;
  )
