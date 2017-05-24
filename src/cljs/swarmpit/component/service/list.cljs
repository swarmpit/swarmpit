(ns swarmpit.component.service.list
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [clojure.string :as string]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form :service :list])

(def headers ["Name" "Mode" "Replicas" "Image"])

(defn- filter-items
  "Filter list items based on given predicate"
  [items predicate]
  (filter #(string/includes? (:serviceName %) predicate) items))

(defn- render-item
  [item]
  (val item))

(rum/defc service-list < rum/reactive [items]
  (let [{:keys [predicate]} (state/react cursor)
        filtered-items (filter-items items predicate)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-text-field
         {:hintText "Filter by name"
          :onChange (fn [_ v]
                      (state/update-value :predicate v cursor))})]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href    "/#/services/create"
            :label   "Create"
            :primary true}))]]
     (comp/list-table headers
                      filtered-items
                      render-item
                      [:serviceName :mode :replicas :image]
                      "/#/services/")]))

(defn mount!
  [items]
  (rum/mount (service-list items) (.getElementById js/document "content")))