(ns swarmpit.event.rules.subscription-stats
  (:refer-clojure :exclude [list])
  (:require [swarmpit.api :as api]
            [swarmpit.stats :as stats]))

(def subscribers #{:index :stack-info :service-info :task-list :task-info :node-list :node-info})

(defn dashboard-data
  [user]
  (let [stats (when (stats/ready?) (stats/cluster))
        nodes-ts (when (stats/influx-configured?) (stats/hosts-timeseries-memo))
        services-ts-cpu (when (stats/influx-configured?) (stats/services-cpu-timeseries-memo))
        services-ts-memory (when (stats/influx-configured?) (stats/services-memory-timeseries-memo))
        dashboard-user (api/user-by-username user)]
    {:stats              stats
     :services           (api/services)
     :services-dashboard (:service-dashboard dashboard-user)
     :services-ts-cpu    services-ts-cpu
     :services-ts-memory services-ts-memory
     :nodes              (api/nodes)
     :nodes-dashboard    (:node-dashboard dashboard-user)
     :nodes-ts           nodes-ts}))

(defn- service-info-data
  [service-id]
  (let [service (api/service service-id)
        tasks (api/service-tasks service-id)
        networks (api/service-networks service-id)
        stats (when (stats/ready?) (stats/cluster))]
    {:service  service
     :tasks    tasks
     :networks networks
     :stats    stats}))

(defn- stack-info-data
  [stack-name]
  (let [stack-tasks (api/stack-tasks stack-name)
        stats (when (stats/ready?) (stats/cluster))]
    (merge (api/stack stack-name)
           {:tasks stack-tasks
            :stats stats})))

(defn- node-info-data
  [node-id]
  (let [node (api/node node-id)
        tasks (api/node-tasks node-id)]
    {:node  node
     :tasks tasks}))

(defn subscribed-data
  [{:keys [handler params] :as subscription} user]
  (case handler
    :index (dashboard-data user)
    :service-info (service-info-data (:id params))
    :stack-info (stack-info-data (:name params))
    :task-list (api/tasks)
    :task-info (api/service-tasks (:serviceName params))
    :node-list (api/nodes)
    :node-info (node-info-data (:id params))))