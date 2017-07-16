(ns swarmpit.component.service.form-deployment-placement
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :service :wizard :deployment :placement])

(def headers [{:name  "Rule"
               :width "500px"}])

(defn- form-placement [value index placement]
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
                      :dataSource    placement}))

(defn- render-placement
  [item index data]
  (let [{:keys [rule]} item]
    [(form-placement rule index data)]))

(defn- form-table
  [placement data]
  (comp/form-table-headless headers
                            placement
                            data
                            render-placement
                            (fn [index] (state/remove-item index cursor))))

(defn- add-item
  []
  (state/add-item {:rule ""} cursor))

(def render-item-keys
  [[:rule]])

(defn- render-item
  [item]
  (val item))

(rum/defc form < rum/reactive [data]
  (let [placement (state/react cursor)]
    [:div
     (form-table placement data)]))

(rum/defc form-view < rum/static [placement]
  (comp/form-info-table-headless headers
                                 placement
                                 render-item
                                 render-item-keys))
