(ns org.soulspace.arrayfire.api.device
  "Idiomatic Clojure API for querying ArrayFire devices and contexts.
   
   This namespace includes functions for
   - listing available devices
   - querying device properties such as name, compute capability, memory size, etc.
   - querying memory usage and limits 
   
   Setting the active device and backend is handled by the with-arrayfire macro
   in org.soulspace.arrayfire.api.core, which ensures that all operations
   within its scope use the specified device and backend.
   
   If you need to manage devices and contexts more directly, you can use the
   functions of the org.soulspace.arrayfire.integration.unified-api.device
   namespace, which provide lower-level access to device management and context
   handling. However, for most use cases, the with-arrayfire macro should be
   sufficient to ensure that your code runs on the desired device without
   needing to manually manage contexts."
  (:require [org.soulspace.arrayfire.integration.base.definitions :as defs]
            [org.soulspace.arrayfire.integration.unified-api.device :as device]
            [org.soulspace.arrayfire.integration.unified-api.memory :as memory]
            [org.soulspace.arrayfire.api.core :as af]))

