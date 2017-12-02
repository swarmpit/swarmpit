(ns swarmpit.component.task.list
  (:require [material.component.label :as label]
            [material.component.panel :as panel]
            [material.component.list-table :as list]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.handler :as handler]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :task :list])

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

(defn- filter-items
  [items name running?]
  (let [is-running (fn [item] (= "running" (:state item)))]
    (if running?
      (filter #(and (string/includes? (:serviceName %) name)
                    (is-running %)) items)
      (filter #(string/includes? (:serviceName %) name) items))))

(defn- data-handler
  []
  (handler/get
    (routes/path-for-backend :tasks)
    {:on-success (fn [response]
                   (state/update-value [:data] response cursor))}))

(defn- init-state
  [tasks]
  (state/set-value {:filter {:serviceName ""
                             :running     true}
                    :data   tasks} cursor))

(def init-state-mixin
  (mixin/init
    (fn [data]
      (init-state data))))

(rum/defc form < rum/reactive
                 init-state-mixin
                 mixin/focus-filter [_]
  (let [{:keys [filter data]} (state/react cursor)
        filtered-items (filter-items data
                                     (:serviceName filter)
                                     (:running filter))]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/text-field
         {:id       "filter"
          :hintText "Filter by service name"
          :onChange (fn [_ v]
                      (state/update-value [:filter :serviceName] v cursor))})
       [:span.form-panel-space]
       (panel/checkbox
         {:checked (false? (:running filter))
          :label   "Show all"
          :onCheck (fn [_ v]
                     (state/update-value [:filter :running] (false? v) cursor))})]]
     (list/table headers
                 (sort-by :serviceName filtered-items)
                 render-item
                 render-item-keys
                 onclick-handler)]))