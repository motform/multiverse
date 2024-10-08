(ns org.motform.multiverse.components.landing
  (:require
    [clojure.string :as str]
    [org.motform.multiverse.routes :as routes]
    [org.motform.multiverse.util :as util]
    [re-frame.core :as rf]))

(defn elide [s n dots]
  (apply str (concat
               (take n s)
               (repeat dots ".")
               (drop (- (count s) n) s))))

(defn KeyInputTextarea [api-key validated?]
  [:textarea.key-input.shadow-medium.textarea-small.rounded.mono
   {:value        (if (< 10 (count api-key)) (elide api-key 3 16) api-key)
    :spell-check  false
    :class        (when validated? "key-valid")
    :on-change    #(rf/dispatch [:open-ai/update-api-key (.. % -target -value)])
    :on-key-down  #(when (= (.-key %) "Enter")
                     (.preventDefault %)
                     (if validated?
                       (rf/dispatch [:page/active :page/new-story])
                       (rf/dispatch [:open-ai/validate-api-key])))}])

(defn KeyInput []
  (let [{:open-ai/keys [api-key validated?]} @(rf/subscribe [:open-ai/key])]
    [:article.key-input-container.v-stack.gap-quarter.rounded-large.shadow-large.blurred.pad-3-4.border
     [:div.v-stack.gap-quarter
      [:label.offset-label "OpenAI API key"]
      [:div.h-stack.gap-half
       [KeyInputTextarea api-key validated?]
       [:button.open-ai-key-dispatch.rounded.shadow-small
        {:disabled (str/blank? api-key)
         :on-pointer-down #(when-not validated? (rf/dispatch [:open-ai/validate-api-key]))}
        (if @(rf/subscribe [:open-ai/pending-request?])
          [util/spinner-small]
          "Check")]]]
     [:a {:href (when validated?
                  (routes/url-for (if (empty? @(rf/subscribe [:db/stories])) :page/new-story :page/library)))}
      [:button.open-ai-key-dispatch.rounded.shadow-small
       {:disabled (not validated?)
        :style {:width "100%"  :margin-top "var(--space-half)"}}
       "start"]]]))

(defn LandingBlurb []
  [:section.landing-blurb.v-stack.gap-half
   [:h1 "Multiverse"]
   [:p "A vision of the future of interactive generative literature. Non-linearly explore a literary space generated on the fly by OpenAI’s GPT family of machine learning langauge models. Read more " [:a {:href "https://motform.org/multiverse" :target "_blank"} "in my case study"] "."]
   [:p "Requires a GPT-4 enabled " [:a {:href "https://openai.com/api/" :target "_blank"} "OpenAI API key"] ". You will have to provide your own unless otherwise specified. Multiverse works best in fullscreen mode with toolbars disabled. All data is stored locally."]])

(defn Landing []
  [:div.landing-container.h-stack.gap-double
   [LandingBlurb]
   [KeyInput]])
