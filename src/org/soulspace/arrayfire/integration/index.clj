(ns org.soulspace.arrayfire.integration.index
  "Integration of the ArrayFire indexing FFI bindings with error handling
   and resource management on the JVM.
   
   This namespace provides high-level functions for array indexing and slicing:
   
   - Basic indexing: index, lookup
   - Generalized indexing: index-gen with af_index_t
   - Assignment operations: assign-seq, assign-gen
   - Sequence utilities: make-seq
   - Indexer management: create-indexers, set-array-indexer, set-seq-indexer, release-indexers
   
   ArrayFire indexing supports:
   - Sequence-based indexing (start:end:step)
   - Array-based indexing (fancy indexing)
   - Mixed indexing (sequences and arrays)
   - Multi-dimensional indexing
   
   All functions work with AFArray instances and follow ArrayFire's memory management.
   
   ## Memory Management
   
   ArrayFire indexing functions (af_index, af_lookup, af_index_gen) return
   NEW af_array* handles. Even though these operations may create views that
   share underlying GPU memory with the input array, the returned handle is a
   distinct C++ object with its own reference tracking via std::shared_ptr.
   
   Therefore, we use jvm/af-array-new (not jvm/af-array-retained) because:
   1. ArrayFire's C++ implementation calls getHandle(createSubArray(...))
   2. getHandle creates a new Array<T>* via copy constructor
   3. The copy constructor (defaulted) copies the std::shared_ptr<cl::Buffer>
   4. Copying shared_ptr automatically increments its internal refcount
   5. From JVM's perspective, we receive a new handle with proper ownership
   
   This applies to all indexing and assignment operations in this namespace."
  (:require [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.index :as index-ffi]
            [org.soulspace.arrayfire.ffi.assign :as assign-ffi]
            [org.soulspace.arrayfire.integration.jvm-integration :as jvm])
  (:import (org.soulspace.arrayfire.integration.jvm_integration AFArray)
           (java.lang.foreign ValueLayout)))

;;;
;;; Sequence Utilities
;;;

(defn make-seq
  "Create an ArrayFire sequence (af_seq) for indexing.
   
   A sequence represents a range with optional step: begin:end:step
   
   Parameters:
   - begin: Start index (double)
   - end: End index (double), inclusive
   - step: Step size (double), default 1.0
   
   Returns:
   MemorySegment containing af_seq structure
   
   Examples:
   ```clojure
   ;; Select all elements
   (make-seq 0 -1 1)  ; 0:end:1
   
   ;; Select first 10 elements
   (make-seq 0 9 1)   ; 0:9:1
   
   ;; Select every other element
   (make-seq 0 -1 2)  ; 0:end:2
   
   ;; Reverse order
   (make-seq -1 0 -1) ; end:0:-1
   ```"
  ([begin end]
   (make-seq begin end 1.0))
  ([begin end step]
   (index-ffi/af-make-seq (double begin) (double end) (double step))))

;;;
;;; Basic Indexing
;;;

(defn index
  "Index an array using sequences.
   
   Extract a subarray using sequence-based indexing (like MATLAB/NumPy slicing).
   
   Parameters:
   - in: Input array (AFArray)
   - seqs: Vector of sequences (MemorySegments from make-seq)
   
   Returns:
   Indexed subarray (AFArray)
   
   Examples:
   ```clojure
   ;; Select rows 0-9, all columns
   (let [s0 (make-seq 0 9 1)
         s1 (make-seq 0 -1 1)]
     (index arr [s0 s1]))
   
   ;; Select every other row and column
   (let [s0 (make-seq 0 -1 2)
         s1 (make-seq 0 -1 2)]
     (index arr [s0 s1]))
   ```"
  [^AFArray in seqs]
  (let [out (jvm/native-af-array-pointer)
        ndims (count seqs)
        ;; Create array of af_seq pointers
        seqs-array (mem/alloc (* ndims 8))] ; Assuming 8 bytes per pointer
    ;; Write sequence pointers to array
    (doseq [[i seq-ptr] (map-indexed vector seqs)]
      (mem/write-long seqs-array (* i 8) (.address seq-ptr)))
    (jvm/check! (index-ffi/af-index out (jvm/af-handle in) (int ndims) seqs-array)
                "af-index")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn lookup
  "Lookup values in an array by indexing with another array (fancy indexing).
   
   Extracts elements from 'in' at positions specified by 'indices'.
   
   Parameters:
   - in: Input array to query (AFArray)
   - indices: Array of indices (AFArray), integer type
   - dim: Dimension along which to index (int), default 0
   
   Returns:
   Array with looked-up values (AFArray)
   
   Examples:
   ```clojure
   ;; Extract specific rows
   (let [idx (create-array [0 2 5 7])]
     (lookup data-array idx 0))
   
   ;; Extract specific columns
   (let [idx (create-array [1 3 5])]
     (lookup data-array idx 1))
   ```"
  ([^AFArray in ^AFArray indices]
   (lookup in indices 0))
  ([^AFArray in ^AFArray indices dim]
   (let [out (jvm/native-af-array-pointer)]
     (jvm/check! (index-ffi/af-lookup out (jvm/af-handle in) (jvm/af-handle indices) (int dim))
                 "af-lookup")
     (jvm/af-array-new (jvm/deref-af-array out)))))

;;;
;;; Generalized Indexing
;;;

(defn index-gen
  "Generalized indexing using af_index_t indexers.
   
   Most flexible indexing function - supports mixing sequences and arrays.
   Requires creating indexers with create-indexers and setting them.
   
   Parameters:
   - in: Input array (AFArray)
   - indexers: Pointer to af_index_t array (MemorySegment)
   - ndims: Number of dimensions being indexed (long)
   
   Returns:
   Indexed subarray (AFArray)
   
   Example:
   ```clojure
   (let [indexers (create-indexers)
         seq0 (make-seq 0 9 1)]
     (set-seq-indexer! indexers seq0 0 false)
     (set-seq-indexer! indexers (make-seq 0 -1 1) 1 false)
     (let [result (index-gen arr indexers 2)]
       (release-indexers! indexers)
       result))
   ```"
  [^AFArray in indexers ndims]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (index-ffi/af-index-gen out (jvm/af-handle in) (long ndims) indexers)
                "af-index-gen")
    (jvm/af-array-new (jvm/deref-af-array out))))

;;;
;;; Assignment Operations
;;;

(defn assign-seq
  "Assign values to a subarray specified by sequences.
   
   Creates a new array with values from 'rhs' assigned to the region
   specified by sequences in 'lhs'.
   
   Parameters:
   - lhs: Left-hand side array (AFArray)
   - seqs: Vector of sequences defining the region
   - rhs: Right-hand side values to assign (AFArray)
   
   Returns:
   New array with assignment applied (AFArray)
   
   Examples:
   ```clojure
   ;; Assign zeros to first 10 rows
   (let [s0 (make-seq 0 9 1)
         s1 (make-seq 0 -1 1)
         zeros (constant 0.0 [10 cols])]
     (assign-seq arr [s0 s1] zeros))
   ```"
  [^AFArray lhs seqs ^AFArray rhs]
  (let [out (jvm/native-af-array-pointer)
        ndims (count seqs)
        seqs-array (mem/alloc (* ndims 8))]
    (doseq [[i seq-ptr] (map-indexed vector seqs)]
      (mem/write-long seqs-array (* i 8) (.address seq-ptr)))
    (jvm/check! (assign-ffi/af-assign-seq out (jvm/af-handle lhs) (int ndims) 
                                          seqs-array (jvm/af-handle rhs))
                "af-assign-seq")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn assign-gen
  "Generalized assignment using af_index_t indexers.
   
   Most flexible assignment - supports mixing sequences and arrays.
   
   Parameters:
   - lhs: Left-hand side array (AFArray)
   - indexers: Pointer to af_index_t array (MemorySegment)
   - ndims: Number of dimensions being indexed (long)
   - rhs: Right-hand side values to assign (AFArray)
   
   Returns:
   New array with assignment applied (AFArray)
   
   Example:
   ```clojure
   (let [indexers (create-indexers)
         idx-array (create-array [0 2 4 6])]
     (set-array-indexer! indexers idx-array 0)
     (set-seq-indexer! indexers (make-seq 0 -1 1) 1 false)
     (let [result (assign-gen arr indexers 2 new-values)]
       (release-indexers! indexers)
       result))
   ```"
  [^AFArray lhs indexers ndims ^AFArray rhs]
  (let [out (jvm/native-af-array-pointer)]
    (jvm/check! (assign-ffi/af-assign-gen out (jvm/af-handle lhs) (long ndims)
                                          indexers (jvm/af-handle rhs))
                "af-assign-gen")
    (jvm/af-array-new (jvm/deref-af-array out))))

;;;
;;; Indexer Management
;;;

(defn create-indexers
  "Create an array of af_index_t indexers.
   
   Allocates indexer array for up to 4 dimensions. Must be released
   with release-indexers! after use.
   
   Returns:
   Pointer to indexer array (MemorySegment)
   
   Example:
   ```clojure
   (let [indexers (create-indexers)]
     (try
       ;; Use indexers...
       (finally
         (release-indexers! indexers))))
   ```"
  []
  (let [indexers-ptr (mem/alloc 8)] ; Pointer to pointer
    (jvm/check! (index-ffi/af-create-indexers indexers-ptr)
                "af-create-indexers")
    (.get indexers-ptr ValueLayout/ADDRESS 0)))

(defn set-array-indexer!
  "Set an array as the indexer for a specific dimension.
   
   Configures one dimension to use array-based (fancy) indexing.
   
   Parameters:
   - indexers: Indexer array pointer (MemorySegment)
   - idx: Index array (AFArray)
   - dim: Dimension to configure (long), 0-3
   
   Returns:
   nil
   
   Example:
   ```clojure
   (let [indexers (create-indexers)
         indices (create-array [0 5 10 15])]
     (set-array-indexer! indexers indices 0))
   ```"
  [indexers ^AFArray idx dim]
  (jvm/check! (index-ffi/af-set-array-indexer indexers (jvm/af-handle idx) (long dim))
              "af-set-array-indexer")
  nil)

(defn set-seq-indexer!
  "Set a sequence as the indexer for a specific dimension.
   
   Configures one dimension to use sequence-based (range) indexing.
   
   Parameters:
   - indexers: Indexer array pointer (MemorySegment)
   - seq: Sequence (MemorySegment from make-seq)
   - dim: Dimension to configure (long), 0-3
   - is-batch: Whether this is batch indexing (boolean), default false
   
   Returns:
   nil
   
   Example:
   ```clojure
   (let [indexers (create-indexers)
         s (make-seq 0 9 1)]
     (set-seq-indexer! indexers s 0 false))
   ```"
  ([indexers seq dim]
   (set-seq-indexer! indexers seq dim false))
  ([indexers seq dim is-batch]
   (jvm/check! (index-ffi/af-set-seq-indexer indexers seq (long dim) (if is-batch 1 0))
               "af-set-seq-indexer")
   nil))

(defn set-seq-param-indexer!
  "Set a sequence as indexer using parameters (convenience function).
   
   Like set-seq-indexer! but creates the sequence inline from parameters.
   
   Parameters:
   - indexers: Indexer array pointer (MemorySegment)
   - begin: Start index (double)
   - end: End index (double)
   - step: Step size (double)
   - dim: Dimension to configure (long), 0-3
   - is-batch: Whether this is batch indexing (boolean), default false
   
   Returns:
   nil
   
   Example:
   ```clojure
   (let [indexers (create-indexers)]
     ;; Select rows 0-9
     (set-seq-param-indexer! indexers 0 9 1 0 false)
     ;; Select all columns
     (set-seq-param-indexer! indexers 0 -1 1 1 false))
   ```"
  ([indexers begin end step dim]
   (set-seq-param-indexer! indexers begin end step dim false))
  ([indexers begin end step dim is-batch]
   (jvm/check! (index-ffi/af-set-seq-param-indexer indexers (double begin) (double end)
                                                    (double step) (long dim) (if is-batch 1 0))
               "af-set-seq-param-indexer")
   nil))

(defn release-indexers!
  "Release (deallocate) an indexer array.
   
   Must be called to free indexers created with create-indexers.
   
   Parameters:
   - indexers: Indexer array pointer (MemorySegment)
   
   Returns:
   nil
   
   Example:
   ```clojure
   (let [indexers (create-indexers)]
     (try
       (set-seq-indexer! indexers (make-seq 0 -1 1) 0 false)
       (index-gen arr indexers 1)
       (finally
         (release-indexers! indexers))))
   ```"
  [indexers]
  (jvm/check! (index-ffi/af-release-indexers indexers)
              "af-release-indexers")
  nil)

;;;
;;; Helper Functions
;;;

(defn slice
  "Convenience function for simple slicing (Python-style).
   
   Simplified interface for common slicing operations without managing
   sequences explicitly.
   
   Parameters:
   - arr: Input array (AFArray)
   - ranges: Vector of [start end] or [start end step] for each dimension
             Use nil for a dimension to select all elements
   
   Returns:
   Sliced array (AFArray)
   
   Examples:
   ```clojure
   ;; Select rows 0-9, all columns
   (slice arr [[0 9] nil])
   
   ;; Select rows 5-14, columns 10-19
   (slice arr [[5 14] [10 19]])
   
   ;; Every other row, every third column
   (slice arr [[0 -1 2] [0 -1 3]])
   ```"
  [^AFArray arr ranges]
  (let [seqs (mapv (fn [range-spec]
                    (cond
                      (nil? range-spec)
                      (make-seq 0 -1 1)
                      
                      (= 2 (count range-spec))
                      (let [[start end] range-spec]
                        (make-seq start end 1))
                      
                      (= 3 (count range-spec))
                      (let [[start end step] range-spec]
                        (make-seq start end step))
                      
                      :else
                      (throw (ex-info "Invalid range specification" {:range range-spec}))))
                   ranges)]
    (index arr seqs)))
