(ns swarmpit.component.task.list
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [clojure.string :as string]
            [rum.core :as rum]
            [sablono.core :refer-macros [html]]))

(enable-console-print!)

(def cursor [:form :task :list :filter])

(def headers ["Name" "Service" "Image" "Node" ""])

(defn- filter-items
  "Filter list `items` based on `name` & `running?` flag"
  [items name running?]
  (let [is-running (fn [item] (= "running" (:state item)))]
    (if running?
      (filter #(and (string/includes? (:serviceName %) name)
                    (is-running %)) items)
      (filter #(string/includes? (:serviceName %) name) items))))

(defn- render-item
  [item]
  (if (= :state (key item))
    (case (val item)
      "running" (html [:span.label.label-running (val item)])
      "shutdown" (html [:span.label.label-shutdown (val item)])
      "failed" (html [:span.label.label-failed (val item)]))
    (val item)))

(rum/defc task-list < rum/reactive [items]
  (let [{:keys [serviceName running]} (state/react cursor)
        filtered-items (filter-items items serviceName running)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-text-field
         {:hintText "Filter by service name"
          :onChange (fn [_ v]
                      (state/update-value :serviceName v cursor))})
       [:span.form-panel-space]
       (comp/panel-comp
         "Show all tasks"
         (comp/checkbox
           {:checked (false? running)
            :onCheck (fn [_ v]
                       (state/update-value :running (false? v) cursor))}))]]
     (comp/list-table headers
                      filtered-items
                      render-item
                      [[:taskName] [:serviceName] [:image] [:node :nodeName] [:state]]
                      "/#/tasks/"
                      nil)]))

(defn mount!
  [items]
  (rum/mount (task-list items) (.getElementById js/document "content")))
