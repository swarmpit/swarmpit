(ns swarmpit.component.service.form-networks
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :service :wizard :networks])

(def headers ["Name"])

(def undefined
  (comp/form-value "Service is not connected to any networks."))

(defn- form-network [value index data]
  (comp/table-row-column
    {:name (str "form-network-" index)
     :key  (str "form-network-" index)}
    (comp/form-list-selectfield
      {:name     (str "form-network-select-" index)
       :key      (str "form-network-select-" index)
       :value    value
       :onChange (fn [_ _ v]
                   (state/update-item index :networkName v cursor))}
      (->> data
           (map #(comp/menu-item
                   {:name        (str "form-network-item-" (:networkName %))
                    :key         (str "form-network-item-" (:networkName %))
                    :value       (:networkName %)
                    :primaryText (:networkName %)}))))))

(defn- render-networks
  [item index data]
  (let [{:keys [networkName]} item]
    [(form-network networkName index data)]))

(defn- form-table
  [networks data editable?]
  (comp/form-table []
                   networks
                   data
                   editable?
                   render-networks
                   (fn [index] (state/remove-item index cursor))))

(defn add-item
  []
  (state/add-item {:networkName ""} cursor))

(rum/defc form-create < rum/reactive [data]
  (let [networks (state/react cursor)]
    [:div
     (comp/form-add-btn "Attach network" add-item)
     (if (not (empty? networks))
       (form-table networks data true))]))

(rum/defc form-update < rum/reactive [data]
  (let [networks (state/react cursor)]
    (if (empty? networks)
      undefined
      (form-table networks data false))))