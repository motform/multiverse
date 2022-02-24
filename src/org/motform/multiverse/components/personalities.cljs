(ns org.motform.multiverse.components.personalities
  (:require [re-frame.core :as rf]))

(defn personality-toggle [personality active-personality page tooltip-position]
  [:div.personality.shadow-medium.tooltip-container
   {:class (str "personality-" (name personality) "-" (when-not (= personality active-personality) "in") "active")
    :on-pointer-down (when-not (= personality active-personality)
                       (case page
                         :page/story     #(rf/dispatch [:open-ai/replace-completions personality])
                         :page/new-story #(rf/dispatch [:personality/active personality])))}
   [:span.tooltip.rounded
    {:style tooltip-position}
    (name personality)]])

(defn personalities [page]
  (let [active-personality @(rf/subscribe [:personality/active])]
    [:aside.personalities.v-stack.gap-half.centered
     (for [personality @(rf/subscribe [:personality/personalities])]
       ^{:key personality} [personality-toggle personality active-personality page
                            {:top "15%" :left "120%"}])]))
