(ns org.motform.multiverse.components.header
  (:require [org.motform.multiverse.routes :as routes]
            [re-frame.core :as rf]))

(def icon-plus
  [:svg
   {:view-box "0 0 18 18",
    :fill "currentColor",
    :height "18",
    :width "18"}
   [:path {:d "M8 4a.5.5 0 0 1 .5.5v3h3a.5.5 0 0 1 0 1h-3v3a.5.5 0 0 1-1 0v-3h-3a.5.5 0 0 1 0-1h3v-3A.5.5 0 0 1 8 4z"}]])

(def icon-library
  [:svg.icon
   {:view-box "0 0 16 16",
    :fill "currentColor",
    :height "16",
    :width "16"}
   [:path {:d "M2.5 3.5a.5.5 0 0 1 0-1h11a.5.5 0 0 1 0 1h-11zm2-2a.5.5 0 0 1 0-1h7a.5.5 0 0 1 0 1h-7zM0 13a1.5 1.5 0 0 0 1.5 1.5h13A1.5 1.5 0 0 0 16 13V6a1.5 1.5 0 0 0-1.5-1.5h-13A1.5 1.5 0 0 0 0 6v7zm1.5.5A.5.5 0 0 1 1 13V6a.5.5 0 0 1 .5-.5h13a.5.5 0 0 1 .5.5v7a.5.5 0 0 1-.5.5h-13z"}]])

(defn item [label key active-page tooltip class]
  [:a.hitem.tooltip-container.shadow-small
   {:class (str class " " (when (= key active-page) "highlight"))
    :href (routes/url-for key)}
   label
   [:span.tooltip.rounded tooltip]])

(defn header [content]
  (let [active-page @(rf/subscribe [:active-page])]
    [:aside.header.h-stack.spaced
     [item "Multiverse" :story active-page "Story" "header-wordmark"] ; TODO should be the H1
     [:section.header-content (or content [:div])]
     [:nav.h-stack.gap-half
      [item icon-library :library active-page "Library"]
      [item icon-plus :new-story active-page "New"]]]))
