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

;;; State

(reg-event-fx
 :db/initialize
 [(inject-cofx :local-store-collections)]
 (fn [{:keys [local-store-collections]} [_ default-db]]
   {:db (merge default-db (if local-store-collections local-store-collections {}))}))

(reg-event-fx
 :page/active
 (fn [{:keys [db]} [_ page]]
   {:db (assoc-in db [:state :page/active] page)
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
   (assoc-in db [:state :story/active] story)))

(reg-event-db
 :sentence/active
 (fn [db [_ id]]
   (let [story (get-in db [:state :story/active])]
     (assoc-in db [:stories story :meta :sentence/active] id))))

(reg-event-db
 :sentence/highlight
 (fn [db [_ sentence source]]
   (assoc-in db [:state :sentence/highlight] {:id sentence :source source})))

(reg-event-db
 :sentence/remove-highlight
 (fn [db _]
   (assoc-in db [:state :sentence/highlight] nil)))

(reg-event-db
 :select-sentence
 (fn [db [_ sentence]]
   (let [story (get-in db [:state :story/active])]
     (-> db
         (assoc-in [:state :preview] sentence)
         (assoc-in [:stories story :meta :sentence/active] sentence)))))

(reg-event-db
 :preview-sentence
 (fn [db [_ sentence]]
   (let [story (get-in db [:state :story/active])
         active-sentence (get-in db [:stories story :meta :sentence/active])]
     (-> db
         (assoc-in [:state :preview] active-sentence)
         (assoc-in [:stories story :meta :sentence/active] sentence)))))

(reg-event-db
 :remove-preview
 (fn [db _]
   (let [real-active-sentence (get-in db [:state :preview])
         story (get-in db [:state :story/active])]
     (-> db
         (assoc-in [:stories story :meta :sentence/active] real-active-sentence)
         (assoc-in [:state :preview] nil)))))

(reg-event-db
 :open-ai/update-api-key
 (fn [db [_ api-key]]
   (assoc-in db [:state :open-ai :api-key] api-key)))

;;; Prompt

(defn ->sentence [id text path children]
  {:id id :text text :path path :children children :personality :personality/neutral})

(defn ->story [story-id sentence-id prompt]
  {:meta {:id      story-id
          :title   ""
          :updated (js/Date.)
          :sentence/active sentence-id}
   :sentences {sentence-id (->sentence sentence-id prompt [sentence-id] [])}})

(reg-event-fx
 :submit-new-story
 (fn [{:keys [db]} _]
   (let [prompt (get-in db [:state :new-story/prompt])]
     {:db (assoc-in db [:state :new-story/prompt] "")
      :dispatch [:story prompt]})))

(reg-event-fx
 :story
 [local-storage-interceptor]
 (fn [{:keys [db]} [_ prompt]]
   (let [story-id    (nano-id 10)
         sentence-id (nano-id 10)]
     {:db (-> db
              (assoc-in [:stories story-id] (->story story-id sentence-id prompt))
              (assoc-in [:state :story/active] story-id))
      :dispatch [:open-ai/title]})))

(reg-event-db
 :prompt
 (fn [db [_ prompt]]
   (assoc-in db [:state :new-story/prompt] prompt)))

(reg-event-db
 :dissoc-story
 [local-storage-interceptor]
 (fn [db [_ id]]
   (update db :stories dissoc id)))

;;; Library

(reg-event-db
 :clear-library
 [local-storage-interceptor]
 (fn [db _]
   (assoc db :stories {})))

;;; Story

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
 :handle-children
 [local-storage-interceptor]
 (fn [{:keys [db]} [_ story parent completions]]
   (let [parent-path (get-in db [:stories story :sentences parent :path])
         child-ids (repeatedly 3 #(nano-id 10))
         children (->children parent-path child-ids (open-ai-texts completions))]
     {:db (-> db
              (update-in [:stories story :sentences] merge children)
              (assoc-in  [:stories story :sentences parent :children] child-ids)
              (assoc-in  [:stories story :meta :updated] (js/Date.))
              (assoc-in  [:state :pending-request?] false))})))

(reg-event-db
 :handle-title
 [local-storage-interceptor]
 (fn [db [_ story-id title]]
   (-> db
       (assoc-in [:stories story-id :meta :title] (-> title :choices first :text)))))


(reg-event-db
 :open-ai/handle-validate-api-key
 [local-storage-interceptor]
 (fn [db _]
   (-> db
       (assoc-in [:state :open-ai :validated?] true) ; failed requests go to :failure-http
       (assoc-in [:state :pending-request?] false)))) 

(reg-event-fx
 :open-ai/validate-api-key
 (fn [{:keys [db]} _]
   (let [api-key (get-in db [:state :open-ai :api-key])]
     {:db (assoc-in db [:state :pending-request?] true)
      :http-xhrio {:method          :get
                   :uri             "https://api.openai.com/v1/engines"
                   :headers         {"Authorization" (str "Bearer " api-key)}
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:open-ai/handle-validate-api-key]
                   :on-failure      [:failure-http]}})))

(reg-event-fx
 :open-ai/completions
 (fn [{:keys [db]} [_ parent-id prompt]]
   (let [story-id  (get-in db [:state :story/active])
         api-key   (get-in db [:state :open-ai :api-key])
         {:keys [uri params]} (open-ai/completion-with :ada #_:text-davinci-001 
                                                       {:prompt prompt})]
     {:db (assoc-in db [:state :pending-request?] true)
      :http-xhrio {:method  :post
                   :uri     uri 
                   :headers {"Authorization" (str "Bearer " api-key)}
                   :params  params
                   :format  (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [:handle-children story-id parent-id] ; TODO
                   :on-failure [:failure-http]}})))

(reg-event-fx
 :open-ai/title
 (fn [{:keys [db]} _]
   (let [api-key   (get-in db [:state :open-ai :api-key])
         story-id  (get-in db [:state :story/active])
         sentences (->> (get-in db [:stories story-id :sentences]) vals open-ai/format-prompt)
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
                   :on-success      [:handle-title story-id]
                   :on-failure      [:failure-http]}})))

(reg-event-db
 :failure-http
 (fn [db [_ result]]
   (assoc-in db [:state :failure-http] result)))

