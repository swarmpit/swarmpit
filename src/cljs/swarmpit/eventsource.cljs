(ns swarmpit.eventsource
  (:require [swarmpit.routes :as routes]
            [clojure.walk :refer [keywordize-keys]]))

(defn parse-event
  [event]
  (-> (.-data event)
      (js/JSON.parse)
      (js->clj)
      (keywordize-keys)))

(defn handle-docker-event!
  [event-message]
  "TODO: Handler docker events"
  (print event-message))

(defn handle-event!
  [event-data]
  (let [event-source (:From event-data)
        event-message (:Message event-data)]
    (case event-source
      "DOCKER" (handle-docker-event! event-message)
      (print (str "Unknown event source: " event-source)))))

(defn init!
  []
  (let [event-source (js/EventSource. (routes/path-for-backend :events))]
    (.addEventListener event-source "message"
                       (fn [event]
                         (let [event-data (parse-event event)]
                           (handle-event! event-data))))
    (.addEventListener event-source "open"
                       (fn [_]
                         (print "Swarmpit event connection was opened")))
    (.addEventListener event-source "error"
                       (fn [event]
                         (let [state (.-readyState event)]
                           (print state))))))


