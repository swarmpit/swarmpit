(ns swarmpit.component.service.form-secrets
  (:require [material.component :as comp]
            [material.component.form :as form]
            [material.component.list-table-form :as list]
            [swarmpit.component.state :as state]
            [swarmpit.routes :as routes]
            [swarmpit.ajax :as ajax]
            [sablono.core :refer-macros [html]]
            [clojure.string :as str]
            [rum.core :as rum]))

(enable-console-print!)

(def form-value-cursor (conj state/form-value-cursor :secrets))

(def form-state-cursor (conj state/form-state-cursor :secrets))

(def headers [{:name  "Name"
               :width "35%"}
              {:name  "Target"
               :width "35%"}])

(def undefined-info
  (form/value
    [:span "No secrets found. Create new "
     [:a {:href (routes/path-for-frontend :secret-create)} "secret."]]))

(defn- form-secret [value index secrets-list]
  (list/selectfield
    {:name     (str "form-secret-select-" index)
     :key      (str "form-secret-select-" index)
     :value    value
     :onChange (fn [_ _ v]
                 (state/update-item index :secretName v form-value-cursor))}
    (->> secrets-list
         (map #(comp/menu-item
                 {:name        (str "form-secret-item-" (:secretName %))
                  :key         (str "form-secret-item-" (:secretName %))
                  :value       (:secretName %)
                  :primaryText (:secretName %)})))))

(defn- form-secret-target [value name index]
  (list/textfield
    {:name     (str "form-secret-target-" index)
     :key      (str "form-secret-target-" index)
     :hintText (when (str/blank? value)
                 name)
     :value    value
     :onChange (fn [_ v]
                 (state/update-item index :secretTarget v form-value-cursor))}))

(defn- render-secrets
  [item index data]
  (let [{:keys [secretName secretTarget]} item]
    [(form-secret secretName index data)
     (form-secret-target secretTarget secretName index)]))

(defn- form-table
  [secrets secrets-list]
  (form/form
    {}
    (list/table-raw headers
                    secrets
                    secrets-list
                    render-secrets
                    (fn [index] (state/remove-item index form-value-cursor)))))

(defn- add-item
  []
  (state/add-item {:secretName   ""
                   :secretTarget ""} form-value-cursor))

(defn secrets-handler
  []
  (ajax/get
    (routes/path-for-backend :secrets)
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:list] response form-state-cursor))}))

(rum/defc form < rum/reactive []
  (let [{:keys [list]} (state/react form-state-cursor)
        secrets (state/react form-value-cursor)]
    (if (empty? secrets)
      (form/value "No secrets defined for the service.")
      (if (empty? list)
        undefined-info
        (form-table secrets list)))))