(ns swarmpit.component.service.form-volumes
  (:require [swarmpit.material :as material :refer [svg]]
            [swarmpit.utils :as util]
            [rum.core :as rum]))

(enable-console-print!)

(defonce state (atom []))

(def state-item {:containerPath ""
                 :hostPath      ""
                 :readOnly      false})

(def form-headers ["Container path" "Host path" "Read only"])

(defn- form-container [value index]
  (material/table-row-column
    #js {:key (str "containerPath" index)}
    (material/text-field
      #js {:id       "containerPath"
           :style    #js {:width "100%"}
           :value    value
           :onChange (fn [e v] (util/update-item state index :containerPath v))})))

(defn- form-host [value index]
  (material/table-row-column
    #js {:key (str "hostPath" index)}
    (material/text-field
      #js {:id       "hostPath"
           :style    #js {:width "100%"}
           :value    value
           :onChange (fn [e v] (util/update-item state index :hostPath v))})))

(defn- form-readonly [value index]
  (material/table-row-column
    #js {:key (str "readOnly" index)}
    (material/checkbox
      #js {:checked value
           :onCheck (fn [e v] (util/update-item state index :readOnly v))})))

(rum/defc form < rum/reactive []
  (let [volumes (rum/react state)]
    (material/theme
      (material/table
        #js {:selectable false}
        (material/table-header-form form-headers #(util/add-item state state-item))
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
                  #(util/remove-item state index))))
            volumes))))))