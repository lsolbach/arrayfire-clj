(ns org.soulspace.arrayfire.api.machine-learning
  "Idiomatic Clojure machine-learning API for ArrayFire arrays.

   Provides GPU-accelerated primitives for training convolutional neural
   networks (CNNs), specifically the backward-pass gradient computations
   needed for backpropagation.

   ## CNN Training Workflow

   1. **Forward pass** — compute layer activations:
      ```clojure
      (require '[org.soulspace.arrayfire.api.signal-processing :as sp])
      (def output (sp/convolve2-nn signal filter [1 1] [1 1] [1 1]))
      ```

   2. **Compute loss and incoming gradient** at the output layer.

   3. **Backward pass** — compute parameter gradients (this namespace):
      ```clojure
      (require '[org.soulspace.arrayfire.api.machine-learning :as ml])

      ;; Gradient for updating weights
      (def dW (ml/filter-gradient loss-grad signal filter output [1 1] [1 1] [1 1]))

      ;; Gradient for backprop into the preceding layer
      (def dX (ml/data-gradient loss-grad signal filter output [1 1] [1 1] [1 1]))

      ;; Gradient for updating bias terms (if any)
      (def db (ml/bias-gradient loss-grad signal filter output [1 1] [1 1] [1 1]))

      ;; Or compute all three at once
      (def grads (ml/all-gradients loss-grad signal filter output [1 1] [1 1] [1 1]))
      ```

   4. **Parameter update** — apply an optimizer using the gradient maps.

   ## Gradient Types

   | Keyword    | Meaning                         | Shape                              |
   |------------|---------------------------------|------------------------------------|
   | `:filter`  | ∂L/∂W — for weight updates      | [kernel_h kernel_w channels nfilt] |
   | `:data`    | ∂L/∂X — backprop to prev layer  | [height width channels batch]      |
   | `:bias`    | ∂L/∂b — bias update             | [1 1 num_filters 1]                |
   | `:default` | Same as `:filter`               |                                    |

   ## Dimensional Conventions

   - Signal arrays:  `[height × width × channels × batch_size]`
   - Filter arrays:  `[kernel_h × kernel_w × channels × num_filters]`
   - strides, paddings, dilations: two-element vectors of longs, e.g. `[1 1]`

   ## Top-k

   For top-k selection (argmax, classification, beam search) see
   `org.soulspace.arrayfire.api.statistics/topk`.

   All functions must be called within a `with-arrayfire` region from
   `org.soulspace.arrayfire.api.core`."
  (:require [org.soulspace.arrayfire.api.core :as core :refer [assert-within-arrayfire!]]
            [org.soulspace.arrayfire.integration.unified-api.ml :as ml])
  (:import (org.soulspace.arrayfire.integration.base.resource AFArray)))

;;;
;;; CNN Backpropagation Gradient Functions
;;;

(defn convolve2-gradient
  "Compute backward-pass gradient of a 2D convolution for neural networks.

   Computes one of three gradient types depending on `gradient-type`:

   | `gradient-type` | Computes       | Returns shape                               |
   |-----------------|----------------|---------------------------------------------|
   | `:filter`       | ∂L/∂W weights  | [kernel_h kernel_w channels num_filters]    |
   | `:data`         | ∂L/∂X input   | [height width channels batch_size]          |
   | `:bias`         | ∂L/∂b bias term| [1 1 num_filters 1]                         |
   | `:default`      | same as :filter|                                             |

   Parameters:
     incoming-gradient — gradient from the next layer (∂L/∂output); same
                         shape as the convolution output: [h_out w_out nfilt batch]
     signal            — original input from the forward pass (AFArray)
     filter            — original filter/weights from the forward pass (AFArray)
     output            — output produced by the forward pass (AFArray)
     strides           — stride used in the forward pass, e.g. `[1 1]`
     paddings          — padding used in the forward pass, e.g. `[1 1]`
     dilations         — dilation used in the forward pass, e.g. `[1 1]`
     gradient-type     — one of `:filter`, `:data`, `:bias`, `:default`

   Returns:
     AFArray with the requested gradient.

   Example:
   ```clojure
   (af/with-arrayfire
     (let [output (sp/convolve2-nn signal filt [1 1] [1 1] [1 1])
           dW     (ml/convolve2-gradient loss-grad signal filt output
                                         [1 1] [1 1] [1 1] :filter)]
       ;; update: new-filter = filter - lr × dW
       ))
   ```

   See also: `filter-gradient`, `data-gradient`, `bias-gradient`, `all-gradients`."
  [^AFArray incoming-gradient
   ^AFArray signal
   ^AFArray filter
   ^AFArray output
   strides paddings dilations
   gradient-type]
  (assert-within-arrayfire! "convolve2-gradient")
  (ml/convolve2-gradient-nn incoming-gradient signal filter output
                            strides paddings dilations gradient-type))

(defn filter-gradient
  "Compute the filter/weight gradient ∂L/∂W for a 2D convolutional layer.

   The filter gradient is used to update the layer's learned weights during
   the optimisation step:  W_new = W - lr × (filter-gradient ...)

   Parameters:
     incoming-gradient — gradient from the next layer (AFArray)
     signal            — original input from the forward pass (AFArray)
     filter            — original filter/weights from the forward pass (AFArray)
     output            — output produced by the forward pass (AFArray)
     strides           — stride used in the forward pass, e.g. `[1 1]`
     paddings          — padding used in the forward pass, e.g. `[1 1]`
     dilations         — dilation used in the forward pass, e.g. `[1 1]`

   Returns:
     AFArray shaped `[kernel_h × kernel_w × channels × num_filters]`.

   Example:
   ```clojure
   (af/with-arrayfire
     (let [output (sp/convolve2-nn signal filt [1 1] [1 1] [1 1])
           dW     (ml/filter-gradient loss-grad signal filt output
                                      [1 1] [1 1] [1 1])]
       (af/sub filt (af/mul dW learning-rate))))
   ```"
  [^AFArray incoming-gradient
   ^AFArray signal
   ^AFArray filter
   ^AFArray output
   strides paddings dilations]
  (assert-within-arrayfire! "filter-gradient")
  (ml/filter-gradient incoming-gradient signal filter output
                      strides paddings dilations))

(defn data-gradient
  "Compute the input-data gradient ∂L/∂X for a 2D convolutional layer.

   The data gradient is propagated backward to the preceding layer, enabling
   multi-layer backpropagation.

   Parameters:
     incoming-gradient — gradient from the next layer (AFArray)
     signal            — original input from the forward pass (AFArray)
     filter            — original filter/weights from the forward pass (AFArray)
     output            — output produced by the forward pass (AFArray)
     strides           — stride used in the forward pass, e.g. `[1 1]`
     paddings          — padding used in the forward pass, e.g. `[1 1]`
     dilations         — dilation used in the forward pass, e.g. `[1 1]`

   Returns:
     AFArray shaped `[height × width × channels × batch_size]`.

   Example:
   ```clojure
   (af/with-arrayfire
     (let [output (sp/convolve2-nn signal filt [1 1] [1 1] [1 1])
           dX     (ml/data-gradient loss-grad signal filt output
                                    [1 1] [1 1] [1 1])]
       (backward-previous-layer dX)))
   ```"
  [^AFArray incoming-gradient
   ^AFArray signal
   ^AFArray filter
   ^AFArray output
   strides paddings dilations]
  (assert-within-arrayfire! "data-gradient")
  (ml/data-gradient incoming-gradient signal filter output
                    strides paddings dilations))

(defn bias-gradient
  "Compute the bias gradient ∂L/∂b for a 2D convolutional layer.

   One gradient value is produced per output channel, accumulated over the
   spatial and batch dimensions.

   Parameters:
     incoming-gradient — gradient from the next layer (AFArray)
     signal            — original input from the forward pass (AFArray)
     filter            — original filter/weights from the forward pass (AFArray)
     output            — output produced by the forward pass (AFArray)
     strides           — stride used in the forward pass, e.g. `[1 1]`
     paddings          — padding used in the forward pass, e.g. `[1 1]`
     dilations         — dilation used in the forward pass, e.g. `[1 1]`

   Returns:
     AFArray shaped `[1 × 1 × num_filters × 1]`.

   Example:
   ```clojure
   (af/with-arrayfire
     (let [output (sp/convolve2-nn signal filt [1 1] [1 1] [1 1])
           db     (ml/bias-gradient loss-grad signal filt output
                                    [1 1] [1 1] [1 1])]
       (af/sub bias (af/mul db learning-rate))))
   ```"
  [^AFArray incoming-gradient
   ^AFArray signal
   ^AFArray filter
   ^AFArray output
   strides paddings dilations]
  (assert-within-arrayfire! "bias-gradient")
  (ml/bias-gradient incoming-gradient signal filter output
                    strides paddings dilations))

(defn all-gradients
  "Compute all three CNN gradients — filter, data, and bias — in a single call.

   Convenience wrapper that computes all gradient types needed for a standard
   convolutional layer backward pass and returns them in a map.

   Parameters:
     incoming-gradient — gradient from the next layer (AFArray)
     signal            — original input from the forward pass (AFArray)
     filter            — original filter/weights from the forward pass (AFArray)
     output            — output produced by the forward pass (AFArray)
     strides           — stride used in the forward pass, e.g. `[1 1]`
     paddings          — padding used in the forward pass, e.g. `[1 1]`
     dilations         — dilation used in the forward pass, e.g. `[1 1]`

   Returns:
     Map with keys:
     - `:filter` — AFArray `[kernel_h kernel_w channels num_filters]`
     - `:data`   — AFArray `[height width channels batch_size]`
     - `:bias`   — AFArray `[1 1 num_filters 1]`

   Example:
   ```clojure
   (af/with-arrayfire
     (let [output (sp/convolve2-nn signal filt [1 1] [1 1] [1 1])
           {:keys [filter data bias]}
             (ml/all-gradients loss-grad signal filt output [1 1] [1 1] [1 1])]
       ;; Update weights and bias, propagate data gradient
       ))
   ```"
  [^AFArray incoming-gradient
   ^AFArray signal
   ^AFArray filter
   ^AFArray output
   strides paddings dilations]
  (assert-within-arrayfire! "all-gradients")
  (ml/all-gradients incoming-gradient signal filter output
                    strides paddings dilations))

(comment
  ;; ---------------------------------------------------------------------------
  ;; REPL exploration
  ;; ---------------------------------------------------------------------------

  (require '[org.soulspace.arrayfire.api.core :as af])
  (require '[org.soulspace.arrayfire.api.signal-processing :as sp])
  (require '[org.soulspace.arrayfire.api.machine-learning :as ml])

  ;; Minimal forward + backward example
  (af/with-arrayfire
    (let [signal (af/array [1.0 0.0 0.0 1.0
                            0.0 1.0 1.0 0.0] [2 2 2 1] :f64)
          filt   (af/array [1.0 0.0
                            0.0 1.0] [2 2 1 1] :f64)
          output (sp/convolve2-nn signal filt [1 1] [1 1] [1 1])
          out-dims (af/shape output)
          n      (reduce * out-dims)
          grad   (af/array (vec (repeat n 1.0)) out-dims :f64)

          dW     (ml/filter-gradient grad signal filt output [1 1] [1 1] [1 1])
          dX     (ml/data-gradient   grad signal filt output [1 1] [1 1] [1 1])
          db     (ml/bias-gradient   grad signal filt output [1 1] [1 1] [1 1])]
      {:dW-shape (af/shape dW)
       :dX-shape (af/shape dX)
       :db-shape (af/shape db)}))
  )

