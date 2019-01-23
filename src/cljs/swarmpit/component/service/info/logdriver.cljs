(ns swarmpit.component.service.info.logdriver
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

(rum/defc form < rum/static [{:keys [name opts]} service-id]
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :title     (comp/typography {:variant "h6"} "Log driver")
       :action    (comp/icon-button
                    {:aria-label "Edit"
                     :href       (routes/path-for-frontend
                                   :service-edit
                                   {:id service-id}
                                   {:section "Log driver"})}
                    (comp/svg icon/edit-path))})
    (comp/card-content
      {}
      name)
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (when (not-empty opts)
        [(comp/divider {})
         (list/list
           render-metadata
           opts
           nil)]))))