(ns multiverse.events
  (:require [ajax.core :as ajax]
            [clojure.spec.alpha :as s]
            [multiverse.db :as db]
            [multiverse.routes :as routes]
            [multiverse.util :as util]
            [nano-id.core :refer [nano-id]]
            [re-frame.core :as rf :refer [reg-event-db reg-event-fx reg-fx inject-cofx path after debug]]))

;; TODO replace localhost


;;; Interceptors

;; TODO enable interceptor
(defn- check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`.

  SOURCE: re-frame docs."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))

(def check-spec-interceptor (after (partial check-and-throw :multiverse.db/db)))
(def spec-interceptor [check-spec-interceptor])

(def ->local-storage (after db/collections->local-storage))
(def local-storage-interceptor [->local-storage])

;;; State

(reg-event-fx
 :initialize-db
 [(inject-cofx :local-store-collections) spec-interceptor]
 (fn [{:keys [local-store-collections]} [_ default-db]]
   {:db (util/?assoc default-db :stories local-store-collections)}))

(reg-event-fx
 :active-page
 [spec-interceptor]
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
 [spec-interceptor]
 (fn [db [_ story]]
   (assoc-in db [:state :active-story] story)))

(reg-event-db
 :active-sentence
 [spec-interceptor]
 (fn [db [_ id]]
   (let [story (get-in db [:state :active-story])]
     (assoc-in db [:stories story :meta :active-sentence] id))))

(reg-event-db
 :highlight-sentence
 [spec-interceptor]
 (fn [db [_ sentence]]
   (assoc-in db [:state :highlight] sentence)))

(reg-event-db
 :remove-highlight
 [spec-interceptor]
 (fn [db _]
   (assoc-in db [:state :highlight] nil)))

(reg-event-db
 :select-sentence
 [spec-interceptor]
 (fn [db [_ sentence]]
   (let [story (get-in db [:state :active-story])]
     (-> db
         (assoc-in [:state :preview] sentence)
         (assoc-in [:stories story :meta :active-sentence] sentence)))))

(reg-event-db
 :preview-sentence
 [spec-interceptor]
 (fn [db [_ sentence]]
   (let [story (get-in db [:state :active-story])
         active-sentence (get-in db [:stories story :meta :active-sentence])]
     (-> db
         (assoc-in [:state :preview] active-sentence)
         (assoc-in [:stories story :meta :active-sentence] sentence)))))

(reg-event-db
 :remove-preview
 [spec-interceptor]
 (fn [db _]
   (let [real-active-sentence (get-in db [:state :preview])
         story (get-in db [:state :active-story])]
     (-> db
         (assoc-in [:stories story :meta :active-sentence] real-active-sentence)
         (assoc-in [:state :preview] nil)))))

(reg-event-db
 :change-model
 [spec-interceptor]
 (fn [db [_ model]]
   (let [story (get-in db [:state :active-story])]
     (-> db
         (update-in [:stories story :meta :authors] conj model)
         (assoc-in [:stories story :meta :model] model)))))

;;; Prompt

(defn ->node [id text path children]
  {:id id :text text :path path :children children})

(defn ->story [story-id sentence-id {:keys [text author model]}]
  {:meta {:id story-id
          :active-sentence sentence-id
          :authors #{author model}
          :title ""
          :model model
          :updated (js/Date.)}
   :sentences {sentence-id (->node sentence-id text [sentence-id] [])}})

(reg-event-fx
 :submit-new-story
 (fn [{:keys [db]} _]
   (let [input (get-in db [:state :new-story])]
     {:db (-> db
              (assoc-in [:state :active-page] :story)
              (assoc-in [:state :new-story :text] ""))
      :dispatch [:story input]})))

(reg-event-fx
 :story
 [spec-interceptor local-storage-interceptor]
 (fn [{:keys [db]} [_ input]]
   (let [story-id (nano-id 10)
         sentence-id (nano-id 10)]
     {:db (-> db
              (assoc-in [:stories story-id] (->story story-id sentence-id input))
              (assoc-in [:state :active-story] story-id))
      :dispatch [:request-title]})))

(reg-event-db
 :prompt
 (fn [db [_ k v]]
   (assoc-in db [:state :new-story k] v)))

(reg-event-db
 :dissoc-story
 [spec-interceptor local-storage-interceptor]
 (fn [db [_ id]]
   (update-in db [:stories] dissoc id)))

;;; Library

(reg-event-db
 :clear-library
 [spec-interceptor local-storage-interceptor]
 (fn [db _]
   (assoc db :stories {})))

(reg-event-db
 :library-sort
 [spec-interceptor]
 (fn [db [_ sorting]]
   (assoc-in db [:state :sorting] sorting)))

;;; Story

(defn ->children
  "Make children map to be merged into sentences."
  [parent-path child-ids texts]
  (let [child-pairs (util/pairs child-ids texts)]
    (reduce (fn [children [id text]]
              (assoc children id (->node id text (conj parent-path id) [])))
            {} child-pairs)))

(reg-event-fx
 :handle-children
 [spec-interceptor local-storage-interceptor]
 (fn [{:keys [db]} [_ parent texts]]
   (let [story (get-in db [:state :active-story])
         parent-path (get-in db [:stories story :sentences parent :path])
         child-ids (repeatedly 3 #(nano-id 10))
         children (->children parent-path child-ids texts)]
     {:db (-> db
              (update-in [:stories story :sentences] merge children)
              (assoc-in [:stories story :sentences parent :children] child-ids)
              (assoc-in [:stories story :meta :updated] (js/Date.))
              (assoc-in [:state :pending-request?] false))
      :dispatch [:request-title]})))

(reg-event-db
 :handle-title
 [spec-interceptor local-storage-interceptor]
 (fn [db [_ title]]
   (let [story (get-in db [:state :active-story])]
     (assoc-in db [:stories story :meta :title] title))))

(reg-event-fx
 :request-children
 (fn [{:keys [db]} [_ parent prompt]]
   {:db (assoc-in db [:state :pending-request?] true)
    :http-xhrio {:method :post
                 :uri "http://localhost:3333/generate/sentences"
                 :timeout 800000
                 :body (util/->transit+json prompt)
                 :format (ajax/transit-request-format)
                 :response-format (ajax/transit-response-format {:keywords? true})
                 :on-success [:handle-children parent]
                 :on-failure [:failure-http]}}))

(reg-event-fx
 :request-title
 (fn [{:keys [db]} _]
   (let [story (get-in db [:state :active-story])
         sentences (->> (get-in db [:stories story :sentences]) vals util/format-story)]
     {:http-xhrio {:method :post
                   :uri "http://localhost:3333/generate/title"
                   :timeout 800000
                   :body (util/->transit+json sentences)
                   :format (ajax/transit-request-format)
                   :response-format (ajax/transit-response-format {:keywords? true})
                   :on-success [:handle-title]
                   :on-failure [:failure-http]}})))

(reg-event-db
 :failure-http
 (fn [db [_ result]]
   (assoc-in db [:state :failure-http] result)))
