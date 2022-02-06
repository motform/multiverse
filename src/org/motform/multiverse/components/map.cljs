(ns org.motform.multiverse.components.map
  (:require [cljsjs.d3]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [reagent.dom :as rdom]))

(defn redraw-radial-map
  "Based on:  https://medium.com/analytics-vidhya/creating-a-radial-tree-using-d3-js-for-javascript-be943e23b74e"
  [node {:keys [active-path sentence-tree active-sentence root-sentence]}]
  (.. js/d3 (select "g") remove) ; clear old image arst
  (let [w (.-clientWidth node)
        h (.-clientHeight node)

        svg (.. js/d3 (select "#radial-map-tree")
                (attr "width" w)
                (attr "height" h))

        tree (.. js/d3 tree
                 (size #js [(* Math/PI 2) (/ (* h 0.9) 2)])
                 (separation (fn [a b] (/ (if (= (.-parent a) (.-parent b)) 1 2)
                                          (.-depth a)))))

        tree-data (tree (.hierarchy js/d3 (clj->js sentence-tree)))
        nodes     (.descendants tree-data)
        links     (.links tree-data)

        radial-link (.. js/d3 linkRadial
                        (angle  #(.-x %))
                        (radius #(.-y %)))

        graph-group (.. svg (append "g")
                        (attr "transform" (str "translate(" (/ w 2) "," (/ h 2) ")")))

        links (.. graph-group (selectAll ".link")
                  (data links)
                  (join "path")
                  (attr "class" #(if (active-path (.. % -target -data -name))
                                   "radial-map-link-active" "radial-map-link"))
                  (attr "d" radial-link))

        nodes (.. graph-group (selectAll ".node")
                  (data nodes)
                  (join "g")
                  (attr "class" "tree-map-node")
                  (attr "class" #(let [id (.. %  -data -name)]
                                   (cond (= root-sentence id)       "tree-map-node-root"
                                         (= active-sentence id)     "tree-map-node-current"
                                         (contains? active-path id) "tree-map-node-active"
                                         :else                      "tree-map-node-inactive")))
                  (attr "transform" #(str "rotate(" (- (/ (* (.-x %) 180) Math/PI) 90) ") " "translate(" (.-y %) ", 0)"))
                  (on "pointerdown" #(println "hello sailor")) ; TODO use to shift focus
                  (append "circle")
                  (attr "r" #(condp = (.. %  -data -name)
                               root-sentence 12
                               active-sentence 10
                               5)))]))

(defn redraw [this]
  (redraw-radial-map (rdom/dom-node this) (r/props this)))

(defn radial-map-d3 []
  (r/create-class
   {:display-name         "radial-tree-map"
    :component-did-mount  redraw
    :component-did-update redraw
    :reagent-render       (fn [] [:section#radial-map [:svg#radial-map-tree]])}))


(defn radial-map []
  (let [sentence (or @(rf/subscribe [:highlight]) @(rf/subscribe [:active-sentence]))]
    (fn []
      [radial-map-d3 {:sentence-tree   @(rf/subscribe [:sentence-tree])
                      :active-path     (set @(rf/subscribe [:path sentence]))
                      :active-sentence sentence
                      :root-sentence   @(rf/subscribe [:root-sentence])}])))
