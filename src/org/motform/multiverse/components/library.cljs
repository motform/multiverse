(ns org.motform.multiverse.components.library
  (:require
    [nano-id.core :refer [nano-id]]
    [org.motform.multiverse.routes :as routes]
    [org.motform.multiverse.util :as util]
    [re-frame.core :as rf]))

(defn- format-date [date]
  (-> (js/Intl.DateTimeFormat. "en-US" #js {:year "numeric" :month "2-digit" :day "2-digit"})
      (.format date)))

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
     [:td model]
     [:td (or prompt-version "N/A")]
     [:td (count sentences)]
     [:td (format-date updated)]]))

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
      ^{:key (get-in story [:story/meta :story/id])} [LibraryItemRow story])]])

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
     {:on-pointer-down #(export-library)}
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
