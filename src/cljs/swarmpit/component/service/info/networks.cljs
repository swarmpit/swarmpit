(ns swarmpit.component.service.info.networks
  (:require [material.components :as comp]
            [material.component.list.basic :as list]
            [swarmpit.component.network.list :as networks]
            [rum.core :as rum]))

(enable-console-print!)

(rum/defc form < rum/static [networks]
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :title     "Networks"})
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (list/responsive
        networks/render-metadata
        networks
        networks/onclick-handler))))
