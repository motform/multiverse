(ns org.motform.multiverse.subs
  (:require [re-frame.core :as rf :refer [reg-sub]]))

;;; Prompt

(reg-sub
 :new-story/prompt
 (fn [db _]
   (get-in db [:state :new-story/prompt])))

;;; State

(reg-sub
 :page/active
 (fn [db _]
   (get-in db [:state :page/active])))

(reg-sub
 :sentence/active
 (fn [db _]
   (let [story (get-in db [:state :story/active])]
     (get-in db [:stories story :meta :sentence/active]))))

(reg-sub
 :sentence/highlight
 (fn [db _]
   (get-in db [:state :sentence/highlight])))

(reg-sub
 :story/active
 (fn [db _]
   (get-in db [:state :story/active])))

(reg-sub
 :prospect-path
 (fn [db _]
   (let [{prospect-sentence-id :id} (get-in db [:state :sentence/highlight])
         story (get-in db [:state :story/active])
         sentences (get-in db [:stories story :sentences])
         prospect-sentence (sentences prospect-sentence-id)]
     prospect-sentence)))

(reg-sub
 :preview?
 (fn [db _]
   (get-in db [:state :preview])))

(reg-sub
 :open-ai
 (fn [db _]
   (get-in db [:state :open-ai])))

;;; Library

(reg-sub
 :stories
 (fn [db _]
   (vals (:stories db))))

(reg-sub
 :author
 (fn [db _]
   (->> db :stories vals (map (comp :authors :meta)) first)))

;;; Story

(reg-sub
 :meta
 (fn [db _]
   (let [story (get-in db [:state :story/active])]
     (get-in db [:stories story :meta]))))

(reg-sub
 :path
 (fn [db [_ id]]
   (let [story (get-in db [:state :story/active])]
     (get-in db [:stories story :sentences id :path]))))

(reg-sub
 :active-path
 (fn [db _]
   (let [story           (get-in db [:state :story/active])
         active-sentence @(rf/subscribe [:sentence/active])
         {highlight :id} @(rf/subscribe [:sentence/highlight])]
     (set (get-in db [:stories story :sentences (or highlight active-sentence) :path])))))

(reg-sub
 :sentences
 (fn [db [_ id]]
   (let [story (get-in db [:state :story/active])
         path (get-in db [:stories story :sentences id :path])]
     (reduce (fn [sentences id]
               (conj sentences (get-in db [:stories story :sentences id])))
             [] path))))


(defn sentence-tree-level [sentences sentence-id active-sentence-id parent-id]
  (let [{:keys [children]} (sentences sentence-id)]
    {:name     sentence-id
     :info     parent-id  ; XXX confusing key
     :children (for [child-id children]
                 (sentence-tree-level sentences child-id active-sentence-id sentence-id))}))


(reg-sub
 :sentence-tree
 (fn [db _]
   (sentence-tree-level (get-in db [:stories @(rf/subscribe [:story/active]) :sentences])
                        @(rf/subscribe [:root-sentence])
                        @(rf/subscribe [:sentence/active])
                        nil)))

(reg-sub
 :root-sentence
 (fn [db _]
   (let [sentences (get-in db [:stories @(rf/subscribe [:story/active]) :sentences])]
     (-> sentences keys first sentences :path first))))

(reg-sub
 :visited?
 (fn [db [_ parent]]
   (let [story (get-in db [:state :story/active])]
     (seq (get-in db [:stories story :sentences parent :children])))))

(reg-sub
 :children
 (fn [db [_ parent-id]]
   (let [story-id (get-in db [:state :story/active])
         sentences (get-in db [:stories story-id :sentences])
         child-ids (get-in db [:stories story-id :sentences parent-id :children])]
     (vals (select-keys sentences child-ids)))))

(reg-sub
 :count-realized-children
 (fn [_ [_ parent-id]]
   (->> @(rf/subscribe [:children parent-id])
        (remove #(empty? (:children %)))
        count)))

(reg-sub
 :prospect-path-in-parents?
 (fn [_ _]
   (let [parent @(rf/subscribe [:sentence/active])
         sentences @(rf/subscribe [:sentences parent])]
     (contains? (set sentences) @(rf/subscribe [:prospect-path])))))

(reg-sub
 :prospect-path-has-children?
 (fn [_ _]
   (when-let [{highlight :id} @(rf/subscribe [:sentence/highlight])]
     (seq @(rf/subscribe [:children highlight])))))

;;; Personalites 

(reg-sub
 :active-personality
 (fn [db _]
   (get-in db [:state :personality/active])))

(reg-sub
 :personalities
 (fn [db _]
   (-> db :personalities vals)))

;;; Ajax

(reg-sub
 :pending-request?
 (fn [db _]
   (get-in db [:state :pending-request?])))
