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
  [{:keys [id cpu memory disk tasks] :as stats}]
  (let [host-tags (m/->host-tags id)]
    (influx/write-host-points host-tags
                              {:cpuUsage    (m/->cpu-round (:usedPercentage cpu))
                               :memoryUsage (m/->cpu-round (:usedPercentage memory))
                               :diskUsage   (m/->cpu-round (:usedPercentage disk))
                               :memoryUsed  (m/->memory-mb (:used memory))
                               :memoryTotal (m/->memory-mb (:total memory))
                               :memoryFree  (m/->memory-mb (:free memory))
                               :diskUsed    (m/->disk-gb (:used disk))
                               :diskTotal   (m/->disk-gb (:total disk))
                               :diskFree    (m/->disk-gb (:free disk))})
    (doseq [{:keys [cpuPercentage memory memoryLimit memoryPercentage name] :as task} tasks]
      (when-let [task-tags (m/->task-tags name id)]
        (influx/write-task-points task-tags
                                  {:cpuUsage    (m/->cpu-round cpuPercentage)
                                   :memoryUsage (m/->cpu-round memoryPercentage)
                                   :memoryUsed  (m/->memory-mb memory)
                                   :memoryLimit (m/->memory-mb memoryLimit)})))))

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
  []
  (map #(m/->host-ts %)
       (-> (influx/read-host-stats)
           (first)
           (get "series"))))

(defn cluster
  []
  (-> (influx/read-cluster-stats)
      (first)
      (get "series")
      (first)
      (m/->cluster)))



;(defn service-timeseries
;  "Get service timeseries data for last 24 hours"
;  [service-tasks nodes-count]
;  (let [valid-stats (fn [grouped-item]
;                      (->> (second grouped-item)
;                           (map #(drop 1 %))
;                           (filter #(not-every? nil? %))))
;        aggregate-stats (fn [grouped-item stats]
;                          (->> (reduce #(mapv + %1 %2) stats)
;                               (vector (first grouped-item))
;                               (flatten)
;                               (into [])))]
;    (->> service-tasks
;         (map #(timeseries-values %))
;         (apply concat)
;         (group-by first)
;         (sort)
;         (map (fn [item]
;                (let [stats (valid-stats item)]
;                  (if (not-empty stats)
;                    (aggregate-stats item stats)
;                    (vector (first item) nil nil)))))
;         (into [])
;         (m/->format))))

