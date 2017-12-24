(ns swarmpit.event.rule
  (:refer-clojure :exclude [list])
  (:require [swarmpit.api :as api]))

(defprotocol Rule
  (match? [this event])
  (subscription [this event])
  (subscribed-data [this event]))

(defn- service-container-event?
  [event]
  (let [service-id (get-in event [:Actor :Attributes :com.docker.swarm.service.id])
        supported-actions #{"start" "die"}]
    (and (= "container" (:Type event))
         (some? service-id)
         (contains? supported-actions (:Action event)))))

(defn- service-info-data
  [event]
  (let [service-id (get-in event [:Actor :Attributes :com.docker.swarm.service.id])
        service (api/service service-id)
        tasks (api/service-tasks service-id)
        networks (api/service-networks service-id)]
    {:service  service
     :tasks    tasks
     :networks networks}))

(def service-list-container-event
  (reify Rule
    (match? [_ event]
      (service-container-event? event))
    (subscription [_ event]
      {:handler :service-list
       :params  nil})
    (subscribed-data [_ event]
      (api/services-memo))))

(def service-info-by-name-container-event
  (reify Rule
    (match? [_ event]
      (service-container-event? event))
    (subscription [_ event]
      (let [service-name (get-in event [:Actor :Attributes :com.docker.swarm.service.name])]
        {:handler :service-info
         :params  {:id service-name}}))
    (subscribed-data [_ event]
      (service-info-data event))))

(def service-info-by-id-container-event
  (reify Rule
    (match? [_ event]
      (service-container-event? event))
    (subscription [_ event]
      (let [service-id (get-in event [:Actor :Attributes :com.docker.swarm.service.id])]
        {:handler :service-info
         :params  {:id service-id}}))
    (subscribed-data [_ event]
      (service-info-data event))))

(def task-list-container-event
  (reify Rule
    (match? [_ event]
      (service-container-event? event))
    (subscription [_ event]
      {:handler :task-list
       :params  nil})
    (subscribed-data [_ event]
      (api/tasks-memo))))

(def service-list-event
  (reify Rule
    (match? [_ event]
      (= "service" (:Type event)))
    (subscription [_ event]
      {:handler :service-list
       :params  nil})
    (subscribed-data [_ event]
      (api/services))))

(def service-info-by-name-event
  (reify Rule
    (match? [_ event]
      (= "service" (:Type event)))
    (subscription [_ event]
      (let [service-name (get-in event [:Actor :Attributes :com.docker.swarm.service.name])]
        {:handler :service-info
         :params  {:id service-name}}))
    (subscribed-data [_ event]
      (service-info-data event))))

(def service-info-by-id-event
  (reify Rule
    (match? [_ event]
      (= "service" (:Type event)))
    (subscription [_ event]
      (let [service-id (get-in event [:Actor :Attributes :com.docker.swarm.service.id])]
        {:handler :service-info
         :params  {:id service-id}}))
    (subscribed-data [_ event]
      (service-info-data event))))

(def node-list-event
  (reify Rule
    (match? [_ event]
      (= "node" (:Type event)))
    (subscription [_ event]
      {:handler :node-list
       :params  nil})
    (subscribed-data [_ event]
      (api/nodes))))

(def list [service-list-container-event
           service-list-event
           service-info-by-name-container-event
           service-info-by-id-container-event
           service-info-by-name-event
           service-info-by-id-event
           task-list-container-event
           node-list-event])