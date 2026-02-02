(ns org.soulspace.arrayfire.ffi.colorspace
  "Bindings for ArrayFire colorspace conversion functions.
   
   Maps to src/api/c/hsv_rgb.cpp, src/api/c/rgb_gray.cpp, src/api/c/ycbcr_rgb.cpp,
   and src/api/c/colorspace.cpp in ArrayFire."
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; Colorspace conversion constants (af_cspace_t enum):
;; AF_GRAY = 0   - Grayscale
;; AF_RGB = 1    - 3-channel RGB
;; AF_HSV = 2    - 3-channel HSV
;; AF_YCbCr = 3  - 3-channel YCbCr (requires API version >= 31)

;; YCbCr standard constants (af_ycc_std enum):
;; AF_YCC_601 = 601   - ITU-R BT.601 (formerly CCIR 601) standard
;; AF_YCC_709 = 709   - ITU-R BT.709 standard
;; AF_YCC_2020 = 2020 - ITU-R BT.2020 standard

;; af_err af_rgb2gray(af_array *out, const af_array in, const float rPercent, const float gPercent, const float bPercent)
(defcfn af-rgb2gray
  "Convert RGB image to grayscale.
   
   Converts a 3-channel RGB image to a single-channel grayscale image using
   weighted color channel contributions:
   
   Gray = rPercent * R + gPercent * G + bPercent * B
   
   Default weights (ITU-R BT.709): R=0.2126, G=0.7152, B=0.0722
   
   Parameters:
   - out: out pointer for grayscale array
   - in: RGB array handle (must be 3-dimensional with 3 channels)
   - rPercent: weight for red channel (default 0.2126)
   - gPercent: weight for green channel (default 0.7152)
   - bPercent: weight for blue channel (default 0.0722)

   Returns:
   ArrayFire error code (AF_SUCCESS on success)"
  "af_rgb2gray" [::mem/pointer ::mem/pointer ::mem/float ::mem/float ::mem/float] ::mem/int)

;; af_err af_gray2rgb(af_array *out, const af_array in, const float rFactor, const float gFactor, const float bFactor)
(defcfn af-gray2rgb
  "Convert grayscale image to RGB.
   
   Converts a single-channel grayscale image to a 3-channel RGB image by
   replicating the intensity value with optional scaling factors:
   
   R = rFactor * Gray
   G = gFactor * Gray
   B = bFactor * Gray
   
   Parameters:
   - out: out pointer for RGB array (3-dimensional with 3 channels)
   - in: grayscale array handle (must be 2-dimensional)
   - rFactor: scaling factor for red channel (default 1.0)
   - gFactor: scaling factor for green channel (default 1.0)
   - bFactor: scaling factor for blue channel (default 1.0)

   Returns:
   ArrayFire error code (AF_SUCCESS on success)"
  "af_gray2rgb" [::mem/pointer ::mem/pointer ::mem/float ::mem/float ::mem/float] ::mem/int)

;; af_err af_hsv2rgb(af_array *out, const af_array in)
(defcfn af-hsv2rgb
  "Convert HSV colorspace to RGB.
   
   Converts a 3-channel HSV (Hue, Saturation, Value) image to RGB colorspace.
   
   HSV is a cylindrical color model where:
   - Hue represents color type (0-360 degrees, normalized to 0-1)
   - Saturation represents color intensity (0-1)
   - Value represents brightness (0-1)
   
   Parameters:
   - out: out pointer for RGB array
   - in: HSV array handle (must be 3-dimensional with 3 channels)

   Returns:
   ArrayFire error code (AF_SUCCESS on success)
   
   Supported types: f32, f64"
  "af_hsv2rgb" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_rgb2hsv(af_array *out, const af_array in)
(defcfn af-rgb2hsv
  "Convert RGB colorspace to HSV.
   
   Converts a 3-channel RGB image to HSV (Hue, Saturation, Value) colorspace.
   
   Output HSV values are normalized:
   - Hue: 0-1 (representing 0-360 degrees)
   - Saturation: 0-1
   - Value: 0-1
   
   Parameters:
   - out: out pointer for HSV array
   - in: RGB array handle (must be 3-dimensional with 3 channels)

   Returns:
   ArrayFire error code (AF_SUCCESS on success)
   
   Supported types: f32, f64"
  "af_rgb2hsv" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_ycbcr2rgb(af_array *out, const af_array in, const af_ycc_std standard)
(defcfn af-ycbcr2rgb
  "Convert YCbCr colorspace to RGB.
   
   Converts a 3-channel YCbCr image to RGB colorspace. YCbCr is a family of
   color spaces used in video and digital photography systems where:
   - Y: Luma (brightness)
   - Cb: Blue-difference chroma
   - Cr: Red-difference chroma
   
   The conversion coefficients depend on the ITU-R BT standard specified.
   
   Parameters:
   - out: out pointer for RGB array
   - in: YCbCr array handle (must be 3-dimensional, values in range [0,1])
   - standard: ITU-R BT standard (601, 709, or 2020)

   Returns:
   ArrayFire error code (AF_SUCCESS on success)
   
   Requires API version >= 31"
  "af_ycbcr2rgb" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_rgb2ycbcr(af_array *out, const af_array in, const af_ycc_std standard)
(defcfn af-rgb2ycbcr
  "Convert RGB colorspace to YCbCr.
   
   Converts a 3-channel RGB image to YCbCr colorspace. The conversion
   coefficients (Kb, Kr values) are determined by the ITU-R BT standard.
   
   Common standards:
   - BT.601 (601): Standard-definition TV (SDTV)
   - BT.709 (709): High-definition TV (HDTV)
   - BT.2020 (2020): Ultra-high-definition TV (UHDTV)
   
   Parameters:
   - out: out pointer for YCbCr array
   - in: RGB array handle (must be 3-dimensional, values in range [0,1])
   - standard: ITU-R BT standard (601, 709, or 2020)

   Returns:
   ArrayFire error code (AF_SUCCESS on success)
   
   Requires API version >= 31"
  "af_rgb2ycbcr" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_color_space(af_array *out, const af_array image, const af_cspace_t to, const af_cspace_t from)
(defcfn af-color-space
  "General colorspace conversion wrapper.
   
   Converts an image from one colorspace to another using a unified interface.
   This function dispatches to the appropriate conversion function based on
   source and target colorspaces.
   
   Supported colorspaces:
   - AF_GRAY (0): Grayscale
   - AF_RGB (1): RGB
   - AF_HSV (2): HSV
   - AF_YCbCr (3): YCbCr (requires API version >= 31)
   
   Conversion requirements:
   - RGB/HSV/YCbCr conversions: input must be 3-dimensional
   - GRAY to RGB: input must be 2-dimensional
   - RGB to GRAY: input must be 3-dimensional with 3 channels
   
   If source and target are the same, returns a retained reference to input.
   
   Parameters:
   - out: out pointer for converted array
   - image: input array handle
   - to: target colorspace (af_cspace_t enum value)
   - from: source colorspace (af_cspace_t enum value)

   Returns:
   ArrayFire error code (AF_SUCCESS on success)"
  "af_color_space" [::mem/pointer ::mem/pointer ::mem/int ::mem/int] ::mem/int)
