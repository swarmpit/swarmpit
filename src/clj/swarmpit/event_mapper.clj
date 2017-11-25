(ns swarmpit.event-mapper
  (:require [clojure.set :refer [rename-keys]]
            [swarmpit.utils :refer [select-keys*]]))

(defn- service-container-event?
  [event]
  (let [service-id (get-in event [:Actor :Attributes :com.docker.swarm.service.id])]
    (and (= "container" (:Type event))
         (some? service-id))))

(defn- consolidate-keys
  [transformed-event]
  (rename-keys transformed-event {:Action :action, :Type :type, :ID :id}))

(defn- transform-event
  [event]
  (select-keys* event [[:Action] [:Type] [:Actor :ID]]))

(defn- transform-service-container-event
  [service-container-event]
  (-> (select-keys* service-container-event [[:Action]
                                             [:Type]
                                             [:Actor :Attributes :com.docker.swarm.service.name]
                                             [:Actor :Attributes :com.docker.swarm.service.id]])
      (assoc :Type "service-container")))

(defn transform
  [event]
  (let [message (:Message event)]
    (consolidate-keys
      (if (service-container-event? message)
        (transform-service-container-event message)
        (transform-event message)))))

