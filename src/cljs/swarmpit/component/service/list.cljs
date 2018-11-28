(ns swarmpit.component.service.list
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.list.basic :as list]
            [material.component.list.util :as list-util]
            [material.component.label :as label]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(defn- render-item-ports [item]
  (html
    (for [port (:ports item)]
      [:div
       [:span (:hostPort port)
        [:span.Swarmpit-service-list-port (str " [" (:protocol port) "]")]]])))

(defn- render-item-replicas [item]
  (let [tasks (get-in item [:status :tasks])]
    (str (:running tasks) " / " (:total tasks))))

(defn- render-item-name [item]
  (html
    [:div
     [:div
      [:span (:serviceName item)]]
     [:div
      [:span.Swarmpit-list-image (get-in item [:repository :image])]]]))

(defn- render-item-update-state [value]
  (case value
    "rollback_started" (label/info "rollback")
    (label/info value)))

(defn- render-item-state [value]
  (case value
    "running" (label/green value)
    "not running" (label/info value)
    "partly running" (label/yellow value)))

(defn- render-status [item]
  (let [update-status (get-in item [:status :update])]
    (if (or (= "updating" update-status)
            (= "rollback_started" update-status))
      (render-item-update-state update-status)
      (render-item-state (:state item)))))

(def render-metadata
  {:table {:summary [{:name      "Service"
                      :render-fn (fn [item] (render-item-name item))}
                     {:name      "Replicas"
                      :render-fn (fn [item] (render-item-replicas item))}
                     {:name      "Ports"
                      :render-fn (fn [item] (render-item-ports item))}
                     {:name      "Status"
                      :render-fn (fn [item] (render-status item))}]}
   :list  {:primary   (fn [item] (:serviceName item))
           :secondary (fn [item] (get-in item [:repository :image]))
           :status-fn (fn [item] (render-status item))}})

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

(defn form-search-fn
  [event]
  (state/update-value [:filter :query] (-> event .-target .-value) state/form-state-cursor))

(def form-actions
  [{:button (comp/icon-button
              {:color   "inherit"
               :onClick #(dispatch! (routes/path-for-frontend :service-create-image))} icon/add-circle)
    :name   "New service"}])

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
        filtered-items (list-util/filter items (:query filter))]
    (comp/mui
      (html
        [:div.Swarmpit-form
         [:div.Swarmpit-form-context
          (comp/card
            {:className "Swarmpit-card"}
            (comp/card-header
              {:className "Swarmpit-table-card-header"
               :title     "Overview"})
            (comp/card-content
              {}
              (comp/button
                {:variant   "outlined"
                 :className "Swarmpit-icon-button"
                 :size      "small"
                 :onClick   #(dispatch! (routes/path-for-frontend :service-create-image))
                 :color     "primary"}
                (comp/svg {:style {:marginRight "8px"}} icon/add-small)
                "New service"))
            (comp/card-content
              {:className "Swarmpit-table-card-content"}
              (list/responsive
                render-metadata
                (sort-by :serviceName filtered-items)
                onclick-handler)))]]))))
