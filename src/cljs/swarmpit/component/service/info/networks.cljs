(ns swarmpit.component.service.info.networks
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.list.info :as list]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(defn onclick-handler
  [item]
  (routes/path-for-frontend :network-info {:id (:networkName item)}))

(def render-metadata
  [{:name    "Name"
    :primary true
    :key     [:networkName]}
   {:name "Driver"
    :key  [:driver]}
   {:name "Subnet"
    :key  [:ipam :subnet]}
   {:name "Gateway"
    :key  [:ipam :gateway]}])

(rum/defc form < rum/static [networks]
  (comp/card
    {:className "Swarmpit-form-card"}
    (comp/card-header
      {:className "Swarmpit-form-card-header"
       :subheader (form/subheader "Networks" icon/settings)})
    (comp/card-content
      {}
      (list/table
        render-metadata
        networks
        onclick-handler))))
