(ns org.soulspace.arrayfire.ffi.flip
  "Bindings for the ArrayFire flip functions."
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; af_err af_flip(af_array *out, const af_array in, const unsigned dim)
(defcfn af-flip
  "Flip an array along a specified dimension.
   
   The flip function reverses the order of elements along the specified
   dimension. This is equivalent to reversing an array along an axis.
   
   Mathematical Operation:
   For a 1D array of length N, flipping produces:
     out[i] = in[N-1-i]  for all i ∈ [0, N-1]
   
   For multidimensional arrays, the reversal happens along the chosen
   dimension while other dimensions remain unchanged.
   
   Dimension Specification:
   - dim = 0: Flip within columns (reverse rows within each column)
   - dim = 1: Flip within rows (reverse columns within each row)
   - dim = 2: Flip along 3rd dimension
   - dim = 3: Flip along 4th dimension
   
   If the specified dimension exceeds the array's number of dimensions,
   the input array is returned unchanged.
   
   Common Use Cases:
   
   1. **Image Processing**:
      - Horizontal flip (dim=1): Mirror image left-right
      - Vertical flip (dim=0): Mirror image top-bottom
      - Used for data augmentation in machine learning
   
   2. **Signal Processing**:
      - Time reversal of signals
      - Creating symmetric patterns
   
   3. **Data Transformations**:
      - Reversing sequences
      - Creating mirror patterns for boundary conditions
   
   4. **Visualization**:
      - Correcting image orientation
      - Coordinate system transformations
   
   Implementation Details:
   - Zero-copy operation using indexing
   - No memory allocation for output
   - Uses ArrayFire's indexing infrastructure
   - O(1) operation - creates indexed view, not a copy
   - Lazy evaluation until actual values needed
   
   Performance:
   - Constant time O(1) for creating the flip view
   - Actual data reversal happens during evaluation
   - No temporary storage required
   - GPU-friendly operation
   
   Examples:
   
   1. Flip 1D array:
      ```clojure
      ;; Create array [1 2 3 4 5]
      (let [arr (af-constant 1 5 AF-DTYPE-F32)
            flipped (af-flip arr 0)]
        ;; Result: [5 4 3 2 1]
        )
      ```
   
   2. Horizontal flip (mirror left-right):
      ```clojure
      ;; Flip along dimension 1 (columns)
      (let [img (af-load-image \"photo.jpg\")
            h-flipped (af-flip img 1)]
        ;; Image mirrored horizontally
        (af-save-image \"photo_h_flip.jpg\" h-flipped))
      ```
   
   3. Vertical flip (mirror top-bottom):
      ```clojure
      ;; Flip along dimension 0 (rows)
      (let [img (af-load-image \"photo.jpg\")
            v-flipped (af-flip img 0)]
        ;; Image flipped vertically
        (af-save-image \"photo_v_flip.jpg\" v-flipped))
      ```
   
   4. Both horizontal and vertical flip (180° rotation):
      ```clojure
      ;; Flip along both dimensions
      (let [img (af-load-image \"photo.jpg\")
            flipped (-> img
                        (af-flip 0)  ; vertical flip
                        (af-flip 1))]  ; horizontal flip
        ;; Result is equivalent to 180-degree rotation
        (af-save-image \"photo_rotated_180.jpg\" flipped))
      ```
   
   5. Data augmentation for machine learning:
      ```clojure
      ;; Augment training data with flipped versions
      (defn augment-batch [images]
        (let [h-flips (mapv #(af-flip % 1) images)
              v-flips (mapv #(af-flip % 0) images)]
          (concat images h-flips v-flips)))
      ```
   
   6. Create symmetric boundary conditions:
      ```clojure
      ;; Extend signal symmetrically
      (defn symmetric-extension [signal]
        (let [flipped (af-flip signal 0)]
          (af-join 0 flipped signal flipped)))
      ```
   
   7. Reverse time series:
      ```clojure
      ;; Reverse temporal sequence
      (let [time-series (af-randu 100 1 AF-DTYPE-F32)
            reversed (af-flip time-series 0)]
        ;; Time flows backward
        )
      ```
   
   8. Multi-dimensional flip:
      ```clojure
      ;; Flip 3D volume data
      (let [volume (af-randu 64 64 64 AF-DTYPE-F32)
            ;; Flip along each dimension
            flip-x (af-flip volume 0)
            flip-y (af-flip volume 1)
            flip-z (af-flip volume 2)]
        ;; Each creates different mirror view
        )
      ```
   
   Related Functions:
   - af-transpose: Swap dimensions (not a flip)
   - af-rotate: Rotate by arbitrary angle
   - af-shift: Circular shift elements
   - af-reorder: Permute dimensions
   
   Type Support:
   - All data types supported (floating point, integer, complex, boolean)
   - Half precision (f16) supported
   - Preserves input data type
   
   Parameters:
   - out: out pointer for flipped array handle
   - in: input array handle
   - dim: dimension along which to flip (0-3, unsigned int)
   
   Returns:
   ArrayFire error code
   
   Notes:
   - If dim >= ndims(in), returns copy of input unchanged
   - Zero-copy operation - creates indexed view
   - Lazy evaluation - no immediate computation
   - Combine with af-rotate for 180° rotation (faster than rotate)
   - For 2D images: dim=0 is vertical flip, dim=1 is horizontal flip
   - Can be chained: flip(flip(A, d), d) = A (involution property)"
  "af_flip" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)
