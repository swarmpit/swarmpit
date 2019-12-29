(ns swarmpit.influxdb.client
  (:require [swarmpit.http :refer :all]
            [influxdb.client :as client]
            [influxdb.convert :as convert]
            [swarmpit.config :refer [config]]))

(defn- execute [fn]
  (let [conn {:url (config :influxdb-url)}]
    (fn conn)))

(defn ping []
  (execute-in-scope {:method :GET
                     :url    (str (config :influxdb-url) "/ping")
                     :scope  "InfluxDB"}))

(defn- read-doc [q]
  (execute
    (fn [conn] (client/unwrap (client/query conn ::client/read q)))))

(defn- manage-doc [q]
  (execute
    (fn [conn] (client/unwrap (client/query conn ::client/manage q)))))

(defn- write-doc [point]
  (execute
    (fn [conn] (client/write conn "swarmpit" (convert/point->line point)))))

(defn create-database []
  (manage-doc "CREATE DATABASE swarmpit"))

(defn drop-database []
  (manage-doc "DROP DATABASE swarmpit"))

(defn list-databases []
  (read-doc "SHOW DATABASES"))

(defn write-task-points [tags {:keys [cpuPercentage memory memoryLimit memoryPercentage] :as task}]
  (write-doc {:meas   "task_stats"
              :tags   tags
              :fields {:cpuUsage    (-> cpuPercentage (double))
                       :memoryUsage (-> memoryPercentage (double))
                       :memoryUsed  memory
                       :memoryLimit memoryLimit}}))

(defn write-host-points [tags {:keys [cpu memory disk] :as stats}]
  (write-doc {:meas   "host_stats"
              :tags   tags
              :fields {:cpuUsage    (-> (:usedPercentage cpu) (double))
                       :memoryUsage (-> (:usedPercentage memory) (double))
                       :diskUsage   (-> (:usedPercentage disk) (double))
                       :memoryUsed  (:used memory)
                       :memoryTotal (:total memory)
                       :memoryFree  (:free memory)
                       :diskUsed    (:used disk)
                       :diskTotal   (:total disk)
                       :diskFree    (:free disk)}}))

(defn read-task-stats
  [task-name]
  (read-doc
    (str
      "SELECT
        MAX(cpuUsage) / 100 as cpu,
        MAX(memoryUsed) as memory
         FROM swarmpit..task_stats
          WHERE task = '" task-name "' AND time > now() - 1d
          GROUP BY time(1m), task, service")))

(defn read-services-cpu-stats
  []
  (read-doc
    "SELECT
      SUM(cpu) as cpu
       FROM
        (SELECT
          MAX(cpuUsage) / 100  as cpu
           FROM swarmpit..task_stats
            WHERE time > now() - 1d
            GROUP BY time(1m), task, service ORDER BY ASC SLIMIT 10)
       GROUP BY time(1m), service ORDER BY ASC"))

(defn read-services-memory-stats
  []
  (read-doc
    "SELECT
      SUM(memory) as memory
       FROM
        (SELECT
          MAX(memoryUsed) as memory
           FROM swarmpit..task_stats
            WHERE time > now() - 1d
            GROUP BY time(1m), task, service ORDER BY ASC SLIMIT 10)
       GROUP BY time(1m), service ORDER BY ASC"))

(defn read-host-stats
  []
  (read-doc
    "SELECT
      MAX(cpuUsage) as cpu,
      MAX(memoryUsage) as memory
       FROM swarmpit..host_stats
        WHERE time > now() - 1d
        GROUP BY time(1m), host"))