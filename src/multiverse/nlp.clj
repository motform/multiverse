(ns multiverse.nlp
  (:require [opennlp.nlp :as opennlp]))

(defn nlp-fn [class model]
  (fn [s]
    (let [process (class model)]
      (process s))))

(def detokenize
  (nlp-fn opennlp/make-detokenizer "resources/opennlp/english-detokenizer.xml"))

(def split-sentences
  (nlp-fn opennlp/make-sentence-detector "resources/opennlp/en-sent.bin"))

(def tokenize
  (nlp-fn opennlp/make-tokenizer "resources/opennlp/en-token.bin"))

(defn take-sentences
  "Returns `n` first sentences from `s`."
  [s n]
  (->> s split-sentences (take n) detokenize))

(defn take-last-words
  "Like `take-last`, but returns a string instead of a seq of chars"
  [s n]
  (->> s tokenize (take-last n) detokenize))

