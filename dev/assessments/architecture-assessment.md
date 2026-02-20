# Architecture Assessment: Layered Design Conformance

**Date:** 2026-02-20  
**Branch:** main  
**Scope:** Full codebase against the layered design specification in [AGENTS.md](../AGENTS.md)

---

## Executive Summary

The three-layer architecture described in AGENTS.md (FFI → Integration → Clojure API) is clearly
visible in the directory structure and is largely respected in the dependency graph. The FFI and
Integration layers are in good shape. The API layer shows the most gaps: secondary API namespaces
are stubs, Clojure Spec is absent, and several concerns that belong to the Integration layer have
leaked into `api/core.clj`.

---

## Layer-by-Layer Findings

### 1. FFI Layer (`ffi/`)

**Goal (AGENTS.md):** Raw coffi bindings, true to the C API, plus library loading and constant
definitions. No higher-level abstractions.

#### 1.1 `ffi/base/loader.clj`
- Loads the `af` library via `coffi.ffi/load-system-library`. Library name is configurable via the
  `ARRAYFIRE_LIB` environment variable — good practice.
- No dependencies on any other `arrayfire-clj` namespace — correctly foundational.
- **Status: ✅ Conformant**

#### 1.2 `ffi/base/definitions.clj`
- Defines all ArrayFire C-level constants and enums (`af_err`, `af_dtype`, `af_source`,
  interpolation modes, convolution modes, etc.) as bare `def` forms with numeric values.
- Includes the `dtype->size` lookup map — still a raw data structure, acceptable at this level.
- **Status: ✅ Conformant**

#### 1.3 `ffi/c_api/` (~100 files)
- Each file corresponds to a C API module (`array.clj`, `complex.clj`, `unary.clj`, `binary.clj`,
  …) and exposes `defcfn` forms from coffi, mapping directly to named C functions (`af_create_array`,
  `af_release_array`, etc.).
- The type signatures use raw coffi types (`::mem/pointer`, `::mem/int`, `::mem/long`), which is
  appropriate for this layer.
- Every `defcfn` file requires `ffi.base.loader` to guarantee the library is loaded before binding
  creation — correct ordering.
- Docstrings document C parameter names and return-code semantics, making the C reference docs
  directly applicable.
- **Status: ✅ Conformant**

#### 1.4 FFI Layer Gaps
| Gap | Severity |
|-----|----------|
| No typedef aliases for `af_array` (opaque pointer), `af_features`, `af_window`, etc. | Minor — the TODO already lists this. Without named types, downstream code must use generic `::mem/pointer` everywhere, reducing self-documentation. |
| `half` (`f16`) dtype is present in the constants but its FFI size is not defined in `dtype->size`. | Minor |

---

### 2. Integration Layer (`integration/`)

**Goal (AGENTS.md):** Bridge FFI → JVM ecosystem: resource management, error handling, Arena-based
memory, dtype-next interop. Structured to mirror the ArrayFire Unified API.

#### 2.1 `integration/base/definitions.clj`
- Maps C integer constants to idiomatic Clojure keywords (`:f32`, `:cpu`, `:err-no-mem`, …) and
  provides reverse lookups.
- `resolve-dtype` converts a keyword to the raw constant with a clear error message.
- Depends only on `ffi.base.definitions` — correct layering.
- **Status: ✅ Conformant**

#### 2.2 `integration/base/error.clj`
- Provides a single `check!` function that throws `ex-info` on non-zero return codes.
- Very minimal — appropriately simple.
- **Status: ✅ Conformant**

#### 2.3 `integration/base/memory.clj`
- Defines `*af-arena*` dynamic var — **this is the right place** for it according to AGENTS.md.
- Provides `open-arena`, typed read/write helpers (`write-float!`, `read-double`, …), and
  conversion utilities (`dims->segment`, `c-string->string`, java-array converters).
- Provides `null-ptr` constant for optional FFI params.
- **Status: ✅ Conformant**

#### 2.4 `integration/base/resource.clj`
- Defines `AFArray` (record implementing `AutoCloseable`) with `Cleaner`-based automatic release.
- `af-release-array!` and `af-retain-array!` wrap the FFI functions with `check!`.
- Uses `tech.v3.resource` for stack-based lifecycle management.
- `native-af-array-pointer` allocates the out-pointer needed by FFI functions.
- **Note:** `native-af-array-pointer/0` creates an `Arena/ofAuto` arena internally when no arena is
  passed. This is correct as a fallback, but the `*af-arena*` from `memory.clj` is not consulted
  here — allocation bypasses the configured arena. The TODO already flags this inconsistency.
- **Status: ✅ Mostly conformant, minor arena inconsistency**

#### 2.5 `integration/unified_api/` (~25 files)
- Files follow the ArrayFire Unified API module grouping (`array`, `arith`, `blas`, `complex`,
  `data`, `device`, `error`, `features`, `image`, `lapack`, `memory`, `ml`, `moments`, `opencl`,
  `random`, `signal`, `sparse`, `statistics`, `util`, `vision`, …).
- Each function wraps the corresponding `ffi/c_api` call with:
  1. `check!` (error handling),
  2. `res/native-af-array-pointer` (out-pointer allocation),
  3. `res/af-array-new` / `res/deref-af-array` (resource registration).
- Naming follows Clojure conventions (`init!`, `within-arrayfire?`, `get-device`, `cplx2`, …).
- Complex number support (`cplx`, `cplx2`, `real`, `imag`, …) is implemented end-to-end from FFI
  to integration.
- `dtype_next/dtype_next.clj` provides zero-copy conversion between ArrayFire arrays and
  dtype-next native buffers, including datatype mapping tables and both directions
  (`create-array-from-native`, `to-native-buffer`).
- **Status: ✅ Conformant — this is the strongest part of the codebase**

#### 2.6 Integration Layer Gaps
| Gap | Severity |
|-----|----------|
| `native-af-array-pointer/0` does not use `*af-arena*` | Moderate — allocation escapes the configured arena scope |
| No Spec or validation at the integration layer boundary | Minor — AGENTS.md mandates invariant enforcement here |
| `integration/unified_api/error.clj` exists but the base `check!` in `integration/base/error.clj` does not look up rich error messages from it | Minor — `check!` only records the error code, not the ArrayFire descriptive string |
| `half` / `f16` dtype missing from integration datatype maps | Minor |

---

### 3. Clojure API Layer (`api/`)

**Goal (AGENTS.md):** Idiomatic Clojure API with keyword arguments, Clojure-style naming, Spec
validation, and assertion that all calls happen within a `with-arrayfire` region.

#### 3.1 `api/core.clj` (1892 lines)
This is the largest file in the project. It covers:
- `with-arrayfire` macro and `within-arrayfire?` / `assert-within-arrayfire!`
- `to-host`, `result-convert`, `->native-buffer`, `->value`
- Backend/device management helpers
- Array creation, arithmetic, algorithm, data, random functions
- Spec definitions (currently absent — see below)

**Positive observations:**
- `with-arrayfire` correctly establishes Arena scope, tech.resource scope, ArrayFire init,
  backend/device switching (serialized via a lock), and converts all `AFArray` results to host
  data before returning.
- `result-convert` recursively walks the return value (map, vector, set, seq, scalar) — good for
  composeable return types.
- `->native-buffer` and `->value` give the user a choice between dtype-next native buffers and
  pure Clojure values.
- `within-arrayfire?` / `assert-within-arrayfire!` provide the guard mechanism mandated by
  AGENTS.md.

**Issues:**

| Issue | Severity |
|-------|----------|
| **No Clojure Spec definitions** — AGENTS.md explicitly requires Spec for API contracts; none exist anywhere in the codebase. | High |
| `assert-within-arrayfire!` is defined but **not called** inside most API functions in `core.clj`. The guard mechanism is present but not enforced. | High |
| **Arena/resource scope logic resides in the API layer** (`with-arrayfire` macro) instead of being provided by the Integration layer. The TODO acknowledges this with "move Arena handling mechanism from core to integration layer". | Moderate |
| **`to-host`, `result-convert`, `->native-buffer`, `->value`** are in the API layer but are closer to Integration layer concerns (converting AFArray → host, dtype-next interop). | Moderate |
| `core.clj` is monolithic at ~1892 lines; it should be split into focused sub-namespaces matching the Unified API groupings (arith, data, random, algorithm, etc.). | Moderate |
| **`backend-lock`** (a lock object) is marked `public` and documented as "referenced by the `with-arrayfire` macro expansion from other namespaces" — this is an implementation detail leaking into the public API surface. | Minor |

#### 3.2 Secondary API Files

| File | State |
|------|-------|
| `api/image_processing.clj` | **Stub** — namespace declaration and docstring only, no functions. |
| `api/linear_algebra.clj` | **Stub** — namespace declaration and docstring only, no functions. |
| `api/machine_learning.clj` | **Stub** — namespace declaration and docstring only, no functions. |
| `api/signal_processing.clj` | **Stub** — namespace declaration and docstring only, no functions. |
| `api/statistics.clj` | **Stub** — namespace declaration and docstring only, no functions. |

All five secondary namespaces have detailed module docstrings describing their intended scope but
contain zero function implementations. The Integration layer counterparts (`unified_api/image.clj`,
`unified_api/lapack.clj`, `unified_api/ml.clj`, `unified_api/signal.clj`,
`unified_api/statistics.clj`) do have implementations that are not yet exposed through the API
layer.

---

## Cross-Cutting Concerns

### Dependency Direction
The dependency graph is correct and acyclic:
```
api/* → integration/* → ffi/c_api/* → ffi/base/*
api/* → integration/dtype_next/* → integration/unified_api/*
```
No reverse dependencies were found.

### Namespace Naming Conventions
| Namespace | Convention used | Note |
|-----------|-----------------|------|
| `ffi.c-api.*` | kebab-case (Clojure) matches `ffi/c_api/*` (snake_case file names) | ✅ Correct Clojure/file mapping |
| `integration.unified-api.*` | kebab-case | ✅ Correct |
| `integration.dtype-next.*` | kebab-case | ✅ Correct |
| `api.*` | kebab-case | ✅ Correct |

### Documentation Quality
- FFI layer: Good — every `defcfn` has a docstring documenting params and return code.  
- Integration layer: Excellent — rich docstrings with descriptions, parameter tables, examples, and
  use-case notes.  
- API layer: Good where present; absent in stub files.

### Test Coverage
- Test directory structure mirrors source structure (`test/…/api/`, `test/…/integration/base/`,
  `test/…/integration/unified_api/`).
- Only `api/core_test.clj` was found under `api/`.
- Integration and FFI test coverage extent is unclear from the directory listing alone.
- No property-based/generative tests using Spec generators (consistent with absence of Spec).

---

## Summary Table

| Layer | Structural Conformance | Functional Completeness | Key Issues |
|-------|------------------------|-------------------------|------------|
| **FFI** | ✅ High | ✅ High (~100 modules) | Missing typedef aliases, `f16` size |
| **Integration** | ✅ High | 🟡 High (unified_api) / Low (dtype_next is one file) | Arena not used in `resource.clj/0`, no Spec |
| **API** | 🟡 Medium | ❌ Low (5 of 6 files are stubs) | No Spec, guards not enforced, concerns leaked from integration layer, monolithic `core.clj` |

---

## Prioritised Recommendations

1. **Add Clojure Spec** to the API layer — this is a AGENTS.md requirement and enables generative
   testing.
2. **Enforce `assert-within-arrayfire!`** at the top of all public API functions.
3. **Implement the five stub API namespaces** by wrapping the existing integration layer
   implementations.
4. **Move Arena scope management** from `api/core.clj` into an integration-layer macro/helper,
   reducing the API layer's responsibility and making the arena consistently available to
   `resource.clj`.
5. **Split `api/core.clj`** into focused sub-files aligned with the Unified API groupings
   (e.g., `api/arith.clj`, `api/data.clj`, `api/random.clj`, …), keeping `core.clj` as the
   entry-point and re-export namespace.
6. **Consult `*af-arena*`** in `integration/base/resource.clj` `native-af-array-pointer/0` to
   make arena usage consistent across the integration layer.
7. **Add `af_array` typedef alias** in `ffi/base/definitions.clj` to improve self-documentation.
8. **Enrich `check!`** to optionally call `get-last-error` so error messages include the ArrayFire
   descriptive text, not just the numeric code.
