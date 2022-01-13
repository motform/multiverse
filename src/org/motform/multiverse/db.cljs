(ns org.motform.multiverse.db
  (:require [cljs.reader :as reader]
            [re-frame.core :as rf]))

;; We store sentences as tree implemented by as an indexed map.
;; The map uses nano id's to index at root level, an entry always looks like this:
;;  
;; "h7yr0N3hOX" {:text "foo", :id "h7yr0N3hOX", :path ["lnhSB6_qB7" "h7yr0N3hOX"], :children []}
;;  
;; This would be a leaf node, following its `:path` _in order_ yields this story.
;; Note that the path always ends with self. `:children` is simply a vec of id's.
;; There duplication as the id should always eq the end of the path vec, but we let that slide for nice destructuring.
;;  
;; The reason for this structure is that I had some troubles getting zippers to do what I wanted.
;; Also, as Jblow teaches us, a specialized solution is always preferable than something overly-generic.
;; Using a shallow map and indexes, we should have constant access times (every step is O(log₃₂n), so constant-ish).
;;  
;; — LLA, 200514

;; TODO add timestamp to each sentence

(def default-db
  {:state {:active-page      :landing
           :active-sentence  nil
           :active-story     nil
           :highlight        nil
           :preview          nil
           :pending-request? false
           :new-story       {:text "" :author "" :model "GPT-3"}
           :sorting         {:order :updated :desc? false}
           :open-ai         {:api-key ""
                             :valid-format? false
                             :validated?    false}}
   :stories {}})

;;; local-storage

(def ls-key "multiverse.stories")

(defn collections->local-storage [db]
  (.setItem js/localStorage ls-key (str (:stories db))))

(rf/reg-cofx ; source: re-frame docs
 :local-store-collections
 (fn [cofx _]
   (assoc cofx :local-store-collections
          (some->> (.getItem js/localStorage ls-key) (reader/read-string)))))
