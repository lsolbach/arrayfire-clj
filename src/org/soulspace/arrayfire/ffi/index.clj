(ns org.soulspace.arrayfire.ffi.index
  "Bindings for the ArrayFire array indexing functions.
  
  Corresponds to src/api/c/index.cpp in ArrayFire."
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; Indexing functions

;; af_err af_index(af_array *result, const af_array in, const unsigned ndims, const af_seq* indices)
(defcfn af-index
  "Index an array using sequences.
  
  Parameters:
  result - pointer to output array
  in - input array to be indexed
  ndims - number of dimensions to index
  indices - pointer to array of af_seq sequences
  
  Returns:
  af_err error code"
  "af_index" [::mem/pointer ::mem/pointer ::mem/int ::mem/pointer] ::mem/int)

;; af_err af_lookup(af_array *out, const af_array in, const af_array indices, const unsigned dim)
(defcfn af-lookup
  "Lookup values in an array by indexing with another array.
  
  Parameters:
  out - pointer to output array
  in - input array to be queried
  indices - array of lookup indices
  dim - dimension along which to index
  
  Returns:
  af_err error code"
  "af_lookup" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_index_gen(af_array *out, const af_array in, const dim_t ndims, const af_index_t* indices)
(defcfn af-index-gen
  "Generalized indexing using af_index_t (can be sequences or arrays).
  
  Parameters:
  out - pointer to output array
  in - input array to be indexed
  ndims - number of dimensions to index
  indices - pointer to array of af_index_t objects
  
  Returns:
  af_err error code"
  "af_index_gen" [::mem/pointer ::mem/pointer ::mem/long ::mem/pointer] ::mem/int)

;; af_seq af_make_seq(double begin, double end, double step)
(defcfn af-make-seq
  "Create an af_seq sequence object.
  
  Parameters:
  begin - start value of sequence
  end - end value of sequence (inclusive)
  step - step size
  
  Returns:
  af_seq struct (returned by value)"
  "af_make_seq" [::mem/double ::mem/double ::mem/double] ::mem/pointer)

;; Indexer management functions

;; af_err af_create_indexers(af_index_t** indexers)
(defcfn af-create-indexers
  "Create an array of af_index_t indexers (4 elements for 4 dimensions).
  
  Parameters:
  indexers - pointer to pointer where indexer array will be allocated
  
  Returns:
  af_err error code"
  "af_create_indexers" [::mem/pointer] ::mem/int)

;; af_err af_set_array_indexer(af_index_t* indexer, const af_array idx, const dim_t dim)
(defcfn af-set-array-indexer
  "Set an af_array as the indexer for a specific dimension.
  
  Parameters:
  indexer - pointer to indexer array
  idx - af_array to use as indexer
  dim - dimension to set (0-3)
  
  Returns:
  af_err error code"
  "af_set_array_indexer" [::mem/pointer ::mem/pointer ::mem/long] ::mem/int)

;; af_err af_set_seq_indexer(af_index_t* indexer, const af_seq* idx, const dim_t dim, const bool is_batch)
(defcfn af-set-seq-indexer
  "Set an af_seq sequence as the indexer for a specific dimension.
  
  Parameters:
  indexer - pointer to indexer array
  idx - pointer to af_seq to use as indexer
  dim - dimension to set (0-3)
  is_batch - whether this is a batch indexing operation
  
  Returns:
  af_err error code"
  "af_set_seq_indexer" [::mem/pointer ::mem/pointer ::mem/long ::mem/int] ::mem/int)

;; af_err af_set_seq_param_indexer(af_index_t* indexer, const double begin, const double end, const double step, const dim_t dim, const bool is_batch)
(defcfn af-set-seq-param-indexer
  "Set a sequence indexer by providing begin, end, step parameters directly.
  
  Parameters:
  indexer - pointer to indexer array
  begin - start value of sequence
  end - end value of sequence (inclusive)
  step - step size
  dim - dimension to set (0-3)
  is_batch - whether this is a batch indexing operation
  
  Returns:
  af_err error code"
  "af_set_seq_param_indexer" [::mem/pointer ::mem/double ::mem/double ::mem/double ::mem/long ::mem/int] ::mem/int)

;; af_err af_release_indexers(af_index_t* indexers)
(defcfn af-release-indexers
  "Release memory allocated for indexer array.
  
  Parameters:
  indexers - pointer to indexer array to release
  
  Returns:
  af_err error code"
  "af_release_indexers" [::mem/pointer] ::mem/int)
