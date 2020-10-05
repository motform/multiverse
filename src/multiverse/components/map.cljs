(ns multiverse.components.map
  (:require [re-frame.core :as rf]))

(defn highlight? [id]
  (let [sentence (or @(rf/subscribe [:highlight]) @(rf/subscribe [:active-sentence]))
        path (set @(rf/subscribe [:path sentence]))]
    (if (contains? path id)
      "active"
      "inactive")))

(defn leaf? [children]
  (if (seq children)
    "branch"
    "leaf"))

(defn level [{:keys [id path children]}]
  [:div.level
   {:id id :class (str (leaf? children) "-" (highlight? id))
    :style {:margin-left (str (* 3 (count path)) "rem")}
    :on-click #(rf/dispatch [:select-sentence id])
    :on-mouse-over #(rf/dispatch [:preview-sentence id])
    :on-mouse-out  #(rf/dispatch [:remove-preview])}])

(defn walk-sentences
  ([sentences]
   (let [root (-> sentences keys first sentences :path first)]
     [:nav
      (walk-sentences sentences root)]))
  ([sentences id]
   (let [sentence (sentences id)]
     (concat [^{:key id} [level sentence]]
             (for [c (:children sentence)]
               (walk-sentences sentences c))))))

(defn tree-map []
  (let [sentences @(rf/subscribe [:sentence-tree])]
    [:section.map
     [walk-sentences sentences]]))
