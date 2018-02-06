(ns swarmpit.component.node.list
  (:require [material.icon :as icon]
            [material.component.label :as label]
            [material.component.panel :as panel]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [material.component.list-table :as list]
            [swarmpit.component.handler :as handler]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form])

(defn- node-item-state [value]
  (case value
    "ready" (label/green value)
    "down" (label/red value)))

(defn- node-item-states [item]
  [:div.node-item-states
   [:span.node-item-state (node-item-state (:state item))]
   (when (:leader item)
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
     [:a {:href  (str "/#/nodes/" (:id item))
          :style {:color          "inherit"
                  :textDecoration "inherit"}}
      (node-item-header item)
      (node-item-states item)
      [:div
       [:span.node-item-secondary "ip: " (:address item)]]
      [:div
       [:span.node-item-secondary "version: " (:engine item)]]
      [:div
       [:span.node-item-secondary "availability: " (:availability item)]]]]))

(defn- nodes-handler
  []
  (handler/get
    (routes/path-for-backend :nodes)
    {:on-success (fn [response]
                   (state/update-value [:items] response cursor))}))

(defn- init-state
  []
  (state/set-value {:filter {:query ""}} cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-state)
      (nodes-handler))))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form
                 mixin/focus-filter [_]
  (let [{:keys [filter items]} (state/react cursor)
        filtered-items (list/filter items (:query filter))]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/text-field
         {:id       "filter"
          :hintText "Search nodes"
          :onChange (fn [_ v]
                      (state/update-value [:filter :query] v cursor))})]]
     [:div.content-grid.mdl-grid
      (->> (sort-by :nodeName filtered-items)
           (map #(node-item %)))]]))
