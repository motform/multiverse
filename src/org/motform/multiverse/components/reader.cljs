(ns org.motform.multiverse.components.reader
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [org.motform.multiverse.util :as util]))

(defn toggle [icon mode tooltip]
  [:div.mode-toggle.tooltip-container
   {:on-pointer-down #(rf/dispatch [:story/mode mode])
    :class (when (= @(rf/subscribe [:story/mode]) mode) "mode-toggle-active")}
   [icon]
   [:span.tooltip.rounded.shadow-small
    {:style {:left "0" :top "120%"}}
    tooltip]])

(defn toggles []
  [:section.mode-toggles.h-stack.gap-quarter
   [toggle util/icon-tree       :mode/explore "Explore"]
   [toggle util/icon-collection :mode/compare "Compare"]
   [toggle util/icon-text       :mode/reader  "Read"]])

;; TODO use OpenAI to detech paragraphs
;; https://andrewmayneblog.wordpress.com/2020/06/13/openai-api-alchemy-smart-formatting-and-code-creation/
(defn format-story [paragraphs]
  (reduce
   (fn [story {:sentence/keys [text]}]
     (if (str/starts-with? (str/trim text) "\"")
       (conj story text)
       (conj (pop story) (apply str (last story) " " text))))
   [(-> paragraphs first :sentence/text)] (rest paragraphs)))

(defn literary [paragraphs]
  [:article.reader.shadow-large.pad-full.rounded.v-stack.gap-full
   [:h2 (:story/title @(rf/subscribe [:story/meta]))]
   (for [paragraph (format-story paragraphs)]
     [:p paragraph])])

(comment
  )
