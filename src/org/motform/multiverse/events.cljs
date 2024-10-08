(ns org.motform.multiverse.events
  (:require
    [ajax.core :as ajax]
    [clojure.string :as str]
    [nano-id.core :refer [nano-id]]
    [org.motform.multiverse.db :as db]
    [org.motform.multiverse.open-ai :as open-ai]
    [org.motform.multiverse.prompts :refer [prompts]]
    [org.motform.multiverse.routes :as routes]
    [org.motform.multiverse.story :as story]
    [re-frame.core :as rf :refer [reg-event-db reg-event-fx reg-fx inject-cofx after]]))

;; Interceptors

(def ->local-storage (after db/collections->local-storage))
(def local-storage-interceptor [->local-storage])

;; DB

(reg-event-fx
  :db/initialize
  [(inject-cofx :local-store-collections)]
  (fn [{:keys [local-store-collections]} [_ default-db]]
    {:db (merge default-db (if local-store-collections local-store-collections {}))}))

;; Story

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
  (fn [db [_ story-id]]
    (-> db
        (assoc-in  [:db/state :story/active] story-id)
        (update-in [:db/state :story/recent] conj story-id))))

(reg-event-db
  :sentence/active
  (fn [db [_ id]]
    (let [story (get-in db [:db/state :story/active])]
      (assoc-in db [:db/stories story :story/meta :sentence/active] id))))

(reg-event-db
  :sentence/highlight
  (fn [db [_ sentence source]]
    (assoc-in db [:db/state :sentence/highlight] {:id sentence :source source})))

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
    (assoc-in db [:db/state :open-ai/key :open-ai/api-key] (str/trim api-key))))

;; Tabs

(reg-event-db
  :tab/highlight
  (fn [db [_ story-id]]
    (assoc-in db [:db/state :tab/highlight] story-id)))

(reg-event-db
  :tab/remove-highlight
  (fn [db _]
    (assoc-in db [:db/state :tab/highlight] nil)))

(reg-event-db
  :new-story/model
  (fn [db [_ model]]
    (assoc-in db [:db/state :new-story/model] model)))

(reg-event-db
  :new-story/prompt-version
  (fn [db [_ version]]
    (-> db
        (assoc-in [:db/state :new-story/prompt-version] version)
        (assoc-in [:db/state :new-story/system-message] (get-in prompts [version :system] ""))
        (assoc-in [:db/state :new-story/user-message]   (get-in prompts [version :user]   "")))))

(def key-of-new-story?
  #{:new-story/prompt :new-story/system-message :new-story/user-message :new-story/title})

(reg-event-db
  :new-story/update
  (fn [db [_ k v]]
    {:pre [(key-of-new-story? k) (string? v)]}
    (assoc-in db [:db/state k] v)))

(reg-event-fx
  :new-story/submit
  (fn [{:keys [db]} _]
    (let [prompt (get-in db [:db/state :new-story/prompt])
          title  (get-in db [:db/state :new-story/title])
          model  (get-in db [:db/state :new-story/model])
          version (get-in db [:db/state :new-story/prompt-version])
          system-message (get-in db [:db/state :new-story/system-message])
          user-message (get-in db [:db/state :new-story/user-message])]
      {:db (-> db
               (assoc-in [:db/state :new-story/prompt] "")
               (assoc-in [:db/state :new-story/title] ""))
       :dispatch [:story/new prompt title model version system-message user-message]})))

(reg-event-fx
  :story/new
  [local-storage-interceptor]
  (fn [{:keys [db]} [_ prompt title model version system-message user-message]]
    (let [story-id    (nano-id 10)
          sentence-id (nano-id 10)
          story       (story/->story prompt
                                     :id story-id
                                     :title title
                                     :sentence-id sentence-id
                                     :model model
                                     :version version
                                     :system-message system-message
                                     :user-message user-message)]
      {:db (-> db
               (assoc-in  [:db/stories story-id] story)
               (assoc-in  [:db/state :story/active] story-id)
               (assoc-in  [:db/state :sentence/active] sentence-id)
               (assoc-in  [:db/state :sentence/highlight] {:id sentence-id :source :page/new-story})
               (update-in [:db/state :story/recent] conj story-id))})))

;; Library

(reg-event-db
  :library/clear
  [local-storage-interceptor]
  (fn [db _]
    (assoc db :db/stories {})))

(reg-event-db
  :story/delete
  [local-storage-interceptor]
  (fn [db [_ story-id]]
    (-> db
        (update :db/stories dissoc story-id)
        (assoc-in [:db/state :story/active] nil))))

;; OpenAI

(reg-event-fx
  :open-ai/handle-children
  [local-storage-interceptor]
  (fn [{:keys [db]} [_ story-id parent-id completions]]
    (let [parent-path (get-in db [:db/stories story-id :story/sentences parent-id :sentence/path])
          child-ids (repeatedly 3 #(nano-id 10))
          children (story/->children
                     (open-ai/completion-texts completions)
                     :child-ids child-ids
                     :parent-path parent-path)]
      {:db (-> db
               (update-in [:db/stories story-id :story/sentences] merge children)
               (assoc-in  [:db/stories story-id :story/sentences parent-id :sentence/children] child-ids)
               (assoc-in  [:db/stories story-id :story/meta :story/updated] (js/Date.))
               (assoc-in  [:db/state :open-ai/pending-request?] false))})))

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
       :http-xhrio
       {:method  :get
        ;; The key is "validated" by the endpoint, not the request.
        :uri     (open-ai/endpoint :models)
        :headers (open-ai/auth api-key)
        :response-format (ajax/json-response-format {:keywords? true})
        :on-success [:open-ai/handle-validate-api-key]
        :on-failure [:failure-http]}})))

(reg-event-fx
  :open-ai/completions
  (fn [{:keys [db]} [_ parent-id prompt]]
    (let [{:keys [story-id api-key model system-message user-message]} (db/request-data db)
          params (open-ai/request-next-sentence
                   :model model
                   :paragraphs prompt
                   :system-message system-message
                   :user-message user-message)]
      {:db (assoc-in db [:db/state :open-ai/pending-request?] true)
       :http-xhrio
       {:method  :post
        :uri     (open-ai/endpoint :chat)
        :headers (open-ai/auth api-key)
        :params  params
        :format  (ajax/json-request-format)
        :response-format (ajax/json-response-format {:keywords? true})
        :on-success [:open-ai/handle-children story-id parent-id]
        :on-failure [:open-ai/failure]}})))

(reg-event-db
  :open-ai/failure
  (fn [db [_ result]]
    (assoc-in db [:db/state :open-ai/failure] result)))
