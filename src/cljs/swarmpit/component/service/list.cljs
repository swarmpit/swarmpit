(ns swarmpit.component.service.list
  (:require [material.component :as comp]
            [swarmpit.uri :refer [dispatch!]]
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
        filtered-items (filter-items items predicate)
        service-id (fn [index] (:id (nth filtered-items index)))]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/mui
         (comp/text-field
           {:hintText       "Filter by name"
            :onChange       (fn [e v]
                              (state/update-value :predicate v cursor))
            :underlineStyle {:borderColor "rgba(0, 0, 0, 0.2)"}
            :style          {:height     "44px"
                             :lineHeight "15px"}}))]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href    "/#/services/create"
            :label   "Create"
            :primary true}))]]
     (comp/mui
       (comp/table
         {:selectable  false
          :onCellClick (fn [i] (dispatch!
                                 (str "/#/services/" (service-id i))))}
         (comp/list-table-header headers)
         (comp/list-table-body filtered-items
                               render-item
                               [:serviceName :mode :replicas :image])))]))

(defn mount!
  [items]
  (rum/mount (service-list items) (.getElementById js/document "content")))