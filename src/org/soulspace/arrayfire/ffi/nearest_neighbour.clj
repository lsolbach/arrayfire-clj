(ns org.soulspace.arrayfire.ffi.nearest-neighbour
  "Bindings for the ArrayFire nearest neighbour search functions.
   
   Nearest neighbour search is a fundamental operation in machine learning,
   computer vision, and data mining that finds the closest points in a dataset
   to a given query point based on some distance metric.
   
   ## What is Nearest Neighbour Search?
   
   Nearest neighbour (NN) search finds the k-nearest points in a training set
   to each point in a query set. This is also known as k-NN (k-nearest
   neighbours) and is one of the simplest yet most effective algorithms in
   machine learning.
   
   **Basic Concept**:
   Given:
   - Training set T: {t₁, t₂, ..., tₙ} (known points with labels)
   - Query set Q: {q₁, q₂, ..., qₘ} (points to classify/find neighbours for)
   - Distance metric d(·,·)
   - Number of neighbours k
   
   For each query point qᵢ:
   1. Compute distance d(qᵢ, tⱼ) to all training points
   2. Sort distances to find k smallest
   3. Return indices and distances of k-nearest neighbours
   
   **Visual Example** (2D space, k=3):
   ```
   Training points (T):     Query point (q):
       t1: (1, 2)              q: (5, 5)
       t2: (2, 3)          
       t3: (4, 4)          Distances:
       t4: (5, 6)              d(q, t1) = 5.0
       t5: (8, 9)              d(q, t2) = 4.2
       t6: (3, 1)              d(q, t3) = 1.4  ← nearest
                               d(q, t4) = 1.0  ← nearest
                               d(q, t5) = 5.7
                               d(q, t6) = 5.7
   
   3-Nearest neighbours: [t4, t3, t2]
   Distances: [1.0, 1.4, 4.2]
   ```
   
   ## Distance Metrics
   
   ArrayFire supports three distance metrics via the af_match_type enum:
   
   ### 1. Sum of Absolute Differences (SAD / Manhattan / L1)
   
   **Formula**:
   d_SAD(x, y) = Σᵢ |xᵢ - yᵢ|
   
   **Properties**:
   - Also called Manhattan distance or L1 norm
   - Measures distance along axes (like city blocks)
   - Less sensitive to outliers than SSD
   - Faster to compute than SSD (no multiplication)
   
   **When to use**:
   - When outliers are present
   - When computational efficiency is critical
   - For high-dimensional sparse data
   
   **Example** (2D):
   ```
   x = [3, 4]
   y = [1, 2]
   d_SAD = |3-1| + |4-2| = 2 + 2 = 4
   ```
   
   ### 2. Sum of Squared Differences (SSD / Euclidean² / L2²)
   
   **Formula**:
   d_SSD(x, y) = Σᵢ (xᵢ - yᵢ)²
   
   **Properties**:
   - Squared Euclidean distance (avoids sqrt for speed)
   - More sensitive to large differences (outliers)
   - Standard metric for k-NN classification
   - Monotonic with Euclidean distance (same ordering)
   
   **When to use**:
   - Standard k-NN classification
   - When outliers should be penalized more
   - Most common choice for NN search
   
   **Example** (2D):
   ```
   x = [3, 4]
   y = [1, 2]
   d_SSD = (3-1)² + (4-2)² = 4 + 4 = 8
   ```
   
   ### 3. Hamming Distance (SHD)
   
   **Formula**:
   d_SHD(x, y) = popcount(x ⊕ y)
   
   Where:
   - ⊕ is bitwise XOR
   - popcount counts number of 1-bits
   
   **Properties**:
   - For binary/integer data only
   - Counts number of differing bits
   - Very fast (single XOR + popcount instruction)
   - Used for binary descriptors (ORB, BRIEF, BRISK)
   
   **When to use**:
   - Binary feature descriptors
   - DNA sequence matching
   - Error detection/correction
   - Network addresses
   
   **Example** (8-bit):
   ```
   x = 0b10110100  (180)
   y = 0b10010110  (150)
   XOR = 0b00100010
   d_SHD = popcount(XOR) = 2  (2 bits differ)
   ```
   
   **Supported Types for Hamming**:
   - u8 (uchar): 8-bit binary descriptors
   - u16 (ushort): 16-bit descriptors
   - u32 (uint): 32-bit descriptors
   - u64 (uintl): 64-bit descriptors
   
   ## Data Layout and dist_dim Parameter
   
   The `dist_dim` parameter specifies which dimension contains the feature
   coordinates. This affects how points are organized in memory.
   
   ### dist_dim = 0 (Features along dimension 0)
   
   **Layout**:
   - dim0: Feature dimension (coordinates)
   - dim1: Sample dimension (points)
   - Shape: [n_features, n_samples]
   
   **Example** (3 features, 5 samples):
   ```
   Array shape: [3, 5]
   
   [[x1, x2, x3, x4, x5]    ← Feature 0 (e.g., red channel)
    [y1, y2, y3, y4, y5]    ← Feature 1 (e.g., green channel)
    [z1, z2, z3, z4, z5]]   ← Feature 2 (e.g., blue channel)
    
   Point 1: [x1, y1, z1]
   Point 2: [x2, y2, z2]
   ...
   ```
   
   **Memory layout**: Column-major (features contiguous in memory)
   **Common for**: Computer vision features, color histograms
   
   ### dist_dim = 1 (Features along dimension 1)
   
   **Layout**:
   - dim0: Sample dimension (points)
   - dim1: Feature dimension (coordinates)
   - Shape: [n_samples, n_features]
   
   **Example** (5 samples, 3 features):
   ```
   Array shape: [5, 3]
   
   [[x1, y1, z1]    ← Point 1
    [x2, y2, z2]    ← Point 2
    [x3, y3, z3]    ← Point 3
    [x4, y4, z4]    ← Point 4
    [x5, y5, z5]]   ← Point 5
   ```
   
   **Memory layout**: Row-major (points contiguous in memory)
   **Common for**: Machine learning datasets, tabular data
   
   ### Choosing dist_dim
   
   - **dist_dim = 0**: When features are naturally separate (images, signals)
   - **dist_dim = 1**: When samples are independent (ML datasets)
   - **Performance**: Choose based on how data is naturally organized
   - **Consistency**: Query and train must use same dist_dim
   
   ## Output Format
   
   The function returns two arrays:
   
   ### Indices Array
   
   **Shape**: [n_dist, n_queries]
   **Type**: uint (u32)
   **Content**: indices[i, j] = index of i-th nearest training point to query j
   
   **Example** (k=3, 2 queries):
   ```
   indices = [[2, 5]     ← 1st nearest for query 0, query 1
              [7, 3]     ← 2nd nearest
              [1, 9]]    ← 3rd nearest
   ```
   
   ### Distances Array
   
   **Shape**: [n_dist, n_queries]
   **Type**: Same as input type (or uint for Hamming on integer types)
   **Content**: dist[i, j] = distance from query j to its i-th nearest neighbour
   
   **Example** (k=3, 2 queries):
   ```
   distances = [[1.2, 0.8]    ← Distance to 1st nearest
                [2.5, 1.1]    ← Distance to 2nd nearest
                [3.7, 2.3]]   ← Distance to 3rd nearest
   ```
   
   ## Algorithm and Performance
   
   **Algorithm**:
   1. Compute all pairwise distances: O(N_q × N_t × d)
      - N_q: number of queries
      - N_t: number of training points
      - d: feature dimensionality
   2. Find k smallest via partial sort: O(N_q × N_t × log(k))
   3. Total: O(N_q × N_t × d)
   
   **Implementation Details**:
   - **Distance computation**: Highly parallelized on GPU
     * CUDA: Uses shared memory for training features
     * Threshold for shared memory: depends on feature dimension
     * Unrolled loops for small feature dimensions
   - **k-selection**: Uses topk() for finding k smallest
     * Efficient partial sort (heap-based)
     * Only k=1 to 256 supported currently
   - **Memory optimization**:
     * Transposed data for optimal memory access
     * Shared memory for frequently accessed training data
     * Coalesced memory accesses on GPU
   
   **Performance Characteristics**:
   - GPU acceleration: 50-200× faster than CPU
   - Scales with:
     * Number of queries (linear)
     * Number of training points (linear)
     * Feature dimension (linear)
     * k (logarithmic for k < 256)
   - Bottleneck: Usually distance computation, not k-selection
   
   **Typical Timings** (NVIDIA GPU, f32):
   - 1000 queries × 10,000 training × 128 features: ~5-10ms
   - 10,000 queries × 100,000 training × 64 features: ~100-200ms
   - Small k (1-10): Minimal overhead
   - Large k (100-256): 2-3× slower due to k-selection
   
   ## Applications
   
   ### 1. k-NN Classification
   ```clojure
   ;; Train: features + labels
   ;; Test: features to classify
   ;; Find k=5 nearest, vote on labels
   (let [k 5
         [idx dist] (nearest-neighbour test-features train-features 0 k AF_SSD)
         nearest-labels (index train-labels idx)  ; Get labels of k-nearest
         predictions (mode nearest-labels 0)]     ; Majority vote per query
     predictions)
   ```
   
   ### 2. Anomaly Detection
   ```clojure
   ;; If nearest neighbour is far, point is anomalous
   (let [[idx dist] (nearest-neighbour data data 0 1 AF_SSD)
         threshold 10.0
         anomalies (> dist threshold)]
     anomalies)
   ```
   
   ### 3. Image Retrieval
   ```clojure
   ;; Find similar images by feature vectors
   (let [query-features (extract-features query-image)
         db-features (extract-features image-database)
         k 10
         [idx dist] (nearest-neighbour query-features db-features 0 k AF_SSD)
         similar-images (index image-database idx)]
     similar-images)
   ```
   
   ### 4. Feature Matching (Computer Vision)
   ```clojure
   ;; Match ORB/BRIEF binary descriptors between images
   (let [desc1 (extract-orb-descriptors img1)  ; u8 binary descriptors
         desc2 (extract-orb-descriptors img2)
         [idx dist] (nearest-neighbour desc1 desc2 0 2 AF_SHD)
         ;; Ratio test: match if dist[0]/dist[1] < 0.8
         good-matches (< (/ (index dist 0) (index dist 1)) 0.8)]
     good-matches)
   ```
   
   ### 5. Recommendation Systems
   ```clojure
   ;; Find similar users/items
   (let [user-features (compute-embeddings users)
         [idx dist] (nearest-neighbour target-user user-features 0 10 AF_SSD)
         similar-users (index users idx)
         recommendations (get-items similar-users)]
     recommendations)
   ```
   
   ### 6. Clustering (k-means)
   ```clojure
   ;; Assign points to nearest centroid
   (loop [centroids initial-centroids
          iterations 0]
     (let [[idx dist] (nearest-neighbour data centroids 0 1 AF_SSD)
           new-centroids (compute-centroids data idx)]
       (if (or (converged? centroids new-centroids)
               (>= iterations max-iterations))
         new-centroids
         (recur new-centroids (inc iterations)))))
   ```
   
   ### 7. Density Estimation
   ```clojure
   ;; Estimate local density via k-nearest distances
   (let [k 10
         [idx dist] (nearest-neighbour data data 0 k AF_SSD)
         ;; Density ∝ 1 / (average distance to k-nearest)
         avg-dist (mean dist 0)
         density (/ 1.0 (+ avg-dist epsilon))]
     density)
   ```
   
   ## Design Patterns
   
   ### Pattern 1: Ratio Test for Robust Matching
   ```clojure
   ;; Find 2-nearest, accept only if significantly closer than 2nd
   (defn robust-match [query train ratio-threshold]
     (let [[idx dist] (nearest-neighbour query train 0 2 AF_SSD)
           dist1 (index dist 0)
           dist2 (index dist 1)
           ratio (/ dist1 (+ dist2 1e-10))
           good-matches (< ratio ratio-threshold)
           idx1 (index idx 0)]
       ;; Return only confident matches
       {:indices (where good-matches idx1)
        :distances (where good-matches dist1)}))
   ```
   
   ### Pattern 2: Weighted k-NN Classification
   ```clojure
   ;; Weight votes by inverse distance
   (defn weighted-knn-classify [test-features train-features train-labels k]
     (let [[idx dist] (nearest-neighbour test-features train-features 0 k AF_SSD)
           nearest-labels (index train-labels idx)
           ;; Weight by 1/(dist + epsilon)
           weights (/ 1.0 (+ dist 1e-10))
           ;; Weighted vote per class
           predictions (weighted-mode nearest-labels weights)]
       predictions))
   ```
   
   ### Pattern 3: Radius-based Neighbours
   ```clojure
   ;; Find all neighbours within radius r
   (defn radius-neighbours [query train radius]
     (let [;; Get many neighbours (upper bound)
           k-max 256
           [idx dist] (nearest-neighbour query train 0 k-max AF_SSD)
           ;; Filter by radius
           in-radius (< dist (* radius radius))
           valid-idx (where in-radius idx)
           valid-dist (where in-radius dist)]
       {:indices valid-idx
        :distances valid-dist}))
   ```
   
   ### Pattern 4: Batch Processing
   ```clojure
   ;; Process queries in batches for memory efficiency
   (defn batch-nearest-neighbour [queries train batch-size k dist-type]
     (let [n-queries (dim queries 1)
           n-batches (ceil (/ n-queries batch-size))]
       (loop [i 0
              all-idx []
              all-dist []]
         (if (>= i n-batches)
           {:indices (join all-idx 1)
            :distances (join all-dist 1)}
           (let [start (* i batch-size)
                 end (min (* (inc i) batch-size) n-queries)
                 batch (slice queries [start end])
                 [idx dist] (nearest-neighbour batch train 0 k dist-type)]
             (recur (inc i)
                    (conj all-idx idx)
                    (conj all-dist dist)))))))
   ```
   
   ## Type Support and Constraints
   
   ### For SAD and SSD distance types:
   - **Supported types**: f32, f64, s32, u32, s64, u64, s16, u16, s8, u8
   - **Output distances**: Same type as input
   - **Output indices**: Always uint (u32)
   
   ### For SHD (Hamming) distance type:
   - **Supported types**: u8, u16, u32, u64 (unsigned integers only)
   - **Output distances**: uint (u32) - bit count
   - **Output indices**: uint (u32)
   
   ### Constraints:
   - Query and train must have same type
   - Query and train must have same feature dimension
   - Both arrays must be 2D (dims[2] = dims[3] = 1)
   - dist_dim must be 0 or 1
   - n_dist must be > 0 and <= 256
   - n_dist must be <= number of training samples
   
   ## Performance Optimization Tips
   
   1. **Choose appropriate distance metric**:
      - SSD for most cases (standard k-NN)
      - SAD when outliers present or speed critical
      - SHD for binary descriptors (much faster)
   
   2. **Organize data for memory locality**:
      - dist_dim=0 for image features (channels separate)
      - dist_dim=1 for ML datasets (samples separate)
   
   3. **Use appropriate k**:
      - Small k (1-10): Very fast
      - Large k (100-256): 2-3× slower
      - If k > 256 needed, find 256 then post-process
   
   4. **Batch queries when possible**:
      - GPU processes all queries in parallel
      - More queries = better GPU utilization
      - Watch memory: N_q × N_t × sizeof(distance)
   
   5. **Precompute features**:
      - Extract/compute features once
      - Cache training features
      - Reuse for multiple query batches
   
   6. **Consider approximate methods for large scale**:
      - For N_t > 1M, consider approximate NN (not in ArrayFire)
      - Locality-sensitive hashing (LSH)
      - Product quantization
      - Tree-based methods (KD-tree, ball tree)
   
   ## Comparison with Alternatives
   
   ### vs Manual Distance + Sort:
   - **Nearest neighbour**: Optimized, uses topk
   - **Manual**: More flexible but slower
   - **Advantage NN**: Single function call, optimized kernels
   
   ### vs Approximate NN (ANN):
   - **Exact NN**: Guaranteed correct, O(N) query time
   - **ANN**: Approximate, O(log N) or O(1) query time
   - **Use exact when**: N < 100K, exact matches critical
   - **Use ANN when**: N > 1M, approximate ok
   
   ### vs Spatial Data Structures (KD-tree):
   - **GPU brute force**: O(N_q × N_t × d), highly parallel
   - **KD-tree (CPU)**: O(N_q × log(N_t) × d), serial
   - **GPU advantage**: For d < 20-30 and GPU available
   - **KD-tree advantage**: Very high dimension, CPU only
   
   ## Common Issues and Solutions
   
   ### Issue 1: Out of Memory
   **Symptom**: Crashes with large query or training sets
   **Solution**: Batch processing (see pattern above)
   ```clojure
   ;; Split queries into batches
   (batch-nearest-neighbour queries train 1000 k dist-type)
   ```
   
   ### Issue 2: Slow Performance
   **Symptom**: Takes longer than expected
   **Solutions**:
   - Check if GPU is being used (backend selection)
   - Reduce k if possible
   - Use SAD instead of SSD if acceptable
   - Use SHD for binary features
   - Ensure data is in correct format (not transposing unnecessarily)
   
   ### Issue 3: Incorrect Results
   **Symptom**: Unexpected nearest neighbours
   **Solutions**:
   - Verify dist_dim matches data layout
   - Check distance metric is appropriate
   - Normalize features if using Euclidean distance
   - For Hamming, ensure data is unsigned integer
   
   ### Issue 4: Type Mismatch Error
   **Symptom**: Error about incompatible types
   **Solution**: Ensure query and train have same type
   ```clojure
   ;; Cast to same type if needed
   (let [train-f32 (cast train f32)
         query-f32 (cast query f32)]
     (nearest-neighbour query-f32 train-f32 0 k AF_SSD))
   ```
   
   ## Mathematical Properties
   
   ### Distance Metric Properties
   
   For a valid distance metric d(x, y):
   1. **Non-negativity**: d(x, y) ≥ 0
   2. **Identity**: d(x, x) = 0
   3. **Symmetry**: d(x, y) = d(y, x)
   4. **Triangle inequality**: d(x, z) ≤ d(x, y) + d(y, z)
   
   - SAD, SSD (and Euclidean), Hamming all satisfy these
   - SSD is not a true metric (no triangle inequality) but monotonic with Euclidean
   
   ### Curse of Dimensionality
   
   As feature dimension d increases:
   - All points become approximately equidistant
   - Nearest and farthest neighbours have similar distances
   - k-NN performance degrades for d > 20-30
   
   **Mitigation**:
   - Dimensionality reduction (PCA, t-SNE)
   - Feature selection
   - Distance metric learning
   - Use approximate methods
   
   ## Error Handling
   
   The function returns an error code (af_err). Common errors:
   
   - **AF_ERR_ARG**: Invalid arguments
     * dist_dim not 0 or 1
     * n_dist not in range [1, 256]
     * dist_type not AF_SAD, AF_SSD, or AF_SHD
   
   - **AF_ERR_SIZE**: Dimension mismatch
     * Query and train feature dimensions don't match
     * Arrays not 2D (dims[2] or dims[3] != 1)
     * n_dist > number of training samples
   
   - **AF_ERR_TYPE**: Type mismatch or unsupported type
     * Query and train types differ
     * Hamming used with non-unsigned-integer type
   
   - **AF_ERR_MEM**: Out of memory
     * Query × train distance matrix too large
   
   ## See Also
   
   - af-hamming-matcher: Specialized wrapper for Hamming distance
   - af-match-template: Template matching (different problem)
   - af-topk: Used internally for k-selection
   - Machine learning libraries: For higher-level k-NN classifiers
   
   ## References
   
   - Cover, T., & Hart, P. (1967). \"Nearest neighbor pattern classification\"
   - Garcia, V., et al. (2008). \"Fast k nearest neighbor search using GPU\"
   - Lowe, D. (2004). \"Distinctive image features from scale-invariant keypoints\" (ratio test)"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; af_match_type enum constants
;; Used for specifying distance metric in nearest neighbour search
(def AF_SAD 0)  ; Sum of Absolute Differences (L1 / Manhattan distance)
(def AF_SSD 1)  ; Sum of Squared Differences (L2² / Squared Euclidean)
(def AF_SHD 2)  ; Hamming distance (for binary descriptors)

;; af_err af_nearest_neighbour(af_array *idx, af_array *dist, const af_array query, const af_array train, const dim_t dist_dim, const unsigned n_dist, const af_match_type dist_type)
(defcfn af-nearest-neighbour
  "Find k-nearest neighbours in a training set for each query point.
   
   Computes the k nearest neighbours from a training dataset for each point
   in a query dataset using the specified distance metric. This is the core
   function for k-NN classification, feature matching, and similarity search.
   
   Parameters:
   - idx: out pointer for indices array
     * Array of uint (u32) with shape [n_dist, n_queries]
     * idx[i, j] = index of i-th nearest training point to query j
     * Indices are 0-based offsets into training array
     * Sorted by distance (idx[0, :] = nearest, idx[k-1, :] = k-th nearest)
   
   - dist: out pointer for distances array
     * Array with shape [n_dist, n_queries]
     * Type: Same as input for SAD/SSD, uint for SHD on integers
     * dist[i, j] = distance from query j to its i-th nearest neighbour
     * Sorted in ascending order (dist[0, :] <= dist[1, :] <= ...)
     * Distances computed according to dist_type
   
   - query: query points array
     * Shape: If dist_dim=0: [n_features, n_queries]
     *        If dist_dim=1: [n_queries, n_features]
     * Must be 2D: dims[2] = dims[3] = 1
     * Type: f32, f64, s32, u32, s64, u64, s16, u16, s8, u8
     *       For SHD: u8, u16, u32, u64 only
     * Each column (dist_dim=0) or row (dist_dim=1) is one query point
   
   - train: training points array
     * Shape: If dist_dim=0: [n_features, n_train]
     *        If dist_dim=1: [n_train, n_features]
     * Must be 2D: dims[2] = dims[3] = 1
     * Type: Must match query array type
     * Each column (dist_dim=0) or row (dist_dim=1) is one training point
     * Must have same feature dimension as query
   
   - dist-dim: dimension containing feature coordinates
     * 0: Features along dim0 (column vectors)
       - Query: [n_features, n_queries]
       - Train: [n_features, n_train]
       - Common for: Computer vision, image features
     * 1: Features along dim1 (row vectors)
       - Query: [n_queries, n_features]
       - Train: [n_train, n_features]
       - Common for: Machine learning datasets
     * Must be same for both query and train
     * Only 0 or 1 are valid
   
   - n-dist: number of nearest neighbours to find (k in k-NN)
     * Range: 1 to 256 (inclusive)
     * Must be <= number of training points
     * Typical values: 1 (nearest), 3, 5, 10 (classification), 2 (ratio test)
     * Larger k: More computation but more robust classification
   
   - dist-type: distance metric to use (af_match_type enum)
     * AF_SAD (0): Sum of Absolute Differences
       - Formula: Σᵢ |qᵢ - tᵢ|
       - Also called L1 or Manhattan distance
       - Robust to outliers
       - Faster than SSD
     * AF_SSD (1): Sum of Squared Differences
       - Formula: Σᵢ (qᵢ - tᵢ)²
       - Squared Euclidean distance (omits sqrt)
       - Standard for k-NN
       - More weight to large differences
     * AF_SHD (2): Hamming distance
       - Formula: popcount(q ⊕ t)
       - For binary descriptors only
       - Counts differing bits
       - Very fast on GPU
       - Requires unsigned integer types
   
   Algorithm:
   1. Compute all pairwise distances between queries and training points
      - For each query q and training point t:
        * SAD: d = Σᵢ |q[i] - t[i]|
        * SSD: d = Σᵢ (q[i] - t[i])²
        * SHD: d = popcount(q ⊕ t)
   2. For each query, find k smallest distances using partial sort
   3. Return indices and distances of k-nearest neighbours
   
   Performance:
   - Complexity: O(N_q × N_t × d + N_q × N_t × log(k))
     * N_q: number of queries
     * N_t: number of training points
     * d: feature dimension
     * k: n_dist
   - GPU acceleration: 50-200× faster than CPU
   - Memory: Temporary distance matrix of size N_q × N_t
   - Typical timing (1000 queries, 10K train, 128 features, f32): ~10ms on GPU
   
   Type Support:
   - SAD/SSD: f32, f64, s32, u32, s64, u64, s16, u16, s8, u8
   - SHD: u8, u16, u32, u64 only (unsigned integers for binary data)
   - Output indices: Always uint (u32)
   - Output distances: Same as input for SAD/SSD, uint for SHD
   
   Example 1: k-NN Classification (k=5)
   ```clojure
   ;; Classify test samples based on 5 nearest training samples
   (let [train-features (create-array train-data [128 10000])  ; 128 features, 10K samples
         train-labels (create-array labels [10000])            ; Labels for training
         test-features (create-array test-data [128 1000])     ; 1000 test samples
         
         idx-ptr (mem/alloc-pointer ::mem/pointer)
         dist-ptr (mem/alloc-pointer ::mem/pointer)
         k 5
         
         ;; Find 5 nearest neighbours
         _ (af-nearest-neighbour idx-ptr dist-ptr test-features train-features 0 k AF_SSD)
         
         idx (mem/read-pointer idx-ptr ::mem/pointer)
         dist (mem/read-pointer dist-ptr ::mem/pointer)
         
         ;; Get labels of k-nearest neighbours
         nearest-labels (index train-labels idx)  ; Shape: [5, 1000]
         
         ;; Majority vote for each test sample
         predictions (mode nearest-labels 0)]
     
     predictions)  ; Shape: [1000] - predicted label for each test sample
   ```
   
   Example 2: Feature Matching with Ratio Test (Computer Vision)
   ```clojure
   ;; Match ORB binary descriptors between two images using ratio test
   (let [desc1 (extract-orb-descriptors img1)  ; [256, N1] u8 binary descriptors
         desc2 (extract-orb-descriptors img2)  ; [256, N2] u8 binary descriptors
         
         idx-ptr (mem/alloc-pointer ::mem/pointer)
         dist-ptr (mem/alloc-pointer ::mem/pointer)
         
         ;; Find 2 nearest neighbours for ratio test
         _ (af-nearest-neighbour idx-ptr dist-ptr desc1 desc2 0 2 AF_SHD)
         
         idx (mem/read-pointer idx-ptr ::mem/pointer)
         dist (mem/read-pointer dist-ptr ::mem/pointer)
         
         ;; Ratio test: accept match if dist1/dist2 < 0.8
         dist1 (index dist 0)  ; Distance to nearest
         dist2 (index dist 1)  ; Distance to 2nd nearest
         ratio (/ dist1 (+ dist2 1e-10))
         good-matches (< ratio 0.8)
         
         ;; Get indices of good matches
         idx1 (index idx 0)
         matched-indices (where good-matches idx1)]
     
     {:num-matches (sum good-matches)
      :matched-indices matched-indices
      :match-distances (where good-matches dist1)})
   ```
   
   Example 3: Image Retrieval (Find Similar Images)
   ```clojure
   ;; Find 10 most similar images in database
   (let [query-image (load-image \"query.jpg\")
         query-features (extract-features query-image)  ; [2048] feature vector
         query-array (reshape query-features [2048 1])  ; [2048, 1]
         
         db-features (load-database-features)  ; [2048, 100000] - 100K images
         
         idx-ptr (mem/alloc-pointer ::mem/pointer)
         dist-ptr (mem/alloc-pointer ::mem/pointer)
         k 10
         
         ;; Find 10 most similar images
         _ (af-nearest-neighbour idx-ptr dist-ptr query-array db-features 0 k AF_SSD)
         
         idx (mem/read-pointer idx-ptr ::mem/pointer)
         dist (mem/read-pointer dist-ptr ::mem/pointer)
         
         ;; Get image IDs and similarity scores
         similar-image-ids (index db-image-ids idx)
         similarity-scores (/ 1.0 (+ dist 1.0))]  ; Convert distance to similarity
     
     {:image-ids similar-image-ids
      :scores similarity-scores})
   ```
   
   Example 4: Anomaly Detection
   ```clojure
   ;; Detect anomalies by distance to nearest normal sample
   (let [normal-data (create-array normal-samples [64 10000])  ; 10K normal samples
         test-data (create-array test-samples [64 1000])       ; 1000 test samples
         
         idx-ptr (mem/alloc-pointer ::mem/pointer)
         dist-ptr (mem/alloc-pointer ::mem/pointer)
         
         ;; Find 1 nearest neighbour
         _ (af-nearest-neighbour idx-ptr dist-ptr test-data normal-data 0 1 AF_SSD)
         
         dist (mem/read-pointer dist-ptr ::mem/pointer)
         
         ;; Samples with large distance are anomalies
         threshold 100.0
         is-anomaly (> dist threshold)
         anomaly-scores (squeeze dist)]
     
     {:is-anomaly is-anomaly
      :anomaly-scores anomaly-scores
      :num-anomalies (sum is-anomaly)})
   ```
   
   Example 5: Clustering Assignment (k-means)
   ```clojure
   ;; Assign data points to nearest cluster centroid
   (let [data (create-array samples [128 50000])     ; 50K samples, 128 features
         centroids (create-array centers [128 10])   ; 10 cluster centers
         
         idx-ptr (mem/alloc-pointer ::mem/pointer)
         dist-ptr (mem/alloc-pointer ::mem/pointer)
         
         ;; Find nearest centroid for each point
         _ (af-nearest-neighbour idx-ptr dist-ptr data centroids 0 1 AF_SSD)
         
         cluster-assignments (mem/read-pointer idx-ptr ::mem/pointer)  ; [1, 50000]
         distances-to-centroid (mem/read-pointer dist-ptr ::mem/pointer)]
     
     {:assignments (squeeze cluster-assignments)
      :distances (squeeze distances-to-centroid)
      :inertia (sum distances-to-centroid)})  ; Sum of squared distances
   ```
   
   Example 6: Recommendation System
   ```clojure
   ;; Find similar users for recommendations
   (let [user-embeddings (compute-embeddings users)  ; [256, 10000] user feature vectors
         target-user-id 42
         target-embedding (slice user-embeddings [target-user-id (inc target-user-id)])
         target-reshaped (reshape target-embedding [256 1])
         
         idx-ptr (mem/alloc-pointer ::mem/pointer)
         dist-ptr (mem/alloc-pointer ::mem/pointer)
         k 20  ; Find 20 similar users
         
         ;; Find k most similar users
         _ (af-nearest-neighbour idx-ptr dist-ptr target-reshaped user-embeddings 0 k AF_SSD)
         
         idx (mem/read-pointer idx-ptr ::mem/pointer)
         dist (mem/read-pointer dist-ptr ::mem/pointer)
         
         ;; First match is self, skip it
         similar-user-ids (slice idx [1 k])
         similarities (/ 1.0 (+ (slice dist [1 k]) 1.0))
         
         ;; Get items from similar users
         recommended-items (get-items-from-users similar-user-ids)]
     
     {:similar-users similar-user-ids
      :similarities similarities
      :recommendations recommended-items})
   ```
   
   Example 7: Density Estimation
   ```clojure
   ;; Estimate local density using k-nearest distances
   (let [data (create-array samples [3 5000])  ; 5000 3D points
         
         idx-ptr (mem/alloc-pointer ::mem/pointer)
         dist-ptr (mem/alloc-pointer ::mem/pointer)
         k 10  ; Use 10-nearest for density
         
         ;; Find k nearest neighbours for each point (including self)
         _ (af-nearest-neighbour idx-ptr dist-ptr data data 0 k AF_SSD)
         
         dist (mem/read-pointer dist-ptr ::mem/pointer)
         
         ;; Average distance to k-nearest neighbours
         avg-dist (mean dist 0)  ; [5000]
         
         ;; Density inversely proportional to average distance
         density (/ 1.0 (+ avg-dist 1e-10))
         
         ;; Normalize to [0, 1]
         max-density (max density)
         normalized-density (/ density max-density)]
     
     {:density normalized-density
      :high-density-points (> normalized-density 0.8)})
   ```
   
   Common Use Cases:
   - **k-NN classification**: Supervised learning, pattern recognition
   - **Feature matching**: SIFT, ORB, BRIEF descriptor matching
   - **Image retrieval**: Content-based image search
   - **Anomaly detection**: Novelty detection, outlier detection
   - **Clustering**: k-means cluster assignment
   - **Recommendation**: Collaborative filtering
   - **Density estimation**: DBSCAN, mean-shift
   
   Common Patterns:
   
   Pattern 1: Weighted k-NN (distance-weighted voting)
   ```clojure
   ;; Weight neighbours by inverse distance
   (let [[idx dist] (nearest-neighbour query train 0 k AF_SSD)
         weights (/ 1.0 (+ dist 1e-10))
         nearest-labels (index train-labels idx)
         weighted-vote (weighted-mode nearest-labels weights)]
     weighted-vote)
   ```
   
   Pattern 2: Batch processing for large datasets
   ```clojure
   ;; Process in batches to avoid memory issues
   (defn batch-nn [queries train batch-size k]
     (let [n-queries (dim queries 1)
           results (for [start (range 0 n-queries batch-size)]
                     (let [end (min (+ start batch-size) n-queries)
                           batch (slice queries [start end])]
                       (nearest-neighbour batch train 0 k AF_SSD)))]
       (join results 1)))
   ```
   
   Pattern 3: Radius-based neighbours (using k as upper bound)
   ```clojure
   ;; Find all neighbours within radius
   (let [[idx dist] (nearest-neighbour query train 0 256 AF_SSD)
         within-radius (< dist (* radius radius))
         valid-idx (where within-radius idx)
         valid-dist (where within-radius dist)]
     [valid-idx valid-dist])
   ```
   
   When to use which distance:
   - **AF_SSD**: Default choice, k-NN classification, most ML tasks
   - **AF_SAD**: When outliers present, computational efficiency critical
   - **AF_SHD**: Binary descriptors (ORB, BRIEF, BRISK), very fast
   
   When NOT to use:
   - Very high dimension (d > 50): Consider approximate NN methods
   - Very large datasets (N > 1M): Consider LSH or tree-based methods
   - Need exact Euclidean: SSD is squared, use sqrt in post-processing if needed
   - Hamming for non-binary data: Use SAD or SSD instead
   
   Gotchas:
   - **Memory**: Distance matrix is N_q × N_t, watch for OOM
   - **Type mismatch**: Query and train must be same type
   - **Dimension mismatch**: Feature dimensions must match
   - **dist_dim confusion**: Must be consistent between query and train
   - **k too large**: Only k <= 256 supported
   - **Hamming types**: Only unsigned integers (u8, u16, u32, u64)
   - **Self-matches**: If query = train, first match is self (distance 0)
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-hamming-matcher: Convenience function for Hamming distance (calls this with AF_SHD)
   - af-match-template: Different problem (template matching in images)
   - af-topk: Internal function used for k-selection"
  "af_nearest_neighbour" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/long ::mem/int ::mem/int] ::mem/int)
