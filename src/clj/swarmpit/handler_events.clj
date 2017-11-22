(ns swarmpit.handler-events
  (:require [org.httpkit.server :refer [with-channel on-close send!]]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.core.memoize :as memo]
            [cheshire.core :refer [generate-string]]
            [immutant.scheduling :refer :all]
            [swarmpit.slt :as slt]
            [swarmpit.handler :refer [dispatch resp-accepted resp-error resp-unauthorized]]))

(def channel-hub (atom {}))

(defn broadcast [data]
  "Broadcast message to all channels and return message hash."
  (let [message (str "data: " (generate-string data) "\n\n")]
    (doseq [channel (keys @channel-hub)]
      (send! channel message false))
    (hash data)))

;; Swarm scoped events are received from each manager, but
;; we want broadcast to FE only once to prevent duplicity.
;; Therefore we cache event message hash for 1 second.
(def broadcast-memo (memo/ttl broadcast :ttl/threshold 1000))

(defmethod dispatch :events [_]
  (fn [{:keys [query-params] :as request}]
    (let [slt (-> (keywordize-keys query-params) :slt)]
      (if (slt/valid? slt)
        (with-channel request channel
                      (send! channel {:status  200
                                      :headers {"Content-Type"  "text/event-stream"
                                                "Cache-Control" "no-cache"
                                                "Connection"    "keep-alive"}
                                      :body    ":ok\n\n"} false)
                      (swap! channel-hub assoc channel request)
                      (on-close channel (fn [_]
                                          (swap! channel-hub dissoc channel))))
        (resp-unauthorized "Invalid slt")))))

(defmethod dispatch :event-push [_]
  (fn [{:keys [params]}]
    (if (some? params)
      (do
        (broadcast-memo params)
        (resp-accepted (str "Broadcasted to " (count @channel-hub) " clients")))
      (resp-error 400 "No data sent"))))