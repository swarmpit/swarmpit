(ns swarmpit.event.docker
  (:require [swarmpit.component.service.list :as service-list]
            [swarmpit.component.node.list :as node-list]
            [swarmpit.component.task.list :as task-list]))

(defn- service-event?
  [event]
  (= "service" (:Type event)))

(defn- node-event?
  [event]
  (= "node" (:Type event)))

(defmulti handle (fn [route event] (:handler route)))

(defmethod handle :service-list
  [_ event]
  (when (service-event? event)
    (service-list/data-handler)))

(defmethod handle :node-list
  [_ event]
  (when (node-event? event)
    (node-list/data-handler)))

(defmethod handle :task-list
  [_ event]
  (when (service-event? event)
    (task-list/data-handler)))

;;default handling, do nothing
(defmethod handle :default
  [_ _])
