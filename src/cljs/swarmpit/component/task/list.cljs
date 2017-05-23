(ns swarmpit.component.task.list
  (:require [material.component :as comp]
            [swarmpit.uri :refer [dispatch!]]
            [swarmpit.component.state :as state]
            [clojure.string :as string]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form :task :list])

(def headers ["Name" "Service" "Image" "Node" "State"])

(defn- filter-items
  "Filter list items based on given predicate"
  [items predicate]
  (filter #(string/includes? (:service %) predicate) items))

(defn- render-item
  [item]
  (val item))

(rum/defc task-list < rum/reactive [items]
  (let [{:keys [predicate]} (state/react cursor)
        filtered-items (filter-items items predicate)
        task-id (fn [index] (:id (nth filtered-items index)))]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/mui
         (comp/text-field
           {:hintText       "Filter by service name"
            :onChange       (fn [e v]
                              (state/update-value :predicate v cursor))
            :underlineStyle {:borderColor "rgba(0, 0, 0, 0.2)"}
            :style          {:height     "44px"
                             :lineHeight "15px"}}))]]
     (comp/mui
       (comp/table
         {:selectable  false
          :onCellClick (fn [i] (dispatch!
                                 (str "/#/tasks/" (task-id i))))}
         (comp/list-table-header headers)
         (comp/list-table-body filtered-items
                               render-item
                               [:name :service :image :node :state])))]))

(defn mount!
  [items]
  (rum/mount (task-list items) (.getElementById js/document "content")))
