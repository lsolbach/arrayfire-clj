(ns org.soulspace.arrayfire.api.statistics
  "Idiomatic Clojure statistical API for ArrayFire arrays.

   Provides GPU-accelerated descriptive statistics, including:

   **Central tendency** — `mean`, `mean-all`, `mean-weighted`, `mean-all-weighted`,
                          `median`, `median-all`

   **Dispersion** — `variance`, `variance-all`, `variance-weighted`, `variance-all-weighted`,
                    `stdev`, `stdev-all`, `mean-and-variance`

   **Correlation / covariance** — `covariance`, `correlation`

   **Ranking** — `topk`

   ## Bias convention

   Variance and standard-deviation functions accept an optional `bias` keyword:
   - `:sample`     — Bessel-corrected, N−1 denominator (default, unbiased estimator)
   - `:population` — full-population N denominator

   ## Quick start

   ```clojure
   (require '[org.soulspace.arrayfire.api.core       :as af])
   (require '[org.soulspace.arrayfire.api.statistics :as stats])

   (af/with-arrayfire {:backend :cpu}
     (let [data (af/random-normal [1000] :f64)]
       {:mean   (stats/mean-all data)
        :stdev  (stats/stdev-all data)
        :median (stats/median-all data)}))
   ```

   All functions **must** be called inside a `with-arrayfire` region."
  (:require [org.soulspace.arrayfire.integration.base.definitions :as defs]
            [org.soulspace.arrayfire.integration.unified-api.statistics :as stats]
            [org.soulspace.arrayfire.api.core :refer [assert-within-arrayfire!]])
  (:import (org.soulspace.arrayfire.integration.base.resource AFArray)))

;;;
;;; Private helpers
;;;

(defn- resolve-bias!
  "Like resolve-bias but throws on unrecognised keyword."
  [bias-kw]
  (or (get defs/variance-bias-kw->const bias-kw)
      (throw (ex-info (str "Unknown variance bias keyword: " bias-kw
                           ". Use :sample, :population, or :default.")
                      {:bias bias-kw}))))

(defn- resolve-topk-order!
  "Resolve a topk order keyword to the ArrayFire integer constant.
   Accepts :max (default), :min, :stable, :stable-max, :stable-min."
  [order-kw]
  (or (get defs/topk-fn-kw->const order-kw)
      (throw (ex-info (str "Unknown topk order keyword: " order-kw
                           ". Use :max, :min, :stable, :stable-max, or :stable-min.")
                      {:order order-kw}))))

;;;
;;; Mean
;;;

(defn mean
  "Compute the arithmetic mean along a dimension.

   μ = (1/N) × Σ xᵢ

   Parameters:
   - arr  AFArray input
   - dim  (optional) dimension along which to reduce; default −1
          (ArrayFire interprets −1 as the first non-singleton dim)

   Returns: AFArray with reduced dimension.
   Requires an active `with-arrayfire` region.

   Examples:
   (mean arr)      ; → shape [1 cols …]
   (mean arr 0)    ; column means  → shape [1 cols]
   (mean arr 1)    ; row means     → shape [rows 1]"
  (^AFArray [^AFArray arr]
   (assert-within-arrayfire! "mean")
   (stats/mean arr))
  (^AFArray [^AFArray arr dim]
   (assert-within-arrayfire! "mean")
   (stats/mean arr (int dim))))

(defn mean-weighted
  "Compute a weighted arithmetic mean along a dimension.

   μw = Σ(wᵢ × xᵢ) / Σ wᵢ

   Parameters:
   - arr      AFArray input
   - weights  AFArray of weights (same shape as arr)
   - dim      (optional) dimension; default −1

   Returns: AFArray."
  (^AFArray [^AFArray arr ^AFArray weights]
   (assert-within-arrayfire! "mean-weighted")
   (stats/mean-weighted arr weights))
  (^AFArray [^AFArray arr ^AFArray weights dim]
   (assert-within-arrayfire! "mean-weighted")
   (stats/mean-weighted arr weights (int dim))))

(defn mean-all
  "Compute the mean of all elements, returning a scalar.

   Returns: double for real arrays; [real imag] for complex arrays.
   Requires an active `with-arrayfire` region.

   Example:
   (mean-all arr)  ; => 0.4973 (double)"
  [^AFArray arr]
  (assert-within-arrayfire! "mean-all")
  (stats/mean-all arr))

(defn mean-all-weighted
  "Compute the weighted mean of all elements, returning a scalar.

   Parameters:
   - arr      AFArray input
   - weights  AFArray of weights (same shape as arr)

   Returns: double for real; [real imag] for complex."
  [^AFArray arr ^AFArray weights]
  (assert-within-arrayfire! "mean-all-weighted")
  (stats/mean-all-weighted arr weights))

;;;
;;; Variance
;;;

(defn variance
  "Compute variance along a dimension.

   - `:sample`     (default) — Bessel-corrected, 1/(N−1) denominator
   - `:population`            — 1/N denominator

   Parameters:
   - arr   AFArray input
   - bias  (optional) keyword :sample | :population | :default; default :sample
   - dim   (optional) integer dimension; default −1

   Returns: AFArray.
   Requires an active `with-arrayfire` region."
  (^AFArray [^AFArray arr]
   (assert-within-arrayfire! "variance")
   (stats/var arr))
  (^AFArray [^AFArray arr bias]
   (assert-within-arrayfire! "variance")
   (stats/var arr (resolve-bias! bias)))
  (^AFArray [^AFArray arr bias dim]
   (assert-within-arrayfire! "variance")
   (stats/var arr (resolve-bias! bias) (int dim))))

(defn variance-weighted
  "Compute weighted variance along a dimension.

   σ²w = Σ wᵢ(xᵢ − μw)² / Σ wᵢ

   Parameters:
   - arr      AFArray input
   - weights  AFArray weights (same shape)
   - dim      (optional) integer dimension; default −1

   Returns: AFArray."
  (^AFArray [^AFArray arr ^AFArray weights]
   (assert-within-arrayfire! "variance-weighted")
   (stats/var-weighted arr weights))
  (^AFArray [^AFArray arr ^AFArray weights dim]
   (assert-within-arrayfire! "variance-weighted")
   (stats/var-weighted arr weights (int dim))))

(defn variance-all
  "Compute variance of all elements, returning a scalar.

   Parameters:
   - arr   AFArray input
   - bias  (optional) keyword :sample | :population | :default; default :sample

   Returns: double for real; [real imag] for complex.
   Requires an active `with-arrayfire` region."
  ([^AFArray arr]
   (assert-within-arrayfire! "variance-all")
   (stats/var-all arr))
  ([^AFArray arr bias]
   (assert-within-arrayfire! "variance-all")
   (stats/var-all arr (resolve-bias! bias))))

(defn variance-all-weighted
  "Compute weighted variance of all elements, returning a scalar.

   Parameters:
   - arr      AFArray input
   - weights  AFArray weights (same shape)

   Returns: double for real; [real imag] for complex."
  [^AFArray arr ^AFArray weights]
  (assert-within-arrayfire! "variance-all-weighted")
  (stats/var-all-weighted arr weights))

;;;
;;; Standard deviation
;;;

(defn stdev
  "Compute standard deviation along a dimension.

   σ = √variance

   Parameters:
   - arr   AFArray input
   - bias  (optional) keyword :sample | :population | :default; default :sample
   - dim   (optional) integer dimension; default −1

   Returns: AFArray.
   Requires an active `with-arrayfire` region."
  (^AFArray [^AFArray arr]
   (assert-within-arrayfire! "stdev")
   (stats/stdev arr))
  (^AFArray [^AFArray arr bias]
   (assert-within-arrayfire! "stdev")
   (stats/stdev arr (resolve-bias! bias)))
  (^AFArray [^AFArray arr bias dim]
   (assert-within-arrayfire! "stdev")
   (stats/stdev arr (resolve-bias! bias) (int dim))))

(defn stdev-all
  "Compute standard deviation of all elements, returning a scalar.

   Parameters:
   - arr   AFArray input
   - bias  (optional) keyword :sample | :population | :default; default :sample

   Returns: double for real; [real imag] for complex.
   Requires an active `with-arrayfire` region."
  ([^AFArray arr]
   (assert-within-arrayfire! "stdev-all")
   (stats/stdev-all arr))
  ([^AFArray arr bias]
   (assert-within-arrayfire! "stdev-all")
   (stats/stdev-all arr (resolve-bias! bias))))

;;;
;;; Median
;;;

(defn median
  "Compute median along a dimension (robust to outliers; O(n log n)).

   Parameters:
   - arr  AFArray input
   - dim  (optional) integer dimension; default −1

   Returns: AFArray.
   Requires an active `with-arrayfire` region."
  (^AFArray [^AFArray arr]
   (assert-within-arrayfire! "median")
   (stats/median arr))
  (^AFArray [^AFArray arr dim]
   (assert-within-arrayfire! "median")
   (stats/median arr (int dim))))

(defn median-all
  "Compute median of all elements, returning a scalar.

   Returns: double for real; [real imag] for complex.
   Requires an active `with-arrayfire` region."
  [^AFArray arr]
  (assert-within-arrayfire! "median-all")
  (stats/median-all arr))

;;;
;;; Combined mean + variance (single pass)
;;;

(defn mean-and-variance
  "Compute mean and variance in a single pass (more efficient than two calls).

   Parameters:
   - arr      AFArray input
   - weights  AFArray weights (same shape as arr)
   - bias     (optional) keyword :sample | :population | :default; default :sample
   - dim      (optional) integer dimension; default 0

   Returns: map `{:mean AFArray :var AFArray}`.
   Requires an active `with-arrayfire` region.

   Example:
   (let [{:keys [mean var]} (mean-and-variance data weights)]
     {:mean mean :stdev (af/sqrt var)})"
  ([^AFArray arr ^AFArray weights]
   (assert-within-arrayfire! "mean-and-variance")
   (stats/meanvar arr weights))
  ([^AFArray arr ^AFArray weights bias]
   (assert-within-arrayfire! "mean-and-variance")
   (stats/meanvar arr weights (resolve-bias! bias)))
  ([^AFArray arr ^AFArray weights bias dim]
   (assert-within-arrayfire! "mean-and-variance")
   (stats/meanvar arr weights (resolve-bias! bias) (int dim))))

;;;
;;; Covariance and correlation
;;;

(defn covariance
  "Compute covariance between two variables along axis 0.

   Cov(X,Y) = (1/(N−k)) × Σ(xᵢ − μₓ)(yᵢ − μᵧ)
   where k = 1 for `:sample` bias, k = 0 for `:population`.

   Parameters:
   - x     AFArray first variable
   - y     AFArray second variable (same shape as x)
   - bias  (optional) keyword :sample | :population | :default; default :sample

   Returns: AFArray representing the covariance matrix.
   Requires an active `with-arrayfire` region."
  (^AFArray [^AFArray x ^AFArray y]
   (assert-within-arrayfire! "covariance")
   (stats/cov x y))
  (^AFArray [^AFArray x ^AFArray y bias]
   (assert-within-arrayfire! "covariance")
   (stats/cov x y (resolve-bias! bias))))

(defn correlation
  "Compute the Pearson correlation coefficient between two variables.

   ρ(X,Y) = Cov(X,Y) / (σₓ × σᵧ) ∈ [−1, 1]

   Parameters:
   - x  AFArray first variable
   - y  AFArray second variable (same shape as x)

   Returns: double (real arrays) or [real imag] (complex arrays).
   Requires an active `with-arrayfire` region.

   Examples:
   (correlation x x)  ; => 1.0
   (correlation x y)  ; => ≈ 0.0 for independent series"
  [^AFArray x ^AFArray y]
  (assert-within-arrayfire! "correlation")
  (stats/corrcoef x y))

;;;
;;; Top-k selection
;;;

(defn topk
  "Find the k largest (or smallest) values along a dimension.

   O(n log k), much faster than full sort when k ≪ n.

   Parameters:
   - arr    AFArray input
   - k      integer — number of elements to select
   - dim    (optional) integer dimension; default −1
   - order  (optional) keyword controlling selection order:
            :max (default), :min, :stable, :stable-max, :stable-min

   Returns: `[values indices]` where both are AFArrays.
            `values`  — the k selected values (sorted)
            `indices` — their original positions (u32)

   Requires an active `with-arrayfire` region.

   Examples:
   (let [[vals idx] (topk scores 5)]
     {:top-values vals :positions idx})

   ;; Bottom-5 per column
   (let [[vmin imin] (topk data 5 0 :min)]
     vmin)"
  ([^AFArray arr k]
   (assert-within-arrayfire! "topk")
   (stats/topk arr (int k)))
  ([^AFArray arr k dim]
   (assert-within-arrayfire! "topk")
   (stats/topk arr (int k) (int dim)))
  ([^AFArray arr k dim order]
   (assert-within-arrayfire! "topk")
   (stats/topk arr (int k) (int dim) (resolve-topk-order! order))))

(comment
  ;; api.statistics REPL experiments
  (require '[org.soulspace.arrayfire.api.core       :as af]
           '[org.soulspace.arrayfire.api.statistics :as stat])

  ;; Basic scalars — 5-element vector [1 2 3 4 5]
  (af/with-arrayfire {:backend :cpu}
    (let [v (af/array [1.0 2.0 3.0 4.0 5.0] [5] :f64)]
      {:mean   (stat/mean-all v)          ; => 3.0
       :var    (stat/variance-all v)      ; => 2.5 (sample)
       :stdev  (stat/stdev-all v)         ; => 1.5811...
       :median (stat/median-all v)}))     ; => 3.0

  ;; Column-wise mean of a 3×2 matrix
  ;; col-major: [[1 4] [2 5] [3 6]] -> stored [1 2 3 4 5 6]
  (af/with-arrayfire {:backend :cpu}
    (let [m (af/array [1.0 2.0 3.0 4.0 5.0 6.0] [3 2] :f64)]
      (af/->value (stat/mean m 0))))     ; => [[2.0 5.0]]

  ;; Correlation: perfect positive
  (af/with-arrayfire {:backend :cpu}
    (let [x (af/array [1.0 2.0 3.0 4.0] [4] :f64)
          y (af/array [2.0 4.0 6.0 8.0] [4] :f64)]
      (stat/correlation x y)))           ; => 1.0

  ;; Top-3 from a 6-element vector
  (af/with-arrayfire {:backend :cpu}
    (let [v (af/array [3.0 1.0 4.0 1.0 5.0 9.0] [6] :f64)
          [vals idx] (stat/topk v 3)]
      {:values (af/->value vals)
       :indices (af/->value idx)}))

  )