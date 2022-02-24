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
    (as-> title <>
      (str/trim <>)
      (str/split <> #" ")
      (mapv #(if-not (stop-words %) (str/capitalize %) %) <>)
      (update <> 0 str/capitalize) ; always capitalize the leading word
      (str/join " " <>))))

(defn two-digitize [x]
  (if (> 10 x) (str "0" x) x))

(defn format-date [date]
  (str (two-digitize (.getHours date)) ":" (two-digitize (.getMinutes date)) ", "
       (.getFullYear date) "–" (two-digitize (.getMonth date)) "–"  (two-digitize (.getDay date))))

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
  [:div.v-stack.centered
   [:svg {:height 30
          :width 80}
    [:circle.spinner-1 {:cx 10 :cy 10 :r 8 :fill "var(--spinner-fill)"}]
    [:circle.spinner-2 {:cx 40 :cy 10 :r 8 :fill "var(--spinner-fill)"}]
    [:circle.spinner-3 {:cx 70 :cy 10 :r 8 :fill "var(--spinner-fill)"}]]])

(def icon-plus
  [:svg {:view-box "0 0 18 18",
         :fill "currentColor",
         :height "30",
         :width "30"}
   [:path {:d "M8 4a.5.5 0 0 1 .5.5v3h3a.5.5 0 0 1 0 1h-3v3a.5.5 0 0 1-1 0v-3h-3a.5.5 0 0 1 0-1h3v-3A.5.5 0 0 1 8 4z"}]])

(defn icon-refersh []
  [:svg {:width "16"
         :height "16"
         :fill "currentColor"
         :viewbox "0 0 16 16"}
   [:path {:fill-rule "evenodd", :d "M8 3a5 5 0 1 0 4.546 2.914.5.5 0 0 1 .908-.417A6 6 0 1 1 8 2v1z"}]
   [:path {:d "M8 4.466V.534a.25.25 0 0 1 .41-.192l2.36 1.966c.12.1.12.284 0 .384L8.41 4.658A.25.25 0 0 1 8 4.466z"}]])
