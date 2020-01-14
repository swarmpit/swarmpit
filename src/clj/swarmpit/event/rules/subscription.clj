(ns swarmpit.event.rules.subscription
  (:refer-clojure :exclude [list])
  (:require [swarmpit.api :as api]
            [swarmpit.docker.utils :as du]
            [swarmpit.stats :as stats]
            [swarmpit.event.rules.predicate :refer :all]
            [clojure.tools.logging :as log]))

(defn- service-id
  [service-event-message]
  (or (get-in service-event-message [:Actor :Attributes :com.docker.swarm.service.id])
      (get-in service-event-message [:Actor :ID])))

(defn- service-name
  [service-event-message]
  (or (get-in service-event-message [:Actor :Attributes :com.docker.swarm.service.name])
      (get-in service-event-message [:Actor :Attributes :name])))

(defn- node-id
  [node-event-message]
  (or (get-in node-event-message [:Actor :Attributes :com.docker.swarm.node.id])
      (get-in node-event-message [:Actor :ID])))

;; Subscribed Data

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
  [service-event-message]
  (let [service-id (or (service-id service-event-message)
                       (service-name service-event-message))
        service (api/service service-id)
        tasks (api/service-tasks service-id)
        networks (api/service-networks service-id)
        stats (when (stats/ready?) (stats/cluster))]
    {:service  service
     :tasks    tasks
     :networks networks
     :stats    stats}))

(defn- node-info-data
  [node-event-message]
  (let [node-id (node-id node-event-message)
        node (api/node node-id)
        tasks (api/node-tasks node-id)]
    {:node  node
     :tasks tasks}))

(defn- stack-info-data
  [service-event-message]
  (let [service-name (service-name service-event-message)
        stack-name (du/hypothetical-stack service-name)
        stack-tasks (api/stack-tasks stack-name)
        stats (when (stats/ready?) (stats/cluster))]
    (merge (api/stack stack-name)
           {:tasks stack-tasks
            :stats stats})))

;; Rules

(defprotocol Rule
  (match? [this type message])
  (subscription [this message])
  (subscribed-data [this message user]))

(def refresh-dashboard
  (reify Rule
    (match? [_ type message]
      (and (event? type)
           (or (service-event? message)
               (node-event? message))))
    (subscription [_ message]
      {:handler :index
       :params  {}})
    (subscribed-data [_ message user]
      (dashboard-data user))))

(def refresh-service-list
  (reify Rule
    (match? [_ type message]
      (and (event? type)
           (or (service-event? message)
               (service-task-event? message))))
    (subscription [_ message]
      {:handler :service-list
       :params  {}})
    (subscribed-data [_ message user]
      (api/services))))

(def refresh-service-info-by-name
  (reify Rule
    (match? [_ type message]
      (and (event? type)
           (or (service-event? message)
               (service-task-event? message))))
    (subscription [_ message]
      {:handler :service-info
       :params  {:id (service-name message)}})
    (subscribed-data [_ message user]
      (service-info-data message))))

(def refresh-service-info-by-id
  (reify Rule
    (match? [_ type message]
      (and (event? type)
           (or (service-event? message)
               (service-task-event? message))))
    (subscription [_ message]
      {:handler :service-info
       :params  {:id (service-id message)}})
    (subscribed-data [_ message user]
      (service-info-data message))))

(def refresh-task-list
  (reify Rule
    (match? [_ type message]
      (and (event? type)
           (service-task-event? message)))
    (subscription [_ message]
      {:handler :task-list
       :params  {}})
    (subscribed-data [_ message user]
      (api/tasks-memo))))

(def refresh-task-info
  (reify Rule
    (match? [_ type message]
      (and (event? type)
           (service-task-event? message)))
    (subscription [_ message]
      {:handler :task-info
       :params  {:serviceName (service-name message)}})
    (subscribed-data [_ message user]
      (api/service-tasks (service-name message)))))

(def refresh-node-info
  (reify Rule
    (match? [_ type message]
      (and (event? type)
           (node-event? message)))
    (subscription [_ message]
      {:handler :node-info
       :params  {:id (node-id message)}})
    (subscribed-data [_ message user]
      (node-info-data message))))

(def refresh-node-list
  (reify Rule
    (match? [_ type message]
      (and (event? type)
           (node-event? message)))
    (subscription [_ message]
      {:handler :node-list
       :params  {}})
    (subscribed-data [_ message user]
      (api/nodes))))

(def refresh-stack-list
  (reify Rule
    (match? [_ type message]
      (and (event? type)
           (service-event? message)))
    (subscription [_ message]
      {:handler :stack-list
       :params  {}})
    (subscribed-data [_ message user]
      (api/stacks))))

(def refresh-stack-info
  (reify Rule
    (match? [_ type message]
      (and (event? type)
           (or (service-event? message)
               (service-task-event? message))))
    (subscription [_ message]
      (let [service-name (service-name message)
            stack-name (du/hypothetical-stack service-name)]
        {:handler :stack-info
         :params  {:name stack-name}}))
    (subscribed-data [_ message user]
      (stack-info-data message))))

(def list [refresh-dashboard
           refresh-service-list
           refresh-service-info-by-name
           refresh-service-info-by-id
           refresh-task-list
           refresh-task-info
           refresh-node-list
           refresh-node-info
           refresh-stack-list
           refresh-stack-info])