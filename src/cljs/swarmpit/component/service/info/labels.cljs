(ns swarmpit.component.service.info.labels
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.list.basic :as list]
            [rum.core :as rum]))

(enable-console-print!)

(def render-metadata
  {:primary   (fn [item] (:name item))
   :secondary (fn [item] (:value item))})

(rum/defc form < rum/static [labels]
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :title     "Labels"})
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (list/list
        render-metadata
        labels
        nil))))
