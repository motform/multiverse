(ns multiverse.ml
  "Calls out to HuggingFace Transformers using libpython-clj.

  The original version of this was written before HF provided a pipeline for
  text generation and used lower level calls. If you are interested in how that
  might have looked, see git rev `3e9c619` or earlier."
  (:require [clojure.string :as str]
            [libpython-clj.python :as py :refer [py. py.. py.-]]
            [libpython-clj.require :refer [require-python]]
            [multiverse.nlp :as nlp]))

(require-python 'transformers)
(require-python 'torch)

(def summarizer (transformers/pipeline "summarization"))

(defn generate-title [text]
  (-> (summarizer text :max_length 12 :min_length 5)
      first
      (get "summary_text")))

(def ner (transformers/pipeline "ner"))

(defn named-entity-recognition [text]
  (ner text))

(defn ->text-generator [model]
  (transformers/pipeline "text-generation" :model model :framework "pt"))

(def models
  {"GPT-2"    (->text-generator "gpt2")
   "XLNet"    (->text-generator "xlnet-base-cased") 
   "Reformer" (->text-generator "google/reformer-crime-and-punishment")})

(defn generate-text [prompt model]
  (let [generator #((models model) % :max_length 200)]
    (-> prompt generator first (get "generated_text"))))

(defn remove-prompt [new prompt]
  (str/trim (str/replace new (re-pattern prompt) "")))

(defn continuations [generated prompt n]
  (-> generated (remove-prompt prompt) (nlp/take-sentences n)))

(defn generate-sentences
  ([prompt model]
   (generate-sentences 3 prompt model))
  ([n prompt model]
   (if (str/blank? prompt)
     ["ERROR: blank string."] ; return a vector to keep API compatibility
     (->> (repeatedly n #(generate-text prompt model))
          (map #(continuations % prompt 2))))))

(comment

  (def example-prompt
    "The basic idea is simple: the tree is turned inside-out like a returned glove, pointers from the root to the current position being reversed in a path structure. The current location holds both the downward current subtree and the upward path.")

  )
