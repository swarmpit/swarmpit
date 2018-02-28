(ns swarmpit.event.channel
  (:refer-clojure :exclude [list])
  (:require [org.httpkit.server :refer [send!]]
            [clojure.core.async :refer [go <! timeout]]
            [clojure.core.memoize :as memo]
            [clojure.edn :as edn]
            [clojure.walk :refer [keywordize-keys]]
            [cheshire.core :refer [generate-string]]
            [swarmpit.base64 :as base64]
            [swarmpit.event.rules.subscription :as rule]))

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
  ([event]
   "Get subscribed channels based on given event"
   (->> (filter #(rule/match? % event) rule/list)
        (map #(rule/subscription % event))
        (map #(subscribers %))
        (flatten)
        (filter #(some? %))
        (map #(str %))))
  ([]
   "Get subscribed channels"
   (->> (keys @hub)
        (map #(str %)))))

(defn broadcast-data
  [event]
  "Broadcast data to subscribers based on event subscription.
   Broadcast processing is delayed for 1 second due to cluster sync"
  (go
    (<! (timeout 1000))
    (doseq [rule (filter #(rule/match? % event) rule/list)]
      (let [subscription (rule/subscription rule event)
            subscribers (subscribers subscription)]
        (doseq [subscriber subscribers]
          (let [data (rule/subscribed-data rule event)]
            (send! subscriber (event-data {:data data}) false)))))))

;; To prevent data duplicity/spam we cache:
;;
;; 1) Swarm scoped events that are received from each manager at the same time
;; 2) Same local scoped events that occured within 1 second
(def broadcast-data-memo (memo/ttl broadcast-data :ttl/threshold 1000))

(defn broadcast-message
  [message]
  "Broadcast message to all subscribers."
  (go
    (doseq [subscriber (subscribers)]
      (send! subscriber (event-data {:message message}) false))))

(defn broadcast-info
  [target text]
  "Broadcast `text` message for action initiated by `target` user"
  (broadcast-message {:type   :info
                      :text   text
                      :target target}))

(defn broadcast-error
  [target text ex]
  "Broadcast `text` message for action initiated by `target` user"
  (broadcast-message {:type   :error
                      :text   (str text " " (get-in (ex-data ex) [:body :error]))
                      :target target}))