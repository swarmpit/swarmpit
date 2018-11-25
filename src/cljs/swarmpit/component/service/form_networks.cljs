(ns swarmpit.component.service.form-networks
  (:require [material.component :as comp]
            [material.component.list.edit :as list]
            [swarmpit.component.state :as state]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def form-value-cursor (conj state/form-value-cursor :networks))

(def form-state-cursor (conj state/form-state-cursor :networks))

(defn- form-network [value index networks-list]
  (comp/text-field
    {:fullWidth       true
     :id              "network"
     :label           "Network"
     :select          true
     :value           value
     :variant         "outlined"
     :margin          "dense"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-item index :networkName (-> % .-target .-value) form-value-cursor)}
    (->> networks-list
         (map #(comp/menu-item
                 {:key   (str "form-network-" (:networkName %))
                  :value (:networkName %)} (:networkName %))))))

(defn- form-ports-metadata [network-list]
  [{:name      "Network"
    :primary   true
    :key       [:networkName]
    :render-fn (fn [value _ index] (form-network value index network-list))}])

(defn- form-table
  [networks networks-list]
  (list/list
    (form-ports-metadata networks-list)
    networks
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
      (html [:div "Service is not connected to any networks."])
      (form-table networks list))))