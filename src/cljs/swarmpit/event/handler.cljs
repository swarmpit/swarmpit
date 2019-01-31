(ns swarmpit.event.handler
  (:require [swarmpit.component.state :as state]))

(enable-console-print!)

(defmulti handle (fn [handler event] handler))

(defmethod handle :stack-list
  [_ event]
  (state/update-value [:items] event state/form-value-cursor))

(defmethod handle :service-list
  [_ event]
  (state/update-value [:items] event state/form-value-cursor))

(defmethod handle :node-list
  [_ event]
  (state/update-value [:items] event state/form-value-cursor))

(defmethod handle :task-list
  [_ event]
  (state/update-value [:items] event state/form-value-cursor))

(defmethod handle :default
  [_ event]
  (state/set-value event state/form-value-cursor))