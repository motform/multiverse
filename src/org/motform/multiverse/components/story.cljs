(ns org.motform.multiverse.components.story
  (:use-macros [cljs.core.async.macros :only [go]])
  (:require [clojure.string :as str]
            [clojure.core.async :refer [<! timeout]]
            [re-frame.core :as rf]                       
            [reagent.core :as r]
            [org.motform.multiverse.components.header :refer [header]]           [reagent.dom :as rdom]
            [org.motform.multiverse.components.map :as map]
            [org.motform.multiverse.components.personalities :refer [personalities]]
            [org.motform.multiverse.open-ai :as open-ai]
            [org.motform.multiverse.util :as util]))

;;; Util

(defn highlight? [id]
  (when-let [highlight @(rf/subscribe [:highlight])]
    (let [path (set @(rf/subscribe [:path highlight]))]
      (when-not (contains? path id)
        "inactive"))))

;;; Landing
;;; Story

(defn child-sentence [text id visited?]
  [:div>div {:id id
             :class (str (when-not visited?  "un") "visited child " #_(highlight? id)) ; NOTE
             :on-pointer-down #(rf/dispatch [:active-sentence    id])
             :on-pointer-over #(rf/dispatch [:highlight-sentence id])
             :on-pointer-out  #(rf/dispatch [:remove-highlight])}
   (if (str/blank? text) "..." text)])

(defn typewrite [text]
  (let [*i (r/atom 0)]
    (fn [text]
      (when-not (= @*i (count text))
        (go (<! (timeout 1)) (swap! *i inc)))
      [:span (subs text 0 @*i)])))

(defn branch-marks [id first-sentence?]  
  (let [count-branches @(rf/subscribe [:count-realized-children id])]
    [:span.branch-marks
     (if (zero? count-branches)
       [:span.weak-branch-mark]
       (for [i (range count-branches)]
         ^{:key i} [:span.branch-mark]))]))

(defn parent-sentence [{:keys [id text] :as sentence} sentences potential-path]
  (let [sentence-not-in-story? (and (not (contains? (set sentences) potential-path))
                                    (= id (get potential-path :id nil)))]
    [:span {:id id
            :class (str "parent " (highlight? id))
            :on-pointer-down #(rf/dispatch [:active-sentence    id])
            :on-pointer-over #(rf/dispatch [:highlight-sentence id])
            :on-pointer-out  #(rf/dispatch [:remove-highlight])}
     [:<> (if sentence-not-in-story?
            [typewrite text]
            text)
      (when-not sentence-not-in-story? [branch-marks id])]]))

(defn scroll-indicators [event]
  (let [node (.-target event)]
    (if (< 20 (.-scrollTop node))
      (set! (.. node -style -borderTopWidth) "4px")
      (set! (.. node -style -borderTopWidth) "0px"))))

(defn parent-sentences [sentences potential-path potential-path-in-parents?]
  (r/create-class
   {:display-name "stories"

    :component-did-update
    (fn [this] ; Scroll to the bottom of the story view when we append a completion.
      (let [[_ _ potential-path potential-path-in-parents?] (r/argv this)]
        (when-not (or potential-path-in-parents? (not potential-path))
          (let [node (rdom/dom-node this)]
            (set! (.-scrollTop node) (.-scrollHeight node))))))

    :reagent-render 
    (fn [sentences potential-path potential-path-in-parents?]
      [:section.sentences.pad-full
       {:on-scroll scroll-indicators}
       (for [{:keys [id] :as sentence} (distinct (util/conj? sentences potential-path))]
         ^{:key id} [parent-sentence sentence sentences potential-path])])}))

(defn story []
  ;; NOTE this implementation means there can only be a single request out per parent, in theory, it is possible/preferable to have multiple ones.
  (let [parent         @(rf/subscribe [:active-sentence])
        request?       @(rf/subscribe [:pending-request?])
        sentences      @(rf/subscribe [:sentences parent])
        potential-path @(rf/subscribe [:potential-path])
        children        (if @(rf/subscribe [:potential-path-in-parents?])
                          @(rf/subscribe [:children (:id potential-path)])
                          @(rf/subscribe [:children parent]))]

    ;; First, see if we have to request any new completions
    (when (not-any? identity [children request? @(rf/subscribe [:preview?])])
      (rf/dispatch [:open-ai/completions parent (open-ai/format-prompt sentences)]))

    [:<>
     (when sentences
       [:<> 
        [parent-sentences sentences potential-path]
        [map/radial-map]])
     (if request?
       [:section.children.pad-full [util/spinner]]
       [:section.children.h-equal-3.gap-double.pad-full
        (for [{:keys [id text children]} children]
          ^{:key id} [child-sentence text id (seq children)])])]))

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
   [:main.story.blurred.shadow-large.h-stack
    [personalities]
    [story]]])
