(ns swarmpit.component.service.info.variables
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [material.component.list.basic :as list]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]
            [swarmpit.storage :as storage]))

(enable-console-print!)

(def render-metadata
  {:primary   (fn [item] (:name item))
   :secondary (fn [item] (:value item))})

(rum/defc form < rum/static [variables service-id immutable?]
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :title     (comp/typography {:variant "h6"} "Environment variables")
       :action    (if (storage/user?)
                    (comp/icon-button
                      {:aria-label "Edit"
                       :disabled   immutable?
                       :href       (routes/path-for-frontend
                                     :service-edit
                                     {:id service-id}
                                     {:section 2})}
                      (comp/svg icon/edit-path))
                    nil)})
    (if (empty? variables)
      (comp/card-content
        {}
        (form/item-info "No variables defined for the service."))
      (comp/card-content
        {:className "Swarmpit-table-card-content"}
        (list/list
          render-metadata
          variables
          nil)))))