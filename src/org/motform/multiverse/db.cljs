(ns org.motform.multiverse.db
  (:require
    [cljs.reader :as reader]
    [org.motform.multiverse.open-ai :as-alias open-ai]
    [org.motform.multiverse.prompts :refer [prompts]]
    [re-frame.core :as rf]))

;; We store sentences as tree implemented by as an indexed map.
;; The map uses nano id's to index at root level, an entry always looks like this:
;;  
;; "h7yr0N3hOX" {sentence/:text "foo", sentence/:id "h7yr0N3hOX", :sentence/path ["lnhSB6_qB7" "h7yr0N3hOX"], :sentence/children []}
;;  
;; This would be a leaf node, following its `:sentence/path` _in order_ yields this story.
;; Note that the path always ends with self. `:sentence/children` is simply a vec of `:sentence/id`.
;; There duplication as the id should always eq the end of the path vec, but we let that slide for nice destructuring.
;;  
;; The reason for this structure is that I had some troubles getting zippers to do what I wanted.
;; Using a shallow map and indexes, we should have constant access times (every step is O(log₃₂n), so constant-ish).
;;  
;; — LLA, 200514

(def default-db
  {:db/stories {}
   :db/state
   {:page/active              :page/landing
    :new-story/title          ""
    :new-story/prompt         "" ; the initial sentence
    :new-story/prompt-version :prompt/v1 ; :prompt/v1, :prompt/v2
    :new-story/system-message (get-in prompts [:prompt/v1 :system])
    :new-story/user-message   (get-in prompts [:prompt/v1 :user])
    :new-story/model          ::open-ai/gpt-3.5-turbo ; ::open-ai/gpt-4o
    :story/active             nil
    :story/recent             []
    :sentence/active          nil
    :sentence/highlight       {:id nil :source nil}
    :sentence/preview         nil
    :sentence/tab             nil
    :open-ai/pending-request? false
    :open-ai/key              #:open-ai{:api-key "" :validated? false}}})

;; local-storage

(def ls-key "multiverse.stories")

(defn collections->local-storage [db]
  (.setItem js/localStorage ls-key (str db)))

(rf/reg-cofx ; source: re-frame docs
 :local-store-collections
 (fn [cofx _]
   (assoc cofx :local-store-collections
          (some->> (.getItem js/localStorage ls-key) (reader/read-string)))))

(defn children
  ([db parent-id] (children db parent-id nil))
  ([db parent-id story-id]
   (let [story-id (or story-id (get-in db [:db/state :story/active]))]
     (select-keys (get-in db [:db/stories story-id :story/sentences])
                  (get-in db [:db/stories story-id :story/sentences parent-id :sentence/children])))))

(defn request-data [db]
  (let [story-id  (get-in db [:db/state :story/active])
        story-meta #(conj [:db/stories story-id :story/meta] %)]
    {:story-id  story-id
     :api-key   (get-in db [:db/state :open-ai/key :open-ai/api-key])
     :parent-id (get-in db (story-meta :sentence/active))
     :model     (get-in db (story-meta :story/model))
     :system-message (get-in db (story-meta :story/system-message))
     :user-message   (get-in db (story-meta :story/user-message))}))

(defn paragraph [db story-id sentence-id]
  (reduce
    (fn [sentences id]
      (conj sentences (get-in db [:db/stories story-id :story/sentences id])))
    [] (get-in db [:db/stories story-id :story/sentences sentence-id :sentence/path])))
