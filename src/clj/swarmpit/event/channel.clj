(ns swarmpit.event.channel
  (:refer-clojure :exclude [list])
  (:require [org.httpkit.server :refer [send!]]
            [clojure.core.async :refer [go <! timeout]]
            [clojure.core.memoize :as memo]
            [clojure.edn :as edn]
            [clojure.walk :refer [keywordize-keys]]
            [cheshire.core :refer [generate-string]]
            [swarmpit.base64 :as base64]
            [swarmpit.event.rules.subscription :as rule]
            [swarmpit.event.rules.subscription-stats :as stats]))

(def hub (atom {}))

(defn- event-data
  [data]
  (str "data: " (generate-string data) "\n\n"))

(defn- subscription
  [channel]
  (-> (val channel)
      (get-in [:query-params "subscription"])
      (base64/decode)
      (edn/read-string)))

(defn- subscribers
  ([channel-subscription]
   "Get subscribers based on given subscription"
   (->> @hub
        (filter #(= channel-subscription (subscription %)))
        (keys)))
  ([]
   "Get all subscribers"
   (-> @hub (keys))))

(defn list
  ([{:keys [type message] :as event}]
   "Get subscribed channels based on given event"
   (->> (filter #(rule/match? % type message) rule/list)
        (map #(rule/subscription % message))
        (map #(subscribers %))
        (flatten)
        (filter #(some? %))
        (map #(str %))))
  ([]
   "Get subscribed channels"
   (->> (keys @hub)
        (map #(str %)))))

(defn broadcast
  [{:keys [type message] :as event}]
  "Broadcast data to subscribers based on event subscription.
   Broadcast processing is delayed for 1 second due to cluster sync"
  (go
    (<! (timeout 1000))
    (doseq [rule (filter #(rule/match? % type message) rule/list)]
      (let [subscription (rule/subscription rule message)
            subscribers (subscribers subscription)]
        (doseq [subscriber subscribers]
          (let [data (rule/subscribed-data rule message)]
            (send! subscriber (event-data data) false)))))))

(defn broadcast-statistics
  []
  "Broadcast data with actual statistics records to all corresponding subscribers"
  (go
    (let [subscribers (filter #(contains? stats/subscribers (:handler (subscription %))) @hub)]
      (doseq [subscriber subscribers]
        (let [subscription (subscription subscriber)
              data (stats/subscribed-data subscription)]
          (send! (key subscriber) (event-data data) false))))))

;; To prevent data duplicity/spam we cache:
;;
;; 1) Swarm scoped events that are received from each manager at the same time
;; 2) Same local scoped events that occured within 1 second
(def broadcast-memo (memo/ttl broadcast :ttl/threshold 1000))