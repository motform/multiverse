(ns org.motform.multiverse.open-ai
  (:require [clojure.set :as set]
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
   :top_p       1
   :stop        [".\"." "." "!" "?"]})

(defn- request [engine task params]
  {:uri    (str "https://api.openai.com/v1/engines/" engine "/" task)
   :params (merge param-defaults params)})

(defn completion-with
  {:style/indent 1}
  [engine params]
  {:pre [(valid-engines engine)
         (set/subset? (set (keys params)) valid-params)]}
  (request (name engine) "completions" params))

(defn ->title-template [story]
  (str "I was asked to give a title to this story:\n\"\"\"\n\n" story "\n\"\"\"\nThe title i came up with was:\n\"\"\""))

(defn format-prompt
  "Returns space-delimited str from a vec of `sentences`"
  [sentences]
  (let [personality (get @(rf/subscribe [:personalities]) @(rf/subscribe [:active-personality]))]
    (->> sentences (map :text) (interpose " ") (apply str))))
