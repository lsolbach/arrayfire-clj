# ArrayFire Unified API Completeness Assessment

**Date:** 2026-02-23 (third revision)  
**Scope:** All `org.soulspace.arrayfire.api.*` namespaces  
**Reference:** `dev/arrayfire-unified-cpp-api-catalog.md`, the ArrayFire Unified C++ API, and the previous assessment (second revision, 2026-02-22)

---

## Executive Summary

All gaps identified in the second revision have been resolved with the exception
of two minor omissions. The entire idiomatic Clojure API surface is now
implemented and production-ready.

The most notable advances since the second revision:

- **`api.io`** — formerly a stub, now fully implemented (6 functions including
  `save-array-map`, `load-array-at`, and `array-key-index`).
- **`api.device`** — formerly a near-stub, now fully implemented with all
  read-only query functions including per-array identity and lock-state queries.
- **`api.statistics`** — formerly a stub, now fully implemented (16 functions).
- **`api.signal-processing`** — formerly a stub, now fully implemented
  (39 functions covering all FFT variants, convolutions, filters, and
  interpolation).
- **`api.image-processing`** — formerly a stub, now fully implemented
  (44 functions covering all image operations and moments).
- **`api.machine-learning`** — formerly a stub, now fully implemented
  (5 backpropagation gradient functions).
- **`api.core`** — `with-random-engine` macro implemented; `:manual-eval` option
  added to `with-arrayfire`. Gap H (random engine) and Gap O (JIT control) closed.

| Namespace | Functions exposed | Estimated coverage | Status |
|---|---|---|---|
| `api.core` | ~185 | ~93% core + `:manual-eval` + `with-random-engine` | Excellent — one minor gap remains |
| `api.linear-algebra` | 21 | ~100% of BLAS + LAPACK | Complete |
| `api.sparse` | 9 | ~90% of sparse creation/access | Complete — `sparse-info` convenience missing |
| `api.device` | 9 | 100% read-only query surface | Complete |
| `api.io` | 6 | 100% of array persistence API | Complete |
| `api.statistics` | 16 | 100% of statistics API | Complete |
| `api.signal-processing` | 39 | 100% of signal processing API | Complete |
| `api.image-processing` | 44 | ~100% of image processing API | Complete |
| `api.machine-learning` | 5 | 100% of CNN backprop API | Complete |

---

## 1. `api.core` — Updated Completeness

### 1.1 Gaps closed since the second revision

| Previous gap | Resolution |
|---|---|
| H. Random engine API | `with-random-engine` macro implemented. `random-uniform` and `random-normal` auto-dispatch to the bound engine via `*random-engine*` dynamic var. |
| J. `topk` | Moved to `api.statistics/topk` (correct fit — returns `[values indices]` pair alongside other ranking operations). |
| O. JIT control flags | `:manual-eval` boolean option added to `with-arrayfire`. Saves/restores the flag in a `finally` block. |

### 1.2 `with-arrayfire` — Current option set

| Option | Purpose | Status |
|---|---|---|
| `:backend` | Backend switching (`:cpu`, `:cuda`, `:opencl`, `:oneapi`) | ✅ Implemented |
| `:device` | Active device switching (integer index) | ✅ Implemented |
| `:converter-fn` | Result conversion function (default `->native-buffer`) | ✅ Implemented |
| `:arena-type` | Arena scope type (`:confined` / `:shared`) | ✅ Implemented |
| `:manual-eval` | Enable manual JIT evaluation mode | ✅ Implemented (gap O) |
| `:random-seed` | Set global random seed for duration of body | ❌ Not yet |

The `:random-seed` option remains the only missing `with-arrayfire` option.
Its absence is partially mitigated by:
- `set-random-seed!` / `get-random-seed` available in `api.core` for explicit control.
- `with-random-engine` for reproducible per-scope random streams.

**Proposed addition** (trivial effort):

```clojure
;; In the macro expansion, before the body:
(when random-seed
  (random/set-seed! random-seed))
;; In the finally block:
(when random-seed
  (random/set-seed! prev-seed))
```

**Priority:** Low — workarounds are ergonomic; this is a convenience shortcut only.

### 1.3 `with-random-engine` macro

```clojure
(with-random-engine {:type :philox :seed 42}
  (random-uniform [100 100] :f32))
```

The macro:
1. Creates a random engine via `random/create-engine` (resolving `:philox`,
   `:threefry`, `:mersenne` keywords).
2. Binds the handle to `*random-engine*` for the duration of the body.
3. Releases the engine in a `finally` block.
4. `random-uniform` and `random-normal` inspect `*random-engine*` at call-time:
   non-nil → engine-qualified integration functions; nil → default engine (`randu`/`randn`).

This is a clean, resource-safe design that models the same scoped lifecycle as
`with-arrayfire` itself.

### 1.4 Well-covered areas (unchanged from second revision)

All previously documented complete areas remain complete:

- Array creation (`array`, `constant`, `constant-complex`, `zeros`, `ones`, `range`, `iota`, `random-uniform`, `random-normal`, and all `*-like` variants)
- Array properties (`shape`, `raw-shape`, `size`, `ndim`, `element-count`, `datatype`)
- Array predicates (all 13 `*-array?` predicates)
- Slicing and indexing (`slice`, `at`, `row`, `col`, `rows`, `cols`, `select`, `assoc-slice`)
- Array manipulation (`reshape`, `flatten`, `transpose`, `join`, `flip`, `shift`, `tile`, `reorder`, `get-diagonal`, `lower-tri`, `upper-tri`, `pad`, `select-where`, `replace-where!`)
- Full arithmetic, math, trig, hyperbolic, and special functions surface
- Complex number operations (`complex`, `real-part`, `imag-part`, `conjg`, `arg`, `complex-array?`, `constant-complex`)
- Comparison and logical operations
- Bitwise operations (including `bitshiftl`, `bitshiftr`)
- Reductions (all standard reductions, NaN-safe variants, by-key variants, `argmin`, `argmax`)
- Finite differences (`diff1`, `diff2`)
- Sorting and set operations
- Matrix multiply and dot
- Sparse predicates (`sparse-array?`)
- Device introspection (`device-count`, `available-backends`, `backend-device-info`)
- JIT evaluation (`eval!`, `eval-multiple!`)
- Debugging utilities (`print-array`, `array->string`, `print-array-gen`)
- Random seed management (`set-random-seed!`, `get-random-seed`)

---

## 2. `api.device` — Complete

The `api.device` namespace is now fully implemented with 9 read-only query
functions:

| Function | Purpose | Status |
|---|---|---|
| `current-device` | Current active device integer ID | ✅ |
| `active-backend` | Current active backend as keyword | ✅ |
| `backend-count` | Number of available backends | ✅ |
| `double-available?` | `:f64` support query (optional device-id) | ✅ |
| `half-available?` | `:f16` support query (optional device-id) | ✅ |
| `device-mem-info` | Memory usage map (alloc/lock bytes+buffers) | ✅ |
| `array-backend` | Backend keyword of a specific AFArray | ✅ |
| `array-device` | Device integer ID of a specific AFArray | ✅ |
| `locked-array?` | Whether an AFArray's memory is currently locked | ✅ |

The design holds firm: all mutation (`set-device!`, `set-backend!`) remains in
the integration layer, accessed exclusively via the `with-arrayfire` macro. This
prevents callers from leaving the device/backend in wrong state on error.

---

## 3. `api.io` — Complete

`api.io` is now fully implemented with 6 functions:

| Function | Purpose | Status |
|---|---|---|
| `save-array` | Save array to `.af` file by key (optional append) | ✅ |
| `save-array-map` | Save a map of `{string-key → AFArray}` pairs to a file | ✅ |
| `load-array` | Load array from `.af` file by string key | ✅ |
| `load-array-at` | Load array from `.af` file by integer index (O(1)) | ✅ |
| `array-key-index` | Return the integer index of a named array, or nil | ✅ |
| `array-exists?` | Boolean check for named array existence | ✅ |

The public API goes slightly beyond the integration layer's 4 primitives,
adding `save-array-map` (bulk-save convenience) and `array-key-index`
(index-key bridge for `load-array-at` fast access).

---

## 4. `api.sparse` — Complete (minor omission)

The 9 core functions remain complete. The one low-priority omission from the
second revision is still absent:

| Gap | Status | Priority |
|---|---|---|
| `sparse-info` — convenience map `{:values :row-indices :col-indices :nnz :storage}` | ❌ Not yet | Very low |

`sparse-info` is a pure convenience wrapper; all its constituent data is
obtainable via the existing inspection functions. No integration layer changes
are needed; it would be a 7-line API form:

```clojure
(defn sparse-info [^AFArray sp]
  (assert-within-arrayfire! "sparse-info")
  {:values      (sparse-values sp)
   :row-indices (sparse-row-indices sp)
   :col-indices (sparse-col-indices sp)
   :nnz         (sparse-nnz sp)
   :storage     (sparse-storage sp)})
```

---

## 5. `api.statistics` — Complete

All 16 functions are implemented:

| Function | Purpose | Status |
|---|---|---|
| `mean` | Arithmetic mean along dim | ✅ |
| `mean-weighted` | Weighted mean along dim | ✅ |
| `mean-all` | Mean of all elements → scalar | ✅ |
| `mean-all-weighted` | Weighted mean of all elements → scalar | ✅ |
| `variance` | Variance along dim with `:sample`/`:population` bias | ✅ |
| `variance-weighted` | Weighted variance along dim | ✅ |
| `variance-all` | Variance of all elements → scalar | ✅ |
| `variance-all-weighted` | Weighted variance of all elements → scalar | ✅ |
| `stdev` | Standard deviation along dim | ✅ |
| `stdev-all` | Standard deviation of all elements → scalar | ✅ |
| `median` | Median along dim | ✅ |
| `median-all` | Median of all elements → scalar | ✅ |
| `mean-and-variance` | Single-pass mean+variance → `{:mean :var}` map | ✅ |
| `covariance` | Covariance matrix between two variables | ✅ |
| `correlation` | Pearson correlation coefficient → scalar | ✅ |
| `topk` | Top-k values and indices along dim → `[values indices]` | ✅ |

Bias resolution uses a private helper `resolve-bias!` that maps `:sample`,
`:population`, and `:default` keywords to ArrayFire integer constants and throws
a clear error on unknown keywords. `topk` similarly uses `resolve-topk-order!`
for `:max`, `:min`, `:stable`, `:stable-max`, `:stable-min`.

---

## 6. `api.signal-processing` — Complete

All 39 functions are implemented, covering the full ArrayFire signal processing
surface:

### FFT — Out-of-place (12 functions)
`fft`, `fft2`, `fft3`, `ifft`, `ifft2`, `ifft3`,
`fft-normalized`, `ifft-normalized`, `fft2-normalized`, `ifft2-normalized`,
`fft3-normalized`, `ifft3-normalized`

### FFT — Real/complex specialized (6 functions)
`fft-r2c`, `fft2-r2c`, `fft3-r2c`, `fft-c2r`, `fft2-c2r`, `fft3-c2r`

### FFT — In-place (6 functions)
`fft!`, `ifft!`, `fft2!`, `ifft2!`, `fft3!`, `ifft3!`

### Convolution (7 functions)
`convolve1`, `convolve2`, `convolve3`, `convolve2-sep`, `convolve2-nn`,
`fft-convolve1`, `fft-convolve2`, `fft-convolve3`

### Filtering (3 functions)
`iir`, `median-filter1`, `median-filter2`

### Interpolation (4 functions)
`approx1`, `approx1-uniform`, `approx2`, `approx2-uniform`

Keyword tables in the namespace docstring document convolution modes
(`:default`, `:expand`), convolution domains (`:auto`, `:spatial`, `:freq`),
10 interpolation methods, and 4 edge padding modes.

The Quantum Fourier Transform (QFT) use case is fully supported:
`fft`, `ifft`, and their normalized/r2c/c2r variants all work on complex arrays
and are accessible from a single `require`.

---

## 7. `api.image-processing` — Complete

All 44 functions are implemented:

### Edge and gradient detection (3)
`gradient`, `sobel`, `canny`

### Image I/O (4)
`load-image`, `save-image`, `load-image-native`, `save-image-native`

### Geometric transformations (7)
`resize`, `rotate`, `translate`, `scale`, `skew`, `transform`,
`transform-coordinates`

### Morphological operations (4)
`dilate`, `dilate3`, `erode`, `erode3`

### Filtering (6)
`bilateral`, `mean-shift`, `min-filter`, `max-filter`,
`gaussian-kernel`, `anisotropic-diffusion`

### Histogram operations (2)
`histogram`, `histogram-equalize`

### Color space conversions (7)
`rgb->gray`, `gray->rgb`, `rgb->hsv`, `hsv->rgb`, `rgb->ycbcr`, `ycbcr->rgb`,
`convert-color-space`

### Connected components and segmentation (2)
`regions`, `confidence-connected`

### Patch operations (2)
`unwrap`, `wrap`

### Integral image (1)
`summed-area-table`

### Deconvolution (2)
`iterative-deconv`, `inverse-deconv`

### Image moments (4)
`moments`, `moments-all`, `centroid`, `image-area`

The previous assessment proposed `moments` and `moments-all` as candidates
from `integration.unified-api.moments`. Both are implemented, along with the
derived convenience functions `centroid` and `image-area`. This makes the
moments surface fully available at the API layer.

---

## 8. `api.machine-learning` — Complete

All 5 backpropagation gradient functions are implemented:

| Function | Purpose | Status |
|---|---|---|
| `convolve2-gradient` | Generic gradient dispatcher (`:filter`/`:data`/`:bias`) | ✅ |
| `filter-gradient` | ∂L/∂W — filter/weight update gradient | ✅ |
| `data-gradient` | ∂L/∂X — backprop gradient for preceding layer | ✅ |
| `bias-gradient` | ∂L/∂b — bias update gradient | ✅ |
| `all-gradients` | All three gradients at once → `{:filter :data :bias}` map | ✅ |

The namespace docstring documents the CNN training workflow end-to-end (forward
pass via `api.signal-processing/convolve2-nn`, backward pass via this namespace)
with dimension conventions for signal arrays, filter arrays, and the
stride/padding/dilation parameter vectors.

---

## 9. `api.linear-algebra` — Unchanged

`api.linear-algebra` remains complete against the ArrayFire BLAS and LAPACK
unified APIs. No new gaps have been identified.

---

## 10. Complex Number Support — Unchanged

All complex number operations remain complete:

| Function | Status |
|---|---|
| `complex` | ✅ real+imag → complex array |
| `real-part` | ✅ |
| `imag-part` | ✅ |
| `conjg` | ✅ |
| `arg` | ✅ phase angle |
| `abs` | ✅ returns modulus for complex |
| `complex-array?` | ✅ |
| `constant-complex` | ✅ |

The quantum computing use case (complex array creation, arithmetic, linear
algebra, and the `constant-complex` initialiser) is fully supported.

---

## 11. Remaining Gaps — Summary

Only two very minor omissions remain across the entire API surface:

### Gap 1 — `:random-seed` option on `with-arrayfire` (priority: low)

A `:random-seed` key on the `with-arrayfire` options map would set the global
random seed before the body and restore it in `finally`. The functionality is
already available through explicit `set-random-seed!` / `get-random-seed` calls
and through `with-random-engine`.

**Effort:** ~15 lines of macro code.

### Gap 2 — `sparse-info` convenience function (priority: very low)

A single function returning all sparse metadata as a Clojure map. All
constituent data is obtainable via existing inspection functions.

**Effort:** ~7 lines.

---

## 12. Prioritised Action Plan (Updated)

The previous 8-item action plan is now fully executed. Only two trivial items
remain:

### Priority 1 — Add `:random-seed` to `with-arrayfire` (effort: trivial)

Adds ergonomic seed management without requiring explicit `set-random-seed!`
calls. Consistent with the `:manual-eval` pattern added in this cycle.

### Priority 2 — Add `sparse-info` to `api.sparse` (effort: trivial)

Pure convenience; the integration layer and all constituent API functions already
exist.

---

## 13. Methodology

This assessment was produced by:

1. Reading all 9 `api.*` source files in full.
2. Enumerating all `defn` / `defmacro` / `def` forms in each namespace via grep.
3. Cross-referencing with the second revision to identify newly closed gaps.
4. Inspecting the `with-arrayfire` macro source to enumerate all supported
   option keys (`known-opts` set: `:backend :device :converter-fn :arena-type :manual-eval`).
5. Inspecting the `with-random-engine` macro implementation for gap H.
6. Verifying that `topk` was moved to `api.statistics` (gap J resolution).
7. Confirming `:manual-eval` save/restore implementation in `with-arrayfire` (gap O).
