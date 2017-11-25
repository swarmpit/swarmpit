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

(defn handle-event!
  [event-data]
  "Handle docker events. Events are delayed for 1 second due to internal swarm resource state management"
  (let [route (state/get-value [:route])]
    (js/setTimeout
      (fn []
        (docker-event/handle route event-data)) 1000)))

(defn- on-message!
  [event]
  (let [event-data (parse-event event)]
    (handle-event! event-data)))

(defn- on-open!
  [event]
  (print "Swarmpit event connection has been opened"))

(defn- on-error!
  [event error-fx]
  (when (= (.-readyState @es) 2)
    (do
      (print "Swarmpit event connection failed. Reconnecting in 5 sec...")
      (js/setTimeout #(error-fx) 5000))))

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
                     (.addEventListener event-source "error" (fn [e] (on-error! e init!)))
                     (reset! es event-source)))}))