(ns swarmpit.component.service.form-variables
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :service :wizard :variables])

(def headers ["Name" "Value"])

(defn- form-name [value index]
  (comp/table-row-column
    {:name (str "form-name-" index)
     :key  (str "form-name-" index)}
    (comp/form-list-textfield
      {:name     (str "form-name-text-" index)
       :key      (str "form-name-text-" index)
       :value    value
       :onChange (fn [_ v]
                   (state/update-item index :name v cursor))})))

(defn- form-value [value index]
  (comp/table-row-column
    {:name (str "form-value-" index)
     :key  (str "form-value-" index)}
    (comp/form-list-textfield
      {:name     (str "form-value-text-" index)
       :key      (str "form-value-text-" index)
       :value    value
       :onChange (fn [_ v]
                   (state/update-item index :value v cursor))})))

(defn- render-variables
  [item index]
  (let [{:keys [name
                value]} item]
    [(form-name name index)
     (form-value value index)]))

(rum/defc form < rum/reactive []
  (let [variables (state/react cursor)]
    (comp/form-table headers
                     variables
                     nil
                     true
                     render-variables
                     (fn [] (state/add-item {:name  ""
                                             :value ""} cursor))
                     (fn [index] (state/remove-item index cursor)))))
