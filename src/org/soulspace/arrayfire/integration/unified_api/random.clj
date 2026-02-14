(ns org.soulspace.arrayfire.integration.unified-api.random
  "Integration of the ArrayFire random number generation FFI bindings with
   error handling and resource management on the JVM.
   
   Random Number Generation in ArrayFire:
   
   ArrayFire provides GPU-accelerated random number generation with multiple
   high-quality pseudorandom number generators (PRNGs). This namespace provides
   both convenience functions using the default engine and fine-grained control
   with custom random engines.
   
   **Quick Start with Default Engine**:
   
   ```clojure
   (require '[org.soulspace.arrayfire.integration.random :as rand])
   
   ;; Set seed for reproducibility
   (rand/set-seed! 42)
   
   ;; Generate uniform random array [0, 1)
   (def uniform-data (rand/randu [512 512] jvm/AF_DTYPE_F32))
   
   ;; Generate normal random array N(0,1)
   (def normal-data (rand/randn [1000] jvm/AF_DTYPE_F64))
   ```
   
   **Custom Random Engines**:
   
   Use custom engines for independent random streams or specific algorithms:
   
   ```clojure
   ;; Create a Philox engine with seed
   (def engine (rand/create-engine :philox 12345))
   
   ;; Generate with custom engine
   (def data (rand/random-uniform [256 256] jvm/AF_DTYPE_F32 engine))
   
   ;; Change seed
   (rand/set-engine-seed! engine 99999)
   
   ;; Switch algorithm
   (rand/set-engine-type! engine :mersenne)
   
   ;; Release when done
   (rand/release-engine! engine)
   ```
   
   **Engine Types**:
   
   - `:philox` (default) - Fast, parallel-friendly counter-based RNG
   - `:threefry` - Counter-based RNG with cryptographic origins
   - `:mersenne` - Traditional high-quality PRNG with long period
   
   **Random Distributions**:
   
   - **Uniform**: Values in [0, 1) for floats, full range for integers
   - **Normal**: Gaussian distribution with mean=0, std=1
   
   **Resource Management**:
   
   Custom random engines must be explicitly released with `release-engine!`.
   The default engine is managed by ArrayFire and should not be released.
   
   **Thread Safety**:
   
   Each device maintains its own default random engine. Custom engines are
   independent and can be used concurrently across threads.
   
   See also:
   - ArrayFire random documentation for statistical properties
   - integration.device for backend management"
  (:require [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.definitions :as defs]
            [org.soulspace.arrayfire.ffi.c-api.random :as random]
            [org.soulspace.arrayfire.integration.base.error :refer [check!]]
            [org.soulspace.arrayfire.integration.base.memory :as bmem]
            [org.soulspace.arrayfire.integration.base.resource :as res]))

;;;
;;; Random Engine Type Constants
;;;

;; Re-export engine type constants from FFI layer
;; Engine type keyword mapping
(def ^:private engine-type-map
  {:philox defs/AF_RANDOM_ENGINE_PHILOX
   :threefry defs/AF_RANDOM_ENGINE_THREEFRY
   :mersenne defs/AF_RANDOM_ENGINE_MERSENNE
   :default defs/AF_RANDOM_ENGINE_DEFAULT})

(def ^:private engine-type-names
  {defs/AF_RANDOM_ENGINE_PHILOX "Philox"
   defs/AF_RANDOM_ENGINE_THREEFRY "Threefry"
   defs/AF_RANDOM_ENGINE_MERSENNE "Mersenne"})

;;;
;;; Helper Functions
;;;

(defn- engine-type->int
  "Convert engine type keyword or integer to integer constant."
  [engine-type]
  (cond
    (keyword? engine-type) (or (get engine-type-map engine-type)
                                (throw (ex-info "Invalid engine type keyword"
                                                {:type engine-type
                                                 :valid-types (keys engine-type-map)})))
    (integer? engine-type) engine-type
    :else (throw (ex-info "Engine type must be keyword or integer"
                          {:type engine-type}))))

(defn engine-type-name
  "Get the name of an engine type.
   
   Parameters:
   - engine-type: Engine type constant (integer)
   
   Returns:
   String name of the engine type
   
   Example:
   ```clojure
   (engine-type-name RANDOM-ENGINE-PHILOX)
   ; => \"Philox\"
   ```"
  [engine-type]
  (get engine-type-names engine-type "Unknown"))

;;;
;;; Random Engine Management
;;;

(defn create-engine
  "Create a new random engine with specified type and seed.
   
   Creates an independent random engine that can be used for generating
   random numbers without affecting the default engine. The engine must
   be explicitly released with `release-engine!` when no longer needed.
   
   Parameters:
   - engine-type: Engine type - keyword (:philox, :threefry, :mersenne)
                  or integer constant (default :philox)
   - seed: Initial seed value (default 0)
   
   Returns:
   Random engine handle (long) wrapped as MemorySegment
   
   Example:
   ```clojure
   ;; Create Philox engine with seed 42
   (def engine (create-engine :philox 42))
   
   ;; Use for random generation
   (random-uniform [512 512] jvm/AF_DTYPE_F32 engine)
   
   ;; Release when done
   (release-engine! engine)
   ```
   
   See also:
   - release-engine!: Free engine resources
   - get-default-engine: Get the default engine"
  ([engine-type]
   (create-engine engine-type 0))
  ([engine-type seed]
   (let [engine-ptr (mem/alloc 8)
         type-int (engine-type->int engine-type)]
     (check! (random/af-create-random-engine engine-ptr type-int (long seed))
                 "af-create-random-engine")
     (mem/read-long engine-ptr 0))))

(defn retain-engine
  "Retain (copy) a random engine handle.
   
   Creates a copy of the random engine handle, incrementing its reference
   count. Both handles refer to the same underlying engine state.
   
   Parameters:
   - engine: Random engine handle
   
   Returns:
   New random engine handle
   
   See also:
   - release-engine!: Decrement reference count"
  [engine]
  (let [out-ptr (mem/alloc 8)]
    (check! (random/af-retain-random-engine out-ptr (mem/as-segment engine))
                "af-retain-random-engine")
    (mem/read-long out-ptr 0)))

(defn release-engine!
  "Release (free) a random engine.
   
   Decrements the reference count of the random engine and frees
   resources when count reaches zero. Should be called for all custom
   engines created with `create-engine`.
   
   **Important**: Do NOT call this on the default engine obtained from
   `get-default-engine` - it is managed by ArrayFire.
   
   Parameters:
   - engine: Random engine handle to release
   
   Returns:
   nil
   
   Example:
   ```clojure
   (let [engine (create-engine :philox 42)]
     (try
       (random-uniform [100] jvm/AF_DTYPE_F32 engine)
       (finally
         (release-engine! engine))))
   ```
   
   See also:
   - create-engine: Create engine"
  [engine]
  (check! (random/af-release-random-engine (mem/as-segment engine))
              "af-release-random-engine")
  nil)

;;;
;;; Random Engine Configuration
;;;

(defn set-engine-type!
  "Set the type of a random engine.
   
   Changes the algorithm used by the random engine. Resets the engine
   state with the current seed.
   
   Parameters:
   - engine: Random engine handle
   - engine-type: New engine type - keyword (:philox, :threefry, :mersenne)
                  or integer constant
   
   Returns:
   The updated engine handle
   
   Example:
   ```clojure
   (def engine (create-engine :philox 42))
   
   ;; Switch to Mersenne Twister
   (set-engine-type! engine :mersenne)
   ```
   
   See also:
   - get-engine-type: Query current type"
  [engine engine-type]
  (let [type-int (engine-type->int engine-type)
        engine-ptr (mem/alloc 8)]
    (mem/write-long engine-ptr 0 engine)
    (check! (random/af-random-engine-set-type engine-ptr type-int)
                "af-random-engine-set-type")
    engine))

(defn get-engine-type
  "Get the type of a random engine.
   
   Returns the algorithm type currently used by the engine.
   
   Parameters:
   - engine: Random engine handle
   
   Returns:
   Engine type as integer constant
   
   Example:
   ```clojure
   (def engine (create-engine :philox 42))
   (get-engine-type engine)
   ; => 100 (RANDOM-ENGINE-PHILOX)
   
   (engine-type-name (get-engine-type engine))
   ; => \"Philox\"
   ```
   
   See also:
   - set-engine-type!: Change engine type
   - engine-type-name: Get type name"
  [engine]
  (let [type-ptr (mem/alloc 4)]
    (check! (random/af-random-engine-get-type type-ptr (mem/as-segment engine))
                "af-random-engine-get-type")
    (mem/read-int type-ptr 0)))
   
(defn set-engine-seed!
  "Set the seed of a random engine.
   
   Changes the seed and resets the engine state. Use for reproducible
   random sequences.
   
   Parameters:
   - engine: Random engine handle
   - seed: New seed value (64-bit unsigned integer)
   
   Returns:
   The updated engine handle
   
   Example:
   ```clojure
   (def engine (create-engine :philox 42))
   
   ;; Change seed for different sequence
   (set-engine-seed! engine 99999)
   ```
   
   See also:
   - get-engine-seed: Query current seed"
  [engine seed]
  (let [engine-ptr (mem/alloc 8)]
    (mem/write-long engine-ptr 0 engine)
    (check! (random/af-random-engine-set-seed engine-ptr (long seed))
                "af-random-engine-set-seed")
    engine))

(defn get-engine-seed
  "Get the seed of a random engine.
   
   Returns the seed value used to initialize the engine.
   
   Parameters:
   - engine: Random engine handle
   
   Returns:
   Seed value as long
   
   Example:
   ```clojure
   (def engine (create-engine :philox 12345))
   (get-engine-seed engine)
   ; => 12345
   ```
   
   See also:
   - set-engine-seed!: Change seed"
  [engine]
  (let [seed-ptr (mem/alloc 8)]
    (check! (random/af-random-engine-get-seed seed-ptr (mem/as-segment engine))
                "af-random-engine-get-seed")
    (mem/read-long seed-ptr 0)))

;;;
;;; Default Random Engine
;;;

(defn get-default-engine
  "Get the default random engine for the current device.
   
   Returns a handle to the per-device default random engine. Each device
   maintains its own default engine.
   
   **Important**: Do NOT release the default engine - it's managed by ArrayFire.
   
   Returns:
   Default random engine handle
   
   Example:
   ```clojure
   (def default-engine (get-default-engine))
   
   ;; Use with random generation
   (random-uniform [100] jvm/AF_DTYPE_F32 default-engine)
   
   ;; Do NOT call (release-engine! default-engine)
   ```
   
   See also:
   - set-default-engine-type!: Change default engine type"
  []
  (let [engine-ptr (mem/alloc 8)]
    (check! (random/af-get-default-random-engine engine-ptr)
                "af-get-default-random-engine")
    (mem/read-long engine-ptr 0)))

(defn set-default-engine-type!
  "Set the type of the default random engine.
   
   Changes the algorithm used by the default engine for the current device.
   
   Parameters:
   - engine-type: Engine type - keyword (:philox, :threefry, :mersenne)
                  or integer constant
   
   Returns:
   nil
   
   Example:
   ```clojure
   ;; Set default to Mersenne Twister
   (set-default-engine-type! :mersenne)
   
   ;; All subsequent randu/randn calls use Mersenne
   (randu [100] jvm/AF_DTYPE_F32)
   ```
   
   See also:
   - get-default-engine: Get default engine handle"
  [engine-type]
  (let [type-int (engine-type->int engine-type)]
    (check! (random/af-set-default-random-engine-type type-int)
                "af-set-default-random-engine-type")
    nil))

;;;
;;; Random Number Generation with Custom Engine
;;;

(defn random-uniform
  "Generate uniformly distributed random numbers using a custom engine.
   
   Creates an AFArray of random numbers from uniform distribution [0, 1)
   for floating point, or full type range for integers.
   
   Parameters:
   - dims: Vector of dimensions, e.g., [512 512] or [1000]
   - dtype: Data type constant (jvm/AF_DTYPE_F32, etc.)
   - engine: Random engine handle
   
   Returns:
   AFArray with uniformly distributed random values
   
   Distribution:
   - Float/complex: [0.0, 1.0) uniform
   - Integers: Full type range uniform
   - Boolean: 0 or 1 with equal probability
   
   Example:
   ```clojure
   (def engine (create-engine :philox 42))
   
   ;; Generate 512x512 float32 uniform random array
   (def data (random-uniform [512 512] jvm/AF_DTYPE_F32 engine))
   
   (release-engine! engine)
   ```
   
   See also:
   - randu: Use default engine
   - random-normal: Normal distribution"
  [dims dtype engine]
  (let [out (res/native-af-array-pointer)
        ndims (count dims)
        dims-seg (bmem/dims->segment dims)]
    (check! (random/af-random-uniform out ndims dims-seg dtype (mem/as-segment engine))
                "af-random-uniform")
    (res/af-array-new (res/deref-af-array out))))

(defn random-normal
  "Generate normally distributed random numbers using a custom engine.
   
   Creates an AFArray of random numbers from normal distribution with
   mean=0 and standard deviation=1.
   
   Parameters:
   - dims: Vector of dimensions, e.g., [512 512] or [1000]
   - dtype: Data type constant (jvm/AF_DTYPE_F32, jvm/AF_DTYPE_F64, etc.)
            Only floating-point types supported (f16, f32, f64, c32, c64)
   - engine: Random engine handle
   
   Returns:
   AFArray with normally distributed random values N(0, 1)
   
   Distribution:
   - Real: N(0, 1) Gaussian
   - Complex: Real and imaginary parts independently N(0, 1)
   
   Example:
   ```clojure
   (def engine (create-engine :philox 42))
   
   ;; Generate 1000 float64 normal random values
   (def data (random-normal [1000] jvm/AF_DTYPE_F64 engine))
   
   (release-engine! engine)
   ```
   
   See also:
   - randn: Use default engine
   - random-uniform: Uniform distribution"
  [dims dtype engine]
  (let [out (res/native-af-array-pointer)
        ndims (count dims)
        dims-seg (bmem/dims->segment dims)]
    (check! (random/af-random-normal out ndims dims-seg dtype (mem/as-segment engine))
                "af-random-normal")
    (res/af-array-new (res/deref-af-array out))))

;;;
;;; Random Number Generation with Default Engine
;;;

(defn randu
  "Generate uniformly distributed random numbers using default engine.
   
   Creates an AFArray of random numbers from uniform distribution using
   the default random engine for the current device. This is the most
   convenient way to generate random numbers.
   
   Parameters:
   - dims: Vector of dimensions, e.g., [512 512] or [1000]
   - dtype: Data type constant (jvm/AF_DTYPE_F32, etc.)
   
   Returns:
   AFArray with uniformly distributed random values
   
   Distribution:
   - Float/complex: [0.0, 1.0) uniform
   - Integers: Full type range uniform
   - Boolean: 0 or 1 with equal probability
   
   Example:
   ```clojure
   ;; Set seed for reproducibility
   (set-seed! 42)
   
   ;; Generate 512x512 float32 uniform random array
   (def data (randu [512 512] jvm/AF_DTYPE_F32))
   
   ;; Generate 1000 integer random values
   (def ints (randu [1000] jvm/AF_DTYPE_S32))
   ```
   
   See also:
   - random-uniform: Use custom engine
   - set-seed!: Set seed for reproducibility
   - randn: Normal distribution"
  [dims dtype]
  (let [out (res/native-af-array-pointer)
        ndims (count dims)
        dims-seg (bmem/dims->segment dims)]
    (check! (random/af-randu out ndims dims-seg dtype)
                "af-randu")
    (res/af-array-new (res/deref-af-array out))))

(defn randn
  "Generate normally distributed random numbers using default engine.
   
   Creates an AFArray of random numbers from normal distribution N(0,1)
   using the default random engine for the current device.
   
   Parameters:
   - dims: Vector of dimensions, e.g., [512 512] or [1000]
   - dtype: Data type constant (jvm/AF_DTYPE_F32, jvm/AF_DTYPE_F64, etc.)
            Only floating-point types supported (f16, f32, f64, c32, c64)
   
   Returns:
   AFArray with normally distributed random values N(0, 1)
   
   Distribution:
   - Real: N(0, 1) Gaussian
   - Complex: Real and imaginary parts independently N(0, 1)
   
   Example:
   ```clojure
   ;; Set seed for reproducibility
   (set-seed! 42)
   
   ;; Generate 1000 float64 normal random values
   (def data (randn [1000] jvm/AF_DTYPE_F64))
   
   ;; Generate 256x256 complex normal values
   (def complex-data (randn [256 256] jvm/AF_DTYPE_C32))
   ```
   
   See also:
   - random-normal: Use custom engine
   - set-seed!: Set seed for reproducibility
   - randu: Uniform distribution"
  [dims dtype]
  (let [out (res/native-af-array-pointer)
        ndims (count dims)
        dims-seg (bmem/dims->segment dims)]
    (check! (random/af-randn out ndims dims-seg dtype)
                "af-randn")
    (res/af-array-new (res/deref-af-array out))))

;;;
;;; Default Engine Seed Management
;;;

(defn set-seed!
  "Set the seed of the default random engine.
   
   Sets the seed for the default random engine on the current device.
   Use for reproducible random sequences.
   
   Parameters:
   - seed: Seed value (64-bit unsigned integer)
   
   Returns:
   nil
   
   Example:
   ```clojure
   ;; Set seed for reproducibility
   (set-seed! 42)
   
   ;; Generate reproducible random data
   (def data1 (randu [100] jvm/AF_DTYPE_F32))
   
   ;; Reset seed to regenerate same sequence
   (set-seed! 42)
   (def data2 (randu [100] jvm/AF_DTYPE_F32))
   
   ;; data1 and data2 will be identical
   ```
   
   See also:
   - get-seed: Query current seed
   - set-engine-seed!: Set custom engine seed"
  [seed]
  (check! (random/af-set-seed (long seed))
              "af-set-seed")
  nil)

(defn get-seed
  "Get the seed of the default random engine.
   
   Returns the current seed value of the default random engine on the
   current device.
   
   Returns:
   Seed value as long
   
   Example:
   ```clojure
   (set-seed! 42)
   (get-seed)
   ; => 42
   ```
   
   See also:
   - set-seed!: Set seed
   - get-engine-seed: Get custom engine seed"
  []
  (let [seed-ptr (mem/alloc 8)]
    (check! (random/af-get-seed seed-ptr)
                "af-get-seed")
    (mem/read-long seed-ptr 0)))

;;;
;;; Convenience Functions
;;;

(defn engine-info
  "Get comprehensive information about a random engine.
   
   Parameters:
   - engine: Random engine handle
   
   Returns:
   Map with engine information:
   - :type - Engine type constant
   - :type-name - Engine type name (\"Philox\", \"Threefry\", \"Mersenne\")
   - :seed - Current seed value
   
   Example:
   ```clojure
   (def engine (create-engine :philox 42))
   (engine-info engine)
   ; => {:type 100, :type-name \"Philox\", :seed 42}
   ```"
  [engine]
  (let [type (get-engine-type engine)
        seed (get-engine-seed engine)]
    {:type type
     :type-name (engine-type-name type)
     :seed seed}))
