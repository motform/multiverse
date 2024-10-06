(ns org.motform.multiverse.components.library
  (:require
    [clojure.string :as str]
    [nano-id.core :refer [nano-id]]
    [org.motform.multiverse.routes :as routes]
    [org.motform.multiverse.story :as story]
    [org.motform.multiverse.util :as util]
    [re-frame.core :as rf]))


(defn export-markdown []
  (util/download-file
    (->> @(rf/subscribe [:db/stories]) (map story/->md) (str/join "\n\n"))
    (str "multiverse-library-export-" (nano-id) ".md")
    "text/markdown"))

(defn open-story [id]
  (rf/dispatch [:story/active id])
  (rf/dispatch [:page/active :page/story])
  (. (.-history js/window) pushState #js {} "" (routes/url-for :page/story)))

(defn LibraryItemRow [{:story/keys [meta sentences]}]
  (let [{:story/keys [title id model prompt-version updated]} meta]
    [:tr.library-item-row
     {:on-pointer-down #(open-story id)}
     [:td (or (util/title-case title) "Generating title...")]
     [:td id]
     [:td (name model)]
     [:td (or prompt-version "N/A")]
     [:td (count sentences)]
     [:td (util/format-date updated)]]))

(defn LibraryItems []
  [:table.library-items
   [:thead
    [:tr
     [:th "Generated Title"]
     [:th "ID"]
     [:th "Model"]
     [:th "Prompt"]
     [:th "Length"]
     [:th "Date"]]]
   [:tbody
    (for [story (reverse @(rf/subscribe [:db/stories]))]
      ^{:key (get-in story [:story/meta :story/id])}
      [LibraryItemRow story])]])

(defn LibraryToggles []
  [:section.h-stack.spaced.centered
   [:p>a.source-code {:href "https://github.com/motform/multiverse" :target "_bank"}
    "Source code avalible on GitHub"]
   [:section.h-stack.gap-half
    [:button.shadow-medium.button-secondary.rounded
     {:on-pointer-down #(when (.confirm js/window "Do you really want to empty the library? This deletes all stories and can not be undone!")
                          (rf/dispatch [:library/clear]))}
     "empty library"]
    [:button.button-secondary.rounded.shadow-medium
     {:on-pointer-down #(export-markdown)}
     "export library"]]])

(defn Empty []
  [:section>p "The Library is empty, go" [:br]
   [:a {:href (routes/url-for :page/new-story)} "explore"]
   " a new story."])

(defn Library []
  [:main.library.v-stack.gap-double.pad-half
   (if @(rf/subscribe [:db/stories])
     [:<>
      [LibraryItems]
      [LibraryToggles]]
     [Empty])])
