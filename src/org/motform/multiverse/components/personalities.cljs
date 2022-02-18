(ns org.motform.multiverse.components.personalities
  (:require [re-frame.core :as rf]))

(defn personality-toggle [personality active-personality]
  [:div.personality.shadow-medium.tooltip-container
   {:class (str "personality-" (name personality) "-" (when-not (= personality active-personality) "in") "active")
    :on-pointer-down #(rf/dispatch [:personality/active personality])}
   [:span.tooltip.rounded {:style {:top "15%" :left "120%"}} (name personality)]])

(defn personalities []
  (let [active-personality @(rf/subscribe [:personality/active])]
    [:aside.personalities.v-stack.gap-half.centered
     (for [personality @(rf/subscribe [:personality/personalities])]
       ^{:key personality} [personality-toggle personality active-personality])]))
