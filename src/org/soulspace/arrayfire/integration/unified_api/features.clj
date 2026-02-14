(ns org.soulspace.arrayfire.integration.unified-api.features
  "Integration of the ArrayFire features related FFI bindings with the error
   handling and resource management on the JVM.
   
   This namespace provides idiomatic Clojure wrappers around ArrayFire's
   feature detection infrastructure. Features are lightweight structures that
   represent detected keypoints, corners, or interest points from computer
   vision algorithms.
   
   The af_features Structure:
   
   Features contain arrays of properties for N detected points:
   - X coordinates (float array)
   - Y coordinates (float array)
   - Scores/responses (float array)
   - Orientations in radians (float array)
   - Sizes/scales (float array)
   
   All arrays have the same length N (number of features). The structure uses
   a Structure-of-Arrays (SoA) layout optimized for GPU processing.
   
   Feature Lifecycle:
   
   1. Create/Detect: Features are typically created by detectors (FAST, Harris,
      SIFT, ORB, etc.) or manually allocated with create-features
   2. Query: Extract properties (count, coordinates, scores, etc.)
   3. Use: Process features for matching, tracking, or analysis
   4. Release: Free resources when done (manual memory management)
   
   Basic Usage Pattern:
   
   ```clojure
   ;; Features from detector (e.g., FAST)
   (let [features (detect-fast image)]
     (try
       (let [n (get-features-num features)
             x (get-features-xpos features)
             y (get-features-ypos features)
             scores (get-features-score features)]
         (println \"Found\" n \"features\")
         ;; Process coordinates and scores...
         )
       (finally
         (release-features! features))))
   ```
   
   Manual Feature Construction:
   
   ```clojure
   ;; Create features manually
   (let [features (create-features 100)]
     (try
       ;; Populate with custom data
       (let [x (get-features-xpos features)
             y (get-features-ypos features)]
         ;; Write coordinate data to x and y arrays
         )
       (finally
         (release-features! features))))
   ```
   
   Feature Filtering Example:
   
   ```clojure
   ;; Filter features by score threshold
   (defn filter-strong-features [features threshold]
     (let [scores (get-features-score features)
           mask (arith/ge scores (constant threshold [n] AF_DTYPE_F32))
           n-filtered (algorithm/count mask)]
       ;; Create filtered feature set
       (create-filtered-features features mask n-filtered)))
   ```
   
   Notes:
   
   - Features use reference counting (retain/release semantics)
   - Property arrays are non-owning references (don't release separately)
   - Coordinate system: origin at top-left (0,0), sub-pixel precision
   - Scores are algorithm-dependent (higher = stronger feature)
   - Orientation may be 0 for single-orientation detectors
   - Size may be 1.0 for single-scale detectors
   - All arrays are float32 (single precision)
   - Features live in GPU memory"
  (:require [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.c-api.features :as ffi]
            [org.soulspace.arrayfire.integration.base.error :refer [check!]]
            [org.soulspace.arrayfire.integration.base.resource :as res]))

;;;
;;; Feature Lifecycle Management
;;;

(defn create-features
  "Create a new features structure with space for num features.
   
   Allocates an af_features handle with uninitialized arrays for x, y,
   score, orientation, and size properties. Each array has length num.
   
   This function is typically used for manual feature construction. Most
   users will receive features from detector functions (FAST, Harris, ORB,
   SIFT, etc.).
   
   The created features have reference count 1. The caller must release
   the handle when done.
   
   Parameters:
   - num: Number of features to allocate space for (long integer)
   
   Returns:
   Long integer representing the features handle (native pointer)
   
   Example:
   ```clojure
   ;; Manual feature construction
   (let [features (create-features 100)]
     (try
       ;; Populate coordinate and property arrays
       (let [x (get-features-xpos features)
             y (get-features-ypos features)]
         ;; Write data to arrays...
         )
       (finally
         (release-features! features))))
   
   ;; Create from filtered subset
   (defn create-filtered [src-features mask]
     (let [n (count-true mask)
           dst (create-features n)]
       ;; Copy filtered data
       dst))
   ```
   
   Notes:
   - Arrays are allocated but uninitialized (undefined values)
   - All arrays are float32 type
   - Arrays live in GPU memory on current device
   - Must explicitly release to prevent memory leaks
   - Empty features (num=0) are valid but rarely useful"
  [num]
  (let [features-ptr-buf (mem/alloc 8)] ; Buffer to hold af_features pointer
    (check! (ffi/af-create-features features-ptr-buf (long num))
                "af-create-features")
    (mem/read-long features-ptr-buf 0)))

(defn retain-features
  "Increment reference count and create a new handle to features.
   
   Creates a shallow copy of the features structure by incrementing the
   internal reference count. Both the original and returned handles point
   to the same underlying data.
   
   This enables efficient sharing of features between code paths without
   deep copying. Each handle can be released independently; memory is
   freed only when the last reference is released.
   
   Parameters:
   - features: Features handle (long integer) to retain
   
   Returns:
   Long integer representing the new features handle
   
   Example:
   ```clojure
   ;; Share features between parallel processes
   (let [features (detect-features image)
         features2 (retain-features features)]
     (future
       (try
         (process-features-a features)
         (finally
           (release-features! features))))
     (future
       (try
         (process-features-b features2)
         (finally
           (release-features! features2)))))
   
   ;; Return features from function (caller owns copy)
   (defn detect-and-filter [image threshold]
     (let [features (detect-features image)]
       (try
         (let [filtered (filter-by-score features threshold)]
           (retain-features filtered))
         (finally
           (release-features! features)))))
   ```
   
   Notes:
   - This is a shallow copy (shares data, not duplicates)
   - Each handle must be released exactly once
   - Reference count incremented atomically
   - Original handle remains valid
   - Efficient: O(1) operation, no data copying"
  [features]
  (let [out-ptr-buf (mem/alloc 8)
        features-segment (mem/as-segment features)]
    (check! (ffi/af-retain-features out-ptr-buf features-segment)
                "af-retain-features")
    (mem/read-long out-ptr-buf 0)))

(defn release-features!
  "Release a features handle and free resources.
   
   Decrements the reference count of the features structure. When the
   count reaches zero, frees all internal arrays (x, y, score, orientation,
   size) and the features structure itself.
   
   Every features handle must be released exactly once. Failure to release
   causes memory leaks; releasing twice causes undefined behavior.
   
   Parameters:
   - features: Features handle (long integer) to release
   
   Returns:
   nil
   
   Example:
   ```clojure
   ;; Basic pattern with try/finally
   (let [features (detect-features image)]
     (try
       (process-features features)
       (finally
         (release-features! features))))
   
   ;; Multiple handles (independent release)
   (let [features1 (detect-features img1)
         features2 (retain-features features1)]
     (try
       (process features1)
       (process features2)
       (finally
         (release-features! features1)
         (release-features! features2))))
   ```
   
   Notes:
   - Always call in finally block or use RAII pattern
   - Handle becomes invalid after release
   - Safe to call with nil/0 (no-op)
   - Releases all internal arrays
   - Not thread-safe: don't release while another thread uses handle"
  [features]
  (when features
    (let [features-segment (mem/as-segment features)]
      (check! (ffi/af-release-features features-segment)
                  "af-release-features")))
  nil)

;;;
;;; Feature Property Access
;;;

(defn get-features-num
  "Get the number of features in a features structure.
   
   Returns the count of features (N) in the structure. This is the length
   of all internal property arrays.
   
   Parameters:
   - features: Features handle (long integer)
   
   Returns:
   Long integer - number of features (>= 0)
   
   Example:
   ```clojure
   ;; Check if features were detected
   (let [features (detect-features image)
         n (get-features-num features)]
     (if (zero? n)
       (println \"No features detected\")
       (println \"Found\" n \"features\")))
   
   ;; Ensure sufficient features before processing
   (defn process-if-sufficient [features min-count]
     (when (>= (get-features-num features) min-count)
       (process-features features)))
   ```
   
   Notes:
   - Fast: O(1) lookup
   - May be zero (valid state, no features detected)
   - All property arrays have this length
   - Thread-safe (read-only operation)"
  [features]
  (let [num-buf (mem/alloc 8)
        features-segment (mem/as-segment features)]
    (check! (ffi/af-get-features-num num-buf features-segment)
                "af-get-features-num")
    (mem/read-long num-buf 0)))

(defn get-features-xpos
  "Get the X coordinates array from features.
   
   Returns an AFArray containing the X (horizontal) pixel coordinates of
   detected features. Coordinates have sub-pixel precision (float).
   
   The returned array is a non-owning reference to the internal array.
   Do not release it separately - it remains valid while the features
   structure is valid.
   
   Parameters:
   - features: Features handle (long integer)
   
   Returns:
   AFArray - float array of length N containing X coordinates
   
   Example:
   ```clojure
   ;; Extract X coordinates
   (let [features (detect-features image)
         x (get-features-xpos features)
         y (get-features-ypos features)]
     (try
       ;; Process coordinates
       (doseq [i (range (get-features-num features))]
         (let [xi (get-element x i)
               yi (get-element y i)]
           (process-point xi yi)))
       (finally
         (release-features! features))))
   
   ;; Filter by X coordinate range
   (defn filter-by-x-range [features min-x max-x]
     (let [x (get-features-xpos features)
           mask (arith/and (arith/ge x min-x)
                           (arith/le x max-x))]
       (select-features features mask)))
   ```
   
   Coordinate System:
   - Origin: Top-left corner (0, 0)
   - X-axis: Increases rightward (horizontal)
   - Range: [0, image_width)
   - Sub-pixel: Fractional coordinates for accuracy
   
   Notes:
   - Non-owning reference (don't release separately)
   - Float32 array (single precision)
   - Coordinates may be fractional
   - Valid as long as features structure is valid"
  [features]
  (let [x-ptr-buf (mem/alloc 8)
        features-segment (mem/as-segment features)]
    (check! (ffi/af-get-features-xpos x-ptr-buf features-segment)
                "af-get-features-xpos")
    (res/af-array-retained (mem/read-long x-ptr-buf 0))))

(defn get-features-ypos
  "Get the Y coordinates array from features.
   
   Returns an AFArray containing the Y (vertical) pixel coordinates of
   detected features. Coordinates have sub-pixel precision (float).
   
   The returned array is a non-owning reference to the internal array.
   
   Parameters:
   - features: Features handle (long integer)
   
   Returns:
   AFArray - float array of length N containing Y coordinates
   
   Example:
   ```clojure
   ;; Extract coordinate pairs
   (defn get-coordinate-pairs [features]
     (let [n (get-features-num features)
           x (get-features-xpos features)
           y (get-features-ypos features)]
       (for [i (range n)]
         [(get-element x i) (get-element y i)])))
   
   ;; Filter by Y coordinate (e.g., upper half of image)
   (defn filter-upper-half [features img-height]
     (let [y (get-features-ypos features)
           mask (arith/lt y (/ img-height 2.0))]
       (select-features features mask)))
   ```
   
   Coordinate System:
   - Origin: Top-left corner (0, 0)
   - Y-axis: Increases downward (vertical)
   - Range: [0, image_height)
   - Sub-pixel: Fractional coordinates for accuracy
   
   Notes:
   - Non-owning reference (don't release separately)
   - Float32 array (single precision)
   - Coordinates may be fractional
   - Valid as long as features structure is valid"
  [features]
  (let [y-ptr-buf (mem/alloc 8)
        features-segment (mem/as-segment features)]
    (check! (ffi/af-get-features-ypos y-ptr-buf features-segment)
                "af-get-features-ypos")
    (res/af-array-retained (mem/read-long y-ptr-buf 0))))

(defn get-features-score
  "Get the score/response array from features.
   
   Returns an AFArray containing the strength or quality score for each
   detected feature. Higher scores indicate stronger, more reliable features.
   
   The score meaning is algorithm-specific:
   - FAST: Sum of intensity differences around circle
   - Harris: Corner response R = det(M) - k*trace(M)²
   - SIFT: Contrast value after thresholding
   - ORB: Harris response at FAST location
   - SUSAN: USAN response
   
   The returned array is a non-owning reference to the internal array.
   
   Parameters:
   - features: Features handle (long integer)
   
   Returns:
   AFArray - float array of length N containing scores
   
   Example:
   ```clojure
   ;; Filter features by score threshold
   (defn filter-by-score [features threshold]
     (let [scores (get-features-score features)
           mask (arith/ge scores threshold)]
       (select-features features mask)))
   
   ;; Get top N strongest features
   (defn get-top-n-features [features n]
     (let [scores (get-features-score features)
           indices (algorithm/sort-index scores 0 false)] ; descending
       (select-features-by-indices features indices n)))
   
   ;; Calculate statistics
   (defn score-statistics [features]
     (let [scores (get-features-score features)]
       {:min (algorithm/min scores)
        :max (algorithm/max features)
        :mean (/ (algorithm/sum scores) 
                 (get-features-num features))}))
   ```
   
   Notes:
   - Non-owning reference (don't release separately)
   - Float32 array (single precision)
   - Higher values = stronger features
   - Scale and meaning algorithm-dependent
   - Useful for filtering and ranking features"
  [features]
  (let [score-ptr-buf (mem/alloc 8)
        features-segment (mem/as-segment features)]
    (check! (ffi/af-get-features-score score-ptr-buf features-segment)
                "af-get-features-score")
    (res/af-array-retained (mem/read-long score-ptr-buf 0))))

(defn get-features-orientation
  "Get the orientation array from features.
   
   Returns an AFArray containing the dominant orientation/direction of each
   feature in radians. This enables rotation-invariant feature matching.
   
   Not all detectors compute orientation:
   - FAST, Harris, SUSAN: Always 0 (not computed)
   - SIFT: Dominant gradient orientation
   - ORB: Intensity centroid orientation
   
   The returned array is a non-owning reference to the internal array.
   
   Parameters:
   - features: Features handle (long integer)
   
   Returns:
   AFArray - float array of length N containing orientations in radians
   
   Example:
   ```clojure
   ;; Check if orientations are computed
   (defn has-orientation? [features]
     (let [ori (get-features-orientation features)
           non-zero (algorithm/count (arith/neq ori 0.0))]
       (pos? non-zero)))
   
   ;; Normalize descriptor by rotation
   (defn extract-normalized-descriptor [image features idx]
     (let [x (get-element (get-features-xpos features) idx)
           y (get-element (get-features-ypos features) idx)
           ori (get-element (get-features-orientation features) idx)
           size (get-element (get-features-size features) idx)]
       (extract-rotated-patch image x y size ori)))
   
   ;; Group features by orientation bins
   (defn group-by-orientation-bin [features num-bins]
     (let [ori (get-features-orientation features)
           bin-size (/ (* 2.0 Math/PI) num-bins)
           bins (arith/div ori bin-size)]
       (group-by-bins features bins num-bins)))
   ```
   
   Notes:
   - Non-owning reference (don't release separately)
   - Float32 array (single precision)
   - Units: Radians (not degrees)
   - Range: [0, 2π) or [-π, π) algorithm-dependent
   - May be all zeros for single-orientation detectors
   - Check before using for rotation invariance"
  [features]
  (let [ori-ptr-buf (mem/alloc 8)
        features-segment (mem/as-segment features)]
    (check! (ffi/af-get-features-orientation ori-ptr-buf features-segment)
                "af-get-features-orientation")
    (res/af-array-retained (mem/read-long ori-ptr-buf 0))))

(defn get-features-size
  "Get the size/scale array from features.
   
   Returns an AFArray containing the characteristic scale or size of each
   feature in pixels. This enables scale-invariant feature matching.
   
   Not all detectors compute size:
   - FAST, Harris, SUSAN: Always 1.0 (single-scale)
   - SIFT: Gaussian scale-space sigma
   - ORB: Pyramid level scale factor
   
   The returned array is a non-owning reference to the internal array.
   
   Parameters:
   - features: Features handle (long integer)
   
   Returns:
   AFArray - float array of length N containing sizes in pixels
   
   Example:
   ```clojure
   ;; Check if multi-scale detection was used
   (defn multi-scale? [features]
     (let [sizes (get-features-size features)
           unique (algorithm/set-unique sizes)]
       (> (get-elements unique) 1)))
   
   ;; Filter by scale range
   (defn filter-by-scale [features min-size max-size]
     (let [sizes (get-features-size features)
           mask (arith/and (arith/ge sizes min-size)
                           (arith/le sizes max-size))]
       (select-features features mask)))
   
   ;; Extract descriptor with scale-adapted window
   (defn extract-scaled-descriptor [image features idx]
     (let [x (get-element (get-features-xpos features) idx)
           y (get-element (get-features-ypos features) idx)
           size (get-element (get-features-size features) idx)
           window-size (* size descriptor-scale-factor)]
       (extract-window image x y window-size)))
   
   ;; Scale distribution statistics
   (defn scale-distribution [features]
     (let [sizes (get-features-size features)]
       {:min (algorithm/min sizes)
        :max (algorithm/max sizes)
        :mean (/ (algorithm/sum sizes)
                 (get-features-num features))}))
   ```
   
   Notes:
   - Non-owning reference (don't release separately)
   - Float32 array (single precision)
   - Units: Pixels (interpretation algorithm-dependent)
   - May be all 1.0 for single-scale detectors
   - Used for scale-invariant matching
   - Affects descriptor extraction window size"
  [features]
  (let [size-ptr-buf (mem/alloc 8)
        features-segment (mem/as-segment features)]
    (check! (ffi/af-get-features-size size-ptr-buf features-segment)
                "af-get-features-size")
    (res/af-array-retained (mem/read-long size-ptr-buf 0))))
