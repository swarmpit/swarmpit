(ns swarmpit.component.task.list
  (:require [material.component :as comp]
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
        filtered-items (filter-items items predicate)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-text-field
         {:hintText "Filter by service name"
          :onChange (fn [_ v]
                      (state/update-value :predicate v cursor))})]]
     (comp/list-table headers
                      filtered-items
                      render-item
                      [:name :service :image :node :state]
                      "/#/tasks/")]))

(defn mount!
  [items]
  (rum/mount (task-list items) (.getElementById js/document "content")))
