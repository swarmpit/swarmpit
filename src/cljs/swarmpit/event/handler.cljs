(ns swarmpit.event.handler
  (:require [swarmpit.component.state :as state]))

(defmulti handle (fn [handler event] handler))

(defmethod handle :default
  [_ event]
  (state/set-value event [:form]))