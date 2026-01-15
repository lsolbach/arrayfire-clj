(ns org.soulspace.arrayfire.ffi.print
  "Bindings for the ArrayFire array printing and string conversion functions.
   
   Array printing is essential for debugging, development, and understanding array
   contents. ArrayFire provides functions to print arrays to standard output or
   convert them to string representations for logging and display.
   
   ## What is Array Printing?
   
   Array printing displays the contents, dimensions, and metadata of ArrayFire
   arrays in a human-readable format. This is crucial for:
   
   - **Debugging**: Inspect intermediate results during development
   - **Verification**: Confirm array contents match expectations
   - **Logging**: Capture array states for analysis
   - **Development**: Understand data flow through computations
   - **Documentation**: Generate examples and demonstrations
   
   ## Printing Modes
   
   ArrayFire supports two main printing approaches:
   
   ### 1. Direct Printing (af-print-array)
   
   Prints directly to standard output (stdout):
   ```
   No Name Array
   [512 512 1 1]
   1.0000  2.0000  3.0000 ...
   4.0000  5.0000  6.0000 ...
   ...
   ```
   
   Features:
   - Immediate output to console
   - Fixed precision (4 decimal places)
   - Suitable for quick inspection
   - No string allocation overhead
   
   ### 2. Named Printing (af-print-array-gen)
   
   Prints with custom name and precision:
   ```
   myArray
   [100 100 1 1]
   1.000000  2.000000  3.000000 ...
   4.000000  5.000000  6.000000 ...
   ...
   ```
   
   Features:
   - Custom array name/expression display
   - Configurable precision (0-15+ decimals)
   - Better for labeled output
   - More informative for debugging
   
   ### 3. String Conversion (af-array-to-string)
   
   Converts array to string for programmatic use:
   ```clojure
   (let [str-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-array-to-string str-ptr \"data\" arr 6 true)
         str-addr (mem/read-pointer str-ptr ::mem/pointer)
         array-str (mem/read-string str-addr)]
     (println array-str)
     (af-free-host str-addr))
   ```
   
   Features:
   - Returns string instead of printing
   - Full control over formatting
   - Can be logged, stored, transmitted
   - Requires explicit memory management
   
   ## Display Format
   
   ### Array Metadata
   
   Every print includes:
   ```
   ArrayName          ← Name/expression (if provided)
   [rows cols depth batch]  ← Dimensions (dim4 format)
   ```
   
   Example:
   ```
   myData
   [3 4 1 1]
   ```
   
   ### Data Layout
   
   **Column-major order** (ArrayFire default):
   ```
   For [3, 4] matrix:
   [0,0] [0,1] [0,2] [0,3]
   [1,0] [1,1] [1,2] [1,3]
   [2,0] [2,1] [2,2] [2,3]
   ```
   
   **Transposed display** (transpose=true, default):
   - More natural for reading (rows × cols)
   - Matches mathematical notation
   - Default for af-array-to-string
   
   **Non-transposed display** (transpose=false):
   - Shows internal memory layout
   - Column-major format
   - Useful for understanding storage
   
   ### Large Array Handling
   
   For large arrays, output is truncated intelligently:
   
   **Many elements:**
   ```
   [1000 1000 1 1]
   1.0  2.0  3.0  ...  ← First few columns
   4.0  5.0  6.0  ...
   ...                ← Rows truncated
   ```
   
   **Multiple dimensions:**
   ```
   [10 10 5 3]
   [:, :, 0, 0]       ← First 2D slice
   1.0  2.0  ...
   ...
   
   [:, :, 1, 0]       ← Second 2D slice
   ...
   ```
   
   Truncation helps manage output size while providing representative data.
   
   ## Precision Control
   
   Precision determines decimal places displayed:
   
   | Precision | Display | Use Case |
   |-----------|---------|----------|
   | 0 | Integer | Counts, indices |
   | 2 | 1.23 | Percentages, ratios |
   | 4 | 1.2345 | Default, general use |
   | 6 | 1.234567 | Scientific data |
   | 10 | 1.2345678901 | High precision |
   | 15 | 1.234567890123456 | Maximum (double) |
   
   **Type-appropriate precision:**
   - Float32: 6-7 significant digits (precision ≤ 7)
   - Float64: 15-16 significant digits (precision ≤ 16)
   - Integers: Precision affects spacing, not value
   
   **Choosing precision:**
   ```clojure
   ;; For visualization (default)
   (af-print-array-gen \"data\" arr 4)
   
   ;; For scientific accuracy
   (af-print-array-gen \"result\" arr 10)
   
   ;; For compactness
   (af-print-array-gen \"indices\" arr 0)
   ```
   
   ## Type Support
   
   ArrayFire printing supports all array types:
   
   ### Real Types
   - **f32** (float): 1.2345
   - **f64** (double): 1.234567890123
   - **f16** (half): 1.234 (limited precision)
   
   ### Integer Types
   - **s32** (int): -12345
   - **u32** (unsigned): 12345
   - **s64** (long): -1234567890
   - **u64** (ulong): 1234567890
   - **s16** (short): -12345
   - **u16** (ushort): 12345
   - **s8** (char): -123
   - **u8** (uchar): 255
   
   ### Complex Types
   - **c32** (cfloat): (1.23, 4.56)
   - **c64** (cdouble): (1.234567, 8.901234)
   
   Format: (real, imag)
   
   ### Boolean Type
   - **b8** (bool): 0 or 1
   
   ### Sparse Arrays
   
   Sparse arrays print additional information:
   ```
   mySparseArray
   Storage Format : AF_STORAGE_CSR
   [1000 1000 1 1]
   mySparseArray: Values
   [500 1 1 1]
   1.5  2.3  0.8 ...
   
   mySparseArray: RowIdx
   [500 1 1 1]
   0  5  12 ...
   
   mySparseArray: ColIdx
   [500 1 1 1]
   3  7  15 ...
   ```
   
   Shows:
   - Storage format (CSR, CSC, COO)
   - Non-zero values
   - Row/column indices
   - Compressed representation
   
   ## Performance Considerations
   
   ### Memory Usage
   
   **af-print-array, af-print-array-gen:**
   - Temporary: Copy to host memory (~array size)
   - Brief: Released immediately after print
   - Efficient: Direct output, no string allocation
   
   **af-array-to-string:**
   - Allocates: String buffer (varies with size/precision)
   - Persistent: Caller must free with af-free-host
   - Overhead: String formatting costs
   
   **Large arrays:**
   - 10×10: ~1 KB host memory
   - 100×100: ~100 KB host memory
   - 1000×1000: ~10 MB host memory
   - Output truncated, but full copy made
   
   ### Time Complexity
   
   **GPU to Host Transfer:**
   - O(N) where N = array elements
   - Dominant cost for large arrays
   - 10-100 μs for small arrays (< 1000 elements)
   - 1-10 ms for medium arrays (10k-1M elements)
   - 10-100 ms for large arrays (> 1M elements)
   
   **Formatting:**
   - O(N × P) where P = precision
   - String conversion and formatting
   - Negligible compared to transfer
   
   **I/O:**
   - stdout printing: 1-10 ms typical
   - Can be bottleneck for very large output
   - Buffering helps with repeated prints
   
   ### Best Practices
   
   1. **Avoid in production loops:**
      ```clojure
      ;; Bad: Print every iteration
      (dotimes [i 10000]
        (let [result (compute-step i)]
          (af-print-array-gen \"step\" result 4)))
      
      ;; Good: Print selectively
      (dotimes [i 10000]
        (let [result (compute-step i)]
          (when (= 0 (mod i 1000))
            (af-print-array-gen \"checkpoint\" result 4))))
      ```
   
   2. **Use appropriate precision:**
      ```clojure
      ;; Wasteful: Too much precision for f32
      (af-print-array-gen \"data\" float32-arr 15)
      
      ;; Appropriate
      (af-print-array-gen \"data\" float32-arr 6)
      ```
   
   3. **Prefer quick print for exploration:**
      ```clojure
      ;; Quick inspection
      (af-print-array arr)  ; Fast, no string allocation
      
      ;; Detailed when needed
      (af-print-array-gen \"detailed\" arr 8)
      ```
   
   4. **Free string memory promptly:**
      ```clojure
      (let [str-ptr (mem/alloc-pointer ::mem/pointer)
            _ (af-array-to-string str-ptr \"data\" arr 4 true)
            str-addr (mem/read-pointer str-ptr ::mem/pointer)]
        (try
          (let [s (mem/read-string str-addr)]
            (process-string s))
          (finally
            (af-free-host str-addr))))
      ```
   
   ## Common Use Cases
   
   ### 1. Quick Debugging
   
   ```clojure
   (defn debug-computation [x]
     (let [step1 (transform x)
           _ (af-print-array step1)  ; Quick check
           step2 (filter-data step1)
           _ (af-print-array step2)  ; Another check
           result (aggregate step2)]
       result))
   ```
   
   ### 2. Labeled Output
   
   ```clojure
   (let [input (create-array data [100 100])
         output (process input)]
     (af-print-array-gen \"Input data\" input 4)
     (af-print-array-gen \"Processed result\" output 4)
     (af-print-array-gen \"Difference\" 
                        (af-sub output input) 6))
   ```
   
   ### 3. Logging
   
   ```clojure
   (defn log-array-state [name arr]
     (let [str-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-array-to-string str-ptr name arr 4 true)
           str-addr (mem/read-pointer str-ptr ::mem/pointer)
           s (mem/read-string str-addr)]
       (try
         (log/info s)
       (finally
         (af-free-host str-addr)))))
   ```
   
   ### 4. Testing Verification
   
   ```clojure
   (deftest matrix-multiply-test
     (let [a (create-array [[1 2] [3 4]] [2 2])
           b (create-array [[5 6] [7 8]] [2 2])
           result (af-matmul a b 0 0)
           expected (create-array [[19 22] [43 50]] [2 2])]
       
       ;; Print for debugging if test fails
       (when-not (arrays-equal? result expected)
         (af-print-array-gen \"Result\" result 4)
         (af-print-array-gen \"Expected\" expected 4))
       
       (is (arrays-equal? result expected))))
   ```
   
   ### 5. Educational/Demo Code
   
   ```clojure
   (defn demonstrate-fft []
     (println \"\\nFFT Demonstration\")
     (let [signal (af-randu [16] f32)]
       (af-print-array-gen \"Original signal\" signal 4)
       
       (let [freq (af-fft signal 1.0 0)]
         (af-print-array-gen \"Frequency domain\" freq 4)
         
         (let [reconstructed (af-ifft freq 1.0 0)]
           (af-print-array-gen \"Reconstructed\" reconstructed 4)))))
   ```
   
   ### 6. Conditional Debug Prints
   
   ```clojure
   (def debug-mode (atom false))
   
   (defn debug-print [name arr]
     (when @debug-mode
       (af-print-array-gen name arr 4)))
   
   (defn complex-algorithm [data]
     (debug-print \"Input\" data)
     (let [step1 (transform data)]
       (debug-print \"After transform\" step1)
       (let [step2 (filter-step step1)]
         (debug-print \"After filter\" step2)
         (finalize step2))))
   ```
   
   ### 7. Array Comparison
   
   ```clojure
   (defn compare-arrays [name1 arr1 name2 arr2]
     (println \"\\n=== Array Comparison ===\")
     (af-print-array-gen name1 arr1 6)
     (af-print-array-gen name2 arr2 6)
     (let [diff (af-sub arr1 arr2)]
       (af-print-array-gen \"Difference\" diff 6)
       (let [max-diff (af-max diff)]
         (println \"Max difference:\" max-diff))))
   ```
   
   ## Integration with REPL Development
   
   Printing is especially valuable in REPL-driven development:
   
   ```clojure
   ;; In REPL
   user=> (def data (af-randu [5 5] f32))
   user=> (af-print-array data)
   No Name Array
   [5 5 1 1]
   0.3745  0.9507  0.7320  0.5987  0.1560
   0.2230  0.9999  0.2785  0.1576  0.1559
   0.4474  0.0871  0.5469  0.9706  0.9124
   0.4968  0.0202  0.5144  0.1193  0.2787
   0.3906  0.8326  0.1235  0.5467  0.0540
   
   user=> (af-print-array-gen \"Random data\" data 2)
   Random data
   [5 5 1 1]
   0.37  0.95  0.73  0.60  0.16
   0.22  1.00  0.28  0.16  0.16
   0.45  0.09  0.55  0.97  0.91
   0.50  0.02  0.51  0.12  0.28
   0.39  0.83  0.12  0.55  0.05
   ```
   
   Benefits:
   - Immediate feedback
   - Interactive exploration
   - Quick verification
   - Iterative development
   
   ## Memory Management for af-array-to-string
   
   The af-array-to-string function allocates memory that must be freed:
   
   **Allocation:**
   - ArrayFire allocates via af_alloc_host
   - Pointer returned in output parameter
   - Size: Variable (depends on array and formatting)
   
   **Deallocation:**
   - Must call af-free-host on returned pointer
   - Failure to free causes memory leak
   - Use try/finally for guaranteed cleanup
   
   **Correct Pattern:**
   ```clojure
   (let [str-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-array-to-string str-ptr \"data\" arr 4 true)
         str-addr (mem/read-pointer str-ptr ::mem/pointer)]
     (try
       (let [s (mem/read-string str-addr)]
         ;; Use string
         (process s))
       (finally
         ;; Always free
         (af-free-host str-addr))))
   ```
   
   **Common Mistakes:**
   ```clojure
   ;; BAD: Memory leak
   (let [str-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-array-to-string str-ptr \"data\" arr 4 true)
         str-addr (mem/read-pointer str-ptr ::mem/pointer)
         s (mem/read-string str-addr)]
     (process s))  ; Never freed!
   
   ;; BAD: Potential leak on exception
   (let [str-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-array-to-string str-ptr \"data\" arr 4 true)
         str-addr (mem/read-pointer str-ptr ::mem/pointer)
         s (mem/read-string str-addr)]
     (process s)  ; Exception here means free never called
     (af-free-host str-addr))
   ```
   
   ## Comparison with Other Display Methods
   
   | Method | Speed | Memory | Control | Use Case |
   |--------|-------|--------|---------|----------|
   | af-print-array | Fast | Low | None | Quick inspection |
   | af-print-array-gen | Fast | Low | Name + precision | Labeled debugging |
   | af-array-to-string | Slower | Higher | Full | Logging, storage |
   | af-save-array | Slow | Disk | Binary | Persistence |
   | Custom formatter | Varies | Varies | Total | Special needs |
   
   **When to use each:**
   - **Quick check**: af-print-array
   - **Development**: af-print-array-gen
   - **Logging**: af-array-to-string
   - **Persistence**: af-save-array
   - **Custom format**: Roll your own
   
   ## Empty Arrays
   
   Empty arrays (0 elements) display specially:
   ```
   EmptyArray
   [0 100 1 1]
   <empty>
   ```
   
   Or:
   ```
   ZeroDim
   [100 0 1 1]
   <empty>
   ```
   
   Helps identify arrays with zero dimensions.
   
   ## Debugging Tips
   
   1. **Check dimensions first:**
      ```clojure
      ;; Dimensions wrong? Fix before looking at values
      (af-print-array-gen \"check-dims\" arr 0)
      ```
   
   2. **Use precision to spot patterns:**
      ```clojure
      ;; Low precision to see structure
      (af-print-array-gen \"structure\" arr 1)
      
      ;; High precision to see numerical issues
      (af-print-array-gen \"detailed\" arr 12)
      ```
   
   3. **Print intermediate steps:**
      ```clojure
      (-> input
          (transform1)
          (tap> #(af-print-array-gen \"after-t1\" % 4))
          (transform2)
          (tap> #(af-print-array-gen \"after-t2\" % 4))
          (finalize))
      ```
   
   4. **Compare with expected:**
      ```clojure
      (let [result (compute)
            expected (load-expected)]
        (af-print-array-gen \"Result\" result 6)
        (af-print-array-gen \"Expected\" expected 6)
        (af-print-array-gen \"Diff\" (af-sub result expected) 8))
      ```
   
   5. **Check for NaN/Inf:**
      ```clojure
      (defn check-valid [name arr]
        (let [has-nan (> (af-any-true (af-isnan arr)) 0)
              has-inf (> (af-any-true (af-isinf arr)) 0)]
          (when (or has-nan has-inf)
            (println name \"has NaN or Inf:\")
            (af-print-array-gen name arr 4))))
      ```
   
   ## Best Practices Summary
   
   1. **Use af-print-array for quick inspection** during development
   2. **Use af-print-array-gen with meaningful names** for debugging
   3. **Choose precision appropriate to data type** (6 for f32, 12 for f64)
   4. **Avoid printing in hot loops** - it's slow
   5. **Always free af-array-to-string output** with af-free-host
   6. **Print intermediate results** in complex computations
   7. **Use conditional printing** (debug flags) for production
   8. **Transpose for readability** (default true)
   9. **Check dimensions first** before investigating values
   10. **Combine with af-eval** to force computation before print
   
   ## Limitations
   
   1. **Large arrays truncated**: Only representative data shown
   2. **GPU to host transfer**: Mandatory, can be slow
   3. **No column alignment**: Values may not align perfectly
   4. **Fixed format**: Cannot customize layout significantly
   5. **No color coding**: Plain text only
   6. **Stdout only**: af-print-* go to stdout (use af-array-to-string for logging)
   7. **Blocking**: Waits for print completion
   
   ## Related Functions
   
   - **af-save-array**: Save array to binary file
   - **af-read-array**: Load array from binary file  
   - **af-eval**: Force computation (useful before print)
   - **af-get-data-ptr**: Direct host memory access
   - **af-free-host**: Free host memory (for strings)
   
   See also:
   - Array creation functions for test data
   - Math functions for computing statistics
   - Comparison functions for validation"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; af_err af_print_array(af_array arr)
(defcfn af-print-array
  "Print an array to standard output with default formatting.
   
   Prints array directly to stdout (console) with fixed precision (4 decimals).
   Quick and simple for debugging and exploration.
   
   Parameters:
   - arr: array handle to print
   
   Output Format:
   ```
   No Name Array
   [rows cols depth batch]
   data...
   ```
   
   Example 1: Quick inspection
   ```clojure
   (let [data (af-randu [5 5] f32)]
     (af-print-array data))
   ;; Output:
   ;; No Name Array
   ;; [5 5 1 1]
   ;; 0.3745  0.9507  0.7320  0.5987  0.1560
   ;; 0.2230  0.9999  0.2785  0.1576  0.1559
   ;; ...
   ```
   
   Example 2: Debug intermediate result
   ```clojure
   (defn process-data [input]
     (let [step1 (transform input)
           _ (af-print-array step1)  ; Quick check
           step2 (filter-data step1)]
       step2))
   ```
   
   Example 3: REPL exploration
   ```clojure
   user=> (def x (af-range [10] 0 f32))
   user=> (af-print-array x)
   No Name Array
   [10 1 1 1]
   0.0000  1.0000  2.0000  3.0000  4.0000  5.0000  6.0000  7.0000  8.0000  9.0000
   ```
   
   Features:
   - **Fast**: No string allocation, direct output
   - **Simple**: No parameters needed
   - **Fixed format**: 4 decimal precision, transposed display
   - **Stdout**: Output goes to console
   
   Use Cases:
   - Quick debugging during development
   - REPL exploration
   - Checking array shape and type
   - Verifying computation results
   
   Precision: Fixed at 4 decimal places
   
   Display: Always transposed (rows × cols orientation)
   
   Type Support: All types (f32, f64, c32, c64, s32, u32, s16, u16, s8, u8, b8, f16)
   
   Sparse Arrays: Prints storage format and indices
   
   Empty Arrays: Shows \"<empty>\" for zero-element arrays
   
   Performance:
   - Small arrays (< 1000 elements): < 1 ms
   - Medium arrays (10k elements): 1-5 ms
   - Large arrays (1M elements): 10-50 ms
   - Dominated by GPU-to-host transfer
   
   Memory: Temporary host copy (released immediately)
   
   When to Use:
   - ✓ Quick inspection
   - ✓ REPL development
   - ✓ Initial debugging
   - ✗ Need custom precision
   - ✗ Need array name
   - ✗ Production logging
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-print-array-gen: Print with name and custom precision
   - af-array-to-string: Convert to string for logging"
  "af_print_array" [::mem/pointer] ::mem/int)

;; af_err af_print_array_gen(const char *exp, const af_array arr, const int precision)
(defcfn af-print-array-gen
  "Print an array with custom name and precision.
   
   Prints array to standard output with configurable precision and a
   descriptive name/expression. Better for labeled debugging output.
   
   Parameters:
   - exp: C string with array name/expression to display
   - arr: array handle to print
   - precision: number of decimal places (0-15+)
   
   Output Format:
   ```
   ArrayName
   [rows cols depth batch]
   data with specified precision...
   ```
   
   Example 1: Named output
   ```clojure
   (let [input (create-array data [100 50])
         result (process input)]
     (af-print-array-gen \"Input data\" input 4)
     (af-print-array-gen \"Processing result\" result 4))
   ;; Output:
   ;; Input data
   ;; [100 50 1 1]
   ;; 1.2345  2.3456  ...
   ;;
   ;; Processing result
   ;; [100 50 1 1]
   ;; 5.6789  6.7890  ...
   ```
   
   Example 2: Precision control
   ```clojure
   (let [measurements (load-sensor-data)]
     ;; Low precision for overview
     (af-print-array-gen \"Overview\" measurements 2)
     ;; High precision for analysis
     (af-print-array-gen \"Detailed\" measurements 10))
   ```
   
   Example 3: Intermediate steps
   ```clojure
   (defn debug-pipeline [data]
     (af-print-array-gen \"1. Raw input\" data 4)
     (let [normalized (normalize data)]
       (af-print-array-gen \"2. Normalized\" normalized 6)
       (let [filtered (apply-filter normalized)]
         (af-print-array-gen \"3. Filtered\" filtered 6)
         (let [result (final-transform filtered)]
           (af-print-array-gen \"4. Final result\" result 4)
           result))))
   ```
   
   Example 4: Comparing arrays
   ```clojure
   (let [computed (algorithm-result)
         expected (load-reference)]
     (af-print-array-gen \"Computed\" computed 8)
     (af-print-array-gen \"Expected\" expected 8)
     (af-print-array-gen \"Difference\" 
                        (af-sub computed expected) 10))
   ```
   
   Example 5: Type-appropriate precision
   ```clojure
   (let [f32-data (af-randu [10 10] f32)
         f64-data (af-randu [10 10] f64)]
     ;; Float32: ~7 significant digits
     (af-print-array-gen \"Float32\" f32-data 6)
     ;; Float64: ~16 significant digits  
     (af-print-array-gen \"Float64\" f64-data 14))
   ```
   
   Example 6: Debug with precision 0 (integers)
   ```clojure
   (let [indices (af-range [20] 0 s32)]
     ;; Precision 0 for integer display
     (af-print-array-gen \"Array indices\" indices 0))
   ;; Output:
   ;; Array indices
   ;; [20 1 1 1]
   ;; 0  1  2  3  4  5  6  7  8  9  10  11  12  13  14  15  16  17  18  19
   ```
   
   Example 7: Complex numbers
   ```clojure
   (let [spectrum (af-fft signal 1.0 0)]
     (af-print-array-gen \"Frequency spectrum\" spectrum 4))
   ;; Output:
   ;; Frequency spectrum
   ;; [256 1 1 1]
   ;; (1.2345, 2.3456)  (3.4567, 4.5678)  ...
   ```
   
   Precision Guidelines:
   
   | Precision | Decimals | Use Case |
   |-----------|----------|----------|
   | 0 | 0 | Indices, counts |
   | 2 | 0.00 | Percentages |
   | 4 | 0.0000 | Default (general) |
   | 6 | 0.000000 | Float32 maximum |
   | 8 | 0.00000000 | Scientific data |
   | 10-12 | 0.0000000000 | High precision |
   | 14-15 | 0.00000000000000 | Float64 near max |
   
   Float32 (f32):
   - Effective precision: ~7 significant digits
   - Recommended: precision ≤ 6
   - Higher values show noise in least significant bits
   
   Float64 (f64):
   - Effective precision: ~16 significant digits
   - Recommended: precision ≤ 14
   - Can use up to 15-16 for maximum accuracy
   
   Integer types:
   - Precision affects spacing, not representation
   - Use precision 0 for compact display
   - Higher precision adds spacing
   
   Complex types:
   - Precision applies to both real and imaginary parts
   - Format: (real, imag) with specified decimals
   
   Name/Expression:
   - Can be any descriptive string
   - Often variable name: \"myArray\"
   - Can be expression: \"result * 2\"
   - Can be description: \"Filtered sensor data\"
   - Helps identify output in logs
   
   Type Support: All types
   
   Sparse Arrays: 
   - Shows storage format (CSR, CSC, COO)
   - Prints values, row indices, column indices separately
   - Each component labeled with array name prefix
   
   Display: Always transposed (default, row-major appearance)
   
   Performance: Same as af-print-array (GPU-to-host transfer dominates)
   
   Memory: Temporary host copy (released after print)
   
   When to Use:
   - ✓ Labeled debugging output
   - ✓ Need custom precision
   - ✓ Comparing multiple arrays
   - ✓ Step-by-step algorithm debugging
   - ✓ Documentation/examples
   - ✗ Need string for logging (use af-array-to-string)
   
   Best Practices:
   1. Use descriptive names for clarity
   2. Match precision to data type (6 for f32, 12+ for f64)
   3. Lower precision for overview, higher for analysis
   4. Print intermediate steps in complex algorithms
   5. Compare related arrays with same precision
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-print-array: Quick print with defaults
   - af-array-to-string: Get string for logging/storage"
  "af_print_array_gen" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_array_to_string(char **output, const char *exp, const af_array arr, const int precision, const bool transpose)
(defcfn af-array-to-string
  "Convert array to string representation.
   
   Converts array to formatted string for programmatic use (logging, storage,
   transmission). Unlike af-print-*, returns string instead of printing.
   
   Parameters:
   - output: out pointer to receive string pointer (char**)
   - exp: C string with array name/expression
   - arr: array handle to convert
   - precision: decimal places (0-15+)
   - transpose: whether to transpose display (true=row-major, false=col-major)
   
   String Format: Same as af-print-array-gen output
   
   Memory Management:
   **CRITICAL**: Must free returned string with af-free-host!
   
   Example 1: Basic string conversion
   ```clojure
   (let [data (af-randu [5 5] f32)
         str-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-array-to-string str-ptr \"random\" data 4 true)
         str-addr (mem/read-pointer str-ptr ::mem/pointer)]
     (try
       (let [s (mem/read-string str-addr)]
         (println \"Array as string:\")
         (println s))
       (finally
         (af-free-host str-addr))))
   ```
   
   Example 2: Logging to file
   ```clojure
   (defn log-array-to-file [filename arr-name arr precision]
     (let [str-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-array-to-string str-ptr arr-name arr precision true)
           str-addr (mem/read-pointer str-ptr ::mem/pointer)]
       (try
         (let [s (mem/read-string str-addr)]
           (spit filename s :append true))
         (finally
           (af-free-host str-addr)))))
   
   (log-array-to-file \"results.log\" \"Iteration 1\" result 6)
   ```
   
   Example 3: Send over network
   ```clojure
   (defn transmit-array [socket arr-name arr]
     (let [str-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-array-to-string str-ptr arr-name arr 4 true)
           str-addr (mem/read-pointer str-ptr ::mem/pointer)]
       (try
         (let [s (mem/read-string str-addr)
               bytes (.getBytes s \"UTF-8\")]
           (send-over-socket socket bytes))
         (finally
           (af-free-host str-addr)))))
   ```
   
   Example 4: Build report
   ```clojure
   (defn generate-report [data-map]
     (let [report (StringBuilder.)]
       (doseq [[name arr] data-map]
         (let [str-ptr (mem/alloc-pointer ::mem/pointer)
               _ (af-array-to-string str-ptr name arr 6 true)
               str-addr (mem/read-pointer str-ptr ::mem/pointer)]
           (try
             (.append report (mem/read-string str-addr))
             (.append report \"\\n\\n\")
             (finally
               (af-free-host str-addr)))))
       (.toString report)))
   
   (let [report (generate-report {\"Input\" input-arr
                                   \"Output\" output-arr
                                   \"Error\" error-arr})]
     (save-report \"analysis.txt\" report))
   ```
   
   Example 5: Conditional logging
   ```clojure
   (defn conditional-log [name arr threshold]
     (let [max-val (af-max arr)]
       (when (> max-val threshold)
         (let [str-ptr (mem/alloc-pointer ::mem/pointer)
               _ (af-array-to-string str-ptr name arr 8 true)
               str-addr (mem/read-pointer str-ptr ::mem/pointer)]
           (try
             (let [s (mem/read-string str-addr)]
               (log/warn \"Threshold exceeded:\" s))
             (finally
               (af-free-host str-addr)))))))
   ```
   
   Example 6: Transposed vs non-transposed
   ```clojure
   (let [matrix (create-array [[1 2 3]
                               [4 5 6]] [2 3])
         str-ptr1 (mem/alloc-pointer ::mem/pointer)
         str-ptr2 (mem/alloc-pointer ::mem/pointer)
         
         _ (af-array-to-string str-ptr1 \"Transposed\" matrix 2 true)
         _ (af-array-to-string str-ptr2 \"Column-major\" matrix 2 false)
         
         addr1 (mem/read-pointer str-ptr1 ::mem/pointer)
         addr2 (mem/read-pointer str-ptr2 ::mem/pointer)]
     (try
       (println (mem/read-string addr1))
       (println (mem/read-string addr2))
       (finally
         (af-free-host addr1)
         (af-free-host addr2))))
   
   ;; Transposed (true): Row-major appearance
   ;; 1.00  2.00  3.00
   ;; 4.00  5.00  6.00
   ;;
   ;; Column-major (false): Internal storage order
   ;; 1.00  4.00
   ;; 2.00  5.00
   ;; 3.00  6.00
   ```
   
   Example 7: Helper function with automatic cleanup
   ```clojure
   (defn array->string 
     \"Convert array to string with automatic memory management.\"
     [name arr precision transpose]
     (let [str-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-array-to-string str-ptr name arr precision transpose)
           str-addr (mem/read-pointer str-ptr ::mem/pointer)]
       (try
         (mem/read-string str-addr)
         (finally
           (af-free-host str-addr)))))
   
   ;; Usage:
   (let [s (array->string \"data\" my-array 6 true)]
     (process-string s))
   ```
   
   Memory Management (CRITICAL):
   
   **Allocation:**
   - ArrayFire allocates string via af_alloc_host
   - Size depends on array dimensions and precision
   - Pointer returned in `output` parameter
   
   **Deallocation:**
   - MUST call af-free-host on returned pointer
   - Failure causes memory leak
   - Use try/finally for guaranteed cleanup
   
   **Correct Pattern:**
   ```clojure
   (let [str-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-array-to-string str-ptr \"name\" arr precision transpose)
         str-addr (mem/read-pointer str-ptr ::mem/pointer)]
     (try
       ;; Use string
       (process (mem/read-string str-addr))
       (finally
         ;; Always free
         (af-free-host str-addr))))
   ```
   
   **Wrong Pattern (memory leak):**
   ```clojure
   ;; BAD: No cleanup
   (let [str-ptr (mem/alloc-pointer ::mem/pointer)
         _ (af-array-to-string str-ptr \"name\" arr 4 true)
         str-addr (mem/read-pointer str-ptr ::mem/pointer)]
     (mem/read-string str-addr))  ; Leak!
   ```
   
   Transpose Parameter:
   
   **true (default, recommended):**
   - Displays in row-major order
   - Natural reading: rows × columns
   - Matches mathematical notation
   - Most intuitive for humans
   
   **false (column-major):**
   - Shows internal storage layout
   - ArrayFire's native memory order
   - Useful for understanding storage
   - Less intuitive visually
   
   When transpose matters:
   - Matrices: true for natural row×col view
   - Debugging memory: false to see actual layout
   - Documentation: true for readability
   - Storage analysis: false for accuracy
   
   Precision: Same guidelines as af-print-array-gen
   
   Type Support: All types (dense and sparse)
   
   Performance:
   - Similar to af-print-array-gen
   - Additional: String allocation overhead
   - Time: GPU transfer + formatting + allocation
   - Memory: String buffer persists until freed
   
   String Size Estimate:
   - Roughly: elements × (precision + 10) bytes
   - 10×10 f32, precision=4: ~4 KB
   - 100×100 f64, precision=6: ~160 KB
   - Plus metadata (name, dimensions)
   
   Use Cases:
   - ✓ Logging arrays to files
   - ✓ Transmitting array data
   - ✓ Building reports
   - ✓ Storing array snapshots
   - ✓ Test result capture
   - ✓ Programmatic processing
   - ✗ Simple console output (use af-print-*)
   
   Common Patterns:
   
   **1. Logging wrapper:**
   ```clojure
   (defn log-array [logger level name arr]
     (let [str-ptr (mem/alloc-pointer ::mem/pointer)
           _ (af-array-to-string str-ptr name arr 6 true)
           str-addr (mem/read-pointer str-ptr ::mem/pointer)]
       (try
         (log logger level (mem/read-string str-addr))
         (finally
           (af-free-host str-addr)))))
   ```
   
   **2. Assertion message:**
   ```clojure
   (defn assert-array-close [expected actual tolerance]
     (when-not (arrays-close? expected actual tolerance)
       (let [msg (str \"Arrays not close:\\n\"
                      (array->string \"Expected\" expected 8 true)
                      (array->string \"Actual\" actual 8 true))]
         (throw (ex-info msg {:expected expected :actual actual})))))
   ```
   
   **3. Batch processing:**
   ```clojure
   (defn log-all-arrays [arrays]
     (doseq [[name arr] arrays]
       (let [str-ptr (mem/alloc-pointer ::mem/pointer)
             _ (af-array-to-string str-ptr name arr 4 true)
             str-addr (mem/read-pointer str-ptr ::mem/pointer)]
         (try
           (log/info (mem/read-string str-addr))
           (finally
             (af-free-host str-addr))))))
   ```
   
   Returns:
   ArrayFire error code (af_err enum)
   
   Output Parameter:
   - output: Pointer to allocated C string
   - Must be freed with af-free-host
   - String is null-terminated
   
   See also:
   - af-print-array: Quick console output
   - af-print-array-gen: Named console output
   - af-free-host: Free allocated string memory
   - af-save-array: Binary array persistence"
  "af_array_to_string" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/int] ::mem/int)
