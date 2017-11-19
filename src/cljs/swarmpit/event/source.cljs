(ns swarmpit.event.source
  (:require [swarmpit.routes :as routes]
            [swarmpit.component.state :as state]
            [swarmpit.component.handler :as handler]
            [swarmpit.event.docker :as docker-event]
            [clojure.walk :refer [keywordize-keys]]))

(def es (atom nil))

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

(defn handle-event!
  [event-data]
  "Handler events"
  (let [event-source (:From event-data)
        event-message (:Message event-data)]
    (case event-source
      "DOCKER" (handle-docker-event! event-message)
      (print (str "Unknown event source: " event-source)))))

(defn- on-message!
  [event]
  (let [event-data (parse-event event)]
    (handle-event! event-data)))

(defn- on-open!
  [event]
  (print "Swarmpit event connection has been opened"))

(defn- on-error!
  [event]
  (print "Swarmpit event connection failed. Reconnecting in 5 sec..."))

(defn close!
  []
  (when (some? @es)
    (.close @es)))

(defn init!
  []
  (handler/get
    (routes/path-for-backend :slt)
    {:on-success (fn [{:keys [slt]}]
                   (let [event-url (str (routes/path-for-backend :events) "?slt=" slt)
                         event-source (js/EventSource. event-url)]
                     (.addEventListener event-source "message" on-message!)
                     (.addEventListener event-source "open" on-open!)
                     (.addEventListener event-source "error" on-error!)
                     (reset! es event-source)))}))