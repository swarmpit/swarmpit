(ns swarmpit.component.service.form-labels
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :service :wizard :labels])

(def headers [{:name  "Name"
               :width "35%"}
              {:name  "Value"
               :width "35%"}])

(def render-item-keys
  [[:name] [:value]])

(defn render-item
  [item]
  (val item))

(def empty-info
  (comp/form-value "No labels defined for the service."))

(defn- form-name [value index]
  (comp/form-list-textfield
    {:name     (str "form-name-text-" index)
     :key      (str "form-name-text-" index)
     :value    value
     :onChange (fn [_ v]
                 (state/update-item index :name v cursor))}))

(defn- form-value [value index]
  (comp/form-list-textfield
    {:name     (str "form-value-text-" index)
     :key      (str "form-value-text-" index)
     :value    value
     :onChange (fn [_ v]
                 (state/update-item index :value v cursor))}))

(defn- render-labels
  [item index]
  (let [{:keys [name
                value]} item]
    [(form-name name index)
     (form-value value index)]))

(defn- form-table
  [labels]
  (comp/form-table headers
                   labels
                   nil
                   render-labels
                   (fn [index] (state/remove-item index cursor))))

(defn- add-item
  []
  (state/add-item {:name  ""
                   :value ""} cursor))

(rum/defc form-create < rum/reactive []
  (let [labels (state/react cursor)]
    [:div
     (comp/form-add-btn "Add label" add-item)
     (if (not (empty? labels))
       (form-table labels))]))

(rum/defc form-update < rum/reactive []
  (let [labels (state/react cursor)]
    (if (empty? labels)
      empty-info
      (form-table labels))))