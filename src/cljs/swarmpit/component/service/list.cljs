(ns swarmpit.component.service.list
  (:require [material.component :as comp]
            [material.component.label :as label]
            [material.component.panel :as panel]
            [material.component.list-table :as list]
            [material.component.responsive-table :as responsive]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]
            [material.component.form :as form]))

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

(def status-key [:status :info])

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
  ;mixin/focus-filter
  [_]
  (let [{:keys [items]} (state/react state/form-value-cursor)
        {:keys [loading? filter]} (state/react state/form-state-cursor)
        filtered-items (list/filter items (:query filter))]

    (comp/mui
      (html
        [:div.Swarmpit-form
         [:div.Swarmpit-form-panel
          (panel/search-2 "Find service")
          (comp/button
            {:variant "contained"
             :color   "primary"} "New Service")]
         [:div.Swarmpit-form-context
          (responsive/responsive-table
            render-metadata
            status-key
            (sort-by :serviceName filtered-items))]]))





    ;[:div
    ; [:div.form-panel
    ;  [:div.form-panel-left
    ;   (panel/text-field
    ;     {:id       "filter"
    ;      :hintText "Search services"
    ;      :onChange (fn [_ v]
    ;                  (state/update-value [:filter :query] v state/form-state-cursor))})]
    ;  [:div.form-panel-right
    ;   (comp/mui
    ;     (comp/raised-button
    ;       {:href    (routes/path-for-frontend :service-create-image)
    ;        :label   "New service"
    ;        :primary true}))]]
    ; (list/table headers
    ;             (sort-by :serviceName filtered-items)
    ;             loading?
    ;             render-item
    ;             render-item-keys
    ;             onclick-handler)]

    ))