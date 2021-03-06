(ns org.motform.multiverse.util
  (:require [clojure.string :as str]
            #?(:cljs [cognitect.transit :as transit])
            #?(:clj  [muuntaja.core :as muuntaja])))

(defn ->transit+json
  "Returns `data` encoded in transit+json."
  [data]
  #?(:cljs (transit/write (transit/writer :json) data)
     :clj (muuntaja/encode "application/transit+json" data)))

(defn enumerate
  "Returns `xs` enumerated as \"i. (str `x`)\", starting from 1."
  [xs]
  (map-indexed (fn [i x] [i x]) xs))

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
  [title]
  (let [stop-words #{"the" "a" "an" "for" "but" "not" "yet"
                     "so" "at" "around" "by" "of" "from" "on"
                     "with" "to" "without" "after" "and" "how"}]
    (as-> title <>
      (str/replace <> #"\'|\"|“" "")
      (str/split <> #" ")
      (mapv #(if-not (stop-words %) (str/capitalize %) %) <>)
      (update <> 0 str/capitalize) ; always capitalize the leading word
      (str/join " " <>))))

(defn format-title
  "Applies title-case and removes punctuation in `title`."
  [title]
  (when-not (str/blank? title)
    (-> title (str/replace #"[',\"\.\!]" "") title-case)))

(defn format-story
  "Returns space-delimited str from a vec of `sentences`"
  [sentences]
  (->> sentences (map :text) (interpose " ") (apply str)))

(defn proper-separation [strings]
  (let [comma-sep (into [] (interpose ", " strings))
        idx (- (count comma-sep) 2)]
    (if (pos? idx) (assoc comma-sep idx " & ") strings)))

(defn two-digitize [x]
  (if (> 10 x) (str "0" x) x))

#?(:cljs
   (defn format-date [date]
     (str (two-digitize (.getHours date)) ":" (two-digitize (.getMinutes date)) ", "
          (.getFullYear date) "–" (two-digitize (.getMonth date)) "–"  (two-digitize (.getDay date)))))

(defn realize
  "Blocks main thread while trying to realize `x`, sorry about that."
  [x]
  (while (not (realized? x)))
  @x)
