(ns org.soulspace.arrayfire.integration.base.definitions
    "Core definitions and constants for ArrayFire integration.
     
     This namespace defines mappings between Clojure keywords and ArrayFire
     constants e.g. for data types, backends, error codes, and random engines.
     These definitions are used across the API and integration layers to ensure
     consistent handling of ArrayFire concepts in a Clojure-friendly way."
  (:require [org.soulspace.arrayfire.ffi.base.definitions :as defs]))
  
;;;
;;; ArrayFire constants to keyword mappings 
;;;
(def dtype-kw->const
  "Mapping of Clojure keywords to ArrayFire dtype constants."
  {:f32 defs/AF_DTYPE_F32 ; float
   :c32 defs/AF_DTYPE_C32 ; complex float
   :f64 defs/AF_DTYPE_F64 ; double
   :c64 defs/AF_DTYPE_C64 ; complex double
   :b8  defs/AF_DTYPE_B8  ; bool
   :s32 defs/AF_DTYPE_S32 ; int
   :u32 defs/AF_DTYPE_U32 ; unsigned int
   :u8  defs/AF_DTYPE_U8  ; unsigned char
   :s64 defs/AF_DTYPE_S64 ; long
   :u64 defs/AF_DTYPE_U64 ; unsigned long
   :s16 defs/AF_DTYPE_S16 ; short
   :u16 defs/AF_DTYPE_U16 ; unsigned short
   })

(def dtype-const->kw
  "Mapping of ArrayFire dtype constants to Clojure keywords."
  (into {}
        (map (fn [[k v]] [v k]) dtype-kw->const)))

(defn resolve-dtype
  "Resolve a dtype keyword or integer to an ArrayFire dtype constant.
   
   Parameters:
   - dtype: keyword (e.g. :f32, :f64, :s32) or integer constant
   
   Returns:
   ArrayFire dtype constant (integer)."
  [dtype]
  (or (dtype-kw->const dtype)
      (throw (ex-info "Unsupported dtype" {:dtype dtype}))))

(def dtype-kw->size
  "Mapping of Clojure keywords to sizes in bytes for each ArrayFire dtype."
  {:f32 4  ; float
   :c32 8  ; complex float (2 floats)
   :f64 8  ; double
   :c64 16 ; complex double (2 doubles)
   :b8  1  ; bool
   :s32 4  ; int
   :u32 4  ; unsigned int
   :u8  1  ; unsigned char
   :s64 8  ; long
   :u64 8  ; unsigned long
   :s16 2  ; short
   :u16 2  ; unsigned short
   })

(def return-kw->const
  "Mapping of error keywords to ArrayFire error codes."
  {:success                    defs/AF_SUCCESS
   :err-no-mem                 defs/AF_ERR_NO_MEM
   :err-driver                 defs/AF_ERR_DRIVER
   :err-runtime                defs/AF_ERR_RUNTIME
   :err-invalid-array          defs/AF_ERR_INVALID_ARRAY
   :err-arg                    defs/AF_ERR_ARG
   :err-size                   defs/AF_ERR_SIZE
   :err-type                   defs/AF_ERR_TYPE
   :err-diff-type              defs/AF_ERR_DIFF_TYPE
   :err-batch                  defs/AF_ERR_BATCH
   :err-device                 defs/AF_ERR_DEVICE
   :err-not-supported          defs/AF_ERR_NOT_SUPPORTED
   :err-not-configured         defs/AF_ERR_NOT_CONFIGURED
   :err-non-free               defs/AF_ERR_NONFREE
   :err-no-double              defs/AF_ERR_NO_DBL
   :err-no-gfx                 defs/AF_ERR_NO_GFX
   :err-no-half                defs/AF_ERR_NO_HALF
   :err-load-lib               defs/AF_ERR_LOAD_LIB
   :err-load-sym               defs/AF_ERR_LOAD_SYM
   :err-array-backend-mismatch defs/AF_ERR_ARR_BKND_MISMATCH
   :err-internal               defs/AF_ERR_INTERNAL
   :err-unknown                defs/AF_ERR_UNKNOWN
   ;
   })

(def return-const->return-kw
  "Mapping of ArrayFire return codes to error keywords."
  (into {}
        (map (fn [[k v]] [v k]) return-kw->const)))

(def backend-kw->const
  "Mapping of backend keywords to ArrayFire backend constants."
  {:default defs/AF_BACKEND_DEFAULT
   :cpu     defs/AF_BACKEND_CPU
   :cuda    defs/AF_BACKEND_CUDA
   :opencl  defs/AF_BACKEND_OPENCL
   :oneapi  defs/AF_BACKEND_ONEAPI})

(def backend-const->kw
  "Mapping of ArrayFire backend constants to backend keywords."
  (into {}
        (map (fn [[k v]] [v k]) backend-kw->const)))

(defn resolve-backend
  "Resolve a backend keyword or integer to an ArrayFire backend constant.
   
   Parameters:
   - backend: keyword (:cpu, :cuda, :opencl, :oneapi, :default) or integer constant
   
   Returns:
   ArrayFire backend constant (integer)."
  [backend]
  (if (keyword? backend)
    (or (get backend-kw->const backend)
        (throw (ex-info (str "Unknown backend: " backend)
                        {:backend backend
                         :valid-backends (keys backend-kw->const)})))
    (int backend)))

(def random-engine-kw->const
  "Mapping of random engine keywords to ArrayFire constants."
  {:default defs/AF_RANDOM_ENGINE_DEFAULT
   :philox  defs/AF_RANDOM_ENGINE_PHILOX
   :threefry defs/AF_RANDOM_ENGINE_THREEFRY
   :mersenne defs/AF_RANDOM_ENGINE_MERSENNE})

(def random-engine-const->kw
  "Mapping of ArrayFire random engine constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) random-engine-kw->const)))


(def source-kw->const
  "Mapping of source keywords to ArrayFire source constants."
  {:device defs/AF_SOURCE_DEVICE
   :host   defs/AF_SOURCE_HOST})

(def source-const->kw
  "Mapping of ArrayFire source constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) source-kw->const)))

(def interp-kw->const
  "Mapping of interpolation type keywords to ArrayFire constants."
  {:nearest        defs/AF_INTERP_NEAREST
   :linear         defs/AF_INTERP_LINEAR
   :bilinear       defs/AF_INTERP_BILINEAR
   :cubic          defs/AF_INTERP_CUBIC
   :lower          defs/AF_INTERP_LOWER
   :linear-cosine  defs/AF_INTERP_LINEAR_COSINE
   :bilinear-cosine defs/AF_INTERP_BILINEAR_COSINE
   :bicubic        defs/AF_INTERP_BICUBIC
   :cubic-spline   defs/AF_INTERP_CUBIC_SPLINE
   :bicubic-spline defs/AF_INTERP_BICUBIC_SPLINE})

(def interp-const->kw
  "Mapping of ArrayFire interpolation type constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) interp-kw->const)))

(def border-kw->const
  "Mapping of border/padding type keywords to ArrayFire constants."
  {:zero          defs/AF_PAD_ZERO
   :sym           defs/AF_PAD_SYM
   :clamp-to-edge defs/AF_PAD_CLAMP_TO_EDGE
   :periodic      defs/AF_PAD_PERIODIC})

(def border-const->kw
  "Mapping of ArrayFire border/padding type constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) border-kw->const)))

(def connectivity-kw->const
  "Mapping of connectivity keywords to ArrayFire constants."
  {:4 defs/AF_CONNECTIVITY_4
   :8 defs/AF_CONNECTIVITY_8})

(def connectivity-const->kw
  "Mapping of ArrayFire connectivity constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) connectivity-kw->const)))

(def conv-mode-kw->const
  "Mapping of convolution mode keywords to ArrayFire constants."
  {:default defs/AF_CONV_DEFAULT
   :expand  defs/AF_CONV_EXPAND})

(def conv-mode-const->kw
  "Mapping of ArrayFire convolution mode constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) conv-mode-kw->const)))

(def conv-domain-kw->const
  "Mapping of convolution domain keywords to ArrayFire constants."
  {:auto    defs/AF_CONV_AUTO
   :spatial defs/AF_CONV_SPATIAL
   :freq    defs/AF_CONV_FREQ})

(def conv-domain-const->kw
  "Mapping of ArrayFire convolution domain constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) conv-domain-kw->const)))

(def match-type-kw->const
  "Mapping of match type keywords to ArrayFire constants."
  {:sad  defs/AF_SAD
   :zsad defs/AF_ZSAD
   :lsad defs/AF_LSAD
   :ssd  defs/AF_SSD
   :zssd defs/AF_ZSSD
   :lssd defs/AF_LSSD
   :ncc  defs/AF_NCC
   :zncc defs/AF_ZNCC
   :shd  defs/AF_SHD})

(def match-type-const->kw
  "Mapping of ArrayFire match type constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) match-type-kw->const)))

(def ycc-std-kw->const
  "Mapping of YCC standard keywords to ArrayFire constants."
  {:bt601  defs/AF_YCC_601
   :bt709  defs/AF_YCC_709
   :bt2020 defs/AF_YCC_2020})

(def ycc-std-const->kw
  "Mapping of ArrayFire YCC standard constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) ycc-std-kw->const)))

(def colorspace-kw->const
  "Mapping of color space keywords to ArrayFire constants."
  {:gray  defs/AF_GRAY
   :rgb   defs/AF_RGB
   :hsv   defs/AF_HSV
   :ycbcr defs/AF_YCbCr})

(def colorspace-const->kw
  "Mapping of ArrayFire color space constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) colorspace-kw->const)))

(def mat-prop-kw->const
  "Mapping of matrix property keywords to ArrayFire constants.
   These values are bit flags and may be combined with bit-or."
  {:none       defs/AF_MAT_NONE
   :trans      defs/AF_MAT_TRANS
   :ctrans     defs/AF_MAT_CTRANS
   :conj       defs/AF_MAT_CONJ
   :upper      defs/AF_MAT_UPPER
   :lower      defs/AF_MAT_LOWER
   :diag-unit  defs/AF_MAT_DIAG_UNIT
   :sym        defs/AF_MAT_SYM
   :posdef     defs/AF_MAT_POSDEF
   :orthog     defs/AF_MAT_ORTHOG
   :tri-diag   defs/AF_MAT_TRI_DIAG
   :block-diag defs/AF_MAT_BLOCK_DIAG})

(def mat-prop-const->kw
  "Mapping of ArrayFire matrix property constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) mat-prop-kw->const)))

(def norm-type-kw->const
  "Mapping of norm type keywords to ArrayFire constants."
  {:vector-1   defs/AF_NORM_VECTOR_1
   :vector-inf defs/AF_NORM_VECTOR_INF
   :vector-2   defs/AF_NORM_VECTOR_2
   :vector-p   defs/AF_NORM_VECTOR_P
   :matrix-1   defs/AF_NORM_MATRIX_1
   :matrix-inf defs/AF_NORM_MATRIX_INF
   :matrix-2   defs/AF_NORM_MATRIX_2
   :matrix-lpq defs/AF_NORM_MATRIX_L_PQ
   :euclid     defs/AF_NORM_EUCLID})

(def norm-type-const->kw
  "Mapping of ArrayFire norm type constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) norm-type-kw->const)))

(def image-format-kw->const
  "Mapping of image format keywords to ArrayFire constants."
  {:bmp    defs/AF_FIF_BMP
   :ico    defs/AF_FIF_ICO
   :jpeg   defs/AF_FIF_JPEG
   :jng    defs/AF_FIF_JNG
   :png    defs/AF_FIF_PNG
   :ppm    defs/AF_FIF_PPM
   :ppmraw defs/AF_FIF_PPMRAW
   :tiff   defs/AF_FIF_TIFF
   :psd    defs/AF_FIF_PSD
   :hdr    defs/AF_FIF_HDR
   :exr    defs/AF_FIF_EXR
   :jp2    defs/AF_FIF_JP2
   :raw    defs/AF_FIF_RAW})

(def image-format-const->kw
  "Mapping of ArrayFire image format constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) image-format-kw->const)))

(def moment-type-kw->const
  "Mapping of moment type keywords to ArrayFire constants.
   :first-order is a composite of :m00 :m01 :m10 :m11 combined with bit-or."
  {:m00          defs/AF_MOMENT_M00
   :m01          defs/AF_MOMENT_M01
   :m10          defs/AF_MOMENT_M10
   :m11          defs/AF_MOMENT_M11
   :first-order  defs/AF_MOMENT_FIRST_ORDER})

(def moment-type-const->kw
  "Mapping of ArrayFire moment type constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) moment-type-kw->const)))

(def homography-type-kw->const
  "Mapping of homography type keywords to ArrayFire constants."
  {:ransac defs/AF_HOMOGRAPHY_RANSAC
   :lmeds  defs/AF_HOMOGRAPHY_LMEDS})

(def homography-type-const->kw
  "Mapping of ArrayFire homography type constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) homography-type-kw->const)))

(def binary-op-kw->const
  "Mapping of binary operator keywords to ArrayFire constants."
  {:add defs/AF_BINARY_ADD
   :mul defs/AF_BINARY_MUL
   :min defs/AF_BINARY_MIN
   :max defs/AF_BINARY_MAX})

(def binary-op-const->kw
  "Mapping of ArrayFire binary operator constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) binary-op-kw->const)))

(def storage-kw->const
  "Mapping of sparse storage type keywords to ArrayFire constants."
  {:dense defs/AF_STORAGE_DENSE
   :csr   defs/AF_STORAGE_CSR
   :csc   defs/AF_STORAGE_CSC
   :coo   defs/AF_STORAGE_COO})

(def storage-const->kw
  "Mapping of ArrayFire sparse storage type constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) storage-kw->const)))

(def flux-fn-kw->const
  "Mapping of flux function keywords to ArrayFire constants."
  {:quadratic   defs/AF_FLUX_QUADRATIC
   :exponential defs/AF_FLUX_EXPONENTIAL
   :default     defs/AF_FLUX_DEFAULT})

(def flux-fn-const->kw
  "Mapping of ArrayFire flux function constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) flux-fn-kw->const)))

(def diffusion-eq-kw->const
  "Mapping of diffusion equation keywords to ArrayFire constants."
  {:grad    defs/AF_DIFFUSION_GRAD
   :mcde    defs/AF_DIFFUSION_MCDE
   :default defs/AF_DIFFUSION_DEFAULT})

(def diffusion-eq-const->kw
  "Mapping of ArrayFire diffusion equation constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) diffusion-eq-kw->const)))

(def topk-fn-kw->const
  "Mapping of top-k function keywords to ArrayFire constants.
   :stable-min and :stable-max are composed bit flags."
  {:min        defs/AF_TOPK_MIN
   :max        defs/AF_TOPK_MAX
   :stable     defs/AF_TOPK_STABLE
   :stable-min defs/AF_TOPK_STABLE_MIN
   :stable-max defs/AF_TOPK_STABLE_MAX
   :default    defs/AF_TOPK_DEFAULT})

(def topk-fn-const->kw
  "Mapping of ArrayFire top-k function constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) topk-fn-kw->const)))

(def variance-bias-kw->const
  "Mapping of variance bias keywords to ArrayFire constants."
  {:default    defs/AF_VARIANCE_DEFAULT
   :sample     defs/AF_VARIANCE_SAMPLE
   :population defs/AF_VARIANCE_POPULATION})

(def variance-bias-const->kw
  "Mapping of ArrayFire variance bias constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) variance-bias-kw->const)))

(def iterative-deconv-algo-kw->const
  "Mapping of iterative deconvolution algorithm keywords to ArrayFire constants."
  {:landweber        defs/AF_ITERATIVE_DECONV_LANDWEBER
   :richardson-lucy  defs/AF_ITERATIVE_DECONV_RICHARDSONLUCY
   :default          defs/AF_ITERATIVE_DECONV_DEFAULT})

(def iterative-deconv-algo-const->kw
  "Mapping of ArrayFire iterative deconvolution algorithm constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) iterative-deconv-algo-kw->const)))

(def inverse-deconv-algo-kw->const
  "Mapping of inverse deconvolution algorithm keywords to ArrayFire constants."
  {:tikhonov defs/AF_INVERSE_DECONV_TIKHONOV
   :default  defs/AF_INVERSE_DECONV_DEFAULT})

(def inverse-deconv-algo-const->kw
  "Mapping of ArrayFire inverse deconvolution algorithm constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) inverse-deconv-algo-kw->const)))

(def conv-gradient-kw->const
  "Mapping of convolution gradient type keywords to ArrayFire constants."
  {:default defs/AF_CONV_GRADIENT_DEFAULT
   :filter  defs/AF_CONV_GRADIENT_FILTER
   :data    defs/AF_CONV_GRADIENT_DATA
   :bias    defs/AF_CONV_GRADIENT_BIAS})

(def conv-gradient-const->kw
  "Mapping of ArrayFire convolution gradient type constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) conv-gradient-kw->const)))

(def colormap-kw->const
  "Mapping of colormap keywords to ArrayFire constants."
  {:default  defs/AF_COLORMAP_DEFAULT
   :spectrum defs/AF_COLORMAP_SPECTRUM
   :colors   defs/AF_COLORMAP_COLORS
   :red      defs/AF_COLORMAP_RED
   :mood     defs/AF_COLORMAP_MOOD
   :heat     defs/AF_COLORMAP_HEAT
   :blue     defs/AF_COLORMAP_BLUE
   :inferno  defs/AF_COLORMAP_INFERNO
   :magma    defs/AF_COLORMAP_MAGMA
   :plasma   defs/AF_COLORMAP_PLASMA
   :viridis  defs/AF_COLORMAP_VIRIDIS})

(def colormap-const->kw
  "Mapping of ArrayFire colormap constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) colormap-kw->const)))

(def marker-type-kw->const
  "Mapping of marker type keywords to ArrayFire constants."
  {:none     defs/AF_MARKER_NONE
   :point    defs/AF_MARKER_POINT
   :circle   defs/AF_MARKER_CIRCLE
   :square   defs/AF_MARKER_SQUARE
   :triangle defs/AF_MARKER_TRIANGLE
   :cross    defs/AF_MARKER_CROSS
   :plus     defs/AF_MARKER_PLUS
   :star     defs/AF_MARKER_STAR})

(def marker-type-const->kw
  "Mapping of ArrayFire marker type constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) marker-type-kw->const)))

(def canny-threshold-kw->const
  "Mapping of canny threshold type keywords to ArrayFire constants."
  {:manual    defs/AF_CANNY_THRESHOLD_MANUAL
   :auto-otsu defs/AF_CANNY_THRESHOLD_AUTO_OTSU})

(def canny-threshold-const->kw
  "Mapping of ArrayFire canny threshold type constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) canny-threshold-kw->const)))
