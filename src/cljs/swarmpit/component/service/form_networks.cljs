(ns swarmpit.component.service.form-networks
  (:require [material.component :as comp]
            [material.component.form :as form]
            [material.component.list-table-form :as list]
            [swarmpit.component.state :as state]
            [swarmpit.component.handler :as handler]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form :networks])

(defonce networks-list (atom []))

(defn networks-handler
  []
  (handler/get
    (routes/path-for-backend :networks)
    {:on-success (fn [response]
                   (reset! networks-list
                           (->> response
                                (filter #(= "swarm" (:scope %)))
                                (into []))))}))

(def headers [{:name  "Name"
               :width "20%"}
              {:name  "Driver"
               :width "20%"}
              {:name  "Subnet"
               :width "20%"}
              {:name  "Gateway"
               :width "20%"}
              {:name  ""
               :width "20%"}])

(defn- form-network [value index networks-list]
  (list/selectfield
    {:name     (str "form-network-select-" index)
     :key      (str "form-network-select-" index)
     :value    value
     :onChange (fn [_ _ v]
                 (state/update-item index :networkName v cursor))}
    (->> networks-list
         (map #(comp/menu-item
                 {:name        (str "form-network-item-" (:networkName %))
                  :key         (str "form-network-item-" (:networkName %))
                  :value       (:networkName %)
                  :primaryText (:networkName %)})))))

(defn- render-networks
  [item index data]
  (let [{:keys [networkName]} item]
    [(form-network networkName index data)]))

(defn- form-table
  [networks networks-list]
  (list/table-headless [{:name  "Name"
                         :width "300px"}]
                       networks
                       networks-list
                       render-networks
                       (fn [index] (state/remove-item index cursor))))

(defn- add-item
  []
  (state/add-item {:networkName ""} cursor))

(rum/defc form < rum/reactive []
  (let [networks (state/react cursor)]
    (if (empty? networks)
      (form/value "Service is not connected to any networks.")
      (form-table networks (rum/react networks-list)))))