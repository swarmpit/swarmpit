(ns swarmpit.component.task.list
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.list :as list]
            [material.component.label :as label]
            [material.component.panel :as panel]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [clojure.contrib.humanize :as humanize]
            [goog.string :as gstring]
            [goog.string.format]
            [rum.core :as rum]))

(enable-console-print!)

(defn- render-percentage
  [val]
  (if (some? val)
    (str (gstring/format "%.2f" val) "%")
    "-"))

(defn- render-capacity
  [val]
  (if (some? val)
    (humanize/filesize val :binary false)
    "-"))

(defn render-item-state [value]
  (case value
    "preparing" (label/yellow value)
    "starting" (label/yellow value)
    "pending" (label/yellow value)
    "new" (label/blue value)
    "ready" (label/blue value)
    "assigned" (label/blue value)
    "accepted" (label/blue value)
    "complete" (label/blue value)
    "running" (label/green value)
    "shutdown" (label/grey value)
    "orphaned" (label/grey value)
    "rejected" (label/red value)
    "failed" (label/red value)))

(def render-metadata
  [{:name    "Name"
    :key     [:taskName]
    :primary true}
   {:name "Image"
    :key  [:repository :image]}
   {:name "Node"
    :key  [:nodeName]}
   {:name      "CPU Usage"
    :key       [:stats :cpuPercentage]
    :render-fn (fn [value _] (render-percentage value))}
   {:name      "Memory Usage"
    :key       [:stats :memoryPercentage]
    :render-fn (fn [value _] (render-percentage value))}
   {:name      "Memory"
    :key       [:stats :memory]
    :render-fn (fn [value _] (render-capacity value))}
   {:name      "Status"
    :key       [:state]
    :render-fn (fn [value _] (render-item-state value))}])

(defn- render-state-fn
  [item]
  (case (:state item)
    "preparing" [:div.Swarmpit-icon-ok icon/sync]
    "starting" [:div.Swarmpit-icon-ok icon/sync]
    "pending" [:div.Swarmpit-icon-warning icon/sync]
    "new" [:div.Swarmpit-icon-accept icon/check-circle]
    "ready" [:div.Swarmpit-icon-accept icon/check-circle]
    "assigned" [:div.Swarmpit-icon-accept icon/check-circle]
    "accepted" [:div.Swarmpit-icon-accept icon/check-circle]
    "complete" [:div.Swarmpit-icon-accept icon/check-circle]
    "running" [:div.Swarmpit-icon-ok icon/check-circle]
    "shutdown" [:div.Swarmpit-icon-info icon/cancel]
    "orphaned" [:div.Swarmpit-icon-info icon/cancel]
    "rejected" [:div.Swarmpit-icon-error icon/error]
    "failed" [:div.Swarmpit-icon-error icon/error]))

(defn- onclick-handler
  [item]
  (routes/path-for-frontend :task-info (select-keys item [:id])))

(defn- tasks-handler
  []
  (ajax/get
    (routes/path-for-backend :tasks)
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:items] response state/form-value-cursor))}))

(defn- init-form-state
  []
  (state/set-value {:loading? false
                    :filter   {:query ""}} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (tasks-handler))))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form
                 mixin/focus-filter [_]
  (let [{:keys [items]} (state/react state/form-value-cursor)
        {:keys [loading? filter]} (state/react state/form-state-cursor)
        filtered-items (list/filter items (:query filter))]
    (comp/mui
      (html
        [:div.Swarmpit-form
         [:div.Swarmpit-form-panel
          (panel/search
            "Search tasks"
            (fn [event]
              (state/update-value [:filter :query] (-> event .-target .-value) state/form-state-cursor)))]
         [:div.Swarmpit-form-context
          (list/view
            render-metadata
            render-state-fn
            (sort-by :serviceName filtered-items)
            onclick-handler)]]))))