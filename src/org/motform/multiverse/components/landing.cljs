(ns org.motform.multiverse.components.landing
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [org.motform.multiverse.routes :as routes]))

(defn circle [r cx cy]
  (let [refresh (r/atom 5)
        filled? (when (< 2 (rand-int @refresh))
                  {:fill "var(--circle-fill)"
                   :stroke "var(--circle-fill)"})]
    (js/setInterval #(swap! refresh inc) 3000)
    (when (< 1 (rand-int @refresh))
      [:circle.circle
       (merge {:r r :cx cx :cy cy
               :stroke "var(--circle-stroke)"
               :stroke-width "1"
               :fill "none"}
              filled?)])))

(defn circles []
  (let [wh (quot (. js/window -innerHeight) 2.7) ; arbitrary
        ww (. js/window -innerWidth)
        r  (dec (quot wh 6))] ; dec to properly fit three circles with gaps
    [:div.circles-container>svg.circles
     (for [cy (range (inc r) (+ 200 wh) (+ 2 (* r 2)))  ; three rows
           cx (range 0       (inc ww) (+ 2 (* r 2)))] ; fills the rest
       ^{:key (str cx cy)} [circle r cx cy])]))

(defn elide [s n dots]
  (apply str (concat (take n s) (repeat dots ".") (drop (- (count s) n) s))))

(defn key-input-textarea [api-key validated?]
  [:textarea.key-input.shadow-medium.textarea-small.rounded.mono
   {:value        (if (< 10 (count api-key)) (elide api-key 3 20) api-key)
    :spell-check  false
    :class        (when validated? "key-valid")
    :on-change    #(rf/dispatch [:open-ai/update-api-key (.. % -target -value)])
    :on-key-down  #(when (= (.-key %) "Enter")
                     (.preventDefault %)
                     (if validated?
                       (rf/dispatch [:active-page :new-story])
                       (rf/dispatch [:open-ai/validate-api-key])))}])

(defn name-input-textarea []
  [:textarea.author.shadow-medium.textarea-small.rounded
   {:value @(rf/subscribe [:name])
    :on-change #(rf/dispatch [:name (-> % .-target .-value)])}])

(defn key-input []
  (let [{:keys [api-key validated?]} @(rf/subscribe [:open-ai])]
    [:article.key-input-container.v-stack.gap-full.rounded-large.shadow-large.blurred.pad-full.border
     [:section.v-stack.gap-half
      [:div.v-stack.gap-quarter 
       [:label.offset-label "Name"]
       [name-input-textarea]]
      [:div.v-stack.gap-quarter 
       [:label.offset-label "OpenAI API key"]
       [key-input-textarea api-key validated?]]] 
     [:a {:href (when validated? (routes/url-for :new-story))
          :style {:width "100%"}
          :on-pointer-down #(when (not validated?)
                              (.preventDefault %)
                              (rf/dispatch [:open-ai/validate-api-key]))}
      [:button.open-ai-key-dispatch.rounded.shadow-small
       {:disabled (some str/blank? [name api-key])
        :style {:width "100%"}}
       (if validated?
         "start" ; TODO: go to the library if we have stroies 
         "validate key")]]]))

(defn landing-blurb []
  [:section.landing-blurb.v-stack.gap-half
   [:p "is a prototype system for interactive generative literature. It allows to user-reader to non-linearly explore a literary space generated by OpenAI’s GPT family of machine learning langauge models. Read more "[:a {:href "https://motform.org/multiverse" :target "_blank"} "here"] "."]
   [:p "The system is requires an " [:a {:href "https://openai.com/api/" :target "_blank"} "OpenAI API key"] " to run. You will have to provide your own unless otherwise specified. All data is stored locally."]])

(defn landing []
  [:<>
   [circles]
   [:section.landing.v-stack.overlay
    [:div.landing-container.v-stack.gap-half
     [:h1 "Multiverse"]
     [:div.h-stack.gap-double
      [landing-blurb]
      [key-input]]]]])
