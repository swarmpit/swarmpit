(ns swarmpit.component.service.info.labels
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.list.basic :as list]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def render-metadata
  {:primary   (fn [item] (:name item))
   :secondary (fn [item] (:value item))})

(rum/defc form < rum/static [labels service-id]
  (comp/card
    {:className "Swarmpit-card"
     :key       "slc"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :key       "slch"
       :title     "Labels"
       :action    (comp/icon-button
                    {:aria-label "Edit"
                     :href       (routes/path-for-frontend
                                   :service-edit
                                   {:id service-id}
                                   {:section "Labels"})}
                    (comp/svg icon/edit-path))})
    (comp/card-content
      {:className "Swarmpit-table-card-content"
       :key       "slcc"}
      (rum/with-key
        (list/list
          render-metadata
          labels
          nil) "slccl"))))
