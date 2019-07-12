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
  [{:keys [id cpu memory tasks] :as stats}]
  (let [host-tags (m/->host-tags id)]
    (influx/write-host-points host-tags
                              (m/->cpu-round (:usedPercentage cpu))
                              (m/->memory-mb (:used memory)))
    (doseq [{:keys [cpuPercentage memory name] :as task} tasks]
      (when-let [task-tags (m/->task-tags name id)]
        (influx/write-task-points task-tags
                                  (m/->cpu-round cpuPercentage)
                                  (m/->memory-mb memory))))))

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

(defn- timeseries-values [task-name]
  (-> (influx/read-task-stats task-name)
      (first)
      (get "series")
      (first)
      (get "values")))

(defn task-timeseries
  "Get task timeseries data for last 24 hours"
  [task-name]
  (let [values (timeseries-values task-name)]
    (m/->format values)))

(defn service-timeseries
  "Get service timeseries data for last 24 hours"
  [service-tasks nodes-count]
  (let [valid-stats (fn [grouped-item]
                      (->> (second grouped-item)
                           (map #(drop 1 %))
                           (filter #(not-every? nil? %))))
        aggregate-stats (fn [grouped-item stats]
                          (->> (reduce #(mapv + %1 %2) stats)
                               (vector (first grouped-item))
                               (flatten)
                               (into [])))]
    (->> service-tasks
         (map #(timeseries-values %))
         (apply concat)
         (group-by first)
         (sort)
         (map (fn [item]
                (let [stats (valid-stats item)]
                  (if (not-empty stats)
                    (aggregate-stats item stats)
                    (vector (first item) nil nil)))))
         (into [])
         (m/->format))))

