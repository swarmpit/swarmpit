(ns swarmpit.component.service.form-networks
  (:require [material.component :as comp]
            [material.component.form :as form]
            [material.component.list-table-form :as list]
            [swarmpit.component.state :as state]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def form-value-cursor (conj state/form-value-cursor :networks))

(def form-state-cursor (conj state/form-state-cursor :networks))

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
                 (state/update-item index :networkName v form-value-cursor))}
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
                       (fn [index] (state/remove-item index form-value-cursor))))

(defn- add-item
  []
  (state/add-item {:networkName ""} form-value-cursor))

(defn networks-handler
  []
  (ajax/get
    (routes/path-for-backend :networks)
    {:on-success (fn [{:keys [response]}]
                   (let [resp (->> response
                                   (filter #(= "swarm" (:scope %)))
                                   (into []))]
                     (state/update-value [:list] resp form-state-cursor)))}))

(rum/defc form < rum/reactive []
  (let [{:keys [list]} (state/react form-state-cursor)
        networks (state/react form-value-cursor)]
    (if (empty? networks)
      (form/value "Service is not connected to any networks.")
      (form-table networks list))))