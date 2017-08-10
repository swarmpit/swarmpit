(ns swarmpit.component.service.form-networks
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.component.handler :as handler]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :service :wizard :networks])

(defonce networks (atom []))

(defn networks-handler
  []
  (handler/get
    (routes/path-for-backend :networks)
    {:on-success (fn [response]
                   (reset! networks
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

(def empty-info
  (comp/form-value "Service is not connected to any networks."))

(defn- form-network [value index networks-list]
  (comp/form-list-selectfield
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
  (comp/form-table-headless [{:name  "Name"
                              :width "300px"}]
                            networks
                            networks-list
                            render-networks
                            (fn [index] (state/remove-item index cursor))))

(defn- add-item
  []
  (state/add-item {:networkName ""} cursor))

(rum/defc form-create < rum/reactive []
  (let [networks-list (rum/react networks)
        networks (state/react cursor)]
    [:div
     (comp/form-add-btn "Attach network" add-item)
     (if (not (empty? networks))
       (form-table networks networks-list))]))

(rum/defc form-update < rum/reactive []
  (let [networks-list (rum/react networks)
        networks (state/react cursor)]
    (if (empty? networks)
      empty-info
      (form-table networks networks-list))))