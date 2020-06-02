(ns multiverse.server
  (:gen-class)
  (:require [multiverse.ml :as ml]
            [multiverse.util :as util]
            [bidi.ring :as bidi]
            [clojure.string :as str]
            [muuntaja.core :as muuntaja]
            [org.httpkit.server :refer [run-server]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.flash :refer [wrap-flash]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.logger :as logger]
            [ring.util.response :as response]))

(set! *warn-on-reflection* 1)

;; Routes

(defn- transit+json-response [data]
  (-> data
      util/->transit+json
      response/response
      (response/header "Content-Type" "application/transit+json")))

(defn- home-page [_]
  (response/file-response "index.html" {:root "resources/public"}))

(defn- summary [{:keys [body]}]
  (let [text (muuntaja/decode "application/transit+json" body)
        summary (ml/summarize text)]
    (transit+json-response summary)))

(defn- ner [{:keys [body]}]
  (let [text (muuntaja/decode "application/transit+json" body)
        ner (ml/named-entity-recognition text)]
    (transit+json-response ner)))

(defn- sentences [{:keys [body]}]
  (let [prompt (muuntaja/decode "application/transit+json" body)
        sentences (ml/generate-sentences prompt 3)]
    (transit+json-response sentences)))

(defn- title [{:keys [body]}]
  (let [story (muuntaja/decode "application/transit+json" body)
        title (ml/generate-title story)]
    (transit+json-response title)))

(def route-handler
  (bidi/make-handler
   ["/" {"" home-page
         "generate/" {"ner" ner
                      "sentences" sentences
                      "summary" summary
                      "title" title}}]))

;; Ring

(defn- wrap-body-string [handler]
  (fn [request]
    (handler (if (:body request)
               (assoc request :body (->> request :body .bytes slurp))
               request))))
(def app
  (-> #'route-handler
      logger/wrap-log-response
      (wrap-cors :access-control-allow-origin  [#"http://localhost:8021"]
                 :access-control-allow-methods [:post :get])
      (wrap-resource "public")
      wrap-not-modified
      wrap-body-string
      wrap-session
      wrap-params
      wrap-reload
      wrap-flash
      logger/wrap-log-request-start))

(defn -main []
  (run-server app {:port 3333}))
