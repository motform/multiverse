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

(defn spinner-small []
  [:div.v-stack.centered
   [:svg {:height 23
          :width 54}
    [:circle.spinner-1 {:cx 6  :cy 8 :r 6 :fill "var(--spinner-fill)"}]
    [:circle.spinner-2 {:cx 26 :cy 8 :r 6 :fill "var(--spinner-fill)"}]
    [:circle.spinner-3 {:cx 46 :cy 8 :r 6 :fill "var(--spinner-fill)"}]]])

(def icon-plus
  [:svg {:view-box "0 0 18 18",
         :fill "currentColor",
         :height "30",
         :width "30"}
   [:path {:d "M8 4a.5.5 0 0 1 .5.5v3h3a.5.5 0 0 1 0 1h-3v3a.5.5 0 0 1-1 0v-3h-3a.5.5 0 0 1 0-1h3v-3A.5.5 0 0 1 8 4z"}]])

(defn icon-square []
  [:svg.icon {:width "16"
         :height "16"
         :fill "currentColor"
         :viewbox "0 0 16 16"}
   [:path {:d "M11 2a3 3 0 0 1 3 3v6a3 3 0 0 1-3 3H5a3 3 0 0 1-3-3V5a3 3 0 0 1 3-3h6zM5 1a4 4 0 0 0-4 4v6a4 4 0 0 0 4 4h6a4 4 0 0 0 4-4V5a4 4 0 0 0-4-4H5z"}]])

(defn icon-split []
  [:svg.icon {:width "16"
         :height "16"
         :fill "currentColor"
         :viewbox "0 0 16 16"}
   [:path {:d "M0 3a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2V3zm8.5-1v12H14a1 1 0 0 0 1-1V3a1 1 0 0 0-1-1H8.5zm-1 0H2a1 1 0 0 0-1 1v10a1 1 0 0 0 1 1h5.5V2z"}]])

(defn icon-tree []
  [:svg {:width "16"
         :height "16"
         :fill "currentColor"
         :viewbox "0 0 16 16"}
   [:path {:fill-rule "evenodd", :d "M6 3.5A1.5 1.5 0 0 1 7.5 2h1A1.5 1.5 0 0 1 10 3.5v1A1.5 1.5 0 0 1 8.5 6v1H14a.5.5 0 0 1 .5.5v1a.5.5 0 0 1-1 0V8h-5v.5a.5.5 0 0 1-1 0V8h-5v.5a.5.5 0 0 1-1 0v-1A.5.5 0 0 1 2 7h5.5V6A1.5 1.5 0 0 1 6 4.5v-1zM8.5 5a.5.5 0 0 0 .5-.5v-1a.5.5 0 0 0-.5-.5h-1a.5.5 0 0 0-.5.5v1a.5.5 0 0 0 .5.5h1zM0 11.5A1.5 1.5 0 0 1 1.5 10h1A1.5 1.5 0 0 1 4 11.5v1A1.5 1.5 0 0 1 2.5 14h-1A1.5 1.5 0 0 1 0 12.5v-1zm1.5-.5a.5.5 0 0 0-.5.5v1a.5.5 0 0 0 .5.5h1a.5.5 0 0 0 .5-.5v-1a.5.5 0 0 0-.5-.5h-1zm4.5.5A1.5 1.5 0 0 1 7.5 10h1a1.5 1.5 0 0 1 1.5 1.5v1A1.5 1.5 0 0 1 8.5 14h-1A1.5 1.5 0 0 1 6 12.5v-1zm1.5-.5a.5.5 0 0 0-.5.5v1a.5.5 0 0 0 .5.5h1a.5.5 0 0 0 .5-.5v-1a.5.5 0 0 0-.5-.5h-1zm4.5.5a1.5 1.5 0 0 1 1.5-1.5h1a1.5 1.5 0 0 1 1.5 1.5v1a1.5 1.5 0 0 1-1.5 1.5h-1a1.5 1.5 0 0 1-1.5-1.5v-1zm1.5-.5a.5.5 0 0 0-.5.5v1a.5.5 0 0 0 .5.5h1a.5.5 0 0 0 .5-.5v-1a.5.5 0 0 0-.5-.5h-1z"}]])
