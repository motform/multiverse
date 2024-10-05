(ns org.motform.multiverse.components.app
  (:require
    [org.motform.multiverse.components.header    :refer [Header]]
    [org.motform.multiverse.components.landing   :refer [Landing]]
    [org.motform.multiverse.components.library   :refer [Library]]
    [org.motform.multiverse.components.new-story :refer [NewStory]]
    [org.motform.multiverse.components.story     :refer [Multiverse]]
    [re-frame.core :as rf]))


(defn active-page
  [page]
  (case page
    :page/landing   Landing
    :page/library   Library
    :page/new-story NewStory
    :page/story     Multiverse))


(defn background-class
  [page]
  (case page
    :page/landing
    "background-landing"
    (:page/new-story :page/story)
    (str "background"
         (when @(rf/subscribe [:open-ai/pending-request?]) " fast"))
    "background-other"))


(defn app
  []
  (let [page @(rf/subscribe [:page/active])
        view (active-page page)]
    [:div.app-container.v-stack
     {:class (background-class page)}
     (when-not (= page :page/landing) [Header])
     [view]]))
