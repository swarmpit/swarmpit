(ns swarmpit.component.network.list
  (:require [material.component :as comp]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :network :list])

(def headers [{:name  "Name"
               :width "20%"}
              {:name  "Driver"
               :width "20%"}
              {:name  "Subnet"
               :width "20%"}
              {:name  "Gateway"
               :width "20%"}
              {:name  ""
               :width "20%"}])

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
  (routes/path-for-frontend :network-info {:id (:networkName item)}))

(defn- filter-items
  [items predicate]
  (filter #(string/includes? (:networkName %) predicate) items))

(defn- init-state
  []
  (state/set-value {:filter {:networkName ""}} cursor))

(def init-state-mixin
  (mixin/init
    (fn [_]
      (init-state))))

(rum/defc form < rum/reactive
                 init-state-mixin [items]
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