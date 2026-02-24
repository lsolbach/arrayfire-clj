(ns org.soulspace.arrayfire.api.image-processing
  "Idiomatic Clojure image processing API for ArrayFire arrays.

   Provides GPU-accelerated image processing operations including:

   Edge and Gradient Detection:
   - `gradient`               — finite-difference image gradients [dx dy]
   - `sobel`                  — Sobel edge detection gradients [dx dy]
   - `canny`                  — Canny edge detection

   Geometric Transformations:
   - `resize`                 — resize to new dimensions
   - `rotate`                 — rotate by angle (radians)
   - `translate`              — shift image
   - `scale`                  — scale by factors
   - `skew`                   — skew transform
   - `transform`              — general affine transformation
   - `transform-coordinates`  — transform coordinates through matrix

   Morphological Operations:
   - `dilate`, `dilate3`      — morphological dilation (2D/3D)
   - `erode`, `erode3`        — morphological erosion (2D/3D)

   Filtering:
   - `bilateral`              — edge-preserving bilateral filter
   - `mean-shift`             — mean shift filter / segmentation
   - `min-filter`             — minimum (erosion-like) filter
   - `max-filter`             — maximum (dilation-like) filter
   - `gaussian-kernel`        — generate Gaussian convolution kernel
   - `anisotropic-diffusion`  — Perona-Malik edge-preserving denoising

   Histogram Operations:
   - `histogram`              — compute histogram
   - `histogram-equalize`     — histogram equalization

   Color Space Conversions:
   - `rgb->gray`, `gray->rgb`
   - `rgb->hsv`, `hsv->rgb`
   - `rgb->ycbcr`, `ycbcr->rgb`
   - `convert-color-space`    — generic color space conversion

   Connected Components and Segmentation:
   - `regions`                — label connected components
   - `confidence-connected`   — confidence-connected region growing

   Patch Operations:
   - `unwrap`                 — extract patches into columns
   - `wrap`                   — reconstruct from patches (inverse of unwrap)

   Integral Image:
   - `summed-area-table`      — compute integral image (SAT)

   Deconvolution:
   - `iterative-deconv`       — iterative deconvolution (Landweber, Richardson-Lucy)
   - `inverse-deconv`         — inverse filter deconvolution (Tikhonov)

   Image I/O:
   - `load-image`, `save-image`
   - `load-image-native`, `save-image-native`

   Image Moments:
   - `moments`                — compute image moments (M00, M01, M10, M11)
   - `moments-all`            — moments as a map (single image convenience)
   - `centroid`               — compute centroid (center of mass)
   - `image-area`             — compute total area / intensity sum

   ## Interpolation methods (for geometric transforms)

   | Keyword            | Description                           |
   |--------------------|---------------------------------------|
   | `:nearest`         | Nearest neighbor                      |
   | `:linear`          | Linear interpolation                  |
   | `:bilinear`        | Bilinear (2D)                         |
   | `:cubic`           | Cubic spline                          |
   | `:lower`           | Floor rounding                        |
   | `:linear-cosine`   | Linear with cosine smoothing          |
   | `:bilinear-cosine` | Bilinear with cosine smoothing        |
   | `:bicubic`         | Bicubic                               |
   | `:cubic-spline`    | Cubic spline                          |
   | `:bicubic-spline`  | Bicubic spline                        |

   ## Edge padding modes (for filters)

   | Keyword          | Description                             |
   |------------------|-----------------------------------------|
   | `:zero`          | Pad with zeros                          |
   | `:sym`           | Symmetric (mirror) padding              |
   | `:clamp-to-edge` | Clamp to border values                  |
   | `:periodic`      | Wrap-around (periodic) padding          |

   ## Canny threshold types

   | Keyword      | Description                              |
   |--------------|------------------------------------------|
   | `:manual`    | Manual threshold values                  |
   | `:auto-otsu` | Automatic Otsu-based thresholds          |

   ## Flux function types (for anisotropic diffusion)

   | Keyword       | Description                             |
   |---------------|-----------------------------------------|
   | `:exponential`| Exponential flux function               |
   | `:quadratic`  | Quadratic flux function                 |

   ## Diffusion equation types

   | Keyword  | Description                                |
   |----------|--------------------------------------------|
   | `:grad`  | Gradient-based diffusion                   |
   | `:mcde`  | Modified curvature diffusion equation      |

   ## Deconvolution algorithms

   Iterative: `:landweber`, `:richardson-lucy`
   Inverse:   `:tikhonov`

   ## Color spaces

   | Keyword  | Description                                |
   |----------|--------------------------------------------|
   | `:gray`  | Grayscale                                  |
   | `:rgb`   | Red-Green-Blue                             |
   | `:hsv`   | Hue-Saturation-Value                       |
   | `:ycbcr` | Luma-Chroma (YCbCr)                        |

   ## YCbCr standards

   | Keyword   | Description                               |
   |-----------|-------------------------------------------|
   | `:bt601`  | BT.601 (SD video)                         |
   | `:bt709`  | BT.709 (HD video)                         |
   | `:bt2020` | BT.2020 (UHD video)                       |

   ## Connectivity types (for region labeling)

   | Keyword | Description                                 |
   |---------|---------------------------------------------|
   | `:4`    | 4-connected (cardinal neighbors)            |
   | `:8`    | 8-connected (cardinal + diagonal neighbors) |

   ## Moment types

   | Keyword        | Description                             |
   |----------------|-----------------------------------------|
   | `:m00`         | Zeroth moment (total mass/area)         |
   | `:m01`         | First moment about y-axis               |
   | `:m10`         | First moment about x-axis               |
   | `:m11`         | Second mixed moment                     |
   | `:first-order` | All four first-order moments            |

   All functions must be called within a `with-arrayfire` region from
   `org.soulspace.arrayfire.api.core`."
  (:require [org.soulspace.arrayfire.api.core :as core :refer [assert-within-arrayfire!]]
            [org.soulspace.arrayfire.integration.base.definitions :as defs]
            [org.soulspace.arrayfire.integration.unified-api.image :as image]
            [org.soulspace.arrayfire.integration.unified-api.moments :as moments])
  (:import (org.soulspace.arrayfire.integration.base.resource AFArray)))

;;; TODO refactor arguments of image processing functions to use 
;;;      positional args for required parameters (e.g. image array) and
;;;      keyword args / arg-maps (with :or defaults) for optional parameters

;;;
;;; TODO return image from functions returning nil (e.g. save-image) for chaining
;;;

;;;
;;; Edge and Gradient Detection
;;;

(defn gradient
  "Compute image gradients using finite differences.

   Calculates the horizontal (dx) and vertical (dy) gradients of an image
   using finite difference approximations.

   Parameters:
   - in — input image (AFArray)

   Returns:
   Vector `[dx dy]` where dx and dy are gradient AFArrays.

   Example:
   (let [[dx dy] (gradient img)]
     ;; dx: horizontal gradient, dy: vertical gradient
     )"
  [^AFArray in]
  (assert-within-arrayfire! "gradient")
  (image/gradient in))

(defn sobel
  "Compute Sobel edge detection gradients.

   Applies the Sobel operator to compute image gradients for edge detection.

   Parameters:
   - img      — input grayscale image (AFArray)
   - ker-size — (optional) kernel size, must be odd; default 3

   Returns:
   Vector `[dx dy]` where dx and dy are Sobel gradient AFArrays.

   Example:
   (let [[dx dy] (sobel img)]
     ;; combine into edge magnitude
     )"
  ([^AFArray img]
   (sobel img 3))
  ([^AFArray img ker-size]
   (assert-within-arrayfire! "sobel")
   (image/sobel img (int ker-size))))

(defn canny
  "Canny edge detection.

   Performs multi-stage edge detection: noise reduction, gradient calculation,
   non-maximum suppression, and hysteresis thresholding.

   Parameters:
   - in              — input grayscale image (AFArray)
   - threshold-type  — (optional) `:manual` or `:auto-otsu`; default `:manual`
   - low-threshold   — (optional) low threshold value; default 0.1
   - high-threshold  — (optional) high threshold value; default 0.3
   - sobel-window    — (optional) Sobel kernel size; default 3
   - fast?           — (optional) use fast mode; default true

   Returns:
   Binary edge map (AFArray) where edges are 1.

   Example:
   (canny img)                          ; auto defaults
   (canny img :auto-otsu)               ; automatic thresholds
   (canny img :manual 0.05 0.15 3 true) ; full control"
  ([^AFArray in]
   (canny in :manual 0.1 0.3 3 true))
  ([^AFArray in threshold-type]
   (canny in threshold-type 0.1 0.3 3 true))
  ([^AFArray in threshold-type low-threshold high-threshold]
   (canny in threshold-type low-threshold high-threshold 3 true))
  ([^AFArray in threshold-type low-threshold high-threshold sobel-window fast?]
   (assert-within-arrayfire! "canny")
   (image/canny in (defs/resolve-canny-threshold threshold-type)
                low-threshold high-threshold sobel-window fast?)))

;;;
;;; Image I/O
;;;

(defn load-image
  "Load an image from file.

   Supports common formats (PNG, JPEG, BMP, etc.).

   Parameters:
   - filename — path to image file (string)
   - color?   — (optional) load as color (true) or grayscale (false); default true

   Returns:
   AFArray containing the loaded image.

   Example:
   (load-image \"photo.jpg\")
   (load-image \"photo.jpg\" false) ; grayscale"
  ([filename]
   (load-image filename true))
  ([filename color?]
   (assert-within-arrayfire! "load-image")
   (image/load-image filename color?)))

(defn save-image
  "Save an image to file.

   Format is determined by file extension.

   Parameters:
   - filename — destination path (string)
   - img      — image to save (AFArray)

   Returns:
     nil

   Example:
   (save-image \"output.png\" processed-img)"
  [filename ^AFArray img]
  (assert-within-arrayfire! "save-image")
  (image/save-image filename img))

(defn load-image-native
  "Load an image in its native format without conversion.

   Parameters:
   - filename — path to image file (string)

   Returns:
   AFArray containing the image in native format."
  [filename]
  (assert-within-arrayfire! "load-image-native")
  (image/load-image-native filename))

(defn save-image-native
  "Save an image in its native format without conversion.

   Parameters:
   - filename — destination path (string)
   - img      — image to save (AFArray)

   Returns:
   nil"
  [filename ^AFArray img]
  (assert-within-arrayfire! "save-image-native")
  (image/save-image-native filename img))

;;;
;;; Geometric Transformations
;;;

(defn resize
  "Resize an image to new dimensions.

   Parameters:
   - img    — input image (AFArray)
   - height — output height (long)
   - width  — output width (long)
   - method — (optional) interpolation method keyword; default `:nearest`

   Returns:
   Resized image (AFArray).

   Example:
   (resize img 512 512)
   (resize img 256 256 :bilinear)"
  ([^AFArray img height width]
   (resize img height width :nearest))
  ([^AFArray img height width method]
   (assert-within-arrayfire! "resize")
   (image/resize img (long height) (long width) (defs/resolve-interp method))))

(defn rotate
  "Rotate an image by an angle.

   Parameters:
   - img    — input image (AFArray)
   - theta  — rotation angle in radians (double)
   - crop?  — (optional) crop to original dimensions; default true
   - method — (optional) interpolation method keyword; default `:nearest`

   Returns:
   Rotated image (AFArray).

   Example:
   (rotate img (/ Math/PI 4))              ; 45° rotation
   (rotate img (/ Math/PI 2) false :linear) ; 90° without crop"
  ([^AFArray img theta]
   (rotate img theta true :nearest))
  ([^AFArray img theta crop?]
   (rotate img theta crop? :nearest))
  ([^AFArray img theta crop? method]
   (assert-within-arrayfire! "rotate")
   (image/rotate img (float theta) crop? (defs/resolve-interp method))))

(defn translate
  "Translate (shift) an image.

   Parameters:
   - img    — input image (AFArray)
   - trans0 — translation in first dimension (double)
   - trans1 — translation in second dimension (double)
   - height — (optional) output height; default 0 (same as input)
   - width  — (optional) output width; default 0 (same as input)
   - method — (optional) interpolation method keyword; default `:nearest`

   Returns:
   Translated image (AFArray)."
  ([^AFArray img trans0 trans1]
   (translate img trans0 trans1 0 0 :nearest))
  ([^AFArray img trans0 trans1 height width]
   (translate img trans0 trans1 height width :nearest))
  ([^AFArray img trans0 trans1 height width method]
   (assert-within-arrayfire! "translate")
   (image/translate img (float trans0) (float trans1)
                    (long height) (long width) (defs/resolve-interp method))))

(defn scale
  "Scale an image by factors.

   Parameters:
   - img    — input image (AFArray)
   - scale0 — scale factor for dimension 0 (double)
   - scale1 — scale factor for dimension 1 (double)
   - height — (optional) output height; default 0 (computed from scale)
   - width  — (optional) output width; default 0 (computed from scale)
   - method — (optional) interpolation method keyword; default `:nearest`

   Returns:
   Scaled image (AFArray)."
  ([^AFArray img scale0 scale1]
   (scale img scale0 scale1 0 0 :nearest))
  ([^AFArray img scale0 scale1 height width]
   (scale img scale0 scale1 height width :nearest))
  ([^AFArray img scale0 scale1 height width method]
   (assert-within-arrayfire! "scale")
   (image/scale img (float scale0) (float scale1)
                (long height) (long width) (defs/resolve-interp method))))

(defn skew
  "Skew an image.

   Parameters:
   - img      — input image (AFArray)
   - skew0    — skew factor for dimension 0 (double)
   - skew1    — skew factor for dimension 1 (double)
   - height   — (optional) output height; default 0
   -  width    — (optional) output width; default 0
   - method   — (optional) interpolation method keyword; default `:nearest`
   - inverse? — (optional) apply inverse transform; default false

   Returns:
   Skewed image (AFArray)."
  ([^AFArray img skew0 skew1]
   (skew img skew0 skew1 0 0 :nearest false))
  ([^AFArray img skew0 skew1 height width]
   (skew img skew0 skew1 height width :nearest false))
  ([^AFArray img skew0 skew1 height width method inverse?]
   (assert-within-arrayfire! "skew")
   (image/skew img (float skew0) (float skew1)
               (long height) (long width) (defs/resolve-interp method) inverse?)))

(defn transform
  "Apply a general affine transformation to an image.

   Parameters:
   - img              — input image (AFArray)
   - transform-matrix — 3×2 transformation matrix (AFArray)
   - height           — output height (long)
   - width            — output width (long)
   - method           — (optional) interpolation method keyword; default `:nearest`
   - inverse?         — (optional) apply inverse transform; default true

   Returns:
   Transformed image (AFArray)."
  ([^AFArray img ^AFArray transform-matrix height width]
   (transform img transform-matrix height width :nearest true))
  ([^AFArray img ^AFArray transform-matrix height width method inverse?]
   (assert-within-arrayfire! "transform")
   (image/transform img transform-matrix (long height) (long width)
                    (defs/resolve-interp method) inverse?)))

(defn transform-coordinates
  "Transform coordinates using a transformation matrix.

   Parameters:
   - transform-matrix — transformation matrix (AFArray)
   - d0               — coordinate in dimension 0 (double)
   - d1               — coordinate in dimension 1 (double)

   Returns:
   Transformed coordinates (AFArray)."
  [^AFArray transform-matrix d0 d1]
  (assert-within-arrayfire! "transform-coordinates")
  (image/transform-coordinates transform-matrix (float d0) (float d1)))

;;;
;;; Morphological Operations
;;;

(defn dilate
  "Morphological dilation (2D).

   Expands bright regions in an image using a structuring element.

   Parameters:
   - img  — input image (AFArray)
   - mask — structuring element (AFArray)

   Returns:
   Dilated image (AFArray).

   Example:
   (let [se (af/constant 1.0 [3 3] :f32)]
     (dilate binary-img se))"
  ^AFArray [^AFArray img ^AFArray mask]
  (assert-within-arrayfire! "dilate")
  (image/dilate img mask))

(defn dilate3
  "Morphological dilation (3D).

   3D version of dilation for volumetric data.

   Parameters:
   - vol  — input volume (AFArray)
   - mask — 3D structuring element (AFArray)

   Returns:
   Dilated volume (AFArray)."
  ^AFArray [^AFArray vol ^AFArray mask]
  (assert-within-arrayfire! "dilate3")
  (image/dilate3 vol mask))

(defn erode
  "Morphological erosion (2D).

   Shrinks bright regions in an image using a structuring element.

   Parameters:
   - img  — input image (AFArray)
   - mask — structuring element (AFArray)

   Returns:
   Eroded image (AFArray)."
  ^AFArray [^AFArray img ^AFArray mask]
  (assert-within-arrayfire! "erode")
  (image/erode img mask))

(defn erode3
  "Morphological erosion (3D).

   3D version of erosion for volumetric data.

   Parameters:
   - vol  — input volume (AFArray)
   - mask — 3D structuring element (AFArray)

   Returns:
   Eroded volume (AFArray)."
  ^AFArray [^AFArray vol ^AFArray mask]
  (assert-within-arrayfire! "erode3")
  (image/erode3 vol mask))

;;;
;;; Filtering
;;;

(defn bilateral
  "Bilateral filter for edge-preserving smoothing.

   Smooths images while preserving edges by considering both spatial
   and intensity differences.

   Parameters:
   - img             — input image (AFArray)
   - spatial-sigma   — spatial Gaussian standard deviation (double)
   - chromatic-sigma — intensity/color Gaussian standard deviation (double)
   - color?          — (optional) image is color; default false

   Returns:
   Filtered image (AFArray).

   Example:
   (bilateral noisy-img 5.0 25.0)
   (bilateral noisy-color-img 5.0 25.0 true)"
  ([^AFArray img spatial-sigma chromatic-sigma]
   (bilateral img spatial-sigma chromatic-sigma false))
  ([^AFArray img spatial-sigma chromatic-sigma color?]
   (assert-within-arrayfire! "bilateral")
   (image/bilateral img (float spatial-sigma) (float chromatic-sigma) color?)))

(defn mean-shift
  "Mean shift filter for edge-preserving smoothing and segmentation.

   Parameters:
   - img             — input image (AFArray)
   - spatial-sigma   — spatial bandwidth (double)
   - chromatic-sigma — color bandwidth (double)
   - iterations      — (optional) number of iterations; default 10
   - color?          — (optional) image is color; default false

   Returns:
   Filtered image (AFArray)."
  ([^AFArray img spatial-sigma chromatic-sigma]
   (mean-shift img spatial-sigma chromatic-sigma 10 false))
  ([^AFArray img spatial-sigma chromatic-sigma iterations color?]
   (assert-within-arrayfire! "mean-shift")
   (image/mean-shift img (float spatial-sigma) (float chromatic-sigma)
                     (int iterations) color?)))

(defn min-filter
  "Minimum filter.

   Replaces each pixel with the minimum value in its neighborhood.

   Parameters:
   - img         — input image (AFArray)
   - wind-length — (optional) window length; default 3
   - wind-width  — (optional) window width; default 3
   - edge-pad    — (optional) border padding keyword; default `:zero`

   Returns:
   Filtered image (AFArray)."
  ([^AFArray img]
   (min-filter img 3 3 :zero))
  ([^AFArray img wind-length wind-width]
   (min-filter img wind-length wind-width :zero))
  ([^AFArray img wind-length wind-width edge-pad]
   (assert-within-arrayfire! "min-filter")
   (image/minfilt img (long wind-length) (long wind-width) (defs/resolve-edge-pad edge-pad))))

(defn max-filter
  "Maximum filter.

   Replaces each pixel with the maximum value in its neighborhood.

   Parameters:
   - img         — input image (AFArray)
   - wind-length — (optional) window length; default 3
   - wind-width  — (optional) window width; default 3
   - edge-pad    — (optional) border padding keyword; default `:zero`

   Returns:
   Filtered image (AFArray)."
  ([^AFArray img]
   (max-filter img 3 3 :zero))
  ([^AFArray img wind-length wind-width]
   (max-filter img wind-length wind-width :zero))
  ([^AFArray img wind-length wind-width edge-pad]
   (assert-within-arrayfire! "max-filter")
   (image/maxfilt img (long wind-length) (long wind-width) (defs/resolve-edge-pad edge-pad))))

(defn gaussian-kernel
  "Generate a Gaussian convolution kernel.

   Parameters:
   - rows    — number of rows (int)
   - cols    — number of columns (int)
   - sigma-r — (optional) row sigma; default 0.0 (auto-computed)
   - sigma-c — (optional) column sigma; default 0.0 (auto-computed)

   Returns:
   Gaussian kernel (AFArray).

   Example:
   (gaussian-kernel 5 5)
   (gaussian-kernel 5 5 1.0 1.0)"
  ([rows cols]
   (gaussian-kernel rows cols 0.0 0.0))
  ([rows cols sigma-r sigma-c]
   (assert-within-arrayfire! "gaussian-kernel")
   (image/gaussian-kernel (int rows) (int cols) (double sigma-r) (double sigma-c))))

(defn anisotropic-diffusion
  "Anisotropic diffusion for edge-preserving denoising.

   Perona-Malik anisotropic diffusion smooths images while preserving edges.

   Parameters:
   - img          — input image (AFArray)
   - dt           — (optional) time step; default 0.125
   - k            — (optional) diffusion constant; default 0.1
   - iterations   — (optional) number of iterations; default 10
   -  flux-fn      — (optional) flux function keyword: `:exponential`, `:quadratic`;
                    default `:quadratic`
   - diffusion-eq — (optional) diffusion equation keyword: `:grad`, `:mcde`;
                    default `:grad`

   Returns:
   Diffused image (AFArray).

   Example:
   (anisotropic-diffusion noisy-img)
   (anisotropic-diffusion noisy-img 0.125 0.1 20 :exponential :grad)"
  ([^AFArray img]
   (anisotropic-diffusion img 0.125 0.1 10 :quadratic :grad))
  ([^AFArray img dt k iterations]
   (anisotropic-diffusion img dt k iterations :quadratic :grad))
  ([^AFArray img dt k iterations flux-fn diffusion-eq]
   (assert-within-arrayfire! "anisotropic-diffusion")
   (image/anisotropic-diffusion img (float dt) (float k) (int iterations)
                                (defs/resolve-flux-fn flux-fn)
                                (defs/resolve-diffusion-eq diffusion-eq))))

;;;
;;; Histogram Operations
;;;

(defn histogram
  "Compute histogram of an array.

   Parameters:
   - img    — input array (AFArray)
   - nbins  — number of bins (int)
   - minval — minimum value (double)
   - maxval — maximum value (double)

   Returns:
   Histogram (AFArray).

   Example:
   (histogram img 256 0.0 255.0)"
  ^AFArray [^AFArray img nbins minval maxval]
  (assert-within-arrayfire! "histogram")
  (image/histogram img (int nbins) (double minval) (double maxval)))

(defn histogram-equalize
  "Histogram equalization for contrast enhancement.

   Parameters:
   - img  — input image (AFArray)
   - hist — histogram for equalization (AFArray), typically from `histogram`

   Returns:
   Equalized image (AFArray).

   Example:
   (let [hist (histogram img 256 0.0 255.0)]
     (histogram-equalize img hist))"
  ^AFArray [^AFArray img ^AFArray hist]
  (assert-within-arrayfire! "histogram-equalize")
  (image/hist-equal img hist))

;;;
;;; Color Space Conversions
;;;

(defn rgb->gray
  "Convert RGB image to grayscale.

   Parameters:
   - img       — input RGB image (AFArray, shape [h w 3])
   - r-percent — (optional) red channel weight; default 0.2126
   - g-percent — (optional) green channel weight; default 0.7152
   - b-percent — (optional) blue channel weight; default 0.0722

   Returns:
   Grayscale image (AFArray, shape [h w 1])."
  ([^AFArray img]
   (rgb->gray img 0.2126 0.7152 0.0722))
  ([^AFArray img r-percent g-percent b-percent]
   (assert-within-arrayfire! "rgb->gray")
   (image/rgb->gray img (float r-percent) (float g-percent) (float b-percent))))

(defn gray->rgb
  "Convert grayscale image to RGB.

   Parameters:
   - img      — input grayscale image (AFArray)
   - r-factor — (optional) red channel multiplier; default 1.0
   - g-factor — (optional) green channel multiplier; default 1.0
   - b-factor — (optional) blue channel multiplier; default 1.0

   Returns:
   RGB image (AFArray, shape [h w 3])."
  ([^AFArray img]
   (gray->rgb img 1.0 1.0 1.0))
  ([^AFArray img r-factor g-factor b-factor]
   (assert-within-arrayfire! "gray->rgb")
   (image/gray->rgb img (float r-factor) (float g-factor) (float b-factor))))

(defn rgb->hsv
  "Convert RGB to HSV color space.

   Parameters:
   - img — input RGB image (AFArray, shape [h w 3])

   Returns:
   HSV image (AFArray, shape [h w 3])."
  ^AFArray [^AFArray img]
  (assert-within-arrayfire! "rgb->hsv")
  (image/rgb->hsv img))

(defn hsv->rgb
  "Convert HSV to RGB color space.

   Parameters:
     img — input HSV image (AFArray, shape [h w 3])

   Returns:
     RGB image (AFArray, shape [h w 3])."
  ^AFArray [^AFArray img]
  (assert-within-arrayfire! "hsv->rgb")
  (image/hsv->rgb img))

(defn rgb->ycbcr
  "Convert RGB to YCbCr color space.

   Parameters:
   - img      — input RGB image (AFArray, shape [h w 3])
   - standard — (optional) YCbCr standard keyword: `:bt601`, `:bt709`, `:bt2020`;
                default `:bt601`

   Returns:
   YCbCr image (AFArray, shape [h w 3])."
  ([^AFArray img]
   (rgb->ycbcr img :bt601))
  ([^AFArray img standard]
   (assert-within-arrayfire! "rgb->ycbcr")
   (image/rgb->ycbcr img (defs/resolve-ycc-std standard))))

(defn ycbcr->rgb
  "Convert YCbCr to RGB color space.

   Parameters:
   - img      — input YCbCr image (AFArray, shape [h w 3])
   - standard — (optional) YCbCr standard keyword: `:bt601`, `:bt709`, `:bt2020`;
                default `:bt601`

   Returns:
   RGB image (AFArray, shape [h w 3])."
  ([^AFArray img]
   (ycbcr->rgb img :bt601))
  ([^AFArray img standard]
   (assert-within-arrayfire! "ycbcr->rgb")
   (image/ycbcr->rgb img (defs/resolve-ycc-std standard))))

(defn convert-color-space
  "Convert between arbitrary color spaces.

   Parameters:
   - img  — input image (AFArray)
   - to   — target color space keyword (`:gray`, `:rgb`, `:hsv`, `:ycbcr`)
   - from — source color space keyword (`:gray`, `:rgb`, `:hsv`, `:ycbcr`)

   Returns:
   Converted image (AFArray)."
  ^AFArray [^AFArray img to from]
  (assert-within-arrayfire! "convert-color-space")
  (image/color-space img (defs/resolve-colorspace to) (defs/resolve-colorspace from)))

;;;
;;; Connected Components and Segmentation
;;;

(defn regions
  "Label connected components in a binary image.

   Each connected region in the input binary image is assigned a unique
   integer label in the output.

   Parameters:
   - img          — input binary image (AFArray)
   - connectivity — (optional) connectivity keyword: `:4` or `:8`; default `:4`

   Returns:
   Labeled image (AFArray) where each region has a unique integer label.

   Example:
   (regions binary-img)
   (regions binary-img :8) ; 8-connected"
  ([^AFArray img]
   (regions img :4))
  ([^AFArray img connectivity]
   (assert-within-arrayfire! "regions")
   (image/regions img (defs/resolve-connectivity connectivity) 5)))

(defn confidence-connected
  "Confidence-connected region growing segmentation.

   Segments an image based on seed points and statistical criteria.

   Parameters:
   - img             — input image (AFArray)
   - seed-x          — seed x coordinates (AFArray)
   - seed-y          — seed y coordinates (AFArray)
   - radius          — neighborhood radius (int)
   - multiplier      — threshold multiplier (int)
   - iterations      — number of iterations (int)
   - segmented-value — value for segmented pixels (double)

   Returns:
   Segmented image (AFArray)."
  ^AFArray [^AFArray img ^AFArray seed-x ^AFArray seed-y
            radius multiplier iterations segmented-value]
  (assert-within-arrayfire! "confidence-connected")
  (image/confidence-connected img seed-x seed-y
                              (int radius) (int multiplier) (int iterations)
                              (double segmented-value)))

;;;
;;; Patch Operations
;;;

(defn unwrap
  "Extract image patches into columns (im2col).

   Extracts patches of size `[wx wy]` with given stride and padding,
   stacking them as columns in the output array.

   Parameters:
   - img       — input image (AFArray)
   - wx        — patch width (long)
   - wy        — patch height (long)
   - sx        — stride in x (long)
   - sy        — stride in y (long)
   - px        — (optional) padding in x; default 0
   - py        — (optional) padding in y; default 0
   - column?   — (optional) column-major layout; default true

   Returns:
   Unwrapped patches (AFArray)."
  ([^AFArray img wx wy sx sy]
   (unwrap img wx wy sx sy 0 0 true))
  ([^AFArray img wx wy sx sy px py column?]
   (assert-within-arrayfire! "unwrap")
   (image/unwrap img (long wx) (long wy) (long sx) (long sy)
                 (long px) (long py) column?)))

(defn wrap
  "Reconstruct image from patches (col2im, inverse of `unwrap`).

   Parameters:
   - patches — unwrapped patches (AFArray)
   - ox      — output width (long)
   - oy      — output height (long)
   - wx      — patch width (long)
   - wy      — patch height (long)
   - sx      — stride in x (long)
   - sy      — stride in y (long)
   - px      — (optional) padding in x; default 0
   - py      — (optional) padding in y; default 0
   - column? — (optional) column-major layout; default true

   Returns:
   Reconstructed image (AFArray)."
  ([^AFArray patches ox oy wx wy sx sy]
   (wrap patches ox oy wx wy sx sy 0 0 true))
  ([^AFArray patches ox oy wx wy sx sy px py column?]
   (assert-within-arrayfire! "wrap")
   (image/wrap patches (long ox) (long oy) (long wx) (long wy)
              (long sx) (long sy) (long px) (long py) column?)))

;;;
;;; Integral Image
;;;

(defn summed-area-table
  "Compute the summed area table (integral image).

   The SAT enables O(1) computation of the sum of any rectangular
   region in the original image.

   Parameters:
   - img — input image (AFArray)

   Returns:
   Summed area table (AFArray).

   Example:
   (summed-area-table img)"
  ^AFArray [^AFArray img]
  (assert-within-arrayfire! "summed-area-table")
  (image/sat img))

;;;
;;; Deconvolution
;;;

(defn iterative-deconv
  "Iterative deconvolution for image restoration.

   Parameters:
   - img          — input blurred image (AFArray)
   - kernel       — point spread function kernel (AFArray)
   - iterations   — number of iterations (int)
   - relax-factor — (optional) relaxation factor; default 1.0
   - algo         — (optional) algorithm keyword: `:landweber`, `:richardson-lucy`;
                    default `:landweber`

   Returns:
   Deconvolved image (AFArray)."
  ([^AFArray img ^AFArray kernel iterations]
   (iterative-deconv img kernel iterations 1.0 :landweber))
  ([^AFArray img ^AFArray kernel iterations relax-factor algo]
   (assert-within-arrayfire! "iterative-deconv")
   (image/iterative-deconv img kernel (int iterations)
                           (float relax-factor)
                           (defs/resolve-iterative-deconv-algo algo))))

(defn inverse-deconv
  "Inverse filter deconvolution for image restoration.

   Parameters:
   - img   — input blurred image (AFArray)
   - psf   — point spread function (AFArray)
   - gamma — (optional) regularization parameter; default 1.0
   - algo  — (optional) algorithm keyword: `:tikhonov`; default `:tikhonov`

   Returns:
   Deconvolved image (AFArray)."
  ([^AFArray img ^AFArray psf]
   (inverse-deconv img psf 1.0 :tikhonov))
  ([^AFArray img ^AFArray psf gamma algo]
   (assert-within-arrayfire! "inverse-deconv")
   (image/inverse-deconv img psf (float gamma) (defs/resolve-inverse-deconv-algo algo))))

;;;
;;; Image Moments
;;;

(defn moments
  "Compute image moments for one or more images.

   Moments are quantitative measures of shape and intensity distribution.

   Parameters:
   - img         — input image(s) (AFArray)
   - moment-type — moment type keyword: `:m00`, `:m01`, `:m10`, `:m11`,
                   `:first-order`, or a set like `#{:m00 :m01}`

   Returns:
   AFArray of moment values. For `:first-order`, shape is [4 × 1 × ...].

   Example:
   (moments img :first-order)
   (moments img :m00)"
  ^AFArray [^AFArray img moment-type]
  (assert-within-arrayfire! "moments")
  (moments/moments img moment-type))

(defn moments-all
  "Compute image moments and return as a map (single image convenience).

   Parameters:
   - img         — input 2D image (AFArray)
   - moment-type — (optional) moment type; default `:first-order`

   Returns:
   Map with keys `:m00`, `:m01`, `:m10`, `:m11` (as applicable).

   Example:
   (let [{:keys [m00 m01 m10]} (moments-all img)]
     {:area m00 :centroid-x (/ m01 m00) :centroid-y (/ m10 m00)})"
  ([^AFArray img]
   (moments-all img :first-order))
  ([^AFArray img moment-type]
   (assert-within-arrayfire! "moments-all")
   (moments/moments-all img moment-type)))

(defn centroid
  "Compute the centroid (center of mass) of an image.

   Parameters:
   - img — input image (AFArray)

   Returns:
   Map `{:x cx :y cy :area M00}`, or nil if image is empty (M00 = 0).

   Example:
   (when-let [{:keys [x y]} (centroid object-mask)]
     (println \"Object at\" x y))"
  [^AFArray img]
  (assert-within-arrayfire! "centroid")
  (moments/centroid img))

(defn image-area
  "Compute the total area (pixel count or intensity sum) of an image.

   For binary images, this is the number of foreground pixels.
   For grayscale images, this is the sum of all pixel intensities (M00).

   Parameters:
   - img — input image (AFArray)

   Returns:
   Double value representing M00."
  [^AFArray img]
  (assert-within-arrayfire! "image-area")
  (moments/area img))


(comment
  ;; Image processing REPL experiments
  ;; All examples must be called inside (with-arrayfire ...).
  ;;
  ;; Load namespace:
  ;;   (require '[org.soulspace.arrayfire.api.core :as af])
  ;;   (require '[org.soulspace.arrayfire.api.image-processing :as ip])

  ;; --- Gradient computation ---
  (core/with-arrayfire
    (let [img (core/array [1.0 2.0 3.0 4.0 5.0 6.0 7.0 8.0 9.0] [3 3] :f64)
          [dx dy] (gradient img)]
      {:dx-shape (core/shape dx) :dy-shape (core/shape dy)}))

  ;; --- Sobel edge detection ---
  (core/with-arrayfire
    (let [img (core/array [1.0 2.0 3.0 4.0 5.0 6.0 7.0 8.0 9.0] [3 3] :f64)
          [dx dy] (sobel img)]
      {:dx-shape (core/shape dx) :dy-shape (core/shape dy)}))

  ;; --- Resize ---
  (core/with-arrayfire
    (let [img (core/array [1.0 2.0 3.0 4.0] [2 2] :f32)
          big (resize img 4 4 :nearest)]
      (core/shape big)))

  ;; --- Rotate ---
  (core/with-arrayfire
    (let [img (core/array (vec (map double (range 16))) [4 4] :f64)
          rotated (rotate img (/ Math/PI 4))]
      (core/shape rotated)))

  ;; --- Morphological operations ---
  (core/with-arrayfire
    (let [img (core/array [0.0 0.0 0.0 0.0 1.0 0.0 0.0 0.0 0.0] [3 3] :f32)
          se  (core/constant 1.0 [3 3] :f32)
          dilated (dilate img se)]
      (core/->value dilated)))

  ;; --- Gaussian kernel ---
  (core/with-arrayfire
    (let [k (gaussian-kernel 5 5 1.0 1.0)]
      {:shape (core/shape k) :values (core/->value k)}))

  ;; --- Histogram ---
  (core/with-arrayfire
    (let [data (core/array [1.0 2.0 3.0 4.0 5.0 1.0 2.0 3.0 4.0 5.0] [10] :f32)
          hist (histogram data 5 1.0 5.0)]
      (core/->value hist)))

  ;; --- Color space conversion: gray->rgb->gray roundtrip ---
  (core/with-arrayfire
    (let [gray (core/array [0.5 0.5 0.5 0.5] [2 2] :f32)
          rgb  (gray->rgb gray)
          back (rgb->gray rgb)]
      {:rgb-shape (core/shape rgb) :gray-shape (core/shape back)}))

  ;; --- Summed area table ---
  (core/with-arrayfire
    (let [img (core/array [1.0 2.0 3.0 4.0 5.0 6.0 7.0 8.0 9.0] [3 3] :f64)
          sat (summed-area-table img)]
      (core/->value sat)))

  ;; --- Unwrap/Wrap roundtrip ---
  (core/with-arrayfire
    (let [img (core/array (vec (map double (range 16))) [4 4] :f32)
          patches (unwrap img 2 2 2 2)
          back    (wrap patches 4 4 2 2 2 2)]
      {:patches-shape (core/shape patches) :back-shape (core/shape back)}))

  ;; --- Moments ---
  (core/with-arrayfire
    (let [img (core/array [1.0 0.0 0.0 1.0] [2 2] :f32)]
      (moments-all img)))

  ;; --- Centroid ---
  (core/with-arrayfire
    (let [img (core/array [0.0 0.0 0.0 0.0 1.0 0.0 0.0 0.0 0.0] [3 3] :f32)]
      (centroid img)))

  ;
  )

