(ns swarmpit.component.service.info.variables
  (:require [material.component :as comp]
            [material.component.list.basic :as list]
            [rum.core :as rum]))

(enable-console-print!)

(def render-metadata
  {:primary   (fn [item] (:name item))
   :secondary (fn [item] (:value item))})

(rum/defc form < rum/static [variables]
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :title     "Environment variables"})
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (list/list
        render-metadata
        variables
        nil))))
