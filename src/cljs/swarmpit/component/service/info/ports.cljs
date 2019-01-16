(ns swarmpit.component.service.info.ports
  (:require [material.icon :as icon]
            [material.components :as comp]
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

(rum/defc form < rum/static [ports service-id]
  (comp/card
    {:className "Swarmpit-card"
     :key       "spc"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :key       "spch"
       :title     (comp/typography
                    {:variant "h6"
                     :key     "ports-title"} "Ports")
       :action    (comp/icon-button
                    {:aria-label "Edit"
                     :href       (routes/path-for-frontend
                                   :service-edit
                                   {:id service-id}
                                   {:section "Ports"})}
                    (comp/svg icon/edit-path))})
    (if (empty? ports)
      (comp/card-content
        {:key "spcce"}
        (html [:div "No ports exposed for the service."]))
      (comp/card-content
        {:className "Swarmpit-table-card-content"
         :key       "spcc"}
        (rum/with-key
          (list/responsive
            render-metadata
            ports
            nil) "spccrl")))))
