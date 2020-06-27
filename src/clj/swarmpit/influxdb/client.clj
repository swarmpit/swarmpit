(ns swarmpit.influxdb.client
  (:require [swarmpit.http :refer :all]
            [clojure.string :as str]
            [influxdb.client :as client]
            [influxdb.convert :as convert]
            [swarmpit.config :refer [config]]
            [clojure.tools.logging :as log]))

(defn- execute [fn]
  (let [conn {:url (config :influxdb-url)}]
    (fn conn)))

(defn ping
  []
  (execute-in-scope {:method :GET
                     :url    (str (config :influxdb-url) "/ping")
                     :scope  "InfluxDB"}))

(defn- read-doc
  [q]
  (execute
    (fn [conn] (client/unwrap (client/query conn ::client/read q)))))

(defn- manage-doc
  [q]
  (execute
    (fn [conn] (client/unwrap (client/query conn ::client/manage q)))))

(defn- write-doc
  [point]
  (execute
    (fn [conn] (client/write conn "swarmpit" (convert/point->line point)))))

;; Database

(defn create-database
  []
  (manage-doc "CREATE DATABASE swarmpit"))

(defn drop-database
  []
  (manage-doc "DROP DATABASE swarmpit"))

(defn list-databases
  []
  (read-doc "SHOW DATABASES"))

;; Retention Policy

(defn create-a-day-rp
  "Automatically delete resolution data from CQ that are older than 24 hours"
  []
  (manage-doc "CREATE RETENTION POLICY a_day ON swarmpit DURATION 1d REPLICATION 1"))

(defn create-an-hour-rp
  "Automatically delete the raw resolution data from agent that are older than 1 hour"
  []
  (manage-doc "CREATE RETENTION POLICY an_hour ON swarmpit DURATION 1h REPLICATION 1 DEFAULT"))

(defn drop-retention-policy
  [rp]
  (manage-doc (str "DROP RETENTION POLICY " rp " ON swarmpit")))

(defn list-retention-policies
  [db]
  (read-doc (str "SHOW RETENTION POLICIES ON " db)))

(defn retention-policy-summary
  []
  (->> (-> (list-retention-policies "swarmpit")
           (first)
           (get "series")
           (first)
           (get "values"))
       (map first)
       (set)))

;; Continuous Queries

(defn task-cq
  []
  (manage-doc
    "CREATE CONTINUOUS QUERY cq_tasks_1m ON swarmpit
     BEGIN
       SELECT
        MAX(cpuUsage) / 100 as cpu,
        MAX(memoryUsed) as memory
       INTO a_day.downsampled_tasks
       FROM swarmpit..task_stats
       GROUP BY time(1m), task, service
     END"))

(defn service-cq
  "Warning: Dependant on [cq_tasks_1m]"
  []
  (manage-doc
    "CREATE CONTINUOUS QUERY cq_services_1m ON swarmpit
     BEGIN
       SELECT
        SUM(cpu) as cpu,
        SUM(memory) as memory
       INTO a_day.downsampled_services
       FROM swarmpit.a_day.downsampled_tasks
       GROUP BY time(1m), service
     END"))

(defn service-max-usage-cq
  "Warning: Dependant on [cq_services_1m]"
  []
  (manage-doc
    "CREATE CONTINUOUS QUERY cq_max_usage_services_30m ON swarmpit
     BEGIN
       SELECT
        MAX(cpu) as max_cpu,
        MAX(memory) as max_memory
       INTO a_day.downsampled_max_usage_services
       FROM swarmpit.a_day.downsampled_services
       GROUP BY time(30m), service
     END"))

(defn host-cq
  []
  (manage-doc
    "CREATE CONTINUOUS QUERY cq_hosts_1m ON swarmpit
     BEGIN
       SELECT
        MAX(cpuUsage) as cpu,
        MAX(memoryUsage) as memory
       INTO a_day.downsampled_hosts
       FROM swarmpit..host_stats
       GROUP BY time(1m), host
     END"))

(defn drop-continuous-queries
  [cq]
  (manage-doc (str "DROP CONTINUOUS QUERY " cq " ON swarmpit")))

(defn list-continuous-queries
  []
  (read-doc "SHOW CONTINUOUS QUERIES"))

(defn continuous-query-summary
  []
  (let [queries (-> (list-continuous-queries)
                    (first)
                    (get "series"))
        swarmpit-queries (-> (filter #(= "swarmpit" (get % "name")) queries)
                             (first)
                             (get "values"))]
    (->> swarmpit-queries
         (map first)
         (set))))

(defn write-task-points
  [tags {:keys [cpuPercentage memory memoryLimit memoryPercentage] :as task}]
  (write-doc {:meas   "task_stats"
              :tags   tags
              :fields {:cpuUsage    (-> cpuPercentage (double))
                       :memoryUsage (-> memoryPercentage (double))
                       :memoryUsed  memory
                       :memoryLimit memoryLimit}}))

(defn write-host-points
  [tags {:keys [cpu memory disk] :as stats}]
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
      "SELECT cpu, memory
        FROM swarmpit.a_day.downsampled_tasks
        WHERE task = '" task-name "'
        GROUP BY task, service")))

(defn read-service-stats
  [services]
  (let [cond (->> services
                  (map #(str "service = '" % "'"))
                  (str/join " OR "))]
    (read-doc
      (str
        "SELECT cpu, memory
          FROM swarmpit.a_day.downsampled_services
          WHERE " cond "
          GROUP BY service"))))

(defn read-max-usage-service-stats
  []
  (read-doc
    "SELECT MAX(max_cpu), MAX(max_memory)
      FROM swarmpit.a_day.downsampled_max_usage_services
      GROUP BY service"))

(defn read-host-stats
  []
  (read-doc
    "SELECT cpu, memory
      FROM swarmpit.a_day.downsampled_hosts
      GROUP BY host"))