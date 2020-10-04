(ns multiverse.server
  (:gen-class)
  (:require [bidi.ring :as bidi]
            [multiverse.ml :as ml]
            [multiverse.util :as util]
            [muuntaja.core :as muuntaja]
            [org.httpkit.server :refer [run-server]]
            [ring.logger :as logger]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.flash :refer [wrap-flash]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.session :refer [wrap-session]]
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

(defn- ner [{:keys [body]}]
  (-> (muuntaja/decode "application/transit+json" body)
      (ml/named-entity-recognition)
      (transit+json-response)))

(defn- title [{:keys [body]}]
  (-> (muuntaja/decode "application/transit+json" body)
      (ml/generate-title)
      (transit+json-response)))

(defn- sentences [{:keys [body]}]
  (-> (muuntaja/decode "application/transit+json" body)
      (ml/generate-sentences :GPT-2)
      (transit+json-response)))

(def route-handler
  (bidi/make-handler
   ["/" {"" home-page
         "generate/" {"ner" ner
                      "sentences" sentences
                      "title" title}}]))

;; Ring

(defn- wrap-body-string [handler]
  (fn [request]
    (handler
     (if (:body request)
       (assoc request :body (->> request :body .bytes slurp))
       #_(update request :body (comp slurp .bytes)) ;; TODO does this work?
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
