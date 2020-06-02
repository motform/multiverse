(ns multiverse.main
  (:require [multiverse.components.app :refer [app]]
            [multiverse.db :as db]
            [multiverse.routes :as routes]
            [multiverse.events]
            [multiverse.subs]
            [goog.dom :as gdom]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [day8.re-frame.http-fx]))

(enable-console-print!) 

(defn render []
  (r/render [app]
            (gdom/getElement "mount")))

(defn ^:dev/after-load clear-cache-and-render! []
  (rf/clear-subscription-cache!)
  (render))

(defn ^:export mount []
  (routes/app-routes)
  (rf/dispatch-sync [:initialize-db db/default-db])
  (render))
