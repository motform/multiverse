(ns org.motform.multiverse.components.app
  (:require [re-frame.core :as rf]
            [org.motform.multiverse.components.landing   :refer [landing]]
            [org.motform.multiverse.components.library   :refer [library]]
            [org.motform.multiverse.components.new-story :refer [new-story]]
            [org.motform.multiverse.components.story     :refer [multiverse]]))

(defn active-page [page]
  (case page
    :landing   landing
    :library   library
    :new-story new-story
    :story     multiverse))

(defn app []
  (let [page @(rf/subscribe [:active-page])
        view (active-page page)]
    [view]))
