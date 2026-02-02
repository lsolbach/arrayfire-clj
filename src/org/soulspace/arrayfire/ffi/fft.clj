(ns org.soulspace.arrayfire.ffi.fft
  "Bindings for the ArrayFire Fast Fourier Transform (FFT) functions.
   
   The Fast Fourier Transform is a fundamental algorithm in signal processing
   that computes the Discrete Fourier Transform (DFT) of a signal efficiently.
   ArrayFire provides highly optimized GPU-accelerated FFT implementations
   for 1D, 2D, and 3D transforms in both forward and inverse directions.
   
   Mathematical Foundation:
   
   The Discrete Fourier Transform converts a signal from the time/spatial
   domain to the frequency domain:
   
   Forward DFT:
   X[k] = Σ(n=0 to N-1) x[n] * exp(-2πi * k * n / N)
   
   Inverse DFT:
   x[n] = (1/N) * Σ(k=0 to N-1) X[k] * exp(2πi * k * n / N)
   
   Where:
   - x[n] is the input signal in time/spatial domain
   - X[k] is the output in frequency domain
   - N is the signal length
   - i is the imaginary unit (√-1)
   - k is the frequency bin index
   - n is the time/spatial sample index
   
   Normalization:
   
   ArrayFire FFT functions include a norm_factor parameter that scales
   the output. Standard conventions:
   
   1. Forward: norm_factor = 1.0 (no normalization)
      Inverse: norm_factor = 1/N (divide by signal length)
   
   2. Symmetric: Both forward and inverse use norm_factor = 1/√N
   
   3. Custom: Any scaling factor for specialized applications
   
   The default behavior in ArrayFire's high-level functions:
   - Forward FFT: No normalization (factor = 1.0)
   - Inverse FFT: Automatic normalization (factor = 1/N)
   
   Transform Types:
   
   1. Complex-to-Complex (C2C):
      - Most general form
      - Input: Complex array
      - Output: Complex array
      - Functions: af_fft, af_fft2, af_fft3, af_ifft, af_ifft2, af_ifft3
   
   2. Real-to-Complex (R2C):
      - Optimized for real inputs
      - Input: Real array (float/double)
      - Output: Complex array (half spectrum + DC)
      - Functions: af_fft_r2c, af_fft2_r2c, af_fft3_r2c
      - Output size: (N/2 + 1) × M × P (first dimension reduced)
      - Exploits conjugate symmetry: X[N-k] = conj(X[k])
   
   3. Complex-to-Real (C2R):
      - Inverse of R2C
      - Input: Complex array (half spectrum)
      - Output: Real array
      - Functions: af_fft_c2r, af_fft2_c2r, af_fft3_c2r
      - Requires is_odd flag to determine output length
   
   In-Place vs Out-of-Place:
   
   1. Out-of-Place:
      - Separate input and output arrays
      - Original input preserved
      - Functions: af_fft, af_fft2, af_fft3, etc.
      - More memory usage but safer
   
   2. In-Place:
      - Transform overwrites input array
      - Memory efficient
      - Functions: af_fft_inplace, af_fft2_inplace, etc.
      - Input must be complex type
   
   Padding and Truncation:
   
   All FFT functions support optional padding or truncation via
   output dimension parameters (odim0, odim1, odim2):
   
   - odim = 0: Use input dimension (no change)
   - odim < input: Truncate signal
   - odim > input: Zero-pad signal
   
   Padding to power-of-2 sizes improves performance:
   - 256, 512, 1024, 2048, 4096, etc.
   - Use: odim = next_power_of_2(input_size)
   
   Common Applications:
   
   1. Frequency Analysis:
      - Identify frequency components in signals
      - Spectral analysis of audio, vibration, etc.
      - Power spectral density estimation
   
   2. Filtering:
      - Apply frequency-domain filters
      - Lowpass, highpass, bandpass filters
      - FFT → multiply by filter → IFFT
   
   3. Convolution:
      - Fast convolution via multiplication in frequency domain
      - Conv(x, h) = IFFT(FFT(x) * FFT(h))
      - Faster than time-domain for large signals
   
   4. Correlation:
      - Cross-correlation for template matching
      - Auto-correlation for periodicity detection
   
   5. Image Processing:
      - 2D FFT for image filtering
      - Phase correlation for image registration
      - Texture analysis
   
   6. Compression:
      - JPEG uses DCT (related to DFT)
      - MP3 uses MDCT
      - Remove high-frequency components
   
   7. Differential Equations:
      - Solve PDEs in frequency domain
      - Spectral methods
   
   Performance Characteristics:
   
   1. Complexity:
      - Naive DFT: O(N²)
      - FFT: O(N log N)
      - Massive speedup for large N
   
   2. GPU Optimization:
      - Highly parallel algorithm
      - Excellent GPU utilization
      - Batched transforms for efficiency
   
   3. Size Considerations:
      - Power-of-2 sizes: Best performance
      - Prime sizes: Slower (fallback algorithms)
      - Mixed-radix: Moderate performance
   
   4. Memory:
      - Out-of-place: 2× memory (input + output)
      - In-place: 1× memory (complex arrays)
      - R2C/C2R: ~1.5× memory (reduced output)
   
   Workflow Patterns:
   
   1. Basic Forward FFT:
   ```clojure
   ;; Time domain → Frequency domain
   (let [signal (create-array [1024])  ; Real or complex signal
         spectrum-ptr (mem/alloc-instance ::mem/pointer)]
     (af-fft spectrum-ptr signal 1.0 0)
     (let [spectrum (mem/read-ptr spectrum-ptr)]
       ;; Analyze frequency content
       (process-spectrum spectrum)
       (af-release-array spectrum)))
   ```
   
   2. Forward-Inverse Round-Trip:
   ```clojure
   ;; Verify transform accuracy
   (let [signal (create-array [1024])
         fwd-ptr (mem/alloc-instance ::mem/pointer)
         inv-ptr (mem/alloc-instance ::mem/pointer)]
     ;; Forward: time → frequency
     (af-fft fwd-ptr signal 1.0 0)
     (let [spectrum (mem/read-ptr fwd-ptr)]
       ;; Inverse: frequency → time
       (af-ifft inv-ptr spectrum (/ 1.0 1024.0) 0)
       (let [reconstructed (mem/read-ptr inv-ptr)]
         ;; Should match original signal
         (compare-arrays signal reconstructed))))
   ```
   
   3. Frequency Domain Filtering:
   ```clojure
   ;; Apply lowpass filter
   (defn lowpass-filter [signal cutoff]
     (let [n (get-dim0 signal)
           ;; Forward FFT
           spec-ptr (mem/alloc-instance ::mem/pointer)]
       (af-fft spec-ptr signal 1.0 0)
       (let [spectrum (mem/read-ptr spec-ptr)
             ;; Create filter (1 below cutoff, 0 above)
             filter (create-filter n cutoff)
             ;; Multiply in frequency domain
             filtered (af-mul spectrum filter)
             ;; Inverse FFT
             out-ptr (mem/alloc-instance ::mem/pointer)]
         (af-ifft out-ptr filtered (/ 1.0 n) 0)
         (let [result (mem/read-ptr out-ptr)]
           ;; Cleanup
           (af-release-array spectrum)
           (af-release-array filtered)
           result))))
   ```
   
   4. Real-to-Complex Optimization:
   ```clojure
   ;; Efficient FFT for real signals
   (defn fft-real [signal]
     (let [n (get-dim0 signal)
           ;; Use R2C transform (2× faster, half memory)
           spec-ptr (mem/alloc-instance ::mem/pointer)]
       (af-fft-r2c spec-ptr signal 1.0 0)
       (let [spectrum (mem/read-ptr spec-ptr)]
         ;; spectrum has size (n/2 + 1)
         spectrum)))
   
   (defn ifft-real [spectrum is-odd]
     (let [out-ptr (mem/alloc-instance ::mem/pointer)
           n (get-original-length spectrum is-odd)]
       (af-fft-c2r out-ptr spectrum (/ 1.0 n) is-odd)
       (mem/read-ptr out-ptr)))
   ```
   
   5. 2D Image Processing:
   ```clojure
   ;; 2D FFT for image filtering
   (defn fft-image-filter [image filter-fn]
     (let [[h w] (get-dims-2d image)
           ;; 2D forward FFT
           spec-ptr (mem/alloc-instance ::mem/pointer)]
       (af-fft2 spec-ptr image 1.0 0 0)
       (let [spectrum (mem/read-ptr spec-ptr)
             ;; Apply frequency filter
             filtered (filter-fn spectrum)
             ;; 2D inverse FFT
             out-ptr (mem/alloc-instance ::mem/pointer)]
         (af-ifft2 out-ptr filtered (/ 1.0 h w) 0 0)
         (let [result (mem/read-ptr out-ptr)]
           (af-release-array spectrum)
           (af-release-array filtered)
           result))))
   ```
   
   6. Batched Processing:
   ```clojure
   ;; Process multiple signals efficiently
   (defn fft-batch [signals]
     ;; Stack signals along 3rd or 4th dimension
     ;; ArrayFire automatically batches
     (let [batch (af-join signals 2)  ; Stack along dim 2
           spec-ptr (mem/alloc-instance ::mem/pointer)]
       ;; FFT processes all signals in parallel
       (af-fft spec-ptr batch 1.0 0)
       (mem/read-ptr spec-ptr)))
   ```
   
   Best Practices:
   
   1. Size Selection:
      - Pad to next power-of-2 for best performance
      - Use (af-nextpow2 n) to find optimal size
      - Balance speed vs memory usage
   
   2. Normalization:
      - Forward: norm_factor = 1.0
      - Inverse: norm_factor = 1.0 / (N * M * P)
      - Or use symmetric: 1.0 / sqrt(N * M * P) for both
   
   3. Memory Management:
      - Release intermediate arrays promptly
      - Use in-place when possible
      - Consider batching for multiple transforms
   
   4. Real Signals:
      - Always use R2C/C2R for real signals
      - 2× faster and half memory vs C2C
      - Remember conjugate symmetry
   
   5. Precision:
      - Single precision (c32/f32) usually sufficient
      - Double precision (c64/f64) if needed
      - GPU performance difference significant
   
   6. Error Handling:
      - Check return codes
      - Validate input dimensions
      - Ensure complex type for in-place
   
   Common Pitfalls:
   
   1. Forgetting Normalization:
      ```clojure
      ;; WRONG: No normalization on inverse
      (af-ifft out spectrum 1.0 0)  ; Result scaled by N!
      
      ;; CORRECT: Normalize inverse
      (af-ifft out spectrum (/ 1.0 n) 0)
      ```
   
   2. Using C2C for Real Signals:
      ```clojure
      ;; INEFFICIENT: Convert to complex first
      (let [complex-sig (af-cplx real-sig)]
        (af-fft out complex-sig 1.0 0))
      
      ;; EFFICIENT: Use R2C directly
      (af-fft-r2c out real-sig 1.0 0)
      ```
   
   3. Incorrect C2R Output Size:
      ```clojure
      ;; WRONG: Forgetting is_odd flag
      (af-fft-c2r out spectrum 1.0 false)  ; Wrong size!
      
      ;; CORRECT: Track original size
      (af-fft-c2r out spectrum 1.0 is-odd)
      ```
   
   4. In-Place on Real Arrays:
      ```clojure
      ;; WRONG: Can't in-place transform real array
      (af-fft-inplace real-signal 1.0)  ; ERROR!
      
      ;; CORRECT: Use out-of-place for real
      (af-fft out real-signal 1.0 0)
      ```
   
   Advanced Topics:
   
   1. Windowing:
      - Apply window function before FFT
      - Reduces spectral leakage
      - Common: Hann, Hamming, Blackman
   
   2. Overlap-Add/Save:
      - Process long signals in chunks
      - Handle filtering with overlap
      - Efficient for streaming
   
   3. Zero-Phase Filtering:
      - Forward-backward filtering
      - No phase distortion
      - Double filtering effect
   
   4. Chirp Z-Transform:
      - Arbitrary frequency resolution
      - Zoom FFT for specific bands
      - Built on top of FFT
   
   5. Short-Time FFT (STFT):
      - Time-frequency analysis
      - Spectrograms
      - Overlapping windows
   
   6. Multidimensional FFT:
      - 3D FFT for volumetric data
      - Separable transforms
      - Medical imaging, seismology
   
   Related Functions:
   - af_convolve: Time-domain convolution
   - af_fft_convolve: FFT-based convolution
   - af_approx1/2: Interpolation in frequency domain"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; Forward FFT functions

;; af_err af_fft(af_array *out, const af_array in, const double norm_factor, const dim_t odim0)
(defcfn af-fft
  "One-dimensional forward Fast Fourier Transform.
   
   Computes the 1D forward FFT of the input signal along the first dimension.
   Supports both real and complex inputs. Real inputs are automatically
   converted to complex internally.
   
   Parameters:
   
   - out: af_array* output spectrum
     * Complex array (c32 or c64)
     * Same dimensions as input (after padding/truncation)
     * Frequency domain representation
   
   - in: af_array input signal
     * Can be real (f32, f64) or complex (c32, c64)
     * 1D, 2D, 3D, or 4D array
     * FFT applied along first dimension only
     * Higher dimensions batched automatically
   
   - norm_factor: double normalization scale
     * Multiply output by this factor
     * Typical: 1.0 for forward transform
     * Use 1/N for inverse to normalize properly
     * Can be any value for custom scaling
   
   - odim0: dim_t output length along first dimension
     * 0: Use input size (no padding/truncation)
     * > input: Zero-pad signal (improves resolution)
     * < input: Truncate signal
     * Power-of-2 recommended for speed
   
   Returns:
   AF_SUCCESS (0) on success, error code otherwise:
   - AF_ERR_ARG: Invalid input array
   - AF_ERR_SIZE: Invalid output dimension
   - AF_ERR_TYPE: Unsupported data type
   - AF_ERR_NO_MEM: Insufficient GPU memory
   
   Usage:
   ```clojure
   ;; Basic 1D FFT
   (let [signal (create-array [1024])
         out-ptr (mem/alloc-instance ::mem/pointer)]
     (af-fft out-ptr signal 1.0 0)
     (let [spectrum (mem/read-ptr out-ptr)]
       ;; Process frequency domain data
       ...))
   
   ;; With padding to power-of-2
   (let [signal (create-array [1000])
         out-ptr (mem/alloc-instance ::mem/pointer)]
     (af-fft out-ptr signal 1.0 1024)  ; Pad to 1024
     ...)
   ```
   
   See Also:
   - af_ifft: Inverse 1D FFT
   - af_fft2: 2D FFT
   - af_fft_r2c: Optimized real-to-complex FFT"
  "af_fft" [::mem/pointer ::mem/pointer ::mem/double ::mem/long] ::mem/int)

;; af_err af_fft2(af_array *out, const af_array in, const double norm_factor, const dim_t odim0, const dim_t odim1)
(defcfn af-fft2
  "Two-dimensional forward Fast Fourier Transform.
   
   Computes the 2D forward FFT of the input along the first two dimensions.
   Commonly used for image processing and 2D signal analysis.
   
   Parameters:
   
   - out: af_array* output spectrum
   - in: af_array input signal (2D, 3D, or 4D)
   - norm_factor: double normalization scale (typically 1.0)
   - odim0: dim_t output length along first dimension (0 = no change)
   - odim1: dim_t output length along second dimension (0 = no change)
   
   Returns: AF_SUCCESS or error code
   
   Usage:
   ```clojure
   ;; 2D FFT for image
   (let [image (load-image \"photo.jpg\")
         out-ptr (mem/alloc-instance ::mem/pointer)]
     (af-fft2 out-ptr image 1.0 0 0)
     (let [spectrum (mem/read-ptr out-ptr)]
       ;; Apply frequency domain filter
       ...))
   ```
   
   See Also:
   - af_ifft2: Inverse 2D FFT
   - af_fft2_r2c: Optimized 2D real-to-complex FFT"
  "af_fft2" [::mem/pointer ::mem/pointer ::mem/double ::mem/long ::mem/long] ::mem/int)

;; af_err af_fft3(af_array *out, const af_array in, const double norm_factor, const dim_t odim0, const dim_t odim1, const dim_t odim2)
(defcfn af-fft3
  "Three-dimensional forward Fast Fourier Transform.
   
   Computes the 3D forward FFT along the first three dimensions. Used
   for volumetric data processing, 3D medical imaging, and scientific
   simulations.
   
   Parameters:
   
   - out: af_array* output spectrum
   - in: af_array input signal (3D or 4D)
   - norm_factor: double normalization (typically 1.0)
   - odim0: dim_t output length along dim 0
   - odim1: dim_t output length along dim 1
   - odim2: dim_t output length along dim 2
   
   Returns: AF_SUCCESS or error code
   
   See Also:
   - af_ifft3: Inverse 3D FFT
   - af_fft3_r2c: Optimized 3D real-to-complex FFT"
  "af_fft3" [::mem/pointer ::mem/pointer ::mem/double ::mem/long ::mem/long ::mem/long] ::mem/int)

;; Inverse FFT functions

;; af_err af_ifft(af_array *out, const af_array in, const double norm_factor, const dim_t odim0)
(defcfn af-ifft
  "One-dimensional inverse Fast Fourier Transform.
   
   Computes the 1D inverse FFT to convert from frequency domain back to
   time/spatial domain. This is the inverse operation of af_fft.
   
   Parameters:
   
   - out: af_array* output signal (time domain)
   - in: af_array input spectrum (frequency domain, complex)
   - norm_factor: double normalization scale
     * Typical: 1.0/N where N is signal length
     * This compensates for forward transform scaling
     * Result: forward(inverse(x)) = x
   - odim0: dim_t output length (0 = use input size)
   
   Returns: AF_SUCCESS or error code
   
   Usage:
   ```clojure
   ;; Round-trip transform
   (let [signal (create-array [1024])
         fwd-ptr (mem/alloc-instance ::mem/pointer)]
     ;; Forward
     (af-fft fwd-ptr signal 1.0 0)
     (let [spectrum (mem/read-ptr fwd-ptr)
           inv-ptr (mem/alloc-instance ::mem/pointer)]
       ;; Inverse with normalization
       (af-ifft inv-ptr spectrum (/ 1.0 1024.0) 0)
       (let [reconstructed (mem/read-ptr inv-ptr)]
         ;; reconstructed ≈ signal
         ...)))
   ```
   
   See Also:
   - af_fft: Forward 1D FFT
   - af_fft_c2r: Complex-to-real inverse FFT"
  "af_ifft" [::mem/pointer ::mem/pointer ::mem/double ::mem/long] ::mem/int)

;; af_err af_ifft2(af_array *out, const af_array in, const double norm_factor, const dim_t odim0, const dim_t odim1)
(defcfn af-ifft2
  "Two-dimensional inverse Fast Fourier Transform.
   
   Parameters:
   - out: af_array* output signal
   - in: af_array input spectrum (complex)
   - norm_factor: double (typical: 1.0/(N*M))
   - odim0: dim_t output dim 0
   - odim1: dim_t output dim 1
   
   Returns: AF_SUCCESS or error code
   
   See Also:
   - af_fft2: Forward 2D FFT"
  "af_ifft2" [::mem/pointer ::mem/pointer ::mem/double ::mem/long ::mem/long] ::mem/int)

;; af_err af_ifft3(af_array *out, const af_array in, const double norm_factor, const dim_t odim0, const dim_t odim1, const dim_t odim2)
(defcfn af-ifft3
  "Three-dimensional inverse Fast Fourier Transform.
   
   Parameters:
   - out: af_array* output signal
   - in: af_array input spectrum (complex)
   - norm_factor: double (typical: 1.0/(N*M*P))
   - odim0, odim1, odim2: dim_t output dimensions
   
   Returns: AF_SUCCESS or error code
   
   See Also:
   - af_fft3: Forward 3D FFT"
  "af_ifft3" [::mem/pointer ::mem/pointer ::mem/double ::mem/long ::mem/long ::mem/long] ::mem/int)

;; In-place FFT functions

;; af_err af_fft_inplace(af_array in, const double norm_factor)
(defcfn af-fft-inplace
  "One-dimensional forward FFT in-place (overwrites input).
   
   Memory-efficient variant that transforms the array in-place without
   allocating a separate output array. The input array is modified.
   
   Parameters:
   
   - in: af_array input/output array
     * MUST be complex type (c32 or c64)
     * Overwritten with transform result
     * FFT applied along first dimension
   
   - norm_factor: double normalization (typically 1.0)
   
   Returns: AF_SUCCESS or error code
   - AF_ERR_ARG: Input not complex type
   
   Usage:
   ```clojure
   ;; In-place transform (saves memory)
   (let [signal (create-complex-array [1024])]
     (af-fft-inplace signal 1.0)
     ;; signal now contains spectrum
     ...)
   ```
   
   Notes:
   - Input MUST be complex
   - Original data is lost
   - More memory efficient
   - Slightly faster than out-of-place
   
   See Also:
   - af_fft: Out-of-place variant
   - af_ifft_inplace: Inverse in-place"
  "af_fft_inplace" [::mem/pointer ::mem/double] ::mem/int)

;; af_err af_fft2_inplace(af_array in, const double norm_factor)
(defcfn af-fft2-inplace
  "Two-dimensional forward FFT in-place.
   
   Parameters:
   - in: af_array complex input/output
   - norm_factor: double
   
   Returns: AF_SUCCESS or error code
   
   See Also: af_fft2, af_ifft2_inplace"
  "af_fft2_inplace" [::mem/pointer ::mem/double] ::mem/int)

;; af_err af_fft3_inplace(af_array in, const double norm_factor)
(defcfn af-fft3-inplace
  "Three-dimensional forward FFT in-place.
   
   Parameters:
   - in: af_array complex input/output
   - norm_factor: double
   
   Returns: AF_SUCCESS or error code
   
   See Also: af_fft3, af_ifft3_inplace"
  "af_fft3_inplace" [::mem/pointer ::mem/double] ::mem/int)

;; af_err af_ifft_inplace(af_array in, const double norm_factor)
(defcfn af-ifft-inplace
  "One-dimensional inverse FFT in-place.
   
   Parameters:
   - in: af_array complex input/output
   - norm_factor: double (typical: 1.0/N)
   
   Returns: AF_SUCCESS or error code
   
   See Also: af_ifft, af_fft_inplace"
  "af_ifft_inplace" [::mem/pointer ::mem/double] ::mem/int)

;; af_err af_ifft2_inplace(af_array in, const double norm_factor)
(defcfn af-ifft2-inplace
  "Two-dimensional inverse FFT in-place.
   
   Parameters:
   - in: af_array complex input/output
   - norm_factor: double
   
   Returns: AF_SUCCESS or error code"
  "af_ifft2_inplace" [::mem/pointer ::mem/double] ::mem/int)

;; af_err af_ifft3_inplace(af_array in, const double norm_factor)
(defcfn af-ifft3-inplace
  "Three-dimensional inverse FFT in-place.
   
   Parameters:
   - in: af_array complex input/output
   - norm_factor: double
   
   Returns: AF_SUCCESS or error code"
  "af_ifft3_inplace" [::mem/pointer ::mem/double] ::mem/int)

;; Real-to-Complex FFT functions

;; af_err af_fft_r2c(af_array *out, const af_array in, const double norm_factor, const dim_t pad0)
(defcfn af-fft-r2c
  "One-dimensional real-to-complex forward FFT.
   
   Optimized FFT for real-valued input signals. Exploits the conjugate
   symmetry property of real signals' Fourier transforms, computing only
   the non-redundant half of the spectrum.
   
   For a real signal of length N, the output has length (N/2 + 1).
   The full spectrum can be reconstructed using conjugate symmetry:
   X[N-k] = conj(X[k])
   
   Parameters:
   
   - out: af_array* output spectrum
     * Complex array (c32 or c64)
     * Length: (pad0/2 + 1) along first dimension
     * Contains DC (index 0) and positive frequencies
     * Negative frequencies implicit (conjugate symmetric)
   
   - in: af_array input signal
     * MUST be real type (f32 or f64)
     * Any dimensions (FFT on first dimension)
   
   - norm_factor: double (typically 1.0)
   
   - pad0: dim_t output length before reduction
     * 0: Use input size
     * > input: Zero-pad for better resolution
     * Actual output: (pad0/2 + 1)
   
   Returns: AF_SUCCESS or error code
   
   Benefits:
   - 2× faster than complex FFT
   - 2× less memory usage
   - Exact same result (no approximation)
   
   Usage:
   ```clojure
   ;; Efficient FFT for real signal
   (let [signal (create-array [1024])  ; Real f32
         out-ptr (mem/alloc-instance ::mem/pointer)]
     (af-fft-r2c out-ptr signal 1.0 0)
     (let [spectrum (mem/read-ptr out-ptr)]
       ;; spectrum has size 513 = (1024/2 + 1)
       ...))
   ```
   
   See Also:
   - af_fft_c2r: Inverse (complex-to-real)
   - af_fft: General complex FFT"
  "af_fft_r2c" [::mem/pointer ::mem/pointer ::mem/double ::mem/long] ::mem/int)

;; af_err af_fft2_r2c(af_array *out, const af_array in, const double norm_factor, const dim_t pad0, const dim_t pad1)
(defcfn af-fft2-r2c
  "Two-dimensional real-to-complex forward FFT.
   
   2D version of R2C transform. Output dimensions:
   - First dimension: (pad0/2 + 1)
   - Second dimension: pad1
   - Higher dimensions: unchanged
   
   Parameters:
   - out: af_array* complex output
   - in: af_array real input
   - norm_factor: double
   - pad0: dim_t first dimension size
   - pad1: dim_t second dimension size
   
   Returns: AF_SUCCESS or error code
   
   See Also:
   - af_fft2_c2r: Inverse 2D C2R
   - af_fft_r2c: 1D version"
  "af_fft2_r2c" [::mem/pointer ::mem/pointer ::mem/double ::mem/long ::mem/long] ::mem/int)

;; af_err af_fft3_r2c(af_array *out, const af_array in, const double norm_factor, const dim_t pad0, const dim_t pad1, const dim_t pad2)
(defcfn af-fft3-r2c
  "Three-dimensional real-to-complex forward FFT.
   
   3D version of R2C transform. First dimension reduced to (pad0/2 + 1).
   
   Parameters:
   - out: af_array* complex output
   - in: af_array real input
   - norm_factor: double
   - pad0, pad1, pad2: dim_t dimensions
   
   Returns: AF_SUCCESS or error code
   
   See Also: af_fft3_c2r"
  "af_fft3_r2c" [::mem/pointer ::mem/pointer ::mem/double ::mem/long ::mem/long ::mem/long] ::mem/int)

;; Complex-to-Real FFT functions

;; af_err af_fft_c2r(af_array *out, const af_array in, const double norm_factor, const bool is_odd)
(defcfn af-fft-c2r
  "One-dimensional complex-to-real inverse FFT.
   
   Inverse of af_fft_r2c. Converts half-spectrum back to real signal.
   Requires is_odd flag to determine if output length should be even or odd.
   
   Parameters:
   
   - out: af_array* output signal
     * Real type (f32 or f64)
     * Length: 2*(input_len - 1) + is_odd
     * Example: input=513, is_odd=false → output=1024
     * Example: input=513, is_odd=true → output=1025
   
   - in: af_array input spectrum
     * Complex type (c32 or c64)
     * Half-spectrum from R2C transform
     * Length: (N/2 + 1) where N is original signal length
   
   - norm_factor: double normalization
     * Typical: 1.0 / original_signal_length
   
   - is_odd: bool output length parity
     * false: Even length (N = 2*(M-1))
     * true: Odd length (N = 2*(M-1) + 1)
     * M = input length
     * Must match original signal
   
   Returns: AF_SUCCESS or error code
   
   Usage:
   ```clojure
   ;; Round-trip R2C → C2R
   (let [signal (create-array [1024])  ; Even length
         spec-ptr (mem/alloc-instance ::mem/pointer)]
     ;; Forward R2C
     (af-fft-r2c spec-ptr signal 1.0 0)
     (let [spectrum (mem/read-ptr spec-ptr)  ; Length 513
           out-ptr (mem/alloc-instance ::mem/pointer)]
       ;; Inverse C2R
       (af-fft-c2r out-ptr spectrum (/ 1.0 1024.0) false)
       (let [reconstructed (mem/read-ptr out-ptr)]
         ;; reconstructed ≈ signal (length 1024)
         ...)))
   ```
   
   See Also:
   - af_fft_r2c: Forward real-to-complex
   - af_ifft: General inverse FFT"
  "af_fft_c2r" [::mem/pointer ::mem/pointer ::mem/double ::mem/int] ::mem/int)

;; af_err af_fft2_c2r(af_array *out, const af_array in, const double norm_factor, const bool is_odd)
(defcfn af-fft2-c2r
  "Two-dimensional complex-to-real inverse FFT.
   
   2D inverse of R2C. First dimension restored based on is_odd flag.
   
   Parameters:
   - out: af_array* real output
   - in: af_array complex half-spectrum
   - norm_factor: double
   - is_odd: bool first dimension parity
   
   Returns: AF_SUCCESS or error code
   
   See Also: af_fft2_r2c"
  "af_fft2_c2r" [::mem/pointer ::mem/pointer ::mem/double ::mem/int] ::mem/int)

;; af_err af_fft3_c2r(af_array *out, const af_array in, const double norm_factor, const bool is_odd)
(defcfn af-fft3-c2r
  "Three-dimensional complex-to-real inverse FFT.
   
   3D inverse of R2C. First dimension restored based on is_odd flag.
   
   Parameters:
   - out: af_array* real output
   - in: af_array complex half-spectrum
   - norm_factor: double
   - is_odd: bool first dimension parity
   
   Returns: AF_SUCCESS or error code
   
   See Also: af_fft3_r2c"
  "af_fft3_c2r" [::mem/pointer ::mem/pointer ::mem/double ::mem/int] ::mem/int)
