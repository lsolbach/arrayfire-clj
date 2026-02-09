# Idiomatic Clojure API Layer Design

This document proposes an idiomatic Clojure API layer built on top of the
integration layer. It defines goals, namespaces, data model, function naming,
dtype-next integration, and example usage.

## Goals

- Provide an idiomatic, data-oriented Clojure API on top of the integration
  layer while keeping the integration layer thin and stable.
- Preserve zero-copy where possible on the JVM side and be explicit about
  unavoidable host<->device copies.
- Keep resource management explicit but ergonomic with scoped helpers.
- Enable polymorphic inputs (Clojure collections, primitive arrays,
  dtype-next containers, native buffers) with predictable dispatch.
- Maintain compatibility with ArrayFire API semantics and performance.

## Non-Goals

- Rewriting or duplicating the integration layer.
- Hiding all resource management. Explicit release and scoped helpers remain
  available and recommended.
- Creating a full tensor DSL in the first iteration.

## Namespace Layout

Proposed high-level namespaces (thin wrappers around integration layer):

- org.soulspace.arrayfire.api.core
  - init, info, backend selection, device info
- org.soulspace.arrayfire.api.array
  - array creation, conversion, shape, dtype, predicates
- org.soulspace.arrayfire.api.arith
  - arithmetic, element-wise math, comparisons
- org.soulspace.arrayfire.api.blas
  - matmul, gemm, dot, transpose
- org.soulspace.arrayfire.api.lapack
  - lu, qr, svd, inverse, solve, etc
- org.soulspace.arrayfire.api.signal
  - fft, ifft, conv, filters
- org.soulspace.arrayfire.api.image
  - io, transforms, filters
- org.soulspace.arrayfire.api.index
  - indexing helpers and idiomatic slicing
- org.soulspace.arrayfire.api.resource
  - resource helpers, scoped lifetimes
- org.soulspace.arrayfire.api.dtype
  - dtype conversion, dtype-next helpers

Rationale: keep API surface modular and discoverable while mapping closely to
ArrayFire module domains.

## Data Model

### Primary Array Type

- Use integration.jvm-integration/AFArray as the primary runtime handle.
- Provide a lightweight record or map wrapper for metadata (dims, dtype,
  backend) only when needed by the API layer.
- Avoid wrapping AFArray in multiple layers that could obscure resource
  management.

### Optional Wrapper

- Provide an optional wrapper record `AF` to attach metadata lazily.
- Keep conversion functions to unwrap AF to AFArray and vice versa.

Example:

- AFArray: the actual handle
- AF: {:arr <AFArray> :dtype :f64 :dims [3 3]} (optional, derived on demand)

## Resource Management

Provide explicit and scoped helpers:

- `release!` -> delegate to integration array release
- `with-array` / `with-arrays` -> scoped release using try/finally
- `retain` -> wraps af-retain-array semantics

Examples:

- (with-array [a (af/array [1 2 3])] (af/sum a))
- (with-arrays [[a (af/array [1 2 3])]
               [b (af/array [4 5 6])]]
    (af/add a b))

Rationale: avoid leaks while keeping explicit control available.

## Input Coercion and Dispatch

Define a single coercion entry point in api.array:

- `->array` (or `array`)
  - Accepts:
    - AFArray
    - Clojure seqs/vectors
    - primitive arrays
    - dtype-next containers
    - dtype-next native buffers
    - MemorySegment (native buffer)
  - Optional args:
    - dims
    - dtype (keyword or ArrayFire constant)
    - copy? (default true for heap data, false for native buffers)

### Coercion Rules

1. AFArray -> return as-is (no copy)
2. dtype-next native buffer -> zero-copy host, then upload to device
3. dtype-next heap buffer -> copy to native, then upload
4. Clojure collection -> copy to native, then upload
5. MemorySegment -> use directly

Clarify that host->device is always a copy (ArrayFire limitation).

## Dtype Model

Map keywords to ArrayFire constants:

- :f32, :f64, :c32, :c64, :s32, :u32, :s64, :u64, :s16, :u16, :u8, :b8

Provide helpers:

- `dtype` -> returns keyword
- `dtype-const` -> returns ArrayFire constant
- `dtype-size` -> bytes per element
- `complex?` -> dtype predicate

Integrate with dtype-next:

- `dtype-next->af` (keyword mapping)
- `af->dtype-next` (keyword mapping)

## Shape and Dim Handling

- Support dims as vector or list
- Allow `:auto` dims for 1D data
- Provide `shape` and `ndims`
- Provide `reshape` (moddims)

## Naming Conventions

- Clojure-friendly names, no C++ operator names
- Use `!` for in-place operations
- Avoid core name conflicts (`minof`, `maxof`, `conjg`, etc)
- Provide aliases to integration functions when safe

## API Surface Sketch

### Core

- init!, info
- set-backend!, get-backend
- device-count, device-info, device?

### Array

- array (->array)
- zeros, ones, constant, identity, range
- shape, ndims, elements, dtype
- copy, clone, retain, release!
- to-host, to-native-buffer

### Arithmetic

- add, sub, mul, div, pow, sqrt
- sin, cos, tan, exp, log
- eq, neq, lt, le, gt, ge
- minof, maxof, clamp

### BLAS/LAPACK

- matmul, gemm, dot, transpose
- lu, qr, svd, inverse, solve

### Index

- at, slice, select, assign
- row, rows, col, cols

### Signal/Image

- fft, ifft, convolve, filter, resize, rotate

## dtype-next Integration Details

### Create from dtype-next

- If `dtype/as-native-buffer` returns non-nil, use it directly as host buffer.
- If not, copy via dtype-next to native heap buffer.

### Return dtype-next

- Provide `to-native-buffer` returning dtype-next container backed by native
  memory.
- Provide `to-host` to return persistent vectors (copying)

### Zero-Copy Semantics

- Zero-copy is limited to JVM-side native buffers. Host<->GPU is always a copy.
- Document this clearly in API docs and README.

## Error Handling

- Use integration.jvm-integration/check! for FFI errors.
- Provide `with-error-context` helper that calls
  integration.error/get-last-error on exception to enrich messages.

## Example API Usage

```clojure
(require '[org.soulspace.arrayfire.api.array :as af]
         '[org.soulspace.arrayfire.api.arith :as arith]
         '[tech.v3.datatype :as dtype])

(af/init!)

;; Create from Clojure data
(with-array [a (af/array [1.0 2.0 3.0])
             b (af/array [10.0 20.0 30.0])]
  (-> (arith/add a b)
      (af/to-host :f64 3)))

;; dtype-next native buffer zero-copy on host
(let [tensor (dtype/make-container :native-heap :float64 [1.0 2.0 3.0])]
  (with-array [a (af/array tensor)]
    (af/to-native-buffer a :float64 3)))
```

## Incremental Implementation Plan

1. Add api.array and api.core namespaces with coercion and resource helpers.
2. Wire dtype-next conversions and document zero-copy semantics.
3. Add arithmetic and BLAS wrappers with arity sugar and aliasing.
4. Add indexing convenience helpers (row/col/slice), with tests.
5. Expand to signal and image modules.
6. Update README and CHANGELOG to reflect API layer.

## Testing Strategy

- Unit tests for coercion: collection, primitive array, dtype-next, MemorySegment.
- Resource tests: retain/release, with-array scope.
- Roundtrip tests: array -> native buffer -> array.
- Backend-conditional tests for CUDA print limitations.

## Open Questions

- Preferred namespace prefix: org.soulspace.arrayfire.api.* or org.soulspace.arrayfire.*?
- Should the API layer expose AFArray directly or wrap it in a record?
- Should zero-copy be opt-in via `:copy?` or based on input type?

