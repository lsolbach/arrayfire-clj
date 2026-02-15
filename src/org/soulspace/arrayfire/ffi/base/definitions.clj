(ns org.soulspace.arrayfire.ffi.base.definitions
  "Base definitions for ArrayFire integration, including constants and enums.")

;;;
;;; ArrayFire constants and enums
;;;

;; AF error codes (af/defines.h - enum af_err)
(def AF_SUCCESS 0)
(def AF_ERR_NO_MEM 101)
(def AF_ERR_DRIVER 102)
(def AF_ERR_RUNTIME 103)
(def AF_ERR_INVALID_ARRAY 201)
(def AF_ERR_ARG 202)
(def AF_ERR_SIZE 203)
(def AF_ERR_TYPE 204)
(def AF_ERR_DIFF_TYPE 205)
(def AF_ERR_BATCH 207)
(def AF_ERR_DEVICE 208)
(def AF_ERR_NOT_SUPPORTED 301)
(def AF_ERR_NOT_CONFIGURED 302)
(def AF_ERR_NONFREE 303)
(def AF_ERR_NO_DBL 401)
(def AF_ERR_NO_GFX 402)
(def AF_ERR_NO_HALF 403)
(def AF_ERR_LOAD_LIB 501)
(def AF_ERR_LOAD_SYM 502)
(def AF_ERR_ARR_BKND_MISMATCH 503)
(def AF_ERR_INTERNAL 998)
(def AF_ERR_UNKNOWN 999)

;; AF data type constants (af/defines.h - enum af_dtype)
(def AF_DTYPE_F32 0)   ; float
(def AF_DTYPE_C32 1)   ; complex float
(def AF_DTYPE_F64 2)   ; double
(def AF_DTYPE_C64 3)   ; complex double
(def AF_DTYPE_B8  4)   ; bool
(def AF_DTYPE_S32 5)   ; int
(def AF_DTYPE_U32 6)   ; unsigned int
(def AF_DTYPE_U8  7)   ; unsigned char
(def AF_DTYPE_S64 8)   ; long long
(def AF_DTYPE_U64 9)   ; unsigned long long
(def AF_DTYPE_S16 10)  ; short
(def AF_DTYPE_U16 11)  ; unsigned short

(def dtype->size
  "Size in bytes for each ArrayFire dtype."
  {AF_DTYPE_F32 4    ; float
   AF_DTYPE_C32 8    ; complex float (2 floats)
   AF_DTYPE_F64 8    ; double
   AF_DTYPE_C64 16   ; complex double (2 doubles)
   AF_DTYPE_B8  1    ; bool
   AF_DTYPE_S32 4    ; int
   AF_DTYPE_U32 4    ; unsigned int
   AF_DTYPE_U8  1    ; unsigned char
   AF_DTYPE_S64 8    ; long long
   AF_DTYPE_U64 8    ; unsigned long long
   AF_DTYPE_S16 2    ; short
   AF_DTYPE_U16 2})  ; unsigned short

;; AF source constants (af/defines.h - enum af_source)
(def AF_SOURCE_DEVICE 0)
(def AF_SOURCE_HOST 1)

;; AF interpolation constants (af/defines.h - enum af_interp_type)
(def AF_INTERP_NEAREST 0)
(def AF_INTERP_LINEAR 1)
(def AF_INTERP_BILINEAR 2)
(def AF_INTERP_CUBIC 3)
(def AF_INTERP_LOWER 4)
(def AF_INTERP_LINEAR_COSINE 5)
(def AF_INTERP_BILINEAR_COSINE 6)
(def AF_INTERP_BICUBIC 7)
(def AF_INTERP_CUBIC_SPLINE 8)
(def AF_INTERP_BICUBIC_SPLINE 9)

;; AF border type constants (af/defines.h - enum af_border_type)
(def AF_PAD_ZERO 0)
(def AF_PAD_SYM 1)
(def AF_PAD_CLAMP_TO_EDGE 2)
(def AF_PAD_PERIODIC 3)

;; AF connectivity constants (af/defines.h - enum af_connectivity)
(def AF_CONNECTIVITY_4 4) ; Connectivity includes neighbors, North, East, South and West of current pixel
(def AF_CONNECTIVITY_8 8) ; Connectivity includes 4-connectivity neigbors and also those on Northeast, Northwest, Southeast and Southwest

;; AF convolution mode constants (af/defines.h - enum af_conv_mode)
(def AF_CONV_DEFAULT 0) ; Output of the convolution is the same size as input
(def AF_CONV_EXPAND 1)  ; Output of the convolution is signal_len + filter_len - 1

;; AF convolution domain constants (af/defines.h - enum af_conv_domain)
(def AF_CONV_AUTO 0)    ; Automatic selection based on input sizes
(def AF_CONV_SPATIAL 1) ; Spatial domain convolution
(def AF_CONV_FREQ 2)    ; Frequency domain convolution

;; AF match type constants (af/defines.h - enum af_match_type)
(def AF_SAD 0)  ; Sum of Absolute Differences
(def AF_ZSAD 1) ; Zero-mean Sum of Absolute Differences
(def AF_LSAD 2) ; Locally scaled Sum of Absolute Differences
(def AF_SSD 3)  ; Sum of Squared Differences
(def AF_ZSSD 4) ; Zero-mean Sum of Squared Differences
(def AF_LSSD 5) ; Locally scaled Sum of Squared Differences
(def AF_NCC 6)  ; Normalized Cross Correlation
(def AF_ZNCC 7) ; Zero-mean Normalized Cross Correlation
(def AF_SHD 8)  ; Sum of Hamming Distances

;; AF YCC standard constants (af/defines.h - enum af_ycc_std)
(def AF_YCC_601 0) ; ITU-R BT.601 standard
(def AF_YCC_709 1) ; ITU-R BT.709 standard
(def AF_YCC_2020 2) ; ITU-R BT.2020 standard

;; AF color space constants (af/defines.h - enum af_cspace_t)
(def AF_GRAY 0) ; Grayscale color space
(def AF_RGB 1)  ; RGB color space
(def AF_HSV 2)  ; HSV color space
(def AF_YCbCr 3) ; YCbCr color space

;; AF matrix property constants (af/defines.h - enum af_mat_prop)
(def AF_MAT_NONE 0)          ; Default
(def AF_MAT_TRANS 1)         ; Data needs to be transposed
(def AF_MAT_CTRANS 2)        ; Data needs to be conjugate transposed
(def AF_MAT_CONJ 4)          ; Data needs to be conjugate
(def AF_MAT_UPPER 32)        ; Matrix is upper triangular
(def AF_MAT_LOWER 64)        ; Matrix is lower triangular
(def AF_MAT_DIAG_UNIT 128)   ; Matrix diagonal contains unitary values
(def AF_MAT_SYM 512)         ; Matrix is symmetric
(def AF_MAT_POSDEF 1024)     ; Matrix is positive definite
(def AF_MAT_ORTHOG 2048)     ; Matrix is orthogonal
(def AF_MAT_TRI_DIAG 4096)   ; Matrix is tri-diagonal
(def AF_MAT_BLOCK_DIAG 8192) ; Matrix is block-diagonal

;; AF norm type constants (af/defines.h - enum af_norm_type)
(def AF_NORM_VECTOR_1 0)              ; treats the input as a vector and returns the sum of absolute values
(def AF_NORM_VECTOR_INF 1)            ; treats the input as a vector and returns the max of absolute values
(def AF_NORM_VECTOR_2 2)              ; treats the input as a vector and returns euclidean norm
(def AF_NORM_VECTOR_P 3)              ; treats the input as a vector and returns the p-norm
(def AF_NORM_MATRIX_1 4)              ; return the max of column sums
(def AF_NORM_MATRIX_INF 5)            ; return the max of row sums
(def AF_NORM_MATRIX_2 6)              ; returns the max singular value). Currently NOT SUPPORTED
(def AF_NORM_MATRIX_L_PQ 7)           ; returns Lpq-norm
(def AF_NORM_EUCLID AF_NORM_VECTOR_2) ; Default, Euclidean norm

;; AF image format constants (af/defines.h - enum af_image_format)
(def AF_FIF_BMP 0)    ; FreeImage Enum for Bitmap File
(def AF_FIF_ICO 1)    ; FreeImage Enum for Windows Icon File
(def AF_FIF_JPEG 2)   ; FreeImage Enum for JPEG File
(def AF_FIF_JNG 3)    ; FreeImage Enum for JPEG Network Graphics File
(def AF_FIF_PNG 13)   ; FreeImage Enum for Portable Network Graphics File
(def AF_FIF_PPM 14)   ; FreeImage Enum for Portable Pixelmap (ASCII) File
(def AF_FIF_PPMRAW 15) ; FreeImage Enum for Portable Pixelmap (Binary) File
(def AF_FIF_TIFF 18)  ; FreeImage Enum for Tagged Image File Format File
(def AF_FIF_PSD 20)   ; FreeImage Enum for Adobe Photoshop File
(def AF_FIF_HDR 26)   ; FreeImage Enum for High Dynamic Range File
(def AF_FIF_EXR 29)   ; FreeImage Enum for ILM OpenEXR File
(def AF_FIF_JP2 31)   ; FreeImage Enum for JPEG-2000 File
(def AF_FIF_RAW 34)   ; FreeImage Enum for RAW Camera Image File

;; AF moment type constants (af/defines.h - enum af_moment_type)
(def AF_MOMENT_M00 1)
(def AF_MOMENT_M01 2)
(def AF_MOMENT_M10 4)
(def AF_MOMENT_M11 8)
(def AF_MOMENT_FIRST_ORDER (bit-or AF_MOMENT_M00 AF_MOMENT_M01 AF_MOMENT_M10 AF_MOMENT_M11))

;; AF homography type constants (af/defines.h - enum af_homography_type)
(def AF_HOMOGRAPHY_RANSAC 0) ; Computes homography using RANSAC
(def AF_HOMOGRAPHY_LMEDS 1)   ; Computes homography using Least Median of Squares

;; AF backend type constants (af/defines.h - enum af_backend)
(def AF_BACKEND_DEFAULT 0) ; Default backend for the platform
(def AF_BACKEND_CPU 1)     ; CPU backend
(def AF_BACKEND_CUDA 2)   ; CUDA backend
(def AF_BACKEND_OPENCL 4)  ; OpenCL backend
(def AF_BACKEND_ONEAPI 8)  ; oneAPI backend

;; AF binary operator type constants (af/defines.h - enum af_binary_op)
(def AF_BINARY_ADD 0)      ; Addition
(def AF_BINARY_MUL 1)      ; Multiplication
(def AF_BINARY_MIN 2)      ; Minimum
(def AF_BINARY_MAX 3)      ; Maximum

;; AF random engine type constants (af/defines.h - enum af_random_engine)
(def AF_RANDOM_ENGINE_PHILOX_4X32_10 100)     ; Philox variant with N = 4, W = 32 and Rounds = 10
(def AF_RANDOM_ENGINE_THREEFRY_2X32_16 200)   ; Threefry variant with N = 2, W = 32 and Rounds = 16
(def AF_RANDOM_ENGINE_MERSENNE_GP11213 300)   ; Mersenne variant with MEXP = 11213
(def AF_RANDOM_ENGINE_PHILOX AF_RANDOM_ENGINE_PHILOX_4X32_10) ; Resolves to Philox 4x32_10
(def AF_RANDOM_ENGINE_THREEFRY AF_RANDOM_ENGINE_THREEFRY_2X32_16) ; Resolves to Threefry 2X32_16
(def AF_RANDOM_ENGINE_MERSENNE AF_RANDOM_ENGINE_MERSENNE_GP11213) ; Resolves to Mersenne GP 11213
(def AF_RANDOM_ENGINE_DEFAULT AF_RANDOM_ENGINE_PHILOX) ; Resolves to Philox

;; AF canny threshold type constants (af/defines.h - enum af_canny_threshold)
(def AF_CANNY_THRESHOLD_MANUAL 0)
(def AF_CANNY_THRESHOLD_AUTO_OTSU 1)

;; AF sparse matrix storage type constants (af/defines.h - enum af_storage)
(def AF_STORAGE_DENSE 0) ; Storage type is dense
(def AF_STORAGE_CSR 1)   ; Storage type is CSR
(def AF_STORAGE_CSC 2)   ; Storage type is CSC
(def AF_STORAGE_COO 3)   ; Storage type is COO

;; AF flux function type constants (af/defines.h - enum af_flux_function)
(def AF_FLUX_QUADRATIC 1)   ; Quadratic flux function
(def AF_FLUX_EXPONENTIAL 2) ; Exponential flux function
(def AF_FLUX_DEFAULT 0)     ; Default flux function is exponential

;; AF diffusion equation type constants (af/defines.h - enum af_diffusion_eq)
(def AF_DIFFUSION_GRAD 1)      ; Gradient diffusion equation
(def AF_DIFFUSION_MCDE 2)      ; Modified curvature diffusion equation
(def AF_DIFFUSION_DEFAULT 0)   ; Default option is same as AF_DIFFUSION_GRAD

;; AF top-k function type constants (af/defines.h - enum af_topk_function)
(def AF_TOPK_MIN 1)         ; Top k min values
(def AF_TOPK_MAX 2)         ; Top k max values
(def AF_TOPK_STABLE 4)      ; Preserve order of indices for equal values
(def AF_TOPK_STABLE_MIN (bit-or AF_TOPK_STABLE AF_TOPK_MIN)) ; Top k min with stable indices
(def AF_TOPK_STABLE_MAX (bit-or AF_TOPK_STABLE AF_TOPK_MAX)) ; Top k max with stable indices
(def AF_TOPK_DEFAULT 0)     ; Default option (max)

;; AF variance bias type constants (af/defines.h - enum af_var_bias)
(def AF_VARIANCE_DEFAULT 0)    ; Default (Population) variance
(def AF_VARIANCE_SAMPLE 1)     ; Sample variance
(def AF_VARIANCE_POPULATION 2) ; Population variance

;; AF iterative deconvolution algorithm type constants (af/defines.h - enum af_iterative_deconv_algo)
(def AF_ITERATIVE_DECONV_LANDWEBER 1)       ; Landweber Deconvolution
(def AF_ITERATIVE_DECONV_RICHARDSONLUCY 2)  ; Richardson-Lucy Deconvolution
(def AF_ITERATIVE_DECONV_DEFAULT 0)         ; Default is Landweber deconvolution

;; AF inverse deconvolution algorithm type constants (af/defines.h - enum af_inverse_deconv_algo)
(def AF_INVERSE_DECONV_TIKHONOV 1) ; Tikhonov inverse deconvolution
(def AF_INVERSE_DECONV_DEFAULT 0)  ; Default is Tikhonov deconvolution

;; AF convolution gradient type constants (af/defines.h - enum af_conv_gradient_type)
(def AF_CONV_GRADIENT_DEFAULT 0) ; Default option computes gradient with respect to both filter and data
(def AF_CONV_GRADIENT_FILTER 1)  ; Compute gradient with respect to filter only
(def AF_CONV_GRADIENT_DATA 2)    ; Compute gradient with respect to data only
(def AF_CONV_GRADIENT_BIAS 3)    ; Compute gradient with respect to bias only (currently only supported for 2D convolution with AF

;;;
;;; FORGE / Graphics Related Enums
;;; 

;; AF colormap type constants (af/defines.h - enum af_colormap)
(def AF_COLORMAP_DEFAULT 0)    ; Default grayscale map
(def AF_COLORMAP_SPECTRUM 1)   ; Spectrum map (390nm-830nm, in sRGB colorspace)
(def AF_COLORMAP_COLORS 2)    ; Colors, aka. Rainbow
(def AF_COLORMAP_RED 3)       ; Red hue map
(def AF_COLORMAP_MOOD 4)      ; Mood map
(def AF_COLORMAP_HEAT 5)      ; Heat map
(def AF_COLORMAP_BLUE 6)      ; Blue hue map
(def AF_COLORMAP_INFERNO 7)   ; Perceptually uniform shades of black-red-yellow
(def AF_COLORMAP_MAGMA 8)     ; Perceptually uniform shades of black-red-white
(def AF_COLORMAP_PLASMA 9)    ; Perceptually uniform shades of blue-red-yellow
(def AF_COLORMAP_VIRIDIS 10)   ; Perceptually uniform shades of blue-green-yellow 

;; AF marker type constants (af/defines.h - enum af_marker_type)
(def AF_MARKER_NONE 0)
(def AF_MARKER_POINT 1)
(def AF_MARKER_CIRCLE 2)
(def AF_MARKER_SQUARE 3)
(def AF_MARKER_TRIANGLE 4)
(def AF_MARKER_CROSS 5)
(def AF_MARKER_PLUS 6)
(def AF_MARKER_STAR 7)

