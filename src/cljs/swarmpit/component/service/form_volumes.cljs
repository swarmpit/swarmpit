(ns swarmpit.component.service.form-volumes
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form :service :volumes])

(def state-item {:containerPath ""
                 :hostPath      ""
                 :readOnly      false})

(def headers ["Container path" "Host path" "Read only"])

(defn- form-container [value index]
  (comp/table-row-column
    {:key (str "vcp-" index)}
    (comp/form-list-textfield
      {:id       "containerPath"
       :value    value
       :onChange (fn [_ v]
                   (state/update-item index :containerPath v cursor))})))

(defn- form-host [value index]
  (comp/table-row-column
    {:key (str "vhp-" index)}
    (comp/form-list-textfield
      {:id       "hostPath"
       :value    value
       :onChange (fn [_ v]
                   (state/update-item index :hostPath v cursor))})))

(defn- form-readonly [value index]
  (comp/table-row-column
    {:key (str "vro-" index)}
    (comp/checkbox
      {:checked value
       :onCheck (fn [_ v]
                  (state/update-item index :readOnly v cursor))})))

(defn- render-volumes
  [item index]
  (let [{:keys [containerPath
                hostPath
                readOnly]} item]
    [(form-container containerPath index)
     (form-host hostPath index)
     (form-readonly readOnly index)]))

(rum/defc form < rum/reactive []
  (let [volumes (state/react cursor)]
    (comp/form-table headers
                     volumes
                     render-volumes
                     (fn [] (state/add-item {:containerPath ""
                                             :hostPath      ""
                                             :readOnly      false} cursor))
                     (fn [index] (state/remove-item index cursor)))))