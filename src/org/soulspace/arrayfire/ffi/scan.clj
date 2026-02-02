(ns org.soulspace.arrayfire.ffi.scan
  "Bindings for the ArrayFire scan (prefix sum) functions.
   
   Scan operations, also known as prefix sums or cumulative operations, are
   fundamental parallel algorithms that compute running totals or other
   cumulative binary operations along a dimension of an array.
   
   Mathematical Definition:
   
   For an input array A = [a₀, a₁, a₂, ..., aₙ₋₁] and binary operation ⊕:
   
   **Inclusive Scan**:
   Output[i] = a₀ ⊕ a₁ ⊕ ... ⊕ aᵢ
   Result: [a₀, a₀⊕a₁, a₀⊕a₁⊕a₂, ..., a₀⊕a₁⊕...⊕aₙ₋₁]
   
   **Exclusive Scan**:
   Output[i] = identity ⊕ a₀ ⊕ a₁ ⊕ ... ⊕ aᵢ₋₁
   Result: [identity, a₀, a₀⊕a₁, ..., a₀⊕a₁⊕...⊕aₙ₋₂]
   
   Where identity is:
   - 0 for addition
   - 1 for multiplication
   - +∞ for minimum
   - -∞ for maximum
   
   Examples:
   
   **Inclusive Sum** (Cumulative Sum):
   Input:  [1, 2, 3, 4, 5]
   Output: [1, 3, 6, 10, 15]
   
   **Exclusive Sum**:
   Input:  [1, 2, 3, 4, 5]
   Output: [0, 1, 3, 6, 10]
   
   **Inclusive Product**:
   Input:  [1, 2, 3, 4, 5]
   Output: [1, 2, 6, 24, 120]
   
   **Inclusive Maximum** (Running Maximum):
   Input:  [3, 1, 4, 1, 5, 9, 2, 6]
   Output: [3, 3, 4, 4, 5, 9, 9, 9]
   
   **Inclusive Minimum** (Running Minimum):
   Input:  [5, 3, 7, 2, 8, 1, 9]
   Output: [5, 3, 3, 2, 2, 1, 1]
   
   Binary Operations:
   
   Scan supports the following binary operations (af_binary_op enum):
   
   - **AF_BINARY_ADD (0)**: Addition (cumulative sum)
     * Most common scan operation
     * Identity: 0
     * Associative and commutative
   
   - **AF_BINARY_MUL (1)**: Multiplication (cumulative product)
     * Useful for probability calculations
     * Identity: 1
     * Associative and commutative
     * Warning: Can overflow quickly for integer types
   
   - **AF_BINARY_MIN (2)**: Minimum (running minimum)
     * Finds minimum value seen so far
     * Identity: +∞ (maximum value of type)
     * Associative and commutative
   
   - **AF_BINARY_MAX (3)**: Maximum (running maximum)
     * Finds maximum value seen so far
     * Identity: -∞ (minimum value of type)
     * Associative and commutative
   
   Scan By Key:
   
   Scan-by-key is a segmented scan that resets the scan operation whenever
   the key changes. This enables parallel processing of multiple independent
   sequences within a single array.
   
   Example:
   Keys:   [0, 0, 0, 1, 1, 2, 2, 2, 2]
   Values: [1, 2, 3, 4, 5, 6, 7, 8, 9]
   Sum:    [1, 3, 6, 4, 9, 6, 13, 21, 30]
            └─────┘  └───┘  └──────────┘
            Seg 0    Seg 1    Segment 2
   
   The scan resets to identity when the key changes (0→1, 1→2).
   
   Key Requirements:
   - Keys must be integer types: s32, u32, s64, u64
   - Keys and values arrays must have same dimensions
   - Keys should be sorted for meaningful results (not enforced)
   
   Type Conversions:
   
   Some input types are promoted to prevent overflow:
   
   Input Type → Output Type:
   - f32, f64, c32, c64 → Same (no conversion)
   - s32, u32, s64, u64 → Same (no conversion)
   - s16 → s32 (promoted)
   - u16 → u32 (promoted)
   - s8  → s32 (promoted)
   - u8  → u32 (promoted)
   - b8  → u32 (promoted, counts non-zero elements)
   
   Performance Characteristics:
   
   Scan is a fundamental parallel algorithm with excellent GPU performance:
   
   - **Work Complexity**: O(n) total work
   - **Depth Complexity**: O(log n) parallel steps
   - **GPU Speedup**: 10-100× over sequential CPU scan
   - **Memory**: O(n) auxiliary storage for intermediate results
   
   Parallel Implementation:
   The GPU uses the Blelloch scan algorithm (work-efficient):
   1. Up-sweep (reduce) phase: O(log n) steps
   2. Down-sweep (distribute) phase: O(log n) steps
   3. Total parallel time: O(log n) with n/log n processors
   
   Applications:
   
   1. **Stream Compaction**:
      Remove elements from array based on predicate
      - Mark elements to keep (0 or 1)
      - Exclusive scan to get output positions
      - Scatter kept elements to output positions
   
   2. **Radix Sort**:
      Parallel integer sorting using scan
      - Count occurrences of each digit
      - Scan counts to get output positions
      - Scatter elements to sorted positions
   
   3. **Allocation/Memory Management**:
      Allocate dynamic amounts per thread
      - Each thread computes required size
      - Exclusive scan gives starting offset
      - Each thread writes to its allocated region
   
   4. **Polynomial Evaluation**:
      Horner's method for parallel polynomial evaluation
   
   5. **Tree Operations**:
      Build trees, compute tree properties in parallel
   
   6. **Line-of-Sight**:
      Visibility calculations in terrain rendering
      - Running maximum of slope angles
   
   7. **Recurrence Relations**:
      Solve first-order linear recurrences
      - Transform to scan operation
      - Parallelize sequential dependencies
   
   8. **Parallel Reduction**:
      Compute prefix information for divide-and-conquer
   
   9. **Quicksort Partitioning**:
      Parallel partition using dual-scan
   
   10. **Financial Time Series**:
       - Cumulative returns
       - Running totals
       - Moving window statistics (with segmented scan)
   
   Multi-dimensional Arrays:
   
   - Scan operates along a single specified dimension
   - For 2D arrays:
     * dim=0: Scan down columns (cumsum of rows)
     * dim=1: Scan across rows (cumsum of columns)
   - For higher dimensions: Scan along specified axis
   - Other dimensions remain independent
   
   Example (2D array, dim=0):
   Input:     Scan (dim=0):
   [1 2 3]    [1  2  3]
   [4 5 6] →  [5  7  9]
   [7 8 9]    [12 15 18]
   
   Example (2D array, dim=1):
   Input:     Scan (dim=1):
   [1 2 3]    [1  3  6]
   [4 5 6] →  [4  9  15]
   [7 8 9]    [7  15 24]
   
   Best Practices:
   
   1. **Choose appropriate operation**:
      - Use AF_BINARY_ADD for cumulative sums (most common)
      - Use AF_BINARY_MAX/MIN for running extrema
      - Be cautious with AF_BINARY_MUL (overflow risk)
   
   2. **Handle overflow**:
      - For integer types, product scan overflows quickly
      - Consider using f32/f64 for large products
      - Or use logarithmic domain: log(product) = sum(log(values))
   
   3. **Inclusive vs Exclusive**:
      - Inclusive: Last element contains total reduction
      - Exclusive: First element is identity, easier for indexing
      - Exclusive is common in parallel algorithms (allocation, etc.)
   
   4. **Scan-by-key for segmented data**:
      - Process multiple sequences in parallel
      - Avoid separate scans for each segment
      - Keys should be sorted (or use stable sort first)
   
   5. **Dimension selection**:
      - Choose dimension based on data layout
      - Consider memory access patterns
      - GPU prefers coalesced memory access
   
   Limitations:
   
   - Dimension must be 0-3 (ARG_ASSERT will fail otherwise)
   - For scan-by-key: keys must be integer types (s32, u32, s64, u64)
   - For scan-by-key: keys and values must have same dimensions
   - Only 4 binary operations supported (add, mul, min, max)
   - Operation must be associative for correct results
   
   Historical Note:
   
   Parallel scan (prefix sum) was one of the first non-trivial parallel
   algorithms to be studied systematically. Guy Blelloch's 1990 work on
   work-efficient parallel scan revolutionized GPU computing, enabling
   efficient implementation of numerous algorithms that seem inherently
   sequential.
   
   See also:
   - af-sum: Total reduction (single value)
   - af-product: Total product reduction
   - af-diff: Differences (inverse of cumulative sum)
   - Reduce functions for other reductions"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; af_err af_accum(af_array *out, const af_array in, const int dim)
(defcfn af-accum
  "Compute cumulative sum (inclusive scan with addition) along a dimension.
   
   Convenience function for inclusive scan with AF_BINARY_ADD operation.
   Computes the running sum along the specified dimension.
   
   Parameters:
   - out: Output pointer for cumulative sum array
   - in: Input array
   - dim: Dimension along which to compute cumulative sum (0-3)
     * 0: Accumulate down columns (sum rows)
     * 1: Accumulate across rows (sum columns)
     * 2, 3: Accumulate along higher dimensions
   
   Operation:
   Output[i] = Input[0] + Input[1] + ... + Input[i]
   
   This is equivalent to:
   af-scan(out, in, dim, AF_BINARY_ADD, true)
   
   Type Conversions:
   To prevent overflow, small integer types are promoted:
   - f32, f64, c32, c64, s32, u32, s64, u64 → Same type
   - s16 → s32, u16 → u32
   - s8 → s32, u8 → u32
   - b8 → u32 (counts non-zero elements, not sum of values)
   
   Example (1D array):
   ```clojure
   (let [data (create-array [1 2 3 4 5] [5])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-accum out-ptr data 0)]
     (mem/read-pointer out-ptr ::mem/pointer))
   ;; Result: [1, 3, 6, 10, 15]
   ```
   
   Example (2D array, accumulate down columns):
   ```clojure
   (let [data (create-array [[1 2 3]
                             [4 5 6]
                             [7 8 9]] [3 3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-accum out-ptr data 0)]  ; dim=0: down columns
     (mem/read-pointer out-ptr ::mem/pointer))
   ;; Result: [[1  2  3]
   ;;          [5  7  9]
   ;;          [12 15 18]]
   ```
   
   Example (2D array, accumulate across rows):
   ```clojure
   (let [data (create-array [[1 2 3]
                             [4 5 6]] [2 3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-accum out-ptr data 1)]  ; dim=1: across rows
     (mem/read-pointer out-ptr ::mem/pointer))
   ;; Result: [[1  3  6]
   ;;          [4  9  15]]
   ```
   
   Example (financial cumulative returns):
   ```clojure
   ;; Daily returns to cumulative returns
   (let [daily-returns (create-array [0.01 -0.02 0.03 0.01 -0.01] [5])
         cum-returns-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-accum cum-returns-ptr daily-returns 0)]
     (mem/read-pointer cum-returns-ptr ::mem/pointer))
   ;; Result: [0.01, -0.01, 0.02, 0.03, 0.02]
   ```
   
   Example (counting events):
   ```clojure
   ;; Boolean array: count cumulative occurrences
   (let [events (create-array [1 0 1 1 0 1 0 1] [8])  ; 1=event occurred
         count-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-accum count-ptr events 0)]
     (mem/read-pointer count-ptr ::mem/pointer))
   ;; Result: [1, 1, 2, 3, 3, 4, 4, 5] - running count of events
   ```
   
   Applications:
   - Financial analysis: Cumulative returns, running totals
   - Signal processing: Integration, cumulative energy
   - Statistics: Cumulative distribution functions
   - Physics: Position from velocity, charge accumulation
   - Computer graphics: Opacity accumulation, path tracing
   
   Performance:
   - Complexity: O(n) work, O(log n) parallel depth
   - GPU highly efficient (parallel prefix sum algorithm)
   - For large arrays: 10-100× faster than CPU
   - Memory: O(n) output + O(n) temporary storage
   
   Dimension Handling:
   - If dim >= number of dimensions in input, returns copy of input
   - No error for dim >= ndims, just returns input unchanged
   - dim must be 0-3 (asserts otherwise)
   
   Notes:
   - This is an inclusive scan (output[i] includes input[i])
   - For exclusive scan, use af-scan with inclusive_scan=false
   - Also known as: cumsum, prefix sum, scan
   - Inverse operation: af-diff (differences)
   
   Overflow Warning:
   - For integer types, sum can overflow
   - Consider using f32/f64 for large sums
   - Or use s64/u64 for maximum integer range
   
   Returns:
   ArrayFire error code (af_err enum)
   - AF_SUCCESS (0): Cumulative sum computed successfully
   - AF_ERR_ARG: Invalid dimension (dim < 0 or dim >= 4)
   - AF_ERR_TYPE: Unsupported data type
   
   See also:
   - af-scan: Generalized scan with any binary operation
   - af-sum: Total sum (single value reduction)
   - af-diff: Differences (inverse of cumsum)"
  "af_accum" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_scan(af_array *out, const af_array in, const int dim, af_binary_op op, bool inclusive_scan)
(defcfn af-scan
  "Perform generalized scan operation along a dimension.
   
   Computes a running binary operation (prefix operation) along the specified
   dimension. This is a generalization of cumulative sum to arbitrary
   associative binary operations.
   
   Parameters:
   - out: Output pointer for scan result
   - in: Input array
   - dim: Dimension along which to scan (0-3)
   - op: Binary operation to apply (af_binary_op enum)
     * 0 (AF_BINARY_ADD): Cumulative sum
     * 1 (AF_BINARY_MUL): Cumulative product
     * 2 (AF_BINARY_MIN): Running minimum
     * 3 (AF_BINARY_MAX): Running maximum
   - inclusive-scan: Boolean flag
     * true (1): Inclusive scan (output[i] includes input[i])
     * false (0): Exclusive scan (output[i] excludes input[i])
   
   Scan Types:
   
   **Inclusive Scan**:
   Output[i] = Input[0] ⊕ Input[1] ⊕ ... ⊕ Input[i]
   
   **Exclusive Scan**:
   Output[i] = identity ⊕ Input[0] ⊕ Input[1] ⊕ ... ⊕ Input[i-1]
   Output[0] = identity (0 for add, 1 for mul, etc.)
   
   Type Support:
   - All numeric types: f32, f64, s32, u32, s64, u64
   - Complex types: c32, c64 (for add/mul operations)
   - Promoted types: s16→s32, u16→u32, s8→s32, u8→u32, b8→u32
   
   Example (inclusive sum):
   ```clojure
   (let [data (create-array [1 2 3 4 5] [5])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-scan out-ptr data 0 0 true)]  ; op=0 (ADD), inclusive
     (mem/read-pointer out-ptr ::mem/pointer))
   ;; Result: [1, 3, 6, 10, 15]
   ```
   
   Example (exclusive sum):
   ```clojure
   (let [data (create-array [1 2 3 4 5] [5])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-scan out-ptr data 0 0 false)]  ; op=0 (ADD), exclusive
     (mem/read-pointer out-ptr ::mem/pointer))
   ;; Result: [0, 1, 3, 6, 10]
   ```
   
   Example (cumulative product):
   ```clojure
   (let [data (create-array [1.0 2.0 3.0 4.0] [4])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-scan out-ptr data 0 1 true)]  ; op=1 (MUL), inclusive
     (mem/read-pointer out-ptr ::mem/pointer))
   ;; Result: [1.0, 2.0, 6.0, 24.0]
   ```
   
   Example (running maximum):
   ```clojure
   (let [data (create-array [3 1 4 1 5 9 2 6] [8])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-scan out-ptr data 0 3 true)]  ; op=3 (MAX), inclusive
     (mem/read-pointer out-ptr ::mem/pointer))
   ;; Result: [3, 3, 4, 4, 5, 9, 9, 9]
   ```
   
   Example (running minimum):
   ```clojure
   (let [data (create-array [5 3 7 2 8 1 9] [7])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-scan out-ptr data 0 2 true)]  ; op=2 (MIN), inclusive
     (mem/read-pointer out-ptr ::mem/pointer))
   ;; Result: [5, 3, 3, 2, 2, 1, 1]
   ```
   
   Example (stream compaction - using exclusive scan):
   ```clojure
   ;; Remove negative numbers from array
   (let [data (create-array [3 -1 4 -2 5 9 -6] [7])
         ;; Step 1: Mark elements to keep (1 if >= 0, 0 otherwise)
         keep-mask (ge data 0)  ; [1 0 1 0 1 1 0]
         ;; Step 2: Exclusive scan to get output indices
         indices-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-scan indices-ptr keep-mask 0 0 false)  ; [0 1 1 2 2 3 4]
         indices (mem/read-pointer indices-ptr ::mem/pointer)
         ;; Step 3: Scatter kept elements (not shown - needs gather)
         ;; Result would be: [3 4 5 9]
         ])
   ```
   
   Example (2D scan along different dimensions):
   ```clojure
   ;; Scan 2D array down columns (dim=0)
   (let [data (create-array [[1 2 3]
                             [4 5 6]] [2 3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-scan out-ptr data 0 0 true)]  ; dim=0, sum
     (mem/read-pointer out-ptr ::mem/pointer))
   ;; Result: [[1  2  3]
   ;;          [5  7  9]]
   
   ;; Scan 2D array across rows (dim=1)
   (let [data (create-array [[1 2 3]
                             [4 5 6]] [2 3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-scan out-ptr data 1 0 true)]  ; dim=1, sum
     (mem/read-pointer out-ptr ::mem/pointer))
   ;; Result: [[1  3  6]
   ;;          [4  9  15]]
   ```
   
   Applications by Operation:
   
   **ADD (cumulative sum)**:
   - Financial: Cumulative returns, running totals
   - Physics: Integration, cumulative displacement
   - Statistics: CDF computation
   - Allocation: Compute output positions
   
   **MUL (cumulative product)**:
   - Probability: Joint probabilities
   - Compound interest: Cumulative growth factors
   - Signal processing: Cumulative gain
   - Warning: Overflows quickly for integers!
   
   **MAX (running maximum)**:
   - Peak detection: Maximum value seen so far
   - Envelope detection: Upper envelope of signal
   - Line-of-sight: Visibility in terrain rendering
   - Monotone sequences: Force non-decreasing
   
   **MIN (running minimum)**:
   - Valley detection: Minimum value seen so far
   - Lower envelope: Envelope of signal
   - Optimization: Track best solution so far
   - Monotone sequences: Force non-increasing
   
   Performance:
   - Work complexity: O(n)
   - Parallel depth: O(log n)
   - GPU speedup: 10-100× over CPU sequential scan
   - Memory: O(n) output + O(n) temporary
   
   Inclusive vs Exclusive:
   - **Inclusive**: Last element = total reduction
     * More intuitive for cumulative operations
     * Direct interpretation (sum so far)
   - **Exclusive**: First element = identity
     * Better for indexing/allocation algorithms
     * Output[i] = sum of all elements before i
     * Common in parallel algorithms
   
   Parallel Algorithm:
   GPU uses work-efficient parallel scan (Blelloch):
   1. Up-sweep phase: Reduce to root (O(log n) steps)
   2. Down-sweep phase: Distribute results (O(log n) steps)
   3. Total: O(n) work, O(log n) depth
   
   Dimension Handling:
   - If dim >= ndims: Returns copy of input (no scan needed)
   - dim must be 0-3 (will assert-fail otherwise)
   - Scan operates independently along specified dimension
   
   Notes:
   - Binary operation must be associative for correct results
   - For commutative operations (add, mul, max, min), order doesn't matter
   - Complex types only support add and mul operations
   - Boolean (b8) treated as 0/1 for counting
   
   Overflow Considerations:
   - Integer addition: Can overflow for large sums
   - Integer multiplication: Overflows very quickly!
   - Use f32/f64 for large products (convert to log domain if possible)
   - Max/Min operations: No overflow (bounded by input range)
   
   Returns:
   ArrayFire error code (af_err enum)
   - AF_SUCCESS (0): Scan computed successfully
   - AF_ERR_ARG: Invalid dim (< 0 or >= 4) or invalid op
   - AF_ERR_TYPE: Unsupported type for operation
   
   See also:
   - af-accum: Convenience function for cumulative sum
   - af-scan-by-key: Segmented scan with keys
   - af-sum: Total reduction (single value)
   - af-product, af-min, af-max: Total reductions"
  "af_scan" [::mem/pointer ::mem/pointer ::mem/int ::mem/int ::mem/int] ::mem/int)

;; af_err af_scan_by_key(af_array *out, const af_array key, const af_array in, const int dim, af_binary_op op, bool inclusive_scan)
(defcfn af-scan-by-key
  "Perform segmented scan operation based on key array.
   
   Computes a scan operation that resets whenever the key changes.
   This enables parallel processing of multiple independent sequences
   within a single array. The scan accumulates values within each
   segment defined by consecutive equal keys.
   
   Parameters:
   - out: Output pointer for scan result
   - key: Key array (must be integer type: s32, u32, s64, u64)
   - in: Input values array
   - dim: Dimension along which to scan (0-3)
   - op: Binary operation (0=ADD, 1=MUL, 2=MIN, 3=MAX)
   - inclusive-scan: Boolean flag (1=inclusive, 0=exclusive)
   
   Operation:
   Scan resets to identity value whenever key[i] ≠ key[i-1].
   Each segment with the same key is scanned independently.
   
   Key Requirements:
   - Must be integer type: s32, u32, s64, or u64
   - Should be sorted for meaningful results (not enforced)
   - Same dimensions as input values array
   
   Segmentation:
   Keys define segments. Scan resets at segment boundaries.
   
   Example:
   Keys:   [0, 0, 0, 1, 1, 1, 2, 2]
   Values: [1, 2, 3, 4, 5, 6, 7, 8]
   Sum:    [1, 3, 6, 4, 9, 15, 7, 15]
            └─────┘  └─────┘  └────┘
            Seg 0    Seg 1    Seg 2
   
   The scan resets when key changes (0→1 and 1→2).
   
   Example (basic segmented sum):
   ```clojure
   (let [keys (create-array [0 0 0 1 1 2 2 2] [8])
         vals (create-array [1 2 3 4 5 6 7 8] [8])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-scan-by-key out-ptr keys vals 0 0 true)]  ; ADD, inclusive
     (mem/read-pointer out-ptr ::mem/pointer))
   ;; Result: [1, 3, 6, 4, 9, 6, 13, 21]
   ;;          └─────┘  └───┘  └───────┘
   ;;          Key=0    Key=1   Key=2
   ```
   
   Example (exclusive segmented sum):
   ```clojure
   (let [keys (create-array [0 0 0 1 1 1] [6])
         vals (create-array [5 3 7 2 8 4] [6])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-scan-by-key out-ptr keys vals 0 0 false)]  ; ADD, exclusive
     (mem/read-pointer out-ptr ::mem/pointer))
   ;; Result: [0, 5, 8, 0, 2, 10]
   ;;          └─────┘  └────────┘
   ;;          Seg 0    Segment 1
   ;; (First element of each segment is identity = 0)
   ```
   
   Example (running maximum per group):
   ```clojure
   ;; Find running maximum for each category
   (let [category (create-array [1 1 1 2 2 2 3 3] [8])
         values   (create-array [3 1 4 5 2 7 6 8] [8])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-scan-by-key out-ptr category values 0 3 true)]  ; MAX
     (mem/read-pointer out-ptr ::mem/pointer))
   ;; Result: [3, 3, 4, 5, 5, 7, 6, 8]
   ;;          └─────┘  └─────┘  └───┘
   ;;          Cat 1    Cat 2    Cat 3
   ```
   
   Example (time series by symbol):
   ```clojure
   ;; Cumulative returns per stock symbol
   (let [symbols (create-array [101 101 101 102 102 103 103 103] [8])  ; Stock IDs
         returns (create-array [0.01 -0.02 0.03 0.02 -0.01 0.01 0.02 -0.01] [8])
         cum-ret-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-scan-by-key cum-ret-ptr symbols returns 0 0 true)]
     (mem/read-pointer cum-ret-ptr ::mem/pointer))
   ;; Result: [0.01, -0.01, 0.02, 0.02, 0.01, 0.01, 0.03, 0.02]
   ;;          └──────────────┘  └────────┘  └──────────────┘
   ;;          Stock 101         Stock 102   Stock 103
   ```
   
   Example (2D segmented scan):
   ```clojure
   ;; Scan by key along each column independently
   (let [keys (create-array [[1 2 3]
                             [1 2 3]
                             [1 2 4]] [3 3])  ; Third column has different segment
         vals (create-array [[1 10 100]
                             [2 20 200]
                             [3 30 300]] [3 3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-scan-by-key out-ptr keys vals 0 0 true)]  ; dim=0, ADD
     (mem/read-pointer out-ptr ::mem/pointer))
   ;; Result: [[1  10  100]
   ;;          [3  30  300]  ; Cumsum continues within key
   ;;          [6  60  300]] ; Third col resets (key 3→4)
   ```
   
   Applications:
   
   **Time Series Analysis**:
   - Cumulative values per entity (stock, sensor, etc.)
   - Running statistics per group
   - Segmented moving averages
   
   **Database Operations**:
   - GROUP BY with cumulative aggregation
   - Window functions (OVER PARTITION BY)
   - Running totals per category
   
   **Graph Algorithms**:
   - Process disconnected components
   - Per-vertex accumulation in segmented vertices
   
   **Stream Processing**:
   - Accumulate per session/window
   - Reset counters on key change
   - Segmented event processing
   
   **Machine Learning**:
   - Batch normalization per group
   - Cumulative features per sequence
   - RNN hidden state initialization per sequence
   
   **Image Processing**:
   - Region-based cumulative operations
   - Segmented filtering
   - Per-object statistics
   
   Performance:
   - Slightly slower than regular scan (needs key comparisons)
   - Still highly parallel: O(n) work, O(log n) depth
   - GPU efficient for many segments
   - Best when segments aren't too small (< 32 elements)
   
   Key Sorting:
   - Keys should be sorted for meaningful segmentation
   - Unsorted keys will still work but may not give expected results
   - Example unsorted: keys=[1,2,1] → 3 segments, not 2!
   - Use af-sort-by-key first if keys aren't sorted
   
   Segment Boundaries:
   - Detected by comparing consecutive keys
   - Keys are compared for inequality (key[i] ≠ key[i-1])
   - Exact equality testing (no tolerance for floating-point)
   
   Type Conversions:
   Like regular scan:
   - Small integer types promoted: s16→s32, u16→u32, s8→s32, u8→u32, b8→u32
   - Float and large integer types unchanged
   
   Dimension Handling:
   - If dim >= ndims: Returns copy of input
   - dim must be 0-3 (will assert-fail otherwise)
   - Keys and values must have same dimensions
   
   Notes:
   - Keys must be integer types (s32, u32, s64, u64)
   - Keys and values must have identical dimensions
   - Empty segments (no elements with that key) are fine
   - Single-element segments work correctly
   - Keys should be sorted for intuitive results
   
   Common Patterns:
   
   **Per-group cumulative sum**:
   ```clojure
   ;; First sort by group, then scan by key
   (let [groups (create-array [2 1 2 1 2 3] [6])
         values (create-array [5 3 7 8 2 9] [6])
         ;; Sort by groups first
         sorted-groups (sort groups 0 true)
         sorted-values (sort-by-key groups values 0 true)
         ;; Now scan by sorted groups
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-scan-by-key out-ptr sorted-groups sorted-values 0 0 true)]
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Returns:
   ArrayFire error code (af_err enum)
   - AF_SUCCESS (0): Segmented scan computed successfully
   - AF_ERR_ARG: Invalid dim, dimensions mismatch, or invalid key type
   - AF_ERR_TYPE: Unsupported value type or non-integer key type
   
   See also:
   - af-scan: Regular (non-segmented) scan
   - af-sort-by-key: Sort arrays by key before scan
   - af-accum: Simple cumulative sum
   - Reduce functions for per-segment reductions"
  "af_scan_by_key" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/int ::mem/int] ::mem/int)
