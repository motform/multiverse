(ns org.motform.multiverse.components.library
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [nano-id.core :refer [nano-id]]
            [org.motform.multiverse.routes :as routes]
            [org.motform.multiverse.util :as util]))

(defn library-item [{:story/keys [meta sentences]}]
  (let [{:story/keys [updated title id]} meta]
    [:a.library-item.v-stack.spaced.gap-full.rounded.shadow-large.pad-half.border
     [:div 
      {:href (routes/url-for :story)
       :on-pointer-down #(do (rf/dispatch [:story/active id])
                             (rf/dispatch [:page/active :page/story])
                             (. (.-history js/window) pushState #js {} "" (routes/url-for :page/story)))}
      [:h2 (if-not (str/blank? title) (util/title-case title) "Generating title...")]]
     [:section.h-stack.spaced.library-item-meta.centered
      [:p (str (count sentences) " sentences")]
      [:p.library-delete-story
       {:on-pointer-down #(rf/dispatch [:story/delete id])}
       "Delete Story"]]]))

(defn library-items []
  [:section.library-items
   (for [story (reverse @(rf/subscribe [:db/stories]))]
     ^{:key (get-in story [:story/meta :story/id])} [library-item story])])

(defn export-library
  "SOURCE: https://gist.github.com/zoren/cc74758198b503b1755b75d1a6b376e7"
  []
  (let [library   (js/Blob. #js [(prn-str @(rf/subscribe [:db/stories]))] #js {:type "application/edn"})
        file-name (nano-id)
        edn-url   (js/URL.createObjectURL library)
        anchor    (doto (js/document.createElement "a")
                    (-> .-href (set! edn-url))
                    (-> .-download (set! file-name)))]
    (.click anchor)
    (js/URL.revokeObjectURL edn-url)))

(defn library-toggles []
  [:section.h-stack.spaced.centered
   [:p>a.source-code {:href "https://github.com/motform/multiverse" :target "_bank"}
    "Source code avalible on GitHub"]
   [:section.h-stack.gap-half
    [:button.shadow-medium.button-secondary.rounded
     {:on-pointer-down #(when (.confirm js/window "Do you really want to clear the library? This can not be undone!")
                          (rf/dispatch [:library/clear]))}
     "empty library"]
    [:button.button-secondary.rounded.shadow-medium
     {:on-pointer-down #(export-library)}
     "export library"]]])

(defn empty-library []
  [:section>p "The Library is empty, go" [:br]
   [:a {:href (routes/url-for :page/new-story)} "explore"]
   " a literary space."])

(defn library []
  [:main.library.v-stack.gap-double.pad-half
   (if @(rf/subscribe [:db/stories])
     [:<>
      [library-items]
      [library-toggles]]
     [empty-library])])
