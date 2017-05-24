(ns swarmpit.component.node.list
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [clojure.string :as string]
            [rum.core :as rum]
            [sablono.core :refer-macros [html]]))

(enable-console-print!)

(def cursor [:form :node :list])

(def headers ["Name" "Status" "Availability" ""])

(defn- filter-items
  "Filter list items based on given predicate"
  [items predicate]
  (filter #(string/includes? (:name %) predicate) items))

(defn- render-item
  [item]
  (if (and (= :leader (key item))
           (val item))
    (html [:span.label.label-leader "Leader"])
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
                      "/#/nodes/"
                      nil)]))

(defn mount!
  [items]
  (rum/mount (node-list items) (.getElementById js/document "content")))
