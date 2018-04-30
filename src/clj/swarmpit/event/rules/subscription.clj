(ns swarmpit.event.rules.subscription
  (:refer-clojure :exclude [list])
  (:require [swarmpit.api :as api]
            [swarmpit.docker.utils :as du]))

;; Rule Predicates

(defn- service-container-event?
  [event]
  (let [service-id (get-in event [:Actor :Attributes :com.docker.swarm.service.id])
        supported-actions #{"start" "die"}]
    (and (= "container" (:Type event))
         (some? service-id)
         (contains? supported-actions (:Action event)))))

;; Subscribed Data

(defn- service-info-data
  [service-event]
  (let [service-id (or (get-in service-event [:Actor :Attributes :com.docker.swarm.service.id])
                       (get-in service-event [:Actor :ID]))
        service (api/service service-id)
        tasks (api/service-tasks service-id)
        networks (api/service-networks service-id)]
    {:service  service
     :tasks    tasks
     :networks networks}))

(defn- stack-info-data
  [service-event]
  (let [service-name (or (get-in service-event [:Actor :Attributes :com.docker.swarm.service.name])
                         (get-in service-event [:Actor :Attributes :name]))
        stack-name (du/hypothetical-stack service-name)]
    (api/stack stack-name)))

;; Rules

(defprotocol Rule
  (match? [this event])
  (subscription [this event])
  (subscribed-data [this event]))

(def refresh-service-list
  (reify Rule
    (match? [_ event]
      (or (= "service" (:Type event))
          (service-container-event? event)))
    (subscription [_ event]
      {:handler :service-list
       :params  nil})
    (subscribed-data [_ event]
      (api/services))))

(def refresh-service-info-by-name
  (reify Rule
    (match? [_ event]
      (or (= "service" (:Type event))
          (service-container-event? event)))
    (subscription [_ event]
      (let [service-name (or (get-in event [:Actor :Attributes :com.docker.swarm.service.name])
                             (get-in event [:Actor :Attributes :name]))]
        {:handler :service-info
         :params  {:id service-name}}))
    (subscribed-data [_ event]
      (service-info-data event))))

(def refresh-service-info-by-id
  (reify Rule
    (match? [_ event]
      (or (= "service" (:Type event))
          (service-container-event? event)))
    (subscription [_ event]
      (let [service-id (or (get-in event [:Actor :Attributes :com.docker.swarm.service.id])
                           (get-in event [:Actor :ID]))]
        {:handler :service-info
         :params  {:id service-id}}))
    (subscribed-data [_ event]
      (service-info-data event))))

(def refresh-task-list
  (reify Rule
    (match? [_ event]
      (service-container-event? event))
    (subscription [_ event]
      {:handler :task-list
       :params  nil})
    (subscribed-data [_ event]
      (api/tasks-memo))))

(def refresh-node-list
  (reify Rule
    (match? [_ event]
      (= "node" (:Type event)))
    (subscription [_ event]
      {:handler :node-list
       :params  nil})
    (subscribed-data [_ event]
      (api/nodes))))

(def refresh-stack-list
  (reify Rule
    (match? [_ event]
      (= "service" (:Type event)))
    (subscription [_ event]
      {:handler :stack-list
       :params  nil})
    (subscribed-data [_ event]
      (api/stacks))))

(def refresh-stack-info
  (reify Rule
    (match? [_ event]
      (or (= "service" (:Type event))
          (service-container-event? event)))
    (subscription [_ event]
      (let [service-name (or (get-in event [:Actor :Attributes :com.docker.swarm.service.name])
                             (get-in event [:Actor :Attributes :name]))
            stack-name (du/hypothetical-stack service-name)]
        {:handler :stack-info
         :params  {:name stack-name}}))
    (subscribed-data [_ event]
      (stack-info-data event))))

(def list [refresh-service-list
           refresh-service-info-by-name
           refresh-service-info-by-id
           refresh-task-list
           refresh-node-list
           refresh-stack-list
           refresh-stack-info])