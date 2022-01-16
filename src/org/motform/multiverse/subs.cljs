(ns org.motform.multiverse.subs
  (:require [re-frame.core :refer [reg-sub]]))

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
 :potential-path
 (fn [db _]
   (let [potential-sentence-id (get-in db [:state :highlight])
         story (get-in db [:state :active-story])
         sentences (get-in db [:stories story :sentences])
         potential-sentence (sentences potential-sentence-id)]
     potential-sentence)))

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

;;; Landing 

(reg-sub
 :name
 (fn [db _]
   (get-in db [:state :name])))

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
 :sentences
 (fn [db [_ id]]
   (let [story (get-in db [:state :active-story])
         path (get-in db [:stories story :sentences id :path])]
     (reduce (fn [sentences id]
               (conj sentences (get-in db [:stories story :sentences id])))
             [] path))))

(reg-sub
 :sentence-tree
 (fn [db _]
   (let [story (get-in db [:state :active-story])]
     (get-in db [:stories story :sentences]))))

(reg-sub
 :visited?
 (fn [db [_ parent]]
   (let [story (get-in db [:state :active-story])]
     (seq (get-in db [:stories story :sentences parent :children])))))

(reg-sub
 :children
 (fn [db [_ parent]]
   (let [story (get-in db [:state :active-story])
         sentences (get-in db [:stories story :sentences])
         children (get-in db [:stories story :sentences parent :children])]
     (vals (select-keys sentences children)))))

;;; Ajax

(reg-sub
 :pending-request?
 (fn [db _]
   (get-in db [:state :pending-request?])))
