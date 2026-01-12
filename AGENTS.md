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

