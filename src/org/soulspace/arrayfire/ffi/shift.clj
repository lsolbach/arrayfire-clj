(ns org.soulspace.arrayfire.ffi.shift
  "Bindings for the ArrayFire shift function.
   
   Array shifting performs circular shifts (rotation) of array elements
   along specified dimensions. Elements shifted beyond array boundaries
   wrap around to the opposite side.
   
   **Shift Operation**:
   
   A shift moves array elements by a specified offset along each dimension.
   When elements move past the array boundary, they reappear at the
   opposite boundary (circular/cyclic shift).
   
   **Mathematical Definition**:
   
   For 1D array A of length n with shift s:
   - Output[i] = Input[(i - s) mod n]
   - Positive shift: elements move right (indices increase)
   - Negative shift: elements move left (indices decrease)
   
   For multidimensional arrays, shifts are applied independently per dimension.
   
   **Shift Semantics**:
   
   - **Positive shift (+s)**: Moves elements toward higher indices
     * [1,2,3,4] with shift +1 → [4,1,2,3]
     * Elements wrap from end to beginning
   
   - **Negative shift (-s)**: Moves elements toward lower indices
     * [1,2,3,4] with shift -1 → [2,3,4,1]
     * Elements wrap from beginning to end
   
   - **Zero shift (0)**: No change along that dimension
   
   **Use Cases**:
   
   1. **Circular Buffer Operations**:
      - Audio/signal processing ring buffers
      - Time-series sliding windows
   
   2. **Image Processing**:
      - Image translation with wrapping
      - Periodic boundary conditions
      - Texture tiling
   
   3. **FFT Operations**:
      - Frequency domain centering (fftshift)
      - Moving zero-frequency component to center
   
   4. **Data Alignment**:
      - Phase alignment in signals
      - Cyclic permutations
   
   **Performance**:
   - O(n) complexity - all elements copied
   - GPU-accelerated memory operations
   - Efficient for all array sizes
   - No data sorting or computation, pure memory movement
   
   **Multidimensional Shifts**:
   
   Each dimension can be shifted independently:
   ```
   Original 2D array:      Shift (1, 1):
   [1 2 3]                 [9 7 8]
   [4 5 6]        →        [3 1 2]
   [7 8 9]                 [6 4 5]
   ```
   
   **Boundary Behavior**:
   
   Shift always uses circular/periodic boundaries:
   - No zero-padding or clamping
   - Data wraps around like a torus topology
   - Preserves all original values
   
   **Type Support**:
   - All types supported: f16, f32, f64, c32, c64, b8, s8-s64, u8-u64
   - Works on any dimensional array (1D-4D)
   
   **Examples**:
   
   ```clojure
   ;; 1D shift right by 2
   (af-shift out [1 2 3 4 5] 2 0 0 0)
   ;; Result: [4 5 1 2 3]
   
   ;; 2D shift: 1 row down, 1 col right
   (af-shift out matrix-2d 1 1 0 0)
   
   ;; Center FFT spectrum (fftshift equivalent)
   (let [n (quot width 2)
         m (quot height 2)]
     (af-shift out fft-result n m 0 0))
   ```
   
   See also:
   - af-reorder: Permute dimensions (transpose)
   - af-flip: Reverse elements along dimension
   - af-tile: Replicate array along dimensions"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; af_err af_shift(af_array *out, const af_array in, const int x, const int y, const int z, const int w)
(defcfn af-shift
  "Circular shift (rotate) array elements along specified dimensions.
   
   Performs circular shifts along each dimension independently. Elements
   that move past array boundaries wrap around to the opposite side,
   implementing periodic boundary conditions.
   
   Parameters:
   - out: Output pointer for shifted array
   - in: Input array (any dimensions, any type)
   - x: Shift amount along first dimension (width/columns)
     * Positive: shift right/forward
     * Negative: shift left/backward
     * Zero: no shift
   - y: Shift amount along second dimension (height/rows)
   - z: Shift amount along third dimension (depth)
   - w: Shift amount along fourth dimension
   
   Shift Semantics:
   - Positive values shift toward higher indices (right/down)
   - Negative values shift toward lower indices (left/up)
   - Shift amount can exceed array size (wraps multiple times)
   - Effective shift = shift_amount mod dimension_size
   
   Algorithm:
   For each dimension d with size n and shift s:
   1. Compute effective shift: s_eff = s mod n
   2. Map output[i] = input[(i - s_eff + n) mod n]
   
   Performance:
   - O(N) where N = total elements
   - Memory-bound operation (no computation)
   - Efficient GPU memory copy with wrapping
   - Same cost regardless of shift amount
   
   Example (1D):
   ```clojure
   ;; Original: [1 2 3 4 5]
   (let [arr (create-array [1 2 3 4 5] [5])
         out-ptr (mem/alloc-pointer ::mem/pointer)]
     
     ;; Shift right by 2
     (af-shift out-ptr arr 2 0 0 0)
     ;; Result: [4 5 1 2 3]
     
     ;; Shift left by 1 (same as right by 4)
     (af-shift out-ptr arr -1 0 0 0)
     ;; Result: [2 3 4 5 1]
     
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (2D):
   ```clojure
   ;; Original 3x3:
   ;; [1 2 3]
   ;; [4 5 6]
   ;; [7 8 9]
   
   (let [matrix (create-array data [3 3])
         out-ptr (mem/alloc-pointer ::mem/pointer)]
     
     ;; Shift: 1 column right, 1 row down
     (af-shift out-ptr matrix 1 1 0 0)
     ;; Result:
     ;; [9 7 8]
     ;; [3 1 2]
     ;; [6 4 5]
     
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Example (FFT centering):
   ```clojure
   ;; Center zero-frequency component (fftshift)
   (let [fft-result (af-fft2 image)
         [h w] (get-dims fft-result)
         out-ptr (mem/alloc-pointer ::mem/pointer)]
     
     ;; Shift by half dimensions
     (af-shift out-ptr fft-result 
               (quot w 2) (quot h 2) 0 0)
     
     (mem/read-pointer out-ptr ::mem/pointer))
   ```
   
   Applications:
   - Circular buffer rotation for signal processing
   - Image translation with periodic boundaries
   - FFT spectrum centering (fftshift)
   - Data alignment in cross-correlation
   - Cyclic permutations for algorithm testing
   - Phase shift in periodic signals
   
   Type Support:
   - All numeric types: f16, f32, f64, c32, c64
   - Integer types: s8, u8, s16, u16, s32, u32, s64, u64
   - Boolean: b8
   
   Notes:
   - Always circular/periodic boundaries (no zero-padding)
   - Shift amounts can be larger than array dimensions
   - Negative shifts equivalent to positive shift in opposite direction
   - Empty arrays (0 elements) return unchanged
   - For non-circular shifts, use indexing operations instead
   
   Returns:
   ArrayFire error code (af_err enum)
   
   See also:
   - af-reorder: Permute/transpose dimensions
   - af-flip: Reverse elements (non-circular)
   - af-tile: Replicate array content
   - af-moddims: Reshape array dimensions"
  "af_shift" [::mem/pointer ::mem/pointer ::mem/int ::mem/int ::mem/int ::mem/int] ::mem/int)
