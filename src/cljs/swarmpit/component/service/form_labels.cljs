(ns swarmpit.component.service.form-labels
  (:require [material.component :as comp]
            [material.component.list.edit :as list]
            [swarmpit.component.state :as state]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def form-value-cursor (conj state/form-value-cursor :labels))

(def form-state-cursor (conj state/form-state-cursor :labels))

(defn- form-name [value index]
  (comp/text-field
    {:fullWidth       true
     :label           "Name"
     :key             (str "form-label-name-" index)
     :value           value
     :required        true
     :variant         "outlined"
     :margin          "dense"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-item index :name (-> % .-target .-value) form-value-cursor)}))

(defn- form-value [value index]
  (comp/text-field
    {:fullWidth       true
     :label           "Value"
     :key             (str "form-label-value-" index)
     :value           value
     :required        true
     :variant         "outlined"
     :margin          "dense"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-item index :value (-> % .-target .-value) form-value-cursor)}))

(def form-metadata
  [{:name      "Name"
    :primary   true
    :key       [:name]
    :render-fn (fn [value _ index] (form-name value index))}
   {:name      "Value"
    :key       [:value]
    :render-fn (fn [value _ index] (form-value value index))}])

(defn- form-table
  [labels names]
  (list/responsive
    form-metadata
    labels
    (fn [index] (state/remove-item index form-value-cursor))))

(defn- add-item
  []
  (state/add-item {:name  ""
                   :value ""} form-value-cursor))

(defn labels-handler
  []
  (ajax/get
    (routes/path-for-backend :labels-service)
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:names] response form-state-cursor))}))

(rum/defc form < rum/reactive []
  (let [{:keys [names]} (state/react form-state-cursor)
        labels (state/react form-value-cursor)]
    (if (empty? labels)
      (html [:div "No labels defined for the service."])
      (form-table labels names))))