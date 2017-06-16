(ns swarmpit.component.service.form-mounts
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :service :wizard :mounts])

(def headers ["Container path" "Host path" "Type" "Read only"])

(def undefined
  (comp/form-value "No mounts defined for the service."))

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

(defn- form-type [value index]
  (comp/table-row-column
    {:name (str "form-type-" index)
     :key  (str "form-type-" index)}
    (comp/form-list-selectfield
      {:name     (str "form-type-select-" index)
       :key      (str "form-type-select-" index)
       :value    value
       :onChange (fn [_ _ v]
                   (state/update-item index :type v cursor))}
      (comp/menu-item
        {:name        (str "form-type-bind-" index)
         :key         (str "form-type-bind-" index)
         :value       "bind"
         :primaryText "bind"})
      (comp/menu-item
        {:name        (str "form-type-volume-" index)
         :key         (str "form-type-volume-" index)
         :value       "volume"
         :primaryText "volume"}))))

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

(defn- render-mounts
  [item index]
  (let [{:keys [containerPath
                hostPath
                type
                readOnly]} item]
    [(form-container containerPath index)
     (form-host hostPath index)
     (form-type type index)
     (form-readonly readOnly index)]))

(defn- form-table
  [mounts]
  (comp/form-table headers
                   mounts
                   nil
                   render-mounts
                   (fn [index] (state/remove-item index cursor))))

(defn add-item
  []
  (state/add-item {:containerPath ""
                   :hostPath      ""
                   :type          "bind"
                   :readOnly      false} cursor))

(rum/defc form-create < rum/reactive []
  (let [mounts (state/react cursor)]
    [:div
     (comp/form-add-btn "Mount volume" add-item)
     (if (not (empty? mounts))
       (form-table mounts))]))

(rum/defc form-update < rum/reactive []
  (let [mounts (state/react cursor)]
    (if (empty? mounts)
      undefined
      (form-table mounts))))

(rum/defc form-view < rum/static [mounts]
  (if (empty? mounts)
    undefined
    (comp/form-info-table headers mounts identity "150vh")))