(ns org.motform.multiverse.subs
  (:require [re-frame.core :as rf :refer [reg-sub]]
            [org.motform.multiverse.util :as util]))

(defn from-state [k]
  (fn [db _]
    (-> db :db/state k)))

;;; new-story

(reg-sub :new-story/prompt (from-state :new-story/prompt))
(reg-sub :new-story/template (from-state :new-story/template))

;;; tabs

(reg-sub :tab/highlight (from-state :tab/highlight))

;;; app

(reg-sub :page/active (from-state :page/active))

;;; sentence

(reg-sub :sentence/highlight (from-state :sentence/highlight))
(reg-sub :sentence/preview? (from-state :sentence/preview))

(reg-sub
 :sentence/active
 (fn [db [_ story-id]]
   (let [story-id (or story-id @(rf/subscribe [:story/active]))]
     (get-in db [:db/stories story-id :story/meta :sentence/active]))))

;;; story

(reg-sub :story/active (from-state :story/active))

(reg-sub
 :story/prospect-path
 (fn [db _]
   (let [{prospect-sentence-id :id} (get-in db [:db/state :sentence/highlight])
         story-id (get-in db [:db/state :story/active])
         sentences (get-in db [:db/stories story-id :story/sentences])
         prospect-sentence (sentences prospect-sentence-id)]
     prospect-sentence)))

(reg-sub
 :sentence/children
 (fn [db [_ parent-id story-id]]
   (vals (util/children db parent-id story-id))))

;;; db

(reg-sub
 :db/stories
 (fn [db _]
   (vals (:db/stories db))))

;;; Story

(reg-sub
 :story/meta
 (fn [db _]
   (let [story (get-in db [:db/state :story/active])]
     (get-in db [:db/stories story :story/meta]))))

(reg-sub
 :story/active-path
 (fn [db [_ story-id]]
   (let [story (or story-id (get-in db [:db/state :story/active]))
         active-sentence @(rf/subscribe [:sentence/active story])
         {highlight :id} @(rf/subscribe [:sentence/highlight])]
     (set (get-in db [:db/stories story :story/sentences (or highlight active-sentence) :sentence/path]))))) ; we don't have an inative path, but maybe that is OK?

;; The "paragraph" of a sentence returns all the complete sentence maps
;; Whereas the "path" of a sentence returns a vector with id's of its path from the root

(reg-sub
 :sentence/path
 (fn [db [_ id]]
   (let [story (get-in db [:db/state :story/active])]
     (get-in db [:db/stories story :story/sentences id :sentence/path]))))

(reg-sub
 :sentence/paragraph
 (fn [db [_ sentence-id story-id]]
   (let [story-id (or story-id @(rf/subscribe [:story/active]))
         sentence-id (or sentence-id @(rf/subscribe [:sentence/active story-id]))]
     (util/paragraph db story-id sentence-id))))

(defn sentence-tree-level [sentences sentence-id active-sentence-id parent-id]
  (let [{:sentence/keys [personality children]} (sentences sentence-id)]
    {:name        sentence-id
     :info        parent-id  ; XXX confusing key
     :personality personality
     :children    (for [child-id children]
                    (sentence-tree-level sentences child-id active-sentence-id sentence-id))}))

(reg-sub
 :story/sentence-tree
 (fn [db [_ story-id]]
   (let [story (or story-id @(rf/subscribe [:story/active]))]
     (sentence-tree-level (get-in db [:db/stories story :story/sentences])
                          @(rf/subscribe [:story/root-sentence story])
                          @(rf/subscribe [:sentence/active story])
                          nil))))

(reg-sub
 :story/root-sentence
 (fn [db [_ story-id]]
   (let [story (or story-id @(rf/subscribe [:story/active]))
         sentences (get-in db [:db/stories story :story/sentences])]
     (-> sentences keys first sentences :sentence/path first))))

(reg-sub
 :sentence/count-realized-children
 (fn [_ [_ parent-id]]
   (->> @(rf/subscribe [:sentence/children parent-id])
        (remove #(empty? (:sentence/children %)))
        count)))

(reg-sub
 :story/prospect-path-has-children?
 (fn [_ _]
   (when-let [{highlight :id} @(rf/subscribe [:sentence/highlight])]
     (seq @(rf/subscribe [:sentence/children highlight])))))

(reg-sub
 :story/recent
 (fn [db _]
   (let [recent  (get-in db [:db/state :story/recent])
         stories (-> db :db/stories (select-keys recent) vals)]
     (map :story/meta stories))))

(defn not-realized? [sentence]
  (->> sentence :sentence/children empty?))

(defn leaf? [db]
  (fn [sentence]
    (and (->> sentence :sentence/children seq)
         (->> sentence (util/children db) (remove not-realized?) count zero?))))

(reg-sub
 :story/leafs
 (fn [db [_ story-id]]
   (let [story-id (or story-id @(rf/subscribe [:story/active]))]
     (->> (get-in db [:db/stories story-id :story/sentences])
          vals
          (filter (leaf? db))))))

;;; Personalites 

(reg-sub :personality/active (from-state :personality/active))

(reg-sub
 :personality/personalities
 (fn [db _]
   (db :db/personalities)))

(reg-sub
 :personality/dominant-personality
 (fn [_ [_ sentence-id]]
   (->> @(rf/subscribe [:sentence/children (or sentence-id @(rf/subscribe [:sentence/active]))])
        (filter #(empty? (:sentence/children %)))
        first ; the invariant states that all unrealized children are from the same personality
        :sentence/personality)))

(reg-sub
 :sentence/child-personalities
 (fn [_ [_ sentence-id]]
   (let [children @(rf/subscribe [:sentence/children sentence-id])]
     (map :sentence/personality children))))

;;; OpenAI

(reg-sub :open-ai/pending-request? (from-state :open-ai/pending-request?))
(reg-sub :open-ai/key (from-state :open-ai/key))
