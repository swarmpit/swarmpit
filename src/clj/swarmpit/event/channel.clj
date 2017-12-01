(ns swarmpit.event.channel
  (:require [org.httpkit.server :refer [send!]]
            [clojure.core.async :refer [go <! timeout]]
            [clojure.core.memoize :as memo]
            [clojure.walk :refer [keywordize-keys]]
            [cheshire.core :refer [generate-string]]
            [swarmpit.base64 :as base64]
            [swarmpit.event.rules :refer :all]))

(def channel-hub (atom {}))

(defn- channel-message
  [data]
  (str "data: " (generate-string data) "\n\n"))

(defn- channel-subscription
  [channel]
  (-> (val channel)
      (get-in [:query-params "subscription"])))

(defn- channels
  [subscription]
  (let [encoded-suscription (base64/encode subscription)]
    (->> @channel-hub
         (filter #(= encoded-suscription (channel-subscription %)))
         (keys))))

(def rules [service-container-event
            task-container-event
            service-event
            node-event])

(defn- event-rules
  [event]
  (filter #(match? % event) rules))

(defn broadcast [event]
  "Broadcast data to subscribers based on event subscription.
   Broadcast processing is delayed for 1 second due to cluster sync"
  (go
    (<! (timeout 1000))
    (doseq [rule (event-rules event)]
      (let [subscriber (subscriber rule event)
            subscribed-channels (channels subscriber)]
        (when (not-empty subscribed-channels)
          (doseq [channel subscribed-channels]
            (send! channel (channel-message (subscribed-data rule event)) false)))))))

;; To prevent duplicity/spam we cache:
;;
;; 1) Swarm scoped events that are received from each manager at the same time
;; 2) Same local scoped events that occured within 1 second
(def broadcast-memo (memo/ttl broadcast :ttl/threshold 1000))