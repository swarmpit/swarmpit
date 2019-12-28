(ns material.component.chartj
  (:require [cljsjs.chartjs]
            [sablono.core :refer-macros [html]]))

(.register
  (.-pluginService js/Chart)
  (clj->js
    {:beforeDraw
     (fn [chart]
       (set! (-> chart .-chart .-ctx .-font) "30px Arial")
       (let [ctx (-> chart .-chart .-ctx)

             ;; Get options from the center object in options
             center-config (-> chart .-config .-options .-elements .-center)
             txt (-> center-config .-text)
             color (or (-> center-config .-color) "#000")
             side-padding (or (-> center-config .-sidePadding) 20)
             side-padding-calc (* (/ side-padding 100) (* (.-innerRadius chart) 2))

             ;; Get the width of the string and also the width of the
             ;; element minus 10 to give it 5px side padding
             string-width (.-width (.measureText ctx txt))
             element-width (- (* (.-innerRadius chart) 2) side-padding-calc)

             ;; Find out how much the font can grow in width
             width-ratio (/ element-width string-width)
             new-font-size (.floor js/Math (* 30 width-ratio))
             element-height (* (.-innerRadius chart) 2)

             ;; Pick a new font size so it will not be larger than
             ;; the height of label
             font-size-to-use (.min js/Math new-font-size element-height)]
         (set! (.-textAlign ctx) "center")
         (set! (.-textBaseline ctx) "middle")
         (set! (.-font ctx) (str font-size-to-use "px Arial"))
         (set! (.-fillStyle ctx) color)
         (.fillText ctx
                    txt
                    (/ (+ (-> chart .-chartArea .-left) (-> chart .-chartArea .-right)) 2)
                    (/ (+ (-> chart .-chartArea .-top) (-> chart .-chartArea .-bottom)) 2))))}))

(defn tooltip
  "Hack to associate different labels with each dataset
   See: https://github.com/chartjs/Chart.js/issues/3953#issuecomment-333340242"
  [tooltip-item data]
  (let [ds-index (.-datasetIndex tooltip-item)
        ds (nth (.-datasets data) ds-index)
        index (.-index tooltip-item)]
    (str " " (nth (.-labels ds) index) ": " (nth (.-data ds) index))))

(defn default
  [chart-id chart-config]
  (js/Chart.
    (.getContext (.getElementById js/document chart-id) "2d")
    (clj->js chart-config)))

(defn doughnut
  [chart-id chart-datasets chart-title]
  (let [datasets (into [] (map (fn [i] {:data            (into [] (map :value i))
                                        :backgroundColor (into [] (map :color i))
                                        :labels          (into [] (map :name i))}) chart-datasets))]
    (default
      chart-id
      {:type    "doughnut"
       :data    {:datasets datasets}
       :options {:responsive          true
                 :maintainAspectRatio false
                 :cutoutPercentage    75
                 :legend              {:display false}
                 :tooltips            {:enabled   false
                                       :callbacks {:label tooltip}}
                 :elements            {:center {:text        chart-title
                                                :sidePadding 15}}}})))

(defn doughnut-canvas
  [chart-id]
  (html
    [:div.Swarmpit-doughnut
     [:canvas {:id chart-id}]]))