(ns org.motform.multiverse.components.new-story
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [org.motform.multiverse.routes :as routes]
            [org.motform.multiverse.components.personalities :as personalities]))

(defn personalities []
  (let [active-personality @(rf/subscribe [:personality/active])]
    [:section.prompt-personalities.h-stack.gap-half
     (for [personality @(rf/subscribe [:personality/personalities])]
       [personalities/personality-toggle personality active-personality
                                  {:top "120%" :left "0%"}])]))

(defn prompt []
  (let [prompt @(rf/subscribe [:new-story/prompt])
        blank? (str/blank? prompt)]
    [:section.prompt.v-stack.gap-half.rounded-large.shadow-large.blurred.border.pad-half
     [:textarea.textarea-large.rounded.shadow-large.pad-half
      {:value prompt
       :auto-focus true
       :on-change #(rf/dispatch [:new-story/update-prompt (.. % -target -value)])}]
     [:section.h-stack.spaced
      [personalities]
      [:button.rounded.shadow-medium
       {:disabled blank?
        :on-pointer-down #(when (not blank?)
                            (rf/dispatch [:new-story/submit]) ; TODO move into route controller
                            (rf/dispatch [:page/active :page/story])
                            (. (.-history js/window) pushState #js {} "" (routes/url-for :page/story)))}
       "Explore"]]]))

(defn new-story []
  [:main.new-story.v-stack
   [:div.new-story-container.v-stack.gap-full
    [:div.gap-half.landing-blurb.v-stack
     [:h2.prompt-title "Prompt the network"]
     [:p "to enter a literary space. Language models, despite trained on massive data sets of text, always require something to instagate the generative process."]
     [:p "Try experimenting with points of view, actions and names. Two to three sentences are often enought to get it going."]]
    [prompt]]])
