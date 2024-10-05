(ns org.motform.multiverse.open-ai)

(defn auth [api-key]
  {"Authorization" (str "Bearer " api-key)})

(def endpoint
  {:chat   "https://api.openai.com/v1/chat/completions"
   :models "https://api.openai.com/v1/models"})

(def valid-model?
  #{:gpt-3.5-turbo :gpt-4-1106-preview :gpt-4})

(def valid-role?
  #{:user :assistant :system})

(defn ->message
  "Create a message map with the given role and content."
  [role ^String content]
  (:pre [(valid-role? role)])
  {:role (name role) :content content})

(defn completion-texts [completion]
  (map #(-> % :message :content)
       (:choices completion)))

;; Chat 

(def system-message
  (->message :system
             "You are an award winning AUTHOR writing an experimental, hypertext story.
Your work is boundary pushing and the stories you write are often nonlinear.
Twists and turns are your specialty. Characters meander through your stories,
often in a postmodern fashion. You are a master of the craft. You are a genius.
The story is written in a contemporary, neutral style.
BE CONCISE. Be creative. Be weird. Be yourself. Write like your life depends on it.
Your response should ONLY BE THE THE NEXT SENTENCE OF THE STORY.
It is very important that you only respond WITH A SINGLE SENTENCE, or else the game will break.
NEVER include your prompt, or any other texts other than THE NEXT SENTENCE ONLY."))

(def next-sentence
  (->message :user
             "Now it is your time to write the next sentence.
It is VERY important that respond in the style you were ask to emulate.
As a reminder, your style is: The story is written in a contemporary, neutral style.
The next sentence is:"))


(defn request-next-sentence [model paragraphs]
  {:pre [(valid-model? model)]}
  {:n 3
   :model model
   :messages (flatten
               [system-message
                (map #(->message :assistant (:sentence/text %)) paragraphs)
                next-sentence])})

;; Title

(def title-prompt
  "You are an award winning AUTHOR writing an experimental, hypertext story.
Your work is boundary pushing and your titles are witty, smart and experimental. You will be provided with a short story.
Your task is to SUGGEST A TITLE FOR IT. The title should give the reader a good idea of the story, in a clever, funny way.
Respond ONLY with the title and no other content.
NEVER repeat anything from your prompt.
DO NOT ENCLOSE THE TITLE IN QUOTATION MARKS.")

(defn request-title [model paragraphs]
  {:pre [(valid-model? model)]}
  (let [sentences (map :sentence/text paragraphs)]
    {:n 1
     :model model
     :messages [(->message :system title-prompt)
                (->message :user (apply str "Here is the short story:\n" sentences))]}))
