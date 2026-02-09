(ns org.soulspace.arrayfire.ffi.c-api.tile
  "ArrayFire FFI bindings for array tiling operations.

  Tiling is a fundamental array manipulation operation that replicates an array
  along one or more dimensions, creating a larger array by repeating the pattern
  of the input array.

  ## Concept

  Tiling takes an input array and creates copies of it along specified dimensions.
  Think of it as laying tiles on a floor - you repeat the same pattern (array)
  multiple times to fill a larger space.

  ### Visual Example (2D)

  Original 2×2 array:
  ```
  [1 2]
  [3 4]
  ```

  Tiled 2×3 (2 copies along dim0, 3 copies along dim1):
  ```
  [1 2 | 1 2 | 1 2]
  [3 4 | 3 4 | 3 4]
  -------+-----+-----
  [1 2 | 1 2 | 1 2]
  [3 4 | 3 4 | 3 4]
  ```

  Result: 4×6 array

  ## Mathematical Notation

  For input array A with dimensions [m, n, p, q] and tiling factors [x, y, z, w]:
  ```
  T = tile(A, x, y, z, w)
  T[i, j, k, l] = A[i mod m, j mod n, k mod p, l mod q]
  ```

  Output dimensions: [m×x, n×y, p×z, q×w]

  ## Use Cases

  ### Broadcasting & Element-wise Operations
  - **Repeat vector for matrix operations**: Convert 1D vector to matrix rows/columns
  - **Match dimensions**: Prepare arrays for element-wise operations
  - **Broadcasting emulation**: Manually broadcast for operations that don't auto-broadcast

  ### Signal & Image Processing
  - **Texture tiling**: Create seamless repeating patterns
  - **Signal replication**: Extend signals for convolution boundaries
  - **Image mosaics**: Create larger images from smaller tiles
  - **Pattern generation**: Generate periodic patterns

  ### Machine Learning & Data Science
  - **Batch replication**: Repeat samples for batch processing
  - **Data augmentation**: Create training variations by tiling
  - **Feature expansion**: Replicate features across dimensions
  - **Synthetic data**: Generate larger datasets from small samples

  ### Scientific Computing
  - **Boundary conditions**: Extend computational domains
  - **Grid generation**: Create meshgrids for numerical methods
  - **Parameter sweeps**: Replicate base arrays for parameter variations
  - **Periodic boundary conditions**: Simulate infinite domains

  ### Visualization
  - **Grid patterns**: Create grids for plotting
  - **Color palettes**: Expand color samples to full images
  - **Texture mapping**: Tile textures onto surfaces

  ## Performance Characteristics

  ### Computational Complexity
  - **Time**: O(x × y × z × w × elements) - proportional to output size
  - **Memory**: Output array size = input size × x × y × z × w
  - **GPU**: Highly parallel, efficient memory coalescing

  ### Optimization
  ArrayFire's tiling is optimized:
  - Memory coalescing for GPU
  - Minimal data copying when possible
  - Efficient for large tiling factors

  ### Performance Tips
  1. **Memory**: Be aware tiling can create very large arrays
     - 100×100 tiled 100×100 = 10,000×10,000 = 400MB (f32)
  2. **Alternative**: Use broadcasting operations when possible (automatic, no memory overhead)
  3. **Batching**: For operations on tiled data, consider batch operations instead

  ## Tiling vs Broadcasting

  ### Broadcasting (Automatic)
  - No memory overhead
  - Implicit during operations
  - Limited to compatible shapes
  - Example: `a + b` auto-broadcasts if shapes compatible

  ### Tiling (Explicit)
  - Creates physical copies in memory
  - Explicit control over dimensions
  - Works for any tiling pattern
  - Necessary when:
    * Broadcasting not supported for operation
    * Need explicit tiled array
    * Preparing data for algorithms requiring specific shapes

  ## Dimension Semantics

  ### 4D Tiling
  ArrayFire arrays are up to 4-dimensional [dim0, dim1, dim2, dim3]:
  - **dim0**: Typically rows (or x-coordinate)
  - **dim1**: Typically columns (or y-coordinate)  
  - **dim2**: Typically channels/depth (or z-coordinate)
  - **dim3**: Typically batch dimension (or w-coordinate)

  ### Tile Count Includes Original
  **IMPORTANT**: Tiling factor = number of copies (including original)
  - `tile(A, 1, 1, 1, 1)` → no change (1 copy = original)
  - `tile(A, 2, 1, 1, 1)` → 2 copies along dim0 (original + 1 duplicate)
  - `tile(A, 3, 2, 1, 1)` → 3 copies along dim0, 2 copies along dim1

  ## Common Patterns

  ### Pattern 1: Vector to Matrix (Row Replication)
  ```clojure
  ;; Replicate row vector to matrix
  (let [row-vec (af/af-range [5] 0)  ;; [0 1 2 3 4]
        matrix (af/af-tile row-vec 3 1 1 1)]  ;; 3 rows
    ;; Result: [[0 1 2 3 4]
    ;;          [0 1 2 3 4]
    ;;          [0 1 2 3 4]]
    matrix)
  ```

  ### Pattern 2: Vector to Matrix (Column Replication)
  ```clojure
  ;; Replicate column vector to matrix
  (let [col-vec (af/af-range [1 5] 0)  ;; [[0] [1] [2] [3] [4]]
        matrix (af/af-tile col-vec 1 3 1 1)]  ;; 3 columns
    ;; Result: [[0 0 0]
    ;;          [1 1 1]
    ;;          [2 2 2]
    ;;          [3 3 3]
    ;;          [4 4 4]]
    matrix)
  ```

  ### Pattern 3: Meshgrid Creation
  ```clojure
  ;; Create coordinate grids for 2D functions
  (defn meshgrid [x-vec y-vec]
    (let [nx (af/af-get-elements x-vec)
          ny (af/af-get-elements y-vec)
          ;; Tile x along columns
          X (af/af-tile x-vec 1 ny 1 1)
          ;; Tile y along rows  
          Y (af/af-tile y-vec nx 1 1 1)]
      [X Y]))

  ;; Use for function evaluation
  (let [[X Y] (meshgrid (af/af-range [100] 0) (af/af-range [100] 0))
        Z (af/af-add (af/af-pow X 2) (af/af-pow Y 2))]
    ;; Z now contains x² + y² at each grid point
    Z)
  ```

  ### Pattern 4: Batch Replication
  ```clojure
  ;; Replicate single sample to batch
  (let [sample (af/af-randn [28 28 1] :f32)  ;; Single 28×28 image
        batch (af/af-tile sample 1 1 1 32)]   ;; Replicate to batch of 32
    ;; Result: [28 28 1 32] - 32 copies in batch dimension
    batch)
  ```

  ### Pattern 5: Texture/Pattern Tiling
  ```clojure
  ;; Create large texture from small pattern
  (let [pattern (load-pattern \"tile.png\")  ;; e.g., 64×64 texture
        large-texture (af/af-tile pattern 10 10 1 1)]  ;; 640×640 tiled texture
    (save-image large-texture \"tiled.png\"))
  ```

  ### Pattern 6: Feature Expansion
  ```clojure
  ;; Replicate features for each sample in batch
  (let [features (af/af-randn [100] :f32)     ;; 100 features
        n-samples 1000
        ;; Tile to match batch dimension
        expanded (af/af-tile features n-samples 1 1 1)]  ;; [100 n-samples]
    expanded)
  ```

  ## Edge Cases

  ### Empty Arrays
  - Tiling empty array returns empty array
  - No error, gracefully handled

  ### Single Element Arrays
  - Can tile scalar-like arrays to any size
  - Efficient way to create constant arrays

  ### Identity Tiling
  - `tile(A, 1, 1, 1, 1)` returns copy of A
  - Useful for ensuring array ownership

  ### Large Tiling Factors
  - Can create very large arrays quickly
  - Watch for memory limits
  - Example: 1000×1000 array tiled 1000×1000 = 1TB (f32)!

  ## Type Support

  **All ArrayFire types supported**:
  - **Floating-point**: f32, f64, f16
  - **Complex**: c32, c64
  - **Integer**: s8, s16, s32, s64, u8, u16, u32, u64
  - **Boolean**: b8

  Output type = input type (no conversion)

  ## Memory Considerations

  ### Memory Usage
  Output memory = input memory × x × y × z × w

  Examples:
  - 10×10 f32 array (400 bytes) tiled 10×10 → 100×100 (40KB)
  - 100×100 f32 array (40KB) tiled 100×100 → 10,000×10,000 (400MB)
  - 1000×1000 f32 array (4MB) tiled 100×100 → 100,000×100,000 (40GB)

  **Rule of thumb**: Check output size before tiling large arrays!

  ### Memory Efficiency
  - Tiling creates physical copies (not views)
  - Consider if broadcasting can achieve same result
  - For read-only operations, broadcasting saves memory

  ## Alternatives to Tiling

  ### When to Use Tiling
  - Need explicit tiled array (not just virtual broadcasting)
  - Subsequent operations don't support broadcasting
  - Performance testing shows tiling is faster

  ### When to Use Broadcasting
  - ArrayFire operation supports it (most do)
  - Want to save memory
  - Element-wise operations on different-shaped arrays

  ### When to Use Batch Operations
  - Processing multiple arrays with same operation
  - Can stack arrays along batch dimension
  - Leverage batch-optimized kernels

  ## Comparison with Similar Operations

  | Operation | Purpose | Memory | Use Case |
  |-----------|---------|--------|----------|
  | **tile** | Replicate array | Creates copies | Explicit replication needed |
  | Broadcasting | Implicit tiling | No overhead | Element-wise ops on compatible shapes |
  | moddims | Reshape | None (view) | Change interpretation of dimensions |
  | join | Concatenate | New array | Combine different arrays |
  | repeat | Similar to tile | Creates copies | NumPy compatibility |

  ## Error Conditions

  - **AF_ERR_ARG**: Invalid arguments (null pointers)
  - **AF_ERR_SIZE**: Tiling would exceed size limits
  - **AF_ERR_MEM**: Insufficient memory for output array

  ## Implementation Notes

  - Uses optimized CUDA/OpenCL kernels for GPU
  - Memory coalescing for efficient access
  - Can be combined with JIT for optimization
  - Part of ArrayFire's data manipulation suite

  ## Best Practices

  1. **Check output size**: `input_elements × x × y × z × w`
  2. **Use broadcasting when possible**: Saves memory, often faster
  3. **Consider batch dimension**: Use dim3 for batch processing
  4. **Profile memory**: Monitor GPU memory usage for large tiles
  5. **Alternative approaches**: Sometimes reshape/moddims can achieve similar results

  ## Related Operations

  - **moddims**: Reshape array (no data copy)
  - **reorder**: Permute dimensions
  - **join**: Concatenate arrays
  - **flat**: Flatten to 1D
  - **repeat**: NumPy-style repetition (if available)

  See also:
  - ArrayFire documentation: https://arrayfire.org/docs/group__manip__func__tile.htm
  - Broadcasting guide: https://arrayfire.org/docs/broadcasting.htm"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;;
;; Array Tiling Functions
;;

(defcfn af-tile
  "Tile (replicate) an array along specified dimensions.

  Creates a larger array by replicating the input array a specified number of
  times along each dimension. The tiling factors include the original array
  in the count.

  Parameters:
  - out: Output pointer for tiled array
  - in: Input array to tile
  - x: Number of tiles along dimension 0 (rows)
  - y: Number of tiles along dimension 1 (columns)
  - z: Number of tiles along dimension 2 (depth/channels)
  - w: Number of tiles along dimension 3 (batch)

  Returns:
  Error code indicating success or failure.

  ## Dimension Transformation

  Input shape: [m, n, p, q]
  Tiling: x, y, z, w
  Output shape: [m×x, n×y, p×z, q×w]

  ## Important: Tile Count Semantics

  **The tile count INCLUDES the original array**:
  - x=1 means \"1 copy\" (the original, no duplication)
  - x=2 means \"2 copies\" (original + 1 duplicate)
  - x=3 means \"3 copies\" (original + 2 duplicates)

  ## Basic Example: Vector Tiling

  ```clojure
  (require '[org.soulspace.arrayfire.ffi.core :as af])
  (require '[org.soulspace.arrayfire.ffi.tile :as tile])
  (require '[coffi.mem :as mem])

  ;; Create 1×5 vector
  (let [vec-data [1.0 2.0 3.0 4.0 5.0]
        input (af/af-create-array vec-data [1 5] :f32)
        out-ptr (mem/alloc-instance ::mem/pointer)]
    
    ;; Tile 3 times along dimension 0 (create 3 rows)
    (tile/af-tile out-ptr input 3 1 1 1)
    
    (let [result (mem/read-pointer out-ptr ::mem/pointer)]
      ;; Result is 3×5 matrix:
      ;; [[1 2 3 4 5]
      ;;  [1 2 3 4 5]
      ;;  [1 2 3 4 5]]
      (println \"Tiled to matrix:\")
      (af/af-print-array result)
      
      ;; Clean up
      (af/af-release-array input)
      (af/af-release-array result)))
  ```

  ## Example: 2D Tiling

  ```clojure
  ;; Create 2×2 matrix
  (let [data [1.0 2.0
              3.0 4.0]
        input (af/af-create-array data [2 2] :f32)
        out-ptr (mem/alloc-instance ::mem/pointer)]
    
    ;; Tile 2×3 (2 times vertically, 3 times horizontally)
    (tile/af-tile out-ptr input 2 3 1 1)
    
    (let [result (mem/read-pointer out-ptr ::mem/pointer)]
      ;; Result is 4×6 matrix:
      ;; [[1 2 | 1 2 | 1 2]
      ;;  [3 4 | 3 4 | 3 4]
      ;;  -----+-----+-----
      ;;  [1 2 | 1 2 | 1 2]
      ;;  [3 4 | 3 4 | 3 4]]
      (println \"Tiled 2×3:\")
      (af/af-print-array result)
      
      (af/af-release-array input)
      (af/af-release-array result)))
  ```

  ## Example: Meshgrid for Function Evaluation

  ```clojure
  ;; Create coordinate grids for evaluating z = f(x, y)
  (defn create-meshgrid [nx ny]
    (let [;; Create x coordinates: [0, 1, 2, ..., nx-1]
          x-vec (af/af-range [nx] 0 :f32)
          ;; Create y coordinates: [0, 1, 2, ..., ny-1]
          y-vec (af/af-range [ny] 0 :f32)
          
          ;; Tile x along columns (repeat each row)
          x-out (mem/alloc-instance ::mem/pointer)
          _ (tile/af-tile x-out x-vec 1 ny 1 1)
          X (mem/read-pointer x-out ::mem/pointer)
          
          ;; Tile y along rows (repeat each column)
          ;; First reshape y to column vector
          y-col (af/af-moddims y-vec [1 ny])
          y-out (mem/alloc-instance ::mem/pointer)
          _ (tile/af-tile y-out y-col nx 1 1 1)
          Y (mem/read-pointer y-out ::mem/pointer)]
      
      [X Y]))

  ;; Use meshgrid to evaluate function
  (let [[X Y] (create-meshgrid 100 100)
        ;; Compute z = sin(x/10) * cos(y/10)
        X-scaled (af/af-div X 10.0)
        Y-scaled (af/af-div Y 10.0)
        Z (af/af-mul (af/af-sin X-scaled) (af/af-cos Y-scaled))]
    
    (println \"Evaluated function on 100×100 grid\")
    (af/af-print-array Z)
    
    ;; Clean up
    (af/af-release-array X)
    (af/af-release-array Y)
    (af/af-release-array Z))
  ```

  ## Example: Batch Replication

  ```clojure
  ;; Replicate single sample to create batch
  (let [;; Single 28×28 image
        sample (af/af-randn [28 28] :f32)
        out-ptr (mem/alloc-instance ::mem/pointer)
        batch-size 64]
    
    ;; Tile along dimension 3 (batch dimension)
    (tile/af-tile out-ptr sample 1 1 1 batch-size)
    
    (let [batch (mem/read-pointer out-ptr ::mem/pointer)]
      ;; Result: [28 28 1 64] - batch of 64 identical images
      (println \"Created batch of\" batch-size \"samples\")
      (let [[d0 d1 d2 d3] (af/af-get-dims batch)]
        (println \"Batch dimensions:\" d0 d1 d2 d3))
      
      (af/af-release-array sample)
      (af/af-release-array batch)))
  ```

  ## Example: Texture/Pattern Tiling

  ```clojure
  ;; Create large tiled texture from small pattern
  (defn tile-texture [pattern tiles-x tiles-y]
    (let [out-ptr (mem/alloc-instance ::mem/pointer)]
      (tile/af-tile out-ptr pattern tiles-x tiles-y 1 1)
      (mem/read-pointer out-ptr ::mem/pointer)))

  ;; Load small pattern and tile it
  (let [pattern (load-pattern \"brick.png\")  ;; 64×64 texture
        tiled (tile-texture pattern 10 10)]    ;; Create 640×640 texture
    (save-image tiled \"wall.png\")
    (af/af-release-array pattern)
    (af/af-release-array tiled))
  ```

  ## Example: Broadcasting Alternative

  ```clojure
  ;; Compare tiling vs broadcasting for element-wise operation
  
  ;; Approach 1: Explicit tiling
  (let [vec (af/af-range [5] 0)        ;; [0 1 2 3 4]
        mat (af/af-randn [3 5] :f32)   ;; 3×5 random matrix
        
        ;; Tile vector to match matrix
        vec-tiled-ptr (mem/alloc-instance ::mem/pointer)
        _ (tile/af-tile vec-tiled-ptr vec 3 1 1 1)
        vec-tiled (mem/read-pointer vec-tiled-ptr ::mem/pointer)
        
        ;; Add
        result1 (af/af-add mat vec-tiled)]
    
    (af/af-release-array vec-tiled)
    (af/af-release-array result1))
  
  ;; Approach 2: Broadcasting (preferred - no memory overhead)
  (let [vec (af/af-range [5] 0)
        mat (af/af-randn [3 5] :f32)
        ;; Broadcasting happens automatically!
        result2 (af/af-add mat vec)]
    
    ;; result2 same as result1, but no tiling memory used
    (af/af-release-array result2))
  ```

  ## Example: Identity Tiling (Copy Array)

  ```clojure
  ;; Tile with factors of 1 creates a copy
  (let [original (af/af-randn [100 100] :f32)
        copy-ptr (mem/alloc-instance ::mem/pointer)]
    
    ;; No actual tiling, just copy
    (tile/af-tile copy-ptr original 1 1 1 1)
    
    (let [copy (mem/read-pointer copy-ptr ::mem/pointer)]
      ;; copy is independent of original
      (af/af-release-array original)
      ;; copy still valid
      (af/af-release-array copy)))
  ```

  ## Tiling Dimensions Guide

  ### Dimension 0 (Rows/X)
  - Tiles vertically (stacks rows)
  - `tile(A, 3, 1, 1, 1)` → 3× taller

  ### Dimension 1 (Columns/Y)
  - Tiles horizontally (extends columns)
  - `tile(A, 1, 3, 1, 1)` → 3× wider

  ### Dimension 2 (Depth/Channels/Z)
  - Tiles along third dimension
  - `tile(A, 1, 1, 3, 1)` → 3× deeper
  - Useful for color channels, feature maps

  ### Dimension 3 (Batch/W)
  - Tiles along batch dimension
  - `tile(A, 1, 1, 1, 3)` → 3× in batch
  - Useful for creating batches from single samples

  ## Performance Tips

  1. **Check output size first**:
     ```clojure
     (let [[m n p q] (af/af-get-dims input)
           output-size (* m x n y p z q w)]
       (when (> output-size (* 1e9))  ;; > 1 billion elements
         (println \"WARNING: Large tiled array!\")))
     ```

  2. **Use broadcasting when possible**:
     - Many operations automatically broadcast
     - No memory overhead
     - Often faster than explicit tiling

  3. **Consider batch operations**:
     - Instead of tiling for parallel operations
     - Use batch dimension (dim3) natively

  4. **Memory-efficient alternatives**:
     - For read-only: Use views/broadcasting
     - For write: Consider in-place operations

  ## Type Support

  All ArrayFire types supported:
  - f32, f64, f16
  - c32, c64
  - s8, s16, s32, s64
  - u8, u16, u32, u64
  - b8

  Output type matches input type.

  ## Output Size Calculation

  ```
  Input:  [m, n, p, q]
  Tile:   [x, y, z, w]
  Output: [m×x, n×y, p×z, q×w]
  Elements: m×n×p×q × x×y×z×w
  ```

  ## Memory Usage

  ```
  Memory = input_elements × x × y × z × w × sizeof(type)
  ```

  Example:
  ```clojure
  ;; 100×100 f32 array (40 KB)
  ;; Tiled 10×10
  ;; Output: 1000×1000 f32 (4 MB)
  ;; Memory multiplier: 10×10 = 100×
  ```

  ## Error Conditions

  - **AF_ERR_ARG**: Null pointers, invalid tile factors
  - **AF_ERR_SIZE**: Output would be too large
  - **AF_ERR_MEM**: Insufficient memory for output
  - **AF_ERR_TYPE**: Input type not supported (shouldn't happen)

  ## Edge Cases

  - **Empty input**: Returns empty array
  - **Zero tile factor**: Not allowed (use 1 minimum)
  - **Single element**: Tiles efficiently to any size
  - **Very large factors**: May exhaust memory

  ## Implementation Details

  - GPU: Optimized kernels with memory coalescing
  - CPU: Efficient memory copy operations
  - JIT: Can be optimized with other operations
  - Memory: Physical copies created (not views)

  ## Comparison with NumPy

  ```python
  # NumPy
  tiled = np.tile(arr, (2, 3))  # Tile 2×3

  # ArrayFire (equivalent)
  (af-tile out arr 2 3 1 1)
  ```

  ## When NOT to Use Tile

  - Operation supports broadcasting → Use broadcasting
  - Just need different shape → Use moddims/reshape
  - Combining different arrays → Use join
  - Temporary for single operation → Let broadcasting handle it

  ## Best Practices

  1. **Verify dimensions**: Check input/output shapes
  2. **Memory check**: Calculate output size before tiling
  3. **Consider alternatives**: Broadcasting, moddims, join
  4. **Use batch dimension**: For batch processing (dim3)
  5. **Profile**: Test if tiling helps or hurts performance

  See also:
  - af_tile (ArrayFire C API)
  - moddims: Reshape without copying
  - join: Concatenate arrays
  - reorder: Permute dimensions
  - Broadcasting operations: Automatic tiling for element-wise ops"
  "af_tile" [::mem/pointer ::mem/pointer ::mem/int ::mem/int ::mem/int ::mem/int] ::mem/int)
