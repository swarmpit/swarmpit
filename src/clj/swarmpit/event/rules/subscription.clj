(ns swarmpit.event.rules.subscription
  (:refer-clojure :exclude [list])
  (:require [swarmpit.api :as api]
            [swarmpit.docker.utils :as du]
            [swarmpit.event.rules.predicate :refer :all]))

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

(defn- service-info-data
  [service-event-message]
  (let [service-id (or (service-id service-event-message)
                       (service-name service-event-message))
        service (api/service service-id)
        tasks (api/service-tasks service-id)
        networks (api/service-networks service-id)]
    {:service  service
     :tasks    tasks
     :networks networks}))

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
        stack-name (du/hypothetical-stack service-name)]
    (api/stack stack-name)))

;; Rules

(defprotocol Rule
  (match? [this type message])
  (subscription [this message])
  (subscribed-data [this message]))

(def refresh-service-list
  (reify Rule
    (match? [_ type message]
      (and (event? type)
           (or (service-event? message)
               (service-task-event? message))))
    (subscription [_ message]
      {:handler :service-list
       :params  nil})
    (subscribed-data [_ message]
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
    (subscribed-data [_ message]
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
    (subscribed-data [_ message]
      (service-info-data message))))

(def refresh-task-list
  (reify Rule
    (match? [_ type message]
      (and (event? type)
           (service-task-event? message)))
    (subscription [_ message]
      {:handler :task-list
       :params  nil})
    (subscribed-data [_ message]
      (api/tasks-memo))))

(def refresh-node-info
  (reify Rule
    (match? [_ type message]
      (and (event? type)
           (node-event? message)))
    (subscription [_ message]
      {:handler :node-info
       :params  {:id (node-id message)}})
    (subscribed-data [_ message]
      (node-info-data message))))

(def refresh-node-list
  (reify Rule
    (match? [_ type message]
      (and (event? type)
           (node-event? message)))
    (subscription [_ message]
      {:handler :node-list
       :params  nil})
    (subscribed-data [_ message]
      (api/nodes))))

(def refresh-stack-list
  (reify Rule
    (match? [_ type message]
      (and (event? type)
           (service-event? message)))
    (subscription [_ message]
      {:handler :stack-list
       :params  nil})
    (subscribed-data [_ message]
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
    (subscribed-data [_ message]
      (stack-info-data message))))

(def list [refresh-service-list
           refresh-service-info-by-name
           refresh-service-info-by-id
           refresh-task-list
           refresh-node-list
           refresh-node-info
           refresh-stack-list
           refresh-stack-info])