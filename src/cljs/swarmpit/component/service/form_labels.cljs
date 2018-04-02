(ns swarmpit.component.service.form-labels
  (:require [material.component :as comp]
            [material.component.form :as form]
            [material.component.list-table-form :as list]
            [swarmpit.component.state :as state]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def form-value-cursor (conj state/form-value-cursor :labels))

(def form-state-cursor (conj state/form-state-cursor :labels))

(def headers [{:name  "Name"
               :width "35%"}
              {:name  "Value"
               :width "35%"}])

(defn- form-name [value index label-names]
  (comp/autocomplete
    {:name          (str "form-name-text-" index)
     :key           (str "form-name-text-" index)
     :value         value
     :onUpdateInput #(state/update-item index :name % form-value-cursor)
     :fullWidth     true
     :searchText    value
     :dataSource    label-names}))

(defn- form-value [value index]
  (list/textfield
    {:name     (str "form-value-text-" index)
     :key      (str "form-value-text-" index)
     :value    value
     :onChange (fn [_ v]
                 (state/update-item index :value v form-value-cursor))}))

(defn- render-labels
  [item index data]
  (let [{:keys [name
                value]} item]
    [(form-name name index data)
     (form-value value index)]))

(defn- form-table
  [labels label-names]
  (form/form
    {}
    (list/table-raw headers
                    labels
                    label-names
                    render-labels
                    (fn [index] (state/remove-item index form-value-cursor)))))

(defn- add-item
  []
  (state/add-item {:name  ""
                   :value ""} form-value-cursor))

(defn labels-handler
  []
  (ajax/get
    (routes/path-for-backend :labels-service)
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:names] response form-state-cursor))}))

(rum/defc form < rum/reactive []
  (let [{:keys [names]} (state/react form-state-cursor)
        labels (state/react form-value-cursor)]
    (if (empty? labels)
      (form/value "No labels defined for the service.")
      (form-table labels names))))