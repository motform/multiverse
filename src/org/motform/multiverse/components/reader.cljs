(ns org.motform.multiverse.components.reader
  (:require
    [clojure.string :as str]
    [re-frame.core :as rf]))

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
