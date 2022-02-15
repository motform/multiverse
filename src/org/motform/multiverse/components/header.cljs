(ns org.motform.multiverse.components.header
  (:require [org.motform.multiverse.routes :as routes]
            [org.motform.multiverse.util :as util]
            [re-frame.core :as rf]))

(defn item [label key active-page tooltip class]
  [:a.hitem.tooltip-container.shadow-small
   {:class (str class " " (when (= key active-page) "highlight"))
    :href (routes/url-for key)}
   label
   [:span.tooltip.rounded tooltip]])

(defn header [content]
  (let [active-page @(rf/subscribe [:page/active])]
    [:header.header.h-stack.spaced
     [:section.header-content (or content [:div])]
     [:nav.h-stack.gap-half
      [item util/icon-library :page/library   active-page "Library"]
      [item util/icon-plus    :page/new-story active-page "New"]]]))
