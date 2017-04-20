(ns swarmpit.component.service.form-variables
  (:require [swarmpit.material :as material :refer [svg]]
            [swarmpit.utils :as util]
            [rum.core :as rum]))

(enable-console-print!)

(defonce state (atom []))

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
           :onChange (fn [e v] (util/update-item state index :name v))})))

(defn- form-value [value index]
  (material/table-row-column
    #js {:key (str "value" index)}
    (material/text-field
      #js {:id       "value"
           :style    #js {:width "100%"}
           :value    value
           :onChange (fn [e v] (util/update-item state index :value v))})))

(rum/defc form < rum/reactive []
  (let [variables (rum/react state)]
    (material/theme
      (material/table
        #js {:selectable false}
        (material/table-header-form form-headers #(util/add-item state state-item))
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
                  #(util/remove-item state index))))
            variables))))))
