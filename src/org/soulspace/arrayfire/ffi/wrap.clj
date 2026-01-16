(ns org.soulspace.arrayfire.ffi.wrap
  "Bindings for the ArrayFire wrap operation.
   
   Image Wrapping and Patch Reconstruction:
   
   The wrap operation is the inverse of unwrap, reconstructing a 2D image from
   a set of extracted patches (windows). This is a fundamental operation in
   image processing, particularly for sliding-window algorithms, patch-based
   methods, and neural network operations.
   
   Mathematical Foundation:
   
   **Wrap Operation**:
   Given an input array where each column (or row) represents a flattened patch,
   wrap reconstructs the original 2D image by placing patches back into their
   spatial positions according to the specified stride and padding.
   
   **Patch Placement**:
   For output dimensions (ox, oy) and window size (wx, wy):
   - Number of patches in x: nx = (ox + 2*px - wx)/sx + 1
   - Number of patches in y: ny = (oy + 2*py - wy)/sy + 1
   - Total patches: nx * ny
   
   **Overlapping Patches**:
   When stride < window size, patches overlap. In overlapping regions, values
   are accumulated (summed), not averaged. This is the adjoint operation to
   unwrap and is crucial for gradient computations in deep learning.
   
   Key Concepts:
   
   1. **Window Size (wx, wy)**:
      - Size of each patch being reconstructed
      - Must match patch size from unwrap operation
      - Patch area: wx * wy
   
   2. **Stride (sx, sy)**:
      - Step size between consecutive patches
      - Controls overlap: stride < window → overlap
      - Stride = window → no overlap (tiling)
   
   3. **Padding (px, py)**:
      - Virtual border added during unwrap
      - Must use same padding to correctly reconstruct
      - Affects number of patches calculation
   
   4. **Column vs Row Order (is_column)**:
      - True: patches stored as columns (default)
      - False: patches stored as rows
      - Must match unwrap orientation
   
   Input Requirements:
   
   - **Patch dimension**: input.dims[0] = wx * wy (if is_column=true)
                         or input.dims[1] = wx * wy (if is_column=false)
   - **Number of patches**: input.dims[1] = nx * ny (if is_column=true)
                           or input.dims[0] = nx * ny (if is_column=false)
   - **Batch processing**: Dimensions 2 and 3 preserved for batches
   
   Overlap Handling:
   
   When patches overlap (stride < window size), overlapping pixels are summed:
   - Not averaged or max-pooled
   - Sum accumulation enables gradient flow
   - Useful for inverse operations in backpropagation
   - May need normalization based on overlap factor
   
   Performance:
   - Complexity: O(ox * oy * wx * wy) worst case
   - GPU-accelerated parallel patch placement
   - Efficient for large images and many patches
   - Memory: Output allocated automatically (af-wrap) or user-provided (af-wrap-v2)
   
   Common Applications:
   
   1. **Image Reconstruction**:
      - Inverse of sliding window extraction
      - Reconstructing from patch-based processing
      - Stitching processed patches back together
   
   2. **Deep Learning**:
      - Transposed convolutions (deconvolution)
      - Gradient computation for conv layers
      - Patch-based neural networks
      - Autoencoder reconstruction
   
   3. **Image Processing**:
      - Block-based compression reconstruction
      - Non-local means filtering
      - Patch-based denoising
      - Super-resolution
   
   4. **Signal Processing**:
      - Overlapped short-time processing
      - Spectrogram reconstruction
      - Frame-based audio processing
   
   Version Differences:
   
   - **af-wrap**: Automatically allocates output array
   - **af-wrap-v2**: Accepts pre-allocated output (efficient for reuse)
                     Available in API version 3.7+
   
   Type Support:
   - All numeric types: f32, f64, s32, u32, s64, u64, s16, u16, s8, u8
   - Complex types: c32, c64
   - Boolean: b8
   
   Example Workflow:
   ```clojure
   ;; 1. Extract patches with unwrap
   (let [img (create-array data [100 100])
         patches-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-unwrap patches-ptr img 8 8 2 2 0 0 true)
         patches (mem/read-pointer patches-ptr ::mem/pointer)]
     
     ;; 2. Process patches (e.g., filter, transform)
     (let [processed (process-patches patches)
           
           ;; 3. Reconstruct with wrap (same parameters)
           reconstructed-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-wrap reconstructed-ptr processed 100 100 8 8 2 2 0 0 true)
           result (mem/read-pointer reconstructed-ptr ::mem/pointer)]
       result))
   ```
   
   Important Notes:
   - Parameters must match those used in af-unwrap for correct reconstruction
   - Output dimensions (ox, oy) should be original image size
   - Overlapping regions will have summed values (not averaged)
   - Consider normalization after wrap for overlapping patches
   - Batch operations process multiple images efficiently
   
   See also:
   - af-unwrap: Extract patches from image (inverse operation)
   - af-convolve2: Alternative for sliding window operations
   - af-moddims: For reshaping operations"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; af_err af_wrap(af_array* out, const af_array in, const dim_t ox, const dim_t oy, const dim_t wx, const dim_t wy, const dim_t sx, const dim_t sy, const dim_t px, const dim_t py, const bool is_column)
(defcfn af-wrap
  "Reconstruct 2D image from patches (columns or rows).
   
   Wrap is the inverse of unwrap, placing flattened patches back into their
   spatial positions to reconstruct the original 2D image. This function
   automatically allocates the output array.
   
   Parameters:
   - out: Output pointer for reconstructed image
   - in: Input array with patches as columns (or rows)
        * If is_column=true: dims[0] = wx*wy, dims[1] = nx*ny
        * If is_column=false: dims[0] = nx*ny, dims[1] = wx*wy
   - ox: Output image width (dimension 0)
   - oy: Output image height (dimension 1)
   - wx: Patch width (window size along dim 0)
   - wy: Patch height (window size along dim 1)
   - sx: Stride along dimension 0 (must be ≥ 1)
   - sy: Stride along dimension 1 (must be ≥ 1)
   - px: Padding along dimension 0 (must be < wx)
   - py: Padding along dimension 1 (must be < wy)
   - is_column: True if patches are columns, false if rows (use 1/0 for bool)
   
   The number of patches must satisfy:
   - nx = (ox + 2*px - wx)/sx + 1
   - ny = (oy + 2*py - wy)/sy + 1
   - Total patches = nx * ny
   
   Overlap Behavior:
   When stride < window size, patches overlap. In overlapping regions,
   pixel values are accumulated (summed). This is intentional and matches
   the adjoint operation needed for gradient computation.
   
   To normalize overlapping regions:
   - Count overlaps per pixel
   - Divide reconstructed image by overlap count
   - Or use appropriate scaling during processing
   
   Example (reconstruct from patches):
   ```clojure
   ;; Extract and reconstruct with no overlap (tiling)
   (let [img (create-array data [64 64])
         ;; Extract 8x8 patches with stride=8 (no overlap)
         patches-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-unwrap patches-ptr img 8 8 8 8 0 0 true)
         patches (mem/read-pointer patches-ptr ::mem/pointer)
         
         ;; Process patches here...
         
         ;; Reconstruct (use same parameters)
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-wrap out-ptr patches 64 64 8 8 8 8 0 0 true)
         reconstructed (mem/read-pointer out-ptr ::mem/pointer)]
     reconstructed)
   ```
   
   Example (overlapping patches):
   ```clojure
   ;; Extract overlapping patches (stride < window)
   (let [img (create-array data [100 100])
         ;; 8x8 patches with stride=4 (50% overlap)
         patches-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-unwrap patches-ptr img 8 8 4 4 0 0 true)
         patches (mem/read-pointer patches-ptr ::mem/pointer)
         
         ;; Reconstruct - overlaps will be summed
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-wrap out-ptr patches 100 100 8 8 4 4 0 0 true)
         reconstructed (mem/read-pointer out-ptr ::mem/pointer)]
     ;; Note: overlapping regions contain summed values
     ;; May need normalization based on overlap factor
     reconstructed)
   ```
   
   Example (with padding):
   ```clojure
   ;; Wrap with padding (for border handling)
   (let [patches (create-array patch-data [64 144])  ; 8x8 patches
         ;; Reconstruct 32x32 image with padding=2
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-wrap out-ptr patches 32 32 8 8 2 2 2 2 true)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)
   ```
   
   Example (batch processing):
   ```clojure
   ;; Wrap multiple images at once
   (let [batch-patches (create-array data [64 100 10])  ; 10 images
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-wrap out-ptr batch-patches 32 32 8 8 4 4 0 0 true)
         batch-result (mem/read-pointer out-ptr ::mem/pointer)]
     ;; Result has dims [32 32 10]
     batch-result)
   ```
   
   Typical Use Cases:
   - Reconstructing after patch-based denoising
   - Inverse operation for sliding-window feature extraction
   - Transposed convolution (deconvolution) operations
   - Patch-based neural network reconstruction layers
   - Block-based image compression reconstruction
   
   Constraints:
   - wx, wy must be > 0
   - sx, sy must be ≥ 1
   - px must be in [0, wx)
   - py must be in [0, wy)
   - Patch size must equal wx * wy
   - Number of patches must equal nx * ny
   - is_column orientation must match unwrap
   
   Returns:
   AF_SUCCESS or error code
   
   See also:
   - af-unwrap: Extract patches (inverse operation)
   - af-wrap-v2: Version with pre-allocated output"
  "af_wrap" [::mem/pointer ::mem/pointer ::mem/long ::mem/long ::mem/long ::mem/long ::mem/long ::mem/long ::mem/long ::mem/long ::mem/int] ::mem/int)

;; af_err af_wrap_v2(af_array* out, const af_array in, const dim_t ox, const dim_t oy, const dim_t wx, const dim_t wy, const dim_t sx, const dim_t sy, const dim_t px, const dim_t py, const bool is_column)
(defcfn af-wrap-v2
  "Reconstruct 2D image from patches with pre-allocated output.
   
   Version of af-wrap that accepts a pre-allocated output array, enabling
   efficient memory reuse in loops or repeated operations. If output pointer
   is NULL, allocates automatically like af-wrap.
   
   Parameters:
   - out: Output pointer (can be pre-allocated or NULL for auto-allocation)
   - in: Input array with patches
   - ox: Output image width
   - oy: Output image height
   - wx: Patch width
   - wy: Patch height
   - sx: Stride along dimension 0
   - sy: Stride along dimension 1
   - px: Padding along dimension 0
   - py: Padding along dimension 1
   - is_column: True if patches are columns (use 1/0 for bool)
   
   Behavior:
   - If *out == NULL: Allocates output automatically (same as af-wrap)
   - If *out != NULL: Uses pre-allocated array (must have correct dimensions)
   
   Pre-allocation Benefits:
   - Reduces memory allocation overhead in loops
   - Enables output buffer reuse
   - Better performance for repeated operations
   - Useful in real-time or streaming scenarios
   
   Example (efficient loop with reuse):
   ```clojure
   ;; Process multiple frames with buffer reuse
   (let [out-ptr (mem/alloc-pointer ::mem/pointer)
         ;; First call allocates
         _ (mem/write-pointer! out-ptr (mem/nullptr) ::mem/pointer)]
     
     (doseq [frame frames]
       (let [patches (extract-patches frame)
             ;; Reuses existing output buffer
             _ (af-wrap-v2 out-ptr patches 64 64 8 8 4 4 0 0 true)]
         (process-result (mem/read-pointer out-ptr ::mem/pointer))))
     
     ;; Clean up
     (af-release-array (mem/read-pointer out-ptr ::mem/pointer)))
   ```
   
   Example (pre-allocated for known size):
   ```clojure
   ;; Pre-allocate output for known dimensions
   (let [out-ptr (mem/alloc-pointer ::mem/pointer)
         ;; Create pre-allocated output array
         _ (af-constant out-ptr 0.0 [64 64] AF_F32)
         
         patches (create-patches data)
         ;; Uses pre-allocated buffer
         _ (af-wrap-v2 out-ptr patches 64 64 8 8 4 4 0 0 true)
         result (mem/read-pointer out-ptr ::mem/pointer)]
     result)
   ```
   
   Example (streaming reconstruction):
   ```clojure
   ;; Efficient streaming with buffer reuse
   (let [out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (mem/write-pointer! out-ptr (mem/nullptr) ::mem/pointer)]
     
     (loop [stream patch-stream]
       (when-let [patches (get-next-patches stream)]
         (af-wrap-v2 out-ptr patches 128 128 16 16 8 8 0 0 true)
         (display-result (mem/read-pointer out-ptr ::mem/pointer))
         (recur stream)))
     
     ;; Cleanup
     (when-not (mem/nullptr? (mem/read-pointer out-ptr ::mem/pointer))
       (af-release-array (mem/read-pointer out-ptr ::mem/pointer))))
   ```
   
   Pre-allocation Requirements:
   - Output must have dimensions [ox, oy, dims[2], dims[3]]
   - Output type must match input type
   - Output must be valid array handle (not released)
   
   Performance Notes:
   - Significant speedup when called repeatedly
   - Eliminates allocation overhead per call
   - Reduces memory fragmentation
   - Ideal for real-time processing pipelines
   
   Available in API version 3.7 and later.
   
   Returns:
   AF_SUCCESS or error code
   
   See also:
   - af-wrap: Version with automatic allocation
   - af-unwrap-v2: Unwrap with pre-allocation"
  "af_wrap_v2" [::mem/pointer ::mem/pointer ::mem/long ::mem/long ::mem/long ::mem/long ::mem/long ::mem/long ::mem/long ::mem/long ::mem/int] ::mem/int)
