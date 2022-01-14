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
                (highlight? id))
    :on-click      #(rf/dispatch [:active-sentence id])
    :on-mouse-over #(rf/dispatch [:highlight-sentence id])
    :on-mouse-out  #(rf/dispatch [:remove-highlight])}
   (if (str/blank? text) "..." text)])

(defn pending []
  [:div.pending
   [:img.scribble {:src "assets/scribble-story.gif" :alt "Generating text…"}]])

;; NOTE this implementation means there can only be a single request out per parent,
;;      in theory, it is possible/preferable to have multiple ones
(defn story []
  (let [parent    @(rf/subscribe [:active-sentence])
        request?  @(rf/subscribe [:pending-request?])
        preview?  @(rf/subscribe [:preview?])
        sentences @(rf/subscribe [:sentences parent])
        children  @(rf/subscribe [:children parent])
        _ (when (and (not children) (not preview?) (not request?))
            (rf/dispatch [:open-ai/completions parent (util/format-story sentences)]))]
    [:section.story 
     [:div.sentences
      (for [{:keys [text id]} sentences]
        ^{:key id} [sentence text id "parent"])]
     (if request?
       [:section.children [pending]]
       [:section.children
        (for [{:keys [id text children]} children]
          ^{:key id} [sentence text id "child" true (seq children)])])]))

;;; Sidebar

(defn format-title [title]
  (if-not (str/blank? title)
    (util/title-case title)
    [:img.scribble {:src "assets/scribble-title.gif" :alt "Generating title…"}]))

(defn old-sidebar []
  (let [{:keys [title updated]} @(rf/subscribe [:meta])]
    [:aside
     [:section.title
      [:div.update-title-container>button.update-title
       {:on-click #(rf/dispatch [:open-ai/title nil])}
       "↻"]
      [:h1 (format-title title)]]
     [tree-map]
     [:section.meta "Last Exploration " (util/format-date updated)]]))

;;; Main

(defn multiverse []
  [:main.multiverse
   [sidebar]
   [story]])
