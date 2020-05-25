(ns neural.novelty.ml
  (:require [clojure.string :as str]
            [libpython-clj.python :as py :refer [py. py.. py.-]]
            [libpython-clj.require :refer [require-python]]
            [neural.novelty.nlp :as nlp]))

(require-python 'transformers)
(require-python 'torch)

;;; Text Generation

(def transformer "distilgpt2" #_"gpt2-medium" #_"gpt2")
(def tokenizer (py. transformers/GPT2Tokenizer from_pretrained transformer))
(def model (py. transformers/GPT2LMHeadModel
                from_pretrained transformer :pad_token_id (py.- tokenizer eos_token_id)))

(defn ->tokens [s]
  (py. tokenizer encode s :return_tensors "pt"))

(defn decode [ts]
  (map #(py. tokenizer decode % :skip_special_tokens true) ts))

(defn generate [input-ids len n]
  (py. model generate
       input-ids :do_sample true :max_length len :top_p 0.92 :top_k 50 :temperature 0.8 :num_return_sequences n))

(defn generate-text [prompt len n]
  (let [input-ids (->tokens prompt)]
    (decode (generate input-ids len n))))

(defn remove-prompt [new prompt]
  (str/trim (str/replace new (re-pattern prompt) "")))

;; TODO rename
(defn continuations [generated prompt n]
  (-> generated (remove-prompt prompt) (nlp/take-sentences n)))

;; TODO make this accept an options map instead of that arbitrary and confusing positional argument
(defn generate-sentences [prompt n]
  (if (str/blank? prompt)
    ["ERROR: blank string."]
    (->> (generate-text prompt 300 3) (map #(continuations % prompt n)))))

;;; Summarization

(def summarizer (transformers/pipeline "summarization"))

(defn summarize [text]
  (summarizer text :max_length 130 :min_length 30))

(defn generate-title [text]
  (-> (summarizer text :max_length 12 :min_length 5)
      first
      (get "summary_text")))

;;; NER

(def ner (transformers/pipeline "ner"))

(defn named-entity-recognition [text]
  (ner text))


;; WARN Broken Refactor
;;      for some reason, this completely breaks the generation, after like one cycle
;;      I'm probably just feeding it to few sentences, but not sure why/how

;; (defn in-limit? [s limit]
;;   (let [limiter 50]
;;     (if (< (- limit limiter) (count s))
;;       (nlp/take-last-words s limit)
;;       s)))

;; (defn next-sentences-2 [prompt]
;;   (let [n-sentences 2
;;         n-return-sequences 3
;;         max-len 300
;;         ;; prompt (in-limit? prompt max-len)
;;         ]
;;     (map (comp #(remove-prompt % prompt)
;;                #(nlp/take-sentences % n-sentences))
;;          (generate-text prompt max-len n-return-sequences))))

(comment

  (def example-prompt
    "The basic idea is simple: the tree is turned inside-out like a returned glove, pointers from the root to the current position being reversed in a path structure. The current location holds both the downward current subtree and the upward path.")
  )
