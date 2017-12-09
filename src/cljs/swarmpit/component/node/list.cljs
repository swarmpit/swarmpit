(ns swarmpit.component.node.list
  (:require [material.component.label :as label]
            [material.component.panel :as panel]
            [material.icon :as icon]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.handler :as handler]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form])

(defn- filter-items
  "Filter list items based on given predicate"
  [items predicate]
  (filter #(string/includes? (:nodeName %) predicate) items))

(defn- node-item-state [value]
  (case value
    "ready" (label/green value)
    "down" (label/red value)))

(defn- node-item-states [item]
  [:div.node-item-states
   [:span.node-item-state (node-item-state (:state item))]
   (if (:leader item)
     [:span.node-item-state (label/blue "leader")])
   [:span.node-item-state (label/blue (:role item))]])

(defn- node-item-header [item]
  [:div
   [:span
    [:svg.node-item-ico {:width  "24"
                         :height "24"
                         :fill   "rgb(117, 117, 117)"}
     [:path {:d icon/docker}]]]
   [:span [:b (:nodeName item)]]])

(defn- node-item
  [item]
  (html
    [:div.mdl-cell.node-item {:key (:id item)}
     (node-item-header item)
     (node-item-states item)
     [:div
      [:span.node-item-secondary "ip: " (:address item)]]
     [:div
      [:span.node-item-secondary "version: " (:engine item)]]
     [:div
      [:span.node-item-secondary "availability: " (:availability item)]]]))

(defn- nodes-handler
  []
  (handler/get
    (routes/path-for-backend :nodes)
    {:on-success (fn [response]
                   (state/update-value [:items] response cursor))}))

(defn- init-state
  []
  (state/set-value {:filter {:nodeName ""}} cursor))

(def mixin-init-state
  (mixin/init-state
    (fn []
      (init-state)
      (nodes-handler))))

(rum/defc form < rum/reactive
                 mixin-init-state
                 mixin/focus-filter []
  (let [{:keys [filter items]} (state/react cursor)
        filtered-items (filter-items items (:nodeName filter))]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/text-field
         {:id       "filter"
          :hintText "Filter by name"
          :onChange (fn [_ v]
                      (state/update-value [:filter :nodeName] v cursor))})]]
     [:div.content-grid.mdl-grid
      (->> (sort-by :nodeName filtered-items)
           (map #(node-item %)))]]))
