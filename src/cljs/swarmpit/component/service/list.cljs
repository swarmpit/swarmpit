(ns swarmpit.component.service.list
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [clojure.string :as string]
            [rum.core :as rum]
            [sablono.core :refer-macros [html]]))

(enable-console-print!)

(def cursor [:form :service :list :filter])

(def headers ["Name" "Image" "Mode" "Replicas" "Status" "Updates"])

(defn- filter-items
  "Filter list items based on given predicate"
  [items predicate]
  (filter #(string/includes? (:serviceName %) predicate) items))

(defn- render-item
  [item]
  (let [value (val item)]
    (case (key item)
      :state (case value
               "running" (comp/label-green value)
               "not running" (comp/label-grey value)
               "partial running" (comp/label-yellow value))
      :info (comp/chip {:style      {:backgroundColor "rgb(245, 245, 245)"
                                     :border          "1px solid rgb(224, 228, 231)"}
                        :labelStyle {:fontSize "13px"}} value)
      :update (comp/refresh-indicator {:size   30
                                       :left   10
                                       :top    0
                                       :status "loading"
                                       :style  {:display  "inline-block"
                                                :position "relative"}})
      value)))

(rum/defc service-list < rum/reactive [items]
  (let [{:keys [serviceName]} (state/react cursor)
        filtered-items (filter-items items serviceName)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-text-field
         {:hintText "Filter by name"
          :onChange (fn [_ v]
                      (state/update-value :serviceName v cursor))})]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href    "/#/services/create"
            :label   "Create"
            :primary true}))]]
     (comp/list-table headers
                      filtered-items
                      render-item
                      [[:serviceName] [:image] [:mode] [:status :info] [:state] [:status :update]]
                      "/#/services/")]))

(defn- init-state
  []
  (state/set-value {:serviceName ""} cursor))

(defn mount!
  [items]
  (init-state)
  (rum/mount (service-list items) (.getElementById js/document "content")))