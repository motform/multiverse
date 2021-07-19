(ns org.motform.multiverse.components.new-story
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [org.motform.multiverse.routes :as routes]))

(defn input-name [author]
  [:section.author
   [:label "Enter your name"]
   [:textarea
    {:value author
     :on-change #(rf/dispatch [:prompt :author (-> % .-target .-value)])}]])

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

(defn new-story []
  (let [{:keys [text author] :as input} @(rf/subscribe [:new-story])]
    [:main
     [prompt text]
     [:div.setup-meta
      [input-name author]
      [submit-btn input]]]))
