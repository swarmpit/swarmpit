(ns swarmpit.component.service.info.logdriver
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.list.basic :as list]
            [swarmpit.routes :as routes]
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
       :title     "Log driver options"
       :action    (comp/icon-button
                    {:aria-label "Edit"
                     :href       (routes/path-for-frontend
                                   :service-edit
                                   {:id service-id}
                                   {:section "Logging"})}
                    (comp/svg icon/edit))})
    (comp/card-content
      {:className "Swarmpit-table-card-content"
       :key       "sldcc"}
      (rum/with-key
        (list/list
          render-metadata
          opts
          nil) "sldccl"))))