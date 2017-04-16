(ns swarmpit.component.service.form-volumes
  (:require [swarmpit.material :as material :refer [svg]]
            [swarmpit.utils :refer [remove-el]]
            [rum.core :as rum]))

(enable-console-print!)

(defonce state (atom []))

(def form-headers ["Container path" "Host path" "Read only"])

(defn- add-item
  "Create new form item"
  []
  (swap! state
         (fn [p] (conj p {:containerPath ""
                          :hostPath      ""
                          :readOnly      false}))))

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

(defn- form-container [value index]
  (material/table-row-column
    #js {:key (str "containerPath" index)}
    (material/text-field
      #js {:id       "containerPath"
           :style    #js {:width "100%"}
           :value    value
           :onChange (fn [e v] (update-item index :containerPath v))})))

(defn- form-host [value index]
  (material/table-row-column
    #js {:key (str "hostPath" index)}
    (material/text-field
      #js {:id       "hostPath"
           :style    #js {:width "100%"}
           :value    value
           :onChange (fn [e v] (update-item index :hostPath v))})))

(defn- form-readonly [value index]
  (material/table-row-column
    #js {:key (str "readOnly" index)}
    (material/checkbox
      #js {:checked value
           :onCheck (fn [e v] (update-item index :readOnly v))})))

(rum/defc form < rum/reactive []
  (let [volumes (rum/react state)]
    (material/theme
      (material/table
        #js {:selectable false}
        (material/table-header-form form-headers #(add-item))
        (material/table-body
          #js {:displayRowCheckbox false}
          (map-indexed
            (fn [index item]
              (let [{:keys [containerPath
                            hostPath
                            readOnly]} item]
                (material/table-row-form
                  index
                  [(form-container containerPath index)
                   (form-host hostPath index)
                   (form-readonly readOnly index)]
                  (fn [] (remove-item index)))))
            volumes))))))