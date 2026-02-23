(ns org.soulspace.arrayfire.api.computer-vision
  "Idiomatic Clojure API for ArrayFire's computer vision functions.
   
   This namespace provides high-level, user-friendly functions that wrap the lower-level ArrayFire computer vision API.
   It includes functions from the vision and features Unified API namespaces."
  (:require [org.soulspace.arrayfire.integration.base.definitions :as defs]
            [org.soulspace.arrayfire.integration.unified-api.features :as features]
            [org.soulspace.arrayfire.integration.unified-api.vision :as vision]
            [org.soulspace.arrayfire.api.image-processing :as ip]
            [org.soulspace.arrayfire.api.core :as core]))

