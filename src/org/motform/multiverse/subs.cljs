(ns org.motform.multiverse.subs
  (:require [re-frame.core :as rf :refer [reg-sub]]))

;;; Prompt

(reg-sub
 :new-story/prompt
 (fn [db _]
   (get-in db [:db/state :new-story/prompt])))

;;; State

(reg-sub
 :page/active
 (fn [db _]
   (get-in db [:db/state :page/active])))

(reg-sub
 :sentence/active
 (fn [db _]
   (let [story (get-in db [:db/state :story/active])]
     (get-in db [:db/stories story :meta :sentence/active]))))

(reg-sub
 :sentence/highlight
 (fn [db _]
   (get-in db [:db/state :sentence/highlight])))

(reg-sub
 :story/active
 (fn [db _]
   (get-in db [:db/state :story/active])))

(reg-sub
 :story/prospect-path
 (fn [db _]
   (let [{prospect-sentence-id :id} (get-in db [:db/state :sentence/highlight])
         story (get-in db [:db/state :story/active])
         sentences (get-in db [:db/stories story :sentences])
         prospect-sentence (sentences prospect-sentence-id)]
     prospect-sentence)))

(reg-sub
 :sentence/preview?
 (fn [db _]
   (get-in db [:db/state :sentence/preview])))

(reg-sub
 :sentence/children
 (fn [db [_ parent-id]]
   (let [story-id (get-in db [:db/state :story/active])
         sentences (get-in db [:db/stories story-id :sentences])
         child-ids (get-in db [:db/stories story-id :sentences parent-id :children])]
     (vals (select-keys sentences child-ids)))))

;;; Library

(reg-sub
 :db/stories
 (fn [db _]
   (vals (:db/stories db))))

;;; Story

(reg-sub
 :story/meta
 (fn [db _]
   (let [story (get-in db [:db/state :story/active])]
     (get-in db [:db/stories story :meta]))))

(reg-sub
 :story/active-path
 (fn [db _]
   (let [story           (get-in db [:db/state :story/active])
         active-sentence @(rf/subscribe [:sentence/active])
         {highlight :id} @(rf/subscribe [:sentence/highlight])]
     (set (get-in db [:db/stories story :sentences (or highlight active-sentence) :path])))))

;; The "paragraph" of a sentence returns all the complete sentence maps
;; Whereas the "path" of a sentence returns a vector with id's of its path from the root

(reg-sub
 :sentence/path
 (fn [db [_ id]]
   (let [story (get-in db [:db/state :story/active])]
     (get-in db [:db/stories story :sentences id :path]))))

(reg-sub
 :sentence/paragraph
 (fn [db [_ id]]
   (let [story (get-in db [:db/state :story/active])
         path (get-in db [:db/stories story :sentences id :path])]
     (reduce (fn [sentences id]
               (conj sentences (get-in db [:db/stories story :sentences id])))
             [] path))))


(defn sentence-tree-level [sentences sentence-id active-sentence-id parent-id]
  (let [{:keys [children]} (sentences sentence-id)]
    {:name     sentence-id
     :info     parent-id  ; XXX confusing key
     :children (for [child-id children]
                 (sentence-tree-level sentences child-id active-sentence-id sentence-id))}))


(reg-sub
 :story/sentence-tree
 (fn [db _]
   (sentence-tree-level (get-in db [:db/stories @(rf/subscribe [:story/active]) :sentences])
                        @(rf/subscribe [:root-sentence])
                        @(rf/subscribe [:sentence/active])
                        nil)))

(reg-sub
 :root-sentence
 (fn [db _]
   (let [sentences (get-in db [:db/stories @(rf/subscribe [:story/active]) :sentences])]
     (-> sentences keys first sentences :path first))))

(reg-sub
 :visited?
 (fn [db [_ parent]]
   (let [story (get-in db [:db/state :story/active])]
     (seq (get-in db [:db/stories story :sentences parent :children])))))


(reg-sub
 :story/count-realized-children
 (fn [_ [_ parent-id]]
   (->> @(rf/subscribe [:sentence/children parent-id])
        (remove #(empty? (:children %)))
        count)))

(reg-sub
 :story/prospect-path-has-children?
 (fn [_ _]
   (when-let [{highlight :id} @(rf/subscribe [:sentence/highlight])]
     (seq @(rf/subscribe [:sentence/children highlight])))))

;;; Personalites 

(reg-sub
 :personality/active
 (fn [db _]
   (get-in db [:db/state :personality/active])))

(reg-sub
 :personality/personalities
 (fn [db _]
   (-> db :db/personalities vals)))

;;; OpenAI

(reg-sub
 :open-ai/pending-request?
 (fn [db _]
   (get-in db [:db/state :open-ai/pending-request?])))

(reg-sub
 :open-ai/key
 (fn [db _]
   (get-in db [:db/state :open-ai/key])))
