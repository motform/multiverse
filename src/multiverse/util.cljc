(ns multiverse.util
  (:require [clojure.string :as str]
            #?(:cljs [cognitect.transit :as transit])
            #?(:clj  [muuntaja.core :as muuntaja])))

(defn ->transit+json
  "Returns `data` encoded in transit+json."
  [data]
  #?(:cljs (transit/write (transit/writer :json) data)
     :clj (muuntaja/encode "application/transit+json" data)))

(defn enumerate-str
  "Returns `xs` enumerated as \"i. (str `x`)\", starting from 1."
  [xs]
  (map-indexed (fn [i x] (str (inc i) ". " x)) xs))

(defn enumerate
  "Returns `xs` enumerated as \"i. (str `x`)\", starting from 1."
  [xs]
  (map-indexed (fn [i x] [i x]) xs))

(defn gen-key
  "Generates a React key by hashing the str representation of `x`
  `rest`, and a random int to prevent collisions."
  [x & rest]
  (hash (str (rand-int 255) x rest)))

(defn pairs
  "Returns a vec with two-tuples [[x1 y1] [x2 y2]] from `xs` and `ys`."
  [xs ys]
  (mapv (fn [x y] [x y]) xs ys))

(defn ?assoc
  "Associates the `k` into the `m` if the `v` is truthy, otherwise returns `m`.
  NOTE: this version of ?assoc only does a single kv pair."
  [m k v]
  (if v (assoc m k v) m))

(defn title-case
  "A simple title case by short list of common stop words."
  [s]
  (let [stop-words #{"the" "a" "an" "for" "but" "not" "yet"
                    "so" "at" "around" "by" "of" "from" "on"
                    "with" "to" "without" "after" "and"}]
    (as-> s title
      (str/split title #" ")
      (mapv #(if-not (stop-words %) (str/capitalize %) %) title)
      (update title 0 str/capitalize) ; always capitalize the leading word
      (str/join " " title))))

(defn format-title
  "Applies title-case and removes punctuation in `title`."
  [title]
  (if-not (str/blank? title)
    (-> title (str/replace #"[',\"\.\!]" "") title-case)
    "Generating title..."))

(defn format-story
  "Returns space-delimited str from a vec of `sentences`"
  [sentences]
  (->> sentences (map :text) (interpose " ") (apply str)))
