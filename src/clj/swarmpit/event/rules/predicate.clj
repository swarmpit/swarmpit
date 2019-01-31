(ns swarmpit.event.rules.predicate)

(def task-actions #{"start" "die"})

(defn event?
  [type]
  (= "event" type))

(defn stats?
  [type]
  (= "stats" type))

(defn service-event?
  [event-message]
  (= "service" (:Type event-message)))

(defn service-remove-event?
  [event-message]
  (and (service-event? event-message)
       (= "remove" (:Action event-message))))

(defn service-task-event?
  [event-message]
  (let [service-id (get-in event-message [:Actor :Attributes :com.docker.swarm.service.id])]
    (and (= "container" (:Type event-message))
         (some? service-id)
         (contains? task-actions (:Action event-message)))))

(defn node-event?
  [event-message]
  (= "node" (:Type event-message)))