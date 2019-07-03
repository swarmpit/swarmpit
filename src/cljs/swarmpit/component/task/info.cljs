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
            [swarmpit.time :as time]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [clojure.contrib.humanize :as humanize]
            [rum.core :as rum]))

(enable-console-print!)

(def plot-id "taskStats")

(defn- event-handler
  [task-id]
  (fn [event]
    (let [task (->> event
                    (filter #(= task-id (:id %)))
                    (first))]
      (if task
        (state/set-value task state/form-value-cursor)
        (state/update-value [:state] "removed" state/form-value-cursor)))))

(defn- task-handler
  [route]
  (ajax/get
    (routes/path-for-backend :task (:params route))
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
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

(defn task-plot [stats-ts y-key title y-title]
  (let [time (:time stats-ts)
        now (last time)
        now-4-hours (time/in-past-string 60)]
    (plot/default
      plot-id
      [{:x           (:time stats-ts)
        :y           (y-key stats-ts)
        :connectgaps false
        :fill        "tozeroy"
        :line        {:color "#43a047"}
        :type        "scatter"
        :mode        "lines"}]
      {:title title
       :xaxis {:range [now-4-hours now]}
       :yaxis {:title     y-title
               :rangemode "tozero"}})))

(defn task-cpu-plot [stats-ts]
  (print (clj->js (:cpu stats-ts)))
  (print (map #(if (nil? %)
                 js/NaN
                 identity)) (:cpu stats-ts))

  (task-plot stats-ts
             :cpu
             "Container CPU Usage"
             "CPU Load) [%]"))

(defn task-ram-plot [stats-ts]
  (task-plot stats-ts
             :memory
             "Container Memory Usage"
             "Memory Used [MB]"))

(def mixin-init-scatter
  {:did-mount
   (fn [state]
     (let [stats-ts (:stats-timeseries (first (:rum/args state)))]
       (task-cpu-plot stats-ts))
     state)})

(rum/defc form-stats < rum/reactive
                       mixin-init-scatter [item]
  (let [{:keys [tab]} (state/react state/form-state-cursor)]
    (comp/card
      {:className "Swarmpit-card"}
      ;(comp/card-header
      ;  {:className "Swarmpit-table-card-header"
      ;   :title     (comp/typography {:variant "h6"} "Statistics")})
      (comp/card-content
        {:className "Swarmpit-table-card-content"}
        (comp/tabs
          {:className      "Swarmpit-service-tabs"
           :value          tab
           :onChange       (fn [e v]
                             (case v
                               0 (task-cpu-plot (:stats-timeseries item))
                               1 (task-ram-plot (:stats-timeseries item)))
                             (state/update-value [:tab] v state/form-state-cursor))
           :fullWidth      true
           :indicatorColor "primary"
           :textColor      "primary"
           :centered       true}
          (comp/tab
            {:label "CPU"})
          (comp/tab
            {:label "MEMORY"}))
        (html [:div {:id    plot-id
                     :style {:height "400px"}}])))))

(defn form-general-grid [task]
  (comp/grid
    {:item true
     :xs   12}
    (form-general task)))

(defn form-stats-grid [item]
  (comp/grid
    {:item true
     :xs   12}
    (form-stats item)))

(rum/defc form-info < rum/reactive [{:keys [repository status] :as item}]
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
                (form-stats-grid item)))))
        (comp/hidden
          {:smUp           true
           :implementation "js"}
          (comp/grid
            {:container true
             :spacing   16}
            (form-general-grid item)
            (form-stats-grid item)))]])))

(def unsubscribe-form
  {:will-unmount (fn [state]
                   (event/close!)
                   state)})

(defn- init-form-state
  []
  (state/set-value {:loading? true
                    :tab      0} state/form-state-cursor))

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
