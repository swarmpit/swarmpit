(ns material.component.chart
  (:require [material.components :as comp]
            [sablono.core :refer-macros [html]]))

(defn- label-fill []
  (if (= "dark" (comp/current-theme-mode)) "#e0e0e0" "#333"))

(defn pie [data label className id tooltip]
  (let [hashd (hash data)]
    (html
      [:div {:className className
             :key       (str "pie-wrapper-" id "-" hashd)}
       (comp/responsive-container
         {}
         (comp/pie-chart
           {}
           (comp/pie
             {:key               (str "pie-" id)
              :data              data
              :dataKey           "value"
              :isAnimationActive false
              :innerRadius       "80%"
              :outerRadius       "100%"
              :startAngle        90
              :endAngle          -270
              :fill              "#8884d8"
              :stroke            "none"}
             (map-indexed
               (fn [index item]
                 (comp/cell {:fill (:color item)
                             :key  (str "pie-cell-" id "-" index)})) data)
             (comp/re-label
               (merge {:width    30
                       :position "center"
                       :fill     (label-fill)}
                      (when (= "Loading" label)
                        {:fill "#ccc"})) label))
           (when tooltip
             (comp/tooltip-chart tooltip))))])))