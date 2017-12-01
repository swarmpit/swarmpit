(ns swarmpit.event.channel
  (:refer-clojure :exclude [list])
  (:require [org.httpkit.server :refer [send!]]
            [clojure.core.async :refer [go <! timeout]]
            [clojure.core.memoize :as memo]
            [clojure.edn :as edn]
            [clojure.walk :refer [keywordize-keys]]
            [cheshire.core :refer [generate-string]]
            [swarmpit.base64 :as base64]
            [swarmpit.event.rule :as rule]))

(def hub (atom {}))

(defn- message
  [data]
  (str "data: " (generate-string data) "\n\n"))

(defn- subscription
  [channel]
  (-> (val channel)
      (get-in [:query-params "subscription"])
      (base64/decode)
      (edn/read-string)))

(defn- subscribers
  [channel-subscription]
  (->> @hub
       (filter #(= channel-subscription (subscription %)))
       (keys)))

(defn list
  ([event]
   "Get subscribed channel list based on given event"
   (->> (filter #(rule/match? % event) rule/list)
        (map #(rule/subscription % event))
        (map #(subscribers %))
        (flatten)
        (filter #(some? %))
        (map #(str %))))
  ([]
   "Get susbcribed channel list"
   (->> (keys @hub)
        (map #(str %)))))

(defn broadcast
  [event]
  "Broadcast data to subscribers based on event subscription.
   Broadcast processing is delayed for 1 second due to cluster sync"
  (go
    (<! (timeout 1000))
    (doseq [rule (filter #(rule/match? % event) rule/list)]
      (let [subscription (rule/subscription rule event)
            subscribers (subscribers subscription)]
        (when (not-empty subscribers)
          (doseq [subscriber subscribers]
            (let [data (rule/subscribed-data rule event)]
              (send! subscriber (message data) false))))))))

;; To prevent duplicity/spam we cache:
;;
;; 1) Swarm scoped events that are received from each manager at the same time
;; 2) Same local scoped events that occured within 1 second
(def broadcast-memo (memo/ttl broadcast :ttl/threshold 1000))