# `with-arrayfire` Execution Region Specification

## Overview

`with-arrayfire` defines a **deterministic GPU compute region** for ArrayFire-backed numerical execution in Clojure.

It establishes:

* Backend / device scoping
* Native FFM arena scoping (via Coffi)
* Deterministic AFArray resource management (via `tech.resource`)
* Strict non-escape semantics for `AFArray`
* Optional result conversion

This macro forms the **host ↔ device boundary** for GPU-accelerated computation.

---

# Design Goals

1. **Deterministic GPU memory cleanup**
2. **Deterministic native memory (FFM) cleanup**
3. **Thread-safe backend/device switching**
4. **No GPU handle leakage**
5. **Composable with dtype-next**
6. **Region-based compute semantics**
7. **Customizable result materialization**

---

# Conceptual Model

```
with-arrayfire
  ├── backend/device binding (serialized)
  ├── FFM Arena scope (confined)
  ├── tech.resource scope (AFArray lifecycle)
  ├── user body
  ├── result conversion (if needed)
  ├── AFArray release
  ├── Arena close
  └── backend/device restore
```

The region enforces:

> AFArray values may not escape the region.

---

# Macro Signature

```clojure
(with-arrayfire body...)

(with-arrayfire
  {:backend :cuda|:opencl|:cpu
   :device  <int>
   :converter-fn <fn>}
  body...)
```

---

# Options

| Key             | Type    | Description                                      |
| --------------- | ------- | ------------------------------------------------ |
| `:backend`      | keyword | Optional backend override                        |
| `:device`       | integer | Optional device index override                   |
| `:converter-fn` | fn      | Function used to convert returned AFArray values |

---

# Execution Semantics

## 1. Initialization

* Ensures ArrayFire is initialized exactly once.
* Thread-safe via `compare-and-set!`.

---

## 2. Backend & Device Binding

* Serialized using a global lock.
* Previous backend/device captured.
* Optional overrides applied.
* Always restored in `finally`.

This prevents race conditions in global ArrayFire state.

---

## 3. FFM Arena Scope

* A `coffi.mem/confined-arena` is created.
* The arena is closed deterministically.
* Native memory allocated via Coffi inside the region is invalidated at exit.

Optional dynamic binding:

```clojure
(def ^:dynamic *af-arena* nil)
```

If exposed, low-level FFM calls may use it.

---

## 4. Resource Scope

`tech.v3.resource/with-resource-context` is established.

All `AFArray` instances:

* Automatically register via `(resource/track ...)`
* Are deterministically released at scope exit
* Are idempotently safe via atomic release guard

Cleaner remains fallback only.

---

## 5. Result Handling

After executing the body:

* If the result is an `AFArray`, it is converted using `:converter-fn`
* If not, it is returned unchanged

Conversion occurs **inside** the resource context.

After conversion:

* All AFArray instances are released
* The converted value survives

---

# Non-Escape Rule

`AFArray` instances MUST NOT escape the region.

Violations would:

* Return GPU handles tied to released resources
* Break deterministic semantics
* Cause undefined behavior

Therefore:

* Top-level AFArray return values are always converted
* Nested structures may optionally be deep-converted (implementation choice)

---

# Default Converter

```clojure
(defn default-af-converter
  [^AFArray arr]
  (af->host arr))
```

Recommended behavior:

* Convert to dtype-next CPU container
* Preserve shape
* Preserve element type

Avoid nested vector conversion by default for performance reasons.

---

# Supporting Code

## Initialization

```clojure
(defonce ^:private af-initialized? (atom false))

(defn ensure-af-init! []
  (when (compare-and-set! af-initialized? false true)
    (af-init!)))
```

---

## Backend Lock

```clojure
(def ^:private backend-lock (Object.))
```

---

## Result Conversion

```clojure
(defn af-result-convert
  [converter result]
  (cond
    (instance? AFArray result)
    (converter result)

    :else
    result))
```

Optional deep conversion:

```clojure
(defn deep-convert [converter x]
  (cond
    (instance? AFArray x)
    (converter x)

    (map? x)
    (into {} (map (fn [[k v]] [k (deep-convert converter v)]) x))

    (vector? x)
    (mapv #(deep-convert converter %) x)

    (sequential? x)
    (map #(deep-convert converter %) x)

    :else
    x))
```

---

# Full Macro Definition

```clojure
(defmacro with-arrayfire
  [& args]
  (let [[opts body] (if (map? (first args))
                      [(first args) (rest args)]
                      [{} args])
        converter (or (:converter-fn opts)
                      `default-af-converter)]
    `(do
       (ensure-af-init!)
       (locking backend-lock
         (let [prev-backend# (af-get-backend)
               prev-device#  (af-get-device)]
           (try
             (when-let [b# (:backend ~opts)]
               (af-set-backend! b#))
             (when-let [d# (:device ~opts)]
               (af-set-device! d#))
             (with-open [arena# (coffi.mem/confined-arena)]
               (binding [*af-arena* arena#]
                 (stack-resource-context
                   (let [result# (do ~@body)]
                     (af-result-convert ~converter result#)))))
             (finally
               (when (:device ~opts)
                 (af-set-device! prev-device#))
               (when (:backend ~opts)
                 (af-set-backend! prev-backend#)))))))))
```

---

# dtype-next Integration Semantics

Inside `with-arrayfire`:

* dtype operations dispatch to AFArray implementations
* Binary ops promote CPU → GPU
* Reductions run on GPU
* Results are AFArray until region exit

Outside region:

* All values are CPU-native
* dtype-next behaves normally

This establishes:

> Scoped GPU acceleration.

---

# Nested Regions

Nested `with-arrayfire` blocks:

* Are supported
* Establish independent Arena scopes
* Serialize backend switching
* Do not leak GPU resources

---

# Thread Safety

Backend/device switching is protected via a global lock.

This ensures:

* No concurrent backend mutation
* Correct restoration ordering
* Safe multi-threaded use

Note: True parallel multi-device execution requires a higher-level scheduler.

---

# Failure Semantics

If body throws:

* All AFArray instances are released
* Arena is closed
* Backend/device is restored
* Exception propagates

System remains consistent.

---

# Performance Considerations

* Arena allocation is cheap (confined scope)
* tech.resource tracking is minimal overhead
* Backend locking only impacts backend-switching calls
* GPU compute dominates runtime cost

---

# Summary

`with-arrayfire` defines a:

* Deterministic
* Region-scoped
* GPU-safe
* FFM-safe
* dtype-compatible
* Thread-safe

execution boundary for ArrayFire compute in Clojure.

It enforces a clean separation between:

* Device world (inside region)
* Host world (outside region)

This provides predictable semantics and prevents resource leaks while integrating naturally with the tech ecosystem.

---

If you'd like, I can next produce:

* A diagram of lifecycle transitions
* A companion spec for AFArray itself
* A dtype-next integration spec
* Or a test strategy document for correctness verification
