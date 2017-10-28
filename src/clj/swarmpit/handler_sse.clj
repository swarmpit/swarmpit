(ns swarmpit.handler-sse
  (:require [org.httpkit.server :refer [run-server with-channel on-close send! close]]
            [swarmpit.handler :refer [dispatch resp-accepted resp-error]]))

(def channel-hub (atom {}))

(defn broadcast [message]
  (doseq [channel (keys @channel-hub)]
    (send! channel message false)))

(defmethod dispatch :events [_]
  (fn [req]
    (with-channel req channel
                  (send! channel {:status  200
                                  :headers {"Content-Type"                "text/event-stream"
                                            "Access-Control-Allow-Origin" "*"
                                            "Cache-Control"               "no-cache"
                                            "Connection"                  "keep-alive"}
                                  :body    ":ok\n\n"} false)
                  (swap! channel-hub assoc channel req)
                  (on-close channel (fn [_]
                                      (swap! channel-hub dissoc channel))))))

(defmethod dispatch :event-push [_]
  (fn [{:keys [params]}]
    (if (some? params)
      (do
        (broadcast (str "data: " params "\n\n"))
        (resp-accepted (str "Broadcasted to " (count @channel-hub) " clients")))
      (resp-error 400 "No event data send"))))
