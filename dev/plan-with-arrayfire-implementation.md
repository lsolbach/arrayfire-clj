# Implementation Plan: `with-arrayfire` Execution Region

## 1. Specification Assessment

### 1.1 Plausibility Verdict

The specification in `with-arrayfire-context.md` is **fundamentally sound** and
well-aligned with the existing codebase architecture. The layered approach
(FFM arena → tech.resource scope → backend/device scoping) correctly addresses
the three distinct resource categories at play. The implementation can draw
directly on the already-implemented building blocks:

| Concern | Existing Building Block | Namespace |
|---|---|---|
| Initialization | `init!` / `ensure-af-init!` | `core` |
| Backend switching | `set-backend!`, `get-active-backend` | `integration.unified-api.device` |
| Device switching | `set-device!`, `get-device` | `integration.unified-api.device` |
| AFArray lifecycle | `AFArray`, `af-array-new`, `resource/track` | `integration.base.resource` |
| Resource scoping | `stack-resource-context` | `tech.v3.resource` |
| Coffi arenas | `confined-arena` + `with-open` | `coffi.mem` |
| Error handling | `check!` | `integration.base.error` |
| Host transfer | `to-host`, `to-native-buffer` | `core` |

### 1.2 Issues & Improvement Suggestions

#### Issue 1: Global `locking` serializes ALL compute — critical bottleneck

**Problem**: The spec uses `(locking backend-lock ...)` around the entire body,
including GPU compute. This means only one thread can execute GPU work at a time
*even when no backend/device switching is needed* (the common case). This
defeats the purpose of GPU acceleration for concurrent workloads.

**Suggestion**: Only hold the lock for the brief backend/device save-set-restore
operations, not for the compute body. Use a `ReentrantLock` with lock/unlock
around backend mutation only:

```
   lock   → save prev backend/device → set new → unlock
   body (runs without lock)
   lock   → restore prev backend/device → unlock
```

If the user does NOT pass `:backend` or `:device` options (the common case),
skip locking entirely.

**Tradeoff**: Two concurrent `with-arrayfire` blocks with *different* backends
could interleave badly because ArrayFire's backend state is global (per-process).
But the spec already acknowledges this: "True parallel multi-device execution
requires a higher-level scheduler." The improvement is that the *default*
no-backend-switch case runs fully parallel.

#### Issue 2: `with-open` on `confined-arena` — thread confinement constraint

**Problem**: `coffi.mem/confined-arena` creates a `java.lang.foreign.Arena/ofConfined`
which is **thread-confined** — it cannot be used from a different thread than
the one that created it. The spec exposes this arena via `*af-arena*` dynamic
binding, but dynamic vars *can* be visible on other threads (e.g. `future`,
core.async `go` blocks). Using the arena from such threads would throw
`WrongThreadException`.

**Suggestion**: Document this clearly. Offer a `:shared-arena` option
for advanced multi-threaded use cases:

```clojure
(with-arrayfire {:arena-type :shared} ...)
```

Default should remain `:confined` for performance and safety.

#### Issue 3: Backend keywords need mapping to constants

**Problem**: The spec uses `:cuda`, `:opencl`, `:cpu` as keyword values for
`:backend`, but the existing code uses integer constants (`AF_BACKEND_CUDA = 2`,
etc.). A mapping is needed.

**Suggestion**: Define a `backend-keyword->constant` map and resolve keywords
inside the macro expansion.

#### Issue 4: `result-convert` may not find `AFArray` in collections

**Problem**: The simple `(instance? AFArray result)` check in `result-convert`
only handles the case where the *top-level* return value is an `AFArray`. If the
user returns `{:a some-af-array :b some-af-array}`, those handles escape. The
spec mentions `deep-convert` as optional.

**Suggestion**: Make `deep-convert` the default behavior. The performance cost
of walking a small return structure is negligible compared to GPU compute. Users
who know they return a single scalar or have already converted can use
`:converter-fn identity` to opt out.

#### Issue 5: Nested `with-arrayfire` and backend lock reentrancy

**Problem**: The spec says nested regions "serialize backend switching" using
`locking`. Java's `synchronized` (used by `locking`) IS reentrant per-thread,
so nesting works from the same thread. However, inner regions that switch
backends would restore the *inner* previous backend, not the outermost one,
potentially leaving the outer region on the wrong backend after the inner
region exits.

**Suggestion**: Use a thread-local stack for backend/device state instead of
simple save/restore. Each `with-arrayfire` pushes and pops. This ensures
correct restoration even with interleaved switches.

#### Issue 6: Spec references functions not yet in `core.clj`

**Problem**: The spec references `get-backend`, `af->host`. These exist in the integration
layer (`unified-api.device`) under different names: `get-active-backend`.

**Action**: Use the existing integration layer functions. No renaming needed —
the macro is internal to `core.clj`.

#### Issue 7: `*af-arena*` exposure may be premature

**Problem**: Exposing the arena as a dynamic var creates a public API surface
that is hard to change later. Most users should never need to allocate their own
native memory inside `with-arrayfire`.

**Suggestion**: Keep `*af-arena*` private and `^:dynamic` for now. Expose it
only if a clear use case emerges. The integration layer functions should take
arenas as parameters where needed, not from a global binding.

#### Issue 8: Missing `sync!` before result conversion

**Problem**: On CUDA/OpenCL backends, ArrayFire operations are asynchronous.
If results are converted to host buffers without synchronization, data may be
incomplete.

**Suggestion**: Call `(device/sync! -1)` before result conversion to ensure all
pending GPU operations are complete.

---

## 2. Implementation Plan

### 2.1 Prerequisites

The following already exist and need no changes:

- `ensure-af-init!` in `core.clj`
- `device/set-backend!`, `device/get-active-backend` in integration layer
- `device/set-device!`, `device/get-device` in integration layer
- `device/sync!` in integration layer
- `AFArray` type in `integration.base.resource`
- `stack-resource-context` from `tech.v3.resource`
- `coffi.mem/confined-arena` and `coffi.mem/shared-arena`

### 2.2 Implementation Steps

All implementation steps target
`src/org/soulspace/arrayfire/core.clj` unless noted otherwise.

---

#### Step 1: Add required namespace imports

Add the following to the `ns` `:require` vector:

```clojure
[org.soulspace.arrayfire.integration.base.resource :as res]
```

And import:

```clojure
(:import (org.soulspace.arrayfire.integration.base.resource AFArray))
```

The integration layer device namespace is already required as `device`. Also
ensure `coffi.mem` (already required as `mem`) covers `confined-arena` and
`shared-arena`.

---

#### Step 2: Define backend keyword mapping

Add after the existing `constant->return` def:

```clojure
(def backend-keyword->constant
  "Mapping of backend keywords to ArrayFire backend constants."
  {:default defs/AF_BACKEND_DEFAULT
   :cpu     defs/AF_BACKEND_CPU
   :cuda    defs/AF_BACKEND_CUDA
   :opencl  defs/AF_BACKEND_OPENCL
   :oneapi  defs/AF_BACKEND_ONEAPI})
```

Add a helper to resolve backend values (keyword or int):

```clojure
(defn- resolve-backend
  "Resolve a backend keyword or integer to an ArrayFire backend constant."
  [backend]
  (if (keyword? backend)
    (or (get backend-keyword->constant backend)
        (throw (ex-info (str "Unknown backend: " backend)
                        {:backend backend
                         :valid-backends (keys backend-keyword->constant)})))
    (int backend)))
```

---

#### Step 3: Define the dynamic arena var and backend lock

```clojure
(def ^:private ^:dynamic *af-arena*
  "Dynamic var holding the current coffi Arena inside a `with-arrayfire` region.
   Private — not part of the public API."
  nil)

(def ^:private backend-lock
  "Lock object for serializing backend/device switching."
  (Object.))
```

---

#### Step 4: Implement the `af-result-convert` function

```clojure
(defn- result-convert
  "Convert AFArray values in the result before they escape the resource context.
   Walks the result structure to find and convert any AFArray instances."
  [converter result]
  (cond
    (instance? AFArray result)
    (converter result)

    (map? result)
    (persistent!
     (reduce-kv (fn [m k v]
                  (assoc! m k (result-convert converter v)))
                (transient {}) result))

    (vector? result)
    (mapv #(result-convert converter %) result)

    (sequential? result)
    (map #(result-convert converter %) result)

    (set? result)
    (into #{} (map #(result-convert converter %)) result)

    :else
    result))
```

---

#### Step 5: Implement the default converter function

```clojure
(defn default-af-converter
  "Default converter for AFArray → host data.
   Converts an AFArray to a dtype-next native buffer, preserving shape and dtype.

   Parameters:
   - arr: AFArray instance

   Returns:
   dtype-next native buffer with the array data."
  [^AFArray arr]
  (let [handle (res/af-handle-value arr)
        ;; Get array dimensions and type from ArrayFire
        n-dims-buf (mem/alloc 4)
        _  (check! (af-array/get-numdims n-dims-buf
                     (res/af-handle arr))
                   "af_get_numdims")
        n-dims (mem/read-int n-dims-buf 0)
        dims-buf (mem/alloc (* 8 4)) ;; max 4 dims
        _  (check! (af-array/get-dims dims-buf
                     (mem/alloc 8) (mem/alloc 8) (mem/alloc 8)
                     (res/af-handle arr))
                   "af_get_dims")
        ;; Get element count
        elem-count-buf (mem/alloc 8)
        _  (check! (af-array/get-elements elem-count-buf
                     (res/af-handle arr))
                   "af_get_elements")
        n (mem/read-long elem-count-buf 0)
        ;; Get type
        type-buf (mem/alloc 4)
        _  (check! (af-array/get-type type-buf
                     (res/af-handle arr))
                   "af_get_type")
        af-type (mem/read-int type-buf 0)
        dtype-kw (get {defs/AF_DTYPE_F32 :float32
                       defs/AF_DTYPE_F64 :float64
                       defs/AF_DTYPE_S32 :int32
                       defs/AF_DTYPE_U32 :uint32
                       defs/AF_DTYPE_S64 :int64
                       defs/AF_DTYPE_U64 :uint64
                       defs/AF_DTYPE_S16 :int16
                       defs/AF_DTYPE_U16 :uint16
                       defs/AF_DTYPE_U8  :uint8
                       defs/AF_DTYPE_B8  :uint8}
                      af-type :float64)]
    (to-native-buffer handle dtype-kw n)))
```

**Note**: This step depends on the integration bindings for `get_numdims`,
`get_elements`, `get_type`, and `get_dims` already existing in
`integration.unified-api.array`. Verify these exist. If `get-dims` has a different
signature (4 separate out-pointers for d0,d1,d2,d3), adjust accordingly.

**Alternative (simpler first implementation)**: If complexity is too high for
the converter to auto-detect dtype, require the user to provide a converter
that knows the expected type. Or, implement a simplified version that always
uses `:float64`.

---

#### Step 6: Implement the `with-arrayfire` macro

```clojure
(defmacro with-arrayfire
  "Execute body within a deterministic GPU compute region.
   
   Establishes:
   - ArrayFire initialization (once)
   - Optional backend/device switching (serialized)
   - FFM Arena scope (confined, deterministic cleanup)
   - tech.resource scope (AFArray lifecycle management)
   - Result conversion (AFArray → host data)
   
   AFArray values MUST NOT escape this region. Any AFArray in the return
   value is automatically converted to host data via the converter function.
   
   Options map (optional first argument):
   - :backend    - keyword (:cpu, :cuda, :opencl, :oneapi) or int constant
   - :device     - integer device index
   - :converter-fn - function to convert AFArray to host data (default: default-af-converter)
   
   Examples:
     ;; Basic usage
     (with-arrayfire
       (let [a (create-array [1.0 2.0 3.0 4.0] [2 2])]
         (to-host a 4)))
   
     ;; With backend selection
     (with-arrayfire {:backend :cuda :device 0}
       ...)"
  [& args]
  (let [[opts body] (if (map? (first args))
                      [(first args) (rest args)]
                      [{} args])
        converter (or (:converter-fn opts) `default-af-converter)
        has-backend? (contains? opts :backend)
        has-device?  (contains? opts :device)]
    (if (or has-backend? has-device?)
      ;; With backend/device switching — needs lock
      `(do
         (ensure-af-init!)
         (locking backend-lock
           (let [prev-backend# (device/get-active-backend)
                 prev-device#  (device/get-device)]
             (try
               ~(when has-backend?
                  `(device/set-backend! (resolve-backend ~(:backend opts))))
               ~(when has-device?
                  `(device/set-device! ~(:device opts)))
               (with-open [arena# (mem/confined-arena)]
                 (binding [*af-arena* arena#]
                   (stack-resource-context
                     (let [result# (do ~@body)]
                       (device/sync!)
                       (af-result-convert ~converter result#)))))
               (finally
                 ~(when has-device?
                    `(device/set-device! prev-device#))
                 ~(when has-backend?
                    `(device/set-backend! prev-backend#)))))))
      ;; No backend/device switching — no lock needed
      `(do
         (ensure-af-init!)
         (with-open [arena# (mem/confined-arena)]
           (binding [*af-arena* arena#]
             (stack-resource-context
               (let [result# (do ~@body)]
                 (device/sync!)
                 (af-result-convert ~converter result#)))))))))
```

**Key design decisions in this implementation:**

1. **Lock only when switching backend/device** — avoids serializing the common
   case.
2. **`device/sync!`** is called before result conversion to ensure GPU ops
   complete.
3. **Deep conversion** via `af-result-convert` handles nested structures.
4. **`*af-arena*`** is bound but private — internal use only.
5. **`confined-arena`** is used by default (safe, performant, deterministic).

---

#### Step 7: Verify integration function availability

Before implementation, verify these functions exist in `integration.unified-api.array`:

- `get-numdims` — needed by `default-af-converter`
- `get-elements` — needed by `default-af-converter`
- `get-type` — needed by `default-af-converter`
- `get-dims` — needed by `default-af-converter`

Check by searching the codebase:

```
grep -l "af-get-numdims\|af-get-elements\|af-get-type\|af-get-dims" \
  src/org/soulspace/arrayfire/integration/unified_api/array.clj
```

If any are missing, they need to be added to the integration layer first, or the
default converter must be simplified.

---

#### Step 8: Write tests

Create `test/org/soulspace/arrayfire/core_test.clj`:

**Test 1: Basic region — no options**
```clojure
(deftest with-arrayfire-basic-test
  (testing "Basic with-arrayfire region returns host data"
    (let [result (with-arrayfire
                   (create-array [1.0 2.0 3.0 4.0] [2 2]))]
      ;; Result should be host data, not an AFArray
      (is (not (instance? AFArray result))))))
```

**Test 2: Explicit host conversion inside region**
```clojure
(deftest with-arrayfire-explicit-host-test
  (testing "Explicit to-host inside with-arrayfire"
    (let [result (with-arrayfire
                   (let [a (create-array [1.0 2.0 3.0 4.0] [2 2])]
                     (vec (to-host a 4))))]
      (is (= [1.0 2.0 3.0 4.0] result)))))
```

**Test 3: Backend switching**
```clojure
(deftest with-arrayfire-backend-test
  (testing "Backend option sets and restores backend"
    (let [original-backend (do (ensure-af-init!)
                               (device/get-active-backend))]
      (with-arrayfire {:backend :cpu}
        ;; Inside: should be CPU backend
        (is (= defs/AF_BACKEND_CPU (device/get-active-backend))))
      ;; Outside: should be restored
      (is (= original-backend (device/get-active-backend))))))
```

**Test 4: Exception safety**
```clojure
(deftest with-arrayfire-exception-safety-test
  (testing "Backend/device restored after exception"
    (let [original-backend (do (ensure-af-init!)
                               (device/get-active-backend))]
      (is (thrown? Exception
            (with-arrayfire {:backend :cpu}
              (throw (Exception. "test error")))))
      (is (= original-backend (device/get-active-backend))))))
```

**Test 5: Nested regions**
```clojure
(deftest with-arrayfire-nested-test
  (testing "Nested with-arrayfire regions work correctly"
    (let [result (with-arrayfire
                   (let [outer (create-array [1.0 2.0] [2])]
                     (with-arrayfire
                       (let [inner (create-array [3.0 4.0] [2])]
                         (vec (to-host inner 2))))))]
      (is (= [3.0 4.0] result)))))
```

**Test 6: Map result with AFArray values (deep conversion)**
```clojure
(deftest with-arrayfire-deep-convert-test
  (testing "Map containing AFArray is deep-converted"
    (let [result (with-arrayfire
                   {:converter-fn (fn [arr] (vec (to-host arr 4)))}
                   {:data (create-array [1.0 2.0 3.0 4.0] [2 2])})]
      (is (map? result))
      (is (vector? (:data result))))))
```

---

#### Step 9: Add rich comment block for REPL experimentation

Append to `core.clj`:

```clojure
(comment
  ;; with-arrayfire REPL experiments
  
  ;; Basic usage
  (with-arrayfire
    (let [a (create-array [1.0 2.0 3.0 4.0] [2 2])]
      (vec (to-host a 4))))

  ;; With backend selection
  (with-arrayfire {:backend :cpu}
    (let [a (create-array [1.0 2.0 3.0] [3])]
      (vec (to-host a 3))))

  ;; Nested regions
  (with-arrayfire
    (with-arrayfire
      (vec (to-host (create-array [42.0] [1]) 1))))
  )
```

---

### 2.3 Implementation Order

Work should proceed in this order, testing at each step via the REPL:

| # | Step | Depends On | Effort |
|---|------|-----------|--------|
| 1 | Add namespace imports | — | Small |
| 2 | Backend keyword mapping + `resolve-backend` | Step 1 | Small |
| 3 | Dynamic arena var + backend lock | — | Small |
| 4 | `result-convert` | — | Small |
| 5 | `default-af-converter` | Step 1, verify integration fns | Medium |
| 6 | `with-arrayfire` macro | Steps 1–5 | Medium |
| 7 | `with-arrayfire-raw` (optional) | Step 6 | Small |
| 8 | Verify integration functions | — | Small |
| 9 | Tests | Steps 1–6 | Medium |
| 10 | RCF for REPL experiments | Steps 1–6 | Small |

**Estimated total effort**: ~4 hours for a developer familiar with the codebase.

---

### 2.4 Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| `default-af-converter` integration fns missing | Fall back to simplified converter requiring explicit dtype |
| `confined-arena` thread issues with async code | offer `:shared` option |
| `locking` performance impact | Only lock when switching backend/device |
| Complex number dtypes in converter | Handle `c32`/`c64` as special cases returning `[real imag]` pairs |
| `stack-resource-context` interaction with `with-open` | Both use try-finally; ordering is correct (resource release → arena close) |

---

### 2.5 Future Enhancements (Out of Scope)

- **dtype-next protocol extensions** for AFArray (so `dtype/shape`, `dtype/elemwise-datatype` dispatch to GPU queries)
- **Parallel multi-device scheduler** built on top of `with-arrayfire`
- **Clojure Spec** for the options map
- **`with-arrayfire-async`** variant using `shared-arena` and returning a future
