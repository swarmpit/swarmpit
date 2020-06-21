(ns material.component.composite
  (:require [material.components :as cmp]
            [sablono.core :refer-macros [html]]))

(set! *warn-on-infer* true)

(defn progress-button
  ([action action-fn processing?]
   (progress-button action action-fn processing? false))
  ([action action-fn processing? disabled?]
   (progress-button action action-fn processing? disabled? {}))
  ([action action-fn processing? disabled? opts]
   (html
     [:div.Swarmpit-progress-button-wrapper
      (cmp/button
        (merge
          {:variant  "contained"
           :color    "primary"
           :disabled (or processing? disabled?)
           :onClick  action-fn} opts) action)
      (when processing?
        (cmp/circular-progress
          {:size      24
           :className "Swarmpit-progress-button"}))])))