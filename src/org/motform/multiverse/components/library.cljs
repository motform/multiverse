(ns org.motform.multiverse.components.library
  (:require [cljs.reader :as reader]
            [clojure.string :as str]
            [re-frame.core :as rf]
            [org.motform.multiverse.routes :as routes]
            [org.motform.multiverse.util :as util]))

(defn stort-library [stories {:keys [order desc?]} ]
  (let [sort-fn (case order
                  :updated #(get-in % [:meta :updated])
                  :sentences #(count (:sentences %)))]
    (cond->> stories
      sort-fn (sort-by sort-fn)
      desc? reverse)))

(defn library-item [{:keys [meta sentences]}]
  (let [{:keys [updated title id]} meta]
    [:a.library-item
     {:href (routes/url-for :story)
      :on-click #(do (rf/dispatch [:active-story id])
                     (rf/dispatch [:active-page :story])
                     true)}
     [:h1 (if-not (str/blank? title) (util/title-case title) "Generating title...")]
     [:div.linfo
      [:div (str (count sentences) " sentences")]
      [:div "Last explored " (util/format-date updated)]
      ;; TODO Right now, this will obliviously not work as the parent on-click
      ;;      acts before this one, making for all sorts of strange things.
      ;;      Will solve this when I'm a bit more attentive.
      #_[:div.delete
         {:on-click #(rf/dispatch [:dissoc-story id])}
         "Delete story"]]]))

(defn library-items [stories]
  (let [sorting @(rf/subscribe [:sorting])
        sorted-stories (stort-library stories sorting)]
    [:<>
     (for [story sorted-stories]
       ^{:key (get-in story [:meta :id])}
       [library-item story])]))

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
  [:div.toggles
   [:div "SORT BY"]
   [:select {:on-change #(rf/dispatch [:library-sort (-> % .-target .-value reader/read-string)])} ;; HACK
    [:option {:value "{:order :updated :desc? true}"} "Last explored"]
    [:option {:value "{:order :updated :desc? false}"} "Unlast explored"] ; LOL
    [:option {:value "{:order :sentences :desc? true}"} "Most sentences"]
    [:option {:value "{:order :sentences :desc? false}"} "Least sentences"]]
   [:span {:on-click #(when (.confirm js/window "Do you really want to clear the library? This can not be undone!")
                        (rf/dispatch [:clear-library]))}
    "empty library"]
   [:span {:on-click #(export-library)} "export library"]])

(defn empty-library []
  [:section.landing>div "The Library is empty, go" [:br]
   [:a {:href (routes/url-for :new-story)} "explore"]
   " a literary space."])

(defn library []
  (let [stories @(rf/subscribe [:stories])]
    [:main.library
     (if stories 
       [:<>
        [library-toggles]
        [library-items stories]]
       [empty-library])]))
