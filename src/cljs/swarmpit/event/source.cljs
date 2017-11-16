(ns swarmpit.event.source
  (:require [yaffle.es]
            [swarmpit.routes :as routes]
            [swarmpit.storage :as storage]
            [swarmpit.component.state :as state]
            [swarmpit.event.docker :as docker-event]
            [clojure.walk :refer [keywordize-keys]]))

(defn parse-event
  [event]
  (-> (.-data event)
      (js/JSON.parse)
      (js->clj)
      (keywordize-keys)))

(defn handle-docker-event!
  [event-message]
  "Handler events from docker server"
  (let [route (state/get-value [:route])]
    (docker-event/handle route event-message)))

(defn handle-swarmpit-event!
  [event-message]
  "Handler events from swarmpit be"
  (when (not= "heartbeat" event-message)
    ;; If no heartbeat handle event
    ))

(defn handle-event!
  [event-data]
  "Handler events"
  (let [event-source (:From event-data)
        event-message (:Message event-data)]
    (case event-source
      "DOCKER" (handle-docker-event! event-message)
      "SWARMPIT" (handle-swarmpit-event! event-message)
      (print (str "Unknown event source: " event-source)))))

(defn headers
  []
  (->> {:headers {"Authorization" (storage/get "token")}}
       (clj->js)))

(defn init!
  []
  (let [event-source (js/EventSourcePolyfill. (routes/path-for-backend :events) (headers))]
    (.addEventListener event-source "message"
                       (fn [event]
                         (let [event-data (parse-event event)]
                           (handle-event! event-data))))
    (.addEventListener event-source "open"
                       (fn [_]
                         (print "Swarmpit event connection has been opened")))
    (.addEventListener event-source "error"
                       (fn [_]
                         (print "Swarmpit event connection failed. Reconnecting in 5 sec...")
                         (js/setTimeout #(init!) 5000)))))