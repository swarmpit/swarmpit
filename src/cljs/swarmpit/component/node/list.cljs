(ns swarmpit.component.node.list
  (:require [material.component :as comp]
            [swarmpit.uri :refer [dispatch!]]
            [swarmpit.component.state :as state]
            [clojure.string :as string]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form :node :list])

(def headers ["Name" "Status" "Availability" "Leader"])

(defn- filter-items
  "Filter list items based on given predicate"
  [items predicate]
  (filter #(string/includes? (:name %) predicate) items))

(defn- render-item
  [item]
  (case (val item)
    true "yes"
    false "no"
    (val item)))

(rum/defc node-list < rum/reactive [items]
  (let [{:keys [predicate]} (state/react cursor)
        filtered-items (filter-items items predicate)
        node-id (fn [index] (:id (nth filtered-items index)))]
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
                             :lineHeight "15px"}}))]]
     (comp/mui
       (comp/table
         {:selectable  false
          :onCellClick (fn [i] (dispatch!
                                 (str "/#/nodes/" (node-id i))))}
         (comp/list-table-header headers)
         (comp/list-table-body filtered-items
                               render-item
                               [:name :state :availability :leader])))]))

(defn mount!
  [items]
  (rum/mount (node-list items) (.getElementById js/document "content")))
