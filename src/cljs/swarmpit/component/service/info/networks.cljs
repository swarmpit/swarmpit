(ns swarmpit.component.service.info.networks
  (:require [material.component.form :as form]
            [material.component.list-table-auto :as list]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def headers ["Name" "Driver" "Subnet" "Gateway" ""])

(def render-item-keys
  [[:networkName] [:driver] [:ipam :subnet] [:ipam :gateway] [:internal]])

(defn render-item
  [item _]
  (val item))

(defn onclick-handler
  [item]
  (routes/path-for-frontend :network-info {:id (:networkName item)}))

(rum/defc form < rum/static [networks]
  (when (not-empty networks)
    [:div.form-layout-group.form-layout-group-border
     (form/section "Networks")
     (list/table headers
                 networks
                 render-item
                 render-item-keys
                 onclick-handler)]))
