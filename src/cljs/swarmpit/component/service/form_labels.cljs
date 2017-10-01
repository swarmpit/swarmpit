(ns swarmpit.component.service.form-labels
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [rum.core :as rum]
            [swarmpit.component.handler :as handler]
            [swarmpit.routes :as routes]))

(enable-console-print!)

(def cursor [:page :service :wizard :labels])

(def headers [{:name  "Name"
               :width "35%"}
              {:name  "Value"
               :width "35%"}])

(def empty-info
  (comp/form-value "No labels defined for the service."))

(defonce label-names (atom []))

(defn labels-handler
  []
  (handler/get
    (routes/path-for-backend :labels-service)
    {:on-success (fn [response]
                   (reset! label-names response))}))

(defn- form-name [value index label-names]
  (comp/autocomplete
    {:name          (str "form-name-text-" index)
     :key           (str "form-name-text-" index)
     :value         value
     :onUpdateInput #(state/update-item index :name % cursor)
     :fullWidth     true
     :searchText    value
     :dataSource    label-names}))

(defn- form-value [value index]
  (comp/form-list-textfield
    {:name     (str "form-value-text-" index)
     :key      (str "form-value-text-" index)
     :value    value
     :onChange (fn [_ v]
                 (state/update-item index :value v cursor))}))

(defn- render-labels
  [item index data]
  (let [{:keys [name
                value]} item]
    [(form-name name index data)
     (form-value value index)]))

(defn- form-table
  [labels label-names]
  (comp/form-table headers
                   labels
                   label-names
                   render-labels
                   (fn [index] (state/remove-item index cursor))))

(defn- add-item
  []
  (state/add-item {:name  ""
                   :value ""} cursor))

(rum/defc form-create < rum/reactive []
  (let [labels (state/react cursor)
        label-names (rum/react label-names)]
    [:div
     (comp/form-add-btn "Add label" add-item)
     (if (not (empty? labels))
       (form-table labels label-names))]))

(rum/defc form-update < rum/reactive []
  (let [labels (state/react cursor)
        label-names (rum/react label-names)]
    (if (empty? labels)
      empty-info
      (form-table labels label-names))))