(ns neural.multiverse.events
  (:require [ajax.core :as ajax]
            [clojure.spec.alpha :as s]
            [nano-id.core :refer [nano-id]]
            [neural.multiverse.db :as db]
            [neural.util :as util]
            [re-frame.core :as rf :refer [reg-event-db reg-event-fx inject-cofx path after debug]]))

(defn ->node [id text path children]
  {:id id :text text :path path :children children})

(defn ->story [story-id sentence-id {:keys [text author model]}]
  {:meta {:id story-id
          :author author
          :title ""
          :model model}
   :sentences {sentence-id (->node sentence-id text [sentence-id] [])}})

;;; Interceptors

;; TODO enable interceptor
(defn- check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`.

  SOURCE: re-frame docs."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))

(def check-spec-interceptor (after (partial check-and-throw :neural.multiverse.db/db)))
(def spec-interceptor [#_check-spec-interceptor])

;;; State

(reg-event-db
 :reset
 [spec-interceptor]
 (fn [_ _]
   db/default-db))

(reg-event-db
 :initialize-db
 [spec-interceptor]
 (fn [db [_ default-db]]
   (merge db default-db)))

(reg-event-db
 :active-page
 [spec-interceptor]
 (fn [db [_ page]]
   (assoc-in db [:state :active-page] page)))

(reg-event-db
 :active-story
 [spec-interceptor]
 (fn [db [_ story]]
   (assoc-in db [:state :active-story] story)))

(reg-event-db
 :active-sentence
 [spec-interceptor]
 (fn [db [_ id]]
   (assoc-in db [:state :active-sentence] id)))

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
   (-> db
       (assoc-in [:state :preview] sentence)
       (assoc-in [:state :active-sentence] sentence))))

(reg-event-db
 :preview-sentence
 [spec-interceptor]
 (fn [db [_ sentence]]
   (let [active-sentence (get-in db [:state :active-sentence])]
     (-> db
         (assoc-in [:state :preview] active-sentence)
         (assoc-in [:state :active-sentence] sentence)))))

(reg-event-db
 :remove-preview
 [spec-interceptor]
 (fn [db _]
   (let [real-active-sentence (get-in db [:state :preview])]
     (-> db
         (assoc-in [:state :active-sentence] real-active-sentence)
         (assoc-in [:state :preview] nil)))))

;;; Prompt

(reg-event-db
 :submit-new-story
 (fn [db _]
   (let [input (get-in db [:state :new-story])]
     (rf/dispatch [:story input])
     (-> db
         (assoc-in [:state :active-page] :story)
         (assoc-in [:state :new-story :text] "")))))

(reg-event-db
 :story
 [spec-interceptor]
 (fn [db [_ input]]
   (let [story-id (nano-id 10)
         sentence-id (nano-id 10)]
     (rf/dispatch [:request-title])
     (-> db
         (assoc-in [:stories story-id] (->story story-id sentence-id input))
         (assoc-in [:state :active-sentence] sentence-id)
         (assoc-in [:state :active-story] story-id)))))

(reg-event-db
 :prompt-text
 (fn [db [_ text]]
   (assoc-in db [:state :new-story :text] text)))

(reg-event-db
 :prompt-author
 (fn [db [_ author]]
   (assoc-in db [:state :new-story :author] author)))

(reg-event-db
 :prompt-model
 (fn [db [_ model]]
   (assoc-in db [:state :new-story :model] model)))

;;; Story

(reg-event-db
 :sentence
 (fn [db [_ id]]
   (let [story (get-in db [:state :active-sentence])
         path (get-in db [:stories story :sentences id :path])]
     ;; we do a `reduce` as opposed to a `select-keys` in order to retain order
     (reduce (fn [sentences id]
               (conj sentences (get-in db [:stories story :sentences id :text])))
             [] path))))

(defn ->children
  "Make children map to be merged into sentences."
  [parent-path child-ids texts]
  (let [child-pairs (util/pairs child-ids texts)]
    (reduce (fn [children [id text]]
              (assoc children id (->node id text (conj parent-path id) [])))
            {} child-pairs)))

;; A slightly more imperative function than I would have liked, but
;; it is important that the appending of children is an atomic translation
(reg-event-db
 :handle-children
 [spec-interceptor]
 (fn [db [_ parent texts]]
   (let [story (get-in db [:state :active-story])
         parent-path (get-in db [:stories story :sentences parent :path])
         child-ids (repeatedly 3 #(nano-id 10))
         children (->children parent-path child-ids texts)]
     (rf/dispatch [:request-title])
     (-> db
         (update-in [:stories story :sentences] merge children)
         (assoc-in [:stories story :sentences parent :children] child-ids)
         (assoc-in [:state :pending-request?] false)))))

(reg-event-db
 :handle-title
 [spec-interceptor]
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
