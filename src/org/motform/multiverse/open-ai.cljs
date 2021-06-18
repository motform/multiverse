(ns org.motform.multiverse.open-ai
  (:require [clojure.set :as set]))

(def valid-engines
  #{:content-filter-alpha-c4 :content-filter-dev :cursing-filter-v6
    :ada
    :babbage
    :curie   :curie-instruct-beta
    :davinci :davinci-instruct-beta})

(def valid-params
  #{:logit_bias :frequency_penalty :presence_penalty
    :stop       :echo              :logprobs
    :stream     :n                 :best_of
    :top_p      :temperature       :max_tokens
    :prompt})

(def param-defaults
  {:max_tokens  64
   :temperature 0.7
   :top_p       1})

(defn- request [engine task params]
  {:url        (str "https://api.openai.com/v1/engines/" engine "/" task)
   :headers    {"content-type" "application/json"}
   :basic-auth ["" api-key]
   :body       (merge param-defaults params)})

(defn completion-with
  {:style/indent 1}
  [engine params]
  {:pre [(valid-engines engine)
         (set/subset? (set (keys params)) valid-params)]}
  (request (name engine) "completions" params))

;; (defn response-body [response]    (-> response  :body parse-json))
;; (defn response-text [response]    (-> response  :body parse-json :open-ai/choices first :open-ai/text))
;; (defn response-choices [response] (->> response :body parse-json :open-ai/choices (map :open-ai/text)))

(defn ->title-template [story]
  (str "Make a title for the following text:\n\"\"\"\n"
       story
       "\n\"\"\"\n"
       "The title of this piece is:"
       "\"\"\""))
