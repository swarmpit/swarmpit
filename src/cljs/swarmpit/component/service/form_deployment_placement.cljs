(ns swarmpit.component.service.form-deployment-placement
  (:require [material.component :as comp]
            [material.component.list-table-form :as list]
            [swarmpit.component.state :as state]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form :deployment :placement])

(defonce placement-list (atom []))

(defn placement-handler
  []
  (ajax/get
    (routes/path-for-backend :placement)
    {:on-success (fn [response]
                   (reset! placement-list response))}))

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
                                       (state/update-item index :rule v cursor))
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
                       (fn [index] (state/remove-item index cursor))))

(defn- add-item
  []
  (state/add-item {:rule ""} cursor))

(rum/defc form < rum/reactive []
  (let [placement (state/react cursor)]
    (form-table placement (rum/react placement-list))))
