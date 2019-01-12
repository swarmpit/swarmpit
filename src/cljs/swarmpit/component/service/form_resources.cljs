(ns swarmpit.component.service.form-resources
  (:require [material.components :as comp]
            [material.component.form :as form]
            [swarmpit.component.parser :refer [parse-int parse-float]]
            [swarmpit.component.state :as state]
            [sablono.core :refer-macros [html]]
            [clojure.walk :refer [keywordize-keys]]
            [rum.core :as rum]))

(enable-console-print!)

(def form-value-cursor (conj state/form-value-cursor :resources))

(def form-state-cursor (conj state/form-state-cursor :resources))

(defn- cpu-value
  [value]
  (if (zero? value)
    "unlimited"
    value))

(defn- form-cpu-reservation [value]
  (html
    [:div {:class "Swarmpit-margin-normal"
           :key   "cpu-reservation-wrap"}
     [:div {:class "Swarmpit-service-slider-title"
            :key   "cpu-reservation-label"}
      (str "CPU  " "(" (cpu-value value) ")")]
     [:div {:key "cpu-reservation-slider"}
      (comp/rc-slider
        {:key          "cpu-reservation"
         :min          0
         :max          2
         :step         0.10
         :defaultValue 0
         :value        value
         :style        {:maxWidth "300px"}
         :onChange     #(state/update-value [:reservation :cpu] (parse-float %) form-value-cursor)})]]))

(defn- form-memory-reservation [value]
  (comp/text-field
    {:label           "Memory"
     :key             "memory-reservation"
     :type            "number"
     :variant         "outlined"
     :margin          "normal"
     :style           {:maxWidth "300px"}
     :helperText      "Use minimum of 4 MB or leave blank for unlimited"
     :min             4
     :fullWidth       true
     :required        true
     :defaultValue    value
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:reservation :memory] (parse-int (-> % .-target .-value)) form-value-cursor)}))

(defn- form-cpu-limit [value]
  (html
    [:div {:class "Swarmpit-margin-normal"
           :key   "cpu-limit-wrap"}
     [:div {:class "Swarmpit-service-slider-title"
            :key   "cpu-limit-label"}
      (str "CPU  " "(" (cpu-value value) ")")]
     [:div {:key "cpu-limit-slider"}
      (comp/rc-slider
        {:key          "cpu-limit"
         :min          0
         :max          2
         :step         0.10
         :defaultValue 0
         :value        value
         :style        {:maxWidth "300px"}
         :onChange     #(state/update-value [:limit :cpu] (parse-float %) form-value-cursor)})]]))

(defn- form-memory-limit [value]
  (comp/text-field
    {:label           "Memory"
     :key             "memory-limit"
     :type            "number"
     :variant         "outlined"
     :margin          "normal"
     :style           {:maxWidth "300px"}
     :helperText      "Use minimum of 4 MB or leave blank for unlimited"
     :min             4
     :fullWidth       true
     :required        true
     :defaultValue    value
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:limit :memory] (parse-int (-> % .-target .-value)) form-value-cursor)}))

(rum/defc form < rum/reactive []
  (let [{:keys [reservation limit]} (state/react form-value-cursor)]
    (comp/grid
      {:container true
       :key       "sfrcg"
       :spacing   40}
      (comp/grid
        {:item true
         :key  "sfrcgir"
         :xs   12
         :sm   6}
        (form/subsection "Reservation")
        ;(html [:div "Minimal resource availability to run a task. Empty for unlimited."])
        (form-memory-reservation (:memory reservation))
        (form-cpu-reservation (:cpu reservation)))
      (comp/grid
        {:item true
         :key  "sfrcgil"
         :xs   12
         :sm   6}
        (form/subsection "Limit")
        ;(html [:div "Maximal resource usage per task. Empty for unlimited."])
        (form-memory-limit (:memory limit))
        (form-cpu-limit (:cpu limit))))))
