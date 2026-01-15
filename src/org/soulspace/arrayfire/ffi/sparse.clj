(ns org.soulspace.arrayfire.ffi.sparse
  "ArrayFire FFI bindings for sparse matrix operations.

  Sparse matrices store only non-zero elements, providing significant memory
  savings and computational efficiency for matrices with many zeros. This is
  essential for large-scale scientific computing, graph algorithms, and
  machine learning applications.

  ## Mathematical Foundation

  A sparse matrix is one where most elements are zero. For an m×n matrix:
  - Density: fraction of non-zero elements = nnz / (m×n)
  - Sparse: density < 0.1 (typically < 0.01)
  - Dense: density > 0.5

  Memory savings for sparse matrix with k non-zero elements:
  - Dense storage: O(m×n) memory
  - Sparse storage: O(k) memory where k << m×n

  ## Storage Formats

  ArrayFire supports multiple sparse matrix storage formats:

  ### 1. COO (Coordinate List)
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

  ### 2. CSR (Compressed Sparse Row)
  Row-oriented compressed format:
  ```
  values:  [v1, v2, v3, ...]        (nnz elements)
  colIdx:  [c1, c2, c3, ...]        (nnz elements)
  rowPtr:  [p0, p1, p2, ..., pn]    (nRows+1 elements)
  ```
  - rowPtr[i] points to start of row i in values/colIdx
  - rowPtr[i+1] - rowPtr[i] = number of non-zeros in row i
  - Efficient for row-wise operations
  - Good for sparse matrix-vector multiplication
  - Storage: 2×nnz + (nRows+1)

  ### 3. CSC (Compressed Sparse Column)
  Column-oriented compressed format (similar to CSR but column-wise):
  ```
  values:  [v1, v2, v3, ...]        (nnz elements)
  rowIdx:  [r1, r2, r3, ...]        (nnz elements)
  colPtr:  [p0, p1, p2, ..., pm]    (nCols+1 elements)
  ```
  - Efficient for column-wise operations
  - Good for sparse matrix transposition
  - Storage: 2×nnz + (nCols+1)

  Note: CSC is currently not fully supported in ArrayFire.

  ## Format Conversion

  Converting between formats:
  - COO ↔ CSR: Common for different operations
  - CSR ↔ Dense: For mixed sparse/dense algorithms
  - Dense → Sparse: Automatic zero detection
  - Sparse → Dense: Reconstruction with zeros

  ## Computational Complexity

  Storage comparison for m×n matrix with k non-zeros:
  - Dense: O(m×n)
  - COO: O(3k)
  - CSR/CSC: O(2k + m) or O(2k + n)

  Typical operations:
  - Matrix-vector multiply (SpMV): O(k) vs O(m×n) dense
  - Matrix-matrix multiply: Depends on sparsity patterns
  - Format conversion: O(k) to O(k log k)
  - Element access: O(1) to O(log k) depending on format

  ## Supported Types

  Sparse matrices support floating-point types only:
  - Single precision: f32, c32 (complex float)
  - Double precision: f64, c64 (complex double)

  Integer types are not supported for sparse matrix values (but used for indices).

  ## Performance Notes

  - GPU acceleration provides 10-100× speedup for sparse operations
  - Memory savings: 10-1000× for typical sparse matrices
  - Best speedup when density < 0.01
  - Choose storage format based on access patterns:
    * COO: General purpose, matrix construction
    * CSR: Row-wise access, SpMV
    * CSC: Column-wise access, transposition
  - Format conversion has overhead; minimize conversions

  ## Applications

  - Scientific computing: Finite element methods, discretized PDEs
  - Graph algorithms: Adjacency matrices, PageRank
  - Machine learning: Feature matrices, recommender systems
  - Natural language processing: Document-term matrices
  - Computer graphics: Mesh processing, deformation
  - Circuit simulation: Network analysis
  - Social networks: Relationship graphs
  - Image processing: Sparse signal representations

  ## Sparsity Patterns

  Common sparse matrix patterns:
  - Diagonal: Diagonal elements only
  - Tridiagonal: Main diagonal plus adjacent diagonals
  - Block diagonal: Blocks along diagonal
  - Banded: Elements within band around diagonal
  - Random sparse: Randomly distributed non-zeros
  - Structured: Domain-specific patterns (graphs, grids)

  See also:
  - Linear algebra operations (af_sparse_matmul for sparse×dense)
  - BLAS operations for dense matrices
  - Solve functions for sparse linear systems"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem :refer [deref-ptr]])
  (:refer-clojure :exclude [deref]))

;;
;; Sparse Matrix Creation
;;

(defcfn af-create-sparse-array
  "Create a sparse array from arrays of values and indices.

  Constructs a sparse matrix from three arrays containing non-zero values,
  row indices, and column indices. This is the primary way to create sparse
  matrices from existing data.

  Parameters:
  - out: Output sparse array
  - n-rows: Number of rows in the sparse matrix
  - n-cols: Number of columns in the sparse matrix
  - values: Array containing non-zero values (floating-point type)
  - row-idx: Array containing row indices (s32 type)
  - col-idx: Array containing column indices (s32 type)
  - stype: Storage format (af_storage enum)
    * AF_STORAGE_COO (0): Coordinate format
    * AF_STORAGE_CSR (1): Compressed Sparse Row
    * AF_STORAGE_CSC (2): Compressed Sparse Column (limited support)

  Returns:
  Error code indicating success or failure.

  Index Array Requirements:
  - COO format: rowIdx, colIdx, and values must have same length (nnz)
  - CSR format: rowIdx length = nRows+1, colIdx and values length = nnz
  - CSC format: colIdx length = nCols+1, rowIdx and values length = nnz

  Example (COO format):
  ```clojure
  ;; Create sparse matrix from triplets:
  ;; [1.0 0   0  ]
  ;; [0   2.5 0  ]
  ;; [0   0   3.7]
  (let [values (af-constant [3] [1.0 2.5 3.7] :f32)
        rows (af-constant [3] [0 1 2] :s32)
        cols (af-constant [3] [0 1 2] :s32)
        sparse (mem/alloc-instance ::mem/pointer)]
    (af-create-sparse-array sparse 3 3 values rows cols 0) ;; AF_STORAGE_COO
    ;; sparse now contains 3×3 diagonal matrix
    )
  ```

  Example (CSR format):
  ```clojure
  ;; Same matrix in CSR format
  ;; values: [1.0, 2.5, 3.7]
  ;; colIdx: [0, 1, 2]
  ;; rowPtr: [0, 1, 2, 3] (row 0 starts at 0, row 1 at 1, row 2 at 2, end at 3)
  (let [values (af-constant [3] [1.0 2.5 3.7] :f32)
        row-ptr (af-constant [4] [0 1 2 3] :s32) ;; nRows+1
        col-idx (af-constant [3] [0 1 2] :s32)
        sparse (mem/alloc-instance ::mem/pointer)]
    (af-create-sparse-array sparse 3 3 values row-ptr col-idx 1) ;; AF_STORAGE_CSR
    )
  ```

  Example (general sparse matrix):
  ```clojure
  ;; Create matrix:
  ;; [5.0 0   2.0]
  ;; [0   8.0 0  ]
  ;; [1.0 0   3.0]
  (let [values (af-constant [6] [5.0 2.0 8.0 1.0 3.0] :f64)
        rows (af-constant [6] [0 0 1 2 2] :s32)
        cols (af-constant [6] [0 2 1 0 2] :s32)
        sparse (mem/alloc-instance ::mem/pointer)]
    (af-create-sparse-array sparse 3 3 values rows cols 0) ;; COO
    )
  ```

  Type Requirements:
  - values: f32, f64, c32, or c64
  - rowIdx and colIdx: s32 (32-bit signed integer)

  Memory Management:
  - Input arrays (values, rowIdx, colIdx) are shallow copied
  - No deep copy of data is performed
  - Original arrays can be released after creation

  Notes:
  - Indices are 0-based
  - For COO: Order of elements doesn't matter (can be unsorted)
  - For CSR/CSC: Elements must be sorted by row/column
  - Duplicate indices may be summed (implementation-dependent)

  See also:
  - af_create_sparse_array (ArrayFire C API)
  - af-create-sparse-array-from-ptr: Create from raw pointers
  - af-create-sparse-array-from-dense: Convert dense to sparse"
  "af_create_sparse_array" [::mem/pointer ::mem/long ::mem/long ::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

(defcfn af-create-sparse-array-from-ptr
  "Create a sparse array from raw host or device pointers.

  Constructs a sparse matrix directly from C-style pointers to values and
  indices. This provides fine-grained control over memory management and
  enables zero-copy creation from external data.

  Parameters:
  - out: Output sparse array
  - n-rows: Number of rows in the sparse matrix
  - n-cols: Number of columns in the sparse matrix
  - n-nz: Number of non-zero elements
  - values: Pointer to non-zero values
  - row-idx: Pointer to row indices (s32)
  - col-idx: Pointer to column indices (s32)
  - type: Data type of values (af_dtype enum)
  - stype: Storage format (af_storage enum)
  - source: Source of pointers (af_source enum)
    * AF_HOST (0): Pointers are host memory
    * AF_DEVICE (1): Pointers are device memory

  Returns:
  Error code indicating success or failure.

  Example (from host memory):
  ```clojure
  ;; Create sparse matrix from host arrays
  (let [values-ptr (get-host-pointer float-array)
        rows-ptr (get-host-pointer int-array)
        cols-ptr (get-host-pointer int-array)
        sparse (mem/alloc-instance ::mem/pointer)]
    (af-create-sparse-array-from-ptr 
      sparse 100 100 500           ;; 100×100 matrix, 500 non-zeros
      values-ptr rows-ptr cols-ptr
      0 0 0)                        ;; f32, COO, host
    )
  ```

  Example (from device memory):
  ```clojure
  ;; Create sparse matrix from device pointers (zero-copy)
  (let [dev-values (allocate-device-memory (* nnz 4)) ;; 4 bytes for f32
        dev-rows (allocate-device-memory (* nnz 4))
        dev-cols (allocate-device-memory (* nnz 4))
        sparse (mem/alloc-instance ::mem/pointer)]
    ;; Copy data to device...
    (af-create-sparse-array-from-ptr
      sparse n-rows n-cols nnz
      dev-values dev-rows dev-cols
      0 0 1)                        ;; f32, COO, device
    )
  ```

  Memory Ownership:
  - AF_HOST: ArrayFire copies data to device (host memory can be freed)
  - AF_DEVICE: ArrayFire takes ownership of device pointers (no copy)

  Deep Copy vs Reference:
  - Same rules as regular af_array creation
  - AF_HOST source: Always creates device copy
  - AF_DEVICE source: Takes ownership, no copy

  Type Support:
  - type parameter: f32, f64, c32, c64
  - Indices are always s32

  Storage Format Requirements:
  Same as af-create-sparse-array:
  - COO: All three arrays have length nnz
  - CSR: rowIdx length = nRows+1, others length = nnz
  - CSC: colIdx length = nCols+1, others length = nnz

  Use Cases:
  - Interfacing with C libraries
  - Zero-copy from device memory
  - Custom memory management
  - Large-scale data import

  Notes:
  - Pointers must remain valid during function call
  - For AF_DEVICE source, ArrayFire manages memory after call
  - Empty sparse matrix (nnz=0) is valid

  See also:
  - af_create_sparse_array_from_ptr (ArrayFire C API)
  - af-create-sparse-array: Create from af_array
  - af-create-sparse-array-from-dense: Convert dense array"
  "af_create_sparse_array_from_ptr" [::mem/pointer ::mem/long ::mem/long ::mem/long ::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/int ::mem/int] ::mem/int)

(defcfn af-create-sparse-array-from-dense
  "Convert a dense array to sparse format.

  Automatically identifies non-zero elements in a dense matrix and creates
  an equivalent sparse representation. Zero elements are detected and excluded.

  Parameters:
  - out: Output sparse array
  - dense: Input dense array (must be 2D)
  - stype: Storage format for output (af_storage enum)
    * AF_STORAGE_COO (0): Coordinate format
    * AF_STORAGE_CSR (1): Compressed Sparse Row
    * AF_STORAGE_CSC (2): Compressed Sparse Column (limited support)

  Returns:
  Error code indicating success or failure.

  Example (basic conversion):
  ```clojure
  ;; Convert dense matrix to sparse
  (let [dense (af-constant [3 3] [[1.0 0 0]
                                   [0 2.0 0]
                                   [0 0 3.0]] :f32)
        sparse (mem/alloc-instance ::mem/pointer)]
    (af-create-sparse-array-from-dense sparse dense 0) ;; COO format
    ;; Result: 3 non-zero elements stored efficiently
    )
  ```

  Example (large sparse matrix):
  ```clojure
  ;; Create large sparse matrix from dense
  (let [dense (af-randn [1000 1000] :f64)
        ;; Zero out 95% of elements
        mask (af-lt (af-randu [1000 1000] :f64) 0.05)
        sparse-dense (af-mul dense mask)
        sparse (mem/alloc-instance ::mem/pointer)]
    (af-create-sparse-array-from-dense sparse sparse-dense 1) ;; CSR
    ;; Stores ~50,000 non-zeros instead of 1M elements
    ;; Memory savings: ~95%
    )
  ```

  Example (graph adjacency matrix):
  ```clojure
  ;; Convert adjacency matrix to sparse for graph algorithms
  (let [adj-matrix (create-adjacency-matrix nodes edges)
        sparse-graph (mem/alloc-instance ::mem/pointer)]
    (af-create-sparse-array-from-dense sparse-graph adj-matrix 1) ;; CSR
    ;; CSR is efficient for graph traversal (row = node, cols = neighbors)
    )
  ```

  Zero Detection:
  - Elements exactly equal to 0.0 are excluded
  - Very small values (near-zero but not exactly zero) are kept
  - Use thresholding before conversion if needed:
    ```clojure
    (let [thresholded (af-where (af-gt (af-abs dense) threshold) dense 0)]
      (af-create-sparse-array-from-dense sparse thresholded format))
    ```

  Type Requirements:
  - Input must be 2D matrix (ndims = 2)
  - Type must be floating-point: f32, f64, c32, c64
  - Output inherits input type

  Performance:
  - Scans entire matrix to find non-zeros: O(m×n)
  - Efficient GPU parallel scan
  - Creates compact representation automatically

  Memory Efficiency:
  - Input: m×n elements (all stored)
  - Output: k elements where k = number of non-zeros
  - Savings: (1 - k/(m×n)) × 100%

  Use Cases:
  - Converting results of dense operations to sparse
  - Initializing sparse matrices from formulas
  - Importing data from dense file formats
  - Hybrid dense/sparse algorithms

  Notes:
  - Input must be exactly 2D (no higher dimensions)
  - Zero detection is exact (0.0), not epsilon-based
  - Output format affects subsequent operation efficiency

  See also:
  - af_create_sparse_array_from_dense (ArrayFire C API)
  - af-sparse-to-dense: Reverse conversion
  - af-create-sparse-array: Create from indices/values"
  "af_create_sparse_array_from_dense" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;;
;; Sparse Format Conversion
;;

(defcfn af-sparse-convert-to
  "Convert sparse array between different storage formats.

  Converts a sparse matrix from one storage format to another, or from dense
  to sparse. This enables choosing the optimal format for different operations.

  Parameters:
  - out: Output array in destination format
  - in: Input sparse or dense array
  - dest-storage: Destination storage format (af_storage enum)
    * AF_STORAGE_DENSE (3): Convert to dense
    * AF_STORAGE_COO (0): Coordinate format
    * AF_STORAGE_CSR (1): Compressed Sparse Row
    * AF_STORAGE_CSC (2): Compressed Sparse Column (limited support)

  Returns:
  Error code indicating success or failure.

  Example (COO to CSR):
  ```clojure
  ;; Convert COO to CSR for efficient SpMV
  (let [coo-matrix (create-sparse-coo ...)
        csr-matrix (mem/alloc-instance ::mem/pointer)]
    (af-sparse-convert-to csr-matrix coo-matrix 1) ;; to CSR
    ;; Now use csr-matrix for matrix-vector multiplications
    )
  ```

  Example (sparse to dense):
  ```clojure
  ;; Convert sparse to dense for dense operations
  (let [sparse-matrix (create-sparse ...)
        dense-matrix (mem/alloc-instance ::mem/pointer)]
    (af-sparse-convert-to dense-matrix sparse-matrix 3) ;; to DENSE
    ;; dense-matrix is now a regular af_array with all zeros filled in
    )
  ```

  Example (dense to sparse):
  ```clojure
  ;; Convert dense to sparse (alternative to af-create-sparse-array-from-dense)
  (let [dense (af-randn [100 100] :f32)
        sparse (mem/alloc-instance ::mem/pointer)]
    (af-sparse-convert-to sparse dense 0) ;; to COO
    )
  ```

  Example (format optimization):
  ```clojure
  ;; Choose format based on operation
  (defn optimize-for-operation [sparse-matrix op]
    (let [optimal-format (case op
                           :row-access 1    ;; CSR
                           :col-access 2    ;; CSC
                           :assembly 0      ;; COO
                           :dense-ops 3)    ;; DENSE
          optimized (mem/alloc-instance ::mem/pointer)]
      (af-sparse-convert-to optimized sparse-matrix optimal-format)
      optimized))
  ```

  Format Conversion Paths:
  - COO ↔ CSR: Supported
  - COO ↔ Dense: Supported
  - CSR ↔ Dense: Supported
  - CSC ↔ Others: Limited support

  Same Format Optimization:
  If input already in destination format:
  - Returns reference to input (no copy)
  - Use af-retain-array semantics

  Performance:
  - Conversion complexity: O(k) to O(k log k) where k = nnz
  - COO → CSR requires sorting: O(k log k)
  - CSR → COO: O(k)
  - Sparse ↔ Dense: O(m×n)

  Type Preservation:
  - Output has same element type as input
  - Only storage format changes

  Limitations:
  - CSC format has limited support
  - No direct CSC conversion (convert via COO)
  - Input must be valid sparse or dense array

  Use Cases:
  - Optimizing for specific operations
  - Preparing data for algorithms with format requirements
  - Converting computation results
  - Interfacing between libraries

  Notes:
  - Prefer native format when possible (avoid conversions)
  - Conversion has overhead; batch operations when possible
  - Dense conversion fills in all zeros (memory intensive)

  See also:
  - af_sparse_convert_to (ArrayFire C API)
  - af-sparse-to-dense: Specific sparse→dense conversion
  - af-create-sparse-array-from-dense: Specific dense→sparse"
  "af_sparse_convert_to" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

(defcfn af-sparse-to-dense
  "Convert a sparse array to dense format.

  Reconstructs a full dense matrix from sparse representation by filling in
  all zero elements. This is the inverse of sparse creation from dense.

  Parameters:
  - out: Output dense array with all elements
  - in: Input sparse array

  Returns:
  Error code indicating success or failure.

  Example (basic conversion):
  ```clojure
  ;; Convert sparse to dense
  (let [sparse (create-sparse-matrix ...)
        dense (mem/alloc-instance ::mem/pointer)]
    (af-sparse-to-dense dense sparse)
    ;; dense is now a regular af_array with all zeros filled
    )
  ```

  Example (visualization):
  ```clojure
  ;; Convert to dense for display/visualization
  (defn display-sparse-matrix [sparse]
    (let [dense (mem/alloc-instance ::mem/pointer)]
      (af-sparse-to-dense dense sparse)
      (print-array dense)
      (af-release-array (mem/read-pointer dense ::mem/pointer))))
  ```

  Example (mixed operations):
  ```clojure
  ;; Perform dense operation on sparse matrix
  (let [sparse (create-sparse ...)
        dense (mem/alloc-instance ::mem/pointer)
        result (mem/alloc-instance ::mem/pointer)]
    (af-sparse-to-dense dense sparse)
    ;; Now can use dense matrix operations
    (af-matmul result dense other-dense ...)
    )
  ```

  Memory Impact:
  For m×n sparse matrix with k non-zeros:
  - Sparse memory: O(k) elements
  - Dense memory: O(m×n) elements
  - Memory increase: (m×n / k)×

  Example memory usage:
  - 10,000×10,000 matrix, 0.1% density (10K non-zeros)
  - Sparse: ~10K elements (~40 KB for f32)
  - Dense: 100M elements (400 MB for f32)
  - Increase: 10,000×

  Type Preservation:
  - Output has same type as input sparse matrix
  - Zero elements are exact 0.0 (or 0.0+0.0i for complex)

  Performance:
  - Complexity: O(m×n) to allocate and fill dense matrix
  - GPU parallel fill operation
  - Can be expensive for large matrices

  Use Cases:
  - Visualization and inspection
  - Interfacing with dense-only algorithms
  - Small matrices where sparse overhead not worth it
  - Final result extraction
  - Debugging and verification

  Alternatives to Consider:
  - If only reading few elements: Use sparse accessors
  - If performing vector products: Stay in sparse format
  - If mixing with dense: Use sparse×dense operations

  Notes:
  - Can trigger out-of-memory for very large sparse matrices
  - Consider staying in sparse format when possible
  - Dense conversion loses sparsity information

  See also:
  - af_sparse_to_dense (ArrayFire C API)
  - af-sparse-convert-to: General conversion function
  - af-create-sparse-array-from-dense: Reverse operation"
  "af_sparse_to_dense" [::mem/pointer ::mem/pointer] ::mem/int)

;;
;; Sparse Array Information
;;

(defcfn af-sparse-get-info
  "Get all components of a sparse array.

  Retrieves the internal representation components of a sparse matrix:
  values array, index arrays, and storage format. This is a convenience
  function that calls all individual getters.

  Parameters:
  - values: Output array of non-zero values (can be NULL)
  - rows: Output array of row indices (can be NULL)
  - cols: Output array of column indices (can be NULL)
  - stype: Output storage format (can be NULL)
  - in: Input sparse array

  Returns:
  Error code indicating success or failure.

  Example (get all info):
  ```clojure
  ;; Extract all components
  (let [sparse (create-sparse ...)
        values (mem/alloc-instance ::mem/pointer)
        rows (mem/alloc-instance ::mem/pointer)
        cols (mem/alloc-instance ::mem/pointer)
        stype (mem/alloc-instance ::mem/int)]
    (af-sparse-get-info values rows cols stype sparse)
    ;; Now have access to all internal arrays
    )
  ```

  Example (selective retrieval):
  ```clojure
  ;; Only get values and storage type
  (let [sparse (create-sparse ...)
        values (mem/alloc-instance ::mem/pointer)
        stype (mem/alloc-instance ::mem/int)]
    (af-sparse-get-info values nil nil stype sparse)
    ;; rows and cols not retrieved (nil/NULL)
    )
  ```

  Example (inspect sparse matrix):
  ```clojure
  ;; Inspect structure of sparse matrix
  (defn inspect-sparse [sparse]
    (let [vals (mem/alloc-instance ::mem/pointer)
          rows (mem/alloc-instance ::mem/pointer)
          cols (mem/alloc-instance ::mem/pointer)
          fmt (mem/alloc-instance ::mem/int)]
      (af-sparse-get-info vals rows cols fmt sparse)
      {:values (mem/read-pointer vals ::mem/pointer)
       :rows (mem/read-pointer rows ::mem/pointer)
       :cols (mem/read-pointer cols ::mem/pointer)
       :format (mem/read-int fmt)
       :nnz (af-get-elements vals)}))
  ```

  NULL Parameters:
  Any output parameter can be NULL/nil:
  - NULL parameters are skipped (not retrieved)
  - Efficient when only subset of info needed
  - No error if NULL

  Individual Getters:
  This function is equivalent to calling:
  - af-sparse-get-values for values
  - af-sparse-get-row-idx for rows
  - af-sparse-get-col-idx for cols
  - af-sparse-get-storage for stype

  Use Cases:
  - Inspecting sparse matrix structure
  - Exporting sparse data to other formats
  - Debugging and verification
  - Custom sparse algorithms

  Notes:
  - Returned arrays are references (not copies)
  - Modifying returned arrays affects sparse matrix
  - Use af-retain-array if keeping references

  See also:
  - af_sparse_get_info (ArrayFire C API)
  - af-sparse-get-values: Get values only
  - af-sparse-get-row-idx: Get row indices only
  - af-sparse-get-col-idx: Get column indices only
  - af-sparse-get-storage: Get storage format only"
  "af_sparse_get_info" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-sparse-get-values
  "Get the non-zero values array from a sparse matrix.

  Extracts the array containing all non-zero element values from the
  sparse representation.

  Parameters:
  - out: Output array containing non-zero values
  - in: Input sparse array

  Returns:
  Error code indicating success or failure.

  Example:
  ```clojure
  ;; Extract values for processing
  (let [sparse (create-sparse ...)
        values (mem/alloc-instance ::mem/pointer)]
    (af-sparse-get-values values sparse)
    ;; Can now operate on values array directly
    (let [vals-array (mem/read-pointer values ::mem/pointer)]
      (af-sum vals-array ...)))
  ```

  See also:
  - af_sparse_get_values (ArrayFire C API)
  - af-sparse-get-info: Get all components"
  "af_sparse_get_values" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-sparse-get-row-idx
  "Get the row indices array from a sparse matrix.

  Extracts the row index array from the sparse representation. The
  interpretation depends on storage format (COO vs CSR).

  Parameters:
  - out: Output array containing row indices
  - in: Input sparse array

  Returns:
  Error code indicating success or failure.

  Example:
  ```clojure
  ;; Get row indices
  (let [sparse (create-sparse ...)
        rows (mem/alloc-instance ::mem/pointer)]
    (af-sparse-get-row-idx rows sparse)
    ;; rows contains row information based on format
    )
  ```

  Format-Specific Meaning:
  - COO: Array of length nnz with row index for each non-zero
  - CSR: Array of length nRows+1 with row pointers
  - CSC: Array of length nnz with row index for each non-zero

  See also:
  - af_sparse_get_row_idx (ArrayFire C API)
  - af-sparse-get-col-idx: Get column indices"
  "af_sparse_get_row_idx" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-sparse-get-col-idx
  "Get the column indices array from a sparse matrix.

  Extracts the column index array from the sparse representation. The
  interpretation depends on storage format (COO vs CSR).

  Parameters:
  - out: Output array containing column indices
  - in: Input sparse array

  Returns:
  Error code indicating success or failure.

  Example:
  ```clojure
  ;; Get column indices
  (let [sparse (create-sparse ...)
        cols (mem/alloc-instance ::mem/pointer)]
    (af-sparse-get-col-idx cols sparse)
    ;; cols contains column information
    )
  ```

  Format-Specific Meaning:
  - COO: Array of length nnz with column index for each non-zero
  - CSR: Array of length nnz with column index for each non-zero
  - CSC: Array of length nCols+1 with column pointers

  See also:
  - af_sparse_get_col_idx (ArrayFire C API)
  - af-sparse-get-row-idx: Get row indices"
  "af_sparse_get_col_idx" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-sparse-get-nnz
  "Get the number of non-zero elements in a sparse matrix.

  Returns the count of non-zero elements stored in the sparse representation.

  Parameters:
  - out: Output variable for non-zero count
  - in: Input sparse array

  Returns:
  Error code indicating success or failure.

  Example:
  ```clojure
  ;; Get sparsity information
  (let [sparse (create-sparse ...)
        nnz (mem/alloc-instance ::mem/long)]
    (af-sparse-get-nnz nnz sparse)
    (let [count (mem/read-long nnz)]
      (println \"Non-zeros:\" count)))
  ```

  Example (compute density):
  ```clojure
  ;; Calculate matrix density
  (defn sparse-density [sparse]
    (let [nnz-out (mem/alloc-instance ::mem/long)
          dims (af-get-dims sparse)]
      (af-sparse-get-nnz nnz-out sparse)
      (/ (mem/read-long nnz-out)
         (* (first dims) (second dims)))))
  ```

  See also:
  - af_sparse_get_nnz (ArrayFire C API)
  - af-sparse-get-info: Get all components"
  "af_sparse_get_nnz" [::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-sparse-get-storage
  "Get the storage format of a sparse matrix.

  Returns the storage format type used by the sparse array.

  Parameters:
  - out: Output variable for storage format (af_storage enum)
  - in: Input sparse array

  Returns:
  Error code indicating success or failure.

  Example:
  ```clojure
  ;; Check storage format
  (let [sparse (create-sparse ...)
        fmt (mem/alloc-instance ::mem/int)]
    (af-sparse-get-storage fmt sparse)
    (case (mem/read-int fmt)
      0 (println \"COO format\")
      1 (println \"CSR format\")
      2 (println \"CSC format\")
      3 (println \"Dense format\")))
  ```

  Storage Format Values:
  - 0: AF_STORAGE_COO (Coordinate)
  - 1: AF_STORAGE_CSR (Compressed Sparse Row)
  - 2: AF_STORAGE_CSC (Compressed Sparse Column)
  - 3: AF_STORAGE_DENSE (should not occur for sparse arrays)

  See also:
  - af_sparse_get_storage (ArrayFire C API)
  - af-sparse-convert-to: Convert between formats"
  "af_sparse_get_storage" [::mem/pointer ::mem/pointer] ::mem/int)
