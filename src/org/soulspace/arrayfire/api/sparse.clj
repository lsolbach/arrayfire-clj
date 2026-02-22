(ns org.soulspace.arrayfire.api.sparse
  "Idiomatic Clojure sparse-matrix API for arrayfire-clj.

   Provides access to ArrayFire sparse matrix operations via keyword-based
   storage-format arguments and idiomatic naming. All functions must be called
   within a `with-arrayfire` region (see `api.core/with-arrayfire`).

   ## Storage formats

   Sparse matrices are parameterised by a storage format keyword:

   | Keyword  | AF constant           | Value | Description                    |
   |----------|-----------------------|-------|--------------------------------|
   | :dense   | AF_STORAGE_DENSE      |   0   | Dense (not truly sparse)       |
   | :csr     | AF_STORAGE_CSR        |   1   | Compressed Sparse Row          |
   | :csc     | AF_STORAGE_CSC        |   2   | Compressed Sparse Column       |
   | :coo     | AF_STORAGE_COO        |   3   | Coordinate / triplet list      |

   ## Quick start

   ```clojure
   (require '[org.soulspace.arrayfire.api.core   :as core]
            '[org.soulspace.arrayfire.api.sparse  :as sparse])

   ;; Create a 3×3 COO sparse matrix
   (core/with-arrayfire {:backend :cpu}
     (let [vals  (core/array [1.0 2.0 3.0] [3])
           rows  (core/array [0   1   2  ] [3] :s32)
           cols  (core/array [0   1   2  ] [3] :s32)
           sp    (sparse/create-sparse 3 3 vals rows cols :coo)
           dense (sparse/to-dense sp)]
       (core/->value dense)))
   ```"
  (:require [org.soulspace.arrayfire.integration.base.definitions :as defs]
            [org.soulspace.arrayfire.integration.unified-api.sparse :as sparse]
            [org.soulspace.arrayfire.api.core :refer [assert-within-arrayfire!]])
  (:import (org.soulspace.arrayfire.integration.base.resource AFArray)))

;;;
;;; Storage-format helpers
;;;

(defn- resolve-storage
  "Coerce `storage` to an af_storage integer.
   Accepts a keyword (:dense :csr :csc :coo) or a raw integer constant.
   Delegates to the `defs/storage-kw->const` map."
  [storage]
  (if (keyword? storage)
    (or (defs/storage-kw->const storage)
        (throw (ex-info (str "Unknown storage-format keyword: " storage
                             ". Valid keys: " (keys defs/storage-kw->const))
                        {:storage storage})))
    (int storage)))

;;;
;;; Sparse array creation
;;;

(defn create-sparse
  "Create a sparse matrix from component AFArrays.

   Parameters:
   - n-rows: Number of rows (integer)
   - n-cols: Number of columns (integer)
   - values: AFArray of non-zero values
   - row-idx: AFArray of row indices (COO/CSC) or row pointers (CSR)
   - col-idx: AFArray of column indices (COO/CSR) or column pointers (CSC)
   - storage: Storage format keyword (:coo :csr :csc) or integer constant.
              Defaults to :coo.

   Returns:
   AFArray representing the sparse matrix."
  ([n-rows n-cols ^AFArray values ^AFArray row-idx ^AFArray col-idx]
   (create-sparse n-rows n-cols values row-idx col-idx :coo))
  ([n-rows n-cols ^AFArray values ^AFArray row-idx ^AFArray col-idx storage]
   (assert-within-arrayfire! "create-sparse")
   (sparse/create n-rows n-cols values row-idx col-idx (resolve-storage storage))))

(defn from-dense
  "Create a sparse matrix from a dense matrix.

   Parameters:
   - in: Input dense matrix (AFArray)
   - storage: Storage format keyword (:coo :csr :csc) or integer constant.
              Defaults to :coo.

   Returns:
   AFArray representing the sparse matrix."
  ([^AFArray in]
   (from-dense in :coo))
  ([^AFArray in storage]
   (assert-within-arrayfire! "from-dense")
   (sparse/from-dense in (resolve-storage storage))))

;;;
;;; Format conversion
;;;

(defn to-dense
  "Convert a sparse matrix to a dense matrix.

   Parameters:
   - in: Input sparse matrix (AFArray)

   Returns:
   AFArray in dense format."
  [^AFArray in]
  (assert-within-arrayfire! "to-dense")
  (sparse/to-dense in))

(defn convert-sparse
  "Convert a sparse matrix to a different storage format.

   Parameters:
   - in: Input sparse matrix (AFArray)
   - storage: Destination storage format keyword (:dense :coo :csr :csc) or
              integer constant.

   Returns:
   AFArray in the new storage format."
  [^AFArray in storage]
  (assert-within-arrayfire! "convert-sparse")
  (sparse/convert-to in (resolve-storage storage)))

;;;
;;; Sparse matrix inspection
;;;

(defn sparse-values
  "Extract the non-zero values from a sparse matrix.

   Parameters:
   - in: Input sparse matrix (AFArray)

   Returns:
   AFArray of non-zero values."
  [^AFArray in]
  (assert-within-arrayfire! "sparse-values")
  (sparse/values in))

(defn sparse-row-indices
  "Extract the row index array from a sparse matrix.

   The semantic depends on storage format:
   - COO / CSC: row indices (length = nnz)
   - CSR: row pointers (length = n-rows + 1)

   Parameters:
   - in: Input sparse matrix (AFArray)

   Returns:
   AFArray of row indices or row pointers."
  [^AFArray in]
  (assert-within-arrayfire! "sparse-row-indices")
  (sparse/row-indices in))

(defn sparse-col-indices
  "Extract the column index array from a sparse matrix.

   The semantic depends on storage format:
   - COO / CSR: column indices (length = nnz)
   - CSC: column pointers (length = n-cols + 1)

   Parameters:
   - in: Input sparse matrix (AFArray)

   Returns:
   AFArray of column indices or column pointers."
  [^AFArray in]
  (assert-within-arrayfire! "sparse-col-indices")
  (sparse/col-indices in))

(defn sparse-nnz
  "Return the number of non-zero elements in a sparse matrix.

   Parameters:
   - in: Input sparse matrix (AFArray)

   Returns:
   Long integer count of non-zero elements."
  [^AFArray in]
  (assert-within-arrayfire! "sparse-nnz")
  (sparse/nnz in))

(defn sparse-storage
  "Return the storage format of a sparse matrix as a keyword.

   Returns one of :dense, :csr, :csc, :coo.

   Parameters:
   - in: Input sparse matrix (AFArray)

   Returns:
   Storage format keyword."
  [^AFArray in]
  (assert-within-arrayfire! "sparse-storage")
  (get defs/storage-const->kw (sparse/storage-format in)))

(comment
  ;; REPL experiments
  (require '[org.soulspace.arrayfire.api.core   :as core]
           '[org.soulspace.arrayfire.api.sparse  :as sparse])

  ;; Basic roundtrip: dense → sparse → dense
  (core/with-arrayfire {:backend :cpu}
    (let [a     (core/array [1.0 0.0 0.0
                             0.0 2.0 0.0
                             0.0 0.0 3.0] [3 3])
          sp    (sparse/from-dense a :coo)
          nz    (sparse/sparse-nnz sp)
          fmt   (sparse/sparse-storage sp)
          back  (core/->value (sparse/to-dense sp))]
      {:nnz nz :storage fmt :dense back}))

  ;
  )
