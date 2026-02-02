(ns org.soulspace.arrayfire.ffi.hamming
  "Bindings for ArrayFire Hamming matcher function.
   
   The Hamming matcher is a specialized nearest neighbor search for binary
   feature descriptors using the Hamming distance metric. It's commonly used
   with binary descriptor algorithms like ORB, BRIEF, BRISK, and FREAK.
   
   Mathematical Foundation:
   
   Hamming Distance:
   The Hamming distance between two binary strings is the number of positions
   at which the corresponding bits are different.
   
   For binary vectors a and b of length n:
   d_H(a, b) = Σ(i=0 to n-1) [a[i] ⊕ b[i]]
   
   Where ⊕ is the XOR operation. Equivalently:
   d_H(a, b) = popcount(a XOR b)
   
   Properties:
   - Range: [0, n] where n is the number of bits
   - Integer-valued (discrete metric)
   - Symmetric: d(a,b) = d(b,a)
   - Triangle inequality: d(a,c) ≤ d(a,b) + d(b,c)
   - d(a,b) = 0 if and only if a = b
   
   Binary Descriptors:
   
   Binary feature descriptors represent image features as binary strings,
   typically 128, 256, or 512 bits. Each bit encodes a pairwise intensity
   comparison in the feature's local neighborhood.
   
   Advantages:
   - Compact representation (1 bit per test)
   - Fast to compute (integer operations)
   - Extremely fast matching (XOR + popcount)
   - Low memory footprint
   - Hardware-friendly (SIMD, GPU)
   
   Common Binary Descriptors:
   1. ORB (Oriented FAST and Rotated BRIEF): 256 bits, rotation-invariant
   2. BRIEF (Binary Robust Independent Elementary Features): 128-512 bits, fast
   3. BRISK (Binary Robust Invariant Scalable Keypoints): 512 bits, scale-invariant
   4. FREAK (Fast Retina Keypoint): 512 bits, bio-inspired sampling pattern
   5. AKAZE: 486 bits, nonlinear scale space
   
   Matching Strategy:
   
   For each query descriptor, find the n_dist nearest neighbors in the
   training set based on Hamming distance. The function performs exhaustive
   search (brute-force) across all training descriptors.
   
   Complexity: O(Q × T × B)
   - Q: Number of query descriptors
   - T: Number of training descriptors
   - B: Number of bits / 64 (word comparisons)
   
   GPU Acceleration:
   The Hamming matcher is highly parallelizable:
   - Each query processed independently
   - XOR + popcount operations vectorize well
   - Reduction for finding k nearest neighbors
   - Typical speedup: 10-100× over CPU
   
   Lowe's Ratio Test:
   
   A common post-processing step for robust matching:
   
   ratio = dist1 / dist2
   
   Where dist1 and dist2 are the distances to the first and second nearest
   neighbors. Accept match only if ratio < threshold (typically 0.7-0.8).
   This rejects ambiguous matches where multiple candidates are similar.
   
   Implementation:
   af_hamming_matcher internally calls af_nearest_neighbour with the
   AF_SHD (Sum of Hamming Distances) distance type. It's a convenience
   wrapper for the most common use case of binary descriptor matching.
   
   Relationship to af_nearest_neighbour:
   ```c
   af_hamming_matcher(idx, dist, query, train, dist_dim, n_dist)
   ==
   af_nearest_neighbour(idx, dist, query, train, dist_dim, n_dist, AF_SHD)
   ```
   
   For other distance metrics (Euclidean, Manhattan, etc.), use
   af_nearest_neighbour directly with the appropriate distance type."
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.loader]))

;; af_err af_hamming_matcher(af_array *idx, af_array *dist,
;;                           const af_array query, const af_array train,
;;                           const dim_t dist_dim, const unsigned n_dist)
(defcfn af-hamming-matcher
  "Hamming distance based nearest neighbor matcher for binary descriptors.
   
   Finds the nearest neighbors for each query descriptor in the training set
   using Hamming distance. This is a specialized function for matching binary
   feature descriptors commonly used in computer vision (ORB, BRIEF, BRISK, etc.).
   
   Hamming distance counts the number of differing bits between two binary
   vectors, computed efficiently using XOR and bit counting operations.
   
   Parameters:
   
   - idx: af_array* output indices
     * Integer array containing indices of nearest neighbors
     * Dimensions: [n_dist, num_queries] if dist_dim=0
     * Dimensions: [num_queries, n_dist] if dist_dim=1
     * Values: Indices into the training set [0, num_train-1]
     * Type: u32 or s32
     * For query i and rank j: idx[i,j] = index of jth nearest neighbor
   
   - dist: af_array* output distances
     * Integer array containing Hamming distances
     * Same dimensions as idx
     * Values: Hamming distance (number of differing bits)
     * Type: u32 (always positive)
     * Range: [0, descriptor_bits]
     * For query i: dist[i,0] ≤ dist[i,1] ≤ ... ≤ dist[i,n_dist-1]
   
   - query: af_array query descriptors
     * Binary descriptors to match
     * Dimensions: [descriptor_bits, num_queries] if dist_dim=0
     * Dimensions: [num_queries, descriptor_bits] if dist_dim=1
     * Type: Typically u8 or u32 (packed bits)
     * Each descriptor is a binary feature vector
   
   - train: af_array training descriptors
     * Database of descriptors to search
     * Same structure as query
     * Dimensions: [descriptor_bits, num_train] if dist_dim=0
     * Type: Must match query type
   
   - dist_dim: dim_t dimension along which to compute distances
     * 0: Descriptors in columns (typical for ArrayFire)
     * 1: Descriptors in rows
     * Determines array layout and output orientation
   
   - n_dist: unsigned number of nearest neighbors
     * How many matches to return per query
     * Range: [1, 256] (current implementation limit)
     * Common values: 1 (nearest), 2 (for ratio test)
   
   Returns:
   AF_SUCCESS (0) on success, error code otherwise:
   - AF_ERR_ARG: Invalid array handles
   - AF_ERR_SIZE: Mismatched descriptor dimensions
   - AF_ERR_TYPE: Incompatible data types
   - AF_ERR_NOT_SUPPORTED: n_dist > 256
   
   Algorithm:
   
   1. For each query descriptor q:
      a. For each training descriptor t:
         - Compute Hamming distance: d = popcount(q XOR t)
      b. Find n_dist smallest distances
      c. Store indices and distances
   
   The XOR operation identifies differing bits:
   ```
   query:    10110011
   train:    10010111
   XOR:      00100100  (2 bits differ)
   distance: 2
   ```
   
   Optimization Techniques:
   - GPU parallel processing across queries
   - Vectorized XOR and popcount operations
   - Efficient k-nearest neighbor selection
   - Memory coalescing for descriptor access
   
   Typical Performance:
   - GPU: ~1-10 ms for 1000 queries × 10000 training
   - Depends on descriptor length and n_dist
   - Memory bandwidth bound for large databases
   
   Use Cases:
   
   1. Feature Matching:
   ```clojure
   ;; Match ORB descriptors between two images
   (let [descriptors1 (detect-orb image1)  ; [256 bits, N1 features]
         descriptors2 (detect-orb image2)  ; [256 bits, N2 features]
         idx-ptr (mem/alloc-instance ::mem/pointer)
         dist-ptr (mem/alloc-instance ::mem/pointer)]
     ;; Find 2 nearest neighbors for ratio test
     (af-hamming-matcher idx-ptr dist-ptr descriptors1 descriptors2 0 2)
     (let [indices (mem/read-ptr idx-ptr)     ; [2, N1]
           distances (mem/read-ptr dist-ptr)]  ; [2, N1]
       ;; Apply Lowe's ratio test
       (filter-matches indices distances 0.75)))
   ```
   
   2. Object Recognition:
   ```clojure
   ;; Match object descriptors against database
   (let [object-desc (detect-features object-image)
         database-desc (load-descriptor-database)
         idx-ptr (mem/alloc-instance ::mem/pointer)
         dist-ptr (mem/alloc-instance ::mem/pointer)]
     (af-hamming-matcher idx-ptr dist-ptr object-desc database-desc 0 1)
     (let [best-matches (mem/read-ptr idx-ptr)
           match-distances (mem/read-ptr dist-ptr)]
       ;; Find consensus matches
       (identify-object best-matches match-distances)))
   ```
   
   3. Image Retrieval:
   ```clojure
   ;; Find similar images using binary descriptors
   (let [query-desc (extract-descriptors query-image)
         db-descriptors (load-image-database)
         idx-ptr (mem/alloc-instance ::mem/pointer)
         dist-ptr (mem/alloc-instance ::mem/pointer)]
     (af-hamming-matcher idx-ptr dist-ptr query-desc db-descriptors 0 10)
     (let [top-matches (mem/read-ptr idx-ptr)]
       ;; Aggregate matches by image ID
       (rank-similar-images top-matches)))
   ```
   
   4. Visual SLAM (Simultaneous Localization and Mapping):
   ```clojure
   ;; Match current frame features to map points
   (let [current-features (detect-features current-frame)
         map-features (get-map-descriptors slam-map)
         idx-ptr (mem/alloc-instance ::mem/pointer)
         dist-ptr (mem/alloc-instance ::mem/pointer)]
     (af-hamming-matcher idx-ptr dist-ptr current-features map-features 0 1)
     (let [correspondences (mem/read-ptr idx-ptr)
           match-quality (mem/read-ptr dist-ptr)]
       ;; Use for pose estimation
       (estimate-camera-pose correspondences match-quality)))
   ```
   
   5. Duplicate Detection:
   ```clojure
   ;; Find near-duplicate images
   (let [descriptors (extract-all-descriptors images)
         idx-ptr (mem/alloc-instance ::mem/pointer)
         dist-ptr (mem/alloc-instance ::mem/pointer)
         threshold 20]  ; Maximum Hamming distance
     ;; Self-matching (exclude identical)
     (af-hamming-matcher idx-ptr dist-ptr descriptors descriptors 0 5)
     (let [neighbors (mem/read-ptr idx-ptr)
           distances (mem/read-ptr dist-ptr)]
       ;; Filter by distance threshold
       (find-duplicates neighbors distances threshold)))
   ```
   
   6. Augmented Reality Tracking:
   ```clojure
   ;; Track markers using descriptor matching
   (let [marker-desc (load-marker-descriptors markers)
         frame-desc (detect-features video-frame)
         idx-ptr (mem/alloc-instance ::mem/pointer)
         dist-ptr (mem/alloc-instance ::mem/pointer)]
     (af-hamming-matcher idx-ptr dist-ptr frame-desc marker-desc 0 1)
     (let [matches (mem/read-ptr idx-ptr)]
       ;; Compute homography for AR overlay
       (compute-marker-pose matches)))
   ```
   
   7. Vocabulary Tree Matching:
   ```clojure
   ;; Hierarchical descriptor matching
   (let [query-desc (extract-descriptors query)
         cluster-centers (get-vocabulary-tree-nodes)
         idx-ptr (mem/alloc-instance ::mem/pointer)
         dist-ptr (mem/alloc-instance ::mem/pointer)]
     ;; Assign descriptors to visual words
     (af-hamming-matcher idx-ptr dist-ptr query-desc cluster-centers 0 1)
     (let [word-assignments (mem/read-ptr idx-ptr)]
       ;; Use for bag-of-words image retrieval
       (build-visual-vocabulary word-assignments)))
   ```
   
   Best Practices:
   
   1. Descriptor Normalization:
      - Ensure descriptors are properly packed
      - Use consistent bit ordering
      - Pad to word boundaries for efficiency
   
   2. Lowe's Ratio Test:
      - Use n_dist=2 to get first and second matches
      - Compute ratio = dist[0] / dist[1]
      - Threshold typically 0.7-0.8
      - Rejects ambiguous matches
   
   3. Distance Thresholding:
      - Set maximum acceptable Hamming distance
      - ORB: typical threshold 40-50 (out of 256 bits)
      - BRIEF: threshold 30-40 (out of 256 bits)
      - Lower threshold = stricter matching
   
   4. Bidirectional Matching:
      - Match A→B and B→A
      - Keep only mutual best matches
      - Increases match reliability
   
   5. Geometric Verification:
      - Use RANSAC with matched points
      - Estimate homography or fundamental matrix
      - Filter outliers based on reprojection error
   
   6. Spatial Verification:
      - Check spatial consistency of matches
      - Reject matches with inconsistent transformations
      - Use local neighborhoods
   
   7. Performance Optimization:
      - Batch multiple queries together
      - Use appropriate n_dist value
      - Consider vocabulary trees for large databases
      - Profile GPU memory usage
   
   Comparison with Other Matchers:
   
   | Metric          | Hamming      | Euclidean    | Manhattan   |
   |-----------------|--------------|--------------|-------------|
   | Descriptor Type | Binary       | Float        | Any         |
   | Computation     | XOR+popcount | Sqrt+sum     | Abs+sum     |
   | Speed           | Fastest      | Medium       | Fast        |
   | Memory          | Smallest     | Largest      | Medium      |
   | Accuracy        | Good         | Best         | Good        |
   | GPU Efficiency  | Excellent    | Good         | Good        |
   
   When to Use Hamming:
   - Binary descriptors (ORB, BRIEF, BRISK, FREAK)
   - Real-time applications (speed critical)
   - Memory-constrained systems
   - Large-scale matching (millions of descriptors)
   - Mobile/embedded platforms
   
   When to Use Other Metrics:
   - SIFT/SURF descriptors → Euclidean (af_nearest_neighbour with AF_SSD)
   - HOG features → Euclidean or Manhattan
   - Color histograms → Chi-square or Earth Mover's Distance
   - Deep learning features → Cosine similarity or Euclidean
   
   Limitations:
   
   1. Binary Descriptors Only:
      - Not suitable for float descriptors
      - Use af_nearest_neighbour for SIFT/SURF
   
   2. Exhaustive Search:
      - O(N × M) complexity for N queries, M training
      - Slow for very large databases (millions)
      - Consider approximate methods (LSH, vocabulary trees)
   
   3. No Approximate Matching:
      - Always computes exact k-NN
      - Approximate methods can be faster
      - Trade accuracy for speed
   
   4. Current n_dist Limit:
      - Maximum 256 neighbors
      - Most applications use n_dist ≤ 10
      - For larger k, use multiple calls or post-process
   
   Performance Tips:
   
   1. Descriptor Layout:
      - Prefer column-major (dist_dim=0)
      - Better memory coalescing on GPU
   
   2. Batch Size:
      - Larger batches improve GPU utilization
      - Balance memory usage vs. throughput
   
   3. Data Types:
      - Use u8 for compact storage
      - u32 for faster XOR operations
      - Ensure proper bit packing
   
   4. Memory Management:
      - Reuse arrays when possible
      - Release intermediate arrays
      - Monitor GPU memory usage
   
   Related Functions:
   - af_nearest_neighbour: General k-NN with multiple distance metrics
   - af_orb: ORB feature detection with binary descriptors
   - af_fast: Corner detection (often paired with binary descriptors)
   - af_features: Structure for managing detected features"
  "af_hamming_matcher" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/long ::mem/int] ::mem/int)
