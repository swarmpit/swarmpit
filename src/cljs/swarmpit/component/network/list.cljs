(ns swarmpit.component.network.list
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.list.basic :as list]
            [material.component.list.util :as list-util]
            [material.component.label :as label]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def render-metadata
  [{:name    "Name"
    :key     [:networkName]
    :primary true}
   {:name "Driver"
    :key  [:driver]}
   {:name "Subnet"
    :key  [:ipam :subnet]}
   {:name "Gateway"
    :key  [:ipam :gateway]}
   {:name      "Status"
    :key       [:internal]
    :render-fn (fn [value _] (when value (label/blue "internal")))}])

(defn- onclick-handler
  [item]
  (dispatch! (routes/path-for-frontend :network-info {:id (:networkName item)})))

(defn- networks-handler
  []
  (ajax/get
    (routes/path-for-backend :networks)
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:items] response state/form-value-cursor))}))

(defn form-search-fn
  [event]
  (state/update-value [:filter :query] (-> event .-target .-value) state/form-state-cursor))

(def form-actions
  [{:button (comp/icon-button
              {:color   "inherit"
               :onClick #(dispatch! (routes/path-for-frontend :network-create))} icon/add-circle)
    :name   "New network"}])

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
        filtered-items (list-util/filter items (:query filter))]
    (comp/mui
      (html
        [:div.Swarmpit-form
         [:div.Swarmpit-form-context
          (list/responsive
            render-metadata
            nil
            (sort-by :networkName filtered-items)
            onclick-handler)]]))))