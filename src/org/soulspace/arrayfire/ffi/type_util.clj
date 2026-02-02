(ns org.soulspace.arrayfire.ffi.type-util
  "Bindings for the ArrayFire type utility functions.
   
   Type utilities provide information about ArrayFire's fundamental data types,
   enabling runtime introspection and memory calculations. These functions are
   essential for understanding memory requirements, optimizing allocations, and
   performing type-generic operations.
   
   ArrayFire Data Types:
   
   ArrayFire supports a comprehensive set of data types represented by the
   af_dtype enumeration. Each type has specific memory requirements and
   computational characteristics.
   
   **Floating-Point Types**:
   
   - **f32** (float): 32-bit single-precision IEEE 754
     * Size: 4 bytes
     * Range: ±3.4×10³⁸ (approximate)
     * Precision: ~7 decimal digits
     * Use: General-purpose GPU computing, ML, graphics
     * Performance: Fastest on most GPUs (native type)
   
   - **f64** (double): 64-bit double-precision IEEE 754
     * Size: 8 bytes
     * Range: ±1.7×10³⁰⁸ (approximate)
     * Precision: ~15-16 decimal digits
     * Use: Scientific computing, high-precision numerics
     * Performance: 2-32× slower than f32 on most GPUs
   
   - **f16** (half): 16-bit half-precision IEEE 754-2008
     * Size: 2 bytes
     * Range: ±65,504
     * Precision: ~3-4 decimal digits
     * Use: Deep learning, memory-constrained scenarios
     * Performance: 2-8× faster than f32 on modern GPUs (Volta+)
     * Note: Limited precision, requires careful numerical handling
   
   **Complex Types**:
   
   - **c32**: 32-bit complex (2× float)
     * Size: 8 bytes (two f32 components: real + imaginary)
     * Structure: {float real, float imag}
     * Use: Signal processing, quantum computing, FFT
     * Storage: Interleaved format (real₁, imag₁, real₂, imag₂, ...)
   
   - **c64**: 64-bit complex (2× double)
     * Size: 16 bytes (two f64 components)
     * Structure: {double real, double imag}
     * Use: High-precision complex arithmetic
     * Storage: Interleaved format
   
   **Signed Integer Types**:
   
   - **s8**: 8-bit signed char
     * Size: 1 byte
     * Range: -128 to 127
     * Use: Compact storage, text processing
   
   - **s16**: 16-bit signed short
     * Size: 2 bytes
     * Range: -32,768 to 32,767
     * Use: Audio samples, sensor data
   
   - **s32**: 32-bit signed int
     * Size: 4 bytes
     * Range: -2,147,483,648 to 2,147,483,647
     * Use: General integer computations, indices
   
   - **s64**: 64-bit signed long long
     * Size: 8 bytes
     * Range: -9,223,372,036,854,775,808 to 9,223,372,036,854,775,807
     * Use: Large counts, timestamps, high-precision indices
   
   **Unsigned Integer Types**:
   
   - **u8**: 8-bit unsigned char
     * Size: 1 byte
     * Range: 0 to 255
     * Use: Images (pixel values), binary data, masks
     * Most common image format
   
   - **u16**: 16-bit unsigned short
     * Size: 2 bytes
     * Range: 0 to 65,535
     * Use: Medical imaging (16-bit grayscale), depth maps
   
   - **u32**: 32-bit unsigned int
     * Size: 4 bytes
     * Range: 0 to 4,294,967,295
     * Use: Large indices, histogram bins, labels
   
   - **u64**: 64-bit unsigned long long
     * Size: 8 bytes
     * Range: 0 to 18,446,744,073,709,551,615
     * Use: Very large datasets, unique identifiers
   
   **Boolean Type**:
   
   - **b8**: 8-bit boolean
     * Size: 1 byte (stored as unsigned char)
     * Values: 0 (false) or 1 (true)
     * Use: Masks, conditional operations, binary maps
     * Note: Uses full byte despite representing single bit
   
   Type Size Table:
   
   ```text
   Type  | Size (bytes) | Description              | Common Use
   ------|--------------|--------------------------|---------------------------
   f32   | 4            | Single-precision float   | ML, graphics, general GPU
   f64   | 8            | Double-precision float   | Scientific computing
   f16   | 2            | Half-precision float     | Deep learning inference
   c32   | 8            | Complex single           | FFT, signal processing
   c64   | 16           | Complex double           | High-precision complex math
   s8    | 1            | Signed 8-bit             | Compact signed data
   u8    | 1            | Unsigned 8-bit           | Images, masks
   s16   | 2            | Signed 16-bit            | Audio samples
   u16   | 2            | Unsigned 16-bit          | Medical images
   s32   | 4            | Signed 32-bit            | Integer computation
   u32   | 4            | Unsigned 32-bit          | Labels, large indices
   s64   | 8            | Signed 64-bit            | Large counts
   u64   | 8            | Unsigned 64-bit          | Very large datasets
   b8    | 1            | Boolean                  | Masks, conditions
   ```
   
   Memory Calculation:
   
   Total array memory = product(dimensions) × size_of(type)
   
   Examples:
   ```text
   Array Shape    | Type | Memory
   ---------------|------|------------------
   [1000, 1000]   | f32  | 1000×1000×4 = 4 MB
   [1920, 1080]   | u8   | 1920×1080×1 = 2 MB
   [256, 256, 3]  | u8   | 256×256×3×1 = 192 KB (RGB image)
   [1024, 1024]   | c64  | 1024×1024×16 = 16 MB
   [10000, 10000] | f64  | 10000×10000×8 = 763 MB
   ```
   
   Type Selection Guidelines:
   
   **Performance Optimization**:
   
   1. **f32 is usually fastest**:
      - Native on most GPUs
      - Hardware acceleration for most operations
      - Best balance of precision and speed
   
   2. **f16 for deep learning**:
      - 2× memory savings
      - 2-8× speedup on modern GPUs (Tensor Cores)
      - Sufficient for neural network inference
      - Mixed-precision training (f16 + f32)
   
   3. **Integers for indexing**:
      - u32/s32 for general indices
      - u8 for masks and images
      - Faster integer operations than float
   
   **Memory Optimization**:
   
   1. **Use smallest sufficient type**:
      - u8 for images (0-255 range)
      - f16 instead of f32 when precision allows
      - u32 instead of u64 for indices < 4B
   
   2. **Memory hierarchy**:
      ```text
      f16 < u8=s8=b8 < u16=s16 < f32=u32=s32 < f64=s64=u64=c32 < c64
      ```
   
   3. **GPU memory constraints**:
      - f32: 1000×1000 matrix = 4 MB
      - f64: 1000×1000 matrix = 8 MB (2× more)
      - f16: 1000×1000 matrix = 2 MB (½ less)
   
   **Precision Requirements**:
   
   1. **High precision needed** (f64):
      - Iterative methods (conjugate gradient, Newton)
      - Condition number > 10⁶
      - Accumulated rounding errors matter
      - Financial calculations
      - Scientific simulations
   
   2. **Standard precision** (f32):
      - Machine learning training/inference
      - Graphics and visualization
      - Image processing
      - General numerical computing
      - Condition number < 10⁶
   
   3. **Low precision acceptable** (f16):
      - Neural network inference
      - Real-time graphics
      - Memory-bound operations
      - When speed > accuracy
   
   Type Conversion Costs:
   
   Converting between types has performance implications:
   
   - **Same category, different size**: Fast (e.g., f32 ↔ f64)
   - **Float ↔ Integer**: Moderate overhead
   - **Complex ↔ Real**: Requires magnitude/phase computation
   - **Any ↔ f16**: May require software emulation on older GPUs
   
   Common Patterns:
   
   **Pattern 1: Memory Footprint Calculation**
   ```clojure
   ;; Calculate array memory requirements before allocation
   (defn calculate-memory [dims type]
     (let [size-ptr (mem/alloc-pointer ::mem/long)
           _ (af-get-size-of size-ptr type)
           type-size (mem/read-long size-ptr)
           num-elements (reduce * dims)
           total-bytes (* num-elements type-size)
           total-mb (/ total-bytes 1024.0 1024.0)]
       {:type type
        :type-size type-size
        :dimensions dims
        :num-elements num-elements
        :bytes total-bytes
        :megabytes total-mb}))
   
   ;; Example usage
   (calculate-memory [1920 1080 3] AF_U8)
   ;; => {:type 3, :type-size 1, :dimensions [1920 1080 3],
   ;;     :num-elements 6220800, :bytes 6220800, :megabytes 5.93}
   ```
   
   **Pattern 2: Type-Generic Buffer Allocation**
   ```clojure
   ;; Allocate buffer based on array type
   (defn allocate-host-buffer [array]
     (let [dims (af-get-dims array)
           type (af-get-type array)
           size-ptr (mem/alloc-pointer ::mem/long)
           _ (af-get-size-of size-ptr type)
           element-size (mem/read-long size-ptr)
           num-elements (reduce * dims)
           buffer-size (* num-elements element-size)
           buffer (mem/alloc-native buffer-size)]
       {:buffer buffer
        :size buffer-size
        :elements num-elements
        :type type}))
   ```
   
   **Pattern 3: Type-Aware Data Transfer**
   ```clojure
   ;; Copy array to host with proper type sizing
   (defn copy-to-host [af-array]
     (let [dims (af-get-dims af-array)
           type (af-get-type af-array)
           size-ptr (mem/alloc-pointer ::mem/long)
           _ (af-get-size-of size-ptr type)
           elem-size (mem/read-long size-ptr)
           n-elems (reduce * dims)
           buffer-size (* n-elems elem-size)
           host-buffer (mem/alloc-native buffer-size)
           _ (af-get-data-ptr host-buffer af-array)]
       {:buffer host-buffer
        :dimensions dims
        :type type
        :element-size elem-size
        :total-bytes buffer-size}))
   ```
   
   **Pattern 4: Memory Budget Planning**
   ```clojure
   ;; Check if operation fits in GPU memory
   (defn fits-in-memory? [gpu-memory-mb operations]
     (let [calc-mem (fn [op]
                      (let [size-ptr (mem/alloc-pointer ::mem/long)
                            _ (af-get-size-of size-ptr (:type op))
                            elem-size (mem/read-long size-ptr)
                            n-elems (reduce * (:dims op))]
                        (* n-elems elem-size)))
           total-bytes (reduce + (map calc-mem operations))
           total-mb (/ total-bytes 1024.0 1024.0)
           ;; Leave 10% buffer for overhead
           available-mb (* gpu-memory-mb 0.9)]
       (< total-mb available-mb)))
   
   ;; Example
   (fits-in-memory? 8192  ; 8GB GPU
                    [{:dims [10000 10000] :type AF_F32}   ; 400 MB
                     {:dims [10000 10000] :type AF_F32}   ; 400 MB
                     {:dims [10000 10000] :type AF_F32}]) ; 400 MB
   ;; => true (1200 MB < 7372 MB)
   ```
   
   **Pattern 5: Precision Downgrade for Speed**
   ```clojure
   ;; Dynamically choose type based on data range
   (defn choose-optimal-type [data-range precision-needed]
     (let [max-val (apply max (map abs data-range))]
       (cond
         ;; High precision required
         (> precision-needed 1e-10) AF_F64
         
         ;; Small integers fit in u8
         (and (<= max-val 255)
              (every? integer? data-range)) AF_U8
         
         ;; Half precision sufficient for small values
         (and (<= max-val 65000)
              (< precision-needed 1e-3)) AF_F16
         
         ;; Default to f32
         :else AF_F32)))
   ```
   
   **Pattern 6: Batch Memory Estimation**
   ```clojure
   ;; Estimate memory for batched operation
   (defn estimate-batch-memory [single-item-dims item-type batch-size]
     (let [size-ptr (mem/alloc-pointer ::mem/long)
           _ (af-get-size-of size-ptr item-type)
           elem-size (mem/read-long size-ptr)
           elems-per-item (reduce * single-item-dims)
           bytes-per-item (* elems-per-item elem-size)
           total-bytes (* bytes-per-item batch-size)
           total-gb (/ total-bytes 1024.0 1024.0 1024.0)]
       {:batch-size batch-size
        :item-dims single-item-dims
        :type item-type
        :bytes-per-item bytes-per-item
        :total-bytes total-bytes
        :total-gb total-gb}))
   
   ;; Example: Batch of images
   (estimate-batch-memory [224 224 3] AF_F32 64)
   ;; => {:batch-size 64, :item-dims [224 224 3], :type 0,
   ;;     :bytes-per-item 602112, :total-bytes 38535168, :total-gb 0.036}
   ```
   
   Use Cases:
   
   **Memory Management**:
   - Pre-allocation sizing: Calculate exact buffer sizes
   - Capacity planning: Estimate GPU memory requirements
   - Batch size optimization: Maximize batches within memory limits
   - Memory pressure detection: Monitor total allocations
   
   **Data Transfer**:
   - Host-device copying: Allocate correct buffer sizes
   - Serialization: Compute serialized data size
   - Interop: Interface with other libraries (NumPy, PyTorch)
   - File I/O: Determine file buffer sizes
   
   **Type-Generic Programming**:
   - Generic array operations: Handle any type uniformly
   - Runtime type dispatch: Select algorithm by type
   - Type conversion utilities: Calculate conversion buffers
   - FFI interfaces: Compute C structure sizes
   
   **Performance Optimization**:
   - Memory bandwidth analysis: Bytes transferred per operation
   - Cache sizing: L1/L2 cache utilization estimates
   - Type downgrade decisions: Trade precision for speed
   - Memory layout optimization: Structure-of-arrays vs array-of-structures
   
   **Scientific Computing**:
   - Precision analysis: Choose type for numerical stability
   - Error propagation: Estimate rounding errors
   - Mixed-precision algorithms: Combine f32 and f64
   - Resource budgeting: Plan computation within constraints
   
   **Machine Learning**:
   - Model size calculation: Total parameter count × type size
   - Activation memory: Forward/backward pass requirements
   - Batch size selection: Fit in GPU memory
   - Mixed-precision training: f16 for speed, f32 for stability
   
   **Image Processing**:
   - Image buffer allocation: width × height × channels × type_size
   - Format conversion: Calculate intermediate buffer sizes
   - Pyramid construction: Multi-resolution memory requirements
   - Video processing: Frame buffer sizing
   
   Example Calculations:
   
   **Deep Learning Model**:
   ```clojure
   ;; ResNet-50 approximate memory (inference)
   (defn resnet50-memory [batch-size image-size]
     (let [;; Input: batch × 3 × 224 × 224
           input-elems (* batch-size 3 image-size image-size)
           ;; Activations ~100MB per image (approximate)
           activation-elems (* batch-size 100 1024 1024 (/ 1 4))
           ;; Parameters: 25M weights
           param-elems (* 25 1024 1024)
           
           ;; Use f32
           size-ptr (mem/alloc-pointer ::mem/long)
           _ (af-get-size-of size-ptr AF_F32)
           f32-size (mem/read-long size-ptr)
           
           total-mb (/ (* (+ input-elems activation-elems param-elems)
                          f32-size)
                       1024.0 1024.0)]
       total-mb))
   
   (resnet50-memory 32 224)  ; 32-image batch
   ;; => ~1200 MB (fits in 2GB GPU)
   ```
   
   **Scientific Simulation**:
   ```clojure
   ;; 3D computational fluid dynamics grid
   (defn cfd-grid-memory [nx ny nz n-vars precision]
     (let [type (if (= precision :double) AF_F64 AF_F32)
           size-ptr (mem/alloc-pointer ::mem/long)
           _ (af-get-size-of size-ptr type)
           elem-size (mem/read-long size-ptr)
           
           ;; Variables: pressure, velocity (3), temperature, density
           total-elems (* nx ny nz n-vars)
           total-gb (/ (* total-elems elem-size) 1024.0 1024.0 1024.0)]
       {:grid [nx ny nz]
        :variables n-vars
        :type type
        :precision precision
        :total-gb total-gb}))
   
   (cfd-grid-memory 512 512 512 6 :single)
   ;; => {:grid [512 512 512], :variables 6, :type 0,
   ;;     :precision :single, :total-gb 3.0}
   ```
   
   **Image Pyramid**:
   ```clojure
   ;; Gaussian pyramid memory requirements
   (defn pyramid-memory [base-width base-height levels]
     (let [size-ptr (mem/alloc-pointer ::mem/long)
           _ (af-get-size-of size-ptr AF_U8)
           u8-size (mem/read-long size-ptr)
           
           ;; Each level is 1/4 size of previous
           level-sizes (map (fn [lvl]
                             (let [w (/ base-width (Math/pow 2 lvl))
                                   h (/ base-height (Math/pow 2 lvl))]
                               (* w h)))
                           (range levels))
           total-pixels (reduce + level-sizes)
           total-mb (/ (* total-pixels u8-size) 1024.0 1024.0)]
       {:base-size [base-width base-height]
        :levels levels
        :level-sizes level-sizes
        :total-pixels total-pixels
        :total-mb total-mb}))
   
   (pyramid-memory 1920 1080 5)
   ;; => 5 levels from 1920×1080 down to 120×67
   ;;    Total: ~2.7 MB
   ```
   
   Performance Characteristics:
   
   - **af-get-size-of**: O(1) constant time
     * Simple table lookup
     * No GPU interaction
     * Negligible overhead (<1 μs)
   
   Type Size Constants:
   
   The function returns these constant values:
   ```clojure
   (def type-sizes
     {AF_F32 4    ; float
      AF_C32 8    ; complex float (2× f32)
      AF_F64 8    ; double
      AF_C64 16   ; complex double (2× f64)
      AF_B8  1    ; bool (stored as byte)
      AF_S32 4    ; int
      AF_U32 4    ; unsigned int
      AF_U8  1    ; unsigned char
      AF_S64 8    ; long long
      AF_U64 8    ; unsigned long long
      AF_S16 2    ; short
      AF_U16 2    ; unsigned short
      AF_F16 2    ; half float
      AF_S8  1})  ; signed char
   ```
   
   Error Handling:
   
   Common errors:
   - **AF_ERR_ARG**: Invalid type enum value
     * Type must be valid af_dtype enum value
     * Range: 0-13 depending on API version
   
   Best Practices:
   
   1. **Pre-calculate memory requirements**:
      - Avoid out-of-memory errors
      - Plan batch sizes appropriately
      - Validate before large allocations
   
   2. **Use in generic functions**:
      - Write type-agnostic code
      - Handle any ArrayFire type uniformly
      - Enable runtime type decisions
   
   3. **Optimize memory usage**:
      - Choose smallest sufficient type
      - Consider memory vs precision tradeoffs
      - Use f16 for memory-bound operations
   
   4. **Cache type sizes**:
      - Query once, reuse multiple times
      - Store in lookup table for hot paths
      - Avoid repeated calls in loops
   
   5. **Consider alignment**:
      - GPU memory often aligned to 256 bytes
      - Actual allocation may exceed calculated size
      - Add padding for optimal performance
   
   Limitations:
   
   - Only provides size information, not alignment requirements
   - Size is platform-independent (logical size)
   - Actual GPU memory allocation may differ due to alignment
   - Does not account for ArrayFire metadata overhead
   
   Mathematical Notes:
   
   **Precision and Range**:
   
   For floating-point types, precision and range are inversely related:
   - More bits → greater range AND precision
   - f64: 11-bit exponent (range) + 52-bit mantissa (precision)
   - f32: 8-bit exponent (range) + 23-bit mantissa (precision)
   - f16: 5-bit exponent (range) + 10-bit mantissa (precision)
   
   **Rounding Errors**:
   
   Machine epsilon (smallest representable difference from 1):
   - f64: ε ≈ 2.22×10⁻¹⁶ (double precision)
   - f32: ε ≈ 1.19×10⁻⁷ (single precision)
   - f16: ε ≈ 9.77×10⁻⁴ (half precision)
   
   Accumulated errors in n operations: approximately n×ε
   
   **Integer Overflow**:
   
   Be aware of maximum representable values:
   - u8: 255 (2⁸-1)
   - u16: 65,535 (2¹⁶-1)
   - u32: 4,294,967,295 (2³²-1)
   - s32: ±2,147,483,647 (±(2³¹-1))
   
   See also:
   - af-get-type: Query array's data type
   - af-cast: Convert array to different type
   - af-get-dims: Get array dimensions for memory calculation"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; Type size query

;; af_err af_get_size_of(size_t *size, af_dtype type)
(defcfn af-get-size-of
  "Get the size in bytes of an ArrayFire data type.
   
   Returns the number of bytes required to store one element of the
   specified type. Essential for memory calculations, buffer allocations,
   and data transfer operations.
   
   Parameters:
   - size: Output pointer for size in bytes (size_t)
   - type: ArrayFire data type enum (af_dtype)
     * 0 = AF_F32 (float, 4 bytes)
     * 1 = AF_C32 (complex float, 8 bytes)
     * 2 = AF_F64 (double, 8 bytes)
     * 3 = AF_C64 (complex double, 16 bytes)
     * 4 = AF_B8 (bool, 1 byte)
     * 5 = AF_S32 (int, 4 bytes)
     * 6 = AF_U32 (unsigned int, 4 bytes)
     * 7 = AF_U8 (unsigned char, 1 byte)
     * 8 = AF_S64 (long long, 8 bytes)
     * 9 = AF_U64 (unsigned long long, 8 bytes)
     * 10 = AF_S16 (short, 2 bytes)
     * 11 = AF_U16 (unsigned short, 2 bytes)
     * 12 = AF_F16 (half float, 2 bytes)
     * 13 = AF_S8 (signed char, 1 byte)
   
   Operation:
   Returns constant size for each type (platform-independent).
   
   Type Sizes:
   ```text
   Type  | Enum Value | Size | Description
   ------|------------|------|----------------------------
   f32   | 0          | 4    | Single-precision float
   c32   | 1          | 8    | Complex float (2× f32)
   f64   | 2          | 8    | Double-precision float
   c64   | 3          | 16   | Complex double (2× f64)
   b8    | 4          | 1    | Boolean (as byte)
   s32   | 5          | 4    | 32-bit signed int
   u32   | 6          | 4    | 32-bit unsigned int
   u8    | 7          | 1    | 8-bit unsigned (images)
   s64   | 8          | 8    | 64-bit signed int
   u64   | 9          | 8    | 64-bit unsigned int
   s16   | 10         | 2    | 16-bit signed int
   u16   | 11         | 2    | 16-bit unsigned (medical)
   f16   | 12         | 2    | Half-precision float
   s8    | 13         | 1    | 8-bit signed int
   ```
   
   Performance:
   - O(1) constant time (table lookup)
   - No GPU interaction
   - Negligible overhead (<1 microsecond)
   - Safe to call frequently
   
   Example (Basic Usage):
   ```clojure
   ;; Get size of float32
   (let [size-ptr (mem/alloc-pointer ::mem/long)
         _ (af-get-size-of size-ptr 0)  ; AF_F32
         size (mem/read-long size-ptr)]
     size)  ; Returns 4
   ```
   
   Example (Memory Calculation):
   ```clojure
   ;; Calculate array memory requirements
   (defn array-memory-mb [dims type]
     (let [size-ptr (mem/alloc-pointer ::mem/long)
           _ (af-get-size-of size-ptr type)
           elem-size (mem/read-long size-ptr)
           n-elems (reduce * dims)
           total-bytes (* n-elems elem-size)
           mb (/ total-bytes 1024.0 1024.0)]
       mb))
   
   ;; Example: 1000×1000 float array
   (array-memory-mb [1000 1000] 0)  ; AF_F32
   ;; => 3.814 MB
   ```
   
   Example (Buffer Allocation):
   ```clojure
   ;; Allocate host buffer for array data
   (defn allocate-buffer-for-array [array]
     (let [dims (af-get-dims array)
           type (af-get-type array)
           size-ptr (mem/alloc-pointer ::mem/long)
           _ (af-get-size-of size-ptr type)
           elem-size (mem/read-long size-ptr)
           num-elems (reduce * dims)
           buffer-size (* num-elems elem-size)
           buffer (mem/alloc-native buffer-size)]
       {:buffer buffer
        :size buffer-size
        :type type
        :element-size elem-size}))
   ```
   
   Example (Type Comparison):
   ```clojure
   ;; Compare memory usage of different types
   (defn compare-type-memory [dims]
     (let [types [{:name \"f16\" :enum 12}
                  {:name \"f32\" :enum 0}
                  {:name \"f64\" :enum 2}]
           calc-mem (fn [type-enum]
                     (let [size-ptr (mem/alloc-pointer ::mem/long)
                           _ (af-get-size-of size-ptr type-enum)
                           elem-size (mem/read-long size-ptr)
                           total (* (reduce * dims) elem-size)]
                       total))]
       (map (fn [{:keys [name enum]}]
              {:type name
               :bytes (calc-mem enum)
               :mb (/ (calc-mem enum) 1024.0 1024.0)})
            types)))
   
   (compare-type-memory [1920 1080])
   ;; => ({:type \"f16\", :bytes 4147200, :mb 3.955}
   ;;     {:type \"f32\", :bytes 8294400, :mb 7.910}
   ;;     {:type \"f64\", :bytes 16588800, :mb 15.820})
   ```
   
   Example (Batch Size Planning):
   ```clojure
   ;; Determine max batch size for GPU memory
   (defn max-batch-size [item-dims item-type gpu-memory-mb]
     (let [size-ptr (mem/alloc-pointer ::mem/long)
           _ (af-get-size-of size-ptr item-type)
           elem-size (mem/read-long size-ptr)
           elems-per-item (reduce * item-dims)
           bytes-per-item (* elems-per-item elem-size)
           available-bytes (* gpu-memory-mb 1024 1024 0.9)  ; 90% usable
           max-batch (int (/ available-bytes bytes-per-item))]
       {:max-batch max-batch
        :item-size-mb (/ bytes-per-item 1024.0 1024.0)
        :total-mb (* max-batch (/ bytes-per-item 1024.0 1024.0))}))
   
   ;; For 8GB GPU with 224×224×3 f32 images
   (max-batch-size [224 224 3] 0 8192)
   ;; => {:max-batch 1365, :item-size-mb 0.588, :total-mb 803.0}
   ```
   
   Example (Type-Generic Operations):
   ```clojure
   ;; Generic function working with any type
   (defn copy-array-to-host [af-array]
     (let [dims (af-get-dims af-array)
           type (af-get-type af-array)
           size-ptr (mem/alloc-pointer ::mem/long)
           _ (af-get-size-of size-ptr type)
           elem-size (mem/read-long size-ptr)
           num-elems (reduce * dims)
           buffer-size (* num-elems elem-size)
           host-buffer (mem/alloc-native buffer-size)
           _ (af-get-data-ptr host-buffer af-array)]
       {:data host-buffer
        :dimensions dims
        :type type
        :element-size elem-size
        :total-bytes buffer-size}))
   ```
   
   Use Cases:
   - **Memory planning**: Calculate array memory before allocation
   - **Buffer sizing**: Allocate correct host buffer sizes
   - **Capacity planning**: Determine max batch sizes for GPU
   - **Type-generic code**: Handle any type uniformly
   - **Data transfer**: Compute transfer buffer requirements
   - **Serialization**: Calculate serialized data size
   - **Performance analysis**: Memory bandwidth calculations
   
   Common Patterns:
   - Query once, cache for repeated use
   - Combine with dimensions for total memory
   - Use in allocation routines
   - Essential for FFI/interop code
   
   Type Selection Guide:
   - **f32**: Default for most GPU operations (fastest)
   - **f64**: Scientific computing requiring high precision
   - **f16**: Deep learning, memory-constrained scenarios
   - **u8**: Images, masks (most compact for 0-255 data)
   - **s32/u32**: Integer computations, indices
   - **c32/c64**: Complex arithmetic, FFT, signal processing
   
   Notes:
   - Size is platform-independent (logical size)
   - Actual GPU allocation may include padding/alignment
   - Complex types store real and imaginary parts contiguously
   - Boolean stored as full byte (not bit)
   - All sizes are fixed at compile time
   
   Returns:
   ArrayFire error code (af_err enum):
   - AF_SUCCESS (0): Size retrieved successfully
   - AF_ERR_ARG: Invalid type enum value
   
   See also:
   - af-get-type: Query array's data type
   - af-get-dims: Get array dimensions
   - af-cast: Convert array to different type
   - af-get-elements: Get total element count"
  "af_get_size_of" [::mem/pointer ::mem/int] ::mem/int)
