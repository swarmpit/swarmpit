(ns swarmpit.component.service.list
  (:require [material.icon :as icon]
            [material.component :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.handler :as handler]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :service :list])

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
    "rollback_started" (comp/label-update "rollback")
    (comp/label-update value)))

(defn- render-item-state [value]
  (case value
    "running" (comp/label-green value)
    "not running" (comp/label-grey value)
    "partly running" (comp/label-yellow value)))

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
      :info (comp/label-info value)
      value)))

(defn- onclick-handler
  [item]
  (routes/path-for-frontend :service-info {:id (:serviceName item)}))

(defn- filter-unhealthy-items
  [items name]
  (let [is-running (fn [item] (= "running" (:state item)))
        is-updating (fn [item] (= "updating" (get-in item [:status :update])))]
    (filter #(and (string/includes? (:serviceName %) name)
                  (and (not (is-running %))
                       (not (is-updating %)))) items)))

(defn- filter-items
  [items name unhealthy?]
  (if unhealthy?
    (filter-unhealthy-items items name)
    (filter #(string/includes? (:serviceName %) name) items)))

(defn- data-handler
  []
  (handler/get
    (routes/path-for-backend :services)
    {:on-success (fn [response]
                   (state/update-value [:data] response cursor))}))

(defn- init-state
  [services]
  (state/set-value {:filter {:serviceName ""
                             :unhealthy   false}
                    :data   services} cursor))

(def refresh-state-mixin
  (mixin/refresh data-handler))

(def init-state-mixin
  (mixin/init
    (fn [data]
      (init-state data))))

(rum/defc form < rum/reactive
                 init-state-mixin
                 refresh-state-mixin [_]
  (let [{:keys [filter data]} (state/react cursor)
        filtered-items (filter-items data
                                     (:serviceName filter)
                                     (:unhealthy filter))]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-text-field
         {:hintText "Filter by name"
          :onChange (fn [_ v]
                      (state/update-value [:filter :serviceName] v cursor))})
       [:span.form-panel-space]
       (comp/panel-checkbox
         {:checked (:unhealthy filter)
          :label   "Show unhealthy"
          :onCheck (fn [_ v]
                     (state/update-value [:filter :unhealthy] v cursor))})]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href    (routes/path-for-frontend :service-create-image)
            :label   "New service"
            :primary true}))]]
     (comp/list-table headers
                      (sort-by :serviceName filtered-items)
                      render-item
                      render-item-keys
                      onclick-handler)]))