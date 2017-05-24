(ns swarmpit.component.node.list
  (:require [material.component :as comp]
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
        filtered-items (filter-items items predicate)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-text-field
         {:hintText "Filter by name"
          :onChange (fn [_ v]
                      (state/update-value :predicate v cursor))})]]
     (comp/list-table headers
                      filtered-items
                      render-item
                      [:name :state :availability :leader]
                      "/#/nodes/")]))

(defn mount!
  [items]
  (rum/mount (node-list items) (.getElementById js/document "content")))
