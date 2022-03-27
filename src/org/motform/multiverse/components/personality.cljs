(ns org.motform.multiverse.components.personality
  (:require [re-frame.core :as rf]))

(defn toggle-class [personality active-personality]
  (str "personality-" (name personality) "-" (when-not (= personality active-personality) "in") "active"))

(def personality-descriptions
  {:personality/neutral "No fuzz"
   :personality/sci-fi  "Aliens"
   :personality/fantasy "Dragons"
   :personality/poetic  "Grandiose"})

(defn toggle-new-story [personality active-personality]
  [:div.personality-toggle.prompt-personality-toggle.h-stack.centered.gap-half.rounded.pad-quarter
   {:on-pointer-down #(rf/dispatch [:personality/active personality])
    :class (when (= personality active-personality) "prompt-personality-toggle-active")}
   [:div.personality.shadow-medium.tooltip-container.h-stack.centered
    {:class (str (toggle-class personality active-personality))}]
   [:div.v-stack.gap-eight
    [:label.personality-label personality]
    [:p.prompt-description (personality-descriptions personality)]]])

(defn toggle-story [personality active-personality dominant-personality]
  (let [active? (= personality active-personality)
        replacement-avalible? (and active? (not= personality dominant-personality))]
    [:div.personality.shadow-medium.tooltip-container.h-stack.centered
     {:class (str (toggle-class personality active-personality)
                  (when replacement-avalible? " personality-replace"))
      :on-pointer-down (cond replacement-avalible?  #(rf/dispatch [:open-ai/replace-completions personality])
                             (not active?)          #(do (rf/dispatch [:personality/active personality])
                                                         (rf/dispatch [:open-ai/replace-completions personality])))}
     [:span.tooltip.rounded.shadow-small
      {:style (merge {:top "15%" :left "120%"}
                     (when replacement-avalible? {:width "174px"}))}
      (if replacement-avalible? "Replace suggestions" (name personality))]]))

(defn toggles [page]
  (let [active-personality   @(rf/subscribe [:personality/active])
        dominant-personality @(rf/subscribe [:personality/dominant-personality])]
    [:aside.personalities.v-stack.gap-quarter.centered
     (for [personality @(rf/subscribe [:personality/personalities])]
       ^{:key personality}
       [toggle-story personality active-personality dominant-personality])]))
