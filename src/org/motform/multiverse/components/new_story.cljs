(ns org.motform.multiverse.components.new-story
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [org.motform.multiverse.routes :as routes]
            [org.motform.multiverse.components.personality :as personality]
            [org.motform.multiverse.icon :as icon]))

(defn personalities []
  (let [active-personality @(rf/subscribe [:personality/active])]
    [:section.prompt-personalities.prompt-background.rounded-large.shadow-large.pad-quarter.gap-half
     {:class (str "prompt-" (name active-personality))}
     (for [personality @(rf/subscribe [:personality/personalities])]
       ^{:key personality}
       [personality/toggle-new-story personality active-personality])]))

(defn template-toggle [active icon template tooltip]
  [:div.template-toggle.tooltip-container
   {:on-pointer-down #(rf/dispatch [:new-story/template template])
    :class (when (= active template) "template-toggle-active shadow-small")}
   [icon]
   [:span.tooltip.rounded.shadow-small.tooltip-new-story
    tooltip]])

(defn templates []
  (let [active-template @(rf/subscribe [:new-story/template])
        toggle (partial template-toggle active-template)]
    [:section.template-toggles.h-stack.gap-quarter
     [toggle icon/file     :template/blank   "Blank"]
     [toggle icon/building :template/urban   "Urban experience"]
     [toggle icon/boombox  :template/musical "Musical"]
     [toggle icon/news     :template/news    "Newsworthy"]
     [toggle icon/cpu      :template/ai      "AI co-existance"]]))

(defn prompt []
  (let [prompt @(rf/subscribe [:new-story/prompt])
        blank? (str/blank? prompt)]
    [:section.prompt.prompt-background.v-stack.gap-half.rounded-large.shadow-large.pad-half
     {:class (str "prompt-" (name @(rf/subscribe [:personality/active])))}
     [:textarea#prompt-textarea.textarea-large.rounded.shadow-large.pad-half
      {:value prompt
       :auto-focus true
       :on-change #(rf/dispatch [:new-story/update-prompt (.. % -target -value)])}]
     [:section.h-stack.spaced
      [templates]
      [:button.rounded.shadow-medium.tab.prompt-button-submit.blurred
       {:disabled blank?
        :on-pointer-down #(when (not blank?)
                            (rf/dispatch [:new-story/submit]) ; TODO move into route controller
                            (rf/dispatch [:page/active :page/story])
                            (. (.-history js/window) pushState #js {} "" (routes/url-for :page/story)))} ; TODO move into routing
       "Explore"]]]))

(defn new-story []
  [:main.new-story.v-stack.gap-full
   [:div.gap-half.landing-blurb.v-stack
    [:h3 "Literary style"]
    [:p "The style affects the direction that the exploration is taking by nudging the algorithm. Don't think too hard about it, you can change style at any point."]]
   [personalities]
   [:div.gap-half.landing-blurb.v-stack
    [:h3 "Story prompt"]
    [:p "The prompt serves as a root from which all other points in the literary space will branch. Language models, despite being trained on massive data sets of text, always require something to instagate the generative process. Experiment with points of view, given names or even pop-cultural references."]]
   [prompt]
   [:p.template-tip "Start with a blank slate or a template."]])


