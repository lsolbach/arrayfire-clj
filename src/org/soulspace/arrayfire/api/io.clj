(ns org.soulspace.arrayfire.api.io
  "Idiomatic Clojure API for ArrayFire matrix I/O operations.
   
   This namespace provides functions for saving and loading ArrayFire arrays to
   and from disk. The API is designed to be simple and efficient, leveraging
   ArrayFire's native binary format for optimal performance.
   
   Supported file format:
   - .af: ArrayFire's native binary format, which preserves all array metadata
          and is optimized for performance."
  (:require [org.soulspace.arrayfire.integration.base.definitions :as defs]
            [org.soulspace.arrayfire.integration.unified-api.array :as array]
            [org.soulspace.arrayfire.integration.unified-api.util :as util])
  (:import (org.soulspace.arrayfire.integration.base.resource AFArray)))

;; TODO add an idiomatic Clojure API for the I/O functions in the util namespace.
;; TODO may add image I/O functions in the future, but for now we can focus on the core array I/O functionality.