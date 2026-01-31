(ns org.soulspace.arrayfire.ffi.features
  "Bindings for the ArrayFire features structure management functions.
   
   The af_features structure is a fundamental data type in ArrayFire's
   computer vision module, used to represent detected features (corners,
   keypoints, interest points) from various feature detection algorithms.
   
   Structure Definition:
   
   The af_features_t struct contains:
   ```c
   typedef struct {
       size_t n;              // Number of features detected
       af_array x;            // X coordinates (float array of length n)
       af_array y;            // Y coordinates (float array of length n)
       af_array score;        // Corner/feature response strength (length n)
       af_array orientation;  // Feature orientation in radians (length n)
       af_array size;         // Feature scale/size (length n)
   } af_features_t;
   ```
   
   Design Philosophy:
   
   1. Opaque Handle:
      - af_features is an opaque pointer type
      - Internal structure hidden from users
      - Accessed only through API functions
      - Enables future ABI compatibility
   
   2. Array-of-Structures vs Structure-of-Arrays:
      - Uses SoA (Structure-of-Arrays) layout
      - Separate arrays for each property (x, y, score, etc.)
      - GPU-friendly: Enables coalesced memory access
      - Flexible: Can process individual properties independently
      - Efficient: Better cache utilization and vectorization
   
   3. Reference Counting:
      - Automatic memory management
      - Shared ownership semantics
      - Safe copying without deep copy overhead
      - Explicit retain/release for manual control
   
   Feature Properties Explained:
   
   1. X and Y Coordinates:
      - Float arrays of length n
      - Sub-pixel precision coordinates
      - Origin at top-left corner (0, 0)
      - X increases rightward, Y increases downward
      - Coordinates reference the image coordinate system
      - May be fractional for sub-pixel accuracy
   
   2. Score:
      - Feature strength/response value
      - Algorithm-dependent meaning:
        * FAST: Sum of intensity differences
        * Harris: Corner response R = det(M) - k*trace(M)²
        * SIFT: Contrast value after threshold
        * ORB: Harris response at FAST locations
      - Higher scores indicate stronger/more reliable features
      - Used for non-maximal suppression
      - Useful for feature quality filtering
   
   3. Orientation:
      - Feature orientation in radians
      - Range: [0, 2π) or [-π, π) depending on algorithm
      - Enables rotation-invariant matching
      - Algorithm-specific:
        * FAST: Always 0 (not computed)
        * Harris: Always 0 (not computed)
        * SIFT: Dominant gradient orientation
        * ORB: Intensity centroid orientation
        * SUSAN: Always 0 (not computed)
      - Computed from local gradient or moment analysis
   
   4. Size:
      - Feature scale or characteristic size
      - Units: Pixels (diameter or radius, algorithm-dependent)
      - Enables scale-invariant matching
      - Algorithm-specific:
        * FAST: Always 1.0 (single-scale)
        * Harris: Always 1.0 (single-scale)
        * SIFT: Gaussian scale-space sigma
        * ORB: Pyramid level scale factor
        * SUSAN: Always 1.0 (single-scale)
      - Multi-scale detectors use this for invariance
   
   5. Count (n):
      - Total number of features detected
      - All arrays have this length
      - May be zero if no features found
      - Limited by feature_ratio parameter (detector-specific)
   
   Feature Detection Workflow:
   
   1. Detection Phase:
      ```clojure
      ;; Run detector (returns af_features handle)
      (let [features-ptr (mem/alloc-instance ::mem/pointer)]
        (af-fast features-ptr img 20.0 12 1 0.05 3)
        (let [features (mem/read-ptr features-ptr)]
          ;; features now holds opaque handle
          ))
      ```
   
   2. Query Phase:
      ```clojure
      ;; Get feature count
      (let [n-ptr (mem/alloc-instance ::mem/long)]
        (af-get-features-num n-ptr features)
        (let [n (mem/read-long n-ptr)]
          (println \"Found\" n \"features\")))
      
      ;; Extract coordinate arrays
      (let [x-ptr (mem/alloc-instance ::mem/pointer)
            y-ptr (mem/alloc-instance ::mem/pointer)]
        (af-get-features-xpos x-ptr features)
        (af-get-features-ypos y-ptr features)
        ;; Use x and y arrays...
        )
      ```
   
   3. Usage Phase:
      ```clojure
      ;; Use features for tracking, matching, etc.
      ;; Features are GPU arrays - process on device
      
      ;; Extract scores for filtering
      (let [score-ptr (mem/alloc-instance ::mem/pointer)]
        (af-get-features-score score-ptr features)
        ;; Filter by score threshold
        )
      ```
   
   4. Cleanup Phase:
      ```clojure
      ;; Release when done
      (af-release-features features)
      ```
   
   Memory Management Patterns:
   
   1. Feature Lifetime:
      - Features created by detectors (af_fast, af_harris, etc.)
      - Reference count starts at 1
      - Caller owns the returned feature handle
      - Must call af_release_features when done
   
   2. Array Lifetime:
      - Internal arrays (x, y, score, etc.) are managed
      - Retained when feature is retained
      - Released when feature is released
      - Getters return array handles (no ownership transfer)
   
   3. Copying:
      - af_retain_features: Shallow copy (shared arrays)
      - Increments reference count
      - Fast and memory-efficient
      - Both copies share same data
      - Changes to arrays affect all copies
   
   4. Manual Creation:
      - af_create_features: Allocate empty features
      - Arrays are uninitialized
      - Used for custom feature construction
      - Rarely needed (detectors create features)
   
   Integration with Detectors:
   
   All feature detectors return af_features:
   
   1. FAST (af_fast):
      - x, y: Sub-pixel corner locations
      - score: Sum of intensity differences
      - orientation: 0 (not computed)
      - size: 1.0 (single-scale)
   
   2. Harris (af_harris):
      - x, y: Sub-pixel corner locations
      - score: Harris corner response
      - orientation: 0 (not computed)
      - size: 1.0 (single-scale)
   
   3. SUSAN (af_susan):
      - x, y: Corner/edge locations
      - score: USAN response
      - orientation: 0 (not computed)
      - size: 1.0 (single-scale)
   
   4. ORB (af_orb):
      - x, y: Multi-scale corner locations
      - score: Harris response at FAST location
      - orientation: Intensity centroid angle
      - size: Pyramid scale factor
      - Also returns descriptor array separately
   
   5. SIFT (af_sift):
      - x, y: Scale-space extrema locations
      - score: Contrast value
      - orientation: Dominant gradient orientation
      - size: Gaussian scale (sigma)
      - Also returns descriptor array separately
   
   6. GLOH (af_gloh):
      - x, y: Same as SIFT
      - score: Same as SIFT
      - orientation: Same as SIFT
      - size: Same as SIFT
      - Descriptor is GLOH (272-dim) instead of SIFT (128-dim)
   
   Common Use Cases:
   
   1. Feature Point Tracking:
      ```clojure
      ;; Frame 1: Detect features
      (let [feat1 (detect-features frame1)]
        ;; Frame 2: Track with optical flow
        (track-features feat1 frame1 frame2))
      ```
   
   2. Feature Matching:
      ```clojure
      ;; Detect in both images
      (let [feat1 (af-orb img1 ...)
            feat2 (af-orb img2 ...)]
        ;; Match descriptors
        (match-features feat1 feat2))
      ```
   
   3. Visual Odometry:
      ```clojure
      ;; Detect and track over sequence
      (reduce (fn [{:keys [features prev-frame]} frame]
                (let [tracked (track features prev-frame frame)
                      new-feat (if (< (count tracked) threshold)
                                 (detect-features frame)
                                 tracked)]
                  {:features new-feat :prev-frame frame}))
              {:features (detect-features frame0) :prev-frame frame0}
              frames)
      ```
   
   4. 3D Reconstruction:
      ```clojure
      ;; Detect features in multiple views
      (let [features (map detect-features images)]
        ;; Match across views
        (let [matches (multi-view-match features)]
          ;; Triangulate 3D points
          (triangulate matches camera-matrices)))
      ```
   
   5. Image Registration:
      ```clojure
      ;; Find features in reference and target
      (let [ref-feat (detect-features reference)
            tgt-feat (detect-features target)]
        ;; Match and compute transformation
        (compute-homography ref-feat tgt-feat))
      ```
   
   6. Object Detection:
      ```clojure
      ;; Detect features in template
      (let [template-feat (detect-features template)]
        ;; Match against scene
        (locate-object template-feat scene-image))
      ```
   
   Performance Considerations:
   
   1. GPU Memory:
      - Features live in GPU memory
      - Transfer to CPU only when necessary
      - Use GPU-based processing pipelines
      - Minimize host-device transfers
   
   2. Feature Count:
      - More features = more memory
      - More features = slower matching
      - Use feature_ratio to limit count
      - Filter by score for quality
   
   3. Array Access:
      - Getters return handles, not data
      - Arrays remain on GPU
      - Use ArrayFire operations for processing
      - Avoid unnecessary copies
   
   4. Lifetime Management:
      - Release features promptly
      - Don't leak feature handles
      - Use reference counting carefully
      - Consider RAII patterns
   
   Best Practices:
   
   1. Always Release:
      - Call af_release_features when done
      - Use try-finally or resource management
      - Don't leak feature handles
      - Check for null before release
   
   2. Count Before Access:
      - Check n before processing
      - Handle zero features gracefully
      - Avoid accessing empty arrays
   
   3. Immutable Semantics:
      - Treat features as immutable
      - Don't modify internal arrays directly
      - Create new features for modifications
   
   4. Error Handling:
      - Check return codes
      - Handle allocation failures
      - Validate feature counts
      - Check array dimensions
   
   5. Type Safety:
      - Feature arrays are always float (f32)
      - Count is always dim_t (long/int64)
      - Orientation in radians
      - Coordinates are pixel-based
   
   Advanced Patterns:
   
   1. Feature Filtering:
      ```clojure
      ;; Filter by score threshold
      (defn filter-features-by-score [features threshold]
        (let [score-array (get-features-score features)
              mask (af-gt score-array threshold)]
          (af-select-features features mask)))
      ```
   
   2. Feature Merging:
      ```clojure
      ;; Combine features from multiple detectors
      (defn merge-features [feat1 feat2]
        (let [n1 (get-num-features feat1)
              n2 (get-num-features feat2)
              n-total (+ n1 n2)]
          (af-create-merged-features feat1 feat2)))
      ```
   
   3. Spatial Binning:
      ```clojure
      ;; Distribute features across image regions
      (defn bin-features [features width height bins]
        (let [x (get-features-xpos features)
              y (get-features-ypos features)]
          (partition-by-region x y width height bins)))
      ```
   
   4. Temporal Consistency:
      ```clojure
      ;; Match features across frames
      (defn track-temporal [features-seq]
        (map (fn [[prev curr]]
               (match-features prev curr))
             (partition 2 1 features-seq)))
      ```
   
   Limitations and Caveats:
   
   1. Opaque Structure:
      - Cannot access fields directly
      - Must use getter functions
      - Overhead from function calls
      - Less flexible than direct access
   
   2. All-or-Nothing Arrays:
      - Cannot have partial features
      - All arrays must be same length
      - Missing data represented by 0 or NaN
   
   3. Type Constraints:
      - Coordinates always float (no double)
      - May lose precision for large images
      - Count limited by dim_t range
   
   4. Memory Overhead:
      - Five arrays per feature set
      - Unused fields still allocated
      - No sparse representation
   
   5. Thread Safety:
      - Not thread-safe without synchronization
      - Shared features need protection
      - Use per-thread copies or locks
   
   Debugging Tips:
   
   1. Print Feature Count:
      ```clojure
      (println \"Features:\" (get-num-features features))
      ```
   
   2. Visualize Coordinates:
      ```clojure
      ;; Copy to host and plot
      (let [x (host-array (get-features-xpos features))
            y (host-array (get-features-ypos features))]
        (plot-points x y))
      ```
   
   3. Check Score Distribution:
      ```clojure
      (let [scores (get-features-score features)]
        (println \"Min:\" (af-min-all scores))
        (println \"Max:\" (af-max-all scores))
        (println \"Mean:\" (af-mean-all scores)))
      ```
   
   4. Validate Consistency:
      ```clojure
      ;; Ensure all arrays have same length
      (assert (= (af-get-elements (get-x features))
                 (af-get-elements (get-y features))
                 (get-num-features features)))
      ```
   
   Related Functions:
   - af_fast, af_harris, af_susan: Single-scale detectors
   - af_orb, af_sift, af_gloh: Multi-scale detectors with descriptors
   - af_hamming_matcher, af_nearest_neighbour: Feature matching
   - af_homography: Geometric transformation estimation"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; Features structure management functions

;; af_err af_create_features(af_features *feat, dim_t num)
(defcfn af-create-features
  "Create a new features structure with space for num features.
   
   Allocates an af_features handle with uninitialized arrays for x, y,
   score, orientation, and size. Each array has length num. This function
   is typically used for manual feature construction; most users will
   receive features from detectors (af_fast, af_harris, af_orb, etc.).
   
   The created features structure has reference count 1. The caller owns
   the handle and must call af_release_features when done.
   
   Parameters:
   
   - feat: af_features* output handle
     * Points to newly allocated features structure
     * Ownership transferred to caller
     * Must be released with af_release_features
   
   - num: dim_t (long/int64) number of features
     * Allocates arrays of length num
     * If num = 0, creates empty features (valid but useless)
     * Each property array allocated but uninitialized
     * All arrays are float32 type
     * Arrays live in GPU memory
   
   Array Allocation:
   - x: float[num] - X coordinates (uninitialized)
   - y: float[num] - Y coordinates (uninitialized)
   - score: float[num] - Feature scores (uninitialized)
   - orientation: float[num] - Orientations in radians (uninitialized)
   - size: float[num] - Feature sizes (uninitialized)
   
   Returns:
   AF_SUCCESS (0) on success, error code otherwise:
   - AF_ERR_NO_MEM: Insufficient GPU memory for arrays
   - AF_ERR_ARG: Invalid num (negative)
   
   Usage:
   
   Manual feature construction:
   ```clojure
   (let [feat-ptr (mem/alloc-instance ::mem/pointer)
         n 100]
     (af-create-features feat-ptr n)
     (let [feat (mem/read-ptr feat-ptr)]
       ;; Now populate arrays manually
       (let [x-ptr (mem/alloc-instance ::mem/pointer)
             y-ptr (mem/alloc-instance ::mem/pointer)]
         (af-get-features-xpos x-ptr feat)
         (af-get-features-ypos y-ptr feat)
         ;; Write data to x and y arrays
         ;; ...
         (af-release-features feat))))
   ```
   
   Typical use case:
   ```clojure
   ;; Filter existing features
   (defn filter-features [features threshold]
     (let [score-arr (get-score features)
           mask (af-gt score-arr threshold)
           n-filtered (af-count-all mask)
           filtered (mem/alloc-instance ::mem/pointer)]
       (af-create-features filtered n-filtered)
       ;; Copy filtered coordinates
       ;; ...
       ))
   ```
   
   Notes:
   - Arrays are allocated but not initialized
   - Must initialize before use (undefined values)
   - Consider using detectors instead of manual creation
   - Empty features (num=0) are valid but rarely useful
   - All arrays are float32 (single precision)
   - Arrays allocated in current device memory
   
   See Also:
   - af_release_features: Free features when done
   - af_retain_features: Increment reference count
   - af_fast, af_harris, af_orb: Feature detectors (preferred)"
  "af_create_features" [::mem/pointer ::mem/long] ::mem/int)

;; af_err af_retain_features(af_features *out, const af_features feat)
(defcfn af-retain-features
  "Increment reference count and create a new handle to features.
   
   Creates a shallow copy of the features structure by incrementing the
   reference count of the internal arrays. Both the original and the
   returned handle point to the same underlying data. This is an efficient
   way to share features between multiple code paths without deep copying.
   
   When any handle is released, only the reference count is decremented.
   The actual memory is freed only when the last reference is released.
   
   Parameters:
   
   - out: af_features* output handle
     * New handle pointing to same features
     * Independent handle (can be released separately)
     * Shares data with input features
     * Caller owns this handle
   
   - feat: af_features input handle
     * Features to retain/copy
     * Reference count incremented
     * Remains valid after call
     * Original handle unaffected
   
   Reference Counting Behavior:
   
   1. Initial state: feat has ref_count = 1
   2. After af_retain_features(&out, feat):
      - feat still valid (ref_count now 2)
      - out now valid (same data, ref_count = 2)
   3. After af_release_features(feat):
      - feat handle freed (ref_count now 1)
      - out still valid (data still alive)
   4. After af_release_features(out):
      - out handle freed (ref_count now 0)
      - Data finally freed
   
   Returns:
   AF_SUCCESS (0) on success, error code otherwise:
   - AF_ERR_ARG: Invalid input features handle
   - AF_ERR_NO_MEM: Failed to allocate new handle
   
   Usage:
   
   Sharing features:
   ```clojure
   (defn process-features [features]
     ;; Retain for asynchronous processing
     (let [retained-ptr (mem/alloc-instance ::mem/pointer)]
       (af-retain-features retained-ptr features)
       (let [retained (mem/read-ptr retained-ptr)]
         ;; Pass to another thread/function
         (future
           (do-expensive-work retained)
           ;; Release when done
           (af-release-features retained)))))
   ```
   
   Caching:
   ```clojure
   (let [cache (atom {})]
     (defn get-or-compute-features [img]
       (if-let [cached (@cache img)]
         ;; Return retained copy
         (let [out-ptr (mem/alloc-instance ::mem/pointer)]
           (af-retain-features out-ptr cached)
           (mem/read-ptr out-ptr))
         ;; Compute and cache
         (let [features (af-fast img 20.0 12 true 0.05 3)]
           (swap! cache assoc img features)
           features))))
   ```
   
   Notes:
   - Shallow copy: Shares underlying arrays
   - Fast: O(1) operation, no data copying
   - Modifying arrays affects all handles
   - Each handle must be released independently
   - Thread-safe at the ArrayFire array level
   - Not the same as deep copy (use manual copy for that)
   
   Common Pitfall:
   ```clojure
   ;; WRONG: Releasing original invalidates returned handle
   (let [out-ptr (mem/alloc-instance ::mem/pointer)]
     (af-retain-features out-ptr features)
     (af-release-features features)  ; DON'T do this immediately!
     (mem/read-ptr out-ptr))  ; out is still valid (ref count = 1)
   
   ;; CORRECT: Each handle released when no longer needed
   (let [out-ptr (mem/alloc-instance ::mem/pointer)]
     (af-retain-features out-ptr features)
     (let [out (mem/read-ptr out-ptr)]
       ;; Use out...
       ;; Original can be released in its own scope
       (af-release-features features)
       ;; out still valid, release when done
       (do-work out)
       (af-release-features out)))
   ```
   
   See Also:
   - af_release_features: Decrement reference count
   - af_create_features: Create new features (ref count = 1)
   - af_array retain/release: Similar pattern for arrays"
  "af_retain_features" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_release_features(af_features feat)
(defcfn af-release-features
  "Release a features handle and decrement its reference count.
   
   Decrements the reference count of the features structure. When the count
   reaches zero, frees all internal arrays (x, y, score, orientation, size)
   and the features structure itself.
   
   This is the complement to af_create_features and af_retain_features.
   Every features handle must be released exactly once. Failure to release
   leads to memory leaks; releasing twice leads to undefined behavior.
   
   Parameters:
   
   - feat: af_features handle to release
     * Handle becomes invalid after this call
     * Do not use feat after release
     * Internal arrays may be freed (if ref count reaches 0)
     * Safe to pass NULL/0 (no-op)
   
   Reference Counting:
   - Decrements reference count
   - If ref count > 0: Data remains alive
   - If ref count = 0: Frees all memory
   - Each retain must be matched with release
   
   Returns:
   AF_SUCCESS (0) always (even if feat is NULL):
   - Does not return errors for invalid handles
   - Safe to call multiple times (discouraged)
   - Idempotent on NULL
   
   Usage:
   
   Basic pattern:
   ```clojure
   (let [feat-ptr (mem/alloc-instance ::mem/pointer)]
     (af-fast feat-ptr img 20.0 12 1 0.05 3)
     (try
       (let [feat (mem/read-ptr feat-ptr)]
         ;; Use features
         (process-features feat))
       (finally
         ;; Always release in finally block
         (when-let [feat (mem/read-ptr feat-ptr)]
           (af-release-features feat)))))
   ```
   
   With resource management:
   ```clojure
   (defmacro with-features [[binding detector-form] & body]
     `(let [ptr# (mem/alloc-instance ::mem/pointer)
            _# ~detector-form  ; Writes to ptr#
            ~binding (mem/read-ptr ptr#)]
        (try
          ~@body
          (finally
            (when ~binding
              (af-release-features ~binding))))))
   
   (with-features [feat (af-fast feat-ptr img 20.0 12 1 0.05 3)]
     (println \"Found\" (get-num-features feat) \"features\"))
   ```
   
   Multiple handles:
   ```clojure
   ;; Each handle released independently
   (let [feat1 (detect-features img1)
         feat2-ptr (mem/alloc-instance ::mem/pointer)]
     (af-retain-features feat2-ptr feat1)
     (let [feat2 (mem/read-ptr feat2-ptr)]
       (try
         (process feat1)
         (process feat2)
         (finally
           (af-release-features feat1)
           (af-release-features feat2)))))
   ```
   
   Notes:
   - Always call in finally block or RAII pattern
   - Safe to call on NULL (no-op)
   - Do not use handle after release
   - Releases internal arrays (x, y, score, etc.)
   - Arrays may remain alive if retained elsewhere
   - Not thread-safe: Don't release while another thread uses
   
   Common Pitfalls:
   
   1. Forgetting to release:
   ```clojure
   ;; WRONG: Memory leak
   (defn detect-and-count [img]
     (let [feat-ptr (mem/alloc-instance ::mem/pointer)]
       (af-fast feat-ptr img 20.0 12 1 0.05 3)
       (get-num-features (mem/read-ptr feat-ptr))))
   ;; feat never released - memory leak!
   
   ;; CORRECT:
   (defn detect-and-count [img]
     (let [feat-ptr (mem/alloc-instance ::mem/pointer)]
       (af-fast feat-ptr img 20.0 12 1 0.05 3)
       (let [feat (mem/read-ptr feat-ptr)]
         (try
           (get-num-features feat)
           (finally
             (af-release-features feat))))))
   ```
   
   2. Using after release:
   ```clojure
   ;; WRONG: Use after free
   (let [feat (detect-features img)]
     (af-release-features feat)
     (process-features feat))  ; UNDEFINED BEHAVIOR!
   
   ;; CORRECT: Release after last use
   (let [feat (detect-features img)]
     (try
       (process-features feat)
       (finally
         (af-release-features feat))))
   ```
   
   3. Double release:
   ```clojure
   ;; WRONG: Double free
   (let [feat (detect-features img)]
     (af-release-features feat)
     (af-release-features feat))  ; UNDEFINED BEHAVIOR!
   
   ;; CORRECT: Release once, nullify
   (let [feat (atom (detect-features img))]
     (try
       (process @feat)
       (finally
         (when-let [f @feat]
           (af-release-features f)
           (reset! feat nil)))))
   ```
   
   See Also:
   - af_create_features: Allocate new features
   - af_retain_features: Increment reference count
   - af_array release: Similar pattern for arrays"
  "af_release_features" [::mem/pointer] ::mem/int)

;; af_err af_get_features_num(dim_t *num, const af_features feat)
(defcfn af-get-features-num
  "Get the number of features in a features structure.
   
   Returns the count of features (n) in the features structure. This is
   the length of all internal arrays (x, y, score, orientation, size).
   
   Parameters:
   
   - num: dim_t* (long* / int64*) output count
     * Number of features
     * Range: [0, MAX_DIM_T]
     * 0 indicates no features detected
     * All arrays have this length
   
   - feat: af_features handle
     * Features structure to query
     * Must be valid handle
     * Not modified by this call
   
   Returns:
   AF_SUCCESS (0) on success, error code otherwise:
   - AF_ERR_ARG: Invalid features handle
   
   Usage:
   ```clojure
   (let [n-ptr (mem/alloc-instance ::mem/long)]
     (af-get-features-num n-ptr features)
     (let [n (mem/read-long n-ptr)]
       (if (zero? n)
         (println \"No features detected\")
         (println \"Found\" n \"features\"))))
   ```
   
   Checking before processing:
   ```clojure
   (defn process-if-sufficient [features min-features]
     (let [n (get-num features)]
       (when (>= n min-features)
         (process-features features))))
   ```
   
   Notes:
   - Always check before processing
   - May be zero (valid state)
   - Matches length of all internal arrays
   - Fast: O(1) lookup
   - Thread-safe (read-only)
   
   See Also:
   - af_get_elements: Get array element counts
   - af_get_features_xpos: Extract X coordinate array"
  "af_get_features_num" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_get_features_xpos(af_array *x, const af_features feat)
(defcfn af-get-features-xpos
  "Get the X coordinates array from a features structure.
   
   Returns a handle to the internal X coordinates array. The array
   contains float values representing the X (horizontal) pixel coordinates
   of detected features. Coordinates may be sub-pixel (fractional).
   
   The returned array handle is not a copy; it points to the internal
   array. The handle does not have ownership; do not release it. The array
   remains valid as long as the features structure is valid.
   
   Parameters:
   
   - x: af_array* output array handle
     * Float array of length n (n = number of features)
     * X coordinates in pixels
     * Sub-pixel precision (float)
     * Range: [0, image_width)
     * Non-owning reference (do not release)
   
   - feat: af_features handle
     * Features structure to query
     * Must be valid handle
     * Not modified by this call
   
   Coordinate System:
   - Origin: Top-left corner (0, 0)
   - X-axis: Increases rightward (horizontal)
   - Fractional: Sub-pixel accuracy
   - Units: Pixels
   
   Returns:
   AF_SUCCESS (0) on success, error code otherwise:
   - AF_ERR_ARG: Invalid features handle
   
   Usage:
   ```clojure
   (let [x-ptr (mem/alloc-instance ::mem/pointer)]
     (af-get-features-xpos x-ptr features)
     (let [x-array (mem/read-ptr x-ptr)]
       ;; Use x-array (don't release!)
       (process-coordinates x-array)))
   ```
   
   Visualizing features:
   ```clojure
   (defn draw-features [img features]
     (let [x-ptr (mem/alloc-instance ::mem/pointer)
           y-ptr (mem/alloc-instance ::mem/pointer)]
       (af-get-features-xpos x-ptr features)
       (af-get-features-ypos y-ptr features)
       (let [x (mem/read-ptr x-ptr)
             y (mem/read-ptr y-ptr)]
         ;; Draw circles at (x[i], y[i])
         (draw-points img x y))))
   ```
   
   Filtering by position:
   ```clojure
   (defn filter-by-region [features x-min x-max y-min y-max]
     (let [x (get-x features)
           y (get-y features)
           mask-x (af-and (af-ge x x-min) (af-le x x-max))
           mask-y (af-and (af-ge y y-min) (af-le y y-max))
           mask (af-and mask-x mask-y)]
       (select-features features mask)))
   ```
   
   Notes:
   - Array handle is non-owning reference
   - Do NOT call af_release_array on returned handle
   - Array remains valid while features valid
   - Modifying array affects the features structure
   - Always float type (f32)
   - May contain NaN or Inf (check if concerned)
   
   Common Pitfall:
   ```clojure
   ;; WRONG: Don't release the array!
   (let [x-ptr (mem/alloc-instance ::mem/pointer)]
     (af-get-features-xpos x-ptr features)
     (let [x (mem/read-ptr x-ptr)]
       (process x)
       (af-release-array x)))  ; WRONG! Don't release!
   
   ;; CORRECT: Just use the array
   (let [x-ptr (mem/alloc-instance ::mem/pointer)]
     (af-get-features-xpos x-ptr features)
     (let [x (mem/read-ptr x-ptr)]
       (process x)))  ; Array freed when features released
   ```
   
   See Also:
   - af_get_features_ypos: Get Y coordinates
   - af_get_features_num: Get number of features
   - af_get_elements: Get array length"
  "af_get_features_xpos" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_get_features_ypos(af_array *y, const af_features feat)
(defcfn af-get-features-ypos
  "Get the Y coordinates array from a features structure.
   
   Returns a handle to the internal Y coordinates array. The array
   contains float values representing the Y (vertical) pixel coordinates
   of detected features. Coordinates may be sub-pixel (fractional).
   
   The returned array handle is not a copy; it points to the internal
   array. The handle does not have ownership; do not release it.
   
   Parameters:
   
   - y: af_array* output array handle
     * Float array of length n
     * Y coordinates in pixels
     * Sub-pixel precision (float)
     * Range: [0, image_height)
     * Non-owning reference
   
   - feat: af_features handle
   
   Coordinate System:
   - Origin: Top-left corner (0, 0)
   - Y-axis: Increases downward (vertical)
   - Fractional: Sub-pixel accuracy
   
   Returns:
   AF_SUCCESS (0) on success, error code otherwise.
   
   See af_get_features_xpos for usage examples and notes.
   
   See Also:
   - af_get_features_xpos: Get X coordinates"
  "af_get_features_ypos" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_get_features_score(af_array *score, const af_features feat)
(defcfn af-get-features-score
  "Get the score/response array from a features structure.
   
   Returns a handle to the internal score array. Scores represent the
   strength or quality of each detected feature. Higher scores indicate
   stronger, more reliable features. The meaning of the score value is
   algorithm-specific.
   
   The returned array handle is non-owning; do not release it.
   
   Parameters:
   
   - score: af_array* output array handle
     * Float array of length n
     * Feature strength/quality values
     * Higher = stronger/better feature
     * Algorithm-dependent scale
     * Non-owning reference
   
   - feat: af_features handle
   
   Score Meaning by Detector:
   - FAST: Sum of intensity differences around circle
   - Harris: Corner response R = det(M) - k*trace(M)²
   - SIFT: Contrast value (after thresholding)
   - ORB: Harris response at FAST location
   - SUSAN: USAN (Univalue Segment Assimilating Nucleus) response
   
   Returns:
   AF_SUCCESS (0) on success, error code otherwise.
   
   Usage:
   ```clojure
   ;; Filter by score threshold
   (defn filter-by-score [features threshold]
     (let [score-ptr (mem/alloc-instance ::mem/pointer)]
       (af-get-features-score score-ptr features)
       (let [score (mem/read-ptr score-ptr)
             mask (af-ge score threshold)]
         (select-features features mask))))
   
   ;; Get top N features
   (defn get-top-n [features n]
     (let [score (get-score features)
           sorted-idx (af-sort-index score false)]  ; descending
       (select-features features (af-seq 0 (dec n) 1))))
   ```
   
   See Also:
   - af_get_features_xpos: Get X coordinates
   - af_get_features_orientation: Get orientations"
  "af_get_features_score" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_get_features_orientation(af_array *orientation, const af_features feat)
(defcfn af-get-features-orientation
  "Get the orientation array from a features structure.
   
   Returns a handle to the internal orientation array. Orientations are
   in radians and represent the dominant direction or angle of each
   feature. This enables rotation-invariant feature matching.
   
   Not all detectors compute orientation:
   - FAST, Harris, SUSAN: Always 0 (not computed)
   - SIFT: Dominant gradient orientation
   - ORB: Intensity centroid orientation
   
   The returned array handle is non-owning; do not release it.
   
   Parameters:
   
   - orientation: af_array* output array handle
     * Float array of length n
     * Orientation in radians
     * Range: [0, 2π) or [-π, π) depending on algorithm
     * 0 if not computed
     * Non-owning reference
   
   - feat: af_features handle
   
   Returns:
   AF_SUCCESS (0) on success, error code otherwise.
   
   Usage:
   ```clojure
   ;; Check if orientations computed
   (defn has-orientation? [features]
     (let [ori (get-orientation features)
           non-zero (af-count-all (af-neq ori 0.0))]
       (pos? non-zero)))
   
   ;; Rotate descriptor based on orientation
   (defn normalize-descriptor [desc orientation]
     (rotate-descriptor desc (- orientation)))
   ```
   
   Notes:
   - May be all zeros (check before using)
   - Units: Radians (not degrees)
   - Used for rotation invariance
   - Algorithm-specific computation method
   
   See Also:
   - af_get_features_size: Get feature scales
   - af_get_features_score: Get feature strengths"
  "af_get_features_orientation" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_get_features_size(af_array *size, const af_features feat)
(defcfn af-get-features-size
  "Get the size/scale array from a features structure.
   
   Returns a handle to the internal size array. Sizes represent the
   characteristic scale of each feature in pixels. This enables
   scale-invariant feature matching.
   
   Not all detectors compute size:
   - FAST, Harris, SUSAN: Always 1.0 (single-scale)
   - SIFT: Gaussian scale-space sigma
   - ORB: Pyramid level scale factor
   
   The returned array handle is non-owning; do not release it.
   
   Parameters:
   
   - size: af_array* output array handle
     * Float array of length n
     * Feature scale/size in pixels
     * 1.0 for single-scale detectors
     * Units: Pixels (diameter or sigma)
     * Non-owning reference
   
   - feat: af_features handle
   
   Returns:
   AF_SUCCESS (0) on success, error code otherwise.
   
   Usage:
   ```clojure
   ;; Check if multi-scale
   (defn multi-scale? [features]
     (let [size (get-size features)
           unique-sizes (af-set-unique size)]
       (> (af-get-elements unique-sizes) 1)))
   
   ;; Filter by scale range
   (defn filter-by-scale [features min-size max-size]
     (let [size (get-size features)
           mask (af-and (af-ge size min-size)
                        (af-le size max-size))]
       (select-features features mask)))
   
   ;; Scale descriptor window
   (defn extract-descriptor [img x y size]
     (let [window-size (* size descriptor-scale)]
       (extract-window img x y window-size)))
   ```
   
   Notes:
   - May be all 1.0 (single-scale detectors)
   - Units and meaning algorithm-specific
   - Used for scale invariance
   - Affects descriptor extraction window size
   
   See Also:
   - af_get_features_orientation: Get feature orientations
   - af_get_features_score: Get feature strengths
   - af_get_features_num: Get number of features"
  "af_get_features_size" [::mem/pointer ::mem/pointer] ::mem/int)
