(ns org.soulspace.arrayfire.integration.unified-api.ml-test
  (:require [clojure.test :refer [deftest is testing run-test run-tests]]
            [org.soulspace.arrayfire.ffi.base.definitions :as defs]
            [org.soulspace.arrayfire.integration.base.resource :as res]
            [org.soulspace.arrayfire.integration.unified-api.ml :as ml]
            [org.soulspace.arrayfire.integration.unified-api.array :as array]
            [org.soulspace.arrayfire.integration.unified-api.signal :as signal]
            [org.soulspace.arrayfire.integration.unified-api.device :as device]
            [tech.v3.resource :refer [releasing!]])
  (:import [org.soulspace.arrayfire.integration.base.resource AFArray]))

;;;
;;; Gradient Type Conversion Tests
;;;

(deftest test-gradient-type-to-int-filter
  (testing "gradient-type->int converts :filter to correct integer"
    (device/init!)
    (let [result (ml/gradient-type->int :filter)]
      (is (integer? result))
      (is (= 1 result))))) ; AF_CONV_GRADIENT_FILTER

(deftest test-gradient-type-to-int-data
  (testing "gradient-type->int converts :data to correct integer"
    (device/init!)
    (let [result (ml/gradient-type->int :data)]
      (is (integer? result))
      (is (= 2 result))))) ; AF_CONV_GRADIENT_DATA

(deftest test-gradient-type-to-int-bias
  (testing "gradient-type->int converts :bias to correct integer"
    (device/init!)
    (let [result (ml/gradient-type->int :bias)]
      (is (integer? result))
      (is (= 3 result))))) ; AF_CONV_GRADIENT_BIAS

(deftest test-gradient-type-to-int-default
  (testing "gradient-type->int handles default case"
    (device/init!)
    (let [result (ml/gradient-type->int :filter)]
      (is (integer? result)))))

;;;
;;; Core CNN Gradient Computation Tests
;;;

(deftest test-convolve2-gradient-nn-filter
  (testing "convolve2-gradient-nn computes filter gradient"
    (device/init!)
    (releasing!
      (let [;; Create original input
            original-input (array/create-array (float-array [1.0 0.0 0.0 1.0 
                                                             0.0 1.0 1.0 0.0]) 
                                              [2 2 2 1] defs/AF_DTYPE_F32)
            ;; Create original filter
            original-filter (array/create-array (float-array [1.0 0.0 0.0 1.0 
                                                              0.0 1.0 1.0 0.0]) 
                                               [2 2 2 1] defs/AF_DTYPE_F32)
            ;; Perform forward pass to get output
            output (signal/convolve2-nn original-input original-filter [1 1] [1 1] [1 1])
            output-dims (array/get-dims output)
            ;; Create incoming gradient with output dimensions
            incoming-grad (array/create-array (float-array (repeat (reduce * (take 2 output-dims)) 1.0)) 
                                             output-dims defs/AF_DTYPE_F32)
            result (ml/convolve2-gradient-nn incoming-grad original-input 
                                             original-filter output [1 1] [1 1] [1 1]
                                             :filter)] ; AF_CONV_GRADIENT_FILTER
        (is (instance? AFArray result))
        (is (= [2 2] (take 2 (array/get-dims result))))))))

(deftest test-convolve2-gradient-nn-data
  (testing "convolve2-gradient-nn computes data gradient"
    (device/init!)
    (releasing!
      (let [original-input (array/create-array (float-array [1.0 0.0 0.0 1.0 
                                                             0.0 1.0 1.0 0.0]) 
                                              [2 2 2 1] defs/AF_DTYPE_F32)
            original-filter (array/create-array (float-array [1.0 0.0 0.0 1.0 
                                                              0.0 1.0 1.0 0.0]) 
                                               [2 2 2 1] defs/AF_DTYPE_F32)
            ;; Perform forward pass to get output
            output (signal/convolve2-nn original-input original-filter [1 1] [1 1] [1 1])
            output-dims (array/get-dims output)
            incoming-grad (array/create-array (float-array (repeat (reduce * (take 2 output-dims)) 1.0)) 
                                             output-dims defs/AF_DTYPE_F32)
            result (ml/convolve2-gradient-nn incoming-grad original-input 
                                             original-filter output [1 1] [1 1] [1 1]
                                             :data)] ; AF_CONV_GRADIENT_DATA
        (is (instance? AFArray result))))))

(deftest test-convolve2-gradient-nn-bias
  (testing "convolve2-gradient-nn computes bias gradient"
    (device/init!)
    (releasing!
      (let [original-input (array/create-array (float-array [1.0 0.0 0.0 1.0 
                                                             0.0 1.0 1.0 0.0]) 
                                              [2 2 2 1] defs/AF_DTYPE_F32)
            original-filter (array/create-array (float-array [1.0 0.0 0.0 1.0 
                                                              0.0 1.0 1.0 0.0 
                                                              1.0 0.0 0.0 1.0 
                                                              0.0 1.0 1.0 0.0]) 
                                               [2 2 2 2] defs/AF_DTYPE_F32)
            ;; Perform forward pass to get output
            output (signal/convolve2-nn original-input original-filter [1 1] [1 1] [1 1])
            output-dims (array/get-dims output)
            incoming-grad (array/create-array (float-array (repeat (reduce * output-dims) 1.0)) 
                                             output-dims defs/AF_DTYPE_F32)
            result (ml/convolve2-gradient-nn incoming-grad original-input 
                                             original-filter output [1 1] [1 1] [1 1]
                                             :bias)] ; AF_CONV_GRADIENT_BIAS
        (is (instance? AFArray result))))))

(deftest test-convolve2-gradient-nn-with-stride
  (testing "convolve2-gradient-nn respects stride parameters"
    (device/init!)
    (releasing!
      (let [original-input (array/create-array (float-array (repeat 16 1.0)) 
                                              [4 4 1 1] defs/AF_DTYPE_F32)
            original-filter (array/create-array (float-array [1.0 1.0 
                                                              1.0 1.0]) 
                                               [2 2 1 1] defs/AF_DTYPE_F32)
            ;; Perform forward pass to get output
            output (signal/convolve2-nn original-input original-filter [2 2] [1 1] [1 1])
            output-dims (array/get-dims output)
            incoming-grad (array/create-array (float-array (repeat (reduce * (take 2 output-dims)) 1.0)) 
                                             output-dims defs/AF_DTYPE_F32)
            result (ml/convolve2-gradient-nn incoming-grad original-input 
                                             original-filter output [2 2] [1 1] [1 1]
                                             :filter)]
        (is (instance? AFArray result))))))

(deftest test-convolve2-gradient-nn-with-padding
  (testing "convolve2-gradient-nn handles padding"
    (device/init!)
    (releasing!
      (let [original-input (array/create-array (float-array [1.0 0.0 0.0 1.0]) 
                                              [2 2 1 1] defs/AF_DTYPE_F32)
            original-filter (array/create-array (float-array [1.0 0.0 
                                                              0.0 1.0]) 
                                               [2 2 1 1] defs/AF_DTYPE_F32)
            ;; Perform forward pass to get output
            output (signal/convolve2-nn original-input original-filter [1 1] [2 2] [1 1])
            output-dims (array/get-dims output)
            incoming-grad (array/create-array (float-array (repeat (reduce * (take 2 output-dims)) 1.0)) 
                                             output-dims defs/AF_DTYPE_F32)
            result (ml/convolve2-gradient-nn incoming-grad original-input 
                                             original-filter output [1 1] [2 2] [1 1]
                                             :filter)]
        (is (instance? AFArray result))))))

(deftest test-convolve2-gradient-nn-with-dilation
  (testing "convolve2-gradient-nn supports dilation"
    (device/init!)
    (releasing!
      (let [original-input (array/create-array (float-array (repeat 16 1.0)) 
                                              [4 4 1 1] defs/AF_DTYPE_F32)
            original-filter (array/create-array (float-array [1.0 0.0 
                                                              0.0 1.0]) 
                                               [2 2 1 1] defs/AF_DTYPE_F32)
            ;; Perform forward pass to get output
            output (signal/convolve2-nn original-input original-filter [1 1] [2 2] [1 1])
            output-dims (array/get-dims output)
            incoming-grad (array/create-array (float-array (repeat (reduce * (take 2 output-dims)) 1.0)) 
                                             output-dims defs/AF_DTYPE_F32)
            result (ml/convolve2-gradient-nn incoming-grad original-input 
                                             original-filter output [1 1] [2 2] [1 1]
                                             :filter)]
        (is (instance? AFArray result))))))

;;;
;;; Convenience Wrapper Tests
;;;

(deftest test-filter-gradient
  (testing "filter-gradient computes filter gradient"
    (device/init!)
    (releasing!
      (let [original-input (array/create-array (float-array [1.0 0.0 0.0 1.0 
                                                             0.0 1.0 1.0 0.0]) 
                                              [2 2 2 1] defs/AF_DTYPE_F32)
            original-filter (array/create-array (float-array [1.0 0.0 0.0 1.0 
                                                              0.0 1.0 1.0 0.0]) 
                                               [2 2 2 1] defs/AF_DTYPE_F32)
            ;; Perform forward pass to get output
            output (signal/convolve2-nn original-input original-filter [1 1] [1 1] [1 1])
            output-dims (array/get-dims output)
            incoming-grad (array/create-array (float-array (repeat (reduce * (take 2 output-dims)) 1.0)) 
                                             output-dims defs/AF_DTYPE_F32)
            result (ml/filter-gradient incoming-grad original-input original-filter output
                                       [1 1] [1 1] [1 1])]
        (is (instance? AFArray result))))))

(deftest test-filter-gradient-with-params
  (testing "filter-gradient with custom stride, dilation, padding"
    (device/init!)
    (releasing!
      (let [original-input (array/create-array (float-array (repeat 16 1.0)) 
                                              [4 4 1 1] defs/AF_DTYPE_F32)
            original-filter (array/create-array (float-array [1.0 1.0 
                                                              1.0 1.0]) 
                                               [2 2 1 1] defs/AF_DTYPE_F32)
            ;; Perform forward pass to get output
            output (signal/convolve2-nn original-input original-filter [2 2] [1 1] [1 1])
            output-dims (array/get-dims output)
            incoming-grad (array/create-array (float-array (repeat (reduce * (take 2 output-dims)) 1.0)) 
                                             output-dims defs/AF_DTYPE_F32)
            result (ml/filter-gradient incoming-grad original-input original-filter output
                                      [2 2] [1 1] [1 1])]
        (is (instance? AFArray result))))))

(deftest test-data-gradient
  (testing "data-gradient computes data gradient"
    (device/init!)
    (releasing!
      (let [original-input (array/create-array (float-array [1.0 0.0 0.0 1.0 
                                                             0.0 1.0 1.0 0.0]) 
                                              [2 2 2 1] defs/AF_DTYPE_F32)
            original-filter (array/create-array (float-array [1.0 0.0 0.0 1.0 
                                                              0.0 1.0 1.0 0.0]) 
                                               [2 2 2 1] defs/AF_DTYPE_F32)
            ;; Perform forward pass to get output
            output (signal/convolve2-nn original-input original-filter [1 1] [1 1] [1 1])
            output-dims (array/get-dims output)
            incoming-grad (array/create-array (float-array (repeat (reduce * (take 2 output-dims)) 1.0)) 
                                             output-dims defs/AF_DTYPE_F32)
            result (ml/data-gradient incoming-grad original-input original-filter output
                                     [1 1] [1 1] [1 1])]
        (is (instance? AFArray result))))))

(deftest test-data-gradient-with-params
  (testing "data-gradient with custom stride, dilation, padding"
    (device/init!)
    (releasing!
      (let [original-input (array/create-array (float-array (repeat 16 1.0)) 
                                              [4 4 1 1] defs/AF_DTYPE_F32)
            original-filter (array/create-array (float-array [1.0 1.0 
                                                              1.0 1.0]) 
                                               [2 2 1 1] defs/AF_DTYPE_F32)
            ;; Perform forward pass to get output
            output (signal/convolve2-nn original-input original-filter [1 1] [1 1] [1 1])
            output-dims (array/get-dims output)
            incoming-grad (array/create-array (float-array (repeat (reduce * output-dims) 1.0)) 
                                             output-dims defs/AF_DTYPE_F32)
            result (ml/data-gradient incoming-grad original-input original-filter output
                                    [1 1] [1 1] [1 1])]
        (is (instance? AFArray result))))))

(deftest test-bias-gradient
  (testing "bias-gradient computes bias gradient"
    (device/init!)
    (releasing!
      (let [original-input (array/create-array (float-array [1.0 0.0 0.0 1.0 
                                                             0.0 1.0 1.0 0.0]) 
                                              [2 2 2 1] defs/AF_DTYPE_F32)
            original-filter (array/create-array (float-array [1.0 0.0 0.0 1.0 
                                                              0.0 1.0 1.0 0.0 
                                                              1.0 0.0 0.0 1.0 
                                                              0.0 1.0 1.0 0.0]) 
                                               [2 2 2 2] defs/AF_DTYPE_F32)
            ;; Perform forward pass to get output
            output (signal/convolve2-nn original-input original-filter [1 1] [1 1] [1 1])
            output-dims (array/get-dims output)
            incoming-grad (array/create-array (float-array (repeat (reduce * output-dims) 1.0)) 
                                             output-dims defs/AF_DTYPE_F32)
            result (ml/bias-gradient incoming-grad original-input original-filter output
                                     [1 1] [1 1] [1 1])]
        (is (instance? AFArray result))))))

(deftest test-bias-gradient-with-params
  (testing "bias-gradient with custom stride, dilation, padding"
    (device/init!)
    (releasing!
      (let [original-input (array/create-array (float-array (repeat 16 1.0)) 
                                              [4 4 1 1] defs/AF_DTYPE_F32)
            original-filter (array/create-array (float-array (repeat 8 1.0)) 
                                               [2 2 1 2] defs/AF_DTYPE_F32)
            ;; Perform forward pass to get output
            output (signal/convolve2-nn original-input original-filter [1 1] [1 1] [1 1])
            output-dims (array/get-dims output)
            incoming-grad (array/create-array (float-array (repeat (reduce * output-dims) 1.0)) 
                                             output-dims defs/AF_DTYPE_F32)
            result (ml/bias-gradient incoming-grad original-input original-filter output
                                    [1 1] [1 1] [1 1])]
        (is (instance? AFArray result))))))

(deftest test-all-gradients
  (testing "all-gradients computes filter, data, and bias gradients together"
    (device/init!)
    (releasing!
      (let [original-input (array/create-array (float-array [1.0 0.0 0.0 1.0 
                                                             0.0 1.0 1.0 0.0]) 
                                              [2 2 2 1] defs/AF_DTYPE_F32)
            original-filter (array/create-array (float-array [1.0 0.0 0.0 1.0 
                                                              0.0 1.0 1.0 0.0 
                                                              1.0 0.0 0.0 1.0 
                                                              0.0 1.0 1.0 0.0]) 
                                               [2 2 2 2] defs/AF_DTYPE_F32)
            ;; Perform forward pass to get output
            output (signal/convolve2-nn original-input original-filter [1 1] [1 1] [1 1])
            output-dims (array/get-dims output)
            incoming-grad (array/create-array (float-array (repeat (reduce * output-dims) 1.0)) 
                                             output-dims defs/AF_DTYPE_F32)
            {:keys [filter data bias]} 
            (ml/all-gradients incoming-grad original-input original-filter output
                             [1 1] [1 1] [1 1])]
        (is (instance? AFArray filter))
        (is (instance? AFArray data))
        (is (instance? AFArray bias))))))

(deftest test-all-gradients-with-params
  (testing "all-gradients with custom stride, dilation, padding"
    (device/init!)
    (releasing!
      (let [original-input (array/create-array (float-array (repeat 16 1.0)) 
                                              [4 4 1 1] defs/AF_DTYPE_F32)
            original-filter (array/create-array (float-array [1.0 1.0 
                                                              1.0 1.0]) 
                                               [2 2 1 1] defs/AF_DTYPE_F32)
            ;; Perform forward pass to get output
            output (signal/convolve2-nn original-input original-filter [2 2] [1 1] [1 1])
            output-dims (array/get-dims output)
            incoming-grad (array/create-array (float-array (repeat (reduce * output-dims) 1.0)) 
                                             output-dims defs/AF_DTYPE_F32)
            {:keys [filter data bias]} 
            (ml/all-gradients incoming-grad original-input original-filter output
                             [2 2] [1 1] [1 1])]
        (is (instance? AFArray filter))
        (is (instance? AFArray data))
        (is (instance? AFArray bias))))))

;;;
;;; Integration Tests
;;;

(deftest test-gradient-consistency
  (testing "all-gradients produces same results as individual gradient functions"
    (device/init!)
    (releasing!
      (let [original-input (array/create-array (float-array [1.0 0.0 0.0 1.0 
                                                             0.0 1.0 1.0 0.0]) 
                                              [2 2 2 1] defs/AF_DTYPE_F32)
            original-filter (array/create-array (float-array [1.0 0.0 0.0 1.0 
                                                              0.0 1.0 1.0 0.0 
                                                              1.0 0.0 0.0 1.0 
                                                              0.0 1.0 1.0 0.0]) 
                                               [2 2 2 2] defs/AF_DTYPE_F32)
            ;; Perform forward pass to get output
            output (signal/convolve2-nn original-input original-filter [1 1] [1 1] [1 1])
            output-dims (array/get-dims output)
            incoming-grad (array/create-array (float-array (repeat (reduce * output-dims) 1.0)) 
                                             output-dims defs/AF_DTYPE_F32)
            filter-grad-single (ml/filter-gradient incoming-grad original-input original-filter output
                                                   [1 1] [1 1] [1 1])
            data-grad-single (ml/data-gradient incoming-grad original-input original-filter output
                                               [1 1] [1 1] [1 1])
            bias-grad-single (ml/bias-gradient incoming-grad original-input original-filter output
                                               [1 1] [1 1] [1 1])
            {:keys [filter data bias]} 
            (ml/all-gradients incoming-grad original-input original-filter output
                             [1 1] [1 1] [1 1])]
        ;; Verify all gradients are AFArray instances
        (is (instance? AFArray filter-grad-single))
        (is (instance? AFArray data-grad-single))
        (is (instance? AFArray bias-grad-single))
        (is (instance? AFArray filter))
        (is (instance? AFArray data))
        (is (instance? AFArray bias))
        ;; Verify dimensions match
        (is (= (array/get-dims filter-grad-single) (array/get-dims filter)))
        (is (= (array/get-dims data-grad-single) (array/get-dims data)))
        (is (= (array/get-dims bias-grad-single) (array/get-dims bias)))))))

(deftest test-gradient-shapes
  (testing "Gradients have expected shapes relative to inputs"
    (device/init!)
    (releasing!
      (let [;; 3x3 input, 2x2 filter
            original-input (array/create-array (float-array (repeat 9 1.0)) 
                                              [3 3 1 1] defs/AF_DTYPE_F32)
            original-filter (array/create-array (float-array [1.0 1.0 
                                                              1.0 1.0]) 
                                               [2 2 1 1] defs/AF_DTYPE_F32)
            ;; Perform forward pass to get output
            output (signal/convolve2-nn original-input original-filter [1 1] [1 1] [1 1])
            output-dims (array/get-dims output)
            incoming-grad (array/create-array (float-array (repeat (reduce * (take 2 output-dims)) 1.0)) 
                                             output-dims defs/AF_DTYPE_F32)
            filter-grad (ml/filter-gradient incoming-grad original-input original-filter output
                                            [1 1] [1 1] [1 1])
            data-grad (ml/data-gradient incoming-grad original-input original-filter output
                                        [1 1] [1 1] [1 1])
            bias-grad (ml/bias-gradient incoming-grad original-input original-filter output
                                        [1 1] [1 1] [1 1])]
        ;; Filter gradient should match filter shape
        (is (= [2 2 1 1] (array/get-dims filter-grad)))
        ;; Data gradient should match input shape
        (is (= [3 3 1 1] (array/get-dims data-grad)))
        ;; Bias gradient has specific shape
        (is (vector? (array/get-dims bias-grad)))))))

(comment
  ;; run all tests from REPL
  (run-tests)
  
  ;; run individual tests
  (run-test test-gradient-type-to-int-filter)
  (run-test test-gradient-type-to-int-data)
  (run-test test-gradient-type-to-int-bias)
  (run-test test-gradient-type-to-int-default)
  (run-test test-convolve2-gradient-nn-filter)
  (run-test test-convolve2-gradient-nn-data)
  (run-test test-convolve2-gradient-nn-bias)
  (run-test test-convolve2-gradient-nn-with-stride)
  (run-test test-convolve2-gradient-nn-with-padding)
  (run-test test-convolve2-gradient-nn-with-dilation)
  (run-test test-filter-gradient)
  (run-test test-filter-gradient-with-params)
  (run-test test-data-gradient)
  (run-test test-data-gradient-with-params)
  (run-test test-bias-gradient)
  (run-test test-bias-gradient-with-params)
  (run-test test-all-gradients)
  (run-test test-all-gradients-with-params)
  (run-test test-gradient-consistency)
  (run-test test-gradient-shapes))
