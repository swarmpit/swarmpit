(ns swarmpit.component.service.info.ports
  (:require [material.components :as comp]
            [material.component.list.basic :as list]
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

(rum/defc form < rum/static [ports]
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :title     "Ports"})
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (list/responsive
        render-metadata
        ports
        nil))))
