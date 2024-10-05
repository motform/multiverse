(ns org.motform.multiverse.util
  (:require
    [clojure.string :as str]))

;; Stdlib

(defn conj?
  "`conj` `x` to `xs` if non-nil, otherwise return `xs`"
  [xs x]
  (if x (conj xs x) xs))

(defn pairs
  "Returns a vec with two-tuples [[x1 y1] [x2 y2]] from `xs` and `ys`."
  [xs ys]
  (mapv (fn [x y] [x y]) xs ys))

;; Text formatting

(defn title-case
  "A simple title case by short list of common stop words."
  [title]
  (let [stop-words #{"the" "a" "an" "for" "but" "not" "yet"
                     "so" "at" "around" "by" "of" "from" "on"
                     "with" "to" "without" "after" "and" "how"}]
    (as-> title $
          (str/trim $)
          (str/split $ #" ")
          (mapv #(if-not (stop-words %) (str/capitalize %) %) $)
          (update $ 0 str/capitalize) ; always capitalize the leading word
          (str/join " " $))))

;; Graphical elements

(defn Spinner []
  [:div.v-stack.centered.spinner-container
   [:svg {:height 30
          :width 80}
    [:circle.spinner-1 {:cx 10 :cy 10 :r 8 :fill "var(--spinner-fill)"}]
    [:circle.spinner-2 {:cx 40 :cy 10 :r 8 :fill "var(--spinner-fill)"}]
    [:circle.spinner-3 {:cx 70 :cy 10 :r 8 :fill "var(--spinner-fill)"}]]])

(defn spinner-small []
  [:div.v-stack.centered
   [:svg {:height 21
          :width 54}
    [:circle.spinner-small-1 {:cx 6  :cy 6 :r 5 :fill "var(--spinner-fill)"}]
    [:circle.spinner-small-2 {:cx 26 :cy 6 :r 5 :fill "var(--spinner-fill)"}]
    [:circle.spinner-small-3 {:cx 46 :cy 6 :r 5 :fill "var(--spinner-fill)"}]]])
