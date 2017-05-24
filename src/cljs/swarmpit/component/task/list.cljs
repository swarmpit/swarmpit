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
      (filter #(and (string/includes? (:service %) name)
                    (is-running %)) items)
      (filter #(string/includes? (:service %) name) items))))

(defn- render-item
  [item]
  (if (= :state (key item))
    (case (val item)
      "running" (html [:span.label.label-running (val item)])
      "shutdown" (html [:span.label.label-shutdown (val item)])
      "failed" (html [:span.label.label-failed (val item)]))
    (val item)))

(rum/defc task-list < rum/reactive [items]
  (let [{:keys [name running]} (state/react cursor)
        filtered-items (filter-items items name running)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-text-field
         {:hintText "Filter by service name"
          :onChange (fn [_ v]
                      (state/update-value :name v cursor))})
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
                      [:name :service :image :node :state]
                      "/#/tasks/"
                      nil)]))

(defn mount!
  [items]
  (rum/mount (task-list items) (.getElementById js/document "content")))
