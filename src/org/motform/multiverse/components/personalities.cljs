(ns org.motform.multiverse.components.personalities
  (:require [re-frame.core :as rf]))

(defn personality-toggle [{:personality/keys [id]} active-personality]
  [:div.personality.shadow-medium.tooltip-container
   {:class (str "personality-" (when-not (= id active-personality) "in") "active")}
   [:span.tooltip.rounded {:style {:width 100 :margin-left -50}} (name id)]])

(defn personalities []
  (let [active-personality @(rf/subscribe [:personality/active])]
    [:aside.personalities.v-stack.gap-half.centered.shadow-large.rounded-large.pad-half
     (for [personality @(rf/subscribe [:personality/personalities])]
       ^{:key (:personality/id personality)}
       [personality-toggle personality active-personality])]))
