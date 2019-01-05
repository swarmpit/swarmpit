(ns material.component.chart
  (:require [material.components :as comp]
            [sablono.core :refer-macros [html]]))

(defn pie [data label className id tooltip]
  (let [hashd (hash data)]
    (html
      [:div {:className className
             :key       (str "p-" id "-" hashd)}
       (comp/responsive-container
         {:key     (str "rc-" id)}
         (comp/pie-chart
           {:key (str "pc-" id)}
           (comp/pie
             {:key               (str "pp-" id)
              :data              data
              :dataKey           "value"
              :isAnimationActive false
              :cx                "50"
              :innerRadius       "60%"
              :outerRadius       "80%"
              :startAngle        90
              :endAngle          -270
              :fill              "#8884d8"}
             (map-indexed
               (fn [index item]
                 (comp/cell {:fill (:color item)
                             :key  (str "pce-" id "-" index)})) data)
             (comp/re-label
               {:key      (str "pl-" id)
                :width    30
                :position "center"} label))
           (when tooltip
             (comp/tooltip-chart
               (merge tooltip
                      {:key (str "pt-" id)})))))])))


