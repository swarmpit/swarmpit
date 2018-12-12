(ns swarmpit.component.service.form-networks
  (:require [swarmpit.component.state :as state]
            [material.component.composite :as composite]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def form-value-cursor (conj state/form-value-cursor :networks))

(def form-state-cursor (conj state/form-state-cursor :networks))

(defn networks-handler
  []
  (ajax/get
    (routes/path-for-backend :networks)
    {:on-success (fn [{:keys [response]}]
                   (let [resp (->> response
                                   (filter #(= "swarm" (:scope %)))
                                   (into []))]
                     (state/update-value [:list] resp form-state-cursor)))}))

(defn- form-network [networks network-list]
  (let [suggestions (map #(hash-map :label (:networkName %)
                                    :value (:networkName %)) network-list)]
    (composite/autocomplete
      {:options        suggestions
       :textFieldProps {:id              "Networks"
                        :label           "Network"
                        :helperText      "Attach to network"
                        :margin          "normal"
                        :InputLabelProps {:shrink true}}
       :onChange       (fn [value]
                         (state/set-value
                           (->> (js->clj value)
                                (map #(hash-map :networkName (get % "value")))) form-value-cursor))
       :value          (map #(hash-map :label (:networkName %)
                                       :value (:networkName %)
                                       :key (:networkName %)) networks)
       :placeholder    "Add network"
       :isMulti        true})))

(rum/defc form < rum/reactive []
  (let [{:keys [list]} (state/react form-state-cursor)
        networks (state/react form-value-cursor)]
    (form-network networks list)))