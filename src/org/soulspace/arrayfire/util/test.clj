(ns org.soulspace.arrayfire.util.test)

(defn approx=
  "Compare expected/actual values within a tolerance."
  ([expected actual]
   (approx= expected actual 1e-6))
  ([expected actual tolerance]
   (<= (Math/abs (- (double expected) (double actual)))
       (double tolerance))))

