(ns org.soulspace.arrayfire.integration.ffi-test
  "Tests for FFI declarations - validates syntax without requiring ArrayFire library"
  (:require [clojure.test :refer [deftest is run-tests testing]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.integration.ffi :as ffi]))

(deftest test-ffi-syntax-validation
  (testing "FFI namespace can be loaded (syntax is valid)"
    ;; This test validates that the defcfn declarations are syntactically correct
    ;; It will fail to load if there are syntax errors in the FFI declarations
    (is (try
          ;; Try to load the namespace, catch library loading errors
          (require '[org.soulspace.arrayfire.ffi :as ffi])
          true ; If it succeeds without error, ArrayFire is installed
          (catch java.lang.UnsatisfiedLinkError e
            ;; Expected error when ArrayFire is not installed
            ;; This is actually SUCCESS for our test - syntax is valid
            (boolean (.contains (.getMessage e) "no af in java.library.path")))
          (catch clojure.lang.Compiler$CompilerException e
            ;; Compiler error means syntax problems in our FFI declarations
            (println "Compiler error (BAD - syntax issue):" (.getMessage e))
            false)
          (catch Exception e
            ;; Any other error might indicate issues
            (println "Unexpected error:" (.getMessage e))
            false))
        "FFI declarations should be syntactically valid (library loading error is OK)")))

(deftest test-type-specifications
  (testing "Type specifications use correct coffi keywords"
    ;; Validate that our type keywords are valid coffi types
    (is (= 4 (mem/size-of ::mem/int)) "int type should be 4 bytes")
    (is (= 8 (mem/size-of ::mem/long)) "long type should be 8 bytes")
    (is (= 8 (mem/size-of ::mem/double)) "double type should be 8 bytes")
    (is (some? (mem/size-of ::mem/pointer)) "pointer type should have a size")))

(deftest test-memory-operations
  (testing "Float read/write operations"
    (let [buf (mem/alloc 4)]
      (ffi/write-float! buf 0 3.14)
      (is (< (Math/abs (- 3.14 (ffi/read-float buf 0))) 0.001) "Float roundtrip")))
  
  (testing "Double read/write operations"
    (let [buf (mem/alloc 8)]
      (ffi/write-double! buf 0 2.71828)
      (is (< (Math/abs (- 2.71828 (ffi/read-double buf 0))) 0.00001) "Double roundtrip")))
  
  (testing "Int read/write operations"
    (let [buf (mem/alloc 4)]
      (ffi/write-int! buf 0 42)
      (is (= 42 (ffi/read-int buf 0)) "Int roundtrip")))
  
  (testing "Long read/write operations"
    (let [buf (mem/alloc 8)]
      (ffi/write-long! buf 0 123456789)
      (is (= 123456789 (ffi/read-long buf 0)) "Long roundtrip")))
  
  (testing "Short read/write operations"
    (let [buf (mem/alloc 2)]
      (ffi/write-short! buf 0 1000)
      (is (= 1000 (ffi/read-short buf 0)) "Short roundtrip")))
  
  (testing "Byte read/write operations"
    (let [buf (mem/alloc 1)]
      (ffi/write-byte! buf 0 127)
      (is (= 127 (ffi/read-byte buf 0)) "Byte roundtrip")))
  
  (testing "Complex float read/write operations"
    (let [buf (mem/alloc 8)]
      (ffi/write-complex-float! buf 0 [1.5 2.5])
      (let [[r i] (ffi/read-complex-float buf 0)]
        (is (< (Math/abs (- 1.5 r)) 0.001) "Complex float real part")
        (is (< (Math/abs (- 2.5 i)) 0.001) "Complex float imag part"))))
  
  (testing "Complex double read/write operations"
    (let [buf (mem/alloc 16)]
      (ffi/write-complex-double! buf 0 [3.14 2.71])
      (let [[r i] (ffi/read-complex-double buf 0)]
        (is (< (Math/abs (- 3.14 r)) 0.00001) "Complex double real part")
        (is (< (Math/abs (- 2.71 i)) 0.00001) "Complex double imag part")))))

(deftest test-dims-conversion
  (testing "dims->native converts dimensions correctly"
    (let [dims [2 3 4]
          native-dims (ffi/dims->native dims)]
      ;; We can't directly read the values without native interop
      ;; but we can verify it doesn't throw and returns a segment
      (is (some? native-dims) "dims->native returns a memory segment"))))

(deftest test-type-sizes
  (testing "Type sizes map has all ArrayFire dtypes"
    (is (= 4 (get ffi/type-sizes ffi/AF_DTYPE_F32)) "float32 is 4 bytes")
    (is (= 8 (get ffi/type-sizes ffi/AF_DTYPE_F64)) "float64 is 8 bytes")
    (is (= 8 (get ffi/type-sizes ffi/AF_DTYPE_C32)) "complex32 is 8 bytes")
    (is (= 16 (get ffi/type-sizes ffi/AF_DTYPE_C64)) "complex64 is 16 bytes")
    (is (= 1 (get ffi/type-sizes ffi/AF_DTYPE_B8)) "bool is 1 byte")
    (is (= 4 (get ffi/type-sizes ffi/AF_DTYPE_S32)) "int32 is 4 bytes")
    (is (= 4 (get ffi/type-sizes ffi/AF_DTYPE_U32)) "uint32 is 4 bytes")
    (is (= 8 (get ffi/type-sizes ffi/AF_DTYPE_S64)) "int64 is 8 bytes")
    (is (= 8 (get ffi/type-sizes ffi/AF_DTYPE_U64)) "uint64 is 8 bytes")
    (is (= 2 (get ffi/type-sizes ffi/AF_DTYPE_S16)) "int16 is 2 bytes")
    (is (= 2 (get ffi/type-sizes ffi/AF_DTYPE_U16)) "uint16 is 2 bytes")
    (is (= 1 (get ffi/type-sizes ffi/AF_DTYPE_U8)) "uint8 is 1 byte")))

(deftest test-arrayfire-dtype-constants
  (testing "ArrayFire dtype constants are defined"
    (is (= 0 ffi/AF_DTYPE_F32))
    (is (= 1 ffi/AF_DTYPE_C32))
    (is (= 2 ffi/AF_DTYPE_F64))
    (is (= 3 ffi/AF_DTYPE_C64))
    (is (= 4 ffi/AF_DTYPE_B8))
    (is (= 5 ffi/AF_DTYPE_S32))
    (is (= 6 ffi/AF_DTYPE_U32))
    (is (= 7 ffi/AF_DTYPE_U8))
    (is (= 8 ffi/AF_DTYPE_S64))
    (is (= 9 ffi/AF_DTYPE_U64))
    (is (= 10 ffi/AF_DTYPE_S16))
    (is (= 11 ffi/AF_DTYPE_U16))))

(comment
  ;; Manual test to check if ArrayFire is installed
  (require '[org.soulspace.arrayfire.ffi :as ffi])
  
  ;; This will show the actual error if library is not found
  (try
    (require '[org.soulspace.arrayfire.ffi :as ffi] :reload)
    (println "ArrayFire library loaded successfully!")
    (catch Exception e
      (println "Error loading ArrayFire:" (.getMessage e))))
  
  ;; Run tests
  (run-tests)
  )
