(ns org.soulspace.arrayfire.integration.unified-api.signal
  "Integration of ArrayFire signal processing FFI bindings with error
   handling and resource management on the JVM.
   
   Signal Processing in ArrayFire:
   
   ArrayFire provides GPU-accelerated signal processing operations including
   Fast Fourier Transforms (FFT), convolutions, interpolation/approximation,
   and digital filters. All operations are highly optimized for parallel
   execution on GPUs and multi-core CPUs.
   
   **Quick Start - FFT Example**:
   
   ```clojure
   (require '[org.soulspace.arrayfire.integration.signal :as signal])
   (require '[org.soulspace.arrayfire.integration.jvm-integration :as jvm])
   
   ;; Create a signal (e.g., sum of sine waves)
   (def time-signal (create-test-signal 1024))
   
   ;; Forward FFT to frequency domain
   (def freq-spectrum (fft time-signal))
   
   ;; Inverse FFT back to time domain
   (def reconstructed (ifft freq-spectrum))
   ```
   
   **Signal Processing Categories**:
   
   1. **Fourier Transforms** - Convert between time/spatial and frequency domains
      - Complex FFT: `fft`, `fft2`, `fft3`, `ifft`, `ifft2`, `ifft3`
      - Real-to-Complex: `fft-r2c`, `fft2-r2c`, `fft3-r2c`
      - Complex-to-Real: `fft-c2r`, `fft2-c2r`, `fft3-c2r`
      - In-place: `fft!`, `fft2!`, `fft3!`, `ifft!`, `ifft2!`, `ifft3!`
   
   2. **Convolution** - Signal filtering and correlation
      - Standard: `convolve1`, `convolve2`, `convolve3`
      - FFT-based: `fft-convolve1`, `fft-convolve2`, `fft-convolve3`
      - Separable: `convolve2-sep`
      - Neural network: `convolve2-nn`, `convolve2-gradient-nn`
   
   3. **Interpolation/Approximation** - Resample and interpolate signals
      - 1D: `approx1`, `approx1-uniform`
      - 2D: `approx2`, `approx2-uniform`
   
   4. **Digital Filters**
      - FIR: `fir` (Finite Impulse Response)
      - IIR: `iir` (Infinite Impulse Response)
      - Median: `medfilt`, `medfilt1`, `medfilt2`
   
   **Normalization in FFT**:
   
   FFT operations support custom normalization factors:
   - Forward FFT: typically norm=1.0 (no scaling)
   - Inverse FFT: typically norm=1.0/N (divide by length)
   - Symmetric: norm=1.0/sqrt(N) for both forward and inverse
   
   **Convolution Modes**:
   
   - `:default` (AF_CONV_DEFAULT) - Auto-select best algorithm
   - `:expand` (AF_CONV_EXPAND) - Output size = input + filter - 1
   - `:same` (AF_CONV_SAME) - Output size = input size
   
   **Interpolation Methods**:
   
   - `:nearest` (AF_INTERP_NEAREST) - Nearest neighbor
   - `:linear` (AF_INTERP_LINEAR) - Linear interpolation
   - `:bilinear` (AF_INTERP_BILINEAR) - Bilinear (2D)
   - `:cubic` (AF_INTERP_CUBIC) - Cubic spline
   - `:lower` (AF_INTERP_LOWER) - Lower interpolation
   
   **Performance Tips**:
   
   - Use power-of-2 sizes for FFT (256, 512, 1024, 2048, etc.)
   - Use FFT-based convolution for large filters (>32 elements)
   - Use real-to-complex FFT (`fft-r2c`) for real-valued signals
   - Use in-place FFT operations to save memory
   - Batch multiple operations to amortize kernel launch overhead
   
   **Common Use Cases**:
   
   - Audio processing: filtering, compression, analysis
   - Image processing: edge detection, smoothing, enhancement
   - Scientific computing: spectral analysis, correlation
   - Machine learning: feature extraction, convolution layers
   - Communications: modulation, demodulation, channel estimation
   
   See also:
   - integration.arith for element-wise operations
   - integration.algorithm for reduction and sorting"
  (:require [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.c-api.approx :as approx]
            [org.soulspace.arrayfire.ffi.c-api.fft :as fft]
            [org.soulspace.arrayfire.ffi.c-api.convolve :as convolve]
            [org.soulspace.arrayfire.ffi.c-api.fftconvolve :as fftconvolve]
            [org.soulspace.arrayfire.ffi.c-api.iir :as iir]
            [org.soulspace.arrayfire.ffi.c-api.filters :as filters]
            [org.soulspace.arrayfire.integration.unified-api.array :as array]
            [org.soulspace.arrayfire.integration.base.error :refer [check!]]
            [org.soulspace.arrayfire.integration.base.memory :as bmem]
            [org.soulspace.arrayfire.integration.base.jvm-integration :as jvm])
  (:import (org.soulspace.arrayfire.integration.base.jvm_integration AFArray)))

;;;
;;; Fast Fourier Transform (FFT)
;;;

;;
;; 1D FFT
;;

(defn fft
  "Compute the 1D Fast Fourier Transform (forward).
   
   Transforms a signal from the time domain to the frequency domain using
   the FFT algorithm. Supports complex and real inputs.
   
   Parameters:
   - in: Input array (AFArray) - can be real or complex
   - norm-factor: Normalization factor (default 1.0, no scaling)
   - output-size: Output length (default 0 = input length)
     - 0: Use input size
     - < input: Truncate signal
     - > input: Zero-pad signal (use power-of-2 for best performance)
   
   Returns:
   Complex AFArray with frequency spectrum
   
   Example:
   ```clojure
   ;; Basic FFT
   (def freq (fft time-signal))
   
   ;; FFT with padding to power-of-2
   (def freq-padded (fft time-signal 1.0 2048))
   
   ;; Custom normalization
   (def freq-norm (fft time-signal 0.5 0))
   ```
   
   See also:
   - ifft: Inverse FFT
   - fft-r2c: Optimized for real inputs"
  ([in]
   (fft in 1.0 0))
  ([in norm-factor]
   (fft in norm-factor 0))
  ([^AFArray in norm-factor output-size]
   (let [out (jvm/native-af-array-pointer)]
     (check! (fft/af-fft out (jvm/af-handle in) (double norm-factor) (long output-size))
                 "af-fft")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn fft2
  "Compute the 2D Fast Fourier Transform (forward).
   
   Transforms a 2D signal (e.g., image) from spatial domain to frequency domain.
   
   Parameters:
   - in: Input 2D array (AFArray)
   - norm-factor: Normalization factor (default 1.0)
   - output-size0: Output size for dimension 0 (default 0 = input size)
   - output-size1: Output size for dimension 1 (default 0 = input size)
   
   Returns:
   Complex AFArray with 2D frequency spectrum
   
   Example:
   ```clojure
   ;; 2D FFT of an image
   (def freq-2d (fft2 image))
   
   ;; With padding
   (def freq-padded (fft2 image 1.0 512 512))
   ```
   
   See also:
   - ifft2: Inverse 2D FFT
   - fft2-r2c: Optimized for real inputs"
  ([in]
   (fft2 in 1.0 0 0))
  ([in norm-factor]
   (fft2 in norm-factor 0 0))
  ([in norm-factor output-size0]
   (fft2 in norm-factor output-size0 0))
  ([^AFArray in norm-factor output-size0 output-size1]
   (let [out (jvm/native-af-array-pointer)]
     (check! (fft/af-fft2 out (jvm/af-handle in) (double norm-factor) 
                               (long output-size0) (long output-size1))
                 "af-fft2")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn fft3
  "Compute the 3D Fast Fourier Transform (forward).
   
   Transforms a 3D signal (e.g., volumetric data) to frequency domain.
   
   Parameters:
   - in: Input 3D array (AFArray)
   - norm-factor: Normalization factor (default 1.0)
   - output-size0: Output size for dimension 0 (default 0 = input size)
   - output-size1: Output size for dimension 1 (default 0 = input size)
   - output-size2: Output size for dimension 2 (default 0 = input size)
   
   Returns:
   Complex AFArray with 3D frequency spectrum
   
   See also:
   - ifft3: Inverse 3D FFT"
  ([in]
   (fft3 in 1.0 0 0 0))
  ([in norm-factor]
   (fft3 in norm-factor 0 0 0))
  ([in norm-factor output-size0]
   (fft3 in norm-factor output-size0 0 0))
  ([in norm-factor output-size0 output-size1]
   (fft3 in norm-factor output-size0 output-size1 0))
  ([^AFArray in norm-factor output-size0 output-size1 output-size2]
   (let [out (jvm/native-af-array-pointer)]
     (check! (fft/af-fft3 out (jvm/af-handle in) (double norm-factor)
                               (long output-size0) (long output-size1) (long output-size2))
                 "af-fft3")
     (jvm/af-array-new (jvm/deref-af-array out)))))

;;
;; 1D Inverse FFT
;;

(defn ifft
  "Compute the 1D Inverse Fast Fourier Transform.
   
   Transforms from frequency domain back to time domain.
   
   Parameters:
   - in: Input frequency spectrum (AFArray, complex)
   - norm-factor: Normalization factor (default 1.0/N for proper scaling)
   - output-size: Output length (default 0 = input length)
   
   Returns:
   Complex AFArray with time-domain signal
   
   Example:
   ```clojure
   ;; Standard inverse FFT with auto-normalization
   (def time (ifft freq-spectrum))
   
   ;; Custom normalization
   (def time-custom (ifft freq-spectrum 0.001 0))
   ```
   
   See also:
   - fft: Forward FFT"
  ([in]
   (ifft in 1.0 0))
  ([in norm-factor]
   (ifft in norm-factor 0))
  ([^AFArray in norm-factor output-size]
   (let [out (jvm/native-af-array-pointer)]
     (check! (fft/af-ifft out (jvm/af-handle in) (double norm-factor) (long output-size))
                 "af-ifft")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn ifft2
  "Compute the 2D Inverse Fast Fourier Transform.
   
   Parameters:
   - in: Input 2D frequency spectrum (AFArray, complex)
   - norm-factor: Normalization factor (default 1.0/(N*M))
   - output-size0: Output size for dimension 0 (default 0 = input size)
   - output-size1: Output size for dimension 1 (default 0 = input size)
   
   Returns:
   Complex AFArray with 2D spatial-domain signal
   
   See also:
   - fft2: Forward 2D FFT"
  ([in]
   (ifft2 in 1.0 0 0))
  ([in norm-factor]
   (ifft2 in norm-factor 0 0))
  ([in norm-factor output-size0]
   (ifft2 in norm-factor output-size0 0))
  ([^AFArray in norm-factor output-size0 output-size1]
   (let [out (jvm/native-af-array-pointer)]
     (check! (fft/af-ifft2 out (jvm/af-handle in) (double norm-factor)
                                (long output-size0) (long output-size1))
                 "af-ifft2")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn ifft3
  "Compute the 3D Inverse Fast Fourier Transform.
   
   Parameters:
   - in: Input 3D frequency spectrum (AFArray, complex)
   - norm-factor: Normalization factor (default 1.0/(N*M*P))
   - output-size0: Output size for dimension 0 (default 0 = input size)
   - output-size1: Output size for dimension 1 (default 0 = input size)
   - output-size2: Output size for dimension 2 (default 0 = input size)
   
   Returns:
   Complex AFArray with 3D spatial-domain signal
   
   See also:
   - fft3: Forward 3D FFT"
  ([in]
   (ifft3 in 1.0 0 0 0))
  ([in norm-factor]
   (ifft3 in norm-factor 0 0 0))
  ([in norm-factor output-size0]
   (ifft3 in norm-factor output-size0 0 0))
  ([in norm-factor output-size0 output-size1]
   (ifft3 in norm-factor output-size0 output-size1 0))
  ([^AFArray in norm-factor output-size0 output-size1 output-size2]
   (let [out (jvm/native-af-array-pointer)]
     (check! (fft/af-ifft3 out (jvm/af-handle in) (double norm-factor)
                                (long output-size0) (long output-size1) (long output-size2))
                 "af-ifft3")
     (jvm/af-array-new (jvm/deref-af-array out)))))

;;  
;; Normalized FFT Operations (convenience functions)
;;

(defn fft-norm
  "1D FFT with automatic 1/N normalization.
   
   Convenience wrapper that applies standard 1/N normalization automatically.
   Useful when you want normalized frequency domain coefficients.
   
   Parameters:
   - in: Input array (AFArray)
   - output-size: Output length (default 0 = input length)
   
   Returns:
   Complex AFArray with normalized frequency spectrum
   
   Example:
   ```clojure
   ;; Standard normalized FFT
   (def freq-norm (fft-norm time-signal))
   
   ;; With padding
   (def freq-padded (fft-norm time-signal 2048))
   ```
   
   Notes:
   - Normalization factor is 1/N where N is the signal length
   - For round-trip FFT/IFFT without normalization loss, use:
     * fft-norm + ifft (no norm), OR
     * fft (no norm) + ifft-norm
   
   See also:
   - fft: FFT with custom normalization
   - ifft-norm: Normalized inverse FFT"
  ([^AFArray in]
   (fft-norm in 0))
  ([^AFArray in output-size]
   (let [n (array/get-elements in)
         norm-factor (/ 1.0 (double n))]
     (fft in norm-factor output-size))))

(defn ifft-norm
  "1D inverse FFT with automatic 1/N normalization.
   
   Convenience wrapper for inverse FFT with standard normalization.
   
   Parameters:
   - in: Input frequency spectrum (AFArray, complex)
   - output-size: Output length (default 0 = input length)
   
   Returns:
   Complex AFArray with normalized time-domain signal
   
   Example:
   ```clojure
   ;; Standard normalized inverse FFT
   (def time-norm (ifft-norm freq-spectrum))
   ```
   
   Notes:
   - Normalization factor is 1/N where N is the spectrum length
   - Ensures proper scaling when doing round-trip fft/ifft
   
   See also:
   - ifft: Inverse FFT with custom normalization
   - fft-norm: Normalized forward FFT"
  ([^AFArray in]
   (ifft-norm in 0))
  ([^AFArray in output-size]
   (let [n (array/get-elements in)
         norm-factor (/ 1.0 (double n))]
     (ifft in norm-factor output-size))))

(defn fft2-norm
  "2D FFT with automatic 1/(N*M) normalization.
   
   Parameters:
   - in: Input 2D array (AFArray)
   - output-size0: Output size for dimension 0 (default 0)
   - output-size1: Output size for dimension 1 (default 0)
   
   Returns:
   Complex AFArray with normalized 2D frequency spectrum
   
   Example:
   ```clojure
   (def freq-2d-norm (fft2-norm image))
   ```"
  ([^AFArray in]
   (fft2-norm in 0 0))
  ([^AFArray in output-size0]
   (fft2-norm in output-size0 0))
  ([^AFArray in output-size0 output-size1]
   (let [n (array/get-elements in)
         norm-factor (/ 1.0 (double n))]
     (fft2 in norm-factor output-size0 output-size1))))

(defn ifft2-norm
  "2D inverse FFT with automatic 1/(N*M) normalization.
   
   Parameters:
   - in: Input 2D frequency spectrum (AFArray, complex)
   - output-size0: Output size for dimension 0 (default 0)
   - output-size1: Output size for dimension 1 (default 0)
   
   Returns:
   Complex AFArray with normalized 2D spatial signal
   
   Example:
   ```clojure
   (def image-norm (ifft2-norm freq-2d))
   ```"
  ([^AFArray in]
   (ifft2-norm in 0 0))
  ([^AFArray in output-size0]
   (ifft2-norm in output-size0 0))
  ([^AFArray in output-size0 output-size1]
   (let [n (array/get-elements in)
         norm-factor (/ 1.0 (double n))]
     (ifft2 in norm-factor output-size0 output-size1))))

(defn fft3-norm
  "3D FFT with automatic 1/(N*M*P) normalization.
   
   Parameters:
   - in: Input 3D array (AFArray)
   - output-size0: Output size for dimension 0 (default 0)
   - output-size1: Output size for dimension 1 (default 0)
   - output-size2: Output size for dimension 2 (default 0)
   
   Returns:
   Complex AFArray with normalized 3D frequency spectrum"
  ([^AFArray in]
   (fft3-norm in 0 0 0))
  ([^AFArray in output-size0]
   (fft3-norm in output-size0 0 0))
  ([^AFArray in output-size0 output-size1]
   (fft3-norm in output-size0 output-size1 0))
  ([^AFArray in output-size0 output-size1 output-size2]
   (let [n (array/get-elements in)
         norm-factor (/ 1.0 (double n))]
     (fft3 in norm-factor output-size0 output-size1 output-size2))))

(defn ifft3-norm
  "3D inverse FFT with automatic 1/(N*M*P) normalization.
   
   Parameters:
   - in: Input 3D frequency spectrum (AFArray, complex)
   - output-size0: Output size for dimension 0 (default 0)
   - output-size1: Output size for dimension 1 (default 0)
   - output-size2: Output size for dimension 2 (default 0)
   
   Returns:
   Complex AFArray with normalized 3D spatial signal"
  ([^AFArray in]
   (ifft3-norm in 0 0 0))
  ([^AFArray in output-size0]
   (ifft3-norm in output-size0 0 0))
  ([^AFArray in output-size0 output-size1]
   (ifft3-norm in output-size0 output-size1 0))
  ([^AFArray in output-size0 output-size1 output-size2]
   (let [n (array/get-elements in)
         norm-factor (/ 1.0 (double n))]
     (ifft3 in norm-factor output-size0 output-size1 output-size2))))

;;
;; Real-to-Complex FFT (optimized for real inputs)
;;

(defn fft-r2c
  "Compute 1D Real-to-Complex FFT (optimized for real inputs).
   
   More efficient than standard FFT for real-valued signals. Output is
   half-spectrum plus DC component due to conjugate symmetry.
   
   Parameters:
   - in: Input real array (AFArray)
   - norm-factor: Normalization factor (default 1.0)
   - pad0: Padding for dimension 0 (default 0 = no padding)
   
   Returns:
   Complex AFArray of size (N/2 + 1) with frequency spectrum
   
   Example:
   ```clojure
   ;; Real FFT of audio signal
   (def freq (fft-r2c audio-signal))
   
   ;; With padding
   (def freq-padded (fft-r2c audio-signal 1.0 2048))
   ```
   
   See also:
   - fft-c2r: Complex-to-real inverse"
  ([in]
   (fft-r2c in 1.0 0))
  ([in norm-factor]
   (fft-r2c in norm-factor 0))
  ([^AFArray in norm-factor pad0]
   (let [out (jvm/native-af-array-pointer)]
     (check! (fft/af-fft-r2c out (jvm/af-handle in) (double norm-factor) (long pad0))
                 "af-fft-r2c")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn fft2-r2c
  "Compute 2D Real-to-Complex FFT.
   
   Parameters:
   - in: Input real 2D array (AFArray)
   - norm-factor: Normalization factor (default 1.0)
   - pad0: Padding for dimension 0 (default 0)
   - pad1: Padding for dimension 1 (default 0)
   
   Returns:
   Complex AFArray with 2D frequency spectrum (half-spectrum in first dimension)
   
   See also:
   - fft2-c2r: 2D complex-to-real inverse"
  ([in]
   (fft2-r2c in 1.0 0 0))
  ([in norm-factor]
   (fft2-r2c in norm-factor 0 0))
  ([in norm-factor pad0]
   (fft2-r2c in norm-factor pad0 0))
  ([^AFArray in norm-factor pad0 pad1]
   (let [out (jvm/native-af-array-pointer)]
     (check! (fft/af-fft2-r2c out (jvm/af-handle in) (double norm-factor)
                                   (long pad0) (long pad1))
                 "af-fft2-r2c")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn fft3-r2c
  "Compute 3D Real-to-Complex FFT.
   
   Parameters:
   - in: Input real 3D array (AFArray)
   - norm-factor: Normalization factor (default 1.0)
   - pad0: Padding for dimension 0 (default 0)
   - pad1: Padding for dimension 1 (default 0)
   - pad2: Padding for dimension 2 (default 0)
   
   Returns:
   Complex AFArray with 3D frequency spectrum
   
   See also:
   - fft3-c2r: 3D complex-to-real inverse"
  ([in]
   (fft3-r2c in 1.0 0 0 0))
  ([in norm-factor]
   (fft3-r2c in norm-factor 0 0 0))
  ([in norm-factor pad0]
   (fft3-r2c in norm-factor pad0 0 0))
  ([in norm-factor pad0 pad1]
   (fft3-r2c in norm-factor pad0 pad1 0))
  ([^AFArray in norm-factor pad0 pad1 pad2]
   (let [out (jvm/native-af-array-pointer)]
     (check! (fft/af-fft3-r2c out (jvm/af-handle in) (double norm-factor)
                                   (long pad0) (long pad1) (long pad2))
                 "af-fft3-r2c")
     (jvm/af-array-new (jvm/deref-af-array out)))))

;;
;; Complex-to-Real FFT (inverse of R2C)
;;

(defn fft-c2r
  "Compute 1D Complex-to-Real inverse FFT.
   
   Inverse of fft-r2c. Transforms half-spectrum back to real signal.
   
   Parameters:
   - in: Input complex half-spectrum (AFArray, size N/2+1)
   - norm-factor: Normalization factor (default 1.0/N)
   - is-odd: True if original signal length was odd (default false)
   
   Returns:
   Real AFArray with time-domain signal
   
   Example:
   ```clojure
   ;; Inverse of real FFT
   (def time (fft-c2r freq-half false))
   ```
   
   See also:
   - fft-r2c: Real-to-complex forward FFT"
  ([in is-odd]
   (fft-c2r in 1.0 is-odd))
  ([^AFArray in norm-factor is-odd]
   (let [out (jvm/native-af-array-pointer)]
     (check! (fft/af-fft-c2r out (jvm/af-handle in) (double norm-factor) (if is-odd 1 0))
                 "af-fft-c2r")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn fft2-c2r
  "Compute 2D Complex-to-Real inverse FFT.
   
   Parameters:
   - in: Input complex 2D half-spectrum (AFArray)
   - norm-factor: Normalization factor (default 1.0/(N*M))
   - is-odd: True if original signal's first dimension was odd
   
   Returns:
   Real AFArray with 2D spatial-domain signal
   
   See also:
   - fft2-r2c: 2D real-to-complex forward FFT"
  ([in is-odd]
   (fft2-c2r in 1.0 is-odd))
  ([^AFArray in norm-factor is-odd]
   (let [out (jvm/native-af-array-pointer)]
     (check! (fft/af-fft2-c2r out (jvm/af-handle in) (double norm-factor) (if is-odd 1 0))
                 "af-fft2-c2r")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn fft3-c2r
  "Compute 3D Complex-to-Real inverse FFT.
   
   Parameters:
   - in: Input complex 3D half-spectrum (AFArray)
   - norm-factor: Normalization factor (default 1.0/(N*M*P))
   - is-odd: True if original signal's first dimension was odd
   
   Returns:
   Real AFArray with 3D spatial-domain signal
   
   See also:
   - fft3-r2c: 3D real-to-complex forward FFT"
  ([in is-odd]
   (fft3-c2r in 1.0 is-odd))
  ([^AFArray in norm-factor is-odd]
   (let [out (jvm/native-af-array-pointer)]
     (check! (fft/af-fft3-c2r out (jvm/af-handle in) (double norm-factor) (if is-odd 1 0))
                 "af-fft3-c2r")
     (jvm/af-array-new (jvm/deref-af-array out)))))

;;
;; In-place FFT (memory-efficient)
;;

(defn fft!
  "Compute 1D FFT in-place (overwrites input).
   
   Memory-efficient version that modifies the input array directly.
   Input must be complex type.
   
   Parameters:
   - in: Input/output complex array (AFArray, modified in-place)
   - norm-factor: Normalization factor (default 1.0)
   
   Returns:
   The modified input AFArray
   
   Example:
   ```clojure
   (def signal (create-complex-signal))
   (fft! signal) ; signal now contains frequency spectrum
   ```
   
   See also:
   - fft: Out-of-place version"
  ([in]
   (fft! in 1.0))
  ([^AFArray in norm-factor]
   (check! (fft/af-fft-inplace (jvm/af-handle in) (double norm-factor))
               "af-fft-inplace")
   in))

(defn fft2!
  "Compute 2D FFT in-place.
   
   Parameters:
   - in: Input/output complex 2D array (AFArray, modified in-place)
   - norm-factor: Normalization factor (default 1.0)
   
   Returns:
   The modified input AFArray
   
   See also:
   - fft2: Out-of-place version"
  ([in]
   (fft2! in 1.0))
  ([^AFArray in norm-factor]
   (check! (fft/af-fft2-inplace (jvm/af-handle in) (double norm-factor))
               "af-fft2-inplace")
   in))

(defn fft3!
  "Compute 3D FFT in-place.
   
   Parameters:
   - in: Input/output complex 3D array (AFArray, modified in-place)
   - norm-factor: Normalization factor (default 1.0)
   
   Returns:
   The modified input AFArray
   
   See also:
   - fft3: Out-of-place version"
  ([in]
   (fft3! in 1.0))
  ([^AFArray in norm-factor]
   (check! (fft/af-fft3-inplace (jvm/af-handle in) (double norm-factor))
               "af-fft3-inplace")
   in))

(defn ifft!
  "Compute 1D inverse FFT in-place.
   
   Parameters:
   - in: Input/output complex array (AFArray, modified in-place)
   - norm-factor: Normalization factor (default 1.0/N)
   
   Returns:
   The modified input AFArray
   
   See also:
   - ifft: Out-of-place version"
  ([in]
   (ifft! in 1.0))
  ([^AFArray in norm-factor]
   (check! (fft/af-ifft-inplace (jvm/af-handle in) (double norm-factor))
               "af-ifft-inplace")
   in))

(defn ifft2!
  "Compute 2D inverse FFT in-place.
   
   Parameters:
   - in: Input/output complex 2D array (AFArray, modified in-place)
   - norm-factor: Normalization factor (default 1.0/(N*M))
   
   Returns:
   The modified input AFArray
   
   See also:
   - ifft2: Out-of-place version"
  ([in]
   (ifft2! in 1.0))
  ([^AFArray in norm-factor]
   (check! (fft/af-ifft2-inplace (jvm/af-handle in) (double norm-factor))
               "af-ifft2-inplace")
   in))

(defn ifft3!
  "Compute 3D inverse FFT in-place.
   
   Parameters:
   - in: Input/output complex 3D array (AFArray, modified in-place)
   - norm-factor: Normalization factor (default 1.0/(N*M*P))
   
   Returns:
   The modified input AFArray
   
   See also:
   - ifft3: Out-of-place version"
  ([in]
   (ifft3! in 1.0))
  ([^AFArray in norm-factor]
   (check! (fft/af-ifft3-inplace (jvm/af-handle in) (double norm-factor))
               "af-ifft3-inplace")
   in))

;;;
;;; Convolution
;;;

(defn convolve1
  "Compute 1D convolution of signal and filter.
   
   Convolves a 1D signal with a filter kernel. Automatically selects
   spatial or frequency domain algorithm based on sizes.
   
   Parameters:
   - signal: Input signal (AFArray)
   - filter: Filter kernel (AFArray)
   - mode: Convolution mode (default 0 = AF_CONV_DEFAULT)
     - 0: Auto-select algorithm
     - 1: Expand mode (output size = signal + filter - 1)
     - 2: Same mode (output size = signal size)
   - domain: Computation domain (default 0 = AF_CONV_AUTO)
     - 0: Auto-select (spatial or frequency)
     - 1: Spatial domain
     - 2: Frequency domain
   
   Returns:
   Convolved signal (AFArray)
   
   Example:
   ```clojure
   ;; Smooth signal with averaging filter
   (def smoothed (convolve1 noisy-signal avg-filter))
   
   ;; Edge detection
   (def edges (convolve1 signal edge-filter 2 0))
   ```
   
   See also:
   - fft-convolve1: Frequency domain convolution"
  ([signal filter]
   (convolve1 signal filter 0 0))
  ([signal filter mode]
   (convolve1 signal filter mode 0))
  ([^AFArray signal ^AFArray filter mode domain]
   (let [out (jvm/native-af-array-pointer)]
     (check! (convolve/af-convolve1 out (jvm/af-handle signal) (jvm/af-handle filter)
                                         (int mode) (int domain))
                 "af-convolve1")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn convolve2
  "Compute 2D convolution of signal and filter.
   
   Convolves a 2D signal (e.g., image) with a 2D filter kernel.
   Used for image filtering, edge detection, blurring, etc.
   
   Parameters:
   - signal: Input 2D signal (AFArray)
   - filter: 2D filter kernel (AFArray)
   - mode: Convolution mode (default 0)
   - domain: Computation domain (default 0)
   
   Returns:
   Convolved 2D signal (AFArray)
   
   Example:
   ```clojure
   ;; Gaussian blur
   (def blurred (convolve2 image gaussian-kernel))
   
   ;; Sobel edge detection
   (def edges-x (convolve2 image sobel-x-kernel 2 0))
   ```
   
   See also:
   - convolve2-sep: Separable convolution (faster for certain kernels)"
  ([signal filter]
   (convolve2 signal filter 0 0))
  ([signal filter mode]
   (convolve2 signal filter mode 0))
  ([^AFArray signal ^AFArray filter mode domain]
   (let [out (jvm/native-af-array-pointer)]
     (check! (convolve/af-convolve2 out (jvm/af-handle signal) (jvm/af-handle filter)
                                         (int mode) (int domain))
                 "af-convolve2")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn convolve3
  "Compute 3D convolution of signal and filter.
   
   Convolves a 3D signal (e.g., volumetric data) with a 3D filter kernel.
   
   Parameters:
   - signal: Input 3D signal (AFArray)
   - filter: 3D filter kernel (AFArray)
   - mode: Convolution mode (default 0)
   - domain: Computation domain (default 0)
   
   Returns:
   Convolved 3D signal (AFArray)
   
   See also:
   - fft-convolve3: Frequency domain convolution"
  ([signal filter]
   (convolve3 signal filter 0 0))
  ([signal filter mode]
   (convolve3 signal filter mode 0))
  ([^AFArray signal ^AFArray filter mode domain]
   (let [out (jvm/native-af-array-pointer)]
     (check! (convolve/af-convolve3 out (jvm/af-handle signal) (jvm/af-handle filter)
                                         (int mode) (int domain))
                 "af-convolve3")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn convolve2-sep
  "Compute 2D separable convolution (faster for separable kernels).
   
   Separable convolution applies row and column filters independently,
   which is much faster than full 2D convolution for separable kernels.
   
   Parameters:
   - col-filter: Column filter (1D AFArray)
   - row-filter: Row filter (1D AFArray)
   - signal: Input 2D signal (AFArray)
   - mode: Convolution mode (default 0)
   
   Returns:
   Convolved 2D signal (AFArray)
   
   Example:
   ```clojure
   ;; Gaussian blur using separable filters
   (def blurred (convolve2-sep gauss-1d gauss-1d image))
   ```
   
   Note: Only applicable when filter is separable (outer product of two 1D filters)
   
   See also:
   - convolve2: General 2D convolution"
  ([col-filter row-filter signal]
   (convolve2-sep col-filter row-filter signal 0))
  ([^AFArray col-filter ^AFArray row-filter ^AFArray signal mode]
   (let [out (jvm/native-af-array-pointer)]
     (check! (convolve/af-convolve2-sep out (jvm/af-handle col-filter) 
                                             (jvm/af-handle row-filter)
                                             (jvm/af-handle signal) (int mode))
                 "af-convolve2-sep")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn convolve2-nn
  "Compute 2D convolution optimized for neural networks.
   
   Performs 2D convolution with stride, padding, and dilation support,
   specifically designed for deep learning applications. This function
   performs correlation (not convolution), which is standard in CNNs.
   
   Parameters:
   - signal: Input signal/image (AFArray) [height × width × channels × batch]
   - filter: Filter/kernel (AFArray) [kernel_h × kernel_w × channels × num_filters]
   - strides: Stride values [stride_height stride_width] (vector)
   - paddings: Padding values [pad_height pad_width] (vector)
   - dilations: Dilation values [dilation_height dilation_width] (vector)
   
   Returns:
   Convolved output (AFArray) [out_h × out_w × num_filters × batch]
   
   Output dimensions:
   - out_h = floor((height + 2×pad_h - dil_h×(kernel_h-1) - 1) / stride_h) + 1
   - out_w = floor((width + 2×pad_w - dil_w×(kernel_w-1) - 1) / stride_w) + 1
   
   Parameters explained:
   
   **Stride**: Controls output size by moving filter in larger steps
   - [1 1]: Standard convolution, output nearly same size as input
   - [2 2]: Downsamples by 2x, common in pooling layers
   
   **Padding**: Adds zeros around input border
   - [0 0]: Valid convolution, output smaller than input
   - [(k-1)/2 (k-1)/2]: Same convolution, preserves input size
   
   **Dilation**: Spaces out filter elements (atrous convolution)
   - [1 1]: Standard convolution
   - [2 2]: Dilated convolution with receptive field 2x larger
   
   Example:
   ```clojure
   ;; Standard 3×3 convolution
   (def output (convolve2-nn input filter [1 1] [1 1] [1 1]))
   
   ;; Stride-2 downsampling convolution
   (def downsampled (convolve2-nn input filter [2 2] [1 1] [1 1]))
   
   ;; Dilated convolution for larger receptive field
   (def dilated (convolve2-nn input filter [1 1] [0 0] [2 2]))
   ```
   
   See also:
   - convolve2-gradient-nn in ml namespace (for backpropagation)
   - convolve2: General purpose 2D convolution"
  [^AFArray signal ^AFArray filter strides paddings dilations]
  (let [;; Convert strides to native array
        stride-count (count strides)
        stride-buf (bmem/dims->segment strides)
        
        ;; Convert paddings to native array
        padding-count (count paddings)
        padding-buf (bmem/dims->segment paddings)
        
        ;; Convert dilations to native array
        dilation-count (count dilations)
        dilation-buf (bmem/dims->segment dilations)
        
        ;; Allocate output
        out (jvm/native-af-array-pointer)]
    
    (check! (convolve/af-convolve2-nn
                  out
                  (jvm/af-handle signal)
                  (jvm/af-handle filter)
                  (int stride-count)
                  stride-buf
                  (int padding-count)
                  padding-buf
                  (int dilation-count)
                  dilation-buf)
                "af-convolve2-nn")
    
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn fft-convolve1
  "Compute 1D convolution using FFT (frequency domain).
   
   Always uses frequency domain algorithm. Faster than spatial domain
   for large filters (typically > 32 elements).
   
   Parameters:
   - signal: Input signal (AFArray)
   - filter: Filter kernel (AFArray)
   - mode: Convolution mode (default 0)
   
   Returns:
   Convolved signal (AFArray)
   
   See also:
   - convolve1: Auto-select algorithm"
  ([signal filter]
   (fft-convolve1 signal filter 0))
  ([^AFArray signal ^AFArray filter mode]
   (let [out (jvm/native-af-array-pointer)]
     (check! (fftconvolve/af-fft-convolve1 out (jvm/af-handle signal) 
                                                 (jvm/af-handle filter) (int mode))
                 "af-fft-convolve1")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn fft-convolve2
  "Compute 2D convolution using FFT.
   
   Parameters:
   - signal: Input 2D signal (AFArray)
   - filter: 2D filter kernel (AFArray)
   - mode: Convolution mode (default 0)
   
   Returns:
   Convolved 2D signal (AFArray)
   
   See also:
   - convolve2: Auto-select algorithm"
  ([signal filter]
   (fft-convolve2 signal filter 0))
  ([^AFArray signal ^AFArray filter mode]
   (let [out (jvm/native-af-array-pointer)]
     (check! (fftconvolve/af-fft-convolve2 out (jvm/af-handle signal)
                                                 (jvm/af-handle filter) (int mode))
                 "af-fft-convolve2")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn fft-convolve3
  "Compute 3D convolution using FFT.
   
   Parameters:
   - signal: Input 3D signal (AFArray)
   - filter: 3D filter kernel (AFArray)
   - mode: Convolution mode (default 0)
   
   Returns:
   Convolved 3D signal (AFArray)
   
   See also:
   - convolve3: Auto-select algorithm"
  ([signal filter]
   (fft-convolve3 signal filter 0))
  ([^AFArray signal ^AFArray filter mode]
   (let [out (jvm/native-af-array-pointer)]
     (check! (fftconvolve/af-fft-convolve3 out (jvm/af-handle signal)
                                                 (jvm/af-handle filter) (int mode))
                 "af-fft-convolve3")
     (jvm/af-array-new (jvm/deref-af-array out)))))

;;;
;;; Digital Filters
;;;

(defn iir
  "Apply Infinite Impulse Response (IIR) digital filter.
   
   IIR filters have feedback and infinite impulse response. Common types
   include Butterworth, Chebyshev, and elliptic filters.
   
   Transfer function: H(z) = B(z)/A(z)
   where B(z) are feedforward coefficients, A(z) are feedback coefficients.
   
   Parameters:
   - b: Feedforward coefficients (AFArray, numerator)
   - a: Feedback coefficients (AFArray, denominator)
   - x: Input signal (AFArray)
   
   Returns:
   Filtered signal (AFArray)
   
   Example:
   ```clojure
   ;; Low-pass Butterworth filter
   (def b-coeffs (create-butterworth-b))
   (def a-coeffs (create-butterworth-a))
   (def filtered (iir b-coeffs a-coeffs noisy-signal))
   ```
   
   Note: First element of 'a' should typically be 1.0
   
   See also:
   - fir: Finite impulse response filter"
  [^AFArray b ^AFArray a ^AFArray x]
  (let [out (jvm/native-af-array-pointer)]
    (check! (iir/af-iir out (jvm/af-handle b) (jvm/af-handle a) (jvm/af-handle x))
                "af-iir")
    (jvm/af-array-new (jvm/deref-af-array out))))

;;;
;;; Median Filtering
;;;

(defn medfilt
  "Apply 2D median filter for noise reduction.
   
   Median filtering is a nonlinear filter that replaces each pixel with
   the median of its neighborhood. Excellent for salt-and-pepper noise
   removal while preserving edges.
   
   Parameters:
   - in: Input 2D array (AFArray)
   - wind-length: Window length (height) (default 3)
   - wind-width: Window width (default 3)
   - edge-pad: Border handling mode (default 0 = AF_PAD_ZERO)
     - 0: Zero padding
     - 1: Symmetric padding
   
   Returns:
   Median-filtered array (AFArray)
   
   Example:
   ```clojure
   ;; 3x3 median filter
   (def denoised (medfilt noisy-image))
   
   ;; 5x5 median filter
   (def smooth (medfilt noisy-image 5 5))
   ```
   
   See also:
   - medfilt1: 1D median filter
   - medfilt2: Alias for medfilt"
  ([in]
   (medfilt in 3 3 0))
  ([in wind-length]
   (medfilt in wind-length 3 0))
  ([in wind-length wind-width]
   (medfilt in wind-length wind-width 0))
  ([^AFArray in wind-length wind-width edge-pad]
   (let [out (jvm/native-af-array-pointer)]
     (check! (filters/af-medfilt out (jvm/af-handle in) (long wind-length) 
                                      (long wind-width) (int edge-pad))
                 "af-medfilt")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn medfilt1
  "Apply 1D median filter.
   
   Parameters:
   - in: Input 1D array (AFArray)
   - wind-width: Window width (default 3)
   - edge-pad: Border handling mode (default 0)
   
   Returns:
   Median-filtered array (AFArray)
   
   Example:
   ```clojure
   ;; Remove spikes from 1D signal
   (def clean (medfilt1 noisy-1d-signal 5))
   ```
   
   See also:
   - medfilt: 2D median filter"
  ([in]
   (medfilt1 in 3 0))
  ([in wind-width]
   (medfilt1 in wind-width 0))
  ([^AFArray in wind-width edge-pad]
   (let [out (jvm/native-af-array-pointer)]
     (check! (filters/af-medfilt1 out (jvm/af-handle in) (long wind-width) (int edge-pad))
                 "af-medfilt1")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn medfilt2
  "Apply 2D median filter (alias for medfilt).
   
   Parameters:
   - in: Input 2D array (AFArray)
   - wind-length: Window length (default 3)
   - wind-width: Window width (default 3)
   - edge-pad: Border handling mode (default 0)
   
   Returns:
   Median-filtered array (AFArray)
   
   See also:
   - medfilt: Main 2D median filter function"
  ([in]
   (medfilt2 in 3 3 0))
  ([in wind-length]
   (medfilt2 in wind-length 3 0))
  ([in wind-length wind-width]
   (medfilt2 in wind-length wind-width 0))
  ([^AFArray in wind-length wind-width edge-pad]
   (let [out (jvm/native-af-array-pointer)]
     (check! (filters/af-medfilt2 out (jvm/af-handle in) (long wind-length)
                                       (long wind-width) (int edge-pad))
                 "af-medfilt2")
     (jvm/af-array-new (jvm/deref-af-array out)))))

;;;
;;; Interpolation/Approximation
;;;

(defn approx1
  "1D interpolation/approximation at specified positions.
   
   Interpolates values from input array yi at positions specified by xo.
   Supports various interpolation methods.
   
   Parameters:
   - yi: Input values (AFArray)
   - xo: Output positions to interpolate at (AFArray)
   - method: Interpolation method (default 1 = AF_INTERP_LINEAR)
     - 0: Nearest neighbor
     - 1: Linear
     - 2: Bilinear (for 2D)
     - 3: Cubic
   - off-grid: Value for out-of-bounds positions (default 0.0)
   
   Returns:
   Interpolated values at positions xo (AFArray)
   
   Example:
   ```clojure
   ;; Resample signal at new positions
   (def resampled (approx1 original-signal new-positions 1 0.0))
   ```
   
   See also:
   - approx1-uniform: Uniform grid interpolation"
  ([yi xo]
   (approx1 yi xo 1 0.0))
  ([yi xo method]
   (approx1 yi xo method 0.0))
  ([^AFArray yi ^AFArray xo method off-grid]
   (let [out (jvm/native-af-array-pointer)]
     (check! (approx/af-approx1-v2 out (jvm/af-handle yi) (jvm/af-handle xo)
                                        (int method) (float off-grid))
                 "af-approx1-v2")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn approx1-uniform
  "1D interpolation on uniform grid.
   
   More efficient than approx1 when input is uniformly spaced.
   
   Parameters:
   - yi: Input values (AFArray)
   - xo: Output positions (AFArray)
   - xdim: Dimension along which to interpolate (default 0)
   - xi-beg: Start of input grid (default 0.0)
   - xi-step: Step size of input grid (default 1.0)
   - method: Interpolation method (default 1)
   - off-grid: Value for out-of-bounds (default 0.0)
   
   Returns:
   Interpolated values (AFArray)
   
   See also:
   - approx1: General 1D interpolation"
  ([yi xo]
   (approx1-uniform yi xo 0 0.0 1.0 1 0.0))
  ([yi xo xdim]
   (approx1-uniform yi xo xdim 0.0 1.0 1 0.0))
  ([yi xo xdim xi-beg xi-step]
   (approx1-uniform yi xo xdim xi-beg xi-step 1 0.0))
  ([yi xo xdim xi-beg xi-step method]
   (approx1-uniform yi xo xdim xi-beg xi-step method 0.0))
  ([^AFArray yi ^AFArray xo xdim xi-beg xi-step method off-grid]
   (let [out (jvm/native-af-array-pointer)]
     (check! (approx/af-approx1-uniform-v2 out (jvm/af-handle yi) (jvm/af-handle xo)
                                                 (int xdim) (double xi-beg) (double xi-step)
                                                 (int method) (float off-grid))
                 "af-approx1-uniform-v2")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn approx2
  "2D interpolation/approximation at specified positions.
   
   Interpolates values from 2D input array zi at 2D positions (xo, yo).
   
   Parameters:
   - zi: Input 2D values (AFArray)
   - xo: Output x-positions (AFArray)
   - yo: Output y-positions (AFArray)
   - method: Interpolation method (default 2 = AF_INTERP_BILINEAR)
   - off-grid: Value for out-of-bounds (default 0.0)
   
   Returns:
   Interpolated 2D values (AFArray)
   
   Example:
   ```clojure
   ;; Warp image to new coordinate system
   (def warped (approx2 image new-x-coords new-y-coords 2 0.0))
   ```
   
   See also:
   - approx2-uniform: Uniform grid interpolation"
  ([zi xo yo]
   (approx2 zi xo yo 2 0.0))
  ([zi xo yo method]
   (approx2 zi xo yo method 0.0))
  ([^AFArray zi ^AFArray xo ^AFArray yo method off-grid]
   (let [out (jvm/native-af-array-pointer)]
     (check! (approx/af-approx2-v2 out (jvm/af-handle zi) (jvm/af-handle xo)
                                        (jvm/af-handle yo) (int method) (float off-grid))
                 "af-approx2-v2")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn approx2-uniform
  "2D interpolation on uniform grid.
   
   More efficient for uniformly-spaced 2D grids.
   
   Parameters:
   - zi: Input 2D values (AFArray)
   - xo: Output x-positions (AFArray)
   - xdim: X-dimension for interpolation (default 0)
   - xi-beg: Start of input x-grid (default 0.0)
   - xi-step: Step size of input x-grid (default 1.0)
   - yo: Output y-positions (AFArray)
   - ydim: Y-dimension for interpolation (default 1)
   - yi-beg: Start of input y-grid (default 0.0)
   - yi-step: Step size of input y-grid (default 1.0)
   - method: Interpolation method (default 2)
   - off-grid: Value for out-of-bounds (default 0.0)
   
   Returns:
   Interpolated 2D values (AFArray)
   
   See also:
   - approx2: General 2D interpolation"
  ([zi xo yo]
   (approx2-uniform zi xo 0 0.0 1.0 yo 1 0.0 1.0 2 0.0))
  ([zi xo xdim xi-beg xi-step yo ydim yi-beg yi-step]
   (approx2-uniform zi xo xdim xi-beg xi-step yo ydim yi-beg yi-step 2 0.0))
  ([zi xo xdim xi-beg xi-step yo ydim yi-beg yi-step method]
   (approx2-uniform zi xo xdim xi-beg xi-step yo ydim yi-beg yi-step method 0.0))
  ([^AFArray zi ^AFArray xo xdim xi-beg xi-step ^AFArray yo ydim yi-beg yi-step method off-grid]
   (let [out (jvm/native-af-array-pointer)]
     (check! (approx/af-approx2-uniform-v2 out (jvm/af-handle zi) (jvm/af-handle xo)
                                                 (int xdim) (double xi-beg) (double xi-step)
                                                 (jvm/af-handle yo) (int ydim) (double yi-beg)
                                                 (double yi-step) (int method) (float off-grid))
                 "af-approx2-uniform-v2")
     (jvm/af-array-new (jvm/deref-af-array out)))))

