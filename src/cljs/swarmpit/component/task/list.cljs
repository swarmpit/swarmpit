(ns swarmpit.component.task.list
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [clojure.string :as string]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :task :list :filter])

(def headers ["Name" "Service" "Image" "Node" "Status"])

(defn form-state [value]
  (case value
    "preparing" (comp/label-yellow value)
    "starting" (comp/label-yellow value)
    "pending" (comp/label-yellow value)

    "new" (comp/label-blue value)
    "ready" (comp/label-blue value)
    "assigned" (comp/label-blue value)
    "running" (comp/label-green value)
    "shutdown" (comp/label-grey value)
    "rejected" (comp/label-red value)
    "failed" (comp/label-red value)))

(defn- filter-items
  "Filter list `items` based on service `name` & `running?` flag"
  [items name running?]
  (let [is-running (fn [item] (= "running" (:state item)))]
    (if running?
      (filter #(and (string/includes? (:serviceName %) name)
                    (is-running %)) items)
      (filter #(string/includes? (:serviceName %) name) items))))

(def render-item-keys
  [[:taskName] [:serviceName] [:repository :image] [:node :nodeName] [:state]])

(defn- render-item
  [item]
  (let [value (val item)]
    (if (= :state (key item))
      (form-state value)
      (val item))))

(rum/defc task-list < rum/reactive [items]
  (let [{:keys [serviceName running]} (state/react cursor)
        filtered-items (filter-items items serviceName running)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-text-field
         {:hintText "Filter by service name"
          :onChange (fn [_ v]
                      (state/update-value [:serviceName] v cursor))})
       [:span.form-panel-space]
       (comp/panel-checkbox
         {:checked (false? running)
          :label   "Show all tasks"
          :onCheck (fn [_ v]
                     (state/update-value [:running] (false? v) cursor))})]]
     (comp/list-table headers
                      filtered-items
                      render-item
                      render-item-keys
                      :task-info)]))

(defn- init-state
  []
  (state/set-value {:serviceName ""
                    :running     true} cursor))

(defn mount!
  [items]
  (init-state)
  (rum/mount (task-list items) (.getElementById js/document "content")))
