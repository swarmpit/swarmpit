(ns swarmpit.component.dashboard
  (:require [material.components :as comp]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.common :as common]
            [swarmpit.component.plot :as plot]
            [swarmpit.component.service.list :as services]
            [swarmpit.component.node.list :as nodes]
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

(def plot-service-cpu-id "serviceCpuStats")

(def plot-service-ram-id "serviceRamStats")

(defn node-cpu-plot [stats-ts]
  (plot/multi plot-node-cpu-id
              stats-ts
              :cpu
              :name
              "CPU utilization by Node"
              "[%]"
              [0 100]))

(defn node-ram-plot [stats-ts]
  (plot/multi plot-node-ram-id
              stats-ts
              :memory
              :name
              "Memory utilization by Node"
              "[%]"
              [0 100]))

(defn service-cpu-plot [tasks-ts]
  (plot/multi plot-service-cpu-id
              tasks-ts
              :cpu
              :service
              "CPU usage by Service"
              "[vCPU]"))

(defn service-ram-plot [tasks-ts]
  (plot/multi plot-service-ram-id
              tasks-ts
              :memory
              :service
              "Memory usage by Service"
              "[MiB]"))

(defn- stats-handler
  []
  (ajax/get
    (routes/path-for-backend :stats)
    {:state      [:loading? :stats]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:stats] response state/form-value-cursor))
     :on-error   (fn [_])}))

(defn- services-ts-handler
  []
  (ajax/get
    (routes/path-for-backend :services-ts)
    {:state      [:loading? :services-ts]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:services-ts] response state/form-value-cursor))
     :on-error   (fn [_])}))

(defn- nodes-ts-handler
  []
  (ajax/get
    (routes/path-for-backend :nodes-ts)
    {:state      [:loading? :nodes-ts]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:nodes-ts] response state/form-value-cursor))
     :on-error   (fn [_])}))

(defn- nodes-handler
  []
  (ajax/get
    (routes/path-for-backend :nodes)
    {:state      [:loading? :nodes]
     :on-success (fn [{:keys [response]}]
                   (nodes-ts-handler)
                   (state/update-value [:nodes] response state/form-value-cursor))}))

(defn- services-handler
  []
  (ajax/get
    (routes/path-for-backend :services)
    {:state      [:loading? :services]
     :on-success (fn [{:keys [response origin?]}]
                   (state/update-value [:services] response state/form-value-cursor))}))

(defn- me-handler
  []
  (ajax/get
    (routes/path-for-backend :me)
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:services-dashboard] (:service-dashboard response) state/form-value-cursor)
                   (state/update-value [:nodes-dashboard] (:node-dashboard response) state/form-value-cursor))}))

(defn- init-form-state
  []
  (state/set-value {:loading? {:stats       true
                               :nodes       true
                               :nodes-ts    true
                               :services    true
                               :services-ts true}} state/form-state-cursor))

(defn- init-form-value
  []
  (state/set-value {:stats              {}
                    :services           []
                    :services-dashboard []
                    :services-ts        []
                    :nodes              []
                    :nodes-dashboard    []
                    :nodes-ts           []} state/form-value-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (init-form-value)
      (stats-handler)
      (nodes-handler)
      (services-handler)
      (services-ts-handler)
      (me-handler))))

(defn- resource-chip
  [name count]
  (comp/chip {:key       name
              :avatar    (comp/avatar {} count)
              :className "Swarmpit-dashboard-chip"
              :label     (inflect/pluralize-noun count name)}))

(rum/defc dashboard-cluster < rum/static [nodes]
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

(rum/defc dashboard-memory < rum/static [{:keys [usage used total] :as memory}]
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
          (common/render-capacity used true))]
       [:div.Swarmpit-dashbord-section-graph
        (common/resource-pie
          {:value used
           :limit total
           :usage usage
           :type  :memory}
          (str (common/render-capacity total true) " ram")
          "graph-memory")]])))

(rum/defc dashboard-disk < rum/static [{:keys [usage used total] :as disk}]
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
          (common/render-capacity used false))]
       [:div.Swarmpit-dashbord-section-graph
        (common/resource-pie
          {:value used
           :limit total
           :usage usage
           :type  :disk}
          (str (common/render-capacity total false) " size")
          "graph-disk")]])))

(rum/defc dashboard-cpu < rum/static [{:keys [usage cores] :as cpu}]
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
          (common/render-cores (/ cores usage)))]
       [:div.Swarmpit-dashbord-section-graph
        (common/resource-pie
          {:value (/ cores usage)
           :limit cores
           :usage usage
           :type  :cpu}
          (str cores " vCPU")
          "graph-cpu")]])))

(defn dashboard-node-ram-callback
  [state]
  (let [ts (first (:rum/args state))]
    (node-ram-plot ts))
  state)

(rum/defc dashboard-node-ram-stats < rum/static
                                     {:did-mount  dashboard-node-ram-callback
                                      :did-update dashboard-node-ram-callback} [nodes-ts]
  (comp/card
    (if (empty? nodes-ts)
      {:className "Swarmpit-card hide"}
      {:className "Swarmpit-card"})
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (html [:div {:id plot-node-ram-id}]))))

(defn dashboard-node-cpu-callback
  [state]
  (let [ts (first (:rum/args state))]
    (node-cpu-plot ts))
  state)

(rum/defc dashboard-node-cpu-stats < rum/static
                                     {:did-mount  dashboard-node-cpu-callback
                                      :did-update dashboard-node-cpu-callback} [nodes-ts]
  (comp/card
    (if (empty? nodes-ts)
      {:className "Swarmpit-card hide"}
      {:className "Swarmpit-card"})
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (html [:div {:id plot-node-cpu-id}]))))

(defn dashboard-service-ram-callback
  [state]
  (let [ts (first (:rum/args state))]
    (service-ram-plot ts))
  state)

(rum/defc dashboard-service-ram-stats < rum/static
                                        {:did-mount  dashboard-service-ram-callback
                                         :did-update dashboard-service-ram-callback} [services-memory-ts]
  (comp/card
    (if (empty? services-memory-ts)
      {:className "Swarmpit-card hide"}
      {:className "Swarmpit-card"})
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (html [:div {:id plot-service-ram-id}]))))

(defn dashboard-service-cpu-callback
  [state]
  (let [ts (first (:rum/args state))]
    (service-cpu-plot ts))
  state)

(rum/defc dashboard-service-cpu-stats < rum/static
                                        {:did-mount  dashboard-service-cpu-callback
                                         :did-update dashboard-service-cpu-callback} [services-cpu-ts]
  (comp/card
    (if (empty? services-cpu-ts)
      {:className "Swarmpit-card hide"}
      {:className "Swarmpit-card"})
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (html [:div {:id plot-service-cpu-id}]))))

(rum/defc form-info < rum/static [{:keys [stats services services-ts services-dashboard nodes nodes-ts nodes-dashboard] :as item}]
  (let [pinned-services (filter #(contains? (set services-dashboard) (:id %)) services)
        pinned-nodes (filter #(contains? (set nodes-dashboard) (:id %)) nodes)
        running-services (->> (filter #(not= "not running" (:state %)) services)
                              (map :serviceName)
                              (set))
        services-cpu-ts (filter #(contains? running-services (:service %)) (:cpu services-ts))
        services-memory-ts (filter #(contains? running-services (:service %)) (:memory services-ts))]
    (comp/mui
      (html
        [:div.Swarmpit-form
         [:div.Swarmpit-form-context
          (comp/grid
            {:container true
             :spacing   2}
            (comp/grid
              {:item true
               :xs   12
               :sm   6
               :lg   3
               :xl   3}
              (dashboard-cluster nodes))
            (comp/grid
              {:item true
               :xs   12
               :sm   6
               :lg   3
               :xl   3}
              (dashboard-disk (:disk stats)))
            (comp/grid
              {:item true
               :xs   12
               :sm   6
               :lg   3
               :xl   3}
              (dashboard-memory (:memory stats)))
            (comp/grid
              {:item true
               :xs   12
               :sm   6
               :lg   3
               :xl   3}
              (dashboard-cpu (:cpu stats)))
            (when (not-empty pinned-services)
              (comp/grid
                {:item true
                 :xs   12
                 :sm   12
                 :lg   12
                 :xl   12}
                (services/pinned pinned-services)))
            (comp/grid
              {:item true
               :xs   12
               :sm   12
               :md   12
               :lg   6
               :xl   6}
              (dashboard-service-ram-stats services-memory-ts))
            (comp/grid
              {:item true
               :xs   12
               :sm   12
               :md   12
               :lg   6
               :xl   6}
              (dashboard-service-cpu-stats services-cpu-ts))
            (when (not-empty pinned-nodes)
              (comp/grid
                {:item true
                 :xs   12
                 :sm   12
                 :lg   12
                 :xl   12}
                (nodes/pinned pinned-nodes)))
            (comp/grid
              {:item true
               :xs   12
               :sm   12
               :md   12
               :lg   6
               :xl   6}
              (dashboard-node-ram-stats nodes-ts))
            (comp/grid
              {:item true
               :xs   12
               :sm   12
               :md   12
               :lg   6
               :xl   6}
              (dashboard-node-cpu-stats nodes-ts)))]]))))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [{{:keys [name]} :params}]
  (let [{:keys [loading?]} (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (or (:stats loading?)
          (:nodes loading?)
          (:nodes-ts loading?)
          (:services loading?)
          (:services-ts loading?))
      (form-info item))))