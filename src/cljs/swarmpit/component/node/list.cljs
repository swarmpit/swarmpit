(ns swarmpit.component.node.list
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.component.state :as state]
            [clojure.string :as string]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :node :list :filter])

(defn- filter-items
  "Filter list items based on given predicate"
  [items predicate]
  (filter #(string/includes? (:nodeName %) predicate) items))

(defn form-state [value]
  (case value
    "ready" (comp/label-green value)
    "down" (comp/label-red value)))

(defn- node-item
  [item]
  (html
    [:div.mdl-cell.node-item
     [:div
      [:span
       [:svg.node-item-ico {::width "24" :height "24" :fill "rgb(117, 117, 117)"}
        [:path {:d icon/docker}]]]
      [:span [:b (:nodeName item)]]]
     [:div.node-item-states
      [:span.node-item-state (form-state (:state item))]
      (if (:leader item)
        [:span.node-item-state (comp/label-blue "leader")])]
     [:div
      [:span "[ " (:role item) " ]"]]
     [:div
      [:span.node-item-secondary "address: " (:address item)]]
     [:div
      [:span.node-item-secondary "engine: " (:engine item)]]
     [:div
      [:span.node-item-secondary "availability: " (:availability item)]]]))

(rum/defc node-list < rum/reactive [items]
  (let [{:keys [nodeName]} (state/react cursor)
        filtered-items (filter-items items nodeName)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-text-field
         {:hintText "Filter by name"
          :onChange (fn [_ v]
                      (state/update-value [:nodeName] v cursor))})]]
     [:div.content-grid.mdl-grid
      (->> (sort-by :nodeName filtered-items)
           (map #(node-item %)))]]))

(defn- init-state
  []
  (state/set-value {:nodeName ""} cursor))

(defn mount!
  [items]
  (init-state)
  (rum/mount (node-list items) (.getElementById js/document "content")))
