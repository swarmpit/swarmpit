(ns swarmpit.component.service.info.ports
  (:require [material.component.form :as form]
            [material.component.list-table-auto :as list]
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
    [:div.form-layout-group.form-layout-group-border
     (form/section "Ports")
     (list/table headers
                 ports
                 render-item
                 render-item-keys
                 nil)]))
