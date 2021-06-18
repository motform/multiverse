(ns org.motform.multiverse.main
  (:require [day8.re-frame.http-fx]
            [goog.dom :as gdom]
            [re-frame.core :as rf]
            [reagent.dom :as rdom]
            [org.motform.multiverse.components.app :refer [app]]
            [org.motform.multiverse.db :as db]
            [org.motform.multiverse.events]
            [org.motform.multiverse.routes :as routes]
            [org.motform.multiverse.subs]))

(enable-console-print!) 

(defn render []
  (rdom/render [app] (gdom/getElement "mount")))

(defn ^:dev/after-load clear-cache-and-render! []
  (rf/clear-subscription-cache!)
  (render))

(defn ^:export mount []
  (routes/app-routes)
  (rf/dispatch-sync [:initialize-db db/default-db])
  (render))
