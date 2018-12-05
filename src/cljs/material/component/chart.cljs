(ns material.component.chart
  (:require [material.components :as comp]
            [sablono.core :refer-macros [html]]))

(defn pie [data label className id tooltip]
  (html
    [:div {:className className
           :key       (str "Pie-" id "-" (hash data))}
     (comp/responsive-container
       (comp/pie-chart
         {}
         (comp/pie
           {:data              data
            :isAnimationActive false
            :cx                "50"
            :innerRadius       "60%"
            :outerRadius       "80%"
            :startAngle        90
            :endAngle          -270
            :fill              "#8884d8"}
           (map #(comp/cell {:fill (:color %)}) data)
           (comp/re-label
             {:width    30
              :position "center"} label))
         (when tooltip
           (comp/tooltip-chart tooltip))))]))


