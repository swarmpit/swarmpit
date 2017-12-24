(ns swarmpit.component.service.info.labels
  (:require [material.component.form :as form]
            [material.component.list-table-auto :as alist]
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
     (form/section "Labels")
     (alist/table headers
                  labels
                  render-item
                  render-item-keys
                  nil)]))
