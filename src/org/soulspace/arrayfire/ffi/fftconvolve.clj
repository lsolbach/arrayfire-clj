(ns org.soulspace.arrayfire.ffi.fftconvolve
  "Bindings for the ArrayFire FFT-based convolution functions.
   
   FFT Convolution performs convolution in the frequency domain using the Fast
   Fourier Transform, exploiting the convolution theorem which states:
   
   Convolution Theorem:
   f ⊗ g = F^(-1)[F[f] · F[g]]
   
   Where:
   - ⊗ denotes convolution
   - F denotes the Fourier transform
   - F^(-1) denotes the inverse Fourier transform
   - · denotes element-wise multiplication
   
   Mathematical Foundation:
   
   Discrete Convolution (spatial domain):
   (f ⊗ g)[n] = Σ f[m] * g[n - m]
   
   Complexity: O(N²) for spatial domain
   Complexity: O(N log N) for frequency domain
   
   Performance:
   - FFT convolution is faster for large filter sizes (typically > 11×11)
   - Spatial convolution is faster for small filters
   - ArrayFire automatically selects the best algorithm with AF_CONV_AUTO
   
   FFT convolution workflow:
   1. Pad signal and filter to avoid circular convolution artifacts
   2. Apply FFT to both signal and filter
   3. Multiply element-wise in frequency domain
   4. Apply inverse FFT to get result
   
   Modes:
   - AF_CONV_DEFAULT (0): Output size = input size (same padding)
   - AF_CONV_EXPAND (1): Output size = signal_size + filter_size - 1 (full)
   
   Applications:
   - Large filter convolution (Gaussian blur, motion blur)
   - Template matching
   - Correlation (cross-correlation)
   - Edge detection with large kernels
   - Frequency domain filtering
   
   Batching Support:
   - AF_BATCH_NONE: Single signal, single filter
   - AF_BATCH_LHS: Multiple signals, single filter (common)
   - AF_BATCH_RHS: Single signal, multiple filters
   - AF_BATCH_SAME: Multiple signals, multiple filters (paired)
   
   See also:
   - fft.clj for direct FFT operations
   - Regular convolve functions that use AF_CONV_AUTO domain selection"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; FFT-based convolution functions

;; af_err af_fft_convolve1(af_array *out, const af_array signal, const af_array filter, const af_conv_mode mode)
(defcfn af-fft-convolve1
  "Perform 1D convolution using Fast Fourier Transform.
   
   Uses the convolution theorem to compute convolution in frequency domain:
   f ⊗ g = F^(-1)[F[f] · F[g]]
   
   This is faster than spatial domain convolution for large filters.
   
   Parameters:
   - out: out pointer for result array
   - signal: input signal array (1D or higher)
   - filter: filter/kernel array (1D or higher)
   - mode: convolution mode (af_conv_mode enum)
     * AF_CONV_DEFAULT (0): Output size = signal size
     * AF_CONV_EXPAND (1): Output size = signal_size + filter_size - 1
   
   Output Size:
   - AF_CONV_DEFAULT: Same as signal (center of full convolution)
   - AF_CONV_EXPAND: signal_len + filter_len - 1 (full convolution)
   
   Workflow:
   1. Zero-pad signal and filter to avoid circular convolution
      - Padded size: signal_len + filter_len - 1
   2. Apply FFT to both arrays
   3. Element-wise multiply in frequency domain
   4. Apply inverse FFT
   5. Extract result based on mode
   
   Performance:
   - Complexity: O(N log N) due to FFT operations
   - Faster than spatial convolution for large filters (> ~100 elements)
   - GPU acceleration provides massive speedup
   
   Type Support:
   - Integral types: Converted to float internally
   - f32: Uses float precision
   - f64: Uses double precision
   - c32/c64: Complex types supported
   
   Batching:
   - Signal dims: [N, batch_s, ...]
   - Filter dims: [M, batch_f, ...] or [M, 1, ...]
   - Output dims: [output_len, max(batch_s, batch_f), ...]
   
   Example (1D audio filtering):
   ```clojure
   ;; Apply lowpass filter to audio signal
   (let [audio (create-array audio-data [44100])  ; 1 second @ 44.1kHz
         filter (create-array lpf-kernel [101])    ; 101-tap FIR filter
         out-ptr (mem/alloc-pointer ::mem/pointer)
         mode 0]  ; AF_CONV_DEFAULT
     (af-fft-convolve1 out-ptr audio filter mode)
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Common Use Cases:
   - Audio processing (filtering, equalization)
   - Time series analysis (moving average, trend removal)
   - Signal smoothing with large kernels
   - Cross-correlation for signal alignment
   
   Notes:
   - Zero-padding is automatic to prevent circular convolution
   - Use AF_CONV_EXPAND to see full convolution output
   - Filter is NOT flipped (correlation behavior)
     For true convolution, flip filter before calling
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-fft-convolve2: 2D FFT convolution
   - af-fft-convolve3: 3D FFT convolution
   - Regular convolve1 with AF_CONV_FREQ domain"
  "af_fft_convolve1" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_fft_convolve2(af_array *out, const af_array signal, const af_array filter, const af_conv_mode mode)
(defcfn af-fft-convolve2
  "Perform 2D convolution using Fast Fourier Transform.
   
   Uses 2D FFT to compute convolution in frequency domain, which is
   significantly faster than spatial domain for large filters.
   
   Parameters:
   - out: out pointer for result array
   - signal: input signal array (2D or higher)
   - filter: filter/kernel array (2D or higher)
   - mode: convolution mode (af_conv_mode enum)
     * AF_CONV_DEFAULT (0): Output size = signal size
     * AF_CONV_EXPAND (1): Output size = signal_size + filter_size - 1
   
   Output Size:
   - AF_CONV_DEFAULT: Same as signal dimensions
   - AF_CONV_EXPAND:
     * dim0: signal_dim0 + filter_dim0 - 1
     * dim1: signal_dim1 + filter_dim1 - 1
   
   Workflow:
   1. Zero-pad both arrays to avoid circular convolution
      - Padded size: signal_size + filter_size - 1 (per dimension)
   2. Apply 2D FFT to both arrays
   3. Element-wise multiply in frequency domain
   4. Apply inverse 2D FFT
   5. Extract result region based on mode
   
   Performance:
   - Complexity: O(N M log(N M)) for N×M images
   - Faster than spatial domain for filters > ~11×11
   - GPU provides 10-100× speedup over CPU
   - Parallel processing across batches
   
   Type Support:
   - Converts integral types to float internally
   - f32/c32: Single precision (faster, less memory)
   - f64/c64: Double precision (more accurate)
   
   Batching:
   - Signal: [H, W, batch_s, ...] (multiple images)
   - Filter: [h, w, batch_f, ...] or [h, w, 1, ...]
   - Output: [out_h, out_w, max(batch_s, batch_f), ...]
   
   Example (Image filtering):
   ```clojure
   ;; Apply Gaussian blur using FFT
   (let [image (create-array img-data [512 512])  ; 512×512 image
         gaussian (create-array gauss-kernel [31 31])  ; 31×31 kernel
         out-ptr (mem/alloc-pointer ::mem/pointer)
         mode 0]  ; AF_CONV_DEFAULT
     (af-fft-convolve2 out-ptr image gaussian mode)
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (Batch processing):
   ```clojure
   ;; Filter multiple images with same kernel
   (let [images (create-array batch-data [512 512 100])  ; 100 images
         kernel (create-array sobel-x [3 3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         mode 0]
     (af-fft-convolve2 out-ptr images kernel mode)
     ;; Output: [512 512 100] - all filtered in parallel
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Common Use Cases:
   - Image filtering (blur, sharpen, denoise)
   - Edge detection with large kernels
   - Template matching
   - Phase correlation (image registration)
   - Frequency domain filtering
   
   Notes:
   - Automatically zero-pads to prevent wrap-around artifacts
   - For small filters (< 11×11), spatial domain may be faster
   - Filter is NOT flipped (this is correlation, not convolution)
     To get true convolution, flip filter with af_flip
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-fft-convolve1: 1D FFT convolution
   - af-fft-convolve3: 3D FFT convolution
   - af-convolve2 with AF_CONV_FREQ domain"
  "af_fft_convolve2" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_fft_convolve3(af_array *out, const af_array signal, const af_array filter, const af_conv_mode mode)
(defcfn af-fft-convolve3
  "Perform 3D convolution using Fast Fourier Transform.
   
   Uses 3D FFT to compute volumetric convolution in frequency domain,
   essential for processing large 3D datasets efficiently.
   
   Parameters:
   - out: out pointer for result array
   - signal: input signal array (3D or higher)
   - filter: filter/kernel array (3D or higher)
   - mode: convolution mode (af_conv_mode enum)
     * AF_CONV_DEFAULT (0): Output size = signal size
     * AF_CONV_EXPAND (1): Output size = signal_size + filter_size - 1
   
   Output Size:
   - AF_CONV_DEFAULT: Same as signal dimensions
   - AF_CONV_EXPAND: signal_size + filter_size - 1 (per dimension)
     * dim0: signal_dim0 + filter_dim0 - 1
     * dim1: signal_dim1 + filter_dim1 - 1
     * dim2: signal_dim2 + filter_dim2 - 1
   
   Workflow:
   1. Zero-pad both volumes to avoid circular convolution
      - Padded size: signal_size + filter_size - 1 (each dimension)
   2. Apply 3D FFT to both arrays
   3. Element-wise multiply in frequency domain
   4. Apply inverse 3D FFT
   5. Extract result region based on mode
   
   Performance:
   - Complexity: O(N³ log(N³)) for N³ volumes
   - Much faster than spatial domain for large 3D filters
   - GPU provides massive speedup (100-1000×)
   - Memory intensive - consider volume sizes carefully
   
   Type Support:
   - Converts integral types to float internally
   - f32/c32: Single precision (recommended for memory)
   - f64/c64: Double precision (very memory intensive)
   
   Memory Considerations:
   - 3D FFT requires significant memory
   - Padded size can be large: (N+M-1)³ complex values
   - Example: 512³ volume padded to 1024³ requires ~8GB (f32)
   - Use f32 unless high precision is essential
   
   Batching:
   - Signal: [D, H, W, batch_s, ...] (multiple volumes)
   - Filter: [d, h, w, batch_f, ...] or [d, h, w, 1, ...]
   - Output: [out_d, out_h, out_w, max(batch_s, batch_f)]
   
   Example (3D medical imaging):
   ```clojure
   ;; Apply 3D Gaussian filter to CT scan
   (let [volume (create-array ct-data [256 256 256])  ; CT scan
         gaussian3d (create-array gauss-kernel [15 15 15])  ; 3D kernel
         out-ptr (mem/alloc-pointer ::mem/pointer)
         mode 0]  ; AF_CONV_DEFAULT
     (af-fft-convolve3 out-ptr volume gaussian3d mode)
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (Batch 3D processing):
   ```clojure
   ;; Process multiple MRI volumes
   (let [mri-scans (create-array scan-data [128 128 128 20])  ; 20 scans
         filter3d (create-array kernel [7 7 7])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         mode 0]
     (af-fft-convolve3 out-ptr mri-scans filter3d mode)
     ;; Output: [128 128 128 20] - all processed in parallel
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Common Use Cases:
   - Medical imaging (CT, MRI filtering)
   - 3D deconvolution microscopy
   - Volumetric rendering
   - 3D fluid simulation
   - Video processing (treating time as 3rd dimension)
   - Seismic data processing
   
   Optimization Tips:
   - Use power-of-2 sizes when possible for fastest FFT
   - Consider using f32 to reduce memory usage
   - For very large volumes, consider tiling/streaming
   - GPU memory limits may constrain volume size
   
   Notes:
   - Automatic zero-padding prevents circular convolution
   - For small 3D filters, spatial domain may be competitive
   - Filter is NOT flipped (correlation behavior)
     For true convolution, flip filter in all 3 dimensions
   - Monitor GPU memory usage for large volumes
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-fft-convolve1: 1D FFT convolution
   - af-fft-convolve2: 2D FFT convolution
   - af-convolve3 with AF_CONV_FREQ domain"
  "af_fft_convolve3" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)
