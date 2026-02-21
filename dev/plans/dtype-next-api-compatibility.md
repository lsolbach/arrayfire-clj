# Plan: dtype-next API Compatibility for `arrayfire-clj` Core API

**Date:** 2026-02-20  
**Goal:** Make the array and arithmetic functions in `org.soulspace.arrayfire.api.core` call-compatible with `tech.v3.datatype` / `tech.v3.datatype.functional` wherever possible, treating dtype-next as the canonical naming convention.

---

## 1. Background & Motivation

`arrayfire-clj` wraps the GPU-accelerated ArrayFire library.  The public API in
`core.clj` is intended to be the primary entry point for end-users.  Many users of
this library will also use `dtype-next` for data manipulation on the JVM.  Making the
two APIs call-compatible means:

- Users can swap or mix backends with minimal cognitive overhead.
- Functions doing the same thing have the same name and arity.
- Where ArrayFire offers something dtype-next does not, the same naming conventions
  (`->`, `?`, `!`, kebab-case) still apply.

### Constraint

ArrayFire arrays (`AFArray`) must not outlive the enclosing `with-arrayfire` region.
All new functions must call `assert-within-arrayfire!` before manipulating `AFArray`
instances.

---

## 2. Reference Material

| Source | Location |
|---|---|
| dtype-next datatype API | `reference/dtype-next/src/tech/v3/datatype.clj` |
| dtype-next tensor API | `reference/dtype-next/src/tech/v3/tensor.clj` |
| dtype-next functional API | `reference/dtype-next/src/tech/v3/datatype/functional.clj` |
| arrayfire-clj public core | `src/org/soulspace/arrayfire/api/core.clj` |
| arrayfire-clj integration layer | `src/org/soulspace/arrayfire/integration/unified_api/` |

---

## 3. Gap Analysis

### 3.1 Array Inspection Functions (missing from `core.clj`)

These are present in the integration layer but not exposed in the public API.
dtype-next provides them as standard operations.

| dtype-next function | Maps to integration layer | Notes |
|---|---|---|
| `shape` | `array/get-dims` | Return persistent vector of dimensions |
| `ecount` | `array/get-elements` | Return total element count as long |
| `elemwise-datatype` | `array/get-type` + `defs/` mapping | Return dtype keyword (`:f64`, `:f32`, …) |
| `clone` | (`array/copy-array` or deep copy) | Return independent copy of the array |
| `cast` / `elemwise-cast` | integration-layer cast functions | Cast element type |
| `tensor?` | `instance? AFArray` check | Return true if AFArray |

### 3.2 Tensor Manipulation Functions (missing from `core.clj`)

| dtype-next / common name | Maps to integration layer | Notes |
|---|---|---|
| `reshape` | `data/moddims` | Change shape without copying data |
| `transpose` | `blas/transpose` | Reverse or permute dimensions |
| `broadcast` / `tile` | `data/tile` | Repeat array along dimensions |
| `flat` / `flatten` | `data/flat` | Collapse to 1D |
| `select` / `index` | `index/index-seq` etc. | Subrect selection |
| `rows` / `columns` | slicing along dim 0 / dim 1 | Return sequence of row/col tensors |

### 3.3 Arithmetic / Math — Naming Mismatches

| Current `core.clj` | dtype-next name | Action required |
|---|---|---|
| `sign` | `signum` | **Rename** to `signum`. Note: `af_sign` is the *sign bit* (0 or 1), not the mathematical signum (-1, 0, +1). The public API should expose the mathematical `signum` (wrap with a conditional: `(- 1 (* 2 (sign x)))`) **or** document clearly that ArrayFire's sign is sign-bit only and provide `signum` separately. |
| `sigmoid` | `logistic` | **Add alias** `logistic` pointing to `sigmoid`, or rename. |
| `inf?` | `infinite?` | **Add alias** `infinite?`. |
| `ne` | `not-eq` | **Add alias** `not-eq`. |
| `lt`, `le`, `gt`, `ge` | `<`, `<=`, `>`, `>=` | **Add short-form aliases** (requires updating `:refer-clojure :exclude`) and keep long names as backwards-compatible aliases. |
| `nan?` | `nan?` | ✅ Match — no change needed. |
| `abs` | `abs` | ✅ Match. |
| `sqrt`, `cbrt` | `sqrt`, `cbrt` | ✅ Match. |
| `pow` | `pow` | ✅ Match. |
| `exp`, `expm1` | `exp`, `expm1` | ✅ Match. |
| `log`, `log2`, `log10`, `log1p` | `log`, `log2`, `log10`, `log1p` | ✅ Match (dtype-next has all). |
| `floor`, `ceil`, `round` | `floor`, `ceil`, `round` | ✅ Match. |
| `rem`, `mod` | `rem` (only) | `mod` not in dtype-next functional; keep both. |
| `sin`, `cos`, `tan`, `asin`, `acos`, `atan` | same | ✅ Match. |
| `sinh`, `cosh`, `tanh`, `asinh`, `acosh`, `atanh` | same | ✅ Match. |
| `erf`, `erfc` | (not in dtype-next) | Keep, follow same conventions. |
| `tgamma`, `lgamma`, `factorial` | (not in dtype-next) | Keep, following conventions. |
| `arg` | (not in dtype-next) | Keep with clear docstring. |
| `neg` | (not in dype-next — `(- x)` negates) | Keep `neg` as a named function, it is idiomatic. |
| `rsqrt`, `pow2` | (not in dtype-next) | Keep — ArrayFire-specific. |
| `trunc` | (not in dtype-next) | Keep. |

### 3.4 Math Functions Missing from `core.clj`

These exist in dtype-next's functional API but are not currently in `core.clj`.
They either map to existing integration layer functions or can be composed.

| dtype-next function | Description | Integration-layer mapping / composition |
|---|---|---|
| `signum` | Mathematical sign: -1, 0, +1 | Compose: `(- (* 2 (ge x 0)) 1)` clamped, or use `af_sign` + transform |
| `sq` | Element-wise x² | `(arith/pow x (scalar->array 2.0 …) true)` or `(arith/mul x x)` |
| `rint` | Round to nearest int, return float | `arith/round` (same semantics in ArrayFire) |
| `hypot` | sqrt(x² + y²) | `(sqrt (+ (sq x) (sq y)))` or direct compose |
| `finite?` | Element-wise finite check (¬NaN ∧ ¬Inf) | `(arith/and (arith/not (nan? x)) (arith/not (inf? x)))` |
| `infinite?` | Element-wise infinity check | Alias of existing `inf?` |
| `logistic` | Same as `sigmoid` | Alias |
| `not-eq` | Same as `ne` — element-wise ≠ | Alias |

### 3.5 Reduction Functions (missing from `core.clj`)

The integration layer has these; they need to be exposed in the public API
with dtype-next-compatible names.

| dtype-next name | Integration-layer function | Notes |
|---|---|---|
| `sum` | `algo/sum`, `algo/sum-nan` | scalar sum over entire array |
| `min` | `algo/min` | element-wise min or reduce to scalar |
| `max` | `algo/max` | element-wise max or reduce to scalar |
| `mean` | `stats/mean-all` | scalar mean |
| `variance` | `stats/var-all` | scalar variance |
| `standard-deviation` | `stats/stdev-all` | scalar std dev |
| `median` | `stats/median-all` | scalar median |
| `reduce-+` | loop over `algo/sum` | reduce with + |
| `reduce-*` | loop over `algo/product` | reduce with * |
| `cumsum` | `algo/accum` | cumulative sum |
| `cummax`, `cummin` | `algo/scan` with max/min ops | cumulative max/min |

---

## 4. Implementation Plan

Work in the following order (bottom-of-file first where replacing forms,
but ordered here conceptually).

### Step 1 — Update `ns` form to extend `:refer-clojure :exclude`

Add the comparison operator symbols to the exclusion list so they can be
defined in `core.clj` with dtype-next-compatible names.

```clojure
(:refer-clojure :exclude [+ - * / abs mod rem < <= > >= min max])
```

Rationale: dtype-next uses `<`, `<=`, `>`, `>=`, `min`, `max` as public API names.
Shadowing these in the arrayfire-clj core namespace allows the same idioms.

### Step 2 — Add array inspection functions

Add the following to the `"Array inspection"` section (create new section after
`"Helper functions"`):

#### 2a. `shape`
```clojure
(defn shape
  "Return the effective shape of an AFArray as a persistent vector of dimensions.
   Trailing size-1 dimensions are stripped (effective logical shape).
   Compatible with tech.v3.datatype/shape.

   Example:
   (shape arr) ; => [3 4] for a 3×4 array"
  [^AFArray arr]
  (assert-within-arrayfire! "shape")
  (let [dims (array/get-dims arr)]
    (vec (drop-while #(= 1 %) (reverse dims)))))    ; strip trailing 1s
```

#### 2b. `ecount`
```clojure
(defn ecount
  "Return the total number of elements in an AFArray as a long.
   Compatible with tech.v3.datatype/ecount.

   Example:
   (ecount arr) ; => 12 for a [3 4] array"
  ^long [^AFArray arr]
  (assert-within-arrayfire! "ecount")
  (long (array/get-elements arr)))
```

#### 2c. `elemwise-datatype`
```clojure
(defn elemwise-datatype
  "Return the element datatype keyword of an AFArray (e.g. :f64, :f32, :s32, …).
   Compatible with tech.v3.datatype/elemwise-datatype.

   Example:
   (elemwise-datatype arr) ; => :f64"
  [^AFArray arr]
  (assert-within-arrayfire! "elemwise-datatype")
  (get defs/af-type->dtype-kw (array/get-type arr)))
```
_(Requires a reverse lookup map `af-type->dtype-kw` in the `defs` namespace if not
already present — see Step 2g.)_

#### 2d. `tensor?`
```clojure
(defn tensor?
  "Return true if obj is an AFArray (an arrayfire-clj tensor).
   Compatible with tech.v3.tensor/tensor?"
  [obj]
  (instance? AFArray obj))
```

#### 2e. `clone`
```clojure
(defn clone
  "Return an independent copy of an AFArray.
   Compatible with tech.v3.datatype/clone.

   Example:
   (clone arr) ; => new AFArray with the same values"
  ^AFArray [^AFArray arr]
  (assert-within-arrayfire! "clone")
  (array/copy-array arr))
```

#### 2f. `cast` / `elemwise-cast`
```clojure
(defn elemwise-cast
  "Cast an AFArray to a new element dtype.
   Compatible with tech.v3.datatype/elemwise-cast.

   Parameters:
   - arr: AFArray
   - new-dtype: keyword (:f32, :f64, :s32, …)

   Example:
   (elemwise-cast arr :f32) ; cast to float32"
  ^AFArray [^AFArray arr new-dtype]
  (assert-within-arrayfire! "elemwise-cast")
  (arith/cast arr (defs/resolve-dtype new-dtype)))

(def cast
  "Alias of elemwise-cast for dtype-next compatibility."
  elemwise-cast)
```

#### 2g. Ensure reverse dtype lookup map in `defs` namespace

Check `integration.base.definitions` for a map from ArrayFire integer type constant
to keyword.  If absent, add:
```clojure
(def af-type->dtype-kw
  "Reverse lookup: ArrayFire integer dtype constant → keyword."
  (clojure.set/map-invert dtype-kw->const))
```

### Step 3 — Add tensor manipulation functions

New section `"Tensor manipulation"` in `core.clj`.

#### 3a. `reshape`
```clojure
(defn reshape
  "Reshape an AFArray into a new shape without copying data (when possible).
   Compatible with tech.v3.tensor/reshape.

   Parameters:
   - arr: AFArray
   - new-shape: vector of new dimensions

   Example:
   (reshape arr [4 3]) ; reshape a [3 4] array to [4 3]"
  ^AFArray [^AFArray arr new-shape]
  (assert-within-arrayfire! "reshape")
  (data/moddims arr (normalize-dims new-shape)))
```

#### 3b. `transpose`
```clojure
(defn transpose
  "Transpose (conjugate-transpose for complex) an AFArray.
   Compatible with tech.v3.tensor/transpose.

   Parameters:
   - arr: AFArray (2D, or reordered via reorder-indexes for ND)
   - conjugate?: optional boolean, default false

   Example:
   (transpose arr)        ; standard transpose of a 2D matrix
   (transpose arr true)   ; conjugate transpose of a complex matrix"
  (^AFArray [^AFArray arr]
   (transpose arr false))
  (^AFArray [^AFArray arr conjugate?]
   (assert-within-arrayfire! "transpose")
   (blas/transpose arr conjugate?)))
```
_(Requires requiring `org.soulspace.arrayfire.integration.unified-api.blas :as blas`.)_

#### 3c. `broadcast` / `tile`
```clojure
(defn tile
  "Tile (repeat) an AFArray along each dimension.
   ArrayFire's equivalent to dtype-next's broadcast.

   Parameters:
   - arr: AFArray
   - dims: vector of repetition counts per dimension

   Example:
   (tile arr [2 3]) ; repeat the array 2 times along dim0, 3 times along dim1"
  ^AFArray [^AFArray arr dims]
  (assert-within-arrayfire! "tile")
  (data/tile arr (normalize-dims dims)))

(def broadcast
  "Alias of tile for dtype-next compatibility."
  tile)
```

#### 3d. `flat` / `flatten`
```clojure
(defn flat
  "Flatten an AFArray to a 1D array.
   Compatible with tech.v3.tensor slice-all-dims conceptually.

   Example:
   (flat arr) ; => 1D AFArray"
  ^AFArray [^AFArray arr]
  (assert-within-arrayfire! "flat")
  (data/flat arr))
```

### Step 4 — Fix `sign` → `signum` semantic gap

ArrayFire's `af_sign` returns the **sign bit** (0 = non-negative, 1 = negative),
not the mathematical signum (-1, 0, +1).

**Action:**

1. Rename the current `sign` wrapper to `sign-bit` with a clear docstring explaining
   the sign-bit semantics.
2. Add `signum` function that computes the mathematical sign:
   ```clojure
   (defn signum
     "Element-wise mathematical signum: -1 for negative, 0 for zero, +1 for positive.
      Compatible with tech.v3.datatype.functional/signum.

      This is NOT the same as ArrayFire's af_sign (which is the sign bit: 1 if
      negative, 0 otherwise). Use sign-bit for that.

      Example:
      (signum (create-array [-3.0 0.0 5.0] [3])) ; => [-1.0 0.0 1.0]"
     [^AFArray a]
     (assert-within-arrayfire! "signum")
     ;; signum(x) = (x > 0) - (x < 0)  element-wise
     (arith/sub (arith/gt a (scalar->array 0.0 (array/get-type a)))
                (arith/lt a (scalar->array 0.0 (array/get-type a)))))
   ```

### Step 5 — Add comparison operators with dtype-next names

Add the short-form comparison operators alongside the existing `lt`, `le`,
`gt`, `ge` (keep those as aliases for backward compatibility).  Update the
`ns` `:refer-clojure :exclude` list (Step 1).

```clojure
;; Short-form aliases (dtype-next compatible)
(def < "Element-wise less-than. See lt." lt)
(def <= "Element-wise less-than-or-equal. See le." le)
(def > "Element-wise greater-than. See gt." gt)
(def >= "Element-wise greater-than-or-equal. See ge." ge)
```

_Note: `eq` and `ne`/`not-eq` require separate treatment (see Step 6)._

### Step 6 — Add `not-eq`, `infinite?`, `logistic` aliases

```clojure
(def not-eq
  "Element-wise inequality test. Alias of ne.
   Compatible with tech.v3.datatype.functional/not-eq."
  ne)

(def infinite?
  "Element-wise infinity check. Alias of inf?.
   Compatible with tech.v3.datatype.functional/infinite?"
  inf?)

(def logistic
  "Element-wise logistic (sigmoid) function: 1 / (1 + exp(-x)).
   Alias of sigmoid. Compatible with tech.v3.datatype.functional/logistic."
  sigmoid)
```

### Step 7 — Add missing math functions

#### 7a. `sq` (x²)

Note: The current `pow2` means 2^x (two raised to x), **not** x squared.
`sq` in dtype-next means x² (x squared).  These are **different functions**.
Both should exist.

```clojure
(defn sq
  "Element-wise square: x².
   Compatible with tech.v3.datatype.functional/sq.

   Note: This is NOT pow2 (2^x). See pow2 for the two-to-the-power-of-x function.

   Example:
   (sq (create-array [2.0 3.0 4.0] [3])) ; => [4.0 9.0 16.0]"
  [^AFArray a]
  (assert-within-arrayfire! "sq")
  (arith/mul a a))
```

#### 7b. `rint` (round to integer value, returning float)

```clojure
(defn rint
  "Element-wise round to nearest integer value, returning a float-typed array.
   Equivalent to calling round and keeping the same float dtype.
   Compatible with tech.v3.datatype.functional/rint.

   Example:
   (rint (create-array [1.4 1.5 -1.5] [3])) ; => [1.0 2.0 -2.0]"
  [^AFArray a]
  (assert-within-arrayfire! "rint")
  (arith/round a))
```

#### 7c. `hypot`

```clojure
(defn hypot
  "Element-wise hypotenuse: sqrt(x² + y²).
   Supports scalar broadcasting.
   Compatible with tech.v3.datatype.functional/hypot.

   Example:
   (hypot (create-array [3.0] [1]) (create-array [4.0] [1])) ; => [5.0]"
  [x y]
  (assert-within-arrayfire! "hypot")
  (sqrt (+ (sq x) (sq y))))
```

#### 7d. `finite?`

```clojure
(defn finite?
  "Element-wise finite check: true (1) where element is neither NaN nor infinite.
   Returns a boolean array (b8).
   Compatible with tech.v3.datatype.functional/finite?

   Example:
   (finite? (create-array [1.0 Double/NaN Double/POSITIVE_INFINITY] [3])) ; => [1 0 0]"
  [^AFArray a]
  (assert-within-arrayfire! "finite?")
  (arith/and (arith/not (nan? a)) (arith/not (inf? a))))
```
_(Check whether `arith/and` and `arith/not` exist in the integration layer;
if not, use the equivalent bitwise or logical operations.)_

### Step 8 — Add reduction functions

New section `"Reductions"` in `core.clj`, requiring
`org.soulspace.arrayfire.integration.unified-api.statistics :as stats` (add to `:require`).

Functions to implement with dtype-next-compatible signatures:

| Function | Behaviour | Integration mapping |
|---|---|---|
| `sum` | Scalar sum over all elements (or along an axis) | `algo/sum` (reduce) or `stats/...` |
| `min` | Element-wise min of two arrays, or scalar min over array | `algo/min` |
| `max` | Element-wise max of two arrays, or scalar max over array | `algo/max` |
| `mean` | Scalar mean | `stats/mean-all` |
| `variance` | Scalar variance | `stats/var-all` |
| `standard-deviation` | Scalar std deviation | `stats/stdev-all` |
| `median` | Scalar median | `stats/median-all` |
| `cumsum` | Cumulative sum | `algo/accum` with add |

**Important design note for `min` and `max`:**  
dtype-next's `min`/`max` can act as both element-wise binary (two arrays → array)
and full-reduce unary (one array → scalar).  The arrayfire-clj versions should
mirror this:

```clojure
(defn min
  "Element-wise minimum of two arrays, or scalar minimum of one array.
   Compatible with tech.v3.datatype.functional/min.

   Arities:
   - (arr)       → scalar minimum across all elements
   - (arr1 arr2) → element-wise minimum of two AFArrays
   - (arr scalar) → element-wise minimum with scalar broadcast"
  ([^AFArray a]
   (assert-within-arrayfire! "min")
   (algo/min-all a))    ; returns scalar
  ([lhs rhs]
   (assert-within-arrayfire! "min")
   (algo/min-elementwise lhs rhs)))  ; element-wise — check integration API name
```

_(Look up exact function names in `integration/unified_api/algorithm.clj`.)_

### Step 9 — Update `(:refer-clojure :exclude ...)` in `ns` form

After Steps 1–8, the final exclusion list will be:

```clojure
(:refer-clojure :exclude [+ - * / abs mod rem < <= > >= min max])
```

### Step 10 — Update `ns` docstring

Update the namespace docstring to mention:
- dtype-next API compatibility
- Which functions have the same name as dtype-next
- Notable differences (e.g. `signum` vs `sign-bit`, column-major layout, GPU execution region requirement)

---

## 5. Function Name Mapping Reference Table

After all changes, the mapping from dtype-next names to arrayfire-clj should be:

### Array Inspection
| dtype-next | arrayfire-clj | Status |
|---|---|---|
| `shape` | `shape` | New |
| `ecount` | `ecount` | New |
| `elemwise-datatype` | `elemwise-datatype` | New |
| `clone` | `clone` | New |
| `cast` | `cast` / `elemwise-cast` | New |
| `tensor?` | `tensor?` | New |
| `reshape` | `reshape` | New |
| `transpose` | `transpose` | New |
| `broadcast` | `broadcast` / `tile` | New |
| `select` | `select` / `index` | Future (complex — depends on index API) |
| `rows` | `rows` | Future |
| `columns` | `columns` | Future |

### Math / Functional
| dtype-next | arrayfire-clj | Status |
|---|---|---|
| `+` `-` `*` `/` | `+` `-` `*` `/` | ✅ Exists |
| `abs` | `abs` | ✅ Exists |
| `sqrt` `cbrt` | `sqrt` `cbrt` | ✅ Exists |
| `pow` | `pow` | ✅ Exists |
| `exp` `expm1` | `exp` `expm1` | ✅ Exists |
| `log` `log2` `log10` `log1p` | same | ✅ Exists |
| `floor` `ceil` `round` `rem` `mod` | same | ✅ Exists |
| `sin` `cos` `tan` `asin` `acos` `atan` `atan2` | same | ✅ Exists |
| `sinh` `cosh` `tanh` `asinh` `acosh` `atanh` | same | ✅ Exists |
| `nan?` | `nan?` | ✅ Exists |
| `eq` | `eq` | ✅ Exists |
| `<` `<=` `>` `>=` | `<` `<=` `>` `>=` (+ `lt` `le` `gt` `ge`) | Step 5 |
| `signum` | `signum` (+ `sign-bit`) | Step 4 |
| `logistic` | `logistic` (alias `sigmoid`) | Step 6 |
| `infinite?` | `infinite?` (alias `inf?`) | Step 6 |
| `not-eq` | `not-eq` (alias `ne`) | Step 6 |
| `finite?` | `finite?` | Step 7d |
| `sq` | `sq` | Step 7a |
| `rint` | `rint` | Step 7b |
| `hypot` | `hypot` | Step 7c |
| `sum` | `sum` | Step 8 |
| `min` `max` | `min` `max` | Step 8 |
| `mean` | `mean` | Step 8 |
| `variance` | `variance` | Step 8 |
| `standard-deviation` | `standard-deviation` | Step 8 |
| `median` | `median` | Step 8 |
| `cumsum` | `cumsum` | Step 8 |
| `neg` | `neg` | ✅ Exists (not in dtype-next, but follows convention) |

### ArrayFire-specific (no dtype-next equivalent — keep with clear docs)
| arrayfire-clj | Description |
|---|---|
| `rsqrt` | 1/sqrt(x) — GPU-optimized reciprocal square root |
| `pow2` | 2^x — two to the power of x |
| `sign-bit` | ArrayFire sign bit (0=non-negative, 1=negative) |
| `erf` `erfc` | Error functions |
| `tgamma` `lgamma` `factorial` | Gamma functions |
| `arg` | Complex phase angle |
| `trunc` | Truncate toward zero |
| `sigmoid` | Logistic function (also aliased as `logistic`) |

---

## 6. Required Integration Layer Checks

Before implementing, verify the following in the integration layer:

1. **`defs/af-type->dtype-kw`** — reverse dtype map from int constant to keyword. If absent, add to `integration/base/definitions.clj`.
2. **`arith/and`, `arith/not`** — logical element-wise operations for `finite?`. Check `integration/unified_api/arith.clj`.
3. **`algo/min-all`, `algo/max-all`** — scalar reductions. In `algorithm.clj`, the current `algo/min` and `algo/max` may return arrays, not scalars — check signatures.
4. **`blas/transpose`** — confirm it is already in `integration/unified_api/blas.clj` (it is, from our grep).
5. **`array/copy-array`** — confirm signature and whether it returns a new independent `AFArray`.
6. **`defs/resolve-dtype`** — already used in `core.clj`, confirm it handles all dtype keywords.

---

## 7. `ns` and `:require` Changes

```clojure
(ns org.soulspace.arrayfire.api.core
  ...
  (:refer-clojure :exclude [+ - * / abs mod rem < <= > >= min max])
  (:require ...
            [org.soulspace.arrayfire.integration.unified-api.statistics :as stats]
            [org.soulspace.arrayfire.integration.unified-api.blas :as blas]
            ...)
  ...)
```

---

## 8. Testing

### Unit test additions (`test/org/soulspace/arrayfire/api/core_test.clj`)

For each new function, add a `deftest` that:

1. Wraps the call in `(with-arrayfire {:backend :cpu} ...)`.
2. Verifies the return value matches the expected result.
3. For alias functions, verifies both the alias and the original return equivalent results.
4. For `signum`, tests positive, negative, and zero inputs.
5. For `sq`, explicitly verifies it is x² (not 2^x), since `pow2` is also in scope.

Use `clojure.test/is` and `clojure.test/deftest`.  For floating point results,
use a tolerance check helper or `(Math/abs (- result expected))`.

### REPL-driven testing (during development)

Add a `(comment ...)` Rich Comment Form at the bottom of `core.clj` with
runnable examples for each new function.  Test each incrementally in the
REPL before committing.

---

## 9. Documentation

1. Update the `ns` docstring to list dtype-next-compatible functions.
2. Add a `## dtype-next Compatibility` section to `README.md`.
3. Docstrings should note `Compatible with tech.v3.datatype[.functional]/fn-name`
   where applicable.
4. For functions that diverge semantically (e.g. `signum` vs ArrayFire's `sign`),
   add explicit `:note` or warning in the docstring.

---

## 10. Suggested Implementation Order

Work in this order to keep the file compilable after each step:

1. Step 9 (ns `:refer-clojure :exclude` update) — do this first to unblock aliases.
2. Step 2g (check/add `af-type->dtype-kw` in `defs`).
3. Step 2 (array inspection functions).
4. Step 3 (tensor manipulation — `reshape`, `transpose`, `tile`/`broadcast`, `flat`).
5. Step 4 (`signum` + rename `sign` → `sign-bit`).
6. Step 5 (comparison operator short-form aliases: `<`, `<=`, `>`, `>=`).
7. Step 6 (aliases: `not-eq`, `infinite?`, `logistic`).
8. Step 7 (new math functions: `sq`, `rint`, `hypot`, `finite?`).
9. Step 8 (reduction functions: `sum`, `min`, `max`, `mean`, etc.).
10. Update tests.
11. Update documentation.

After each step, evaluate affected namespace in the REPL and run the tests:
```clojure
(require '[org.soulspace.arrayfire.api.core-test :as t] :reload)
(clojure.test/run-tests 'org.soulspace.arrayfire.api.core-test)
```

---

## 11. Open Questions and Design Decisions

1. **`sign` rename backward compatibility:** Rename to `sign-bit` breaks existing
   callers of `sign`.  Consider keeping `sign` as a deprecated alias that emits
   a warning and calls `sign-bit`.

2. **Element-wise `min`/`max` vs scalar reduce `min`/`max`:** dtype-next uses the
   same name for both.  ArrayFire's `af_minof` is element-wise (array+array→array)
   and `af_min` is reduction (array→scalar).  The overloaded arity approach (0-arg
   scalar, 1-arg reduce, 2-arg element-wise) should work cleanly in Clojure.

3. **`select` / indexing API:** dtype-next's `select` is a rich function.  The
   ArrayFire index API is complex.  This is deferred to a separate task.

4. **Complex types:** `sq`, `hypot`, `finite?` behaviour on complex arrays needs
   to be verified against the ArrayFire C API to ensure no unexpected type errors.

5. **Broadcasting semantics:** ArrayFire's broadcasting via `tile` repeats data,
   while dtype-next `broadcast` creates a view without copying.  Document this
   difference clearly.

6. **`->tensor` alias:** dtype-next's primary "create from data" function is
   `->tensor`.  Adding `->tensor` as an alias for `create-array` could cause
   confusion because dtype-next `->tensor` accepts nested Clojure collections
   (row-major), while ArrayFire arrays are column-major.  Defer or add with
   explicit docs warning about column-major layout.
