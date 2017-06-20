(ns swarmpit.component.network.list
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :network :list])

(def headers ["Name" "Driver" "Subnet" "Gateway" ""])

(defn- filter-items
  "Filter list items based on given predicate"
  [items predicate]
  (filter #(string/includes? (:networkName %) predicate) items))

(def render-item-keys
  [[:networkName] [:driver] [:ipam :subnet] [:ipam :gateway] [:internal]])

(defn- render-item
  [item _]
  (let [value (val item)]
    (case (key item)
      :internal (if value
                  (comp/label-blue "internal"))
      value)))

(defn- onclick-handler
  [item]
  (routes/path-for-frontend :network-info (select-keys item [:id])))

(rum/defc network-list < rum/reactive [items]
  (let [{{:keys [networkName]} :filter} (state/react cursor)
        filtered-items (filter-items items networkName)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-text-field
         {:hintText "Filter by name"
          :onChange (fn [_ v]
                      (state/update-value [:filter :networkName] v cursor))})]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href    (routes/path-for-frontend :network-create)
            :label   "New network"
            :primary true}))]]
     (comp/list-table headers
                      (sort-by :networkName filtered-items)
                      render-item
                      render-item-keys
                      onclick-handler)]))

(defn- init-state
  []
  (state/set-value {:filter {:networkName ""}} cursor))

(defn mount!
  [items]
  (init-state)
  (rum/mount (network-list items) (.getElementById js/document "content")))