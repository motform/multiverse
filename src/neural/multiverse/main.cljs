(ns neural.multiverse.main
  (:require [neural.multiverse.components.app :refer [app]]
            [neural.multiverse.db :as db]
            [neural.multiverse.routes :as routes]
            [neural.multiverse.events]
            [neural.multiverse.subs]
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
