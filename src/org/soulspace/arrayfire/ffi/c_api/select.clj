(ns org.soulspace.arrayfire.ffi.c-api.select
  "Bindings for the ArrayFire select (conditional element selection) functions.
   
   Select operations provide element-wise conditional selection, similar to
   the ternary operator in C (condition ? a : b) but operating in parallel
   on entire arrays. This is a fundamental operation for implementing
   conditional logic in data-parallel computations.
   
   Mathematical Definition:
   
   For arrays cond, a, and b, the select operation produces:
   
   output[i] = cond[i] ? a[i] : b[i]
   
   Where each element is independently selected based on the corresponding
   condition value:
   - If cond[i] is true (non-zero), select a[i]
   - If cond[i] is false (zero), select b[i]
   
   Examples:
   
   **Basic Selection**:
   cond: [1, 0, 1, 0, 1]  (boolean/b8)
   a:    [10, 20, 30, 40, 50]
   b:    [1, 2, 3, 4, 5]
   out:  [10, 2, 30, 4, 50]  (a where cond=1, b where cond=0)
   
   **Clipping Values (max)**:
   data:  [3, 7, 2, 9, 5]
   max:   6
   cond:  data > max  → [0, 1, 0, 1, 0]
   out:   select(cond, max, data) → [3, 6, 2, 6, 5]
   
   **Clipping Values (min)**:
   data:  [3, 1, 5, 2, 4]
   min:   2
   cond:  data < min  → [0, 1, 0, 0, 0]
   out:   select(cond, min, data) → [3, 2, 5, 2, 4]
   
   **Replacing Invalid Values**:
   data:  [3.0, NaN, 5.0, -Inf, 4.0]
   cond:  isnan(data) | isinf(data)  → [0, 1, 0, 1, 0]
   out:   select(cond, 0.0, data)     → [3.0, 0.0, 5.0, 0.0, 4.0]
   
   Function Variants:
   
   1. **af-select**: Array ⊗ Array → Array
      Select between two arrays element-wise
      out[i] = cond[i] ? a[i] : b[i]
   
   2. **af-select-scalar-r**: Array ⊗ Scalar → Array
      Select between array and scalar (scalar on right)
      out[i] = cond[i] ? a[i] : b
      
   3. **af-select-scalar-l**: Scalar ⊗ Array → Array
      Select between scalar and array (scalar on left)
      out[i] = cond[i] ? a : b[i]
   
   4. **Long integer variants** (-long, -ulong):
      Support 64-bit integer scalars without precision loss
   
   Broadcasting:
   
   Select supports broadcasting (implicit dimension expansion) when array
   dimensions don't match:
   
   - Arrays with dimension 1 are broadcast to match other arrays
   - All three arrays (cond, a, b) can have different shapes
   - Output dimensions = max of all input dimensions
   
   Example:
   cond: [3, 1] (column vector)
   a:    [1, 3] (row vector)
   b:    [2x3] matrix
   
   All broadcast to [2x3]:
   cond → [[cond[0], cond[0], cond[0]]
           [cond[1], cond[1], cond[1]]]
   a    → [[a[0], a[1], a[2]]
           [a[0], a[1], a[2]]]
   Result is [2x3] with element-wise selection
   
   Condition Array Requirements:
   
   - Must be boolean type (b8 / af_dtype b8 = 0)
   - Created by comparison operators: <, >, <=, >=, ==, !=
   - Or logical operators: &, |, ! (and, or, not)
   - Non-zero values treated as true
   - Zero values treated as false
   
   Creating Condition Arrays:
   ```clojure
   ;; Using comparison operators
   (let [data (create-array [1 2 3 4 5] [5])
         threshold 3
         cond (gt data threshold)]  ; data > 3 → [0,0,0,1,1] (b8)
     ;; cond is now a boolean array
     )
   
   ;; Combining conditions with logical operators
   (let [data (create-array [-2 -1 0 1 2] [5])
         cond (and (ge data -1) (le data 1))]  ; -1 <= data <= 1
     ;; cond → [0,1,1,1,0] (b8)
     )
   ```
   
   Type Compatibility:
   
   - Arrays `a` and `b` must have the same data type
   - Condition must be boolean (b8)
   - All numeric types supported: f16, f32, f64, c32, c64,
     s8, s16, s32, s64, u8, u16, u32, u64, b8
   - Output has same type as input arrays
   
   Performance Characteristics:
   
   - **Complexity**: O(n) where n = number of elements
   - **GPU Acceleration**: Highly parallel (each element independent)
   - **Memory**: O(n) output + O(1) temporary
   - **Speedup**: 10-100× over CPU for large arrays
   - **Branching**: No actual branching on GPU (both paths computed)
   
   GPU Implementation:
   On GPU, select is implemented without actual branching:
   ```
   output = cond * a + (1 - cond) * b
   ```
   Both a and b are evaluated for all elements, then multiplied by
   appropriate masks. This avoids thread divergence.
   
   Applications:
   
   1. **Clipping/Clamping Values**:
      Limit values to a range [min, max]
      ```clojure
      ;; Clamp to [0, 1]
      (let [data (create-array [-0.5 0.3 1.5 0.7] [4])
            too-low (lt data 0.0)
            too-high (gt data 1.0)
            clamped1 (select too-low 0.0 data)
            clamped2 (select too-high 1.0 clamped1)]
        clamped2)  ; [0.0, 0.3, 1.0, 0.7]
      ```
   
   2. **Replacing Invalid Values**:
      Remove NaN, Inf, or other special values
      ```clojure
      (let [data (create-array [1.0 NaN 3.0 Inf] [4])
            valid (and (not (isnan data)) (not (isinf data)))
            clean (select (not valid) 0.0 data)]
        clean)  ; [1.0, 0.0, 3.0, 0.0]
      ```
   
   3. **Implementing Activation Functions**:
      ReLU, Leaky ReLU, ELU, etc.
      ```clojure
      ;; ReLU: max(0, x)
      (let [x (create-array [-2 -1 0 1 2] [5])
            relu (select (gt x 0) x 0)]
        relu)  ; [0, 0, 0, 1, 2]
      
      ;; Leaky ReLU: x if x > 0 else 0.01*x
      (let [x (create-array [-2 -1 0 1 2] [5])
            leak 0.01
            leaky-relu (select (gt x 0) x (mul x leak))]
        leaky-relu)  ; [-0.02, -0.01, 0, 1, 2]
      ```
   
   4. **Piecewise Functions**:
      Implement functions with different formulas in different regions
      ```clojure
      ;; Piecewise linear: y = 2x if x < 0 else x
      (let [x (create-array [-2 -1 0 1 2] [5])
            negative (lt x 0)
            y (select negative (mul x 2) x)]
        y)  ; [-4, -2, 0, 1, 2]
      ```
   
   5. **Data Filtering/Masking**:
      Keep valid data, replace invalid with sentinel
      ```clojure
      ;; Replace negative values with -1
      (let [data (create-array [3 -5 7 -2 9] [5])
            negative (lt data 0)
            filtered (select negative -1 data)]
        filtered)  ; [3, -1, 7, -1, 9]
      ```
   
   6. **Feature Engineering**:
      Create new features based on conditions
      ```clojure
      ;; Binary feature: 1 if value > threshold else 0
      (let [values (create-array [23 45 67 12 89] [5])
            threshold 50
            feature (select (gt values threshold) 1 0)]
        feature)  ; [0, 0, 1, 0, 1]
      ```
   
   7. **Numerical Stability**:
      Avoid division by zero, log of zero, etc.
      ```clojure
      ;; Safe division: x/y, but 0 if y==0
      (let [x (create-array [10 20 30] [3])
            y (create-array [2 0 5] [3])
            safe-div (select (eq y 0) 0 (div x y))]
        safe-div)  ; [5, 0, 6]
      ```
   
   8. **Image Processing**:
      Thresholding, masking, region selection
      ```clojure
      ;; Binary threshold: 255 if pixel > threshold else 0
      (let [img (create-array img-data [512 512])
            threshold 128
            binary (select (gt img threshold) 255 0)]
        binary)
      ```
   
   9. **Machine Learning**:
      Gradient clipping, dropout masks, attention masks
      ```clojure
      ;; Gradient clipping: clip gradients to [-1, 1]
      (let [grads (create-array grad-data [1000])
            clipped-hi (select (gt grads 1.0) 1.0 grads)
            clipped-lo (select (lt clipped-hi -1.0) -1.0 clipped-hi)]
        clipped-lo)
      ```
   
   10. **Statistics**:
       Conditional aggregation, outlier removal
       ```clojure
       ;; Replace outliers (> 3σ) with median
       (let [data (create-array values [n])
             mean (mean-all data)
             std (stdev-all data)
             outliers (gt (abs (sub data mean)) (mul std 3))
             median (median-all data)
             clean (select outliers median data)]
         clean)
       ```
   
   Common Patterns:
   
   **Clamping to Range [min, max]**:
   ```clojure
   (defn clamp [data min-val max-val]
     (let [below-min (lt data min-val)
           above-max (gt data max-val)
           clamped-min (select below-min min-val data)
           clamped-max (select above-max max-val clamped-min)]
       clamped-max))
   ```
   
   **ReLU Activation**:
   ```clojure
   (defn relu [x]
     (select (gt x 0) x 0))
   ```
   
   **Safe Division**:
   ```clojure
   (defn safe-div [x y]
     (select (eq y 0) 0 (div x y)))
   ```
   
   **Indicator Function**:
   ```clojure
   (defn indicator [x threshold]
     (select (gt x threshold) 1 0))
   ```
   
   Broadcasting Examples:
   
   **Scalar broadcast**:
   ```clojure
   ;; Replace all values > 100 with 100
   (let [data (create-array [50 150 75 200] [4])
         cond (gt data 100)
         out (af-select-scalar-r out-ptr cond data 100.0)]
     out)  ; [50, 100, 75, 100]
   ```
   
   **Vector broadcast to matrix**:
   ```clojure
   ;; Apply per-row thresholds to matrix
   (let [matrix (create-array [[1 2 3]
                               [4 5 6]] [2 3])
         thresholds (create-array [2 5] [2 1])  ; Column vector
         cond (gt matrix thresholds)  ; Broadcasts threshold to each row
         result (select cond matrix 0)]
     result)  ; [[0, 0, 3], [0, 0, 6]]
   ```
   
   Performance Tips:
   
   1. **Use scalar variants when possible**:
      - af-select-scalar-r/l are more efficient than creating scalar arrays
      - Avoid: (select cond a (constant scalar dims))
      - Use: (af-select-scalar-r cond a scalar)
   
   2. **Combine conditions before select**:
      - Better: cond = (and cond1 cond2), then one select
      - Worse: select within select (multiple passes)
   
   3. **Reuse condition arrays**:
      - Compute condition once if used multiple times
      - GPU condition evaluation is not free
   
   4. **Consider type**:
      - Use f32 instead of f64 for better GPU utilization
      - Integer types may be faster than floating-point
   
   5. **Broadcasting is free**:
      - Don't manually expand dimensions
      - Let select handle broadcasting automatically
   
   Comparison with Alternatives:
   
   **Select vs Where**:
   - Select: Element-wise conditional selection (keeps all elements)
   - Where: Returns indices of true conditions (sparse result)
   
   **Select vs Masked Operations**:
   - Select: Creates new array
   - Replace: Modifies existing array in-place (not available in select.cpp)
   
   **Select vs Explicit Loops**:
   - Select: Parallel, GPU-accelerated, single pass
   - Loops: Sequential, CPU-only, multiple passes
   
   Limitations:
   
   - Condition must be boolean (b8) type
   - Arrays a and b must have same data type
   - Broadcasting rules must be satisfied
   - Both branches evaluated on GPU (no short-circuit)
   - No in-place operation (creates new array)
   
   Scalar Type Notes:
   
   The scalar variants come in three flavors for precision:
   - *_r / *_l: double precision (64-bit float)
   - *_r_long / *_l_long: signed 64-bit integer
   - *_r_ulong / *_l_ulong: unsigned 64-bit integer
   
   Use the appropriate variant to avoid precision loss:
   - Use double variants for floating-point scalars
   - Use long variants for integers > 2³¹
   - Use ulong variants for unsigned integers > 2³²
   
   See also:
   - Comparison operators: af-lt, af-gt, af-le, af-ge, af-eq, af-ne
   - Logical operators: af-and, af-or, af-not
   - af-replace: In-place conditional replacement (not in select.cpp)
   - af-where: Get indices of true values"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; af_err af_select(af_array *out, const af_array cond, const af_array a, const af_array b)
(defcfn af-select
  "Select elements from two arrays based on a condition.
   
   Element-wise conditional selection: output[i] = cond[i] ? a[i] : b[i]
   For each element, if condition is true (non-zero), select from array a,
   otherwise select from array b.
   
   Parameters:
   - out: Output pointer for result array
   - cond: Conditional array (must be boolean type b8)
   - a: Array to select from when condition is true
   - b: Array to select from when condition is false
   
   Operation:
   For each index i:
   - If cond[i] != 0: output[i] = a[i]
   - If cond[i] == 0: output[i] = b[i]
   
   Type Requirements:
   - cond must be boolean (b8)
   - a and b must have the same data type
   - Output has same type as a and b
   
   Broadcasting:
   Arrays can have different dimensions if they satisfy broadcasting rules:
   - Dimension of size 1 broadcasts to match other dimensions
   - Output dimensions = max(cond_dims, a_dims, b_dims)
   - Example: cond[3], a[3,1], b[1,5] → out[3,5]
   
   Type Support:
   All ArrayFire types: f16, f32, f64, c32, c64, s8, s16, s32, s64,
   u8, u16, u32, u64, b8
   
   Example (basic selection):
   ```clojure
   (let [cond (create-array [1 0 1 0 1] [5])  ; true, false, true, false, true
         a (create-array [10 20 30 40 50] [5])
         b (create-array [1 2 3 4 5] [5])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-select out-ptr cond a b)]
     (mem/read-pointer out-ptr ::mem/pointer))
   ;; Result: [10, 2, 30, 4, 50]
   ```
   
   Example (clipping to maximum):
   ```clojure
   ;; Clip values above 100 to 100
   (let [data (create-array [50 150 75 200 100] [5])
         max-val (constant 100 [5])
         too-high (gt data max-val)  ; [false, true, false, true, false]
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-select out-ptr too-high max-val data)]
     (mem/read-pointer out-ptr ::mem/pointer))
   ;; Result: [50, 100, 75, 100, 100]
   ```
   
   Example (replace invalid values):
   ```clojure
   ;; Replace NaN and Inf with 0.0
   (let [data (create-array [1.0 (/ 0.0 0.0) 3.0 (/ 1.0 0.0)] [4])  ; [1.0, NaN, 3.0, Inf]
         invalid (or (isnan data) (isinf data))
         zero (constant 0.0 [4])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-select out-ptr invalid zero data)]
     (mem/read-pointer out-ptr ::mem/pointer))
   ;; Result: [1.0, 0.0, 3.0, 0.0]
   ```
   
   Example (ReLU activation function):
   ```clojure
   ;; ReLU: max(0, x)
   (let [x (create-array [-2.0 -1.0 0.0 1.0 2.0] [5])
         positive (gt x 0.0)
         zero (constant 0.0 [5])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-select out-ptr positive x zero)]
     (mem/read-pointer out-ptr ::mem/pointer))
   ;; Result: [0.0, 0.0, 0.0, 1.0, 2.0]
   ```
   
   Example (broadcasting with scalar):
   ```clojure
   ;; Replace negative values with 0
   (let [data (create-array [[-2 3] [-1 4]] [2 2])
         negative (lt data 0)
         zero (constant 0 [1])  ; Broadcasts to [2,2]
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-select out-ptr negative zero data)]
     (mem/read-pointer out-ptr ::mem/pointer))
   ;; Result: [[0, 3], [0, 4]]
   ```
   
   Example (piecewise linear function):
   ```clojure
   ;; y = 2x if x < 0, else y = x
   (let [x (create-array [-2 -1 0 1 2] [5])
         negative (lt x 0)
         doubled (mul x 2)
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-select out-ptr negative doubled x)]
     (mem/read-pointer out-ptr ::mem/pointer))
   ;; Result: [-4, -2, 0, 1, 2]
   ```
   
   Applications:
   - Clipping/clamping values to range
   - Implementing activation functions (ReLU, etc.)
   - Replacing invalid/missing values
   - Piecewise function evaluation
   - Conditional data transformation
   - Image thresholding
   
   Performance:
   - O(n) complexity where n = number of output elements
   - Highly parallel on GPU (each element independent)
   - Both a and b are evaluated for all elements (no short-circuit)
   - 10-100× speedup on GPU vs CPU
   
   Broadcasting Rules:
   - Dimensions must match or be 1
   - Dimension 1 broadcasts to any size
   - Output size = max of all input dimensions
   - Example: [3,1] and [1,5] → [3,5]
   
   Notes:
   - Condition is evaluated as boolean: non-zero = true, zero = false
   - GPU evaluates both branches (no branching divergence)
   - Creates new output array (not in-place)
   - For scalar b, use af-select-scalar-r instead (more efficient)
   - For scalar a, use af-select-scalar-l instead (more efficient)
   
   Returns:
   ArrayFire error code (af_err enum)
   - AF_SUCCESS (0): Selection successful
   - AF_ERR_ARG: Type mismatch or invalid condition type
   - AF_ERR_SIZE: Broadcasting incompatible dimensions
   
   See also:
   - af-select-scalar-r: Select between array and scalar (scalar on right)
   - af-select-scalar-l: Select between scalar and array (scalar on left)
   - Comparison ops: af-lt, af-gt, af-le, af-ge, af-eq, af-ne
   - Logical ops: af-and, af-or, af-not"
  "af_select" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_select_scalar_r(af_array *out, const af_array cond, const af_array a, const double b)
(defcfn af-select-scalar-r
  "Select between array and scalar based on condition (scalar on right).
   
   Element-wise: output[i] = cond[i] ? a[i] : b
   More efficient than af-select when b is a constant scalar value.
   
   Parameters:
   - out: Output pointer for result array
   - cond: Conditional array (boolean b8)
   - a: Array to select from when condition is true
   - b: Scalar value (double) to use when condition is false
   
   Operation:
   For each index i:
   - If cond[i] != 0: output[i] = a[i]
   - If cond[i] == 0: output[i] = b (scalar)
   
   The scalar b is broadcast to all positions where cond is false.
   
   Example (clamp maximum):
   ```clojure
   ;; Clip all values above 100 to 100
   (let [data (create-array [50 150 75 200 90] [5])
         too-high (gt data 100)
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-select-scalar-r out-ptr too-high 100.0 data)]
     (mem/read-pointer out-ptr ::mem/pointer))
   ;; Result: [50, 100, 75, 100, 90]
   ```
   
   Example (replace negative with zero):
   ```clojure
   (let [data (create-array [-2 3 -1 4 5] [5])
         negative (lt data 0)
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-select-scalar-r out-ptr negative 0.0 data)]
     (mem/read-pointer out-ptr ::mem/pointer))
   ;; Result: [0, 3, 0, 4, 5]
   ```
   
   Example (safe division by zero):
   ```clojure
   ;; Division: return 0 when divisor is 0
   (let [x (create-array [10.0 20.0 30.0] [3])
         y (create-array [2.0 0.0 5.0] [3])
         ratio (div x y)  ; [5.0, Inf, 6.0]
         div-by-zero (eq y 0)
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-select-scalar-r out-ptr div-by-zero 0.0 ratio)]
     (mem/read-pointer out-ptr ::mem/pointer))
   ;; Result: [5.0, 0.0, 6.0]
   ```
   
   Applications:
   - Clipping maximum values
   - Replacing invalid values with default
   - Setting floor values
   - Default value for failed conditions
   
   Performance:
   - More efficient than creating scalar array
   - No memory allocation for scalar
   - Same parallel efficiency as af-select
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-select: Select between two arrays
   - af-select-scalar-l: Scalar on left
   - af-select-scalar-r-long: For 64-bit integers"
  "af_select_scalar_r" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/double] ::mem/int)

;; af_err af_select_scalar_r_long(af_array *out, const af_array cond, const af_array a, const long long b)
(defcfn af-select-scalar-r-long
  "Select between array and 64-bit integer scalar (scalar on right).
   
   Like af-select-scalar-r but for signed 64-bit integer scalars.
   Use when scalar value doesn't fit in double precision or requires
   exact integer representation.
   
   Parameters:
   - out: Output pointer for result array
   - cond: Conditional array (boolean b8)
   - a: Array to select from when condition is true
   - b: Scalar value (long long / s64) when condition is false
   
   Example (replace with sentinel value):
   ```clojure
   ;; Use large integer as \"missing value\" sentinel
   (let [data (create-array [100 -1 200 -1 300] [5])  ; -1 = missing
         missing (eq data -1)
         sentinel 9223372036854775807  ; Max s64
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-select-scalar-r-long out-ptr missing sentinel data)]
     (mem/read-pointer out-ptr ::mem/pointer))
   ;; Result: [100, 9223372036854775807, 200, 9223372036854775807, 300]
   ```
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-select-scalar-r: For double precision scalars
   - af-select-scalar-r-ulong: For unsigned 64-bit integers"
  "af_select_scalar_r_long" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/long] ::mem/int)

;; af_err af_select_scalar_r_ulong(af_array *out, const af_array cond, const af_array a, const unsigned long long b)
(defcfn af-select-scalar-r-ulong
  "Select between array and unsigned 64-bit integer scalar (scalar on right).
   
   Like af-select-scalar-r-long but for unsigned 64-bit integers.
   
   Parameters:
   - out: Output pointer for result array
   - cond: Conditional array (boolean b8)
   - a: Array to select from when condition is true
   - b: Scalar value (unsigned long long / u64) when condition is false
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-select-scalar-r-long: For signed 64-bit integers"
  "af_select_scalar_r_ulong" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/long] ::mem/int)

;; af_err af_select_scalar_l(af_array *out, const af_array cond, const double a, const af_array b)
(defcfn af-select-scalar-l
  "Select between scalar and array based on condition (scalar on left).
   
   Element-wise: output[i] = cond[i] ? a : b[i]
   More efficient than af-select when a is a constant scalar value.
   
   Parameters:
   - out: Output pointer for result array
   - cond: Conditional array (boolean b8)
   - a: Scalar value (double) to use when condition is true
   - b: Array to select from when condition is false
   
   Operation:
   For each index i:
   - If cond[i] != 0: output[i] = a (scalar)
   - If cond[i] == 0: output[i] = b[i]
   
   The scalar a is broadcast to all positions where cond is true.
   
   Example (clamp minimum):
   ```clojure
   ;; Ensure all values are at least 0 (floor at 0)
   (let [data (create-array [-2 3 -1 4 5] [5])
         below-zero (lt data 0)
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-select-scalar-l out-ptr below-zero 0.0 data)]
     (mem/read-pointer out-ptr ::mem/pointer))
   ;; Result: [0, 3, 0, 4, 5]
   ```
   
   Example (binary threshold):
   ```clojure
   ;; Set to 255 if above threshold, keep original otherwise
   (let [img (create-array [50 150 200 75] [4])
         above-thresh (gt img 100)
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-select-scalar-l out-ptr above-thresh 255.0 img)]
     (mem/read-pointer out-ptr ::mem/pointer))
   ;; Result: [50, 255, 255, 75]
   ```
   
   Example (indicator/dummy variable):
   ```clojure
   ;; Create binary indicator: 1 if condition met, 0 otherwise
   (let [ages (create-array [25 45 35 60 18] [5])
         senior (ge ages 60)  ; Age >= 60
         zero (constant 0 [5])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-select-scalar-l out-ptr senior 1.0 zero)]
     (mem/read-pointer out-ptr ::mem/pointer))
   ;; Result: [0, 0, 0, 1, 0]
   ```
   
   Applications:
   - Clipping minimum values
   - Setting ceiling values
   - Binary indicators/flags
   - Default value for successful conditions
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-select: Select between two arrays
   - af-select-scalar-r: Scalar on right
   - af-select-scalar-l-long: For 64-bit integers"
  "af_select_scalar_l" [::mem/pointer ::mem/pointer ::mem/double ::mem/pointer] ::mem/int)

;; af_err af_select_scalar_l_long(af_array *out, const af_array cond, const long long a, const af_array b)
(defcfn af-select-scalar-l-long
  "Select between 64-bit integer scalar and array (scalar on left).
   
   Like af-select-scalar-l but for signed 64-bit integer scalars.
   
   Parameters:
   - out: Output pointer for result array
   - cond: Conditional array (boolean b8)
   - a: Scalar value (long long / s64) when condition is true
   - b: Array to select from when condition is false
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-select-scalar-l: For double precision scalars
   - af-select-scalar-l-ulong: For unsigned 64-bit integers"
  "af_select_scalar_l_long" [::mem/pointer ::mem/pointer ::mem/long ::mem/pointer] ::mem/int)

;; af_err af_select_scalar_l_ulong(af_array *out, const af_array cond, const unsigned long long a, const af_array b)
(defcfn af-select-scalar-l-ulong
  "Select between unsigned 64-bit integer scalar and array (scalar on left).
   
   Like af-select-scalar-l-long but for unsigned 64-bit integers.
   
   Parameters:
   - out: Output pointer for result array
   - cond: Conditional array (boolean b8)
   - a: Scalar value (unsigned long long / u64) when condition is true
   - b: Array to select from when condition is false
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-select-scalar-l-long: For signed 64-bit integers"
  "af_select_scalar_l_ulong" [::mem/pointer ::mem/pointer ::mem/long ::mem/pointer] ::mem/int)
