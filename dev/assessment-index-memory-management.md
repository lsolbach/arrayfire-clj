# Assessment: integration.index Memory Management

## Summary

The resource management in `integration.index.clj` is **CORRECT**. All indexing functions properly use `jvm/af-array-new` to wrap returned handles.

## Assessment Date

2025

## Question

Does the integration.index namespace correctly manage ArrayFire array references, particularly for indexing operations that might return views/slices sharing memory with input arrays?

## Answer

**YES - The current implementation is correct.**

## Analysis

### ArrayFire C++ Implementation

Investigation of the ArrayFire source code (`reference/arrayfire/`) reveals:

1. **Handle Creation**
   - `af_index`, `af_lookup`, `af_index_gen` all call `getHandle(createSubArray(...))`
   - Located in: `reference/arrayfire/src/api/c/index.cpp`

2. **getHandle Function** (`reference/arrayfire/src/api/c/handle.hpp:83-86`)
   ```cpp
   template<typename T>
   af_array getHandle(const detail::Array<T> &A) {
       detail::Array<T> *ret = new detail::Array<T>(A);  // Copy constructor
       return static_cast<af_array>(ret);
   }
   ```

3. **Array Copy Constructor** (`reference/arrayfire/src/backend/opencl/Array.hpp:35`)
   ```cpp
   Array(const Array<T> &other) = default;
   ```
   - Default copy constructor copies all members
   - The `data` member is `std::shared_ptr<cl::Buffer>`
   - Copying shared_ptr **automatically increments refcount**

4. **Memory Sharing**
   - While `createSubArray` may create views that share GPU memory
   - The `af_array*` handle itself is **always a new C++ object**
   - Reference tracking is handled by `std::shared_ptr` inside `Array<T>`
   - When copied, shared_ptr refcount is incremented automatically

### JVM Integration Pattern

From `integration/jvm_integration.clj`:

- **af-array-new**: Wraps handles returned from ArrayFire C API (refcount already 1)
- **af-array-retained**: For wrapping existing JVM-side handles we want to keep

### Conclusion

ArrayFire indexing functions return **new handles** with proper reference tracking, even when the underlying GPU memory is shared. The returned handle has:
- New C++ `Array<T>*` object
- Shared_ptr to GPU buffer (refcount properly incremented)
- Independent ownership from JVM's perspective

Therefore, `jvm/af-array-new` is the correct wrapper function.

## Functions Verified

All functions in integration.index.clj correctly use `jvm/af-array-new`:

1. `index` - Sequence-based indexing
2. `lookup` - Array-based indexing  
3. `index-gen` - Generalized indexing
4. `assign-seq` - Sequence-based assignment
5. `assign-gen` - Generalized assignment
6. `slice` - Helper calling index

## Documentation Updates

Updated the namespace docstring in [integration/index.clj](../src/org/soulspace/arrayfire/integration/index.clj) to:
- Remove TODO comments about memory management
- Add comprehensive memory management explanation
- Document why af-array-new is correct for these operations

## Key Insights

1. **ArrayFire's Design**: Always returns new handles, even for views
2. **Refcount Location**: Managed by C++ shared_ptr, not af_array* count
3. **Copy Semantics**: Copy constructor shares memory but increments refcount
4. **JVM Mapping**: New handle from C → use af-array-new

## References

- ArrayFire source: `reference/arrayfire/src/api/c/index.cpp`
- Handle management: `reference/arrayfire/src/api/c/handle.hpp`
- Array implementation: `reference/arrayfire/src/backend/opencl/Array.hpp`
- JVM integration: `src/org/soulspace/arrayfire/integration/jvm_integration.clj`
