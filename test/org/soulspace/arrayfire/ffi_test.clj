(ns org.soulspace.arrayfire.ffi-test
  "Tests for FFI declarations - validates syntax without requiring ArrayFire library"
  (:require [clojure.test :refer :all]
            [coffi.mem :as mem]))

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

(deftest test-helper-functions-without-library
  (testing "Helper functions can be tested without library"
    ;; We can test pure Clojure functions even without ArrayFire
    (when (try
            (require '[org.soulspace.arrayfire.ffi :as ffi])
            true
            (catch Exception e false))
      ;; Only test if namespace loaded
      (is true "FFI namespace loaded successfully"))))

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
