(ns swarmpit.event.handler.data
  (:require [swarmpit.component.state :as state]))

(def cursor [:form])

(defmulti handle (fn [handler data] handler))

(defmethod handle :service-list
  [_ data]
  (state/update-value [:items] data cursor))

(defmethod handle :node-list
  [_ data]
  (state/update-value [:items] data cursor))

(defmethod handle :task-list
  [_ data]
  (state/update-value [:items] data cursor))

(defmethod handle :stack-list
  [_ data]
  (state/update-value [:items] data cursor))

(defmethod handle :default
  [_ data]
  (state/set-value data cursor))