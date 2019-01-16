(ns swarmpit.component.service.info.variables
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.list.basic :as list]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def render-metadata
  {:primary   (fn [item] (:name item))
   :secondary (fn [item] (:value item))})

(rum/defc form < rum/static [variables service-id]
  (comp/card
    {:className "Swarmpit-card"
     :key       "svc"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :key       "svch"
       :title     (comp/typography
                    {:variant "h6"
                     :key     "variables-title"} "Environment variables")
       :action    (comp/icon-button
                    {:aria-label "Edit"
                     :href       (routes/path-for-frontend
                                   :service-edit
                                   {:id service-id}
                                   {:section "Environment variables"})}
                    (comp/svg icon/edit-path))})
    (if (empty? variables)
      (comp/card-content
        {:key "svcce"}
        (html [:div "No variables defined for the service."]))
      (comp/card-content
        {:className "Swarmpit-table-card-content"
         :key       "svcc"}
        (rum/with-key
          (list/list
            render-metadata
            variables
            nil) "svccl")))))
