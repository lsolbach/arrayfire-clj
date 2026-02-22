(ns org.soulspace.arrayfire.util.test)

(defn approx=
  "Compare expected/actual values within a tolerance."
  ([expected actual]
   (approx= expected actual 1e-6))
  ([expected actual tolerance]
   (<= (Math/abs (- (double expected) (double actual)))
       (double tolerance))))

(defn seq-approx=
  "Returns true when every corresponding pair of elements in flat sequences xs
   and ys is approximately equal within tol."
  ([xs ys]     (seq-approx= xs ys 1e-5))
  ([xs ys tol] (and (= (count xs) (count ys))
                    (every? true? (map #(approx= %1 %2 tol) xs ys)))))

