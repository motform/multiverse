(ns org.motform.multiverse.story
  (:require
    [org.motform.multiverse.util :as util]))

(defn ->sentence [text & {:keys [id path children]}]
  #:sentence{:id id :text text :path path :children children})

(defn ->story [prompt & {:keys [id sentence-id model version system-message user-message]}]
  {:story/meta {:story/id id
                :story/title ""
                :story/updated (js/Date.)
                :sentence/active sentence-id
                :story/model model
                :story/prompt-version version
                :story/system-message system-message
                :story/user-message user-message}
   :story/sentences {sentence-id (->sentence prompt
                                             :id sentence-id
                                             :path [sentence-id]
                                             :children [])}})

(defn ->children
  "Make children map to be merged into sentences."
  [texts & {:keys [child-ids parent-path]}]
  (let [child-pairs (util/pairs child-ids texts)]
    (reduce (fn [children [id text]]
              (assoc children id
                     (->sentence text
                                 :id id
                                 :path (conj parent-path id)
                                 :children [])))
            {} child-pairs)))
