(ns org.motform.multiverse.open-ai
  (:require
    [org.motform.multiverse.prompts :as prompts]))

(defn auth [api-key]
  {"Authorization" (str "Bearer " api-key)})

(def endpoint
  {:chat   "https://api.openai.com/v1/chat/completions"
   :models "https://api.openai.com/v1/models"})

(def valid-model?
  #{::gpt-3.5-turbo ::gpt-4-1106-preview ::gpt-4o-2024-08-06})

(def valid-role?
  #{::user ::assistant ::system})

(defn ->message
  "Create a message map with the given role and content."
  [role ^String content]
  (:pre [(valid-role? role)])
  {:role (name role) :content content})

(defn completion-texts [completion]
  (map #(-> % :message :content)
       (:choices completion)))

;; Chat 

(defn request-next-sentence [& {:keys [model paragraphs system-message user-message]}]
  {:pre [(valid-model? model)]}
  {:n 3
   :model model
   :messages (flatten
               [(->message ::system system-message)
                (map #(->message ::assistant (:sentence/text %)) paragraphs)
                (->message ::user user-message)])})

;; Title

(defn request-title [model paragraphs]
  {:pre [(valid-model? model)]}
  (let [sentences (map :sentence/text paragraphs)]
    {:n 1
     :model model
     :messages [(->message ::system prompts/title)
                (->message ::user (apply str "Here is the short story:\n" sentences))]}))
