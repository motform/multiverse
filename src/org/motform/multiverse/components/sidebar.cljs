(ns org.motform.multiverse.components.sidebar
  (:require [org.motform.multiverse.routes :as routes]
            [re-frame.core :as rf]))

(def icon-plus
  [:svg
   {:viewbox "0 0 16 16",
    :fill "currentColor",
    :height "16",
    :width "16"}
   [:path
    {:d
     "M8 4a.5.5 0 0 1 .5.5v3h3a.5.5 0 0 1 0 1h-3v3a.5.5 0 0 1-1 0v-3h-3a.5.5 0 0 1 0-1h3v-3A.5.5 0 0 1 8 4z"}]])

(def icon-library
  [:svg.icon
   {:viewbox "0 0 16 16",
    :fill "currentColor",
    :height "16",
    :width "16"}
   [:path {:d "M2.5 3.5a.5.5 0 0 1 0-1h11a.5.5 0 0 1 0 1h-11zm2-2a.5.5 0 0 1 0-1h7a.5.5 0 0 1 0 1h-7zM0 13a1.5 1.5 0 0 0 1.5 1.5h13A1.5 1.5 0 0 0 16 13V6a1.5 1.5 0 0 0-1.5-1.5h-13A1.5 1.5 0 0 0 0 6v7zm1.5.5A.5.5 0 0 1 1 13V6a.5.5 0 0 1 .5-.5h13a.5.5 0 0 1 .5.5v7a.5.5 0 0 1-.5.5h-13z"}]])

(def icon-bookmark
  [:svg.icon
   {:viewbox "0 0 16 16",
    :fill "currentColor",
    :height "16",
    :width "16"}
   [:path {:d
           "M2 2a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v13.5a.5.5 0 0 1-.777.416L8 13.101l-5.223 2.815A.5.5 0 0 1 2 15.5V2zm2-1a1 1 0 0 0-1 1v12.566l4.723-2.482a.5.5 0 0 1 .554 0L13 14.566V2a1 1 0 0 0-1-1H4z"}]])

(defn item [label key active-page tooltip class]
  [:a.hitem.tooltip-container.shadow-small
   {:class (str class " " (when (= key active-page) "highlight")) ; TODO update highlight
    :href (routes/url-for key)}
   label
   [:span.tooltip.rounded tooltip]])

;; TODO Add icons for new story and for lib
(defn sidebar [content]
  (let [active-page @(rf/subscribe [:active-page])]
    [:aside.sidebar
     [:header.sidebar-header.h-stack.spaced
      [item "multiverse" :story active-page "Back to story" "sidebar-wordmark"]
      [:section.h-stack.gap-half
       [item icon-library :library active-page "Library"]
       [item icon-plus :new-story active-page "New story"]]]
     [:section.sidebar-content
      [content]]]))
