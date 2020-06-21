(ns swarmpit.component.task.list
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [material.component.list.basic :as list]
            [material.component.list.util :as list-util]
            [material.component.label :as label]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.common :as common]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [clojure.contrib.inflect :as inflect]
            [goog.string.format]
            [rum.core :as rum]))

(enable-console-print!)

(defn render-item-state [value]
  (html
    [:span.Swarmpit-table-status
     (case value
       "preparing" (label/base value "pulsing")
       "starting" (label/base value "pulsing")
       "pending" (label/base value "yellow")
       "new" (label/base value "blue")
       "ready" (label/base value "blue")
       "assigned" (label/base value "blue")
       "accepted" (label/base value "blue")
       "complete" (label/base value "blue")
       "running" (label/base value "green")
       "shutdown" (label/base value "grey")
       "orphaned" (label/base value "grey")
       "removed" (label/base value "grey")
       "rejected" (label/base value "red")
       "failed" (label/base value "red"))]))

(defn- render-item-name [item]
  (html
    [:div
     [:div
      [:span
       (:taskName item)]]
     [:div
      [:span.Swarmpit-table-cell-secondary
       (get-in item [:repository :image])]]]))

(defn- render-item-cpu-usage [item]
  (if (:stats item)
    (html
      [:div
       [:div.Swarmpit-task-memory-usage
        [:span (common/render-percentage (get-in item [:stats :cpuPercentage]))]
        [:span.Swarmpit-table-cell-secondary
         (str (common/render-cores (get-in item [:stats :cpu])) "vCPU")]]])
    (html [:span "-"])))

(defn- render-item-memory-usage [item]
  (if (:stats item)
    (html
      [:div
       [:div.Swarmpit-task-memory-usage
        [:span (common/render-percentage (get-in item [:stats :memoryPercentage]))]
        [:span.Swarmpit-table-cell-secondary
         (common/render-capacity (get-in item [:stats :memory]) true)]]])
    (html [:span "-"])))

(defn- render-item-status [item]
  (let [error (get-in item [:status :error])]
    (if error
      (comp/tooltip
        {:title           error
         :TransitionProps {:className "Swarmpit-table-error-tooltip"}
         :placement       "left"}
        (render-item-state (:state item)))
      (render-item-state (:state item)))))

(def render-metadata
  {:table {:summary [{:name      "Task"
                      :render-fn (fn [item] (render-item-name item))}
                     {:name      "Node"
                      :render-fn (fn [item] (:nodeName item))}
                     {:name      "CPU Usage"
                      :tooltip   "Task cpu usage per Limit (resource/node)"
                      :render-fn (fn [item] (render-item-cpu-usage item))}
                     {:name      "Memory Usage"
                      :tooltip   "Task memory usage per Limit (resource/node)"
                      :render-fn (fn [item] (render-item-memory-usage item))}
                     {:name      "Last update"
                      :render-fn (fn [item] (form/item-date (:updatedAt item)))}
                     {:name      ""
                      :status    true
                      :render-fn (fn [item] (render-item-status item))}]}
   :list  {:primary   (fn [item] (:taskName item))
           :secondary (fn [item] (get-in item [:repository :image]))
           :status-fn (fn [item] (render-item-state (:state item)))}})

(defn onclick-handler
  [item]
  (routes/path-for-frontend :task-info (select-keys item [:id])))

(defn- tasks-handler
  []
  (ajax/get
    (routes/path-for-backend :tasks)
    {:state      [:loading?]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (state/update-value [:items] response state/form-value-cursor)))}))

(defn form-search-fn
  [event]
  (state/update-value [:query] (-> event .-target .-value) state/search-cursor))

(defn- init-form-state
  []
  (state/set-value {:loading?    false
                    :filter      {:state nil}
                    :filterOpen? false} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (tasks-handler))))

(rum/defc form-filters < rum/static [filterOpen? {:keys [state] :as filter}]
  (common/list-filters
    filterOpen?
    (comp/text-field
      {:fullWidth       true
       :label           "State"
       :helperText      "Filter by task state"
       :select          true
       :value           state
       :variant         "outlined"
       :margin          "normal"
       :InputLabelProps {:shrink true}
       :onChange        #(state/update-value [:filter :state] (-> % .-target .-value) state/form-state-cursor)}
      (comp/menu-item
        {:key   "running"
         :value "running"} "running")
      (comp/menu-item
        {:key   "failed"
         :value "failed"} "failed")
      (comp/menu-item
        {:key   "shutdown"
         :value "shutdown"} "shutdown"))))

(def toolbar-render-metadata
  [{:name     "Show filters"
    :onClick  #(state/update-value [:filterOpen?] true state/form-state-cursor)
    :icon     (icon/filter-list)
    :icon-alt (icon/filter-list)
    :variant  "outlined"
    :color    "default"}])

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form
                 mixin/focus-filter [_]
  (let [{:keys [items]} (state/react state/form-value-cursor)
        {:keys [query]} (state/react state/search-cursor)
        {:keys [loading? filter filterOpen?]} (state/react state/form-state-cursor)
        filtered-items (->> (list-util/filter items query)
                            (clojure.core/filter #(if (some? (:state filter))
                                                    (= (:state filter) (:state %))
                                                    true))
                            (sort-by :createdAt)
                            (reverse))]

    (progress/form
      loading?
      (comp/box
        {}
        (common/list "Tasks"
                     items
                     filtered-items
                     render-metadata
                     onclick-handler
                     toolbar-render-metadata)
        (form-filters filterOpen? filter)))))
