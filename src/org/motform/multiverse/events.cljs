(ns org.motform.multiverse.events
  (:require
    [ajax.core :as ajax]
    [clojure.string :as str]
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

(reg-event-fx :db/initialize
  [(inject-cofx :local-store-collections)]
  (fn [{:keys [local-store-collections]} [_ default-db]]
    {:db (merge default-db (if local-store-collections local-store-collections {}))}))

;;; Story

(reg-event-fx :page/active
  (fn [{:keys [db]} [_ page]]
    {:db (assoc-in db [:db/state :page/active] page)
     :dispatch [:page/title page]}))

(reg-fx :title
  (fn [name]
    (let [separator (when name " | ")
          title (str "Multiverse" separator name)]
      (set! (.-title js/document) title))))

(reg-event-fx :page/title
  (fn [_ [_ page]]
    (let [page-name (routes/titles page)]
      {:title page-name})))

(reg-event-db :story/active
  (fn [db [_ story-id]]
    (-> db
      (assoc-in  [:db/state :story/active] story-id)
      (update-in [:db/state :story/recent] conj story-id))))

(reg-event-db :sentence/active
  (fn [db [_ id]]
    (let [story (get-in db [:db/state :story/active])]
      (assoc-in db [:db/stories story :story/meta :sentence/active] id))))

(reg-event-db :sentence/highlight
  (fn [db [_ sentence source]]
    (assoc-in db [:db/state :sentence/highlight] {:id sentence :source source})))

(reg-event-db :sentence/remove-highlight
  (fn [db _]
    (assoc-in db [:db/state :sentence/highlight] nil)))

(reg-event-db :sentence/preview
  (fn [db [_ sentence]]
    (let [story (get-in db [:db/state :story/active])
          active-sentence (get-in db [:db/stories story :story/meta :sentence/active])]
      (-> db
        (assoc-in [:db/state :sentence/preview] active-sentence)
        (assoc-in [:db/stories story :story/meta :sentence/active] sentence)))))

(reg-event-db :open-ai/update-api-key
  (fn [db [_ api-key]]
    (assoc-in db [:db/state :open-ai/key :open-ai/api-key] (str/trim api-key))))

;;; Tabs

(reg-event-db :tab/highlight
  (fn [db [_ story-id]]
    (assoc-in db [:db/state :tab/highlight] story-id)))

(reg-event-db :tab/remove-highlight
  (fn [db _]
    (assoc-in db [:db/state :tab/highlight] nil)))

;;; Personalites

(def personality-color #:personality
   {:neutral "#4e5558"
    :sci-fi  "#7e6a02"
    :fantasy "#01382f"
    :poetic  "#02557e"})

(reg-event-db :personality/active
  (fn [db [_ personality-id]]
    (println (.setAttribute (.querySelector js/document "meta[name=\"theme-color\"]") "content" (personality-color personality-id)))
    (assoc-in db [:db/state :personality/active] personality-id)))

;;; New-story

(defn ->sentence [id text path children personality] #:sentence
   {:id   id
    :text text
    :path path
    :children children
    :personality personality})

(defn ->story [story-id sentence-id prompt personality]
  {:story/meta {:story/id story-id
                :story/title ""
                :story/updated (js/Date.)
                :sentence/active sentence-id}
   :story/sentences {sentence-id (->sentence sentence-id prompt [sentence-id] [] personality)}})

(def prompt-templates
  #:template
   {:blank   ""
    :urban   "I was walking my tan Whippet down the Avenue of the Ameriacs, when suddenly, the ground begain to shake. We looked up in unison and where utterly shocked to see..."
    :musical "Lyrics: ♪ Bunnies aren’t just cute like everyone supposes. They got them hoppy legs and twitchy little noses, and what’s with all the carrots!? ♪"
    :news    "BREAKING NEWS: The world's largest pumpkin has turned sentient is a chocking turn of events. But while some have resorted to running amok on the streets, local farmers claim the incident as a \"Relatively commonplace fall missunderstanding\"."
    :ai      "The rapid progression of AI technolgies had worried Sam. They argued benevolence as never given, citing Assmiov's laws as thin veneer. That all changed once GLADOS came into the picture."})

(reg-event-db :new-story/template
  (fn [db [_ template]]
    (-> db
      (assoc-in [:db/state :new-story/template] template)
      (assoc-in [:db/state :new-story/prompt] (prompt-templates template)))))

(reg-event-fx :new-story/submit
  (fn [{:keys [db]} _]
    (let [prompt (get-in db [:db/state :new-story/prompt])]
      {:db (-> db
             (assoc-in [:db/state :new-story/prompt] "")
             (assoc-in [:db/state :new-story/template] :template/blank))
       :dispatch [:story/new prompt]})))

(reg-event-fx
  :story/new
  [local-storage-interceptor]
  (fn [{:keys [db]} [_ prompt]]
    (let [story-id    (nano-id 10)
          sentence-id (nano-id 10)
          active-personality (get-in db [:db/state :personality/active])]
      {:db (-> db
             (assoc-in  [:db/stories story-id] (->story story-id sentence-id prompt active-personality))
             (assoc-in  [:db/state :story/active] story-id)
             (assoc-in  [:db/state :sentence/active] sentence-id)
             (assoc-in  [:db/state :sentence/highlight] {:id sentence-id :source :page/new-story})
             (update-in [:db/state :story/recent] conj story-id))
       :dispatch [:open-ai/title]})))

(reg-event-db :new-story/update-prompt
  (fn [db [_ prompt]]
    (assoc-in db [:db/state :new-story/prompt] prompt)))

;;; Library

(reg-event-db :library/clear
  [local-storage-interceptor]
  (fn [db _]
    (assoc db :db/stories {})))

(reg-event-db :story/delete
  [local-storage-interceptor]
  (fn [db [_ story-id]]
    (-> db
      (update :db/stories dissoc story-id)
      (assoc-in [:db/state :story/active] nil))))

;;; OpenAI

(defn auth [api-key]
  {"Authorization" (str "Bearer " api-key)})

(defn ->children
  "Make children map to be merged into sentences."
  [parent-path child-ids texts active-personality]
  (let [child-pairs (util/pairs child-ids texts)]
    (reduce (fn [children [id text]]
              (assoc children id (->sentence id text (conj parent-path id) [] active-personality)))
      {} child-pairs)))

(defn- open-ai-texts [completion]
  (map #(-> % :message :content)
    (:choices completion)))

(reg-event-fx :open-ai/handle-children
  [local-storage-interceptor]
  (fn [{:keys [db]} [_ story-id parent-id completions]]
    (let [parent-path (get-in db [:db/stories story-id :story/sentences parent-id :sentence/path])
          child-ids (repeatedly 3 #(nano-id 10))
          children (->children
                     parent-path
                     child-ids
                     (open-ai-texts completions)
                     (get-in db [:db/state :personality/active]))]
      {:db (-> db
             (update-in [:db/stories story-id :story/sentences] merge children)
             (assoc-in  [:db/stories story-id :story/sentences parent-id :sentence/children] child-ids)
             (assoc-in  [:db/stories story-id :story/meta :story/updated] (js/Date.))
             (assoc-in  [:db/state :open-ai/pending-request?] false))})))

(reg-event-db :open-ai/handle-title
  [local-storage-interceptor]
  (fn [db [_ story-id title]]
    (db story-id title)
    (-> db
      (assoc-in [:db/stories story-id :story/meta :story/title]
        (-> title :choices first :message :content)))))

(reg-event-db :open-ai/handle-validate-api-key
  [local-storage-interceptor]
  (fn [db _]
    (-> db
      (assoc-in [:db/state :open-ai/key :open-ai/validated?] true)
      (assoc-in [:db/state :open-ai/pending-request?] false))))

(reg-event-fx :open-ai/validate-api-key
  (fn [{:keys [db]} _]
    (let [api-key (get-in db [:db/state :open-ai/key :open-ai/api-key])]
      {:db (assoc-in db [:db/state :open-ai/pending-request?] true)
       :http-xhrio
       {:method  :get
        ;; The key is "validated" by the endpoint, not the request.
        :uri     (open-ai/endpoint :models)
        :headers (auth api-key)
        :response-format (ajax/json-response-format {:keywords? true})
        :on-success [:open-ai/handle-validate-api-key]
        :on-failure [:failure-http]}})))

(reg-event-fx :open-ai/completions
  (fn [{:keys [db]} [_ parent-id prompt]]
    (let [{:keys [story-id api-key]} (util/completion-data db)
          params (open-ai/payload :gpt-4-1106-preview prompt)]
      {:db (assoc-in db [:db/state :open-ai/pending-request?] true)
       :http-xhrio
       {:method  :post
        :uri     (open-ai/endpoint :chat)
        :headers (auth api-key)
        :params  params
        :format  (ajax/json-request-format)
        :response-format (ajax/json-response-format {:keywords? true})
        :on-success [:open-ai/handle-children story-id parent-id]
        :on-failure [:open-ai/failure]}})))

(reg-event-fx :open-ai/title
  (fn [{:keys [db]} _]
    (let [{:keys [story-id api-key]} (util/completion-data db)
          story (vals (get-in db [:db/stories story-id :story/sentences]))
          params (open-ai/payload :gpt-4-1106-preview story)]
      {:http-xhrio {:method  :post
                    :uri     (open-ai/endpoint :chat)
                    :headers (auth api-key)
                    :params  params
                    :format  (ajax/json-request-format)
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success [:open-ai/handle-title story-id]
                    :on-failure [:open-ai/failure]}})))

(reg-event-db :open-ai/replace-children
  (fn [db [_ story-id parent-id unrealized-child-ids completions]]
    (let [original-children (util/children db parent-id story-id)
          realized-children (select-keys original-children
                              (->> original-children
                                vals
                                (map :sentence/id)
                                (remove unrealized-child-ids)))
          new-child-ids (repeatedly (count unrealized-child-ids)
                          #(nano-id 10))
          parent-path (get-in db [:db/stories story-id :story/sentences parent-id :sentence/path])
          new-children (->children parent-path
                         new-child-ids (open-ai-texts completions) (get-in db [:db/state :personality/active]))]
      (-> db
        (update-in [:db/stories story-id :story/sentences] #(apply dissoc % unrealized-child-ids))
        (update-in [:db/stories story-id :story/sentences] merge new-children)
        (assoc-in  [:db/stories story-id :story/sentences parent-id :sentence/children] (keys (merge realized-children new-children)))
        (assoc-in  [:db/stories story-id :story/meta :story/updated] (js/Date.))
        (assoc-in  [:db/state :open-ai/pending-request?] false)))))

(reg-event-fx :open-ai/replace-completions
  (fn [{:keys [db]} [_ new-personality]]
    (let [{:keys [story-id parent-id api-key]} (util/completion-data db)
          unrealized-children (->> (util/children db parent-id)
                                vals
                                (filter #(empty? (:sentence/children %))))
          n-unrealized-children (count unrealized-children)]
      (when-not (or (zero? n-unrealized-children)
                  (= new-personality
                    (-> unrealized-children first :sentence/personality)))
        (let [paragraph (util/paragraph db story-id parent-id)
              params (open-ai/payload :gpt-4-1106-preview paragraph)]
          {:db (-> db
                 (assoc-in [:db/state :personality/active] new-personality)
                 (assoc-in [:db/state :open-ai/pending-request?] true))
           :http-xhrio
           {:method  :post
            :uri     (open-ai/endpoint :chat)
            :headers (auth api-key)
            :params  params
            :format  (ajax/json-request-format)
            :response-format (ajax/json-response-format {:keywords? true})
            :on-success [:open-ai/replace-children story-id parent-id (set (map :sentence/id unrealized-children))]
            :on-failure [:open-ai/failure]}})))))

(reg-event-db :open-ai/failure
  (fn [db [_ result]]
    (assoc-in db [:db/state :open-ai/failure] result)))
