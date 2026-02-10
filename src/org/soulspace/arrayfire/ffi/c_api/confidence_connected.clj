(ns org.soulspace.arrayfire.ffi.c-api.confidence-connected
  "Bindings for ArrayFire confidence connected components segmentation.
   
   Maps to src/api/c/confidence_connected.cpp in ArrayFire."
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]
            [org.soulspace.arrayfire.ffi.base.loader]))

;; af_err af_confidence_cc(af_array *out, const af_array in, const af_array seedx,
;;                         const af_array seedy, const unsigned radius,
;;                         const unsigned multiplier, const int iter,
;;                         const double segmented_value)
(defcfn af-confidence-cc
  "Segment an image using confidence connected components algorithm.
   
   Performs region growing image segmentation using confidence connected
   components. Starting from seed points, the algorithm iteratively expands
   regions based on statistical analysis of pixel intensities.
   
   Algorithm overview:
   1. Compute mean and variance in neighborhood around each seed point
   2. Define threshold range: [mean - multiplier*stddev, mean + multiplier*stddev]
   3. Grow region by including connected pixels within threshold
   4. Iteratively refine: recompute statistics using segmented region
   5. Stop when variance approaches zero or max iterations reached
   
   Parameters:
   - out: out pointer for segmented image array
   - in: input image array (expects f32, u32, u16, or u8 type)
   - seedx: array of x-coordinates of seed points (u32 type, 1D)
   - seedy: array of y-coordinates of seed points (u32 type, 1D)
   - radius: neighborhood radius around seed points (unsigned int)
   - multiplier: controls threshold range (unsigned int)
             Higher values = more permissive region growing
   - iter: maximum number of iterations (int)
   - segmented_value: value assigned to valid pixels in output (double)

   Returns:
   ArrayFire error code (AF_SUCCESS on success)
   
   Input requirements:
   - in: 2D image (ndims <= 2), non-complex, supported types: f32/u32/u16/u8
   - seedx, seedy: 1D arrays with same length (number of seed points)
   - seedx[i], seedy[i] represent coordinates of i-th seed point
   
   Output:
   - Same dimensions as input
   - Pixels in segmented region set to segmented_value
   - Other pixels set to 0
   
   Statistical parameters:
   - neighborhood_size = (2*radius + 1)^2
   - Initial threshold computed from all seed neighborhoods
   - Subsequent iterations refine based on segmented region statistics
   
   Algorithm behavior:
   - If initial variance ≈ 0: stops after first segmentation
   - If iteration variance ≈ 0: stops early (region stabilized)
   - Adjusts thresholds to include min/max seed intensities
   
   Use cases:
   - Medical image segmentation (tumor detection, organ boundaries)
   - Object extraction from noisy backgrounds
   - Region-based image analysis
   - Brain scan segmentation (depression.jpg example)
   
   Example parameters:
   - Medical imaging: radius=3, multiplier=2-3, iter=3-25
   - General segmentation: radius=3-5, multiplier=2-4, iter=10-30
   
   Performance considerations:
   - Larger radius = more computation per iteration
   - More iterations = better refinement but slower
   - Multiple seed points processed in batch
   
   Notes:
   - Seed points near borders may cause indexing issues (limitation)
   - Batch processing not yet supported
   - Uses flood fill internally for region growing
   - Employs integral images for efficient neighborhood statistics"
  "af_confidence_cc" [::mem/pointer ::mem/pointer ::mem/pointer ::mem/pointer ::mem/int ::mem/int ::mem/int ::mem/double] ::mem/int)
