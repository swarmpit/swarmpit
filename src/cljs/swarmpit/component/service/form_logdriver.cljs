(ns swarmpit.component.service.form-logdriver
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :service :wizard :logdriver])

(def headers [{:name  "Name"
               :width "35%"}
              {:name  "Value"
               :width "35%"}])

(defn- form-driver [value]
  (comp/form-comp
    "DRIVER NAME"
    (comp/select-field
      {:value    value
       :onChange (fn [_ _ v]
                   (state/update-value [:name] v cursor))}
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
  (comp/form-list-textfield
    {:name     (str "form-name-text-" index)
     :key      (str "form-name-text-" index)
     :value    value
     :onChange (fn [_ v]
                 (state/update-item index :name v (conj cursor :opts)))}))

(defn- form-value [value index]
  (comp/form-list-textfield
    {:name     (str "form-value-text-" index)
     :key      (str "form-value-text-" index)
     :value    value
     :onChange (fn [_ v]
                 (state/update-item index :value v (conj cursor :opts)))}))

(defn- render-variables
  [item index]
  (let [{:keys [name
                value]} item]
    [(form-name name index)
     (form-value value index)]))

(defn- form-table
  [opts]
  (comp/form-table headers
                   opts
                   nil
                   render-variables
                   (fn [index] (state/remove-item index (conj cursor :opts)))))

(defn- add-item
  []
  (state/add-item {:name  ""
                   :value ""} (conj cursor :opts)))

(rum/defc form < rum/reactive []
  (let [{:keys [name opts]} (state/react cursor)]
    [:div
     (comp/mui (form-driver name))
     (comp/form-subsection-add "Add log driver option" add-item)
     (if (not (empty? opts))
       (form-table opts))]))