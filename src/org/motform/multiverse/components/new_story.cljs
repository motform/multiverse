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
        system-message @(rf/subscribe [:new-story/system-message])
        user-message @(rf/subscribe [:new-story/user-message])
        promt-version @(rf/subscribe [:new-story/prompt-version])
        blank? (str/blank? prompt)]
    [:div.prompt-container
     [:section.prompt.prompt-background.v-stack.gap-full.rounded-large.shadow-large.pad-half
      [:section.v-stack.gap-full.full-width
       [:section.v-stack.gap-half
        [:span.prompt-label "System Message"]
        [:textarea#system-prompt.prompt-textarea.textarea-large.rounded.shadow-large.pad-half
         {:value system-message
          :rows 10
          :disabled (= promt-version :prompt/v1)
          :on-change #(rf/dispatch [:new-story/update :new-story/system-message (.. % -target -value)])}]
        [:p.template-tip "The system message is the message that the AI will see before generating the story.\nOpenAI claims it is weighted more heavily than the user message."]]

       [:section.v-stack.gap-half
        [:span.prompt-label "User Message"]
        [:textarea#system-prompt.prompt-textarea.textarea-large.rounded.shadow-large.pad-half
         {:value user-message
          :rows 6
          :disabled (= promt-version :prompt/v1)
          :on-change #(rf/dispatch [:new-story/update :new-story/user-message (.. % -target -value)])}]
        [:p.template-tip "The user message is the last message the AI sees, after having been given the entire story up to this point."]]

       [PromptVersions]]

      [:section.v-stack.gap-full.full-width
       [:section.v-stack.gap-half
        [:span.prompt-label "Story Prompt"]
        [:textarea#story-prompt.prompt-textarea.textarea-large.rounded.shadow-large.pad-half
         {:value prompt
          :rows 4
          :auto-focus true
          :on-change #(rf/dispatch [:new-story/update :new-story/prompt (.. % -target -value)])
          :on-key-down #(when (and (or (.-metaKey %) (.-ctrlKey %))
                                   (= (.-key %) "Enter"))
                          (submit-story))}]]

       [:section.h-stack.spaced {:style {:align-items "flex-end"}}
        [Models]

        [:button.rounded.shadow-medium.tab.prompt-button-submit.blurred
         {:disabled blank?
          :on-pointer-down #(when-not blank? (submit-story))}
         "Explore"]]]]]))

(defn NewStory []
  [:main.new-story.v-stack.gap-full
   [Prompt]])
