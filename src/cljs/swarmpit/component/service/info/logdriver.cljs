(ns swarmpit.component.service.info.logdriver
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

(rum/defc form < rum/static [{:keys [name opts]}]
  [:div.form-layout-group.form-layout-group-border
   (form/section "Logging")
   (form/item "DRIVER" (or name "none"))
   (when (not-empty opts)
     [:div
      (form/subsection "Log driver options")
      (list/table headers
                  opts
                  render-item
                  render-item-keys
                  nil)])])
