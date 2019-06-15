(ns swarmpit.component.plot
  (:require [cljsjs.plotly]))

(defn default [plot-id data layout]
  (js/Plotly.newPlot
    (.getElementById js/document plot-id)
    (clj->js data)
    (clj->js layout)))
