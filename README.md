# ArrayFire-CLJ
Arrayfire-clj provides a Clojure wrapper around the
[ArrayFire](https://github.com/arrayfire/arrayfire) Unified API using Java 22+
Foreign Function & Memory API via the [Coffi](https://github.com/IGJoshua/coffi)
library. It brings ArrayFire to the JVM, with some unique features not 
available otherwise on the JVM, like GPU enabled linear algebra over the field
of complex numbers. 

ArrayFire is a general-purpose tensor library that simplifies the software development process for the parallel architectures found in CPUs, GPUs, and other hardware acceleration devices.

ArrayFire provides:
* Hundreds of functions in the following categories
  * Array handling
  * Computer vision
  * Image processing
  * Linear algebra
  * Machine learning
  * Standard math
  * Signal Processing
  * Statistics
  * Vector algorithms
* Cross-platform compatibility with support for CUDA, oneAPI, OpenCL,
  and native CPU on Windows, Mac, and Linux

[![Clojars Project](https://img.shields.io/clojars/v/org.soulspace/arrayfire-clj.svg)](https://clojars.org/org.soulspace/arrayfire-clj)
[![cljdoc badge](https://cljdoc.org/badge/org.soulspace/arrayfire-clj)](https://cljdoc.org/d/org.soulspace/arrayfire-clj)
![GitHub](https://img.shields.io/github/license/lsolbach/arrayfire-clj)

## Requirements

- **Java 22+** with FFM API support
- **ArrayFire 3.10 library** installed
- **JVM flags**: `--enable-native-access=ALL-UNNAMED`

## Installing ArrayFire

See [ArrayFire Installer](https://arrayfire.org/docs/installing.htm) for the
installation instructions for your operating system.

## Configuration

### JVM Options

For Leiningen, add to `project.clj`:

```clojure
:jvm-opts ["--enable-native-access=ALL-UNNAMED"]
```

For deps.edn, add to your alias:

```clojure
{:aliases
 {:dev {:jvm-opts ["--enable-native-access=ALL-UNNAMED"]}}}
```

## Usage

Example usage:

```clojure
(require '[org.soulspace.arrayfire.core :as af])

;; Initialize ArrayFire
(af/init!)

;; Print device information
(af/info)

;; Create arrays and perform operations with doubles
(let [a (af/create-array-f64 [1.0 2.0 3.0] [3])
      b (af/create-array-f64 [10.0 20.0 30.0] [3])
      c (af/add a b)]
  (println (af/to-host-f64 c 3))
  ;; => [11.0 22.0 33.0]
  (af/release a)
  (af/release b)
  (af/release c))

;; Complex number operations
(let [a (af/create-array-c64 [[1.0 2.0] [3.0 4.0]] [2])
      b (af/create-array-c64 [[5.0 6.0] [7.0 8.0]] [2])
      c (af/add a b)]
  (println (af/to-host-c64 c 2))
  ;; => [[6.0 8.0] [10.0 12.0]]
  (af/release a)
  (af/release b)
  (af/release c))

;; dtype-next integration (zero-copy on host)
(require '[tech.v3.datatype :as dtype])

(let [tensor (dtype/make-container :native-heap :float64 [1.0 2.0 3.0])
      arr (af/create-array-from-tensor tensor)
      result (af/to-native-buffer arr :float64 3)]
  (println (vec result))
  ;; => [1.0 2.0 3.0]
  (af/release arr))
```

## Rationale
Complex tensors are at the heart of the simulation of quantum systems, e.g.
quantum computers. Many other physical systems are also described by
formulas involving complex numbers. 

The JVM and Clojure are lacking a fast, GPU enabled tensor library supporting
tensors over the field of complex numbers. By adapting ArrayFire to Clojure
on the JVM, this gap is closed.

## Development

### Running Tests

```bash
lein test
```

## Implementation Status
*Alpha* but usable

Done:
* All ArrayFire 3.10 C API bindings
* JVM/Clojure integration of the ArrayFire Unified API
  * error/exception handling 
  * resource management

Planned:
* Idiomatic Clojure API
* dtype-next integration

## ArrayFire Backends
As ArrayFire-CLJ is wrapping the ArrayFire library, the support of the
different backends (CUDA, oneAPI, OpenCL and CPU) directly depends on their
implementation status in ArrayFire itself. The CUDA and CPU backends are the
most mature backends, the OpenCL backend is mature in its functional
implementation but maybe not as optimized. The oneAPI backend is lacking in
the implementation of these areas:

* image processing
* computer vision
* drawing operations via Forge

### ArrayFire Backend Selection
The order of the automatic backend selection by the Unified API is
CUDA -> oneAPI -> OpenCL -> CPU.

This might be a problem on systems with an Intel GPU when you need
functionality not yet implemented on the oneAPI. You can use the functions
of the device namespace to select the backend, e.g. OpenCL or CPU.

## Copyright
© 2026 Ludger Solbach

arrayfire-clj is not affiliated with or endorsed by ArrayFire. The ArrayFire literal mark is used under a limited license granted by ArrayFire the trademark holder in the United States and other countries.

## License
Eclipse Public License 1.0 (EPL1.0)
