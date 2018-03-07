(ns swarmpit.component.service.form-logdriver
  (:require [material.component :as comp]
            [material.component.form :as form]
            [material.component.list-table-form :as list]
            [swarmpit.component.state :as state]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def form-value-cursor (conj state/form-value-cursor :logdriver))

(def form-value-opts-cursor (conj form-value-cursor :opts))

(def headers [{:name  "Name"
               :width "35%"}
              {:name  "Value"
               :width "35%"}])

(defn- form-driver [value]
  (form/comp
    "DRIVER"
    (comp/select-field
      {:value    value
       :onChange (fn [_ _ v]
                   (state/update-value [:name] v form-value-cursor))}
      (comp/menu-item
        {:key         "none"
         :value       "none"
         :primaryText "none"})
      (comp/menu-item
        {:key         "json-file"
         :value       "json-file"
         :primaryText "json-file"})
      (comp/menu-item
        {:key         "journald"
         :value       "journald"
         :primaryText "journald"}))))

(defn- form-name [value index]
  (list/textfield
    {:name     (str "form-name-text-" index)
     :key      (str "form-name-text-" index)
     :value    value
     :onChange (fn [_ v]
                 (state/update-item index :name v form-value-opts-cursor))}))

(defn- form-value [value index]
  (list/textfield
    {:name     (str "form-value-text-" index)
     :key      (str "form-value-text-" index)
     :value    value
     :onChange (fn [_ v]
                 (state/update-item index :value v form-value-opts-cursor))}))

(defn- render-variables
  [item index]
  (let [{:keys [name
                value]} item]
    [(form-name name index)
     (form-value value index)]))

(defn- form-table
  [opts]
  (list/table-raw headers
                  opts
                  nil
                  render-variables
                  (fn [index] (state/remove-item index form-value-opts-cursor))))

(defn- add-item
  []
  (state/add-item {:name  ""
                   :value ""} form-value-opts-cursor))

(rum/defc form < rum/reactive []
  (let [{:keys [name opts]} (state/react form-value-cursor)]
    (form/form
      {}
      (form-driver name)
      (html (form/subsection-add "Add log driver option" add-item))
      (when (not (empty? opts))
        (form-table opts)))))