(ns org.motform.multiverse.components.app
  (:require [re-frame.core :as rf]
            [org.motform.multiverse.components.about     :refer [about]]
            [org.motform.multiverse.components.library   :refer [library]]
            [org.motform.multiverse.components.new-story :refer [new-story]]
            [org.motform.multiverse.components.story     :refer [multiverse]]))

(defn app []
  (let [page @(rf/subscribe [:active-page])]
    (case page
      :about     [about] ; TODO remove
      :library   [library]
      :new-story [new-story]
      :story     [multiverse])))
