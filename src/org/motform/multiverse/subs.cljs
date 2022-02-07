(ns org.motform.multiverse.subs
  (:require [re-frame.core :as rf :refer [reg-sub]]))

;;; Prompt

(reg-sub
 :new-story
 (fn [db _]
   (get-in db [:state :new-story])))

;;; State

(reg-sub
 :active-page
 (fn [db _]
   (get-in db [:state :active-page])))

(reg-sub
 :active-sentence
 (fn [db _]
   (let [story (get-in db [:state :active-story])]
     (get-in db [:stories story :meta :active-sentence]))))

(reg-sub
 :active-story
 (fn [db _]
   (get-in db [:state :active-story])))

(reg-sub
 :highlight
 (fn [db _]
   (get-in db [:state :highlight])))

(reg-sub
 :prospect-path
 (fn [db _]
   (let [prospect-sentence-id (get-in db [:state :highlight])
         story (get-in db [:state :active-story])
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
   (let [story (get-in db [:state :active-story])]
     (get-in db [:stories story :meta]))))

(reg-sub
 :path
 (fn [db [_ id]]
   (let [story (get-in db [:state :active-story])]
     (get-in db [:stories story :sentences id :path]))))

(reg-sub
 :active-path
 (fn [db _]
   (let [story           (get-in db [:state :active-story])
         active-sentence @(rf/subscribe [:active-sentence])
         highlight       @(rf/subscribe [:highlight])]
     (set (get-in db [:stories story :sentences (or highlight active-sentence) :path])))))

(reg-sub
 :sentences
 (fn [db [_ id]]
   (let [story (get-in db [:state :active-story])
         path (get-in db [:stories story :sentences id :path])]
     (reduce (fn [sentences id]
               (conj sentences (get-in db [:stories story :sentences id])))
             [] path))))


(defn sentence-tree-level [sentences sentence-id active-sentence-id parent-id]
  (let [{:keys [children]} (sentences sentence-id)]
    {:name     sentence-id
     :info     parent-id  ; XXX confusing key
     :children (for [child-id children
                     :when
                     true
                     #_(or (= active-sentence-id sentence-id)
                           (-> (sentences child-id) :children seq))
                     #_(-> (sentences child-id) :children seq)]
                 (sentence-tree-level sentences child-id active-sentence-id sentence-id))}))


(reg-sub
 :sentence-tree
 (fn [db _]
   (sentence-tree-level (get-in db [:stories @(rf/subscribe [:active-story]) :sentences])
                        @(rf/subscribe [:root-sentence])
                        @(rf/subscribe [:active-sentence])
                        nil)))

(reg-sub
 :root-sentence
 (fn [db _]
   (let [sentences (get-in db [:stories @(rf/subscribe [:active-story]) :sentences])]
     (-> sentences keys first sentences :path first))))

(reg-sub
 :visited?
 (fn [db [_ parent]]
   (let [story (get-in db [:state :active-story])]
     (seq (get-in db [:stories story :sentences parent :children])))))

(reg-sub
 :children
 (fn [db [_ parent-id]]
   (let [story-id (get-in db [:state :active-story])
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
   (let [parent @(rf/subscribe [:active-sentence])
         sentences @(rf/subscribe [:sentences parent])]
     (contains? (set sentences) @(rf/subscribe [:prospect-path])))))

(reg-sub
 :prospect-path-has-children?
 (fn [_ _]
   (when-let [highlight @(rf/subscribe [:highlight])]
     (seq @(rf/subscribe [:children highlight])))))

;;; Personalites 

(reg-sub
 :active-personality
 (fn [db _]
   (get-in db [:state :active-personality])))

(reg-sub
 :personalities
 (fn [db _]
   (-> db :personalities vals)))

;;; Ajax

(reg-sub
 :pending-request?
 (fn [db _]
   (get-in db [:state :pending-request?])))
