(ns swarmpit.component.task.list
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.list.basic :as list]
            [material.component.list.util :as list-util]
            [material.component.label :as label]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.progress :as progress]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [clojure.contrib.humanize :as humanize]
            [goog.string :as gstring]
            [goog.string.format]
            [rum.core :as rum]
            [swarmpit.component.common :as common]))

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
    "preparing" (label/pulsing value)
    "starting" (label/pulsing value)
    "pending" (label/pulsing value)
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

(defn- render-item-name [item]
  (html
    [:div
     [:div
      [:span
       (:taskName item)]]
     [:div
      [:span.Swarmpit-table-cell-secondary
       (get-in item [:repository :image])]]]))

(defn- render-item-memory-usage [item]
  (if (:stats item)
    (html
      [:div
       [:div
        [:span
         (render-percentage (get-in item [:stats :memoryPercentage]))]
        [:span " "]
        [:span.Swarmpit-table-cell-secondary
         (render-capacity (get-in item [:stats :memory]))]]])
    (html [:span "-"])))

(def render-metadata
  {:table {:summary [{:name      "Task"
                      :render-fn (fn [item] (render-item-name item))}
                     {:name      "Node"
                      :render-fn (fn [item] (:nodeName item))}
                     {:name      "CPU Usage"
                      :render-fn (fn [item] (render-percentage (get-in item [:stats :cpuPercentage])))}
                     {:name      "Memory Usage"
                      :render-fn (fn [item] (render-item-memory-usage item))}
                     {:name      ""
                      :render-fn (fn [item] (list/status (render-item-state (:state item))))}]}
   :list  {:primary   (fn [item] (:taskName item))
           :secondary (fn [item] (get-in item [:repository :image]))
           :status-fn (fn [item] (render-item-state (:state item)))}})

(defn onclick-handler
  [item]
  (dispatch! (routes/path-for-frontend :task-info (select-keys item [:id]))))

(defn- tasks-handler
  []
  (ajax/get
    (routes/path-for-backend :tasks)
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:items] response state/form-value-cursor))}))

(defn form-search-fn
  [event]
  (state/update-value [:filter :query] (-> event .-target .-value) state/form-state-cursor))

(defn- init-form-state
  []
  (state/set-value {:loading? false
                    :filter   {:query ""}} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (tasks-handler))))

(defn toolbar-render-metadata
  [filter]
  {:filters [{:checked  (:running filter)
              :name     "Running state"
              :disabled (or (:shutdown filter) (:failed filter) false)
              :onClick  #(state/update-value [:filter :running] (not (:running filter)) state/form-state-cursor)}
             {:checked  (:shutdown filter)
              :name     "Shutdown state"
              :disabled (or (:running filter) (:failed filter) false)
              :onClick  #(state/update-value [:filter :shutdown] (not (:shutdown filter)) state/form-state-cursor)}
             {:checked  (:failed filter)
              :name     "Failed state"
              :disabled (or (:running filter) (:shutdown filter) false)
              :onClick  #(state/update-value [:filter :failed] (not (:failed filter)) state/form-state-cursor)}]})

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form
                 mixin/focus-filter [_]
  (let [{:keys [items]} (state/react state/form-value-cursor)
        {:keys [loading? filter]} (state/react state/form-state-cursor)
        filtered-items (->> (list-util/filter items (:query filter))
                            (clojure.core/filter #(if (:running filter)
                                                    (= "running" (:state %)) true))
                            (clojure.core/filter #(if (:shutdown filter)
                                                    (= "shutdown" (:state %)) true))
                            (clojure.core/filter #(if (:failed filter)
                                                    (= "failed" (:state %)) true)))]
    (progress/form
      loading?
      (common/list "Tasks"
                   items
                   filtered-items
                   render-metadata
                   onclick-handler
                   (toolbar-render-metadata filter)))))
