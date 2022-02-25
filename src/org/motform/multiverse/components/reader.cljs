(ns org.motform.multiverse.components.reader 
  (:require [re-frame.core :as rf]
            [org.motform.multiverse.util :as util]))

(defn toggle [icon mode tooltip]
  [:div.mode-toggle.tooltip-container
   {:on-pointer-down #(rf/dispatch [:story/mode mode])
    :class (when (= @(rf/subscribe [:story/mode]) mode) "mode-toggle-active")}
   [icon]
   [:span.tooltip.rounded
    {:style {:left "0" :top "120%"}}
    tooltip]])

(defn toggles []
  [:section.mode-toggles.h-stack.gap-quarter
   [toggle util/icon-tree   :story/explore "Explore"]
   [toggle util/icon-square :story/reader  "Read"]
   [toggle util/icon-split  :story/compare "Compare"]])

(defn reader []
  (let [paragraph @(rf/subscribe [:sentence/paragraph])]
    ))
