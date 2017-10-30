(ns swarmpit.handler-events
  (:require [org.httpkit.server :refer [run-server with-channel on-close send! close]]
            [cheshire.core :refer [generate-string]]
            [swarmpit.handler :refer [dispatch resp-accepted resp-error]]))

(def channel-hub (atom {}))

(defn broadcast [message]
  (doseq [channel (keys @channel-hub)]
    (send! channel message false)))

(defmethod dispatch :events [_]
  (fn [request]
    (with-channel request channel
                  (send! channel {:status  200
                                  :headers {"Content-Type"                "text/event-stream"
                                            "Access-Control-Allow-Origin" "*"
                                            "Cache-Control"               "no-cache"
                                            "Connection"                  "keep-alive"}
                                  :body    ":ok\n\n"} false)
                  (swap! channel-hub assoc channel request)
                  (on-close channel (fn [_]
                                      (swap! channel-hub dissoc channel))))))

(defmethod dispatch :event-push [_]
  (fn [{:keys [params]}]
    (if (some? params)
      (do
        (broadcast (str "data: " (generate-string params) "\n\n"))
        (resp-accepted (str "Broadcasted to " (count @channel-hub) " clients")))
      (resp-error 400 "No data send"))))
