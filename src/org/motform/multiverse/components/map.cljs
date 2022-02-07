(ns org.motform.multiverse.components.map
  (:require [cljsjs.d3]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [reagent.dom :as rdom]))

(defn draw-radial-map
  "Based on:  https://medium.com/analytics-vidhya/creating-a-radial-tree-using-d3-js-for-javascript-be943e23b74e"
  [node {:keys [active-path sentence-tree active-sentence root-sentence highlight prospective-child?]}]
  (.. js/d3 (select "g") remove) ; clear old image arst
  (let [{highlight :id highlight-source :source} highlight
        w (.-clientWidth node)
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
                  (attr "class" #(cond (active-path (.. % -target -data -name))
                                       "tree-map-link-active"

                                       (seq (.. %  -target -data -children))
                                       "tree-map-link"

                                       (= highlight (.. % -source -data -name))
                                       "tree-map-link-prospective"

                                       ;; should show if: there is an highlight, the highlight is either the parent or the child
                                       (and (= active-sentence (.. % -source -data -name))
                                            (prospective-child? highlight))
                                       "tree-map-link-prospective"

                                       (and highlight (prospective-child? (.. % -target -data -name)))
                                       "hidden"

                                       (prospective-child? (.. % -target -data -name))
                                       "tree-map-link-prospective"

                                       (= active-sentence (.. % -source -data -name))
                                       "tree-map-link"

                                       :else "hidden"))
                  (attr "d" radial-link))

        nodes (.. graph-group (selectAll ".node")
                  (data nodes)
                  (join "g")
                  (attr "class" "tree-map-node")
                  (attr "class" #(let [id (.. %  -data -name)
                                       parent-id (.. % -data -info)] ; the highlight state machine extavaganza!
                                   (cond (= active-sentence id)
                                         (if-not highlight "tree-map-node-current"
                                                 (cond (= highlight active-sentence) "tree-map-node-current"
                                                       (contains? active-path id) "tree-map-node-current-superseded"
                                                       :else "tree-map-node-current-dim"))

                                         (= highlight id)
                                         "tree-map-node-highlight"

                                         (= root-sentence id)
                                         "tree-map-node-root"

                                         (contains? active-path id)
                                         "tree-map-node-active"

                                         (seq (.. %  -data -children))
                                         "tree-map-node-inactive"

                                         (or (and (= active-sentence highlight) (prospective-child? id)) ; hover on child
                                             (and (= active-sentence parent-id) (prospective-child? highlight))) ; hover on parent
                                         "tree-map-node-prospective"

                                         (and highlight (prospective-child? id))
                                         "hidden"

                                         (or (= highlight parent-id) (prospective-child? id))
                                         "tree-map-node-prospective"

                                         :else "hidden")))
                  (attr "transform" #(str "rotate(" (- (/ (* (.-x %) 180) Math/PI) 90) ") " "translate(" (.-y %) ", 0)"))
                  (on "pointerover" #(rf/dispatch [:highlight-sentence (.. %2 -data -name) :source/map]))
                  (on "pointerout"  #(rf/dispatch [:remove-highlight]))
                  (on "pointerdown" #(rf/dispatch [:active-sentence (.. %2 -data -name)]))
                  (append "circle")
                  (attr "r" #(condp = (.. %  -data -name)
                               active-sentence 10
                               root-sentence 10
                               5)))]))

(defn redraw [this]
  (draw-radial-map (rdom/dom-node this) (r/props this)))

(defn radial-map-d3 []
  (r/create-class
   {:display-name         "radial-tree-map"
    :component-did-mount  redraw
    :component-did-update redraw
    :reagent-render       (fn []
                            [:section#radial-map [:svg#radial-map-tree]])}))

(defn radial-map []
  (fn []
    [radial-map-d3 {:sentence-tree   @(rf/subscribe [:sentence-tree])
                    :active-path     @(rf/subscribe [:active-path])
                    :active-sentence @(rf/subscribe [:active-sentence])
                    :highlight       @(rf/subscribe [:highlight])
                    :prospective-child?  (->> @(rf/subscribe [:children @(rf/subscribe [:active-sentence])]) (map :id) set)
                    :root-sentence   @(rf/subscribe [:root-sentence])}]))
