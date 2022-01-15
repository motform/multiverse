(ns org.motform.multiverse.components.new-story
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [org.motform.multiverse.components.header :refer [header]]))

(defn prompt []
  (let [prompt @(rf/subscribe [:new-story])
        blank? (str/blank? prompt)]
    [:section.prompt.v-stack.gap-half
     #_[:label.offset-label "Propmt"]
     [:textarea.textarea-large.rounded-large.border.shadow-large.pad-half
      {:value prompt
       :auto-focus true
       :on-change #(rf/dispatch [:prompt (.. % -target -value)])}]
     [:div.button-container
      [:button.rounded.shadow-medium
       {:disabled blank?
        :on-pointer-down #(when (not blank?)
                            (rf/dispatch [:active-page :story])
                            (rf/dispatch [:submit-new-story]))}
       "prompt"]]]))

(defn sidebar-content []
  [:div])

(defn new-story []
  [:div.app-container.h-stack.overlay
   #_[sidebar sidebar-content]
   [:main.new-story.v-stack
    [:div.new-story-container.v-stack.gap-full.blurred.pad-double.rounded-large.border
     [:div.gap-half.landing-blurb.v-stack
      [:h2.prompt-title "Prompt the network"]
      [:p "to enter a literary space. Language models, despite trained on massive data sets of text, always require something to instagate the generative process."]
      [:p "Try experimenting with points of view, actions and names. Two to three sentences are often enought to get it going."]]
     [prompt]]]])
