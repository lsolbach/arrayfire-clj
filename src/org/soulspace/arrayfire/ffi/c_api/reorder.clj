(ns org.soulspace.arrayfire.ffi.c-api.reorder
  "Bindings for the ArrayFire dimension reordering functions.
   
   Dimension reordering is a fundamental array manipulation operation that
   changes the order of dimensions while preserving the linear ordering of
   data in memory. This enables efficient operations like transposition,
   reshaping, and data reorganization without copying the underlying data
   when possible.
   
   ## What is Dimension Reordering?
   
   Dimension reordering (also called axis permutation or dimension permutation)
   rearranges the dimensions of an array according to a specified order. The
   data itself is not moved in memory when possible - instead, the dimension
   metadata (sizes and strides) are modified to reflect the new view.
   
   **Input**: Array with dimensions [d0, d1, d2, d3] and reorder indices [x, y, z, w]
   **Output**: Array with dimensions [d[x], d[y], d[z], d[w]]
   
   ## Mathematical Foundation
   
   For an array A with dimensions (d0, d1, d2, d3) and indices i = (i0, i1, i2, i3),
   reordering with permutation p = (x, y, z, w) creates array B where:
   
   B[i[x], i[y], i[z], i[w]] = A[i0, i1, i2, i3]
   
   The linear index in memory is preserved:
   linear_index(A, i) = linear_index(B, p(i))
   
   ## Dimension Indexing
   
   Dimensions are zero-indexed:
   - 0: First dimension (rows in column-major layout)
   - 1: Second dimension (columns in column-major layout)
   - 2: Third dimension (depth/channels)
   - 3: Fourth dimension (batch)
   
   ## Common Reordering Patterns
   
   ### Transpose (2D)
   ```
   Original: [rows, cols, 1, 1]
   reorder(arr, 1, 0, 2, 3)
   Result:   [cols, rows, 1, 1]
   ```
   
   ### Channel-First to Channel-Last (Image Data)
   ```
   Original: [width, height, channels, batch]
   reorder(arr, 0, 1, 2, 3)  // No change (already this format)
   
   Or if starting with:
   Original: [channels, height, width, batch]  // Channel-first
   reorder(arr, 2, 1, 0, 3)
   Result:   [width, height, channels, batch]  // Channel-last
   ```
   
   ### Batch Dimension Movement
   ```
   Original: [rows, cols, depth, batch]
   reorder(arr, 3, 0, 1, 2)
   Result:   [batch, rows, cols, depth]
   ```
   
   ### 3D Volume Rotation
   ```
   Original: [x, y, z, batch]
   reorder(arr, 2, 0, 1, 3)  // Rotate axes
   Result:   [z, x, y, batch]
   ```
   
   ## Optimization: Metadata-Only vs Data Movement
   
   ArrayFire optimizes reorder operations when possible:
   
   ### Case 1: Identity (No-op)
   ```clojure
   ;; reorder(arr, 0, 1, 2, 3) - dimensions unchanged
   ;; Result: Reference to original array (af_retain_array)
   ;; Cost: O(1) - no data movement
   ```
   
   ### Case 2: Transpose (2D)
   ```clojure
   ;; reorder(arr, 1, 0, 2, 3) - swap first two dimensions
   ;; Result: Uses specialized transpose operation
   ;; Cost: O(1) or O(N) depending on array state
   ```
   
   ### Case 3: Metadata-Only Reorder
   ```clojure
   ;; When first dimension unchanged: reorder(arr, 0, y, z, w)
   ;; Result: Modifies dimension and stride metadata without copying
   ;; Cost: O(1) - no data movement
   ;; Limitation: Works only when dim[0] stays in position 0
   ```
   
   ### Case 4: Full Data Reorder
   ```clojure
   ;; General case: reorder(arr, x, y, z, w) where x ≠ 0
   ;; Result: GPU kernel rearranges data in memory
   ;; Cost: O(N) - full data copy with reordering
   ```
   
   ## Algorithm Details
   
   The reorder operation uses different strategies based on the permutation:
   
   **Strategy 1: Identity (x=0, y=1, z=2, w=3)**
   - Simply retain the array (increase reference count)
   - No computation required
   
   **Strategy 2: Transpose Detection (x=1, y=0, ...)**
   - Use optimized transpose kernel
   - GPU memory coalescing optimized
   
   **Strategy 3: First-Dimension-Fixed (x=0)**
   - Modify dimension sizes and strides only
   - No data movement in memory
   - Requires evaluation of pending JIT operations first
   
   **Strategy 4: General Reorder**
   - Parallel GPU kernel with proper indexing
   - Each thread computes input index from output index
   - Handles arbitrary dimension permutations
   
   **Complexity**: 
   - Metadata-only: O(1) time, O(1) space
   - Full reorder: O(N) time, O(N) space where N = total elements
   
   ## Examples
   
   ### Example 1: Basic Transpose (2D Matrix)
   ```clojure
   ;; Original matrix [2, 3] (2 rows, 3 columns)
   ;; [[1 3 5]
   ;;  [2 4 6]]
   (let [data (float-array [1.0 2.0 3.0 4.0 5.0 6.0])
         arr (create-array data [2 3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         ;; Transpose: swap rows and columns
         err (af-reorder out-ptr arr 
                         1  ;; dim 1 becomes first (columns -> rows)
                         0  ;; dim 0 becomes second (rows -> columns)
                         2  ;; dim 2 unchanged
                         3)] ;; dim 3 unchanged
     (when (zero? err)
       (let [result (mem/read-pointer out-ptr ::mem/pointer)]
         ;; Result [3, 2] (3 rows, 2 columns)
         ;; [[1 2]
         ;;  [3 4]
         ;;  [5 6]]
         result)))
   ```
   
   ### Example 2: RGB to BGR Conversion (3D Image)
   ```clojure
   ;; Image stored as [width, height, channels=3]
   ;; Channels: 0=R, 1=G, 2=B
   ;; Want to swap R and B channels
   (let [rgb-img (create-array img-data [640 480 3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         ;; Keep width and height, but reverse channel order conceptually
         ;; Note: This doesn't swap RGB->BGR, it reorders dimensions
         ;; For channel swapping, use indexing or other operations
         err (af-reorder out-ptr rgb-img 0 1 2 3)]
     ;; For actual BGR conversion, you'd need channel selection
     (when (zero? err)
       (mem/read-pointer out-ptr ::mem/pointer)))
   ```
   
   ### Example 3: Batch-First to Batch-Last
   ```clojure
   ;; Neural network batch processing
   ;; Original: [batch, channels, height, width] (PyTorch style)
   ;; Target:   [height, width, channels, batch] (TensorFlow style)
   (let [batch-first (create-array data [32 3 224 224])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         err (af-reorder out-ptr batch-first
                         2  ;; height becomes first
                         3  ;; width becomes second  
                         1  ;; channels becomes third
                         0)] ;; batch becomes fourth
     (when (zero? err)
       (let [batch-last (mem/read-pointer out-ptr ::mem/pointer)]
         ;; Result: [224, 224, 3, 32]
         batch-last)))
   ```
   
   ### Example 4: 3D Volume Axis Rotation
   ```clojure
   ;; Medical imaging: rotate CT scan volume axes
   ;; Original: [sagittal, coronal, axial] (X, Y, Z)
   ;; Want:     [axial, sagittal, coronal] (Z, X, Y)
   (let [volume (create-array scan-data [256 256 128])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         err (af-reorder out-ptr volume
                         2  ;; Z (axial) becomes first
                         0  ;; X (sagittal) becomes second
                         1  ;; Y (coronal) becomes third
                         3)] ;; batch unchanged
     (when (zero? err)
       (mem/read-pointer out-ptr ::mem/pointer)))
   ```
   
   ### Example 5: Matrix to Column Vector
   ```clojure
   ;; Reshape by reordering dimensions
   ;; Original: [5, 4, 1, 1] (5x4 matrix)
   ;; Want column view: [20, 1, 1, 1] (20-element column vector)
   ;; Note: Use af_flat or af_moddims for reshaping, not reorder
   ;; Reorder only permutes existing dimensions
   (let [matrix (create-array data [5 4])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         ;; This keeps dimensions as-is, no flattening
         err (af-reorder out-ptr matrix 0 1 2 3)]
     ;; For flattening, use af-flat instead
     (when (zero? err)
       (mem/read-pointer out-ptr ::mem/pointer)))
   ```
   
   ### Example 6: FFT Data Reorganization
   ```clojure
   ;; FFT output often needs dimension reordering
   ;; Original: [freq, time, channels, batch]
   ;; Want:     [time, freq, channels, batch]
   (let [fft-out (create-array freq-data [512 1024 2 16])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         err (af-reorder out-ptr fft-out
                         1  ;; time becomes first
                         0  ;; frequency becomes second
                         2  ;; channels unchanged
                         3)] ;; batch unchanged
     (when (zero? err)
       (mem/read-pointer out-ptr ::mem/pointer)))
   ```
   
   ### Example 7: Data Layout for External Libraries
   ```clojure
   ;; Prepare data for library expecting different layout
   ;; ArrayFire (column-major): [rows, cols, depth, batch]
   ;; External lib (row-major concept): needs [cols, rows, depth, batch]
   (defn prepare-for-external-lib [arrayfire-data]
     (let [out-ptr (mem/alloc-pointer ::mem/pointer)
           err (af-reorder out-ptr arrayfire-data
                           1  ;; cols first
                           0  ;; rows second
                           2  ;; depth unchanged
                           3)] ;; batch unchanged
       (when (zero? err)
         (mem/read-pointer out-ptr ::mem/pointer))))
   ```
   
   ### Example 8: Optimized Batch Processing
   ```clojure
   ;; Move batch dimension to optimize memory access patterns
   ;; Original: [features, samples, 1, batches]
   ;; Optimized: [features, batches, samples, 1] for certain operations
   (let [features (create-array data [128 1000 1 8])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         err (af-reorder out-ptr features
                         0  ;; features unchanged
                         3  ;; batches becomes second
                         1  ;; samples becomes third
                         2)] ;; singleton dimension fourth
     (when (zero? err)
       (mem/read-pointer out-ptr ::mem/pointer)))
   ```
   
   ## Performance Characteristics
   
   **Time Complexity**:
   - Identity reorder: O(1)
   - Metadata-only: O(1) 
   - Transpose (specialized): O(N) with good memory coalescing
   - General reorder: O(N) with potential cache misses
   
   **Space Complexity**:
   - Metadata-only: O(1) - no new array allocated
   - Data reorder: O(N) - new array with reordered data
   
   **GPU Optimization**:
   - Parallel processing: each thread computes independent element
   - Coalesced memory access when possible
   - Tile-based algorithms for better cache utilization
   - Block dimensions: typically 32×8 threads
   - Tile size: 512×32 elements
   
   **Memory Layout Impact**:
   - Column-major (ArrayFire default): consecutive elements in first dimension
   - Best performance when first dimension unchanged (x=0)
   - Worst case: reordering first dimension (x≠0) causes non-coalesced access
   
   ## Validation Rules
   
   The reorder parameters must satisfy:
   
   1. **All dimensions present**: {x, y, z, w} must be permutation of {0, 1, 2, 3}
   2. **No duplicates**: Each of x, y, z, w must be unique
   3. **Valid range**: Each parameter must be in [0, 3]
   
   Example validation:
   ```clojure
   ;; Valid:   (af-reorder out arr 2 0 3 1) ✓ - all dims present once
   ;; Valid:   (af-reorder out arr 0 1 2 3) ✓ - identity
   ;; Valid:   (af-reorder out arr 1 0 2 3) ✓ - transpose
   ;; Invalid: (af-reorder out arr 0 0 2 3) ✗ - dimension 0 repeated
   ;; Invalid: (af-reorder out arr 0 1 2 4) ✗ - dimension 4 out of range
   ;; Invalid: (af-reorder out arr 2 1 0 0) ✗ - dimension 0 repeated
   ```
   
   ## Applications
   
   ### Machine Learning
   - Batch dimension management (batch-first ↔ batch-last)
   - Channel ordering (channel-first ↔ channel-last)
   - Preparing data for different frameworks (PyTorch ↔ TensorFlow)
   - Weight matrix transposition
   
   ### Image Processing
   - Image format conversion (HWC ↔ CHW)
   - Multi-view geometry (reorder spatial dimensions)
   - Video frame organization
   
   ### Signal Processing
   - Time-frequency representation reordering
   - Multi-channel audio processing
   - Sensor array data reorganization
   
   ### Scientific Computing
   - Matrix transposition for linear algebra
   - Tensor contraction preparation
   - Simulation data reorganization
   - Multi-dimensional FFT preparation
   
   ### Computer Vision
   - Feature map reorganization in CNNs
   - Stereo image pair arrangement
   - Multi-scale pyramid processing
   
   ### Data Analysis
   - Pivot table-like reorganization
   - Time series dimension swapping
   - Multi-dimensional data cube slicing
   
   ## Best Practices
   
   1. **Minimize Reordering**:
      - Design algorithms to work with natural data layout
      - Batch reordering operations when possible
      - Consider data layout at algorithm design time
   
   2. **Leverage Metadata-Only Operations**:
      - When possible, keep first dimension unchanged (x=0)
      - This avoids data copying
      - Especially important for large arrays
   
   3. **Chain Operations**:
      - Combine reorder with subsequent operations
      - ArrayFire's JIT may optimize the chain
      - Example: reorder + reduce may fuse
   
   4. **Type Awareness**:
      - All data types supported (no type restrictions)
      - Performance independent of data type
      - Complex types handled correctly
   
   5. **Memory Management**:
      - Always release output arrays when done
      - Consider array lifetime in loops
      - Use resource management (try-finally)
   
   6. **Validation**:
      - Verify dimension permutation is valid
      - Check that all dimensions 0-3 appear exactly once
      - Use assertions for debugging
   
   ## Common Pitfalls
   
   1. **Invalid Permutation**:
      ```clojure
      ;; ERROR: Dimension 1 repeated
      (af-reorder out arr 1 1 2 3)  ; AF_ERR_SIZE
      
      ;; ERROR: Dimension 4 invalid
      (af-reorder out arr 0 1 4 3)  ; AF_ERR_SIZE
      
      ;; ERROR: Missing dimension 2
      (af-reorder out arr 0 1 3 3)  ; AF_ERR_SIZE
      ```
   
   2. **Confusing Reorder with Reshape**:
      - Reorder permutes dimensions, preserves shape product
      - For shape changes, use af-moddims
      ```clojure
      ;; Wrong: trying to flatten with reorder
      (af-reorder out matrix-2x3 0 1 2 3)  ; Still [2, 3]
      
      ;; Correct: use moddims for reshape
      (af-moddims out matrix-2x3 [6 1])  ; Now [6, 1]
      ```
   
   3. **Incorrect Transpose Syntax**:
      ```clojure
      ;; Wrong: trying to transpose [10, 5] matrix
      (af-reorder out arr 0 1 2 3)  ; No change (identity)
      
      ;; Correct: swap first two dimensions
      (af-reorder out arr 1 0 2 3)  ; Now [5, 10]
      ```
   
   4. **Forgetting Higher Dimensions**:
      ```clojure
      ;; Be explicit about all 4 dimensions
      ;; Even for 2D arrays, specify all parameters
      (af-reorder out arr2d 1 0 2 3)  ; Correct
      ```
   
   5. **Performance Assumptions**:
      - Don't assume all reorders are O(1)
      - Metadata-only works only for specific patterns
      - Profile to verify optimization applies
   
   6. **Memory Leaks**:
      ```clojure
      ;; Always release the output array
      (let [out-ptr (mem/alloc-pointer ::mem/pointer)
            err (af-reorder out-ptr arr 1 0 2 3)]
        (try
          (when (zero? err)
            (let [result (mem/read-pointer out-ptr ::mem/pointer)]
              ;; Use result...
              result))
          (finally
            ;; Release the array
            (when (zero? err)
              (af-release-array (mem/read-pointer out-ptr ::mem/pointer))))))
      ```
   
   ## Relationship to Other Operations
   
   **vs. Transpose (af-transpose)**:
   - Transpose: Specialized 2D operation, conjugate option for complex
   - Reorder: General N-dimensional permutation, no conjugate
   - Use transpose for 2D matrices, reorder for general tensors
   
   **vs. Moddims (af-moddims)**:
   - Moddims: Changes dimension sizes, preserves total elements
   - Reorder: Permutes dimensions, preserves dimension sizes
   - Use moddims for reshaping, reorder for permutation
   
   **vs. Flip (af-flip)**:
   - Flip: Reverses elements along one dimension
   - Reorder: Changes dimension order, not element order within dimension
   
   **vs. Shift (af-shift)**:
   - Shift: Circular shift of data along dimensions
   - Reorder: Permutation of dimension order
   
   **vs. Tile (af-tile)**:
   - Tile: Replicates array along dimensions
   - Reorder: Rearranges existing dimensions
   
   ## Implementation Notes
   
   ArrayFire's reorder implementation:
   - **CPU Backend**: Sequential nested loops with index computation
   - **CUDA Backend**: Parallel kernel with thread blocks (32×8 threads)
   - **OpenCL Backend**: Work-group based parallel kernel
   - **OneAPI Backend**: SYCL parallel_for with nd_range
   
   Optimization strategies:
   - **JIT Fusion**: Reorder may fuse with subsequent operations
   - **Lazy Evaluation**: Actual reordering deferred until needed
   - **Smart Metadata**: When possible, only metadata updated
   - **Transpose Fast Path**: Special case for 2D transpose
   
   ## Type Support
   
   **All Types Supported**:
   - Floating point: f32, f64, f16 (half)
   - Complex: c32, c64
   - Integer: s32, u32, s64, u64, s16, u16, s8, u8
   - Boolean: b8
   
   No type restrictions - reorder works uniformly across all types.
   
   ## Error Conditions
   
   - **AF_ERR_SIZE**: Invalid dimension permutation (duplicate or out of range)
   - **AF_ERR_ARG**: NULL pointer arguments
   - **AF_ERR_NO_MEM**: Insufficient memory for output array
   - **AF_ERR_DEVICE**: GPU memory allocation failure
   
   ## See Also
   
   Related array manipulation functions:
   - af-transpose: 2D matrix transpose with conjugate option
   - af-moddims: Reshape array dimensions
   - af-flat: Flatten array to 1D
   - af-tile: Replicate array along dimensions
   - af-shift: Circular shift along dimensions
   - af-flip: Reverse elements along dimension
   - af-join: Concatenate arrays along dimension
   
   Related to:
   - Memory layout and stride manipulation
   - Data format conversion for interoperability
   - Tensor operation preparation"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; Dimension reordering

;; af_err af_reorder(af_array *out, const af_array in, const unsigned x, const unsigned y, const unsigned z, const unsigned w)
(defcfn af-reorder
  "Reorder dimensions of an array.
   
   Changes the order of array dimensions according to the specified permutation.
   This operation rearranges how dimensions are indexed while preserving the
   linear ordering of data in memory when possible.
   
   Parameters:
   - out: out pointer for reordered array
   - in: input array
   - x: specifies which dimension should be first (0-3)
   - y: specifies which dimension should be second (0-3)
   - z: specifies which dimension should be third (0-3)
   - w: specifies which dimension should be fourth (0-3)
   
   Dimension Mapping:
   - x, y, z, w must form a valid permutation of {0, 1, 2, 3}
   - Each value must be unique and in range [0, 3]
   - Output dimensions: [input_dim[x], input_dim[y], input_dim[z], input_dim[w]]
   
   Common Operations:
   
   **Identity (no change)**:
   ```clojure
   (af-reorder out arr 0 1 2 3)
   ;; Result: Reference to original array (no data copy)
   ```
   
   **Transpose (2D)**:
   ```clojure
   (af-reorder out arr 1 0 2 3)
   ;; Input:  [rows, cols, 1, 1]
   ;; Output: [cols, rows, 1, 1]
   ```
   
   **Batch dimension to front**:
   ```clojure
   (af-reorder out arr 3 0 1 2)
   ;; Input:  [features, height, width, batch]
   ;; Output: [batch, features, height, width]
   ```
   
   **Rotate 3D axes**:
   ```clojure
   (af-reorder out arr 2 0 1 3)
   ;; Input:  [X, Y, Z, batch]
   ;; Output: [Z, X, Y, batch]
   ```
   
   Performance Characteristics:
   
   **O(1) - Metadata-only** (no data copy):
   - Identity: (x=0, y=1, z=2, w=3)
   - First dimension unchanged: (x=0, ...)
   - Implementation: Modifies dimension sizes and strides only
   
   **O(N) - Data reordering** (full copy):
   - General case: When first dimension changes (x≠0)
   - Implementation: GPU kernel rearranges data in parallel
   - Memory: Allocates new array with reordered data
   
   **Special case - Transpose**:
   - Pattern: (x=1, y=0, z, w)
   - Implementation: Uses optimized transpose kernel
   - Performance: Better memory coalescing than general reorder
   
   Validation Rules:
   - All values {x, y, z, w} must be in [0, 3]
   - No duplicates allowed (each dimension appears exactly once)
   - Must form valid permutation of {0, 1, 2, 3}
   
   Examples:
   ```clojure
   ;; Example 1: Transpose 2D matrix
   (let [matrix (create-array data [5 4])  ; 5 rows, 4 columns
         out-ptr (mem/alloc-pointer ::mem/pointer)
         err (af-reorder out-ptr matrix 1 0 2 3)]
     (when (zero? err)
       (mem/read-pointer out-ptr ::mem/pointer)))
   ;; Result: [4, 5] (4 rows, 5 columns)
   
   ;; Example 2: Channel-first to channel-last (CNN)
   (let [chw-tensor (create-array data [3 224 224 32])  ; [C,H,W,N]
         out-ptr (mem/alloc-pointer ::mem/pointer)
         err (af-reorder out-ptr chw-tensor 1 2 0 3)]
     (when (zero? err)
       (mem/read-pointer out-ptr ::mem/pointer)))
   ;; Result: [224, 224, 3, 32] (HWC layout)
   
   ;; Example 3: Move batch dimension to front
   (let [features (create-array data [128 64 32 16])  ; [features, h, w, batch]
         out-ptr (mem/alloc-pointer ::mem/pointer)
         err (af-reorder out-ptr features 3 0 1 2)]
     (when (zero? err)
       (mem/read-pointer out-ptr ::mem/pointer)))
   ;; Result: [16, 128, 64, 32] (batch first)
   ```
   
   Valid Permutations:
   ```clojure
   (af-reorder out arr 0 1 2 3)  ; Identity ✓
   (af-reorder out arr 1 0 2 3)  ; Transpose ✓
   (af-reorder out arr 2 1 0 3)  ; Rotate first 3 dims ✓
   (af-reorder out arr 3 2 1 0)  ; Reverse all dims ✓
   ```
   
   Invalid Permutations (will return AF_ERR_SIZE):
   ```clojure
   (af-reorder out arr 0 0 2 3)  ; Duplicate dimension ✗
   (af-reorder out arr 0 1 2 4)  ; Out of range ✗
   (af-reorder out arr 1 2 3 3)  ; Missing dimension 0 ✗
   ```
   
   Type Support:
   - All types: f32, f64, f16, c32, c64, s32, u32, s64, u64, s16, u16, s8, u8, b8
   - No type restrictions or conversions
   
   Applications:
   - Matrix transposition for linear algebra
   - Data layout conversion (batch-first ↔ batch-last)
   - Image format conversion (HWC ↔ CHW)
   - Tensor preparation for neural networks
   - Multi-dimensional FFT setup
   - Scientific data cube reorganization
   
   Memory Management:
   - Metadata-only: No new memory allocated (array reference)
   - Data reorder: New array allocated with reordered data
   - Always release output array when done
   
   Common Use Cases:
   1. **2D Matrix Transpose**: (x=1, y=0, z=2, w=3)
   2. **3D Volume Axis Swap**: (x=2, y=0, z=1, w=3)
   3. **Batch Dimension Movement**: Move batch to/from any position
   4. **Channel Order Change**: CNN layout conversion
   5. **Data Format Interop**: Prepare for external libraries
   
   Notes:
   - For 2D transpose, consider af-transpose (supports conjugate)
   - For reshaping (changing sizes), use af-moddims
   - For flattening, use af-flat
   - Reorder only permutes dimensions, doesn't change total elements
   - Empty arrays return reference to input (no-op)
   
   Error Conditions:
   - AF_ERR_SIZE: Invalid permutation (duplicate or out of range)
   - AF_ERR_ARG: NULL pointer arguments
   - AF_ERR_NO_MEM: Insufficient memory for output
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-transpose: 2D transpose with conjugate option
   - af-moddims: Reshape array dimensions
   - af-flat: Flatten to 1D
   - af-tile: Replicate along dimensions
   - af-shift: Circular shift
   - af-flip: Reverse elements along dimension"
  "af_reorder" [::mem/pointer ::mem/pointer ::mem/int ::mem/int ::mem/int ::mem/int] ::mem/int)
