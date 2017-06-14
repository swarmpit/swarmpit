(ns swarmpit.component.service.form-volumes
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :service :wizard :volumes])

(def state-item {:containerPath ""
                 :hostPath      ""
                 :readOnly      false})

(def headers ["Container path" "Host path" "Read only"])

(defn- form-container [value index]
  (comp/table-row-column
    {:name (str "form-container-path-" index)
     :key  (str "form-container-path-" index)}
    (comp/form-list-textfield
      {:name     (str "form-container-path-text-" index)
       :key      (str "form-container-path-text-" index)
       :value    value
       :onChange (fn [_ v]
                   (state/update-item index :containerPath v cursor))})))

(defn- form-host [value index]
  (comp/table-row-column
    {:name (str "form-host-path-" index)
     :key  (str "form-host-path-" index)}
    (comp/form-list-textfield
      {:name     (str "form-host-path-text-" index)
       :key      (str "form-host-path-text-" index)
       :value    value
       :onChange (fn [_ v]
                   (state/update-item index :hostPath v cursor))})))

(defn- form-readonly [value index]
  (comp/table-row-column
    {:name (str "form-readonly-" index)
     :key  (str "form-readonly-" index)}
    (comp/checkbox
      {:name    (str "form-readonly-box-" index)
       :key     (str "form-readonly-box-" index)
       :checked value
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
                     nil
                     true
                     render-volumes
                     (fn [] (state/add-item {:containerPath ""
                                             :hostPath      ""
                                             :readOnly      false} cursor))
                     (fn [index] (state/remove-item index cursor)))))