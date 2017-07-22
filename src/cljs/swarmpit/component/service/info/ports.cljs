(ns swarmpit.component.service.info.ports
  (:require [material.component :as comp]
            [rum.core :as rum]))

(enable-console-print!)

(def headers ["Container port" "Protocol" "Host port"])

(def render-item-keys
  [[:containerPort] [:protocol] [:hostPort]])

(defn render-item
  [item]
  (val item))

(rum/defc form < rum/static [ports]
  (when (not-empty ports)
    [:div.form-service-view-group.form-service-group-border
     (comp/form-section "Ports")
     (comp/list-table-auto headers
                           ports
                           render-item
                           render-item-keys
                           nil)]))
