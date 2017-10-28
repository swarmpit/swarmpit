(ns swarmpit.eventsource
  (:require [swarmpit.routes :as routes]))

(defn subscribe
  [uri]
  (js/EventSource. uri))

(defn add-listener!
  [evs eventname callback]
  (.addEventListener evs eventname callback))

(defn init!
  []
  (let [event-source (subscribe (routes/path-for-backend :events))]
    (add-listener! event-source "message"
                   (fn [event]
                     (let [data (.-data event)]
                       (print data))))
    (add-listener! event-source "open"
                   (fn [_]
                     (print "Swarmpit event connection was opened")))
    (add-listener! event-source "error"
                   (fn [event]
                     (let [state (.-readyState event)]
                       (print state))))))
