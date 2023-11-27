(ns org.motform.multiverse.util
  (:require [clojure.string :as str]))

;;; Stdlib

(defn conj?
  "`conj` `x` to `xs` if non-nil, otherwise return `xs`"
  [xs x]
  (if x (conj xs x) xs))

(defn pairs
  "Returns a vec with two-tuples [[x1 y1] [x2 y2]] from `xs` and `ys`."
  [xs ys]
  (mapv (fn [x y] [x y]) xs ys))

;;; Text formatting

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

;;; DB helpers

(defn children
  ([db parent-id] (children db parent-id nil))
  ([db parent-id story-id]
   (let [story-id (or story-id (get-in db [:db/state :story/active]))]
     (select-keys (get-in db [:db/stories story-id :story/sentences])
                  (get-in db [:db/stories story-id :story/sentences parent-id :sentence/children])))))

(defn completion-data [db]
  (let [story-id  (get-in db [:db/state :story/active])
        parent-id (get-in db [:db/stories story-id :story/meta :sentence/active])
        api-key   (get-in db [:db/state :open-ai/key :open-ai/api-key])] 
    {:story-id  story-id
     :parent-id parent-id
     :api-key   api-key}))

(defn paragraph [db story-id sentence-id]
  (reduce
   (fn [sentences id]
     (conj sentences (get-in db [:db/stories story-id :story/sentences id])))
   [] (get-in db [:db/stories story-id :story/sentences sentence-id :sentence/path])))

;;; Graphical elements

(defn spinner []
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
