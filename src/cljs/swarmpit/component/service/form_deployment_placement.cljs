(ns swarmpit.component.service.form-deployment-placement
  (:require [material.component :as comp]
            [material.component.list-table-form :as list]
            [swarmpit.component.state :as state]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def form-value-cursor (into state/form-value-cursor [:deployment :placement]))

(def form-state-cursor (conj state/form-state-cursor :placement))

(def headers [{:name  "Rule"
               :width "500px"}])

(defn- form-placement [value index placement-list]
  (comp/autocomplete {:name          "form-placement"
                      :key           "form-placement"
                      :hintText      "e.g. node.role == manager"
                      :anchorOrigin  {:vertical   "top"
                                      :horizontal "left"}
                      :targetOrigin  {:vertical   "bottom"
                                      :horizontal "left"}
                      :fullWidth     true
                      :searchText    value
                      :onUpdateInput (fn [v]
                                       (state/update-item index :rule v form-value-cursor))
                      :dataSource    placement-list}))

(defn- render-placement
  [item index data]
  (let [{:keys [rule]} item]
    [(form-placement rule index data)]))

(defn- form-table
  [placement placement-list]
  (list/table-headless headers
                       placement
                       placement-list
                       render-placement
                       (fn [index] (state/remove-item index form-value-cursor))))

(defn- add-item
  []
  (state/add-item {:rule ""} form-value-cursor))

(defn placement-handler
  []
  (ajax/get
    (routes/path-for-backend :placement)
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:list] response form-state-cursor))}))

(rum/defc form < rum/reactive []
  (let [{:keys [list]} (state/react form-state-cursor)
        placement (state/react form-value-cursor)]
    (form-table placement list)))
