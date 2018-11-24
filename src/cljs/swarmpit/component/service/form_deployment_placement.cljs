(ns swarmpit.component.service.form-deployment-placement
  (:require [material.component :as comp]
            [material.component.list.edit :as list]
            [swarmpit.component.state :as state]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def form-value-cursor (into state/form-value-cursor [:deployment :placement]))

(def form-state-cursor (conj state/form-state-cursor :placement))

(defn- form-placement [value index]
  (comp/text-field
    {:fullWidth       true
     :label           "Placement"
     :key             (str "form-placement-" index)
     :value           value
     :required        true
     :placeholder     "e.g. node.role == manager"
     :variant         "outlined"
     :margin          "dense"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-item index :rule (-> % .-target .-value) form-value-cursor)}))

(def form-metadata
  [{:name      "Rule"
    :primary   true
    :key       [:rule]
    :render-fn (fn [value _ index] (form-placement value index))}])

(defn- form-table
  [placement placement-list]
  (list/responsive
    form-metadata
    placement
    (fn [index] (state/remove-item index form-value-cursor))))

(defn- add-item
  []
  (state/add-item {:rule ""} form-value-cursor))

(defn placement-handler
  []
  (ajax/get
    (routes/path-for-backend :placement)
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:list] response form-state-cursor))}))

(rum/defc form < rum/reactive []
  (let [{:keys [list]} (state/react form-state-cursor)
        placement (state/react form-value-cursor)]
    (form-table placement list)))
