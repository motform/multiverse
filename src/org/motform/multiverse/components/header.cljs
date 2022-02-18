(ns org.motform.multiverse.components.header
  (:require [org.motform.multiverse.routes :as routes]
            [org.motform.multiverse.util :as util]
            [re-frame.core :as rf]))

(defn item [label key active-page tooltip type]
  [:a.hitem.tab
   {:class (str (case type :library "tab" :new-story " tab-new-story")
                (when (= key active-page) " tab-active"))
    :href (routes/url-for key)}
   (when-not (= type :library) label)
   (when tooltip [:p tooltip])])

(defn tabs []
  (let [active-story-id @(rf/subscribe [:story/active])
        active-page @(rf/subscribe [:page/active])]
    [:nav.tabs.h-stack.gap-half
     (for [{:story/keys [title id]} @(rf/subscribe [:story/recent])]
       ^{:key id}
       [:div.tab
        {:class (when (and (= active-page :page/story)
                           (= active-story-id id)) "tab-active")
         :on-pointer-down #(do (rf/dispatch [:story/active id])
                               (rf/dispatch [:page/active :page/story])
                               (. (.-history js/window) pushState #js {} "" (routes/url-for :page/story)))
         ;; :on-pointer-over #(rf/dispatch [:story/active id])
         ;; :on-pointer-out #(rf/dispatch  [:story/active active-story-id])
         }
        title])]))

(defn header []
  (let [active-page @(rf/subscribe [:page/active])]
    [:header.header.h-stack.spaced
     [:section.header-content.h-stack.gap-half
      [tabs]
      [item util/icon-plus :page/new-story active-page nil :new-story]]
     [:nav.header-icons.v-stack
      [item util/icon-library :page/library active-page "Library" :library]]]))
