(ns neural.multiverse.components.map
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

(defn level [{:keys [text id path children]}]
  ^{:key (str id "-M")}
  [:div.level
   {:id id :class (str (leaf? children) "-" (highlight? id))
    :style {:margin-left (str (* 3 (count path)) "rem")}
    :on-click #(rf/dispatch [:select-sentence id])
    :on-mouse-over #(rf/dispatch [:preview-sentence id])
    :on-mouse-out  #(rf/dispatch [:remove-preview])}])

(defn walk-sentences
  ([sentences]
   (let [[root _] (-> sentences keys first sentences :path)]
     [:nav
      (walk-sentences sentences root)]))
  ([sentences id]
   (let [node (sentences id)]
     (concat [[level node]]
             (for [c (:children node)]
               (walk-sentences sentences c))))))

(defn tree-map []
  (let [sentence-tree @(rf/subscribe [:sentence-tree])]
    [:section.map
     [walk-sentences sentence-tree]]))
