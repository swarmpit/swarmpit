(ns swarmpit.component.network.list
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :network :list :filter])

(def headers ["Name" "Driver" "Subnet" "Gateway" ""])

(defn- filter-items
  "Filter list items based on given predicate"
  [items predicate]
  (filter #(string/includes? (:networkName %) predicate) items))

(def render-item-keys
  [[:networkName] [:driver] [:ipam :subnet] [:ipam :gateway] [:internal]])

(defn- render-item
  [item]
  (let [value (val item)]
    (case (key item)
      :internal (if value
                  (comp/label-blue "internal"))
      value)))

(rum/defc network-list < rum/reactive [items]
  (let [{:keys [networkName]} (state/react cursor)
        filtered-items (filter-items items networkName)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-text-field
         {:hintText "Filter by name"
          :onChange (fn [_ v]
                      (state/update-value [:networkName] v cursor))})]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href    (routes/path-for-frontend :network-create)
            :label   "Create"
            :primary true}))]]
     (comp/list-table headers
                      filtered-items
                      render-item
                      render-item-keys
                      :network-info)]))

(defn- init-state
  []
  (state/set-value {:networkName ""} cursor))

(defn mount!
  [items]
  (init-state)
  (rum/mount (network-list items) (.getElementById js/document "content")))