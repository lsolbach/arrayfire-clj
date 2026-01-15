(ns org.soulspace.arrayfire.ffi.replace
  "Bindings for the ArrayFire conditional replacement functions.
   
   Conditional replacement allows in-place modification of array elements
   based on a boolean condition mask. This is a fundamental operation for
   data manipulation, filtering, and masking.
   
   What is Conditional Replacement?
   
   Conditional replacement modifies array elements in-place: where a condition
   is false, the original values are replaced with values from another array
   or a scalar. This is the in-place counterpart to the select operation.
   
   **Key Difference from Select**:
   - **select**: Creates new output array
   - **replace**: Modifies input array in-place (side effect)
   
   Mathematical Definition:
   
   For replace with array:
   a[i] ← a[i]  if cond[i] = true
   a[i] ← b[i]  if cond[i] = false
   
   For replace with scalar:
   a[i] ← a[i]  if cond[i] = true
   a[i] ← s     if cond[i] = false
   
   Common Replacement Patterns:
   
   1. **Value Clamping**: Replace out-of-range values
      ```
      replace(array, array < min_val, min_val)
      replace(array, array > max_val, max_val)
      ```
   
   2. **NaN/Inf Handling**: Replace invalid values
      ```
      replace(array, isNaN(array), 0.0)
      replace(array, isInf(array), max_finite)
      ```
   
   3. **Masking**: Apply boolean mask
      ```
      replace(array, mask, replacement_values)
      ```
   
   4. **Outlier Removal**: Replace statistical outliers
      ```
      replace(array, abs(array - mean) > 3*std, mean)
      ```
   
   5. **Conditional Updates**: Update based on criteria
      ```
      replace(scores, scores < threshold, 0.0)
      ```
   
   Implementation Details:
   
   Internally, replace uses select operations but writes back to the
   input array (copy-on-write semantics):
   
   ```cpp
   void replace(af_array a, const af_array cond, const af_array b) {
       // Get copy-on-write array (creates copy if refcount > 1)
       Array<T>& out = getCopyOnWriteArray<T>(a);
       // Perform select: out = cond ? a : b
       select(out, getArray<char>(cond), getArray<T>(a), getArray<T>(b));
   }
   ```
   
   Copy-on-Write Behavior:
   - If array has references (refcount > 1): Creates copy before modification
   - If array is unique (refcount = 1): Modifies in-place efficiently
   - This ensures safety while optimizing for common cases
   
   Broadcasting Rules:
   
   Arrays can have different sizes as long as dimensions are compatible:
   - Each dimension must match OR be 1 (broadcast)
   - Condition dimensions must match min(a_dims, b_dims)
   - Examples:
     * a: [100, 200], b: [1, 200], cond: [100, 200] ✓
     * a: [100, 200], b: [100, 1], cond: [100, 1] ✓
     * a: [100, 200], b: [50, 200], cond: [50, 200] ✗ (incompatible)
   
   Performance Characteristics:
   
   **Time Complexity**: O(N) where N = number of elements
   - GPU parallelizes across all elements
   - Same as select operation
   - Negligible overhead for copy-on-write check
   
   **Space Complexity**:
   - Best case: O(1) - in-place modification (refcount = 1)
   - Worst case: O(N) - creates copy (refcount > 1)
   
   **GPU Implementation**:
   - Parallel kernel processes all elements
   - Each thread handles one output element
   - Coalesced memory access for efficiency
   
   Type Support:
   
   All ArrayFire types supported:
   - Floating point: f32, f64, f16
   - Complex: c32, c64
   - Integer: s32, u32, s64, u64, s16, u16, s8, u8
   - Boolean: b8
   
   Scalar Types:
   - double: For general numeric types
   - long long: For 64-bit signed integers
   - unsigned long long: For 64-bit unsigned integers
   
   Applications:
   
   1. **Data Cleaning**:
      - Remove outliers
      - Handle missing values (NaN/Inf)
      - Clamp to valid ranges
   
   2. **Image Processing**:
      - Masking regions
      - Thresholding
      - Alpha compositing
   
   3. **Signal Processing**:
      - Spike removal
      - Noise gating
      - Clipping protection
   
   4. **Machine Learning**:
      - Gradient clipping
      - Dropout (zero out elements)
      - Feature masking
   
   5. **Scientific Computing**:
      - Boundary condition application
      - Domain restriction
      - Conditional updates in iterative algorithms
   
   6. **Financial Data**:
      - Winsorization (replace extremes)
      - Missing data imputation
      - Anomaly correction
   
   Best Practices:
   
   1. **Prefer replace for In-Place Updates**:
      - Use replace when you don't need the original array
      - More memory efficient than select for modifications
   
   2. **Check Array Ownership**:
      - Be aware of copy-on-write behavior
      - If you need guaranteed in-place, ensure refcount = 1
   
   3. **Combine Operations**:
      - Chain multiple conditions efficiently:
        ```clojure
        ;; Multiple replacements
        (replace! arr (< arr min-val) min-val)
        (replace! arr (> arr max-val) max-val)
        ```
   
   4. **Use Appropriate Scalar Type**:
      - af-replace-scalar: for general numeric values (double)
      - af-replace-scalar-long: for large signed integers
      - af-replace-scalar-ulong: for large unsigned integers
   
   5. **Consider Type Compatibility**:
      - Ensure replacement values are compatible with array type
      - Complex arrays need complex replacements
   
   6. **Validate Dimensions**:
      - Check dimension compatibility before replace
      - Use broadcasting when appropriate
   
   Common Pitfalls:
   
   1. **Assuming True In-Place**:
      ```clojure
      ;; May create copy if array has references!
      (let [shared-arr original-array]
        (af-replace arr cond replacement)) ; Creates copy
      ```
      Solution: Ensure unique ownership or accept copy-on-write
   
   2. **Type Mismatch**:
      ```clojure
      ;; Error: Complex array, real scalar
      (af-replace-scalar complex-arr cond 3.14) ; Wrong!
      ```
      Solution: Use compatible types
   
   3. **Dimension Errors**:
      ```clojure
      ;; Error: Incompatible dimensions
      (af-replace [100,200] [50,100] [50,100]) ; Wrong!
      ```
      Solution: Verify dimension compatibility
   
   4. **Wrong Condition Type**:
      ```clojure
      ;; Error: Condition must be boolean (b8)
      (af-replace arr numeric-array replacement) ; Wrong!
      ```
      Solution: Use comparison operations to create boolean arrays
   
   5. **Forgetting Scalar Variants**:
      ```clojure
      ;; Inefficient: Creating array for single value
      (af-replace arr cond (constant scalar dims)) ; Wasteful
      ;; Efficient: Use scalar version
      (af-replace-scalar arr cond scalar) ; Better!
      ```
   
   6. **Not Handling NaN Properly**:
      ```clojure
      ;; NaN comparisons always false!
      (af-replace arr (= arr NaN) 0.0) ; Doesn't work!
      ;; Correct:
      (af-replace arr (isNaN arr) 0.0) ; Works!
      ```
      Solution: Use isNaN/isInf predicates
   
   7. **Memory Leak from Temp Arrays**:
      ```clojure
      ;; Multiple temp arrays created
      (let [cond1 (< arr min-val)
            cond2 (> arr max-val)
            _ (af-replace arr cond1 min-val)
            _ (af-replace arr cond2 max-val)]
        ;; Forgot to release cond1, cond2!
        arr)
      ```
      Solution: Use resource management (tech.resource)
   
   Relationship to Other Operations:
   
   - **select**: Non-destructive version (creates new array)
   - **where**: Extract indices where condition is true
   - **assign**: General indexed assignment
   - **mask**: Binary masking operations
   - **clamp**: Specialized value clamping
   
   Example Workflow:
   
   ```clojure
   ;; Clean data: remove outliers and NaN
   (let [data (create-array raw-data dims)
         mean (af-mean data)
         std (af-stdev data)
         lower-bound (- mean (* 3 std))
         upper-bound (+ mean (* 3 std))
         
         ;; Replace outliers with mean
         _ (af-replace data 
                      (< data lower-bound) 
                      mean)
         _ (af-replace data 
                      (> data upper-bound) 
                      mean)
         
         ;; Replace NaN with mean
         _ (af-replace data 
                      (isNaN data) 
                      mean)]
     data) ;; Cleaned data
   ```
   
   See also:
   - select functions for non-destructive conditional selection
   - assign functions for indexed assignment
   - where function for finding condition indices"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; Conditional replacement functions

;; af_err af_replace(af_array a, const af_array cond, const af_array b)
(defcfn af-replace
  "Replace array elements based on condition with values from another array.
   
   Modifies array `a` in-place: where `cond` is false, elements of `a` are
   replaced with corresponding elements from array `b`. Where `cond` is true,
   elements of `a` remain unchanged.
   
   Operation:
   a[i] ← a[i]  if cond[i] = true
   a[i] ← b[i]  if cond[i] = false
   
   Parameters:
   - a: input/output array (modified in-place via copy-on-write)
   - cond: boolean condition array (type b8)
   - b: replacement array (same type as a)
   
   Dimension Requirements:
   - cond.ndims() == min(a.ndims(), b.ndims())
   - cond.dims()[i] == min(a.dims()[i], b.dims()[i]) for each dimension i
   - a.dims()[i] == b.dims()[i] OR b.dims()[i] == 1 (broadcasting)
   - a and b must have the same type
   
   Broadcasting:
   If b has dimension 1 in some axis, it will be broadcast to match a's
   dimension in that axis. For example:
   - a: [100, 200], b: [1, 200], cond: [100, 200] → broadcasts b's first dim
   - a: [100, 200], b: [100, 1], cond: [100, 1] → broadcasts b's second dim
   
   Copy-on-Write Behavior:
   - If `a` has refcount = 1: Modifies in-place (O(1) space)
   - If `a` has refcount > 1: Creates copy first (O(N) space)
   - This ensures safety when array has multiple references
   
   Type Support:
   All types: f32, f64, f16, c32, c64, s32, u32, s64, u64, s16, u16, s8, u8, b8
   
   Example 1 - Replace outliers with values from another array:
   ```clojure
   (let [data (create-array raw-data [1000])
         mean-arr (af-mean data)
         std-arr (af-stdev data)
         
         ;; Compute bounds
         lower (- data (* 3 std-arr))
         upper (+ data (* 3 std-arr))
         
         ;; Replace outliers with median values
         medians (create-array median-vals [1000])
         outlier-mask (or (< data lower) (> data upper))
         
         ;; Replace outliers
         _ (af-replace data outlier-mask medians)]
     data) ;; Data with outliers replaced
   ```
   
   Example 2 - Masked blending of two images:
   ```clojure
   (let [img1 (create-array img1-data [512 512 3])
         img2 (create-array img2-data [512 512 3])
         
         ;; Binary mask: 1=keep img1, 0=use img2
         mask (create-array mask-data [512 512 1]) ; Broadcasts across channels
         
         ;; Blend: where mask=0, use img2
         _ (af-replace img1 mask img2)]
     img1) ;; Blended image
   ```
   
   Example 3 - Conditional array swap:
   ```clojure
   (let [a (create-array a-data dims)
         b (create-array b-data dims)
         
         ;; Condition: magnitude comparison
         cond (> (abs a) (abs b))
         
         ;; Replace small values in a with values from b
         _ (af-replace a cond b)]
     a) ;; a with conditionally replaced values
   ```
   
   Example 4 - Multi-level thresholding:
   ```clojure
   (let [img (create-array img-data [512 512])
         
         ;; Create replacement arrays for different ranges
         low-vals (constant 0.0 [512 512])
         high-vals (constant 1.0 [512 512])
         
         ;; Apply thresholds
         _ (af-replace img (< img 0.3) low-vals)
         _ (af-replace img (> img 0.7) high-vals)]
     img) ;; Thresholded image
   ```
   
   Example 5 - NaN/Inf replacement with interpolation:
   ```clojure
   (let [signal (create-array signal-data [10000])
         
         ;; Compute interpolated values for invalid data
         interpolated (compute-interpolation signal)
         
         ;; Replace NaN and Inf with interpolated values
         invalid-mask (or (isNaN signal) (isInf signal))
         _ (af-replace signal invalid-mask interpolated)]
     signal) ;; Clean signal
   ```
   
   Performance:
   - Time: O(N) - GPU parallel across all elements
   - Space: O(1) best case (in-place), O(N) worst case (copy-on-write)
   - GPU: Highly parallel, coalesced memory access
   
   Notes:
   - This is an in-place operation (with copy-on-write semantics)
   - For non-destructive version, use af-select instead
   - Condition array must be boolean type (b8)
   - Empty condition array (0 elements) is no-op
   - Broadcasting follows standard ArrayFire rules
   
   Returns:
   ArrayFire error code (af_err enum):
   - AF_SUCCESS: Operation successful
   - AF_ERR_ARG: Type mismatch (a and b different types, or cond not b8)
   - AF_ERR_SIZE: Dimension mismatch
   - AF_ERR_TYPE: Invalid type
   - AF_ERR_NO_MEM: Insufficient memory
   
   See also:
   - af-replace-scalar: Replace with scalar value
   - af-select: Non-destructive conditional selection
   - af-where: Get indices where condition is true"
  "af_replace" [::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_replace_scalar(af_array a, const af_array cond, const double b)
(defcfn af-replace-scalar
  "Replace array elements based on condition with a scalar value.
   
   Modifies array `a` in-place: where `cond` is false, elements of `a` are
   replaced with the scalar value `b`. Where `cond` is true, elements of
   `a` remain unchanged.
   
   Operation:
   a[i] ← a[i]  if cond[i] = true
   a[i] ← b     if cond[i] = false
   
   Parameters:
   - a: input/output array (modified in-place via copy-on-write)
   - cond: boolean condition array (type b8)
   - b: replacement scalar value (double)
   
   Dimension Requirements:
   - cond.ndims() == a.ndims()
   - cond.dims()[i] == a.dims()[i] for all dimensions
   - Condition must exactly match array dimensions
   
   Scalar Type Conversion:
   - double value converted to array's type automatically
   - For integer types, fractional part truncated
   - For complex types, scalar becomes real part (imaginary = 0)
   - Use af-replace-scalar-long/ulong for precise 64-bit integers
   
   Copy-on-Write Behavior:
   Same as af-replace - creates copy if array has multiple references.
   
   Type Support:
   All types: f32, f64, f16, c32, c64, s32, u32, s64, u64, s16, u16, s8, u8, b8
   
   Example 1 - Clamp values to range:
   ```clojure
   (let [data (create-array data-vals [1000])
         min-val 0.0
         max-val 100.0
         
         ;; Clamp minimum
         _ (af-replace-scalar data (< data min-val) min-val)
         
         ;; Clamp maximum
         _ (af-replace-scalar data (> data max-val) max-val)]
     data) ;; Clamped data in [min-val, max-val]
   ```
   
   Example 2 - Replace NaN with zero:
   ```clojure
   (let [arr (create-array arr-data [100 100])]
     ;; Replace all NaN with 0.0
     (af-replace-scalar arr (isNaN arr) 0.0)
     arr) ;; Array with NaN replaced by 0.0
   ```
   
   Example 3 - Threshold to binary:
   ```clojure
   (let [img (create-array img-data [512 512])
         threshold 0.5]
     ;; Binary thresholding: values below threshold → 0
     (af-replace-scalar img (< img threshold) 0.0)
     img) ;; Thresholded image
   ```
   
   Example 4 - Remove outliers (replace with mean):
   ```clojure
   (let [data (create-array data-vals [10000])
         mean-val (scalar (af-mean data))
         std-val (scalar (af-stdev data))
         
         ;; 3-sigma rule: replace outliers
         outliers (> (abs (- data mean-val)) (* 3.0 std-val))
         _ (af-replace-scalar data outliers mean-val)]
     data) ;; Data with outliers replaced by mean
   ```
   
   Example 5 - Winsorization (replace extremes):
   ```clojure
   (let [data (create-array data-vals [1000])
         
         ;; Compute percentiles (5th and 95th)
         p5 (percentile data 5)
         p95 (percentile data 95)
         
         ;; Replace extremes with percentile values
         _ (af-replace-scalar data (< data p5) p5)
         _ (af-replace-scalar data (> data p95) p95)]
     data) ;; Winsorized data
   ```
   
   Example 6 - Dropout (zero out random elements):
   ```clojure
   (let [activations (create-array act-data [1000 100])
         dropout-rate 0.5
         
         ;; Random mask: keep with probability (1 - dropout-rate)
         keep-mask (< (randu [1000 100]) (- 1.0 dropout-rate))
         
         ;; Zero out dropped elements
         _ (af-replace-scalar activations (not keep-mask) 0.0)]
     activations) ;; Activations with dropout applied
   ```
   
   Example 7 - Gradient clipping:
   ```clojure
   (let [gradients (create-array grad-data [10000])
         clip-value 5.0
         
         ;; Clip large positive gradients
         _ (af-replace-scalar gradients (> gradients clip-value) clip-value)
         
         ;; Clip large negative gradients
         _ (af-replace-scalar gradients (< gradients (- clip-value)) (- clip-value))]
     gradients) ;; Clipped gradients
   ```
   
   Example 8 - Masking with default value:
   ```clojure
   (let [image (create-array img-data [512 512 3])
         mask (create-array mask-data [512 512]) ; Binary mask
         
         ;; Set masked pixels to black (0.0)
         _ (af-replace-scalar image (not mask) 0.0)]
     image) ;; Image with masked regions set to black
   ```
   
   Performance:
   - Time: O(N) - GPU parallel across all elements
   - Space: O(1) best case (in-place), O(N) worst case (copy-on-write)
   - Scalar replacement typically faster than array replacement
   - GPU: Excellent coalescing, no extra array reads
   
   Notes:
   - More efficient than af-replace when replacing with constant value
   - No need to allocate replacement array
   - Scalar broadcast automatically to all replaced positions
   - For complex arrays: scalar becomes (b + 0i)
   - Condition must exactly match array dimensions (no broadcasting)
   
   Returns:
   ArrayFire error code (af_err enum):
   - AF_SUCCESS: Operation successful
   - AF_ERR_ARG: Condition not boolean type (b8)
   - AF_ERR_SIZE: Dimension mismatch between a and cond
   - AF_ERR_TYPE: Invalid array type
   - AF_ERR_NO_MEM: Insufficient memory
   
   See also:
   - af-replace-scalar-long: For 64-bit signed integer values
   - af-replace-scalar-ulong: For 64-bit unsigned integer values
   - af-replace: Replace with array values
   - af-select-scalar-r: Non-destructive version"
  "af_replace_scalar" [::mem/pointer ::mem/pointer ::mem/double] ::mem/int)

;; af_err af_replace_scalar_long(af_array a, const af_array cond, const long long b)
(defcfn af-replace-scalar-long
  "Replace array elements based on condition with a 64-bit signed integer scalar.
   
   Specialized version of af-replace-scalar for large signed integer values
   that cannot be precisely represented as double. Modifies array `a` in-place.
   
   Operation:
   a[i] ← a[i]  if cond[i] = true
   a[i] ← b     if cond[i] = false
   
   Parameters:
   - a: input/output array (modified in-place via copy-on-write)
   - cond: boolean condition array (type b8)
   - b: replacement scalar value (long long / s64)
   
   When to Use:
   - When replacing with large integers (> 2^53) that lose precision in double
   - For exact 64-bit signed integer operations
   - When working with s64 arrays
   
   Precision Comparison:
   - double can exactly represent integers up to ±2^53
   - Beyond ±2^53, double loses precision due to mantissa limit
   - long long provides exact representation for full s64 range
   
   Example 1 - Large integer replacement:
   ```clojure
   (let [ids (create-array id-data [10000]) ; s64 type
         invalid-id -9223372036854775808 ; MIN_LONG
         
         ;; Replace invalid IDs with sentinel
         invalid-mask (= ids invalid-id)
         replacement-id 0
         _ (af-replace-scalar-long ids invalid-mask replacement-id)]
     ids) ;; IDs with invalids replaced
   ```
   
   Example 2 - Timestamp correction:
   ```clojure
   (let [timestamps (create-array ts-data [5000]) ; Unix timestamps (s64)
         
         ;; Replace future timestamps (beyond 2100)
         max-valid-ts 4102444800 ; 2100-01-01 in seconds
         invalid (> timestamps max-valid-ts)
         current-ts 1673740800 ; 2023-01-15
         _ (af-replace-scalar-long timestamps invalid current-ts)]
     timestamps) ;; Corrected timestamps
   ```
   
   Example 3 - Replace with large constants:
   ```clojure
   (let [counters (create-array counter-data [1000]) ; s64
         
         ;; Replace overflow counters
         max-safe-count 1000000000000 ; 10^12
         overflow-mask (> counters max-safe-count)
         _ (af-replace-scalar-long counters overflow-mask max-safe-count)]
     counters) ;; Counters with overflows clamped
   ```
   
   Type Conversion:
   - For s64 arrays: Direct assignment, no conversion
   - For u64 arrays: Negative values may wrap around
   - For floating-point: Converts to float/double
   - For smaller integers: Truncates to target type range
   
   Performance:
   Same as af-replace-scalar - O(N) time, O(1) best-case space.
   
   Notes:
   - Essential for precise 64-bit signed integer operations
   - Prevents precision loss that occurs with double conversion
   - Condition must exactly match array dimensions
   - Copy-on-write semantics apply
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-replace-scalar: General scalar replacement (double)
   - af-replace-scalar-ulong: For unsigned 64-bit integers"
  "af_replace_scalar_long" [::mem/pointer ::mem/pointer ::mem/long] ::mem/int)

;; af_err af_replace_scalar_ulong(af_array a, const af_array cond, const unsigned long long b)
(defcfn af-replace-scalar-ulong
  "Replace array elements based on condition with a 64-bit unsigned integer scalar.
   
   Specialized version of af-replace-scalar for large unsigned integer values
   that cannot be precisely represented as double. Modifies array `a` in-place.
   
   Operation:
   a[i] ← a[i]  if cond[i] = true
   a[i] ← b     if cond[i] = false
   
   Parameters:
   - a: input/output array (modified in-place via copy-on-write)
   - cond: boolean condition array (type b8)
   - b: replacement scalar value (unsigned long long / u64)
   
   When to Use:
   - When replacing with large unsigned integers (> 2^53)
   - For exact 64-bit unsigned integer operations
   - When working with u64 arrays (IDs, hashes, addresses)
   
   Precision Comparison:
   - double can exactly represent unsigned integers up to 2^53
   - Beyond 2^53, precision is lost
   - unsigned long long provides exact representation for full u64 range
   
   Example 1 - Hash value replacement:
   ```clojure
   (let [hashes (create-array hash-data [10000]) ; u64 type
         
         ;; Replace zero hashes with sentinel value
         zero-mask (= hashes 0)
         sentinel-hash 0xFFFFFFFFFFFFFFFF ; Max u64
         _ (af-replace-scalar-ulong hashes zero-mask sentinel-hash)]
     hashes) ;; Hashes with zeros replaced
   ```
   
   Example 2 - Memory address sanitization:
   ```clojure
   (let [addresses (create-array addr-data [1000]) ; u64 pointers
         
         ;; Replace null pointers with invalid address marker
         null-mask (= addresses 0)
         invalid-addr 0xDEADBEEFDEADBEEF
         _ (af-replace-scalar-ulong addresses null-mask invalid-addr)]
     addresses) ;; Addresses with nulls marked
   ```
   
   Example 3 - Large ID correction:
   ```clojure
   (let [user-ids (create-array id-data [100000]) ; u64
         
         ;; Replace placeholder IDs with generated values
         placeholder 9999999999999999999 ; Large u64 value
         placeholder-mask (= user-ids placeholder)
         new-id (generate-unique-id)
         _ (af-replace-scalar-ulong user-ids placeholder-mask new-id)]
     user-ids) ;; IDs with placeholders replaced
   ```
   
   Example 4 - Bit pattern replacement:
   ```clojure
   (let [bit-patterns (create-array pattern-data [5000]) ; u64
         
         ;; Replace corrupted patterns (all 1s) with default
         corrupted-mask (= bit-patterns 0xFFFFFFFFFFFFFFFF)
         default-pattern 0x0000000000000000
         _ (af-replace-scalar-ulong bit-patterns corrupted-mask default-pattern)]
     bit-patterns) ;; Patterns with corrupted ones replaced
   ```
   
   Type Conversion:
   - For u64 arrays: Direct assignment, no conversion
   - For s64 arrays: Large values may appear negative
   - For floating-point: Converts to float/double
   - For smaller integers: Truncates to target type range
   
   Performance:
   Same as af-replace-scalar - O(N) time, O(1) best-case space.
   
   Notes:
   - Essential for precise 64-bit unsigned integer operations
   - Prevents precision loss from double conversion
   - Commonly used for IDs, hashes, pointers, and large counts
   - Condition must exactly match array dimensions
   - Copy-on-write semantics apply
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-replace-scalar: General scalar replacement (double)
   - af-replace-scalar-long: For signed 64-bit integers"
  "af_replace_scalar_ulong" [::mem/pointer ::mem/pointer ::mem/long] ::mem/int)
