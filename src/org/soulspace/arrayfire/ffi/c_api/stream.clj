(ns org.soulspace.arrayfire.ffi.c-api.stream
  "ArrayFire FFI bindings for array streaming (save/load) operations.

  Array streaming provides functionality to persist ArrayFire arrays to disk
  and read them back. This enables data persistence, checkpointing, result
  sharing, and workflow management across sessions.

  ## File Format

  ArrayFire uses a custom binary format for array storage:

  **Format Version 1 Structure:**
  ```
  Header (per file):
    - char:     Version number (currently 0x1)
    - int:      Number of arrays in file

  Per Array:
    - int:      Length of key string
    - cstring:  Key/tag name
    - intl:     Offset to next array (in bytes)
    - char:     Data type (af_dtype enum value)
    - intl[4]:  Dimensions (4D array shape)
    - T[]:      Array data (elements in row-major order)
  ```

  ## Key Features

  1. **Multiple Arrays per File**:
     - Single file can store multiple arrays
     - Each array tagged with unique key/name
     - Append mode adds arrays to existing files

  2. **Type Preservation**:
     - Original data type preserved (f32, f64, s32, etc.)
     - No type conversion on save/load
     - Dimensions and shape fully preserved

  3. **Sequential Access**:
     - Arrays stored and indexed sequentially (0-based)
     - Can read by index or by key name
     - Index lookup for efficient key-based access

  ## File Operations

  ### Saving Arrays
  - **New file**: Creates file, writes array with key
  - **Append mode**: Adds array to existing file
  - Returns index of saved array in file

  ### Reading Arrays
  - **By index**: Direct sequential access (0-based)
  - **By key**: Lookup array by name/tag
  - **Key check**: Verify key exists before reading

  ## Use Cases

  - **Checkpointing**: Save intermediate computation results
  - **Data sharing**: Exchange arrays between applications
  - **Persistence**: Store trained models, calibration data
  - **Workflow management**: Save pipeline stages
  - **Debugging**: Capture array states for analysis
  - **Batch processing**: Save results from parallel jobs
  - **Version control**: Tag arrays with version/timestamp keys

  ## Performance Considerations

  - Binary format: Fast I/O, compact storage
  - Sequential file format: Best for append-once, read-many workloads
  - Key lookup: O(n) search through file
  - Large files: Consider index-based access for speed
  - GPU→CPU transfer: Data copied to host before save

  ## Workflow Patterns

  ### Checkpointing
  ```
  1. Compute expensive result
  2. Save with timestamp key
  3. Continue computation
  4. On failure: Load last checkpoint
  ```

  ### Result Collection
  ```
  1. Create empty file
  2. For each experiment:
     - Run computation
     - Append result with descriptive key
  3. Later: Load all results by index
  ```

  ### Model Storage
  ```
  1. Train model (weights as arrays)
  2. Save each layer with key (layer1, layer2, ...)
  3. Load model: Read by keys
  4. Restore model state
  ```

  ## File Format Advantages

  - **Self-describing**: Contains type and dimension metadata
  - **Versioned**: Format version enables future compatibility
  - **Compact**: Binary storage, no text overhead
  - **Portable**: Platform-independent format

  ## Limitations

  - Not human-readable (binary format)
  - Sequential format (not random-access optimized)
  - Key lookup requires linear search
  - No compression (raw binary data)
  - No multi-user concurrent write support

  ## Alternatives

  For different needs, consider:
  - HDF5: Complex hierarchical data, large datasets
  - NumPy .npy: Python ecosystem interop
  - CSV/Text: Human-readable, debugging
  - Custom binary: Application-specific formats

  See also:
  - Print functions (af_print_array) for debugging
  - Memory management for array lifecycle
  - Type conversion for format compatibility"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.loader]))

;;
;; Array Streaming Functions
;;

(defcfn af-save-array
  "Save an array to a file with a key/tag.

  Writes an array to disk in ArrayFire's binary format. The array is tagged
  with a key (name) for later retrieval. Supports appending multiple arrays
  to a single file.

  Parameters:
  - index: Output pointer for array's index in file (0-based)
  - key: String key/tag to identify this array (C string)
  - arr: Array to save
  - filename: Path to output file (C string)
  - append: Boolean - true to append, false to create/overwrite

  Returns:
  Error code indicating success or failure.

  Example (save single array):
  ```clojure
  ;; Save array to new file
  (let [data (af-randn [100 100] :f32)
        idx (mem/alloc-instance ::mem/int)
        key \"my_data\"
        filename \"/tmp/arrays.af\"]
    (af-save-array idx key data filename false) ;; create new
    (println \"Saved at index:\" (mem/read-int idx)))
  ```

  Example (append multiple arrays):
  ```clojure
  ;; Save multiple arrays to same file
  (let [filename \"/tmp/results.af\"]
    ;; First array - create file
    (let [arr1 (af-randn [50 50] :f64)
          idx1 (mem/alloc-instance ::mem/int)]
      (af-save-array idx1 \"experiment_1\" arr1 filename false))
    
    ;; Second array - append
    (let [arr2 (af-randn [50 50] :f64)
          idx2 (mem/alloc-instance ::mem/int)]
      (af-save-array idx2 \"experiment_2\" arr2 filename true))
    
    ;; Third array - append
    (let [arr3 (af-randn [50 50] :f64)
          idx3 (mem/alloc-instance ::mem/int)]
      (af-save-array idx3 \"experiment_3\" arr3 filename true)))
  ```

  Example (checkpoint workflow):
  ```clojure
  ;; Save computation checkpoints
  (defn save-checkpoint [array epoch filename]
    (let [idx (mem/alloc-instance ::mem/int)
          key (str \"epoch_\" epoch)]
      (af-save-array idx key array filename true) ;; append
      (println \"Checkpoint\" epoch \"saved at index\" (mem/read-int idx))))

  ;; During training
  (doseq [epoch (range 100)]
    (let [state (train-one-epoch ...)]
      (when (zero? (mod epoch 10))
        (save-checkpoint state epoch \"/tmp/training.af\"))))
  ```

  Example (save with metadata key):
  ```clojure
  ;; Use descriptive keys with metadata
  (defn save-with-metadata [array metadata filename]
    (let [idx (mem/alloc-instance ::mem/int)
          key (str (:name metadata) \"_\" 
                   (:version metadata) \"_\" 
                   (:timestamp metadata))]
      (af-save-array idx key array filename true)
      (mem/read-int idx)))

  (save-with-metadata result-array 
                      {:name \"model_weights\"
                       :version \"v1.2\"
                       :timestamp (System/currentTimeMillis)}
                      \"/tmp/models.af\")
  ```

  File Behavior:
  - **append=false**: Creates new file (overwrites if exists)
  - **append=true**: Adds to existing file (creates if doesn't exist)
  - Empty file: Writes header + single array
  - Existing file: Validates version, increments array count, appends data

  Index Usage:
  - Returns 0-based sequential index of array in file
  - First array in file: index = 0
  - Can use index for fast reading later (af-read-array-index)
  - Index increments with each append

  Key Naming:
  - Keys are C-style null-terminated strings
  - Must be unique within file (not enforced, but recommended)
  - Case-sensitive
  - Can contain any characters (alphanumeric, underscore, etc.)
  - Suggested format: descriptive_name_with_underscores

  Type Preservation:
  All ArrayFire types supported:
  - Floating-point: f32, f64, c32, c64
  - Integers: s8, s16, s32, s64, u8, u16, u32, u64
  - Boolean: b8
  - Type stored in file, preserved on load

  Performance:
  - Data transferred from GPU to host before writing
  - File I/O is sequential (efficient for large arrays)
  - Append mode: Seeks to end of file
  - Binary format: No conversion overhead

  Error Conditions:
  - File cannot be opened/created: AF_ERR_ARG
  - Invalid file permissions: System I/O error
  - Disk full: System I/O error
  - Null key or filename: AF_ERR_ARG

  Notes:
  - Files are platform-independent (portable binary format)
  - Version byte enables future format compatibility
  - No compression applied (raw binary data)
  - Arrays written in row-major order

  See also:
  - af_save_array (ArrayFire C API)
  - af-read-array-index: Read array by index
  - af-read-array-key: Read array by key"
  "af_save_array" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

(defcfn af-read-array-index
  "Read an array from a file by its sequential index.

  Reads an array from disk using its 0-based position in the file. This is
  the fastest read method when you know the array's position.

  Parameters:
  - out: Output pointer for loaded array
  - filename: Path to input file (C string)
  - index: 0-based sequential position of array in file

  Returns:
  Error code indicating success or failure.

  Example (read by index):
  ```clojure
  ;; Read first array from file
  (let [arr (mem/alloc-instance ::mem/pointer)
        filename \"/tmp/arrays.af\"]
    (af-read-array-index arr filename 0) ;; index 0 = first array
    (let [data (mem/read-pointer arr ::mem/pointer)]
      ;; Use loaded array
      (af-print-array data)
      (af-release-array data)))
  ```

  Example (read multiple arrays):
  ```clojure
  ;; Read all arrays from file
  (defn read-all-arrays [filename num-arrays]
    (for [i (range num-arrays)]
      (let [arr (mem/alloc-instance ::mem/pointer)]
        (af-read-array-index arr filename i)
        (mem/read-pointer arr ::mem/pointer))))

  (let [arrays (read-all-arrays \"/tmp/results.af\" 3)]
    ;; Process all loaded arrays
    (doseq [arr arrays]
      (process-array arr)
      (af-release-array arr)))
  ```

  Example (load checkpoint by epoch):
  ```clojure
  ;; Load specific checkpoint
  (defn load-checkpoint [epoch-number filename]
    (let [arr (mem/alloc-instance ::mem/pointer)
          ;; Assuming checkpoints saved every 10 epochs starting at 0
          index (/ epoch-number 10)]
      (af-read-array-index arr filename index)
      (mem/read-pointer arr ::mem/pointer)))

  ;; Resume from epoch 50
  (let [state (load-checkpoint 50 \"/tmp/training.af\")]
    (resume-training state))
  ```

  Index Semantics:
  - 0-based indexing (first array is index 0)
  - Sequential order: Arrays read in order they were written
  - Index must be < number of arrays in file
  - Out of bounds: Throws exception

  File Format:
  - Reads file header to validate version
  - Skips arrays before target index
  - Reads type, dimensions, and data for target array
  - Closes file after reading

  Type Handling:
  - Array type read from file metadata
  - Original type preserved (no conversion)
  - Output array has same type as saved array
  - Dimensions and shape fully restored

  Memory:
  - Array data loaded to GPU device memory
  - Temporary host buffer used during load
  - Output is GPU-resident af_array
  - Caller responsible for releasing array

  Performance:
  - Sequential read: O(n) where n = index
  - Must skip all preceding arrays
  - For random access to many arrays: Consider reading by key once,
    then using indices
  - Binary format: Fast I/O

  Error Conditions:
  - File not found: AF_ERR_ARG
  - File cannot be opened: AF_ERR_ARG
  - Empty file: AF_ERR_ARG
  - Invalid version: AF_ERR_ARG
  - Index out of bounds: Exception
  - Corrupted file: Various errors

  Use Cases:
  - Sequential array processing
  - Known index from previous save
  - Fastest read when index known
  - Batch loading of results

  Notes:
  - Initializes ArrayFire if not already initialized
  - Thread-safe for read operations
  - Can read from file while it's being appended to (not recommended)

  See also:
  - af_read_array_index (ArrayFire C API)
  - af-read-array-key: Read by key name
  - af-save-array: Save array to file"
  "af_read_array_index" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

(defcfn af-read-array-key
  "Read an array from a file by its key/tag.

  Reads an array from disk using its key (name/tag) specified during save.
  Searches file for matching key and loads corresponding array.

  Parameters:
  - out: Output pointer for loaded array
  - filename: Path to input file (C string)
  - key: Key/tag of array to read (C string, exact match required)

  Returns:
  Error code indicating success or failure.

  Example (read by key):
  ```clojure
  ;; Load array by name
  (let [arr (mem/alloc-instance ::mem/pointer)
        filename \"/tmp/results.af\"
        key \"experiment_1\"]
    (af-read-array-key arr filename key)
    (let [data (mem/read-pointer arr ::mem/pointer)]
      (process-data data)
      (af-release-array data)))
  ```

  Example (load with error handling):
  ```clojure
  ;; Try to load array, handle missing key
  (defn try-load-array [filename key]
    (try
      (let [arr (mem/alloc-instance ::mem/pointer)]
        (af-read-array-key arr filename key)
        (mem/read-pointer arr ::mem/pointer))
      (catch Exception e
        (println \"Key not found:\" key)
        nil)))

  (when-let [data (try-load-array \"/tmp/results.af\" \"my_data\")]
    (process-data data)
    (af-release-array data))
  ```

  Example (load model weights):
  ```clojure
  ;; Load neural network layer weights
  (defn load-model-weights [filename]
    (letfn [(load-layer [layer-name]
              (let [arr (mem/alloc-instance ::mem/pointer)]
                (af-read-array-key arr filename layer-name)
                (mem/read-pointer arr ::mem/pointer)))]
      {:layer1 (load-layer \"layer1_weights\")
       :layer2 (load-layer \"layer2_weights\")
       :layer3 (load-layer \"layer3_weights\")}))

  (let [model (load-model-weights \"/tmp/trained_model.af\")]
    (restore-model model))
  ```

  Example (conditional loading):
  ```clojure
  ;; Load specific version if exists
  (defn load-version [filename version]
    (let [key (str \"model_v\" version)
          idx (mem/alloc-instance ::mem/int)]
      ;; Check if key exists first
      (af-read-array-key-check idx filename key)
      (if (>= (mem/read-int idx) 0)
        (let [arr (mem/alloc-instance ::mem/pointer)]
          (af-read-array-key arr filename key)
          (mem/read-pointer arr ::mem/pointer))
        (do
          (println \"Version\" version \"not found\")
          nil))))
  ```

  Key Matching:
  - Exact match required (case-sensitive)
  - Key must match exactly as saved
  - Whitespace significant
  - No partial matching or wildcards

  Search Process:
  - Linear search through file
  - Reads each key sequentially until match found
  - Skips array data for non-matching keys
  - O(n) complexity where n = number of arrays

  Error Handling:
  - Key not found: AF_ERR_INVALID_ARRAY exception
  - Use af-read-array-key-check to verify key exists before reading
  - Recommended for production code to check first

  Type and Dimension:
  - Array restored with original type and dimensions
  - No conversion applied
  - Metadata preserved exactly as saved

  Performance:
  - Linear search: Slower than index-based read
  - Faster for early keys in file
  - Consider caching index for repeated access
  - For many reads: Use af-read-array-key-check once, then read by index

  Optimization Pattern:
  ```clojure
  ;; First, find index
  (let [idx (mem/alloc-instance ::mem/int)]
    (af-read-array-key-check idx filename key)
    (when (>= (mem/read-int idx) 0)
      ;; Then use fast index-based read
      (let [arr (mem/alloc-instance ::mem/pointer)]
        (af-read-array-index arr filename (mem/read-int idx))
        (mem/read-pointer arr ::mem/pointer))))
  ```

  Use Cases:
  - Named data retrieval
  - Configuration loading
  - Model checkpoint restoration
  - Result lookup by experiment name

  Notes:
  - Initializes ArrayFire if not already initialized
  - Throws exception if key not found (not just error code)
  - Caller responsible for releasing loaded array

  See also:
  - af_read_array_key (ArrayFire C API)
  - af-read-array-key-check: Check if key exists
  - af-read-array-index: Read by index (faster)"
  "af_read_array_key" [::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

(defcfn af-read-array-key-check
  "Check if a key exists in a file and return its index.

  Searches file for a key without loading the array. Returns the array's
  index if found, or -1 if not found. Useful for checking existence before
  attempting to load.

  Parameters:
  - index: Output pointer for array index (-1 if not found)
  - filename: Path to input file (C string)
  - key: Key/tag to search for (C string)

  Returns:
  Error code indicating success or failure.

  Example (check before loading):
  ```clojure
  ;; Safely check and load
  (let [idx (mem/alloc-instance ::mem/int)
        filename \"/tmp/data.af\"
        key \"my_array\"]
    (af-read-array-key-check idx filename key)
    (let [index (mem/read-int idx)]
      (if (>= index 0)
        (let [arr (mem/alloc-instance ::mem/pointer)]
          (af-read-array-index arr filename index)
          (println \"Loaded array from index\" index)
          (mem/read-pointer arr ::mem/pointer))
        (println \"Key not found:\" key))))
  ```

  Example (check multiple keys):
  ```clojure
  ;; Check which keys exist
  (defn check-keys [filename keys]
    (for [key keys]
      (let [idx (mem/alloc-instance ::mem/int)]
        (af-read-array-key-check idx filename key)
        {:key key 
         :index (mem/read-int idx)
         :exists? (>= (mem/read-int idx) 0)})))

  (let [status (check-keys \"/tmp/results.af\" 
                           [\"exp1\" \"exp2\" \"exp3\"])]
    (println \"Available arrays:\" 
             (filter :exists? status)))
  ```

  Example (smart loading):
  ```clojure
  ;; Load most recent version available
  (defn load-latest-version [filename base-key max-version]
    (loop [v max-version]
      (when (>= v 0)
        (let [key (str base-key \"_v\" v)
              idx (mem/alloc-instance ::mem/int)]
          (af-read-array-key-check idx filename key)
          (if (>= (mem/read-int idx) 0)
            ;; Found it!
            (let [arr (mem/alloc-instance ::mem/pointer)]
              (af-read-array-index arr filename (mem/read-int idx))
              (mem/read-pointer arr ::mem/pointer))
            ;; Try previous version
            (recur (dec v)))))))

  (let [latest (load-latest-version \"/tmp/models.af\" \"model\" 10)]
    (when latest
      (println \"Loaded latest model\")
      (use-model latest)))
  ```

  Example (index caching):
  ```clojure
  ;; Build index cache for fast repeated access
  (defn build-index-cache [filename keys]
    (reduce (fn [cache key]
              (let [idx (mem/alloc-instance ::mem/int)]
                (af-read-array-key-check idx filename key)
                (let [index (mem/read-int idx)]
                  (if (>= index 0)
                    (assoc cache key index)
                    cache))))
            {}
            keys))

  ;; Use cache for fast loading
  (let [cache (build-index-cache \"/tmp/data.af\" 
                                  [\"data1\" \"data2\" \"data3\"])]
    (when-let [idx (get cache \"data2\")]
      (let [arr (mem/alloc-instance ::mem/pointer)]
        (af-read-array-index arr filename idx)
        (mem/read-pointer arr ::mem/pointer))))
  ```

  Return Values:
  - index >= 0: Key found, this is its position
  - index = -1: Key not found in file

  Performance Benefits:
  - No array data loaded (fast)
  - Can check multiple keys efficiently
  - Build index cache for repeated access
  - Avoid exceptions from failed loads

  Recommended Workflow:
  1. Check key exists with af-read-array-key-check
  2. If exists (index >= 0):
     - Use af-read-array-index with returned index (fast)
  3. If not exists (index = -1):
     - Handle missing key gracefully

  Search Process:
  - Same linear search as af-read-array-key
  - Reads keys but skips array data
  - Returns immediately when key found
  - Scans entire file if key not found

  Use Cases:
  - Graceful error handling
  - Conditional loading
  - Key existence verification
  - Building key→index maps for fast access
  - Version checking

  Error Conditions:
  - File not found: AF_ERR_ARG
  - File cannot be opened: AF_ERR_ARG
  - Empty file: AF_ERR_ARG
  - Invalid version: AF_ERR_ARG
  - Key not found: Returns index=-1 (not an error)

  Notes:
  - Does not throw exception for missing key (returns -1)
  - Initializes ArrayFire if needed
  - Lightweight operation (no array data loaded)
  - Thread-safe for read operations

  See also:
  - af_read_array_key_check (ArrayFire C API)
  - af-read-array-key: Read array by key
  - af-read-array-index: Read array by index"
  "af_read_array_key_check" [::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)
