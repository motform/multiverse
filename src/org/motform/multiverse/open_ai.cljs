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
   :n           3
   :top_p       1
   :stop        ["." "!" "?" ".\""]})

(defn- request [engine task params]
  {:uri    (str "https://api.openai.com/v1/engines/" engine "/" task)
   :params (merge param-defaults params)})

(defn completion-with
  {:style/indent 1}
  [engine params]
  {:pre [(valid-engines engine)
         (set/subset? (set (keys params)) valid-params)]}
  (request (name engine) "completions" params))

(completion-with :davinci
  {:prompt "foo bar baz"})

(defn ->title-template [story]
  (str "Make a title for the following text:\n\"\"\"\n"
       story
       "\n\"\"\"\n"
       "The title of this piece is:"
       "\"\"\""))
