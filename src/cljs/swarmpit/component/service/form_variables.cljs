(ns swarmpit.component.service.form-variables
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form :service :variables])

(def headers ["Name" "Value"])

(defn- form-name [value index]
  (comp/table-row-column
    {:key (str "vn-" index)}
    (comp/form-list-textfield
      {:id       "name"
       :value    value
       :onChange (fn [_ v]
                   (state/update-item index :name v cursor))})))

(defn- form-value [value index]
  (comp/table-row-column
    {:key (str "vv-" index)}
    (comp/form-list-textfield
      {:id       "value"
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
                     render-variables
                     (fn [] (state/add-item {:name  ""
                                             :value ""} cursor))
                     (fn [index] (state/remove-item index cursor)))))
