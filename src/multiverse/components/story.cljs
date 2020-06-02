(ns multiverse.components.story
  (:require [clojure.string :as str]
            [multiverse.components.map :refer [tree-map]]
            [multiverse.routes :as routes]
            [multiverse.util :as util]
            [re-frame.core :as rf]))

;;; Util

(defn highlight? [id]
  (when-let [highlight @(rf/subscribe [:highlight])]
    (let [path (set @(rf/subscribe [:path highlight]))]
      (when-not (contains? path id)
        "inactive"))))

;;; Landing

(defn landing []
  [:div.intro-wrapper>section.landing
   [:h1 "MULTIVERSE"]
   [:div "Cybertextual generative literature through machine learning"]
   [:img {:src "/assets/pattern.svg"}]
   [:div "Start a "
    [:a {:href (routes/url-for :new-story)} "new story"]
    " or keep reading one in the "
    [:a {:href (routes/url-for :library)} "library"]]])

;;; Story

(defn node [text id class]
  [:div
   {:id id :class (str class " " (highlight? id))
    :on-click #(rf/dispatch [:active-sentence id])
    :on-mouse-over #(rf/dispatch [:highlight-sentence id])
    :on-mouse-out  #(rf/dispatch [:remove-highlight])}
   (if (str/blank? text) "..." text)])

(defn pending []
  [:div.pending "Generating text…"])

;; NOTE this implementation means there can only be a single request out per parent,
;;      in theory, it is possible/preferable to have multiple ones
(defn story []
  (let [parent @(rf/subscribe [:active-sentence])
        request? @(rf/subscribe [:pending-request?])
        preview? @(rf/subscribe [:preview?])
        sentences @(rf/subscribe [:sentences parent])
        children @(rf/subscribe [:children parent])
        _ (when (and (not children) (not preview?) (not request?))
            (rf/dispatch [:request-children parent (util/format-story sentences)]))]
    [:section.story 
     [:section.sentences
      (for [{:keys [text id]} sentences]
        ^{:key id} [node text id "parent"])]
     (if request?
       [pending]
       [:section.children
        (for [{:keys [id text]} children]
          ^{:key id} [node text id "child"])])]))

;;; Sidebar

(defn format-title [title]
  (if-not (str/blank? title)
    (-> title (str/replace #"[',\"\.\!]" "") util/title-case)
    "Generating title..."))

(defn sidebar []
  (let [{:keys [title model author]} @(rf/subscribe [:meta])
        _ (rf/dispatch [:page-title-story title])]
    [:aside
     [:section.title>h1 (format-title title)]
     [:section.byline "By " author " & " model]
     [tree-map]
     [:section.meta "Last Exploration 14:32, 2020-01-21"]]))

;;; Main

(defn multiverse []
  (let [story? @(rf/subscribe [:active-story])]
    (if-not story?
      [landing]
      [:main.multiverse
       [sidebar]
       [story]])))
