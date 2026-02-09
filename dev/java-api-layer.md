# Idiomatic Java API Layer Design

This document proposes an idiomatic Java API layer built on top of the
integration layer. It focuses on a Java-friendly surface, predictable
resource management, and optional dtype-next interop via native buffers.

## Goals

- Provide a Java-first API that is safe, explicit, and ergonomic.
- Preserve ArrayFire performance characteristics with minimal overhead.
- Make resource management explicit with AutoCloseable and try-with-resources.
- Support native buffer interop for zero-copy on the JVM side where possible.

## Non-Goals

- Hiding all native lifecycle details. Java callers should still choose
  when to release resources.
- Replacing the Clojure integration layer; this is a thin Java facade.

## High-Level Structure

Proposed Java packages:

- org.soulspace.arrayfire.api
  - Core runtime and device configuration
- org.soulspace.arrayfire.api.array
  - Array creation, conversion, shape, dtype
- org.soulspace.arrayfire.api.arith
  - Arithmetic and element-wise math
- org.soulspace.arrayfire.api.blas
  - Matmul, dot, transpose
- org.soulspace.arrayfire.api.lapack
  - Decompositions, solvers, inverse
- org.soulspace.arrayfire.api.signal
  - FFT, convolution, filters
- org.soulspace.arrayfire.api.image
  - IO, transforms, filters

## Data Model

### AFArray Handle

- Use the existing `AFArray` (AutoCloseable) from the integration layer.
- Provide a thin Java wrapper type `AF` to attach metadata lazily if needed.

Example:

- AFArray: actual native handle
- AF: { AFArray handle, DType dtype, long[] dims }

## Resource Management

- AFArray already implements AutoCloseable via Cleaner.
- Java layer should expose helper factory methods returning AFArray.
- Encourage try-with-resources for deterministic release.

Example:

```
try (AFArray a = Arrays.fromDoubles(new double[]{1,2,3}, new long[]{3});
     AFArray b = Arrays.fromDoubles(new double[]{10,20,30}, new long[]{3})) {
  AFArray c = Arith.add(a, b);
  double[] out = Arrays.toHostDoubles(c, 3);
  c.close();
}
```

## DType Model

Define a Java enum `DType` mapping to ArrayFire constants:

- F32, F64, C32, C64, S32, U32, S64, U64, S16, U16, U8, B8

Helpers:

- `DType.of(int afConst)`
- `int afConst()`
- `int elementSize()`
- `boolean isComplex()`

## Array Creation

Proposed Java factory methods (static):

- `Arrays.fromFloats(float[] data, long[] dims)`
- `Arrays.fromDoubles(double[] data, long[] dims)`
- `Arrays.fromInts(int[] data, long[] dims)`
- `Arrays.fromLongs(long[] data, long[] dims)`
- `Arrays.fromComplex(float[] interleaved, long[] dims)`
- `Arrays.fromComplex(double[] interleaved, long[] dims)`
- `Arrays.fromNative(MemorySegment seg, long[] dims, DType dtype)`

Interop with dtype-next (optional for Java):

- Provide `Arrays.fromNativeBuffer(ByteBuffer or MemorySegment)`
- Keep the API generic to accept a MemorySegment from FFM

## Data Transfer

- `Arrays.toHostDoubles(AFArray arr, long n)`
- `Arrays.toHostFloats(AFArray arr, long n)`
- `Arrays.toHostInts(AFArray arr, long n)`
- `Arrays.toNativeBuffer(AFArray arr, DType dtype, long n)` -> MemorySegment

Note: Host<->device copy is always required.

## Naming and Overloads

- Java method names are verbose and explicit.
- Use overloading for primitive types instead of generics.
- Keep `!` naming out of Java. Use suffix `InPlace` for mutating ops.

Examples:

- `Arith.add(AFArray a, AFArray b)`
- `Arith.addInPlace(AFArray a, AFArray b)`
- `Blas.matmul(AFArray a, AFArray b)`

## Error Handling

- On error, throw runtime exceptions with ArrayFire error code and
  last error message if available.
- Provide `ArrayFireException` with fields: code, where, message.

## Example Usage

```java
import org.soulspace.arrayfire.api.ArrayFire;
import org.soulspace.arrayfire.api.array.Arrays;
import org.soulspace.arrayfire.api.arith.Arith;

public class Example {
  public static void main(String[] args) {
    ArrayFire.init();
    ArrayFire.info();

    try (AFArray a = Arrays.fromDoubles(new double[]{1, 2, 3}, new long[]{3});
         AFArray b = Arrays.fromDoubles(new double[]{10, 20, 30}, new long[]{3});
         AFArray c = Arith.add(a, b)) {
      double[] out = Arrays.toHostDoubles(c, 3);
      // out = [11, 22, 33]
    }
  }
}
```

## Implementation Plan

1. Java-friendly DType enum and ArrayFireException.
2. Arrays factory methods for primitives and MemorySegment.
3. Core device and runtime helpers (init, info, backend).
4. Arithmetic and BLAS wrappers.
5. Signal and image wrappers.
6. Update docs and README to mention Java API availability.

## Open Questions

- Should Java API return AFArray directly or a wrapper type with metadata?
- Should we include optional convenience methods for shape inference?
- Is a builder-style API useful for array creation?

