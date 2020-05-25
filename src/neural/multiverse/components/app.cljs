(ns neural.multiverse.components.app
  (:require [re-frame.core :as rf]
            [neural.multiverse.components.about :refer [about]]
            [neural.multiverse.components.header :refer [header]]
            [neural.multiverse.components.library :refer [library]]
            [neural.multiverse.components.new-story :refer [new-story]]
            [neural.multiverse.components.story :refer [multiverse]]))

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
