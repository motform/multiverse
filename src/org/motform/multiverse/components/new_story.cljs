(ns org.motform.multiverse.components.new-story
  (:require
    [clojure.string :as str]
    [org.motform.multiverse.open-ai :as-alias open-ai]
    [org.motform.multiverse.routes :as routes]
    [re-frame.core :as rf]))

(defn Toggle [active db-key value label]
  [:div.template-toggle.tooltip-container
   {:on-pointer-down #(rf/dispatch [db-key value])
    :class (when (= active value) "template-toggle-active shadow-small")}
   label
   #_[:span.tooltip.rounded.shadow-small.tooltip-new-story
    label]])

(defn Models []
  (let [active @(rf/subscribe [:new-story/model])
        Toggle (partial Toggle active :new-story/model)]
    [:section.v-stack.gap-quarter
     [:span.toggle-label "GPT"]
     [:section.template-toggles.h-stack.gap-quarter
      [Toggle ::open-ai/gpt-3.5-turbo "3.5"]
      [Toggle ::open-ai/gpt-4o-2024-08-06 "4o"]]]))

(defn PromptVersions []
  (let [active @(rf/subscribe [:new-story/prompt-version])
        Toggle (partial Toggle active :new-story/prompt-version)]
    [:section.v-stack.gap-quarter
     [:span.toggle-label "Prompt"]
     [:section.template-toggles.h-stack.gap-quarter
      [Toggle :prompt/v1 "v1"]
      [Toggle :prompt/v2 "v2"]]]))

(defn submit-story []
  (rf/dispatch [:new-story/submit])
  (rf/dispatch [:page/active :page/story])
  (. (.-history js/window)
     pushState #js {} "" (routes/url-for :page/story)))

(defn Prompt []
  (let [prompt @(rf/subscribe [:new-story/prompt])
        blank? (str/blank? prompt)]
    [:section.prompt.prompt-background.v-stack.gap-half.rounded-large.shadow-large.pad-half
     [:textarea#prompt-textarea.textarea-large.rounded.shadow-large.pad-half
      {:value prompt
       :auto-focus true
       :on-change #(rf/dispatch [:new-story/update-prompt (.. % -target -value)])
       :on-key-down #(when (and (or (.-metaKey %) (.-ctrlKey %))
                                (= (.-key %) "Enter"))
                       (submit-story))}]
     [:section.h-stack.spaced {:style {:align-items "flex-end"}}
      [:section.h-stack.gap-half
       [Models]
       [PromptVersions]]
      [:button.rounded.shadow-medium.tab.prompt-button-submit.blurred
       {:disabled blank?
        :on-pointer-down #(when-not blank? (submit-story))}
       "Explore"]]]))

(defn NewStory []
  [:main.new-story.v-stack.gap-full
   [Prompt]])
