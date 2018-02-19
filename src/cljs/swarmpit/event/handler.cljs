(ns swarmpit.event.handler
  (:require [swarmpit.component.state :as state]))

(def cursor [:form])

(defmulti handle (fn [handler event] handler))

(defmethod handle :service-list
  [_ event]
  (state/update-value [:items] event cursor))

(defmethod handle :node-list
  [_ event]
  (state/update-value [:items] event cursor))

(defmethod handle :task-list
  [_ event]
  (state/update-value [:items] event cursor))

(defmethod handle :stack-list
  [_ event]
  (state/update-value [:items] event cursor))

(defmethod handle :default
  [_ event]
  (state/set-value event cursor))