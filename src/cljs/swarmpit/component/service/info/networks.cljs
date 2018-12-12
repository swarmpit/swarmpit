(ns swarmpit.component.service.info.networks
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.list.basic :as list]
            [swarmpit.routes :as routes]
            [swarmpit.component.network.list :as networks]
            [rum.core :as rum]))

(enable-console-print!)

(rum/defc form < rum/static [networks service-id]
  (comp/card
    {:className "Swarmpit-card"
     :key       "snc"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :key       "snch"
       :title     "Networks"
       :action    (comp/icon-button
                    {:aria-label "Edit"
                     :href       (routes/path-for-frontend
                                   :service-edit
                                   {:id service-id}
                                   {:section "Networks"})}
                    (comp/svg icon/edit))})
    (comp/card-content
      {:className "Swarmpit-table-card-content"
       :key       "sncc"}
      (rum/with-key
        (list/responsive
          networks/render-metadata
          networks
          networks/onclick-handler) "snccrl"))))
