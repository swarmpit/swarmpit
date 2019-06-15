(ns swarmpit.stats
  (:require [clojure.core.cache :as cache]
            [swarmpit.influxdb.client :as influx]
            [swarmpit.influxdb.mapper :refer [->task-tags ->host-tags ->memory-mb]]))

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
  [{:keys [id cpu memory tasks] :as stats}]
  (let [host-tags (->host-tags id)]
    (influx/write-host-points host-tags
                              (double (:usedPercentage cpu))
                              (->memory-mb (:used memory)))
    (doseq [{:keys [cpuPercentage memory name] :as task} tasks]
      (when-let [task-tags (->task-tags name id)]
        (influx/write-task-points task-tags
                                  (double cpuPercentage)
                                  (->memory-mb memory))))))

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
  [task-name]
  (let [values (-> (influx/read-task-stats task-name)
                   (first)
                   (get "series")
                   (first)
                   (get "values"))]
    {:time   (into [] (map first values))
     :cpu    (into [] (map second values))
     :memory (into [] (map #(nth % 2) values))}))