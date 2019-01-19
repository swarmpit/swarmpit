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
       :title     (comp/typography {:variant "h6"} "Logging")
       :action    (comp/icon-button
                    {:aria-label "Edit"
                     :href       (routes/path-for-frontend
                                   :service-edit
                                   {:id service-id}
                                   {:section "Logging"})}
                    (comp/svg icon/edit-path))})
    (comp/card-content
      {}
      (html
        [:div {:style {:maxWidth "200px"}} (form/item "Driver" name)]))
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (list/list
        render-metadata
        opts
        nil))))