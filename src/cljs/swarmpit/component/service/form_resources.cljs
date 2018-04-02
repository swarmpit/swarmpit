(ns swarmpit.component.service.form-resources
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [swarmpit.component.parser :refer [parse-int parse-float]]
            [swarmpit.component.state :as state]
            [sablono.core :refer-macros [html]]
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
  (form/comp
    (str "CPU  " "(" (cpu-value value) ")")
    (comp/slider {:min          0
                  :max          2
                  :step         0.10
                  :defaultValue 0
                  :value        value
                  :onChange     (fn [_ v]
                                  (state/update-value [:reservation :cpu] (parse-float v) form-value-cursor))
                  :sliderStyle  {:marginTop "14px"}})))

(defn- form-memory-reservation [value]
  (form/comp
    "MEMORY (MB)"
    (comp/vtext-field
      {:name            "memory-reservation"
       :key             "memory-reservation"
       :type            "number"
       :min             4
       :validations     "isValidMemoryValue"
       :validationError "Please use minimum of 4 MB or leave blank for unlimited"
       :value           value
       :onChange        (fn [_ v]
                          (state/update-value [:reservation :memory] (parse-int v) form-value-cursor))})))

(defn- form-cpu-limit [value]
  (form/comp
    (str "CPU  " "(" (cpu-value value) ")")
    (comp/slider {:min          0
                  :max          2
                  :step         0.10
                  :defaultValue 0
                  :value        value
                  :onChange     (fn [_ v]
                                  (state/update-value [:limit :cpu] (parse-float v) form-value-cursor))
                  :sliderStyle  {:marginTop "14px"}})))

(defn- form-memory-limit [value]
  (form/comp
    "MEMORY (MB)"
    (comp/vtext-field
      {:name            "memory-limit"
       :key             "memory-limit"
       :type            "number"
       :min             4
       :validations     "isValidMemoryValue"
       :validationError "Please use minimum of 4 MB or leave blank for unlimited"
       :value           value
       :onChange        (fn [_ v]
                          (state/update-value [:limit :memory] (parse-int v) form-value-cursor))})))

(rum/defc form < rum/reactive []
  (let [{:keys [reservation limit]} (state/react form-value-cursor)]
    [:div.form-edit
     (form/form
       {:onValid   #(state/update-value [:valid?] true form-state-cursor)
        :onInvalid #(state/update-value [:valid?] false form-state-cursor)}
       (html (form/subsection "Reservation"))
       (html (form/icon-value icon/info [:span "Minimal resource availability to run a task. Empty for unlimited."]))
       (form-cpu-reservation (:cpu reservation))
       (form-memory-reservation (:memory reservation))
       (html (form/subsection "Limit"))
       (html (form/icon-value icon/info [:span "Maximal resource usage per task. Empty for unlimited."]))
       (form-cpu-limit (:cpu limit))
       (form-memory-limit (:memory limit))
       (html [:div {:style {:height "20px"}}]))]))
