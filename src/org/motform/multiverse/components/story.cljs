(ns org.motform.multiverse.components.story
  (:use-macros [cljs.core.async.macros :only [go]])
  (:require [clojure.string :as str]
            [clojure.core.async :refer [<! timeout]]
            [re-frame.core :as rf]                       
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [org.motform.multiverse.components.header :refer [header]]           
            [org.motform.multiverse.components.map :as map]
            [org.motform.multiverse.components.personalities :refer [personalities]]
            [org.motform.multiverse.open-ai :as open-ai]
            [org.motform.multiverse.util :as util]))

(defn highlight? [id]
  (when-let [{highlight :id} @(rf/subscribe [:sentence/highlight])]
    (let [active-sentence    @(rf/subscribe [:sentence/active])
          in-highlight-path? (set @(rf/subscribe [:path highlight]))
          in-active-path?    (set @(rf/subscribe [:path active-sentence]))
          child?             (set (map :id @(rf/subscribe [:sentence/children active-sentence])))]
      (cond (= highlight active-sentence) ""
            (and (= highlight id)         (child? id)) "active"
            (and (in-active-path? id)     (in-highlight-path? id)) "active"
            (in-active-path? id)          "inactive"
            :else "inactive"))))

(defn child-selector [text id visited?]
  [:div {:id id
         :class (str (when-not visited?  "un") "visited child " (highlight? id)) ; NOTE
         :on-pointer-down #(rf/dispatch [:sentence/active id])
         :on-pointer-over #(rf/dispatch [:sentence/highlight id :source/children])
         :on-pointer-out  #(rf/dispatch [:sentence/remove-highlight])}
   (if (str/blank? text) "..." text)])

(defn typewrite [text]
  (let [*i (r/atom 0)]
    (fn [text]
      (when-not (= @*i (count text))
        (go (<! (timeout 1)) (swap! *i inc)))
      [:span (subs text 0 @*i)])))

(defn branch-marks [id]  
  (let [count-branches @(rf/subscribe [:story/count-realized-children id])]
    [:span.branch-marks
     (if (zero? count-branches)
       [:span.weak-branch-mark]
       (for [i (range count-branches)]
         ^{:key i} [:span.branch-mark]))]))

(defn sentence
  [{:keys [id text]} sentences prospect-path]
  (let [sentence-not-in-story? (and (not (contains? (set sentences) prospect-path))
                                    (= id (get prospect-path :id nil)))]
    [:span {:id id
            :class (str "sentence " (highlight? id))
            :on-pointer-down #(rf/dispatch [:sentence/active id])
            :on-pointer-over #(rf/dispatch [:sentence/highlight id :source/sentences])
            :on-pointer-out  #(rf/dispatch [:sentence/remove])}
     (if sentence-not-in-story?
       [typewrite text]
       [:<> text [branch-marks id]])
     #_[:<>
        (if-not sentence-in-story? [typewrite text] text)
        (when sentence-in-story? [branch-marks id])]]))

(defn scroll-indicators [event]
  (let [node (.-target event)]
    (set! (.. node -style -borderTopWidth)
          (if (< 20 (.-scrollTop node)) "4px" "0px"))))

(defn paragraph [sentences prospect-path]
  (r/create-class
   {:display-name "paragraph"

    :component-did-update
    (fn [this] ; Scroll to the bottom of the story view when we append a completion.
      (let [[_ _ prospect-path prospect-path-in-parents?] (r/argv this)
            node (rdom/dom-node this)]
        (when-not (or prospect-path-in-parents? (not prospect-path))
          (set! (.-scrollTop node) (.-scrollHeight node)))))

    :reagent-render 
    (fn [sentences prospect-path]
      [:section.paragraph.pad-full
       {:on-scroll scroll-indicators}
       (for [s (distinct (util/conj? sentences prospect-path))]
         ^{:key (:id s)} [sentence s sentences prospect-path])])}))

(defn story []
  (let [active-sentence @(rf/subscribe [:sentence/active])
        request?        @(rf/subscribe [:open-ai/pending-request?])
        prospect-path   @(rf/subscribe [:story/prospect-path])
        {highlight :id
         highlight-source :source} @(rf/subscribe [:sentence/highlight])

        children            (if (and (= :source/map highlight-source) @(rf/subscribe [:story/prospect-path-has-children?]))
                              @(rf/subscribe [:sentence/children (:id prospect-path)])
                              @(rf/subscribe [:sentence/children active-sentence]))

        highlighting-other-subtree (and highlight ; get another branch if hovering over another branch
                                        (not (contains? (set @(rf/subscribe [:path active-sentence])) highlight))
                                        (not (contains? (set (map :id children)) highlight)))
        
        sentences (if highlighting-other-subtree
                    @(rf/subscribe [:sentences highlight])
                    @(rf/subscribe [:sentences active-sentence]))]

    ;; First, see if we have to request any new completions
    (when (not-any? identity [children request? @(rf/subscribe [:sentence/preview?])])
      (rf/dispatch [:open-ai/completions active-sentence (open-ai/format-prompt sentences)]))

    [:main.story.blurred.shadow-large.h-stack
     [personalities]
     (when sentences
       [:<>
        [paragraph sentences prospect-path]
        [map/radial-map]])
     (if request?
       [:section.children.pad-full [util/spinner]]
       [:section.children.h-equal-3.gap-double.pad-full
        (for [{:keys [id text children]} children]
          ^{:key id} [child-selector text id (seq children)])])]))

;;; Header

(defn title []
  (let [{:keys [title]} @(rf/subscribe [:meta])]
    (if-not (str/blank? title)
      [:h2.title.tooltip-container
       {:on-pointer-down #(rf/dispatch [:open-ai/title nil])}
       title
       [:span.tooltip.tooltip-large.rounded "Generate new title"]]
      [util/spinner])))

;;; Main

(defn multiverse []
  [:div.app-container.v-stack.overlay
   [header [title]]
   [story]])
