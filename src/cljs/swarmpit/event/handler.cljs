(ns swarmpit.event.handler
  (:require [swarmpit.component.state :as state]
            [swarmpit.component.service.list :as service-list]
            [swarmpit.component.node.list :as node-list]
            [swarmpit.component.task.list :as task-list]))

(defmulti handle (fn [handler event] handler))

(defmethod handle :service-list
  [_ event]
  (print event)
  (state/update-value [:data] event service-list/cursor))

(defmethod handle :node-list
  [_ event]
  (state/update-value [:data] event node-list/cursor))

(defmethod handle :task-list
  [_ event]
  (state/update-value [:data] event task-list/cursor))

(def handlers (set (keys (methods handle))))