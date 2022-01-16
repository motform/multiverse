(ns org.motform.multiverse.components.story
  (:use-macros [cljs.core.async.macros :only [go]])
  (:require [clojure.string :as str]
            [clojure.core.async :refer [<! timeout]]
            [re-frame.core :as rf]                       
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [org.motform.multiverse.components.map :refer [tree-map]]
            [org.motform.multiverse.util :as util]
            [org.motform.multiverse.components.header :refer [header]]))

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
             :class (str (when-not visited?  "un") "visited child " (highlight? id))
             :on-click      #(rf/dispatch [:active-sentence    id])
             :on-mouse-over #(rf/dispatch [:highlight-sentence id])
             :on-mouse-out  #(rf/dispatch [:remove-highlight])}
   (if (str/blank? text) "..." text)])

(defn typewrite' [*i text]
  (when-not (= @*i (count text))
    (go (<! (timeout 1)) (swap! *i inc)))
  [:span (subs text 0 @*i)])

(defn typewrite [text]
  (let [*i (r/atom 0)]
    [typewrite' *i text]))

(defn parent-sentence [{:keys [text id children]} sentences potential-path]
  [:span {:id id
          :class (str "parent " (highlight? id))
          :on-pointer-down #(rf/dispatch [:active-sentence    id])
          :on-pointer-over #(rf/dispatch [:highlight-sentence id])
          :on-pointer-out  #(rf/dispatch [:remove-highlight])}
   (if (and (not (contains? (set sentences) potential-path))
            (= id (get potential-path :id nil)))
     (typewrite text)
     text)])

(defn scroll-indicators [event]
  (let [node (.-target event)]
    ;; (println "top" (.-scrollTop node))
    ;; (println "hi" (.-scrollHeight node))
    (if (< 20 (.-scrollTop node))
      (set! (.. node -style -borderTopWidth) "2px")
      (set! (.. node -style -borderTopWidth) "0px"))))

(defn parent-sentences [sentences potential-path]
  (r/create-class
   {:display-name "stories"

    :component-did-update
    (fn [this]
      (let [[_ sentences potential-path] (r/argv this)]
        (println "update!")
        (when-not (or (contains? (set sentences) potential-path)
                      (not potential-path))
          (let [node (rdom/dom-node this)]
            (set! (.-scrollTop node) (.-scrollHeight node))))))

    :reagent-render 
    (fn [sentences potential-path]
      [:section.sentences
       {:on-scroll scroll-indicators}
       (for [{:keys [id] :as sentence} (distinct (util/conj? sentences potential-path))]
         ^{:key id} [parent-sentence sentence sentences potential-path])])}))

(defn story []
  ;; NOTE this implementation means there can only be a single request out per parent, in theory, it is possible/preferable to have multiple ones.
  (let [parent    @(rf/subscribe [:active-sentence])
        request?  @(rf/subscribe [:pending-request?])
        preview?  @(rf/subscribe [:preview?])
        sentences @(rf/subscribe [:sentences parent])
        children  @(rf/subscribe [:children parent])
        potential-path @(rf/subscribe [:potential-path])]

    ;; First, see if we have to request any new completions
    (when (not-any? identity [children preview? request?])
      (rf/dispatch [:open-ai/completions parent (util/format-story sentences)]))

    [:<>
     [parent-sentences sentences potential-path]
     (if request?
       [:section.children.pad-half [util/spinner]]
       [:section.children.h-equal-3.gap-double
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
   [:main.story.pad-full
    [story]]])
