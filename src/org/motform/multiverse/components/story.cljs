(ns org.motform.multiverse.components.story
  (:require [clojure.string :as str]
            [re-frame.core :as rf]                       
            [org.motform.multiverse.components.map :refer [tree-map]]
            [org.motform.multiverse.util :as util]
            [org.motform.multiverse.components.sidebar :refer [sidebar]]))

;;; Util

(defn highlight? [id]
  (when-let [highlight @(rf/subscribe [:highlight])]
    (let [path (set @(rf/subscribe [:path highlight]))]
      (when-not (contains? path id)
        "inactive"))))

;;; Landing
;;; Story

(defn sentence [text id class completion? visited?]
  [(if completion? :div :span)
   {:id id
    :class (str (when (= class "child") (if visited? "visited" "unvisited")) " "
                class " "
                (highlight? id) " ")
    :on-click      #(rf/dispatch [:active-sentence id])
    :on-mouse-over #(rf/dispatch [:highlight-sentence id])
    :on-mouse-out  #(rf/dispatch [:remove-highlight])}
   (if (str/blank? text) "..." text)])

(defn story []
  ;; NOTE this implementation means there can only be a single request out per parent,
  ;;      in theory, it is possible/preferable to have multiple ones
  (let [parent    @(rf/subscribe [:active-sentence])
        request?  @(rf/subscribe [:pending-request?])
        preview?  @(rf/subscribe [:preview?])
        sentences @(rf/subscribe [:sentences parent])
        children  @(rf/subscribe [:children parent])
        _ (when (and (not children) (not preview?) (not request?))
            (rf/dispatch [:open-ai/completions parent (util/format-story sentences)]))]
    [:main.story.v-stack.pad-full.gap-half
     [:div.sentences
      (for [{:keys [text id]} sentences]
        ^{:key id} [sentence text id "parent"])]
     (if request?
       [:section.children.pad-half [util/spinner]]
       [:section.children.h-equal-3.pad-double.gap-double
        (for [{:keys [id text children]} children]
          ^{:key id} [sentence text id "child" true (seq children)])])]))

;;; Header

(defn title []
  (let [{:keys [title]} @(rf/subscribe [:meta])]
    (if-not (str/blank? title)
      [:h2.title.tooltip-container
       {:on-pointer-down #(rf/dispatch [:open-ai/title nil])}
       title
       [:span.tooltip.tooltip-large.rounded "Generate new title"]]
      [util/spinner])))

;;; Main

(defn multiverse []
  [:div.app-container.v-stack.overlay
   [sidebar [title]]
   [story]])
