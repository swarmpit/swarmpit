(ns swarmpit.component.task.list
  (:require [material.component :as comp]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [swarmpit.storage :as storage]
            [clojure.walk :refer [keywordize-keys]]
            [ajax.core :as ajax]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :task :list])

(def headers ["Name" "Service" "Image" "Node" "Status"])

(def render-item-keys
  [[:taskName] [:serviceName] [:repository :image] [:node :nodeName] [:state]])

(defn render-item-state [value]
  (case value
    "preparing" (comp/label-yellow value)
    "starting" (comp/label-yellow value)
    "pending" (comp/label-yellow value)
    "new" (comp/label-blue value)
    "ready" (comp/label-blue value)
    "assigned" (comp/label-blue value)
    "accepted" (comp/label-blue value)
    "complete" (comp/label-blue value)
    "running" (comp/label-green value)
    "shutdown" (comp/label-grey value)
    "rejected" (comp/label-red value)
    "failed" (comp/label-red value)))

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
  (ajax/GET (routes/path-for-backend :tasks)
            {:headers {"Authorization" (storage/get "token")}
             :handler (fn [response]
                        (keywordize-keys response)
                        (let [resp (keywordize-keys response)]
                          (state/update-value [:data] resp cursor)))}))

(defn- init-state
  [tasks]
  (state/set-value {:filter {:serviceName ""
                             :running     true}
                    :data   tasks} cursor))

(def refresh-state-mixin
  (mixin/refresh data-handler))

(def init-state-mixin
  (mixin/init
    (fn [data]
      (init-state data))))

(rum/defc form < rum/reactive
                 init-state-mixin
                 refresh-state-mixin [_]
  (let [{:keys [filter data]} (state/react cursor)
        filtered-items (filter-items data
                                     (:serviceName filter)
                                     (:running filter))]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-text-field
         {:hintText "Filter by service name"
          :onChange (fn [_ v]
                      (state/update-value [:filter :serviceName] v cursor))})
       [:span.form-panel-space]
       (comp/panel-checkbox
         {:checked (false? (:running filter))
          :label   "Show all"
          :onCheck (fn [_ v]
                     (state/update-value [:filter :running] (false? v) cursor))})]]
     (comp/list-table headers
                      (sort-by :serviceName filtered-items)
                      render-item
                      render-item-keys
                      onclick-handler)]))