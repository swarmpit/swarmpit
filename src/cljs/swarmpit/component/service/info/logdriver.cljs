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
    {:className "Swarmpit-card"
     :key       "sldc"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :key       "sldch"
       :title     (comp/typography
                    {:variant "h6"
                     :key     "logging-title"} "Logging")
       :action    (comp/icon-button
                    {:aria-label "Edit"
                     :href       (routes/path-for-frontend
                                   :service-edit
                                   {:id service-id}
                                   {:section "Logging"})}
                    (comp/svg icon/edit-path))})
    (comp/card-content
      {:key "sldcci"}
      (html
        [:div {:style {:maxWidth "200px"}} (form/item "Driver" name)]))
    (comp/card-content
      {:className "Swarmpit-table-card-content"
       :key       "sldcc"}
      (rum/with-key
        (list/list
          render-metadata
          opts
          nil) "sldccl"))))