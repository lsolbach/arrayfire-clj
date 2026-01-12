# ArrayFire for Clojure

Arrayfire-clj provides a minimal Clojure wrapper around the ArrayFire C API
using Java 22+ Foreign Function & Memory API via the Coffi library.

## Requirements

- **Java 22+** with FFM API support
- **ArrayFire library** installed (CPU, CUDA, or OpenCL backend)
- **JVM flags**: `--enable-native-access=ALL-UNNAMED`

## Installing ArrayFire

See [ArrayFire Installer](https://arrayfire.org/docs/installing.htm) for the
installation instructions for your operating system.

## Configuration

### Environment Variables

You can set `ARRAYFIRE_LIB` to specify the library name if needed:

```bash
export ARRAYFIRE_LIB="af"  # Default
# or
export ARRAYFIRE_LIB="afcpu"  # CPU backend specifically
# or
export ARRAYFIRE_LIB="afcuda" # CUDA backend specifically
```

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

## Implementation Status

*Experimental* Currently implemented functions:

- `init!` - Initialize ArrayFire runtime
- `info` - Display device information
- `create-array-f32` - Create float32 arrays
- `create-array-f64` - Create float64 arrays
- `create-array-c32` - Create complex32 arrays
- `create-array-c64` - Create complex64 arrays
- `create-array-s32` - Create int32 arrays
- `create-array-u32` - Create uint32 arrays
- `create-array-s64` - Create int64 arrays
- `create-array-u64` - Create uint64 arrays
- `to-host-f32` - Copy float32 array to host
- `to-host-f64` - Copy float64 array to host
- `to-host-c32` - Copy complex32 array to host
- `to-host-c64` - Copy complex64 array to host
- `to-host-s32` - Copy int32 array to host
- `to-host-u32` - Copy uint32 array to host
- `to-host-s64` - Copy int64 array to host
- `to-host-u64` - Copy uint64 array to host
- `create-array-from-native` - Create array from dtype-next native buffer (zero-copy)
- `create-array-from-tensor` - Create array from dtype-next tensor
- `to-native-buffer` - Copy array to dtype-next native buffer
- `release` - Release array memory
- `add` - Element-wise array addition

More operations will be added as needed.

## Development

### Running Tests

```bash
lein test
```

## Copyright
© 2025 Ludger Solbach

## License
Eclipse Public License 1.0 (EPL1.0)
