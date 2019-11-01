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
            [swarmpit.component.service.list :as services]
            [swarmpit.component.node.list :as nodes]
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
                   (state/update-value [:stats] response state/form-value-cursor))
     :on-error   (fn [_])}))

(defn- nodes-ts-handler
  []
  (ajax/get
    (routes/path-for-backend :nodes-ts)
    {:state      [:node-stats-loading?]
     :on-success (fn [{:keys [response]}]
                   (node-cpu-plot response)
                   (node-ram-plot response)
                   (state/update-value [:nodes-ts] response state/form-value-cursor))
     :on-error   (fn [_])}))

(defn- nodes-handler
  []
  (ajax/get
    (routes/path-for-backend :nodes)
    {:on-success (fn [{:keys [response]}]
                   (nodes-ts-handler)
                   (state/update-value [:nodes] response state/form-value-cursor))}))

(defn- services-handler
  []
  (ajax/get
    (routes/path-for-backend :services)
    {:on-success (fn [{:keys [response origin?]}]
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
  (state/set-value {:loading?            true
                    :node-stats-loading? true} state/form-state-cursor))

(defn- init-form-value
  []
  (state/set-value {:stats              {}
                    :services           []
                    :services-dashboard []
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

(rum/defc dashboard-memory < rum/static [{:keys [usage total] :as memory}]
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
          (str (humanize/filesize total :binary false) " ram"))]
       [:div.Swarmpit-dashbord-section-graph
        (common/resource-pie
          usage
          (render-percentage usage)
          "graph-memory")]])))

(rum/defc dashboard-disk < rum/static [{:keys [usage total] :as disk}]
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
          (str (humanize/filesize total :binary false) " size"))]
       [:div.Swarmpit-dashbord-section-graph
        (common/resource-pie
          usage
          (render-percentage usage)
          "graph-disk")]])))

(rum/defc dashboard-cpu < rum/static [{:keys [usage] :as cpu} nodes]
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
            usage
            (render-percentage usage)
            "graph-cpu")]]))))

(rum/defc dashboard-node-ram-stats < rum/static []
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (html [:div {:id plot-node-ram-id}]))))

(rum/defc dashboard-node-cpu-stats < rum/static []
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (html [:div {:id plot-node-cpu-id}]))))

(rum/defc form-info < rum/reactive
  [{:keys [stats services services-dashboard nodes nodes-dashboard] :as item}]
  (let [{:keys [node-stats-loading?]} (state/react state/form-state-cursor)
        node-stats-empty? (empty? (:nodes-ts item))
        pinned-services (filter #(contains? (set services-dashboard) (:id %)) services)
        pinned-nodes (filter #(contains? (set nodes-dashboard) (:id %)) nodes)]
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
              (dashboard-cpu (:cpu stats) nodes))
            (when (not-empty pinned-services)
              (comp/grid
                {:item true
                 :xs   12
                 :sm   12
                 :lg   12
                 :xl   12}
                (services/pinned pinned-services)))
            (when (not-empty pinned-nodes)
              (comp/grid
                {:item true
                 :xs   12
                 :sm   12
                 :lg   12
                 :xl   12}
                (nodes/pinned pinned-nodes)))
            (comp/grid
              (merge {:item true
                      :xs   12
                      :sm   12
                      :md   12
                      :lg   6
                      :xl   6}
                     (when (and (false? node-stats-loading?) node-stats-empty?)
                       {:className "hide"}))
              (dashboard-node-ram-stats))
            (comp/grid
              (merge {:item true
                      :xs   12
                      :sm   12
                      :md   12
                      :lg   6
                      :xl   6}
                     (when (and (false? node-stats-loading?) node-stats-empty?)
                       {:className "hide"}))
              (dashboard-node-cpu-stats)))]]))))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [{{:keys [name]} :params}]
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info item))))
