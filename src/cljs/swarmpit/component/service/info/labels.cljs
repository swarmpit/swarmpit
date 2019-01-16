(ns swarmpit.component.service.info.labels
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

(rum/defc form < rum/static [labels service-id]
  (comp/card
    {:className "Swarmpit-card"
     :key       "slc"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :key       "slch"
       :title     (comp/typography
                    {:variant "h6"
                     :key     "labels-title"} "Labels")
       :action    (comp/icon-button
                    {:aria-label "Edit"
                     :href       (routes/path-for-frontend
                                   :service-edit
                                   {:id service-id}
                                   {:section "Labels"})}
                    (comp/svg icon/edit-path))})

    (if (empty? labels)
      (comp/card-content
        {:key "slccle"}
        (html [:div "No labels defined for the service."]))
      (comp/card-content
        {:className "Swarmpit-table-card-content"
         :key       "slcc"}
        (rum/with-key
          (list/list
            render-metadata
            labels
            nil) "slccl")))))
