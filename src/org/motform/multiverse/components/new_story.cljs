(ns org.motform.multiverse.components.new-story
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [org.motform.multiverse.routes :as routes]))

(defn li-model [name description active-model]
  [:li
   {:class (if (= name active-model) "model-active" "model-inactive")
    :on-click #(rf/dispatch [:prompt :model name])}   
   name
   [:div description]])

(defn input-name [author]
  [:section.author
   [:label "Enter your name"]
   [:textarea
    {:value author
     :on-change #(rf/dispatch [:prompt :author (-> % .-target .-value)])}]])

(defn select-model [model]
  [:section.model
   [:label "Select a model"]
   [:ul
    [li-model "GPT-2"    "The OpenAI model (in-)famously proclaimed to be to “dangerous” for public release. A well rounded writer." model]
    [li-model "Reformer" "An efficient transformer model from Google, trained on the novel War and Peace" model]
    [li-model "XLNet"    "Trained on one of the largest data-sets in history, this model always has something unexpected to add." model]]])

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
  (let [{:keys [text author model] :as input} @(rf/subscribe [:new-story])]
    [:main
     [:div.setup-meta
      [input-name author]
      [select-model model]
      [submit-btn input]]
     [prompt text]]))
