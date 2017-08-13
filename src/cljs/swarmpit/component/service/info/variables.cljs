(ns swarmpit.component.service.info.variables
  (:require [material.component :as comp]
            [rum.core :as rum]))

(enable-console-print!)

(def headers ["Name" "Value"])

(def render-item-keys
  [[:name] [:value]])

(defn render-item
  [item]
  (val item))

(rum/defc form < rum/static [variables]
  (when (not-empty variables)
    [:div.form-service-view-group.form-service-group-border
     (comp/form-section "Environment Variables")
     (comp/list-table-auto headers
                           variables
                           render-item
                           render-item-keys
                           nil)]))