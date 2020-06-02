(ns multiverse.components.app
  (:require [re-frame.core :as rf]
            [multiverse.components.about :refer [about]]
            [multiverse.components.header :refer [header]]
            [multiverse.components.library :refer [library]]
            [multiverse.components.new-story :refer [new-story]]
            [multiverse.components.story :refer [multiverse]]))

(defn active-page [page]
  (case page
    :about [about]
    :library [library]
    :new-story [new-story]
    :story [multiverse]))

(defn app []
  (let [page @(rf/subscribe [:active-page])]
    [:<>
     [header]
     [active-page page]]))
