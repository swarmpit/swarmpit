(ns swarmpit.event.docker
  (:require [swarmpit.component.service.list :as service-list]
            [swarmpit.component.node.list :as node-list]
            [swarmpit.component.task.list :as task-list]))

(def service-container-action #{"start" "die"})

(defn- service-container-event?
  [event]
  (and (= "service-container" (:type event))
       (contains? service-container-action (:action event))))

(defn- service-event?
  [event]
  (= "service" (:type event)))

(defn- node-event?
  [event]
  (= "node" (:type event)))

(defmulti handle (fn [route event] (:handler route)))

(defmethod handle :service-list
  [_ event]
  (when (or (service-event? event)
            (service-container-event? event))
    (service-list/data-handler)))

(defmethod handle :node-list
  [_ event]
  (when (node-event? event)
    (node-list/data-handler)))

(defmethod handle :task-list
  [_ event]
  (when (service-container-event? event)
    (task-list/data-handler)))

;;default handling, do nothing
(defmethod handle :default
  [_ _])
