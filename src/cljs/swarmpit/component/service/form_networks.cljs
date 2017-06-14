(ns swarmpit.component.service.form-networks
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :service :wizard :networks])

(def headers ["Name"])

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

(rum/defc form < rum/reactive [update-form? data]
  (let [networks (state/react cursor)]
    (comp/form-table headers
                     networks
                     data
                     (not update-form?)
                     render-networks
                     (fn [] (state/add-item {:networkName ""} cursor))
                     (fn [index] (state/remove-item index cursor)))))