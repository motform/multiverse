(ns org.motform.multiverse.routes
  (:require [bidi.bidi :as bidi]
            [re-frame.core :as rf]
            [pushy.core :as pushy]))

(def titles
  {:about "About"
   :new-story "New story"
   :library "Library"})

(def routes ["/" {"" :story
                  "new-story" :new-story
                  "library" :library
                  "about" :about}])

(defn- parse-url [url]
  (bidi/match-route routes url))

(defn dispatch-route [matched-route]
  (let [page (:handler matched-route)]
    (rf/dispatch [:active-page page])))

(defn app-routes []
  (pushy/start! (pushy/pushy dispatch-route parse-url)))

(def url-for (partial bidi/path-for routes))
