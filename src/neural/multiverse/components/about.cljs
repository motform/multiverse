(ns neural.multiverse.components.about
  (:require [re-frame.core :as rf]))

(defn about []
  [:main>div.about
   [:div "Multiverse is cybertextual system for interactive generative literature."]
   [:div "Done as part of my masters education in Interaction Design, for the course \n" [:em "Interaction Design: Thesis Project I"] ", at Malmö University.\nThe accompanying thesis is available through MUEP."]
   [:div "The source is free, open, and available on "
    [:a {:href "https://github.com/motform/multiverse/"} "Github"]
    " under a GPL-3 license."]
   [:div {:style {:padding-bottom 0}} "Love Lagerkvist"]])
