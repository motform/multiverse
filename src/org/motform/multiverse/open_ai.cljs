(ns org.motform.multiverse.open-ai
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [re-frame.core :as rf]))

(def valid-engines
  #{:content-filter-alpha-c4 :content-filter-dev :cursing-filter-v6
    :ada
    :babbage
    :curie   :curie-instruct-beta
    :davinci :text-davinci-001})

(def valid-params
  #{:logit_bias :frequency_penalty :presence_penalty
    :stop       :echo              :logprobs
    :stream     :n                 :best_of
    :top_p      :temperature       :max_tokens
    :prompt})

(def param-defaults
  {:max_tokens  64
   :temperature 0.8
   :n           3
   :top_p       1})

(defn- request [engine task params]
  {:uri    (str "https://api.openai.com/v1/engines/" engine "/" task)
   :params (merge param-defaults params)})

(defn completion-with
  {:style/indent 1}
  [engine params]
  {:pre [(valid-engines engine)
         (set/subset? (set (keys params)) valid-params)]}
  (request (name engine) "completions" params))

(defn format-title [story]
  (str "I was asked to give a title to this story:\n\"\"\"\n\n"
       (->> story (map :sentence/text) (interpose " ") (apply str))
       "\n\"\"\"\nThe title i came up with was:\n\"\"\""))

(defn random-story-start []
  (rand-nth ["I was walking my dog down the Avenue of the Americas. When suddenly, the sky turned dark."
             "\"Well, well, well, what do we have here?\", they said. Fury ravaging in their eyes."
             "It was a calm day, butterflies fluttering as winds blew softly from the north."
             "Urban centers often come to envelop suburban areas, I aruged. The crowd did not seem pleased."]))

(defn training-template [style-description example-completions]
  {:style    style-description
   :template (->> example-completions
                  (map #(str "Write the next sentence of this story. " style-description
                             "\nStory: " (random-story-start) "\n"
                             "Next sentence: " %))
                  (str/join "\n\n"))})

;; It would be great to do this kind of thing on a larger scale using Gutenberg data, but I don't think I will have time for that any time soon…
(def few-shot-personalities
  #:personality
   {:neutral (training-template "" [])

    :sf      (training-template "The story is written in the style of science-fiction"
                                ["The ship slowly came to halt, having just left transgalatic light-speed."
                                 "Sentient AI ruled this planet, at first colonized by humans, only to be occupied by the android."
                                 "My dog was of a curious alien breed, faintly glowing in the dark every time another Auphorian passed by."])

    :fantasy (training-template "The story is written in the style of high fantasy."
                                ["I look up, and a pack of wild harpies where swarming us."
                                 "The elven troops rallied, their long hair reflecting the faint sunlight that was left."
                                 "There where all kinds of wonderous creatures, a mightly display of life and magic."])

    :poetic  (training-template "The story is in the style of an epic poem, similar to the Divine Comedy or the Greek classics."
                                ["Oh heavens, how be thy of such ruthe! Oh hells, why dost thou discrimiate even around fools?"
                                 "Of Beatrice, and that saintly walk. That it may issue, bearing true report. Of the mind's impress; not aught thy words."
                                 "How hard the valley desceneds and climbs, how long the Avenue stretches along the shroes."])

    :unhigned (training-template "The story is in the style of an epic poem, similar to the Divine Comedy or the Greek classics." ; TODO: Write prompts
                                 ["Oh heavens, how be thy of such ruthe! Oh hells, why dost thou discrimiate even around fools?"
                                  "Of Beatrice, and that saintly walk. That it may issue, bearing true report. Of the mind's impress; not aught thy words."
                                  "How hard the valley desceneds and climbs, how long the Avenue stretches along the shroes."])})

(defn format-prompt
  "Returns space-delimited str from a vec of `paragraph`"
  [paragraph]
  (let [{:personality/keys [id]} (get @(rf/subscribe [:personality/personalities]) @(rf/subscribe [:personality/active]))
        {:keys [style template]} (few-shot-personalities id)]
    (str template (when-not (str/blank? template) "\n\n")
         "Write the next sentence of this story." style "\n"
         "Story: " (->> paragraph (map :sentence/text) (interpose " ") (apply str)) "\n"
         "Next sentence:")))
