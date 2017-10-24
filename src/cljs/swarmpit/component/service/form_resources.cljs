(ns swarmpit.component.service.form-resources
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.component.parser :refer [parse-int parse-float]]
            [swarmpit.component.state :as state]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :service :wizard :resources])

(defn- cpu-value
  [value]
  (if (zero? value)
    "unlimited"
    value))

(defn- form-cpu-reservation [value]
  (comp/form-comp
    (str "CPU  " "(" (cpu-value value) ")")
    (comp/slider #js {:min          0
                      :max          2
                      :step         0.10
                      :defaultValue 0
                      :value        value
                      :onChange     (fn [_ v]
                                      (state/update-value [:reservation :cpu] (parse-float v) cursor))
                      :sliderStyle  #js {:marginTop "14px"}})))

(defn- form-memory-reservation [value]
  (comp/form-comp
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
                          (state/update-value [:reservation :memory] (parse-int v) cursor))})))

(defn- form-cpu-limit [value]
  (comp/form-comp
    (str "CPU  " "(" (cpu-value value) ")")
    (comp/slider #js {:min          0
                      :max          2
                      :step         0.10
                      :defaultValue 0
                      :value        value
                      :onChange     (fn [_ v]
                                      (state/update-value [:limit :cpu] (parse-float v) cursor))
                      :sliderStyle  #js {:marginTop "14px"}})))

(defn- form-memory-limit [value]
  (comp/form-comp
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
                          (state/update-value [:limit :memory] (parse-int v) cursor))})))

(rum/defc form < rum/reactive []
  (let [{:keys [reservation
                limit]} (state/react cursor)]
    [:div.form-edit
     (comp/form
       {:onValid   #(state/update-value [:isValid] true cursor)
        :onInvalid #(state/update-value [:isValid] false cursor)}
       (html (comp/form-subsection "Reservation"))
       (html (comp/form-icon-value icon/info [:span "Minimal resource availablility to run a task. Empty for unlimited."]))
       (form-cpu-reservation (:cpu reservation))
       (form-memory-reservation (:memory reservation))
       (html (comp/form-subsection "Limit"))
       (html (comp/form-icon-value icon/info [:span "Maximal resource usage per task. Empty for unlimited."]))
       (form-cpu-limit (:cpu limit))
       (form-memory-limit (:memory limit))
       (html [:div {:style {:height "20px"}}]))]))
