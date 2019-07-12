(ns swarmpit.component.task.info
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.label :as label]
            [material.component.form :as form]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.common :as common]
            [swarmpit.component.plot :as plot]
            [swarmpit.event.source :as event]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [clojure.contrib.humanize :as humanize]
            [rum.core :as rum]))

(enable-console-print!)

(def plot-id "taskStats")

(defonce stats (atom nil))

(defn task-cpu-plot [stats-ts]
  (plot/single plot-id
               stats-ts
               :cpu
               "Container CPU Usage"
               "CPU Load) [%]"))

(defn task-ram-plot [stats-ts]
  (plot/single plot-id
               stats-ts
               :memory
               "Container Memory Usage"
               "Memory Used [MB]"))

(defn- event-handler
  [task-id]
  (fn [event]
    (let [task (->> event
                    (filter #(= task-id (:id %)))
                    (first))]
      (if task
        (state/set-value task state/form-value-cursor)
        (state/update-value [:state] "removed" state/form-value-cursor)))))

(defn- task-stats-handler
  [task-name]
  (ajax/get
    (routes/path-for-backend :task-ts {:name task-name})
    {:state      [:stats-loading?]
     :on-success (fn [{:keys [response]}]
                   (task-cpu-plot response)
                   (reset! stats response))}))

(defn- task-handler
  [route]
  (ajax/get
    (routes/path-for-backend :task (:params route))
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (task-stats-handler (:taskName response))
                   (state/set-value response state/form-value-cursor)
                   (event/open! (merge route {:params {:serviceName (:serviceName response)}})
                                (event-handler (:id response))))}))

(defn form-state [value]
  (case value
    "preparing" (label/pulsing value)
    "starting" (label/pulsing value)
    "pending" (label/yellow value)
    "new" (label/blue value)
    "ready" (label/blue value)
    "assigned" (label/blue value)
    "accepted" (label/blue value)
    "complete" (label/blue value)
    "running" (label/green value)
    "shutdown" (label/grey value)
    "orphaned" (label/grey value)
    "removed" (label/grey value)
    "rejected" (label/red value)
    "failed" (label/red value)))

(rum/defc form-general < rum/static [{:keys [id taskName nodeName state status createdAt updatedAt repository serviceName logdriver stats]}]
  (comp/card
    {:className "Swarmpit-form-card Swarmpit-form-card-single"}
    (comp/card-header
      {:title     taskName
       :className "Swarmpit-form-card-header Swarmpit-card-header-responsive-title"
       :subheader (common/form-subheader
                    (:image repository)
                    (:imageDigest repository))})
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (html
        [:div
         (when (and stats (= "running" state))
           [:div {:class "Swarmpit-node-stat"
                  :key   (str "node-card-stat-cpu")}
            (common/resource-pie
              (get-in stats [:cpuPercentage])
              (str (-> stats :cpuPercentage (Math/ceil)) "% cpu")
              (str "graph-cpu"))
            (common/resource-pie
              (get-in stats [:memoryPercentage])
              (str (humanize/filesize (-> stats :memory) :binary false) " ram")
              (str "graph-memory"))])]))
    (when (:error status)
      (comp/card-content
        {}
        (html
          [:div.Swarmpit-task-error
           (icon/error {:className "Swarmpit-task-error-icon"})
           [:div (:error status)]])))
    (comp/card-content
      {}
      (form/item-labels
        [(form-state state)]))
    (comp/card-actions
      {}
      (comp/button
        {:size  "small"
         :color "primary"
         :href  (routes/path-for-frontend :service-info {:id serviceName})}
        "See service")
      (when nodeName
        (comp/button
          {:size  "small"
           :color "primary"
           :href  (routes/path-for-frontend :node-info {:id nodeName})}
          "See node"))
      (comp/button
        {:size     "small"
         :color    "primary"
         :disabled (not (contains? #{"json-file" "journald"} logdriver))
         :href     (routes/path-for-frontend :service-task-log {:id serviceName :taskId id})}
        "View log"))
    (comp/divider
      {})
    (comp/card-content
      {:style {:paddingBottom "16px"}}
      (form/item-date createdAt updatedAt)
      (form/item-id id))))

(rum/defc form-stats < rum/reactive []
  (let [{:keys [tab stats-loading?]} (state/react state/form-state-cursor)
        stats (rum/react stats)
        stats-empty? (empty? (:time stats))]
    (comp/card
      {:className "Swarmpit-card"}
      (comp/card-content
        {:className "Swarmpit-table-card-content"}
        (comp/tabs
          {:className      "Swarmpit-service-tabs"
           :value          tab
           :onChange       (fn [e v]
                             (case v
                               0 (task-cpu-plot stats)
                               1 (task-ram-plot stats))
                             (state/update-value [:tab] v state/form-state-cursor))
           :fullWidth      true
           :indicatorColor "primary"
           :textColor      "primary"
           :centered       true}
          (comp/tab
            {:label "CPU"})
          (comp/tab
            {:label "MEMORY"})))
      (when (and (false? stats-loading?) stats-empty?)
        (comp/card-content
          {}
          (form/message "No stats available.")))
      (comp/card-content
        {:className "Swarmpit-table-card-content"}
        (html [:div {:id    plot-id
                     :style (when stats-empty? {:height "0"})}])))))

(defn form-general-grid [task]
  (comp/grid
    {:item true
     :xs   12}
    (form-general task)))

(defn form-stats-grid []
  (comp/grid
    {:item true
     :xs   12}
    (form-stats)))

(rum/defc form-info < rum/reactive [item]
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div.Swarmpit-form-context
        (comp/hidden
          {:xsDown         true
           :implementation "js"}
          (comp/grid
            {:container true
             :spacing   16}
            (comp/grid
              {:item true
               :sm   6
               :md   4}
              (comp/grid
                {:container true
                 :spacing   16}
                (form-general-grid item)))
            (comp/grid
              {:item true
               :sm   6
               :md   8}
              (comp/grid
                {:container true
                 :spacing   16}
                (form-stats-grid)))))
        (comp/hidden
          {:smUp           true
           :implementation "js"}
          (comp/grid
            {:container true
             :spacing   16}
            (form-general-grid item)
            (form-stats-grid)))]])))

(def unsubscribe-form
  {:will-unmount (fn [state]
                   (event/close!)
                   state)})

(defn- init-form-state
  []
  (state/set-value {:loading?       true
                    :stats-loading? true
                    :tab            0} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [route]
      (init-form-state)
      (task-handler route))))

(rum/defc form < rum/reactive
                 unsubscribe-form
                 mixin-init-form [_]
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info item))))
