(ns org.motform.multiverse.components.personalities
  (:require [re-frame.core :as rf]))

(defn personality-toggle [{:keys [id]} active-personality]
  [:div.personality.shadow-medium.tooltip-container
   {:class (str "personality-" (when-not (= id active-personality) "in") "active")}
   [:span.tooltip.rounded {:style {:width 100 :margin-left -50}} (name id)]])

(defn personalities []
  (let [active-personality @(rf/subscribe [:active-personality])]
    [:aside.personalities.v-stack.gap-full.centered
     (for [personality @(rf/subscribe [:personalities])]
       ^{:key (:id personality)}
       [personality-toggle personality active-personality])]))
