(ns swarmpit.component.node.list
  (:require [material.icon :as icon]
            [material.component.label :as label]
            [material.component.panel :as panel]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [material.component.list-table :as list]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]
            [clojure.contrib.humanize :as humanize]))

(enable-console-print!)

(defn- node-item-state [value]
  (case value
    "ready" (label/green value)
    "down" (label/red value)))

(defn- node-item-states [item]
  [:div.node-item-states
   [:span.node-item-state (node-item-state (:state item))]
   (when (:leader item)
     [:span.node-item-state (label/blue "leader")])
   [:span.node-item-state (label/blue (:role item))]
   [:span.node-item-state (if (= "active" (:availability item))
                            (label/blue "active")
                            (label/yellow (:availability item)))]])

(defn- node-item-header [item]
  [:div
   [:span
    [:svg.node-item-ico {:width  "24"
                         :height "24"
                         :fill   "rgb(117, 117, 117)"}
     [:path {:d (icon/os (:os item))}]]]
   [:span [:b (:nodeName item)]]])

(defn resources
  [node]
  (let [cpu (-> node :resources :cpu (int))
        memory-bytes (-> node :resources :memory (* 1024 1024))]
    [cpu " " (clojure.contrib.inflect/pluralize-noun cpu "core") ", "
     (humanize/filesize memory-bytes :binary false) " memory"]))

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
       [:span.node-item-secondary (resources item)]]
      [:div
       [:span.node-item-secondary "docker " (:engine item)]]
      [:div
       [:span.node-item-secondary (:address item)]]]]))

(defn- nodes-handler
  []
  (ajax/get
    (routes/path-for-backend :nodes)
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:items] response state/form-value-cursor))}))

(defn- init-form-state
  []
  (state/set-value {:filter {:query ""}} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (nodes-handler))))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form
                 mixin/focus-filter [_]
  (let [{:keys [items]} (state/react state/form-value-cursor)
        {:keys [filter]} (state/react state/form-state-cursor)
        filtered-items (list/filter items (:query filter))]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/text-field
         {:id       "filter"
          :hintText "Search nodes"
          :onChange (fn [_ v]
                      (state/update-value [:filter :query] v state/form-state-cursor))})]]
     [:div.content-grid.mdl-grid
      (->> (sort-by :nodeName filtered-items)
           (map #(node-item %)))]]))
