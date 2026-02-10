(ns org.soulspace.arrayfire.ffi.c-api.ycbcr-rgb
  "Bindings for the ArrayFire YCbCr ↔ RGB color space conversion functions.
   
   YCbCr Color Space Conversion:
   
   YCbCr is a color space commonly used in video and digital photography systems.
   It separates luminance (Y) from chrominance (Cb, Cr), enabling efficient
   compression and processing. These functions provide conversion between RGB
   and YCbCr color spaces following ITU-R BT standards.
   
   Mathematical Foundation:
   
   **RGB to YCbCr Conversion**:
   Y  = Kr*R + Kg*G + Kb*B               (Luminance)
   Cb = 0.5 * (B - Y) / (1 - Kb)        (Blue-difference chroma)
   Cr = 0.5 * (R - Y) / (1 - Kr)        (Red-difference chroma)
   
   Digitized (8-bit range):
   Y'  = 16  + 219 * Y
   Cb' = 128 + 224 * Cb
   Cr' = 128 + 224 * Cr
   
   **YCbCr to RGB Conversion**:
   R = Y + (Cr - 128) * 2 * (1 - Kr) / 224
   G = Y - (Cb - 128) * 2 * Kb * (1 - Kb) / (224 * Kg) 
         - (Cr - 128) * 2 * Kr * (1 - Kr) / (224 * Kg)
   B = Y + (Cb - 128) * 2 * (1 - Kb) / 224
   
   Where Kg = 1 - Kr - Kb (green coefficient)
   
   ITU-R BT Standards:
   
   Different standards define different Kr, Kb coefficients:
   
   1. **BT.601 (AF_YCC_601 = 601)** - Standard Definition TV:
      - Kr = 0.299, Kb = 0.114, Kg = 0.587
      - Originally CCIR 601, most common in SD video
      - Used in DVD, analog TV, many JPEG images
      - Color gamut: Rec. 601
   
   2. **BT.709 (AF_YCC_709 = 709)** - High Definition TV:
      - Kr = 0.2126, Kb = 0.0722, Kg = 0.7152
      - Standard for HD video (720p, 1080p)
      - Used in Blu-ray, HDTV, most HD content
      - Color gamut: Rec. 709
   
   3. **BT.2020 (AF_YCC_2020 = 2020)** - Ultra High Definition TV:
      - Kr = 0.2627, Kb = 0.0593, Kg = 0.6780
      - Standard for 4K/8K UHDTV
      - Wider color gamut: Rec. 2020
      - Future-proof for HDR content
   
   Key Concepts:
   
   1. **Luminance (Y)**:
      - Brightness/intensity information
      - Most important for human perception
      - Can be processed independently (e.g., for sharpening)
      - Range: [16, 235] in 8-bit digital
   
   2. **Chrominance (Cb, Cr)**:
      - Color information separate from brightness
      - Cb: Blue-difference component
      - Cr: Red-difference component
      - Can be subsampled without noticeable quality loss
      - Range: [16, 240] in 8-bit digital
   
   3. **Chroma Subsampling**:
      - YCbCr enables efficient compression via chroma subsampling
      - 4:4:4 - Full resolution chroma (no subsampling)
      - 4:2:2 - Half horizontal chroma resolution
      - 4:2:0 - Half horizontal and vertical chroma (JPEG, H.264)
      - Not directly handled by these functions (requires separate resampling)
   
   Input/Output Requirements:
   
   - **Input Arrays**:
     * Must be 3-dimensional (width × height × 3)
     * Channel order: RGB or YCbCr (depending on conversion)
     * Values should be in range [0, 1] (floating point)
     * Type: f32 or f64 (float or double precision)
   
   - **Output Arrays**:
     * Same dimensions as input [width × height × 3]
     * Automatically allocated
     * Same type as input (f32 or f64)
   
   Value Ranges and Normalization:
   
   - Input values assumed to be normalized to [0, 1]
   - For 8-bit data [0, 255], divide by 255 before conversion
   - Output values are in [0, 1] range
   - Multiply by 255 for 8-bit representation if needed
   - Values may exceed [0, 1] due to rounding or out-of-gamut colors
   
   Performance:
   
   - GPU-accelerated parallel processing
   - O(N) complexity where N = width × height
   - Efficient for real-time video processing
   - Batch processing: Process multiple frames by adding 4th dimension
   
   Common Applications:
   
   1. **Video Processing**:
      - Color space conversion for encoding/decoding
      - YCbCr used in video codecs (H.264, H.265, VP9)
      - Chroma subsampling for compression
      - Color correction in Y channel
   
   2. **Image Compression**:
      - JPEG compression uses YCbCr
      - Perceptual quantization (more aggressive in Cb, Cr)
      - DCT applied separately to Y, Cb, Cr
   
   3. **Computer Vision**:
      - Skin detection (thresholding in CbCr space)
      - Face detection preprocessing
      - Object tracking based on color
      - Shadow removal (invariant to luminance)
   
   4. **Image Processing**:
      - Separate processing of brightness and color
      - Sharpening only luminance (avoid color artifacts)
      - Noise reduction in chroma channels
      - Color grading and tone mapping
   
   5. **Broadcast and Display**:
      - TV signal transmission
      - Display calibration
      - Color space matching for different devices
   
   Typical Workflow:
   
   ```clojure
   ;; RGB to YCbCr conversion
   (let [rgb-img (create-array rgb-data [640 480 3])
         ;; Normalize to [0, 1] if needed
         rgb-normalized (af-div rgb-img 255.0)
         
         ;; Convert to YCbCr (BT.709 for HD content)
         ycbcr-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-rgb2ycbcr ycbcr-ptr rgb-normalized AF_YCC_709)
         ycbcr (mem/read-pointer ycbcr-ptr ::mem/pointer)]
     
     ;; Process in YCbCr space (e.g., sharpen only Y channel)
     (let [processed-ycbcr (process-luminance ycbcr)
           
           ;; Convert back to RGB
           rgb-out-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-ycbcr2rgb rgb-out-ptr processed-ycbcr AF_YCC_709)
           result (mem/read-pointer rgb-out-ptr ::mem/pointer)]
       result))
   ```
   
   Standard Selection Guidelines:
   
   - **Use BT.601** for:
     * SD video content (DVD, analog TV)
     * Legacy systems and compatibility
     * JPEG images (common default)
   
   - **Use BT.709** for:
     * HD video (720p, 1080p)
     * Blu-ray content
     * Modern displays and cameras
     * General-purpose HD processing
   
   - **Use BT.2020** for:
     * 4K/8K UHDTV content
     * HDR video processing
     * Wide color gamut workflows
     * Future-proof applications
   
   Important Notes:
   
   - Conversion is lossy due to finite precision
   - Round-trip RGB→YCbCr→RGB may have small errors
   - Standard must match source content for accurate colors
   - Mismatch causes color shifts (e.g., 709 content with 601 conversion)
   - Values outside [0, 1] should be clamped before display
   - Consider gamma correction for accurate color reproduction
   
   Type Support:
   - Floating point only: f32 (float), f64 (double)
   - Integer types not supported (use conversion to float first)
   
   Available in API version 3.1 and later.
   
   See also:
   - af-rgb2gray: Convert RGB to grayscale
   - af-gray2rgb: Convert grayscale to RGB
   - af-hsv-rgb: HSV color space conversion
   - af-colorspace: General color space conversion"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.loader]))

;; YCbCr standard constants (af_ycc_std enum values)
(def AF_YCC_601 601)   ; ITU-R BT.601 (SD TV, DVD, JPEG)
(def AF_YCC_709 709)   ; ITU-R BT.709 (HD TV, Blu-ray)
(def AF_YCC_2020 2020) ; ITU-R BT.2020 (4K/8K UHDTV, HDR)

;; af_err af_ycbcr2rgb(af_array* out, const af_array in, const af_ycc_std standard)
(defcfn af-ycbcr2rgb
  "Convert YCbCr color space to RGB color space.
   
   Converts an image from YCbCr (luminance-chrominance) representation to
   RGB (red-green-blue) representation using the specified ITU-R BT standard.
   
   Parameters:
   - out: Output pointer for RGB image
   - in: Input YCbCr image (3 channels: Y, Cb, Cr)
   - standard: ITU-R BT standard enum value:
     * AF_YCC_601 (601): BT.601 - Standard Definition TV
     * AF_YCC_709 (709): BT.709 - High Definition TV  
     * AF_YCC_2020 (2020): BT.2020 - Ultra HD TV
   
   Input Requirements:
   - Must be 3-dimensional: [width, height, 3]
   - Channel 0: Y (luminance) in range [0, 1]
   - Channel 1: Cb (blue-difference) in range [0, 1]
   - Channel 2: Cr (red-difference) in range [0, 1]
   - Type: f32 or f64 only
   
   Output:
   - RGB image [width, height, 3]
   - Channel 0: R (red) in range [0, 1]
   - Channel 1: G (green) in range [0, 1]
   - Channel 2: B (blue) in range [0, 1]
   - Same type as input
   
   Conversion Formulas (varies by standard):
   For BT.709 (Kr=0.2126, Kb=0.0722):
   R = Y + 1.5748*(Cr - 128/255)
   G = Y - 0.1873*(Cb - 128/255) - 0.4681*(Cr - 128/255)
   B = Y + 1.8556*(Cb - 128/255)
   
   Example (basic conversion):
   ```clojure
   ;; Convert HD YCbCr video frame to RGB
   (let [ycbcr-frame (create-array ycbcr-data [1920 1080 3])
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-ycbcr2rgb out-ptr ycbcr-frame AF_YCC_709)
         rgb-frame (mem/read-pointer out-ptr ::mem/pointer)]
     rgb-frame)
   ```
   
   Example (with standard selection):
   ```clojure
   ;; Convert based on content type
   (let [ycbcr-img (load-ycbcr-image \"frame.yuv\")
         standard (if (hd-content? ycbcr-img)
                    AF_YCC_709    ; HD content
                    AF_YCC_601)   ; SD content
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-ycbcr2rgb out-ptr ycbcr-img standard)
         rgb (mem/read-pointer out-ptr ::mem/pointer)]
     ;; Clamp to [0, 1] and scale to 8-bit if needed
     (-> rgb
         (af-clamp 0.0 1.0)
         (af-mul 255.0)))
   ```
   
   Example (video processing pipeline):
   ```clojure
   ;; Process YCbCr video, convert to RGB for display
   (doseq [ycbcr-frame video-frames]
     (let [;; Apply processing in YCbCr space
           processed-y (sharpen-luminance ycbcr-frame)
           
           ;; Convert to RGB for display
           rgb-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-ycbcr2rgb rgb-ptr processed-y AF_YCC_709)
           rgb (mem/read-pointer rgb-ptr ::mem/pointer)]
       (display-frame rgb)))
   ```
   
   Example (batch processing):
   ```clojure
   ;; Convert multiple YCbCr frames at once
   (let [ycbcr-batch (create-array batch-data [640 480 3 10])  ; 10 frames
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-ycbcr2rgb out-ptr ycbcr-batch AF_YCC_709)
         rgb-batch (mem/read-pointer out-ptr ::mem/pointer)]
     ;; Result has dims [640 480 3 10]
     rgb-batch)
   ```
   
   Common Use Cases:
   - Video decoding (YCbCr from codec → RGB for display)
   - JPEG decompression (YCbCr → RGB)
   - After YCbCr-space processing (luminance sharpening, etc.)
   - Color space conversion for display devices
   - Video frame analysis in RGB space
   
   Standard Selection:
   - Match the standard to your content's original encoding
   - Wrong standard causes color shifts (red/blue tints)
   - When uncertain, BT.709 is most common for modern content
   - Check video metadata or container info for standard
   
   Performance:
   - O(width × height) complexity
   - Fully parallel on GPU
   - ~0.5-2ms for 1080p on modern GPU
   - Efficient for real-time video
   
   Error Conditions:
   - Input not 3-dimensional → AF_ERR_SIZE
   - Input type not f32/f64 → AF_ERR_TYPE
   - Invalid standard value → undefined (uses BT.601 default)
   
   Available in API version 3.1 and later.
   
   Returns:
   AF_SUCCESS or error code
   
   See also:
   - af-rgb2ycbcr: Inverse conversion (RGB → YCbCr)
   - af-colorspace: General color space conversion"
  "af_ycbcr2rgb" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_rgb2ycbcr(af_array* out, const af_array in, const af_ycc_std standard)
(defcfn af-rgb2ycbcr
  "Convert RGB color space to YCbCr color space.
   
   Converts an image from RGB (red-green-blue) representation to YCbCr
   (luminance-chrominance) representation using the specified ITU-R BT standard.
   Useful for video encoding, compression, and processing where separate
   luminance/chrominance is beneficial.
   
   Parameters:
   - out: Output pointer for YCbCr image
   - in: Input RGB image (3 channels: R, G, B)
   - standard: ITU-R BT standard enum value:
     * AF_YCC_601 (601): BT.601 - SD TV (Kr=0.299, Kb=0.114)
     * AF_YCC_709 (709): BT.709 - HD TV (Kr=0.2126, Kb=0.0722)
     * AF_YCC_2020 (2020): BT.2020 - UHD TV (Kr=0.2627, Kb=0.0593)
   
   Input Requirements:
   - Must be 3-dimensional: [width, height, 3]
   - Channel 0: R (red) in range [0, 1]
   - Channel 1: G (green) in range [0, 1]
   - Channel 2: B (blue) in range [0, 1]
   - Type: f32 or f64 only
   
   Output:
   - YCbCr image [width, height, 3]
   - Channel 0: Y (luminance) in range [0, 1]
   - Channel 1: Cb (blue-difference) in range [0, 1]
   - Channel 2: Cr (red-difference) in range [0, 1]
   - Same type as input
   
   Conversion Formulas (BT.709):
   Y  = 0.2126*R + 0.7152*G + 0.0722*B
   Cb = (B - Y) / (2 * (1 - 0.0722)) + 0.5
   Cr = (R - Y) / (2 * (1 - 0.2126)) + 0.5
   
   Then digitized to [0, 1] range:
   Y'  = (16 + 219*Y) / 255
   Cb' = (128 + 224*Cb) / 255
   Cr' = (128 + 224*Cr) / 255
   
   Example (basic conversion):
   ```clojure
   ;; Convert RGB image to YCbCr for processing
   (let [rgb-img (load-rgb-image \"photo.png\")
         ;; Normalize to [0, 1]
         rgb-norm (af-div rgb-img 255.0)
         
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-rgb2ycbcr out-ptr rgb-norm AF_YCC_709)
         ycbcr (mem/read-pointer out-ptr ::mem/pointer)]
     ycbcr)
   ```
   
   Example (luminance-only processing):
   ```clojure
   ;; Sharpen only brightness, preserve original color
   (let [rgb (load-image \"photo.jpg\")
         ;; Convert to YCbCr
         ycbcr-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-rgb2ycbcr ycbcr-ptr rgb AF_YCC_709)
         ycbcr (mem/read-pointer ycbcr-ptr ::mem/pointer)
         
         ;; Extract Y channel (luminance)
         y-channel (af-index ycbcr [af_span af_span {:seq [0 0 1]}])
         
         ;; Sharpen luminance
         sharpened-y (apply-unsharp-mask y-channel)
         
         ;; Replace Y channel
         ycbcr-sharp (af-replace-channel ycbcr 0 sharpened-y)
         
         ;; Convert back to RGB
         rgb-out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-ycbcr2rgb rgb-out-ptr ycbcr-sharp AF_YCC_709)
         result (mem/read-pointer rgb-out-ptr ::mem/pointer)]
     result)
   ```
   
   Example (video encoding preparation):
   ```clojure
   ;; Prepare RGB frames for H.264 encoding
   (let [rgb-frames (load-video-frames video-file)
         standard AF_YCC_709  ; HD video standard
         
         ycbcr-frames
         (for [rgb-frame rgb-frames]
           (let [out-ptr (mem/alloc-pointer ::mem/pointer)
                 _ (af-rgb2ycbcr out-ptr rgb-frame standard)
                 ycbcr (mem/read-pointer out-ptr ::mem/pointer)]
             ycbcr))]
     ;; ycbcr-frames ready for chroma subsampling and encoding
     ycbcr-frames)
   ```
   
   Example (skin detection preprocessing):
   ```clojure
   ;; Convert to YCbCr for skin tone detection
   (let [rgb (load-image \"person.jpg\")
         ycbcr-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-rgb2ycbcr ycbcr-ptr rgb AF_YCC_601)
         ycbcr (mem/read-pointer ycbcr-ptr ::mem/pointer)
         
         ;; Extract Cb, Cr channels
         cb (af-index ycbcr [af_span af_span {:seq [1 1 1]}])
         cr (af-index ycbcr [af_span af_span {:seq [2 2 1]}])
         
         ;; Skin detection in CbCr space
         skin-mask (detect-skin-tone cb cr)]
     skin-mask)
   ```
   
   Example (batch conversion):
   ```clojure
   ;; Convert multiple RGB images to YCbCr
   (let [rgb-batch (create-array batch-data [512 512 3 20])  ; 20 images
         out-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-rgb2ycbcr out-ptr rgb-batch AF_YCC_709)
         ycbcr-batch (mem/read-pointer out-ptr ::mem/pointer)]
     ;; Result has dims [512 512 3 20]
     ycbcr-batch)
   ```
   
   Common Use Cases:
   - Video encoding preparation (RGB capture → YCbCr for codec)
   - JPEG compression (DCT on YCbCr channels)
   - Luminance-only processing (sharpening, contrast)
   - Chroma subsampling for compression
   - Skin detection (thresholding in CbCr space)
   - Color correction (separate brightness/color adjustment)
   - Shadow removal (luminance invariant features)
   
   Standard Selection:
   - SD content (480p/576p): Use AF_YCC_601
   - HD content (720p/1080p): Use AF_YCC_709
   - UHD content (4K/8K): Use AF_YCC_2020
   - JPEG images: Typically AF_YCC_601 (but varies)
   - Match output standard to target display/codec
   
   Performance:
   - O(width × height) complexity
   - Fully parallel on GPU
   - ~0.5-2ms for 1080p on modern GPU
   - Minimal overhead compared to other operations
   
   Advantages of YCbCr:
   - Separate luminance/chrominance enables:
     * Efficient compression (chroma subsampling)
     * Perceptual quantization (human vision model)
     * Independent processing of brightness/color
     * Backward compatibility with B&W displays
   
   Error Conditions:
   - Input not 3-dimensional → AF_ERR_SIZE
   - Input type not f32/f64 → AF_ERR_TYPE
   - Invalid standard value → undefined (uses BT.601 default)
   
   Available in API version 3.1 and later.
   
   Returns:
   AF_SUCCESS or error code
   
   See also:
   - af-ycbcr2rgb: Inverse conversion (YCbCr → RGB)
   - af-rgb2gray: RGB to grayscale (luminance only)
   - af-colorspace: General color space conversion"
  "af_rgb2ycbcr" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)
