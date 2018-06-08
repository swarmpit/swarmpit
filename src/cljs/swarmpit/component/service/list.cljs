(ns swarmpit.component.service.list
  (:require [material.component :as comp]
            [material.component.label :as label]
            [material.component.panel :as panel]
            [material.component.list-table :as list]
            [material.component.list-table-auto :as list-auto]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]
            [material.component.form :as form]))

(enable-console-print!)

(def headers [{:name  "Name"
               :width "20%"}
              {:name  "Image"
               :width "30%"}
              {:name  "Replicas"
               :width "15%"}
              {:name  "Ports"
               :width "15%"}
              {:name  "Status"
               :width "20%"}])

(def render-item-keys
  [[:serviceName] [:repository :image] [:status :info] [:ports] [:state]])

(defn- render-item-update-state [value]
  (case value
    "rollback_started" (label/update "rollback")
    (label/update value)))

(defn- render-item-state [value]
  (case value
    "running" (label/green value)
    "not running" (label/grey value)
    "partly running" (label/yellow value)))

(defn- render-item-ports [value]
  (html
    (for [port value]
      [:div
       [:span (:hostPort port)
        [:span.service-list-port (str " [" (:protocol port) "]")]]])))

(defn- render-status [value update-status]
  (if (or (= "updating" update-status)
          (= "rollback_started" update-status))
    (render-item-update-state update-status)
    (render-item-state value)))

(defn- render-item
  [item service]
  (let [update (get-in service [:status :update])
        value (val item)]
    (case (key item)
      :ports (render-item-ports value)
      :state (render-status value update)
      :info (label/info value)
      value)))

(defn- onclick-handler
  [item]
  (routes/path-for-frontend :service-info {:id (:serviceName item)}))

(defn- services-handler
  []
  (ajax/get
    (routes/path-for-backend :services)
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
      (services-handler))))

(defn linked-services
  [services]
  (when (not-empty services)
    [:div.form-layout-group.form-layout-group-border
     (form/section "Linked Services")
     (list-auto/table (map :name headers)
                      services
                      render-item
                      render-item-keys
                      onclick-handler)]))

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
          :hintText "Search services"
          :onChange (fn [_ v]
                      (state/update-value [:filter :query] v state/form-state-cursor))})]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href    (routes/path-for-frontend :service-create-image)
            :label   "New service"
            :primary true}))]]
     (list/table headers
                 (sort-by :serviceName filtered-items)
                 loading?
                 render-item
                 render-item-keys
                 onclick-handler)]))