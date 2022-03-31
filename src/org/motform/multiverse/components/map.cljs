(ns org.motform.multiverse.components.map
  (:require [cljsjs.d3]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [reagent.dom :as rdom]))

(defn link-class [{:keys [active-path active-sentence prospective-child? source]
                   {highlight :id} :highlight}]
  (fn [link]
    (let [target-id (.. link -target -data -name)
          source-id (.. link -source -data -name)
          has-children? (seq (.. link -target -data -children))
          ^string personality (.. link -target -data -personality)]
      (str personality "-"
           (cond (active-path target-id) "tree-map-link-active"
                 has-children?           "tree-map-link"
                 (= highlight source-id) "tree-map-link-prospective"
                 (and (= source :source/compare) (= active-sentence source-id)) "hidden"
                 (and (= active-sentence source-id) (prospective-child? highlight)) "tree-map-link-prospective"
                 (and highlight (prospective-child? target-id)) "hidden"
                 (prospective-child? target-id) "tree-map-link-prospective"
                 (= active-sentence source-id)  "tree-map-link"
                 :else "hidden")))))

;; TODO this size should probably be set dynamically in response to the amount of nodes in the graph
(defn node-size [root active source]
  (fn [node]
    (let [id (.. node  -data -name)
          scale (if (= source :source/story) 4 2)]
      (if (or (= root id) (= active id))
        (* scale 2)
        scale))))

(defn node-class [{:keys [active-path active-sentence root-sentence prospective-child? source]
                   {highlight :id} :highlight}]
  (fn [node]
    (let [id (.. node  -data -name)
          parent-id (.. node -data -info)
          ^string personality (.. node -data -personality)]
      (str personality "-"
           (cond (= active-sentence id) (cond (not highlight)               "tree-map-node-current"
                                              (= highlight active-sentence) "tree-map-node-current"
                                              (contains? active-path id)    "tree-map-node-current-superseded"
                                              :else "tree-map-node-current-dim")
                 (= highlight id)                "tree-map-node-highlight"
                 (= root-sentence id)            "tree-map-node-root"
                 (contains? active-path id)      "tree-map-node-active"
                 (seq (.. node -data -children)) "tree-map-node-inactive"
                 (and (= active-sentence highlight) (prospective-child? id))        "tree-map-node-prospective" ; hover on child
                 (and (= active-sentence parent-id) (prospective-child? highlight)) "tree-map-node-prospective" ; hover on parent
                 (and (= source :source/compare) (= active-sentence parent-id))  "hidden"
                 (and highlight (prospective-child? id)) "hidden"
                 (or (= parent-id highlight) (prospective-child? id)) "tree-map-node-prospective"
                 :else "hidden")))))

(defn draw-radial-map
  "Based on: https://medium.com/analytics-vidhya/creating-a-radial-tree-using-d3-js-for-javascript-be943e23b74e"
  [node {:keys [sentence-tree active-sentence root-sentence source dimensions] :as props} map-id]
  (.. js/d3 (select (str "#g-" map-id)) remove) ; clear old image
  (let [link-class (link-class props)
        node-class (node-class props)

        w (:w dimensions (.-clientWidth node))
        h (:h dimensions (.-clientHeight node))

        svg (.. js/d3 (select (str "#" map-id))
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
                        (attr "id" (str "g-" map-id))
                        (attr "transform" (str "translate(" (/ w 2) "," (/ h 2) ")")))

        links (.. graph-group (selectAll ".link")
                  (data links)
                  (join "path")
                  (attr "class" link-class)
                  (attr "d" radial-link))

        nodes (.. graph-group (selectAll ".node")
                  (data nodes)
                  (join "g")
                  (attr "class" node-class)
                  (attr "transform" #(str "rotate(" (- (/ (* (.-x %) 180) Math/PI) 90) ") " "translate(" (.-y %) ", 0)"))
                  (on "pointerover" #(rf/dispatch [:sentence/highlight (.. %2 -data -name) :source/map]))
                  (on "pointerout"  #(rf/dispatch [:sentence/remove-highlight]))
                  (on "pointerdown" #(rf/dispatch [:sentence/active (.. %2 -data -name)]))
                  (append "circle")
                  (attr "r" (node-size root-sentence active-sentence source)))]))

(defn redraw [map-id]
  (fn [this]
    (draw-radial-map (rdom/dom-node this) (r/props this) map-id)))

(defn radial-map-d3 []
  (let [map-id (str "mapid-" (random-uuid))]
    (r/create-class
     {:display-name         (str "radial-tree-map-" map-id)
      :component-did-mount  (redraw map-id)
      :component-did-update (redraw map-id)
      :reagent-render       (fn []
                              [:section.radial-map
                               [:svg {:id map-id}]])})))

(defn radial-map [source story-id settings dimensions]
  (fn []
    [radial-map-d3 (merge {:dimensions         dimensions
                           :sentence-tree      @(rf/subscribe [:story/sentence-tree story-id])
                           :active-path        @(rf/subscribe [:story/active-path story-id])
                           :active-sentence    @(rf/subscribe [:sentence/active story-id])
                           :highlight          @(rf/subscribe [:sentence/highlight])
                           :source             source
                           :prospective-child? (->> @(rf/subscribe [:sentence/children @(rf/subscribe [:sentence/active story-id]) story-id])
                                                    (map :sentence/id)
                                                    set)
                           :root-sentence      @(rf/subscribe [:story/root-sentence story-id])}
                          settings)]))
