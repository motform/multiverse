(ns org.motform.multiverse.components.app
  (:require [re-frame.core :as rf]
            [org.motform.multiverse.components.header    :refer [header]]
            [org.motform.multiverse.components.landing   :refer [landing]]
            [org.motform.multiverse.components.library   :refer [library]]
            [org.motform.multiverse.components.new-story :refer [new-story]]
            [org.motform.multiverse.components.story     :refer [multiverse]]))

(defn active-page [page]
  (case page
    :page/landing   landing
    :page/library   library
    :page/new-story new-story
    :page/story     multiverse))

(defn app []
  (let [page @(rf/subscribe [:page/active])
        view (active-page page)]
    [:div.app-container.v-stack
     (when-not (= page :page/landing) [header])
     [view]]))
