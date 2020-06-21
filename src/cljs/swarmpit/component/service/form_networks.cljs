(ns swarmpit.component.service.form-networks
  (:require [material.components :as comp]
            [swarmpit.component.state :as state]
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
                                   (filter #(or (= "swarm" (:scope %)) (= "host" (:driver %))))
                                   (into []))]
                     (state/update-value [:list] resp form-state-cursor)))}))

(defn- form-network [networks network-list]
  (comp/autocomplete
    {:id                    "network-autocomplete"
     :multiple              true
     :filterSelectedOptions true
     :value                 networks
     :options               network-list
     :getOptionLabel        (fn [option] (goog.object/get option "networkName"))
     :renderInput           (fn [params]
                              (comp/text-field-js
                                (js/Object.assign
                                  params
                                  #js {:label      "Network"
                                       :fullWidth  true
                                       :margin     "normal"
                                       :variant    "outlined"
                                       :helperText "Attach to network"})))
     :onChange              (fn [e v]
                              (state/set-value
                                (->> (js->clj v)
                                     (map #(hash-map :networkName (get % "networkName")))) form-value-cursor))}))

(rum/defc form < rum/reactive []
  (let [{:keys [list]} (state/react form-state-cursor)
        networks (state/react form-value-cursor)]
    (form-network networks list)))