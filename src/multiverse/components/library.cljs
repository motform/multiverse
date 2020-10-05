(ns multiverse.components.library
  (:require [cljs.reader :as reader]
            [clojure.string :as str]
            [multiverse.routes :as routes]
            [multiverse.util :as util]
            [re-frame.core :as rf]))

(defn stort-library [stories {:keys [order desc?]} ]
  (let [sort-fn (case order
                  :updated #(get-in % [:meta :updated])
                  :sentences #(count (:sentences %)))]
    (cond->> stories
      sort-fn (sort-by sort-fn)
      desc? reverse)))

(defn library-item [{:keys [meta sentences]}]
  (let [{:keys [updated title authors id]} meta]
    [:a.library-item
     {:href (routes/url-for :story)
      :on-click #(do (rf/dispatch [:active-story id])
                     (rf/dispatch [:active-page :story])
                     true)}
     [:h1 (if-not (str/blank? title) (util/title-case title) "Generating title...")]
     [:div.lauthor "By " (apply str (util/proper-separation authors))]
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

(defn library-toggles []
  [:div.toggles
   [:div "SORT BY"]
   [:select {:on-change #(rf/dispatch [:library-sort (-> % .-target .-value reader/read-string)])} ;; HACK
    [:option {:value "{:order :updated :desc? true}"} "Last explored"]
    [:option {:value "{:order :updated :desc? false}"} "Unlast explored"]
    [:option {:value "{:order :sentences :desc? true}"} "Most sentences"]
    [:option {:value "{:order :sentences :desc? false}"} "Least sentences"]]
   [:span {:on-click #(when (.confirm js/window "Do you really want to clear the library? This can not be undone!")
                        (rf/dispatch [:clear-library]))}
    "empty library"]])

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
