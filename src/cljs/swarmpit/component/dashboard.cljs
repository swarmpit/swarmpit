(ns swarmpit.component.dashboard
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
            [clojure.contrib.inflect :as inflect]
            [goog.string.format]
            [goog.string :as gstring]
            [rum.core :as rum]))

(def plot-node-cpu-id "nodeCpuStats")

(def plot-node-ram-id "nodeRamStats")

(defn node-cpu-plot [stats-ts]
  (plot/multi plot-node-cpu-id
              stats-ts
              :cpu
              "CPU usage by Node"
              "[%]"))

(defn node-ram-plot [stats-ts]
  (plot/multi plot-node-ram-id
              stats-ts
              :memory
              "Memory usage by Node"
              "[%]"))

(defn- render-percentage
  [val]
  (if (some? val)
    (str (gstring/format "%.2f" val) "%")
    "-"))

(defn- stats-handler
  []
  (ajax/get
    (routes/path-for-backend :stats)
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:stats] response state/form-value-cursor))}))

(defn- nodes-handler
  []
  (ajax/get
    (routes/path-for-backend :nodes)
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:nodes] response state/form-value-cursor))}))

(defn- nodes-ts-handler
  []
  (ajax/get
    (routes/path-for-backend :nodes-ts)
    {:on-success (fn [{:keys [response]}]
                   (node-cpu-plot response)
                   (node-ram-plot response)
                   (state/update-value [:nodes-ts] response state/form-value-cursor))}))

(defn- init-form-state
  []
  (state/set-value {:loading? true} state/form-state-cursor))

(defn- init-form-value
  []
  (state/set-value {:stats    {}
                    :nodes    []
                    :nodes-ts []} state/form-value-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (init-form-value)
      (stats-handler)
      (nodes-handler)
      (nodes-ts-handler))))

(defn- resource-chip
  [name count]
  (comp/chip {:key       name
              :avatar    (comp/avatar {} count)
              :className "Swarmpit-dashboard-chip"
              :label     (inflect/pluralize-noun count name)}))

(rum/defc dashboard-cluster < rum/static [{:keys [stats nodes] :as item}]
  (comp/paper
    {:elevation 0
     :className "Swarmpit-paper Swarmpit-dashboard-paper"}
    (html
      [:div.Swarmpit-dashboard-section
       [:div
        (comp/typography
          {:variant   "body2"
           :className "Swarmpit-dashboard-section-title"}
          "CLUSTER")
        (comp/typography
          {:variant   "h5"
           :className "Swarmpit-dashboard-section-value"}
          (str (count nodes) " " (inflect/pluralize-noun (count nodes) "node")))]
       [:div.Swarmpit-dashbord-section-chips
        (resource-chip "manager" (count (filter #(= "manager" (:role %)) nodes)))
        (resource-chip "worker" (count (filter #(= "worker" (:role %)) nodes)))]])))

(rum/defc dashboard-memory < rum/static [{:keys [stats nodes] :as item}]
  (comp/paper
    {:elevation 0
     :className "Swarmpit-paper Swarmpit-dashboard-paper"}
    (html
      [:div.Swarmpit-dashboard-section
       [:div
        (comp/typography
          {:variant   "body2"
           :className "Swarmpit-dashboard-section-title"}
          "MEMORY")
        (comp/typography
          {:variant   "h5"
           :className "Swarmpit-dashboard-section-value"}
          (str (humanize/filesize (:memoryTotal stats) :binary false) " ram"))]
       [:div.Swarmpit-dashbord-section-graph
        (common/resource-pie
          (:memoryUsage stats)
          (render-percentage (:memoryUsage stats))
          "graph-memory")]])))

(rum/defc dashboard-disk < rum/static [{:keys [stats nodes] :as item}]
  (comp/paper
    {:elevation 0
     :className "Swarmpit-paper Swarmpit-dashboard-paper"}
    (html
      [:div.Swarmpit-dashboard-section
       [:div
        (comp/typography
          {:variant   "body2"
           :className "Swarmpit-dashboard-section-title"}
          "DISK")
        (comp/typography
          {:variant   "h5"
           :className "Swarmpit-dashboard-section-value"}
          (str (humanize/filesize (:diskTotal stats) :binary false) " size"))]
       [:div.Swarmpit-dashbord-section-graph
        (common/resource-pie
          (:diskUsage stats)
          (render-percentage (:diskUsage stats))
          "graph-disk")]])))

(rum/defc dashboard-cpu < rum/static [{:keys [stats nodes] :as item}]
  (let [cores (apply + (map #(get-in % [:resources :cpu]) nodes))]
    (comp/paper
      {:elevation 0
       :className "Swarmpit-paper Swarmpit-dashboard-paper"}
      (html
        [:div.Swarmpit-dashboard-section
         [:div
          (comp/typography
            {:variant   "body2"
             :className "Swarmpit-dashboard-section-title"}
            "CPU")
          (comp/typography
            {:variant   "h5"
             :className "Swarmpit-dashboard-section-value"}
            (str cores " " (inflect/pluralize-noun cores "core")))]
         [:div.Swarmpit-dashbord-section-graph
          (common/resource-pie
            (:cpuUsage stats)
            (render-percentage (:cpuUsage stats))
            "graph-cpu")]]))))

(rum/defc dashborad-node-ram-stats < {:did-mount
                                      (fn [state]
                                        (nodes-ts-handler)
                                        state)}
                                     rum/static []
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (html [:div {:id plot-node-ram-id}]))))

(rum/defc dashborad-node-cpu-stats < rum/static []
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (html [:div {:id plot-node-cpu-id}]))))

(rum/defc form-info < rum/reactive [item]
  (let []
    (comp/mui
      (html
        [:div.Swarmpit-form
         [:div.Swarmpit-form-context
          (comp/grid
            {:container true
             :spacing   16}
            (comp/grid
              {:item true
               :xs   12
               :sm   6
               :lg   3
               :xl   3}
              (dashboard-cluster item))
            (comp/grid
              {:item true
               :xs   12
               :sm   6
               :lg   3
               :xl   3}
              (dashboard-disk item))
            (comp/grid
              {:item true
               :xs   12
               :sm   6
               :lg   3
               :xl   3}
              (dashboard-memory item))
            (comp/grid
              {:item true
               :xs   12
               :sm   6
               :lg   3
               :xl   3}
              (dashboard-cpu item))
            (comp/grid
              {:item true
               :xs   12
               :sm   12
               :md   12
               :lg   6
               :xl   6}
              (dashborad-node-ram-stats))
            (comp/grid
              {:item true
               :xs   12
               :sm   12
               :md   12
               :lg   6
               :xl   6}
              (dashborad-node-cpu-stats)))]]))))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [{{:keys [name]} :params}]
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info item))))
