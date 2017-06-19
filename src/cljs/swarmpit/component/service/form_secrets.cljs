(ns swarmpit.component.service.form-secrets
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.routes :as routes]
            [swarmpit.component.state :as state]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :service :wizard :secrets])

(def headers ["Name"])

(def empty-info
  (comp/form-value "No secrets defined for the service."))

(def undefined-info
  (comp/form-icon-value
    icon/info
    [:span "No secrets found. Create new "
     [:a {:href (routes/path-for-frontend :secret-create)} "secret."]]))

(defn- form-secret [value index data]
  (comp/table-row-column
    {:name (str "form-secret-" index)
     :key  (str "form-secret-" index)}
    (comp/form-list-selectfield
      {:name     (str "form-secret-select-" index)
       :key      (str "form-secret-select-" index)
       :value    value
       :onChange (fn [_ _ v]
                   (state/update-item index :secretName v cursor))}
      (->> data
           (map #(comp/menu-item
                   {:name        (str "form-secret-item-" (:secretName %))
                    :key         (str "form-secret-item-" (:secretName %))
                    :value       (:secretName %)
                    :primaryText (:secretName %)}))))))

(defn- render-secrets
  [item index data]
  (let [{:keys [secretName]} item]
    [(form-secret secretName index data)]))

(defn- form-table
  [secrets data]
  (comp/form-table []
                   secrets
                   data
                   render-secrets
                   (fn [index] (state/remove-item index cursor))))

(defn- add-item
  []
  (state/add-item {:secretName ""
                   :id         ""} cursor))

(def render-item-keys
  [[:secretName]])

(defn- render-item
  [item]
  (val item))

(rum/defc form-create < rum/reactive [data]
  (let [secrets (state/react cursor)]
    [:div
     (if (empty? data)
       undefined-info
       (comp/form-add-btn "Expose secrets" add-item))
     (if (not (empty? secrets))
       (form-table secrets data))]))

(rum/defc form-update < rum/reactive [data]
  (let [secrets (state/react cursor)]
    (if (empty? secrets)
      empty-info
      (form-table secrets data))))

(rum/defc form-view < rum/static [secrets]
  (if (empty? secrets)
    empty-info
    (comp/form-info-table headers
                          secrets
                          render-item
                          render-item-keys
                          "300px")))