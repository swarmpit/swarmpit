(ns swarmpit.component.service.form-configs
  (:require [material.component :as comp]
            [material.component.form :as form]
            [material.component.list-table-form :as list]
            [swarmpit.component.state :as state]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [clojure.string :as str]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form :configs])

(defonce configs-list (atom []))

(defn configs-handler
  []
  (ajax/get
    (routes/path-for-backend :configs)
    {:on-success (fn [response]
                   (reset! configs-list response))}))

(def headers [{:name  "Name"
               :width "35%"}
              {:name  "Target"
               :width "35%"}])

(def undefined-info
  (form/value
    [:span "No configs found. Create new "
     [:a {:href (routes/path-for-frontend :config-create)} "config."]]))

(defn- form-config [value index configs-list]
  (list/selectfield
    {:name     (str "form-config-select-" index)
     :key      (str "form-config-select-" index)
     :value    value
     :onChange (fn [_ _ v]
                 (state/update-item index :configName v cursor))}
    (->> configs-list
         (map #(comp/menu-item
                 {:name        (str "form-config-item-" (:configName %))
                  :key         (str "form-config-item-" (:configName %))
                  :value       (:configName %)
                  :primaryText (:configName %)})))))

(defn- form-config-target [value name index]
  (list/textfield
    {:name     (str "form-config-target-" index)
     :key      (str "form-config-target-" index)
     :hintText (when (str/blank? value)
                 name)
     :value    value
     :onChange (fn [_ v]
                 (state/update-item index :configTarget v cursor))}))

(defn- render-configs
  [item index data]
  (let [{:keys [configName configTarget]} item]
    [(form-config configName index data)
     (form-config-target configTarget configName index)]))

(defn- form-table
  [configs configs-list]
  (list/table headers
              configs
              configs-list
              render-configs
              (fn [index] (state/remove-item index cursor))))

(defn- add-item
  []
  (state/add-item {:configName   ""
                   :configTarget ""} cursor))

(rum/defc form < rum/reactive []
  (let [configs-list (rum/react configs-list)
        configs (state/react cursor)]

    (if (empty? configs)
      (form/value "No configs defined for the service.")
      (if (empty? configs-list)
        undefined-info
        (form-table configs configs-list)))))