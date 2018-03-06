(ns swarmpit.component.network.list
  (:require [material.component :as comp]
            [material.component.label :as label]
            [material.component.panel :as panel]
            [material.component.list-table :as list]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def headers [{:name  "Name"
               :width "20%"}
              {:name  "Driver"
               :width "20%"}
              {:name  "Subnet"
               :width "20%"}
              {:name  "Gateway"
               :width "20%"}
              {:name  ""
               :width "20%"}])

(def render-item-keys
  [[:networkName] [:driver] [:ipam :subnet] [:ipam :gateway] [:internal]])

(defn- render-item
  [item _]
  (let [value (val item)]
    (case (key item)
      :internal (if value
                  (label/blue "internal"))
      value)))

(defn- onclick-handler
  [item]
  (routes/path-for-frontend :network-info {:id (:networkName item)}))

(defn- networks-handler
  []
  (ajax/get
    (routes/path-for-backend :networks)
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:items] response state/form-value-cursor))}))

(defn- init-form-state
  []
  (state/set-value {:loading? false
                    :filter   {:query ""}} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (networks-handler))))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form
                 mixin/focus-filter [_]
  (let [{:keys [items]} (state/react state/form-value-cursor)
        {:keys [loading? filter]} (state/react state/form-state-cursor)
        filtered-items (list/filter items (:query filter))]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/text-field
         {:id       "filter"
          :hintText "Search networks"
          :onChange (fn [_ v]
                      (state/update-value [:filter :query] v state/form-state-cursor))})]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href    (routes/path-for-frontend :network-create)
            :label   "New network"
            :primary true}))]]
     (list/table headers
                 (sort-by :networkName filtered-items)
                 loading?
                 render-item
                 render-item-keys
                 onclick-handler)]))