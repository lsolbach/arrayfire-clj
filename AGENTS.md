# arrayfire-clj: Arrayfire for Clojure

# Goal
The goal of this project 'arrayfire-clj' is to build a performant and
production-ready Clojure wrapper library for the ArrayFire math library using
the coffi library to build an Java 22 foreign function interface (FFI) for it.

'arrayfire-clj' should provide all types and functions of ArrayFire, especially
the types and functions for complex numbers.

'arrayfire-clj' should use dtype-next for zero-copy tensors where feasable. 

# Use Cases
* Quantum computing
* Physics simulations
* Machine learning and artificial intelligence

# References
The source code for the used libraries should be available in the 'reference' folder or on GitHub:
* [arrayfire](https://github.com/arrayfire/arrayfire)
* [coffi](https://github.com/IGJoshua/coffi)
* [dtype-next](https://github.com/cnuernber/dtype-next)
* [tech.resource](https://github.com/techascent/tech.resource)

# Architecture and Design

## Design Principles

### Zero-Copy
No unneccessary copying of data.

### Simplicity and Composeability
Features should be simple, uncomplected and composeable.

## Layers
arrayfire-clj has a layered architecture.

### FFI Layer
The FFI Layer provides only the FFI bindings to the ArrayFire API, as true to
the original ArrayFire API structure as possible, so that the ArrayFire
API documentation can be used for this layer. This structure makes it easy to
keep arrayfire-clj in sync with future ArrayFire versions.
The FFI layer also loads the ArrayFire libraries and provides definitions for
the ArrayFire constants.

### Integration Layer
The integration layer provides the integration of ArrayFire in the Clojure and
JVM ecosystem: 
* initialization of the ArrayFire libraries
* resource management of the created ArrayFire resources (e.g. array memory
  and handles) in Clojure on the JVM.
  * all Arena based memory allocations must be done in the integration layer
  * all memory allocations must use the Arena bound to
    org.soulspace.arrayfire.integration.base.memory/*af-arena* by default,
    if an Arena is bound
* error and exception handling
* dtype-next integration

The structure of the integration layer follows the ArrayFire Unified API.
The name of the functions follow Clojure semantic conventions, e.g. for
predicates and functions with side effects.
The functions in this layer should enforce their invariants.

### Clojure API Layer
The Clojure API layer provides an idiomatic Clojure API for the ArrayFire
features. The functions in this layer use Clojure conventions with regards
to naming, keywords, argument order and return values. Spec is used in the
Clojure API layer to define semantic validation and enforce the API
contracts.

All API functions assert they are called within an arrayfire region

