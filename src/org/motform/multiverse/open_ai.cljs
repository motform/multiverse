(ns org.motform.multiverse.open-ai
  (:require [re-frame.core :as rf]))

(def endpoint
  {:chat   "https://api.openai.com/v1/chat/completions"
   :models "https://api.openai.com/v1/models"})

;; TODO: Update for chat
(defn format-title [story]
  (str "I was asked to give a title to this story:\n\"\"\"\n\n"
    (->> story (map :sentence/text) (interpose " ") (apply str))
    "\n\"\"\"\nThe title i came up with was:\n\"\"\""))

;;; Personalties

(def personality
  #:personality
   {:neutral "The story is written in a contemporary, neutral style."
    :sf      "The story is written in the style of science-fiction, similiar to that of Assimov. The story is set in the future." 
    :fantasy "The story is written in the style of high fantasy, similar to that of Tolkien."
    :poetic  "The story is in the style of a classical epic poem, similar to the Divine Comedy or the Greek classics." })

(defn ->message [role content]
  {:role role :content content})

(defn system-message [style]
  (->message "system"
    (str
      "You are an award winning AUTHOR writing an experimental, hypertext story.
Your work is boundary pushing and the stories you write are often nonlinear.
Twists and turns are your specialty. Characters meander through your stories,
often in a postmodern fashion. You are a master of the craft. You are a genius."
      style
      "BE CONCISE. Be creative. Be weird. Be yourself. Write like your life depends on it.
Your response should ONLY BE THE THE NEXT SENTENCE OF THE STORY.
It is very important that you only respond WITH A SINGLE SENTENCE, or else the game will break.
NEVER include your prompt, or any other texts other than THE NEXT SENTENCE ONLY.")))

(defn next-sentence [style]
  (->message "user"
    (str
      "Now it is your time to write the next sentence."
      "It is VERY important that respond in the style you were ask to emulate. As a reminder, your style is:"
      style
      "The next sentence is:")))

(def valid-models
  #{:gpt-3.5-turbo :gpt-4-1106-preview :gpt-4})

(defn payload
  "Returns space-delimited str from a vec of `paragraph`"
  [model prompt]
  {:pre [(valid-models model)]}
  (let [style (personality @(rf/subscribe [:personality/active]))]
    {:n 3
     :model model
     :messages (flatten [(system-message style)
                         (map #(->message "assistant" (:sentence/text %)) prompt)
                         (next-sentence style)])}))
