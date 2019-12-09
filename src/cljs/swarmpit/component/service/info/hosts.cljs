(ns swarmpit.component.service.info.hosts
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [material.component.list.basic :as list]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def render-metadata
  {:primary   (fn [item] (:name item))
   :secondary (fn [item] (:value item))})

(rum/defc form < rum/static [hosts service-id immutable?]
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :title     (comp/typography {:variant "h6"} "Extra hosts")
       :action    (comp/icon-button
                    {:aria-label "Edit"
                     :disabled   immutable?
                     :href       (routes/path-for-frontend
                                   :service-edit
                                   {:id service-id}
                                   {:section 1})}
                    (comp/svg icon/edit-path))})
    (if (empty? hosts)
      (comp/card-content
        {}
        (form/item-info "No extra hosts defined for the service."))
      (comp/card-content
        {:className "Swarmpit-table-card-content"}
        (list/list
          render-metadata
          hosts
          nil)))))
