(ns swarmpit.component.service.form-volumes
  (:require [swarmpit.component.state :as state]
            [swarmpit.material :as material]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form :service :volumes])

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
           :onChange (fn [e v]
                       (state/update-item index :containerPath v cursor))})))

(defn- form-host [value index]
  (material/table-row-column
    #js {:key (str "hostPath" index)}
    (material/text-field
      #js {:id       "hostPath"
           :style    #js {:width "100%"}
           :value    value
           :onChange (fn [e v]
                       (state/update-item index :hostPath v cursor))})))

(defn- form-readonly [value index]
  (material/table-row-column
    #js {:key (str "readOnly" index)}
    (material/checkbox
      #js {:checked value
           :onCheck (fn [e v]
                      (state/update-item index :readOnly v cursor))})))

(rum/defc form < rum/reactive []
  (let [volumes (state/react cursor)]
    (material/theme
      (material/table
        #js {:selectable false}
        (material/table-header-form form-headers #(state/add-item state-item cursor))
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
                  #(state/remove-item index cursor))))
            volumes))))))