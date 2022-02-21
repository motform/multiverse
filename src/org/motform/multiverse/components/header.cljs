(ns org.motform.multiverse.components.header
  (:require [org.motform.multiverse.routes :as routes]
            [org.motform.multiverse.util :as util]
            [org.motform.multiverse.components.map :as map]
            [re-frame.core :as rf]))

(defn item [label key active-page tooltip type]
  [:a.hitem.tab.shadow-medium
   {:class (str (case type :library "tab tab-secondary" :new-story " tab-new-story")
                (when (= key active-page) " tab-active"))
    :href (routes/url-for key)}
   (when-not (= type :library) label)
   (when tooltip [:p tooltip])])

(defn tab [{:story/keys [title id]} active-story-id active-page]
  [:div.tab.shadow-medium.tooltip-container
   {:class (when (and (= active-page :page/story)
                      (= active-story-id id)) "tab-active")
    :on-pointer-over #(rf/dispatch [:tab/highlight id])
    :on-pointer-out  #(rf/dispatch [:tab/remove-highlight])
    :on-pointer-down #(do (rf/dispatch [:story/active id])
                          (rf/dispatch [:page/active :page/story])
                          (. (.-history js/window) pushState #js {} "" (routes/url-for :page/story)))}
   [:<>
    [:p title]
    (when (= id @(rf/subscribe [:tab/highlight]))
      [:div.tab-map.rounded.shadow-large [map/radial-map :source/header id]])]])

(defn tabs []
  (let [active-story-id @(rf/subscribe [:story/active])
        active-page @(rf/subscribe [:page/active])]
    [:nav.tabs.h-stack.gap-half
     (for [{:story/keys [id] :as story} @(rf/subscribe [:story/recent])]
       ^{:key id} [tab story active-story-id active-page])]))

(defn header []
  (let [active-page @(rf/subscribe [:page/active])]
    (when @(rf/subscribe [:db/stories])
      [:header.header.h-stack.spaced.pad-3-4
       [:section.header-content.h-stack.gap-half
        [tabs]
        [item util/icon-plus :page/new-story active-page nil :new-story]]
       [:nav.header-icons.v-stack
        [item util/icon-library :page/library active-page "Library" :library]]])))
