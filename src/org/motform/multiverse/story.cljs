(ns org.motform.multiverse.story
  (:require
    [org.motform.multiverse.util :as util]))

(defn ->sentence [id text path children]
  #:sentence{:id id :text text :path path :children children})

(defn ->story [story-id sentence-id prompt]
  {:story/meta {:story/id story-id
                :story/title ""
                :story/updated (js/Date.)
                :sentence/active sentence-id}
   :story/sentences {sentence-id (->sentence sentence-id prompt [sentence-id] [])}})

(def prompt-templates
  #:template
  {:blank   ""
   :urban   "I was walking my tan Whippet down the Avenue of the Ameriacs, when suddenly, the ground begain to shake. We looked up in unison and where utterly shocked to see..."
   :musical "Lyrics: ♪ Bunnies aren’t just cute like everyone supposes. They got them hoppy legs and twitchy little noses, and what’s with all the carrots!? ♪"
   :news    "BREAKING NEWS: The world's largest pumpkin has turned sentient is a chocking turn of events. But while some have resorted to running amok on the streets, local farmers claim the incident as a \"Relatively commonplace fall missunderstanding\"."
   :ai      "The rapid progression of AI technolgies had worried Sam. They argued benevolence as never given, citing Assmiov's laws as thin veneer. That all changed once GLADOS came into the picture."})

(defn ->children
  "Make children map to be merged into sentences."
  [parent-path child-ids texts]
  (let [child-pairs (util/pairs child-ids texts)]
    (reduce (fn [children [id text]]
              (assoc children id (->sentence id text (conj parent-path id) [])))
            {} child-pairs)))
