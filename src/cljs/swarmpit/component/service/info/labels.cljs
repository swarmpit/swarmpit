(ns swarmpit.component.service.info.labels
  (:require [material.component :as comp]
            [rum.core :as rum]))

(enable-console-print!)

(def headers ["Name" "Value"])

(def render-item-keys
  [[:name] [:value]])

(defn render-item
  [item]
  (val item))

(rum/defc form < rum/static [labels]
  (when (not-empty labels)
    [:div.form-service-view-group.form-service-group-border
     (comp/form-section "Labels")
     (comp/list-table-auto headers
                           labels
                           render-item
                           render-item-keys
                           nil)]))
