(ns org.motform.multiverse.components.personalities
  (:require [re-frame.core :as rf]))

(defn personality []
  [:div.personality.shadow-medium])

(defn personalities []
  [:aside.personalities.v-stack.gap-full.centered
   (for [_ (range 5)]
     [personality])])
