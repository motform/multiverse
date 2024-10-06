(ns org.motform.multiverse.story
  (:require
    [clojure.string :as str]
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

(defn longest-sentence [story]
  (let [sentences (:story/sentences story)
        sentence-with-children (apply max-key #(count (:sentence/children (second %))) sentences)
        base-sentence (second sentence-with-children)
        children (:sentence/children base-sentence)
        children-texts (map #(get-in sentences [% :sentence/text]) children)]
    (str/join "\n" (cons (:sentence/text base-sentence) children-texts))))

(defn ->md [story]
  (let [meta (:story/meta story)]
    (str
      "# " (:story/title meta) "\n"
      "---\n"
      "id:" (:story/id meta) "\n"
      "model:" (name (:story/model meta)) "\n"
      "prompt-version:" (:story/prompt-version meta) "\n"
      "date:" (util/format-date (:story/updated meta)) "\n"
      "---\n"
      (longest-sentence story))))


(def csv-header
  "id,model,prompt-version,date,sentences")

(defn ->csv [story]
  (let [meta (:story/meta story)]
    (str
      "\"" (:story/id meta) "\","
      "\"" (name (:story/model meta)) "\","
      "\"" (:story/prompt-version meta) "\","
      "\"" (util/format-date (:story/updated meta)) "\","
      "\"" (str/replace (longest-sentence story) #"\"" "'") "\"")))
