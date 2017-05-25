(ns swarmpit.component.node.list
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [clojure.string :as string]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form :node :list :filter])

(def headers ["Name" "Status" "Availability" ""])

(defn- filter-items
  "Filter list items based on given predicate"
  [items predicate]
  (filter #(string/includes? (:nodeName %) predicate) items))

(defn- render-item
  [item]
  (if (and (= :leader (key item))
           (val item))
    (comp/label-blue "Leader")
    (val item)))

(rum/defc node-list < rum/reactive [items]
  (let [{:keys [nodeName]} (state/react cursor)
        filtered-items (filter-items items nodeName)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-text-field
         {:hintText "Filter by name"
          :onChange (fn [_ v]
                      (state/update-value :nodeName v cursor))})]]
     (comp/list-table headers
                      filtered-items
                      render-item
                      [[:nodeName] [:state] [:availability] [:leader]]
                      "/#/nodes/")]))

(defn- init-state
  []
  (state/set-value {:nodeName ""} cursor))

(defn mount!
  [items]
  (init-state)
  (rum/mount (node-list items) (.getElementById js/document "content")))
