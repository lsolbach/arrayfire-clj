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
* [tech.resource]()

# Architecture and Design

## Principles

### Zero-Copy
No unneccessary copying of data.

## Layers
arrayfire-clj should have a layered architecture.

### FFI Layer
The FFI Layer provides only the FFI bindings to the ArrayFire API, as true to
the original ArrayFire API structure as possible, so that the ArrayFire
API documentation can be used for this layer. This structure makes it easy to
keep arrayfire-clj in sync with future ArrayFire versions.

### Integration Layer
The integration layer provides the integration of ArrayFire in the Clojure and
JVM ecosystem: 
* resource management of the created ArrayFire resources (e.g. array memory
  and handles) in Clojure on the JVM.
* error and exception handling
* dtype-next integration

### Clojure API Layer
The Clojure API layer provides an ideomatic Clojure API for the ArrayFire
features.

