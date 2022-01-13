(ns org.motform.multiverse.components.new-story
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [org.motform.multiverse.routes             :as routes]
            [org.motform.multiverse.components.sidebar :refer [sidebar]]))

(defn submit-btn [{:keys [text author]}]
  [:a.submit-btn
   (if (every? (complement #(str/blank? %)) [text author])
     {:on-click #(rf/dispatch [:submit-new-story])
      :href (routes/url-for :story)}
     {:class "btn-inactive"})
   "confirm"])

(defn prompt [text]
  [:section.prompt
   [:label "prompt the model, the more detailed the better"]
   [:textarea 
    {:value text
     :autoFocus true
     :on-change #(rf/dispatch [:prompt :text (-> % .-target .-value)])}]])

(defn sidebar-content []
  [:div])

(defn new-story []
  (let [{:keys [text] :as input} @(rf/subscribe [:new-story])]
    [:div.app-container.h-stack
     [sidebar sidebar-content]
     [:main 
      [prompt text]
      [:div.setup-meta
       [submit-btn input]]]]))
