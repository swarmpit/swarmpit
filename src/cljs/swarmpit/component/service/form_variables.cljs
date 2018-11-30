(ns swarmpit.component.service.form-variables
  (:require [material.components :as comp]
            [material.component.list.basic :as list]
            [swarmpit.component.state :as state]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def form-value-cursor (conj state/form-value-cursor :variables))

(defn- form-name [value index]
  (comp/text-field
    {:fullWidth       true
     :label           "Name"
     :key             (str "form-variable-name-" index)
     :value           value
     :required        true
     :variant         "outlined"
     :margin          "dense"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-item index :name (-> % .-target .-value) form-value-cursor)}))

(defn- form-value [value index]
  (comp/text-field
    {:fullWidth       true
     :label           "Value"
     :key             (str "form-variable-value-" index)
     :value           value
     :required        true
     :variant         "outlined"
     :margin          "dense"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-item index :value (-> % .-target .-value) form-value-cursor)}))

(def form-metadata
  [{:name      "Name"
    :primary   true
    :key       [:name]
    :render-fn (fn [value _ index] (form-name value index))}
   {:name      "Value"
    :key       [:value]
    :render-fn (fn [value _ index] (form-value value index))}])

(defn- form-table
  [variables]
  (list/list
    form-metadata
    variables
    (fn [index] (state/remove-item index form-value-cursor))))

(defn- add-item
  []
  (state/add-item {:name  ""
                   :value ""} form-value-cursor))

(rum/defc form < rum/reactive []
  (let [variables (state/react form-value-cursor)]
    (if (empty? variables)
      (html [:div "No environment variables defined for the service."])
      (form-table variables))))