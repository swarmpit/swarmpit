(ns swarmpit.event.rule
  (:refer-clojure :exclude [list])
  (:require [swarmpit.api :as api]
            [clojure.string :as str]))

(defn- possible-stack-name
  [service-name]
  "Return possible stack name as there are no stack metadata within event api"
  (when (some? service-name)
    (let [seg (str/split service-name #"_")]
      (when (< 1 (count seg))
        (first seg)))))

(defn- service-container-event?
  [event]
  (let [service-id (get-in event [:Actor :Attributes :com.docker.swarm.service.id])
        supported-actions #{"start" "die"}]
    (and (= "container" (:Type event))
         (some? service-id)
         (contains? supported-actions (:Action event)))))

;; Data

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
        stack-name (possible-stack-name service-name)
        stack-label (str "com.docker.stack.namespace=" stack-name)
        stack-services (api/services stack-label)
        stack-networks (api/networks stack-label)
        stack-volumes (api/volumes stack-label)
        stack-secrets (api/secrets stack-label)
        stack-configs (api/configs stack-label)]
    {:services stack-services
     :networks stack-networks
     :volumes  stack-volumes
     :secrets  stack-secrets
     :configs  stack-configs}))

;; Rules

(defprotocol Rule
  (match? [this event])
  (subscription [this event])
  (subscribed-data [this event]))

(def service-list-event
  (reify Rule
    (match? [_ event]
      (or (= "service" (:Type event))
          (service-container-event? event)))
    (subscription [_ event]
      {:handler :service-list
       :params  nil})
    (subscribed-data [_ event]
      (api/services))))

(def service-info-by-name-event
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

(def service-info-by-id-event
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

(def task-list-event
  (reify Rule
    (match? [_ event]
      (service-container-event? event))
    (subscription [_ event]
      {:handler :task-list
       :params  nil})
    (subscribed-data [_ event]
      (api/tasks-memo))))

(def node-list-event
  (reify Rule
    (match? [_ event]
      (= "node" (:Type event)))
    (subscription [_ event]
      {:handler :node-list
       :params  nil})
    (subscribed-data [_ event]
      (api/nodes))))

(def stack-list-event
  (reify Rule
    (match? [_ event]
      (= "service" (:Type event)))
    (subscription [_ event]
      {:handler :stack-list
       :params  nil})
    (subscribed-data [_ event]
      (api/stacks))))

(def stack-info-event
  (reify Rule
    (match? [_ event]
      (or (= "service" (:Type event))
          (service-container-event? event)))
    (subscription [_ event]
      (let [service-name (or (get-in event [:Actor :Attributes :com.docker.swarm.service.name])
                             (get-in event [:Actor :Attributes :name]))
            stack-name (possible-stack-name service-name)]
        {:handler :stack-info
         :params  {:name stack-name}}))
    (subscribed-data [_ event]
      (stack-info-data event))))

(def list [service-list-event
           service-info-by-name-event
           service-info-by-id-event
           task-list-event
           node-list-event
           stack-list-event
           stack-info-event])