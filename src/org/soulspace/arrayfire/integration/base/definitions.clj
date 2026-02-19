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

