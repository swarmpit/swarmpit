(ns swarmpit.stats
  (:require [clojure.core.cache :as cache]
            [swarmpit.influxdb.client :as influx]
            [swarmpit.influxdb.mapper :as m]))

(def cache (atom (cache/basic-cache-factory {})))

(defn configure
  []
  (influx/create-database))

(defn store-to-cache
  "Store stats in local cache"
  [stats]
  (swap! cache assoc (:id stats) stats))

(defn store-to-db
  "Store stats in influxDB as timeseries"
  [{:keys [id tasks] :as stats}]
  (let [host-tags (m/->host-tags id)]
    (influx/write-host-points host-tags stats)
    (doseq [{:keys [name] :as task-stats} tasks]
      (when-let [task-tags (m/->task-tags name id)]
        (influx/write-task-points task-tags task-stats)))))

(defn node
  "Get latest node stats from local cache"
  [node-id]
  (get @cache node-id))

(defn task
  "Get latest task stats from local cache"
  [task]
  (->> (node (:nodeId task))
       :tasks
       (filter #(= (str "/" (:taskName task) "." (:id task))
                   (:name %)))
       (first)))

(defn task-timeseries
  "Get task timeseries data for last 24 hours"
  [task-name]
  (-> (influx/read-task-stats task-name)
      (first)
      (get "series")
      (first)
      (m/->task-ts)))

(defn hosts-timeseries
  "Get hosts timeseries data for last 24 hours"
  []
  (map #(m/->host-ts %)
       (-> (influx/read-host-stats)
           (first)
           (get "series"))))

(defn cluster
  "Get latest cluster statistics"
  []
  (-> (influx/read-cluster-stats)
      (first)
      (get "series")
      (first)
      (m/->cluster)))

