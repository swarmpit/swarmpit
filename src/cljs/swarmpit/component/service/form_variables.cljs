(ns swarmpit.component.service.form-variables
  (:require [swarmpit.component.state :as state]
            [swarmpit.material :as material]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form :service :variables])

(def state-item {:name  ""
                 :value ""})

(def form-headers ["Name" "Value"])

(defn- form-name [value index]
  (material/table-row-column
    #js {:key (str "name" index)}
    (material/text-field
      #js {:id       "name"
           :style    #js {:width "100%"}
           :value    value
           :onChange (fn [e v]
                       (state/update-item index :name v cursor))})))

(defn- form-value [value index]
  (material/table-row-column
    #js {:key (str "value" index)}
    (material/text-field
      #js {:id       "value"
           :style    #js {:width "100%"}
           :value    value
           :onChange (fn [e v]
                       (state/update-item index :value v cursor))})))

(rum/defc form < rum/reactive []
  (let [variables (state/react cursor)]
    (material/theme
      (material/table
        #js {:selectable false}
        (material/table-header-form form-headers #(state/add-item state-item cursor))
        (material/table-body
          #js {:displayRowCheckbox false}
          (map-indexed
            (fn [index item]
              (let [{:keys [name
                            value]} item]
                (material/table-row-form
                  index
                  [(form-name name index)
                   (form-value value index)]
                  #(state/remove-item index cursor))))
            variables))))))
