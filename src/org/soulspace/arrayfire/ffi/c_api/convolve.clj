(ns org.soulspace.arrayfire.ffi.c-api.convolve
  "Bindings for the ArrayFire convolution functions.
   
   Convolution is a fundamental operation in signal and image processing that
   combines two signals to produce a third signal. ArrayFire provides both
   spatial domain and frequency domain (FFT-based) convolution implementations.
   
   ## Convolution Modes
   
   - **AF_CONV_DEFAULT (0)**: Same as AF_CONV_EXPAND
   - **AF_CONV_EXPAND (1)**: Output size = input size + filter size - 1
   - **AF_CONV_SAME (2)**: Output size = input size
   
   ## Convolution Domains
   
   - **AF_CONV_AUTO (0)**: Automatically choose between spatial and frequency domain
     - Heuristics: large filters, many batches → frequency domain
     - Small filters, few batches → spatial domain
   - **AF_CONV_SPATIAL (1)**: Force spatial domain convolution
   - **AF_CONV_FREQ (2)**: Force frequency domain (FFT) convolution
   
   ## Batch Modes
   
   ArrayFire supports several batch convolution patterns:
   - **AF_BATCH_NONE**: One-to-one convolution (no batch)
   - **AF_BATCH_LHS**: Many signals, one filter (MANY-to-ONE)
   - **AF_BATCH_RHS**: One signal, many filters (ONE-to-MANY)
   - **AF_BATCH_SAME**: Equal batches of signals and filters
   - **AF_BATCH_DIFF**: Different batch sizes (uses fallback to spatial domain)
   - **AF_BATCH_UNSUPPORTED**: Incompatible batch dimensions
   
   ## Performance Considerations
   
   - **Spatial domain**: Better for small filters (<128 elements in 1D, <11×11 in 2D)
   - **Frequency domain**: Better for large filters, many batches (≥10)
   - **Separable convolution** (2D only): Optimal for separable filters
     - Reduces complexity from O(M×N×P×Q) to O(M×N×(P+Q))
     - E.g., Gaussian blur, Sobel filters
   
   ## Neural Network Convolution
   
   The af_convolve2_nn function provides convolution optimized for deep learning:
   - Supports stride, padding, and dilation parameters
   - Batch dimensions: signals [d0 d1 d2 Ns], filters [d0 d1 d2 Nf]
   - Output: [d0 d1 Nf Ns]
   - Includes gradient computation via af_convolve2_gradient_nn"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; General convolution functions (auto-dispatch to 1D/2D/3D)

;; af_err af_convolve1(af_array *out, const af_array signal, const af_array filter, const af_conv_mode mode, af_conv_domain domain)
(defcfn af-convolve1
  "1D convolution on one-dimensional signals.
   
   Performs convolution between a signal and a filter in 1 dimension.
   
   Parameters:
   - out: out pointer for result array
   - signal: input signal array
   - filter: filter array (will be flipped for convolution)
   - mode: convolution mode (0=EXPAND, 1=SAME)
   - domain: convolution domain (0=AUTO, 1=SPATIAL, 2=FREQ)
   
   Convolution formula: out[i] = Σ signal[i+j] × filter[N-1-j]
   where N is the filter length.
   
   Output sizes:
   - EXPAND: signal_len + filter_len - 1
   - SAME: signal_len
   
   Domain selection (AUTO):
   - Uses frequency domain if filter_len > 128 or batch ≥ 10
   - Otherwise uses spatial domain
   
   Batch modes supported:
   - ONE-to-ONE: signal[N], filter[M] → out[N+M-1 or N]
   - MANY-to-ONE: signal[N×B], filter[M] → out[(N+M-1)×B]
   - ONE-to-MANY: signal[N], filter[M×B] → out[(N+M-1)×B]
   - MANY-to-MANY: signal[N×B], filter[M×B] → out[(N+M-1)×B]
   
   Use cases:
   - 1D signal filtering (audio, time series)
   - Edge detection (Sobel, Prewitt)
   - Smoothing (Gaussian, moving average)
   
   Returns:
   ArrayFire error code"
  "af_convolve1" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/int] ::mem/int)

;; af_err af_convolve2(af_array *out, const af_array signal, const af_array filter, const af_conv_mode mode, af_conv_domain domain)
(defcfn af-convolve2
  "2D convolution on two-dimensional signals.
   
   Performs convolution between a signal and a filter in 2 dimensions.
   
   Parameters:
   - out: out pointer for result array
   - signal: input signal array (2D or higher)
   - filter: filter array (will be flipped for convolution)
   - mode: convolution mode (0=EXPAND, 1=SAME)
   - domain: convolution domain (0=AUTO, 1=SPATIAL, 2=FREQ)
   
   Convolution formula: out[i,j] = Σₘ Σₙ signal[i+m,j+n] × filter[H-1-m,W-1-n]
   where H×W is the filter size.
   
   Output sizes:
   - EXPAND: [sig_h+filt_h-1, sig_w+filt_w-1]
   - SAME: [sig_h, sig_w]
   
   Domain selection (AUTO):
   - Uses frequency domain if filter > 11×11 or batch ≥ 10
   - Otherwise uses spatial domain
   
   Batch modes: Same as 1D convolution
   
   Special cases:
   - If either dimension is 1D, delegates to af_convolve1
   - Consider af_convolve2_sep for separable filters (more efficient)
   
   Use cases:
   - Image filtering (blur, sharpen, edge detection)
   - Feature extraction (convolutional neural networks)
   - Template matching
   - Motion detection
   
   Returns:
   ArrayFire error code"
  "af_convolve2" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/int] ::mem/int)

;; af_err af_convolve3(af_array *out, const af_array signal, const af_array filter, const af_conv_mode mode, af_conv_domain domain)
(defcfn af-convolve3
  "3D convolution on three-dimensional signals.
   
   Performs convolution between a signal and a filter in 3 dimensions.
   
   Parameters:
   - out: out pointer for result array
   - signal: input signal array (3D or higher)
   - filter: filter array (will be flipped for convolution)
   - mode: convolution mode (0=EXPAND, 1=SAME)
   - domain: convolution domain (0=AUTO, 1=SPATIAL, 2=FREQ)
   
   Convolution formula: out[i,j,k] = Σₗ Σₘ Σₙ signal[i+l,j+m,k+n] × filter[D-1-l,H-1-m,W-1-n]
   where D×H×W is the filter size.
   
   Output sizes:
   - EXPAND: [sig_d+filt_d-1, sig_h+filt_h-1, sig_w+filt_w-1]
   - SAME: [sig_d, sig_h, sig_w]
   
   Domain selection (AUTO): Similar heuristics to 2D
   
   Batch modes: Same as 1D/2D convolution
   
   Special cases:
   - If dimensions < 3, delegates to af_convolve2
   
   Use cases:
   - 3D medical imaging (MRI, CT scans)
   - Video processing (temporal filtering)
   - Volumetric data analysis
   - 3D CNNs for video/volume classification
   
   Returns:
   ArrayFire error code"
  "af_convolve3" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/int] ::mem/int)

;; Separable convolution

;; af_err af_convolve2_sep(af_array *out, const af_array col_filter, const af_array row_filter, const af_array signal, const af_conv_mode mode)
(defcfn af-convolve2-sep
  "Separable 2D convolution using column and row filters.
   
   Performs 2D convolution as two sequential 1D convolutions, which is much
   more efficient when the 2D filter is separable (can be factored into
   column and row components).
   
   Parameters:
   - out: out pointer for result array
   - col_filter: 1D column filter (vertical direction)
   - row_filter: 1D row filter (horizontal direction)
   - signal: input signal array (2D or higher)
   - mode: convolution mode (0=EXPAND, 1=SAME)
   
   Algorithm:
   1. Convolve signal with col_filter vertically
   2. Convolve result with row_filter horizontally
   
   This is equivalent to convolving with col_filter ⊗ row_filter but much faster.
   
   Complexity:
   - Standard 2D: O(M×N×P×Q) where signal is M×N, filter is P×Q
   - Separable: O(M×N×(P+Q))
   
   Output sizes:
   - EXPAND: [sig_h+col_len-1, sig_w+row_len-1]
   - SAME: [sig_h, sig_w]
   
   Batch modes supported:
   - ONE-to-ONE: Standard separable convolution
   - MANY-to-ONE: Multiple signals with same filters
   
   Note: Does NOT support ONE-to-MANY or MANY-to-MANY batch modes
   
   Special cases:
   - If either filter is scalar, it acts as a constant multiplier
   - Both filters must be 1D vectors
   - Signal must be at least 2D
   
   Common separable filters:
   - Gaussian blur: e^(-x²/2σ²) ⊗ e^(-y²/2σ²)
   - Sobel X: [1 2 1]ᵀ ⊗ [1 0 -1]
   - Sobel Y: [1 0 -1]ᵀ ⊗ [1 2 1]
   - Box filter: [1 1 ... 1]ᵀ ⊗ [1 1 ... 1]
   
   Use cases:
   - Fast Gaussian blur for image smoothing
   - Edge detection (Sobel, Scharr operators)
   - Efficient box filtering
   - Any separable kernel operation
   
   Returns:
   ArrayFire error code"
  "af_convolve2_sep" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; Neural network convolution with stride, padding, dilation

;; af_err af_convolve2_nn(af_array *out, const af_array signal, const af_array filter, const unsigned stride_dims, const dim_t *strides, const unsigned padding_dims, const dim_t *paddings, const unsigned dilation_dims, const dim_t *dilations)
(defcfn af-convolve2-nn
  "2D convolution for neural networks with stride, padding, and dilation.
   
   Performs 2D convolution optimized for deep learning applications, supporting
   common CNN operations like stride, padding, and dilated (atrous) convolution.
   
   Parameters:
   - out: out pointer for result array
   - signal: input signal array [d0 d1 d2 Ns] where Ns = number of signals
   - filter: filter array [d0 d1 d2 Nf] where Nf = number of filters
   - stride_dims: number of stride dimensions (typically 2)
   - strides: array of stride values for each dimension
   - padding_dims: number of padding dimensions (typically 2)
   - paddings: array of padding values for each dimension
   - dilation_dims: number of dilation dimensions (typically 2)
   - dilations: array of dilation values for each dimension
   
   Output dimensions: [d0' d1' Nf Ns] where:
   - d0' = floor((d0 + 2×pad0 - dil0×(f0-1) - 1) / stride0) + 1
   - d1' = floor((d1 + 2×pad1 - dil1×(f1-1) - 1) / stride1) + 1
   
   Batch processing:
   - Signals: [d0 d1 d2 Ns] - Ns different input images
   - Filters: [d0 d1 d2 Nf] - Nf different filters
   - Output: [d0' d1' Nf Ns] - Each filter applied to each signal
   
   Parameters explained:
   
   **Stride**: Step size for filter movement
   - stride=1: Filter moves 1 pixel at a time (standard convolution)
   - stride=2: Filter moves 2 pixels at a time (downsampling)
   - Reduces output size, commonly used in pooling layers
   
   **Padding**: Border extension around input
   - padding=0: No padding (valid convolution)
   - padding=(filter_size-1)/2: Same convolution (output size = input size)
   - Prevents information loss at borders
   
   **Dilation**: Spacing between filter elements
   - dilation=1: Standard convolution (no dilation)
   - dilation=2: Filter elements separated by 1 gap
   - Increases receptive field without increasing parameters
   - Also called \"atrous convolution\" or \"dilated convolution\"
   
   Dilated convolution formula:
   out[i,j] = Σₘ Σₙ signal[i+m×dilation, j+n×dilation] × filter[m,n]
   
   Constraints:
   - stride_dims, padding_dims, dilation_dims ∈ {1, 2}
   - Signal and filter must have same d2 dimension
   - Signal: [d0 d1 d2 Ns], Filter: [d0 d1 d2 Nf]
   
   Common use cases:
   - **Stride=2, padding=1**: Downsampling in CNNs (reduces spatial dimensions)
   - **Stride=1, padding='same'**: Preserves spatial dimensions
   - **Dilation=2,4,8**: Multi-scale feature extraction (DeepLab, WaveNet)
   
   Example applications:
   - Convolutional layers in deep neural networks
   - Semantic segmentation (dilated convolutions)
   - Object detection (stride for multi-scale features)
   - Style transfer (different receptive fields)
   
   Note: This function does NOT flip the filter (unlike standard convolution).
   It performs correlation, which is the standard operation in CNNs.
   
   See also: af_convolve2_gradient_nn for backpropagation
   
   Returns:
   ArrayFire error code"
  "af_convolve2_nn" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/pointer ::mem/int ::mem/pointer ::mem/int ::mem/pointer] ::mem/int)

;; FFT-based convolution

;; af_err af_fft_convolve1(af_array *out, const af_array signal, const af_array filter, const af_conv_mode mode)
(defcfn af-fft-convolve1
  "1D convolution using Fast Fourier Transform.
   
   Performs 1D convolution in the frequency domain using FFT. This is more
   efficient than spatial domain convolution for large filters.
   
   Parameters:
   - out: out pointer for result array
   - signal: input signal array
   - filter: filter array (will be used in frequency domain)
   - mode: convolution mode (0=EXPAND, 1=SAME)
   
   Algorithm:
   1. Pad signal and filter to size N = signal_len + filter_len - 1
   2. Compute FFT of both padded arrays
   3. Multiply point-wise in frequency domain: FFT(signal) × FFT(filter)
   4. Compute inverse FFT to get result
   5. Crop to desired size based on mode
   
   Complexity:
   - Spatial domain: O(N×M) where N=signal length, M=filter length
   - Frequency domain: O(N log N + M log M)
   
   Performance considerations:
   - Faster than spatial domain when M > 128
   - Always used when batch ≥ 10
   - Overhead for small filters due to FFT setup
   
   Convolution theorem:
   conv(f, g) = IFFT(FFT(f) × FFT(g))
   
   Type handling:
   - Integer types are converted to float for computation
   - Output type matches input type after conversion back
   
   Use cases:
   - Large filter convolution (> 128 elements)
   - Batch processing of multiple signals
   - Audio processing with long impulse responses
   - Signal analysis in frequency domain
   
   Returns:
   ArrayFire error code"
  "af_fft_convolve1" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_fft_convolve2(af_array *out, const af_array signal, const af_array filter, const af_conv_mode mode)
(defcfn af-fft-convolve2
  "2D convolution using Fast Fourier Transform.
   
   Performs 2D convolution in the frequency domain using 2D FFT.
   
   Parameters:
   - out: out pointer for result array
   - signal: input signal array (2D or higher)
   - filter: filter array (will be used in frequency domain)
   - mode: convolution mode (0=EXPAND, 1=SAME)
   
   Algorithm:
   1. Pad signal and filter to size [H+fh-1, W+fw-1]
   2. Compute 2D FFT of both padded arrays
   3. Multiply point-wise in frequency domain
   4. Compute inverse 2D FFT to get result
   5. Crop to desired size based on mode
   
   Complexity:
   - Spatial domain: O(H×W×fh×fw)
   - Frequency domain: O(H×W×log(H×W))
   
   Performance considerations:
   - Faster than spatial domain for filters > 11×11
   - Always used when batch ≥ 10
   - Particularly efficient for large batches
   
   Special cases:
   - If either dimension < 2, falls back to af_fft_convolve1
   
   Type handling:
   - Integer types converted to float/cfloat
   - Complex inputs: cfloat or cdouble throughout
   - Real inputs: float or double
   
   Use cases:
   - Large kernel image filtering
   - Batch image processing
   - Template matching with many templates
   - Astronomical image processing
   
   Returns:
   ArrayFire error code"
  "af_fft_convolve2" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_fft_convolve3(af_array *out, const af_array signal, const af_array filter, const af_conv_mode mode)
(defcfn af-fft-convolve3
  "3D convolution using Fast Fourier Transform.
   
   Performs 3D convolution in the frequency domain using 3D FFT.
   
   Parameters:
   - out: out pointer for result array
   - signal: input signal array (3D or higher)
   - filter: filter array (will be used in frequency domain)
   - mode: convolution mode (0=EXPAND, 1=SAME)
   
   Algorithm:
   1. Pad signal and filter to size [D+fd-1, H+fh-1, W+fw-1]
   2. Compute 3D FFT of both padded arrays
   3. Multiply point-wise in frequency domain
   4. Compute inverse 3D FFT to get result
   5. Crop to desired size based on mode
   
   Complexity:
   - Spatial domain: O(D×H×W×fd×fh×fw)
   - Frequency domain: O(D×H×W×log(D×H×W))
   
   Performance considerations:
   - Faster than spatial domain for large filters
   - Always used when batch ≥ 10
   - Very efficient for volumetric data with large kernels
   
   Special cases:
   - If dimensions < 3, falls back to af_fft_convolve2
   
   Use cases:
   - 3D medical image filtering
   - Volumetric data processing
   - Video temporal filtering
   - 4D (3D+time) data convolution
   
   Returns:
   ArrayFire error code"
  "af_fft_convolve3" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; Neural network gradient computation

;; af_err af_convolve2_gradient_nn(af_array *out, const af_array incoming_gradient, const af_array original_signal, const af_array original_filter, const af_array convolved_output, const unsigned stride_dims, const dim_t *strides, const unsigned padding_dims, const dim_t *paddings, const unsigned dilation_dims, const dim_t *dilations, af_conv_gradient_type grad_type)
(defcfn af-convolve2-gradient-nn
  "Calculate backward pass gradient of 2D convolution for neural networks.
   
   This function computes the gradient with respect to the output of the
   af_convolve2_nn function. It's the essential backpropagation operation
   for training convolutional neural networks.
   
   Gradient Types (af_conv_gradient_type):
   - 0 (AF_CONV_GRADIENT_DEFAULT): Same as FILTER
   - 1 (AF_CONV_GRADIENT_FILTER): Gradient wrt filter/weights (∂L/∂W)
   - 2 (AF_CONV_GRADIENT_DATA): Gradient wrt input signal/data (∂L/∂X)
   - 3 (AF_CONV_GRADIENT_BIAS): Gradient wrt bias term (∂L/∂b)
   
   Parameters:
   - out: Output pointer for gradient array
   - incoming_gradient: Gradient from next layer (∂L/∂output)
     * Dimensions: same as convolved_output
   - original_signal: Input signal from forward pass [d0 × d1 × d2 × Ns]
   - original_filter: Filter from forward pass [d0 × d1 × d2 × Nf]
   - convolved_output: Output from forward pass convolution
   - stride_dims: Number of stride dimensions (typically 2)
   - strides: Array of stride values (must match forward pass)
   - padding_dims: Number of padding dimensions (typically 2)
   - paddings: Array of padding values (must match forward pass)
   - dilation_dims: Number of dilation dimensions (typically 2)
   - dilations: Array of dilation values (must match forward pass)
   - grad_type: Type of gradient to compute (0=DEFAULT, 1=FILTER, 2=DATA, 3=BIAS)
   
   Output Dimensions:
   - GRADIENT_FILTER: Same as original_filter [d0 × d1 × d2 × Nf]
   - GRADIENT_DATA: Same as original_signal [d0 × d1 × d2 × Ns]
   - GRADIENT_BIAS: [1 × 1 × Nf × 1]
   
   Mathematical Context:
   
   During the forward pass:
     output = convolve2_nn(signal, filter, stride, padding, dilation)
   
   During the backward pass (this function):
     Given ∂L/∂output (incoming_gradient), compute:
     - ∂L/∂filter (GRADIENT_FILTER): Used to update filter weights
     - ∂L/∂signal (GRADIENT_DATA): Backpropagated to previous layer
     - ∂L/∂bias (GRADIENT_BIAS): If bias exists
   
   Gradient Computation:
   
   1. **Filter Gradient** (type=1):
      For each filter f and input channel c:
        ∂L/∂W[f,c] = Σ_batch conv2d(signal[:,:,c,n], incoming_gradient[:,:,f,n])
      
      Accumulates gradients across the batch dimension.
      Used to update convolutional layer weights during training.
   
   2. **Data Gradient** (type=2):
      For each batch n and channel c:
        ∂L/∂X[:,:,c,n] = conv2d_transpose(incoming_gradient[:,:,:,n], filter[:,:,c,:])
      
      Transposed convolution (deconvolution) operation.
      Propagates gradients backward through the network.
   
   3. **Bias Gradient** (type=3):
      For each filter f:
        ∂L/∂b[f] = Σ_{spatial,batch} incoming_gradient[:,:,f,:]
      
      Sum gradients over all spatial positions and batch samples.
   
   Typical CNN Training Loop:
   
   1. Forward pass:
      ```clojure
      (let [output (af-convolve2-nn signal filter 2 stride 2 padding 2 dilation)]
        ;; Compute loss...
        )
      ```
   
   2. Compute loss gradient:
      ```clojure
      (let [loss-gradient (compute-loss-gradient output target)]
        ;; Backpropagate...
        )
      ```
   
   3. Backward pass (this function):
      ```clojure
      ;; Get filter gradient for weight update
      (let [filter-grad (af-convolve2-gradient-nn
                          out loss-gradient signal filter output
                          2 stride 2 padding 2 dilation
                          1)]  ; GRADIENT_FILTER
        ;; Update: filter = filter - learning-rate × filter-grad
        )
      
      ;; Get data gradient for previous layer
      (let [data-grad (af-convolve2-gradient-nn
                        out loss-gradient signal filter output
                        2 stride 2 padding 2 dilation
                        2)]  ; GRADIENT_DATA
        ;; Propagate to previous layer
        )
      ```
   
   Performance Considerations:
   - Filter gradient often slower (accumulation across batch)
   - Data gradient similar to transposed convolution
   - Larger batches amortize overhead
   
   Constraints:
   - All parameters (stride, padding, dilation) must match forward pass
   - Signal, filter, output dimensions must be consistent
   
   Common Pitfalls:
   - Dimension mismatch between forward and backward pass
   - Forgetting to match stride/padding/dilation
   - Numerical instability with large/small gradients
   
   Returns:
   ArrayFire error code (0 = AF_SUCCESS)
   
   See Also:
   - af_convolve2_nn: Forward pass convolution"
  "af_convolve2_gradient_nn"
  [::mem/pointer    ; out
   ::mem/pointer    ; incoming_gradient
   ::mem/pointer    ; original_signal
   ::mem/pointer    ; original_filter
   ::mem/pointer    ; convolved_output
   ::mem/int        ; stride_dims
   ::mem/pointer    ; strides
   ::mem/int        ; padding_dims
   ::mem/pointer    ; paddings
   ::mem/int        ; dilation_dims
   ::mem/pointer    ; dilations
   ::mem/int]       ; grad_type
  ::mem/int)
