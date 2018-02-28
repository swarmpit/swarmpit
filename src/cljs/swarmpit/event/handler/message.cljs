(ns swarmpit.event.handler.message
  (:require [swarmpit.component.message :as message]
            [swarmpit.storage :as storage]))

(defn- display
  [target msg-fx msg-text]
  (when (or (= target (storage/user))
            (nil? target))
    (msg-fx msg-text)))

(defmulti handle (fn [message] (:type message)))

(defmethod handle "info"
  [{:keys [target text]}]
  (display target message/info text))

(defmethod handle "error"
  [{:keys [target text]}]
  (display target message/error text))
