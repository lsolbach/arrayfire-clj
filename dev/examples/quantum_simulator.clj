(ns examples.quantum-simulator
  "Mini quantum circuit simulator — GPU-accelerated complex linear algebra.

   Simulates quantum circuits up to ~12 qubits using arrayfire-clj's complex
   matrix-vector multiply on the GPU — the only such capability on the JVM.

   Gate matrices are built on the CPU via Kronecker products, then uploaded
   to the GPU for the state-vector evolution (the expensive part).

   Usage (REPL):
     (simulate-circuit 10 [:h-all])  ; 10-qubit equal superposition"
  (:refer-clojure :exclude [+ - * / abs range])
  (:require [org.soulspace.arrayfire.api.core :as af]))

;; --- CPU-side gate algebra (small matrices, fast) ---

(defn- c-mul
  "Multiply two complex numbers represented as [re im] pairs."
  [[ar ai] [br bi]]
  [(af/- (af/* ar br) (af/* ai bi))
   (af/+ (af/* ar bi) (af/* ai br))])

(defn- kron
  "Kronecker product of two row-major matrices of [re im] cells."
  [a b]
  (vec (for [row-a a, row-b b]
         (vec (for [a-ij row-a, b-kl row-b]
                (c-mul a-ij b-kl))))))

(def ^:private sqrt-half (af// 1.0 (Math/sqrt 2.0)))

(def gates
  "Standard single-qubit gates as row-major [re im] matrices."
  {:id [[[1.0 0.0] [0.0 0.0]] [[0.0 0.0] [1.0 0.0]]]
   :h  [[[sqrt-half 0.0] [sqrt-half 0.0]]
        [[sqrt-half 0.0] [(af/- sqrt-half) 0.0]]]
   :x  [[[0.0 0.0] [1.0 0.0]] [[1.0 0.0] [0.0 0.0]]]
   :z  [[[1.0 0.0] [0.0 0.0]] [[0.0 0.0] [-1.0 0.0]]]
   :s  [[[1.0 0.0] [0.0 0.0]] [[0.0 0.0] [0.0 1.0]]]
   :t  [[[1.0 0.0] [0.0 0.0]] [[0.0 0.0] [sqrt-half sqrt-half]]]})

(defn- single-qubit-unitary
  "Build full 2^n × 2^n unitary for gate on qubit k (0-indexed)."
  [gate-key qubit-idx n-qubits]
  (reduce kron
          (for [k (clojure.core/range n-qubits)]
            (if (= k qubit-idx) (gates gate-key) (gates :id)))))

(defn- mat->af
  "Upload a CPU row-major complex matrix to GPU as an AFArray."
  [mat]
  (let [n (count mat)
        re (double-array (for [j (clojure.core/range n)
                               i (clojure.core/range n)]
                           (first (get-in mat [i j]))))
        im (double-array (for [j (clojure.core/range n)
                               i (clojure.core/range n)]
                           (second (get-in mat [i j]))))]
    (af/complex (af/array re [n n]) (af/array im [n n]))))

(defn simulate-circuit
  "Run a quantum circuit on n-qubits starting from |0...0⟩.
   ops is a vector of gate specs:
     :h-all            — Hadamard on every qubit
     [:gate qubit-idx] — single gate on one qubit, e.g. [:x 0]
   Returns {:probabilities [...] :sum <double>}."
  [n-qubits ops]
  (let [dim     (bit-shift-left 1 n-qubits)
        build-u (fn [op]
                  (cond
                    (= op :h-all)
                    (reduce kron (repeat n-qubits (gates :h)))

                    (vector? op)
                    (single-qubit-unitary (first op) (second op) n-qubits)))]
    (af/with-arrayfire {:backend :opencl :converter-fn af/->value}
      (let [psi-re (af/array (double-array (cons 1.0 (repeat (dec dim) 0.0)))
                             [dim 1])
            psi0   (af/complex psi-re (af/constant 0.0 [dim 1]))
            psi    (reduce (fn [state op]
                             (af/matmul (mat->af (build-u op)) state))
                           psi0 ops)
            mag    (af/abs psi)
            probs  (af/* mag mag)]
        {:n-qubits     n-qubits
         :dim          dim
         :probabilities probs
         :sum          (af/sum probs)}))))

(comment
  ;; 10-qubit equal superposition: H on all qubits
  ;; All 1024 amplitudes should be 1/1024 ≈ 0.000977
  (time (let [r (simulate-circuit 10 [:h-all])]
          (select-keys r [:n-qubits :dim :probabilities :sum])))

  ;; 12-qubit equal superposition: H on all qubits
  ;; All 4096 amplitudes should be 1/4096 ≈ 0.000244
  (time (let [r (simulate-circuit 12 [:h-all])]
          (select-keys r [:n-qubits :dim :probabilities :sum])))

  ;; 3-qubit circuit: H on qubit 0, then X on qubit 1
  (time (simulate-circuit 3 [:h-all [:x 1]]))

  ;; 5-qubit with phase gates
  (time (let [r (simulate-circuit 5 [:h-all [:s 0] [:t 2]])]
          (select-keys r [:dim :sum])))

  ;
  )
