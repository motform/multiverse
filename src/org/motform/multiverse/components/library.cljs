(ns org.motform.multiverse.components.library
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [org.motform.multiverse.routes :as routes]
            [org.motform.multiverse.components.header :refer [header]]
            [org.motform.multiverse.util :as util]))

;; TODO Fix rounting
(defn library-item [{:keys [meta sentences]}]
  (let [{:keys [updated title id]} meta]
    [:a.library-item.v-stack.spaced.gap-full.blurred.rounded.shadow-large.pad-half.border
     {:href (routes/url-for :story)
      :on-pointer-down #(do (rf/dispatch [:active-story id])
                            (rf/dispatch [:active-page :story])
                            (. (.-history js/window) pushState #js {} "" (routes/url-for :story)))}
     [:h2 (if-not (str/blank? title) (util/title-case title) "Generating title...")]
     [:section.h-stack.spaced
      [:p (str (count sentences) " sentences")]
      [:p "Last explored " (util/format-date updated)]]]))

(defn library-items []
  [:section.library-items
   (for [story @(rf/subscribe [:stories])]
     ^{:key (get-in story [:meta :id])}
     [library-item story])])

(defn export-library
  "SOURCE: https://gist.github.com/zoren/cc74758198b503b1755b75d1a6b376e7"
  []
  (let [library   (js/Blob. #js [(prn-str @(rf/subscribe [:stories]))] #js {:type "application/edn"})
        file-name (str @(rf/subscribe [:author]) ".edn")
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
     {:on-pointer-down
      #(when (.confirm js/window "Do you really want to clear the library? This can not be undone!")
         (rf/dispatch [:clear-library]))}
     "empty library"]
    [:button.library-button.rounded.shadow-medium.blurred
     {:on-pointer-down #(export-library)}
     "export library"]]])

(defn empty-library []
  [:section>p "The Library is empty, go" [:br]
   [:a {:href (routes/url-for :new-story)} "explore"]
   " a literary space."])

(defn library []
  [:div.app-container.v-stack.overlay
   [header [:p.library-title "Library"]]
   [:main.library.v-stack.gap-double.pad-half
    (if @(rf/subscribe [:stories])
      [:<>
       [library-items]
       [library-toggles]]
      [empty-library])]])
