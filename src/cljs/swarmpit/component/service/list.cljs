(ns swarmpit.component.service.list
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.list :as list]
            [material.component.label :as label]
            [material.component.panel :as panel]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(defn- render-item-ports [value]
  (html
    (for [port value]
      [:div
       [:span (:hostPort port)
        [:span.Swarmpit-service-list-port (str " [" (:protocol port) "]")]]])))

(defn- render-item-update-state [value]
  (case value
    "rollback_started" (label/info "rollback")
    (label/info value)))

(defn- render-item-state [value]
  (case value
    "running" (label/green value)
    "not running" (label/info value)
    "partly running" (label/yellow value)))

(defn- render-status [value item]
  (let [update-status (get-in item [:status :update])]
    (if (or (= "updating" update-status)
            (= "rollback_started" update-status))
      (render-item-update-state update-status)
      (render-item-state value))))

(def render-metadata
  [{:name    "Name"
    :key     [:serviceName]
    :primary true}
   {:name "Image"
    :key  [:repository :image]}
   {:name      "Replicas"
    :key       [:status :info]
    :render-fn (fn [value _] (label/info value))}
   {:name      "Ports"
    :key       [:ports]
    :render-fn (fn [value _] (render-item-ports value))}
   {:name      "Status"
    :key       [:state]
    :render-fn (fn [value item] (render-status value item))}])

(defn- render-state-fn
  [item]
  (case (:state item)
    "running" [:div.Swarmpit-icon-ok icon/check-circle]
    "not running" [:div.Swarmpit-icon-info icon/cancel]
    "partly running" [:div.Swarmpit-icon-warning icon/check-circle]))

(defn- onclick-handler
  [item]
  (dispatch! (routes/path-for-frontend :service-info {:id (:serviceName item)})))

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

;(defn linked-services
;  [services]
;  (when (not-empty services)
;    [:div.form-layout-group.form-layout-group-border
;     (form/section "Linked Services")
;     (list-auto/table (map :name headers)
;                      services
;                      render-item
;                      render-item-keys
;                      onclick-handler)]))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form
                 mixin/focus-filter [_]
  (let [{:keys [items]} (state/react state/form-value-cursor)
        {:keys [loading? filter]} (state/react state/form-state-cursor)
        filtered-items (list/filter items (:query filter))]
    (comp/mui
      (html
        [:div.Swarmpit-form
         [:div.Swarmpit-form-panel
          (panel/search
            "Search services"
            (fn [event]
              (state/update-value [:filter :query] (-> event .-target .-value) state/form-state-cursor)))
          (comp/button
            {:variant "contained"
             :onClick #(dispatch! (routes/path-for-frontend :service-create-image))
             :color   "primary"} "New Service")]
         [:div.Swarmpit-form-context
          (list/responsive-table
            render-metadata
            render-state-fn
            (sort-by :serviceName filtered-items)
            onclick-handler)]]))))