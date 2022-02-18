(ns org.motform.multiverse.components.new-story
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [org.motform.multiverse.components.header :refer [header]]
            [org.motform.multiverse.routes :as routes]))

(defn prompt []
  (let [prompt @(rf/subscribe [:new-story/prompt])
        blank? (str/blank? prompt)]
    [:section.prompt.v-stack.gap-half.v-stack.gap-half.rounded-large.shadow-large.blurred.border
     [:textarea.textarea-large.rounded.shadow-large.pad-half
      {:value prompt
       :auto-focus true
       :on-change #(rf/dispatch [:new-story/update-prompt (.. % -target -value)])}]
     [:button.rounded.shadow-medium
      {:disabled blank?
       :on-pointer-down #(when (not blank?)
                           (rf/dispatch [:new-story/submit])
                           (rf/dispatch [:page/active :page/story])
                           (. (.-history js/window) pushState #js {} "" (routes/url-for :page/story)))}
      "Explore"]]))

(defn new-story []
  [:div.app-container.v-stack.overlay
   [header [:p.library-title "NEW STORY"]]
   [:main.new-story.v-stack
    [:div.new-story-container.v-stack.gap-full
     [:div.gap-half.landing-blurb.v-stack
      [:h2.prompt-title "Prompt the network"]
      [:p "to enter a literary space. Language models, despite trained on massive data sets of text, always require something to instagate the generative process."]
      [:p "Try experimenting with points of view, actions and names. Two to three sentences are often enought to get it going."]]
     [prompt]]]])
