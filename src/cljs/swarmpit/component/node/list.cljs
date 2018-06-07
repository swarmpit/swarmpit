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
            [goog.string :as gstring]
            [goog.string.format]
            [clojure.contrib.humanize :as humanize]))

(enable-console-print!)

(defn- node-item-header [item]
  [:div
   [:span
    [:svg.node-item-ico {:width  "24"
                         :height "24"
                         :fill   "rgb(117, 117, 117)"}
     [:path {:d (icon/os (:os item))}]]]
   [:span [:b (:nodeName item)]]])

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

(defn- node-item-engine [item]
  [:div
   [:span.node-item-secondary "docker " (:engine item)]])

(defn- node-item-address [item]
  [:div
   [:span.node-item-secondary (:address item)]])

(defn- node-item-usage [stat]
  (let [stat-string (str (gstring/format "%.2f" stat) "%")]
    (cond
      (< stat 75) [:td {:style {:color "#509E50"}} stat-string]
      (> stat 90) [:td {:style {:color "rgb(244, 67, 54)"}} stat-string]
      :else [:td {:style {:color "#9e931b"}} stat-string])))

(defn- node-item-stats [item]
  (let [cpu (-> item :resources :cpu (int))
        memory-bytes (-> item :resources :memory (* 1024 1024))
        disk-bytes (-> item :stats :disk :total)]
    [:div
     [:table.node-progress-table
      [:tr
       [:td]
       [:td]
       [:td]]
      [:tr.node-progress-table-name
       [:td "CPU"]
       [:td "DISK"]
       [:td "MEMORY"]]
      [:tr
       [:td (str cpu " " (clojure.contrib.inflect/pluralize-noun cpu "core"))]
       [:td (humanize/filesize disk-bytes :binary false)]
       [:td (humanize/filesize memory-bytes :binary false)]]
      [:tr.node-progress-table-usage
       (node-item-usage (get-in item [:stats :cpu :usedPercentage]))
       (node-item-usage (get-in item [:stats :disk :usedPercentage]))
       (node-item-usage (get-in item [:stats :memory :usedPercentage]))]]]))

(defn- node-item
  [item]
  (html
    [:div.mdl-cell.node-item {:key (:id item)}
     [:a {:href  (str "/#/nodes/" (:id item))
          :style {:color          "inherit"
                  :textDecoration "inherit"}}
      (node-item-header item)
      (node-item-engine item)
      (node-item-address item)
      (node-item-states item)
      (node-item-stats item)]]))

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
