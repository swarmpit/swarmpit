(ns swarmpit.component.service.list
  (:require [material.component :as comp]
            [material.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [swarmpit.storage :as storage]
            [clojure.walk :refer [keywordize-keys]]
            [sablono.core :refer-macros [html]]
            [ajax.core :as ajax]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :service :list])

(def headers ["Name" "Image" "Mode" "Replicas" "Status"])

(defn- form-update [value]
  (if value
    (comp/label-update "updating")))

(defn- form-state [value]
  (case value
    "running" (comp/label-green value)
    "not running" (comp/label-grey value)
    "partly running" (comp/label-yellow value)))

(defn- filter-items
  "Filter list `items` based on `name` & `cranky?` flag"
  [items name cranky?]
  (let [is-running (fn [item] (= "running" (:state item)))
        is-updating (fn [item] (get-in item [:status :update]))]
    (if cranky?
      (filter #(and (string/includes? (:serviceName %) name)
                    (and (not (is-running %))
                         (not (is-updating %)))) items)
      (filter #(string/includes? (:serviceName %) name) items))))

(def render-item-keys
  [[:serviceName] [:repository :image] [:mode] [:status :info] [:state]])

(defn- render-item
  [item service]
  (let [update (get-in service [:status :update])
        value (val item)]
    (case (key item)
      :state (html [:span
                    [:span (form-state value)]
                    [:span " "]
                    [:span (form-update update)]])
      :info (comp/label-info value)
      value)))

(defn- onclick-handler
  [item]
  (routes/path-for-frontend :service-info (select-keys item [:id])))

(defn- data-handler
  []
  (ajax/GET (routes/path-for-backend :services)
            {:headers {"Authorization" (storage/get "token")}
             :handler (fn [response]
                        (keywordize-keys response)
                        (let [resp (keywordize-keys response)]
                          (state/update-value [:data] resp cursor)))}))

(def refresh-mixin
  (mixin/list-refresh-mixin data-handler))

(rum/defc service-list < rum/reactive
                         refresh-mixin []
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
            :label   "Create"
            :primary true}))]]
     (comp/list-table headers
                      (sort-by :serviceName filtered-items)
                      render-item
                      render-item-keys
                      onclick-handler)]))

(defn- init-state
  [services]
  (state/set-value {:filter {:serviceName ""
                             :cranky      false}
                    :data   services} cursor))

(defn mount!
  [services]
  (init-state services)
  (rum/mount (service-list) (.getElementById js/document "content")))