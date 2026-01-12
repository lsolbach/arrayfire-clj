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

;; Create arrays and perform operations
(let [a (af/create-array [1.0 2.0 3.0] [3])
      b (af/create-array [10.0 20.0 30.0] [3])
      c (af/add a b)]
  (println (seq (af/to-host c 3)))
  ;; => (11.0 22.0 33.0)
  (af/release a)
  (af/release b)
  (af/release c))
```

## Implementation Status

Currently implemented functions:

- `init!` - Initialize ArrayFire runtime
- `info` - Display device information  
- `create-array` - Create arrays from Clojure vectors
- `release` - Release array memory
- `add` - Element-wise array addition
- `to-host` - Copy array data back to host

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
