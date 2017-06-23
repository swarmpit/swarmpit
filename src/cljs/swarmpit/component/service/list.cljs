(ns swarmpit.component.service.list
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.storage :as storage]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [clojure.walk :refer [keywordize-keys]]
            [sablono.core :refer-macros [html]]
            [ajax.core :as ajax]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :service :list])

(def headers ["Name" "Image" "Mode" "Replicas" "Status"])

(def render-item-keys
  [[:serviceName] [:repository :image] [:mode] [:status :info] [:state]])

(defn- render-item-update-state [value]
  (if value
    (comp/label-update "updating")))

(defn- render-item-state [value]
  (case value
    "running" (comp/label-green value)
    "not running" (comp/label-grey value)
    "partly running" (comp/label-yellow value)))

(defn- render-item
  [item service]
  (let [update (get-in service [:status :update])
        value (val item)]
    (case (key item)
      :state (html [:span
                    [:span (render-item-state value)]
                    [:span " "]
                    [:span (render-item-update-state update)]])
      :info (comp/label-info value)
      value)))

(defn- onclick-handler
  [item]
  (routes/path-for-frontend :service-info (select-keys item [:id])))

(defn- filter-cranky-items
  [items name]
  (let [is-running (fn [item] (= "running" (:state item)))
        is-updating (fn [item] (get-in item [:status :update]))]
    (filter #(and (string/includes? (:serviceName %) name)
                  (and (not (is-running %))
                       (not (is-updating %)))) items)))

(defn- filter-items
  [items name cranky?]
  (if cranky?
    (filter-cranky-items items name)
    (filter #(string/includes? (:serviceName %) name) items)))

(defn- data-handler
  []
  (ajax/GET (routes/path-for-backend :services)
            {:headers {"Authorization" (storage/get "token")}
             :handler (fn [response]
                        (keywordize-keys response)
                        (let [resp (keywordize-keys response)]
                          (state/update-value [:data] resp cursor)))}))

(defn- init-state
  [services]
  (state/set-value {:filter {:serviceName ""
                             :cranky      false}
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
                                     (:cranky filter))]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-text-field
         {:hintText "Filter by name"
          :onChange (fn [_ v]
                      (state/update-value [:filter :serviceName] v cursor))})
       [:span.form-panel-space]
       (comp/panel-checkbox
         {:checked (:cranky filter)
          :label   "Show cranky services"
          :onCheck (fn [_ v]
                     (state/update-value [:filter :cranky] v cursor))})]
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