(ns org.soulspace.arrayfire.ffi.moddims
  "Bindings for the ArrayFire dimension modification functions (src/api/c/moddims.cpp).
   
   Dimension modification functions allow reshaping arrays without copying data.
   These operations modify only the metadata (shape description) of arrays,
   making them extremely fast and memory-efficient.
   
   # What is moddims?
   
   **moddims** (modify dimensions) changes the shape of an array without moving
   or copying any data. It's a zero-cost abstraction that only updates the
   array's dimension metadata.
   
   Think of it like viewing the same data through a different \"lens\" - the
   underlying memory layout remains unchanged, but you interpret it with
   different dimensions.
   
   # Mathematical Foundation
   
   An array in memory is stored as a contiguous sequence of elements:
   ```
   Memory: [a₀, a₁, a₂, a₃, a₄, a₅, a₆, a₇, a₈, a₉, a₁₀, a₁₁]
   ```
   
   This can be interpreted with different shapes:
   - 1D: [12] → all elements in one dimension
   - 2D: [3, 4] → 3 rows, 4 columns
   - 2D: [4, 3] → 4 rows, 3 columns  
   - 2D: [2, 6] → 2 rows, 6 columns
   - 3D: [2, 2, 3] → 2×2×3 cube
   - 4D: [2, 3, 2, 1] → 2×3×2×1 hypercube
   
   **Constraint**: Total number of elements must remain constant.
   ```
   N₀ × N₁ × N₂ × N₃ = constant
   ```
   
   # Column-Major Order (ArrayFire Convention)
   
   ArrayFire uses **column-major** (Fortran-style) memory layout:
   - First dimension varies fastest
   - Elements stored column-by-column
   
   Example: 2×3 matrix [2 rows, 3 columns]
   ```
   Logical view:       Memory layout:
   [a b c]            [a, d, b, e, c, f]
   [d e f]             ↑  ↑  ↑  ↑  ↑  ↑
                      col0  col1  col2
   ```
   
   This affects how moddims interprets reshaping.
   
   # Visual Examples
   
   ## Example 1: 1D → 2D (Column-major)
   ```
   Original [8]: [0, 1, 2, 3, 4, 5, 6, 7]
   
   Reshape to [2, 4] (2 rows, 4 columns):
   [0 2 4 6]   ← row 0
   [1 3 5 7]   ← row 1
   ↑ ↑ ↑ ↑
   col0..3
   
   Note: First dimension (rows) varies fastest!
   ```
   
   ## Example 2: 2D → 2D (Transpose-like)
   ```
   Original [2, 4]:      Reshape to [4, 2]:
   [0 2 4 6]             [0 4]
   [1 3 5 7]             [1 5]
                         [2 6]
                         [3 7]
   
   Memory: [0,1,2,3,4,5,6,7] (unchanged)
   Different interpretation of same data!
   ```
   
   ## Example 3: 2D → 3D (Add dimension)
   ```
   Original [4, 3] (12 elements):
   [0  4  8]
   [1  5  9]
   [2  6  10]
   [3  7  11]
   
   Reshape to [2, 2, 3] (2×2×3 cube):
   Slice 0:    Slice 1:    Slice 2:
   [0 2]       [4 6]       [8  10]
   [1 3]       [5 7]       [9  11]
   
   Same 12 elements, interpreted as 3D structure!
   ```
   
   ## Example 4: Flatten (Any → 1D)
   ```
   Original [2, 3, 2] (12 elements):
   3D structure...
   
   Flatten to [12]:
   [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]
   
   Useful for: vectorization, linear algebra, serialization
   ```
   
   # Performance Characteristics
   
   **Time Complexity**: O(1)
   - No data movement or copying
   - Only metadata update (dimension description)
   - Instant regardless of array size
   
   **Space Complexity**: O(1)
   - Zero additional memory allocation
   - Original data stays in place
   - New array shares underlying buffer
   
   **Comparison with Copy**:
   ```
   Operation        | Time      | Space  | Data Movement
   -----------------|-----------|--------|---------------
   moddims          | O(1)      | O(1)   | None
   copy + reshape   | O(N)      | O(N)   | Full copy
   transpose        | O(N)      | O(N)   | Reordering
   ```
   
   Moddims is ~1,000-1,000,000× faster than copying for large arrays!
   
   # When moddims Works (Metadata-Only)
   
   Moddims is a pure metadata operation (zero-cost) when:
   
   1. **Array is linear (contiguous)**:
      - Original array has no special memory layout
      - Not the result of indexing, reordering, or non-contiguous operations
   
   2. **Array is a JIT node (unevaluated)**:
      - Array represents a pending computation
      - No physical memory allocated yet
      - moddims becomes part of the computation graph
   
   In these cases, moddims simply updates the dimension description.
   
   # When moddims Triggers Copy
   
   Moddims may trigger a data copy when:
   
   1. **Non-linear memory layout**:
      - Array is result of: transpose, reorder, flip, indexing
      - Memory strides don't match new dimensions
      - Example: After af_reorder, memory is non-contiguous
   
   2. **Complex striding patterns**:
      - Advanced indexing created non-standard layout
      - Slicing resulted in non-contiguous view
   
   ArrayFire automatically handles this, ensuring correctness.
   
   # Type Support
   
   **All ArrayFire types supported**:
   - Floating-point: f32, f64, f16
   - Complex: c32 (cfloat), c64 (cdouble)
   - Integer: s32, u32, s64, u64, s16, u16, s8, u8
   - Boolean: b8
   
   Type is preserved through moddims operation.
   
   # Common Use Cases
   
   ## 1. Flattening (Multi-dimensional → 1D)
   ```clojure
   ;; Flatten for vectorization
   (let [matrix (create-array data [100 50]) ; 2D: 100×50
         flat (moddims matrix [5000])]      ; 1D: 5000
     ;; Use for: dot products, norms, serialization
   )
   ```
   
   ## 2. Batch Processing (Add batch dimension)
   ```clojure
   ;; Single image [H, W, C] → Batch of 1 [H, W, C, 1]
   (let [image (create-array pixels [256 256 3]) ; HxWxC
         batch (moddims image [256 256 3 1])]   ; HxWxCx1
     ;; Compatible with batch processing functions
   )
   ```
   
   ## 3. Matrix to Vector (2D → 1D)
   ```clojure
   ;; Column vector [N, 1] → 1D array [N]
   (let [col-vec (create-array data [100 1])
         vec-1d (moddims col-vec [100])]
     ;; Simpler to work with as 1D
   )
   ```
   
   ## 4. Reshaping for Broadcasting
   ```clojure
   ;; Reshape for broadcasting compatibility
   (let [weights (create-array data [64])        ; 1D: 64 elements
         reshaped (moddims weights [1 64])]     ; 2D: 1×64 for broadcasting
     ;; Now can broadcast across batches
   )
   ```
   
   ## 5. Unrolling Convolutions
   ```clojure
   ;; 3D feature maps → 2D matrix for matrix multiply
   (let [features (create-array data [H W C])
         unrolled (moddims features [(* H W) C])]
     ;; Transform convolution to matrix multiply
   )
   ```
   
   ## 6. Tensor Operations
   ```clojure
   ;; Reshape for different tensor interpretations
   (let [tensor (create-array data [2 3 4 5])      ; 4D tensor
         matrix (moddims tensor [6 20])]           ; 2D matrix view
     ;; Apply matrix operations to tensor
   )
   ```
   
   ## 7. Video Frames (Temporal → Spatial)
   ```clojure
   ;; Time series of images [T, H, W] → Stack [H, W, T]
   (let [video (create-array frames [100 256 256])  ; Time, Height, Width
         stack (moddims video [256 256 100])]      ; Height, Width, Time
     ;; Different interpretation for processing
   )
   ```
   
   # Flat vs Moddims
   
   **af-flat** is a specialized version of moddims:
   - Always reshapes to 1D
   - Shortcut: `moddims(arr, [N])`
   - Common operation, deserves dedicated function
   
   ```clojure
   ;; These are equivalent:
   (flat array)                    ; Convenient
   (moddims array [(elements array)])  ; Explicit
   ```
   
   # Design Patterns
   
   ## Pattern 1: Temporary Reshape
   ```clojure
   (let [original (create-array data [10 20])
         flat (moddims original [200])        ; Flatten for operation
         result (some-1d-operation flat)
         reshaped (moddims result [10 20])]  ; Restore shape
     reshaped)
   ```
   
   ## Pattern 2: Shape-Agnostic Functions
   ```clojure
   (defn process-any-shape [arr]
     (let [original-shape (get-dims arr)
           flat (flat arr)                    ; Work in 1D
           processed (process-1d flat)]
       (moddims processed original-shape)))  ; Restore original shape
   ```
   
   ## Pattern 3: Broadcasting Adapter
   ```clojure
   (defn make-broadcastable [arr target-dims]
     ;; Add singleton dimensions for broadcasting
     (let [n (elements arr)]
       (moddims arr (concat [n] (repeat (- (count target-dims) 1) 1)))))
   ```
   
   ## Pattern 4: Batch Unbatch
   ```clojure
   (defn batch-process [items batch-size]
     (let [n (count items)
           batched (moddims items [batch-size (/ n batch-size)])
           results (process-batches batched)]
       (moddims results [n])))  ; Flatten results
   ```
   
   # Restrictions and Gotchas
   
   ## Element Count Must Match
   ```clojure
   ;; ✓ Valid: 24 = 24
   (moddims array [6 4])    ; 24 elements
   (moddims array [2 3 4])  ; 2×3×4 = 24
   
   ;; ✗ Invalid: 24 ≠ 30
   (moddims array [5 6])    ; 5×6 = 30 ≠ 24
   ;; Error: AF_ERR_SIZE
   ```
   
   ## Column-Major Interpretation
   ```clojure
   ;; Memory: [0,1,2,3,4,5]
   (moddims array [2 3])
   ;; Result (column-major):
   ;; [0 2 4]   NOT  [0 1 2]
   ;; [1 3 5]        [3 4 5]
   ```
   
   ## Not a Transpose
   ```clojure
   ;; moddims is NOT transpose!
   (let [a (create-array [[1 2] [3 4]] [2 2])]
     ;; a = [1 2]    Memory: [1,3,2,4] (column-major)
     ;;     [3 4]
     
     (moddims a [2 2])    ; Same shape, same data
     ;; Result: [1 2]  (unchanged)
     ;;         [3 4]
     
     (transpose a)        ; Reorders data
     ;; Result: [1 3]  (transposed)
     ;;         [2 4]
   )
   ```
   
   ## Zero-Size Dimensions
   ```clojure
   ;; Empty arrays (0 elements) are valid
   (moddims empty-array [0])        ; 1D empty
   (moddims empty-array [10 0])     ; 2D with 0 in one dimension
   (moddims empty-array [0 0 0])    ; 3D empty
   ```
   
   # Error Conditions
   
   1. **AF_ERR_SIZE**: Total elements don't match
      ```clojure
      (moddims array [new-dims])  ; new-dims product ≠ array elements
      ```
   
   2. **AF_ERR_ARG**: Invalid arguments
      ```clojure
      (moddims array nil)         ; null dims pointer
      (moddims array [])          ; ndims = 0 (treated as identity)
      ```
   
   3. **AF_ERR_ARR**: Invalid array handle
      ```clojure
      (moddims nil [10])          ; null array
      ```
   
   # Relationship to Other Operations
   
   **Moddims** vs **Transpose**:
   - moddims: Metadata-only, O(1), reinterprets same data
   - transpose: Data reordering, O(N), changes memory layout
   
   **Moddims** vs **Reorder**:
   - moddims: Changes dimensions, preserves memory order
   - reorder: Permutes dimensions, changes memory layout
   
   **Moddims** vs **Copy**:
   - moddims: Shares data, zero-copy (usually)
   - copy: Duplicates data, independent arrays
   
   **Moddims** vs **Reshape** (other libraries):
   - Same concept, different names
   - NumPy: `reshape()`, MATLAB: `reshape()`
   - ArrayFire: `moddims()` and `flat()`
   
   # Applications by Domain
   
   ## Deep Learning
   - Batch dimension manipulation
   - Flattening for fully-connected layers
   - Reshaping feature maps
   - Tensor to matrix conversion
   
   ## Signal Processing
   - Time series windowing
   - Spectrogram reshaping
   - Channel interleaving/deinterleaving
   
   ## Computer Vision
   - Image batch formation
   - Spatial to channel transformation
   - Video frame stacking
   
   ## Linear Algebra
   - Vector/matrix conversions
   - Tensor flattening
   - Block matrix operations
   
   ## Data Science
   - Feature engineering
   - Dataset reshaping
   - Vectorization for algorithms
   
   # Best Practices
   
   1. **Use flat() for 1D conversion**:
      ```clojure
      ;; Preferred
      (flat array)
      ;; vs
      (moddims array [(elements array)])
      ```
   
   2. **Verify element count**:
      ```clojure
      (assert (= (elements array)
                 (reduce * new-dims)))
      ```
   
   3. **Document reshape rationale**:
      ```clojure
      ;; Good: Explains why
      ;; Flatten to 1D for dot product computation
      (flat features)
      
      ;; Bad: No context
      (moddims features [1000])
      ```
   
   4. **Preserve original shape**:
      ```clojure
      (let [orig-shape (get-dims array)]
        ;; ... operations on reshaped array ...
        (moddims result orig-shape))  ; Restore
      ```
   
   5. **Use for zero-cost operations**:
      - When you need different view of same data
      - Avoid if you need actual data reordering
      - Combine with eval() if unsure about linearity
   
   6. **Understand column-major**:
      - First dimension varies fastest
      - Different from C/Python row-major
      - Test with small examples first
   
   # Performance Tips
   
   - moddims is O(1) - use freely
   - Prefer moddims over copy + reshape
   - Chain moddims operations (each is O(1))
   - Use with JIT compilation (becomes part of computation graph)
   - No performance penalty for multiple moddims
   
   # Limitations
   
   - Cannot change element count
   - Cannot reorder data (use reorder/transpose)
   - Column-major only (ArrayFire convention)
   - May trigger copy for non-linear arrays (rare, automatic)
   
   See also:
   - af-reorder: Permute dimensions with data reordering
   - af-transpose: 2D transpose operation  
   - af-flip: Flip array along dimension
   - af-tile: Replicate array along dimensions"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; Dimension modification functions

;; af_err af_moddims(af_array *out, const af_array in, const unsigned ndims, const dim_t *const dims)
(defcfn af-moddims
  "Modify dimensions of an array without copying data.
   
   Changes the shape of an array by reinterpreting the same underlying data
   with different dimensions. This is a metadata-only operation (no data
   movement) in most cases, making it extremely fast.
   
   Parameters:
   - out: out pointer for reshaped array
   - in: input array to reshape
   - ndims: number of dimensions in new shape
   - dims: pointer to array of new dimension sizes
   
   Constraint:
   Total number of elements must remain constant:
   ```
   product(old_dims) = product(new_dims)
   ```
   
   Algorithm:
   1. Verify element count matches (old vs new)
   2. Check if array is linear (contiguous memory)
   3. If linear: Update metadata only (O(1))
   4. If non-linear: Copy data then reshape (O(N))
   5. Return new array handle with updated dimensions
   
   Memory Layout:
   ArrayFire uses **column-major** order (Fortran-style):
   - First dimension varies fastest in memory
   - Elements stored column-by-column
   - Different from C/NumPy row-major!
   
   Performance:
   - Typical case: O(1) - metadata update only
   - Non-linear case: O(N) - copy triggered
   - Zero memory overhead (shares buffer)
   - Instant for most use cases
   
   Type Support:
   All types: f32, f64, f16, c32, c64, s32, u32, s64, u64, s16, u16, s8, u8, b8
   
   Example 1: 1D to 2D
   ```clojure
   ;; Flatten 1D array [8] to 2D matrix [2, 4]
   (let [data (create-array [0 1 2 3 4 5 6 7] [8])
         dims-ptr (mem/alloc-pointer ::mem/long 2)
         _ (mem/write-pointer dims-ptr [2 4] ::mem/long)
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-moddims out-ptr data 2 dims-ptr)]
     ;; Result [2, 4]:
     ;; [0 2 4 6]  ← row 0
     ;; [1 3 5 7]  ← row 1
     ;; Note: Column-major! First dimension varies fastest
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example 2: 2D to 1D (Flatten)
   ```clojure
   ;; Flatten 2D matrix to 1D vector
   (let [matrix (create-array data [10 20])  ; 10×20 matrix
         dims-ptr (mem/alloc-pointer ::mem/long 1)
         _ (mem/write-pointer dims-ptr [200] ::mem/long)
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-moddims out-ptr matrix 1 dims-ptr)]
     ;; Result: 1D array [200]
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example 3: 2D to 3D (Add dimension)
   ```clojure
   ;; Interpret 2D [12, 10] as 3D [4, 3, 10]
   (let [matrix (create-array data [12 10])   ; 12×10 = 120 elements
         dims-ptr (mem/alloc-pointer ::mem/long 3)
         _ (mem/write-pointer dims-ptr [4 3 10] ::mem/long)
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-moddims out-ptr matrix 3 dims-ptr)]
     ;; Result: 3D tensor [4, 3, 10] - same 120 elements
     ;; Now can process as 10 slices of 4×3 matrices
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example 4: Batch processing (Add batch dimension)
   ```clojure
   ;; Single image [H, W, C] to batch of 1 [H, W, C, 1]
   (let [image (create-array pixels [256 256 3])  ; HxWxC
         dims-ptr (mem/alloc-pointer ::mem/long 4)
         _ (mem/write-pointer dims-ptr [256 256 3 1] ::mem/long)
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-moddims out-ptr image 4 dims-ptr)]
     ;; Result: Batch of 1 image [256, 256, 3, 1]
     ;; Compatible with batch processing functions
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example 5: Reshape for broadcasting
   ```clojure
   ;; Reshape vector for broadcasting
   (let [vec (create-array data [64])          ; 1D vector
         dims-ptr (mem/alloc-pointer ::mem/long 2)
         _ (mem/write-pointer dims-ptr [1 64] ::mem/long)
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-moddims out-ptr vec 2 dims-ptr)]
     ;; Result: [1, 64] - row vector for broadcasting
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example 6: Matrix to column vector
   ```clojure
   ;; Column vector [N, 1] to pure 1D [N]
   (let [col-vec (create-array data [100 1])
         dims-ptr (mem/alloc-pointer ::mem/long 1)
         _ (mem/write-pointer dims-ptr [100] ::mem/long)
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-moddims out-ptr col-vec 1 dims-ptr)]
     ;; Result: 1D array [100] - simpler representation
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example 7: Unroll convolution (3D to 2D)
   ```clojure
   ;; Feature maps [H, W, C] to matrix [H*W, C]
   (let [features (create-array data [32 32 64])  ; 32×32 spatial, 64 channels
         h 32
         w 32
         c 64
         dims-ptr (mem/alloc-pointer ::mem/long 2)
         _ (mem/write-pointer dims-ptr [(* h w) c] ::mem/long)
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-moddims out-ptr features 2 dims-ptr)]
     ;; Result: [1024, 64] matrix
     ;; Each row is a spatial location, columns are channels
     ;; Useful for matrix-based convolution implementations
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Common Patterns:
   
   **Flatten for vectorization**:
   ```clojure
   (moddims tensor [(total-elements tensor)])
   ```
   
   **Add batch dimension**:
   ```clojure
   (moddims single-item (conj original-dims 1))
   ```
   
   **Remove singleton dimension**:
   ```clojure
   ;; [N, 1, M] → [N, M]
   (moddims array [N M])
   ```
   
   **Reshape for broadcasting**:
   ```clojure
   ;; [N] → [N, 1, 1, 1] for 4D broadcasting
   (moddims vec [N 1 1 1])
   ```
   
   When to Use:
   - Need different dimensional interpretation of same data
   - Preparing for operations requiring specific shapes
   - Batch/unbatch operations
   - Flattening for linear algebra
   - Zero-cost view changes
   
   When NOT to Use:
   - Need to reorder data (use af-reorder or af-transpose)
   - Want to change element count (impossible)
   - Need row-major interpretation (ArrayFire is column-major)
   - Actual data movement required
   
   Gotchas:
   - **Column-major**: First dimension varies fastest (not intuitive for C/Python users)
   - **Element count**: Must match exactly or error
   - **Not transpose**: moddims doesn't reorder data, only reinterprets
   - **May copy**: Non-linear arrays trigger automatic copy
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-flat: Specialized 1D flattening (convenience wrapper)
   - af-reorder: Permute dimensions with data reordering
   - af-transpose: 2D matrix transpose"
  "af_moddims" [::mem/pointer ::mem/pointer ::mem/int ::mem/pointer] ::mem/int)

;; af_err af_flat(af_array *out, const af_array in)
(defcfn af-flat
  "Flatten an array to one dimension.
   
   Reshapes any multi-dimensional array into a 1D vector containing all
   elements. This is equivalent to moddims with dimension [N] where N is
   the total element count.
   
   Parameters:
   - out: out pointer for flattened 1D array
   - in: input array of any dimensions
   
   Operation:
   - Multi-dimensional array → 1D array [total_elements]
   - Preserves element order (column-major)
   - Zero-copy in most cases (metadata-only)
   
   Algorithm:
   1. Get total element count N
   2. Call moddims(array, [N])
   3. Return 1D array
   
   This is a convenience function. Equivalent to:
   ```clojure
   (moddims array [(elements array)])
   ```
   
   Performance:
   - O(1) - metadata update only
   - No memory allocation
   - Shares buffer with input
   - Instant operation
   
   Type Support:
   All types: f32, f64, f16, c32, c64, s32, u32, s64, u64, s16, u16, s8, u8, b8
   
   Example 1: Flatten 2D matrix
   ```clojure
   ;; 2D matrix to 1D vector
   (let [matrix (create-array [[1.0 2.0 3.0]
                               [4.0 5.0 6.0]] [2 3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-flat out-ptr matrix)]
     ;; Result: [1.0 4.0 2.0 5.0 3.0 6.0]
     ;; Note: Column-major order!
     ;; First column: [1,4], then [2,5], then [3,6]
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example 2: Flatten 3D tensor
   ```clojure
   ;; 3D tensor to 1D vector
   (let [tensor (create-array data [4 5 6])  ; 4×5×6 = 120 elements
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-flat out-ptr tensor)]
     ;; Result: 1D array [120]
     ;; All elements in column-major order
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example 3: Vectorize for operations
   ```clojure
   ;; Flatten for dot product
   (let [array1 (create-array data1 [10 20])
         array2 (create-array data2 [10 20])
         flat1-ptr (mem/alloc-pointer ::mem/pointer)
         flat2-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-flat flat1-ptr array1)
         _ (af-flat flat2-ptr array2)
         flat1 (mem/read-pointer flat1-ptr ::mem/pointer)
         flat2 (mem/read-pointer flat2-ptr ::mem/pointer)]
     ;; Now compute: dot(flat1, flat2)
     ;; More efficient than nested loops
     (dot flat1 flat2))
   ```
   
   Example 4: Serialization
   ```clojure
   ;; Flatten for saving to file
   (let [weights (create-array model-weights [64 128 256])
         flat-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-flat flat-ptr weights)
         flat (mem/read-pointer flat-ptr ::mem/pointer)]
     ;; Write 1D array to file (simpler format)
     (save-to-file flat \"weights.bin\"))
   ```
   
   Example 5: Global operations
   ```clojure
   ;; Flatten for global statistics
   (let [image (create-array pixels [512 512 3])
         flat-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-flat flat-ptr image)
         flat (mem/read-pointer flat-ptr ::mem/pointer)]
     ;; Compute global mean/std
     {:mean (mean flat)
      :std (stdev flat)
      :min (min flat)
      :max (max flat)})
   ```
   
   Example 6: Feature vector extraction
   ```clojure
   ;; Deep learning: Flatten feature maps
   (let [conv-output (create-array features [7 7 512])  ; Conv layer output
         flat-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-flat flat-ptr conv-output)
         flat (mem/read-pointer flat-ptr ::mem/pointer)]
     ;; Result: [25088] vector (7*7*512)
     ;; Feed to fully-connected layer
     (fully-connected flat weights))
   ```
   
   Example 7: Matrix norms
   ```clojure
   ;; Frobenius norm via flattening
   (let [matrix (create-array data [100 50])
         flat-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-flat flat-ptr matrix)
         flat (mem/read-pointer flat-ptr ::mem/pointer)]
     ;; Frobenius norm = L2 norm of flattened matrix
     (norm flat 2))
   ```
   
   Common Patterns:
   
   **Dot product of matrices**:
   ```clojure
   (dot (flat A) (flat B))  ; Treats matrices as vectors
   ```
   
   **Element-wise operations across all dimensions**:
   ```clojure
   (let [flat (flat tensor)]
     ;; Apply to all elements uniformly
     (map-elements some-function flat))
   ```
   
   **Comparison with other operations**:
   ```clojure
   ;; These are equivalent:
   (flat array)                           ; Convenient
   (moddims array [(elements array)])     ; Explicit
   (moddims array [(.elements array)])    ; Java interop style
   ```
   
   **Reshape back after operations**:
   ```clojure
   (let [original-shape (get-dims array)
         flat (flat array)
         processed (process-1d flat)]
     ;; Restore original shape
     (moddims processed original-shape))
   ```
   
   When to Use:
   - Need 1D representation for algorithms
   - Vectorization for performance
   - Global operations (sum, mean, norm, etc.)
   - Serialization/deserialization
   - Linear algebra operations expecting vectors
   - Feeding to fully-connected layers (deep learning)
   
   When NOT to Use:
   - Already 1D (no-op, but safe)
   - Need specific reshape (use moddims directly)
   - Want to preserve structure for later
   
   Advantages over moddims:
   - More convenient for flattening
   - Self-documenting intent
   - No need to compute element count
   - Common operation, dedicated function
   
   Memory Behavior:
   - Zero-copy: Shares buffer with input
   - No allocation: Metadata-only
   - Reference counting: Input not released until flat released
   - Safe: Can coexist with original array
   
   Edge Cases:
   - Already 1D: Returns copy (or reference, depending on implementation)
   - Empty array: Returns empty 1D array [0]
   - Single element: Returns [1]
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-moddims: General dimension modification
   - af-get-elements: Get total element count"
  "af_flat" [::mem/pointer ::mem/pointer] ::mem/int)
