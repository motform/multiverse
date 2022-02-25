(ns org.motform.multiverse.components.personality
  (:require [re-frame.core :as rf]))

(defn toggle-class [personality active-personality]
  (str "personality-" (name personality) "-" (when-not (= personality active-personality) "in") "active"))

(defn toggle [personality active-personality page children-to-replace? tooltip-position]
  (let [active? (= personality active-personality)
        replacement-avalible? (and active? children-to-replace? (= page :page/story))]
    [:div.personality.shadow-medium.tooltip-container.h-stack.centered
     {:class (str (toggle-class personality active-personality)
                  (when replacement-avalible? " personality-replace"))
      :on-pointer-down (cond replacement-avalible? #(rf/dispatch [:open-ai/replace-completions personality])
                             (not active?) (case page
                                             :page/story     #(rf/dispatch [:open-ai/replace-completions personality])
                                             :page/new-story #(rf/dispatch [:personality/active personality])))}
     [:span.tooltip.rounded.shadow-small
      {:style (merge tooltip-position (when replacement-avalible? {:width "256px"}))}
      (if replacement-avalible? 
        "Replace unxeplored paths"
        (name personality))]]))

(defn toggles [page]
  (let [active-personality @(rf/subscribe [:personality/active])
        children-to-replace? @(rf/subscribe [:personality/childern-to-replace? active-personality])]
    [:aside.personalities.v-stack.gap-quarter.centered
     (for [personality @(rf/subscribe [:personality/personalities])]
       ^{:key personality} [toggle personality active-personality page children-to-replace?
                            {:top "15%" :left "120%"}])]))
