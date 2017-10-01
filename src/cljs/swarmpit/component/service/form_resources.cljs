(ns swarmpit.component.service.form-resources
  (:require [material.component :as comp]
            [swarmpit.utils :refer [parse-int parse-float]]
            [swarmpit.component.state :as state]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :service :wizard :resources])

(defn- form-cpu-reservation [value]
  (comp/form-comp
    "CPU"
    (comp/vtext-field
      {:name            "cpu-reservation"
       :key             "cpu-reservation"
       :type            "number"
       :min             0.000
       :step            0.001
       :max             1.0
       :validations     "isValidCPUValue"
       :validationError "Please use maximum of 1.0 (represents a whole CPU) or leave blank for default value"
       :value           value
       :onChange        (fn [_ v]
                          (state/update-value [:reservation :cpu] (parse-float v) cursor))})))

(defn- form-memory-reservation [value]
  (comp/form-comp
    "MEMORY (MB)"
    (comp/vtext-field
      {:name            "memory-reservation"
       :key             "memory-reservation"
       :type            "number"
       :min             4
       :validations     "isValidMemoryValue"
       :validationError "Please use minimum of 4 MB or leave blank for default value"
       :value           value
       :onChange        (fn [_ v]
                          (state/update-value [:reservation :memory] (parse-int v) cursor))})))

(defn- form-cpu-limit [value]
  (comp/form-comp
    "CPU"
    (comp/vtext-field
      {:name            "cpu-limit"
       :key             "cpu-limit"
       :type            "number"
       :min             0.000
       :step            0.001
       :max             1.0
       :validations     "isValidCPUValue"
       :validationError "Please use maximum of 1.0 (represents a whole CPU) or leave blank for default value"
       :value           value
       :onChange        (fn [_ v]
                          (state/update-value [:limit :cpu] (parse-float v) cursor))})))

(defn- form-memory-limit [value]
  (comp/form-comp
    "MEMORY (MB)"
    (comp/vtext-field
      {:name            "memory-limit"
       :key             "memory-limit"
       :type            "number"
       :min             4
       :validations     "isValidMemoryValue"
       :validationError "Please use minimum of 4 MB or leave blank for default value"
       :value           value
       :onChange        (fn [_ v]
                          (state/update-value [:limit :memory] (parse-int v) cursor))})))

(rum/defc form < rum/reactive []
  (let [{:keys [reservation
                limit]} (state/react cursor)]
    [:div.form-edit
     (comp/form
       {:onValid   #(state/update-value [:isValid] true cursor)
        :onInvalid #(state/update-value [:isValid] false cursor)}
       (html (comp/form-subsection "Reservation"))
       (form-cpu-reservation (:cpu reservation))
       (form-memory-reservation (:memory reservation))
       (html (comp/form-subsection "Limit"))
       (form-cpu-limit (:cpu limit))
       (form-memory-limit (:memory limit))
       (html [:div {:style {:height "20px"}}]))]))
