(ns neural.multiverse.db
  (:require [cljs.spec.alpha :as s]))

;; We store sentences as graph implemented by as an indexed map.
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

(def =id-coll-item
  ^{:doc "The key in the containing map is the same `:id` as in the item."} 
  (s/every (fn [[k v]] (= (:id v) k))))

(s/def ::id (s/and string? #(= 10 (count %))))

(s/def ::db (s/keys :req-un [::state ::stories]))

(s/def ::state (s/keys :req-un [::active-page ::active-sentence ::active-story ::highlight
                                ::new-story ::pending-request? ::preview]))

(s/def ::active-page #{:about :library :new-story :story})
(s/def ::active-sentence (s/nilable ::id))
(s/def ::active-story (s/nilable ::id))
(s/def ::highlight (s/nilable ::id))
(s/def ::pending-request boolean?)
(s/def ::preview (s/nilable ::id))

(s/def ::new-story (s/keys :req-un [::text ::author ::model]))
(s/def ::text (s/nilable string?))
(s/def ::author (s/nilable string?))
(s/def ::model (s/nilable string?))

(s/def ::stories (s/and (s/map-of ::id ::story)
                        (s/every (fn [[k v]] (= (get-in v [:meta :id]) k)))))
(s/def ::story (s/keys :req-un [::meta ::sentences]))

(s/def ::meta (s/keys :req-un [::title ::author ::model ::id]))
(s/def ::title string?)

(s/def ::sentences (s/and (s/map-of ::id ::sentence)
                          (s/every (fn [[k v]] (= (:id v) k)))))
(s/def ::sentence (s/keys :req-un [::text ::id ::path ::children]))
(s/def ::path (s/coll-of ::id))
(s/def ::children (s/coll-of ::id))


(def default-db
  {:state {:active-page :story
           :active-sentence nil
           :active-story nil
           :highlight nil
           :preview nil
           :pending-request? false
           :new-story {:text ""
                       :author ""
                       :model "GPT-2"}}
   :stories {}})
