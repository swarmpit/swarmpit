(ns swarmpit.component.task.list
  (:require [material.component.label :as label]
            [material.component.panel :as panel]
            [material.component.list-table :as list]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

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
  (ajax/get
    (routes/path-for-backend :tasks)
    {:progress   [:loading?]
     :on-success (fn [response]
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
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/text-field
         {:id       "filter"
          :hintText "Search tasks"
          :onChange (fn [_ v]
                      (state/update-value [:filter :query] v state/form-state-cursor))})]]
     (list/table headers
                 (sort-by :serviceName filtered-items)
                 loading?
                 render-item
                 render-item-keys
                 onclick-handler)]))