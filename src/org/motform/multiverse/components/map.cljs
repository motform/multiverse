(ns org.motform.multiverse.components.map
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
  (let [class (str (leaf? children) "-" (highlight? id))]
    (when-not (= class "leaf-inactive")
      [:div.level
       {:class (str (leaf? children) "-" (highlight? id))
        :style {:margin-left (str (* 2 (count path)) "rem")}
        :on-pointer-down #(rf/dispatch [:select-sentence id])
        :on-pointer-over #(rf/dispatch [:preview-sentence id])
        :on-pointer-out  #(rf/dispatch [:remove-preview])
        :id id}
       [:div.node]])))

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
