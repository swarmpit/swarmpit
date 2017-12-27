(ns swarmpit.component.task.list
  (:require [material.component.label :as label]
            [material.component.panel :as panel]
            [material.component.list-table :as list]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.handler :as handler]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form])

(def headers [{:name  "Name"
               :width "20%"}
              {:name  "Service"
               :width "20%"}
              {:name  "Image"
               :width "30%"}
              {:name  "Node"
               :width "15%"}
              {:name  "Status"
               :width "15%"}])

(def render-item-keys
  [[:taskName] [:serviceName] [:repository :image] [:nodeName] [:state]])

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

(defn- render-item
  [item _]
  (let [value (val item)]
    (if (= :state (key item))
      (render-item-state value)
      (val item))))

(defn- onclick-handler
  [item]
  (routes/path-for-frontend :task-info (select-keys item [:id])))

(defn- tasks-handler
  []
  (handler/get
    (routes/path-for-backend :tasks)
    {:on-success (fn [response]
                   (state/update-value [:items] response cursor))}))

(defn- init-state
  []
  (state/set-value {:filter {:query ""}} cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-state)
      (tasks-handler))))

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
          :hintText "Search tasks"
          :onChange (fn [_ v]
                      (state/update-value [:filter :query] v cursor))})]]
     (list/table headers
                 (sort-by :serviceName filtered-items)
                 (nil? items)
                 render-item
                 render-item-keys
                 onclick-handler)]))