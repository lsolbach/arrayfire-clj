(ns org.soulspace.arrayfire.integration.unified-api.sparse
  "High-level Clojure wrapper for ArrayFire sparse matrix operations.

  Sparse matrices store only non-zero elements, providing significant memory
  savings and computational efficiency for matrices with many zeros. This is
  essential for large-scale scientific computing, graph algorithms, and
  machine learning applications.

  ## Quick Start

  ```clojure
  (require '[org.soulspace.arrayfire.integration.sparse :as sparse])
  (require '[org.soulspace.arrayfire.integration.array :as array])

  ;; Create sparse matrix from dense
  (let [dense (array/constant 5.0 [1000 1000])
        sparse-mat (sparse/from-dense dense sparse/COO)]
    (println \"Non-zeros:\" (sparse/nnz sparse-mat)))

  ;; Create from COO format
  (let [values (array/from-vec [1.0 2.0 3.0] [3])
        rows (array/from-vec [0 1 2] [3])
        cols (array/from-vec [0 1 2] [3])
        sparse-mat (sparse/create 3 3 values rows cols sparse/COO)]
    (sparse/to-dense sparse-mat))

  ;; Convert between formats
  (let [coo-mat (sparse/from-dense dense-mat sparse/COO)
        csr-mat (sparse/convert-to coo-mat sparse/CSR)]
    (sparse/storage-format csr-mat)) ;; => 1 (CSR)
  ```

  ## Storage Formats

  ArrayFire supports multiple sparse matrix storage formats, each optimized
  for different access patterns and operations:

  ### COO (Coordinate List) - AF_STORAGE_COO (0)
  Stores triplets (row, col, value) for each non-zero element:
  ```
  values: [v1, v2, v3, ...]
  rows:   [r1, r2, r3, ...]
  cols:   [c1, c2, c3, ...]
  ```
  - Most flexible format
  - Easy to construct and modify
  - Efficient for matrix assembly
  - Storage: 3×nnz (three arrays)
  - Best for: Initial construction, modifications, general purpose

  ### CSR (Compressed Sparse Row) - AF_STORAGE_CSR (1)
  Row-oriented compressed format:
  ```
  values:  [v1, v2, v3, ...]        (nnz elements)
  colIdx:  [c1, c2, c3, ...]        (nnz elements)
  rowPtr:  [p0, p1, p2, ..., pn]    (nRows+1 elements)
  ```
  - rowPtr[i] points to start of row i
  - rowPtr[i+1] - rowPtr[i] = non-zeros in row i
  - Efficient for row-wise operations
  - Excellent for sparse matrix-vector multiplication (SpMV)
  - Storage: 2×nnz + (nRows+1)
  - Best for: SpMV, row slicing, iterative solvers

  ### CSC (Compressed Sparse Column) - AF_STORAGE_CSC (2)
  Column-oriented compressed format (similar to CSR but column-wise):
  ```
  values:  [v1, v2, v3, ...]        (nnz elements)
  rowIdx:  [r1, r2, r3, ...]        (nnz elements)
  colPtr:  [p0, p1, p2, ..., pm]    (nCols+1 elements)
  ```
  - Efficient for column-wise operations
  - Good for sparse matrix transposition
  - Storage: 2×nnz + (nCols+1)
  - Best for: Column slicing, transposition
  - Note: Limited support in ArrayFire

  ## Performance Characteristics

  Storage comparison for m×n matrix with k non-zeros:
  - Dense: O(m×n) memory
  - COO: O(3k) memory
  - CSR/CSC: O(2k + m) or O(2k + n) memory

  Typical operations:
  - Matrix-vector multiply (SpMV): O(k) vs O(m×n) dense
  - Format conversion: O(k) to O(k log k)
  - Element access: O(1) to O(log k) depending on format

  Memory savings: 10-1000× for typical sparse matrices (density < 0.01)
  GPU acceleration: 10-100× speedup for sparse operations

  ## Supported Types

  Sparse matrices support floating-point types only:
  - Single precision: f32, c32 (complex float)
  - Double precision: f64, c64 (complex double)

  Integer types are not supported for values (but are used for indices).

  ## Common Use Cases

  ### 1. Scientific Computing
  - Finite element methods
  - Discretized PDEs
  - Large-scale linear systems

  ### 2. Graph Algorithms
  - Adjacency matrices
  - PageRank
  - Network analysis

  ### 3. Machine Learning
  - Feature matrices
  - Document-term matrices
  - Recommender systems

  ### 4. Image Processing
  - Sparse signal representations
  - Compressed sensing

  ## Best Practices

  ### Format Selection
  - Use COO for matrix construction and general operations
  - Convert to CSR for iterative algorithms and SpMV
  - Convert to CSC for column-oriented algorithms
  - Minimize format conversions (they have overhead)

  ### Memory Management
  - Prefer sparse for density < 0.1
  - Use dense for density > 0.5
  - Convert to sparse after construction for memory savings

  ### Performance Tips
  - Keep data on GPU to avoid transfers
  - Batch operations to minimize kernel launches
  - Choose format based on dominant operations
  - Preallocate when possible

  See also:
  - ArrayFire sparse documentation: https://arrayfire.org/docs/group__sparse__mat.htm
  - Linear algebra operations for sparse×dense multiplication
  - BLAS operations for dense matrices"
  (:require [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.c-api.sparse :as sparse]
            [org.soulspace.arrayfire.integration.base.jvm-integration :as jvm])
  (:import (org.soulspace.arrayfire.integration.base.jvm_integration AFArray)))

;;;
;;; Storage Format Constants
;;;

(def COO
  "Coordinate list (COO) format - stores (row, col, value) triplets.
   
   Most flexible format, easy to construct and modify.
   Best for: matrix assembly, general purpose operations.
   Storage: 3×nnz elements."
  0)

(def CSR
  "Compressed Sparse Row (CSR) format - row-oriented compressed storage.
   
   Efficient for row-wise operations and sparse matrix-vector multiplication.
   Best for: iterative solvers, row slicing, SpMV operations.
   Storage: 2×nnz + (nRows+1) elements."
  1)

(def CSC
  "Compressed Sparse Column (CSC) format - column-oriented compressed storage.
   
   Efficient for column-wise operations and matrix transposition.
   Best for: column slicing, transposition.
   Storage: 2×nnz + (nCols+1) elements.
   Note: Limited support in ArrayFire."
  2)

(def DENSE
  "Dense storage format (should not occur for sparse arrays).
   
   Used only in conversion operations to indicate dense destination."
  3)

;;;
;;; Sparse Matrix Creation
;;;

(defn create
  "Create a sparse matrix from arrays of non-zero values and their indices.
   
   This function creates a sparse matrix in the specified storage format from
   coordinate arrays. The arrays must have compatible dimensions.
   
   Parameters:
   - n-rows: Number of rows in the sparse matrix (integer)
   - n-cols: Number of columns in the sparse matrix (integer)
   - values: AFArray of non-zero values
   - row-idx: AFArray of row indices for COO, or row pointers for CSR
   - col-idx: AFArray of column indices
   - storage-type: Storage format (COO, CSR, or CSC constant, default COO)
   
   Returns:
   AFArray representing the sparse matrix
   
   For COO format:
   - values[i], row-idx[i], col-idx[i] form the i-th non-zero triplet
   - All three arrays must have the same length (nnz)
   
   For CSR format:
   - values and col-idx have length nnz
   - row-idx (row pointers) has length (n-rows + 1)
   - row-idx[i] points to the start of row i in values/col-idx
   
   Example (COO format):
   ```clojure
   (let [values (array/from-vec [1.0 2.0 3.0] [3])
         rows (array/from-vec [0 1 2] [3])
         cols (array/from-vec [0 1 2] [3])
         sparse-mat (create 3 3 values rows cols COO)]
     ;; Creates 3×3 diagonal matrix with values 1, 2, 3
     sparse-mat)
   ```
   
   Example (CSR format):
   ```clojure
   ;; Matrix: [1 0 2]
   ;;         [0 3 0]
   ;;         [4 0 5]
   (let [values (array/from-vec [1.0 2.0 3.0 4.0 5.0] [5])
         col-idx (array/from-vec [0 2 1 0 2] [5])
         row-ptr (array/from-vec [0 2 3 5] [4]) ;; n-rows+1
         sparse-mat (create 3 3 values row-ptr col-idx CSR)]
     sparse-mat)
   ```
   
   See also:
   - from-dense: Create sparse from dense matrix
   - from-ptr: Create from raw pointers"
  ([n-rows n-cols ^AFArray values ^AFArray row-idx ^AFArray col-idx]
   (create n-rows n-cols values row-idx col-idx COO))
  ([n-rows n-cols ^AFArray values ^AFArray row-idx ^AFArray col-idx storage-type]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (sparse/af-create-sparse-array
                  out
                  (long n-rows)
                  (long n-cols)
                  (jvm/af-handle values)
                  (jvm/af-handle row-idx)
                  (jvm/af-handle col-idx)
                  (int storage-type))
                 "af-create-sparse-array")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn from-ptr
  "Create a sparse matrix from raw memory pointers.
   
   This is a lower-level function for creating sparse matrices directly from
   memory buffers. Use this when interfacing with native code or for performance-
   critical operations where arrays are already in memory.
   
   Parameters:
   - n-rows: Number of rows (integer)
   - n-cols: Number of columns (integer)
   - nnz: Number of non-zero elements (integer)
   - values: Memory pointer to values array
   - row-idx: Memory pointer to row indices/pointers
   - col-idx: Memory pointer to column indices
   - dtype: Data type of values (AF_DTYPE_F32, AF_DTYPE_F64, etc.)
   - storage-type: Storage format (COO, CSR, or CSC constant, default COO)
   - source: Memory source (AF_SOURCE_HOST or AF_SOURCE_DEVICE, default host)
   
   Returns:
   AFArray representing the sparse matrix
   
   Example:
   ```clojure
   (let [values-ptr (mem/allocate (* nnz 8)) ;; 8 bytes per f64
         rows-ptr (mem/allocate (* nnz 4))   ;; 4 bytes per i32
         cols-ptr (mem/allocate (* nnz 4))
         ;; ... fill pointers with data ...
         sparse-mat (from-ptr 100 100 1000 
                              values-ptr rows-ptr cols-ptr
                              AF_DTYPE_F64 COO 0)]
     sparse-mat)
   ```
   
   See also:
   - create: Higher-level creation from AFArrays
   - from-dense: Create from dense matrix"
  ([n-rows n-cols nnz values row-idx col-idx dtype storage-type]
   (from-ptr n-rows n-cols nnz values row-idx col-idx dtype storage-type 0))
  ([n-rows n-cols nnz values row-idx col-idx dtype storage-type source]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (sparse/af-create-sparse-array-from-ptr
                  out
                  (long n-rows)
                  (long n-cols)
                  (long nnz)
                  values
                  row-idx
                  col-idx
                  (int dtype)
                  (int storage-type)
                  (int source))
                 "af-create-sparse-array-from-ptr")
     (jvm/af-array-new (jvm/deref-af-array out)))))

(defn from-dense
  "Create a sparse matrix from a dense matrix.
   
   Converts a dense matrix to sparse format by identifying and storing only
   the non-zero elements. Elements with values close to machine epsilon are
   treated as zero.
   
   This is useful for converting dense matrices with many zeros to sparse
   format for memory savings and computational efficiency.
   
   Parameters:
   - in: Input dense matrix (AFArray)
   - storage-type: Storage format (COO, CSR, or CSC constant, default COO)
   
   Returns:
   AFArray representing the sparse matrix
   
   Example:
   ```clojure
   ;; Create dense matrix with some zeros
   (let [dense (array/constant 0.0 [1000 1000])
         ;; Set some non-zero elements
         _ (array/set-value! dense 0 0 5.0)
         _ (array/set-value! dense 1 1 10.0)
         ;; Convert to sparse (huge memory savings!)
         sparse-mat (from-dense dense COO)]
     (println \"Non-zeros:\" (nnz sparse-mat))
     (println \"Density:\" (/ (nnz sparse-mat) 1000000.0))
     sparse-mat)
   ```
   
   Example (convert computed result):
   ```clojure
   ;; Compute something that produces sparse result
   (let [a (array/random [1000 1000])
         b (array/lt a 0.1) ;; Threshold - sparse boolean
         sparse-b (from-dense b COO)]
     sparse-b)
   ```
   
   See also:
   - to-dense: Convert sparse to dense
   - create: Create from coordinate arrays"
  ([^AFArray in]
   (from-dense in COO))
  ([^AFArray in storage-type]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (sparse/af-create-sparse-array-from-dense
                  out
                  (jvm/af-handle in)
                  (int storage-type))
                 "af-create-sparse-array-from-dense")
     (jvm/af-array-new (jvm/deref-af-array out)))))

;;;
;;; Format Conversion
;;;

(defn convert-to
  "Convert a sparse matrix from one storage format to another.
   
   Changes the internal storage representation of the sparse matrix.
   This is useful for optimizing operations:
   - Convert to CSR for row-oriented operations and SpMV
   - Convert to CSC for column-oriented operations
   - Convert to COO for general operations
   
   Format conversions have computational overhead, so minimize them in
   performance-critical code.
   
   Parameters:
   - in: Input sparse matrix (AFArray)
   - dest-storage: Destination storage format (COO, CSR, CSC, or DENSE constant)
   
   Returns:
   AFArray in the new storage format
   
   Example (optimize for SpMV):
   ```clojure
   (let [coo-mat (from-dense dense-mat COO)
         ;; Convert to CSR for efficient matrix-vector multiplication
         csr-mat (convert-to coo-mat CSR)
         vector (array/random [n 1])
         ;; Now SpMV will be faster with CSR format
         result (sparse-matmul csr-mat vector)]
     result)
   ```
   
   Example (format pipeline):
   ```clojure
   ;; Construct in COO (easy to build)
   (let [coo-mat (create n n values rows cols COO)
         ;; Convert to CSR for computation
         csr-mat (convert-to coo-mat CSR)
         ;; ... perform operations ...
         ;; Convert back to COO for modifications
         coo-mat2 (convert-to csr-mat COO)]
     coo-mat2)
   ```
   
   See also:
   - to-dense: Convert to dense matrix
   - storage-format: Query current format"
  [^AFArray in dest-storage]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (sparse/af-sparse-convert-to
                 out
                 (jvm/af-handle in)
                 (int dest-storage))
                "af-sparse-convert-to")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn to-dense
  "Convert a sparse matrix to dense format.
   
   Reconstructs a full dense matrix from the sparse representation by
   filling in all the zero elements. The resulting matrix will have
   dimensions n-rows × n-cols.
   
   Use this when you need to perform operations that require dense format,
   or for final output/visualization.
   
   Warning: This can significantly increase memory usage for large matrices.
   For a 10000×10000 matrix with 0.01% density:
   - Sparse: ~8 KB (COO format)
   - Dense: ~800 MB
   
   Parameters:
   - in: Input sparse matrix (AFArray)
   
   Returns:
   AFArray in dense format
   
   Example:
   ```clojure
   (let [sparse-mat (from-dense dense-mat COO)
         ;; Do sparse operations...
         ;; Convert back to dense for output
         result-dense (to-dense sparse-mat)]
     result-dense)
   ```
   
   Example (selective conversion):
   ```clojure
   ;; Only convert small results to dense
   (let [sparse-mat (create n n values rows cols COO)]
     (if (< n 1000)
       (to-dense sparse-mat)  ;; Small enough for dense
       sparse-mat))           ;; Keep sparse for large matrices
   ```
   
   See also:
   - from-dense: Convert dense to sparse
   - convert-to: Convert between sparse formats"
  [^AFArray in]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (sparse/af-sparse-to-dense
                 out
                 (jvm/af-handle in))
                "af-sparse-to-dense")
    (jvm/af-array-new (jvm/deref-af-array out))))

;;;
;;; Sparse Matrix Inspection
;;;

(defn info
  "Get complete information about a sparse matrix's internal structure.
   
   Returns all the component arrays (values, row indices, column indices)
   and the storage format. This is useful for:
   - Inspecting sparse matrix contents
   - Extracting components for custom algorithms
   - Debugging sparse matrix operations
   
   Parameters:
   - in: Input sparse matrix (AFArray)
   
   Returns:
   A map containing:
   - :values - AFArray of non-zero values
   - :row-idx - AFArray of row indices (COO) or row pointers (CSR)
   - :col-idx - AFArray of column indices
   - :storage - Storage format (0=COO, 1=CSR, 2=CSC)
   
   Example:
   ```clojure
   (let [sparse-mat (create 3 3 values rows cols COO)
         {:keys [values row-idx col-idx storage]} (info sparse-mat)]
     (println \"Format:\" (case storage
                           0 \"COO\"
                           1 \"CSR\"
                           2 \"CSC\"))
     (println \"Non-zeros:\" (array/elements values))
     {:values values :row-idx row-idx :col-idx col-idx})
   ```
   
   Example (extract for custom algorithm):
   ```clojure
   (defn custom-sparse-op [sparse-mat]
     (let [{:keys [values row-idx col-idx storage]} (info sparse-mat)]
       ;; Implement custom operation on components
       (case storage
         0 (process-coo values row-idx col-idx)
         1 (process-csr values row-idx col-idx)
         (throw (ex-info \"Unsupported format\" {:storage storage})))))
   ```
   
   See also:
   - values: Get only the values array
   - row-indices: Get only the row index array
   - col-indices: Get only the column index array
   - storage-format: Get only the storage format"
  [^AFArray in]
  (let [values-ptr (jvm/native-af-array-pointer)
        row-idx-ptr (jvm/native-af-array-pointer)
        col-idx-ptr (jvm/native-af-array-pointer)
        storage-buf (mem/alloc-instance ::mem/int)]
    (jvm/check! (sparse/af-sparse-get-info
                 values-ptr
                 row-idx-ptr
                 col-idx-ptr
                 storage-buf
                 (jvm/af-handle in))
                "af-sparse-get-info")
    {:values (jvm/af-array-new (jvm/deref-af-array values-ptr))
     :row-idx (jvm/af-array-new (jvm/deref-af-array row-idx-ptr))
     :col-idx (jvm/af-array-new (jvm/deref-af-array col-idx-ptr))
     :storage (mem/read-int storage-buf)}))

(defn values
  "Extract the non-zero values from a sparse matrix.
   
   Returns an AFArray containing only the non-zero values stored in the
   sparse representation. The length of this array is equal to nnz.
   
   Parameters:
   - in: Input sparse matrix (AFArray)
   
   Returns:
   AFArray of non-zero values
   
   Example:
   ```clojure
   (let [sparse-mat (create 3 3 vals rows cols COO)
         val-array (values sparse-mat)]
     (println \"Values:\" (array/to-vec val-array)))
   ```
   
   See also:
   - row-indices: Get row index array
   - col-indices: Get column index array
   - info: Get all components"
  [^AFArray in]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (sparse/af-sparse-get-values
                 out
                 (jvm/af-handle in))
                "af-sparse-get-values")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn row-indices
  "Extract the row index array from a sparse matrix.
   
   The meaning depends on storage format:
   - COO: Array of row indices (length = nnz)
   - CSR: Array of row pointers (length = n-rows + 1)
   - CSC: Array of row indices (length = nnz)
   
   Parameters:
   - in: Input sparse matrix (AFArray)
   
   Returns:
   AFArray of row indices or pointers
   
   Example (COO format):
   ```clojure
   (let [sparse-mat (create n n vals rows cols COO)
         row-array (row-indices sparse-mat)]
     ;; row-array[i] is the row index of the i-th non-zero
     (array/to-vec row-array))
   ```
   
   Example (CSR format):
   ```clojure
   (let [sparse-mat (convert-to coo-mat CSR)
         row-ptrs (row-indices sparse-mat)]
     ;; row-ptrs[i] points to start of row i
     ;; Number of non-zeros in row i = row-ptrs[i+1] - row-ptrs[i]
     (array/to-vec row-ptrs))
   ```
   
   See also:
   - col-indices: Get column index array
   - values: Get values array
   - info: Get all components"
  [^AFArray in]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (sparse/af-sparse-get-row-idx
                 out
                 (jvm/af-handle in))
                "af-sparse-get-row-idx")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn col-indices
  "Extract the column index array from a sparse matrix.
   
   The meaning depends on storage format:
   - COO: Array of column indices (length = nnz)
   - CSR: Array of column indices (length = nnz)
   - CSC: Array of column pointers (length = n-cols + 1)
   
   Parameters:
   - in: Input sparse matrix (AFArray)
   
   Returns:
   AFArray of column indices or pointers
   
   Example:
   ```clojure
   (let [sparse-mat (create n n vals rows cols COO)
         col-array (col-indices sparse-mat)]
     ;; col-array[i] is the column index of the i-th non-zero
     (array/to-vec col-array))
   ```
   
   See also:
   - row-indices: Get row index array
   - values: Get values array
   - info: Get all components"
  [^AFArray in]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (sparse/af-sparse-get-col-idx
                 out
                 (jvm/af-handle in))
                "af-sparse-get-col-idx")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn nnz
  "Get the number of non-zero elements in a sparse matrix.
   
   Returns the count of non-zero elements stored in the sparse representation.
   This is useful for:
   - Computing matrix density
   - Allocating buffers for operations
   - Analyzing sparsity patterns
   
   Parameters:
   - in: Input sparse matrix (AFArray)
   
   Returns:
   Integer count of non-zero elements
   
   Example:
   ```clojure
   (let [sparse-mat (from-dense dense-mat COO)
         nz-count (nnz sparse-mat)
         dims (array/dims sparse-mat)
         density (/ nz-count (* (first dims) (second dims)))]
     (println \"Non-zeros:\" nz-count)
     (println \"Density:\" density))
   ```
   
   Example (sparsity analysis):
   ```clojure
   (defn analyze-sparsity [sparse-mat]
     (let [nz (nnz sparse-mat)
           [rows cols] (array/dims sparse-mat)
           total (* rows cols)
           density (/ nz (double total))
           sparsity (- 1.0 density)]
       {:nnz nz
        :total total
        :density density
        :sparsity sparsity
        :memory-ratio (/ nz total)}))
   ```
   
   See also:
   - info: Get all sparse matrix information
   - storage-format: Get storage format"
  [^AFArray in]
  (let [nnz-buf (mem/alloc-instance ::mem/long)]
    (jvm/check! (sparse/af-sparse-get-nnz
                 nnz-buf
                 (jvm/af-handle in))
                "af-sparse-get-nnz")
    (mem/read-long nnz-buf)))

(defn storage-format
  "Get the storage format of a sparse matrix.
   
   Returns an integer indicating which storage format is currently used:
   - 0: COO (Coordinate list)
   - 1: CSR (Compressed Sparse Row)
   - 2: CSC (Compressed Sparse Column)
   - 3: Dense (should not occur for sparse arrays)
   
   Parameters:
   - in: Input sparse matrix (AFArray)
   
   Returns:
   Integer storage format code (0=COO, 1=CSR, 2=CSC, 3=Dense)
   
   Example:
   ```clojure
   (let [sparse-mat (create n n vals rows cols COO)
         fmt (storage-format sparse-mat)]
     (case fmt
       0 (println \"COO format\")
       1 (println \"CSR format\")
       2 (println \"CSC format\")
       3 (println \"Dense format\")))
   ```
   
   Example (conditional conversion):
   ```clojure
   (defn ensure-csr [sparse-mat]
     (if (= (storage-format sparse-mat) CSR)
       sparse-mat
       (convert-to sparse-mat CSR)))
   ```
   
   See also:
   - convert-to: Convert between formats
   - info: Get all sparse matrix information"
  [^AFArray in]
  (let [storage-buf (mem/alloc-instance ::mem/int)]
    (jvm/check! (sparse/af-sparse-get-storage
                 storage-buf
                 (jvm/af-handle in))
                "af-sparse-get-storage")
    (mem/read-int storage-buf)))

