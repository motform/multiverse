(ns neural.multiverse.components.header
  (:require [neural.multiverse.routes :as routes]
            [re-frame.core :as rf]))

(defn header-item [label key active-page]
  [:a.hitem
   {:class (when (= key active-page) "highlight")
    :href (routes/url-for key)}
   label])

(defn header []
  (let [active-page @(rf/subscribe [:active-page])]
    [:header
     [header-item "multiverse" :story active-page]
     [header-item "new story" :new-story active-page]
     [header-item "library" :library active-page]
     [header-item "about" :about active-page]]))
