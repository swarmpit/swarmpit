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

(def plot-cpu-id "taskCpuStats")

(def plot-ram-id "taskRamStats")

(defn task-cpu-plot [stats-ts]
  (plot/single plot-cpu-id
               stats-ts
               :cpu
               "CPU Usage"
               "[%]"))

(defn task-ram-plot [stats-ts]
  (plot/single plot-ram-id
               stats-ts
               :memory
               "Memory Usage"
               "[MB]"))

(defn- event-handler
  [task-id]
  (fn [event]
    (let [task (->> event
                    (filter #(= task-id (:id %)))
                    (first))]
      (if task
        (state/update-value [:task] task state/form-value-cursor)
        (state/update-value [:state] "removed" state/form-value-cursor)))))

(defn- task-stats-handler
  [task-name]
  (ajax/get
    (routes/path-for-backend :task-ts {:name task-name})
    {:state      [:loading? :task-ts]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:task-ts] response state/form-value-cursor))
     :on-error   (fn [_])}))

(defn- task-handler
  [route]
  (ajax/get
    (routes/path-for-backend :task (:params route))
    {:state      [:loading? :task]
     :on-success (fn [{:keys [response]}]
                   (task-stats-handler (:taskName response))
                   (state/update-value [:task] response state/form-value-cursor)
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
    {:className "Swarmpit-form-card"}
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
           [:div.Swarmpit-task-error-message (:error status)]])))
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

(rum/defc form-cpu-stats < rum/static
                           {:did-mount (fn [state _]
                                         (let [ts (first (:rum/args state))]
                                           (task-cpu-plot ts))
                                         state)} [task-ts]
  (comp/card
    (if (empty? task-ts)
      {:className "Swarmpit-card hide"}
      {:className "Swarmpit-card"})
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (html [:div {:id plot-cpu-id}]))))

(rum/defc form-ram-stats < rum/static
                           {:did-mount (fn [state _]
                                         (let [ts (first (:rum/args state))]
                                           (task-ram-plot ts))
                                         state)} [task-ts]
  (comp/card
    (if (empty? task-ts)
      {:className "Swarmpit-card hide"}
      {:className "Swarmpit-card"})
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (html [:div {:id plot-ram-id}]))))

(defn form-general-grid [task]
  (comp/grid
    {:item true
     :xs   12}
    (form-general task)))

(defn form-cpu-stats-grid [task-ts]
  (comp/grid
    {:item true
     :xs   12}
    (form-cpu-stats task-ts)))

(defn form-ram-stats-grid [task-ts]
  (comp/grid
    {:item true
     :xs   12}
    (form-ram-stats task-ts)))

(rum/defc form-info < rum/static [{:keys [task task-ts] :as item}]
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div.Swarmpit-form-context
        (comp/hidden
          {:xsDown         true
           :implementation "js"}
          (comp/grid
            {:container true
             :spacing   2}
            (comp/grid
              {:item true
               :sm   6
               :md   4}
              (comp/grid
                {:container true
                 :spacing   2}
                (form-general-grid task)))
            (comp/grid
              {:item true
               :sm   6
               :md   8}
              (comp/grid
                {:container true
                 :spacing   2}
                (form-cpu-stats-grid task-ts)
                (form-ram-stats-grid task-ts)))))
        (comp/hidden
          {:smUp           true
           :implementation "js"}
          (comp/grid
            {:container true
             :spacing   2}
            (form-general-grid task)
            (form-cpu-stats-grid task-ts)
            (form-ram-stats-grid task-ts)))]])))

(def unsubscribe-form
  {:will-unmount (fn [state]
                   (event/close!)
                   state)})

(defn- init-form-value
  []
  (state/set-value {:task    {}
                    :task-ts []} state/form-value-cursor))

(defn- init-form-state
  []
  (state/set-value {:loading? {:task    true
                               :task-ts true}} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [route]
      (init-form-state)
      (init-form-value)
      (task-handler route))))

(rum/defc form < rum/reactive
                 unsubscribe-form
                 mixin-init-form [_]
  (let [{:keys [loading?]} (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (or (:task loading?)
          (:task-ts loading?))
      (form-info item))))
