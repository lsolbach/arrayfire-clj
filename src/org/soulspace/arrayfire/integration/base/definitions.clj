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
  (if (keyword? dtype)
    (or (dtype-kw->const dtype)
        (throw (ex-info (str "Unknown dtype keyword: " dtype
                             ". Valid keys: " (keys dtype-kw->const))
                        {:dtype dtype})))
    (int dtype)))

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

(defn resolve-return
  "Resolve an ArrayFire return code integer to a keyword.
   
   Parameters:
   - code: integer return code from an ArrayFire function call
   
   Returns:
   Keyword representing the return status (e.g. :success, :err-no-mem)."
  [code]
  (get return-const->return-kw (int code) :err-unknown))

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

(defn resolve-random-engine
  "Resolve a random engine keyword or integer to an ArrayFire random engine constant.
   
   Parameters:
   - engine: keyword (:philox, :threefry, :mersenne, :default) or integer constant
   
   Returns:
   ArrayFire random engine constant (integer)."
  [engine]
  (if (keyword? engine)
    (or (get random-engine-kw->const engine)
        (throw (ex-info (str "Unknown random engine: " engine)
                        {:engine engine
                         :valid-engines (keys random-engine-kw->const)})))
    (int engine)))

(def source-kw->const
  "Mapping of source keywords to ArrayFire source constants."
  {:device defs/AF_SOURCE_DEVICE
   :host   defs/AF_SOURCE_HOST})

(def source-const->kw
  "Mapping of ArrayFire source constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) source-kw->const)))

(defn resolve-source
  "Resolve a source keyword or integer to an ArrayFire source constant.
   
   Parameters:
   - source: keyword (:device, :host) or integer constant
   
   Returns:
   ArrayFire source constant (integer)."
  [source]
  (if (keyword? source)
    (or (get source-kw->const source)
        (throw (ex-info (str "Unknown source: " source)
                        {:source source
                         :valid-sources (keys source-kw->const)})))
    (int source)))

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

(defn resolve-interp
  "Resolve an interpolation method keyword or integer to an ArrayFire interpolation constant."
  [method]
  (if (keyword? method)
    (or (get interp-kw->const method)
        (throw (ex-info (str "Unknown interpolation method keyword: " method
                             ". Valid keys: " (keys interp-kw->const))
                        {:method method})))
    (int method)))

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

(defn resolve-edge-pad
  "Resolve an edge padding keyword or integer to an ArrayFire border constant."
  [edge-pad]
  (if (keyword? edge-pad)
    (or (border-kw->const edge-pad)
        (throw (ex-info (str "Unknown edge padding keyword: " edge-pad
                             ". Valid keys: " (keys border-kw->const))
                        {:edge-pad edge-pad})))
    (int edge-pad)))

(def resolve-border
  "Resolve a border type keyword or integer to an ArrayFire border constant.
   This is an alias for resolve-edge-pad since border and edge padding types are the same."
  resolve-edge-pad)

(def connectivity-kw->const
  "Mapping of connectivity keywords to ArrayFire constants."
  {:4 defs/AF_CONNECTIVITY_4
   :8 defs/AF_CONNECTIVITY_8})

(def connectivity-const->kw
  "Mapping of ArrayFire connectivity constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) connectivity-kw->const)))

(defn resolve-connectivity
  "Resolve a connectivity keyword or integer to an ArrayFire connectivity constant."
  [connectivity]
  (if (keyword? connectivity)
    (or (connectivity-kw->const connectivity)
        (throw (ex-info (str "Unknown connectivity keyword: " connectivity
                             ". Valid keys: " (keys connectivity-kw->const))
                        {:connectivity connectivity})))
    (int connectivity)))

(def conv-mode-kw->const
  "Mapping of convolution mode keywords to ArrayFire constants."
  {:default defs/AF_CONV_DEFAULT
   :expand  defs/AF_CONV_EXPAND})

(def conv-mode-const->kw
  "Mapping of ArrayFire convolution mode constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) conv-mode-kw->const)))

(defn resolve-conv-mode
  "Resolve a convolution mode keyword or integer to an ArrayFire convolution mode constant."
  [mode]
  (if (keyword? mode)
    (or (conv-mode-kw->const mode)
        (throw (ex-info (str "Unknown convolution mode keyword: " mode
                             ". Valid keys: " (keys conv-mode-kw->const))
                        {:mode mode})))
    (int mode)))

(def conv-domain-kw->const
  "Mapping of convolution domain keywords to ArrayFire constants."
  {:auto    defs/AF_CONV_AUTO
   :spatial defs/AF_CONV_SPATIAL
   :freq    defs/AF_CONV_FREQ})

(def conv-domain-const->kw
  "Mapping of ArrayFire convolution domain constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) conv-domain-kw->const)))

(defn resolve-conv-domain
  "Resolve a convolution domain keyword or integer to an ArrayFire convolution domain constant."
  [domain]
  (if (keyword? domain)
    (or (conv-domain-kw->const domain)
        (throw (ex-info (str "Unknown convolution domain keyword: " domain
                             ". Valid keys: " (keys conv-domain-kw->const))
                        {:domain domain})))
    (int domain)))

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

(defn resolve-match-type
  "Resolve a match type keyword or integer to an ArrayFire match type constant."
  [match-type]
  (if (keyword? match-type)
    (or (match-type-kw->const match-type)
        (throw (ex-info (str "Unknown match type keyword: " match-type
                             ". Valid keys: " (keys match-type-kw->const))
                        {:match-type match-type})))
    (int match-type)))

(def ycc-std-kw->const
  "Mapping of YCC standard keywords to ArrayFire constants."
  {:bt601  defs/AF_YCC_601
   :bt709  defs/AF_YCC_709
   :bt2020 defs/AF_YCC_2020})

(def ycc-std-const->kw
  "Mapping of ArrayFire YCC standard constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) ycc-std-kw->const)))

(defn resolve-ycc-std
  "Resolve a YCC standard keyword or integer to an ArrayFire YCC standard constant."
  [ycc-std]
  (if (keyword? ycc-std)
    (or (ycc-std-kw->const ycc-std)
        (throw (ex-info (str "Unknown YCC standard keyword: " ycc-std
                             ". Valid keys: " (keys ycc-std-kw->const))
                        {:ycc-std ycc-std})))
    (int ycc-std)))

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

(defn resolve-colorspace
  "Resolve a color space keyword or integer to an ArrayFire color space constant."
  [colorspace]
  (if (keyword? colorspace)
    (or (colorspace-kw->const colorspace)
        (throw (ex-info (str "Unknown color space keyword: " colorspace
                             ". Valid keys: " (keys colorspace-kw->const))
                        {:colorspace colorspace})))
    (int colorspace)))

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

(defn resolve-mat-prop
  "Resolve a matrix property keyword or integer to an ArrayFire matrix property constant.
   This function can handle combined properties using bit flags."
  [prop]
  (if (keyword? prop)
    (or (mat-prop-kw->const prop)
        (throw (ex-info (str "Unknown matrix property keyword: " prop
                             ". Valid keys: " (keys mat-prop-kw->const))
                        {:mat-prop prop})))
    (int prop)))

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

(defn resolve-norm-type
  "Resolve a norm type keyword or integer to an ArrayFire norm type constant.
   Falls back to 2 (AF_NORM_VECTOR_2 / L2) for unknown keywords."
  [k]
  (if (keyword? k)
    (get norm-type-kw->const k 2)
    (int k)))

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

(defn resolve-image-format
  "Resolve an image format keyword or integer to an ArrayFire image format constant."
  [format]
  (if (keyword? format)
    (or (image-format-kw->const format)
        (throw (ex-info (str "Unknown image format keyword: " format
                             ". Valid keys: " (keys image-format-kw->const))
                        {:format format})))
    (int format)))

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

(defn resolve-moment-type
  "Resolve a moment type keyword or integer to an ArrayFire moment type constant.
   This function can handle combined moment types using bit flags."
  [moment-type]
  (if (keyword? moment-type)
    (or (moment-type-kw->const moment-type)
        (throw (ex-info (str "Unknown moment type keyword: " moment-type
                             ". Valid keys: " (keys moment-type-kw->const))
                        {:moment-type moment-type})))
    (int moment-type)))

(def homography-type-kw->const
  "Mapping of homography type keywords to ArrayFire constants."
  {:ransac defs/AF_HOMOGRAPHY_RANSAC
   :lmeds  defs/AF_HOMOGRAPHY_LMEDS})

(def homography-type-const->kw
  "Mapping of ArrayFire homography type constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) homography-type-kw->const)))

(defn resolve-homography-type
  "Resolve a homography type keyword or integer to an ArrayFire homography type constant."
  [homography-type]
  (if (keyword? homography-type)
    (or (homography-type-kw->const homography-type)
        (throw (ex-info (str "Unknown homography type keyword: " homography-type
                             ". Valid keys: " (keys homography-type-kw->const))
                        {:homography-type homography-type})))
    (int homography-type)))

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

(defn resolve-binary-op
  "Resolve a binary operator keyword or integer to an ArrayFire binary operator constant."
  [op]
  (if (keyword? op)
    (or (binary-op-kw->const op)
        (throw (ex-info (str "Unknown binary operator keyword: " op
                             ". Valid keys: " (keys binary-op-kw->const))
                        {:binary-op op})))
    (int op)))

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

(defn resolve-storage
  "Resolve a sparse storage type keyword or integer to an ArrayFire storage constant."
  [storage]
  (if (keyword? storage)
    (or (storage-kw->const storage)
        (throw (ex-info (str "Unknown storage type keyword: " storage
                             ". Valid keys: " (keys storage-kw->const))
                        {:storage storage})))
    (int storage)))

(def flux-fn-kw->const
  "Mapping of flux function keywords to ArrayFire constants."
  {:quadratic   defs/AF_FLUX_QUADRATIC
   :exponential defs/AF_FLUX_EXPONENTIAL
   :default     defs/AF_FLUX_DEFAULT})

(def flux-fn-const->kw
  "Mapping of ArrayFire flux function constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) flux-fn-kw->const)))

(defn resolve-flux-fn
  "Resolve a flux function keyword or integer to an ArrayFire flux function constant."
  [flux-fn]
  (if (keyword? flux-fn)
    (or (flux-fn-kw->const flux-fn)
        (throw (ex-info (str "Unknown flux function keyword: " flux-fn
                             ". Valid keys: " (keys flux-fn-kw->const))
                        {:flux-fn flux-fn})))
    (int flux-fn)))

(def diffusion-eq-kw->const
  "Mapping of diffusion equation keywords to ArrayFire constants."
  {:grad    defs/AF_DIFFUSION_GRAD
   :mcde    defs/AF_DIFFUSION_MCDE
   :default defs/AF_DIFFUSION_DEFAULT})

(def diffusion-eq-const->kw
  "Mapping of ArrayFire diffusion equation constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) diffusion-eq-kw->const)))

(defn resolve-diffusion-eq
  "Resolve a diffusion equation keyword or integer to an ArrayFire diffusion equation constant."
  [diffusion-eq]
  (if (keyword? diffusion-eq)
    (or (diffusion-eq-kw->const diffusion-eq)
        (throw (ex-info (str "Unknown diffusion equation keyword: " diffusion-eq
                             ". Valid keys: " (keys diffusion-eq-kw->const))
                        {:diffusion-eq diffusion-eq})))
    (int diffusion-eq)))

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

(defn resolve-topk-fn
  "Resolve a top-k function keyword or integer to an ArrayFire top-k function constant.
   This function can handle combined stable min/max options using bit flags."
  [topk-fn]
  (if (keyword? topk-fn)
    (or (topk-fn-kw->const topk-fn)
        (throw (ex-info (str "Unknown top-k function keyword: " topk-fn
                             ". Valid keys: " (keys topk-fn-kw->const))
                        {:topk-fn topk-fn})))
    (int topk-fn)))

(def variance-bias-kw->const
  "Mapping of variance bias keywords to ArrayFire constants."
  {:default    defs/AF_VARIANCE_DEFAULT
   :sample     defs/AF_VARIANCE_SAMPLE
   :population defs/AF_VARIANCE_POPULATION})

(def variance-bias-const->kw
  "Mapping of ArrayFire variance bias constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) variance-bias-kw->const)))

(defn resolve-variance-bias
  "Resolve a variance bias keyword or integer to an ArrayFire variance bias constant."
  [bias]
  (if (keyword? bias)
    (or (variance-bias-kw->const bias)
        (throw (ex-info (str "Unknown variance bias keyword: " bias
                             ". Valid keys: " (keys variance-bias-kw->const))
                        {:variance-bias bias})))
    (int bias)))

(def iterative-deconv-algo-kw->const
  "Mapping of iterative deconvolution algorithm keywords to ArrayFire constants."
  {:landweber        defs/AF_ITERATIVE_DECONV_LANDWEBER
   :richardson-lucy  defs/AF_ITERATIVE_DECONV_RICHARDSONLUCY
   :default          defs/AF_ITERATIVE_DECONV_DEFAULT})

(def iterative-deconv-algo-const->kw
  "Mapping of ArrayFire iterative deconvolution algorithm constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) iterative-deconv-algo-kw->const)))

(defn resolve-iterative-deconv-algo
  "Resolve an iterative deconvolution algorithm keyword or integer to an ArrayFire constant."
  [algo]
  (if (keyword? algo)
    (or (iterative-deconv-algo-kw->const algo)
        (throw (ex-info (str "Unknown iterative deconvolution algorithm keyword: " algo
                             ". Valid keys: " (keys iterative-deconv-algo-kw->const))
                        {:iterative-deconv-algo algo})))
    (int algo)))

(def inverse-deconv-algo-kw->const
  "Mapping of inverse deconvolution algorithm keywords to ArrayFire constants."
  {:tikhonov defs/AF_INVERSE_DECONV_TIKHONOV
   :default  defs/AF_INVERSE_DECONV_DEFAULT})

(def inverse-deconv-algo-const->kw
  "Mapping of ArrayFire inverse deconvolution algorithm constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) inverse-deconv-algo-kw->const)))

(defn resolve-inverse-deconv-algo
  "Resolve an inverse deconvolution algorithm keyword or integer to an ArrayFire constant."
  [algo]
  (if (keyword? algo)
    (or (inverse-deconv-algo-kw->const algo)
        (throw (ex-info (str "Unknown inverse deconvolution algorithm keyword: " algo
                             ". Valid keys: " (keys inverse-deconv-algo-kw->const))
                        {:inverse-deconv-algo algo})))
    (int algo)))

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

(defn resolve-conv-gradient
  "Resolve a convolution gradient type keyword or integer to an ArrayFire constant."
  [conv-gradient]
  (if (keyword? conv-gradient)
    (or (conv-gradient-kw->const conv-gradient)
        (throw (ex-info (str "Unknown convolution gradient type keyword: " conv-gradient
                             ". Valid keys: " (keys conv-gradient-kw->const))
                        {:conv-gradient conv-gradient})))
    (int conv-gradient)))

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

(defn resolve-colormap
  "Resolve a colormap keyword or integer to an ArrayFire colormap constant."
  [colormap]
  (if (keyword? colormap)
    (or (colormap-kw->const colormap)
        (throw (ex-info (str "Unknown colormap keyword: " colormap
                             ". Valid keys: " (keys colormap-kw->const))
                        {:colormap colormap})))
    (int colormap)))

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

(defn resolve-marker-type
  "Resolve a marker type keyword or integer to an ArrayFire marker type constant."
  [marker-type]
  (if (keyword? marker-type)
    (or (marker-type-kw->const marker-type)
        (throw (ex-info (str "Unknown marker type keyword: " marker-type
                             ". Valid keys: " (keys marker-type-kw->const))
                        {:marker-type marker-type})))
    (int marker-type)))

(def canny-threshold-kw->const
  "Mapping of canny threshold type keywords to ArrayFire constants."
  {:manual    defs/AF_CANNY_THRESHOLD_MANUAL
   :auto-otsu defs/AF_CANNY_THRESHOLD_AUTO_OTSU})

(def canny-threshold-const->kw
  "Mapping of ArrayFire canny threshold type constants to keywords."
  (into {}
        (map (fn [[k v]] [v k]) canny-threshold-kw->const)))

(defn resolve-canny-threshold
  "Resolve a canny threshold type keyword or integer to an ArrayFire constant."
  [threshold-type]
  (if (keyword? threshold-type)
    (or (canny-threshold-kw->const threshold-type)
        (throw (ex-info (str "Unknown canny threshold type keyword: " threshold-type
                             ". Valid keys: " (keys canny-threshold-kw->const))
                        {:canny-threshold-type threshold-type})))
    (int threshold-type)))
