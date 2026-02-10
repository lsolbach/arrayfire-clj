(ns introduction
  (:require [org.soulspace.arrayfire.integration.unified-api.device :as device]
            [org.soulspace.arrayfire.integration.base.jvm-integration :as jvm]
            [org.soulspace.arrayfire.integration.unified-api.array :as array]
            [coffi.mem :as mem]))

;; # Introduction to ArrayFire-CLJ
;; This notebook provides an introduction to using the [ArrayFire-CLJ](https://github.com/lsolbach/arrayfire-clj) library,
;; a Clojure wrapper for the [ArrayFire](https://github.com/arrayfire/arrayfire) tensor math library. We will cover the
;; basics of initializing the library, creating and manipulating arrays, and
;; managing resources.
;;
;; ## ArrayFire
;; [ArrayFire](https://github.com/arrayfire/arrayfire) is a general-purpose
;; tensor library that simplifies the software development process for the
;; parallel architectures found in CPUs, GPUs, and other hardware
;; acceleration devices.
;; 
;; ArrayFire provides:
;; * Hundreds of functions in the following categories
;;   * Array handling
;;   * Computer vision
;;   * Image processing
;;   * Linear algebra
;;   * Machine learning
;;   * Standard math
;;   * Signal Processing
;;   * Statistics
;;   * Vector algorithms
;; * Cross-platform compatibility with support for CUDA, oneAPI, OpenCL,
;;   and native CPU on Windows, Mac, and Linux
;;

;; ## ArrayFire-CLJ
;; [ArrayFire-CLJ](https://github.com/lsolbach/arrayfire-clj) is a Clojure
;; wrapper around the ArrayFire library, providing a more idiomatic Clojure
;; interface for working with ArrayFire. It abstracts away the complexities of
;; the underlying C API and allows you to work with ArrayFire arrays using
;; familiar Clojure data structures and functions.
;;
;; The library also includes integration tests to ensure that the core
;; functionality works correctly across different platforms and configurations.
;;
;; In this notebook, we will explore the core features of ArrayFire-CLJ and how
;; to use it effectively in your Clojure projects. We will cover device
;; initialization, array creation and manipulation, memory management,
;; and error handling. By the end of this notebook, you should have a solid
;; understanding of how to use ArrayFire-CLJ to perform high-performance
;; computations in Clojure.
;;
;; ## Initialization
;; Before using ArrayFire, we need to initialize the library and select a
;; device. The `device` namespace provides functions for this purpose.

(device/init!)

;; This initializes ArrayFire and selects the default device. You can also
;; specify a backend (e.g., CUDA, OpenCL) if you have multiple backends
;; available.
;;
;; ## Device Information
;; We can query information about the available devices and their capabilities.

(let [device-count (device/get-device-count)
      current-device (device/get-device)
      device-info (device/info-string)
      dbl-support (device/dbl-support?)
      half-support (device/half-support?)
      mem-info (device/device-mem-info)]
  (println "Device Count:" device-count)
  (println "Current Device ID:" current-device)
  (println "Device Info:\n" device-info)
  (println "Double Precision Support:" dbl-support)
  (println "Half Precision Support:" half-support)
  (println "Memory Info:" mem-info))

;; ## Array Creation and Manipulation
;; The `array` namespace provides functions for creating and manipulating
;; ArrayFire arrays. We can create arrays from Clojure data structures or
;; directly from memory.

