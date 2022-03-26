(ns org.motform.multiverse.components.new-story
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [org.motform.multiverse.routes :as routes]
            [org.motform.multiverse.components.personality :as personality]))

(defn personalities []
  (let [active-personality @(rf/subscribe [:personality/active])]
    [:section.prompt-personalities.prompt-background.rounded-large.shadow-large.pad-quarter.gap-half
     {:class (str "prompt-" (name active-personality))}
     (for [personality @(rf/subscribe [:personality/personalities])]
       ^{:key personality}
       [personality/toggle-new-story personality active-personality])]))

(defn prompt []
  (let [prompt @(rf/subscribe [:new-story/prompt])
        blank? (str/blank? prompt)]
    [:section.prompt.prompt-background.v-stack.gap-half.rounded-large.shadow-large.pad-half
     {:class (str "prompt-" (name @(rf/subscribe [:personality/active])))}
     [:textarea#prompt-textarea.textarea-large.rounded.shadow-large.pad-half.blurred
      {:value prompt
       :auto-focus true
       :on-change #(rf/dispatch [:new-story/update-prompt (.. % -target -value)])}]
     [:section.h-stack.spaced
      [:p.prompt-description "Your prompt is displayed as " [:span.root] " in the literary space."]
      [:button.rounded.shadow-medium.tab.prompt-button-submit.blurred
       {:disabled blank?
        :on-pointer-down #(when (not blank?)
                            (rf/dispatch [:new-story/submit]) ; TODO move into route controller
                            (rf/dispatch [:page/active :page/story])
                            (rf/dispatch [:story/mode :mode/explore])
                            (. (.-history js/window) pushState #js {} "" (routes/url-for :page/story)))} ; TODO move into routing
       "Explore"]]]))

(defn new-story []
  [:main.new-story.v-stack
   [:div.new-story-container.v-stack.gap-full.centered
    [:div.gap-half.landing-blurb.v-stack
     [:h2.prompt-title "Explore a new literary space"]] 
    [:div.gap-half.landing-blurb.v-stack
     [:p "First, select a the literary style of the generative network. The style affects the direction that the exploration is taking by nudging the algorithm. Don't think too hard about it, you can change style at any point during the exploration."]]
    [personalities]
    [:div.gap-full.landing-blurb.v-stack
     [:p "Second, write an story prompt. Language models, despite being trained on massive data sets of text, always require something to instagate the generative process. The prompt serves as a root from which all other points in the literary space will branch. Experiment with points of view, given names or even pop-cultural references. Two or three sentences are usually enough to get the process going."]]
    [prompt]]])


