(ns swarmpit.component.task.list
  (:require [material.component :as comp]
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
    "shutdown" (label/info value)
    "orphaned" (label/info value)
    "rejected" (label/red value)
    "failed" (label/red value)))

(defn- render-item
  [item _]
  (let [value (val item)]
    (case (key item)
      :state (render-item-state value)
      :cpuPercentage (render-percentage value)
      :memoryPercentage (render-percentage value)
      :memory (render-capacity value)
      value)))

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
          (list/responsive-table
            render-metadata
            nil
            (sort-by :serviceName filtered-items)
            onclick-handler)]]))))