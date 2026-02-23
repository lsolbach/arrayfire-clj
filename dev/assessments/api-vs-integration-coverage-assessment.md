# Clojure API Layer Coverage of the Integration Unified API

**Date:** 2026-02-23  
**Scope:** All `org.soulspace.arrayfire.api.*` namespaces vs all
`org.soulspace.arrayfire.integration.unified-api.*` namespaces  

---

## Executive Summary

The Clojure API layer exposes **~342 public definitions** (340 functions + 2
macros) across 10 namespaces. The integration unified API layer contains **439
public functions** across 27 namespaces. Of these, **~330 integration functions
are covered** by an idiomatic API wrapper, yielding an overall function-level
coverage of **~75%**.

The uncovered **~109 functions** fall into two categories:

1. **Entirely uncovered namespaces (10 namespaces, ~98 functions):** Graphic,
   vision, events, features, CUDA, OpenCL, memory management, internal, error,
   and JIT test API — these have zero API-layer wrappers.
2. **Partially covered namespaces (2 namespaces, ~11 functions):** Random
   engine management and utility print/string functions.

The covered namespaces (algorithm, arith, array, blas, complex, data, device,
image, index, lapack, ml, moments, signal, sparse, statistics) have **near-100%
coverage** of their integration functions.

---

## Coverage Matrix

| Integration Namespace | Public Fns | API Namespace(s) | API Fns | Coverage | Status |
|---|---|---|---|---|---|
| `algorithm` | 30 | `api.core` | 28 | **93%** | Excellent |
| `arith` | 68 | `api.core` | 66 | **97%** | Excellent |
| `array` | 39 | `api.core` | 33 | **85%** | Very Good |
| `blas` | 11 | `api.core` + `api.linear-algebra` | 11 | **100%** | Complete |
| `complex` | 13 | `api.core` | 7 | **54%** | Partial |
| `data` | 25 | `api.core` | 25 | **100%** | Complete |
| `device` | 32 | `api.core` + `api.device` | 26 | **81%** | Good |
| `error` | 3 | — | 0 | **0%** | Not covered |
| `event` | 5 | — | 0 | **0%** | Not covered |
| `features` | 9 | — | 0 | **0%** | Not covered |
| `graphic` | 27 | `api.graphic` (empty) | 0 | **0%** | Not covered |
| `image` | 43 | `api.image-processing` | 43 | **100%** | Complete |
| `index` | 12 | `api.core` | 10 | **83%** | Good |
| `internal` | 8 | — | 0 | **0%** | Not covered |
| `jit_test_api` | 4 | — | 0 | **0%** | Not covered |
| `lapack` | 16 | `api.linear-algebra` | 16 | **100%** | Complete |
| `memory` | 15 | — | 0 | **0%** | Not covered |
| `ml` | 6 | `api.machine-learning` | 5 | **83%** | Good |
| `moments` | 6 | `api.image-processing` | 5 | **83%** | Good |
| `opencl` | 12 | — | 0 | **0%** | Not covered |
| `cuda` | 4 | — | 0 | **0%** | Not covered |
| `random` | 17 | `api.core` | 6 | **35%** | Partial |
| `signal` | 40 | `api.signal-processing` | 39 | **98%** | Excellent |
| `sparse` | 11 | `api.sparse` | 9 | **82%** | Good |
| `statistics` | 16 | `api.statistics` | 16 | **100%** | Complete |
| `util` | 7 | `api.core` + `api.io` | 5 | **71%** | Partial |
| `vision` | 11 | — | 0 | **0%** | Not covered |
| **Totals** | **439** | | **~330** | **~75%** | |

---

## Detailed Analysis by Category

### Fully Covered Integration Namespaces (~100%)

These integration namespaces have complete or near-complete API coverage:

#### `blas` → `api.core` + `api.linear-algebra` (100%)
All 11 functions covered: `gemm`, `matmul`, `dot`, `dot-all`, `transpose`,
`transpose!`, `matmul-nt`, `matmul-tn`, `matmul-tt`, `matmul3`, `matmul4`.
Note: The convenience variants (`matmul-nt/tn/tt`, `matmul3`, `matmul4`) are
available through `api.core/matmul` with keyword options and multi-arity calls.

#### `data` → `api.core` (100%)
All 25 functions covered with idiomatic names: `constant`, `identity-matrix`,
`range`, `iota`, `diagonal`/`get-diagonal`, `join`, `tile`, `reorder`, `shift`,
`reshape`, `flatten`, `flip`, `lower-tri`, `upper-tri`, `select-where`,
`replace-where!`, `pad`, etc.

#### `lapack` → `api.linear-algebra` (100%)
All 16 functions covered: `lu`, `lu!`, `qr`, `qr!`, `svd`, `svd!`, `cholesky`,
`cholesky!`, `det` (as `determinant`), `rank` (as `matrix-rank`), `norm`,
`inverse`, `pinverse` (as `pseudo-inverse`), `solve`, `solve-lu`,
`lapack-available?`.

#### `image` → `api.image-processing` (100%)
All 43 functions covered with idiomatic Clojure names. Notable renames: `sat` →
`summed-area-table`, `hist-equal` → `histogram-equalize`, `minfilt`/`maxfilt` →
`min-filter`/`max-filter`, `color-space` → `convert-color-space`.

#### `statistics` → `api.statistics` (100%)
All 16 functions covered. Notable renames: `var` → `variance`, `cov` →
`covariance`, `corrcoef` → `correlation`, `meanvar` → `mean-and-variance`.

#### `signal` → `api.signal-processing` (98%)
39 of 40 functions covered. `medfilt` (alias for `medfilt2`) is covered as
`median-filter2`; `medfilt1` is covered as `median-filter1`. The only uncovered
function is `medfilt` (which is redundant with `medfilt2`/`median-filter2`).

#### `arith` → `api.core` (97%)
66 of 68 functions covered. The two uncovered functions are `cplx2` and `arg`
which are duplicates of functions in the `complex` namespace (already exposed
via `api.core/complex` and `api.core/arg`).

#### `algorithm` → `api.core` (93%)
28 of 30 functions covered. Notable renames: `sort-index` → `argsort`,
`set-unique` → `unique`, `set-union` → `array-union`, `set-intersect` →
`array-intersect`, `all-true` → `all`, `any-true` → `any`, `count` →
`count-nonzero`. The `accum` function is covered by `cumsum` (with
`af-binary-add` as default).

#### `moments` → `api.image-processing` (83%)
5 of 6 functions covered: `moments`, `moments-all`, `centroid`, `area` (as
`image-area`). The utility function `moment-type->int` is not directly exposed
(used internally by `resolve-moment-type`).

### Well-Covered Namespaces (75%–90%)

#### `array` → `api.core` (85%)
33 of 39 public functions covered. The type-checking predicates (`floats?`,
`ints?`, `shorts?`, `doubles?`, `longs?`, `bytes?`) that check JVM array types
are not directly exposed in the API layer — these are internal helpers for the
integration layer. All ArrayFire array predicates (`empty?`, `scalar?`, `row?`,
`column?`, `vector?`, `complex?`, `real?`, etc.) are fully covered.

Uncovered: `create-handle` (low-level), `write-array!` (low-level write-back),
`get-data-ptr` (raw pointer access), `get-data-ref-count` (internal),
`get-allocated-bytes`/`allocated-bytes` (memory introspection), and JVM type
predicates.

#### `device` → `api.core` + `api.device` (81%)
26 of 32 functions covered. The mutation functions (`init!`, `set-device!`,
`set-backend!`, `sync!`, `set-manual-eval-flag!`, `get-manual-eval-flag`,
`set-kernel-cache-directory!`, `get-kernel-cache-directory`) are deliberately
used internally by the `with-arrayfire` macro rather than exposed publicly. This
is a design choice: device/backend mutation is scoped and safe.

Uncovered at API level but used internally: `ensure-af-init!`, `set-backend!`,
`set-device!`, `sync!`, `set-manual-eval-flag!`, `get-manual-eval-flag`.

Uncovered and not used: `info`, `info-string`, `print-mem-info!`,
`set-mem-step-size!`, `get-mem-step-size`, `set-kernel-cache-directory!`,
`get-kernel-cache-directory`.

#### `index` → `api.core` (83%)
10 of 12 functions covered via the `slice`, `at`, `row`, `col`, `rows`, `cols`,
`select`, `assoc-slice` functions. The low-level `make-seq` and
`set-seq-param-indexer!` are used internally but not directly exposed.

#### `sparse` → `api.sparse` (82%)
9 of 11 functions covered. Uncovered: `from-ptr` (create from raw memory
pointers — advanced use case) and `info` (could be a convenience wrapper, noted
in previous assessments as `sparse-info`).

#### `ml` → `api.machine-learning` (83%)
5 of 6 functions covered. The utility function `gradient-type->int` is not
directly exposed (used internally).

### Partially Covered Namespaces

#### `random` → `api.core` (35%)
6 of 17 functions covered: `randu` (via `random-uniform`), `randn` (via
`random-normal`), `random-uniform`, `random-normal`, `set-seed!` (as
`set-random-seed!`), `get-seed` (as `get-random-seed`).

The `with-random-engine` macro covers `create-engine` and `release-engine!`
internally.

**Uncovered (11 functions):**
- Engine management: `retain-engine`, `set-engine-type!`, `get-engine-type`,
  `set-engine-seed!`, `get-engine-seed`, `get-default-engine`,
  `set-default-engine-type!`
- Engine info: `engine-type-name`, `engine-info`

These are intentionally not exposed: engine lifecycle is managed by
`with-random-engine`, and direct engine mutation could break scope safety.

#### `complex` → `api.core` (54%)
7 of 13 functions covered: `cplx2` (as `complex`), `real`, `imag`, `conjg`,
`abs`, `arg`, `eq` (partial — via `core/eq` which handles complex arrays).

**Uncovered (6 functions):** `cplx`, `add`, `sub`, `mul`, `div`, `neq`.
The arithmetic functions (`add`, `sub`, `mul`, `div`) are redundant with the
corresponding `arith` functions that already handle complex arrays
transparently. `cplx` (single-arg real→complex promotion) could be a useful
addition.

#### `util` → `api.core` + `api.io` (71%)
5 of 7 functions covered: `print-array-gen` (in `api.core`), `save-array`,
`read-array-key`, `read-array-index`, `read-array-key-check` (all in `api.io`).

**Uncovered (2 functions):** `print-array` and `array-to-string`. The API
layer's `print-array` and `array->string` use a different implementation path
(via `->value`) rather than delegating to the ArrayFire C library's print/string
functions.

### Entirely Uncovered Namespaces (0%)

#### `graphic` (27 functions) — **Empty API file exists**
The `api.graphic` file exists with the namespace declaration but contains zero
functions. This is the largest uncovered namespace. Functions include window
management (`create-window`, `destroy-window!`, `show!`, etc.), plot drawing
(`draw-plot-2d!`, `draw-plot-3d!`, `draw-scatter-2d!`, etc.), and axis
configuration (`set-axes-limits-*!`, `set-axes-titles!`).

#### `vision` (11 functions)
Computer vision feature detectors and matchers: `fast`, `harris`, `susan`,
`orb`, `sift`, `gloh`, `hamming-matcher`, `nearest-neighbour`,
`match-template`, `dog`, `homography`.

#### `memory` (15 functions)
Device memory management: `alloc-pinned`, `free-pinned!`, `alloc-host`,
`free-host!`, `alloc-device`, `free-device!`, `device-mem-info`,
`print-mem-info`, `device-gc`, `set-mem-step-size!`, `get-mem-step-size`,
`lock-array!`, `unlock-array!`, `is-locked-array?`, `get-device-ptr`.

Note: `api.core` requires `unified-api.memory` as `uamem` but never uses it.

#### `features` (9 functions)
Feature object management: `create-features`, `retain-features`,
`release-features!`, `get-features-num`, `get-features-xpos`,
`get-features-ypos`, `get-features-score`, `get-features-orientation`,
`get-features-size`.

#### `event` (5 functions)
GPU event management: `create-event!`, `delete-event!`, `mark-event!`,
`enqueue-wait-event!`, `block-event!`.

#### `internal` (8 functions)
Low-level array internals: `create-strided-array`, `get-strides`, `get-offset`,
`is-linear?`, `is-owner?`, `get-raw-ptr`, `get-allocated-bytes`, `array-info`.

#### `cuda` (4 functions)
CUDA-specific functions: `get-stream`, `get-native-id`, `set-native-id!`,
`cublas-set-math-mode!`.

#### `opencl` (12 functions)
OpenCL-specific functions: `get-context`, `get-queue`, `get-device-id`,
`set-device-id!`, `add-device-context!`, `set-device-context!`,
`delete-device-context!`, `get-device-type`, `get-platform`,
`device-type-name`, `platform-name`, `device-info`.

#### `error` (3 functions)
Error reporting: `get-last-error`, `err-to-string`,
`set-enable-stacktrace!`.

#### `jit_test_api` (4 functions)
JIT testing: `get-max-jit-len`, `set-max-jit-len!`, `with-jit-len`,
`jit-info`. Note: `with-arrayfire` covers the `:manual-eval` flag but not JIT
length control.

---

## Architectural Assessment

### Intentional Omissions (by design)

Several uncovered namespaces are intentionally not in the API layer:

1. **Device mutation functions** — `set-device!`, `set-backend!`, `sync!` etc.
   are used internally by `with-arrayfire` to ensure scope safety. Exposing them
   would allow callers to leave the system in an inconsistent state.

2. **Random engine management** — The lifecycle is scoped by
   `with-random-engine`. Direct mutation (`set-engine-type!`, etc.) is
   intentionally hidden.

3. **Backend-specific APIs** (`cuda`, `opencl`) — These are intentionally not in
   the API layer since the project targets the *Unified* API. Backend-specific
   functions are available through the integration layer for advanced users.

4. **Low-level internals** (`internal`, `error`) — Raw pointer access, strided
   array creation, and error string functions are considered integration-layer
   concerns.

5. **Complex arithmetic duplicates** — `complex/add`, `complex/sub`, etc. are
   redundant with `arith/add`, `arith/sub` which handle complex types.

### Genuine Gaps

The following omissions represent genuine gaps in the API layer:

| Gap | Integration Namespace | Functions | Priority | Effort |
|---|---|---|---|---|
| **Graphic API** | `graphic` | 27 | High | Medium |
| **Vision API** | `vision` | 11 | Medium | Low-Medium |
| **Features API** | `features` | 9 | Medium | Low |
| **Memory management** | `memory` | 6-8 useful fns | Medium | Low |
| **Events API** | `event` | 5 | Low | Low |
| **JIT control** | `jit_test_api` | 2-3 useful fns | Low | Trivial |
| **`cplx` (real→complex)** | `complex` | 1 | Low | Trivial |
| **`sparse-info`** | `sparse` | 1 | Very low | Trivial |

---

## Recommendations

### Priority 1 — Implement `api.graphic` (High)

The graphic namespace has 27 integration functions and an empty API file already
exists. ArrayFire's graphic capabilities are useful for debugging and
visualization during development. This is the largest single gap.

Recommended approach: wrap all 27 functions with idiomatic Clojure naming,
keyword-based option maps for marker types and color maps, and a
`with-window` macro for scoped window lifecycle.

### Priority 2 — Implement `api.vision` (Medium)

Computer vision is a significant use case. The 11 functions cover feature
detection (FAST, Harris, SUSAN, ORB, SIFT, GLOH), feature matching
(hamming-matcher, nearest-neighbour), template matching, and homography
estimation. A new `api.vision` or `api.computer-vision` namespace would be
appropriate.

This depends on the `features` namespace for the feature object type, so both
should be implemented together.

### Priority 3 — Implement `api.features` or include in `api.vision` (Medium)

The `features` namespace provides the feature data type used by vision
functions. These could either be a separate `api.features` namespace or
integrated into the vision API with idiomatic accessors.

### Priority 4 — Expose selective memory introspection (Medium)

A few memory functions would be useful at the API level:
- `device-mem-info` (already in `api.device`, duplicated in memory)
- `print-mem-info` (debugging aid)
- `device-gc` (manual garbage collection trigger)
- `alloc-host`/`free-host!` (for advanced zero-copy workflows)

### Priority 5 — Minor additions (Low)

- `cplx` (real→complex promotion) in `api.core`
- `sparse-info` convenience in `api.sparse`
- JIT length control (`with-jit-len` or option on `with-arrayfire`)

---

## Summary Statistics

| Category | Namespaces | Functions | % of Total |
|---|---|---|---|
| Fully covered (≥90%) | 10 | ~294 | 67% |
| Well covered (75–89%) | 5 | ~36 | 8% |
| Partially covered (<75%) | 2 | ~11 | 3% |
| Not covered (0%) | 10 | ~98 | 22% |
| **Total** | **27** | **439** | **100%** |

The API layer provides excellent coverage of the core mathematical functionality
(arithmetic, linear algebra, signal processing, statistics, image processing).
The primary gaps are in visualization (graphic), computer vision (vision +
features), GPU-specific backends (CUDA/OpenCL), and low-level memory/event
management — areas that are either specialized use cases or intentionally
scoped to the integration layer by design.

---

## Methodology

This assessment was produced by:

1. Enumerating all public `defn`/`defmacro` forms in every file of both layers.
2. Tracing `require` aliases in all API namespaces to identify integration
   dependencies.
3. Searching for alias usage (`grep`) to confirm which integration functions are
   actually called from each API namespace.
4. Classifying uncovered functions as intentional omissions vs genuine gaps
   based on the architectural principles in AGENTS.md.
5. Cross-referencing with the existing `unified-api-completeness-assessment.md`.
