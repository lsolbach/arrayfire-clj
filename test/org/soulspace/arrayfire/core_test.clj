(ns org.soulspace.arrayfire.core-test
  (:require [clojure.test :refer [deftest is run-tests]]
            [org.soulspace.arrayfire.core :as af]))


(deftest simple-add
  (af/init!)
  (is (= :ok (af/info)))
  (let [a (af/create-array [1.0 2.0 3.0] [3])
        b (af/create-array [10.0 20.0 30.0] [3])
        c (af/add a b)
        out (af/to-host c 3)]
    (is (= [11.0 22.0 33.0] (seq out)))
    (af/release a)
    (af/release b)
    (af/release c)))

(comment
  ;; To run tests from REPL
  (run-tests)
  ;
  )