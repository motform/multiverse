(ns org.motform.multiverse.routes
  (:require
    [bidi.bidi :as bidi]
    [pushy.core :as pushy]
    [re-frame.core :as rf]))

(def titles
  {:page/new-story "New story"
   :page/story     "Exploration"
   :page/library   "Library"})

(def routes
  ["/" {""          :page/landing
        "story"     :page/story
        "new-story" :page/new-story
        "library"   :page/library}])

(defn- parse-url [url]
  (bidi/match-route routes url))

(defn dispatch-route [matched-route]
  (let [page (:handler matched-route)]
    (rf/dispatch [:page/active page])))

(defn app-routes []
  (pushy/start! (pushy/pushy dispatch-route parse-url)))

(def url-for (partial bidi/path-for routes))
