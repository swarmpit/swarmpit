(ns swarmpit.component.node.list
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [clojure.string :as string]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form :node :list :filter])

(def headers ["Name" "Availability" "Status" ""])

(defn- filter-items
  "Filter list items based on given predicate"
  [items predicate]
  (filter #(string/includes? (:nodeName %) predicate) items))

(def render-item-keys
  [[:nodeName] [:availability] [:state] [:leader]])

(defn- render-item
  [item]
  (let [value (val item)]
    (case (key item)
      :state (comp/label-green value)
      :leader (if (val item)
                (comp/label-blue "Leader"))
      value)))

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
                      render-item-keys
                      "/#/nodes/")]))

(defn- init-state
  []
  (state/set-value {:nodeName ""} cursor))

(defn mount!
  [items]
  (init-state)
  (rum/mount (node-list items) (.getElementById js/document "content")))
