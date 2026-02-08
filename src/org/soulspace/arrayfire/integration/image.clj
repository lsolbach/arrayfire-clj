(ns org.soulspace.arrayfire.integration.image
  "Integration of the ArrayFire image processing FFI bindings with error
   handling and resource management on the JVM.
   
   This namespace provides high-level functions for image processing operations:
   
   - Geometric transformations: resize, rotate, translate, scale, skew, transform
   - Morphological operations: dilate, erode (2D and 3D)
   - Filtering: bilateral, meanshift, minfilt, maxfilt, gaussian-kernel
   - Edge detection: gradient, sobel, canny
   - Color space conversions: rgb2gray, gray2rgb, rgb2hsv, hsv2rgb, rgb2ycbcr, ycbcr2rgb
   - Histogram operations: histogram, hist-equal
   - Image I/O: load-image, save-image, load-image-memory, save-image-memory
   - Advanced: regions, unwrap, wrap, sat, anisotropic-diffusion, deconvolution
   
   All functions work with AFArray instances and follow ArrayFire's memory management."
  (:require [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.gradient :as gradient]
            [org.soulspace.arrayfire.ffi.imageio :as imageio]
            [org.soulspace.arrayfire.ffi.imageio2 :as imageio2]
            [org.soulspace.arrayfire.ffi.resize :as resize]
            [org.soulspace.arrayfire.ffi.rotate :as rotate]
            [org.soulspace.arrayfire.ffi.transform :as transform]
            [org.soulspace.arrayfire.ffi.transform-coordinates :as transform-coords]
            [org.soulspace.arrayfire.ffi.histogram :as histogram]
            [org.soulspace.arrayfire.ffi.morph :as morph]
            [org.soulspace.arrayfire.ffi.bilateral :as bilateral]
            [org.soulspace.arrayfire.ffi.meanshift :as meanshift]
            [org.soulspace.arrayfire.ffi.filters :as filters]
            [org.soulspace.arrayfire.ffi.regions :as regions]
            [org.soulspace.arrayfire.ffi.sobel :as sobel]
            [org.soulspace.arrayfire.ffi.rgb-gray :as rgb-gray]
            [org.soulspace.arrayfire.ffi.histeq :as histeq]
            [org.soulspace.arrayfire.ffi.gaussian-kernel :as gaussian-kernel]
            [org.soulspace.arrayfire.ffi.hsv-rgb :as hsv-rgb]
            [org.soulspace.arrayfire.ffi.colorspace :as colorspace]
            [org.soulspace.arrayfire.ffi.unwrap :as unwrap]
            [org.soulspace.arrayfire.ffi.wrap :as wrap]
            [org.soulspace.arrayfire.ffi.sat :as sat]
            [org.soulspace.arrayfire.ffi.ycbcr-rgb :as ycbcr-rgb]
            [org.soulspace.arrayfire.ffi.canny :as canny]
            [org.soulspace.arrayfire.ffi.anisotropic-diffusion :as aniso]
            [org.soulspace.arrayfire.ffi.deconvolution :as deconv]
            [org.soulspace.arrayfire.ffi.confidence-connected :as confidence]
            [org.soulspace.arrayfire.integration.jvm-integration :as jvm])
  (:import (org.soulspace.arrayfire.integration.jvm_integration AFArray)))

;;;
;;; Gradient and Edge Detection
;;;

(defn gradient
  "Compute image gradients using finite differences.
   
   Calculates the horizontal (dx) and vertical (dy) gradients of an image
   using finite difference approximations.
   
   Parameters:
   - in: Input image (AFArray)
   
   Returns:
   Vector of [dx dy] where:
   - dx: Horizontal gradient (AFArray)
   - dy: Vertical gradient (AFArray)
   
   Example:
   ```clojure
   (let [[dx dy] (gradient img)]
     (println \"Gradient computed\"))
   ```"
  [^AFArray in]
  (let [dx (jvm/native-af-array-pointer)
        dy (jvm/native-af-array-pointer)]
    (jvm/check! (gradient/af-gradient dx dy (jvm/af-handle in)) "af-gradient")
    [(jvm/af-array-new (jvm/deref-af-array dx))
     (jvm/af-array-new (jvm/deref-af-array dy))]))

(defn sobel
  "Compute Sobel edge detection gradients.
   
   Applies Sobel operator to compute image gradients for edge detection.
   
   Parameters:
   - img: Input image (AFArray)
   - ker-size: Kernel size (default 3), must be odd
   
   Returns:
   Vector of [dx dy] where:
   - dx: Horizontal Sobel gradient (AFArray)
   - dy: Vertical Sobel gradient (AFArray)
   
   Example:
   ```clojure
   (let [[dx dy] (sobel grayscale-img 3)]
     (magnitude (add (mul dx dx) (mul dy dy))))
   ```"
  ([^AFArray img]
   (sobel img 3))
  ([^AFArray img ker-size]
   (let [dx (jvm/native-af-array-pointer)
         dy (jvm/native-af-array-pointer)]
     (jvm/check! (sobel/af-sobel-operator dx dy (jvm/af-handle img) (int ker-size))
                 "af-sobel-operator")
     [(jvm/af-array-new (jvm/deref-af-array dx))
      (jvm/af-array-new (jvm/deref-af-array dy))])))

(defn canny
  "Canny edge detection.
   
   Performs edge detection using the Canny algorithm with multiple stages:
   noise reduction, gradient calculation, non-maximum suppression, and
   hysteresis thresholding.
   
   Parameters:
   - in: Input grayscale image (AFArray)
   - threshold-type: Threshold method (0=manual, 1=auto-otsu)
   - low-threshold: Low threshold value or ratio (default 0.1)
   - high-threshold: High threshold value or ratio (default 0.3)
   - sobel-window: Sobel kernel size (default 3)
   - is-fast: Use fast mode (default true)
   
   Returns:
   Binary edge map (AFArray) where edges are 1, non-edges are 0
   
   Example:
   ```clojure
   (let [edges (canny gray-img 0 0.1 0.3 3 true)]
     (save-image \"edges.png\" edges))
   ```"
  ([^AFArray in]
   (canny in 0 0.1 0.3 3 true))
  ([^AFArray in threshold-type]
   (canny in threshold-type 0.1 0.3 3 true))
  ([^AFArray in threshold-type low-threshold high-threshold]
   (canny in threshold-type low-threshold high-threshold 3 true))
  ([^AFArray in threshold-type low-threshold high-threshold sobel-window is-fast]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (canny/af-canny out (jvm/af-handle in) (int threshold-type)
                                 (float low-threshold) (float high-threshold)
                                 (int sobel-window) (if is-fast 1 0))
                 "af-canny")
     (jvm/af-array-new (jvm/deref-af-array out)))))

;;;
;;; Image I/O
;;;

(defn load-image
  "Load an image from file.
   
   Loads an image from disk in various formats (PNG, JPEG, BMP, etc.).
   
   Parameters:
   - filename: Path to image file (string)
   - is-color: Load as color (true) or grayscale (false), default true
   
   Returns:
   AFArray containing the loaded image
   
   Example:
   ```clojure
   (let [img (load-image \"photo.jpg\" true)]
     (println \"Image loaded\"))
   ```"
  ([filename]
   (load-image filename true))
  ([filename is-color]
   (let [out (jvm/native-af-array-pointer)
         c-filename (jvm/string->c-string filename)]
     (jvm/check! (imageio/af-load-image out c-filename (if is-color 1 0))
                 "af-load-image")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn save-image
  "Save an image to file.
   
   Saves an image to disk. Format is determined by file extension.
   
   Parameters:
   - filename: Destination path (string)
   - in: Image to save (AFArray)
   
   Returns:
   nil
   
   Example:
   ```clojure
   (save-image \"output.png\" processed-img)
   ```"
  [filename ^AFArray in]
  (let [c-filename (jvm/string->c-string filename)]
    (jvm/check! (imageio/af-save-image c-filename (jvm/af-handle in))
                "af-save-image")
    nil))

(defn load-image-native
  "Load an image in its native format without conversion.
   
   Parameters:
   - filename: Path to image file (string)
   
   Returns:
   AFArray containing the image in native format"
  [filename]
  (let [out (jvm/native-af-array-pointer)
        c-filename (jvm/string->c-string filename)]
    (jvm/check! (imageio2/af-load-image-native out c-filename)
                "af-load-image-native")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn save-image-native
  "Save an image in its native format without conversion.
   
   Parameters:
   - filename: Destination path (string)
   - in: Image to save (AFArray)
   
   
   Returns:
   nil"
  [filename ^AFArray in]
  (let [c-filename (jvm/string->c-string filename)]
    (jvm/check! (imageio2/af-save-image-native c-filename (jvm/af-handle in))
                "af-save-image-native")
    nil))

(defn load-image-memory
  "Load an image from a memory buffer.
   
   Decodes an image from a FreeImage FIMEMORY buffer previously created
   or loaded into memory. Useful for loading images from network, database,
   or embedded resources without file I/O.
   
   Parameters:
   - ptr: Pointer to FreeImage FIMEMORY structure (from save-image-memory
          or externally created)
   
   Returns:
   AFArray containing the decoded image
   
   Example:
   ```clojure
   ;; Load from HTTP response
   (let [img-bytes (http-get-bytes \"https://example.com/image.png\")
         fimem-ptr (bytes->fimem img-bytes)
         img (load-image-memory fimem-ptr)]
     (process-image img))
   
   ;; Load from embedded resource
   (let [resource-ptr (get-embedded-image-ptr)
         img (load-image-memory resource-ptr)]
     img)
   ```
   
   Use cases:
   - Network: Load images from HTTP responses
   - Database: Decode BLOBs
   - Embedded: Load from compiled-in resources
   - IPC: Receive images from other processes
   - Cache: Load from in-memory cache"
  [ptr]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (imageio/af-load-image-memory out ptr)
                "af-load-image-memory")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn save-image-memory
  "Save an image to a memory buffer.
   
   Encodes an image and stores it in a FreeImage FIMEMORY structure.
   The buffer can be used for network transmission, storage, or further
   processing without file I/O.
   
   Parameters:
   - in: Image to save (AFArray)
   - format: Image format code (integer)
     * 0 - BMP (fast, uncompressed)
     * 2 - JPEG (lossy, small)
     * 13 - PNG (lossless, good compression)
     * 18 - TIFF
     * 29 - EXR (HDR)
     See af/defines.h for complete list
   
   Returns:
   Pointer to FreeImage FIMEMORY buffer (must be freed with delete-image-memory!)
   
   Example:
   ```clojure
   ;; Save as PNG for network upload
   (let [img (process-image original)
         mem-ptr (save-image-memory img 13)]  ; PNG format
     (try
       (let [bytes (fimem->bytes mem-ptr)]
         (http-post \"https://api.example.com/upload\" bytes))
       (finally
         (delete-image-memory! mem-ptr))))  ; Critical: free memory!
   
   ;; Save as JPEG for small size
   (let [photo (capture-photo)
         jpeg-ptr (save-image-memory photo 2)]  ; JPEG format
     (try
       (db-store! {:image (fimem->blob jpeg-ptr)})
       (finally
         (delete-image-memory! jpeg-ptr))))
   ```
   
   Memory management:
   CRITICAL: Always call delete-image-memory! on the returned pointer
   to prevent memory leaks!
   
   Use cases:
   - Upload to servers
   - Store in databases
   - Send over network
   - In-memory caching
   - IPC (inter-process communication)"
  [^AFArray in format]
  (let [ptr-ptr (mem/alloc 8)]  ; Allocate buffer for pointer
    (jvm/check! (imageio/af-save-image-memory ptr-ptr (jvm/af-handle in) (int format))
                "af-save-image-memory")
    (mem/read-long ptr-ptr 0)))

(defn delete-image-memory!
  "Free a memory buffer created by save-image-memory.
   
   Releases the FreeImage FIMEMORY structure allocated by save-image-memory.
   This MUST be called for every buffer created to prevent memory leaks.
   
   Parameters:
   - ptr: FIMEMORY pointer returned by save-image-memory
   
   Returns:
   nil
   
   Example:
   ```clojure
   (let [img (load-image \"photo.jpg\")
         mem-ptr (save-image-memory img 13)]  ; PNG
     (try
       ;; Use the memory buffer
       (upload-bytes (extract-bytes mem-ptr))
       (finally
         ;; ALWAYS cleanup!
         (delete-image-memory! mem-ptr))))
   ```
   
   CRITICAL:
   - Call exactly once per buffer
   - Do not use pointer after deletion
   - Always pair with save-image-memory
   - Use try-finally to ensure cleanup"
  [ptr]
  (jvm/check! (imageio/af-delete-image-memory ptr)
              "af-delete-image-memory")
  nil)

;;;
;;; Geometric Transformations
;;;

(defn resize
  "Resize an image to new dimensions.
   
   Parameters:
   - in: Input image (AFArray)
   - odim0: Output height (long)
   - odim1: Output width (long)
   - method: Interpolation method (0=nearest, 1=linear, 2=bilinear, 3=cubic, 4=lower), default 1
   
   Returns:
   Resized image (AFArray)
   
   Example:
   ```clojure
   (let [resized (resize img 512 512 1)]
     resized)
   ```"
  ([^AFArray in odim0 odim1]
   (resize in odim0 odim1 1))
  ([^AFArray in odim0 odim1 method]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (resize/af-resize out (jvm/af-handle in) (long odim0) (long odim1) (int method))
                 "af-resize")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn rotate
  "Rotate an image by an angle.
   
   Parameters:
   - in: Input image (AFArray)
   - theta: Rotation angle in radians (float)
   - crop: Crop to original dimensions (boolean), default true
   - method: Interpolation method (default 1)
   
   Returns:
   Rotated image (AFArray)
   
   Example:
   ```clojure
   (let [rotated (rotate img (/ Math/PI 4) true 1)]
     rotated)
   ```"
  ([^AFArray in theta]
   (rotate in theta true 1))
  ([^AFArray in theta crop]
   (rotate in theta crop 1))
  ([^AFArray in theta crop method]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (rotate/af-rotate out (jvm/af-handle in) (float theta)
                                   (if crop 1 0) (int method))
                 "af-rotate")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn translate
  "Translate (shift) an image.
   
   Parameters:
   - in: Input image (AFArray)
   - trans0: Translation in first dimension (float)
   - trans1: Translation in second dimension (float)
   - odim0: Output height (long), default 0 (same as input)
   - odim1: Output width (long), default 0 (same as input)
   - method: Interpolation method (default 1)
   
   Returns:
   Translated image (AFArray)"
  ([^AFArray in trans0 trans1]
   (translate in trans0 trans1 0 0 1))
  ([^AFArray in trans0 trans1 odim0 odim1]
   (translate in trans0 trans1 odim0 odim1 1))
  ([^AFArray in trans0 trans1 odim0 odim1 method]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (transform/af-translate out (jvm/af-handle in) (float trans0) (float trans1)
                                         (long odim0) (long odim1) (int method))
                 "af-translate")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn scale
  "Scale an image by factors.
   
   Parameters:
   - in: Input image (AFArray)
   - scale0: Scale factor for dimension 0 (float)
   - scale1: Scale factor for dimension 1 (float)
   - odim0: Output height (long), default 0 (computed from scale)
   - odim1: Output width (long), default 0 (computed from scale)
   - method: Interpolation method (default 1)
   
   Returns:
   Scaled image (AFArray)"
  ([^AFArray in scale0 scale1]
   (scale in scale0 scale1 0 0 1))
  ([^AFArray in scale0 scale1 odim0 odim1]
   (scale in scale0 scale1 odim0 odim1 1))
  ([^AFArray in scale0 scale1 odim0 odim1 method]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (transform/af-scale out (jvm/af-handle in) (float scale0) (float scale1)
                                     (long odim0) (long odim1) (int method))
                 "af-scale")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn skew
  "Skew an image.
   
   Parameters:
   - in: Input image (AFArray)
   - skew0: Skew factor for dimension 0 (float)
   - skew1: Skew factor for dimension 1 (float)
   - odim0: Output height (long), default 0
   - odim1: Output width (long), default 0
   - method: Interpolation method (default 1)
   - inverse: Apply inverse transform (boolean), default false
   
   Returns:
   Skewed image (AFArray)"
  ([^AFArray in skew0 skew1]
   (skew in skew0 skew1 0 0 1 false))
  ([^AFArray in skew0 skew1 odim0 odim1]
   (skew in skew0 skew1 odim0 odim1 1 false))
  ([^AFArray in skew0 skew1 odim0 odim1 method inverse]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (transform/af-skew out (jvm/af-handle in) (float skew0) (float skew1)
                                    (long odim0) (long odim1) (int method) (if inverse 1 0))
                 "af-skew")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn transform
  "Apply affine transformation to an image.
   
   Parameters:
   - in: Input image (AFArray)
   - transform-matrix: 3x2 transformation matrix (AFArray)
   - odim0: Output height (long)
   - odim1: Output width (long)
   - method: Interpolation method (default 1)
   - inverse: Apply inverse transform (boolean), default true
   
   Returns:
   Transformed image (AFArray)"
  ([^AFArray in ^AFArray transform-matrix odim0 odim1]
   (transform in transform-matrix odim0 odim1 1 true))
  ([^AFArray in ^AFArray transform-matrix odim0 odim1 method inverse]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (transform/af-transform out (jvm/af-handle in) (jvm/af-handle transform-matrix)
                                         (long odim0) (long odim1) (int method) (if inverse 1 0))
                 "af-transform")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn transform-coordinates
  "Transform coordinates using a transformation matrix.
   
   Parameters:
   - tf: Transformation matrix (AFArray)
   - d0: Coordinate in dimension 0 (float)
   - d1: Coordinate in dimension 1 (float)
   
   Returns:
   Transformed coordinates (AFArray)"
  [^AFArray tf d0 d1]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (transform-coords/af-transform-coordinates out (jvm/af-handle tf) (float d0) (float d1))
                "af-transform-coordinates")
    (jvm/af-array-new (jvm/deref-af-array out))))

;;;
;;; Morphological Operations
;;;

(defn dilate
  "Morphological dilation (2D).
   
   Expands bright regions in an image using a structuring element.
   
   Parameters:
   - in: Input image (AFArray)
   - mask: Structuring element (AFArray)
   
   Returns:
   Dilated image (AFArray)
   
   Example:
   ```clojure
   (let [se (constant 1.0 [3 3])
         dilated (dilate binary-img se)]
     dilated)
   ```"
  [^AFArray in ^AFArray mask]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (morph/af-dilate out (jvm/af-handle in) (jvm/af-handle mask))
                "af-dilate")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn dilate3
  "Morphological dilation (3D).
   
   3D version of dilation for volumetric data.
   
   Parameters:
   - in: Input volume (AFArray)
   - mask: 3D structuring element (AFArray)
   
   Returns:
   Dilated volume (AFArray)"
  [^AFArray in ^AFArray mask]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (morph/af-dilate3 out (jvm/af-handle in) (jvm/af-handle mask))
                "af-dilate3")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn erode
  "Morphological erosion (2D).
   
   Shrinks bright regions in an image using a structuring element.
   
   Parameters:
   - in: Input image (AFArray)
   - mask: Structuring element (AFArray)
   
   Returns:
   Eroded image (AFArray)"
  [^AFArray in ^AFArray mask]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (morph/af-erode out (jvm/af-handle in) (jvm/af-handle mask))
                "af-erode")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn erode3
  "Morphological erosion (3D).
   
   3D version of erosion for volumetric data.
   
   Parameters:
   - in: Input volume (AFArray)
   - mask: 3D structuring element (AFArray)
   
   Returns:
   Eroded volume (AFArray)"
  [^AFArray in ^AFArray mask]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (morph/af-erode3 out (jvm/af-handle in) (jvm/af-handle mask))
                "af-erode3")
    (jvm/af-array-new (jvm/deref-af-array out))))

;;;
;;; Filtering
;;;

(defn bilateral
  "Bilateral filter for edge-preserving smoothing.
   
   Smooths images while preserving edges by considering both spatial
   and intensity differences.
   
   Parameters:
   - in: Input image (AFArray)
   - spatial-sigma: Spatial Gaussian standard deviation (float)
   - chromatic-sigma: Intensity/color Gaussian standard deviation (float)
   - is-color: Image is color (boolean), default false
   
   Returns:
   Filtered image (AFArray)
   
   Example:
   ```clojure
   (let [smooth (bilateral noisy-img 5.0 25.0 true)]
     smooth)
   ```"
  ([^AFArray in spatial-sigma chromatic-sigma]
   (bilateral in spatial-sigma chromatic-sigma false))
  ([^AFArray in spatial-sigma chromatic-sigma is-color]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (bilateral/af-bilateral out (jvm/af-handle in)
                                         (float spatial-sigma) (float chromatic-sigma)
                                         (if is-color 1 0))
                 "af-bilateral")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn mean-shift
  "Mean shift filter for edge-preserving smoothing and segmentation.
   
   Parameters:
   - in: Input image (AFArray)
   - spatial-sigma: Spatial bandwidth (float)
   - chromatic-sigma: Color bandwidth (float)
   - iter: Number of iterations (int), default 10
   - is-color: Image is color (boolean), default false
   
   Returns:
   Filtered image (AFArray)"
  ([^AFArray in spatial-sigma chromatic-sigma]
   (mean-shift in spatial-sigma chromatic-sigma 10 false))
  ([^AFArray in spatial-sigma chromatic-sigma iter is-color]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (meanshift/af-mean-shift out (jvm/af-handle in)
                                          (float spatial-sigma) (float chromatic-sigma)
                                          (int iter) (if is-color 1 0))
                 "af-mean-shift")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn minfilt
  "Minimum filter.
   
   Replaces each pixel with the minimum value in its neighborhood.
   
   Parameters:
   - in: Input image (AFArray)
   - wind-length: Window length (long), default 3
   - wind-width: Window width (long), default 3
   - edge-pad: Border padding type (int), default 0
   
   Returns:
   Filtered image (AFArray)"
  ([^AFArray in]
   (minfilt in 3 3 0))
  ([^AFArray in wind-length wind-width]
   (minfilt in wind-length wind-width 0))
  ([^AFArray in wind-length wind-width edge-pad]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (filters/af-minfilt out (jvm/af-handle in)
                                     (long wind-length) (long wind-width) (int edge-pad))
                 "af-minfilt")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn maxfilt
  "Maximum filter.
   
   Replaces each pixel with the maximum value in its neighborhood.
   
   Parameters:
   - in: Input image (AFArray)
   - wind-length: Window length (long), default 3
   - wind-width: Window width (long), default 3
   - edge-pad: Border padding type (int), default 0
   
   Returns:
   Filtered image (AFArray)"
  ([^AFArray in]
   (maxfilt in 3 3 0))
  ([^AFArray in wind-length wind-width]
   (maxfilt in wind-length wind-width 0))
  ([^AFArray in wind-length wind-width edge-pad]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (filters/af-maxfilt out (jvm/af-handle in)
                                     (long wind-length) (long wind-width) (int edge-pad))
                 "af-maxfilt")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn gaussian-kernel
  "Generate a Gaussian kernel.
   
   Creates a Gaussian kernel for convolution operations.
   
   Parameters:
   - rows: Number of rows (int)
   - cols: Number of columns (int)
   - sigma-r: Row sigma (double), default 0.0 (auto)
   - sigma-c: Column sigma (double), default 0.0 (auto)
   
   Returns:
   Gaussian kernel (AFArray)
   
   Example:
   ```clojure
   (let [kernel (gaussian-kernel 5 5 1.0 1.0)]
     kernel)
   ```"
  ([rows cols]
   (gaussian-kernel rows cols 0.0 0.0))
  ([rows cols sigma-r sigma-c]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (gaussian-kernel/af-gaussian-kernel out (int rows) (int cols)
                                                     (double sigma-r) (double sigma-c))
                 "af-gaussian-kernel")
     (jvm/af-array-new (jvm/deref-af-array out)))))

;;;
;;; Histogram Operations
;;;

(defn histogram
  "Compute histogram of an array.
   
   Parameters:
   - in: Input array (AFArray)
   - nbins: Number of bins (int)
   - minval: Minimum value (double)
   - maxval: Maximum value (double)
   
   Returns:
   Histogram (AFArray)
   
   Example:
   ```clojure
   (let [hist (histogram img 256 0.0 255.0)]
     hist)
   ```"
  [^AFArray in nbins minval maxval]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (histogram/af-histogram out (jvm/af-handle in) (int nbins)
                                        (double minval) (double maxval))
                "af-histogram")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn hist-equal
  "Histogram equalization.
   
   Enhances image contrast using histogram equalization.
   
   Parameters:
   - in: Input image (AFArray)
   - hist: Histogram for equalization (AFArray)
   
   Returns:
   Equalized image (AFArray)
   
   Example:
   ```clojure
   (let [hist (histogram img 256 0.0 255.0)
         eq (hist-equal img hist)]
     eq)
   ```"
  [^AFArray in ^AFArray hist]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (histeq/af-hist-equal out (jvm/af-handle in) (jvm/af-handle hist))
                "af-hist-equal")
    (jvm/af-array-new (jvm/deref-af-array out))))

;;;
;;; Color Space Conversions
;;;

(defn rgb->gray
  "Convert RGB image to grayscale.
   
   Parameters:
   - in: Input RGB image (AFArray)
   - r-percent: Red channel weight (float), default 0.2126
   - g-percent: Green channel weight (float), default 0.7152
   - b-percent: Blue channel weight (float), default 0.0722
   
   Returns:
   Grayscale image (AFArray)
   
   Example:
   ```clojure
   (let [gray (rgb->gray rgb-img)]
     gray)
   ```"
  ([^AFArray in]
   (rgb->gray in 0.2126 0.7152 0.0722))
  ([^AFArray in r-percent g-percent b-percent]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (rgb-gray/af-rgb2gray out (jvm/af-handle in)
                                       (float r-percent) (float g-percent) (float b-percent))
                 "af-rgb2gray")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn gray->rgb
  "Convert grayscale image to RGB.
   
   Parameters:
   - in: Input grayscale image (AFArray)
   - r-factor: Red channel multiplier (float), default 1.0
   - g-factor: Green channel multiplier (float), default 1.0
   - b-factor: Blue channel multiplier (float), default 1.0
   
   Returns:
   RGB image (AFArray)"
  ([^AFArray in]
   (gray->rgb in 1.0 1.0 1.0))
  ([^AFArray in r-factor g-factor b-factor]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (rgb-gray/af-gray2rgb out (jvm/af-handle in)
                                       (float r-factor) (float g-factor) (float b-factor))
                 "af-gray2rgb")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn rgb->hsv
  "Convert RGB to HSV color space.
   
   Parameters:
   - in: Input RGB image (AFArray)
   
   Returns:
   HSV image (AFArray)"
  [^AFArray in]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (hsv-rgb/af-rgb2hsv out (jvm/af-handle in))
                "af-rgb2hsv")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn hsv->rgb
  "Convert HSV to RGB color space.
   
   Parameters:
   - in: Input HSV image (AFArray)
   
   Returns:
   RGB image (AFArray)"
  [^AFArray in]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (hsv-rgb/af-hsv2rgb out (jvm/af-handle in))
                "af-hsv2rgb")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn rgb->ycbcr
  "Convert RGB to YCbCr color space.
   
   Parameters:
   - in: Input RGB image (AFArray)
   - standard: YCbCr standard (int), default 0 (601)
   
   Returns:
   YCbCr image (AFArray)"
  ([^AFArray in]
   (rgb->ycbcr in 0))
  ([^AFArray in standard]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (ycbcr-rgb/af-rgb2ycbcr out (jvm/af-handle in) (int standard))
                 "af-rgb2ycbcr")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn ycbcr->rgb
  "Convert YCbCr to RGB color space.
   
   Parameters:
   - in: Input YCbCr image (AFArray)
   - standard: YCbCr standard (int), default 0 (601)
   
   Returns:
   RGB image (AFArray)"
  ([^AFArray in]
   (ycbcr->rgb in 0))
  ([^AFArray in standard]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (ycbcr-rgb/af-ycbcr2rgb out (jvm/af-handle in) (int standard))
                 "af-ycbcr2rgb")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn color-space
  "Convert between arbitrary color spaces.
   
   Parameters:
   - image: Input image (AFArray)
   - to: Target color space (int)
   - from: Source color space (int)
   
   Returns:
   Converted image (AFArray)"
  [^AFArray image to from]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (colorspace/af-color-space out (jvm/af-handle image) (int to) (int from))
                "af-color-space")
    (jvm/af-array-new (jvm/deref-af-array out))))

;;;
;;; Region and Segmentation
;;;

(defn regions
  "Label connected components in binary image.
   
   Parameters:
   - in: Input binary image (AFArray)
   - connectivity: Connectivity type (4 or 8), default 4
   - dtype: Output data type (int), default 5 (S32)
   
   Returns:
   Labeled image (AFArray) where each region has unique integer label
   
   Example:
   ```clojure
   (let [labels (regions binary-img 4)]
     labels)
   ```"
  ([^AFArray in]
   (regions in 4 5))
  ([^AFArray in connectivity dtype]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (regions/af-regions out (jvm/af-handle in) (int connectivity) (int dtype))
                 "af-regions")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn confidence-connected
  "Confidence connected region growing segmentation.
   
   Segments an image based on seed points and statistical criteria.
   
   Parameters:
   - in: Input image (AFArray)
   - seedx: Seed x coordinates (AFArray)
   - seedy: Seed y coordinates (AFArray)
   - radius: Neighborhood radius (int)
   - multiplier: Threshold multiplier (int)
   - iter: Number of iterations (int)
   - segmented-value: Value for segmented pixels (double)
   
   Returns:
   Segmented image (AFArray)"
  [^AFArray in ^AFArray seedx ^AFArray seedy radius multiplier iter segmented-value]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (confidence/af-confidence-cc out (jvm/af-handle in)
                                             (jvm/af-handle seedx) (jvm/af-handle seedy)
                                             (int radius) (int multiplier) (int iter)
                                             (double segmented-value))
                "af-confidence-cc")
    (jvm/af-array-new (jvm/deref-af-array out))))

;;;
;;; Advanced Image Processing
;;;

(defn unwrap
  "Image unwrapping for patch extraction.
   
   Extracts patches from an image into columns of output array.
   
   Parameters:
   - in: Input image (AFArray)
   - wx: Patch width (long)
   - wy: Patch height (long)
   - sx: Stride in x (long)
   - sy: Stride in y (long)
   - px: Padding in x (long), default 0
   - py: Padding in y (long), default 0
   - is-column: Column-major layout (boolean), default true
   
   Returns:
   Unwrapped patches (AFArray)"
  ([^AFArray in wx wy sx sy]
   (unwrap in wx wy sx sy 0 0 true))
  ([^AFArray in wx wy sx sy px py is-column]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (unwrap/af-unwrap out (jvm/af-handle in)
                                   (long wx) (long wy) (long sx) (long sy)
                                   (long px) (long py) (if is-column 1 0))
                 "af-unwrap")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn wrap
  "Image wrapping (inverse of unwrap).
   
   Reconstructs image from unwrapped patches.
   
   Parameters:
   - in: Input unwrapped array (AFArray)
   - ox: Output width (long)
   - oy: Output height (long)
   - wx: Patch width (long)
   - wy: Patch height (long)
   - sx: Stride in x (long)
   - sy: Stride in y (long)
   - px: Padding in x (long), default 0
   - py: Padding in y (long), default 0
   - is-column: Column-major layout (boolean), default true
   
   Returns:
   Wrapped image (AFArray)"
  ([^AFArray in ox oy wx wy sx sy]
   (wrap in ox oy wx wy sx sy 0 0 true))
  ([^AFArray in ox oy wx wy sx sy px py is-column]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (wrap/af-wrap out (jvm/af-handle in)
                               (long ox) (long oy) (long wx) (long wy)
                               (long sx) (long sy) (long px) (long py)
                               (if is-column 1 0))
                 "af-wrap")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn sat
  "Summed Area Table (integral image).
   
   Computes the integral image for fast region sum queries.
   
   Parameters:
   - in: Input image (AFArray)
   
   Returns:
   Summed area table (AFArray)
   
   Example:
   ```clojure
   (let [integral (sat img)]
     integral)
   ```"
  [^AFArray in]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (sat/af-sat out (jvm/af-handle in))
                "af-sat")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn anisotropic-diffusion
  "Anisotropic diffusion for edge-preserving denoising.
   
   Perona-Malik anisotropic diffusion smooths images while preserving edges.
   
   Parameters:
   - in: Input image (AFArray)
   - dt: Time step (float), default 0.125
   - K: Diffusion constant (float), default 0.1
   - iterations: Number of iterations (int), default 10
   - fftype: Flux function type (0=exponential, 1=quadratic), default 1
   - diffusion-eq: Equation type (0=grad, 1=mcde), default 0
   
   Returns:
   Diffused image (AFArray)
   
   Example:
   ```clojure
   (let [denoised (anisotropic-diffusion noisy-img 0.125 0.1 10)]
     denoised)
   ```"
  ([^AFArray in]
   (anisotropic-diffusion in 0.125 0.1 10 1 0))
  ([^AFArray in dt K iterations]
   (anisotropic-diffusion in dt K iterations 1 0))
  ([^AFArray in dt K iterations fftype diffusion-eq]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (aniso/af-anisotropic-diffusion out (jvm/af-handle in)
                                                 (float dt) (float K) (int iterations)
                                                 (int fftype) (int diffusion-eq))
                 "af-anisotropic-diffusion")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn iterative-deconv
  "Iterative deconvolution for image restoration.
   
   Parameters:
   - in: Input blurred image (AFArray)
   - ker: Point spread function kernel (AFArray)
   - iterations: Number of iterations (int)
   - relax-factor: Relaxation factor (float), default 1.0
   - algo: Algorithm (0=default, 1=lucy-richardson), default 0
   
   Returns:
   Deconvolved image (AFArray)"
  ([^AFArray in ^AFArray ker iterations]
   (iterative-deconv in ker iterations 1.0 0))
  ([^AFArray in ^AFArray ker iterations relax-factor algo]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (deconv/af-iterative-deconv out (jvm/af-handle in) (jvm/af-handle ker)
                                             (int iterations) (float relax-factor) (int algo))
                 "af-iterative-deconv")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn inverse-deconv
  "Inverse filter deconvolution.
   
   Parameters:
   - in: Input blurred image (AFArray)
   - psf: Point spread function (AFArray)
   - gamma: Regularization parameter (float), default 1.0
   - algo: Algorithm (0=default, 1=tikhonov), default 0
   
   Returns:
   Deconvolved image (AFArray)"
  ([^AFArray in ^AFArray psf]
   (inverse-deconv in psf 1.0 0))
  ([^AFArray in ^AFArray psf gamma algo]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (deconv/af-inverse-deconv out (jvm/af-handle in) (jvm/af-handle psf)
                                           (float gamma) (int algo))
                 "af-inverse-deconv")
     (jvm/af-array-new (jvm/deref-af-array out)))))
