(ns swarmpit.component.service.info.logdriver
  (:require [material.component :as comp]
            [rum.core :as rum]))

(enable-console-print!)

(def headers ["Name" "Value"])

(def render-item-keys
  [[:name] [:value]])

(defn render-item
  [item]
  (val item))

(rum/defc form < rum/static [{:keys [name opts]}]
  [:div.form-service-view-group.form-service-group-border
   (comp/form-section "Log Driver")
   (comp/form-item "DRIVER NAME" (or name "none"))
   (when (not-empty opts)
     [:div
      (comp/form-subsection "Log driver options")
      (comp/list-table-auto headers
                            opts
                            render-item
                            render-item-keys
                            nil)])])
