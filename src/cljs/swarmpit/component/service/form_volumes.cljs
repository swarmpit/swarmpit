(ns swarmpit.component.service.form-volumes
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form :service :volumes])

(def state-item {:containerPath ""
                 :hostPath      ""
                 :readOnly      false})

(def form-headers ["Container path" "Host path" "Read only"])

(defn- form-container [value index]
  (comp/table-row-column
    {:key (str "containerPath" index)}
    (comp/text-field
      {:id       "containerPath"
       :style    {:width "100%"}
       :value    value
       :onChange (fn [e v]
                   (state/update-item index :containerPath v cursor))})))

(defn- form-host [value index]
  (comp/table-row-column
    {:key (str "hostPath" index)}
    (comp/text-field
      {:id       "hostPath"
       :style    {:width "100%"}
       :value    value
       :onChange (fn [e v]
                   (state/update-item index :hostPath v cursor))})))

(defn- form-readonly [value index]
  (comp/table-row-column
    {:key (str "readOnly" index)}
    (comp/checkbox
      {:checked value
       :onCheck (fn [e v]
                  (state/update-item index :readOnly v cursor))})))

(rum/defc form < rum/reactive []
  (let [volumes (state/react cursor)]
    (comp/mui
      (comp/table
        {:selectable false}
        (comp/table-header-form form-headers #(state/add-item state-item cursor))
        (comp/table-body
          {:displayRowCheckbox false}
          (map-indexed
            (fn [index item]
              (let [{:keys [containerPath
                            hostPath
                            readOnly]} item]
                (comp/table-row-form
                  index
                  [(form-container containerPath index)
                   (form-host hostPath index)
                   (form-readonly readOnly index)]
                  #(state/remove-item index cursor))))
            volumes))))))