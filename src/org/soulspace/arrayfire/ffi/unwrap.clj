(ns org.soulspace.arrayfire.ffi.unwrap
  "Bindings for the ArrayFire image unwrapping function.
   
   Image unwrapping is a specialized operation that rearranges windowed sections
   of an input image into columns or rows. This is the inverse of the wrap
   operation and is particularly useful in signal processing, machine learning,
   and image analysis workflows.
   
   Mathematical Operation:
   
   Unwrap extracts overlapping or non-overlapping patches from an image and
   arranges them as columns (or rows) in the output array. This transformation
   is commonly used in:
   
   - **Convolutional Neural Networks**: Converting image patches to matrix form
   - **Patch-based processing**: Operating on local neighborhoods
   - **Signal processing**: Windowed analysis (STFT, spectrogram)
   - **Linear algebra formulations**: Matrix-vector operations on patches
   
   **Operation Description**:
   
   Given an input image of size [H, W] and window parameters:
   - Window size: [wx, wy]
   - Stride: [sx, sy]
   - Padding: [px, py]
   
   The unwrap operation:
   1. Pads the input image with [px, py] padding
   2. Extracts windows of size [wx, wy] at stride [sx, sy]
   3. Flattens each window into a 1D vector
   4. Arranges vectors as columns (or rows) in output
   
   **Output Dimensions**:
   
   For input [H, W, ...]:
   - Number of windows in x: nx = (H + 2*px - wx) / sx + 1
   - Number of windows in y: ny = (W + 2*py - wy) / sy + 1
   - Patch size: wx × wy
   
   If is_column = true:
   - Output: [wx*wy, nx*ny, ...]
   - Each column is one flattened patch
   
   If is_column = false:
   - Output: [nx*ny, wx*wy, ...]
   - Each row is one flattened patch
   
   **Example Visualization**:
   
   Input image (6×6) with 3×3 windows, stride 2, no padding:
   ```
   ┌─────────────────┐
   │ 1  2  3  4  5  6│
   │ 7  8  9 10 11 12│
   │13 14 15 16 17 18│
   │19 20 21 22 23 24│
   │25 26 27 28 29 30│
   │31 32 33 34 35 36│
   └─────────────────┘
   ```
   
   Windows extracted (3×3, stride 2):
   ```
   Window 1:        Window 2:        Window 3:        Window 4:
   ┌────────┐      ┌────────┐      ┌────────┐      ┌────────┐
   │ 1  2  3│      │ 3  4  5│      │13 14 15│      │15 16 17│
   │ 7  8  9│      │ 9 10 11│      │19 20 21│      │21 22 23│
   │13 14 15│      │15 16 17│      │25 26 27│      │27 28 29│
   └────────┘      └────────┘      └────────┘      └────────┘
   
   nx = (6 - 3)/2 + 1 = 2
   ny = (6 - 3)/2 + 1 = 2
   Total windows: 2 × 2 = 4
   ```
   
   Output (is_column=true): [9, 4]
   ```
   Column 1   Column 2   Column 3   Column 4
   ┌──────────────────────────────────────┐
   │   1         3        13        15    │
   │   2         4        14        16    │
   │   3         5        15        17    │
   │   7         9        19        21    │
   │   8        10        20        22    │
   │   9        11        21        23    │
   │  13        15        25        27    │
   │  14        16        26        28    │
   │  15        17        27        29    │
   └──────────────────────────────────────┘
   ```
   
   **Stride Effects**:
   
   - **stride = 1**: Maximum overlap, dense sampling
     * Each pixel appears in multiple patches
     * Large output (many columns/rows)
     * Use: Dense feature extraction
   
   - **stride = window_size**: No overlap, tiling
     * Each pixel appears in exactly one patch
     * Smaller output
     * Use: Non-overlapping blocks
   
   - **stride > window_size**: Sparse sampling with gaps
     * Some pixels may not appear in any patch
     * Smallest output
     * Use: Subsampling, efficiency
   
   **Padding Effects**:
   
   Padding adds border pixels before windowing:
   
   - **padding = 0**: No border, windows must fit entirely inside image
     * Output windows: ⌊(dim - window)/stride⌋ + 1
   
   - **padding > 0**: Extends image border, allows windows at edges
     * Enables extracting patches centered at edge pixels
     * Common for maintaining spatial dimensions in CNNs
     * Padded values typically zero (but ArrayFire implementation specific)
   
   - **padding = (window - 1) / 2**: \"Same\" padding
     * With stride=1, output has same spatial dimensions as input
     * Common in image processing and CNNs
   
   **Window Size Selection**:
   
   - **Small windows (3×3, 5×5)**: Local features
     * Good for edge detection, texture
     * Less computational cost
     * More spatial resolution in output
   
   - **Medium windows (7×7, 11×11)**: Regional features
     * Captures larger patterns
     * Balance of local vs global information
   
   - **Large windows (15×15+)**: Global features
     * Context-aware processing
     * Higher computational cost
     * Fewer output patches
   
   Algorithm Complexity:
   
   For input [H, W] with window [wx, wy], stride [sx, sy]:
   - Number of windows: nx × ny = O((H/sx) × (W/sy))
   - Time: O(nx × ny × wx × wy) = O(H × W × wx × wy / (sx × sy))
   - Space: O(nx × ny × wx × wy) for output
   
   GPU parallelization:
   - Each window extraction independent
   - Parallel across all windows
   - Memory-bound operation (data movement)
   - Typical speedup: 10-100× vs CPU
   
   Relationship to Other Operations:
   
   **Unwrap vs Wrap**:
   - Unwrap: Image → Columns (forward transform)
   - Wrap: Columns → Image (inverse transform)
   - With matching parameters: wrap(unwrap(img)) ≈ img
   - Note: Overlapping patches sum in wrap operation
   
   **Unwrap vs im2col (MATLAB/NumPy)**:
   - Functionally equivalent to im2col
   - Standard operation in CNN implementations
   - Enables matrix multiplication formulation of convolution
   
   **Convolution as Matrix Multiplication**:
   ```
   Unwrap input:  X_unrolled = unwrap(input, kh, kw, stride, stride, pad, pad)
   Flatten kernel: W_flat = reshape(kernel, [kh*kw, num_filters])
   Convolution:    output = W_flat^T × X_unrolled
   ```
   
   Use Cases:
   
   **1. Convolutional Neural Networks**:
   
   Im2col approach for efficient convolution:
   ```clojure
   (defn conv-via-unwrap [input kernel stride padding]
     (let [[kh kw] (get-dims kernel)
           ;; Unwrap input patches
           patches-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-unwrap patches-ptr input kh kw stride stride 
                       padding padding true)
           patches (mem/read-pointer patches-ptr ::mem/pointer)
           
           ;; Flatten kernel
           kernel-flat-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-flat kernel-flat-ptr kernel)
           kernel-flat (mem/read-pointer kernel-flat-ptr ::mem/pointer)
           
           ;; Matrix multiplication
           conv-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-matmul conv-ptr kernel-flat patches 
                       AF_MAT_TRANS AF_MAT_NONE)
           conv-result (mem/read-pointer conv-ptr ::mem/pointer)]
       conv-result))
   ```
   
   **2. Patch-Based Image Processing**:
   
   Apply function to all image patches:
   ```clojure
   (defn process-patches [image window-size patch-fn]
     (let [[wx wy] window-size
           ;; Extract all patches
           patches-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-unwrap patches-ptr image wx wy wx wy 0 0 true)
           patches (mem/read-pointer patches-ptr ::mem/pointer)
           
           ;; Process each patch (column)
           processed (patch-fn patches)
           
           ;; Reshape back if needed
           result-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-wrap result-ptr processed wx wy wx wy 0 0 true)
           result (mem/read-pointer result-ptr ::mem/pointer)]
       result))
   ```
   
   **3. Sliding Window Feature Extraction**:
   
   Extract features from overlapping windows:
   ```clojure
   (defn sliding-window-features [image]
     (let [;; Overlapping 8×8 windows, stride 4
           patches-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-unwrap patches-ptr image 8 8 4 4 0 0 true)
           patches (mem/read-pointer patches-ptr ::mem/pointer)
           
           ;; Compute features (e.g., mean, variance)
           mean-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-mean mean-ptr patches 0)
           mean (mem/read-pointer mean-ptr ::mem/pointer)
           
           var-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-var var-ptr patches false 0)
           var (mem/read-pointer var-ptr ::mem/pointer)]
       {:mean mean :variance var}))
   ```
   
   **4. Spectrogram/STFT (Signal Processing)**:
   
   Time-frequency analysis via windowing:
   ```clojure
   (defn compute-spectrogram [signal window-size hop-size]
     (let [;; Reshape signal to 2D [N, 1]
           signal-2d (af-moddims signal [(count signal) 1])
           
           ;; Extract overlapping windows
           windows-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-unwrap windows-ptr signal-2d window-size 1 
                       hop-size 1 0 0 true)
           windows (mem/read-pointer windows-ptr ::mem/pointer)
           
           ;; Apply FFT to each window
           fft-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-fft fft-ptr windows)
           spectrogram (mem/read-pointer fft-ptr ::mem/pointer)
           
           ;; Magnitude
           mag-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-abs mag-ptr spectrogram)]
       (mem/read-pointer mag-ptr ::mem/pointer)))
   ```
   
   **5. Dictionary Learning/Sparse Coding**:
   
   Learn dictionary from image patches:
   ```clojure
   (defn extract-training-patches [images patch-size num-patches]
     (let [[px py] patch-size
           ;; Extract patches from multiple images
           all-patches (for [img images]
                        (let [p-ptr (mem/alloc-pointer ::mem/pointer)
                              _ (af-unwrap p-ptr img px py 
                                          px py 0 0 true)
                              patches (mem/read-pointer p-ptr ::mem/pointer)]
                          patches))
           ;; Concatenate patches from all images
           combined-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-join combined-ptr 1 all-patches)]
       (mem/read-pointer combined-ptr ::mem/pointer)))
   ```
   
   **6. Texture Analysis**:
   
   Local Binary Patterns or texture descriptors:
   ```clojure
   (defn texture-descriptor [image]
     (let [;; Extract 3×3 neighborhoods
           patches-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-unwrap patches-ptr image 3 3 1 1 1 1 true)
           patches (mem/read-pointer patches-ptr ::mem/pointer)
           
           ;; Center pixel is element 4 (0-indexed)
           ;; Compare neighbors to center
           ;; ... compute texture features ...]
       patches))
   ```
   
   **7. Non-Local Means Denoising**:
   
   Compare patches for denoising:
   ```clojure
   (defn non-local-means-setup [image patch-size search-size]
     (let [[px py] patch-size
           ;; Extract all patches with overlap
           patches-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-unwrap patches-ptr image px py 1 1 
                       (/ (dec px) 2) (/ (dec py) 2) true)
           patches (mem/read-pointer patches-ptr ::mem/pointer)
           
           ;; Now can compute patch-to-patch distances
           ;; and weight contributions for denoising
           ;; ... distance computation ...]
       patches))
   ```
   
   Performance Considerations:
   
   **Memory Usage**:
   - Output can be much larger than input
   - For 512×512 image with 8×8 windows, stride 1:
     * Input: 512 × 512 = 262K elements
     * Output: 64 × (505 × 505) = 16.3M elements (62× larger!)
   - Use larger strides to reduce memory footprint
   - Consider batch processing for limited GPU memory
   
   **Computational Cost**:
   - Memory-bound operation (data movement dominates)
   - Time proportional to number of windows × window size
   - Reducing stride dramatically improves performance
   - GPU parallelization very effective
   
   **Optimization Strategies**:
   
   1. **Stride selection**: Balance between overlap and memory
      - stride = window_size for non-overlapping (fastest)
      - stride < window_size for overlap (slower, more memory)
   
   2. **Batch processing**: Process multiple images together
      - Better GPU utilization
      - Amortize kernel launch overhead
   
   3. **In-place operations**: Reuse buffers where possible
      - Minimize allocations
      - Reduce memory fragmentation
   
   4. **Fused operations**: Combine unwrap with subsequent ops
      - Avoid intermediate storage
      - Better memory locality
   
   Common Patterns:
   
   **Pattern 1: Non-Overlapping Tiling**
   ```clojure
   ;; Divide image into non-overlapping blocks
   (let [block-size 16
         patches-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-unwrap patches-ptr image 
                     block-size block-size    ; window
                     block-size block-size    ; stride = window (no overlap)
                     0 0                      ; no padding
                     true)]                   ; columns
     (mem/read-pointer patches-ptr ::mem/pointer))
   ```
   
   **Pattern 2: Dense Overlapping Windows**
   ```clojure
   ;; Maximum overlap (stride = 1)
   (let [window-size 5
         patches-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-unwrap patches-ptr image 
                     window-size window-size  ; window
                     1 1                      ; stride = 1 (dense)
                     0 0                      ; no padding
                     true)]
     (mem/read-pointer patches-ptr ::mem/pointer))
   ```
   
   **Pattern 3: Same-Size Convolution Setup**
   ```clojure
   ;; Padding to maintain dimensions
   (defn conv-same-unwrap [image kernel-size]
     (let [pad (quot (dec kernel-size) 2)
           patches-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-unwrap patches-ptr image 
                       kernel-size kernel-size
                       1 1              ; stride 1
                       pad pad          ; \"same\" padding
                       true)]
       (mem/read-pointer patches-ptr ::mem/pointer)))
   ```
   
   **Pattern 4: Pooling Operation**
   ```clojure
   ;; Max pooling via unwrap + reduce
   (defn max-pool [image pool-size stride]
     (let [patches-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-unwrap patches-ptr image 
                       pool-size pool-size
                       stride stride
                       0 0
                       true)
           patches (mem/read-pointer patches-ptr ::mem/pointer)
           
           ;; Max over each patch
           pooled-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-max pooled-ptr patches 0)]
       (mem/read-pointer pooled-ptr ::mem/pointer)))
   ```
   
   **Pattern 5: Batch Image Processing**
   ```clojure
   ;; Process batch of images with same parameters
   (defn unwrap-batch [images wx wy sx sy]
     (let [;; Images: [H, W, N] where N = batch size
           patches-ptr (mem/alloc-pointer ::mem/pointer)
           ;; Unwrap preserves batch dimension
           _ (af-unwrap patches-ptr images wx wy sx sy 0 0 true)
           patches (mem/read-pointer patches-ptr ::mem/pointer)]
       ;; Output: [wx*wy, num_windows, N]
       patches))
   ```
   
   Type Support:
   
   All ArrayFire types supported:
   - Floating-point: f32, f64
   - Complex: c32, c64
   - Signed integers: s8, s16, s32, s64
   - Unsigned integers: u8, u16, u32, u64
   - Boolean: b8
   
   Batch Processing:
   
   Input can be 3D or 4D for batch processing:
   - 2D input [H, W]: Single image
   - 3D input [H, W, N]: N images processed identically
   - 4D input [H, W, N, M]: N×M images
   
   Output preserves batch dimensions:
   - 2D input → 2D output [patch_size, num_patches]
   - 3D input → 3D output [patch_size, num_patches, N]
   - 4D input → 4D output [patch_size, num_patches, N, M]
   
   Parameter Constraints:
   
   - wx, wy: Window size, must be > 0
   - wx ≤ input.dims[0] + 2*px
   - wy ≤ input.dims[1] + 2*py
   - sx, sy: Stride, must be > 0
   - px, py: Padding, must be ≥ 0
   - px < wx, py < wy
   - is_column: true for column output, false for row output
   
   Error Conditions:
   
   - Window size too large for image
   - Invalid stride (≤ 0)
   - Invalid padding (negative or ≥ window size)
   - Insufficient memory for output
   
   Best Practices:
   
   1. **Choose stride wisely**:
      - Larger stride = less memory, faster
      - Smaller stride = more information, overlapping
   
   2. **Memory awareness**:
      - Check output size before unwrap
      - Use stride ≥ window_size for memory efficiency
      - Consider downsampling large images first
   
   3. **Batch processing**:
      - Process multiple images together
      - Leverage GPU parallelism
      - Amortize overhead
   
   4. **Combine with other ops**:
      - Use unwrap as part of larger pipeline
      - Fuse operations when possible
      - Minimize data transfers
   
   5. **Choose column vs row layout**:
      - Column major for BLAS operations
      - Row major for specific algorithms
      - Match downstream requirements
   
   Comparison with Other Frameworks:
   
   - **MATLAB**: im2col function
   - **NumPy/SciPy**: No direct equivalent (use stride_tricks)
   - **TensorFlow**: tf.image.extract_patches
   - **PyTorch**: torch.nn.Unfold
   - **Caffe**: im2col in convolution layers
   
   See also:
   - af-wrap: Inverse operation (columns/rows to image)
   - af-convolve: Direct convolution (alternative to unwrap+matmul)
   - af-moddims: Reshape arrays
   - af-tile: Replicate arrays"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; Image unwrapping

;; af_err af_unwrap(af_array* out, const af_array in, const dim_t wx, const dim_t wy, const dim_t sx, const dim_t sy, const dim_t px, const dim_t py, const bool is_column)
(defcfn af-unwrap
  "Rearrange windowed sections of an input image into columns or rows.
   
   Extracts overlapping or non-overlapping patches from an image and arranges
   them as columns (or rows) in the output. This is the im2col operation
   commonly used in convolutional neural networks and patch-based processing.
   
   Parameters:
   - out: Output pointer for unwrapped array
   - in: Input image array (2D, 3D, or 4D)
   - wx: Window size along dimension 0 (height)
   - wy: Window size along dimension 1 (width)
   - sx: Stride along dimension 0
   - sy: Stride along dimension 1
   - px: Padding along dimension 0
   - py: Padding along dimension 1
   - is-column: If true (1), each patch becomes a column; if false (0), a row
   
   Operation:
   1. Pads input with [px, py] on borders
   2. Extracts windows of size [wx, wy] at stride [sx, sy]
   3. Flattens each window into vector
   4. Arranges as columns (is_column=true) or rows (is_column=false)
   
   Output Dimensions:
   For input [H, W, ...]:
   - nx = (H + 2*px - wx) / sx + 1  (windows in x direction)
   - ny = (W + 2*py - wy) / sy + 1  (windows in y direction)
   - num_patches = nx × ny
   - patch_size = wx × wy
   
   If is_column = true:  [wx*wy, nx*ny, ...]
   If is_column = false: [nx*ny, wx*wy, ...]
   
   Type Support: All types (f32, f64, c32, c64, integers, b8)
   
   Example (Non-Overlapping Blocks):
   ```clojure
   ;; Extract 8×8 non-overlapping blocks
   (let [image (create-array img-data [256 256])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-unwrap out-ptr image 
                     8 8      ; 8×8 windows
                     8 8      ; stride = window (no overlap)
                     0 0      ; no padding
                     1)       ; column output
         patches (mem/read-pointer out-ptr ::mem/pointer)]
     ;; Output: [64, 1024] = [8*8, (256/8)*(256/8)]
     patches)
   ```
   
   Example (Overlapping Windows):
   ```clojure
   ;; Dense overlapping 5×5 patches
   (let [image (create-array img-data [64 64])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-unwrap out-ptr image 
                     5 5      ; 5×5 windows
                     1 1      ; stride = 1 (maximum overlap)
                     0 0      ; no padding
                     1)       ; column output
         patches (mem/read-pointer out-ptr ::mem/pointer)]
     ;; Output: [25, 3600] = [5*5, 60*60]
     ;; Each pixel (except borders) appears in multiple patches
     patches)
   ```
   
   Example (Convolution Setup):
   ```clojure
   ;; Prepare for convolution as matrix multiplication
   (defn convolve-via-unwrap [image kernel]
     (let [[kh kw] (get-kernel-size kernel)
           pad (quot (dec kh) 2)  ; \"same\" padding
           
           ;; Unwrap input into patches
           patches-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-unwrap patches-ptr image 
                       kh kw        ; kernel size
                       1 1          ; stride 1
                       pad pad      ; padding for \"same\"
                       1)           ; columns
           patches (mem/read-pointer patches-ptr ::mem/pointer)
           
           ;; Flatten kernel
           kernel-flat (flatten-kernel kernel)
           
           ;; Convolution = matrix multiplication
           result-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-matmul result-ptr kernel-flat patches 
                       AF_MAT_TRANS AF_MAT_NONE)
           result (mem/read-pointer result-ptr ::mem/pointer)]
       result))
   ```
   
   Example (Batch Processing):
   ```clojure
   ;; Process batch of images
   (let [images (create-array batch-data [128 128 32])  ; 32 images
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-unwrap out-ptr images 
                     7 7      ; 7×7 windows
                     4 4      ; stride 4
                     0 0      ; no padding
                     1)       ; columns
         patches (mem/read-pointer out-ptr ::mem/pointer)]
     ;; Output: [49, 961, 32] = [7*7, 31*31, 32]
     ;; All 32 images unwrapped in parallel
     patches)
   ```
   
   Example (Spectrogram):
   ```clojure
   ;; Time-frequency analysis via windowing
   (defn spectrogram [signal window-size hop-size]
     (let [;; Reshape to 2D
           sig-2d-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-moddims sig-2d-ptr signal [(count signal) 1])
           sig-2d (mem/read-pointer sig-2d-ptr ::mem/pointer)
           
           ;; Extract windows
           windows-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-unwrap windows-ptr sig-2d 
                       window-size 1    ; window size
                       hop-size 1       ; hop (stride)
                       0 0              ; no padding
                       1)               ; columns
           windows (mem/read-pointer windows-ptr ::mem/pointer)
           
           ;; FFT each window
           fft-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-fft fft-ptr windows)
           spectrum (mem/read-pointer fft-ptr ::mem/pointer)
           
           ;; Magnitude
           mag-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-abs mag-ptr spectrum)]
       (mem/read-pointer mag-ptr ::mem/pointer)))
   ```
   
   Example (Max Pooling):
   ```clojure
   ;; Implement max pooling using unwrap
   (defn max-pool [image pool-size stride]
     (let [;; Extract patches
           patches-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-unwrap patches-ptr image 
                       pool-size pool-size
                       stride stride
                       0 0
                       1)  ; columns
           patches (mem/read-pointer patches-ptr ::mem/pointer)
           
           ;; Max over each patch (column)
           pooled-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-max pooled-ptr patches 0)
           pooled (mem/read-pointer pooled-ptr ::mem/pointer)]
       pooled))
   ```
   
   Use Cases:
   - CNN convolution (im2col approach)
   - Patch-based image processing
   - Sliding window feature extraction
   - Spectrogram computation
   - Dictionary learning from patches
   - Texture analysis (local neighborhoods)
   - Non-local means denoising setup
   
   Parameter Selection:
   
   **Window Size (wx, wy)**:
   - Small (3×3, 5×5): Local features, edge detection
   - Medium (7×7, 11×11): Regional patterns, texture
   - Large (15×15+): Global context, object parts
   
   **Stride (sx, sy)**:
   - stride = 1: Maximum overlap, dense sampling
   - stride = window: No overlap, efficient tiling
   - stride > window: Sparse sampling, some pixels skipped
   
   **Padding (px, py)**:
   - padding = 0: Windows fit entirely in image
   - padding = (window-1)/2: \"Same\" size output (with stride=1)
   - padding > 0: Include border pixels, extend image
   
   **Layout (is_column)**:
   - true: Column layout (standard for BLAS)
   - false: Row layout (specific algorithm needs)
   
   Memory Considerations:
   
   Output size can be much larger than input:
   - Input: H × W
   - Output: (wx × wy) × (nx × ny)
   - With stride=1, output can be 10-100× larger
   
   For 512×512 image, 8×8 windows, stride 1:
   - Input: 262,144 elements
   - Output: 64 × 255,025 = 16,321,600 elements (62× larger!)
   
   Memory optimization:
   - Use larger strides (reduces number of patches)
   - Process in batches if memory limited
   - Consider stride = window_size for efficiency
   
   Performance:
   - Time: O((H/sx) × (W/sy) × wx × wy)
   - Space: O(nx × ny × wx × wy)
   - GPU parallelized across all windows
   - Memory-bound operation
   - Typical speedup vs CPU: 10-100×
   
   Constraints:
   - wx, wy must be > 0
   - wx ≤ H + 2*px, wy ≤ W + 2*py
   - sx, sy must be > 0
   - px, py must be ≥ 0
   - px < wx, py < wy
   
   Batch Processing:
   - 3D input [H, W, N]: N images → [patch_size, num_patches, N]
   - 4D input [H, W, N, M]: N×M images → [patch_size, num_patches, N, M]
   - Batch dimension preserved
   
   Notes:
   - Inverse operation: af-wrap
   - Memory-intensive for small strides
   - Efficient GPU implementation
   - Standard im2col operation for CNNs
   
   Returns:
   ArrayFire error code (af_err enum):
   - AF_SUCCESS (0): Unwrap successful
   - AF_ERR_ARG: Invalid parameters (window size, stride, padding)
   - AF_ERR_SIZE: Window doesn't fit in padded image
   - AF_ERR_NO_MEM: Insufficient memory
   
   See also:
   - af-wrap: Inverse operation (columns/rows to image)
   - af-convolve: Direct convolution
   - af-moddims: Array reshaping
   - af-tile: Array replication"
  "af_unwrap" [::mem/pointer ::mem/pointer ::mem/long ::mem/long 
               ::mem/long ::mem/long ::mem/long ::mem/long ::mem/int] ::mem/int)
