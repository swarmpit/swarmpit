(ns swarmpit.component.service.info.ports
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [material.component.list.basic :as list]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def render-metadata
  {:table {:summary [{:name      "Container port"
                      :render-fn (fn [item] (:containerPort item))}
                     {:name      "Protocol"
                      :render-fn (fn [item] (:protocol item))}
                     {:name      "Mode"
                      :render-fn (fn [item] (:mode item))}
                     {:name      "Host port"
                      :render-fn (fn [item] (:hostPort item))}]}
   :list  {:primary   (fn [item] (str (:hostPort item) ":" (:containerPort item)))
           :secondary (fn [item] (:protocol item))}})

(rum/defc form < rum/static [ports service-id immutable?]
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :title     (comp/typography {:variant "h6"} "Ports")
       :action    (comp/icon-button
                    {:aria-label "Edit"
                     :disabled   immutable?
                     :href       (routes/path-for-frontend
                                   :service-edit
                                   {:id service-id}
                                   {:section 1})}
                    (comp/svg icon/edit-path))})
    (if (empty? ports)
      (comp/card-content
        {}
        (form/item-info "No ports exposed for the service."))
      (comp/card-content
        {:className "Swarmpit-table-card-content"}
        (list/responsive
          render-metadata
          ports
          nil)))))
