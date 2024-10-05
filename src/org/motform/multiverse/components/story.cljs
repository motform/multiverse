(ns org.motform.multiverse.components.story
  (:require
    [clojure.core.async :refer [<! timeout go]]
    [clojure.string :as str]
    [org.motform.multiverse.components.map :as map]
    [org.motform.multiverse.util :as util]
    [re-frame.core :as rf]
    [reagent.core :as r]
    [reagent.dom :as rdom]))

(defn highlight? [id]
  (when-let [{highlight :id} @(rf/subscribe [:sentence/highlight])]
    (let [active-sentence    @(rf/subscribe [:sentence/active])
          in-highlight-path? (set @(rf/subscribe [:sentence/path highlight]))
          in-active-path?    (set @(rf/subscribe [:sentence/path active-sentence]))
          child?             (set (map :sentence/id @(rf/subscribe [:sentence/children active-sentence])))]
      (cond (= highlight active-sentence) ""
            (and (= highlight id)         (child? id)) "active"
            (and (in-active-path? id)     (in-highlight-path? id)) "active"
            (in-active-path? id)          "inactive"
            :else "inactive"))))

(defn ChildSelector [text id visited?]
  [:div>div.shadow-large
   {:id id
    :class (str "neutral-" (when-not visited? "un") "visited child " (highlight? id)) ; NOTE
    :on-pointer-down #(rf/dispatch [:sentence/active id])
    :on-pointer-over #(rf/dispatch [:sentence/highlight id :source/children])
    :on-pointer-out  #(rf/dispatch [:sentence/remove-highlight])}
   (if (str/blank? text) "…" text)])

(defn typewrite [text]
  (let [*i (r/atom 0)]
    (fn [text]
      (when-not (= @*i (count text))
        (go (<! (timeout 1)) (swap! *i inc)))
      [:span (subs text 0 @*i)])))

(defn branch-marks [id]
  (let [count-branches @(rf/subscribe [:sentence/count-realized-children id])]
    [:span.branch-marks
     (if (zero? count-branches)
       [:span.weak-branch-mark {:class "neutral-branch-mark"}]
       (for [i (range count-branches)] ^{:key i}
            [:span.branch-mark {:class "neutral-branch-mark"}]))]))

(defn Sentence [{:sentence/keys [id text]} sentences prospect-path]
  (let [sentence-not-in-story? (and (not (contains? (set sentences) prospect-path))
                                    (= id (get prospect-path :sentence/id nil)))]
    [:span {:id id
            :class (str "sentence " (highlight? id))
            :on-pointer-down #(rf/dispatch [:sentence/active id])
            :on-pointer-over #(rf/dispatch [:sentence/highlight id :source/paragraph])
            :on-pointer-out  #(rf/dispatch [:sentence/remove-highlight])}
     (if sentence-not-in-story?
       [typewrite text]
       [:<> text [branch-marks id]])]))

(defn scroll-indicators [event]
  (let [node (.-target event)]
    (set! (.. node -style -borderTopWidth)
          (if (< 20 (.-scrollTop node)) "4px" "0px"))))

(defn Paragraph [paragraph prospect-path]
  (r/create-class
    {:display-name "paragraph"

     :component-did-update
     (fn [this] ; Scroll to the bottom of the story view when we append a completion.
       (let [[_ _ prospect-path prospect-path-in-parents?] (r/argv this)
             node (rdom/dom-node this)]
         (when-not (or prospect-path-in-parents? (not prospect-path))
           (set! (.-scrollTop node) (.-scrollHeight node)))))

     :reagent-render
     (fn [paragraph prospect-path]
       [:section.paragraph
        {:on-scroll scroll-indicators}
        (for [s (distinct (util/conj? paragraph prospect-path))]
          ^{:key (:sentence/id s)} [Sentence s paragraph prospect-path])])}))

(defn Multiverse []
  (let [active-sentence @(rf/subscribe [:sentence/active])
        request?        @(rf/subscribe [:open-ai/pending-request?])
        prospect-path   @(rf/subscribe [:story/prospect-path])
        {highlight :id highlight-source :source} @(rf/subscribe [:sentence/highlight])

        children            (if (and (= :source/map highlight-source) @(rf/subscribe [:story/prospect-path-has-children?]))
                              @(rf/subscribe [:sentence/children (:sentence/id prospect-path)])
                              @(rf/subscribe [:sentence/children active-sentence]))

        highlighting-other-subtree (and highlight ; get another branch if hovering over another branch
                                        (not (contains? (set @(rf/subscribe [:sentence/path active-sentence])) highlight))
                                        (not (contains? (set (map :sentence/id children)) highlight)))

        paragraphs (if highlighting-other-subtree
                     @(rf/subscribe [:sentence/paragraph highlight])
                     @(rf/subscribe [:sentence/paragraph active-sentence]))]

    ;; Do we have to request any new completions
    (when (not-any? identity [children request? @(rf/subscribe [:sentence/preview?])])
      (rf/dispatch [:open-ai/completions active-sentence paragraphs]))

    [:main.h-stack.story.gap-full
     [:<>
      (when paragraphs
        [:section.h-stack.gap-double.story-views.spaced
         [Paragraph paragraphs prospect-path]
         [map/RadialMap :source/story]])
      [:section.v-stack.gap-full.pad-full
       (if request?
         [:section.children.pad-full [util/spinner]]
         [:section.children.h-equal-3.gap-full
          (for [{:sentence/keys [id text children]} children] ^{:key id}
               [ChildSelector text id (seq children)])])]]]))
