(ns org.motform.multiverse.components.story
  (:require [clojure.string :as str]
            [re-frame.core :as rf]                       
            [org.motform.multiverse.components.map :refer [tree-map]]
            [org.motform.multiverse.routes :as routes]
            [org.motform.multiverse.util :as util]))

;;; Util

(defn highlight? [id]
  (when-let [highlight @(rf/subscribe [:highlight])]
    (let [path (set @(rf/subscribe [:path highlight]))]
      (when-not (contains? path id)
        "inactive"))))

;;; Landing

(defn circle [r cx cy]
  (let [filled? (when (< 2 (rand-int 5)) {:fill "gray" :class "filled"})]
    (when (< 1 (rand-int 5))
      [:circle.circle
       (merge {:r r :cx cx :cy cy :stroke "gray" :stroke-width "1" :fill "none"}
              filled?)])))

(defn circles []
  (let [wh (quot (. js/window -innerHeight) 2.7) ; arbitrary
        ww (. js/window -innerWidth)
        r  (dec (quot wh 6))] ; dec to properly fit three circles with gaps
    [:svg.circles {:style {:height wh :width ww}}
     (for [cy (range (inc r) (inc wh) (+ 2 (* r 2)))  ; three rows
           cx (range 0       (inc ww) (+ 2 (* r 2)))] ; fills the rest
       ^{:key (str cx cy)} [circle r cx cy])]))

(defn open-ai-key-input []
  (let [{:keys [api-key valid-format? validated?]} @(rf/subscribe [:open-ai])]
    [:div.open-ai-key
     [:p "OPEN-AI KEY"]
     [:textarea.open-ai-key-input
      {:value     api-key
       :class     (when (not (str/blank? api-key))
                    (if (and valid-format? validated?)
                      "open-ai-key-valid"
                      "open-ai-key-invalid"))
       :on-change #(rf/dispatch [:open-ai/update-api-key (.. % -target -value)])}]
     [:div {:style {:cursor "pointer"}
            :on-click #(rf/dispatch [:open-ai/validate-api-key])}
      "→"]]))

(defn landing []
  [:div.intro-wrapper>section.landing
   [:h1 "MULTIVERSE"]
   [:div "Cybertextual generative literature through machine learning"]
   [circles]
   [:div "Start a "
    [:a {:href (routes/url-for :new-story)} "new story"]
    " or keep reading one in the "
    [:a {:href (routes/url-for :library)} "library"]]
   [open-ai-key-input]])

;;; Story

(defn sentence [text id model class completion?]
  [(if completion? :div :span)
   {:id id :class (str class " " (highlight? id))
    :on-click #(rf/dispatch [:active-sentence id])
    :on-mouse-over #(rf/dispatch [:highlight-sentence id])
    :on-mouse-out  #(rf/dispatch [:remove-highlight])}
   #_[:span.sentence-model (if (= "Reformer" model) "RFMR" model)] ;; HACK
   (if (str/blank? text) "..." text)])

(defn pending []
  [:div.pending
   [:img.scribble {:src "assets/scribble-story.gif" :alt "Generating text…"}]])

;; NOTE this implementation means there can only be a single request out per parent,
;;      in theory, it is possible/preferable to have multiple ones
(defn story []
  (let [parent    @(rf/subscribe [:active-sentence])
        request?  @(rf/subscribe [:pending-request?])
        preview?  @(rf/subscribe [:preview?])
        sentences @(rf/subscribe [:sentences parent])
        children  @(rf/subscribe [:children parent])
        _ (when (and (not children) (not preview?) (not request?))
            (rf/dispatch [:open-ai/completions parent (util/format-story sentences)]))]
    [:section.story 
     [:div.sentences
      (for [{:keys [text id model]} sentences]
        ^{:key id} [sentence text id model "parent"])]
     (if request?
       [:section.children [pending]]
       [:section.children
        (for [{:keys [id text model]} children]
          ^{:key id} [sentence text id model "child" true])])]))

;;; Sidebar

(defn format-title [title]
  (if-not (str/blank? title)
    (-> title (str/replace #"[',\"\.\!]" "") util/title-case)
    "TODO"
    #_[:img.scribble {:src "assets/scribble-title.gif" :alt "Generating title…"}]))

(defn li-model [name active-model]
  [:li
   {:class (if (= name active-model) "active" "inactive")
    :on-click #(rf/dispatch [:change-model name])}
   name])

(defn sidebar []
  (let [{:keys [title model authors updated]} @(rf/subscribe [:meta])
        ;; _ (rf/dispatch [:page-title-story title])
        ]
    [:aside
     [:section.title>h1 (format-title title)]
     [:section.byline "By " (apply str (util/proper-separation authors))]
     [tree-map]
     #_[:section.model-sidebar
        ;; [:label "Model"]
        [:ul
         [li-model "GPT-2"  model]
         [li-model "Reformer"   model]
         [li-model "XLNet"  model]]]
     [:section.meta "Last Exploration " (util/format-date updated)]]))

;;; Main

(defn multiverse []
  (let [story? @(rf/subscribe [:active-story])]
    (if-not story?
      [landing]
      [:main.multiverse
       [sidebar]
       [story]])))
