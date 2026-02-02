(ns org.soulspace.arrayfire.ffi.random
  "Bindings for the ArrayFire random number generation functions.
   
   Random Number Generation in ArrayFire:
   
   ArrayFire provides GPU-accelerated random number generation with
   multiple high-quality pseudorandom number generators (PRNGs).
   
   **Random Engine Types**:
   - **Philox** (AF_RANDOM_ENGINE_PHILOX = 100): Counter-based RNG,
     excellent parallel properties, default engine
   - **Threefry** (AF_RANDOM_ENGINE_THREEFRY = 200): Counter-based RNG
     based on Threefish block cipher
   - **Mersenne Twister** (AF_RANDOM_ENGINE_MERSENNE = 300): Classic
     high-quality PRNG with long period (2^11213-1)
   
   **Distributions**:
   - **Uniform Distribution**: Values in range [0, 1) for floating point,
     full range for integers
   - **Normal Distribution**: Gaussian distribution with mean=0, std=1
   
   **Usage Patterns**:
   
   1. **Default Engine** (simplest):
   ```clojure
   ;; Generate uniform random array with default engine
   (af-randu out-ptr 2 dims-ptr af-dtype-f32)
   
   ;; Set seed for reproducibility
   (af-set-seed 42)
   ```
   
   2. **Custom Engine** (for control):
   ```clojure
   ;; Create engine with specific type and seed
   (af-create-random-engine engine-ptr af-random-engine-philox 12345)
   
   ;; Generate random arrays with custom engine
   (af-random-uniform out-ptr 2 dims-ptr af-dtype-f32 engine)
   
   ;; Clean up
   (af-release-random-engine engine)
   ```
   
   **Performance**:
   - GPU-accelerated: 10-100× faster than CPU
   - Philox/Threefry: Best for parallel generation, no state sharing
   - Mersenne: Better statistical properties but requires state management
   - All engines produce high-quality random numbers suitable for
     scientific computing
   
   **Thread Safety**:
   - Each device maintains its own default random engine
   - Custom engines are independent and can be used concurrently
   - Seed setting affects the current device's default engine
   
   **Type Support**:
   - Uniform: All numeric types (f16, f32, f64, c32, c64, s8-s64, u8-u64, b8)
   - Normal: Floating point only (f16, f32, f64, c32, c64)
   
   See also:
   - ArrayFire random documentation for statistical properties
   - Counter-based RNGs paper (Salmon et al.) for Philox/Threefry details"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;;;
;;; Random Engine Type Constants
;;;

;; af_random_engine_type enum values (from af/defines.h)
(def AF-RANDOM-ENGINE-PHILOX-4X32-10 100)
(def AF-RANDOM-ENGINE-THREEFRY-2X32-16 200)
(def AF-RANDOM-ENGINE-MERSENNE-GP11213 300)

;; Convenience aliases
(def AF-RANDOM-ENGINE-PHILOX AF-RANDOM-ENGINE-PHILOX-4X32-10)
(def AF-RANDOM-ENGINE-THREEFRY AF-RANDOM-ENGINE-THREEFRY-2X32-16)
(def AF-RANDOM-ENGINE-MERSENNE AF-RANDOM-ENGINE-MERSENNE-GP11213)
(def AF-RANDOM-ENGINE-DEFAULT AF-RANDOM-ENGINE-PHILOX)

;;;
;;; Random Engine Management
;;;

;; af_err af_create_random_engine(af_random_engine *engine, af_random_engine_type rtype, unsigned long long seed)
(defcfn af-create-random-engine
  "Create a new random engine with specified type and seed.
   
   Creates an independent random engine that can be used for generating
   random numbers without affecting the default engine.
   
   Parameters:
   - engine: Output pointer for the created random engine handle
   - rtype: Random engine type (AF_RANDOM_ENGINE_PHILOX=100,
     AF_RANDOM_ENGINE_THREEFRY=200, AF_RANDOM_ENGINE_MERSENNE=300)
   - seed: Initial seed value (64-bit unsigned integer)
   
   Engine Types:
   - 100 (Philox): Fast, parallel-friendly, default
   - 200 (Threefry): Counter-based, cryptographic origins
   - 300 (Mersenne): Traditional high-quality PRNG
   
   Example:
   ```clojure
   (let [engine-ptr (mem/alloc-pointer ::mem/pointer)]
     (af-create-random-engine engine-ptr 100 42)
     (let [engine (mem/read-pointer engine-ptr ::mem/pointer)]
       ;; Use engine for random generation
       (af-release-random-engine engine)))
   ```
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-release-random-engine: Free engine resources
   - af-retain-random-engine: Copy engine handle"
  "af_create_random_engine" [::mem/pointer ::mem/int ::mem/long] ::mem/int)

;; af_err af_retain_random_engine(af_random_engine *out, const af_random_engine engine)
(defcfn af-retain-random-engine
  "Retain (copy) a random engine handle.
   
   Creates a copy of the random engine handle, incrementing its reference
   count. Both handles refer to the same underlying engine state.
   
   Parameters:
   - out: Output pointer for the new engine handle
   - engine: Input random engine handle to copy
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-release-random-engine: Decrement reference count"
  "af_retain_random_engine" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_release_random_engine(af_random_engine engine)
(defcfn af-release-random-engine
  "Release (free) a random engine.
   
   Decrements the reference count of the random engine and frees
   resources when count reaches zero.
   
   Parameters:
   - engine: Random engine handle to release
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-create-random-engine: Create engine"
  "af_release_random_engine" [::mem/pointer] ::mem/int)

;; Random engine configuration

;; af_err af_random_engine_set_type(af_random_engine *engine, const af_random_engine_type rtype)
(defcfn af-random-engine-set-type
  "Set the type of a random engine.
   
   Changes the algorithm used by the random engine. Resets the engine
   state with the current seed.
   
   Parameters:
   - engine: Random engine handle (input/output)
   - rtype: New random engine type (100=Philox, 200=Threefry, 300=Mersenne)
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-random-engine-get-type: Query current type"
  "af_random_engine_set_type" [::mem/pointer ::mem/int] ::mem/int)

;; af_err af_random_engine_get_type(af_random_engine_type *rtype, const af_random_engine engine)
(defcfn af-random-engine-get-type
  "Get the type of a random engine.
   
   Returns the algorithm type currently used by the engine.
   
   Parameters:
   - rtype: Output pointer for engine type
   - engine: Random engine handle
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-random-engine-set-type: Change engine type"
  "af_random_engine_get_type" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_random_engine_set_seed(af_random_engine *engine, const unsigned long long seed)
(defcfn af-random-engine-set-seed
  "Set the seed of a random engine.
   
   Changes the seed and resets the engine state. Use for reproducible
   random sequences.
   
   Parameters:
   - engine: Random engine handle (input/output)
   - seed: New seed value (64-bit unsigned integer)
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-random-engine-get-seed: Query current seed"
  "af_random_engine_set_seed" [::mem/pointer ::mem/long] ::mem/int)

;; af_err af_random_engine_get_seed(unsigned long long *seed, af_random_engine engine)
(defcfn af-random-engine-get-seed
  "Get the seed of a random engine.
   
   Returns the seed value used to initialize the engine.
   
   Parameters:
   - seed: Output pointer for seed value
   - engine: Random engine handle
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-random-engine-set-seed: Change seed"
  "af_random_engine_get_seed" [::mem/pointer ::mem/pointer] ::mem/int)

;; Default random engine

;; af_err af_get_default_random_engine(af_random_engine *engine)
(defcfn af-get-default-random-engine
  "Get the default random engine for the current device.
   
   Returns a handle to the per-device default random engine. Each device
   maintains its own default engine.
   
   Parameters:
   - engine: Output pointer for default engine handle
   
   Note: Do NOT release the default engine - it's managed by ArrayFire.
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-set-default-random-engine-type: Change default engine type"
  "af_get_default_random_engine" [::mem/pointer] ::mem/int)

;; af_err af_set_default_random_engine_type(const af_random_engine_type rtype)
(defcfn af-set-default-random-engine-type
  "Set the type of the default random engine.
   
   Changes the algorithm used by the default engine for the current device.
   
   Parameters:
   - rtype: Random engine type (100=Philox, 200=Threefry, 300=Mersenne)
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-get-default-random-engine: Get default engine handle"
  "af_set_default_random_engine_type" [::mem/int] ::mem/int)

;; Random number generation with custom engine

;; af_err af_random_uniform(af_array *out, const unsigned ndims, const dim_t *dims, const af_dtype type, af_random_engine engine)
(defcfn af-random-uniform
  "Generate uniformly distributed random numbers using a custom engine.
   
   Creates an array of random numbers from uniform distribution [0, 1)
   for floating point, or full type range for integers.
   
   Parameters:
   - out: Output pointer for random array
   - ndims: Number of dimensions (1-4)
   - dims: Pointer to array of dimension sizes
   - type: Data type (af_dtype enum: f16, f32, f64, c32, c64, s8-s64, u8-u64, b8)
   - engine: Random engine to use
   
   Distribution:
   - Float/complex: [0.0, 1.0) uniform
   - Integers: Full type range uniform
   - Boolean: 0 or 1 with equal probability
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-randu: Use default engine
   - af-random-normal: Normal distribution"
  "af_random_uniform" [::mem/pointer ::mem/int ::mem/pointer ::mem/int ::mem/pointer] ::mem/int)

;; af_err af_random_normal(af_array *out, const unsigned ndims, const dim_t *dims, const af_dtype type, af_random_engine engine)
(defcfn af-random-normal
  "Generate normally distributed random numbers using a custom engine.
   
   Creates an array of random numbers from normal distribution with
   mean=0 and standard deviation=1.
   
   Parameters:
   - out: Output pointer for random array
   - ndims: Number of dimensions (1-4)
   - dims: Pointer to array of dimension sizes
   - type: Data type (af_dtype enum: f16, f32, f64, c32, c64 only)
   - engine: Random engine to use
   
   Distribution:
   - Real: N(0, 1) Gaussian
   - Complex: Real and imaginary parts independently N(0, 1)
   
   Note: Only floating-point types supported (f16, f32, f64, c32, c64).
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-randn: Use default engine
   - af-random-uniform: Uniform distribution"
  "af_random_normal" [::mem/pointer ::mem/int ::mem/pointer ::mem/int ::mem/pointer] ::mem/int)

;; Random number generation with default engine

;; af_err af_randu(af_array *out, const unsigned ndims, const dim_t *dims, const af_dtype type)
(defcfn af-randu
  "Generate uniformly distributed random numbers using default engine.
   
   Creates an array of random numbers from uniform distribution using
   the default random engine for the current device.
   
   Parameters:
   - out: Output pointer for random array
   - ndims: Number of dimensions (1-4)
   - dims: Pointer to array of dimension sizes
   - type: Data type (af_dtype enum)
   
   Example:
   ```clojure
   (let [dims (mem/serialize-into [512 512] [::mem/long ::mem/long])
         out-ptr (mem/alloc-pointer ::mem/pointer)]
     (af-randu out-ptr 2 dims 0) ; 0 = f32
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-random-uniform: Use custom engine
   - af-set-seed: Set default engine seed"
  "af_randu" [::mem/pointer ::mem/int ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_randn(af_array *out, const unsigned ndims, const dim_t *dims, const af_dtype type)
(defcfn af-randn
  "Generate normally distributed random numbers using default engine.
   
   Creates an array of random numbers from normal distribution N(0,1)
   using the default random engine for the current device.
   
   Parameters:
   - out: Output pointer for random array
   - ndims: Number of dimensions (1-4)
   - dims: Pointer to array of dimension sizes
   - type: Data type (af_dtype enum: f16, f32, f64, c32, c64 only)
   
   Example:
   ```clojure
   (let [dims (mem/serialize-into [1000] [::mem/long])
         out-ptr (mem/alloc-pointer ::mem/pointer)]
     (af-randn out-ptr 1 dims 1) ; 1 = f32
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-random-normal: Use custom engine
   - af-set-seed: Set default engine seed"
  "af_randn" [::mem/pointer ::mem/int ::mem/pointer ::mem/int] ::mem/int)

;; Default engine seed management

;; af_err af_set_seed(const unsigned long long seed)
(defcfn af-set-seed
  "Set the seed of the default random engine.
   
   Sets the seed for the default random engine on the current device.
   Use for reproducible random sequences.
   
   Parameters:
   - seed: Seed value (64-bit unsigned integer)
   
   Example:
   ```clojure
   ;; Set seed for reproducibility
   (af-set-seed 42)
   
   ;; Generate reproducible random data
   (af-randu out-ptr 2 dims af-dtype-f32)
   ```
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-get-seed: Query current seed
   - af-random-engine-set-seed: Set custom engine seed"
  "af_set_seed" [::mem/long] ::mem/int)

;; af_err af_get_seed(unsigned long long *seed)
(defcfn af-get-seed
  "Get the seed of the default random engine.
   
   Returns the current seed value of the default random engine on the
   current device.
   
   Parameters:
   - seed: Output pointer for seed value
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-set-seed: Set seed
   - af-random-engine-get-seed: Get custom engine seed"
  "af_get_seed" [::mem/pointer] ::mem/int)
