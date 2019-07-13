(ns swarmpit.influxdb.client
  (:require [influxdb.client :as client]
            [influxdb.convert :as convert]
            [swarmpit.config :refer [config]]
            [clojure.tools.logging :as log]))

(defn- execute [fn]
  (let [conn {:url (config :influxdb-url)}]
    (fn conn)))

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

(defn write-task-points [tags fields]
  (write-doc {:meas   "task_stats"
              :tags   tags
              :fields fields}))

(defn write-host-points [tags fields]
  (write-doc {:meas   "host_stats"
              :tags   tags
              :fields fields}))

(defn read-task-stats
  ([]
   (read-doc "SELECT MAX(cpuUsage) as cpu, MAX(memoryUsed) as memory FROM swarmpit..task_stats WHERE time > now() - 1d GROUP BY time(1m), task, service"))
  ([task-name]
   (read-doc
     (str "SELECT MAX(cpuUsage) as cpu, MAX(memoryUsed) as memory FROM swarmpit..task_stats WHERE task = '" task-name "' AND time > now() - 1d GROUP BY time(1m), task, service"))))

(defn read-host-stats
  []
  (read-doc "SELECT MAX(cpuUsage) as cpu, MAX(memoryUsed) as memory FROM swarmpit..host_stats WHERE time > now() - 1d GROUP BY time(1m), host"))

(def last-host-query
  "SELECT
    LAST(cpuUsage) as cpuUsage,
    LAST(memoryUsage) as memoryUsage,
    LAST(diskUsage) as diskUsage,
    LAST(memoryUsed) as memoryUsed,
    LAST(memoryTotal) as memoryTotal,
    LAST(diskUsed) as diskUsed,
    LAST(diskTotal) as diskTotal FROM swarmpit..host_stats")

(defn read-cluster-stats
  []
  (read-doc
    (str
      "SELECT
        MEAN(cpuUsage) as cpuUsage,
        MEAN(memoryUsage) as memoryUsage,
        MEAN(diskUsage) as diskUsage,
        SUM(memoryUsed) as memoryUsed,
        SUM(memoryTotal) as memoryTotal,
        SUM(diskUsed) as diskUsed,
        SUM(diskTotal) as diskTotal FROM (" last-host-query " GROUP BY host)")))

;(read-doc "SELECT MEAN(memory) as avg_memory, MAX(memory) as max_memory, MIN(memory) as min_memory FROM swarmpit..task_stats GROUP BY task, service")

(defn read-tasks-avg-memory []
  (read-doc "SELECT TOP(average_memory, 10) as memory, task, service FROM (SELECT MEAN(memory) as average_memory FROM swarmpit..task_stats GROUP BY task, service)"))

(defn read-tasks-avg-cpu []
  (read-doc "SELECT TOP(average_cpu, 10) as cpu, task, service FROM (SELECT MEAN(cpu) as average_cpu FROM swarmpit..task_stats GROUP BY task, service)"))

(defn read-service-atats [service-name nodes-count]
  (read-doc
    (str "SELECT SUM(cpu) / " nodes-count " as cpu, SUM(memory) as memory FROM swarmpit..task_stats WHERE service = '" service-name "' AND time > now() - 1d GROUP BY time(30s)")))