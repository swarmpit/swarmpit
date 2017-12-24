(ns swarmpit.component.service.form-secrets
  (:require [material.component :as comp]
            [material.component.form :as form]
            [material.component.list-table-form :as list]
            [material.icon :as icon]
            [swarmpit.routes :as routes]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.state :as state]
            [rum.core :as rum]
            [clojure.string :as str]))

(enable-console-print!)

(def cursor [:form :secrets])

(defonce secrets-list (atom []))

(defn secrets-handler
  []
  (handler/get
    (routes/path-for-backend :secrets)
    {:on-success (fn [response]
                   (reset! secrets-list response))}))

(def headers [{:name  "Name"
               :width "35%"}
              {:name  "Target"
               :width "35%"}])

(def empty-info
  (form/value "No secrets defined for the service."))

(def undefined-info
  (form/icon-value
    icon/info
    [:span "No secrets found. Create new "
     [:a {:href (routes/path-for-frontend :secret-create)} "secret."]]))

(defn- form-secret [value index secrets-list]
  (list/selectfield
    {:name     (str "form-secret-select-" index)
     :key      (str "form-secret-select-" index)
     :value    value
     :onChange (fn [_ _ v]
                 (state/update-item index :secretName v cursor))}
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
                 (state/update-item index :secretTarget v cursor))}))

(defn- render-secrets
  [item index data]
  (let [{:keys [secretName secretTarget]} item]
    [(form-secret secretName index data)
     (form-secret-target secretTarget secretName index)]))

(defn- form-table
  [secrets secrets-list]
  (list/table headers
              secrets
              secrets-list
              render-secrets
              (fn [index] (state/remove-item index cursor))))

(defn- add-item
  []
  (state/add-item {:secretName   ""
                   :secretTarget ""} cursor))

(rum/defc form-create < rum/reactive []
  (let [secrets-list (rum/react secrets-list)
        secrets (state/react cursor)]
    [:div
     (when (empty? secrets-list)
       undefined-info)
     (when (not (empty? secrets))
       (form-table secrets secrets-list))]))

(rum/defc form-update < rum/reactive []
  (let [secrets (state/react cursor)]
    (if (empty? secrets)
      empty-info
      (form-table secrets (rum/react secrets-list)))))