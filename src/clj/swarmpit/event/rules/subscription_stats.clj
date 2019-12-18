(ns swarmpit.event.rules.subscription-stats
  (:refer-clojure :exclude [list])
  (:require [swarmpit.api :as api]))

(def subscribers #{:service-info :task-list :task-info :node-list :node-info})

(defn- service-info-data
  [service-id]
  (let [service (api/service service-id)
        tasks (api/service-tasks service-id)
        networks (api/service-networks service-id)
        nodes (api/nodes)]
    {:service  service
     :tasks    tasks
     :networks networks
     :nodes    nodes}))

(defn- node-info-data
  [node-id]
  (let [node (api/node node-id)
        tasks (api/node-tasks node-id)]
    {:node  node
     :tasks tasks}))

(defn subscribed-data
  [{:keys [handler params] :as subscription}]
  (case handler
    :service-info (service-info-data (:id params))
    :task-list (api/tasks)
    :task-info (api/service-tasks (:serviceName params))
    :node-list (api/nodes)
    :node-info (node-info-data (:id params))))