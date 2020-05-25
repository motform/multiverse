(ns neural.multiverse.components.library
  (:require [re-frame.core :as rf]
            [neural.multiverse.routes :as routes]
            [neural.util :as util]
            [clojure.string :as str]))

(defn library-item [{:keys [meta sentences]}]
  (let [{:keys [title author model id]} meta]
    [:a.library-item
     {:href (routes/url-for :story)
      :on-click #(do (rf/dispatch [:active-story id])
                     (rf/dispatch [:active-page :story])
                     true)}
     [:h1 (if-not (str/blank? title) (util/title-case title) "Generating title...")]
     [:div.lauthor (str "By " author " & " model)]
     [:div.linfo (str (count sentences) " branches | 5 dimensions")]]))

(defn library []
  (let [stories @(rf/subscribe [:stories])]
    [:main.library
     (for [story stories]
       ^{:key (:title story)}
       [library-item story])]))
