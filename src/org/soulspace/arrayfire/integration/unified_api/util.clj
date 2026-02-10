(ns org.soulspace.arrayfire.integration.unified-api.util
  "High-level Clojure wrapper for ArrayFire utility functions.
   
   Utility functions provide essential debugging, persistence, and data
   inspection capabilities. These include array printing, string conversion,
   and file-based array streaming (save/load operations).
   
   ## Quick Start
   
   ```clojure
   (require '[org.soulspace.arrayfire.integration.util :as util])
   (require '[org.soulspace.arrayfire.integration.array :as array])
   
   ;; Print array to stdout
   (let [data (array/random [10 10])]
     (util/print-array data))
   
   ;; Print with custom name and precision
   (let [data (array/random [5 5])]
     (util/print-array-gen \"my-data\" data 6))
   
   ;; Convert array to string
   (let [data (array/from-vec [1.0 2.0 3.0] [3])
         s (util/array-to-string \"values\" data 4 false)]
     (println s))
   
   ;; Save array to file
   (let [data (array/random [100 100])
         idx (util/save-array \"dataset1\" data \"data.af\" false)]
     (println \"Saved at index\" idx))
   
   ;; Load array by key
   (let [loaded (util/read-array-key \"data.af\" \"dataset1\")]
     loaded)
   
   ;; Load array by index
   (let [loaded (util/read-array-index \"data.af\" 0)]
     loaded)
   ```
   
   ## Printing and Debugging
   
   ### Array Printing
   
   ArrayFire provides functions to display array contents, dimensions, and
   metadata in human-readable format:
   
   - **print-array**: Quick print to stdout with default formatting
   - **print-array-gen**: Named print with custom precision
   - **array-to-string**: Convert to string for logging/display
   
   Printed format includes:
   - Array name (if specified)
   - Dimensions [rows cols depth batches]
   - Data values (formatted with specified precision)
   - Ellipsis (...) for large arrays
   
   **Use cases**:
   - Interactive development and debugging
   - Verification of intermediate results
   - Understanding data flow through pipelines
   - Logging array states for analysis
   - Generating documentation examples
   
   ### String Conversion
   
   The array-to-string function provides programmatic access to formatted
   array representations:
   
   ```clojure
   (let [data (array/from-vec [1.0 2.0 3.0 4.0] [2 2])
         s (array-to-string \"matrix\" data 2 false)]
     ;; s contains formatted string with 2 decimal precision
     (println s))
   ```
   
   Parameters:
   - **expression**: Name/label for the array
   - **precision**: Number of decimal places (0-16)
   - **transpose**: Display as transposed (for readability)
   
   ## Array Persistence (Streaming)
   
   ### File Format
   
   ArrayFire uses a custom binary format for efficient array storage:
   
   **Structure**:
   - Header: Version (1 byte), array count (4 bytes)
   - Per array: Key length, key string, data offset, dtype, dims[4], data
   
   **Features**:
   - Multiple arrays per file (tagged with keys)
   - Type preservation (no conversion)
   - Dimension/shape fully preserved
   - Binary format (fast I/O, compact)
   - Platform-independent
   
   ### Saving Arrays
   
   Save arrays with descriptive keys for later retrieval:
   
   ```clojure
   ;; Create new file
   (let [data1 (array/random [100 100])
         idx1 (save-array \"experiment1\" data1 \"results.af\" false)]
     idx1)
   
   ;; Append to existing file
   (let [data2 (array/random [100 100])
         idx2 (save-array \"experiment2\" data2 \"results.af\" true)]
     idx2)
   ```
   
   The function returns the 0-based index of the saved array in the file.
   
   ### Loading Arrays
   
   Retrieve arrays by index (fast) or by key (requires linear search):
   
   **By index** (fastest):
   ```clojure
   (let [arr (read-array-index \"results.af\" 0)]  ; First array
     arr)
   ```
   
   **By key** (by name):
   ```clojure
   (let [arr (read-array-key \"results.af\" \"experiment1\")]
     arr)
   ```
   
   **Check if key exists**:
   ```clojure
   (let [idx (read-array-key-check \"results.af\" \"experiment1\")]
     (if (>= idx 0)
       (println \"Found at index\" idx)
       (println \"Key not found\")))
   ```
   
   ## Common Use Cases
   
   ### Checkpointing
   
   Save intermediate results during long computations:
   
   ```clojure
   (doseq [epoch (range 100)]
     (let [weights (train-step model data)]
       (when (zero? (mod epoch 10))
         (save-array (str \"epoch-\" epoch) weights \"checkpoint.af\" true))))
   ```
   
   ### Result Collection
   
   Gather results from multiple experiments:
   
   ```clojure
   ;; Create file
   (save-array \"baseline\" baseline-result \"experiments.af\" false)
   
   ;; Add experiments
   (doseq [[name result] experiments]
     (save-array name result \"experiments.af\" true))
   
   ;; Later: load all results
   (let [baseline (read-array-key \"experiments.af\" \"baseline\")]
     baseline)
   ```
   
   ### Model Persistence
   
   Save/load neural network weights:
   
   ```clojure
   ;; Save model layers
   (doseq [[layer-name weights] model]
     (save-array layer-name weights \"model.af\" true))
   
   ;; Load model
   (let [layer1 (read-array-key \"model.af\" \"layer1\")
         layer2 (read-array-key \"model.af\" \"layer2\")]
     {:layer1 layer1 :layer2 layer2})
   ```
   
   ### Batch Processing
   
   Process and save results in parallel:
   
   ```clojure
   (doseq [i (range 10)]
     (let [result (process-batch i data)
           key (str \"batch-\" i)]
       (save-array key result \"batches.af\" true)))
   ```
   
   ## Performance Tips
   
   1. **Index vs Key**: Use index-based access when possible (O(1) vs O(n))
   2. **Batch saves**: Group related arrays in single file for locality
   3. **Append mode**: Reuse files to reduce file count
   4. **Key naming**: Use systematic naming for easy programmatic access
   5. **File organization**: Separate files for different data types/stages
   
   ## Limitations
   
   - Binary format (not human-readable)
   - Key lookup is linear search (O(n))
   - Sequential format (not optimized for random access)
   - No built-in compression
   - No concurrent write support
   
   ## Alternatives
   
   For specific needs, consider:
   - **HDF5**: Hierarchical data, large datasets, metadata
   - **NumPy .npy**: Python ecosystem interoperability
   - **CSV/Text**: Human-readable, debugging, small datasets
   - **Custom binary**: Application-specific optimizations
   
   See also:
   - ArrayFire util documentation: https://arrayfire.org/docs/group__util__func.htm
   - Data functions for array creation
   - Memory management for resource handling"
  (:require [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.c-api.print :as print]
            [org.soulspace.arrayfire.ffi.c-api.stream :as stream]
            [org.soulspace.arrayfire.integration.base.jvm-integration :as jvm])
  (:import (org.soulspace.arrayfire.integration.base.jvm_integration AFArray)))

;;;
;;; Array Printing and Display
;;;

(defn print-array
  "Print an array to standard output.
   
   Displays array contents, dimensions, and metadata in a human-readable
   format. Useful for debugging and interactive development.
   
   Format:
   ```
   No Name Array
   [rows cols depth batch]
   1.0000  2.0000  3.0000 ...
   4.0000  5.0000  6.0000 ...
   ...
   ```
   
   Parameters:
   - arr: Input array (AFArray)
   
   Returns:
   nil (prints to stdout)
   
   Example:
   ```clojure
   (let [data (array/random [10 10])]
     (print-array data))
   ;; Prints:
   ;; No Name Array
   ;; [10 10 1 1]
   ;; 0.1234  0.5678 ...
   ;; ...
   ```
   
   See also:
   - print-array-gen: Named print with custom precision
   - array-to-string: Convert to string for programmatic use"
  [^AFArray arr]
  (jvm/check! (print/af-print-array (jvm/af-handle arr))
              "af-print-array")
  nil)

(defn print-array-gen
  "Print an array with a custom name and precision.
   
   Displays array with a user-specified name and decimal precision.
   More flexible than print-array for formatted output.
   
   Format:
   ```
   <expression>
   [rows cols depth batch]
   1.000000  2.000000  3.000000 ...
   4.000000  5.000000  6.000000 ...
   ...
   ```
   
   Parameters:
   - expression: Name/label for the array (String)
   - arr: Input array (AFArray)
   - precision: Number of decimal places (0-16, default 4)
   
   Returns:
   nil (prints to stdout)
   
   Example:
   ```clojure
   (let [data (array/random [5 5])]
     (print-array-gen \"my-matrix\" data 6))
   ;; Prints:
   ;; my-matrix
   ;; [5 5 1 1]
   ;; 0.123456  0.567890 ...
   ;; ...
   ```
   
   See also:
   - print-array: Quick print with default formatting
   - array-to-string: Get formatted string instead of printing"
  ([expression ^AFArray arr]
   (print-array-gen expression arr 4))
  ([expression ^AFArray arr precision]
   (let [c-str (jvm/string->c-string expression)]
     (jvm/check! (print/af-print-array-gen c-str (jvm/af-handle arr) (int precision))
                 "af-print-array-gen")
     nil)))

(defn array-to-string
  "Convert an array to a formatted string representation.
   
   Returns array contents as a string rather than printing to stdout.
   Useful for logging, display in UIs, or programmatic string processing.
   
   Parameters:
   - expression: Name/label for the array (String)
   - arr: Input array (AFArray)
   - precision: Number of decimal places (0-16, default 4)
   - transpose: Display as transposed for readability (default false)
   
   Returns:
   Formatted string containing array representation
   
   Example:
   ```clojure
   (let [data (array/from-vec [1.0 2.0 3.0 4.0] [2 2])
         s (array-to-string \"matrix\" data 2 false)]
     (println s))
   ;; Prints:
   ;; matrix
   ;; [2 2 1 1]
   ;; 1.00  2.00
   ;; 3.00  4.00
   ```
   
   Use cases:
   - Logging array states
   - Display in web UIs
   - Generating reports
   - Debugging with custom output
   - Testing array contents
   
   See also:
   - print-array: Direct print to stdout
   - print-array-gen: Named print with precision"
  ([expression ^AFArray arr]
   (array-to-string expression arr 4 false))
  ([expression ^AFArray arr precision]
   (array-to-string expression arr precision false))
  ([expression ^AFArray arr precision transpose]
   (let [c-expr (jvm/string->c-string expression)
         output-ptr (jvm/native-af-array-pointer)]
     (jvm/check! (print/af-array-to-string output-ptr c-expr (jvm/af-handle arr) 
                                           (int precision) (if transpose 1 0))
                 "af-array-to-string")
     (let [str-addr (jvm/deref-af-array output-ptr)
           str-segment (java.lang.foreign.MemorySegment/ofAddress str-addr)]
       (jvm/c-string->string str-segment)))))

;;;
;;; Array Persistence (Streaming)
;;;

(defn save-array
  "Save an array to a file with a key/tag.
   
   Writes array to disk in ArrayFire's binary format. The array is tagged
   with a key (name) for later retrieval. Supports multiple arrays per file
   through append mode.
   
   File format preserves:
   - Data type (no conversion)
   - Dimensions and shape
   - All element values
   
   Parameters:
   - key: String key/tag to identify this array (String)
   - arr: Array to save (AFArray)
   - filename: Path to output file (String)
   - append: Append to existing file (default false = create new/overwrite)
   
   Returns:
   Integer index (0-based) of saved array in file
   
   Examples:
   ```clojure
   ;; Create new file
   (let [data (array/random [100 100])
         idx (save-array \"dataset1\" data \"data.af\" false)]
     (println \"Saved at index\" idx))
   
   ;; Append to existing file
   (let [data2 (array/random [100 100])
         idx2 (save-array \"dataset2\" data2 \"data.af\" true)]
     (println \"Appended at index\" idx2))
   
   ;; Save multiple arrays
   (doseq [[name arr] datasets]
     (save-array name arr \"datasets.af\" true))
   ```
   
   Use cases:
   - Checkpointing computations
   - Model persistence
   - Result collection
   - Data sharing
   - Debugging/analysis
   
   See also:
   - read-array-key: Load by key name
   - read-array-index: Load by index
   - read-array-key-check: Check if key exists"
  ([key ^AFArray arr filename]
   (save-array key arr filename false))
  ([key ^AFArray arr filename append]
   (let [c-key (jvm/string->c-string key)
         c-filename (jvm/string->c-string filename)
         index-buf (mem/alloc-instance ::mem/int)]
     (jvm/check! (stream/af-save-array index-buf c-key (jvm/af-handle arr) 
                                      c-filename (if append 1 0))
                 "af-save-array")
     (mem/read-int index-buf 0))))

(defn read-array-index
  "Read an array from a file by its index position.
   
   Fast O(1) access to arrays by their sequential position in the file.
   Arrays are indexed starting from 0 in the order they were saved.
   
   Parameters:
   - filename: Path to input file (String)
   - index: 0-based index of array in file (Integer)
   
   Returns:
   AFArray loaded from file
   
   Example:
   ```clojure
   ;; Load first array
   (let [arr (read-array-index \"data.af\" 0)]
     arr)
   
   ;; Load third array
   (let [arr (read-array-index \"data.af\" 2)]
     arr)
   
   ;; Load all arrays sequentially
   (let [count 5  ; number of arrays in file
         arrays (mapv #(read-array-index \"data.af\" %) (range count))]
     arrays)
   ```
   
   Performance: O(1) - Direct offset calculation, fastest access method
   
   See also:
   - read-array-key: Load by key name (slower, O(n))
   - save-array: Save array with key
   - read-array-key-check: Get index for a key"
  [filename index]
  (let [c-filename (jvm/string->c-string filename)
        out (jvm/native-af-array-pointer)]
    (jvm/check! (stream/af-read-array-index out c-filename (int index))
                "af-read-array-index")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn read-array-key
  "Read an array from a file by its key/tag name.
   
   Searches file for array with matching key and loads it. More convenient
   than index-based access but slower for large files (O(n) search).
   
   Parameters:
   - filename: Path to input file (String)
   - key: String key/tag identifying the array (String)
   
   Returns:
   AFArray loaded from file
   
   Throws:
   Exception if key not found in file
   
   Example:
   ```clojure
   ;; Load by descriptive name
   (let [baseline (read-array-key \"experiments.af\" \"baseline\")
         improved (read-array-key \"experiments.af\" \"improved-v2\")]
     {:baseline baseline :improved improved})
   
   ;; Load model layers
   (let [layer1 (read-array-key \"model.af\" \"conv1\")
         layer2 (read-array-key \"model.af\" \"conv2\")]
     {:layer1 layer1 :layer2 layer2})
   ```
   
   Performance: O(n) - Linear search through file, slower than index access
   
   Tip: Use read-array-key-check first to verify key exists
   
   See also:
   - read-array-index: Faster index-based access
   - save-array: Save with key
   - read-array-key-check: Check key existence"
  [filename key]
  (let [c-filename (jvm/string->c-string filename)
        c-key (jvm/string->c-string key)
        out (jvm/native-af-array-pointer)]
    (jvm/check! (stream/af-read-array-key out c-filename c-key)
                "af-read-array-key")
    (jvm/af-array-new (jvm/deref-af-array out))))

(defn read-array-key-check
  "Check if a key exists in a file and return its index.
   
   Searches file for array with matching key without loading it. Returns
   the index if found, -1 if not found. Useful for verifying key existence
   before attempting to load.
   
   Parameters:
   - filename: Path to input file (String)
   - key: String key/tag to search for (String)
   
   Returns:
   Integer index (>= 0) if key found, -1 if not found
   
   Example:
   ```clojure
   ;; Check if key exists
   (let [idx (read-array-key-check \"data.af\" \"experiment1\")]
     (if (>= idx 0)
       (println \"Found at index\" idx)
       (println \"Key not found\")))
   
   ;; Conditional loading
   (let [idx (read-array-key-check \"data.af\" \"checkpoint\")]
     (when (>= idx 0)
       (read-array-index \"data.af\" idx)))
   
   ;; Validate multiple keys
   (let [keys [\"baseline\" \"experiment1\" \"experiment2\"]
         results (map #(read-array-key-check \"data.af\" %) keys)]
     (zipmap keys results))
   ```
   
   Use cases:
   - Verify key existence before loading
   - Handle missing data gracefully
   - Convert key to index for faster repeated access
   - Validate file contents
   
   See also:
   - read-array-key: Load by key
   - read-array-index: Load by index
   - save-array: Save with key"
  [filename key]
  (let [c-filename (jvm/string->c-string filename)
        c-key (jvm/string->c-string key)
        index-buf (mem/alloc-instance ::mem/int)]
    (jvm/check! (stream/af-read-array-key-check index-buf c-filename c-key)
                "af-read-array-key-check")
    (mem/read-int index-buf 0)))

