(ns swarmpit.component.service.form-variables
  (:require [swarmpit.material :as material :refer [svg]]
            [swarmpit.utils :refer [remove-el]]
            [rum.core :as rum]))

(enable-console-print!)

(defonce state (atom []))

(def form-headers ["Name" "Value"])

(defn- add-item
  "Create new form item"
  []
  (swap! state
         (fn [p] (conj p {:name  ""
                          :value ""}))))

(defn- remove-item
  "Remove form item"
  [index]
  (swap! state
         (fn [p] (remove-el p index))))

(defn- update-item
  "Update form item configuration"
  [index k v]
  (swap! state
         (fn [p] (assoc-in p [index k] v))))

(defn- form-name [value index]
  (material/table-row-column
    #js {:key index}
    (material/text-field
      #js {:id       "name"
           :value    value
           :onChange (fn [e v] (update-item index :name v))})))

(defn- form-value [value index]
  (material/table-row-column
    #js {:key index}
    (material/text-field
      #js {:id       "value"
           :value    value
           :onChange (fn [e v] (update-item index :value v))})))

(rum/defc form < rum/reactive []
  (let [variables (rum/react state)]
    [:form
     [:fieldset
      [:legend "Environment variables"]
      (material/theme
        (material/table
          #js {:selectable false}
          (material/table-header-form form-headers #(add-item))
          (material/table-body
            #js {:displayRowCheckbox false}
            (for [index (range (count variables))]
              (let [variable (nth variables index)
                    {:keys [name
                            value]} variable]
                (material/table-row-form
                  index
                  [(form-name name index)
                   (form-value value index)]
                  (fn [] (remove-item index))))))))]]))
