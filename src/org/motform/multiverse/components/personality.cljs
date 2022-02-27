(ns org.motform.multiverse.components.personality
  (:require [re-frame.core :as rf]))

(defn toggle-class [personality active-personality]
  (str "personality-" (name personality) "-" (when-not (= personality active-personality) "in") "active"))

(defn toggle [personality active-personality page dominant-personality tooltip-position]
  (let [active? (= personality active-personality)
        replacement-avalible? (and active? (not= personality dominant-personality) (= page :page/story))]
    [:div.personality.shadow-medium.tooltip-container.h-stack.centered
     {:class (str (toggle-class personality active-personality)
                  (when replacement-avalible? " personality-replace"))
      :on-pointer-down (cond replacement-avalible?    #(rf/dispatch [:open-ai/replace-completions personality])
                             (= page :page/new-story) #(rf/dispatch [:personality/active personality])
                             (not active?)            #(rf/dispatch [:personality/active personality] #_[:open-ai/replace-completions personality]))}
     [:span.tooltip.rounded.shadow-small
      {:style (merge tooltip-position (when replacement-avalible? {:width "215px"}))}
      (if replacement-avalible? 
        "Replace unxeplored paths"
        (name personality))]]))

(defn toggles [page]
  (let [active-personality   @(rf/subscribe [:personality/active])
        dominant-personality @(rf/subscribe [:personality/dominant-personality])]
    [:aside.personalities.v-stack.gap-quarter.centered
     (for [personality @(rf/subscribe [:personality/personalities])]
       ^{:key personality} [toggle personality active-personality page dominant-personality
                            {:top "15%" :left "120%"}])]))
