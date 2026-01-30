(ns org.soulspace.arrayfire.integration.ml
  "Integration of the ArrayFire machine learning related FFI bindings with the error
   handling and resource management on the JVM.
   
   This namespace provides machine learning operations for training convolutional
   neural networks, specifically gradient computation for backpropagation.
   
   ## Gradient Types
   
   - :default (0) - Default gradient type (same as :filter)
   - :filter (1) - Gradient with respect to filter/weights (∂L/∂W)
   - :data (2) - Gradient with respect to input data (∂L/∂X)
   - :bias (3) - Gradient with respect to bias (∂L/∂b)
   
   ## CNN Training Workflow
   
   1. **Forward Pass**: Compute layer outputs
      - Use convolve2-nn for forward convolution
      - Apply activations, pooling, etc.
      - Compute loss at output
   
   2. **Backward Pass**: Compute gradients (this namespace)
      - Use convolve2-gradient-nn to compute gradients
      - Propagate gradients backward through network
      - Accumulate weight gradients
   
   3. **Parameter Update**: Update weights
      - Apply optimizer (SGD, Adam, etc.)
      - Update filters using computed gradients
   
   ## Key Concepts
   
   **Filter Gradient** (∂L/∂W):
   - Most important for training
   - Used to update convolutional layer weights
   - Computed by correlating input with incoming gradient
   
   **Data Gradient** (∂L/∂X):
   - Used for backpropagation to previous layers
   - Necessary for multi-layer networks
   - Computed via transposed convolution
   
   **Bias Gradient** (∂L/∂b):
   - If using bias terms in convolution
   - Sum of incoming gradient over spatial dimensions
   - One value per output channel"
  (:require [org.soulspace.arrayfire.ffi.convolve :as convolve-ffi]
            [org.soulspace.arrayfire.integration.jvm-integration :as jvm])
  (:import (org.soulspace.arrayfire.integration.jvm_integration AFArray)))

;;;
;;; Gradient Type Constants
;;;

(def ^:const gradient-type-default 0)
(def ^:const gradient-type-filter 1)
(def ^:const gradient-type-data 2)
(def ^:const gradient-type-bias 3)

(defn gradient-type->int
  "Convert gradient type keyword to integer constant.
   
   Parameters:
   - type: Gradient type (:default, :filter, :data, :bias)
   
   Returns:
   Integer constant for gradient type"
  [type]
  (case type
    :default gradient-type-default
    :filter gradient-type-filter
    :data gradient-type-data
    :bias gradient-type-bias
    (throw (ex-info "Unknown gradient type" {:type type}))))

;;;
;;; Convolution Gradient
;;;

(defn convolve2-gradient-nn
  "Compute backward pass gradient of 2D convolution for neural networks.
   
   This function is essential for training convolutional neural networks.
   It computes gradients for the backward pass of a convolutional layer,
   enabling weight updates and gradient backpropagation.
   
   Forward Pass Context:
   During forward propagation, you compute:
     output = convolve2-nn(signal, filter, stride, padding, dilation)
   
   Then compute loss and get gradient:
     loss = loss-function(output, target)
     incoming-gradient = ∂loss/∂output
   
   Backward Pass (this function):
   Computes one of three gradient types:
   - Filter gradient: For updating weights
   - Data gradient: For backpropagating to previous layer
   - Bias gradient: For bias terms (if applicable)
   
   Parameters:
   - incoming-gradient: Gradient from next layer (∂L/∂output), AFArray
     * Same dimensions as convolved output
     * Structure: [height_out × width_out × num_filters × batch_size]
   
   - original-signal: Input signal from forward pass, AFArray
     * Structure: [height × width × channels × batch_size]
     * Must be the exact same array used in forward pass
   
   - original-filter: Filter/weights from forward pass, AFArray
     * Structure: [kernel_h × kernel_w × channels × num_filters]
     * Must be the exact same array used in forward pass
   
   - convolved-output: Output from forward pass, AFArray
     * Structure: [height_out × width_out × num_filters × batch_size]
     * May be needed for certain gradient computations
   
   - strides: Stride values [stride_height stride_width], vector of longs
     * Must match stride used in forward pass
     * Controls output subsampling
   
   - paddings: Padding values [pad_height pad_width], vector of longs
     * Must match padding used in forward pass
     * Zero-padding around input signal
   
   - dilations: Dilation values [dilation_h dilation_w], vector of longs
     * Must match dilation used in forward pass
     * Spacing between kernel elements (atrous convolution)
   
   - gradient-type: Type of gradient to compute
     * :default or :filter - Gradient wrt filter (for weight updates)
     * :data - Gradient wrt input (for backprop to previous layer)
     * :bias - Gradient wrt bias term
   
   Returns:
   AFArray containing the requested gradient:
   - Filter gradient: [kernel_h × kernel_w × channels × num_filters]
   - Data gradient: [height × width × channels × batch_size]
   - Bias gradient: [1 × 1 × num_filters × 1]
   
   Gradient Types Explained:
   
   1. **Filter Gradient** (:filter):
      Most commonly used for training. Computes ∂L/∂W where W is the
      filter/weights. This gradient is used to update the convolutional
      layer's weights during optimization.
      
      Usage:
      ```clojure
      (let [filter-grad (convolve2-gradient-nn
                          loss-grad signal filter output
                          [1 1] [0 0] [1 1] :filter)]
        ;; Update: filter-new = filter - learning-rate × filter-grad
        (update-weights filter filter-grad learning-rate))
      ```
   
   2. **Data Gradient** (:data):
      Used for backpropagation to previous layers. Computes ∂L/∂X where
      X is the input signal. This gradient flows backward through the
      network to earlier layers.
      
      Usage:
      ```clojure
      (let [data-grad (convolve2-gradient-nn
                        loss-grad signal filter output
                        [1 1] [0 0] [1 1] :data)]
        ;; Propagate to previous layer
        (backward-previous-layer data-grad))
      ```
   
   3. **Bias Gradient** (:bias):
      For layers with bias terms. Computes ∂L/∂b where b is the bias
      vector. One gradient value per output channel.
      
      Usage:
      ```clojure
      (let [bias-grad (convolve2-gradient-nn
                        loss-grad signal filter output
                        [1 1] [0 0] [1 1] :bias)]
        ;; Update: bias-new = bias - learning-rate × bias-grad
        (update-bias bias bias-grad learning-rate))
      ```
   
   Complete Training Example:
   ```clojure
   (defn train-conv-layer
     \"Train a single convolutional layer for one batch.\"
     [{:keys [signal target filter bias learning-rate]}]
     (let [;; Forward pass
           output (convolve2-nn signal filter [1 1] [1 1] [1 1])
           output-with-bias (add output bias)
           
           ;; Compute loss gradient (from loss function)
           loss-grad (compute-loss-gradient output-with-bias target)
           
           ;; Backward pass - compute all gradients
           filter-grad (convolve2-gradient-nn
                         loss-grad signal filter output
                         [1 1] [1 1] [1 1] :filter)
           
           data-grad (convolve2-gradient-nn
                       loss-grad signal filter output
                       [1 1] [1 1] [1 1] :data)
           
           bias-grad (convolve2-gradient-nn
                       loss-grad signal filter output
                       [1 1] [1 1] [1 1] :bias)
           
           ;; Update parameters
           new-filter (sub filter (mul filter-grad learning-rate))
           new-bias (sub bias (mul bias-grad learning-rate))]
       
       {:filter new-filter
        :bias new-bias
        :data-gradient data-grad}))
   ```
   
   Stride, Padding, Dilation:
   
   - **Stride**: Controls output spatial dimensions
     * Stride [2 2] halves spatial dimensions
     * Larger stride → faster but coarser features
     * Must match forward pass exactly
   
   - **Padding**: Adds zeros around input
     * Padding [1 1] maintains spatial dimensions
     * Affects gradient at boundaries
     * Must match forward pass exactly
   
   - **Dilation**: Spaces out kernel elements
     * Dilation [2 2] doubles receptive field
     * Atrous/dilated convolution
     * Must match forward pass exactly
   
   Advanced Example with Strides and Padding:
   ```clojure
   ;; Stride-2 convolution (downsampling)
   (let [;; Forward pass
         output (convolve2-nn signal filter [2 2] [1 1] [1 1])
         loss-grad (compute-loss-gradient output target)
         
         ;; Backward pass with same parameters
         filter-grad (convolve2-gradient-nn
                       loss-grad signal filter output
                       [2 2]  ; Same stride!
                       [1 1]  ; Same padding!
                       [1 1]  ; Same dilation!
                       :filter)]
     ;; Update filter...
     )
   ```
   
   Multi-Layer Network Example:
   ```clojure
   (defn backprop-network
     \"Backpropagate through multiple convolutional layers.\"
     [layers loss-gradient]
     (reduce
       (fn [grad layer]
         (let [;; Compute filter gradient for this layer
               filter-grad (convolve2-gradient-nn
                             grad
                             (:signal layer)
                             (:filter layer)
                             (:output layer)
                             (:stride layer)
                             (:padding layer)
                             (:dilation layer)
                             :filter)
               
               ;; Compute data gradient to pass backward
               data-grad (convolve2-gradient-nn
                           grad
                           (:signal layer)
                           (:filter layer)
                           (:output layer)
                           (:stride layer)
                           (:padding layer)
                           (:dilation layer)
                           :data)]
           
           ;; Store filter gradient for update
           (store-gradient! layer filter-grad)
           
           ;; Return data gradient for previous layer
           data-grad))
       
       loss-gradient
       (reverse layers)))  ; Backprop goes backward!
   ```
   
   Batch Processing:
   The batch dimension (4th dimension) is automatically handled:
   - Filter gradients: Accumulated across batch
   - Data gradients: Computed per batch sample
   - Larger batches → better gradient estimates
   
   Performance Tips:
   
   1. **Reuse Arrays**: Keep forward pass arrays for backward pass
   2. **Batch Size**: Larger batches improve efficiency
   3. **Memory**: Gradient arrays same size as originals
   4. **In-Place Ops**: Use in-place operations for updates when possible
   
   Common Pitfalls:
   
   1. **Parameter Mismatch**: Stride/padding/dilation must match forward pass
   2. **Dimension Errors**: Check array dimensions carefully
   3. **Gradient Explosion**: Use gradient clipping if needed
   4. **Memory Leaks**: Properly release intermediate arrays
   
   See Also:
   - convolve2-nn in integration.signal (forward pass)
   - Optimizer implementations for weight updates
   - Loss functions and their gradients"
  [^AFArray incoming-gradient
   ^AFArray original-signal
   ^AFArray original-filter
   ^AFArray convolved-output
   strides paddings dilations
   gradient-type]
  (let [;; Convert strides to native array
        stride-count (count strides)
        stride-buf (jvm/dims->segment strides)
        
        ;; Convert paddings to native array
        padding-count (count paddings)
        padding-buf (jvm/dims->segment paddings)
        
        ;; Convert dilations to native array
        dilation-count (count dilations)
        dilation-buf (jvm/dims->segment dilations)
        
        ;; Convert gradient type to integer
        grad-type-int (gradient-type->int gradient-type)
        
        ;; Allocate output
        out (jvm/native-af-array-pointer)]
    
    (jvm/check! (convolve-ffi/af-convolve2-gradient-nn
                  out
                  (jvm/af-handle incoming-gradient)
                  (jvm/af-handle original-signal)
                  (jvm/af-handle original-filter)
                  (jvm/af-handle convolved-output)
                  (int stride-count)
                  stride-buf
                  (int padding-count)
                  padding-buf
                  (int dilation-count)
                  dilation-buf
                  (int grad-type-int))
                "af-convolve2-gradient-nn")
    
    (jvm/af-array-new (jvm/deref-af-array out))))

;;;
;;; Convenience Functions
;;;

(defn filter-gradient
  "Compute gradient with respect to filter/weights.
   
   Convenience function that calls convolve2-gradient-nn with :filter type.
   This is the most commonly used gradient for training CNNs.
   
   Parameters:
   - incoming-gradient: Gradient from next layer (AFArray)
   - signal: Input signal from forward pass (AFArray)
   - filter: Filter from forward pass (AFArray)
   - output: Output from forward pass (AFArray)
   - strides: Stride values (vector)
   - paddings: Padding values (vector)
   - dilations: Dilation values (vector)
   
   Returns:
   AFArray with filter gradient [kernel_h × kernel_w × channels × num_filters]
   
   Example:
   ```clojure
   (let [grad (filter-gradient loss-grad signal filter output
                                [1 1] [1 1] [1 1])]
     ;; Update weights: filter = filter - lr × grad
     (update-filter! filter grad learning-rate))
   ```"
  [incoming-gradient signal filter output strides paddings dilations]
  (convolve2-gradient-nn incoming-gradient signal filter output
                         strides paddings dilations :filter))

(defn data-gradient
  "Compute gradient with respect to input data.
   
   Convenience function that calls convolve2-gradient-nn with :data type.
   Used for backpropagating gradients to previous layers.
   
   Parameters:
   - incoming-gradient: Gradient from next layer (AFArray)
   - signal: Input signal from forward pass (AFArray)
   - filter: Filter from forward pass (AFArray)
   - output: Output from forward pass (AFArray)
   - strides: Stride values (vector)
   - paddings: Padding values (vector)
   - dilations: Dilation values (vector)
   
   Returns:
   AFArray with data gradient [height × width × channels × batch_size]
   
   Example:
   ```clojure
   (let [grad (data-gradient loss-grad signal filter output
                              [1 1] [1 1] [1 1])]
     ;; Propagate to previous layer
     (backward-to-previous-layer grad))
   ```"
  [incoming-gradient signal filter output strides paddings dilations]
  (convolve2-gradient-nn incoming-gradient signal filter output
                         strides paddings dilations :data))

(defn bias-gradient
  "Compute gradient with respect to bias.
   
   Convenience function that calls convolve2-gradient-nn with :bias type.
   Used when convolutional layers have bias terms.
   
   Parameters:
   - incoming-gradient: Gradient from next layer (AFArray)
   - signal: Input signal from forward pass (AFArray)
   - filter: Filter from forward pass (AFArray)
   - output: Output from forward pass (AFArray)
   - strides: Stride values (vector)
   - paddings: Padding values (vector)
   - dilations: Dilation values (vector)
   
   Returns:
   AFArray with bias gradient [1 × 1 × num_filters × 1]
   
   Example:
   ```clojure
   (let [grad (bias-gradient loss-grad signal filter output
                              [1 1] [1 1] [1 1])]
     ;; Update bias: bias = bias - lr × grad
     (update-bias! bias grad learning-rate))
   ```"
  [incoming-gradient signal filter output strides paddings dilations]
  (convolve2-gradient-nn incoming-gradient signal filter output
                         strides paddings dilations :bias))

(defn all-gradients
  "Compute all gradient types (filter, data, bias) in one call.
   
   Efficiently computes all three gradient types that are typically needed
   during backpropagation in CNN training.
   
   Parameters:
   - incoming-gradient: Gradient from next layer (AFArray)
   - signal: Input signal from forward pass (AFArray)
   - filter: Filter from forward pass (AFArray)
   - output: Output from forward pass (AFArray)
   - strides: Stride values (vector)
   - paddings: Padding values (vector)
   - dilations: Dilation values (vector)
   
   Returns:
   Map containing:
   - :filter - Filter gradient for weight updates
   - :data - Data gradient for backprop to previous layer
   - :bias - Bias gradient for bias updates
   
   Example:
   ```clojure
   (let [{:keys [filter data bias]} (all-gradients loss-grad signal filt out
                                                     [1 1] [1 1] [1 1])]
     ;; Update filter
     (update-filter! filt filter learning-rate)
     ;; Update bias
     (update-bias! bias-param bias learning-rate)
     ;; Backprop to previous layer
     (backward-to-previous-layer data))
   ```"
  [incoming-gradient signal filter output strides paddings dilations]
  {:filter (filter-gradient incoming-gradient signal filter output
                            strides paddings dilations)
   :data (data-gradient incoming-gradient signal filter output
                        strides paddings dilations)
   :bias (bias-gradient incoming-gradient signal filter output
                        strides paddings dilations)})
