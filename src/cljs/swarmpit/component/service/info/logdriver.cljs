(ns swarmpit.component.service.info.logdriver
  (:require [material.components :as comp]
            [material.component.list.basic :as list]
            [rum.core :as rum]))

(enable-console-print!)

(def render-metadata
  {:primary   (fn [item] (:name item))
   :secondary (fn [item] (:value item))})

(rum/defc form < rum/static [{:keys [name opts]}]
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :title     "Log driver options"})
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (list/list
        render-metadata
        opts
        nil))))
