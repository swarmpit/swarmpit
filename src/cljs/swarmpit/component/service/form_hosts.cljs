(ns swarmpit.component.service.form-hosts
  (:require [material.components :as comp]
            [material.component.form :as form]
            [material.component.list.edit :as list]
            [swarmpit.component.state :as state]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def form-value-cursor (conj state/form-value-cursor :hosts))

(defn- form-name [value index]
  (comp/text-field
    {:fullWidth       true
     :placeholder     "Hostname"
     :key             (str "form-host-name-" index)
     :defaultValue    value
     :required        true
     :variant         "outlined"
     :margin          "dense"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-item index :name (-> % .-target .-value) form-value-cursor)}))

(defn- form-ip [value index]
  (comp/text-field
    {:fullWidth       true
     :placeholder     "IP address"
     :key             (str "form-host-ip-" index)
     :defaultValue    value
     :required        true
     :variant         "outlined"
     :margin          "dense"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-item index :value (-> % .-target .-value) form-value-cursor)}))

(def form-metadata
  [{:name      "Hostname"
    :primary   true
    :key       [:name]
    :render-fn (fn [value _ index] (form-name value index))}
   {:name      "IP address"
    :key       [:value]
    :render-fn (fn [value _ index] (form-ip value index))}])

(defn- form-table
  [hosts]
  (list/list
    form-metadata
    hosts
    (fn [index] (state/remove-item index form-value-cursor))))

(defn add-item
  []
  (state/add-item {:name  ""
                   :value ""} form-value-cursor))

(rum/defc form < rum/reactive []
  (let [hosts (state/react form-value-cursor)]
    (if (empty? hosts)
      (form/item-info "No extra hosts defined for the service.")
      (form-table hosts))))

