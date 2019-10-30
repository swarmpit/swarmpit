(ns swarmpit.component.service.info.networks
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.list.basic :as list]
            [swarmpit.routes :as routes]
            [swarmpit.component.network.list :as networks]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(rum/defc form < rum/static [networks service-id immutable?]
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :title     (comp/typography {:variant "h6"} "Networks")
       :action    (comp/icon-button
                    {:aria-label "Edit"
                     :disabled   immutable?
                     :href       (routes/path-for-frontend
                                   :service-edit
                                   {:id service-id}
                                   {:section "Networks"})}
                    (comp/svg icon/edit-path))})
    (if (empty? networks)
      (comp/card-content
        {}
        (html [:div "No networks attached to the service."]))
      (comp/card-content
        {:className "Swarmpit-table-card-content"}
        (list/responsive
          networks/render-metadata
          networks
          networks/onclick-handler)))))
