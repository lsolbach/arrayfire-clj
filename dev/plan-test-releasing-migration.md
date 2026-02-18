# Plan: Migrate Tests from Manual `.close` to `releasing!`

## Context

AFArray handles are now registered with `tech.v3.resource` via `resource/track` in
`af-array-new`. The `releasing!` macro from `tech.v3.resource` establishes a resource
scope: all tracked resources created within the body are automatically released when
the scope exits (normally or via exception). This replaces the need for manual
`.close` calls and `try/finally` blocks for AFArray cleanup.

The introduction notebook already demonstrates the new pattern. All 30 test files
must be migrated to follow suit.

---

## Scope

### What changes

| Change | Description |
|--------|-------------|
| Add `releasing!` require | Each test ns needs `[tech.v3.resource :refer [releasing!]]` |
| Wrap test bodies | Each `deftest` body that creates AFArray resources gets wrapped in `(releasing! ...)` |
| Remove `.close` calls | All explicit `(.close arr)` calls on AFArrays are removed |
| Remove `try/finally` for AFArrays | `try/finally` blocks whose sole purpose is calling `.close` are flattened |

### What does NOT change

Non-AFArray resources have their own lifecycle and must keep their existing cleanup:

| Resource Type | Cleanup Function | Keep As-Is |
|---------------|-----------------|------------|
| Events | `delete-event!` | ✅ |
| Features | `release-features!` | ✅ |
| Indexers | `release-indexers!` | ✅ |
| Windows | `destroy-window!` | ✅ |
| Random Engines | `release-engine!` | ✅ |
| Pinned/Host/Device memory | `free-pinned!` / `free-host!` / `free-device!` | ✅ |
| File cleanup | `io/delete-file` | ✅ |
| State restoration | `set-mem-step-size!`, `set-max-jit-len!` etc. | ✅ |
| Arena (Java FFI) | `.close` on Arena | ✅ |

Tests that don't create AFArray resources (e.g., device_test, error_test, cuda_test,
opencl_test, jit_test_api_test) need no changes.

---

## Transformation Patterns

### Pattern 1: Bare `.close` (no try/finally)

**Before:**
```clojure
(deftest test-foo
  (testing "foo works"
    (device/init!)
    (let [a (array/create-array ...)
          b (arith/some-op a)
          buf (mem/alloc ...)]
      (array/get-data-ptr b buf)
      (is (approx= ...))
      (.close a)
      (.close b))))
```

**After:**
```clojure
(deftest test-foo
  (testing "foo works"
    (device/init!)
    (releasing!
      (let [a (array/create-array ...)
            b (arith/some-op a)
            buf (mem/alloc ...)]
        (array/get-data-ptr b buf)
        (is (approx= ...))))))
```

**Applies to:** arith_test, array_test, data_test, algorithm_test, blas_test,
complex_test, statistics_test, image_test (some tests), index_test (some tests)

---

### Pattern 2: `try/finally` + `.close` on AFArrays only

**Before:**
```clojure
(deftest test-bar
  (testing "bar works"
    (device/init!)
    (let [signal (array/create-array ...)
          freq (signal/fft signal)]
      (try
        (is (instance? AFArray freq))
        (finally
          (.close signal)
          (.close freq))))))
```

**After:**
```clojure
(deftest test-bar
  (testing "bar works"
    (device/init!)
    (releasing!
      (let [signal (array/create-array ...)
            freq (signal/fft signal)]
        (is (instance? AFArray freq))))))
```

**Applies to:** signal_test, ml_test, sparse_test, moments_test, lapack_test,
internal_test, util_test, vision_test, memory_test (unified_api)

---

### Pattern 3: `try/finally` with mixed cleanup (AFArray + non-AFArray)

**Before:**
```clojure
(deftest test-baz
  (testing "baz works"
    (device/init!)
    (let [evt (event/create-event!)
          arr (array/create-array ...)]
      (try
        (event/mark-event! evt)
        (is ...)
        (finally
          (.close arr)
          (event/delete-event! evt))))))
```

**After:**
```clojure
(deftest test-baz
  (testing "baz works"
    (device/init!)
    (let [evt (event/create-event!)]
      (try
        (releasing!
          (let [arr (array/create-array ...)]
            (event/mark-event! evt)
            (is ...)))
        (finally
          (event/delete-event! evt))))))
```

The `try/finally` is retained for the non-AFArray resource; only AFArray `.close`
calls are removed because `releasing!` handles them.

**Applies to:** event_test, features_test (tests that extract AFArrays from
features), index_test (tests with `release-indexers!`), graphic_test
(tests with `destroy-window!`), random_test (tests with `release-engine!`)

---

### Pattern 4: Nested `try/finally` for AFArrays (deeply nested)

**Before:**
```clojure
(deftest test-nested
  (testing "nested"
    (device/init!)
    (let [img (array/create-array ...)]
      (try
        (let [[dx dy] (img/sobel img 3)]
          (try
            (is (instance? AFArray dx))
            (finally
              (.close dx)
              (.close dy))))
        (finally
          (.close img))))))
```

**After:**
```clojure
(deftest test-nested
  (testing "nested"
    (device/init!)
    (releasing!
      (let [img (array/create-array ...)
            [dx dy] (img/sobel img 3)]
        (is (instance? AFArray dx))))))
```

`releasing!` handles all AFArrays in the scope, so nesting is unnecessary.

**Applies to:** image_test (sobel, resize, etc.)

---

## File-by-File Change List

### No changes needed (5 files)

| File | Reason |
|------|--------|
| base/error_test.clj | No AFArray resources |
| base/memory_test.clj | No AFArray resources (tests memory utilities) |
| unified_api/device_test.clj | No AFArray resources |
| unified_api/cuda_test.clj | No AFArray resources (skip-if-not-cuda) |
| unified_api/opencl_test.clj | No AFArray resources (skip-if-not-opencl) |

### Special handling (1 file)

| File | Reason |
|------|--------|
| base/resource_test.clj | Tests AFArray lifecycle itself (`.close`, GC, closed-access); keep `.close`/Arena cleanup as-is since tests verify the close mechanism. However, `releasing!` can be used where the test doesn't verify close behavior. |

### Pattern 1 — bare `.close` removal (8 files)

| File | ~Tests to change |
|------|-----------------|
| unified_api/arith_test.clj | ~25 |
| unified_api/array_test.clj | ~20 |
| unified_api/data_test.clj | ~25 |
| unified_api/algorithm_test.clj | ~22 |
| unified_api/blas_test.clj | ~15 |
| unified_api/complex_test.clj | ~30 |
| unified_api/statistics_test.clj | ~28 |
| unified_api/error_test.clj | ~15 |

### Pattern 2 — `try/finally` + `.close` → `releasing!` (10 files)

| File | ~Tests to change |
|------|-----------------|
| unified_api/signal_test.clj | ~45 |
| unified_api/ml_test.clj | ~25 |
| unified_api/sparse_test.clj | ~22 |
| unified_api/moments_test.clj | ~13 |
| unified_api/lapack_test.clj | ~28 |
| unified_api/internal_test.clj | ~18 |
| unified_api/util_test.clj | ~15 |
| unified_api/vision_test.clj | ~20 |
| unified_api/image_test.clj | ~18 (mixed with Pattern 4) |
| unified_api/memory_test.clj | ~20 |

### Pattern 3 — mixed cleanup (5 files)

| File | ~Tests to change | Non-AFArray resources |
|------|------------------|-----------------------|
| unified_api/event_test.clj | 4 | `delete-event!` |
| unified_api/features_test.clj | ~19 | `release-features!` |
| unified_api/index_test.clj | ~25 | `release-indexers!` |
| unified_api/graphic_test.clj | ~20 | `destroy-window!` |
| unified_api/random_test.clj | ~18 | `release-engine!` |

### Pattern 2 + state restoration (1 file)

| File | ~Tests to change | Notes |
|------|-----------------|-------|
| unified_api/jit_test_api_test.clj | ~20 | Keep `try/finally` for `set-max-jit-len!` state restoration |

---

## Step-by-Step Procedure per File

For each test file that needs changes:

### Step 1: Add `releasing!` to requires

Add `[tech.v3.resource :refer [releasing!]]` to the `:require` vector of the
namespace declaration.

### Step 2: Transform each `deftest`

For each test that creates AFArray resources:

1. **Identify the scope**: Find the outermost `let` binding that creates the first
   AFArray.
2. **Wrap in `releasing!`**: Insert `(releasing! ...)` around the body, just inside
   the `testing` form, after `(device/init!)`.
3. **Remove all `(.close ...)` calls** on AFArray instances.
4. **Flatten `try/finally`** blocks that only existed for AFArray `.close` calls.
5. **Preserve `try/finally`** for non-AFArray cleanup (events, features, indexers,
   windows, engines, state restoration).
6. **For mixed cases**: Move the `releasing!` scope inside the `try` block so that
   non-AFArray `finally` cleanup still runs.

### Step 3: Verify

After editing each file:

1. Load the test namespace in the REPL: `(require 'ns-name :reload)`
2. Run the tests: `(run-tests 'ns-name)`
3. Verify all tests pass and no resource leaks are reported.

---

## Execution Order

Process files in order of complexity (simplest first):

1. **Pattern 1 files** (bare `.close`) — straightforward mechanical replacement
2. **Pattern 2 files** (`try/finally` → `releasing!`) — flatten try blocks
3. **Pattern 3 files** (mixed) — careful scoping of `releasing!` around AFArray
   creation, preserving non-AFArray cleanup
4. **Pattern 4 in image_test** (nested) — flatten nested try blocks
5. **base/resource_test.clj** — minimal changes; preserve tests that verify close
   behavior
6. **Run full test suite** to confirm all tests pass

---

## Risks and Mitigations

| Risk | Mitigation |
|------|-----------|
| `releasing!` releases in reverse creation order; some tests may depend on close order | ArrayFire's refcount is order-independent; no issue expected |
| `releasing!` catches exceptions and still releases | Same as `try/finally`; behavior matches |
| Non-AFArray resources accidentally covered by `releasing!` | Only AFArray is tracked via `resource/track`; non-AFArray resources are unaffected |
| Tests that verify `.close` behavior (resource_test) | Keep those tests unchanged; they test the mechanism itself |
| Feature extraction returns AFArrays that were previously leaked | `releasing!` will now properly release them — this fixes bugs |

---

## Estimated Effort

~550 test functions across 25 files need modification. Each modification is
mechanical (wrap in `releasing!`, delete `.close` calls, flatten `try/finally`).
Estimated: ~2–3 hours for an experienced developer, or ~30 minutes per file batch
for an LLM agent with sequential verification.
