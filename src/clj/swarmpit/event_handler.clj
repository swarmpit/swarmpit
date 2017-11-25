(ns swarmpit.event-handler
  (:require [org.httpkit.server :refer [with-channel on-close send!]]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.core.memoize :as memo]
            [cheshire.core :refer [generate-string]]
            [immutant.scheduling :refer :all]
            [swarmpit.slt :as slt]
            [swarmpit.event-mapper :as event]
            [swarmpit.handler :refer [dispatch resp-accepted resp-error resp-unauthorized]]))

(def channel-hub (atom {}))

(defn broadcast [event]
  "Broadcast message to all channels and return message hash."
  (let [message (str "data: " (generate-string event) "\n\n")]
    (doseq [channel (keys @channel-hub)]
      (send! channel message false))
    (hash event)))

;; To prevent duplicity/spam we cache:
;;
;; 1) Swarm scoped events that are received from each manager at the same time
;; 2) Same local scoped service container events that occured within 1 second
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
        (-> (keywordize-keys params)
            (event/transform)
            (broadcast-memo))
        (resp-accepted (str "Broadcasted to " (count @channel-hub) " clients")))
      (resp-error 400 "No data sent"))))