(ns org.motform.multiverse.events
  (:require [ajax.core :as ajax]
            [nano-id.core :refer [nano-id]]
            [re-frame.core :as rf :refer [reg-event-db reg-event-fx reg-fx inject-cofx after]]
            [org.motform.multiverse.db :as db]
            [org.motform.multiverse.open-ai :as open-ai]
            [org.motform.multiverse.routes :as routes]
            [org.motform.multiverse.util :as util]))

;;; Interceptors

(def ->local-storage (after db/collections->local-storage))
(def local-storage-interceptor [->local-storage])

;;; DB

(reg-event-fx
 :db/initialize
 [(inject-cofx :local-store-collections)]
 (fn [{:keys [local-store-collections]} [_ default-db]]
   {:db (merge default-db (if local-store-collections local-store-collections {}))}))

;;; Story

(reg-event-fx
 :page/active
 (fn [{:keys [db]} [_ page]]
   {:db (assoc-in db [:db/state :page/active] page)
    :dispatch [:page/title page]}))

(reg-fx
 :title
 (fn [name]
   (let [separator (when name " | ")
         title (str "Multiverse" separator name)]
     (set! (.-title js/document) title))))

(reg-event-fx
 :page/title
 (fn [_ [_ page]]
   (let [page-name (routes/titles page)]
     {:title page-name})))

(reg-event-db
 :story/active
 (fn [db [_ story]]
   (assoc-in db [:db/state :story/active] story)))

(reg-event-db
 :sentence/active
 (fn [db [_ id]]
   (let [story (get-in db [:db/state :story/active])]
     (assoc-in db [:db/stories story :story/meta :sentence/active] id))))

(reg-event-db
 :sentence/highlight
 (fn [db [_ sentence source]]
   (assoc-in db [:db/state :sentence/highlight] {:id sentence :source source}))) ; TODO

(reg-event-db
 :sentence/remove-highlight
 (fn [db _]
   (assoc-in db [:db/state :sentence/highlight] nil)))

(reg-event-db
 :sentence/preview
 (fn [db [_ sentence]]
   (let [story (get-in db [:db/state :story/active])
         active-sentence (get-in db [:db/stories story :story/meta :sentence/active])]
     (-> db
         (assoc-in [:db/state :sentence/preview] active-sentence)
         (assoc-in [:db/stories story :story/meta :sentence/active] sentence)))))

(reg-event-db
 :open-ai/update-api-key
 (fn [db [_ api-key]]
   (assoc-in db [:db/state :open-ai/key :open-ai/api-key] api-key)))

;;; Personalites

(reg-event-db
 :personality/active
 (fn [db [_ personality-id]]
   (assoc-in db [:db/state :personality/active] personality-id)))

;;; New-story

(defn ->sentence [id text path children]
  #:sentence
  {:id   id
   :text text
   :path path
   :children children
   :personality :personality/neutral})

(defn ->story [story-id sentence-id prompt]
  {:story/meta {:story/id story-id
                :story/title    ""
                :story/updated (js/Date.)
                :sentence/active sentence-id}
   :story/sentences {sentence-id (->sentence sentence-id prompt [sentence-id] [])}})

(reg-event-fx
 :new-story/submit
 (fn [{:keys [db]} _]
   (let [prompt (get-in db [:db/state :new-story/prompt])]
     {:db (assoc-in db [:db/state :new-story/prompt] "")
      :dispatch [:story/new prompt]})))

(reg-event-fx
 :story/new
 [local-storage-interceptor]
 (fn [{:keys [db]} [_ prompt]]
   (let [story-id    (nano-id 10)
         sentence-id (nano-id 10)]
     {:db (-> db
              (assoc-in [:db/stories story-id] (->story story-id sentence-id prompt))
              (assoc-in [:db/state :story/active] story-id))
      :dispatch [:open-ai/title]})))

(reg-event-db
 :new-story/update-prompt
 (fn [db [_ prompt]]
   (assoc-in db [:db/state :new-story/prompt] prompt)))

;;; Library

(reg-event-db
 :library/clear
 [local-storage-interceptor]
 (fn [db _]
   (assoc db :db/stories {})))

;;; OpenAI

(defn ->children
  "Make children map to be merged into sentences."
  [parent-path child-ids texts]
  (let [child-pairs (util/pairs child-ids texts)]
    (reduce (fn [children [id text]]
              (assoc children id (->sentence id text (conj parent-path id) [])))
            {} child-pairs)))

(defn- open-ai-texts [completions]
  (map (comp #(str % \.) :text) (:choices completions)))

(reg-event-fx
 :open-ai/handle-children
 [local-storage-interceptor]
 (fn [{:keys [db]} [_ story parent completions]]
   (let [parent-path (get-in db [:db/stories story :story/sentences parent :sentence/path])
         child-ids (repeatedly 3 #(nano-id 10))
         children (->children parent-path child-ids (open-ai-texts completions))]
     {:db (-> db
              (update-in [:db/stories story :story/sentences] merge children)
              (assoc-in  [:db/stories story :story/sentences parent :sentence/children] child-ids)
              (assoc-in  [:db/stories story :story/meta :story/updated] (js/Date.))
              (assoc-in  [:db/state :open-ai/pending-request?] false))})))

(reg-event-db
 :open-ai/handle-title
 [local-storage-interceptor]
 (fn [db [_ story-id title]]
   (-> db
       (assoc-in [:db/stories story-id :story/meta :story/title]
                 (-> title :choices first :text)))))


(reg-event-db
 :open-ai/handle-validate-api-key
 [local-storage-interceptor]
 (fn [db _]
   (-> db
       (assoc-in [:db/state :open-ai/key :open-ai/validated?] true)
       (assoc-in [:db/state :open-ai/pending-request?] false)))) 

(reg-event-fx
 :open-ai/validate-api-key
 (fn [{:keys [db]} _]
   (let [api-key (get-in db [:db/state :open-ai/key :open-ai/api-key])]
     {:db (assoc-in db [:db/state :open-ai/pending-request?] true)
      :http-xhrio {:method          :get
                   :uri             "https://api.openai.com/v1/engines"
                   :headers         {"Authorization" (str "Bearer " api-key)}
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:open-ai/handle-validate-api-key]
                   :on-failure      [:failure-http]}})))

(reg-event-fx
 :open-ai/completions
 (fn [{:keys [db]} [_ parent-id prompt]]
   (let [story-id  (get-in db [:db/state :story/active])
         api-key   (get-in db [:db/state :open-ai/key :open-ai/api-key])
         {:keys [uri params]} (open-ai/completion-with :ada #_:text-davinci-001 
                                                       {:prompt prompt})]
     {:db (assoc-in db [:db/state :open-ai/pending-request?] true)
      :http-xhrio {:method  :post
                   :uri     uri 
                   :headers {"Authorization" (str "Bearer " api-key)}
                   :params  params
                   :format  (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [:open-ai/handle-children story-id parent-id] ; TODO
                   :on-failure [:open-ai/failure]}})))

(reg-event-fx
 :open-ai/title
 (fn [{:keys [db]} _]
   (let [api-key   (get-in db [:db/state :open-ai/key :open-ai/api-key])
         story-id  (get-in db [:db/state :story/active])
         sentences (->> (get-in db [:db/stories story-id :story/sentences]) vals open-ai/format-prompt)
         {:keys [uri params]} (open-ai/completion-with :text-davinci-001
                                                       {:prompt (open-ai/format-title sentences)
                                                        :n           1
                                                        :max_tokens  15})]
     {:http-xhrio {:method          :post
                   :uri             uri 
                   :headers         {"Authorization" (str "Bearer " api-key)}
                   :params          params
                   :format          (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:open-ai/handle-title story-id]
                   :on-failure      [:open-ai/failure]}})))

(reg-event-db
 :open-ai/failure
 (fn [db [_ result]]
   (assoc-in db [:db/state :open-ai/failure] result)))
