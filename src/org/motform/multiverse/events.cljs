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
 :initialize-db
 [(inject-cofx :local-store-collections)]
 (fn [{:keys [local-store-collections]} [_ default-db]]
   {:db (merge default-db (if local-store-collections local-store-collections {}))}))

(reg-event-fx
 :active-page
 (fn [{:keys [db]} [_ page]]
   {:db (assoc-in db [:state :active-page] page)
    :dispatch [:page-title page]}))

(reg-fx
 :title
 (fn [name]
   (let [separator (when name " | ")
         title (str "Multiverse" separator name)]
     (set! (.-title js/document) title))))

(reg-event-fx
 :page-title
 (fn [_ [_ page]]
   (let [page-name (routes/titles page)]
     {:title page-name})))

(reg-event-fx
 :page-title-story
 (fn [_ [_ title]]
   {:title title}))

(reg-event-db
 :active-story
 (fn [db [_ story]]
   (assoc-in db [:state :active-story] story)))

(reg-event-db
 :active-sentence
 (fn [db [_ id]]
   (let [story (get-in db [:state :active-story])]
     (assoc-in db [:stories story :meta :active-sentence] id))))

(reg-event-db
 :highlight-sentence
 (fn [db [_ sentence]]
   (assoc-in db [:state :highlight] sentence)))

(reg-event-db
 :remove-highlight
 (fn [db _]
   (assoc-in db [:state :highlight] nil)))

(reg-event-db
 :select-sentence
 (fn [db [_ sentence]]
   (let [story (get-in db [:state :active-story])]
     (-> db
         (assoc-in [:state :preview] sentence)
         (assoc-in [:stories story :meta :active-sentence] sentence)))))

(reg-event-db
 :preview-sentence
 (fn [db [_ sentence]]
   (let [story (get-in db [:state :active-story])
         active-sentence (get-in db [:stories story :meta :active-sentence])]
     (-> db
         (assoc-in [:state :preview] active-sentence)
         (assoc-in [:stories story :meta :active-sentence] sentence)))))

(reg-event-db
 :remove-preview
 (fn [db _]
   (let [real-active-sentence (get-in db [:state :preview])
         story (get-in db [:state :active-story])]
     (-> db
         (assoc-in [:stories story :meta :active-sentence] real-active-sentence)
         (assoc-in [:state :preview] nil)))))

;; TODO add this into localstorage
(reg-event-db
 :open-ai/update-api-key
 (fn [db [_ api-key]]
   (assoc-in db [:state :open-ai :api-key] api-key)))

;;; Prompt

(defn ->sentence [id text path children]
  {:id id :text text :path path :children children})

(defn ->story [story-id sentence-id prompt]
  {:meta {:id      story-id
          :title   ""
          :updated (js/Date.)
          :active-sentence sentence-id}
   :sentences {sentence-id (->sentence sentence-id prompt [sentence-id] [])}})

(reg-event-fx
 :submit-new-story
 (fn [{:keys [db]} _]
   (let [prompt (get-in db [:state :new-story])]
     {:db (assoc-in db [:state :new-story] "")
      :dispatch [:story prompt]})))

(reg-event-fx
 :story
 [local-storage-interceptor]
 (fn [{:keys [db]} [_ prompt]]
   (let [story-id    (nano-id 10)
         sentence-id (nano-id 10)]
     {:db (-> db
              (assoc-in [:stories story-id] (->story story-id sentence-id prompt))
              (assoc-in [:state :active-story] story-id))
      :dispatch [:open-ai/title]})))

(reg-event-db
 :prompt
 (fn [db [_ prompt]]
   (assoc-in db [:state :new-story] prompt)))

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
   (let [story-id  (get-in db [:state :active-story])
         api-key   (get-in db [:state :open-ai :api-key])
         {:keys [uri params]} (open-ai/completion-with :curie
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
   (let [api-key  (get-in db [:state :open-ai :api-key])
         story-id (get-in db [:state :active-story])
         sentences (->> (get-in db [:stories story-id :sentences]) vals util/format-story)
         {:keys [uri params]} (open-ai/completion-with :curie
                                {:prompt (open-ai/->title-template sentences)
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

