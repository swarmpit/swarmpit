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
            [swarmpit.component.toolbar :as toolbar]
            [swarmpit.component.service.log :as log]
            [swarmpit.component.parser :refer [parse-int]]
            [swarmpit.event.source :as event]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [clojure.contrib.inflect :as inflect]
            [rum.core :as rum]))

(enable-console-print!)

(def plot-cpu-id "taskCpuStats")

(def plot-ram-id "taskRamStats")

(defn task-cpu-plot [stats-ts]
  (plot/single plot-cpu-id
               stats-ts
               :cpu
               {:title      "CPU usage"
                :showlegend false
                :yaxis      {:title "[vCPU]"}}))

(defn task-ram-plot [stats-ts]
  (plot/single plot-ram-id
               stats-ts
               :memory
               {:title      "Memory usage"
                :showlegend false
                :yaxis      {:title "[MiB]"}}))

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
    "failed" (label/base value "red")))

(rum/defc form-stats [{:keys [cpuPercentage cpuLimit cpu memoryPercentage memoryLimit memory] :as stats}]
  (comp/box
    {:class "Swarmpit-stat"}
    (common/resource-pie
      {:value cpu
       :limit cpuLimit
       :usage cpuPercentage
       :type  :cpu}
      (str cpuLimit " vCPU")
      (str "graph-cpu"))
    (common/resource-pie
      {:value memory
       :limit memoryLimit
       :usage memoryPercentage
       :type  :memory}
      (str (common/render-capacity memoryLimit true) " ram")
      (str "graph-memory"))))

(rum/defc form-general < rum/static [{:keys [id taskName nodeName state status createdAt updatedAt repository serviceName logdriver stats]}]
  (comp/card
    {:className "Swarmpit-form-card"}
    (comp/card-header
      {:subheader (form-state state)})
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (when (and stats (= "running" state))
        (form-stats stats)))
    (when (:error status)
      (comp/card-content
        {}
        (comp/box
          {:className "Swarmpit-task-error"}
          (icon/error {:className "Swarmpit-task-error-icon"})
          (comp/box
            {:className "Swarmpit-task-error-message"} (:error status)))))
    (form/item-main "ID" id false)
    (form/item-main "Image" (:image repository))
    (when (:imageDigest repository)
      (form/item-main "Image digest" (:imageDigest repository)))
    (form/item-main "Created" (form/item-date createdAt))
    (form/item-main "Last update" (form/item-date updatedAt))
    (comp/divider {})
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
         :href     (routes/path-for-frontend :task-info
                                             {:id id}
                                             {:log 1})}
        "View log"))))

(defn form-cpu-stats-callback
  [state]
  (let [ts (first (:rum/args state))]
    (task-cpu-plot ts))
  state)

(rum/defc form-cpu-stats < rum/static
                           {:did-mount    form-cpu-stats-callback
                            :will-unmount (fn [state] (plot/purge plot-cpu-id) state)}
  [task-ts]
  (comp/card
    (if (empty? task-ts)
      {:className "Swarmpit-card hide"}
      {:className "Swarmpit-card"})
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (html [:div {:id plot-cpu-id}]))))

(defn form-ram-stats-callback
  [state]
  (let [ts (first (:rum/args state))]
    (task-ram-plot ts))
  state)

(rum/defc form-ram-stats < rum/static
                           {:did-mount    form-ram-stats-callback
                            :will-unmount (fn [state] (plot/purge plot-ram-id) state)}
  [task-ts]
  (comp/card
    (if (empty? task-ts)
      {:className "Swarmpit-card hide"}
      {:className "Swarmpit-card"})
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (html [:div {:id plot-ram-id}]))))

(rum/defc form-info < rum/static [{:keys [task task-ts] :as item} log]
  (comp/mui
    (html
      [:div.Swarmpit-form
       (log/dialog (:serviceName task) (:id task) (= 1 log))
       [:div.Swarmpit-form-toolbar
        (comp/container
          {:maxWidth  "md"
           :className "Swarmpit-container"}
          (comp/grid
            {:container true
             :spacing   2}
            (comp/grid
              {:item true
               :xs   12}
              (toolbar/toolbar "Task" (:taskName task) nil))
            (comp/grid
              {:item true
               :xs   12}
              (form-general task))
            (comp/grid
              {:item true
               :xs   12}
              (form-cpu-stats task-ts))
            (comp/grid
              {:item true
               :xs   12}
              (form-ram-stats task-ts))))]])))

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
                 mixin-init-form [{{:keys [log]} :params}]
  (let [{:keys [loading?]} (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (or (:task loading?)
          (:task-ts loading?))
      (form-info item (parse-int log)))))
