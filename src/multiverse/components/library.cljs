(ns multiverse.components.library
  (:require [re-frame.core :as rf]
            [multiverse.routes :as routes]
            [multiverse.util :as util]
            [clojure.string :as str]))

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
      [:div (str (count sentences) " nodes")]
      [:div "Last explored " (util/format-date updated)]
      ;; TODO Right now, this will obliviously not work as the parent on-click
      ;;      acts before this one, making for all sorts of strange things.
      ;;      Will solve this when I'm a bit more attentive.
      #_[:div.delete
         {:on-click #(rf/dispatch [:dissoc-story id])}
         "Delete story"]]]))

(defn library-items [stories]
  [:<>
   (for [story stories]
     ^{:key (:title story)}
     [library-item story])
   [:div.clear-library
    [:span
     {:on-click #(when (.confirm js/window "Do you really want to clear the library? This can not be undone!")
                   (rf/dispatch [:clear-library]))}
     "empty library"]]])

(defn empty-library []
  [:section.landing>div "The Library is empty, go" [:br]
   [:a {:href (routes/url-for :new-story)} "explore"]
   " a literary space."])

(defn library []
  (let [stories @(rf/subscribe [:stories])]
    [:main.library
     (if stories 
       [library-items stories]
       [empty-library])]))
