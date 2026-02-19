(ns org.soulspace.arrayfire.integration.base.definitions-test
  "Tests for org.soulspace.arrayfire.integration.base.definitions,
   including keyword→constant mappings and the resolve-backend helper."
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [org.soulspace.arrayfire.ffi.base.definitions :as ffi-defs]
            [org.soulspace.arrayfire.integration.base.definitions :as defs]))

;;;
;;; resolve-backend tests
;;;

(deftest resolve-backend-keyword-test
  (testing "Resolves backend keywords to constants"
    (is (= ffi-defs/AF_BACKEND_CPU     (defs/resolve-backend :cpu)))
    (is (= ffi-defs/AF_BACKEND_CUDA    (defs/resolve-backend :cuda)))
    (is (= ffi-defs/AF_BACKEND_OPENCL  (defs/resolve-backend :opencl)))
    (is (= ffi-defs/AF_BACKEND_ONEAPI  (defs/resolve-backend :oneapi)))
    (is (= ffi-defs/AF_BACKEND_DEFAULT (defs/resolve-backend :default)))))

(deftest resolve-backend-integer-test
  (testing "Passes integer constants through"
    (is (= 2 (defs/resolve-backend 2)))
    (is (= 4 (defs/resolve-backend 4)))))

(deftest resolve-backend-invalid-test
  (testing "Throws on invalid keyword"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Unknown backend"
          (defs/resolve-backend :invalid)))))
