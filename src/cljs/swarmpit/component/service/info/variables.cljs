(ns swarmpit.component.service.info.variables
  (:require [material.component.form :as form]
            [material.component.list-table-auto :as list]
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
    [:div.form-layout-group.form-layout-group-border
     (form/section "Environment Variables")
     (list/table headers
                 variables
                 render-item
                 render-item-keys
                 nil)]))