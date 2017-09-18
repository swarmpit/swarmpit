(ns swarmpit.component.service.form-resources
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :service :wizard :resources])

(defn- form-cpu-reservation [value]
  (comp/form-comp
    "CPU"
    (comp/text-field
      {:name     "cpu-reservation"
       :key      "cpu-reservation"
       :type     "number"
       :min      0.000
       :step     0.001
       :max      1.0
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:reservation :cpu] (js/parseFloat v) cursor))})))

(defn- form-memory-reservation [value]
  (comp/form-comp
    "MEMORY (MB)"
    (comp/text-field
      {:name     "memory-reservation"
       :key      "memory-reservation"
       :type     "number"
       :min      0
       :max      1024
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:reservation :memory] (js/parseInt v) cursor))})))

(defn- form-cpu-limit [value]
  (comp/form-comp
    "CPU"
    (comp/text-field
      {:name     "cpu-limit"
       :key      "cpu-limit"
       :type     "number"
       :min      0.000
       :step     0.001
       :max      1.0
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:limit :cpu] (js/parseFloat v) cursor))})))

(defn- form-memory-limit [value]
  (comp/form-comp
    "MEMORY (MB)"
    (comp/text-field
      {:name     "memory-limit"
       :key      "memory-limit"
       :type     "number"
       :min      0
       :max      1024
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:limit :memory] (js/parseInt v) cursor))})))

(rum/defc form < rum/reactive []
  (let [{:keys [reservation
                limit]} (state/react cursor)]
    [:div.form-edit
     (comp/form
       {}
       (html (comp/form-subsection "Reservation"))
       (form-cpu-reservation (:cpu reservation))
       (form-memory-reservation (:memory reservation))
       (html (comp/form-subsection "Limit"))
       (form-cpu-limit (:cpu limit))
       (form-memory-limit (:memory limit)))]))
