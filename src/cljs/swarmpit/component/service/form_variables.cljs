(ns swarmpit.component.service.form-variables
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form :service :variables])

(def state-item {:name  ""
                 :value ""})

(def form-headers ["Name" "Value"])

(defn- form-name [value index]
  (comp/table-row-column
    {:key (str "name" index)}
    (comp/text-field
      {:id       "name"
       :style    {:width "100%"}
       :value    value
       :onChange (fn [e v]
                   (state/update-item index :name v cursor))})))

(defn- form-value [value index]
  (comp/table-row-column
    {:key (str "value" index)}
    (comp/text-field
      {:id       "value"
       :style    {:width "100%"}
       :value    value
       :onChange (fn [e v]
                   (state/update-item index :value v cursor))})))

(rum/defc form < rum/reactive []
  (let [variables (state/react cursor)]
    (comp/mui
      (comp/table
        {:selectable false}
        (comp/table-header-form form-headers #(state/add-item state-item cursor))
        (comp/table-body
          {:displayRowCheckbox false}
          (map-indexed
            (fn [index item]
              (let [{:keys [name
                            value]} item]
                (comp/table-row-form
                  index
                  [(form-name name index)
                   (form-value value index)]
                  #(state/remove-item index cursor))))
            variables))))))
