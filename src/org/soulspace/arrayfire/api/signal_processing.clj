(ns org.soulspace.arrayfire.api.signal-processing
  "Idiomatic Clojure signal processing API for ArrayFire arrays.

   Provides GPU-accelerated signal processing operations including:

   Fourier Transforms:
   - 1D: `fft`, `ifft`, `fft-normalized`, `ifft-normalized`
   - 2D: `fft2`, `ifft2`, `fft2-normalized`, `ifft2-normalized`
   - 3D: `fft3`, `ifft3`, `fft3-normalized`, `ifft3-normalized`
   - Real-to-complex: `fft-r2c`, `fft2-r2c`, `fft3-r2c`
   - Complex-to-real: `fft-c2r`, `fft2-c2r`, `fft3-c2r`
   - In-place: `fft!`, `ifft!`, `fft2!`, `ifft2!`, `fft3!`, `ifft3!`

   Convolution:
   - Standard: `convolve1`, `convolve2`, `convolve3`
   - FFT-based: `fft-convolve1`, `fft-convolve2`, `fft-convolve3`
   - Separable: `convolve2-sep`
   - Neural network: `convolve2-nn`

   Filtering:
   - IIR digital filter: `iir`
   - Median filters: `median-filter1`, `median-filter2`

   Interpolation:
   - 1D: `approx1`, `approx1-uniform`
   - 2D: `approx2`, `approx2-uniform`

   ## Convolution modes

   | Keyword   | Description                                  |
   |-----------|----------------------------------------------|
   | `:default` | Output same size as input (default)          |
   | `:expand`  | Output size = signal + filter - 1            |

   ## Convolution domains

   | Keyword    | Description                                 |
   |------------|---------------------------------------------|
   | `:auto`    | Auto-select best algorithm (default)        |
   | `:spatial` | Force spatial domain convolution            |
   | `:freq`    | Force frequency domain convolution          |

   ## Interpolation methods

   | Keyword           | Description                            |
   |-------------------|----------------------------------------|
   | `:nearest`        | Nearest neighbor                       |
   | `:linear`         | Linear interpolation                   |
   | `:bilinear`       | Bilinear (2D)                          |
   | `:cubic`          | Cubic spline                           |
   | `:lower`          | Lower interpolation                    |
   | `:linear-cosine`  | Linear cosine                          |
   | `:bilinear-cosine`| Bilinear cosine                        |
   | `:bicubic`        | Bicubic                                |
   | `:cubic-spline`   | Cubic spline                           |
   | `:bicubic-spline` | Bicubic spline                         |

   ## Edge padding modes

   | Keyword          | Description                              |
   |------------------|------------------------------------------|
   | `:zero`          | Zero padding (default)                   |
   | `:sym`           | Symmetric padding                        |
   | `:clamp-to-edge` | Clamp to edge value                      |
   | `:periodic`      | Periodic / wrap-around                   |

   All functions must be called within a `with-arrayfire` region from
   `org.soulspace.arrayfire.api.core`."
  (:require [org.soulspace.arrayfire.integration.base.definitions :as defs]
            [org.soulspace.arrayfire.integration.unified-api.signal :as signal]
            [org.soulspace.arrayfire.api.core :refer [assert-within-arrayfire!]])
  (:import (org.soulspace.arrayfire.integration.base.resource AFArray)))

;;;
;;; Private keyword→integer constant resolution
;;;

(defn- resolve-conv-mode
  "Resolve a convolution mode keyword to its integer constant."
  [mode]
  (if (keyword? mode)
    (or (defs/conv-mode-kw->const mode)
        (throw (ex-info (str "Unknown convolution mode keyword: " mode
                             ". Valid keys: " (keys defs/conv-mode-kw->const))
                        {:mode mode})))
    (int mode)))

(defn- resolve-conv-domain
  "Resolve a convolution domain keyword to its integer constant."
  [domain]
  (if (keyword? domain)
    (or (defs/conv-domain-kw->const domain)
        (throw (ex-info (str "Unknown convolution domain keyword: " domain
                             ". Valid keys: " (keys defs/conv-domain-kw->const))
                        {:domain domain})))
    (int domain)))

(defn- resolve-interp
  "Resolve an interpolation method keyword to its integer constant."
  [method]
  (if (keyword? method)
    (or (defs/interp-kw->const method)
        (throw (ex-info (str "Unknown interpolation method keyword: " method
                             ". Valid keys: " (keys defs/interp-kw->const))
                        {:method method})))
    (int method)))

(defn- resolve-edge-pad
  "Resolve an edge padding keyword to its integer constant."
  [edge-pad]
  (if (keyword? edge-pad)
    (or (defs/border-kw->const edge-pad)
        (throw (ex-info (str "Unknown edge padding keyword: " edge-pad
                             ". Valid keys: " (keys defs/border-kw->const))
                        {:edge-pad edge-pad})))
    (int edge-pad)))

;;;
;;; Fast Fourier Transform (FFT) — Out-of-place
;;;

(defn fft
  "Compute the 1D Fast Fourier Transform (forward).

   Transforms a signal from the time domain to the frequency domain.

   Parameters:
     in          — input AFArray (real or complex)
     norm-factor — (optional) normalization factor; default 1.0 (no scaling)
     output-size — (optional) output length; default 0 (= input length).
                   Use a power-of-2 for best performance. Values > input
                   zero-pad, values < input truncate.

   Returns:
     Complex AFArray with frequency spectrum.

   Example:
     (fft signal)              ; basic 1D FFT
     (fft signal 1.0 2048)     ; FFT with zero-padding to 2048"
  (^AFArray [^AFArray in]
   (assert-within-arrayfire! "fft")
   (signal/fft in))
  (^AFArray [^AFArray in norm-factor]
   (assert-within-arrayfire! "fft")
   (signal/fft in (double norm-factor)))
  (^AFArray [^AFArray in norm-factor output-size]
   (assert-within-arrayfire! "fft")
   (signal/fft in (double norm-factor) (long output-size))))

(defn fft2
  "Compute the 2D Fast Fourier Transform (forward).

   Transforms a 2D signal (e.g. image) from spatial domain to frequency domain.

   Parameters:
     in           — input 2D AFArray
     norm-factor  — (optional) normalization factor; default 1.0
     output-size0 — (optional) output size for dimension 0; default 0
     output-size1 — (optional) output size for dimension 1; default 0

   Returns:
     Complex AFArray with 2D frequency spectrum."
  (^AFArray [^AFArray in]
   (assert-within-arrayfire! "fft2")
   (signal/fft2 in))
  (^AFArray [^AFArray in norm-factor]
   (assert-within-arrayfire! "fft2")
   (signal/fft2 in (double norm-factor)))
  (^AFArray [^AFArray in norm-factor output-size0]
   (assert-within-arrayfire! "fft2")
   (signal/fft2 in (double norm-factor) (long output-size0)))
  (^AFArray [^AFArray in norm-factor output-size0 output-size1]
   (assert-within-arrayfire! "fft2")
   (signal/fft2 in (double norm-factor) (long output-size0) (long output-size1))))

(defn fft3
  "Compute the 3D Fast Fourier Transform (forward).

   Transforms a 3D signal (e.g. volumetric data) to frequency domain.

   Parameters:
     in           — input 3D AFArray
     norm-factor  — (optional) normalization factor; default 1.0
     output-size0 — (optional) output size for dimension 0; default 0
     output-size1 — (optional) output size for dimension 1; default 0
     output-size2 — (optional) output size for dimension 2; default 0

   Returns:
     Complex AFArray with 3D frequency spectrum."
  (^AFArray [^AFArray in]
   (assert-within-arrayfire! "fft3")
   (signal/fft3 in))
  (^AFArray [^AFArray in norm-factor]
   (assert-within-arrayfire! "fft3")
   (signal/fft3 in (double norm-factor)))
  (^AFArray [^AFArray in norm-factor output-size0]
   (assert-within-arrayfire! "fft3")
   (signal/fft3 in (double norm-factor) (long output-size0)))
  (^AFArray [^AFArray in norm-factor output-size0 output-size1]
   (assert-within-arrayfire! "fft3")
   (signal/fft3 in (double norm-factor) (long output-size0) (long output-size1)))
  (^AFArray [^AFArray in norm-factor output-size0 output-size1 output-size2]
   (assert-within-arrayfire! "fft3")
   (signal/fft3 in (double norm-factor) (long output-size0) (long output-size1) (long output-size2))))

;;;
;;; Inverse FFT — Out-of-place
;;;

(defn ifft
  "Compute the 1D Inverse Fast Fourier Transform.

   Transforms from frequency domain back to time domain.

   Parameters:
     in          — input complex AFArray (frequency spectrum)
     norm-factor — (optional) normalization factor; default 1.0.
                   Use 1/N for standard inverse normalization.
     output-size — (optional) output length; default 0 (= input length)

   Returns:
     Complex AFArray with time-domain signal.

   Example:
     (ifft freq-spectrum)          ; basic inverse FFT
     (ifft freq-spectrum 0.25)     ; with 1/N normalization for N=4"
  (^AFArray [^AFArray in]
   (assert-within-arrayfire! "ifft")
   (signal/ifft in))
  (^AFArray [^AFArray in norm-factor]
   (assert-within-arrayfire! "ifft")
   (signal/ifft in (double norm-factor)))
  (^AFArray [^AFArray in norm-factor output-size]
   (assert-within-arrayfire! "ifft")
   (signal/ifft in (double norm-factor) (long output-size))))

(defn ifft2
  "Compute the 2D Inverse Fast Fourier Transform.

   Parameters:
     in           — input complex 2D AFArray (frequency spectrum)
     norm-factor  — (optional) normalization factor; default 1.0
     output-size0 — (optional) output size for dimension 0; default 0
     output-size1 — (optional) output size for dimension 1; default 0

   Returns:
     Complex AFArray with 2D spatial-domain signal."
  (^AFArray [^AFArray in]
   (assert-within-arrayfire! "ifft2")
   (signal/ifft2 in))
  (^AFArray [^AFArray in norm-factor]
   (assert-within-arrayfire! "ifft2")
   (signal/ifft2 in (double norm-factor)))
  (^AFArray [^AFArray in norm-factor output-size0]
   (assert-within-arrayfire! "ifft2")
   (signal/ifft2 in (double norm-factor) (long output-size0)))
  (^AFArray [^AFArray in norm-factor output-size0 output-size1]
   (assert-within-arrayfire! "ifft2")
   (signal/ifft2 in (double norm-factor) (long output-size0) (long output-size1))))

(defn ifft3
  "Compute the 3D Inverse Fast Fourier Transform.

   Parameters:
     in           — input complex 3D AFArray (frequency spectrum)
     norm-factor  — (optional) normalization factor; default 1.0
     output-size0 — (optional) output size for dimension 0; default 0
     output-size1 — (optional) output size for dimension 1; default 0
     output-size2 — (optional) output size for dimension 2; default 0

   Returns:
     Complex AFArray with 3D spatial-domain signal."
  (^AFArray [^AFArray in]
   (assert-within-arrayfire! "ifft3")
   (signal/ifft3 in))
  (^AFArray [^AFArray in norm-factor]
   (assert-within-arrayfire! "ifft3")
   (signal/ifft3 in (double norm-factor)))
  (^AFArray [^AFArray in norm-factor output-size0]
   (assert-within-arrayfire! "ifft3")
   (signal/ifft3 in (double norm-factor) (long output-size0)))
  (^AFArray [^AFArray in norm-factor output-size0 output-size1]
   (assert-within-arrayfire! "ifft3")
   (signal/ifft3 in (double norm-factor) (long output-size0) (long output-size1)))
  (^AFArray [^AFArray in norm-factor output-size0 output-size1 output-size2]
   (assert-within-arrayfire! "ifft3")
   (signal/ifft3 in (double norm-factor) (long output-size0) (long output-size1) (long output-size2))))

;;;
;;; Normalized FFT convenience functions
;;;

(defn fft-normalized
  "1D FFT with automatic 1/N normalization.

   Convenience wrapper that applies standard 1/N normalization automatically.
   Useful when you want normalized frequency-domain coefficients.

   For a proper round-trip without normalization loss, use either:
   - `fft-normalized` + `ifft` (no norm), or
   - `fft` (no norm) + `ifft-normalized`

   Parameters:
     in          — input AFArray
     output-size — (optional) output length; default 0 (= input length)

   Returns:
     Complex AFArray with normalized frequency spectrum."
  (^AFArray [^AFArray in]
   (assert-within-arrayfire! "fft-normalized")
   (signal/fft-norm in))
  (^AFArray [^AFArray in output-size]
   (assert-within-arrayfire! "fft-normalized")
   (signal/fft-norm in (long output-size))))

(defn ifft-normalized
  "1D inverse FFT with automatic 1/N normalization.

   Convenience wrapper for inverse FFT with standard normalization.

   Parameters:
     in          — input complex AFArray (frequency spectrum)
     output-size — (optional) output length; default 0 (= input length)

   Returns:
     Complex AFArray with normalized time-domain signal."
  (^AFArray [^AFArray in]
   (assert-within-arrayfire! "ifft-normalized")
   (signal/ifft-norm in))
  (^AFArray [^AFArray in output-size]
   (assert-within-arrayfire! "ifft-normalized")
   (signal/ifft-norm in (long output-size))))

(defn fft2-normalized
  "2D FFT with automatic 1/(N*M) normalization.

   Parameters:
     in           — input 2D AFArray
     output-size0 — (optional) output size for dimension 0; default 0
     output-size1 — (optional) output size for dimension 1; default 0

   Returns:
     Complex AFArray with normalized 2D frequency spectrum."
  (^AFArray [^AFArray in]
   (assert-within-arrayfire! "fft2-normalized")
   (signal/fft2-norm in))
  (^AFArray [^AFArray in output-size0]
   (assert-within-arrayfire! "fft2-normalized")
   (signal/fft2-norm in (long output-size0)))
  (^AFArray [^AFArray in output-size0 output-size1]
   (assert-within-arrayfire! "fft2-normalized")
   (signal/fft2-norm in (long output-size0) (long output-size1))))

(defn ifft2-normalized
  "2D inverse FFT with automatic 1/(N*M) normalization.

   Parameters:
     in           — input complex 2D AFArray (frequency spectrum)
     output-size0 — (optional) output size for dimension 0; default 0
     output-size1 — (optional) output size for dimension 1; default 0

   Returns:
     Complex AFArray with normalized 2D spatial signal."
  (^AFArray [^AFArray in]
   (assert-within-arrayfire! "ifft2-normalized")
   (signal/ifft2-norm in))
  (^AFArray [^AFArray in output-size0]
   (assert-within-arrayfire! "ifft2-normalized")
   (signal/ifft2-norm in (long output-size0)))
  (^AFArray [^AFArray in output-size0 output-size1]
   (assert-within-arrayfire! "ifft2-normalized")
   (signal/ifft2-norm in (long output-size0) (long output-size1))))

(defn fft3-normalized
  "3D FFT with automatic 1/(N*M*P) normalization.

   Parameters:
     in           — input 3D AFArray
     output-size0 — (optional) output size for dimension 0; default 0
     output-size1 — (optional) output size for dimension 1; default 0
     output-size2 — (optional) output size for dimension 2; default 0

   Returns:
     Complex AFArray with normalized 3D frequency spectrum."
  (^AFArray [^AFArray in]
   (assert-within-arrayfire! "fft3-normalized")
   (signal/fft3-norm in))
  (^AFArray [^AFArray in output-size0]
   (assert-within-arrayfire! "fft3-normalized")
   (signal/fft3-norm in (long output-size0)))
  (^AFArray [^AFArray in output-size0 output-size1]
   (assert-within-arrayfire! "fft3-normalized")
   (signal/fft3-norm in (long output-size0) (long output-size1)))
  (^AFArray [^AFArray in output-size0 output-size1 output-size2]
   (assert-within-arrayfire! "fft3-normalized")
   (signal/fft3-norm in (long output-size0) (long output-size1) (long output-size2))))

(defn ifft3-normalized
  "3D inverse FFT with automatic 1/(N*M*P) normalization.

   Parameters:
     in           — input complex 3D AFArray (frequency spectrum)
     output-size0 — (optional) output size for dimension 0; default 0
     output-size1 — (optional) output size for dimension 1; default 0
     output-size2 — (optional) output size for dimension 2; default 0

   Returns:
     Complex AFArray with normalized 3D spatial signal."
  (^AFArray [^AFArray in]
   (assert-within-arrayfire! "ifft3-normalized")
   (signal/ifft3-norm in))
  (^AFArray [^AFArray in output-size0]
   (assert-within-arrayfire! "ifft3-normalized")
   (signal/ifft3-norm in (long output-size0)))
  (^AFArray [^AFArray in output-size0 output-size1]
   (assert-within-arrayfire! "ifft3-normalized")
   (signal/ifft3-norm in (long output-size0) (long output-size1)))
  (^AFArray [^AFArray in output-size0 output-size1 output-size2]
   (assert-within-arrayfire! "ifft3-normalized")
   (signal/ifft3-norm in (long output-size0) (long output-size1) (long output-size2))))

;;;
;;; Real-to-Complex FFT (optimized for real inputs)
;;;

(defn fft-r2c
  "Compute 1D real-to-complex FFT (optimized for real inputs).

   More efficient than standard `fft` for real-valued signals. Output
   is the half-spectrum (size N/2+1) due to conjugate symmetry.

   Parameters:
     in          — input real AFArray
     norm-factor — (optional) normalization factor; default 1.0
     pad0        — (optional) padding for dimension 0; default 0

   Returns:
     Complex AFArray of size (N/2+1) with frequency spectrum."
  (^AFArray [^AFArray in]
   (assert-within-arrayfire! "fft-r2c")
   (signal/fft-r2c in))
  (^AFArray [^AFArray in norm-factor]
   (assert-within-arrayfire! "fft-r2c")
   (signal/fft-r2c in (double norm-factor)))
  (^AFArray [^AFArray in norm-factor pad0]
   (assert-within-arrayfire! "fft-r2c")
   (signal/fft-r2c in (double norm-factor) (long pad0))))

(defn fft2-r2c
  "Compute 2D real-to-complex FFT.

   Parameters:
     in          — input real 2D AFArray
     norm-factor — (optional) normalization factor; default 1.0
     pad0        — (optional) padding for dimension 0; default 0
     pad1        — (optional) padding for dimension 1; default 0

   Returns:
     Complex AFArray with 2D frequency spectrum (half-spectrum in first dimension)."
  (^AFArray [^AFArray in]
   (assert-within-arrayfire! "fft2-r2c")
   (signal/fft2-r2c in))
  (^AFArray [^AFArray in norm-factor]
   (assert-within-arrayfire! "fft2-r2c")
   (signal/fft2-r2c in (double norm-factor)))
  (^AFArray [^AFArray in norm-factor pad0]
   (assert-within-arrayfire! "fft2-r2c")
   (signal/fft2-r2c in (double norm-factor) (long pad0)))
  (^AFArray [^AFArray in norm-factor pad0 pad1]
   (assert-within-arrayfire! "fft2-r2c")
   (signal/fft2-r2c in (double norm-factor) (long pad0) (long pad1))))

(defn fft3-r2c
  "Compute 3D real-to-complex FFT.

   Parameters:
     in          — input real 3D AFArray
     norm-factor — (optional) normalization factor; default 1.0
     pad0        — (optional) padding for dimension 0; default 0
     pad1        — (optional) padding for dimension 1; default 0
     pad2        — (optional) padding for dimension 2; default 0

   Returns:
     Complex AFArray with 3D frequency spectrum."
  (^AFArray [^AFArray in]
   (assert-within-arrayfire! "fft3-r2c")
   (signal/fft3-r2c in))
  (^AFArray [^AFArray in norm-factor]
   (assert-within-arrayfire! "fft3-r2c")
   (signal/fft3-r2c in (double norm-factor)))
  (^AFArray [^AFArray in norm-factor pad0]
   (assert-within-arrayfire! "fft3-r2c")
   (signal/fft3-r2c in (double norm-factor) (long pad0)))
  (^AFArray [^AFArray in norm-factor pad0 pad1]
   (assert-within-arrayfire! "fft3-r2c")
   (signal/fft3-r2c in (double norm-factor) (long pad0) (long pad1)))
  (^AFArray [^AFArray in norm-factor pad0 pad1 pad2]
   (assert-within-arrayfire! "fft3-r2c")
   (signal/fft3-r2c in (double norm-factor) (long pad0) (long pad1) (long pad2))))

;;;
;;; Complex-to-Real FFT (inverse of R2C)
;;;

(defn fft-c2r
  "Compute 1D complex-to-real inverse FFT.

   Inverse of `fft-r2c`. Transforms half-spectrum back to real signal.

   Parameters:
     in          — input complex half-spectrum AFArray (size N/2+1)
     norm-factor — (optional) normalization factor; default 1.0
     odd?        — was the original signal length odd? Default false.

   Returns:
     Real AFArray with time-domain signal."
  (^AFArray [^AFArray in odd?]
   (assert-within-arrayfire! "fft-c2r")
   (signal/fft-c2r in odd?))
  (^AFArray [^AFArray in norm-factor odd?]
   (assert-within-arrayfire! "fft-c2r")
   (signal/fft-c2r in (double norm-factor) odd?)))

(defn fft2-c2r
  "Compute 2D complex-to-real inverse FFT.

   Inverse of `fft2-r2c`.

   Parameters:
     in          — input complex 2D half-spectrum AFArray
     norm-factor — (optional) normalization factor; default 1.0
     odd?        — was the original signal's first dimension odd? Default false.

   Returns:
     Real AFArray with 2D spatial-domain signal."
  (^AFArray [^AFArray in odd?]
   (assert-within-arrayfire! "fft2-c2r")
   (signal/fft2-c2r in odd?))
  (^AFArray [^AFArray in norm-factor odd?]
   (assert-within-arrayfire! "fft2-c2r")
   (signal/fft2-c2r in (double norm-factor) odd?)))

(defn fft3-c2r
  "Compute 3D complex-to-real inverse FFT.

   Inverse of `fft3-r2c`.

   Parameters:
     in          — input complex 3D half-spectrum AFArray
     norm-factor — (optional) normalization factor; default 1.0
     odd?        — was the original signal's first dimension odd? Default false.

   Returns:
     Real AFArray with 3D spatial-domain signal."
  (^AFArray [^AFArray in odd?]
   (assert-within-arrayfire! "fft3-c2r")
   (signal/fft3-c2r in odd?))
  (^AFArray [^AFArray in norm-factor odd?]
   (assert-within-arrayfire! "fft3-c2r")
   (signal/fft3-c2r in (double norm-factor) odd?)))

;;;
;;; In-place FFT (memory-efficient, input must be complex)
;;;

(defn fft!
  "Compute 1D FFT in-place (overwrites input).

   Memory-efficient version that modifies the input array directly.
   Input must be of complex type.

   Parameters:
     in          — input/output complex AFArray (modified in-place)
     norm-factor — (optional) normalization factor; default 1.0

   Returns:
     The modified input AFArray."
  (^AFArray [^AFArray in]
   (assert-within-arrayfire! "fft!")
   (signal/fft! in))
  (^AFArray [^AFArray in norm-factor]
   (assert-within-arrayfire! "fft!")
   (signal/fft! in (double norm-factor))))

(defn ifft!
  "Compute 1D inverse FFT in-place.

   Parameters:
     in          — input/output complex AFArray (modified in-place)
     norm-factor — (optional) normalization factor; default 1.0

   Returns:
     The modified input AFArray."
  (^AFArray [^AFArray in]
   (assert-within-arrayfire! "ifft!")
   (signal/ifft! in))
  (^AFArray [^AFArray in norm-factor]
   (assert-within-arrayfire! "ifft!")
   (signal/ifft! in (double norm-factor))))

(defn fft2!
  "Compute 2D FFT in-place (overwrites input).

   Parameters:
     in          — input/output complex 2D AFArray (modified in-place)
     norm-factor — (optional) normalization factor; default 1.0

   Returns:
     The modified input AFArray."
  (^AFArray [^AFArray in]
   (assert-within-arrayfire! "fft2!")
   (signal/fft2! in))
  (^AFArray [^AFArray in norm-factor]
   (assert-within-arrayfire! "fft2!")
   (signal/fft2! in (double norm-factor))))

(defn ifft2!
  "Compute 2D inverse FFT in-place.

   Parameters:
     in          — input/output complex 2D AFArray (modified in-place)
     norm-factor — (optional) normalization factor; default 1.0

   Returns:
     The modified input AFArray."
  (^AFArray [^AFArray in]
   (assert-within-arrayfire! "ifft2!")
   (signal/ifft2! in))
  (^AFArray [^AFArray in norm-factor]
   (assert-within-arrayfire! "ifft2!")
   (signal/ifft2! in (double norm-factor))))

(defn fft3!
  "Compute 3D FFT in-place (overwrites input).

   Parameters:
     in          — input/output complex 3D AFArray (modified in-place)
     norm-factor — (optional) normalization factor; default 1.0

   Returns:
     The modified input AFArray."
  (^AFArray [^AFArray in]
   (assert-within-arrayfire! "fft3!")
   (signal/fft3! in))
  (^AFArray [^AFArray in norm-factor]
   (assert-within-arrayfire! "fft3!")
   (signal/fft3! in (double norm-factor))))

(defn ifft3!
  "Compute 3D inverse FFT in-place.

   Parameters:
     in          — input/output complex 3D AFArray (modified in-place)
     norm-factor — (optional) normalization factor; default 1.0

   Returns:
     The modified input AFArray."
  (^AFArray [^AFArray in]
   (assert-within-arrayfire! "ifft3!")
   (signal/ifft3! in))
  (^AFArray [^AFArray in norm-factor]
   (assert-within-arrayfire! "ifft3!")
   (signal/ifft3! in (double norm-factor))))

;;;
;;; Convolution
;;;

(defn convolve1
  "Compute 1D convolution of a signal with a filter kernel.

   Automatically selects spatial or frequency domain algorithm based on
   input sizes when domain is `:auto`.

   Parameters:
     signal  — input signal AFArray
     filter  — filter kernel AFArray
     mode    — (optional) convolution mode keyword: `:default` or `:expand`;
               default `:default`
     domain  — (optional) computation domain keyword: `:auto`, `:spatial`,
               or `:freq`; default `:auto`

   Returns:
     Convolved signal AFArray.

   Example:
     (convolve1 signal kernel)
     (convolve1 signal kernel :expand :freq)"
  (^AFArray [^AFArray signal ^AFArray filter]
   (assert-within-arrayfire! "convolve1")
   (signal/convolve1 signal filter))
  (^AFArray [^AFArray signal ^AFArray filter mode]
   (assert-within-arrayfire! "convolve1")
   (signal/convolve1 signal filter (resolve-conv-mode mode)))
  (^AFArray [^AFArray signal ^AFArray filter mode domain]
   (assert-within-arrayfire! "convolve1")
   (signal/convolve1 signal filter (resolve-conv-mode mode) (resolve-conv-domain domain))))

(defn convolve2
  "Compute 2D convolution of a signal with a filter kernel.

   Used for image filtering, edge detection, blurring, etc.

   Parameters:
     signal  — input 2D signal AFArray
     filter  — 2D filter kernel AFArray
     mode    — (optional) convolution mode keyword; default `:default`
     domain  — (optional) computation domain keyword; default `:auto`

   Returns:
     Convolved 2D signal AFArray.

   Example:
     (convolve2 image gaussian-kernel)
     (convolve2 image sobel-x :expand :spatial)"
  (^AFArray [^AFArray signal ^AFArray filter]
   (assert-within-arrayfire! "convolve2")
   (signal/convolve2 signal filter))
  (^AFArray [^AFArray signal ^AFArray filter mode]
   (assert-within-arrayfire! "convolve2")
   (signal/convolve2 signal filter (resolve-conv-mode mode)))
  (^AFArray [^AFArray signal ^AFArray filter mode domain]
   (assert-within-arrayfire! "convolve2")
   (signal/convolve2 signal filter (resolve-conv-mode mode) (resolve-conv-domain domain))))

(defn convolve3
  "Compute 3D convolution of a signal with a filter kernel.

   Parameters:
     signal  — input 3D signal AFArray
     filter  — 3D filter kernel AFArray
     mode    — (optional) convolution mode keyword; default `:default`
     domain  — (optional) computation domain keyword; default `:auto`

   Returns:
     Convolved 3D signal AFArray."
  (^AFArray [^AFArray signal ^AFArray filter]
   (assert-within-arrayfire! "convolve3")
   (signal/convolve3 signal filter))
  (^AFArray [^AFArray signal ^AFArray filter mode]
   (assert-within-arrayfire! "convolve3")
   (signal/convolve3 signal filter (resolve-conv-mode mode)))
  (^AFArray [^AFArray signal ^AFArray filter mode domain]
   (assert-within-arrayfire! "convolve3")
   (signal/convolve3 signal filter (resolve-conv-mode mode) (resolve-conv-domain domain))))

(defn convolve2-sep
  "Compute 2D separable convolution (faster for separable kernels).

   Separable convolution applies row and column filters independently,
   which is much faster than full 2D convolution when the 2D kernel can
   be expressed as the outer product of two 1D filters.

   Parameters:
     col-filter — column filter (1D AFArray)
     row-filter — row filter (1D AFArray)
     signal     — input 2D signal AFArray
     mode       — (optional) convolution mode keyword; default `:default`

   Returns:
     Convolved 2D signal AFArray.

   Example:
     (convolve2-sep gauss-1d gauss-1d image)"
  (^AFArray [^AFArray col-filter ^AFArray row-filter ^AFArray signal]
   (assert-within-arrayfire! "convolve2-sep")
   (signal/convolve2-sep col-filter row-filter signal))
  (^AFArray [^AFArray col-filter ^AFArray row-filter ^AFArray signal mode]
   (assert-within-arrayfire! "convolve2-sep")
   (signal/convolve2-sep col-filter row-filter signal (resolve-conv-mode mode))))

(defn convolve2-nn
  "Compute 2D convolution optimized for neural networks.

   Performs 2D correlation (standard in CNNs) with stride, padding, and
   dilation support.

   Parameters:
     signal    — input AFArray [height x width x channels x batch]
     filter    — filter AFArray [kernel_h x kernel_w x channels x num_filters]
     strides   — stride values [stride_h stride_w]
     paddings  — padding values [pad_h pad_w]
     dilations — dilation values [dilation_h dilation_w]

   Returns:
     Output AFArray [out_h x out_w x num_filters x batch].

   Output dimensions:
     out_h = floor((height + 2*pad_h - dil_h*(kernel_h-1) - 1) / stride_h) + 1
     out_w = floor((width  + 2*pad_w - dil_w*(kernel_w-1) - 1) / stride_w) + 1

   Example:
     (convolve2-nn input weights [1 1] [1 1] [1 1])"
  ^AFArray [^AFArray signal ^AFArray filter strides paddings dilations]
  (assert-within-arrayfire! "convolve2-nn")
  (signal/convolve2-nn signal filter strides paddings dilations))

;;;
;;; FFT-based convolution (always frequency domain)
;;;

(defn fft-convolve1
  "Compute 1D convolution using FFT (frequency domain).

   Always uses the frequency domain algorithm. Faster than spatial domain
   convolution for large filters (typically > 32 elements).

   Parameters:
     signal — input signal AFArray
     filter — filter kernel AFArray
     mode   — (optional) convolution mode keyword; default `:default`

   Returns:
     Convolved signal AFArray."
  (^AFArray [^AFArray signal ^AFArray filter]
   (assert-within-arrayfire! "fft-convolve1")
   (signal/fft-convolve1 signal filter))
  (^AFArray [^AFArray signal ^AFArray filter mode]
   (assert-within-arrayfire! "fft-convolve1")
   (signal/fft-convolve1 signal filter (resolve-conv-mode mode))))

(defn fft-convolve2
  "Compute 2D convolution using FFT (frequency domain).

   Parameters:
     signal — input 2D signal AFArray
     filter — 2D filter kernel AFArray
     mode   — (optional) convolution mode keyword; default `:default`

   Returns:
     Convolved 2D signal AFArray."
  (^AFArray [^AFArray signal ^AFArray filter]
   (assert-within-arrayfire! "fft-convolve2")
   (signal/fft-convolve2 signal filter))
  (^AFArray [^AFArray signal ^AFArray filter mode]
   (assert-within-arrayfire! "fft-convolve2")
   (signal/fft-convolve2 signal filter (resolve-conv-mode mode))))

(defn fft-convolve3
  "Compute 3D convolution using FFT (frequency domain).

   Parameters:
     signal — input 3D signal AFArray
     filter — 3D filter kernel AFArray
     mode   — (optional) convolution mode keyword; default `:default`

   Returns:
     Convolved 3D signal AFArray."
  (^AFArray [^AFArray signal ^AFArray filter]
   (assert-within-arrayfire! "fft-convolve3")
   (signal/fft-convolve3 signal filter))
  (^AFArray [^AFArray signal ^AFArray filter mode]
   (assert-within-arrayfire! "fft-convolve3")
   (signal/fft-convolve3 signal filter (resolve-conv-mode mode))))

;;;
;;; Digital Filters
;;;

(defn iir
  "Apply an Infinite Impulse Response (IIR) digital filter.

   IIR filters have feedback and infinite impulse response. Implements
   the transfer function H(z) = B(z)/A(z).

   Parameters:
     b — feedforward coefficients AFArray (numerator)
     a — feedback coefficients AFArray (denominator; first element typically 1.0)
     x — input signal AFArray

   Returns:
     Filtered signal AFArray.

   Example:
     (iir b-coeffs a-coeffs signal)"
  ^AFArray [^AFArray b ^AFArray a ^AFArray x]
  (assert-within-arrayfire! "iir")
  (signal/iir b a x))

;;;
;;; Median Filtering
;;;

(defn median-filter1
  "Apply 1D median filter for noise reduction.

   Median filtering is a nonlinear filter that replaces each element
   with the median of its neighborhood. Excellent for spike/impulse
   noise removal while preserving edges.

   Parameters:
     in        — input 1D AFArray
     window    — (optional) window width; default 3
     edge-pad  — (optional) border handling keyword: `:zero`, `:sym`,
                 `:clamp-to-edge`, `:periodic`; default `:zero`

   Returns:
     Median-filtered AFArray.

   Example:
     (median-filter1 noisy-signal)
     (median-filter1 noisy-signal 5 :sym)"
  (^AFArray [^AFArray in]
   (assert-within-arrayfire! "median-filter1")
   (signal/medfilt1 in))
  (^AFArray [^AFArray in window]
   (assert-within-arrayfire! "median-filter1")
   (signal/medfilt1 in (long window)))
  (^AFArray [^AFArray in window edge-pad]
   (assert-within-arrayfire! "median-filter1")
   (signal/medfilt1 in (long window) (resolve-edge-pad edge-pad))))

(defn median-filter2
  "Apply 2D median filter for noise reduction.

   Replaces each pixel with the median of its rectangular neighborhood.
   Excellent for salt-and-pepper noise removal while preserving edges.

   Parameters:
     in          — input 2D AFArray
     wind-length — (optional) window height; default 3
     wind-width  — (optional) window width; default 3
     edge-pad    — (optional) border handling keyword: `:zero`, `:sym`,
                   `:clamp-to-edge`, `:periodic`; default `:zero`

   Returns:
     Median-filtered AFArray.

   Example:
     (median-filter2 noisy-image)
     (median-filter2 noisy-image 5 5 :sym)"
  (^AFArray [^AFArray in]
   (assert-within-arrayfire! "median-filter2")
   (signal/medfilt in))
  (^AFArray [^AFArray in wind-length]
   (assert-within-arrayfire! "median-filter2")
   (signal/medfilt in (long wind-length)))
  (^AFArray [^AFArray in wind-length wind-width]
   (assert-within-arrayfire! "median-filter2")
   (signal/medfilt in (long wind-length) (long wind-width)))
  (^AFArray [^AFArray in wind-length wind-width edge-pad]
   (assert-within-arrayfire! "median-filter2")
   (signal/medfilt in (long wind-length) (long wind-width) (resolve-edge-pad edge-pad))))

;;;
;;; Interpolation / Approximation
;;;

(defn approx1
  "1D interpolation at specified positions.

   Interpolates values from input array at positions specified by `xo`.

   Parameters:
     yi       — input values AFArray
     xo       — output positions AFArray to interpolate at
     method   — (optional) interpolation method keyword: `:nearest`, `:linear`,
                `:cubic`, etc.; default `:linear`
     off-grid — (optional) value for out-of-bounds positions; default 0.0

   Returns:
     Interpolated values AFArray at positions `xo`.

   Example:
     (approx1 values positions)
     (approx1 values positions :cubic 0.0)"
  (^AFArray [^AFArray yi ^AFArray xo]
   (assert-within-arrayfire! "approx1")
   (signal/approx1 yi xo))
  (^AFArray [^AFArray yi ^AFArray xo method]
   (assert-within-arrayfire! "approx1")
   (signal/approx1 yi xo (resolve-interp method)))
  (^AFArray [^AFArray yi ^AFArray xo method off-grid]
   (assert-within-arrayfire! "approx1")
   (signal/approx1 yi xo (resolve-interp method) (float off-grid))))

(defn approx1-uniform
  "1D interpolation on a uniform grid.

   More efficient than `approx1` when the input is uniformly spaced.

   Parameters:
     yi       — input values AFArray
     xo       — output positions AFArray
     xdim     — (optional) dimension along which to interpolate; default 0
     xi-beg   — (optional) start of input grid; default 0.0
     xi-step  — (optional) step size of input grid; default 1.0
     method   — (optional) interpolation method keyword; default `:linear`
     off-grid — (optional) value for out-of-bounds positions; default 0.0

   Returns:
     Interpolated values AFArray."
  (^AFArray [^AFArray yi ^AFArray xo]
   (assert-within-arrayfire! "approx1-uniform")
   (signal/approx1-uniform yi xo))
  (^AFArray [^AFArray yi ^AFArray xo xdim]
   (assert-within-arrayfire! "approx1-uniform")
   (signal/approx1-uniform yi xo (int xdim)))
  (^AFArray [^AFArray yi ^AFArray xo xdim xi-beg xi-step]
   (assert-within-arrayfire! "approx1-uniform")
   (signal/approx1-uniform yi xo (int xdim) (double xi-beg) (double xi-step)))
  (^AFArray [^AFArray yi ^AFArray xo xdim xi-beg xi-step method]
   (assert-within-arrayfire! "approx1-uniform")
   (signal/approx1-uniform yi xo (int xdim) (double xi-beg) (double xi-step) (resolve-interp method)))
  (^AFArray [^AFArray yi ^AFArray xo xdim xi-beg xi-step method off-grid]
   (assert-within-arrayfire! "approx1-uniform")
   (signal/approx1-uniform yi xo (int xdim) (double xi-beg) (double xi-step) (resolve-interp method) (float off-grid))))

(defn approx2
  "2D interpolation at specified positions.

   Interpolates values from a 2D input array at 2D positions (xo, yo).

   Parameters:
     zi       — input 2D values AFArray
     xo       — output x-positions AFArray
     yo       — output y-positions AFArray
     method   — (optional) interpolation method keyword; default `:bilinear`
     off-grid — (optional) value for out-of-bounds positions; default 0.0

   Returns:
     Interpolated 2D values AFArray.

   Example:
     (approx2 image new-x-coords new-y-coords)
     (approx2 image new-x-coords new-y-coords :bicubic 0.0)"
  (^AFArray [^AFArray zi ^AFArray xo ^AFArray yo]
   (assert-within-arrayfire! "approx2")
   (signal/approx2 zi xo yo))
  (^AFArray [^AFArray zi ^AFArray xo ^AFArray yo method]
   (assert-within-arrayfire! "approx2")
   (signal/approx2 zi xo yo (resolve-interp method)))
  (^AFArray [^AFArray zi ^AFArray xo ^AFArray yo method off-grid]
   (assert-within-arrayfire! "approx2")
   (signal/approx2 zi xo yo (resolve-interp method) (float off-grid))))

(defn approx2-uniform
  "2D interpolation on a uniform grid.

   More efficient than `approx2` for uniformly-spaced 2D grids.

   Parameters:
     zi       — input 2D values AFArray
     xo       — output x-positions AFArray
     xdim     — x-dimension for interpolation (default 0)
     xi-beg   — start of input x-grid (default 0.0)
     xi-step  — step size of input x-grid (default 1.0)
     yo       — output y-positions AFArray
     ydim     — y-dimension for interpolation (default 1)
     yi-beg   — start of input y-grid (default 0.0)
     yi-step  — step size of input y-grid (default 1.0)
     method   — (optional) interpolation method keyword; default `:bilinear`
     off-grid — (optional) value for out-of-bounds positions; default 0.0

   Returns:
     Interpolated 2D values AFArray."
  (^AFArray [^AFArray zi ^AFArray xo ^AFArray yo]
   (assert-within-arrayfire! "approx2-uniform")
   (signal/approx2-uniform zi xo yo))
  (^AFArray [^AFArray zi ^AFArray xo xdim xi-beg xi-step ^AFArray yo ydim yi-beg yi-step]
   (assert-within-arrayfire! "approx2-uniform")
   (signal/approx2-uniform zi xo (int xdim) (double xi-beg) (double xi-step)
                            yo (int ydim) (double yi-beg) (double yi-step)))
  (^AFArray [^AFArray zi ^AFArray xo xdim xi-beg xi-step ^AFArray yo ydim yi-beg yi-step method]
   (assert-within-arrayfire! "approx2-uniform")
   (signal/approx2-uniform zi xo (int xdim) (double xi-beg) (double xi-step)
                            yo (int ydim) (double yi-beg) (double yi-step)
                            (resolve-interp method)))
  (^AFArray [^AFArray zi ^AFArray xo xdim xi-beg xi-step ^AFArray yo ydim yi-beg yi-step method off-grid]
   (assert-within-arrayfire! "approx2-uniform")
   (signal/approx2-uniform zi xo (int xdim) (double xi-beg) (double xi-step)
                            yo (int ydim) (double yi-beg) (double yi-step)
                            (resolve-interp method) (float off-grid))))


(comment
  ;; Signal processing REPL experiments
  ;; All examples must be called inside (with-arrayfire ...).
  (require '[org.soulspace.arrayfire.api.core :as af] :reload)
  (require '[org.soulspace.arrayfire.api.signal-processing :as sp] :reload)

  ;; --- 1D FFT round-trip ---
  ;; Signal: [1 2 3 4], FFT then IFFT with 1/N normalization
  (af/with-arrayfire
    (let [s (af/array [1.0 2.0 3.0 4.0] [4] :f32)
          freq (sp/fft s)
          reconstructed (sp/ifft freq 0.25)]
      {:shape (af/shape freq)
       :complex? (af/complex-array? freq)}))
  ;; => {:shape [4], :complex? true}

  ;; --- 2D FFT ---
  (af/with-arrayfire
    (let [img (af/array (vec (map float (range 16))) [4 4] :f32)
          freq (sp/fft2 img)]
      (af/shape freq)))
  ;; => [4 4]

  ;; --- Normalized FFT round-trip ---
  (af/with-arrayfire
    (let [s (af/array [1.0 0.0 1.0 0.0] [4] :f32)
          freq (sp/fft-normalized s)]
      (af/shape freq)))

  ;; --- 1D convolution with averaging filter ---
  (af/with-arrayfire
    (let [signal (af/array [0.0 0.0 1.0 0.0 0.0] [5] :f32)
          kernel (af/array [0.333 0.334 0.333] [3] :f32)]
      (af/->value (sp/convolve1 signal kernel))))

  ;; --- 2D convolution (identity filter) ---
  (af/with-arrayfire
    (let [img (af/array [1.0 2.0 3.0 4.0] [2 2] :f32)
          kernel (af/array [0.0 0.0 0.0
                            0.0 1.0 0.0
                            0.0 0.0 0.0] [3 3] :f32)]
      (af/->value (sp/convolve2 img kernel))))

  ;; --- Median filter ---
  (af/with-arrayfire
    (let [noisy (af/array [1.0 100.0 2.0 3.0 4.0] [5] :f32)]
      (af/->value (sp/median-filter1 noisy 3))))

  ;; --- 1D interpolation ---
  (af/with-arrayfire
    (let [yi (af/array [0.0 1.0 2.0 3.0] [4] :f32)
          xo (af/array [0.5 1.5 2.5] [3] :f32)]
      (af/->value (sp/approx1 yi xo :linear))))
  ;; => [0.5 1.5 2.5]

  ;
  )