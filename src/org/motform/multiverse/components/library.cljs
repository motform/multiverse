(ns org.motform.multiverse.components.library
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [nano-id.core :refer [nano-id]]
            [org.motform.multiverse.routes :as routes]
            [org.motform.multiverse.components.header :refer [header]]
            [org.motform.multiverse.util :as util]))

(defn library-item [{:keys [meta sentences]}]
  (let [{:keys [updated title id]} meta]
    [:a.library-item.v-stack.spaced.gap-full.blurred.rounded.shadow-large.pad-half.border
     {:href (routes/url-for :story)
      :on-pointer-down #(do (rf/dispatch [:story/active id])
                            (rf/dispatch [:page/active :page/story])
                            (. (.-history js/window) pushState #js {} "" (routes/url-for :page/story)))}
     [:h2 (if-not (str/blank? title) (util/title-case title) "Generating title...")]
     [:section.h-stack.spaced
      [:p (str (count sentences) " sentences")]
      [:p "Last explored " (util/format-date updated)]]]))

(defn new-story-item []
  [:section.library-item-new-story-card.v-stack.centered
   [:a.library-item-new-story.shadow-large.tooltip-container
    {:href (routes/url-for :page/new-story)
     :on-pointer-down #(do (rf/dispatch [:page/active :page/new-story])
                           (. (.-history js/window) pushState #js {} "" (routes/url-for :page/new-story)))} ; routing schmouting
    "+"
    [:span.tooltip.rounded {:style {:margin-top "30px"}} "New Story"]]])

(defn library-items []
  [:section.library-items
   (for [story @(rf/subscribe [:db/stories])]
     ^{:key (get-in story [:meta :id])} [library-item story])
   [new-story-item]])

(defn export-library
  "SOURCE: https://gist.github.com/zoren/cc74758198b503b1755b75d1a6b376e7"
  []
  (let [library   (js/Blob. #js [(prn-str @(rf/subscribe [:db/stories]))] #js {:type "application/edn"})
        file-name (nano-id)
        edn-url   (js/URL.createObjectURL library)
        anchor    (doto (js/document.createElement "a")
                    (-> .-href (set! edn-url))
                    (-> .-download (set! file-name)))]
    (.appendChild (.-body js/document) anchor)
    (.click anchor)
    (.removeChild (.body js/document) anchor)
    (js/URL.revokeObjectURL edn-url)))

(defn library-toggles []
  [:section.h-stack.spaced.centered
   [:p>a.source-code {:href "https://github.com/motform/multiverse" :target "_bank"}
    "Source code avalible on GitHub"]
   [:section.h-stack.gap-half
    [:button.library-button.rounded.shadow-medium.blurred
     {:on-pointer-down #(when (.confirm js/window "Do you really want to clear the library? This can not be undone!")
                          (rf/dispatch [:library/clear]))}
     "empty library"]
    [:button.library-button.rounded.shadow-medium.blurred
     {:on-pointer-down #(export-library)}
     "export library"]]])

(defn empty-library []
  [:section>p "The Library is empty, go" [:br]
   [:a {:href (routes/url-for :page/new-story)} "explore"]
   " a literary space."])

(defn library []
  [:div.app-container.v-stack.overlay
   [header [:p.title.title-library "LIBRARY"]]
   [:main.library.v-stack.gap-double.pad-half
    (if @(rf/subscribe [:db/stories])
      [:<>
       [library-items]
       [library-toggles]]
      [empty-library])]])
