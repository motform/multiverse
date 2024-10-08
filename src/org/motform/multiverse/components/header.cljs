(ns org.motform.multiverse.components.header
  (:require
    [org.motform.multiverse.components.map :as map]
    [org.motform.multiverse.icon :as icon]
    [org.motform.multiverse.routes :as routes]
    [re-frame.core :as rf]
    [reagent.core :as r]))

(defn Item [key active-page type label]
  (let [*visible? (r/atom false)]
    (fn [key active-page type label]
      (let [active? (= key active-page)]
        [:<>
         [:a.hitem.tab.shadow-medium
          {:class (str (case type :library "tab tab-secondary library-icon" :new-story " tab-new-story")
                       (when active? " tab-active"))
           :href (routes/url-for key)
           :on-pointer-over #(reset! *visible? true)
           :on-pointer-out  #(reset! *visible? false)}
          label]
         (when (and (= type :new-story)
                    (or @*visible? active?))
           [:label.floating-tab-label
            {:class (when (and @*visible? (not active?)) "tab-label-inactive")}
            "Add new story"])]))))

(defn Tab [{:story/keys [title id]} active-story-id active-page]
  [:div.tab.shadow-medium.tooltip-container.blurred
   {:class (when (and (= active-page :page/story)
                      (= active-story-id id)) "tab-active")
    :on-pointer-over #(rf/dispatch [:tab/highlight id])
    :on-pointer-out  #(rf/dispatch [:tab/remove-highlight])
    :on-pointer-down #(do (rf/dispatch [:story/active id])
                          (rf/dispatch [:page/active :page/story])
                          (. (.-history js/window) pushState #js {} "" (routes/url-for :page/story)))}
   [:<>
    [:p.tab-label title]
    (when (= id @(rf/subscribe [:tab/highlight]))
      [:div.tab-map.rounded.shadow-large
       [map/RadialMap :source/header id {} {:w 400 :h 250}]])]])

(defn Tabs []
  (let [active-story-id @(rf/subscribe [:story/active])
        active-page @(rf/subscribe [:page/active])
        stories (->> @(rf/subscribe [:story/recent]) reverse (take 3))]
    [:nav.tabs.h-stack.gap-half
     (for [{:story/keys [id] :as story} stories]
       ^{:key id} [Tab story active-story-id active-page])]))

(defn Header []
  (let [active-page @(rf/subscribe [:page/active])]
    [:header.header.h-stack.spaced.pad-3-4
     [:section.header-content.h-stack.gap-half
      [Tabs]
      [Item :page/new-story active-page :new-story icon/plus]]
     [Item :page/library active-page :library "Library"]]))
